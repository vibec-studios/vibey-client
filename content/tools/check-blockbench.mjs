// Cross-checks the starter set against the Blockbench plugin (blockbench/vibey.js) and the REAL installed Blockbench.
//
//   npm run check:blockbench                 # static checks + Blockbench: import/re-export every dist/*.vibey,
//                                            # screenshots of the default set into dist/blockbench/
//   npm run check:blockbench -- --static     # only the Node checks (no Blockbench)
//   npm run check:blockbench -- --shots all  # screenshot every item (default: SHOTS below)
//   options: --blockbench <Blockbench.exe>  --keep-open  --no-screenshots  --only <name,name>
//
// Static (Node): every bundle passes the plugin's validator (Core.validateBundle) and the plugin's independent
// SPEC validator (blockbench/test/spec-validator.js), and survives the plugin's decompile → compile round trip.
// Blockbench: like blockbench/test/blockbench-e2e.js, Blockbench is started with an isolated temporary profile
// (--userData / --user-data-dir) and remote debugging; the plugin is loaded from the repo; each bundle is
// imported with the plugin (File > Import path) and re-exported, and the re-export must be semantically equal to
// the source (numbers within 1e-6; texture PNGs compared by decoded pixels). Only the Blockbench process started
// here is closed.
import { spawn, execFileSync } from "node:child_process";
import { createRequire } from "node:module";
import { readFileSync, readdirSync, writeFileSync, mkdirSync, mkdtempSync, rmSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createServer } from "node:net";
import { decodePNG } from "./png.mjs";

const require = createRequire(import.meta.url);
const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const REPO = resolve(ROOT, "..");
const DIST = join(ROOT, "dist");
const OUT = join(DIST, "blockbench");
const PLUGIN = join(REPO, "blockbench", "vibey.js");
const Core = require(PLUGIN);
const { validate: specValidate } = require(join(REPO, "blockbench", "test", "spec-validator.js"));

const args = process.argv.slice(2);
const flag = (n) => args.includes("--" + n);
const opt = (n, d) => (args.indexOf("--" + n) >= 0 ? args[args.indexOf("--" + n) + 1] : d);
const BB_EXE = opt("blockbench", join(process.env.LOCALAPPDATA || "", "Programs", "Blockbench", "Blockbench.exe"));
/** Items screenshotted by default (front view; emotes at a mid-animation frame). */
const SHOTS = ["vibe_cape", "crystal_wings", "seal_buddy", "seal_beanie", "seal_backpack", "guitar", "seal_summon", "wave"];
/** Emote frame to show (seconds); default: half the animation length. */
const EMOTE_T = { cheer: 0.85, seal_summon: 2.0, guitar: 0.25, wave: 1.1 };
const only = opt("only")?.split(",");
const shots = opt("shots") === "all" ? null : SHOTS;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let failures = 0;
const fail = (msg) => { failures++; console.log("  ✖ " + msg); };
const pass = (msg) => console.log("  ✔ " + msg);

// ------------------------------------------------------------------ semantic comparison
function pngPixels(b64) {
  const d = decodePNG(Buffer.from(b64, "base64"));
  return { w: d.width, h: d.height, data: d.data };
}
/** Differences between two bundles: numbers within eps, PNGs by pixels, everything else exact. */
export function semanticDiff(a, b, path = "$", out = [], eps = 1e-6) {
  if (out.length > 20) return out;
  if (typeof a === "number" && typeof b === "number") {
    if (Math.abs(a - b) > eps) out.push(`${path}: ${a} != ${b}`);
  } else if (Array.isArray(a) || Array.isArray(b)) {
    if (!Array.isArray(a) || !Array.isArray(b) || a.length !== b.length) out.push(`${path}: array ${JSON.stringify(a)?.slice(0, 80)} != ${JSON.stringify(b)?.slice(0, 80)}`);
    else a.forEach((x, i) => semanticDiff(x, b[i], `${path}[${i}]`, out, eps));
  } else if (a && b && typeof a === "object" && typeof b === "object") {
    for (const k of new Set([...Object.keys(a), ...Object.keys(b)])) {
      if (!(k in a)) out.push(`${path}.${k}: only in re-export (${JSON.stringify(b[k]).slice(0, 60)})`);
      else if (!(k in b)) out.push(`${path}.${k}: missing in re-export`);
      else if (k === "png" && typeof a[k] === "string" && a[k] !== b[k]) {
        const pa = pngPixels(a[k]), pb = pngPixels(b[k]);
        if (pa.w !== pb.w || pa.h !== pb.h || Buffer.compare(Buffer.from(pa.data), Buffer.from(pb.data)) !== 0) out.push(`${path}.png: pixels differ`);
      } else semanticDiff(a[k], b[k], `${path}.${k}`, out, eps);
    }
  } else if (a !== b) out.push(`${path}: ${JSON.stringify(a)} != ${JSON.stringify(b)}`);
  return out;
}

