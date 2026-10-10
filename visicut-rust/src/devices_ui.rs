//! Lasercutter management, rotary axis and camera for the egui interface.
use eframe::egui::{self, Color32, Pos2, Rect, Sense, Stroke, Vec2};
use std::sync::mpsc::{Receiver, TryRecvError, channel};
use visicut_core::{
    camera,
    device::{self, CameraCalibration, DeviceStore, LaserDevice},
    project::Project,
};

type Pending = Receiver<Result<image::RgbaImage, String>>;
type Download = Receiver<Result<Vec<LaserDevice>, String>>;

/// Arbeitsbett des LTT iLaser 4000 in mm. Die Geräteimporte lehnen andere
/// Maße ab, deshalb legt das Gerät die Bettgröße fest (wie macOS).
pub const BED_MM: [f32; 2] = [1000.0, 600.0];

fn spawn(work: impl FnOnce() -> Result<image::RgbaImage, String> + Send + 'static) -> Pending {
    let (tx, rx) = channel();
    std::thread::spawn(move || {
        let _ = tx.send(work());
    });
    rx
}

/// Polls background work; `None` while it is still running.
fn poll(
    pending: &mut Option<Pending>,
    ctx: &egui::Context,
) -> Option<Result<image::RgbaImage, String>> {
    let result = match pending.as_ref()?.try_recv() {
        Ok(result) => result,
        Err(TryRecvError::Empty) => {
            ctx.request_repaint_after(std::time::Duration::from_millis(100));
            return None;
        }
        Err(TryRecvError::Disconnected) => Err("Kamera unerwartet abgebrochen".into()),
    };
    *pending = None;
    Some(result)
}

fn texture(ctx: &egui::Context, name: &str, image: &image::RgbaImage) -> egui::TextureHandle {
    let pixels = egui::ColorImage::from_rgba_unmultiplied(
        [image.width() as usize, image.height() as usize],
        image.as_raw(),
    );
    ctx.load_texture(name, pixels, egui::TextureOptions::LINEAR)
}

struct Calibration {
    device: LaserDevice,
    points: CameraCalibration,
    picture: Option<(egui::TextureHandle, Vec2)>,
    loading: Option<Pending>,
    selected: usize,
    problem: Option<String>,
}

pub enum Action {
    /// Open VisiCut's calibration marks for these bed points as a project.
    CalibrationPage(Vec<[f64; 2]>),
}

pub struct DeviceUi {
    pub store: DeviceStore,
    draft: Option<(DeviceStore, usize)>,
    show_camera: bool,
    camera: Option<egui::TextureHandle>,
    camera_loading: Option<Pending>,
    calibration: Option<Calibration>,
    /// Laufender Download von Labor-Einstellungen (Laborname, Ergebnis).
    download: Option<(String, Download)>,
}

impl DeviceUi {
    pub fn load() -> (Self, Option<String>) {
        let (store, error) = match DeviceStore::load(&device::config_dir()) {
            Ok(store) => (store, None),
            Err(e) => (
                DeviceStore::default(),
                Some(format!("Geräteliste nicht lesbar: {e}")),
            ),
        };
        let ui = Self {
            store,
            draft: None,
            show_camera: false,
            camera: None,
            camera_loading: None,
            calibration: None,
            download: None,
        };
        (ui, error)
    }

    pub fn device(&self) -> &LaserDevice {
        &self.store.devices[self.store.selected]
    }

    /// The selected device defines the job target, the bed and rotary availability.
    pub fn apply(&self, project: &mut Project) -> bool {
        let device = self.device();
        let changed = project.hostname != device.hostname
            || project.port != device.port
            || [project.bed_width_mm, project.bed_height_mm] != BED_MM
            || (project.rotary_axis && !device.rotary_axis);
        project.hostname = device.hostname.clone();
        project.port = device.port;
        [project.bed_width_mm, project.bed_height_mm] = BED_MM;
        project.rotary_axis &= device.rotary_axis;
        changed
    }

