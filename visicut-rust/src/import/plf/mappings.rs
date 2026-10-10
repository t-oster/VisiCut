//! Zuordnungen einer PLF-Datei (`mappings.xml`) als Bearbeitungsschritte.
//!
//! Die Datei ist die XStream-Serialisierung einer VisiCut-`MappingSet`: eine
//! Liste von `mapping`-Einträgen mit Filtersatz (Element `a`, fehlt bei `null`)
//! und Profil (Element `b`, fehlt bei `null`). Abbildung nach VisiCut:
//!
//! - Filtersatz und Profil gesetzt: ein Schritt. Die Filter werden wie
//!   `FilterSet.getMatchingObjects` ausgewertet (UND-Verknüpfung; eine leere
//!   Liste trifft alles). Stimmt die Auswahl im zusammengesetzten Motiv mit der
//!   Auswahl des Teils überein, bleibt der Schritt eine Regel. Sonst wird die
//!   Auswahl als feste Objektliste übernommen, damit ein Teil keine Objekte
//!   eines anderen Teils erhält.
//! - Kein Filtersatz (`null`): Rest. Der Rest eines Teils sind seine Objekte,
//!   die kein Filtersatz erfasst, auch nicht ein Ignorier-Eintrag
//!   (`PlfPart.getUnmatchedObjects`). Er wird als feste Objektliste übernommen.
//! - Kein Profil (`null`): ignorieren. Der Eintrag erzeugt keinen Schritt,
//!   schließt seine Objekte aber aus dem Rest aus.
//! - Profile: `vectorProfile` mit `isCut` ergibt Schneiden, ohne `isCut`
//!   Markieren (Java zeichnet nur mit anderer Farbe, die Verfahren sind
//!   gleich). `rasterProfile` ergibt Gravur, `raster3dProfile` 3D-Gravur.
//!
//! Laser-Einstellungen (Leistung, Geschwindigkeit, Durchgänge, Fokus) stehen
//! nicht in der PLF-Datei. VisiCut speichert sie lokal pro Gerät, Material,
//! Stärke und Profilname (`LaserPropertyManager`, Ablage
//! `laserprofiles/<Gerät>/<Material>/<Stärke>mm/<Profil>.xml`). Die Schritte
//! erhalten deshalb die Standardwerte von `JobStep::new`. Eine Umrechnung der
//! Java-Geschwindigkeit in Prozent ist ohne Quelldaten nicht möglich und
//! unterbleibt. Der Hinweis nennt die Standardwerte.
//!
//! Abweichungen von Java, die als Warnung gemeldet werden: Konturversatz
//! (`useOutline`), Sortierung außer `INNER_FIRST`, Auflösung außer 500 DPI,
//! unbekannte Rasterverfahren und unbekannte Filter. Ein Filter, der nicht
//! übersetzt werden kann, verwirft nur seinen Eintrag. Ein Eintrag mit
//! unbekanntem Filter im Rest macht den Rest ungültig, damit kein Objekt
//! stillschweigend geschnitten wird.

use crate::mapping::{Attribute, Filter, ObjectAttributes};
use crate::project::{JobStep, Operation};
use crate::raster::{Dithering, RasterSettings};
use roxmltree::Node;

/// Höchstzahl der Bearbeitungsschritte in einem Projekt (`Project::validate`).
const MAX_STEPS: usize = 64;
/// Auflösung, mit der VisiCutRust rastert (`ltt::RASTER_DPI`) und der Java-Standard.
const SUPPORTED_DPI: f64 = 500.0;

/// Ein Wert eines Filters, so wie VisiCut ihn speichert.
#[derive(Clone, Debug, PartialEq)]
pub enum Value {
    /// `java.awt.Color` mit Alpha 255.
    Color([u8; 3]),
    /// Zahl (z. B. Linienstärke in mm).
    Number(f64),
    /// Zeichenkette (z. B. `none`, Gruppenname, ID).
    Text(String),
    /// Typ, den VisiCutRust nicht kennt, mit seiner Bezeichnung.
    Unsupported(String),
}

#[derive(Clone, Debug, PartialEq)]
pub struct JavaFilter {
    pub attribute: String,
    pub value: Value,
    pub inverted: bool,
    pub compare: bool,
}

