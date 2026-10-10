# LTT iLaser 4000: Portierungsstand

Der Rust-Treiber basiert auf `LaserToolsTechnicsCutter.java` aus
LibLaserCut (Autor Maximilian Gaukler, mit Teilen von Thomas Oster,
LGPL-3.0-or-later). Die Originalquelle und das FAU-Geräteprofil stehen
im Ordner `reference`. Abruf: 8. Oktober 2026.

Originalquellen:

- https://github.com/t-oster/LibLaserCut/blob/master/src/main/java/de/thomas_oster/liblasercut/drivers/LaserToolsTechnicsCutter.java
- https://github.com/fau-fablab/visicut-settings/blob/master/devices/LTT_32_iLaser_32_4000.xml

Übernommen wurden Initialisierung, Dateiname mit VC-Präfix, XY-Modus,
Bounding-Box-Struktur, Prescaling 8 (4000/500 DPI), Materialradius,
Kompressionskonstante C0, Graustufenpalette, Vektormodus,
Leistungs- und Geschwindigkeitsbefehle, PPI-Teiler, PA/PR/PD/PU,
Rasterzeilen, abschließendes BYE, additive 16-Bit-Prüfsumme und
32-Bit-Dateilänge. Zahlen sind Big Endian. Der normale Y-Ursprung
wird für Bewegungen vom oberen zum unteren linken Rand umgerechnet.
Bounding-Box-Werte folgen der separaten Längen-Konvertierung des
Java-Treibers und werden dort nicht gespiegelt.

Das veröffentlichte FAU-Profil verwendet `lasercutter2:9100`,
1000 × 600 mm, 4000 Maschinen-DPI, 500 Job-DPI und 338,677 mm/s
nominale Schnittgeschwindigkeit. Der Faktor zwischen Raster- und
Schnittgeschwindigkeit ist 6,4 wie im Java-Treiber. Die Raster-Verschiebetabelle
ist [-2, -4, -7, -10, -13, -15, -17, -19, -21, -23].

Seit Version 0.2.1 ist die eingestellte Geschwindigkeit ein Prozentwert
von 0,1 bis 100 %, wie im ursprünglichen LTT-Treiber und in den FAU-Profilen.
Der ESC-S-Befehl erhält diesen Wert in Zehntelprozent: 9 % wird zu 90,
100 % zu 1000. Die zuvor verwendete Umrechnung in mm/s ist ausschließlich
für die Migration von Projektformat 1 erhalten. Neue Projekte speichern
`speed_percent` in Formatversion 2; die Migration berücksichtigt Schnitt
und Gravur getrennt, damit bestehende Einstellungen erhalten bleiben.

Die Rust-App schickt Jobs ohne Autorun über eine TCP-Verbindung, deren
Schreibseite anschließend geschlossen wird. Das Protokoll enthält hier
keine Gerätequittung; erfolgreiche TCP-Übertragung bestätigt keine
erfolgreiche Verarbeitung auf dem Lasercutter. Es gibt keine automatische
Wiederholung nach Fehlern.

## Bewusste Einschränkungen

- Kurven werden wie Java2Ds `FlatteningPathIterator` in Geraden zerlegt: in
  500-DPI-Pixeln mit der Flachheit 0,2 px (`getRequiredCurvePrecision` bei
  Tangentialkurven), höchstens zehn Halbierungen. Wie VisiCuts
  `ShapeConverter` entfallen Linienenden, deren abgeschnittenes Pixel dem des
  letzten Punkts gleicht; die Schließlinie bleibt immer. Konturpunkte sind
  `f64` wie in Java. Die Pfade werden wie im Java-Treiber (`curveOrLine`) an
  Ecken geteilt. Flache Abschnitte gehen
  als verbundene Tangentialkurve (`PJ … PE <Tempo> PR … PF`) mit
  Geschwindigkeitsplanung bis 2000 mm/s² hinaus (`curve`,
  `curveWithKnownSpeed`, Neuinterpolation 0,9 mm, zehn Aufwärmrunden).
  Geschlossene Kreise bis 101 mm Radius nutzen den Kreisbefehl
  (`PJ PB 0 0 <Mitte> PF`) mit begrenzter Geschwindigkeit und
  proportional reduzierter Leistung; größere Kreise und Drehachsen-Jobs
  verwenden normale Kurven. Arc compensation am Gerät wird wie im FAU-Profil
  als eingeschaltet angenommen. Die Zeitschätzung berücksichtigt Beschleunigung
  und Bremsen (`cuttingTimeForPxDistance`).
  Gestrichelte Konturen (`stroke-dasharray`, `stroke-dashoffset`) werden wie
  in VisiCut (`DashedShape`) als einzelne Striche geschnitten, etwa für
  Perforationen. Strichlängen gelten im Koordinatensystem des Pfads und
  skalieren mit Transformationen und viewBox; das Muster beginnt in jedem
  Teilpfad neu und läuft bei geschlossenen Formen über den Startpunkt
  hinweg. Reine Füllformen bleiben durchgehend.