    /// Öffnet die Lasercutter-Verwaltung (oder lässt sie offen).
    pub fn open_manager(&mut self) {
        if self.draft.is_none() {
            self.draft = Some((self.store.clone(), self.store.selected));
        }
    }

    pub fn has_camera(&self) -> bool {
        !self.device().camera_url.is_empty()
    }

    pub fn camera_shown(&self) -> bool {
        self.show_camera
    }

    pub fn camera_loading(&self) -> bool {
        self.camera_loading.is_some()
    }

    /// Kamerabild ein- oder ausblenden; lädt es beim ersten Einblenden.
    pub fn toggle_camera(&mut self, project: &Project) {
        if !self.has_camera() {
            return;
        }
        self.show_camera = !self.show_camera;
        if self.show_camera && self.camera.is_none() {
            self.refresh_camera(project);
        }
    }

    fn save(&mut self, store: DeviceStore, project: &mut Project, status: &mut String) -> bool {
        if let Err(e) = store.save(&device::config_dir()) {
            *status = e;
            return false;
        }
        let camera_changed = {
            let (old, new) = (self.device(), &store.devices[store.selected]);
            old.camera_url != new.camera_url || old.camera_calibration != new.camera_calibration
        };
        self.store = store;
        if camera_changed {
            self.camera = None;
            self.show_camera = false;
        }
        self.apply(project);
        *status = format!(
            "Lasercutter: {} ({}:{})",
            self.device().name,
            self.device().hostname,
            self.device().port
        );
        true
    }

    pub fn refresh_camera(&mut self, project: &Project) {
        if self.camera_loading.is_some() || !self.has_camera() {
            return;
        }
        let device = self.device().clone();
        let (w, h) = (project.bed_width_mm as f64, project.bed_height_mm as f64);
        self.camera_loading = Some(spawn(move || camera::background_image(&device, w, h)));
    }

    pub fn sidebar(
        &mut self,
        ui: &mut egui::Ui,
        project: &mut Project,
        dirty: &mut bool,
        status: &mut String,
    ) {
        ui.label("Lasercutter");
        let mut selected = self.store.selected;
        egui::ComboBox::from_id_salt("device")
            .width(200.0)
            .selected_text(&self.device().name)
            .show_ui(ui, |ui| {
                for (i, device) in self.store.devices.iter().enumerate() {
                    ui.selectable_value(&mut selected, i, &device.name);
                }
            });
        if selected != self.store.selected {
            let mut store = self.store.clone();
            store.selected = selected;
            if self.save(store, project, status) {
                *dirty = true;
            }
        }
        ui.small(format!("{}:{}", self.device().hostname, self.device().port));
        if ui.button("Lasercutter verwalten …").clicked() {
            self.open_manager();
        }
        if self.device().rotary_axis {
            *dirty |= ui
                .checkbox(&mut project.rotary_axis, "Drehachse verwenden")
                .changed();
            if project.rotary_axis {
                *dirty |= crate::number(
                    ui,
                    "Durchmesser",
                    &mut project.rotary_diameter_mm,
                    5.0..=1000.0,
                    " mm",
                );
                ui.small(format!(
                    "Y = Umfang ({:.1} mm). Schritte je Umdrehung aus LibLaserCut, am Gerät nicht validiert.",
                    std::f32::consts::PI * project.rotary_diameter_mm
                ));
            }
        }
        if !self.device().camera_url.is_empty() {
            ui.horizontal(|ui| {
                if ui.checkbox(&mut self.show_camera, "Kamerabild").changed()
                    && self.show_camera
                    && self.camera.is_none()
                {
                    self.refresh_camera(project);
                }
                if self.camera_loading.is_some() {
                    ui.spinner();
                } else if self.show_camera && ui.button("Aktualisieren").clicked() {
                    self.refresh_camera(project);
                }
            });
        }
    }

