//! Tolerant reader for ASCII and binary DXF files.
//!
//! The file is split into group-code/value pairs, the pairs into records
//! (everything between two `0` codes) and the records into the sections the
//! importer needs: header variables, layer and linetype tables, blocks and
//! entities. Unknown sections, objects and group codes are ignored, and badly
//! formatted numbers read as missing instead of rejecting the whole file.
use crate::{i18n::tr, tr};
use std::collections::HashMap;

#[derive(Clone, Debug, PartialEq)]
pub struct Pair {
    pub code: i32,
    pub value: String,
}

/// A DXF entity with its group codes. POLYLINE keeps its VERTEX records and
/// INSERT its ATTRIB records as children.
#[derive(Clone, Debug, Default)]
pub struct Entity {
    pub kind: String,
    pub pairs: Vec<Pair>,
    pub children: Vec<Entity>,
}

impl Entity {
    pub fn str(&self, code: i32) -> Option<&str> {
        self.pairs
            .iter()
            .find(|p| p.code == code)
            .map(|p| p.value.as_str())
    }

    pub fn f(&self, code: i32) -> Option<f64> {
        self.pairs
            .iter()
            .find(|p| p.code == code)
            .and_then(|p| number(&p.value))
    }

    pub fn fd(&self, code: i32, default: f64) -> f64 {
        self.f(code).unwrap_or(default)
    }

    pub fn int(&self, code: i32) -> Option<i64> {
        self.f(code).map(|v| v as i64)
    }

    pub fn all(&self, code: i32) -> impl Iterator<Item = f64> + '_ {
        self.pairs
            .iter()
            .filter(move |p| p.code == code)
            .map(|p| number(&p.value).unwrap_or(0.0))
    }

    /// Points given by repeated `x_code`/`x_code + 10` pairs, in file order.
    pub fn points(&self, x_code: i32) -> Vec<[f64; 2]> {
        let mut points = Vec::new();
        for pair in &self.pairs {
            if pair.code == x_code {
                points.push([number(&pair.value).unwrap_or(0.0), 0.0]);
            } else if pair.code == x_code + 10
                && let Some(last) = points.last_mut()
            {
                last[1] = number(&pair.value).unwrap_or(0.0);
            }
        }
        points
    }
}

/// Parses a DXF number; some exporters write a decimal comma.
pub fn number(value: &str) -> Option<f64> {
    let value = value.trim();
    value
        .parse::<f64>()
        .ok()
        .or_else(|| value.replace(',', ".").parse().ok())
        .filter(|v: &f64| v.is_finite())
}

#[derive(Clone, Debug)]
pub struct Layer {
    pub name: String,
    pub color: i64,
    pub true_color: Option<u32>,
    pub linetype: String,
    pub lineweight: i64,
    pub flags: i64,
}

impl Layer {
    pub fn hidden(&self) -> bool {
        self.color < 0 || self.flags & 1 != 0
    }
}

#[derive(Clone, Debug, Default)]
pub struct Block {
    pub base: [f64; 2],
    pub entities: Vec<Entity>,
}

#[derive(Debug, Default)]
pub struct Document {
    pub header: HashMap<String, Vec<Pair>>,
    pub layers: Vec<Layer>,
    /// Upper-case name → index into `layers`.
    pub layer_index: HashMap<String, usize>,
    /// Upper-case linetype name → DXF pattern (negative = gap, 0 = dot).
    pub linetypes: HashMap<String, Vec<f64>>,
    /// Upper-case block name → block.
    pub blocks: HashMap<String, Block>,
    pub entities: Vec<Entity>,
}

impl Document {
    pub fn header_f(&self, name: &str, code: i32) -> Option<f64> {
        self.header
            .get(name)?
            .iter()
            .find(|p| p.code == code)
            .and_then(|p| number(&p.value))
    }

