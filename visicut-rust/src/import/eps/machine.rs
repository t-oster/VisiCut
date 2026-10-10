//! Operand stack, dictionary stack, graphics state and operators of the
//! built-in interpreter. Operators outside the supported subset fail with a
//! German message that names them; the caller then hands the file to Ghostscript.
use super::graphics::{self, IDENTITY, Matrix, Path, arc_curves, multiply, num, point, vector};
use super::parse::Obj;
use crate::svg_import::MAX_SVG_BYTES;
use std::collections::HashMap;
use std::fmt::Write;
use std::rc::Rc;

/// Operations allowed per file; stops endless loops.
const MAX_STEPS: u64 = 2_000_000;
const MAX_OPERANDS: usize = 65_536;
/// Deepest nesting of procedure calls.
const MAX_DEPTH: usize = 128;
/// The thinnest line ("hairline") is 0.1 mm, as in the PDF import.
const HAIRLINE_PT: f64 = 0.1 * 72.0 / 25.4;

/// Part of the graphics state that `gsave` and `grestore` save and restore.
#[derive(Clone)]
struct GState {
    ctm: Matrix,
    rgb: [f64; 3],
    line_width: f64,
}

pub(super) struct Machine {
    operands: Vec<Obj>,
    /// Index 0 is the user dictionary; `def` writes to the top one.
    dicts: Vec<HashMap<String, Obj>>,
    state: GState,
    saved: Vec<GState>,
    path: Path,
    /// SVG elements for everything painted so far.
    out: String,
    steps: u64,
    depth: usize,
}

impl Machine {
    /// `ctm` maps user space to SVG device space in points.
    pub(super) fn new(ctm: Matrix) -> Self {
        Self {
            operands: Vec::new(),
            dicts: vec![HashMap::new()],
            state: GState {
                ctm,
                rgb: [0.0; 3],
                line_width: 1.0,
            },
            saved: Vec::new(),
            path: Path::default(),
            out: String::new(),
            steps: 0,
            depth: 0,
        }
    }

    pub(super) fn run(&mut self, program: &[Obj]) -> Result<(), String> {
        for item in program {
            self.exec(item)?;
        }
        Ok(())
    }

    /// The SVG elements of everything painted.
    pub(super) fn into_elements(self) -> String {
        self.out
    }

    fn tick(&mut self) -> Result<(), String> {
        self.steps += 1;
        if self.steps > MAX_STEPS {
            return Err("Die EPS-Datei braucht zu viele Operationen".into());
        }
        if self.operands.len() > MAX_OPERANDS {
            return Err("Stapelüberlauf im eingebauten PostScript-Interpreter".into());
        }
        if self.out.len() + self.path.bytes() > MAX_SVG_BYTES {
            return Err("EPS-Datei ist zu komplex (SVG größer als 20 MB)".into());
        }
        Ok(())
    }

    fn exec(&mut self, obj: &Obj) -> Result<(), String> {
        self.tick()?;
        match obj {
            Obj::Name {
                name,
                executable: true,
            } => self.exec_name(name),
            other => {
                self.operands.push(other.clone());
                Ok(())
            }
        }
    }

    /// Runs the value bound to `name` in the dictionaries, or else the operator.
    fn exec_name(&mut self, name: &str) -> Result<(), String> {
        if let Some(value) = self.lookup(name).cloned() {
            return match value {
                Obj::Proc(body) => self.call_proc(&body),
                other => {
                    self.operands.push(other);
                    Ok(())
                }
            };
        }
        self.operator(name)
    }

    fn call_proc(&mut self, body: &[Obj]) -> Result<(), String> {
        self.tick()?;
        if self.depth >= MAX_DEPTH {
            return Err("Prozeduren sind zu tief ineinander verschachtelt".into());
        }
        self.depth += 1;
        for item in body {
            self.exec(item)?;
        }
        self.depth -= 1;
        Ok(())
    }

    fn lookup(&self, name: &str) -> Option<&Obj> {
        self.dicts.iter().rev().find_map(|dict| dict.get(name))
    }

    fn pop_any(&mut self, op: &str) -> Result<Obj, String> {
        self.operands
            .pop()
            .ok_or_else(|| format!("Stapelunterlauf bei „{op}“"))
    }

