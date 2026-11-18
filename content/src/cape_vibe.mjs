// Vibe Cape – brand purple with the pixel seal emblem and a teal hem.
import { C, cosmetic, capeLayout, gradient, SEAL_FACE, shade, mix } from "./_common.mjs";

export default function () {
  const b = cosmetic("vibe_cape", {
    name: { en: "Vibe Cape", de: "Vibe-Umhang" },
    description: {
      en: "The classic Vibe purple with our seal emblem and a teal hem.",
      de: "Das klassische Vibe-Lila mit unserem Robben-Emblem und türkisem Saum.",
    },
    tags: ["vibe", "seal", "purple", "starter"],
    rarity: "rare",
    cosmetic: { slot: "cape", cape: { texture: "cape" } },
  });
  b.texture("cape", { width: 64, height: 32, notModel: true }).paint((img) => paint(img));
  return b;
}

export function paint(img) {
  const L = capeLayout(img);
  const edge = C.brandDark;
  // outer face (seen from behind)
  gradient(L.outer, "#7a5cf5", "#5a3fe0", 0.025, 11);
  L.outer.col(0, edge).col(9, edge).row(0, shade(C.brandLight, 0.95));
  // emblem plate
  L.outer.rect(1, 3, 8, 8, C.brandNight);
  L.outer.border(mix(C.brandNight, C.brand, 0.45), 0, 2, 10, 10);
  L.outer.stamp(1, 4, SEAL_FACE.rows, SEAL_FACE.colors);
  // teal hem with a small pixel chevron
  L.outer.row(13, C.teal, 1, 8).row(14, C.tealDark, 1, 8).row(15, C.brandDark);
  L.outer.px(C.tealLight, [[4, 12], [5, 12]]).px(C.teal, [[3, 12], [6, 12]]);
  L.outer.px(C.tealLight, [[2, 13], [7, 13]]);
  // inner face: plain darker purple with a soft sheen
  gradient(L.inner, "#4a33b0", "#36238a", 0.02, 12);
  L.inner.row(15, C.tealDark);
  L.top.noisy(C.brandLight, 0.02, 13);
  L.bottom.noisy(C.tealDark, 0.02, 14);
  L.left.noisy(edge, 0.02, 15).set(0, 13, C.teal);
  L.right.noisy(edge, 0.02, 16).set(0, 13, C.teal);
  // elytra in the same style
  gradient(L.elytraOuter, "#7a5cf5", "#4a33b0", 0.025, 17);
  L.elytraOuter.col(0, edge).row(17, C.teal).row(18, C.tealDark);
  L.elytraOuter.stamp(1, 5, SEAL_FACE.rows, SEAL_FACE.colors);
  gradient(L.elytraInner, "#4a33b0", "#36238a", 0.02, 18);
  for (const e of L.elytraEdges) e.noisy(edge, 0.02, 19);
}
