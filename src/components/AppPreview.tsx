import {
  Activity, Aperture, Camera, Check, ChevronDown, Clock3, Copy, FolderCog,
  Grid3x3, Heart, Images, Layers, Lock, Mic, Pause, Play, HardDrive, Square,
  Timer, Trash2, Usb, Video, Wifi, X, Zap,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import feedImg from "../assets/passthrough-view.jpg";
import { Reveal } from "./Reveal";

type Tab = "camera" | "gallery" | "storage" | "diag";
type Mode = "PHOTO" | "BURST" | "TIMER" | "INTERVAL" | "VIDEO";
type Dest = "internal" | "saf" | "usb";

type Shot = {
  id: number;
  name: string;
  kind: "photo" | "video";
  fav: boolean;
  size: string;
  duration?: string;
  filter: string;
};

const FILTERS = [
  "none",
  "hue-rotate(24deg) saturate(1.2)",
  "hue-rotate(-30deg) brightness(1.08)",
  "saturate(0.4) brightness(1.12) hue-rotate(12deg)",
  "hue-rotate(60deg) saturate(0.9)",
  "brightness(0.92) saturate(1.35)",
  "hue-rotate(-70deg) brightness(1.05)",
  "saturate(0.7) hue-rotate(40deg)",
];

const SEED: Shot[] = FILTERS.map((f, i) => ({
  id: i + 1,
  name: `QCS_photo_2026-02-1${(i % 9) + 1}_0${i % 6}${(i * 7) % 10}-2${i % 10}_00${i + 1}.jpg`,
  kind: i % 3 === 2 ? "video" : "photo",
  fav: i === 2 || i === 5,
  size: `${(0.7 + (i % 5) * 0.4).toFixed(1)} MB`,
  duration: i % 3 === 2 ? `00:${20 + i * 3}` : undefined,
  filter: f,
}));

function renderName(template: string, mode: Mode, counter: number, ext: string) {
  const d = new Date();
  const pad = (n: number, w = 2) => String(n).padStart(w, "0");
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const time = `${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`;
  let out = template || "QCS_{mode}_{date}_{time}_{counter}";
  out = out
    .split("{date}").join(date)
    .split("{time}").join(time)
    .split("{mode}").join(mode.toLowerCase())
    .split("{counter}").join(pad(counter, 3))
    .replace(/[^A-Za-z0-9._-]/g, "_");
  if (!out.toLowerCase().endsWith(`.${ext}`)) out += `.${ext}`;
  return out;
}

function fmtClock(s: number) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(Math.floor(s / 3600))}:${pad(Math.floor((s % 3600) / 60))}:${pad(s % 60)}`;
}

function Pill({ children, tone = "cy" }: { children: React.ReactNode; tone?: "cy" | "bad" | "ok" }) {
  const map = {
    cy: "bg-abyss/70 text-cy",
    bad: "bg-bad/90 text-white",
    ok: "bg-abyss/70 text-ok",
  };
  return (
    <span className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 font-mono text-[10px] tracking-[0.12em] uppercase backdrop-blur ${map[tone]}`}>
      {children}
    </span>
  );
}

function Switch({ on, onToggle, disabled = false }: { on: boolean; onToggle: () => void; disabled?: boolean }) {
  return (
    <button
      onClick={onToggle}
      disabled={disabled}
      className={`relative h-6 w-11 rounded-full border transition ${
        on ? "border-cy/60 bg-cy/30" : "border-line bg-panel"
      } ${disabled ? "opacity-40 cursor-not-allowed" : "cursor-pointer"}`}
    >
      <span
        className={`absolute top-0.5 size-4.5 rounded-full transition-all ${
          on ? "left-[22px] bg-cy" : "left-0.5 bg-mist"
        }`}
      />
    </button>
  );
}

