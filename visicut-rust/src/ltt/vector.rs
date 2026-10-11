// Vector output of the LTT driver: joint tangent curves with speed
// planning, the circle command and acceleration-aware time estimates.
// Port of LaserToolsTechnicsCutter.java (curveOrLine, curve,
// curveWithKnownSpeed, circle, reinterpolateWithMaximumDistance,
// cuttingTimeForPxDistance) and LibLaserCut Circle.fromPointList.
// LGPL-3.0-or-later; original author Maximilian Gaukler.
use super::{Axis, NOMINAL_CUT_SPEED, RASTER_DPI, word};
use crate::project::LIBLASERCUT_CURVE_ACCELERATION;
use crate::timeline::{MotionKind, Program};

/// Acceleration of the time estimate for straight moves: the FAU device's
/// tangentCurveMaxAcceleration as in Java (the controller ramps start/stop
/// moves with vxacc/vyacc, 0.24 g on an iLaser 4000).
const MAX_ACCELERATION: f64 = LIBLASERCUT_CURVE_ACCELERATION;
const LENGTH_TOLERANCE_MM: f64 = 0.1;
const ANGLE_TOLERANCE_SHORT: f64 = 40.0 * std::f64::consts::PI / 180.0;
const ANGLE_TOLERANCE_LONG: f64 = 10.0 * std::f64::consts::PI / 180.0;
const CIRCLE_MAX_RADIUS_MM: f64 = 101.0;

fn px_to_mm(px: f64) -> f64 {
    px * 25.4 / RASTER_DPI
}

fn mm_to_px(mm: f64) -> f64 {
    mm * RASTER_DPI / 25.4
}

fn percent_to_mm_s(percent: f64) -> f64 {
    percent / 100.0 * NOMINAL_CUT_SPEED
}

/// Time for a straight move with acceleration and braking (tangent curves on).
pub fn cutting_time_mm(distance_mm: f64, speed_percent: f64) -> f64 {
    let speed = percent_to_mm_s(speed_percent);
    if distance_mm == 0.0 {
        return 0.0;
    }
    let accel_distance = 0.5 * (speed * speed / MAX_ACCELERATION).sqrt();
    if distance_mm > 2.0 * accel_distance {
        (distance_mm + 2.0 * accel_distance) / speed
    } else {
        2.0 * distance_mm / (MAX_ACCELERATION * distance_mm).sqrt()
    }
}

#[derive(Clone, Copy)]
struct P {
    x: f64,
    y: f64,
    speed: f64,
    /// Delta to the previous point, if any.
    delta: Option<(f64, f64)>,
    angle: f64,
}

impl P {
    fn new(x: f64, y: f64) -> Self {
        Self {
            x,
            y,
            speed: f64::NAN,
            delta: None,
            angle: f64::NAN,
        }
    }
}

fn hypot(d: (f64, f64)) -> f64 {
    d.0.hypot(d.1)
}

/// Point.absAngleTo, including its (over-)approximation.
fn abs_angle(a: (f64, f64), b: (f64, f64)) -> f64 {
    let mut angle = (a.1.atan2(a.0) - b.1.atan2(b.0)).abs();
    if angle > std::f64::consts::PI {
        angle -= 2.0 * std::f64::consts::PI;
    }
    angle.abs()
}

/// Writes vector commands with the driver's state (position, laser, speed, power).
pub struct Encoder<'a> {
    pub out: &'a mut Vec<u8>,
    pub axis: Axis,
    /// Y scale for rotary jobs (acceleration is planned on scaled Y).
    pub prescale_y: f64,
    x: f64,
    y: f64,
    laser_on: bool,
    pub speed: f32,
    pub power: f32,
    pub circles: usize,
    pub curves: usize,
    program: Option<&'a mut Program>,
    kind: MotionKind,
    /// Record motions in the timeline (off for repeated passes).
    pub recording: bool,
    /// Planning limit of joint curves and circles (mm/s²).
    pub curve_acceleration: f64,
    /// Controller dwell after reaching the start of a contour (s), only for
    /// the time estimate.
    pub pierce_delay: f64,
    /// A move to a contour start happened since the laser was last switched on.
    moved: bool,
}

