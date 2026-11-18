// Polar Pals – shared palette, HD painting helpers and preview plans.
//
// All Polar Pals textures are HD (K pixels per UV unit, see Texture { hd }), so faces can carry soft shading, fur
// noise, anti-aliased round eyes with reflections and thin whiskers. Every helper below takes coordinates in UV units
// (= model pixels) relative to the face view it paints into, and converts to texture pixels with the view's own
// density, so the same painter works for any HD factor.
import { cosmetic, mix, shade, rgba, rng } from "../_common.mjs";

export { cosmetic, mix, shade, rgba, rng };

/** Texture pixels per UV unit for Polar Pals models. */
export const K = 4;

/** Palette: soft arctic pastels + the Vibe accents. */
export const P = {
  // seal pup (lighter, bluer and softer than the mascot, same family)
  sealTop: "#8d9aab", seal: "#a3aebc", sealSide: "#99a5b4", sealLow: "#b9c2cd", sealDark: "#6f7b8b",
  belly: "#e7ecf1", bellyShade: "#cfd7e0", muzzle: "#f6f8fa", muzzleShade: "#dde3ea", flipper: "#7f8b9b", flipperDark: "#5f6a79",
  spot: "#7c8899",
  // eyes
  eye: "#1b1726", eyeIris: "#2c2440", eyeRim: "#0c0a12", shine: "#ffffff", shine2: "#cfe6ff",
  nose: "#2c2530", noseShine: "#6b6274", mouth: "#4a3f52", blush: "#ff8fb1", tongue: "#ff9fb3",
  // penguin
  pengBack: "#22283a", pengBack2: "#2e3550", pengBackLight: "#3b4566", pengBelly: "#f5f7fb", pengBellyShade: "#d9e0ea",
  beak: "#ffb03d", beakDark: "#e98a24", feet: "#ff9a3c", feetDark: "#dc7424",
  // knit / vibe
  brand: "#8b6cff", brandDark: "#5b3fd6", brandDeep: "#3d2a9a", brandLight: "#b7a5ff",
  teal: "#3fe0c5", tealDark: "#1fae96", tealDeep: "#137a69", tealLight: "#a6f5e6",
  snow: "#f7fbff", snowShade: "#dbe7f3", snowDeep: "#b8cde2",
  ice: "#d9efff", iceMid: "#a8d6f5", iceDeep: "#5fa3d6", iceNight: "#1c3b63",
  fish: "#5ba7e0", fishLight: "#bfe6ff", fishDark: "#2e6fa8", fishFin: "#ffb547",
  // aurora
  auroraG: "#5cffb0", auroraT: "#3fe0c5", auroraB: "#58b8ff", auroraP: "#8b6cff", auroraM: "#ff6fd8",
};

const clamp01 = (v) => (v < 0 ? 0 : v > 1 ? 1 : v);

/** Texture pixels per UV unit of a face view (faces are always whole UV units wide). */
const dens = (img, w) => (w ? img.width / w : K);

/**
 * Per-pixel painter in UV units: fn(u, v, x, y) → colour | null, where (u, v) is the pixel centre in UV units of
 * this face and (x, y) the texture pixel. `k` = pixels per UV unit (default K).
 */
export function each(img, fn, k = K) {
  for (let y = 0; y < img.height; y++)
    for (let x = 0; x < img.width; x++) {
      const c = fn((x + 0.5) / k, (y + 0.5) / k, x, y);
      if (c != null) img.blend(x, y, c);
    }
  return img;
}

/** Hash noise in [0, 1) for integer coordinates (stable, seeded). */
export function hash(x, y, seed = 1) {
  let h = Math.imul(x | 0, 374761393) ^ Math.imul(y | 0, 668265263) ^ Math.imul(seed | 0, 2246822519);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
}

/** Smooth value noise in [0, 1) at a continuous position (bilinear between hashed lattice points). */
export function vnoise(x, y, seed = 1) {
  const xi = Math.floor(x), yi = Math.floor(y), fx = x - xi, fy = y - yi;
  const sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
  const a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
  return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy;
}

/**
 * Soft fur: vertical gradient top → bottom, fine per-pixel grain plus short vertical "strands" (stretched noise),
 * replaces the face. opts: { top, bottom, grain (0..), strands (0..), seed, flip (strands horizontal) }
 */
