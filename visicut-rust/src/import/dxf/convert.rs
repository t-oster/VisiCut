//! DXF entities → paths and texts in world millimetres (Y up).
use super::colors::aci;
use super::geom::{Affine, P, PathBuilder, Seg, full_turn};
use super::reader::{Document, Entity, Pair, number};
use super::text;
use crate::{i18n::tr, tr};
use std::collections::{BTreeMap, BTreeSet};
use std::f64::consts::TAU;

pub enum Prim {
    Path {
        layer: String,
        stroke: Option<u32>,
        fill: Option<u32>,
        /// Stroke width in mm.
        width: f64,
        dash: Option<Vec<f64>>,
        evenodd: bool,
        segs: Vec<Seg>,
    },
    Text {
        layer: String,
        fill: u32,
        /// SVG text coordinates (Y down) → world millimetres.
        m: Affine,
        size: f64,
        anchor: &'static str,
        lines: Vec<String>,
        first_baseline: f64,
        line_height: f64,
    },
}

impl Prim {
    pub fn layer(&self) -> &str {
        match self {
            Prim::Path { layer, .. } | Prim::Text { layer, .. } => layer,
        }
    }
}

pub struct Converted {
    pub prims: Vec<Prim>,
    pub warnings: Vec<String>,
}

const MAX_PRIMITIVES: usize = 500_000;
const MAX_DEPTH: u32 = 16;
/// Cap height of typical fonts relative to the font size; DXF text height is
/// the cap height.
const CAP_HEIGHT: f64 = 0.7;

/// Inherited attributes of the INSERT (or DIMENSION) a block is drawn by.
#[derive(Clone)]
struct Ctx {
    m: Affine,
    layer: Option<String>,
    color: u32,
    lineweight: f64,
    linetype: String,
    depth: u32,
}

struct Style {
    color: u32,
    lineweight: f64,
    linetype: String,
    /// Entity linetype scale (code 48).
    ltscale: f64,
}

struct Converter<'a> {
    doc: &'a Document,
    ltscale: f64,
    prims: Vec<Prim>,
    unsupported: BTreeMap<String, usize>,
    hidden: usize,
    missing_blocks: BTreeSet<String>,
    pattern_hatch: bool,
    too_deep: bool,
}

pub fn convert(doc: &Document, unit_mm: f64) -> Result<Converted, String> {
    let mut converter = Converter {
        doc,
        ltscale: doc
            .header_f("$LTSCALE", 40)
            .filter(|s| *s > 0.0)
            .unwrap_or(1.0),
        prims: Vec::new(),
        unsupported: BTreeMap::new(),
        hidden: 0,
        missing_blocks: BTreeSet::new(),
        pattern_hatch: false,
        too_deep: false,
    };
    let ctx = Ctx {
        m: Affine::scale(unit_mm, unit_mm),
        layer: None,
        color: 0,
        lineweight: 0.25,
        linetype: "CONTINUOUS".into(),
        depth: 0,
    };
    // Model space only, unless the drawing has nothing else.
    let mut entities: Vec<&Entity> = doc
        .entities
        .iter()
        .filter(|e| e.int(67) != Some(1))
        .collect();
    if entities.is_empty() {
        entities = doc.entities.iter().collect();
    }
    if entities.is_empty()
        && let Some(block) = doc.block("*Model_Space")
    {
        entities = block.entities.iter().collect();
    }
    for entity in entities {
        converter.entity(entity, &ctx)?;
    }
    let mut warnings = Vec::new();
    if !converter.unsupported.is_empty() {
        let list: Vec<String> = converter
            .unsupported
            .iter()
            .map(|(kind, n)| format!("{kind} ({n})"))
            .collect();
        warnings.push(tr!(
            "Nicht unterstützte DXF-Objekte wurden ausgelassen: {list}",
            list = list.join(", ")
        ));
    }
    if converter.hidden > 0 {
        warnings.push(tr!(
            "{count} Objekte auf ausgeschalteten oder gefrorenen Ebenen wurden ausgelassen",
            count = converter.hidden
        ));
    }
    for name in &converter.missing_blocks {
        warnings.push(tr!("Block „{name}“ fehlt in der DXF-Datei", name = name));
    }
    if converter.pattern_hatch {
        warnings.push(tr("Schraffurmuster werden als gefüllte Flächen übernommen").into());
    }
    if converter.too_deep {
        warnings.push(tr("Zu tief verschachtelte Blöcke wurden ausgelassen").into());
    }
    Ok(Converted {
        prims: converter.prims,
        warnings,
    })
}

