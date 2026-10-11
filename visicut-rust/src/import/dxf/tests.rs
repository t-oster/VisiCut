use super::*;
use crate::geometry::{Contour, contours};
use crate::mapping::{Attribute, Filter, attributes};
use crate::project::Project;
use std::sync::atomic::{AtomicUsize, Ordering};

/// DXF text from `|`-separated group codes and values.
fn dxf(pairs: &str) -> String {
    let mut out = String::new();
    for (i, token) in pairs.split('|').enumerate() {
        if i % 2 == 0 {
            out.push_str(&format!("{:>3}\r\n", token.trim()));
        } else {
            out.push_str(token);
            out.push_str("\r\n");
        }
    }
    out
}

fn drawing(header: &str, tables: &str, blocks: &str, entities: &str) -> String {
    let mut pairs = String::from("0|SECTION|2|HEADER|9|$ACADVER|1|AC1015");
    pairs.push_str(header);
    pairs.push_str("|0|ENDSEC|0|SECTION|2|TABLES");
    pairs.push_str(tables);
    pairs.push_str("|0|ENDSEC|0|SECTION|2|BLOCKS");
    pairs.push_str(blocks);
    pairs.push_str("|0|ENDSEC|0|SECTION|2|ENTITIES");
    pairs.push_str(entities);
    pairs.push_str("|0|ENDSEC|0|EOF");
    dxf(&pairs)
}

const MM: &str = "|9|$INSUNITS|70|4";

fn import_bytes(content: &[u8]) -> Result<Imported, String> {
    static COUNTER: AtomicUsize = AtomicUsize::new(0);
    let dir = std::env::temp_dir().join(format!(
        "visicut-dxf-{}-{}",
        std::process::id(),
        COUNTER.fetch_add(1, Ordering::Relaxed)
    ));
    std::fs::create_dir_all(&dir).unwrap();
    let path = dir.join("zeichnung.dxf");
    std::fs::write(&path, content).unwrap();
    let result = read(&path);
    std::fs::remove_dir_all(&dir).ok();
    result
}

fn import(content: &str) -> Imported {
    import_bytes(content.as_bytes()).unwrap_or_else(|e| panic!("{e}"))
}

fn size(svg: &str) -> (f32, f32) {
    let preview = crate::svg::render(svg).unwrap();
    (preview.width_mm, preview.height_mm)
}

fn cut(svg: &str) -> Vec<Contour> {
    let (width_mm, height_mm) = size(svg);
    contours(&Project {
        svg: svg.into(),
        x_mm: 0.0,
        y_mm: 0.0,
        width_mm,
        height_mm,
        ..Default::default()
    })
    .unwrap()
}

fn near(a: impl Into<f64>, b: impl Into<f64>) -> bool {
    let (a, b): (f64, f64) = (a.into(), b.into());
    (a - b).abs() < 0.01
}

fn assert_size(svg: &str, width: f32, height: f32) {
    let (w, h) = size(svg);
    assert!(near(w, width) && near(h, height), "{w} × {h}\n{svg}");
}

#[test]
fn imports_line_and_circle_in_millimetres() {
    let imported = import(&drawing(
        MM,
        "",
        "",
        "|0|LINE|8|0|10|0|20|0|11|100|21|0|0|CIRCLE|8|0|10|50|20|20|40|10",
    ));
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    assert_size(&imported.svg, 100.0, 30.0);
    let paths = cut(&imported.svg);
    assert_eq!(paths.len(), 2);
    // DXF Y points up: y = 0 is the bottom edge of the drawing.
    assert!(near(paths[0][0][0], 0.0) && near(paths[0][0][1], 30.0));
    assert!(near(paths[0][1][0], 100.0) && near(paths[0][1][1], 30.0));
    assert_eq!(paths[1].first(), paths[1].last());
    assert!(
        paths[1]
            .iter()
            .all(|p| near((p[0] - 50.0).hypot(p[1] - 10.0), 10.0))
    );
}

