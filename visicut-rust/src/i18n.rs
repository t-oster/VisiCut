//! Sprachen der Oberfläche und der Meldungen. Der deutsche Text ist zugleich der
//! Schlüssel: `tr("Öffnen …")` liefert die Übersetzung der gewählten Sprache
//! oder, wenn sie fehlt, den deutschen Text. Platzhalter wie `{name}` füllt
//! `tr!` nach dem Übersetzen. Die Tabellen liegen in `resources/i18n`.

use std::collections::HashMap;
use std::sync::OnceLock;
use std::sync::atomic::{AtomicU8, Ordering};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Language {
    German,
    English,
    French,
    Italian,
    Dutch,
}

impl Language {
    pub const ALL: [Language; 5] = [
        Self::German,
        Self::English,
        Self::French,
        Self::Italian,
        Self::Dutch,
    ];

    /// Kürzel für Einstellungen und Systemsprache (ISO 639-1).
    pub fn code(self) -> &'static str {
        match self {
            Self::German => "de",
            Self::English => "en",
            Self::French => "fr",
            Self::Italian => "it",
            Self::Dutch => "nl",
        }
    }

    /// Name in der Sprache selbst, damit man sie immer wiederfindet.
    pub fn native_name(self) -> &'static str {
        match self {
            Self::German => "Deutsch",
            Self::English => "English",
            Self::French => "Français",
            Self::Italian => "Italiano",
            Self::Dutch => "Nederlands",
        }
    }

    /// Sprache aus einem Kürzel wie `fr`, `fr-FR` oder `nl_BE`.
    pub fn from_code(code: &str) -> Option<Self> {
        let prefix = code
            .split(['-', '_', '.'])
            .next()
            .unwrap_or_default()
            .to_ascii_lowercase();
        Self::ALL.into_iter().find(|l| l.code() == prefix)
    }

    /// Systemsprache; Englisch, wenn sie nicht unterstützt wird.
    pub fn system() -> Self {
        sys_locale::get_locales()
            .find_map(|locale| Self::from_code(&locale))
            .unwrap_or(Self::English)
    }

    fn table(self) -> Option<&'static HashMap<String, String>> {
        fn load(
            cell: &'static OnceLock<HashMap<String, String>>,
            json: &str,
        ) -> &'static HashMap<String, String> {
            cell.get_or_init(|| {
                serde_json::from_str(json).expect("Übersetzungstabelle ist gültiges JSON")
            })
        }
        static EN: OnceLock<HashMap<String, String>> = OnceLock::new();
        static FR: OnceLock<HashMap<String, String>> = OnceLock::new();
        static IT: OnceLock<HashMap<String, String>> = OnceLock::new();
        static NL: OnceLock<HashMap<String, String>> = OnceLock::new();
        match self {
            Self::German => None,
            Self::English => Some(load(&EN, include_str!("../resources/i18n/en.json"))),
            Self::French => Some(load(&FR, include_str!("../resources/i18n/fr.json"))),
            Self::Italian => Some(load(&IT, include_str!("../resources/i18n/it.json"))),
            Self::Dutch => Some(load(&NL, include_str!("../resources/i18n/nl.json"))),
        }
    }
}

static CURRENT: AtomicU8 = AtomicU8::new(0);

pub fn set_language(language: Language) {
    let index = Language::ALL
        .iter()
        .position(|l| *l == language)
        .unwrap_or(0);
    CURRENT.store(index as u8, Ordering::Relaxed);
}

pub fn language() -> Language {
    Language::ALL[usize::from(CURRENT.load(Ordering::Relaxed)).min(Language::ALL.len() - 1)]
}

