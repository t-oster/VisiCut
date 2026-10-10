import AppKit
import SwiftUI
import UniformTypeIdentifiers
import Combine

@_silgen_name("visicut_execute")
private func rustExecute(_ request: UnsafePointer<CChar>) -> UnsafeMutablePointer<CChar>?
@_silgen_name("visicut_free")
private func rustFree(_ response: UnsafeMutablePointer<CChar>)

enum RustCore {
    static func call(_ action: String, project: Project? = nil, path: String? = nil, values: [String: Any] = [:]) throws -> Data {
        var request = values
        request["action"] = action
        if let project {
            request["project"] = try JSONSerialization.jsonObject(with: JSONEncoder().encode(project))
        }
        if let path { request["path"] = path }
        let json = String(decoding: try JSONSerialization.data(withJSONObject: request), as: UTF8.self)
        let data: Data = try json.withCString { pointer in
            guard let response = rustExecute(pointer) else { throw CoreError("Rust hat keine Antwort geliefert") }
            defer { rustFree(response) }
            return Data(String(cString: response).utf8)
        }
        guard let response = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw CoreError("Ungültige Antwort des Rust-Kerns")
        }
        guard response["ok"] as? Bool == true else { throw CoreError(response["error"] as? String ?? "Verarbeitung fehlgeschlagen") }
        return try JSONSerialization.data(withJSONObject: response["result"] ?? [:])
    }

    static func decode<T: Decodable>(_ action: String, project: Project? = nil, path: String? = nil, values: [String: Any] = [:]) throws -> T {
        try JSONDecoder().decode(T.self, from: call(action, project: project, path: path, values: values))
    }

    static func json<T: Encodable>(_ value: T) throws -> Any {
        try JSONSerialization.jsonObject(with: JSONEncoder().encode(value), options: .fragmentsAllowed)
    }
}

struct CoreError: LocalizedError {
    let message: String
    init(_ message: String) { self.message = message }
    var errorDescription: String? { message }
}

enum Operation: String, Codable, CaseIterable, Identifiable {
    case cut = "Cut", engrave = "Engrave", engrave3d = "Engrave3d", mark = "Mark"
    var id: String { rawValue }
    var title: String {
        switch self { case .cut: return "Schneiden"; case .engrave: return "Gravieren"; case .engrave3d: return "3D-Gravur"; case .mark: return "Markieren" }
    }
    var symbol: String {
        switch self { case .cut: return "scissors"; case .engrave: return "square.stack.3d.up"; case .engrave3d: return "cube"; case .mark: return "pencil.tip" }
    }
    var color: Color {
        switch self { case .cut: return .red; case .engrave: return .blue; case .engrave3d: return .orange; case .mark: return .purple }
    }
    var isRaster: Bool { self == .engrave || self == .engrave3d }
}

struct Project: Codable, Equatable {
    var format_version: Int
    var name: String
    var svg: String
    var bed_width_mm: Double
    var bed_height_mm: Double
    var x_mm: Double
    var y_mm: Double
    var width_mm: Double
    var height_mm: Double
    var material: String
    var thickness_mm: Double
    var operation: Operation
    var power_percent: Double
    var speed_percent: Double
    var passes: Int
    var hostname: String
    var port: Int
    var steps: [JobStep]
    var rotary_axis: Bool
    var rotary_diameter_mm: Double
    var raster: RasterSettings
    var ignore_filters: [[Filter]]
    /// Rust serialisiert das Feld immer und setzt beim Laden älterer Dateien den Standard.
    var cut_order: CutOrder

    var hasArtwork: Bool { !svg.isEmpty }
    var fitsBed: Bool {
        x_mm.isFinite && y_mm.isFinite && width_mm.isFinite && height_mm.isFinite &&
        x_mm >= 0 && y_mm >= 0 && width_mm > 0 && height_mm > 0 &&
        x_mm + width_mm <= bed_width_mm + 0.001 && y_mm + height_mm <= bed_height_mm + 0.001
    }
}

/// Reihenfolge der Vektorpfade (visicut_core::project::CutOrder).
enum CutOrder: String, Codable, CaseIterable, Identifiable {
    case visiCut = "VisiCut", shortestTravel = "ShortestTravel"
    var id: String { rawValue }
    var title: String {
        switch self { case .visiCut: return "Wie VisiCut"; case .shortestTravel: return "Kürzeste Leerfahrten (experimentell)" }
    }
}

struct Material: Codable, Identifiable, Equatable {
    var id: String
    var name: String
    var profiles: [MaterialProfile]
    var thicknesses: [Double] { Array(Set(profiles.map(\.thickness_mm))).sorted() }
}
struct MaterialProfile: Codable, Equatable {
    var thickness_mm: Double
    var operation: Operation
    var power_percent: Double
    var speed_percent: Double
    var source: String?
}
struct SVGObject: Decodable, Identifiable { let id: Int; let label: String }
struct JobStep: Codable, Equatable {
    var operation: Operation
    var objects: [Int]
    var power_percent: Double
    var speed_percent: Double
    var passes: Int
    var filters: [Filter]?
    var rest: Bool
    var raster: RasterSettings
    var additional: [ParameterSet]
}
struct PreparedStep: Decodable {
    let name: String
    let operation: Operation
    let description: String
    let estimated_seconds: Double
    let power_percent: Double
    let speed_percent: Double
    let passes: Int
    let parameter_sets: Int
}
struct ProjectResponse: Decodable { let project: Project; let preview: Preview?; let objects: [SVGObject]?; let warnings: [String]? }
struct Preview: Decodable { let png: [UInt8] }
struct OutputJob: Decodable {
    let name: String
    let operation: Operation
    let bytes: [UInt8]
}
struct TransmissionResponse: Decodable { let sent: [String] }
struct PreparedJob: Decodable {
    let jobs: [OutputJob]
    var byteCount: Int { jobs.reduce(0) { $0 + $1.bytes.count } }
    let description: String
    let estimated_seconds: Double
    let preview_png: [UInt8]
    let steps: [PreparedStep]
    let timeline: LaserTimeline
    let warnings: [String]
}

func duration(_ seconds: Double) -> String {
    let total = max(1, Int(ceil(seconds)))
    if total >= 3600 { return "\(total / 3600) h \((total % 3600) / 60) min" }
    if total >= 60 { return "\(total / 60) min \(total % 60) s" }
    return "\(total) s"
}

@MainActor
final class AppModel: ObservableObject {
    @Published var project: Project { didSet {
        if !replacing { dirty = true }
        if oldValue.svg != project.svg || oldValue.steps != project.steps || oldValue.ignore_filters != project.ignore_filters {
            scheduleMappingRefresh()
        }
        if preparedJob != nil { status = "Auftrag geändert. Vorschau und Zeit neu berechnen." }
        preparedJob = nil
        jobImage = nil
        showJobPreview = false
        playback.seek(0, duration: 0)
    } }
    let playback = JobPlayback()
    @Published var objects: [SVGObject] = []
    @Published var preparedJob: PreparedJob?
    @Published var jobImage: NSImage?
    @Published var showJobPreview = false
    @Published var showProcessing = true
    @Published var image: NSImage?
    @Published var dirty = false
    @Published var busy = false
    @Published var status = "SVG importieren, um einen Job vorzubereiten."
    @Published var error: String?
    @Published var keepProportions = true
    @Published var zoom = 1.0
    @Published var showGrid = true
    @Published var devices: DeviceStore
    @Published var showCamera = false
    @Published var cameraImage: NSImage?
    @Published var cameraLoading = false
    let labs: [LabSetting]
    @Published var materials: [Material]
    @Published var materialsCustom = false
    @Published var mapping: MappingInfo?
    @Published var mappingError: String?
    @Published var vectorizeSource: BitmapSource?
    var materialSource = ""
    var mappingScheduled = false
    var projectURL: URL?
    private var replacing = true

    init() {
        do {
            let initial: ProjectResponse = try RustCore.decode("default")
            let list: DeviceList = try RustCore.decode("devices")
            devices = list.store
            labs = list.labs
            project = initial.project
            let catalog: MaterialCatalog = try RustCore.decode("materials")
            materials = catalog.materials
            materialSource = catalog.source
            materialsCustom = catalog.custom
            project.material = ""
            error = list.error.map { "Geräteliste nicht lesbar, FAU-Standard wird verwendet: " + $0 }
                ?? catalog.error.map { "Materialbibliothek nicht lesbar, FAU-Bibliothek wird verwendet: " + $0 }
        } catch {
            fatalError("Rust-Kern konnte nicht initialisiert werden: \(error)")
        }
        applyDevice()
        replacing = false
        if CommandLine.arguments.contains("--demo") || CommandLine.arguments.contains("--ui-test") {
            demo()
        } else if let path = CommandLine.arguments.dropFirst().first, !path.hasPrefix("-") {
            open(URL(fileURLWithPath: path))
        }
    }

    var selectedMaterial: Material? { materials.first { $0.name == project.material } }
    var materialSelection: String {
        project.material.isEmpty ? "" : selectedMaterial?.name ?? "__custom"
    }
    var preset: MaterialProfile? {
        selectedMaterial?.profiles.first { abs($0.thickness_mm - project.thickness_mm) < 0.0001 && $0.operation == project.operation && $0.power_percent > 0 && $0.speed_percent > 0 }
    }
    var sendEnabled: Bool { project.hasArtwork && project.fitsBed && !busy }

    func selectMaterial(_ selection: String) {
        if selection == "__custom" {
            project.material = "Eigenes Material"
        } else {
            project.material = selection
            if let material = selectedMaterial, !material.thicknesses.contains(project.thickness_mm),
               let first = material.thicknesses.first {
                project.thickness_mm = first
            }
        }
    }

    func applyPreset() {
        if !project.steps.isEmpty {
            for index in project.steps.indices { applyStepPreset(index) }
            status = "Verfügbare FAU-Profile für alle Verfahren übernommen."
            return
        }
        guard let preset else { return }
        project.power_percent = preset.power_percent
        project.speed_percent = preset.speed_percent
        project.passes = 1
        status = "FAU-Profil für \(project.material), \(format(project.thickness_mm)) mm, \(project.operation.title) übernommen."
    }

