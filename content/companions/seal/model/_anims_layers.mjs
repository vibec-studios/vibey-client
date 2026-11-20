import { Z0, OUT, M } from "./_anims_base.mjs";

export function clap(b) {
  const L = 1.4;
  const a = b.anim("clap", L, false);
  a.rot("body", [[0, Z0], [0.25, [26, 0, 0]], [1.1, [26, 0, 0]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.25, [-10, 0, 0]], [1.1, [-6, 0, 6]], [L, Z0]]);
  const OPEN = [0, 140, 12], SHUT = [0, 176, 12];
  const keys = [[0, Z0], [0.3, OPEN], [0.45, SHUT], [0.6, OPEN], [0.75, SHUT], [0.9, OPEN], [1.05, SHUT], [1.2, [0, 60, 6]], [L, Z0]];
  a.rot("fin_r", keys.map(([t, v]) => [t, M(v, 1)]));
  a.rot("fin_l", keys.map(([t, v]) => [t, M(v, -1)]));
  a.rot("tail", [[0, Z0], [0.25, [-8, 0, 0]], [1.1, [-8, 0, 0]], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [0.25, OUT, "step"], [1.15, Z0, "step"], [L, Z0, "step"]], "step");
  for (const t of [0.45, 0.75, 1.05]) a.sound(t, "squeak", { volume: 0.16, pitch: 1.2 + t * 0.15 });
  a.particle(0.75, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.12, 0.05, 0.12], velocity: [0, 0.03, 0] });
  return a;
}

export function bark(b) {
  const L = 0.6;
  const a = b.anim("bark", L, false);
  a.rot("head", [[0, Z0], [0.12, [20, 0, 0]], [0.3, [14, 0, 0]], [L, Z0]]);
  a.scale("body", [[0, [1, 1, 1]], [0.12, [0.96, 1.06, 1]], [0.3, [1.03, 0.97, 1]], [L, [1, 1, 1]]]);
  a.rot("fin_r", [[0, Z0], [0.12, [0, 0, 14]], [L, Z0]]);
  a.rot("fin_l", [[0, Z0], [0.12, [0, 0, -14]], [L, Z0]]);
  return a;
}

export function shiver(b) {
  const L = 0.4;
  const a = b.anim("shiver", L, true);
  const k = [];
  for (let i = 0; i <= 8; i++) k.push([i * 0.05, [0, 0, i % 2 ? 3 : -3], "linear"]);
  a.rot("body", k, "linear");
  a.scale("body", [[0, [1.03, 0.96, 1]], [0.2, [1.04, 0.95, 1]], [L, [1.03, 0.96, 1]]]);
  a.rot("fin_r", [[0, [0, -30, 0]], [L, [0, -30, 0]]]);
  a.rot("fin_l", [[0, [0, 30, 0]], [L, [0, 30, 0]]]);
  a.rot("head", [[0, [-6, 0, 0]], [L, [-6, 0, 0]]]);
  return a;
}

export function shake(b) {
  const L = 1.1;
  const a = b.anim("shake", L, false);
  const k = [[0, Z0]];
  for (let i = 1; i <= 8; i++) k.push([i * 0.11, [0, (i % 2 ? 1 : -1) * 28 * (1 - i / 10), (i % 2 ? -1 : 1) * 8]]);
  k.push([L, Z0]);
  a.rot("body", k);
  a.rot("head", k.map(([t, v]) => [t, [v[0], -v[1] * 0.8, v[2]]]));
  a.rot("tail", k.map(([t, v]) => [t, [0, -v[1], 0]]));
  a.pos("happy", [[0, OUT, "step"], [1.0, Z0, "step"], [L, Z0, "step"]], "step");
  for (const t of [0.2, 0.45, 0.7]) a.particle(t, { particle: "minecraft:splash", locator: "belly", count: 8, spread: [0.35, 0.2, 0.35], velocity: [0, 0.12, 0] });
  return a;
}

export function lookUp(b) {
  const L = 1.6;
  const a = b.anim("look_up", L, false);
  a.rot("head", [[0, Z0], [0.4, [26, 0, 0]], [1.1, [26, 4, 0]], [L, Z0]]);
  a.pos("lids", [[0, Z0, "step"], [0.8, OUT, "step"], [0.92, Z0, "step"], [L, Z0, "step"]], "step");
  return a;
}

export function wave(b) {
  const L = 1.6;
  const a = b.anim("wave", L, false);
  a.rot("fin_r", [[0, Z0], [0.25, [0, 30, 70]], [0.5, [0, 30, 40]], [0.75, [0, 30, 70]], [1.0, [0, 30, 40]], [1.25, [0, 30, 70]], [L, Z0]]);
  a.rot("body", [[0, Z0], [0.25, [6, 0, -6]], [1.25, [6, 0, -6]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.3, [4, 0, -8]], [1.25, [4, 0, -8]], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [0.3, OUT, "step"], [1.3, Z0, "step"], [L, Z0, "step"]], "step");
  return a;
}
