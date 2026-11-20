export const Z0 = [0, 0, 0];
export const OUT = [0, 0, -0.25];
export const ONE = [1, 1, 1];

export function lids(wins, len) {
  const k = [[0, Z0, "step"]];
  for (const [a, c] of wins) k.push([a, OUT, "step"], [c, Z0, "step"]);
  k.push([len, Z0, "step"]);
  return k.sort((x, y) => x[0] - y[0]);
}

export function osc(len, n, a, c = 0, axis = 2) {
  const k = [];
  for (let i = 0; i <= n; i++) {
    const v = [0, 0, 0];
    v[axis] = i % 2 ? a : c;
    k.push([(len * i) / n, v]);
  }
  k[n][1] = k[0][1];
  return k;
}

export function idle(b) {
  const L = 6;
  const a = b.anim("idle", L, true);
  a.pos("body", [[0, Z0], [0.75, [0, 0.35, 0]], [1.5, Z0], [2.25, [0, 0.35, 0]], [3, Z0], [3.75, [0, 0.35, 0]], [4.5, Z0], [5.25, [0, 0.35, 0]], [L, Z0]]);
  a.rot("head", [[0, Z0], [1.0, [0, 12, 7]], [1.9, [0, 12, 7]], [2.4, Z0], [3.9, [5, -14, -4]], [4.8, [5, -14, -4]], [5.3, Z0], [L, Z0]]);
  a.rot("antenna", [[0, Z0], [1.0, [0, 0, -12]], [1.3, [0, 0, 8]], [1.6, [0, 0, -4]], [2.0, Z0], [3.9, [0, 0, 10]], [4.2, [0, 0, -7]], [4.5, [0, 0, 3]], [5.0, Z0], [L, Z0]]);
  a.rot("arm_r", [[0, [0, 0, 4]], [3, [4, 0, 8]], [L, [0, 0, 4]]]);
  a.rot("arm_l", [[0, [0, 0, -4]], [3, [-4, 0, -8]], [L, [0, 0, -4]]]);
  a.pos("lids", lids([[1.5, 1.62], [4.25, 4.37], [4.55, 4.67]], L), "step");
  return a;
}

export function walk(b) {
  const L = 0.5, h = L / 2;
  const a = b.anim("walk", L, true);
  a.rot("leg_r", [[0, [-26, 0, 0]], [h, [26, 0, 0]], [L, [-26, 0, 0]]]);
  a.rot("leg_l", [[0, [26, 0, 0]], [h, [-26, 0, 0]], [L, [26, 0, 0]]]);
  a.rot("foot_r", [[0, [20, 0, 0]], [h, [-20, 0, 0]], [L, [20, 0, 0]]]);
  a.rot("foot_l", [[0, [-20, 0, 0]], [h, [20, 0, 0]], [L, [-20, 0, 0]]]);
  a.pos("leg_r", [[0, Z0], [h / 2, [0, 1, 0]], [h, Z0], [L, Z0]]);
  a.pos("leg_l", [[0, Z0], [h, Z0], [h * 1.5, [0, 1, 0]], [L, Z0]]);
  a.pos("body", [[0, Z0], [h / 2, [0, 0.7, 0]], [h, Z0], [h * 1.5, [0, 0.7, 0]], [L, Z0]]);
  a.rot("body", [[0, [0, 4, -3]], [h, [0, -4, 3]], [L, [0, 4, -3]]]);
  a.rot("arm_r", [[0, [24, 0, 6]], [h, [-24, 0, 6]], [L, [24, 0, 6]]]);
  a.rot("arm_l", [[0, [-24, 0, -6]], [h, [24, 0, -6]], [L, [-24, 0, -6]]]);
  a.rot("head", [[0, [0, -3, 3]], [h, [0, 3, -3]], [L, [0, -3, 3]]]);
  a.rot("antenna", [[0, [-8, 0, -8]], [h, [-8, 0, 8]], [L, [-8, 0, -8]]]);
  a.sound(0.02, "minecraft:block.copper.step", { volume: 0.1, pitch: 1.7 });
  a.sound(h + 0.02, "minecraft:block.copper.step", { volume: 0.1, pitch: 1.8 });
  return a;
}

export function sprint(b) {
  const L = 0.36, h = L / 2;
  const a = b.anim("sprint", L, true);
  a.rot("body", [[0, [-14, 0, -3]], [h, [-14, 0, 3]], [L, [-14, 0, -3]]]);
  a.rot("head", [[0, [10, 0, 0]], [L, [10, 0, 0]]]);
  a.rot("leg_r", [[0, [-40, 0, 0]], [h, [40, 0, 0]], [L, [-40, 0, 0]]]);
  a.rot("leg_l", [[0, [40, 0, 0]], [h, [-40, 0, 0]], [L, [40, 0, 0]]]);
  a.pos("bot", [[0, Z0], [h / 2, [0, 1.2, 0]], [h, Z0], [h * 1.5, [0, 1.2, 0]], [L, Z0]]);
  a.rot("arm_r", [[0, [50, 0, 12]], [h, [-50, 0, 12]], [L, [50, 0, 12]]]);
  a.rot("arm_l", [[0, [-50, 0, -12]], [h, [50, 0, -12]], [L, [-50, 0, -12]]]);
  a.rot("antenna", [[0, [-30, 0, 0]], [h, [-20, 0, 0]], [L, [-30, 0, 0]]]);
  a.particle(0.1, { particle: "minecraft:small_flame", locator: "jet", count: 1, spread: [0.04, 0.02, 0.04], velocity: [0, -0.02, 0.04] });
  return a;
}

