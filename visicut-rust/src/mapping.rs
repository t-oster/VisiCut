//! Rule-based assignment of SVG objects to processing steps, following
//! VisiCut's MappingSet/FilterSet/MappingFilter and SVG attribute rules
//! (SVGShape/SVGObject.getAttributeValues).
use crate::project::{JobStep, Operation, Project};
use resvg::usvg::{self, roxmltree};
use serde::{Deserialize, Serialize};
use std::collections::BTreeSet;

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
pub enum Attribute {
    /// Stroke or fill colour (VisiCut "Color").
    Color,
    StrokeColor,
    FillColor,
    StrokeWidth,
    Type,
    Id,
    /// Enclosing groups and Inkscape layers, by label or id.
    Group,
}

impl Attribute {
    pub const ALL: [Attribute; 7] = [
        Self::Color,
        Self::StrokeColor,
        Self::FillColor,
        Self::StrokeWidth,
        Self::Group,
        Self::Type,
        Self::Id,
    ];
    pub fn title(self) -> &'static str {
        match self {
            Self::Color => "Farbe",
            Self::StrokeColor => "Linienfarbe",
            Self::FillColor => "Füllfarbe",
            Self::StrokeWidth => "Linienstärke (mm)",
            Self::Type => "Typ",
            Self::Id => "ID",
            Self::Group => "Gruppe/Ebene",
        }
    }
}

/// One condition. Colours are `#rrggbb` or `none`, stroke widths in mm.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct Filter {
    pub attribute: Attribute,
    pub value: String,
    /// Stroke width: match values up to `value` instead of equality.
    #[serde(default)]
    pub compare: bool,
    #[serde(default)]
    pub inverted: bool,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize)]
pub struct ObjectAttributes {
    pub color: Vec<String>,
    pub stroke_color: Vec<String>,
    pub fill_color: Vec<String>,
    pub stroke_width: Vec<String>,
    pub r#type: Vec<String>,
    pub id: Vec<String>,
    pub group: Vec<String>,
}

impl ObjectAttributes {
    pub fn values(&self, attribute: Attribute) -> &[String] {
        match attribute {
            Attribute::Color => &self.color,
            Attribute::StrokeColor => &self.stroke_color,
            Attribute::FillColor => &self.fill_color,
            Attribute::StrokeWidth => &self.stroke_width,
            Attribute::Type => &self.r#type,
            Attribute::Id => &self.id,
            Attribute::Group => &self.group,
        }
    }
}

impl Filter {
    pub fn validate(&self) -> Result<(), String> {
        if self.attribute == Attribute::StrokeWidth
            && !self.value.trim().parse::<f64>().is_ok_and(f64::is_finite)
        {
            return Err(format!("Linienstärke „{}“ ist keine Zahl", self.value));
        }
        Ok(())
    }

    /// MappingFilter.matches: numbers compare the first value, other
    /// attributes match if any value is equal; missing values match only
    /// inverted filters.
    pub fn matches(&self, object: &ObjectAttributes) -> bool {
        let values = object.values(self.attribute);
        let result = if self.attribute == Attribute::StrokeWidth {
            let (Some(first), Ok(limit)) = (values.first(), self.value.trim().parse::<f64>())
            else {
                return self.inverted;
            };
            let width: f64 = first.parse().unwrap_or(f64::NAN);
            if self.compare {
                width <= limit + 1e-6
            } else {
                (width - limit).abs() < 1e-6
            }
        } else {
            let wanted = normalise(self.attribute, &self.value);
            values.contains(&wanted)
        };
        result != self.inverted
    }

    pub fn describe(&self) -> String {
        let operator = match (self.compare, self.inverted) {
            (true, false) => "≤",
            (true, true) => ">",
            (false, false) => "=",
            (false, true) => "≠",
        };
        format!("{} {operator} {}", self.attribute.title(), self.value)
    }
}

fn normalise(attribute: Attribute, value: &str) -> String {
    match attribute {
        Attribute::Color | Attribute::StrokeColor | Attribute::FillColor => {
            let value = value.trim();
            if value.eq_ignore_ascii_case("none") {
                return "none".into();
            }
            if value.len() == 7 && value.starts_with('#') {
                return value.to_lowercase();
            }
            svgtypes_color(value).unwrap_or_else(|| value.to_lowercase())
        }
        _ => value.trim().to_string(),
    }
}

fn hex(color: usvg::Color) -> String {
    format!("#{:02x}{:02x}{:02x}", color.red, color.green, color.blue)
}

