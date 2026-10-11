//! Job preview, time estimate and chronological laser simulation for the egui
//! interface. The job is prepared on a background thread; the result becomes
//! stale as soon as the project changes.
use crate::jobs_ui::operation_title;
use eframe::egui::{self, Color32, Pos2, Rect, Stroke, Vec2};
use std::hash::Hasher;
use std::sync::mpsc::{Receiver, TryRecvError};
use visicut_core::{
    ltt::{self, PreparedJob},
    project::{Operation, Project},
    timeline::{MotionKind, Program, Timeline},
};

/// What the main window should do after the preview dialog was drawn.
pub enum Action {
    Send,
    Export,
}

const SPEEDS: [f64; 5] = [1.0, 5.0, 10.0, 25.0, 100.0];
/// Height of one engraving row at 500 DPI.
const RASTER_ROW_MM: f64 = 25.4 / 500.0;
const MAX_TEXTURE_SIDE: u32 = 2048;

/// Result of the background thread: the job plus decoded images, ready to be
/// uploaded as textures on the UI thread.
struct Computed {
    job: PreparedJob,
    preview: Option<egui::ColorImage>,
    rasters: Vec<Option<egui::ColorImage>>,
}

/// Consecutive motions of one kind forming a connected polyline.
struct Chain {
    kind: MotionKind,
    /// Index of the first motion; the chain holds `points.len() - 1` motions.
    start: usize,
    points: Vec<[f64; 2]>,
}

struct Ready {
    job: PreparedJob,
    preview: Option<egui::TextureHandle>,
    rasters: Vec<Option<egui::TextureHandle>>,
    chains: Vec<Vec<Chain>>,
    motion_bounds: Option<[f64; 4]>,
    bytes: usize,
}

enum State {
    Idle,
    /// A result existed or was being computed, but the project changed since.
    Stale,
    Computing {
        fingerprint: u64,
        receiver: Receiver<Result<Computed, String>>,
    },
    Ready {
        fingerprint: u64,
        ready: Box<Ready>,
    },
    Failed {
        fingerprint: u64,
        error: String,
    },
}

pub struct PreviewUi {
    pub open: bool,
    /// Show the processing preview on the bed instead of the plain motif.
    pub show_processing: bool,
    state: State,
    playback: Playback,
    last_check: f64,
}

impl Default for PreviewUi {
    fn default() -> Self {
        Self {
            open: false,
            show_processing: true,
            state: State::Idle,
            playback: Playback::default(),
            last_check: f64::NEG_INFINITY,
        }
    }
}

impl PreviewUi {
    fn ready(&self) -> Option<&Ready> {
        match &self.state {
            State::Ready { ready, .. } => Some(ready),
            _ => None,
        }
    }

    /// Opens the dialog and prepares the job unless a current result exists.
    pub fn request(&mut self, ctx: &egui::Context, project: &Project, status: &mut String) {
        self.open = true;
        let current = fingerprint(project);
        match &self.state {
            State::Ready { fingerprint, .. } | State::Computing { fingerprint, .. }
                if *fingerprint == current =>
            {
                return;
            }
            _ => {}
        }
        self.playback.seek(0.0, 0.0);
        let snapshot = project.clone();
        let (sender, receiver) = std::sync::mpsc::channel();
        let repaint = ctx.clone();
        std::thread::spawn(move || {
            let _ = sender.send(compute(&snapshot));
            repaint.request_repaint();
        });
        self.state = State::Computing {
            fingerprint: current,
            receiver,
        };
        *status = "Auftragsvorschau und Bearbeitungszeit werden berechnet …".into();
    }

    /// Toolbar entry: compute button, estimate and the processing toggle.
    pub fn toolbar(&mut self, ui: &mut egui::Ui, project: &Project, status: &mut String) {
        let computing = matches!(self.state, State::Computing { .. });
        if ui
            .add_enabled(
                !computing && !project.svg.is_empty(),
                egui::Button::new("Vorschau & Zeit …"),
            )
            .on_hover_text("Auftragsvorschau, Zeitschätzung und Simulation (Strg/⌘+Umschalt+P)")
            .clicked()
        {
            self.request(ui.ctx(), project, status);
        }
        match &self.state {
            State::Computing { .. } => {
                ui.spinner();
            }
            State::Ready { ready, .. } => {
                ui.label(format!("Ca. {}", duration(ready.job.estimated_seconds)));
                ui.checkbox(&mut self.show_processing, "Bearbeitung anzeigen")
                    .on_hover_text("Rot: Schnitt · Blau: Gravur · Orange: 3D · Violett: Markieren");
            }
            State::Stale => {
                ui.small("Nach Änderungen neu berechnen");
            }
            State::Idle | State::Failed { .. } => {}
        }
    }

    /// Paints the processing preview over the motif on the bed.
    pub fn overlay(&self, painter: &egui::Painter, motif: Rect) {
        if !self.show_processing {
            return;
        }
        if let Some(texture) = self.ready().and_then(|r| r.preview.as_ref()) {
            painter.rect_filled(motif, 0.0, Color32::from_white_alpha(170));
            painter.image(texture.id(), motif, full_uv(), Color32::WHITE);
        }
    }