    /// Paints the camera picture on the bed; returns whether it covers it.
    pub fn paint_camera(&self, painter: &egui::Painter, bed: Rect) -> bool {
        match (&self.camera, self.show_camera) {
            (Some(texture), true) => {
                painter.image(
                    texture.id(),
                    bed,
                    Rect::from_min_max(Pos2::ZERO, Pos2::new(1.0, 1.0)),
                    Color32::WHITE,
                );
                true
            }
            _ => false,
        }
    }

    pub fn windows(
        &mut self,
        ctx: &egui::Context,
        project: &mut Project,
        status: &mut String,
    ) -> Option<Action> {
        match poll(&mut self.camera_loading, ctx) {
            Some(Ok(image)) => {
                self.camera = Some(texture(ctx, "Kamera", &image));
                *status = "Kamerabild aktualisiert".into();
            }
            Some(Err(e)) => {
                self.show_camera = false;
                *status = format!("Kamerabild: {e}");
            }
            None => {}
        }
        self.poll_download(ctx, status);
        let mut action = None;
        self.manager(ctx, project, status);
        if let Some(calibration) = &mut self.calibration {
            let mut close = false;
            let mut apply = None;
            calibration_window(ctx, calibration, &mut close, &mut apply, &mut action);
            if let Some(points) = apply
                && let Some((draft, _)) = &mut self.draft
                && let Some(device) = draft
                    .devices
                    .iter_mut()
                    .find(|d| d.name == calibration.device.name)
            {
                device.camera_calibration = Some(points);
            }
            if close {
                self.calibration = None;
            }
        }
        if action.is_some() {
            self.calibration = None;
        }
        action
    }

    fn start_download(&mut self, name: &str, url: &'static str, status: &mut String) {
        if self.download.is_some() {
            return;
        }
        let (tx, rx) = channel();
        std::thread::spawn(move || {
            let _ = tx.send(device::download(url));
        });
        self.download = Some((name.to_owned(), rx));
        *status = format!("Einstellungen von „{name}“ werden heruntergeladen …");
    }

    /// Übernimmt fertige Downloads in den Entwurf der Verwaltung.
    fn poll_download(&mut self, ctx: &egui::Context, status: &mut String) {
        let Some((name, receiver)) = &self.download else {
            return;
        };
        let result = match receiver.try_recv() {
            Ok(result) => result,
            Err(TryRecvError::Empty) => {
                ctx.request_repaint_after(std::time::Duration::from_millis(100));
                return;
            }
            Err(TryRecvError::Disconnected) => Err("Download unerwartet abgebrochen".into()),
        };
        let name = name.clone();
        self.download = None;
        match result {
            Ok(devices) => {
                self.open_manager();
                if let Some((draft, editing)) = &mut self.draft {
                    let added = draft.merge(devices);
                    *editing = draft.devices.len() - 1;
                    *status = format!(
                        "{added} LTT iLaser 4000 von „{name}“ importiert · zum Übernehmen sichern"
                    );
                }
            }
            Err(e) => *status = format!("Download von „{name}“ fehlgeschlagen: {e}"),
        }
    }

