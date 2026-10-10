//! Raster images (PNG, JPEG, BMP, GIF) as an SVG holding one embedded image.
//!
//! Java's `JPGPNGImporter` always scales pixels with 72 DPI ("TODO: Get Real
//! Resolution"). The resolution stored in the file (PNG `pHYs`, JPEG JFIF or
//! Exif, BMP pixels per metre) is used instead when present; 72 DPI remains
//! the fallback.
use super::Imported;
use crate::svg_import::MAX_SVG_BYTES;
use base64::Engine;
use image::{DynamicImage, ImageDecoder, ImageFormat, ImageReader, metadata::Orientation};
use std::io::Cursor;
use std::path::Path;

/// Java's fixed scale `Util.inch2mm(1d/72d)` per pixel.
const DEFAULT_DPI: f64 = 72.0;
/// Resolutions outside this range are treated as missing.
const DPI_RANGE: std::ops::RangeInclusive<f64> = 1.0..=100_000.0;
const DAMAGED: &str = "Bilddatei ist beschädigt oder hat ein nicht unterstütztes Format";

pub fn read(path: &Path) -> Result<Imported, String> {
    let data = std::fs::read(path).map_err(|e| e.to_string())?;
    from_bytes(data)
}

fn from_bytes(data: Vec<u8>) -> Result<Imported, String> {
    let format = image::guess_format(&data).map_err(|_| DAMAGED.to_string())?;
    let (stored_dpi, cmyk) = match format {
        ImageFormat::Png => (png_dpi(&data), false),
        ImageFormat::Jpeg => jpeg_info(&data),
        ImageFormat::Bmp => (bmp_dpi(&data), false),
        ImageFormat::Gif => (None, false),
        _ => return Err(DAMAGED.into()),
    };
    let mut decoder = ImageReader::with_format(Cursor::new(&data), format)
        .into_decoder()
        .map_err(|e| format!("{DAMAGED}: {e}"))?;
    let orientation = decoder.orientation().unwrap_or(Orientation::NoTransforms);
    let exif = decoder.exif_metadata().ok().flatten();
    let mut image = DynamicImage::from_decoder(decoder).map_err(|e| format!("{DAMAGED}: {e}"))?;
    let stored_dpi = stored_dpi.or_else(|| exif.as_deref().and_then(exif_dpi));

    let mut warnings = Vec::new();
    let (mut dpi_x, mut dpi_y) = stored_dpi.unwrap_or_else(|| {
        warnings.push(format!(
            "Bild enthält keine Auflösung; {DEFAULT_DPI:.0} DPI angenommen (wie VisiCut)"
        ));
        (DEFAULT_DPI, DEFAULT_DPI)
    });

    // resvg ignores Exif orientation, cannot draw CMYK JPEGs and only decodes
    // PNG, JPEG, GIF and WebP, so everything else is re-encoded.
    let (mime, bytes) = match format {
        ImageFormat::Png | ImageFormat::Gif | ImageFormat::Jpeg
            if orientation == Orientation::NoTransforms && !cmyk =>
        {
            let mime = match format {
                ImageFormat::Png => "image/png",
                ImageFormat::Gif => "image/gif",
                _ => "image/jpeg",
            };
            (mime, data)
        }
        _ => {
            if matches!(
                orientation,
                Orientation::Rotate90
                    | Orientation::Rotate270
                    | Orientation::Rotate90FlipH
                    | Orientation::Rotate270FlipH
            ) {
                std::mem::swap(&mut dpi_x, &mut dpi_y);
            }
            image.apply_orientation(orientation);
            encode(&image, format == ImageFormat::Jpeg)?
        }
    };

    let (width, height) = (image.width(), image.height());
    let encoded_len = bytes.len().div_ceil(3) * 4;
    if encoded_len + 512 > MAX_SVG_BYTES {
        return Err(format!(
            "Bild ist eingebettet größer als {} MB; bitte verkleinern oder stärker komprimieren",
            MAX_SVG_BYTES / (1024 * 1024)
        ));
    }
    let mut svg = String::with_capacity(encoded_len + 512);
    svg.push_str(&format!(
        r#"<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{}mm" height="{}mm" viewBox="0 0 {width} {height}" preserveAspectRatio="none"><image width="{width}" height="{height}" preserveAspectRatio="none" xlink:href="data:{mime};base64,"#,
        number(width as f64 * 25.4 / dpi_x),
        number(height as f64 * 25.4 / dpi_y),
    ));
    base64::engine::general_purpose::STANDARD.encode_string(&bytes, &mut svg);
    svg.push_str("\"/></svg>");
    Ok(Imported {
        svg,
        warnings,
        ..Default::default()
    })
}

