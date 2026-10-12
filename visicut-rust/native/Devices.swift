import AppKit
import SwiftUI
import UniformTypeIdentifiers

struct CameraCalibration: Codable, Equatable {
    var reference_points: [[Double]]
    var view_points: [[Double]]
}

struct LaserDevice: Codable, Equatable {
    var name: String
    var description: String
    var hostname: String
    var port: Int
    var rotary_axis: Bool
    var job_sent_text: String
    var camera_url: String
    var camera_calibration: CameraCalibration?
    var machine: MachineSettings
}

/// Geräteeigene Werte (visicut_core::project::MachineSettings).
struct MachineSettings: Codable, Equatable {
    /// Zeilenversatz für 10 % … 100 % Geschwindigkeit in 1/4000 Zoll.
    var engrave_shift: [Double]
    var curve_acceleration_mm_s2: Double
    var pierce_delay_ms: Double

    static let libLaserCutCurveAcceleration = 2000.0
    static let firmwareCurveAcceleration = 981.0
    static let standard = MachineSettings(engrave_shift: [-2, -4, -7, -10, -13, -15, -17, -19, -21, -23],
                                          curve_acceleration_mm_s2: libLaserCutCurveAcceleration, pierce_delay_ms: 50)
    var experimentalCurves: Bool { abs(curve_acceleration_mm_s2 - Self.libLaserCutCurveAcceleration) > 1e-9 }
}
struct MachineResponse: Decodable { let machine: MachineSettings }

struct DeviceStore: Codable, Equatable {
    var format_version: Int
    var devices: [LaserDevice]
    var selected: Int
}

struct LabSetting: Decodable { let name: String; let url: String }
struct DeviceList: Decodable { let store: DeviceStore; let labs: [LabSetting]; let error: String? }
struct DeviceImport: Decodable { let store: DeviceStore; let imported: Int }
struct CameraPicture: Decodable { let png: [UInt8]; let width: Int; let height: Int }
struct CameraBackground: Decodable { let png: [UInt8] }

extension AppModel {
    var device: LaserDevice { devices.devices[devices.selected] }

    /// The selected device defines where jobs go and whether rotary jobs are possible.
    func applyDevice() {
        if project.hostname != device.hostname { project.hostname = device.hostname }
        if project.port != device.port { project.port = device.port }
        if !device.rotary_axis && project.rotary_axis { project.rotary_axis = false }
        if project.machine != device.machine { project.machine = device.machine }
    }

    /// Zeilenversatz-Testmuster des aktiven Lasercutters als neues Projekt.
    func openShiftPattern(power: Double) {
        guard canDiscard() else { return }
        do {
            let response: ProjectResponse = try RustCore.decode("shift_pattern", project: project,
                values: ["power_percent": power])
            accept(response, dirty: true)
            projectURL = nil
            status = tr("Zeilenversatz-Testmuster geöffnet: Material prüfen, senden, danach je Zeile den besten Block eintragen.")
        } catch { self.error = error.localizedDescription }
    }

    /// Tabelle nach dem Test; `best` enthält je Geschwindigkeit den Block 1 … 5.
    func applyShiftTest(_ machine: MachineSettings, best: [Int]) -> MachineSettings? {
        do {
            let response: MachineResponse = try RustCore.decode("apply_shift_test",
                values: ["machine": RustCore.json(machine), "best": best])
            return response.machine
        } catch { self.error = error.localizedDescription; return nil }
    }

