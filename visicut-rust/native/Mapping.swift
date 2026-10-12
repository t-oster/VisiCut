import AppKit
import SwiftUI
import UniformTypeIdentifiers

enum Dithering: String, Codable, CaseIterable, Identifiable {
    case brightenedHalftone = "BrightenedHalftone", floydSteinberg = "FloydSteinberg", halftone = "Halftone",
         ordered = "Ordered", average = "Average", grid = "Grid", random = "Random", threshold = "Threshold"
    var id: String { rawValue }
    var title: String {
        switch self {
        case .brightenedHalftone: return "Halbton aufgehellt"
        case .floydSteinberg: return "Floyd-Steinberg"
        case .halftone: return "Halbton"
        case .ordered: return "Geordnet"
        case .average: return "Mittelwert"
        case .grid: return "Raster"
        case .random: return "Zufall"
        case .threshold: return "Schwellwert 50 %"
        }
    }
}

struct RasterSettings: Codable, Equatable {
    var dithering: Dithering
    var invert: Bool
    var color_shift: Int
    var bidirectional: Bool
    var bottom_up: Bool
    static let fau = RasterSettings(dithering: .brightenedHalftone, invert: false, color_shift: 0, bidirectional: true, bottom_up: false)
}

enum FilterAttribute: String, Codable, CaseIterable, Identifiable {
    case color = "Color", strokeColor = "StrokeColor", fillColor = "FillColor", strokeWidth = "StrokeWidth",
         group = "Group", type = "Type", id = "Id"
    var id: String { rawValue }
    var title: String {
        switch self {
        case .color: return "Farbe"
        case .strokeColor: return "Linienfarbe"
        case .fillColor: return "Füllfarbe"
        case .strokeWidth: return "Linienstärke (mm)"
        case .group: return "Gruppe/Ebene"
        case .type: return "Typ"
        case .id: return "ID"
        }
    }
    var isColor: Bool { self == .color || self == .strokeColor || self == .fillColor }
}

struct Filter: Codable, Equatable {
    var attribute: FilterAttribute
    var value: String
    var compare: Bool
    var inverted: Bool
}

struct ParameterSet: Codable, Equatable {
    var power_percent: Double
    var speed_percent: Double
    var passes: Int
}

struct AttributeValue: Decodable, Hashable { let value: String; let count: Int }
struct AttributeValues: Decodable { let attribute: FilterAttribute; let values: [AttributeValue] }
struct PredefinedRule: Decodable { let operation: Operation; let filters: [Filter]?; let rest: Bool }
struct PredefinedMapping: Decodable { let name: String; let rules: [PredefinedRule]; let ignore: [[Filter]] }
struct MappingInfo: Decodable {
    let selections: [[Int]]
    let values: [AttributeValues]
    let predefined: [PredefinedMapping]
    func suggestions(_ attribute: FilterAttribute) -> [AttributeValue] {
        values.first { $0.attribute == attribute }?.values ?? []
    }
}

enum AssignmentMode: String, CaseIterable, Identifiable {
    case whole, objects, rules
    var id: String { rawValue }
    var title: String {
        switch self { case .whole: return "Gesamt"; case .objects: return "Einzeln"; case .rules: return "Regeln" }
    }
}

struct MaterialLibrary: Codable { var source: String; var device: String; var materials: [Material] }
struct MaterialCatalog: Decodable {
    let materials: [Material]; let source: String; let device: String; let custom: Bool; let error: String?
    /// FAU-Profile der anderen Lasercutter (nur lesbar).
    let others: [MaterialLibrary]
}
struct MaterialImport: Decodable { let library: MaterialLibrary; let imported: Int }
struct MaterialReset: Decodable { let library: MaterialLibrary }

extension JobStep {
    init(operation: Operation, objects: [Int], power_percent: Double, speed_percent: Double, passes: Int) {
        self.init(operation: operation, objects: objects, power_percent: power_percent, speed_percent: speed_percent,
                  passes: passes, filters: nil, rest: false, raster: .fau, additional: [])
    }
    var isRule: Bool { filters != nil || rest }
}

extension AppModel {
    var assignmentMode: AssignmentMode {
        project.steps.isEmpty ? .whole : (project.steps.contains(where: \.isRule) ? .rules : .objects)
    }

