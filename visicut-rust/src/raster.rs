//! Engraving rasters: greyscale conversion and dithering, ported from
//! LibLaserCut (utils/BufferedImageAdapter.java, dithering/*.java),
//! LGPL-3.0-or-later. Original authors: Thomas Oster, Max Gaukler.
use crate::i18n::tr;
use resvg::tiny_skia;
use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum Dithering {
    /// Fixed luminance threshold 128 (VisiCutRust ≤ 0.3).
    Threshold,
    FloydSteinberg,
    Average,
    Random,
    Ordered,
    Grid,
    Halftone,
    BrightenedHalftone,
}

impl Dithering {
    pub const ALL: [Dithering; 8] = [
        Self::BrightenedHalftone,
        Self::FloydSteinberg,
        Self::Halftone,
        Self::Ordered,
        Self::Average,
        Self::Grid,
        Self::Random,
        Self::Threshold,
    ];
    pub fn title(self) -> &'static str {
        match self {
            Self::Threshold => tr("Schwellwert 50 %"),
            Self::FloydSteinberg => "Floyd-Steinberg",
            Self::Average => tr("Mittelwert"),
            Self::Random => tr("Zufall"),
            Self::Ordered => tr("Geordnet"),
            Self::Grid => tr("Raster"),
            Self::Halftone => tr("Halbton"),
            Self::BrightenedHalftone => tr("Halbton aufgehellt"),
        }
    }
}

/// Raster options of an engraving step, as in VisiCut's raster profiles and
/// LaosEngraveProperty.
#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
#[serde(default, deny_unknown_fields)]
pub struct RasterSettings {
    pub dithering: Dithering,
    pub invert: bool,
    /// Added to the grey value (−255 … 255); positive values engrave lighter.
    pub color_shift: i32,
    pub bidirectional: bool,
    pub bottom_up: bool,
}

/// New steps follow the FAU "engrave" profile.
impl Default for RasterSettings {
    fn default() -> Self {
        Self {
            dithering: Dithering::BrightenedHalftone,
            invert: false,
            color_shift: 0,
            bidirectional: true,
            bottom_up: false,
        }
    }
}

impl RasterSettings {
    /// Behaviour of projects saved before raster options existed.
    pub fn legacy() -> Self {
        Self {
            dithering: Dithering::Threshold,
            bidirectional: false,
            ..Self::default()
        }
    }

    pub fn validate(&self) -> Result<(), String> {
        if !(-255..=255).contains(&self.color_shift) {
            return Err(tr("Helligkeitsverschiebung muss zwischen −255 und 255 liegen").into());
        }
        Ok(())
    }
}

/// Grey values 0 (black) … 255 (white) as in BufferedImageAdapter.getGreyScale.
pub fn greyscale(pixmap: &tiny_skia::Pixmap, settings: &RasterSettings) -> Vec<u8> {
    pixmap
        .data()
        .chunks_exact(4)
        .map(|p| {
            let value = if settings.dithering == Dithering::Threshold {
                // Legacy luminance, so old projects keep their exact output.
                ((p[0] as u32 * 2126 + p[1] as u32 * 7152 + p[2] as u32 * 722) / 10000) as i32
            } else {
                (0.3 * p[0] as f64 + 0.59 * p[1] as f64 + 0.11 * p[2] as f64) as i32
            };
            let value = (value + settings.color_shift).clamp(0, 255) as u8;
            if settings.invert { 255 - value } else { value }
        })
        .collect()
}

