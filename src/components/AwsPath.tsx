import {
  AlertTriangle, Check, Cloud, Cpu, Database, FileCode2, MonitorSmartphone,
  Server, Smartphone, X,
} from "lucide-react";
import { Reveal } from "./Reveal";

type Route = {
  rank: string;
  icon: typeof Server;
  title: string;
  badge: string;
  badgeTone: string;
  desc: string;
  points: string[];
  code?: { label: string; body: string };
};

const ROUTES: Route[] = [
  {
    rank: "01",
    icon: Server,
    title: "EC2 self-hosted runner",
    badge: "Recommended",
    badgeTone: "bg-ok/10 text-ok",
    desc: "Keep the exact same workflow file — only the runner changes. The Android SDK stays installed on the instance, so builds take minutes instead of re-downloading ~2 GB every run.",
    points: [
      "Launch EC2 (Amazon Linux 2023, t3.large spot ≈ $0.01–0.02/hr)",
      "Install JDK 17 + Android SDK 34 + Gradle (commands in aws/Dockerfile)",
      "GitHub → Settings → Actions → Runners → register with label `android`",
      "Stop the instance when idle — pay only while building",
    ],
    code: {
      label: "the only workflow change",
      body: `jobs:
  build:
    runs-on: [self-hosted, linux, x64, android]`,
    },
  },
  {
    rank: "02",
    icon: Cloud,
    title: "CodeBuild + CodePipeline",
    badge: "Pure AWS",
    badgeTone: "bg-cy/10 text-cy",
    desc: "Fully managed AWS-native CI. A custom ECR image with the SDK baked in (aws/Dockerfile) plus the buildspec in the ZIP (aws/buildspec.yml) drive test → assemble → S3 artifacts.",
    points: [
      "Build image once, push to ECR — never re-download the SDK",
      "Source via CodePipeline from GitHub (CodeCommit is closed to new customers)",
      "Signing secrets from AWS Secrets Manager, not git",
      "Artifacts → S3, serve via CloudFront for easy APK download",
    ],
    code: {
      label: "aws/buildspec.yml — core phases",
      body: `build:
  commands:
    - gradle --no-daemon testDebugUnitTest
    - gradle --no-daemon assembleDebug assembleRelease
artifacts:
  files: [dist/*.apk]`,
    },
  },
  {
    rank: "03",
    icon: Smartphone,
    title: "Device Farm",
    badge: "Real hardware",
    badgeTone: "bg-vi/15 text-vi",
    desc: "Closes the one gap CI can't: actual headset behavior. Device Farm runs your app on real physical devices with remote access and automated runs — Meta itself uses it for Quest testing (re:Invent 2024). Check the catalog for Quest devices in your region.",
    points: [
      "Remote access: interact with a real device from your browser",
      "Automated runs: install, launch, screenshot, logcat capture",
      "Pay-per-minute, no device provisioning or management",
      "Use it to verify Camera2 sessions on physical hardware",
    ],
  },
];

const COMPARE: Array<{ row: string; gh: string; ec2: string; cb: string }> = [
  { row: "Cost", gh: "Free-tier minutes", ec2: "~$0.01–0.02/hr (spot)", cb: "Pay-per-minute" },
  { row: "Setup effort", gh: "Zero", ec2: "~30 min", cb: "~1 hr (ECR image)" },
  { row: "Build speed", gh: "SDK download each run", ec2: "Fastest — warm SDK", cb: "Fast — baked image" },
  { row: "Android emulator", gh: "Yes (KVM)", ec2: "Yes (metal instances)", cb: "No — no KVM" },
  { row: "Signing secrets", gh: "Repo secrets", ec2: "Repo secrets", cb: "Secrets Manager" },
  { row: "Best for", gh: "Most people", ec2: "Frequent builds", cb: "Fully-AWS shops" },
];

const CAVEATS = [
  "CodeBuild has no nested virtualization — no emulator, unit tests + assemble only",
  "Never re-download the SDK per build — bake it into the ECR image",
  "CodeCommit no longer accepts new customers — keep the repo on GitHub",
  "Keystore lives in Secrets Manager / SSM — never in git",
  "Distribute via S3 + CloudFront; QR code → phone → ADB / SideQuest sideload",
];

