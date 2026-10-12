//! Parametric SVG templates, ported from VisiCut's `ParametricSVGImporter`
//! (`.parametric.svg`, Thymeleaf attributes such as `th:attr`, `th:each`,
//! `th:if`, `th:text`) and `PSVGImporter` (`.psvg`, attribute values with
//! `{expression}` parts and `<ref param="$name" default="expression"/>`).
//!
//! VisiCut asks for the parameter values in a dialog. Here the declared
//! defaults (or the values saved next to the file in `<name>.parameters`)
//! are used. Thymeleaf evaluates OGNL; these expressions are translated to
//! JavaScript (`gt` → `>`, `#numbers.sequence` → a helper, …) and run in the
//! sandboxed engine, which also evaluates PSVG defaults like VisiCut's
//! `Helper.evaluateExpression`.
use super::script::{self, Engine};
use super::xml;
use crate::{i18n::tr, tr};
use boa_engine::{JsString, JsValue, js_string};
use roxmltree::Node;
use std::ops::Range;
use std::time::Duration;

const BOOTSTRAP: &str = r#"
function __iter(v) {
  if (v === null || v === undefined) return [];
  if (Array.isArray(v)) return v;
  if (typeof v === 'object' && typeof v.length === 'number') return Array.prototype.slice.call(v);
  return [v];
}
function __seq(from, to, step) {
  from = Math.trunc(from); to = Math.trunc(to);
  if (step === undefined) step = from <= to ? 1 : -1;
  var r = [];
  if (!step) return r;
  for (var i = from; step > 0 ? i <= to : i >= to; i += step) {
    r.push(i);
    if (r.length > 100000) throw new RangeError('#numbers.sequence too long');
  }
  return r;
}
function __truthy(v) {
  if (v === null || v === undefined) return false;
  if (typeof v === 'boolean') return v;
  if (typeof v === 'number') return v !== 0 && !isNaN(v);
  if (typeof v === 'string') { var s = v.toLowerCase(); return s !== 'false' && s !== 'off' && s !== 'no'; }
  return true;
}
function __status(index, size, current) {
  return { index: index, count: index + 1, size: size, current: current,
           even: (index + 1) % 2 === 0, odd: (index + 1) % 2 === 1,
           first: index === 0, last: index === size - 1 };
}
"#;

#[derive(Clone, Debug, PartialEq)]
pub enum Value {
    Number(f64),
    Integer(i64),
    Boolean(bool),
    Text(String),
    Null,
}

impl Value {
    fn to_js(&self) -> JsValue {
        match self {
            Value::Number(v) => JsValue::from(*v),
            Value::Integer(v) => JsValue::from(*v as f64),
            Value::Boolean(v) => JsValue::from(*v),
            Value::Text(v) => JsValue::from(JsString::from(v.as_str())),
            Value::Null => JsValue::null(),
        }
    }

    pub fn display(&self) -> String {
        match self {
            Value::Number(v) => format!("{v}"),
            Value::Integer(v) => v.to_string(),
            Value::Boolean(v) => v.to_string(),
            Value::Text(v) => format!("„{v}“"),
            Value::Null => tr("leer").into(),
        }
    }
}

pub struct Parameter {
    pub name: String,
    pub value: Value,
    pub saved: bool,
}

pub struct Rendered {
    pub svg: String,
    pub parameters: Vec<Parameter>,
    pub warnings: Vec<String>,
}

