//! Materials, processing steps, object assignment, rules and engraving
//! options for the egui interface (counterpart of the macOS inspector in
//! native/VisiCutApp.swift and native/Mapping.swift).
use eframe::egui;
use visicut_core::{
    device,
    mapping::{self, Attribute, Filter, Predefined, ValueCounts},
    materials::{Library, Material, MaterialProfile},
    project::{CutOrder, JobStep, Operation, ParameterSet, Project},
    raster::{Dithering, RasterSettings},
    selection,
};

/// Name given to a material that is not part of the library.
const CUSTOM_MATERIAL: &str = "Eigenes Material";
const WARNING: egui::Color32 = egui::Color32::from_rgb(180, 75, 40);

pub fn operation_title(operation: Operation) -> &'static str {
    match operation {
        Operation::Cut => "Schneiden",
        Operation::Engrave => "Gravieren",
        Operation::Engrave3d => "3D-Gravur",
        Operation::Mark => "Markieren",
    }
}

/// Assignment of an object in the individual mode; `None` ignores it.
fn assignment_title(operation: Option<Operation>) -> &'static str {
    operation.map_or("Ignorieren", operation_title)
}

pub fn cut_order_title(order: CutOrder) -> &'static str {
    match order {
        CutOrder::VisiCut => "Wie VisiCut",
        CutOrder::ShortestTravel => "Kürzeste Leerfahrten (experimentell)",
    }
}

/// How objects are assigned to processing steps (macOS `AssignmentMode`).
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum AssignmentMode {
    /// One operation for the whole motif (no steps).
    Whole,
    /// Every object is assigned to a step explicitly.
    Objects,
    /// Steps select objects by filters.
    Rules,
}

impl AssignmentMode {
    pub const ALL: [AssignmentMode; 3] = [Self::Whole, Self::Objects, Self::Rules];
    pub fn title(self) -> &'static str {
        match self {
            Self::Whole => "Gesamt",
            Self::Objects => "Einzeln",
            Self::Rules => "Regeln",
        }
    }
}

fn is_rule(step: &JobStep) -> bool {
    step.filters.is_some() || step.rest
}

pub fn assignment_mode(project: &Project) -> AssignmentMode {
    if project.steps.is_empty() {
        AssignmentMode::Whole
    } else if project.steps.iter().any(is_rule) {
        AssignmentMode::Rules
    } else {
        AssignmentMode::Objects
    }
}

fn same_thickness(a: f32, b: f32) -> bool {
    (a - b).abs() < 1e-4
}

/// Distinct thicknesses of the material's profiles, ascending.
pub fn thicknesses(material: &Material) -> Vec<f32> {
    let mut list: Vec<f32> = material.profiles.iter().map(|p| p.thickness_mm).collect();
    list.sort_by(f32::total_cmp);
    list.dedup_by(|a, b| same_thickness(*a, *b));
    list
}

fn find_material<'a>(library: &'a Library, name: &str) -> Option<&'a Material> {
    library.materials.iter().find(|m| m.name == name)
}

/// A usable profile of the project's material and thickness; profiles with
/// 0 % power or speed count as "not applicable".
pub fn preset<'a>(
    library: &'a Library,
    project: &Project,
    operation: Operation,
) -> Option<&'a MaterialProfile> {
    find_material(library, &project.material)?
        .profiles
        .iter()
        .find(|p| {
            same_thickness(p.thickness_mm, project.thickness_mm)
                && p.operation == operation
                && p.power_percent > 0.0
                && p.speed_percent > 0.0
        })
}

/// A new step with the material profile, else the motif's parameters.
/// Marking without a profile starts at 0 % power so a cutting power is never
/// taken over by accident (macOS `defaultStep`).
pub fn default_step(library: &Library, project: &Project, operation: Operation) -> JobStep {
    let mut step = JobStep::new(operation);
    let mark = operation == Operation::Mark && project.operation != Operation::Mark;
    (step.power_percent, step.speed_percent) = match preset(library, project, operation) {
        Some(p) => (p.power_percent, p.speed_percent),
        None if mark => (0.0, 100.0),
        None => (project.power_percent, project.speed_percent),
    };
    step.passes = project.passes;
    if operation.is_raster() && project.operation.is_raster() {
        step.raster = project.raster;
    }
    step
}

/// Whole-motif operation; marking resets to 0 % power (macOS `selectOperation`).
pub fn select_operation(project: &mut Project, operation: Operation) {
    if operation == project.operation {
        return;
    }
    project.operation = operation;
    if operation == Operation::Mark {
        (project.power_percent, project.speed_percent, project.passes) = (0.0, 100.0, 1);
    }
}

pub fn apply_predefined(library: &Library, project: &mut Project, predefined: &Predefined) {
    project.steps = predefined
        .rules
        .iter()
        .map(|(operation, filters, rest)| JobStep {
            filters: filters.clone(),
            rest: *rest,
            ..default_step(library, project, *operation)
        })
        .collect();
    project.ignore_filters = predefined.ignore.clone();
}

/// Switches the assignment mode like macOS `setAssignmentMode`: individual
/// assignment starts with all objects on the current operation, rules with
/// the first predefined mapping.
pub fn set_mode(library: &Library, project: &mut Project, mode: AssignmentMode, objects: usize) {
    if mode == assignment_mode(project) {
        return;
    }
    project.ignore_filters.clear();
    match mode {
        AssignmentMode::Whole => project.steps.clear(),
        AssignmentMode::Objects => {
            project.steps = Operation::ALL
                .into_iter()
                .filter(|o| *o != Operation::Engrave3d || project.operation == *o)
                .map(|operation| JobStep {
                    objects: if operation == project.operation {
                        (0..objects).collect()
                    } else {
                        Vec::new()
                    },
                    ..default_step(library, project, operation)
                })
                .collect();
        }
        AssignmentMode::Rules => {
            if let Some(first) = mapping::predefined().first() {
                apply_predefined(library, project, first);
            }
        }
    }
}

