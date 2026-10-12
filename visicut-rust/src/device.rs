//! Configured LTT iLaser 4000 installations. Other cutter drivers are not
//! supported; imports from VisiCut settings keep only LTT devices.
use crate::project::MachineSettings;
use crate::{i18n::tr, tr};
use serde::{Deserialize, Serialize};
use std::{
    io::{Cursor, Read},
    path::{Path, PathBuf},
};

const LTT_CLASS: &str = "de.thomas_oster.liblasercut.drivers.LaserToolsTechnicsCutter";
const STORE_FILE: &str = "devices.json";

/// Camera correspondences as in VisiCut: bed positions in mm and the pixel
/// positions where the markers appear in the camera image.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct CameraCalibration {
    pub reference_points: Vec<[f64; 2]>,
    pub view_points: Vec<[f64; 2]>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(default, deny_unknown_fields)]
pub struct LaserDevice {
    pub name: String,
    pub description: String,
    pub hostname: String,
    pub port: u16,
    pub rotary_axis: bool,
    /// Checklist shown after sending, e.g. focus and air assist. The LTT
    /// protocol has no commands for them; they are set at the machine.
    pub job_sent_text: String,
    pub camera_url: String,
    pub camera_calibration: Option<CameraCalibration>,
    /// Coordinates are shown from the bottom left corner of the bed (VisiCut
    /// `originBottomLeft`): rulers and Y count upwards. Display only; the
    /// job output does not change.
    pub origin_bottom_left: bool,
    /// Engrave shift table, curve acceleration and contour start dwell.
    pub machine: MachineSettings,
}

impl Default for LaserDevice {
    fn default() -> Self {
        Self {
            name: "LTT iLaser 4000".into(),
            description: String::new(),
            hostname: "lasercutter2".into(),
            port: 9100,
            rotary_axis: false,
            job_sent_text: String::new(),
            camera_url: String::new(),
            camera_calibration: None,
            origin_bottom_left: false,
            machine: MachineSettings::default(),
        }
    }
}

impl LaserDevice {
    /// The FAU FabLab configuration shipped with VisiCut settings.
    pub fn fau() -> Self {
        let mut devices = from_visicut_xml(include_str!("../reference/FAU-LTT-iLaser-4000.xml"))
            .expect("bundled FAU device must parse");
        devices.description = "FAU FabLab".into();
        devices
    }