/// Evaluates the template with default parameter values, replaced by
/// `saved` values where names match (VisiCut's `.parameters` file).
pub fn render(
    source: String,
    psvg: bool,
    saved: Vec<(String, Value)>,
    limit: Duration,
) -> Result<Rendered, String> {
    let source = xml::declare_thymeleaf(&source);
    script::run_isolated(limit, move |engine| {
        let options = roxmltree::ParsingOptions {
            allow_dtd: true,
            ..Default::default()
        };
        let document = roxmltree::Document::parse_with_options(&source, options)
            .map_err(|e| tr!("SVG konnte nicht gelesen werden: {error}", error = e))?;
        engine.eval(BOOTSTRAP)?;
        let mut renderer = Renderer {
            engine,
            source: &source,
            psvg,
            scope: Vec::new(),
            warnings: Vec::new(),
        };
        let mut parameters = renderer.parameters(&document)?;
        for (name, value) in saved {
            if let Some(p) = parameters.iter_mut().find(|p| p.name == name) {
                p.value = value;
                p.saved = true;
            }
        }
        renderer.scope = parameters
            .iter()
            .map(|p| (p.name.clone(), p.value.to_js()))
            .collect();
        let root = document.root_element();
        let mut svg = source[..root.range().start].to_string();
        renderer.element(root, &mut svg)?;
        svg.push_str(&source[root.range().end..]);
        let warnings = renderer.warnings;
        Ok(Rendered {
            svg,
            parameters,
            warnings,
        })
    })
}

struct Renderer<'a> {
    engine: &'a mut Engine,
    source: &'a str,
    psvg: bool,
    scope: Vec<(String, JsValue)>,
    warnings: Vec<String>,
}

/// An attribute value to be computed, with the source range it replaces.
struct Computed {
    name: String,
    expression: String,
    range: Range<usize>,
}