/// The operation an object is assigned to in the individual mode.
pub fn assignment(project: &Project, object: usize) -> Option<Operation> {
    project
        .steps
        .iter()
        .find(|s| s.objects.contains(&object))
        .map(|s| s.operation)
}

/// Moves an object to the step of `operation`, creating it if needed;
/// `None` ignores the object.
pub fn assign(
    library: &Library,
    project: &mut Project,
    object: usize,
    operation: Option<Operation>,
) {
    if let Some(target) = operation
        && !project.steps.iter().any(|s| s.operation == target)
    {
        let step = default_step(library, project, target);
        project.steps.push(step);
    }
    for step in &mut project.steps {
        step.objects.retain(|o| *o != object);
        if Some(step.operation) == operation {
            step.objects.push(object);
        }
    }
}

/// Takes over the material profile for one step; `false` if there is none.
pub fn apply_step_preset(library: &Library, project: &mut Project, index: usize) -> bool {
    let Some(p) = preset(library, project, project.steps[index].operation) else {
        return false;
    };
    let step = &mut project.steps[index];
    (step.power_percent, step.speed_percent, step.passes) = (p.power_percent, p.speed_percent, 1);
    true
}

/// Takes over the material profiles for the motif or every step; returns
/// how many parameter sets changed.
pub fn apply_presets(library: &Library, project: &mut Project) -> usize {
    if project.steps.is_empty() {
        let Some(p) = preset(library, project, project.operation) else {
            return 0;
        };
        (project.power_percent, project.speed_percent, project.passes) =
            (p.power_percent, p.speed_percent, 1);
        return 1;
    }
    (0..project.steps.len())
        .filter(|i| apply_step_preset(library, project, *i))
        .count()
}

/// `None` chooses an own material that is not part of the library.
pub fn select_material(library: &Library, project: &mut Project, name: Option<&str>) {
    let Some(name) = name else {
        project.material = CUSTOM_MATERIAL.into();
        return;
    };
    project.material = name.into();
    if let Some(material) = find_material(library, name) {
        let list = thicknesses(material);
        if let Some(first) = list.first()
            && !list
                .iter()
                .any(|t| same_thickness(*t, project.thickness_mm))
        {
            project.thickness_mm = *first;
        }
    }
}

fn unique_name(materials: &[Material], base: &str) -> String {
    let mut name = base.to_string();
    let mut n = 2;
    while materials.iter().any(|m| m.name == name) {
        name = format!("{base} ({n})");
        n += 1;
    }
    name
}

fn unique_id(materials: &[Material]) -> String {
    (materials.len()..)
        .map(|n| format!("eigen-{n}"))
        .find(|id| !materials.iter().any(|m| &m.id == id))
        .expect("unbounded range")
}

pub fn new_material(materials: &mut Vec<Material>) -> usize {
    materials.push(Material {
        id: unique_id(materials),
        name: unique_name(materials, "Neues Material"),
        profiles: Vec::new(),
    });
    materials.len() - 1
}

/// Copies a material under a free name; returns the copy's index.
pub fn duplicate_material(materials: &mut Vec<Material>, index: usize) -> usize {
    let mut copy = materials[index].clone();
    copy.id = unique_id(materials);
    copy.name = unique_name(materials, &copy.name);
    materials.push(copy);
    materials.len() - 1
}

/// `#rrggbb` as a colour for swatches.
pub fn parse_color(value: &str) -> Option<egui::Color32> {
    let hex = value.trim().strip_prefix('#')?;
    if hex.len() != 6 || !hex.is_ascii() {
        return None;
    }
    let channel = |i: usize| u8::from_str_radix(&hex[i..i + 2], 16).ok();
    Some(egui::Color32::from_rgb(
        channel(0)?,
        channel(2)?,
        channel(4)?,
    ))
}

fn swatch(ui: &mut egui::Ui, color: Option<egui::Color32>) {
    let (rect, _) = ui.allocate_exact_size(egui::vec2(14.0, 14.0), egui::Sense::hover());
    if let Some(color) = color {
        ui.painter().rect_filled(rect, 3.0, color);
        ui.painter().rect_stroke(
            rect,
            3.0,
            egui::Stroke::new(1.0, egui::Color32::from_gray(150)),
            egui::StrokeKind::Inside,
        );
    }
}

fn is_color(attribute: Attribute) -> bool {
    matches!(
        attribute,
        Attribute::Color | Attribute::StrokeColor | Attribute::FillColor
    )
}

/// One row of the individual assignment list.
struct ObjectRow {
    label: String,
    color: Option<egui::Color32>,
}

type Selections = Result<Vec<Vec<usize>>, String>;

/// Per-document data that is expensive to compute every frame.
#[derive(Default)]
struct Document {
    svg: String,
    objects: Vec<ObjectRow>,
    values: Vec<(Attribute, ValueCounts)>,
    error: Option<String>,
    resolved: Option<(Vec<JobStep>, Vec<Vec<Filter>>, Selections)>,
}

impl Document {
    fn refresh(&mut self, svg: &str) {
        if self.svg.len() == svg.len() && self.svg == svg {
            return;
        }
        self.svg = svg.to_string();
        self.resolved = None;
        self.error = None;
        let attributes = mapping::attributes(svg).unwrap_or_default();
        self.objects = match selection::objects(svg) {
            Ok(list) => list
                .into_iter()
                .map(|o| ObjectRow {
                    color: attributes.get(o.id).and_then(|a| {
                        a.stroke_color
                            .iter()
                            .chain(&a.fill_color)
                            .find_map(|c| parse_color(c))
                    }),
                    label: o.label,
                })
                .collect(),
            Err(e) => {
                self.error = Some(e);
                Vec::new()
            }
        };
        self.values = mapping::values(svg).unwrap_or_default();
    }

