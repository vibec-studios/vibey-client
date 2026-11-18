// Aurora Cape – animated northern lights over snowy peaks (8-frame strip).
import { C, cosmetic, capeLayout, gradient, mix, rng } from "./_common.mjs";

const FRAMES = 8;

export default function () {
  const b = cosmetic("aurora_cape", {
    name: { en: "Aurora Cape", de: "Polarlicht-Umhang" },
    description: {
      en: "Northern lights that slowly ripple across your back.",
      de: "Nordlichter, die langsam über deinen Rücken wogen.",
    },
    tags: ["aurora", "animated", "night", "starter"],
    rarity: "epic",
    cosmetic: { slot: "cape", cape: { texture: "cape" } },
  });
  b.texture("cape", { width: 64, height: 32, frames: FRAMES, frameTime: 4, notModel: true }).paint((img, f) => paint(img, f));
  return b;
}

const BAND_A = ["#3fe0c5", "#62f0a8", "#9af3e3"];
const BAND_B = ["#8b6cff", "#b06cff", "#dcd3ff"];

/** Paint an aurora sky into view v (w×h) for frame f. */
function aurora(v, f, seed, phase = 0) {
  const w = v.width, h = v.height;
  gradient(v, "#070b22", "#132048", 0.02, seed);
  const r = rng(seed + 99);
  const stars = [...Array(Math.round((w * h) / 14))].map(() => [Math.floor(r() * w), Math.floor(r() * h * 0.7), r()]);
  for (const [x, y, k] of stars) {
    const tw = Math.sin((f / FRAMES) * Math.PI * 2 + k * 6.28) > 0.3;
    v.set(x, y, tw ? "#ffffff" : "#6f7fb0");
  }
  const t = (f / FRAMES) * Math.PI * 2;
  const base = h * 0.32;
  for (let x = 0; x < w; x++) {
    // one bright ribbon (purple fringe on top, white-teal core, green curtain fading down) + soft rays
    const c = base + Math.sin(x * 0.6 + t + phase) * 1.6 + Math.sin(x * 0.25 - t) * 0.8;
    const ray = 0.6 + 0.4 * Math.sin(x * 1.7 + t * 2 + phase);
    for (let y = 0; y < h; y++) {
      const d = y - c;
      if (d >= -1.5 && d < -0.5) v.blend(x, y, "#b06cff", 0.75);
      else if (d >= -0.5 && d < 0.5) v.blend(x, y, "#c9fff4", 0.95);
      else if (d >= 0.5 && d < 1.5) v.blend(x, y, "#3fe0c5", 0.9);
      else if (d >= 1.5 && d < 4.5) v.blend(x, y, d < 3 ? "#2fc79a" : "#1f9e8a", (0.75 - (d - 1.5) * 0.2) * ray);
    }
  }
}

const PROFILE = [3, 2, 1, 2, 3, 3, 2, 0, 1, 2, 3, 2];
function peaks(v, base) {
  const w = v.width, h = v.height;
  for (let x = 0; x < w; x++) {
    const top = base + PROFILE[x % PROFILE.length];
    for (let y = top; y < h; y++) {
      const snow = y === top && PROFILE[x % PROFILE.length] <= 1;
      v.set(x, y, snow ? C.iceLight : mix("#2a3a68", "#141c3c", (y - top) / Math.max(1, h - top)));
    }
  }
}

export function paint(img, f) {
  const L = capeLayout(img);
  aurora(L.outer, f, 31);
  peaks(L.outer, 11);
  L.outer.col(0, "#0b1230", 0, 11).col(9, "#0b1230", 0, 11);
  aurora(L.inner, f, 32, 1.3);
  L.top.noisy("#070b22", 0.02, 33);
  L.bottom.noisy("#121a38", 0.02, 34);
  L.left.noisy("#0b1230", 0.02, 35);
  L.right.noisy("#0b1230", 0.02, 36);
  aurora(L.elytraOuter, f, 37);
  peaks(L.elytraOuter, 15);
  aurora(L.elytraInner, f, 38, 2);
  for (const e of L.elytraEdges) e.noisy("#0b1230", 0.02, 39);
}
