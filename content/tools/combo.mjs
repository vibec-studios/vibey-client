// Quick look at several bundles worn together (with the offsetWhen rules applied like the client does):
//   node tools/combo.mjs <out.png> <a.vibey>[:anim=t,...] <b.vibey>[:anim=t,...] … [--yaw 90,180,0] [--pitch 10] [--size 420]
// Each bundle's offsetWhen rules whose slot is worn by another listed bundle become a position/rotation on its root
// bones (Blockbench convention, like BoneRenderer). Pets are drawn at the client's ground spot when --pet-offset is given.
import { readFileSync, writeFileSync } from "node:fs";
import { Img } from "./canvas.mjs";
import { loadBundle, buildScene, camera, sceneBounds, fitView, render } from "./render/renderer.mjs";

const args = process.argv.slice(2);
const opt = (k, d) => {
  const i = args.indexOf(k);
  if (i < 0) return d;
  const v = args[i + 1];
  args.splice(i, 2);
  return v;
};
const yaws = opt("--yaw", "90,180,0").split(",").map(Number);
const pitch = +opt("--pitch", "10");
const size = +opt("--size", "420");
const petOff = opt("--pet-offset", null);
const [out, ...items] = args;

const parsed = items.map((it) => {
  const m = it.match(/^(.*?\.vibey)(?::(.*))?$/);
  const json = JSON.parse(readFileSync(m[1], "utf8"));
  const anims = (m[2] ?? "idle=0").split(",").filter(Boolean).map((s) => {
    const [nw, t] = s.split("=");
    const [name, w] = nw.split("@");
    return { name, t: +t, weight: w ? +w : 1 };
  });
  return { json, anims };
});
const worn = new Set(parsed.map((p) => p.json.cosmetic?.slot));
for (const p of parsed) {
  const rules = (p.json.cosmetic?.offsetWhen ?? []).filter((r) => r.slot !== p.json.cosmetic.slot && worn.has(r.slot));
  if (!rules.length) continue;
  const off = [0, 0, 0], rot = [0, 0, 0];
  for (const r of rules) for (let c = 0; c < 3; c++) (off[c] += r.offset?.[c] ?? 0), (rot[c] += r.rotation?.[c] ?? 0);
  const bones = {};
  for (const b of p.json.model.bones.filter((b) => !b.parent))
    bones[b.name] = { position: [{ t: 0, v: off, i: "linear" }], rotation: [{ t: 0, v: rot, i: "linear" }] };
  p.json.animations.__offsetWhen = { length: 1, loop: true, bones };
  p.anims.push({ name: "__offsetWhen", t: 0, weight: 1 });
  console.log(p.json.id, "offsetWhen", off, rot);
}

const shots = [];
for (const yaw of yaws) {
  const cam = camera(yaw, pitch);
  let scene = null;
  parsed.forEach((p, i) => {
    const B = loadBundle(p.json);
    const o = { player: i === 0, anims: p.anims, time: p.anims[0]?.t ?? 0, particles: false };
    if (p.json.cosmetic?.pet && petOff) {
      o.worldOffset = petOff.split(",").map(Number);
      o.petScale = p.json.cosmetic.pet.scale;
    }
    const s = buildScene(B, o);
    if (!scene) scene = s;
    else scene.quads.push(...s.quads);
  });
  const view = fitView(sceneBounds(scene, cam, undefined, false), size, size, 16, 40);
  shots.push(render(scene, { width: size, height: size, cam, view, ss: 3 }));
}
const img = new Img(shots.length * size, size);
img.fill("#15151b");
shots.forEach((s, i) => img.draw(s, i * size, 0, true));
writeFileSync(out, img.toPNG());
