export const Z0 = [0, 0, 0];
export const OUT = [0, 0, -0.3];
export const ONE = [1, 1, 1];
export const BEAK = [0, -1.3, -7.6];

export function lids(wins, len, bone = "lids") {
  const k = [[0, Z0, "step"]];
  for (const [a, c] of wins) k.push([a, OUT, "step"], [c, Z0, "step"]);
  k.push([len, Z0, "step"]);
  return k.sort((x, y) => x[0] - y[0]);
}

export const shut = (len) => [[0, OUT, "step"], [len, OUT, "step"]];

export function loopKeys(len, period, a, b) {
  const k = [];
  for (let t = 0, i = 0; t < len - 1e-6; t += period / 2, i++) k.push([+t.toFixed(4), i % 2 ? b : a]);
  k.push([len, a]);
  return k;
}

function breathe(a, len, period = 2, amount = 0.025) {
  return a.scale("body", loopKeys(len, period, ONE, [1 + amount * 0.6, 1 + amount, 1 + amount * 0.6]));
}

export function idle(b) {
  const L = 6;
  const a = b.anim("idle", L, true);
  breathe(a, L);
  a.rot("head", [[0, Z0], [1.0, [4, 10, 6]], [2.0, [4, 10, 6]], [2.5, Z0], [3.6, [-3, -12, -5]], [4.6, [-3, -12, -5]], [5.2, Z0], [L, Z0]]);
  a.rot("body", [[0, Z0], [2.8, Z0], [3.1, [0, 0, 3]], [3.4, Z0], [L, Z0]]);
  const flick = (sx, t) => [[0, Z0], [t, Z0], [t + 0.12, [0, 0, 26 * sx]], [t + 0.24, [0, 0, 6 * sx]], [t + 0.36, [0, 0, 24 * sx]], [t + 0.6, Z0], [L, Z0]];
  a.rot("wing_r", flick(1, 2.9));
  a.rot("wing_l", flick(-1, 2.95));
  a.pos("foot_l", [[0, Z0], [5.3, Z0], [5.45, [0, 0.8, -0.3]], [5.6, Z0], [L, Z0]]);
  a.rot("jaw", [[0, Z0], [3.0, Z0], [3.1, [14, 0, 0]], [3.3, Z0], [L, Z0]]);
  a.pos("lids", lids([[1.6, 1.72], [4.1, 4.22], [4.35, 4.47]], L), "step");
  return a;
}

export function sit(b) {
  const L = 3;
  const a = b.anim("sit", L, true);
  a.pos("peng", [[0, [0, -1.4, 0]], [L, [0, -1.4, 0]]]);
  a.rot("body", [[0, [8, 0, 0]], [1.5, [8, 0, 2]], [L, [8, 0, 0]]]);
  a.scale("body", loopKeys(L, 3, [1.05, 0.93, 1.05], [1.06, 0.95, 1.06]));
  a.rot("head", [[0, [-8, 0, 0]], [1.5, [-6, 0, 6]], [L, [-8, 0, 0]]]);
  a.rot("wing_r", [[0, [0, 0, 24]], [L, [0, 0, 24]]]);
  a.rot("wing_l", [[0, [0, 0, -24]], [L, [0, 0, -24]]]);
  for (const f of ["foot_r", "foot_l"]) {
    a.rot(f, [[0, [-70, 0, 0]], [1.2, [-62, 0, 0]], [L, [-70, 0, 0]]]);
    a.pos(f, [[0, [0, 1.6, -0.6]], [L, [0, 1.6, -0.6]]]);
  }
  a.pos("lids", lids([[2.1, 2.22]], L), "step");
  return a;
}

