import { fur, rim, each, ellipse, stroke, arc, mix, rgba, K } from "../../../src/polar-pals/_polar.mjs";
import { R, metal, plain, glass, bulb } from "./_palette.mjs";
import { FACES } from "./_faces.mjs";

const trim = (seed) => plain(R.trimLight, R.trim, seed, 0.2);
const accent = (seed) => plain(mix(R.accent, "#ffffff", 0.2), R.accentDark, seed, 0.18);

function gauge(img) {
  metal(30, { patina: false }).sides(img, null, "north");
  ellipse(img, 2.5, 2, 1.45, 1.45, R.trim);
  ellipse(img, 2.5, 2, 1.2, 1.2, "#f6f1e3");
  for (const a of [200, 240, 280, 320]) stroke(img, arc(2.5, 2, 1.0, 1.0, a, a + 1, 1), 0.14, R.trim);
  stroke(img, [[2.5, 2], [3.25, 1.35]], 0.18, R.warn);
  ellipse(img, 2.5, 2, 0.22, 0.22, R.trim);
  ellipse(img, 4.4, 1.0, 0.24, 0.24, R.ok);
  return ellipse(img, 4.4, 2.0, 0.24, 0.24, R.amber);
}

function cells(img) {
  each(img, () => rgba(R.cell));
  const w = img.width / K, h = img.height / K;
  for (let u = 1; u < w; u += 1) stroke(img, [[u, 0.2], [u, h - 0.2]], 0.1, R.cellLine, 0.8);
  for (let v = 1.25; v < h; v += 1.25) stroke(img, [[0.2, v], [w - 0.2, v]], 0.1, R.cellLine, 0.8);
  each(img, (u, v) => ((u + v) % 3 < 0.5 ? [255, 255, 255, 40] : null));
  return rim(img, 0.25, 0.4);
}

export function body(b) {
  b.bone("bot", { attach: "world", pivot: [0, 0, 0] });
  for (const [s, x] of [["r", 1], ["l", -1]]) {
    const leg = b.bone(`leg_${s}`, { parent: "bot", pivot: [2 * x, 5, 0] });
    leg.box([x > 0 ? 1 : -3, 2, -1], [2, 3, 2], trim(10 + x));
    const foot = b.bone(`foot_${s}`, { parent: `leg_${s}`, pivot: [2 * x, 2, 0] });
    foot.box([x > 0 ? 0 : -4, 0, -2.5], [4, 2, 4], metal(12 + x, { line: 1.2 }), { inflate: 0.1 });
  }
  const torso = b.bone("body", { parent: "bot", pivot: [0, 5, 0] });
  torso.box([-4, 5, -3], [8, 6, 6], metal(20, { rivets: true }));
  torso.box([-3, 4, -2], [6, 1, 4], trim(21));
  torso.box([-2.5, 6, -3.5], [5, 4, 1], { north: gauge, all: metal(22).sides, up: metal(22).up });
  torso.box([-1, 3, 2], [2, 2, 2], trim(23));
  const solar = b.bone("solar", { parent: "body", pivot: [0, 10.5, 3.5] });
  solar.box([-3, 5.5, 3], [6, 5, 1], { south: cells, all: plain(R.trimLight, R.trim, 24).all });
  for (const [s, x] of [["r", 1], ["l", -1]]) {
    const w = b.bone(`solar_${s}`, { parent: "solar", pivot: [3 * x, 8, 4.5] });
    w.box([x > 0 ? 0 : -3, 5.5, 4], [3, 5, 1], { north: cells, south: cells, all: plain(R.trimLight, R.trim, 25 + x).all });
  }
  for (const [s, x] of [["r", 1], ["l", -1]]) {
    const arm = b.bone(`arm_${s}`, { parent: "body", pivot: [4.5 * x, 10, 0] });
    arm.box([x > 0 ? 4 : -6, 6.5, -1], [2, 4, 2], trim(30 + x));
    arm.box([x > 0 ? 3.75 : -5.75, 9, -1.25], [2, 2, 2], metal(31 + x, { patina: false }), { inflate: 0.25 });
    arm.box([x > 0 ? 3.75 : -5.75, 4.5, -1], [2, 2, 2], accent(32 + x), { inflate: 0.3 });
  }
}

export function head(b) {
  const h = b.bone("head", { parent: "body", pivot: [0, 11.5, 0] });
  h.box([-1.5, 11, -1.5], [3, 1, 3], trim(40));
  const m = metal(41, { rivets: true });
  h.box([-5, 12, -4.5], [10, 8, 9], { ...m, north: (img) => rim(fur(img, { top: R.trimLight, bottom: R.trim, seed: 42, strands: 0, grain: 0.02 }), 0.25, 0.9) });
  h.box([-6, 14, -1.5], [1, 3, 3], accent(43), { inflate: 0.1 });
  h.box([5, 14, -1.5], [1, 3, 3], accent(44), { inflate: 0.1 });
  h.box([-1.5, 19, -5.5], [3, 2, 1], trim(45));
  const screen = b.bone("screen", { parent: "head", pivot: [0, 16, -4.6] });
  const dark = (img) => each(img, () => rgba(R.screenLow));
  screen.box([-4, 12.75, -5.1], [8, 6, 1], { north: FACES.base, all: dark });
  const lids = b.bone("lids", { parent: "head", pivot: [0, 16, -5.05], emissive: true, translucent: true });
  lids.box([-4, 12.75, -5.05], [8, 6, 0], { north: FACES.lids });
  for (const f of ["happy", "heart", "error", "sleep", "scan", "wow", "sad", "cool"]) {
    const bone = b.bone(`face_${f}`, { parent: "head", pivot: [0, 16, -5.18], emissive: true, translucent: true });
    bone.box([-4, 12.75, -5.18], [8, 6, 0], { north: FACES[f] });
  }
  const ant = b.bone("antenna", { parent: "head", pivot: [0, 20, 0] });
  ant.box([-0.5, 20, -0.5], [1, 3, 1], trim(46));
  ant.box([-1, 20, -1], [2, 1, 2], accent(47));
  const bulbs = [["bulb", R.glow, 0], ["bulb_happy", R.ok, 0.06], ["bulb_low", R.amber, 0.12], ["bulb_alert", R.warn, 0.18]];
  for (const [name, c, inflate] of bulbs) {
    b.bone(name, { parent: "antenna", pivot: [0, 24, 0], emissive: true }).box([-1, 23, -1], [2, 2, 2], bulb(c, 48), { inflate });
  }
}

