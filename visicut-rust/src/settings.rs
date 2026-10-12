//! Einstellungen der egui-Oberfläche: Sprache und zuletzt geöffnete Dateien.
//! Sie liegen neben der Geräteliste im Einstellungsordner.

use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};
use visicut_core::i18n::Language;

const FILE: &str = "app-settings.json";
/// Länge der Liste „Zuletzt geöffnet“.
pub const RECENT_MAX: usize = 10;

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct AppSettings {
    /// Sprachkürzel (`de`, `en`, …); fehlt es, gilt die Systemsprache.
    pub language: Option<String>,
    /// Zuletzt geöffnete Dateien, neueste zuerst.
    pub recent: Vec<PathBuf>,
}

impl AppSettings {
    /// Liest die Einstellungen; fehlt die Datei oder ist sie defekt, gelten
    /// die Standardwerte.
    pub fn load() -> Self {
        std::fs::read(visicut_core::device::config_dir().join(FILE))
            .ok()
            .and_then(|bytes| serde_json::from_slice(&bytes).ok())
            .unwrap_or_default()
    }

    pub fn save(&self) -> Result<(), String> {
        let dir = visicut_core::device::config_dir();
        std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
        let bytes = serde_json::to_vec_pretty(self).map_err(|e| e.to_string())?;
        crate::workspace::write_atomic(&dir.join(FILE), &bytes)
    }

    /// Gewählte Sprache oder die Systemsprache.
    pub fn language(&self) -> Language {
        self.language
            .as_deref()
            .and_then(Language::from_code)
            .unwrap_or_else(Language::system)
    }

    /// Setzt eine Datei an den Anfang der Liste (ohne Doppelte).
    pub fn remember(&mut self, path: &Path) {
        let path = std::fs::canonicalize(path).unwrap_or_else(|_| path.to_owned());
        let path = strip_verbatim(path);
        self.recent.retain(|p| p != &path);
        self.recent.insert(0, path);
        self.recent.truncate(RECENT_MAX);
    }
}

/// Windows liefert kanonische Pfade als `\\?\C:\…`; für Anzeige und
/// Dateidialoge die gewohnte Form.
fn strip_verbatim(path: PathBuf) -> PathBuf {
    match path.to_str().and_then(|p| p.strip_prefix(r"\\?\")) {
        Some(rest) if !rest.starts_with("UNC") => PathBuf::from(rest),
        _ => path,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn recent_files_are_unique_newest_first_and_limited() {
        let mut settings = AppSettings::default();
        for i in 0..12 {
            settings.remember(Path::new(&format!("datei-{i}.svg")));
        }
        settings.remember(Path::new("datei-5.svg"));
        assert_eq!(settings.recent.len(), RECENT_MAX);
        assert!(settings.recent[0].ends_with("datei-5.svg"));
        assert_eq!(
            settings
                .recent
                .iter()
                .filter(|p| p.ends_with("datei-5.svg"))
                .count(),
            1
        );
    }

    #[test]
    fn missing_fields_use_defaults() {
        let settings: AppSettings = serde_json::from_str(r#"{"language":"fr"}"#).unwrap();
        assert_eq!(settings.language(), Language::French);
        assert!(settings.recent.is_empty());
    }
}
