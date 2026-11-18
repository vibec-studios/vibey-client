// Polar Pals · Seal Pup – the centrepiece pet, penguin-sized: lying it reaches your hip, sitting up your shoulder. A chibi seal with a big round head,
// glossy eyes, whiskers and soft fur. idle (13 s): breathe, blink, head tilt, sit up and clap (hearts + squeak), roll
// over, shake. walk = galumph, sprint = fast belly scoot, glide = superman, fly = flapping superman, sneak = shy
// blushing loaf, happy (while you play an emote) = hop-hop-clap with hearts, idle_long (you stood still ~10 s) =
// a nap with floating Zs.
import { soundFile } from "../_common.mjs";
import { K, P, cosmetic, fur, rim, edges, each, ellipse, blush, eye, closedEye, stroke, arc, shape, mix, rgba, petPreview } from "./_polar.mjs";

const L = 13; // idle length (s)

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
      en: "A round, penguin-sized seal pup with glossy eyes. Blinks, tilts its head, claps with hearts, rolls over, scoots after you on its belly, cheers when you emote and naps when you stand still.",
      de: "Ein kugelrundes Robbenbaby in Pinguingröße mit Glanzaugen. Blinzelt, legt den Kopf schief, klatscht mit Herzchen, rollt sich, rutscht dir auf dem Bauch hinterher, jubelt bei deinen Emotes und schläft ein, wenn du still stehst.",
    },
    tags: ["polar-pals", "seal", "cute", "pet", "animated"],
    rarity: "legendary",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 0.85 } },
    preview: petPreview([
      { label: "idle · look", t: 2.9 },
      { label: "idle · clap", t: 5.2 },
      { label: "idle · roll", t: 10.0 },
      { label: "happy · joy", anim: "happy", t: 0.45 },
      { label: "idle_long · nap", anim: "idle_long", t: 2.2 },
      { label: "walk · galumph", anim: "walk", t: 0.25 },
      { label: "sprint · belly scoot", anim: "sprint", t: 0.15, cam: { yaw: 70, pitch: 12 } },
      { label: "glide · superman", anim: "glide", t: 0.6, cam: { yaw: 60, pitch: 16 } },
      { label: "fly", anim: "fly", t: 0.3 },
      { label: "sneak · shy", anim: "sneak", t: 0.5 },
      { label: "front", t: 0.5, cam: { yaw: 0, pitch: 6 } },
      { label: "side", t: 0.5, cam: { yaw: 90, pitch: 6 } },
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
  body.box([-6, 0, -7], [12, 9, 12], furry(10, { band: 2.2, north: chest }));
  body.box([-5, 9, -6], [10, 1, 10], furry(12, { side: [P.sealTop, P.seal] }));
  body.box([-7, 1, -6], [1, 7, 10], furry(13, { band: 1.6 }));
  body.box([6, 1, -6], [1, 7, 10], furry(14, { band: 1.6 }));
  // hips taper towards the tail (short: a round chibi body)
  body.box([-5, 0, 5], [10, 7, 3], furry(15, { band: 1.8 }));
  body.box([-4, 7, 5], [8, 1, 2], furry(16));

  const tail = b.bone("tail", { parent: "body", pivot: [0, 2, 8] });
  tail.box([-3, 0.5, 8], [6, 4, 4], furry(20, { band: 1.2 }));
  const flukeR = b.bone("fluke_r", { parent: "tail", pivot: [1.5, 2, 11.5], rotation: [0, -28, 0] });
  const flukeL = b.bone("fluke_l", { parent: "tail", pivot: [-1.5, 2, 11.5], rotation: [0, 28, 0] });
  const flipperPaint = (seed) => ({
    up: (img) => rim(fur(img, { top: P.flipper, bottom: P.flipperDark, seed, strands: 0.03, horizontal: true }), 0.15, 0.8),
    down: (img) => rim(fur(img, { top: P.flipperDark, seed: seed + 1, strands: 0.02 }), 0.1, 0.8),
    sides: (img) => fur(img, { top: P.flipper, bottom: P.flipperDark, seed: seed + 2, strands: 0.02 }),
  });
  flukeR.box([1, 1.5, 11], [3, 1, 5], flipperPaint(21), { inflate: 0.15 });
  flukeL.box([-4, 1.5, 11], [3, 1, 5], flipperPaint(23), { inflate: 0.15 });

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
    // a 7×7 "Z" sunk well inside the body (the viewer's right is −X): top bar, five-step diagonal, bottom bar
    z.box([-3.5, 7, -0.5], [7, 1, 1], zPaint);
    for (let k = 0; k < 5; k++) z.box([-2.5 + k, 6 - k, -0.5], [1, 1, 1], zPaint);
    z.box([-3.5, 1, -0.5], [7, 1, 1], zPaint);
  }

  b.locator("heart", "head", [0, 18, -11]);
  b.locator("belly", "body", [0, 0.5, -2]);
  b.locator("nose", "head", [0, 9, -17.5]);
  b.sound("squeak", soundFile("squeak"));

  // ------------------------------------------------------------------------------------------------ idle (13 s)
  const idle = b.anim("idle", L, true);
  const Z0 = [0, 0, 0];
  idle.rot("head", [
    [0, Z0], [1, [2, 0, 0]], [2, Z0], [2.45, [4, 16, 13]], [3.4, [4, 16, 13]], [3.9, Z0],
    [4.3, [-16, 0, 0]], [4.95, [-12, 0, -7]], [5.35, [-12, 0, 7]], [5.75, [-12, 0, -5]], [6.15, [-6, 0, 0]], [6.6, Z0],
    [7.0, [0, -22, -5]], [7.8, [0, -22, -5]], [8.3, [3, 10, 0]], [8.8, Z0],
    [9.3, [6, 0, 0]], [10.7, [6, 0, 0]], [11.0, [0, 13, 0]], [11.15, [0, -13, 0]], [11.3, [0, 10, 0]], [11.45, [0, -7, 0]], [11.65, Z0], [L, Z0],
  ]);
  // body: sit up for the clap, breathe all the time
  idle.rot("body", [[0, Z0], [4.0, Z0], [4.35, [30, 0, 0]], [6.05, [30, 0, 0]], [6.45, [-2, 0, 0]], [6.7, Z0], [L, Z0]]);
  const breath = [];
  for (let t = 0; t < 12; t += 2) breath.push([t, [1, 1, 1]], [t + 1, [1.012, 1.03, 1.0]]);
  breath.push([L, [1, 1, 1]]);
  idle.scale("body", breath);
  // flippers: tiny fidget, the clap (three claps)
  const OPEN = [0, 140, 12], SHUT = [0, 176, 12];
  const clapR = [[4.0, Z0], [4.45, OPEN], [4.8, SHUT], [4.98, OPEN], [5.15, SHUT], [5.33, OPEN], [5.5, SHUT], [5.7, OPEN], [6.1, [0, 60, 6]], [6.45, Z0]];
  const M = (v, sx) => [v[0], v[1] * sx, v[2] * sx];
  const finKeys = (sx) => [[0, Z0], [1.6, Z0], [1.9, [0, 0, 10 * sx]], [2.2, Z0], ...clapR.map(([t, v]) => [t, M(v, sx)]), [L, Z0]];
  idle.rot("fin_r", finKeys(1));
  idle.rot("fin_l", finKeys(-1));
  // tail flukes wag
  const wag = (sx, len, step = 0.8, a = 9, c = -4) => {
    const k = [];
    const n = Math.round(len / step);
    for (let i = 0; i <= n; i++) k.push([Math.min(len, i * step), [0, (i % 2 ? a : c) * sx, 0]]);
    k[k.length - 1] = [len, [0, c * sx, 0]];
    return k;
  };
  idle.rot("fluke_r", wag(1, L, 13 / 16));
  idle.rot("fluke_l", wag(-1, L, 13 / 16));
  idle.rot("tail", [[0, Z0], [4.0, Z0], [4.4, [-8, 0, 0]], [6.0, [-8, 0, 0]], [6.4, Z0], [9.3, Z0], [9.6, [-12, 0, 0]], [10.6, [-12, 0, 0]], [10.9, Z0], [L, Z0]]);
  // roll over (with a little hop so the round body clears the ground)
  idle.rot("roll", [[0, Z0, "linear"], [9.4, Z0, "linear"], [9.8, [0, 0, 120], "linear"], [10.2, [0, 0, 240], "linear"], [10.6, [0, 0, 360], "step"], [10.61, Z0, "linear"], [L, Z0, "linear"]], "linear");
  idle.pos("seal", [[0, Z0], [9.3, Z0], [9.55, [0, 3.2, 0]], [10.45, [0, 3.2, 0]], [10.7, [0, -0.2, 0]], [10.9, Z0], [L, Z0]]);
  // eyelids: step keys so blinks are crisp
  const OUT = [0, 0, -0.3];
  const lidWin = (wins, len = L) => {
    const k = [[0, Z0, "step"]];
    for (const [a, c] of wins) k.push([a, OUT, "step"], [c, Z0, "step"]);
    k.push([len, Z0, "step"]);
    return k.sort((x, y) => x[0] - y[0]);
  };
  idle.pos("lids", lidWin([[1.4, 1.55], [3.0, 3.13], [8.1, 8.24], [12.2, 12.33]]), "step");
  idle.pos("happy", lidWin([[4.55, 6.15], [10.1, 11.0]]), "step");
  for (const z of ["z1", "z2", "z3"]) idle.visible(0, z, false);
  // events: hearts + squeak on each clap, snow puff when landing from the roll
  for (const t of [4.8, 5.15, 5.5]) {
    idle.particle(t, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.12, 0.05, 0.12], velocity: [0, 0.03, 0] });
    idle.sound(t, "squeak", { volume: 0.22, pitch: t === 5.5 ? 1.35 : 1.2 });
  }
  idle.particle(10.65, { particle: "minecraft:snowflake", locator: "belly", count: 6, spread: [0.25, 0.04, 0.25], velocity: [0, 0.03, 0] });
  idle.particle(11.0, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.1, 0.05, 0.1], velocity: [0, 0.02, 0] });

  // ------------------------------------------------------------------------------------------------ walk (1.0 s)
  // galumph, galumph: front lifts, back follows, flippers paddle
  const walk = b.anim("walk", 1.0, true);
  walk.pos("seal", [[0, Z0], [0.2, [0, 2.2, 0]], [0.45, [0, -0.1, 0]], [0.5, Z0], [0.7, [0, 2.2, 0]], [0.95, [0, -0.1, 0]], [1.0, Z0]]);
  walk.rot("body", [[0, [-3, 0, 0]], [0.12, [10, 0, 0]], [0.32, [-6, 0, 0]], [0.5, [-3, 0, 0]], [0.62, [10, 0, 0]], [0.82, [-6, 0, 0]], [1.0, [-3, 0, 0]]]);
  walk.scale("body", [[0, [1.04, 0.94, 1]], [0.18, [0.97, 1.05, 1]], [0.45, [1.04, 0.94, 1]], [0.5, [1.04, 0.94, 1]], [0.68, [0.97, 1.05, 1]], [0.95, [1.04, 0.94, 1]], [1.0, [1.04, 0.94, 1]]]);
  walk.rot("head", [[0, [-4, 0, 0]], [0.2, [6, 0, 3]], [0.5, [-4, 0, 0]], [0.7, [6, 0, -3]], [1.0, [-4, 0, 0]]]);
  const paddle = (sx) => [[0, [0, 25 * sx, 0]], [0.25, [0, -30 * sx, 0]], [0.5, [0, 25 * sx, 0]], [0.75, [0, -30 * sx, 0]], [1.0, [0, 25 * sx, 0]]];
  walk.rot("fin_r", paddle(1));
  walk.rot("fin_l", paddle(-1));
  walk.rot("tail", [[0, [6, 0, 0]], [0.25, [-10, 0, 0]], [0.5, [6, 0, 0]], [0.75, [-10, 0, 0]], [1.0, [6, 0, 0]]]);
  walk.particle(0.45, { particle: "minecraft:snowflake", locator: "belly", count: 2, spread: [0.2, 0.02, 0.2], velocity: [0, 0.02, 0] });
  walk.particle(0.95, { particle: "minecraft:snowflake", locator: "belly", count: 2, spread: [0.2, 0.02, 0.2], velocity: [0, 0.02, 0] });

  // ------------------------------------------------------------------------------------------------ sprint (0.5 s)
  // fast belly scoot: flat and stretched, flippers whirl, quick little bounces, snow spray, happy squint
  const sprint = b.anim("sprint", 0.5, true);
  sprint.pos("seal", [[0, Z0], [0.125, [0, 1.1, 0]], [0.25, Z0], [0.375, [0, 1.1, 0]], [0.5, Z0]]);
  sprint.rot("body", [[0, [-5, 0, -3]], [0.25, [-5, 0, 3]], [0.5, [-5, 0, -3]]]);
  sprint.scale("body", [[0, [0.98, 0.9, 1.08]], [0.125, [0.96, 0.94, 1.1]], [0.25, [0.98, 0.9, 1.08]], [0.375, [0.96, 0.94, 1.1]], [0.5, [0.98, 0.9, 1.08]]]);
  sprint.rot("head", [[0, [10, 0, -3]], [0.25, [10, 0, 3]], [0.5, [10, 0, -3]]]);
  sprint.pos("head", [[0, [0, -0.6, -0.4]], [0.5, [0, -0.6, -0.4]]]);
  const whirl = (sx, ph) => {
    const a = (ph ? -45 : 35) * sx, c = (ph ? 35 : -45) * sx;
    return [[0, [0, a, -6 * sx]], [0.125, [0, c, 2 * sx]], [0.25, [0, a, -6 * sx]], [0.375, [0, c, 2 * sx]], [0.5, [0, a, -6 * sx]]];
  };
  sprint.rot("fin_r", whirl(1, 0));
  sprint.rot("fin_l", whirl(-1, 1));
  sprint.rot("tail", [[0, [-16, 8, 0]], [0.25, [-16, -8, 0]], [0.5, [-16, 8, 0]]]);
  sprint.rot("fluke_r", [[0, [0, 12, 0]], [0.125, [0, -6, 0]], [0.25, [0, 12, 0]], [0.375, [0, -6, 0]], [0.5, [0, 12, 0]]]);
  sprint.rot("fluke_l", [[0, [0, -12, 0]], [0.125, [0, 6, 0]], [0.25, [0, -12, 0]], [0.375, [0, 6, 0]], [0.5, [0, -12, 0]]]);
  sprint.pos("happy", [[0, OUT, "step"], [0.5, OUT, "step"]], "step");
  sprint.particle(0.0, { particle: "minecraft:snowflake", locator: "belly", count: 3, spread: [0.25, 0.02, 0.15], velocity: [0, 0.03, 0.05] });
  sprint.particle(0.25, { particle: "minecraft:snowflake", locator: "belly", count: 3, spread: [0.25, 0.02, 0.15], velocity: [0, 0.03, 0.05] });

  // ------------------------------------------------------------------------------------------------ glide (2 s)
  // superman: stretched out, flippers spread like little wings, tail up, banking gently, eyes squeezed with joy
  const glide = b.anim("glide", 2, true);
  glide.pos("seal", [[0, [0, 2, 0]], [1, [0, 3.2, 0]], [2, [0, 2, 0]]]);
  glide.rot("body", [[0, [-8, 0, -6]], [1, [-6, 0, 6]], [2, [-8, 0, -6]]]);
  glide.scale("body", [[0, [0.98, 0.94, 1.06]], [2, [0.98, 0.94, 1.06]]]);
  glide.rot("head", [[0, [16, 0, 4]], [1, [14, 0, -4]], [2, [16, 0, 4]]]);
  const spread = (sx) => [[0, [0, 30 * sx, 14 * sx]], [0.5, [0, 30 * sx, 22 * sx]], [1, [0, 30 * sx, 14 * sx]], [1.5, [0, 30 * sx, 6 * sx]], [2, [0, 30 * sx, 14 * sx]]];
  glide.rot("fin_r", spread(1));
  glide.rot("fin_l", spread(-1));
  glide.rot("tail", [[0, [-18, 0, 0]], [1, [-22, 0, 0]], [2, [-18, 0, 0]]]);
  glide.rot("fluke_r", [[0, [0, 16, 0]], [1, [0, 10, 0]], [2, [0, 16, 0]]]);
  glide.rot("fluke_l", [[0, [0, -16, 0]], [1, [0, -10, 0]], [2, [0, -16, 0]]]);
  glide.pos("happy", [[0, OUT, "step"], [2, OUT, "step"]], "step");

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

  // ------------------------------------------------------------------------------------------------ happy (2.4 s)
  // joy while the owner plays an emote: sits up, hop-hop with a clap above the head, wiggles, hearts
  const happy = b.anim("happy", 2.4, true);
  const hop = [];
  for (let k = 0; k < 4; k++) hop.push([k * 0.6, Z0], [k * 0.6 + 0.3, [0, 3.4, 0]]);
  hop.push([2.4, Z0]);
  happy.pos("seal", hop);
  const sq = [];
  for (let k = 0; k < 4; k++) sq.push([k * 0.6, [1.06, 0.9, 1.04]], [k * 0.6 + 0.15, [0.96, 1.06, 0.98]], [k * 0.6 + 0.45, [1, 1, 1]]);
  sq.push([2.4, [1.06, 0.9, 1.04]]);
  happy.scale("body", sq);
  happy.rot("body", [[0, [24, 0, 0]], [2.4, [24, 0, 0]]]);
  happy.rot("head", [[0, [-10, 0, 10]], [0.6, [-10, 0, -10]], [1.2, [-10, 0, 10]], [1.8, [-10, 0, -10]], [2.4, [-10, 0, 10]]]);
  const UP = [0, 150, 30], CLAP = [0, 178, 30];
  const clapUp = (sx) => {
    const k = [];
    for (let i = 0; i < 4; i++) k.push([i * 0.6, M(UP, sx)], [i * 0.6 + 0.3, M(CLAP, sx)]);
    k.push([2.4, M(UP, sx)]);
    return k;
  };
  happy.rot("fin_r", clapUp(1));
  happy.rot("fin_l", clapUp(-1));
  const tw = [];
  for (let i = 0; i <= 8; i++) tw.push([i * 0.3, [-10, i % 2 ? -10 : 10, 0]]);
  happy.rot("tail", tw);
  happy.rot("fluke_r", wag(1, 2.4, 0.15, 16, -6));
  happy.rot("fluke_l", wag(-1, 2.4, 0.15, 16, -6));
  happy.pos("happy", [[0, OUT, "step"], [2.4, OUT, "step"]], "step");
  happy.particle(0.3, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.15, 0.05, 0.15], velocity: [0, 0.03, 0] });
  happy.particle(1.5, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.15, 0.05, 0.15], velocity: [0, 0.03, 0] });
  happy.sound(0.3, "squeak", { volume: 0.2, pitch: 1.3 });

  // ------------------------------------------------------------------------------------------------ idle_long (6 s)
  // nap: lies flat with the head on its flippers, eyes shut, slow deep breaths, Zs drift up and spin
  const nap = b.anim("idle_long", 6, true);
  const deep = [];
  for (let t = 0; t < 6; t += 1.5) deep.push([t, [1.03, 0.9, 1.02]], [t + 0.75, [1.06, 0.95, 1.03]]);
  deep.push([6, [1.03, 0.9, 1.02]]);
  nap.scale("body", deep);
  nap.rot("body", [[0, [-3, 0, 0]], [6, [-3, 0, 0]]]);
  nap.rot("head", [[0, [-14, 0, 12]], [3, [-15, 0, 13]], [6, [-14, 0, 12]]]);
  nap.pos("head", [[0, [0, -2.4, 0.6]], [6, [0, -2.4, 0.6]]]);
  const pillow = (sx) => [[0, [0, -55 * sx, -6 * sx]], [6, [0, -55 * sx, -6 * sx]]];
  nap.rot("fin_r", pillow(1));
  nap.rot("fin_l", pillow(-1));
  nap.rot("tail", [[0, [4, 0, 0]], [3, [6, 0, 0]], [6, [4, 0, 0]]]);
  nap.rot("fluke_r", [[0, [0, 4, 0]], [3, [0, 10, 0]], [6, [0, 4, 0]]]);
  nap.rot("fluke_l", [[0, [0, -4, 0]], [3, [0, -10, 0]], [6, [0, -4, 0]]]);
  nap.pos("lids", [[0, OUT, "step"], [6, OUT, "step"]], "step");
  // Zs: rise from above the head, grow and spin, then vanish (they rest hidden inside the body)
  const zStart = { z1: 0.4, z2: 2.4, z3: 4.4 };
  for (const [z, t0] of Object.entries(zStart)) {
    const pos = [[0, Z0, "step"]], sc = [[0, [1, 1, 1], "step"]], rot = [[0, Z0, "step"]];
    pos.push([t0, [2, 12, -10]], [t0 + 0.6, [3.5, 16, -9.5]], [t0 + 1.5, [6, 22, -8], "step"], [t0 + 1.51, Z0, "step"], [6, Z0, "step"]);
    sc.push([t0, [0.4, 0.4, 0.4]], [t0 + 0.6, [1.1, 1.1, 1.1]], [t0 + 1.5, [1.5, 1.5, 1.5], "step"], [t0 + 1.51, [1, 1, 1], "step"], [6, [1, 1, 1], "step"]);
    rot.push([t0, [0, -25, 8], "linear"], [t0 + 1.5, [0, 35, -8], "step"], [t0 + 1.51, Z0, "step"], [6, Z0, "step"]);
    nap.pos(z, pos, "catmullrom").scale(z, sc, "catmullrom").rot(z, rot, "linear");
    nap.visible(0, z, false).visible(t0, z, true).visible(t0 + 1.5, z, false);
  }
  return b;
}
