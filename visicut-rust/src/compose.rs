//! Text and shape tools, copies and simple nesting without an external
//! editor. The current motif is embedded unchanged as a nested `<svg>` in a
//! new document whose user units are bed millimetres, so object numbers,
//! assignments and rules keep working; new elements are appended after it.
use crate::{
    project::{JobStep, Operation, Project},
    selection, svg,
    svg_import::value_range,
};
use resvg::{tiny_skia, usvg};
use serde::{Deserialize, Serialize};
use std::fmt::Write;

/// Largest composed document, like [`crate::svg::render`].
const MAX_SVG_BYTES: usize = 20 * 1024 * 1024;
/// Most copies of one array or nesting run.
pub const MAX_COPIES: usize = 400;

/// Horizontal alignment of text lines.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum Align {
    Left,
    #[default]
    Center,
    Right,
}

/// A new element; sizes in millimetres.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(tag = "kind")]
pub enum Shape {
    Rectangle {
        width_mm: f64,
        height_mm: f64,
        /// Corner radius (0 = sharp corners).
        corner_mm: f64,
    },
    Ellipse {
        width_mm: f64,
        height_mm: f64,
    },
    /// Regular polygon on a circle of this diameter, one corner at the top.
    Polygon {
        sides: u32,
        diameter_mm: f64,
    },
    Star {
        points: u32,
        diameter_mm: f64,
        /// Inner radius in percent of the outer radius.
        inner_percent: f64,
    },
    /// Text in a font of the application, converted to outlines. Lines are
    /// separated by line breaks.
    Text {
        text: String,
        font: String,
        /// Font size (em height) in mm.
        size_mm: f64,
        bold: bool,
        italic: bool,
        align: Align,
        /// Line distance in multiples of the font size.
        line_spacing: f64,
    },
}

/// A new element at a bed position (top left of its bounding box). The
/// operation sets the colour: engrave black fill, cut red and mark blue
/// outlines, as in VisiCut's default mappings.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct NewObject {
    pub shape: Shape,
    pub operation: Operation,
    pub x_mm: f64,
    pub y_mm: f64,
}

/// A rectangular sign: plate, mounting holes and centred text.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Sign {
    pub x_mm: f64,
    pub y_mm: f64,
    pub width_mm: f64,
    pub height_mm: f64,
    pub corner_mm: f64,
    /// 0, 2 (left and right) or 4 (corners).
    pub holes: u32,
    pub hole_diameter_mm: f64,
    /// Distance of the hole centres from the plate edge.
    pub hole_inset_mm: f64,
    pub text: String,
    pub font: String,
    pub size_mm: f64,
    pub bold: bool,
    /// How the text is made: engraved, marked or cut out.
    pub text_operation: Operation,
    /// Engraved frame inside the plate edge at this distance (0 = none).
    pub frame_inset_mm: f64,
}

impl Default for Sign {
    fn default() -> Self {
        Self {
            x_mm: 10.0,
            y_mm: 10.0,
            width_mm: 150.0,
            height_mm: 50.0,
            corner_mm: 5.0,
            holes: 2,
            hole_diameter_mm: 4.0,
            hole_inset_mm: 8.0,
            text: "Schild".into(),
            font: "sans-serif".into(),
            size_mm: 18.0,
            bold: true,
            text_operation: Operation::Engrave,
            frame_inset_mm: 0.0,
        }
    }
}

/// Where and how copies of the parts are nested.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct NestSettings {
    /// Sheet on the bed: x, y, width, height in mm.
    pub area_mm: [f64; 4],
    /// Distance between parts.
    pub gap_mm: f64,
    /// Copies of every part.
    pub quantity: u32,
    /// Parts may be turned by 90°.
    pub rotate: bool,
}

impl Default for NestSettings {
    fn default() -> Self {
        Self {
            area_mm: [5.0, 5.0, 990.0, 590.0],
            gap_mm: 3.0,
            quantity: 1,
            rotate: true,
        }
    }
}

/// Outcome of a nesting run.
pub struct Nested {
    pub project: Project,
    pub placed: usize,
    pub missing: usize,
}

/// Font families available for text, generic families first.
pub fn font_families() -> Vec<String> {
    let mut names: Vec<String> = svg::fonts()
        .faces()
        .flat_map(|face| face.families.iter().map(|(name, _)| name.clone()))
        .collect();
    names.sort_by_key(|n| n.to_lowercase());
    names.dedup();
    ["sans-serif", "serif", "monospace"]
        .into_iter()
        .map(String::from)
        .chain(names)
        .collect()
}