    fn pop_num(&mut self, op: &str) -> Result<f64, String> {
        match self.pop_any(op)? {
            Obj::Num(value) if value.is_finite() => Ok(value),
            Obj::Num(_) => Err(format!("Ungültiger Zahlenwert bei „{op}“")),
            _ => Err(format!("Falscher Operandtyp bei „{op}“ (Zahl erwartet)")),
        }
    }

    /// Pops `N` numbers and returns them in the order they were pushed.
    fn pop_nums<const N: usize>(&mut self, op: &str) -> Result<[f64; N], String> {
        let mut values = [0.0; N];
        for slot in values.iter_mut().rev() {
            *slot = self.pop_num(op)?;
        }
        Ok(values)
    }

    /// A count or index: a non-negative integer.
    fn pop_index(&mut self, op: &str) -> Result<usize, String> {
        let value = self.pop_num(op)?;
        if value.fract() != 0.0 || value < 0.0 {
            return Err(format!(
                "Bereichsfehler bei „{op}“ (ganze Zahl ≥ 0 erwartet)"
            ));
        }
        Ok(value as usize)
    }

    fn pop_integer(&mut self, op: &str) -> Result<i64, String> {
        let value = self.pop_num(op)?;
        if value.fract() != 0.0 {
            return Err(format!("Bereichsfehler bei „{op}“ (ganze Zahl erwartet)"));
        }
        Ok(value as i64)
    }

    fn pop_bool(&mut self, op: &str) -> Result<bool, String> {
        match self.pop_any(op)? {
            Obj::Bool(value) => Ok(value),
            _ => Err(format!("Falscher Operandtyp bei „{op}“ (Bool erwartet)")),
        }
    }

    fn pop_proc(&mut self, op: &str) -> Result<Rc<[Obj]>, String> {
        match self.pop_any(op)? {
            Obj::Proc(body) => Ok(body),
            _ => Err(format!(
                "Falscher Operandtyp bei „{op}“ (Prozedur erwartet)"
            )),
        }
    }

    fn pop_matrix(&mut self, op: &str) -> Result<Matrix, String> {
        match self.pop_any(op)? {
            Obj::Matrix(matrix) => Ok(matrix),
            _ => Err(format!("Falscher Operandtyp bei „{op}“ (Matrix erwartet)")),
        }
    }

    /// The name of a key for `def` or `load`.
    fn pop_key(&mut self, op: &str) -> Result<String, String> {
        match self.pop_any(op)? {
            Obj::Name { name, .. } => Ok(name.to_string()),
            _ => Err(format!("Falscher Operandtyp bei „{op}“ (Name erwartet)")),
        }
    }

    /// A point in device space; rejects values that overflow to infinity.
    fn device_point(&self, x: f64, y: f64, op: &str) -> Result<(f64, f64), String> {
        let p = point(self.state.ctm, x, y);
        if p.0.is_finite() && p.1.is_finite() {
            Ok(p)
        } else {
            Err(format!("Ungültige Koordinate bei „{op}“"))
        }
    }

    fn current_point(&self, op: &str) -> Result<(f64, f64), String> {
        self.path
            .current()
            .ok_or_else(|| format!("Kein aktueller Punkt bei „{op}“"))
    }

    /// `base` moved by a displacement given in user space.
    fn displaced(
        &self,
        base: (f64, f64),
        dx: f64,
        dy: f64,
        op: &str,
    ) -> Result<(f64, f64), String> {
        let (vx, vy) = vector(self.state.ctm, dx, dy);
        let p = (base.0 + vx, base.1 + vy);
        if p.0.is_finite() && p.1.is_finite() {
            Ok(p)
        } else {
            Err(format!("Ungültige Koordinate bei „{op}“"))
        }
    }

    /// The current point moved by a displacement in user space.
    fn relative_point(&self, dx: f64, dy: f64, op: &str) -> Result<(f64, f64), String> {
        let base = self.current_point(op)?;
        self.displaced(base, dx, dy, op)
    }

    fn fill(&mut self, even_odd: bool) {
        if self.path.drawn() {
            let rule = if even_odd {
                " fill-rule=\"evenodd\""
            } else {
                ""
            };
            let _ = writeln!(
                self.out,
                "<path d=\"{}\" fill=\"{}\" stroke=\"none\"{rule}/>",
                self.path.data(),
                colour(self.state.rgb)
            );
        }
        self.path.clear();
    }

