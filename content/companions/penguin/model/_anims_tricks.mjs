import { Z0, OUT, ONE, lids } from "./_anims_base.mjs";

export function spin(b) {
  const L = 1.8;
  const a = b.anim("spin", L, false);
  a.rot("peng", [[0, Z0, "linear"], [0.2, Z0, "linear"], [1.4, [0, 720, 0], "linear"], [1.41, Z0, "step"], [L, Z0]], "linear");
  a.pos("peng", [[0, Z0], [0.2, [0, 1.2, 0]], [1.4, [0, 1.2, 0]], [1.6, Z0], [L, Z0]]);
  a.rot("wing_r", [[0, Z0], [0.25, [0, 0, 85]], [1.4, [0, 0, 85]], [1.6, [0, 0, 150]], [L, Z0]]);
  a.rot("wing_l", [[0, Z0], [0.25, [0, 0, -85]], [1.4, [0, 0, -85]], [1.6, [0, 0, -150]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.25, [-10, 0, 0]], [1.4, [-10, 0, 0]], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [0.2, OUT, "step"], [L, Z0, "step"]], "step");
  a.particle(1.45, { particle: "minecraft:snowflake", locator: "feet", count: 8, spread: [0.3, 0.05, 0.3], velocity: [0, 0.05, 0] });
  return a;
}

export function bow(b) {
  const L = 2.2;
  const a = b.anim("bow", L, false);
  a.rot("body", [[0, Z0], [0.5, [-42, 0, 0]], [1.4, [-42, 0, 0]], [1.9, Z0], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.5, [20, 0, 0]], [1.4, [20, 0, 0]], [1.9, Z0], [L, Z0]]);
  a.rot("wing_r", [[0, Z0], [0.5, [-50, -70, 20]], [1.4, [-50, -70, 20]], [1.9, Z0], [L, Z0]]);
  a.rot("wing_l", [[0, Z0], [0.5, [40, 0, -60]], [1.4, [40, 0, -60]], [1.9, Z0], [L, Z0]]);
  a.rot("foot_r", [[0, Z0], [0.5, [20, 0, 0]], [1.4, [20, 0, 0]], [1.9, Z0], [L, Z0]]);
  a.pos("lids", lids([[0.5, 1.5]], L), "step");
  return a;
}

export function wave(b) {
  const L = 1.8;
  const a = b.anim("wave", L, false);
  a.rot("wing_r", [[0, Z0], [0.25, [0, 0, 150]], [0.5, [0, 0, 115]], [0.75, [0, 0, 150]], [1.0, [0, 0, 115]], [1.25, [0, 0, 150]], [1.5, [0, 0, 115]], [L, Z0]]);
  a.rot("body", [[0, Z0], [0.25, [0, 0, -6]], [1.5, [0, 0, -6]], [L, Z0]]);
  a.rot("head", [[0, Z0], [0.3, [-6, 0, -10]], [1.5, [-6, 0, -10]], [L, Z0]]);
  a.rot("jaw", [[0, Z0], [0.3, [18, 0, 0]], [0.6, Z0], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [0.3, OUT, "step"], [1.6, Z0, "step"], [L, Z0, "step"]], "step");
  return a;
}

function circlePath(phase, loops, period, t0, fishRest) {
  const keys = [[0, Z0, "step"], [t0, Z0, "step"]];
  const n = 12;
  for (let l = 0; l < loops; l++)
    for (let i = 0; i < n; i++) {
      const t = t0 + l * period + (i / n) * period;
      const th = -Math.PI / 2 + 2 * Math.PI * (i / n + phase);
      const x = 4.5 * Math.cos(th), y = 19 + 7 * Math.sin(-th);
      keys.push([t, [x - fishRest[0], y - fishRest[1], -7.5 - fishRest[2]]]);
    }
  const end = t0 + loops * period;
  keys.push([end, [-fishRest[0] + 4.5 * Math.cos(-Math.PI / 2 + 2 * Math.PI * phase), 19 + 7 - fishRest[1], -7.5 - fishRest[2]], "step"], [end + 0.01, Z0, "step"]);
  return keys;
}

export function juggle(b) {
  const L = 4.8;
  const a = b.anim("juggle", L, false);
  const period = 1.2, loops = 3, t0 = 0.3, end = t0 + loops * period;
  a.pos("fish", circlePath(0, loops, period, t0, [-1, 17, 0]), "catmullrom");
  a.pos("fish2", circlePath(1 / 3, loops, period, t0, [0, 14, -6]), "catmullrom");
  a.pos("fish3", circlePath(2 / 3, loops, period, t0, [0, 14, -6]), "catmullrom");
  for (const f of ["fish", "fish2", "fish3"]) a.rot(f, [[0, Z0, "linear"], [t0, Z0, "linear"], [end, [0, 0, 1080], "linear"], [end + 0.01, Z0, "step"], [L, Z0]], "linear");
  const hands = (sx) => {
    const k = [[0, Z0], [t0, [-40, 0, 30 * sx]]];
    for (let i = 0; i < loops * 3; i++) k.push([t0 + (i + 0.5) * (period / 3), [-40, 0, (i % 2 ? 30 : 60) * sx]]);
    k.push([end, [-40, 0, 30 * sx]], [end + 0.3, Z0], [L, Z0]);
    return k;
  };
  a.rot("wing_r", hands(1));
  a.rot("wing_l", hands(-1));
  a.rot("head", [[0, Z0], [t0, [-24, 0, 0]], [end, [-24, 0, 0]], [end + 0.3, Z0], [L, Z0]]);
  a.rot("jaw", [[0, Z0], [end, Z0], [end + 0.1, [24, 0, 0]], [end + 0.5, Z0], [L, Z0]]);
  a.pos("peng", [[0, Z0], [end + 0.2, Z0], [end + 0.4, [0, 2, 0]], [end + 0.6, Z0], [L, Z0]]);
  a.pos("happy", [[0, Z0, "step"], [end, OUT, "step"], [L, Z0, "step"]], "step");
  for (const f of ["fish", "fish2", "fish3"]) a.visible(0, f, false).visible(t0, f, true).visible(end, f, false);
  a.particle(end, { particle: "minecraft:splash", locator: "heart", count: 10, spread: [0.3, 0.1, 0.3], velocity: [0, 0.1, 0] });
  return a;
}
