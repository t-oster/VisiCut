mod devices_ui;
mod jobs_ui;
mod preview_ui;
mod vectorize_ui;
mod workspace;

use eframe::egui::{
    self, Color32, Key, KeyboardShortcut, Modifiers, Pos2, Rect, Sense, Stroke, Vec2,
};
use std::path::{Path, PathBuf};
use visicut_core::{
    import,
    ltt::{self, OutputJob, PreparedJob},
    project::{Operation, Project},
    svg,
};

const WARNING: Color32 = Color32::from_rgb(180, 75, 40);
const OK: Color32 = Color32::from_rgb(34, 130, 85);

/// Befehle aus Menü und Tastatur. `Modifiers::COMMAND` ist Strg unter
/// Windows und Linux (⌘ unter macOS).
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Command {
    New,
    Open,
    Demo,
    Vectorize,
    Save,
    SaveAs,
    Export,
    Quit,
    Center,
    Devices,
    ToggleGrid,
    ToggleCamera,
    RefreshCamera,
    ZoomIn,
    ZoomOut,
    ZoomFit,
    Materials,
    Preview,
    Send,
}

const COMMAND_SHIFT: Modifiers = Modifiers::COMMAND.plus(Modifiers::SHIFT);

impl Command {
    /// Reihenfolge der Tastaturprüfung: Kürzel mit Umschalt zuerst, weil egui
    /// zusätzliche Umschalttasten beim Vergleich ignoriert.
    const KEYBOARD: [Command; 15] = [
        Self::SaveAs,
        Self::Export,
        Self::Materials,
        Self::RefreshCamera,
        Self::Preview,
        Self::New,
        Self::Open,
        Self::Save,
        Self::Quit,
        Self::Devices,
        Self::ToggleCamera,
        Self::ZoomFit,
        Self::ZoomIn,
        Self::ZoomOut,
        Self::ToggleGrid,
    ];

    fn title(self) -> &'static str {
        match self {
            Self::New => "Neues Projekt",
            Self::Open => "Öffnen …",
            Self::Demo => "Beispiel öffnen",
            Self::Vectorize => "Bitmap vektorisieren …",
            Self::Save => "Sichern",
            Self::SaveAs => "Sichern unter …",
            Self::Export => "LTT-Job exportieren …",
            Self::Quit => "Beenden",
            Self::Center => "Motiv auf dem Arbeitsbett zentrieren",
            Self::Devices => "Lasercutter verwalten …",
            Self::ToggleGrid => "Raster anzeigen",
            Self::ToggleCamera => "Kamerabild anzeigen",
            Self::RefreshCamera => "Kamerabild aktualisieren",
            Self::ZoomIn => "Vergrößern",
            Self::ZoomOut => "Verkleinern",
            Self::ZoomFit => "Ansicht einpassen",
            Self::Materials => "Materialbibliothek …",
            Self::Preview => "Vorschau & Zeit …",
            Self::Send => "An Lasercutter senden …",
        }
    }

    fn shortcut(self) -> Option<KeyboardShortcut> {
        let (modifiers, key) = match self {
            Self::New => (Modifiers::COMMAND, Key::N),
            Self::Open => (Modifiers::COMMAND, Key::O),
            Self::Save => (Modifiers::COMMAND, Key::S),
            Self::SaveAs => (COMMAND_SHIFT, Key::S),
            Self::Export => (COMMAND_SHIFT, Key::E),
            Self::Quit => (Modifiers::COMMAND, Key::Q),
            Self::Devices => (Modifiers::COMMAND, Key::Comma),
            Self::ToggleCamera => (Modifiers::COMMAND, Key::K),
            Self::RefreshCamera => (COMMAND_SHIFT, Key::K),
            Self::ZoomIn => (Modifiers::COMMAND, Key::Plus),
            Self::ZoomOut => (Modifiers::COMMAND, Key::Minus),
            Self::ZoomFit => (Modifiers::COMMAND, Key::Num0),
            Self::Materials => (COMMAND_SHIFT, Key::M),
            Self::Preview => (COMMAND_SHIFT, Key::P),
            Self::ToggleGrid => (Modifiers::COMMAND, Key::G),
            Self::Demo | Self::Vectorize | Self::Center | Self::Send => return None,
        };
        Some(KeyboardShortcut::new(modifiers, key))
    }
}

/// Aktionen, die ungesicherte Änderungen verwerfen würden.
enum Replace {
    New,
    Open(PathBuf),
    Demo,
    Vectorized { svg: String, name: String },
    Calibration(Vec<[f64; 2]>),
    Quit,
}

/// Mehrere LTT-Aufträge warten auf die Bestätigung zum Ersetzen.
struct PendingExport {
    targets: Vec<PathBuf>,
    existing: Vec<String>,
    jobs: Vec<OutputJob>,
}

struct VisiCutRust {
    project: Project,
    texture: Option<egui::TextureHandle>,
    status: String,
    /// Deutlich sichtbarer Hinweis (Fehler, Importwarnungen) in einem Dialog.
    alert: Option<String>,
    project_path: Option<PathBuf>,
    dirty: bool,
    proportional: bool,
    allow_close: bool,
    zoom: f32,
    show_grid: bool,
    /// Projekt ist vollständig und passt aufs Bett (pro Bild neu bestimmt).
    sendable: bool,
    /// Wartet auf „Sichern / Nicht sichern / Abbrechen“.
    replace: Option<Replace>,
    overwrite: Option<PendingExport>,
    confirm_send: Option<PreparedJob>,
    preview: preview_ui::PreviewUi,
    /// „Vorschau & Zeit“ (Menü, Seitenleiste, Strg+Umschalt+P) setzt dieses
    /// Feld; das Fenster liest und schließt es.
    show_preview: bool,
    send_result: Option<std::sync::mpsc::Receiver<Result<Vec<String>, String>>>,
    devices: devices_ui::DeviceUi,
    jobs: jobs_ui::JobUi,
    vectorize: vectorize_ui::VectorizeUi,
    #[cfg(feature = "screenshot")]
    capture: Option<(PathBuf, u32)>,
}

