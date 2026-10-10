# Byte-Vergleich mit Java-VisiCut

`generate.sh` schickt jeden Fall aus `tests/java_parity/` durch die originale
Java-Pipeline: VisiCuts SVG-Import, `VectorProfile`/`RasterProfile`/
`Raster3dProfile` und LibLaserCuts `LaserToolsTechnicsCutter` mit dem
FAU-Gerät aus `reference/FAU-LTT-iLaser-4000.xml`. Das Ergebnis landet als
`expected.ltt` neben dem Fall. `cargo test --test java_parity` vergleicht die
Rust-Ausgabe damit Byte für Byte; Java wird dafür nicht gebraucht.

```sh
cd visicut-rust
bash scripts/java-parity/generate.sh             # alle Fälle
bash scripts/java-parity/generate.sh cut_curves  # einzelne Fälle
REBUILD=1 bash scripts/java-parity/generate.sh   # Java neu bauen
cargo test --locked --test java_parity
```

Benötigt werden Java 17 oder neuer, Maven, Zugriff auf Maven Central und das
Submodul `LibLaserCut` (wird bei Bedarf geholt). Der erste Lauf baut
LibLaserCut und VisiCut (`mvn initialize` installiert die kabeja-Jars aus
`legacy/`). VisiCut läuft ohne Oberfläche mit eigenem `user.home` im
Temp-Ordner und liest keine Benutzereinstellungen.

## Ein Fall

`case.properties` beschreibt den Auftrag für beide Seiten:

| Schlüssel | Bedeutung |
| --- | --- |
| `name` | Projektname; der Gerätename ist wie in Rust `<Präfix>_<name>`, höchstens 15 Zeichen |
| `svg` | Eingabedatei im Fallordner |
| `operation` | `cut`, `mark`, `engrave` oder `engrave3d` |
| `power`, `speed`, `passes` | Parameter in Prozent bzw. Durchgänge |
| `additional` | weitere Parametersätze `Leistung:Tempo:Durchgänge;…` |
| `thickness_mm` | Materialstärke; VisiCut addiert sie zum Fokus |
| `x_mm`, `y_mm`, `width_mm`, `height_mm` | Lage und Größe der SVG auf dem Bett |
| `dither` | Klassenname aus `liblasercut.dithering`, z. B. `FloydSteinberg` |
| `invert`, `color_shift`, `unidirectional`, `bottom_up` | Rasteroptionen |
| `compare=header` | nur bis zur ersten Rasterzeile vergleichen (Grund im Kommentar) |

Abweichungen meldet der Test mit dem ersten abweichenden Byte und legt die
Rust-Ausgabe unter `target/tmp/<fall>.ltt` ab.

## Grenzen

- Rust erzeugt pro Verfahren einen eigenen Auftrag, Java einen gemeinsamen;
  jeder Fall enthält daher genau ein Verfahren.
- Java2D und tiny-skia rastern Kanten verschieden (ohne Antialiasing nach
  Pixelmitte mit eigener Rundung, mit Antialiasing mit anderer
  Teilabdeckung). Rasterfälle legen ihre Kanten deshalb auf ganze
  500-DPI-Pixel, Lage und Größe sind in `f32` exakt (127 mm = 2500 px), und
  ein Hintergrund um eine Viertelpixel versetzt sorgt dafür, dass beide
  Seiten den Rasterbereich gleich abschneiden.
- „Zufall“ ist in Java ungeseedet und wird nicht verglichen.
- Bewusste Abweichungen von Java stehen in `PROTOCOL.md`.
