// Ember Sparks – a slowly turning Vibe rune ring at your feet with purple and teal embers rising from it.
import { C, cosmetic, mix } from "./_common.mjs";

const R = 12; // ring radius (texture is 2R × 2R)
const SPOTS = 6;

export default function () {
  const b = cosmetic("ember_sparks", {
    name: { en: "Ember Sparks", de: "Glutfunken" },
    description: {
      en: "A glowing rune ring at your feet with purple and teal embers drifting up.",
      de: "Ein leuchtender Runenring zu deinen Füßen, aus dem lila und türkise Funken aufsteigen.",
    },
    tags: ["aura", "sparks", "glow", "vibe", "starter"],
    rarity: "epic",
    cosmetic: { slot: "aura" },
  });
  const tex = b.texture("main");
  const ring = b.bone("ring", { attach: "world", pivot: [0, 0, 0], emissive: true, translucent: true });
  const paintRing = (img) => {
    img.fill([0, 0, 0, 0]);
    for (let y = 0; y < 2 * R; y++)
      for (let x = 0; x < 2 * R; x++) {
        const dx = x + 0.5 - R, dy = y + 0.5 - R;
        const d = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
        const seg = Math.floor(((a + Math.PI) / (2 * Math.PI)) * 16);
        if (d > R - 1.6 && d <= R - 0.2) img.set(x, y, seg % 2 ? [139, 108, 255, 230] : [182, 163, 255, 245]);
        else if (d > R - 3.2 && d <= R - 2.4 && seg % 4 === 0) img.set(x, y, [63, 224, 197, 220]);
        else if (d > R - 6 && d <= R - 5 && seg % 2 === 0) img.set(x, y, [139, 108, 255, 150]);
        else if (d <= R - 1.6) img.set(x, y, [139, 108, 255, Math.round(40 * Math.max(0, 1 - (R - 1.6 - d) / 5))]);
      }
  };
  ring.box([-R, 0.05, -R], [2 * R, 0, 2 * R], { up: paintRing, down: (img) => img.fill([0, 0, 0, 0]) });
  for (let i = 0; i < SPOTS; i++) {
    const a = (i / SPOTS) * Math.PI * 2;
    b.locator(`spot_${i}`, "ring", [+(Math.cos(a) * (R - 1)).toFixed(2), 0.6, +(Math.sin(a) * (R - 1)).toFixed(2)]);
  }
  const ember = tex.region(3, 3, (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, [".p.", "pWp", ".p."], { p: C.brand, W: "#f3eaff" }));
  const spark = tex.region(3, 3, (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, [".t.", "tWt", ".t."], { t: C.teal, W: "#e6fffa" }));
  const mote = tex.region(2, 2, (img) => img.fill(mix(C.brandLight, "#ffffff", 0.3)));
  b.sprite("ember", "main", ember).sprite("spark", "main", spark).sprite("mote", "main", mote);

  const idle = b.anim("idle", 6, true);
  idle.rot("ring", [[0, [0, 0, 0]], [6, [0, 360, 0]]], "linear");
  const kinds = ["ember", "spark", "ember", "mote"];
  for (let k = 0; k < 30; k++) {
    const sprite = kinds[k % kinds.length];
    idle.particle(+(k * 0.2).toFixed(2), {
      sprite, locator: `spot_${(k * 5) % SPOTS}`, count: sprite === "mote" ? 2 : 1,
      lifetime: 1.5, size: sprite === "mote" ? 0.06 : 0.12, spread: [0.15, 0.05, 0.15],
      velocity: [0, 0.035, 0], gravity: -0.0012, emissive: true,
    });
  }
  return b;
}