fn point(e: &Entity, code: i32) -> P {
    [e.fd(code, 0.0), e.fd(code + 10, 0.0)]
}

fn point3(e: &Entity, code: i32, default: [f64; 3]) -> [f64; 3] {
    [
        e.fd(code, default[0]),
        e.fd(code + 10, default[1]),
        e.fd(code + 20, default[2]),
    ]
}

fn cross(a: [f64; 3], b: [f64; 3]) -> [f64; 3] {
    [
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    ]
}

fn normalize(v: [f64; 3]) -> Option<[f64; 3]> {
    let length = (v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).sqrt();
    (length > 1e-12 && length.is_finite()).then(|| v.map(|c| c / length))
}

fn extrusion(e: &Entity) -> [f64; 3] {
    normalize(point3(e, 210, [0.0, 0.0, 1.0])).unwrap_or([0.0, 0.0, 1.0])
}

/// Object coordinate system (arbitrary axis algorithm) projected onto the
/// XY plane; `z` is the elevation along the extrusion direction.
fn ocs(e: &Entity, z: f64) -> Affine {
    let n = extrusion(e);
    let ax = if n[0].abs() < 1.0 / 64.0 && n[1].abs() < 1.0 / 64.0 {
        cross([0.0, 1.0, 0.0], n)
    } else {
        cross([0.0, 0.0, 1.0], n)
    };
    let ax = normalize(ax).unwrap_or([1.0, 0.0, 0.0]);
    let ay = normalize(cross(n, ax)).unwrap_or([0.0, 1.0, 0.0]);
    Affine {
        a: ax[0],
        b: ax[1],
        c: ay[0],
        d: ay[1],
        e: n[0] * z,
        f: n[1] * z,
    }
}

/// `(start, sweep)` in radians for an arc from `start` to `end` degrees.
fn sweep(start: f64, end: f64) -> (f64, f64) {
    let start = start.to_radians();
    let span = (end.to_radians() - start).rem_euclid(TAU);
    (start, if span < 1e-9 { TAU } else { span })
}

fn polyline(b: &mut PathBuilder, vertices: &[(P, f64)], closed: bool) {
    let Some(first) = vertices.first() else {
        return;
    };
    b.break_path();
    b.move_to(first.0);
    for pair in vertices.windows(2) {
        b.bulge_to(pair[0].0, pair[1].0, pair[0].1);
    }
    if closed && vertices.len() > 1 {
        let last = vertices[vertices.len() - 1];
        if last.0 != first.0 || last.1 != 0.0 {
            b.bulge_to(last.0, first.0, last.1);
        }
        b.close();
    }
}

/// Point of a (rational) B-spline by de Boor's algorithm.
fn de_boor(t: f64, p: usize, knots: &[f64], ctrl: &[P], weights: &[f64]) -> P {
    let n = ctrl.len();
    let mut k = p;
    while k < n - 1 && t >= knots[k + 1] {
        k += 1;
    }
    let mut d: Vec<[f64; 3]> = (0..=p)
        .map(|j| {
            let i = j + k - p;
            let w = weights[i];
            [ctrl[i][0] * w, ctrl[i][1] * w, w]
        })
        .collect();
    for r in 1..=p {
        for j in (r..=p).rev() {
            let i = j + k - p;
            let denominator = knots[i + p + 1 - r] - knots[i];
            let alpha = if denominator.abs() < 1e-12 {
                0.0
            } else {
                (t - knots[i]) / denominator
            };
            d[j] = [0, 1, 2].map(|c| (1.0 - alpha) * d[j - 1][c] + alpha * d[j][c]);
        }
    }
    let w = if d[p][2].abs() < 1e-12 { 1.0 } else { d[p][2] };
    [d[p][0] / w, d[p][1] / w]
}

