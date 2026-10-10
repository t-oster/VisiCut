//! Bitmap zu Vektor: Graustufen, Schwellwert, Konturverfolgung an Pixelgrenzen
//! und Douglas-Peucker-Vereinfachung, ausgegeben als SVG-Pfade in Millimetern.
//!
//! Entspricht dem Java-Dialog „Bitmap vektorisieren“, der mkbitmap und potrace
//! aufruft. Hier ist alles in Rust umgesetzt, ohne externes Programm. Dunkle
//! Pixel (Helligkeit unter dem Schwellwert) bilden das Motiv, Invertierung
//! kehrt das um. Das Motiv wird achtfach zusammenhängend verfolgt: Pixel, die
//! sich nur an einer Ecke berühren, gehören zu einer Kontur. Löcher entstehen
//! als eigene, gegenläufige Konturen und werden mit der Füllregel evenodd
//! gezeichnet.
//!
//! Das Ergebnis ist ein SVG-Dokument wie bei jedem anderen Import und läuft
//! deshalb über denselben Pfad wie SVG-Dateien (`import::Imported`).
use image::ImageReader;
use std::fmt::Write as _;
use std::io::Cursor;

/// Größte Bitmap in Pixeln, damit Speicher und Rechenzeit begrenzt bleiben.
pub const MAX_PIXELS: u64 = 25_000_000;
const DAMAGED: &str = "Bilddatei ist beschädigt oder hat ein nicht unterstütztes Format";

/// Größe des Ergebnisses. Die Höhe folgt dem Seitenverhältnis des Bildes.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum Size {
    /// Breite in mm.
    WidthMm(f64),
    /// Auflösung in Pixeln pro Zoll (wie beim Bildimport, Standard 72 DPI).
    Dpi(f64),
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Options {
    /// Helligkeit von 0 bis 255. Pixel darunter gehören zum Motiv.
    pub threshold: u8,
    /// Helle statt dunkle Pixel nachzeichnen.
    pub invert: bool,
    /// Toleranz der Vereinfachung in Pixeln.
    pub tolerance_px: f64,
    pub size: Size,
}

impl Default for Options {
    fn default() -> Self {
        Self {
            threshold: 128,
            invert: false,
            tolerance_px: 1.0,
            size: Size::Dpi(72.0),
        }
    }
}

/// Graustufenbild. Transparente Pixel zählen als weißer Hintergrund.
#[derive(Debug)]
pub struct Bitmap {
    width: usize,
    height: usize,
    luma: Vec<u8>,
}

/// Ergebnis einer Vektorisierung.
#[derive(Debug)]
pub struct Vectorized {
    /// SVG-Dokument mit einem `<path>` aller Konturen, Einheit mm.
    pub svg: String,
    /// Anzahl der Konturen (Außenkanten und Löcher zusammen).
    pub paths: usize,
    pub width_mm: f64,
    pub height_mm: f64,
}

/// Dekodiert ein PNG, JPEG, BMP oder GIF und vektorisiert es in einem Schritt.
pub fn vectorize(data: &[u8], options: &Options) -> Result<Vectorized, String> {
    Bitmap::decode(data)?.vectorize(options)
}

impl Bitmap {
    pub fn decode(data: &[u8]) -> Result<Self, String> {
        let damaged = |e: image::ImageError| format!("{DAMAGED}: {e}");
        let reader = ImageReader::new(Cursor::new(data))
            .with_guessed_format()
            .map_err(|e| format!("{DAMAGED}: {e}"))?;
        let (width, height) = reader.into_dimensions().map_err(damaged)?;
        if u64::from(width) * u64::from(height) > MAX_PIXELS {
            return Err(format!(
                "Bild hat mehr als {} Megapixel; bitte verkleinern",
                MAX_PIXELS / 1_000_000
            ));
        }
        let image = ImageReader::new(Cursor::new(data))
            .with_guessed_format()
            .map_err(|e| format!("{DAMAGED}: {e}"))?
            .decode()
            .map_err(damaged)?;
        let luma = image
            .to_rgba8()
            .pixels()
            .map(|pixel| {
                let [r, g, b, a] = pixel.0.map(u32::from);
                let value = (299 * r + 587 * g + 114 * b) / 1000;
                // Auf Weiß verrechnen, damit transparente Bereiche Hintergrund sind.
                ((value * a + 255 * (255 - a)) / 255) as u8
            })
            .collect();
        Ok(Self {
            width: width as usize,
            height: height as usize,
            luma,
        })
    }

