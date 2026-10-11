#!/usr/bin/env bash
# Regenerates expected.ltt of every case in tests/java_parity with the
# original Java pipeline (VisiCut + LibLaserCut). Needs Java 17+, Maven,
# network access to Maven Central and the LibLaserCut submodule.
#
#   bash scripts/java-parity/generate.sh            # all cases
#   bash scripts/java-parity/generate.sh cut_rect   # selected cases
#
# The Java build is reused; REBUILD=1 forces a new one.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
crate="$(cd "$here/../.." && pwd)"
repo="$(cd "$crate/.." && pwd)"
cases="$crate/tests/java_parity"
jar="$repo/target/visicut-1.9-SNAPSHOT-full.jar"
work="${TMPDIR:-/tmp}/visicut-java-parity"

if [ ! -f "$repo/LibLaserCut/pom.xml" ]; then
  git -C "$repo" submodule update --init LibLaserCut
fi
if [ ! -f "$jar" ] || [ "${REBUILD:-0}" = 1 ]; then
  (cd "$repo/LibLaserCut" && mvn -q -B -DskipTests install)
  # initialize installs the bundled kabeja jars from legacy/.
  (cd "$repo" && mvn -q -B initialize && mvn -q -B -DskipTests package)
fi

rm -rf "$work/classes"
mkdir -p "$work/classes"
javac -cp "$jar" -d "$work/classes" "$here/LttParity.java"

# Same module openings as the Add-Opens entries of VisiCut's jar manifest.
opens=()
for p in java.base/java.util java.base/java.lang java.base/java.io java.base/java.text \
  java.desktop/java.awt.geom java.desktop/java.awt java.xml/com.sun.org.apache.xerces.internal.parsers; do
  opens+=("--add-opens=$p=ALL-UNNAMED")
done

if [ "$#" -gt 0 ]; then
  dirs=()
  for name in "$@"; do dirs+=("$cases/$name"); done
else
  dirs=("$cases"/*/)
fi
# A private home keeps VisiCut from reading or writing the user's settings.
mkdir -p "$work/home"
java "${opens[@]}" -Djava.awt.headless=true -Duser.home="$work/home" \
  -cp "$jar:$work/classes" LttParity "$crate/reference/FAU-LTT-iLaser-4000.xml" "${dirs[@]}"
