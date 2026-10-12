//! Bausteine für die Seitenleiste im Stil der macOS-Oberfläche: Abschnitte
//! als abgerundete Karten mit Überschrift, Zeilen mit Bezeichnung links und
//! Bedienelement rechts, segmentierte Auswahl.

use eframe::egui::{self, Color32, CornerRadius, RichText, Stroke, Vec2};

/// Akzentfarbe (Systemblau wie unter macOS).
pub const ACCENT: Color32 = Color32::from_rgb(0, 122, 255);
/// Hintergrund der Seitenleiste; die Karten heben sich weiß davon ab.
pub const SIDEBAR: Color32 = Color32::from_gray(236);
/// Hintergrund hinter dem Arbeitsbett.
pub const CANVAS: Color32 = Color32::from_gray(226);
const CARD_STROKE: Color32 = Color32::from_gray(218);
const DIVIDER: Color32 = Color32::from_gray(212);

/// Abstände, Rundungen und Rahmen etwas näher an macOS.
pub fn apply_style(ctx: &egui::Context) {
    ctx.all_styles_mut(|style| {
        style.spacing.item_spacing = Vec2::new(8.0, 4.0);
        style.spacing.button_padding = Vec2::new(8.0, 3.0);
        style.spacing.interact_size.y = 20.0;
        let visuals = &mut style.visuals;
        visuals.window_corner_radius = CornerRadius::same(10);
        visuals.menu_corner_radius = CornerRadius::same(8);
        visuals.hyperlink_color = ACCENT;
        for widget in [
            &mut visuals.widgets.noninteractive,
            &mut visuals.widgets.inactive,
            &mut visuals.widgets.hovered,
            &mut visuals.widgets.active,
            &mut visuals.widgets.open,
        ] {
            widget.corner_radius = CornerRadius::same(4);
        }
        visuals.widgets.inactive.bg_stroke = Stroke::new(1.0, Color32::from_gray(205));
        visuals.widgets.inactive.weak_bg_fill = Color32::WHITE;
        visuals.widgets.inactive.bg_fill = Color32::WHITE;
        visuals.widgets.hovered.weak_bg_fill = Color32::from_gray(244);
        visuals.widgets.hovered.bg_fill = Color32::from_gray(244);
        visuals.widgets.hovered.bg_stroke = Stroke::new(1.0, Color32::from_gray(185));
        visuals.widgets.active.weak_bg_fill = Color32::from_gray(228);
        visuals.widgets.active.bg_fill = Color32::from_gray(228);
    });
}

/// Abschnitt mit kleiner grauer Überschrift und Karte darunter.
pub fn section<R>(ui: &mut egui::Ui, title: &str, add: impl FnOnce(&mut egui::Ui) -> R) -> R {
    ui.add_space(6.0);
    ui.horizontal(|ui| {
        ui.add_space(4.0);
        ui.label(RichText::new(title).strong().color(Color32::from_gray(90)));
    });
    card(ui, add)
}

/// Weiße, abgerundete Karte über die volle Breite.
pub fn card<R>(ui: &mut egui::Ui, add: impl FnOnce(&mut egui::Ui) -> R) -> R {
    let inner = egui::Frame::new()
        .fill(Color32::WHITE)
        .stroke(Stroke::new(1.0, CARD_STROKE))
        .corner_radius(8.0)
        .inner_margin(egui::Margin::symmetric(10, 8))
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            add(ui)
        })
        .inner;
    ui.add_space(4.0);
    inner
}

/// Zeile wie `LabeledContent`: Bezeichnung links, Bedienelement rechtsbündig.
/// Die Elemente in `add` stehen von rechts nach links.
pub fn row<R>(
    ui: &mut egui::Ui,
    label: impl Into<egui::WidgetText>,
    add: impl FnOnce(&mut egui::Ui) -> R,
) -> R {
    ui.horizontal(|ui| {
        ui.label(label);
        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), add)
            .inner
    })
    .inner
}

/// Kleiner grauer Erläuterungstext, umbrechend.
pub fn caption(ui: &mut egui::Ui, text: impl Into<String>) {
    ui.add(
        egui::Label::new(
            RichText::new(text.into())
                .size(11.5)
                .color(Color32::from_gray(95)),
        )
        .wrap(),
    );
}

/// Dünne waagerechte Linie ohne Abstand (wie `Divider`).
pub fn divider(ui: &mut egui::Ui) {
    let (rect, _) =
        ui.allocate_exact_size(Vec2::new(ui.available_width(), 1.0), egui::Sense::hover());
    ui.painter()
        .hline(rect.x_range(), rect.center().y, Stroke::new(1.0, DIVIDER));
}

/// Segmentierte Auswahl über die volle Breite; liefert die neu gewählte
/// Option.
pub fn segmented<T: PartialEq + Copy>(
    ui: &mut egui::Ui,
    current: T,
    options: &[(T, &str)],
) -> Option<T> {
    let mut chosen = None;
    egui::Frame::new()
        .fill(Color32::from_gray(228))
        .corner_radius(7.0)
        .inner_margin(2.0)
        .show(ui, |ui| {
            ui.spacing_mut().item_spacing.x = 2.0;
            let count = options.len().max(1) as f32;
            let width = (ui.available_width() - 2.0 * (count - 1.0)) / count;
            ui.horizontal(|ui| {
                for &(value, title) in options {
                    let selected = value == current;
                    let button = egui::Button::new(RichText::new(title).color(if selected {
                        Color32::from_gray(20)
                    } else {
                        Color32::from_gray(80)
                    }))
                    .fill(if selected {
                        Color32::WHITE
                    } else {
                        Color32::TRANSPARENT
                    })
                    .stroke(if selected {
                        Stroke::new(1.0, Color32::from_gray(200))
                    } else {
                        Stroke::NONE
                    })
                    .corner_radius(5.0)
                    .truncate();
                    if ui.add_sized([width, 22.0], button).clicked() && !selected {
                        chosen = Some(value);
                    }
                }
            });
        });
    chosen
}

/// Farbige Auswahlfläche wie der Verfahrenswähler unter macOS.
pub fn tinted_choice(
    ui: &mut egui::Ui,
    title: &str,
    color: Color32,
    selected: bool,
    width: f32,
) -> bool {
    let alpha = |a: f32| {
        Color32::from_rgba_unmultiplied(color.r(), color.g(), color.b(), (a * 255.0) as u8)
    };
    let text = RichText::new(title).size(12.0).color(color);
    // Feste Breite: längere Übersetzungen dürfen die Seitenleiste nicht verbreitern.
    ui.add_sized(
        [width, 30.0],
        egui::Button::new(if selected { text.strong() } else { text })
            .fill(alpha(if selected { 0.18 } else { 0.04 }))
            .stroke(Stroke::new(1.0, alpha(if selected { 0.8 } else { 0.2 })))
            .corner_radius(6.0)
            .wrap(),
    )
    .clicked()
}

/// Hervorgehobene Schaltfläche (wie `.borderedProminent`).
pub fn prominent(ui: &mut egui::Ui, enabled: bool, title: &str) -> bool {
    ui.add_enabled(
        enabled,
        egui::Button::new(RichText::new(title).color(Color32::WHITE))
            .fill(ACCENT)
            .stroke(Stroke::NONE)
            .min_size(Vec2::new(0.0, 28.0)),
    )
    .clicked()
}
