use crate::project::Project;
use resvg::{tiny_skia, usvg};
use std::sync::{Arc, Mutex};

pub type Point = [f32; 2];
pub type Contour = Vec<Point>;

pub fn contours(project: &Project) -> Result<Vec<Contour>, String> {
    contours_with_fonts(project, crate::svg::fonts())
}

fn contours_with_fonts(
    project: &Project,
    fonts: Arc<usvg::fontdb::Database>,
) -> Result<Vec<Contour>, String> {
    let document = usvg::roxmltree::Document::parse(&project.svg).map_err(|e| e.to_string())?;
    for node in document.descendants().filter(|node| node.is_element()) {
        if node.tag_name().name() == "image"
            && !node.ancestors().any(|n| {
                matches!(
                    n.tag_name().name(),
                    "defs" | "symbol" | "clipPath" | "mask" | "pattern" | "marker"
                )
            })
        {
            return Err(
                "SVG enthält Rasterbilder; zuerst in Pfade umwandeln oder Gravieren wählen".into(),
            );
        }
    }
    check_clip_references(&document)?;
    // Text is cut along its glyph outlines, like the Java importer does. Glyphs
    // usvg cannot place would otherwise vanish silently, so they are reported.
    let missing = Mutex::new(Vec::new());
    let options = usvg::Options {
        fontdb: fonts,
        font_resolver: outline_fonts(&missing),
        ..crate::svg::options()
    };
    let mut tree = usvg::Tree::from_str(&project.svg, &options).map_err(|e| e.to_string())?;
    if let Some(problem) = missing.lock().map_err(|e| e.to_string())?.first() {
        return Err(problem.clone());
    }
    if contains_text(tree.root()) {
        // usvg keeps glyph outlines relative to their <text> element (the flattened
        // paths carry an identity transform); writing the tree back out places them
        // under their ancestors' transforms like ordinary paths.
        let flattened = tree.to_string(&usvg::WriteOptions::default());
        tree = usvg::Tree::from_str(&flattened, &options).map_err(|e| e.to_string())?;
    }
    let mut result = Vec::new();
    visit(tree.root(), project, tree.size(), &[], &mut result)?;
    if result.is_empty() {
        return Err("Keine schneidbaren Vektorpfade in der SVG".into());
    }
    if result.iter().map(Vec::len).sum::<usize>() > 1_000_000 {
        return Err("Zu viele Vektorpunkte".into());
    }
    Ok(result)
}

fn outline_fonts(missing: &Mutex<Vec<String>>) -> usvg::FontResolver<'_> {
    let select = usvg::FontResolver::default_font_selector();
    let fallback = usvg::FontResolver::default_fallback_selector();
    let report = move |problem: String| {
        if let Ok(mut list) = missing.lock() {
            list.push(problem);
        }
    };
    usvg::FontResolver {
        select_font: Box::new(move |font, db| {
            let id = select(font, db);
            if id.is_none() {
                let families: Vec<String> = font.families().iter().map(|f| f.to_string()).collect();
                report(format!(
                    "Schriftart für Text nicht gefunden ({}); Schriftart installieren oder Text in Pfade umwandeln",
                    families.join(", ")
                ));
            }
            id
        }),
        select_fallback: Box::new(move |c, used, db| {
            let id = fallback(c, used, db);
            let invisible = c.is_whitespace()
                || c.is_control()
                || matches!(c, '\u{200B}'..='\u{200F}' | '\u{202A}'..='\u{202E}' | '\u{2060}'..='\u{206F}' | '\u{FE00}'..='\u{FE0F}' | '\u{FEFF}');
            if id.is_none() && !invisible {
                report(format!(
                    "Zeichen „{c}“ (U+{:04X}) ist in keiner Schriftart enthalten; Schriftart installieren oder Text in Pfade umwandeln",
                    c as u32
                ));
            }
            id
        }),
    }
}

fn contains_text(group: &usvg::Group) -> bool {
    group.children().iter().any(|node| match node {
        usvg::Node::Text(_) => true,
        usvg::Node::Group(group) => contains_text(group),
        _ => false,
    })
}

/// Masks and filters cannot be cut: a mask only sets transparency and a filter
/// changes the picture, so neither defines a cut line.
const MASK_REJECTED: &str = "Masken können nicht geschnitten werden, weil sie nur Transparenz festlegen und keine Schnittlinie ergeben; Maske entfernen oder Objekt vorher in Pfade umwandeln";
const FILTER_REJECTED: &str = "Filter wie Weichzeichnen oder Schlagschatten verändern das Motiv nur als Pixelbild und ergeben keine Schnittlinie; Filter entfernen oder Objekt vorher in Pfade umwandeln";
const NESTED_CLIP: &str = "Verschachtelte Clip-Pfade (Clipping auf einem Clip-Pfad oder innerhalb eines Clip-Pfads) werden beim Schneiden nicht unterstützt";
const EMPTY_CLIP: &str = "Clip-Pfad ist leer, daher bliebe nichts zu schneiden; Clip-Pfad mit Formen füllen oder Clipping entfernen";
const CLIP_IMAGE: &str = "Rasterbild im Clip-Pfad: Schneiden unterstützt keine Rasterbilder; Clip-Pfad nur mit Vektorformen verwenden";
const CLIP_TEXT: &str =
    "Text im Clip-Pfad wird beim Schneiden nicht unterstützt; Text vorher in Pfade umwandeln";