    fn suggestions(&self, attribute: Attribute) -> &[(String, usize)] {
        self.values
            .iter()
            .find(|(a, _)| *a == attribute)
            .map_or(&[], |(_, v)| v.as_slice())
    }

    fn first_color(&self, fallback: &str) -> String {
        self.suggestions(Attribute::Color)
            .iter()
            .map(|(v, _)| v)
            .find(|v| parse_color(v).is_some())
            .map_or(fallback.into(), Clone::clone)
    }

    /// Objects per step, recomputed when the steps or ignore rules change.
    fn selections(&mut self, project: &Project) -> Selections {
        let fresh = matches!(&self.resolved, Some((steps, ignore, _))
            if *steps == project.steps && *ignore == project.ignore_filters);
        if !fresh {
            self.resolved = Some((
                project.steps.clone(),
                project.ignore_filters.clone(),
                mapping::resolve(project),
            ));
        }
        self.resolved.as_ref().expect("just resolved").2.clone()
    }
}

fn operation_selector(ui: &mut egui::Ui, project: &mut Project) -> bool {
    let mut changed = false;
    ui.horizontal_wrapped(|ui| {
        for candidate in Operation::ALL {
            if ui
                .selectable_label(project.operation == candidate, operation_title(candidate))
                .clicked()
                && project.operation != candidate
            {
                select_operation(project, candidate);
                changed = true;
            }
        }
    });
    changed
}

fn raster_options(
    ui: &mut egui::Ui,
    id: impl std::hash::Hash,
    settings: &mut RasterSettings,
    operation: Operation,
) -> bool {
    let mut changed = false;
    if operation == Operation::Engrave {
        egui::ComboBox::from_id_salt(("dithering", id))
            .selected_text(settings.dithering.title())
            .show_ui(ui, |ui| {
                for d in Dithering::ALL {
                    changed |= ui
                        .selectable_value(&mut settings.dithering, d, d.title())
                        .changed();
                }
            });
    } else {
        ui.small("Graustufen: Leistung folgt der Helligkeit.");
    }
    ui.horizontal(|ui| {
        ui.label("Helligkeit");
        changed |= ui
            .add(egui::DragValue::new(&mut settings.color_shift).range(-255..=255))
            .changed();
    });
    changed |= ui
        .checkbox(&mut settings.invert, "Farben invertieren")
        .changed();
    changed |= ui
        .checkbox(&mut settings.bidirectional, "Bidirektional")
        .changed();
    changed |= ui
        .checkbox(&mut settings.bottom_up, "Von unten nach oben")
        .changed();
    changed
}

fn parameters(ui: &mut egui::Ui, power: &mut f32, speed: &mut f32, passes: &mut u32) -> bool {
    let mut changed = crate::number(ui, "Leistung", power, 0.0..=100.0, " %");
    changed |= crate::number(ui, "Geschwindigkeit", speed, 0.1..=100.0, " %");
    ui.horizontal(|ui| {
        ui.label("Durchgänge");
        changed |= ui
            .add(egui::DragValue::new(passes).range(1..=100))
            .changed();
    });
    changed
}

fn comparison_title(compare: bool, inverted: bool) -> &'static str {
    match (compare, inverted) {
        (false, false) => "=",
        (false, true) => "≠",
        (true, false) => "≤",
        (true, true) => ">",
    }
}

/// Conditions of one rule; all must match (VisiCut FilterSet).
fn filters(
    ui: &mut egui::Ui,
    id: impl std::hash::Hash + Copy,
    filters: &mut Vec<Filter>,
    document: &Document,
) -> bool {
    let mut changed = false;
    let mut remove = None;
    if filters.is_empty() {
        ui.small("Ohne Bedingung: alle Objekte");
    }
    for (i, filter) in filters.iter_mut().enumerate() {
        ui.horizontal(|ui| {
            egui::ComboBox::from_id_salt(("attribute", id, i))
                .width(120.0)
                .selected_text(filter.attribute.title())
                .show_ui(ui, |ui| {
                    for a in Attribute::ALL {
                        if ui
                            .selectable_label(filter.attribute == a, a.title())
                            .clicked()
                            && filter.attribute != a
                        {
                            *filter = Filter {
                                attribute: a,
                                value: if a == Attribute::StrokeWidth {
                                    "0.1".into()
                                } else {
                                    String::new()
                                },
                                compare: false,
                                inverted: false,
                            };
                            changed = true;
                        }
                    }
                });
            let mut options = vec![(false, false), (false, true)];
            if filter.attribute == Attribute::StrokeWidth {
                options.extend([(true, false), (true, true)]);
            }
            egui::ComboBox::from_id_salt(("comparison", id, i))
                .width(36.0)
                .selected_text(comparison_title(filter.compare, filter.inverted))
                .show_ui(ui, |ui| {
                    for (compare, inverted) in options {
                        if ui
                            .selectable_label(
                                (filter.compare, filter.inverted) == (compare, inverted),
                                comparison_title(compare, inverted),
                            )
                            .clicked()
                        {
                            (filter.compare, filter.inverted) = (compare, inverted);
                            changed = true;
                        }
                    }
                });
            if ui
                .small_button("🗑")
                .on_hover_text("Bedingung entfernen")
                .clicked()
            {
                remove = Some(i);
            }
        });
        ui.horizontal(|ui| {
            if is_color(filter.attribute) {
                swatch(ui, parse_color(&filter.value));
            }
            changed |= ui
                .add(
                    egui::TextEdit::singleline(&mut filter.value)
                        .hint_text("Wert")
                        .desired_width(110.0),
                )
                .changed();
            let suggestions = document.suggestions(filter.attribute);
            if !suggestions.is_empty() {
                ui.menu_button("Werte", |ui| {
                    egui::ScrollArea::vertical()
                        .max_height(260.0)
                        .show(ui, |ui| {
                            for (value, count) in suggestions {
                                ui.horizontal(|ui| {
                                    if is_color(filter.attribute) {
                                        swatch(ui, parse_color(value));
                                    }
                                    if ui.button(format!("{value} ({count})")).clicked() {
                                        filter.value = value.clone();
                                        changed = true;
                                        ui.close();
                                    }
                                });
                            }
                        });
                })
                .response
                .on_hover_text("Werte aus der SVG");
            }
        });
    }
    if let Some(i) = remove {
        filters.remove(i);
        changed = true;
    }
    if ui.small_button("Bedingung hinzufügen").clicked() {
        filters.push(Filter {
            attribute: Attribute::Color,
            value: document.first_color("#000000"),
            compare: false,
            inverted: false,
        });
        changed = true;
    }
    changed
}

