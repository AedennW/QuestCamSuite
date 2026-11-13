import {
  AlertTriangle, Check, CheckCircle2, Copy, FileCode2, FileText, FolderGit2,
  FolderOpen, MousePointerClick, Package, ShieldAlert, Terminal, Upload,
} from "lucide-react";
import { useState } from "react";
import { PROJECT_ZIP_B64, PROJECT_ZIP_NAME } from "../assets/projectZip";
import { Reveal } from "./Reveal";

// ── File tree data ────────────────────────────────────────────────────────

type TreeFile = {
  path: string;
  depth: number;
  kind: "folder" | "file" | "workflow";
  status: "upload" | "ci" | "never";
  note: string;
};

const TREE: TreeFile[] = [
  { path: ".github/workflows/android-build.yml", depth: 2, kind: "workflow", status: "upload", note: "the CI pipeline — Actions finds it automatically" },
  { path: "app/build.gradle.kts", depth: 1, kind: "file", status: "upload", note: "module config, deps, optional env signing" },
  { path: "app/src/main/AndroidManifest.xml", depth: 3, kind: "file", status: "upload", note: "both camera permissions live here" },
  { path: "app/src/main/java/com/camerasuite/quest/", depth: 4, kind: "folder", status: "upload", note: "13 Kotlin sources — engine, capture, storage, UI" },
  { path: "app/src/main/res/", depth: 3, kind: "folder", status: "upload", note: "theme, strings, adaptive icon" },
  { path: "app/src/test/java/", depth: 4, kind: "folder", status: "upload", note: "JVM unit tests — gate every CI build" },
  { path: "aws/buildspec.yml + Dockerfile", depth: 1, kind: "folder", status: "upload", note: "CodeBuild route (keep even if unused)" },
  { path: "build.gradle.kts · settings.gradle.kts", depth: 0, kind: "file", status: "upload", note: "Gradle root config" },
  { path: "gradle.properties · gradle/wrapper/", depth: 0, kind: "file", status: "upload", note: "JVM settings, wrapper properties" },
  { path: "README.md · FEATURES.md · TROUBLESHOOTING.md", depth: 0, kind: "file", status: "upload", note: "repo landing page + docs" },
  { path: ".gitignore", depth: 0, kind: "file", status: "upload", note: "blocks keystores, local.properties, build output" },
  { path: "*.jks / *.keystore", depth: 0, kind: "file", status: "never", note: "signing keys → GitHub Secrets only, never git" },
  { path: "local.properties", depth: 0, kind: "file", status: "never", note: "your local SDK path — machine-specific" },
  { path: "app/build/ · .gradle/", depth: 0, kind: "folder", status: "never", note: "build output — CI regenerates everything" },
];

const STATUS_STYLES = {
  upload: "bg-ok/10 text-ok",
  ci: "bg-cy/10 text-cy",
  never: "bg-bad/10 text-bad",
} as const;

// ── Copy + code blocks ────────────────────────────────────────────────────

function CopyBtn({ text }: { text: string }) {
  const [ok, setOk] = useState(false);
  return (
    <button
      onClick={() => {
        navigator.clipboard?.writeText(text);
        setOk(true);
        window.setTimeout(() => setOk(false), 1600);
      }}
      className="absolute right-3 top-2.5 grid size-8 place-items-center rounded-lg border border-line bg-panel/80 text-mist transition hover:text-cy"
      aria-label="copy"
    >
      {ok ? <Check size={13} className="text-ok" /> : <Copy size={13} />}
    </button>
  );
}

function Code({ label, body }: { label: string; body: string }) {
  return (
    <div className="relative overflow-hidden rounded-xl border border-line bg-abyss">
      <div className="border-b border-line/60 px-4 py-2 font-mono text-[9px] tracking-[0.18em] uppercase text-mist">
        {label}
      </div>
      <pre className="overflow-x-auto p-4 font-mono text-[11px] leading-relaxed text-cy">{body}</pre>
      <CopyBtn text={body} />
    </div>
  );
}

// ── Step cards per method ────────────────────────────────────────────────