    pub fn layer(&self, name: &str) -> Option<&Layer> {
        self.layer_index
            .get(&name.to_uppercase())
            .map(|i| &self.layers[*i])
    }

    pub fn block(&self, name: &str) -> Option<&Block> {
        self.blocks.get(&name.to_uppercase())
    }
}

pub fn parse(bytes: &[u8]) -> Result<Document, String> {
    let pairs = if bytes.starts_with(b"AutoCAD Binary DXF") {
        binary_pairs(bytes)?
    } else {
        ascii_pairs(bytes)?
    };
    let records = records(pairs);
    if !records
        .iter()
        .any(|r| r.kind == "SECTION" || r.kind == "EOF")
    {
        return Err(tr("Keine gültige DXF-Datei").into());
    }
    Ok(document(records))
}

fn decode(bytes: &[u8]) -> String {
    let bytes = bytes.strip_prefix(b"\xEF\xBB\xBF").unwrap_or(bytes);
    match std::str::from_utf8(bytes) {
        Ok(text) => text.to_owned(),
        // Files before AutoCAD 2007 use the ANSI code page, mostly Windows-1252.
        Err(_) => bytes.iter().map(|b| windows_1252(*b)).collect(),
    }
}

fn windows_1252(byte: u8) -> char {
    const HIGH: [char; 32] = [
        '€', '\u{81}', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u{8D}', 'Ž',
        '\u{8F}', '\u{90}', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u{9D}',
        'ž', 'Ÿ',
    ];
    match byte {
        0x80..=0x9F => HIGH[(byte - 0x80) as usize],
        _ => byte as char,
    }
}

fn ascii_pairs(bytes: &[u8]) -> Result<Vec<Pair>, String> {
    let text = decode(bytes);
    let text = if text.contains('\n') {
        text
    } else {
        text.replace('\r', "\n")
    };
    let mut lines = text.lines().enumerate();
    let mut pairs = Vec::new();
    while let Some((number, line)) = lines.next() {
        let line = line.trim();
        if line.is_empty() {
            continue;
        }
        let Ok(code) = line.parse::<i32>() else {
            if pairs.is_empty() {
                return Err(tr("Keine gültige DXF-Datei").into());
            }
            return Err(tr!(
                "DXF-Datei ist beschädigt (Zeile {line})",
                line = number + 1
            ));
        };
        let Some((_, value)) = lines.next() else {
            break;
        };
        let value = value.strip_suffix('\r').unwrap_or(value);
        let value = if code == 1 || code == 3 {
            // Text keeps its leading spaces.
            value.trim_end().to_owned()
        } else {
            value.trim().to_owned()
        };
        let end = code == 0 && value == "EOF";
        pairs.push(Pair { code, value });
        if end {
            break;
        }
    }
    Ok(pairs)
}

enum Kind {
    Text,
    Double,
    I16,
    I32,
    I64,
    Bool,
    Binary,
}

fn value_kind(code: i32) -> Option<Kind> {
    Some(match code {
        0..=9
        | 100..=109
        | 300..=309
        | 320..=369
        | 390..=399
        | 410..=419
        | 430..=439
        | 470..=481
        | 999
        | 1000..=1003
        | 1005..=1009 => Kind::Text,
        10..=59 | 110..=149 | 210..=239 | 460..=469 | 1010..=1059 => Kind::Double,
        60..=79 | 170..=179 | 270..=289 | 370..=389 | 400..=409 | 1060..=1070 => Kind::I16,
        90..=99 | 420..=429 | 440..=459 | 1071 => Kind::I32,
        160..=169 => Kind::I64,
        290..=299 => Kind::Bool,
        310..=319 | 1004 => Kind::Binary,
        _ => return None,
    })
}