impl<'a> Encoder<'a> {
    pub fn new(
        out: &'a mut Vec<u8>,
        axis: Axis,
        speed: f32,
        power: f32,
        program: Option<&'a mut Program>,
        kind: MotionKind,
    ) -> Self {
        let prescale_y = axis.prescale();
        Self {
            out,
            axis,
            prescale_y,
            x: 0.0,
            y: 0.0,
            laser_on: false,
            speed,
            power,
            circles: 0,
            curves: 0,
            program,
            kind,
            recording: true,
            curve_acceleration: LIBLASERCUT_CURVE_ACCELERATION,
            pierce_delay: 0.0,
            moved: false,
        }
    }

    fn mm(&self, x: f64, y: f64) -> [f64; 2] {
        [px_to_mm(x), px_to_mm(y / self.prescale_y)]
    }

    fn record(&mut self, kind: MotionKind, x: f64, y: f64, seconds: f64) {
        let to = self.mm(x, y);
        if !self.recording {
            return;
        }
        if let Some(program) = self.program.as_deref_mut() {
            if kind == MotionKind::Travel {
                program.travel_timed(to, seconds);
            } else {
                program.line_timed(kind, to, seconds);
            }
        }
    }

    fn set_laser(&mut self, on: bool) {
        if on != self.laser_on {
            self.out.extend(if on { b"PD" } else { b"PU" });
            self.laser_on = on;
        }
        if on && self.moved {
            self.moved = false;
            if self.recording
                && let Some(program) = self.program.as_deref_mut()
            {
                program.dwell(self.pierce_delay);
            }
        }
    }

    pub fn set_speed(&mut self, speed: f32) {
        if speed != self.speed {
            self.out.extend([0x1b, 0x53]);
            word(self.out, ((speed * 10.0) as i32).clamp(1, 1000) as u16);
            self.speed = speed;
        }
    }

    pub fn set_power(&mut self, power: f32) {
        if power != self.power {
            self.out.extend([0x1b, 0x4a]);
            word(self.out, ((power * 10.0) as i32).clamp(1, 1000) as u16);
            self.power = power;
        }
    }

    fn go_to(&mut self, x: f64, y: f64, relative: bool) {
        if relative {
            self.out.extend(b"PR");
            // Truncate before subtracting so increments sum to the position.
            let dx = x as i32 - self.x as i32;
            let dy = y as i32 - self.y as i32;
            self.out.extend((dx * 8).to_be_bytes());
            self.out
                .extend(self.axis.relative_scaled(dy, self.prescale_y).to_be_bytes());
        } else {
            self.out.extend(b"PA");
            self.out.extend(((x as i32) * 8).to_be_bytes());
            self.out.extend(
                (self.axis.absolute_scaled(y as i32, self.prescale_y) as u32).to_be_bytes(),
            );
        }
        self.x = x;
        self.y = y;
    }

    /// Travel with the laser off. Returns the estimated time.
    pub fn move_to(&mut self, x: f64, y: f64) -> f64 {
        self.set_laser(false);
        let time = cutting_time_mm(px_to_mm((x - self.x).hypot(y - self.y)), 100.0);
        let to = self.mm(x, y);
        if self.recording
            && let Some(program) = self.program.as_deref_mut()
        {
            program.travel_timed(to, time);
        }
        self.go_to(x, y, false);
        self.moved = true;
        time
    }

    fn line(&mut self, x: f64, y: f64) -> f64 {
        self.set_laser(true);
        let time = cutting_time_mm(px_to_mm((x - self.x).hypot(y - self.y)), self.speed as f64);
        self.go_to(x, y, true);
        time
    }

    pub fn finish(&mut self) {
        self.set_laser(false);
    }