/// Profil eines Eintrags mit den Feldern, die die PLF speichert.
#[derive(Clone, Debug, PartialEq)]
pub enum Profile {
    Vector {
        is_cut: bool,
        use_outline: bool,
        order: String,
        dpi: f64,
    },
    Raster {
        dpi: f64,
        invert: bool,
        color_shift: i32,
        /// Einfacher Klassenname des Rasterverfahrens, z. B. `FloydSteinberg`.
        dithering: Option<String>,
    },
    Raster3d {
        dpi: f64,
        invert: bool,
        color_shift: i32,
    },
}

#[derive(Clone, Debug, PartialEq)]
pub struct Entry {
    /// `None`: Rest (`null`). `Some(leer)`: alle Objekte.
    pub filters: Option<Vec<JavaFilter>>,
    /// `None`: ignorieren.
    pub profile: Option<Profile>,
}

/// Ein Teil der PLF-Datei, wie er im zusammengesetzten Motiv liegt.
pub struct Placed {
    pub name: String,
    /// Attribute der Objekte im SVG des Teils (`None`: nicht lesbar).
    pub objects: Option<Vec<ObjectAttributes>>,
    /// Das Teil ist SVG oder PSVG. Nur dafür kennt VisiCutRust die Attribute
    /// so wie VisiCut.
    pub svg: bool,
    /// Inhalt von `mappings.xml` (`None`: die Datei fehlt).
    pub mapping: Option<Result<Vec<Entry>, String>>,
}

#[derive(Debug, Default)]
pub struct Translation {
    pub steps: Vec<JobStep>,
    /// Deutsche Hinweise für den Import.
    pub warnings: Vec<String>,
}

/// Liest `mappings.xml`.
pub fn parse(bytes: &[u8]) -> Result<Vec<Entry>, String> {
    let text = String::from_utf8_lossy(bytes);
    let document = roxmltree::Document::parse(&text).map_err(|e| e.to_string())?;
    let root = document.root_element();
    if !root.tag_name().name().ends_with("MappingSet") {
        return Err("kein Zuordnungssatz".into());
    }
    let list = child(root, "linked-list").ok_or("Liste der Zuordnungen fehlt")?;
    list.children()
        .filter(|n| n.is_element() && n.has_tag_name("mapping"))
        .map(parse_entry)
        .collect()
}

fn parse_entry(node: Node) -> Result<Entry, String> {
    let filters = match child(node, "a") {
        Some(set) => {
            let list = child(set, "linked-list").ok_or("Filtersatz ohne Liste")?;
            Some(
                list.children()
                    .filter(|n| n.is_element() && n.has_tag_name("filter"))
                    .map(parse_filter)
                    .collect::<Result<Vec<_>, _>>()?,
            )
        }
        None => None,
    };
    let profile = child(node, "b").map(parse_profile).transpose()?;
    Ok(Entry { filters, profile })
}

fn parse_filter(node: Node) -> Result<JavaFilter, String> {
    let attribute = text(child(node, "attribute")).ok_or("Filter ohne Attribut")?;
    let value = child(node, "value").ok_or("Filter ohne Wert")?;
    Ok(JavaFilter {
        attribute,
        value: parse_value(value),
        inverted: flag(node, "inverted", false),
        compare: flag(node, "compare", false),
    })
}

fn parse_value(node: Node) -> Value {
    let class = node.attribute("class").unwrap_or("");
    let text = node.text().unwrap_or("").trim();
    match class {
        "awt-color" | "java.awt.Color" => color(node)
            .map(Value::Color)
            .unwrap_or_else(|| Value::Unsupported(class.into())),
        "string" => Value::Text(text.into()),
        "double" | "float" | "int" | "integer" | "long" | "short" => text
            .parse::<f64>()
            .map(Value::Number)
            .unwrap_or_else(|_| Value::Unsupported(class.into())),
        // Ohne Klasse: Farbe mit Komponenten oder Text.
        "" if child(node, "red").is_some() => color(node)
            .map(Value::Color)
            .unwrap_or_else(|| Value::Unsupported("Farbe".into())),
        "" => Value::Text(text.into()),
        other => Value::Unsupported(other.into()),
    }
}