// ------------------------------------------------------------------ static checks
const files = readdirSync(DIST).filter((f) => f.endsWith(".vibey")).sort()
  .filter((f) => !only || only.includes(f.replace(/\.vibey$/, "")));
console.log(`Static checks (plugin validator, independent SPEC validator, plugin round trip) – ${files.length} bundles`);
const sources = {};
for (const f of files) {
  const text = readFileSync(join(DIST, f), "utf8");
  const b = JSON.parse(text);
  sources[f] = { text, b };
  const v1 = Core.validateBundle(b), v2 = specValidate(b);
  const rt = Core.compileBundle(Core.decompileBundle(b));
  const d = semanticDiff(b, rt.bundle);
  const problems = [...v1.errors.map((e) => "plugin: " + e), ...v2.errors.map((e) => "spec: " + e), ...rt.errors.map((e) => "compile: " + e), ...d.map((e) => "round trip: " + e)];
  if (problems.length) fail(`${f}\n      ${problems.join("\n      ")}`);
  else pass(`${f}${v1.warnings.length + v2.warnings.length ? `  (warnings: ${[...v1.warnings, ...v2.warnings].join("; ")})` : ""}`);
}
if (flag("static")) finish();

// ------------------------------------------------------------------ Blockbench (CDP)
class CDP {
  constructor(url) {
    this.ws = new WebSocket(url);
    this.id = 0;
    this.pending = new Map();
    this.ws.addEventListener("message", (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.id && this.pending.has(msg.id)) {
        this.pending.get(msg.id)(msg);
        this.pending.delete(msg.id);
      }
    });
  }
  open() { return new Promise((res, rej) => { this.ws.addEventListener("open", res, { once: true }); this.ws.addEventListener("error", rej, { once: true }); }); }
  send(method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = ++this.id;
      this.pending.set(id, (msg) => (msg.error ? reject(new Error(method + ": " + msg.error.message)) : resolve(msg.result)));
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(expression) {
    const r = await this.send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true, userGesture: true });
    if (r.exceptionDetails) throw new Error("page: " + (r.exceptionDetails.exception?.description ?? r.exceptionDetails.text));
    return r.result.value;
  }
  run(fn, ...a) { return this.eval(`(async () => (${fn.toString()})(...${JSON.stringify(a)}))()`); }
  async waitFor(fn, timeout = 15000) {
    const t0 = Date.now();
    let last;
    while (Date.now() - t0 < timeout) {
      try { last = await this.run(fn); if (last) return last; } catch (e) { last = e.message; }
      await sleep(150);
    }
    throw new Error("timeout waiting for " + fn.toString().slice(0, 100) + " (last: " + JSON.stringify(last) + ")");
  }
  close() { try { this.ws.close(); } catch {} }
}
const freePort = () => new Promise((res) => { const s = createServer().listen(0, "127.0.0.1", () => { const p = s.address().port; s.close(() => res(p)); }); });

// page-side helpers (self-contained functions, run inside Blockbench)
const page = {
  ready: () => typeof BBPlugin !== "undefined" && typeof Formats !== "undefined" && !!document.getElementById("status_bar") && typeof Interface !== "undefined" && !!Interface.Panels,
  loadPlugin: async (file) => {
    if (window.VibeyPlugin) return true;
    await new Plugin().loadFromFile({ path: file, name: "vibey.js", content: "" }, false);
    return !!window.VibeyPlugin;
  },
  importAndExport: async (text, path) => {
    for (const p of ModelProject.all.slice()) await p.close(true);
    if (!VibeyPlugin.importBundle(text, path)) return { error: "import failed" };
    for (let i = 0; i < 200 && !Texture.all.every((t) => t.width && t.img?.complete); i++) await new Promise((r) => setTimeout(r, 50));
    if (Dialog.open) Dialog.open.close(0);
    const c = VibeyPlugin.compileProjectJSON();
    return { json: c.json, errors: c.errors, warnings: c.warnings, format: Format.id, groups: Group.all.length, cubes: Cube.all.length };
  },
  pose: async (o) => {
    VibeyPlugin.setReferenceVisible(true);
    if (o.emote) {
      Modes.options.animate.select();
      const anim = Animator.animations.find((a) => a.name === "main") || Animator.animations[0];
      anim.select();
      Timeline.setTime(o.t);
      Animator.preview();
    } else Modes.options.edit.select();
    unselectAllElements?.();
    Canvas.updateAll();
    Preview.selected.loadAnglePreset({ position: o.position, target: o.target });
    await new Promise((r) => setTimeout(r, 500));
    if (o.emote) { Timeline.setTime(o.t); Animator.preview(); }
    const r = Preview.selected.canvas.getBoundingClientRect();
    return { x: r.x, y: r.y, width: r.width, height: r.height };
  },
};

