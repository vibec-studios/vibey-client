// Neon Pixel Wings – glowing outline wings in Vibe purple + teal with a light pulse running along the edge.
import { C, cosmetic, mix } from "./_common.mjs";

const FRAMES = 8;
const L = 18, H = 16;
// silhouette per column (x = 0 at the shoulder, 17 = tip)
const TOP = [6, 6, 5, 5, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 1];
const BOT = [9, 10, 11, 12, 13, 12, 13, 14, 14, 13, 14, 15, 15, 13, 13, 11, 8, 5];
const SPLITS = [5, 9, 13, 16]; // feather separations

function wingFace(img, f) {
  img.fill([0, 0, 0, 0]);
  for (let x = 0; x < L; x++)
    for (let y = TOP[x]; y <= BOT[x]; y++) {
      const edgeTop = y === TOP[x] || (x > 0 && y < TOP[x - 1]) || (x < L - 1 && y < TOP[x + 1]);
      const edgeBot = y === BOT[x] || (x > 0 && y > BOT[x - 1]) || (x < L - 1 && y > BOT[x + 1]) || x === L - 1;
      const split = SPLITS.includes(x) && y > BOT[x] - 4;
      // pulse travelling from the shoulder to the tip
      const phase = ((x + (H - y) * 0.4 - (f / FRAMES) * (L + 8)) % (L + 8) + (L + 8)) % (L + 8);
      const pulse = phase < 3 ? 1 - phase / 3 : 0;
      if (edgeTop) img.set(x, y, mix(C.teal, "#ffffff", 0.15 + pulse * 0.7));
      else if (edgeBot || x === 0) img.set(x, y, mix(C.brand, "#ffffff", 0.1 + pulse * 0.6));
      else if (split) img.set(x, y, mix(C.brandDeep, C.brand, 0.4 + pulse * 0.6));
      else {
        const g = Math.max(0, 1 - (y - TOP[x]) / 6);
        img.set(x, y, [...mix(mix(C.brandNight, C.tealDeep, g * 0.6), C.brand, pulse * 0.25).slice(0, 3), 175]);
      }
    }
}

function mirror(img) {
  const w = img.width;
  for (let y = 0; y < img.height; y++)
    for (let x = 0; x < w >> 1; x++) {
      const a = img.get(x, y), c = img.get(w - 1 - x, y);
      img.set(x, y, c).set(w - 1 - x, y, a);
    }
}

export default function () {
  const b = cosmetic("neon_wings", {
    name: { en: "Neon Pixel Wings", de: "Neon-Pixelflügel" },
    description: {
      en: "Glowing pixel wings in Vibe purple and teal with a light pulse running along the edge.",
      de: "Leuchtende Pixelflügel in Vibe-Lila und Türkis, an deren Kante ein Lichtpuls entlangläuft.",
    },
    tags: ["neon", "wings", "glow", "vibe", "starter"],
    rarity: "epic",
    cosmetic: { slot: "wings", hideWithArmor: ["chest"] },
  });
  b.texture("main", { frames: FRAMES, frameTime: 2 });
  for (const s of [1, -1]) {
    const side = s > 0 ? "left" : "right";
    const root = b.bone(`wing_${side}`, { attach: "body", pivot: [s * 1.5, 21, 3], rotation: [0, s * -20, -s * 12], emissive: true, translucent: true });
    root.box([s > 0 ? 1.5 : -1.5 - L, 27 - H, 2.5], [L, H, 1], {
      north: (img, f) => { wingFace(img, f); if (s < 0) mirror(img); },
      south: (img, f) => { wingFace(img, f); if (s > 0) mirror(img); },
      up: (img) => img.fill([0, 0, 0, 0]),
      down: (img) => img.fill([0, 0, 0, 0]),
      sides: (img) => img.fill([0, 0, 0, 0]),
    });
  }
  const idle = b.anim("idle", 2.4, true);
  idle.rot("wing_left", [[0, [0, 0, 0]], [1.2, [0, -8, -3]], [2.4, [0, 0, 0]]]);
  idle.rot("wing_right", [[0, [0, 0, 0]], [1.2, [0, 8, 3]], [2.4, [0, 0, 0]]]);
  const fly = b.anim("fly", 0.7, true);
  fly.rot("wing_left", [[0, [0, 12, 4]], [0.35, [0, -50, -10]], [0.7, [0, 12, 4]]]);
  fly.rot("wing_right", [[0, [0, -12, -4]], [0.35, [0, 50, 10]], [0.7, [0, -12, -4]]]);
  return b;
}