/// `java.awt.Color` mit Alpha 255; andere Farben haben im SVG keine Entsprechung.
fn color(node: Node) -> Option<[u8; 3]> {
    let channel = |name: &str| -> Option<u8> { child(node, name)?.text()?.trim().parse().ok() };
    if let Some(alpha) = child(node, "alpha")
        && alpha.text().map(str::trim) != Some("255")
    {
        return None;
    }
    Some([channel("red")?, channel("green")?, channel("blue")?])
}

fn parse_profile(node: Node) -> Result<Profile, String> {
    let dpi = number(node, "DPI").unwrap_or(SUPPORTED_DPI);
    match node.attribute("class").unwrap_or("") {
        "vectorProfile" => Ok(Profile::Vector {
            is_cut: flag(node, "isCut", true),
            use_outline: flag(node, "useOutline", false),
            order: text(child(node, "orderStrategy")).unwrap_or_else(|| "INNER_FIRST".into()),
            dpi,
        }),
        "rasterProfile" => Ok(Profile::Raster {
            dpi,
            invert: flag(node, "invertColors", false),
            color_shift: int(node, "colorShift"),
            dithering: child(node, "ditherAlgorithm")
                .and_then(|d| d.attribute("class"))
                .map(|class| class.rsplit('.').next().unwrap_or(class).to_string()),
        }),
        "raster3dProfile" => Ok(Profile::Raster3d {
            dpi,
            invert: flag(node, "invertColors", false),
            color_shift: int(node, "colorShift"),
        }),
        other => Err(format!("Profil „{other}“ ist unbekannt")),
    }
}

fn child<'a, 'i>(node: Node<'a, 'i>, name: &str) -> Option<Node<'a, 'i>> {
    node.children()
        .find(|c| c.is_element() && c.has_tag_name(name))
}

fn text(node: Option<Node>) -> Option<String> {
    node.and_then(|n| n.text())
        .map(|t| t.trim().to_string())
        .filter(|t| !t.is_empty())
}

fn flag(node: Node, name: &str, default: bool) -> bool {
    match child(node, name).and_then(|c| c.text()).map(str::trim) {
        Some("true") => true,
        Some("false") => false,
        _ => default,
    }
}

fn number(node: Node, name: &str) -> Option<f64> {
    child(node, name)?.text()?.trim().parse().ok()
}

fn int(node: Node, name: &str) -> i32 {
    child(node, name)
        .and_then(|c| c.text())
        .and_then(|t| t.trim().parse().ok())
        .unwrap_or(0)
}

/// Rasterverfahren nach dem Klassennamen in VisiCut (`liblasercut.dithering`).
fn dithering_named(name: &str) -> Option<Dithering> {
    Some(match name {
        "Threshold" => Dithering::Threshold,
        "FloydSteinberg" => Dithering::FloydSteinberg,
        "Average" => Dithering::Average,
        "Random" => Dithering::Random,
        "Ordered" => Dithering::Ordered,
        "Grid" => Dithering::Grid,
        "Halftone" => Dithering::Halftone,
        "BrightenedHalftone" => Dithering::BrightenedHalftone,
        _ => return None,
    })
}

