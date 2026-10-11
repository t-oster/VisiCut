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
uses GitHub-hosted Windows, Linux and Apple Silicon Mac runners. Pull
requests, changes on `master`/`main`, and manual runs build downloadable
artifacts.

**Releases.** A push to `master` publishes a full GitHub release `v<version>`
(not a pre-release, marked as latest) when the version in
`visicut-rust/Cargo.toml` has no release yet; its description is the matching
section of [`visicut-rust/CHANGELOG.md`](visicut-rust/CHANGELOG.md). Later
pushes with the same version only build artifacts. To release, raise the
version in `Cargo.toml`/`Cargo.lock`, add a `## <version> – <date>` section to
the changelog and merge to `master`. Pushing a `v*` tag publishes or updates
the release of that tag (tag and Cargo version must match; tags with a suffix
such as `-rc.1` become pre-releases). Release archives include licenses and
documentation; `SHA256SUMS.txt` is attached.

**macOS signing.** Without secrets the app is signed ad hoc. With these
repository secrets it is signed with a Developer ID (hardened runtime) and,
if the Apple ID values are present too, notarized and stapled:

| Secret | Content |
| --- | --- |
| `MACOS_CERTIFICATE` | Developer ID Application certificate with private key as `.p12`, base64-encoded (`base64 -i cert.p12`) |
| `MACOS_CERTIFICATE_PASSWORD` | Password of the `.p12` file |
| `MACOS_SIGN_IDENTITY` | e.g. `Developer ID Application: Name (TEAMID)` |
| `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_PASSWORD` | Apple ID, team ID and an app-specific password for `notarytool` |

Locally, `MACOS_SIGN_IDENTITY=… bash scripts/bundle-macos.sh` signs with a
certificate from the login keychain.

## Original VisiCut

The original Java source tree and its LGPL notices are retained in this fork.
Its distribution workflow can be started manually. The upstream external
build server and legacy release upload only run for `t-oster/VisiCut`.

- [Original project](https://www.visicut.org)
- [Java development guide](https://github.com/t-oster/VisiCut/wiki/Development:-Getting-started)
- [LibLaserCut](https://github.com/t-oster/LibLaserCut)
- License: [LGPL v3 or later](COPYING.LESSER), with the accompanying [GPL v3](COPYING).