    /// curveOrLine: cut the polyline from the current point, split at corners,
    /// using circle commands or joint curves where possible.
    pub fn polyline(&mut self, points: &[(f64, f64)]) -> Result<f64, String> {
        let mut time = 0.0;
        if points.is_empty() {
            return Ok(0.0);
        }
        let tolerance = mm_to_px(LENGTH_TOLERANCE_MM).max(1.0);
        let mut list = vec![P::new(self.x, self.y)];
        for &(x, y) in points {
            let last = *list.last().unwrap();
            let mut point = P::new(x, y);
            let delta = (x - last.x, y - last.y);
            point.delta = Some(delta);
            let length = hypot(delta);
            if length < tolerance {
                continue;
            }
            if let Some(previous) = last.delta {
                let angle = abs_angle(previous, delta);
                list.last_mut().unwrap().angle = angle;
                if angle * length > tolerance * 5.0
                    || angle > ANGLE_TOLERANCE_SHORT
                    || (length > tolerance * 100.0 && angle > ANGLE_TOLERANCE_LONG)
                {
                    time += self.curve(std::mem::take(&mut list))?;
                    list.push(P::new(last.x, last.y));
                    // The new segment starts at the corner.
                    point.delta = Some((x - last.x, y - last.y));
                }
            }
            list.push(point);
        }
        if list.len() == 1 {
            return Ok(time);
        }
        if let Some(center) = circle(&list, tolerance)
            && let Some(t) = self.circle(center)
        {
            return Ok(time + t);
        }
        Ok(time + self.curve(list)?)
    }

    fn circle(&mut self, center: (f64, f64)) -> Option<f64> {
        if self.prescale_y != 1.0 {
            return None;
        }
        let radius_px = (center.0 - self.x).hypot(center.1 - self.y);
        let radius_mm = px_to_mm(radius_px);
        if radius_mm > CIRCLE_MAX_RADIUS_MM {
            // Larger circles accelerate violently on the real machine.
            return None;
        }
        // Constant speed on a circle: a = v² / r.
        let max_speed =
            ((self.curve_acceleration * radius_mm).sqrt() / percent_to_mm_s(1.0)) as f32 * 0.7;
        let (old_speed, old_power) = (self.speed, self.power);
        if self.speed > max_speed {
            self.set_speed(max_speed);
            self.set_power(old_power * max_speed / old_speed);
        }
        self.set_laser(true);
        self.out.extend(b"PJPB");
        self.out.extend([0; 8]);
        let dx = center.0 as i32 - self.x as i32;
        let dy = center.1 as i32 - self.y as i32;
        self.out.extend((dx * 8).to_be_bytes());
        self.out
            .extend(self.axis.relative_scaled(dy, self.prescale_y).to_be_bytes());
        self.out.extend(b"PF");
        let seconds = cutting_time_mm(
            px_to_mm(2.0 * std::f64::consts::PI * radius_px),
            self.speed as f64,
        );
        // The timeline shows the circle as a polygon.
        let (x0, y0) = (self.x, self.y);
        let start = (y0 - center.1).atan2(x0 - center.0);
        for i in 1..=36 {
            let a = start + i as f64 / 36.0 * 2.0 * std::f64::consts::PI;
            let (x, y) = (
                center.0 + radius_px * a.cos(),
                center.1 + radius_px * a.sin(),
            );
            let (x, y) = if i == 36 { (x0, y0) } else { (x, y) };
            self.record(self.kind, x, y, seconds / 36.0);
        }
        self.set_speed(old_speed);
        self.set_power(old_power);
        self.circles += 1;
        Some(seconds)
    }

