<div align="center">

# Quest Camera Suite

**An honest, advanced camera & capture suite for Meta Quest 3 / 3S — built entirely on officially supported APIs.**

![Platform](https://img.shields.io/badge/platform-Android%2014%20%C2%B7%20Horizon%20OS%20v76%2B-1c1e26?style=for-the-badge&logo=meta)
![API](https://img.shields.io/badge/API-Camera2%20%2B%20Passthrough%20Camera%20API-45e8ff?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-8f7bff?style=for-the-badge&logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-3ce8a5?style=for-the-badge)
![Build](https://img.shields.io/github/actions/workflow/status/OWNER/REPO/android-build.yml?style=for-the-badge&label=CI)

[Features](#-feature-matrix) · [Quick start](#-quick-start) · [CI/CD](#-cicd-pipeline) · [AWS](#-building-on-aws) · [Troubleshooting](TROUBLESHOOTING.md)

</div>

---

## What it is

Quest 3's stock software gives you system recording and casting — but no manual camera
app for the passthrough hardware. **Quest Camera Suite fills that gap as far as the
platform honestly allows.**

Before any code was written, the Quest 3's camera access restrictions were verified
against Meta's current documentation: the **Passthrough Camera API** (Horizon OS v74+,
Camera2 from v76+) exposes the two forward-facing **RGB cameras** at up to
**1280×960 @ 30 FPS per eye** with 40–60 ms latency. Everything this app does is built
on that pipeline — and everything it refuses to fake is documented below and inside
[`FEATURES.md`](FEATURES.md).

> **Unofficial, open-source project.** Not affiliated with Meta Platforms, Inc.
> No undocumented hardware access, no Quest system modifications, no simulated controls.

---

## The reality check (read this first)

| | Capability | Why |
|---|---|---|
| ✅ | Passthrough RGB cameras, both eyes | Camera2 + Meta vendor keys `com.meta.extra_metadata.camera_source` / `.position` |
| ✅ | 1280×960 @ 30 FPS feed | The pipeline's documented maximum — we request every bit of it |
| ✅ | Photos, video, audio, storage, sharing | Standard, supported Android APIs (Camera2, MediaRecorder, MediaStore, SAF) |
| 🧪 | Digital zoom / exposure comp | Applied **only if** the HAL reports support; controls hide otherwise |
| 🧪 | HEVC, AF modes | Probed from `MediaCodecList` / `CameraCharacteristics` at runtime |
| ⛔ | IR tracking cameras | Reserved for system SLAM — never enumerable through Camera2 |
| ⛔ | Higher resolution / FPS / slow-mo | 30 FPS is the hardware ceiling of this API |
| ⛔ | Autofocus, OIS/EIS, HDR, color dials | The passthrough HAL exposes none of them |
| ⛔ | POV screen capture | `MediaProjection` is privileged on Quest — excluded by design |

The app enforces this table at runtime: every control is probed before it's drawn, so
you never see a dead switch. The built-in **Diagnostics** tab shows the live result of
each probe, with reasons attached.

---

## Feature matrix

<details open>
<summary><b>&nbsp;✅ Implemented in v1.0</b></summary>

| Area | Highlights |
|---|---|
| **Camera** | Both passthrough eyes, live 1280×960 feed, eye switcher, latency-first preview session |
| **Photo studio** | Single · burst · self-timer · interval — JPEG (quality dial) or lossless PNG, converted in-app from raw YUV |
| **Video lab** | H.264/HEVC presets (Maximum → Custom), pause/resume, live REC indicator, auto-split for long recordings, too-short clip recovery |
| **Destination vault** | Internal MediaStore, any SAF folder, USB-OTG drives — persisted grants across sessions, date auto-organization, overwrite-proof naming, low-space warnings |
| **Gallery** | Grid/list, search, sort, favorites, multi-select, bulk export, bulk delete, rename, share sheet, Media3 playback |
| **Connectivity** | Built-in zero-dependency LAN share server — pull originals from any PC/phone browser on the same Wi-Fi; no internet, no account |
| **Diagnostics** | Live probes: permissions, vendor-key cameras, stream maps, fps ranges, encoders, storage, thermal, battery — copyable as a text report |
| **UX** | Dark spatial theme, rail navigation, overlays (grid / timestamp / watermark), accents, UI scale, per-mode HUD |

</details>

<details>
<summary><b>&nbsp;🧪 Gated / experimental</b></summary>

- `HEADSET_CAMERA` runtime permission (sideload + Developer Mode only)
- Requires Horizon OS **v76+** — older builds report zero cameras
- Timelapse: frames capture today; in-app MP4 assembly is roadmap
- Full immersive MR composite needs an OpenXR native activity — roadmap
- Store distribution of camera apps requires Meta's data-use review; sideloading is unaffected

</details>

The complete, per-requested-feature accounting — including every ⛔ with its platform
reason — lives in [`FEATURES.md`](FEATURES.md).

---

## Quick start

### Build the APK

```bash
git clone https://github.com/OWNER/REPO.git
cd REPO

gradle wrapper --gradle-version 8.9   # once, if the wrapper jar isn't committed
gradle testDebugUnitTest              # JVM unit tests
gradle assembleDebug                  # → app/build/outputs/apk/debug/app-debug.apk
```

Requirements: **JDK 17**, **Android SDK 34** + build-tools 34.0.0.
(Or just push to GitHub and let Actions build it — [see below](#-cicd-pipeline).)

### Install on Quest 3

1. Enable **Developer Mode** in the Meta Horizon phone app → your headset.
2. Connect USB-C (or wireless ADB), accept the debug prompt inside the headset.
3. Install:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

4. Launch → grant **Camera** *and* **Headset Camera**
   (`horizonos.permission.HEADSET_CAMERA`) → enable system passthrough.
5. Open the **Diagnostics** tab → *Re-run*. Every green row is a probe that just
   passed against your actual hardware.

---

## Architecture

One process, zero magic. All capture flows through officially supported APIs:

```
  RGB sensors ──▶ Camera2 HAL ──▶ CameraEngine ──▶ Photo / Video controllers
        (Meta vendor keys)              │                    │
                                        ▼                    ▼
                              DestinationManager ──▶ MediaStore · SAF · USB-OTG
                                        │
                                        ▼
                    Gallery (repository) ──▶ LAN share server ──▶ PC / phone browser
                                        │
                                        ▼
                              Compose UI + Diagnostics probe
```

```
app/src/main/java/com/camerasuite/quest/
├── MainActivity.kt            # single activity, permission + SAF wiring
├── CameraSuiteApp.kt          # Application, app-scope, event bus
├── camera/
│   ├── QuestCamera.kt         # Meta vendor keys + capability model
│   └── CameraEngine.kt        # Camera2 sessions, HAL-probed controls
├── capture/
│   ├── PhotoController.kt     # single/burst/timer/interval, YUV→JPEG/PNG
│   └── VideoController.kt     # MediaRecorder lab, pause/resume, auto-split
├── storage/
│   ├── DestinationManager.kt  # MediaStore + SAF destinations, persisted grants
│   ├── MediaRepository.kt     # gallery index over MediaStore
│   └── Naming.kt              # file-name template engine (unit-tested)
├── diagnostics/
│   └── DiagnosticsCollector.kt# live HAL/permission/encoder/thermal probes
├── settings/
│   └── SettingsStore.kt       # DataStore-backed settings
├── share/
│   └── HttpShareServer.kt     # zero-dependency LAN share server
└── ui/                        # Compose screens, rail nav, HUD studio
```

---

## CI/CD pipeline

[`.github/workflows/android-build.yml`](.github/workflows/android-build.yml) runs on
**pushes to main, PRs, manual dispatch, and `v*` tags**:

| Stage | What happens |
|---|---|
| Setup | JDK 17 (Temurin) · Android SDK 34 · Gradle 8.9 · wrapper regeneration |
| Validate | Kotlin compile · `testDebugUnitTest` · lint (report-only) |
| Build | `assembleDebug` + `assembleRelease` |
| Sign | Release signed **only** when secrets are configured |
| Verify | APK exists · manifest unpacks · `aapt` dump · `apksigner verify` |
| Artifact | `MetaQuestCamera-debug.apk` + `MetaQuestCamera.apk` uploaded |
| Release | On `v*` tags: GitHub Release with APKs + notes attached |

```bash
git tag v1.0.0 && git push origin v1.0.0    # cut a release build
```

### Signing secrets (optional — never commit keys)

| Secret | Content |
|---|---|
| `QUEST_KEYSTORE_BASE64` | `base64 -w0 release.keystore` |
| `QUEST_KEYSTORE_PASSWORD` | keystore password |
| `QUEST_KEY_ALIAS` | key alias |
| `QUEST_KEY_PASSWORD` | key password |

Set repo variable `QUEST_SIGNING_ENABLED = true` to activate. Without it, CI ships a
**debug-signed APK that installs via ADB immediately** — no setup penalty.

### What CI actually proves

Compilation, unit tests, APK structure, manifest, permissions, signature validity.
**Not** on-headset behavior — Camera2 session negotiation only exists on physical
hardware. Flash the debug build and check Diagnostics; report deltas as issues.

---

## Building on AWS

Prefer AWS over GitHub-hosted runners? Three routes, files included under [`aws/`](aws/):

1. **EC2 self-hosted runner** *(recommended)* — register an EC2 instance in
   *Actions → Runners* with label `android`, change `runs-on:` to
   `[self-hosted, linux, x64, android]`, stop it when idle (~$0.01–0.02/hr spot).
2. **CodeBuild + CodePipeline** — build [`aws/Dockerfile`](aws/Dockerfile), push to
   ECR, point a CodeBuild project at [`aws/buildspec.yml`](aws/buildspec.yml).
   No KVM → no emulator → unit tests + APK assembly only. Secrets via Secrets Manager.
3. **Device Farm** — real-device validation for the part CI can't prove
   (Meta used this service for Quest testing at re:Invent 2024).

---

## Documentation

| File | Contents |
|---|---|
| [`FEATURES.md`](FEATURES.md) | Every requested feature: implemented / experimental / platform-blocked, with reasons |
| [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) | Install, permission, capture, storage, LAN-share, thermal fixes |
| [`aws/`](aws/) | CodeBuild buildspec + custom build-image Dockerfile |

> **Screenshots in this README?** Intentionally absent — they're only honest after
> validation on real hardware. Add your own under `docs/` once you've flashed it.

---

## Roadmap

- [ ] OpenXR / Meta XR native activity for a true immersive MR composite
- [ ] In-app interval → timelapse MP4 assembly
- [ ] QR-code pairing for the LAN share server
- [ ] Crop/resize photo editor
- [ ] Preset import / export
- [ ] Performance benchmarking screen

---

## Contributing

Issues and PRs welcome. The one rule: **no fake capability** — a feature either works
against a supported API, or it's presented as unavailable with the reason. If you find
a new HAL capability on a newer Horizon OS build (zoom range, EV, AF mode…), the engine
probes it and the UI unlocks automatically — the PR only needs a note in `FEATURES.md`.

---

## License & disclaimer

MIT. The Quest 3, Horizon OS and all Meta platform trademarks remain with Meta
Platforms, Inc. This project is unofficial and not affiliated with Meta. It uses no
undocumented hardware access and modifies no system software.
