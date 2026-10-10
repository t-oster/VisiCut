//! Stateless C ABI for the native macOS shell. The caller owns requests and
//! must release each response with visicut_free. No Rust state crosses the ABI.
use crate::{
    camera,
    device::{self, DeviceStore, LaserDevice},
    ltt,
    materials::Library,
    project::Project,
    svg,
};
use serde_json::{Value, json};
use std::ffi::{CStr, CString, c_char};

fn get_project(request: &Value) -> Result<Project, String> {
    serde_json::from_value(request["project"].clone()).map_err(|e| e.to_string())
}

fn preview(source: &str) -> Result<Value, String> {
    let rendered = svg::render(source)?;
    let size = resvg::tiny_skia::IntSize::from_wh(
        rendered.image.size[0] as u32,
        rendered.image.size[1] as u32,
    )
    .ok_or("Ungültige Vorschaugröße")?;
    let pixels: Vec<u8> = rendered
        .image
        .pixels
        .iter()
        .flat_map(|pixel| pixel.to_array())
        .collect();
    let pixmap =
        resvg::tiny_skia::Pixmap::from_vec(pixels, size).ok_or("Ungültige Vorschaupixel")?;
    let png = pixmap.encode_png().map_err(|e| e.to_string())?;
    Ok(json!({"width_mm": rendered.width_mm, "height_mm": rendered.height_mm, "png": png}))
}

fn read(path: &str) -> Result<String, String> {
    let meta = std::fs::metadata(path).map_err(|e| e.to_string())?;
    if meta.len() > 25 * 1024 * 1024 {
        return Err("Datei ist größer als 25 MB".into());
    }
    std::fs::read_to_string(path).map_err(|e| e.to_string())
}

fn get<T: serde::de::DeserializeOwned>(request: &Value, key: &str) -> Result<T, String> {
    serde_json::from_value(request[key].clone()).map_err(|e| format!("{key}: {e}"))
}

fn path(request: &Value) -> Result<&str, String> {
    request["path"]
        .as_str()
        .ok_or_else(|| "Dateipfad fehlt".into())
}

fn merge_devices(request: &Value, devices: Vec<LaserDevice>) -> Result<Value, String> {
    let mut store: DeviceStore = get(request, "store")?;
    let imported = store.merge(devices);
    store.save(&device::config_dir())?;
    Ok(json!({"store": store, "imported": imported}))
}

