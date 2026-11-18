// Builds every item in content/src (one script per item) into content/dist:
//   dist/<name>.vibey, dist/previews/<name>.png, dist/thumbnails/<name>.png, dist/index.json
// Usage: node tools/build.mjs [name-filter …] [--no-preview]
import { readdirSync, writeFileSync, mkdirSync, readFileSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { stringify } from "./vibey.mjs";
import { validate } from "./validate.mjs";
import { loadBundle, sheet, thumbnail } from "./render/preview.mjs";
import { previewPlan } from "./plans.mjs";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const SRC = join(ROOT, "src");
const DIST = join(ROOT, "dist");
const args = process.argv.slice(2);
const noPreview = args.includes("--no-preview");
const filters = args.filter((a) => !a.startsWith("--"));

mkdirSync(join(DIST, "previews"), { recursive: true });
mkdirSync(join(DIST, "thumbnails"), { recursive: true });

const files = readdirSync(SRC).filter((f) => f.endsWith(".mjs") && !f.startsWith("_")).sort();
let failed = 0;
const index = [];
for (const f of files) {
  const mod = await import(pathToFileURL(join(SRC, f)).href);
  const bundle = await mod.default();
  const name = bundle.file;
  const matches = !filters.length || filters.some((x) => f.includes(x) || name.includes(x));
  const t0 = Date.now();
  const { json } = bundle.build();
  const text = stringify(json) + "\n";
  const bytes = Buffer.from(text, "utf8");
  const v = validate(bytes);
  const out = join(DIST, `${name}.vibey`);
  if (matches || !existsSync(out)) writeFileSync(out, bytes);
  const sha = createHash("sha256").update(bytes).digest("hex");
  index.push({
    id: json.id, kind: json.kind, slot: json.cosmetic?.slot ?? null, file: `${name}.vibey`,
    thumbnail: `thumbnails/${name}.png`, preview: `previews/${name}.png`,
    name: json.name, rarity: json.rarity, tags: json.tags, size: bytes.length, sha256: sha,
  });
  if (!matches) continue;
  let msg = `${v.ok ? "ok  " : "FAIL"} ${json.id.padEnd(28)} ${(bytes.length / 1024).toFixed(1).padStart(6)} KiB`;
  if (!noPreview) {
    const B = loadBundle(json);
    const plan = previewPlan(B, bundle.preview);
    writeFileSync(join(DIST, "previews", `${name}.png`), sheet(B, json.name.en, plan.panels, plan.sheet).toPNG());
    writeFileSync(join(DIST, "thumbnails", `${name}.png`), thumbnail(B, plan.thumb).toPNG());
  }
  console.log(`${msg}  ${Date.now() - t0} ms`);
  for (const e of v.errors) console.log(`     error: ${e}`);
  for (const w of v.warnings) console.log(`     warn:  ${w}`);
  if (!v.ok) failed++;
}
// keep the order stable: cosmetics by slot, then emotes
const SLOT_ORDER = ["cape", "wings", "aura", "pet", "shield", "hat", "back"];
index.sort((a, b) => (a.kind === b.kind ? 0 : a.kind === "cosmetic" ? -1 : 1) || SLOT_ORDER.indexOf(a.slot) - SLOT_ORDER.indexOf(b.slot) || a.id.localeCompare(b.id));
index.forEach((it, i) => (it.sort = (i + 1) * 10));
writeFileSync(join(DIST, "index.json"), stringify({ format: "vibey-index", formatVersion: 1, items: index }) + "\n");
console.log(`${index.length} items, ${failed} failed`);
process.exit(failed ? 1 : 0);
