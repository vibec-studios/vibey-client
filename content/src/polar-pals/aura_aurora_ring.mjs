// Polar Pals · Aurora Ring – a glowing band of northern lights circles your hips. Twelve curtain panels ripple up and
// down as a wave runs around the ring, the light drifts along them (animated strip), three crystal snowflakes orbit
// the other way and sparkles rise from the ring.
import { K, P, cosmetic, each, stroke, ellipse, mix, rgba, vnoise } from "./_polar.mjs";

const FRAMES = 8;
const N = 12, R = 12.5, W = 7, H = 7, Y = 4;
const L = 4; // wave period
const Z0 = [0, 0, 0];

function panel(img, frame, i) {
  const w = img.width / K, h = img.height / K;
  const ph = frame / FRAMES;
  each(img, () => [0, 0, 0, 0]);
  each(img, (u, v) => {
    const along = i * W + u; // continuous around the ring
    const t = 1 - v / h; // 0 bottom … 1 top
    const ray = vnoise(along * 0.8 - ph * 6.4, t * 0.7, 21) * 0.6 + vnoise(along * 1.9 + ph * 12.8, t, 22) * 0.4;
    let c = mix(P.auroraG, P.auroraT, Math.min(1, t * 2));
    c = mix(c, t > 0.5 ? P.auroraP : P.auroraB, Math.max(0, (t - 0.3) / 0.7));
    if (t > 0.8) c = mix(c, P.auroraM, (t - 0.8) / 0.2 * 0.6);
    const hem = Math.max(0, 1 - (v - (h - 0.8)) / 0.8);
    let a = (0.12 + 0.88 * ray ** 1.5) * (1 - t * 0.7) * hem;
    if (v > h - 0.5) c = mix(c, "#eafff8", 0.55);
    if (a < 0.04) return null;
    const C = rgba(c);
    return [C[0], C[1], C[2], Math.round(255 * Math.min(0.9, a))];
  });
  return img;
}

function flake(img) {
  each(img, () => [0, 0, 0, 0]);
  const c = 2.5;
  for (let k = 0; k < 6; k++) {
    const a = (k * Math.PI) / 3;
    const ex = c + Math.cos(a) * 2.3, ey = c + Math.sin(a) * 2.3;
    stroke(img, [[c, c], [ex, ey]], 0.32, "#eafcff");
    const mx = c + Math.cos(a) * 1.4, my = c + Math.sin(a) * 1.4;
    for (const d of [-0.6, 0.6]) stroke(img, [[mx, my], [mx + Math.cos(a + d) * 0.7, my + Math.sin(a + d) * 0.7]], 0.22, P.tealLight);
  }
  ellipse(img, c, c, 0.5, 0.5, "#ffffff");
  return img;
}

export default function () {
  const b = cosmetic("polar_aurora_ring", {
    name: { en: "Polar Pals · Aurora Ring", de: "Polar Pals · Polarlicht-Ring" },
    description: {
      en: "A glowing ring of northern lights circles you. A wave ripples around it, the light drifts along the curtains, crystal snowflakes orbit and sparkles rise.",
      de: "Ein leuchtender Ring aus Polarlicht umkreist dich. Eine Welle läuft um ihn herum, das Licht wandert über die Vorhänge, Kristall-Schneeflocken kreisen und Funken steigen auf.",
    },
    tags: ["polar-pals", "aura", "aurora", "glow", "animated"],
    rarity: "epic",
    cosmetic: { slot: "aura" },
    preview: {
      panels: (p) => [
        { ...p[0], label: "front", bloom: 0.5 },
        { ...p[1], label: "back (later)", bloom: 0.5 },
        { ...p[2], label: "top", bloom: 0.5 },
      ],
      sheet: { cols: 3, panelW: 320, panelH: 340 },
      thumb: { cam: { yaw: 30, pitch: 22 }, bloom: 0.6 },
    },
  });
  b.texture("main", { hd: K, frames: FRAMES, frameTime: 2 });
  const tex = b.textures.get("main");
  const spark = tex.region(3, 3, (img) => {
    each(img, () => [0, 0, 0, 0]);
    stroke(img, [[1.5, 0.2], [1.5, 2.8]], 0.32, "#eafff8");
    stroke(img, [[0.2, 1.5], [2.8, 1.5]], 0.32, "#eafff8");
    ellipse(img, 1.5, 1.5, 0.5, 0.5, "#ffffff");
    return img;
  });
  b.sprite("spark", "main", spark);

  b.bone("ring", { attach: "world", pivot: [0, 0, 0], emissive: true, translucent: true });
  for (let i = 0; i < N; i++) {
    const bone = b.bone(`panel${i}`, { parent: "ring", pivot: [0, Y, 0], rotation: [0, (360 / N) * i, 0], emissive: true, translucent: true });
    bone.box([-W / 2, Y, R], [W, H, 0], { north: (img, f) => panel(img, f, i), south: (img, f) => panel(img, f, i) });
    if (i % 3 === 0) b.locator(`p${i}`, `panel${i}`, [0, Y + 1, R]);
  }
  b.bone("flakes", { attach: "world", pivot: [0, 0, 0], emissive: true });
  for (let k = 0; k < 3; k++) {
    const a = (k * 2 * Math.PI) / 3;
    const x = Math.sin(a) * 9.5, z = Math.cos(a) * 9.5, y = 15 + k * 3;
    const fb = b.bone(`flake${k}`, { parent: "flakes", pivot: [x, y, z], emissive: true });
    fb.box([x - 2.5, y - 2.5, z], [5, 5, 0], { north: flake, south: flake });
  }

  const idle = b.anim("idle", 12, true);
  idle.rot("ring", [[0, Z0], [12, [0, 360, 0]]], "linear");
  idle.rot("flakes", [[0, Z0], [12, [0, -720, 0]]], "linear");
  for (let k = 0; k < 3; k++) {
    idle.rot(`flake${k}`, [[0, Z0], [12, [0, 0, 360 * (k % 2 ? 1 : -1)]]], "linear");
    const bob = [];
    for (let s = 0; s <= 12; s++) bob.push([s, [0, Math.sin((s / 3) * Math.PI * 2 + k * 2) * 1.5, 0]]);
    idle.pos(`flake${k}`, bob);
  }
  // the wave: each panel bobs and leans, phase-shifted around the ring (3 full waves per 12 s loop)
  for (let i = 0; i < N; i++) {
    const pos = [], rot = [];
    for (let s = 0; s <= 24; s++) {
      const t = s / 2;
      const ph = (t / L) * Math.PI * 2 - (i / N) * Math.PI * 4;
      pos.push([t, [0, Math.sin(ph) * 2.2, 0]]);
      rot.push([t, [Math.cos(ph) * 10, 0, 0]]);
    }
    idle.pos(`panel${i}`, pos);
    idle.rot(`panel${i}`, rot);
  }
  for (let s = 0; s < 8; s++)
    idle.particle(s * 1.5, { sprite: "spark", locator: `p${(s % 4) * 3}`, count: 2, lifetime: 1.6, size: 0.12, spread: [0.15, 0.1, 0.15], velocity: [0, 0.035, 0], gravity: -0.0005, emissive: true });
  return b;
}