pub(crate) fn execute(request: &Value) -> Result<Value, String> {
    match request["action"].as_str().unwrap_or("") {
        "default" => Ok(json!({"project": Project::default()})),
        "materials" => {
            let (library, custom, error) = match Library::load(&device::config_dir()) {
                Ok((library, custom)) => (library, custom, None),
                Err(e) => (Library::bundled(), false, Some(e)),
            };
            Ok(
                json!({"materials": library.materials, "source": library.source,
                "device": library.device, "custom": custom, "error": error}),
            )
        }
        "save_materials" => {
            get::<Library>(request, "library")?.save(&device::config_dir())?;
            Ok(json!({}))
        }
        "reset_materials" => Ok(json!({"library": Library::reset(&device::config_dir())?})),
        "import_materials" => {
            let mut library: Library = get(request, "library")?;
            let bytes = std::fs::read(path(request)?).map_err(|e| e.to_string())?;
            let imported = library.merge(Library::import(&bytes)?);
            library.save(&device::config_dir())?;
            Ok(json!({"library": library, "imported": imported}))
        }
        "export_materials" => {
            get::<Library>(request, "library")?.export(std::path::Path::new(path(request)?))?;
            Ok(json!({}))
        }
        "mapping" => {
            let project = get_project(request)?;
            let selections = if project.steps.is_empty() {
                Vec::new()
            } else {
                crate::mapping::resolve(&project)?
            };
            let values: Vec<Value> = crate::mapping::values(&project.svg)?
                .into_iter()
                .map(|(attribute, values)| json!({"attribute": attribute,
                    "values": values.into_iter().map(|(value, count)| json!({"value": value, "count": count})).collect::<Vec<_>>()}))
                .collect();
            let predefined: Vec<Value> = crate::mapping::predefined()
                .into_iter()
                .map(|p| json!({"name": p.name, "ignore": p.ignore,
                    "rules": p.rules.into_iter().map(|(operation, filters, rest)| json!({"operation": operation, "filters": filters, "rest": rest})).collect::<Vec<_>>()}))
                .collect();
            Ok(json!({"selections": selections, "values": values, "predefined": predefined}))
        }
        "demo" | "import" => {
            let mut project = get_project(request)?;
            let mut warnings = Vec::new();
            let mut steps = Vec::new();
            project.svg = if request["action"] == "demo" {
                include_str!("../examples/demo.svg").into()
            } else {
                let path = request["path"].as_str().ok_or("Dateipfad fehlt")?;
                let imported = crate::import::read_file(std::path::Path::new(path))?;
                warnings = imported.warnings;
                steps = imported.steps;
                imported.svg
            };
            // Every import replaces the mappings; PLF files bring their own.
            project.steps = steps;
            let image = preview(&project.svg)?;
            project.width_mm = image["width_mm"].as_f64().unwrap() as f32;
            project.height_mm = image["height_mm"].as_f64().unwrap() as f32;
            project.name = if request["action"] == "demo" {
                "Beispiel".into()
            } else {
                std::path::Path::new(request["path"].as_str().unwrap())
                    .file_stem()
                    .unwrap_or_default()
                    .to_string_lossy()
                    .into()
            };
            Ok(
                json!({"project": project, "preview": image, "objects": crate::selection::objects(&project.svg)?, "warnings": warnings}),
            )
        }
        "load" => {
            let source = read(request["path"].as_str().ok_or("Dateipfad fehlt")?)?;
            let project: Project = serde_json::from_str(&source).map_err(|e| e.to_string())?;
            project.validate_document()?;
            let image = if project.svg.is_empty() {
                Value::Null
            } else {
                preview(&project.svg)?
            };
            Ok(
                json!({"project": project, "preview": image, "objects": crate::selection::objects(&project.svg)?}),
            )
        }
        "devices" => {
            // A damaged list must not prevent starting; it is replaced only on save.
            let (store, error) = match DeviceStore::load(&device::config_dir()) {
                Ok(store) => (store, None),
                Err(error) => (DeviceStore::default(), Some(error)),
            };
            Ok(json!({
                "store": store,
                "error": error,
                "labs": device::LAB_SETTINGS.iter().map(|(name, url)| json!({"name": name, "url": url})).collect::<Vec<_>>(),
            }))
        }
        "save_devices" => {
            get::<DeviceStore>(request, "store")?.save(&device::config_dir())?;
            Ok(json!({}))
        }
        "import_devices" => {
            let bytes = std::fs::read(path(request)?).map_err(|e| e.to_string())?;
            merge_devices(request, device::import_bytes(&bytes)?)
        }
        "download_devices" => merge_devices(
            request,
            device::download(request["url"].as_str().ok_or("URL fehlt")?)?,
        ),
        "export_devices" => {
            device::export(
                std::path::Path::new(path(request)?),
                &get::<Vec<LaserDevice>>(request, "devices")?,
            )?;
            Ok(json!({}))
        }
        "homography" => Ok(json!({"matrix": camera::homography(&get(request, "calibration")?)?})),
        "camera_image" => {
            let image = camera::capture(&get(request, "device")?)?;
            Ok(
                json!({"png": camera::encode_png(&image)?, "width": image.width(), "height": image.height()}),
            )
        }
        "camera_background" => {
            let project = get_project(request)?;
            let png = camera::background(
                &get(request, "device")?,
                project.bed_width_mm as f64,
                project.bed_height_mm as f64,
            )?;
            Ok(json!({"png": png}))
        }
        "calibration_page" => {
            let mut project = get_project(request)?;
            let points: Vec<[f64; 2]> = get(request, "reference_points")?;
            if points.iter().any(|p| {
                !(5.0..=project.bed_width_mm as f64 - 5.0).contains(&p[0])
                    || !(5.0..=project.bed_height_mm as f64 - 15.0).contains(&p[1])
            }) {
                return Err("Kalibrierpunkte müssen mindestens 5 mm (unten 15 mm) vom Bettrand entfernt sein".into());
            }
            project.svg = camera::calibration_svg(
                &points,
                project.bed_width_mm as f64,
                project.bed_height_mm as f64,
            );
            project.name = "Kalibrierung".into();
            project.steps.clear();
            project.rotary_axis = false;
            (project.x_mm, project.y_mm) = (0.0, 0.0);
            (project.width_mm, project.height_mm) = (project.bed_width_mm, project.bed_height_mm);
            project.operation = crate::project::Operation::Mark;
            let image = preview(&project.svg)?;
            Ok(
                json!({"project": project, "preview": image, "objects": crate::selection::objects(&project.svg)?}),
            )
        }
        "validate_document" => {
            get_project(request)?.validate_document()?;
            Ok(json!({}))
        }
        "prepare" => {
            let prepared = ltt::prepare(&get_project(request)?)?;
            Ok(
                json!({"jobs": prepared.jobs, "description": prepared.description,
                "estimated_seconds": prepared.estimated_seconds, "preview_png": prepared.preview_png,
                "steps": prepared.steps, "timeline": prepared.timeline,
                "warnings": prepared.warnings}),
            )
        }
        "transmit" => {
            let project = get_project(request)?;
            let prepared = ltt::prepare(&project)?;
            let sent = ltt::transmit_jobs(&project.hostname, project.port, &prepared.jobs)?;
            Ok(
                json!({"description": prepared.description, "sent": sent, "warnings": prepared.warnings}),
            )
        }
        _ => Err("Unbekannte Aktion".into()),
    }
}

