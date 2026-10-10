# VisiCutRust für Windows, Linux und macOS

Native macOS-Oberfläche mit AppKit und SwiftUI, SVG-Verarbeitung und
LTT-Treiber in Rust. Der Java-Quellbaum bleibt separat.
Erster Ziel-Lasercutter: LTT iLaser 4000 des FAU FabLabs.

## Starten

Die lokal gebaute App befindet sich in `dist/VisiCutRust.app`.
Version 0.3.0 benötigt macOS 14 oder neuer, weder Java noch Maven.
Die macOS-App wird nur für Apple Silicon
gebaut und veröffentlicht; Intel-Macs werden nicht unterstützt.

Für einen Build aus dem Quellcode werden Rust 1.90 und die Xcode Command
Line Tools benötigt:

```sh
cd visicut-rust
bash scripts/bundle-macos.sh
open 'dist/VisiCutRust.app'
```

Das mitgelieferte `rust-toolchain.toml` wählt Rust 1.90.

```sh
cargo test --lib --locked
```

Das Bundle ist lokal ad hoc signiert, nicht für externe Verteilung
notarisiert.

## Windows, Linux und GitHub Actions

Der Rust-Kern und die egui-Entwicklungsoberfläche bauen auch auf Windows und
Linux. Die vollständige AppKit-/SwiftUI-Oberfläche mit Einzelzuordnung und
Zeitslider ist macOS-spezifisch. Windows/Linux verwenden den gemeinsamen
SVG-/LTT-Kern und können gespeicherte `.vcr`-Projekte öffnen, haben aber noch
nicht dieselben Bedienfunktionen.

Der Workflow **VisiCutRust releases** baut drei native Ziele auf GitHub-gehosteten Runnern:

| Ziel | Oberfläche | Download-Artefakt |
| --- | --- | --- |
| Windows x64 / MSVC | egui | ZIP mit EXE |
| Linux x64 / Ubuntu 24.04 | egui, X11/Wayland | tar.gz mit ausführbarer Datei |
| macOS Apple Silicon | native AppKit/SwiftUI | ZIP mit `.app` |

