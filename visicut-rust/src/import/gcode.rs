//! G-code toolpaths (`.nc`, `.gcode`) as one SVG path, like Java's `GCodeImporter`.
//!
//! Like Java: `G0` moves without drawing, `G1` draws lines, `G2`/`G3` draw
//! clockwise/counter-clockwise arcs; `G20`/`G21` switch inch/mm; coordinates
//! are millimetres on the bed with Y pointing down (no mirroring); the
//! drawing keeps its absolute position because the bounds include the origin
//! and every visited point (Java's path starts with `moveTo(0, 0)`); laser
//! commands (`M3`/`M5`/`S`/`F`) and `Z` are ignored. Beyond Java, which
//! leaves these as TODOs or bugs: `G90`/`G91` (absolute/relative), arcs with
//! `R`, `I`/`J` relative to the arc start (standard; `G90.1` makes them
//! absolute as in Java), single-digit `G0`–`G3`, lower-case words and
//! comments in `(…)` and after `;`.
use super::Imported;
use crate::svg_import::MAX_SVG_BYTES;
use std::f64::consts::{FRAC_PI_2, PI, TAU};
use std::fmt::Write;
use std::path::Path;

const INCH_MM: f64 = 25.4;
const STROKE_MM: f64 = 0.1;
/// Java warns ("No real circle") when start and end radius differ this much.
const RADIUS_TOLERANCE_MM: f64 = 0.1;
const MAX_WARNINGS: usize = 10;
const EPSILON: f64 = 1e-9;

pub fn read(path: &Path) -> Result<Imported, String> {
    let bytes = std::fs::read(path).map_err(|e| e.to_string())?;
    convert(&String::from_utf8_lossy(&bytes))
}

type Point = (f64, f64);

struct Converter {
    /// 0 = move, 1 = line, 2 = clockwise arc, 3 = counter-clockwise arc.
    mode: u8,
    absolute: bool,
    absolute_arcs: bool,
    unit_mm: f64,
    position: Point,
    /// Whether the SVG path currently ends at `position`.
    pen: bool,
    data: String,
    min: Point,
    max: Point,
    drawn: bool,
    warnings: Vec<String>,
}

fn convert(text: &str) -> Result<Imported, String> {
    let mut converter = Converter {
        mode: 0,
        absolute: true,
        absolute_arcs: false,
        unit_mm: 1.0,
        position: (0.0, 0.0),
        pen: false,
        data: String::new(),
        min: (0.0, 0.0),
        max: (0.0, 0.0),
        drawn: false,
        warnings: Vec::new(),
    };
    for (index, line) in text.lines().enumerate() {
        converter.line(index + 1, line);
        if converter.data.len() > MAX_SVG_BYTES {
            return Err("G-Code ergibt eine SVG über 20 MB".into());
        }
    }
    converter.finish()
}

/// Words like `X12.5` of one line without comments; `,` counts as decimal point.
fn words(line: &str, number: usize, warnings: &mut Vec<String>) -> Vec<(char, f64)> {
    let mut code = String::with_capacity(line.len());
    let mut in_comment = false;
    for c in line.chars() {
        match c {
            '(' => in_comment = true,
            ')' if in_comment => in_comment = false,
            ';' if !in_comment => break,
            _ if !in_comment => code.push(c.to_ascii_uppercase()),
            _ => {}
        }
    }
    let mut result = Vec::new();
    let mut chars = code.chars().peekable();
    while let Some(letter) = chars.next() {
        if !letter.is_ascii_uppercase() {
            continue;
        }
        while chars.peek().is_some_and(|c| *c == ' ' || *c == '\t') {
            chars.next();
        }
        let mut value = String::new();
        while let Some(&c) = chars.peek() {
            if c.is_ascii_digit() || matches!(c, '+' | '-' | '.' | ',') {
                value.push(c);
                chars.next();
            } else {
                break;
            }
        }
        if value.is_empty() {
            continue;
        }
        match value.replace(',', ".").parse::<f64>() {
            Ok(parsed) if parsed.is_finite() => result.push((letter, parsed)),
            _ => {
                if warnings.len() < MAX_WARNINGS {
                    warnings.push(format!(
                        "Ungültiger G-Code in Zeile {number}: {letter}{value}"
                    ));
                }
            }
        }
    }
    result
}

