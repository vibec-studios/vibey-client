// Polar Pals · Aurora Ring – soft northern lights around you. Three thin, curved light ribbons at different radii and
// heights circle your hips. Each ribbon is a chain of narrow translucent, emissive segments whose texture is one
// continuous curtain: a bright mint hem that fades upwards through teal and blue into violet, vertical rays that drift
// along (12-frame interpolated strip) and soft alpha everywhere (no hard edges). The segments ripple with
// phase-shifted waves (radial fold, lift, lean, height), so the curtains flow; the whole aurora turns slowly, three
// small crystal snowflakes orbit and sparkles rise.
import { P, cosmetic, each, stroke, ellipse, auroraTexel, smoothstep } from "./_polar.mjs";

const K2 = 2; // texture pixels per UV unit (soft gradients need no more)
const FRAMES = 12;
const L = 16; // loop (s): one slow turn
const W = 3; // segment width (px)
const Z0 = [0, 0, 0];
const RIBBONS = [
  { name: "a", R: 13, Y: 1, H: 14, a0: 0, arc: 250, seed: 1 },
  { name: "b", R: 10, Y: 5, H: 10, a0: 150, arc: 190, seed: 2 },
  { name: "c", R: 16, Y: 0, H: 11, a0: 255, arc: 165, seed: 3 },
];

function flake(img) {
  each(img, () => [0, 0, 0, 0], K2);
  const c = 2.5;
  for (let k = 0; k < 6; k++) {
    const a = (k * Math.PI) / 3 + Math.PI / 6;
    const ex = c + Math.cos(a) * 2.3, ey = c + Math.sin(a) * 2.3;
    stroke(img, [[c, c], [ex, ey]], 0.5, "#e6fbff", 1, K2);
    const mx = c + Math.cos(a) * 1.5, my = c + Math.sin(a) * 1.5;
    for (const d of [-0.7, 0.7]) stroke(img, [[mx, my], [mx + Math.cos(a + d) * 0.7, my + Math.sin(a + d) * 0.7]], 0.4, P.tealLight, 1, K2);
  }
  ellipse(img, c, c, 0.6, 0.6, "#ffffff", 1, K2);
  return img;
}

