// Protocol port from LibLaserCut LaserToolsTechnicsCutter, LGPL-3.0-or-later.
// Original driver: Maximilian Gaukler; portions by Thomas Oster.
// Provenance and deliberate limitations: ../../PROTOCOL.md.
mod order;
mod vector;

use crate::{
    geometry,
    project::{Operation, ParameterSet, Project},
    raster::{Raster, RasterSettings},
    timeline::{MotionKind, Program, Timeline},
};
use resvg::{tiny_skia, usvg};
use std::{
    io::Write,
    net::{Shutdown, TcpStream, ToSocketAddrs},
    time::Duration,
};

const MACHINE_DPI: f64 = 4000.0;
const RASTER_DPI: f64 = 500.0;
const BED_HEIGHT: f64 = 600.0;
const BED_WIDTH: f64 = 1000.0;
/// Cutting speed at 100 % (FAU device profile, nominalCuttingSpeed).
const NOMINAL_CUT_SPEED: f64 = 338.677;
/// Full engrave speed relative to full cutting speed (Java driver).
const ENGRAVE_SPEED_FACTOR: f64 = 6.4;
/// Engrave shift table of the FAU device for 10 %, 20 %, …, 100 % speed.
const ENGRAVE_SHIFT: [f32; 10] = [
    -2.0, -4.0, -7.0, -10.0, -13.0, -15.0, -17.0, -19.0, -21.0, -23.0,
];

fn raw(mm: f64) -> u32 {
    (mm * MACHINE_DPI / 25.4).round() as u32
}
fn px(mm: f64) -> i32 {
    (mm * RASTER_DPI / 25.4) as i32
}
fn word(out: &mut Vec<u8>, value: u16) {
    out.extend(value.to_be_bytes());
}
fn dword(out: &mut Vec<u8>, value: u32) {
    out.extend(value.to_be_bytes());
}

// Rotary engraving: steps per full turn, an approximation in LibLaserCut.
const ROTARY_STEPS_PER_REVOLUTION: f64 = 6400.0;

/// Device Y axis of a job. XY jobs count from the bottom edge of the bed;
/// rotary jobs use the cylinder rotation, counted from the top, unmirrored.
#[derive(Clone, Copy)]
struct Axis {
    rotary_radius_mm: Option<f64>,
}

impl Axis {
    fn of(project: &Project) -> Self {
        Self {
            rotary_radius_mm: project
                .rotary_axis
                .then_some(project.rotary_diameter_mm as f64 / 2.0),
        }
    }
    #[cfg(test)]
    fn xy() -> Self {
        Self {
            rotary_radius_mm: None,
        }
    }
    fn rotary(radius_mm: f64, mm: f64) -> i32 {
        (mm / (radius_mm * 2.0 * std::f64::consts::PI) * ROTARY_STEPS_PER_REVOLUTION).round() as i32
    }
    /// Java prescalingY: vector Y is scaled by this factor while planning
    /// speeds, so one acceleration limit fits both axes on the rotary axis.
    fn prescale(self) -> f64 {
        match self.rotary_radius_mm {
            Some(r) => (Self::rotary(r, 254.0) as f64 / 40000.0).abs(),
            None => 1.0,
        }
    }
    /// Absolute position of a (prescaled) 500-DPI coordinate.
    fn absolute_scaled(self, y: i32, scale: f64) -> i32 {
        let y = y as f64 / scale;
        match self.rotary_radius_mm {
            Some(r) => Self::rotary(r, y * 25.4 / RASTER_DPI),
            None => (BED_HEIGHT * MACHINE_DPI / 25.4) as i32 - (y * 8.0).round() as i32,
        }
    }
    /// Relative move by `dy` (prescaled) 500-DPI rows.
    fn relative_scaled(self, dy: i32, scale: f64) -> i32 {
        let dy = dy as f64 / scale;
        match self.rotary_radius_mm {
            Some(r) => Self::rotary(r, dy * 25.4 / RASTER_DPI),
            None => -(dy * 8.0).round() as i32,
        }
    }
    fn absolute(self, y: i32) -> i32 {
        self.absolute_scaled(y, 1.0)
    }
    #[cfg(test)]
    fn relative(self, dy: i32) -> i32 {
        self.relative_scaled(dy, 1.0)
    }
    /// Unmirrored bounding-box coordinate in mm.
    fn bounding(self, mm: f64) -> u32 {
        match self.rotary_radius_mm {
            Some(r) => Self::rotary(r, mm) as u32,
            None => raw(mm),
        }
    }
}

fn pair(out: &mut Vec<u8>, axis: Axis, x: i32, y: i32) {
    dword(out, (x * 8) as u32);
    dword(out, axis.absolute(y) as u32);
}

#[derive(serde::Serialize)]
pub struct PreparedStep {
    pub operation: Operation,
    pub name: String,
    pub description: String,
    pub estimated_seconds: f64,
    pub power_percent: f32,
    pub speed_percent: f32,
    pub passes: u32,
    pub parameter_sets: usize,
}

#[derive(serde::Serialize)]
pub struct OutputJob {
    pub name: String,
    pub operation: Operation,
    pub bytes: Vec<u8>,
}

pub struct PreparedJob {
    pub jobs: Vec<OutputJob>,
    pub description: String,
    pub estimated_seconds: f64,
    pub preview_png: Vec<u8>,
    pub steps: Vec<PreparedStep>,
    pub timeline: Timeline,
    /// Instructions the operator must follow at the machine.
    pub warnings: Vec<String>,
}

/// Objects of one processing step with its parameter sets.
struct Part {
    operation: Operation,
    svg: String,
    raster: RasterSettings,
    sets: Vec<ParameterSet>,
}

fn overscan(operation: Operation, speed_percent: f32) -> f32 {
    if operation.is_raster() {
        3.5_f32.max(35.0 * (speed_percent / 100.0).powi(2))
    } else {
        0.0
    }
}