export function AppPreview() {
  const [tab, setTab] = useState<Tab>("camera");
  const [mode, setMode] = useState<Mode>("PHOTO");
  const [shots, setShots] = useState<Shot[]>(SEED);
  const [counter, setCounter] = useState(SEED.length + 1);
  const [template, setTemplate] = useState("QCS_{mode}_{date}_{time}_{counter}");
  const [format, setFormat] = useState<"JPEG" | "PNG">("JPEG");
  const [quality, setQuality] = useState(92);
  const [preset, setPreset] = useState("BALANCED");
  const [audio, setAudio] = useState(true);
  const [grid, setGrid] = useState(false);
  const [timestamp, setTimestamp] = useState(true);
  const [watermark, setWatermark] = useState("");
  const [dest, setDest] = useState<Dest>("internal");
  const [shareOn, setShareOn] = useState(false);
  const [flash, setFlash] = useState(0);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [intervalOn, setIntervalOn] = useState(false);
  const [recording, setRecording] = useState(false);
  const [paused, setPaused] = useState(false);
  const [recSec, setRecSec] = useState(0);
  const [fps, setFps] = useState(30);
  const [latency, setLatency] = useState(47);
  const [viewer, setViewer] = useState<Shot | null>(null);
  const [filter, setFilter] = useState<"ALL" | "PHOTOS" | "VIDEOS" | "FAVS">("ALL");
  const [toast, setToast] = useState<string | null>(null);
  const [now, setNow] = useState(new Date());
  const toastTimer = useRef<number | undefined>(undefined);

  const say = (msg: string) => {
    setToast(msg);
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(null), 2200);
  };

  // live HUD tickers
  useEffect(() => {
    const id = window.setInterval(() => {
      setFps(28 + Math.round(Math.random() * 3));
      setLatency(41 + Math.round(Math.random() * 17));
      setNow(new Date());
    }, 1300);
    return () => window.clearInterval(id);
  }, []);

  // recording timer
  useEffect(() => {
    if (!recording || paused) return;
    const id = window.setInterval(() => setRecSec((s) => s + 1), 1000);
    return () => window.clearInterval(id);
  }, [recording, paused]);

  // interval capture loop
  useEffect(() => {
    if (!intervalOn) return;
    const id = window.setInterval(() => captureFrame("INTERVAL"), 3000);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [intervalOn, counter, template, format]);

  function captureFrame(m: Mode, silent = false) {
    setFlash((f) => f + 1);
    const id = counter;
    setCounter((c) => c + 1);
    const name = renderName(template, m, id, format === "PNG" ? "png" : "jpg");
    setShots((s) => [
      {
        id,
        name,
        kind: "photo",
        fav: false,
        size: `${(0.6 + Math.random() * 1.4).toFixed(1)} MB`,
        filter: FILTERS[id % FILTERS.length],
      },
      ...s,
    ]);
    if (!silent) say(`Saved ${name}`);
  }

  function onShutter() {
    if (mode === "PHOTO") return captureFrame("PHOTO");
    if (mode === "BURST") {
      for (let i = 0; i < 5; i++) {
        window.setTimeout(() => captureFrame("BURST", i < 4), i * 190);
      }
      return;
    }
    if (mode === "TIMER") {
      if (countdown !== null) return setCountdown(null);
      let n = 3;
      setCountdown(n);
      const id = window.setInterval(() => {
        n -= 1;
        if (n <= 0) {
          window.clearInterval(id);
          setCountdown(null);
          captureFrame("TIMER");
        } else setCountdown(n);
      }, 900);
      return;
    }
    if (mode === "INTERVAL") {
      setIntervalOn((v) => {
        say(v ? "Interval capture stopped" : "Interval capture running — every 3 s");
        return !v;
      });
      return;
    }
    if (mode === "VIDEO") {
      if (recording) {
        setRecording(false);
        setPaused(false);
        const id = counter;
        setCounter((c) => c + 1);
        const name = renderName(template, "VIDEO", id, "mp4");
        setShots((s) => [
          {
            id, name, kind: "video", fav: false,
            size: `${(recSec * 1.6 + 2).toFixed(1)} MB`,
            duration: fmtClock(recSec).slice(3),
            filter: FILTERS[id % FILTERS.length],
          },
          ...s,
        ]);
        say(`Saved ${name} (${fmtClock(recSec)})`);
        setRecSec(0);
      } else {
        setRecording(true);
        setRecSec(0);
        say(audio ? "REC — mic live" : "REC — mic off");
      }
    }
  }

  const gallery = useMemo(() => {
    switch (filter) {
      case "PHOTOS": return shots.filter((s) => s.kind === "photo");
      case "VIDEOS": return shots.filter((s) => s.kind === "video");
      case "FAVS": return shots.filter((s) => s.fav);
      default: return shots;
    }
  }, [shots, filter]);

  const destLabel = {
    internal: "DCIM/QuestCameraSuite",
    saf: "SAF · Pictures/QCS",
    usb: "USB-OTG · ESD-128G",
  }[dest];

  const TABS: Array<{ id: Tab; icon: typeof Camera; label: string }> = [
    { id: "camera", icon: Camera, label: "Studio" },
    { id: "gallery", icon: Images, label: "Gallery" },
    { id: "storage", icon: FolderCog, label: "Storage" },
    { id: "diag", icon: Activity, label: "Diag" },
  ];

  const DIAG_ROWS: Array<{ ok: boolean | null; label: string; value: string; hint?: string }> = [
    { ok: true, label: "android.permission.CAMERA", value: "granted" },
    { ok: true, label: "horizonos.permission.HEADSET_CAMERA", value: "granted" },
    { ok: true, label: "Passthrough cameras", value: "2 found", hint: "source=0 · position L/R via Meta vendor keys" },
    { ok: true, label: "Streams (YUV_420_888)", value: "1280×960", hint: "30 FPS ceiling · 40–60 ms latency" },
    { ok: false, label: "AF / manual focus", value: "fixed optics", hint: "LENS_MIN_FOCUS_DISTANCE=0 — control hidden" },
    { ok: null, label: "Digital zoom", value: "1.0× only", hint: "HAL reports no zoom range — slider disabled" },
    { ok: true, label: "H.264 encoder", value: "c2.qti.avc" },
    { ok: true, label: "HEVC encoder", value: "c2.qti.hevc", hint: "toggle enabled in video settings" },
    { ok: true, label: "Thermal", value: "cool" },
    { ok: null, label: "MediaProjection", value: "privileged", hint: "casting is system-owned; excluded by design" },
  ];

  return (
    <section id="preview" className="relative mx-auto max-w-7xl px-5 py-28">
      <Reveal>
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
              02 — Interface concept
            </p>
            <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
              Touch the suite <span className="clip-gradient">before it boots.</span>
            </h2>
          </div>
          <div className="flex items-center gap-2 rounded-full border border-am/30 bg-am/10 px-4 py-2 font-mono text-[10px] tracking-[0.14em] uppercase text-am">
            <Zap size={12} />
            In-browser concept — the APK UI mirrors this
          </div>
        </div>
      </Reveal>

      <Reveal delay={120}>
        <div className="glass-strong relative mt-12 overflow-hidden rounded-[2rem]">
          {/* window bar */}
          <div className="flex items-center gap-3 border-b border-line/60 px-5 py-3">
            <div className="flex gap-1.5">
              <span className="size-2.5 rounded-full bg-bad/70" />
              <span className="size-2.5 rounded-full bg-am/70" />
              <span className="size-2.5 rounded-full bg-ok/70" />
            </div>
            <span className="font-mono text-[10px] tracking-[0.18em] text-mist">qcs://studio</span>
            <div className="ml-auto flex items-center gap-2">
              <Pill tone="ok">{fps} FPS</Pill>
              <Pill>{latency} ms</Pill>
            </div>
          </div>

          <div className="flex">
            {/* rail */}
            <div className="flex w-16 flex-col items-center gap-1.5 border-r border-line/60 py-4 sm:w-20">
              {TABS.map((t) => (
                <button
                  key={t.id}
                  onClick={() => setTab(t.id)}
                  className={`group flex w-full flex-col items-center gap-1 py-2 transition ${
                    tab === t.id ? "text-cy" : "text-mist hover:text-frost"
                  }`}
                >
                  <span
                    className={`grid size-10 place-items-center rounded-xl transition ${
                      tab === t.id ? "bg-cy/15 ring-1 ring-cy/40" : "group-hover:bg-panelhi"
                    }`}
                  >
                    <t.icon size={17} />
                  </span>
                  <span className="font-mono text-[8px] tracking-[0.16em] uppercase">{t.label}</span>
                </button>
              ))}
            </div>

            {/* main */}
            <div className="min-w-0 flex-1 p-4 sm:p-5">
              {tab === "camera" && (
                <div className="space-y-4">
                  {/* viewfinder */}
                  <div className="relative overflow-hidden rounded-2xl border border-line">
                    <img src={feedImg} alt="Passthrough feed" className="aspect-[4/3] w-full object-cover sm:aspect-[16/9]" />
                    {grid && (
                      <div className="pointer-events-none absolute inset-0 opacity-40">
                        {[1, 2].map((i) => (
                          <div key={`v${i}`} className="absolute top-0 bottom-0 w-px bg-cy/40" style={{ left: `${i * 33.33}%` }} />
                        ))}
                        {[1, 2].map((i) => (
                          <div key={`h${i}`} className="absolute left-0 right-0 h-px bg-cy/40" style={{ top: `${i * 33.33}%` }} />
                        ))}
                      </div>
                    )}
                    {/* focus brackets */}
                    {["left-5 top-5 border-l-2 border-t-2", "right-5 top-5 border-r-2 border-t-2", "left-5 bottom-5 border-l-2 border-b-2", "right-5 bottom-5 border-r-2 border-b-2"].map((c) => (
                      <span key={c} className={`pointer-events-none absolute h-6 w-6 border-cy/80 ${c}`} />
                    ))}

                    <div className="absolute left-4 top-4 flex flex-wrap gap-2">
                      {recording ? (
                        <Pill tone="bad">
                          <span className="animate-recpulse inline-block size-1.5 rounded-full bg-white" />
                          {paused ? `PAUSED ${fmtClock(recSec)}` : `REC ${fmtClock(recSec)}`}
                        </Pill>
                      ) : (
                        <>
                          <Pill>1280×960 · ≤30 FPS</Pill>
                          <Pill tone="ok">Passthrough · R eye</Pill>
                        </>
                      )}
                    </div>
                    <div className="absolute right-4 top-4">
                      <Pill>UI concept</Pill>
                    </div>

                    {countdown !== null && (
                      <div className="absolute inset-0 grid place-items-center bg-abyss/40 backdrop-blur-[2px]">
                        <span className="font-display text-8xl font-bold text-cy text-glow">{countdown}</span>
                      </div>
                    )}
                    {flash > 0 && <div key={flash} className="capture-flash absolute inset-0 bg-white/80" />}

                    {intervalOn && (
                      <div className="absolute bottom-4 left-1/2 -translate-x-1/2">
                        <Pill tone="ok">
                          <Timer size={11} /> interval live
                        </Pill>
                      </div>
                    )}
                    {timestamp && (
                      <span className="absolute bottom-4 left-4 font-mono text-[10px] tracking-[0.1em] text-white/85">
                        {now.toISOString().replace("T", " ").slice(0, 19)}
                      </span>
                    )}
                    {watermark && (
                      <span className="absolute bottom-4 right-4 font-mono text-[10px] tracking-[0.14em] text-white/70 uppercase">
                        {watermark}
                      </span>
                    )}
                  </div>

                  {/* mode chips */}
                  <div className="flex flex-wrap items-center gap-2">
                    {(["PHOTO", "BURST", "TIMER", "INTERVAL", "VIDEO"] as Mode[]).map((m) => (
                      <button
                        key={m}
                        onClick={() => setMode(m)}
                        className={`rounded-full px-4 py-2 font-mono text-[10px] tracking-[0.14em] transition ${
                          mode === m
                            ? "bg-cy text-abyss font-semibold"
                            : "border border-line text-mist hover:border-cy/40 hover:text-cy"
                        }`}
                      >
                        {m}
                      </button>
                    ))}
                    <span
                      title="Passthrough stream is capped at 30 FPS — no high-frame-rate mode exists"
                      className="flex cursor-not-allowed items-center gap-1.5 rounded-full border border-line/60 px-4 py-2 font-mono text-[10px] tracking-[0.14em] text-mist/40"
                    >
                      <Lock size={10} /> SLOW-MO
                    </span>
                  </div>

                  {/* shutter bar */}
                  <div className="flex items-center gap-4">
                    <button
                      onClick={() => shots.length && setViewer(shots[0])}
                      className="relative size-12 shrink-0 overflow-hidden rounded-xl border border-line transition hover:border-cy/50"
                    >
                      <img src={feedImg} alt="latest" style={{ filter: shots[0]?.filter }} className="h-full w-full object-cover" />
                    </button>
                    <span className="hidden font-mono text-[10px] tracking-[0.1em] text-mist sm:block">
                      {shots.length} captures
                    </span>

                    <div className="mx-auto flex items-center gap-4">
                      {recording && mode === "VIDEO" && (
                        <button
                          onClick={() => setPaused((p) => !p)}
                          className="grid size-10 place-items-center rounded-full border border-line text-mist transition hover:border-cy/50 hover:text-cy"
                        >
                          {paused ? <Play size={15} /> : <Pause size={15} />}
                        </button>
                      )}
                      <button
                        onClick={onShutter}
                        className="group grid size-20 place-items-center rounded-full border-[3px] border-cy transition hover:border-cy/80"
                        aria-label="shutter"
                      >
                        <span
                          className={`grid place-items-center rounded-full transition-all duration-300 ${
                            mode === "VIDEO" && recording
                              ? "size-8 rounded-md bg-bad"
                              : "size-14 bg-cy group-hover:scale-95"
                          }`}
                        >
                          {mode === "VIDEO" && recording && <Square size={12} className="text-white" />}
                          {mode === "TIMER" && countdown !== null && <X size={18} className="text-abyss" />}
                        </span>
                      </button>
                      <span className="w-10" />
                    </div>

                    <span className="hidden shrink-0 items-center gap-1.5 rounded-full border border-line px-3 py-1.5 font-mono text-[9px] tracking-[0.1em] text-mist md:flex">
                      <HardDrive size={11} className="text-cy" /> {destLabel}
                    </span>
                  </div>
                </div>
              )}

              {tab === "gallery" && (
                <div className="space-y-4">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-display text-sm font-semibold tracking-[0.12em] uppercase">Library</span>
                    <span className="font-mono text-[10px] text-mist">{gallery.length}</span>
                    <div className="ml-auto flex gap-1.5">
                      {(["ALL", "PHOTOS", "VIDEOS", "FAVS"] as const).map((f) => (
                        <button
                          key={f}
                          onClick={() => setFilter(f)}
                          className={`rounded-full px-3 py-1.5 font-mono text-[9px] tracking-[0.12em] transition ${
                            filter === f ? "bg-cy text-abyss" : "border border-line text-mist hover:text-cy"
                          }`}
                        >
                          {f}
                        </button>
                      ))}
                    </div>
                  </div>
                  <div className="grid max-h-[380px] grid-cols-2 gap-2.5 overflow-y-auto pr-1 sm:grid-cols-4">
                    {gallery.map((s) => (
                      <button
                        key={s.id}
                        onClick={() => setViewer(s)}
                        className="group relative overflow-hidden rounded-xl border border-line text-left transition hover:border-cy/40"
                      >
                        <div className="relative">
                          <img src={feedImg} alt={s.name} style={{ filter: s.filter }} className="aspect-[4/3] w-full object-cover" />
                          {s.kind === "video" && (
                            <span className="absolute bottom-1.5 right-1.5 rounded bg-abyss/80 px-1.5 py-0.5 font-mono text-[8px] text-white">
                              {s.duration}
                            </span>
                          )}
                          {s.fav && <Heart size={11} className="absolute left-1.5 top-1.5 fill-bad text-bad" />}
                        </div>
                        <div className="bg-panel px-2 py-1.5">
                          <p className="truncate font-mono text-[8px] text-mist group-hover:text-cy">{s.name}</p>
                          <p className="font-mono text-[8px] text-mist/50">{s.size}</p>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {tab === "storage" && (
                <div className="space-y-4">
                  <span className="font-display text-sm font-semibold tracking-[0.12em] uppercase">Destinations</span>
                  <div className="grid gap-2.5 sm:grid-cols-3">
                    {(
                      [
                        { id: "internal" as Dest, icon: HardDrive, title: "Internal", sub: "MediaStore · DCIM/QuestCameraSuite", note: "Fastest · indexable" },
                        { id: "saf" as Dest, icon: FolderCog, title: "SAF folder", sub: "Pictures/QCS · persisted grant", note: "Survives reinstalls" },
                        { id: "usb" as Dest, icon: Usb, title: "USB-OTG", sub: "ESD-128G via system picker", note: "118 GB free" },
                      ]
                    ).map((d) => (
                      <button
                        key={d.id}
                        onClick={() => { setDest(d.id); say(`Photo destination → ${d.title}`); }}
                        className={`rounded-2xl border p-4 text-left transition ${
                          dest === d.id ? "border-cy/60 bg-cy/10" : "border-line bg-panel hover:border-cy/30"
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <d.icon size={17} className={dest === d.id ? "text-cy" : "text-mist"} />
                          {dest === d.id && <Check size={14} className="text-cy" />}
                        </div>
                        <p className="mt-3 font-display text-sm font-semibold">{d.title}</p>
                        <p className="mt-1 font-mono text-[9px] leading-relaxed text-mist">{d.sub}</p>
                        <p className="mt-1 font-mono text-[8px] tracking-[0.12em] uppercase text-ok">{d.note}</p>
                      </button>
                    ))}
                  </div>

                  {/* storage meter */}
                  <div className="rounded-2xl border border-line bg-panel p-4">
                    <div className="flex justify-between font-mono text-[10px] text-mist">
                      <span>Headset storage</span>
                      <span>284 GB free / 512 GB</span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-line">
                      <div className="h-full w-[46%] rounded-full bg-gradient-to-r from-cy to-vi" />
                    </div>
                  </div>

                  {/* LAN share */}
                  <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-line bg-panel p-4">
                    <Wifi size={16} className={shareOn ? "text-ok" : "text-mist"} />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm">Local network share</p>
                      <p className="font-mono text-[9px] text-mist">
                        {shareOn ? "http://192.168.1.42:8137 — open on any same-Wi-Fi browser" : "off — zero network exposure"}
                      </p>
                    </div>
                    {shareOn && (
                      <button
                        onClick={() => { navigator.clipboard?.writeText("http://192.168.1.42:8137"); say("Share URL copied"); }}
                        className="grid size-8 place-items-center rounded-lg border border-line text-mist hover:text-cy"
                      >
                        <Copy size={13} />
                      </button>
                    )}
                    <Switch on={shareOn} onToggle={() => { setShareOn(!shareOn); say(shareOn ? "Share server stopped" : "Serving library on LAN"); }} />
                  </div>

                  {/* naming template */}
                  <div className="rounded-2xl border border-line bg-panel p-4">
                    <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-mist">File name template</p>
                    <input
                      value={template}
                      onChange={(e) => setTemplate(e.target.value)}
                      className="mt-2 w-full rounded-lg border border-line bg-abyss px-3 py-2 font-mono text-xs text-cy outline-none focus:border-cy/60"
                      spellCheck={false}
                    />
                    <p className="mt-2 font-mono text-[9px] text-mist">
                      {"{date} {time} {mode} {counter}"} → next: {renderName(template, mode, counter, format === "PNG" ? "png" : mode === "VIDEO" ? "mp4" : "jpg")}
                    </p>
                  </div>
                </div>
              )}

              {tab === "diag" && (
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <span className="font-display text-sm font-semibold tracking-[0.12em] uppercase">Live probe</span>
                    <span className="animate-recpulse inline-block size-1.5 rounded-full bg-ok" />
                    <button
                      onClick={() => {
                        const text = DIAG_ROWS.map((r) => `${r.label}: ${r.value}${r.hint ? ` (${r.hint})` : ""}`).join("\n");
                        navigator.clipboard?.writeText(text);
                        say("Diagnostics copied to clipboard");
                      }}
                      className="ml-auto flex items-center gap-1.5 rounded-lg border border-line px-3 py-1.5 font-mono text-[9px] tracking-[0.12em] text-mist uppercase transition hover:text-cy"
                    >
                      <Copy size={11} /> Copy report
                    </button>
                  </div>
                  <div className="max-h-[380px] space-y-1.5 overflow-y-auto pr-1">
                    {DIAG_ROWS.map((r) => (
                      <div key={r.label} className="flex items-start gap-3 rounded-xl border border-line/60 bg-panel px-3.5 py-2.5">
                        <span
                          className={`mt-1 size-2 shrink-0 rounded-full ${
                            r.ok === true ? "bg-ok" : r.ok === false ? "bg-bad" : "bg-am"
                          }`}
                        />
                        <div className="min-w-0 flex-1">
                          <div className="flex items-baseline justify-between gap-3">
                            <p className="font-mono text-[10px] text-frost">{r.label}</p>
                            <p className={`font-mono text-[10px] ${r.ok === false ? "text-bad" : r.ok === null ? "text-am" : "text-ok"}`}>
                              {r.value}
                            </p>
                          </div>
                          {r.hint && <p className="mt-0.5 font-mono text-[9px] text-mist/70">{r.hint}</p>}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* control column (camera tab) */}
            <div className={`hidden w-60 shrink-0 border-l border-line/60 p-5 lg:block ${tab !== "camera" ? "opacity-35 pointer-events-none" : ""}`}>
              <p className="font-mono text-[10px] tracking-[0.2em] uppercase text-cy">Controls</p>

              <div className="mt-5 space-y-5">
                <div>
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 font-mono text-[10px] text-mist">
                      <Lock size={9} /> Zoom
                    </span>
                    <span className="font-mono text-[10px] text-am">HAL-locked</span>
                  </div>
                  <input type="range" disabled value={1} min={1} max={3} step={0.1} className="cy-range mt-2 w-full" title="Fixed-focus optics — CONTROL_ZOOM_RATIO_RANGE absent" />
                </div>

                <div>
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 font-mono text-[10px] text-mist">
                      <Lock size={9} /> Exposure comp
                    </span>
                    <span className="font-mono text-[10px] text-am">no range</span>
                  </div>
                  <input type="range" disabled value={0} min={-4} max={4} step={1} className="cy-range mt-2 w-full" title="HAL reports zero compensation range" />
                </div>

                <div>
                  <p className="font-mono text-[10px] text-mist">Format</p>
                  <div className="mt-2 flex gap-1.5">
                    {(["JPEG", "PNG"] as const).map((f) => (
                      <button
                        key={f}
                        onClick={() => setFormat(f)}
                        className={`flex-1 rounded-lg px-2 py-1.5 font-mono text-[10px] transition ${
                          format === f ? "bg-cy font-semibold text-abyss" : "border border-line text-mist"
                        }`}
                      >
                        {f}
                      </button>
                    ))}
                  </div>
                </div>

                {format === "JPEG" && (
                  <div>
                    <div className="flex justify-between">
                      <span className="font-mono text-[10px] text-mist">JPEG quality</span>
                      <span className="font-mono text-[10px] text-cy">{quality}%</span>
                    </div>
                    <input type="range" min={60} max={100} value={quality} onChange={(e) => setQuality(+e.target.value)} className="cy-range mt-2 w-full" />
                  </div>
                )}

                <div>
                  <p className="font-mono text-[10px] text-mist">Video preset</p>
                  <div className="mt-2 grid grid-cols-2 gap-1.5">
                    {["MAXIMUM", "HIGH", "BALANCED", "SMOOTH"].map((p) => (
                      <button
                        key={p}
                        onClick={() => setPreset(p)}
                        className={`rounded-lg px-2 py-1.5 font-mono text-[9px] transition ${
                          preset === p ? "bg-vi/80 text-white" : "border border-line text-mist"
                        }`}
                      >
                        {p}
                      </button>
                    ))}
                  </div>
                </div>

                {[
                  { icon: Mic, label: "Microphone", on: audio, t: () => setAudio(!audio) },
                  { icon: Grid3x3, label: "Grid", on: grid, t: () => setGrid(!grid) },
                  { icon: Clock3, label: "Timestamp", on: timestamp, t: () => setTimestamp(!timestamp) },
                ].map((r) => (
                  <div key={r.label} className="flex items-center gap-2.5">
                    <r.icon size={13} className="text-mist" />
                    <span className="flex-1 text-xs text-frost">{r.label}</span>
                    <Switch on={r.on} onToggle={r.t} />
                  </div>
                ))}

                <div>
                  <p className="flex items-center gap-1.5 font-mono text-[10px] text-mist">
                    <Layers size={10} /> Watermark
                  </p>
                  <input
                    value={watermark}
                    onChange={(e) => setWatermark(e.target.value.toUpperCase().slice(0, 24))}
                    placeholder="QCS · QUEST 3"
                    className="mt-2 w-full rounded-lg border border-line bg-abyss px-3 py-1.5 font-mono text-[10px] text-cy outline-none placeholder:text-mist/40 focus:border-cy/60"
                    spellCheck={false}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* status strip */}
          <div className="flex items-center gap-4 border-t border-line/60 px-5 py-2.5 font-mono text-[9px] tracking-[0.12em] text-mist/70">
            <span className="flex items-center gap-1.5"><Aperture size={10} className="text-cy" /> Camera2 · YUV_420_888</span>
            <span className="hidden sm:inline">dst → {destLabel}</span>
            <span className="ml-auto hidden items-center gap-1.5 sm:flex">
              <ChevronDown size={10} /> {counter - 1} captures this session
            </span>
          </div>

          {/* toast */}
          {toast && (
            <div className="glass-strong absolute bottom-14 left-1/2 z-10 -translate-x-1/2 rounded-full px-5 py-2.5 font-mono text-[10px] tracking-[0.08em] text-cy">
              {toast}
            </div>
          )}

          {/* viewer */}
          {viewer && (
            <div className="absolute inset-0 z-20 grid place-items-center bg-abyss/85 p-6 backdrop-blur-md" onClick={() => setViewer(null)}>
              <div className="glass-strong w-full max-w-2xl overflow-hidden rounded-3xl" onClick={(e) => e.stopPropagation()}>
                <div className="relative">
                  <img src={feedImg} alt={viewer.name} style={{ filter: viewer.filter }} className="aspect-video w-full object-cover" />
                  {viewer.kind === "video" && (
                    <span className="absolute inset-0 grid place-items-center">
                      <span className="grid size-16 place-items-center rounded-full bg-abyss/60 ring-1 ring-cy/50 backdrop-blur">
                        <Play size={22} className="ml-1 text-cy" />
                      </span>
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-3 p-4">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-mono text-[11px] text-frost">{viewer.name}</p>
                    <p className="font-mono text-[9px] text-mist">
                      {viewer.size} · {viewer.kind === "video" ? "MP4 · H.264" : format === "PNG" ? "PNG" : "JPEG"} · 1280×960
                    </p>
                  </div>
                  <button
                    onClick={() => setShots((s) => s.map((x) => (x.id === viewer.id ? { ...x, fav: !x.fav } : x)))}
                    className="grid size-9 place-items-center rounded-lg border border-line text-mist hover:text-bad"
                  >
                    <Heart size={15} className={viewer.fav ? "fill-bad text-bad" : ""} />
                  </button>
                  <button
                    onClick={() => {
                      setShots((s) => s.filter((x) => x.id !== viewer.id));
                      setViewer(null);
                      say("Deleted from library");
                    }}
                    className="grid size-9 place-items-center rounded-lg border border-line text-mist hover:text-bad"
                  >
                    <Trash2 size={15} />
                  </button>
                  <button
                    onClick={() => setViewer(null)}
                    className="grid size-9 place-items-center rounded-lg border border-line text-mist hover:text-cy"
                  >
                    <X size={15} />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </Reveal>

      <Reveal delay={180}>
        <div className="mt-6 grid gap-3 sm:grid-cols-3">
          {[
            { icon: Camera, t: "Mirrors the real UI", d: "Compose screens, rail nav, HUD chips and gating logic map 1:1 to the Kotlin source in the ZIP." },
            { icon: Video, t: "Try every mode", d: "Burst fires 5 frames, Timer counts down, Interval loops until stopped, Video runs a live REC clock with pause." },
            { icon: Lock, t: "Honesty rendered", d: "Zoom/EV sliders and Slow-Mo are disabled here exactly as they are in the APK — with the HAL reason attached." },
          ].map((c) => (
            <div key={c.t} className="glass rounded-2xl p-5">
              <c.icon size={17} className="text-cy" />
              <p className="mt-3 font-display text-sm font-semibold tracking-wide">{c.t}</p>
              <p className="mt-1.5 text-xs leading-relaxed text-mist">{c.d}</p>
            </div>
          ))}
        </div>
      </Reveal>
    </section>
  );
}