- Schnittreihenfolge wie VisiCuts Standard „innen zuerst“: wörtliche
  Portierung von `VectorOptimizer`, `OptimizerUtils.joinContiguousLoopElements`
  und `InnerFirstVectorOptimizer` in 500-DPI-Pixeln. Wie in VisiCuts
  `VectorProfile` werden die Formen einmal je `LaserProperty` hinzugefügt;
  jeder Durchgang ist eine weitere gleiche Eigenschaft. Elemente gleicher
  Werte (Leistung, Geschwindigkeit, Fokus = Materialstärke wie VisiCuts
  `useThicknessAsFocusOffset`) bilden eine Gruppe; die Gruppen folgen der
  Reihenfolge von Javas `HashMap` (Bucket des `LaosCutterProperty`-Hashs,
  `computeIfAbsent` fügt neue Schlüssel am Bucket-Anfang ein). Je Gruppe
  kommen erst die geschlossenen Pfade, dann offene Pfade, die verbunden
  werden, wo genau ein anderes Ende innerhalb von 0,9 px (Manhattan-Abstand)
  liegt, auch über Durchgänge hinweg (eine doppelt geschnittene Linie wird
  Hin- und Rückweg). Danach folgt die stabile Sortierung nach
  Begrenzungsrahmen: unterer Rand aufsteigend, oberer absteigend, rechter
  aufsteigend, linker absteigend. Löcher kommen so vor dem Außenumriss, und
  die Durchgänge einer Kontur folgen direkt aufeinander. Gleiche Rahmen werden
  nicht unterschieden. Ein Teil wird mit einem Encoder erzeugt; Leistung und
  Geschwindigkeit gehen wie im Treiber nur bei Änderung hinaus.
  Nicht nachgebildet: Baum-Buckets der `HashMap` ab acht gleichen Buckets.
- Zuordnung wie VisiCuts Mappings: Schritte wählen Objekte einzeln, über
  Bedingungen (Farbe, Linien-/Füllfarbe, Linienstärke in mm mit „=“ oder „≤“,
  Gruppe/Inkscape-Ebene, Typ, ID; jeweils auch negiert) oder als Rest.
  Ignorierregeln nehmen Objekte aus dem Rest. Ein Objekt kann mehrere
  Schritte durchlaufen. Vorlagen enthalten die beiden FAU-Mappings.
  Jeder Schritt kann weitere Parametersätze haben, die nacheinander dieselben
  Objekte bearbeiten. Pro Verfahren entsteht ein eigenständig gerahmter
  LTT-Job mit allen Schritten dieses Verfahrens, in der Reihenfolge
  Engrav → Eng3D → Mark → Cut, mit jeweils eigener TCP-Verbindung.
  Text wird beim Schneiden und Markieren als Glyphenumriss bearbeitet
  (Systemschriften, Ersatz Ubuntu Light/Hack aus egui); fehlt für ein
  Zeichen jede Schrift, wird der Auftrag abgewiesen. Rasterbilder werden beim
  Schneiden abgewiesen, weil Raster beim Schneiden nicht definiert ist.
  Clip-Pfade (`clipPath`) schneiden die Konturen als Schnittmenge mit der
  Vereinigung der Clip-Formen; jede Form zählt nach ihrer Füllregel (`clip-rule`).
  Offene Konturen werden an den Clip-Kanten geteilt und nur innen geschnitten;
  geschlossene Konturen, die ganz innen liegen, bleiben unverändert, und Teile,
  die am Startpunkt zusammenhängen, werden wieder verbunden. Die Schnittpunkte
  liegen auf Segmenten, nicht auf Eckpunkten; Teilstücke werden per Mittelpunkt-
  test innen oder außen zugeordnet. Parallel verlaufende Clip-Kanten und Konturen
  erzeugen keine Schnittpunkte; dort entscheidet allein der Mittelpunkttest. Ein Pfad
  vollständig außerhalb liefert nichts. Ein leerer Clip, ein fehlender Ziel-
  verweis oder ein Ziel, das kein Clip-Pfad ist, werden mit Meldung abgewiesen,
  ebenso verschachtelte Clips. Die Grenze liegt bei 10⁸ Segment-Kanten-Prüfungen
  je Kontur; darüber wird mit Hinweis abgebrochen. Masken und Filter werden
  beim Schneiden abgewiesen, mit Meldung zum Grund: Masken legen nur Transparenz
  fest, Filter verändern nur ein Pixelbild, beides ergibt keine Schnittlinie.
  Text und Rasterbilder innerhalb eines Clip-Pfads werden abgewiesen. Am Gerät
  wurde das Clipping nicht geprüft.
