// Cut order port of LibLaserCut's InnerFirstVectorOptimizer and
// OptimizerUtils.joinContiguousLoopElements (LGPL-3.0-or-later).
use crate::geometry::{Contour, Point};
use std::collections::HashMap;

/// 0.9 px of the 500-DPI vector profile, Manhattan distance like Java.
const JOIN_TOLERANCE: f32 = 0.9 * 25.4 / 500.0;

/// VisiCut's default order "inner first" for all parameter sets of one step.
///
/// `contours` are cut by each of the `sets` parameter sets. As in Java, where
/// one `VectorPart` holds the shapes of every set and the optimizer sorts that
/// part once, all `(set, path)` entries are sorted together. Open paths meeting
/// end to end without a fork are joined first; the joins are the same for every
/// set because all sets cut the same contours. The sort is stable by bounding box
/// (max y ascending, min y descending, max x ascending, min x descending), so a
/// path comes before every path whose box strictly contains its own. Entries with
/// equal boxes keep the set-major input order: set 0 before set 1, and so on.
///
/// Returns the joined paths and the cutting sequence as `(set, index)` into them.
pub fn inner_first_sets(
    contours: Vec<Contour>,
    sets: usize,
) -> (Vec<Contour>, Vec<(usize, usize)>) {
    let paths = join(contours);
    let boxes: Vec<[f32; 4]> = paths.iter().map(|path| bbox(path)).collect();
    let mut order: Vec<(usize, usize)> = (0..sets)
        .flat_map(|set| (0..paths.len()).map(move |index| (set, index)))
        .collect();
    order.sort_by(|&(_, i), &(_, j)| {
        let (a, b) = (boxes[i], boxes[j]);
        a[3].total_cmp(&b[3])
            .then(b[1].total_cmp(&a[1]))
            .then(a[2].total_cmp(&b[2]))
            .then(b[0].total_cmp(&a[0]))
    });
    (paths, order)
}

fn bbox(path: &[Point]) -> [f32; 4] {
    path.iter()
        .fold([f32::MAX, f32::MAX, f32::MIN, f32::MIN], |b, p| {
            [
                b[0].min(p[0]),
                b[1].min(p[1]),
                b[2].max(p[0]),
                b[3].max(p[1]),
            ]
        })
}

fn closed(path: &[Point]) -> bool {
    path.len() > 1 && path[0] == path[path.len() - 1]
}

/// End `2i` is the start of path `i`, `2i + 1` its end.
fn endpoint(path: &[Point], end: usize) -> Point {
    if end & 1 == 0 {
        path[0]
    } else {
        path[path.len() - 1]
    }
}