impl Renderer<'_> {
    fn parameters(&mut self, document: &roxmltree::Document) -> Result<Vec<Parameter>, String> {
        let mut result: Vec<Parameter> = Vec::new();
        let refs = document
            .descendants()
            .filter(|n| n.is_element() && n.tag_name().name() == "ref");
        for node in refs {
            let Some(param) = node.attribute("param") else {
                continue;
            };
            let name = if self.psvg {
                param.replace('$', "")
            } else {
                param.to_string()
            };
            let mut default = node.attribute("default").map(str::to_string);
            if self.psvg
                && let Some(expression) = &default
            {
                // VisiCut evaluates PSVG defaults as JavaScript with the
                // previous parameters as variables and German decimal commas.
                self.scope = result
                    .iter()
                    .map(|p| (p.name.clone(), p.value.to_js()))
                    .collect();
                let code = expression.replace('$', "").replace(',', ".");
                let value = self.evaluate_js(&code, expression)?;
                let number = value
                    .to_number(&mut self.engine.context)
                    .ok()
                    .filter(|n| !n.is_nan())
                    .ok_or_else(|| {
                        tr!(
                            "Parameter „{name}“: Vorgabe „{expression}“ ist keine Zahl",
                            name = name,
                            expression = expression
                        )
                    })?;
                default = Some(number.to_string());
            }
            let kind = node.attribute("type").unwrap_or("Double");
            let base = kind.split('(').next().unwrap_or(kind).trim();
            let number = |text: &Option<String>| -> Result<f64, String> {
                match text {
                    None => Ok(0.0),
                    Some(t) => t.trim().parse::<f64>().map_err(|_| {
                        tr!(
                            "Parameter „{name}“: Vorgabe „{expression}“ ist keine Zahl",
                            name = name,
                            expression = t
                        )
                    }),
                }
            };
            let value = match base {
                "Double" => Value::Number(number(&default)?),
                "Integer" => Value::Integer(number(&default)?.trunc() as i64),
                "Boolean" => Value::Boolean(
                    default
                        .as_deref()
                        .is_some_and(|d| d.trim().eq_ignore_ascii_case("true")),
                ),
                "String" => Value::Text(default.unwrap_or_default()),
                _ => {
                    self.warnings.push(tr!(
                        "Parameter „{name}“ hat den unbekannten Typ „{kind}“",
                        name = name,
                        kind = kind
                    ));
                    Value::Null
                }
            };
            match result.iter_mut().find(|p| p.name == name) {
                Some(existing) => existing.value = value,
                None => result.push(Parameter {
                    name,
                    value,
                    saved: false,
                }),
            }
        }
        Ok(result)
    }

    fn element(&mut self, node: Node, out: &mut String) -> Result<(), String> {
        if out.len() > crate::svg_import::MAX_SVG_BYTES {
            return Err(tr("Parametrische SVG würde größer als 20 MB").into());
        }
        let mut th = Vec::new();
        let mut computed = Vec::new();
        for attribute in node.attributes() {
            let is_th = attribute.namespace().is_some_and(|uri| {
                uri.contains("thymeleaf") || node.lookup_prefix(uri) == Some("th")
            });
            if is_th {
                th.push((
                    attribute.name().to_string(),
                    attribute.value().to_string(),
                    attribute.range(),
                ));
            } else if self.psvg
                && node.tag_name().name() != "ref"
                && attribute.value().contains('{')
            {
                computed.push(Computed {
                    name: self.source[attribute.range_qname()].to_string(),
                    expression: psvg_template(attribute.value())?,
                    range: attribute.range(),
                });
            }
        }
        if th.is_empty() && computed.is_empty() {
            let tag_end = xml::start_tag_end(self.source, node.range().start);
            out.push_str(&self.source[node.range().start..tag_end]);
            self.content(node, tag_end, out)?;
            out.push_str(self.end_tag(node, tag_end));
            return Ok(());
        }
        let get = |name: &str| {
            th.iter()
                .find(|(n, _, _)| n == name)
                .map(|(_, v, _)| v.clone())
        };
        if let Some(each) = get("each") {
            let (variable, status, expression) = parse_each(&each)?;
            let items = self.items(&expression)?;
            let size = items.len();
            for (index, item) in items.into_iter().enumerate() {
                let depth = self.scope.len();
                self.scope.push(("__current".into(), item.clone()));
                let status_value =
                    self.evaluate_js(&format!("__status({index}, {size}, __current)"), &each)?;
                self.scope.push((variable.clone(), item));
                self.scope.push((status.clone(), status_value));
                let result = self.body(node, &th, &computed, out);
                self.scope.truncate(depth);
                result?;
            }
            return Ok(());
        }
        self.body(node, &th, &computed, out)
    }

    fn body(
        &mut self,
        node: Node,
        th: &[(String, String, Range<usize>)],
        computed: &[Computed],
        out: &mut String,
    ) -> Result<(), String> {
        let get = |name: &str| {
            th.iter()
                .find(|(n, _, _)| n == name)
                .map(|(_, v, _)| v.as_str())
        };
        if let Some(condition) = get("if")
            && !self.truthy(condition)?
        {
            return Ok(());
        }
        if let Some(condition) = get("unless")
            && self.truthy(condition)?
        {
            return Ok(());
        }
        let depth = self.scope.len();
        let result = self.emit(node, th, computed, out);
        self.scope.truncate(depth);
        result
    }

    fn emit(
        &mut self,
        node: Node,
        th: &[(String, String, Range<usize>)],
        computed: &[Computed],
        out: &mut String,
    ) -> Result<(), String> {
        let source = self.source;
        let mut set: Vec<(String, Option<String>)> = Vec::new();
        let mut text = None;
        let mut remove = None;
        for (name, value, _) in th {
            match name.as_str() {
                "each" | "if" | "unless" | "fragment" => {}
                "with" => {
                    for (variable, expression) in assignments(value)? {
                        let result = self.evaluate(&expression)?;
                        self.scope.push((variable, result));
                    }
                }
                "attr" => {
                    for (attribute, expression) in assignments(value)? {
                        let result = self.evaluate(&expression)?;
                        let result = self.optional_string(&result)?;
                        set.push((attribute, result));
                    }
                }
                "attrappend" | "attrprepend" => {
                    for (attribute, expression) in assignments(value)? {
                        let result = self.evaluate(&expression)?;
                        let Some(addition) = self.optional_string(&result)? else {
                            continue;
                        };
                        let current = set
                            .iter()
                            .rev()
                            .find(|(n, _)| *n == attribute)
                            .map(|(_, v)| v.clone().unwrap_or_default())
                            .or_else(|| attribute_value(node, source, &attribute))
                            .unwrap_or_default();
                        let combined = if name == "attrappend" {
                            current + &addition
                        } else {
                            addition + &current
                        };
                        set.push((attribute, Some(combined)));
                    }
                }
                "text" | "utext" => {
                    let result = self.evaluate(value)?;
                    let result = self.optional_string(&result)?.unwrap_or_default();
                    text = Some(if name == "utext" {
                        result
                    } else {
                        xml::escape_text(&result)
                    });
                }
                "remove" => remove = Some(value.trim().to_string()),
                "include" | "replace" | "insert" | "substituteby" | "switch" | "case"
                | "object" | "inline" | "block" => {
                    self.warnings.push(tr!(
                        "Thymeleaf-Attribut „th:{name}“ wird nicht unterstützt und ignoriert",
                        name = name
                    ));
                }
                attribute => {
                    let result = self.evaluate(value)?;
                    let result = self.optional_string(&result)?;
                    set.push((attribute.to_string(), result));
                }
            }
        }
        for item in computed {
            let result = self.evaluate_js(&item.expression, &item.expression)?;
            let result = self.optional_string(&result)?;
            set.push((item.name.clone(), result));
        }
        let mut final_set: Vec<(String, Option<String>)> = Vec::new();
        for (name, value) in set {
            final_set.retain(|(n, _)| *n != name);
            final_set.push((name, value));
        }
        let mut removed: Vec<Range<usize>> = th.iter().map(|(_, _, r)| r.clone()).collect();
        removed.extend(computed.iter().map(|c| c.range.clone()));
        for attribute in node.attributes() {
            let qname = &source[attribute.range_qname()];
            if final_set.iter().any(|(n, _)| n == qname) {
                removed.push(attribute.range());
            }
        }
        let added: Vec<(String, String)> = final_set
            .into_iter()
            .filter_map(|(n, v)| v.map(|v| (n, v)))
            .collect();
        let start = node.range().start;
        let tag_end = xml::start_tag_end(source, start);
        let self_closing = xml::is_self_closing(source, tag_end);
        let mut tag = xml::rewrite_start_tag(source, start, tag_end, &removed, &added);
        let qname = xml::qualified_name(source, start);
        match remove.as_deref() {
            Some("all") => return Ok(()),
            Some("tag") => {
                if text.is_some() {
                    out.push_str(text.as_deref().unwrap_or_default());
                } else {
                    self.content(node, tag_end, out)?;
                }
                return Ok(());
            }
            Some("body") => text = Some(String::new()),
            _ => {}
        }
        if let Some(text) = text {
            if self_closing {
                tag = format!("{}>", tag.trim_end_matches("/>").trim_end());
            }
            out.push_str(&tag);
            out.push_str(&text);
            out.push_str(&format!("</{qname}>"));
            return Ok(());
        }
        out.push_str(&tag);
        if remove.as_deref() == Some("all-but-first") {
            if let Some(first) = node.children().find(|c| c.is_element()) {
                self.element(first, out)?;
            }
        } else {
            self.content(node, tag_end, out)?;
        }
        out.push_str(self.end_tag(node, tag_end));
        Ok(())
    }

    /// Copies everything between the tags, processing child elements.
    fn content(&mut self, node: Node, tag_end: usize, out: &mut String) -> Result<(), String> {
        let source = self.source;
        if xml::is_self_closing(source, tag_end) {
            return Ok(());
        }
        let end = self.end_tag_start(node);
        let mut position = tag_end;
        for child in node.children().filter(|c| c.is_element()) {
            out.push_str(&source[position..child.range().start]);
            self.element(child, out)?;
            position = child.range().end;
        }
        out.push_str(&source[position..end]);
        Ok(())
    }

    fn end_tag_start(&self, node: Node) -> usize {
        let range = node.range();
        self.source[range.clone()]
            .rfind("</")
            .map_or(range.end, |i| range.start + i)
    }

    fn end_tag(&self, node: Node, tag_end: usize) -> &str {
        if xml::is_self_closing(self.source, tag_end) {
            ""
        } else {
            &self.source[self.end_tag_start(node)..node.range().end]
        }
    }

    fn evaluate(&mut self, expression: &str) -> Result<JsValue, String> {
        let code = thymeleaf_to_js(expression)?;
        self.evaluate_js(&code, expression)
    }

    fn evaluate_js(&mut self, code: &str, shown: &str) -> Result<JsValue, String> {
        let global = self.engine.context.global_object();
        for (name, value) in &self.scope {
            global
                .set(
                    JsString::from(name.as_str()),
                    value.clone(),
                    false,
                    &mut self.engine.context,
                )
                .map_err(|e| script::describe(&e))?;
        }
        self.engine
            .eval(code)
            .map_err(|e| tr!("Ausdruck „{shown}“: {error}", shown = shown, error = e))
    }

    fn truthy(&mut self, expression: &str) -> Result<bool, String> {
        let code = format!("__truthy({})", thymeleaf_to_js(expression)?);
        Ok(self.evaluate_js(&code, expression)?.to_boolean())
    }

    fn items(&mut self, expression: &str) -> Result<Vec<JsValue>, String> {
        let code = format!("__iter({})", thymeleaf_to_js(expression)?);
        let array = self.evaluate_js(&code, expression)?;
        let context = &mut self.engine.context;
        let object = array.as_object().ok_or_else(|| {
            tr!(
                "th:each „{expression}“ ergibt keine Liste",
                expression = expression
            )
        })?;
        let length = object
            .get(js_string!("length"), context)
            .and_then(|l| l.to_length(context))
            .map_err(|e| script::describe(&e))?;
        if length > 100_000 {
            return Err(tr!(
                "th:each „{expression}“ ergibt zu viele Elemente",
                expression = expression
            ));
        }
        (0..length as u32)
            .map(|i| object.get(i, context).map_err(|e| script::describe(&e)))
            .collect()
    }

    fn optional_string(&mut self, value: &JsValue) -> Result<Option<String>, String> {
        if value.is_null_or_undefined() {
            Ok(None)
        } else {
            self.engine.string(value).map(Some)
        }
    }
}

