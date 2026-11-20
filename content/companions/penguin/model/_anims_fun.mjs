import { Z0, OUT, ONE, BEAK, lids, loopKeys } from "./_anims_base.mjs";

export function flap(b) {
  const L = 1.1;
  const a = b.anim("flap", L, false);
  a.pos("peng", [[0, Z0], [0.12, [0, -0.6, 0]], [0.3, [0, 2.2, 0]], [0.5, Z0], [L, Z0]]);
  a.scale("body", [[0, ONE], [0.12, [1.08, 0.9, 1.08]], [0.3, [0.95, 1.07, 0.95]], [0.5, [1.04, 0.96, 1.04]], [0.7, ONE], [L, ONE]]);
  const f = (sx) => [[0, Z0], ...[0, 1, 2, 3, 4, 5].map((i) => [0.1 + i * 0.13, [0, 0, (i % 2 ? 25 : 85) * sx]]), [0.95, Z0], [L, Z0]];
  a.rot("wing_r", f(1));
  a.rot("wing_l", f(-1));
  a.rot("jaw", [[0, Z0], [0.15, [24, 0, 0]], [0.45, Z0], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.3, [-12, 0, 8]], [0.8, [-6, 0, -6]], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [0.1, OUT, "step"], [1.0, Z0, "step"], [L, Z0, "step"]], "step");
  a.particle(0.3, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.12, 0.05, 0.12], velocity: [0, 0.03, 0] });
  return a;
}

export function slip(b) {
  const L = 2.4;
  const a = b.anim("slip", L, false);
  a.rot("peng", [[0, Z0], [0.15, [-10, 0, 0]], [0.45, [78, 0, 0]], [1.5, [78, 0, 0]], [1.9, [20, 0, 0]], [2.2, Z0], [L, Z0]]);
  a.pos("peng", [[0, Z0], [0.25, [0, 3, 1]], [0.45, [0, 2.4, 3]], [1.5, [0, 2.4, 3]], [2.0, [0, 0.5, 1]], [L, Z0]]);
  a.rot("wing_r", [[0, Z0], [0.3, [0, 0, 110]], [0.6, [0, 0, 60]], [0.8, [0, 0, 100]], [1.0, [0, 0, 60]], [1.5, [0, 0, 40]], [L, Z0]]);
  a.rot("wing_l", [[0, Z0], [0.3, [0, 0, -110]], [0.6, [0, 0, -60]], [0.8, [0, 0, -100]], [1.0, [0, 0, -60]], [1.5, [0, 0, -40]], [L, Z0]]);
  a.rot("foot_r", [[0, Z0], [0.4, [-60, 0, 0]], [1.0, [-30, 0, 0]], [1.2, [-60, 0, 0]], [1.6, Z0], [L, Z0]]);
  a.rot("foot_l", [[0, Z0], [0.4, [-40, 0, 0]], [1.1, [-70, 0, 0]], [1.3, [-40, 0, 0]], [1.6, Z0], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.45, [-30, 0, 0]], [1.0, [-20, 0, 12]], [1.4, [-20, 0, -12]], [2.0, [10, 0, 0]], [L, Z0]]);
  a.pos("lids", lids([[0.5, 1.4]], L), "step");
  a.particle(0.45, { particle: "minecraft:snowflake", locator: "feet", count: 6, spread: [0.3, 0.05, 0.3], velocity: [0, 0.06, 0] });
  a.sound(0.45, "minecraft:entity.player.small_fall", { volume: 0.4, pitch: 1.5 });
  return a;
}

