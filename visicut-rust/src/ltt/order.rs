// Cut order port of LibLaserCut's InnerFirstVectorOptimizer, VectorOptimizer
// and OptimizerUtils.joinContiguousLoopElements (LGPL-3.0-or-later).
//
// The port is literal on purpose: output must match the Java driver byte for
// byte (tests/java_parity), including its tie-breaking and join quirks.

/// Join tolerance of joinContiguousLoopElements: 0.9 px, Manhattan distance.
const JOIN_TOLERANCE: f64 = 0.9;

/// One path of a vector part in 500-DPI pixels (Java `VectorOptimizer.Element`).
#[derive(Clone, Debug, PartialEq)]
pub struct Element {
    /// Parameter set cutting this path; see [`SetKey`] for equality.
    pub set: usize,
    pub start: [f64; 2],
    pub moves: Vec<[f64; 2]>,
}

impl Element {
    pub fn end(&self) -> [f64; 2] {
        *self.moves.last().unwrap_or(&self.start)
    }

    fn is_closed(&self) -> bool {
        !self.moves.is_empty() && same(self.end(), self.start)
    }

    /// Java `Element.invert`.
    fn invert(&mut self) {
        if !self.moves.is_empty() {
            self.moves.insert(0, self.start);
            self.start = self.moves.pop().unwrap();
            self.moves.reverse();
        }
    }

    /// `[xmin, ymin, xmax, ymax]`.
    fn bounds(&self) -> [f64; 4] {
        self.moves.iter().fold(
            [self.start[0], self.start[1], self.start[0], self.start[1]],
            |b, p| {
                [
                    b[0].min(p[0]),
                    b[1].min(p[1]),
                    b[2].max(p[0]),
                    b[3].max(p[1]),
                ]
            },
        )
    }
}

/// Java `Point.equals`: bitwise equal coordinates.
fn same(a: [f64; 2], b: [f64; 2]) -> bool {
    a[0].to_bits() == b[0].to_bits() && a[1].to_bits() == b[1].to_bits()
}

/// The values of a parameter set that LibLaserCut's `LaosCutterProperty`
/// compares. Sets with equal values are the same property in Java: they share
/// a join group and switching between them sends no commands.
#[derive(Clone, Copy, Debug)]
pub struct SetKey {
    pub power: f32,
    pub speed: f32,
    /// Material thickness, which VisiCut adds to the profile focus (0 for FAU).
    pub focus: f32,
}

impl SetKey {
    fn bits(self) -> [u32; 3] {
        [
            self.power.to_bits(),
            self.speed.to_bits(),
            self.focus.to_bits(),
        ]
    }

    pub fn same(self, other: SetKey) -> bool {
        self.bits() == other.bits()
    }

    /// `LaosCutterProperty.hashCode` with ventilation and purge on and
    /// frequency 500, as created by `getLaserPropertyForVectorPart`.
    fn java_hash(self) -> i32 {
        let bits = |f: f32| f.to_bits() as i32;
        let mut inner: i32 = 7;
        inner = inner.wrapping_mul(67).wrapping_add(bits(self.power));
        inner = inner.wrapping_mul(67).wrapping_add(bits(self.speed));
        inner = inner.wrapping_mul(67).wrapping_add(bits(self.focus));
        inner = inner.wrapping_mul(67).wrapping_add(500);
        let mut hash: i32 = 5;
        hash = hash.wrapping_mul(97).wrapping_add(1); // ventilation
        hash = hash.wrapping_mul(97).wrapping_add(1); // purge
        hash.wrapping_mul(97).wrapping_add(inner)
    }
}

/// Java `InnerFirstVectorOptimizer.optimize`: joins open paths of equal
/// properties, then sorts stably by bounding box (max y ascending, min y
/// descending, max x ascending, min x descending), so a path comes before
/// every path whose box strictly contains its own.
pub fn inner_first(elements: Vec<Element>, keys: &[SetKey]) -> Vec<Element> {
    let mut result = Vec::with_capacity(elements.len());
    for group in java_groups(elements, keys) {
        result.extend(join_contiguous(group));
    }
    let boxes: Vec<[f64; 4]> = result.iter().map(Element::bounds).collect();
    let mut order: Vec<usize> = (0..result.len()).collect();
    order.sort_by(|&i, &j| {
        let (a, b) = (boxes[i], boxes[j]);
        a[3].total_cmp(&b[3])
            .then((-a[1]).total_cmp(&-b[1]))
            .then(a[2].total_cmp(&b[2]))
            .then((-a[0]).total_cmp(&-b[0]))
    });
    let mut slots: Vec<Option<Element>> = result.into_iter().map(Some).collect();
    order
        .into_iter()
        .map(|i| slots[i].take().unwrap())
        .collect()
}