/// Material library editor state: draft and selected material.
struct Editor {
    draft: Vec<Material>,
    selected: usize,
}

pub struct JobUi {
    pub library: Library,
    custom: bool,
    /// "Eigenes Material …" was chosen; keeps the name field while it is empty.
    own_material: bool,
    editor: Option<Editor>,
    document: Document,
}

impl JobUi {
    pub fn load() -> (Self, Option<String>) {
        let (library, custom, error) = match Library::load(&device::config_dir()) {
            Ok((library, custom)) => (library, custom, None),
            Err(e) => (Library::bundled(), false, Some(e)),
        };
        let ui = Self {
            library,
            custom,
            own_material: false,
            editor: None,
            document: Document::default(),
        };
        (ui, error)
    }

    pub fn material(&mut self, ui: &mut egui::Ui, project: &mut Project, dirty: &mut bool) {
        let library_material = find_material(&self.library, &project.material).is_some();
        if library_material {
            self.own_material = false;
        }
        let own = !library_material && (self.own_material || !project.material.is_empty());
        egui::ComboBox::from_id_salt("material")
            .width(200.0)
            .selected_text(if own {
                "Eigenes Material …"
            } else if project.material.is_empty() {
                "Material wählen"
            } else {
                &project.material
            })
            .show_ui(ui, |ui| {
                if ui
                    .selectable_label(!own && project.material.is_empty(), "Material wählen")
                    .clicked()
                {
                    project.material.clear();
                    self.own_material = false;
                    *dirty = true;
                }
                ui.separator();
                for m in &self.library.materials {
                    if ui
                        .selectable_label(project.material == m.name, &m.name)
                        .clicked()
                    {
                        select_material(&self.library, project, Some(&m.name));
                        *dirty = true;
                    }
                }
                ui.separator();
                if ui.selectable_label(own, "Eigenes Material …").clicked() && !own {
                    select_material(&self.library, project, None);
                    self.own_material = true;
                    *dirty = true;
                }
            });
        if own {
            ui.horizontal(|ui| {
                ui.label("Bezeichnung");
                *dirty |= ui
                    .add(
                        egui::TextEdit::singleline(&mut project.material)
                            .hint_text(CUSTOM_MATERIAL)
                            .desired_width(150.0),
                    )
                    .changed();
            });
        }
        let list = find_material(&self.library, &project.material)
            .map(thicknesses)
            .unwrap_or_default();
        if list.is_empty() {
            *dirty |= crate::number(ui, "Stärke", &mut project.thickness_mm, 0.0..=1000.0, " mm");
        } else {
            let title = |t: f32| {
                if t == 0.0 {
                    "Nicht festgelegt".to_string()
                } else {
                    format!("{t} mm")
                }
            };
            ui.horizontal(|ui| {
                ui.label("Stärke");
                egui::ComboBox::from_id_salt("thickness")
                    .width(130.0)
                    .selected_text(title(project.thickness_mm))
                    .show_ui(ui, |ui| {
                        for t in list {
                            if ui
                                .selectable_label(same_thickness(t, project.thickness_mm), title(t))
                                .clicked()
                            {
                                project.thickness_mm = t;
                                *dirty = true;
                            }
                        }
                    });
            });
        }
        if project.steps.is_empty() {
            if let Some(p) = preset(&self.library, project, project.operation) {
                if ui
                    .button(format!(
                        "Profil übernehmen ({} % / {} %)",
                        p.power_percent, p.speed_percent
                    ))
                    .on_hover_text("FAU-Profil für Material, Stärke und Verfahren")
                    .clicked()
                {
                    *dirty |= apply_presets(&self.library, project) > 0;
                }
            } else if project.material.is_empty() {
                ui.small("Material wählen, um passende FAU-Profile zu sehen.");
            } else {
                ui.small("Kein FAU-Profil für diese Kombination; Parameter manuell eingeben.");
            }
        } else {
            let available = project
                .steps
                .iter()
                .filter(|s| preset(&self.library, project, s.operation).is_some())
                .count();
            if ui
                .add_enabled(
                    available > 0,
                    egui::Button::new("Profile für alle Schritte übernehmen"),
                )
                .on_hover_text("Verfügbare FAU-Profile für alle Verfahren übernehmen")
                .on_disabled_hover_text("Kein FAU-Profil für die Verfahren der Schritte")
                .clicked()
            {
                *dirty |= apply_presets(&self.library, project) > 0;
            }
        }
        if ui.button("Materialbibliothek …").clicked() {
            self.editor = Some(Editor {
                draft: self.library.materials.clone(),
                selected: 0,
            });
        }
    }

