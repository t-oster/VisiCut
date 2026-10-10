//! Converts EPS and PostScript files to PDF with an installed Ghostscript.
//! Used as fallback for what the built-in interpreter does not support.
use std::io::Read;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

/// Longest a single Ghostscript conversion may take.
const TIMEOUT: Duration = Duration::from_secs(60);

pub const MISSING: &str = "Für EPS/PS-Dateien wird Ghostscript benötigt, es wurde aber nicht gefunden. \
     Bitte Ghostscript installieren (https://ghostscript.com) oder die Datei als SVG oder PDF speichern";

/// Runs `gs -sDEVICE=pdfwrite` on `path` and returns the PDF bytes.
pub fn to_pdf(path: &Path) -> Result<Vec<u8>, String> {
    let gs = find().ok_or(MISSING)?;
    let input = std::path::absolute(path).map_err(|e| e.to_string())?;
    let output = TempFile::new();
    let mut command = Command::new(&gs);
    command
        .args([
            "-q",
            "-dSAFER",
            "-dBATCH",
            "-dNOPAUSE",
            "-sDEVICE=pdfwrite",
            "-dEPSCrop",
            // Text becomes outlines, which is what a laser cutter follows anyway.
            "-dNoOutputFonts",
        ])
        // `%` would start a page-number template in Ghostscript's output name.
        .arg(format!(
            "-sOutputFile={}",
            output.0.display().to_string().replace('%', "%%")
        ))
        .arg(&input)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::piped());
    hide_console(&mut command);
    let mut child = command
        .spawn()
        .map_err(|e| format!("Ghostscript konnte nicht gestartet werden: {e}"))?;
    let mut stderr = child.stderr.take();
    let reader = std::thread::spawn(move || {
        let mut text = String::new();
        if let Some(stderr) = stderr.as_mut() {
            let _ = stderr.read_to_string(&mut text);
        }
        text
    });
    let started = Instant::now();
    let status = loop {
        match child.try_wait() {
            Ok(Some(status)) => break status,
            Ok(None) if started.elapsed() > TIMEOUT => {
                let _ = child.kill();
                let _ = child.wait();
                return Err(
                    "Ghostscript hat die EPS/PS-Datei nicht innerhalb von 60 Sekunden umgewandelt"
                        .into(),
                );
            }
            Ok(None) => std::thread::sleep(Duration::from_millis(20)),
            Err(e) => return Err(format!("Ghostscript ist fehlgeschlagen: {e}")),
        }
    };
    let messages = reader.join().unwrap_or_default();
    let pdf = std::fs::read(&output.0).unwrap_or_default();
    if !status.success() || pdf.is_empty() {
        let lines = || messages.lines().map(str::trim).filter(|l| !l.is_empty());
        let detail = lines()
            .find(|line| line.starts_with("Error:"))
            .or_else(|| lines().next())
            .unwrap_or("keine Ausgabe");
        return Err(format!(
            "Ghostscript konnte die EPS/PS-Datei nicht umwandeln: {detail}"
        ));
    }
    Ok(pdf)
}

/// Finds a Ghostscript executable on `PATH` or in the usual install locations
/// (GUI apps on macOS do not see the Homebrew `PATH`).
pub fn find() -> Option<PathBuf> {
    let names: &[&str] = if cfg!(windows) {
        &["gswin64c.exe", "gswin32c.exe", "gs.exe"]
    } else {
        &["gs"]
    };
    let mut candidates: Vec<PathBuf> = std::env::var_os("PATH")
        .map(|paths| {
            std::env::split_paths(&paths)
                .flat_map(|dir| names.iter().map(move |name| dir.join(name)))
                .collect()
        })
        .unwrap_or_default();
    if cfg!(windows) {
        for variable in ["ProgramFiles", "ProgramFiles(x86)", "ProgramW6432"] {
            let Some(base) = std::env::var_os(variable) else {
                continue;
            };
            // Ghostscript installs to e.g. C:\Program Files\gs\gs10.04.0\bin.
            let mut versions: Vec<PathBuf> = std::fs::read_dir(Path::new(&base).join("gs"))
                .map(|entries| entries.flatten().map(|e| e.path()).collect())
                .unwrap_or_default();
            versions.sort();
            for version in versions.iter().rev() {
                candidates.extend(names.iter().map(|name| version.join("bin").join(name)));
            }
        }
    } else {
        for dir in [
            "/opt/homebrew/bin",
            "/usr/local/bin",
            "/usr/bin",
            "/opt/local/bin",
        ] {
            candidates.push(Path::new(dir).join("gs"));
        }
    }
    candidates.into_iter().find(|path| path.is_file())
}

#[cfg(windows)]
fn hide_console(command: &mut Command) {
    use std::os::windows::process::CommandExt;
    const CREATE_NO_WINDOW: u32 = 0x0800_0000;
    command.creation_flags(CREATE_NO_WINDOW);
}

#[cfg(not(windows))]
fn hide_console(_: &mut Command) {}

/// A temporary output file that is removed when dropped.
struct TempFile(PathBuf);

impl TempFile {
    fn new() -> Self {
        use std::sync::atomic::{AtomicU32, Ordering};
        static COUNTER: AtomicU32 = AtomicU32::new(0);
        let nanos = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.subsec_nanos())
            .unwrap_or_default();
        let path = std::env::temp_dir().join(format!(
            "visicut-ps-{}-{nanos}-{}.pdf",
            std::process::id(),
            COUNTER.fetch_add(1, Ordering::Relaxed)
        ));
        Self(path)
    }
}

impl Drop for TempFile {
    fn drop(&mut self) {
        let _ = std::fs::remove_file(&self.0);
    }
}
