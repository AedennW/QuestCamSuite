import { Aperture, Download, GitBranch, Radar } from "lucide-react";
import { useEffect, useState } from "react";
import { PROJECT_ZIP_B64, PROJECT_ZIP_NAME } from "../assets/projectZip";

const LINKS: Array<[string, string]> = [
  ["Truth table", "#truth"],
  ["Interface", "#preview"],
  ["Suite", "#features"],
  ["Pipeline", "#pipeline"],
  ["AWS path", "#aws"],
  ["Start here", "#start"],
  ["Install", "#install"],
];

export function Nav() {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 24);
    window.addEventListener("scroll", fn, { passive: true });
    return () => window.removeEventListener("scroll", fn);
  }, []);

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 transition-all duration-500 ${
        scrolled ? "py-2" : "py-4"
      }`}
    >
      <div className="mx-auto flex max-w-7xl items-center gap-6 px-5">
        <div
          className={`glass-strong flex w-full items-center gap-5 rounded-2xl px-5 transition-all duration-500 ${
            scrolled ? "py-2" : "py-3"
          }`}
        >
          <a href="#top" className="group flex items-center gap-2.5">
            <span className="grid size-9 place-items-center rounded-xl bg-cy/10 text-cy ring-1 ring-cy/30 transition group-hover:rotate-90 duration-500">
              <Aperture size={18} />
            </span>
            <span className="font-display text-sm font-semibold tracking-[0.18em] uppercase">
              Quest<span className="text-cy">Cam</span>Suite
            </span>
          </a>

          <nav className="ml-auto hidden items-center gap-6 lg:flex">
            {LINKS.map(([label, href]) => (
              <a
                key={href}
                href={href}
                className="font-mono text-[11px] tracking-[0.18em] uppercase text-mist transition hover:text-cy"
              >
                {label}
              </a>
            ))}
          </nav>

          <a
            href={PROJECT_ZIP_B64}
            download={PROJECT_ZIP_NAME}
            className="ml-auto flex items-center gap-2 rounded-xl bg-cy px-4 py-2.5 font-display text-xs font-semibold tracking-[0.14em] uppercase text-abyss transition hover:brightness-110 lg:ml-4"
          >
            <Download size={14} />
            Get the project
          </a>
        </div>
      </div>
    </header>
  );
}

export function Footer() {
  return (
    <footer className="relative border-t border-line/60">
      <div className="mx-auto grid max-w-7xl gap-10 px-5 py-14 md:grid-cols-[1.4fr_1fr_1fr]">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="grid size-9 place-items-center rounded-xl bg-cy/10 text-cy ring-1 ring-cy/30">
              <Aperture size={18} />
            </span>
            <span className="font-display text-sm font-semibold tracking-[0.18em] uppercase">
              Quest<span className="text-cy">Cam</span>Suite
            </span>
          </div>
          <p className="mt-4 max-w-md text-sm leading-relaxed text-mist">
            An unofficial, open-source camera suite for Meta Quest 3 built on
            documented APIs only. Not affiliated with Meta Platforms, Inc. No
            fake repository URLs, no staged screenshots of features that don't
            exist — clone it, push it, let Actions build the APK.
          </p>
        </div>

        <div>
          <p className="font-mono text-[11px] tracking-[0.2em] uppercase text-mist">
            Build truth
          </p>
          <ul className="mt-4 space-y-2.5 text-sm text-mist">
            <li className="flex items-center gap-2">
              <Radar size={13} className="text-cy" /> Compile ≠ headset-verified — test on hardware
            </li>
            <li className="flex items-center gap-2">
              <Radar size={13} className="text-cy" /> Debug APK installs via ADB with zero setup
            </li>
            <li className="flex items-center gap-2">
              <Radar size={13} className="text-cy" /> Release signing only via repo secrets
            </li>
          </ul>
        </div>

        <div>
          <p className="font-mono text-[11px] tracking-[0.2em] uppercase text-mist">
            Ship it
          </p>
          <div className="mt-4 flex flex-col items-start gap-3">
            <a
              href={PROJECT_ZIP_B64}
              download={PROJECT_ZIP_NAME}
              className="flex items-center gap-2 rounded-xl border border-cy/30 bg-cy/5 px-4 py-2.5 text-sm text-cy transition hover:bg-cy/15"
            >
              <Download size={14} /> MetaQuestCameraSuite.zip
            </a>
            <a
              href="#install"
              className="flex items-center gap-2 rounded-xl border border-line px-4 py-2.5 text-sm text-mist transition hover:border-cy/40 hover:text-cy"
            >
              <GitBranch size={14} /> Push → Actions → APK
            </a>
          </div>
        </div>
      </div>
      <div className="border-t border-line/60 py-5 text-center font-mono text-[10px] tracking-[0.24em] uppercase text-mist/60">
        v1.0.0 · Camera2 + Passthrough Camera API · Horizon OS v76+ · MIT
      </div>
    </footer>
  );
}