    pub fn processing(&mut self, ui: &mut egui::Ui, project: &mut Project, dirty: &mut bool) {
        self.document.refresh(&project.svg);
        let mode = assignment_mode(project);
        ui.add_enabled_ui(!project.svg.is_empty(), |ui| {
            ui.horizontal(|ui| {
                for candidate in AssignmentMode::ALL {
                    if ui
                        .selectable_label(mode == candidate, candidate.title())
                        .clicked()
                        && mode != candidate
                    {
                        let count = self.document.objects.len();
                        set_mode(&self.library, project, candidate, count);
                        *dirty = true;
                    }
                }
            });
        });
        // A click above may have changed the mode.
        let mode = assignment_mode(project);
        match mode {
            AssignmentMode::Whole => {
                ui.small("Ein Verfahren für das gesamte Motiv.");
                *dirty |= operation_selector(ui, project);
                if project.operation == Operation::Mark
                    && preset(&self.library, project, Operation::Mark).is_none()
                {
                    ui.small("Konturen markieren. Eigene Leistung einstellen; kein Schnittprofil übernehmen.");
                }
                *dirty |= parameters(
                    ui,
                    &mut project.power_percent,
                    &mut project.speed_percent,
                    &mut project.passes,
                );
                if project.operation.is_raster() {
                    *dirty |= raster_options(ui, "whole", &mut project.raster, project.operation);
                }
            }
            AssignmentMode::Objects => {
                ui.small("Schneiden, gravieren, markieren oder ignorieren.");
                self.objects(ui, project, dirty);
            }
            AssignmentMode::Rules => {
                ui.small("Objekte nach Farbe, Linienstärke, Ebene, Typ oder ID zuordnen.");
                self.rules_header(ui, project, dirty);
            }
        }
        if mode != AssignmentMode::Whole {
            ui.small("Ein LTT-Auftrag je Verfahren: Engrav › Eng3D › Mark › Cut");
            self.steps(ui, project, dirty);
        }
        if mode == AssignmentMode::Rules {
            self.ignore_rules(ui, project, dirty);
        }
        ui.add_space(6.0);
        ui.label("Schnittreihenfolge");
        egui::ComboBox::from_id_salt("cut_order")
            .width(240.0)
            .selected_text(cut_order_title(project.cut_order))
            .show_ui(ui, |ui| {
                for order in [CutOrder::VisiCut, CutOrder::ShortestTravel] {
                    *dirty |= ui
                        .selectable_value(&mut project.cut_order, order, cut_order_title(order))
                        .changed();
                }
            });
        if project.cut_order == CutOrder::ShortestTravel {
            ui.colored_label(
                WARNING,
                "Experimentell: Die Reihenfolge der Schnitte weicht von VisiCut ab. \
                 Den Auftrag am Gerät beaufsichtigen.",
            );
        }
    }

    /// Individual assignment: one row per SVG object.
    fn objects(&mut self, ui: &mut egui::Ui, project: &mut Project, dirty: &mut bool) {
        if let Some(e) = &self.document.error {
            ui.colored_label(WARNING, e);
        }
        if self.document.objects.is_empty() {
            ui.small("Keine auswählbaren Objekte in der SVG.");
            return;
        }
        let library = &self.library;
        egui::ScrollArea::vertical()
            .id_salt("objects")
            .max_height(220.0)
            .show(ui, |ui| {
                for (i, row) in self.document.objects.iter().enumerate() {
                    ui.horizontal(|ui| {
                        swatch(ui, row.color);
                        let current = assignment(project, i);
                        egui::ComboBox::from_id_salt(("object", i))
                            .width(90.0)
                            .selected_text(assignment_title(current))
                            .show_ui(ui, |ui| {
                                let options = Operation::ALL.map(Some);
                                for candidate in options.into_iter().chain([None]) {
                                    if ui
                                        .selectable_label(
                                            current == candidate,
                                            assignment_title(candidate),
                                        )
                                        .clicked()
                                        && current != candidate
                                    {
                                        assign(library, project, i, candidate);
                                        *dirty = true;
                                    }
                                }
                            });
                        ui.add(egui::Label::new(&row.label).truncate())
                            .on_hover_text(&row.label);
                    });
                }
            });
    }

    fn rules_header(&mut self, ui: &mut egui::Ui, project: &mut Project, dirty: &mut bool) {
        egui::ComboBox::from_id_salt("predefined")
            .width(240.0)
            .selected_text("Vorlage übernehmen …")
            .show_ui(ui, |ui| {
                for p in mapping::predefined() {
                    if ui.selectable_label(false, p.name).clicked() {
                        apply_predefined(&self.library, project, &p);
                        *dirty = true;
                    }
                }
            });
        ui.small(
            "Jeder Schritt bearbeitet die Objekte, die alle Bedingungen erfüllen. \
             Ein Objekt kann mehrere Schritte durchlaufen; „Rest“ erfasst alles, \
             was sonst nicht bearbeitet oder ignoriert wird.",
        );
        ui.horizontal_wrapped(|ui| {
            ui.label("Schritt hinzufügen:");
            for operation in Operation::ALL {
                if ui.small_button(operation_title(operation)).clicked() {
                    let step = JobStep {
                        filters: Some(Vec::new()),
                        ..default_step(&self.library, project, operation)
                    };
                    project.steps.push(step);
                    *dirty = true;
                }
            }
        });
    }