/// The elements of a sign, ready for [`add_objects`].
pub fn sign(sign: &Sign) -> Result<Vec<NewObject>, String> {
    if !(sign.width_mm > 0.0 && sign.height_mm > 0.0) {
        return Err("Schild braucht Breite und Höhe".into());
    }
    let (x, y, w, h) = (sign.x_mm, sign.y_mm, sign.width_mm, sign.height_mm);
    let mut objects = vec![NewObject {
        shape: Shape::Rectangle {
            width_mm: w,
            height_mm: h,
            corner_mm: sign.corner_mm,
        },
        operation: Operation::Cut,
        x_mm: x,
        y_mm: y,
    }];
    if sign.frame_inset_mm > 0.0 {
        let inset = sign.frame_inset_mm;
        if 2.0 * inset >= w.min(h) {
            return Err("Rahmenabstand ist zu groß für das Schild".into());
        }
        objects.push(NewObject {
            shape: Shape::Rectangle {
                width_mm: w - 2.0 * inset,
                height_mm: h - 2.0 * inset,
                corner_mm: (sign.corner_mm - inset).max(0.0),
            },
            operation: Operation::Mark,
            x_mm: x + inset,
            y_mm: y + inset,
        });
    }
    let (d, inset) = (sign.hole_diameter_mm, sign.hole_inset_mm);
    let centres: Vec<(f64, f64)> = match sign.holes {
        0 => Vec::new(),
        2 => vec![(x + inset, y + h / 2.0), (x + w - inset, y + h / 2.0)],
        4 => vec![
            (x + inset, y + inset),
            (x + w - inset, y + inset),
            (x + inset, y + h - inset),
            (x + w - inset, y + h - inset),
        ],
        _ => return Err("Ein Schild hat 0, 2 oder 4 Löcher".into()),
    };
    if !centres.is_empty()
        && (d <= 0.0
            || inset < d / 2.0
            || 2.0 * inset >= w
            || (sign.holes == 4 && 2.0 * inset >= h))
    {
        return Err("Löcher passen nicht auf das Schild".into());
    }
    for (cx, cy) in centres {
        objects.push(NewObject {
            shape: Shape::Ellipse {
                width_mm: d,
                height_mm: d,
            },
            operation: Operation::Cut,
            x_mm: cx - d / 2.0,
            y_mm: cy - d / 2.0,
        });
    }
    if !sign.text.trim().is_empty() {
        let shape = Shape::Text {
            text: sign.text.clone(),
            font: sign.font.clone(),
            size_mm: sign.size_mm,
            bold: sign.bold,
            italic: false,
            align: Align::Center,
            line_spacing: 1.2,
        };
        // Centre the text block on the plate.
        let (_, bounds) = outline(&shape)?;
        objects.push(NewObject {
            shape,
            operation: sign.text_operation,
            x_mm: x + (w - bounds[2]) / 2.0,
            y_mm: y + (h - bounds[3]) / 2.0,
        });
    }
    Ok(objects)
}

/// Adds elements to the motif. In the individual assignment the new objects
/// join the step of their operation (a new step if there is none); rules
/// match them by their colour. Returns the project and the new object ids.
pub fn add_objects(
    project: &Project,
    objects: &[NewObject],
) -> Result<(Project, Vec<usize>), String> {
    if objects.is_empty() {
        return Err("Keine Elemente zum Einfügen".into());
    }
    let existing = selection::objects(&project.svg)?.len();
    let mut body = String::new();
    let mut bounds = motif_bounds(project);
    if !project.svg.is_empty() {
        body += &embed(project, None, None)?;
    }
    for (i, object) in objects.iter().enumerate() {
        let (d, b) = outline(&object.shape)?;
        let (dx, dy) = (object.x_mm - b[0], object.y_mm - b[1]);
        let mut moved = String::new();
        transform_path(&d, dx, dy, &mut moved);
        let (fill, stroke) = style(object.operation);
        write!(
            body,
            r#"<path id="{}" d="{moved}" fill="{fill}" stroke="{stroke}" stroke-width="0.1"/>"#,
            unique_id(&project.svg, &object.shape, existing + i)
        )
        .unwrap();
        bounds = union(bounds, Some([object.x_mm, object.y_mm, b[2], b[3]]));
    }
    let mut result = with_document(project, &body, bounds.unwrap())?;
    let ids: Vec<usize> = (existing..existing + objects.len()).collect();
    // Whole-motif processing turns into individual steps when the new
    // elements need another operation; the motif keeps its parameters.
    let convert =
        project.steps.is_empty() && objects.iter().any(|o| o.operation != project.operation);
    if convert && existing > 0 {
        result.steps.push(JobStep {
            objects: (0..existing).collect(),
            power_percent: project.power_percent,
            speed_percent: project.speed_percent,
            passes: project.passes,
            raster: project.raster,
            ..JobStep::new(project.operation)
        });
    }
    if convert || individual(project) {
        for (id, object) in ids.iter().zip(objects) {
            let operation = object.operation;
            match result.steps.iter_mut().find(|s| s.operation == operation) {
                Some(step) => step.objects.push(*id),
                None => result.steps.push(JobStep {
                    objects: vec![*id],
                    ..JobStep::new(operation)
                }),
            }
        }
    }
    Ok((result, ids))
}

/// Copies of the whole motif in a grid, row by row; the original stays
/// first. Individual assignments apply to every copy.
pub fn array(
    project: &Project,
    columns: u32,
    rows: u32,
    gap_x_mm: f64,
    gap_y_mm: f64,
) -> Result<Project, String> {
    if project.svg.is_empty() {
        return Err("Zuerst ein Motiv öffnen".into());
    }
    let copies = columns as usize * rows as usize;
    if !(2..=MAX_COPIES).contains(&copies) {
        return Err(format!("2 bis {MAX_COPIES} Exemplare sind möglich"));
    }
    if !gap_x_mm.is_finite() || !gap_y_mm.is_finite() {
        return Err("Abstand muss endlich sein".into());
    }
    let (w, h) = (project.width_mm as f64, project.height_mm as f64);
    let objects = selection::objects(&project.svg)?.len();
    let mut body = String::new();
    for copy in 0..copies {
        let (column, row) = (copy % columns as usize, copy / columns as usize);
        let mut placed = project.clone();
        placed.x_mm += (column as f64 * (w + gap_x_mm)) as f32;
        placed.y_mm += (row as f64 * (h + gap_y_mm)) as f32;
        let suffix = format!("-k{}", copy + 1);
        body += &embed(&placed, (copy > 0).then_some(suffix.as_str()), None)?;
        check_size(&body)?;
    }
    let bounds = [
        project.x_mm as f64,
        project.y_mm as f64,
        columns as f64 * w + (columns as f64 - 1.0) * gap_x_mm,
        rows as f64 * h + (rows as f64 - 1.0) * gap_y_mm,
    ];
    let mut result = with_document(project, &body, bounds)?;
    remap(&mut result, |old| {
        (0..copies).map(|copy| copy * objects + old).collect()
    });
    Ok(result)
}