impl VisiCutRust {
    fn new(cc: &eframe::CreationContext<'_>) -> Self {
        cc.egui_ctx.set_visuals(egui::Visuals::light());
        // Strg+Plus/Minus/0 zoomen die Arbeitsfläche, nicht die Oberfläche.
        cc.egui_ctx.options_mut(|o| o.zoom_with_keyboard = false);
        let (devices, device_error) = devices_ui::DeviceUi::load();
        let (jobs, material_error) = jobs_ui::JobUi::load();
        let mut app = Self {
            project: Project::default(),
            texture: None,
            status: "Datei öffnen, hierher ziehen oder das Beispiel öffnen".into(),
            alert: None,
            project_path: None,
            dirty: false,
            proportional: true,
            allow_close: false,
            zoom: 1.0,
            show_grid: true,
            sendable: false,
            replace: None,
            overwrite: None,
            confirm_send: None,
            show_preview: false,
            preview: preview_ui::PreviewUi::default(),
            send_result: None,
            devices,
            jobs,
            vectorize: vectorize_ui::VectorizeUi::default(),
            #[cfg(feature = "screenshot")]
            capture: None,
        };
        app.reset_project();
        if let Some(error) = device_error {
            app.report(format!(
                "Geräteliste nicht lesbar, FAU-Standard wird verwendet: {error}"
            ));
        } else if let Some(error) = material_error {
            app.report(error);
        }
        let arguments: Vec<String> = std::env::args().skip(1).collect();
        #[cfg(feature = "screenshot")]
        if let Some(index) = arguments
            .iter()
            .position(|argument| argument == "--capture")
        {
            app.capture = arguments
                .get(index + 1)
                .map(|path| (PathBuf::from(path), 0));
        }
        if arguments
            .first()
            .is_some_and(|argument| argument == "--demo")
        {
            app.perform(&cc.egui_ctx, Replace::Demo);
        } else if let Some(path) = arguments.first().filter(|a| !a.starts_with('-')) {
            app.perform(&cc.egui_ctx, Replace::Open(PathBuf::from(path)));
        }
        cc.egui_ctx.request_repaint();
        app
    }

    /// Fehler und Warnungen: Statuszeile und ein Hinweisdialog.
    fn report(&mut self, message: String) {
        self.status = message.lines().next().unwrap_or_default().to_owned();
        self.alert = Some(match self.alert.take() {
            Some(previous) if previous != message => format!("{previous}\n\n{message}"),
            _ => message,
        });
    }

    fn busy(&self) -> bool {
        self.send_result.is_some()
    }

    fn modal_open(&self) -> bool {
        self.alert.is_some()
            || self.replace.is_some()
            || self.overwrite.is_some()
            || self.confirm_send.is_some()
    }

    fn reset_project(&mut self) {
        self.project = Project::default();
        self.project.material.clear();
        self.devices.apply(&mut self.project);
        self.texture = None;
        self.project_path = None;
        self.dirty = false;
    }

    /// Übernimmt eine Grafik. Wie bei jedem Import gelten alte Schritte und
    /// Regeln nicht mehr; PLF-Dateien bringen ihre eigenen Schritte mit.
    fn import(
        &mut self,
        ctx: &egui::Context,
        source: String,
        name: String,
        steps: Vec<visicut_core::project::JobStep>,
    ) -> Result<(), String> {
        let preview = svg::render(&source)?;
        self.project.svg = source;
        self.project.name = name;
        self.project.width_mm = preview.width_mm;
        self.project.height_mm = preview.height_mm;
        self.project.steps = steps;
        self.project.ignore_filters.clear();
        self.texture = Some(ctx.load_texture("SVG", preview.image, egui::TextureOptions::LINEAR));
        self.project_path = None;
        self.dirty = true;
        self.status = "Datei importiert · Maße aus der Datei übernommen".into();
        Ok(())
    }

    fn open_path(&mut self, ctx: &egui::Context, path: &Path) -> Result<(), String> {
        let meta = std::fs::metadata(path)
            .map_err(|e| format!("{} ist nicht lesbar: {e}", path.display()))?;
        if meta.len() > import::MAX_FILE_BYTES {
            return Err("Datei ist größer als 25 MB".into());
        }
        if import::is_importable(path) {
            let imported = import::read_file(path)?;
            let name = path.file_stem().unwrap_or_default().to_string_lossy();
            self.import(ctx, imported.svg, name.into(), imported.steps)?;
            self.status = format!(
                "{} importiert · Originalmaße übernommen",
                path.file_name().unwrap_or_default().to_string_lossy()
            );
            if !imported.warnings.is_empty() {
                self.alert = Some(imported.warnings.join("\n"));
            }
            Ok(())
        } else {
            let source = std::fs::read_to_string(path).map_err(|e| e.to_string())?;
            let project: Project =
                serde_json::from_str(&source).map_err(|e| format!("Ungültiges Projekt: {e}"))?;
            // Entwürfe ohne Motiv oder außerhalb des Betts lassen sich öffnen.
            project.validate_document()?;
            self.texture = if project.svg.is_empty() {
                None
            } else {
                let preview = svg::render(&project.svg)?;
                Some(ctx.load_texture("SVG", preview.image, egui::TextureOptions::LINEAR))
            };
            self.project = project;
            if self.project.material == "Material wählen" {
                self.project.material.clear();
            }
            self.devices.apply(&mut self.project);
            self.project_path = Some(path.to_owned());
            self.dirty = false;
            self.status = "Projekt geöffnet".into();
            Ok(())
        }
    }

