import { K, P, fur, rim, each, ellipse, stroke, mix, rgba } from "../../../src/polar-pals/_polar.mjs";

const clear = (img) => each(img, () => [0, 0, 0, 0]);
const flat = (top, bottom, seed, amount = 0.14) => (img) => rim(fur(img, { top, bottom: bottom ?? top, seed, strands: 0, grain: 0.015 }), amount, 0.8);

function wedges(cx, cy, n, a, b) {
  return (img) => each(img, (u, v) => rgba(Math.floor(((Math.atan2(v - cy, u - cx) / Math.PI + 1) * n) / 2) % 2 ? a : b));
}

export function umbrella(b) {
  const u = b.bone("umbrella", { parent: "body", pivot: [7.5, 7, -1], rotation: [0, 0, 13] });
  const wood = flat("#8a5a3b", "#5e3b25", 3, 0.1);
  u.box([7, 7, -1.5], [1, 25, 1], { all: wood });
  u.box([7, 4.5, -1.5], [1, 3, 1], { all: flat("#3b2a22", null, 4) });
  u.box([8, 4, -1.5], [2, 1, 1], { all: flat("#3b2a22", null, 5) });
  u.box([9.6, 4.6, -1.5], [1, 2, 1], { all: flat("#3b2a22", null, 6) });
  const c = b.bone("canopy", { parent: "umbrella", pivot: [7.5, 33.5, -1] });
  const A = () => P.umbA, B = () => P.umbB;
  const top = (n) => (img) => rim(wedges(img.width / K / 2, img.height / K / 2, n, A(), B())(img), 0.08, 0.6);
  const side = (img) => {
    each(img, (u) => rgba(Math.floor(u / 2.25) % 2 ? A() : B()));
    return rim(img, 0.18, 0.5);
  };
  const under = (img) => {
    fur(img, { top: mix(P.umbA, "#000000", 0.25), seed: 9, strands: 0, grain: 0.01 });
    const w = img.width / K, h = img.height / K;
    for (let i = 0; i < 8; i++) {
      const a = (i / 8) * Math.PI * 2;
      stroke(img, [[w / 2, h / 2], [w / 2 + Math.cos(a) * w * 0.48, h / 2 + Math.sin(a) * h * 0.48]], 0.18, "#2b2026", 0.6);
    }
    return img;
  };
  c.box([-1.5, 28, -10], [18, 1, 18], { up: top(8), down: under, sides: side });
  c.box([0.5, 29, -8], [14, 2, 14], { up: top(8), down: under, sides: side });
  c.box([3, 31, -5.5], [9, 1, 9], { up: top(8), sides: side, down: under });
  c.box([5.5, 32, -3], [4, 1, 4], { all: top(4) });
  c.box([7, 33, -1.5], [1, 1, 1], { all: flat("#3b2a22", null, 7) });
  b.locator("umbrella", "canopy", [7.5, 28, -1]);
}

export function sunglasses(b) {
  const s = b.bone("shades", { parent: "head", pivot: [0, 17, -4.5] });
  const lens = (img) => {
    fur(img, { top: "#1b1d2a", bottom: "#33264a", seed: 11, strands: 0, grain: 0.01 });
    const w = img.width / K;
    stroke(img, [[0.5, 0.5], [w * 0.55, 0.5]], 0.35, "#ffffff", 0.55);
    return rim(img, 0.2, 0.5);
  };
  const frame = flat("#ff5ab4", "#d93a8f", 12, 0.1);
  s.box([0.4, 16.3, -4.75], [3, 2, 0], { north: lens, south: lens });
  s.box([-3.4, 16.3, -4.75], [3, 2, 0], { north: lens, south: lens });
  s.box([-0.6, 17.6, -4.8], [1, 1, 0], { north: frame, south: frame });
  s.box([-4.4, 17.9, -4.8], [9, 1, 0], { north: frame, south: frame });
  s.box([4.5, 17.9, -4.4], [0, 1, 4], { east: frame, west: frame });
  s.box([-4.5, 17.9, -4.4], [0, 1, 4], { east: frame, west: frame });
}

export function fan(b) {
  const f = b.bone("fan", { parent: "wing_l", pivot: [-6.1, 5.5, -0.5] });
  const paper = (img) => {
    clear(img);
    const w = img.width / K, h = img.height / K;
    each(img, (u, v) => {
      const dx = u - w / 2, dy = h - v;
      const r = Math.hypot(dx, dy);
      if (r > w / 2 || dy < 0) return null;
      const a = Math.atan2(dy, dx);
      const pleat = Math.floor((a / Math.PI) * 9) % 2;
      if (r < 0.9) return rgba("#6b4630");
      return mix(pleat ? "#ffd1e6" : "#ff9cc9", "#ff5aa0", Math.max(0, r / (w / 2) - 0.7));
    });
    return img;
  };
  f.box([-9.1, 0.5, -0.5], [6, 3, 0], { north: paper, south: paper });
}

