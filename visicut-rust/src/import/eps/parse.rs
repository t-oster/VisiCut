//! Tokenizer and parser for the PostScript subset: numbers, names, literal and
//! hex strings and nested `{ }` procedures. Every other token is an executable
//! name that the interpreter either implements or reports as unsupported.
use super::graphics::Matrix;
use std::rc::Rc;

/// Deepest allowed nesting of `{ }` procedures in the source.
const MAX_NESTING: usize = 100;

/// A PostScript object as far as the interpreter needs it.
#[derive(Clone, Debug)]
pub(super) enum Obj {
    Num(f64),
    Bool(bool),
    /// `/name` is a literal name, `name` an executable one.
    Name {
        name: Rc<str>,
        executable: bool,
    },
    Str(Rc<[u8]>),
    /// `{ ... }`: pushed unexecuted, run by `if`, `repeat`, `exec` and friends.
    Proc(Rc<[Obj]>),
    /// Result of `matrix`, the only array-like object the interpreter knows.
    Matrix(Matrix),
}

/// Parses a whole PostScript program into top-level objects.
pub(super) fn parse(src: &[u8]) -> Result<Rc<[Obj]>, String> {
    let mut root: Vec<Obj> = Vec::new();
    // Procedures that are still open, innermost last.
    let mut open: Vec<Vec<Obj>> = Vec::new();
    let mut i = 0;
    while i < src.len() {
        let byte = src[i];
        if is_space(byte) {
            i += 1;
            continue;
        }
        let (obj, next) = match byte {
            b'%' => {
                i = skip_comment(src, i);
                continue;
            }
            b'{' => {
                if open.len() >= MAX_NESTING {
                    return Err("Prozeduren sind zu tief verschachtelt".into());
                }
                open.push(Vec::new());
                i += 1;
                continue;
            }
            b'}' => {
                let body = open.pop().ok_or("Unerwartete „}“ in der EPS-Datei")?;
                (Obj::Proc(body.into()), i + 1)
            }
            b'(' => {
                let (bytes, next) = read_string(src, i)?;
                (Obj::Str(bytes.into()), next)
            }
            b'<' if src.get(i + 1) == Some(&b'<') => (exec_name("<<"), i + 2),
            b'<' => {
                let (bytes, next) = read_hex_string(src, i)?;
                (Obj::Str(bytes.into()), next)
            }
            b'>' if src.get(i + 1) == Some(&b'>') => (exec_name(">>"), i + 2),
            b'>' => (exec_name(">"), i + 1),
            b'[' => (exec_name("["), i + 1),
            b']' => (exec_name("]"), i + 1),
            b')' => (exec_name(")"), i + 1),
            b'/' => {
                let end = token_end(src, i + 1);
                let text = String::from_utf8_lossy(&src[i + 1..end]);
                (
                    Obj::Name {
                        name: text.as_ref().into(),
                        executable: false,
                    },
                    end,
                )
            }
            _ => {
                // None of the delimiters above, so the token is at least one byte.
                let end = token_end(src, i);
                let text = String::from_utf8_lossy(&src[i..end]);
                (token_object(&text), end)
            }
        };
        match open.last_mut() {
            Some(body) => body.push(obj),
            None => root.push(obj),
        }
        i = next;
    }
    if !open.is_empty() {
        return Err("Eine Prozedur „{“ in der EPS-Datei ist nicht geschlossen".into());
    }
    Ok(root.into())
}

fn is_space(byte: u8) -> bool {
    matches!(byte, b' ' | b'\t' | b'\r' | b'\n' | b'\x0c' | 0)
}

fn is_delimiter(byte: u8) -> bool {
    matches!(
        byte,
        b'(' | b')' | b'<' | b'>' | b'[' | b']' | b'{' | b'}' | b'/' | b'%'
    )
}

fn is_regular(byte: u8) -> bool {
    !is_space(byte) && !is_delimiter(byte)
}

fn token_end(src: &[u8], start: usize) -> usize {
    let mut end = start;
    while end < src.len() && is_regular(src[end]) {
        end += 1;
    }
    end
}

fn skip_comment(src: &[u8], start: usize) -> usize {
    let mut i = start;
    while i < src.len() && src[i] != b'\n' && src[i] != b'\r' {
        i += 1;
    }
    i
}

fn exec_name(text: &str) -> Obj {
    Obj::Name {
        name: text.into(),
        executable: true,
    }
}

/// Numbers are sign, digits and a decimal point; `inf` and `nan`, which Rust
/// would accept, are names here.
fn token_object(text: &str) -> Obj {
    let numeric = text
        .bytes()
        .all(|b| b.is_ascii_digit() || matches!(b, b'+' | b'-' | b'.'))
        && text.bytes().any(|b| b.is_ascii_digit());
    if numeric && let Ok(value) = text.parse::<f64>() {
        return Obj::Num(value);
    }
    exec_name(text)
}

/// Reads a `( ... )` string starting at `start`, honouring nested parentheses
/// and backslash escapes. Returns the bytes and the index after the `)`.
fn read_string(src: &[u8], start: usize) -> Result<(Vec<u8>, usize), String> {
    let mut out = Vec::new();
    let mut depth = 0usize;
    let mut i = start;
    while i < src.len() {
        let byte = src[i];
        i += 1;
        match byte {
            b'(' => {
                depth += 1;
                if depth > 1 {
                    out.push(byte);
                }
            }
            b')' => {
                depth = depth.saturating_sub(1);
                if depth == 0 {
                    return Ok((out, i));
                }
                out.push(byte);
            }
            b'\\' => {
                let Some(&escaped) = src.get(i) else { break };
                i += 1;
                match escaped {
                    b'n' => out.push(b'\n'),
                    b'r' => out.push(b'\r'),
                    b't' => out.push(b'\t'),
                    b'b' => out.push(8),
                    b'f' => out.push(12),
                    b'0'..=b'7' => {
                        // Up to three octal digits; the value wraps to a byte.
                        let mut value = u32::from(escaped - b'0');
                        for _ in 1..3 {
                            match src.get(i) {
                                Some(&d @ b'0'..=b'7') => {
                                    value = value * 8 + u32::from(d - b'0');
                                    i += 1;
                                }
                                _ => break,
                            }
                        }
                        out.push(value as u8);
                    }
                    // Backslash at the end of a line continues the string.
                    b'\r' => {
                        if src.get(i) == Some(&b'\n') {
                            i += 1;
                        }
                    }
                    b'\n' => {}
                    other => out.push(other),
                }
            }
            _ => out.push(byte),
        }
    }
    Err("Eine Zeichenkette „(...)“ in der EPS-Datei ist nicht geschlossen".into())
}

/// Reads a `< ... >` hex string starting at `start`.
fn read_hex_string(src: &[u8], start: usize) -> Result<(Vec<u8>, usize), String> {
    let mut nibbles = Vec::new();
    let mut i = start + 1;
    while i < src.len() {
        let byte = src[i];
        i += 1;
        match byte {
            b'>' => {
                let bytes = nibbles
                    .chunks(2)
                    .map(|pair| (pair[0] << 4) | pair.get(1).copied().unwrap_or(0))
                    .collect();
                return Ok((bytes, i));
            }
            _ if is_space(byte) => {}
            _ => match (byte as char).to_digit(16) {
                Some(value) => nibbles.push(value as u8),
                None => return Err("Ungültige Hex-Zeichenkette in der EPS-Datei".into()),
            },
        }
    }
    Err("Eine Hex-Zeichenkette „<...>“ in der EPS-Datei ist nicht geschlossen".into())
}