/// Draws a NURBS curve; invalid knot vectors and weights fall back to a
/// clamped uniform spline.
fn nurbs(b: &mut PathBuilder, degree: usize, ctrl: &[P], knots: &[f64], weights: &[f64]) {
    let n = ctrl.len();
    if n < 2 {
        return;
    }
    let p = degree.clamp(1, n - 1);
    let valid =
        knots.len() == n + p + 1 && knots.windows(2).all(|w| w[1] >= w[0]) && knots[n] > knots[p];
    let knots: Vec<f64> = if valid {
        knots.to_vec()
    } else {
        (0..n + p + 1)
            .map(|i| i.saturating_sub(p).min(n - p) as f64)
            .collect()
    };
    let weights: Vec<f64> = if weights.len() == n && weights.iter().all(|w| *w > 0.0) {
        weights.to_vec()
    } else {
        vec![1.0; n]
    };
    let mut domain: Vec<f64> = knots[p..=n].to_vec();
    domain.dedup();
    b.curve(&|t| de_boor(t, p, &knots, ctrl, &weights), &domain);
}

/// Smooth curve through fit points (Catmull-Rom tangents).
fn through_points(b: &mut PathBuilder, fit: &[P], start: Option<P>, end: Option<P>) {
    let n = fit.len();
    if n < 2 {
        return;
    }
    let sub = |a: P, c: P| [a[0] - c[0], a[1] - c[1]];
    let scaled = |dir: Option<P>, chord: P| {
        let length = chord[0].hypot(chord[1]);
        dir.and_then(|d| {
            let dl = d[0].hypot(d[1]);
            (dl > 1e-12).then(|| [d[0] / dl * length, d[1] / dl * length])
        })
        .unwrap_or(chord)
    };
    let tangents: Vec<P> = (0..n)
        .map(|i| match i {
            0 => scaled(start, sub(fit[1], fit[0])),
            i if i == n - 1 => scaled(end, sub(fit[n - 1], fit[n - 2])),
            i => {
                let d = sub(fit[i + 1], fit[i - 1]);
                [d[0] / 2.0, d[1] / 2.0]
            }
        })
        .collect();
    b.connect(fit[0]);
    for i in 0..n - 1 {
        let (a, c) = (fit[i], fit[i + 1]);
        b.cubic_to(
            [a[0] + tangents[i][0] / 3.0, a[1] + tangents[i][1] / 3.0],
            [
                c[0] - tangents[i + 1][0] / 3.0,
                c[1] - tangents[i + 1][1] / 3.0,
            ],
            c,
        );
    }
}

/// Converts a DXF linetype pattern to an SVG dash array in millimetres.
fn dash_array(pattern: &[f64], scale: f64) -> Option<Vec<f64>> {
    let mut out: Vec<f64> = Vec::new();
    for value in pattern {
        let on = *value >= 0.0;
        if out.is_empty() && !on {
            out.push(0.0);
        }
        let len = value.abs() * scale;
        if on == out.len().is_multiple_of(2) {
            out.push(len);
        } else if let Some(last) = out.last_mut() {
            *last += len;
        }
    }
    if !out.len().is_multiple_of(2) {
        out.push(0.0);
    }
    let gaps: f64 = out.iter().skip(1).step_by(2).sum();
    let total: f64 = out.iter().sum();
    // Patterns below 0.05 mm would only produce a cloud of dots.
    (gaps > 0.0 && total >= 0.05 && total.is_finite()).then_some(out)
}

/// Sequential reader for the HATCH boundary group codes.
struct Cursor<'a> {
    pairs: &'a [Pair],
    at: usize,
}

impl Cursor<'_> {
    fn peek(&self) -> Option<i32> {
        self.pairs.get(self.at).map(|p| p.code)
    }

    fn take(&mut self, code: i32) -> Option<f64> {
        let pair = self.pairs.get(self.at)?;
        if pair.code != code {
            return None;
        }
        self.at += 1;
        Some(number(&pair.value).unwrap_or(0.0))
    }

    fn get(&mut self, code: i32) -> f64 {
        self.take(code).unwrap_or(0.0)
    }

    fn count(&mut self, code: i32) -> usize {
        (self.get(code).max(0.0) as usize).min(1_000_000)
    }

    /// Skips forward to the next `code`.
    fn find(&mut self, code: i32) -> Option<f64> {
        while self.at < self.pairs.len() {
            if let Some(value) = self.take(code) {
                return Some(value);
            }
            self.at += 1;
        }
        None
    }

    fn point(&mut self, x: i32) -> P {
        [self.get(x), self.get(x + 10)]
    }
}