    @discardableResult
    func saveDevices(_ store: DeviceStore) -> Bool {
        do {
            _ = try RustCore.call("save_devices", values: ["store": RustCore.json(store)])
            let old = device, new = store.devices[store.selected]
            let cameraChanged = old.camera_url != new.camera_url || old.camera_calibration != new.camera_calibration
            devices = store
            applyDevice()
            if cameraChanged { cameraImage = nil; if showCamera { refreshCamera() } }
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func selectDevice(_ index: Int) {
        guard index != devices.selected, devices.devices.indices.contains(index) else { return }
        var store = devices
        store.selected = index
        if saveDevices(store) { status = tr("Lasercutter: {name} ({host}:{port})", ["name": device.name, "host": device.hostname, "port": device.port]) }
    }

    /// Imports into the saved list and returns the added devices.
    func importDevices() -> [LaserDevice] {
        let panel = NSOpenPanel()
        panel.title = tr("Lasercutter importieren (VisiCut-Gerät, .vcsettings oder Export)")
        panel.allowedContentTypes = [.xml, .json, .zip, UTType(filenameExtension: "vcsettings") ?? .data,
                                     UTType(filenameExtension: "vcrdevices") ?? .json]
        guard panel.runModal() == .OK, let url = panel.url else { return [] }
        return mergeDevices("import_devices", values: ["path": url.path])
    }

    func downloadDevices(_ lab: LabSetting) -> [LaserDevice] {
        mergeDevices("download_devices", values: ["url": lab.url])
    }

    private func mergeDevices(_ action: String, values: [String: Any]) -> [LaserDevice] {
        do {
            var request = values
            request["store"] = try RustCore.json(devices)
            let result: DeviceImport = try RustCore.decode(action, values: request)
            devices = result.store
            status = tr("{count} LTT iLaser 4000 importiert", ["count": result.imported])
            return Array(result.store.devices.suffix(result.imported))
        } catch {
            self.error = error.localizedDescription
            return []
        }
    }

    func exportDevices(_ devices: [LaserDevice]) {
        let panel = NSSavePanel()
        panel.title = tr("Lasercutter exportieren")
        panel.allowedContentTypes = [UTType(filenameExtension: "vcrdevices") ?? .json]
        panel.nameFieldStringValue = "Lasercutter.vcrdevices"
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            _ = try RustCore.call("export_devices", path: url.path, values: ["devices": RustCore.json(devices)])
            status = tr("{count} Lasercutter exportiert", ["count": devices.count])
        } catch { self.error = error.localizedDescription }
    }

    func refreshCamera() {
        guard !cameraLoading else { return }
        cameraLoading = true
        status = tr("Kamerabild wird geladen …")
        let device = self.device, project = self.project
        DispatchQueue.global(qos: .userInitiated).async {
            let result = Result { () throws -> CameraBackground in
                try RustCore.decode("camera_background", project: project, values: ["device": RustCore.json(device)])
            }
            DispatchQueue.main.async {
                self.cameraLoading = false
                switch result {
                case .success(let background):
                    self.cameraImage = NSImage(data: Data(background.png))
                    self.status = tr("Kamerabild aktualisiert")
                case .failure(let error):
                    self.showCamera = false
                    self.error = tr("Kamerabild: {error}", ["error": error.localizedDescription])
                    self.status = tr("Kamerabild nicht verfügbar.")
                }
            }
        }
    }

    func toggleCamera() {
        showCamera.toggle()
        if showCamera && cameraImage == nil { refreshCamera() }
    }

    /// Opens VisiCut's calibration marks as a marking job on the whole bed.
    func openCalibrationPage(_ points: [[Double]]) {
        guard canDiscard() else { return }
        do {
            let response: ProjectResponse = try RustCore.decode("calibration_page", project: project,
                values: ["reference_points": points])
            accept(response, dirty: true)
            projectURL = nil
            status = tr("Kalibrierseite geöffnet: Material und Markier-Parameter prüfen, senden, danach Foto aufnehmen.")
        } catch { self.error = error.localizedDescription }
    }

    /// The device's checklist shown after a successful transfer (VisiCut's jobSentText).
    func showJobSentText(_ names: [String]) {
        let text = device.job_sent_text.replacingOccurrences(of: "$jobname", with: names.joined(separator: ", "))
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, !CommandLine.arguments.contains("--ui-test") else { return }
        let alert = NSAlert()
        alert.messageText = tr("An {name} übertragen", ["name": device.name])
        alert.informativeText = text
        alert.runModal()
    }
}

/// Default marker layout for a new calibration, as on the FAU device.
private let defaultReferencePoints: [[Double]] = [[200, 120], [800, 120], [800, 480], [200, 480]]

struct DeviceSettings: View {
    @ObservedObject var model: AppModel
    @State private var draft: DeviceStore
    @State private var editing: Int
    @State private var calibrating = false
    @State private var shiftTest = false

    init(model: AppModel) {
        self.model = model
        _draft = State(initialValue: model.devices)
        _editing = State(initialValue: model.devices.selected)
    }

