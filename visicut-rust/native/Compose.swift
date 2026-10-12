import AppKit
import SwiftUI

/// Werkzeuge im Menü „Objekt“ (visicut_core::compose).
enum ComposeTool: String, Identifiable {
    case text, shape, sign, array, nest
    var id: String { rawValue }
    var title: String {
        switch self {
        case .text: return tr("Text einfügen")
        case .shape: return tr("Form einfügen")
        case .sign: return tr("Schild erstellen")
        case .array: return tr("Kopien im Raster")
        case .nest: return tr("Teile verschachteln (Nesting)")
        }
    }
}

struct FontList: Decodable { let fonts: [String] }

/// Stand vor einer Werkzeug- oder Löschaktion für „Widerrufen“.
struct UndoState {
    let project: Project
    let image: NSImage?
    let objects: [SVGObject]
}

extension AppModel {
    /// Führt ein Werkzeug im Rust-Kern aus und übernimmt das Ergebnis. Neue
    /// Schritte erhalten die Werte des Materialprofils.
    @discardableResult
    func compose(_ action: String, values: [String: Any]) -> Bool {
        do {
            let oldSteps = project.steps.count
            let oldObjects = objects.count
            // Nach einem Teilwechsel gehören Schritte und Objektnummern zu einem
            // anderen Teil; Dateien als Teile behalten ihre eigenen Zuordnungen.
            let switchesPart = ["append", "select_part", "remove_part", "duplicate_part"].contains(action)
            let before = UndoState(project: project, image: image, objects: objects)
            let response: ProjectResponse = try RustCore.decode(action, project: project, values: values)
            accept(response, dirty: true)
            remember(before)
            if !switchesPart && project.steps.count > oldSteps {
                for index in oldSteps..<project.steps.count where project.steps[index].objects.allSatisfy({ $0 >= oldObjects }) {
                    applyStepPreset(index)
                }
            }
            refreshMapping()
            status = tr("Motiv aktualisiert · {width} × {height} mm", ["width": Int(project.width_mm.rounded()), "height": Int(project.height_mm.rounded())])
            if let warnings = response.warnings, !warnings.isEmpty { error = warnings.joined(separator: "\n") }
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// Drehen (positive Winkel im Uhrzeigersinn) und Spiegeln um die Mitte.
    func transformArtwork(rotate: Double = 0, flipHorizontal: Bool = false, flipVertical: Bool = false) {
        guard project.hasArtwork, !busy else { return }
        compose("transform", values: ["rotate_deg": rotate, "flip_horizontal": flipHorizontal, "flip_vertical": flipVertical])
    }

    func duplicateArtwork() {
        guard project.hasArtwork, !busy else { return }
        compose("duplicate", values: [:])
    }

    /// Entfernt das Motiv; Material, Gerät und Einstellungen bleiben. Gibt es
    /// weitere Teile, entfällt nur das aktive Teil.
    func deleteArtwork() {
        guard project.hasArtwork, !busy else { return }
        if !project.parts.isEmpty {
            if compose("remove_part", values: [:]) { status = tr("Teil entfernt · ⌘Z stellt es wieder her") }
            return
        }
        remember(UndoState(project: project, image: image, objects: objects))
        project.svg = ""
        project.steps = []
        project.ignore_filters = []
        image = nil
        objects = []
        dirty = true
        refreshMapping()
        status = tr("Motiv entfernt · ⌘Z stellt es wieder her")
    }

    func deleteObject(_ id: Int) {
        guard !busy else { return }
        compose("remove_objects", values: ["objects": [id]])
    }

    /// Abgelegte Dateien: jede Grafik wird ein eigenes Teil neben den
    /// vorhandenen Teilen (ohne Motiv öffnet die erste), ein einzelnes
    /// Projekt wird geöffnet.
    func dropFiles(_ urls: [URL]) {
        guard !busy else { return }
        let isProject = { (url: URL) in ["vcr", "json"].contains(url.pathExtension.lowercased()) }
        var graphics = urls.filter { !isProject($0) }
        let skipped = urls.filter(isProject).map { tr("{file}: Projekte lassen sich nicht hinzufügen, nur öffnen", ["file": $0.lastPathComponent]) }
        guard !graphics.isEmpty else {
            if let first = urls.first { open(first) }
            return
        }
        let count = graphics.count
        if !project.hasArtwork {
            open(graphics.removeFirst())
            guard project.hasArtwork else { return }
        }
        if !graphics.isEmpty, compose("append", values: ["paths": graphics.map(\.path)]) {
            status = tr("{count} Datei(en) als Teile hinzugefügt · ⌘Z widerruft", ["count": count])
        }
        if !skipped.isEmpty {
            error = ([error].compactMap { $0 } + skipped).joined(separator: "\n")
        }
    }

    /// Merkt den Stand vor einer Aktion für ⌘Z (Fenster-UndoManager).
    func remember(_ state: UndoState) {
        undoState = state
        undoManager?.removeAllActions(withTarget: self)
        undoManager?.registerUndo(withTarget: self) { model in
            MainActor.assumeIsolated { model.undo() }
        }
    }

    func undo() {
        guard let state = undoState, !busy else { return }
        undoState = nil
        project = state.project
        image = state.image
        objects = state.objects
        dirty = true
        refreshMapping()
        status = tr("Letzte Änderung widerrufen")
    }

    /// Neue Elemente erscheinen rechts neben dem Motiv, sonst links oben.
    var composePosition: (Double, Double) {
        guard project.hasArtwork else { return (10, 10) }
        let right = project.x_mm + project.width_mm + 10
        return right < project.bed_width_mm - 20 ? (right, project.y_mm) : (project.x_mm, project.y_mm)
    }
}

private func operationPicker(_ selection: Binding<Operation>) -> some View {
    Picker(tr("Bearbeitung"), selection: selection) {
        Text(tr("Gravieren (schwarz)")).tag(Operation.engrave)
        Text(tr("Schneiden (rot)")).tag(Operation.cut)
        Text(tr("Markieren (blau)")).tag(Operation.mark)
    }
}

private struct MillimetreField: View {
    let title: String
    @Binding var value: Double
    var body: some View {
        LabeledContent(title) {
            HStack(spacing: 4) {
                TextField("", value: $value, format: .number).frame(width: 70).multilineTextAlignment(.trailing)
                Text("mm").foregroundStyle(.secondary)
            }
        }
    }
}

struct ComposeSheet: View {
    @ObservedObject var model: AppModel
    let tool: ComposeTool
    @Environment(\.dismiss) private var dismiss
    @State private var fonts: [String] = ["sans-serif", "serif", "monospace"]
    @State private var x = 10.0
    @State private var y = 10.0
    // Text
    @State private var text = tr("Text")
    @State private var font = "sans-serif"
    @State private var size = 10.0
    @State private var bold = false
    @State private var italic = false
    @State private var align = "Left"
    @State private var lineSpacing = 1.2
    @State private var operation = Operation.engrave
    // Form
    @State private var shapeKind = "Rectangle"
    @State private var width = 50.0
    @State private var height = 30.0
    @State private var corner = 0.0
    @State private var corners = 6
    @State private var diameter = 40.0
    @State private var inner = 50.0
    // Schild
    @State private var signText = tr("Schild")
    @State private var signWidth = 150.0
    @State private var signHeight = 50.0
    @State private var signCorner = 5.0
    @State private var holes = 2
    @State private var holeDiameter = 4.0
    @State private var holeInset = 8.0
    @State private var frameInset = 0.0
    @State private var signSize = 18.0
    @State private var signBold = true
    // Kopien
    @State private var columns = 2
    @State private var rows = 2
    @State private var gapX = 5.0
    @State private var gapY = 5.0
    // Nesting
    @State private var quantity = 1
    @State private var gap = 3.0
    @State private var rotate = true
    @State private var area = [5.0, 5.0, 990.0, 590.0]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(tool.title).font(.title2).bold()
            Form { content }.formStyle(.grouped)
            HStack {
                Spacer()
                Button(tr("Abbrechen")) { dismiss() }.keyboardShortcut(.cancelAction)
                Button(tool == .array || tool == .nest ? tr("Anwenden") : tr("Einfügen")) {
                    if model.compose(action, values: values) { dismiss() }
                }.keyboardShortcut(.defaultAction)
            }
        }
        .padding(20).frame(width: 480)
        .onAppear {
            (x, y) = model.composePosition
            if tool == .shape { operation = .cut }
            if let list: FontList = try? RustCore.decode("fonts") { fonts = list.fonts }
        }
    }