/// Bounding-box nesting: copies of every part (top-level object, or object
/// of a top-level layer) are placed into the sheet, largest first, with the
/// free-rectangle method; parts may be turned by 90°.
pub fn nest(project: &Project, settings: &NestSettings) -> Result<Nested, String> {
    if project.svg.is_empty() {
        return Err("Zuerst ein Motiv öffnen".into());
    }
    let [ax, ay, aw, ah] = settings.area_mm;
    if !settings.area_mm.iter().all(|v| v.is_finite()) || aw <= 0.0 || ah <= 0.0 {
        return Err("Fläche braucht eine positive Breite und Höhe".into());
    }
    if !settings.gap_mm.is_finite() || settings.gap_mm < 0.0 {
        return Err("Abstand darf nicht negativ sein".into());
    }
    let parts = parts(project)?;
    if parts.is_empty() {
        return Err("Keine Teile im Motiv".into());
    }
    let mut instances: Vec<usize> = (0..parts.len())
        .flat_map(|p| std::iter::repeat_n(p, settings.quantity.max(1) as usize))
        .collect();
    if instances.len() > MAX_COPIES {
        return Err(format!("Höchstens {MAX_COPIES} Teile sind möglich"));
    }
    // Largest first; ties keep the drawing order.
    instances.sort_by(|a, b| {
        let area = |p: usize| parts[p].bounds[2] * parts[p].bounds[3];
        area(*b).total_cmp(&area(*a))
    });
    let gap = settings.gap_mm;
    let mut free = vec![[ax, ay, aw + gap, ah + gap]];
    // (part, x, y, rotated)
    let mut placements: Vec<(usize, f64, f64, bool)> = Vec::new();
    let mut missing = 0;
    for part in instances {
        let [_, _, w, h] = parts[part].bounds;
        let mut best: Option<(f64, f64, usize, bool)> = None;
        for (index, rect) in free.iter().enumerate() {
            for rotated in [false, true] {
                if rotated && !settings.rotate {
                    continue;
                }
                let (pw, ph) = if rotated { (h, w) } else { (w, h) };
                let (pw, ph) = (pw + gap, ph + gap);
                if pw <= rect[2] + 1e-9 && ph <= rect[3] + 1e-9 {
                    // Best short side fit, then lowest position.
                    let fit = (rect[2] - pw).min(rect[3] - ph);
                    let score = (fit, rect[1] + rect[0] * 1e-6);
                    if best.is_none_or(|b| (score.0, score.1) < (b.0, b.1)) {
                        best = Some((score.0, score.1, index, rotated));
                    }
                }
            }
        }
        let Some((_, _, index, rotated)) = best else {
            missing += 1;
            continue;
        };
        let rect = free[index];
        let (pw, ph) = if rotated { (h, w) } else { (w, h) };
        let used = [rect[0], rect[1], pw + gap, ph + gap];
        placements.push((part, rect[0], rect[1], rotated));
        split(&mut free, used);
    }
    if placements.is_empty() {
        return Err("Kein Teil passt in die Fläche".into());
    }
    // Output in drawing order of the parts so object numbers stay ordered.
    placements.sort_by_key(|p| p.0);
    let mut body = String::new();
    let mut bounds = None;
    let mut new_ids: Vec<Vec<usize>> = vec![Vec::new(); selection::objects(&project.svg)?.len()];
    let mut next = 0;
    for (copy, (part, x, y, rotated)) in placements.iter().enumerate() {
        let part = &parts[*part];
        let [bx, by, w, h] = part.bounds;
        let transform = if *rotated {
            format!(
                "translate({} {}) rotate(90) translate({} {})",
                x + h,
                y,
                -bx,
                -by
            )
        } else {
            format!("translate({} {})", x - bx, y - by)
        };
        let suffix = format!("-n{}", copy + 1);
        write!(
            body,
            r#"<g transform="{transform}">{}</g>"#,
            embed(
                project,
                (copy > 0).then_some(suffix.as_str()),
                Some(&part.objects)
            )?
        )
        .unwrap();
        check_size(&body)?;
        let (pw, ph) = if *rotated { (h, w) } else { (w, h) };
        bounds = union(bounds, Some([*x, *y, pw, ph]));
        for old in &part.objects {
            new_ids[*old].push(next);
            next += 1;
        }
    }
    let mut result = with_document(project, &body, bounds.unwrap())?;
    remap(&mut result, |old| {
        new_ids.get(old).cloned().unwrap_or_default()
    });
    Ok(Nested {
        project: result,
        placed: placements.len(),
        missing,
    })
}

/// A nestable part: its selectable objects and bounds on the bed.
struct Part {
    objects: Vec<usize>,
    bounds: [f64; 4],
}

