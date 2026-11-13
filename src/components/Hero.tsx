import { Aperture, ArrowDownRight, Ban, Download, Radio, ScanLine } from "lucide-react";
import heroImg from "../assets/quest-hero.jpg";
import { PROJECT_ZIP_B64, PROJECT_ZIP_NAME } from "../assets/projectZip";

const STATS: Array<[string, string]> = [
  ["1280×960", "raw per eye"],
  ["30 FPS", "hardware ceiling"],
  ["40–60 ms", "API latency"],
  ["100%", "official APIs"],
];

const TICKER = [
  "PASSTHROUGH CAMERA API", "HORIZON OS v76+", "CAMERA2 HAL", "YUV_420_888",
  "H.264 / HEVC", "SAF DESTINATIONS", "USB-OTG READY", "LAN SHARE SERVER",
  "LIVE DIAGNOSTICS", "ADB SIDELOAD", "GRADLE 8.9", "JDK 17 TEMURIN",
];

export function Corner({ className = "" }: { className?: string }) {
  return (
    <span className={`pointer-events-none absolute h-5 w-5 border-cy/70 ${className}`} />
  );
}

export function Hero() {
  return (
    <section id="top" className="relative overflow-hidden gridlines">
      {/* ambient glows */}
      <div className="pointer-events-none absolute -left-40 top-24 size-[480px] rounded-full bg-vi/15 blur-[140px]" />
      <div className="pointer-events-none absolute -right-40 top-1/3 size-[520px] rounded-full bg-cy/10 blur-[160px]" />

      <div className="mx-auto grid max-w-7xl items-center gap-14 px-5 pb-24 pt-36 lg:grid-cols-[1.05fr_0.95fr] lg:pt-44">
        {/* copy */}
        <div className="relative">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-cy/25 bg-cy/5 px-4 py-1.5 font-mono text-[10px] tracking-[0.24em] text-cy uppercase">
            <ScanLine size={12} />
            Passthrough Camera API · Horizon OS v76+
          </div>

          <h1 className="font-display text-[13vw] font-bold leading-[0.95] tracking-tight sm:text-7xl lg:text-[5.2rem]">
            <span className="text-stroke block">SEE WHAT</span>
            <span className="clip-gradient block">THE HEADSET</span>
            <span className="block text-glow">SEES<span className="text-cy">.</span></span>
          </h1>

          <p className="mt-7 max-w-xl text-base leading-relaxed text-mist sm:text-lg">
            A complete, GitHub-ready Android Studio project that turns the Quest&nbsp;3's
            forward cameras into an honest capture suite — photo studio, video lab,
            destination vault, diagnostics — with an Actions pipeline that compiles,
            tests, signs and ships <span className="font-mono text-cy">MetaQuestCamera.apk</span> on every push.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-4">
            <a
              href={PROJECT_ZIP_B64}
              download={PROJECT_ZIP_NAME}
              className="group flex items-center gap-3 rounded-2xl bg-cy px-7 py-4 font-display text-sm font-semibold tracking-[0.12em] uppercase text-abyss transition hover:brightness-110"
            >
              <Download size={17} className="transition group-hover:translate-y-0.5" />
              Download project ZIP
            </a>
            <a
              href="#truth"
              className="flex items-center gap-2 rounded-2xl border border-line px-6 py-4 font-display text-sm font-semibold tracking-[0.12em] uppercase text-mist transition hover:border-cy/40 hover:text-cy"
            >
              The truth table
              <ArrowDownRight size={15} />
            </a>
          </div>

          <div className="mt-10 grid max-w-xl grid-cols-2 gap-3 sm:grid-cols-4">
            {STATS.map(([v, l]) => (
              <div key={l} className="glass rounded-xl px-4 py-3">
                <p className="font-display text-lg font-semibold text-frost">{v}</p>
                <p className="font-mono text-[9px] tracking-[0.18em] uppercase text-mist">{l}</p>
              </div>
            ))}
          </div>
        </div>

        {/* visual */}
        <div className="relative mx-auto w-full max-w-lg">
          <div className="animate-spin-slower pointer-events-none absolute -right-14 -top-14 size-40 rounded-full border border-dashed border-cy/30" />
          <div className="pointer-events-none absolute -left-10 bottom-10 size-24 rounded-full border border-vi/40" />

          <div className="glass-strong animate-floaty relative rounded-[2rem] p-3">
            <div className="relative overflow-hidden rounded-[1.6rem]">
              <img
                src={heroImg}
                alt="Meta Quest 3 headset render"
                className="h-full w-full object-cover"
              />
              <div className="scanbeam" />
              <Corner className="left-4 top-4 border-l-2 border-t-2" />
              <Corner className="right-4 top-4 border-r-2 border-t-2" />
              <Corner className="bottom-4 left-4 border-b-2 border-l-2" />
              <Corner className="bottom-4 right-4 border-b-2 border-r-2" />

              <div className="absolute left-5 top-5 flex items-center gap-2 rounded-full bg-abyss/70 px-3 py-1.5 font-mono text-[9px] tracking-[0.2em] text-ok uppercase backdrop-blur">
                <span className="animate-recpulse inline-block size-1.5 rounded-full bg-ok" />
                Camera2 online
              </div>

              <div className="absolute bottom-5 left-5 flex items-center gap-2 rounded-lg bg-abyss/70 px-3 py-2 font-mono text-[9px] tracking-[0.12em] text-cy uppercase backdrop-blur">
                <Aperture size={11} />
                RGB pods ×2 → app-accessible
              </div>
              <div className="absolute bottom-5 right-5 flex items-center gap-2 rounded-lg bg-abyss/70 px-3 py-2 font-mono text-[9px] tracking-[0.12em] text-bad uppercase backdrop-blur">
                <Ban size={11} />
                IR trackers ×4 → system only
              </div>
            </div>
          </div>

          <div className="glass absolute -left-6 top-10 hidden rounded-xl px-4 py-2.5 font-mono text-[10px] tracking-[0.14em] text-mist uppercase sm:block">
            <Radio size={11} className="mr-1.5 inline text-cy" />
            feed: raw, not composited
          </div>
        </div>
      </div>

      {/* ticker */}
      <div className="relative border-y border-line/60 bg-panel/60 py-3.5 backdrop-blur">
        <div className="animate-marquee flex w-max gap-10 whitespace-nowrap">
          {[...TICKER, ...TICKER].map((t, i) => (
            <span key={i} className="flex items-center gap-10 font-mono text-[11px] tracking-[0.26em] text-mist/70">
              {t}
              <span className="text-cy/60">·</span>
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}