fn attribute_value(node: Node, source: &str, qname: &str) -> Option<String> {
    node.attributes()
        .find(|a| &source[a.range_qname()] == qname)
        .map(|a| a.value().to_string())
}

/// `a{$x * 2}b` → `'a' + (x * 2) + 'b'`, like `PSVGImporter`'s translation.
fn psvg_template(value: &str) -> Result<String, String> {
    let value = value.replace('$', "");
    let mut parts = vec!["''".to_string()];
    let mut rest = value.as_str();
    while let Some(open) = rest.find('{') {
        if open > 0 {
            parts.push(js_string_literal(&rest[..open]));
        }
        let close = rest[open..]
            .find('}')
            .map(|i| open + i)
            .ok_or_else(|| tr!("Ausdruck „{text}“: schließende Klammer fehlt", text = value))?;
        parts.push(format!("({})", convert(&rest[open + 1..close], true)?));
        rest = &rest[close + 1..];
    }
    if !rest.is_empty() {
        parts.push(js_string_literal(rest));
    }
    Ok(parts.join(" + "))
}

fn js_string_literal(text: &str) -> String {
    serde_json::to_string(text).unwrap_or_else(|_| "''".into())
}

/// Translates a Thymeleaf standard expression (OGNL inside `${…}`) to JS.
pub fn thymeleaf_to_js(expression: &str) -> Result<String, String> {
    convert(expression, false)
}

