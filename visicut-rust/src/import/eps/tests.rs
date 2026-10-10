use super::interpret;
use crate::project::Project;

/// A small EPS with a red stroked rectangle and a blue filled circle. The
/// BoundingBox is 144 × 72 pt.
const RECT_AND_CIRCLE: &[u8] =
    b"%!PS-Adobe-3.0 EPSF-3.0\n%%BoundingBox: 10 10 154 82\n%%EndComments\n\
    1 0 0 setrgbcolor 2 setlinewidth newpath 20 20 moveto 100 0 rlineto 0 50 rlineto \
    -100 0 rlineto closepath stroke\n\
    0 0 1 setrgbcolor newpath 130 46 20 0 360 arc closepath fill\n\
    showpage\n%%EOF\n";

fn project(svg: &str) -> Project {
    let preview = crate::svg::render(svg).unwrap();
    Project {
        svg: svg.to_string(),
        width_mm: preview.width_mm,
        height_mm: preview.height_mm,
        ..Default::default()
    }
}

fn error(data: &[u8]) -> String {
    interpret(data).err().expect("Fehler erwartet")
}

fn svg(data: &[u8]) -> String {
    interpret(data).expect("Import erwartet").svg
}

#[test]
fn rectangle_and_circle_keep_colours_and_bounding_box_size() {
    let imported = interpret(RECT_AND_CIRCLE).unwrap();
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    assert!(
        imported.svg.contains("stroke=\"#ff0000\""),
        "{}",
        imported.svg
    );
    assert!(
        imported.svg.contains("fill=\"#0000ff\""),
        "{}",
        imported.svg
    );
    let preview = crate::svg::render(&imported.svg).unwrap();
    assert!(
        (preview.width_mm - 50.8).abs() < 0.1,
        "{}",
        preview.width_mm
    );
    assert!(
        (preview.height_mm - 25.4).abs() < 0.1,
        "{}",
        preview.height_mm
    );
    let contours = crate::geometry::contours(&project(&imported.svg)).unwrap();
    assert!(contours.len() >= 2, "{}", contours.len());
}

#[test]
fn bounding_box_sets_viewbox_and_flips_the_y_axis() {
    let svg = svg(RECT_AND_CIRCLE);
    // BoundingBox 10 10 154 82: the lower left corner is the origin, y points down.
    assert!(svg.contains("viewBox=\"0 0 144 72\""), "{svg}");
    // (20, 20) → (10, 82 - 20); the rectangle then runs 100 pt to the right.
    assert!(svg.contains("M10 62 L110 62"), "{svg}");
    assert!(svg.contains("stroke-width=\"2\""), "{svg}");
}

#[test]
fn colour_operators_convert_to_rgb() {
    let svg = svg(b"%%BoundingBox: 0 0 100 100\n\
        0.5 setgray newpath 0 0 moveto 10 0 lineto stroke\n\
        0 0 0 1 setcmykcolor newpath 0 0 moveto 10 0 lineto stroke\n\
        1 0 0 0 setcmykcolor newpath 0 0 moveto 10 0 lineto stroke\n");
    assert!(svg.contains("stroke=\"#808080\""), "{svg}");
    assert!(svg.contains("stroke=\"#000000\""), "{svg}");
    assert!(svg.contains("stroke=\"#00ffff\""), "{svg}");
}

#[test]
fn gsave_and_grestore_restore_the_colour() {
    let svg = svg(b"%%BoundingBox: 0 0 100 100\n\
        1 0 0 setrgbcolor gsave 0 1 0 setrgbcolor grestore \
        newpath 0 0 moveto 10 0 lineto stroke\n");
    assert!(svg.contains("stroke=\"#ff0000\""), "{svg}");
    assert!(!svg.contains("#00ff00"), "{svg}");
}

#[test]
fn repeat_and_for_loops_draw_every_iteration() {
    let svg = svg(b"%%BoundingBox: 0 0 100 100\n\
        3 { newpath 10 10 moveto 20 0 rlineto 0 20 rlineto closepath fill } repeat\n\
        0 1 3 { 5 mul 0 moveto 4 0 rlineto stroke } for\n");
    assert_eq!(svg.matches("fill=\"#000000\"").count(), 3, "{svg}");
    assert_eq!(svg.matches("stroke=\"#000000\"").count(), 4, "{svg}");
    // The last iteration starts at x = 15 pt, y = 100 pt (SVG y points down).
    assert!(svg.contains("M15 100 L19 100"), "{svg}");
}

