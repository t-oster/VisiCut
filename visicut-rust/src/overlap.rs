//! Removal of duplicate cut lines: parts of a contour that run exactly along
//! a line already cut earlier in the same step (e.g. the shared edge of two
//! adjacent shapes, or an object placed twice). Cutting them again only burns
//! the edge, so they are dropped. Contours that lose nothing keep their exact
//! points, so jobs without duplicates are unchanged.

use crate::geometry::{Contour, Point};
use std::collections::HashMap;

/// Two segments overlap when all their end points lie within this distance
/// of the other's line (mm). Well below the 0.05 mm raster of the device.
pub const TOLERANCE_MM: f64 = 0.02;
/// Remaining pieces shorter than this are dropped as well (mm).
const MIN_PIECE_MM: f64 = 2.0 * TOLERANCE_MM;
/// Edge length of the spatial grid cells (mm).
const CELL_MM: f64 = 5.0;

/// What was removed.
#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct Removed {
    /// Number of segment parts removed.
    pub pieces: usize,
    /// Their total length in mm.
    pub length_mm: f64,
}

/// Returns the contours without the parts that duplicate earlier contours.
pub fn remove_duplicates(contours: Vec<Contour>) -> (Vec<Contour>, Removed) {
    let mut grid: HashMap<(i64, i64), Vec<usize>> = HashMap::new();
    let mut accepted: Vec<(Point, Point)> = Vec::new();
    let mut removed = Removed::default();
    let mut result = Vec::with_capacity(contours.len());
    for contour in contours {
        let mut pieces: Vec<Vec<(f64, f64)>> = Vec::with_capacity(contour.len());
        let mut changed = false;
        for segment in contour.windows(2) {
            let (a, b) = (segment[0], segment[1]);
            let length = distance(a, b);
            let mut covered = Vec::new();
            if length > 0.0 {
                let mut seen = Vec::new();
                for cell in cells(a, b) {
                    for &k in grid.get(&cell).into_iter().flatten() {
                        if seen.contains(&k) {
                            continue;
                        }
                        seen.push(k);
                        if let Some(interval) = overlap(a, b, accepted[k].0, accepted[k].1) {
                            covered.push(interval);
                        }
                    }
                }
            }
            let kept = uncovered(covered, length);
            let kept_length: f64 = kept.iter().map(|(t0, t1)| (t1 - t0) * length).sum();
            if kept != [(0.0, 1.0)] && length > 0.0 {
                changed = true;
                let lost = length - kept_length;
                if lost > 0.0 {
                    removed.pieces += 1;
                    removed.length_mm += lost;
                }
            }
            pieces.push(kept);
        }
        // Later contours are compared with this one as a whole: what it lost
        // is covered by earlier lines anyway.
        for segment in contour.windows(2) {
            let index = accepted.len();
            accepted.push((segment[0], segment[1]));
            for cell in cells(segment[0], segment[1]) {
                grid.entry(cell).or_default().push(index);
            }
        }
        if changed {
            result.extend(rebuild(&contour, &pieces));
        } else {
            result.push(contour);
        }
    }
    (result, removed)
}

fn distance(a: Point, b: Point) -> f64 {
    (b[0] - a[0]).hypot(b[1] - a[1])
}

/// Grid cells touched by the segment's bounding box widened by the tolerance.
fn cells(a: Point, b: Point) -> impl Iterator<Item = (i64, i64)> {
    let cell = |v: f64| (v / CELL_MM).floor() as i64;
    let (x0, x1) = (
        cell(a[0].min(b[0]) - TOLERANCE_MM),
        cell(a[0].max(b[0]) + TOLERANCE_MM),
    );
    let (y0, y1) = (
        cell(a[1].min(b[1]) - TOLERANCE_MM),
        cell(a[1].max(b[1]) + TOLERANCE_MM),
    );
    (x0..=x1).flat_map(move |x| (y0..=y1).map(move |y| (x, y)))
}

