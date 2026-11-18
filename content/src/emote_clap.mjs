// Clap – four claps with snare hits and little sparkles.
import { emote, addHands, C } from "./_common.mjs";

export default function () {
  const b = emote("clap", {
    name: { en: "Clap", de: "Klatschen" },
    description: { en: "Give it up! Four claps with a snappy beat.", de: "Applaus! Vier Klatscher mit knackigem Beat." },
    tags: ["applause", "sound", "starter"],
  });
  addHands(b);
  const star = b.defaultTexture().region(3, 3, (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, [".w.", "wWw", ".w."], { w: C.warn, W: "#ffffff" }));
  b.sprite("star", "main", star);
  b.locator("clap", "right_hand", [-4, 12, 0]);
  const a = b.anim("main", 2.2);
  const claps = [0.5, 0.85, 1.2, 1.55];
  const R = [[0, [0, 0, 0]], [0.3, [62, -8, 0]]], L = [[0, [0, 0, 0]], [0.3, [62, 8, 0]]];
  for (const t of claps) {
    R.push([t - 0.12, [64, -10, 0]], [t, [62, 24, 0]]);
    L.push([t - 0.12, [64, 10, 0]], [t, [62, -24, 0]]);
  }
  R.push([1.8, [50, -6, 0]], [2.2, [0, 0, 0]]);
  L.push([1.8, [50, 6, 0]], [2.2, [0, 0, 0]]);
  a.rot("right_arm", R).rot("left_arm", L);
  a.rot("head", [[0, [0, 0, 0]], [0.5, [-4, 0, 0]], [0.85, [2, 0, 0]], [1.2, [-4, 0, 0]], [1.55, [2, 0, 0]], [2.2, [0, 0, 0]]]);
  for (const t of claps) {
    a.sound(t, "minecraft:block.note_block.snare", { volume: 0.7, pitch: 1.35 });
    a.particle(t, { sprite: "star", locator: "clap", count: 3, lifetime: 0.35, size: 0.1, spread: [0.15, 0.1, 0.05], velocity: [0, 0.03, 0], emissive: true });
  }
  return b;
}