/// Accepts `red`, `#f00`, `rgb(…)` etc. and returns `#rrggbb`.
fn svgtypes_color(value: &str) -> Option<String> {
    // Resolve through usvg by styling a tiny document; keeps the CSS colour
    // grammar identical to the renderer's.
    let svg = format!(
        r#"<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"><rect width="1" height="1" fill="{}"/></svg>"#,
        value.replace(['"', '<', '>'], "")
    );
    let tree = usvg::Tree::from_str(&svg, &usvg::Options::default()).ok()?;
    let usvg::Node::Path(path) = tree.root().children().first()? else {
        return None;
    };
    match path.fill()?.paint() {
        usvg::Paint::Color(c) => Some(hex(*c)),
        _ => None,
    }
}

/// Selectable objects (see selection.rs) with their mapping attributes.
pub fn attributes(svg: &str) -> Result<Vec<ObjectAttributes>, String> {
    if svg.is_empty() {
        return Ok(Vec::new());
    }
    let doc = roxmltree::Document::parse(svg).map_err(|e| e.to_string())?;
    let nodes = crate::selection::selectable_nodes(&doc);
    let mut result: Vec<ObjectAttributes> = nodes
        .iter()
        .map(|node| {
            let mut a = ObjectAttributes::default();
            a.id.extend(node.attribute("id").map(str::to_string));
            a.r#type.push(type_name(node.tag_name().name()).into());
            a.r#type.push("Shape".into());
            for group in node.ancestors().skip(1).filter(|n| n.has_tag_name("g")) {
                let label = group
                    .attribute(("http://www.inkscape.org/namespaces/inkscape", "label"))
                    .or_else(|| group.attribute("id"));
                a.group.extend(label.map(str::to_string));
            }
            a
        })
        .collect();
    // Tag every object so usvg's resolved paints can be attributed to it.
    let mut tagged = svg.to_string();
    for (index, node) in nodes.iter().enumerate().rev() {
        let start = node.range().start;
        let name_end = svg[start + 1..]
            .find(|c: char| c.is_whitespace() || c == '/' || c == '>')
            .map_or(start + 1, |i| start + 1 + i);
        if node.attribute("id").is_none() {
            tagged.insert_str(name_end, &format!(" id=\"__visicut_object_{index}\""));
        }
    }
    let ids: Vec<String> = nodes
        .iter()
        .enumerate()
        .map(|(i, n)| {
            n.attribute("id")
                .map_or(format!("__visicut_object_{i}"), str::to_string)
        })
        .collect();
    let tree = usvg::Tree::from_str(&tagged, &crate::svg::options()).map_err(|e| e.to_string())?;
    let mut widths: Vec<BTreeSet<String>> = vec![BTreeSet::new(); nodes.len()];
    collect(tree.root(), None, &ids, &mut result, &mut widths);
    for (object, widths) in result.iter_mut().zip(widths) {
        object.stroke_width = widths.into_iter().collect();
        for list in [
            &mut object.color,
            &mut object.stroke_color,
            &mut object.fill_color,
        ] {
            let mut seen = BTreeSet::new();
            list.retain(|v| seen.insert(v.clone()));
        }
    }
    Ok(result)
}

fn type_name(tag: &str) -> &'static str {
    match tag {
        "path" => "Path",
        "rect" => "Rect",
        "circle" => "Circle",
        "ellipse" => "Ellipse",
        "line" => "Line",
        "polyline" => "Polyline",
        "polygon" => "Polygon",
        "text" => "Text",
        "image" => "Image",
        "use" => "Use",
        _ => "Shape",
    }
}

fn paint(paint: Option<&usvg::Paint>) -> Option<String> {
    match paint? {
        usvg::Paint::Color(c) => Some(hex(*c)),
        // Gradients and patterns have no single colour.
        _ => Some("pattern".into()),
    }
}

fn collect(
    group: &usvg::Group,
    current: Option<usize>,
    ids: &[String],
    result: &mut [ObjectAttributes],
    widths: &mut [BTreeSet<String>],
) {
    let owner = |id: &str, current: Option<usize>| ids.iter().position(|i| i == id).or(current);
    for node in group.children() {
        let object = owner(node.id(), current);
        match node {
            usvg::Node::Group(g) => collect(g, object, ids, result, widths),
            usvg::Node::Text(t) => collect(t.flattened(), object, ids, result, widths),
            usvg::Node::Path(path) => {
                let Some(index) = object else { continue };
                let a = &mut result[index];
                let stroke = path.stroke().and_then(|s| paint(Some(s.paint())));
                let fill = path.fill().and_then(|f| paint(Some(f.paint())));
                a.stroke_color.push(stroke.clone().unwrap_or("none".into()));
                a.fill_color.push(fill.clone().unwrap_or("none".into()));
                a.color.push(stroke.unwrap_or("none".into()));
                a.color.push(fill.unwrap_or("none".into()));
                if let Some(s) = path.stroke() {
                    let t = path.abs_transform();
                    let scale = ((t.sx * t.sy - t.kx * t.ky).abs() as f64).sqrt();
                    let mm = s.width().get() as f64 * scale * 25.4 / 96.0;
                    widths[index].insert(format!("{:.3}", mm));
                }
            }
            usvg::Node::Image(_) => {}
        }
    }
}

