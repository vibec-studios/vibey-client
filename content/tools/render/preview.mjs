// Preview sheets + catalog thumbnails for bundles.
import { Img, mix, rgba } from "../canvas.mjs";
import { drawText, textWidth } from "./font.mjs";
import { loadBundle, buildScene, camera, sceneBounds, fitView, render } from "./renderer.mjs";

export const UI = {
  bg: "#0b0b0e",
  bg2: "#121217",
  panel: "#08080b",
  line: "#22222a",
  text: "#f4f4f5",
  muted: "#a1a1aa",
  dim: "#6b6b75",
  brand: "#8b6cff",
  teal: "#3fe0c5",
};
const RARITY = { common: "#a1a1aa", rare: "#3fe0c5", epic: "#8b6cff", legendary: "#ffb547" };

function background(w, h) {
  const img = new Img(w, h);
  for (let y = 0; y < h; y++) {
    const c = mix(UI.bg2, UI.bg, y / h);
    img.row(y, c);
  }
  // subtle pixel grid
  for (let y = 8; y < h; y += 16) for (let x = 8; x < w; x += 16) img.blend(x, y, "#ffffff", 0.05);
  return img;
}

function shadow(img, cx, cy, rx, ry) {
  for (let y = Math.floor(cy - ry); y <= cy + ry; y++)
    for (let x = Math.floor(cx - rx); x <= cx + rx; x++) {
      const d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2;
      if (d < 1) img.blend(x, y, "#000000", 0.45 * (1 - d));
    }
}

function compose(dst, src, x, y) {
  for (let j = 0; j < src.height; j++)
    for (let i = 0; i < src.width; i++) {
      const c = src.get(i, j);
      if (c[3]) dst.blend(x + i, y + j, c);
    }
}

/** Render one panel spec to { img, feet } with a given view. */
function renderPanel(B, p, size, view) {
  const scene = buildScene(B, p);
  const cam = camera(p.cam.yaw, p.cam.pitch);
  const img = render(scene, { width: size[0], height: size[1], cam, view, ss: p.ss ?? 3, bloom: p.bloom });
  return img;
}

function panelView(B, p) {
  if (p.texture) return null;
  const scene = buildScene(B, p);
  const cam = camera(p.cam.yaw, p.cam.pitch);
  if (p.focusHead) scene.quads = scene.quads.filter((q) => q.pts.every((pt) => pt.p[1] >= 23.5));
  return { b: sceneBounds(scene, cam), cam, own: !!(p.focusHead || p.ownFit) };
}

/**
 * Draw panels side by side with a header. panels: [{ label, cam, player, anims, time, worldOffset, petScale }]
 */