    /// curve: plan speeds within the acceleration limit, then send the
    /// joint curve. Two points are sent as a plain line.
    fn curve(&mut self, points: Vec<P>) -> Result<f64, String> {
        if points.len() <= 1 {
            return Ok(0.0);
        }
        if points.len() == 2 {
            let p = points[1];
            let t = self.line(p.x, p.y);
            self.record(self.kind, p.x, p.y, t);
            return Ok(t);
        }
        let max_speed = self.speed as f64 * NOMINAL_CUT_SPEED / 100.0;
        let to_percent = 100.0 / NOMINAL_CUT_SPEED;
        let px_mm = px_to_mm(1.0);
        let safety = 0.99;
        let mut points = reinterpolate(points, mm_to_px(0.9));
        for p in &mut points {
            p.speed = max_speed;
        }
        let last = points.len() - 1;
        points[0].speed = 0.0;
        points[last].speed = 0.0;
        points[0].angle = 0.0;
        const WARMUP_ROUNDS: i32 = 10;
        const WARMUP_FACTOR_MAX: f64 = 16.0;
        let mut warmup = WARMUP_ROUNDS;
        let mut changed = true;
        let mut iterations = 0;
        while changed || warmup > 0 {
            iterations += 1;
            if iterations > 100_000 {
                return Err("Kurvenplanung konvergiert nicht".into());
            }
            if warmup > 0 {
                warmup -= 1;
            }
            let optimism = if warmup > 0 {
                WARMUP_FACTOR_MAX.powf(warmup as f64 / WARMUP_ROUNDS as f64)
            } else {
                1.0
            };
            changed = false;
            let mut i = 1;
            while i < points.len() {
                let before = points[i - 1];
                let p = points[i];
                let max_avg = (p.speed + before.speed) / 2.0;
                let distance = hypot(p.delta.unwrap()) * px_mm;
                let mut min_time = distance / max_avg;
                const MAX_ACCEL_TIME: f64 = 0.005;
                if before.angle != 0.0 && min_time > MAX_ACCEL_TIME {
                    min_time = MAX_ACCEL_TIME;
                }
                let alpha = before.angle;
                let c = self.curve_acceleration * optimism * min_time;
                if before.speed * alpha.sin() > c {
                    points[i - 1].speed = safety * c / alpha;
                    changed = true;
                }
                let a = points[i - 1].speed;
                let (sin, cos) = alpha.sin_cos();
                let root = (-a * a * sin * sin + c * c).sqrt();
                let mut max_for_accel = a * cos + root;
                let min_for_accel = a * cos - root;
                if min_for_accel.is_nan() || max_for_accel.is_nan() {
                    return Err("Kurvenplanung: kein gültiger Geschwindigkeitsbereich".into());
                }
                if max_for_accel > max_speed {
                    if min_for_accel < max_speed {
                        max_for_accel = max_speed;
                    } else {
                        return Err("Kurvenplanung: Geschwindigkeitsbereich ungültig".into());
                    }
                }
                if p.speed > max_for_accel {
                    points[i].speed =
                        max_for_accel * safety + min_for_accel.max(0.0) * (1.0 - safety);
                    changed = true;
                } else if p.speed < min_for_accel {
                    let b = p.speed;
                    let root = (-b * b * sin * sin + c * c).sqrt();
                    let new_max = b * cos + root;
                    let new_min = (b * cos - root).max(0.0);
                    let new_a = new_max * safety + new_min * (1.0 - safety);
                    if new_a.is_nan() {
                        return Err("Kurvenplanung: Bremsgeschwindigkeit ungültig".into());
                    }
                    points[i - 1].speed = new_a;
                    changed = true;
                    if i >= 2 && warmup <= 0 {
                        i -= 2;
                    }
                }
                i += 1;
            }
        }
        // Drop points on straight segments with little speed change.
        let mut filtered = vec![points[0]];
        let mut speed_now = points[0].speed;
        let mut since_last = 0.0;
        for p in &points[1..last] {
            let threshold = 0.2 + (0.1 * speed_now * (1.0 - since_last / 5.0)).max(0.0);
            if p.angle == 0.0 && (p.speed - speed_now).abs() < threshold {
                since_last += hypot(p.delta.unwrap()) * px_mm;
            } else {
                speed_now = p.speed;
                since_last = 0.0;
                filtered.push(*p);
            }
        }
        filtered.push(points[last]);
        for p in &mut filtered {
            p.speed *= to_percent;
        }
        self.curve_with_known_speed(&filtered)
    }