/// Indices of objects processed by every step. Manual steps list objects,
/// rule steps match filters (an empty filter list matches everything), and
/// rest steps take objects matched by no rule, ignore rule or manual step.
pub fn resolve(project: &Project) -> Result<Vec<Vec<usize>>, String> {
    for filter in project
        .steps
        .iter()
        .flat_map(|s| s.filters.iter().flatten())
        .chain(project.ignore_filters.iter().flatten())
    {
        filter.validate()?;
    }
    let rules = project.steps.iter().any(|s| s.filters.is_some() || s.rest)
        || !project.ignore_filters.is_empty();
    let objects = if rules {
        attributes(&project.svg)?
    } else {
        Vec::new()
    };
    let count = if rules {
        objects.len()
    } else {
        crate::selection::objects(&project.svg)?.len()
    };
    let matching = |filters: &[Filter]| -> Vec<usize> {
        (0..count)
            .filter(|i| filters.iter().all(|f| f.matches(&objects[*i])))
            .collect()
    };
    let mut claimed = vec![false; count];
    let mut selections: Vec<Option<Vec<usize>>> = project
        .steps
        .iter()
        .map(|step: &JobStep| {
            let selected = match &step.filters {
                _ if step.rest => return None,
                Some(filters) => matching(filters),
                None => step.objects.clone(),
            };
            for i in &selected {
                if let Some(c) = claimed.get_mut(*i) {
                    *c = true;
                }
            }
            Some(selected)
        })
        .collect();
    for filters in &project.ignore_filters {
        for i in matching(filters) {
            claimed[i] = true;
        }
    }
    let rest: Vec<usize> = (0..count).filter(|i| !claimed[*i]).collect();
    Ok(selections
        .iter_mut()
        .map(|s| s.take().unwrap_or_else(|| rest.clone()))
        .collect())
}

/// Values of one attribute with the number of objects having them.
pub type ValueCounts = Vec<(String, usize)>;

/// Distinct values per attribute in the document, for the mapping table.
pub fn values(svg: &str) -> Result<Vec<(Attribute, ValueCounts)>, String> {
    let objects = attributes(svg)?;
    Ok(Attribute::ALL
        .iter()
        .map(|attribute| {
            let mut counts = std::collections::BTreeMap::<String, usize>::new();
            for object in &objects {
                for value in object.values(*attribute).iter().collect::<BTreeSet<_>>() {
                    *counts.entry(value.clone()).or_default() += 1;
                }
            }
            (*attribute, counts.into_iter().collect())
        })
        .collect())
}

/// A predefined mapping (VisiCut "mappings/*.xml").
#[derive(Clone, Debug, Serialize)]
pub struct Predefined {
    pub name: &'static str,
    /// (operation, filters, rest)
    pub rules: Vec<(Operation, Option<Vec<Filter>>, bool)>,
    pub ignore: Vec<Vec<Filter>>,
}

fn color(value: &str) -> Vec<Filter> {
    vec![Filter {
        attribute: Attribute::Color,
        value: value.into(),
        compare: false,
        inverted: false,
    }]
}

pub fn predefined() -> Vec<Predefined> {
    let mut list = vec![
        // Reine RGB-Farben wie in vielen Laser-Workflows.
        Predefined {
            name: "Farben: Rot schneiden, Blau gravieren, Grün markieren",
            rules: vec![
                (Operation::Engrave, Some(color("#0000ff")), false),
                (Operation::Mark, Some(color("#00ff00")), false),
                (Operation::Cut, Some(color("#ff0000")), false),
            ],
            ignore: vec![],
        },
        // fau-fablab/visicut-settings/mappings
        Predefined {
            name: "FAU: rot schneiden, grün markieren, blau ignorieren, Rest gravieren",
            rules: vec![
                (Operation::Mark, Some(color("#00ff00")), false),
                (Operation::Cut, Some(color("#ff0000")), false),
                (Operation::Engrave, None, true),
            ],
            ignore: vec![color("#0000ff")],
        },
        Predefined {
            name: "FAU: grün markieren, rot und blau ignorieren, Rest gravieren",
            rules: vec![
                (Operation::Mark, Some(color("#00ff00")), false),
                (Operation::Engrave, None, true),
            ],
            ignore: vec![color("#0000ff"), color("#ff0000")],
        },
        Predefined {
            name: "Rot schneiden, Rest gravieren",
            rules: vec![
                (Operation::Cut, Some(color("#ff0000")), false),
                (Operation::Engrave, None, true),
            ],
            ignore: vec![],
        },
    ];
    for operation in Operation::ALL {
        list.push(Predefined {
            name: match operation {
                Operation::Cut => "Alles schneiden",
                Operation::Engrave => "Alles gravieren",
                Operation::Engrave3d => "Alles 3D-gravieren",
                Operation::Mark => "Alles markieren",
            },
            rules: vec![(operation, Some(Vec::new()), false)],
            ignore: vec![],
        });
    }
    list
}