    fn stroke(&mut self) {
        if self.path.drawn() {
            // Line widths are in user space and scale with the CTM.
            let width = if self.state.line_width > 0.0 {
                self.state.line_width * graphics::scale_of(self.state.ctm)
            } else {
                HAIRLINE_PT
            };
            let _ = writeln!(
                self.out,
                "<path d=\"{}\" fill=\"none\" stroke=\"{}\" stroke-width=\"{}\"/>",
                self.path.data(),
                colour(self.state.rgb),
                num(width)
            );
        }
        self.path.clear();
    }

    /// `x y r angle1 angle2 arc` (or `arcn`, clockwise).
    fn arc(&mut self, op: &str) -> Result<(), String> {
        let [x, y, r, start, end] = self.pop_nums::<5>(op)?;
        if r < 0.0 {
            return Err(format!("Bereichsfehler bei „{op}“ (negativer Radius)"));
        }
        let (s, c) = start.to_radians().sin_cos();
        let first = self.device_point(x + r * c, y + r * s, op)?;
        // As in PostScript, an existing current point is joined to the arc.
        if self.path.current().is_some() {
            self.path.line_to(first);
        } else {
            self.path.move_to(first);
        }
        for [c1, c2, end_point] in arc_curves(x, y, r, start, end, op == "arcn") {
            let c1 = self.device_point(c1.0, c1.1, op)?;
            let c2 = self.device_point(c2.0, c2.1, op)?;
            let end_point = self.device_point(end_point.0, end_point.1, op)?;
            self.path.curve_to(c1, c2, end_point);
        }
        Ok(())
    }

