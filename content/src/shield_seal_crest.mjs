// Seal Crest Shield – Vibe purple field with the seal emblem and a teal chevron.
import { C, cosmetic, shieldLayout, paintShieldBack, gradient, SEAL_FACE, mix } from "./_common.mjs";

export default function () {
  const b = cosmetic("seal_crest_shield", {
    name: { en: "Seal Crest Shield", de: "Robbenwappen-Schild" },
    description: {
      en: "Carry the seal into battle – Vibe purple with a teal chevron.",
      de: "Trag die Robbe in den Kampf – Vibe-Lila mit türkisem Sparren.",
    },
    tags: ["seal", "vibe", "crest", "starter"],
    rarity: "rare",
    cosmetic: { slot: "shield", shield: { texture: "shield" } },
  });
  b.texture("shield", { width: 64, height: 64, notModel: true }).paint((img) => {
    const L = shieldLayout(img);
    const f = L.front;
    gradient(f, "#7a5cf5", "#4a33b0", 0.025, 51);
    f.border(C.brandNight).border(mix(C.brandLight, C.brand, 0.3), 1, 1, 10, 20);
    // crest
    f.stamp(2, 4, SEAL_FACE.rows, SEAL_FACE.colors);
    // chevron below
    f.stamp(2, 13, [
      "tt....tt",
      "Ltt..ttL",
      ".Lttttl.",
      "..Lttl..",
      "...LL...",
    ], { t: C.teal, L: C.tealLight, l: C.tealDark });
    f.px(C.brandPale, [[5, 2], [6, 2]]);
    paintShieldBack(L, "#3a2a6e", 52);
  });
  return b;
}
