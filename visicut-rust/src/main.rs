mod devices_ui;
mod jobs_ui;
mod vectorize_ui;

use eframe::egui::{self, Color32, Pos2, Rect, Sense, Stroke, Vec2};
use std::path::{Path, PathBuf};
use visicut_core::{
    import, ltt,
    project::{Operation, Project},
    svg,
};

struct VisiCutRust {
    project: Project,
    texture: Option<egui::TextureHandle>,
    status: String,
    project_path: Option<PathBuf>,
    dirty: bool,
    proportional: bool,
    confirm_close: bool,
    allow_close: bool,
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
        let (devices, device_error) = devices_ui::DeviceUi::load();
        let (jobs, material_error) = jobs_ui::JobUi::load();
        let mut app = Self {
            project: Project::default(),
            texture: None,
            status: "SVG importieren oder das Beispiel öffnen".into(),
            project_path: None,
            dirty: false,
            proportional: true,
            confirm_close: false,
            allow_close: false,
            send_result: None,
            devices,
            jobs,
            vectorize: vectorize_ui::VectorizeUi::default(),
            #[cfg(feature = "screenshot")]
            capture: None,
        };
        app.devices.apply(&mut app.project);
        if let Some(error) = device_error.or(material_error) {
            app.status = error;
        }
        app.project.material.clear();
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
            if let Err(error) = app.import(
                &cc.egui_ctx,
                include_str!("../examples/demo.svg").into(),
                "Beispiel".into(),
            ) {
                app.status = error;
            }
        } else if let Some(path) = arguments.first()
            && let Err(error) = app.open_path(&cc.egui_ctx, Path::new(path))
        {
            app.status = error;
        }
        cc.egui_ctx.request_repaint();
        app
    }

    fn import(&mut self, ctx: &egui::Context, source: String, name: String) -> Result<(), String> {
        let preview = svg::render(&source)?;
        self.project.svg = source;
        self.project.name = name;
        self.project.width_mm = preview.width_mm;
        self.project.height_mm = preview.height_mm;
        self.texture = Some(ctx.load_texture("SVG", preview.image, egui::TextureOptions::LINEAR));
        self.dirty = true;
        self.status = "Datei importiert · Maße aus der Datei übernommen".into();
        Ok(())
    }

    fn open_path(&mut self, ctx: &egui::Context, path: &Path) -> Result<(), String> {
        let meta = std::fs::metadata(path).map_err(|e| e.to_string())?;
        if meta.len() > 25 * 1024 * 1024 {
            return Err("Datei ist größer als 25 MB".into());
        }
        if import::is_importable(path) {
            let imported = import::read_file(path)?;
            let name = path.file_stem().unwrap_or_default().to_string_lossy();
            self.import(ctx, imported.svg, name.into())?;
            for warning in imported.warnings {
                self.status = format!("{} · {warning}", self.status);
            }
            Ok(())
        } else {
            let source = std::fs::read_to_string(path).map_err(|e| e.to_string())?;
            let project: Project =
                serde_json::from_str(&source).map_err(|e| format!("Ungültiges Projekt: {e}"))?;
            project.validate()?;
            let preview = svg::render(&project.svg)?;
            self.texture =
                Some(ctx.load_texture("SVG", preview.image, egui::TextureOptions::LINEAR));
            self.project = project;
            self.devices.apply(&mut self.project);
            self.project_path = Some(path.to_owned());
            self.dirty = false;
            self.status = "Projekt geöffnet".into();
            Ok(())
        }
    }

    fn save(&mut self) -> Result<(), String> {
        self.project.validate()?;
        let path = self.project_path.clone().or_else(|| {
            rfd::FileDialog::new()
                .add_filter("VisiCutRust Projekt", &["vcr"])
                .set_file_name(format!("{}.vcr", self.project.name))
                .save_file()
        });
        if let Some(path) = path {
            let bytes = serde_json::to_vec_pretty(&self.project).map_err(|e| e.to_string())?;
            // Write beside the destination, then replace it atomically.
            let temporary = path.with_extension(format!("vcr.{}.tmp", std::process::id()));
            use std::io::Write;
            let mut file = std::fs::OpenOptions::new()
                .write(true)
                .create_new(true)
                .open(&temporary)
                .map_err(|e| e.to_string())?;
            file.write_all(&bytes).map_err(|e| e.to_string())?;
            file.sync_all().map_err(|e| e.to_string())?;
            drop(file);
            std::fs::rename(&temporary, &path).map_err(|e| e.to_string())?;
            self.project_path = Some(path);
            self.dirty = false;
            self.status = "Projekt mit eingebetteter SVG gespeichert".into();
        }
        Ok(())
    }

    fn open_dialog(&mut self, ctx: &egui::Context) {
        if self.dirty
            && !rfd::MessageDialog::new()
                .set_title("Ungespeicherte Änderungen")
                .set_description("Änderungen verwerfen und eine Datei öffnen?")
                .set_buttons(rfd::MessageButtons::YesNo)
                .show()
                .eq(&rfd::MessageDialogResult::Yes)
        {
            return;
        }
        if let Some(path) = rfd::FileDialog::new()
            .add_filter(
                "Grafik oder Rust-Projekt",
                &[import::EXTENSIONS, &["vcr"]].concat(),
            )
            .add_filter("Grafik", import::EXTENSIONS)
            .add_filter("VisiCutRust Projekt", &["vcr"])
            .pick_file()
            && let Err(e) = self.open_path(ctx, &path)
        {
            self.status = e;
        }
    }

    fn export_job(&mut self) -> Result<(), String> {
        let prepared = ltt::prepare(&self.project)?;
        if let Some(folder) = rfd::FileDialog::new()
            .set_title("Ordner für LTT-Aufträge")
            .pick_folder()
        {
            for job in &prepared.jobs {
                let path = folder.join(format!("{}.ltt", job.name));
                if path.exists() {
                    return Err(format!(
                        "{} existiert bereits; anderen Ordner wählen",
                        path.display()
                    ));
                }
            }
            for job in &prepared.jobs {
                std::fs::write(folder.join(format!("{}.ltt", job.name)), &job.bytes)
                    .map_err(|e| e.to_string())?;
            }
            self.status = format!(
                "{} LTT-Aufträge exportiert · {}",
                prepared.jobs.len(),
                prepared.description
            );
        }
        Ok(())
    }

    fn send_job(&mut self) -> Result<(), String> {
        let prepared = ltt::prepare(&self.project)?;
        let names = prepared
            .jobs
            .iter()
            .map(|job| job.name.as_str())
            .collect::<Vec<_>>()
            .join(", ");
        let approved = rfd::MessageDialog::new().set_title("LTT-Job übertragen")
            .set_description(format!("{}\nZiel: {}:{}\n{}\nLeistung: {} % · Geschwindigkeit: {} %\n\n{}Der Job wird ohne Autostart übertragen. Vor dem Start am Gerät Fokus, Material und Druckluft prüfen.\n\nDieser Rust-Treiber ist noch nicht am echten Gerät validiert. Job jetzt übertragen?",
                self.project.name, self.project.hostname, self.project.port, names,
                self.project.power_percent, self.project.speed_percent,
                prepared.warnings.iter().map(|w| format!("{w}\n\n")).collect::<String>()))
            .set_buttons(rfd::MessageButtons::YesNo).show();
        if approved != rfd::MessageDialogResult::Yes {
            return Ok(());
        }
        let host = self.project.hostname.clone();
        let port = self.project.port;
        let (tx, rx) = std::sync::mpsc::channel();
        std::thread::spawn(move || {
            let _ = tx.send(ltt::transmit_jobs(&host, port, &prepared.jobs));
        });
        self.send_result = Some(rx);
        self.status = "LTT-Job wird übertragen …".into();
        Ok(())
    }

    fn sidebar(&mut self, ui: &mut egui::Ui) {
        ui.heading("Job vorbereiten");
        ui.add_space(12.0);
        ui.label("LTT iLaser 4000 · FAU FabLab");
        self.dirty |= ui.text_edit_singleline(&mut self.project.name).changed();
        ui.add_space(12.0);
        ui.label(egui::RichText::new("ARBEITSBETT").strong().small());
        self.dirty |= number(
            ui,
            "Breite",
            &mut self.project.bed_width_mm,
            1.0..=10000.0,
            " mm",
        );
        self.dirty |= number(
            ui,
            "Höhe",
            &mut self.project.bed_height_mm,
            1.0..=10000.0,
            " mm",
        );
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
        if ui.button("Auf dem Bett zentrieren").clicked() {
            self.project.x_mm =
                ((self.project.bed_width_mm - self.project.width_mm) / 2.0).max(0.0);
            self.project.y_mm =
                ((self.project.bed_height_mm - self.project.height_mm) / 2.0).max(0.0);
            self.dirty = true;
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
        match self.project.validate() {
            Ok(()) => {
                ui.colored_label(
                    Color32::from_rgb(34, 130, 85),
                    "✓ Motiv passt auf das Arbeitsbett",
                );
            }
            Err(e) => {
                ui.colored_label(Color32::from_rgb(180, 75, 40), e);
            }
        }
        ui.add_space(10.0);
        ui.separator();
        self.devices
            .sidebar(ui, &mut self.project, &mut self.dirty, &mut self.status);
        ui.separator();
        if ui.button("LTT-Datei exportieren …").clicked()
            && let Err(e) = self.export_job()
        {
            self.status = e;
        }
        if ui
            .add_enabled(
                self.send_result.is_none(),
                egui::Button::new("An Lasercutter senden …"),
            )
            .clicked()
            && let Err(e) = self.send_job()
        {
            self.status = e;
        }
        ui.small("Übertragung ohne Autostart · Treiber experimentell");
        ui.small("Parameter sind Entwurfswerte, keine Materialempfehlungen.");
    }

    fn canvas(&mut self, ui: &mut egui::Ui) {
        ui.heading("Arbeitsbereich");
        ui.label(format!(
            "{} × {} mm · Ursprung oben links · Motiv zum Verschieben ziehen",
            self.project.bed_width_mm, self.project.bed_height_mm
        ));
        let space = ui.available_size() - Vec2::splat(36.0);
        let scale = (space.x / self.project.bed_width_mm)
            .min(space.y / self.project.bed_height_mm)
            .max(0.01);
        let size = Vec2::new(self.project.bed_width_mm, self.project.bed_height_mm) * scale;
        let (rect, _) = ui.allocate_exact_size(size + Vec2::splat(24.0), Sense::hover());
        let bed = Rect::from_min_size(rect.min + Vec2::splat(12.0), size);
        let painter = ui.painter().with_clip_rect(bed);
        painter.rect_filled(bed, 0.0, Color32::WHITE);
        let camera = self.devices.paint_camera(&painter, bed);
        // Coarsen the grid at small scales to bound the number of painted lines.
        let step = if camera {
            f32::INFINITY
        } else {
            (10.0 * scale).max(12.0)
        };
        let grid = Stroke::new(0.5, Color32::from_gray(228));
        let mut x = bed.left();
        while x <= bed.right() {
            painter.line_segment([Pos2::new(x, bed.top()), Pos2::new(x, bed.bottom())], grid);
            x += step;
        }
        let mut y = bed.top();
        while y <= bed.bottom() {
            painter.line_segment([Pos2::new(bed.left(), y), Pos2::new(bed.right(), y)], grid);
            y += step;
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
            painter.rect_stroke(
                motif,
                0.0,
                Stroke::new(1.0, Color32::from_rgb(52, 116, 170)),
                egui::StrokeKind::Inside,
            );
            let response = ui.interact(motif.intersect(bed), ui.id().with("motif"), Sense::drag());
            if response.dragged() {
                let delta = ui.input(|i| i.pointer.delta()) / scale;
                self.project.x_mm = (self.project.x_mm + delta.x).clamp(
                    0.0,
                    (self.project.bed_width_mm - self.project.width_mm).max(0.0),
                );
                self.project.y_mm = (self.project.y_mm + delta.y).clamp(
                    0.0,
                    (self.project.bed_height_mm - self.project.height_mm).max(0.0),
                );
                self.dirty = true;
            }
        } else {
            painter.text(
                bed.center(),
                egui::Align2::CENTER_CENTER,
                "SVG importieren",
                egui::FontId::proportional(24.0),
                Color32::from_gray(145),
            );
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
        if let Some((path, frames)) = &mut self.capture {
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
        if let Some(receiver) = &self.send_result {
            match receiver.try_recv() {
                Ok(result) => {
                    self.status = match result {
                        Ok(names) => {
                            let text = self
                                .devices
                                .device()
                                .job_sent_text
                                .replace("$jobname", &names.join(", "));
                            if !text.trim().is_empty() {
                                rfd::MessageDialog::new()
                                    .set_title(format!(
                                        "An {} übertragen",
                                        self.devices.device().name
                                    ))
                                    .set_description(text)
                                    .show();
                            }
                            "Bytes übertragen. Job am Gerät prüfen; Autofokus und Druckluft vor dem Start sicherstellen.".into()
                        }
                        Err(e) => e,
                    };
                    self.send_result = None;
                }
                Err(std::sync::mpsc::TryRecvError::Disconnected) => {
                    self.status = "Übertragung unerwartet abgebrochen; Gerätestatus prüfen".into();
                    self.send_result = None;
                }
                Err(std::sync::mpsc::TryRecvError::Empty) => {
                    ctx.request_repaint_after(std::time::Duration::from_millis(100));
                }
            }
        }
        if ctx.input(|i| i.viewport().close_requested()) && self.send_result.is_some() {
            ctx.send_viewport_cmd(egui::ViewportCommand::CancelClose);
            self.status = "Bitte die laufende Übertragung vor dem Beenden abwarten".into();
        } else if ctx.input(|i| i.viewport().close_requested()) && self.dirty && !self.allow_close {
            ctx.send_viewport_cmd(egui::ViewportCommand::CancelClose);
            self.confirm_close = true;
        }
        if self.confirm_close {
            egui::Window::new("Ungespeicherte Änderungen")
                .collapsible(false)
                .resizable(false)
                .show(ctx, |ui| {
                    ui.label("Vor dem Beenden speichern?");
                    ui.horizontal(|ui| {
                        if ui.button("Speichern").clicked() {
                            if let Err(e) = self.save() {
                                self.status = e;
                            }
                            if !self.dirty {
                                self.allow_close = true;
                                ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                            }
                        }
                        if ui.button("Verwerfen").clicked() {
                            self.allow_close = true;
                            ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                        }
                        if ui.button("Abbrechen").clicked() {
                            self.confirm_close = false;
                        }
                    });
                });
        }
        self.jobs.windows(ctx, &mut self.status);
        if let Some((svg, name)) = self.vectorize.windows(ctx, self.dirty, &mut self.status) {
            // Vektorisierte Bitmaps laufen über denselben Importpfad wie SVG-Dateien.
            if let Err(e) = self.import(ctx, svg, name) {
                self.status = e;
            }
        }
        if let Some(devices_ui::Action::CalibrationPage(points)) =
            self.devices
                .windows(ctx, &mut self.project, &mut self.status)
        {
            if self.dirty {
                self.status = "Zuerst das aktuelle Projekt speichern".into();
            } else {
                let (w, h) = (self.project.bed_width_mm, self.project.bed_height_mm);
                let svg = visicut_core::camera::calibration_svg(&points, w as f64, h as f64);
                match self.import(ctx, svg, "Kalibrierung".into()) {
                    Ok(()) => {
                        self.project.steps.clear();
                        self.project.rotary_axis = false;
                        (self.project.x_mm, self.project.y_mm) = (0.0, 0.0);
                        self.project.operation = Operation::Mark;
                        self.project_path = None;
                        self.status =
                            "Kalibrierseite geöffnet: Markier-Parameter prüfen und senden".into();
                    }
                    Err(e) => self.status = e,
                }
            }
        }
        let open = ctx.input_mut(|i| i.consume_key(egui::Modifiers::COMMAND, egui::Key::O));
        let save = ctx.input_mut(|i| i.consume_key(egui::Modifiers::COMMAND, egui::Key::S));
        if open {
            self.open_dialog(ctx);
        }
        if save && let Err(e) = self.save() {
            self.status = e;
        }
        egui::TopBottomPanel::top("toolbar")
            .min_height(46.0)
            .show(ctx, |ui| {
                ui.add_space(6.0);
                ui.horizontal(|ui| {
                    ui.heading("VisiCutRust");
                    ui.label(
                        egui::RichText::new("RUST · MAC")
                            .small()
                            .color(Color32::from_gray(125)),
                    );
                    ui.separator();
                    if ui.button("Öffnen …  ⌘O").clicked() {
                        self.open_dialog(ctx);
                    }
                    if ui.button("Speichern …  ⌘S").clicked()
                        && let Err(e) = self.save()
                    {
                        self.status = e;
                    }
                    if ui
                        .add_enabled(!self.dirty, egui::Button::new("Beispiel"))
                        .clicked()
                        && let Err(e) = self.import(
                            ctx,
                            include_str!("../examples/demo.svg").into(),
                            "Beispiel".into(),
                        )
                    {
                        self.status = e;
                    }
                    if ui.button("Bitmap vektorisieren …").clicked() {
                        self.vectorize.pick(&mut self.status);
                    }
                    if self.dirty {
                        ui.label("• geändert");
                    }
                });
                ui.add_space(6.0);
            });
        egui::TopBottomPanel::bottom("status").show(ctx, |ui| {
            ui.label(&self.status);
        });
        egui::SidePanel::left("settings")
            .exact_width(285.0)
            .show(ctx, |ui| {
                egui::ScrollArea::vertical().show(ui, |ui| self.sidebar(ui));
            });
        egui::CentralPanel::default().show(ctx, |ui| self.canvas(ui));
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
            .with_inner_size([1180.0, 780.0])
            .with_min_inner_size([850.0, 600.0]),
        ..Default::default()
    };
    eframe::run_native(
        "VisiCutRust",
        options,
        Box::new(|cc| Ok(Box::new(VisiCutRust::new(cc)))),
    )
}
