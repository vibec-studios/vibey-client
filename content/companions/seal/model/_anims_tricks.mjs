import { Z0, OUT, M, hideZs } from "./_anims_base.mjs";

export function slide(b) {
  const L = 0.6;
  const a = hideZs(b.anim("slide", L, true));
  a.pos("seal", [[0, [0, -0.6, 0]], [L, [0, -0.6, 0]]]);
  a.rot("body", [[0, [-6, 0, -3]], [0.3, [-6, 0, 3]], [L, [-6, 0, -3]]]);
  a.scale("body", [[0, [0.97, 0.9, 1.1]], [L, [0.97, 0.9, 1.1]]]);
  a.rot("head", [[0, [16, 0, 0]], [L, [16, 0, 0]]]);
  a.rot("fin_r", [[0, [0, -70, 4]], [L, [0, -70, 4]]]);
  a.rot("fin_l", [[0, [0, 70, -4]], [L, [0, 70, -4]]]);
  a.rot("tail", [[0, [-18, 0, 0]], [L, [-18, 0, 0]]]);
  a.pos("happy", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  a.particle(0.0, { particle: "minecraft:snowflake", locator: "belly", count: 3, spread: [0.25, 0.02, 0.2], velocity: [0, 0.04, 0.06] });
  a.particle(0.3, { particle: "minecraft:snowflake", locator: "belly", count: 3, spread: [0.25, 0.02, 0.2], velocity: [0, 0.04, 0.06] });
  return a;
}

export function trickBall(b) {
  const L = 3.6;
  const a = hideZs(b.anim("trick_ball", L, false));
  a.rot("body", [[0, Z0], [0.3, [28, 0, 0]], [3.3, [28, 0, 0]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.3, [10, 0, 0]], [3.3, [10, 0, 0]], [L, Z0]]);
  const bounce = [[0, Z0], [0.3, Z0]];
  for (let i = 0; i < 3; i++) bounce.push([0.3 + i * 0.5 + 0.25, [0, 5, 0]], [0.3 + i * 0.5 + 0.5, Z0]);
  bounce.push([2.2, [0, 16, 0]], [2.7, [0, 16, 0], "catmullrom"], [3.1, Z0], [L, Z0]);
  a.pos("ball", bounce);
  a.rot("ball", [[0, Z0], [1.8, [0, 360, 0], "linear"], [3.1, [360, 720, 0], "linear"], [3.11, Z0, "step"], [L, Z0]], "linear");
  const OPEN = [0, 140, 12], SHUT = [0, 176, 12];
  const fins = [[0, Z0], [0.3, [0, 40, 6]], [3.1, [0, 40, 6]], [3.2, OPEN], [3.35, SHUT], [3.5, OPEN], [L, Z0]];
  a.rot("fin_r", fins.map(([t, v]) => [t, M(v, 1)]));
  a.rot("fin_l", fins.map(([t, v]) => [t, M(v, -1)]));
  a.pos("happy", [[0, Z0, "step"], [3.1, OUT, "step"], [L, Z0, "step"]], "step");
  for (let i = 0; i < 3; i++) a.sound(0.8 + i * 0.5, "squeak", { volume: 0.12, pitch: 1.5 });
  a.particle(3.2, { particle: "minecraft:note", locator: "heart", count: 2, spread: [0.2, 0.1, 0.2], velocity: [0, 0.04, 0] });
  return a;
}

export function roll(b) {
  const L = 1.8;
  const a = hideZs(b.anim("roll", L, false));
  a.rot("roll", [[0, Z0, "linear"], [0.3, Z0, "linear"], [0.7, [0, 0, 120], "linear"], [1.1, [0, 0, 240], "linear"], [1.5, [0, 0, 360], "step"], [1.51, Z0, "linear"], [L, Z0, "linear"]], "linear");
  a.pos("seal", [[0, Z0], [0.25, Z0], [0.45, [0, 3.2, 0]], [1.35, [0, 3.2, 0]], [1.6, [0, -0.2, 0]], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [0.3, OUT, "step"], [1.7, Z0, "step"], [L, Z0, "step"]], "step");
  a.particle(1.55, { particle: "minecraft:snowflake", locator: "belly", count: 6, spread: [0.25, 0.04, 0.25], velocity: [0, 0.03, 0] });
  return a;
}

export function beg(b) {
  const L = 2;
  const a = hideZs(b.anim("beg", L, true));
  a.rot("body", [[0, [30, 0, 0]], [L, [30, 0, 0]]]);
  a.rot("head", [[0, [-6, 0, 12]], [1, [-6, 0, -12]], [L, [-6, 0, 12]]]);
  const pump = (sx) => [[0, M([0, 150, 20], sx)], [0.25, M([0, 160, 30], sx)], [0.5, M([0, 150, 20], sx)], [0.75, M([0, 160, 30], sx)], [1, M([0, 150, 20], sx)], [1.25, M([0, 160, 30], sx)], [1.5, M([0, 150, 20], sx)], [1.75, M([0, 160, 30], sx)], [L, M([0, 150, 20], sx)]];
  a.rot("fin_r", pump(1));
  a.rot("fin_l", pump(-1));
  a.rot("tail", [[0, [-8, 10, 0]], [0.5, [-8, -10, 0]], [1, [-8, 10, 0]], [1.5, [-8, -10, 0]], [L, [-8, 10, 0]]]);
  a.pos("happy", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  return a;
}
