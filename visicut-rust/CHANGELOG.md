# Änderungen an VisiCutRust

Jede Version hat einen Abschnitt `## <Version> – <Datum>`; der Release-Workflow
übernimmt ihn als Beschreibung des GitHub-Releases.

## 0.12.0 – 12.10.2026

- **Sprachen** (egui und macOS): Oberfläche, Meldungen des Rust-Kerns und der
  Importfilter auf Deutsch, Englisch, Französisch, Italienisch und
  Niederländisch; Auswahl unter **Darstellung → Sprache**, ohne Auswahl gilt
  die Systemsprache. Neu angelegte Standardwerte (Projektname, „Eigenes
  Material“, „Neues Material“, Schildtext, Namen neuer Formen) entstehen in
  der gewählten Sprache. Ein Test stellt sicher, dass jeder Text in jeder
  Sprache mit denselben Platzhaltern übersetzt ist.
- **Zuletzt geöffnet** (egui): **Datei → Zuletzt geöffnet** listet die zehn
  zuletzt geöffneten oder gesicherten Dateien; Sprache und Liste stehen in
  `app-settings.json` im Einstellungsordner.
- **Oberfläche wie unter macOS** (egui): Werkzeugleiste mit Öffnen, Sichern,
  Vorschau, Export und Senden statt Knöpfen am Ende der Seitenleiste;
  Seitenleiste in der Reihenfolge der macOS-App (Projekt, Rotationseinheit,
  Material, Objektzuordnung, Bearbeitung, Position und Größe) als Karten mit
  Bezeichnung links und Feld rechts; segmentierte Zuordnungswahl, farbige
  Verfahrenswahl, ein Abschnitt je Schritt; Vorschauleiste mit Zeitschätzung
  und Legende; Startansicht ohne Motiv wie unter macOS; besser lesbare
  Hinweistexte und Farbtabelle für die Regelvorlage „Farben“.

## 0.11.0 – 11.10.2026

- **Mehrere Dateien per Drag-and-drop hinzufügen** (egui und macOS): Ist schon
  ein Motiv geladen, kommen abgelegte Grafiken in Originalgröße rechts daneben
  (sonst darunter) hinzu, statt das Motiv zu ersetzen. Ohne Motiv öffnet die
  erste Datei, die übrigen werden angehängt. Bestehende Objekte behalten
  Nummern und Zuordnungen; bei Einzelzuordnung nennt ein Hinweis die noch
  nicht zugeordneten neuen Objekte. Gleichnamige IDs und CSS-Klassen (z. B.
  `cls-1` aus Illustrator) werden umbenannt. Widerrufen nimmt das ganze
  Ablegen zurück; nicht lesbare Dateien werden gemeldet.
- **Profile anderer Lasercutter**: Dropdown „Profile“ über dem Material
  wechselt zwischen den FAU-Profilen des LTT iLaser 4000 und des Epilog Zing
  (42 Materialien, 128 Profile, nur lesbar, mit Hinweis zur Prüfung am LTT).

## 0.10.0 – 11.10.2026

- **Absturz behoben** beim Wechsel zwischen Gesamt, Einzeln und Regeln
  (macOS): entfernte Schritte und Bedingungen werden nicht mehr über einen
  veralteten Index gelesen.
- **Nesting** funktioniert mit Kopien im Raster (z. B. 5 × 5 Kopien, 1 mm
  Abstand): Teile werden nach ihren sichtbaren Umrissen angeordnet, und jedes
  Teil wird nur einmal eingebettet statt mit dem ganzen Dokument.
- **Materialprofil ist der Standard**: Es wird bei Material, Stärke und
  Verfahren automatisch übernommen. Abweichende Werte sind orange markiert
  und lassen sich links vom Feld zurücksetzen.