/// `Collectors.groupingBy(el -> el.prop)`: a `HashMap` whose values come out
/// in bucket order of the property hash. `computeIfAbsent` puts a new key at
/// the head of its bucket, so equal buckets come out in reverse insertion
/// order (resizing keeps the relative order).
fn java_groups(elements: Vec<Element>, keys: &[SetKey]) -> Vec<Vec<Element>> {
    let mut groups: Vec<(SetKey, Vec<Element>)> = Vec::new();
    for element in elements {
        let key = keys[element.set];
        match groups.iter_mut().find(|(k, _)| k.same(key)) {
            Some((_, group)) => group.push(element),
            None => groups.push((key, vec![element])),
        }
    }
    // Default capacity 16, doubled whenever the size exceeds 3/4 of it.
    let mut capacity = 16usize;
    while groups.len() > capacity * 3 / 4 {
        capacity *= 2;
    }
    let bucket = |key: SetKey| {
        let h = key.java_hash() as u32;
        ((h ^ (h >> 16)) as usize) & (capacity - 1)
    };
    // Tree bins (eight or more keys in one bucket) are not modelled.
    let mut indexed: Vec<(usize, (SetKey, Vec<Element>))> =
        groups.into_iter().enumerate().collect();
    indexed.sort_by_key(|(i, (key, _))| (bucket(*key), std::cmp::Reverse(*i)));
    indexed.into_iter().map(|(_, (_, group))| group).collect()
}

/// Start or end point of an open path (Java `OptimizerUtils.DirectedElement`).
#[derive(Clone, Copy)]
struct Directed {
    index: usize,
    start: [f64; 2],
    inverted: bool,
}

struct Open {
    element: Element,
    start_index: usize,
    end_index: usize,
}

impl Open {
    fn invert(&mut self) {
        std::mem::swap(&mut self.start_index, &mut self.end_index);
        self.element.invert();
    }

    fn append(&mut self, other: Open) {
        self.element.moves.extend(other.element.moves);
        self.end_index = other.end_index;
    }
}

/// `OptimizerUtils.numElementsLeftOf`, including its result for a list whose
/// points are all left of `x` (the last index, not the length).
fn num_left_of(xs: &[f64], x: f64) -> usize {
    if xs.is_empty() {
        return 0;
    }
    let (mut start, mut end) = (-1i64, xs.len() as i64 - 1);
    while end - start > 1 {
        let mid = (start + end) / 2;
        if xs[mid as usize] > x {
            end = mid;
        } else {
            start = mid;
        }
    }
    end as usize
}

