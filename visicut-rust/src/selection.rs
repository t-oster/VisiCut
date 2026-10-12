//! SVG object selections preserve the original viewport, groups and definitions.
use crate::i18n::tr;
use resvg::usvg::roxmltree::{Document, Node};
use serde::Serialize;

#[derive(Serialize)]
pub struct Object {
    pub id: usize,
    pub label: String,
}

fn drawable(node: Node<'_, '_>) -> bool {
    matches!(
        node.tag_name().name(),
        "path"
            | "rect"
            | "circle"
            | "ellipse"
            | "line"
            | "polyline"
            | "polygon"
            | "text"
            | "image"
            | "use"
    )
}

fn selectable(node: Node<'_, '_>) -> bool {
    node.is_element()
        && drawable(node)
        && !node.ancestors().skip(1).any(|n| {
            drawable(n)
                || matches!(
                    n.tag_name().name(),
                    "defs" | "clipPath" | "mask" | "pattern" | "marker" | "symbol"
                )
        })
}

pub(crate) fn selectable_nodes<'a, 'i>(doc: &'a Document<'i>) -> Vec<Node<'a, 'i>> {
    doc.descendants().filter(|n| selectable(*n)).collect()
}

pub fn objects(svg: &str) -> Result<Vec<Object>, String> {
    if svg.is_empty() {
        return Ok(Vec::new());
    }
    let doc = Document::parse(svg).map_err(|e| e.to_string())?;
    Ok(doc
        .descendants()
        .filter(|n| selectable(*n))
        .enumerate()
        .map(|(id, node)| {
            let name = node.attribute(("http://www.inkscape.org/namespaces/inkscape", "label"));
            let name = name
                .or_else(|| node.attribute("id"))
                .unwrap_or(node.tag_name().name());
            let paint = node
                .attribute("stroke")
                .filter(|p| *p != "none")
                .or_else(|| node.attribute("fill"))
                .unwrap_or(tr("geerbt"));
            Object {
                id,
                label: format!("{} · {} · {}", id + 1, name, paint),
            }
        })
        .collect())
}

pub fn filter(svg: &str, selected: &[usize]) -> Result<String, String> {
    let doc = Document::parse(svg).map_err(|e| e.to_string())?;
    let nodes: Vec<_> = doc.descendants().filter(|n| selectable(*n)).collect();
    if selected.iter().any(|id| *id >= nodes.len()) {
        return Err(tr("SVG-Objekt nicht vorhanden").into());
    }
    // Keep excluded objects inside defs so local <use> references still resolve.
    // The drawing order and ancestor transforms of selected objects stay intact.
    let mut result = svg.to_string();
    for (id, node) in nodes.iter().enumerate().rev() {
        if !selected.contains(&id) {
            let range = node.range();
            result.replace_range(
                range.clone(),
                &format!(
                    "<defs xmlns=\"http://www.w3.org/2000/svg\">{}</defs>",
                    &svg[range]
                ),
            );
        }
    }
    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn excludes_definitions_and_keeps_references_and_transforms() {
        let svg = r##"<svg xmlns="http://www.w3.org/2000/svg" width="100" height="60"><defs><path id="shape" d="M0 0L5 5"/></defs><g transform="translate(10 10)"><rect id="outline" width="20" height="20"/><use href="#shape"/><text>Hi</text></g></svg>"##;
        let list = objects(svg).unwrap();
        assert_eq!(list.len(), 3);
        assert!(list[0].label.contains("outline"));
        let filtered = filter(svg, &[1]).unwrap();
        assert_eq!(objects(&filtered).unwrap().len(), 1);
        assert!(filtered.contains("translate(10 10)"));
        assert!(filtered.contains("id=\"shape\""));
        assert!(filter(svg, &[3]).is_err());
    }
}