    /// Runs one operator. Anything not listed here is unsupported.
    fn operator(&mut self, name: &str) -> Result<(), String> {
        match name {
            // Operand stack
            "pop" => {
                self.pop_any(name)?;
            }
            "exch" => {
                let top = self.pop_any(name)?;
                let below = self.pop_any(name)?;
                self.operands.push(top);
                self.operands.push(below);
            }
            "dup" => {
                let top = self
                    .operands
                    .last()
                    .cloned()
                    .ok_or_else(|| format!("Stapelunterlauf bei „{name}“"))?;
                self.operands.push(top);
            }
            "copy" => {
                let n = self.pop_index(name)?;
                let len = self.operands.len();
                if n > len {
                    return Err(format!("Stapelunterlauf bei „{name}“"));
                }
                let copies: Vec<Obj> = self.operands[len - n..].to_vec();
                self.operands.extend(copies);
            }
            "index" => {
                let n = self.pop_index(name)?;
                let len = self.operands.len();
                if n >= len {
                    return Err(format!("Stapelunterlauf bei „{name}“"));
                }
                let copy = self.operands[len - 1 - n].clone();
                self.operands.push(copy);
            }
            "roll" => {
                let shift = self.pop_integer(name)?;
                let n = self.pop_index(name)?;
                let len = self.operands.len();
                if n > len {
                    return Err(format!("Stapelunterlauf bei „{name}“"));
                }
                if n > 0 {
                    let k = shift.rem_euclid(n as i64) as usize;
                    self.operands[len - n..].rotate_right(k);
                }
            }

            // Dictionaries
            "def" => {
                let value = self.pop_any(name)?;
                let key = self.pop_key(name)?;
                if let Some(dict) = self.dicts.last_mut() {
                    dict.insert(key, value);
                }
            }
            "load" => {
                let key = self.pop_key(name)?;
                let value = self
                    .lookup(&key)
                    .cloned()
                    .ok_or_else(|| format!("„{key}“ ist nicht definiert (bei „load“)"))?;
                self.operands.push(value);
            }

            // Arithmetic
            "add" | "sub" | "mul" | "div" => {
                let b = self.pop_num(name)?;
                let a = self.pop_num(name)?;
                let result = match name {
                    "add" => a + b,
                    "sub" => a - b,
                    "mul" => a * b,
                    _ => {
                        if b == 0.0 {
                            return Err(
                                "Ergebnis nicht definiert bei „div“ (Division durch 0)".into()
                            );
                        }
                        a / b
                    }
                };
                self.push_num(result, name)?;
            }
            "neg" => {
                let a = self.pop_num(name)?;
                self.push_num(-a, name)?;
            }
            "abs" => {
                let a = self.pop_num(name)?;
                self.push_num(a.abs(), name)?;
            }
            "sqrt" => {
                let a = self.pop_num(name)?;
                if a < 0.0 {
                    return Err("Bereichsfehler bei „sqrt“ (negativer Wert)".into());
                }
                self.push_num(a.sqrt(), name)?;
            }
            "sin" => {
                let degrees = self.pop_num(name)?;
                self.push_num(degrees.to_radians().sin(), name)?;
            }
            "cos" => {
                let degrees = self.pop_num(name)?;
                self.push_num(degrees.to_radians().cos(), name)?;
            }

            // Comparison and booleans, for `if` and `ifelse`
            "eq" | "ne" => {
                let b = self.pop_any(name)?;
                let a = self.pop_any(name)?;
                let equal = objects_equal(&a, &b);
                self.operands.push(Obj::Bool(equal == (name == "eq")));
            }
            "lt" | "le" | "gt" | "ge" => {
                let b = self.pop_num(name)?;
                let a = self.pop_num(name)?;
                let result = match name {
                    "lt" => a < b,
                    "le" => a <= b,
                    "gt" => a > b,
                    _ => a >= b,
                };
                self.operands.push(Obj::Bool(result));
            }
            "not" => {
                let value = self.pop_bool(name)?;
                self.operands.push(Obj::Bool(!value));
            }
            "true" => self.operands.push(Obj::Bool(true)),
            "false" => self.operands.push(Obj::Bool(false)),

            // Control
            "if" => {
                let body = self.pop_proc(name)?;
                if self.pop_bool(name)? {
                    self.call_proc(&body)?;
                }
            }
            "ifelse" => {
                let otherwise = self.pop_proc(name)?;
                let then = self.pop_proc(name)?;
                if self.pop_bool(name)? {
                    self.call_proc(&then)?;
                } else {
                    self.call_proc(&otherwise)?;
                }
            }
            "repeat" => {
                let body = self.pop_proc(name)?;
                let count = self.pop_index(name)?;
                for _ in 0..count {
                    self.call_proc(&body)?;
                }
            }
            "for" => {
                let body = self.pop_proc(name)?;
                let limit = self.pop_num(name)?;
                let step = self.pop_num(name)?;
                let start = self.pop_num(name)?;
                if step == 0.0 {
                    return Err("Schrittweite 0 bei „for“".into());
                }
                let mut value = start;
                while (step > 0.0 && value <= limit) || (step < 0.0 && value >= limit) {
                    self.push_num(value, name)?;
                    self.call_proc(&body)?;
                    value += step;
                }
            }
            "exec" => match self.pop_any(name)? {
                Obj::Proc(body) => self.call_proc(&body)?,
                Obj::Name {
                    name: inner,
                    executable: true,
                } => self.exec_name(&inner)?,
                other => self.operands.push(other),
            },
            // Pages end here; further pages are not imported.
            "showpage" => {}
            // Procedures are not bound to their operators; nothing to do.
            "bind" => {}

            // Path construction
            "newpath" => self.path.clear(),
            "moveto" => {
                let [x, y] = self.pop_nums::<2>(name)?;
                let p = self.device_point(x, y, name)?;
                self.path.move_to(p);
            }
            "rmoveto" => {
                let [dx, dy] = self.pop_nums::<2>(name)?;
                let p = self.relative_point(dx, dy, name)?;
                self.path.move_to(p);
            }
            "lineto" => {
                let [x, y] = self.pop_nums::<2>(name)?;
                self.current_point(name)?;
                let p = self.device_point(x, y, name)?;
                self.path.line_to(p);
            }
            "rlineto" => {
                let [dx, dy] = self.pop_nums::<2>(name)?;
                self.current_point(name)?;
                let p = self.relative_point(dx, dy, name)?;
                self.path.line_to(p);
            }
            "curveto" => {
                let [x1, y1, x2, y2, x3, y3] = self.pop_nums::<6>(name)?;
                self.current_point(name)?;
                let c1 = self.device_point(x1, y1, name)?;
                let c2 = self.device_point(x2, y2, name)?;
                let end = self.device_point(x3, y3, name)?;
                self.path.curve_to(c1, c2, end);
            }
            "rcurveto" => {
                // Each displacement is relative to the point before it.
                let [dx1, dy1, dx2, dy2, dx3, dy3] = self.pop_nums::<6>(name)?;
                let start = self.current_point(name)?;
                let c1 = self.displaced(start, dx1, dy1, name)?;
                let c2 = self.displaced(c1, dx2, dy2, name)?;
                let end = self.displaced(c2, dx3, dy3, name)?;
                self.path.curve_to(c1, c2, end);
            }
            "closepath" => self.path.close(),
            "arc" | "arcn" => self.arc(name)?,
            "rect" => {
                let [x, y, w, h] = self.pop_nums::<4>(name)?;
                let corners = [
                    self.device_point(x, y, name)?,
                    self.device_point(x + w, y, name)?,
                    self.device_point(x + w, y + h, name)?,
                    self.device_point(x, y + h, name)?,
                ];
                self.path.move_to(corners[0]);
                for corner in &corners[1..] {
                    self.path.line_to(*corner);
                }
                self.path.close();
            }

            // Painting: the path is consumed
            "fill" => self.fill(false),
            "eofill" => self.fill(true),
            "stroke" => self.stroke(),

            // Colour, converted to RGB
            "setrgbcolor" => {
                let [r, g, b] = self.pop_nums::<3>(name)?;
                self.state.rgb = [r, g, b];
            }
            "setgray" => {
                let gray = self.pop_num(name)?;
                self.state.rgb = [gray; 3];
            }
            "setcmykcolor" => {
                let [c, m, y, k] = self.pop_nums::<4>(name)?;
                self.state.rgb = [
                    (1.0 - c) * (1.0 - k),
                    (1.0 - m) * (1.0 - k),
                    (1.0 - y) * (1.0 - k),
                ];
            }
            "setlinewidth" => {
                self.state.line_width = self.pop_num(name)?.abs();
            }

            // Graphics state and transformations
            "gsave" => self.saved.push(self.state.clone()),
            "grestore" => {
                if let Some(state) = self.saved.pop() {
                    self.state = state;
                }
            }
            "translate" => {
                let [tx, ty] = self.pop_nums::<2>(name)?;
                self.concat([1.0, 0.0, 0.0, 1.0, tx, ty]);
            }
            "scale" => {
                let [sx, sy] = self.pop_nums::<2>(name)?;
                self.concat([sx, 0.0, 0.0, sy, 0.0, 0.0]);
            }
            "rotate" => {
                let (s, c) = self.pop_num(name)?.to_radians().sin_cos();
                self.concat([c, s, -s, c, 0.0, 0.0]);
            }
            "concat" => {
                let matrix = self.pop_matrix(name)?;
                self.concat(matrix);
            }
            "matrix" => self.operands.push(Obj::Matrix(IDENTITY)),
            "setmatrix" => {
                self.state.ctm = self.pop_matrix(name)?;
            }

            _ => return Err(unsupported(name)),
        }
        Ok(())
    }

