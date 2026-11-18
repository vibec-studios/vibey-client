// Polar Pals · Aurora Veil Wings – three glowing curtains of northern light per side, each a chain of three segments
// that ripple like cloth. The texture is an 8-frame strip, so the light streaks drift along the veils; sparkle
// sprites trail from the tips (a few at rest, a stream while flying). sneak folds the veils in.
import { K, P, cosmetic, each, stroke, ellipse, mix, rgba, vnoise } from "./_polar.mjs";

const FRAMES = 8;
const Z0 = [0, 0, 0];
// two veils per side like a butterfly: a big upper curtain and a smaller lower one; [length, height] per segment
const VEILS = [
  { name: "up", ang: 30, seed: 3, seg: [[6, 12], [6, 11], [6, 9]] },
  { name: "low", ang: -34, seed: 11, seg: [[5, 8], [5, 7], [4, 6]] },
];

/**
 * Aurora curtain: vertical rays (bright at the lower hem, fading upwards like real aurora), colour bands
 * green → teal → blue/purple, a pink fringe on top. seg = segment index (0 root … 2 tip), frame = shimmer phase.
 */
function curtain(img, frame, seg, seed, tipStart, total) {
  const W = img.width / K, H = img.height / K;
  const ph = frame / FRAMES;
  each(img, () => [0, 0, 0, 0]);
  each(img, (u, v) => {
    const along = (tipStart ? W - u : u) + seg; // seg = offset of this segment; 0 at the root … total at the tip
    const t = 1 - v / H; // 0 hem (bottom) … 1 top
    // vertical rays drifting outwards over the frames
    const ray = vnoise(along * 0.9 - ph * 7.2, 0.5 + t * 0.6, seed) * 0.65 + vnoise(along * 2.1 - ph * 14.4, t * 0.8, seed + 5) * 0.35;
    let c = mix(P.auroraG, P.auroraT, Math.min(1, t * 1.8));
    c = mix(c, t > 0.55 ? P.auroraP : P.auroraB, Math.max(0, (t - 0.35) / 0.65));
    if (t > 0.82) c = mix(c, P.auroraM, (t - 0.82) / 0.18 * 0.7);
    const hem = Math.max(0, 1 - (v - (H - 0.9)) / 0.9); // soft bright hem line
    let a = (0.1 + 0.9 * ray ** 1.6) * (1 - t * 0.6) * hem;
    // taper towards the tip of the veil and fade at the very end
    const tipFade = Math.min(1, (total - along) / 3, (along + 0.5) / 1.5);
    a *= Math.max(0, tipFade);
    if (v > H - 0.55) c = mix(c, "#eafff8", 0.6);
    if (a < 0.04) return null;
    const C = rgba(c);
    return [C[0], C[1], C[2], Math.round(255 * Math.min(0.92, a))];
  });
  return img;
}

