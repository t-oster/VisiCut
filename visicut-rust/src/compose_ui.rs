//! Text, shapes, signs, copies and nesting for the egui interface.
use eframe::egui;
use visicut_core::{
    compose::{self, Align, NestSettings, NewObject, Shape, Sign},
    project::{Operation, Project},
};

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum Tool {
    Text,
    Shape,
    Sign,
    Array,
    Nest,
}

impl Tool {
    fn title(self) -> &'static str {
        match self {
            Self::Text => "Text einfügen",
            Self::Shape => "Form einfügen",
            Self::Sign => "Schild erstellen",
            Self::Array => "Kopien im Raster",
            Self::Nest => "Teile verschachteln (Nesting)",
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum ShapeKind {
    Rectangle,
    Ellipse,
    Polygon,
    Star,
}

struct TextForm {
    text: String,
    font: String,
    size_mm: f64,
    bold: bool,
    italic: bool,
    align: Align,
    line_spacing: f64,
    operation: Operation,
}

struct ShapeForm {
    kind: ShapeKind,
    width_mm: f64,
    height_mm: f64,
    corner_mm: f64,
    corners: u32,
    diameter_mm: f64,
    inner_percent: f64,
    operation: Operation,
}

/// Ergebnis eines Werkzeugs: neues Projekt und Hinweise.
pub type Composed = Result<(Project, Vec<String>), String>;

pub struct ComposeUi {
    open: Option<Tool>,
    x_mm: f64,
    y_mm: f64,
    text: TextForm,
    shape: ShapeForm,
    sign: Sign,
    columns: u32,
    rows: u32,
    gap_x_mm: f64,
    gap_y_mm: f64,
    nest: NestSettings,
    fonts: Vec<String>,
}

impl Default for ComposeUi {
    fn default() -> Self {
        Self {
            open: None,
            x_mm: 10.0,
            y_mm: 10.0,
            text: TextForm {
                text: "Text".into(),
                font: "sans-serif".into(),
                size_mm: 10.0,
                bold: false,
                italic: false,
                align: Align::Left,
                line_spacing: 1.2,
                operation: Operation::Engrave,
            },
            shape: ShapeForm {
                kind: ShapeKind::Rectangle,
                width_mm: 50.0,
                height_mm: 30.0,
                corner_mm: 0.0,
                corners: 6,
                diameter_mm: 40.0,
                inner_percent: 50.0,
                operation: Operation::Cut,
            },
            sign: Sign::default(),
            columns: 2,
            rows: 2,
            gap_x_mm: 5.0,
            gap_y_mm: 5.0,
            nest: NestSettings::default(),
            fonts: Vec::new(),
        }
    }
}

fn operation_choice(ui: &mut egui::Ui, id: &str, operation: &mut Operation) {
    egui::ComboBox::from_id_salt(id)
        .selected_text(match operation {
            Operation::Cut => "Schneiden (rot)",
            Operation::Mark => "Markieren (blau)",
            _ => "Gravieren (schwarz)",
        })
        .show_ui(ui, |ui| {
            ui.selectable_value(operation, Operation::Engrave, "Gravieren (schwarz)");
            ui.selectable_value(operation, Operation::Cut, "Schneiden (rot)");
            ui.selectable_value(operation, Operation::Mark, "Markieren (blau)");
        });
}

fn mm(ui: &mut egui::Ui, label: &str, value: &mut f64, range: std::ops::RangeInclusive<f64>) {
    ui.label(label);
    ui.add(
        egui::DragValue::new(value)
            .range(range)
            .speed(0.5)
            .suffix(" mm"),
    );
    ui.end_row();
}

fn font_choice(ui: &mut egui::Ui, fonts: &[String], font: &mut String) {
    egui::ComboBox::from_id_salt("font")
        .selected_text(font.as_str())
        .height(320.0)
        .show_ui(ui, |ui| {
            for name in fonts {
                ui.selectable_value(font, name.clone(), name);
            }
        });
}

impl ComposeUi {
    /// Öffnet ein Werkzeug; neue Elemente erscheinen rechts neben dem Motiv.
    pub fn open(&mut self, tool: Tool, project: &Project) {
        if self.fonts.is_empty() {
            self.fonts = compose::font_families();
        }
        if !project.svg.is_empty() {
            let right = (project.x_mm + project.width_mm + 10.0) as f64;
            (self.x_mm, self.y_mm) = if right < project.bed_width_mm as f64 - 20.0 {
                (right, project.y_mm as f64)
            } else {
                (project.x_mm as f64, project.y_mm as f64)
            };
        }
        self.sign.x_mm = self.x_mm;
        self.sign.y_mm = self.y_mm;
        self.open = Some(tool);
    }

    /// Zeigt das offene Werkzeug; liefert das neue Projekt nach „Einfügen“.
    pub fn windows(&mut self, ctx: &egui::Context, project: &Project) -> Option<Composed> {
        let tool = self.open?;
        let mut open = true;
        let mut apply = false;
        egui::Window::new(tool.title())
            .open(&mut open)
            .resizable(false)
            .show(ctx, |ui| {
                match tool {
                    Tool::Text => self.text_form(ui),
                    Tool::Shape => self.shape_form(ui),
                    Tool::Sign => self.sign_form(ui),
                    Tool::Array => self.array_form(ui, project),
                    Tool::Nest => self.nest_form(ui, project),
                }
                ui.separator();
                let label = match tool {
                    Tool::Array | Tool::Nest => "Anwenden",
                    _ => "Einfügen",
                };
                apply = ui.button(label).clicked();
            });
        if !open {
            self.open = None;
        }
        if !apply {
            return None;
        }
        let result = self.compose(tool, project);
        if result.is_ok() {
            self.open = None;
        }
        Some(result)
    }

    fn compose(&self, tool: Tool, project: &Project) -> Composed {
        let add = |objects: Vec<NewObject>| {
            compose::add_objects(project, &objects).map(|(p, _)| (p, Vec::new()))
        };
        match tool {
            Tool::Text => {
                let t = &self.text;
                add(vec![NewObject {
                    shape: Shape::Text {
                        text: t.text.clone(),
                        font: t.font.clone(),
                        size_mm: t.size_mm,
                        bold: t.bold,
                        italic: t.italic,
                        align: t.align,
                        line_spacing: t.line_spacing,
                    },
                    operation: t.operation,
                    x_mm: self.x_mm,
                    y_mm: self.y_mm,
                }])
            }
            Tool::Shape => {
                let s = &self.shape;
                let shape = match s.kind {
                    ShapeKind::Rectangle => Shape::Rectangle {
                        width_mm: s.width_mm,
                        height_mm: s.height_mm,
                        corner_mm: s.corner_mm,
                    },
                    ShapeKind::Ellipse => Shape::Ellipse {
                        width_mm: s.width_mm,
                        height_mm: s.height_mm,
                    },
                    ShapeKind::Polygon => Shape::Polygon {
                        sides: s.corners,
                        diameter_mm: s.diameter_mm,
                    },
                    ShapeKind::Star => Shape::Star {
                        points: s.corners,
                        diameter_mm: s.diameter_mm,
                        inner_percent: s.inner_percent,
                    },
                };
                add(vec![NewObject {
                    shape,
                    operation: s.operation,
                    x_mm: self.x_mm,
                    y_mm: self.y_mm,
                }])
            }
            Tool::Sign => add(compose::sign(&self.sign)?),
            Tool::Array => compose::array(
                project,
                self.columns,
                self.rows,
                self.gap_x_mm,
                self.gap_y_mm,
            )
            .map(|p| (p, Vec::new())),
            Tool::Nest => compose::nest(project, &self.nest).map(|n| {
                let mut notes = Vec::new();
                if n.missing > 0 {
                    notes.push(format!(
                        "{} von {} Teilen passen nicht in die Fläche",
                        n.missing,
                        n.missing + n.placed
                    ));
                }
                (n.project, notes)
            }),
        }
    }

    fn position(&mut self, ui: &mut egui::Ui) {
        mm(ui, "X (links)", &mut self.x_mm, 0.0..=1000.0);
        mm(ui, "Y (oben)", &mut self.y_mm, 0.0..=600.0);
    }

    fn text_form(&mut self, ui: &mut egui::Ui) {
        ui.add(
            egui::TextEdit::multiline(&mut self.text.text)
                .desired_rows(3)
                .desired_width(320.0),
        );
        egui::Grid::new("text").num_columns(2).show(ui, |ui| {
            ui.label("Schrift");
            font_choice(ui, &self.fonts, &mut self.text.font);
            ui.end_row();
            mm(ui, "Schriftgröße", &mut self.text.size_mm, 0.5..=1000.0);
            ui.label("Stil");
            ui.horizontal(|ui| {
                ui.checkbox(&mut self.text.bold, "Fett");
                ui.checkbox(&mut self.text.italic, "Kursiv");
            });
            ui.end_row();
            ui.label("Ausrichtung");
            ui.horizontal(|ui| {
                ui.selectable_value(&mut self.text.align, Align::Left, "Links");
                ui.selectable_value(&mut self.text.align, Align::Center, "Mitte");
                ui.selectable_value(&mut self.text.align, Align::Right, "Rechts");
            });
            ui.end_row();
            ui.label("Zeilenabstand");
            ui.add(
                egui::DragValue::new(&mut self.text.line_spacing)
                    .range(0.5..=5.0)
                    .speed(0.05),
            );
            ui.end_row();
            ui.label("Bearbeitung");
            operation_choice(ui, "text_operation", &mut self.text.operation);
            ui.end_row();
            self.position(ui);
        });
        ui.small("Der Text wird in Umrisse umgewandelt und ist danach nicht mehr editierbar.");
    }

    fn shape_form(&mut self, ui: &mut egui::Ui) {
        let (s, x, y) = (&mut self.shape, &mut self.x_mm, &mut self.y_mm);
        ui.horizontal(|ui| {
            ui.selectable_value(&mut s.kind, ShapeKind::Rectangle, "Rechteck");
            ui.selectable_value(&mut s.kind, ShapeKind::Ellipse, "Ellipse");
            ui.selectable_value(&mut s.kind, ShapeKind::Polygon, "Vieleck");
            ui.selectable_value(&mut s.kind, ShapeKind::Star, "Stern");
        });
        egui::Grid::new("shape").num_columns(2).show(ui, |ui| {
            match s.kind {
                ShapeKind::Rectangle | ShapeKind::Ellipse => {
                    mm(ui, "Breite", &mut s.width_mm, 0.1..=1000.0);
                    mm(ui, "Höhe", &mut s.height_mm, 0.1..=1000.0);
                    if s.kind == ShapeKind::Rectangle {
                        mm(ui, "Eckenradius", &mut s.corner_mm, 0.0..=500.0);
                    }
                }
                ShapeKind::Polygon | ShapeKind::Star => {
                    ui.label(if s.kind == ShapeKind::Star {
                        "Zacken"
                    } else {
                        "Ecken"
                    });
                    ui.add(egui::DragValue::new(&mut s.corners).range(3..=100));
                    ui.end_row();
                    mm(ui, "Durchmesser", &mut s.diameter_mm, 0.1..=1000.0);
                    if s.kind == ShapeKind::Star {
                        ui.label("Innenradius");
                        ui.add(
                            egui::DragValue::new(&mut s.inner_percent)
                                .range(5.0..=95.0)
                                .suffix(" %"),
                        );
                        ui.end_row();
                    }
                }
            }
            ui.label("Bearbeitung");
            operation_choice(ui, "shape_operation", &mut s.operation);
            ui.end_row();
            mm(ui, "X (links)", x, 0.0..=1000.0);
            mm(ui, "Y (oben)", y, 0.0..=600.0);
        });
    }

    fn sign_form(&mut self, ui: &mut egui::Ui) {
        let s = &mut self.sign;
        ui.add(
            egui::TextEdit::multiline(&mut s.text)
                .desired_rows(2)
                .desired_width(320.0),
        );
        egui::Grid::new("sign").num_columns(2).show(ui, |ui| {
            mm(ui, "Breite", &mut s.width_mm, 5.0..=1000.0);
            mm(ui, "Höhe", &mut s.height_mm, 5.0..=600.0);
            mm(ui, "Eckenradius", &mut s.corner_mm, 0.0..=300.0);
            ui.label("Löcher");
            ui.horizontal(|ui| {
                for n in [0, 2, 4] {
                    ui.selectable_value(&mut s.holes, n, n.to_string());
                }
            });
            ui.end_row();
            if s.holes > 0 {
                mm(ui, "Lochdurchmesser", &mut s.hole_diameter_mm, 0.5..=50.0);
                mm(
                    ui,
                    "Lochabstand zum Rand",
                    &mut s.hole_inset_mm,
                    0.5..=200.0,
                );
            }
            mm(
                ui,
                "Rahmen (markiert, 0 = ohne)",
                &mut s.frame_inset_mm,
                0.0..=100.0,
            );
            ui.label("Schrift");
            font_choice(ui, &self.fonts, &mut s.font);
            ui.end_row();
            mm(ui, "Schriftgröße", &mut s.size_mm, 0.5..=500.0);
            ui.label("Text");
            ui.horizontal(|ui| {
                ui.checkbox(&mut s.bold, "Fett");
                operation_choice(ui, "sign_text", &mut s.text_operation);
            });
            ui.end_row();
            mm(ui, "X (links)", &mut s.x_mm, 0.0..=1000.0);
            mm(ui, "Y (oben)", &mut s.y_mm, 0.0..=600.0);
        });
        ui.small("Platte und Löcher werden geschnitten, der Text mittig gesetzt.");
    }

    fn array_form(&mut self, ui: &mut egui::Ui, project: &Project) {
        egui::Grid::new("array").num_columns(2).show(ui, |ui| {
            ui.label("Spalten");
            ui.add(egui::DragValue::new(&mut self.columns).range(1..=50));
            ui.end_row();
            ui.label("Zeilen");
            ui.add(egui::DragValue::new(&mut self.rows).range(1..=50));
            ui.end_row();
            mm(ui, "Abstand X", &mut self.gap_x_mm, -500.0..=500.0);
            mm(ui, "Abstand Y", &mut self.gap_y_mm, -500.0..=500.0);
        });
        let (w, h) = (
            self.columns as f64 * project.width_mm as f64
                + (self.columns as f64 - 1.0) * self.gap_x_mm,
            self.rows as f64 * project.height_mm as f64 + (self.rows as f64 - 1.0) * self.gap_y_mm,
        );
        ui.small(format!(
            "{} Exemplare · {w:.0} × {h:.0} mm; Zuordnungen gelten für alle Kopien.",
            self.columns * self.rows
        ));
    }

    fn nest_form(&mut self, ui: &mut egui::Ui, project: &Project) {
        let n = &mut self.nest;
        egui::Grid::new("nest").num_columns(2).show(ui, |ui| {
            ui.label("Anzahl je Teil");
            ui.add(egui::DragValue::new(&mut n.quantity).range(1..=400));
            ui.end_row();
            mm(ui, "Abstand", &mut n.gap_mm, 0.0..=50.0);
            ui.label("Drehen");
            ui.checkbox(&mut n.rotate, "um 90° erlaubt");
            ui.end_row();
            mm(ui, "Fläche X", &mut n.area_mm[0], 0.0..=1000.0);
            mm(ui, "Fläche Y", &mut n.area_mm[1], 0.0..=600.0);
            mm(ui, "Fläche Breite", &mut n.area_mm[2], 1.0..=1000.0);
            mm(ui, "Fläche Höhe", &mut n.area_mm[3], 1.0..=600.0);
        });
        ui.horizontal(|ui| {
            if ui.button("Ganzes Arbeitsbett").clicked() {
                n.area_mm = NestSettings::default().area_mm;
            }
            if ui.button("Fläche des Motivs").clicked() {
                n.area_mm = [
                    project.x_mm as f64,
                    project.y_mm as f64,
                    project.width_mm as f64,
                    project.height_mm as f64,
                ];
            }
        });
        ui.small(
            "Teile sind die Objekte der obersten Ebene (bzw. einer Inkscape-Ebene); \
             sie werden nach ihren Umrissrechtecken angeordnet, größte zuerst.",
        );
    }
}