/// Übersetzt die Zuordnungen aller Teile. `composed` sind die Attribute des
/// zusammengesetzten Motivs; die Objekte der Teile liegen darin in
/// derselben Reihenfolge hintereinander.
pub fn translate(parts: &[Placed], composed: Result<Vec<ObjectAttributes>, String>) -> Translation {
    // Called only when the PLF file has a mappings.xml somewhere.
    let mut out = Translation::default();
    let composed = match composed {
        Ok(objects) => objects,
        Err(error) => {
            out.warnings
                .push(format!("Zuordnungen nicht übernommen: {error}"));
            return out;
        }
    };
    let mut offsets = Vec::with_capacity(parts.len());
    let mut total = 0;
    for part in parts {
        let Some(objects) = &part.objects else {
            out.warnings.push(format!(
                "Zuordnungen nicht übernommen: Objekte von „{}“ sind nicht lesbar",
                part.name
            ));
            return out;
        };
        offsets.push(total);
        total += objects.len();
    }
    if total != composed.len() {
        out.warnings.push(
            "Zuordnungen nicht übernommen: Das zusammengesetzte Motiv hat andere Objekte als die Teile"
                .into(),
        );
        return out;
    }
    for (part, offset) in parts.iter().zip(offsets) {
        let entries = match &part.mapping {
            None => {
                out.warnings.push(format!(
                    "Teil „{}“ hat keine Zuordnung; seine Objekte werden nicht bearbeitet",
                    part.name
                ));
                continue;
            }
            Some(Err(error)) => {
                out.warnings.push(format!(
                    "Zuordnung von Teil „{}“ nicht lesbar ({error}); seine Objekte werden nicht bearbeitet",
                    part.name
                ));
                continue;
            }
            Some(Ok(entries)) => entries,
        };
        if !part.svg {
            out.warnings.push(format!(
                "Zuordnung von Teil „{}“ nicht übernommen: Nur SVG-Teile lassen sich zuordnen; seine Objekte werden nicht bearbeitet",
                part.name
            ));
            continue;
        }
        if let Some(objects) = &part.objects {
            translate_part(&part.name, entries, objects, offset, &composed, &mut out);
        }
    }
    if out.steps.len() > MAX_STEPS {
        out.steps.truncate(MAX_STEPS);
        out.warnings.push(format!(
            "Mehr als {MAX_STEPS} Bearbeitungsschritte; die übrigen Zuordnungen wurden nicht übernommen"
        ));
    }
    if out.steps.is_empty() {
        // Ohne Schritt würde VisiCutRust das ganze Motiv bearbeiten. Ein leerer
        // Schritt verhindert das, wie VisiCut keine Objekte ohne Zuordnung schneidet.
        out.steps.push(JobStep::new(Operation::Cut));
        out.warnings.push(
            "Keine Zuordnung übernommen: Ohne Bearbeitungsschritt wird kein Objekt bearbeitet; bitte Schritte anlegen"
                .into(),
        );
    } else {
        let count = match out.steps.len() {
            1 => "1 Bearbeitungsschritt".to_string(),
            n => format!("{n} Bearbeitungsschritte"),
        };
        out.warnings.insert(
            0,
            format!(
                "Zuordnungen übernommen: {count}. Laser-Einstellungen (Leistung, Geschwindigkeit, Durchgänge) stehen nicht in der PLF-Datei, sondern lokal in VisiCut pro Gerät, Material und Stärke; die Schritte verwenden die Standardwerte 20 % Leistung, 100 % Geschwindigkeit, 1 Durchgang und müssen angepasst werden."
            ),
        );
    }
    out
}

