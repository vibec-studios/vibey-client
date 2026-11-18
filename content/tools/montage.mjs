// Dev helper: tile PNGs into one image for quick review.
// Usage: node tools/montage.mjs out.png [--cols N] [--scale S] a.png b.png …
import { readFileSync, writeFileSync } from "node:fs";
import { decodePNG } from "./png.mjs";
import { Img } from "./canvas.mjs";

const args = process.argv.slice(2);
const out = args.shift();
let cols = 4, scale = 1;
const files = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] === "--cols") cols = +args[++i];
  else if (args[i] === "--scale") scale = +args[++i];
  else files.push(args[i]);
}
const imgs = files.map((f) => {
  const d = decodePNG(readFileSync(f));
  return Img.fromRGBA(d.width, d.height, d.data);
});
const cw = Math.max(...imgs.map((i) => i.width)) * scale, ch = Math.max(...imgs.map((i) => i.height)) * scale;
const rows = Math.ceil(imgs.length / cols);
const canvas = new Img(cols * cw, rows * ch).fill("#2a2a33");
imgs.forEach((im, k) => {
  const ox = (k % cols) * cw, oy = Math.floor(k / cols) * ch;
  for (let y = 0; y < im.height * scale; y++)
    for (let x = 0; x < im.width * scale; x++) {
      const chk = ((x >> 4) + (y >> 4)) & 1 ? "#33333d" : "#2a2a33";
      canvas.set(ox + x, oy + y, chk);
      canvas.blend(ox + x, oy + y, im.get(Math.floor(x / scale), Math.floor(y / scale)));
    }
});
writeFileSync(out, canvas.toPNG());