- Gravur mit den LibLaserCut-Rasterverfahren Floyd-Steinberg, Mittelwert,
  Zufall, Geordnet, Raster, Halbton und Halbton aufgehellt (FAU-Standard) sowie
  dem früheren Schwellwert 128 (ältere Projekte). Graustufen nach
  BufferedImageAdapter (0,3 R + 0,59 G + 0,11 B), Helligkeitsverschiebung und
  Invertierung wie im Rasterprofil. Bidirektional (`ESC 1`, Zeile gespiegelt)
  und von unten nach oben wie `LaosEngraveProperty`. Die Zeilenverschiebung
  folgt `getEngraveShiftPixels` einschließlich +0,5 Pixel.
  Wie VisiCuts `RasterProfile` wird nur die Bounding Box der Objekte
  (mit halber Strichbreite) gerastert, `Helper.toRect` schneidet x, y, Breite
  und Höhe einzeln ab; Rasterursprung ist deren Ecke. Gravur ohne, 3D-Gravur
  mit Antialiasing, Bilder mit Nächster-Nachbar-Interpolation. Jeder Durchgang
  ist ein eigener Rasterteil mit Farbcode, Einstellungen bei Änderung und
  Beginn nach rechts.
  Abweichungen: Die geordnete Matrix nutzt 255 statt 256, damit reines Weiß
  nicht gepunktet wird; „Zufall“ ist reproduzierbar statt ungeseedet. Kanten,
  die nicht auf ganzen Pixeln liegen, rastern Java2D und tiny-skia
  unterschiedlich (Pixelmitte bzw. Teilabdeckung); einzelne Randpixel können
  daher abweichen.
- 3D-Gravur: eigener Auftrag, Job-Modus 8 Bit je Pixel (`ESC M 0x02`),
  Leistung je Pixel = Dunkelheit (wie der Java-Treiber nach `invertBits`).
  Abweichungen: Leere Ränder werden bei Leistung 0 (weiß) statt bei Grauwert 0
  abgeschnitten, und die Zeilenverschiebung erfolgt in ganzen Pixeln, damit
  Leistungswerte nicht zwischen Pixeln verschoben werden. Am Gerät nicht
  validiert.
- Drehachse wie im Java-Treiber: Job-Modus `ESC M 0x10`, temporärer
  Referenzpunkt Mitte (`ESC a 0x15`), Materialradius in 0,01 mm
  (`ESC R`), Y als Drehwinkel mit 6400 Schritten je Umdrehung, ungespiegelt.
  Die 6400 Schritte sind im Java-Treiber als Schätzung markiert; der
  Durchmesser muss 5–1000 mm betragen, das Motiv darf nicht höher als der
  Umfang sein. Vor dem Start „Adjust rotary temp“ am Gerät; die App nennt den
  nötigen Freiraum links und rechts. Am Gerät noch nicht validiert.
- Externe SVG-Bilder (`<image>` mit `href`/`xlink:href` als relativer oder
  absoluter Dateipfad oder `file://`-URL) werden einmalig beim Import relativ
  zum Ordner der SVG-Datei aufgelöst und als `data:`-URI eingebettet; das
  Projekt bleibt so ohne die Bilddateien portabel. Nur diese Attributwerte
  ändern sich, der übrige SVG-Text bleibt unverändert. Unterstützt werden PNG,
  JPEG, GIF, WebP und SVG (Format nach Dateiinhalt), je Bild bis 10 MB und
  insgesamt bis 20 MB SVG-Größe. `http(s)`-Adressen werden nicht abgerufen.
  Fehlende, zu große oder nicht unterstützte Bilder brechen den Import nicht
  ab, sondern werden als Hinweis angezeigt und bleiben unverändert verknüpft
  (also unsichtbar). Bereits gespeicherte Projekte werden nicht nachträglich
  aufgelöst.