    func setAssignmentMode(_ mode: AssignmentMode) {
        guard mode != assignmentMode else { return }
        switch mode {
        case .whole: project.steps = []; project.ignore_filters = []
        case .objects: project.ignore_filters = []; setIndividual(true)
        case .rules:
            refreshMapping()
            if let first = mapping?.predefined.first { applyPredefined(first) }
        }
    }

    func applyPredefined(_ mapping: PredefinedMapping) {
        var project = self.project
        project.steps = mapping.rules.map { rule in
            var step = defaultStep(rule.operation)
            step.filters = rule.filters
            step.rest = rule.rest
            return step
        }
        project.ignore_filters = mapping.ignore
        self.project = project
        status = "Zuordnung „\(mapping.name)“ übernommen."
    }

    func addRuleStep(_ operation: Operation) {
        var step = defaultStep(operation)
        step.filters = []
        project.steps.append(step)
    }

    func objectCount(_ index: Int) -> Int {
        guard project.steps.indices.contains(index) else { return 0 }
        if project.steps[index].isRule {
            return mapping?.selections.indices.contains(index) == true ? mapping!.selections[index].count : 0
        }
        return project.steps[index].objects.count
    }

    func refreshMapping() {
        guard project.hasArtwork else { mapping = nil; mappingError = nil; return }
        do {
            mapping = try RustCore.decode("mapping", project: project)
            mappingError = nil
        } catch {
            mappingError = error.localizedDescription
        }
    }

    func scheduleMappingRefresh() {
        guard !mappingScheduled else { return }
        mappingScheduled = true
        DispatchQueue.main.async {
            self.mappingScheduled = false
            self.refreshMapping()
        }
    }

    // MARK: Materials

    var materialLibrary: MaterialLibrary { MaterialLibrary(source: materialSource, device: "LTT iLaser 4000", materials: materials) }

    func adoptMaterials(_ library: MaterialLibrary, custom: Bool) {
        materials = library.materials
        materialSource = library.source
        materialsCustom = custom
    }

    @discardableResult
    func saveMaterials(_ materials: [Material]) -> Bool {
        let library = MaterialLibrary(source: materialSource, device: "LTT iLaser 4000", materials: materials)
        do {
            _ = try RustCore.call("save_materials", values: ["library": RustCore.json(library)])
            adoptMaterials(library, custom: true)
            status = "Materialbibliothek gesichert."
            return true
        } catch { self.error = error.localizedDescription; return false }
    }

    func resetMaterials() {
        do {
            let result: MaterialReset = try RustCore.decode("reset_materials")
            adoptMaterials(result.library, custom: false)
            status = "FAU-Materialbibliothek wiederhergestellt."
        } catch { self.error = error.localizedDescription }
    }

    func importMaterials() {
        let panel = NSOpenPanel()
        panel.title = "Materialbibliothek importieren"
        panel.allowedContentTypes = [.json]
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            let result: MaterialImport = try RustCore.decode("import_materials", path: url.path,
                values: ["library": RustCore.json(materialLibrary)])
            adoptMaterials(result.library, custom: true)
            status = "\(result.imported) Materialien importiert."
        } catch { self.error = error.localizedDescription }
    }

    func exportMaterials() {
        let panel = NSSavePanel()
        panel.title = "Materialbibliothek exportieren"
        panel.allowedContentTypes = [.json]
        panel.nameFieldStringValue = "Materialien.json"
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            _ = try RustCore.call("export_materials", path: url.path, values: ["library": RustCore.json(materialLibrary)])
            status = "Materialbibliothek exportiert."
        } catch { self.error = error.localizedDescription }
    }
}

/// Raster options of an engraving step (VisiCut raster profile).
struct RasterOptions: View {
    @Binding var settings: RasterSettings
    let operation: Operation
    var body: some View {
        if operation == .engrave {
            LabeledContent("Rasterung") {
                NativePopup(options: Dithering.allCases.map { PopupOption($0.rawValue, $0.title) },
                    selection: Binding(get: { settings.dithering.rawValue },
                                       set: { if let d = Dithering(rawValue: $0) { settings.dithering = d } }),
                    identifier: "ditheringPicker").frame(width: 170)
            }
        } else {
            Text("Graustufen: Die Laserleistung folgt der Helligkeit (dunkel = tief).")
                .font(.caption).foregroundStyle(.secondary)
        }
        NumberField("Helligkeit", value: Binding(get: { Double(settings.color_shift) },
                                                 set: { settings.color_shift = Int(max(-255, min(255, $0))) }), unit: "±255")
        Toggle("Farben invertieren", isOn: $settings.invert)
        Toggle("Bidirektional gravieren", isOn: $settings.bidirectional)
        Toggle("Von unten nach oben", isOn: $settings.bottom_up)
    }
}

