// Ice Floe Cape – a little seal resting on an ice floe under an arctic night sky.
import { C, cosmetic, capeLayout, gradient, shade, mix } from "./_common.mjs";

export default function () {
  const b = cosmetic("ice_floe_cape", {
    name: { en: "Ice Floe Cape", de: "Eisscholle-Umhang" },
    description: {
      en: "A tiny seal napping on its ice floe under the polar night.",
      de: "Eine kleine Robbe döst auf ihrer Eisscholle unter der Polarnacht.",
    },
    tags: ["seal", "ice", "winter", "starter"],
    rarity: "rare",
    cosmetic: { slot: "cape", cape: { texture: "cape" } },
  });
  b.texture("cape", { width: 64, height: 32, notModel: true }).paint((img) => paint(img));
  return b;
}

function scene(v, seed) {
  // sky
  gradient(v.view(0, 0, 10, 9), "#16284a", "#2b5687", 0.02, seed);
  v.px("#ffffff", [[2, 1], [7, 3]]).px("#bcd9ee", [[5, 0], [8, 6], [1, 5]]);
  // water
  gradient(v.view(0, 9, 10, 7), "#2f6aa0", "#163a63", 0.03, seed + 1);
  v.px("#5f97c7", [[1, 14], [2, 14], [6, 15], [7, 15], [8, 13]]);
  // ice floe
  v.row(10, C.iceLight, 1, 8).row(11, C.ice, 0, 9).row(12, C.iceSide, 0, 9).row(13, C.iceDark, 1, 8);
  v.px(C.iceLight, [[0, 11], [9, 11]]).px(C.iceDeep, [[2, 13], [7, 13]]);
  // seal (side view, head raised on the right)
  v.stamp(1, 6, [
    "......##.",
    ".....#e#s",
    "..#####bb",
    "f######b.",
  ], { "#": C.body, b: C.belly, e: C.eye, s: C.snout, f: C.flipper });
  v.px(C.back, [[3, 8], [4, 8], [5, 8]]).px(C.nose, [[9, 7]]);
}

export function paint(img) {
  const L = capeLayout(img);
  scene(L.outer, 21);
  L.outer.col(0, shade("#16284a", 0.8), 0, 9).col(9, shade("#16284a", 0.8), 0, 9);
  gradient(L.inner, "#cfe5f5", "#9cc4e2", 0.02, 22);
  L.inner.px(C.iceLight, [[2, 3], [7, 8], [4, 12]]);
  L.top.noisy("#16284a", 0.02, 23);
  L.bottom.noisy("#163a63", 0.02, 24);
  L.left.noisy(C.iceDeep, 0.02, 25);
  L.right.noisy(C.iceDeep, 0.02, 26);
  gradient(L.elytraOuter, "#16284a", "#2f6aa0", 0.02, 27);
  L.elytraOuter.row(15, C.iceLight).row(16, C.ice).row(17, C.iceSide).row(18, C.iceDark);
  L.elytraOuter.px("#ffffff", [[2, 2], [7, 5], [4, 9]]);
  gradient(L.elytraInner, "#cfe5f5", "#9cc4e2", 0.02, 28);
  for (const e of L.elytraEdges) e.noisy(C.iceDeep, 0.02, 29);
  return mix;
}
