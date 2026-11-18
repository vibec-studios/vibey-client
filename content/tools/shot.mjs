// Quick look at one bundle pose: node tools/shot.mjs <bundle.vibey> <out.png> [anim[@weight]=t …] [--yaw 35]
//   [--pitch 14] [--size 520] [--player] [--pet-offset x,y,z] [--cols n]
// Several "anim=t" groups separated by "+" render side by side, e.g.  idle=5.2 + walk=0.3,idle=5.2@0.5
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
const flag = (k) => {
  const i = args.indexOf(k);
  if (i >= 0) args.splice(i, 1);
  return i >= 0;
};
const yaws = opt("--yaw", "35").split(",").map(Number);
const pitch = +opt("--pitch", "14");
const size = +opt("--size", "420");
const player = flag("--player");
const petOff = opt("--pet-offset", null);
const [file, out, ...rest] = args;
const json = JSON.parse(readFileSync(file, "utf8"));
const B = loadBundle(json);
const groups = rest.join(" ").split("+").map((g) => g.trim()).filter(Boolean);
if (!groups.length) groups.push("");
const shots = [];
for (const g of groups)
  for (const yaw of yaws) {
    const anims = g.split(/[ ,]+/).filter(Boolean).map((s) => {
      const [nw, t] = s.split("=");
      const [name, w] = nw.split("@");
      return { name, t: +t, weight: w ? +w : 1 };
    });
    const p = { player, anims, time: anims[0]?.t ?? 0, fitParticles: true };
    if (petOff) p.worldOffset = petOff.split(",").map(Number);
    if (json.cosmetic?.pet && petOff) p.petScale = json.cosmetic.pet.scale;
    const scene = buildScene(B, p);
    const cam = camera(yaw, pitch);
    const view = fitView(sceneBounds(scene, cam, undefined, true), size, size, 16, 40);
    shots.push(render(scene, { width: size, height: size, cam, view, ss: 3 }));
  }
const cols = Math.min(shots.length, +opt("--cols", "4"));
const rows = Math.ceil(shots.length / cols);
const img = new Img(cols * size, rows * size);
img.fill("#15151b");
shots.forEach((s, i) => img.draw(s, (i % cols) * size, Math.floor(i / cols) * size, true));
writeFileSync(out, img.toPNG());
