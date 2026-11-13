import {
  CheckCircle2, CircleDot, FileCode2, KeyRound, Loader2, PackageCheck, Radio, Tag,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Reveal } from "./Reveal";

const STEPS = [
  { name: "actions/checkout@v4", note: "repo contents" },
  { name: "setup-java · Temurin 17", note: "toolchain" },
  { name: "setup-android · SDK 34 + build-tools", note: "platforms;android-34" },
  { name: "setup-gradle · 8.9 + wrapper regen", note: "build system" },
  { name: "testDebugUnitTest", note: "JVM unit tests gate the build" },
  { name: "lintDebug", note: "report-only, never silently broken" },
  { name: "assembleDebug", note: "installable debug-signed APK" },
  { name: "assembleRelease", note: "unsigned unless secrets exist" },
  { name: "sign via secrets", note: "QUEST_KEYSTORE_* — only when configured" },
  { name: "verify: aapt + apksigner", note: "package, permissions, signature" },
  { name: "upload-artifact@v4", note: "MetaQuestCamera-apks" },
  { name: "tag v* → GitHub Release", note: "APKs attached + notes" },
];

const YAML = `name: Android CI & APK Builder
on:
  push: { branches: [main], tags: ["v*"] }
  pull_request:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "17" }
      - uses: android-actions/setup-android@v3
      - run: sdkmanager "platforms;android-34" "build-tools;34.0.0"
      - uses: gradle/actions/setup-gradle@v4
        with: { gradle-version: "8.9" }
      - run: gradle testDebugUnitTest assembleDebug assembleRelease
      - run: aapt dump badging app/build/outputs/apk/debug/app-debug.apk
      - uses: actions/upload-artifact@v4
        with: { name: MetaQuestCamera-apks, path: dist/*.apk }

  release:                       # tags only
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - uses: softprops/action-gh-release@v2
        with: { files: dist/*.apk }`;

const SECRETS = ["QUEST_KEYSTORE_BASE64", "QUEST_KEYSTORE_PASSWORD", "QUEST_KEY_ALIAS", "QUEST_KEY_PASSWORD"];

