import { Z0, OUT, ONE, osc } from "./_anims_base.mjs";

export function beep(b) {
  const L = 0.6;
  const a = b.anim("beep", L, false);
  a.rot("head", [[0, Z0], [0.1, [10, 0, 0]], [0.3, [-4, 0, 0]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [0.1, [-22, 0, 0]], [0.25, [14, 0, 0]], [0.4, [-6, 0, 0]], [L, Z0]]);
  a.scale("body", [[0, ONE], [0.1, [1.04, 0.95, 1.04]], [0.25, [0.98, 1.03, 0.98]], [L, ONE]]);
  return a;
}

export function wave(b) {
  const L = 1.6;
  const a = b.anim("wave", L, false);
  a.rot("arm_r", [[0, Z0], [0.25, [0, 0, 150]], [0.5, [0, 15, 122]], [0.75, [0, -10, 150]], [1.0, [0, 15, 122]], [1.25, [0, 0, 150]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.3, [4, 0, -10]], [1.25, [4, 0, -10]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [0.3, [0, 0, 12]], [0.6, [0, 0, -8]], [0.9, [0, 0, 6]], [L, Z0]]);
  return a;
}

export function joy(b) {
  const L = 1.2;
  const a = b.anim("joy", L, false);
  a.pos("bot", [[0, Z0], [0.15, [0, 3, 0]], [0.3, Z0], [0.6, Z0], [0.75, [0, 3, 0]], [0.9, Z0], [L, Z0]]);
  a.scale("body", [[0, [1.05, 0.92, 1.05]], [0.15, [0.96, 1.05, 0.96]], [0.3, [1.05, 0.92, 1.05]], [0.6, [1.05, 0.92, 1.05]], [0.75, [0.96, 1.05, 0.96]], [0.9, [1.05, 0.92, 1.05]], [L, ONE]]);
  a.rot("arm_r", [[0, [0, 0, 140]], [0.3, [0, 0, 120]], [0.6, [0, 0, 145]], [0.9, [0, 0, 120]], [L, Z0]]);
  a.rot("arm_l", [[0, [0, 0, -140]], [0.3, [0, 0, -120]], [0.6, [0, 0, -145]], [0.9, [0, 0, -120]], [L, Z0]]);
  a.rot("antenna", osc(L, 6, 16, -16));
  a.rot("head", [[0, [6, 0, 8]], [0.6, [6, 0, -8]], [L, Z0]]);
  return a;
}

export function scan(b) {
  const L = 2.2;
  const a = b.anim("scan", L, false);
  a.scale("beam", [[0, [1, 1, 0.02]], [0.3, ONE], [1.9, ONE], [L, [1, 1, 0.02]]]);
  a.rot("beam", [[0, Z0], [0.6, [0, 2, 0]], [1.0, [0, -2, 0]], [1.4, [1, 2, 0]], [1.8, [0, -1, 0]], [L, Z0]]);
  a.rot("antenna", [[0, Z0, "linear"], [1.1, [0, 360, 0], "linear"], [L, [0, 720, 0], "linear"]], "linear");
  a.rot("arm_r", [[0, Z0], [0.3, [40, 0, 20]], [1.9, [40, 0, 20]], [L, Z0]]);
  a.scale("body", [[0, ONE], [0.15, [1.03, 0.97, 1.03]], [0.3, ONE], [L, ONE]]);
  for (const t of [0.4, 0.9, 1.4]) a.particle(t, { particle: "minecraft:end_rod", locator: "eye", count: 1, spread: [0.05, 0.05, 0.05], velocity: [0, 0.01, -0.06] });
  return a;
}

export function umbrellaPop(b) {
  const L = 0.9;
  const a = b.anim("umbrella_pop", L, false);
  a.pos("umbrella", [[0, [0, -8, 0]], [0.4, [0, 0.6, 0]], [0.5, Z0], [L, Z0]]);
  a.scale("umbrella_top", [[0, [0.12, 1, 0.12]], [0.35, [0.15, 1, 0.15]], [0.65, [1.12, 1, 1.12]], [L, ONE]]);
  a.rot("umbrella_top", [[0, Z0], [0.65, [0, 90, 0]], [L, [0, 90, 0]]]);
  a.rot("antenna", [[0, Z0], [0.3, [-20, 0, 0]], [0.6, [8, 0, 0]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.4, [16, 0, 0]], [L, [6, 0, 0]]]);
  return a;
}

export function umbrellaHold(b) {
  const L = 3;
  const a = b.anim("umbrella_hold", L, true);
  a.rot("umbrella_top", [[0, [0, 0, -3]], [1.5, [2, 6, 3]], [L, [0, 0, -3]]]);
  a.particle(0.7, { particle: "minecraft:dripping_water", locator: "umbrella", count: 2, spread: [0.3, 0.02, 0.3] });
  a.particle(2.1, { particle: "minecraft:dripping_water", locator: "umbrella", count: 2, spread: [0.3, 0.02, 0.3] });
  return a;
}

export function shake(b) {
  const L = 1.0;
  const a = b.anim("shake", L, false);
  const k = [[0, Z0]];
  for (let i = 1; i <= 7; i++) k.push([i * 0.12, [0, (i % 2 ? 1 : -1) * 26 * (1 - i / 9), 0]]);
  k.push([L, Z0]);
  a.rot("body", k);
  a.rot("head", k.map(([t, v]) => [t, [0, -v[1] * 0.7, 0]]));
  a.rot("antenna", k.map(([t, v]) => [t, [0, 0, v[1]]]));
  for (const t of [0.2, 0.5]) a.particle(t, { particle: "minecraft:splash", locator: "chest", count: 8, spread: [0.3, 0.2, 0.3], velocity: [0, 0.1, 0] });
  return a;
}

export function glitch(b) {
  const L = 0.8;
  const a = b.anim("glitch", L, true);
  a.rot("head", [[0, [0, 0, 6]], [0.1, [0, 8, -4]], [0.2, [4, 0, 2]], [0.35, [0, -6, -6]], [0.5, [-3, 4, 4]], [0.65, [0, 0, -3]], [L, [0, 0, 6]]], "step");
  a.pos("body", [[0, [0.4, 0, 0]], [0.15, [-0.4, 0, 0]], [0.3, [0.2, 0.3, 0]], [0.45, [-0.3, 0, 0]], [0.6, [0.3, 0, 0]], [L, [0.4, 0, 0]]], "step");
  a.rot("antenna", osc(L, 8, 24, -24));
  a.rot("arm_r", [[0, [0, 0, 30]], [0.2, [0, 0, 10]], [0.4, [0, 0, 35]], [L, [0, 0, 30]]], "step");
  a.pos("lids", [[0, Z0, "step"], [0.1, OUT, "step"], [0.18, Z0, "step"], [0.5, OUT, "step"], [0.55, Z0, "step"], [L, Z0, "step"]], "step");
  for (const t of [0.05, 0.45]) a.particle(t, { particle: "minecraft:electric_spark", locator: "antenna", count: 4, spread: [0.12, 0.12, 0.12], velocity: [0, 0.05, 0] });
  return a;
}

export function solarOpen(b) {
  const L = 1.6;
  const a = b.anim("solar_open", L, false);
  a.rot("solar", [[0, Z0], [0.7, [-104, 0, 0]], [0.8, [-96, 0, 0]], [L, [-98, 0, 0]]]);
  a.rot("solar_r", [[0, Z0], [0.75, Z0], [1.3, [0, 180, 0]], [L, [0, 180, 0]]]);
  a.rot("solar_l", [[0, Z0], [0.75, Z0], [1.3, [0, -180, 0]], [L, [0, -180, 0]]]);
  a.rot("head", [[0, Z0], [0.6, [18, 0, 0]], [L, [14, 0, 4]]]);
  a.sound(0.1, "minecraft:block.iron_trapdoor.open", { volume: 0.25, pitch: 1.8 });
  a.sound(0.8, "minecraft:block.lever.click", { volume: 0.25, pitch: 1.6 });
  return a;
}

export function shiver(b) {
  const L = 0.4;
  const a = b.anim("shiver", L, true);
  a.rot("body", osc(L, 8, 3, -3), "linear");
  a.rot("arm_r", [[0, [20, -20, -8]], [L, [20, -20, -8]]]);
  a.rot("arm_l", [[0, [20, 20, 8]], [L, [20, 20, 8]]]);
  a.rot("head", [[0, [-6, 0, 0]], [L, [-6, 0, 0]]]);
  a.rot("antenna", osc(L, 4, 6, -6, 0));
  return a;
}

export function fret(b) {
  const L = 1.6;
  const a = b.anim("fret", L, true);
  a.rot("arm_r", [[0, [45, -20, -18]], [0.8, [50, -25, -22]], [L, [45, -20, -18]]]);
  a.rot("arm_l", [[0, [45, 20, 18]], [0.8, [50, 25, 22]], [L, [45, 20, 18]]]);
  a.rot("head", [[0, [6, 0, 10]], [0.8, [6, 0, -10]], [L, [6, 0, 10]]]);
  a.rot("antenna", [[0, [-26, 0, 0]], [0.8, [-30, 0, 4]], [L, [-26, 0, 0]]]);
  return a;
}

export function tilt(b) {
  const L = 1.4;
  const a = b.anim("tilt", L, false);
  a.rot("head", [[0, Z0], [0.25, [6, 0, 20]], [1.1, [6, 0, 20]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [0.25, [0, 0, -18]], [0.45, [0, 0, 10]], [0.65, [0, 0, -4]], [L, Z0]]);
  return a;
}