    /// Polls the background thread, detects stale results, handles the
    /// shortcut and draws the dialog.
    pub fn windows(
        &mut self,
        ctx: &egui::Context,
        project: &Project,
        status: &mut String,
        send_enabled: bool,
    ) -> Option<Action> {
        if ctx.input_mut(|i| {
            i.consume_key(
                egui::Modifiers::COMMAND | egui::Modifiers::SHIFT,
                egui::Key::P,
            )
        }) {
            self.request(ctx, project, status);
        }
        self.poll(ctx, status);
        self.invalidate(ctx, project, status);
        if !self.open {
            self.playback.pause();
            return None;
        }
        let mut open = self.open;
        let mut action = None;
        let mut recompute = false;
        egui::Window::new("Vorschau & Zeit")
            .open(&mut open)
            .default_size([920.0, 640.0])
            .resizable(true)
            .collapsible(false)
            .show(ctx, |ui| {
                egui::ScrollArea::vertical().show(ui, |ui| match &self.state {
                    State::Idle | State::Stale => {
                        ui.label(if matches!(self.state, State::Stale) {
                            "Auftrag geändert. Vorschau und Zeit neu berechnen."
                        } else {
                            "Noch keine Vorschau berechnet."
                        });
                        recompute = ui.button("Vorschau & Zeit berechnen").clicked();
                    }
                    State::Computing { .. } => {
                        ui.horizontal(|ui| {
                            ui.spinner();
                            ui.label("Auftragsvorschau und Bearbeitungszeit werden berechnet …");
                        });
                    }
                    State::Failed { error, .. } => {
                        ui.colored_label(WARNING, format!("⚠ {error}"));
                        recompute = ui.button("Erneut versuchen").clicked();
                    }
                    State::Ready { ready, .. } => {
                        action = dialog(ui, ready, project, &mut self.playback, send_enabled);
                    }
                });
            });
        if recompute {
            self.request(ctx, project, status);
        }
        if matches!(action, Some(Action::Send)) {
            open = false;
        }
        self.open = open;
        if !self.open {
            self.playback.pause();
        }
        if self.playback.playing {
            ctx.request_repaint();
        }
        action
    }

    fn poll(&mut self, ctx: &egui::Context, status: &mut String) {
        let State::Computing {
            fingerprint,
            receiver,
        } = &self.state
        else {
            return;
        };
        let fingerprint = *fingerprint;
        match receiver.try_recv() {
            Ok(Ok(computed)) => {
                let ready = upload(ctx, computed);
                *status = format!(
                    "Geschätzte Bearbeitungszeit: ca. {}",
                    duration(ready.job.estimated_seconds)
                );
                self.playback.seek(0.0, ready.job.estimated_seconds);
                self.state = State::Ready {
                    fingerprint,
                    ready: Box::new(ready),
                };
            }
            Ok(Err(error)) => {
                *status = "Auftrag konnte nicht vorbereitet werden.".into();
                self.state = State::Failed { fingerprint, error };
            }
            Err(TryRecvError::Disconnected) => {
                *status = "Vorschauberechnung unerwartet abgebrochen.".into();
                self.state = State::Failed {
                    fingerprint,
                    error: "Vorschauberechnung unerwartet abgebrochen".into(),
                };
            }
            Err(TryRecvError::Empty) => {
                ctx.request_repaint_after(std::time::Duration::from_millis(200));
            }
        }
    }

    fn invalidate(&mut self, ctx: &egui::Context, project: &Project, status: &mut String) {
        let previous = match &self.state {
            State::Idle | State::Stale => return,
            State::Computing { fingerprint, .. }
            | State::Ready { fingerprint, .. }
            | State::Failed { fingerprint, .. } => *fingerprint,
        };
        // Hashing a large SVG every frame during playback is wasteful; the
        // project can only change through user input anyway.
        let now = ctx.input(|i| i.time);
        if self.playback.playing && now - self.last_check < 0.25 {
            return;
        }
        self.last_check = now;
        if fingerprint(project) == previous {
            return;
        }
        if matches!(self.state, State::Failed { .. }) {
            self.state = State::Idle;
        } else {
            *status = "Auftrag geändert. Vorschau und Zeit neu berechnen.".into();
            self.state = State::Stale;
        }
        self.playback.seek(0.0, 0.0);
        ctx.request_repaint();
    }
}

/// Hash of everything that influences the prepared job.
pub fn fingerprint(project: &Project) -> u64 {
    struct HashWriter(std::collections::hash_map::DefaultHasher);
    impl std::io::Write for HashWriter {
        fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
            self.0.write(buf);
            Ok(buf.len())
        }
        fn flush(&mut self) -> std::io::Result<()> {
            Ok(())
        }
    }
    let mut writer = HashWriter(Default::default());
    // Streaming avoids copying a large embedded SVG.
    let _ = serde_json::to_writer(&mut writer, project);
    writer.0.finish()
}

