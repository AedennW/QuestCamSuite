# Feature & Compatibility Matrix

Method: every feature from the project brief is classified as

- ✅ **Implemented** — works today against public APIs
- 🧪 **Experimental** — works, gated by Horizon OS version / permission rollout
- ⛔ **Platform-blocked** — no supported API exists; we say why instead of faking it
- 🔮 **Roadmap** — feasible with more work, not in this build

## 1. Camera system

| Feature | Status | Notes |
| --- | --- | --- |
| Passthrough camera feed (both eyes) | ✅ | Camera2 + Meta vendor keys `com.meta.extra_metadata.camera_source` / `.position` |
| Highest available resolution / fps | ✅/⛔ | 1280×960 @ 30 FPS is the **hardware ceiling** of the API pipeline; we request it, more does not exist |
| Exposure / contrast / saturation / sharpness | ⛔/🧪 | HAL exposes no color-adjust vendor controls; exposure compensation slider appears **only** if the HAL reports a non-zero range (currently reports none) |
| Autofocus / manual focus | ⛔ | Passthrough optics are fixed-focus (`LENS_INFO_MINIMUM_FOCUS_DISTANCE = 0`). Controls are hidden rather than broken |
| Digital zoom | 🧪 | Slider enabled only when `CONTROL_ZOOM_RATIO_RANGE` reports > 1.0× |
| Low-latency preview | ✅ | TextureView + repeating preview request (API latency 40–60 ms stated by Meta) |
| Image stabilization (EIS/OIS) | ⛔ | No stabilization modes reported by the HAL |
| HDR | ⛔ | No HDR/HLG capabilities on passthrough stream |
| Auto white balance | ✅ | AWB runs in AUTO mode by HAL default; no manual override exists to expose |
| Orientation / portrait-landscape | ✅ | 2D panel handles rotation; feed itself is fixed 4:3 per eye |
| Live status indicators (resolution/FPS) | ✅ | HUD chips bound to the active session |
| Camera diagnostics + auto-recovery | ✅ | Diagnostics tab + reopen on idle/failure paths |
| Quality presets (Maximum/Balanced/Performance/Low-latency/Custom) | ✅ | Video presets map onto real bitrate tiers of the 1280×960@30 stream; photo presets select format/quality |
| Tracking/IR cameras | ⛔ | Reserved for system SLAM; not enumerable through Camera2. Will never be exposed safely |
| Avatar/eye-tracking cameras | ⛔ | Separate permissions program, not public |

## 2. Destinations & storage

Internal MediaStore (DCIM/QuestCameraSuite), SAF folder picker, USB-OTG via
the same system picker, network providers that register a DocumentsProvider —
all ✅ with **persisted grants** across sessions. Folder browse/create — ✅.
Rename/delete/move/copy — ✅ for suite-owned files (Android forbids arbitrary
file-management without special access, by design).
Storage space display + 2 GiB low-space warning — ✅.
Auto date organisation — ✅. File sizes/formats — ✅. Overwrite prevention via
unique counter filenames — ✅. Transfer progress — ✅ for bulk export.
"PC companion app" — honest substitute: built-in LAN share server (pull from
any browser) ✅; bespoke desktop app 🔮.

## 3. Photo mode

Single / burst / self-timer / interval ✅. Screenshots of the feed ✅ (that is
what a frame capture is). Auto naming ✅ (template engine, unit-tested).
Previews/gallery/favorites/metadata/date/rotation (rotation n/a — feed is
upright) ✅. Crop/resize/adjustable compression: format + JPEG quality ✅;
in-app editor (crop UI) 🔮. PNG + JPEG export ✅ both (in-app YUV conversion).

## 4. Video studio

