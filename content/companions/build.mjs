import { readdirSync, readFileSync, writeFileSync, mkdirSync, existsSync, rmSync, statSync, copyFileSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { stringify } from "../tools/vibey.mjs";
import { validate } from "../tools/validate.mjs";
import { loadBundle, sheet, thumbnail } from "../tools/render/preview.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, "..", "..");
const DIST = join(HERE, "..", "dist", "companions");
const BUILTIN = join(ROOT, "libs", "companio", "src", "main", "resources", "assets", "companio", "companionpacks");
const args = process.argv.slice(2);
const noPreview = args.includes("--no-preview");
const only = args.filter((a) => !a.startsWith("--"));

const packs = readdirSync(HERE).filter((d) => existsSync(join(HERE, d, "companion.json")) && (!only.length || only.includes(d)));
let failed = 0;
mkdirSync(DIST, { recursive: true });
for (const id of packs) failed += await buildPack(id);
writeIndex();
process.exit(failed ? 1 : 0);

async function buildPack(id) {
  const src = join(HERE, id), out = join(DIST, id);
  rmSync(out, { recursive: true, force: true });
  mkdirSync(out, { recursive: true });
  let errors = 0, hero = null;
  const modelDir = join(src, "model");
  const models = existsSync(modelDir) ? readdirSync(modelDir).filter((f) => f.endsWith(".mjs") && !f.startsWith("_")) : [];
  for (const f of models) {
    const mod = await import(pathToFileURL(join(modelDir, f)).href);
    const bundle = await mod.default();
    if (mod.textures) {
      mkdirSync(join(out, "textures"), { recursive: true });
      for (const [file, png] of Object.entries(mod.textures())) writeFileSync(join(out, "textures", file), png);
    }
    const { json } = bundle.build();
    const bytes = Buffer.from(stringify(json) + "\n", "utf8");
    const v = validate(bytes);
    const name = f.replace(/\.mjs$/, "");
    mkdirSync(join(out, "models"), { recursive: true });
    writeFileSync(join(out, "models", `${name}.vibey`), bytes);
    console.log(`${v.ok ? "ok  " : "FAIL"} ${id}/models/${name}.vibey ${(bytes.length / 1024).toFixed(1)} KiB`);
    for (const e of v.errors) console.log(`     error: ${e}`);
    for (const w of v.warnings) if (!w.includes("is not a cosmetic state")) console.log(`     warn:  ${w}`);
    if (!v.ok) errors++;
    if (!noPreview) {
      const B = loadBundle(json);
      if (bundle.preview?.panels) writeFileSync(join(DIST, `${id}-${name}-preview.png`), sheet(B, json.name.en, bundle.preview.panels(null, B), bundle.preview.sheet ?? {}).toPNG());
      hero ??= { B, thumb: bundle.preview?.thumb ?? { cam: { yaw: 35, pitch: 14 }, player: false, petScale: 1, time: 0.6, anims: [{ name: "idle", t: 0.6 }] } };
    }
  }
  copyTree(src, out, (p) => !p.startsWith("model/"));
  if (!existsSync(join(out, "pack.png")) && hero) writeFileSync(join(out, "pack.png"), thumbnail(hero.B, hero.thumb, 64).toPNG());
  const dst = join(BUILTIN, id);
  rmSync(dst, { recursive: true, force: true });
  copyTree(out, dst, () => true);
  return errors;
}

function copyTree(from, to, keep) {
  for (const f of walk(from)) {
    const rel = relative(from, f).replaceAll("\\", "/");
    if (!keep(rel)) continue;
    mkdirSync(dirname(join(to, rel)), { recursive: true });
    copyFileSync(f, join(to, rel));
  }
}

function walk(dir) {
  const out = [];
  for (const f of readdirSync(dir)) {
    const p = join(dir, f);
    if (statSync(p).isDirectory()) out.push(...walk(p));
    else out.push(p);
  }
  return out;
}

function writeIndex() {
  if (!existsSync(BUILTIN)) return;
  const list = readdirSync(BUILTIN).filter((d) => existsSync(join(BUILTIN, d, "companion.json"))).sort((a, b) => order(a) - order(b) || a.localeCompare(b));
  const index = { packs: list.map((d) => ({ id: d, files: walk(join(BUILTIN, d)).map((f) => relative(join(BUILTIN, d), f).replaceAll("\\", "/")).sort() })) };
  writeFileSync(join(BUILTIN, "packs.json"), JSON.stringify(index, null, 2) + "\n");
}

function order(id) {
  const i = ["seal", "penguin", "robot"].indexOf(id);
  return i < 0 ? 99 : i;
}
