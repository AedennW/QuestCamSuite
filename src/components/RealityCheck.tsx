import { Ban, CheckCircle2, FlaskConical, ShieldCheck } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Reveal } from "./Reveal";

type Item = { title: string; note: string };
type Column = {
  tag: string;
  icon: LucideIcon;
  hue: string;
  border: string;
  bg: string;
  items: Item[];
};

const COLUMNS: Column[] = [
  {
    tag: "Ships in v1.0",
    icon: CheckCircle2,
    hue: "text-ok",
    border: "border-ok/25",
    bg: "bg-ok/10",
    items: [
      { title: "Passthrough feed, both eyes", note: "Camera2 + Meta vendor keys, eye switcher included" },
      { title: "Photo studio", note: "Single · burst · timer · interval — JPEG or PNG" },
      { title: "Video lab", note: "H.264/HEVC presets, pause/resume, auto-split, 48 kHz AAC" },
      { title: "Destination vault", note: "Internal MediaStore, SAF folders, USB-OTG — grants persist" },
      { title: "Gallery & metadata", note: "Grid/list, favorites, search, bulk export, Media3 playback" },
      { title: "LAN share to PC/phone", note: "Built-in HTTP server — same Wi-Fi, zero internet" },
      { title: "Live diagnostics probe", note: "Permissions, HAL streams, encoders, thermal, storage" },
      { title: "Overlays & theming", note: "Grid, timestamp, watermark, accents, UI scale" },
    ],
  },
  {
    tag: "Gated / experimental",
    icon: FlaskConical,
    hue: "text-am",
    border: "border-am/25",
    bg: "bg-am/10",
    items: [
      { title: "HEADSET_CAMERA permission", note: "Horizon OS runtime grant — sideload + Dev Mode only" },
      { title: "Camera2 on Quest", note: "Needs Horizon OS ≥ v76; older builds report zero cameras" },
      { title: "Digital zoom slider", note: "Renders only if CONTROL_ZOOM_RATIO_RANGE > 1.0×" },
      { title: "HEVC recording", note: "Enabled after MediaCodecList probe finds an encoder" },
      { title: "Timelapse", note: "Interval capture ships; in-app assembly is roadmap" },
      { title: "Full MR composite", note: "Immersive passthrough needs an OpenXR native path" },
      { title: "Store distribution", note: "%s".replace("%s", "Camera apps require Meta's data-use review") },
      { title: "QR pairing", note: "URL copy ships today; pairing flow is roadmap" },
    ],
  },
  {
    tag: "Blocked by platform",
    icon: Ban,
    hue: "text-bad",
    border: "border-bad/25",
    bg: "bg-bad/10",
    items: [
      { title: "IR tracking cameras", note: "Reserved for system SLAM — never enumerable" },
      { title: "Beyond 1280×960 @ 30 FPS", note: "Meta's documented pipeline ceiling per eye" },
      { title: "Auto / manual focus", note: "Fixed-focus optics: LENS_MIN_FOCUS_DISTANCE = 0" },
      { title: "OIS / EIS toggle", note: "No stabilization modes exist on the passthrough HAL" },
      { title: "HDR modes", note: "No HDR/HLG capabilities on this stream" },
      { title: "Saturation / sharpness dials", note: "HAL exposes no color-tuning vendor controls" },
      { title: "POV screen capture", note: "MediaProjection is privileged on Quest — excluded" },
      { title: "Eye / face sensors", note: "Entitled-apps program, not a public API" },
    ],
  },
];

export function RealityCheck() {
  return (
    <section id="truth" className="relative mx-auto max-w-7xl px-5 py-28">
      <Reveal>
        <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
          01 — Reality check
        </p>
        <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
          No theater. <span className="clip-gradient">Just truth.</span>
        </h2>
        <p className="mt-4 max-w-2xl text-mist leading-relaxed">
          Before writing a line of code, the Quest 3's camera access was verified against
          Meta's current Passthrough Camera API documentation. This is the honest map the
          whole project is built on.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-5 lg:grid-cols-3">
        {COLUMNS.map((col, ci) => (
          <Reveal key={col.tag} delay={ci * 120}>
            <div className={`glass h-full rounded-3xl border ${col.border} p-6`}>
              <div className="flex items-center gap-3">
                <span className={`grid size-10 place-items-center rounded-xl ${col.bg} ${col.hue}`}>
                  <col.icon size={18} />
                </span>
                <h3 className={`font-display text-sm font-semibold tracking-[0.16em] uppercase ${col.hue}`}>
                  {col.tag}
                </h3>
              </div>
              <ul className="mt-6 space-y-4">
                {col.items.map((it) => (
                  <li key={it.title} className="group">
                    <p className="text-sm font-medium text-frost transition group-hover:text-cy">
                      {it.title}
                    </p>
                    <p className="mt-0.5 text-xs leading-relaxed text-mist">{it.note}</p>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
        ))}
      </div>

      <Reveal delay={200}>
        <div className="glass mt-8 flex flex-col items-start gap-3 rounded-3xl border border-cy/20 p-6 sm:flex-row sm:items-center">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-cy/10 text-cy">
            <ShieldCheck size={20} />
          </span>
          <p className="text-sm leading-relaxed text-mist">
            <span className="font-semibold text-frost">The app enforces this table at runtime.</span>{" "}
            Every control is probed against the live HAL before it's drawn — if the platform
            doesn't back a feature, the UI renders it disabled with the reason attached,
            never as a dead switch.
          </p>
        </div>
      </Reveal>
    </section>
  );
}