export function lamp(b) {
  b.bone("lamp", { parent: "head", pivot: [0, 20, -5.6], emissive: true }).box([-1, 19.5, -5.6], [2, 1, 0], { north: (img) => each(img, () => rgba(R.lamp)) });
  const cone = b.bone("lamp_glow", { parent: "head", pivot: [0, 20, -5.6], rotation: [-16, 0, 0], emissive: true, translucent: true });
  [[2, 2, 3, 0.2], [3, 3, 4, 0.13], [5, 4, 5, 0.08], [7, 5, 6, 0.045]].reduce((z, [w, h, d, a]) => {
    cone.box([-w / 2, 20 - h / 2, z - d], [w, h, d], glass(R.lamp, a));
    return z - d;
  }, -5.6);
  const pool = b.bone("lamp_pool", { parent: "bot", pivot: [0, 0, 0], emissive: true, translucent: true });
  const c = rgba(R.lamp);
  pool.box([-6, 0.08, -20], [12, 0, 14], {
    up: (img) => {
      for (let y = 0; y < img.height; y++)
        for (let x = 0; x < img.width; x++) {
          const d = Math.hypot((x + 0.5) / img.width * 2 - 1, (y + 0.5) / img.height * 2 - 1);
          img.set(x, y, [c[0], c[1], c[2], d >= 1 ? 0 : Math.round(150 * (1 - d) ** 1.6)]);
        }
      return img;
    },
  });
}

export function beam(b) {
  const bm = b.bone("beam", { parent: "head", pivot: [0, 16, -4.7], emissive: true, translucent: true });
  bm.box([-0.5, 15.5, -40.7], [1, 1, 36], glass(R.ok, 0.85));
  bm.box([-1.5, 14.5, -40.7], [3, 3, 36], glass(R.ok, 0.2));
}

export function umbrella(b) {
  const u = b.bone("umbrella", { parent: "head", pivot: [0, 20, 3] });
  u.box([-0.5, 19, 2.5], [1, 8, 1], plain(R.trimLight, R.trim, 50));
  const top = b.bone("umbrella_top", { parent: "umbrella", pivot: [0, 27, 3] });
  const stripes = (img) => {
    const w = img.width / K, h = img.height / K;
    each(img, (x, y) => {
      const a = Math.atan2(y - h / 2, x - w / 2);
      return rgba(Math.floor(((a + Math.PI) / (2 * Math.PI)) * 8) % 2 ? R.umbA : R.umbB);
    });
    return rim(img, 0.15, 0.8);
  };
  const under = (img) => rim(fur(img, { top: mix(R.umbA, "#000000", 0.2), seed: 51, strands: 0 }), 0.1, 0.6);
  [[12, 27, 52], [10, 28, 53], [6, 29, 54]].forEach(([w, y, seed]) => {
    top.box([-w / 2, y, 3 - w / 2], [w, 1, w], { up: stripes, down: under, sides: plain(R.umbA, mix(R.umbA, "#000000", 0.15), seed).all });
  });
  top.box([-0.5, 30, 2.5], [1, 1, 1], plain(R.trimLight, R.trim, 55));
}

export function handProps(b) {
  const t = b.bone("torch", { parent: "arm_r", pivot: [5, 5.5, 0] });
  t.box([4.5, 5, -0.5], [1, 7, 1], plain(R.wood, R.woodDark, 60));
  const fl = b.bone("torch_flame", { parent: "torch", pivot: [5, 12, 0], emissive: true });
  fl.box([4.5, 12, -0.5], [1, 2, 1], { all: (img) => fur(img, { top: "#fff6b0", bottom: "#ff9b2e", seed: 61, strands: 0 }) }, { inflate: 0.3 });
  const f = b.bone("flag", { parent: "arm_l", pivot: [-5, 5.5, 0] });
  f.box([-5.5, 5, -0.5], [1, 13, 1], plain("#e8e8ee", "#a9adb8", 62));
  f.box([-5.5, 18, -0.5], [1, 1, 1], plain(R.star, R.amber, 63), { inflate: 0.2 });
  const cloth = b.bone("flag_cloth", { parent: "flag", pivot: [-5.5, 17, 0] });
  const paint = (img) => {
    fur(img, { top: R.flag, bottom: R.flagDark, seed: 64, strands: 0, grain: 0.02 });
    stroke(img, [[3.5, 0.8], [3.9, 1.7], [4.8, 1.8], [4.1, 2.4], [4.3, 3.3], [3.5, 2.8], [2.7, 3.3], [2.9, 2.4], [2.2, 1.8], [3.1, 1.7], [3.5, 0.8]], 0.3, R.star);
    ellipse(img, 3.5, 2.25, 0.55, 0.5, R.star);
    return rim(img, 0.15, 0.6);
  };
  cloth.box([-12.5, 13, 0], [7, 4, 0], { north: paint, south: paint });
}