async function blockbench() {
  if (!existsSync(BB_EXE)) { fail("Blockbench not found at " + BB_EXE + " (use --blockbench <path>)"); return; }
  const profile = mkdtempSync(join(tmpdir(), "vibey-bb-content-"));
  const port = await freePort();
  const proc = spawn(BB_EXE, ["--userData", profile, "--user-data-dir=" + profile, "--remote-debugging-port=" + port, "--lang=en"], { stdio: "ignore" });
  console.log(`\nBlockbench ${BB_EXE} (pid ${proc.pid}, isolated profile ${profile})`);
  let cdp;
  try {
    let target;
    for (let i = 0; i < 120 && !target; i++) {
      try { target = (await (await fetch(`http://127.0.0.1:${port}/json`)).json()).find((t) => t.type === "page" && t.url.includes("index.html")); } catch {}
      if (!target) await sleep(500);
    }
    if (!target) throw new Error("Blockbench page did not appear on the DevTools port");
    cdp = new CDP(target.webSocketDebuggerUrl);
    await cdp.open();
    await cdp.waitFor(page.ready, 60000);
    await cdp.waitFor(() => typeof Plugins !== "undefined" && !Plugins.loading_promise, 60000);
    await sleep(1500);
    await cdp.send("Browser.setWindowBounds", { windowId: (await cdp.send("Browser.getWindowForTarget")).windowId, bounds: { width: 1400, height: 950, windowState: "normal" } }).catch(() => {});
    console.log("Blockbench " + (await cdp.eval("Blockbench.version")));
    if (!(await cdp.run(page.loadPlugin, PLUGIN))) throw new Error("plugin did not load");
    pass("plugin loaded from " + PLUGIN);
    mkdirSync(OUT, { recursive: true });
    for (const f of files) {
      const name = f.replace(/\.vibey$/, "");
      const { text, b } = sources[f];
      const r = await cdp.run(page.importAndExport, text, join(DIST, f));
      if (r.error) { fail(`${f}: ${r.error}`); continue; }
      const d = r.errors.length ? r.errors.map((e) => "export error: " + e) : semanticDiff(b, JSON.parse(r.json));
      if (d.length) fail(`${f} re-export differs:\n      ${d.join("\n      ")}`);
      else pass(`${f}: imported (${r.format}, ${r.groups} groups, ${r.cubes} cubes), re-export semantically equal${r.json === text ? " (byte-identical)" : ""}`);
      if (flag("no-screenshots") || (shots && !shots.includes(name))) continue;
      const emote = b.kind === "emote";
      const slot = b.cosmetic?.slot;
      const len = emote ? b.animations[b.emote.animation].length : 0;
      const views = [["", [-38, 30, -62]]]; // front, from the player's front-left like the previews (player faces -Z)
      if (["cape", "wings", "back"].includes(slot)) views.push(["_back", [38, 30, 62]]);
      for (const [suffix, position] of views) {
        const rect = await cdp.run(page.pose, { emote, t: EMOTE_T[name] ?? len / 2, position, target: [0, slot === "pet" ? 12 : 17, 0] });
        const shot = await cdp.send("Page.captureScreenshot", { format: "png", clip: { ...rect, scale: 1 } });
        const file = join(OUT, `${name}${suffix}.png`);
        writeFileSync(file, Buffer.from(shot.data, "base64"));
        console.log("    screenshot " + file);
      }
    }
  } catch (e) {
    fail(String(e.stack || e));
  } finally {
    cdp?.close();
    if (!flag("keep-open")) {
      try {
        if (process.platform === "win32") execFileSync("taskkill", ["/PID", String(proc.pid), "/T", "/F"], { stdio: "ignore" });
        else proc.kill();
      } catch {}
      await sleep(800);
      try { rmSync(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 300 }); } catch {}
    }
  }
}

function finish() {
  console.log(failures ? `\n${failures} problem(s)` : "\nall checks passed");
  process.exit(failures ? 1 : 0);
}

await blockbench();
finish();