fn reject_effects(group: &usvg::Group) -> Result<(), String> {
    if group.mask().is_some() {
        return Err(MASK_REJECTED.into());
    }
    if !group.filters().is_empty() {
        return Err(FILTER_REJECTED.into());
    }
    Ok(())
}

/// A clip region in mm: the union of the shapes of one clipPath. A point is
/// inside the region when one of its shapes contains it.
#[derive(Clone)]
struct Clip {
    shapes: Vec<ClipShape>,
}

#[derive(Clone)]
struct ClipShape {
    /// Closed rings in mm, last point equal to the first.
    rings: Vec<Contour>,
    even_odd: bool,
    bbox: [f32; 4],
}

impl ClipShape {
    fn new(rings: Vec<Contour>, even_odd: bool) -> Self {
        let bbox = rings
            .iter()
            .flatten()
            .fold([f32::MAX, f32::MAX, f32::MIN, f32::MIN], |b, p| {
                [
                    b[0].min(p[0]),
                    b[1].min(p[1]),
                    b[2].max(p[0]),
                    b[3].max(p[1]),
                ]
            });
        Self {
            rings,
            even_odd,
            bbox,
        }
    }

    /// Winding and crossing counts of a ray from `p` to the right, summed over all rings.
    fn contains(&self, p: Point) -> bool {
        let [x0, y0, x1, y1] = self.bbox;
        if p[0] < x0 || p[0] > x1 || p[1] < y0 || p[1] > y1 {
            return false;
        }
        let (mut winding, mut crossings) = (0i32, 0u32);
        for ring in &self.rings {
            for edge in ring.windows(2) {
                let (a, b) = (edge[0], edge[1]);
                if a[1] <= p[1] {
                    if b[1] > p[1] && side(a, b, p) > 0.0 {
                        winding += 1;
                        crossings += 1;
                    }
                } else if b[1] <= p[1] && side(a, b, p) < 0.0 {
                    winding -= 1;
                    crossings += 1;
                }
            }
        }
        if self.even_odd {
            crossings % 2 == 1
        } else {
            winding != 0
        }
    }
}

impl Clip {
    fn contains(&self, p: Point) -> bool {
        self.shapes.iter().any(|shape| shape.contains(p))
    }
}

/// Positive when `p` lies left of the directed edge `a` → `b`.
fn side(a: Point, b: Point, p: Point) -> f64 {
    let (a, b, p) = (
        [f64::from(a[0]), f64::from(a[1])],
        [f64::from(b[0]), f64::from(b[1])],
        [f64::from(p[0]), f64::from(p[1])],
    );
    (b[0] - a[0]) * (p[1] - a[1]) - (p[0] - a[0]) * (b[1] - a[1])
}

/// Parameter `t` along `a` → `b` where it crosses the edge `c` → `d`, if the crossing
/// lies strictly inside the segment. Parallel edges are not reported; midpoint tests
/// decide those pieces.
fn crossing(a: Point, b: Point, c: Point, d: Point) -> Option<f64> {
    let p = |q: Point| (f64::from(q[0]), f64::from(q[1]));
    let ((ax, ay), (bx, by), (cx, cy), (dx, dy)) = (p(a), p(b), p(c), p(d));
    let r = (bx - ax, by - ay);
    let s = (dx - cx, dy - cy);
    let denominator = r.0 * s.1 - r.1 * s.0;
    if denominator.abs() < 1e-12 {
        return None;
    }
    let qp = (cx - ax, cy - ay);
    let t = (qp.0 * s.1 - qp.1 * s.0) / denominator;
    let u = (qp.0 * r.1 - qp.1 * r.0) / denominator;
    ((0.0..1.0).contains(&t) && (0.0..=1.0).contains(&u)).then_some(t)
}

fn point_at(a: Point, b: Point, t: f64) -> Point {
    [
        (f64::from(a[0]) + (f64::from(b[0]) - f64::from(a[0])) * t) as f32,
        (f64::from(a[1]) + (f64::from(b[1]) - f64::from(a[1])) * t) as f32,
    ]
}

/// Work limit for clipping: contour segments times clip edges.
const CLIP_WORK_LIMIT: usize = 100_000_000;

