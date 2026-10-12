//! Editable material library (VisiCut's material and laser profile
//! editors). The bundled FAU catalogue is used until the user saves own
//! changes to the settings directory.
use crate::project::Operation;
use crate::{i18n::tr, tr};
use serde::{Deserialize, Serialize};
use std::path::Path;

const FILE: &str = "materials.json";

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct MaterialProfile {
    pub thickness_mm: f32,
    pub operation: Operation,
    pub power_percent: f32,
    pub speed_percent: f32,
    /// Origin of imported profiles, e.g. a FAU settings file.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct Material {
    pub id: String,
    pub name: String,
    pub profiles: Vec<MaterialProfile>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct Library {
    pub source: String,
    pub device: String,
    pub materials: Vec<Material>,
}

impl Library {
    pub fn bundled() -> Self {
        serde_json::from_str(include_str!("../resources/materials.json"))
            .expect("bundled material catalogue must parse")
    }

    /// FAU profiles of the other lasercutters (Epilog Zing), selectable as
    /// profile set; read-only and measured on that device, not on the LTT.
    pub fn other_devices() -> Vec<Self> {
        vec![
            serde_json::from_str(include_str!("../resources/profiles/epilog-zing.json"))
                .expect("bundled Epilog catalogue must parse"),
        ]
    }

    /// The user's library, or the bundled one; `true` if it is customised.
    pub fn load(dir: &Path) -> Result<(Self, bool), String> {
        let path = dir.join(FILE);
        if !path.exists() {
            return Ok((Self::bundled(), false));
        }
        let source = std::fs::read_to_string(&path).map_err(|e| e.to_string())?;
        let library: Self = serde_json::from_str(&source).map_err(|e| {
            tr!(
                "Materialbibliothek {path} ist beschädigt: {error}",
                path = path.display(),
                error = e
            )
        })?;
        library.validate()?;
        Ok((library, true))
    }

    pub fn save(&self, dir: &Path) -> Result<(), String> {
        self.validate()?;
        std::fs::create_dir_all(dir).map_err(|e| e.to_string())?;
        let temporary = dir.join(format!("{FILE}.tmp"));
        let json = serde_json::to_string_pretty(self).map_err(|e| e.to_string())?;
        std::fs::write(&temporary, json).map_err(|e| e.to_string())?;
        std::fs::rename(&temporary, dir.join(FILE)).map_err(|e| e.to_string())
    }

    /// Back to the bundled FAU catalogue.
    pub fn reset(dir: &Path) -> Result<Self, String> {
        let path = dir.join(FILE);
        if path.exists() {
            std::fs::remove_file(path).map_err(|e| e.to_string())?;
        }
        Ok(Self::bundled())
    }

    pub fn validate(&self) -> Result<(), String> {
        if self.materials.len() > 10_000 {
            return Err(tr("Zu viele Materialien").into());
        }
        for (i, material) in self.materials.iter().enumerate() {
            if material.name.trim().is_empty() {
                return Err(tr("Materialname fehlt").into());
            }
            if self.materials[..i].iter().any(|m| m.name == material.name) {
                return Err(tr!(
                    "Material „{name}“ ist doppelt vorhanden",
                    name = material.name
                ));
            }
            for (j, p) in material.profiles.iter().enumerate() {
                let label = format!("{}, {} mm", material.name, p.thickness_mm);
                if !p.thickness_mm.is_finite() || !(0.0..=1000.0).contains(&p.thickness_mm) {
                    return Err(tr!(
                        "{label}: Stärke muss zwischen 0 und 1000 mm liegen",
                        label = label
                    ));
                }
                if !p.power_percent.is_finite() || !(0.0..=100.0).contains(&p.power_percent) {
                    return Err(tr!(
                        "{label}: Leistung muss zwischen 0 und 100 % liegen",
                        label = label
                    ));
                }
                if !p.speed_percent.is_finite() || !(0.0..=100.0).contains(&p.speed_percent) {
                    return Err(tr!(
                        "{label}: Geschwindigkeit muss zwischen 0 und 100 % liegen",
                        label = label
                    ));
                }
                if material.profiles[..j].iter().any(|q| {
                    q.operation == p.operation && (q.thickness_mm - p.thickness_mm).abs() < 1e-4
                }) {
                    return Err(tr!(
                        "{label}: Profil für dieses Verfahren doppelt vorhanden",
                        label = label
                    ));
                }
            }
        }
        Ok(())
    }

    /// Adds or replaces materials by name.
    pub fn merge(&mut self, other: Library) -> usize {
        let count = other.materials.len();
        for material in other.materials {
            match self.materials.iter_mut().find(|m| m.name == material.name) {
                Some(existing) => *existing = material,
                None => self.materials.push(material),
            }
        }
        self.materials.sort_by_key(|m| m.name.to_lowercase());
        count
    }

    pub fn import(bytes: &[u8]) -> Result<Self, String> {
        let library: Self = serde_json::from_slice(bytes).map_err(|e| e.to_string())?;
        library.validate()?;
        Ok(library)
    }

    pub fn export(&self, path: &Path) -> Result<(), String> {
        self.validate()?;
        let json = serde_json::to_string_pretty(self).map_err(|e| e.to_string())?;
        std::fs::write(path, json).map_err(|e| e.to_string())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn other_device_catalogues_are_valid() {
        let others = Library::other_devices();
        assert_eq!(others[0].device, "Epilog Zing");
        for library in &others {
            library.validate().unwrap();
            assert!(library.materials.len() > 30);
            assert!(library.materials.iter().any(|m| m.name == "Acryl"));
        }
    }

    #[test]
    fn bundled_catalogue_includes_mark_and_3d_profiles() {
        let library = Library::bundled();
        library.validate().unwrap();
        let profiles = library.materials.iter().flat_map(|m| &m.profiles);
        assert!(profiles.clone().any(|p| p.operation == Operation::Mark));
        assert!(
            profiles
                .clone()
                .any(|p| p.operation == Operation::Engrave3d)
        );
        assert!(profiles.count() >= 115);
    }

    #[test]
    fn save_load_reset_merge_and_validation() {
        let dir = std::env::temp_dir().join(format!("visicut-materials-{}", std::process::id()));
        let (mut library, custom) = Library::load(&dir).unwrap();
        assert!(!custom);
        library.materials[0].profiles[0].power_percent = 42.0;
        library.save(&dir).unwrap();
        let (loaded, custom) = Library::load(&dir).unwrap();
        assert!(custom && loaded == library);
        let path = dir.join("export.json");
        let mut own = Library {
            materials: vec![library.materials[0].clone()],
            ..library.clone()
        };
        own.materials[0].name = "Eigenes Acryl".into();
        own.export(&path).unwrap();
        let count = library.materials.len();
        assert_eq!(
            library.merge(Library::import(&std::fs::read(&path).unwrap()).unwrap()),
            1
        );
        assert_eq!(library.materials.len(), count + 1);
        let mut duplicate = library.clone();
        let profile = duplicate.materials[0].profiles[0].clone();
        duplicate.materials[0].profiles.push(profile);
        assert!(duplicate.save(&dir).unwrap_err().contains("doppelt"));
        assert_eq!(Library::reset(&dir).unwrap(), Library::bundled());
        assert!(!Library::load(&dir).unwrap().1);
        std::fs::remove_dir_all(dir).unwrap();
    }
}