impl Converter<'_> {
    fn effective_layer(&self, e: &Entity, ctx: &Ctx) -> String {
        let name = e
            .str(8)
            .map(text::unicode_escapes)
            .filter(|n| !n.is_empty())
            .unwrap_or_else(|| "0".into());
        match &ctx.layer {
            // Block entities on layer 0 take the layer of the block reference.
            Some(parent) if name == "0" => parent.clone(),
            _ => self
                .doc
                .layer(&name)
                .map_or(name, |layer| layer.name.clone()),
        }
    }

    fn style(&self, e: &Entity, layer: &str, ctx: &Ctx) -> Style {
        let layer = self.doc.layer(layer);
        let color = match (e.int(420), e.int(62).unwrap_or(256)) {
            (Some(rgb), _) => rgb as u32 & 0xFF_FFFF,
            (None, 0) => ctx.color,
            (None, index @ 1..=255) => aci(index),
            _ => layer.map_or(0, |l| l.true_color.unwrap_or_else(|| aci(l.color.abs()))),
        };
        let weight = |value: i64| (value >= 0).then(|| value as f64 / 100.0);
        let lineweight = match e.int(370).unwrap_or(-1) {
            -2 => ctx.lineweight,
            -1 => layer.and_then(|l| weight(l.lineweight)).unwrap_or(0.25),
            value => weight(value).unwrap_or(0.25),
        };
        let linetype = match e.str(6).map(str::to_uppercase).as_deref() {
            None | Some("BYLAYER") => layer.map_or("CONTINUOUS".into(), |l| l.linetype.clone()),
            Some("BYBLOCK") => ctx.linetype.clone(),
            Some(name) => name.to_owned(),
        };
        let linetype = if linetype.eq_ignore_ascii_case("BYBLOCK") {
            ctx.linetype.clone()
        } else {
            linetype
        };
        Style {
            color,
            lineweight,
            linetype,
            ltscale: e.f(48).filter(|s| *s > 0.0).unwrap_or(1.0),
        }
    }

    fn push(&mut self, prim: Prim) -> Result<(), String> {
        if self.prims.len() >= MAX_PRIMITIVES {
            return Err(tr("DXF-Datei enthält zu viele Objekte").into());
        }
        self.prims.push(prim);
        Ok(())
    }

    fn stroke(&mut self, layer: &str, style: &Style, path: PathBuilder) -> Result<(), String> {
        if !path.has_drawing() {
            return Ok(());
        }
        let dash = self
            .doc
            .linetypes
            .get(&style.linetype.to_uppercase())
            .and_then(|pattern| {
                dash_array(
                    pattern,
                    self.ltscale * style.ltscale * path.m.scale_factor(),
                )
            });
        self.push(Prim::Path {
            layer: layer.to_owned(),
            stroke: Some(style.color),
            fill: None,
            width: style.lineweight.max(0.05),
            dash,
            evenodd: false,
            segs: path.segs,
        })
    }

    fn unsupported(&mut self, kind: &str) {
        *self.unsupported.entry(kind.to_owned()).or_default() += 1;
    }

    fn entity(&mut self, e: &Entity, ctx: &Ctx) -> Result<(), String> {
        if e.int(60) == Some(1) {
            return Ok(());
        }
        let layer = self.effective_layer(e, ctx);
        if self.doc.layer(&layer).is_some_and(|l| l.hidden()) {
            self.hidden += 1;
            return Ok(());
        }
        let style = self.style(e, &layer, ctx);
        let wcs = ctx.m;
        let in_ocs = |z: f64| ctx.m.then(ocs(e, z));
        match e.kind.as_str() {
            "LINE" => {
                let mut b = PathBuilder::new(wcs);
                b.move_to(point(e, 10));
                b.line_to(point(e, 11));
                self.stroke(&layer, &style, b)?;
            }
            "CIRCLE" | "ARC" => {
                let r = e.fd(40, 0.0);
                if r <= 0.0 {
                    return Ok(());
                }
                let mut b = PathBuilder::new(in_ocs(e.fd(30, 0.0)));
                let (start, span) = if e.kind == "ARC" {
                    sweep(e.fd(50, 0.0), e.fd(51, 360.0))
                } else {
                    (0.0, TAU)
                };
                b.arc(point(e, 10), [r, 0.0], [0.0, r], start, span);
                if full_turn(span) {
                    b.close();
                }
                self.stroke(&layer, &style, b)?;
            }
            "ELLIPSE" => {
                let major3 = point3(e, 11, [1.0, 0.0, 0.0]);
                let length = (major3[0].powi(2) + major3[1].powi(2) + major3[2].powi(2)).sqrt();
                let ratio = e.fd(40, 1.0);
                let Some(minor_dir) = normalize(cross(extrusion(e), major3)) else {
                    return Ok(());
                };
                let minor = [minor_dir[0] * length * ratio, minor_dir[1] * length * ratio];
                let t0 = e.fd(41, 0.0);
                let span = (e.fd(42, TAU) - t0).rem_euclid(TAU);
                let span = if span < 1e-9 { TAU } else { span };
                let mut b = PathBuilder::new(wcs);
                b.arc(point(e, 10), [major3[0], major3[1]], minor, t0, span);
                if full_turn(span) {
                    b.close();
                }
                self.stroke(&layer, &style, b)?;
            }
            "LWPOLYLINE" => {
                let mut vertices: Vec<(P, f64)> = Vec::new();
                for pair in &e.pairs {
                    let value = number(&pair.value).unwrap_or(0.0);
                    match pair.code {
                        10 => vertices.push(([value, 0.0], 0.0)),
                        20 => {
                            if let Some(v) = vertices.last_mut() {
                                v.0[1] = value;
                            }
                        }
                        42 => {
                            if let Some(v) = vertices.last_mut() {
                                v.1 = value;
                            }
                        }
                        _ => {}
                    }
                }
                let mut b = PathBuilder::new(in_ocs(e.fd(38, 0.0)));
                polyline(&mut b, &vertices, e.int(70).unwrap_or(0) & 1 != 0);
                self.stroke(&layer, &style, b)?;
            }
            "POLYLINE" => {
                let flags = e.int(70).unwrap_or(0);
                if flags & (16 | 64) != 0 {
                    self.unsupported(tr("POLYLINE (Netz)"));
                    return Ok(());
                }
                let is_3d = flags & 8 != 0;
                let vertices: Vec<(P, f64)> = e
                    .children
                    .iter()
                    .filter(|v| v.int(70).unwrap_or(0) & 16 == 0)
                    .map(|v| (point(v, 10), if is_3d { 0.0 } else { v.fd(42, 0.0) }))
                    .collect();
                let m = if is_3d { wcs } else { in_ocs(e.fd(30, 0.0)) };
                let mut b = PathBuilder::new(m);
                polyline(&mut b, &vertices, flags & 1 != 0);
                self.stroke(&layer, &style, b)?;
            }
            "SPLINE" => {
                let mut b = PathBuilder::new(wcs);
                let ctrl = e.points(10);
                if ctrl.len() >= 2 {
                    let knots: Vec<f64> = e.all(40).collect();
                    let weights: Vec<f64> = e.all(41).collect();
                    let degree = e.int(71).unwrap_or(3).clamp(1, 25) as usize;
                    nurbs(&mut b, degree, &ctrl, &knots, &weights);
                } else {
                    let tangent = |code| e.f(code).map(|_| point(e, code));
                    through_points(&mut b, &e.points(11), tangent(12), tangent(13));
                }
                if e.int(70).unwrap_or(0) & 1 != 0 {
                    b.close();
                }
                self.stroke(&layer, &style, b)?;
            }
            "LEADER" => {
                let mut b = PathBuilder::new(wcs);
                let vertices: Vec<(P, f64)> = e.points(10).into_iter().map(|p| (p, 0.0)).collect();
                polyline(&mut b, &vertices, false);
                self.stroke(&layer, &style, b)?;
            }
            "SOLID" | "TRACE" => {
                let mut b = PathBuilder::new(in_ocs(e.fd(30, 0.0)));
                let mut corners = vec![point(e, 10), point(e, 11)];
                if e.f(13).is_some() {
                    corners.push(point(e, 13));
                }
                corners.push(point(e, 12));
                corners.dedup();
                let vertices: Vec<(P, f64)> = corners.into_iter().map(|p| (p, 0.0)).collect();
                polyline(&mut b, &vertices, true);
                if b.has_drawing() {
                    self.push(Prim::Path {
                        layer,
                        stroke: Some(style.color),
                        fill: Some(style.color),
                        width: style.lineweight.max(0.05),
                        dash: None,
                        evenodd: false,
                        segs: b.segs,
                    })?;
                }
            }
            "3DFACE" => {
                let mut corners = vec![point(e, 10), point(e, 11), point(e, 12)];
                if e.f(13).is_some() {
                    corners.push(point(e, 13));
                }
                let hidden_edges = e.int(70).unwrap_or(0) & 15;
                let mut b = PathBuilder::new(wcs);
                if hidden_edges == 0 {
                    corners.dedup();
                    let vertices: Vec<(P, f64)> = corners.into_iter().map(|p| (p, 0.0)).collect();
                    polyline(&mut b, &vertices, true);
                } else {
                    for i in 0..corners.len() {
                        if hidden_edges & (1 << i) == 0 {
                            b.move_to(corners[i]);
                            b.line_to(corners[(i + 1) % corners.len()]);
                        }
                    }
                }
                self.stroke(&layer, &style, b)?;
            }
            "TEXT" => self.text(e, layer, &style, ctx, 73)?,
            "ATTRIB" => {
                if e.int(70).unwrap_or(0) & 1 == 0 {
                    self.text(e, layer, &style, ctx, 74)?;
                }
            }
            "MTEXT" => self.mtext(e, layer, &style, ctx)?,
            "INSERT" => self.insert(e, layer, &style, ctx)?,
            "DIMENSION" => {
                let block = e.str(2).unwrap_or_default();
                if self.doc.block(block).is_none() {
                    self.unsupported(tr("DIMENSION (ohne Block)"));
                    return Ok(());
                }
                let at = point(e, 12);
                let m = in_ocs(0.0).then(Affine::translate(at[0], at[1]));
                self.block(block, m, layer, &style, ctx)?;
            }
            "HATCH" => self.hatch(e, layer, &style, in_ocs(e.fd(30, 0.0)))?,
            "POINT" | "ATTDEF" | "VIEWPORT" | "SEQEND" | "VERTEX" => {}
            other => self.unsupported(other),
        }
        Ok(())
    }

    fn block(
        &mut self,
        name: &str,
        m: Affine,
        layer: String,
        style: &Style,
        ctx: &Ctx,
    ) -> Result<(), String> {
        let Some(block) = self.doc.block(name) else {
            self.missing_blocks.insert(name.to_owned());
            return Ok(());
        };
        if ctx.depth >= MAX_DEPTH {
            self.too_deep = true;
            return Ok(());
        }
        let child = Ctx {
            m: ctx
                .m
                .then(m)
                .then(Affine::translate(-block.base[0], -block.base[1])),
            layer: Some(layer),
            color: style.color,
            lineweight: style.lineweight,
            linetype: style.linetype.clone(),
            depth: ctx.depth + 1,
        };
        if !child.m.is_finite() {
            return Ok(());
        }
        for entity in &block.entities {
            self.entity(entity, &child)?;
        }
        Ok(())
    }

    fn insert(
        &mut self,
        e: &Entity,
        layer: String,
        style: &Style,
        ctx: &Ctx,
    ) -> Result<(), String> {
        let name = e.str(2).unwrap_or_default();
        let at = point(e, 10);
        let rotation = Affine::rotate(e.fd(50, 0.0).to_radians());
        let scale = Affine::scale(e.fd(41, 1.0), e.fd(42, 1.0));
        let columns = e.int(70).unwrap_or(1).max(1);
        let rows = e.int(71).unwrap_or(1).max(1);
        if columns.saturating_mul(rows) > 100_000 {
            return Err(tr("DXF-Datei enthält zu viele Blockreferenzen").into());
        }
        let (dx, dy) = (e.fd(44, 0.0), e.fd(45, 0.0));
        let placement = ocs(e, e.fd(30, 0.0)).then(Affine::translate(at[0], at[1]));
        for row in 0..rows {
            for column in 0..columns {
                let m = placement
                    .then(rotation)
                    .then(Affine::translate(column as f64 * dx, row as f64 * dy))
                    .then(scale);
                self.block(name, m, layer.clone(), style, ctx)?;
            }
        }
        for attribute in &e.children {
            self.entity(attribute, ctx)?;
        }
        Ok(())
    }

    fn text(
        &mut self,
        e: &Entity,
        layer: String,
        style: &Style,
        ctx: &Ctx,
        vertical_code: i32,
    ) -> Result<(), String> {
        let content = text::single_line(e.str(1).unwrap_or_default());
        if content.trim().is_empty() {
            return Ok(());
        }
        let height = e.f(40).filter(|h| *h > 0.0).unwrap_or(2.5);
        let width = e.f(41).filter(|w| *w > 0.0).unwrap_or(1.0);
        let mirror = e.int(71).unwrap_or(0);
        let horizontal = e.int(72).unwrap_or(0);
        let vertical = e.int(vertical_code).unwrap_or(0);
        let start = point(e, 10);
        let alignment = e.f(11).map(|_| point(e, 11));
        let mut rotation = e.fd(50, 0.0).to_radians();
        let (origin, anchor, vertical) = match (horizontal, alignment) {
            // Aligned and fit text run along the baseline from 10 to 11.
            (3 | 5, Some(end)) => {
                rotation = (end[1] - start[1]).atan2(end[0] - start[0]);
                (start, "start", 0)
            }
            (0, _) if vertical == 0 => (start, "start", 0),
            (_, alignment) => (
                alignment.unwrap_or(start),
                match horizontal {
                    1 | 4 => "middle",
                    2 => "end",
                    _ => "start",
                },
                if horizontal == 4 { 2 } else { vertical },
            ),
        };
        let first_baseline = match vertical {
            1 => -0.3 * height,
            2 => height / 2.0,
            3 => height,
            _ => 0.0,
        };
        let flip_x = if mirror & 2 != 0 { -1.0 } else { 1.0 };
        let flip_y = if mirror & 4 != 0 { 1.0 } else { -1.0 };
        let m = ctx
            .m
            .then(ocs(e, e.fd(30, 0.0)))
            .then(Affine::translate(origin[0], origin[1]))
            .then(Affine::rotate(rotation))
            .then(Affine::skew_x(e.fd(51, 0.0).to_radians()))
            .then(Affine::scale(width * flip_x, flip_y));
        self.push(Prim::Text {
            layer,
            fill: style.color,
            m,
            size: height / CAP_HEIGHT,
            anchor,
            lines: vec![content],
            first_baseline,
            line_height: 0.0,
        })
    }

    fn mtext(&mut self, e: &Entity, layer: String, style: &Style, ctx: &Ctx) -> Result<(), String> {
        let mut raw: String = e
            .pairs
            .iter()
            .filter(|p| p.code == 3)
            .map(|p| p.value.as_str())
            .collect();
        raw.push_str(e.str(1).unwrap_or_default());
        let mut lines = text::mtext_lines(&raw);
        while lines.last().is_some_and(|l| l.trim().is_empty()) {
            lines.pop();
        }
        if lines.is_empty() {
            return Ok(());
        }
        let height = e.f(40).filter(|h| *h > 0.0).unwrap_or(2.5);
        let line_height = height * 5.0 / 3.0 * e.f(44).filter(|f| *f > 0.0).unwrap_or(1.0);
        let n = extrusion(e);
        let x_axis = e
            .f(11)
            .and_then(|_| normalize(point3(e, 11, [1.0, 0.0, 0.0])))
            .unwrap_or_else(|| {
                let system = ocs(e, 0.0);
                let (sin, cos) = e.fd(50, 0.0).to_radians().sin_cos();
                [
                    system.a * cos + system.c * sin,
                    system.b * cos + system.d * sin,
                    0.0,
                ]
            });
        let y_axis = cross(n, x_axis);
        let at = point(e, 10);
        let basis = Affine {
            a: x_axis[0],
            b: x_axis[1],
            c: y_axis[0],
            d: y_axis[1],
            e: at[0],
            f: at[1],
        };
        let attachment = e.int(71).unwrap_or(1).clamp(1, 9) - 1;
        let block_height = (lines.len() - 1) as f64 * line_height;
        let first_baseline = match attachment / 3 {
            0 => height,
            1 => height - (block_height + height) / 2.0,
            _ => -block_height,
        };
        self.push(Prim::Text {
            layer,
            fill: style.color,
            m: ctx.m.then(basis).then(Affine::scale(1.0, -1.0)),
            size: height / CAP_HEIGHT,
            anchor: ["start", "middle", "end"][(attachment % 3) as usize],
            lines,
            first_baseline,
            line_height,
        })
    }

    fn hatch(&mut self, e: &Entity, layer: String, style: &Style, m: Affine) -> Result<(), String> {
        let Some(start) = e.pairs.iter().position(|p| p.code == 91) else {
            return Ok(());
        };
        let mut cursor = Cursor {
            pairs: &e.pairs,
            at: start,
        };
        let loops = cursor.count(91);
        let mut b = PathBuilder::new(m);
        for _ in 0..loops {
            let Some(flags) = cursor.find(92) else { break };
            b.break_path();
            if flags as i64 & 2 != 0 {
                let bulges = cursor.get(72) != 0.0;
                cursor.take(73);
                let count = cursor.count(93);
                let mut vertices = Vec::with_capacity(count);
                for _ in 0..count {
                    let p = cursor.point(10);
                    let bulge = if bulges { cursor.get(42) } else { 0.0 };
                    vertices.push((p, bulge));
                }
                polyline(&mut b, &vertices, true);
            } else {
                for _ in 0..cursor.count(93) {
                    match cursor.get(72) as i64 {
                        1 => {
                            let from = cursor.point(10);
                            let to = cursor.point(11);
                            b.connect(from);
                            b.line_to(to);
                        }
                        2 => {
                            let center = cursor.point(10);
                            let r = cursor.get(40);
                            let (s, t) = (cursor.get(50), cursor.get(51));
                            let (t0, span) = edge_sweep(s, t, cursor.get(73) != 0.0);
                            b.arc(center, [r, 0.0], [0.0, r], t0, span);
                        }
                        3 => {
                            let center = cursor.point(10);
                            let major = cursor.point(11);
                            let ratio = cursor.get(40);
                            let (s, t) = (cursor.get(50), cursor.get(51));
                            let (t0, span) = edge_sweep(s, t, cursor.get(73) != 0.0);
                            let minor = [-major[1] * ratio, major[0] * ratio];
                            b.arc(center, major, minor, t0, span);
                        }
                        4 => hatch_spline(&mut cursor, &mut b),
                        _ => break,
                    }
                }
                b.close();
            }
            if cursor.peek() == Some(97) {
                for _ in 0..cursor.count(97) {
                    cursor.take(330);
                }
            }
        }
        if !b.has_drawing() {
            return Ok(());
        }
        let solid =
            e.int(70) == Some(1) || e.str(2).is_some_and(|n| n.eq_ignore_ascii_case("SOLID"));
        if !solid {
            self.pattern_hatch = true;
        }
        self.push(Prim::Path {
            layer,
            stroke: None,
            fill: Some(style.color),
            width: 0.0,
            dash: None,
            evenodd: true,
            segs: b.segs,
        })
    }
}