fn parts(project: &Project) -> Result<Vec<Part>, String> {
    let doc = parse(&project.svg)?;
    let selectable = selection::selectable_nodes(&doc);
    let mut roots = Vec::new();
    for child in doc.root_element().children().filter(|n| n.is_element()) {
        let layer = child.tag_name().name() == "g"
            && child.attribute(("http://www.inkscape.org/namespaces/inkscape", "groupmode"))
                == Some("layer");
        if layer {
            roots.extend(child.children().filter(|n| n.is_element()));
        } else {
            roots.push(child);
        }
    }
    let (sx, sy) = scale(project)?;
    let mut parts = Vec::new();
    for root in roots {
        let objects: Vec<usize> = selectable
            .iter()
            .enumerate()
            .filter(|(_, n)| **n == root || n.ancestors().any(|a| a == root))
            .map(|(i, _)| i)
            .collect();
        if objects.is_empty() {
            continue;
        }
        let only = selection::filter(&project.svg, &objects)?;
        let tree = usvg::Tree::from_str(&only, &svg::options()).map_err(|e| e.to_string())?;
        let Some(b) = tree.root().abs_stroke_bounding_box().to_non_zero_rect() else {
            continue;
        };
        parts.push(Part {
            objects,
            bounds: [
                project.x_mm as f64 + b.x() as f64 * sx,
                project.y_mm as f64 + b.y() as f64 * sy,
                b.width() as f64 * sx,
                b.height() as f64 * sy,
            ],
        });
    }
    Ok(parts)
}

/// Splits the free rectangles around a used one and drops contained ones.
fn split(free: &mut Vec<[f64; 4]>, used: [f64; 4]) {
    let mut next = Vec::new();
    for r in free.drain(..) {
        let overlaps = used[0] < r[0] + r[2]
            && used[0] + used[2] > r[0]
            && used[1] < r[1] + r[3]
            && used[1] + used[3] > r[1];
        if !overlaps {
            next.push(r);
            continue;
        }
        if used[0] > r[0] {
            next.push([r[0], r[1], used[0] - r[0], r[3]]);
        }
        if used[0] + used[2] < r[0] + r[2] {
            let x = used[0] + used[2];
            next.push([x, r[1], r[0] + r[2] - x, r[3]]);
        }
        if used[1] > r[1] {
            next.push([r[0], r[1], r[2], used[1] - r[1]]);
        }
        if used[1] + used[3] < r[1] + r[3] {
            let y = used[1] + used[3];
            next.push([r[0], y, r[2], r[1] + r[3] - y]);
        }
    }
    let contains = |a: &[f64; 4], b: &[f64; 4]| {
        a[0] <= b[0] && a[1] <= b[1] && a[0] + a[2] >= b[0] + b[2] && a[1] + a[3] >= b[1] + b[3]
    };
    let mut kept: Vec<[f64; 4]> = Vec::new();
    for (i, r) in next.iter().enumerate() {
        let inside = next
            .iter()
            .enumerate()
            .any(|(j, o)| j != i && contains(o, r) && (o != r || j < i));
        if !inside && r[2] > 1e-9 && r[3] > 1e-9 {
            kept.push(*r);
        }
    }
    *free = kept;
}

/// Explicit object lists of all steps after objects were copied or moved.
fn remap(project: &mut Project, map: impl Fn(usize) -> Vec<usize>) {
    for step in &mut project.steps {
        step.objects = step.objects.iter().flat_map(|o| map(*o)).collect();
        step.objects.sort_unstable();
    }
}

/// Steps list their objects (individual assignment).
fn individual(project: &Project) -> bool {
    !project.steps.is_empty() && project.steps.iter().all(|s| s.filters.is_none() && !s.rest)
}

fn style(operation: Operation) -> (&'static str, &'static str) {
    match operation {
        Operation::Cut => ("none", "#ff0000"),
        Operation::Mark => ("none", "#0000ff"),
        Operation::Engrave | Operation::Engrave3d => ("#000000", "none"),
    }
}

fn unique_id(svg: &str, shape: &Shape, number: usize) -> String {
    let base = match shape {
        Shape::Rectangle { .. } => "Rechteck",
        Shape::Ellipse { .. } => "Ellipse",
        Shape::Polygon { .. } => "Vieleck",
        Shape::Star { .. } => "Stern",
        Shape::Text { .. } => "Text",
    };
    let mut n = number + 1;
    while svg.contains(&format!("id=\"{base}{n}\"")) {
        n += 1;
    }
    format!("{base}{n}")
}

fn union(a: Option<[f64; 4]>, b: Option<[f64; 4]>) -> Option<[f64; 4]> {
    match (a, b) {
        (Some(a), Some(b)) => {
            let x0 = a[0].min(b[0]);
            let y0 = a[1].min(b[1]);
            let x1 = (a[0] + a[2]).max(b[0] + b[2]);
            let y1 = (a[1] + a[3]).max(b[1] + b[3]);
            Some([x0, y0, x1 - x0, y1 - y0])
        }
        (a, None) => a,
        (None, b) => b,
    }
}

fn motif_bounds(project: &Project) -> Option<[f64; 4]> {
    (!project.svg.is_empty()).then_some([
        project.x_mm as f64,
        project.y_mm as f64,
        project.width_mm as f64,
        project.height_mm as f64,
    ])
}

fn check_size(body: &str) -> Result<(), String> {
    if body.len() > MAX_SVG_BYTES {
        return Err(
            "Ergebnis wäre größer als 20 MB; weniger Kopien oder kleinere Bilder verwenden".into(),
        );
    }
    Ok(())
}

/// The project with a new document in bed millimetres covering `bounds`.
fn with_document(project: &Project, body: &str, bounds: [f64; 4]) -> Result<Project, String> {
    let [x, y, w, h] = bounds;
    if !(w > 0.0 && h > 0.0) {
        return Err("Ergebnis hat keine Fläche".into());
    }
    let document = format!(
        r#"<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{}mm" height="{}mm" viewBox="{} {} {} {}">{body}</svg>"#,
        num(w),
        num(h),
        num(x),
        num(y),
        num(w),
        num(h)
    );
    check_size(&document)?;
    let mut result = Project {
        svg: document,
        x_mm: x as f32,
        y_mm: y as f32,
        width_mm: w as f32,
        height_mm: h as f32,
        ..project.clone()
    };
    if result.name.is_empty() {
        result.name = "Schild".into();
    }
    result.validate_document()?;
    Ok(result)
}