    fn steps(&mut self, ui: &mut egui::Ui, project: &mut Project, dirty: &mut bool) {
        let selections = self.document.selections(project);
        if let Err(e) = &selections {
            ui.colored_label(WARNING, e);
        }
        let mut remove = None;
        for i in 0..project.steps.len() {
            let objects = match &selections {
                Ok(s) => s.get(i).map_or(0, Vec::len),
                Err(_) if !is_rule(&project.steps[i]) => project.steps[i].objects.len(),
                Err(_) => 0,
            };
            let has_preset = preset(&self.library, project, project.steps[i].operation).is_some();
            let mut take_preset = false;
            let step = &mut project.steps[i];
            egui::CollapsingHeader::new(format!(
                "{}. {} · {} Objekte",
                i + 1,
                operation_title(step.operation),
                objects
            ))
            .id_salt(("step", i))
            .default_open(true)
            .show(ui, |ui| {
                if is_rule(step) {
                    if ui
                        .checkbox(&mut step.rest, "Rest: alle übrigen Objekte")
                        .changed()
                    {
                        step.filters = if step.rest { None } else { Some(Vec::new()) };
                        step.objects.clear();
                        *dirty = true;
                    }
                    if let Some(list) = &mut step.filters {
                        *dirty |= filters(ui, ("step", i), list, &self.document);
                    }
                }
                if step.operation == Operation::Mark && step.power_percent == 0.0 {
                    ui.small("Zum Markieren zuerst eine eigene Leistung einstellen.");
                }
                *dirty |= parameters(
                    ui,
                    &mut step.power_percent,
                    &mut step.speed_percent,
                    &mut step.passes,
                );
                take_preset = ui
                    .add_enabled(has_preset, egui::Button::new("FAU-Profil übernehmen"))
                    .on_disabled_hover_text("Kein FAU-Profil für Material, Stärke und Verfahren")
                    .clicked();
                let first = ParameterSet {
                    power_percent: step.power_percent,
                    speed_percent: step.speed_percent,
                    passes: step.passes,
                };
                let mut drop = None;
                for (j, set) in step.additional.iter_mut().enumerate() {
                    ui.label(format!("Parametersatz {}", j + 2));
                    *dirty |= parameters(
                        ui,
                        &mut set.power_percent,
                        &mut set.speed_percent,
                        &mut set.passes,
                    );
                    if ui.small_button("Parametersatz entfernen").clicked() {
                        drop = Some(j);
                    }
                }
                if let Some(j) = drop {
                    step.additional.remove(j);
                    *dirty = true;
                }
                if step.additional.len() < 15 && ui.small_button("Weiteren Parametersatz").clicked()
                {
                    step.additional.push(first);
                    *dirty = true;
                }
                if step.operation.is_raster() {
                    *dirty |= raster_options(ui, ("step", i), &mut step.raster, step.operation);
                }
                if is_rule(step) && ui.small_button("Schritt entfernen").clicked() {
                    remove = Some(i);
                }
            });
            if take_preset {
                *dirty |= apply_step_preset(&self.library, project, i);
            }
        }
        if let Some(i) = remove {
            project.steps.remove(i);
            *dirty = true;
        }
    }

    fn ignore_rules(&mut self, ui: &mut egui::Ui, project: &mut Project, dirty: &mut bool) {
        ui.label("Ignorieren");
        let mut remove = None;
        for (i, list) in project.ignore_filters.iter_mut().enumerate() {
            ui.horizontal(|ui| {
                ui.label(format!("Regel {}", i + 1));
                if ui.small_button("Ignorierregel entfernen").clicked() {
                    remove = Some(i);
                }
            });
            *dirty |= filters(ui, ("ignore", i), list, &self.document);
        }
        if let Some(i) = remove {
            project.ignore_filters.remove(i);
            *dirty = true;
        }
        if ui.small_button("Ignorierregel hinzufügen").clicked() {
            project.ignore_filters.push(vec![Filter {
                attribute: Attribute::Color,
                value: self.document.first_color("#0000ff"),
                compare: false,
                inverted: false,
            }]);
            *dirty = true;
        }
    }