/// Re-encodes photos as JPEG (quality 95) and everything else losslessly as PNG.
fn encode(image: &DynamicImage, photo: bool) -> Result<(&'static str, Vec<u8>), String> {
    let mut bytes = Vec::new();
    if photo {
        let encoder = image::codecs::jpeg::JpegEncoder::new_with_quality(&mut bytes, 95);
        DynamicImage::ImageRgb8(image.to_rgb8())
            .write_with_encoder(encoder)
            .map_err(|e| e.to_string())?;
        Ok(("image/jpeg", bytes))
    } else {
        let image = if image.color().has_alpha() {
            DynamicImage::ImageRgba8(image.to_rgba8())
        } else {
            DynamicImage::ImageRgb8(image.to_rgb8())
        };
        image
            .write_to(Cursor::new(&mut bytes), ImageFormat::Png)
            .map_err(|e| e.to_string())?;
        Ok(("image/png", bytes))
    }
}

fn number(value: f64) -> String {
    let text = format!("{value:.4}");
    text.trim_end_matches('0').trim_end_matches('.').to_string()
}

fn valid(dpi_x: f64, dpi_y: f64) -> Option<(f64, f64)> {
    (DPI_RANGE.contains(&dpi_x) && DPI_RANGE.contains(&dpi_y)).then_some((dpi_x, dpi_y))
}

fn be16(data: &[u8], at: usize) -> Option<u16> {
    Some(u16::from_be_bytes(data.get(at..at + 2)?.try_into().ok()?))
}

fn be32(data: &[u8], at: usize) -> Option<u32> {
    Some(u32::from_be_bytes(data.get(at..at + 4)?.try_into().ok()?))
}

/// PNG `pHYs` chunk in pixels per metre (unit 1); unit 0 is only an aspect ratio.
fn png_dpi(data: &[u8]) -> Option<(f64, f64)> {
    let mut at = 8;
    while let (Some(length), Some(kind)) = (be32(data, at), data.get(at + 4..at + 8)) {
        let body = at + 8;
        match kind {
            b"pHYs" if data.get(body + 8) == Some(&1) => {
                let x = be32(data, body)? as f64 * 0.0254;
                let y = be32(data, body + 4)? as f64 * 0.0254;
                return valid(x, y);
            }
            b"IDAT" | b"IEND" => return None,
            _ => at = body.checked_add(length as usize)?.checked_add(4)?,
        }
    }
    None
}

/// JFIF density and whether the JPEG has four colour components (CMYK/YCCK).
fn jpeg_info(data: &[u8]) -> (Option<(f64, f64)>, bool) {
    let mut dpi = None;
    let mut cmyk = false;
    let mut at = 2;
    while data.get(at) == Some(&0xFF) {
        let Some(&marker) = data.get(at + 1) else {
            break;
        };
        match marker {
            0xFF => {
                at += 1;
                continue;
            }
            0x01 | 0xD0..=0xD8 => {
                at += 2;
                continue;
            }
            0xD9 | 0xDA => break,
            _ => {}
        }
        let Some(length) = be16(data, at + 2) else {
            break;
        };
        let segment = data.get(at + 4..at + 2 + length as usize).unwrap_or(&[]);
        if marker == 0xE0 && segment.len() >= 12 && segment.starts_with(b"JFIF\0") {
            let (x, y) = (be16(segment, 8), be16(segment, 10));
            if let (Some(x), Some(y)) = (x, y) {
                dpi = match segment[7] {
                    1 => valid(x as f64, y as f64),
                    2 => valid(x as f64 * 2.54, y as f64 * 2.54),
                    _ => None,
                };
            }
        }
        if matches!(marker, 0xC0..=0xCF) && !matches!(marker, 0xC4 | 0xC8 | 0xCC) {
            cmyk = segment.get(5) == Some(&4);
        }
        at += 2 + length as usize;
    }
    (dpi, cmyk)
}

/// Exif `XResolution`/`YResolution` with `ResolutionUnit` (inch by default).
fn exif_dpi(chunk: &[u8]) -> Option<(f64, f64)> {
    let tiff = chunk.strip_prefix(b"Exif\0\0").unwrap_or(chunk);
    let little = match tiff.get(..4)? {
        b"II*\0" => true,
        b"MM\0*" => false,
        _ => return None,
    };
    let u16_at = |at: usize| -> Option<u16> {
        let bytes: [u8; 2] = tiff.get(at..at + 2)?.try_into().ok()?;
        Some(if little {
            u16::from_le_bytes(bytes)
        } else {
            u16::from_be_bytes(bytes)
        })
    };
    let u32_at = |at: usize| -> Option<u32> {
        let bytes: [u8; 4] = tiff.get(at..at + 4)?.try_into().ok()?;
        Some(if little {
            u32::from_le_bytes(bytes)
        } else {
            u32::from_be_bytes(bytes)
        })
    };
    let ifd = u32_at(4)? as usize;
    let (mut x, mut y, mut unit) = (None, None, 2);
    for index in 0..u16_at(ifd)? as usize {
        let entry = ifd + 2 + index * 12;
        let rational = || -> Option<f64> {
            let offset = u32_at(entry + 8)? as usize;
            let denominator = u32_at(offset + 4)?;
            (denominator != 0).then(|| u32_at(offset).map(|n| n as f64 / denominator as f64))?
        };
        match u16_at(entry)? {
            0x011A => x = rational(),
            0x011B => y = rational(),
            0x0128 => unit = u16_at(entry + 8)?,
            _ => {}
        }
    }
    let scale = match unit {
        2 => 1.0,
        3 => 2.54,
        _ => return None,
    };
    let x = x?;
    valid(x * scale, y.unwrap_or(x) * scale)
}