#[test]
fn lwpolyline_bulges_become_arcs() {
    // Two half circles: a closed circle of radius 5 around (5, 0).
    let imported = import(&drawing(
        MM,
        "",
        "",
        "|0|LWPOLYLINE|8|0|90|2|70|1|10|0|20|0|42|1|10|10|20|0|42|1",
    ));
    assert_size(&imported.svg, 10.0, 10.0);
    let paths = cut(&imported.svg);
    assert_eq!(paths.len(), 1);
    assert!(paths[0].len() > 20);
    assert!(
        paths[0]
            .iter()
            .all(|p| near((p[0] - 5.0).hypot(p[1] - 5.0), 5.0))
    );
}

#[test]
fn inserts_rotated_and_scaled_blocks() {
    let imported = import(&drawing(
        MM,
        "",
        "|0|BLOCK|8|0|2|Strich|70|0|10|1|20|0|0|LINE|8|0|10|1|20|0|11|11|21|0|0|ENDBLK",
        "|0|LINE|8|0|10|0|20|0|11|1|21|0|0|INSERT|8|0|2|Strich|10|20|20|20|41|2|42|2|50|90",
    ));
    // The block line (base point 1,0) runs 20 mm upwards from the insertion point.
    assert_size(&imported.svg, 20.0, 40.0);
    let paths = cut(&imported.svg);
    let line = &paths[1];
    assert!(near(line[0][0], 20.0) && near(line[0][1], 20.0), "{line:?}");
    assert!(near(line[1][0], 20.0) && near(line[1][1], 0.0), "{line:?}");
}

#[test]
fn nested_blocks_inherit_layer_and_colour() {
    let imported = import(&drawing(
        MM,
        "|0|TABLE|2|LAYER|0|LAYER|2|0|62|7|0|LAYER|2|Teile|62|1|0|ENDTAB",
        concat!(
            "|0|BLOCK|8|0|2|Innen|10|0|20|0|0|LINE|8|0|62|0|10|0|20|0|11|5|21|0|0|ENDBLK",
            "|0|BLOCK|8|0|2|Aussen|10|0|20|0|0|INSERT|8|0|62|0|2|Innen|10|0|20|5|0|ENDBLK"
        ),
        "|0|INSERT|8|Teile|2|Aussen|10|0|20|0",
    ));
    let objects = attributes(&imported.svg).unwrap();
    assert_eq!(objects.len(), 1);
    assert_eq!(objects[0].stroke_color, vec!["#ff0000"]);
    assert_eq!(objects[0].group, vec!["Teile"]);
    assert_size(&imported.svg, 5.0, 0.01);
}

#[test]
fn converts_inches_to_millimetres() {
    let imported = import(&drawing(
        "|9|$INSUNITS|70|1",
        "",
        "",
        "|0|LINE|8|0|10|1|20|1|11|3|21|2",
    ));
    assert_size(&imported.svg, 50.8, 25.4);
    let paths = cut(&imported.svg);
    assert!(near(paths[0][1][0], 50.8) && near(paths[0][1][1], 0.0));
}

#[test]
fn unitless_drawings_are_millimetres_with_a_warning() {
    let imported = import(&drawing("", "", "", "|0|LINE|8|0|10|0|20|0|11|7|21|3"));
    assert_size(&imported.svg, 7.0, 3.0);
    assert_eq!(imported.warnings.len(), 1);
    assert!(imported.warnings[0].contains("Millimeter"));
}

