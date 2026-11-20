import { Z0, ONE, osc } from "./_anims_base.mjs";

export function polish(b) {
  const L = 2.4;
  const a = b.anim("polish", L, false);
  const rub = [[0, Z0], [0.35, [10, 0, -150]]];
  for (let i = 0; i < 6; i++) rub.push([0.5 + i * 0.25, [i % 2 ? 22 : -4, 0, -150]]);
  rub.push([2.1, [10, 0, -150]], [L, Z0]);
  a.rot("arm_l", rub);
  a.rot("head", [[0, Z0], [0.35, [0, 0, -12]], [2.1, [0, 0, -12]], [L, Z0]]);
  for (const t of [0.7, 1.2, 1.7]) a.particle(t, { particle: "minecraft:wax_off", locator: "heart", count: 2, spread: [0.2, 0.1, 0.2], velocity: [0, 0.02, 0] });
  a.particle(2.15, { particle: "minecraft:happy_villager", locator: "heart", count: 3, spread: [0.25, 0.1, 0.25] });
  return a;
}

export function gauge(b) {
  const L = 2.0;
  const a = b.anim("gauge", L, false);
  a.rot("head", [[0, Z0], [0.3, [-28, 0, 0]], [1.6, [-28, 0, 6]], [L, Z0]]);
  a.rot("arm_r", [[0, Z0], [0.4, [62, 0, -22]], [0.55, [52, 0, -22]], [0.7, [62, 0, -22]], [0.85, [52, 0, -22]], [1.6, [52, 0, -22]], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [1.6, Z0], [1.75, [0, 0, 14]], [L, Z0]]);
  a.sound(0.55, "minecraft:block.lever.click", { volume: 0.12, pitch: 2 });
  a.sound(0.85, "minecraft:block.lever.click", { volume: 0.12, pitch: 2 });
  return a;
}

export function vent(b) {
  const L = 1.6;
  const a = b.anim("vent", L, true);
  const k = [];
  for (let i = 0; i <= 8; i++) k.push([i * 0.2, [80, 0, i % 2 ? -40 : -10]]);
  a.rot("arm_r", k);
  a.rot("head", [[0, [10, 0, 0]], [0.8, [10, 0, 4]], [L, [10, 0, 0]]]);
  a.rot("antenna", [[0, [-20, 0, 0]], [L, [-20, 0, 0]]]);
  a.particle(0.3, { particle: "minecraft:white_smoke", locator: "antenna", count: 2, spread: [0.1, 0.05, 0.1], velocity: [0, 0.04, 0] });
  a.particle(1.1, { particle: "minecraft:white_smoke", locator: "antenna", count: 2, spread: [0.1, 0.05, 0.1], velocity: [0, 0.04, 0] });
  return a;
}

export function carryTorch(b) {
  const L = 1.2;
  const a = b.anim("carry_torch", L, true);
  a.rot("arm_r", [[0, [38, 0, 16]], [0.6, [42, 0, 18]], [L, [38, 0, 16]]]);
  a.rot("torch", [[0, [-38, 0, -16]], [0.6, [-42, 0, -18]], [L, [-38, 0, -16]]]);
  a.scale("torch_flame", [[0, ONE], [0.3, [1.1, 1.25, 1.1]], [0.6, [0.95, 0.9, 0.95]], [0.9, [1.1, 1.2, 1.1]], [L, ONE]]);
  a.particle(0.2, { particle: "minecraft:small_flame", locator: "flame", count: 1, spread: [0.03, 0.02, 0.03], velocity: [0, 0.015, 0] });
  a.particle(0.8, { particle: "minecraft:smoke", locator: "flame", count: 1, spread: [0.03, 0.02, 0.03], velocity: [0, 0.02, 0] });
  return a;
}

export function flagWave(b) {
  const L = 1.0;
  const a = b.anim("flag_wave", L, true);
  a.rot("arm_l", [[0, [0, 0, -60]], [0.5, [0, 0, -95]], [L, [0, 0, -60]]]);
  a.rot("flag", [[0, [0, 0, 55]], [0.5, [0, 0, 80]], [L, [0, 0, 55]]]);
  a.rot("flag_cloth", osc(L, 4, 28, -22, 1));
  a.rot("arm_r", [[0, [0, 0, 120]], [0.5, [0, 0, 140]], [L, [0, 0, 120]]]);
  a.pos("bot", [[0, Z0], [0.25, [0, 1.6, 0]], [0.5, Z0], [0.75, [0, 1.6, 0]], [L, Z0]]);
  a.rot("antenna", osc(L, 4, 14, -14));
  return a;
}
