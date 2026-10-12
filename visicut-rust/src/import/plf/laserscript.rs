//! LaserScript (`.ls`): JavaScript that draws with `move(x, y)` and
//! `line(x, y)` in millimetres, like VisiCut's `LaserScriptImporter` and
//! LibLaserCut's `LaserScriptBootstrap.js`.
use super::script::{self, Engine};
use crate::{i18n::tr, tr};
use boa_engine::{
    Context, JsArgs, JsNativeError, JsResult, JsString, JsValue, NativeFunction, js_string,
};
use std::cell::RefCell;
use std::time::Duration;

/// Largest number of points a script may draw (as for cutting contours).
const MAX_POINTS: usize = 1_000_000;
const MAX_MESSAGES: usize = 20;

/// Same helpers as LibLaserCut's bootstrap; `set`/`get` keep values only for
/// the script itself, as in VisiCut's importer.
const BOOTSTRAP: &str = r#"
var __settings = {};
function set(property, value) { __settings[property] = value; }
function get(property) { var v = __settings[property]; return v === undefined ? null : v; }
function promptFloat(title, defaultValue) {
  var result = parseFloat(prompt(title, defaultValue.toString()));
  return isNaN(result) ? defaultValue : result;
}
"#;

#[derive(Debug)]
pub struct Drawing {
    pub path: String,
    pub bounds: Option<[f64; 4]>,
    pub lines: usize,
    pub messages: Vec<String>,
}

#[derive(Default)]
struct State {
    path: String,
    bounds: Option<[f64; 4]>,
    started: bool,
    points: usize,
    max_points: usize,
    lines: usize,
    overflow: bool,
    messages: Vec<String>,
    dropped_messages: usize,
}

thread_local! {
    // Each script runs on its own worker thread, so this state is per run.
    static STATE: RefCell<State> = RefCell::new(State::default());
}

pub fn run(source: String, limit: Duration) -> Result<Drawing, String> {
    run_with(source, limit, MAX_POINTS)
}

fn run_with(source: String, limit: Duration, max_points: usize) -> Result<Drawing, String> {
    script::run_isolated(limit, move |engine| execute(engine, &source, max_points))
}

fn execute(engine: &mut Engine, source: &str, max_points: usize) -> Result<Drawing, String> {
    STATE.with(|s| s.borrow_mut().max_points = max_points);
    register(&mut engine.context, "move", 2, js_move)?;
    register(&mut engine.context, "line", 2, js_line)?;
    register(&mut engine.context, "echo", 1, js_echo)?;
    register(&mut engine.context, "prompt", 2, js_prompt)?;
    engine.eval(BOOTSTRAP)?;
    let result = engine.eval(source);
    let state = STATE.with(|s| std::mem::take(&mut *s.borrow_mut()));
    if state.overflow {
        return Err(tr!(
            "LaserScript zeichnet mehr als {count} Punkte; Import abgebrochen",
            count = max_points
        ));
    }
    result.map_err(|e| format!("LaserScript: {e}"))?;
    let mut messages = state.messages;
    if state.dropped_messages > 0 {
        messages.push(tr!(
            "LaserScript: {count} weitere Ausgaben ausgelassen",
            count = state.dropped_messages
        ));
    }
    Ok(Drawing {
        path: state.path,
        bounds: state.bounds,
        lines: state.lines,
        messages,
    })
}

fn register(
    context: &mut Context,
    name: &str,
    length: usize,
    function: fn(&JsValue, &[JsValue], &mut Context) -> JsResult<JsValue>,
) -> Result<(), String> {
    context
        .register_global_builtin_callable(
            JsString::from(name),
            length,
            NativeFunction::from_fn_ptr(function),
        )
        .map_err(|e| script::describe(&e))
}

fn coordinates(name: &str, args: &[JsValue], context: &mut Context) -> JsResult<(f64, f64)> {
    let x = args.get_or_undefined(0).to_number(context)?;
    let y = args.get_or_undefined(1).to_number(context)?;
    if !x.is_finite() || !y.is_finite() {
        return Err(JsNativeError::typ()
            .with_message(format!("{name} called with ({x},{y})"))
            .into());
    }
    Ok((x, y))
}

fn add_point(command: char, x: f64, y: f64) -> JsResult<()> {
    STATE.with(|state| {
        let mut state = state.borrow_mut();
        state.points += 1;
        if state.points > state.max_points {
            state.overflow = true;
            return Err(JsNativeError::range()
                .with_message("too many points")
                .into());
        }
        if command == 'L' {
            state.lines += 1;
        }
        state.started = true;
        let [x0, y0, x1, y1] = state.bounds.unwrap_or([x, y, x, y]);
        state.bounds = Some([x0.min(x), y0.min(y), x1.max(x), y1.max(y)]);
        let separator = if state.path.is_empty() { "" } else { " " };
        let segment = format!("{separator}{command}{} {}", number(x), number(y));
        state.path.push_str(&segment);
        Ok(())
    })
}

