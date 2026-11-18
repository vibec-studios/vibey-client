// Pixel Stars Cape – night sky with a crescent moon, twinkling stars and a shooting star (4 frames).
import { C, cosmetic, capeLayout, gradient, rng } from "./_common.mjs";

const FRAMES = 4;

export default function () {
  const b = cosmetic("pixel_stars_cape", {
    name: { en: "Pixel Stars Cape", de: "Pixelsterne-Umhang" },
    description: {
      en: "A clear night sky full of twinkling pixel stars.",
      de: "Ein klarer Nachthimmel voller funkelnder Pixelsterne.",
    },
    tags: ["night", "stars", "animated", "starter"],
    rarity: "rare",
    cosmetic: { slot: "cape", cape: { texture: "cape" } },
  });
  b.texture("cape", { width: 64, height: 32, frames: FRAMES, frameTime: 8, notModel: true }).paint((img, f) => paint(img, f));
  return b;
}

function sky(v, f, seed, moon) {
  const w = v.width, h = v.height;
  gradient(v, "#0d0b2a", "#251a5c", 0.025, seed);
  const r = rng(seed);
  for (let i = 0; i < Math.round((w * h) / 7); i++) {
    const x = Math.floor(r() * w), y = Math.floor(r() * h), k = Math.floor(r() * FRAMES), big = r() < 0.18;
    const on = (f + k) % FRAMES !== 0;
    if (big && on && (f + k) % 2 === 0) {
      v.set(x, y, "#ffffff").blend(x - 1, y, "#b6a3ff", 0.7).blend(x + 1, y, "#b6a3ff", 0.7).blend(x, y - 1, "#b6a3ff", 0.7).blend(x, y + 1, "#b6a3ff", 0.7);
    } else v.set(x, y, on ? (r() < 0.5 ? "#dcd3ff" : "#9af3e3") : "#4a3f8a");
  }
  if (moon) {
    v.stamp(w - 5, 1, ["..mm", ".mm.", ".mm.", ".mm.", "..mm"], { m: "#fff4cf" });
    v.px("#e8d9a8", [[w - 4, 2], [w - 4, 3], [w - 4, 4]]);
  }
}

export function paint(img, f) {
  const L = capeLayout(img);
  sky(L.outer, f, 41, true);
  // shooting star in frame 2
  if (f === 2) L.outer.px("#ffffff", [[2, 7]]).px("#9af3e3", [[3, 6]]).px("#3fe0c5", [[4, 5]]).px("#8b6cff", [[5, 4]]);
  L.outer.col(0, "#0a0820").col(9, "#0a0820").row(15, C.brand);
  sky(L.inner, f, 42, false);
  L.top.noisy("#0d0b2a", 0.02, 43);
  L.bottom.noisy(C.brandDark, 0.02, 44);
  L.left.noisy("#0a0820", 0.02, 45);
  L.right.noisy("#0a0820", 0.02, 46);
  sky(L.elytraOuter, f, 47, true);
  sky(L.elytraInner, f, 48, false);
  for (const e of L.elytraEdges) e.noisy("#0a0820", 0.02, 49);
}
