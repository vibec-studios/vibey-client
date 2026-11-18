// Seal Summon – conjure the Vibe seal into your arms in a puff of snow, cuddle it, and let it go.
import { emote, aim, SHOULDER, C, soundFile } from "./_common.mjs";
import { addSeal } from "./_seal.mjs";

const HANDS = [0, 15, -9]; // where the hands meet in front of the chest
const S = 0.5; // seal size in the arms

export default function () {
  const b = emote("seal_summon", {
    name: { en: "Seal Summon", de: "Robben-Beschwörung" },
    description: {
      en: "Conjure a seal into your arms in a flurry of snow. Squeak!",
      de: "Beschwöre eine Robbe in deine Arme – in einem Wirbel aus Schnee. Quiek!",
    },
    tags: ["seal", "magic", "props", "particles", "sound", "starter"],
    rarity: "epic",
  });
  // prop: the seal lies across the arms, head to the left-front so you can see its face
  const s = addSeal(b, { prefix: "seal", attach: "root", o: [HANDS[0], HANDS[1] + 1.5, HANDS[2]] });
  b.bones.get(s.root).rotation = [0, 55, 0];
  b.bone("hands", { attach: "root", pivot: HANDS });
  b.locator("hands", "hands", [HANDS[0], HANDS[1] + 2, HANDS[2]]);
  // original synthesized sounds (tools/synth.mjs), embedded as OGG Vorbis
  b.sound("chime", soundFile("chime")).sound("pop", soundFile("pop")).sound("squeak", soundFile("squeak"));
  const tex = b.defaultTexture();
  b.sprite("snow", "main", tex.region(3, 3, (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, [".w.", "wWw", ".w."], { w: [210, 238, 255, 235], W: "#ffffff" })));
  b.sprite("sparkle", "main", tex.region(5, 5, (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, ["..t..", "..t..", "ttWtt", "..t..", "..t.."], { t: C.teal, W: "#ffffff" })));

  const R = aim(SHOULDER.right, [-2.2, 15, -9]);
  const L = aim(SHOULDER.left, [2.2, 15, -9]);
  const cup = (v, d) => [v[0] + d, v[1], 0];
  const a = b.anim("main", 4.2);
  // arms: reach out, cradle, let go
  a.rot("right_arm", [[0, [0, 0, 0]], [0.4, cup(R, 8)], [0.6, R], [0.75, cup(R, -5)], [0.9, R], [2.9, R], [3.15, cup(R, 15)], [3.7, [10, 0, 0]], [4.2, [0, 0, 0]]]);
  a.rot("left_arm", [[0, [0, 0, 0]], [0.4, cup(L, 8)], [0.6, L], [0.75, cup(L, -5)], [0.9, L], [2.9, L], [3.15, cup(L, 15)], [3.7, [10, 0, 0]], [4.2, [0, 0, 0]]]);
  a.rot("head", [[0, [0, 0, 0]], [0.5, [-22, 0, 0]], [1.0, [-26, 10, 6]], [1.6, [-20, 14, 8]], [2.2, [4, -6, 0]], [2.8, [-18, 8, 4]], [3.3, [8, 0, 0]], [4.2, [0, 0, 0]]]);
  a.rot("root", [[0, [0, 0, 0]], [1.1, [0, 0, 0]], [1.5, [0, 0, 3]], [1.9, [0, 0, -3]], [2.3, [0, 0, 2]], [2.7, [0, 0, 0]], [4.2, [0, 0, 0]]]);
  // the seal pops in, wiggles, blinks, pops out
  a.visible(0, s.root, false).visible(0.58, s.root, true).visible(3.2, s.root, false);
  const sc = (k) => [S * k, S * k, S * k];
  a.scale(s.root, [[0, [0, 0, 0]], [0.58, [0, 0, 0]], [0.75, sc(1.25)], [0.9, sc(0.92)], [1.05, sc(1)], [2.95, sc(1)], [3.05, sc(1.2)], [3.2, [0, 0, 0]], [4.2, [0, 0, 0]]]);
  a.rot(s.head, [[0, [0, 0, 0]], [1.1, [0, 0, 0]], [1.4, [-8, 20, 0]], [1.8, [4, 20, 0]], [2.2, [-6, -10, 0]], [2.6, [0, 0, 0]], [4.2, [0, 0, 0]]]);
  const flap = [[0, [0, 0, 0]], [1.2, [0, 0, 0]], [1.3, [0, 0, -25]], [1.4, [0, 0, 5]], [1.5, [0, 0, -25]], [1.6, [0, 0, 5]], [1.7, [0, 0, -25]], [1.85, [0, 0, 0]], [4.2, [0, 0, 0]]];
  a.rot(s.finL, flap);
  a.rot(s.finR, flap.map(([t, v]) => [t, [0, 0, -v[2]]]));
  a.rot(s.tailL, [[0, [0, 0, 0]], [1.9, [0, 0, 0]], [2.1, [0, 18, 0]], [2.3, [0, -6, 0]], [2.5, [0, 18, 0]], [2.7, [0, 0, 0]], [4.2, [0, 0, 0]]]);
  a.rot(s.tailR, [[0, [0, 0, 0]], [1.9, [0, 0, 0]], [2.1, [0, -18, 0]], [2.3, [0, 6, 0]], [2.5, [0, -18, 0]], [2.7, [0, 0, 0]], [4.2, [0, 0, 0]]]);
  a.visible(0, s.lids, false).visible(2.35, s.lids, true).visible(2.5, s.lids, false);

  // snow flurry in, sparkles, snow puff out
  for (const [t, v] of [[0.35, [0.03, 0.02, 0]], [0.42, [-0.03, 0.02, 0]], [0.5, [0, 0.03, 0.03]], [0.58, [0, 0.03, -0.03]]])
    a.particle(t, { sprite: "snow", locator: "hands", count: 5, lifetime: 0.9, size: 0.1, spread: [0.35, 0.25, 0.35], velocity: v, gravity: 0.002, emissive: true });
  a.particle(0.6, { particle: "minecraft:snowflake", locator: "hands", count: 8, spread: [0.4, 0.3, 0.4], velocity: [0, 0.04, 0] });
  a.particle(0.62, { sprite: "sparkle", locator: "hands", count: 5, lifetime: 0.6, size: 0.14, spread: [0.45, 0.35, 0.45], velocity: [0, 0.01, 0], emissive: true });
  a.particle(3.15, { particle: "minecraft:snowflake", locator: "hands", count: 8, spread: [0.35, 0.25, 0.35], velocity: [0, 0.05, 0] });
  a.particle(3.15, { sprite: "snow", locator: "hands", count: 10, lifetime: 0.8, size: 0.1, spread: [0.3, 0.2, 0.3], velocity: [0, 0.05, 0], gravity: 0.004, emissive: true });
  a.sound(0.45, "chime", { volume: 0.9, pitch: 1 });
  a.sound(0.62, "minecraft:entity.player.levelup", { volume: 0.35, pitch: 1.8 });
  a.sound(1.3, "squeak", { volume: 0.8, pitch: 1 });
  a.sound(2.1, "minecraft:entity.dolphin.play", { volume: 0.7, pitch: 1.4 });
  a.sound(3.12, "pop", { volume: 0.9, pitch: 1 });
  a.sound(3.15, "chime", { volume: 0.5, pitch: 1.3 });
  return b;
}