    fn curve_with_known_speed(&mut self, points: &[P]) -> Result<f64, String> {
        self.set_laser(true);
        self.out.extend(b"PJ");
        let (mut vx, mut vy, mut v) = (0.0, 0.0, 0.0);
        let mut total = 0.0;
        for i in 1..points.len() {
            let (x, y) = (points[i].x, points[i].y);
            let speed = points[i].speed;
            let before = points[i - 1].speed;
            if self.x == x && self.y == y {
                return Err("Kurve enthält einen doppelten Punkt".into());
            }
            let dx = px_to_mm(x - self.x);
            let dy = px_to_mm(y - self.y);
            let length = dx.hypot(dy);
            let new_vx = percent_to_mm_s(speed) * dx / length;
            let new_vy = percent_to_mm_s(speed) * dy / length;
            let new_v = new_vx.hypot(new_vy);
            let time = length / ((v + new_v) / 2.0);
            total += time;
            let ax = (new_vx - vx).abs() / time;
            let ay = (new_vy - vy).abs() / time;
            let tolerance = 1.00001;
            let limit = self.curve_acceleration * tolerance;
            if ax.is_nan() || ax > limit || ay.is_nan() || ay > limit || ax.hypot(ay) > limit {
                return Err(format!("Beschleunigung im Kurvensegment {i} zu hoch"));
            }
            (vx, vy, v) = (new_vx, new_vy, new_v);
            self.out.extend(b"PE");
            // Target speed in the middle of the segment; arc compensation on.
            let sent = (before + speed) / 2.0;
            word(
                self.out,
                ((sent * 10.0).round() as i32).clamp(1, 1000) as u16,
            );
            self.line(x, y);
            self.record(self.kind, x, y, time);
        }
        self.out.extend(b"PF");
        self.curves += 1;
        Ok(total)
    }
}

fn reinterpolate(points: Vec<P>, max_distance: f64) -> Vec<P> {
    let mut result = vec![points[0]];
    let mut previous = points[0];
    for mut point in points.into_iter().skip(1) {
        let delta = point.delta.unwrap();
        let length = hypot(delta);
        if length == 0.0 {
            continue;
        }
        if length >= max_distance {
            let extra = (length / max_distance).ceil() as usize - 1;
            for i in 0..extra {
                let f = (extra - i) as f64 / (1 + extra) as f64;
                let mut p = P::new(point.x - delta.0 * f, point.y - delta.1 * f);
                p.angle = 0.0;
                p.delta = Some((p.x - previous.x, p.y - previous.y));
                result.push(p);
                previous = p;
            }
            point.delta = Some((point.x - previous.x, point.y - previous.y));
        }
        result.push(point);
        previous = point;
    }
    result
}