export function catchFish(b) {
  const L = 3.2;
  const a = b.anim("catch_fish", L, false);
  a.rot("body", [[0, Z0], [0.4, [-38, 0, 0]], [0.75, [-46, 0, 0]], [1.0, Z0], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.4, [30, 0, 0]], [0.75, [40, 0, 0]], [1.0, [-10, 0, 0]], [1.2, [0, 0, 14]], [1.4, [0, 0, -14]], [1.6, [0, 0, 14]], [1.8, Z0], [2.1, [-14, 0, 0]], [2.35, [28, 0, 0]], [2.6, [-8, 0, 0]], [L, Z0]]);
  a.rot("jaw", [[0, Z0], [0.6, [26, 0, 0]], [0.9, Z0], [2.1, Z0], [2.2, [26, 0, 0]], [2.6, [26, 0, 0]], [2.7, Z0], [L, Z0]]);
  a.pos("fish", [[0, Z0, "step"], [0.9, BEAK], [2.1, BEAK], [2.35, [0, 7, -6]], [2.6, [0, 0, -6.6]], [2.7, [0, -1.3, -5], "step"], [2.71, Z0, "step"], [L, Z0, "step"]]);
  a.rot("fish", [[0, Z0, "step"], [0.9, Z0], [1.2, [0, 0, 15]], [1.5, [0, 0, -15]], [2.1, Z0, "linear"], [2.35, [0, 0, 200], "linear"], [2.6, [0, 0, 270]], [2.7, [0, 0, 270], "step"], [2.71, Z0, "step"], [L, Z0, "step"]]);
  a.rot("wing_r", [[0, Z0], [2.75, Z0], [2.9, [0, 0, 60]], [3.05, [0, 0, 20]], [L, Z0]]);
  a.rot("wing_l", [[0, Z0], [2.75, Z0], [2.9, [0, 0, -60]], [3.05, [0, 0, -20]], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [2.7, OUT, "step"], [L, Z0, "step"]], "step");
  a.visible(0, "fish", false).visible(0.9, "fish", true).visible(2.7, "fish", false);
  a.particle(0.75, { particle: "minecraft:splash", locator: "beak", count: 8, spread: [0.15, 0.05, 0.15], velocity: [0, 0.1, 0] });
  a.particle(2.7, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.1, 0.05, 0.1], velocity: [0, 0.03, 0] });
  a.sound(0.75, "minecraft:entity.fish.swim", { volume: 0.3, pitch: 1.4 });
  a.sound(2.7, "minecraft:entity.generic.eat", { volume: 0.3, pitch: 1.6 });
  return a;
}

export function worried(b) {
  const L = 1.6;
  const a = b.anim("worried", L, true);
  a.rot("body", [[0, [-6, 0, 0]], [L, [-6, 0, 0]]]);
  a.rot("head", [[0, [6, 0, 10]], [0.8, [6, 0, -10]], [L, [6, 0, 10]]]);
  a.rot("wing_r", loopKeys(L, 0.4, [-30, -40, 10], [-30, -25, 20]));
  a.rot("wing_l", loopKeys(L, 0.4, [-30, 25, -20], [-30, 40, -10]));
  a.pos("foot_r", [[0, Z0], [0.4, [0, 0.8, 0]], [0.6, Z0], [L, Z0]]);
  a.pos("foot_l", [[0, Z0], [1.2, [0, 0.8, 0]], [1.4, Z0], [L, Z0]]);
  return a;
}