#[test]
fn preserves_layers_and_colours_for_mapping() {
    let imported = import(&drawing(
        MM,
        "|0|TABLE|2|LAYER|0|LAYER|2|Schnitt|62|1|70|0|0|LAYER|2|Gravur|62|5|70|0|0|LAYER|2|Aus|62|-3|70|0|0|ENDTAB",
        "",
        concat!(
            "|0|LINE|8|Gravur|10|0|20|0|11|10|21|0",
            "|0|LINE|8|Schnitt|10|0|20|5|11|10|21|5|370|50",
            "|0|LINE|8|Gravur|62|3|10|0|20|10|11|10|21|10",
            "|0|LINE|8|Schnitt|62|1|420|1193046|10|0|20|15|11|10|21|15",
            "|0|LINE|8|Aus|10|0|20|50|11|10|21|50",
        ),
    ));
    // Layer table order, hidden layer skipped.
    let schnitt = imported.svg.find(r#"inkscape:label="Schnitt""#).unwrap();
    let gravur = imported.svg.find(r#"inkscape:label="Gravur""#).unwrap();
    assert!(schnitt < gravur);
    assert!(!imported.svg.contains("Aus\""));
    assert!(
        imported
            .warnings
            .iter()
            .any(|w| w.contains("ausgeschalteten"))
    );
    assert_size(&imported.svg, 10.0, 15.0);

    let objects = attributes(&imported.svg).unwrap();
    let summary: Vec<(String, String)> = objects
        .iter()
        .map(|o| (o.group[0].clone(), o.stroke_color[0].clone()))
        .collect();
    assert_eq!(
        summary,
        vec![
            ("Schnitt".into(), "#ff0000".into()),
            ("Schnitt".into(), "#123456".into()),
            ("Gravur".into(), "#0000ff".into()),
            ("Gravur".into(), "#00ff00".into()),
        ]
    );
    assert_eq!(objects[0].stroke_width, vec!["0.500"]);
    let red = Filter {
        attribute: Attribute::StrokeColor,
        value: "#ff0000".into(),
        compare: false,
        inverted: false,
    };
    let layer = Filter {
        attribute: Attribute::Group,
        value: "Gravur".into(),
        compare: false,
        inverted: false,
    };
    assert_eq!(objects.iter().filter(|o| red.matches(o)).count(), 1);
    assert_eq!(objects.iter().filter(|o| layer.matches(o)).count(), 2);
}

#[test]
fn mirrors_entities_with_negative_extrusion() {
    // Circle centre (10, 0) in an OCS with Z down lies at x = -10 in the WCS.
    let imported = import(&drawing(
        MM,
        "",
        "",
        concat!(
            "|0|LINE|8|0|10|0|20|0|11|1|21|0",
            "|0|CIRCLE|8|0|10|10|20|0|30|0|40|2|210|0|220|0|230|-1",
            "|0|LWPOLYLINE|8|0|90|2|70|0|10|0|20|1|10|3|20|1|210|0|220|0|230|-1",
        ),
    ));
    assert_size(&imported.svg, 13.0, 4.0);
    let paths = cut(&imported.svg);
    assert!(
        paths[1]
            .iter()
            .all(|p| near((p[0] - 2.0).hypot(p[1] - 2.0), 2.0))
    );
    // Polyline (0,1)→(3,1) becomes (0,1)→(-3,1).
    assert!(near(paths[2][0][0], 12.0) && near(paths[2][1][0], 9.0));
}

#[test]
fn reports_unsupported_entities_once() {
    let imported = import(&drawing(
        MM,
        "",
        "",
        concat!(
            "|0|XLINE|8|0|10|0|20|0|11|1|21|0",
            "|0|LINE|8|0|10|0|20|0|11|1|21|1",
            "|0|3DSOLID|8|0|1|abc",
            "|0|XLINE|8|0|10|0|20|0|11|0|21|1",
            "|0|POINT|8|0|10|5|20|5",
        ),
    ));
    assert_eq!(
        imported.warnings,
        vec!["Nicht unterstützte DXF-Objekte wurden ausgelassen: 3DSOLID (1), XLINE (2)"]
    );
    assert_size(&imported.svg, 1.0, 1.0);
}

#[test]
fn rejects_broken_files() {
    assert!(import_bytes(b"Hallo Welt\nkein DXF").is_err());
    assert!(import_bytes(b"").is_err());
    let broken = dxf("0|SECTION|2|ENTITIES|0|LINE|8|0|zehn|0|20|0");
    let Err(error) = import_bytes(broken.as_bytes()) else {
        panic!("broken file accepted");
    };
    assert!(error.contains("beschädigt"), "{error}");
    let empty = drawing(MM, "", "", "");
    assert!(import_bytes(empty.as_bytes()).is_err());
    assert!(import_bytes(b"AutoCAD Binary DXF\r\n\x1a\0\0SECT").is_err());
}

#[test]
fn reads_binary_dxf() {
    let mut bytes = b"AutoCAD Binary DXF\r\n\x1a\0".to_vec();
    let text = |bytes: &mut Vec<u8>, code: u8, value: &str| {
        bytes.push(code);
        bytes.extend_from_slice(value.as_bytes());
        bytes.push(0);
    };
    let double = |bytes: &mut Vec<u8>, code: u8, value: f64| {
        bytes.push(code);
        bytes.extend_from_slice(&value.to_le_bytes());
    };
    text(&mut bytes, 0, "SECTION");
    text(&mut bytes, 2, "ENTITIES");
    text(&mut bytes, 0, "LINE");
    text(&mut bytes, 8, "Kontur");
    bytes.extend_from_slice(&[62, 1, 0]);
    double(&mut bytes, 10, 0.0);
    double(&mut bytes, 20, 0.0);
    double(&mut bytes, 11, 40.0);
    double(&mut bytes, 21, 30.0);
    text(&mut bytes, 0, "ENDSEC");
    text(&mut bytes, 0, "EOF");
    let imported = import_bytes(&bytes).unwrap_or_else(|e| panic!("{e}"));
    assert_size(&imported.svg, 40.0, 30.0);
    assert!(imported.svg.contains(r#"inkscape:label="Kontur""#));
    assert!(imported.svg.contains(r##"stroke="#ff0000""##));
}

#[test]
fn flattens_splines_and_draws_ellipses() {
    // A clamped cubic spline with four control points is one Bézier segment.
    let imported = import(&drawing(
        MM,
        "",
        "",
        concat!(
            "|0|SPLINE|8|0|70|8|71|3|72|8|73|4",
            "|40|0|40|0|40|0|40|0|40|1|40|1|40|1|40|1",
            "|10|0|20|0|10|0|20|10|10|10|20|10|10|10|20|0",
            "|0|ELLIPSE|8|0|10|30|20|0|11|10|21|0|40|0.5|41|0|42|3.14159265358979",
        ),
    ));
    // Spline peaks at y = 7.5; the upper half ellipse spans x 20..40, y 0..5.
    assert_size(&imported.svg, 40.0, 7.5);
    let paths = cut(&imported.svg);
    let spline = &paths[0];
    assert!(near(spline[0][0], 0.0) && near(spline[0][1], 7.5));
    let last = spline.last().unwrap();
    assert!(near(last[0], 10.0) && near(last[1], 7.5));
    assert!(spline.iter().any(|p| near(p[0], 5.0) && near(p[1], 0.0)));
    let ellipse = &paths[1];
    assert!(near(ellipse[0][0], 40.0) && near(ellipse.last().unwrap()[0], 20.0));
}

#[test]
fn emits_text_mtext_hatch_and_linetypes() {
    let imported = import(&drawing(
        MM,
        concat!(
            "|0|TABLE|2|LTYPE|0|LTYPE|2|DASHED|72|65|73|2|40|7.5|49|5|74|0|49|-2.5|74|0|0|ENDTAB",
            "|0|TABLE|2|LAYER|0|LAYER|2|0|62|7|6|CONTINUOUS|0|ENDTAB",
        ),
        "",
        concat!(
            "|0|LINE|8|0|6|DASHED|10|0|20|0|11|100|21|0",
            "|0|TEXT|8|0|10|0|20|10|40|7|1|M%%c8 & <Loch>",
            "|0|MTEXT|8|0|10|50|20|60|40|5|71|1|1|Zeile 1\\PZeile 2",
            "|0|HATCH|8|0|62|3|10|0|20|0|30|0|2|SOLID|70|1|71|0|91|1",
            "|92|3|72|0|73|1|93|4|10|80|20|0|10|90|20|0|10|90|20|10|10|80|20|10|97|0",
            "|75|0|76|1|98|0",
        ),
    ));
    let svg = &imported.svg;
    assert!(svg.contains(r#"stroke-dasharray="5 2.5""#), "{svg}");
    assert!(svg.contains("M⌀8 &amp; &lt;Loch&gt;"));
    assert!(svg.contains(r#"font-size="10""#));
    assert!(svg.contains(">Zeile 1</tspan>") && svg.contains(">Zeile 2</tspan>"));
    assert!(svg.contains(r##"fill="#00ff00" fill-rule="evenodd" stroke="none""##));
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    // MTEXT hangs below its top-left insertion point (y = 60).
    let (_, height) = size(svg);
    assert!(height > 59.0 && height < 61.0, "{height}");
    let objects = attributes(svg).unwrap();
    assert_eq!(objects.len(), 4);
    assert!(objects[1].r#type.contains(&"Text".to_string()));
}
