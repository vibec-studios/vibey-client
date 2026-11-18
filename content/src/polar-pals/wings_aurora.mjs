// Polar Pals · Aurora Veil Wings – two soft curtains of northern light per side, like a butterfly. Each veil is a
// chain of narrow translucent, emissive segments (2 px) carrying one continuous aurora curtain (shared painter with
// the Aurora Ring: mint hem, light fading up through teal and blue into violet, drifting rays, soft alpha everywhere).
// Phase-shifted waves run along the chains, so the veils flow like silk. idle = slow ripple, fly = big flowing
// strokes with a sparkle trail, glide = veils stretched back and streaming, sneak = folded in. The crystal knots sit
// at the outer shoulder blades (DX); with a backpack they lift and move back a little (cosmetic.offsetWhen) and glow
// right beside its top, the veils never cut into it.
import { P, cosmetic, each, stroke, ellipse, mix, auroraTexel, smoothstep } from "./_polar.mjs";

const K2 = 2;
const FRAMES = 12;
const SEG = 2; // segment length (px)
const Z0 = [0, 0, 0];
// roots this far out from x = ±1.5: room for a backpack between the two knots
const DX = 3;
// two veils per side: a big upper curtain and a smaller lower one; heights per segment taper towards the tip
const VEILS = [
  { name: "up", ang: 28, seed: 3, n: 12, h0: 16, h1: 9 },
  { name: "low", ang: -32, seed: 11, n: 8, h0: 11, h1: 6 },
];
const heightOf = (veil, i) => Math.round(veil.h0 + ((veil.h1 - veil.h0) * i) / (veil.n - 1));