/// Circle.fromPointList: the closed polyline is a circle within `tolerance`.
fn circle(points: &[P], tolerance: f64) -> Option<(f64, f64)> {
    if points.len() < 8 {
        return None;
    }
    let (start, end) = (points[0], points[points.len() - 1]);
    if start.x != end.x || start.y != end.y {
        return None;
    }
    let (mut cx, mut cy, mut length, mut max_segment) = (0.0, 0.0, 0.0, 0.0f64);
    let mut before = points[0];
    for p in points {
        let segment = (p.x - before.x).hypot(p.y - before.y);
        cx += (p.x + before.x) / 2.0 * segment;
        cy += (p.y + before.y) / 2.0 * segment;
        max_segment = max_segment.max(segment);
        length += segment;
        before = *p;
    }
    cx /= length;
    cy /= length;
    let (mut min_r2, mut max_r2, mut sum_r2) = (f64::INFINITY, f64::NEG_INFINITY, 0.0);
    for p in points {
        let r2 = (cx - p.x).powi(2) + (cy - p.y).powi(2);
        sum_r2 += r2;
        let changed = r2 < min_r2 || r2 > max_r2;
        min_r2 = min_r2.min(r2);
        max_r2 = max_r2.max(r2);
        if changed {
            if (max_r2 - min_r2).powi(2) > 4.0 * tolerance * tolerance * min_r2 {
                return None;
            }
            if min_r2 <= 45.0 * tolerance {
                return None;
            }
        }
    }
    let radius = (sum_r2 / points.len() as f64).sqrt();
    if max_segment * max_segment > 8.0 * radius * tolerance {
        return None;
    }
    if (2.0 * std::f64::consts::PI * radius - length).abs() > 0.05 * length {
        return None;
    }
    Some((cx, cy))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn encode(points: &[(f64, f64)], speed: f32) -> (Vec<u8>, f64, usize, usize) {
        let mut out = Vec::new();
        let mut program = Program::new(crate::project::Operation::Cut);
        let mut e = Encoder::new(
            &mut out,
            Axis::xy(),
            speed,
            50.0,
            Some(&mut program),
            MotionKind::Cut,
        );
        e.move_to(points[0].0, points[0].1);
        let t = e.polyline(&points[1..]).unwrap();
        e.finish();
        let (c, k) = (e.circles, e.curves);
        assert!(program.duration_seconds > 0.0);
        (out, t, c, k)
    }

    fn has(bytes: &[u8], pattern: &[u8]) -> bool {
        bytes.windows(pattern.len()).any(|w| w == pattern)
    }

    #[test]
    fn corners_are_plain_lines_and_time_includes_acceleration() {
        let square = [
            (100.0, 100.0),
            (600.0, 100.0),
            (600.0, 600.0),
            (100.0, 600.0),
            (100.0, 100.0),
        ];
        let (bytes, time, circles, curves) = encode(&square, 50.0);
        assert_eq!((circles, curves), (0, 0));
        assert!(!has(&bytes, b"PJ"));
        assert_eq!(bytes.windows(2).filter(|w| *w == b"PR").count(), 4);
        // 4 × 25.4 mm at 169.3 mm/s plus acceleration distance.
        let plain = 4.0 * 25.4 / (0.5 * NOMINAL_CUT_SPEED);
        assert!(time > plain && time < plain * 1.5, "{time}");
    }

    #[test]
    fn circles_use_the_circle_command_with_reduced_speed_and_restore_it() {
        let r = 200.0; // 10.16 mm radius
        let ring: Vec<_> = (0..=64)
            .map(|i| {
                let a = i as f64 / 64.0 * 2.0 * std::f64::consts::PI;
                if i == 64 {
                    (300.0 + r, 300.0)
                } else {
                    (300.0 + r * a.cos(), 300.0 + r * a.sin())
                }
            })
            .collect();
        let (bytes, _, circles, _) = encode(&ring, 100.0);
        assert_eq!(circles, 1);
        assert!(has(&bytes, b"PJPB\0\0\0\0\0\0\0\0"));
        // Max. speed sqrt(2000 · 10.16) / 3.387 · 0.7 = 29.46 % → 294, then back to 100 %.
        assert!(has(&bytes, &[0x1b, 0x53, 0x01, 0x26]));
        assert!(has(&bytes, &[0x1b, 0x53, 0x03, 0xe8]));
        // Centre relative to the start point: about (−200, 0) px, in 1/8 px.
        let at = bytes.windows(4).position(|w| w == b"PJPB").unwrap() + 12;
        let dx = i32::from_be_bytes(bytes[at..at + 4].try_into().unwrap());
        let dy = i32::from_be_bytes(bytes[at + 4..at + 8].try_into().unwrap());
        assert!((dx + 1600).abs() <= 8 && dy.abs() <= 8, "{dx} {dy}");
        assert_eq!(&bytes[at + 8..at + 10], b"PF");
    }

    #[test]
    fn gentle_curves_become_joint_curves_within_acceleration_limits() {
        let arc: Vec<_> = (0..=40)
            .map(|i| {
                let a = i as f64 / 40.0 * std::f64::consts::FRAC_PI_2;
                (100.0 + 2000.0 * a.sin(), 100.0 + 2000.0 * (1.0 - a.cos()))
            })
            .collect();
        let (bytes, time, circles, curves) = encode(&arc, 100.0);
        assert_eq!((circles, curves), (0, 1));
        assert!(has(&bytes, b"PJPE") && has(&bytes, b"PF"));
        let length = px_to_mm(2000.0 * std::f64::consts::FRAC_PI_2);
        assert!(time > length / NOMINAL_CUT_SPEED, "{time}");
    }

    #[test]
    fn detects_only_real_circles() {
        let hexagon: Vec<P> = (0..=6)
            .map(|i| {
                let a = i as f64 / 6.0 * 2.0 * std::f64::consts::PI;
                P::new(
                    (300.0 + 200.0 * a.cos()).round(),
                    (300.0 + 200.0 * a.sin()).round(),
                )
            })
            .collect();
        assert!(circle(&hexagon, 2.0).is_none());
    }
}