/// Distance of `p` from the infinite line through `a` and `b`.
fn line_distance(p: Point, a: Point, b: Point) -> f64 {
    let length = distance(a, b);
    ((b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])).abs() / length
}

/// Part of segment `a`–`b` (as parameters 0…1) that the earlier segment
/// `c`–`d` covers, if both lie on one line within the tolerance.
fn overlap(a: Point, b: Point, c: Point, d: Point) -> Option<(f64, f64)> {
    let length = distance(a, b);
    if distance(c, d) <= TOLERANCE_MM {
        return None;
    }
    let off = |p: Point, x: Point, y: Point| line_distance(p, x, y) > TOLERANCE_MM;
    if off(a, c, d) || off(b, c, d) {
        return None;
    }
    // A very short segment has no reliable direction; lying on `c`–`d` is enough.
    if length > TOLERANCE_MM && (off(c, a, b) || off(d, a, b)) {
        return None;
    }
    let along = |p: Point| {
        ((p[0] - a[0]) * (b[0] - a[0]) + (p[1] - a[1]) * (b[1] - a[1])) / (length * length)
    };
    let (tc, td) = (along(c), along(d));
    // No slack at the ends: lines that only touch end to end must keep their
    // full length. Tiny leftovers are dropped by the minimum piece length.
    let (lo, hi) = (tc.min(td).max(0.0), tc.max(td).min(1.0));
    (hi > lo).then_some((lo, hi))
}

/// The parameter ranges of 0…1 not covered by the intervals, without pieces
/// shorter than the minimum length. `[(0.0, 1.0)]` when nothing is covered.
fn uncovered(mut covered: Vec<(f64, f64)>, length: f64) -> Vec<(f64, f64)> {
    if covered.is_empty() || length <= 0.0 {
        return vec![(0.0, 1.0)];
    }
    covered.sort_by(|x, y| x.0.total_cmp(&y.0));
    let mut kept = Vec::new();
    let mut at = 0.0;
    for (lo, hi) in covered {
        if lo > at {
            kept.push((at, lo));
        }
        at = f64::max(at, hi);
    }
    if at < 1.0 {
        kept.push((at, 1.0));
    }
    kept.retain(|(t0, t1)| (t1 - t0) * length >= MIN_PIECE_MM);
    kept
}

/// Polylines from the kept parts of each segment; consecutive parts that meet
/// at a vertex stay connected, and on a closed contour the last and first
/// parts are joined again across the start point.
fn rebuild(contour: &[Point], pieces: &[Vec<(f64, f64)>]) -> Vec<Contour> {
    let at = |k: usize, t: f64| -> Point {
        let (a, b) = (contour[k], contour[k + 1]);
        if t == 0.0 {
            a
        } else if t == 1.0 {
            b
        } else {
            [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]
        }
    };
    let mut lines: Vec<Contour> = Vec::new();
    let mut open = false;
    for (k, kept) in pieces.iter().enumerate() {
        for &(t0, t1) in kept {
            let (p0, p1) = (at(k, t0), at(k, t1));
            match lines.last_mut() {
                Some(line) if open && t0 == 0.0 && line.last() == Some(&p0) => line.push(p1),
                _ => lines.push(vec![p0, p1]),
            }
            open = t1 == 1.0;
        }
        if kept.is_empty() {
            open = false;
        }
    }
    let closed = contour.len() > 2 && contour.first() == contour.last();
    if closed && lines.len() >= 2 {
        let starts_at_start = pieces
            .first()
            .and_then(|p| p.first())
            .is_some_and(|p| p.0 == 0.0);
        let ends_at_end = pieces
            .last()
            .and_then(|p| p.last())
            .is_some_and(|p| p.1 == 1.0);
        if starts_at_start && ends_at_end {
            let first = lines.remove(0);
            lines.last_mut().unwrap().extend(first.into_iter().skip(1));
        }
    }
    lines
}

