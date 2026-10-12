#!/usr/bin/env python3
"""Import single-pass cut, engrave, mark and 3D engrave profiles from a local FAU checkout.

Usage: python3 scripts/import-fau-materials.py /path/to/visicut-settings
Writes the LTT iLaser 4000 catalogue (resources/materials.json) and the
catalogues of the other FAU lasercutters (resources/profiles/*.json).
No downloads or laser communication are performed.
"""
import argparse
import json
import re
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path


# FAU profile file name and VisiCutRust operation.
OPERATIONS = (
    ("cut", "Cut"), ("engrave", "Engrave"), ("mark", "Mark"),
    ("engrave_32_3d", "Engrave3d"), ("engrave3d", "Engrave3d"),
)
# FAU device directory, device name and destination in resources/.
DEVICES = (
    ("LTT_32_iLaser_32_4000", "LTT iLaser 4000", "materials.json"),
    ("Epilog_95_Zing_95_Fablab", "Epilog Zing", "profiles/epilog-zing.json"),
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("settings", type=Path)
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    settings = args.settings.resolve()
    for directory, device, destination in DEVICES:
        import_device(project, settings, directory, device, destination)


def import_device(project, settings, device_directory, device, destination):
    profiles = settings / "laserprofiles" / device_directory
    materials = []
    for directory in sorted(profiles.iterdir()):
        if not directory.is_dir():
            continue
        source = settings / "materials" / f"{directory.name}.xml"
        name = ET.parse(source).findtext("name") if source.exists() else directory.name
        name = re.sub(r"_(\d+)_", lambda match: chr(int(match.group(1))) if int(match.group(1)) >= 32 else match.group(0), name)
        choices = []
        for thickness in sorted(directory.iterdir(), key=lambda path: float(path.name[:-2])):
            for operation, kind in OPERATIONS:
                if any(c["thickness_mm"] == float(thickness.name[:-2]) and c["operation"] == kind
                       for c in choices):
                    continue
                path = thickness / f"{operation}.xml"
                if not path.exists():
                    continue
                entries = list(ET.parse(path).getroot())
                if len(entries) != 1:
                    continue
                power = entries[0].findtext("power")
                speed = entries[0].findtext("speed")
                if power is None or speed is None:
                    continue
                choices.append({
                    "thickness_mm": float(thickness.name[:-2]),
                    "operation": kind,
                    "power_percent": float(power), "speed_percent": float(speed),
                    "source": str(path.relative_to(settings)),
                })
                reference = project / "reference/fau-settings" / path.relative_to(settings)
                reference.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(path, reference)
        if choices:
            materials.append({"id": directory.name, "name": name, "profiles": choices})
            if source.exists():
                reference = project / "reference/fau-settings/materials" / source.name
                reference.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, reference)
    materials.sort(key=lambda material: material["name"].casefold())
    destination = project / "resources" / destination
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps({
        "source": "fau-fablab/visicut-settings", "device": device,
        "materials": materials,
    }, ensure_ascii=False, indent=2) + "\n")
    print(f"{device}: imported {len(materials)} materials and {sum(len(m['profiles']) for m in materials)} profiles")


if __name__ == "__main__":
    main()
