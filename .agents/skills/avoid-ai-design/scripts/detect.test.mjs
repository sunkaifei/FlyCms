// Regression tests for scripts/detect.mjs. Zero dependencies: `node --test scripts/detect.test.mjs`
// avoid-ai-design-ignore-file  (this file holds intentional slop fixtures as strings)
import { test } from "node:test";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const DETECT = new URL("./detect.mjs", import.meta.url).pathname;
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "aad-"));
const app = path.join(tmp, "app");
fs.mkdirSync(app, { recursive: true });
const put = (name, body) => { const p = path.join(app, name); fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, body); return p; };
const run = (args, input) => spawnSync(process.execPath, [DETECT, ...args], { input, encoding: "utf8", cwd: tmp });
const ids = (file, extra = []) => {
  const r = run(["--json", ...extra, file]);
  return new Set(JSON.parse(r.stdout).findings.map((f) => f.id));
};

const SLOP_TSX = `import { Sparkles, Zap, Rocket, ArrowRight } from "lucide-react";
import { Spotlight } from "@/components/ui/spotlight";
import { motion } from "framer-motion";
import { Inter } from "next/font/google";
const inter = Inter({ subsets: ["latin"] });
export default function Page() {
  return (
    <main className="container mx-auto px-4">
      <nav className="fixed top-0 backdrop-blur-md bg-white/70">Acme</nav>
      <section className="container mx-auto px-4 text-center">
        <span className="rounded-full bg-indigo-50 px-3 py-1">✨ New: AI features</span>
        <h1 className="text-6xl">Elevate your workflow with <span className="bg-gradient-to-r from-indigo-500 to-purple-600 bg-clip-text text-transparent">AI</span></h1>
        <button className="bg-indigo-600 text-white rounded-2xl shadow-lg">Get Started →</button>
      </section>
      <motion.div initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} />
      <div className="grid md:grid-cols-3 gap-6">
        <div className="rounded-2xl shadow-lg p-6"><div className="h-12 w-12 rounded-xl bg-indigo-500/10"><Zap /></div></div>
        <div className="rounded-2xl shadow-lg p-6"><div className="h-12 w-12 rounded-xl bg-indigo-500/10"><Rocket /></div></div>
        <div className="rounded-2xl shadow-lg p-6"><div className="h-12 w-12 rounded-xl bg-indigo-500/10"><Sparkles /></div></div>
      </div>
      <div className="border-l-4 border-indigo-500 p-4">Faster, smarter, simpler.</div>
      <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=a" alt="" />
      <div className="max-w-7xl mx-auto">x</div>
      <Spotlight />
    </main>
  );
}
`;
const CLEAN_HTML = `<!doctype html><html lang="en"><head><style>
  @font-face { font-family: "Tidewater Grotesk"; src: url(t.woff2) format("woff2"); }
  :root { --ground:#e9eef2; --ink:#16232d; --route:#1f6f8b; --mut:#45525c; }
  body { font-family: "Tidewater Grotesk", Georgia, serif; background: var(--ground); color: var(--ink); }
  .note { color: var(--mut); }
  .link { box-shadow: inset 0 -2px 0 var(--route); }
  a:focus-visible, button:focus-visible { outline: 3px solid var(--route); }
</style></head><body>
  <h1>Next boat to Orcas: 14:20</h1>
  <p class="note">Service runs every 50 minutes until 22:10.</p>
  <div class="rows"><div class="row"></div><div class="row"></div><div class="row"></div></div>
  <p><span>Anacortes → Orcas</span></p>
  <button>Buy a walk-on ticket</button>
</body></html>
`;

test("flags the stock React/Tailwind page", () => {
  const found = ids(put("src/Page.tsx", SLOP_TSX));
  for (const id of ["C1", "C2", "C6", "T1", "L1", "L2", "L6", "K2", "K3", "K4", "K6", "K10", "I3", "M1", "SD5", "CP1", "CP3", "CP4", "IM4"])
    assert.ok(found.has(id), `expected ${id}`);
});

test("a deliberate page produces no findings and exit 0", () => {
  const file = put("src/clean.html", CLEAN_HTML);
  const r = run(["--json", file]);
  assert.equal(r.status, 0);
  assert.deepEqual(JSON.parse(r.stdout).findings, []);
});

