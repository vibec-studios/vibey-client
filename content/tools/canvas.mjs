// Tiny pixel canvas for procedural textures (in the spirit of vibe-launcher's seal3d.ts painters).
import { encodePNG } from "./png.mjs";

/** Deterministic LCG, same as seal3d.ts. Returns floats in [0, 1). */
export function rng(seed) {
  let s = seed >>> 0;
  return () => (s = (Math.imul(s, 1664525) + 1013904223) >>> 0) / 4294967296;
}

/** Hash a string to a 32-bit seed. */
export function seedOf(str) {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) h = Math.imul(h ^ str.charCodeAt(i), 16777619) >>> 0;
  return h;
}

/** Parse "#rgb", "#rrggbb", "#rrggbbaa" or [r,g,b,a?] into [r,g,b,a]. */
export function rgba(c) {
  if (Array.isArray(c)) return [c[0], c[1], c[2], c.length > 3 ? c[3] : 255];
  if (c == null || c === "transparent") return [0, 0, 0, 0];
  let s = c.replace("#", "");
  if (s.length === 3) s = s.split("").map((ch) => ch + ch).join("");
  const n = parseInt(s.slice(0, 6), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255, s.length >= 8 ? parseInt(s.slice(6, 8), 16) : 255];
}

const clamp255 = (v) => (v < 0 ? 0 : v > 255 ? 255 : Math.round(v));

export function hex(c) {
  const [r, g, b, a] = rgba(c);
  const h = (v) => clamp255(v).toString(16).padStart(2, "0");
  return "#" + h(r) + h(g) + h(b) + (a !== 255 ? h(a) : "");
}

/** Linear mix of two colours (t = 0 → a, 1 → b), alpha mixed too. */
export function mix(a, b, t) {
  const A = rgba(a), B = rgba(b);
  return A.map((v, i) => clamp255(v + (B[i] - v) * t));
}

/** Multiply RGB by f (keeps alpha). */
export function shade(c, f) {
  const [r, g, b, a] = rgba(c);
  return [clamp255(r * f), clamp255(g * f), clamp255(b * f), a];
}

export function withAlpha(c, a) {
  const [r, g, b] = rgba(c);
  return [r, g, b, clamp255(a)];
}

/** RGBA image, optionally a clipped view into a parent image (shares pixels). */
export class Img {
  constructor(width, height, data = null, ox = 0, oy = 0, root = null) {
    this.width = width;
    this.height = height;
    this.root = root ?? this;
    this.ox = ox;
    this.oy = oy;
    if (!root) {
      this.data = data ?? new Uint8ClampedArray(width * height * 4);
      this.stride = width;
    }
  }
  get w() { return this.width; }
  get h() { return this.height; }

  /** A sub-rectangle sharing pixels; coordinates inside are local and clipped. */
  view(x, y, w, h) {
    return new Img(w, h, null, this.ox + x, this.oy + y, this.root);
  }

  _idx(x, y) {
    x = Math.floor(x);
    y = Math.floor(y);
    if (x < 0 || y < 0 || x >= this.width || y >= this.height) return -1;
    const R = this.root;
    const gx = x + this.ox, gy = y + this.oy;
    if (gx < 0 || gy < 0 || gx >= R.width || gy >= R.height) return -1;
    return (gy * R.width + gx) * 4;
  }

  get(x, y) {
    const i = this._idx(x, y);
    if (i < 0) return [0, 0, 0, 0];
    const d = this.root.data;
    return [d[i], d[i + 1], d[i + 2], d[i + 3]];
  }

  /** Set a pixel (replaces, no blending). */
  set(x, y, c) {
    const i = this._idx(x, y);
    if (i < 0) return this;
    const [r, g, b, a] = rgba(c);
    const d = this.root.data;
    d[i] = r; d[i + 1] = g; d[i + 2] = b; d[i + 3] = a;
    return this;
  }

  /** Alpha-blend a colour over a pixel ("source over"). */
  blend(x, y, c, opacity = 1) {
    const i = this._idx(x, y);
    if (i < 0) return this;
    const [r, g, b, a0] = rgba(c);
    const a = (a0 / 255) * opacity;
    if (a <= 0) return this;
    const d = this.root.data;
    const da = d[i + 3] / 255;
    const oa = a + da * (1 - a);
    if (oa <= 0) return this;
    d[i] = clamp255((r * a + d[i] * da * (1 - a)) / oa);
    d[i + 1] = clamp255((g * a + d[i + 1] * da * (1 - a)) / oa);
    d[i + 2] = clamp255((b * a + d[i + 2] * da * (1 - a)) / oa);
    d[i + 3] = clamp255(oa * 255);
    return this;
  }