/// Closed paths first, then open paths chained wherever two ends are each
/// other's only neighbour within the tolerance (forks stay apart).
fn join(contours: Vec<Contour>) -> Vec<Contour> {
    let key = |p: Point| {
        (
            (p[0] / JOIN_TOLERANCE).floor() as i64,
            (p[1] / JOIN_TOLERANCE).floor() as i64,
        )
    };
    let mut cells = HashMap::<(i64, i64), Vec<usize>>::new();
    for (i, path) in contours.iter().enumerate() {
        if !closed(path) {
            for e in [2 * i, 2 * i + 1] {
                cells.entry(key(endpoint(path, e))).or_default().push(e);
            }
        }
    }
    let neighbours = |e: usize| {
        let p = endpoint(&contours[e / 2], e);
        let (x, y) = key(p);
        let mut found = Vec::new();
        for (dx, dy) in (-1..=1).flat_map(|dx| (-1..=1).map(move |dy| (dx, dy))) {
            for &f in cells.get(&(x + dx, y + dy)).into_iter().flatten() {
                let q = endpoint(&contours[f / 2], f);
                if f / 2 != e / 2 && (p[0] - q[0]).abs() + (p[1] - q[1]).abs() < JOIN_TOLERANCE {
                    found.push(f);
                }
            }
        }
        found
    };
    let mut link = vec![None; contours.len() * 2];
    for &e in cells.values().flatten() {
        if let [f] = neighbours(e)[..]
            && neighbours(f) == [e]
        {
            link[e] = Some(f);
        }
    }
    drop(cells);

    let mut result: Vec<Contour> = contours.iter().filter(|p| closed(p)).cloned().collect();
    let mut used: Vec<bool> = contours.iter().map(|p| closed(p)).collect();
    // Chains start at a free end; open paths left afterwards form rings.
    for rings in [false, true] {
        for i in 0..contours.len() {
            let entry = match (link[2 * i], link[2 * i + 1]) {
                _ if used[i] => continue,
                (None, _) => 2 * i,
                (_, None) => 2 * i + 1,
                _ if rings => 2 * i,
                _ => continue,
            };
            let mut chain = Vec::new();
            let mut e = entry;
            loop {
                used[e / 2] = true;
                chain.push(e);
                match link[e ^ 1] {
                    Some(f) if !used[f / 2] => e = f,
                    _ => break,
                }
            }
            // The earliest path in the file keeps its direction.
            if chain
                .iter()
                .min_by_key(|&&e| e / 2)
                .is_some_and(|&e| e & 1 == 1)
            {
                chain.reverse();
                chain.iter_mut().for_each(|e| *e ^= 1);
            }
            let mut path = Contour::new();
            for e in chain {
                let part = &contours[e / 2];
                let skip = usize::from(!path.is_empty());
                if e & 1 == 0 {
                    path.extend(part.iter().skip(skip));
                } else {
                    path.extend(part.iter().rev().skip(skip));
                }
            }
            result.push(path);
        }
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    fn square(x: f32, y: f32, size: f32) -> Contour {
        vec![
            [x, y],
            [x + size, y],
            [x + size, y + size],
            [x, y + size],
            [x, y],
        ]
    }

    /// Single parameter set: the paths in cutting order.
    fn inner_first(contours: Vec<Contour>) -> Vec<Contour> {
        let (paths, order) = inner_first_sets(contours, 1);
        order.into_iter().map(|(_, i)| paths[i].clone()).collect()
    }

    #[test]
    fn parameter_sets_of_one_step_are_sorted_together() {
        let outer = square(10.0, 10.0, 100.0);
        let hole = square(50.0, 50.0, 10.0);
        let (paths, order) = inner_first_sets(vec![outer.clone(), hole.clone()], 2);
        // The hole comes first with both sets, then the outline with both sets.
        let cut: Vec<(Contour, usize)> = order
            .into_iter()
            .map(|(set, i)| (paths[i].clone(), set))
            .collect();
        assert_eq!(
            cut,
            vec![(hole.clone(), 0), (hole, 1), (outer.clone(), 0), (outer, 1)]
        );
    }

    fn travel(paths: &[Contour]) -> f32 {
        let mut at = [0.0f32, 0.0];
        let mut sum = 0.0;
        for p in paths {
            sum += (p[0][0] - at[0]).hypot(p[0][1] - at[1]);
            at = p[p.len() - 1];
        }
        sum
    }

    fn sorted(mut paths: Vec<Contour>) -> Vec<Contour> {
        paths.sort_by(|a, b| a.partial_cmp(b).unwrap());
        paths
    }

    #[test]
    fn hole_after_outer_contour_is_cut_first() {
        let outer = square(10.0, 10.0, 100.0);
        let hole = square(50.0, 50.0, 10.0);
        let order = inner_first(vec![outer.clone(), hole.clone()]);
        assert_eq!(order, vec![hole, outer]);
    }

    #[test]
    fn three_nesting_levels_are_cut_inside_out() {
        let a = square(0.0, 0.0, 300.0);
        let b = square(100.0, 100.0, 100.0);
        let c = square(140.0, 140.0, 20.0);
        let d = square(500.0, 0.0, 50.0);
        let order = inner_first(vec![a.clone(), b.clone(), d.clone(), c.clone()]);
        assert_eq!(order, vec![d, c, b, a]);
    }

    #[test]
    fn holes_touching_the_outline_still_come_first() {
        let outer = square(0.0, 0.0, 100.0);
        let bottom = square(40.0, 90.0, 10.0);
        let corner = square(90.0, 0.0, 10.0);
        let order = inner_first(vec![outer.clone(), bottom.clone(), corner.clone()]);
        assert_eq!(order, vec![corner, bottom, outer]);
    }

    #[test]
    fn row_sweep_reduces_travel() {
        let paths: Vec<Contour> = (0..400u32)
            .map(|i| (i * 7919) % 400)
            .map(|k| square((k % 20) as f32 * 30.0, (k / 20) as f32 * 25.0, 5.0))
            .collect();
        let order = inner_first(paths.clone());
        assert_eq!(order[0], square(0.0, 0.0, 5.0));
        assert_eq!(order[20], square(0.0, 25.0, 5.0));
        assert!(travel(&order) * 2.0 < travel(&paths));
        assert_eq!(sorted(order), sorted(paths));
    }

    #[test]
    fn open_paths_keep_direction_and_are_joined() {
        let single = vec![vec![[100.0, 0.0], [10.0, 0.0]]];
        assert_eq!(inner_first(single.clone()), single);

        let a = vec![[200.0, 200.0], [210.0, 200.0]];
        let b = vec![[220.0, 210.0], [210.0, 200.01]];
        assert_eq!(
            inner_first(vec![b, a]),
            vec![vec![[220.0, 210.0], [210.0, 200.01], [200.0, 200.0]]]
        );

        let fork = vec![
            vec![[0.0, 50.0], [10.0, 50.0]],
            vec![[10.0, 50.0], [20.0, 60.0]],
            vec![[10.0, 50.0], [20.0, 40.0]],
        ];
        assert_eq!(inner_first(fork).len(), 3);
    }

    #[test]
    fn split_rectangles_are_joined_into_rings_and_ordered() {
        let lines = |x: f32, y: f32, s: f32| {
            vec![
                vec![[x, y], [x + s, y]],
                vec![[x + s, y + s], [x, y + s]],
                vec![[x + s, y], [x + s, y + s]],
                vec![[x, y + s], [x, y]],
            ]
        };
        let mut paths = lines(0.0, 0.0, 100.0);
        paths.extend(lines(40.0, 40.0, 10.0));
        let order = inner_first(paths);
        assert_eq!(
            order,
            vec![square(40.0, 40.0, 10.0), square(0.0, 0.0, 100.0)]
        );
    }

    #[test]
    fn many_paths_stay_fast() {
        let mut paths = Vec::new();
        for i in 0..100_000 {
            let (x, y) = ((i % 250) as f32 * 4.0, (i / 250) as f32 * 1.5);
            paths.push(square(x, y, 1.0));
            paths.push(vec![[x + 2.0, y], [x + 3.0, y + 1.0]]);
        }
        let start = std::time::Instant::now();
        let order = inner_first(paths.clone());
        assert!(start.elapsed().as_secs() < 20);
        assert_eq!(sorted(order), sorted(paths));
    }
}