fn parse(source: &str) -> Result<roxmltree::Document<'_>, String> {
    if source.contains("<!ENTITY") {
        return Err(
            "SVG mit DTD-Entitäten lässt sich nicht erweitern; bitte als normales SVG speichern"
                .into(),
        );
    }
    let options = roxmltree::ParsingOptions {
        allow_dtd: true,
        ..Default::default()
    };
    roxmltree::Document::parse_with_options(source, options).map_err(|e| e.to_string())
}

/// Bed millimetres per SVG canvas pixel.
fn scale(project: &Project) -> Result<(f64, f64), String> {
    let tree = usvg::Tree::from_str(&project.svg, &svg::options())
        .map_err(|e| format!("SVG konnte nicht gelesen werden: {e}"))?;
    let size = tree.size();
    Ok((
        project.width_mm as f64 / size.width() as f64,
        project.height_mm as f64 / size.height() as f64,
    ))
}

/// The motif as it lies on the bed: its root `<svg>` sized in canvas pixels
/// inside a group that scales to the project's position and size. `suffix`
/// is appended to all ids (references keep pointing at the first copy);
/// `visible` limits the drawn objects like a step selection.
fn embed(
    project: &Project,
    suffix: Option<&str>,
    visible: Option<&[usize]>,
) -> Result<String, String> {
    let tree = usvg::Tree::from_str(&project.svg, &svg::options())
        .map_err(|e| format!("SVG konnte nicht gelesen werden: {e}"))?;
    let size = tree.size();
    let source = match visible {
        Some(objects) => selection::filter(&project.svg, objects)?,
        None => project.svg.clone(),
    };
    let doc = parse(&source)?;
    let root = doc.root_element();
    let range = root.range();
    let start_end = start_tag_end(&source, range.start).ok_or("SVG-Wurzel ist unvollständig")?;
    let mut edits: Vec<(std::ops::Range<usize>, String)> = Vec::new();
    for attribute in root.attributes() {
        if attribute.namespace().is_none()
            && matches!(
                attribute.name(),
                "x" | "y" | "width" | "height" | "overflow"
            )
        {
            // Remove the attribute including the white space before it.
            let r = attribute.range();
            let mut start = r.start;
            while start > range.start && source.as_bytes()[start - 1].is_ascii_whitespace() {
                start -= 1;
            }
            edits.push((start..r.end, String::new()));
        }
    }
    let close = if source[..start_end].ends_with("/>") {
        start_end - 2
    } else {
        start_end - 1
    };
    edits.push((
        close..close,
        format!(
            r#" width="{}" height="{}" overflow="visible""#,
            num(size.width() as f64),
            num(size.height() as f64)
        ),
    ));
    if let Some(suffix) = suffix {
        for node in root.descendants() {
            for attribute in node.attributes() {
                if attribute.namespace().is_none()
                    && attribute.name() == "id"
                    && let Some(value) = value_range(&source, attribute.range())
                {
                    edits.push((value.end..value.end, suffix.to_string()));
                }
            }
        }
    }
    edits.sort_by_key(|(r, _)| (r.start, r.end));
    let mut text = String::with_capacity(range.len() + 256);
    let mut position = range.start;
    for (r, replacement) in edits {
        text.push_str(&source[position..r.start]);
        text.push_str(&replacement);
        position = r.end;
    }
    text.push_str(&source[position..range.end]);
    let (sx, sy) = (
        project.width_mm as f64 / size.width() as f64,
        project.height_mm as f64 / size.height() as f64,
    );
    Ok(format!(
        r#"<g transform="matrix({} 0 0 {} {} {})">{text}</g>"#,
        num(sx),
        num(sy),
        num(project.x_mm as f64),
        num(project.y_mm as f64)
    ))
}

/// Byte index after the `>` of the start tag beginning at `start`.
fn start_tag_end(source: &str, start: usize) -> Option<usize> {
    let mut quote = None;
    for (i, c) in source[start..].char_indices() {
        match (quote, c) {
            (None, '"' | '\'') => quote = Some(c),
            (Some(q), c) if c == q => quote = None,
            (None, '>') => return Some(start + i + 1),
            _ => {}
        }
    }
    None
}

fn num(value: f64) -> String {
    let text = format!("{value:.4}");
    let text = text.trim_end_matches('0').trim_end_matches('.');
    if text == "-0" {
        "0".into()
    } else {
        text.into()
    }
}

