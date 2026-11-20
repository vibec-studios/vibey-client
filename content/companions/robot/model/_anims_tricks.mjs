import { Z0, OUT, ONE, osc } from "./_anims_base.mjs";

export function spin(b) {
  const L = 1.3;
  const a = b.anim("spin", L, false);
  a.rot("bot", [[0, Z0, "linear"], [0.15, Z0, "linear"], [1.1, [0, 360, 0], "linear"], [1.11, Z0, "step"], [L, Z0]], "linear");
  a.pos("bot", [[0, Z0], [0.15, [0, -0.5, 0]], [0.6, [0, 4, 0]], [1.1, Z0], [L, Z0]]);
  a.rot("arm_r", [[0, Z0], [0.2, [0, 0, 80]], [1.0, [0, 0, 80]], [L, Z0]]);
  a.rot("arm_l", [[0, Z0], [0.2, [0, 0, -80]], [1.0, [0, 0, -80]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [0.6, [0, 0, -30]], [1.1, [0, 0, 20]], [L, Z0]]);
  return a;
}

export function jumpBoost(b) {
  const L = 2.2;
  const a = b.anim("jump_boost", L, false);
  a.pos("bot", [[0, Z0], [0.35, [0, -1.5, 0]], [0.5, Z0], [1.0, [0, 22, 0]], [1.4, [0, 20, 0]], [1.9, Z0], [2.0, [0, -1, 0]], [L, Z0]]);
  a.scale("body", [[0, ONE], [0.35, [1.08, 0.86, 1.08]], [0.5, [0.94, 1.08, 0.94]], [1.8, ONE], [2.0, [1.08, 0.9, 1.08]], [L, ONE]]);
  a.rot("leg_r", [[0, Z0], [0.5, Z0], [0.7, [20, 0, 6]], [1.8, [20, 0, 6]], [1.9, Z0], [L, Z0]]);
  a.rot("leg_l", [[0, Z0], [0.5, Z0], [0.7, [20, 0, -6]], [1.8, [20, 0, -6]], [1.9, Z0], [L, Z0]]);
  a.rot("arm_r", [[0, Z0], [0.35, [-30, 0, 10]], [0.6, [0, 0, 160]], [1.5, [0, 0, 160]], [1.9, [0, 0, 30]], [L, Z0]]);
  a.rot("arm_l", [[0, Z0], [0.35, [-30, 0, -10]], [0.6, [0, 0, -40]], [1.5, [0, 0, -40]], [1.9, [0, 0, -30]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [0.6, [-40, 0, 0]], [1.0, [10, 0, 0]], [1.9, [-30, 0, 0]], [2.05, [14, 0, 0]], [L, Z0]]);
  a.particle(0.5, { particle: "minecraft:poof", locator: "feet", count: 6, spread: [0.25, 0.02, 0.25], velocity: [0, 0.02, 0] });
  for (let t = 0.5; t < 1.5; t += 0.12) a.particle(t, { particle: "minecraft:flame", locator: "jet", count: 2, spread: [0.04, 0.02, 0.04], velocity: [0, -0.1, 0] });
  a.particle(1.95, { particle: "minecraft:poof", locator: "feet", count: 5, spread: [0.25, 0.02, 0.25], velocity: [0, 0.02, 0] });
  return a;
}

export function salute(b) {
  const L = 1.8;
  const a = b.anim("salute", L, false);
  a.rot("arm_r", [[0, Z0], [0.3, [48, -30, 138]], [1.4, [48, -30, 138]], [1.5, [52, -30, 134]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.3, [6, 0, 0]], [1.4, [6, 0, 0]], [L, Z0]]);
  a.rot("body", [[0, Z0], [0.25, [3, 0, 0]], [1.4, [3, 0, 0]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [0.3, [-18, 0, 0]], [0.45, [10, 0, 0]], [0.6, Z0], [L, Z0]]);
  a.sound(0.3, "minecraft:block.iron_trapdoor.close", { volume: 0.18, pitch: 2 });
  return a;
}

export function blinkPattern(b) {
  const L = 2.4;
  const a = b.anim("blink_pattern", L, false);
  const k = [[0, Z0, "step"]];
  for (const [t0, t1] of [[0.2, 0.3], [0.4, 0.5], [0.6, 0.9], [1.1, 1.4], [1.6, 1.7], [1.8, 1.9]]) k.push([t0, OUT, "step"], [t1, Z0, "step"]);
  k.push([L, Z0, "step"]);
  a.pos("lids", k, "step");
  const pulse = [[0, ONE]];
  for (let i = 0; i < 6; i++) pulse.push([0.2 + i * 0.3, [1.45, 1.45, 1.45]], [0.35 + i * 0.3, ONE]);
  pulse.push([L, ONE]);
  for (const n of ["bulb", "bulb_happy", "bulb_low", "bulb_alert"]) a.scale(n, pulse);
  a.rot("head", [[0, Z0], [0.3, [0, 0, 8]], [1.1, [0, 0, -8]], [1.9, [0, 0, 8]], [L, Z0]]);
  for (const t of [0.2, 0.8, 1.4, 2.0]) a.particle(t, { particle: "minecraft:end_rod", locator: "antenna", count: 1, spread: [0.02, 0.02, 0.02], velocity: [0, 0.05, 0] });
  return a;
}

export function fold(b) {
  const L = 3.4;
  const a = b.anim("fold", L, false);
  const hold = (v) => [[0, Z0], [0.8, v], [2.4, v], [3.1, Z0], [L, Z0]];
  a.pos("body", hold([0, -5, 0]));
  a.pos("head", hold([0, -2, 0]));
  a.pos("antenna", hold([0, -4.5, 0]));
  a.pos("leg_r", hold([0, 4, 0]));
  a.pos("leg_l", hold([0, 4, 0]));
  a.pos("arm_r", hold([-1.2, 1, 0]));
  a.pos("arm_l", hold([1.2, 1, 0]));
  a.rot("arm_r", hold([0, 0, 6]));
  a.rot("arm_l", hold([0, 0, -6]));
  a.rot("bot", [[0, Z0], [0.8, Z0], [1.3, [0, 0, -8]], [1.5, [0, 0, 8]], [1.7, Z0], [2.4, Z0], [L, Z0]]);
  a.pos("bot", [[0, Z0], [1.6, Z0], [1.8, [0, 2, 0]], [2.0, Z0], [L, Z0]]);
  a.pos("lids", [[0, Z0, "step"], [0.7, OUT, "step"], [2.5, Z0, "step"], [L, Z0, "step"]], "step");
  a.particle(3.1, { particle: "minecraft:electric_spark", locator: "antenna", count: 5, spread: [0.2, 0.1, 0.2], velocity: [0, 0.04, 0] });
  return a;
}

export function dance(b) {
  const L = 1.6;
  const a = b.anim("dance", L, true);
  const poses = [[[90, 0, 0], [0, 0, -90]], [[0, 0, 90], [90, 0, 0]], [[0, 0, 150], [0, 0, -40]], [[0, 0, 40], [0, 0, -150]]];
  const r = [], l = [];
  poses.forEach(([pr, pl], i) => { r.push([i * 0.4, pr]); l.push([i * 0.4, pl]); });
  r.push([L, poses[0][0]]);
  l.push([L, poses[0][1]]);
  a.rot("arm_r", r, "step");
  a.rot("arm_l", l, "step");
  a.rot("head", [[0, [0, 20, 0]], [0.4, [0, -20, 0]], [0.8, [0, 20, 6]], [1.2, [0, -20, -6]], [L, [0, 20, 0]]], "step");
  const bounce = [];
  for (let i = 0; i <= 8; i++) bounce.push([i * 0.2, i % 2 ? [0, 1.2, 0] : Z0]);
  a.pos("bot", bounce);
  a.rot("body", [[0, [0, 10, 0]], [0.8, [0, -10, 0]], [L, [0, 10, 0]]]);
  a.rot("leg_r", [[0, [-14, 0, 0]], [0.4, Z0], [0.8, [-14, 0, 0]], [1.2, Z0], [L, [-14, 0, 0]]]);
  a.rot("leg_l", [[0, Z0], [0.4, [-14, 0, 0]], [0.8, Z0], [1.2, [-14, 0, 0]], [L, Z0]]);
  a.rot("antenna", osc(L, 8, 16, -16));
  a.particle(0.4, { particle: "minecraft:note", locator: "heart", count: 1, spread: [0.2, 0.1, 0.2] });
  a.particle(1.2, { particle: "minecraft:note", locator: "heart", count: 1, spread: [0.2, 0.1, 0.2] });
  return a;
}