    /// Breite und Höhe in Pixeln.
    pub fn dimensions(&self) -> (usize, usize) {
        (self.width, self.height)
    }

    /// Vektorisiert das Bild mit den Optionen.
    pub fn vectorize(&self, options: &Options) -> Result<Vectorized, String> {
        let scale = match options.size {
            Size::WidthMm(width) if width.is_finite() && width > 0.0 => width / self.width as f64,
            Size::Dpi(dpi) if dpi.is_finite() && dpi > 0.0 => 25.4 / dpi,
            _ => return Err("Ungültige Größe für die Vektorisierung".into()),
        };
        let contours = self.contours(options);
        let (width_mm, height_mm) = (self.width as f64 * scale, self.height as f64 * scale);
        let mut d = String::new();
        for contour in &contours {
            for (index, point) in contour.iter().enumerate() {
                let command = if index == 0 { 'M' } else { 'L' };
                let _ = write!(
                    d,
                    "{command}{} {} ",
                    number(point[0] * scale),
                    number(point[1] * scale)
                );
            }
            d.push_str("Z ");
        }
        let mut svg = format!(
            r#"<svg xmlns="http://www.w3.org/2000/svg" width="{w}mm" height="{h}mm" viewBox="0 0 {w} {h}">"#,
            w = number(width_mm),
            h = number(height_mm),
        );
        if !contours.is_empty() {
            let _ = write!(
                svg,
                r#"<path d="{}" fill="black" fill-rule="evenodd"/>"#,
                d.trim_end()
            );
        }
        svg.push_str("</svg>");
        Ok(Vectorized {
            svg,
            paths: contours.len(),
            width_mm,
            height_mm,
        })
    }

    /// Geschlossene, vereinfachte Konturen in Pixelkoordinaten. Die Ecken der
    /// Pixelgrenzen liegen auf ganzen Zahlen; Punkte sind ohne Wiederholung des
    /// Startpunkts angegeben.
    pub fn contours(&self, options: &Options) -> Vec<Vec<[f64; 2]>> {
        let tolerance = options.tolerance_px.max(0.0);
        let mask: Vec<bool> = self
            .luma
            .iter()
            .map(|&value| (value < options.threshold) != options.invert)
            .collect();
        trace(&mask, self.width, self.height)
            .iter()
            .map(|ring| simplify_closed(ring, tolerance))
            .filter(|ring| ring.len() >= 3)
            .collect()
    }
}

/// Richtungen ausgehend von einem Gitterpunkt: Ost, Süd, West, Nord.
const DIRECTIONS: [(i64, i64); 4] = [(1, 0), (0, 1), (-1, 0), (0, -1)];