/// Path data of a shape at its own origin and its bounds `[x, y, w, h]`.
fn outline(shape: &Shape) -> Result<(String, [f64; 4]), String> {
    let positive = |v: f64| v.is_finite() && v > 0.0 && v <= 10_000.0;
    match shape {
        Shape::Rectangle {
            width_mm: w,
            height_mm: h,
            corner_mm: r,
        } => {
            if !positive(*w) || !positive(*h) || !r.is_finite() || *r < 0.0 {
                return Err("Rechteck braucht positive Maße".into());
            }
            let r = r.min(w / 2.0).min(h / 2.0);
            let d = if r > 0.0 {
                format!(
                    "M{r} 0H{}A{r} {r} 0 0 1 {w} {r}V{}A{r} {r} 0 0 1 {} {h}H{r}A{r} {r} 0 0 1 0 {}V{r}A{r} {r} 0 0 1 {r} 0Z",
                    w - r,
                    h - r,
                    w - r,
                    h - r
                )
            } else {
                format!("M0 0H{w}V{h}H0Z")
            };
            Ok((d, [0.0, 0.0, *w, *h]))
        }
        Shape::Ellipse {
            width_mm: w,
            height_mm: h,
        } => {
            if !positive(*w) || !positive(*h) {
                return Err("Ellipse braucht positive Maße".into());
            }
            let (rx, ry) = (w / 2.0, h / 2.0);
            Ok((
                format!("M0 {ry}A{rx} {ry} 0 1 0 {w} {ry}A{rx} {ry} 0 1 0 0 {ry}Z"),
                [0.0, 0.0, *w, *h],
            ))
        }
        Shape::Polygon { sides, diameter_mm } => {
            if !(3..=100).contains(sides) || !positive(*diameter_mm) {
                return Err("Vieleck braucht 3 bis 100 Ecken und einen Durchmesser".into());
            }
            Ok(polygon(
                (0..*sides).map(|i| (diameter_mm / 2.0, i as f64 / *sides as f64)),
            ))
        }
        Shape::Star {
            points,
            diameter_mm,
            inner_percent,
        } => {
            if !(3..=100).contains(points)
                || !positive(*diameter_mm)
                || !(5.0..=95.0).contains(inner_percent)
            {
                return Err("Stern braucht 3 bis 100 Zacken und 5–95 % Innenradius".into());
            }
            let outer = diameter_mm / 2.0;
            let inner = outer * inner_percent / 100.0;
            Ok(polygon((0..2 * points).map(|i| {
                (
                    if i % 2 == 0 { outer } else { inner },
                    i as f64 / (2 * points) as f64,
                )
            })))
        }
        Shape::Text { .. } => text_outline(shape),
    }
}

/// Closed polygon from (radius, fraction of a turn) pairs, first at the top.
fn polygon(corners: impl Iterator<Item = (f64, f64)>) -> (String, [f64; 4]) {
    let points: Vec<(f64, f64)> = corners
        .map(|(r, t)| {
            let a = t * std::f64::consts::TAU - std::f64::consts::FRAC_PI_2;
            (r * a.cos(), r * a.sin())
        })
        .collect();
    let (mut x0, mut y0, mut x1, mut y1) = (f64::MAX, f64::MAX, f64::MIN, f64::MIN);
    for (x, y) in &points {
        (x0, y0, x1, y1) = (x0.min(*x), y0.min(*y), x1.max(*x), y1.max(*y));
    }
    let mut d = String::new();
    for (i, (x, y)) in points.iter().enumerate() {
        write!(
            d,
            "{}{} {}",
            if i == 0 { "M" } else { "L" },
            num(*x),
            num(*y)
        )
        .unwrap();
    }
    d.push('Z');
    (d, [x0, y0, x1 - x0, y1 - y0])
}

fn escape(text: &str) -> String {
    text.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
}

/// Text converted to outlines with the application's fonts.
fn text_outline(shape: &Shape) -> Result<(String, [f64; 4]), String> {
    let Shape::Text {
        text,
        font,
        size_mm,
        bold,
        italic,
        align,
        line_spacing,
    } = shape
    else {
        unreachable!()
    };
    if !size_mm.is_finite() || !(0.5..=1000.0).contains(size_mm) {
        return Err("Schriftgröße muss zwischen 0,5 und 1000 mm liegen".into());
    }
    if !line_spacing.is_finite() || !(0.5..=5.0).contains(line_spacing) {
        return Err("Zeilenabstand muss zwischen 0,5 und 5 liegen".into());
    }
    let lines: Vec<&str> = text.lines().collect();
    if lines.iter().all(|l| l.trim().is_empty()) || text.chars().count() > 2000 {
        return Err("Text fehlt oder ist länger als 2000 Zeichen".into());
    }
    let anchor = match align {
        Align::Left => "start",
        Align::Center => "middle",
        Align::Right => "end",
    };
    let mut spans = String::new();
    for (i, line) in lines.iter().enumerate() {
        write!(
            spans,
            r#"<tspan x="0" y="{}">{}</tspan>"#,
            num(i as f64 * size_mm * line_spacing),
            escape(line)
        )
        .unwrap();
    }
    let source = format!(
        r#"<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><text xml:space="preserve" font-family="{}" font-size="{}" font-weight="{}" font-style="{}" text-anchor="{anchor}">{spans}</text></svg>"#,
        escape(font),
        num(*size_mm),
        if *bold { "bold" } else { "normal" },
        if *italic { "italic" } else { "normal" },
    );
    let tree = usvg::Tree::from_str(&source, &svg::options()).map_err(|e| e.to_string())?;
    let mut d = String::new();
    let mut bounds: Option<tiny_skia::Rect> = None;
    collect(tree.root(), &mut d, &mut bounds);
    let b = bounds.ok_or("Der Text ergibt keine Umrisse (Schrift ohne passende Zeichen?)")?;
    Ok((
        d,
        [
            b.x() as f64,
            b.y() as f64,
            b.width() as f64,
            b.height() as f64,
        ],
    ))
}

fn collect(group: &usvg::Group, d: &mut String, bounds: &mut Option<tiny_skia::Rect>) {
    for node in group.children() {
        match node {
            usvg::Node::Group(g) => collect(g, d, bounds),
            usvg::Node::Text(t) => collect(t.flattened(), d, bounds),
            usvg::Node::Path(p) => {
                let Some(path) = p.data().clone().transform(p.abs_transform()) else {
                    continue;
                };
                let tight = path.compute_tight_bounds();
                *bounds = match (*bounds, tight) {
                    (Some(a), Some(b)) => tiny_skia::Rect::from_ltrb(
                        a.left().min(b.left()),
                        a.top().min(b.top()),
                        a.right().max(b.right()),
                        a.bottom().max(b.bottom()),
                    ),
                    (a, b) => a.or(b),
                };
                write_path(&path, d);
            }
            usvg::Node::Image(_) => {}
        }
    }
}