impl Converter {
    fn line(&mut self, number: usize, line: &str) {
        let (mut x, mut y, mut i, mut j, mut r) = (None, None, None, None, None);
        for (letter, value) in words(line, number, &mut self.warnings) {
            match letter {
                'G' => match (value * 10.0).round() as i64 {
                    0 | 10 | 20 | 30 => self.mode = (value.round()) as u8,
                    200 => self.unit_mm = INCH_MM,
                    210 => self.unit_mm = 1.0,
                    900 => self.absolute = true,
                    910 => self.absolute = false,
                    901 => self.absolute_arcs = true,
                    911 => self.absolute_arcs = false,
                    _ => {}
                },
                'X' => x = Some(value),
                'Y' => y = Some(value),
                'I' => i = Some(value),
                'J' => j = Some(value),
                'R' => r = Some(value),
                _ => {}
            }
        }
        let arc = matches!(self.mode, 2 | 3);
        if x.is_none() && y.is_none() && !(arc && (i.is_some() || j.is_some())) {
            return;
        }
        let unit = self.unit_mm;
        let (px, py) = self.position;
        let target = if self.absolute {
            (x.map_or(px, |v| v * unit), y.map_or(py, |v| v * unit))
        } else {
            (px + x.unwrap_or(0.0) * unit, py + y.unwrap_or(0.0) * unit)
        };
        match self.mode {
            0 => {
                self.pen = false;
                self.include(target);
            }
            1 => {
                if target != self.position {
                    self.pen_down();
                    self.write('L', &[target]);
                }
            }
            _ => {
                let center = match r {
                    Some(radius) => self.center_from_radius(number, target, radius * unit),
                    None if self.absolute_arcs => {
                        Some((i.map_or(px, |v| v * unit), j.map_or(py, |v| v * unit)))
                    }
                    None => Some((px + i.unwrap_or(0.0) * unit, py + j.unwrap_or(0.0) * unit)),
                };
                if let Some(center) = center {
                    self.arc(number, target, center, self.mode == 2);
                }
            }
        }
        self.position = target;
    }

    /// Arc centre for the `R` form: positive radius takes the shorter arc,
    /// negative the longer one (same formula as grbl).
    fn center_from_radius(&mut self, number: usize, target: Point, radius: f64) -> Option<Point> {
        let (sx, sy) = self.position;
        let (dx, dy) = (target.0 - sx, target.1 - sy);
        let chord = dx.hypot(dy);
        if chord < EPSILON {
            self.warn(format!(
                "G-Code-Zeile {number}: Vollkreis mit R ist nicht eindeutig und wird übersprungen"
            ));
            return None;
        }
        let mut height = 4.0 * radius * radius - chord * chord;
        if height < 0.0 {
            if (2.0 * radius.abs() - chord) < -RADIUS_TOLERANCE_MM {
                self.warn(format!(
                    "G-Code-Zeile {number}: Radius ist kleiner als der halbe Abstand der Endpunkte"
                ));
            }
            height = 0.0;
        }
        let mut factor = height.sqrt() / chord;
        if self.mode == 2 {
            factor = -factor;
        }
        if radius < 0.0 {
            factor = -factor;
        }
        Some((sx + 0.5 * (dx - dy * factor), sy + 0.5 * (dy + dx * factor)))
    }