#[cfg(test)]
mod tests {
    use super::*;

    const SVG: &str = r##"<svg xmlns="http://www.w3.org/2000/svg" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape" width="100mm" height="50mm" viewBox="0 0 100 50">
        <g inkscape:label="Schnitt" id="layer1"><rect x="1" y="1" width="98" height="48" fill="none" stroke="red" stroke-width="0.2"/></g>
        <circle id="dot" cx="20" cy="20" r="5" style="fill:#000"/>
        <g style="stroke:#00ff00"><path d="M30 10 L60 10" fill="none" stroke-width="0.5"/></g>
        <path d="M30 30 L60 30" stroke="blue" fill="none" transform="scale(2)"/>
    </svg>"##;

    #[test]
    fn extracts_resolved_colours_widths_types_ids_and_layers() {
        let objects = attributes(SVG).unwrap();
        assert_eq!(objects.len(), 4);
        assert_eq!(objects[0].stroke_color, ["#ff0000"]);
        assert_eq!(objects[0].fill_color, ["none"]);
        assert_eq!(objects[0].group, ["Schnitt"]);
        assert_eq!(objects[0].r#type, ["Rect", "Shape"]);
        assert_eq!(objects[0].stroke_width, ["0.200"]);
        assert_eq!(objects[1].id, ["dot"]);
        assert_eq!(objects[1].color, ["none", "#000000"]);
        assert_eq!(objects[2].stroke_color, ["#00ff00"]);
        assert_eq!(objects[3].stroke_width, ["2.000"]);
        assert!(objects[0].id.is_empty());
    }

    #[test]
    fn filters_follow_visicut_semantics() {
        let objects = attributes(SVG).unwrap();
        let red = Filter {
            attribute: Attribute::Color,
            value: "red".into(),
            compare: false,
            inverted: false,
        };
        assert!(red.matches(&objects[0]) && !red.matches(&objects[1]));
        let thin = Filter {
            attribute: Attribute::StrokeWidth,
            value: "0.5".into(),
            compare: true,
            inverted: false,
        };
        assert!(
            thin.matches(&objects[0]) && thin.matches(&objects[2]) && !thin.matches(&objects[3])
        );
        // Objects without a stroke have no width: they only match inverted filters.
        assert!(!thin.matches(&objects[1]));
        assert!(
            Filter {
                inverted: true,
                ..thin.clone()
            }
            .matches(&objects[1])
        );
        let layer = Filter {
            attribute: Attribute::Group,
            value: "Schnitt".into(),
            compare: false,
            inverted: false,
        };
        assert!(layer.matches(&objects[0]) && !layer.matches(&objects[2]));
        assert!(
            Filter {
                attribute: Attribute::StrokeWidth,
                value: "x".into(),
                compare: false,
                inverted: false
            }
            .validate()
            .is_err()
        );
    }

    #[test]
    fn resolve_assigns_rules_rest_ignore_and_overlaps() {
        let mut project = Project {
            svg: SVG.into(),
            ..Default::default()
        };
        let fau = predefined()
            .into_iter()
            .find(|p| p.name.starts_with("FAU: rot"))
            .unwrap();
        let fau = &fau;
        for (operation, filters, rest) in &fau.rules {
            project.steps.push(JobStep {
                filters: filters.clone(),
                rest: *rest,
                ..JobStep::new(*operation)
            });
        }
        project.ignore_filters = fau.ignore.clone();
        let selections = resolve(&project).unwrap();
        assert_eq!(selections, [vec![2], vec![0], vec![1]]);
        // A second rule may select the same object again (e.g. engrave and cut).
        project.steps.push(JobStep {
            filters: Some(vec![]),
            ..JobStep::new(Operation::Engrave3d)
        });
        assert_eq!(resolve(&project).unwrap()[3], [0, 1, 2, 3]);
        assert!(values(SVG).unwrap().iter().any(
            |(a, v)| *a == Attribute::Color && v.iter().any(|(c, n)| c == "#ff0000" && *n == 1)
        ));
    }
}