fn convert(text: &str, inside: bool) -> Result<String, String> {
    let chars: Vec<char> = text.chars().collect();
    let mut out = String::new();
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        let next = chars.get(i + 1).copied();
        match c {
            '\'' | '"' => {
                let end = string_end(&chars, i).ok_or_else(|| {
                    tr!(
                        "Ausdruck „{text}“: Anführungszeichen nicht geschlossen",
                        text = text
                    )
                })?;
                if c == '\'' {
                    let literal: String = chars[i + 1..end].iter().collect();
                    out.push_str(&js_string_literal(&literal.replace("\\'", "'")));
                } else {
                    out.extend(&chars[i..=end]);
                }
                i = end + 1;
            }
            '$' | '*' if !inside && next == Some('{') => {
                let end = brace_end(&chars, i + 1).ok_or_else(|| {
                    tr!("Ausdruck „{text}“: schließende Klammer fehlt", text = text)
                })?;
                let inner: String = chars[i + 2..end].iter().collect();
                out.push('(');
                out.push_str(&convert(&inner, true)?);
                out.push(')');
                i = end + 1;
            }
            '#' | '@' | '~' if !inside && next == Some('{') => {
                return Err(tr!(
                    "Thymeleaf-Ausdruck „{expression}“ in „{text}“ wird nicht unterstützt",
                    expression = format!("{c}{{…}}"),
                    text = text
                ));
            }
            '#' if inside => {
                let rest: String = chars[i..].iter().collect();
                const SEQUENCE: &str = "#numbers.sequence";
                if rest.starts_with(SEQUENCE) {
                    out.push_str("__seq");
                    i += SEQUENCE.chars().count();
                } else {
                    let name: String = rest
                        .chars()
                        .take_while(|c| *c == '#' || c.is_alphanumeric() || *c == '.')
                        .collect();
                    return Err(tr!(
                        "Thymeleaf-Hilfsobjekt „{name}“ wird nicht unterstützt",
                        name = name
                    ));
                }
            }
            '|' if !inside => {
                let end = chars[i + 1..]
                    .iter()
                    .position(|c| *c == '|')
                    .map(|p| i + 1 + p)
                    .ok_or_else(|| tr!("Ausdruck „{text}“: schließendes | fehlt", text = text))?;
                let mut parts = vec!["''".to_string()];
                let mut literal = String::new();
                let mut j = i + 1;
                while j < end {
                    if chars[j] == '$' && chars.get(j + 1) == Some(&'{') {
                        let close = brace_end(&chars[..end], j + 1).ok_or_else(|| {
                            tr!("Ausdruck „{text}“: schließende Klammer fehlt", text = text)
                        })?;
                        if !literal.is_empty() {
                            parts.push(js_string_literal(&std::mem::take(&mut literal)));
                        }
                        let inner: String = chars[j + 2..close].iter().collect();
                        parts.push(format!("({})", convert(&inner, true)?));
                        j = close + 1;
                    } else {
                        literal.push(chars[j]);
                        j += 1;
                    }
                }
                if !literal.is_empty() {
                    parts.push(js_string_literal(&literal));
                }
                out.push('(');
                out.push_str(&parts.join(" + "));
                out.push(')');
                i = end + 1;
            }
            c if c.is_alphabetic() || c == '_' => {
                let start = i;
                while i < chars.len() && (chars[i].is_alphanumeric() || chars[i] == '_') {
                    i += 1;
                }
                let word: String = chars[start..i].iter().collect();
                let after_dot = chars[..start]
                    .iter()
                    .rev()
                    .find(|c| !c.is_whitespace())
                    .is_some_and(|c| *c == '.');
                let operator = match word.as_str() {
                    "gt" => Some(">"),
                    "lt" => Some("<"),
                    "ge" => Some(">="),
                    "le" => Some("<="),
                    "eq" => Some("=="),
                    "ne" | "neq" => Some("!="),
                    "and" => Some("&&"),
                    "or" => Some("||"),
                    "not" => Some("!"),
                    "div" => Some("/"),
                    "mod" => Some("%"),
                    _ => None,
                };
                match operator {
                    Some(op) if !after_dot => {
                        out.push(' ');
                        out.push_str(op);
                        out.push(' ');
                    }
                    _ if inside
                        || after_dot
                        || matches!(word.as_str(), "true" | "false" | "null") =>
                    {
                        out.push_str(&word);
                    }
                    // Outside `${…}` a bare word is a Thymeleaf token literal.
                    _ => out.push_str(&js_string_literal(&word)),
                }
            }
            _ => {
                out.push(c);
                i += 1;
            }
        }
    }
    Ok(out)
}

