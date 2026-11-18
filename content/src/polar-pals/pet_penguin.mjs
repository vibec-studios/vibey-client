// Polar Pals · Penguin – a round penguin chick in a knitted Vibe scarf. Waddles with wiggling flippers, belly-slides
// every few steps, and now and then shows you a fish, tosses it up and gulps it down.
import { K, P, cosmetic, fur, rim, edges, each, ellipse, blush, eye, closedEye, stroke, arc, mix, rgba, petPreview } from "./_polar.mjs";

const L = 16;
const Z0 = [0, 0, 0];

/** Black/white body painter: dark back, white front band on the side faces (front = −Z). */
function bodyPaint(seed, o = {}) {
  const white = o.white ?? 0; // UV units of white towards the front on east/west faces
  const side = (img, f, face) => {
    fur(img, { top: P.pengBack2, bottom: P.pengBack, seed: seed + face.length, strands: 0.05, grain: 0.03 });
    if (white && (face === "east" || face === "west")) {
      const w = img.width / K, h = img.height / K;
      each(img, (u, v) => {
        const fromFront = face === "east" ? w - u : u; // east: u = max is the front
        const edge = white - 0.6 * Math.sin((v / h) * Math.PI);
        if (fromFront > edge) return null;
        return mix(P.pengBelly, P.pengBellyShade, v / h);
      });
    }
    if (o.north && face === "north") o.north(img);
    return rim(img, 0.14, 1);
  };
  return {
    up: (img) => rim(fur(img, { top: P.pengBack2, bottom: P.pengBack, seed, strands: 0.05 }), 0.12),
    down: (img) => rim(fur(img, { top: o.downWhite ? P.pengBellyShade : P.pengBack, seed: seed + 1 }), 0.1),
    sides: side,
  };
}

const knit = (img, a, b2, seed = 1) => {
  // knitted rows: little "v" stitches, alternating stripes a / b2
  const W = img.width, H = img.height;
  for (let y = 0; y < H; y++)
    for (let x = 0; x < W; x++) {
      const row = Math.floor(y / (K * 0.75));
      const c = Math.floor(row / 2) % 2 ? b2 : a;
      const sx = (x % K) / K, sy = (y % (K * 0.75)) / (K * 0.75);
      const v = Math.abs(sx - 0.5) * 2; // 0 centre .. 1 edge
      const stitch = 0.9 + 0.18 * (1 - Math.abs(v - sy)) - 0.08 * sy;
      const n = 1 + ((x * 7 + y * 13 + seed) % 5) * 0.01;
      const C = rgba(c);
      img.set(x, y, [C[0] * stitch * n, C[1] * stitch * n, C[2] * stitch * n, 255]);
    }
  return img;
};

