//! Byte comparison with the original Java pipeline.
//!
//! Every folder in `tests/java_parity` holds an input SVG, its settings
//! (`case.properties`) and `expected.ltt`, the job written by Java VisiCut
//! with LibLaserCut's LaserToolsTechnicsCutter for the FAU device. The
//! references are regenerated with `scripts/java-parity/generate.sh`.

use std::{collections::HashMap, fs, path::Path};
use visicut_core::{
    ltt,
    project::{JobStep, Operation, ParameterSet, Project},
    raster::{Dithering, RasterSettings},
};

fn properties(text: &str) -> HashMap<String, String> {
    text.lines()
        .map(str::trim)
        .filter(|l| !l.is_empty() && !l.starts_with('#'))
        .filter_map(|l| l.split_once('='))
        .map(|(k, v)| (k.trim().to_owned(), v.trim().to_owned()))
        .collect()
}

fn project(dir: &Path, case: &HashMap<String, String>) -> Project {
    let get = |key: &str| {
        case.get(key)
            .unwrap_or_else(|| panic!("{}: {key} fehlt", dir.display()))
    };
    let number = |key: &str| -> f32 { get(key).parse().unwrap() };
    let operation = match get("operation").as_str() {
        "cut" => Operation::Cut,
        "mark" => Operation::Mark,
        "engrave" => Operation::Engrave,
        "engrave3d" => Operation::Engrave3d,
        other => panic!("{}: unbekanntes Verfahren {other}", dir.display()),
    };
    let mut project = Project {
        name: get("name").clone(),
        svg: fs::read_to_string(dir.join(get("svg"))).unwrap(),
        x_mm: number("x_mm"),
        y_mm: number("y_mm"),
        width_mm: number("width_mm"),
        height_mm: number("height_mm"),
        operation,
        power_percent: number("power"),
        speed_percent: number("speed"),
        passes: case.get("passes").map_or(1, |p| p.parse().unwrap()),
        thickness_mm: case.get("thickness_mm").map_or(0.0, |t| t.parse().unwrap()),
        raster: raster(dir, case),
        ..Project::default()
    };
    // Further parameter sets need a step; filters `[]` select every object.
    if let Some(additional) = case.get("additional") {
        project.steps = vec![JobStep {
            filters: Some(Vec::new()),
            power_percent: project.power_percent,
            speed_percent: project.speed_percent,
            passes: project.passes,
            raster: project.raster,
            additional: additional
                .split(';')
                .map(|set| {
                    let v: Vec<&str> = set.trim().split(':').collect();
                    ParameterSet {
                        power_percent: v[0].parse().unwrap(),
                        speed_percent: v[1].parse().unwrap(),
                        passes: v[2].parse().unwrap(),
                    }
                })
                .collect(),
            ..JobStep::new(operation)
        }];
    }
    project
}

/// Raster options under the names of the Java harness (LttParity.java).
fn raster(dir: &Path, case: &HashMap<String, String>) -> RasterSettings {
    let flag = |key: &str| case.get(key).is_some_and(|v| v == "true");
    let dithering = match case.get("dither").map_or("FloydSteinberg", String::as_str) {
        "FloydSteinberg" => Dithering::FloydSteinberg,
        "Average" => Dithering::Average,
        "Random" => Dithering::Random,
        "Ordered" => Dithering::Ordered,
        "Grid" => Dithering::Grid,
        "Halftone" => Dithering::Halftone,
        "BrightenedHalftone" => Dithering::BrightenedHalftone,
        other => panic!("{}: unbekanntes Rasterverfahren {other}", dir.display()),
    };
    RasterSettings {
        dithering,
        invert: flag("invert"),
        color_shift: case.get("color_shift").map_or(0, |v| v.parse().unwrap()),
        bidirectional: !flag("unidirectional"),
        bottom_up: flag("bottom_up"),
    }
}

/// First differing offset with a little context, for a readable failure.
fn difference(actual: &[u8], expected: &[u8]) -> Option<String> {
    let at = actual
        .iter()
        .zip(expected)
        .position(|(a, e)| a != e)
        .or_else(|| (actual.len() != expected.len()).then(|| actual.len().min(expected.len())))?;
    let window = |bytes: &[u8]| {
        bytes[at.saturating_sub(8)..(at + 24).min(bytes.len())]
            .iter()
            .map(|b| format!("{b:02x}"))
            .collect::<Vec<_>>()
            .join(" ")
    };
    Some(format!(
        "erste Abweichung bei Byte {at} (Rust {} Bytes, Java {} Bytes)\n  Rust: {}\n  Java: {}",
        actual.len(),
        expected.len(),
        window(actual),
        window(expected)
    ))
}

#[test]
fn ltt_output_matches_java_byte_for_byte() {
    let root = Path::new(env!("CARGO_MANIFEST_DIR")).join("tests/java_parity");
    let mut dirs: Vec<_> = fs::read_dir(&root)
        .unwrap()
        .map(|e| e.unwrap().path())
        .filter(|p| p.join("case.properties").exists())
        .collect();
    dirs.sort();
    assert!(!dirs.is_empty(), "keine Vergleichsfälle gefunden");
    let mut failures = Vec::new();
    for dir in &dirs {
        let name = dir.file_name().unwrap().to_string_lossy().into_owned();
        let case = properties(&fs::read_to_string(dir.join("case.properties")).unwrap());
        let expected_file = fs::read(dir.join("expected.ltt"))
            .unwrap_or_else(|_| panic!("{name}: expected.ltt fehlt, generate.sh ausführen"));
        let prepared = match ltt::prepare(&project(dir, &case)) {
            Ok(prepared) => prepared,
            Err(e) => {
                failures.push(format!("{name}: Rust lehnt ab: {e}"));
                continue;
            }
        };
        if prepared.jobs.len() != 1 {
            failures.push(format!("{name}: {} Aufträge statt 1", prepared.jobs.len()));
            continue;
        }
        let mut actual = prepared.jobs[0].bytes.as_slice();
        let mut expected = expected_file.as_slice();
        // `compare=header`: only up to the first raster line, for documented
        // deliberate differences in the lines (reason in case.properties).
        if case.get("compare").is_some_and(|c| c == "header") {
            let header = |bytes: &[u8]| {
                bytes
                    .windows(2)
                    .position(|w| w == [0x1b, b'0'] || w == [0x1b, b'1'])
                    .unwrap_or(bytes.len())
            };
            expected = &expected[..header(expected)];
            actual = &actual[..header(actual).max(expected.len()).min(actual.len())];
        }
        if let Some(diff) = difference(actual, expected) {
            // Kept for inspection next to the build output.
            let path = Path::new(env!("CARGO_TARGET_TMPDIR")).join(format!("{name}.ltt"));
            fs::write(&path, actual).unwrap();
            failures.push(format!(
                "{name}: {diff}\n  Rust-Ausgabe: {}",
                path.display()
            ));
        }
    }
    assert!(
        failures.is_empty(),
        "{} von {} Fällen weichen von Java ab:\n{}",
        failures.len(),
        dirs.len(),
        failures.join("\n")
    );
}