fn string_end(chars: &[char], start: usize) -> Option<usize> {
    let quote = chars[start];
    let mut i = start + 1;
    while i < chars.len() {
        match chars[i] {
            '\\' => i += 2,
            c if c == quote => return Some(i),
            _ => i += 1,
        }
    }
    None
}

/// Index of the `}` matching the `{` at `open`, skipping strings.
fn brace_end(chars: &[char], open: usize) -> Option<usize> {
    let mut depth = 0;
    let mut i = open;
    while i < chars.len() {
        match chars[i] {
            '\'' | '"' => i = string_end(chars, i)?,
            '{' => depth += 1,
            '}' => {
                depth -= 1;
                if depth == 0 {
                    return Some(i);
                }
            }
            _ => {}
        }
        i += 1;
    }
    None
}

/// Splits at `separator` outside strings, brackets and `${…}`.
fn split_top_level(text: &str, separator: char) -> Vec<String> {
    let chars: Vec<char> = text.chars().collect();
    let mut parts = Vec::new();
    let mut current = String::new();
    let mut depth = 0i32;
    let mut pipe = false;
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        if (c == '\'' || c == '"')
            && let Some(end) = string_end(&chars, i)
        {
            current.extend(&chars[i..=end]);
            i = end + 1;
            continue;
        }
        match c {
            '(' | '[' | '{' => depth += 1,
            ')' | ']' | '}' => depth -= 1,
            '|' => pipe = !pipe,
            _ => {}
        }
        if c == separator && depth == 0 && !pipe {
            parts.push(std::mem::take(&mut current));
        } else {
            current.push(c);
        }
        i += 1;
    }
    parts.push(current);
    parts
}