fn compute(project: &Project) -> Result<Computed, String> {
    let job = ltt::prepare(project)?;
    let preview = decode_png(&job.preview_png);
    let rasters = job
        .timeline
        .programs
        .iter()
        .map(|program| decode_png(&program.raster_preview_png))
        .collect();
    Ok(Computed {
        job,
        preview,
        rasters,
    })
}

fn decode_png(bytes: &[u8]) -> Option<egui::ColorImage> {
    if bytes.is_empty() {
        return None;
    }
    let mut image = image::load_from_memory_with_format(bytes, image::ImageFormat::Png).ok()?;
    if image.width() > MAX_TEXTURE_SIDE || image.height() > MAX_TEXTURE_SIDE {
        image = image.thumbnail(MAX_TEXTURE_SIDE, MAX_TEXTURE_SIDE);
    }
    let rgba = image.to_rgba8();
    Some(egui::ColorImage::from_rgba_unmultiplied(
        [rgba.width() as usize, rgba.height() as usize],
        rgba.as_raw(),
    ))
}

fn upload(ctx: &egui::Context, computed: Computed) -> Ready {
    let options = egui::TextureOptions::LINEAR;
    let preview = computed
        .preview
        .map(|image| ctx.load_texture("Auftragsvorschau", image, options));
    let rasters = computed
        .rasters
        .into_iter()
        .enumerate()
        .map(|(index, image)| {
            image.map(|image| ctx.load_texture(format!("Gravur {index}"), image, options))
        })
        .collect();
    let job = computed.job;
    Ready {
        chains: job.timeline.programs.iter().map(chains).collect(),
        motion_bounds: motion_bounds(&job.timeline),
        bytes: job.jobs.iter().map(|j| j.bytes.len()).sum(),
        preview,
        rasters,
        job,
    }
}

/// Vector polylines of a program; rasters are shown as images instead.
fn chains(program: &Program) -> Vec<Chain> {
    let mut chains: Vec<Chain> = Vec::new();
    if program.operation.is_raster() {
        return chains;
    }
    for (index, motion) in program.motions.iter().enumerate() {
        if motion.kind == MotionKind::Dwell {
            continue;
        }
        match chains.last_mut() {
            Some(chain)
                if chain.kind == motion.kind
                    && chain.start + chain.points.len() - 1 == index
                    && chain.points.last() == Some(&motion.from_mm) =>
            {
                chain.points.push(motion.to_mm);
            }
            _ => chains.push(Chain {
                kind: motion.kind,
                start: index,
                points: vec![motion.from_mm, motion.to_mm],
            }),
        }
    }
    chains
}

/// Bounding box [min x, min y, max x, max y] of all motions in mm.
fn motion_bounds(timeline: &Timeline) -> Option<[f64; 4]> {
    let mut points = timeline
        .programs
        .iter()
        .flat_map(|p| p.motions.iter())
        .flat_map(|m| [m.from_mm, m.to_mm]);
    let first = points.next()?;
    Some(
        points.fold([first[0], first[1], first[0], first[1]], |b, p| {
            [
                b[0].min(p[0]),
                b[1].min(p[1]),
                b[2].max(p[0]),
                b[3].max(p[1]),
            ]
        }),
    )
}

/// Visible bed section in mm: motif and motions with a margin.
pub fn viewport(project: &Project, motions: Option<[f64; 4]>) -> [f64; 4] {
    let mut b = [
        project.x_mm as f64,
        project.y_mm as f64,
        (project.x_mm + project.width_mm) as f64,
        (project.y_mm + project.height_mm) as f64,
    ];
    if let Some(m) = motions {
        b = [
            b[0].min(m[0]),
            b[1].min(m[1]),
            b[2].max(m[2]),
            b[3].max(m[3]),
        ];
    }
    let dx = 2f64.max((b[2] - b[0]) * 0.05);
    let dy = 2f64.max((b[3] - b[1]) * 0.05);
    [b[0] - dx, b[1] - dy, b[2] + dx, b[3] + dy]
}

/// Simulation state at one point in time.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Frame {
    pub run_index: usize,
    pub program_index: usize,
    pub pass: u32,
    /// `None` while travelling to the start of a run.
    pub motion_index: Option<usize>,
    pub kind: MotionKind,
    pub from: [f64; 2],
    pub position: [f64; 2],
    pub finished: bool,
}

fn interpolate(a: [f64; 2], b: [f64; 2], fraction: f64) -> [f64; 2] {
    [
        a[0] + (b[0] - a[0]) * fraction,
        a[1] + (b[1] - a[1]) * fraction,
    ]
}