/// BMP `biXPelsPerMeter`/`biYPelsPerMeter` of BITMAPINFOHEADER and later.
fn bmp_dpi(data: &[u8]) -> Option<(f64, f64)> {
    let header = u32::from_le_bytes(data.get(14..18)?.try_into().ok()?);
    if header < 40 {
        return None;
    }
    let x = i32::from_le_bytes(data.get(38..42)?.try_into().ok()?);
    let y = i32::from_le_bytes(data.get(42..46)?.try_into().ok()?);
    valid(x as f64 * 0.0254, y as f64 * 0.0254)
}

#[cfg(test)]
mod tests {
    use super::*;
    use image::{Rgb, RgbImage, Rgba, RgbaImage};

    fn size_mm(svg: &str) -> (f32, f32) {
        let preview = crate::svg::render(svg).unwrap();
        (preview.width_mm, preview.height_mm)
    }

    fn encoded(image: DynamicImage, format: ImageFormat) -> Vec<u8> {
        let mut bytes = Vec::new();
        image.write_to(Cursor::new(&mut bytes), format).unwrap();
        bytes
    }

    fn checker(width: u32, height: u32) -> DynamicImage {
        DynamicImage::ImageRgb8(RgbImage::from_fn(width, height, |x, y| {
            if (x + y) % 2 == 0 {
                Rgb([0, 0, 0])
            } else {
                Rgb([255, 0, 0])
            }
        }))
    }

    fn crc32(bytes: &[u8]) -> u32 {
        let mut crc = !0u32;
        for &byte in bytes {
            crc ^= byte as u32;
            for _ in 0..8 {
                crc = if crc & 1 == 1 {
                    (crc >> 1) ^ 0xEDB8_8320
                } else {
                    crc >> 1
                };
            }
        }
        !crc
    }

    /// Inserts an ancillary chunk directly after IHDR.
    fn with_chunk(png: &[u8], kind: &[u8; 4], body: &[u8]) -> Vec<u8> {
        let mut chunk = (body.len() as u32).to_be_bytes().to_vec();
        chunk.extend_from_slice(kind);
        chunk.extend_from_slice(body);
        chunk.extend_from_slice(&crc32(&chunk[4..]).to_be_bytes());
        let ihdr_end = 8 + 8 + 13 + 4;
        [&png[..ihdr_end], &chunk, &png[ihdr_end..]].concat()
    }

    fn assert_close(actual: (f32, f32), expected: (f32, f32)) {
        assert!(
            (actual.0 - expected.0).abs() < 0.01 && (actual.1 - expected.1).abs() < 0.01,
            "{actual:?} != {expected:?}"
        );
    }