fn binary_pairs(bytes: &[u8]) -> Result<Vec<Pair>, String> {
    let broken = || tr("Binäre DXF-Datei ist beschädigt").to_string();
    // Sentinel "AutoCAD Binary DXF\r\n\x1a\0"; tolerate a lost '\r'.
    let start = bytes
        .iter()
        .take(26)
        .position(|b| *b == 0x1A)
        .filter(|i| bytes.get(i + 1) == Some(&0))
        .ok_or_else(broken)?
        + 2;
    // R12 writes one-byte group codes, R13 and later two bytes.
    let wide = bytes.get(start) == Some(&0) && bytes.get(start + 1) == Some(&0);
    let mut at = start;
    let take = |at: &mut usize, n: usize| -> Result<&[u8], String> {
        let slice = bytes.get(*at..*at + n).ok_or_else(broken)?;
        *at += n;
        Ok(slice)
    };
    let mut pairs = Vec::new();
    while at < bytes.len() {
        let code = if wide {
            i16::from_le_bytes(take(&mut at, 2)?.try_into().map_err(|_| broken())?) as i32
        } else {
            match take(&mut at, 1)?[0] {
                255 => {
                    i16::from_le_bytes(take(&mut at, 2)?.try_into().map_err(|_| broken())?) as i32
                }
                code => code as i32,
            }
        };
        let value = match value_kind(code).ok_or_else(broken)? {
            Kind::Text => {
                let len = bytes[at..]
                    .iter()
                    .position(|b| *b == 0)
                    .ok_or_else(broken)?;
                let text = decode(take(&mut at, len)?);
                at += 1;
                text
            }
            Kind::Double => f64::from_le_bytes(take(&mut at, 8)?.try_into().unwrap()).to_string(),
            Kind::I16 => i16::from_le_bytes(take(&mut at, 2)?.try_into().unwrap()).to_string(),
            Kind::I32 => i32::from_le_bytes(take(&mut at, 4)?.try_into().unwrap()).to_string(),
            Kind::I64 => i64::from_le_bytes(take(&mut at, 8)?.try_into().unwrap()).to_string(),
            Kind::Bool => take(&mut at, 1)?[0].to_string(),
            Kind::Binary => {
                let len = take(&mut at, 1)?[0] as usize;
                take(&mut at, len)?;
                String::new()
            }
        };
        let end = code == 0 && value == "EOF";
        pairs.push(Pair { code, value });
        if end {
            break;
        }
    }
    Ok(pairs)
}

struct Record {
    kind: String,
    pairs: Vec<Pair>,
}

fn records(pairs: Vec<Pair>) -> Vec<Record> {
    let mut records: Vec<Record> = Vec::new();
    for pair in pairs {
        if pair.code == 999 {
            continue;
        }
        if pair.code == 0 {
            records.push(Record {
                kind: pair.value.trim().to_uppercase(),
                pairs: Vec::new(),
            });
        } else if let Some(record) = records.last_mut() {
            record.pairs.push(pair);
        }
    }
    records
}

fn document(records: Vec<Record>) -> Document {
    let mut doc = Document::default();
    let mut section = String::new();
    let mut table = String::new();
    let mut block: Option<(String, Block, Vec<Record>)> = None;
    let mut entities = Vec::new();
    for record in records {
        match record.kind.as_str() {
            "SECTION" => {
                section = value(&record.pairs, 2).to_uppercase();
                if section == "HEADER" {
                    read_header(&mut doc, &record.pairs);
                }
                continue;
            }
            "ENDSEC" => {
                section.clear();
                continue;
            }
            "EOF" => break,
            _ => {}
        }
        match section.as_str() {
            "TABLES" => match record.kind.as_str() {
                "TABLE" => table = value(&record.pairs, 2).to_uppercase(),
                "ENDTAB" => table.clear(),
                "LAYER" if table == "LAYER" => read_layer(&mut doc, &record.pairs),
                "LTYPE" if table == "LTYPE" => read_linetype(&mut doc, &record.pairs),
                _ => {}
            },
            "BLOCKS" => match record.kind.as_str() {
                "BLOCK" => {
                    let name = value(&record.pairs, 2).to_owned();
                    let entity = Entity {
                        kind: record.kind,
                        pairs: record.pairs,
                        children: Vec::new(),
                    };
                    let base = [entity.fd(10, 0.0), entity.fd(20, 0.0)];
                    block = Some((
                        name,
                        Block {
                            base,
                            entities: Vec::new(),
                        },
                        Vec::new(),
                    ));
                }
                "ENDBLK" => {
                    if let Some((name, mut b, records)) = block.take() {
                        b.entities = group(records);
                        doc.blocks.insert(name.to_uppercase(), b);
                    }
                }
                _ => {
                    if let Some((_, _, records)) = block.as_mut() {
                        records.push(record);
                    }
                }
            },
            "ENTITIES" => entities.push(record),
            _ => {}
        }
    }
    doc.entities = group(entities);
    doc
}