fn js_move(_: &JsValue, args: &[JsValue], context: &mut Context) -> JsResult<JsValue> {
    let (x, y) = coordinates("Move", args, context)?;
    add_point('M', x, y)?;
    Ok(JsValue::undefined())
}

fn js_line(_: &JsValue, args: &[JsValue], context: &mut Context) -> JsResult<JsValue> {
    let (x, y) = coordinates("Line", args, context)?;
    // VisiCut starts a shape at the origin when it begins with a line.
    if !STATE.with(|s| s.borrow().started) {
        add_point('M', 0.0, 0.0)?;
    }
    add_point('L', x, y)?;
    Ok(JsValue::undefined())
}

fn message(text: String) {
    STATE.with(|state| {
        let mut state = state.borrow_mut();
        if state.messages.len() < MAX_MESSAGES {
            let text: String = text.chars().take(300).collect();
            state.messages.push(text);
        } else {
            state.dropped_messages += 1;
        }
    });
}

fn js_echo(_: &JsValue, args: &[JsValue], context: &mut Context) -> JsResult<JsValue> {
    let text = args.get_or_undefined(0).to_string(context)?;
    message(tr!(
        "LaserScript-Ausgabe: {text}",
        text = text.to_std_string_escaped()
    ));
    Ok(JsValue::undefined())
}

/// VisiCut asks the user; the import uses the script's default answer.
fn js_prompt(_: &JsValue, args: &[JsValue], context: &mut Context) -> JsResult<JsValue> {
    let title = args.get_or_undefined(0).to_string(context)?;
    let default = args.get_or_undefined(1);
    let answer = if default.is_null_or_undefined() {
        js_string!("")
    } else {
        default.to_string(context)?
    };
    message(tr!(
        "LaserScript-Eingabe „{title}“: Vorgabewert „{value}“ verwendet",
        title = title.to_std_string_escaped(),
        value = answer.to_std_string_escaped()
    ));
    Ok(answer.into())
}

pub fn number(value: f64) -> String {
    let rounded = (value * 10_000.0).round() / 10_000.0;
    if rounded == 0.0 {
        "0".into()
    } else {
        format!("{rounded}")
    }
}

/// SVG in millimetres; coordinates stay those of the script, so the drawing
/// keeps its position relative to the laser's origin.
pub fn to_svg(drawing: &Drawing) -> Result<(String, Vec<String>), String> {
    let Some([x0, y0, x1, y1]) = drawing.bounds.filter(|_| drawing.lines > 0) else {
        return Err(tr("LaserScript hat keine Linien gezeichnet").into());
    };
    let mut warnings = Vec::new();
    if x0 < 0.0 || y0 < 0.0 {
        warnings.push(
            tr("LaserScript zeichnet links oder oberhalb des Nullpunkts; \
                die Grafik beginnt deshalb dort")
            .into(),
        );
    }
    let left = x0.min(0.0);
    let top = y0.min(0.0);
    let width = (x1 - left).max(1.0);
    let height = (y1 - top).max(1.0);
    let svg = format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"{w}mm\" height=\"{h}mm\" viewBox=\"{l} {t} {w} {h}\">\n<path id=\"laserscript\" d=\"{d}\" fill=\"none\" stroke=\"#000000\" stroke-width=\"0.1\"/>\n</svg>\n",
        w = number(width),
        h = number(height),
        l = number(left),
        t = number(top),
        d = drawing.path,
    );
    Ok((svg, warnings))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn draws_paths_in_millimetres() {
        let drawing = run(
            "set('power', 50); move(1, 2); line(3.5, 2); line(get('power') / 10, 4); echo('ok');"
                .into(),
            script::TIME_LIMIT,
        )
        .unwrap();
        assert_eq!(drawing.path, "M1 2 L3.5 2 L5 4");
        assert_eq!(drawing.messages, ["LaserScript-Ausgabe: ok"]);
    }

    #[test]
    fn line_without_move_starts_at_origin() {
        let drawing = run("line(10, 0)".into(), script::TIME_LIMIT).unwrap();
        assert_eq!(drawing.path, "M0 0 L10 0");
    }

    #[test]
    fn prompts_use_defaults() {
        let drawing = run(
            "var s = promptFloat('Größe', 7); move(0, 0); line(s, 0);".into(),
            script::TIME_LIMIT,
        )
        .unwrap();
        assert_eq!(drawing.path, "M0 0 L7 0");
        assert!(drawing.messages[0].contains("Vorgabewert „7“"));
    }

    #[test]
    fn rejects_invalid_coordinates_and_errors() {
        assert!(run("move('a', 1)".into(), script::TIME_LIMIT).is_err());
        let error = run("move(0, 0); line(1, ".into(), script::TIME_LIMIT).unwrap_err();
        assert!(error.starts_with("LaserScript: Skriptfehler"), "{error}");
    }

    #[test]
    fn limits_output_size() {
        let error = run_with(
            "for (var i = 0; i < 2000; i++) { try { line(i % 100, 1); } catch (e) {} }".into(),
            script::TIME_LIMIT,
            1000,
        )
        .unwrap_err();
        assert!(error.contains("Punkte"), "{error}");
    }
}