    /// Sichert das Projekt; `false`, wenn der Dialog abgebrochen wurde.
    fn save(&mut self, as_copy: bool) -> Result<bool, String> {
        self.project.validate_document()?;
        let path = self.project_path.clone().filter(|_| !as_copy).or_else(|| {
            rfd::FileDialog::new()
                .set_title("VisiCutRust-Projekt sichern")
                .add_filter("VisiCutRust Projekt", &["vcr"])
                .set_file_name(format!("{}.vcr", self.project.name))
                .save_file()
        });
        let Some(path) = path else {
            return Ok(false);
        };
        let bytes = serde_json::to_vec_pretty(&self.project).map_err(|e| e.to_string())?;
        workspace::write_atomic(&path, &bytes)?;
        self.project_path = Some(path);
        self.dirty = false;
        self.status = "Projekt mit eingebetteter SVG gesichert".into();
        Ok(true)
    }

    /// Führt eine Aktion aus oder fragt vorher nach ungesicherten Änderungen.
    fn request(&mut self, ctx: &egui::Context, action: Replace) {
        if self.busy() && !matches!(action, Replace::Quit) {
            self.status = "Bitte die laufende Übertragung abwarten".into();
        } else if self.dirty {
            self.replace = Some(action);
        } else {
            self.perform(ctx, action);
        }
    }

    fn perform(&mut self, ctx: &egui::Context, action: Replace) {
        let result = match action {
            Replace::New => {
                self.reset_project();
                self.status = "Neues Projekt. Datei öffnen, um zu beginnen.".into();
                Ok(())
            }
            Replace::Open(path) => self.open_path(ctx, &path),
            Replace::Demo => self
                .import(
                    ctx,
                    include_str!("../examples/demo.svg").into(),
                    "Beispiel".into(),
                    Vec::new(),
                )
                .map(|()| {
                    self.status = "Beispiel geöffnet. Material und Bearbeitung auswählen.".into();
                }),
            Replace::Vectorized { svg, name } => self.import(ctx, svg, name, Vec::new()),
            Replace::Calibration(points) => self.calibration_page(ctx, &points),
            Replace::Quit => {
                self.allow_close = true;
                ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                Ok(())
            }
        };
        if let Err(e) = result {
            self.report(e);
        }
    }

    /// VisiCuts Kalibriermarken als Markierauftrag auf dem ganzen Bett.
    fn calibration_page(&mut self, ctx: &egui::Context, points: &[[f64; 2]]) -> Result<(), String> {
        let [w, h] = devices_ui::BED_MM.map(f64::from);
        if points
            .iter()
            .any(|p| !(5.0..=w - 5.0).contains(&p[0]) || !(5.0..=h - 15.0).contains(&p[1]))
        {
            return Err(
                "Kalibrierpunkte müssen mindestens 5 mm (unten 15 mm) vom Bettrand entfernt sein"
                    .into(),
            );
        }
        let svg = visicut_core::camera::calibration_svg(points, w, h);
        self.import(ctx, svg, "Kalibrierung".into(), Vec::new())?;
        self.project.rotary_axis = false;
        (self.project.x_mm, self.project.y_mm) = (0.0, 0.0);
        self.project.operation = Operation::Mark;
        self.status = "Kalibrierseite geöffnet: Material und Markier-Parameter prüfen, senden, danach Foto aufnehmen.".into();
        Ok(())
    }

    fn choose_file(&mut self, ctx: &egui::Context) {
        if self.busy() {
            return;
        }
        if let Some(path) = rfd::FileDialog::new()
            .set_title("Grafik oder VisiCutRust-Projekt öffnen")
            .add_filter(
                "Grafik oder VisiCutRust-Projekt",
                &[import::EXTENSIONS, &["vcr", "json"]].concat(),
            )
            .add_filter("Grafik", import::EXTENSIONS)
            .add_filter("VisiCutRust-Projekt", &["vcr", "json"])
            .pick_file()
        {
            self.request(ctx, Replace::Open(path));
        }
    }

    fn export(&mut self) {
        if !self.sendable {
            return;
        }
        let prepared = match ltt::prepare(&self.project) {
            Ok(prepared) => prepared,
            Err(e) => return self.report(e),
        };
        let mut jobs = prepared.jobs;
        if jobs.len() == 1 {
            let job = jobs.remove(0);
            let Some(path) = rfd::FileDialog::new()
                .set_title("LTT-Auftrag exportieren")
                .add_filter("LTT-Auftrag", &["ltt"])
                .set_file_name(format!("{}.ltt", job.name))
                .save_file()
            else {
                return;
            };
            match workspace::write_atomic(&path, &job.bytes) {
                Ok(()) => self.status = format!("Exportiert: {}", path.display()),
                Err(e) => self.report(e),
            }
            return;
        }
        let Some(folder) = rfd::FileDialog::new()
            .set_title(format!(
                "Ordner für {} getrennte LTT-Aufträge wählen",
                jobs.len()
            ))
            .pick_folder()
        else {
            return;
        };
        let plan = workspace::export_plan(&folder, jobs.iter().map(|job| job.name.as_str()));
        let pending = PendingExport {
            targets: plan.targets,
            existing: plan.existing,
            jobs,
        };
        if pending.existing.is_empty() {
            self.write_export(pending);
        } else {
            self.overwrite = Some(pending);
        }
    }

    fn write_export(&mut self, pending: PendingExport) {
        for (path, job) in pending.targets.iter().zip(&pending.jobs) {
            if let Err(e) = workspace::write_atomic(path, &job.bytes) {
                return self.report(e);
            }
        }
        self.status = format!(
            "Exportiert: {}",
            pending
                .jobs
                .iter()
                .map(|job| job.name.as_str())
                .collect::<Vec<_>>()
                .join(", ")
        );
    }

    fn send(&mut self) {
        if !self.sendable {
            return;
        }
        match ltt::prepare(&self.project) {
            Ok(prepared) => self.confirm_send = Some(prepared),
            Err(e) => self.report(e),
        }
    }

