import { fur, rim, each, ellipse, stroke, mix, rgba, vnoise, K } from "../../../src/polar-pals/_polar.mjs";

export const R = {
  metal: "#d98a52", light: "#f4ad74", dark: "#b06538", deep: "#7c4226",
  trim: "#3a3f4c", trimLight: "#565d6e", accent: "#4fbfa6", accentDark: "#2f8f7b",
  patina: "#5cc4a6", patinaA: 0.55,
  screen: "#18202c", screenLow: "#0e141c", glow: "#7ff4ff", glowDim: "#3fb8c9",
  love: "#ff6fa8", warn: "#ff4d4d", ok: "#7dff9a", amber: "#ffc04a",
  lamp: "#fff3b0", wood: "#8a5a34", woodDark: "#5e3b20",
  flag: "#8b6cff", flagDark: "#5b3fd6", star: "#ffe066",
  umbA: "#ffd23f", umbB: "#3fe0c5", cell: "#24365e", cellLine: "#6f8fd6",
};

export const VARIANTS = {
  iron: {
    metal: "#b3bcc9", light: "#dce2ea", dark: "#848e9d", deep: "#5c6574",
    accent: "#5b8def", accentDark: "#3d68c4", patinaA: 0, glow: "#9fe3ff", glowDim: "#5aa9d6",
  },
  retro: {
    metal: "#efe2c4", light: "#fff7e4", dark: "#cdb98e", deep: "#a38f66",
    trim: "#2a8f87", trimLight: "#45b2a8", accent: "#e2483a", accentDark: "#b02f24",
    patinaA: 0, screen: "#20251f", screenLow: "#121610", glow: "#ffcf6a", glowDim: "#c9973a",
  },
};

export function metal(seed, o = {}) {
  const top = o.top ?? R.light, bottom = o.bottom ?? R.metal;
  const side = (img, f, face) => {
    fur(img, { top, bottom, seed: seed + (face?.length ?? 0), strands: 0.015, grain: 0.022 });
    if (o.patina !== false && R.patinaA > 0) patina(img, seed);
    if (o.rivets) rivets(img);
    if (o.line) panelLine(img, o.line);
    return rim(img, 0.2, 0.8);
  };
  return {
    up: (img) => rim(fur(img, { top: R.light, bottom: mix(R.light, R.metal, 0.4), seed, strands: 0, grain: 0.02 }), 0.16, 0.7),
    down: (img) => rim(fur(img, { top: R.dark, bottom: R.deep, seed: seed + 1, strands: 0, grain: 0.02 }), 0.12, 0.6),
    sides: side,
  };
}

export function plain(top, bottom, seed, amount = 0.16) {
  return { all: (img) => rim(fur(img, { top, bottom: bottom ?? top, seed, strands: 0, grain: 0.02 }), amount, 0.6) };
}

export function patina(img, seed) {
  const c = rgba(R.patina);
  return each(img, (u, v, x, y) => {
    const n = vnoise(x * 0.18 + seed * 3.1, y * 0.18, seed + 11);
    if (n < 0.74) return null;
    return [c[0], c[1], c[2], 255 * R.patinaA * Math.min(1, (n - 0.74) * 6)];
  });
}

export function rivets(img) {
  const w = img.width / K, h = img.height / K;
  if (w < 4 || h < 4) return img;
  for (const [u, v] of [[0.8, 0.8], [w - 0.8, 0.8], [0.8, h - 0.8], [w - 0.8, h - 0.8]]) {
    ellipse(img, u, v, 0.34, 0.34, R.deep);
    ellipse(img, u - 0.08, v - 0.08, 0.2, 0.2, R.light);
  }
  return img;
}

export function panelLine(img, v0) {
  const w = img.width / K;
  stroke(img, [[0.3, v0], [w - 0.3, v0]], 0.18, R.deep, 0.7);
  return stroke(img, [[0.3, v0 + 0.22], [w - 0.3, v0 + 0.22]], 0.12, R.light, 0.5);
}

export function glass(color, alpha) {
  const c = rgba(color);
  const fill = (img) => {
    const w = img.width, h = img.height;
    for (let y = 0; y < h; y++)
      for (let x = 0; x < w; x++) {
        const e = Math.min(x + 0.5, y + 0.5, w - x - 0.5, h - y - 0.5) / (Math.min(w, h) / 2);
        img.set(x, y, [c[0], c[1], c[2], Math.round(255 * alpha * Math.min(1, 0.35 + e))]);
      }
    return img;
  };
  return { all: fill };
}

export function bulb(color, seed) {
  return {
    all: (img) => {
      fur(img, { top: mix(color, "#ffffff", 0.55), bottom: color, seed, strands: 0, grain: 0.02 });
      ellipse(img, 0.6, 0.6, 0.32, 0.26, "#ffffff", 0.85);
      return rim(img, 0.1, 0.5);
    },
  };
}