export function hover(b) {
  const L = 0.6;
  const a = b.anim("hover", L, true);
  a.rot("leg_r", [[0, [12, 0, 4]], [0.3, [18, 0, 4]], [L, [12, 0, 4]]]);
  a.rot("leg_l", [[0, [18, 0, -4]], [0.3, [12, 0, -4]], [L, [18, 0, -4]]]);
  a.rot("arm_r", [[0, [0, 0, 40]], [0.3, [0, 0, 48]], [L, [0, 0, 40]]]);
  a.rot("arm_l", [[0, [0, 0, -40]], [0.3, [0, 0, -48]], [L, [0, 0, -40]]]);
  a.rot("antenna", [[0, [-16, 0, 0]], [0.3, [-24, 0, 0]], [L, [-16, 0, 0]]]);
  a.pos("lids", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  for (const t of [0, 0.15, 0.3, 0.45]) a.particle(t, { particle: "minecraft:small_flame", locator: "jet", count: 1, spread: [0.04, 0.02, 0.04], velocity: [0, -0.06, 0] });
  return a;
}

export function sit(b) {
  const L = 3;
  const a = b.anim("sit", L, true);
  a.pos("body", [[0, [0, -3, 0]], [1.5, [0, -2.8, 0]], [L, [0, -3, 0]]]);
  a.pos("leg_r", [[0, [0, -3, 0]], [L, [0, -3, 0]]]);
  a.pos("leg_l", [[0, [0, -3, 0]], [L, [0, -3, 0]]]);
  a.rot("leg_r", [[0, [80, 0, 6]], [L, [80, 0, 6]]]);
  a.rot("leg_l", [[0, [80, 0, -6]], [L, [80, 0, -6]]]);
  a.rot("foot_r", [[0, [-70, 0, 0]], [1.5, [-60, 0, 0]], [L, [-70, 0, 0]]]);
  a.rot("foot_l", [[0, [-70, 0, 0]], [1.7, [-80, 0, 0]], [L, [-70, 0, 0]]]);
  a.rot("arm_r", [[0, [30, 0, 8]], [L, [30, 0, 8]]]);
  a.rot("arm_l", [[0, [30, 0, -8]], [L, [30, 0, -8]]]);
  a.rot("head", [[0, [4, 0, 6]], [1.5, [4, 0, -6]], [L, [4, 0, 6]]]);
  a.pos("lids", lids([[2.2, 2.32]], L), "step");
  return a;
}

export function powerDown(b) {
  const L = 4;
  const a = b.anim("power_down", L, true);
  a.pos("body", [[0, [0, -0.8, 0]], [2, [0, -1.0, 0]], [L, [0, -0.8, 0]]]);
  a.rot("leg_r", [[0, [10, 0, 0]], [L, [10, 0, 0]]]);
  a.rot("leg_l", [[0, [10, 0, 0]], [L, [10, 0, 0]]]);
  a.rot("foot_r", [[0, [-10, 0, 0]], [L, [-10, 0, 0]]]);
  a.rot("foot_l", [[0, [-10, 0, 0]], [L, [-10, 0, 0]]]);
  a.rot("head", [[0, [-16, 0, 4]], [2, [-19, 0, 6]], [L, [-16, 0, 4]]]);
  a.pos("head", [[0, [0, -0.6, 0]], [L, [0, -0.6, 0]]]);
  a.rot("antenna", [[0, [-38, 0, 10]], [2, [-42, 0, 12]], [L, [-38, 0, 10]]]);
  a.rot("arm_r", [[0, [-6, 0, -2]], [L, [-6, 0, -2]]]);
  a.rot("arm_l", [[0, [-6, 0, 2]], [L, [-6, 0, 2]]]);
  a.pos("lids", [[0, OUT, "step"], [L, OUT, "step"]], "step");
  return a;
}

export function lowPower(b) {
  const L = 3;
  const a = b.anim("low_power", L, true);
  a.pos("body", [[0, [0, -0.5, 0]], [1.5, [0, -0.7, 0]], [L, [0, -0.5, 0]]]);
  a.rot("head", [[0, [-10, 0, 8]], [1.5, [-12, 0, -4]], [L, [-10, 0, 8]]]);
  a.rot("antenna", [[0, [-30, 0, 0]], [1.5, [-34, 0, 6]], [L, [-30, 0, 0]]]);
  a.rot("arm_r", [[0, [-4, 0, 0]], [L, [-4, 0, 0]]]);
  a.rot("arm_l", [[0, [-4, 0, 0]], [L, [-4, 0, 0]]]);
  a.pos("lids", lids([[0.6, 1.4]], L), "step");
  return a;
}

export function held(b) {
  const L = 0.8;
  const a = b.anim("held", L, true);
  a.rot("leg_r", [[0, [-30, 0, 0]], [0.2, [30, 0, 0]], [0.4, [-30, 0, 0]], [0.6, [30, 0, 0]], [L, [-30, 0, 0]]]);
  a.rot("leg_l", [[0, [30, 0, 0]], [0.2, [-30, 0, 0]], [0.4, [30, 0, 0]], [0.6, [-30, 0, 0]], [L, [30, 0, 0]]]);
  a.rot("arm_r", [[0, [0, 0, 120]], [0.2, [0, 0, 70]], [0.4, [0, 0, 120]], [0.6, [0, 0, 70]], [L, [0, 0, 120]]]);
  a.rot("arm_l", [[0, [0, 0, -70]], [0.2, [0, 0, -120]], [0.4, [0, 0, -70]], [0.6, [0, 0, -120]], [L, [0, 0, -70]]]);
  a.rot("antenna", osc(L, 4, 18, -18));
  a.rot("head", [[0, [8, 0, 6]], [0.4, [8, 0, -6]], [L, [8, 0, 6]]]);
  return a;
}