pub fn prepare(project: &Project) -> Result<PreparedJob, String> {
    project.validate()?;
    if (project.bed_width_mm - BED_WIDTH as f32).abs() > 0.01
        || (project.bed_height_mm - BED_HEIGHT as f32).abs() > 0.01
    {
        return Err("Das FAU-LTT-Profil benötigt ein Arbeitsbett von 1000 × 600 mm".into());
    }
    if project.rotary_axis
        && project.height_mm > std::f32::consts::PI * project.rotary_diameter_mm + 0.001
    {
        return Err(format!(
            "Motiv ist höher als der Umfang des Werkstücks ({:.1} mm bei {:.1} mm Durchmesser)",
            std::f32::consts::PI * project.rotary_diameter_mm,
            project.rotary_diameter_mm
        ));
    }
    let mut parts = Vec::new();
    if project.steps.is_empty() {
        parts.push(Part {
            operation: project.operation,
            svg: project.svg.clone(),
            raster: project.raster,
            sets: vec![ParameterSet {
                power_percent: project.power_percent,
                speed_percent: project.speed_percent,
                passes: project.passes,
            }],
        });
    } else {
        let selections = crate::mapping::resolve(project)?;
        for (step, selected) in project.steps.iter().zip(selections) {
            if selected.is_empty() {
                continue;
            }
            parts.push(Part {
                operation: step.operation,
                svg: crate::selection::filter(&project.svg, &selected)?,
                raster: step.raster,
                sets: step.parameters(),
            });
        }
    }
    if parts.is_empty() {
        return Err("Keine Objekte zur Bearbeitung ausgewählt".into());
    }
    let mut jobs = Vec::new();
    let mut total_bytes: usize = 0;
    let scale = 1600.0 / project.width_mm.max(project.height_mm);
    let mut preview = tiny_skia::Pixmap::new(
        (project.width_mm * scale).ceil().max(1.0) as u32,
        (project.height_mm * scale).ceil().max(1.0) as u32,
    )
    .ok_or("Vorschau konnte nicht erstellt werden")?;
    let mut steps = Vec::new();
    let mut timeline = Timeline::default();
    let mut warnings = Vec::new();
    if project.rotary_axis {
        let overscan = parts
            .iter()
            .flat_map(|p| {
                p.sets
                    .iter()
                    .map(|s| overscan(p.operation, s.speed_percent))
            })
            .fold(0.0, f32::max);
        let left = project.width_mm / 2.0 + overscan.min(project.x_mm);
        let right = project.width_mm / 2.0
            + overscan.min(BED_WIDTH as f32 - project.x_mm - project.width_mm);
        warnings.push(format!(
            "Drehachse aktiv: Am Gerät mit „Adjust rotary temp“ die Mitte der Gravur einstellen. \
             Der Laserkopf muss {left:.0} mm nach links und {right:.0} mm nach rechts \
             kollisionsfrei fahren können (inklusive Bremsweg)."
        ));
    }
    // Finishing operations first, cutting last; one LTT file per operation
    // holding its steps in order.
    for operation in Operation::ALL {
        let group: Vec<&Part> = parts.iter().filter(|p| p.operation == operation).collect();
        if group.is_empty() {
            continue;
        }
        let name = device_job_name(operation, &project.name);
        let overscan = group
            .iter()
            .flat_map(|p| p.sets.iter().map(|s| overscan(operation, s.speed_percent)))
            .fold(0.0, f32::max);
        // The header holds the bounds of the actual content (LaserJob
        // .getBoundingBox in Java), so it is written after the parts.
        let mut body = Vec::new();
        let mut bounds: Option<Bounds> = None;
        let mut state = DeviceState::default();
        for (index, part) in group.iter().enumerate() {
            let (step, part_bounds) = append_part(
                &mut body,
                &mut state,
                project,
                part,
                index == 0,
                &name,
                &mut preview,
                &mut timeline,
            )?;
            steps.push(step);
            bounds = Some(bounds.map_or(part_bounds, |b| b.union(part_bounds)));
        }
        let mut out = header(project, overscan, &name, bounds.unwrap_or_default());
        out.extend(body);
        finish(&mut out)?;
        total_bytes = total_bytes.saturating_add(out.len());
        if total_bytes > 256 * 1024 * 1024 {
            return Err("Gesamtauftrag ist größer als 256 MB".into());
        }
        jobs.push(OutputJob {
            name,
            operation,
            bytes: out,
        });
    }
    Ok(PreparedJob {
        jobs,
        description: steps
            .iter()
            .map(|s| s.description.as_str())
            .collect::<Vec<_>>()
            .join(" · "),
        estimated_seconds: timeline.duration_seconds,
        preview_png: preview.encode_png().map_err(|e| e.to_string())?,
        steps,
        timeline,
        warnings,
    })
}

fn device_job_name(operation: Operation, name: &str) -> String {
    format!("{}_{}", operation.prefix(), name)
        .chars()
        .map(|c| {
            if c.is_ascii_alphanumeric() || c == '-' || c == '_' {
                c
            } else {
                '_'
            }
        })
        .take(15)
        .collect()
}

fn finish(out: &mut Vec<u8>) -> Result<(), String> {
    out.extend([0x1b, 0x42, 0x59, 0x45]);
    let checksum = out
        .iter()
        .fold(0u16, |sum, byte| sum.wrapping_add(*byte as u16));
    let length = u32::try_from(out.len() + 6).map_err(|_| "Job ist zu groß")?;
    word(out, checksum);
    dword(out, length);
    Ok(())
}

/// Speed and power last sent in a job; the Java driver resets them to -1 at
/// the start of a job and sends each only when it changes.
#[derive(Clone, Copy)]
struct DeviceState {
    speed: f32,
    power: f32,
}

impl Default for DeviceState {
    fn default() -> Self {
        Self {
            speed: -1.0,
            power: -1.0,
        }
    }
}