export default function () {
  const b = cosmetic("polar_aurora_ring", {
    name: { en: "Polar Pals · Aurora Ring", de: "Polar Pals · Polarlicht-Ring" },
    description: {
      en: "Soft northern lights circle you: three thin ribbons of light ripple and fold, rays drift along them from mint to violet, crystal snowflakes orbit and sparkles rise.",
      de: "Sanftes Polarlicht umkreist dich: drei feine Lichtbänder wogen und falten sich, Strahlen wandern von Mint bis Violett an ihnen entlang, Kristall-Schneeflocken kreisen und Funken steigen auf.",
    },
    tags: ["polar-pals", "aura", "aurora", "glow", "animated"],
    rarity: "epic",
    cosmetic: { slot: "aura" },
    preview: {
      panels: (p) => [
        { ...p[0], label: "front", bloom: 0.6 },
        { ...p[1], label: "back (later)", bloom: 0.6, time: 5.3, anims: [{ name: "idle", t: 5.3 }] },
        { ...p[2], label: "top", bloom: 0.6 },
      ],
      sheet: { cols: 3, panelW: 320, panelH: 340 },
      thumb: { cam: { yaw: 30, pitch: 18 }, bloom: 0.8 },
    },
  });
  b.texture("main", { hd: K2, frames: FRAMES, frameTime: 4, interpolate: true });
  const tex = b.textures.get("main");
  const spark = tex.region(3, 3, (img) => {
    each(img, () => [0, 0, 0, 0], K2);
    stroke(img, [[1.5, 0.3], [1.5, 2.7]], 0.4, "#eafff8", 1, K2);
    stroke(img, [[0.3, 1.5], [2.7, 1.5]], 0.4, "#eafff8", 1, K2);
    ellipse(img, 1.5, 1.5, 0.5, 0.5, "#ffffff", 1, K2);
    return img;
  });
  b.sprite("spark", "main", spark);

  b.bone("ring", { attach: "world", pivot: [0, 0, 0], emissive: true, translucent: true });
  const segs = [];
  for (const r of RIBBONS) {
    const dTheta = (2 * Math.atan(W / 2 / r.R) * 180) / Math.PI; // planes tangent at R meet exactly edge to edge
    const n = Math.round(r.arc / dTheta);
    const total = n * W;
    b.bone(`rib_${r.name}`, { parent: "ring", pivot: [0, 0, 0], emissive: true, translucent: true });
    for (let i = 0; i < n; i++) {
      const seg = b.bone(`${r.name}${i}`, { parent: `rib_${r.name}`, pivot: [0, r.Y, 0], rotation: [0, r.a0 + i * dTheta, 0], emissive: true, translucent: true });
      const fold = b.bone(`${r.name}${i}f`, { parent: seg.name, pivot: [0, r.Y, r.R], emissive: true, translucent: true });
      // both faces carry the same curtain (consistent in world space), each with half the opacity
      const paint = (face) => (img, frame) => {
        const ph = frame / FRAMES;
        return each(img, (u, v) => {
          const along = i * W + (face === "south" ? u : W - u);
          const env = smoothstep(0, 7, along) * smoothstep(0, 7, total - along);
          return auroraTexel(1 - v / r.H, along, ph, { seed: r.seed, env }) ?? [0, 0, 0, 0];
        }, K2);
      };
      fold.box([-W / 2, r.Y, r.R], [W, r.H, 0], { north: paint("north"), south: paint("south") });
      segs.push({ r, i, n, name: fold.name });
      if (i === Math.round(n / 2) || i === Math.round(n / 4)) b.locator(`s_${r.name}${i}`, fold.name, [0, r.Y + 2, r.R]);
    }
  }
  b.bone("flakes", { attach: "world", pivot: [0, 0, 0], emissive: true });
  for (let k = 0; k < 3; k++) {
    const a = (k * 2 * Math.PI) / 3;
    const x = Math.sin(a) * 8.5, z = Math.cos(a) * 8.5, y = 16 + k * 2.5;
    const fb = b.bone(`flake${k}`, { parent: "flakes", pivot: [x, y, z], emissive: true });
    fb.box([x - 2.5, y - 2.5, z], [5, 5, 0], { north: flake, south: flake });
  }

  const idle = b.anim("idle", L, true);
  idle.rot("ring", [[0, Z0], [L, [0, -360, 0]]], "linear");
  idle.rot("flakes", [[0, Z0], [L, [0, 720, 0]]], "linear");
  for (let k = 0; k < 3; k++) {
    idle.rot(`flake${k}`, [[0, Z0], [L, [0, 0, 360 * (k % 2 ? 1 : -1)]]], "linear");
    const bob = [];
    for (let s = 0; s <= L; s += 2) bob.push([s, [0, Math.sin((s / 4) * Math.PI * 2 + k * 2) * 1.2, 0]]);
    idle.pos(`flake${k}`, bob);
  }
  // the flow: every segment folds in/out, lifts, leans and stretches with phase-shifted waves along its ribbon
  for (const { r, i, name } of segs) {
    const pos = [], rot = [], sc = [];
    for (let t = 0; t <= L; t += 1) {
      const ph = (t / L) * Math.PI * 2;
      const fold = 2 * ph - i * 0.42 + r.seed;
      const lift = 3 * ph - i * 0.3 + r.seed * 2;
      const grow = 2 * ph - i * 0.25 + r.seed * 3;
      pos.push([t, [0, Math.sin(lift) * 0.9, Math.sin(fold) * 1.6]]);
      rot.push([t, [Math.cos(fold) * 7, 0, 0]]);
      sc.push([t, [1, 1 + Math.sin(grow) * 0.14, 1]]);
    }
    idle.pos(name, pos);
    idle.rot(name, rot);
    idle.scale(name, sc);
  }
  const locs = Object.keys(b.locators);
  for (let s = 0; s < 8; s++)
    idle.particle(s * 2, { sprite: "spark", locator: locs[s % locs.length], count: 1, lifetime: 1.8, size: 0.1, spread: [0.2, 0.15, 0.2], velocity: [0, 0.03, 0], gravity: -0.0004, emissive: true });
  return b;
}