export function AwsPath() {
  return (
    <section id="aws" className="relative mx-auto max-w-7xl px-5 py-28">
      <Reveal>
        <p className="font-mono text-[11px] tracking-[0.3em] uppercase text-cy">
          06 — The AWS path
        </p>
        <h2 className="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">
          Yes — on AWS. <span className="clip-gradient">Three routes, ranked.</span>
        </h2>
        <p className="mt-4 max-w-2xl leading-relaxed text-mist">
          The GitHub Actions pipeline is the default, but nothing in this project is
          GitHub-locked. If your world is AWS, the same source builds the same APK three
          different ways — and the AWS files are already in the ZIP under{" "}
          <span className="font-mono text-cy">aws/</span>.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-5 lg:grid-cols-3">
        {ROUTES.map((r, i) => (
          <Reveal key={r.title} delay={i * 110}>
            <div className="glass group flex h-full flex-col rounded-3xl p-6 transition duration-300 hover:-translate-y-1.5 hover:border-cy/30">
              <div className="flex items-center justify-between">
                <span className="grid size-11 place-items-center rounded-xl bg-cy/10 text-cy transition group-hover:bg-cy/20">
                  <r.icon size={19} />
                </span>
                <span className={`rounded-full px-3 py-1 font-mono text-[9px] tracking-[0.16em] uppercase ${r.badgeTone}`}>
                  {r.badge}
                </span>
              </div>
              <p className="mt-5 font-mono text-[10px] tracking-[0.2em] text-mist">
                ROUTE {r.rank}
              </p>
              <h3 className="mt-1.5 font-display text-lg font-semibold">{r.title}</h3>
              <p className="mt-2.5 text-sm leading-relaxed text-mist">{r.desc}</p>
              <ul className="mt-4 space-y-2.5">
                {r.points.map((p) => (
                  <li key={p} className="flex items-start gap-2.5 text-xs leading-relaxed text-mist">
                    <Check size={13} className="mt-0.5 shrink-0 text-ok" />
                    <span>{p}</span>
                  </li>
                ))}
              </ul>
              {r.code && (
                <div className="mt-5 overflow-hidden rounded-xl border border-line bg-abyss">
                  <div className="flex items-center gap-2 border-b border-line/60 px-3.5 py-2 font-mono text-[9px] tracking-[0.16em] uppercase text-mist">
                    <FileCode2 size={11} className="text-cy" />
                    {r.code.label}
                  </div>
                  <pre className="overflow-x-auto p-3.5 font-mono text-[10.5px] leading-relaxed text-cy">
                    {r.code.body}
                  </pre>
                </div>
              )}
            </div>
          </Reveal>
        ))}
      </div>

      {/* comparison */}
      <Reveal delay={120}>
        <div className="glass-strong mt-10 overflow-hidden rounded-3xl">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-left text-sm">
              <thead>
                <tr className="border-b border-line/60 font-mono text-[10px] tracking-[0.16em] uppercase text-mist">
                  <th className="px-6 py-4 font-medium">Dimension</th>
                  <th className="px-6 py-4 font-medium">GitHub Actions</th>
                  <th className="px-6 py-4 font-medium text-ok">EC2 runner</th>
                  <th className="px-6 py-4 font-medium text-cy">CodeBuild</th>
                </tr>
              </thead>
              <tbody>
                {COMPARE.map((c) => (
                  <tr key={c.row} className="border-b border-line/30 last:border-0">
                    <td className="px-6 py-3.5 font-medium text-frost">{c.row}</td>
                    <td className="px-6 py-3.5 text-mist">{c.gh}</td>
                    <td className="px-6 py-3.5 text-mist">{c.ec2}</td>
                    <td className="px-6 py-3.5 text-mist">{c.cb}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </Reveal>

      {/* caveats */}
      <Reveal delay={160}>
        <div className="glass mt-6 rounded-3xl border border-am/25 p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-am/10 text-am">
              <AlertTriangle size={17} />
            </span>
            <p className="font-display text-sm font-semibold tracking-[0.12em] uppercase">
              AWS caveats — read before you build
            </p>
          </div>
          <ul className="mt-5 grid gap-3 sm:grid-cols-2">
            {CAVEATS.map((c) => (
              <li key={c} className="flex items-start gap-2.5 text-sm leading-relaxed text-mist">
                <X size={14} className="mt-0.5 shrink-0 text-am" />
                <span>{c}</span>
              </li>
            ))}
          </ul>
        </div>
      </Reveal>

      {/* supporting services strip */}
      <Reveal delay={200}>
        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          {[
            { icon: Database, t: "Secrets Manager", d: "Keystore + passwords as SECRETS_MANAGER env vars in CodeBuild." },
            { icon: Cpu, t: "ECR custom image", d: "aws/Dockerfile bakes JDK 17 + SDK 34 + Gradle into the build image." },
            { icon: MonitorSmartphone, t: "S3 + CloudFront", d: "Host the APK, scan a QR code, sideload from your phone." },
          ].map((s) => (
            <div key={s.t} className="glass flex items-start gap-3.5 rounded-2xl p-5">
              <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-vi/10 text-vi">
                <s.icon size={17} />
              </span>
              <div>
                <p className="font-display text-sm font-semibold">{s.t}</p>
                <p className="mt-1 text-xs leading-relaxed text-mist">{s.d}</p>
              </div>
            </div>
          ))}
        </div>
      </Reveal>
    </section>
  );
}