- **Regeln** erklärt; neue erste Vorlage „Rot schneiden, Blau gravieren, Grün
  markieren“ (#ff0000, #0000ff, #00ff00).
- **Bezugspunkt** als Rahmen mit acht verbundenen Quadraten und freiem
  Mittelpunkt.
- **Arbeitsbett**: rote Warnung über dem Bett, wenn das Motiv nicht passt;
  Rasterschalter auch auf macOS; Zoom mit Mausrad und Trackpad um den
  Mauszeiger, Verschieben mit zwei Fingern.
- Dropdowns rechtsbündig mit sichtbarem Menüpfeil; „Drehachse“ heißt jetzt
  „Rotationseinheit“; „Auf dem Arbeitsbett zentrieren“ und die Hinweise am
  Ende der Seitenleiste entfallen.

## 0.9.0 – 11.10.2026

- **Transformieren** wie in Grafikprogrammen: Bezugspunkt (3 × 3) für X/Y
  und Größenänderungen, Drehen um einen beliebigen Winkel, 90°-Schritte,
  horizontal und vertikal spiegeln (ohne Verzerren).
- **Rechtsklick auf das Motiv**: Duplizieren, Kopien im Raster, Nesting,
  Drehen, Spiegeln, Zentrieren und Löschen.
- **Löschen**: Entf/⌫ (macOS auch ⌘⌫) entfernt das Motiv, „Objekt löschen“
  im Kontextmenü der Objektliste ein einzelnes Objekt; Zuordnungen rücken
  nach.
- **Widerrufen** (⌘Z/Strg+Z) für Löschen, Drehen, Spiegeln, Duplizieren und
  die Objekt-Werkzeuge (ein Schritt).
- **Duplizieren** (⌘D/Strg+D) legt eine Kopie rechts daneben oder darunter.
- macOS: Der Inspektor scrollt nicht mehr unter die Fensterknöpfe.

## 0.8.0 – 11.10.2026

- **Text und Formen ohne Inkscape** (Menü „Objekt“): Text mit den Schriften
  der App (fett, kursiv, mehrzeilig, Ausrichtung), Rechteck mit Eckenradius,
  Ellipse, Vieleck und Stern, je zum Gravieren, Schneiden oder Markieren.
  Text wird in Umrisse umgewandelt und daher überall gleich geschnitten.
- **Schild-Assistent**: Platte mit Eckenradius, 0/2/4 Befestigungslöchern,
  optionalem markiertem Rahmen und zentriertem Text in einem Schritt.
- **Kopien im Raster**: Spalten, Zeilen und Abstände; Zuordnungen gelten für
  alle Kopien.
- **Einfaches Nesting**: Teile (Objekte der obersten Ebene bzw. einer
  Inkscape-Ebene) in gewünschter Anzahl nach Umrissrechtecken auf eine
  Fläche verteilt, größte zuerst, optional um 90° gedreht; nicht passende
  Teile werden gemeldet.
- Das bestehende Motiv bleibt dabei unverändert eingebettet; Objektnummern,
  Einzelzuordnungen und Regeln bleiben gültig. Neue Schritte erhalten das
  Materialprofil.
- Releases: Jede neue Version auf `master` wird als vollständiges Release
  (nicht als Vorabversion) mit diesem Abschnitt veröffentlicht. Die
  macOS-App kann mit einer Developer ID signiert und notarisiert werden,
  sobald die Zertifikats-Secrets hinterlegt sind.

## 0.7.0 – 11.10.2026

- Maschinenwerte je Lasercutter: Zeilenversatz-Tabelle, Kurvenbeschleunigung
  und Verweilzeit am Konturstart; Import aus VisiCut-Gerätedateien.
- Zeilenversatz-Testmuster mit Übernahme des besten Blocks in die Tabelle.
- Experimentelle Kurvenplanung mit der Firmware-Rampe 0,1 g.
- Genauere Zeitschätzung (Verweilzeit je Kontur, Gravurfaktor 6).
- Firmware-Analyse des iLaser 4000 in PROTOCOL.md.

## 0.6.0 – 11.10.2026

- Gleicher Funktionsumfang in der macOS- und der Windows/Linux-Oberfläche:
  Menüleiste, Vorschau mit Zeitslider, Materialbibliothek, Einzel- und
  Regelzuordnung mit Wertvorschlägen.
- Option „Doppelte Linien entfernen“.

## 0.5.0 – 10.10.2026

- Funktionsgleichheit mit VisiCut Java: Bitmap-Vektorisierung,
  PLF-Zuordnungen, Clipping, Schnittreihenfolge „innen zuerst“,
  eingebauter EPS-Interpreter.
- Byte-genauer Vergleich mit LibLaserCut in 14 Testfällen.
- Experimentelle Schnittreihenfolge „Kürzeste Leerfahrten“.
