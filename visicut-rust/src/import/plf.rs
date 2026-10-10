//! VisiCut project files (`.plf`), parametric SVG (`.psvg`,
//! `.parametric.svg`) and LaserScript (`.ls`).
//!
//! A PLF file is a ZIP archive written by VisiCut's
//! `VisicutModel.savePlfToStream`. Part `i` (counting from 0) stores its
//! files under the prefix `i/` (no prefix for part 0):
//!
//! - the original graphic file in any importable format,
//! - `transform.xml`: a `java.beans.XMLEncoder` `AffineTransform` mapping the
//!   file's own coordinates (SVG user units, image pixels, G-code mm, …) to
//!   millimetres on the laser bed,
//! - `mappings.xml`: the part's mapping (filters → VisiCut laser profiles),
//! - `<file>.parameters`: saved values of a parametric SVG.
//!
//! Material, thickness, laser settings and start point are not part of the
//! format. The parts are composed into one SVG in millimetres at their
//! positions on the bed. Mappings become processing steps (see `mappings`);
//! their laser settings stay at the defaults, because VisiCut keeps those
//! per device, material and thickness outside the PLF file.
mod laserscript;
mod mappings;
mod parametric;
mod script;
mod xml;

use super::{Imported, MAX_FILE_BYTES};
use crate::mapping::attributes;
use resvg::usvg;
use std::collections::{BTreeMap, HashSet};
use std::io::Read;
use std::path::{Path, PathBuf};

const MAX_ENTRIES: usize = 1000;
const MAX_TOTAL_BYTES: u64 = 100 * 1024 * 1024;
const PX_PER_MM: f64 = 96.0 / 25.4;

/// Affine matrix `[a, b, c, d, e, f]` as in SVG `matrix()` and Java's
/// `AffineTransform(double[6])`.
type Matrix = [f64; 6];

pub fn read_plf(path: &Path) -> Result<Imported, String> {
    let file = std::fs::File::open(path).map_err(|e| e.to_string())?;
    let mut archive = zip::ZipArchive::new(file)
        .map_err(|e| format!("PLF-Datei ist kein gültiges ZIP-Archiv: {e}"))?;
    if archive.len() > MAX_ENTRIES {
        return Err(format!("PLF-Datei enthält mehr als {MAX_ENTRIES} Einträge"));
    }
    let temp = TempDir::new()?;
    let mut parts: BTreeMap<u32, PartFiles> = BTreeMap::new();
    let mut total = 0u64;
    for i in 0..archive.len() {
        let mut entry = archive
            .by_index(i)
            .map_err(|e| format!("PLF-Datei ist beschädigt: {e}"))?;
        if entry.is_dir() {
            continue;
        }
        // VisiCut writes UTF-8 names without setting the ZIP's UTF-8 flag.
        let name = std::str::from_utf8(entry.name_raw())
            .map(str::to_string)
            .unwrap_or_else(|_| String::from_utf8_lossy(entry.name_raw()).into_owned());
        let mut bytes = Vec::new();
        (&mut entry)
            .take(MAX_FILE_BYTES + 1)
            .read_to_end(&mut bytes)
            .map_err(|e| format!("„{name}“ in der PLF-Datei ist nicht lesbar: {e}"))?;
        if bytes.len() as u64 > MAX_FILE_BYTES {
            return Err(format!("„{name}“ in der PLF-Datei ist größer als 25 MB"));
        }
        total += bytes.len() as u64;
        if total > MAX_TOTAL_BYTES {
            return Err("PLF-Datei ist entpackt größer als 100 MB".into());
        }
        let (index, local) = split_index(&name);
        let part = parts.entry(index).or_default();
        if local == "transform.xml" {
            part.transform = Some(parse_transform(&bytes));
        } else if local == "mappings.xml" {
            part.mapping = Some(bytes);
        } else {
            let directory = temp.0.join(index.to_string());
            std::fs::create_dir_all(&directory).map_err(|e| e.to_string())?;
            let file_name = sanitize(local);
            let target = directory.join(&file_name);
            std::fs::write(&target, &bytes).map_err(|e| e.to_string())?;
            // Parameter files only accompany a parametric SVG.
            if !file_name.to_ascii_lowercase().ends_with(".parameters") {
                let shown = local.rsplit(['/', '\\']).next().unwrap_or(local);
                part.source = Some((shown.to_string(), target));
            }
        }
    }

    let mut composer = Composer::default();
    let mut warnings = Vec::new();
    let any_mapping = parts.values().any(|part| part.mapping.is_some());
    let mut placed = Vec::new();
    for (index, part) in parts {
        let mapping = part.mapping.map(|bytes| mappings::parse(&bytes));
        let Some((name, file)) = part.source else {
            warnings.push(format!(
                "Teil {}: Grafikdatei fehlt in der PLF-Datei",
                index + 1
            ));
            continue;
        };
        if file
            .extension()
            .is_some_and(|e| e.eq_ignore_ascii_case("plf"))
        {
            warnings.push(format!(
                "Teil „{name}“ übersprungen: PLF-Dateien in PLF-Dateien werden nicht geöffnet"
            ));
            continue;
        }
        let imported = match super::read_file(&file) {
            Ok(imported) => imported,
            Err(error) => {
                warnings.push(format!("Teil „{name}“ übersprungen: {error}"));
                continue;
            }
        };
        warnings.extend(imported.warnings.iter().map(|w| format!("{name}: {w}")));
        let transform = match part.transform {
            Some(Ok(transform)) => Some(transform),
            Some(Err(error)) => {
                warnings.push(format!(
                    "Teil „{name}“: Position nicht lesbar ({error}); die Grafik liegt am Nullpunkt"
                ));
                None
            }
            None => {
                warnings.push(format!(
                    "Teil „{name}“: keine Position gespeichert; die Grafik liegt am Nullpunkt"
                ));
                None
            }
        };
        match composer.add(&name, &file, &imported.svg, transform) {
            Ok(()) => placed.push(mappings::Placed {
                // Same source as the composer parses (without DOCTYPE), with the original ids.
                objects: attributes(&xml::strip_doctype(&imported.svg)).ok(),
                svg: is_svg(&file),
                mapping,
                name,
            }),
            Err(error) => warnings.push(format!("Teil „{name}“ übersprungen: {error}")),
        }
    }
    if composer.parts.is_empty() {
        return Err(if warnings.is_empty() {
            "PLF-Datei enthält keine Grafik".into()
        } else {
            format!(
                "PLF-Datei enthält keine importierbare Grafik ({})",
                warnings.join("; ")
            )
        });
    }
    let (svg, more) = composer.finish()?;
    warnings.extend(more);
    let translation = if any_mapping {
        mappings::translate(&placed, attributes(&svg))
    } else {
        mappings::Translation::default()
    };
    warnings.extend(translation.warnings);
    Ok(Imported {
        svg,
        warnings,
        steps: translation.steps,
    })
}