/// Laser head state at `seconds` after the job start.
pub fn frame_at(timeline: &Timeline, seconds: f64) -> Option<Frame> {
    if timeline.runs.is_empty() {
        return None;
    }
    let time = seconds.max(0.0).min(timeline.duration_seconds);
    let finished = time >= timeline.duration_seconds;
    let run_index = timeline
        .runs
        .partition_point(|r| r.end_seconds <= time)
        .min(timeline.runs.len() - 1);
    let run = &timeline.runs[run_index];
    let program = &timeline.programs[run.program_index];
    let base = Frame {
        run_index,
        program_index: run.program_index,
        pass: run.pass,
        motion_index: None,
        kind: MotionKind::Travel,
        from: run.from_mm,
        position: program.start_mm,
        finished,
    };
    if time < run.entry_end_seconds {
        let fraction =
            (time - run.start_seconds) / (run.entry_end_seconds - run.start_seconds).max(1e-12);
        return Some(Frame {
            position: interpolate(run.from_mm, program.start_mm, fraction.clamp(0.0, 1.0)),
            finished: false,
            ..base
        });
    }
    let local = time - run.entry_end_seconds;
    if program.motions.is_empty() {
        return Some(Frame {
            from: program.start_mm,
            position: program.end_mm,
            ..base
        });
    }
    let index = program
        .motions
        .partition_point(|m| m.end_seconds <= local)
        .min(program.motions.len() - 1);
    let motion = &program.motions[index];
    let fraction = ((local - motion.start_seconds)
        / (motion.end_seconds - motion.start_seconds).max(1e-12))
    .clamp(0.0, 1.0);
    Some(Frame {
        motion_index: Some(index),
        kind: motion.kind,
        from: motion.from_mm,
        position: interpolate(motion.from_mm, motion.to_mm, fraction),
        ..base
    })
}

/// Already engraved part of a raster program as rectangles [x, y, w, h] in mm.
pub fn revealed_rows(program: &Program, frame: &Frame, bounds: [f64; 4]) -> Vec<[f64; 4]> {
    let Some(index) = frame.motion_index else {
        return Vec::new();
    };
    let [min_x, min_y, width, height] = bounds;
    let (max_x, max_y) = (min_x + width, min_y + height);
    let row = RASTER_ROW_MM;
    // Rows run top-down or bottom-up.
    let first_row = program
        .motions
        .iter()
        .find(|m| m.kind == MotionKind::Raster)
        .map_or(min_y, |m| m.from_mm[1]);
    let finished_rows = |y: f64, including: bool| {
        let edge = if including { row } else { 0.0 };
        if first_row <= y {
            [min_x, min_y, width, (y + edge - min_y).max(0.0)]
        } else {
            [
                min_x,
                y + row - edge,
                width,
                (max_y - y - row + edge).max(0.0),
            ]
        }
    };
    let motion = &program.motions[index];
    if motion.kind == MotionKind::Raster {
        let y = motion.from_mm[1];
        let x = frame.position[0].clamp(min_x, max_x.max(min_x));
        let partial = if motion.to_mm[0] >= motion.from_mm[0] {
            [min_x, y, x - min_x, row]
        } else {
            [x, y, max_x - x, row]
        };
        vec![finished_rows(y, false), partial]
    } else if let Some(previous) = program.motions[..index]
        .iter()
        .rev()
        .find(|m| m.kind == MotionKind::Raster)
    {
        vec![finished_rows(previous.to_mm[1], true)]
    } else {
        Vec::new()
    }
}

/// Play/pause state of the simulation; `now` is a monotonic time in seconds.
#[derive(Debug)]
pub struct Playback {
    pub seconds: f64,
    pub playing: bool,
    pub speed: f64,
    last_tick: Option<f64>,
}

impl Default for Playback {
    fn default() -> Self {
        Self {
            seconds: 0.0,
            playing: false,
            speed: 10.0,
            last_tick: None,
        }
    }
}

impl Playback {
    pub fn seek(&mut self, time: f64, duration: f64) {
        self.pause();
        self.seconds = time.max(0.0).min(duration.max(0.0));
    }

    pub fn toggle(&mut self, duration: f64, now: f64) {
        if self.playing {
            self.pause();
            return;
        }
        if duration <= 0.0 {
            return;
        }
        if self.seconds >= duration {
            self.seconds = 0.0;
        }
        self.playing = true;
        self.last_tick = Some(now);
    }

    pub fn pause(&mut self) {
        self.playing = false;
        self.last_tick = None;
    }

    pub fn tick(&mut self, now: f64, duration: f64) {
        if !self.playing {
            return;
        }
        let delta = (now - self.last_tick.unwrap_or(now)).max(0.0);
        self.last_tick = Some(now);
        self.seconds = (self.seconds + delta * self.speed).min(duration);
        if self.seconds >= duration {
            self.pause();
        }
    }
}

const WARNING: Color32 = Color32::from_rgb(196, 98, 16);

fn operation_color(operation: Operation) -> Color32 {
    match operation {
        Operation::Cut => Color32::from_rgb(220, 52, 52),
        Operation::Engrave => Color32::from_rgb(35, 113, 210),
        Operation::Engrave3d => Color32::from_rgb(222, 140, 30),
        Operation::Mark => Color32::from_rgb(142, 68, 210),
    }
}

