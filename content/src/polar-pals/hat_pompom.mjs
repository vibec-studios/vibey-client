// Polar Pals · Pompom Beanie – a chunky knitted beanie with a Nordic snowflake band, ear flaps with braids and a big
// fluffy pompom on a springy knot. The pompom boings, bounces while walking, streams back while flying.
import { K, P, cosmetic, fur, rim, each, ellipse, stroke, mix, rgba, hash, vnoise } from "./_polar.mjs";

const Z0 = [0, 0, 0];

/** Knitted stitches ("v" columns) over a colour function col(u, v). Optional vertical ribs. */
function knit(img, col, o = {}) {
  const sw = o.sw ?? 1, sh = o.sh ?? 0.75; // stitch size in UV units
  each(img, (u, v) => {
    const cu = Math.floor(u / sw), cv = Math.floor(v / sh);
    const fx = (u / sw) % 1, fy = (v / sh) % 1;
    const c = rgba(col(cu * sw + sw / 2, cv * sh + sh / 2, cu, cv));
    const vshape = 1 - Math.abs(Math.abs(fx - 0.5) * 2 - fy); // bright along the "v"
    let f = 0.84 + 0.22 * vshape - 0.06 * fy + (hash(cu, cv, o.seed ?? 1) - 0.5) * 0.05;
    if (o.ribs) f *= 0.9 + 0.12 * Math.cos(fx * Math.PI * 2);
    return [c[0] * f, c[1] * f, c[2] * f, 255];
  });
  return img;
}

/** Nordic band: white stripe with a teal snowflake / zigzag pattern (in stitch cells). */
const NORDIC = [
  "#...",
  ".#.#",
  "..#.",
];

/** Crown colours: purple top, then a teal line, a zigzag band (teal on snow white) and a teal line. */
function beanieColor(bandTop) {
  return (u, v, cu, cv) => {
    const row = cv - bandTop;
    if (row === 0 || row === 4) return P.teal;
    if (row > 0 && row < 4) {
      const r = NORDIC[row - 1];
      return r[((cu % r.length) + r.length) % r.length] === "#" ? P.tealDark : P.snow;
    }
    return mix(P.brand, P.brandDark, Math.min(1, cv / 8));
  };
}

function fluff(img, seed, base = "#ffffff") {
  each(img, (u, v) => {
    const n = vnoise(u * 3, v * 3, seed) * 0.6 + hash(Math.floor(u * K), Math.floor(v * K), seed) * 0.4;
    // confetti yarn: snow white with teal and purple fibres
    let c = mix(base, "#ebe6fb", n * 0.4);
    const f = hash(Math.floor(u * K * 0.7), Math.floor(v * K * 1.4), seed + 3);
    if (f > 0.86) c = mix(c, P.teal, 0.75);
    else if (f < 0.12) c = mix(c, P.brand, 0.7);
    return c;
  });
  return rim(img, 0.08, 0.5);
}