/// The parts of an open or closed polyline that lie inside all clip regions.
/// Each segment is split where it crosses a clip edge; the pieces between
/// splits are kept when their midpoint is inside. A closed contour that stays
/// inside as a whole is returned unchanged; pieces that meet at the start point
/// of a closed contour are joined again.
fn clip_contour(contour: &Contour, clips: &[Clip]) -> Result<Vec<Contour>, String> {
    let edges: Vec<(Point, Point)> = clips
        .iter()
        .flat_map(|clip| &clip.shapes)
        .flat_map(|shape| &shape.rings)
        .flat_map(|ring| ring.windows(2).map(|w| (w[0], w[1])))
        .collect();
    if edges.len().saturating_mul(contour.len()) > CLIP_WORK_LIMIT {
        return Err(
            "Clip-Pfad ist zu komplex für die Schnittberechnung; Clip vereinfachen oder Objekt in Pfade umwandeln"
                .into(),
        );
    }
    let inside = |p: Point| clips.iter().all(|clip| clip.contains(p));
    let mut pieces: Vec<Contour> = Vec::new();
    let mut current: Option<Contour> = None;
    for segment in contour.windows(2) {
        let (a, b) = (segment[0], segment[1]);
        let mut ts = vec![0.0, 1.0];
        ts.extend(edges.iter().filter_map(|&(c, d)| crossing(a, b, c, d)));
        ts.sort_by(f64::total_cmp);
        ts.dedup();
        for pair in ts.windows(2) {
            let (t0, t1) = (pair[0], pair[1]);
            if inside(point_at(a, b, (t0 + t1) / 2.0)) {
                let end = if t1 == 1.0 { b } else { point_at(a, b, t1) };
                if let Some(piece) = current.as_mut() {
                    if piece.last() != Some(&end) {
                        piece.push(end);
                    }
                } else {
                    let start = if t0 == 0.0 { a } else { point_at(a, b, t0) };
                    current = Some(vec![start, end]);
                }
            } else if let Some(piece) = current.take() {
                pieces.push(piece);
            }
        }
    }
    pieces.extend(current);
    if contour.len() > 1 && contour.first() == contour.last() && pieces.len() >= 2 {
        let first_at_start = pieces.first().and_then(|piece| piece.first()) == contour.first();
        let last_at_end = pieces.last().and_then(|piece| piece.last()) == contour.last();
        if first_at_start && last_at_end {
            let first = pieces.remove(0);
            if let Some(last) = pieces.last_mut() {
                last.extend(first.into_iter().skip(1));
            }
        }
    }
    pieces.retain(|piece| piece.len() >= 2);
    Ok(pieces)
}

/// Rejects a `clipPath` reference whose target is missing, is not a clipPath or
/// has no children. usvg would drop such elements silently, so this is checked
/// on the XML before the tree is built.
fn check_clip_references(document: &usvg::roxmltree::Document) -> Result<(), String> {
    for node in document.descendants().filter(|n| n.is_element()) {
        let Some(id) = clip_reference(node) else {
            continue;
        };
        let target = document
            .descendants()
            .find(|n| n.is_element() && n.attribute("id") == Some(id.as_str()));
        match target {
            None => {
                return Err(format!(
                    "Clip-Pfad „#{id}“ ist nicht vorhanden; Clipping entfernen oder Objekt vorher in Pfade umwandeln"
                ));
            }
            Some(target) if target.tag_name().name() != "clipPath" => {
                return Err(format!("„#{id}“ ist kein Clip-Pfad"));
            }
            Some(target) if !target.children().any(|child| child.is_element()) => {
                return Err(EMPTY_CLIP.into());
            }
            Some(_) => {}
        }
    }
    Ok(())
}

/// Id of the element referenced by `clip-path`, as attribute or style property.
fn clip_reference(node: usvg::roxmltree::Node) -> Option<String> {
    let value = node.attribute("clip-path").or_else(|| {
        node.attribute("style")?.split(';').find_map(|declaration| {
            let (name, value) = declaration.split_once(':')?;
            (name.trim() == "clip-path").then(|| value.trim())
        })
    })?;
    let id = value
        .strip_prefix("url(")?
        .strip_suffix(')')?
        .trim()
        .trim_matches(|c| c == '\'' || c == '"')
        .strip_prefix('#')?;
    Some(id.to_string())
}

/// Region of a clipPath in mm; `base` maps the clipPath's user space (the
/// referencing element's transform) to user units.
fn clip_region(
    clip: &usvg::ClipPath,
    base: tiny_skia::Transform,
    project: &Project,
    size: usvg::Size,
) -> Result<Clip, String> {
    if clip.clip_path().is_some() {
        return Err(NESTED_CLIP.into());
    }
    let mut shapes = Vec::new();
    clip_shapes(
        clip.root(),
        base.pre_concat(clip.transform()),
        project,
        size,
        &mut shapes,
    )?;
    if shapes.is_empty() {
        return Err(EMPTY_CLIP.into());
    }
    Ok(Clip { shapes })
}

fn clip_shapes(
    group: &usvg::Group,
    base: tiny_skia::Transform,
    project: &Project,
    size: usvg::Size,
    shapes: &mut Vec<ClipShape>,
) -> Result<(), String> {
    reject_effects(group)?;
    if group.clip_path().is_some() {
        return Err(NESTED_CLIP.into());
    }
    for node in group.children() {
        match node {
            usvg::Node::Group(group) => clip_shapes(group, base, project, size, shapes)?,
            // The clip geometry counts whatever its fill or stroke, like SVG's clip-path.
            usvg::Node::Path(path) if path.is_visible() => {
                let rings: Vec<Contour> = flatten_path(
                    path.data(),
                    base.pre_concat(path.abs_transform()),
                    project,
                    size,
                )
                .into_iter()
                .map(|(mut ring, _)| {
                    if ring.first() != ring.last() {
                        if let Some(first) = ring.first().copied() {
                            ring.push(first);
                        }
                    }
                    ring
                })
                .filter(|ring| ring.len() >= 4)
                .collect();
                if !rings.is_empty() {
                    let even_odd = path
                        .fill()
                        .is_some_and(|fill| fill.rule() == usvg::FillRule::EvenOdd);
                    shapes.push(ClipShape::new(rings, even_odd));
                }
            }
            usvg::Node::Image(_) => return Err(CLIP_IMAGE.into()),
            usvg::Node::Text(_) => return Err(CLIP_TEXT.into()),
            _ => {}
        }
    }
    Ok(())
}

