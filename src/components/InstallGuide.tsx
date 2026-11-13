import { Check, Copy, Download, GitFork, Glasses, KeyRound, Terminal } from "lucide-react";
import { useState, type ReactNode } from "react";
import { PROJECT_ZIP_B64, PROJECT_ZIP_NAME } from "../assets/projectZip";
import { Reveal } from "./Reveal";

function CopyBtn({ text }: { text: string }) {
  const [ok, setOk] = useState(false);
  return (
    <button
      onClick={() => {
        navigator.clipboard?.writeText(text);
        setOk(true);
        window.setTimeout(() => setOk(false), 1600);
      }}
      className="absolute right-3 top-3 grid size-8 place-items-center rounded-lg border border-line bg-panel/80 text-mist transition hover:text-cy"
      aria-label="copy"
    >
      {ok ? <Check size={13} className="text-ok" /> : <Copy size={13} />}
    </button>
  );
}

function Code({ label, children, copy }: { label: string; children: string; copy?: string }) {
  return (
    <div className="relative mt-4 overflow-hidden rounded-xl border border-line bg-abyss">
      <div className="border-b border-line/60 px-4 py-2 font-mono text-[9px] tracking-[0.18em] uppercase text-mist">
        {label}
      </div>
      <pre className="overflow-x-auto p-4 font-mono text-[11px] leading-relaxed text-cy">{children}</pre>
      <CopyBtn text={copy ?? children} />
    </div>
  );
}

function StepCard({
  index, icon: Icon, title, desc, children,
}: {
  index: string; icon: typeof Terminal; title: string; desc: string; children?: ReactNode;
}) {
  return (
    <div className="glass h-full rounded-3xl p-6">
      <div className="flex items-center gap-3">
        <span className="grid size-10 place-items-center rounded-xl bg-cy/10 font-display text-sm font-bold text-cy">
          {index}
        </span>
        <Icon size={17} className="text-mist" />
      </div>
      <p className="mt-4 font-display text-base font-semibold">{title}</p>
      <p className="mt-2 text-sm leading-relaxed text-mist">{desc}</p>
      {children}
    </div>
  );
}

export function InstallGuide() {
  return (
    <section id="install" className="relative mx-auto max-w-7xl px-5 py-28">
      <Reveal>
        <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
          08 — Zero to headset
        </p>
        <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
          Four moves. <span className="clip-gradient">One APK.</span>
        </h2>
      </Reveal>

      <div className="mt-14 grid gap-5 lg:grid-cols-2">
        <Reveal>
          <StepCard
            index="01" icon={GitFork}
            title="Put the project on your GitHub"
            desc="Unzip the download into a fresh folder, init git, and push. The workflow file rides along — no edits needed."
          >
            <Code label="terminal" copy={`mkdir quest-cam && cd quest-cam
unzip ../MetaQuestCameraSuite.zip
git init -b main && git add -A
git commit -m "Quest Camera Suite v1.0.0"
gh repo create quest-camera-suite --public --source=. --push`}>
{`mkdir quest-cam && cd quest-cam
unzip ../MetaQuestCameraSuite.zip
git init -b main && git add -A
git commit -m "Quest Camera Suite v1.0.0"
gh repo create quest-camera-suite --public --source=. --push`}
            </Code>
          </StepCard>
        </Reveal>

        <Reveal delay={90}>
          <StepCard
            index="02" icon={Download}
            title="Let Actions build the APK"
            desc="Every push compiles, unit-tests and verifies, then uploads the MetaQuestCamera-apks artifact. Tag v1.0.0 and it lands on a GitHub Release too."
          >
            <Code label="optional — cut a release">{"git tag v1.0.0 && git push origin v1.0.0"}</Code>
            <Code label="or build locally (JDK 17 + Android SDK 34)">{"gradle wrapper --gradle-version 8.9\ngradle assembleDebug"}</Code>
          </StepCard>
        </Reveal>

        <Reveal delay={140}>
          <StepCard
            index="03" icon={Terminal}
            title="Sideload onto the Quest 3"
            desc="Enable Developer Mode in the Meta Horizon phone app, connect over USB-C (or wireless ADB), accept the in-headset prompt, install."
          >
            <Code label="adb" copy={`adb devices
adb install -r MetaQuestCamera-debug.apk`}>
{`adb devices
adb install -r MetaQuestCamera-debug.apk`}
            </Code>
            <p className="mt-3 font-mono text-[10px] tracking-[0.06em] text-mist/70">
              Headset: Horizon OS v76+ required for Camera2 passthrough.
            </p>
          </StepCard>
        </Reveal>

        <Reveal delay={190}>
          <StepCard
            index="04" icon={Glasses}
            title="Grant both cameras, then fly"
            desc="The app asks for android.permission.CAMERA and horizonos.permission.HEADSET_CAMERA — both required. Enable system passthrough, open the Diagnostics tab to verify every probe reads green."
          >
            <Code label="headset">{"Settings → Developer → allow USB debugging\nApp prompt → allow Camera + Headset Camera\nQCS → Diagnostics → Re-run"}</Code>
          </StepCard>
        </Reveal>
      </div>

      {/* CTA band */}
      <Reveal delay={120}>
        <div className="glass-strong relative mt-12 overflow-hidden rounded-[2rem] p-10 text-center sm:p-14">
          <div className="pointer-events-none absolute -left-24 -top-24 size-64 rounded-full bg-cy/15 blur-[90px]" />
          <div className="pointer-events-none absolute -bottom-24 -right-24 size-64 rounded-full bg-vi/15 blur-[90px]" />
          <p className="font-mono text-[10px] tracking-[0.3em] uppercase text-cy">
            The ZIP below is the entire deliverable
          </p>
          <h3 className="mx-auto mt-4 max-w-2xl font-display text-3xl font-bold leading-tight sm:text-4xl">
            Kotlin source, manifest, Compose UI, CI workflow, docs. Ready to push.
          </h3>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
            <a
              href={PROJECT_ZIP_B64}
              download={PROJECT_ZIP_NAME}
              className="flex items-center gap-3 rounded-2xl bg-cy px-8 py-4 font-display text-sm font-semibold tracking-[0.12em] uppercase text-abyss transition hover:brightness-110"
            >
              <Download size={16} />
              MetaQuestCameraSuite.zip
            </a>
            <span className="flex items-center gap-2 rounded-2xl border border-line px-5 py-4 font-mono text-[11px] text-mist">
              <KeyRound size={13} className="text-am" />
              27 files · zero secrets · MIT
            </span>
          </div>
        </div>
      </Reveal>
    </section>
  );
}