    private var current: Binding<LaserDevice> {
        Binding(get: { draft.devices[min(editing, draft.devices.count - 1)] },
                set: { draft.devices[min(editing, draft.devices.count - 1)] = $0 })
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 0) {
                VStack(spacing: 0) {
                    List(selection: Binding(get: { editing }, set: { if let value = $0 { editing = value } })) {
                        ForEach(draft.devices.indices, id: \.self) { index in
                            VStack(alignment: .leading, spacing: 2) {
                                Text(draft.devices[index].name).lineLimit(1)
                                Text(index == draft.selected ? tr("Aktiv · {host}", ["host": draft.devices[index].hostname]) : draft.devices[index].hostname)
                                    .font(.caption).foregroundStyle(.secondary).lineLimit(1)
                            }.tag(index)
                        }
                    }
                    Divider()
                    HStack(spacing: 2) {
                        Button { duplicate() } label: { Image(systemName: "plus") }.help(tr("Kopie anlegen"))
                        Button { remove() } label: { Image(systemName: "minus") }.help(tr("Entfernen"))
                            .disabled(draft.devices.count < 2)
                        Spacer()
                    }.buttonStyle(.borderless).padding(6)
                }.frame(width: 210)
                Divider()
                Form {
                    Section("LTT iLaser 4000 · 1000 × 600 mm") {
                        TextField(tr("Name"), text: current.name)
                        TextField(tr("Beschreibung"), text: current.description)
                        TextField(tr("Hostname / IP"), text: current.hostname)
                        TextField(tr("Port"), value: current.port, format: .number.grouping(.never))
                        Toggle(tr("Rotationseinheit vorhanden"), isOn: current.rotary_axis)
                    }
                    Section(tr("Kamera")) {
                        TextField(tr("Kamera-URL"), text: current.camera_url, prompt: Text("http://…/visicam.jpg"))
                        LabeledContent(tr("Kalibrierung")) {
                            HStack {
                                Text(current.wrappedValue.camera_calibration.map { tr("{count} Punkte", ["count": $0.reference_points.count]) } ?? tr("Nicht kalibriert"))
                                    .foregroundStyle(.secondary)
                                Button(tr("Kalibrieren …")) { calibrating = true }
                                    .disabled(current.wrappedValue.camera_url.isEmpty)
                            }
                        }
                    }
                    Section(tr("Maschine")) {
                        MachineSettingsView(machine: current.machine)
                        LabeledContent(tr("Zeilenversatz")) {
                            Button(tr("Kalibrieren …")) { shiftTest = true }
                        }
                    }
                    Section(tr("Hinweis nach dem Senden")) {
                        TextEditor(text: current.job_sent_text).font(.callout).frame(height: 70)
                        Text(tr("Autofokus, Druckluft und Absaugung stellt der LTT-Treiber nicht ein; dieser Text erinnert nach dem Senden daran. $jobname wird durch die Auftragsnamen ersetzt."))
                            .font(.caption).foregroundStyle(.secondary)
                    }
                }.formStyle(.grouped)
            }
            Divider()
            HStack {
                Button(tr("Importieren …")) { adopt(model.importDevices()) }
                Menu(tr("Herunterladen")) {
                    ForEach(model.labs, id: \.url) { lab in
                        Button(lab.name) { adopt(model.downloadDevices(lab)) }
                    }
                }.fixedSize()
                Button(tr("Exportieren …")) { model.exportDevices(draft.devices) }
                Spacer()
                Button(tr("Als aktiven Lasercutter verwenden")) { draft.selected = editing }
                    .disabled(editing == draft.selected)
                Button(tr("Verwerfen")) { draft = model.devices; editing = min(editing, draft.devices.count - 1) }
                    .disabled(draft == model.devices)
                Button(tr("Sichern")) { model.saveDevices(draft) }
                    .keyboardShortcut(.defaultAction).disabled(draft == model.devices)
            }.padding(12)
        }
        .frame(minWidth: 760, minHeight: 520)
        .sheet(isPresented: $shiftTest) {
            ShiftCalibrationView(model: model, device: current.wrappedValue,
                                 canOpenPattern: draft == model.devices && editing == draft.selected) { machine in
                current.wrappedValue.machine = machine
            }
        }
        .sheet(isPresented: $calibrating) {
            CameraCalibrationView(model: model, device: current.wrappedValue) { calibration in
                current.wrappedValue.camera_calibration = calibration
            }
        }
    }

    /// Imports are already saved; unsaved edits in the draft are kept.
    private func adopt(_ imported: [LaserDevice]) {
        guard !imported.isEmpty else { return }
        draft.devices.append(contentsOf: imported)
        editing = draft.devices.count - 1
    }

    private func duplicate() {
        var copy = current.wrappedValue
        let base = copy.name
        var n = 2
        while draft.devices.contains(where: { $0.name == copy.name }) { copy.name = "\(base) (\(n))"; n += 1 }
        draft.devices.append(copy)
        editing = draft.devices.count - 1
    }

    private func remove() {
        guard draft.devices.count > 1 else { return }
        draft.devices.remove(at: editing)
        if draft.selected == editing { draft.selected = 0 } else if draft.selected > editing { draft.selected -= 1 }
        editing = min(editing, draft.devices.count - 1)
    }
}