/// Additional power/speed sets processed after the first one.
struct ParameterSets: View {
    @Binding var sets: [ParameterSet]
    let first: ParameterSet
    // One container: several ForEach with index ids in one Form section drop rows.
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
        ForEach(sets.indices, id: \.self) { index in
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Parametersatz \(index + 2)").font(.callout).fontWeight(.medium)
                    Spacer()
                    Button { if sets.indices.contains(index) { sets.remove(at: index) } } label: { Image(systemName: "minus.circle") }
                        .buttonStyle(.borderless).help("Parametersatz entfernen")
                }
                let set = $sets.safe(index, default: sets[index])
                NumberField("Leistung", value: set.power_percent, unit: "%")
                NumberField("Geschwindigkeit", value: set.speed_percent, unit: "%")
                Stepper(value: set.passes, in: 1...100) {
                    LabeledContent("Durchgänge", value: String(set.wrappedValue.passes))
                }
            }
        }
        Button("Weiteren Parametersatz", systemImage: "plus") { sets.append(first) }
            .disabled(sets.count >= 15)
        }
    }
}

/// Conditions of one rule; all must match (VisiCut FilterSet).
struct FilterSetEditor: View {
    @Binding var filters: [Filter]
    let mapping: MappingInfo?
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
        if filters.isEmpty {
            Text("Ohne Bedingung: alle Objekte").font(.caption).foregroundStyle(.secondary)
        }
        ForEach(filters.indices, id: \.self) { index in
            FilterRow(filter: $filters.safe(index, default: filters[index]),
                      suggestions: mapping?.suggestions(filters[index].attribute) ?? []) {
                if filters.indices.contains(index) { filters.remove(at: index) }
            }
        }
        Button("Bedingung hinzufügen", systemImage: "plus") {
            let value = mapping?.suggestions(.color).first?.value ?? "#000000"
            filters.append(Filter(attribute: .color, value: value, compare: false, inverted: false))
        }
        }
    }
}

struct FilterRow: View {
    @Binding var filter: Filter
    let suggestions: [AttributeValue]
    let remove: () -> Void

    private var comparison: Binding<String> {
        Binding(get: { (filter.compare ? "le" : "eq") + (filter.inverted ? "!" : "") },
                set: { filter.compare = $0.hasPrefix("le"); filter.inverted = $0.hasSuffix("!") })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                NativePopup(options: FilterAttribute.allCases.map { PopupOption($0.rawValue, $0.title) },
                    selection: Binding(get: { filter.attribute.rawValue }, set: {
                        guard let attribute = FilterAttribute(rawValue: $0), attribute != filter.attribute else { return }
                        filter = Filter(attribute: attribute, value: attribute == .strokeWidth ? "0.1" : "", compare: false, inverted: false)
                    }), identifier: "filterAttribute").frame(width: 140)
                NativePopup(options: [PopupOption("eq", "="), PopupOption("eq!", "≠")]
                    + (filter.attribute == .strokeWidth ? [PopupOption("le", "≤"), PopupOption("le!", ">")] : []),
                    selection: comparison, identifier: "filterComparison").frame(width: 56)
                Spacer()
                Button(action: remove) { Image(systemName: "trash") }.buttonStyle(.borderless).help("Bedingung entfernen")
            }
            HStack(spacing: 6) {
                if filter.attribute.isColor, let color = swatch(filter.value) {
                    RoundedRectangle(cornerRadius: 3).fill(color).frame(width: 14, height: 14)
                        .overlay(RoundedRectangle(cornerRadius: 3).stroke(.secondary.opacity(0.5)))
                }
                TextField("Wert", text: $filter.value).textFieldStyle(.roundedBorder)
                if !suggestions.isEmpty {
                    Menu {
                        ForEach(suggestions, id: \.self) { item in
                            Button("\(item.value) (\(item.count))") { filter.value = item.value }
                        }
                    } label: { Image(systemName: "list.bullet") }
                        .menuStyle(.borderlessButton).fixedSize().help("Werte aus der SVG")
                }
            }
        }.padding(.vertical, 2)
    }

    private func swatch(_ value: String) -> Color? {
        guard value.count == 7, value.hasPrefix("#"), let rgb = Int(value.dropFirst(), radix: 16) else { return nil }
        return Color(red: Double(rgb >> 16 & 0xff) / 255, green: Double(rgb >> 8 & 0xff) / 255, blue: Double(rgb & 0xff) / 255)
    }
}