export function Pipeline() {
  const [active, setActive] = useState(0);
  useEffect(() => {
    const id = window.setInterval(() => setActive((a) => (a + 1) % (STEPS.length + 2)), 1100);
    return () => window.clearInterval(id);
  }, []);

  return (
    <section id="pipeline" className="relative mx-auto max-w-7xl px-5 py-28">
      <Reveal>
        <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
          04 — Ship pipeline
        </p>
        <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
          Push to main. <span className="clip-gradient">Download the APK.</span>
        </h2>
        <p className="mt-4 max-w-2xl leading-relaxed text-mist">
          The workflow in <span className="font-mono text-cy">.github/workflows/android-build.yml</span> runs
          on pushes, PRs, manual dispatches and version tags — compile, test, sign, verify,
          publish. No local Android Studio required to get a build.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-6 lg:grid-cols-[1fr_1.1fr]">
        {/* animated job */}
        <Reveal>
          <div className="glass-strong h-full rounded-3xl p-6">
            <div className="flex items-center justify-between">
              <p className="font-mono text-[10px] tracking-[0.22em] uppercase text-mist">
                workflow run · simulated
              </p>
              <span className="flex items-center gap-1.5 rounded-full bg-ok/10 px-3 py-1 font-mono text-[9px] tracking-[0.14em] text-ok uppercase">
                <Radio size={10} className="animate-recpulse" /> live queue
              </span>
            </div>

            <div className="mt-6 space-y-1.5">
              {STEPS.map((s, i) => {
                const done = active > i;
                const current = active === i;
                return (
                  <div
                    key={s.name}
                    className={`flex items-center gap-3 rounded-xl border px-4 py-2.5 transition-all duration-500 ${
                      current
                        ? "border-cy/50 bg-cy/10"
                        : done
                          ? "border-line/40 bg-panel/50"
                          : "border-transparent opacity-45"
                    }`}
                  >
                    {done ? (
                      <CheckCircle2 size={15} className="shrink-0 text-ok" />
                    ) : current ? (
                      <Loader2 size={15} className="shrink-0 animate-spin text-cy" />
                    ) : (
                      <CircleDot size={15} className="shrink-0 text-mist/50" />
                    )}
                    <span className={`font-mono text-[11px] ${current ? "text-cy" : "text-frost/80"}`}>
                      {s.name}
                    </span>
                    <span className="ml-auto hidden font-mono text-[9px] text-mist/70 sm:block">
                      {s.note}
                    </span>
                  </div>
                );
              })}
            </div>

            <div className={`mt-5 flex items-center gap-3 rounded-xl border px-4 py-3 transition-all duration-500 ${
              active >= STEPS.length ? "border-ok/50 bg-ok/10" : "border-line/40"
            }`}>
              <PackageCheck size={16} className={active >= STEPS.length ? "text-ok" : "text-mist/50"} />
              <span className="font-mono text-[11px]">
                {active >= STEPS.length ? "artifact ready · MetaQuestCamera.apk" : "awaiting artifact…"}
              </span>
            </div>
          </div>
        </Reveal>

        {/* workflow source */}
        <Reveal delay={120}>
          <div className="glass-strong overflow-hidden rounded-3xl">
            <div className="flex items-center gap-2.5 border-b border-line/60 px-5 py-3">
              <FileCode2 size={14} className="text-cy" />
              <span className="font-mono text-[10px] tracking-[0.12em] text-mist">
                .github/workflows/android-build.yml
              </span>
              <span className="ml-auto rounded-full bg-vi/15 px-2.5 py-1 font-mono text-[8px] tracking-[0.14em] text-vi uppercase">
                excerpt — full file in ZIP
              </span>
            </div>
            <pre className="max-h-[430px] overflow-auto p-5 font-mono text-[11px] leading-relaxed text-mist">
              {YAML}
            </pre>
          </div>
        </Reveal>
      </div>

      {/* secrets + honesty */}
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Reveal delay={100}>
          <div className="glass h-full rounded-3xl p-6">
            <div className="flex items-center gap-3">
              <span className="grid size-10 place-items-center rounded-xl bg-am/10 text-am">
                <KeyRound size={17} />
              </span>
              <p className="font-display text-sm font-semibold tracking-[0.12em] uppercase">
                Signing via secrets, never commits
              </p>
            </div>
            <p className="mt-4 text-sm leading-relaxed text-mist">
              Release signing activates only when the keystore arrives through GitHub
              Actions secrets and repo variable <span className="font-mono text-cy">QUEST_SIGNING_ENABLED=true</span>.
              Without it, CI ships a debug-signed APK that installs via ADB immediately.
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              {SECRETS.map((s) => (
                <span key={s} className="rounded-lg border border-line bg-panel px-3 py-1.5 font-mono text-[9px] tracking-[0.08em] text-mist">
                  {s}
                </span>
              ))}
            </div>
          </div>
        </Reveal>

        <Reveal delay={180}>
          <div className="glass h-full rounded-3xl p-6">
            <div className="flex items-center gap-3">
              <span className="grid size-10 place-items-center rounded-xl bg-ok/10 text-ok">
                <Tag size={17} />
              </span>
              <p className="font-display text-sm font-semibold tracking-[0.12em] uppercase">
                What green CI actually proves
              </p>
            </div>
            <div className="mt-4 space-y-3 text-sm leading-relaxed text-mist">
              <p>
                <span className="font-semibold text-ok">Proven by CI:</span> Kotlin compiles,
                unit tests pass, APK exists, manifest + permissions parse via aapt,
                signature verifies via apksigner.
              </p>
              <p>
                <span className="font-semibold text-am">Needs your headset:</span> Camera2 session
                behavior against the real passthrough HAL, SAF provider quirks, thermal behavior.
                The in-app Diagnostics tab exists precisely to close that gap.
              </p>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
