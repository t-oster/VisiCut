#!/usr/bin/env python3
"""Package host-built CI artifacts. Run from any working directory."""
import argparse
import json
import shutil
import subprocess
import tarfile
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", choices=[
        "x86_64-unknown-linux-gnu", "x86_64-pc-windows-msvc",
        "aarch64-apple-darwin",
    ])
    target = parser.parse_args().target
    project = Path(__file__).resolve().parent.parent
    dist = project / "dist"
    dist.mkdir(exist_ok=True)
    metadata = json.loads(subprocess.check_output([
        "cargo", "metadata", "--locked", "--no-deps", "--format-version", "1",
    ], cwd=project))
    version = metadata["packages"][0]["version"]
    name = f"VisiCutRust-{version}-{target}"

    if target.endswith("apple-darwin"):
        app = dist / "VisiCutRust.app"
        if not (app / "Contents/MacOS/visicut-rust").is_file():
            parser.error("Build the native application with bundle-macos.sh first")
        archive = dist / f"{name}.zip"
        subprocess.run([
            "ditto", "-c", "-k", "--sequesterRsrc", "--keepParent", str(app), str(archive),
        ], check=True)
    else:
        stage = dist / name
        if stage.exists():
            shutil.rmtree(stage)
        stage.mkdir(exist_ok=True)
        suffix = ".exe" if "windows" in target else ""
        binary = project / "target/release" / f"visicut-rust{suffix}"
        if not binary.is_file():
            parser.error("Build the portable application with cargo build --release first")
        shutil.copy2(binary, stage / f"visicut-rust{suffix}")
        server = project / "target/release" / f"visicut-cameraserver{suffix}"
        if not server.is_file():
            parser.error("Build all binaries with cargo build --release --bins first")
        shutil.copy2(server, stage / server.name)
        for source in [project / "README.md", project / "PROTOCOL.md",
                       project.parent / "COPYING", project.parent / "COPYING.LESSER"]:
            shutil.copy2(source, stage / source.name)
        (stage / "START-HERE.txt").write_text(
            "VisiCutRust - portable egui development interface\n\n"
            f"Start visicut-rust{suffix}. No Java is required.\n"
            "It offers the same features as the macOS app: object assignment, material\n"
            "library, text/shape/sign tools, copies, nesting, job preview with timeline,\n"
            "LTT iLaser 4000 devices, rotary jobs and the calibrated camera background.\n"
            f"visicut-cameraserver{suffix} serves photos from a capture command.\n"
            "Linux requires a graphical X11/Wayland session, OpenGL/EGL, libxkbcommon,\n"
            "and a desktop portal for file dialogs. Built on Ubuntu 24.04 (glibc 2.39).\n"
            "See README.md and PROTOCOL.md for features and hardware validation status.\n",
            encoding="utf-8",
        )
        if "windows" in target:
            archive = dist / f"{name}.zip"
            with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED) as output:
                for path in sorted(stage.iterdir()):
                    output.write(path, f"{name}/{path.name}")
        else:
            archive = dist / f"{name}.tar.gz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(stage, arcname=name)
    print(archive)


if __name__ == "__main__":
    main()