    #[test]
    fn png_resolution_sets_physical_size() {
        let png = encoded(checker(300, 150), ImageFormat::Png);
        let ppm = (300.0f64 / 0.0254).round() as u32;
        let mut phys = ppm.to_be_bytes().to_vec();
        phys.extend_from_slice(&ppm.to_be_bytes());
        phys.push(1);
        let imported = from_bytes(with_chunk(&png, b"pHYs", &phys)).unwrap();
        assert!(imported.warnings.is_empty());
        assert!(imported.svg.contains("data:image/png;base64,"));
        assert!(imported.svg.contains(r#"viewBox="0 0 300 150""#));
        assert_close(size_mm(&imported.svg), (25.4, 12.7));
    }

    #[test]
    fn jpeg_without_density_falls_back_to_72_dpi() {
        let jpeg = encoded(checker(72, 144), ImageFormat::Jpeg);
        assert_eq!(jpeg_info(&jpeg), (None, false));
        let imported = from_bytes(jpeg).unwrap();
        assert_eq!(imported.warnings.len(), 1);
        assert!(imported.svg.contains("data:image/jpeg;base64,"));
        assert_close(size_mm(&imported.svg), (25.4, 50.8));
    }

    #[test]
    fn jpeg_jfif_and_exif_density_are_read() {
        let mut jfif = vec![0xFF, 0xD8, 0xFF, 0xE0, 0, 16];
        jfif.extend_from_slice(b"JFIF\0\x01\x02\x02\x00\x76\x00\x76\x00\x00");
        // 118 dots per cm ≈ 299.72 DPI
        assert_eq!(jpeg_info(&jfif).0, Some((118.0 * 2.54, 118.0 * 2.54)));

        let mut exif = b"Exif\0\0MM\0*\0\0\0\x08\0\x02".to_vec();
        exif.extend_from_slice(&[0x01, 0x1A, 0, 5, 0, 0, 0, 1, 0, 0, 0, 38]);
        exif.extend_from_slice(&[0x01, 0x28, 0, 3, 0, 0, 0, 1, 0, 2, 0, 0]);
        exif.extend_from_slice(&[0, 0, 0, 0]);
        exif.extend_from_slice(&[0, 0, 0x02, 0x58, 0, 0, 0, 2]);
        assert_eq!(exif_dpi(&exif), Some((300.0, 300.0)));
    }

    #[test]
    fn bmp_is_reencoded_as_png_and_renders() {
        let bmp = encoded(checker(40, 20), ImageFormat::Bmp);
        let imported = from_bytes(bmp).unwrap();
        assert!(imported.svg.contains("data:image/png;base64,"));
        let preview = crate::svg::render(&imported.svg).unwrap();
        assert!(
            preview
                .image
                .pixels
                .iter()
                .any(|p| p.a() == 255 && p.r() > 200 && p.g() < 50)
        );
    }

    #[test]
    fn bmp_pixels_per_metre_set_size() {
        let mut bmp = encoded(checker(100, 50), ImageFormat::Bmp);
        let ppm = 3937i32; // 100 DPI
        bmp[38..42].copy_from_slice(&ppm.to_le_bytes());
        bmp[42..46].copy_from_slice(&ppm.to_le_bytes());
        let imported = from_bytes(bmp).unwrap();
        assert!(imported.warnings.is_empty());
        assert_close(size_mm(&imported.svg), (25.4, 12.7));
    }

    #[test]
    fn gif_is_embedded_and_renders() {
        let gif = encoded(
            DynamicImage::ImageRgba8(RgbaImage::from_pixel(8, 8, Rgba([0, 0, 255, 255]))),
            ImageFormat::Gif,
        );
        let imported = from_bytes(gif).unwrap();
        assert!(imported.svg.contains("data:image/gif;base64,"));
        let preview = crate::svg::render(&imported.svg).unwrap();
        assert!(
            preview
                .image
                .pixels
                .iter()
                .any(|p| p.a() == 255 && p.b() > 200)
        );
        assert_close(
            (preview.width_mm, preview.height_mm),
            (8.0 * 25.4 / 72.0, 8.0 * 25.4 / 72.0),
        );
    }

    #[test]
    fn exif_rotation_is_applied() {
        let jpeg = encoded(checker(40, 20), ImageFormat::Jpeg);
        let mut exif = b"Exif\0\0II*\0\x08\0\0\0\x01\0".to_vec();
        exif.extend_from_slice(&[0x12, 0x01, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0]);
        exif.extend_from_slice(&[0, 0, 0, 0]);
        let mut app1 = vec![0xFF, 0xE1];
        app1.extend_from_slice(&((exif.len() + 2) as u16).to_be_bytes());
        app1.extend_from_slice(&exif);
        let rotated = [&jpeg[..2], &app1, &jpeg[2..]].concat();
        let imported = from_bytes(rotated).unwrap();
        assert!(imported.svg.contains(r#"viewBox="0 0 20 40""#));
    }

    #[test]
    fn oversized_image_is_rejected() {
        let png = encoded(checker(2, 2), ImageFormat::Png);
        let padding = vec![0u8; 16 * 1024 * 1024];
        let Err(error) = from_bytes(with_chunk(&png, b"zzZz", &padding)) else {
            panic!("oversized image was accepted");
        };
        assert!(error.contains("20 MB"), "{error}");
    }

    #[test]
    fn damaged_files_are_reported() {
        assert!(from_bytes(b"not an image".to_vec()).is_err());
        let png = encoded(checker(4, 4), ImageFormat::Png);
        assert!(from_bytes(png[..40].to_vec()).is_err());
    }

    #[test]
    fn reads_from_disk() {
        let path = std::env::temp_dir().join(format!("visicut-raster-{}.png", std::process::id()));
        std::fs::write(&path, encoded(checker(4, 4), ImageFormat::Png)).unwrap();
        let result = read(&path);
        let _ = std::fs::remove_file(&path);
        assert!(result.unwrap().svg.starts_with("<svg"));
    }
}