test("no false positives: skeleton rows (SD7), inset underline (C5), route arrow (CP3), chip tint as button (C2)", () => {
  const found = ids(put("src/clean2.html", CLEAN_HTML));
  for (const id of ["SD7", "C5", "CP3", "C2"]) assert.ok(!found.has(id), `did not expect ${id}`);
  assert.equal([...ids(put("src/Page2.tsx", SLOP_TSX))].filter((i) => i === "C2").length, 1);
});

test("L4 counts proof strips, not percentages in a data table", () => {
  const strip = '<div><b>12,000+</b><span>Teams</span></div><div><b>8B</b></div><div>12,000<b>+</b></div><div><b>99.99%</b></div>';
  assert.ok(ids(put("l4/strip.html", strip)).has("L4"));
  const table = "<table><tr><td>100%</td><td>71%</td><td>58%</td><td>52%</td></tr></table>";
  assert.ok(!ids(put("l4/table.html", table)).has("L4"));
});

test("untouched shadcn theme", () => {
  const css = put("shadcn/globals.css", ":root {\n  --primary: oklch(0.205 0 0);\n  --radius: 0.625rem;\n}\n");
  const found = ids(css);
  assert.ok(found.has("K1") && found.has("K8"));
  assert.ok(ids(put("shadcn/components.json", '{ "tailwind": { "baseColor": "zinc" } }')).has("K1"));
});

test("second-order clusters: cream + terracotta (SD1), near-black + acid green (SD2)", () => {
  assert.ok(ids(put("sd/cream.tsx", '<body className="bg-amber-50"><h2 className="text-orange-700">Heirloom</h2></body>')).has("SD1"));
  assert.ok(ids(put("sd/dark.tsx", '<body className="bg-zinc-950"><span className="text-lime-400">Live</span></body>')).has("SD2"));
  assert.ok(ids(put("sd/cream.html", "<style>body{background:#f4f1ea}.a{color:#d97757}</style><h1>Ledger</h1>")).has("SD1"));
});

test("a face declared but never loaded (T7)", () => {
  assert.ok(ids(put("t7.html", '<style>body{font-family:"Söhne",system-ui}</style><h1>Rates</h1>')).has("T7"));
});

test("ignore comments: line, file with IDs, whole file", () => {
  const line = SLOP_TSX.replace('<div className="border-l-4', '{/* avoid-ai-design-ignore: K4 */}\n      <div className="border-l-4');
  assert.ok(!ids(put("ign/line.tsx", line)).has("K4"));
  const some = ids(put("ign/ids.tsx", "// avoid-ai-design-ignore-file: C1, T1\n" + SLOP_TSX));
  assert.ok(!some.has("C1") && !some.has("T1") && some.has("C6"));
  assert.equal(ids(put("ign/all.tsx", "// avoid-ai-design-ignore-file\n" + SLOP_TSX)).size, 0);
});

test("skips slop-example files and example folders unless --include-examples", () => {
  const marked = put("marked.html", "<!-- slop-example -->\n<style>.g{background:linear-gradient(90deg,#6366f1,#a855f7)}</style>");
  assert.equal(ids(marked).size, 0);
  assert.ok(ids(marked, ["--include-examples"]).has("C1"));
});

test("hook mode: exit 2 with stderr on tells, 0 when clean, 0 for fixtures and bad input", () => {
  const bad = run(["--hook"], JSON.stringify({ tool_input: { file_path: put("src/Hook.tsx", SLOP_TSX) } }));
  assert.equal(bad.status, 2);
  assert.match(bad.stderr, /P0 C1/);
  assert.equal(run(["--hook"], JSON.stringify({ tool_input: { file_path: path.join(app, "src/clean.html") } })).status, 0);
  const fx = path.join(tmp, "fixtures", "Slop.tsx"); fs.mkdirSync(path.dirname(fx), { recursive: true }); fs.writeFileSync(fx, SLOP_TSX);
  assert.equal(run(["--hook"], JSON.stringify({ tool_input: { file_path: fx } })).status, 0);
  assert.equal(run(["--hook"], "not json").status, 0);
});

test("exit code 2 on P0/P1, and --min filters", () => {
  const file = path.join(app, "src/Page.tsx");
  assert.equal(run([file]).status, 2);
  const p0 = JSON.parse(run(["--json", "--min=P0", file]).stdout);
  assert.ok(p0.findings.length > 0 && p0.findings.every((f) => f.severity === "P0"));
});