    fn transmit(&mut self, prepared: PreparedJob) {
        let host = self.project.hostname.clone();
        let port = self.project.port;
        let (tx, rx) = std::sync::mpsc::channel();
        std::thread::spawn(move || {
            let _ = tx.send(ltt::transmit_jobs(&host, port, &prepared.jobs));
        });
        self.send_result = Some(rx);
        self.status = "Job wird an den Lasercutter übertragen …".into();
    }

    /// Hook für die Materialbibliothek aus jobs_ui.rs: Dort fehlt noch eine
    /// öffentliche Methode zum Öffnen des Editors (z. B. `open_library()`).
    fn open_material_library(&mut self) {
        self.jobs.open_library();
    }

    fn enabled(&self, command: Command) -> bool {
        match command {
            Command::New
            | Command::Open
            | Command::Demo
            | Command::Vectorize
            | Command::Devices => !self.busy(),
            Command::Export | Command::Send | Command::Preview => self.sendable,
            Command::Center => self.texture.is_some() && !self.busy(),
            Command::ToggleCamera => self.devices.has_camera(),
            Command::RefreshCamera => self.devices.camera_shown() && !self.devices.camera_loading(),
            Command::ZoomIn => self.zoom < workspace::ZOOM_MAX,
            Command::ZoomOut => self.zoom > workspace::ZOOM_MIN,
            Command::Save
            | Command::SaveAs
            | Command::Quit
            | Command::ToggleGrid
            | Command::ZoomFit
            | Command::Materials => true,
        }
    }

    fn run(&mut self, ctx: &egui::Context, command: Command) {
        if !self.enabled(command) {
            return;
        }
        match command {
            Command::New => self.request(ctx, Replace::New),
            Command::Open => self.choose_file(ctx),
            Command::Demo => self.request(ctx, Replace::Demo),
            Command::Vectorize => self.vectorize.pick(&mut self.status),
            Command::Save | Command::SaveAs => {
                if let Err(e) = self.save(command == Command::SaveAs) {
                    self.report(e);
                }
            }
            Command::Export => self.export(),
            Command::Quit => ctx.send_viewport_cmd(egui::ViewportCommand::Close),
            Command::Center => {
                workspace::center(&mut self.project);
                self.dirty = true;
            }
            Command::Devices => self.devices.open_manager(),
            Command::ToggleGrid => self.show_grid = !self.show_grid,
            Command::ToggleCamera => self.devices.toggle_camera(&self.project),
            Command::RefreshCamera => self.devices.refresh_camera(&self.project),
            Command::ZoomIn => self.zoom = workspace::zoom_in(self.zoom),
            Command::ZoomOut => self.zoom = workspace::zoom_out(self.zoom),
            Command::ZoomFit => self.zoom = 1.0,
            Command::Materials => self.open_material_library(),
            Command::Preview => self.show_preview = true,
            Command::Send => self.send(),
        }
    }

    fn keyboard(&mut self, ctx: &egui::Context) {
        if self.modal_open() {
            return;
        }
        let mut commands = Vec::new();
        ctx.input_mut(|i| {
            for command in Command::KEYBOARD {
                if command
                    .shortcut()
                    .is_some_and(|shortcut| i.consume_shortcut(&shortcut))
                {
                    commands.push(command);
                }
            }
            // „+“ liegt auf vielen Tastaturen auf der Taste mit „=“.
            if i.consume_key(Modifiers::COMMAND, Key::Equals) {
                commands.push(Command::ZoomIn);
            }
        });
        for command in commands {
            self.run(ctx, command);
        }
        // Pfeiltasten verschieben das Motiv, solange kein Eingabefeld aktiv ist.
        if self.texture.is_none() || self.busy() || ctx.memory(|m| m.focused().is_some()) {
            return;
        }
        let (dx, dy) = ctx.input_mut(|i| {
            let step = if i.modifiers.shift { 10.0 } else { 1.0 };
            let mut delta = (0.0, 0.0);
            for (key, dx, dy) in [
                (Key::ArrowLeft, -step, 0.0),
                (Key::ArrowRight, step, 0.0),
                (Key::ArrowUp, 0.0, -step),
                (Key::ArrowDown, 0.0, step),
            ] {
                let presses = i.count_and_consume_key(Modifiers::SHIFT, key)
                    + i.count_and_consume_key(Modifiers::NONE, key);
                delta.0 += dx * presses as f32;
                delta.1 += dy * presses as f32;
            }
            delta
        });
        if (dx, dy) != (0.0, 0.0) && workspace::nudge(&mut self.project, dx, dy) {
            self.dirty = true;
        }
    }

    fn menu_item(&self, ui: &mut egui::Ui, command: Command, clicked: &mut Option<Command>) {
        let mut button = egui::Button::new(command.title());
        if let Some(shortcut) = command.shortcut() {
            button = button.shortcut_text(ui.ctx().format_shortcut(&shortcut));
        }
        let checked = match command {
            Command::ToggleGrid => Some(self.show_grid),
            Command::ToggleCamera => Some(self.devices.camera_shown()),
            _ => None,
        };
        if let Some(checked) = checked {
            button = button.selected(checked);
        }
        if ui.add_enabled(self.enabled(command), button).clicked() {
            *clicked = Some(command);
        }
    }

