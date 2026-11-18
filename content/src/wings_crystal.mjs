// Crystal Ice Wings – fans of translucent ice shards with a glint running along them.
import { C, cosmetic, mix } from "./_common.mjs";

const FRAMES = 6;
// [length, height, fan angle]
const SHARDS = [
  [22, 4, 50],
  [19, 3, 32],
  [16, 3, 15],
  [12, 3, -1],
  [9, 2, -17],
];

function shardFace(img, f, k) {
  const L = img.width, H = img.height;
  img.fill([0, 0, 0, 0]);
  for (let x = 0; x < L; x++)
    for (let y = 0; y < H; y++) {
      // taper the last pixels into a point
      const fromTip = L - 1 - x;
      if (fromTip === 0 && y !== Math.floor(H / 2)) continue;
      if (fromTip === 1 && H > 2 && y === H - 1) continue;
      const t = x / (L - 1);
      let c = mix([96, 184, 240, 190], [214, 244, 255, 220], t);
      if (y === 0) c = mix([215, 238, 252, 225], [255, 255, 255, 240], t);
      if (y === H - 1 && H > 2) c = mix([70, 140, 210, 205], [140, 205, 245, 215], t);
      img.set(x, y, c);
    }
  // travelling glint
  const gx = Math.round(((f / FRAMES) * (L + 6)) - 3 + k);
  for (let y = 0; y < H; y++) {
    const x = gx - y;
    if (x >= 0 && x < L && img.get(x, y)[3]) img.set(x, y, [255, 255, 255, 250]);
  }
}

export default function () {
  const b = cosmetic("crystal_wings", {
    name: { en: "Crystal Ice Wings", de: "Kristalleisflügel" },
    description: {
      en: "Shards of glacier ice that shimmer as a glint runs along them.",
      de: "Splitter aus Gletschereis, über die ein Lichtfunke wandert.",
    },
    tags: ["ice", "wings", "crystal", "starter"],
    rarity: "legendary",
    cosmetic: { slot: "wings", hideWithArmor: ["chest"] },
  });
  b.texture("main", { frames: FRAMES, frameTime: 3 });
  for (const s of [1, -1]) {
    const side = s > 0 ? "left" : "right";
    const root = b.bone(`wing_${side}`, { attach: "body", pivot: [s * 1.5, 20, 3], rotation: [0, s * -24, 0], translucent: true });
    SHARDS.forEach(([L, H, ang], k) => {
      const bone = b.bone(`wing_${side}_${k}`, { parent: root.name, pivot: [s * 1.5, 20, 3], rotation: [0, 0, -s * ang], translucent: true });
      bone.box([s > 0 ? 1.5 : -1.5 - L, 20 - H / 2, 2.6 + k * 0.02], [L, H, 1], {
        north: (img, f) => { shardFace(img, f, k); if (s < 0) mirror(img); },
        south: (img, f) => { shardFace(img, f, k); if (s > 0) mirror(img); },
        up: (img) => img.fill([235, 248, 255, 225]),
        down: (img) => img.fill([140, 195, 235, 205]),
        sides: (img) => img.fill([210, 236, 252, 215]),
      });
    });
  }
  const idle = b.anim("idle", 3, true);
  idle.rot("wing_left", [[0, [0, 0, 0]], [1.5, [0, -7, 2]], [3, [0, 0, 0]]]);
  idle.rot("wing_right", [[0, [0, 0, 0]], [1.5, [0, 7, -2]], [3, [0, 0, 0]]]);
  const fly = b.anim("fly", 0.8, true);
  fly.rot("wing_left", [[0, [0, 10, 0]], [0.4, [0, -45, 8]], [0.8, [0, 10, 0]]]);
  fly.rot("wing_right", [[0, [0, -10, 0]], [0.4, [0, 45, -8]], [0.8, [0, -10, 0]]]);
  for (let k = 0; k < SHARDS.length; k++) {
    const lag = [[0, [0, 0, 0]], [0.4, [0, 0, 4 + k]], [0.8, [0, 0, 0]]];
    fly.rot(`wing_left_${k}`, lag);
    fly.rot(`wing_right_${k}`, lag.map(([t, v]) => [t, [0, 0, -v[2]]]));
  }
  return b;
}

function mirror(img) {
  const w = img.width;
  for (let y = 0; y < img.height; y++)
    for (let x = 0; x < w >> 1; x++) {
      const a = img.get(x, y), c = img.get(w - 1 - x, y);
      img.set(x, y, c).set(w - 1 - x, y, a);
    }
}