/// Zeilenversatz-Tabelle, Kurvenbeschleunigung und Verweilzeit eines Geräts.
struct MachineSettingsView: View {
    @Binding var machine: MachineSettings

    var body: some View {
        LabeledContent(tr("Zeilenversatz (1/4000 Zoll, 8 = ein Pixel)")) {
            Grid(alignment: .trailing, horizontalSpacing: 6, verticalSpacing: 4) {
                ForEach(0..<2, id: \.self) { row in
                    GridRow {
                        ForEach(0..<5, id: \.self) { column in
                            let index = row * 5 + column
                            Text("\((index + 1) * 10) %").font(.caption).foregroundStyle(.secondary)
                            TextField("", value: $machine.engrave_shift[index], format: .number)
                                .frame(width: 44).multilineTextAlignment(.trailing)
                        }
                    }
                }
            }
        }
        LabeledContent(tr("Kurvenbeschleunigung")) {
            HStack {
                TextField("", value: $machine.curve_acceleration_mm_s2, format: .number.grouping(.never))
                    .frame(width: 64).multilineTextAlignment(.trailing)
                Text("mm/s²").foregroundStyle(.secondary)
                Button("LibLaserCut") { machine.curve_acceleration_mm_s2 = MachineSettings.libLaserCutCurveAcceleration }
                Button(tr("Firmware 0,1 g")) { machine.curve_acceleration_mm_s2 = MachineSettings.firmwareCurveAcceleration }
                    .help(tr("Rampe des Controllers bei verbundenen Kurven (experimentell)"))
            }
        }
        if machine.experimentalCurves {
            Text(tr("Experimentell: Kurven werden anders geplant als in VisiCut.")).font(.caption).foregroundStyle(.secondary)
        }
        LabeledContent(tr("Verweilzeit am Konturstart")) {
            HStack {
                TextField("", value: $machine.pierce_delay_ms, format: .number)
                    .frame(width: 64).multilineTextAlignment(.trailing)
                Text("ms").foregroundStyle(.secondary)
            }
        }
        HStack {
            Text(tr("pddelay der Gerätekonfiguration; nur für die Zeitschätzung.")).font(.caption).foregroundStyle(.secondary)
            Spacer()
            Button(tr("Standardwerte")) { machine = .standard }
        }
    }
}