/// Parts that VisiCutRust knows as SVG; their objects have the attributes VisiCut uses.
fn is_svg(file: &Path) -> bool {
    file.extension()
        .and_then(|e| e.to_str())
        .is_some_and(|e| e.eq_ignore_ascii_case("svg") || e.eq_ignore_ascii_case("psvg"))
}

pub fn read_parametric_svg(path: &Path) -> Result<Imported, String> {
    let source = std::fs::read_to_string(path).map_err(|e| e.to_string())?;
    let name = path
        .file_name()
        .unwrap_or_default()
        .to_string_lossy()
        .to_ascii_lowercase();
    // Like VisiCut, `.parametric.svg` files skip the PSVG translation.
    let psvg = !name.ends_with(".parametric.svg");
    let mut warnings = Vec::new();
    let mut saved = Vec::new();
    let mut parameter_file = path.as_os_str().to_owned();
    parameter_file.push(".parameters");
    let parameter_file = PathBuf::from(parameter_file);
    if parameter_file.is_file() {
        match std::fs::read_to_string(&parameter_file)
            .map_err(|e| e.to_string())
            .and_then(|text| parametric::read_saved_values(&text))
        {
            Ok(values) => saved = values,
            Err(error) => warnings.push(format!(
                "Gespeicherte Parameterwerte nicht lesbar ({error}); Standardwerte verwendet"
            )),
        }
    }
    let rendered = parametric::render(source, psvg, saved, script::TIME_LIMIT)
        .map_err(|e| format!("Parametrische SVG: {e}"))?;
    let embedded = crate::svg_import::embed_external_images(
        &rendered.svg,
        path.parent().unwrap_or(Path::new(".")),
    );
    warnings.extend(rendered.warnings);
    if !rendered.parameters.is_empty() {
        let list = |saved: bool| {
            rendered
                .parameters
                .iter()
                .filter(|p| p.saved == saved)
                .map(|p| format!("{} = {}", p.name, p.value.display()))
                .collect::<Vec<_>>()
                .join(", ")
        };
        let (defaults, stored) = (list(false), list(true));
        let mut text = String::from("Parametrische SVG:");
        if !defaults.is_empty() {
            text.push_str(&format!(" Standardwerte verwendet ({defaults})."));
        }
        if !stored.is_empty() {
            text.push_str(&format!(" Gespeicherte Werte verwendet ({stored})."));
        }
        text.push_str(" Andere Werte lassen sich in VisiCutRust noch nicht eingeben.");
        warnings.push(text);
    }
    warnings.extend(embedded.warnings);
    Ok(Imported {
        svg: embedded.svg,
        warnings,
        ..Default::default()
    })
}

pub fn read_laser_script(path: &Path) -> Result<Imported, String> {
    let bytes = std::fs::read(path).map_err(|e| e.to_string())?;
    let source = String::from_utf8_lossy(&bytes).into_owned();
    let drawing = laserscript::run(source, script::TIME_LIMIT)?;
    let (svg, mut warnings) = laserscript::to_svg(&drawing)?;
    warnings.extend(drawing.messages);
    Ok(Imported {
        svg,
        warnings,
        ..Default::default()
    })
}

#[derive(Default)]
struct PartFiles {
    source: Option<(String, PathBuf)>,
    transform: Option<Result<Matrix, String>>,
    /// Content of `mappings.xml`.
    mapping: Option<Vec<u8>>,
}

/// `"3/x.svg"` → `(3, "x.svg")`; entries without a number belong to part 0
/// (VisiCut only strips prefixes of parts after the first).
fn split_index(name: &str) -> (u32, &str) {
    if let Some((first, rest)) = name.split_once('/')
        && !first.is_empty()
        && first.chars().all(|c| c.is_ascii_digit())
        && let Ok(index) = first.parse::<u32>()
        && index > 0
    {
        return (index, rest);
    }
    (0, name)
}

/// File name for extraction: no directories, no unusual characters.
fn sanitize(name: &str) -> String {
    let base = name.rsplit(['/', '\\']).next().unwrap_or(name);
    let cleaned: String = base
        .chars()
        .map(|c| {
            if c.is_alphanumeric() || " -_.()+,".contains(c) {
                c
            } else {
                '_'
            }
        })
        .collect();
    let cleaned = cleaned.trim_start_matches('.').trim();
    let count = cleaned.chars().count();
    let cleaned: String = cleaned.chars().skip(count.saturating_sub(120)).collect();
    if cleaned.is_empty() {
        "teil".into()
    } else {
        cleaned
    }
}