/// Übersetzt einen deutschen Text der Oberfläche.
pub fn tr(german: &'static str) -> &'static str {
    language()
        .table()
        .and_then(|table| table.get(german))
        .map_or(german, String::as_str)
}

/// Ob ein gespeicherter Wert ein Standardtext in irgendeiner Sprache ist,
/// z. B. „Eigenes Material“ aus einem Projekt, das auf Englisch angelegt
/// wurde. Gespeicherte Standardwerte werden beim Anlegen übersetzt; Vergleiche
/// müssen deshalb alle Sprachen kennen.
pub fn is_default_text(german: &'static str, value: &str) -> bool {
    value == german
        || Language::ALL
            .iter()
            .filter_map(|l| l.table())
            .any(|table| table.get(german).is_some_and(|t| t == value))
}

/// Übersetzungstabelle der aktuellen Sprache (für die macOS-Oberfläche, die
/// sie über die Brücke holt); leer für Deutsch.
pub fn current_table() -> HashMap<String, String> {
    language().table().cloned().unwrap_or_default()
}

/// Setzt `{name}`-Platzhalter in einen (übersetzten) Text ein.
pub fn fill(template: &str, args: &[(&str, String)]) -> String {
    let mut text = template.to_owned();
    for (name, value) in args {
        text = text.replace(&format!("{{{name}}}"), value);
    }
    text
}

/// `tr!("{count} Objekte", count = n)`: übersetzt und füllt Platzhalter.
#[macro_export]
macro_rules! tr {
    ($german:literal) => {
        $crate::i18n::tr($german)
    };
    ($german:literal, $($name:ident = $value:expr),+ $(,)?) => {
        $crate::i18n::fill(
            $crate::i18n::tr($german),
            &[$((stringify!($name), ($value).to_string())),+],
        )
    };
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::BTreeSet;

    fn sources(dir: &std::path::Path, files: &mut Vec<std::path::PathBuf>) {
        for entry in std::fs::read_dir(dir).unwrap() {
            let path = entry.unwrap().path();
            if path.is_dir() {
                sources(&path, files);
            } else if path.extension().is_some_and(|e| e == "rs" || e == "swift")
                && !path.ends_with("i18n.rs")
            {
                files.push(path);
            }
        }
    }

    /// Alle Texte aus `tr("…")` und `tr!("…"` in den Rust- und Swift-Quellen.
    fn keys() -> BTreeSet<String> {
        let mut keys = BTreeSet::new();
        let mut files = Vec::new();
        sources(
            std::path::Path::new(concat!(env!("CARGO_MANIFEST_DIR"), "/src")),
            &mut files,
        );
        // Die macOS-Oberfläche nutzt dieselben Tabellen (`tr("…")` in Swift).
        sources(
            std::path::Path::new(concat!(env!("CARGO_MANIFEST_DIR"), "/native")),
            &mut files,
        );
        for path in files {
            let source = std::fs::read_to_string(&path).unwrap();
            for marker in ["tr(", "tr!("] {
                for (start, _) in source.match_indices(marker) {
                    // Nur `tr(` selbst, nicht etwa `attr(`.
                    let before = source[..start].chars().next_back();
                    if before.is_some_and(|c| c.is_alphanumeric() || c == '_') {
                        continue;
                    }
                    // rustfmt bricht lange Aufrufe nach der Klammer um.
                    let rest = source[start + marker.len()..].trim_start();
                    if let Some(rest) = rest.strip_prefix('"') {
                        keys.insert(literal(rest));
                    }
                }
            }
        }
        keys
    }

    /// Inhalt eines Rust-Stringliterals bis zum schließenden Anführungszeichen.
    fn literal(source: &str) -> String {
        let mut key = String::new();
        let mut chars = source.chars().peekable();
        while let Some(c) = chars.next() {
            match c {
                '"' => break,
                '\\' => match chars.next() {
                    Some('n') => key.push('\n'),
                    // Zeilenfortsetzung: Umbruch und Einrückung entfallen.
                    Some('\n' | '\r') => {
                        while chars.peek().is_some_and(|c| c.is_whitespace()) {
                            chars.next();
                        }
                    }
                    // Swift-Interpolation `\(…)` gehört nicht in einen Schlüssel.
                    Some('(') => key.push_str("\\("),
                    Some(other) => key.push(other),
                    None => {}
                },
                c => key.push(c),
            }
        }
        key
    }

    fn placeholders(text: &str) -> BTreeSet<String> {
        let mut found = BTreeSet::new();
        let mut rest = text;
        while let Some(start) = rest.find('{') {
            rest = &rest[start + 1..];
            if let Some(end) = rest.find('}') {
                found.insert(rest[..end].to_owned());
                rest = &rest[end + 1..];
            }
        }
        found
    }

    #[test]
    fn every_text_is_translated_with_the_same_placeholders() {
        let keys = keys();
        let interpolated: Vec<_> = keys.iter().filter(|k| k.contains("\\(")).collect();
        assert!(
            interpolated.is_empty(),
            "Platzhalter {{name}} statt Swift-Interpolation: {interpolated:#?}"
        );
        assert!(keys.len() > 100, "zu wenige Texte gefunden: {}", keys.len());
        for language in &Language::ALL[1..] {
            let table = language.table().unwrap();
            let missing: Vec<_> = keys.iter().filter(|k| !table.contains_key(*k)).collect();
            assert!(missing.is_empty(), "{language:?} fehlt: {missing:#?}");
            for key in &keys {
                assert_eq!(
                    placeholders(key),
                    placeholders(&table[key]),
                    "{language:?}: Platzhalter in „{key}“"
                );
            }
            let stale: Vec<_> = table.keys().filter(|k| !keys.contains(*k)).collect();
            assert!(stale.is_empty(), "{language:?} veraltet: {stale:#?}");
        }
    }

    /// Hilfe zum Übersetzen: schreibt je Sprache die fehlenden Texte nach
    /// `target/i18n/missing-<code>.json`, alle Schlüssel nach `keys.json`.
    /// `cargo test --lib i18n::tests::export_missing -- --ignored`
    #[test]
    #[ignore]
    fn export_missing() {
        let keys = keys();
        let dir = concat!(env!("CARGO_MANIFEST_DIR"), "/target/i18n");
        std::fs::create_dir_all(dir).unwrap();
        std::fs::write(
            format!("{dir}/keys.json"),
            serde_json::to_string_pretty(&keys).unwrap(),
        )
        .unwrap();
        for language in &Language::ALL[1..] {
            let table = language.table().unwrap();
            let missing: std::collections::BTreeMap<_, _> = keys
                .iter()
                .filter(|k| !table.contains_key(*k))
                .map(|k| (k.as_str(), ""))
                .collect();
            let path = format!("{dir}/missing-{}.json", language.code());
            std::fs::write(&path, serde_json::to_string_pretty(&missing).unwrap()).unwrap();
        }
    }

    #[test]
    fn language_codes_and_fallback() {
        assert_eq!(Language::from_code("fr-FR"), Some(Language::French));
        assert_eq!(Language::from_code("nl_BE.UTF-8"), Some(Language::Dutch));
        assert_eq!(Language::from_code("es"), None);
        assert_eq!(
            fill(
                "{n} von {total}",
                &[("n", "2".into()), ("total", "3".into())]
            ),
            "2 von 3"
        );
    }
}