    @ViewBuilder private var content: some View {
        switch tool {
        case .text:
            Section {
                TextEditor(text: $text).font(.body).frame(height: 70)
                Picker(tr("Schrift"), selection: $font) { ForEach(fonts, id: \.self) { Text($0).tag($0) } }
                MillimetreField(title: tr("Schriftgröße"), value: $size)
                Toggle(tr("Fett"), isOn: $bold)
                Toggle(tr("Kursiv"), isOn: $italic)
                Picker(tr("Ausrichtung"), selection: $align) {
                    Text(tr("Links")).tag("Left"); Text(tr("Mitte")).tag("Center"); Text(tr("Rechts")).tag("Right")
                }.pickerStyle(.segmented)
                LabeledContent(tr("Zeilenabstand")) {
                    TextField("", value: $lineSpacing, format: .number).frame(width: 70).multilineTextAlignment(.trailing)
                }
                operationPicker($operation)
            } footer: {
                Text(tr("Der Text wird in Umrisse umgewandelt und ist danach nicht mehr editierbar.")).font(.caption).foregroundStyle(.secondary)
            }
            position
        case .shape:
            Section {
                Picker(tr("Form"), selection: $shapeKind) {
                    Text(tr("Rechteck")).tag("Rectangle"); Text(tr("Ellipse")).tag("Ellipse")
                    Text(tr("Vieleck")).tag("Polygon"); Text(tr("Stern")).tag("Star")
                }.pickerStyle(.segmented)
                if shapeKind == "Rectangle" || shapeKind == "Ellipse" {
                    MillimetreField(title: tr("Breite"), value: $width)
                    MillimetreField(title: tr("Höhe"), value: $height)
                    if shapeKind == "Rectangle" { MillimetreField(title: tr("Eckenradius"), value: $corner) }
                } else {
                    Stepper("\(shapeKind == "Star" ? tr("Zacken") : tr("Ecken")): \(corners)", value: $corners, in: 3...100)
                    MillimetreField(title: tr("Durchmesser"), value: $diameter)
                    if shapeKind == "Star" {
                        LabeledContent(tr("Innenradius")) {
                            HStack(spacing: 4) {
                                TextField("", value: $inner, format: .number).frame(width: 70).multilineTextAlignment(.trailing)
                                Text("%").foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                operationPicker($operation)
            }
            position
        case .sign:
            Section {
                TextEditor(text: $signText).font(.body).frame(height: 50)
                MillimetreField(title: tr("Breite"), value: $signWidth)
                MillimetreField(title: tr("Höhe"), value: $signHeight)
                MillimetreField(title: tr("Eckenradius"), value: $signCorner)
                Picker(tr("Löcher"), selection: $holes) { Text("0").tag(0); Text("2").tag(2); Text("4").tag(4) }.pickerStyle(.segmented)
                if holes > 0 {
                    MillimetreField(title: tr("Lochdurchmesser"), value: $holeDiameter)
                    MillimetreField(title: tr("Lochabstand zum Rand"), value: $holeInset)
                }
                MillimetreField(title: tr("Rahmen (markiert, 0 = ohne)"), value: $frameInset)
                Picker(tr("Schrift"), selection: $font) { ForEach(fonts, id: \.self) { Text($0).tag($0) } }
                MillimetreField(title: tr("Schriftgröße"), value: $signSize)
                Toggle(tr("Fett"), isOn: $signBold)
                operationPicker($operation)
            } footer: {
                Text(tr("Platte und Löcher werden geschnitten, der Text mittig gesetzt.")).font(.caption).foregroundStyle(.secondary)
            }
            position
        case .array:
            Section {
                Stepper("\(tr("Spalten")): \(columns)", value: $columns, in: 1...50)
                Stepper("\(tr("Zeilen")): \(rows)", value: $rows, in: 1...50)
                MillimetreField(title: tr("Abstand X"), value: $gapX)
                MillimetreField(title: tr("Abstand Y"), value: $gapY)
            } footer: {
                let w = Double(columns) * model.project.width_mm + Double(columns - 1) * gapX
                let h = Double(rows) * model.project.height_mm + Double(rows - 1) * gapY
                VStack(alignment: .leading, spacing: 2) {
                    Text(tr("{count} Exemplare · {width} × {height} mm; Zuordnungen gelten für alle Kopien.", ["count": columns * rows, "width": Int(w.rounded()), "height": Int(h.rounded())]))
                    if !model.project.parts.isEmpty { Text(tr("Wirkt nur auf das aktive Teil.")) }
                }.font(.caption).foregroundStyle(.secondary)
            }
        case .nest:
            Section {
                Stepper("\(tr("Anzahl je Teil")): \(quantity)", value: $quantity, in: 1...400)
                MillimetreField(title: tr("Abstand"), value: $gap)
                Toggle(tr("Drehen um 90° erlaubt"), isOn: $rotate)
            }
            Section {
                MillimetreField(title: "X", value: $area[0])
                MillimetreField(title: "Y", value: $area[1])
                MillimetreField(title: tr("Breite"), value: $area[2])
                MillimetreField(title: tr("Höhe"), value: $area[3])
                HStack {
                    Button(tr("Ganzes Arbeitsbett")) { area = [5, 5, 990, 590] }
                    Button(tr("Fläche des Motivs")) {
                        area = [model.project.x_mm, model.project.y_mm, model.project.width_mm, model.project.height_mm]
                    }
                }
            } header: { Text(tr("Fläche")) } footer: {
                VStack(alignment: .leading, spacing: 2) {
                    Text(tr("Teile sind die Objekte der obersten Ebene (bzw. einer Inkscape-Ebene); sie werden nach ihren Umrissrechtecken angeordnet, größte zuerst."))
                    if !model.project.parts.isEmpty { Text(tr("Wirkt nur auf das aktive Teil.")) }
                }.font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private var position: some View {
        Section(tr("Position")) {
            MillimetreField(title: tr("X (links)"), value: $x)
            MillimetreField(title: tr("Y (oben)"), value: $y)
        }
    }

    private var action: String {
        switch tool {
        case .text, .shape: return "add_objects"
        case .sign: return "sign"
        case .array: return "array"
        case .nest: return "nest"
        }
    }

    private var values: [String: Any] {
        switch tool {
        case .text:
            let shape: [String: Any] = ["kind": "Text", "text": text, "font": font, "size_mm": size, "bold": bold,
                                        "italic": italic, "align": align, "line_spacing": lineSpacing]
            return ["objects": [["shape": shape, "operation": operation.rawValue, "x_mm": x, "y_mm": y]]]
        case .shape:
            var shape: [String: Any] = ["kind": shapeKind]
            switch shapeKind {
            case "Rectangle": shape.merge(["width_mm": width, "height_mm": height, "corner_mm": corner]) { $1 }
            case "Ellipse": shape.merge(["width_mm": width, "height_mm": height]) { $1 }
            case "Polygon": shape.merge(["sides": corners, "diameter_mm": diameter]) { $1 }
            default: shape.merge(["points": corners, "diameter_mm": diameter, "inner_percent": inner]) { $1 }
            }
            return ["objects": [["shape": shape, "operation": operation.rawValue, "x_mm": x, "y_mm": y]]]
        case .sign:
            return ["sign": ["x_mm": x, "y_mm": y, "width_mm": signWidth, "height_mm": signHeight, "corner_mm": signCorner,
                             "holes": holes, "hole_diameter_mm": holeDiameter, "hole_inset_mm": holeInset,
                             "text": signText, "font": font, "size_mm": signSize, "bold": signBold,
                             "text_operation": operation.rawValue, "frame_inset_mm": frameInset] as [String: Any]]
        case .array:
            return ["columns": columns, "rows": rows, "gap_x_mm": gapX, "gap_y_mm": gapY]
        case .nest:
            return ["settings": ["area_mm": area, "gap_mm": gap, "quantity": quantity, "rotate": rotate] as [String: Any]]
        }
    }
}

/// Bezugspunkt wie in Grafikprogrammen: ein Rahmen, dessen acht Quadrate an
/// Ecken und Seitenmitten über die Rahmenlinien verbunden sind, und ein
/// freies Quadrat in der Mitte. Das gewählte Quadrat ist gefüllt.
struct ReferencePointPicker: View {
    @Binding var x: Double
    @Binding var y: Double
    private let size: CGFloat = 40
    private let box: CGFloat = 8
    var body: some View {
        ZStack(alignment: .topLeading) {
            // Rahmen durch die Mitten der äußeren Quadrate.
            Path { path in
                path.addRect(CGRect(x: box / 2, y: box / 2, width: size - box, height: size - box))
            }.stroke(Color.secondary, lineWidth: 1)
            ForEach(0..<9, id: \.self) { index in
                let fx = Double(index % 3) / 2, fy = Double(index / 3) / 2
                let selected = x == fx && y == fy
                Rectangle()
                    .fill(selected ? Color.primary : Color(nsColor: .controlBackgroundColor))
                    .overlay(Rectangle().stroke(Color.secondary, lineWidth: 1))
                    .frame(width: box, height: box)
                    .offset(x: fx * (size - box), y: fy * (size - box))
                    .contentShape(Rectangle().inset(by: -3))
                    .onTapGesture { x = fx; y = fy }
            }
        }
        .frame(width: size, height: size)
        .help(tr("Bezugspunkt für X/Y und Größenänderungen"))
        .accessibilityLabel(tr("Bezugspunkt"))
    }
}

/// Drehen um einen Winkel, 90°-Schritte und Spiegeln (kein Verzerren).
struct TransformControls: View {
    @ObservedObject var model: AppModel
    @State private var angle = 0.0
    var body: some View {
        LabeledContent(tr("Drehen")) {
            HStack(spacing: 6) {
                TextField(tr("Winkel"), value: $angle, format: .number).multilineTextAlignment(.trailing)
                    .textFieldStyle(.roundedBorder).frame(width: 60).labelsHidden()
                Text("°").foregroundStyle(.secondary)
                Button(tr("Anwenden")) { model.transformArtwork(rotate: angle); angle = 0 }
                    .disabled(angle == 0 || !model.project.hasArtwork)
                    .help(tr("Positive Winkel drehen im Uhrzeigersinn"))
            }
        }
        HStack {
            Button { model.transformArtwork(rotate: -90) } label: { Image(systemName: "rotate.left") }.help(tr("90° nach links drehen"))
            Button { model.transformArtwork(rotate: 90) } label: { Image(systemName: "rotate.right") }.help(tr("90° nach rechts drehen"))
            Button { model.transformArtwork(flipHorizontal: true) } label: { Image(systemName: "arrow.left.and.right") }.help(tr("Horizontal spiegeln"))
            Button { model.transformArtwork(flipVertical: true) } label: { Image(systemName: "arrow.up.and.down") }.help(tr("Vertikal spiegeln"))
            Spacer()
        }
        .disabled(!model.project.hasArtwork)
    }
}

/// Werkzeuge: Schild auf leerem Projekt, Text dazu, Kopien und Nesting.
@MainActor
func testComposeTools(_ model: AppModel) throws {
    let saved = model.project, savedObjects = model.objects, savedImage = model.image
    defer { model.project = saved; model.objects = savedObjects; model.image = savedImage; model.dirty = false }
    // Moduswechsel mit Neuzeichnen: entfernte Schritte dürfen keine Ansicht
    // mit veraltetem Index mehr abstürzen lassen.
    for mode in [AssignmentMode.rules, .whole, .objects, .rules, .objects, .whole] {
        model.setAssignmentMode(mode)
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))
    }
    let fonts: FontList = try RustCore.decode("fonts")
    guard fonts.fonts.first == "sans-serif" else { throw CoreError("Schriftliste fehlt") }
    let empty: ProjectResponse = try RustCore.decode("default")
    model.accept(empty, dirty: false)
    guard model.compose("sign", values: ["sign": ["text": "Werkstatt", "x_mm": 20.0, "y_mm": 20.0] as [String: Any]]),
          model.objects.count == 4, model.project.steps.count == 2, abs(model.project.width_mm - 150) < 0.01
    else { throw CoreError("Schild nicht erzeugt") }
    let text: [String: Any] = ["kind": "Text", "text": "Hallo", "font": "sans-serif", "size_mm": 8.0, "bold": false,
                               "italic": false, "align": "Left", "line_spacing": 1.2]
    guard model.compose("add_objects", values: ["objects": [["shape": text, "operation": "Mark", "x_mm": 200.0, "y_mm": 20.0]]]),
          model.objects.count == 5, model.project.steps.contains(where: { $0.operation == .mark && $0.objects == [4] })
    else { throw CoreError("Text nicht eingefügt") }
    guard model.compose("array", values: ["columns": 2, "rows": 1, "gap_x_mm": 5.0, "gap_y_mm": 5.0]),
          model.objects.count == 10 else { throw CoreError("Kopien fehlen") }
    let job: PreparedJob = try RustCore.decode("prepare", project: model.project)
    guard job.steps.contains(where: { $0.operation == .cut }), job.steps.contains(where: { $0.operation == .engrave })
    else { throw CoreError("Schild-Auftrag unvollständig") }
    // Drehen tauscht Breite und Höhe, Widerrufen stellt den Stand wieder her.
    let (w, h) = (model.project.width_mm, model.project.height_mm)
    model.transformArtwork(rotate: 90)
    guard abs(model.project.width_mm - h) < 0.01, abs(model.project.height_mm - w) < 0.01 else { throw CoreError("Drehen fehlgeschlagen") }
    model.undo()
    guard abs(model.project.width_mm - w) < 0.01, model.undoState == nil else { throw CoreError("Widerrufen fehlgeschlagen") }
    model.deleteObject(0)
    guard model.objects.count == 9 else { throw CoreError("Objekt nicht gelöscht") }
    model.undo()
    model.deleteArtwork()
    guard !model.project.hasArtwork, model.objects.isEmpty else { throw CoreError("Motiv nicht gelöscht") }
    model.undo()
    guard model.objects.count == 10 else { throw CoreError("Löschen nicht widerrufen") }
    guard model.compose("nest", values: ["settings": ["area_mm": [5.0, 5.0, 990.0, 590.0], "gap_mm": 3.0, "quantity": 1, "rotate": true] as [String: Any]]),
          model.objects.count == 10 else { throw CoreError("Nesting fehlgeschlagen") }
}