/// Verfolgt alle Umrisskanten zwischen Motiv und Hintergrund. Jede Kante liegt
/// so, dass das Motiv in Laufrichtung immer rechts liegt (Bildschirmkoordinaten,
/// y nach unten). Dadurch sind äußere Konturen und Löcher gegenläufig.
fn trace(mask: &[bool], width: usize, height: usize) -> Vec<Vec<[f64; 2]>> {
    let (w, h) = (width as i64, height as i64);
    let filled = |x: i64, y: i64| x >= 0 && y >= 0 && x < w && y < h && mask[(y * w + x) as usize];
    let edge = |x: i64, y: i64, dir: usize| match dir {
        0 => filled(x, y) && !filled(x, y - 1),
        1 => filled(x - 1, y) && !filled(x, y),
        2 => filled(x - 1, y - 1) && !filled(x - 1, y),
        _ => filled(x, y - 1) && !filled(x - 1, y - 1),
    };
    let slot = |x: i64, y: i64, dir: usize| ((y as usize) * (width + 1) + x as usize) * 4 + dir;
    let mut used = vec![false; (width + 1) * (height + 1) * 4];
    let mut rings = Vec::new();
    for y in 0..=h {
        for x in 0..=w {
            for start in 0..4 {
                if !edge(x, y, start) || used[slot(x, y, start)] {
                    continue;
                }
                let mut ring = Vec::new();
                let (mut px, mut py, mut dir) = (x, y, start);
                loop {
                    used[slot(px, py, dir)] = true;
                    ring.push([px as f64, py as f64]);
                    px += DIRECTIONS[dir].0;
                    py += DIRECTIONS[dir].1;
                    // Bei zwei Möglichkeiten (zwei Pixel berühren sich diagonal)
                    // wird die Rechtskurve gewählt, damit die Pixel verbunden bleiben.
                    let mut next = None;
                    for candidate in 0..4 {
                        if candidate == (dir + 2) % 4
                            || !edge(px, py, candidate)
                            || used[slot(px, py, candidate)]
                        {
                            continue;
                        }
                        next = Some(match next {
                            Some(previous) if turn(dir, candidate) >= 0 => previous,
                            _ => candidate,
                        });
                    }
                    match next {
                        Some(candidate) => dir = candidate,
                        // Ohne weitere Kante ist der Startpunkt erreicht.
                        None => break,
                    }
                }
                // Der kleinste Umriss eines Pixels hat vier Ecken.
                if ring.len() >= 4 {
                    rings.push(ring);
                }
            }
        }
    }
    rings
}

/// Kreuzprodukt der Richtungen: negativ bei Rechtskurve in Bildkoordinaten.
fn turn(from: usize, to: usize) -> i64 {
    let (ax, ay) = DIRECTIONS[from];
    let (bx, by) = DIRECTIONS[to];
    ax * by - ay * bx
}

/// Douglas-Peucker für geschlossene Ringe: Der Startpunkt und sein am weitesten
/// entfernter Punkt teilen den Ring in zwei offene Züge.
fn simplify_closed(ring: &[[f64; 2]], tolerance: f64) -> Vec<[f64; 2]> {
    let n = ring.len();
    if n < 3 {
        return ring.to_vec();
    }
    let far = (1..n)
        .max_by(|&a, &b| {
            distance_squared(ring[a], ring[0]).total_cmp(&distance_squared(ring[b], ring[0]))
        })
        .unwrap_or(1);
    let first = douglas_peucker(&ring[..=far], tolerance);
    let mut second = ring[far..].to_vec();
    second.push(ring[0]);
    let second = douglas_peucker(&second, tolerance);
    // Beide Züge enden an Gitterpunkten, die im Ergebnis doppelt vorkommen würden.
    let mut result = first;
    result.extend_from_slice(&second[1..second.len() - 1]);
    result
}

/// Iterative Douglas-Peucker-Vereinfachung eines offenen Zugs.
fn douglas_peucker(points: &[[f64; 2]], tolerance: f64) -> Vec<[f64; 2]> {
    let n = points.len();
    if n < 3 {
        return points.to_vec();
    }
    let mut keep = vec![false; n];
    keep[0] = true;
    keep[n - 1] = true;
    let mut stack = vec![(0, n - 1)];
    while let Some((a, b)) = stack.pop() {
        let (mut far, mut far_distance) = (a, 0.0);
        for (index, point) in points.iter().enumerate().take(b).skip(a + 1) {
            let distance = distance_to_segment(*point, points[a], points[b]);
            if distance > far_distance {
                far = index;
                far_distance = distance;
            }
        }
        if far_distance > tolerance {
            keep[far] = true;
            stack.push((a, far));
            stack.push((far, b));
        }
    }
    points
        .iter()
        .zip(keep)
        .filter(|(_, keep)| *keep)
        .map(|(point, _)| *point)
        .collect()
}

fn distance_squared(a: [f64; 2], b: [f64; 2]) -> f64 {
    (a[0] - b[0]).powi(2) + (a[1] - b[1]).powi(2)
}