const WEB_STEPS = [
  {
    title: "Download & unzip",
    body: "Grab MetaQuestCameraSuite.zip below and extract it anywhere.",
  },
  {
    title: "Create an empty repo",
    body: `GitHub → "New repository" → name it (e.g. quest-camera-suite) → Create. Leave every starter checkbox (README/license) UNCHECKED so it opens on the empty-repo page.`,
  },
  {
    title: "Upload the whole folder",
    body: `On that page click "uploading an existing file" (or later: Add file → Upload files). Drag the entire unzipped folder onto the page — GitHub keeps every subfolder — then "Commit changes".`,
  },
  {
    title: "Enable Actions",
    body: `Actions tab → "I understand my workflows, go ahead and enable them". The first build starts by itself.`,
  },
  {
    title: "Grab the APK",
    body: `Wait ~5–10 min → open the green run → Artifacts → MetaQuestCamera-apks → unzip → MetaQuestCamera-debug.apk.`,
  },
  {
    title: "Tag a release (optional)",
    body: `Releases → Draft a new release → tag v1.0.0 → Publish. CI rebuilds and attaches the APK right on the release page.`,
  },
];

const CLI_BODY = `# 1 — unzip and enter the project
unzip MetaQuestCameraSuite.zip -d quest-camera-suite
cd quest-camera-suite

# 2 — make it a repo and commit everything
git init -b main
git add -A
git commit -m "Quest Camera Suite v1.0.0"

# 3 — create the GitHub repo and push
gh repo create quest-camera-suite --public --source=. --push
# (no gh CLI? create it on github.com, then:
#  git remote add origin https://github.com/YOU/quest-camera-suite.git
#  git push -u origin main)

# 4 — optional: cut a release build
git tag v1.0.0 && git push origin v1.0.0`;