    fn menu_bar(&self, ui: &mut egui::Ui) -> Option<Command> {
        let mut clicked = None;
        egui::MenuBar::new().ui(ui, |ui| {
            ui.menu_button("Datei", |ui| {
                for command in [
                    Command::New,
                    Command::Open,
                    Command::Demo,
                    Command::Vectorize,
                ] {
                    self.menu_item(ui, command, &mut clicked);
                }
                ui.separator();
                for command in [Command::Save, Command::SaveAs, Command::Export] {
                    self.menu_item(ui, command, &mut clicked);
                }
                ui.separator();
                self.menu_item(ui, Command::Quit, &mut clicked);
            });
            ui.menu_button("Bearbeiten", |ui| {
                self.menu_item(ui, Command::Center, &mut clicked);
                ui.add_enabled(
                    false,
                    egui::Button::new("Motiv verschieben").shortcut_text("Pfeiltasten"),
                )
                .on_disabled_hover_text("1 mm je Tastendruck, mit Umschalt 10 mm");
                ui.separator();
                self.menu_item(ui, Command::Devices, &mut clicked);
            });
            ui.menu_button("Darstellung", |ui| {
                for command in [Command::ZoomIn, Command::ZoomOut, Command::ZoomFit] {
                    self.menu_item(ui, command, &mut clicked);
                }
                self.menu_item(ui, Command::ToggleGrid, &mut clicked);
                ui.separator();
                self.menu_item(ui, Command::ToggleCamera, &mut clicked);
                self.menu_item(ui, Command::RefreshCamera, &mut clicked);
            });
            ui.menu_button("Job", |ui| {
                for command in [Command::Materials, Command::Preview] {
                    self.menu_item(ui, command, &mut clicked);
                }
                ui.separator();
                self.menu_item(ui, Command::Send, &mut clicked);
            });
        });
        clicked
    }

    fn sidebar(&mut self, ui: &mut egui::Ui) -> Option<Command> {
        let mut clicked = None;
        ui.heading("Job vorbereiten");
        ui.add_space(8.0);
        ui.label("Projektname");
        self.dirty |= ui.text_edit_singleline(&mut self.project.name).changed();
        ui.add_space(8.0);
        self.devices
            .sidebar(ui, &mut self.project, &mut self.dirty, &mut self.status);
        ui.small(format!(
            "Arbeitsbett {} × {} mm (durch den Lasercutter festgelegt)",
            self.project.bed_width_mm, self.project.bed_height_mm
        ));
        ui.separator();
        ui.label(egui::RichText::new("POSITION & GRÖSSE").strong().small());
        self.dirty |= number(ui, "X", &mut self.project.x_mm, 0.0..=10000.0, " mm");
        self.dirty |= number(ui, "Y", &mut self.project.y_mm, 0.0..=10000.0, " mm");
        let ratio = self.project.height_mm / self.project.width_mm;
        if number(
            ui,
            "Breite",
            &mut self.project.width_mm,
            0.1..=10000.0,
            " mm",
        ) {
            if self.proportional {
                self.project.height_mm = self.project.width_mm * ratio;
            }
            self.dirty = true;
        }
        if number(
            ui,
            "Höhe",
            &mut self.project.height_mm,
            0.1..=10000.0,
            " mm",
        ) {
            if self.proportional {
                self.project.width_mm = self.project.height_mm / ratio;
            }
            self.dirty = true;
        }
        ui.checkbox(&mut self.proportional, "Seitenverhältnis beibehalten");
        if ui
            .add_enabled(
                self.enabled(Command::Center),
                egui::Button::new("Auf dem Bett zentrieren"),
            )
            .clicked()
        {
            clicked = Some(Command::Center);
        }
        ui.separator();
        ui.label(
            egui::RichText::new("MATERIAL & BEARBEITUNG")
                .strong()
                .small(),
        );
        self.jobs.material(ui, &mut self.project, &mut self.dirty);
        ui.separator();
        ui.label(egui::RichText::new("ZUORDNUNG").strong().small());
        self.jobs.processing(ui, &mut self.project, &mut self.dirty);
        ui.add_space(16.0);
        if self.project.svg.is_empty() {
            ui.colored_label(Color32::from_gray(110), "Noch kein Motiv importiert");
        } else {
            match self.project.validate() {
                Ok(()) => {
                    ui.colored_label(OK, "Motiv passt auf das Arbeitsbett");
                }
                Err(e) => {
                    ui.colored_label(WARNING, format!("⚠ {e}"));
                }
            }
        }
        ui.add_space(10.0);
        ui.separator();
        for command in [Command::Preview, Command::Export, Command::Send] {
            let label = match command {
                Command::Export => "LTT-Datei exportieren …",
                _ => command.title(),
            };
            if ui
                .add_enabled(self.enabled(command), egui::Button::new(label))
                .clicked()
            {
                clicked = Some(command);
            }
        }
        ui.small("Übertragung ohne Autostart · Treiber experimentell");
        ui.small("Parameter sind Entwurfswerte, keine Materialempfehlungen.");
        clicked
    }