/// Testmuster für den Zeilenversatz gravieren und je Geschwindigkeit den
/// besten Block in die Tabelle übernehmen.
struct ShiftCalibrationView: View {
    @ObservedObject var model: AppModel
    let device: LaserDevice
    let canOpenPattern: Bool
    let apply: (MachineSettings) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var power = 30.0
    @State private var best = Array(repeating: 3, count: 10)

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(tr("Zeilenversatz kalibrieren · {device}", ["device": device.name])).font(.title2).bold()
            Text(tr("1. Testmuster öffnen, senden und gravieren. Je Geschwindigkeit (10 % oben bis 100 % unten, Balken links = Tempo) entstehen fünf Blöcke mit −2 … +2 Pixeln Versatz; Block 3 unter dem Dreieck nutzt die aktuelle Tabelle."))
                .fixedSize(horizontal: false, vertical: true)
            HStack {
                TextField(tr("Leistung"), value: $power, format: .number).frame(width: 60)
                Text("%")
                Button(tr("Testmuster öffnen")) {
                    let value = power
                    dismiss()
                    DispatchQueue.main.async { model.openShiftPattern(power: value) }
                }
                .disabled(!canOpenPattern || !(1.0...100.0).contains(power))
                if !canOpenPattern {
                    Text(tr("Nur für den aktiven Lasercutter ohne ungesicherte Änderungen")).font(.caption).foregroundStyle(.secondary)
                }
            }
            Divider()
            Text(tr("2. Je Zeile den Block mit den geradesten Balkenkanten wählen. Liegt er am Rand, übernehmen und das Testmuster erneut gravieren."))
                .fixedSize(horizontal: false, vertical: true)
            Grid(alignment: .leading, verticalSpacing: 4) {
                ForEach(0..<10, id: \.self) { index in
                    GridRow {
                        Text("\((index + 1) * 10) %").monospacedDigit()
                        Picker("", selection: $best[index]) {
                            ForEach(1...5, id: \.self) { Text("\($0)").tag($0) }
                        }.pickerStyle(.segmented).labelsHidden().frame(width: 220)
                    }
                }
            }
            HStack {
                Spacer()
                Button(tr("Abbrechen")) { dismiss() }.keyboardShortcut(.cancelAction)
                Button(tr("In die Tabelle übernehmen")) {
                    if let machine = model.applyShiftTest(device.machine, best: best) {
                        apply(machine)
                        dismiss()
                    }
                }.keyboardShortcut(.defaultAction)
            }
        }
        .padding(20).frame(width: 520)
    }
}

/// Assigns camera pixels to bed positions. Drag the numbered markers onto the
/// burnt calibration crosses (or click to move the selected marker there).
struct CameraCalibrationView: View {
    @ObservedObject var model: AppModel
    let device: LaserDevice
    let apply: (CameraCalibration) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var calibration: CameraCalibration
    @State private var picture: NSImage?
    @State private var pictureSize = CGSize(width: 1, height: 1)
    @State private var loading = false
    @State private var selected = 0
    @State private var problem: String?

    init(model: AppModel, device: LaserDevice, apply: @escaping (CameraCalibration) -> Void) {
        self.model = model; self.device = device; self.apply = apply
        _calibration = State(initialValue: device.camera_calibration
            ?? CameraCalibration(reference_points: defaultReferencePoints, view_points: []))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(tr("Kamera kalibrieren · {device}", ["device": device.name])).font(.title2).bold()
            HStack(alignment: .top, spacing: 16) {
                GeometryReader { geometry in
                    let scale = min(geometry.size.width / pictureSize.width, geometry.size.height / pictureSize.height)
                    let origin = CGPoint(x: (geometry.size.width - pictureSize.width * scale) / 2,
                                         y: (geometry.size.height - pictureSize.height * scale) / 2)
                    ZStack(alignment: .topLeading) {
                        Color(nsColor: .underPageBackgroundColor)
                        if let picture {
                            Image(nsImage: picture).resizable()
                                .frame(width: pictureSize.width * scale, height: pictureSize.height * scale)
                                .offset(x: origin.x, y: origin.y)
                                .onTapGesture(coordinateSpace: .local) { location in
                                    move(selected, to: CGPoint(x: location.x / scale, y: location.y / scale))
                                }
                            ForEach(calibration.view_points.indices, id: \.self) { index in
                                let p = calibration.view_points[index]
                                Marker(number: index + 1, selected: index == selected)
                                    .position(x: origin.x + p[0] * scale, y: origin.y + p[1] * scale)
                                    .gesture(DragGesture(coordinateSpace: .named("picture")).onChanged { drag in
                                        selected = index
                                        move(index, to: CGPoint(x: (drag.location.x - origin.x) / scale,
                                                                y: (drag.location.y - origin.y) / scale))
                                    })
                            }
                        } else {
                            VStack(spacing: 8) {
                                if loading { ProgressView() }
                                Text(loading ? tr("Foto wird aufgenommen …") : tr("Kein Kamerabild")).foregroundStyle(.secondary)
                            }.frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                    }.coordinateSpace(name: "picture")
                }
                VStack(alignment: .leading, spacing: 8) {
                    Text(tr("Marker auf dem Bett (mm)")).font(.headline)
                    ScrollView {
                        VStack(spacing: 6) {
                            ForEach(calibration.reference_points.indices, id: \.self) { index in
                                HStack(spacing: 6) {
                                    Button("\(index + 1)") { selected = index }
                                        .buttonStyle(.bordered).tint(index == selected ? .accentColor : .secondary)
                                    TextField("X", value: $calibration.reference_points[index][0], format: .number)
                                        .textFieldStyle(.roundedBorder).frame(width: 64)
                                    TextField("Y", value: $calibration.reference_points[index][1], format: .number)
                                        .textFieldStyle(.roundedBorder).frame(width: 64)
                                }
                            }
                        }
                    }
                    HStack {
                        Button(tr("Punkt hinzufügen")) {
                            calibration.reference_points.append([500, 300])
                            calibration.view_points.append([pictureSize.width / 2, pictureSize.height / 2])
                            selected = calibration.reference_points.count - 1
                        }.disabled(picture == nil)
                        Button(tr("Entfernen")) {
                            calibration.reference_points.remove(at: selected)
                            if calibration.view_points.indices.contains(selected) { calibration.view_points.remove(at: selected) }
                            selected = max(0, selected - 1)
                        }.disabled(calibration.reference_points.count <= 2)
                    }.controlSize(.small)
                    Text(tr("1. Kalibrierseite als Projekt öffnen und auf Restmaterial markieren.\n2. Foto aufnehmen.\n3. Nummerierte Marker auf die Kreuze ziehen (Nummer = Anzahl Striche).\nMindestens 4 Punkte, nicht auf einer Linie."))
                        .font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                    if let problem { Text(problem).font(.caption).foregroundStyle(.orange) }
                }.frame(width: 240)
            }
            HStack {
                Button(tr("Foto aufnehmen"), systemImage: "camera") { capture() }.disabled(loading)
                Button(tr("Kalibrierseite als Projekt öffnen …")) {
                    let points = calibration.reference_points
                    dismiss()
                    DispatchQueue.main.async { model.openCalibrationPage(points) }
                }
                Spacer()
                Button(tr("Abbrechen")) { dismiss() }.keyboardShortcut(.cancelAction)
                Button(tr("Übernehmen")) { save() }.keyboardShortcut(.defaultAction)
                    .disabled(calibration.view_points.count != calibration.reference_points.count)
            }
        }
        .padding(20).frame(width: 1040, height: 680)
        .onAppear { capture() }
    }

