// One self-contained, offline HTML page for a collection: thumbnails + in-game screenshots (from ShowcaseGameTest)
// embedded as base64, item names, descriptions and animations, Vibe look.
//
//   node tools/showcase-html.mjs --dir dist/polar-pals --shots <screenshot folder> --plan src/polar-pals/showcase.json
//        --out dist/polar-pals/polar-pals.html [--title "Polar Pals"] [--width 960] [--quality 5]
//
// Screenshots (sc_<item>_<nn>_<what>.png, sc_hero_<name>.png) are scaled to --width and JPEG-encoded with ffmpeg
// (must be on PATH) to keep the page small.
import { readFileSync, readdirSync, writeFileSync, mkdtempSync, rmSync, existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { tmpdir } from "node:os";
import { join, resolve, basename } from "node:path";

const args = process.argv.slice(2);
const opt = (k, d) => (args.indexOf(k) >= 0 ? args[args.indexOf(k) + 1] : d);
const dir = resolve(opt("--dir", "dist/polar-pals"));
const shots = resolve(opt("--shots", "."));
const planFile = opt("--plan", null);
const out = resolve(opt("--out", join(dir, "index.html")));
const title = opt("--title", "Collection");
const width = +opt("--width", "960");
const quality = opt("--quality", "5");
const plan = planFile ? JSON.parse(readFileSync(planFile, "utf8")) : {};

const esc = (s) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
const tmp = mkdtempSync(join(tmpdir(), "showcase-"));
let jpgBytes = 0;
function jpeg(png) {
  const o = join(tmp, basename(png).replace(/\.png$/, ".jpg"));
  execFileSync("ffmpeg", ["-v", "error", "-y", "-i", png, "-vf", `scale=${width}:-2:flags=lanczos`, "-q:v", quality, o]);
  const b = readFileSync(o);
  jpgBytes += b.length;
  return "data:image/jpeg;base64," + b.toString("base64");
}
const pngData = (f) => "data:image/png;base64," + readFileSync(f).toString("base64");

const CAPTION = {
  "01_front": "front", "02_side": "side", "03_back": "back", "20_walk_a": "walking", "21_walk_b": "walking (later)",
  "30_fly_a": "creative flying", "31_fly_b": "flying (later)", "32_elytra": "elytra gliding", "40_sneak": "sneaking",
};
function caption(rest, item) {
  if (CAPTION[rest]) return CAPTION[rest];
  const m = rest.match(/^1\d_idle_(\d+)_(\d+)$/);
  if (m) {
    const t = `${m[1]}.${m[2]}`;
    const label = item?.moments?.[t];
    return `idle @ ${t} s${label ? " · " + label : ""}`;
  }
  return rest.replace(/_/g, " ");
}

const index = JSON.parse(readFileSync(join(dir, "index.json"), "utf8")).items;
// plan order first (the plan lists the centrepiece first), then the rest
const order = Object.keys(plan.items ?? {});
const pos = (id) => (order.indexOf(id) < 0 ? 999 : order.indexOf(id));
index.sort((a, b) => pos(a.id) - pos(b.id));
const allShots = existsSync(shots) ? readdirSync(shots).filter((f) => /^sc_.*\.png$/.test(f)).sort() : [];
const RARITY = { common: "#a1a1aa", rare: "#3fe0c5", epic: "#8b6cff", legendary: "#ffb547" };
const SLOT_DE = { pet: "Pet", wings: "Flügel", hat: "Hut", back: "Rücken", aura: "Aura", cape: "Umhang", shield: "Schild" };

const heroOrder = (plan.hero ?? []).map((h) => `sc_hero_${h.name}.png`);
const hpos = (f) => (heroOrder.indexOf(f) < 0 ? 999 : heroOrder.indexOf(f));
const heroes = allShots.filter((f) => f.startsWith("sc_hero_")).sort((a, b) => hpos(a) - hpos(b));
let html = "";
html += `<section class="hero"><div class="grid hero-grid">${heroes
  .map((f) => `<figure><img loading="lazy" src="${jpeg(join(shots, f))}" alt=""><figcaption>${esc(f.replace(/^sc_hero_|\.png$/g, "").replace(/_/g, " "))}</figcaption></figure>`)
  .join("")}</div></section>`;

for (const it of index) {
  const name = it.file.replace(/\.vibey$/, "");
  const json = JSON.parse(readFileSync(join(dir, it.file), "utf8"));
  const p = plan.items?.[it.id] ?? {};
  const anims = Object.entries(json.animations ?? {}).map(([k, a]) => {
    const ev = (a.events ?? []).filter((e) => e.type !== "visibility").length;
    return `<span class="chip">${esc(k)} · ${a.length}s${a.loop ? " loop" : ""}${ev ? ` · ${ev} fx` : ""}</span>`;
  });
  const bones = json.model?.bones?.length ?? 0;
  const tex = json.textures?.[0];
  const mine = allShots.filter((f) => f.startsWith(`sc_${name}_`));
  const thumb = join(dir, "thumbnails", `${name}.png`);
  html += `<section class="item" id="${esc(name)}">
  <div class="head">
    ${existsSync(thumb) ? `<img class="thumb" src="${pngData(thumb)}" alt="">` : ""}
    <div class="meta">
      <div class="tags"><span class="rarity" style="--c:${RARITY[it.rarity] ?? "#a1a1aa"}">${esc(it.rarity)}</span><span class="slot">${esc(it.slot)} · ${esc(SLOT_DE[it.slot] ?? it.slot)}</span><code>${esc(it.id)}</code></div>
      <h2>${esc(it.name.en)} <small>${esc(it.name.de)}</small></h2>
      <p>${esc(json.description?.en)}</p>
      <p class="de">${esc(json.description?.de)}</p>
      ${p.story ? `<p class="story">${esc(p.story)}</p>` : ""}
      <div class="chips">${anims.join("")}<span class="chip dim">${bones} bones · ${tex ? `${tex.width}×${tex.height} UV${tex.animation ? ` · ${tex.animation.frames.length} frames` : ""}` : ""} · ${(it.size / 1024).toFixed(0)} KiB</span></div>
    </div>
  </div>
  <div class="grid">${mine
    .map((f) => {
      const rest = f.slice(`sc_${name}_`.length).replace(/\.png$/, "");
      return `<figure><img loading="lazy" src="${jpeg(join(shots, f))}" alt=""><figcaption>${esc(caption(rest, p))}</figcaption></figure>`;
    })
    .join("")}</div>
</section>`;
}

const notes = plan.notes ?? [];
const page = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}</title>
<style>
:root{--bg:#000;--s1:#0b0b0e;--s2:#121217;--s3:#18181f;--line:#22222a;--text:#f4f4f5;--muted:#a1a1aa;--dim:#6b6b75;--brand:#8b6cff;--teal:#3fe0c5}
*{box-sizing:border-box}html{background:var(--bg)}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.55 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
header{padding:48px 24px 28px;border-bottom:1px solid var(--line);background:linear-gradient(180deg,#141022,#000)}
.wrap{max-width:1240px;margin:0 auto;padding:0 16px}
h1{margin:0;font-size:44px;letter-spacing:.02em}h1 span{color:var(--teal)}
.sub{color:var(--muted);max-width:820px}
nav{display:flex;flex-wrap:wrap;gap:6px;margin-top:18px}nav a{color:var(--text);text-decoration:none;border:1px solid var(--line);background:var(--s2);padding:4px 10px;font-size:13px}nav a:hover{border-color:var(--brand)}
section{padding:28px 0;border-bottom:1px solid var(--line)}
.head{display:flex;gap:20px;align-items:flex-start;margin-bottom:16px}
.thumb{width:128px;height:128px;image-rendering:pixelated;background:var(--s2);border:1px solid var(--line);flex:none}
.meta{min-width:0}h2{margin:6px 0 6px;font-size:24px}h2 small{color:var(--muted);font-weight:400;font-size:16px;margin-left:6px}
p{margin:4px 0;color:#d4d4d8}p.de{color:var(--muted)}p.story{color:var(--teal);font-size:14px}
.tags{display:flex;flex-wrap:wrap;gap:8px;align-items:center;font-size:12px}
.rarity{border:1px solid var(--c);color:var(--c);padding:1px 7px;text-transform:uppercase;letter-spacing:.08em}
.slot{color:var(--muted);text-transform:uppercase;letter-spacing:.08em}code{color:var(--dim);font:12px ui-monospace,Consolas,monospace}
.chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px}.chip{font:12px ui-monospace,Consolas,monospace;border:1px solid var(--line);background:var(--s2);padding:2px 8px;color:var(--teal)}.chip.dim{color:var(--dim)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:10px}.hero-grid{grid-template-columns:repeat(auto-fill,minmax(380px,1fr))}
figure{margin:0;background:var(--s1);border:1px solid var(--line)}figure img{display:block;width:100%;height:auto}
figcaption{padding:5px 9px;font-size:12px;color:var(--muted);border-top:1px solid var(--line)}
.notes{columns:2 360px;color:var(--muted);font-size:14px}.notes li{margin-bottom:6px;break-inside:avoid}
footer{padding:24px 0 48px;color:var(--dim);font-size:13px}
@media (max-width:640px){.head{flex-direction:column}h1{font-size:32px}}
</style></head><body>
<header><div class="wrap"><h1>${esc(title)} <span>·</span> Vibey Client</h1>
<p class="sub">${esc(plan.tagline ?? "")}</p>
<nav>${index.map((it) => `<a href="#${esc(it.file.replace(/\.vibey$/, ""))}">${esc(it.name.en.replace(/^.*·\s*/, ""))}</a>`).join("")}</nav></div></header>
<main class="wrap">${html}
${notes.length ? `<section><h2>Engine notes</h2><ul class="notes">${notes.map((n) => `<li>${esc(n)}</li>`).join("")}</ul></section>` : ""}
</main>
<footer class="wrap">Screenshots: Minecraft 1.21.11 (Fabric), Vibey Client dev build, items loaded as local DEV items by <code>ShowcaseGameTest</code>. Original artwork by Vibe Craft Studios – local test build, not in the catalog.</footer>
</body></html>
`;
writeFileSync(out, page);
rmSync(tmp, { recursive: true, force: true });
console.log(`${out}: ${(page.length / 1024 / 1024).toFixed(2)} MB (${allShots.length} screenshots, JPEG ${(jpgBytes / 1024 / 1024).toFixed(2)} MB)`);
