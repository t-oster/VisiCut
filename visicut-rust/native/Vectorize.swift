import AppKit
import SwiftUI
import UniformTypeIdentifiers

/// Bild, das gerade im Sheet „Bitmap vektorisieren“ bearbeitet wird.
struct BitmapSource: Identifiable {
    let id = UUID()
    let url: URL
}

/// Antwort von `vectorize` ohne `apply`: nur Vorschau, Pfadanzahl und Größe.
struct VectorizePreview: Decodable {
    let paths: Int
    let width_mm: Double
    let height_mm: Double
    let width_px: Int
    let height_px: Int
    let preview: Preview
}

extension AppModel {
    func chooseBitmap() {
        guard !busy, !showJobPreview, vectorizeSource == nil else { return }
        let panel = NSOpenPanel()
        panel.title = "Bitmap zum Vektorisieren wählen"
        panel.allowedContentTypes = ["png", "jpg", "jpeg", "bmp", "gif"].compactMap { UTType(filenameExtension: $0) }
        panel.allowsMultipleSelection = false
        if panel.runModal() == .OK, let url = panel.url { vectorizeSource = BitmapSource(url: url) }
    }

    /// Ersetzt wie ein Import Motiv und Schritte. Liefert false, wenn ungesicherte
    /// Änderungen nicht verworfen werden sollen.
    func applyVectorized(_ url: URL, options: [String: Any]) throws -> Bool {
        guard canDiscard() else { return false }
        var values = options
        values["apply"] = true
        let response: ProjectResponse = try RustCore.decode("vectorize", project: project, path: url.path, values: values)
        accept(response, dirty: true)
        projectURL = nil
        status = "\(url.lastPathComponent) vektorisiert. Material und Bearbeitung auswählen."
        if let warnings = response.warnings, !warnings.isEmpty { self.error = warnings.joined(separator: "\n") }
        return true
    }
}

struct VectorizeSheet: View {
    @ObservedObject var model: AppModel
    let source: BitmapSource
    @Environment(\.dismiss) private var dismiss
    @State private var threshold = 128.0
    @State private var invert = false
    /// 0: Standardbreite des Kerns (72 DPI wie beim Bildimport).
    @State private var width_mm = 0.0
    @State private var result: VectorizePreview?
    @State private var vectors: NSImage?
    @State private var original: NSImage?
    @State private var loading = false
    @State private var pending = false
    @State private var problem: String?