fn translate_part(
    name: &str,
    entries: &[Entry],
    objects: &[ObjectAttributes],
    offset: usize,
    composed: &[ObjectAttributes],
    out: &mut Translation,
) {
    // Filter umwandeln. Ein Fehler betrifft nur den Eintrag mit diesem Filter.
    let converted: Vec<Result<Option<Vec<Filter>>, String>> = entries
        .iter()
        .map(|entry| match &entry.filters {
            None => Ok(None),
            Some(list) => list
                .iter()
                .map(|f| convert(f, name, &mut out.warnings))
                .collect::<Result<Vec<_>, _>>()
                .map(Some),
        })
        .collect();
    for result in &converted {
        if let Err(error) = result {
            out.warnings.push(format!(
                "Zuordnung in Teil „{name}“ nicht übernommen: {error}"
            ));
        }
    }

    // Rest des Teils (VisiCut: alles, was kein Filtersatz erfasst).
    let rest_wanted = entries
        .iter()
        .any(|e| e.profile.is_some() && e.filters.is_none());
    let rest: Option<Vec<usize>> = if !rest_wanted {
        None
    } else if converted.iter().any(Result::is_err) {
        out.warnings.push(format!(
            "Rest von Teil „{name}“ nicht übernommen: Ein Filter ist unbekannt, der Rest ließe sich nicht bestimmen"
        ));
        None
    } else {
        let mut remaining = vec![true; objects.len()];
        for list in converted.iter().flatten().flatten() {
            for (i, object) in objects.iter().enumerate() {
                if list.iter().all(|f| f.matches(object)) {
                    remaining[i] = false;
                }
            }
        }
        Some((0..objects.len()).filter(|i| remaining[*i]).collect())
    };

    for (entry, condition) in entries.iter().zip(&converted) {
        let (Some(profile), Ok(condition)) = (&entry.profile, condition) else {
            continue;
        };
        let (selected, filters): (Vec<usize>, Option<Vec<Filter>>) = match condition {
            None => match &rest {
                Some(rest) => (rest.clone(), None),
                None => continue,
            },
            Some(list) => (
                (0..objects.len())
                    .filter(|i| list.iter().all(|f| f.matches(&objects[*i])))
                    .collect(),
                Some(list.clone()),
            ),
        };
        if selected.is_empty() {
            out.warnings.push(format!(
                "Teil „{name}“: Eine Zuordnung trifft kein Objekt und wurde nicht übernommen"
            ));
            continue;
        }
        let (operation, raster) = profile_settings(profile, name, &mut out.warnings);
        let expected: Vec<usize> = selected.iter().map(|i| i + offset).collect();
        let mut step = JobStep::new(operation);
        step.raster = raster;
        match filters {
            // Die Bedingungen gelten im ganzen Motiv nur dann genauso, wenn sie
            // im ganzen Motiv dieselben Objekte treffen wie in diesem Teil.
            Some(list) if matching(&list, composed) == expected => step.filters = Some(list),
            Some(_) => {
                out.warnings.push(format!(
                    "Teil „{name}“: Die Bedingungen würden auch Objekte anderer Teile treffen; die Objekte dieses Teils wurden fest übernommen"
                ));
                step.objects = expected;
            }
            None => step.objects = expected,
        }
        out.steps.push(step);
    }
}

/// Indizes der Objekte im ganzen Motiv, die alle Filter treffen.
fn matching(filters: &[Filter], objects: &[ObjectAttributes]) -> Vec<usize> {
    objects
        .iter()
        .enumerate()
        .filter(|(_, o)| filters.iter().all(|f| f.matches(o)))
        .map(|(i, _)| i)
        .collect()
}

/// Filter von VisiCut in einen Filter von VisiCutRust.
///
/// Java wertet „Color“ bei SVG-Objekten nur als Linienfarbe aus, deshalb
/// entspricht es hier `StrokeColor`. Die Linienstärke vergleicht Java genau,
/// VisiCutRust mit 1e-6 mm Toleranz.
fn convert(filter: &JavaFilter, part: &str, warnings: &mut Vec<String>) -> Result<Filter, String> {
    let attribute = match filter.attribute.as_str() {
        "Stroke Color" | "Color" => Attribute::StrokeColor,
        "Fill Color" => Attribute::FillColor,
        "Stroke Width" => Attribute::StrokeWidth,
        "Type" => Attribute::Type,
        "Id" | "ID" => Attribute::Id,
        "Group" => Attribute::Group,
        other => return Err(format!("Filter „{other}“ ist in VisiCutRust nicht bekannt")),
    };
    let value = match (&filter.value, attribute) {
        (Value::Color([r, g, b]), Attribute::StrokeColor | Attribute::FillColor) => {
            format!("#{r:02x}{g:02x}{b:02x}")
        }
        (Value::Text(text), Attribute::StrokeColor | Attribute::FillColor) => text.clone(),
        (Value::Number(width), Attribute::StrokeWidth) => width.to_string(),
        (Value::Text(text), Attribute::StrokeWidth) => {
            let width: f64 = text
                .parse()
                .map_err(|_| format!("Linienstärke „{text}“ ist keine Zahl"))?;
            // VisiCut vergleicht eine Zeichenkette nie mit der Zahl und trifft
            // daher nichts. Übernommen wird der erkennbare Wert.
            warnings.push(format!(
                "Teil „{part}“: Die Linienstärke {text} ist als Text gespeichert; VisiCut wendet einen solchen Filter nicht an, übernommen als Zahl"
            ));
            width.to_string()
        }
        (Value::Text(text), _) => text.clone(),
        _ => {
            return Err(format!(
                "Wert des Filters „{}“ wird nicht unterstützt",
                filter.attribute
            ));
        }
    };
    Ok(Filter {
        attribute,
        value,
        compare: filter.compare,
        inverted: filter.inverted,
    })
}

