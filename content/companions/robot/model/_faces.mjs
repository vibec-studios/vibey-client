import { each, ellipse, stroke, arc, shape, blush, mix, rgba, rim } from "../../../src/polar-pals/_polar.mjs";
import { R } from "./_palette.mjs";

function screen(img) {
  const top = rgba(R.screen), low = rgba(R.screenLow);
  each(img, (u, v, x, y) => {
    const t = v / 6;
    const c = top.map((a, i) => a + (low[i] - a) * t);
    const s = y % 2 ? 0.9 : 1;
    return [c[0] * s, c[1] * s, c[2] * s, 255];
  });
  blush(img, 4, 3, 4.2, 3.2, R.glowDim, 0.16);
  return rim(img, 0.35, 0.5);
}

const lineEye = (img, cx, cy, kind, c = R.glow) => stroke(img, kind === "happy" ? arc(cx, cy + 0.35, 0.75, 0.7, 200, 340, 10) : arc(cx, cy - 0.2, 0.75, 0.55, 20, 160, 10), 0.42, c);

function smile(img, w = 0.9, c = R.glow) {
  return stroke(img, arc(4, 4.05, w, 0.55, 20, 160, 10), 0.32, c);
}

function heart(img, cx, cy, s, c) {
  return shape(img, (u, v) => {
    const x = (u - cx) / s, y = -(v - cy) / s + 0.2;
    return (x * x + y * y - 1) ** 3 - x * x * y * y * y <= 0;
  }, c, 1, undefined, [cx - 1.4 * s, cy - 1.4 * s, cx + 1.4 * s, cy + 1.4 * s]);
}

export const FACES = {
  base: (img) => {
    screen(img);
    for (const cx of [2.55, 5.45]) {
      ellipse(img, cx, 2.75, 0.78, 1.05, R.glow);
      ellipse(img, cx - 0.25, 2.35, 0.24, 0.28, "#ffffff", 0.9);
    }
    blush(img, 1.45, 4.1, 0.7, 0.4, R.love, 0.45);
    blush(img, 6.55, 4.1, 0.7, 0.4, R.love, 0.45);
    return smile(img, 0.55);
  },
  lids: (img) => {
    screen(img);
    for (const cx of [2.55, 5.45]) lineEye(img, cx, 2.9, "sleep");
    return smile(img, 0.55);
  },
  happy: (img) => {
    screen(img);
    for (const cx of [2.55, 5.45]) lineEye(img, cx, 2.6, "happy");
    shape(img, (u, v) => ((u - 4) / 1.1) ** 2 + ((v - 3.9) / 0.85) ** 2 <= 1 && v > 3.9, R.glow);
    shape(img, (u, v) => ((u - 4) / 0.55) ** 2 + ((v - 4.45) / 0.3) ** 2 <= 1 && v > 4.3, R.love, 0.9);
    blush(img, 1.4, 4.1, 0.75, 0.42, R.love, 0.6);
    return blush(img, 6.6, 4.1, 0.75, 0.42, R.love, 0.6);
  },
  heart: (img) => {
    screen(img);
    heart(img, 2.55, 2.75, 0.95, R.love);
    heart(img, 5.45, 2.75, 0.95, R.love);
    ellipse(img, 2.2, 2.3, 0.2, 0.16, "#ffffff", 0.9);
    ellipse(img, 5.1, 2.3, 0.2, 0.16, "#ffffff", 0.9);
    return smile(img, 0.7, R.love);
  },
  error: (img) => {
    screen(img);
    for (const cx of [2.55, 5.45]) {
      stroke(img, [[cx - 0.7, 2.05], [cx + 0.7, 3.45]], 0.42, R.warn);
      stroke(img, [[cx + 0.7, 2.05], [cx - 0.7, 3.45]], 0.42, R.warn);
    }
    return stroke(img, [[2.9, 4.6], [3.35, 4.2], [3.8, 4.6], [4.25, 4.2], [4.7, 4.6], [5.15, 4.2]], 0.28, R.warn);
  },
  sleep: (img) => {
    screen(img);
    for (const cx of [2.55, 5.45]) stroke(img, [[cx - 0.7, 3.0], [cx + 0.7, 3.0]], 0.36, R.glowDim);
    stroke(img, [[6.1, 0.8], [7.0, 0.8], [6.1, 1.7], [7.0, 1.7]], 0.22, R.glow);
    stroke(img, [[5.3, 1.5], [5.8, 1.5], [5.3, 2.0], [5.8, 2.0]], 0.16, R.glowDim);
    return stroke(img, arc(4, 4.4, 0.35, 0.25, 0, 360, 14), 0.22, R.glowDim);
  },
  scan: (img) => {
    screen(img);
    stroke(img, arc(4, 3, 1.55, 1.55, 0, 360, 28), 0.26, R.ok);
    stroke(img, arc(4, 3, 0.7, 0.7, 0, 360, 18), 0.22, R.ok);
    for (const [a, b] of [[[4, 0.9], [4, 1.8]], [[4, 4.2], [4, 5.1]], [[1.9, 3], [2.8, 3]], [[5.2, 3], [6.1, 3]]]) stroke(img, [a, b], 0.22, R.ok);
    each(img, (u, v) => (Math.abs(v - 3) < 0.12 ? [...rgba(R.ok).slice(0, 3), 150] : null));
    return ellipse(img, 4, 3, 0.22, 0.22, "#ffffff");
  },
  wow: (img) => {
    screen(img);
    for (const cx of [2.45, 5.55]) {
      stroke(img, arc(cx, 2.65, 0.85, 0.95, 0, 360, 24), 0.34, R.glow);
      ellipse(img, cx, 2.65, 0.28, 0.32, R.glow);
    }
    return stroke(img, arc(4, 4.5, 0.38, 0.42, 0, 360, 16), 0.28, R.glow);
  },
  sad: (img) => {
    screen(img);
    stroke(img, [[1.8, 2.2], [3.1, 2.7]], 0.32, R.glow);
    stroke(img, [[6.2, 2.2], [4.9, 2.7]], 0.32, R.glow);
    ellipse(img, 2.6, 3.2, 0.5, 0.5, R.glow);
    ellipse(img, 5.4, 3.2, 0.5, 0.5, R.glow);
    ellipse(img, 1.85, 4.2, 0.22, 0.34, "#7cc8ff", 0.95);
    return stroke(img, arc(4, 4.95, 0.75, 0.45, 200, 340, 10), 0.3, R.glow);
  },
  cool: (img) => {
    screen(img);
    for (const cx of [2.45, 5.55]) {
      shape(img, (u, v) => v > 2.05 && v < 3.5 && Math.abs(u - cx) < 1.15 - Math.max(0, v - 2.8) * 0.9, mix(R.screenLow, R.glowDim, 0.35));
      stroke(img, [[cx - 1.15, 2.05], [cx + 1.15, 2.05]], 0.3, R.glow);
      stroke(img, [[cx - 0.6, 2.45], [cx - 0.1, 2.45]], 0.18, "#ffffff", 0.85);
    }
    stroke(img, [[3.6, 2.15], [4.4, 2.15]], 0.24, R.glow);
    return stroke(img, [[3.4, 4.45], [4.3, 4.45], [4.8, 4.05]], 0.3, R.glow);
  },
};
