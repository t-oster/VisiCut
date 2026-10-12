use crate::{i18n::tr, tr};
use resvg::usvg::fontdb;
use resvg::{tiny_skia, usvg};
use std::sync::{Arc, OnceLock};

pub struct Preview {
    pub width_mm: f32,
    pub height_mm: f32,
    pub image: eframe::egui::ColorImage,
}

/// System fonts plus the fonts bundled with egui (Ubuntu Light, Hack), loaded once.
///
/// Preview, engraving, cutting and the mapping all share this database so text
/// is laid out identically everywhere. The bundled fonts guarantee that the
/// generic families (`serif`, `sans-serif`, …) resolve even on machines without
/// any system fonts, e.g. minimal CI images.
pub fn fonts() -> Arc<fontdb::Database> {
    static FONTS: OnceLock<Arc<fontdb::Database>> = OnceLock::new();
    FONTS
        .get_or_init(|| {
            let mut db = fontdb::Database::new();
            db.load_system_fonts();
            let mut family = |data: &'static [u8]| {
                let ids = db.load_font_source(fontdb::Source::Binary(Arc::new(data)));
                ids.first()
                    .and_then(|id| db.face(*id))
                    .and_then(|face| face.families.first())
                    .map(|(name, _)| name.clone())
            };
            let sans = family(epaint_default_fonts::UBUNTU_LIGHT).unwrap_or_default();
            let mono = family(epaint_default_fonts::HACK_REGULAR).unwrap_or_default();
            let pick = |db: &fontdb::Database, candidates: &[&str], fallback: &str| {
                candidates
                    .iter()
                    .find(|name| {
                        db.query(&fontdb::Query {
                            families: &[fontdb::Family::Name(name)],
                            ..Default::default()
                        })
                        .is_some()
                    })
                    .map_or(fallback.to_string(), |name| name.to_string())
            };
            let sans = pick(
                &db,
                &[
                    "Arial",
                    "Helvetica",
                    "Liberation Sans",
                    "DejaVu Sans",
                    "Noto Sans",
                ],
                &sans,
            );
            let serif = pick(
                &db,
                &[
                    "Times New Roman",
                    "Times",
                    "Liberation Serif",
                    "DejaVu Serif",
                    "Noto Serif",
                ],
                &sans,
            );
            let mono = pick(
                &db,
                &[
                    "Courier New",
                    "Courier",
                    "Liberation Mono",
                    "DejaVu Sans Mono",
                ],
                &mono,
            );
            let cursive = pick(&db, &["Comic Sans MS"], &sans);
            let fantasy = pick(&db, &["Impact"], &sans);
            db.set_sans_serif_family(sans);
            db.set_serif_family(serif);
            db.set_monospace_family(mono);
            db.set_cursive_family(cursive);
            db.set_fantasy_family(fantasy);
            Arc::new(db)
        })
        .clone()
}

/// usvg options shared by preview, engraving, cutting and the mapping.
pub fn options() -> usvg::Options<'static> {
    // Embedded projects must remain portable: external resources are not resolved.
    usvg::Options {
        image_href_resolver: usvg::ImageHrefResolver {
            resolve_data: usvg::ImageHrefResolver::default_data_resolver(),
            resolve_string: Box::new(|_, _| None),
        },
        fontdb: fonts(),
        ..Default::default()
    }
}

pub fn render(source: &str) -> Result<Preview, String> {
    if source.len() > 20 * 1024 * 1024 {
        return Err(tr("SVG ist größer als 20 MB").into());
    }
    let tree = usvg::Tree::from_str(source, &options())
        .map_err(|e| tr!("SVG konnte nicht gelesen werden: {error}", error = e))?;
    let size = tree.size();
    let scale = (1600.0 / size.width().max(size.height())).min(2.0);
    let width = (size.width() * scale).ceil().max(1.0) as u32;
    let height = (size.height() * scale).ceil().max(1.0) as u32;
    let mut pixmap = tiny_skia::Pixmap::new(width, height).ok_or(tr("Vorschau zu groß"))?;
    resvg::render(
        &tree,
        tiny_skia::Transform::from_scale(scale, scale),
        &mut pixmap.as_mut(),
    );
    // tiny-skia stores premultiplied RGBA; convert before handing pixels to egui.
    let mut pixels = pixmap.take();
    for pixel in pixels.chunks_exact_mut(4) {
        let alpha = pixel[3] as u32;
        if alpha > 0 {
            for channel in &mut pixel[..3] {
                *channel = ((*channel as u32 * 255 + alpha / 2) / alpha).min(255) as u8;
            }
        }
    }
    Ok(Preview {
        width_mm: size.width() * 25.4 / 96.0,
        height_mm: size.height() * 25.4 / 96.0,
        image: eframe::egui::ColorImage::from_rgba_unmultiplied(
            [width as usize, height as usize],
            &pixels,
        ),
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn respects_physical_svg_dimensions() {
        let preview = render(include_str!("../examples/demo.svg")).unwrap();
        assert!((preview.width_mm - 100.0).abs() < 0.01);
        assert!((preview.height_mm - 60.0).abs() < 0.01);
        assert!(preview.image.pixels.iter().any(|p| p.a() > 0));
    }
    #[test]
    fn rejects_malformed_svg() {
        assert!(render("not an SVG").is_err());
    }
}
