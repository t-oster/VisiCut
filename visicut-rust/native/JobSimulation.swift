import AppKit
import SwiftUI
import Combine

enum MotionKind: String, Decodable {
    case travel = "Travel", cut = "Cut", mark = "Mark", raster = "Raster", dwell = "Dwell"
    var title: String {
        switch self {
        case .travel: return tr("Leerfahrt · Laser aus")
        case .cut: return tr("Schneiden")
        case .mark: return tr("Markieren")
        case .raster: return tr("Gravurzeile")
        case .dwell: return tr("Zeilenwechsel · Laser aus")
        }
    }
}

struct LaserMotion: Decodable {
    let kind: MotionKind
    let from_mm: [Double]
    let to_mm: [Double]
    let start_seconds: Double
    let end_seconds: Double
}

struct LaserProgram: Decodable {
    let operation: Operation
    let start_mm: [Double]
    let end_mm: [Double]
    let duration_seconds: Double
    let motions: [LaserMotion]
    let raster_preview_png: [UInt8]
    let raster_bounds_mm: [Double]?
}

struct LaserRun: Decodable {
    let program_index: Int
    let pass: Int
    let from_mm: [Double]
    let start_seconds: Double
    let entry_end_seconds: Double
    let end_seconds: Double
}

struct LaserFrame {
    let runIndex: Int
    let programIndex: Int
    let pass: Int
    let motionIndex: Int?
    let kind: MotionKind
    let from: CGPoint
    let position: CGPoint
    let fraction: Double
    let finished: Bool
}

struct LaserTimeline: Decodable {
    let programs: [LaserProgram]
    let runs: [LaserRun]
    let duration_seconds: Double

    func frame(at seconds: Double) -> LaserFrame? {
        guard !runs.isEmpty else { return nil }
        let time = min(max(0, seconds), duration_seconds)
        let runIndex = firstEnding(after: time, count: runs.count) { runs[$0].end_seconds }
        let run = runs[runIndex]
        let program = programs[run.program_index]
        if time < run.entry_end_seconds {
            let fraction = (time - run.start_seconds) / max(1e-12, run.entry_end_seconds - run.start_seconds)
            return LaserFrame(runIndex: runIndex, programIndex: run.program_index, pass: run.pass,
                motionIndex: nil, kind: .travel, from: point(run.from_mm),
                position: interpolate(run.from_mm, program.start_mm, fraction), fraction: fraction, finished: false)
        }
        let localTime = time - run.entry_end_seconds
        guard !program.motions.isEmpty else {
            return LaserFrame(runIndex: runIndex, programIndex: run.program_index, pass: run.pass,
                motionIndex: nil, kind: .travel, from: point(program.start_mm),
                position: point(program.end_mm), fraction: 1, finished: time >= duration_seconds)
        }
        let index = firstEnding(after: localTime, count: program.motions.count) { program.motions[$0].end_seconds }
        let motion = program.motions[index]
        let fraction = min(1, max(0, (localTime - motion.start_seconds) / max(1e-12, motion.end_seconds - motion.start_seconds)))
        return LaserFrame(runIndex: runIndex, programIndex: run.program_index, pass: run.pass,
            motionIndex: index, kind: motion.kind, from: point(motion.from_mm),
            position: interpolate(motion.from_mm, motion.to_mm, fraction), fraction: fraction,
            finished: time >= duration_seconds)
    }
}

private func firstEnding(after time: Double, count: Int, end: (Int) -> Double) -> Int {
    var low = 0, high = count
    while low < high {
        let mid = (low + high) / 2
        if end(mid) <= time { low = mid + 1 } else { high = mid }
    }
    return min(low, count - 1)
}

private func point(_ values: [Double]) -> CGPoint { CGPoint(x: values[0], y: values[1]) }
private func interpolate(_ a: [Double], _ b: [Double], _ fraction: Double) -> CGPoint {
    CGPoint(x: a[0] + (b[0] - a[0]) * fraction, y: a[1] + (b[1] - a[1]) * fraction)
}