    fn arc(&mut self, number: usize, target: Point, center: Point, clockwise: bool) {
        let (sx, sy) = self.position;
        let (cx, cy) = center;
        let radius = (sx - cx).hypot(sy - cy);
        if radius < EPSILON {
            self.warn(format!("G-Code-Zeile {number}: Kreisbogen ohne Radius"));
            if target != self.position {
                self.pen_down();
                self.write('L', &[target]);
            }
            return;
        }
        if (radius - (target.0 - cx).hypot(target.1 - cy)).abs() >= RADIUS_TOLERANCE_MM {
            self.warn(format!(
                "G-Code-Zeile {number}: Kreisbogen hat unterschiedliche Start- und Endradien"
            ));
        }
        let start = (sy - cy).atan2(sx - cx);
        let end = (target.1 - cy).atan2(target.0 - cx);
        let mut sweep = if clockwise { start - end } else { end - start }.rem_euclid(TAU);
        if (target.0 - sx).hypot(target.1 - sy) < EPSILON {
            sweep = TAU;
        }
        let direction = if clockwise { -1.0 } else { 1.0 };
        self.pen_down();
        // SVG arcs cannot describe full circles, so long arcs are split in two.
        let pieces = if sweep > PI { 2 } else { 1 };
        for piece in 1..=pieces {
            let point = if piece == pieces {
                target
            } else {
                let angle = start + direction * sweep * piece as f64 / pieces as f64;
                (cx + radius * angle.cos(), cy + radius * angle.sin())
            };
            // Angles grow towards +Y; SVG's sweep flag 1 is that direction too.
            let _ = write!(
                self.data,
                "A{} {} 0 0 {}",
                number_text(radius),
                number_text(radius),
                u8::from(!clockwise)
            );
            self.write(' ', &[point]);
        }
        for quarter in 0..4 {
            let angle = quarter as f64 * FRAC_PI_2;
            let offset = (direction * (angle - start)).rem_euclid(TAU);
            if offset <= sweep {
                self.include((cx + radius * angle.cos(), cy + radius * angle.sin()));
            }
        }
    }

    fn pen_down(&mut self) {
        if !self.pen {
            let position = self.position;
            self.write('M', &[position]);
            self.pen = true;
        }
        self.drawn = true;
    }

    fn write(&mut self, command: char, points: &[Point]) {
        self.data.push(command);
        for (index, &point) in points.iter().enumerate() {
            if index > 0 {
                self.data.push(' ');
            }
            let _ = write!(
                self.data,
                "{} {}",
                number_text(point.0),
                number_text(point.1)
            );
            self.include(point);
        }
    }

    fn include(&mut self, (x, y): Point) {
        self.min = (self.min.0.min(x), self.min.1.min(y));
        self.max = (self.max.0.max(x), self.max.1.max(y));
    }

    fn warn(&mut self, warning: String) {
        if self.warnings.len() < MAX_WARNINGS && !self.warnings.contains(&warning) {
            self.warnings.push(warning);
        }
    }

    fn finish(self) -> Result<Imported, String> {
        if !self.drawn {
            return Err("G-Code enthält keine Bahnen (G1, G2 oder G3 mit Koordinaten)".into());
        }
        let width = (self.max.0 - self.min.0).max(STROKE_MM);
        let height = (self.max.1 - self.min.1).max(STROKE_MM);
        let svg = format!(
            r##"<svg xmlns="http://www.w3.org/2000/svg" width="{w}mm" height="{h}mm" viewBox="{x} {y} {w} {h}"><path d="{d}" fill="none" stroke="#ff0000" stroke-width="{STROKE_MM}"/></svg>"##,
            w = number_text(width),
            h = number_text(height),
            x = number_text(self.min.0),
            y = number_text(self.min.1),
            d = self.data,
        );
        if svg.len() > MAX_SVG_BYTES {
            return Err("G-Code ergibt eine SVG über 20 MB".into());
        }
        Ok(Imported {
            svg,
            warnings: self.warnings,
            ..Default::default()
        })
    }
}