Bei Pull Requests und Änderungen an `master`/`main` laufen Rust-Tests auf
allen drei Zielen. Linux prüft zusätzlich Formatierung und Clippy; das
Mac-Ziel baut und signiert das Bundle und testet die native Oberfläche
einschließlich Zeitslider. TCP-Tests verwenden ausschließlich localhost.
Die Downloads und Mac-Testbilder liegen unter **Actions → VisiCutRust releases →
Artifacts** (14 Tage Aufbewahrung). Jeder Push auf `master` veröffentlicht
nach erfolgreichen Builds auf allen drei Zielen automatisch eine
[GitHub Release](https://github.com/dbambus/VisiCutRust/releases) mit drei
Archiven und `SHA256SUMS.txt`, getaggt als `v<Cargo-Version>-build.<Laufnummer>`
am gebauten Commit. Tags im Format `v<Version>` veröffentlichen ebenfalls;
dort müssen Tag und Cargo-Version übereinstimmen. Wird während eines Laufs
erneut gepusht, entfällt das Release des älteren Laufs.
Der macOS-Download heißt
`VisiCutRust-<Version>-aarch64-apple-darwin.zip`.
Versionen unter 1.0 sowie Versionen mit Suffix werden als Vorabversion markiert.
Ein manueller Lauf auf einem Branch baut nur Artefakte; auf einem vorhandenen
Versionstag kann er die Veröffentlichung wiederholen.
Weitere Workflows gibt es nicht: Die alten Java-Distributionsbuilds und der
Auslöser für den externen VisiCut-Buildserver wurden entfernt.

Für lokale portable Builds aus `visicut-rust`:

```sh
cargo build --locked --release --bin visicut-rust
cargo test --locked --all-targets
```

Linux benötigt eine grafische Sitzung, OpenGL/EGL, libxkbcommon und einen
Desktop-Portal-Dienst für Dateidialoge. Der CI-Build setzt Ubuntu 24.04 oder
eine kompatible glibc-Laufzeit voraus. Die Pakete enthalten Hinweise zum Start
und zum Funktionsumfang. `scripts/package-ci.py` erstellt dieselben Archive
lokal nach einem Build für die jeweilige Host-Architektur.


## Workflow

1. Eine SVG über **Öffnen** (⌘O) importieren oder **Beispiel** wählen.
   Verknüpfte Bilddateien (PNG, JPEG, GIF, WebP, SVG) werden dabei relativ zur
   SVG geladen und in das Projekt eingebettet; Webadressen werden nicht
   abgerufen, fehlende Bilder werden als Hinweis gemeldet.
   Rasterbilder (PNG, JPEG, BMP, GIF) erhalten ihre Größe aus der
   Auflösung in der Datei, sonst 72 DPI wie in VisiCut; G-Code (`.nc`,
   `.gcode`) wird als roter Pfad an seiner Position in mm übernommen.
   **PDF** wird ohne Zusatzprogramme direkt gelesen (Seite 1; bei mehreren
   Seiten erscheint ein Hinweis). Pfade bleiben Vektoren mit ihren Füll- und
   Linienfarben und Linienstärken, Text wird als Glyphenkontur übernommen,
   Rasterbilder werden eingebettet; die Größe stammt aus der Crop-/MediaBox
   (1 pt = 25,4/72 mm). Haarlinien (Linienstärke 0) erhalten 0,1 mm.
   Verschlüsselte PDFs werden mit Hinweis abgelehnt. **EPS/PS** wird ohne
   Zusatzprogramm von einem eingebauten PostScript-Interpreter gelesen (Pfade,
   Farben, Linienstärken, Prozeduren und Schleifen; die Größe folgt der
   BoundingBox). Text, Schriften, Bilder und andere Operatoren, die er nicht
   kennt, übergibt er an ein installiertes Ghostscript (`gs`, unter Windows
   `gswin64c`). Ohne Ghostscript erscheint ein Hinweis mit dem betroffenen
   Operator; dann hilft nur, die Datei als SVG oder PDF zu speichern.
   DXF-Dateien (ASCII oder binär) werden in Millimetern übernommen
   (`$INSUNITS`, ohne Angabe Millimeter); DXF-Ebenen und -Farben bleiben als
   Ebenen und Linienfarben für die Zuordnung erhalten.
2. Motiv positionieren, skalieren oder auf dem Arbeitsbett zentrieren.
3. **Material** und **Stärke** aus den nativen Auswahlmenüs wählen.
   Die Bibliothek enthält 31 Materialien und 115 Schnitt-, Gravur-, Markier-
   und 3D-Profile aus den öffentlichen FAU-LTT-Einstellungen.
   **Job → Materialbibliothek …** (⇧⌘M) bearbeitet Materialien und Profile,
   importiert/exportiert sie als JSON und stellt die FAU-Bibliothek wieder her;
   eigene Änderungen liegen im Einstellungsordner (siehe unten). **Profil übernehmen**
   setzt Leistung und Geschwindigkeit für die gewählte Kombination.
   Beide Werte werden in Prozent angezeigt und eingegeben. Geschwindigkeit
   wird direkt als LTT-Prozentwert übertragen (zulässig: 0,1–100 %).
   Bei fehlendem oder deaktiviertem Profil bleiben die Parameter manuell.
   **Eigenes Material …** erlaubt eine eigene Bezeichnung und Stärke.
4. Unter **Objektzuordnung** zwischen **Gesamt**, **Einzeln** und **Regeln**
   wählen. **Einzeln**: jedem SVG-Objekt **Schneiden** (rot), **Gravieren**
   (blau), **3D-Gravur** (orange), **Markieren** (violett) oder **Ignorieren**
   zuweisen. **Regeln**: Schritte wählen Objekte nach Farbe, Linien- oder
   Füllfarbe, Linienstärke, Gruppe/Ebene, Typ oder ID; ein Restschritt nimmt
   alles Übrige, Ignorierregeln schließen Objekte aus. Vorlagen enthalten die
   FAU-Zuordnungen („rot schneiden, grün markieren, blau ignorieren, Rest
   gravieren“). Die Wertliste neben jeder Bedingung zeigt die Werte der SVG.
   Die Liste zeigt
   Objektnummer, SVG-ID bzw. Bezeichnung und direkt gesetzte Farbe.
   Gruppen bleiben mit ihren Transformationen erhalten; Text, Bilder und
   SVG-Instanzen (`use`) zählen jeweils als ein Objekt.
   Ohne Einzelzuordnung gilt das gewählte Verfahren für das gesamte Motiv.
5. Leistung, Geschwindigkeit und Durchgänge je Schritt einstellen; **Weiterer
   Parametersatz** bearbeitet dieselben Objekte anschließend erneut mit anderen
   Werten. Vorhandene FAU-Profile lassen sich übernehmen. **Markieren** fährt
   Konturen mit eigenen Parametern ab; ohne Markierprofil startet die Leistung
   bei 0 % und muss vor der Vorbereitung eingestellt werden.
   **Schneiden** bearbeitet Pfadkonturen als Tangentialkurven und Kreisbefehle
   wie der Java-Treiber, innere Konturen vor den äußeren. **Gravieren**
   rastert mit 500 DPI und wählbarem Verfahren (Standard „Halbton
   aufgehellt“ wie im FAU-Profil, außerdem
   Floyd-Steinberg, Halbton, Geordnet, Mittelwert, Raster, Zufall, Schwellwert),
   Helligkeit, Invertierung, bidirektional oder einseitig und von oben oder
   unten. **3D-Gravur** steuert die Leistung je Pixel nach der Helligkeit.
   Text wird beim Schneiden und Markieren entlang der Glyphenumrisse
   bearbeitet (wie in Java). Schriften kommen vom System; fehlen sie, greifen
   die mit egui gelieferten Schriften Ubuntu Light und Hack (auch für
   Vorschau und Gravur). Zeichen ohne passende Schrift werden gemeldet statt
   verworfen.
6. **Vorschau & Zeit** (⇧⌘P) berechnet den Auftrag im Hintergrund. Die Vorschau
   zeigt tatsächliche Schnittkonturen rot und Gravurflächen blau; ignorierte
   Objekte fehlen. Markierkonturen erscheinen violett. Der **Zeitslider**
   zeigt den Ablauf mit Laserkopfposition, Leerfahrten, Rasterzeilen und
   Durchgängen. **Abspielen/Pause**, Anfang/Ende und **1× bis 100×** erlauben
   die Simulation. Beim Verschieben pausiert die Wiedergabe. Bereits
   bearbeitete Bereiche sind kräftig, geplante Bereiche blass dargestellt.
   Angezeigt werden die geschätzte Gesamtdauer, Dauer und
   Parameter je Schritt sowie die Dateigröße. Änderungen verwerfen die alte
   Vorschau und erfordern eine neue Berechnung.
7. Im Vorschaudialog **An Lasercutter senden …** wählen oder LTT exportieren.
   Pro aktivem Verfahren entsteht **ein eigener Auftrag**, maximal vier:
   **Engrav_… → Eng3D_… → Mark_… → Cut_…**. Diese Präfixe stehen auch im Gerätenamen
   (maximal 15 ASCII-Zeichen). Jeder Auftrag erhält eine eigene TCP-Verbindung
   und muss am Gerät separat gestartet werden. Es gibt keinen Autostart.
   Bei einem Fehler wird gestoppt und die App nennt bereits übertragene
   Aufträge; es gibt keine automatische Wiederholung. Beim Export mehrerer
   Aufträge wird ein Zielordner für die einzelnen `.ltt`-Dateien gewählt.
8. Mit ⌘S das `.vcr`-Projekt inklusive SVG, Objektzuordnungen und aller
   Parametersätze speichern.

Zum Ausprobieren im Beispiel **Rahmen → Schneiden**, **Gravurkreis → Gravieren**
und **Dreieck → Markieren** wählen; Markierparameter passend zum Material setzen.

Die Dauer ist eine Schätzung aus den erzeugten, quantisierten Fahrwegen,
Geschwindigkeit und Durchgängen. Bei Gravur zählen Rasterzeilen, Overscan,
Rückfahrten und 0,1 s Zusatzzeit je Zeile dazu. Nominalwerte sind 338,677 mm/s
für Schnitt und der Faktor 6,4 für Gravur; Beschleunigung (2000 mm/s²) und
Kurvenplanung folgen dem Java-Treiber. Geräteeinstellungen,
Übertragung und Bedienzeiten sind nicht vollständig modelliert; die Schätzung
ist noch nicht am Gerät kalibriert. Die Simulation ist eine Vorschau ohne
Geräteverbindung und keine Live-Anzeige eines laufenden Lasers.

Standard-Hostname ist `lasercutter2`, Port `9100`. Der Mac muss den Namen im
FabLab-Netz auflösen können; alternativ eine vom FabLab bestätigte IP
eintragen. Die Übertragung erfordert eine ausdrückliche Bestätigung in
der App und startet den Laser nicht automatisch.

Das Projektformat ist ein versioniertes JSON mit eingebetteter SVG.
Material, Stärke, Verfahren, Objektzuordnungen und Parameter werden mitgespeichert.
Neue Projekte verwenden Formatversion 2 mit `speed_percent`.
Projekte aus Version 0.1/0.2 mit `speed_mm_s` werden beim Öffnen entsprechend
dem gespeicherten Verfahren in Prozent umgerechnet. Beim nächsten Sichern
wird das neue Format verwendet; die ursprüngliche Datei wird beim Öffnen
nicht verändert.
Entwürfe können auch ohne Motiv oder mit einer noch unpassenden Position
gesichert werden; das Senden prüft strengere Bedingungen.
VisiCut-Projekte (`.plf`) werden importiert: Alle Teile erscheinen als
benannte Gruppen an ihrer gespeicherten Position und Größe auf dem
Arbeitsbett. Zuordnungen und Laser-Einstellungen der PLF-Datei werden nicht
übernommen (Hinweis beim Öffnen); sie werden in VisiCutRust neu gewählt.
Parametrische SVG (`.parametric.svg`, `.psvg`) werden mit den Standardwerten
ihrer Parameter bzw. den in der PLF gespeicherten Werten erzeugt; der Hinweis
nennt die verwendeten Werte. LaserScript-Dateien (`.ls`) laufen in einer
abgeschotteten JavaScript-Umgebung ohne Datei- und Netzzugriff mit
Zeitlimit (5 s); `prompt`-Abfragen erhalten ihren Vorgabewert.

## Lasercutter, Drehachse und Kamera

**VisiCutRust → Lasercutter …** (⌘,) verwaltet mehrere LTT iLaser 4000:
Name, Hostname, Port, Drehachse, Kamera-URL, Kamerakalibrierung und den
Hinweis nach dem Senden. Andere Lasercutter-Typen werden nicht unterstützt.
Beim ersten Start ist das FAU-Gerät aus den VisiCut-Einstellungen eingerichtet.
Die Auswahl im Inspektor bestimmt Ziel und verfügbare Funktionen.

- **Importieren** liest VisiCut-Gerätedateien (`devices/*.xml`),
  `.vcsettings`-Archive und eigene Exporte (`.vcrdevices`). **Herunterladen**
  lädt die Labor-Einstellungen aus VisiCuts Liste und übernimmt nur
  LTT-iLaser-4000-Geräte. **Exportieren** schreibt alle Geräte als JSON.
- Die Geräteliste liegt unter `~/Library/Application Support/VisiCutRust`
  (Windows `%APPDATA%\VisiCutRust`, Linux `~/.config/visicut-rust`);
  `VISICUT_RUST_CONFIG_DIR` setzt einen anderen Ordner.
- **Drehachse**: Bei Geräten mit Drehachse lässt sie sich je Projekt mit
  Werkstückdurchmesser einschalten (siehe [PROTOCOL.md](PROTOCOL.md)).
- **Autofokus, Druckluft, Absaugung** steuert der LTT-Treiber nicht – auch
  nicht im Java-Original. Der Gerätehinweis erinnert nach dem Senden daran.
- **Kamera** (⌘K): Das Foto der Kamera-URL wird über die Homographie der
  Kalibrierung entzerrt und unter das Arbeitsbett gelegt (⇧⌘K aktualisiert).
  Das FAU-Kamerabild ist nur im FAU-Netz erreichbar.
- **Kalibrieren**: Im Kalibrierdialog die Kalibrierseite als Projekt öffnen
  und auf Restmaterial markieren (je Marker ein 10-mm-Kreuz mit
  Zählstrichen wie in VisiCut). Danach ein Foto aufnehmen, die nummerierten
  Marker auf die Kreuze ziehen und übernehmen. Mindestens vier Punkte, nicht
  auf einer Linie.
- **Kameraserver**: `visicut-cameraserver` ersetzt `tools/cameraserver` und
  beantwortet jede HTTP-Anfrage mit einem frischen Foto eines
  Aufnahmebefehls, z. B.
  `visicut-cameraserver -- gphoto2 --capture-image-and-download --stdout` oder
  `visicut-cameraserver --output snap.jpg -- imagesnap -q snap.jpg`
  (`--port`, `--rotate 90`). Er liegt in `VisiCutRust.app/Contents/MacOS`
  bzw. neben der Windows-/Linux-Programmdatei.

Windows und Linux bieten Geräteauswahl, -verwaltung, Drehachse, Kamerabild,
Kalibrierung, Materialbibliothek, Regel-Zuordnung mit Vorlagen,
Parametersätze, 3D-Gravur und Rasteroptionen in der egui-Oberfläche.
Die native Menüleiste bietet Ablage, Bearbeiten, Darstellung und Job.
Die Oberfläche verwendet Systemschrift, Systemfarben, native Werkzeugleiste,
Systemdialoge und automatische Hell-/Dunkel-Darstellung.

## Entwicklung und UI-Test

`src/lib.rs` baut den Rust-Kern als statische Bibliothek.
`native/VisiCutApp.swift` ist die native Oberfläche; `native/JobSimulation.swift`
implementiert Zeitslider und Wiedergabe. Die zustandslose
C-Schnittstelle in `src/bridge.rs` transportiert JSON und gibt jede
Antwort mit `visicut_free` wieder frei. Das Bundle enthält den Rust-Kern;
es werden keine zusätzlichen Runtime-Dateien benötigt.

Der frühere egui-Prototyp bleibt für Entwicklung mit `cargo run` verfügbar;
er ist nicht die Oberfläche des ausgelieferten App-Bundles.

```sh
'dist/VisiCutRust.app/Contents/MacOS/visicut-rust' --ui-test
```

Der UI-Test prüft den nativen Material-Control über Accessibility,
Material-/Stärken-/Verfahrenswechsel, Profilübernahme, Skalierung,
natives Speichern mit Rust-Laden und LTT-Export. Außerdem prüft er gemischte
Objektzuordnungen, getrennte Parameter, Vorschau, Zeitschätzung, erneutes Laden
und die automatische Ungültigkeit alter Vorschauen. Abschließend öffnet er
die Auftragsvorschau über die reguläre asynchrone Vorbereitung. Der Test
bewegt den echten nativen Slider vorwärts/rückwärts und prüft Interpolation,
Durchgangswechsel, Ende der Wiedergabe sowie drei getrennte Aufträge.
Er prüft außerdem Regel-Zuordnung mit Rest und Ignorierregeln, 3D-Gravur, Rasterverfahren, Parametersätze und die Materialbibliothek (Sichern, doppelte Einträge, Wiederherstellen). Außerdem prüft er Geräteliste und Gerätewechsel in einem temporären Einstellungsordner, Drehachsen-Aufträge, Kamerahintergrund aus einer lokalen Bilddatei und die Kalibrierseite. Rust-Tests prüfen drei unabhängige TCP-Verbindungen ausschließlich lokal. Er sendet keinen
Job an den Lasercutter. Der normale Start enthält keinen Selbsttest.

Die Materialdaten sind in `resources/materials.json` eingebettet.
Originaldateien liegen unter `reference/fau-settings`.
Der Import lässt sich mit `scripts/import-fau-materials.py` aus einem
lokalen Checkout der FAU-Einstellungen wiederholen. 0 mm bedeutet in
den FAU-Profilen „nicht festgelegt“ und wird entsprechend angezeigt.
Profile mit 0 % Leistung werden nicht als übernehmbare Presets angeboten.

Details und fehlende Funktionen: [PROTOCOL.md](PROTOCOL.md).
Lizenz: LGPL-3.0-or-later, siehe `../COPYING.LESSER`.