  fill(c) { return this.rect(0, 0, this.width, this.height, c); }

  rect(x, y, w, h, c) {
    for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) this.set(x + i, y + j, c);
    return this;
  }

  /** Base colour with subtle per-pixel brightness noise (like seal3d.ts `noisy`). */
  noisy(c, amount = 0.02, seed = 1, x = 0, y = 0, w = this.width, h = this.height) {
    const r = rng(seed);
    const [R, G, B, A] = rgba(c);
    for (let j = 0; j < h; j++)
      for (let i = 0; i < w; i++) {
        const f = 1 + (r() - 0.5) * 2 * amount;
        this.set(x + i, y + j, [R * f, G * f, B * f, A]);
      }
    return this;
  }

  /** Per-pixel colour from a function (x, y) → colour | null (null = untouched). */
  each(fn) {
    for (let y = 0; y < this.height; y++)
      for (let x = 0; x < this.width; x++) {
        const c = fn(x, y, this.get(x, y));
        if (c != null) this.set(x, y, c);
      }
    return this;
  }

  px(c, list) {
    for (const [x, y] of list) this.set(x, y, c);
    return this;
  }

  row(y, c, x0 = 0, x1 = this.width - 1) {
    for (let x = x0; x <= x1; x++) this.set(x, y, c);
    return this;
  }

  col(x, c, y0 = 0, y1 = this.height - 1) {
    for (let y = y0; y <= y1; y++) this.set(x, y, c);
    return this;
  }

  /** 1px outline of the view/rectangle. */
  border(c, x = 0, y = 0, w = this.width, h = this.height) {
    this.row(y, c, x, x + w - 1).row(y + h - 1, c, x, x + w - 1);
    this.col(x, c, y, y + h - 1).col(x + w - 1, c, y, y + h - 1);
    return this;
  }

  line(x0, y0, x1, y1, c) {
    x0 = Math.round(x0); y0 = Math.round(y0); x1 = Math.round(x1); y1 = Math.round(y1);
    const dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
    const sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
    let err = dx + dy;
    for (;;) {
      this.set(x0, y0, c);
      if (x0 === x1 && y0 === y1) break;
      const e2 = 2 * err;
      if (e2 >= dy) { err += dy; x0 += sx; }
      if (e2 <= dx) { err += dx; y0 += sy; }
    }
    return this;
  }

  /**
   * Stamp an ASCII pattern. Each character is looked up in `colors` (missing / "." / " " = skip).
   * Rows can be an array of strings or one multi-line string.
   */
  stamp(x, y, rows, colors) {
    if (typeof rows === "string") rows = rows.replace(/^\n/, "").replace(/\n\s*$/, "").split("\n").map((r) => r.trim());
    rows.forEach((row, j) => {
      [...row].forEach((ch, i) => {
        const c = colors[ch];
        if (c != null) this.set(x + i, y + j, c);
      });
    });
    return this;
  }

  /** Copy (replace) another image into this one. */
  draw(img, x = 0, y = 0, blend = false) {
    for (let j = 0; j < img.height; j++)
      for (let i = 0; i < img.width; i++) {
        const c = img.get(i, j);
        if (blend) this.blend(x + i, y + j, c);
        else this.set(x + i, y + j, c);
      }
    return this;
  }

  /** Mirror the left half onto the right half (for symmetric designs). */
  mirrorX() {
    const w = this.width;
    for (let y = 0; y < this.height; y++) for (let x = 0; x < w >> 1; x++) this.set(w - 1 - x, y, this.get(x, y));
    return this;
  }

  clone() {
    const out = new Img(this.width, this.height);
    out.draw(this);
    return out;
  }

  /** Raw RGBA bytes of this image/view. */
  pixels() {
    if (this.root === this) return this.data;
    const out = new Uint8ClampedArray(this.width * this.height * 4);
    for (let y = 0; y < this.height; y++)
      for (let x = 0; x < this.width; x++) out.set(this.get(x, y), (y * this.width + x) * 4);
    return out;
  }

  toPNG() {
    const px = this.pixels();
    return encodePNG(this.width, this.height, Buffer.from(px.buffer, px.byteOffset, this.width * this.height * 4));
  }

  static fromRGBA(width, height, data) {
    return new Img(width, height, new Uint8ClampedArray(data));
  }
}
