export const Z0 = [0, 0, 0];
export const OUT = [0, 0, -0.3];
export const M = (v, sx) => [v[0], v[1] * sx, v[2] * sx];

export function wag(sx, len, step = 0.8, a = 9, c = -4) {
  const k = [];
  const n = Math.round(len / step);
  for (let i = 0; i <= n; i++) k.push([Math.min(len, i * step), [0, (i % 2 ? a : c) * sx, 0]]);
  k[k.length - 1] = [len, [0, c * sx, 0]];
  return k;
}

export function lids(wins, len) {
  const k = [[0, Z0, "step"]];
  for (const [a, c] of wins) k.push([a, OUT, "step"], [c, Z0, "step"]);
  k.push([len, Z0, "step"]);
  return k.sort((x, y) => x[0] - y[0]);
}

export function hideZs(a) {
  for (const z of ["z1", "z2", "z3"]) a.visible(0, z, false);
  return a;
}

function breathe(a, len, period = 2, amount = 0.03) {
  const k = [];
  for (let t = 0; t < len - 0.01; t += period) k.push([t, [1, 1, 1]], [t + period / 2, [1 + amount * 0.4, 1 + amount, 1]]);
  k.push([len, [1, 1, 1]]);
  return a.scale("body", k);
}

export function idle(b) {
  const L = 6;
  const a = hideZs(b.anim("idle", L, true));
  breathe(a, L);
  a.rot("head", [[0, Z0], [1.2, [3, 6, 4]], [2.4, [3, 6, 4]], [3.0, Z0], [4.2, [1, -8, -3]], [5.2, [1, -8, -3]], [L, Z0]]);
  a.rot("fluke_r", wag(1, L, 0.75, 7, -3));
  a.rot("fluke_l", wag(-1, L, 0.75, 7, -3));
  a.rot("tail", [[0, Z0], [1.5, [-4, 0, 0]], [3, Z0], [4.5, [-4, 0, 0]], [L, Z0]]);
  a.rot("fin_r", [[0, Z0], [2.0, Z0], [2.3, [0, 0, 8]], [2.6, Z0], [L, Z0]]);
  a.rot("fin_l", [[0, Z0], [4.6, Z0], [4.9, [0, 0, -8]], [5.2, Z0], [L, Z0]]);
  a.pos("lids", lids([[1.7, 1.83], [4.0, 4.12], [4.3, 4.42]], L), "step");
  return a;
}

export function sit(b) {
  const L = 3;
  const a = hideZs(b.anim("sit", L, true));
  a.rot("body", [[0, [28, 0, 0]], [1.5, [28, 0, 2]], [L, [28, 0, 0]]]);
  a.scale("body", [[0, [1, 1, 1]], [1.5, [1.01, 1.03, 1]], [L, [1, 1, 1]]]);
  a.rot("head", [[0, [-14, 0, 0]], [1.5, [-12, 4, 5]], [L, [-14, 0, 0]]]);
  a.rot("tail", [[0, [-8, 0, 0]], [L, [-8, 0, 0]]]);
  a.rot("fin_r", [[0, [0, 40, 6]], [1.5, [0, 44, 8]], [L, [0, 40, 6]]]);
  a.rot("fin_l", [[0, [0, -40, -6]], [1.5, [0, -44, -8]], [L, [0, -40, -6]]]);
  a.rot("fluke_r", wag(1, L, 0.75, 8, -2));
  a.rot("fluke_l", wag(-1, L, 0.75, 8, -2));
  a.pos("lids", lids([[2.2, 2.33]], L), "step");
  return a;
}

