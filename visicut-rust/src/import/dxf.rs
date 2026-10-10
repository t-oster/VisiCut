//! DXF import (ASCII and binary, AutoCAD R12 to current versions).
//!
//! Like VisiCut's Java `DXFImporter` (kabeja → SVG) the drawing becomes an SVG
//! in millimetres whose origin is the top-left corner of the drawing's
//! bounding box. Layers turn into Inkscape layers and DXF colours (ACI and
//! true colour, BYLAYER/BYBLOCK resolved) into stroke colours, so mapping
//! rules can select by layer and colour. Unlike kabeja, `$INSUNITS` is honoured;
//! unitless drawings are read as millimetres as in Java.
mod colors;
mod convert;
mod geom;
mod reader;
#[cfg(test)]
mod tests;
mod text;

use super::Imported;
use convert::Prim;
use geom::{Affine, BBox, Seg};
use std::fmt::Write as _;
use std::path::Path;

pub fn read(path: &Path) -> Result<Imported, String> {
    let bytes = std::fs::read(path).map_err(|e| e.to_string())?;
    from_bytes(&bytes)
}

fn from_bytes(bytes: &[u8]) -> Result<Imported, String> {
    let doc = reader::parse(bytes)?;
    let mut warnings = Vec::new();
    let units = doc.header_f("$INSUNITS", 70).map_or(0, |u| u as i64);
    let unit_mm = unit_mm(units).unwrap_or_else(|| {
        warnings.push(
            "DXF-Datei ohne Einheitenangabe ($INSUNITS); Koordinaten werden als Millimeter übernommen"
                .into(),
        );
        1.0
    });
    let converted = convert::convert(&doc, unit_mm)?;
    warnings.extend(converted.warnings);
    if converted.prims.is_empty() {
        return Err("DXF-Datei enthält keine darstellbaren Objekte".into());
    }
    let layer_order: Vec<String> = doc.layers.iter().map(|l| l.name.clone()).collect();
    let (svg, [width, height]) = write_svg(&converted.prims, &layer_order)?;
    if width > 5000.0 || height > 5000.0 {
        warnings.push(format!(
            "DXF-Zeichnung ist sehr groß ({} × {} mm); Einheiten prüfen",
            num(width),
            num(height)
        ));
    }
    Ok(Imported {
        svg,
        warnings,
        ..Default::default()
    })
}

/// Millimetres per drawing unit for a `$INSUNITS` code.
fn unit_mm(code: i64) -> Option<f64> {
    Some(match code {
        1 => 25.4,
        2 => 304.8,
        3 => 1_609_344.0,
        4 => 1.0,
        5 => 10.0,
        6 => 1000.0,
        7 => 1e6,
        8 => 25.4e-6,
        9 => 0.0254,
        10 => 914.4,
        11 => 1e-7,
        12 => 1e-6,
        13 => 1e-3,
        14 => 100.0,
        15 => 1e4,
        16 => 1e5,
        17 => 1e12,
        18 => 1.495_978_707e14,
        19 => 9.460_730_472_580_8e18,
        20 => 3.085_677_581_49e19,
        21 => 1_200_000.0 / 3937.0,
        22 => 100_000.0 / 3937.0,
        23 => 3_600_000.0 / 3937.0,
        24 => 6_336_000_000.0 / 3937.0,
        _ => return None,
    })
}

fn num(value: f64) -> String {
    let mut s = format!("{value:.4}");
    if s.contains('.') {
        s = s.trim_end_matches('0').trim_end_matches('.').to_owned();
    }
    if s == "-0" { "0".into() } else { s }
}

fn precise(value: f64) -> String {
    let mut s = format!("{value:.8}");
    s = s.trim_end_matches('0').trim_end_matches('.').to_owned();
    if s == "-0" { "0".into() } else { s }
}

fn escape(text: &str) -> String {
    text.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
}