fn write_path(path: &tiny_skia::Path, d: &mut String) {
    use tiny_skia::PathSegment as S;
    let p = |pt: tiny_skia::Point| format!("{} {}", num(pt.x as f64), num(pt.y as f64));
    for segment in path.segments() {
        match segment {
            S::MoveTo(a) => write!(d, "M{}", p(a)),
            S::LineTo(a) => write!(d, "L{}", p(a)),
            S::QuadTo(a, b) => write!(d, "Q{} {}", p(a), p(b)),
            S::CubicTo(a, b, c) => write!(d, "C{} {} {}", p(a), p(b), p(c)),
            S::Close => write!(d, "Z"),
        }
        .unwrap();
    }
}

/// Moves absolute path data (as written here: M, L, H, V, A, Q, C, Z with
/// absolute coordinates) by `(dx, dy)`.
fn transform_path(d: &str, dx: f64, dy: f64, out: &mut String) {
    let mut command = ' ';
    let mut index = 0;
    let mut number = String::new();
    let flush = |number: &mut String, command: char, index: &mut usize, out: &mut String| {
        if number.is_empty() {
            return;
        }
        let value: f64 = number.parse().unwrap_or(0.0);
        // Which argument of the command is an x or y coordinate.
        let shifted = match command {
            'H' => value + dx,
            'V' => value + dy,
            'A' => match *index % 7 {
                5 => value + dx,
                6 => value + dy,
                _ => value,
            },
            _ => {
                if (*index).is_multiple_of(2) {
                    value + dx
                } else {
                    value + dy
                }
            }
        };
        if !out.ends_with(|c: char| c.is_ascii_alphabetic()) && !out.is_empty() {
            out.push(' ');
        }
        out.push_str(&num(shifted));
        *index += 1;
        number.clear();
    };
    for c in d.chars() {
        if c.is_ascii_alphabetic() && c != 'e' {
            flush(&mut number, command, &mut index, out);
            command = c;
            index = 0;
            out.push(c);
        } else if c == ' ' || c == ',' {
            flush(&mut number, command, &mut index, out);
        } else if c == '-' && !number.is_empty() && !number.ends_with('e') {
            flush(&mut number, command, &mut index, out);
            number.push(c);
        } else {
            number.push(c);
        }
    }
    flush(&mut number, command, &mut index, out);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ltt;

    fn demo() -> Project {
        Project {
            svg: include_str!("../examples/demo.svg").into(),
            x_mm: 20.0,
            y_mm: 30.0,
            width_mm: 100.0,
            height_mm: 60.0,
            ..Default::default()
        }
    }

    #[test]
    fn moving_path_data_shifts_coordinates_only() {
        let mut out = String::new();
        transform_path("M1 2H5V-3A2 2 0 0 1 4 5Z", 10.0, 100.0, &mut out);
        assert_eq!(out, "M11 102H15V97A2 2 0 0 1 14 105Z");
    }

    #[test]
    fn shapes_have_exact_bounds() {
        let (_, b) = outline(&Shape::Polygon {
            sides: 4,
            diameter_mm: 20.0,
        })
        .unwrap();
        assert!((b[2] - 20.0).abs() < 1e-9 && (b[3] - 20.0).abs() < 1e-9);
        assert!(
            outline(&Shape::Star {
                points: 2,
                diameter_mm: 10.0,
                inner_percent: 50.0
            })
            .is_err()
        );
        assert!(
            outline(&Shape::Rectangle {
                width_mm: 0.0,
                height_mm: 1.0,
                corner_mm: 0.0
            })
            .is_err()
        );
    }

    #[test]
    fn text_becomes_outlines_in_millimetres() {
        let shape = Shape::Text {
            text: "Hallo\nWelt".into(),
            font: "sans-serif".into(),
            size_mm: 10.0,
            bold: false,
            italic: false,
            align: Align::Left,
            line_spacing: 1.2,
        };
        let (d, b) = outline(&shape).unwrap();
        assert!(d.starts_with('M') && d.contains('Z'));
        // Two lines of 10 mm text: roughly 7 + 12 mm high, wider than tall.
        assert!(b[3] > 15.0 && b[3] < 25.0, "{b:?}");
        assert!(b[2] > 20.0, "{b:?}");
    }

    #[test]
    fn adding_keeps_the_motif_and_its_assignments() {
        let mut project = demo();
        let count = selection::objects(&project.svg).unwrap().len();
        project.steps = vec![JobStep {
            objects: (0..count).collect(),
            ..JobStep::new(Operation::Engrave)
        }];
        let before = ltt::prepare(&project).unwrap();
        let (added, ids) = add_objects(
            &project,
            &[NewObject {
                shape: Shape::Rectangle {
                    width_mm: 20.0,
                    height_mm: 10.0,
                    corner_mm: 2.0,
                },
                operation: Operation::Cut,
                x_mm: 200.0,
                y_mm: 40.0,
            }],
        )
        .unwrap();
        assert_eq!(ids, vec![count]);
        assert_eq!(selection::objects(&added.svg).unwrap().len(), count + 1);
        assert_eq!((added.x_mm, added.y_mm), (20.0, 30.0));
        assert!((added.width_mm - 200.0).abs() < 1e-3);
        assert_eq!(added.steps[0].objects, (0..count).collect::<Vec<_>>());
        assert_eq!(added.steps[1].operation, Operation::Cut);
        assert_eq!(added.steps[1].objects, vec![count]);
        // The engraving of the motif is unchanged on the bed.
        let after = ltt::prepare(&added).unwrap();
        let engrave = |job: &ltt::PreparedJob| {
            job.jobs
                .iter()
                .find(|j| j.operation == Operation::Engrave)
                .unwrap()
                .bytes
                .clone()
        };
        let (a, b) = (engrave(&before), engrave(&after));
        // f32 transforms of the nested motif may move a raster edge by one
        // pixel; the engraving stays the same within that.
        assert!(
            a.len().abs_diff(b.len()) * 100 < a.len(),
            "{} {}",
            a.len(),
            b.len()
        );
        let lines = |job: &ltt::PreparedJob| job.steps[0].description.clone();
        let (l0, l1) = (lines(&before), lines(&after));
        let count_of = |text: &str| -> i64 { text.split(' ').next().unwrap().parse().unwrap() };
        assert!((count_of(&l0) - count_of(&l1)).abs() <= 1, "{l0} / {l1}");
        let cut = after
            .jobs
            .iter()
            .find(|j| j.operation == Operation::Cut)
            .unwrap();
        assert!(!cut.bytes.is_empty());
        // Adding again nests once more and keeps everything.
        let (twice, _) = add_objects(
            &added,
            &[NewObject {
                shape: Shape::Ellipse {
                    width_mm: 5.0,
                    height_mm: 5.0,
                },
                operation: Operation::Mark,
                x_mm: 5.0,
                y_mm: 5.0,
            }],
        )
        .unwrap();
        assert_eq!((twice.x_mm, twice.y_mm), (5.0, 5.0));
        assert_eq!(selection::objects(&twice.svg).unwrap().len(), count + 2);
        ltt::prepare(&twice).unwrap();
    }

    #[test]
    fn a_sign_starts_an_empty_project() {
        let objects = sign(&Sign::default()).unwrap();
        // Plate, two holes, text.
        assert_eq!(objects.len(), 4);
        let (project, ids) = add_objects(&Project::default(), &objects).unwrap();
        assert_eq!(project.steps.len(), 2);
        assert_eq!(ids.len(), 4);
        assert_eq!((project.x_mm, project.y_mm), (10.0, 10.0));
        assert!((project.width_mm - 150.0).abs() < 1e-3);
        let job = ltt::prepare(&project).unwrap();
        assert!(job.jobs.iter().any(|j| j.operation == Operation::Engrave));
        assert!(job.jobs.iter().any(|j| j.operation == Operation::Cut));
    }

    #[test]
    fn arrays_copy_the_motif_and_its_assignments() {
        let mut project = demo();
        let count = selection::objects(&project.svg).unwrap().len();
        project.steps = vec![JobStep {
            objects: vec![0],
            ..JobStep::new(Operation::Cut)
        }];
        let copies = array(&project, 3, 2, 5.0, 4.0).unwrap();
        assert_eq!(selection::objects(&copies.svg).unwrap().len(), 6 * count);
        assert!((copies.width_mm - 310.0).abs() < 1e-3);
        assert!((copies.height_mm - 124.0).abs() < 1e-3);
        assert_eq!(
            copies.steps[0].objects,
            (0..6).map(|c| c * count).collect::<Vec<_>>()
        );
        // Ids stay unique.
        assert!(copies.svg.contains("-k6\""));
        ltt::prepare(&copies).unwrap();
        assert!(array(&project, 1, 1, 0.0, 0.0).is_err());
        assert!(array(&project, 30, 30, 0.0, 0.0).is_err());
    }

    #[test]
    fn nesting_fills_the_sheet_and_reports_what_does_not_fit() {
        let project = Project {
            svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="50mm" viewBox="0 0 100 50"><rect id="a" width="40" height="20" fill="none" stroke="red" stroke-width="0.1"/><g><circle cx="70" cy="25" r="10" fill="none" stroke="red" stroke-width="0.1"/><circle cx="70" cy="25" r="3" fill="none" stroke="red" stroke-width="0.1"/></g></svg>"#.into(),
            x_mm: 0.0,
            y_mm: 0.0,
            width_mm: 100.0,
            height_mm: 50.0,
            steps: vec![JobStep { objects: vec![0, 1, 2], ..JobStep::new(Operation::Cut) }],
            ..Default::default()
        };
        let settings = NestSettings {
            area_mm: [10.0, 10.0, 120.0, 60.0],
            gap_mm: 2.0,
            quantity: 4,
            rotate: true,
        };
        let nested = nest(&project, &settings).unwrap();
        assert_eq!(nested.placed + nested.missing, 8);
        assert_eq!(nested.placed, 6);
        let p = &nested.project;
        assert!(p.x_mm >= 10.0 - 0.01 && p.y_mm >= 10.0 - 0.01);
        assert!(p.x_mm + p.width_mm <= 130.01 && p.y_mm + p.height_mm <= 70.01);
        let objects = selection::objects(&p.svg).unwrap().len();
        assert_eq!(p.steps[0].objects.len(), objects);
        ltt::prepare(p).unwrap();
        let tight = NestSettings {
            area_mm: [0.0, 0.0, 5.0, 5.0],
            ..settings
        };
        assert!(nest(&project, &tight).is_err());
    }

    #[test]
    fn fonts_include_generic_families() {
        let fonts = font_families();
        assert_eq!(fonts[0], "sans-serif");
        assert!(fonts.len() > 3);
    }
}