fn visit(
    group: &usvg::Group,
    project: &Project,
    size: usvg::Size,
    clips: &[Clip],
    result: &mut Vec<Contour>,
) -> Result<(), String> {
    reject_effects(group)?;
    let stacked: Vec<Clip>;
    let clips = match group.clip_path() {
        Some(clip) => {
            let mut all = clips.to_vec();
            all.push(clip_region(clip, group.abs_transform(), project, size)?);
            stacked = all;
            stacked.as_slice()
        }
        None => clips,
    };
    if group.opacity().get() == 0.0 {
        return Ok(());
    }
    for node in group.children() {
        match node {
            usvg::Node::Group(group) => visit(group, project, size, clips, result)?,
            usvg::Node::Path(path) if path.is_visible() => {
                if path.fill().is_none_or(|fill| fill.opacity().get() == 0.0)
                    && path
                        .stroke()
                        .is_none_or(|stroke| stroke.opacity().get() == 0.0)
                {
                    continue;
                }
                let subpaths = flatten_path(path.data(), path.abs_transform(), project, size);
                // Like Java's SVGShape/DashedShape: the dash pattern is applied in the path's
                // user space, so it scales with the element's transform and the viewBox.
                let dash = path.stroke().and_then(|stroke| {
                    let pattern = dash_pattern(stroke.dasharray()?)?;
                    let metric = user_length_metric(path.abs_transform(), project, size)?;
                    Some((pattern, f64::from(stroke.dashoffset()), metric))
                });
                let start = result.len();
                for (contour, closed) in subpaths {
                    if contour.len() < 2 {
                        continue;
                    }
                    match &dash {
                        Some((pattern, offset, metric)) => {
                            dash_contour(&contour, closed, pattern, *offset, metric, result)?
                        }
                        None => result.push(contour),
                    }
                }
                if !clips.is_empty() {
                    for piece in result.split_off(start) {
                        result.extend(clip_contour(&piece, clips)?);
                    }
                }
            }
            usvg::Node::Image(_) => {
                return Err(
                    "Schneiden unterstützt keine Rasterbilder; bitte Gravieren wählen".into(),
                );
            }
            // `contours` re-imports text as paths before visiting.
            usvg::Node::Text(_) => {
                return Err("Text konnte nicht in Pfade umgewandelt werden".into());
            }
            _ => {}
        }
    }
    Ok(())
}

/// Flattens the subpaths of a path into mm contours, each with its closed flag.
/// `transform` maps the path's coordinates to user units of the whole document.
fn flatten_path(
    data: &tiny_skia::Path,
    transform: tiny_skia::Transform,
    project: &Project,
    size: usvg::Size,
) -> Vec<(Contour, bool)> {
    let map = |point: tiny_skia::Point| {
        let mut point = point;
        transform.map_point(&mut point);
        [
            project.x_mm + point.x * project.width_mm / size.width(),
            project.y_mm + point.y * project.height_mm / size.height(),
        ]
    };
    let mut subpaths = Vec::new();
    let mut contour = Vec::new();
    let mut closed = false;
    let mut current = [0.0; 2];
    let mut start = [0.0; 2];
    for segment in data.segments() {
        if !matches!(segment, tiny_skia::PathSegment::MoveTo(_)) {
            closed = false;
        }
        match segment {
            tiny_skia::PathSegment::MoveTo(point) => {
                subpaths.push((std::mem::take(&mut contour), closed));
                closed = false;
                current = map(point);
                start = current;
                contour.push(current);
            }
            tiny_skia::PathSegment::LineTo(point) => {
                current = map(point);
                contour.push(current);
            }
            tiny_skia::PathSegment::QuadTo(a, end) => {
                let a = map(a);
                let end = map(end);
                let c1 = mix(current, a, 2.0 / 3.0);
                let c2 = mix(end, a, 2.0 / 3.0);
                flatten([current, c1, c2, end], 0, &mut contour);
                current = end;
            }
            tiny_skia::PathSegment::CubicTo(a, b, end) => {
                let end = map(end);
                flatten([current, map(a), map(b), end], 0, &mut contour);
                current = end;
            }
            tiny_skia::PathSegment::Close => {
                contour.push(start);
                current = start;
                closed = true;
            }
        }
    }
    subpaths.push((contour, closed));
    subpaths
}

/// Upper bound for contours a dash pattern may add; every contour has at least two
/// points, so this matches the global limit of one million points.
const MAX_DASH_CONTOURS: usize = 500_000;

/// Normalizes `stroke-dasharray` per SVG: odd lists repeat, negative or all-zero lists
/// mean a solid stroke.
fn dash_pattern(dasharray: &[f32]) -> Option<Vec<f64>> {
    if dasharray.is_empty() || dasharray.iter().any(|v| !v.is_finite() || *v < 0.0) {
        return None;
    }
    let mut pattern: Vec<f64> = dasharray.iter().map(|v| f64::from(*v)).collect();
    if pattern.iter().sum::<f64>() <= 0.0 {
        return None;
    }
    if pattern.len() % 2 == 1 {
        pattern.extend_from_within(..);
    }
    Some(pattern)
}