/// `OptimizerUtils.joinContiguousLoopElements` for one property: closed paths
/// first, then open paths merged wherever exactly one other end lies within
/// the tolerance (forks stay apart).
fn join_contiguous(group: Vec<Element>) -> Vec<Element> {
    let (mut result, open): (Vec<Element>, Vec<Element>) =
        group.into_iter().partition(Element::is_closed);
    let mut points: Vec<Option<Directed>> = Vec::with_capacity(open.len() * 2);
    for (i, element) in open.iter().enumerate() {
        points.push(Some(Directed {
            index: i,
            start: element.start,
            inverted: false,
        }));
        points.push(Some(Directed {
            index: i,
            start: element.end(),
            inverted: true,
        }));
    }
    // Collections.sort is stable.
    points.sort_by(|a, b| a.unwrap().start[0].total_cmp(&b.unwrap().start[0]));
    let xs: Vec<f64> = points.iter().map(|p| p.unwrap().start[0]).collect();
    let mut elements: Vec<Option<Open>> = open
        .into_iter()
        .map(|element| {
            Some(Open {
                element,
                start_index: 0,
                end_index: 0,
            })
        })
        .collect();
    for (i, point) in points.iter().enumerate() {
        let point = point.unwrap();
        let open = elements[point.index].as_mut().unwrap();
        if point.inverted {
            open.end_index = i;
        } else {
            open.start_index = i;
        }
    }

    let mut changed = true;
    while changed {
        changed = false;
        for current in 0..elements.len() {
            if elements[current].is_none() {
                continue;
            }
            let mut any_neighbours = false;
            for invert in [true, false] {
                let open = elements[current].as_ref().unwrap();
                let head = if invert {
                    open.element.end()
                } else {
                    open.element.start
                };
                let mut nearby = 0;
                let mut start_near = None;
                let mut end_near = None;
                for e in points[num_left_of(&xs, head[0] - JOIN_TOLERANCE)..]
                    .iter()
                    .flatten()
                {
                    if e.index == current {
                        continue;
                    }
                    if e.start[0] > head[0] + JOIN_TOLERANCE || nearby >= 2 {
                        break;
                    }
                    if (e.start[0] - head[0]).abs() + (e.start[1] - head[1]).abs() < JOIN_TOLERANCE
                    {
                        nearby += 1;
                        if e.inverted {
                            end_near = Some(e.index);
                        } else {
                            start_near = Some(e.index);
                        }
                    }
                }
                if nearby >= 1 {
                    any_neighbours = true;
                }
                if nearby != 1 {
                    continue;
                }
                let merged = if let Some(other) = start_near {
                    let mut cur = elements[current].take().unwrap();
                    let o = elements[other].take().unwrap();
                    if invert {
                        // current.end ---- other.start
                        points[cur.end_index] = None;
                    } else {
                        // current.start ---- other.start, by reversing current
                        points[cur.start_index] = None;
                        cur.invert();
                    }
                    points[o.start_index] = None;
                    cur.append(o);
                    elements[current] = Some(cur);
                    current
                } else {
                    let other = end_near.unwrap();
                    let mut cur = elements[current].take().unwrap();
                    let mut o = elements[other].take().unwrap();
                    if invert {
                        // current.end ---- other.end, by reversing other
                        points[cur.end_index] = None;
                        points[o.end_index] = None;
                        o.invert();
                        cur.append(o);
                        elements[current] = Some(cur);
                        current
                    } else {
                        // other.end ---- current.start
                        points[o.end_index] = None;
                        points[cur.start_index] = None;
                        o.append(cur);
                        elements[other] = Some(o);
                        other
                    }
                };
                let open = elements[merged].as_ref().unwrap();
                let (start_index, end_index) = (open.start_index, open.end_index);
                if open.element.is_closed() {
                    result.push(elements[merged].take().unwrap().element);
                    points[start_index] = None;
                    points[end_index] = None;
                } else {
                    for (i, inverted) in [(start_index, false), (end_index, true)] {
                        if let Some(p) = points[i].as_mut() {
                            p.inverted = inverted;
                            p.index = merged;
                        }
                    }
                }
                changed = true;
                break;
            }
            if !any_neighbours {
                result.push(elements[current].take().unwrap().element);
            }
        }
    }
    result.extend(elements.into_iter().flatten().map(|open| open.element));
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    const KEY: SetKey = SetKey {
        power: 50.0,
        speed: 10.0,
        focus: 0.0,
    };

    fn path(points: &[[f64; 2]]) -> Element {
        Element {
            set: 0,
            start: points[0],
            moves: points[1..].to_vec(),
        }
    }

    fn square(x: f64, y: f64, size: f64) -> Element {
        path(&[
            [x, y],
            [x + size, y],
            [x + size, y + size],
            [x, y + size],
            [x, y],
        ])
    }

    fn order(elements: Vec<Element>) -> Vec<Element> {
        inner_first(elements, &[KEY])
    }

    fn sorted(mut paths: Vec<Element>) -> Vec<Element> {
        paths.sort_by(|a, b| {
            (a.start, &a.moves)
                .partial_cmp(&(b.start, &b.moves))
                .unwrap()
        });
        paths
    }

    #[test]
    fn hole_after_outer_contour_is_cut_first() {
        let outer = square(10.0, 10.0, 100.0);
        let hole = square(50.0, 50.0, 10.0);
        assert_eq!(order(vec![outer.clone(), hole.clone()]), vec![hole, outer]);
    }

    #[test]
    fn three_nesting_levels_are_cut_inside_out() {
        let a = square(0.0, 0.0, 300.0);
        let b = square(100.0, 100.0, 100.0);
        let c = square(140.0, 140.0, 20.0);
        let d = square(500.0, 0.0, 50.0);
        let result = order(vec![a.clone(), b.clone(), d.clone(), c.clone()]);
        assert_eq!(result, vec![d, c, b, a]);
    }

    #[test]
    fn holes_touching_the_outline_still_come_first() {
        let outer = square(0.0, 0.0, 100.0);
        let bottom = square(40.0, 90.0, 10.0);
        let corner = square(90.0, 0.0, 10.0);
        let result = order(vec![outer.clone(), bottom.clone(), corner.clone()]);
        assert_eq!(result, vec![corner, bottom, outer]);
    }

    #[test]
    fn equal_properties_share_the_order_like_repeated_passes() {
        // Java: each further LaserProperty adds the shapes again; with equal
        // values both copies sort next to each other.
        let outer = square(10.0, 10.0, 100.0);
        let hole = square(50.0, 50.0, 10.0);
        let second = |e: &Element| Element {
            set: 1,
            ..e.clone()
        };
        let elements = vec![outer.clone(), hole.clone(), second(&outer), second(&hole)];
        let result = inner_first(elements, &[KEY, KEY]);
        assert_eq!(
            result,
            vec![hole.clone(), second(&hole), outer.clone(), second(&outer)]
        );
    }

    #[test]
    fn open_paths_are_joined_end_to_start_and_end_to_end() {
        let single = vec![path(&[[100.0, 0.0], [10.0, 0.0]])];
        assert_eq!(order(single.clone()), single);

        // b's end meets a's start: Java appends a to b.
        let a = path(&[[200.0, 200.0], [210.0, 200.0]]);
        let b = path(&[[220.0, 210.0], [200.0, 200.5]]);
        assert_eq!(
            order(vec![b, a]),
            vec![path(&[[220.0, 210.0], [200.0, 200.5], [210.0, 200.0]])]
        );

        // The same line twice (two passes) becomes there and back again.
        let line = path(&[[0.0, 0.0], [50.0, 0.0]]);
        assert_eq!(
            order(vec![line.clone(), line]),
            vec![path(&[[0.0, 0.0], [50.0, 0.0], [0.0, 0.0]])]
        );

        let fork = vec![
            path(&[[0.0, 50.0], [10.0, 50.0]]),
            path(&[[10.0, 50.0], [20.0, 60.0]]),
            path(&[[10.0, 50.0], [20.0, 40.0]]),
        ];
        assert_eq!(order(fork).len(), 3);
    }

    #[test]
    fn split_rectangles_are_joined_into_rings_and_ordered() {
        let lines = |x: f64, y: f64, s: f64| {
            vec![
                path(&[[x, y], [x + s, y]]),
                path(&[[x + s, y + s], [x, y + s]]),
                path(&[[x + s, y], [x + s, y + s]]),
                path(&[[x, y + s], [x, y]]),
            ]
        };
        let mut paths = lines(0.0, 0.0, 100.0);
        paths.extend(lines(40.0, 40.0, 10.0));
        let result = order(paths);
        assert_eq!(result.len(), 2);
        assert!(result.iter().all(Element::is_closed));
        assert_eq!(result[0].bounds(), [40.0, 40.0, 50.0, 50.0]);
        assert_eq!(result[1].bounds(), [0.0, 0.0, 100.0, 100.0]);
    }

    #[test]
    fn groups_follow_java_hash_map_order() {
        let a = SetKey {
            power: 80.0,
            speed: 5.0,
            focus: 3.0,
        };
        let b = SetKey {
            power: 20.0,
            speed: 50.0,
            focus: 3.0,
        };
        // Values printed by Java's LaosCutterProperty.hashCode for these settings.
        assert_eq!(a.java_hash(), -840030222);
        assert_eq!(b.java_hash(), -36296718);
        let c = SetKey {
            power: 60.0,
            speed: 12.0,
            focus: 0.0,
        };
        assert_eq!(c.java_hash(), -1447155726);
        let p = |set| Element {
            set,
            ..square(0.0, 0.0, 10.0)
        };
        let groups = java_groups(vec![p(0), p(1), p(0)], &[a, b]);
        assert_eq!(groups.iter().map(Vec::len).sum::<usize>(), 3);
        assert!(groups.iter().all(|g| g.iter().all(|e| e.set == g[0].set)));
    }

    #[test]
    fn many_paths_stay_fast() {
        let mut paths = Vec::new();
        for i in 0..100_000 {
            let (x, y) = ((i % 250) as f64 * 40.0, (i / 250) as f64 * 15.0);
            paths.push(square(x, y, 10.0));
            paths.push(path(&[[x + 20.0, y], [x + 30.0, y + 10.0]]));
        }
        let start = std::time::Instant::now();
        let result = order(paths.clone());
        assert!(start.elapsed().as_secs() < 20);
        assert_eq!(sorted(result), sorted(paths));
    }
}