    func changeWidth(_ width: Double) {
        guard width.isFinite, width > 0 else { return }
        if keepProportions { project.height_mm *= width / project.width_mm }
        project.width_mm = width
    }
    func changeHeight(_ height: Double) {
        guard height.isFinite, height > 0 else { return }
        if keepProportions { project.width_mm *= height / project.height_mm }
        project.height_mm = height
    }
    func center() {
        project.x_mm = max(0, (project.bed_width_mm - project.width_mm) / 2)
        project.y_mm = max(0, (project.bed_height_mm - project.height_mm) / 2)
    }

    func accept(_ response: ProjectResponse, dirty: Bool) {
        replacing = true
        project = response.project
        if project.material == "Material wählen" { project.material = "" }
        applyDevice()
        image = response.preview.flatMap { NSImage(data: Data($0.png)) }
        objects = response.objects ?? []
        replacing = false
        self.dirty = dirty
    }

    func demo() {
        guard canDiscard() else { return }
        do {
            let response: ProjectResponse = try RustCore.decode("demo", project: project)
            accept(response, dirty: true)
            projectURL = nil
            status = "Beispiel geöffnet. Material und Bearbeitung auswählen."
        } catch { self.error = error.localizedDescription }
    }

    func newProject() {
        guard canDiscard() else { return }
        do {
            let response: ProjectResponse = try RustCore.decode("default")
            accept(response, dirty: false)
            projectURL = nil
            status = "Neues Projekt. SVG importieren, um zu beginnen."
        } catch { self.error = error.localizedDescription }
    }

    func chooseFile() {
        guard !busy else { return }
        let panel = NSOpenPanel()
        panel.title = "Grafik oder VisiCutRust-Projekt öffnen"
        let graphics = ["svg", "psvg", "dxf", "eps", "ps", "pdf", "png", "jpg", "jpeg", "bmp", "gif", "nc", "gcode", "plf", "ls"]
        panel.allowedContentTypes = (graphics + ["vcr"]).compactMap { UTType(filenameExtension: $0) } + [.json]
        panel.allowsMultipleSelection = false
        if panel.runModal() == .OK, let url = panel.url { open(url) }
    }

    func open(_ url: URL) {
        guard !busy, canDiscard() else { return }
        do {
            let isSVG = !["vcr", "json"].contains(url.pathExtension.lowercased())
            let response: ProjectResponse = try RustCore.decode(isSVG ? "import" : "load", project: project, path: url.path)
            accept(response, dirty: isSVG)
            projectURL = isSVG ? nil : url
            status = isSVG ? "\(url.lastPathComponent) importiert. Originalmaße übernommen." : "Projekt geöffnet."
            if let warnings = response.warnings, !warnings.isEmpty { self.error = warnings.joined(separator: "\n") }
        } catch { self.error = error.localizedDescription }
    }

    @discardableResult
    func save(asCopy: Bool = false) -> Bool {
        do {
            _ = try RustCore.call("validate_document", project: project)
            var url = asCopy ? nil : projectURL
            if url == nil {
                let panel = NSSavePanel()
                panel.title = "VisiCutRust-Projekt speichern"
                panel.allowedContentTypes = [UTType(filenameExtension: "vcr") ?? .json]
                panel.nameFieldStringValue = project.name + ".vcr"
                guard panel.runModal() == .OK else { return false }
                url = panel.url
            }
            guard let url else { return false }
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
            try encoder.encode(project).write(to: url, options: .atomic)
            projectURL = url
            dirty = false
            status = "Projekt gespeichert."
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func canDiscard() -> Bool {
        guard !busy else { return false }
        guard dirty else { return true }
        let alert = NSAlert()
        alert.messageText = "Änderungen an „\(project.name)“ sichern?"
        alert.informativeText = "Nicht gesicherte Änderungen gehen verloren."
        alert.addButton(withTitle: "Sichern")
        alert.addButton(withTitle: "Abbrechen")
        alert.addButton(withTitle: "Nicht sichern")
        switch alert.runModal() {
        case .alertFirstButtonReturn: return save()
        case .alertThirdButtonReturn: return true
        default: return false
        }
    }

    func export() {
        guard sendEnabled else { return }
        do {
            let prepared: PreparedJob = try preparedJob ?? RustCore.decode("prepare", project: project)
            guard let first = prepared.jobs.first else { return }
            if prepared.jobs.count == 1 {
                let panel = NSSavePanel()
                panel.title = "LTT-Auftrag exportieren"
                panel.allowedContentTypes = [UTType(filenameExtension: "ltt") ?? .data]
                panel.nameFieldStringValue = first.name + ".ltt"
                guard panel.runModal() == .OK, let url = panel.url else { return }
                try Data(first.bytes).write(to: url, options: .atomic)
            } else {
                let panel = NSOpenPanel()
                panel.title = "Ordner für \(prepared.jobs.count) getrennte LTT-Aufträge wählen"
                panel.canChooseDirectories = true
                panel.canChooseFiles = false
                panel.canCreateDirectories = true
                panel.prompt = "Exportieren"
                guard panel.runModal() == .OK, let folder = panel.url else { return }
                let existing = prepared.jobs.filter {
                    FileManager.default.fileExists(atPath: folder.appendingPathComponent($0.name + ".ltt").path)
                }
                if !existing.isEmpty {
                    let alert = NSAlert()
                    alert.messageText = "Vorhandene LTT-Dateien ersetzen?"
                    alert.informativeText = existing.map { $0.name + ".ltt" }.joined(separator: "\n")
                    alert.addButton(withTitle: "Ersetzen"); alert.addButton(withTitle: "Abbrechen")
                    guard alert.runModal() == .alertFirstButtonReturn else { return }
                }
                for job in prepared.jobs {
                    try Data(job.bytes).write(to: folder.appendingPathComponent(job.name + ".ltt"), options: .atomic)
                }
            }
            status = "Exportiert: " + prepared.jobs.map(\.name).joined(separator: ", ")
        } catch { self.error = error.localizedDescription }
    }

    func selectOperation(_ operation: Operation) {
        guard operation != project.operation else { return }
        project.operation = operation
        if operation == .mark {
            project.power_percent = 0
            project.speed_percent = 100
            project.passes = 1
            status = "Markieren: eigene Leistung einstellen oder ein passendes Profil übernehmen."
        }
    }

    func defaultStep(_ operation: Operation) -> JobStep {
        let preset = selectedMaterial?.profiles.first {
            abs($0.thickness_mm - project.thickness_mm) < 0.0001 && $0.operation == operation && $0.power_percent > 0
        }
        var step = JobStep(operation: operation, objects: [],
            power_percent: preset?.power_percent ?? (operation == .mark && project.operation != .mark ? 0 : project.power_percent),
            speed_percent: preset?.speed_percent ?? (operation == .mark && project.operation != .mark ? 100 : project.speed_percent),
            passes: project.passes)
        if operation.isRaster && project.operation.isRaster { step.raster = project.raster }
        return step
    }

    func setIndividual(_ enabled: Bool) {
        if !enabled { project.steps = []; return }
        project.steps = [Operation.engrave, .mark, .cut].map { operation in
            var step = defaultStep(operation)
            step.objects = operation == project.operation ? objects.map(\.id) : []
            return step
        }
    }

    func assignment(_ object: Int) -> String {
        project.steps.first { $0.objects.contains(object) }?.operation.rawValue ?? "Ignore"
    }

    func assign(_ object: Int, to operation: String) {
        var steps = project.steps
        if let target = Operation(rawValue: operation), !steps.contains(where: { $0.operation == target }) {
            steps.append(defaultStep(target))
        }
        for index in steps.indices {
            steps[index].objects.removeAll { $0 == object }
            if steps[index].operation.rawValue == operation { steps[index].objects.append(object) }
        }
        project.steps = steps
    }

    func applyStepPreset(_ index: Int) {
        guard let preset = selectedMaterial?.profiles.first(where: {
            abs($0.thickness_mm - project.thickness_mm) < 0.0001 && $0.operation == project.steps[index].operation && $0.power_percent > 0
        }) else { return }
        project.steps[index].power_percent = preset.power_percent
        project.steps[index].speed_percent = preset.speed_percent
        project.steps[index].passes = 1
    }

    func previewJob() {
        guard sendEnabled else { return }
        if preparedJob != nil { showJobPreview = true; return }
        busy = true
        status = "Auftragsvorschau und Bearbeitungszeit werden berechnet …"
        let snapshot = project
        DispatchQueue.global(qos: .userInitiated).async {
            let result = Result { try RustCore.decode("prepare", project: snapshot) as PreparedJob }
            DispatchQueue.main.async {
                self.busy = false
                guard self.project == snapshot else {
                    self.status = "Auftrag geändert. Vorschau bitte erneut berechnen."
                    return
                }
                switch result {
                case .success(let prepared):
                    self.playback.seek(0, duration: prepared.estimated_seconds)
                    self.preparedJob = prepared
                    self.jobImage = NSImage(data: Data(prepared.preview_png))
                    self.showJobPreview = true
                    self.status = "Geschätzte Bearbeitungszeit: ca. \(duration(prepared.estimated_seconds))"
                case .failure(let error):
                    self.error = error.localizedDescription
                    self.status = "Auftrag konnte nicht vorbereitet werden."
                }
            }
        }
    }

    func send() { previewJob() }

    func sendReviewedJob() {
        guard sendEnabled, let prepared = preparedJob else { return }
        let snapshot = project
        let alert = NSAlert()
        alert.messageText = "\(prepared.jobs.count) Auftrag/Aufträge an den Lasercutter senden?"
        alert.informativeText = "\(prepared.jobs.map(\.name).joined(separator: " → "))\nJe Verfahren ein separater Auftrag.\nCa. \(duration(prepared.estimated_seconds))\n\(device.name) · \(snapshot.hostname):\(snapshot.port)\n\n" + prepared.warnings.map { $0 + "\n\n" }.joined() + "Die Übertragung startet den Laser nicht. Aufträge am Gerät in dieser Reihenfolge einzeln starten. Treiber am Gerät noch nicht validiert. Vor dem Start Material, Fokus und Druckluft prüfen."
        alert.addButton(withTitle: "Senden")
        alert.addButton(withTitle: "Abbrechen")
        guard alert.runModal() == .alertFirstButtonReturn, snapshot == project else { return }
        showJobPreview = false
        busy = true
        status = "Job wird an den Lasercutter übertragen …"
        DispatchQueue.global(qos: .userInitiated).async {
            let result = Result { try RustCore.decode("transmit", project: snapshot) as TransmissionResponse }
            DispatchQueue.main.async {
                self.busy = false
                switch result {
                case .success(let response):
                    self.status = "Übertragen: " + response.sent.joined(separator: ", ") + ". Aufträge am Gerät prüfen und einzeln starten."
                    self.showJobSentText(response.sent)
                case .failure(let error): self.error = error.localizedDescription; self.status = "Übertragung fehlgeschlagen. Gerätestatus prüfen."
                }
            }
        }
    }

}

func format(_ value: Double) -> String {
    value.formatted(.number.precision(.fractionLength(0...2)))
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate, NSToolbarDelegate, NSMenuItemValidation, NSToolbarItemValidation {
    var model: AppModel!
    var workspaceWindow: NSWindow?
    var settingsWindow: NSWindow?
    var materialsWindow: NSWindow?
    private var changes: AnyCancellable?
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        model = AppModel()
        installMenus()
        showWorkspace()
        changes = model.objectWillChange.sink { [weak self] in
            DispatchQueue.main.async { self?.refreshWindow() }
        }
        NSApp.activate(ignoringOtherApps: true)
        if CommandLine.arguments.contains("--ui-test") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { runUITest(self.model) }
            DispatchQueue.main.asyncAfter(deadline: .now() + 30) {
                fputs("Native UI test timed out: \(NSApp.windows.count) windows\n", stderr)
                exit(1)
            }
        }
    }
    func applicationShouldTerminate(_ sender: NSApplication) -> NSApplication.TerminateReply {
        guard let model else { return .terminateNow }
        return model.canDiscard() ? .terminateNow : .terminateCancel
    }
    func application(_ application: NSApplication, open urls: [URL]) {
        if let url = urls.first { model?.open(url) }
    }
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        showWorkspace(); return true
    }