    fn canvas(&mut self, ui: &mut egui::Ui) -> Option<Command> {
        let mut clicked = None;
        ui.horizontal(|ui| {
            ui.vertical(|ui| {
                ui.heading(if self.texture.is_some() {
                    self.project.name.as_str()
                } else {
                    "Arbeitsbereich"
                });
                ui.label(format!(
                    "{} · {} × {} mm{}",
                    self.devices.device().name,
                    self.project.bed_width_mm,
                    self.project.bed_height_mm,
                    if self.project.rotary_axis {
                        " · Drehachse"
                    } else {
                        ""
                    }
                ));
            });
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                let tool = |ui: &mut egui::Ui, command: Command, text: &str| {
                    let mut hover = command.title().to_owned();
                    if let Some(shortcut) = command.shortcut() {
                        hover += &format!(" ({})", ui.ctx().format_shortcut(&shortcut));
                    }
                    ui.add_enabled(self.enabled(command), egui::Button::new(text))
                        .on_hover_text(hover)
                        .clicked()
                        .then_some(command)
                };
                let mut chosen = Vec::new();
                chosen.extend(tool(ui, Command::ZoomFit, "Einpassen"));
                chosen.extend(tool(ui, Command::ZoomIn, "+"));
                ui.label(format!("{:.0} %", self.zoom * 100.0));
                chosen.extend(tool(ui, Command::ZoomOut, "−"));
                ui.separator();
                let mut grid = self.show_grid;
                if ui.checkbox(&mut grid, "Raster").changed() {
                    clicked = Some(Command::ToggleGrid);
                }
                if self.devices.has_camera() {
                    if self.devices.camera_loading() {
                        ui.spinner();
                    } else if self.devices.camera_shown() {
                        chosen.extend(tool(ui, Command::RefreshCamera, "Aktualisieren"));
                    }
                    let mut camera = self.devices.camera_shown();
                    if ui.checkbox(&mut camera, "Kamera").changed() {
                        clicked = Some(Command::ToggleCamera);
                    }
                }
                ui.separator();
                self.preview.toolbar(ui, &self.project, &mut self.status);
                if let Some(command) = chosen.pop() {
                    clicked = Some(command);
                }
            });
        });
        ui.separator();
        const RULER: f32 = 30.0;
        const MARGIN: f32 = 14.0;
        let bed_mm = Vec2::new(self.project.bed_width_mm, self.project.bed_height_mm);
        let available = ui.available_size() - Vec2::splat(RULER + MARGIN);
        let scale =
            workspace::fit_scale([available.x, available.y], [bed_mm.x, bed_mm.y]) * self.zoom;
        let panel = ui.max_rect();
        egui::ScrollArea::both().auto_shrink(false).show(ui, |ui| {
            let size = bed_mm * scale + Vec2::splat(RULER + MARGIN);
            let (rect, response) = ui.allocate_exact_size(size, Sense::click());
            if response.clicked()
                && let Some(id) = ui.memory(|m| m.focused())
            {
                ui.memory_mut(|m| m.surrender_focus(id));
            }
            let bed = Rect::from_min_size(rect.min + Vec2::splat(RULER), bed_mm * scale);
            self.rulers(ui.painter(), bed, scale);
            let painter = ui.painter().with_clip_rect(bed.intersect(ui.clip_rect()));
            painter.rect_filled(bed, 0.0, Color32::WHITE);
            let camera = self.devices.paint_camera(&painter, bed);
            if self.show_grid && !camera {
                let step = workspace::grid_step_mm(scale);
                let grid = Stroke::new(0.5, Color32::from_gray(222));
                for x in workspace::ruler_marks(bed_mm.x, step) {
                    let x = bed.left() + x * scale;
                    painter
                        .line_segment([Pos2::new(x, bed.top()), Pos2::new(x, bed.bottom())], grid);
                }
                for y in workspace::ruler_marks(bed_mm.y, step) {
                    let y = bed.top() + y * scale;
                    painter
                        .line_segment([Pos2::new(bed.left(), y), Pos2::new(bed.right(), y)], grid);
                }
            }
            painter.rect_stroke(
                bed,
                0.0,
                Stroke::new(1.0, Color32::from_gray(145)),
                egui::StrokeKind::Inside,
            );
            if let Some(texture) = &self.texture {
                let motif = Rect::from_min_size(
                    bed.min + Vec2::new(self.project.x_mm, self.project.y_mm) * scale,
                    Vec2::new(self.project.width_mm, self.project.height_mm) * scale,
                );
                painter.image(
                    texture.id(),
                    motif,
                    Rect::from_min_max(Pos2::ZERO, Pos2::new(1.0, 1.0)),
                    Color32::WHITE,
                );
                self.preview.overlay(&painter, motif);
                painter.rect_stroke(
                    motif,
                    0.0,
                    Stroke::new(1.5, Color32::from_rgb(52, 116, 170)),
                    egui::StrokeKind::Inside,
                );
                let response = ui
                    .interact(motif.intersect(bed), ui.id().with("motif"), Sense::drag())
                    .on_hover_cursor(egui::CursorIcon::Grab);
                if response.dragged() && !self.busy() {
                    let delta = response.drag_delta() / scale;
                    let (x, y) = (self.project.x_mm + delta.x, self.project.y_mm + delta.y);
                    if workspace::place(&mut self.project, x, y) {
                        self.dirty = true;
                    }
                }
            } else {
                let open = ui
                    .ctx()
                    .format_shortcut(&Command::Open.shortcut().expect("Öffnen hat ein Kürzel"));
                painter.text(
                    bed.center(),
                    egui::Align2::CENTER_CENTER,
                    format!("Datei öffnen ({open}) oder hierher ziehen"),
                    egui::FontId::proportional(22.0),
                    Color32::from_gray(145),
                );
            }
        });
        if ui.ctx().input(|i| !i.raw.hovered_files.is_empty()) {
            let painter = ui.ctx().layer_painter(egui::LayerId::new(
                egui::Order::Foreground,
                egui::Id::new("drop"),
            ));
            painter.rect_filled(
                panel,
                6.0,
                Color32::from_rgba_unmultiplied(52, 116, 170, 60),
            );
            painter.text(
                panel.center(),
                egui::Align2::CENTER_CENTER,
                "Loslassen zum Öffnen",
                egui::FontId::proportional(26.0),
                Color32::from_rgb(30, 70, 110),
            );
        }
        clicked
    }

    /// Lineale mit mm-Beschriftung oben und links vom Bett.
    fn rulers(&self, painter: &egui::Painter, bed: Rect, scale: f32) {
        let step = workspace::ruler_step_mm(scale);
        let color = Color32::from_gray(110);
        let font = egui::FontId::monospace(10.0);
        let tick = Stroke::new(1.0, Color32::from_gray(160));
        for x in workspace::ruler_marks(self.project.bed_width_mm, step) {
            let px = bed.left() + x * scale;
            painter.line_segment(
                [Pos2::new(px, bed.top() - 6.0), Pos2::new(px, bed.top())],
                tick,
            );
            painter.text(
                Pos2::new(px, bed.top() - 7.0),
                egui::Align2::CENTER_BOTTOM,
                format!("{x:.0}"),
                font.clone(),
                color,
            );
        }
        for y in workspace::ruler_marks(self.project.bed_height_mm, step) {
            let py = bed.top() + y * scale;
            painter.line_segment(
                [Pos2::new(bed.left() - 6.0, py), Pos2::new(bed.left(), py)],
                tick,
            );
            painter.text(
                Pos2::new(bed.left() - 8.0, py),
                egui::Align2::RIGHT_CENTER,
                format!("{y:.0}"),
                font.clone(),
                color,
            );
        }
        painter.text(
            bed.min - Vec2::splat(8.0),
            egui::Align2::RIGHT_BOTTOM,
            "mm",
            font,
            color,
        );
    }

    /// Dialoge: Hinweis, ungesicherte Änderungen, Ersetzen, Senden.
    fn dialogs(&mut self, ctx: &egui::Context) {
        if let Some(action) = self.replace.take() {
            let mut choice = None;
            let modal = egui::Modal::new(egui::Id::new("replace")).show(ctx, |ui| {
                ui.set_max_width(380.0);
                ui.heading(format!("Änderungen an „{}“ sichern?", self.project.name));
                ui.label("Nicht gesicherte Änderungen gehen verloren.");
                ui.add_space(8.0);
                ui.horizontal(|ui| {
                    if ui.button("Sichern …").clicked() {
                        choice = Some(true);
                    }
                    if ui.button("Nicht sichern").clicked() {
                        choice = Some(false);
                    }
                    if ui.button("Abbrechen").clicked() {
                        ui.close();
                    }
                });
            });
            match choice {
                Some(true) => match self.save(false) {
                    Ok(true) => self.perform(ctx, action),
                    Ok(false) => {}
                    Err(e) => self.report(format!("Sichern nicht möglich: {e}")),
                },
                Some(false) => self.perform(ctx, action),
                None if modal.should_close() => {}
                None => self.replace = Some(action),
            }
        }
        if let Some(pending) = self.overwrite.take() {
            let mut replace = false;
            let modal = egui::Modal::new(egui::Id::new("overwrite")).show(ctx, |ui| {
                ui.heading("Vorhandene LTT-Dateien ersetzen?");
                for name in &pending.existing {
                    ui.monospace(name);
                }
                ui.add_space(8.0);
                ui.horizontal(|ui| {
                    replace = ui.button("Ersetzen").clicked();
                    if ui.button("Abbrechen").clicked() {
                        ui.close();
                    }
                });
            });
            if replace {
                self.write_export(pending);
            } else if !modal.should_close() {
                self.overwrite = Some(pending);
            }
        }
        if let Some(prepared) = self.confirm_send.take() {
            let mut send = false;
            let modal = egui::Modal::new(egui::Id::new("send")).show(ctx, |ui| {
                self.send_summary(ui, &prepared);
                ui.add_space(8.0);
                ui.horizontal(|ui| {
                    send = ui.button("Senden").clicked();
                    if ui.button("Abbrechen").clicked() {
                        ui.close();
                    }
                });
            });
            if send {
                self.transmit(prepared);
            } else if !modal.should_close() {
                self.confirm_send = Some(prepared);
            }
        }
        // Hinweise zuletzt, damit sie über anderen Dialogen liegen.
        if let Some(text) = &self.alert {
            let mut close = false;
            let modal = egui::Modal::new(egui::Id::new("alert")).show(ctx, |ui| {
                ui.set_max_width(460.0);
                ui.horizontal(|ui| {
                    ui.label(egui::RichText::new("⚠").size(22.0).color(WARNING));
                    ui.heading("Hinweis");
                });
                egui::ScrollArea::vertical()
                    .max_height(320.0)
                    .show(ui, |ui| ui.label(text));
                ui.add_space(8.0);
                close = ui.button("OK").clicked();
            });
            if close || modal.should_close() {
                self.alert = None;
            }
        }
    }

    fn send_summary(&self, ui: &mut egui::Ui, prepared: &PreparedJob) {
        ui.set_max_width(560.0);
        let count = prepared.jobs.len();
        ui.heading(format!(
            "{count} {} an den Lasercutter senden?",
            if count == 1 { "Auftrag" } else { "Aufträge" }
        ));
        ui.label(format!(
            "{} · {}:{}",
            self.devices.device().name,
            self.project.hostname,
            self.project.port
        ));
        ui.label(format!(
            "Aufträge: {} (je Verfahren ein separater Auftrag) · ca. {}",
            prepared
                .jobs
                .iter()
                .map(|job| job.name.as_str())
                .collect::<Vec<_>>()
                .join(" → "),
            workspace::duration(prepared.estimated_seconds)
        ));
        ui.add_space(6.0);
        egui::ScrollArea::vertical()
            .max_height(260.0)
            .show(ui, |ui| {
                egui::Grid::new("send-steps")
                    .num_columns(3)
                    .striped(true)
                    .show(ui, |ui| {
                        ui.strong("Schritt");
                        ui.strong("Parameter");
                        ui.strong("Zeit");
                        ui.end_row();
                        for (index, step) in prepared.steps.iter().enumerate() {
                            ui.label(format!(
                                "{}. {}",
                                index + 1,
                                jobs_ui::operation_title(step.operation)
                            ))
                            .on_hover_text(&step.description);
                            ui.label(workspace::step_parameters(step));
                            ui.label(workspace::duration(step.estimated_seconds));
                            ui.end_row();
                        }
                    });
            });
        for warning in &prepared.warnings {
            ui.colored_label(WARNING, format!("⚠ {warning}"));
        }
        ui.add_space(6.0);
        ui.small(
            "Die Übertragung startet den Laser nicht. Aufträge am Gerät in dieser Reihenfolge \
             einzeln starten. Treiber am Gerät noch nicht validiert. Vor dem Start Material, \
             Fokus und Druckluft prüfen.",
        );
    }

    fn poll_send(&mut self, ctx: &egui::Context) {
        let Some(receiver) = &self.send_result else {
            return;
        };
        let result = match receiver.try_recv() {
            Ok(result) => result,
            Err(std::sync::mpsc::TryRecvError::Disconnected) => {
                Err("Übertragung unerwartet abgebrochen; Gerätestatus prüfen".into())
            }
            Err(std::sync::mpsc::TryRecvError::Empty) => {
                ctx.request_repaint_after(std::time::Duration::from_millis(100));
                return;
            }
        };
        self.send_result = None;
        match result {
            Ok(names) => {
                self.status = format!(
                    "Übertragen: {}. Aufträge am Gerät prüfen und einzeln starten.",
                    names.join(", ")
                );
                let text = self
                    .devices
                    .device()
                    .job_sent_text
                    .replace("$jobname", &names.join(", "));
                if !text.trim().is_empty() {
                    self.alert = Some(text);
                }
            }
            Err(e) => self.report(format!("Übertragung fehlgeschlagen: {e}")),
        }
    }

    #[cfg(feature = "screenshot")]
    fn capture(&mut self, ctx: &egui::Context) {
        let Some((path, frames)) = &mut self.capture else {
            return;
        };
        if let Some(image) = ctx.input(|i| {
            i.events.iter().find_map(|event| {
                if let egui::Event::Screenshot { image, .. } = event {
                    Some(image.clone())
                } else {
                    None
                }
            })
        }) {
            let pixels = image
                .pixels
                .iter()
                .flat_map(|pixel| pixel.to_array())
                .collect();
            let size =
                resvg::tiny_skia::IntSize::from_wh(image.size[0] as u32, image.size[1] as u32)
                    .unwrap();
            let pixmap = resvg::tiny_skia::Pixmap::from_vec(pixels, size).unwrap();
            if let Err(error) = pixmap.save_png(path) {
                eprintln!("{error}");
            }
            self.allow_close = true;
            ctx.send_viewport_cmd(egui::ViewportCommand::Close);
        } else {
            *frames += 1;
            if *frames == 10 {
                ctx.send_viewport_cmd(egui::ViewportCommand::Screenshot(Default::default()));
            }
            ctx.request_repaint_after(std::time::Duration::from_millis(50));
        }
    }
}