/// Inverse of the linear part of the user space → mm mapping, used to measure mm
/// segments in the path's user units. `None` for a degenerate transform.
fn user_length_metric(
    transform: tiny_skia::Transform,
    project: &Project,
    size: usvg::Size,
) -> Option<[f64; 4]> {
    let scale_x = f64::from(project.width_mm) / f64::from(size.width());
    let scale_y = f64::from(project.height_mm) / f64::from(size.height());
    let a = scale_x * f64::from(transform.sx);
    let b = scale_x * f64::from(transform.kx);
    let c = scale_y * f64::from(transform.ky);
    let d = scale_y * f64::from(transform.sy);
    let det = a * d - b * c;
    if !det.is_finite() || det.abs() < 1e-12 {
        return None;
    }
    Some([d / det, -b / det, -c / det, a / det])
}

fn user_distance(metric: &[f64; 4], a: Point, b: Point) -> f64 {
    let dx = f64::from(b[0] - a[0]);
    let dy = f64::from(b[1] - a[1]);
    (metric[0] * dx + metric[1] * dy).hypot(metric[2] * dx + metric[3] * dy)
}

/// Splits a flattened subpath (in mm) into its dashes. Lengths are measured in user
/// units via `metric`; the pattern restarts at every subpath, and on closed subpaths
/// the last dash continues into the first one across the start point.
fn dash_contour(
    contour: &[Point],
    closed: bool,
    pattern: &[f64],
    offset: f64,
    metric: &[f64; 4],
    result: &mut Vec<Contour>,
) -> Result<(), String> {
    let period: f64 = pattern.iter().sum();
    let lengths: Vec<f64> = contour
        .windows(2)
        .map(|w| user_distance(metric, w[0], w[1]))
        .collect();
    let total: f64 = lengths.iter().sum();
    if !total.is_finite() || total <= 0.0 {
        return Ok(());
    }
    let estimate = (total / period).ceil() * (pattern.len() / 2) as f64 + 1.0;
    if !estimate.is_finite() || result.len() as f64 + estimate > MAX_DASH_CONTOURS as f64 {
        return Err("Strichmuster erzeugt zu viele Vektorpunkte".into());
    }

    let mut index = 0;
    let mut phase = offset.rem_euclid(period);
    for _ in 0..2 * pattern.len() {
        if phase < pattern[index] {
            break;
        }
        phase -= pattern[index];
        index = (index + 1) % pattern.len();
    }
    let mut remaining = (pattern[index] - phase).max(0.0);
    let on_at_start = index % 2 == 0;
    let epsilon = period * 1e-6;

    let mut dashes: Vec<Contour> = Vec::new();
    let mut dash: Contour = Vec::new();
    if on_at_start {
        dash.push(contour[0]);
    }
    for (segment, &length) in contour.windows(2).zip(&lengths) {
        let (a, b) = (segment[0], segment[1]);
        if length <= 0.0 {
            continue;
        }
        let mut t = 0.0;
        while length - t > remaining + epsilon {
            t += remaining;
            let point = mix(a, b, (t / length) as f32);
            if dash.last() != Some(&point) {
                dash.push(point);
            }
            if index % 2 == 0 {
                dashes.push(std::mem::take(&mut dash));
            }
            index = (index + 1) % pattern.len();
            remaining = pattern[index];
        }
        remaining = (remaining - (length - t)).max(0.0);
        if index % 2 == 0 && dash.last() != Some(&b) {
            dash.push(b);
        }
    }
    if index % 2 == 0 {
        if closed && on_at_start && !dashes.is_empty() {
            let first = dashes.remove(0);
            dash.extend_from_slice(&first[1..]);
        }
        dashes.push(dash);
    }
    result.extend(dashes.into_iter().filter(|dash| dash.len() > 1));
    Ok(())
}

fn mix(a: Point, b: Point, t: f32) -> Point {
    [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]
}

fn flatten(p: [Point; 4], depth: u32, out: &mut Contour) {
    // Control-polygon excess catches loops as well as curves along a straight chord.
    let distance = |a: Point, b: Point| (a[0] - b[0]).hypot(a[1] - b[1]);
    let excess =
        distance(p[0], p[1]) + distance(p[1], p[2]) + distance(p[2], p[3]) - distance(p[0], p[3]);
    let chord_distance = |a: Point| {
        let dx = p[3][0] - p[0][0];
        let dy = p[3][1] - p[0][1];
        ((a[0] - p[0][0]) * dy - (a[1] - p[0][1]) * dx).abs() / dx.hypot(dy).max(1e-9)
    };
    if depth >= 16 || (excess <= 0.025 && chord_distance(p[1]).max(chord_distance(p[2])) <= 0.025) {
        out.push(p[3]);
        return;
    }
    let a = mix(p[0], p[1], 0.5);
    let b = mix(p[1], p[2], 0.5);
    let c = mix(p[2], p[3], 0.5);
    let d = mix(a, b, 0.5);
    let e = mix(b, c, 0.5);
    let f = mix(d, e, 0.5);
    flatten([p[0], a, d, f], depth + 1, out);
    flatten([f, e, c, p[3]], depth + 1, out);
}