/// Black (engraved) pixels, row-major.
pub fn dither(grey: &[u8], width: usize, height: usize, algorithm: Dithering) -> Vec<bool> {
    let at = |x: usize, y: usize| grey[y * width + x] as i32;
    match algorithm {
        Dithering::Threshold => grey.iter().map(|g| *g < 128).collect(),
        Dithering::Average | Dithering::Grid => {
            let total: u64 = grey.iter().map(|g| *g as u64).sum();
            let threshold = (total / height.max(1) as u64 / width.max(1) as u64) as i32;
            let (size, gap) = (10, 5);
            (0..width * height)
                .map(|i| {
                    let (x, y) = (i % width, i / width);
                    let inside = algorithm == Dithering::Average
                        || (y % (size + gap) <= size && x % (size + gap) <= size);
                    inside && at(x, y) < threshold
                })
                .collect()
        }
        Dithering::Random => {
            // Seeded so that preview and job are identical; LibLaserCut uses
            // an unseeded generator.
            let mut state = 0x2545_f491_4f6c_dd1du64;
            grey.iter()
                .map(|g| {
                    state ^= state << 13;
                    state ^= state >> 7;
                    state ^= state << 17;
                    (*g as u64) < state % 256
                })
                .collect()
        }
        Dithering::Ordered | Dithering::Halftone | Dithering::BrightenedHalftone => {
            let matrix = threshold_matrix(algorithm);
            let n = matrix.len();
            (0..width * height)
                .map(|i| {
                    let (x, y) = (i % width, i / width);
                    at(x, y) < matrix[x % n][y % n]
                })
                .collect()
        }
        Dithering::FloydSteinberg => {
            let mut black = vec![false; width * height];
            let mut current: Vec<i32> = (0..width).map(|x| at(x, 0)).collect();
            let mut next = vec![0; width];
            for y in 0..height {
                if y + 1 < height {
                    next.iter_mut()
                        .enumerate()
                        .for_each(|(x, v)| *v = at(x, y + 1));
                }
                for x in 0..width {
                    let is_black = current[x] <= 127;
                    black[y * width + x] = is_black;
                    // Integer division truncates toward zero, as in Java.
                    let error = current[x] - if is_black { 0 } else { 255 };
                    if x + 1 < width {
                        current[x + 1] += 7 * error / 16;
                        if y + 1 < height {
                            next[x + 1] += error / 16;
                        }
                    }
                    if y + 1 < height {
                        next[x] += 5 * error / 16;
                        if x > 0 {
                            next[x - 1] += 3 * error / 16;
                        }
                    }
                }
                std::mem::swap(&mut current, &mut next);
            }
            black
        }
    }
}

fn threshold_matrix(algorithm: Dithering) -> Vec<Vec<i32>> {
    if algorithm == Dithering::Ordered {
        // LibLaserCut uses 256 at [3][0], which engraves dots on pure white;
        // 255 keeps white untouched.
        return vec![
            vec![16, 144, 48, 176],
            vec![208, 80, 240, 112],
            vec![64, 192, 32, 160],
            vec![255, 128, 224, 96],
        ];
    }
    // 8×8 clustered dot matrix from http://caca.zoy.org/study/part2.html.
    let base = [
        [24, 10, 12, 26, 35, 47, 49, 37],
        [8, 0, 2, 14, 45, 59, 61, 51],
        [22, 6, 4, 16, 43, 57, 63, 53],
        [30, 20, 18, 28, 33, 41, 55, 39],
        [34, 46, 48, 36, 25, 11, 13, 27],
        [44, 58, 60, 50, 9, 1, 3, 15],
        [42, 56, 62, 52, 23, 7, 5, 17],
        [32, 40, 54, 38, 31, 21, 19, 29],
    ];
    base.iter()
        .map(|row| {
            row.iter()
                .map(|v| {
                    let t = (1 + v) * 256 / 65;
                    if algorithm == Dithering::BrightenedHalftone {
                        brighten(t)
                    } else {
                        t
                    }
                })
                .collect()
        })
        .collect()
}

/// Inverse brightness curve applied to the threshold matrix (BrightenedHalftone).
fn brighten(value: i32) -> i32 {
    let output = [0.0, 192.0, 250.0, 255.0];
    let input = [0.0, 64.0, 230.0, 255.0];
    let v = value as f64;
    let mapped = if v < output[0] {
        input[0]
    } else if v > output[3] {
        input[3]
    } else {
        let i = (1..4)
            .find(|i| output[i - 1] <= v && v <= output[*i])
            .unwrap();
        (v - output[i - 1]) / (output[i] - output[i - 1]) * (input[i] - input[i - 1]) + input[i - 1]
    };
    mapped.round() as i32
}

/// One engraving line: packed bits (MSB first) for normal engraving, or one
/// power byte per pixel (0 = off, 255 = full) for 3D engraving.
pub struct Raster {
    pub width: usize,
    pub height: usize,
    pub bits_per_pixel: u8,
    pub rows: Vec<Vec<u8>>,
}

impl Raster {
    pub fn engrave(pixmap: &tiny_skia::Pixmap, settings: &RasterSettings) -> Self {
        let (width, height) = (pixmap.width() as usize, pixmap.height() as usize);
        let black = dither(
            &greyscale(pixmap, settings),
            width,
            height,
            settings.dithering,
        );
        let rows = (0..height)
            .map(|y| {
                let mut row = vec![0u8; width.div_ceil(8)];
                for x in 0..width {
                    if black[y * width + x] {
                        row[x / 8] |= 0x80 >> (x % 8);
                    }
                }
                row
            })
            .collect();
        Self {
            width,
            height,
            bits_per_pixel: 1,
            rows,
        }
    }