/// `a = expr, b = expr` (`th:attr`, `th:with`).
fn assignments(text: &str) -> Result<Vec<(String, String)>, String> {
    let mut result = Vec::new();
    for part in split_top_level(text, ',') {
        if part.trim().is_empty() {
            continue;
        }
        let chars: Vec<char> = part.chars().collect();
        let position = (0..chars.len()).find(|&i| {
            chars[i] == '='
                && chars.get(i + 1) != Some(&'=')
                && !matches!(chars.get(i.wrapping_sub(1)), Some('=' | '!' | '<' | '>'))
        });
        let Some(position) = position else {
            return Err(tr!("Zuweisung „{part}“ ohne „=“", part = part));
        };
        let name: String = chars[..position].iter().collect();
        let expression: String = chars[position + 1..].iter().collect();
        result.push((name.trim().to_string(), expression.trim().to_string()));
    }
    Ok(result)
}

/// `item : ${list}` or `item, status : ${list}`.
fn parse_each(text: &str) -> Result<(String, String, String), String> {
    let parts = split_top_level(text, ':');
    if parts.len() < 2 {
        return Err(tr!("th:each „{text}“ ist ungültig", text = text));
    }
    let expression = parts[1..].join(":");
    let mut names = parts[0].split(',').map(str::trim);
    let variable = names.next().unwrap_or_default().to_string();
    let status = names
        .next()
        .map_or_else(|| format!("{variable}Stat"), str::to_string);
    if variable.is_empty() {
        return Err(tr!("th:each „{text}“ ist ungültig", text = text));
    }
    Ok((variable, status, expression.trim().to_string()))
}