struct RulesSection: View {
    @ObservedObject var model: AppModel
    var body: some View {
        Section("Regeln") {
            if let mapping = model.mapping {
                Menu("Vorlage übernehmen") {
                    ForEach(mapping.predefined, id: \.name) { item in
                        Button(item.name) { model.applyPredefined(item) }
                    }
                }
            }
            Text("Jeder Schritt bearbeitet die Objekte, die alle Bedingungen erfüllen. Ein Objekt kann mehrere Schritte durchlaufen; „Rest“ erfasst alles, was sonst nicht bearbeitet oder ignoriert wird.")
                .font(.caption).foregroundStyle(.secondary)
            Menu("Schritt hinzufügen") {
                ForEach(Operation.allCases) { operation in
                    Button { model.addRuleStep(operation) } label: { Label(operation.title, systemImage: operation.symbol) }
                }
            }
            if let error = model.mappingError {
                Label(error, systemImage: "exclamationmark.triangle").font(.caption).foregroundStyle(.orange)
            }
        }
        Section("Ignorieren") {
            ForEach(model.project.ignore_filters.indices, id: \.self) { index in
                VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Regel \(index + 1)").font(.callout).fontWeight(.medium)
                    Spacer()
                    Button { if model.project.ignore_filters.indices.contains(index) { model.project.ignore_filters.remove(at: index) } } label: { Image(systemName: "minus.circle") }
                        .buttonStyle(.borderless).help("Ignorierregel entfernen")
                }
                FilterSetEditor(filters: $model.project.ignore_filters.safe(index, default: model.project.ignore_filters[index]), mapping: model.mapping)
                }
            }
            Button("Ignorierregel hinzufügen", systemImage: "plus") {
                let value = model.mapping?.suggestions(.color).first?.value ?? "#0000ff"
                model.project.ignore_filters.append([Filter(attribute: .color, value: value, compare: false, inverted: false)])
            }
        }
    }
}

/// Editor for the material library (VisiCut's material and profile dialogs).
struct MaterialEditor: View {
    @ObservedObject var model: AppModel
    @State private var draft: [Material]
    @State private var selection: Int?