export function swim(b) {
  const L = 1.2;
  const a = hideZs(b.anim("swim", L, true));
  a.pos("seal", [[0, [0, -3.4, 0]], [0.6, [0, -2.8, 0]], [L, [0, -3.4, 0]]]);
  a.rot("body", [[0, [-6, 0, 0]], [0.3, [4, 0, 0]], [0.6, [-6, 0, 0]], [0.9, [4, 0, 0]], [L, [-6, 0, 0]]]);
  a.rot("head", [[0, [14, 0, 0]], [0.6, [10, 0, 0]], [L, [14, 0, 0]]]);
  a.rot("tail", [[0, [16, 0, 0]], [0.3, [-16, 0, 0]], [0.6, [16, 0, 0]], [0.9, [-16, 0, 0]], [L, [16, 0, 0]]]);
  const paddle = (sx) => [[0, [0, 35 * sx, 0]], [0.3, [0, -30 * sx, 10 * sx]], [0.6, [0, 35 * sx, 0]], [0.9, [0, -30 * sx, 10 * sx]], [L, [0, 35 * sx, 0]]];
  a.rot("fin_r", paddle(1));
  a.rot("fin_l", paddle(-1));
  a.rot("fluke_r", [[0, [0, 14, 0]], [0.3, [0, -4, 0]], [0.6, [0, 14, 0]], [0.9, [0, -4, 0]], [L, [0, 14, 0]]]);
  a.rot("fluke_l", [[0, [0, -14, 0]], [0.3, [0, 4, 0]], [0.6, [0, -14, 0]], [0.9, [0, 4, 0]], [L, [0, -14, 0]]]);
  a.pos("happy", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  a.particle(0.3, { particle: "minecraft:bubble", locator: "nose", count: 2, spread: [0.15, 0.05, 0.15], velocity: [0, 0.04, 0] });
  return a;
}

export function swimIdle(b) {
  const L = 2.4;
  const a = hideZs(b.anim("swim_idle", L, true));
  a.pos("seal", [[0, [0, -3.6, 0]], [1.2, [0, -2.8, 0]], [L, [0, -3.6, 0]]]);
  a.rot("body", [[0, [-4, 0, -3]], [1.2, [-2, 0, 3]], [L, [-4, 0, -3]]]);
  a.rot("head", [[0, [12, 0, 4]], [1.2, [10, 0, -4]], [L, [12, 0, 4]]]);
  const scull = (sx) => [[0, [0, 20 * sx, 6 * sx]], [0.6, [0, 5 * sx, 14 * sx]], [1.2, [0, 20 * sx, 6 * sx]], [1.8, [0, 5 * sx, 14 * sx]], [L, [0, 20 * sx, 6 * sx]]];
  a.rot("fin_r", scull(1));
  a.rot("fin_l", scull(-1));
  a.rot("tail", [[0, [6, 0, 0]], [1.2, [-6, 0, 0]], [L, [6, 0, 0]]]);
  a.pos("lids", lids([[1.5, 1.63]], L), "step");
  return a;
}

export function held(b) {
  const L = 0.8;
  const a = hideZs(b.anim("held", L, true));
  a.rot("body", [[0, [8, 0, -5]], [0.4, [8, 0, 5]], [L, [8, 0, -5]]]);
  a.rot("head", [[0, [16, 0, 6]], [0.4, [16, 0, -6]], [L, [16, 0, 6]]]);
  const flail = (sx, ph) => [[0, [0, 10 * sx, (ph ? 40 : -30) * sx]], [0.2, [0, 10 * sx, (ph ? -30 : 40) * sx]], [0.4, [0, 10 * sx, (ph ? 40 : -30) * sx]], [0.6, [0, 10 * sx, (ph ? -30 : 40) * sx]], [L, [0, 10 * sx, (ph ? 40 : -30) * sx]]];
  a.rot("fin_r", flail(1, 0));
  a.rot("fin_l", flail(-1, 1));
  a.rot("tail", [[0, [-20, 15, 0]], [0.2, [-20, -15, 0]], [0.4, [-20, 15, 0]], [0.6, [-20, -15, 0]], [L, [-20, 15, 0]]]);
  a.pos("happy", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  return a;
}

export function yawn(b) {
  const L = 2.2;
  const a = b.anim("yawn", L, false);
  a.rot("head", [[0, Z0], [0.6, [22, 0, 0]], [1.4, [24, 0, 4]], [L, Z0]]);
  a.scale("body", [[0, [1, 1, 1]], [0.8, [0.97, 1.06, 1.04]], [1.4, [0.97, 1.06, 1.04]], [L, [1, 1, 1]]]);
  a.rot("fin_r", [[0, Z0], [0.7, [0, 0, 30]], [1.4, [0, 0, 30]], [L, Z0]]);
  a.rot("fin_l", [[0, Z0], [0.7, [0, 0, -30]], [1.4, [0, 0, -30]], [L, Z0]]);
  a.pos("lids", [[0, Z0, "step"], [0.4, OUT, "step"], [1.7, Z0, "step"], [L, Z0, "step"]], "step");
  return a;
}