export function snowball(b) {
  const s = b.bone("snowball", { parent: "wing_r", pivot: [6.1, 5, 0] });
  const snow = (img) => rim(fur(img, { top: P.snow, bottom: P.snowShade, seed: 21, strands: 0.02, grain: 0.04 }), 0.2, 0.9);
  s.box([5.1, 2, -1.5], [2, 2, 3], { all: snow }, { inflate: 0.3 });
  s.box([4.6, 2.5, -1], [3, 1, 2], { all: snow }, { inflate: 0.2 });
  b.locator("snowball", "snowball", [6.1, 3, 0]);
}

export function partyHat(b) {
  const h = b.bone("party_hat", { parent: "head", pivot: [1, 22, 0], rotation: [0, 0, -10] });
  const stripes = (img) => {
    each(img, (u, v) => rgba(Math.floor((u + v) / 1.2) % 2 ? "#ffd23f" : "#8b6cff"));
    return rim(img, 0.12, 0.6);
  };
  h.box([-2.5, 22, -2.5], [5, 2, 5], { all: stripes });
  h.box([-1.5, 24, -1.5], [3, 2, 3], { all: stripes });
  h.box([-0.5, 26, -0.5], [1, 2, 1], { all: stripes });
  h.box([-1, 28, -1], [2, 2, 2], { all: flat("#ff5a8a", "#e23d6f", 31) }, { inflate: 0.2 });
}

export function jugglers(b) {
  const paint = (img) => rim(fur(img, { top: P.fishDark, bottom: P.fishLight, seed: 41, strands: 0.02 }), 0.12, 0.5);
  for (const n of ["fish2", "fish3"]) {
    const f = b.bone(n, { parent: "peng", pivot: [0, 14, -6] });
    f.box([-2, 13, -6.5], [4, 2, 1], { all: paint }, { inflate: 0.05 });
    f.box([2, 12.5, -6], [2, 3, 0], { north: tail, south: tail });
  }
}

function tail(img) {
  clear(img);
  const w = img.width / K, h = img.height / K;
  return each(img, (u, v) => (Math.abs(v - h / 2) <= (u / w) * (h / 2) + 0.15 ? mix(P.fishFin, "#ff8a3c", u / w) : null));
}

export function crest(b) {
  const c = b.bone("crest", { parent: "head", pivot: [0, 19, -2] });
  const plumes = (mirror) => (img) => {
    clear(img);
    if (!P.crest) return img;
    const w = img.width / K, h = img.height / K;
    for (let i = 0; i < 5; i++) {
      const x0 = 0.6 + i * 0.35, y0 = h - 0.8 - i * 0.25;
      const pts = [[x0, y0], [x0 + 2.4 + i * 0.5, y0 - 1.4 - (i % 2) * 0.6], [x0 + 4.6 + i * 0.6, 0.5 + (i % 3) * 0.5]];
      stroke(img, mirror ? pts.map(([u, v]) => [w - u, v]) : pts, 0.5, i % 2 ? P.crest : P.crestDark);
    }
    return img;
  };
  const brows = (img) => {
    clear(img);
    if (!P.crest) return img;
    const w = img.width / K;
    for (const s of [1, -1]) {
      const cx = w / 2 + s * 1.0;
      stroke(img, [[cx, 2.4], [cx + s * 1.6, 1.7], [cx + s * 3.2, 0.5]], 0.55, P.crest);
      stroke(img, [[cx + s * 0.6, 2.6], [cx + s * 2.4, 1.6]], 0.3, P.crestDark);
    }
    return img;
  };
  c.box([5.6, 18, -4], [0, 6, 9], { east: plumes(true), west: plumes(false) });
  c.box([-5.6, 18, -4], [0, 6, 9], { east: plumes(true), west: plumes(false) });
  c.box([-4.5, 18.4, -4.06], [9, 3, 0], { north: brows, south: (img) => clear(img) });
}

export function sweat(b) {
  const s = b.bone("sweat", { parent: "head", pivot: [4, 20, -4] });
  const drop = (img) => {
    clear(img);
    ellipse(img, 1, 1.3, 0.55, 0.7, "#8fd3ff");
    ellipse(img, 0.85, 1.1, 0.18, 0.22, "#ffffff", 0.9);
    return img;
  };
  s.box([3.6, 19.5, -4.3], [2, 2, 0], { north: drop, south: drop });
}