/// Reads the six matrix values that VisiCut's XMLEncoder output lists as
/// `<void index="i"><double>v</double></void>` (omitted values are 0).
fn parse_transform(bytes: &[u8]) -> Result<Matrix, String> {
    let text = String::from_utf8_lossy(bytes);
    let document = roxmltree::Document::parse(&text).map_err(|e| e.to_string())?;
    let mut matrix = [0.0; 6];
    let mut found = false;
    for node in document.descendants().filter(|n| n.has_tag_name("void")) {
        let Some(index) = node
            .attribute("index")
            .and_then(|i| i.trim().parse::<usize>().ok())
            .filter(|i| *i < 6)
        else {
            continue;
        };
        let value = node
            .children()
            .find(|c| c.has_tag_name("double"))
            .and_then(|d| d.text())
            .and_then(|t| t.trim().parse::<f64>().ok());
        if let Some(value) = value {
            matrix[index] = value;
            found = true;
        }
    }
    let determinant = matrix[0] * matrix[3] - matrix[1] * matrix[2];
    if !found || matrix.iter().any(|v| !v.is_finite()) || determinant.abs() < 1e-12 {
        return Err("keine gültige Transformation".into());
    }
    Ok(matrix)
}

fn multiply(a: Matrix, b: Matrix) -> Matrix {
    [
        a[0] * b[0] + a[2] * b[1],
        a[1] * b[0] + a[3] * b[1],
        a[0] * b[2] + a[2] * b[3],
        a[1] * b[2] + a[3] * b[3],
        a[0] * b[4] + a[2] * b[5] + a[4],
        a[1] * b[4] + a[3] * b[5] + a[5],
    ]
}

fn scale(x: f64, y: f64) -> Matrix {
    [x, 0.0, 0.0, y, 0.0, 0.0]
}

/// Coordinate system of an imported SVG's root element.
struct Frame {
    view_box: [f64; 4],
    width_mm: f64,
    height_mm: f64,
}

impl Frame {
    fn of(root: roxmltree::Node) -> Self {
        let view_box = root.attribute("viewBox").and_then(|v| {
            let numbers: Vec<f64> = v
                .split(|c: char| c.is_whitespace() || c == ',')
                .filter(|s| !s.is_empty())
                .filter_map(|s| s.parse().ok())
                .collect();
            (numbers.len() == 4 && numbers[2] > 0.0 && numbers[3] > 0.0)
                .then(|| [numbers[0], numbers[1], numbers[2], numbers[3]])
        });
        let width = root.attribute("width").and_then(length_px);
        let height = root.attribute("height").and_then(length_px);
        let view_box =
            view_box.unwrap_or_else(|| [0.0, 0.0, width.unwrap_or(100.0), height.unwrap_or(100.0)]);
        Frame {
            view_box,
            width_mm: width.unwrap_or(view_box[2]) / PX_PER_MM,
            height_mm: height.unwrap_or(view_box[3]) / PX_PER_MM,
        }
    }

    /// User units → millimetres of the document placed at the origin.
    fn physical(&self) -> Matrix {
        let [x, y, w, h] = self.view_box;
        let (sx, sy) = (self.width_mm / w, self.height_mm / h);
        [sx, 0.0, 0.0, sy, -x * sx, -y * sy]
    }

    /// User units → the coordinates VisiCut's importer for `file` uses, which
    /// `transform.xml` refers to.
    fn source_coordinates(&self, file: &Path) -> Matrix {
        let name = file.to_string_lossy().to_ascii_lowercase();
        let extension = name.rsplit('.').next().unwrap_or_default();
        let [x, y, w, h] = self.view_box;
        match extension {
            // SVG user units; LaserScript output uses millimetres as user units.
            "svg" | "psvg" | "ls" => [1.0, 0.0, 0.0, 1.0, 0.0, 0.0],
            // Image pixels.
            "png" | "jpg" | "jpeg" | "bmp" | "gif" => match image::image_dimensions(file) {
                Ok((pw, ph)) => {
                    let (sx, sy) = (pw as f64 / w, ph as f64 / h);
                    [sx, 0.0, 0.0, sy, -x * sx, -y * sy]
                }
                Err(_) => multiply(scale(72.0 / 25.4, 72.0 / 25.4), self.physical()),
            },
            // PostScript points of the cropped page.
            "eps" | "ps" => multiply(scale(72.0 / 25.4, 72.0 / 25.4), self.physical()),
            // Absolute millimetres (DXF: 1 unit = 1 mm in VisiCut).
            "nc" | "gcode" | "dxf" => scale(self.width_mm / w, self.height_mm / h),
            _ => self.physical(),
        }
    }
}

/// An SVG length in px (96 dpi); `None` for percentages and invalid values.
fn length_px(text: &str) -> Option<f64> {
    let text = text.trim();
    let split = text
        .find(|c: char| c.is_ascii_alphabetic() || c == '%')
        .unwrap_or(text.len());
    let value: f64 = text[..split].trim().parse().ok()?;
    let factor = match &text[split..] {
        "" | "px" => 1.0,
        "mm" => PX_PER_MM,
        "cm" => PX_PER_MM * 10.0,
        "in" => 96.0,
        "pt" => 96.0 / 72.0,
        "pc" => 16.0,
        "em" => 16.0,
        "ex" => 8.0,
        _ => return None,
    };
    (value > 0.0).then_some(value * factor)
}

#[derive(Default)]
struct Composer {
    parts: Vec<String>,
    ids: HashSet<String>,
}