/// Verfahren und Rastereinstellungen eines Profils; meldet, was nicht übernommen wird.
fn profile_settings(
    profile: &Profile,
    part: &str,
    warnings: &mut Vec<String>,
) -> (Operation, RasterSettings) {
    let dpi_check = |dpi: f64, warnings: &mut Vec<String>| {
        if (dpi - SUPPORTED_DPI).abs() > 1e-6 {
            warnings.push(format!(
                "Teil „{part}“: Auflösung {dpi} DPI wird nicht übernommen; VisiCutRust rastert mit {SUPPORTED_DPI} DPI"
            ));
        }
    };
    let color_shift = |value: i32, warnings: &mut Vec<String>| {
        if !(-255..=255).contains(&value) {
            warnings.push(format!(
                "Teil „{part}“: Helligkeitsverschiebung {value} auf den zulässigen Bereich begrenzt"
            ));
        }
        value.clamp(-255, 255)
    };
    match profile {
        Profile::Vector {
            is_cut,
            use_outline,
            order,
            dpi,
        } => {
            dpi_check(*dpi, warnings);
            if *use_outline {
                warnings.push(format!(
                    "Teil „{part}“: Konturversatz (Außenkontur) wird nicht übernommen"
                ));
            }
            if order != "INNER_FIRST" {
                warnings.push(format!(
                    "Teil „{part}“: Sortierung „{order}“ wird nicht übernommen; VisiCutRust sortiert nach Verschachtelung"
                ));
            }
            let operation = if *is_cut {
                Operation::Cut
            } else {
                Operation::Mark
            };
            (operation, RasterSettings::default())
        }
        Profile::Raster {
            dpi,
            invert,
            color_shift: shift,
            dithering,
        } => {
            dpi_check(*dpi, warnings);
            let default = RasterSettings::default();
            let dithering = match dithering.as_deref() {
                None => default.dithering,
                Some(name) => dithering_named(name).unwrap_or_else(|| {
                    warnings.push(format!(
                        "Teil „{part}“: Rasterverfahren „{name}“ ist unbekannt; Halbton aufgehellt wird verwendet"
                    ));
                    default.dithering
                }),
            };
            (
                Operation::Engrave,
                RasterSettings {
                    dithering,
                    invert: *invert,
                    color_shift: color_shift(*shift, warnings),
                    ..RasterSettings::default()
                },
            )
        }
        Profile::Raster3d {
            dpi,
            invert,
            color_shift: shift,
        } => {
            dpi_check(*dpi, warnings);
            (
                Operation::Engrave3d,
                RasterSettings {
                    invert: *invert,
                    color_shift: color_shift(*shift, warnings),
                    ..RasterSettings::default()
                },
            )
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SET: &str = r#"<com.t_oster.visicut.model.mapping.MappingSet>
  <linked-list>
    <default/>
    <int>3</int>
    <mapping>
      <a class="filters">
        <linked-list><default/><int>1</int>
          <filter><inverted>false</inverted><attribute>Stroke Color</attribute>
            <value class="awt-color"><red>255</red><green>0</green><blue>0</blue><alpha>255</alpha></value>
          </filter>
        </linked-list>
      </a>
      <b class="rasterProfile">
        <DPI>500.0</DPI>
        <invertColors>true</invertColors>
        <colorShift>12</colorShift>
        <ditherAlgorithm class="com.t_oster.liblasercut.dithering.FloydSteinberg"><progress>99</progress></ditherAlgorithm>
      </b>
    </mapping>
    <mapping>
      <b class="vectorProfile"><DPI>1000.0</DPI><isCut>false</isCut><useOutline>true</useOutline><orderStrategy>NEAREST</orderStrategy></b>
    </mapping>
    <mapping>
      <a class="filters">
        <linked-list><default/><int>1</int>
          <filter><inverted>false</inverted><attribute>Stroke Width</attribute><value class="string">0.5</value></filter>
        </linked-list>
      </a>
      <b class="raster3dProfile"><DPI>500.0</DPI><invertColors>false</invertColors><colorShift>0</colorShift></b>
    </mapping>
  </linked-list>
</com.t_oster.visicut.model.mapping.MappingSet>"#;

    #[test]
    fn parses_filters_profiles_and_missing_parts() {
        let entries = parse(SET.as_bytes()).unwrap();
        assert_eq!(entries.len(), 3);
        let first = entries[0].filters.as_ref().unwrap();
        assert_eq!(first[0].value, Value::Color([255, 0, 0]));
        assert_eq!(
            entries[0].profile,
            Some(Profile::Raster {
                dpi: 500.0,
                invert: true,
                color_shift: 12,
                dithering: Some("FloydSteinberg".into()),
            })
        );
        // Fehlt `a`, ist der Filtersatz null (Rest).
        assert_eq!(entries[1].filters, None);
        assert_eq!(
            entries[2].filters.as_ref().unwrap()[0].value,
            Value::Text("0.5".into())
        );
        assert!(parse(b"<mapping/>").is_err());
    }

    #[test]
    fn colour_filters_become_hex_and_widths_numbers() {
        let mut warnings = Vec::new();
        let colour = JavaFilter {
            attribute: "Fill Color".into(),
            value: Value::Color([0, 170, 255]),
            inverted: true,
            compare: false,
        };
        let filter = convert(&colour, "t", &mut warnings).unwrap();
        assert_eq!(filter.attribute, Attribute::FillColor);
        assert_eq!(filter.value, "#00aaff");
        assert!(filter.inverted);
        assert!(warnings.is_empty());
        let width = JavaFilter {
            attribute: "Stroke Width".into(),
            value: Value::Text("0.5".into()),
            inverted: false,
            compare: true,
        };
        let filter = convert(&width, "t", &mut warnings).unwrap();
        assert_eq!(
            (filter.attribute, filter.value.as_str()),
            (Attribute::StrokeWidth, "0.5")
        );
        assert!(filter.compare);
        assert_eq!(warnings.len(), 1);
        let unknown = JavaFilter {
            attribute: "Ebene 7".into(),
            value: Value::Text("x".into()),
            inverted: false,
            compare: false,
        };
        assert!(
            convert(&unknown, "t", &mut warnings)
                .unwrap_err()
                .contains("Ebene 7")
        );
    }

    #[test]
    fn parts_without_a_mapping_cut_nothing() {
        // A file with a mapping elsewhere: the part without one gets no step,
        // and the empty placeholder step keeps VisiCutRust from cutting the motif.
        let placed = [Placed {
            name: "a.svg".into(),
            objects: Some(vec![ObjectAttributes::default()]),
            svg: true,
            mapping: None,
        }];
        let out = translate(&placed, Ok(vec![ObjectAttributes::default()]));
        assert_eq!(out.steps.len(), 1);
        assert!(out.steps[0].objects.is_empty() && out.steps[0].filters.is_none());
        assert!(out.warnings.iter().any(|w| w.contains("keine Zuordnung")));
    }

    #[test]
    fn profiles_map_to_operations_and_report_unsupported_fields() {
        let entries = parse(SET.as_bytes()).unwrap();
        let mut warnings = Vec::new();
        let (op, raster) =
            profile_settings(entries[0].profile.as_ref().unwrap(), "t", &mut warnings);
        assert_eq!(op, Operation::Engrave);
        assert!(raster.invert);
        assert_eq!(raster.color_shift, 12);
        assert_eq!(raster.dithering, Dithering::FloydSteinberg);
        assert!(warnings.is_empty());
        let (op, _) = profile_settings(entries[1].profile.as_ref().unwrap(), "t", &mut warnings);
        assert_eq!(op, Operation::Mark);
        assert_eq!(warnings.len(), 3, "{warnings:?}");
        assert!(warnings.iter().any(|w| w.contains("Auflösung 1000")));
        assert!(warnings.iter().any(|w| w.contains("Konturversatz")));
        assert!(warnings.iter().any(|w| w.contains("NEAREST")));
    }
}
