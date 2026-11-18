// Seal Buddy – the Vibe seal (seal3d.ts blueprint) as a little pet that follows you around.
import { cosmetic } from "./_common.mjs";
import { addSeal } from "./_seal.mjs";

export default function () {
  const b = cosmetic("seal_buddy", {
    name: { en: "Seal Buddy", de: "Robbenkumpel" },
    description: {
      en: "Our mascot as a tiny pet. Blinks, wiggles its flippers and galumphs after you.",
      de: "Unser Maskottchen als kleines Haustier. Blinzelt, wackelt mit den Flossen und robbt dir hinterher.",
    },
    tags: ["seal", "cute", "mascot", "pet", "starter"],
    rarity: "epic",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 0.4 } },
  });
  const s = addSeal(b);

  // ---------------------------------------------------------------- idle: look around, wiggle, blink
  const idle = b.anim("idle", 4, true);
  idle.rot(s.head, [
    [0, [0, 0, 0]], [0.6, [-5, 0, 0]], [1.2, [-2, 16, 0]], [1.9, [-2, 16, 0]],
    [2.5, [3, -10, -4]], [3.2, [0, -6, 0]], [4, [0, 0, 0]],
  ]);
  const flap = (a) => [[0, [0, a, 0]], [0.35, [0, a, 0]], [0.5, [0, a, a > 0 ? -18 : 18]], [0.65, [0, a, a > 0 ? 6 : -6]], [0.8, [0, a, a > 0 ? -18 : 18]], [0.95, [0, a, 0]], [4, [0, a, 0]]];
  // rest rotations live on the bones; animation values are offsets, so flap around 0
  idle.rot(s.finL, flap(0).map(([t, v]) => [t, [0, 0, -v[2] || 0]]));
  idle.rot(s.finR, flap(0).map(([t, v]) => [t, [0, 0, v[2] || 0]]));
  idle.rot(s.tailL, [[0, [0, 0, 0]], [1, [0, 10, 0]], [2, [0, 0, 0]], [3, [0, 10, 0]], [4, [0, 0, 0]]]);
  idle.rot(s.tailR, [[0, [0, 0, 0]], [1, [0, -10, 0]], [2, [0, 0, 0]], [3, [0, -10, 0]], [4, [0, 0, 0]]]);
  idle.rot(s.root, [[0, [0, 0, 0]], [2, [-1.5, 0, 0]], [4, [0, 0, 0]]]);
  idle.visible(0, s.lids, false).visible(2.7, s.lids, true).visible(2.85, s.lids, false);

  // ---------------------------------------------------------------- walk: galumphing hop
  const walk = b.anim("walk", 0.8, true);
  walk.pos(s.root, [[0, [0, 0, 0]], [0.2, [0, 2.5, 0]], [0.4, [0, 0, 0]], [0.6, [0, 2.5, 0]], [0.8, [0, 0, 0]]]);
  walk.rot(s.root, [[0, [-4, 0, 0]], [0.2, [4, 0, 0]], [0.4, [-4, 0, 0]], [0.6, [4, 0, 0]], [0.8, [-4, 0, 0]]]);
  walk.rot(s.head, [[0, [5, 0, 0]], [0.2, [-6, 0, 0]], [0.4, [5, 0, 0]], [0.6, [-6, 0, 0]], [0.8, [5, 0, 0]]]);
  walk.rot(s.finL, [[0, [0, 22, 0]], [0.2, [0, -14, 0]], [0.4, [0, 22, 0]], [0.6, [0, -14, 0]], [0.8, [0, 22, 0]]]);
  walk.rot(s.finR, [[0, [0, -22, 0]], [0.2, [0, 14, 0]], [0.4, [0, -22, 0]], [0.6, [0, 14, 0]], [0.8, [0, -22, 0]]]);
  walk.rot(s.tailL, [[0, [0, -12, 0]], [0.2, [0, 14, 0]], [0.4, [0, -12, 0]], [0.6, [0, 14, 0]], [0.8, [0, -12, 0]]]);
  walk.rot(s.tailR, [[0, [0, 12, 0]], [0.2, [0, -14, 0]], [0.4, [0, 12, 0]], [0.6, [0, -14, 0]], [0.8, [0, 12, 0]]]);
  return b;
}
