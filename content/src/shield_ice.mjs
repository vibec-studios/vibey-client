// Glacier Shield – faceted ice with a frosted rim and a snowflake emblem.
import { C, cosmetic, shieldLayout, paintShieldBack, gradient, mix } from "./_common.mjs";

export default function () {
  const b = cosmetic("glacier_shield", {
    name: { en: "Glacier Shield", de: "Gletscherschild" },
    description: {
      en: "Cut from a glacier, polished by a very patient seal.",
      de: "Aus einem Gletscher geschnitten, poliert von einer sehr geduldigen Robbe.",
    },
    tags: ["ice", "winter", "snowflake", "starter"],
    rarity: "rare",
    cosmetic: { slot: "shield", shield: { texture: "shield" } },
  });
  b.texture("shield", { width: 64, height: 64, notModel: true }).paint((img) => {
    const L = shieldLayout(img);
    const f = L.front;
    gradient(f, "#d9ecf8", "#7fb2dc", 0.03, 71);
    // facets: diagonal light/dark streaks
    for (let i = -22; i < 22; i += 5) {
      f.line(i, 0, i + 22, 22, mix(C.iceLight, "#ffffff", 0.5));
      f.line(i + 2, 0, i + 24, 22, mix(C.iceDark, C.iceDeep, 0.3));
    }
    // inner panel behind the emblem
    f.rect(2, 5, 8, 11, mix(C.iceDeep, "#1d3b5c", 0.4)).border(mix(C.iceDeep, "#ffffff", 0.3), 2, 5, 8, 11);
    f.stamp(3, 7, ["..ww..", "w.ww.w", ".wwww.", "wwbbww", ".wwww.", "w.ww.w", "..ww.."], { w: "#ffffff", b: C.teal });
    f.border("#f6fbff").border(C.iceSide, 1, 1, 10, 20);
    f.px(C.iceDeep, [[0, 0], [11, 0], [0, 21], [11, 21]]);
    paintShieldBack(L, "#4d7fae", 72);
  });
  return b;
}