fn number(
    ui: &mut egui::Ui,
    label: &str,
    value: &mut f32,
    range: std::ops::RangeInclusive<f32>,
    suffix: &str,
) -> bool {
    ui.horizontal(|ui| {
        ui.label(label);
        ui.add(
            egui::DragValue::new(value)
                .speed(0.1)
                .range(range)
                .suffix(suffix),
        )
        .changed()
    })
    .inner
}

impl eframe::App for VisiCutRust {
    fn update(&mut self, ctx: &egui::Context, _: &mut eframe::Frame) {
        #[cfg(feature = "screenshot")]
        self.capture(ctx);
        self.poll_send(ctx);
        self.sendable = !self.busy() && self.project.validate().is_ok();
        if ctx.input(|i| i.viewport().close_requested()) && !self.allow_close {
            if self.busy() {
                ctx.send_viewport_cmd(egui::ViewportCommand::CancelClose);
                self.status = "Bitte die laufende Übertragung vor dem Beenden abwarten".into();
            } else if self.dirty {
                ctx.send_viewport_cmd(egui::ViewportCommand::CancelClose);
                self.replace = Some(Replace::Quit);
            }
        }
        // Aus dem Dateimanager gezogene Dateien öffnen (die erste zählt).
        let dropped = ctx.input(|i| i.raw.dropped_files.iter().find_map(|f| f.path.clone()));
        if let Some(path) = dropped
            && !self.modal_open()
        {
            self.request(ctx, Replace::Open(path));
        }
        self.keyboard(ctx);
        self.jobs.windows(ctx, &mut self.status);
        // Die Rückfrage bei ungesicherten Änderungen stellt `request`.
        if let Some((svg, name)) = self.vectorize.windows(ctx, false, &mut self.status) {
            self.request(ctx, Replace::Vectorized { svg, name });
        }
        if let Some(devices_ui::Action::CalibrationPage(points)) =
            self.devices
                .windows(ctx, &mut self.project, &mut self.status)
        {
            self.request(ctx, Replace::Calibration(points));
        }
        if self.show_preview {
            self.show_preview = false;
            self.preview.request(ctx, &self.project, &mut self.status);
        }
        let mut commands = Vec::new();
        let send_enabled = self.sendable && !self.busy();
        match self
            .preview
            .windows(ctx, &self.project, &mut self.status, send_enabled)
        {
            Some(preview_ui::Action::Send) => commands.push(Command::Send),
            Some(preview_ui::Action::Export) => commands.push(Command::Export),
            None => {}
        }
        egui::TopBottomPanel::top("menu").show(ctx, |ui| {
            commands.extend(self.menu_bar(ui));
        });
        egui::TopBottomPanel::bottom("status").show(ctx, |ui| {
            ui.horizontal(|ui| {
                if self.busy() {
                    ui.spinner();
                }
                ui.label(&self.status);
                if self.dirty {
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        ui.weak("geändert");
                    });
                }
            });
        });
        egui::SidePanel::left("settings")
            .exact_width(285.0)
            .show(ctx, |ui| {
                egui::ScrollArea::vertical().show(ui, |ui| commands.extend(self.sidebar(ui)));
            });
        egui::CentralPanel::default().show(ctx, |ui| commands.extend(self.canvas(ui)));
        for command in commands {
            self.run(ctx, command);
        }
        self.dialogs(ctx);
        ctx.send_viewport_cmd(egui::ViewportCommand::Title(format!(
            "{}{} — VisiCutRust",
            self.project.name,
            if self.dirty { " *" } else { "" }
        )));
    }
}

fn main() -> eframe::Result {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([1240.0, 820.0])
            .with_min_inner_size([900.0, 620.0])
            .with_drag_and_drop(true),
        ..Default::default()
    };
    eframe::run_native(
        "VisiCutRust",
        options,
        Box::new(|cc| Ok(Box::new(VisiCutRust::new(cc)))),
    )
}