export function RepoChecklist() {
  const [method, setMethod] = useState<"web" | "cli">("web");

  return (
    <section id="start" className="relative mx-auto max-w-7xl px-5 py-28">
      <Reveal>
        <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
          07 — Zero to repo
        </p>
        <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
          Which files, <span className="clip-gradient">where, in what order.</span>
        </h2>
        <p className="mt-4 max-w-2xl leading-relaxed text-mist">
          The rule is simple:{" "}
          <span className="font-semibold text-frost">
            everything in the ZIP becomes your repository root.
          </span>{" "}
          Folder structure must survive the trip — Actions finds the workflow file by its
          path. Below is the exact map.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
        {/* file tree */}
        <Reveal>
          <div className="glass-strong overflow-hidden rounded-3xl">
            <div className="flex items-center gap-2.5 border-b border-line/60 px-5 py-3">
              <FolderGit2 size={14} className="text-cy" />
              <span className="font-mono text-[10px] tracking-[0.14em] text-mist">
                quest-camera-suite/ — repo root
              </span>
              <span className="ml-auto rounded-full bg-ok/10 px-2.5 py-1 font-mono text-[8px] tracking-[0.14em] text-ok uppercase">
                33 files
              </span>
            </div>
            <ul className="divide-y divide-line/30">
              {TREE.map((f) => (
                <li key={f.path} className="group flex items-center gap-3 px-5 py-2.5 transition hover:bg-panelhi/40">
                  <span className="shrink-0 text-mist/70">
                    {f.kind === "workflow" ? (
                      <FileCode2 size={14} className="text-cy" />
                    ) : f.kind === "folder" ? (
                      <FolderOpen size={14} className="text-vi" />
                    ) : (
                      <FileText size={14} />
                    )}
                  </span>
                  <span
                    className="min-w-0 flex-1 font-mono text-[11px] text-frost/90"
                    style={{ paddingLeft: f.depth * 12 }}
                  >
                    {f.path}
                  </span>
                  <span className="hidden max-w-[180px] text-right font-mono text-[9px] leading-tight text-mist/60 md:block">
                    {f.note}
                  </span>
                  <span
                    className={`shrink-0 rounded-full px-2.5 py-0.5 font-mono text-[8px] tracking-[0.14em] uppercase ${STATUS_STYLES[f.status]}`}
                  >
                    {f.status === "upload" ? "upload" : f.status === "ci" ? "CI only" : "never commit"}
                  </span>
                </li>
              ))}
            </ul>
            <div className="flex items-start gap-2.5 border-t border-line/60 bg-am/5 px-5 py-4">
              <ShieldAlert size={15} className="mt-0.5 shrink-0 text-am" />
              <p className="text-xs leading-relaxed text-mist">
                <span className="font-semibold text-am">The included .gitignore already blocks the dangerous rows.</span>{" "}
                If you ever add a release-keystore file, it must live only in GitHub → Settings →
                Secrets and variables → Actions. Never in the commit history — not even once.
              </p>
            </div>
          </div>
        </Reveal>

        {/* methods */}
        <Reveal delay={120}>
          <div className="glass-strong flex h-full flex-col rounded-3xl p-6">
            <div className="flex gap-2">
              {(
                [
                  { id: "web" as const, icon: MousePointerClick, label: "Web upload — no git" },
                  { id: "cli" as const, icon: Terminal, label: "Terminal / gh CLI" },
                ]
              ).map((m) => (
                <button
                  key={m.id}
                  onClick={() => setMethod(m.id)}
                  className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 font-mono text-[10px] tracking-[0.12em] uppercase transition ${
                    method === m.id
                      ? "bg-cy font-semibold text-abyss"
                      : "border border-line text-mist hover:border-cy/40 hover:text-cy"
                  }`}
                >
                  <m.icon size={13} />
                  {m.label}
                </button>
              ))}
            </div>

            {method === "web" ? (
              <ol className="mt-6 space-y-0">
                {WEB_STEPS.map((s, i) => (
                  <li key={s.title} className="relative flex gap-4 pb-6 last:pb-0">
                    {i < WEB_STEPS.length - 1 && (
                      <span className="absolute left-[15px] top-9 h-[calc(100%-36px)] w-px bg-line" />
                    )}
                    <span className="grid size-8 shrink-0 place-items-center rounded-full border border-cy/40 bg-cy/10 font-display text-xs font-bold text-cy">
                      {i + 1}
                    </span>
                    <div className="pt-1">
                      <p className="font-display text-sm font-semibold">{s.title}</p>
                      <p className="mt-1 text-xs leading-relaxed text-mist">{s.body}</p>
                    </div>
                  </li>
                ))}
              </ol>
            ) : (
              <div className="mt-6">
                <Code label="terminal" body={CLI_BODY} />
                <p className="mt-3 font-mono text-[10px] leading-relaxed text-mist/70">
                  Push lands → Actions tab shows the run automatically → Artifacts →{" "}
                  MetaQuestCamera-apks. The tag command also creates a GitHub Release with
                  both APKs attached.
                </p>
              </div>
            )}

            <div className="mt-auto pt-6">
              <a
                href={PROJECT_ZIP_B64}
                download={PROJECT_ZIP_NAME}
                className="flex items-center justify-center gap-3 rounded-2xl bg-cy px-6 py-4 font-display text-sm font-semibold tracking-[0.12em] uppercase text-abyss transition hover:brightness-110"
              >
                <Package size={16} />
                Download the ZIP
              </a>
            </div>
          </div>
        </Reveal>
      </div>

      {/* verification strip */}
      <Reveal delay={160}>
        <div className="glass mt-8 rounded-3xl p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-ok/10 text-ok">
              <Upload size={17} />
            </span>
            <p className="font-display text-sm font-semibold tracking-[0.12em] uppercase">
              How you know it worked
            </p>
          </div>
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            {[
              { icon: CheckCircle2, t: "Repo page", d: "github.com/YOU/repo renders the README with badges and the .github folder visible." },
              { icon: FileCode2, t: "Actions tab", d: "A run named after your commit appears within seconds of pushing; green check ≈ 5–10 min." },
              { icon: AlertTriangle, t: "If it fails", d: "Click the failed step — the log names the file and line. 95% of first-run failures are a missing folder from a partial upload." },
            ].map((c) => (
              <div key={c.t} className="rounded-2xl border border-line bg-panel p-4">
                <c.icon size={16} className="text-cy" />
                <p className="mt-2.5 font-display text-sm font-semibold">{c.t}</p>
                <p className="mt-1 text-xs leading-relaxed text-mist">{c.d}</p>
              </div>
            ))}
          </div>
        </div>
      </Reveal>
    </section>
  );
}