fn kind_title(kind: MotionKind) -> &'static str {
    match kind {
        MotionKind::Travel => "Leerfahrt · Laser aus",
        MotionKind::Cut => "Schneiden",
        MotionKind::Mark => "Markieren",
        MotionKind::Raster => "Gravurzeile",
        MotionKind::Dwell => "Zeilenwechsel · Laser aus",
    }
}

/// Rounded-up duration such as "3 min 20 s".
pub fn duration(seconds: f64) -> String {
    let total = (seconds.ceil() as i64).max(1);
    if total >= 3600 {
        format!("{} h {} min", total / 3600, (total % 3600) / 60)
    } else if total >= 60 {
        format!("{} min {} s", total / 60, total % 60)
    } else {
        format!("{total} s")
    }
}

/// Clock time with tenths, e.g. "00:01:05.3".
pub fn clock(seconds: f64) -> String {
    let tenths = (seconds * 10.0).round().max(0.0) as u64;
    let total = tenths / 10;
    format!(
        "{:02}:{:02}:{:02}.{}",
        total / 3600,
        (total / 60) % 60,
        total % 60,
        tenths % 10
    )
}

/// Number with German decimal comma, without trailing ",0".
fn decimal(value: f64) -> String {
    let text = format!("{value:.1}");
    text.strip_suffix(".0").unwrap_or(&text).replace('.', ",")
}

pub fn bytes(count: usize) -> String {
    match count {
        0..1000 => format!("{count} Byte"),
        1000..1_000_000 => format!("{} kB", decimal(count as f64 / 1e3)),
        _ => format!("{} MB", decimal(count as f64 / 1e6)),
    }
}

fn full_uv() -> Rect {
    Rect::from_min_max(Pos2::ZERO, Pos2::new(1.0, 1.0))
}

fn dialog(
    ui: &mut egui::Ui,
    ready: &Ready,
    project: &Project,
    playback: &mut Playback,
    send_enabled: bool,
) -> Option<Action> {
    let job = &ready.job;
    let mut action = None;
    ui.horizontal(|ui| {
        ui.heading(format!("Ca. {}", duration(job.estimated_seconds)));
        ui.label("geschätzte Bearbeitungszeit");
    });
    ui.label(format!(
        "{} · {} Einzelaufträge ({}) · {}:{}",
        bytes(ready.bytes),
        job.jobs.len(),
        job.jobs
            .iter()
            .map(|j| j.name.as_str())
            .collect::<Vec<_>>()
            .join(" → "),
        project.hostname,
        project.port
    ));
    for warning in &job.warnings {
        egui::Frame::group(ui.style())
            .stroke(Stroke::new(1.0, WARNING))
            .show(ui, |ui| {
                ui.colored_label(WARNING, format!("⚠ {warning}"));
            });
    }
    ui.add_space(6.0);
    simulation(ui, ready, project, playback);
    ui.add_space(6.0);
    ui.label(egui::RichText::new("SCHRITTE").strong().small());
    egui::Grid::new("preview_steps")
        .striped(true)
        .num_columns(8)
        .show(ui, |ui| {
            for title in [
                "#",
                "Name",
                "Verfahren",
                "Leistung",
                "Tempo",
                "Durchgänge",
                "Parametersätze",
                "Zeit",
            ] {
                ui.strong(title);
            }
            ui.end_row();
            for (index, step) in job.steps.iter().enumerate() {
                ui.label(format!("{}", index + 1));
                ui.monospace(&step.name).on_hover_text(&step.description);
                ui.colored_label(
                    operation_color(step.operation),
                    operation_title(step.operation),
                );
                ui.label(format!("{} %", decimal(step.power_percent as f64)));
                ui.label(format!("{} %", decimal(step.speed_percent as f64)));
                ui.label(step.passes.to_string());
                ui.label(step.parameter_sets.to_string());
                ui.label(format!("ca. {}", duration(step.estimated_seconds)));
                ui.end_row();
            }
        });
    for (index, step) in job.steps.iter().enumerate() {
        ui.small(format!("{}. {}", index + 1, step.description));
    }
    ui.small(
        "Schätzung aus Fahrwegen, Tempo und Durchgängen; bei Gravur mit Zeilenrücklauf und \
         Overscan. Beschleunigung und Geräteeinstellungen können die tatsächliche Dauer verändern.",
    );
    ui.separator();
    ui.horizontal(|ui| {
        if ui.button("LTT exportieren …").clicked() {
            action = Some(Action::Export);
        }
        if ui
            .add_enabled(send_enabled, egui::Button::new("An Lasercutter senden …"))
            .clicked()
        {
            action = Some(Action::Send);
        }
    });
    action
}