    init(model: AppModel) {
        self.model = model
        _draft = State(initialValue: model.materials)
        _selection = State(initialValue: model.materials.isEmpty ? nil : 0)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                VStack(spacing: 0) {
                    List(selection: $selection) {
                        ForEach(draft.indices, id: \.self) { index in
                            Text(draft[index].name).lineLimit(1).tag(index)
                        }
                    }
                    Divider()
                    HStack(spacing: 2) {
                        Button { add() } label: { Image(systemName: "plus") }.help("Material hinzufügen")
                        Button { duplicate() } label: { Image(systemName: "plus.square.on.square") }
                            .help("Material duplizieren").disabled(selection == nil)
                        Button { remove() } label: { Image(systemName: "minus") }
                            .help("Material entfernen").disabled(selection == nil)
                        Spacer()
                    }.buttonStyle(.borderless).padding(6)
                }.frame(width: 220)
                Divider()
                if let index = selection, draft.indices.contains(index) {
                    MaterialProfilesEditor(material: $draft[index])
                } else {
                    Text("Material auswählen").foregroundStyle(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            Divider()
            HStack {
                Button("Importieren …") { model.importMaterials(); draft = model.materials }
                Button("Exportieren …") { model.exportMaterials() }
                Button("FAU-Bibliothek wiederherstellen") {
                    model.resetMaterials(); draft = model.materials; selection = draft.isEmpty ? nil : 0
                }.disabled(!model.materialsCustom)
                Spacer()
                Text(model.materialsCustom ? "Eigene Bibliothek" : "FAU-Bibliothek").font(.caption).foregroundStyle(.secondary)
                Button("Verwerfen") { draft = model.materials }.disabled(draft == model.materials)
                Button("Sichern") { model.saveMaterials(draft) }
                    .keyboardShortcut(.defaultAction).disabled(draft == model.materials)
            }.padding(12)
        }.frame(minWidth: 820, minHeight: 520)
    }

    private func uniqueName(_ base: String) -> String {
        var name = base, n = 2
        while draft.contains(where: { $0.name == name }) { name = "\(base) (\(n))"; n += 1 }
        return name
    }
    private func add() {
        draft.append(Material(id: UUID().uuidString, name: uniqueName("Neues Material"), profiles: []))
        selection = draft.count - 1
    }
    private func duplicate() {
        guard let index = selection else { return }
        var copy = draft[index]
        copy.id = UUID().uuidString
        copy.name = uniqueName(copy.name)
        draft.append(copy)
        selection = draft.count - 1
    }
    private func remove() {
        guard let index = selection else { return }
        draft.remove(at: index)
        selection = draft.isEmpty ? nil : min(index, draft.count - 1)
    }
}

struct MaterialProfilesEditor: View {
    @Binding var material: Material
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            TextField("Name", text: $material.name).textFieldStyle(.roundedBorder).font(.title3)
            HStack {
                Text("Stärke").frame(width: 90, alignment: .leading)
                Text("Verfahren").frame(width: 130, alignment: .leading)
                Text("Leistung").frame(width: 90, alignment: .leading)
                Text("Geschwindigkeit").frame(width: 110, alignment: .leading)
            }.font(.caption).foregroundStyle(.secondary)
            ScrollView {
                VStack(spacing: 6) {
                    ForEach(material.profiles.indices, id: \.self) { index in
                        HStack {
                            field($material.profiles[index].thickness_mm, unit: "mm").frame(width: 90)
                            NativePopup(options: Operation.allCases.map { PopupOption($0.rawValue, $0.title) },
                                selection: Binding(get: { material.profiles[index].operation.rawValue },
                                                   set: { if let o = Operation(rawValue: $0) { material.profiles[index].operation = o } }),
                                identifier: "profileOperation").frame(width: 130)
                            field($material.profiles[index].power_percent, unit: "%").frame(width: 90)
                            field($material.profiles[index].speed_percent, unit: "%").frame(width: 110)
                            Button { material.profiles.remove(at: index) } label: { Image(systemName: "trash") }
                                .buttonStyle(.borderless).help("Profil entfernen")
                            Spacer()
                        }
                    }
                }
            }
            Button("Profil hinzufügen", systemImage: "plus") {
                let last = material.profiles.last
                material.profiles.append(MaterialProfile(thickness_mm: last?.thickness_mm ?? 3, operation: .cut,
                    power_percent: 100, speed_percent: 10, source: nil))
            }
            Text("Leistung und Geschwindigkeit in Prozent wie am LTT. Je Stärke ist ein Profil pro Verfahren möglich; 0 % Leistung gilt als „nicht übernehmbar“.")
                .font(.caption).foregroundStyle(.secondary)
        }.padding(16).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func field(_ value: Binding<Double>, unit: String) -> some View {
        HStack(spacing: 4) {
            TextField("", value: value, format: .number.precision(.fractionLength(0...3)))
                .multilineTextAlignment(.trailing).textFieldStyle(.roundedBorder)
            Text(unit).foregroundStyle(.secondary).font(.caption)
        }
    }
}


extension Binding where Value: MutableCollection & RandomAccessCollection, Value.Index == Int {
    /// Element binding that survives the element disappearing: SwiftUI can
    /// render a removed row once more (e.g. when the assignment mode drops
    /// steps), which must not index out of range.
    func safe(_ index: Int, default fallback: Value.Element) -> Binding<Value.Element> {
        Binding<Value.Element>(
            get: { wrappedValue.indices.contains(index) ? wrappedValue[index] : fallback },
            set: { if wrappedValue.indices.contains(index) { wrappedValue[index] = $0 } })
    }
}