    private func move(_ index: Int, to point: CGPoint) {
        guard calibration.view_points.indices.contains(index) else { return }
        calibration.view_points[index] = [min(max(0, point.x), pictureSize.width), min(max(0, point.y), pictureSize.height)]
    }

    private func capture() {
        loading = true
        problem = nil
        let device = self.device
        DispatchQueue.global(qos: .userInitiated).async {
            let result = Result { () throws -> CameraPicture in
                try RustCore.decode("camera_image", values: ["device": RustCore.json(device)])
            }
            DispatchQueue.main.async {
                loading = false
                switch result {
                case .success(let shot):
                    picture = NSImage(data: Data(shot.png))
                    pictureSize = CGSize(width: shot.width, height: shot.height)
                    // Place missing markers where an uncalibrated camera would see them.
                    while calibration.view_points.count < calibration.reference_points.count {
                        let r = calibration.reference_points[calibration.view_points.count]
                        calibration.view_points.append([r[0] / 1000 * pictureSize.width, r[1] / 600 * pictureSize.height])
                    }
                case .failure(let error):
                    problem = error.localizedDescription
                }
            }
        }
    }

    private func save() {
        do {
            _ = try RustCore.call("homography", values: ["calibration": RustCore.json(calibration)])
            apply(calibration)
            dismiss()
        } catch { problem = error.localizedDescription }
    }
}

private struct Marker: View {
    let number: Int
    let selected: Bool
    var body: some View {
        ZStack {
            Circle().stroke(selected ? Color.accentColor : .yellow, lineWidth: 2).frame(width: 26, height: 26)
            Path { p in p.move(to: CGPoint(x: 13, y: 0)); p.addLine(to: CGPoint(x: 13, y: 26))
                p.move(to: CGPoint(x: 0, y: 13)); p.addLine(to: CGPoint(x: 26, y: 13)) }
                .stroke(selected ? Color.accentColor : .yellow, lineWidth: 1).frame(width: 26, height: 26)
            Text("\(number)").font(.caption).bold().foregroundStyle(.white)
                .padding(3).background(Circle().fill(.black.opacity(0.6))).offset(x: 17, y: -17)
        }.contentShape(Rectangle())
    }
}
