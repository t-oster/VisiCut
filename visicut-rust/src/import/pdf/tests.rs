use super::*;
use crate::project::Project;
use std::path::PathBuf;

/// Builds a minimal PDF with one content stream per page and a correct xref.
fn pdf(pages: &[&str], media_box: [f32; 4], trailer_extra: &str) -> Vec<u8> {
    let page_ids: Vec<usize> = (0..pages.len()).map(|i| 3 + 2 * i).collect();
    let mut objects = vec![
        "<< /Type /Catalog /Pages 2 0 R >>".to_string(),
        format!(
            "<< /Type /Pages /Kids [{}] /Count {} >>",
            page_ids
                .iter()
                .map(|id| format!("{id} 0 R"))
                .collect::<Vec<_>>()
                .join(" "),
            pages.len()
        ),
    ];
    for (i, content) in pages.iter().enumerate() {
        let [x0, y0, x1, y1] = media_box;
        objects.push(format!(
            "<< /Type /Page /Parent 2 0 R /MediaBox [{x0} {y0} {x1} {y1}] /Contents {} 0 R \
             /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> >>",
            page_ids[i] + 1
        ));
        objects.push(format!(
            "<< /Length {} >>\nstream\n{content}\nendstream",
            content.len() + 1
        ));
    }
    let mut out = b"%PDF-1.4\n".to_vec();
    let mut offsets = Vec::new();
    for (i, object) in objects.iter().enumerate() {
        offsets.push(out.len());
        out.extend_from_slice(format!("{} 0 obj\n{object}\nendobj\n", i + 1).as_bytes());
    }
    let xref = out.len();
    out.extend_from_slice(
        format!("xref\n0 {}\n0000000000 65535 f \n", objects.len() + 1).as_bytes(),
    );
    for offset in offsets {
        out.extend_from_slice(format!("{offset:010} 00000 n \n").as_bytes());
    }
    out.extend_from_slice(
        format!(
            "trailer\n<< /Size {} /Root 1 0 R {trailer_extra} >>\nstartxref\n{xref}\n%%EOF\n",
            objects.len() + 1
        )
        .as_bytes(),
    );
    out
}

/// Red stroked rectangle, blue filled circle (four Béziers), green zero-width line.
const DRAWING: &str = "1 0 0 RG 2 w 20 20 100 50 re S \
    0 0 1 rg 200 60 m 200 87.6 177.6 110 150 110 c 122.4 110 100 87.6 100 60 c \
    100 32.4 122.4 10 150 10 c 177.6 10 200 32.4 200 60 c f \
    0 1 0 RG 0 w 10 130 m 270 130 l S";

fn project(svg: &str) -> Project {
    let preview = crate::svg::render(svg).unwrap();
    Project {
        svg: svg.to_string(),
        width_mm: preview.width_mm,
        height_mm: preview.height_mm,
        ..Default::default()
    }
}

#[test]
fn converts_vector_page_with_physical_size_and_colours() {
    // 283.465 × 141.732 pt = 100 × 50 mm.
    let imported = convert(pdf(&[DRAWING], [0.0, 0.0, 283.465, 141.732], "")).unwrap();
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    let preview = crate::svg::render(&imported.svg).unwrap();
    assert!(
        (preview.width_mm - 100.0).abs() < 0.01,
        "{}",
        preview.width_mm
    );
    assert!(
        (preview.height_mm - 50.0).abs() < 0.01,
        "{}",
        preview.height_mm
    );
    assert!(imported.svg.contains("stroke=\"#ff0000\""));
    assert!(imported.svg.contains("fill=\"#0000ff\""));
    assert!(imported.svg.contains("stroke=\"#00ff00\""));
    assert!(imported.svg.contains("stroke-width=\"2\""));
    assert!(!imported.svg.contains("stroke-width=\"0\""));

    let project = project(&imported.svg);
    let contours = crate::geometry::contours(&project).unwrap();
    assert_eq!(contours.len(), 3, "rect, circle and hairline");
    // The rectangle starts at 20 pt = 7.06 mm from the left and its bottom edge
    // at 20 pt above the page bottom, i.e. 50 - 7.06 mm from the top (y down).
    let rect = contours
        .iter()
        .find(|c| c.len() == 5)
        .expect("closed rectangle");
    let min_x = rect.iter().map(|p| p[0]).fold(f32::MAX, f32::min) - project.x_mm;
    let max_y = rect.iter().map(|p| p[1]).fold(f32::MIN, f32::max) - project.y_mm;
    assert!((min_x - 20.0 * MM_PER_PT).abs() < 0.01, "{min_x}");
    assert!((max_y - (50.0 - 20.0 * MM_PER_PT)).abs() < 0.01, "{max_y}");
}