    fn concat(&mut self, matrix: Matrix) {
        self.state.ctm = multiply(matrix, self.state.ctm);
    }

    fn push_num(&mut self, value: f64, op: &str) -> Result<(), String> {
        if !value.is_finite() {
            return Err(format!("Ungültiger Zahlenwert bei „{op}“"));
        }
        self.operands.push(Obj::Num(value));
        Ok(())
    }
}

/// The message for an operator that the built-in interpreter does not support.
fn unsupported(name: &str) -> String {
    let kind = match name {
        "show" | "ashow" | "widthshow" | "awidthshow" | "xshow" | "yshow" | "xyshow" | "cshow"
        | "kshow" | "stringwidth" | "charpath" | "glyphshow" => "Text-Operator",
        "findfont" | "setfont" | "scalefont" | "makefont" | "selectfont" | "currentfont"
        | "definefont" | "findencoding" => "Schrift-Operator",
        "image" | "imagemask" | "colorimage" | "readimage" => "Bild-Operator",
        _ => return format!("Unbekannter oder nicht unterstützter Operator „{name}“"),
    };
    format!("{kind} „{name}“ wird nicht unterstützt")
}

fn objects_equal(a: &Obj, b: &Obj) -> bool {
    match (a, b) {
        (Obj::Num(x), Obj::Num(y)) => x == y,
        (Obj::Bool(x), Obj::Bool(y)) => x == y,
        (Obj::Name { name: x, .. }, Obj::Name { name: y, .. }) => x == y,
        (Obj::Str(x), Obj::Str(y)) => x == y,
        (Obj::Matrix(x), Obj::Matrix(y)) => x == y,
        _ => false,
    }
}

/// `#rrggbb` for colour components in 0..1.
fn colour(rgb: [f64; 3]) -> String {
    let channel = |v: f64| (v.clamp(0.0, 1.0) * 255.0).round() as u8;
    format!(
        "#{:02x}{:02x}{:02x}",
        channel(rgb[0]),
        channel(rgb[1]),
        channel(rgb[2])
    )
}