    fn manager(&mut self, ctx: &egui::Context, project: &mut Project, status: &mut String) {
        let Some((mut draft, mut editing)) = self.draft.take() else {
            return;
        };
        let mut open = true;
        let mut save = false;
        let mut calibrate = false;
        let mut download = None;
        let downloading = self.download.as_ref().map(|(name, _)| name.clone());
        egui::Window::new("Lasercutter verwalten")
            .open(&mut open)
            .default_size([640.0, 420.0])
            .show(ctx, |ui| {
                ui.horizontal_top(|ui| {
                    ui.vertical(|ui| {
                        ui.set_width(170.0);
                        for (i, device) in draft.devices.iter().enumerate() {
                            let label = if i == draft.selected {
                                format!("{} (aktiv)", device.name)
                            } else {
                                device.name.clone()
                            };
                            ui.selectable_value(&mut editing, i, label);
                        }
                        ui.horizontal(|ui| {
                            if ui.button("Kopie").clicked() {
                                let mut copy = draft.devices[editing].clone();
                                let base = copy.name.clone();
                                let mut n = 2;
                                while draft.devices.iter().any(|d| d.name == copy.name) {
                                    copy.name = format!("{base} ({n})");
                                    n += 1;
                                }
                                draft.devices.push(copy);
                                editing = draft.devices.len() - 1;
                            }
                            if ui
                                .add_enabled(
                                    draft.devices.len() > 1,
                                    egui::Button::new("Entfernen"),
                                )
                                .clicked()
                            {
                                draft.devices.remove(editing);
                                if draft.selected >= editing && draft.selected > 0 {
                                    draft.selected -= 1;
                                }
                                editing = editing.min(draft.devices.len() - 1);
                            }
                        });
                    });
                    ui.separator();
                    ui.vertical(|ui| {
                        let device = &mut draft.devices[editing];
                        egui::Grid::new("device").num_columns(2).show(ui, |ui| {
                            for (label, value) in [
                                ("Name", &mut device.name),
                                ("Beschreibung", &mut device.description),
                                ("Hostname / IP", &mut device.hostname),
                                ("Kamera-URL", &mut device.camera_url),
                            ] {
                                ui.label(label);
                                ui.text_edit_singleline(value);
                                ui.end_row();
                            }
                            ui.label("Port");
                            ui.add(egui::DragValue::new(&mut device.port).range(1..=65535));
                            ui.end_row();
                            ui.label("Drehachse");
                            ui.checkbox(&mut device.rotary_axis, "vorhanden");
                            ui.end_row();
                            ui.label("Kalibrierung");
                            ui.horizontal(|ui| {
                                ui.label(
                                    device
                                        .camera_calibration
                                        .as_ref()
                                        .map_or("keine".into(), |c| {
                                            format!("{} Punkte", c.reference_points.len())
                                        }),
                                );
                                calibrate = ui
                                    .add_enabled(
                                        !device.camera_url.is_empty(),
                                        egui::Button::new("Kalibrieren …"),
                                    )
                                    .clicked();
                            });
                            ui.end_row();
                        });
                        ui.label("Hinweis nach dem Senden ($jobname = Auftragsnamen):");
                        ui.add(
                            egui::TextEdit::multiline(&mut device.job_sent_text).desired_rows(4),
                        );
                        ui.small(
                            "Autofokus, Druckluft und Absaugung stellt der LTT-Treiber nicht ein.",
                        );
                    });
                });
                ui.separator();
                ui.horizontal(|ui| {
                    if ui.button("Importieren …").clicked()
                        && let Some(path) = rfd::FileDialog::new()
                            .add_filter(
                                "VisiCut-Einstellungen",
                                &["xml", "vcsettings", "zip", "vcrdevices", "json"],
                            )
                            .pick_file()
                    {
                        match std::fs::read(&path)
                            .map_err(|e| e.to_string())
                            .and_then(|b| device::import_bytes(&b))
                        {
                            Ok(devices) => {
                                *status =
                                    format!("{} LTT iLaser 4000 importiert", draft.merge(devices));
                                editing = draft.devices.len() - 1;
                            }
                            Err(e) => *status = e,
                        }
                    }
                    ui.add_enabled_ui(downloading.is_none(), |ui| {
                        egui::ComboBox::from_id_salt("labs")
                            .selected_text("Herunterladen")
                            .show_ui(ui, |ui| {
                                for (name, url) in device::LAB_SETTINGS {
                                    if ui.selectable_label(false, *name).clicked() {
                                        download = Some((*name, *url));
                                    }
                                }
                            });
                    });
                    if let Some(name) = &downloading {
                        ui.spinner().on_hover_text(format!("Lädt „{name}“ …"));
                    }
                    if ui.button("Exportieren …").clicked()
                        && let Some(path) = rfd::FileDialog::new()
                            .add_filter("VisiCutRust-Lasercutter", &["vcrdevices"])
                            .set_file_name("Lasercutter.vcrdevices")
                            .save_file()
                    {
                        *status = match device::export(&path, &draft.devices) {
                            Ok(()) => format!("{} Lasercutter exportiert", draft.devices.len()),
                            Err(e) => e,
                        };
                    }
                    if ui
                        .add_enabled(
                            editing != draft.selected,
                            egui::Button::new("Aktiv verwenden"),
                        )
                        .clicked()
                    {
                        draft.selected = editing;
                    }
                    let changed = draft != self.store;
                    if ui
                        .add_enabled(changed, egui::Button::new("Verwerfen"))
                        .on_hover_text("Ungesicherte Änderungen an der Geräteliste verwerfen")
                        .clicked()
                    {
                        draft = self.store.clone();
                        editing = editing.min(draft.devices.len() - 1);
                    }
                    save = ui
                        .add_enabled(changed, egui::Button::new("Sichern"))
                        .clicked();
                });
            });
        if calibrate {
            let device = draft.devices[editing].clone();
            let points = device
                .camera_calibration
                .clone()
                .unwrap_or(CameraCalibration {
                    reference_points: vec![
                        [200.0, 120.0],
                        [800.0, 120.0],
                        [800.0, 480.0],
                        [200.0, 480.0],
                    ],
                    view_points: Vec::new(),
                });
            let shot = device.clone();
            self.calibration = Some(Calibration {
                device,
                points,
                picture: None,
                loading: Some(spawn(move || camera::capture(&shot))),
                selected: 0,
                problem: None,
            });
        }
        if let Some((name, url)) = download {
            self.start_download(name, url, status);
        }
        if save {
            self.save(draft.clone(), project, status);
        }
        if open {
            self.draft = Some((draft, editing));
        }
    }
}

