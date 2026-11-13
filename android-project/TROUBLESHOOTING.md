# Troubleshooting — Quest Camera Suite

Open the in-app **Diagnostics** tab first; it probes the real device state and
prints reasons. The entries below mirror what it can report.

## Installation

**"Install blocked / unknown sources"**
Enable Developer Mode in the Meta Horizon phone app → your headset → Developer
settings, then reinstall via `adb install -r MetaQuestCamera-debug.apk` or
SideQuest.

**`adb devices` shows nothing**
Reboot the headset while connected, accept the "Allow USB debugging" prompt
inside the headset, try a different USB-C cable/port.

## Permissions

**Passthrough preview never starts / banner stays visible**
Both `android.permission.CAMERA` *and* `horizonos.permission.HEADSET_CAMERA`
are required. If you denied once, open
Settings → Privacy & safety → App permissions on the headset and grant both.
Pre-v74 Horizon OS does not know the headset permission at all — update the OS.

**"No passthrough camera reported by Camera2"**
Checklist: Horizon OS ≥ v76 · device is Quest 3/3S · system passthrough
enabled · both permissions granted → reboot. Diagnostics lists every camera
the HAL exposes.

## Capture

**Black or frozen preview after switching tabs quickly**
The HAL has a short reopen budget. Tap another tab and back, or use
Diagnostics → Re-run. Persistent case: reboot (camera service wedged —
`ERROR_CAMERA_SERVICE`).

**Recording stops with "Clip too short — discarded"**
MediaRecorder throws when stopped within ~300 ms. Not a bug; the corrupt stub
file is deleted automatically.

**"Recording failed after ~4 GB on USB"**
Some OTG drives are FAT32 with 4 GiB file limits. Enable **Auto-split
(5/10 min)** in video settings or reformat the drive.

**Silent video**
Grant the Microphone permission; toggle Microphone on in video controls. The
HUD notes when a session records without mic.

## Storage

**SAF destination forgot itself**
Some providers revoke persisted grants after eject/reboot. The app detects
this and falls back to internal storage — re-pick the folder.

**"Under 2 GiB free" warning persists**
Delete clips in Gallery (multi-select → Delete), or move the destination to a
USB drive. MediaStore deletions by third-party file managers can require a
rescan — pull-to-refresh via reopening Gallery.

## LAN share

**Browser can't open the share URL**
Both devices must be on the same Wi-Fi; guest/AP-isolated networks block
peer traffic. Verify the URL shows your headset IP (`192.168.x.x`), confirm
Local-only mode is off, and restart the toggle.

**Wrong/old file list**
The index reflects the suite library at request time — hit Refresh in the
Gallery tab, then reload the browser page.

## Performance & thermals

**Long recordings stutter or stop near thermal limits**
Horizon OS throttles camera+encoder under heat. Diagnostics shows the thermal
state; prefer BALANCED preset, take headset breaks, keep play area ventilated.

**40–60 ms preview latency is expected** — that is Meta's documented floor for
this API, not app overhead.

## Known limitations (by design)

Tracking/IR cameras, 4K capture, autofocus/zoom dials, HDR — unavailable
through any supported API on this hardware today. See FEATURES.md for the
full matrix and the reasoning we surface in-app.