func simulationTime(_ seconds: Double) -> String {
    let tenths = max(0, Int((seconds * 10).rounded()))
    let total = tenths / 10
    return String(format: "%02d:%02d:%02d.%d", total / 3600, (total / 60) % 60, total % 60, tenths % 10)
}

@MainActor
final class JobPlayback: ObservableObject {
    @Published var seconds = 0.0
    @Published var playing = false
    @Published var speed = 10.0
    private var lastTick: Date?

    func seek(_ time: Double, duration: Double) {
        pause()
        seconds = min(max(0, time), duration)
    }
    func toggle(duration: Double) {
        if playing { pause(); return }
        guard duration > 0 else { return }
        if seconds >= duration { seconds = 0 }
        playing = true
        lastTick = Date()
    }
    func pause() { playing = false; lastTick = nil }
    func tick(_ date: Date, duration: Double) {
        guard playing else { return }
        let delta = max(0, date.timeIntervalSince(lastTick ?? date))
        lastTick = date
        seconds = min(duration, seconds + delta * speed)
        if seconds >= duration { pause() }
    }
}

private struct CutChunk {
    let start: Int
    let end: Int
    let path: Path
}

/// Cache full paths in chunks so scrubbing only reconstructs one partial chunk.
private struct SimulationDrawing {
    var rasterImages: [Int: NSImage] = [:]
    var cuts: [Int: [CutChunk]] = [:]
    var motionBounds = CGRect.null

    init(timeline: LaserTimeline) {
        for (index, program) in timeline.programs.enumerated() {
            for motion in program.motions {
                for p in [motion.from_mm, motion.to_mm] {
                    motionBounds = motionBounds.union(CGRect(x: p[0], y: p[1], width: 0.001, height: 0.001))
                }
            }
            if !program.raster_preview_png.isEmpty {
                rasterImages[index] = NSImage(data: Data(program.raster_preview_png))
            }
            guard !program.operation.isRaster else { continue }
            cuts[index] = stride(from: 0, to: program.motions.count, by: 512).map { start in
                let end = min(start + 512, program.motions.count)
                var path = Path()
                for motion in program.motions[start..<end] where (motion.kind == .cut || motion.kind == .mark) {
                    path.move(to: point(motion.from_mm)); path.addLine(to: point(motion.to_mm))
                }
                return CutChunk(start: start, end: end, path: path)
            }
        }
    }
}

struct JobSimulation: View {
    let job: PreparedJob
    let project: Project
    let image: NSImage?
    @ObservedObject var playback: JobPlayback
    @State private var drawing: SimulationDrawing?
    private let timer = Timer.publish(every: 1.0 / 30, on: .main, in: .common).autoconnect()