export function swim(b) {
  const L = 0.8;
  const a = b.anim("swim", L, true);
  a.pos("peng", [[0, [0, -7, 0]], [0.4, [0, -6.4, 0]], [L, [0, -7, 0]]]);
  a.rot("body", [[0, [-86, 0, -4]], [0.4, [-82, 0, 4]], [L, [-86, 0, -4]]]);
  a.pos("body", [[0, [0, 4.2, -1]], [L, [0, 4.2, -1]]]);
  a.rot("head", [[0, [64, 0, 0]], [0.4, [58, 0, 0]], [L, [64, 0, 0]]]);
  a.rot("wing_r", loopKeys(L, 0.4, [-20, 0, 85], [-20, 0, 20]));
  a.rot("wing_l", loopKeys(L, 0.4, [-20, 0, -85], [-20, 0, -20]));
  for (const f of ["foot_r", "foot_l"]) a.pos(f, [[0, [0, 1.8, 3.5]], [L, [0, 1.8, 3.5]]]);
  a.rot("foot_r", loopKeys(L, 0.4, [95, 0, 0], [70, 0, 0]));
  a.rot("foot_l", loopKeys(L, 0.4, [70, 0, 0], [95, 0, 0]));
  a.particle(0.1, { particle: "minecraft:bubble", locator: "beak", count: 2, spread: [0.1, 0.05, 0.1], velocity: [0, 0.05, 0.02] });
  a.particle(0.5, { particle: "minecraft:bubble", locator: "feet", count: 2, spread: [0.15, 0.05, 0.15], velocity: [0, 0.04, 0.03] });
  return a;
}

export function swimIdle(b) {
  const L = 2.4;
  const a = b.anim("swim_idle", L, true);
  a.pos("peng", [[0, [0, -6, 0]], [1.2, [0, -5.3, 0]], [L, [0, -6, 0]]]);
  a.rot("body", [[0, [-8, 0, -4]], [1.2, [-6, 0, 4]], [L, [-8, 0, -4]]]);
  a.rot("head", [[0, [-6, 0, 4]], [1.2, [-8, 10, -4]], [L, [-6, 0, 4]]]);
  a.rot("wing_r", loopKeys(L, 1.2, [0, 20, 40], [0, -10, 55]));
  a.rot("wing_l", loopKeys(L, 1.2, [0, -20, -40], [0, 10, -55]));
  a.pos("lids", lids([[1.7, 1.82]], L), "step");
  return a;
}

export function held(b) {
  const L = 0.8;
  const a = b.anim("held", L, true);
  a.rot("body", [[0, [6, 0, -6]], [0.4, [6, 0, 6]], [L, [6, 0, -6]]]);
  a.rot("head", [[0, [-10, 0, 6]], [0.4, [-10, 0, -6]], [L, [-10, 0, 6]]]);
  a.rot("wing_r", loopKeys(L, 0.2, [0, 0, 80], [0, 0, 30]));
  a.rot("wing_l", loopKeys(L, 0.2, [0, 0, -30], [0, 0, -80]));
  a.rot("foot_r", loopKeys(L, 0.4, [30, 0, 0], [-30, 0, 0]));
  a.rot("foot_l", loopKeys(L, 0.4, [-30, 0, 0], [30, 0, 0]));
  a.rot("jaw", [[0, [18, 0, 0]], [L, [18, 0, 0]]]);
  return a;
}

export function sleep(b) {
  const L = 4;
  const a = b.anim("sleep", L, true);
  a.scale("body", loopKeys(L, 4, [1.03, 0.97, 1.03], [1.06, 1.0, 1.06]));
  a.rot("body", [[0, [4, 0, -2]], [2, [4, 0, 2]], [L, [4, 0, -2]]]);
  a.rot("head", [[0, [24, 30, 16]], [2, [27, 32, 18]], [L, [24, 30, 16]]]);
  a.pos("head", [[0, [0, -1.8, 0.6]], [L, [0, -1.8, 0.6]]]);
  a.rot("wing_r", [[0, [-24, -30, 10]], [2, [-26, -34, 12]], [L, [-24, -30, 10]]]);
  a.rot("wing_l", [[0, [0, 0, -4]], [L, [0, 0, -4]]]);
  a.pos("lids", shut(L), "step");
  for (const [z, t0] of Object.entries({ z1: 0.2, z2: 1.4, z3: 2.6 })) {
    a.pos(z, [[0, Z0, "step"], [t0, [-2, 14, -1]], [t0 + 0.6, [-3.5, 18, -1]], [t0 + 1.2, [-6, 23, 0], "step"], [t0 + 1.21, Z0, "step"], [L, Z0, "step"]]);
    a.scale(z, [[0, ONE, "step"], [t0, [0.3, 0.3, 0.3]], [t0 + 0.6, [0.8, 0.8, 0.8]], [t0 + 1.2, [1.1, 1.1, 1.1], "step"], [t0 + 1.21, ONE, "step"], [L, ONE, "step"]]);
    a.visible(0, z, false).visible(t0, z, true).visible(t0 + 1.2, z, false);
  }
  return a;
}