fn simulation(ui: &mut egui::Ui, ready: &Ready, project: &Project, playback: &mut Playback) {
    let total = ready.job.estimated_seconds;
    playback.tick(ui.input(|i| i.time), total);
    let width = ui.available_width().max(320.0);
    let (rect, _) = ui.allocate_exact_size(
        Vec2::new(width, (width * 0.5).clamp(220.0, 460.0)),
        egui::Sense::hover(),
    );
    let frame = frame_at(&ready.job.timeline, playback.seconds);
    paint_simulation(
        &ui.painter_at(rect),
        rect,
        ready,
        project,
        frame.as_ref(),
        playback.seconds,
    );
    ui.horizontal(|ui| {
        let now = ui.input(|i| i.time);
        if ui.button("⏮").on_hover_text("Zum Anfang").clicked() {
            playback.seek(0.0, total);
        }
        let label = if playback.playing { "⏸" } else { "⏵" };
        if ui
            .add_enabled(total > 0.0, egui::Button::new(label))
            .on_hover_text("Simulation abspielen oder pausieren")
            .clicked()
        {
            playback.toggle(total, now);
        }
        if ui.button("⏭").on_hover_text("Zum Ende").clicked() {
            playback.seek(total, total);
        }
        egui::ComboBox::from_id_salt("preview_speed")
            .width(64.0)
            .selected_text(format!("{}×", playback.speed))
            .show_ui(ui, |ui| {
                for speed in SPEEDS {
                    ui.selectable_value(&mut playback.speed, speed, format!("{speed}×"));
                }
            })
            .response
            .on_hover_text("Abspielgeschwindigkeit");
        let mut seconds = playback.seconds;
        ui.spacing_mut().slider_width = ui.available_width().max(80.0);
        if ui
            .add_enabled(
                total > 0.0,
                egui::Slider::new(&mut seconds, 0.0..=total.max(0.001)).show_value(false),
            )
            .on_hover_text("Auftragszeit")
            .changed()
        {
            playback.seek(seconds, total);
        }
    });
    ui.horizontal(|ui| {
        ui.monospace(format!("{} / {}", clock(playback.seconds), clock(total)));
        if let Some(frame) = frame {
            ui.separator();
            if frame.finished {
                ui.label("Fertig");
            } else {
                ui.label(format!(
                    "{} · Durchgang {}",
                    kind_title(frame.kind),
                    frame.pass
                ));
            }
            ui.separator();
            ui.monospace(format!(
                "Schritt {} · X {} / Y {} mm",
                frame.program_index + 1,
                decimal(frame.position[0]),
                decimal(frame.position[1])
            ));
        }
    });
    ui.small(
        "Geplant: blass · Bearbeitet: kräftig · Gelber Punkt: Laserkopf · \
         Rot: Schneiden · Blau: Gravieren · Orange: 3D-Gravur · Violett: Markieren · \
         Grau gestrichelt: Leerfahrt",
    );
}

/// Maps bed millimetres into the simulation rectangle.
struct View {
    origin: Pos2,
    min: [f64; 2],
    scale: f64,
}

impl View {
    fn new(rect: Rect, viewport: [f64; 4]) -> Self {
        let (w, h) = (viewport[2] - viewport[0], viewport[3] - viewport[1]);
        let scale = (rect.width() as f64 / w).min(rect.height() as f64 / h);
        let offset = Vec2::new(
            ((rect.width() as f64 - w * scale) / 2.0) as f32,
            ((rect.height() as f64 - h * scale) / 2.0) as f32,
        );
        Self {
            origin: rect.min + offset,
            min: [viewport[0], viewport[1]],
            scale,
        }
    }

    fn pos(&self, p: [f64; 2]) -> Pos2 {
        self.origin
            + Vec2::new(
                ((p[0] - self.min[0]) * self.scale) as f32,
                ((p[1] - self.min[1]) * self.scale) as f32,
            )
    }

    fn rect(&self, [x, y, w, h]: [f64; 4]) -> Rect {
        Rect::from_two_pos(self.pos([x, y]), self.pos([x + w, y + h]))
    }
}

