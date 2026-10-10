//! Nicht-grafische Logik der egui-Arbeitsfläche: Zoom, Raster, Lineale,
//! Verschieben des Motivs und das Schreiben von LTT-Dateien.
use std::path::{Path, PathBuf};
use visicut_core::{ltt::PreparedStep, project::Project};

/// Zoomstufen wie in der macOS-Oberfläche: 50 % bis 300 % in 25-%-Schritten.
pub const ZOOM_MIN: f32 = 0.5;
pub const ZOOM_MAX: f32 = 3.0;
const ZOOM_STEP: f32 = 0.25;

pub fn zoom_in(zoom: f32) -> f32 {
    snap_zoom(zoom + ZOOM_STEP)
}

pub fn zoom_out(zoom: f32) -> f32 {
    snap_zoom(zoom - ZOOM_STEP)
}

fn snap_zoom(zoom: f32) -> f32 {
    ((zoom / ZOOM_STEP).round() * ZOOM_STEP).clamp(ZOOM_MIN, ZOOM_MAX)
}

/// Pixel je Millimeter, bei denen das ganze Bett in `available` passt.
pub fn fit_scale(available: [f32; 2], bed_mm: [f32; 2]) -> f32 {
    (available[0] / bed_mm[0])
        .min(available[1] / bed_mm[1])
        .max(0.01)
}

/// Rasterabstand in mm: 10 mm, gröber, sobald Linien näher als 12 px lägen
/// (wie macOS).
pub fn grid_step_mm(scale: f32) -> f32 {
    ((12.0 / scale / 10.0).ceil() * 10.0).max(10.0)
}

/// Abstand der Linealbeschriftung in mm, mindestens 40 px auseinander.
pub fn ruler_step_mm(scale: f32) -> f32 {
    [10.0, 20.0, 50.0, 100.0, 200.0, 500.0]
        .into_iter()
        .find(|step| step * scale >= 40.0)
        .unwrap_or(1000.0)
}

/// Positionen der Linealbeschriftung von 0 bis einschließlich `length`.
pub fn ruler_marks(length_mm: f32, step_mm: f32) -> Vec<f32> {
    let count = (length_mm / step_mm + 1e-3).floor() as usize;
    (0..=count).map(|i| i as f32 * step_mm).collect()
}

/// Größte zulässige Position, damit das Motiv auf dem Bett bleibt.
fn limit(bed: f32, size: f32) -> f32 {
    (bed - size).max(0.0)
}

/// Setzt die Position, auf das Arbeitsbett begrenzt; meldet eine Änderung.
pub fn place(project: &mut Project, x_mm: f32, y_mm: f32) -> bool {
    let x = x_mm.clamp(0.0, limit(project.bed_width_mm, project.width_mm));
    let y = y_mm.clamp(0.0, limit(project.bed_height_mm, project.height_mm));
    let changed = x != project.x_mm || y != project.y_mm;
    (project.x_mm, project.y_mm) = (x, y);
    changed
}

/// Verschiebt das Motiv (Pfeiltasten: 1 mm, mit Umschalt 10 mm).
pub fn nudge(project: &mut Project, dx_mm: f32, dy_mm: f32) -> bool {
    place(project, project.x_mm + dx_mm, project.y_mm + dy_mm)
}

/// Zentriert das Motiv auf dem Bett, wie macOS.
pub fn center(project: &mut Project) {
    project.x_mm = limit(project.bed_width_mm, project.width_mm) / 2.0;
    project.y_mm = limit(project.bed_height_mm, project.height_mm) / 2.0;
}

/// Zieldateien eines Exports in einen Ordner.
pub struct ExportPlan {
    pub targets: Vec<PathBuf>,
    /// Dateinamen, die schon existieren und ersetzt würden.
    pub existing: Vec<String>,
}

pub fn export_plan<'a>(folder: &Path, names: impl IntoIterator<Item = &'a str>) -> ExportPlan {
    let targets: Vec<PathBuf> = names
        .into_iter()
        .map(|name| folder.join(format!("{name}.ltt")))
        .collect();
    let existing = targets
        .iter()
        .filter(|path| path.exists())
        .map(|path| {
            path.file_name()
                .unwrap_or_default()
                .to_string_lossy()
                .into()
        })
        .collect();
    ExportPlan { targets, existing }
}

/// Schreibt neben das Ziel und ersetzt es dann in einem Schritt.
pub fn write_atomic(path: &Path, bytes: &[u8]) -> Result<(), String> {
    use std::io::Write;
    let mut name = path.file_name().unwrap_or_default().to_os_string();
    name.push(format!(".{}.tmp", std::process::id()));
    let temporary = path.with_file_name(name);
    let result = (|| {
        let mut file = std::fs::OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(&temporary)?;
        file.write_all(bytes)?;
        file.sync_all()?;
        drop(file);
        std::fs::rename(&temporary, path)
    })();
    if result.is_err() {
        let _ = std::fs::remove_file(&temporary);
    }
    result.map_err(|e| format!("{}: {e}", path.display()))
}

fn percent(value: f32) -> String {
    let text = format!("{value:.1}");
    text.trim_end_matches(".0").replace('.', ",")
}

