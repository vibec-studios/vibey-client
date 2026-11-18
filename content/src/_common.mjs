// Shared palette + texture layout helpers for the starter set.
import { Bundle } from "../tools/vibey.mjs";
import { Img, mix, shade, rgba, rng, hex } from "../tools/canvas.mjs";
import { readFileSync } from "node:fs";

/** Bytes of an original synthesized sound in content/sounds (see tools/synth.mjs). */
export const soundFile = (name) => readFileSync(new URL(`../sounds/${name}.ogg`, import.meta.url));

export { Bundle, Img, mix, shade, rgba, rng, hex };

/** Vibe design tokens (docs/SPEC.md §4) + seal colours (vibe-launcher seal3d.ts) + a few theme colours. */
export const C = {
  // Vibe UI
  bg: "#000000", surface: "#0b0b0e", surface2: "#121217", surface3: "#18181f", panel: "#08080b",
  text: "#f4f4f5", muted: "#a1a1aa", dim: "#6b6b75",
  brand: "#8b6cff", teal: "#3fe0c5", danger: "#ff5470", warn: "#ffb547",
  // derived purples / teals
  brandDeep: "#4a2fc0", brandDark: "#33208c", brandNight: "#1d1452", brandLight: "#b6a3ff", brandPale: "#dcd3ff",
  tealDark: "#1f9e8a", tealDeep: "#126b5e", tealLight: "#9af3e3",
  // seal
  body: "#8f9aa6", back: "#7d8894", belly: "#cdd4db", spot: "#68737f", snout: "#e8ecf0", flipper: "#707b87",
  eye: "#141418", shine: "#ffffff", lid: "#7d8894", nose: "#2a2a30", whisker: "#a3acb5", cheek: "#ff9db8",
  // ice
  ice: "#e3f0f9", iceLight: "#f6fbff", iceSide: "#bcd9ee", iceDark: "#9cc4e2", iceDeep: "#5f97c7", iceNight: "#1d3b5c",
};

/** Seal face (front view) as an ASCII stamp – original pixel art based on the Vibe seal. */
export const SEAL_FACE = {
  rows: [
    ".######.",
    "########",
    "#ew##ew#",
    "#ee##ee#",
    "c#ssss#c",
    "##snns##",
    ".#ssss#.",
  ],
  colors: { "#": C.body, e: C.eye, w: C.shine, s: C.snout, n: C.nose, c: C.cheek },
};

/** Vanilla cape texture layout (64×32): outer = side seen from behind, inner = side facing the back. */
export function capeLayout(img) {
  return {
    outer: img.view(1, 1, 10, 16),
    inner: img.view(12, 1, 10, 16),
    top: img.view(1, 0, 10, 1),
    bottom: img.view(11, 0, 10, 1),
    left: img.view(0, 1, 1, 16),
    right: img.view(11, 1, 1, 16),
    // elytra (vanilla elytra uses the cape texture): wing faces 10×20 at (24,2) outer / (36,2) inner
    elytraOuter: img.view(24, 2, 10, 20),
    elytraInner: img.view(36, 2, 10, 20),
    elytraEdges: [img.view(22, 2, 2, 20), img.view(34, 2, 2, 20), img.view(24, 0, 10, 2), img.view(34, 0, 10, 2)],
  };
}

/** Vanilla shield texture layout (64×64). */
export function shieldLayout(img) {
  return {
    front: img.view(1, 1, 12, 22),
    back: img.view(14, 1, 12, 22),
    top: img.view(1, 0, 12, 1),
    bottom: img.view(13, 0, 12, 1),
    left: img.view(0, 1, 1, 22),
    right: img.view(13, 1, 1, 22),
    handle: img.view(26, 0, 16, 12),
  };
}

/** Standard wooden-ish back side + handle for shields, tinted. */
export function paintShieldBack(L, base = "#3a2f5e", seed = 7) {
  L.back.noisy(base, 0.04, seed);
  for (let x = 0; x < 12; x += 4) L.back.col(x, shade(base, 0.82));
  L.back.border(shade(base, 0.7));
  L.back.rect(0, 10, 12, 2, shade(base, 0.7));
  for (const v of [L.top, L.bottom, L.left, L.right]) v.noisy(shade(base, 0.85), 0.03, seed + 1);
  L.handle.noisy("#2a2233", 0.04, seed + 2);
}

/** Vertical gradient fill with subtle noise. */
export function gradient(img, top, bottom, amount = 0.02, seed = 1) {
  const r = rng(seed);
  for (let y = 0; y < img.height; y++)
    for (let x = 0; x < img.width; x++) {
      const c = mix(top, bottom, img.height > 1 ? y / (img.height - 1) : 0);
      const f = 1 + (r() - 0.5) * 2 * amount;
      img.set(x, y, [c[0] * f, c[1] * f, c[2] * f, c[3]]);
    }
  return img;
}

/** Shorthand: create a cosmetic bundle with the shared defaults. */
export function cosmetic(id, meta) {
  return new Bundle({ kind: "cosmetic", id: `vibey:${id}`, ...meta });
}

/** Emote bundle with the shared defaults; the animation is always called "main". */
export function emote(id, meta, opts = {}) {
  return new Bundle({
    kind: "emote",
    id: `vibey:${id}`,
    rarity: "common",
    ...meta,
    emote: { animation: "main", loop: !!opts.loop, thirdPerson: true, stopOnMove: opts.stopOnMove ?? true, hideHeldItems: opts.hideHeldItems ?? true },
  });
}

/** Hands: invisible helper bones on the arms for locators (tip of each hand). */
export function addHands(b) {
  b.bone("right_hand", { attach: "right_arm", pivot: [5, 22, 0] });
  b.bone("left_hand", { attach: "left_arm", pivot: [-5, 22, 0] });
  b.locator("right_hand", "right_hand", [6, 12, 0]);
  b.locator("left_hand", "left_hand", [-6, 12, 0]);
}

const DEG = 180 / Math.PI;
/**
 * Spec rotation [rx, ry, 0] that points an arm (or leg) hanging from `from` towards `to` (both in Blockbench
 * coords). Derived from the spec conversion (model dir = (−x, −y, z); vanilla xRot = −rx, yRot = −ry, order ZYX).
 */
export function aim(from, to) {
  let d = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
  const l = Math.hypot(...d);
  d = d.map((v) => v / l);
  const mx = -d[0], my = -d[1], mz = d[2];
  const xr = -Math.acos(Math.max(-1, Math.min(1, my)));
  const sx = Math.sin(xr);
  const yr = Math.abs(sx) < 1e-6 ? 0 : Math.atan2(mx / sx, mz / sx);
  return [+(-xr * DEG).toFixed(1), +(-yr * DEG).toFixed(1), 0];
}
export const SHOULDER = { right: [5, 22, 0], left: [-5, 22, 0] };