    private var options: [String: Any] {
        var values: [String: Any] = ["threshold": Int(threshold.rounded()), "invert": invert]
        if width_mm > 0 { values["width_mm"] = width_mm }
        return values
    }
    private var ready: Bool { !loading && !pending && problem == nil && (result?.paths ?? 0) > 0 }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Bitmap vektorisieren").font(.title2).bold()
                Spacer()
                Text(source.url.lastPathComponent).foregroundStyle(.secondary).lineLimit(1)
            }
            HStack(alignment: .top, spacing: 20) {
                picture(original, title: "Bitmap")
                picture(vectors, title: "Vektorisiert")
                VStack(alignment: .leading, spacing: 12) {
                    LabeledContent("Schwellwert") {
                        HStack(spacing: 6) {
                            Slider(value: $threshold, in: 0...255, step: 1) { Text("Schwellwert") }
                                .labelsHidden().frame(width: 130)
                            Text(String(Int(threshold.rounded()))).monospacedDigit().frame(width: 32, alignment: .trailing)
                        }
                    }
                    Toggle("Invertieren (helle Bereiche nachzeichnen)", isOn: $invert)
                    NumberField("Breite", value: Binding(get: { width_mm > 0 ? width_mm : result?.width_mm ?? 0 },
                                                         set: { width_mm = $0 }), unit: "mm")
                    Divider()
                    status.font(.callout)
                    if let result {
                        Text("Bild: \(result.width_px) × \(result.height_px) Pixel").font(.caption).foregroundStyle(.secondary)
                    }
                    Text("Bildbereiche dunkler als der Schwellwert werden als geschlossene Konturen nachgezeichnet. Übernehmen ersetzt das Motiv und die Zuordnung wie ein Import.")
                        .font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }.frame(width: 300)
            }
            Divider()
            HStack {
                if loading { ProgressView().controlSize(.small) }
                Spacer()
                Button("Abbrechen") { dismiss() }.keyboardShortcut(.cancelAction)
                Button("Übernehmen") { apply() }.keyboardShortcut(.defaultAction).disabled(!ready)
            }
        }
        .padding(24).frame(width: 900, height: 440)
        .background(Color(nsColor: .windowBackgroundColor))
        .onAppear {
            original = NSImage(contentsOf: source.url)
            refresh()
        }
        .onChange(of: threshold) { refresh() }
        .onChange(of: invert) { refresh() }
        .onChange(of: width_mm) { refresh() }
    }

    @ViewBuilder private var status: some View {
        if let problem {
            Label(problem, systemImage: "exclamationmark.triangle").foregroundStyle(.orange)
        } else if let result {
            if result.paths > 0 {
                Text("\(result.paths) Pfade · \(format(result.width_mm)) × \(format(result.height_mm)) mm").monospacedDigit()
            } else {
                Text("Keine Kontur gefunden; Schwellwert anpassen").foregroundStyle(.orange)
            }
        } else {
            Text("Wird berechnet …").foregroundStyle(.secondary)
        }
    }

    private func picture(_ image: NSImage?, title: String) -> some View {
        VStack(spacing: 6) {
            ZStack {
                // Wie das Arbeitsbett: weißes Papier, damit schwarze Konturen auch im Dunkelmodus sichtbar sind.
                Color.white
                if let image {
                    Image(nsImage: image).resizable().interpolation(.high).aspectRatio(contentMode: .fit).padding(8)
                } else if loading {
                    ProgressView()
                }
            }
            .frame(width: 240, height: 300)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.secondary.opacity(0.3), lineWidth: 1))
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
    }

    /// Rechnet im Hintergrund. Änderungen während einer laufenden Berechnung
    /// werden danach einmal mit den dann aktuellen Optionen nachgeholt.
    private func refresh() {
        if loading { pending = true; return }
        loading = true
        pending = false
        let values = options
        let path = source.url.path
        DispatchQueue.global(qos: .userInitiated).async {
            let outcome = Result { try RustCore.decode("vectorize", path: path, values: values) as VectorizePreview }
            DispatchQueue.main.async {
                loading = false
                switch outcome {
                case .success(let preview):
                    result = preview
                    vectors = NSImage(data: Data(preview.preview.png))
                    problem = nil
                case .failure(let error):
                    result = nil
                    vectors = nil
                    problem = error.localizedDescription
                }
                if pending { refresh() }
            }
        }
    }

    private func apply() {
        do {
            if try model.applyVectorized(source.url, options: options) { dismiss() }
        } catch { problem = error.localizedDescription }
    }
}

/// Vektorisierung über die Brücke mit einer erzeugten Bitmap; das Projekt im
/// Modell bleibt unverändert.
@MainActor
func testVectorize(_ model: AppModel) throws {
    let file = FileManager.default.temporaryDirectory.appendingPathComponent("visicut-vectorize-\(UUID().uuidString).png")
    let pixels = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 40, pixelsHigh: 20, bitsPerSample: 8,
        samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    guard let bytes = pixels.bitmapData else { throw CoreError("Testbild ohne Pixel") }
    for y in 0..<20 {
        for x in 0..<40 {
            // Dunkles Quadrat auf weißem Grund.
            let value: UInt8 = (5..<15).contains(x) && (5..<15).contains(y) ? 0 : 255
            let offset = y * pixels.bytesPerRow + x * 4
            bytes[offset] = value; bytes[offset + 1] = value; bytes[offset + 2] = value; bytes[offset + 3] = 255
        }
    }
    try pixels.representation(using: .png, properties: [:])!.write(to: file)
    defer { try? FileManager.default.removeItem(at: file) }
    let preview: VectorizePreview = try RustCore.decode("vectorize", path: file.path, values: ["threshold": 128, "invert": false])
    guard preview.paths == 1, preview.width_px == 40, preview.height_px == 20,
          NSImage(data: Data(preview.preview.png)) != nil else { throw CoreError("Vektorisierungsvorschau falsch: \(preview.paths) Pfade") }
    let inverted: VectorizePreview = try RustCore.decode("vectorize", path: file.path, values: ["threshold": 128, "invert": true])
    guard inverted.paths == 2 else { throw CoreError("Invertierte Vektorisierung falsch: \(inverted.paths) Pfade") }
    let applied: ProjectResponse = try RustCore.decode("vectorize", project: model.project, path: file.path,
        values: ["threshold": 128, "invert": false, "width_mm": 80.0, "apply": true])
    guard applied.project.steps.isEmpty, abs(applied.project.width_mm - 80) < 0.01, abs(applied.project.height_mm - 40) < 0.01,
          applied.objects?.count == 1, applied.preview != nil, applied.project.cut_order == model.project.cut_order
    else { throw CoreError("Vektorisierte Bitmap nicht wie ein Import übernommen") }
}