export default function () {
  const b = cosmetic("polar_pompom_beanie", {
    name: { en: "Polar Pals · Pompom Beanie", de: "Polar Pals · Bommelmütze" },
    description: {
      en: "A chunky knitted beanie with a Nordic snowflake band, braided ear flaps and a big fluffy pompom that boings, bounces while you walk and streams back when you fly.",
      de: "Eine dicke Strickmütze mit nordischem Schneeflocken-Band, geflochtenen Ohrenklappen und einem großen flauschigen Bommel, der wippt, beim Gehen hüpft und im Flug nach hinten weht.",
    },
    tags: ["polar-pals", "hat", "beanie", "knit", "animated"],
    rarity: "rare",
    cosmetic: { slot: "hat", hideWithArmor: ["head"] },
    preview: {
      panels: (p) => [
        { ...p[0], label: "front" },
        { ...p[1], label: "back" },
        { ...p[2], label: "close · boing" },
        { ...p[2], label: "close · fly", anims: [{ name: "fly", t: 0.2 }], time: 0.2, cam: { yaw: 70, pitch: 14 } },
      ],
      sheet: { cols: 4, panelW: 280, panelH: 330 },
      t: 0.35,
    },
  });
  b.texture("main", { hd: K });

  const hat = b.bone("beanie", { attach: "head", pivot: [0, 24, 0] });
  // crown with the band (band sits just above the cuff)
  const crownSides = (img) => rim(knit(img, beanieColor(0), { seed: 3 }), 0.1, 0.6);
  const crownTop = (img) => knit(img, (u, v) => mix(P.brand, P.brandDark, Math.hypot(u - 4, v - 4) / 6), { seed: 4 });
  // (the cuff ends just above the eyes: the face stays free)
  hat.box([-4, 31, -4], [8, 4, 8], { sides: (img) => rim(knit(img, beanieColor(3), { sh: 0.5, seed: 3 }), 0.1, 0.6), up: crownTop, down: crownTop }, { inflate: 0.6 });
  hat.box([-3, 35, -3], [6, 1, 6], { all: (img) => knit(img, () => mix(P.brand, P.brandDark, 0.25), { seed: 5 }), up: crownTop }, { inflate: 0.5 });
  // folded cuff with ribs
  hat.box([-4, 29, -4], [8, 2, 8], { all: (img) => rim(knit(img, () => P.teal, { ribs: true, sw: 0.75, sh: 0.5, seed: 6 }), 0.14, 0.5) }, { inflate: 0.75 });

  // ear flaps + braids
  for (const [side, s] of [["r", 1], ["l", -1]]) {
    const flap = b.bone(`flap_${side}`, { parent: "beanie", pivot: [s * 4.9, 29, 0] });
    flap.box(s > 0 ? [4.5, 25, -2] : [-5.5, 25, -2], [1, 4, 4], { all: (img) => rim(knit(img, (u, v) => (v > 3 ? P.teal : P.brand), { sh: 0.5, seed: 7 }), 0.12, 0.5) }, { inflate: 0.25 });
    // braids hang in front of the shoulders (z < −2), so they never cut into the arm
    const braid = b.bone(`braid_${side}`, { parent: flap.name, pivot: [s * 5, 25, -2.2] });
    braid.box(s > 0 ? [4.5, 21, -3] : [-5.5, 21, -3], [1, 4, 1], {
      all: (img) => {
        each(img, (u, v) => (Math.floor(v * 1.6 + (u > 0.5 ? 0.5 : 0)) % 2 ? P.brandLight : P.snow));
        return rim(img, 0.15, 0.3);
      },
    }, { inflate: 0.1 });
    braid.box(s > 0 ? [4, 18.5, -3.5] : [-6, 18.5, -3.5], [2, 3, 2], { all: (img) => fluff(img, 20 + s) }, { inflate: 0.15 });
  }

  // springy knot + pompom
  const knot = b.bone("knot", { parent: "beanie", pivot: [0, 36, 0] });
  knot.box([-1, 36, -1], [2, 1, 2], { all: (img) => knit(img, () => P.brandDark, { seed: 8 }) });
  const pom = b.bone("pompom", { parent: "knot", pivot: [0, 37, 0] });
  const Y = 37.5;
  pom.box([-2.5, Y, -2.5], [5, 4, 5], { all: (img) => fluff(img, 30) }, { inflate: 0.3 });
  pom.box([-2, Y + 4, -2], [4, 1, 4], { all: (img) => fluff(img, 31) }, { inflate: 0.2 });
  pom.box([-2, Y - 1, -2], [4, 1, 4], { all: (img) => fluff(img, 32) }, { inflate: 0.2 });
  pom.box([-3.5, Y + 0.5, -2], [1, 3, 4], { all: (img) => fluff(img, 33) }, { inflate: 0.2 });
  pom.box([2.5, Y + 0.5, -2], [1, 3, 4], { all: (img) => fluff(img, 34) }, { inflate: 0.2 });
  pom.box([-2, Y + 0.5, -3.5], [4, 3, 1], { all: (img) => fluff(img, 35) }, { inflate: 0.2 });
  pom.box([-2, Y + 0.5, 2.5], [4, 3, 1], { all: (img) => fluff(img, 36) }, { inflate: 0.2 });
  b.locator("pompom", "pompom", [0, Y + 5.5, 0]);

  // idle: every 3 s a little "boing" (damped wobble), squash and stretch
  const idle = b.anim("idle", 3, true);
  idle.rot("knot", [[0, Z0], [0.12, [6, 0, 10]], [0.32, [-4, 0, -7]], [0.52, [3, 0, 4]], [0.75, [-1, 0, -2]], [1.0, Z0], [2.0, [1, 0, 1]], [3, Z0]]);
  idle.scale("pompom", [[0, [1, 1, 1]], [0.1, [1.12, 0.86, 1.12]], [0.25, [0.94, 1.1, 0.94]], [0.42, [1.04, 0.96, 1.04]], [0.6, [1, 1, 1]], [3, [1, 1, 1]]]);
  idle.rot("braid_r", [[0, Z0], [1.5, [0, 0, 3]], [3, Z0]]);
  idle.rot("braid_l", [[0, Z0], [1.5, [0, 0, -3]], [3, Z0]]);
  idle.particle(0.15, { particle: "minecraft:snowflake", locator: "pompom", count: 2, spread: [0.08, 0.03, 0.08], velocity: [0, 0.01, 0] });

  // walk: bounce with the steps (MC walk cycle ≈ 0.6 s per two steps)
  const walk = b.anim("walk", 0.6, true);
  walk.rot("knot", [[0, [10, 0, 6]], [0.15, [-4, 0, -2]], [0.3, [10, 0, -6]], [0.45, [-4, 0, 2]], [0.6, [10, 0, 6]]]);
  walk.scale("pompom", [[0, [1.08, 0.9, 1.08]], [0.15, [0.96, 1.06, 0.96]], [0.3, [1.08, 0.9, 1.08]], [0.45, [0.96, 1.06, 0.96]], [0.6, [1.08, 0.9, 1.08]]]);
  walk.rot("braid_r", [[0, [-14, 0, 8]], [0.3, [10, 0, 4]], [0.6, [-14, 0, 8]]]);
  walk.rot("braid_l", [[0, [10, 0, -4]], [0.3, [-14, 0, -8]], [0.6, [10, 0, -4]]]);
  walk.rot("flap_r", [[0, [0, 0, 4]], [0.3, [0, 0, 0]], [0.6, [0, 0, 4]]]);
  walk.rot("flap_l", [[0, [0, 0, 0]], [0.3, [0, 0, -4]], [0.6, [0, 0, 0]]]);

  // fly: everything streams back in the wind and flutters
  const fly = b.anim("fly", 0.4, true);
  fly.rot("knot", [[0, [42, 0, 4]], [0.2, [48, 0, -4]], [0.4, [42, 0, 4]]]);
  fly.scale("pompom", [[0, [0.95, 1.08, 0.95]], [0.4, [0.95, 1.08, 0.95]]]);
  fly.rot("braid_r", [[0, [-60, 0, 10]], [0.2, [-70, 0, 4]], [0.4, [-60, 0, 10]]]);
  fly.rot("braid_l", [[0, [-70, 0, -4]], [0.2, [-60, 0, -10]], [0.4, [-70, 0, -4]]]);
  fly.rot("flap_r", [[0, [-20, 0, 6]], [0.4, [-20, 0, 6]]]);
  fly.rot("flap_l", [[0, [-20, 0, -6]], [0.4, [-20, 0, -6]]]);
  return b;
}
