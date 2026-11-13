import {
  Activity, Aperture, Clapperboard, FolderCog, MoveRight, ScanEye,
  ShieldCheck, SlidersHorizontal, Video, Wifi, Workflow,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import circuitImg from "../assets/circuit-glow.jpg";
import { Reveal } from "./Reveal";

type Feature = {
  icon: LucideIcon;
  title: string;
  desc: string;
  tag: "SHIPS" | "EXPERIMENTAL" | "ROADMAP";
};

const FEATURES: Feature[] = [
  {
    icon: Video, tag: "SHIPS", title: "Camera2 Studio",
    desc: "Both passthrough eyes at the full 1280×960 @ 30 FPS pipeline, live HUD with resolution, FPS and active eye.",
  },
  {
    icon: Aperture, tag: "SHIPS", title: "Photo lab",
    desc: "Single, burst, self-timer and interval capture. JPEG quality dial or lossless PNG — converted in-app from raw YUV.",
  },
  {
    icon: FolderCog, tag: "SHIPS", title: "Destination vault",
    desc: "Internal MediaStore, any SAF folder, or USB-OTG — with persisted grants, date folders and overwrite-proof naming.",
  },
  {
    icon: Activity, tag: "SHIPS", title: "Live diagnostics",
    desc: "Probes permissions, vendor-key cameras, stream maps, encoders, thermal and battery — copyable as a text report.",
  },
  {
    icon: Wifi, tag: "SHIPS", title: "LAN share server",
    desc: "Zero-dependency HTTP server inside the APK. Pull originals from any PC or phone browser on the same network.",
  },
  {
    icon: SlidersHorizontal, tag: "SHIPS", title: "Real quality presets",
    desc: "Maximum / High / Balanced / Smooth / Custom bitrates mapped onto the actual 30 FPS hardware ceiling — no placebo tiers.",
  },
  {
    icon: ShieldCheck, tag: "SHIPS", title: "Privacy-first core",
    desc: "Runtime permissions, local-only mode, cache purge, no analytics, no accounts. The camera indicator is enforced by Horizon OS.",
  },
  {
    icon: Workflow, tag: "SHIPS", title: "CI-built APK",
    desc: "Kotlin compile, JVM tests, lint report, assemble + aapt/apksigner verification on every push; tagged releases ship signed APKs.",
  },
  {
    icon: ScanEye, tag: "ROADMAP", title: "Immersive MR composite",
    desc: "Fullscreen composited passthrough needs an OpenXR / Meta XR native activity — tracked in the roadmap, not faked today.",
  },
  {
    icon: Clapperboard, tag: "EXPERIMENTAL", title: "Timelapse assembly",
    desc: "Interval frames capture today. In-app stitching to MP4 lands once the MediaCodec muxer path is stabilized.",
  },
];

const PIPELINE_NODES = [
  "RGB sensors", "Camera2 HAL", "CameraEngine", "Photo ▸ Video controllers",
  "DestinationManager", "MediaStore · SAF · OTG", "Gallery · LAN share", "Compose UI",
];

export function Features() {
  return (
    <section id="features" className="relative overflow-hidden py-28">
      <div className="absolute inset-0 opacity-[0.14]">
        <img src={circuitImg} alt="" className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-b from-abyss via-transparent to-abyss" />
      </div>

      <div className="relative mx-auto max-w-7xl px-5">
        <Reveal>
          <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
            03 — The suite
          </p>
          <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
            A suite, <span className="clip-gradient">not a shutter button.</span>
          </h2>
        </Reveal>

        <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          {FEATURES.map((f, i) => (
            <Reveal key={f.title} delay={(i % 5) * 80}>
              <div className="glass group h-full rounded-3xl p-5 transition duration-300 hover:-translate-y-1.5 hover:border-cy/30">
                <div className="flex items-center justify-between">
                  <span className="grid size-10 place-items-center rounded-xl bg-cy/10 text-cy transition group-hover:bg-cy/20">
                    <f.icon size={17} />
                  </span>
                  <span
                    className={`rounded-full px-2.5 py-1 font-mono text-[8px] tracking-[0.16em] ${
                      f.tag === "SHIPS"
                        ? "bg-ok/10 text-ok"
                        : f.tag === "EXPERIMENTAL"
                          ? "bg-am/10 text-am"
                          : "bg-vi/15 text-vi"
                    }`}
                  >
                    {f.tag}
                  </span>
                </div>
                <p className="mt-4 font-display text-sm font-semibold tracking-wide">{f.title}</p>
                <p className="mt-2 text-xs leading-relaxed text-mist">{f.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>

        {/* signal path */}
        <Reveal delay={150}>
          <div className="glass-strong mt-10 rounded-3xl p-6">
            <p className="font-mono text-[10px] tracking-[0.24em] uppercase text-mist">
              Signal path · one process, zero magic
            </p>
            <div className="mt-5 flex flex-wrap items-center gap-x-3 gap-y-3">
              {PIPELINE_NODES.map((n, i) => (
                <div key={n} className="flex items-center gap-3">
                  <span
                    className={`rounded-xl border px-4 py-2.5 font-mono text-[10px] tracking-[0.08em] ${
                      i === 2 || i === 3
                        ? "border-cy/50 bg-cy/10 text-cy"
                        : "border-line bg-panel text-mist"
                    }`}
                  >
                    {n}
                  </span>
                  {i < PIPELINE_NODES.length - 1 && (
                    <MoveRight size={14} className="shrink-0 text-cy/50" />
                  )}
                </div>
              ))}
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
