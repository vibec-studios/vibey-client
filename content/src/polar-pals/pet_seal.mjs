// Polar Pals · Seal Pup – the centrepiece pet. A chibi seal with a big round head, glossy eyes, whiskers, soft fur
// and a 20 s idle "day": breathe, blink, head tilt, sit up and clap (hearts + squeak), roll over, nap with floating
// Zs, wake up and stretch. walk = galumph-galumph-belly-slide, fly = superman with flapping flippers, sneak = shy
// tucked-in, blushing loaf.
import { soundFile } from "../_common.mjs";
import { K, P, cosmetic, fur, rim, edges, each, ellipse, blush, eye, closedEye, stroke, arc, shape, mix, rgba, petPreview } from "./_polar.mjs";

const L = 20; // idle length (s)

/** Fur box painter: top colour, side gradient, optional belly band on the lowest `band` rows, belly underneath. */
function furry(seed, o = {}) {
  const top = o.top ?? P.sealTop, side = o.side ?? [P.seal, P.sealLow], band = o.band ?? 0;
  const bellyBand = (img) => {
    if (!band) return img;
    const h = img.height / K;
    return each(img, (u, v) => {
      const t = (v - (h - band)) / 1.2;
      if (t <= 0) return null;
      const c = rgba(P.belly);
      return [c[0], c[1], c[2], 255 * Math.min(1, t)];
    });
  };
  return {
    up: (img) => rim(fur(img, { top, bottom: top, seed, strands: 0.07 }), 0.1, 1.1),
    down: (img) => rim(fur(img, { top: P.belly, bottom: P.bellyShade, seed: seed + 1, strands: 0.03 }), 0.08),
    sides: (img, f, face) => rim(bellyBand(fur(img, { top: side[0], bottom: side[1], seed: seed + face.length, strands: 0.06 })), 0.12, 1.1),
    ...(o.north ? { north: o.north } : {}),
  };
}

