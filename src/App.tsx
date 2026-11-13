import { AppPreview } from "./components/AppPreview";
import { AwsPath } from "./components/AwsPath";
import { Footer, Nav } from "./components/Chrome";
import { Features } from "./components/Features";
import { Hero } from "./components/Hero";
import { InstallGuide } from "./components/InstallGuide";
import { Pipeline } from "./components/Pipeline";
import { RealityCheck } from "./components/RealityCheck";
import { RepoChecklist } from "./components/RepoChecklist";

export default function App() {
  return (
    <div className="grain relative min-h-screen bg-abyss text-frost antialiased">
      <Nav />
      <main>
        <Hero />
        <RealityCheck />
        <AppPreview />
        <Features />
        <Pipeline />
        <AwsPath />
        <RepoChecklist />
        <InstallGuide />
      </main>
      <Footer />
    </div>
  );
}