impl Composer {
    /// Adds a part's SVG as a nested `<svg>` that keeps its user units,
    /// inside a `<g>` carrying the part's transform to millimetres.
    fn add(
        &mut self,
        name: &str,
        file: &Path,
        svg: &str,
        transform: Option<Matrix>,
    ) -> Result<(), String> {
        let number = self.parts.len() + 1;
        let group_id = format!("plf-teil-{number}");
        let source = xml::strip_doctype(svg);
        let source = self.unique_ids(&source, number)?;
        let parsed = roxmltree::Document::parse(&source)
            .map_err(|e| format!("SVG konnte nicht gelesen werden: {e}"))?;
        let root = parsed.root_element();
        let frame = Frame::of(root);
        let matrix = match transform {
            Some(transform) => multiply(transform, frame.source_coordinates(file)),
            None => frame.physical(),
        };
        let start = root.range().start;
        let tag_end = xml::start_tag_end(&source, start);
        let replaced = [
            "x",
            "y",
            "width",
            "height",
            "viewBox",
            "preserveAspectRatio",
            "overflow",
        ];
        let removed: Vec<_> = root
            .attributes()
            .filter(|a| a.namespace().is_none() && replaced.contains(&a.name()))
            .map(|a| a.range())
            .collect();
        let [x, y, w, h] = frame.view_box;
        // With x/y/width/height equal to the viewBox the nested viewport maps
        // user units 1:1; overflow="visible" avoids a clip path.
        let added: Vec<(String, String)> = [
            ("x", format!("{x}")),
            ("y", format!("{y}")),
            ("width", format!("{w}")),
            ("height", format!("{h}")),
            ("viewBox", format!("{x} {y} {w} {h}")),
            ("preserveAspectRatio", "none".into()),
            ("overflow", "visible".into()),
        ]
        .into_iter()
        .map(|(n, v)| (n.to_string(), v))
        .collect();
        let tag = xml::rewrite_start_tag(&source, start, tag_end, &removed, &added);
        let nested = format!("{tag}{}", &source[tag_end..root.range().end]);
        let matrix = matrix.map(|v| v.to_string()).join(" ");
        let group = format!(
            "<g id=\"{group_id}\" inkscape:label=\"{}\" transform=\"matrix({matrix})\">\n{nested}\n</g>",
            xml::escape_attribute(name)
        );
        usvg::Tree::from_str(&document(1.0, 1.0, None, &group), &crate::svg::options())
            .map_err(|e| format!("SVG konnte nicht gelesen werden: {e}"))?;
        self.parts.push(group);
        self.ids.insert(group_id);
        Ok(())
    }

    /// Prefixes all ids of a part (and references to them) when one of them
    /// is already used by an earlier part, e.g. when a part was duplicated.
    fn unique_ids(&mut self, source: &str, number: usize) -> Result<String, String> {
        let document = roxmltree::Document::parse(source)
            .map_err(|e| format!("SVG konnte nicht gelesen werden: {e}"))?;
        let ids: HashSet<String> = document
            .descendants()
            .filter_map(|n| n.attribute("id"))
            .map(str::to_string)
            .collect();
        let collides = ids
            .iter()
            .any(|id| self.ids.contains(id) || id.starts_with("plf-teil-"));
        if !collides {
            self.ids.extend(ids);
            return Ok(source.to_string());
        }
        let prefix = format!("teil{number}-");
        let mut edits = Vec::new();
        for node in document.descendants().filter(|n| n.is_element()) {
            for attribute in node.attributes() {
                let value = attribute.value();
                let local = attribute.namespace().is_none();
                let replacement = if attribute.name() == "id" && local {
                    Some(format!("{prefix}{value}"))
                } else if attribute.name() == "href"
                    && (local || attribute.namespace() == Some("http://www.w3.org/1999/xlink"))
                    && value.strip_prefix('#').is_some_and(|id| ids.contains(id))
                {
                    Some(format!("#{prefix}{}", &value[1..]))
                } else {
                    prefix_urls(value, &ids, &prefix)
                };
                if let Some(replacement) = replacement {
                    edits.push((attribute.range_value(), xml::escape_attribute(&replacement)));
                }
            }
            if node.tag_name().name() == "style" {
                for text in node.children().filter(|c| c.is_text()) {
                    if let Some(replacement) =
                        prefix_urls(text.text().unwrap_or_default(), &ids, &prefix)
                    {
                        edits.push((text.range(), xml::escape_text(&replacement)));
                    }
                }
            }
        }
        self.ids
            .extend(ids.iter().map(|id| format!("{prefix}{id}")));
        Ok(xml::apply_edits(source, 0, edits))
    }

    /// The composed document, sized to reach from the bed origin to the
    /// furthest part. Parts left of or above the origin move it.
    fn finish(&self) -> Result<(String, Vec<String>), String> {
        let body = self.parts.join("\n");
        let probe = document(1.0, 1.0, None, &body);
        let tree = usvg::Tree::from_str(&probe, &crate::svg::options())
            .map_err(|e| format!("PLF-Grafik konnte nicht zusammengesetzt werden: {e}"))?;
        // The probe's 1 × 1 viewBox spans 1 mm; usvg reports pixels.
        let px = tree.size().width() as f64;
        let bounds = tree.root().abs_bounding_box();
        let stroke = tree.root().abs_stroke_bounding_box();
        let mut warnings = Vec::new();
        let shift_x = -(bounds.left() as f64 / px).min(0.0);
        let shift_y = -(bounds.top() as f64 / px).min(0.0);
        let shift = (shift_x > 1e-3 || shift_y > 1e-3).then(|| {
            warnings.push(format!(
                "Teile lagen links oder oberhalb der Arbeitsfläche; alles wurde um {:.1} × {:.1} mm verschoben",
                shift_x, shift_y
            ));
            (shift_x, shift_y)
        });
        let (sx, sy) = shift.unwrap_or_default();
        let width = (stroke.right() as f64 / px + sx).max(1.0);
        let height = (stroke.bottom() as f64 / px + sy).max(1.0);
        let round_up = |v: f64| (v * 1000.0).ceil() / 1000.0;
        Ok((
            document(round_up(width), round_up(height), shift, &body),
            warnings,
        ))
    }
}

fn document(width: f64, height: f64, shift: Option<(f64, f64)>, body: &str) -> String {
    let (open, close) = match shift {
        Some((x, y)) => (format!("<g transform=\"translate({x} {y})\">\n"), "</g>\n"),
        None => (String::new(), ""),
    };
    format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\" width=\"{width}mm\" height=\"{height}mm\" viewBox=\"0 0 {width} {height}\">\n{open}{body}\n{close}</svg>\n"
    )
}