export function sheet(B, title, panels, opts = {}) {
  const pw = opts.panelW ?? 300, ph = opts.panelH ?? 340;
  const cols = opts.cols ?? panels.length;
  const rows = Math.ceil(panels.length / cols);
  const head = 46, gap = 8, pad = 12;
  const W = pad * 2 + cols * pw + (cols - 1) * gap;
  const H = head + pad + rows * ph + (rows - 1) * gap + pad;
  const img = background(W, H);
  // header
  const j = B.json;
  drawText(img, pad, 12, j.name.en, UI.text, 2);
  const sub = `${j.id}  ·  ${j.kind === "emote" ? "emote" : j.cosmetic.slot}  ·  ${j.name.de}`;
  drawText(img, pad, 32, sub, UI.muted, 1);
  if (j.rarity) {
    const rw = textWidth(j.rarity, 1) + 10;
    img.rect(W - pad - rw, 12, rw, 13, mix(RARITY[j.rarity], UI.bg, 0.75)).border(RARITY[j.rarity], W - pad - rw, 12, rw, 13);
    drawText(img, W - pad - rw + 5, 15, j.rarity, RARITY[j.rarity], 1);
  }
  // shared scale across panels so sizes are comparable
  const margin = opts.margin ?? 22;
  const views = panels.map((p) => panelView(B, p));
  let scale = Infinity;
  const shared = views.filter((v) => v && !v.own);
  if (opts.sharedBounds) {
    const u = { x0: Infinity, y0: Infinity, x1: -Infinity, y1: -Infinity };
    for (const v of shared) { u.x0 = Math.min(u.x0, v.b.x0); u.y0 = Math.min(u.y0, v.b.y0); u.x1 = Math.max(u.x1, v.b.x1); u.y1 = Math.max(u.y1, v.b.y1); }
    shared.forEach((v) => (v.b = u));
  }
  for (const v of shared) scale = Math.min(scale, fitView(v.b, pw, ph - 18, margin, opts.maxScale ?? 9).scale);
  panels.forEach((p, i) => {
    const x = pad + (i % cols) * (pw + gap), y = head + pad + Math.floor(i / cols) * (ph + gap);
    img.rect(x, y, pw, ph, mix(UI.panel, UI.bg2, 0.35)).border(UI.line, x, y, pw, ph);
    const v = views[i];
    if (!v) {
      const t = B.tex.get(p.texture);
      let ts = p.textureScale ?? 4;
      while (ts > 1 && (t.width * ts > pw - 16 || t.height * ts > ph - 34)) ts--;
      const card = textureCard(t, ts);
      compose(img, card, x + Math.floor((pw - card.width) / 2), y + Math.floor((ph - 18 - card.height) / 2));
      img.rect(x + 1, y + ph - 18, pw - 2, 17, [18, 18, 23, 255]);
      drawText(img, x + 8, y + ph - 13, `${p.label} ${t.width}x${t.height}${t.frames > 1 ? ` · ${t.frames} frames` : ""}`, UI.muted, 1);
      return;
    }
    const pscale = v.own ? fitView(v.b, pw, ph - 18, margin, 30).scale : scale;
    const view = { scale: pscale, cx: (v.b.x0 + v.b.x1) / 2, cy: (v.b.y0 + v.b.y1) / 2 };
    // floor shadow at the feet
    if (p.shadow !== false && !v.own) {
      const fy = (-(0) * 0 + 0); // world origin projects to (0, 0) in view space
      const sx = x + pw / 2 + (0 - view.cx) * scale, sy = y + (ph - 18) / 2 + (fy - view.cy) * scale;
      shadow(img, sx, sy, 9 * pscale, 3 * pscale);
    }
    const r = renderPanel(B, p, [pw, ph - 18], view);
    compose(img, r, x, y);
    if (p.label) {
      img.rect(x + 1, y + ph - 18, pw - 2, 17, [18, 18, 23, 255]);
      drawText(img, x + 8, y + ph - 13, p.label, p.accent ?? UI.muted, 1);
    }
  });
  return img;
}

/** 256×256 transparent catalog thumbnail. */
export function thumbnail(B, p, size = 256) {
  const scene = buildScene(B, p);
  const cam = camera(p.cam.yaw, p.cam.pitch);
  const view = fitView(sceneBounds(scene, cam), size, size, p.margin ?? 18, p.maxScale ?? 14);
  return render(scene, { width: size, height: size, cam, view, ss: 4, bloom: p.bloom ?? 0.5 });
}

/** Flat texture view (scaled nearest) with a checkerboard for transparency. */
export function textureCard(tex, scale, frame = 0, crop = null) {
  const [cx, cy, cw, ch] = crop ?? [0, 0, tex.width, tex.height];
  const img = new Img(cw * scale, ch * scale);
  for (let y = 0; y < ch * scale; y++)
    for (let x = 0; x < cw * scale; x++) {
      const chk = ((x >> 3) + (y >> 3)) & 1 ? "#1d1d24" : "#16161c";
      img.set(x, y, chk);
      const c = tex.img.get(cx + Math.floor(x / scale), cy + Math.floor(y / scale) + frame * tex.height);
      if (c[3]) img.blend(x, y, c);
    }
  return img;
}

export { loadBundle, compose, background };