fn paint_simulation(
    painter: &egui::Painter,
    rect: Rect,
    ready: &Ready,
    project: &Project,
    frame: Option<&Frame>,
    seconds: f64,
) {
    let view = View::new(rect, viewport(project, ready.motion_bounds));
    painter.rect_filled(rect, 0.0, Color32::from_gray(232));
    let bed = view.rect([
        0.0,
        0.0,
        project.bed_width_mm as f64,
        project.bed_height_mm as f64,
    ]);
    painter.rect_filled(bed, 0.0, Color32::WHITE);
    painter.rect_stroke(
        bed,
        0.0,
        Stroke::new(1.0, Color32::from_gray(160)),
        egui::StrokeKind::Outside,
    );
    let artwork = view.rect([
        project.x_mm as f64,
        project.y_mm as f64,
        project.width_mm as f64,
        project.height_mm as f64,
    ]);
    // Planned processing, faint.
    if let Some(texture) = &ready.preview {
        painter.image(
            texture.id(),
            artwork,
            full_uv(),
            Color32::WHITE.gamma_multiply(0.22),
        );
    }
    let Some(frame) = frame else {
        return;
    };
    let timeline = &ready.job.timeline;
    let completed: Vec<usize> = timeline.runs[..frame.run_index]
        .iter()
        .map(|r| r.program_index)
        .collect();
    for (index, program) in timeline.programs.iter().enumerate() {
        let full = completed.contains(&index) || (frame.finished && index == frame.program_index);
        let active = index == frame.program_index && seconds > 0.0;
        if !full && !active {
            continue;
        }
        if let (Some(Some(texture)), Some(bounds)) =
            (ready.rasters.get(index), program.raster_bounds_mm)
        {
            let target = view.rect(bounds);
            if full {
                painter.image(texture.id(), target, full_uv(), Color32::WHITE);
            } else {
                for revealed in revealed_rows(program, frame, bounds) {
                    let clip = view.rect(revealed);
                    if clip.width() > 0.0 && clip.height() > 0.0 {
                        // At least one pixel high so single rows stay visible.
                        let clip =
                            clip.expand2(Vec2::new(0.0, (0.5 - clip.height() / 2.0).max(0.0)));
                        painter.with_clip_rect(clip).image(
                            texture.id(),
                            target,
                            full_uv(),
                            Color32::WHITE,
                        );
                    }
                }
            }
        }
        let limit = if full {
            program.motions.len()
        } else {
            frame.motion_index.unwrap_or(0)
        };
        let color = operation_color(program.operation);
        for chain in &ready.chains[index] {
            if chain.start >= limit {
                break;
            }
            let count = (limit - chain.start).min(chain.points.len() - 1);
            let points: Vec<Pos2> = chain.points[..=count]
                .iter()
                .map(|p| view.pos(*p))
                .collect();
            let stroke = match chain.kind {
                MotionKind::Cut | MotionKind::Mark => Stroke::new(1.5, color),
                _ => Stroke::new(0.5, Color32::from_gray(185)),
            };
            painter.add(egui::Shape::line(points, stroke));
        }
    }
    if !frame.finished {
        let line = [view.pos(frame.from), view.pos(frame.position)];
        match frame.kind {
            MotionKind::Travel => {
                painter.extend(egui::Shape::dashed_line(
                    &line,
                    Stroke::new(1.0, Color32::GRAY),
                    4.0,
                    3.0,
                ));
            }
            MotionKind::Cut | MotionKind::Mark => {
                let color = operation_color(timeline.programs[frame.program_index].operation);
                painter.line_segment(line, Stroke::new(2.0, color));
            }
            MotionKind::Raster | MotionKind::Dwell => {}
        }
    }
    let raw = view.pos(frame.position);
    let inner = rect.shrink(8.0);
    let marker = raw.clamp(inner.min, inner.max);
    painter.circle(
        marker,
        5.0,
        Color32::from_rgb(250, 204, 21),
        Stroke::new(1.5, Color32::from_black_alpha(204)),
    );
    if marker != raw {
        painter.text(
            Pos2::new(rect.center().x, rect.top() + 12.0),
            egui::Align2::CENTER_CENTER,
            "Anfahrt außerhalb des Ausschnitts",
            egui::FontId::proportional(11.0),
            Color32::GRAY,
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample() -> Timeline {
        let mut engrave = Program::new(Operation::Engrave);
        engrave.travel([10.0, 10.0]);
        engrave.line(MotionKind::Raster, [30.0, 10.0], 10.0);
        engrave.dwell(0.5);
        engrave.travel([30.0, 10.0 + RASTER_ROW_MM]);
        engrave.line(MotionKind::Raster, [10.0, 10.0 + RASTER_ROW_MM], 10.0);
        let mut cut = Program::new(Operation::Cut);
        cut.travel([40.0, 20.0]);
        cut.line(MotionKind::Cut, [50.0, 20.0], 10.0);
        cut.line(MotionKind::Cut, [50.0, 30.0], 10.0);
        cut.travel([60.0, 30.0]);
        cut.line(MotionKind::Cut, [70.0, 30.0], 10.0);
        let mut timeline = Timeline::default();
        timeline.append(engrave, 1);
        timeline.append(cut, 2);
        timeline
    }

    #[test]
    fn frame_interpolates_entry_travel_and_motions() {
        let timeline = sample();
        let start = frame_at(&timeline, 0.0).unwrap();
        assert_eq!(start.position, [0.0, 0.0]);
        assert_eq!(start.kind, MotionKind::Travel);
        assert_eq!(start.motion_index, None);
        assert!(!start.finished);
        let run = &timeline.runs[0];
        let half_entry = frame_at(&timeline, run.entry_end_seconds / 2.0).unwrap();
        assert!((half_entry.position[0] - 5.0).abs() < 1e-9);
        assert!((half_entry.position[1] - 5.0).abs() < 1e-9);
        // Middle of the first raster line (2 s long).
        let middle = frame_at(&timeline, run.entry_end_seconds + 1.0).unwrap();
        assert_eq!(middle.kind, MotionKind::Raster);
        assert_eq!(middle.motion_index, Some(0));
        assert!((middle.position[0] - 20.0).abs() < 1e-9);
        for (index, run) in timeline.runs.iter().enumerate() {
            let frame = frame_at(&timeline, run.entry_end_seconds).unwrap();
            assert_eq!(frame.run_index, index);
            assert_eq!(frame.pass, run.pass);
        }
        let end = frame_at(&timeline, timeline.duration_seconds + 5.0).unwrap();
        assert!(end.finished);
        assert_eq!(end.program_index, 1);
        assert_eq!(end.position, [70.0, 30.0]);
        assert!(frame_at(&Timeline::default(), 1.0).is_none());
    }

    #[test]
    fn raster_rows_are_revealed_progressively() {
        let timeline = sample();
        let program = &timeline.programs[0];
        let bounds = [10.0, 10.0, 20.0, 2.0 * RASTER_ROW_MM];
        let entry = frame_at(&timeline, timeline.runs[0].entry_end_seconds / 2.0).unwrap();
        assert!(revealed_rows(program, &entry, bounds).is_empty());
        let run = &timeline.runs[0];
        let first = frame_at(&timeline, run.entry_end_seconds + 1.0).unwrap();
        let rows = revealed_rows(program, &first, bounds);
        assert_eq!(rows[0][3], 0.0);
        assert!((rows[1][2] - 10.0).abs() < 1e-9);
        // During the line change the first row is complete.
        let dwell = frame_at(&timeline, run.entry_end_seconds + 2.2).unwrap();
        assert_eq!(dwell.kind, MotionKind::Dwell);
        let rows = revealed_rows(program, &dwell, bounds);
        assert!((rows[0][3] - RASTER_ROW_MM).abs() < 1e-9);
    }

    #[test]
    fn chains_join_connected_cuts_and_split_at_travel() {
        let timeline = sample();
        assert!(chains(&timeline.programs[0]).is_empty());
        let chains = chains(&timeline.programs[1]);
        assert_eq!(chains.len(), 3);
        assert_eq!(chains[0].points.len(), 3);
        assert_eq!(chains[1].kind, MotionKind::Travel);
        assert_eq!(chains[2].start, 3);
        let bounds = motion_bounds(&timeline).unwrap();
        assert_eq!(bounds, [10.0, 10.0, 70.0, 30.0]);
    }

    #[test]
    fn playback_plays_stops_at_end_and_restarts() {
        let mut playback = Playback::default();
        playback.seek(100.0, 50.0);
        assert_eq!(playback.seconds, 50.0);
        assert!(!playback.playing);
        playback.toggle(50.0, 0.0);
        assert!(playback.playing);
        assert_eq!(playback.seconds, 0.0);
        playback.speed = 5.0;
        playback.tick(2.0, 50.0);
        assert_eq!(playback.seconds, 10.0);
        playback.tick(100_000.0, 50.0);
        assert_eq!(playback.seconds, 50.0);
        assert!(!playback.playing);
        playback.toggle(50.0, 0.0);
        playback.seek(25.0, 50.0);
        assert!(!playback.playing);
        playback.toggle(0.0, 0.0);
        assert!(!playback.playing);
    }

    #[test]
    fn fingerprint_detects_project_changes() {
        let project = Project::default();
        let mut moved = project.clone();
        assert_eq!(fingerprint(&project), fingerprint(&moved));
        moved.x_mm += 1.0;
        assert_ne!(fingerprint(&project), fingerprint(&moved));
        let mut svg = project.clone();
        svg.svg.push_str("<svg/>");
        assert_ne!(fingerprint(&project), fingerprint(&svg));
    }

    #[test]
    fn formats_times_and_sizes() {
        assert_eq!(duration(0.2), "1 s");
        assert_eq!(duration(125.0), "2 min 5 s");
        assert_eq!(duration(3725.0), "1 h 2 min");
        assert_eq!(clock(65.34), "00:01:05.3");
        assert_eq!(bytes(512), "512 Byte");
        assert_eq!(bytes(12_345), "12,3 kB");
        assert_eq!(bytes(2_000_000), "2 MB");
        let project = Project::default();
        let v = viewport(&project, None);
        assert_eq!(v, [5.0, 7.0, 115.0, 73.0]);
    }

    #[test]
    fn prepared_demo_job_simulates_to_the_end() {
        let mut project = Project {
            svg: include_str!("../examples/demo.svg").into(),
            ..Project::default()
        };
        project.name = "Demo".into();
        let computed = compute(&project).unwrap();
        assert!(computed.preview.is_some());
        let timeline = &computed.job.timeline;
        let end = frame_at(timeline, computed.job.estimated_seconds).unwrap();
        assert!(end.finished);
        assert_eq!(end.program_index, timeline.programs.len() - 1);
        for run in &timeline.runs {
            let program = &timeline.programs[run.program_index];
            if let Some(motion) = program.motions.iter().find(|m| m.kind == MotionKind::Cut) {
                let t = run.entry_end_seconds + (motion.start_seconds + motion.end_seconds) / 2.0;
                let frame = frame_at(timeline, t).unwrap();
                let expected = interpolate(motion.from_mm, motion.to_mm, 0.5);
                assert!((frame.position[0] - expected[0]).abs() < 1e-6);
                assert!((frame.position[1] - expected[1]).abs() < 1e-6);
                assert_eq!(frame.kind, MotionKind::Cut);
            }
        }
    }
}