export function fur(img, opts) {
  const { top, bottom = top, grain = 0.03, strands = 0.06, seed = 1, horizontal = false } = opts;
  const T = rgba(top), B = rgba(bottom);
  const H = img.height, W = img.width;
  for (let y = 0; y < H; y++)
    for (let x = 0; x < W; x++) {
      const t = H > 1 ? y / (H - 1) : 0;
      const c = T.map((v, i) => v + (B[i] - v) * t);
      const sx = horizontal ? y : x, sy = horizontal ? x : y;
      const s = (vnoise(sx * 0.9, sy * 0.28, seed) - 0.5) * 2 * strands + (hash(x, y, seed + 7) - 0.5) * 2 * grain;
      const f = 1 + s;
      img.set(x, y, [c[0] * f, c[1] * f, c[2] * f, c[3]]);
    }
  return img;
}

/** Darken the border of a face (soft "ambient occlusion" so voxel shapes read rounder). width in UV units. */
export function rim(img, amount = 0.12, width = 0.9, k = dens(img)) {
  const wpx = width * k;
  for (let y = 0; y < img.height; y++)
    for (let x = 0; x < img.width; x++) {
      const d = Math.min(x + 0.5, y + 0.5, img.width - x - 0.5, img.height - y - 0.5);
      if (d >= wpx) continue;
      const c = img.get(x, y);
      if (!c[3]) continue;
      const f = 1 - amount * (1 - d / wpx) ** 1.6;
      img.set(x, y, [c[0] * f, c[1] * f, c[2] * f, c[3]]);
    }
  return img;
}

/** Only the given edges: { top, bottom, left, right } amounts. */
export function edges(img, e, width = 0.9, k = K) {
  const wpx = width * k;
  for (let y = 0; y < img.height; y++)
    for (let x = 0; x < img.width; x++) {
      let a = 0;
      const add = (amt, d) => { if (amt && d < wpx) a = Math.max(a, Math.abs(amt) * (1 - d / wpx) ** 1.5 * Math.sign(amt)); };
      add(e.top, y + 0.5);
      add(e.bottom, img.height - y - 0.5);
      add(e.left, x + 0.5);
      add(e.right, img.width - x - 0.5);
      if (!a) continue;
      const c = img.get(x, y);
      if (!c[3]) continue;
      const f = 1 - a;
      img.set(x, y, [c[0] * f, c[1] * f, c[2] * f, c[3]]);
    }
  return img;
}

/** Coverage (0..1) of a pixel by a shape test inside(u, v), 4×4 supersampled. */
function coverage(x, y, k, inside) {
  let n = 0;
  for (let j = 0; j < 4; j++)
    for (let i = 0; i < 4; i++) if (inside((x + (i + 0.5) / 4) / k, (y + (j + 0.5) / 4) / k)) n++;
  return n / 16;
}

/** Anti-aliased filled shape: inside(u, v) in UV units. colour may be a function (u, v) → colour. */
export function shape(img, inside, color, opacity = 1, k = K, bounds = null) {
  const [x0, y0, x1, y1] = bounds
    ? [Math.max(0, Math.floor(bounds[0] * k) - 1), Math.max(0, Math.floor(bounds[1] * k) - 1), Math.min(img.width, Math.ceil(bounds[2] * k) + 1), Math.min(img.height, Math.ceil(bounds[3] * k) + 1)]
    : [0, 0, img.width, img.height];
  for (let y = y0; y < y1; y++)
    for (let x = x0; x < x1; x++) {
      const cv = coverage(x, y, k, inside);
      if (!cv) continue;
      const c = typeof color === "function" ? color((x + 0.5) / k, (y + 0.5) / k) : color;
      img.blend(x, y, c, cv * opacity);
    }
  return img;
}

