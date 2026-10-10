#!/bin/bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"
if [[ "$(uname -s)" != Darwin ]]; then
  echo "Dieses Script benötigt macOS." >&2
  exit 1
fi
cargo build --release --lib --bin visicut-cameraserver --locked
arch="$(uname -m)"
xcrun swiftc -parse-as-library -swift-version 5 -O \
  -target "$arch-apple-macosx14.0" \
  -module-cache-path target/swift-module-cache \
  native/VisiCutApp.swift native/JobSimulation.swift native/Devices.swift native/Mapping.swift native/Vectorize.swift target/release/libvisicut_core.a \
  -o target/release/visicut-native \
  -framework SwiftUI -framework AppKit -framework CoreFoundation \
  -framework Security -framework OpenGL -framework CoreGraphics \
  -liconv -Xlinker -dead_strip
app="$project_dir/dist/VisiCutRust.app"
mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources"
cp target/release/visicut-native "$app/Contents/MacOS/visicut-rust"
cp target/release/visicut-cameraserver "$app/Contents/MacOS/visicut-cameraserver"
codesign --force --sign - "$app/Contents/MacOS/visicut-cameraserver"
cp ../distribute/mac/MacIcon.icns "$app/Contents/Resources/VisiCut.icns"
cp ../COPYING "$app/Contents/Resources/COPYING"
cp ../COPYING.LESSER "$app/Contents/Resources/COPYING.LESSER"
cp README.md "$app/Contents/Resources/README.md"
cp PROTOCOL.md "$app/Contents/Resources/PROTOCOL.md"
cp resources/materials.json "$app/Contents/Resources/materials.json"
cat > "$app/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleName</key><string>VisiCutRust</string>
  <key>CFBundleDisplayName</key><string>VisiCutRust</string>
  <key>CFBundleExecutable</key><string>visicut-rust</string>
  <key>CFBundleIdentifier</key><string>io.github.dbambus.visicutrust</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleIconFile</key><string>VisiCut.icns</string>
  <key>LSMinimumSystemVersion</key><string>14.0</string>
  <key>NSHighResolutionCapable</key><true/>
  <key>NSPrincipalClass</key><string>NSApplication</string>
  <key>CFBundleDocumentTypes</key><array>
    <dict><key>CFBundleTypeName</key><string>VisiCutRust Projekt</string>
    <key>CFBundleTypeRole</key><string>Editor</string>
    <key>LSItemContentTypes</key><array><string>org.visicut.project</string></array></dict>
    <dict><key>CFBundleTypeName</key><string>SVG</string>
    <key>CFBundleTypeRole</key><string>Viewer</string>
    <key>LSItemContentTypes</key><array><string>public.svg-image</string></array></dict>
  </array>
  <key>UTExportedTypeDeclarations</key><array><dict>
    <key>UTTypeIdentifier</key><string>org.visicut.project</string>
    <key>UTTypeDescription</key><string>VisiCutRust Projekt</string>
    <key>UTTypeConformsTo</key><array><string>public.json</string></array>
    <key>UTTypeTagSpecification</key><dict><key>public.filename-extension</key><array><string>vcr</string></array></dict>
  </dict></array>
</dict></plist>
PLIST
python3 - "$app/Contents/Info.plist" <<'PY'
import json
import plistlib
import subprocess
import sys
from pathlib import Path

metadata = json.loads(subprocess.check_output([
    "cargo", "metadata", "--locked", "--no-deps", "--format-version", "1",
]))
version = metadata["packages"][0]["version"].split("-")[0]
path = Path(sys.argv[1])
info = plistlib.loads(path.read_bytes())
info["CFBundleVersion"] = version
info["CFBundleShortVersionString"] = version
path.write_bytes(plistlib.dumps(info))
PY
plutil -lint "$app/Contents/Info.plist"
codesign --force --sign - "$app"
codesign --verify --strict "$app"
echo "$app"
