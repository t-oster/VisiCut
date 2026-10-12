//! Dialog „Bitmap vektorisieren“ für die egui-Oberfläche. Das Ergebnis wird als
//! SVG an den normalen Importpfad der Hauptansicht übergeben.
use eframe::egui;
use std::path::Path;
use visicut_core::{i18n::tr, tr};
use visicut_core::{
    import::MAX_FILE_BYTES,
    vectorize::{Bitmap, Options, Size, Vectorized},
};

pub struct VectorizeUi {
    open: bool,
    name: String,
    bitmap: Option<Bitmap>,
    threshold: u8,
    invert: bool,
    width_mm: f64,
    /// Ergebnis für die Optionen in `result_for`; wird bei jeder Änderung neu berechnet.
    result: Option<Result<Vectorized, String>>,
    result_for: Option<Options>,
}

impl Default for VectorizeUi {
    fn default() -> Self {
        Self {
            open: false,
            name: String::new(),
            bitmap: None,
            threshold: Options::default().threshold,
            invert: false,
            width_mm: 0.0,
            result: None,
            result_for: None,
        }
    }
}

impl VectorizeUi {
    /// Fragt nach einem Bild und öffnet den Dialog. Fehler landen in der Statuszeile.
    pub fn pick(&mut self, status: &mut String) {
        let Some(path) = rfd::FileDialog::new()
            .add_filter(tr("Bitmap"), &["png", "jpg", "jpeg", "bmp", "gif"])
            .pick_file()
        else {
            return;
        };
        match load(&path) {
            Ok((bitmap, name)) => {
                // Standardgröße wie beim Bildimport: 72 DPI.
                let (width, _) = bitmap.dimensions();
                self.width_mm = width as f64 * 25.4 / 72.0;
                self.bitmap = Some(bitmap);
                self.name = name;
                self.result = None;
                self.result_for = None;
                self.open = true;
            }
            Err(error) => *status = error,
        }
    }

    fn options(&self) -> Options {
        Options {
            threshold: self.threshold,
            invert: self.invert,
            size: Size::WidthMm(self.width_mm),
            ..Options::default()
        }
    }

    /// Zeigt den Dialog. Liefert SVG und Projektname, wenn das Ergebnis übernommen wird.
    pub fn windows(
        &mut self,
        ctx: &egui::Context,
        dirty: bool,
        status: &mut String,
    ) -> Option<(String, String)> {
        if !self.open {
            return None;
        }
        let options = self.options();
        if self.result_for != Some(options) {
            self.result = self
                .bitmap
                .as_ref()
                .map(|bitmap| bitmap.vectorize(&options));
            self.result_for = Some(options);
        }
        let mut open = self.open;
        let mut apply = false;
        let mut cancel = false;
        egui::Window::new(tr("Bitmap vektorisieren"))
            .id(egui::Id::new("vectorize"))
            .open(&mut open)
            .collapsible(false)
            .resizable(false)
            .show(ctx, |ui| {
                ui.label(tr!("Bild: {name}", name = self.name));
                ui.add(egui::Slider::new(&mut self.threshold, 0..=255).text(tr("Schwellwert")));
                ui.checkbox(
                    &mut self.invert,
                    tr("Invertieren (helle Bereiche nachzeichnen)"),
                );
                ui.horizontal(|ui| {
                    ui.label(tr("Breite"));
                    ui.add(
                        egui::DragValue::new(&mut self.width_mm)
                            .range(1.0..=10000.0)
                            .speed(0.5)
                            .suffix(" mm"),
                    );
                });
                ui.separator();
                match &self.result {
                    Some(Ok(result)) if result.paths > 0 => {
                        ui.label(tr!(
                            "{count} Pfade · {width} × {height} mm",
                            count = result.paths,
                            width = format!("{:.1}", result.width_mm),
                            height = format!("{:.1}", result.height_mm)
                        ));
                    }
                    Some(Ok(_)) => {
                        ui.label(tr("Keine Kontur gefunden; Schwellwert anpassen"));
                    }
                    Some(Err(error)) => {
                        ui.colored_label(egui::Color32::from_rgb(170, 40, 40), error);
                    }
                    None => {}
                }
                let ready = matches!(&self.result, Some(Ok(result)) if result.paths > 0);
                ui.horizontal(|ui| {
                    if ui
                        .add_enabled(ready, egui::Button::new(tr("Übernehmen")))
                        .clicked()
                    {
                        apply = true;
                    }
                    if ui.button(tr("Abbrechen")).clicked() {
                        cancel = true;
                    }
                });
            });
        self.open = open && !cancel;
        if !apply {
            return None;
        }
        if dirty
            && !rfd::MessageDialog::new()
                .set_title(tr("Ungespeicherte Änderungen"))
                .set_description(tr(
                    "Änderungen verwerfen und das vektorisierte Bild übernehmen?",
                ))
                .set_buttons(rfd::MessageButtons::YesNo)
                .show()
                .eq(&rfd::MessageDialogResult::Yes)
        {
            return None;
        }
        match &self.result {
            Some(Ok(result)) => {
                self.open = false;
                Some((result.svg.clone(), self.name.clone()))
            }
            _ => {
                *status = tr("Vektorisierung fehlgeschlagen").into();
                None
            }
        }
    }
}

fn load(path: &Path) -> Result<(Bitmap, String), String> {
    let size = std::fs::metadata(path).map_err(|e| e.to_string())?.len();
    if size > MAX_FILE_BYTES {
        return Err(tr("Datei ist größer als 25 MB").into());
    }
    let data = std::fs::read(path).map_err(|e| e.to_string())?;
    let name = path
        .file_stem()
        .unwrap_or_default()
        .to_string_lossy()
        .into_owned();
    Ok((Bitmap::decode(&data)?, name))
}