export function ellipse(img, cx, cy, rx, ry, color, opacity = 1, k = K) {
  return shape(img, (u, v) => ((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2 <= 1, color, opacity, k, [cx - rx, cy - ry, cx + rx, cy + ry]);
}

/** Soft radial glow/blush (alpha falls off smoothly to 0 at the ellipse border). */
export function blush(img, cx, cy, rx, ry, color, maxAlpha = 0.55, k = K) {
  return each(img, (u, v) => {
    const d = Math.sqrt(((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2);
    if (d >= 1) return null;
    const c = rgba(color);
    return [c[0], c[1], c[2], 255 * maxAlpha * (1 - d * d) ** 1.5];
  }, k);
}

/** Distance from point to segment. */
function segDist(px, py, ax, ay, bx, by) {
  const dx = bx - ax, dy = by - ay;
  const l2 = dx * dx + dy * dy;
  const t = l2 ? clamp01(((px - ax) * dx + (py - ay) * dy) / l2) : 0;
  return Math.hypot(px - ax - t * dx, py - ay - t * dy);
}

/** Anti-aliased polyline with round caps. pts in UV units, width in UV units. */
export function stroke(img, pts, width, color, opacity = 1, k = K) {
  const r = width / 2;
  const xs = pts.map((p) => p[0]), ys = pts.map((p) => p[1]);
  const b = [Math.min(...xs) - r, Math.min(...ys) - r, Math.max(...xs) + r, Math.max(...ys) + r];
  return shape(img, (u, v) => {
    for (let i = 0; i + 1 < pts.length; i++) if (segDist(u, v, ...pts[i], ...pts[i + 1]) <= r) return true;
    return pts.length === 1 && Math.hypot(u - pts[0][0], v - pts[0][1]) <= r;
  }, color, opacity, k, b);
}

/** Points along an arc (angles in degrees, 0 = +u, 90 = +v/down). */
export function arc(cx, cy, rx, ry, a0, a1, n = 10) {
  const out = [];
  for (let i = 0; i <= n; i++) {
    const a = ((a0 + ((a1 - a0) * i) / n) * Math.PI) / 180;
    out.push([cx + Math.cos(a) * rx, cy + Math.sin(a) * ry]);
  }
  return out;
}

/**
 * Big glossy cartoon eye: dark rim, deep iris gradient with a lighter lower crescent, a large soft highlight
 * top-left and a small sparkle bottom-right. look = [du, dv] shifts the pupil a little.
 */
export function eye(img, cx, cy, rx, ry, opts = {}) {
  const { iris = P.eyeIris, dark = P.eye, rimC = P.eyeRim, glow = "#5a4a86", k = K, mirror = false } = opts;
  ellipse(img, cx, cy, rx, ry, rimC, 1, k);
  ellipse(img, cx, cy, rx * 0.9, ry * 0.92, (u, v) => {
    const t = clamp01((v - (cy - ry)) / (2 * ry));
    return mix(dark, iris, t * t);
  }, 1, k);
  // lower crescent glow
  shape(img, (u, v) => ((u - cx) / (rx * 0.72)) ** 2 + ((v - cy - ry * 0.32) / (ry * 0.5)) ** 2 <= 1 && ((u - cx) / (rx * 0.8)) ** 2 + ((v - cy - ry * 0.05) / (ry * 0.55)) ** 2 > 1, glow, 0.85, k);
  const s = mirror ? -1 : 1;
  // main highlight (big, slightly tilted oval) + small sparkle
  ellipse(img, cx - s * rx * 0.34, cy - ry * 0.36, rx * 0.36, ry * 0.3, P.shine, 1, k);
  ellipse(img, cx + s * rx * 0.32, cy + ry * 0.36, rx * 0.15, ry * 0.13, P.shine, 0.95, k);
  ellipse(img, cx + s * rx * 0.05, cy - ry * 0.62, rx * 0.12, ry * 0.08, P.shine2, 0.7, k);
  return img;
}

/** Closed eye as a curve: "happy" (∩, upwards) or "sleep" (∪, downwards). */
export function closedEye(img, cx, cy, w, kind = "sleep", color = P.eye, thick = 0.42, k = K) {
  const pts = kind === "happy" ? arc(cx, cy + w * 0.25, w / 2, w * 0.45, 180, 360, 12) : arc(cx, cy - w * 0.15, w / 2, w * 0.38, 0, 180, 12);
  return stroke(img, pts, thick, color, 1, k);
}

// ------------------------------------------------------------------------------------------------ previews
const FRONT = { yaw: 32, pitch: 12 };

/**
 * Sideways follow distance of a ground pet in px, like the client (AvatarTicker.petSide): player radius 0.3 + the
 * widest body cube (cubes thinner than 2 px ignored) × scale + 0.3, at least 0.95 blocks.
 */
export function petSidePx(json) {
  const sc = json.cosmetic.pet?.scale ?? 1;
  let m = 0;
  for (const b of json.model?.bones ?? [])
    for (const c of b.cubes ?? []) {
      if (Math.abs(c.to[1] - c.from[1]) < 2 || Math.abs(c.to[2] - c.from[2]) < 2) continue;
      m = Math.max(m, Math.abs(c.from[0]), Math.abs(c.to[0]) + (c.inflate ?? 0));
    }
  const half = Math.min(1.5, Math.max(0.1, (m * sc) / 16));
  return Math.max(0.95, 0.3 + half + 0.3) * 16;
}

/** Preview panels for pets: close-ups of named moments (any state), plus the pet beside the player. */
export function petPreview(moments, opts = {}) {
  return {
    panels: (_, B) => {
      const sc = B.json.cosmetic.pet?.scale ?? 1;
      const side = petSidePx(B.json);
      const out = [{ label: "with player", cam: FRONT, player: true, worldOffset: [side, 0, 6], petScale: sc, anims: [{ name: "idle", t: moments[0].t }], time: moments[0].t }];
      for (const m of moments)
        out.push({
          label: m.label, ownFit: true, player: false, cam: m.cam ?? { yaw: 35, pitch: 14 }, petScale: sc,
          anims: m.anims ?? [{ name: m.anim ?? "idle", t: m.t }], time: m.t, fitParticles: true,
        });
      return out;
    },
    sheet: { cols: opts.cols ?? 4, panelW: 300, panelH: 300 },
    thumb: opts.thumb,
  };
}

/** Keyframe helper: hold a value between times [[t, v], …] → adds duplicate keys so catmull-rom stays flat. */
export function holds(list) {
  const out = [];
  for (const [t0, t1, v] of list) {
    out.push([t0, v]);
    if (t1 != null && t1 > t0) out.push([t1, v]);
  }
  return out.sort((a, b) => a[0] - b[0]);
}

// ------------------------------------------------------------------------------------------------ aurora
const smooth01 = (a, b, x) => {
  const t = clamp01((x - a) / (b - a));
  return t * t * (3 - 2 * t);
};

/**
 * Vertical aurora rays in [0, 1] at position `along` (UV units along a ribbon) and loop phase ph (0..1). A sum of
 * drifting sines: continuous along the ribbon and seamless in time (every term drifts a whole number of cycles per
 * loop), so animated strips loop without a jump.
 */
export function auroraRays(along, ph, seed = 1) {
  const T = [[0.11, 1, 1.0], [0.23, -2, 0.7], [0.41, 3, 0.45], [0.73, -4, 0.3]];
  let s = 0, n = 0;
  for (const [f, m, w] of T) {
    s += w * (0.5 + 0.5 * Math.sin(2 * Math.PI * (f * along + m * ph) + seed * 1.7 + f * 9));
    n += w;
  }
  return Math.pow(s / n, 1.7);
}

/**
 * One aurora texel: t = height in the curtain (0 bottom hem … 1 top), along = position along the ribbon (UV units),
 * ph = loop phase. Soft everywhere: the hem fades in, the light decays upwards and fades out completely at the top,
 * vertical rays drift, colour runs mint → teal → blue → violet → a pink hint at the top. env (0..1) fades the ribbon
 * ends. faces = 2 when both faces of a plane are drawn on top of each other (alpha is split so they add up).
 * Returns [r, g, b, a] or null (fully transparent).
 */
export function auroraTexel(t, along, ph, o = {}) {
  const { seed = 1, env = 1, maxA = 0.95, faces = 2 } = o;
  const hem = smooth01(0, 0.1, t);
  const body = Math.exp(-1.35 * Math.max(0, t - 0.1));
  const top = 1 - smooth01(0.78, 1, t);
  const rays = auroraRays(along, ph, seed);
  // slow large-scale breathing of the brightness along the ribbon
  const wave = 0.75 + 0.25 * Math.sin(2 * Math.PI * (along * 0.035 - ph) + seed);
  let a = maxA * hem * body * top * (0.32 + 0.68 * rays) * wave * env;
  if (a < 0.003) return null;
  let c = mix("#7dffc4", P.auroraT, smooth01(0.08, 0.45, t));
  c = mix(c, P.auroraB, smooth01(0.3, 0.58, t) * 0.85);
  c = mix(c, P.auroraP, smooth01(0.5, 0.82, t) * 0.9);
  c = mix(c, P.auroraM, smooth01(0.78, 1, t) * 0.6);
  // the bright lower edge
  const core = Math.max(0, 1 - Math.abs(t - 0.1) / 0.07);
  if (core > 0) c = mix(c, "#d6fff0", core * 0.45);
  if (faces === 2) a = 1 - Math.sqrt(1 - Math.min(0.98, a));
  const C = rgba(c);
  return [C[0], C[1], C[2], Math.round(255 * Math.min(1, a))];
}

/** smoothstep between a and b (exported for painters). */
export const smoothstep = smooth01;