export default function () {
  const b = cosmetic("polar_penguin", {
    name: { en: "Polar Pals · Penguin", de: "Polar Pals · Pinguin" },
    description: {
      en: "A chubby penguin chick in a knitted Vibe scarf. Waddles after you, belly-slides every few steps, flaps like mad when you fly – and sometimes shows you a fish before gulping it down.",
      de: "Ein pummeliges Pinguinküken im gestrickten Vibe-Schal. Watschelt dir hinterher, rutscht alle paar Schritte auf dem Bauch, flattert wild, wenn du fliegst – und zeigt dir manchmal einen Fisch, bevor es ihn verschlingt.",
    },
    tags: ["polar-pals", "penguin", "cute", "pet", "animated"],
    rarity: "epic",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 0.42 } },
    preview: petPreview([
      { label: "idle", t: 0.6 },
      { label: "idle · flap", t: 4.5 },
      { label: "idle · fish!", t: 7.6 },
      { label: "idle · toss", t: 8.75 },
      { label: "idle · gulp", t: 9.6 },
      { label: "walk · waddle", anim: "walk", t: 0.3 },
      { label: "walk · belly slide", anim: "walk", t: 1.8, cam: { yaw: 70, pitch: 12 } },
      { label: "fly · flap flap", anim: "fly", t: 0.1 },
      { label: "sneak", anim: "sneak", t: 0.4 },
      { label: "front", t: 0.6, cam: { yaw: 0, pitch: 6 } },
      { label: "back", t: 0.6, cam: { yaw: 200, pitch: 14 } },
    ], { cols: 4 }),
  });
  b.texture("main", { hd: K });

  b.bone("peng", { attach: "world", pivot: [0, 0, 0] });
  const body = b.bone("body", { parent: "peng", pivot: [0, 1, 0] });
  const belly = (img) => {
    const w = img.width / K, h = img.height / K;
    fur(img, { top: P.pengBelly, bottom: P.pengBellyShade, seed: 9, strands: 0.03 });
    return edges(img, { left: 0.1, right: 0.1, bottom: 0.08, top: 0.04 }, 1.2);
  };
  body.box([-5, 2, -4.5], [10, 11, 9], { ...bodyPaint(10, { white: 2.6 }), north: belly });
  body.box([-4, 13, -3.5], [8, 1, 7], bodyPaint(12));
  body.box([-4, 1, -3.5], [8, 1, 7], { ...bodyPaint(13, { downWhite: true }), north: belly });
  body.box([-6, 3, -3.5], [1, 9, 7], bodyPaint(14, { white: 1.8 }));
  body.box([5, 3, -3.5], [1, 9, 7], bodyPaint(15, { white: 1.8 }));
  body.box([-3.5, 3, -5.5], [7, 9, 1], { all: belly, north: (img) => blush(belly(img), 3.5, 2.5, 3, 2.2, "#ffffff", 0.35) });
  body.box([-4, 3, 4.5], [8, 9, 1], bodyPaint(16));
  // tail
  body.box([-1.5, 1.5, 4.5], [3, 2, 2], bodyPaint(17));

  // flippers hang at the sides, pivot at the shoulder
  const flipper = (seed) => ({
    sides: (img) => rim(fur(img, { top: P.pengBack2, bottom: P.pengBack, seed, strands: 0.04 }), 0.18, 0.7),
    up: (img) => fur(img, { top: P.pengBack2, seed: seed + 1 }),
    down: (img) => fur(img, { top: P.pengBackLight, seed: seed + 2 }),
  });
  const wR = b.bone("wing_r", { parent: "body", pivot: [5.6, 12, 0], rotation: [0, 0, 8] });
  wR.box([5.6, 5, -1.5], [1, 7, 3], flipper(20), { inflate: 0.1 });
  const wL = b.bone("wing_l", { parent: "body", pivot: [-5.6, 12, 0], rotation: [0, 0, -8] });
  wL.box([-6.6, 5, -1.5], [1, 7, 3], flipper(22), { inflate: 0.1 });

  // feet
  const footPaint = {
    up: (img) => {
      fur(img, { top: P.feet, bottom: P.feetDark, seed: 30, strands: 0.02 });
      for (const u of [0.75, 1.5, 2.25]) stroke(img, [[u, 2.4], [u, 3.8]], 0.2, P.feetDark, 0.8);
      return img;
    },
    all: (img) => fur(img, { top: P.feet, bottom: P.feetDark, seed: 31, strands: 0.02 }),
  };
  const fR = b.bone("foot_r", { parent: "peng", pivot: [2, 1, 0] });
  fR.box([0.5, 0, -4.5], [3, 1, 4], footPaint, { inflate: 0.05 });
  const fL = b.bone("foot_l", { parent: "peng", pivot: [-2, 1, 0] });
  fL.box([-3.5, 0, -4.5], [3, 1, 4], footPaint, { inflate: 0.05 });

  // knitted scarf around the neck + a hanging end with fringe
  const scarf = b.bone("scarf", { parent: "body", pivot: [0, 13, 0] });
  const scarfPaint = { all: (img) => rim(knit(img, P.teal, P.brand, 3), 0.18, 0.6) };
  scarf.box([-5.5, 12, -5], [11, 2, 9], scarfPaint, { inflate: 0.35 });
  const tail = b.bone("scarf_end", { parent: "scarf", pivot: [3.2, 13, -5.4] });
  tail.box([2.2, 7, -5.9], [2, 6, 1], {
    all: (img) => knit(img, P.teal, P.brand, 5),
    north: (img) => {
      knit(img, P.teal, P.brand, 7);
      // fringe at the bottom
      const h = img.height / K;
      each(img, (u, v) => (v > h - 1 && Math.floor(u * 4) % 2 ? [0, 0, 0, 0] : null));
      return img;
    },
  });

  // ------------------------------------------------------------------------------------------------ head
  const head = b.bone("head", { parent: "body", pivot: [0, 13.5, 0] });
  const mask = (u, v) => {
    // heart-shaped white face: two lobes around the eyes + rounded chin
    const l1 = ((u - 2.6) / 2.3) ** 2 + ((v - 4.1) / 2.6) ** 2 <= 1;
    const l2 = ((u - 6.4) / 2.3) ** 2 + ((v - 4.1) / 2.6) ** 2 <= 1;
    const chin = ((u - 4.5) / 3.6) ** 2 + ((v - 5.6) / 2.5) ** 2 <= 1;
    return l1 || l2 || chin;
  };
  const face = (img) => {
    fur(img, { top: P.pengBack2, bottom: P.pengBack, seed: 40, strands: 0.04 });
    each(img, (u, v) => {
      let n = 0;
      for (let j = 0; j < 3; j++) for (let i = 0; i < 3; i++) if (mask(u + (i - 1) / (3 * K), v + (j - 1) / (3 * K))) n++;
      if (!n) return null;
      const c = rgba(mix(P.pengBelly, P.pengBellyShade, Math.max(0, v - 4) / 4));
      return [c[0], c[1], c[2], (255 * n) / 9];
    });
    blush(img, 1.35, 5.4, 1.0, 0.65, P.blush, 0.6);
    blush(img, 7.65, 5.4, 1.0, 0.65, P.blush, 0.6);
    eye(img, 2.6, 3.7, 1.25, 1.5);
    eye(img, 6.4, 3.7, 1.25, 1.5, { mirror: true });
    return rim(img, 0.12, 0.9);
  };
  head.box([-4.5, 13, -4], [9, 8, 8], { ...bodyPaint(41), north: face });
  head.box([-3.5, 21, -3], [7, 1, 6], bodyPaint(42));
  head.box([-5.5, 14, -3], [1, 6, 6], bodyPaint(43));
  head.box([4.5, 14, -3], [1, 6, 6], bodyPaint(44));
  head.box([-3.5, 14, 4], [7, 6, 1], bodyPaint(45));
  // a little tuft of fluff on top
  const tuft = (img) => {
    each(img, () => [0, 0, 0, 0]);
    stroke(img, [[1.5, 2.9], [1.2, 1.6], [0.6, 0.6]], 0.45, P.pengBack2);
    stroke(img, [[1.6, 2.9], [2.2, 1.3], [2.6, 0.5]], 0.4, P.pengBackLight);
    return img;
  };
  head.box([-1.5, 21.5, -1], [3, 3, 0], { north: tuft, south: tuft });
  // beak (upper + lower)
  const beakPaint = (top, bottom, seed) => ({
    all: (img) => rim(fur(img, { top, bottom, seed, strands: 0.02, grain: 0.02 }), 0.12, 0.5),
    up: (img) => {
      fur(img, { top, bottom, seed, strands: 0.02, grain: 0.02 });
      ellipse(img, 0.9, 1.5, 0.25, 0.18, "#ffe1a8", 0.9);
      return img;
    },
  });
  head.box([-1.5, 15.5, -6.5], [3, 1, 3], beakPaint(P.beak, P.beakDark, 50), { inflate: 0.05 });
  const jaw = b.bone("jaw", { parent: "head", pivot: [0, 15.4, -4] });
  jaw.box([-1, 14.6, -6], [2, 1, 2], beakPaint(P.beakDark, "#c96f1c", 51));

  // eyelids (hidden inside the head at rest, idle pushes them out): blink + happy
  const lidPlane = (kind) => (img) => {
    each(img, () => [0, 0, 0, 0]);
    ellipse(img, 1.5, 1.6, 1.35, 1.6, mix(P.pengBelly, P.pengBellyShade, 0.15));
    closedEye(img, 1.5, kind === "happy" ? 1.6 : 1.8, 2.0, kind, P.eye, 0.32);
    return img;
  };
  for (const [name, kind] of [["lids", "sleep"], ["happy", "happy"]]) {
    const lid = b.bone(name, { parent: "head", pivot: [0, 17, -4] });
    lid.box([0.4, 15.7, -3.9], [3, 3, 0], { north: lidPlane(kind) });
    lid.box([-3.4, 15.7, -3.9], [3, 3, 0], { north: lidPlane(kind) });
  }

  // the fish (inside the head at rest; idle moves it into the beak)
  const fish = b.bone("fish", { parent: "head", pivot: [0, 17, 0] });
  const fishSide = (img, f, face) => {
    const w = img.width / K, h = img.height / K;
    fur(img, { top: P.fishDark, bottom: P.fishLight, seed: 60, strands: 0.02 });
    // scales
    for (let u = 0.6; u < w - 0.4; u += 0.8) for (const v of [0.6, 1.3]) stroke(img, arc(u, v, 0.35, 0.3, 20, 160, 5), 0.12, P.fishDark, 0.45);
    const headU = face === "north" ? 0.6 : w - 0.6; // fish head is at −X: north face u = max is −X … mirrored on south
    ellipse(img, face === "north" ? w - 0.7 : 0.7, 0.75, 0.32, 0.32, "#ffffff");
    ellipse(img, face === "north" ? w - 0.7 : 0.7, 0.78, 0.18, 0.2, P.eye);
    return img;
  };
  fish.box([-3, 16, -0.5], [4, 2, 1], { north: fishSide, south: fishSide, up: (img) => fur(img, { top: P.fishDark, seed: 61 }), down: (img) => fur(img, { top: P.fishLight, seed: 62 }), sides: (img) => fur(img, { top: P.fish, bottom: P.fishLight, seed: 63 }) }, { inflate: 0.1 });
  const finTex = (img) => {
    each(img, () => [0, 0, 0, 0]);
    const w = img.width / K, h = img.height / K;
    each(img, (u, v) => (Math.abs(v - h / 2) <= (u / w) * (h / 2) + 0.15 ? mix(P.fishFin, "#ff8a3c", u / w) : null));
    return img;
  };
  fish.box([1, 15.5, 0], [2, 3, 0], { north: finTex, south: finTex });

  b.locator("heart", "head", [0, 23, -3]);
  b.locator("beak", "head", [0, 15.5, -7]);
  b.locator("feet", "peng", [0, 0.5, -1]);

  // ------------------------------------------------------------------------------------------------ idle (16 s)
  const idle = b.anim("idle", L, true);
  idle.rot("head", [
    [0, Z0], [1.5, [3, 0, 0]], [2.0, [0, 22, 6]], [2.9, [0, 22, 6]], [3.4, Z0],
    [3.6, [6, 0, 0]], [4.0, [6, 0, -6]], [4.4, [6, 0, 6]], [4.8, [6, 0, -6]], [5.2, [6, 0, 0]], [5.6, Z0],
    [6.4, Z0], [6.7, [-4, 0, 0]], [7.1, [2, -14, 12]], [8.2, [2, -14, 12]], [8.5, [-8, 0, 0]], [8.75, [24, 0, 0]], [9.05, [28, 0, 0]], [9.3, [-10, 0, 0]], [9.5, [0, 0, 0]], [9.7, [-6, 0, 0]], [9.9, Z0],
    [10.6, [-22, 0, 0]], [11.6, [-22, 6, 0]], [12.1, Z0], [13, [0, 0, 8]], [13.5, [0, 0, -8]], [14, [0, 0, 8]], [14.5, [0, 0, -8]], [15, Z0], [L, Z0],
  ]);
  idle.rot("jaw", [[0, Z0], [6.5, Z0], [6.6, [18, 0, 0]], [6.75, Z0], [8.5, Z0], [8.6, [26, 0, 0]], [9.1, [26, 0, 0]], [9.25, Z0], [9.45, [12, 0, 0]], [9.6, Z0], [9.75, [12, 0, 0]], [9.9, Z0], [L, Z0]]);
  idle.rot("body", [[0, Z0], [1, [0, 0, 0]], [3.4, Z0], [13, [0, 0, -5]], [13.5, [0, 0, 5]], [14, [0, 0, -5]], [14.5, [0, 0, 5]], [15, Z0], [L, Z0]]);
  const breath = [];
  for (let t = 0; t < L; t += 2) breath.push([t, [1, 1, 1]], [t + 1, [1.02, 1.025, 1.02]]);
  breath.push([L, [1, 1, 1]]);
  idle.scale("body", breath);
  idle.pos("peng", [[0, Z0], [3.6, Z0], [3.8, [0, 1.6, 0]], [4.0, Z0], [4.4, [0, 1.6, 0]], [4.6, Z0], [9.3, Z0], [9.5, [0, 1.4, 0]], [9.7, Z0], [L, Z0]]);
  const flapK = (sx) => [
    [0, Z0], [3.5, Z0], [3.7, [0, 0, 55 * sx]], [3.85, [0, 0, 20 * sx]], [4.0, [0, 0, 60 * sx]], [4.15, [0, 0, 20 * sx]], [4.3, [0, 0, 60 * sx]], [4.45, [0, 0, 20 * sx]], [4.6, [0, 0, 55 * sx]], [5.0, Z0],
    [6.4, Z0], [7.0, [-20, 0, 20 * sx]], [8.2, [-20, 0, 20 * sx]], [8.5, [0, 0, 40 * sx]], [9.4, [0, 0, 40 * sx]], [9.55, [0, 0, 65 * sx]], [9.7, [0, 0, 30 * sx]], [9.85, [0, 0, 65 * sx]], [10.2, Z0],
    [10.6, [-25, 0, 5 * sx]], [11.6, [-25, 0, 5 * sx]], [12.1, Z0], [L, Z0],
  ];
  idle.rot("wing_r", flapK(1));
  idle.rot("wing_l", flapK(-1));
  idle.rot("scarf_end", [[0, [0, 0, 0]], [2, [6, 0, 4]], [4, [-4, 0, -3]], [4.2, [14, 0, 8]], [4.6, [-6, 0, -4]], [6, Z0], [9.5, [12, 0, 0]], [10, Z0], [12, [5, 0, 4]], [14, [-3, 0, -4]], [L, Z0]]);
  idle.rot("foot_r", [[0, Z0], [13, Z0], [13.25, [0, 0, 0]], [13.5, [-15, 0, 0]], [13.75, Z0], [14.5, [-15, 0, 0]], [14.75, Z0], [L, Z0]]);
  idle.pos("foot_r", [[0, Z0], [13.25, Z0], [13.5, [0, 1, 0]], [13.75, Z0], [14.25, Z0], [14.5, [0, 1, 0]], [14.75, Z0], [L, Z0]]);
  idle.pos("foot_l", [[0, Z0], [13.75, Z0], [14.0, [0, 1, 0]], [14.25, Z0], [14.75, Z0], [15.0, [0, 1, 0]], [15.25, Z0], [L, Z0]]);
  // fish: hidden in the head → in the beak (6.6) → tossed up (8.6) → gulped (9.1–9.3)
  const BEAK = [0, -1.3, -7.6];
  idle.pos("fish", [[0, Z0, "step"], [6.6, BEAK], [8.55, BEAK], [8.8, [0, 7, -6]], [9.05, [0, 1, -6.8]], [9.3, [0, -1.3, -5], "step"], [9.31, Z0, "step"], [L, Z0, "step"]]);
  idle.rot("fish", [[0, Z0, "step"], [6.6, Z0], [7.6, [0, 0, -10]], [8.2, [0, 0, 10]], [8.55, Z0, "linear"], [8.8, [0, 0, 200], "linear"], [9.05, [0, 0, 270]], [9.3, [0, 0, 270], "step"], [9.31, Z0, "step"], [L, Z0, "step"]]);
  idle.scale("fish", [[0, [1, 1, 1], "step"], [9.05, [1, 1, 1]], [9.3, [0.5, 0.5, 0.5], "step"], [9.31, [1, 1, 1], "step"], [L, [1, 1, 1], "step"]]);
  idle.visible(0, "fish", false).visible(6.6, "fish", true).visible(9.3, "fish", false);
  const OUT = [0, 0, -0.3];
  const lidWin = (wins) => {
    const k = [[0, Z0, "step"]];
    for (const [a, c] of wins) k.push([a, OUT, "step"], [c, Z0, "step"]);
    k.push([L, Z0, "step"]);
    return k.sort((x, y) => x[0] - y[0]);
  };
  idle.pos("lids", lidWin([[1.2, 1.33], [5.8, 5.93], [11.0, 11.13], [15.3, 15.43]]), "step");
  idle.pos("happy", lidWin([[3.7, 5.1], [9.3, 10.4]]), "step");
  idle.particle(6.6, { particle: "minecraft:splash", locator: "beak", count: 4, spread: [0.08, 0.04, 0.08], velocity: [0, 0.05, 0] });
  idle.particle(9.3, { particle: "minecraft:heart", locator: "heart", count: 1, spread: [0.1, 0.05, 0.1], velocity: [0, 0.03, 0] });
  idle.sound(9.3, "minecraft:entity.generic.eat", { volume: 0.25, pitch: 1.6 });
  idle.sound(6.6, "minecraft:entity.fish.swim", { volume: 0.2, pitch: 1.5 });

  // ------------------------------------------------------------------------------------------------ walk (2.4 s)
  // four waddle steps, then a dive onto the belly and a slide, hop back up
  const walk = b.anim("walk", 2.4, true);
  const st = 0.3;
  walk.rot("body", [[0, [0, 0, -9]], [st, [0, 0, 9]], [2 * st, [0, 0, -9]], [3 * st, [0, 0, 9]], [1.2, [0, 0, 0]], [1.38, [-84, 0, 0]], [2.15, [-84, 0, 0]], [2.3, [-10, 0, -4]], [2.4, [0, 0, -9]]]);
  walk.pos("body", [[0, Z0], [1.2, Z0], [1.38, [0, 4.2, -1]], [2.15, [0, 4.2, -1]], [2.3, [0, 1.2, 0]], [2.4, Z0]]);
  walk.pos("peng", [[0, Z0], [st / 2, [0, 0.8, 0]], [st, Z0], [1.5 * st, [0, 0.8, 0]], [2 * st, Z0], [2.5 * st, [0, 0.8, 0]], [3 * st, Z0], [3.5 * st, [0, 0.8, 0]], [1.2, Z0], [2.4, Z0]]);
  walk.rot("head", [[0, [0, 0, 6]], [st, [0, 0, -6]], [2 * st, [0, 0, 6]], [3 * st, [0, 0, -6]], [1.2, Z0], [1.38, [60, 0, 0]], [2.15, [60, 0, 0]], [2.3, [8, 0, 0]], [2.4, [0, 0, 6]]]);
  const wig = (sx) => [[0, [0, 0, 28 * sx]], [st / 2, [0, 0, 12 * sx]], [st, [0, 0, 28 * sx]], [1.5 * st, [0, 0, 12 * sx]], [2 * st, [0, 0, 28 * sx]], [2.5 * st, [0, 0, 12 * sx]], [3 * st, [0, 0, 28 * sx]], [1.2, [0, 0, 30 * sx]], [1.38, [0, 0, 8 * sx]], [2.15, [0, 0, 8 * sx]], [2.4, [0, 0, 28 * sx]]];
  walk.rot("wing_r", wig(1));
  walk.rot("wing_l", wig(-1));
  const step = (ph) => {
    const k = [];
    for (let i = 0; i < 4; i++) {
      const t = i * st;
      const up = (i % 2) === ph;
      k.push([t, up ? [0, 1.4, -0.5] : Z0]);
    }
    k.push([1.2, Z0], [1.38, [0, 1.8, 3.5]], [2.15, [0, 1.8, 3.5]], [2.3, [0, 0.5, 0]], [2.4, ph === 0 ? [0, 1.4, -0.5] : Z0]);
    return k;
  };
  walk.pos("foot_r", step(0));
  walk.pos("foot_l", step(1));
  walk.rot("foot_r", [[0, Z0], [1.2, Z0], [1.38, [70, 0, 0]], [2.15, [70, 0, 0]], [2.3, Z0], [2.4, Z0]]);
  walk.rot("foot_l", [[0, Z0], [1.2, Z0], [1.38, [70, 0, 0]], [2.15, [70, 0, 0]], [2.3, Z0], [2.4, Z0]]);
  walk.rot("scarf_end", [[0, [10, 0, 6]], [st, [10, 0, -6]], [2 * st, [10, 0, 6]], [3 * st, [10, 0, -6]], [1.2, [20, 0, 0]], [1.38, [-50, 0, 0]], [2.15, [-60, 0, 8]], [2.4, [10, 0, 6]]]);
  walk.pos("happy", [[0, Z0, "step"], [1.3, OUT, "step"], [2.2, Z0, "step"], [2.4, Z0, "step"]], "step");
  walk.particle(1.4, { particle: "minecraft:snowflake", locator: "feet", count: 4, spread: [0.2, 0.02, 0.2], velocity: [0, 0.03, 0] });
  walk.particle(1.8, { particle: "minecraft:snowflake", locator: "feet", count: 2, spread: [0.2, 0.02, 0.1], velocity: [0, 0.02, 0.03] });

  // ------------------------------------------------------------------------------------------------ fly (0.5 s)
  const fly = b.anim("fly", 0.5, true);
  fly.pos("peng", [[0, Z0], [0.25, [0, 1.2, 0]], [0.5, Z0]]);
  fly.rot("body", [[0, [-8, 0, 0]], [0.5, [-8, 0, 0]]]);
  fly.rot("head", [[0, [14, 0, 0]], [0.25, [10, 0, 0]], [0.5, [14, 0, 0]]]);
  const flutter = (sx) => [[0, [0, 0, 95 * sx]], [0.125, [0, 0, 40 * sx]], [0.25, [0, 0, 95 * sx]], [0.375, [0, 0, 40 * sx]], [0.5, [0, 0, 95 * sx]]];
  fly.rot("wing_r", flutter(1));
  fly.rot("wing_l", flutter(-1));
  fly.rot("foot_r", [[0, [20, 0, 0]], [0.25, [-20, 0, 0]], [0.5, [20, 0, 0]]]);
  fly.rot("foot_l", [[0, [-20, 0, 0]], [0.25, [20, 0, 0]], [0.5, [-20, 0, 0]]]);
  fly.rot("scarf_end", [[0, [-50, 0, 10]], [0.25, [-60, 0, -10]], [0.5, [-50, 0, 10]]]);

  // ------------------------------------------------------------------------------------------------ sneak (2 s)
  const sneak = b.anim("sneak", 2, true);
  sneak.scale("body", [[0, [1.06, 0.88, 1.06]], [1, [1.08, 0.86, 1.08]], [2, [1.06, 0.88, 1.06]]]);
  sneak.pos("head", [[0, [0, -2.2, 0.3]], [2, [0, -2.2, 0.3]]]);
  sneak.rot("head", [[0, [-6, 14, 0]], [1, [-6, -14, 0]], [2, [-6, 14, 0]]]);
  sneak.rot("wing_r", [[0, [0, 40, 12]], [2, [0, 40, 12]]]);
  sneak.rot("wing_l", [[0, [0, -40, -12]], [2, [0, -40, -12]]]);
  return b;
}