/// HATCH arc edges store clockwise arcs with mirrored angles.
fn edge_sweep(start: f64, end: f64, ccw: bool) -> (f64, f64) {
    let (t0, span) = sweep(start, end);
    if ccw { (t0, span) } else { (-t0, -span) }
}

fn hatch_spline(cursor: &mut Cursor, b: &mut PathBuilder) {
    let degree = cursor.get(94).max(1.0) as usize;
    let rational = cursor.get(73) != 0.0;
    cursor.take(74);
    let knot_count = cursor.count(95);
    let ctrl_count = cursor.count(96);
    let knots: Vec<f64> = (0..knot_count).map(|_| cursor.get(40)).collect();
    let mut ctrl = Vec::with_capacity(ctrl_count);
    let mut weights = Vec::with_capacity(ctrl_count);
    for _ in 0..ctrl_count {
        ctrl.push(cursor.point(10));
        weights.push(if rational {
            cursor.take(42).unwrap_or(1.0)
        } else {
            1.0
        });
    }
    // Optional fit data (AutoCAD 2010+); a second 97 would be the source
    // boundary count of the loop.
    let next = cursor.pairs.get(cursor.at + 1).map(|p| p.code);
    if cursor.peek() == Some(97) && matches!(next, Some(11 | 97 | 12)) {
        for _ in 0..cursor.count(97) {
            cursor.point(11);
        }
        if cursor.peek() == Some(12) {
            cursor.point(12);
        }
        if cursor.peek() == Some(13) {
            cursor.point(13);
        }
    }
    nurbs(b, degree, &ctrl, &knots, &weights);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn converts_linetype_patterns() {
        assert_eq!(dash_array(&[0.5, -0.25], 2.0), Some(vec![1.0, 0.5]));
        assert_eq!(
            dash_array(&[-0.25, 0.5, 0.0, -0.1], 1.0),
            Some(vec![0.0, 0.25, 0.5, 0.1])
        );
        assert_eq!(dash_array(&[0.5, 0.0], 1.0), None);
        assert_eq!(dash_array(&[], 1.0), None);
    }

    #[test]
    fn clockwise_hatch_arcs_use_mirrored_angles() {
        // Quarter arc from 90° clockwise to 0°, stored as 270° → 360°.
        let (t0, span) = edge_sweep(270.0, 360.0, false);
        assert!((t0.to_degrees() + 270.0).abs() < 1e-9);
        assert!((span.to_degrees() + 90.0).abs() < 1e-9);
    }
}