fn number_text(value: f64) -> String {
    let text = format!("{value:.4}");
    let text = text.trim_end_matches('0').trim_end_matches('.');
    if text == "-0" { "0" } else { text }.to_string()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::geometry::Contour;
    use crate::project::Project;

    /// Contours in G-code coordinates (`origin` = top-left of the SVG bounds).
    fn contours(gcode: &str, origin: (f32, f32)) -> Vec<Contour> {
        let imported = convert(gcode).unwrap();
        let preview = crate::svg::render(&imported.svg).unwrap();
        crate::geometry::contours(&Project {
            svg: imported.svg,
            x_mm: origin.0,
            y_mm: origin.1,
            width_mm: preview.width_mm,
            height_mm: preview.height_mm,
            ..Default::default()
        })
        .unwrap()
    }

    fn size(gcode: &str) -> (f32, f32) {
        let preview = crate::svg::render(&convert(gcode).unwrap().svg).unwrap();
        (preview.width_mm, preview.height_mm)
    }

    fn near(a: [f32; 2], b: [f32; 2]) -> bool {
        (a[0] - b[0]).abs() < 0.01 && (a[1] - b[1]).abs() < 0.01
    }

    fn assert_points(contour: &Contour, expected: &[[f32; 2]]) {
        for point in expected {
            assert!(
                contour.iter().any(|p| near(*p, *point)),
                "{point:?} missing in {contour:?}"
            );
        }
    }

    fn assert_extent(contour: &Contour, min: [f32; 2], max: [f32; 2]) {
        let low = contour
            .iter()
            .fold([f32::MAX; 2], |a, p| [a[0].min(p[0]), a[1].min(p[1])]);
        let high = contour
            .iter()
            .fold([f32::MIN; 2], |a, p| [a[0].max(p[0]), a[1].max(p[1])]);
        let close =
            |a: [f32; 2], b: [f32; 2]| (a[0] - b[0]).abs() < 0.02 && (a[1] - b[1]).abs() < 0.02;
        assert!(
            close(low, min) && close(high, max),
            "extent {low:?}..{high:?}"
        );
    }

    fn assert_on_circle(contour: &Contour, center: [f32; 2], radius: f32) {
        for p in contour {
            let distance = (p[0] - center[0]).hypot(p[1] - center[1]);
            assert!((distance - radius).abs() < 0.05, "{p:?} off circle");
        }
    }

    #[test]
    fn square_with_g1_keeps_position_and_size() {
        let gcode =
            "G21 G90\nG0 X10 Y10\nG1 X30 Y10 F1000 S500\nX30 Y20\nX10 Y20\nX10 Y10\nM5\nG0 X0 Y0\n";
        let result = contours(gcode, (0.0, 0.0));
        assert_eq!(result.len(), 1);
        assert_points(
            &result[0],
            &[[10.0, 10.0], [30.0, 10.0], [30.0, 20.0], [10.0, 20.0]],
        );
        let (width, height) = size(gcode);
        assert!((width - 30.0).abs() < 0.01 && (height - 20.0).abs() < 0.01);
    }

    #[test]
    fn g0_moves_are_not_drawn() {
        let gcode = "G1 X10 Y0\nG0 X20 Y0\nG1 X30 Y0\n";
        let result = contours(gcode, (0.0, 0.0));
        assert_eq!(result.len(), 2);
        assert_points(&result[0], &[[0.0, 0.0], [10.0, 0.0]]);
        assert_points(&result[1], &[[20.0, 0.0], [30.0, 0.0]]);
    }

    #[test]
    fn arcs_with_ij_follow_direction() {
        // Counter-clockwise quarter from 0° to 90° around the origin.
        let ccw = contours("G0 X10 Y0\nG3 X0 Y10 I-10 J0\n", (0.0, 0.0));
        assert_eq!(ccw.len(), 1);
        assert_on_circle(&ccw[0], [0.0, 0.0], 10.0);
        assert!(ccw[0].iter().all(|p| p[0] > -0.01 && p[1] > -0.01));
        assert_points(&ccw[0], &[[10.0, 0.0], [0.0, 10.0]]);

        // Clockwise the same endpoints take the 270° way round.
        let cw = contours("G0 X10 Y0\nG2 X0 Y10 I-10 J0\n", (-10.0, -10.0));
        assert_on_circle(&cw[0], [0.0, 0.0], 10.0);
        assert_points(&cw[0], &[[10.0, 0.0], [0.0, 10.0]]);
        assert_extent(&cw[0], [-10.0, -10.0], [10.0, 10.0]);

        // Full circle when start and end coincide.
        let full = contours("G0 X20 Y10\nG2 X20 Y10 I-5\n", (0.0, 0.0));
        assert_on_circle(&full[0], [15.0, 10.0], 5.0);
        assert_extent(&full[0], [10.0, 5.0], [20.0, 15.0]);
    }

    #[test]
    fn arcs_with_radius() {
        // R > 0: short arc, centre (5, -8.66) for a clockwise move.
        let short = contours("G2 X10 Y0 R10\n", (0.0, 0.0));
        let centre = [5.0, -8.6603];
        assert_on_circle(&short[0], centre, 10.0);
        assert_points(&short[0], &[[0.0, 0.0], [10.0, 0.0]]);
        assert_extent(&short[0], [0.0, 0.0], [10.0, 1.3397]);

        // R < 0: long counter-clockwise arc around (15, -8.66).
        let long = contours("G0 X10 Y0\nG3 X20 Y0 R-10\n", (0.0, -18.6603));
        assert_on_circle(&long[0], [15.0, -8.6603], 10.0);
        assert_points(&long[0], &[[10.0, 0.0], [20.0, 0.0]]);
        assert_extent(&long[0], [5.0, -18.6603], [25.0, 0.0]);
    }

    #[test]
    fn inch_units_are_converted() {
        let gcode = "G20\nG0 X1 Y1\nG1 X2 Y1\nX2 Y2\n";
        let result = contours(gcode, (0.0, 0.0));
        assert_points(&result[0], &[[25.4, 25.4], [50.8, 25.4], [50.8, 50.8]]);
        let (width, height) = size(gcode);
        assert!((width - 50.8).abs() < 0.01 && (height - 50.8).abs() < 0.01);
    }

    #[test]
    fn relative_mode_adds_offsets() {
        let gcode = "G91\nG0 X5 Y5\nG1 X10\nY10\nX-10\nY-10\nG90\nG0 X0 Y0\n";
        let result = contours(gcode, (0.0, 0.0));
        assert_eq!(result.len(), 1);
        assert_points(
            &result[0],
            &[[5.0, 5.0], [15.0, 5.0], [15.0, 15.0], [5.0, 15.0]],
        );
    }

    #[test]
    fn comments_line_numbers_and_number_formats() {
        let gcode = "%\n(header X500 Y500)\nN10 g0 x1 y1 ; park X900\nN20 G01 X2,5 Y1 (cut Y99)\nN30 G1X2.Y3\n";
        let result = contours(gcode, (0.0, 0.0));
        assert_points(&result[0], &[[1.0, 1.0], [2.5, 1.0], [2.0, 3.0]]);
        let (width, height) = size(gcode);
        assert!((width - 2.5).abs() < 0.01 && (height - 3.0).abs() < 0.01);
    }

    #[test]
    fn invalid_words_warn_and_empty_programs_fail() {
        let imported = convert("G1 X1.2.3 Y1\nG1 X4 Y4\n").unwrap();
        assert_eq!(imported.warnings.len(), 1);
        assert!(imported.warnings[0].contains("X1.2.3"));
        assert!(convert("G0 X10 Y10\nM3 S100\nM5\n").is_err());
        assert!(convert("").is_err());
    }

    #[test]
    fn reads_from_disk() {
        let path = std::env::temp_dir().join(format!("visicut-gcode-{}.nc", std::process::id()));
        std::fs::write(&path, "G1 X3 Y4\n").unwrap();
        let result = read(&path);
        let _ = std::fs::remove_file(&path);
        assert!(result.unwrap().svg.contains("d=\"M0 0L3 4\""));
    }
}