#[test]
fn uses_crop_box_offset() {
    let imported = convert(pdf(&[DRAWING], [10.0, 10.0, 82.0, 82.0], "")).unwrap();
    let preview = crate::svg::render(&imported.svg).unwrap();
    assert!((preview.width_mm - 25.4).abs() < 0.01);
    assert!((preview.height_mm - 25.4).abs() < 0.01);
}

#[test]
fn embeds_raster_images_as_data_uris() {
    // 2×1 RGB image (red, blue) scaled to 72 × 36 pt.
    let image = "q 72 0 0 36 0 0 cm BI /W 2 /H 1 /CS /RGB /BPC 8 /F /AHx ID ff00000000ff> EI Q";
    let bytes = pdf(&[image], [0.0, 0.0, 72.0, 36.0], "");
    let imported = convert(bytes).unwrap();
    assert!(imported.svg.contains("data:image/png;base64,"));
    let preview = crate::svg::render(&imported.svg).unwrap();
    assert!(
        preview
            .image
            .pixels
            .iter()
            .any(|p| p.r() > 200 && p.b() < 50)
    );
}

#[test]
fn converts_text_to_cuttable_outlines() {
    // Helvetica is not embedded, so hayro's bundled standard font is used.
    let text = "0 0 1 rg BT /F1 36 Tf 10 10 Td (Hi) Tj ET";
    let imported = convert(pdf(&[text], [0.0, 0.0, 100.0, 50.0], "")).unwrap();
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    assert!(!imported.svg.contains("<text"));
    let contours = crate::geometry::contours(&project(&imported.svg)).unwrap();
    // "H" and the stem and dot of "i".
    assert!(contours.len() >= 3, "{}", contours.len());
}

#[test]
fn clip_paths_are_cut_without_warning() {
    // The clip crosses the right edge of the square: the outline is cut to the parts
    // inside x 50..110 (top, bottom and right edge).
    let clipped = "q 50 -10 60 120 re W n 0 0 0 rg 0 0 100 100 re f Q";
    let imported = convert(pdf(&[clipped], [0.0, 0.0, 100.0, 100.0], "")).unwrap();
    assert!(imported.svg.contains("clip-path"));
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    let paths = crate::geometry::contours(&project(&imported.svg)).unwrap();
    assert!(!paths.is_empty());
    // A clip that only covers the page is dropped and stays cuttable.
    let page_clip = "q 0 0 100 100 re W n 1 0 0 RG 10 10 50 50 re S Q";
    let imported = convert(pdf(&[page_clip], [0.0, 0.0, 100.0, 100.0], "")).unwrap();
    assert!(imported.warnings.is_empty(), "{:?}", imported.warnings);
    assert_eq!(
        crate::geometry::contours(&project(&imported.svg))
            .unwrap()
            .len(),
        1
    );
}

#[test]
fn warns_about_further_pages() {
    let imported = convert(pdf(
        &[
            DRAWING,
            "0 0 0 RG 0 0 m 10 10 l S",
            "0 0 0 RG 0 0 m 5 5 l S",
        ],
        [0.0, 0.0, 300.0, 200.0],
        "",
    ))
    .unwrap();
    assert_eq!(
        imported.warnings,
        vec!["PDF enthält 3 Seiten; nur Seite 1 wurde importiert".to_string()]
    );
    // Only page 1 is drawn: it is the one with the red rectangle.
    assert!(imported.svg.contains("#ff0000"));
}

#[test]
fn rejects_files_that_are_not_pdf() {
    let error = convert(b"hello world".to_vec()).err().unwrap();
    assert!(error.contains("keine gültige PDF"), "{error}");
}

#[test]
fn rejects_broken_pdf() {
    let mut bytes = pdf(&[DRAWING], [0.0, 0.0, 100.0, 100.0], "");
    bytes.truncate(40);
    let error = convert(bytes).err().unwrap();
    assert!(
        error.contains("beschädigt") || error.contains("keine Seiten"),
        "{error}"
    );
}