#[test]
fn dictionaries_procedures_and_conditionals() {
    let svg = svg(b"%%BoundingBox: 0 0 100 100\n\
        /n 3 def\n\
        /square { dup mul } def\n\
        0.5 square setgray newpath 0 0 moveto 10 0 lineto stroke\n\
        n 2 gt { 1 0 0 setrgbcolor } { 0 0 1 setrgbcolor } ifelse\n\
        newpath 0 0 moveto 10 0 lineto stroke\n");
    assert!(svg.contains("stroke=\"#404040\""), "{svg}");
    assert!(svg.contains("stroke=\"#ff0000\""), "{svg}");
    assert!(!svg.contains("#0000ff"), "{svg}");
}

#[test]
fn roll_and_arithmetic_on_the_operand_stack() {
    // 0.1 0.2 0.3 3 1 roll leaves 0.2 on top.
    let svg = svg(b"%%BoundingBox: 0 0 100 100\n\
        0.1 0.2 0.3 3 1 roll setgray newpath 0 0 moveto 10 0 lineto stroke\n\
        0.25 0.5 add setgray newpath 0 0 moveto 10 0 lineto stroke\n");
    assert!(svg.contains("stroke=\"#333333\""), "{svg}");
    assert!(svg.contains("stroke=\"#bfbfbf\""), "{svg}");
}

#[test]
fn translate_and_scale_apply_to_path_and_line_width() {
    // Scale after translate: user points are scaled first, then moved by (10, 20).
    let svg = svg(b"%%BoundingBox: 0 0 100 100\n\
        10 20 translate 2 2 scale newpath 0 0 moveto 5 0 lineto stroke\n");
    assert!(svg.contains("M10 80 L20 80"), "{svg}");
    assert!(svg.contains("stroke-width=\"2\""), "{svg}");
}

#[test]
fn rect_and_eofill_use_the_even_odd_rule() {
    let svg = svg(b"%%BoundingBox: 0 0 10 10\nnewpath 0 0 10 10 rect eofill\n");
    assert!(svg.contains("fill-rule=\"evenodd\""), "{svg}");
    assert!(svg.contains("M0 10 L10 10 L10 0 L0 0 Z"), "{svg}");
}

#[test]
fn bounding_box_comment_at_end_is_skipped_for_page_box() {
    let imported =
        interpret(b"%%BoundingBox: (atend)\n%%PageBoundingBox: 0 0 72 36\nshowpage\n").unwrap();
    assert!(imported.svg.contains("width=\"25.4"), "{}", imported.svg);
    assert!(
        imported.svg.contains("viewBox=\"0 0 72 36\""),
        "{}",
        imported.svg
    );
}

#[test]
fn missing_bounding_box_uses_default_size_with_warning() {
    let imported = interpret(b"newpath 0 0 moveto 1 1 lineto stroke\n").unwrap();
    assert_eq!(imported.warnings.len(), 1, "{:?}", imported.warnings);
    assert!(
        imported.svg.contains("viewBox=\"0 0 800 600\""),
        "{}",
        imported.svg
    );
}

#[test]
fn unknown_operator_is_named_in_the_error() {
    let message = error(b"%%BoundingBox: 0 0 10 10\n0 0 moveto frobnicate\n");
    assert!(message.contains("frobnicate"), "{message}");
}

#[test]
fn text_operator_is_reported_as_unsupported() {
    let message = error(b"%%BoundingBox: 0 0 100 100\n10 10 moveto (Hi) show\n");
    assert!(message.contains("„show“"), "{message}");
    assert!(message.contains("Text"), "{message}");
}

#[test]
fn fonts_and_images_are_reported_as_unsupported() {
    let font = error(b"%%BoundingBox: 0 0 100 100\n/Helvetica findfont 12 scalefont setfont\n");
    assert!(font.contains("findfont"), "{font}");
    assert!(font.contains("Schrift"), "{font}");
    let image = error(b"%%BoundingBox: 0 0 100 100\n0 0 10 10 image\n");
    assert!(image.contains("„image“"), "{image}");
    assert!(image.contains("Bild"), "{image}");
}

#[test]
fn endless_loops_stop_with_a_message() {
    let message = error(b"%%BoundingBox: 0 0 10 10\n100000000 { } repeat\n");
    assert!(message.contains("zu viele Operationen"), "{message}");
}

#[test]
fn unclosed_procedure_is_an_error() {
    let message = error(b"%%BoundingBox: 0 0 10 10\n1 { 0 0 moveto\n");
    assert!(message.contains("„{“"), "{message}");
}