/// Prefixes `#id` inside `url(…)` references to known ids.
fn prefix_urls(value: &str, ids: &HashSet<String>, prefix: &str) -> Option<String> {
    if !value.contains("url(") {
        return None;
    }
    let mut result = String::new();
    let mut rest = value;
    let mut changed = false;
    while let Some(index) = rest.find("url(") {
        let after = index + 4;
        result.push_str(&rest[..after]);
        rest = &rest[after..];
        let quote_len = rest.len() - rest.trim_start_matches(['"', '\'', ' ']).len();
        result.push_str(&rest[..quote_len]);
        rest = &rest[quote_len..];
        if let Some(target) = rest.strip_prefix('#') {
            let end = target.find([')', '"', '\'', ' ']).unwrap_or(target.len());
            if ids.contains(&target[..end]) {
                result.push('#');
                result.push_str(prefix);
                rest = target;
                changed = true;
            }
        }
    }
    result.push_str(rest);
    changed.then_some(result)
}

/// Extraction directory, removed when the import is done.
struct TempDir(PathBuf);

impl TempDir {
    fn new() -> Result<Self, String> {
        use std::sync::atomic::{AtomicU64, Ordering};
        static COUNTER: AtomicU64 = AtomicU64::new(0);
        loop {
            let count = COUNTER.fetch_add(1, Ordering::Relaxed);
            let path =
                std::env::temp_dir().join(format!("visicut-plf-{}-{count}", std::process::id()));
            match std::fs::create_dir(&path) {
                Ok(()) => return Ok(Self(path)),
                Err(e) if e.kind() == std::io::ErrorKind::AlreadyExists => {}
                Err(e) => {
                    return Err(format!(
                        "Temporärer Ordner konnte nicht angelegt werden: {e}"
                    ));
                }
            }
        }
    }
}