impl DeviceState {
    /// setCurrentProperty: speed, then power, each only when changed.
    fn set(&mut self, out: &mut Vec<u8>, speed: f32, power: f32) {
        if speed != self.speed {
            out.extend([0x1b, 0x53]);
            word(out, ((speed * 10.0) as i32).clamp(1, 1000) as u16);
            self.speed = speed;
        }
        if power != self.power {
            out.extend([0x1b, 0x4a]);
            word(out, ((power * 10.0) as i32).clamp(1, 1000) as u16);
            self.power = power;
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn append_part(
    out: &mut Vec<u8>,
    state: &mut DeviceState,
    project: &Project,
    part: &Part,
    first_in_file: bool,
    name: &str,
    preview: &mut tiny_skia::Pixmap,
    timeline: &mut Timeline,
) -> Result<(PreparedStep, Bounds), String> {
    if part.sets.iter().any(|s| s.power_percent < 0.1) {
        return Err("Leistung für einen LTT-Job muss mindestens 0,1 % sein".into());
    }
    let mut source = project.clone();
    source.svg = part.svg.clone();
    source.steps.clear();
    let axis = Axis::of(project);
    let mut estimated = 0.0;
    let description;
    let bounds;
    match part.operation {
        Operation::Cut | Operation::Mark => {
            let paths = geometry::contours(&source)?;
            let passes: usize = part.sets.iter().map(|s| s.passes as usize).sum();
            let estimated_bytes = paths
                .iter()
                .map(Vec::len)
                .sum::<usize>()
                .saturating_mul(passes)
                .saturating_mul(12);
            if estimated_bytes > 256 * 1024 * 1024 {
                return Err("Vektorjob ist größer als 256 MB".into());
            }
            draw_contours(preview, project, &paths, part.operation);
            for point in paths.iter().flatten() {
                if !point[0].is_finite()
                    || !point[1].is_finite()
                    || point[0] < 0.0
                    || point[1] < 0.0
                    || point[0] > BED_WIDTH
                    || point[1] > BED_HEIGHT
                {
                    return Err(
                        "SVG-Pfad liegt außerhalb des tatsächlichen LTT-Arbeitsbetts".into(),
                    );
                }
            }

            out.extend([0x1b, 0x56]); // vector mode
            out.extend([0x1b, 0x45, 0, 0, 0, 0, 0, 0, 0]); // pulse mode off
            out.extend([0x1b, 0x4e, 1]); // colour code red
            out.extend(b"PS");
            out.extend([0x1b, 0x50, 0, 4]); // PPI divisor
            let kind = if part.operation == Operation::Mark {
                MotionKind::Mark
            } else {
                MotionKind::Cut
            };
            // As in VisiCut's VectorProfile, the shapes are added once per
            // LaserProperty and every pass is one more property; the optimizer
            // then orders all of them together (see order::inner_first).
            // VisiCut adds the material thickness to the focus of each property.
            let mut keys = Vec::new();
            for set in &part.sets {
                for _ in 0..set.passes {
                    keys.push(order::SetKey {
                        power: set.power_percent,
                        speed: set.speed_percent,
                        focus: project.thickness_mm,
                    });
                }
            }
            // The shapes in pixels as VisiCut's ShapeConverter adds them.
            let shapes: Vec<([f64; 2], Vec<[f64; 2]>)> = paths
                .iter()
                .filter_map(|path| shape_converter(path))
                .collect();
            let elements: Vec<order::Element> = (0..keys.len())
                .flat_map(|set| {
                    shapes.iter().map(move |(start, moves)| order::Element {
                        set,
                        start: *start,
                        moves: moves.clone(),
                    })
                })
                .collect();
            // LaserJob.getBoundingBox: extent of the commands, px2mm per point.
            bounds = Bounds::of_points(
                shapes
                    .iter()
                    .flat_map(|(start, moves)| std::iter::once(start).chain(moves))
                    .map(|p| [p[0] / RASTER_DPI * 25.4, p[1] / RASTER_DPI * 25.4]),
            )
            .ok_or("Keine Vektorpfade zum Bearbeiten")?;
            let ordered = order::inner_first(elements, &keys);
            // Java prescales Y only while generating the commands.
            let scale_y = axis.prescale();
            let mut program = Program::new(part.operation);
            let (circles, curves) = {
                let mut encoder = vector::Encoder::new(
                    out,
                    axis,
                    state.speed,
                    state.power,
                    Some(&mut program),
                    kind,
                );
                let mut current: Option<order::SetKey> = None;
                for element in &ordered {
                    let key = keys[element.set];
                    if !current.is_some_and(|c| c.same(key)) {
                        encoder.set_speed(key.speed);
                        encoder.set_power(key.power);
                        current = Some(key);
                    }
                    let polyline: Vec<(f64, f64)> = element
                        .moves
                        .iter()
                        .map(|p| (p[0], p[1] * scale_y))
                        .collect();
                    encoder.move_to(element.start[0], element.start[1] * scale_y);
                    encoder.polyline(&polyline)?;
                }
                encoder.finish();
                (state.speed, state.power) = (encoder.speed, encoder.power);
                (encoder.circles, encoder.curves)
            };
            // Passes are interleaved per path like in Java, so the timeline
            // holds the whole sequence once.
            estimated += timeline.append(program, 1);
            let mut text = format!(
                "{} Vektorpfade · {} Durchgänge",
                paths.len(),
                part.sets[0].passes
            );
            if circles + curves > 0 {
                text += &format!(" · {circles} Kreisbefehle · {curves} Kurven");
            }
            description = text;
        }
        Operation::Engrave | Operation::Engrave3d => {
            let (pixmap, area) = render_raster(&source, part.operation)?;
            // RasterizableJobPart: start point plus image size, px2mm.
            let mm = |px: i32| px as f64 / RASTER_DPI * 25.4;
            bounds = Bounds {
                min: [mm(area.x), mm(area.y)],
                max: [
                    mm(area.x + pixmap.width() as i32),
                    mm(area.y + pixmap.height() as i32),
                ],
            };
            let raster = if part.operation == Operation::Engrave3d {
                Raster::engrave_3d(&pixmap, &part.raster)
            } else {
                Raster::engrave(&pixmap, &part.raster)
            };
            drop(pixmap);
            let color = operation_color(part.operation);
            draw_raster(preview, project, &raster, area, color);
            let raster_png = raster_preview(&raster, color)?;
            if first_in_file && raster.bits_per_pixel == 8 {
                // Job mode: eight bits per pixel ("engrave 3D").
                let mode = if project.rotary_axis { 0x10 } else { 0 };
                out.extend([0x1b, 0x4d, mode | 0x02]);
            }
            let mut lines = 0;
            for set in &part.sets {
                let mut program = Program::new(part.operation);
                let encoded;
                (encoded, lines) = raster_code(
                    project,
                    area,
                    part.operation,
                    &raster,
                    &part.raster,
                    set,
                    &mut program,
                )?;
                // Java adds one RasterPart per pass; each sends the colour
                // code and its settings and starts again to the right.
                for _ in 0..set.passes {
                    out.extend([0x1b, 0x4e, 0]); // colour code black
                    state.set(out, set.speed_percent, set.power_percent);
                    out.extend(&encoded);
                }
                program.raster_preview_png = raster_png.clone();
                program.raster_bounds_mm = Some([
                    mm(area.x),
                    mm(area.y),
                    raster.width as f64 * 25.4 / RASTER_DPI,
                    raster.height as f64 * 25.4 / RASTER_DPI,
                ]);
                estimated += timeline.append(program, set.passes);
            }
            if lines == 0 {
                return Err("SVG enthält keine dunklen Gravurpixel".into());
            }
            description = format!(
                "{lines} Gravurzeilen · 500 DPI · {} · {} · {} Durchgänge",
                if part.operation == Operation::Engrave3d {
                    "Graustufen (3D)"
                } else {
                    part.raster.dithering.title()
                },
                if part.raster.bidirectional {
                    "bidirektional"
                } else {
                    "einseitig"
                },
                part.sets[0].passes
            );
        }
    }
    let first = &part.sets[0];
    let step = PreparedStep {
        operation: part.operation,
        name: name.to_string(),
        description,
        estimated_seconds: estimated,
        power_percent: first.power_percent,
        speed_percent: first.speed_percent,
        passes: first.passes,
        parameter_sets: part.sets.len(),
    };
    Ok((step, bounds))
}

/// Area of the job content on the bed in mm, Y downwards.
#[derive(Clone, Copy, Debug, Default, PartialEq)]
struct Bounds {
    min: [f64; 2],
    max: [f64; 2],
}

impl Bounds {
    fn of_points(points: impl IntoIterator<Item = [f64; 2]>) -> Option<Self> {
        points.into_iter().fold(None, |b: Option<Self>, p| {
            Some(b.map_or(Self { min: p, max: p }, |b| {
                b.union(Self { min: p, max: p })
            }))
        })
    }
    fn union(self, other: Self) -> Self {
        Self {
            min: [self.min[0].min(other.min[0]), self.min[1].min(other.min[1])],
            max: [self.max[0].max(other.max[0]), self.max[1].max(other.max[1])],
        }
    }
}

/// `ShapeConverter.addShape` for one contour in mm: points in 500-DPI pixels
/// (`mm × dpi2dpmm(500)`), dropping line ends whose truncated pixel equals that
/// of the last kept point. The closing segment of a closed contour (Java
/// `SEG_CLOSE`) is always kept. `None` when no line remains.
fn shape_converter(path: &[geometry::Point]) -> Option<([f64; 2], Vec<[f64; 2]>)> {
    let px = |p: &geometry::Point| [p[0] * geometry::PX_PER_MM, p[1] * geometry::PX_PER_MM];
    let start = px(path.first()?);
    let closed = path.len() > 2 && path.first() == path.last();
    let truncated = |p: [f64; 2]| (p[0] as i32, p[1] as i32);
    let mut last = truncated(start);
    let mut moves = Vec::new();
    for (i, point) in path.iter().enumerate().skip(1) {
        let point = px(point);
        if closed && i == path.len() - 1 {
            moves.push(start);
        } else if truncated(point) != last {
            moves.push(point);
            last = truncated(point);
        }
    }
    (!moves.is_empty()).then_some((start, moves))
}

fn operation_color(operation: Operation) -> [u8; 3] {
    match operation {
        Operation::Cut => [220, 52, 52],
        Operation::Engrave => [35, 113, 210],
        Operation::Engrave3d => [222, 140, 30],
        Operation::Mark => [142, 68, 210],
    }
}

/// Engraved pixels in the operation colour, opacity = laser power.
fn raster_pixmap(raster: &Raster, color: [u8; 3]) -> Result<tiny_skia::Pixmap, String> {
    let mut mask = tiny_skia::Pixmap::new(raster.width.max(1) as u32, raster.height.max(1) as u32)
        .ok_or("Gravurvorschau konnte nicht erstellt werden")?;
    let width = raster.width;
    for (i, pixel) in mask.data_mut().chunks_exact_mut(4).enumerate() {
        let a = raster.intensity(i % width, i / width) as u32;
        // tiny-skia stores premultiplied colours.
        pixel.copy_from_slice(&[
            (color[0] as u32 * a / 255) as u8,
            (color[1] as u32 * a / 255) as u8,
            (color[2] as u32 * a / 255) as u8,
            a as u8,
        ]);
    }
    Ok(mask)
}

fn draw_raster(
    preview: &mut tiny_skia::Pixmap,
    project: &Project,
    raster: &Raster,
    area: RasterArea,
    color: [u8; 3],
) {
    let Ok(mask) = raster_pixmap(raster, color) else {
        return;
    };
    // Raster pixels → mm on the bed → preview pixels of the motif.
    let scale_x = preview.width() as f64 / project.width_mm as f64 / geometry::PX_PER_MM;
    let scale_y = preview.height() as f64 / project.height_mm as f64 / geometry::PX_PER_MM;
    let transform = tiny_skia::Transform::from_row(
        scale_x as f32,
        0.0,
        0.0,
        scale_y as f32,
        ((area.x as f64 - project.x_mm as f64 * geometry::PX_PER_MM) * scale_x) as f32,
        ((area.y as f64 - project.y_mm as f64 * geometry::PX_PER_MM) * scale_y) as f32,
    );
    preview.draw_pixmap(
        0,
        0,
        mask.as_ref(),
        &tiny_skia::PixmapPaint::default(),
        transform,
        None,
    );
}

/// Raster image for the timeline, at most 1600 pixels wide or high.
fn raster_preview(raster: &Raster, color: [u8; 3]) -> Result<Vec<u8>, String> {
    let mask = raster_pixmap(raster, color)?;
    let scale = (1600.0 / mask.width().max(mask.height()) as f32).min(1.0);
    let mut small = tiny_skia::Pixmap::new(
        ((mask.width() as f32 * scale).ceil() as u32).max(1),
        ((mask.height() as f32 * scale).ceil() as u32).max(1),
    )
    .ok_or("Gravurvorschau konnte nicht erstellt werden")?;
    let paint = tiny_skia::PixmapPaint {
        quality: tiny_skia::FilterQuality::Bilinear,
        ..Default::default()
    };
    small.draw_pixmap(
        0,
        0,
        mask.as_ref(),
        &paint,
        tiny_skia::Transform::from_scale(scale, scale),
        None,
    );
    small.encode_png().map_err(|e| e.to_string())
}

fn draw_contours(
    preview: &mut tiny_skia::Pixmap,
    project: &Project,
    paths: &[geometry::Contour],
    operation: Operation,
) {
    let sx = preview.width() as f32 / project.width_mm;
    let sy = preview.height() as f32 / project.height_mm;
    let mut paint = tiny_skia::Paint::default();
    let [r, g, b] = operation_color(operation);
    paint.set_color_rgba8(r, g, b, 255);
    paint.anti_alias = true;
    for path in paths {
        let mut builder = tiny_skia::PathBuilder::new();
        for (i, point) in path.iter().enumerate() {
            let x = (px(point[0]) as f32 * 25.4 / RASTER_DPI as f32 - project.x_mm) * sx;
            let y = (px(point[1]) as f32 * 25.4 / RASTER_DPI as f32 - project.y_mm) * sy;
            if i == 0 {
                builder.move_to(x, y);
            } else {
                builder.line_to(x, y);
            }
        }
        if let Some(path) = builder.finish() {
            preview.stroke_path(
                &path,
                &paint,
                &tiny_skia::Stroke {
                    width: 1.6,
                    ..Default::default()
                },
                tiny_skia::Transform::identity(),
                None,
            );
        }
    }
}

#[cfg(test)]
fn settings(out: &mut Vec<u8>, power: f32, speed: f32) {
    out.extend([0x1b, 0x53]);
    word(out, (speed * 10.0).clamp(1.0, 1000.0) as u16);
    out.extend([0x1b, 0x4a]);
    word(out, (power * 10.0).clamp(1.0, 1000.0) as u16);
}

fn header(project: &Project, overscan: f32, name: &str, bounds: Bounds) -> Vec<u8> {
    let mut out = b"LTT\x1bv\x01\x01\x02\x1bF".to_vec();
    // Prefix is part of the actual controller name, within its 15-byte limit.
    out.push(name.len() as u8);
    out.extend(name.as_bytes());
    let axis = Axis::of(project);
    if project.rotary_axis {
        out.extend([0x1b, 0x61, 0x15]); // temporary reference point: centre, stay
        out.extend([0x1b, 0x4d, 0x10]); // rotary axis, one bit per raster pixel
    } else {
        out.extend([0x1b, 0x61, 0]); // temporary reference point off
        out.extend([0x1b, 0x4d, 0]); // XY, one bit per raster pixel
    }
    out.extend([0x1b, 0x6c]);
    // Content bounds, then with engrave overscan as in Java setBoundingBox.
    for margin in [0.0, overscan as f64] {
        let xmin = (bounds.min[0] - margin).max(0.0);
        let xmax = (bounds.max[0] + margin).min(BED_WIDTH);
        let [ymin, ymax] = [bounds.min[1], bounds.max[1]];
        dword(&mut out, raw(xmin));
        dword(&mut out, axis.bounding(ymin));
        dword(&mut out, raw(xmax) - raw(xmin));
        dword(&mut out, axis.bounding(ymax) - axis.bounding(ymin));
    }
    out.extend([0x1b, 0x6e, 0, 0, 0x5d, 0xcf, 0, 0, 0x69, 0x56]);
    out.extend([0x1b, 0x4f, 0]); // no autorun
    out.extend([0x1b, 0x51, 0, 0]);
    out.extend([0x1b, 0x44, 8]); // 4000 / 8 = 500 DPI
    // Material radius in 0.01 mm; LibLaserCut sends 42 mm for XY jobs.
    out.extend([0x1b, 0x52]);
    let radius = if project.rotary_axis {
        project.rotary_diameter_mm / 2.0
    } else {
        42.0
    };
    word(&mut out, (radius as f64 / 0.01) as u16);
    out.extend([0x1b, 0x43, 0xc0]);
    out.extend([0x1b, 0x54]);
    out.extend((0..=15).map(|i| i * 0x11));
    out
}

/// Raster area of an engraving in 500-DPI pixels on the bed.
#[derive(Clone, Copy, Debug, PartialEq)]
struct RasterArea {
    x: i32,
    y: i32,
}

/// Renders the objects like VisiCut's RasterProfile/Raster3dProfile: only
/// their bounding box including strokes (`Helper.toRect`, which truncates x,
/// y, width and height separately), on white, without anti-aliasing for
/// engraving and with it for 3D engraving.
fn render_raster(
    project: &Project,
    operation: Operation,
) -> Result<(tiny_skia::Pixmap, RasterArea), String> {
    let smooth = operation == Operation::Engrave3d;
    let options = usvg::Options {
        shape_rendering: if smooth {
            usvg::ShapeRendering::GeometricPrecision
        } else {
            usvg::ShapeRendering::CrispEdges
        },
        text_rendering: if smooth {
            usvg::TextRendering::GeometricPrecision
        } else {
            usvg::TextRendering::OptimizeSpeed
        },
        // Java2D draws images with nearest-neighbour interpolation.
        image_rendering: usvg::ImageRendering::OptimizeSpeed,
        ..crate::svg::options()
    };
    let tree = usvg::Tree::from_str(&project.svg, &options).map_err(|e| e.to_string())?;
    let size = tree.size();
    let sx = f64::from(project.width_mm) / f64::from(size.width());
    let sy = f64::from(project.height_mm) / f64::from(size.height());
    let f = geometry::PX_PER_MM;
    let to_px_x = |u: f32| (sx * f64::from(u) + f64::from(project.x_mm)) * f;
    let to_px_y = |u: f32| (sy * f64::from(u) + f64::from(project.y_mm)) * f;
    let bbox = content_bounds(tree.root()).ok_or("SVG enthält keine dunklen Gravurpixel")?;
    let (x0, y0) = (to_px_x(bbox.left()), to_px_y(bbox.top()));
    let (x1, y1) = (to_px_x(bbox.right()), to_px_y(bbox.bottom()));
    let area = RasterArea {
        x: x0 as i32,
        y: y0 as i32,
    };
    let (width, height) = ((x1 - x0) as i32, (y1 - y0) as i32);
    if width <= 0 || height <= 0 {
        return Err("SVG enthält keine dunklen Gravurpixel".into());
    }
    if width as u64 * height as u64 > 40_000_000 {
        return Err("Gravur ist größer als 40 Millionen Pixel; Motiv verkleinern".into());
    }
    let mut pixmap = tiny_skia::Pixmap::new(width as u32, height as u32)
        .ok_or("Gravur konnte nicht gerendert werden")?;
    pixmap.fill(tiny_skia::Color::WHITE);
    // translate(-bb) · mm2laserPx · (translate(x, y) · scale(document → mm))
    resvg::render(
        &tree,
        tiny_skia::Transform::from_row(
            (sx * f) as f32,
            0.0,
            0.0,
            (sy * f) as f32,
            (f64::from(project.x_mm) * f - f64::from(area.x)) as f32,
            (f64::from(project.y_mm) * f - f64::from(area.y)) as f32,
        ),
        &mut pixmap.as_mut(),
    );
    Ok((pixmap, area))
}

/// Union of the objects' bounding boxes with strokes in document units, like
/// VisiCut's GraphicSet.getBoundingBox. usvg counts the position of an image
/// twice in its bounding box, so images use their size and transform.
fn content_bounds(group: &usvg::Group) -> Option<tiny_skia::Rect> {
    let mut bounds: Option<tiny_skia::Rect> = None;
    let mut add = |rect: Option<tiny_skia::Rect>| {
        if let Some(rect) = rect {
            bounds = Some(match bounds {
                Some(b) => tiny_skia::Rect::from_ltrb(
                    b.left().min(rect.left()),
                    b.top().min(rect.top()),
                    b.right().max(rect.right()),
                    b.bottom().max(rect.bottom()),
                )
                .unwrap_or(b),
                None => rect,
            });
        }
    };
    for node in group.children() {
        match node {
            usvg::Node::Group(group) => add(content_bounds(group)),
            usvg::Node::Image(image) => add(tiny_skia::Rect::from_xywh(
                0.0,
                0.0,
                image.size().width(),
                image.size().height(),
            )
            .and_then(|r| r.transform(image.abs_transform()))),
            node => add(Some(node.abs_stroke_bounding_box())),
        }
    }
    bounds
}

/// getEngraveShiftPixels: line offset in raster pixels for the given speed.
fn engrave_shift_pixels(speed: f32) -> f64 {
    let value = if speed < 10.0 {
        ENGRAVE_SHIFT[0]
    } else if speed >= 100.0 {
        ENGRAVE_SHIFT[9]
    } else {
        let low = (speed / 10.0) as usize - 1;
        let alpha = (speed - (low + 1) as f32 * 10.0) / 10.0;
        ENGRAVE_SHIFT[low] * (1.0 - alpha) + ENGRAVE_SHIFT[low + 1] * alpha
    };
    value as f64 * RASTER_DPI / MACHINE_DPI + 0.5
}

/// The raster lines of one RasterPart and their count.
fn raster_code(
    project: &Project,
    area: RasterArea,
    operation: Operation,
    raster: &Raster,
    options: &RasterSettings,
    set: &ParameterSet,
    program: &mut Program,
) -> Result<(Vec<u8>, usize), String> {
    let speed = set.speed_percent;
    let offset = engrave_shift_pixels(speed);
    let per_byte = (8 / raster.bits_per_pixel) as i32;
    // Shift by whole pixels for 3D rows so power values stay intact; for
    // 1-bit rows this equals the Java driver's bit shift.
    let shift = (-offset) as usize * raster.bits_per_pixel as usize;
    let spare = offset.abs().ceil() as i32;
    let (origin_x, origin_y) = (area.x, area.y);
    let max_x = px(BED_WIDTH);
    let overscan_bytes = (px(overscan(operation, speed) as f64) + per_byte - 1) / per_byte;
    let axis = Axis::of(project);
    let raster_speed = speed as f64 * ENGRAVE_SPEED_FACTOR;
    let mut count = 0;
    let mut encoded = Vec::new();
    let mut right = true;
    let rows: Box<dyn Iterator<Item = usize>> = if options.bottom_up {
        Box::new((0..raster.height).rev())
    } else {
        Box::new(0..raster.height)
    };
    for y in rows {
        let row = &raster.rows[y];
        let Some(first) = row.iter().position(|b| *b != 0) else {
            continue;
        };
        let last = row.iter().rposition(|b| *b != 0).unwrap();
        let mut start_x = origin_x + first as i32 * per_byte;
        let left = overscan_bytes.min(((start_x - spare) / per_byte).max(0)) as usize;
        start_x -= left as i32 * per_byte;
        let mut data = vec![0; left];
        data.extend_from_slice(&row[first..=last]);
        let right_bytes = overscan_bytes.min(
            ((max_x - spare - per_byte - start_x - data.len() as i32 * per_byte) / per_byte).max(0),
        ) as usize;
        data.resize(data.len() + right_bytes, 0);
        let pixels = data.len() as i32 * per_byte;
        if start_x < spare || start_x + pixels > max_x - spare {
            return Err(
                "Gravur zu nahe am Bettrand; bitte Motiv weiter nach innen verschieben".into(),
            );
        }
        let row_mm = (origin_y + y as i32) as f64 * 25.4 / RASTER_DPI;
        let left_mm = start_x as f64 * 25.4 / RASTER_DPI;
        let right_mm = (start_x + pixels) as f64 * 25.4 / RASTER_DPI;
        let (from, to) = if right {
            (left_mm, right_mm)
        } else {
            (right_mm, left_mm)
        };
        program.travel([from, row_mm]);
        program.dwell(0.1);
        program.line_timed(
            MotionKind::Raster,
            [to, row_mm],
            vector::cutting_time_mm(right_mm - left_mm, raster_speed),
        );
        if !right {
            // Right to left: reverse pixel order (bit order for 1-bit rows).
            data.reverse();
            if raster.bits_per_pixel == 1 {
                data.iter_mut().for_each(|b| *b = b.reverse_bits());
            }
        }
        left_shift(&mut data, shift);
        let compressed = compress(&data);
        encoded.extend(if right { [0x1b, 0x30] } else { [0x1b, 0x31] });
        dword(&mut encoded, compressed.len() as u32 + 8);
        pair(
            &mut encoded,
            axis,
            start_x + if right { 0 } else { pixels },
            origin_y + y as i32,
        );
        encoded.extend(compressed);
        count += 1;
        if options.bidirectional {
            right = !right;
        }
    }
    if encoded.len().saturating_mul(set.passes as usize) > 256 * 1024 * 1024 {
        return Err("Gravurjob ist größer als 256 MB".into());
    }
    Ok((encoded, count))
}

/// ByteArrayList.leftShiftBits: shift towards the start, filling with zeros.
fn left_shift(bytes: &mut [u8], bits: usize) {
    let whole = (bits / 8).min(bytes.len());
    bytes.rotate_left(whole);
    let n = bytes.len();
    bytes[n - whole..].fill(0);
    let bits = bits % 8;
    if bits == 0 {
        return;
    }
    for i in 0..bytes.len() {
        bytes[i] = (bytes[i] << bits) | (bytes.get(i + 1).copied().unwrap_or(0) >> (8 - bits));
    }
}

fn compress(bytes: &[u8]) -> Vec<u8> {
    let mut out = Vec::new();
    let mut i = 0;
    while i < bytes.len() {
        let value = bytes[i];
        let mut run = 1;
        while i + run < bytes.len() && bytes[i + run] == value && run < 63 {
            run += 1;
        }
        if run > 1 || value >= 0xc0 {
            out.push(0xc0 + run as u8);
        }
        out.push(value);
        i += run;
    }
    out
}

pub fn transmit_jobs(host: &str, port: u16, jobs: &[OutputJob]) -> Result<Vec<String>, String> {
    transmit_jobs_with(jobs, |bytes| transmit(host, port, bytes))
}

fn transmit_jobs_with(
    jobs: &[OutputJob],
    mut send: impl FnMut(&[u8]) -> Result<(), String>,
) -> Result<Vec<String>, String> {
    let mut sent = Vec::new();
    for job in jobs {
        if let Err(error) = send(&job.bytes) {
            return Err(format!(
                "Übertragung von {} fehlgeschlagen: {}\nBereits übertragen: {}. Dieser Job kann teilweise am Gerät liegen. Weitere Aufträge wurden nicht gesendet; vor erneutem Senden Gerätespeicher prüfen.",
                job.name,
                error,
                if sent.is_empty() {
                    "keine".into()
                } else {
                    sent.join(", ")
                }
            ));
        }
        sent.push(job.name.clone());
    }
    Ok(sent)
}

pub fn transmit(host: &str, port: u16, bytes: &[u8]) -> Result<(), String> {
    if host.trim().is_empty() || port == 0 {
        return Err("Hostname und Port müssen gesetzt sein".into());
    }
    let addresses: Vec<_> = (host, port)
        .to_socket_addrs()
        .map_err(|e| format!("Hostname nicht erreichbar: {e}"))?
        .collect();
    let mut connection = None;
    for address in addresses {
        if let Ok(stream) = TcpStream::connect_timeout(&address, Duration::from_secs(3)) {
            connection = Some(stream);
            break;
        }
    }
    let mut stream = connection.ok_or("Keine Verbindung zum LTT möglich")?;
    stream
        .set_write_timeout(Some(Duration::from_secs(10)))
        .map_err(|e| e.to_string())?;
    stream
        .write_all(bytes)
        .map_err(|e| format!("Übertragung fehlgeschlagen; Teiljob könnte am Gerät liegen: {e}"))?;
    stream.flush().map_err(|e| e.to_string())?;
    stream
        .shutdown(Shutdown::Write)
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    fn sample() -> Project {
        Project {
            svg: include_str!("../../examples/demo.svg").into(),
            ..Default::default()
        }
    }
    fn mixed() -> Project {
        use crate::project::JobStep;
        Project {
            width_mm: 30.0, height_mm: 20.0,
            svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="30mm" height="20mm" viewBox="0 0 30 20"><rect id="cut" x="1" y="1" width="28" height="18" fill="none" stroke="red"/><rect id="engrave" x="5" y="5" width="8" height="5" fill="black"/><text x="15" y="10">ignored</text></svg>"#.into(),
            steps: vec![
                JobStep { objects: vec![0], power_percent: 50.0, speed_percent: 8.0, passes: 2, ..JobStep::new(Operation::Cut) },
                JobStep { objects: vec![1], power_percent: 20.0, speed_percent: 70.0, passes: 1, ..JobStep::new(Operation::Engrave) },
            ], ..Default::default()
        }
    }

    #[test]
    fn mixed_selection_encodes_separate_jobs_engrave_before_cut_with_separate_settings() {
        let p = mixed();
        let job = prepare(&p).unwrap();
        assert_eq!(job.jobs.len(), 2);
        assert_eq!(job.jobs[0].operation, Operation::Engrave);
        assert_eq!(job.jobs[1].operation, Operation::Cut);
        assert!(job.jobs[0].bytes.windows(2).any(|b| b == [0x1b, 0x30]));
        assert!(!job.jobs[0].bytes.windows(2).any(|b| b == [0x1b, 0x56]));
        assert!(job.jobs[1].bytes.windows(2).any(|b| b == [0x1b, 0x56]));
        assert!(job.steps[1].description.contains("1 Vektorpfade"));
        assert!(
            job.jobs[0]
                .bytes
                .windows(4)
                .any(|b| b == [0x1b, 0x53, 2, 188])
        );
        assert!(
            job.jobs[1]
                .bytes
                .windows(4)
                .any(|b| b == [0x1b, 0x53, 0, 80])
        );
        for output in &job.jobs {
            assert_framing(&output.bytes);
        }
        assert!(
            (job.estimated_seconds - job.steps.iter().map(|s| s.estimated_seconds).sum::<f64>())
                .abs()
                < 0.001
        );
        let image = tiny_skia::Pixmap::decode_png(&job.preview_png).unwrap();
        assert!(
            image
                .pixels()
                .iter()
                .any(|p| p.red() > 150 && p.blue() < 100)
        );
        assert!(
            image
                .pixels()
                .iter()
                .any(|p| p.blue() > 150 && p.red() < 100)
        );
        let loaded: Project = serde_json::from_str(&serde_json::to_string(&p).unwrap()).unwrap();
        let reloaded = prepare(&loaded).unwrap();
        for (a, b) in job.jobs.iter().zip(&reloaded.jobs) {
            assert_eq!(a.bytes, b.bytes);
        }
    }

    #[test]
    fn parameter_sets_of_a_cut_step_share_one_cutting_order() {
        use crate::project::JobStep;
        let mut cut = JobStep {
            objects: vec![0, 1],
            power_percent: 50.0,
            speed_percent: 8.0,
            ..JobStep::new(Operation::Cut)
        };
        cut.additional.push(ParameterSet {
            power_percent: 20.0,
            speed_percent: 30.0,
            passes: 1,
        });
        let project = Project {
            width_mm: 30.0,
            height_mm: 20.0,
            svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="30mm" height="20mm" viewBox="0 0 30 20"><rect x="1" y="1" width="28" height="18" fill="none" stroke="red"/><rect x="5" y="5" width="8" height="5" fill="none" stroke="red"/></svg>"#.into(),
            steps: vec![cut],
            ..Default::default()
        };
        let job = prepare(&project).unwrap();
        // The inner rectangle is cut first, with both sets, then the outline with both sets.
        let speeds: Vec<u16> = job.jobs[0]
            .bytes
            .windows(4)
            .filter(|w| w[..2] == [0x1b, 0x53])
            .map(|w| u16::from_be_bytes([w[2], w[3]]))
            .collect();
        // (50 %, 8 %) comes first because of the HashMap order of the Java
        // properties (bucket 4 before 12), not because it is the first set.
        assert_eq!(speeds, vec![80, 300, 80, 300]);
        assert_eq!(job.timeline.programs.len(), 1);
        assert_eq!(job.steps[0].parameter_sets, 2);
    }

    fn three_modes() -> Project {
        let mut p = mixed();
        p.name = "Muster mit sehr langem Namen äöü".into();
        p.svg = p.svg.replace(
            "<text x=\"15\" y=\"10\">ignored</text>",
            "<path d=\"M15 10L25 10\" stroke=\"black\"/>",
        );
        p.steps.push(crate::project::JobStep {
            objects: vec![2],
            power_percent: 5.0,
            speed_percent: 60.0,
            passes: 2,
            ..crate::project::JobStep::new(Operation::Mark)
        });
        p
    }

    #[test]
    fn three_modes_have_distinct_prefixed_files_and_ordered_simulation() {
        let p = three_modes();
        let job = prepare(&p).unwrap();
        assert_eq!(job.jobs.len(), 3);
        for (i, prefix) in ["Engrav_", "Mark_", "Cut_"].iter().enumerate() {
            let output = &job.jobs[i];
            assert!(output.name.starts_with(prefix));
            assert!(output.name.len() <= 15);
            assert!(output.name.is_ascii());
            assert_eq!(output.bytes[10] as usize, output.name.len());
            assert_eq!(
                &output.bytes[11..11 + output.name.len()],
                output.name.as_bytes()
            );
            assert_framing(&output.bytes);
        }
        assert!(
            job.jobs[1]
                .bytes
                .windows(4)
                .any(|b| b == [0x1b, 0x4a, 0, 50])
        );
        assert_eq!(job.timeline.programs.len(), 3);
        // Engrave, mark and cut once each: vector passes are interleaved per
        // path like in Java and belong to the single run of their program.
        assert_eq!(job.timeline.runs.len(), 3);
        assert_eq!(job.timeline.programs[1].operation, Operation::Mark);
        assert!(
            job.timeline.programs[1]
                .motions
                .iter()
                .any(|m| m.kind == MotionKind::Mark)
        );
        for run in &job.timeline.runs {
            assert!(run.start_seconds <= run.entry_end_seconds);
            assert!(run.entry_end_seconds <= run.end_seconds);
        }
        assert_eq!(
            job.estimated_seconds,
            job.timeline.runs.last().unwrap().end_seconds
        );
        let mark = job.timeline.programs[1]
            .motions
            .iter()
            .find(|m| m.kind == MotionKind::Mark)
            .unwrap();
        assert!((mark.to_mm[0] - mark.from_mm[0] - 10.0).abs() < 0.06);
        let saved: Project = serde_json::from_value(serde_json::to_value(&p).unwrap()).unwrap();
        let again = prepare(&saved).unwrap();
        for (a, b) in job.jobs.iter().zip(&again.jobs) {
            assert_eq!(a.bytes, b.bytes);
        }
        let mut mark_only = p.clone();
        mark_only.steps.retain(|s| s.operation == Operation::Mark);
        assert_eq!(prepare(&mark_only).unwrap().jobs.len(), 1);
        mark_only.steps[0].objects.clear();
        assert!(prepare(&mark_only).is_err());
    }

    #[test]
    fn batch_transfer_stops_on_error_and_reports_already_sent_names() {
        let job = prepare(&three_modes()).unwrap();
        let mut calls = 0;
        let error = transmit_jobs_with(&job.jobs, |_| {
            calls += 1;
            if calls == 2 {
                Err("Test-Verbindungsabbruch".into())
            } else {
                Ok(())
            }
        })
        .unwrap_err();
        assert_eq!(calls, 2);
        assert!(error.contains(&job.jobs[0].name));
        assert!(error.contains(&job.jobs[1].name));
        assert!(!error.contains(&job.jobs[2].name));
        let sent = transmit_jobs_with(&job.jobs, |_| Ok(())).unwrap();
        assert_eq!(
            sent,
            job.jobs.iter().map(|j| j.name.clone()).collect::<Vec<_>>()
        );
    }

    #[test]
    fn tcp_batch_uses_three_connections_with_exact_framed_payloads() {
        use std::io::Read;
        let job = prepare(&three_modes()).unwrap();
        let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        listener.set_nonblocking(true).unwrap();
        let port = listener.local_addr().unwrap().port();
        let receiver = std::thread::spawn(move || {
            let deadline = std::time::Instant::now() + Duration::from_secs(10);
            let mut payloads = Vec::new();
            while payloads.len() < 3 {
                match listener.accept() {
                    Ok((mut stream, _)) => {
                        // On macOS accepted sockets inherit the listener's
                        // non-blocking mode; the read below must block.
                        stream.set_nonblocking(false).unwrap();
                        stream
                            .set_read_timeout(Some(Duration::from_secs(3)))
                            .unwrap();
                        let mut bytes = Vec::new();
                        stream.read_to_end(&mut bytes).unwrap();
                        payloads.push(bytes);
                    }
                    Err(error) if error.kind() == std::io::ErrorKind::WouldBlock => {
                        assert!(
                            std::time::Instant::now() < deadline,
                            "Missing batch connection"
                        );
                        std::thread::sleep(Duration::from_millis(10));
                    }
                    Err(error) => panic!("{error}"),
                }
            }
            payloads
        });
        let sent = transmit_jobs("127.0.0.1", port, &job.jobs).unwrap();
        assert_eq!(sent.len(), 3);
        let received = receiver.join().unwrap();
        assert_eq!(received.len(), 3);
        for (bytes, output) in received.iter().zip(&job.jobs) {
            assert_eq!(*bytes, output.bytes);
        }
    }

    #[test]
    fn estimates_quantized_cut_distance_speed_and_passes() {
        let mut p = Project { x_mm: 0.0, y_mm: 0.0, width_mm: 100.0, height_mm: 10.0,
            speed_percent: 10.0,
            svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="10mm" viewBox="0 0 100 10"><path d="M0 0L100 0" stroke="black"/></svg>"#.into(), ..Default::default() };
        // Java estimate: distance plus acceleration distance v²/a at 2000 mm/s².
        let expected = |v: f64| (100.0 + (v * v / 2000.0).sqrt()) / v;
        let one = prepare(&p).unwrap().estimated_seconds;
        assert!((one - expected(33.8677)).abs() < 0.003, "{one}");
        p.speed_percent = 20.0;
        let faster = prepare(&p).unwrap().estimated_seconds;
        assert!((faster - expected(67.7354)).abs() < 0.003, "{faster}");
        // Java joins the line and its second pass into one path there and
        // back: no travel in between, the reversal is a corner.
        p.passes = 2;
        let twice = prepare(&p).unwrap().estimated_seconds;
        assert!((twice - 2.0 * faster).abs() < 0.003, "{twice}");
    }

    #[test]
    fn raster_time_scales_with_passes_and_includes_line_overhead() {
        let mut p = mixed();
        p.steps.remove(0);
        let one = prepare(&p).unwrap();
        assert!(one.estimated_seconds > 5.0 / 25.4 * RASTER_DPI * 0.1);
        p.steps[0].passes = 3;
        let three = prepare(&p).unwrap();
        let program = &three.timeline.programs[0];
        let return_time = (program.end_mm[0] - program.start_mm[0])
            .hypot(program.end_mm[1] - program.start_mm[1])
            / 338.677;
        assert!(
            (three.estimated_seconds
                - one.estimated_seconds
                - 2.0 * (program.duration_seconds + return_time))
                .abs()
                < 0.001
        );
        assert_eq!(
            one.timeline.programs[0].motions.len(),
            program.motions.len()
        );
    }

    #[test]
    fn rejects_empty_duplicate_stale_and_invalid_step_selections() {
        let mut p = mixed();
        p.steps[0].objects.clear();
        p.steps[1].objects.clear();
        assert!(prepare(&p).err().unwrap().contains("Keine Objekte"));
        // As in VisiCut, one object may be processed by several steps.
        let mut p = mixed();
        p.steps[0].objects = vec![1];
        assert_eq!(prepare(&p).unwrap().jobs.len(), 2);
        let mut p = mixed();
        p.steps[0].objects = vec![1000];
        assert!(prepare(&p).is_err());
        let mut p = mixed();
        p.steps[1].speed_percent = 0.0;
        assert!(prepare(&p).is_err());
    }

    #[test]
    fn framing_checksum_length_and_no_autorun() {
        let prepared = prepare(&sample()).unwrap();
        assert_framing(&prepared.jobs[0].bytes);
    }

    fn assert_framing(bytes: &[u8]) {
        assert!(bytes.starts_with(b"LTT\x1bv\x01\x01\x02"));
        assert!(bytes.windows(3).any(|b| b == [0x1b, 0x4f, 0]));
        let n = bytes.len();
        assert_eq!(
            u32::from_be_bytes(bytes[n - 4..].try_into().unwrap()) as usize,
            n
        );
        assert_eq!(
            u16::from_be_bytes(bytes[n - 6..n - 4].try_into().unwrap()),
            bytes[..n - 6]
                .iter()
                .fold(0u16, |s, b| s.wrapping_add(*b as u16))
        );
        assert_eq!(&bytes[n - 10..n - 6], b"\x1bBYE");
    }
    #[test]
    fn coordinates_match_java_driver_conversion() {
        let mut bytes = Vec::new();
        pair(
            &mut bytes,
            Axis {
                rotary_radius_mm: None,
            },
            196,
            393,
        );
        // Java casts bedHeight*4000/25.4 to int, scales 500-DPI coordinates by eight.
        assert_eq!(bytes, [0, 0, 6, 32, 0, 1, 100, 208]);
    }
    #[test]
    fn compression_escapes_magic_and_splits_long_runs() {
        assert_eq!(compress(&[1, 1, 1, 0xc0, 2]), [0xc3, 1, 0xc1, 0xc0, 2]);
        assert_eq!(compress(&[0xff; 64]), [0xff, 0xff, 0xc1, 0xff]);
    }

    #[test]
    fn encodes_profile_speed_directly_as_tenths_of_percent() {
        let p = Project {
            speed_percent: 9.0,
            ..sample()
        };
        let prepared = prepare(&p).unwrap();
        let bytes = &prepared.jobs[0].bytes;
        assert!(bytes.windows(4).any(|b| b == [0x1b, 0x53, 0, 90]));
        let mut settings_bytes = Vec::new();
        settings(&mut settings_bytes, 20.0, 100.0);
        assert_eq!(&settings_bytes[..4], [0x1b, 0x53, 3, 232]);
    }
    #[test]
    fn rejects_zero_power_and_wrong_machine_dimensions() {
        let mut p = sample();
        p.power_percent = 0.0;
        assert!(prepare(&p).is_err());
        p.power_percent = 20.0;
        p.bed_height_mm = 400.0;
        assert!(prepare(&p).is_err());
    }
    #[test]
    fn raster_job_contains_compressed_scanlines() {
        let p = Project { operation: Operation::Engrave, width_mm: 10.0, height_mm: 6.0,
            svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="100" height="60"><rect x="20" y="20" width="60" height="20" fill="black"/></svg>"#.into(), ..sample() };
        let prepared = prepare(&p).unwrap();
        assert!(prepared.jobs[0].bytes.windows(2).any(|b| b == [0x1b, 0x30]));
        assert!(prepared.description.contains("Gravurzeilen"));
    }
    #[test]
    fn rotary_axis_uses_rotation_steps_centre_reference_and_radius() {
        fn has(bytes: &[u8], pattern: &[u8]) -> bool {
            bytes.windows(pattern.len()).any(|b| b == pattern)
        }
        let xy = prepare(&sample()).unwrap();
        assert!(has(&xy.jobs[0].bytes, &[0x1b, 0x61, 0, 0x1b, 0x4d, 0]));
        assert!(has(&xy.jobs[0].bytes, &[0x1b, 0x52, 0x10, 0x68]));
        assert!(xy.warnings.is_empty());

        let p = Project {
            rotary_axis: true,
            rotary_diameter_mm: 100.0,
            ..sample()
        };
        let rotary = prepare(&p).unwrap();
        let bytes = &rotary.jobs[0].bytes;
        assert!(has(bytes, &[0x1b, 0x61, 0x15, 0x1b, 0x4d, 0x10]));
        // Radius 50 mm in 0.01 mm.
        assert!(has(bytes, &[0x1b, 0x52, 0x13, 0x88]));
        // Bounding box of the contours; Y as rotation steps on a 314.16 mm
        // circumference instead of 4000-DPI machine units.
        let b =
            Bounds::of_points(geometry::contours(&p).unwrap().iter().flatten().copied()).unwrap();
        let axis = Axis::of(&p);
        assert!(axis.bounding(b.max[1]) < raw(b.max[1]));
        let mut bbox = vec![0x1b, 0x6c];
        dword(&mut bbox, raw(b.min[0]));
        dword(&mut bbox, axis.bounding(b.min[1]));
        dword(&mut bbox, raw(b.max[0]) - raw(b.min[0]));
        dword(&mut bbox, axis.bounding(b.max[1]) - axis.bounding(b.min[1]));
        assert!(has(bytes, &bbox));
        let mut absolute = Vec::new();
        pair(&mut absolute, Axis::of(&p), 100, 500);
        assert_eq!(absolute[4..], 517i32.to_be_bytes()); // 25.4 mm
        assert_eq!(Axis::of(&p).relative(-10), -10);
        assert!(rotary.warnings[0].contains("Adjust rotary temp"));
        assert_framing(bytes);

        let too_small = Project {
            rotary_diameter_mm: 10.0,
            ..p.clone()
        };
        assert!(prepare(&too_small).err().unwrap().contains("Umfang"));
        let invalid = Project {
            rotary_diameter_mm: 4.0,
            ..p
        };
        assert!(prepare(&invalid).err().unwrap().contains("Durchmesser"));
    }
    fn has(bytes: &[u8], pattern: &[u8]) -> bool {
        bytes.windows(pattern.len()).any(|b| b == pattern)
    }

    #[test]
    fn engraving_options_direction_order_3d_and_parameter_sets() {
        use crate::project::{JobStep, ParameterSet};
        use crate::raster::{Dithering, RasterSettings};
        let svg = r##"<svg xmlns="http://www.w3.org/2000/svg" width="20mm" height="4mm" viewBox="0 0 20 4"><defs><linearGradient id="g"><stop offset="0" stop-color="#000"/><stop offset="1" stop-color="#fff"/></linearGradient></defs><rect width="20" height="4" fill="url(#g)"/></svg>"##;
        let base = Project {
            svg: svg.into(),
            width_mm: 20.0,
            height_mm: 4.0,
            x_mm: 100.0,
            y_mm: 100.0,
            ..Default::default()
        };
        let step = |operation, raster| JobStep {
            objects: vec![0],
            raster,
            ..JobStep::new(operation)
        };
        let one_way = RasterSettings {
            dithering: Dithering::FloydSteinberg,
            bidirectional: false,
            ..Default::default()
        };
        let p = Project {
            steps: vec![step(Operation::Engrave, one_way)],
            ..base.clone()
        };
        let bytes = &prepare(&p).unwrap().jobs[0].bytes;
        assert!(has(bytes, &[0x1b, 0x30]) && !has(bytes, &[0x1b, 0x31]));

        let both = RasterSettings {
            bidirectional: true,
            ..one_way
        };
        let p = Project {
            steps: vec![step(Operation::Engrave, both)],
            ..base.clone()
        };
        let job = prepare(&p).unwrap();
        assert!(has(&job.jobs[0].bytes, &[0x1b, 0x31]));
        let raster: Vec<_> = job.timeline.programs[0]
            .motions
            .iter()
            .filter(|m| m.kind == MotionKind::Raster)
            .collect();
        assert!(
            raster[0].to_mm[0] > raster[0].from_mm[0] && raster[1].to_mm[0] < raster[1].from_mm[0]
        );

        let up = RasterSettings {
            bottom_up: true,
            ..one_way
        };
        let p = Project {
            steps: vec![step(Operation::Engrave, up)],
            ..base.clone()
        };
        let job = prepare(&p).unwrap();
        let rows: Vec<f64> = job.timeline.programs[0]
            .motions
            .iter()
            .filter(|m| m.kind == MotionKind::Raster)
            .map(|m| m.from_mm[1])
            .collect();
        assert!(rows.windows(2).all(|w| w[1] < w[0]));

        // 3D: own file, eight bits per pixel, power ramps with darkness.
        let mut deep = step(Operation::Engrave3d, RasterSettings::default());
        deep.additional.push(ParameterSet {
            power_percent: 40.0,
            speed_percent: 50.0,
            passes: 2,
        });
        let p = Project {
            steps: vec![step(Operation::Engrave, one_way), deep],
            ..base.clone()
        };
        let job = prepare(&p).unwrap();
        assert_eq!(job.jobs.len(), 2);
        assert!(job.jobs[1].name.starts_with("Eng3D_"));
        let bytes = &job.jobs[1].bytes;
        assert!(has(bytes, &[0x1b, 0x4d, 0x02]));
        assert!(has(bytes, &[0x1b, 0x53, 0x03, 0xe8]) && has(bytes, &[0x1b, 0x53, 0x01, 0xf4]));
        assert_eq!(job.steps[1].parameter_sets, 2);
        // One program per parameter set; the second runs twice.
        assert_eq!(job.timeline.programs.len(), 3);
        assert_eq!(job.timeline.runs.len(), 4);
        assert!(job.steps[1].description.contains("3D"));
        assert_framing(bytes);
    }

    #[test]
    fn rule_steps_select_by_colour_with_rest_and_ignore() {
        let svg = r##"<svg xmlns="http://www.w3.org/2000/svg" width="30mm" height="20mm" viewBox="0 0 30 20"><rect x="1" y="1" width="28" height="18" fill="none" stroke="#ff0000"/><rect x="5" y="5" width="8" height="5" fill="black"/><path d="M15 10L25 10" stroke="#00ff00"/><path d="M15 15L25 15" stroke="#0000ff"/></svg>"##;
        let mut p = Project {
            svg: svg.into(),
            width_mm: 30.0,
            height_mm: 20.0,
            ..Default::default()
        };
        let fau = &crate::mapping::predefined()[0];
        for (operation, filters, rest) in &fau.rules {
            p.steps.push(crate::project::JobStep {
                filters: filters.clone(),
                rest: *rest,
                ..crate::project::JobStep::new(*operation)
            });
        }
        p.ignore_filters = fau.ignore.clone();
        let job = prepare(&p).unwrap();
        let operations: Vec<_> = job.jobs.iter().map(|j| j.operation).collect();
        assert_eq!(
            operations,
            [Operation::Engrave, Operation::Mark, Operation::Cut]
        );
        assert!(job.steps[2].description.contains("1 Vektorpfade"));
        let saved: Project = serde_json::from_str(&serde_json::to_string(&p).unwrap()).unwrap();
        assert_eq!(prepare(&saved).unwrap().jobs[2].bytes, job.jobs[2].bytes);
    }

    #[test]
    fn tcp_transfer_delivers_exact_bytes() {
        use std::io::Read;
        let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        let port = listener.local_addr().unwrap().port();
        let receiver = std::thread::spawn(move || {
            let (mut stream, _) = listener.accept().unwrap();
            stream
                .set_read_timeout(Some(Duration::from_secs(3)))
                .unwrap();
            let mut bytes = Vec::new();
            stream.read_to_end(&mut bytes).unwrap();
            bytes
        });
        transmit("127.0.0.1", port, b"test job").unwrap();
        assert_eq!(receiver.join().unwrap(), b"test job");
    }
}
