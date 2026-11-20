import { Z0, OUT, ONE, lids, loopKeys } from "./_anims_base.mjs";

const CLOSED = [0.14, 1.9, 0.14];
const HOLD_R = [-8, 0, 34];

export function umbrellaOpen(b) {
  const L = 1.3;
  const a = b.anim("umbrella_open", L, false);
  a.rot("wing_r", [[0, Z0], [0.3, HOLD_R], [L, HOLD_R]]);
  a.scale("canopy", [[0, CLOSED], [0.45, CLOSED], [0.85, [1.12, 0.85, 1.12]], [1.05, [0.96, 1.04, 0.96]], [L, ONE]]);
  a.rot("umbrella", [[0, [0, 0, -10]], [0.45, [0, 0, -6]], [0.85, [0, 0, 4]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.6, [-18, 0, 0]], [1.0, [-18, 0, 4]], [L, [-8, 0, 0]]]);
  a.pos("happy", [[0, Z0, "step"], [0.85, OUT, "step"], [L, OUT, "step"]], "step");
  a.sound(0.8, "minecraft:entity.chicken.egg", { volume: 0.4, pitch: 1.5 });
  return a;
}

export function umbrellaTwirl(b) {
  const L = 2;
  const a = b.anim("umbrella_twirl", L, false);
  a.rot("canopy", [[0, Z0, "linear"], [1.6, [0, 360, 0], "linear"], [1.61, Z0, "step"], [L, Z0]], "linear");
  a.rot("body", [[0, Z0], [0.5, [0, 0, -4]], [1.0, [0, 0, 4]], [1.5, [0, 0, -4]], [L, Z0]]);
  a.pos("happy", [[0, OUT, "step"], [1.8, Z0, "step"], [L, Z0, "step"]], "step");
  for (const t of [0.4, 1.2]) a.particle(t, { particle: "minecraft:splash", locator: "umbrella", count: 6, spread: [0.5, 0.05, 0.5], velocity: [0, 0.08, 0] });
  return a;
}

export function umbrellaClose(b) {
  const L = 2.2;
  const a = b.anim("umbrella_close", L, false);
  a.rot("wing_r", [[0, HOLD_R], [1.9, HOLD_R], [L, Z0]]);
  a.scale("canopy", [[0, ONE], [0.45, CLOSED], [L, CLOSED]]);
  const shake = [[0, Z0], [0.5, Z0]];
  for (let i = 0; i < 8; i++) shake.push([0.6 + i * 0.12, [0, 0, (i % 2 ? 1 : -1) * 22]]);
  shake.push([1.6, Z0], [L, Z0]);
  a.rot("umbrella", shake);
  a.rot("head", [[0, Z0], [0.5, [6, 0, 8]], [1.6, [6, 0, -8]], [L, Z0]]);
  for (const t of [0.7, 0.95, 1.2, 1.45]) a.particle(t, { particle: "minecraft:splash", locator: "umbrella", count: 8, spread: [0.25, 0.3, 0.25], velocity: [0, 0.12, 0] });
  a.sound(0.6, "minecraft:entity.wolf.shake", { volume: 0.4, pitch: 1.6 });
  return a;
}

export function lookUp(b) {
  const L = 1.4;
  const a = b.anim("look_up", L, false);
  a.rot("head", [[0, Z0], [0.35, [-30, 0, 0]], [1.0, [-30, 6, 0]], [L, Z0]]);
  a.rot("jaw", [[0, Z0], [0.5, [16, 0, 0]], [0.9, Z0], [L, Z0]]);
  a.pos("lids", lids([[0.7, 0.82]], L), "step");
  return a;
}

export function shiver(b) {
  const L = 0.4;
  const a = b.anim("shiver", L, true);
  const k = [];
  for (let i = 0; i <= 8; i++) k.push([i * 0.05, [0, 0, i % 2 ? 3 : -3], "linear"]);
  a.rot("body", k, "linear");
  a.scale("body", [[0, [1.05, 0.95, 1.05]], [0.2, [1.06, 0.94, 1.06]], [L, [1.05, 0.95, 1.05]]]);
  a.rot("wing_r", [[0, [0, -40, -6]], [L, [0, -40, -6]]]);
  a.rot("wing_l", [[0, [0, 40, 6]], [L, [0, 40, 6]]]);
  a.pos("head", [[0, [0, -1.2, 0]], [L, [0, -1.2, 0]]]);
  a.pos("lids", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  return a;
}

export function fanning(b) {
  const L = 1.2;
  const a = b.anim("fanning", L, true);
  a.rot("wing_l", loopKeys(L, 0.3, [-125, 0, -18], [-125, 0, 14]));
  a.rot("head", [[0, [-10, 0, -6]], [0.6, [-10, 0, 6]], [L, [-10, 0, -6]]]);
  a.rot("body", [[0, [4, 0, 0]], [L, [4, 0, 0]]]);
  a.rot("wing_r", [[0, [0, 0, 20]], [L, [0, 0, 20]]]);
  a.rot("jaw", [[0, [10, 0, 0]], [0.6, [4, 0, 0]], [L, [10, 0, 0]]]);
  return a;
}

export function snowPlay(b) {
  const L = 3.4;
  const a = b.anim("snow_play", L, false);
  a.rot("body", [[0, Z0], [0.4, [-34, 0, 0]], [0.9, [-34, 0, 0]], [1.3, Z0], [1.8, [10, 0, 0]], [2.05, [-14, 0, 0]], [2.4, Z0], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.4, [20, 0, 0]], [0.9, [20, 0, 0]], [1.3, [-10, 0, 0]], [2.05, [-20, 0, 0]], [2.6, Z0], [L, Z0]]);
  a.rot("wing_r", [[0, Z0], [0.4, [-50, 0, 10]], [0.7, [-30, 0, 30]], [0.9, [-50, 0, 10]], [1.3, [-20, 0, 30]], [1.8, [-170, 0, 20]], [2.05, [-40, 0, 20]], [2.4, Z0], [L, Z0]]);
  a.rot("wing_l", [[0, Z0], [0.4, [-50, 0, -10]], [0.9, [-50, 0, -10]], [1.3, Z0], [2.6, Z0], [2.8, [0, 0, -60]], [3.0, [0, 0, -20]], [3.2, [0, 0, -60]], [L, Z0]]);
  a.pos("peng", [[0, Z0], [2.6, Z0], [2.75, [0, 2, 0]], [2.9, Z0], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [2.1, OUT, "step"], [L, Z0, "step"]], "step");
  a.visible(0, "snowball", false).visible(0.7, "snowball", true).visible(2.05, "snowball", false);
  for (const t of [0.5, 0.75]) a.particle(t, { particle: "minecraft:snowflake", locator: "feet", count: 4, spread: [0.25, 0.05, 0.25], velocity: [0, 0.05, 0] });
  a.particle(2.05, { particle: "minecraft:item_snowball", locator: "snowball", count: 6, spread: [0.1, 0.1, 0.1], velocity: [0, 0.1, -0.2] });
  a.sound(2.0, "minecraft:entity.snowball.throw", { volume: 0.5, pitch: 1.3 });
  return a;
}