    private var frame: LaserFrame? { job.timeline.frame(at: playback.seconds) }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SimulationCanvas(timeline: job.timeline, project: project, image: image,
                drawing: drawing, frame: frame, seconds: playback.seconds)
                .background(.white)
                .accessibilityLabel(tr("Chronologische Laservorschau"))
            HStack(spacing: 10) {
                Button { playback.seek(0, duration: job.estimated_seconds) } label: {
                    Image(systemName: "backward.end.fill")
                }.help(tr("Zum Anfang")).accessibilityLabel(tr("Zum Anfang"))
                Button { playback.toggle(duration: job.estimated_seconds) } label: {
                    Image(systemName: playback.playing ? "pause.fill" : "play.fill").frame(width: 16)
                }.help(tr("Simulation abspielen oder pausieren")).accessibilityLabel(playback.playing ? tr("Pause") : tr("Abspielen"))
                    .disabled(job.estimated_seconds <= 0)
                SimulationTimeSlider(seconds: Binding(get: { playback.seconds }, set: {
                    playback.seek($0, duration: job.estimated_seconds)
                }), duration: job.estimated_seconds)
                    .frame(minWidth: 100).frame(height: 20)
                Button { playback.seek(job.estimated_seconds, duration: job.estimated_seconds) } label: {
                    Image(systemName: "forward.end.fill")
                }.help(tr("Zum Ende")).accessibilityLabel(tr("Zum Ende"))
                Picker(tr("Tempo"), selection: $playback.speed) {
                    ForEach([1.0, 5, 10, 25, 100], id: \.self) { speed in
                        Text("\(Int(speed))×").tag(speed)
                    }
                }.labelsHidden().frame(width: 72).help(tr("Abspielgeschwindigkeit"))
            }.controlSize(.small)
            HStack {
                Text("\(simulationTime(playback.seconds)) / \(simulationTime(job.estimated_seconds))")
                    .monospacedDigit().accessibilityIdentifier("jobSimulationTime")
                Spacer()
                if let frame {
                    Text(frame.finished ? tr("Fertig") : tr("{motion} · Durchgang {pass}", ["motion": frame.kind.title, "pass": frame.pass]))
                        .lineLimit(1)
                }
            }.font(.caption)
            if let frame {
                Text(tr("Schritt {step} · X {x} / Y {y} mm", ["step": frame.programIndex + 1, "x": format(frame.position.x), "y": format(frame.position.y)]))
                    .font(.caption).foregroundStyle(.secondary).monospacedDigit()
            }
            Text(tr("Geplant: blass · Bearbeitet: kräftig · Gelber Punkt: Laserkopf"))
                .font(.caption).foregroundStyle(.secondary)
        }
        .onAppear { drawing = SimulationDrawing(timeline: job.timeline) }
        .onReceive(timer) { playback.tick($0, duration: job.estimated_seconds) }
        .onDisappear { playback.pause() }
    }
}

private struct SimulationCanvas: NSViewRepresentable {
    let timeline: LaserTimeline
    let project: Project
    let image: NSImage?
    let drawing: SimulationDrawing?
    let frame: LaserFrame?
    let seconds: Double

    func makeNSView(context: Context) -> SimulationView { SimulationView() }
    func updateNSView(_ view: SimulationView, context: Context) { view.canvas = self; view.needsDisplay = true }

    private var viewport: CGRect {
        var bounds = CGRect(x: project.x_mm, y: project.y_mm, width: project.width_mm, height: project.height_mm)
        if let drawing, !drawing.motionBounds.isNull { bounds = bounds.union(drawing.motionBounds) }
        return bounds.insetBy(dx: -max(2, bounds.width * 0.05), dy: -max(2, bounds.height * 0.05))
    }