- Rasterbilder (PNG, JPEG, BMP, GIF) werden als SVG mit einem eingebetteten
  `<image>` importiert. Die Größe ergibt sich aus der Auflösung in der Datei
  (PNG `pHYs`, JPEG JFIF- oder Exif-Dichte, BMP Pixel je Meter); fehlt sie,
  gilt wie im Java-`JPGPNGImporter` fest 72 DPI (mit Hinweis). BMP wird
  verlustfrei als PNG eingebettet, JPEGs mit Exif-Drehung oder CMYK werden
  gedreht bzw. als RGB-JPEG (Qualität 95) neu kodiert, da resvg beides nicht
  beherrscht. Eingebettet höchstens 20 MB SVG; animierte GIFs zeigen nur das
  erste Bild.
- G-Code (`.nc`, `.gcode`) wie im Java-`GCodeImporter`: `G0` bewegt ohne zu
  zeichnen, `G1` zeichnet Linien, `G2`/`G3` Bögen im/gegen den Uhrzeigersinn,
  `G20`/`G21` Zoll/mm; Koordinaten sind mm auf dem Arbeitsbett, Y nach unten,
  ungespiegelt; der Ursprung zählt wie beim Java-Pfad (`moveTo(0, 0)`) zur
  Ausdehnung, so bleiben absolute Positionen erhalten. `M3`/`M5`, `S`, `F`
  und `Z` werden ignoriert, die Bahn ist ein roter Pfad (0,1 mm). Abweichend
  vom Java-Code (dort TODO bzw. fehlerhaft): `G90`/`G91`, Bögen mit `R`,
  `I`/`J` relativ zum Bogenanfang (`G90.1` absolut wie in Java), `G0`–`G3`
  auch einstellig, Kleinbuchstaben sowie Kommentare in `( )` und nach `;`.
- Autofokus, Druckluft und Absaugung: Der Java-Treiber enthält dafür nur
  Platzhalter (`setFocus`, `setPurge`, `setVentilation` senden keine Bytes,
  `isAutoFocus` ist nicht implementiert); das LTT-Protokoll dafür ist nicht
  dokumentiert. Der Rust-Treiber sendet daher ebenfalls nichts. Stattdessen
  zeigt die App nach dem Senden den Gerätehinweis (`jobSentText` aus den
  VisiCut-Einstellungen, beim FAU-Gerät „Autofokus machen, Druckluft an“).
- SVG und `.vcr` werden unterstützt. Version 0.2 ergänzt eine native
  AppKit-/SwiftUI-Oberfläche und eine Auswahl der FAU-LTT-Materialprofile.
  VisiCut-PLF-Dateien werden mit Geometrie und den Zuordnungen der SVG-Teile
  übernommen (`src/import/plf/mappings.rs`). Die Laser-Einstellungen fehlen,
  weil VisiCut sie lokal pro Gerät, Material und Stärke speichert; die Schritte
  erhalten die Standardwerte. Parametrische SVG nur mit Standard- bzw.
  gespeicherten Parameterwerten. Allgemeiner Materialbibliothek-Import
  und andere Gerätetreiber fehlen. Nur der LTT iLaser 4000
  (1000 × 600 mm, 4000 DPI) wird unterstützt; andere Geräte werden beim
  Import abgewiesen.
- PDF-Import (neu gegenüber Java-VisiCut, das kein PDF liest): Seite 1 wird
  mit `hayro-svg` (reines Rust, MIT/Apache-2.0) in SVG umgewandelt; Pfade,
  Farben, Linienstärken und eingebettete Bilder bleiben erhalten, Text wird zu
  Glyphenkonturen. Größe aus der CropBox in pt (× 25,4/72 mm). Linienstärke 0
  (PDF: dünnste Linie) wird zu 0,1 mm. Clip-Pfade bleiben erhalten und werden
  beim Schneiden berücksichtigt; Soft-Masks bleiben für die Vorschau erhalten,
  verhindern aber das Schneiden (Hinweis beim Import).
