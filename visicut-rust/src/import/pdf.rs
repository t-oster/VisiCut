//! PDF, EPS and PostScript import.
//!
//! PDF pages are interpreted by the pure-Rust `hayro` stack and written as SVG
//! by `hayro-svg`: paths stay vector paths with their fill and stroke colours
//! and widths, text becomes glyph outlines and raster images are embedded as
//! PNG `data:` URIs. Java VisiCut has no PDF importer; this follows its EPS
//! importer instead and maps 1 pt to 25.4 / 72 mm.
//!
//! EPS and PS files are converted to PDF by Ghostscript (if installed) and then
//! imported like a PDF, which replaces Java VisiCut's built-in PostScript
//! interpreter. `-dEPSCrop` makes the page match the EPS BoundingBox.
mod ghostscript;

use super::Imported;
use hayro_svg::hayro_interpret::{InterpreterSettings, InterpreterWarning};
use hayro_svg::hayro_syntax::{LoadPdfError, Pdf, PdfData};
use std::path::Path;
use std::sync::{Arc, Mutex};

/// Millimetres per PostScript point.
const MM_PER_PT: f32 = 25.4 / 72.0;
/// Width given to PDF zero-width strokes ("thinnest line the device can
/// render"), which SVG would not draw at all.
const HAIRLINE_MM: f32 = 0.1;

pub fn read_pdf(path: &Path) -> Result<Imported, String> {
    let data = std::fs::read(path).map_err(|e| format!("PDF konnte nicht gelesen werden: {e}"))?;
    convert(data)
}

pub fn read_postscript(path: &Path) -> Result<Imported, String> {
    let pdf = ghostscript::to_pdf(path)?;
    convert(pdf)
}

/// Converts the first page of a PDF document into an SVG sized in millimetres.
fn convert(data: Vec<u8>) -> Result<Imported, String> {
    if !data.starts_with(b"%PDF") && !data.windows(5).take(1024).any(|w| w == b"%PDF-") {
        return Err("Datei ist keine gültige PDF-Datei".into());
    }
    // hayro does not use unsafe code but may still panic on malformed input;
    // a broken file must not take the whole application down.
    std::panic::catch_unwind(move || convert_unchecked(data))
        .unwrap_or_else(|_| Err("PDF ist beschädigt und konnte nicht gelesen werden".into()))
}

fn convert_unchecked(data: Vec<u8>) -> Result<Imported, String> {
    let pdf = Pdf::new(PdfData::from(data)).map_err(|e| match e {
        LoadPdfError::Decryption(_) => "PDF ist verschlüsselt oder passwortgeschützt; \
            bitte eine ungeschützte Kopie speichern"
            .to_string(),
        LoadPdfError::Invalid => "PDF ist beschädigt und konnte nicht gelesen werden".to_string(),
    })?;
    let pages = pdf.pages();
    let page = pages.first().ok_or("PDF enthält keine Seiten")?;

    let problems = Arc::new(Mutex::new(Vec::new()));
    let sink = problems.clone();
    let settings = InterpreterSettings {
        warning_sink: Arc::new(move |warning| {
            if let Ok(mut list) = sink.lock() {
                list.push(warning);
            }
        }),
        ..Default::default()
    };
    let cache = hayro_svg::RenderCache::new();
    let svg = hayro_svg::convert(page, &cache, &settings, &Default::default());

    let (width_pt, height_pt) = page.render_dimensions();
    let svg = postprocess(&svg, width_pt, height_pt)?;
    if svg.len() > crate::svg_import::MAX_SVG_BYTES {
        return Err("PDF-Seite ist zu komplex (SVG größer als 20 MB)".into());
    }

    let mut warnings = Vec::new();
    if pages.len() > 1 {
        warnings.push(format!(
            "PDF enthält {} Seiten; nur Seite 1 wurde importiert",
            pages.len()
        ));
    }
    let problems = problems.lock().map(|list| list.clone()).unwrap_or_default();
    if problems
        .iter()
        .any(|p| matches!(p, InterpreterWarning::UnsupportedFont))
    {
        warnings.push("PDF enthält eine nicht unterstützte Schrift; Text fehlt eventuell".into());
    }
    if problems
        .iter()
        .any(|p| matches!(p, InterpreterWarning::ImageDecodeFailure))
    {
        warnings.push("Ein Bild im PDF konnte nicht dekodiert werden und fehlt".into());
    }
    // Clip paths are cut as the intersection of the outlines (see
    // `geometry::contours`); masks cannot be cut.
    if svg.contains("mask=\"url(") {
        warnings.push(
            "PDF enthält Masken; Masken lassen sich nicht schneiden, zum Schneiden \
             vorher in echte Pfade umwandeln"
                .into(),
        );
    }
    Ok(Imported { svg, warnings })
}

/// Gives the SVG its physical size and makes zero-width strokes visible.
///
/// hayro-svg writes the page size in points as unitless width/height, which
/// SVG reads as CSS pixels; the root start tag is therefore rewritten with
/// millimetres while the viewBox stays in points.
fn postprocess(svg: &str, width_pt: f32, height_pt: f32) -> Result<String, String> {
    let document = roxmltree::Document::parse(svg)
        .map_err(|e| format!("PDF konnte nicht umgewandelt werden: {e}"))?;
    let root = document.root_element();
    let body_start = svg[root.range()]
        .find('>')
        .map(|i| root.range().start + i + 1)
        .ok_or("PDF konnte nicht umgewandelt werden")?;
    let mut out = String::with_capacity(svg.len() + 128);
    out.push_str(&format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" \
         width=\"{}mm\" height=\"{}mm\" viewBox=\"0 0 {width_pt} {height_pt}\">",
        width_pt * MM_PER_PT,
        height_pt * MM_PER_PT,
    ));

    let mut copied = body_start;
    for node in document.descendants().filter(|n| n.is_element()) {
        let zero = node
            .attribute("stroke-width")
            .and_then(|w| w.parse::<f32>().ok())
            .is_some_and(|w| w <= 0.0);
        if !zero {
            continue;
        }
        let range = node.range();
        let Some(offset) = svg[range.clone()].find("stroke-width=\"") else {
            continue;
        };
        let value_start = range.start + offset + "stroke-width=\"".len();
        let Some(value_len) = svg[value_start..].find('"') else {
            continue;
        };
        let scale = node.attribute("transform").map_or(1.0, transform_scale);
        let width = HAIRLINE_MM / MM_PER_PT / scale.max(f32::EPSILON);
        out.push_str(&svg[copied..value_start]);
        out.push_str(&width.to_string());
        copied = value_start + value_len;
    }
    out.push_str(&svg[copied..]);
    Ok(out)
}

/// Mean scale factor of a transform written by hayro-svg (`scale`,
/// `translate` or `matrix`).
fn transform_scale(transform: &str) -> f32 {
    let numbers: Vec<f32> = transform
        .split(|c: char| !(c.is_ascii_digit() || matches!(c, '.' | '-' | 'e' | 'E')))
        .filter_map(|n| n.parse().ok())
        .collect();
    let scale = if transform.starts_with("matrix") && numbers.len() == 6 {
        (numbers[0] * numbers[3] - numbers[1] * numbers[2])
            .abs()
            .sqrt()
    } else if transform.starts_with("scale") && !numbers.is_empty() {
        let sy = numbers.get(1).copied().unwrap_or(numbers[0]);
        (numbers[0] * sy).abs().sqrt()
    } else {
        1.0
    };
    if scale.is_finite() && scale > 0.0 {
        scale
    } else {
        1.0
    }
}

#[cfg(test)]
mod tests;