export default function () {
  const b = cosmetic("polar_seal", {
    name: { en: "Polar Pals · Seal Pup", de: "Polar Pals · Robbenbaby" },
    description: {
      en: "A round little seal pup with big glossy eyes. Blinks, tilts its head, claps its flippers with hearts, rolls over, naps with floating Zs – and belly-slides after you.",
      de: "Ein kugelrundes Robbenbaby mit großen Glanzaugen. Blinzelt, legt den Kopf schief, klatscht mit Herzchen in die Flossen, rollt sich, hält ein Nickerchen mit schwebenden Zs – und rutscht dir auf dem Bauch hinterher.",
    },
    tags: ["polar-pals", "seal", "cute", "pet", "animated"],
    rarity: "legendary",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 0.45 } },
    preview: petPreview([
      { label: "idle · look", t: 2.9 },
      { label: "idle · clap", t: 5.2 },
      { label: "idle · roll", t: 10.0 },
      { label: "idle · nap", t: 15.0 },
      { label: "walk · hop", anim: "walk", t: 0.15 },
      { label: "walk · belly slide", anim: "walk", t: 1.2, cam: { yaw: 70, pitch: 12 } },
      { label: "fly", anim: "fly", t: 0.3 },
      { label: "sneak · shy", anim: "sneak", t: 0.5 },
      { label: "front", t: 0.5, cam: { yaw: 0, pitch: 6 } },
      { label: "side", t: 0.5, cam: { yaw: 90, pitch: 6 } },
      { label: "back", t: 0.5, cam: { yaw: 200, pitch: 18 } },
    ], { cols: 4 }),
  });
  b.texture("main", { hd: K });

  // ------------------------------------------------------------------------------------------------ bones
  b.bone("seal", { attach: "world", pivot: [0, 0, 0] });
  b.bone("roll", { parent: "seal", pivot: [0, 5, 0] });
  const body = b.bone("body", { parent: "roll", pivot: [0, 0, 10] });

  // torso: core + rounding layers (top, sides) so the voxel body reads soft and round
  const chest = (img) => {
    fur(img, { top: P.seal, bottom: P.sealLow, seed: 11 });
    ellipse(img, img.width / K / 2, img.height / K * 0.95, img.width / K * 0.36, img.height / K * 0.75, (u, v) => mix(P.belly, P.bellyShade, v / 9));
    return rim(img, 0.12, 1.1);
  };
  body.box([-6, 0, -7], [12, 9, 14], furry(10, { band: 2.2, north: chest }));
  body.box([-5, 9, -6], [10, 1, 12], furry(12, { side: [P.sealTop, P.seal] }));
  body.box([-7, 1, -6], [1, 7, 12], furry(13, { band: 1.6 }));
  body.box([6, 1, -6], [1, 7, 12], furry(14, { band: 1.6 }));
  // hips taper towards the tail
  body.box([-5, 0, 7], [10, 7, 4], furry(15, { band: 1.8 }));
  body.box([-4, 7, 7], [8, 1, 3], furry(16));

  const tail = b.bone("tail", { parent: "body", pivot: [0, 2, 11] });
  tail.box([-3, 0.5, 11], [6, 4, 5], furry(20, { band: 1.2 }));
  const flukeR = b.bone("fluke_r", { parent: "tail", pivot: [1.5, 2, 15.5], rotation: [0, -28, 0] });
  const flukeL = b.bone("fluke_l", { parent: "tail", pivot: [-1.5, 2, 15.5], rotation: [0, 28, 0] });
  const flipperPaint = (seed) => ({
    up: (img) => rim(fur(img, { top: P.flipper, bottom: P.flipperDark, seed, strands: 0.03, horizontal: true }), 0.15, 0.8),
    down: (img) => rim(fur(img, { top: P.flipperDark, seed: seed + 1, strands: 0.02 }), 0.1, 0.8),
    sides: (img) => fur(img, { top: P.flipper, bottom: P.flipperDark, seed: seed + 2, strands: 0.02 }),
  });
  flukeR.box([1, 1.5, 15], [3, 1, 6], flipperPaint(21), { inflate: 0.15 });
  flukeL.box([-4, 1.5, 15], [3, 1, 6], flipperPaint(23), { inflate: 0.15 });

  // front flippers (pivot at the shoulder root), with three little claw marks at the tip
  const finPaint = (seed, right) => ({
    ...flipperPaint(seed),
    up: (img) => {
      rim(fur(img, { top: P.flipper, bottom: P.flipperDark, seed, strands: 0.03, horizontal: true }), 0.15, 0.8);
      const w = img.width / K; // w = 6 (x), 3 deep (z)
      const tip = right ? 0.55 : w - 0.55; // +X tip is at u = 0 on the up face
      for (const z of [0.7, 1.5, 2.3]) stroke(img, [[tip, z], [tip + (right ? 0.5 : -0.5), z]], 0.22, P.flipperDark);
      return img;
    },
  });
  const finR = b.bone("fin_r", { parent: "body", pivot: [5, 1.5, -4.5], rotation: [0, -25, -12] });
  finR.box([5, 1, -6], [6, 1, 3], finPaint(30, true), { inflate: 0.2 });
  const finL = b.bone("fin_l", { parent: "body", pivot: [-5, 1.5, -4.5], rotation: [0, 25, 12] });
  finL.box([-11, 1, -6], [6, 1, 3], finPaint(32, false), { inflate: 0.2 });

  // ------------------------------------------------------------------------------------------------ head
  const head = b.bone("head", { parent: "body", pivot: [0, 9, -6] });
  const faceFur = (v) => mix(P.sealTop, P.sealLow, Math.min(1, v / 10));
  const face = (img) => {
    fur(img, { top: P.sealTop, bottom: P.sealLow, seed: 40, strands: 0.05 });
    // lighter mask around the eyes and cheeks
    ellipse(img, 6, 6.4, 5.4, 3.8, (u, v) => mix(P.seal, P.sealLow, v / 10), 0.55);
    blush(img, 1.75, 6.75, 1.25, 0.8, P.blush, 0.6);
    blush(img, 10.25, 6.75, 1.25, 0.8, P.blush, 0.6);
    eye(img, 3, 4.1, 1.55, 1.8);
    eye(img, 9, 4.1, 1.55, 1.8, { mirror: true });
    // tiny brows for expression
    stroke(img, arc(3, 2.05, 0.9, 0.35, 200, 340, 6), 0.24, P.sealDark, 0.55);
    stroke(img, arc(9, 2.05, 0.9, 0.35, 200, 340, 6), 0.24, P.sealDark, 0.55);
    return rim(img, 0.1, 1);
  };
  head.box([-6, 6, -15], [12, 10, 10], { ...furry(41, { band: 0 }), north: face, down: (img) => fur(img, { top: P.belly, seed: 42 }) });
  head.box([-5, 16, -14], [10, 1, 8], furry(43, { side: [P.sealTop, P.sealTop] }));
  head.box([-5, 5, -14], [10, 1, 8], { all: (img) => fur(img, { top: P.sealLow, bottom: P.belly, seed: 44 }) });
  head.box([-7, 7, -14], [1, 8, 8], furry(45));
  head.box([6, 7, -14], [1, 8, 8], furry(46));
  // muzzle with nose, "w" mouth and whisker dots
  head.box([-3, 6, -17], [6, 4, 2], {
    north: (img) => {
      fur(img, { top: P.muzzle, bottom: P.muzzleShade, seed: 50, strands: 0.02 });
      edges(img, { top: 0.04, bottom: 0.1, left: 0.1, right: 0.1 }, 0.8);
      ellipse(img, 3, 0.95, 1.05, 0.62, P.nose);
      ellipse(img, 2.7, 0.75, 0.32, 0.18, P.noseShine, 0.9);
      stroke(img, [[3, 1.5], [3, 2.1]], 0.2, P.mouth);
      stroke(img, [...arc(2.45, 2.1, 0.55, 0.45, 0, 180, 6).reverse()], 0.2, P.mouth);
      stroke(img, arc(3.55, 2.1, 0.55, 0.45, 0, 180, 6), 0.2, P.mouth);
      for (const [u, v] of [[1.2, 1.5], [0.8, 2.2], [1.4, 2.6], [4.8, 1.5], [5.2, 2.2], [4.6, 2.6]]) ellipse(img, u, v, 0.14, 0.14, P.sealDark, 0.7);
      return img;
    },
    up: (img) => fur(img, { top: P.muzzleShade, bottom: P.muzzle, seed: 51, strands: 0.02 }),
    sides: (img) => fur(img, { top: P.muzzle, bottom: P.muzzleShade, seed: 52, strands: 0.02 }),
    down: (img) => fur(img, { top: P.muzzleShade, seed: 53 }),
  });
  // whiskers: thin transparent planes splayed out of the muzzle sides
  const whisker = (img) => {
    stroke(img, [[0.2, 0.6], [2.2, 0.75], [3.8, 0.45]], 0.16, "#f4f6fa", 0.95);
    stroke(img, [[0.2, 1.4], [2.0, 1.45], [3.8, 1.75]], 0.16, "#f4f6fa", 0.95);
    return img;
  };
  head.box([3, 7, -16.5], [4, 2, 0], { north: whisker, south: whisker }, { rotation: [0, -20, -6], pivot: [3, 8, -16.5] });
  head.box([-7, 7, -16.5], [4, 2, 0], { north: whisker, south: whisker }, { rotation: [0, 20, 6], pivot: [-3, 8, -16.5] });

  // eyelids: planes just *inside* the face; idle pushes them out in front of the eyes (blink / sleep / happy).
  // At rest (and whenever idle has no weight, e.g. while walking) they are hidden inside the head.
  const lidPlane = (kind) => (img) => {
    each(img, () => [0, 0, 0, 0]);
    ellipse(img, 2, 2.1, 1.75, 2.0, (u, v) => faceFur(v + 1.9));
    closedEye(img, 2, kind === "happy" ? 2.0 : 2.3, 2.5, kind, P.eye, 0.36);
    if (kind === "happy") blush(img, 2, 3.5, 1.3, 0.5, P.blush, 0.35);
    return img;
  };
  for (const [name, kind] of [["lids", "sleep"], ["happy", "happy"]]) {
    const lid = b.bone(name, { parent: "head", pivot: [0, 12, -15] });
    lid.box([1, 10, -14.9], [4, 4, 0], { north: lidPlane(kind) });
    lid.box([-5, 10, -14.9], [4, 4, 0], { north: lidPlane(kind) });
  }

  // floating Zs for the nap (3D glyphs, hidden inside the body at rest)
  const zPaint = { all: (img) => rim(fur(img, { top: "#f3f0ff", bottom: "#d6ccff", seed: 70, strands: 0.0, grain: 0.01 }), 0.2, 0.5) };
  for (const n of [1, 2, 3]) {
    const z = b.bone(`z${n}`, { parent: "seal", pivot: [0, 5, 0] });
    // a 7×7 "Z" (the viewer's right is −X): top bar, five-step diagonal, bottom bar
    z.box([-3.5, 8, -0.5], [7, 1, 1], zPaint);
    for (let k = 0; k < 5; k++) z.box([-2.5 + k, 7 - k, -0.5], [1, 1, 1], zPaint);
    z.box([-3.5, 2, -0.5], [7, 1, 1], zPaint);
  }

  b.locator("heart", "head", [0, 18, -11]);
  b.locator("belly", "body", [0, 0.5, -2]);
  b.locator("nose", "head", [0, 9, -17.5]);
  b.sound("squeak", soundFile("squeak"));

  // ------------------------------------------------------------------------------------------------ idle (20 s)
  const idle = b.anim("idle", L, true);
  const Z0 = [0, 0, 0];
  idle.rot("head", [
    [0, Z0], [1, [2, 0, 0]], [2, Z0], [2.45, [4, 16, 13]], [3.4, [4, 16, 13]], [3.9, Z0],
    [4.3, [-16, 0, 0]], [4.95, [-12, 0, -7]], [5.35, [-12, 0, 7]], [5.75, [-12, 0, -5]], [6.15, [-6, 0, 0]], [6.6, Z0],
    [7.0, [0, -22, -5]], [7.8, [0, -22, -5]], [8.3, [3, 10, 0]], [8.8, Z0],
    [9.3, [6, 0, 0]], [10.7, [6, 0, 0]], [11.0, [0, 13, 0]], [11.15, [0, -13, 0]], [11.3, [0, 10, 0]], [11.45, [0, -7, 0]], [11.65, Z0],
    [12.0, Z0], [12.9, [-15, 0, 9]], [17.6, [-15, 0, 9]], [18.2, [10, 0, 0]], [18.9, Z0], [L, Z0],
  ]);
  idle.pos("head", [[0, Z0], [12.0, Z0], [12.9, [0, -1.6, 0]], [17.6, [0, -1.6, 0]], [18.2, [0, 0.6, 0]], [18.9, Z0], [L, Z0]]);
  // body: sit up for the clap, breathe all day (deeper while napping)
  idle.rot("body", [[0, Z0], [4.0, Z0], [4.35, [30, 0, 0]], [6.05, [30, 0, 0]], [6.45, [-2, 0, 0]], [6.7, Z0], [12, Z0], [12.9, [-1.5, 0, 0]], [17.6, [-1.5, 0, 0]], [18.2, [4, 0, 0]], [18.9, Z0], [L, Z0]]);
  const breath = [];
  for (let t = 0; t < 12; t += 2) breath.push([t, [1, 1, 1]], [t + 1, [1.012, 1.03, 1.0]]);
  breath.push([12, [1, 1, 1]]);
  for (let t = 13; t < 18; t += 1.25) breath.push([t, [1.02, 1.05, 1.0]], [t + 0.625, [1, 0.99, 1]]);
  breath.push([18.2, [1, 1, 1]], [19, [1.012, 1.03, 1]], [L, [1, 1, 1]]);
  idle.scale("body", breath);
  // flippers: tiny fidget, the clap (three claps), stretch after the nap
  const OPEN = [0, 140, 12], SHUT = [0, 176, 12];
  const clapR = [[4.0, Z0], [4.45, OPEN], [4.8, SHUT], [4.98, OPEN], [5.15, SHUT], [5.33, OPEN], [5.5, SHUT], [5.7, OPEN], [6.1, [0, 60, 6]], [6.45, Z0]];
  const finKeys = (sx) => [
    [0, Z0], [1.6, Z0], [1.9, [0, 0, 10 * sx]], [2.2, Z0], ...clapR.map(([t, v]) => [t, [v[0], v[1] * sx, v[2] * sx]]),
    [12, Z0], [12.9, [0, 18 * sx, 4 * sx]], [17.6, [0, 18 * sx, 4 * sx]], [18.2, [0, -15 * sx, 30 * sx]], [18.9, Z0], [L, Z0],
  ];
  idle.rot("fin_r", finKeys(1));
  idle.rot("fin_l", finKeys(-1));
  // tail flukes wag
  const wag = (sx) => {
    const k = [];
    for (let t = 0; t <= L; t += 0.8) k.push([t, [0, (Math.round(t / 0.8) % 2 ? 9 : -4) * sx, 0]]);
    k[k.length - 1] = [L, [0, -4 * sx, 0]];
    return k;
  };
  idle.rot("fluke_r", wag(1));
  idle.rot("fluke_l", wag(-1));
  idle.rot("tail", [[0, Z0], [4.0, Z0], [4.4, [-8, 0, 0]], [6.0, [-8, 0, 0]], [6.4, Z0], [9.3, Z0], [9.6, [-12, 0, 0]], [10.6, [-12, 0, 0]], [10.9, Z0], [L, Z0]]);
  // roll over (with a little hop so the round body clears the ground)
  idle.rot("roll", [[0, Z0, "linear"], [9.4, Z0, "linear"], [9.8, [0, 0, 120], "linear"], [10.2, [0, 0, 240], "linear"], [10.6, [0, 0, 360], "step"], [10.61, Z0, "linear"], [L, Z0, "linear"]], "linear");
  idle.pos("seal", [[0, Z0], [9.3, Z0], [9.55, [0, 3.2, 0]], [10.45, [0, 3.2, 0]], [10.7, [0, -0.2, 0]], [10.9, Z0], [L, Z0]]);
  // eyelids: step keys so blinks are crisp
  const OUT = [0, 0, -0.3];
  const lidWin = (wins) => {
    const k = [[0, Z0, "step"]];
    for (const [a, c] of wins) k.push([a, OUT, "step"], [c, Z0, "step"]);
    k.push([L, Z0, "step"]);
    return k.sort((x, y) => x[0] - y[0]);
  };
  idle.pos("lids", lidWin([[1.4, 1.55], [3.0, 3.13], [8.1, 8.24], [12.75, 18.0], [18.4, 18.52], [18.75, 18.87]]), "step");
  idle.pos("happy", lidWin([[4.55, 6.15], [10.1, 11.0]]), "step");
  // Zs: rise from above the head, grow and spin, then vanish
  const zWins = { z1: [13.0, 16.6], z2: [14.2, 17.8], z3: [15.4] };
  for (const [z, starts] of Object.entries(zWins)) {
    const pos = [[0, Z0, "step"]], sc = [[0, [1, 1, 1], "step"]], rot = [[0, Z0, "step"]];
    for (const t0 of starts) {
      pos.push([t0, [2, 12, -10]], [t0 + 0.6, [3.5, 16, -9.5]], [t0 + 1.5, [6, 22, -8], "step"], [t0 + 1.51, Z0, "step"]);
      sc.push([t0, [0.4, 0.4, 0.4]], [t0 + 0.6, [1.1, 1.1, 1.1]], [t0 + 1.5, [1.5, 1.5, 1.5], "step"], [t0 + 1.51, [1, 1, 1], "step"]);
      rot.push([t0, [0, -25, 8], "linear"], [t0 + 1.5, [0, 35, -8], "step"], [t0 + 1.51, Z0, "step"]);
      idle.visible(t0, z, true).visible(t0 + 1.5, z, false);
    }
    pos.push([L, Z0, "step"]);
    sc.push([L, [1, 1, 1], "step"]);
    rot.push([L, Z0, "step"]);
    idle.pos(z, pos, "catmullrom").scale(z, sc, "catmullrom").rot(z, rot, "linear");
    idle.visible(0, z, false);
  }
  // events: hearts + squeak on each clap, snow puff when landing from the roll, sleepy sigh
  for (const t of [4.8, 5.15, 5.5]) {
    idle.particle(t, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.12, 0.05, 0.12], velocity: [0, 0.03, 0] });
    idle.sound(t, "squeak", { volume: 0.22, pitch: t === 5.5 ? 1.35 : 1.2 });
  }
  idle.particle(10.65, { particle: "minecraft:snowflake", locator: "belly", count: 6, spread: [0.25, 0.04, 0.25], velocity: [0, 0.03, 0] });
  idle.particle(11.0, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.1, 0.05, 0.1], velocity: [0, 0.02, 0] });

  // ------------------------------------------------------------------------------------------------ walk (1.6 s)
  // galumph, galumph, then a happy belly slide on the ice
  const walk = b.anim("walk", 1.6, true);
  walk.pos("seal", [[0, Z0], [0.2, [0, 2.4, 0]], [0.4, Z0], [0.6, [0, 2.4, 0]], [0.8, [0, -0.2, 0]], [1.0, Z0], [1.6, Z0]]);
  walk.rot("body", [[0, [-3, 0, 0]], [0.1, [11, 0, 0]], [0.3, [-6, 0, 0]], [0.4, [-3, 0, 0]], [0.5, [11, 0, 0]], [0.7, [-6, 0, 0]], [0.85, [-4, 0, 0]], [1.0, [-2, 0, 0]], [1.45, [-2, 0, 0]], [1.6, [-3, 0, 0]]]);
  walk.scale("body", [[0, [1.04, 0.94, 1]], [0.15, [0.97, 1.05, 1]], [0.4, [1.04, 0.94, 1]], [0.55, [0.97, 1.05, 1]], [0.8, [1.05, 0.93, 1]], [1.0, [1.02, 0.97, 1.03]], [1.45, [1.02, 0.97, 1.03]], [1.6, [1.04, 0.94, 1]]]);
  walk.rot("head", [[0, [-4, 0, 0]], [0.2, [6, 0, 0]], [0.4, [-4, 0, 0]], [0.6, [6, 0, 0]], [0.8, [-2, 0, 0]], [1.0, [12, 0, 4]], [1.4, [12, 0, -4]], [1.6, [-4, 0, 0]]]);
  const paddle = (sx) => [[0, [0, 25 * sx, 0]], [0.2, [0, -30 * sx, 0]], [0.4, [0, 25 * sx, 0]], [0.6, [0, -30 * sx, 0]], [0.85, [0, -55 * sx, -4 * sx]], [1.45, [0, -55 * sx, -4 * sx]], [1.6, [0, 25 * sx, 0]]];
  walk.rot("fin_r", paddle(1));
  walk.rot("fin_l", paddle(-1));
  walk.rot("tail", [[0, [6, 0, 0]], [0.2, [-10, 0, 0]], [0.4, [6, 0, 0]], [0.6, [-10, 0, 0]], [0.85, [-18, 0, 0]], [1.45, [-18, 0, 0]], [1.6, [6, 0, 0]]]);
  walk.rot("fluke_r", [[0, Z0], [0.9, Z0], [1.05, [0, 14, 0]], [1.2, [0, -6, 0]], [1.35, [0, 14, 0]], [1.6, Z0]]);
  walk.rot("fluke_l", [[0, Z0], [0.9, Z0], [1.05, [0, -14, 0]], [1.2, [0, 6, 0]], [1.35, [0, -14, 0]], [1.6, Z0]]);
  walk.pos("happy", [[0, Z0, "step"], [0.9, OUT, "step"], [1.5, Z0, "step"], [1.6, Z0, "step"]], "step");
  walk.particle(0.4, { particle: "minecraft:snowflake", locator: "belly", count: 2, spread: [0.15, 0.02, 0.15], velocity: [0, 0.02, 0] });
  walk.particle(0.8, { particle: "minecraft:snowflake", locator: "belly", count: 3, spread: [0.2, 0.02, 0.2], velocity: [0, 0.025, 0] });
  walk.particle(1.1, { particle: "minecraft:snowflake", locator: "belly", count: 2, spread: [0.2, 0.02, 0.1], velocity: [0, 0.02, 0.03] });

  // ------------------------------------------------------------------------------------------------ fly (1.2 s)
  const fly = b.anim("fly", 1.2, true);
  fly.pos("seal", [[0, Z0], [0.6, [0, 1.6, 0]], [1.2, Z0]]);
  fly.rot("body", [[0, [-6, 0, 0]], [0.6, [-2, 0, 0]], [1.2, [-6, 0, 0]]]);
  fly.rot("head", [[0, [12, 0, 0]], [0.6, [8, 0, 0]], [1.2, [12, 0, 0]]]);
  const flap = (sx) => [[0, [0, 28 * sx, 34 * sx]], [0.3, [0, 28 * sx, 0]], [0.6, [0, 28 * sx, -22 * sx]], [0.9, [0, 28 * sx, 0]], [1.2, [0, 28 * sx, 34 * sx]]];
  fly.rot("fin_r", flap(1));
  fly.rot("fin_l", flap(-1));
  fly.rot("tail", [[0, [-14, 0, 0]], [0.3, [-6, 12, 0]], [0.6, [-14, 0, 0]], [0.9, [-6, -12, 0]], [1.2, [-14, 0, 0]]]);
  fly.particle(0.0, { particle: "minecraft:snowflake", locator: "belly", count: 1, spread: [0.1, 0.05, 0.1], velocity: [0, -0.01, 0.02] });

  // ------------------------------------------------------------------------------------------------ sneak (2 s)
  // shy: tucks the head in, squeezes the eyes shut with a blush, flippers hugging the body, tail wiggles
  const sneak = b.anim("sneak", 2, true);
  sneak.rot("body", [[0, [-3, 0, 0]], [2, [-3, 0, 0]]]);
  sneak.scale("body", [[0, [1.05, 0.9, 1.02]], [1, [1.06, 0.88, 1.02]], [2, [1.05, 0.9, 1.02]]]);
  sneak.rot("head", [[0, [-16, 0, 8]], [1.0, [-16, 0, -8]], [2, [-16, 0, 8]]]);
  sneak.pos("head", [[0, [0, -1.5, 0.5]], [2, [0, -1.5, 0.5]]]);
  const hug = (sx) => [[0, [0, -40 * sx, -4 * sx]], [1, [0, -48 * sx, -4 * sx]], [2, [0, -40 * sx, -4 * sx]]];
  sneak.rot("fin_r", hug(1));
  sneak.rot("fin_l", hug(-1));
  sneak.pos("happy", [[0, OUT, "step"], [2, OUT, "step"]], "step");
  sneak.rot("tail", [[0, [-6, -10, 0]], [0.5, [-6, 10, 0]], [1, [-6, -10, 0]], [1.5, [-6, 10, 0]], [2, [-6, -10, 0]]]);
  return b;
}