    /// 3D engraving: laser power follows darkness (Raster3dPart).
    pub fn engrave_3d(pixmap: &tiny_skia::Pixmap, settings: &RasterSettings) -> Self {
        let (width, height) = (pixmap.width() as usize, pixmap.height() as usize);
        let grey = greyscale(pixmap, settings);
        let rows = grey
            .chunks(width)
            .map(|r| r.iter().map(|g| 255 - g).collect())
            .collect();
        Self {
            width,
            height,
            bits_per_pixel: 8,
            rows,
        }
    }

    /// Engraving intensity 0 … 255 of a pixel, for previews.
    pub fn intensity(&self, x: usize, y: usize) -> u8 {
        if self.bits_per_pixel == 8 {
            self.rows[y][x]
        } else if self.rows[y][x / 8] & (0x80 >> (x % 8)) != 0 {
            255
        } else {
            0
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn gradient(width: usize) -> Vec<u8> {
        (0..width * 16)
            .map(|i| ((i % width) * 255 / (width - 1)) as u8)
            .collect()
    }

    #[test]
    fn halftone_tables_match_liblasercut() {
        assert_eq!(threshold_matrix(Dithering::Halftone)[1][1], 3);
        assert_eq!(threshold_matrix(Dithering::Halftone)[2][6], 252);
        // Brightened: the inverse curve maps the knots 192 → 64, 250 → 230.
        assert_eq!(brighten(0), 0);
        assert_eq!(brighten(192), 64);
        assert_eq!(brighten(250), 230);
        assert_eq!(brighten(255), 255);
    }

    #[test]
    fn all_algorithms_keep_black_and_white_and_follow_gradient_density() {
        let width = 64;
        let grey = gradient(width);
        for algorithm in Dithering::ALL {
            let black = dither(&grey, width, 16, algorithm);
            let dark: usize = (0..16)
                .map(|y| {
                    black[y * width..y * width + 8]
                        .iter()
                        .filter(|b| **b)
                        .count()
                })
                .sum();
            let light: usize = (0..16)
                .map(|y| {
                    black[y * width + 56..y * width + 64]
                        .iter()
                        .filter(|b| **b)
                        .count()
                })
                .sum();
            assert!(dark > light, "{algorithm:?}: {dark} ≤ {light}");
            let solid = dither(&[0; 64], 8, 8, algorithm);
            // Average and Grid compare with the image mean, so solid black stays
            // unengraved; Random spares black with probability 1/256 (both as
            // in LibLaserCut).
            if !matches!(
                algorithm,
                Dithering::Grid | Dithering::Average | Dithering::Random
            ) {
                assert!(solid.iter().all(|b| *b), "{algorithm:?} black");
            }
            assert!(
                dither(&[255; 64], 8, 8, algorithm).iter().all(|b| !*b),
                "{algorithm:?} white"
            );
        }
    }

    #[test]
    fn floyd_steinberg_matches_reference_values() {
        // 50 % grey alternates in a checkerboard-like pattern.
        let black = dither(&[128, 128, 128, 128], 4, 1, Dithering::FloydSteinberg);
        assert_eq!(black, [false, true, false, true]);
    }

    #[test]
    fn greyscale_applies_shift_and_inversion_and_3d_power() {
        let mut pixmap = tiny_skia::Pixmap::new(2, 1).unwrap();
        pixmap
            .data_mut()
            .copy_from_slice(&[0, 0, 0, 255, 200, 200, 200, 255]);
        let settings = RasterSettings {
            color_shift: 20,
            ..Default::default()
        };
        assert_eq!(greyscale(&pixmap, &settings), [20, 220]);
        let inverted = RasterSettings {
            invert: true,
            ..Default::default()
        };
        assert_eq!(greyscale(&pixmap, &inverted), [255, 55]);
        let raster = Raster::engrave_3d(&pixmap, &RasterSettings::default());
        assert_eq!(raster.rows, [vec![255, 55]]);
        assert_eq!(raster.intensity(1, 0), 55);
        let bw = Raster::engrave(&pixmap, &RasterSettings::legacy());
        assert_eq!(bw.rows, [vec![0x80]]);
    }
}