    func draw(in context: CGContext, size: CGSize) {
        let viewport = viewport
        let scale = min(size.width / viewport.width, size.height / viewport.height)
        let offset = CGPoint(x: (size.width - viewport.width * scale) / 2 - viewport.minX * scale,
            y: (size.height - viewport.height * scale) / 2 - viewport.minY * scale)
        let transform = CGAffineTransform(a: scale, b: 0, c: 0, d: scale, tx: offset.x, ty: offset.y)
        let artwork = CGRect(x: project.x_mm, y: project.y_mm, width: project.width_mm, height: project.height_mm)
        func stroke(_ path: Path, _ color: Color, _ width: CGFloat, dash: [CGFloat] = []) {
            context.addPath(path.cgPath)
            context.setStrokeColor(NSColor(color).cgColor)
            context.setLineWidth(width)
            context.setLineDash(phase: 0, lengths: dash)
            context.strokePath()
        }
        if let image {
            image.draw(in: artwork.applying(transform), from: .zero, operation: .sourceOver, fraction: 0.18,
                respectFlipped: true, hints: nil)
        }
        guard let frame, let drawing else { return }
        let completed = Set(timeline.runs.prefix(frame.runIndex).map(\.program_index))
        for (index, program) in timeline.programs.enumerated() {
            let full = completed.contains(index) || (frame.finished && index == frame.programIndex)
            let active = index == frame.programIndex && seconds > 0
            guard full || active else { continue }
            if let raster = drawing.rasterImages[index], let b = program.raster_bounds_mm {
                let bounds = CGRect(x: b[0], y: b[1], width: b[2], height: b[3])
                context.saveGState()
                var visible = true
                if !full {
                    var revealed = Path()
                    if let mi = frame.motionIndex {
                        let row = 25.4 / 500
                        // Rows run top-down or bottom-up; lines left-to-right or alternating.
                        let firstRow = program.motions.first { $0.kind == .raster }?.from_mm[1] ?? bounds.minY
                        func finishedRows(through y: Double, including: Bool) -> CGRect {
                            let edge = including ? row : 0
                            return firstRow <= y
                                ? CGRect(x: bounds.minX, y: bounds.minY, width: bounds.width, height: max(0, y + edge - bounds.minY))
                                : CGRect(x: bounds.minX, y: y + row - edge, width: bounds.width, height: max(0, bounds.maxY - y - row + edge))
                        }
                        let motion = program.motions[mi]
                        if motion.kind == .raster {
                            revealed.addRect(finishedRows(through: motion.from_mm[1], including: false))
                            let x = min(max(frame.position.x, bounds.minX), bounds.maxX)
                            revealed.addRect(motion.to_mm[0] >= motion.from_mm[0]
                                ? CGRect(x: bounds.minX, y: motion.from_mm[1], width: x - bounds.minX, height: row)
                                : CGRect(x: x, y: motion.from_mm[1], width: bounds.maxX - x, height: row))
                        } else if let previous = program.motions[..<mi].last(where: { $0.kind == .raster }) {
                            revealed.addRect(finishedRows(through: previous.to_mm[1], including: true))
                        }
                    }
                    // An empty Core Graphics clip path leaves the clip unchanged,
                    // whereas nothing has been engraved yet.
                    visible = !revealed.isEmpty
                    context.addPath(revealed.applying(transform).cgPath)
                    context.clip()
                }
                if visible {
                    raster.draw(in: bounds.applying(transform), from: .zero, operation: .sourceOver, fraction: 1,
                        respectFlipped: true, hints: nil)
                }
                context.restoreGState()
            }
            if let chunks = drawing.cuts[index] {
                let limit = full ? program.motions.count : frame.motionIndex ?? 0
                for chunk in chunks {
                    if chunk.end <= limit {
                        stroke(chunk.path.applying(transform), program.operation.color, 1.5)
                    } else if chunk.start < limit {
                        var partial = Path()
                        for motion in program.motions[chunk.start..<limit] where (motion.kind == .cut || motion.kind == .mark) {
                            partial.move(to: point(motion.from_mm)); partial.addLine(to: point(motion.to_mm))
                        }
                        stroke(partial.applying(transform), program.operation.color, 1.5)
                        break
                    } else { break }
                }
            }
        }
        if !frame.finished {
            var current = Path(); current.move(to: frame.from.applying(transform)); current.addLine(to: frame.position.applying(transform))
            if frame.kind == .travel {
                stroke(current, .gray, 1, dash: [4, 3])
            } else if frame.kind == .cut || frame.kind == .mark {
                stroke(current, timeline.programs[frame.programIndex].operation.color, 2)
            }
        }
        let raw = frame.position.applying(transform)
        let marker = CGPoint(x: min(max(8, raw.x), size.width - 8), y: min(max(8, raw.y), size.height - 8))
        let circle = CGRect(x: marker.x - 5, y: marker.y - 5, width: 10, height: 10)
        context.setFillColor(NSColor.systemYellow.cgColor)
        context.fillEllipse(in: circle)
        stroke(Path(ellipseIn: circle), .black.opacity(0.8), 1.5)
        if raw != marker {
            let note = NSAttributedString(string: tr("Anfahrt außerhalb des Ausschnitts"), attributes: [
                .font: NSFont.preferredFont(forTextStyle: .caption2), .foregroundColor: NSColor.gray])
            let noteSize = note.size()
            note.draw(at: CGPoint(x: (size.width - noteSize.width) / 2, y: 12 - noteSize.height / 2))
        }
    }
}