Start/stop ✅, pause/resume ✅ (API 24+), timer + live REC indicator ✅,
resolution/FPS selection ⛔/✅ (locked to 1280×960@30 — the only stream
that exists; displayed honestly), bitrate ✅ (2–40 Mbps + presets),
audio ✅ (48 kHz AAC stereo, mic permission-gated), audio monitoring (levels)
🔮, quality presets ✅ (Maximum/High/Balanced/Smooth/Custom),
auto file splitting ✅ (duration-based MediaRecorder rotation),
duration/storage estimates ✅ (bitrate × free-space estimate),
interrupted-recording recovery ✅ (IS_PENDING cleanup, short-clip discard),
MP4 ✅ (the supported muxer).

## 5. Passthrough viewer

Live feed fullscreen ✅, floating control panel ✅, minimal mode ✅,
snapshots ✅, recording toggle via Camera tab ✅.
**Distinction is on-screen**: the app shows the *raw camera feed*, not the
system's composited MR view. True immersive passthrough compositing requires
an OpenXR/Meta XR native activity — 🔮 roadmap.

## 6. Modes

Photo ✅, Video ✅, Live preview ✅, Passthrough ✅, Burst ✅, Interval ✅,
Timelapse 🧪 (interval + external assembly; in-app assembly 🔮),
Slow-motion ⛔ (30 FPS ceiling — chip explains it), Diagnostics mode ✅,
Gallery mode ✅.

## 7. Gallery

Browse/grid/list ✅, fullscreen viewer ✅, video playback ✅ (Media3),
sorting ✅, filename search ✅, type/date filters ✅/🧪 (date via sort),
favorites ✅, custom albums 🧪 (SAF folders + Move via export), file info ✅,
multi-select ✅, bulk export ✅ (to current destination), bulk delete ✅,
share sheet ✅, thumbnails ✅ (Coil), storage overview ✅.

## 8. Connectivity

Windows/Mac/phone transfer ✅ via LAN share (browser pull, same Wi-Fi, no
internet). QR pairing 🔮 (URL copy shipped instead). USB transfer ✅ (OTG as
destination / direct copy). Secure pairing 🧪 (LAN server is plain HTTP — use
trusted networks; auth 🔮). Transfer history 🧪 (snackbar events only).
Configurable transfer quality ⛔ (originals only — transcoding 🔮).
Offline camera operation ✅ fully.

## 9–13. UI, performance, settings, diagnostics

Dark spatial theme, rail navigation, large VR-friendly targets ✅.
Controller-friendly via standard Android focus; hand-tracking gestures are a
system feature of Horizon OS for 2D panels ✅(platform).
Interface scale/accent/start-screen/language hooks ✅.
Settings: launch, defaults, naming, storage, privacy (local-only, cache purge,
system permission link) ✅.
Diagnostics: permissions, camera enumeration with vendor tags, streams, fps
ranges, AF/zoom/EV support, encoders (H.264/HEVC/AAC), storage, thermal,
battery ✅ with copyable text report.

## 14–15. APK + extras

Custom overlays (grid/timestamp/watermark) ✅, recording overlays ✅,
quick floating toolbar 🧪 (passthrough panel), auto-organisation ✅,
auto-backup 🧪 (LAN pull), recording notifications 🧪 (channel registered),
capture profiles = presets ✅, preset import/export 🔮,
cleanup recommendations ✅ (low-space), metadata viewer ✅ (info dialog),
capture history ✅ (gallery), thumbnails ✅, session folders ✅ (date),
custom naming ✅, performance benchmarking 🔮 (thermal/status shown),
update notifications 🔮 (no auto-update in sideload builds),
experimental section → Passthrough viewer + HEVC toggle ✅.

## Hard platform limits (for the record)

1. **1280×960 @ 30 FPS per eye, 40–60 ms latency** — Meta's stated ceiling.
2. **No access to IR tracking, hand-tracking or depth sensors as cameras.**
3. **No AF/zoom/HDR controls** — HAL reports none; we probe and hide.
4. **MediaProjection ≠ camera** — casting POV capture is privileged on Quest
   and excluded by design.
5. **Store distribution of camera apps** requires Meta's data-use review —
   sideloading is unaffected.