#[cfg(test)]
mod dash_tests {
    use super::*;

    /// Default project: 100×60 user units map to 100×60 mm at offset (10, 10).
    fn cut(body: &str) -> Result<Vec<Contour>, String> {
        cut_with_viewbox("", body)
    }

    fn cut_with_viewbox(view_box: &str, body: &str) -> Result<Vec<Contour>, String> {
        let svg = format!(
            r#"<svg xmlns="http://www.w3.org/2000/svg" width="100" height="60" {view_box}>{body}</svg>"#
        );
        contours(&Project {
            svg,
            ..Default::default()
        })
    }

    fn length(contour: &Contour) -> f32 {
        contour
            .windows(2)
            .map(|w| (w[1][0] - w[0][0]).hypot(w[1][1] - w[0][1]))
            .sum()
    }

    fn assert_dashes(paths: &[Contour], expected: &[(f32, f32)]) {
        assert_eq!(paths.len(), expected.len(), "{paths:?}");
        for (path, &(start, len)) in paths.iter().zip(expected) {
            assert!((path[0][0] - 10.0 - start).abs() < 1e-3, "{paths:?}");
            assert!((length(path) - len).abs() < 1e-3, "{paths:?}");
        }
    }

    fn line(attributes: &str) -> String {
        format!(r#"<path d="M0 0H30" fill="none" stroke="black" {attributes}/>"#)
    }

    #[test]
    fn splits_a_line_into_dashes() {
        let paths = cut(&line(r#"stroke-dasharray="5 5""#)).unwrap();
        assert_dashes(&paths, &[(0.0, 5.0), (10.0, 5.0), (20.0, 5.0)]);
        assert!(paths.iter().flatten().all(|p| (p[1] - 10.0).abs() < 1e-6));
    }

    #[test]
    fn keeps_dashes_and_transforms_when_text_is_present() {
        let body = format!(
            r#"<g transform="translate(0 20)">{}</g><text x="50" y="50" font-size="10">H</text>"#,
            line(r#"stroke-dasharray="5 5""#)
        );
        let paths = cut(&body).unwrap();
        let dashes: Vec<_> = paths.iter().filter(|p| p[0][1] < 40.0).cloned().collect();
        assert_dashes(&dashes, &[(0.0, 5.0), (10.0, 5.0), (20.0, 5.0)]);
        assert!(dashes.iter().flatten().all(|p| (p[1] - 30.0).abs() < 1e-3));
        assert!(paths.len() > dashes.len());
    }

    #[test]
    fn honours_positive_and_negative_dashoffset() {
        let paths = cut(&line(r#"stroke-dasharray="5 5" stroke-dashoffset="2""#)).unwrap();
        assert_dashes(&paths, &[(0.0, 3.0), (8.0, 5.0), (18.0, 5.0), (28.0, 2.0)]);
        let paths = cut(&line(r#"stroke-dasharray="5 5" stroke-dashoffset="-2""#)).unwrap();
        assert_dashes(&paths, &[(2.0, 5.0), (12.0, 5.0), (22.0, 5.0)]);
        // Offsets larger than the pattern wrap around.
        let paths = cut(&line(r#"stroke-dasharray="5 5" stroke-dashoffset="22""#)).unwrap();
        assert_dashes(&paths, &[(0.0, 3.0), (8.0, 5.0), (18.0, 5.0), (28.0, 2.0)]);
    }

    #[test]
    fn repeats_odd_dasharrays() {
        let paths = cut(&line(r#"stroke-dasharray="5""#)).unwrap();
        assert_dashes(&paths, &[(0.0, 5.0), (10.0, 5.0), (20.0, 5.0)]);
        // "4 2 1" becomes "4 2 1 4 2 1": on 4, off 2, on 1, off 4, on 2, off 1.
        let paths = cut(&line(r#"stroke-dasharray="4,2,1""#)).unwrap();
        assert_dashes(
            &paths,
            &[
                (0.0, 4.0),
                (6.0, 1.0),
                (11.0, 2.0),
                (14.0, 4.0),
                (20.0, 1.0),
                (25.0, 2.0),
                (28.0, 2.0),
            ],
        );
    }

    #[test]
    fn dashes_run_continuously_around_closed_shapes() {
        let paths = cut(r#"<rect width="10" height="10" fill="none" stroke="black" stroke-dasharray="6 4" stroke-dashoffset="3"/>"#).unwrap();
        // Perimeter 40: [0,3] and [37,40] join into one dash across the start corner.
        assert_eq!(paths.len(), 4, "{paths:?}");
        assert!(
            paths.iter().all(|p| (length(p) - 6.0).abs() < 1e-3),
            "{paths:?}"
        );
        let across_start = paths
            .iter()
            .find(|p| p[1..p.len() - 1].contains(&[10.0, 10.0]))
            .expect("one dash passes through the start point");
        assert_eq!(across_start.first(), Some(&[10.0, 13.0]));
        assert_eq!(across_start.last(), Some(&[13.0, 10.0]));

        // A dash longer than the perimeter keeps the whole closed outline.
        let paths = cut(
            r#"<rect width="10" height="10" fill="none" stroke="black" stroke-dasharray="50 5"/>"#,
        )
        .unwrap();
        assert_eq!(paths.len(), 1);
        assert_eq!(paths[0].first(), paths[0].last());
        assert!((length(&paths[0]) - 40.0).abs() < 1e-3);
    }

    #[test]
    fn scales_dash_lengths_with_viewbox_and_transforms() {
        // viewBox halves the user space: 5 user units become 10 mm.
        let paths = cut_with_viewbox(
            r#"viewBox="0 0 50 30""#,
            r#"<path d="M0 0H20" fill="none" stroke="black" stroke-dasharray="5 5"/>"#,
        )
        .unwrap();
        assert_dashes(&paths, &[(0.0, 10.0), (20.0, 10.0)]);
        let paths = cut(&format!(
            r#"<g transform="scale(2)">{}</g>"#,
            line(r#"stroke-dasharray="5 5""#)
        ))
        .unwrap();
        assert_dashes(&paths, &[(0.0, 10.0), (20.0, 10.0), (40.0, 10.0)]);
    }

    #[test]
    fn stays_solid_without_a_usable_dasharray() {
        for attributes in [
            "",
            r#"stroke-dasharray="none""#,
            r#"stroke-dasharray="0 0""#,
            r#"stroke-dasharray="0""#,
            r#"stroke-dasharray="5 -1""#,
        ] {
            let paths = cut(&line(attributes)).unwrap();
            assert_dashes(&paths, &[(0.0, 30.0)]);
        }
        // dasharray only affects strokes; a fill-only shape is cut along its full outline.
        let paths =
            cut(r#"<rect width="10" height="10" fill="black" stroke-dasharray="2 2"/>"#).unwrap();
        assert_eq!(paths.len(), 1);
        assert!((length(&paths[0]) - 40.0).abs() < 1e-3);
    }

    #[test]
    fn drops_zero_length_dashes_and_rejects_explosive_patterns() {
        let paths = cut(&line(r#"stroke-dasharray="0 10""#)).unwrap_or_default();
        assert!(paths.is_empty());
        assert!(cut(&line(r#"stroke-dasharray="0.00001""#)).is_err());
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn transforms_curves_and_closes_shapes() {
        let project = Project {
            svg: include_str!("../examples/demo.svg").into(),
            ..Default::default()
        };
        let paths = contours(&project).unwrap();
        assert_eq!(paths.len(), 3);
        assert!(paths[1].len() > 30);
        assert_eq!(paths[1].first(), paths[1].last());
        assert!(paths.iter().flatten().all(|p| p[0] >= 10.0 && p[1] >= 10.0));
    }

    #[test]
    fn applies_viewbox_and_parent_transform() {
        let project = Project { svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="60mm" viewBox="0 0 100 60"><g transform="translate(20 5)"><path d="M0 0L10 10" fill="none" stroke="black"/></g></svg>"#.into(), ..Default::default() };
        let paths = contours(&project).unwrap();
        assert!((paths[0][0][0] - 30.0).abs() < 0.001);
        assert!((paths[0][0][1] - 15.0).abs() < 0.001);
        assert!((paths[0][1][0] - 40.0).abs() < 0.001);
    }

    #[test]
    fn cuts_text_along_glyph_outlines_in_mm() {
        let project = Project { svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="60mm" viewBox="0 0 100 60"><g transform="translate(20 30) scale(2)"><text x="0" y="0" font-family="sans-serif" font-size="10" fill="none" stroke="red">H</text></g><rect x="90" y="50" width="5" height="5"/></svg>"#.into(), x_mm: 0.0, y_mm: 0.0, width_mm: 100.0, height_mm: 60.0, ..Default::default() };
        let paths = contours(&project).unwrap();
        let glyph: Vec<Point> = paths
            .iter()
            .flatten()
            .copied()
            .filter(|p| p[0] < 80.0)
            .collect();
        assert!(!glyph.is_empty());
        let (min_x, max_x) = glyph
            .iter()
            .fold((f32::MAX, f32::MIN), |(a, b), p| (a.min(p[0]), b.max(p[0])));
        let (min_y, max_y) = glyph
            .iter()
            .fold((f32::MAX, f32::MIN), |(a, b), p| (a.min(p[1]), b.max(p[1])));
        // A 20 mm "H" sits on the baseline at y = 30 mm, starting at x = 20 mm.
        assert!(min_x >= 20.0 && max_x < 40.0, "x {min_x}..{max_x}");
        assert!(max_y <= 30.5 && max_y > 29.0, "y max {max_y}");
        assert!(min_y > 10.0 && min_y < 25.0, "y min {min_y}");
        assert!(paths.iter().any(|p| p.iter().all(|q| q[0] >= 90.0)));
    }

    #[test]
    fn reports_text_without_font() {
        let project = Project { svg: r#"<svg xmlns="http://www.w3.org/2000/svg" width="100" height="60"><text x="10" y="20">Job</text><rect width="10" height="10"/></svg>"#.into(), ..Default::default() };
        let error =
            contours_with_fonts(&project, Arc::new(usvg::fontdb::Database::new())).unwrap_err();
        assert!(error.contains("Schriftart"), "{error}");
    }

    #[test]
    fn reports_characters_missing_from_every_font() {
        let mut fonts = usvg::fontdb::Database::new();
        fonts.load_font_data(epaint_default_fonts::UBUNTU_LIGHT.to_vec());
        let project = Project { svg: "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"60\"><text x=\"10\" y=\"20\" font-family=\"Ubuntu\">A \u{10FFFD}</text></svg>".into(), ..Default::default() };
        let error = contours_with_fonts(&project, Arc::new(fonts)).unwrap_err();
        assert!(error.contains("U+10FFFD"), "{error}");
    }

    #[test]
    fn bundled_fonts_cover_generic_families() {
        let fonts = crate::svg::fonts();
        for family in [
            usvg::fontdb::Family::Serif,
            usvg::fontdb::Family::SansSerif,
            usvg::fontdb::Family::Monospace,
            usvg::fontdb::Family::Cursive,
            usvg::fontdb::Family::Fantasy,
        ] {
            let query = usvg::fontdb::Query {
                families: &[family],
                ..Default::default()
            };
            assert!(fonts.query(&query).is_some(), "{family:?}");
        }
    }
}

#[cfg(test)]
mod clip_tests {
    use super::*;

    /// Default project: 100×60 user units at offset (10, 10) mm.
    fn cut(body: &str) -> Result<Vec<Contour>, String> {
        contours(&Project {
            svg: format!(
                r#"<svg xmlns="http://www.w3.org/2000/svg" width="100" height="60">{body}</svg>"#
            ),
            ..Default::default()
        })
    }

    fn length(contour: &Contour) -> f32 {
        contour
            .windows(2)
            .map(|w| (w[1][0] - w[0][0]).hypot(w[1][1] - w[0][1]))
            .sum()
    }

    /// Clip square covering user units 0..20, i.e. mm 10..30.
    const CLIP: &str = r#"<defs><clipPath id="c"><rect width="20" height="20"/></clipPath></defs>"#;

    #[test]
    fn keeps_the_part_of_a_square_inside_the_clip() {
        // The square spans mm 20..60; the clip covers mm 10..30 in both axes.
        let paths = cut(&format!(
            r#"{CLIP}<rect x="10" y="10" width="40" height="40" fill="none" stroke="black" clip-path="url(#c)"/>"#
        ))
        .unwrap();
        // Top and left edge inside the clip, 10 mm each.
        assert_eq!(paths.len(), 1, "{paths:?}");
        assert!((length(&paths[0]) - 20.0).abs() < 1e-3, "{paths:?}");
        assert!(
            paths
                .iter()
                .flatten()
                .all(|p| (19.99..=30.01).contains(&p[0]) && (19.99..=30.01).contains(&p[1]))
        );
    }

    #[test]
    fn keeps_closed_shapes_that_lie_inside_unchanged() {
        // User 2..7 is mm 12..17: entirely inside, so the closed outline stays whole.
        let paths = cut(&format!(
            r#"{CLIP}<rect x="2" y="2" width="5" height="5" fill="none" stroke="black" clip-path="url(#c)"/>"#
        ))
        .unwrap();
        assert_eq!(paths.len(), 1);
        assert_eq!(paths[0].first(), paths[0].last());
        assert!((length(&paths[0]) - 20.0).abs() < 1e-3);
    }

    #[test]
    fn path_entirely_outside_the_clip_gives_nothing() {
        // User 70..90 is mm 80..100, outside the clip.
        let error = cut(&format!(
            r#"{CLIP}<path d="M70 5H90" stroke="black" clip-path="url(#c)"/>"#
        ))
        .unwrap_err();
        assert!(error.contains("Keine schneidbaren"), "{error}");

        // Inside the group, the outside path is dropped and the inside one kept.
        let paths = cut(&format!(
            r#"{CLIP}<g clip-path="url(#c)"><path d="M70 5H90" stroke="black"/><path d="M5 5H15" stroke="black"/></g>"#
        ))
        .unwrap();
        assert_eq!(paths.len(), 1);
        assert!((length(&paths[0]) - 10.0).abs() < 1e-3);
    }

    #[test]
    fn empty_clip_path_is_an_error() {
        let error = cut(
            r#"<defs><clipPath id="c"/></defs><rect width="10" height="10" clip-path="url(#c)"/>"#,
        )
        .unwrap_err();
        assert!(error.contains("leer"), "{error}");
    }

    #[test]
    fn missing_clip_path_is_an_error() {
        let error = cut(r#"<rect width="10" height="10" clip-path="url(#missing)"/>"#).unwrap_err();
        assert!(error.contains("nicht vorhanden"), "{error}");
    }

    #[test]
    fn masks_and_filters_are_rejected_with_reasons() {
        let mask = cut(
            r#"<defs><mask id="m"><rect width="50" height="50" fill="white"/></mask></defs><rect width="10" height="10" mask="url(#m)"/>"#,
        )
        .unwrap_err();
        assert!(
            mask.contains("Masken") && mask.contains("Transparenz"),
            "{mask}"
        );

        let filter = cut(
            r#"<defs><filter id="f"><feGaussianBlur stdDeviation="2"/></filter></defs><rect width="10" height="10" filter="url(#f)"/>"#,
        )
        .unwrap_err();
        assert!(
            filter.contains("Filter") && filter.contains("Pixelbild"),
            "{filter}"
        );
    }
}