#[cfg(test)]
mod tests {
    use super::*;

    fn square(x: f64, y: f64, s: f64) -> Contour {
        vec![[x, y], [x + s, y], [x + s, y + s], [x, y + s], [x, y]]
    }

    fn length(contours: &[Contour]) -> f64 {
        contours
            .iter()
            .flat_map(|c| c.windows(2))
            .map(|w| distance(w[0], w[1]))
            .sum()
    }

    #[test]
    fn unrelated_contours_stay_exactly_the_same() {
        let input = vec![square(0.0, 0.0, 10.0), square(20.0, 0.0, 10.0)];
        let (output, removed) = remove_duplicates(input.clone());
        assert_eq!(output, input);
        assert_eq!(removed, Removed::default());
    }

    #[test]
    fn shared_edge_of_adjacent_squares_is_cut_once() {
        let (output, removed) =
            remove_duplicates(vec![square(0.0, 0.0, 10.0), square(10.0, 0.0, 10.0)]);
        assert_eq!(output[0], square(0.0, 0.0, 10.0));
        // The second square loses its left edge and becomes one open path.
        assert_eq!(output.len(), 2);
        assert_eq!(
            output[1],
            vec![[10.0, 0.0], [20.0, 0.0], [20.0, 10.0], [10.0, 10.0]]
        );
        assert_eq!(removed.pieces, 1);
        assert!((removed.length_mm - 10.0).abs() < 1e-9);
        assert!((length(&output) - 70.0).abs() < 1e-9);
    }

    #[test]
    fn duplicated_and_reversed_objects_disappear() {
        let a = square(5.0, 5.0, 20.0);
        let mut reversed = a.clone();
        reversed.reverse();
        let (output, removed) = remove_duplicates(vec![a.clone(), a.clone(), reversed]);
        assert_eq!(output, vec![a]);
        assert_eq!(removed.pieces, 8);
        assert!((removed.length_mm - 160.0).abs() < 1e-9);
    }

    #[test]
    fn partial_overlap_keeps_the_rest_and_tolerates_tiny_offsets() {
        let first = vec![[0.0, 0.0], [10.0, 0.0]];
        // Overlaps 4 mm of the first line, 0.01 mm beside it.
        let second = vec![[6.0, 0.01], [16.0, 0.01]];
        let (output, removed) = remove_duplicates(vec![first.clone(), second]);
        assert_eq!(output[0], first);
        assert_eq!(output.len(), 2);
        let rest = &output[1];
        assert!((rest[0][0] - 10.0).abs() < 1e-9, "{rest:?}");
        assert_eq!(rest[1], [16.0, 0.01]);
        assert_eq!(removed.pieces, 1);
    }

    #[test]
    fn parallel_lines_further_apart_are_kept() {
        let input = vec![vec![[0.0, 0.0], [10.0, 0.0]], vec![[0.0, 0.1], [10.0, 0.1]]];
        let (output, removed) = remove_duplicates(input.clone());
        assert_eq!(output, input);
        assert_eq!(removed.pieces, 0);
    }

    #[test]
    fn closed_contour_with_a_removed_middle_edge_joins_across_its_start() {
        // A rectangle starting in the middle of its bottom edge; its right
        // edge duplicates an earlier line.
        let line = vec![[10.0, 0.0], [10.0, 10.0]];
        let ring = vec![
            [5.0, 0.0],
            [10.0, 0.0],
            [10.0, 10.0],
            [0.0, 10.0],
            [0.0, 0.0],
            [5.0, 0.0],
        ];
        let (output, _) = remove_duplicates(vec![line, ring]);
        assert_eq!(output.len(), 2);
        assert_eq!(
            output[1],
            vec![
                [10.0, 10.0],
                [0.0, 10.0],
                [0.0, 0.0],
                [5.0, 0.0],
                [10.0, 0.0]
            ]
        );
    }
}