/// Parameter eines Schritts für die Sende-Bestätigung.
pub fn step_parameters(step: &PreparedStep) -> String {
    let mut text = format!(
        "Leistung {} % · Geschwindigkeit {} % · {} {}",
        percent(step.power_percent),
        percent(step.speed_percent),
        step.passes,
        if step.passes == 1 {
            "Durchgang"
        } else {
            "Durchgänge"
        }
    );
    if step.parameter_sets > 1 {
        text += &format!(" · {} Parametersätze", step.parameter_sets);
    }
    text
}

/// Dauer wie „ca. 3 min 05 s“.
pub fn duration(seconds: f64) -> String {
    let total = seconds.max(0.0).round() as u64;
    match (total / 3600, total / 60 % 60, total % 60) {
        (0, 0, s) => format!("{s} s"),
        (0, m, s) => format!("{m} min {s:02} s"),
        (h, m, _) => format!("{h} h {m:02} min"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use visicut_core::project::Operation;

    #[test]
    fn zoom_steps_are_bounded_and_snapped() {
        assert_eq!(zoom_in(1.0), 1.25);
        assert_eq!(zoom_out(1.0), 0.75);
        assert_eq!(zoom_in(ZOOM_MAX), ZOOM_MAX);
        assert_eq!(zoom_out(ZOOM_MIN), ZOOM_MIN);
        assert_eq!(zoom_in(1.1), 1.25);
    }

    #[test]
    fn fit_scale_uses_the_tighter_axis() {
        assert_eq!(fit_scale([1000.0, 1200.0], [1000.0, 600.0]), 1.0);
        assert_eq!(fit_scale([2000.0, 600.0], [1000.0, 600.0]), 1.0);
        assert_eq!(fit_scale([0.0, 0.0], [1000.0, 600.0]), 0.01);
    }

    #[test]
    fn grid_and_ruler_steps_stay_readable() {
        assert_eq!(grid_step_mm(2.0), 10.0);
        assert_eq!(grid_step_mm(0.5), 30.0);
        assert!(grid_step_mm(0.5) * 0.5 >= 12.0);
        assert_eq!(ruler_step_mm(4.0), 10.0);
        assert_eq!(ruler_step_mm(0.6), 100.0);
        assert_eq!(ruler_step_mm(0.01), 1000.0);
        assert_eq!(ruler_marks(600.0, 200.0), vec![0.0, 200.0, 400.0, 600.0]);
        assert_eq!(ruler_marks(650.0, 100.0).len(), 7);
    }

    fn motif() -> Project {
        Project {
            x_mm: 10.0,
            y_mm: 10.0,
            width_mm: 100.0,
            height_mm: 60.0,
            ..Project::default()
        }
    }

    #[test]
    fn nudge_moves_and_stops_at_the_bed_edges() {
        let mut p = motif();
        assert!(nudge(&mut p, 1.0, -10.0));
        assert_eq!((p.x_mm, p.y_mm), (11.0, 0.0));
        assert!(!nudge(&mut p, 0.0, -1.0));
        assert!(nudge(&mut p, 10_000.0, 10_000.0));
        assert_eq!((p.x_mm, p.y_mm), (900.0, 540.0));
        // Ein Motiv größer als das Bett bleibt am Ursprung.
        p.width_mm = 1200.0;
        assert!(nudge(&mut p, 5.0, 0.0));
        assert_eq!(p.x_mm, 0.0);
    }

    #[test]
    fn center_matches_macos() {
        let mut p = motif();
        center(&mut p);
        assert_eq!((p.x_mm, p.y_mm), (450.0, 270.0));
        p.height_mm = 700.0;
        center(&mut p);
        assert_eq!(p.y_mm, 0.0);
    }

    #[test]
    fn export_plan_reports_existing_files_and_writes_atomically() {
        let folder =
            std::env::temp_dir().join(format!("visicut-export-test-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&folder);
        std::fs::create_dir_all(&folder).unwrap();
        std::fs::write(folder.join("Cut.ltt"), b"alt").unwrap();
        let plan = export_plan(&folder, ["Engrav", "Cut"]);
        assert_eq!(plan.targets.len(), 2);
        assert_eq!(plan.existing, vec!["Cut.ltt".to_string()]);
        write_atomic(&plan.targets[1], b"neu").unwrap();
        assert_eq!(std::fs::read(folder.join("Cut.ltt")).unwrap(), b"neu");
        let leftovers = std::fs::read_dir(&folder).unwrap().count();
        assert_eq!(leftovers, 1, "keine temporären Dateien zurücklassen");
        assert!(write_atomic(&folder.join("fehlt/x.ltt"), b"").is_err());
        std::fs::remove_dir_all(&folder).unwrap();
    }

    #[test]
    fn step_parameters_and_duration_are_readable() {
        let step = PreparedStep {
            operation: Operation::Cut,
            name: "Cut".into(),
            description: String::new(),
            estimated_seconds: 0.0,
            power_percent: 80.0,
            speed_percent: 1.5,
            passes: 2,
            parameter_sets: 3,
        };
        assert_eq!(
            step_parameters(&step),
            "Leistung 80 % · Geschwindigkeit 1,5 % · 2 Durchgänge · 3 Parametersätze"
        );
        assert_eq!(duration(42.4), "42 s");
        assert_eq!(duration(185.0), "3 min 05 s");
        assert_eq!(duration(3_900.0), "1 h 05 min");
    }
}