fn value(pairs: &[Pair], code: i32) -> &str {
    pairs
        .iter()
        .find(|p| p.code == code)
        .map_or("", |p| p.value.as_str())
}

fn read_header(doc: &mut Document, pairs: &[Pair]) {
    let mut current: Option<String> = None;
    for pair in pairs {
        if pair.code == 9 {
            let name = pair.value.trim().to_uppercase();
            doc.header.entry(name.clone()).or_default();
            current = Some(name);
        } else if let Some(name) = &current {
            doc.header
                .entry(name.clone())
                .or_default()
                .push(pair.clone());
        }
    }
}

fn read_layer(doc: &mut Document, pairs: &[Pair]) {
    let entity = Entity {
        kind: "LAYER".into(),
        pairs: pairs.to_vec(),
        children: Vec::new(),
    };
    let name = super::text::unicode_escapes(entity.str(2).unwrap_or("0"));
    let layer = Layer {
        color: entity.int(62).unwrap_or(7),
        true_color: entity.int(420).map(|c| c as u32 & 0xFF_FFFF),
        linetype: entity.str(6).unwrap_or("CONTINUOUS").to_owned(),
        lineweight: entity.int(370).unwrap_or(-3),
        flags: entity.int(70).unwrap_or(0),
        name,
    };
    let key = layer.name.to_uppercase();
    match doc.layer_index.get(&key) {
        Some(index) => doc.layers[*index] = layer,
        None => {
            doc.layer_index.insert(key, doc.layers.len());
            doc.layers.push(layer);
        }
    }
}

fn read_linetype(doc: &mut Document, pairs: &[Pair]) {
    let name = value(pairs, 2).to_uppercase();
    let pattern: Vec<f64> = pairs
        .iter()
        .filter(|p| p.code == 49)
        .map(|p| number(&p.value).unwrap_or(0.0))
        .collect();
    doc.linetypes.insert(name, pattern);
}

/// Attaches VERTEX records to their POLYLINE and ATTRIB records to their
/// INSERT, dropping the closing SEQEND.
fn group(records: Vec<Record>) -> Vec<Entity> {
    let mut result: Vec<Entity> = Vec::new();
    let mut open = false;
    for record in records {
        let entity = Entity {
            kind: record.kind,
            pairs: record.pairs,
            children: Vec::new(),
        };
        let child_of = |parent: &Entity| match parent.kind.as_str() {
            "POLYLINE" => entity.kind == "VERTEX",
            "INSERT" => entity.kind == "ATTRIB",
            _ => false,
        };
        if open && let Some(parent) = result.last_mut() {
            if child_of(parent) {
                parent.children.push(entity);
                continue;
            }
            open = false;
            if entity.kind == "SEQEND" {
                continue;
            }
        }
        match entity.kind.as_str() {
            "SEQEND" | "VERTEX" | "ATTRIB" => continue,
            "POLYLINE" | "INSERT" => open = true,
            _ => {}
        }
        result.push(entity);
    }
    result
}
