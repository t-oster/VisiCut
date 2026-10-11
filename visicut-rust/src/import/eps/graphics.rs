//! Matrices, path construction and arc approximation for the PostScript
//! interpreter. Paths are stored in device space, as PostScript transforms
//! points when they are added to a path; the SVG path data is written directly.
use std::fmt::Write;

/// PostScript matrix `[a b c d tx ty]`: a point `(x, y)` maps to
/// `(a·x + c·y + tx, b·x + d·y + ty)`.
pub(super) type Matrix = [f64; 6];

pub(super) const IDENTITY: Matrix = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0];

/// The transformation that applies `first`, then `then` (PostScript's
/// `CTM = M × CTM` for `concat`).
pub(super) fn multiply(first: Matrix, then: Matrix) -> Matrix {
    let [a, b, c, d, e, f] = first;
    let [p, q, r, s, t, u] = then;
    [
        a * p + b * r,
        a * q + b * s,
        c * p + d * r,
        c * q + d * s,
        e * p + f * r + t,
        e * q + f * s + u,
    ]
}

/// Transforms a point by the whole matrix, including the translation.
pub(super) fn point(m: Matrix, x: f64, y: f64) -> (f64, f64) {
    (m[0] * x + m[2] * y + m[4], m[1] * x + m[3] * y + m[5])
}

/// Transforms a displacement by the linear part of the matrix only.
pub(super) fn vector(m: Matrix, dx: f64, dy: f64) -> (f64, f64) {
    (m[0] * dx + m[2] * dy, m[1] * dx + m[3] * dy)
}

/// Uniform scale of a matrix, used to convert line widths to device units.
pub(super) fn scale_of(m: Matrix) -> f64 {
    (m[0] * m[3] - m[1] * m[2]).abs().sqrt()
}

/// Formats a coordinate with at most four decimals, without trailing zeros.
pub(super) fn num(value: f64) -> String {
    let text = format!("{value:.4}");
    let trimmed = text.trim_end_matches('0').trim_end_matches('.');
    if trimmed == "-0" {
        "0".into()
    } else {
        trimmed.into()
    }
}

/// Cubic Bézier pieces `[control 1, control 2, end]` of an arc in user space.
///
/// `start` and `end` are angles in degrees. Counter-clockwise arcs (`arc`)
/// sweep upwards from `start` to `end`, clockwise ones (`arcn`) downwards.
/// Each piece covers at most 90 degrees.
pub(super) fn arc_curves(
    cx: f64,
    cy: f64,
    r: f64,
    start: f64,
    end: f64,
    clockwise: bool,
) -> Vec<[(f64, f64); 3]> {
    let difference = end - start;
    let sweep = if clockwise {
        if difference <= 0.0 {
            difference
        } else {
            difference.rem_euclid(360.0) - 360.0
        }
    } else if difference >= 0.0 {
        difference
    } else {
        difference.rem_euclid(360.0)
    };
    // Bounded so that a huge angle cannot create an enormous path.
    let sweep = sweep.clamp(-3600.0, 3600.0);
    if sweep.abs() < 1e-9 {
        return Vec::new();
    }
    let count = (sweep.abs() / 90.0).ceil() as usize;
    let step = sweep / count as f64;
    // Control point distance for a cubic approximation of one piece.
    let k = 4.0 / 3.0 * (step.to_radians() / 4.0).tan() * r;
    (0..count)
        .map(|i| {
            let t0 = (start + step * i as f64).to_radians();
            let t1 = t0 + step.to_radians();
            let (s0, c0) = t0.sin_cos();
            let (s1, c1) = t1.sin_cos();
            let p3 = (cx + r * c1, cy + r * s1);
            [
                (cx + r * c0 - k * s0, cy + r * s0 + k * c0),
                (p3.0 + k * s1, p3.1 - k * c1),
                p3,
            ]
        })
        .collect()
}

/// The current path in device space as SVG path data.
#[derive(Default)]
pub(super) struct Path {
    data: String,
    /// Whether a line or curve was added, so that painting has something to draw.
    drawn: bool,
    current: Option<(f64, f64)>,
    subpath_start: (f64, f64),
}

impl Path {
    pub(super) fn clear(&mut self) {
        *self = Self::default();
    }

    pub(super) fn current(&self) -> Option<(f64, f64)> {
        self.current
    }

    pub(super) fn drawn(&self) -> bool {
        self.drawn
    }

    pub(super) fn bytes(&self) -> usize {
        self.data.len()
    }

    pub(super) fn data(&self) -> &str {
        self.data.trim_end()
    }

    pub(super) fn move_to(&mut self, (x, y): (f64, f64)) {
        let _ = write!(self.data, "M{} {} ", num(x), num(y));
        self.current = Some((x, y));
        self.subpath_start = (x, y);
    }

    /// Callers check that there is a current point.
    pub(super) fn line_to(&mut self, (x, y): (f64, f64)) {
        let _ = write!(self.data, "L{} {} ", num(x), num(y));
        self.current = Some((x, y));
        self.drawn = true;
    }

    /// Callers check that there is a current point.
    pub(super) fn curve_to(&mut self, c1: (f64, f64), c2: (f64, f64), end: (f64, f64)) {
        let _ = write!(
            self.data,
            "C{} {} {} {} {} {} ",
            num(c1.0),
            num(c1.1),
            num(c2.0),
            num(c2.1),
            num(end.0),
            num(end.1)
        );
        self.current = Some(end);
        self.drawn = true;
    }

    /// Closes the current subpath and returns to its start point.
    pub(super) fn close(&mut self) {
        if self.current.is_some() {
            self.data.push_str("Z ");
            self.current = Some(self.subpath_start);
        }
    }
}