/// Text elements laid out at the origin in their own coordinates (SVG Y down).
fn text_markup(size: f64, anchor: &str, lines: &[String], first: f64, step: f64) -> String {
    let mut out = format!(
        r#"font-family="sans-serif" font-size="{}" text-anchor="{anchor}" xml:space="preserve">"#,
        num(size)
    );
    if lines.len() == 1 {
        let _ = write!(
            out,
            r#"<tspan x="0" y="{}">{}</tspan>"#,
            num(first),
            escape(&lines[0])
        );
    } else {
        for (i, line) in lines.iter().enumerate() {
            let _ = write!(
                out,
                r#"<tspan x="0" y="{}">{}</tspan>"#,
                num(first + step * i as f64),
                escape(line)
            );
        }
    }
    out
}

/// Bounding boxes of all texts in their local coordinates, measured with the
/// same fonts the preview uses; `None` where no glyphs were laid out.
fn text_extents(prims: &[Prim]) -> Vec<Option<[f64; 4]>> {
    // Lay out at a fixed size so tiny or huge drawings keep f32 precision.
    const SIZE: f64 = 100.0;
    let mut svg = String::from(r#"<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1">"#);
    let mut ids = Vec::new();
    for (i, prim) in prims.iter().enumerate() {
        if let Prim::Text {
            size,
            anchor,
            lines,
            first_baseline,
            line_height,
            ..
        } = prim
        {
            let k = SIZE / size;
            let _ = write!(
                svg,
                r#"<text id="t{i}" {}</text>"#,
                text_markup(SIZE, anchor, lines, first_baseline * k, line_height * k)
            );
            ids.push(i);
        }
    }
    svg.push_str("</svg>");
    let mut result = vec![None; prims.len()];
    if ids.is_empty() {
        return result;
    }
    let Ok(tree) = resvg::usvg::Tree::from_str(&svg, &crate::svg::options()) else {
        return result;
    };
    for i in ids {
        let Some(resvg::usvg::Node::Text(text)) = tree.node_by_id(&format!("t{i}")) else {
            continue;
        };
        let glyphs = text.flattened().abs_bounding_box();
        let rect = if glyphs.width() > 0.0 || glyphs.height() > 0.0 {
            glyphs
        } else {
            text.abs_bounding_box()
        };
        if let Prim::Text { size, .. } = &prims[i] {
            let k = size / SIZE;
            result[i] = Some([
                rect.left() as f64 * k,
                rect.top() as f64 * k,
                rect.right() as f64 * k,
                rect.bottom() as f64 * k,
            ]);
        }
    }
    result
}

/// Rough extent for texts usvg could not lay out (no fonts installed).
fn estimated_extent(prim: &Prim) -> Option<[f64; 4]> {
    let Prim::Text {
        size,
        anchor,
        lines,
        first_baseline,
        line_height,
        ..
    } = prim
    else {
        return None;
    };
    let chars = lines.iter().map(|l| l.chars().count()).max()? as f64;
    let width = chars * size * 0.55;
    let left = match *anchor {
        "middle" => -width / 2.0,
        "end" => -width,
        _ => 0.0,
    };
    let bottom = first_baseline + line_height * (lines.len() - 1) as f64 + size * 0.2;
    Some([left, first_baseline - size * 0.75, left + width, bottom])
}

fn write_svg(prims: &[Prim], layer_order: &[String]) -> Result<(String, [f64; 2]), String> {
    let extents = text_extents(prims);
    let mut bbox = BBox::default();
    for (prim, extent) in prims.iter().zip(&extents) {
        match prim {
            Prim::Path { segs, .. } => bbox.add_segments(segs),
            Prim::Text { m, .. } => {
                if let Some([x0, y0, x1, y1]) = extent.or_else(|| estimated_extent(prim)) {
                    for corner in [[x0, y0], [x1, y0], [x1, y1], [x0, y1]] {
                        bbox.add(m.apply(corner));
                    }
                }
            }
        }
    }
    if bbox.is_empty() || !bbox.min.iter().chain(&bbox.max).all(|v| v.is_finite()) {
        return Err("DXF-Datei enthält keine darstellbaren Objekte".into());
    }
    let width = (bbox.max[0] - bbox.min[0]).max(0.01);
    let height = (bbox.max[1] - bbox.min[1]).max(0.01);
    if width > 1e7 || height > 1e7 {
        return Err(format!(
            "DXF-Zeichnung ist zu groß ({} × {} mm); Einheiten prüfen",
            num(width),
            num(height)
        ));
    }
    // World (Y up) → SVG (Y down, origin at the top-left of the drawing).
    let flip = Affine {
        a: 1.0,
        b: 0.0,
        c: 0.0,
        d: -1.0,
        e: -bbox.min[0],
        f: bbox.max[1],
    };

    let mut layers: Vec<&str> = Vec::new();
    for name in layer_order {
        if prims.iter().any(|p| p.layer() == name) {
            layers.push(name);
        }
    }
    for prim in prims {
        if !layers.contains(&prim.layer()) {
            layers.push(prim.layer());
        }
    }

    let mut svg = format!(
        concat!(
            r#"<?xml version="1.0" encoding="UTF-8"?>"#,
            "\n",
            r#"<svg xmlns="http://www.w3.org/2000/svg" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape" width="{w}mm" height="{h}mm" viewBox="0 0 {w} {h}">"#,
            "\n"
        ),
        w = num(width),
        h = num(height)
    );
    for (index, layer) in layers.iter().enumerate() {
        let _ = writeln!(
            svg,
            r#"<g id="layer{}" inkscape:groupmode="layer" inkscape:label="{}">"#,
            index + 1,
            escape(layer)
        );
        for prim in prims.iter().filter(|p| p.layer() == *layer) {
            match prim {
                Prim::Path {
                    stroke,
                    fill,
                    width,
                    dash,
                    evenodd,
                    segs,
                    ..
                } => {
                    let _ = write!(svg, r#"<path d="{}""#, path_data(segs, &flip));
                    match fill {
                        Some(color) => {
                            let _ = write!(svg, r#" fill="{}""#, colors::hex(*color));
                        }
                        None => svg.push_str(r#" fill="none""#),
                    }
                    if *evenodd {
                        svg.push_str(r#" fill-rule="evenodd""#);
                    }
                    match stroke {
                        Some(color) => {
                            let _ = write!(
                                svg,
                                r#" stroke="{}" stroke-width="{}" stroke-linecap="round" stroke-linejoin="round""#,
                                colors::hex(*color),
                                num(*width)
                            );
                            if let Some(dash) = dash {
                                let values: Vec<String> = dash.iter().map(|v| num(*v)).collect();
                                let _ = write!(svg, r#" stroke-dasharray="{}""#, values.join(" "));
                            }
                        }
                        None => svg.push_str(r#" stroke="none""#),
                    }
                    svg.push_str("/>\n");
                }
                Prim::Text {
                    fill,
                    m,
                    size,
                    anchor,
                    lines,
                    first_baseline,
                    line_height,
                    ..
                } => {
                    let t = flip.then(*m);
                    let _ = writeln!(
                        svg,
                        r#"<text transform="matrix({} {} {} {} {} {})" fill="{}" {}</text>"#,
                        precise(t.a),
                        precise(t.b),
                        precise(t.c),
                        precise(t.d),
                        num(t.e),
                        num(t.f),
                        colors::hex(*fill),
                        text_markup(*size, anchor, lines, *first_baseline, *line_height)
                    );
                }
            }
        }
        svg.push_str("</g>\n");
    }
    svg.push_str("</svg>\n");
    Ok((svg, [width, height]))
}

fn path_data(segs: &[Seg], m: &Affine) -> String {
    let mut d = String::new();
    let point = |d: &mut String, p: [f64; 2]| {
        let p = m.apply(p);
        let _ = write!(d, "{} {}", num(p[0]), num(p[1]));
    };
    for seg in segs {
        if !d.is_empty() {
            d.push(' ');
        }
        match *seg {
            Seg::Move(p) => {
                d.push('M');
                point(&mut d, p);
            }
            Seg::Line(p) => {
                d.push('L');
                point(&mut d, p);
            }
            Seg::Cubic(c1, c2, p) => {
                d.push('C');
                point(&mut d, c1);
                d.push(' ');
                point(&mut d, c2);
                d.push(' ');
                point(&mut d, p);
            }
            Seg::Close => d.push('Z'),
        }
    }
    d
}