    pub fn validate(&self) -> Result<(), String> {
        if self.name.trim().is_empty() {
            return Err(tr("Gerätename fehlt").into());
        }
        if self.hostname.trim().is_empty() || self.port == 0 {
            return Err(tr!(
                "{device}: Hostname und Port müssen gesetzt sein",
                device = self.name
            ));
        }
        self.machine
            .validate()
            .map_err(|e| format!("{}: {e}", self.name))?;
        if let Some(c) = &self.camera_calibration {
            if c.reference_points.len() != c.view_points.len()
                || !(c.reference_points.len() == 2 || c.reference_points.len() >= 4)
            {
                return Err(tr!(
                    "{device}: Kamerakalibrierung benötigt 2 oder mindestens 4 Punktpaare",
                    device = self.name
                ));
            }
            let finite = |p: &[f64; 2]| p.iter().all(|v| v.is_finite());
            if !c.reference_points.iter().chain(&c.view_points).all(finite) {
                return Err(tr!(
                    "{device}: Kamerapunkte müssen endlich sein",
                    device = self.name
                ));
            }
        }
        Ok(())
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct DeviceStore {
    pub format_version: u32,
    pub devices: Vec<LaserDevice>,
    pub selected: usize,
}

impl Default for DeviceStore {
    fn default() -> Self {
        Self {
            format_version: 1,
            devices: vec![LaserDevice::fau()],
            selected: 0,
        }
    }
}

impl DeviceStore {
    pub fn validate(&self) -> Result<(), String> {
        if self.format_version != 1 {
            return Err(tr("Unbekannte Version der Geräteliste").into());
        }
        if self.devices.is_empty() {
            return Err(tr("Mindestens ein Lasercutter muss eingerichtet sein").into());
        }
        if self.selected >= self.devices.len() {
            return Err(tr("Ausgewählter Lasercutter existiert nicht").into());
        }
        for (i, device) in self.devices.iter().enumerate() {
            device.validate()?;
            if self.devices[..i].iter().any(|d| d.name == device.name) {
                return Err(tr!(
                    "Gerätename „{name}“ ist doppelt vorhanden",
                    name = device.name
                ));
            }
        }
        Ok(())
    }

    /// Adds devices, renaming duplicates as VisiCut does for copies.
    pub fn merge(&mut self, devices: Vec<LaserDevice>) -> usize {
        let count = devices.len();
        for mut device in devices {
            let base = device.name.clone();
            let mut n = 2;
            while self.devices.iter().any(|d| d.name == device.name) {
                device.name = format!("{base} ({n})");
                n += 1;
            }
            self.devices.push(device);
        }
        count
    }

    pub fn load(dir: &Path) -> Result<Self, String> {
        let path = dir.join(STORE_FILE);
        if !path.exists() {
            return Ok(Self::default());
        }
        let source = std::fs::read_to_string(&path).map_err(|e| e.to_string())?;
        let store: Self = serde_json::from_str(&source).map_err(|e| {
            tr!(
                "Geräteliste {path} ist beschädigt: {error}",
                path = path.display(),
                error = e
            )
        })?;
        store.validate()?;
        Ok(store)
    }

    pub fn save(&self, dir: &Path) -> Result<(), String> {
        self.validate()?;
        std::fs::create_dir_all(dir).map_err(|e| e.to_string())?;
        let json = serde_json::to_string_pretty(self).map_err(|e| e.to_string())?;
        // Write atomically so a crash never leaves a truncated device list.
        let temporary = dir.join(format!("{STORE_FILE}.tmp"));
        std::fs::write(&temporary, json).map_err(|e| e.to_string())?;
        std::fs::rename(&temporary, dir.join(STORE_FILE)).map_err(|e| e.to_string())
    }
}

/// Per-user settings directory; `VISICUT_RUST_CONFIG_DIR` overrides it (tests).
pub fn config_dir() -> PathBuf {
    if let Some(dir) = std::env::var_os("VISICUT_RUST_CONFIG_DIR") {
        return dir.into();
    }
    let home = || PathBuf::from(std::env::var_os("HOME").unwrap_or_default());
    if cfg!(target_os = "macos") {
        home().join("Library/Application Support/VisiCutRust")
    } else if cfg!(windows) {
        PathBuf::from(std::env::var_os("APPDATA").unwrap_or_default()).join("VisiCutRust")
    } else {
        std::env::var_os("XDG_CONFIG_HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|| home().join(".config"))
            .join("visicut-rust")
    }
}

fn child<'a>(node: roxmltree::Node<'a, 'a>, name: &str) -> Option<roxmltree::Node<'a, 'a>> {
    node.children().find(|n| n.has_tag_name(name))
}

fn text(node: roxmltree::Node, name: &str) -> Option<String> {
    child(node, name).map(|n| n.text().unwrap_or_default().to_string())
}

fn number(node: roxmltree::Node, name: &str) -> Result<Option<f64>, String> {
    text(node, name)
        .map(|t| {
            t.trim()
                .parse()
                .map_err(|_| tr!("Ungültiger Wert für {name}", name = name))
        })
        .transpose()
}

fn points(node: Option<roxmltree::Node>) -> Result<Vec<[f64; 2]>, String> {
    let Some(node) = node else {
        return Ok(Vec::new());
    };
    node.children()
        .filter(|n| n.is_element())
        // XStream omits fields that are 0.0, e.g. a marker at x = 0.
        .map(|p| {
            Ok([
                number(p, "x")?.unwrap_or(0.0),
                number(p, "y")?.unwrap_or(0.0),
            ])
        })
        .collect()
}

/// Reads a VisiCut `LaserDevice` XML file. Only LTT iLaser 4000 devices with the
/// machine geometry of the Rust driver are accepted.
pub fn from_visicut_xml(source: &str) -> Result<LaserDevice, String> {
    let doc = roxmltree::Document::parse(source).map_err(|e| e.to_string())?;
    let root = doc.root_element();
    if !root.has_tag_name("laserDevice") {
        return Err(tr("Keine VisiCut-Gerätedatei").into());
    }
    let cutter = child(root, "laserCutter").ok_or(tr("Gerätedatei ohne Lasercutter"))?;
    if cutter.attribute("class") != Some(LTT_CLASS) {
        return Err(tr!(
            "Nur der LTT iLaser 4000 wird unterstützt (Treiber: {driver})",
            driver = cutter.attribute("class").unwrap_or(tr("unbekannt"))
        ));
    }
    for (name, expected) in [
        ("bedWidth", 1000.0),
        ("bedHeight", 600.0),
        ("maxDPI", 4000.0),
    ] {
        if let Some(value) = number(cutter, name)?
            && (value - expected).abs() > 0.01
        {
            return Err(tr!(
                "{name} = {value} weicht vom LTT iLaser 4000 ab ({expected}); nicht unterstützt",
                name = name,
                value = value,
                expected = expected
            ));
        }
    }
    let defaults = LaserDevice::default();
    let camera_calibration = match child(root, "cameraHomography") {
        Some(h) => Some(CameraCalibration {
            reference_points: points(child(h, "referencePoints"))?,
            view_points: points(child(h, "viewPoints"))?,
        }),
        None => None,
    };
    let mut machine = MachineSettings::default();
    if let Some(list) = child(cutter, "engraveShiftList") {
        let values = list
            .children()
            .filter(|n| n.is_element())
            .map(|n| {
                n.text()
                    .unwrap_or_default()
                    .trim()
                    .parse::<f32>()
                    .map_err(|_| tr("Ungültiger Wert in engraveShiftList").to_string())
            })
            .collect::<Result<Vec<_>, _>>()?;
        machine.engrave_shift = values
            .try_into()
            .map_err(|_| tr("engraveShiftList braucht zehn Werte (10 % … 100 %)"))?;
    }
    if text(cutter, "useTangentCurves").as_deref() != Some("false")
        && let Some(value) = number(cutter, "tangentCurveMaxAcceleration")?
    {
        machine.curve_acceleration_mm_s2 = value;
    }
    let device = LaserDevice {
        name: text(root, "name")
            .filter(|n| !n.trim().is_empty())
            .unwrap_or(defaults.name),
        description: text(root, "description").unwrap_or_default(),
        hostname: text(cutter, "hostname").unwrap_or(defaults.hostname),
        port: match text(cutter, "port") {
            Some(p) => p.trim().parse().map_err(|_| tr("Ungültiger Port"))?,
            None => defaults.port,
        },
        rotary_axis: text(cutter, "rotaryAxisSupported").as_deref() == Some("true"),
        job_sent_text: text(root, "jobSentText").unwrap_or_default(),
        camera_url: text(root, "cameraURL").unwrap_or_default(),
        camera_calibration,
        origin_bottom_left: text(root, "originBottomLeft").as_deref() == Some("true"),
        machine,
    };
    device.validate()?;
    Ok(device)
}

/// Imports LTT devices from a VisiCut device XML, a `.vcsettings`/GitHub
/// settings archive, or a VisiCutRust device export (JSON).
pub fn import_bytes(bytes: &[u8]) -> Result<Vec<LaserDevice>, String> {
    if bytes.starts_with(b"PK") {
        return import_archive(bytes);
    }
    let source = std::str::from_utf8(bytes).map_err(|_| tr("Datei ist kein Text"))?;
    if source.trim_start().starts_with('{') {
        let store: DeviceStore = serde_json::from_str(source).map_err(|e| e.to_string())?;
        store.validate()?;
        return Ok(store.devices);
    }
    Ok(vec![from_visicut_xml(source)?])
}

fn import_archive(bytes: &[u8]) -> Result<Vec<LaserDevice>, String> {
    let mut archive = zip::ZipArchive::new(Cursor::new(bytes)).map_err(|e| e.to_string())?;
    let mut devices = Vec::new();
    let mut skipped = Vec::new();
    for i in 0..archive.len() {
        let mut file = archive.by_index(i).map_err(|e| e.to_string())?;
        let name = file.name().map_err(|e| e.to_string())?.to_string();
        // Settings archives keep devices in devices/, GitHub adds a top folder.
        let in_devices = name.split('/').rev().nth(1) == Some("devices");
        if !in_devices || !name.ends_with(".xml") || file.size() > 5 * 1024 * 1024 {
            continue;
        }
        let mut source = String::new();
        file.read_to_string(&mut source)
            .map_err(|e| e.to_string())?;
        match from_visicut_xml(&source) {
            Ok(device) => devices.push(device),
            Err(_) => skipped.push(name.rsplit('/').next().unwrap_or(&name).to_string()),
        }
    }
    if devices.is_empty() {
        return Err(if skipped.is_empty() {
            tr("Archiv enthält keine VisiCut-Geräte").into()
        } else {
            tr!(
                "Archiv enthält keinen LTT iLaser 4000 (übersprungen: {skipped})",
                skipped = skipped.join(", ")
            )
        });
    }
    Ok(devices)
}

pub fn export(path: &Path, devices: &[LaserDevice]) -> Result<(), String> {
    let store = DeviceStore {
        format_version: 1,
        devices: devices.to_vec(),
        selected: 0,
    };
    store.validate()?;
    let json = serde_json::to_string_pretty(&store).map_err(|e| e.to_string())?;
    std::fs::write(path, json).map_err(|e| e.to_string())
}

/// Lab settings published for VisiCut (LabSettings.java). Only LTT devices
/// in these archives can be imported.
pub const LAB_SETTINGS: &[(&str, &str)] = &[
    (
        "France, Chemillé en Anjou : FabLab le Boc@l",
        "https://github.com/bocal-chemille/Visicut/raw/master/config_laser_bocal.vcsettings",
    ),
    (
        "France, Le Mans: HAUM Hackerspace",
        "https://github.com/haum/visicut-settings/archive/master.zip",
    ),
    (
        "France, Roche aux Fées: FabLabs La Fabrique",
        "https://github.com/LaFabrique35/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Aachen: FabLab RWTH Aachen",
        "https://github.com/renebohne/zing6030-visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Berlin: Fab Lab Berlin",
        "https://github.com/FabLabBerlin/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Berlin: xHain Hack+Makespace",
        "https://github.com/xHain-hackspace/visicut-settings/raw/refs/heads/main/master.zip",
    ),
    (
        "Germany, Dresden: Konglomerat e.V.",
        "https://github.com/konglomerat/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Dresden: Makerspace Dresden",
        "https://github.com/Makerspace-Dresden/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Erlangen: FAU FabLab",
        "https://github.com/fau-fablab/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Erlangen: ZAM",
        "https://github.com/zam-haus/visicut-settings/archive/refs/heads/main.zip",
    ),
    (
        "Germany, Gunzenhausen: FabLab Altmühlfranken",
        "https://git.fablab-altmuehlfranken.de/fablab/visicut-settings/archive/main.zip",
    ),
    (
        "Germany, Hamburg: Fab Lab Fabulous St. Pauli",
        "https://github.com/Fab-Lab-Fabulous-St-Pauli/visicut-settings/archive/main.zip",
    ),
    (
        "Germany, Heidelberg: Heidelberg Makerspace",
        "https://github.com/heidelberg-makerspace/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Karlsruhe: SPE Innovationswerkstatt",
        "https://github.com/spe-khe/visicut_settings/archive/main.zip",
    ),
    (
        "Germany, Nuremberg: Fab lab Region Nürnberg e.V.",
        "https://github.com/fablabnbg/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Paderborn: FabLab Paderborn e.V.",
        "https://github.com/fablab-paderborn/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Reutlingen: INNOPORT / MakeRTreff",
        "https://github.com/InnoportReutlingen/VisiCut-Settings/archive/main.zip",
    ),
    (
        "Germany, Veitsbronn: FabLab Landkreis Fürth e.V.",
        "https://github.com/falafue/visicut-settings/archive/master.zip",
    ),
    (
        "Germany, Ansbach: FabLab Ansbach e.V.",
        "https://github.com/FabLab-Ansbach/visicut-settings/archive/main.zip",
    ),
    (
        "India, Bangalore: Makerspace BLR",
        "https://github.com/pallavagarwal07/Makerspace-BLR-Visicut-Settings/archive/master.zip",
    ),
    (
        "Netherlands, Amersfoort: FabLab",
        "https://github.com/Fablab-Amersfoort/visicut-settings/archive/master.zip",
    ),
    (
        "Netherlands, Enschede: TkkrLab",
        "https://github.com/TkkrLab/visicut-settings/archive/master.zip",
    ),
    (
        "United Kingdom, Leeds: Hackspace",
        "https://github.com/leedshackspace/visicut-settings/archive/master.zip",
    ),
    (
        "United Kingdom, Manchester: HacMan",
        "https://github.com/HACManchester/visicut-settings/archive/master.zip",
    ),
    (
        "United States, Seattle: Fremont Hangar",
        "https://github.com/hghile/visicut-settings/archive/master.zip",
    ),
    (
        "United States, Seattle: SLU Makerspace",
        "https://github.com/hghile/visicut-settings-slu/archive/master.zip",
    ),
];

pub fn download(url: &str) -> Result<Vec<LaserDevice>, String> {
    import_bytes(&crate::camera::fetch(url, 50 * 1024 * 1024)?)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fau_reference_device_is_imported_with_camera_and_rotary() {
        let device = LaserDevice::fau();
        assert_eq!(device.name, "LTT iLaser 4000");
        assert_eq!(
            (device.hostname.as_str(), device.port),
            ("lasercutter2", 9100)
        );
        assert!(device.rotary_axis);
        assert!(device.job_sent_text.contains("Autofokus"));
        assert_eq!(device.camera_url, "https://marvin.fablab.fau.de/image");
        let calibration = device.camera_calibration.unwrap();
        assert_eq!(calibration.reference_points.len(), 6);
        assert_eq!(calibration.reference_points[0], [200.0, 120.0]);
        assert_eq!(calibration.view_points[1], [1994.0, 1759.0]);
    }

    #[test]
    fn machine_settings_come_from_the_visicut_driver() {
        let device = LaserDevice::fau();
        assert_eq!(device.machine, MachineSettings::default());
        assert_eq!(device.machine.engrave_shift[9], -23.0);
        let xml = include_str!("../reference/FAU-LTT-iLaser-4000.xml")
            .replace("<int>-23</int>", "<int>-11</int>")
            .replace(
                "<tangentCurveMaxAcceleration>2000.0",
                "<tangentCurveMaxAcceleration>981.0",
            );
        let device = from_visicut_xml(&xml).unwrap();
        assert_eq!(device.machine.engrave_shift[9], -11.0);
        assert_eq!(device.machine.curve_acceleration_mm_s2, 981.0);
        let short = xml.replace("<int>-11</int>", "");
        assert!(from_visicut_xml(&short).unwrap_err().contains("zehn Werte"));
        let mut bad = LaserDevice::fau();
        bad.machine.curve_acceleration_mm_s2 = 100.0;
        assert!(bad.validate().unwrap_err().contains("Kurvenbeschleunigung"));
    }

    #[test]
    fn rejects_other_drivers_and_geometry() {
        let xml = include_str!("../reference/FAU-LTT-iLaser-4000.xml");
        let epilog = xml.replace(LTT_CLASS, "de.thomas_oster.liblasercut.drivers.EpilogZing");
        assert!(
            from_visicut_xml(&epilog)
                .unwrap_err()
                .contains("Nur der LTT")
        );
        let wide = xml.replace("<bedWidth>1000.0</bedWidth>", "<bedWidth>1200.0</bedWidth>");
        assert!(from_visicut_xml(&wide).unwrap_err().contains("bedWidth"));
    }

    #[test]
    fn archive_import_keeps_only_ltt_devices() {
        use std::io::Write;
        let xml = include_str!("../reference/FAU-LTT-iLaser-4000.xml");
        let mut bytes = Vec::new();
        {
            let mut zip = zip::ZipWriter::new(Cursor::new(&mut bytes));
            let options = zip::write::SimpleFileOptions::default();
            zip.start_file("visicut-settings-master/devices/LTT.xml", options)
                .unwrap();
            zip.write_all(xml.as_bytes()).unwrap();
            zip.start_file("visicut-settings-master/devices/Epilog.xml", options)
                .unwrap();
            zip.write_all(xml.replace(LTT_CLASS, "Other").as_bytes())
                .unwrap();
            zip.start_file("visicut-settings-master/materials/LTT.xml", options)
                .unwrap();
            zip.write_all(xml.as_bytes()).unwrap();
            zip.finish().unwrap();
        }
        let devices = import_bytes(&bytes).unwrap();
        assert_eq!(devices.len(), 1);
        assert_eq!(devices[0].hostname, "lasercutter2");
    }

    #[test]
    fn store_roundtrip_merge_and_validation() {
        let dir = std::env::temp_dir().join(format!("visicut-devices-{}", std::process::id()));
        let mut store = DeviceStore::load(&dir).unwrap();
        assert_eq!(store.devices.len(), 1);
        assert_eq!(store.merge(vec![LaserDevice::fau()]), 1);
        assert_eq!(store.devices[1].name, "LTT iLaser 4000 (2)");
        store.selected = 1;
        store.save(&dir).unwrap();
        assert_eq!(DeviceStore::load(&dir).unwrap(), store);
        let export = dir.join("export.json");
        export_and_reimport(&export, &store.devices);
        store.devices[1].name = store.devices[0].name.clone();
        assert!(store.save(&dir).unwrap_err().contains("doppelt"));
        std::fs::remove_dir_all(dir).unwrap();
    }

    fn export_and_reimport(path: &Path, devices: &[LaserDevice]) {
        export(path, devices).unwrap();
        assert_eq!(
            import_bytes(&std::fs::read(path).unwrap()).unwrap(),
            devices
        );
    }
}

#[cfg(test)]
mod network_tests {
    /// Needs internet access: `cargo test -- --ignored`.
    #[test]
    #[ignore]
    fn downloads_fau_lab_settings() {
        let devices = super::download(
            super::LAB_SETTINGS
                .iter()
                .find(|l| l.0.contains("FAU"))
                .unwrap()
                .1,
        )
        .unwrap();
        assert!(
            devices.iter().any(|d| d.hostname == "lasercutter2"),
            "{devices:?}"
        );
    }
}