- EPS/PS: Wie Java-VisiCut (`EPSImporter`) liest ein eingebauter, bewusst
  begrenzter PostScript-Interpreter (`src/import/eps/`) die Datei ohne
  Zusatzprogramm und gibt SVG aus. Größe aus `%%BoundingBox:` (sonst
  `%%PageBoundingBox:`, Zeilen mit `(atend)` werden übersprungen; ohne Angabe
  800 × 600 pt mit Hinweis), 1 pt = 25,4/72 mm. Der viewBox ist in pt, der
  Ursprung liegt links unten in der BoundingBox, y zeigt nach unten (wie bei
  der Ghostscript-Umwandlung). Unterstützt:
  - Operand- und Dictionary-Stack: `pop exch dup copy index roll`, `def load`
    (nur das Benutzerwörterbuch, kein `begin`/`end`)
  - Arithmetik und Vergleiche: `add sub mul div neg abs sqrt sin cos`,
    `eq ne lt le gt ge not true false`
  - Prozeduren und Schleifen: `{ }`, `exec if ifelse repeat for`
  - Pfade: `newpath moveto rmoveto lineto rlineto curveto rcurveto closepath
    arc arcn rect`; Malen: `fill eofill stroke`
  - Farben (nach RGB): `setrgbcolor setgray setcmykcolor`; `setlinewidth`
    (0 wird zu 0,1 mm)
  - Matrizen: `gsave grestore translate scale rotate concat matrix setmatrix`
  - ohne Wirkung: `showpage bind`

  Nicht unterstützt sind Text (`show` usw.), Schriften (`findfont`,
  `setfont` usw.), Bilder (`image`, `colorimage`), Clipping, `[ ]`-Arrays,
  `<< >>`-Dictionaries, `loop`, `exit` und Binär-EPS mit DOS-Header. Trifft
  der Interpreter auf einen solchen Operator, wird die Datei mit Ghostscript
  (`-sDEVICE=pdfwrite -dEPSCrop -dSAFER -dNoOutputFonts`, 60 s Zeitlimit,
  temporäre Datei im Temp-Ordner) in PDF umgewandelt und wie PDF importiert.
  Gesucht wird im `PATH`, unter macOS zusätzlich in `/opt/homebrew/bin` und
  `/usr/local/bin`, unter Windows in `Programme\gs\*\bin`. Ohne Ghostscript
  scheitert der Import mit einer Meldung, die den Operator nennt (z. B.
  „Text-Operator „show“ wird nicht unterstützt“) und auf Ghostscript verweist.
- DXF (ASCII und binär, R12 bis aktuell) wird mit einem eigenen Parser in eine
  SVG in Millimetern umgewandelt (`src/import/dxf.rs`). Anders als VisiCuts
  kabeja-Import wird `$INSUNITS` beachtet; Zeichnungen ohne Einheit gelten wie
  in Java als Millimeter. Ebenen werden Inkscape-Ebenen, ACI-/True-Color-Farben
  (BYLAYER/BYBLOCK aufgelöst) Linienfarben, Linientypen Strichmuster.
  Unterstützt: LINE, LWPOLYLINE/POLYLINE (Bögen), CIRCLE, ARC, ELLIPSE, SPLINE,
  INSERT/MINSERT (verschachtelt), DIMENSION, LEADER, TEXT/MTEXT/ATTRIB,
  SOLID/TRACE, 3DFACE und HATCH (als Fläche, Muster werden nicht gezeichnet).
  Netze, 3D-Körper, Bilder und unendliche Linien werden mit Hinweis
  ausgelassen; ausgeschaltete/gefrorene Ebenen und der Papierbereich ebenfalls.

Dies ist ein ausführbarer Anfang der Portierung mit durchgehendem
SVG→Rust→LTT-Workflow, keine vollständige Funktionsparität mit VisiCut.
Vor Produktionseinsatz ist ein beaufsichtigter Vergleich mit dem
Java-Treiber am tatsächlichen Gerät erforderlich.

## Auftragsvorschau und Dauer

Die Vorschau entsteht aus den für den Job verwendeten Konturen und
Schwarz/Weiß-Rasterpixeln. Schnittkonturen erscheinen rot, Gravurpixel blau und Markierkonturen violett.
Die Schätzung verwendet die quantisierten Vektorwege und die tatsächlichen
Rasterzeilen einschließlich Overscan, nomineller Rückfahrt und Durchgängen.
Rasterzeilen erhalten wie im Java-Treiber einen Zuschlag von 0,1 Sekunden.
Die Geschwindigkeiten werden für die Schätzung wie für ESC-S auf Zehntelprozent
quantisiert. Nicht modelliert sind die exakten Beschleunigungsprofile,
Gerätestart, Übertragungszeit und Bedienpausen. Keine Kalibrierung am Gerät.