/// Values from VisiCut's `.parameters` file (XStream XML of a map).
pub fn read_saved_values(text: &str) -> Result<Vec<(String, Value)>, String> {
    let document = roxmltree::Document::parse(text).map_err(|e| e.to_string())?;
    let mut result = Vec::new();
    for entry in document
        .root_element()
        .children()
        .filter(|n| n.is_element())
    {
        let mut items = entry.children().filter(|n| n.is_element());
        let (Some(key), Some(value)) = (items.next(), items.next()) else {
            continue;
        };
        let key = key.text().unwrap_or_default().to_string();
        let text = value.text().unwrap_or_default().trim();
        let value = match value.tag_name().name() {
            "double" | "float" => text.parse().map(Value::Number).unwrap_or(Value::Null),
            "int" | "long" | "short" | "byte" => {
                text.parse().map(Value::Integer).unwrap_or(Value::Null)
            }
            "boolean" => Value::Boolean(text == "true"),
            "null" => Value::Null,
            _ => Value::Text(value.text().unwrap_or_default().to_string()),
        };
        result.push((key, value));
    }
    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn render_ok(source: &str, psvg: bool) -> Rendered {
        render(source.into(), psvg, Vec::new(), script::TIME_LIMIT).unwrap()
    }

    #[test]
    fn translates_thymeleaf_expressions() {
        assert_eq!(
            thymeleaf_to_js("'translate(' + ${36 * i} + ', 0)'").unwrap(),
            "\"translate(\" + (36 * i) + \", 0)\""
        );
        assert_eq!(
            thymeleaf_to_js("${smileys gt 0 and not x}").unwrap(),
            "(smileys  >  0  &&   !  x)"
        );
        assert_eq!(
            thymeleaf_to_js("${#numbers.sequence(1, n)}").unwrap(),
            "(__seq(1, n))"
        );
        assert!(thymeleaf_to_js("#{message}").is_err());
    }

    #[test]
    fn renders_parametric_svg_with_defaults() {
        let source = r#"<svg xmlns="http://www.w3.org/2000/svg" xmlns:th="http://www.thymeleaf.org" width="100mm" height="50mm" viewBox="0 0 100 50">
  <defs><ref param="holes" type="Integer" default="3"/><ref param="mood" type="String(happy,sad)" default="happy"/><ref param="cut" type="Boolean" default="false"/></defs>
  <rect id="r" width="1" height="1" th:attr="width=${holes * 10}, height = 5"/>
  <g th:each="i : ${#numbers.sequence(1, holes)}" th:attr="transform='translate(' + ${i * 20} + ',0)'"><circle r="2"/></g>
  <path d="M0 0" th:if="${mood == 'sad'}"/>
  <path id="cut" d="M0 0 L1 1" th:unless="${cut}"/>
  <text th:text="${'Mood: ' + mood}">old<tspan>x</tspan></text>
</svg>"#;
        let rendered = render_ok(source, false);
        let svg = &rendered.svg;
        assert!(
            svg.contains(r#"<rect id="r" width="30" height="5"/>"#),
            "{svg}"
        );
        assert!(svg.contains(r#"transform="translate(20,0)""#));
        assert!(svg.contains(r#"transform="translate(60,0)""#));
        assert!(!svg.contains("translate(80"));
        assert!(!svg.contains("th:"), "{svg}");
        assert!(!svg.contains(r#"<path d="M0 0"/>"#));
        assert!(svg.contains(r#"id="cut""#));
        assert!(svg.contains(">Mood: happy</text>"));
        roxmltree::Document::parse(svg).unwrap();
        let names: Vec<_> = rendered
            .parameters
            .iter()
            .map(|p| (p.name.as_str(), p.value.display()))
            .collect();
        assert_eq!(
            names,
            [
                ("holes", "3".to_string()),
                ("mood", "„happy“".to_string()),
                ("cut", "false".to_string())
            ]
        );
    }

    #[test]
    fn renders_psvg_expressions_and_saved_values() {
        let source = r#"<svg xmlns="http://www.w3.org/2000/svg" width="{$w + 10}mm" height="20mm" viewBox="0 0 {$w + 10} 20">
  <defs><ref param="$w" default="40"/><ref param="$h" default="$w / 4"/></defs>
  <rect width="{$w}" height="{$h}" x="5"/>
</svg>"#;
        let rendered = render_ok(source, true);
        assert!(
            rendered
                .svg
                .contains(r#"<rect x="5" width="40" height="10"/>"#),
            "{}",
            rendered.svg
        );
        assert!(rendered.svg.contains(r#"width="50mm""#));
        let saved = vec![("w".to_string(), Value::Number(60.0))];
        let rendered = render(source.into(), true, saved, script::TIME_LIMIT).unwrap();
        assert!(rendered.svg.contains(r#"width="60""#), "{}", rendered.svg);
        assert!(rendered.parameters[0].saved);
    }

    #[test]
    fn reads_xstream_parameter_files() {
        let text = "<parameters>\n  <entry>\n    <string>holes</string>\n    <int>5</int>\n  </entry>\n  <entry>\n    <string>mood</string>\n    <string>sad</string>\n  </entry>\n</parameters>";
        assert_eq!(
            read_saved_values(text).unwrap(),
            [
                ("holes".to_string(), Value::Integer(5)),
                ("mood".to_string(), Value::Text("sad".into()))
            ]
        );
    }
}
