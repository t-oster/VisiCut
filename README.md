# VisiCutRust

Rust implementation of VisiCut maintained by [dbambus](https://github.com/dbambus),
forked from [t-oster/VisiCut](https://github.com/t-oster/VisiCut).
Prepare SVG laser jobs, save `.vcr` projects, and export or send LTT jobs for
the FAU FabLab LTT iLaser 4000.

## Downloads

Get the packages from [GitHub Releases](https://github.com/dbambus/VisiCutRust/releases).
Versions before 1.0 are published as development prereleases.

| Platform | Package | Interface |
| --- | --- | --- |
| Windows x64 | `VisiCutRust-<version>-x86_64-pc-windows-msvc.zip` | Portable egui application |
| Linux x64 | `VisiCutRust-<version>-x86_64-unknown-linux-gnu.tar.gz` | Portable egui application; Ubuntu 24.04 or compatible |
| macOS Universal (Intel + Apple Silicon) | `VisiCutRust-<version>-universal-apple-darwin.zip` | Native AppKit/SwiftUI; macOS 14+ |

No Java is required. macOS uses a native AppKit/SwiftUI interface, Windows
and Linux an egui interface in their standard style; both offer the same
functions. macOS applications are ad hoc signed and not Apple-notarized.
See the [implementation documentation](visicut-rust/README.md) for build
instructions, platform requirements and features, and the
[protocol notes](visicut-rust/PROTOCOL.md) for hardware validation status.

## Build and release

The Rust implementation is in [`visicut-rust`](visicut-rust). Its Cargo package
and executable are called `visicut-rust`; the application is named **VisiCutRust**.
Rust 1.90.0 is selected by the included toolchain file.

```sh
cd visicut-rust
cargo test --locked --all-targets
cargo build --locked --release --bin visicut-rust
# Native macOS app:
bash scripts/bundle-macos.sh
```

[GitHub Actions](https://github.com/dbambus/VisiCutRust/actions/workflows/rust-desktop.yml)
uses GitHub-hosted Windows, Linux, Intel Mac and Apple Silicon Mac runners.
Pull requests, changes on `master`/`main`, and manual runs build downloadable
artifacts. Pushing a `v*` tag also publishes a release after all four builds
and tests pass. The two Mac builds are combined with `lipo` into one Universal
application, signed again, and tested on both architectures before publishing.
The tag must match the version in `visicut-rust/Cargo.toml`.

To publish the current version from a clean checkout of `master`:

```sh
git tag v0.5.0
git push origin v0.5.0
```

For subsequent releases, update `Cargo.toml` and `Cargo.lock`, commit and push,
then push the matching new version tag. Tags must be pushed by a maintainer
with write access. Release archives include licenses and documentation;
`SHA256SUMS.txt` is attached to each release. Signing certificates and a
self-hosted runner are not required. Native macOS UI checks also save a
screenshot and log under the workflow artifacts.

## Original VisiCut

The original Java source tree and its LGPL notices are retained in this fork.
Its distribution workflow can be started manually. The upstream external
build server and legacy release upload only run for `t-oster/VisiCut`.

- [Original project](https://www.visicut.org)
- [Java development guide](https://github.com/t-oster/VisiCut/wiki/Development:-Getting-started)
- [LibLaserCut](https://github.com/t-oster/LibLaserCut)
- License: [LGPL v3 or later](COPYING.LESSER), with the accompanying [GPL v3](COPYING).