Projektformat 2 enthält optional `steps`: maximal je einen Schnitt-, Markier-
und Gravurschritt, mit nullbasierten SVG-Objektindizes und eigenen Parametern.
Ein fehlendes oder leeres `steps` behält die bisherige Ganzmotiv-Bearbeitung bei.
Leere Objektauswahlen innerhalb expliziter Schritte werden übersprungen;
bei vollständig leerem Auftrag, doppelten oder ungültigen Objektindizes wird
kein Job erzeugt. Jeder Import ersetzt vorhandene Zuordnungen; eine PLF-Datei
bringt ihre eigenen mit.

PLF-Zuordnungen werden je Teil nach VisiCuts Regeln übersetzt: Filter werden
gegen die Objekte des eigenen Teils ausgewertet. Die Bedingungen bleiben als
Regel erhalten, wenn sie im ganzen Motiv genau diese Objekte treffen; sonst
übernimmt der Import die Objekte fest. Der Rest eines Teils und die Ignorier-
Einträge werden ebenfalls als feste Objektliste übernommen. Ein Teil ohne
lesbare Zuordnung erhält keinen Schritt. Ohne übernommenen Schritt entsteht
ein leerer Schritt, damit nicht das ganze Motiv bearbeitet wird.

Der Zeitslider verwendet Bewegungen aus der Erzeugung der tatsächlichen
LTT-Daten. Jeder Vektorschritt wird nach 500-DPI-Quantisierung erfasst,
Rasterzeilen einschließlich Overscan und Leerfahrten. Wiederholte
Rasterdurchgänge teilen die gespeicherte Geometrie. Vektordurchgänge sind wie
in Java je Kontur verschränkt und bilden daher einen Ablauf ohne getrennte
Durchgänge; Zeitabschnitte bilden die Reihenfolge ab.
Die erste Anfahrt wird ab (0,0) geschätzt; danach wird die letzte Position
fortgeschrieben. Tatsächliche Kopfposition und Pausen zwischen den getrennt
zu startenden Gerätejobs sind nicht bekannt. Die Simulation sendet keine Daten.

Die Bounding Box im Header ist wie `LaserJob.getBoundingBox` die des
tatsächlichen Inhalts (Vektorpunkte bzw. Rasterbereich), nicht die der
SVG-Seite.

Gerätenamen beginnen direkt mit `Cut_`, `Engrav_` bzw. `Mark_`, ohne zusätzlichen
VC-Präfix, und werden auf 15 ASCII-Zeichen begrenzt. Der Präfix bleibt beim
Kürzen erhalten. Alle Teiljobs werden vor dem ersten Netzwerkzugriff geprüft.
Jeder hat eigenen Header, BYE, Prüfsumme, Länge und deaktivierten Autostart.
Bei einer fehlgeschlagenen Übertragung stoppt die Folge; die Fehlermeldung
nennt den betroffenen und bereits übertragenen Teil. Es gibt weder eine
Gerätequittung noch automatisches Fortsetzen oder eine automatische Wiederholung.

## Byte-Vergleich mit Java

`tests/java_parity.rs` vergleicht die Rust-Ausgabe Byte für Byte mit
Referenzaufträgen des originalen Java-VisiCut (LibLaserCut-LTT-Treiber,
FAU-Gerät, `scripts/java-parity`). Jeder Fall in `tests/java_parity/` enthält
die SVG, die Einstellungen (`case.properties`) und `expected.ltt`. Abgedeckt
sind Rechteck, Kreisbefehl, Bézierkurven, verschachtelte und offene Konturen,
Durchgänge, mehrere Parametersätze, Markieren sowie Gravur mit
Floyd-Steinberg, Halbton, Halbton aufgehellt (einseitig, von unten), Geordnet,
Mittelwert (Helligkeit, Invertierung) und Raster einschließlich eines
eingebetteten Graustufenbilds. Die Rasterfälle liegen auf ganzen Pixeln, weil
Java2D und tiny-skia schräge oder gebrochene Kanten verschieden rastern. Der
Fall „Geordnet“ enthält kein reines Weiß, und bei der 3D-Gravur wird nur der
Header verglichen; beides sind die oben beschriebenen bewussten Abweichungen.
Rust schickt pro Verfahren einen eigenen Auftrag; die Fälle enthalten daher je
ein Verfahren. Neu erzeugen:
`bash scripts/java-parity/generate.sh` (Java 17+, Maven, Submodul LibLaserCut).
