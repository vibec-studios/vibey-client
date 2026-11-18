// Vibe Chevron Shield – stacked purple and teal chevrons on a dark field.
import { C, cosmetic, shieldLayout, paintShieldBack, gradient } from "./_common.mjs";

export default function () {
  const b = cosmetic("vibe_chevron_shield", {
    name: { en: "Vibe Chevron Shield", de: "Vibe-Sparren-Schild" },
    description: {
      en: "Bold purple and teal chevrons – the Vibe colours, made to block.",
      de: "Kräftige lila und türkise Sparren – die Vibe-Farben, gemacht zum Blocken.",
    },
    tags: ["vibe", "chevron", "starter"],
    rarity: "common",
    cosmetic: { slot: "shield", shield: { texture: "shield" } },
  });
  b.texture("shield", { width: 64, height: 64, notModel: true }).paint((img) => {
    const L = shieldLayout(img);
    const f = L.front;
    gradient(f, "#18181f", "#0b0b0e", 0.03, 61);
    // chevrons: V shapes, 2 px thick, alternating colours
    const cols = [[C.brand, C.brandLight], [C.teal, C.tealLight]];
    for (let k = 0; k < 4; k++) {
      const top = 2 + k * 5;
      const [c, hi] = cols[k % 2];
      for (let x = 0; x < 12; x++) {
        const dx = x < 6 ? 5 - x : x - 6; // 0 at centre
        const y = top + (5 - dx);
        f.set(x, y - 5, hi).set(x, y - 4, c).set(x, y - 3, c);
      }
    }
    f.border("#08080b").row(0, "#26262f");
    paintShieldBack(L, "#22222b", 62);
  });
  return b;
}