/// # Safety
/// `request` must be null or point to a valid NUL-terminated UTF-8 C string
/// for the entire duration of this call. Free the returned pointer exactly once.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn visicut_execute(request: *const c_char) -> *mut c_char {
    let result = std::panic::catch_unwind(|| {
        if request.is_null() {
            return Err("Leere Anfrage".to_string());
        }
        let request = unsafe { CStr::from_ptr(request) }
            .to_str()
            .map_err(|e| e.to_string())?;
        let value: Value = serde_json::from_str(request).map_err(|e| e.to_string())?;
        execute(&value)
    });
    let response = match result {
        Ok(Ok(value)) => json!({"ok": true, "result": value}),
        Ok(Err(error)) => json!({"ok": false, "error": error}),
        Err(_) => json!({"ok": false, "error": "Rust-Verarbeitung unerwartet abgebrochen"}),
    };
    CString::new(response.to_string())
        .expect("JSON cannot contain literal NUL")
        .into_raw()
}

/// # Safety
/// `response` must be null or a live pointer returned by `visicut_execute`,
/// and must not have been freed previously.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn visicut_free(response: *mut c_char) {
    if !response.is_null() {
        drop(unsafe { CString::from_raw(response) });
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn ffi_returns_owned_json_and_reports_errors() {
        let request = CString::new(r#"{"action":"default"}"#).unwrap();
        unsafe {
            let response = visicut_execute(request.as_ptr());
            let value: Value =
                serde_json::from_str(CStr::from_ptr(response).to_str().unwrap()).unwrap();
            assert_eq!(value["result"]["project"]["bed_width_mm"], 1000.0);
            visicut_free(response);
            let response = visicut_execute(std::ptr::null());
            let value: Value =
                serde_json::from_str(CStr::from_ptr(response).to_str().unwrap()).unwrap();
            assert_eq!(value["ok"], false);
            visicut_free(response);
        }
    }
    #[test]
    fn demo_bridge_produces_a_decodable_png() {
        let result = execute(&json!({"action": "demo", "project": Project::default()})).unwrap();
        let png: Vec<u8> = serde_json::from_value(result["preview"]["png"].clone()).unwrap();
        assert!(resvg::tiny_skia::Pixmap::decode_png(&png).is_ok());
        assert_eq!(result["project"]["width_mm"], 100.0);
    }
    #[test]
    fn import_reports_images_that_cannot_be_embedded() {
        let dir =
            std::env::temp_dir().join(format!("visicut-bridge-import-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("art.svg");
        std::fs::write(
            &path,
            r#"<svg xmlns="http://www.w3.org/2000/svg" width="10mm" height="10mm"><rect width="5" height="5"/><image width="5" height="5" href="missing.png"/></svg>"#,
        )
        .unwrap();
        let result =
            execute(&json!({"action": "import", "project": Project::default(), "path": path}));
        std::fs::remove_dir_all(&dir).unwrap();
        let warnings = result.unwrap()["warnings"].clone();
        assert!(warnings[0].as_str().unwrap().contains("missing.png"));
    }
    #[test]
    fn new_artwork_resets_old_assignments_and_returns_object_list() {
        let mut p = Project::default();
        p.steps.push(crate::project::JobStep {
            objects: vec![999],
            ..crate::project::JobStep::new(crate::project::Operation::Cut)
        });
        let result = execute(&json!({"action": "demo", "project": p})).unwrap();
        assert_eq!(result["objects"].as_array().unwrap().len(), 3);
        assert!(result["project"]["steps"].as_array().unwrap().is_empty());
    }

    #[test]
    fn device_actions_persist_import_export_and_calibration_page() {
        let dir = std::env::temp_dir().join(format!("visicut-bridge-{}", std::process::id()));
        // Only this test reads the configuration directory.
        unsafe { std::env::set_var("VISICUT_RUST_CONFIG_DIR", &dir) };
        let listed = execute(&json!({"action": "devices"})).unwrap();
        assert_eq!(
            listed["store"]["devices"][0]["camera_url"],
            "https://marvin.fablab.fau.de/image"
        );
        assert!(listed["labs"].as_array().unwrap().len() > 20);
        let export = dir.join("devices.vcrdevices");
        std::fs::create_dir_all(&dir).unwrap();
        execute(&json!({"action": "export_devices", "path": export, "devices": listed["store"]["devices"]}))
            .unwrap();
        let merged =
            execute(&json!({"action": "import_devices", "path": export, "store": listed["store"]}))
                .unwrap();
        assert_eq!(merged["imported"], 1);
        assert_eq!(merged["store"]["devices"][1]["name"], "LTT iLaser 4000 (2)");
        let reloaded = execute(&json!({"action": "devices"})).unwrap();
        assert_eq!(reloaded["store"], merged["store"]);
        let mut broken = merged["store"].clone();
        broken["selected"] = json!(5);
        assert!(execute(&json!({"action": "save_devices", "store": broken})).is_err());
        std::fs::remove_dir_all(&dir).unwrap();

        let page = execute(
            &json!({"action": "calibration_page", "project": Project::default(),
            "reference_points": [[200.0, 120.0], [800.0, 480.0], [800.0, 120.0], [200.0, 480.0]]}),
        )
        .unwrap();
        let project: Project = serde_json::from_value(page["project"].clone()).unwrap();
        assert_eq!((project.width_mm, project.height_mm), (1000.0, 600.0));
        assert!(ltt::prepare(&project).is_ok());
        assert!(
            execute(
                &json!({"action": "calibration_page", "project": Project::default(),
            "reference_points": [[2.0, 120.0]]})
            )
            .is_err()
        );
    }

    #[test]
    fn catalog_contains_fau_acrylic_3mm_cut_profile() {
        let catalog = execute(&json!({"action": "materials"})).unwrap();
        let acrylic = catalog["materials"]
            .as_array()
            .unwrap()
            .iter()
            .find(|m| m["name"] == "Acryl")
            .unwrap();
        let profile = acrylic["profiles"]
            .as_array()
            .unwrap()
            .iter()
            .find(|p| p["thickness_mm"] == 3.0 && p["operation"] == "Cut")
            .unwrap();
        assert_eq!(profile["power_percent"], 100.0);
        assert_eq!(profile["speed_percent"], 9.0);
    }
}