    func showWorkspace() {
        if let window = workspaceWindow { window.makeKeyAndOrderFront(nil); return }
        let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 1240, height: 820),
            styleMask: [.titled, .closable, .miniaturizable, .resizable, .fullSizeContentView], backing: .buffered, defer: false)
        window.contentView = NSHostingView(rootView: Workspace(model: model))
        window.minSize = NSSize(width: 960, height: 700)
        window.isReleasedWhenClosed = false
        window.delegate = self
        window.toolbarStyle = .unified
        let toolbar = NSToolbar(identifier: "VisiCutToolbar")
        toolbar.delegate = self
        toolbar.displayMode = .iconOnly
        toolbar.allowsUserCustomization = true
        window.toolbar = toolbar
        workspaceWindow = window
        window.center()
        window.makeKeyAndOrderFront(nil)
        refreshWindow()
    }

    func refreshWindow() {
        workspaceWindow?.title = model.project.name + " — VisiCutRust"
        workspaceWindow?.isDocumentEdited = model.dirty
        workspaceWindow?.representedURL = model.projectURL
        workspaceWindow?.toolbar?.validateVisibleItems()
    }

    func windowShouldClose(_ sender: NSWindow) -> Bool {
        sender !== workspaceWindow || model.canDiscard()
    }
    func windowWillClose(_ notification: Notification) {
        if let window = notification.object as? NSWindow, window === workspaceWindow { workspaceWindow = nil }
    }

    private func menuItem(_ title: String, action: Selector?, key: String = "", modifiers: NSEvent.ModifierFlags = .command, target: AnyObject? = nil) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: action, keyEquivalent: key)
        item.keyEquivalentModifierMask = modifiers
        item.target = target ?? self
        return item
    }
    private func installMenus() {
        let bar = NSMenu()
        func submenu(_ title: String) -> NSMenu {
            let item = NSMenuItem(title: title, action: nil, keyEquivalent: "")
            let menu = NSMenu(title: title); item.submenu = menu; bar.addItem(item); return menu
        }
        let app = submenu("VisiCutRust")
        app.addItem(menuItem("Über VisiCutRust", action: #selector(NSApplication.orderFrontStandardAboutPanel(_:)), target: NSApp))
        app.addItem(.separator())
        app.addItem(menuItem("Lasercutter …", action: #selector(showSettings), key: ","))
        app.addItem(.separator())
        let services = NSMenuItem(title: "Dienste", action: nil, keyEquivalent: "")
        services.submenu = NSMenu(title: "Dienste"); app.addItem(services); NSApp.servicesMenu = services.submenu
        app.addItem(.separator())
        app.addItem(menuItem("VisiCutRust ausblenden", action: #selector(NSApplication.hide(_:)), key: "h", target: NSApp))
        app.addItem(menuItem("Andere ausblenden", action: #selector(NSApplication.hideOtherApplications(_:)), key: "h", modifiers: [.command, .option], target: NSApp))
        app.addItem(menuItem("Alle einblenden", action: #selector(NSApplication.unhideAllApplications(_:)), target: NSApp))
        app.addItem(.separator())
        app.addItem(menuItem("VisiCutRust beenden", action: #selector(NSApplication.terminate(_:)), key: "q", target: NSApp))
        let file = submenu("Ablage")
        file.addItem(menuItem("Neues Projekt", action: #selector(newProject), key: "n"))
        file.addItem(menuItem("Öffnen …", action: #selector(openFile), key: "o"))
        file.addItem(menuItem("Beispiel öffnen", action: #selector(openDemo)))
        file.addItem(menuItem("Bitmap vektorisieren …", action: #selector(vectorizeBitmap)))
        file.addItem(.separator())
        file.addItem(menuItem("Sichern", action: #selector(saveProject), key: "s"))
        file.addItem(menuItem("Sichern unter …", action: #selector(saveAs), key: "s", modifiers: [.command, .shift]))
        file.addItem(menuItem("LTT-Job exportieren …", action: #selector(exportJob), key: "e", modifiers: [.command, .shift]))
        file.addItem(.separator())
        let close = menuItem("Fenster schließen", action: #selector(NSWindow.performClose(_:)), key: "w"); close.target = nil; file.addItem(close)
        let edit = submenu("Bearbeiten")
        for (title, selector, key) in [("Widerrufen", "undo:", "z"), ("Ausschneiden", "cut:", "x"), ("Kopieren", "copy:", "c"), ("Einsetzen", "paste:", "v"), ("Alles auswählen", "selectAll:", "a")] {
            let item = menuItem(title, action: Selector(selector), key: key); item.target = nil; edit.addItem(item)
        }
        let view = submenu("Darstellung")
        view.addItem(menuItem("Ansicht einpassen", action: #selector(fitView), key: "0"))
        view.addItem(menuItem("Raster anzeigen", action: #selector(toggleGrid)))
        view.addItem(.separator())
        view.addItem(menuItem("Kamerabild anzeigen", action: #selector(toggleCamera), key: "k"))
        view.addItem(menuItem("Kamerabild aktualisieren", action: #selector(refreshCamera), key: "k", modifiers: [.command, .shift]))
        let job = submenu("Job")
        job.addItem(menuItem("Auf dem Arbeitsbett zentrieren", action: #selector(centerArtwork)))
        job.addItem(menuItem("FAU-Materialprofil übernehmen", action: #selector(applyProfile)))
        job.addItem(menuItem("Materialbibliothek …", action: #selector(showMaterials), key: "m", modifiers: [.command, .shift]))
        job.addItem(menuItem("Auftragsvorschau und Zeit …", action: #selector(previewJob), key: "p", modifiers: [.command, .shift]))
        job.addItem(.separator())
        job.addItem(menuItem("An Lasercutter senden …", action: #selector(sendJob)))
        let windows = submenu("Fenster")
        windows.addItem(menuItem("Arbeitsbereich anzeigen", action: #selector(showMainWindow)))
        let minimize = menuItem("Im Dock ablegen", action: #selector(NSWindow.performMiniaturize(_:)), key: "m"); minimize.target = nil; windows.addItem(minimize)
        NSApp.windowsMenu = windows
        NSApp.mainMenu = bar
    }

    func validateMenuItem(_ item: NSMenuItem) -> Bool {
        switch item.action {
        case #selector(exportJob), #selector(sendJob), #selector(previewJob): return model.sendEnabled
        case #selector(applyProfile): return !model.busy && (model.preset != nil || model.project.steps.contains { step in
            model.selectedMaterial?.profiles.contains {
                abs($0.thickness_mm - model.project.thickness_mm) < 0.0001 && $0.operation == step.operation && $0.power_percent > 0
            } ?? false
        })
        case #selector(centerArtwork): return model.project.hasArtwork && !model.busy
        case #selector(toggleGrid): item.state = model.showGrid ? .on : .off; return true
        case #selector(toggleCamera): item.state = model.showCamera ? .on : .off; return !model.device.camera_url.isEmpty
        case #selector(refreshCamera): return model.showCamera && !model.cameraLoading
        case #selector(openFile), #selector(openDemo), #selector(newProject), #selector(showSettings): return !model.busy
        case #selector(vectorizeBitmap): return !model.busy && !model.showJobPreview && model.vectorizeSource == nil
        default: return true
        }
    }
    func validateToolbarItem(_ item: NSToolbarItem) -> Bool {
        if ["send", "export", "preview"].contains(item.itemIdentifier.rawValue) { return model.sendEnabled }
        return !model.busy
    }
    @objc func newProject() { showWorkspace(); model.newProject() }
    @objc func openFile() { showWorkspace(); model.chooseFile() }
    @objc func openDemo() { showWorkspace(); model.demo() }
    @objc func vectorizeBitmap() { showWorkspace(); model.chooseBitmap() }
    @objc func saveProject() { model.save() }
    @objc func saveAs() { model.save(asCopy: true) }
    @objc func exportJob() { model.export() }
    @objc func sendJob() { model.send() }
    @objc func previewJob() { model.previewJob() }
    @objc func applyProfile() { model.applyPreset() }
    @objc func centerArtwork() { model.center() }
    @objc func fitView() { model.zoom = 1 }
    @objc func toggleGrid() { model.showGrid.toggle() }
    @objc func toggleCamera() { model.toggleCamera() }
    @objc func refreshCamera() { model.refreshCamera() }
    @objc func showMainWindow() { showWorkspace() }
    @objc func showMaterials() {
        if let window = materialsWindow { window.makeKeyAndOrderFront(nil); return }
        let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 860, height: 560), styleMask: [.titled, .closable, .resizable], backing: .buffered, defer: false)
        window.contentView = NSHostingView(rootView: MaterialEditor(model: model))
        window.title = "Materialbibliothek"
        window.isReleasedWhenClosed = false
        materialsWindow = window
        window.center(); window.makeKeyAndOrderFront(nil)
    }
    @objc func showSettings() {
        if let window = settingsWindow { window.makeKeyAndOrderFront(nil); return }
        let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 820, height: 560), styleMask: [.titled, .closable, .resizable], backing: .buffered, defer: false)
        window.contentView = NSHostingView(rootView: DeviceSettings(model: model))
        window.title = "Lasercutter"
        window.isReleasedWhenClosed = false
        settingsWindow = window
        window.center(); window.makeKeyAndOrderFront(nil)
    }
    func toolbarAllowedItemIdentifiers(_ toolbar: NSToolbar) -> [NSToolbarItem.Identifier] { toolbarDefaultItemIdentifiers(toolbar) + [.space] }
    func toolbarDefaultItemIdentifiers(_ toolbar: NSToolbar) -> [NSToolbarItem.Identifier] { [.init("open"), .init("save"), .flexibleSpace, .init("preview"), .init("export"), .init("send")] }
    func toolbar(_ toolbar: NSToolbar, itemForItemIdentifier identifier: NSToolbarItem.Identifier, willBeInsertedIntoToolbar flag: Bool) -> NSToolbarItem? {
        let spec: (String, String, Selector)
        switch identifier.rawValue {
        case "open": spec = ("Öffnen", "folder", #selector(openFile))
        case "save": spec = ("Sichern", "square.and.arrow.down", #selector(saveProject))
        case "preview": spec = ("Vorschau & Zeit", "eye", #selector(previewJob))
        case "export": spec = ("Exportieren", "square.and.arrow.up", #selector(exportJob))
        case "send": spec = ("An Lasercutter senden", "paperplane", #selector(sendJob))
        default: return nil
        }
        let item = NSToolbarItem(itemIdentifier: identifier)
        item.label = spec.0; item.paletteLabel = spec.0; item.toolTip = spec.0
        item.image = NSImage(systemSymbolName: spec.1, accessibilityDescription: spec.0)
        item.target = self; item.action = spec.2
        item.isEnabled = ["send", "export", "preview"].contains(identifier.rawValue) ? model.sendEnabled : !model.busy
        return item
    }
}

@main
enum VisiCutRustApp {
    @MainActor static func main() {
        if CommandLine.arguments.contains("--ui-test") {
            // Never read or change the user's device list during self-tests.
            let settings = FileManager.default.temporaryDirectory.appendingPathComponent("visicut-ui-test-\(UUID().uuidString)")
            setenv("VISICUT_RUST_CONFIG_DIR", settings.path, 1)
        }
        let app = NSApplication.shared
        let delegate = AppDelegate()
        app.delegate = delegate
        app.setActivationPolicy(.regular)
        app.run()
        withExtendedLifetime(delegate) {}
    }
}

struct Workspace: View {
    @ObservedObject var model: AppModel

    var body: some View {
        NavigationSplitView {
            Inspector(model: model)
                .navigationSplitViewColumnWidth(min: 320, ideal: 340, max: 400)
        } detail: {
            VStack(spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(model.project.hasArtwork ? model.project.name : "Arbeitsbereich").font(.title2).fontWeight(.semibold)
                        Text("\(model.device.name) · 1000 × 600 mm" + (model.project.rotary_axis ? " · Drehachse" : "")).foregroundStyle(.secondary)
                    }
                    Spacer()
                    if !model.device.camera_url.isEmpty {
                        Toggle(isOn: Binding(get: { model.showCamera }, set: { _ in model.toggleCamera() })) {
                            Image(systemName: "camera")
                        }.toggleStyle(.button).help("Kamerabild anzeigen")
                        if model.showCamera {
                            if model.cameraLoading { ProgressView().controlSize(.small) }
                            else { Button { model.refreshCamera() } label: { Image(systemName: "arrow.clockwise") }.help("Kamerabild aktualisieren") }
                        }
                    }
                    Button { model.zoom = max(0.5, model.zoom - 0.25) } label: { Image(systemName: "minus.magnifyingglass") }.help("Verkleinern")
                    Text("\(Int(model.zoom * 100)) %").monospacedDigit().frame(width: 48)
                    Button { model.zoom = min(3, model.zoom + 0.25) } label: { Image(systemName: "plus.magnifyingglass") }.help("Vergrößern")
                    Button("Einpassen") { model.zoom = 1 }
                }.buttonStyle(.borderless).padding(20)
                Divider()
                HStack {
                    Button("Vorschau & Zeit berechnen", systemImage: "eye") { model.previewJob() }
                        .disabled(!model.sendEnabled)
                    if let prepared = model.preparedJob {
                        Text("Ca. \(duration(prepared.estimated_seconds))").monospacedDigit()
                        Spacer()
                        Toggle("Bearbeitung anzeigen", isOn: $model.showProcessing)
                        Text("Rot: Schnitt · Blau: Gravur · Orange: 3D · Violett: Markieren").font(.caption).foregroundStyle(.secondary)
                    } else {
                        Text("Nach Änderungen neu berechnen").font(.caption).foregroundStyle(.secondary)
                        Spacer()
                    }
                }.padding(.horizontal, 20).padding(.vertical, 10)
                Divider()
                ZStack {
                    Color(nsColor: .underPageBackgroundColor)
                    if model.project.hasArtwork {
                        BedCanvas(model: model).padding(24)
                    } else {
                        VStack(spacing: 16) {
                            Image(systemName: "square.and.arrow.down").font(.system(size: 44, weight: .light)).foregroundStyle(.secondary)
                            Text("Mit einer SVG beginnen").font(.title2).fontWeight(.semibold)
                            Text("Importiere dein Motiv, wähle ein Material\nund bereite den Job für den Lasercutter vor.")
                                .foregroundStyle(.secondary).multilineTextAlignment(.center)
                            Button("SVG importieren …") { model.chooseFile() }.buttonStyle(.borderedProminent)
                            Button("Beispiel öffnen") { model.demo() }.buttonStyle(.link)
                            Button("Bitmap vektorisieren …") { model.chooseBitmap() }.buttonStyle(.link)
                        }.padding(40)
                    }
                }
                Divider()
                HStack(spacing: 8) {
                    if model.busy { ProgressView().controlSize(.small) }
                    else { Image(systemName: "info.circle").foregroundStyle(.secondary) }
                    Text(model.status).font(.callout).foregroundStyle(.secondary).lineLimit(2)
                    Spacer()
                }.padding(.horizontal, 16).padding(.vertical, 10)
            }
            .frame(minWidth: 560, minHeight: 640)
            .sheet(item: $model.vectorizeSource) { source in VectorizeSheet(model: model, source: source) }
        }
        .navigationTitle(model.project.name)
        .sheet(isPresented: $model.showJobPreview) { JobPreview(model: model) }
        .alert("VisiCutRust", isPresented: Binding(get: { model.error != nil }, set: { if !$0 { model.error = nil } })) {
            Button("OK", role: .cancel) { model.error = nil }
        } message: { Text(model.error ?? "") }
        .onDrop(of: [.fileURL], isTargeted: nil) { providers in
            guard !model.busy, let provider = providers.first else { return false }
            provider.loadItem(forTypeIdentifier: UTType.fileURL.identifier, options: nil) { item, _ in
                guard let data = item as? Data, let url = URL(dataRepresentation: data, relativeTo: nil) else { return }
                DispatchQueue.main.async { model.open(url) }
            }
            return true
        }
    }
}

struct Inspector: View {
    @ObservedObject var model: AppModel
    var material: Binding<String> { Binding(get: { model.materialSelection }, set: { model.selectMaterial($0) }) }

    var body: some View {
        Form {
            Section("Projekt") {
                TextField("Name", text: $model.project.name)
                LabeledContent("Lasercutter") {
                    NativePopup(options: model.devices.devices.indices.map { PopupOption(String($0), model.devices.devices[$0].name) },
                        selection: Binding(get: { String(model.devices.selected) }, set: { if let index = Int($0) { model.selectDevice(index) } }),
                        identifier: "devicePicker")
                        .frame(width: 180)
                }
            }
            if model.device.rotary_axis {
                Section("Drehachse") {
                    Toggle("Drehachse verwenden", isOn: $model.project.rotary_axis)
                    if model.project.rotary_axis {
                        NumberField("Durchmesser", value: $model.project.rotary_diameter_mm, unit: "mm")
                        Text("Y entspricht dem Umfang (\(format(Double.pi * model.project.rotary_diameter_mm)) mm). Gravur zentriert am Gerät ausrichten. Schritte je Umdrehung aus LibLaserCut, am Gerät nicht validiert.")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
            Section("Material") {
                LabeledContent("Material") {
                    NativePopup(options: [PopupOption("", "Material wählen"), .separator]
                        + model.materials.map { PopupOption($0.name, $0.name) }
                        + [.separator, PopupOption("__custom", "Eigenes Material …")],
                        selection: material, identifier: "materialPicker")
                        .frame(width: 180)
                }
                if model.materialSelection == "__custom" {
                    TextField("Bezeichnung", text: $model.project.material)
                }
                if let material = model.selectedMaterial {
                    LabeledContent("Stärke") {
                        NativePopup(options: material.thicknesses.map {
                            PopupOption(String($0), $0 == 0 ? "Nicht festgelegt" : "\(format($0)) mm")
                        }, selection: Binding(get: { String(model.project.thickness_mm) }, set: {
                            if let value = Double($0) { model.project.thickness_mm = value }
                        }), identifier: "thicknessPicker")
                        .frame(width: 140)
                    }
                } else {
                    NumberField("Stärke", value: $model.project.thickness_mm, unit: "mm")
                }
            }
            Section("Objektzuordnung") {
                Picker("Zuordnung", selection: Binding(get: { model.assignmentMode }, set: { model.setAssignmentMode($0) })) {
                    ForEach(AssignmentMode.allCases) { Text($0.title).tag($0) }
                }.pickerStyle(.segmented).disabled(!model.project.hasArtwork)
                    .accessibilityIdentifier("assignmentMode")
                if model.assignmentMode == .whole {
                    Text("Ein Verfahren für das gesamte Motiv.").font(.caption).foregroundStyle(.secondary)
                } else if model.assignmentMode == .rules {
                    Text("Objekte nach Farbe, Linienstärke, Ebene, Typ oder ID zuordnen.").font(.caption).foregroundStyle(.secondary)
                }
                if model.assignmentMode == .objects {
                    Text("Schneiden, gravieren, markieren oder ignorieren.")
                        .font(.caption).foregroundStyle(.secondary)
                    ScrollView {
                        VStack(alignment: .leading, spacing: 10) {
                            ForEach(model.objects) { object in
                                HStack {
                                    Text(object.label).lineLimit(2).font(.callout)
                                    Spacer(minLength: 6)
                                    AssignmentMenu(selection: Binding(get: { model.assignment(object.id) },
                                        set: { model.assign(object.id, to: $0) }))
                                }

                            }
                        }.padding(.vertical, 4)
                    }.frame(height: min(220, Double(model.objects.count) * 38))
                }
                if model.assignmentMode != .whole {
                    Text("Ein LTT-Auftrag je Verfahren: Engrav → Eng3D → Mark → Cut").font(.caption).foregroundStyle(.secondary)
                }
                LabeledContent("Schnittreihenfolge") {
                    NativePopup(options: CutOrder.allCases.map { PopupOption($0.rawValue, $0.title) },
                        selection: Binding(get: { model.project.cut_order.rawValue }, set: {
                            if let order = CutOrder(rawValue: $0) { model.project.cut_order = order }
                        }), identifier: "cutOrderPicker")
                        .frame(width: 180)
                }
                if model.project.cut_order == .shortestTravel {
                    Text("Experimentell: Die Reihenfolge der Schnitt- und Markierpfade weicht von VisiCut ab und ist am Gerät nicht erprobt. Den Auftrag am Gerät beaufsichtigen.")
                        .font(.caption).foregroundStyle(.orange)
                } else {
                    Text("Innere Konturen zuerst, Reihenfolge wie VisiCut.").font(.caption).foregroundStyle(.secondary)
                }
            }
            if model.assignmentMode == .rules { RulesSection(model: model) }
            if model.project.steps.isEmpty {
            Section("Bearbeitung · gesamtes Motiv") {
                OperationSelector(selection: Binding(get: { model.project.operation }, set: { model.selectOperation($0) }))
                if model.project.operation == .mark && model.preset == nil {
                    Text("Konturen markieren. Eigene Leistung einstellen; kein Schnittprofil übernehmen.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                if let preset = model.preset {
                    VStack(alignment: .leading, spacing: 8) {
                        Label("FAU-Profil verfügbar", systemImage: "slider.horizontal.3").font(.callout).fontWeight(.medium)
                        Text("\(format(preset.power_percent)) % Leistung · \(format(preset.speed_percent)) % Geschwindigkeit").font(.callout).foregroundStyle(.secondary)
                        Button("Profil übernehmen") { model.applyPreset() }
                            .accessibilityIdentifier("applyMaterialProfile")
                    }.padding(.vertical, 4)
                } else {
                    Text(model.project.material.isEmpty ? "Wähle ein Material, um passende FAU-Profile zu sehen." : "Für diese Kombination ist kein FAU-Profil hinterlegt. Parameter manuell eingeben.")
                        .font(.callout).foregroundStyle(.secondary)
                }
                NumberField("Leistung", value: $model.project.power_percent, unit: "%")
                NumberField("Geschwindigkeit", value: $model.project.speed_percent, unit: "%")
                Stepper(value: $model.project.passes, in: 1...100) {
                    LabeledContent("Durchgänge", value: String(model.project.passes))
                }
                if model.project.operation.isRaster {
                    RasterOptions(settings: $model.project.raster, operation: model.project.operation)
                }
            }
            } else {
                ForEach(model.project.steps.indices, id: \.self) { index in
                    StepInspector(model: model, index: index)
                }
            }
            Section("Position und Größe") {
                NumberField("X", value: $model.project.x_mm, unit: "mm")
                NumberField("Y", value: $model.project.y_mm, unit: "mm")
                NumberField("Breite", value: Binding(get: { model.project.width_mm }, set: { model.changeWidth($0) }), unit: "mm")
                NumberField("Höhe", value: Binding(get: { model.project.height_mm }, set: { model.changeHeight($0) }), unit: "mm")
                Toggle("Seitenverhältnis beibehalten", isOn: $model.keepProportions)
                Button("Auf dem Arbeitsbett zentrieren") { model.center() }.disabled(!model.project.hasArtwork)
            }
            Section {
                Label(model.project.hasArtwork ? (model.project.fitsBed ? "Motiv passt auf das Arbeitsbett" : "Motiv liegt außerhalb des Arbeitsbetts") : "Noch kein Motiv importiert",
                      systemImage: model.project.hasArtwork && model.project.fitsBed ? "checkmark.circle" : "exclamationmark.circle")
                    .font(.callout)
                    .foregroundStyle(model.project.hasArtwork && !model.project.fitsBed ? Color.orange : Color.secondary)
                Text("Übertragung ohne Autostart. LTT-Treiber experimentell.").font(.caption).foregroundStyle(.secondary)
            }
        }
        .formStyle(.grouped)
        .disabled(model.busy)
        .navigationTitle("Job vorbereiten")
    }
}

struct OperationSelector: View {
    @Binding var selection: Operation
    var body: some View {
        HStack(spacing: 5) {
            ForEach(Operation.allCases) { operation in
                Button { selection = operation } label: {
                    Text(operation.title).font(.caption).fontWeight(selection == operation ? .semibold : .regular)
                        .frame(maxWidth: .infinity).padding(.vertical, 8)
                        .foregroundStyle(operation.color)
                        .background(operation.color.opacity(selection == operation ? 0.18 : 0.04), in: RoundedRectangle(cornerRadius: 6))
                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(operation.color.opacity(selection == operation ? 0.8 : 0.2), lineWidth: 1))
                }.buttonStyle(.plain).accessibilityAddTraits(selection == operation ? .isSelected : [])
            }
        }
    }
}

struct AssignmentMenu: View {
    @Binding var selection: String
    private var operation: Operation? { Operation(rawValue: selection) }
    var body: some View {
        Menu {
            ForEach(Operation.allCases) { operation in
                Button { selection = operation.rawValue } label: { Label(operation.title, systemImage: operation.symbol) }
            }
            Divider()
            Button("Ignorieren", systemImage: "eye.slash") { selection = "Ignore" }
        } label: {
            Label(operation?.title ?? "Ignorieren", systemImage: operation?.symbol ?? "eye.slash")
                .font(.callout).foregroundStyle(operation?.color ?? .secondary)
                .padding(.horizontal, 8).padding(.vertical, 5)
                .background((operation?.color ?? .gray).opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
        }.menuStyle(.borderlessButton).fixedSize().accessibilityLabel("Bearbeitung zuweisen")
    }
}

struct StepInspector: View {
    @ObservedObject var model: AppModel
    let index: Int
    private var step: JobStep { model.project.steps[index] }
    var body: some View {
        Section {
            HStack {
                Label(step.operation.title, systemImage: step.operation.symbol)
                    .fontWeight(.semibold).foregroundStyle(step.operation.color)
                Spacer()
                if step.isRule {
                    Button { model.project.steps.remove(at: index) } label: { Image(systemName: "trash") }
                        .buttonStyle(.borderless).help("Schritt entfernen")
                }
            }
            Text("\(model.objectCount(index)) Objekte").foregroundStyle(.secondary)
            if step.isRule {
                Toggle("Rest: alle übrigen Objekte", isOn: Binding(get: { step.rest }, set: {
                    model.project.steps[index].rest = $0
                    model.project.steps[index].filters = $0 ? nil : []
                }))
                if !step.rest {
                    FilterSetEditor(filters: Binding(get: { model.project.steps[index].filters ?? [] },
                                                     set: { model.project.steps[index].filters = $0 }),
                                    mapping: model.mapping)
                }
            }
            if step.operation == .mark && step.power_percent == 0 {
                Text("Zum Markieren zuerst eine eigene Leistung einstellen.").font(.caption).foregroundStyle(.secondary)
            }
            NumberField("Leistung", value: $model.project.steps[index].power_percent, unit: "%")
            NumberField("Geschwindigkeit", value: $model.project.steps[index].speed_percent, unit: "%")
            Stepper(value: $model.project.steps[index].passes, in: 1...100) {
                LabeledContent("Durchgänge", value: String(step.passes))
            }
            Button("FAU-Profil übernehmen") { model.applyStepPreset(index) }
                .disabled(!(model.selectedMaterial?.profiles.contains {
                    abs($0.thickness_mm - model.project.thickness_mm) < 0.0001 &&
                    $0.operation == step.operation && $0.power_percent > 0
                } ?? false))
            ParameterSets(sets: $model.project.steps[index].additional,
                          first: ParameterSet(power_percent: step.power_percent, speed_percent: step.speed_percent, passes: step.passes))
            if step.operation.isRaster {
                RasterOptions(settings: $model.project.steps[index].raster, operation: step.operation)
            }
        }
    }
}

struct JobPreview: View {
    @ObservedObject var model: AppModel
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Auftragsvorschau").font(.title2).bold()
                Spacer()
                Text(model.project.name).foregroundStyle(.secondary)
            }
            if let prepared = model.preparedJob {
                HStack(alignment: .top, spacing: 24) {
                    VStack {
                        JobSimulation(job: prepared, project: model.project, image: model.jobImage, playback: model.playback)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                        Text("Rot: Schneiden · Blau: Gravieren · Orange: 3D-Gravur · Violett: Markieren")
                            .font(.caption).foregroundStyle(.secondary)
                        Text("\(format(model.project.width_mm)) × \(format(model.project.height_mm)) mm · X \(format(model.project.x_mm)) / Y \(format(model.project.y_mm)) mm")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Ca. \(duration(prepared.estimated_seconds))").font(.title).monospacedDigit()
                        Text("Geschätzte Bearbeitungszeit").foregroundStyle(.secondary)
                        ForEach(prepared.warnings, id: \.self) { warning in
                            Label(warning, systemImage: "exclamationmark.triangle").font(.callout).foregroundStyle(.orange)
                        }
                        ForEach(prepared.steps.indices, id: \.self) { index in
                            let step = prepared.steps[index]
                            VStack(alignment: .leading, spacing: 5) {
                                Label("\(index + 1). \(step.operation.title)", systemImage: step.operation.symbol)
                                    .bold().foregroundStyle(step.operation.color)
                                Text(step.name).font(.caption).monospaced()
                                Text("Ca. \(duration(step.estimated_seconds)) · \(step.passes) Durchgänge" + (step.parameter_sets > 1 ? " · \(step.parameter_sets) Parametersätze" : ""))
                                Text("Leistung \(format(step.power_percent)) % · Tempo \(format(step.speed_percent)) %")
                                Text(step.description).foregroundStyle(.secondary)
                            }.font(.callout)
                        }
                        Text("Schätzung aus Fahrwegen, Tempo und Durchgängen; bei Gravur mit Zeilenrücklauf und Overscan. Beschleunigung und Geräteeinstellungen können die tatsächliche Dauer verändern.")
                            .font(.caption).foregroundStyle(.secondary)
                        Text("\(ByteCountFormatter.string(fromByteCount: Int64(prepared.byteCount), countStyle: .file)) · \(model.project.hostname):\(String(model.project.port))")
                            .font(.caption).foregroundStyle(.secondary)
                        Text("\(prepared.jobs.count) Einzelaufträge · Engrav → Eng3D → Mark → Cut").font(.caption).foregroundStyle(.secondary)
                    }
                    }.frame(width: 270, alignment: .leading)
                }
            }
            Divider()
            HStack {
                Button("Schließen") { model.showJobPreview = false }.keyboardShortcut(.cancelAction)
                Spacer()
                Button("LTT exportieren …") { model.export() }.disabled(!model.sendEnabled)
                Button("An Lasercutter senden …") { model.sendReviewedJob() }
                    .buttonStyle(.borderedProminent).disabled(!model.sendEnabled || model.preparedJob == nil)
            }
        }.padding(24).frame(width: 940, height: 640)
            .background(Color(nsColor: .windowBackgroundColor))
    }
}

struct PopupOption: Equatable {
    let id: String
    let title: String
    let isSeparator: Bool
    init(_ id: String, _ title: String) { self.id = id; self.title = title; self.isSeparator = false }
    private init() { id = ""; title = ""; isSeparator = true }
    static var separator: Self { Self() }
}

struct NativePopup: NSViewRepresentable {
    let options: [PopupOption]
    @Binding var selection: String
    let identifier: String
    @Environment(\.isEnabled) private var isEnabled

    func makeCoordinator() -> Coordinator { Coordinator(selection: $selection) }
    func makeNSView(context: Context) -> NSPopUpButton {
        let button = NSPopUpButton(frame: .zero, pullsDown: false)
        button.controlSize = .regular
        button.lineBreakMode = .byTruncatingTail
        button.setAccessibilityIdentifier(identifier)
        button.target = context.coordinator
        button.action = #selector(Coordinator.changed(_:))
        return button
    }
    func updateNSView(_ button: NSPopUpButton, context: Context) {
        context.coordinator.selection = $selection
        if context.coordinator.options != options {
            button.removeAllItems()
            for option in options {
                if option.isSeparator { button.menu?.addItem(.separator()) }
                else {
                    button.addItem(withTitle: option.title)
                    button.lastItem?.representedObject = option.id
                }
            }
            context.coordinator.options = options
        }
        let index = button.itemArray.firstIndex { $0.representedObject as? String == selection }
        button.selectItem(at: index ?? -1)
        button.isEnabled = isEnabled
        button.toolTip = button.selectedItem?.title
    }
    final class Coordinator: NSObject {
        var selection: Binding<String>
        var options: [PopupOption] = []
        init(selection: Binding<String>) { self.selection = selection }
        @objc func changed(_ sender: NSPopUpButton) {
            if let value = sender.selectedItem?.representedObject as? String { selection.wrappedValue = value }
        }
    }
}

struct NumberField: View {
    let title: String
    @Binding var value: Double
    let unit: String
    init(_ title: String, value: Binding<Double>, unit: String) {
        self.title = title; self._value = value; self.unit = unit
    }
    var body: some View {
        LabeledContent(title) {
            HStack(spacing: 6) {
                TextField(title, value: $value, format: .number.precision(.fractionLength(0...3)))
                    .multilineTextAlignment(.trailing).textFieldStyle(.roundedBorder)
                    .frame(width: 82).labelsHidden()
                Text(unit).foregroundStyle(.secondary).fixedSize().frame(width: 40, alignment: .leading)
            }
        }
    }
}

struct BedCanvas: NSViewRepresentable {
    @ObservedObject var model: AppModel
    func makeNSView(context: Context) -> BedView { BedView(model: model) }
    func updateNSView(_ view: BedView, context: Context) { view.model = model; view.needsDisplay = true }
}

final class BedView: NSView {
    var model: AppModel
    private var dragStart: NSPoint?
    private var originalPosition = NSPoint.zero
    override var isFlipped: Bool { true }
    override var acceptsFirstResponder: Bool { true }
    init(model: AppModel) { self.model = model; super.init(frame: .zero) }
    required init?(coder: NSCoder) { fatalError("Not used") }

    private var scale: Double {
        min(max(1, bounds.width - 42) / model.project.bed_width_mm,
            max(1, bounds.height - 42) / model.project.bed_height_mm) * model.zoom
    }
    private var bed: NSRect {
        NSRect(x: 32, y: 30, width: model.project.bed_width_mm * scale, height: model.project.bed_height_mm * scale)
    }
    private var motif: NSRect {
        let p = model.project
        return NSRect(x: bed.minX + p.x_mm * scale, y: bed.minY + p.y_mm * scale, width: p.width_mm * scale, height: p.height_mm * scale)
    }
    override func draw(_ dirtyRect: NSRect) {
        // The bed is document content: keep a white paper surface so black SVG
        // paths stay visible even when macOS chrome uses Dark Mode.
        NSColor.white.setFill(); bed.fill()
        if model.showCamera, let camera = model.cameraImage {
            camera.draw(in: bed, from: .zero, operation: .sourceOver, fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
        }
        let path = NSBezierPath(rect: bed); path.lineWidth = 1
        NSColor(calibratedWhite: 0.65, alpha: 1).setStroke(); path.stroke()
        if model.showGrid && !(model.showCamera && model.cameraImage != nil) {
            let grid = NSBezierPath(); grid.lineWidth = 0.5
            let step = max(10, ceil(12 / scale / 10) * 10)
            for x in stride(from: 0.0, through: model.project.bed_width_mm, by: step) {
                grid.move(to: NSPoint(x: bed.minX + x * scale, y: bed.minY))
                grid.line(to: NSPoint(x: bed.minX + x * scale, y: bed.maxY))
            }
            for y in stride(from: 0.0, through: model.project.bed_height_mm, by: step) {
                grid.move(to: NSPoint(x: bed.minX, y: bed.minY + y * scale))
                grid.line(to: NSPoint(x: bed.maxX, y: bed.minY + y * scale))
            }
            NSColor(calibratedWhite: 0.84, alpha: 1).setStroke(); grid.stroke()
        }
        let attributes: [NSAttributedString.Key: Any] = [.font: NSFont.monospacedDigitSystemFont(ofSize: 10, weight: .regular), .foregroundColor: NSColor.secondaryLabelColor]
        for x in stride(from: 0.0, through: model.project.bed_width_mm, by: 100.0) {
            (String(Int(x)) as NSString).draw(at: NSPoint(x: bed.minX + x * scale - 8, y: 10), withAttributes: attributes)
        }
        for y in stride(from: 0.0, through: model.project.bed_height_mm, by: 100.0) {
            (String(Int(y)) as NSString).draw(at: NSPoint(x: 2, y: bed.minY + y * scale - 6), withAttributes: attributes)
        }
        NSGraphicsContext.saveGraphicsState()
        NSBezierPath(rect: bed).addClip()
        (model.showProcessing ? model.jobImage ?? model.image : model.image)?.draw(in: motif, from: .zero, operation: .sourceOver, fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
        let selection = NSBezierPath(rect: motif); selection.lineWidth = 1.5
        NSColor.controlAccentColor.setStroke(); selection.stroke()
        NSGraphicsContext.restoreGraphicsState()
    }
    override func mouseDown(with event: NSEvent) {
        let point = convert(event.locationInWindow, from: nil)
        guard !model.busy, motif.contains(point) else { return }
        window?.makeFirstResponder(self)
        dragStart = point
        originalPosition = NSPoint(x: model.project.x_mm, y: model.project.y_mm)
        NSCursor.closedHand.push()
    }
    override func mouseDragged(with event: NSEvent) {
        guard let start = dragStart else { return }
        let point = convert(event.locationInWindow, from: nil)
        model.project.x_mm = min(max(0, originalPosition.x + (point.x - start.x) / scale), max(0, model.project.bed_width_mm - model.project.width_mm))
        model.project.y_mm = min(max(0, originalPosition.y + (point.y - start.y) / scale), max(0, model.project.bed_height_mm - model.project.height_mm))
        needsDisplay = true
    }
    override func mouseUp(with event: NSEvent) {
        if dragStart != nil { NSCursor.pop() }
        dragStart = nil
    }
    override func keyDown(with event: NSEvent) {
        guard !model.busy else { return }
        let step = event.modifierFlags.contains(.shift) ? 10.0 : 1.0
        switch event.keyCode {
        case 123: model.project.x_mm = max(0, model.project.x_mm - step)
        case 124: model.project.x_mm = min(max(0, model.project.bed_width_mm - model.project.width_mm), model.project.x_mm + step)
        case 125: model.project.y_mm = min(max(0, model.project.bed_height_mm - model.project.height_mm), model.project.y_mm + step)
        case 126: model.project.y_mm = max(0, model.project.y_mm - step)
        default: super.keyDown(with: event)
        }
    }
}

/// Device list, rotary jobs and the camera pipeline, using an isolated
/// settings directory and a local image instead of a network camera.
@MainActor
func testDevicesRotaryAndCamera(_ model: AppModel) throws {
    let fau = model.device
    guard model.devices.devices.count == 1, fau.name == "LTT iLaser 4000", fau.rotary_axis,
          fau.camera_calibration?.reference_points.count == 6, model.project.hostname == "lasercutter2"
    else { throw CoreError("FAU-Lasercutter nicht als Standard eingerichtet") }
    var store = model.devices
    var second = fau
    second.name = "LTT Test"; second.hostname = "127.0.0.1"; second.port = 9101; second.rotary_axis = false
    let camera = FileManager.default.temporaryDirectory.appendingPathComponent("visicut-camera-\(UUID().uuidString).png")
    let pixels = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 100, pixelsHigh: 60, bitsPerSample: 8,
        samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    try pixels.representation(using: .png, properties: [:])!.write(to: camera)
    defer { try? FileManager.default.removeItem(at: camera) }
    second.camera_url = camera.path
    second.camera_calibration = CameraCalibration(reference_points: [[0, 0], [1000, 600]], view_points: [[0, 0], [100, 60]])
    store.devices.append(second)
    guard model.saveDevices(store) else { throw CoreError("Geräteliste nicht gespeichert") }
    model.project.rotary_axis = true
    model.selectDevice(1)
    guard model.project.hostname == "127.0.0.1", model.project.port == 9101, !model.project.rotary_axis
    else { throw CoreError("Gerätewechsel übernimmt Ziel oder Drehachse nicht") }
    let reloaded: DeviceList = try RustCore.decode("devices")
    guard reloaded.store == model.devices, reloaded.store.selected == 1 else { throw CoreError("Geräteliste nicht dauerhaft gespeichert") }

    let background: CameraBackground = try RustCore.decode("camera_background", project: model.project,
        values: ["device": RustCore.json(model.device)])
    guard let image = NSImage(data: Data(background.png)), image.size.width >= 100 else { throw CoreError("Kamerahintergrund fehlt") }
    let shot: CameraPicture = try RustCore.decode("camera_image", values: ["device": RustCore.json(model.device)])
    guard shot.width == 100, shot.height == 60 else { throw CoreError("Kamerabild falsch gelesen") }
    let page: ProjectResponse = try RustCore.decode("calibration_page", project: model.project,
        values: ["reference_points": fau.camera_calibration!.reference_points])
    let calibrationJob: PreparedJob = try RustCore.decode("prepare", project: page.project)
    guard page.objects?.count == 1, calibrationJob.steps.first?.operation == .mark else { throw CoreError("Kalibrierseite ungültig") }

    model.selectDevice(0)
    guard model.project.hostname == "lasercutter2" else { throw CoreError("Rückwechsel zum FAU-Gerät fehlgeschlagen") }
    model.project.rotary_axis = true
    model.project.rotary_diameter_mm = 80
    let rotary: PreparedJob = try RustCore.decode("prepare", project: model.project)
    guard rotary.warnings.contains(where: { $0.contains("Adjust rotary temp") }) else { throw CoreError("Drehachsen-Hinweis fehlt") }
    model.project.rotary_diameter_mm = 2
    guard (try? RustCore.decode("prepare", project: model.project) as PreparedJob) == nil else { throw CoreError("Zu kleiner Durchmesser akzeptiert") }
    model.project.rotary_axis = false
    model.project.rotary_diameter_mm = 100
}

/// Rule-based mapping, engraving options, parameter sets and the material
/// library; restores the project afterwards.
@MainActor
func testRulesEngravingAndMaterials(_ model: AppModel) throws {
    let saved = model.project
    defer { model.project = saved }
    model.setAssignmentMode(.rules)
    guard model.assignmentMode == .rules, let mapping = model.mapping,
          let template = mapping.predefined.first(where: { $0.name.hasPrefix("Rot schneiden") }),
          mapping.suggestions(.strokeColor).contains(where: { $0.value == "#ef7141" })
    else { throw CoreError("Regelzuordnung nicht verfügbar") }
    model.applyPredefined(template)
    guard let cut = model.project.steps.firstIndex(where: { $0.operation == .cut }) else { throw CoreError("Vorlage ohne Schnitt") }
    model.project.steps[cut].filters = [Filter(attribute: .strokeColor, value: "#ef7141", compare: false, inverted: false)]
    model.project.steps[cut].additional = [ParameterSet(power_percent: 50, speed_percent: 30, passes: 1)]
    model.addRuleStep(.engrave3d)
    let deep = model.project.steps.count - 1
    model.project.steps[deep].filters = [Filter(attribute: .id, value: "Gravurkreis", compare: false, inverted: false)]
    model.project.steps[deep].power_percent = 40
    model.project.ignore_filters = [[Filter(attribute: .id, value: "Dreieck", compare: false, inverted: false)]]
    model.refreshMapping()
    // Ignored and rule-matched objects are not part of the rest.
    guard model.objectCount(cut) == 1, model.objectCount(deep) == 1,
          let rest = model.project.steps.firstIndex(where: \.rest), model.objectCount(rest) == 0
    else { throw CoreError("Regeln wählen falsche Objekte: \(model.mapping?.selections ?? [])") }
    model.project.ignore_filters = []
    model.refreshMapping()
    guard model.objectCount(rest) == 1 else { throw CoreError("Rest ohne Ignorierregel falsch") }
    if let engrave = model.project.steps.firstIndex(where: { $0.operation == .engrave }) {
        model.project.steps[engrave].raster.dithering = .floydSteinberg
        model.project.steps[engrave].raster.bidirectional = false
    }
    let job: PreparedJob = try RustCore.decode("prepare", project: model.project)
    guard job.jobs.map(\.operation) == [.engrave, .engrave3d, .cut],
          job.steps.first(where: { $0.operation == .cut })?.parameter_sets == 2,
          job.steps.first(where: { $0.operation == .engrave })?.description.contains("Floyd-Steinberg") == true,
          job.timeline.programs.contains(where: { $0.operation == .engrave3d })
    else { throw CoreError("Regel-Auftrag falsch: \(job.jobs.map(\.name))") }
    model.project.steps[cut].filters = [Filter(attribute: .strokeWidth, value: "breit", compare: false, inverted: false)]
    model.refreshMapping()
    guard model.mappingError != nil else { throw CoreError("Ungültige Linienstärke nicht gemeldet") }

    var materials = model.materials
    materials.append(Material(id: "ui-test", name: "UI-Test Material", profiles: [
        MaterialProfile(thickness_mm: 2, operation: .engrave3d, power_percent: 35, speed_percent: 80, source: nil)]))
    guard model.saveMaterials(materials) else { throw CoreError("Materialbibliothek nicht gesichert") }
    let reloaded: MaterialCatalog = try RustCore.decode("materials")
    guard reloaded.custom, reloaded.materials.contains(where: { $0.name == "UI-Test Material" }) else {
        throw CoreError("Eigene Materialbibliothek nicht geladen")
    }
    var duplicate = materials
    duplicate.append(materials[0])
    guard (try? RustCore.call("save_materials", values: ["library": RustCore.json(MaterialLibrary(source: "", device: "", materials: duplicate))])) == nil
    else { throw CoreError("Doppeltes Material akzeptiert") }
    model.resetMaterials()
    guard !model.materialsCustom, !model.materials.contains(where: { $0.name == "UI-Test Material" }),
          model.materials.contains(where: { $0.profiles.contains { $0.operation == .mark } })
    else { throw CoreError("FAU-Bibliothek nicht wiederhergestellt") }
}

@MainActor
func runUITest(_ model: AppModel) {
    // Explicit development mode: exercises native state, persistence and Rust
    // export without contacting the physical cutter or opening file dialogs.
    do {
        guard let root = NSApp.windows.first(where: { $0.isVisible })?.contentView else { throw CoreError("Arbeitsfenster fehlt") }
        root.layoutSubtreeIfNeeded()
        guard let materialButton = popupControl(root, identifier: "materialPicker") else {
            dumpNativeViews(root)
            throw CoreError("Natives Materialmenü fehlt in der View-Hierarchie")
        }
        guard materialButton.indexOfItem(withTitle: "Acryl") >= 0 else { throw CoreError("Acryl fehlt im nativen Menü") }
        materialButton.selectItem(withTitle: "Acryl")
        guard let action = materialButton.action,
              NSApp.sendAction(action, to: materialButton.target, from: materialButton),
              model.project.material == "Acryl" else { throw CoreError("Natives Materialmenü ändert das Projekt nicht") }
        model.project.thickness_mm = 3
        model.project.operation = .cut
        model.applyPreset()
        guard model.preset != nil, model.project.power_percent == 100,
              abs(model.project.speed_percent - 9) < 0.001 else { throw CoreError("Materialprofil nicht übernommen") }
        model.project.operation = .engrave
        model.applyPreset()
        guard model.project.power_percent == 20, abs(model.project.speed_percent - 100) < 0.01 else { throw CoreError("Gravurprofil nicht übernommen") }
        model.selectMaterial("Sperrholz Pappel")
        guard let material = model.selectedMaterial, material.thicknesses.contains(model.project.thickness_mm) else { throw CoreError("Stärkenauswahl nicht synchronisiert") }
        model.selectMaterial("Acryl")
        model.project.thickness_mm = 3
        model.project.operation = .cut
        model.applyPreset()
        model.center()
        let prepared: PreparedJob = try RustCore.decode("prepare", project: model.project)
        guard prepared.jobs.first?.bytes.starts(with: Array("LTT".utf8)) == true else { throw CoreError("LTT-Export fehlgeschlagen") }
        let data = try JSONEncoder().encode(model.project)
        let reloaded = try JSONDecoder().decode(Project.self, from: data)
        guard reloaded == model.project else { throw CoreError("Projekt speichert Auswahl nicht") }
        let temporary = FileManager.default.temporaryDirectory.appendingPathComponent("visicut-native-ui-\(UUID().uuidString).vcr")
        defer { try? FileManager.default.removeItem(at: temporary) }
        model.projectURL = temporary
        guard model.save() else { throw CoreError("Projekt konnte nicht gesichert werden") }
        let loaded: ProjectResponse = try RustCore.decode("load", path: temporary.path)
        guard loaded.project.material == "Acryl", loaded.project.thickness_mm == 3,
              loaded.project.operation == .cut, loaded.project.power_percent == 100,
              abs(loaded.project.speed_percent - model.project.speed_percent) < 0.001,
              loaded.preview != nil else { throw CoreError("Rust lädt Materialauswahl nicht korrekt") }
        guard loaded.project.cut_order == .visiCut else { throw CoreError("Standard-Schnittreihenfolge nicht VisiCut") }
        model.project.cut_order = .shortestTravel
        guard model.save() else { throw CoreError("Schnittreihenfolge konnte nicht gesichert werden") }
        let ordered: ProjectResponse = try RustCore.decode("load", path: temporary.path)
        let orderedJob: PreparedJob = try RustCore.decode("prepare", project: model.project)
        guard ordered.project.cut_order == .shortestTravel,
              orderedJob.warnings.contains(where: { $0.contains("Kürzeste Leerfahrten") })
        else { throw CoreError("Schnittreihenfolge geht beim Sichern/Laden verloren") }
        // Ältere Projektdateien ohne das Feld laden mit der VisiCut-Reihenfolge.
        guard var legacy = try JSONSerialization.jsonObject(with: Data(contentsOf: temporary)) as? [String: Any]
        else { throw CoreError("Gesichertes Projekt ist kein JSON-Objekt") }
        legacy.removeValue(forKey: "cut_order")
        try JSONSerialization.data(withJSONObject: legacy).write(to: temporary, options: .atomic)
        let legacyLoaded: ProjectResponse = try RustCore.decode("load", path: temporary.path)
        guard legacyLoaded.project.cut_order == .visiCut else { throw CoreError("Ältere Projektdatei ohne Schnittreihenfolge nicht geladen") }
        model.project.cut_order = .visiCut
        try testVectorize(model)
        model.projectURL = nil
        let oldWidth = model.project.width_mm, oldHeight = model.project.height_mm
        model.changeWidth(oldWidth * 2)
        guard abs(model.project.height_mm - oldHeight * 2) < 0.001 else { throw CoreError("Proportionale Skalierung fehlgeschlagen") }
        model.changeWidth(oldWidth)
        model.center()
        guard model.objects.count == 3 else { throw CoreError("SVG-Objektliste fehlt") }
        model.setIndividual(true)
        model.assign(1, to: "Engrave")
        model.assign(2, to: "Mark")
        // Marking uses the FAU mark profile if there is one, never the cut parameters.
        let markPreset = model.selectedMaterial?.profiles.first {
            abs($0.thickness_mm - model.project.thickness_mm) < 0.0001 && $0.operation == .mark && $0.power_percent > 0
        }
        guard let markIndex = model.project.steps.firstIndex(where: { $0.operation == .mark }),
              model.project.steps[markIndex].power_percent == (markPreset?.power_percent ?? 0)
        else { throw CoreError("Markieren übernimmt ungewollt Schnittparameter") }
        model.project.steps[markIndex].power_percent = 5
        model.project.steps[markIndex].speed_percent = 60
        model.project.steps[markIndex].passes = 2
        let mixed: PreparedJob = try RustCore.decode("prepare", project: model.project)
        guard mixed.steps.count == 3, mixed.steps[0].operation == .engrave,
              mixed.steps[1].operation == .mark, mixed.steps[2].operation == .cut, mixed.estimated_seconds > 0,
              NSImage(data: Data(mixed.preview_png)) != nil else { throw CoreError("Gemischte Auftragsvorschau fehlt") }
        try testJobSimulation(mixed)
        model.projectURL = temporary
        guard model.save() else { throw CoreError("Objektzuordnung konnte nicht gespeichert werden") }
        let mixedReloaded: ProjectResponse = try RustCore.decode("load", path: temporary.path)
        guard mixedReloaded.project.steps == model.project.steps else { throw CoreError("Zuordnung oder Parameter gingen verloren") }
        model.projectURL = nil
        model.preparedJob = mixed
        model.jobImage = NSImage(data: Data(mixed.preview_png))
        model.project.x_mm += 1
        guard model.preparedJob == nil, model.jobImage == nil else { throw CoreError("Veraltete Auftragsvorschau bleibt aktiv") }
        model.project.x_mm -= 1
        model.preparedJob = mixed
        model.jobImage = NSImage(data: Data(mixed.preview_png))
        try testDevicesRotaryAndCamera(model)
        try testRulesEngravingAndMaterials(model)
        // Exercise the actual asynchronous preparation and sheet presentation.
        model.preparedJob = nil
        model.jobImage = nil
        model.previewJob()
        model.dirty = false
        print("Native UI state tests passed: material, thickness, operation, presets, proportional scaling, native save / Rust reload, cut order and legacy files, bitmap vectorization, Rust export, object assignments, mixed job preview and estimate, stale preview invalidation, devices, rotary axis, camera background and calibration, rule mapping, 3D engraving, dithering, parameter sets, material library")
    } catch {
        fputs("Native UI tests failed: \(error)\n", stderr)
        exit(1)
    }
    captureUITest(model, deadline: Date().addingTimeInterval(20))
}

@MainActor
func captureUITest(_ model: AppModel, deadline: Date) {
    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
        if model.busy, Date() < deadline { captureUITest(model, deadline: deadline); return }
        guard !model.busy, model.preparedJob != nil, model.showJobPreview else {
            fputs("Native UI tests failed: asynchronous preview not ready\n", stderr); exit(1)
        }
        // SwiftUI presents sheets using different window hierarchies across
        // macOS versions. Wait for the actual slider to enter a visible view,
        // rather than assuming that updating the model has presented the sheet.
        let windows = NSApp.windows.filter { $0.isVisible }
        let roots = windows.flatMap { window in
            ([window] + window.sheets + (window.childWindows ?? [])).compactMap { $0.contentView }
        }
        for root in roots { root.layoutSubtreeIfNeeded() }
        guard let view = roots.first(where: { simulationSlider($0) != nil }),
              let slider = simulationSlider(view), let prepared = model.preparedJob else {
            if Date() < deadline { captureUITest(model, deadline: deadline); return }
            fputs("Native UI tests failed: preview slider not presented before deadline (\(windows.count) visible windows)\n", stderr)
            for window in windows {
                fputs("Window: \(window.title), sheets: \(window.sheets.count), children: \(window.childWindows?.count ?? 0)\n", stderr)
            }
            for root in roots { dumpNativeViews(root) }
            exit(1)
        }
        // Finish the asynchronous setup before checking that slider actions do
        // not dirty the document. SwiftUI may commit focused fields while the
        // preview is being presented on older macOS versions.
        model.dirty = false
        let document = model.project
        @MainActor func moveSlider(_ value: Double) {
            slider.doubleValue = value
            guard let action = slider.action, NSApp.sendAction(action, to: slider.target, from: slider),
                  abs(model.playback.seconds - value) < 0.001 else {
                fputs("Native UI tests failed: Zeitslider ohne Wirkung\n", stderr); exit(1)
            }
        }
        moveSlider(prepared.estimated_seconds)
        moveSlider(0)
        let markRun = prepared.timeline.runs.first { prepared.timeline.programs[$0.program_index].operation == .mark }!
        moveSlider((markRun.entry_end_seconds + markRun.end_seconds) / 2)
        guard model.project == document, !model.dirty else {
            fputs("Native UI tests failed: moving the slider changed the document (content: \(model.project != document), dirty: \(model.dirty))\n", stderr); exit(1)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            guard let image = view.bitmapImageRepForCachingDisplay(in: view.bounds) else {
                fputs("Native UI tests failed: could not capture preview view\n", stderr); exit(1)
            }
            view.cacheDisplay(in: view.bounds, to: image)
            if let index = CommandLine.arguments.firstIndex(of: "--capture"), CommandLine.arguments.count > index + 1 {
                guard let png = image.representation(using: .png, properties: [:]) else {
                    fputs("Native UI tests failed: could not encode preview PNG\n", stderr); exit(1)
                }
                do { try png.write(to: URL(fileURLWithPath: CommandLine.arguments[index + 1])) }
                catch { fputs("Capture failed: \(error)\n", stderr); exit(1) }
            }
            print("Native simulation tests passed: slider start/end/backward, interpolation, playback, three operation jobs")
            exit(0)
        }
    }
}

@MainActor
func simulationSlider(_ view: NSView) -> NSSlider? {
    if let slider = view as? NSSlider, slider.accessibilityIdentifier() == "jobTimeSlider" { return slider }
    for child in view.subviews { if let slider = simulationSlider(child) { return slider } }
    return nil
}

@MainActor
func dumpNativeViews(_ view: NSView, depth: Int = 0) {
    guard depth < 40 else { return }
    if depth < 4 || view is NSPopUpButton {
        fputs("View \(depth): \(type(of: view)), identifier=\(view.accessibilityIdentifier()), frame=\(view.frame)\n", stderr)
    }
    for child in view.subviews { dumpNativeViews(child, depth: depth + 1) }
}

@MainActor
func popupControl(_ view: NSView, identifier: String, depth: Int = 0) -> NSPopUpButton? {
    guard depth < 40 else { return nil }
    if let popup = view as? NSPopUpButton, popup.accessibilityIdentifier() == identifier { return popup }
    for child in view.subviews {
        if let found = popupControl(child, identifier: identifier, depth: depth + 1) { return found }
    }
    return nil
}