export function celebrate(b) {
  const L = 2.6;
  const a = b.anim("celebrate", L, false);
  a.pos("peng", [[0, Z0], [0.15, [0, -0.8, 0]], [0.45, [0, 4, 0]], [0.75, Z0], [0.9, [0, -0.8, 0]], [1.3, [0, 6, 0]], [1.75, Z0], [L, Z0]]);
  a.rot("peng", [[0, Z0, "linear"], [0.9, Z0, "linear"], [1.75, [0, 360, 0], "linear"], [1.76, Z0, "step"], [L, Z0]], "linear");
  a.scale("body", [[0, ONE], [0.15, [1.08, 0.9, 1.08]], [0.45, [0.95, 1.08, 0.95]], [0.75, [1.06, 0.92, 1.06]], [0.9, [1.08, 0.9, 1.08]], [1.3, [0.95, 1.08, 0.95]], [1.75, [1.06, 0.92, 1.06]], [2.0, ONE], [L, ONE]]);
  const up = (sx) => [[0, Z0], [0.4, [0, 0, 150 * sx]], [0.6, [0, 0, 120 * sx]], [0.8, [0, 0, 150 * sx]], [1.2, [0, 0, 160 * sx]], [1.8, [0, 0, 140 * sx]], [2.0, [0, 0, 160 * sx]], [2.2, [0, 0, 120 * sx]], [L, Z0]];
  a.rot("wing_r", up(1));
  a.rot("wing_l", up(-1));
  a.rot("jaw", [[0, Z0], [0.4, [26, 0, 0]], [2.2, [26, 0, 0]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.4, [-16, 0, 0]], [2.2, [-16, 0, 0]], [L, Z0]]);
  a.pos("happy", [[0, OUT, "step"], [2.4, Z0, "step"], [L, Z0, "step"]], "step");
  for (const t of [0.45, 1.3, 1.75]) a.particle(t, { particle: "minecraft:happy_villager", locator: "heart", count: 6, spread: [0.4, 0.2, 0.4], velocity: [0, 0.05, 0] });
  a.particle(1.3, { particle: "minecraft:totem_of_undying", locator: "heart", count: 14, spread: [0.3, 0.2, 0.3], velocity: [0, 0.2, 0] });
  return a;
}

export function preen(b) {
  const L = 2.6;
  const a = b.anim("preen", L, false);
  a.rot("wing_l", [[0, Z0], [0.4, [0, -10, -55]], [2.2, [0, -10, -55]], [L, Z0]]);
  const pecks = [[0, Z0], [0.5, [24, -60, -10]]];
  for (let i = 0; i < 6; i++) pecks.push([0.7 + i * 0.22, [i % 2 ? 30 : 18, -66, -10]]);
  pecks.push([2.2, [24, -60, -10]], [L, Z0]);
  a.rot("head", pecks);
  a.rot("body", [[0, Z0], [0.5, [0, 0, 6]], [2.2, [0, 0, 6]], [L, Z0]]);
  a.pos("lids", lids([[0.6, 2.1]], L), "step");
  a.particle(1.4, { particle: "minecraft:white_ash", locator: "heart", count: 3, spread: [0.15, 0.1, 0.15], velocity: [0, -0.01, 0] });
  return a;
}

export function dance(b) {
  const L = 1.6;
  const a = b.anim("dance", L, true);
  a.rot("peng", loopKeys(L, 0.8, [0, 20, 0], [0, -20, 0]));
  a.rot("body", loopKeys(L, 0.4, [0, 0, 10], [0, 0, -10]));
  a.pos("peng", loopKeys(L, 0.4, Z0, [0, 1.4, 0]));
  a.rot("wing_r", [[0, [0, 0, 130]], [0.4, [0, 0, 20]], [0.8, [0, 0, 20]], [1.2, [0, 0, 130]], [L, [0, 0, 130]]]);
  a.rot("wing_l", [[0, [0, 0, -20]], [0.4, [0, 0, -130]], [0.8, [0, 0, -130]], [1.2, [0, 0, -20]], [L, [0, 0, -20]]]);
  a.rot("head", loopKeys(L, 0.4, [0, 0, -12], [0, 0, 12]));
  a.pos("foot_r", loopKeys(L, 0.8, Z0, [0, 1.4, -0.4]));
  a.pos("foot_l", loopKeys(L, 0.8, [0, 1.4, -0.4], Z0));
  a.pos("happy", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  a.particle(0.2, { particle: "minecraft:note", locator: "heart", count: 1, spread: [0.3, 0.1, 0.3] });
  return a;
}

export function land(b) {
  const L = 0.5;
  const a = b.anim("land", L, false);
  a.scale("body", [[0, ONE], [0.08, [1.12, 0.84, 1.12]], [0.25, [0.96, 1.05, 0.96]], [L, ONE]]);
  a.rot("wing_r", [[0, Z0], [0.1, [0, 0, 50]], [L, Z0]]);
  a.rot("wing_l", [[0, Z0], [0.1, [0, 0, -50]], [L, Z0]]);
  return a;
}
