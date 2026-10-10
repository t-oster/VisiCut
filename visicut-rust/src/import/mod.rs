//! Converts every importable file format into an SVG document.
//!
//! Projects store a single SVG, so each importer produces SVG text whose
//! physical size (width/height in mm) matches the source file, like VisiCut's
//! Java `GraphicFileImporter` does by converting to its own graphic model.
mod dxf;
mod eps;
mod gcode;
mod pdf;
mod plf;
mod raster;

pub use crate::svg_import::Imported;
use std::path::Path;

/// Largest file that is read for import.
pub const MAX_FILE_BYTES: u64 = 25 * 1024 * 1024;

/// File extensions offered in the open dialogs (without the project format).
pub const EXTENSIONS: &[&str] = &[
    "svg", "psvg", "dxf", "eps", "ps", "pdf", "png", "jpg", "jpeg", "bmp", "gif", "nc", "gcode",
    "plf", "ls",
];

/// Whether `path` is a graphic to import rather than a saved Rust project.
pub fn is_importable(path: &Path) -> bool {
    extension(path).is_some_and(|e| EXTENSIONS.contains(&e.as_str()))
}

pub fn read_file(path: &Path) -> Result<Imported, String> {
    let meta = std::fs::metadata(path).map_err(|e| e.to_string())?;
    if meta.len() > MAX_FILE_BYTES {
        return Err("Datei ist größer als 25 MB".into());
    }
    let name = path
        .file_name()
        .unwrap_or_default()
        .to_string_lossy()
        .to_ascii_lowercase();
    if name.ends_with(".parametric.svg") {
        return plf::read_parametric_svg(path);
    }
    match extension(path).as_deref() {
        Some("svg") => crate::svg_import::read_svg_file(path),
        Some("psvg") => plf::read_parametric_svg(path),
        Some("dxf") => dxf::read(path),
        Some("pdf") => pdf::read_pdf(path),
        Some("eps" | "ps") => pdf::read_postscript(path),
        Some("png" | "jpg" | "jpeg" | "bmp" | "gif") => raster::read(path),
        Some("nc" | "gcode") => gcode::read(path),
        Some("plf") => plf::read_plf(path),
        Some("ls") => plf::read_laser_script(path),
        _ => Err("Unbekanntes Dateiformat".into()),
    }
}

fn extension(path: &Path) -> Option<String> {
    path.extension()
        .and_then(|e| e.to_str())
        .map(str::to_ascii_lowercase)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn recognises_import_formats_case_insensitively() {
        assert!(is_importable(Path::new("a/B.SVG")));
        assert!(is_importable(Path::new("teil.Dxf")));
        assert!(is_importable(Path::new("x.parametric.svg")));
        assert!(!is_importable(Path::new("job.vcr")));
        assert!(!is_importable(Path::new("noextension")));
    }
}