#[test]
fn rejects_encrypted_pdf() {
    let encrypt = "/Encrypt << /Filter /Standard /V 2 /R 3 /Length 128 /P -4 \
        /O <00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff> \
        /U <00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff> >> \
        /ID [<00112233445566778899aabbccddeeff> <00112233445566778899aabbccddeeff>]";
    let error = convert(pdf(&[DRAWING], [0.0, 0.0, 100.0, 100.0], encrypt))
        .err()
        .unwrap();
    assert!(error.contains("verschlüsselt"), "{error}");
}

#[test]
fn reads_pdf_files_from_disk() {
    let path = std::env::temp_dir().join(format!("visicut-pdf-test-{}.pdf", std::process::id()));
    std::fs::write(&path, pdf(&[DRAWING], [0.0, 0.0, 300.0, 150.0], "")).unwrap();
    let imported = crate::import::read_file(&path);
    let _ = std::fs::remove_file(&path);
    assert!(imported.unwrap().svg.contains("#0000ff"));
}

#[test]
fn hairline_width_follows_the_path_transform() {
    assert_eq!(transform_scale("matrix(0.1 0 0 -0.1 0 200)"), 0.1);
    assert_eq!(transform_scale("scale(2 2)"), 2.0);
    assert_eq!(transform_scale("translate(5 -3)"), 1.0);
    let svg = postprocess(
        "<svg viewBox=\"0 0 10 10\" width=\"10\" height=\"10\" xmlns=\"http://www.w3.org/2000/svg\">\
         <path d=\"M0,0 L1,1\" stroke-width=\"0\" fill=\"none\" stroke=\"#000000\" transform=\"scale(0.5 0.5)\"/></svg>",
        10.0,
        10.0,
    )
    .unwrap();
    let expected = HAIRLINE_MM / MM_PER_PT / 0.5;
    assert!(
        svg.contains(&format!("stroke-width=\"{expected}\"")),
        "{svg}"
    );
    assert!(svg.contains("width=\"3.5277"), "{svg}");
}

fn ghostscript_available() -> bool {
    let found = ghostscript::find().is_some();
    if !found {
        eprintln!("Ghostscript nicht gefunden; EPS-Test übersprungen");
    }
    found
}

struct TempPath(PathBuf);
impl Drop for TempPath {
    fn drop(&mut self) {
        let _ = std::fs::remove_file(&self.0);
    }
}

#[test]
fn imports_eps_through_ghostscript() {
    if !ghostscript_available() {
        return;
    }
    // BoundingBox 10 10 → 154 82 pt: 144 × 72 pt = 50.8 × 25.4 mm.
    let eps = "%!PS-Adobe-3.0 EPSF-3.0\n%%BoundingBox: 10 10 154 82\n%%EndComments\n\
        1 0 0 setrgbcolor 2 setlinewidth newpath 20 20 moveto 100 0 rlineto 0 50 rlineto \
        -100 0 rlineto closepath stroke\n\
        0 0 1 setrgbcolor newpath 130 46 20 0 360 arc closepath fill\n\
        /Helvetica findfont 12 scalefont setfont 0 setgray 25 30 moveto (Hi) show\n\
        showpage\n%%EOF\n";
    let path =
        TempPath(std::env::temp_dir().join(format!("visicut-eps-test-{}.eps", std::process::id())));
    std::fs::write(&path.0, eps).unwrap();
    let imported = crate::import::read_file(&path.0).unwrap();
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
    assert!(imported.svg.contains("#ff0000"), "{}", imported.svg);
    assert!(imported.svg.contains("#0000ff"));
    let project = project(&imported.svg);
    let contours = crate::geometry::contours(&project).unwrap();
    // Rectangle, circle and the outlines of "Hi" (at least H, i stem, i dot).
    assert!(contours.len() >= 5, "{}", contours.len());
}

#[test]
fn reports_ghostscript_errors() {
    if !ghostscript_available() {
        return;
    }
    let path =
        TempPath(std::env::temp_dir().join(format!("visicut-ps-test-{}.ps", std::process::id())));
    std::fs::write(&path.0, "%!PS\nthis_is_not_an_operator\n").unwrap();
    let error = crate::import::read_file(&path.0).err().unwrap();
    assert!(error.contains("Ghostscript"), "{error}");
}