export default function () {
  const b = cosmetic("polar_aurora_wings", {
    name: { en: "Polar Pals · Aurora Veil Wings", de: "Polar Pals · Polarlicht-Schleierflügel" },
    description: {
      en: "Two soft curtains of northern light on each side. They flow like silk, rays of light drift along them, they stream back when you glide and leave a trail of sparkles when you fly.",
      de: "Zwei sanfte Polarlicht-Vorhänge auf jeder Seite. Sie fließen wie Seide, Lichtstrahlen wandern an ihnen entlang, im Gleitflug wehen sie nach hinten und im Flug ziehen sie eine Funkelspur hinter sich her.",
    },
    tags: ["polar-pals", "wings", "aurora", "glow", "animated"],
    rarity: "epic",
    cosmetic: { slot: "wings", offsetWhen: [{ slot: "back", offset: [0, 2, 4] }] },
    preview: {
      panels: (p) => [
        { ...p[1], label: "back · idle", bloom: 0.5 },
        { ...p[0], label: "front · idle", bloom: 0.5 },
        { ...p[1], label: "back · fly", anims: [{ name: "fly", t: 0.4 }], time: 0.4, bloom: 0.5 },
        { ...p[1], label: "back · glide", anims: [{ name: "glide", t: 1.0 }], time: 1.0, cam: { yaw: 210, pitch: 24 }, bloom: 0.5 },
        { ...p[1], label: "side · sneak", anims: [{ name: "sneak", t: 0.5 }], time: 0.5, cam: { yaw: 250, pitch: 10 }, bloom: 0.5 },
        { ...p[1], label: "top · idle", cam: { yaw: 180, pitch: 70 }, bloom: 0.5 },
      ],
      sheet: { cols: 3, panelW: 320, panelH: 330 },
      thumb: { cam: { yaw: 200, pitch: 12 }, bloom: 0.8 },
    },
  });
  b.texture("main", { hd: K2, frames: FRAMES, frameTime: 4, interpolate: true });
  const tex = b.textures.get("main");
  const spark = tex.region(3, 3, (img) => {
    each(img, () => [0, 0, 0, 0], K2);
    stroke(img, [[1.5, 0.2], [1.5, 2.8]], 0.4, "#eafff8", 1, K2);
    stroke(img, [[0.2, 1.5], [2.8, 1.5]], 0.4, "#eafff8", 1, K2);
    ellipse(img, 1.5, 1.5, 0.55, 0.55, "#ffffff", 1, K2);
    return img;
  });
  const spark2 = tex.region(3, 3, (img) => {
    each(img, () => [0, 0, 0, 0], K2);
    stroke(img, [[1.5, 0.3], [1.5, 2.7]], 0.36, P.tealLight, 1, K2);
    stroke(img, [[0.3, 1.5], [2.7, 1.5]], 0.36, P.brandLight, 1, K2);
    ellipse(img, 1.5, 1.5, 0.45, 0.45, "#ffffff", 1, K2);
    return img;
  });
  b.sprite("spark", "main", spark).sprite("spark2", "main", spark2);

  for (const [side, s] of [["r", 1], ["l", -1]]) {
    const X = (x) => s * (x + DX);
    const box = (bone, [fx, fy, fz], size, paint, opts) => bone.box(s > 0 ? [fx + DX, fy, fz] : [-fx - DX - size[0], fy, fz], size, paint, opts);
    // north face: u = 0 is +X; along the veil = distance from the root (consistent in world space on both faces)
    const fromRoot = (face, u) => ((face === "north") === (s > 0) ? SEG - u : u);
    const root = b.bone(`aurora_${side}`, { attach: "body", pivot: [X(1.5), 21, 2.4], rotation: [0, s * -22, 0], emissive: true, translucent: true });
    // a small glowing crystal knot at the shoulder blade
    box(root, [1, 20, 2.2], [1, 2, 1], { all: (img) => each(img, (u, v) => mix(P.tealLight, P.teal, v / 2), K2) });
    VEILS.forEach((veil, vi) => {
      let parent = root.name;
      let x0 = 1.5;
      const total = veil.n * SEG;
      for (let i = 0; i < veil.n; i++) {
        const name = `${side}_${veil.name}${i + 1}`;
        const h = heightOf(veil, i);
        const z = 2.5 + vi * 0.05;
        const bone = b.bone(name, { parent, pivot: [X(x0), 21, z], rotation: i === 0 ? [0, 0, s * veil.ang] : Z0, emissive: true, translucent: true });
        const paint = (face) => (img, frame) => {
          const ph = frame / FRAMES;
          return each(img, (u, v) => {
            const along = i * SEG + fromRoot(face, u);
            const env = smoothstep(0, 3, along) * smoothstep(0, 6, total - along);
            return auroraTexel(1 - v / h, along * 1.6, ph, { seed: veil.seed, env, maxA: 1.0 }) ?? [0, 0, 0, 0];
          }, K2);
        };
        box(bone, [x0, 21 - h / 2, z], [SEG, h, 0], { north: paint("north"), south: paint("south") });
        parent = name;
        x0 += SEG;
      }
      b.locator(`tip_${side}_${veil.name}`, `${side}_${veil.name}${veil.n}`, [X(x0 - 1), 21, 2.5]);
    });
  }

  // ripple: every segment swings around its hinge with a phase delay along the veil (a wave running to the tip)
  const ripple = (anim, len, amp, rootAmp, sweep, opts = {}) => {
    const steps = opts.steps ?? 8, cycles = opts.cycles ?? 1, lift = opts.lift ?? 0;
    for (const [side, s] of [["r", 1], ["l", -1]]) {
      const rk = [];
      for (let k = 0; k <= steps; k++) {
        const t = (k / steps) * len;
        const w = Math.sin((k / steps) * Math.PI * 2 * cycles);
        rk.push([t, [lift, s * (sweep + w * rootAmp[0]), s * w * rootAmp[1]]]);
      }
      anim.rot(`aurora_${side}`, rk);
      VEILS.forEach((veil, vi) => {
        for (let i = 0; i < veil.n; i++) {
          const k2 = [];
          for (let k = 0; k <= steps; k++) {
            const t = (k / steps) * len;
            const w = Math.sin((k / steps) * Math.PI * 2 * cycles - (i + 1) * 0.55 - vi * 0.6);
            k2.push([t, [0, s * w * amp * (0.7 + i * 0.06), s * w * amp * 0.35]]);
          }
          anim.rot(`${side}_${veil.name}${i + 1}`, k2);
        }
      });
    }
  };
  const idle = b.anim("idle", 4, true);
  ripple(idle, 4, 3.2, [5, 2], 0);
  for (const [t, side, v] of [[0.4, "r", "up"], [1.6, "l", "low"], [2.6, "r", "low"], [3.4, "l", "up"]])
    idle.particle(t, { sprite: t > 2 ? "spark2" : "spark", locator: `tip_${side}_${v}`, count: 1, lifetime: 1.2, size: 0.12, spread: [0.05, 0.05, 0.05], velocity: [0, 0.01, 0.005], gravity: -0.0005, emissive: true });

  const fly = b.anim("fly", 1.2, true);
  ripple(fly, 1.2, 5.5, [-14, 24], 14);
  for (let i = 0; i < 4; i++)
    for (const side of ["r", "l"])
      fly.particle(i * 0.3, { sprite: i % 2 ? "spark2" : "spark", locator: `tip_${side}_${["up", "low"][i % 2]}`, count: 1, lifetime: 1.4, size: 0.14, spread: [0.06, 0.06, 0.06], velocity: [0, -0.005, 0.01], gravity: 0.0005, emissive: true });

  // glide (elytra / long fall): veils stretched out and swept back, streaming in the wind
  const glide = b.anim("glide", 2, true);
  ripple(glide, 2, 2.6, [3, 3], 12, { cycles: 2, lift: -6 });
  glide.particle(0.5, { sprite: "spark", locator: "tip_r_up", count: 1, lifetime: 1.6, size: 0.12, spread: [0.05, 0.05, 0.05], velocity: [0, 0, 0.02], gravity: 0, emissive: true });
  glide.particle(1.5, { sprite: "spark2", locator: "tip_l_up", count: 1, lifetime: 1.6, size: 0.12, spread: [0.05, 0.05, 0.05], velocity: [0, 0, 0.02], gravity: 0, emissive: true });

  const sneak = b.anim("sneak", 3, true);
  ripple(sneak, 3, 1.8, [3, 1], -40);
  return b;
}