export default function () {
  const b = cosmetic("polar_aurora_wings", {
    name: { en: "Polar Pals · Aurora Veil Wings", de: "Polar Pals · Polarlicht-Schleierflügel" },
    description: {
      en: "Three glowing curtains of northern light on each side. They ripple like silk, the light drifts along them, and they leave a trail of sparkles when you fly.",
      de: "Drei leuchtende Polarlicht-Vorhänge auf jeder Seite. Sie wogen wie Seide, das Licht wandert an ihnen entlang, und im Flug ziehen sie eine Funkelspur hinter sich her.",
    },
    tags: ["polar-pals", "wings", "aurora", "glow", "animated"],
    rarity: "epic",
    cosmetic: { slot: "wings" },
    preview: {
      panels: (p) => [
        { ...p[1], label: "back · idle", bloom: 0.45 },
        { ...p[0], label: "front · idle", bloom: 0.45 },
        { ...p[1], label: "back · fly", anims: [{ name: "fly", t: 0.3 }], time: 0.3, bloom: 0.45 },
        { ...p[1], label: "back · fly (later)", anims: [{ name: "fly", t: 0.75 }], time: 0.75, bloom: 0.45 },
        { ...p[1], label: "side · sneak", anims: [{ name: "sneak", t: 0.5 }], time: 0.5, cam: { yaw: 250, pitch: 10 }, bloom: 0.45 },
        { ...p[1], label: "top · idle", cam: { yaw: 180, pitch: 70 }, bloom: 0.45 },
      ],
      sheet: { cols: 3, panelW: 320, panelH: 330 },
      thumb: { cam: { yaw: 200, pitch: 12 }, bloom: 0.8 },
    },
  });
  b.texture("main", { hd: K, frames: FRAMES, frameTime: 2 });
  const tex = b.textures.get("main");
  const spark = tex.region(3, 3, (img) => {
    each(img, () => [0, 0, 0, 0]);
    stroke(img, [[1.5, 0.1], [1.5, 2.9]], 0.36, "#eafff8");
    stroke(img, [[0.1, 1.5], [2.9, 1.5]], 0.36, "#eafff8");
    ellipse(img, 1.5, 1.5, 0.55, 0.55, "#ffffff");
    return img;
  });
  const spark2 = tex.region(3, 3, (img) => {
    each(img, () => [0, 0, 0, 0]);
    stroke(img, [[1.5, 0.3], [1.5, 2.7]], 0.3, P.tealLight);
    stroke(img, [[0.3, 1.5], [2.7, 1.5]], 0.3, P.brandLight);
    ellipse(img, 1.5, 1.5, 0.45, 0.45, "#ffffff");
    return img;
  });
  b.sprite("spark", "main", spark).sprite("spark2", "main", spark2);

  for (const [side, s] of [["r", 1], ["l", -1]]) {
    const X = (x) => s * x;
    const box = (bone, from, size, paint, opts) => bone.box(s > 0 ? from : [-from[0] - size[0], from[1], from[2]], size, paint, opts);
    const tipStart = (face) => (face === "north") === (s > 0);
    const root = b.bone(`aurora_${side}`, { attach: "body", pivot: [X(1.5), 21, 2.4], rotation: [0, s * -22, 0], emissive: true, translucent: true });
    // a small glowing crystal knot at the shoulder blade
    box(root, [1, 20, 2.2], [1, 2, 1], {
      all: (img) => each(img, (u, v) => mix(P.tealLight, P.teal, v / 2)),
    });
    VEILS.forEach((veil, vi) => {
      let parent = root.name;
      let x0 = 1.5;
      const total = veil.seg.reduce((a, [l]) => a + l, 0);
      veil.seg.forEach(([len, h], si) => {
        const name = `${side}_${veil.name}${si + 1}`;
        const bone = b.bone(name, { parent, pivot: [X(x0), 21, 2.5 + vi * 0.04], rotation: si === 0 ? [0, 0, s * veil.ang] : Z0, emissive: true, translucent: true });
        const off = x0 - 1.5;
        const paint = (face) => (img, frame) => curtain(img, frame, off, veil.seed, tipStart(face), total);
        box(bone, [x0, 21 - h / 2, 2.5 + vi * 0.04], [len, h, 0], { north: paint("north"), south: paint("south") });
        parent = name;
        x0 += len;
      });
      b.locator(`tip_${side}_${veil.name}`, `${side}_${veil.name}3`, [X(x0 - 1), 21, 2.5]);
    });
  }

  // ripple: every segment swings around its hinge with a phase delay along the veil
  const ripple = (anim, len, amp, rootAmp, sweep, cycles = 1) => {
    const n = 8;
    for (const [side, s] of [["r", 1], ["l", -1]]) {
      const rk = [];
      for (let i = 0; i <= n; i++) {
        const t = (i / n) * len;
        const w = Math.sin((i / n) * Math.PI * 2 * cycles);
        rk.push([t, [0, s * (sweep + w * rootAmp[0]), s * w * rootAmp[1]]]);
      }
      anim.rot(`aurora_${side}`, rk);
      VEILS.forEach((veil, vi) => {
        for (let si = 0; si < veil.seg.length; si++) {
          const k = [];
          for (let i = 0; i <= n; i++) {
            const t = (i / n) * len;
            const w = Math.sin((i / n) * Math.PI * 2 * cycles - (si + 1) * 0.9 - vi * 0.5);
            k.push([t, [0, s * w * amp * (1 + si * 0.25), s * w * amp * 0.3]]);
          }
          anim.rot(`${side}_${veil.name}${si + 1}`, k);
        }
      });
    }
  };
  const idle = b.anim("idle", 4, true);
  ripple(idle, 4, 9, [5, 2], 0);
  for (const [t, side, v] of [[0.4, "r", "up"], [1.6, "l", "low"], [2.6, "r", "low"], [3.4, "l", "up"]])
    idle.particle(t, { sprite: t > 2 ? "spark2" : "spark", locator: `tip_${side}_${v}`, count: 1, lifetime: 1.2, size: 0.14, spread: [0.05, 0.05, 0.05], velocity: [0, 0.01, 0.005], gravity: -0.0005, emissive: true });

  const fly = b.anim("fly", 1, true);
  ripple(fly, 1, 16, [-14, 24], 14);
  for (let i = 0; i < 4; i++)
    for (const side of ["r", "l"])
      fly.particle(i * 0.25, { sprite: i % 2 ? "spark2" : "spark", locator: `tip_${side}_${["up", "low"][i % 2]}`, count: 1, lifetime: 1.4, size: 0.16, spread: [0.06, 0.06, 0.06], velocity: [0, -0.005, 0.01], gravity: 0.0005, emissive: true });

  const sneak = b.anim("sneak", 3, true);
  ripple(sneak, 3, 5, [3, 1], -40);
  return b;
}