/// SwiftUI's Canvas renders through Metal, which aborts on Macs without a usable
/// GPU such as the Intel CI virtual machines; Core Graphics draws everywhere.
private final class SimulationView: NSView {
    var canvas: SimulationCanvas?
    override var isFlipped: Bool { true }
    override func draw(_ dirtyRect: NSRect) {
        guard let canvas, let context = NSGraphicsContext.current?.cgContext else { return }
        context.clip(to: bounds)
        canvas.draw(in: context, size: bounds.size)
    }
}

struct SimulationTimeSlider: NSViewRepresentable {
    @Binding var seconds: Double
    let duration: Double
    func makeCoordinator() -> Coordinator { Coordinator(seconds: $seconds) }
    func makeNSView(context: Context) -> NSSlider {
        let slider = NSSlider(value: 0, minValue: 0, maxValue: max(0.001, duration),
            target: context.coordinator, action: #selector(Coordinator.changed(_:)))
        slider.isContinuous = true
        slider.setAccessibilityIdentifier("jobTimeSlider")
        slider.setAccessibilityLabel(tr("Auftragszeit"))
        return slider
    }
    func updateNSView(_ slider: NSSlider, context: Context) {
        context.coordinator.seconds = $seconds
        slider.maxValue = max(0.001, duration)
        slider.doubleValue = min(max(0, seconds), duration)
        slider.isEnabled = duration > 0
    }
    final class Coordinator: NSObject {
        var seconds: Binding<Double>
        init(seconds: Binding<Double>) { self.seconds = seconds }
        @objc func changed(_ sender: NSSlider) { seconds.wrappedValue = sender.doubleValue }
    }
}

@MainActor
func testJobSimulation(_ job: PreparedJob) throws {
    let timeline = job.timeline
    guard let first = timeline.frame(at: 0), let last = timeline.frame(at: job.estimated_seconds),
          first.position == .zero, !first.finished, last.finished,
          last.programIndex == timeline.programs.count - 1 else { throw CoreError("Simulationsgrenzen fehlerhaft") }
    for (runIndex, run) in timeline.runs.enumerated() {
        guard let frame = timeline.frame(at: run.entry_end_seconds),
              frame.runIndex == runIndex, frame.pass == run.pass else { throw CoreError("Durchgangswechsel fehlt") }
        let program = timeline.programs[run.program_index]
        if let motion = program.motions.first(where: { $0.kind == .cut || $0.kind == .mark || $0.kind == .raster }) {
            let t = run.entry_end_seconds + (motion.start_seconds + motion.end_seconds) / 2
            guard let middle = timeline.frame(at: t) else { throw CoreError("Simulationsposition fehlt") }
            let expected = interpolate(motion.from_mm, motion.to_mm, 0.5)
            guard abs(middle.position.x - expected.x) < 0.001,
                  abs(middle.position.y - expected.y) < 0.001,
                  middle.kind == motion.kind else { throw CoreError("Laserpfad nicht zeitlich interpoliert") }
        }
    }
    let playback = JobPlayback()
    playback.seek(job.estimated_seconds, duration: job.estimated_seconds)
    playback.toggle(duration: job.estimated_seconds)
    guard playback.seconds == 0, playback.playing else { throw CoreError("Neustart am Ende fehlt") }
    playback.tick(Date().addingTimeInterval(100_000), duration: job.estimated_seconds)
    guard playback.seconds == job.estimated_seconds, !playback.playing else { throw CoreError("Simulation stoppt nicht am Ende") }
    playback.seek(job.estimated_seconds / 2, duration: job.estimated_seconds)
    guard !playback.playing else { throw CoreError("Scrubbing pausiert nicht") }
    playback.seek(0, duration: job.estimated_seconds)
    guard playback.seconds == 0 else { throw CoreError("Rückwärtssprung fehlt") }
}