    pub fn windows(&mut self, ctx: &egui::Context, status: &mut String) {
        let Some(Editor {
            mut draft,
            mut selected,
        }) = self.editor.take()
        else {
            return;
        };
        let mut open = true;
        let mut keep = true;
        egui::Window::new("Materialbibliothek")
            .open(&mut open)
            .default_size([700.0, 460.0])
            .show(ctx, |ui| {
                ui.horizontal_top(|ui| {
                    ui.vertical(|ui| {
                        ui.set_width(190.0);
                        egui::ScrollArea::vertical()
                            .id_salt("materials")
                            .max_height(360.0)
                            .show(ui, |ui| {
                                for (i, m) in draft.iter().enumerate() {
                                    ui.selectable_value(&mut selected, i, &m.name);
                                }
                            });
                        ui.horizontal_wrapped(|ui| {
                            if ui.button("Neu").clicked() {
                                selected = new_material(&mut draft);
                            }
                            if ui
                                .add_enabled(
                                    selected < draft.len(),
                                    egui::Button::new("Duplizieren"),
                                )
                                .clicked()
                            {
                                selected = duplicate_material(&mut draft, selected);
                            }
                            if ui
                                .add_enabled(selected < draft.len(), egui::Button::new("Entfernen"))
                                .clicked()
                            {
                                draft.remove(selected);
                                selected = selected.min(draft.len().saturating_sub(1));
                            }
                        });
                    });
                    ui.separator();
                    ui.vertical(|ui| {
                        let Some(material) = draft.get_mut(selected) else {
                            ui.label("Material auswählen");
                            return;
                        };
                        ui.text_edit_singleline(&mut material.name);
                        let mut drop = None;
                        egui::ScrollArea::vertical()
                            .id_salt("profiles")
                            .max_height(320.0)
                            .show(ui, |ui| {
                                egui::Grid::new("profiles").num_columns(5).show(ui, |ui| {
                                    ui.label("Stärke");
                                    ui.label("Verfahren");
                                    ui.label("Leistung");
                                    ui.label("Geschwindigkeit");
                                    ui.end_row();
                                    for (j, p) in material.profiles.iter_mut().enumerate() {
                                        ui.add(
                                            egui::DragValue::new(&mut p.thickness_mm)
                                                .range(0.0..=1000.0)
                                                .suffix(" mm"),
                                        );
                                        egui::ComboBox::from_id_salt(("profile", j))
                                            .selected_text(operation_title(p.operation))
                                            .show_ui(ui, |ui| {
                                                for o in Operation::ALL {
                                                    ui.selectable_value(
                                                        &mut p.operation,
                                                        o,
                                                        operation_title(o),
                                                    );
                                                }
                                            });
                                        ui.add(
                                            egui::DragValue::new(&mut p.power_percent)
                                                .range(0.0..=100.0)
                                                .suffix(" %"),
                                        );
                                        ui.add(
                                            egui::DragValue::new(&mut p.speed_percent)
                                                .range(0.0..=100.0)
                                                .suffix(" %"),
                                        );
                                        if ui
                                            .small_button("🗑")
                                            .on_hover_text("Profil entfernen")
                                            .clicked()
                                        {
                                            drop = Some(j);
                                        }
                                        ui.end_row();
                                    }
                                });
                            });
                        if let Some(j) = drop {
                            material.profiles.remove(j);
                        }
                        if ui.button("Profil hinzufügen").clicked() {
                            let thickness =
                                material.profiles.last().map_or(3.0, |p| p.thickness_mm);
                            material.profiles.push(MaterialProfile {
                                thickness_mm: thickness,
                                operation: Operation::Cut,
                                power_percent: 100.0,
                                speed_percent: 10.0,
                                source: None,
                            });
                        }
                        ui.small(
                            "Leistung und Geschwindigkeit in Prozent wie am LTT. Je Stärke ist \
                             ein Profil pro Verfahren möglich; 0 % Leistung gilt als \
                             „nicht übernehmbar“.",
                        );
                    });
                });
                ui.separator();
                ui.horizontal(|ui| {
                    if ui.button("Importieren …").clicked()
                        && let Some(path) = rfd::FileDialog::new()
                            .add_filter("JSON", &["json"])
                            .pick_file()
                    {
                        let mut library = Library {
                            materials: draft.clone(),
                            ..self.library.clone()
                        };
                        match std::fs::read(&path)
                            .map_err(|e| e.to_string())
                            .and_then(|b| Library::import(&b))
                        {
                            Ok(other) => {
                                *status =
                                    format!("{} Materialien importiert", library.merge(other));
                                draft = library.materials;
                            }
                            Err(e) => *status = e,
                        }
                    }
                    if ui.button("Exportieren …").clicked()
                        && let Some(path) = rfd::FileDialog::new()
                            .add_filter("JSON", &["json"])
                            .set_file_name("Materialien.json")
                            .save_file()
                    {
                        let library = Library {
                            materials: draft.clone(),
                            ..self.library.clone()
                        };
                        *status = match library.export(&path) {
                            Ok(()) => "Materialbibliothek exportiert".into(),
                            Err(e) => e,
                        };
                    }
                    if ui
                        .add_enabled(self.custom, egui::Button::new("FAU wiederherstellen"))
                        .clicked()
                    {
                        match Library::reset(&device::config_dir()) {
                            Ok(library) => {
                                draft = library.materials.clone();
                                selected = 0;
                                self.library = library;
                                self.custom = false;
                                *status = "FAU-Materialbibliothek wiederhergestellt".into();
                            }
                            Err(e) => *status = e,
                        }
                    }
                    ui.small(if self.custom {
                        "Eigene Bibliothek"
                    } else {
                        "FAU-Bibliothek"
                    });
                    let changed = draft != self.library.materials;
                    if ui
                        .add_enabled(changed, egui::Button::new("Verwerfen"))
                        .on_hover_text("Ungesicherte Änderungen verwerfen")
                        .clicked()
                    {
                        draft = self.library.materials.clone();
                        selected = selected.min(draft.len().saturating_sub(1));
                    }
                    if ui
                        .add_enabled(changed, egui::Button::new("Sichern"))
                        .clicked()
                    {
                        let library = Library {
                            materials: draft.clone(),
                            ..self.library.clone()
                        };
                        match library.save(&device::config_dir()) {
                            Ok(()) => {
                                self.library = library;
                                self.custom = true;
                                *status = "Materialbibliothek gesichert".into();
                            }
                            Err(e) => *status = e,
                        }
                    }
                    if ui.button("Schließen").clicked() {
                        keep = false;
                    }
                });
            });
        if open && keep {
            self.editor = Some(Editor { draft, selected });
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SVG: &str = r##"<svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="60mm" viewBox="0 0 100 60">
        <rect id="Rahmen" x="1" y="1" width="98" height="58" fill="none" stroke="#ff0000"/>
        <circle id="Kreis" cx="25" cy="30" r="16" fill="#222222"/>
        <path id="Dreieck" d="M55 15 L85 30 L55 45 Z" fill="none" stroke="#00ff00"/>
    </svg>"##;

    fn profile(thickness_mm: f32, operation: Operation, power: f32) -> MaterialProfile {
        MaterialProfile {
            thickness_mm,
            operation,
            power_percent: power,
            speed_percent: 20.0,
            source: None,
        }
    }

    fn library() -> Library {
        Library {
            source: "Test".into(),
            device: "LTT iLaser 4000".into(),
            materials: vec![Material {
                id: "pappel".into(),
                name: "Pappel".into(),
                profiles: vec![
                    profile(4.0, Operation::Cut, 90.0),
                    profile(3.0, Operation::Cut, 80.0),
                    profile(3.0, Operation::Engrave, 30.0),
                    profile(3.0, Operation::Mark, 0.0),
                    profile(0.0, Operation::Engrave, 25.0),
                ],
            }],
        }
    }

    fn project() -> Project {
        Project {
            svg: SVG.into(),
            material: "Pappel".into(),
            thickness_mm: 3.0,
            operation: Operation::Engrave,
            power_percent: 55.0,
            speed_percent: 66.0,
            passes: 2,
            ..Default::default()
        }
    }

    #[test]
    fn thickness_list_is_sorted_and_unique() {
        assert_eq!(thicknesses(&library().materials[0]), [0.0, 3.0, 4.0]);
    }

    #[test]
    fn selecting_material_keeps_or_fixes_thickness_and_allows_own_names() {
        let library = library();
        let mut p = project();
        p.material.clear();
        p.thickness_mm = 3.0;
        select_material(&library, &mut p, Some("Pappel"));
        assert_eq!((p.material.as_str(), p.thickness_mm), ("Pappel", 3.0));
        p.thickness_mm = 7.0;
        select_material(&library, &mut p, Some("Pappel"));
        assert_eq!(p.thickness_mm, 0.0);
        select_material(&library, &mut p, None);
        assert_eq!(p.material, CUSTOM_MATERIAL);
        assert!(preset(&library, &p, Operation::Cut).is_none());
    }

    #[test]
    fn default_steps_use_profiles_and_mark_without_profile_starts_at_zero() {
        let library = library();
        let p = project();
        let cut = default_step(&library, &p, Operation::Cut);
        assert_eq!((cut.power_percent, cut.speed_percent), (80.0, 20.0));
        assert_eq!(cut.passes, 2);
        // The 0 % mark profile is not applicable.
        let mark = default_step(&library, &p, Operation::Mark);
        assert_eq!((mark.power_percent, mark.speed_percent), (0.0, 100.0));
        let engrave3d = default_step(&library, &p, Operation::Engrave3d);
        assert_eq!(
            (engrave3d.power_percent, engrave3d.speed_percent),
            (55.0, 66.0)
        );
        assert_eq!(engrave3d.raster, p.raster);
        let mut marking = p.clone();
        select_operation(&mut marking, Operation::Mark);
        assert_eq!(
            (marking.power_percent, marking.speed_percent, marking.passes),
            (0.0, 100.0, 1)
        );
        let mark = default_step(&library, &marking, Operation::Mark);
        assert_eq!(mark.power_percent, 0.0);
    }

    #[test]
    fn individual_assignment_round_trips_through_steps() {
        let library = library();
        let mut p = project();
        assert_eq!(assignment_mode(&p), AssignmentMode::Whole);
        set_mode(&library, &mut p, AssignmentMode::Objects, 3);
        assert_eq!(assignment_mode(&p), AssignmentMode::Objects);
        let operations: Vec<_> = p.steps.iter().map(|s| s.operation).collect();
        assert_eq!(
            operations,
            [Operation::Engrave, Operation::Mark, Operation::Cut]
        );
        assert_eq!(p.steps[0].objects, [0, 1, 2]);
        assign(&library, &mut p, 0, Some(Operation::Cut));
        assign(&library, &mut p, 2, None);
        assert_eq!(assignment(&p, 0), Some(Operation::Cut));
        assert_eq!(assignment(&p, 1), Some(Operation::Engrave));
        assert_eq!(assignment(&p, 2), None);
        assign(&library, &mut p, 1, Some(Operation::Engrave3d));
        assert_eq!(p.steps.len(), 4);
        assert_eq!(p.steps[3].objects, [1]);
        assert!(p.steps[0].objects.is_empty());
        let selections = mapping::resolve(&p).unwrap();
        assert_eq!(selections, [vec![], vec![], vec![0], vec![1]]);
        // 3D engraving as the motif operation keeps all objects assigned.
        let mut deep = project();
        deep.operation = Operation::Engrave3d;
        set_mode(&library, &mut deep, AssignmentMode::Objects, 3);
        assert_eq!(assignment(&deep, 2), Some(Operation::Engrave3d));
        set_mode(&library, &mut deep, AssignmentMode::Whole, 3);
        assert!(deep.steps.is_empty());
    }

    #[test]
    fn rules_mode_applies_first_template_and_presets_cover_all_steps() {
        let library = library();
        let mut p = project();
        set_mode(&library, &mut p, AssignmentMode::Rules, 3);
        assert_eq!(assignment_mode(&p), AssignmentMode::Rules);
        assert_eq!(p.steps.len(), mapping::predefined()[0].rules.len());
        assert!(!p.ignore_filters.is_empty());
        for step in &mut p.steps {
            (step.power_percent, step.speed_percent, step.passes) = (1.0, 1.0, 3);
        }
        // Mark has no usable profile; cut and engrave do.
        assert_eq!(apply_presets(&library, &mut p), 2);
        let cut = p
            .steps
            .iter()
            .find(|s| s.operation == Operation::Cut)
            .unwrap();
        assert_eq!((cut.power_percent, cut.passes), (80.0, 1));
        let mark = p
            .steps
            .iter()
            .find(|s| s.operation == Operation::Mark)
            .unwrap();
        assert_eq!(mark.power_percent, 1.0);
        set_mode(&library, &mut p, AssignmentMode::Objects, 3);
        assert!(p.ignore_filters.is_empty());
        let mut whole = project();
        assert_eq!(apply_presets(&library, &mut whole), 1);
        assert_eq!((whole.power_percent, whole.passes), (30.0, 1));
    }

    #[test]
    fn duplicating_materials_picks_free_names_and_ids() {
        let mut materials = library().materials;
        let copy = duplicate_material(&mut materials, 0);
        assert_eq!(materials[copy].name, "Pappel (2)");
        assert_eq!(materials[copy].profiles, materials[0].profiles);
        let again = duplicate_material(&mut materials, 0);
        assert_eq!(materials[again].name, "Pappel (3)");
        assert_ne!(materials[copy].id, materials[again].id);
        let new = new_material(&mut materials);
        let other = new_material(&mut materials);
        assert_eq!(materials[new].name, "Neues Material");
        assert_eq!(materials[other].name, "Neues Material (2)");
        let library = Library {
            materials,
            ..library()
        };
        library.validate().unwrap();
    }

    #[test]
    fn document_lists_objects_values_and_colours() {
        let mut document = Document::default();
        document.refresh(SVG);
        assert_eq!(document.objects.len(), 3);
        assert!(document.objects[0].label.contains("Rahmen"));
        assert_eq!(
            document.objects[0].color,
            Some(egui::Color32::from_rgb(255, 0, 0))
        );
        assert_eq!(
            document.objects[1].color,
            Some(egui::Color32::from_rgb(0x22, 0x22, 0x22))
        );
        assert!(
            document
                .suggestions(Attribute::Id)
                .iter()
                .any(|(v, n)| v == "Kreis" && *n == 1)
        );
        assert!(parse_color(&document.first_color("#0000ff")).is_some());
        assert_eq!(parse_color("#00ff00"), Some(egui::Color32::GREEN));
        assert_eq!(parse_color("none"), None);
        assert_eq!(parse_color("#12345"), None);
    }
}