fn distance_to_segment(point: [f64; 2], a: [f64; 2], b: [f64; 2]) -> f64 {
    let (dx, dy) = (b[0] - a[0], b[1] - a[1]);
    let length_squared = dx * dx + dy * dy;
    if length_squared == 0.0 {
        return distance_squared(point, a).sqrt();
    }
    let t = (((point[0] - a[0]) * dx + (point[1] - a[1]) * dy) / length_squared).clamp(0.0, 1.0);
    distance_squared(point, [a[0] + t * dx, a[1] + t * dy]).sqrt()
}

/// Zahl für SVG mit höchstens vier Nachkommastellen.
fn number(value: f64) -> String {
    let text = format!("{value:.4}");
    let trimmed = text.trim_end_matches('0').trim_end_matches('.');
    if trimmed == "-0" {
        "0".to_string()
    } else {
        trimmed.to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Bild mit dunklen Pixeln dort, wo `dark` wahr ist, sonst weiß.
    fn bitmap(width: usize, height: usize, dark: impl Fn(usize, usize) -> bool) -> Bitmap {
        let dark = &dark;
        let luma = (0..height)
            .flat_map(|y| (0..width).map(move |x| if dark(x, y) { 0 } else { 255 }))
            .collect();
        Bitmap {
            width,
            height,
            luma,
        }
    }

    /// Vorzeichenbehaftete Fläche (Shoelace); Löcher haben das entgegengesetzte Vorzeichen.
    fn signed_area(ring: &[[f64; 2]]) -> f64 {
        let n = ring.len();
        (0..n)
            .map(|i| {
                let (a, b) = (ring[i], ring[(i + 1) % n]);
                a[0] * b[1] - b[0] * a[1]
            })
            .sum::<f64>()
            / 2.0
    }

    fn exact() -> Options {
        Options {
            tolerance_px: 0.0,
            ..Options::default()
        }
    }

    #[test]
    fn square_becomes_one_contour() {
        let image = bitmap(12, 12, |x, y| (2..10).contains(&x) && (2..10).contains(&y));
        let contours = image.contours(&Options::default());
        assert_eq!(contours.len(), 1);
        let ring = &contours[0];
        assert_eq!(ring.len(), 4, "Quadrat hat vier Ecken: {ring:?}");
        let xs: Vec<f64> = ring.iter().map(|p| p[0]).collect();
        let ys: Vec<f64> = ring.iter().map(|p| p[1]).collect();
        assert_eq!(xs.iter().cloned().fold(f64::MAX, f64::min), 2.0);
        assert_eq!(xs.iter().cloned().fold(f64::MIN, f64::max), 10.0);
        assert_eq!(ys.iter().cloned().fold(f64::MAX, f64::min), 2.0);
        assert_eq!(ys.iter().cloned().fold(f64::MIN, f64::max), 10.0);
        assert_eq!(signed_area(ring).abs(), 64.0);
    }

    #[test]
    fn hole_becomes_second_contour_with_opposite_orientation() {
        let image = bitmap(12, 12, |x, y| {
            (2..10).contains(&x)
                && (2..10).contains(&y)
                && !((4..8).contains(&x) && (4..8).contains(&y))
        });
        let contours = image.contours(&Options::default());
        assert_eq!(contours.len(), 2, "Außenkante und Loch: {contours:?}");
        let areas: Vec<f64> = contours.iter().map(|c| signed_area(c)).collect();
        assert_eq!(areas.iter().map(|a| a.abs()).fold(0.0, f64::max), 64.0);
        assert_eq!(areas.iter().map(|a| a.abs()).fold(f64::MAX, f64::min), 16.0);
        assert!(
            areas[0] * areas[1] < 0.0,
            "Loch muss gegenläufig sein: {areas:?}"
        );

        let svg = image.vectorize(&Options::default()).unwrap();
        assert_eq!(svg.paths, 2);
        assert!(svg.svg.contains(r#"fill-rule="evenodd""#));
        assert_eq!(svg.svg.matches('M').count(), 2);
    }

    #[test]
    fn inversion_traces_light_areas() {
        let image = bitmap(10, 10, |x, y| (3..7).contains(&x) && (3..7).contains(&y));
        let normal = image.contours(&exact());
        assert_eq!(normal.len(), 1);
        assert_eq!(signed_area(&normal[0]).abs(), 16.0);

        // Invertiert: der helle Rand mit dem dunklen Quadrat als Loch.
        let inverted = image.contours(&Options {
            invert: true,
            ..exact()
        });
        assert_eq!(inverted.len(), 2);
        let mut areas: Vec<f64> = inverted.iter().map(|c| signed_area(c).abs()).collect();
        areas.sort_by(f64::total_cmp);
        assert_eq!(areas, vec![16.0, 100.0]);
    }

    #[test]
    fn threshold_decides_which_grey_is_dark() {
        let image = Bitmap {
            width: 4,
            height: 4,
            luma: vec![150; 16],
        };
        assert!(image.contours(&Options::default()).is_empty());
        let darker_limit = image.contours(&Options {
            threshold: 200,
            ..Options::default()
        });
        assert_eq!(darker_limit.len(), 1);
    }

    #[test]
    fn empty_image_yields_no_paths() {
        let image = bitmap(8, 8, |_, _| false);
        let result = image.vectorize(&Options::default()).unwrap();
        assert_eq!(result.paths, 0);
        assert!(!result.svg.contains("<path"));
        assert!(result.svg.starts_with("<svg"));
    }

    #[test]
    fn diagonal_pixels_form_one_contour() {
        let image = bitmap(4, 4, |x, y| (x + y) % 2 == 0 && x < 2 && y < 2);
        let contours = image.contours(&exact());
        assert_eq!(contours.len(), 1);
        assert_eq!(
            contours.iter().map(|c| signed_area(c).abs()).sum::<f64>(),
            2.0
        );
    }

    #[test]
    fn size_sets_millimetres() {
        let image = bitmap(12, 12, |x, y| (2..10).contains(&x) && (2..10).contains(&y));
        let by_dpi = image
            .vectorize(&Options {
                size: Size::Dpi(254.0),
                ..Options::default()
            })
            .unwrap();
        assert!((by_dpi.width_mm - 1.2).abs() < 1e-9);
        assert!(
            by_dpi
                .svg
                .contains(r#"width="1.2mm" height="1.2mm" viewBox="0 0 1.2 1.2""#)
        );

        let by_width = image
            .vectorize(&Options {
                size: Size::WidthMm(60.0),
                ..Options::default()
            })
            .unwrap();
        assert!((by_width.width_mm - 60.0).abs() < 1e-9);
        assert!((by_width.height_mm - 60.0).abs() < 1e-9);
        assert!(
            by_width
                .svg
                .contains(r#"d="M10 10 L50 10 L50 50 L10 50 Z""#)
        );
    }

    #[test]
    fn invalid_size_is_rejected() {
        let image = bitmap(4, 4, |_, _| true);
        let error = image
            .vectorize(&Options {
                size: Size::Dpi(0.0),
                ..Options::default()
            })
            .unwrap_err();
        assert!(error.contains("Größe"));
    }

    #[test]
    fn decodes_png_and_treats_transparency_as_background() {
        let mut bytes = Vec::new();
        let dark = image::RgbaImage::from_fn(6, 6, |x, y| {
            if (1..5).contains(&x) && (1..5).contains(&y) {
                image::Rgba([0, 0, 0, 255])
            } else {
                image::Rgba([255, 255, 255, 255])
            }
        });
        image::DynamicImage::ImageRgba8(dark)
            .write_to(&mut Cursor::new(&mut bytes), image::ImageFormat::Png)
            .unwrap();
        let result = vectorize(&bytes, &Options::default()).unwrap();
        assert_eq!(result.paths, 1);

        let transparent = image::RgbaImage::from_pixel(4, 4, image::Rgba([0, 0, 0, 0]));
        let mut bytes = Vec::new();
        image::DynamicImage::ImageRgba8(transparent)
            .write_to(&mut Cursor::new(&mut bytes), image::ImageFormat::Png)
            .unwrap();
        assert_eq!(vectorize(&bytes, &Options::default()).unwrap().paths, 0);
    }

    #[test]
    fn damaged_data_is_reported() {
        let error = vectorize(b"kein Bild", &Options::default()).unwrap_err();
        assert!(error.contains("beschädigt"));
    }
}
