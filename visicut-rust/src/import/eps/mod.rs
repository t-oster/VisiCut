//! Built-in PostScript interpreter for EPS and PS files, the Rust counterpart
//! of Java VisiCut's `EPSImporter`.
//!
//! It runs the subset of PostScript that draws paths (see `machine.rs`) and
//! writes the result as SVG, sized by the BoundingBox in points (1 pt =
//! 25.4 / 72 mm). Coordinates are mapped so that the BoundingBox's lower left
//! corner becomes the SVG origin and the y axis points down, as in the
//! Ghostscript conversion. Anything outside the subset is an error that names
//! the operator; `pdf::read_postscript` then falls back to Ghostscript.
mod graphics;
mod machine;
mod parse;
#[cfg(test)]
mod tests;

use crate::svg_import::{Imported, MAX_SVG_BYTES};
use graphics::num;

/// Millimetres per PostScript point.
const MM_PER_PT: f64 = 25.4 / 72.0;
/// Size used when the file has no BoundingBox, as in Java VisiCut.
const DEFAULT_BOX: [f64; 4] = [0.0, 0.0, 800.0, 600.0];

/// Interprets an EPS or PS file and returns its paths as SVG.
pub(super) fn interpret(data: &[u8]) -> Result<Imported, String> {
    let mut warnings = Vec::new();
    let [llx, lly, urx, ury] = bounding_box(data).unwrap_or_else(|| {
        warnings
            .push("EPS-Datei ohne BoundingBox; es wird die Größe 800 × 600 pt angenommen".into());
        DEFAULT_BOX
    });
    let width = urx - llx;
    let height = ury - lly;
    if !(width > 0.0 && height > 0.0) {
        return Err("Die BoundingBox der EPS-Datei ist leer oder ungültig".into());
    }
    let program = parse::parse(data)?;
    // Maps user space to SVG device space: origin at the lower left corner of
    // the BoundingBox, y pointing down.
    let mut machine = machine::Machine::new([1.0, 0.0, 0.0, -1.0, -llx, ury]);
    machine.run(&program)?;
    let svg = format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"{}mm\" height=\"{}mm\" \
         viewBox=\"0 0 {} {}\">\n{}</svg>\n",
        num(width * MM_PER_PT),
        num(height * MM_PER_PT),
        num(width),
        num(height),
        machine.into_elements(),
    );
    if svg.len() > MAX_SVG_BYTES {
        return Err("EPS-Datei ist zu komplex (SVG größer als 20 MB)".into());
    }
    Ok(Imported { svg, warnings })
}

/// The first `%%BoundingBox:` or `%%PageBoundingBox:` comment with four
/// numbers, as Java VisiCut reads it. `(atend)` boxes are skipped.
fn bounding_box(data: &[u8]) -> Option<[f64; 4]> {
    data.split(|&byte| byte == b'\n' || byte == b'\r')
        .find_map(|line| {
            let rest = line
                .strip_prefix(b"%%BoundingBox:")
                .or_else(|| line.strip_prefix(b"%%PageBoundingBox:"))?;
            let numbers: Vec<f64> = std::str::from_utf8(rest)
                .ok()?
                .split_whitespace()
                .map(str::parse)
                .collect::<Result<_, _>>()
                .ok()?;
            match numbers.as_slice() {
                &[llx, lly, urx, ury] if [llx, lly, urx, ury].iter().all(|v| v.is_finite()) => {
                    Some([llx, lly, urx, ury])
                }
                _ => None,
            }
        })
}