impl Drop for TempDir {
    fn drop(&mut self) {
        let _ = std::fs::remove_dir_all(&self.0);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::mapping::{Attribute, Filter};
    use crate::project::{Operation, Project};
    use std::io::Write;

    /// Minimal VisiCut mapping: red → cut; an unknown filter attribute → skipped.
    const MAPPINGS: &[u8] = include_bytes!("plf/testdata/zuordnung.xml");

    fn transform_xml(m: Matrix) -> String {
        let mut entries = String::new();
        for (index, value) in m.iter().enumerate() {
            if *value != 0.0 {
                entries.push_str(&format!(
                    "   <void index=\"{index}\"> \n    <double>{value:?}</double> \n   </void> \n"
                ));
            }
        }
        format!(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<java version=\"1.6.0_31\" class=\"java.beans.XMLDecoder\"> \n <object class=\"java.awt.geom.AffineTransform\"> \n  <array class=\"double\" length=\"6\"> \n{entries}  </array> \n </object> \n</java> \n"
        )
    }

    fn write_plf(entries: &[(&str, &[u8])]) -> (tempdir::Dir, PathBuf) {
        let dir = tempdir::Dir::new();
        let path = dir.0.join("test.plf");
        let file = std::fs::File::create(&path).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        for (name, bytes) in entries {
            zip.start_file(*name, zip::write::SimpleFileOptions::default())
                .unwrap();
            zip.write_all(bytes).unwrap();
        }
        zip.finish().unwrap();
        (dir, path)
    }

    mod tempdir {
        pub struct Dir(pub std::path::PathBuf);
        impl Dir {
            pub fn new() -> Self {
                Dir(super::TempDir::new().unwrap().keep())
            }
        }
        impl Drop for Dir {
            fn drop(&mut self) {
                let _ = std::fs::remove_dir_all(&self.0);
            }
        }
    }

    impl TempDir {
        fn keep(self) -> PathBuf {
            let path = self.0.clone();
            std::mem::forget(self);
            path
        }
    }

    fn bounds(svg: &str) -> [f32; 4] {
        let preview = crate::svg::render(svg).unwrap();
        let project = Project {
            svg: svg.into(),
            x_mm: 0.0,
            y_mm: 0.0,
            width_mm: preview.width_mm,
            height_mm: preview.height_mm,
            ..Project::default()
        };
        let contours = crate::geometry::contours(&project).unwrap();
        let mut b = [f32::MAX, f32::MAX, f32::MIN, f32::MIN];
        for point in contours.iter().flatten() {
            b = [
                b[0].min(point[0]),
                b[1].min(point[1]),
                b[2].max(point[0]),
                b[3].max(point[1]),
            ];
        }
        b
    }

    fn contour_bounds(svg: &str) -> Vec<[f32; 4]> {
        let preview = crate::svg::render(svg).unwrap();
        let project = Project {
            svg: svg.into(),
            x_mm: 0.0,
            y_mm: 0.0,
            width_mm: preview.width_mm,
            height_mm: preview.height_mm,
            ..Project::default()
        };
        crate::geometry::contours(&project)
            .unwrap()
            .iter()
            .map(|c| {
                c.iter()
                    .fold([f32::MAX, f32::MAX, f32::MIN, f32::MIN], |b, p| {
                        [
                            b[0].min(p[0]),
                            b[1].min(p[1]),
                            b[2].max(p[0]),
                            b[3].max(p[1]),
                        ]
                    })
            })
            .collect()
    }

    fn close(a: [f32; 4], b: [f32; 4]) -> bool {
        a.iter().zip(b).all(|(x, y)| (x - y).abs() < 0.01)
    }

    #[test]
    fn places_plf_parts_in_millimetres() {
        // Part 0: 100 × 100 user units on a 50 mm page, moved by VisiCut.
        let first = br##"<svg xmlns="http://www.w3.org/2000/svg" width="50mm" height="50mm" viewBox="0 0 100 100"><rect id="r" x="10" y="10" width="20" height="20" fill="none" stroke="#f00"/></svg>"##;
        // Part 1: px units, same ids (a duplicated part), rotated by 90°.
        let second = br##"<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="40" height="40"><defs><path id="r" d="M0 0 H10 V5 H0 Z"/></defs><use xlink:href="#r" fill="none" stroke="#00f"/></svg>"##;
        // Java stores SVG user units → mm: scale 0.5 plus a move of 20/30 mm.
        let t0 = transform_xml([0.5, 0.0, 0.0, 0.5, 20.0, 30.0]);
        // Rotation by 90° (x' = -y + 100, y' = x + 10), 1 user unit = 1 mm.
        let t1 = transform_xml([0.0, 1.0, -1.0, 0.0, 100.0, 10.0]);
        let (_dir, path) = write_plf(&[
            ("Teil eins.svg", first),
            ("transform.xml", t0.as_bytes()),
            ("mappings.xml", b"<mapping/>"),
            ("1/zwei.svg", second),
            ("1/transform.xml", t1.as_bytes()),
        ]);
        let imported = read_plf(&path).unwrap();
        let svg = &imported.svg;
        assert!(svg.contains("inkscape:label=\"Teil eins.svg\""), "{svg}");
        assert!(svg.contains("id=\"teil2-r\""), "{svg}");
        assert!(svg.contains("xlink:href=\"#teil2-r\""), "{svg}");
        // `<mapping/>` is not a mapping set: reported, geometry unaffected.
        assert!(
            imported.warnings.iter().any(|w| w.contains("nicht lesbar")),
            "{:?}",
            imported.warnings
        );
        assert!(imported.steps.len() == 1 && imported.steps[0].objects.is_empty());
        let mut parts = contour_bounds(svg);
        parts.sort_by(|a, b| a[0].total_cmp(&b[0]));
        // Part 0: rect 10..30 user units → 25..35 mm, 35..45 mm.
        assert!(close(parts[0], [25.0, 35.0, 35.0, 45.0]), "{parts:?}");
        // Part 1: x' = 100 - y (y 0..5) → 95..100, y' = 10 + x (x 0..10) → 10..20.
        assert!(close(parts[1], [95.0, 10.0, 100.0, 20.0]), "{parts:?}");
        let preview = crate::svg::render(svg).unwrap();
        assert!(
            preview.width_mm >= 100.0 && preview.width_mm < 101.0,
            "{} {svg}",
            preview.width_mm
        );
        assert!(preview.height_mm >= 45.0 && preview.height_mm < 46.0);
    }

    /// Two rectangles, red and blue, in millimetres.
    const TWO_RECTS: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="50mm" viewBox="0 0 100 50"><rect x="10" y="10" width="20" height="20" fill="none" stroke="#ff0000"/><rect x="50" y="10" width="20" height="20" fill="none" stroke="#0000ff"/></svg>"##;

    fn one_mapping(filter: &str, profile: &str) -> String {
        format!(
            r#"<com.t_oster.visicut.model.mapping.MappingSet><linked-list><default/><int>1</int><mapping>{}<b class="vectorProfile"><DPI>500.0</DPI>{profile}</b></mapping></linked-list></com.t_oster.visicut.model.mapping.MappingSet>"#,
            if filter.is_empty() {
                String::new()
            } else {
                format!(
                    r#"<a class="filters"><linked-list><default/><int>1</int>{filter}</linked-list></a>"#
                )
            }
        )
    }

    fn stroke_filter(hex_rgb: [u8; 3]) -> String {
        let [r, g, b] = hex_rgb;
        format!(
            r#"<filter><inverted>false</inverted><attribute>Stroke Color</attribute><value class="awt-color"><red>{r}</red><green>{g}</green><blue>{b}</blue><alpha>255</alpha></value></filter>"#
        )
    }

    #[test]
    fn maps_colour_filters_and_keeps_geometry() {
        let transform = transform_xml([1.0, 0.0, 0.0, 1.0, 0.0, 0.0]);
        let (_mapped_dir, mapped) = write_plf(&[
            ("a.svg", TWO_RECTS),
            ("transform.xml", transform.as_bytes()),
            ("mappings.xml", MAPPINGS),
        ]);
        let (_plain_dir, plain) = write_plf(&[
            ("a.svg", TWO_RECTS),
            ("transform.xml", transform.as_bytes()),
        ]);
        let with = read_plf(&mapped).unwrap();
        let without = read_plf(&plain).unwrap();
        // The geometry is the same with and without the mapping.
        assert_eq!(with.svg, without.svg);
        // The red rectangle is cut; the unknown filter's entry produces no step.
        assert_eq!(with.steps.len(), 1, "{:?}", with.steps);
        let step = &with.steps[0];
        assert_eq!(step.operation, Operation::Cut);
        assert_eq!(
            step.filters,
            Some(vec![Filter {
                attribute: Attribute::StrokeColor,
                value: "#ff0000".into(),
                compare: false,
                inverted: false,
            }])
        );
        assert!(step.objects.is_empty());
        // Laser settings are not in the file: defaults, reported.
        assert_eq!(
            (step.power_percent, step.speed_percent, step.passes),
            (20.0, 100.0, 1)
        );
        assert!(
            with.warnings
                .iter()
                .any(|w| w.starts_with("Zuordnungen übernommen: 1 ")),
            "{:?}",
            with.warnings
        );
        assert!(with.warnings.iter().any(|w| w.contains("Standardwerte")));
        // The unknown filter is reported.
        assert!(
            with.warnings
                .iter()
                .any(|w| w.contains("Gravur-Ebene") && w.contains("nicht bekannt")),
            "{:?}",
            with.warnings
        );
        assert!(without.steps.is_empty() && without.warnings.is_empty());
    }

    #[test]
    fn rest_and_ignore_follow_visicut() {
        let svg = br##"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="50mm" viewBox="0 0 100 50"><rect x="5" y="5" width="10" height="10" fill="none" stroke="#ff0000"/><rect x="20" y="5" width="10" height="10" fill="none" stroke="#0000ff"/><rect x="35" y="5" width="10" height="10" fill="none" stroke="#00ff00"/></svg>"##;
        // red → cut, blue → ignored, everything else → mark (the rest).
        let set = format!(
            r#"<com.t_oster.visicut.model.mapping.MappingSet><linked-list><default/><int>3</int><mapping><a class="filters"><linked-list><default/><int>1</int>{}</linked-list></a><b class="vectorProfile"><DPI>500.0</DPI><isCut>true</isCut></b></mapping><mapping><a class="filters"><linked-list><default/><int>1</int>{}</linked-list></a></mapping><mapping><b class="vectorProfile"><DPI>500.0</DPI><isCut>false</isCut></b></mapping></linked-list></com.t_oster.visicut.model.mapping.MappingSet>"#,
            stroke_filter([255, 0, 0]),
            stroke_filter([0, 0, 255])
        );
        let transform = transform_xml([1.0, 0.0, 0.0, 1.0, 0.0, 0.0]);
        let (_dir, path) = write_plf(&[
            ("a.svg", svg),
            ("transform.xml", transform.as_bytes()),
            ("mappings.xml", set.as_bytes()),
        ]);
        let imported = read_plf(&path).unwrap();
        assert_eq!(imported.steps.len(), 2, "{:?}", imported.steps);
        assert_eq!(imported.steps[0].operation, Operation::Cut);
        assert!(imported.steps[0].filters.is_some());
        // Only the green rectangle (index 2) is left for the rest step.
        assert_eq!(imported.steps[1].operation, Operation::Mark);
        assert_eq!(imported.steps[1].objects, vec![2]);
        assert!(imported.steps[1].filters.is_none());
    }

    #[test]
    fn colour_rules_of_one_part_do_not_select_other_parts() {
        // Part 1 maps red to cut; part 2 also has a red rectangle and no mapping.
        let transform = transform_xml([1.0, 0.0, 0.0, 1.0, 0.0, 0.0]);
        let set = one_mapping(&stroke_filter([255, 0, 0]), r#"<isCut>true</isCut>"#);
        let (_dir, path) = write_plf(&[
            ("a.svg", TWO_RECTS),
            ("transform.xml", transform.as_bytes()),
            ("mappings.xml", set.as_bytes()),
            ("1/b.svg", TWO_RECTS),
            ("1/transform.xml", transform.as_bytes()),
        ]);
        let imported = read_plf(&path).unwrap();
        // Objects: a.svg red (0), a.svg blue (1), b.svg red (2), b.svg blue (3).
        assert_eq!(imported.steps.len(), 1, "{:?}", imported.steps);
        assert_eq!(imported.steps[0].operation, Operation::Cut);
        assert!(imported.steps[0].filters.is_none());
        assert_eq!(imported.steps[0].objects, vec![0]);
        assert!(
            imported
                .warnings
                .iter()
                .any(|w| w.contains("b.svg") && w.contains("keine Zuordnung")),
            "{:?}",
            imported.warnings
        );
    }

    #[test]
    fn unreadable_mapping_cuts_nothing() {
        let transform = transform_xml([1.0, 0.0, 0.0, 1.0, 0.0, 0.0]);
        let (_dir, path) = write_plf(&[
            ("a.svg", TWO_RECTS),
            ("transform.xml", transform.as_bytes()),
            ("mappings.xml", b"<wrong/>"),
        ]);
        let imported = read_plf(&path).unwrap();
        // A step without objects: the whole motif is not processed by default.
        assert_eq!(imported.steps.len(), 1);
        assert!(imported.steps[0].objects.is_empty() && imported.steps[0].filters.is_none());
        assert!(imported.warnings.iter().any(|w| w.contains("nicht lesbar")));
        assert!(
            imported
                .warnings
                .iter()
                .any(|w| w.contains("Keine Zuordnung übernommen"))
        );
    }

    #[test]
    fn skips_parts_whose_format_is_not_available() {
        let svg = br#"<svg xmlns="http://www.w3.org/2000/svg" width="10mm" height="10mm" viewBox="0 0 10 10"><path d="M1 1 H9 V9 Z" stroke="black" fill="none"/></svg>"#;
        let t = transform_xml([1.0, 0.0, 0.0, 1.0, 5.0, 5.0]);
        let (_dir, path) = write_plf(&[
            ("a.svg", svg),
            ("transform.xml", t.as_bytes()),
            (
                "1/zeichnung.dxf",
                b"0\nSECTION\n2\nENTITIES\n0\nENDSEC\n0\nEOF\n",
            ),
            ("1/transform.xml", t.as_bytes()),
        ]);
        // Whether or not the DXF importer exists yet, the SVG part is kept and
        // the DXF part either joins it or is reported.
        let imported = read_plf(&path).unwrap();
        let reported = imported
            .warnings
            .iter()
            .any(|w| w.contains("Teil „zeichnung.dxf“ übersprungen"));
        assert!(
            reported || imported.svg.contains("inkscape:label=\"zeichnung.dxf\""),
            "{:?}",
            imported.warnings
        );
        assert!(
            close(bounds(&imported.svg), [6.0, 6.0, 14.0, 14.0]),
            "{}",
            imported.svg
        );
    }

    #[test]
    fn rejects_broken_or_empty_plf_files() {
        let dir = tempdir::Dir::new();
        let path = dir.0.join("kaputt.plf");
        std::fs::write(&path, b"kein zip").unwrap();
        assert!(read_plf(&path).err().unwrap().contains("ZIP"));
        let (_dir, path) = write_plf(&[("transform.xml", b"<java/>")]);
        assert!(read_plf(&path).is_err());
    }

    #[test]
    fn reads_parametric_parts_with_saved_values() {
        let svg = br#"<svg xmlns="http://www.w3.org/2000/svg" xmlns:th="http://www.thymeleaf.org" width="100mm" height="100mm" viewBox="0 0 100 100"><defs><ref param="size" type="Double" default="10"/></defs><rect x="0" y="0" height="5" fill="none" stroke="black" th:attr="width=${size}"/></svg>"#;
        let parameters = b"<parameters>\n  <entry>\n    <string>size</string>\n    <double>30.0</double>\n  </entry>\n</parameters>";
        let t = transform_xml([1.0, 0.0, 0.0, 1.0, 0.0, 0.0]);
        let (_dir, path) = write_plf(&[
            ("box.parametric.svg", svg),
            ("box.parametric.svg.parameters", parameters),
            ("transform.xml", t.as_bytes()),
        ]);
        let imported = read_plf(&path).unwrap();
        assert!(
            close(bounds(&imported.svg), [0.0, 0.0, 30.0, 5.0]),
            "{}",
            imported.svg
        );
        assert!(imported.warnings.iter().any(|w| w.contains("size = 30")));
    }

    #[test]
    fn reads_the_bundled_example() {
        let path = Path::new(env!("CARGO_MANIFEST_DIR"))
            .join("../distribute/files/examples/fablab-schluesselanhaenger.plf");
        if !path.exists() {
            return;
        }
        let imported = read_plf(&path).unwrap();
        assert!(imported.svg.contains("Schlu"), "{}", &imported.svg[..300]);
        // Only the green line mapping matches; the others match nothing in VisiCut
        // (a filter set with three fill colours needs one object with all three,
        // and the stroke width is stored as text).
        assert_eq!(imported.steps.len(), 1, "{:?}", imported.steps);
        assert_eq!(imported.steps[0].operation, Operation::Mark);
        assert_eq!(
            imported.steps[0].filters.as_deref().map(<[_]>::len),
            Some(1)
        );
        assert!(
            imported
                .warnings
                .iter()
                .any(|w| w.contains("NEAREST") && w.contains("nicht übernommen")),
            "{:?}",
            imported.warnings
        );
        let preview = crate::svg::render(&imported.svg).unwrap();
        // VisiCut placed the keyring (about 40 × 37 mm) at the bed's top left corner.
        assert!(
            (35.0..50.0).contains(&preview.width_mm),
            "{}",
            preview.width_mm
        );
        assert!(
            (30.0..45.0).contains(&preview.height_mm),
            "{}",
            preview.height_mm
        );
    }

    #[test]
    fn imports_parametric_svg_with_defaults() {
        let dir = tempdir::Dir::new();
        let path = dir.0.join("kiste.psvg");
        std::fs::write(
            &path,
            r#"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="100mm" viewBox="0 0 100 100"><defs><ref param="$breite" default="20" label="Breite"/></defs><rect x="10" y="10" width="{$breite * 2}" height="{$breite}" fill="none" stroke="black"/></svg>"#,
        )
        .unwrap();
        let imported = crate::import::read_file(&path).unwrap();
        assert!(
            close(bounds(&imported.svg), [10.0, 10.0, 50.0, 30.0]),
            "{}",
            imported.svg
        );
        let warning = imported
            .warnings
            .iter()
            .find(|w| w.contains("Standardwerte"))
            .unwrap();
        assert!(warning.contains("breite = 20"), "{warning}");
    }

    #[test]
    fn imports_laser_script_square() {
        let dir = tempdir::Dir::new();
        let path = dir.0.join("quadrat.ls");
        std::fs::write(
            &path,
            "function rect(x, y, s) { move(x, y); line(x + s, y); line(x + s, y + s); line(x, y + s); line(x, y); }\nrect(10, 20, 30);\n",
        )
        .unwrap();
        let imported = crate::import::read_file(&path).unwrap();
        assert!(
            close(bounds(&imported.svg), [10.0, 20.0, 40.0, 50.0]),
            "{}",
            imported.svg
        );
        let preview = crate::svg::render(&imported.svg).unwrap();
        assert!((preview.width_mm - 40.0).abs() < 0.01);
        assert!((preview.height_mm - 50.0).abs() < 0.01);
    }

    #[test]
    fn aborts_endless_laser_scripts() {
        let error = laserscript::run(
            "move(0, 0); while (true) { line(1, 1); move(0, 0); }".into(),
            std::time::Duration::from_millis(500),
        )
        .unwrap_err();
        assert!(error.contains("abgebrochen"), "{error}");
        let error = laserscript::run(
            "var i = 0; for (;;) { i++; }".into(),
            std::time::Duration::from_millis(500),
        )
        .unwrap_err();
        assert!(error.contains("abgebrochen"), "{error}");
    }

    #[test]
    fn transforms_compose_like_java() {
        let t = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0];
        assert_eq!(multiply(t, [1.0, 0.0, 0.0, 1.0, 0.0, 0.0]), t);
        assert_eq!(
            multiply(scale(2.0, 3.0), [1.0, 0.0, 0.0, 1.0, 1.0, 1.0]),
            [2.0, 0.0, 0.0, 3.0, 2.0, 3.0]
        );
        assert_eq!(parse_transform(transform_xml(t).as_bytes()).unwrap(), t);
        assert!(parse_transform(b"<java/>").is_err());
        assert_eq!(split_index("12/a/b.svg"), (12, "a/b.svg"));
        assert_eq!(split_index("a.svg"), (0, "a.svg"));
        assert_eq!(sanitize("../../etc/pass wd"), "pass wd");
        assert_eq!(sanitize(".."), "teil");
    }
}

#[cfg(test)]
mod example_tests {
    use std::path::Path;

    #[test]
    fn imports_bundled_parametric_and_script_examples() {
        let base = Path::new(env!("CARGO_MANIFEST_DIR")).join("../distribute/files/examples");
        if !base.exists() {
            return;
        }
        for name in [
            "Parametric/CableHolder.parametric.svg",
            "Parametric/CandleHolder.parametric.svg",
            "Parametric/Card.parametric.svg",
            "Parametric/Smiley.parametric.svg",
            "Laser-Script/LaserScriptExample.ls",
            "Laser-Script/LaserScriptFocustest.ls",
            "Laser-Script/LaserScriptSpeedtest.ls",
        ] {
            let imported = crate::import::read_file(&base.join(name))
                .unwrap_or_else(|e| panic!("{name}: {e}"));
            let preview =
                crate::svg::render(&imported.svg).unwrap_or_else(|e| panic!("{name}: {e}"));
            assert!(
                preview.width_mm > 50.0 && preview.height_mm > 50.0,
                "{name}"
            );
            for attribute in [" th:attr=", " th:each=", " th:if=", " th:text="] {
                assert!(!imported.svg.contains(attribute), "{name}");
            }
        }
    }
}