fn calibration_window(
    ctx: &egui::Context,
    c: &mut Calibration,
    close: &mut bool,
    apply: &mut Option<CameraCalibration>,
    action: &mut Option<Action>,
) {
    match poll(&mut c.loading, ctx) {
        Some(Ok(image)) => {
            let size = Vec2::new(image.width() as f32, image.height() as f32);
            c.picture = Some((texture(ctx, "Kalibrierfoto", &image), size));
            // Place missing markers where an uncalibrated camera would see them.
            while c.points.view_points.len() < c.points.reference_points.len() {
                let r = c.points.reference_points[c.points.view_points.len()];
                c.points
                    .view_points
                    .push([r[0] / 1000.0 * size.x as f64, r[1] / 600.0 * size.y as f64]);
            }
        }
        Some(Err(e)) => c.problem = Some(e),
        None => {}
    }
    let mut open = true;
    egui::Window::new(format!("Kamera kalibrieren · {}", c.device.name))
        .open(&mut open)
        .default_size([900.0, 560.0])
        .show(ctx, |ui| {
            ui.horizontal_top(|ui| {
                let space = Vec2::new((ui.available_width() - 220.0).max(200.0), 460.0);
                let (rect, response) = ui.allocate_exact_size(space, Sense::click_and_drag());
                let painter = ui.painter().with_clip_rect(rect);
                painter.rect_filled(rect, 0.0, Color32::from_gray(235));
                if let Some((texture, size)) = &c.picture {
                    let scale = (rect.width() / size.x).min(rect.height() / size.y);
                    let image = Rect::from_center_size(rect.center(), *size * scale);
                    painter.image(texture.id(), image, Rect::from_min_max(Pos2::ZERO, Pos2::new(1.0, 1.0)), Color32::WHITE);
                    let to_screen = |p: [f64; 2]| image.min + Vec2::new(p[0] as f32, p[1] as f32) * scale;
                    if let Some(pointer) = response.interact_pointer_pos() {
                        if response.drag_started() || response.clicked() {
                            // Grab the nearest marker, otherwise move the selected one.
                            if let Some((i, _)) = c.points.view_points.iter().enumerate()
                                .map(|(i, p)| (i, to_screen(*p).distance(pointer)))
                                .filter(|(_, d)| *d < 16.0)
                                .min_by(|a, b| a.1.total_cmp(&b.1))
                            {
                                c.selected = i;
                            }
                        }
                        if let Some(p) = c.points.view_points.get_mut(c.selected) {
                            let local = (pointer - image.min) / scale;
                            *p = [local.x.clamp(0.0, size.x) as f64, local.y.clamp(0.0, size.y) as f64];
                        }
                    }
                    for (i, p) in c.points.view_points.iter().enumerate() {
                        let center = to_screen(*p);
                        let color = if i == c.selected { Color32::from_rgb(40, 120, 230) } else { Color32::YELLOW };
                        painter.circle_stroke(center, 12.0, Stroke::new(2.0, color));
                        painter.line_segment([center - Vec2::X * 12.0, center + Vec2::X * 12.0], Stroke::new(1.0, color));
                        painter.line_segment([center - Vec2::Y * 12.0, center + Vec2::Y * 12.0], Stroke::new(1.0, color));
                        painter.text(center + Vec2::new(16.0, -16.0), egui::Align2::LEFT_BOTTOM, (i + 1).to_string(),
                            egui::FontId::proportional(14.0), color);
                    }
                } else {
                    painter.text(rect.center(), egui::Align2::CENTER_CENTER,
                        if c.loading.is_some() { "Foto wird aufgenommen …" } else { "Kein Kamerabild" },
                        egui::FontId::proportional(16.0), Color32::from_gray(120));
                }
                ui.vertical(|ui| {
                    ui.label("Marker auf dem Bett (mm)");
                    for (i, r) in c.points.reference_points.iter_mut().enumerate() {
                        ui.horizontal(|ui| {
                            ui.selectable_value(&mut c.selected, i, (i + 1).to_string());
                            ui.add(egui::DragValue::new(&mut r[0]).range(0.0..=1000.0));
                            ui.add(egui::DragValue::new(&mut r[1]).range(0.0..=600.0));
                        });
                    }
                    ui.horizontal(|ui| {
                        if ui.add_enabled(c.picture.is_some(), egui::Button::new("Punkt +")).clicked() {
                            let size = c.picture.as_ref().unwrap().1;
                            c.points.reference_points.push([500.0, 300.0]);
                            c.points.view_points.push([size.x as f64 / 2.0, size.y as f64 / 2.0]);
                            c.selected = c.points.reference_points.len() - 1;
                        }
                        if ui.add_enabled(c.points.reference_points.len() > 2, egui::Button::new("Punkt −")).clicked() {
                            c.points.reference_points.remove(c.selected);
                            if c.selected < c.points.view_points.len() {
                                c.points.view_points.remove(c.selected);
                            }
                            c.selected = c.selected.saturating_sub(1);
                        }
                    });
                    ui.small("1. Kalibrierseite öffnen und markieren.\n2. Foto aufnehmen.\n3. Marker auf die Kreuze ziehen (Nummer = Anzahl Striche).\nMindestens 4 Punkte.");
                    if let Some(problem) = &c.problem {
                        ui.colored_label(Color32::from_rgb(180, 75, 40), problem);
                    }
                });
            });
            ui.horizontal(|ui| {
                if ui.add_enabled(c.loading.is_none(), egui::Button::new("Foto aufnehmen")).clicked() {
                    let device = c.device.clone();
                    c.problem = None;
                    c.loading = Some(spawn(move || camera::capture(&device)));
                }
                if ui.button("Kalibrierseite als Projekt öffnen").clicked() {
                    *action = Some(Action::CalibrationPage(c.points.reference_points.clone()));
                }
                if ui.add_enabled(c.points.view_points.len() == c.points.reference_points.len(), egui::Button::new("Übernehmen")).clicked() {
                    match camera::homography(&c.points) {
                        Ok(_) => {
                            *apply = Some(c.points.clone());
                            *close = true;
                        }
                        Err(e) => c.problem = Some(e),
                    }
                }
            });
        });
    *close |= !open;
}
