// Polar Pals · Pompom Beanie – a calm, chunky knitted beanie: soft purple crown, a ribbed cuff in a deeper purple
// and one big, clean snow-white pompom on a springy knot. Only three colours, soft shading instead of noise. The
// pompom boings, bounces while walking and streams back while flying.
import { K, P, cosmetic, rim, each, mix, rgba, hash } from "./_polar.mjs";

const Z0 = [0, 0, 0];
const CROWN = "#8f74f2", CROWN_DARK = "#6c52d4", CUFF = "#5a43be", CUFF_DARK = "#463299";
const POM = "#fbfcff", POM_SHADE = "#d9def0";

/**
 * Knitted stitches ("v" columns) over a colour function col(u, v): gentle, low-contrast stitch shading, no colour
 * noise. Optional vertical ribs (cuff).
 */
function knit(img, col, o = {}) {
  const sw = o.sw ?? 1, sh = o.sh ?? 0.75; // stitch size in UV units
  each(img, (u, v) => {
    const cu = Math.floor(u / sw), cv = Math.floor(v / sh);
    const fx = (u / sw) % 1, fy = (v / sh) % 1;
    const c = rgba(col(u, v));
    const vshape = 1 - Math.abs(Math.abs(fx - 0.5) * 2 - fy); // bright along the "v"
    let f = 0.92 + 0.12 * vshape - 0.04 * fy + (hash(cu, cv, o.seed ?? 1) - 0.5) * 0.02;
    if (o.ribs) f *= 0.93 + 0.09 * Math.cos(fx * Math.PI * 2);
    return [c[0] * f, c[1] * f, c[2] * f, 255];
  });
  return img;
}

/**
 * Pompom face: smooth, round-looking shading (bright centre, soft darker rim, a little darker towards the bottom)
 * and a very fine, low-contrast yarn texture – no speckles. light = 0..1 how much this face faces up.
 */
function pom(img, light = 0.5) {
  const W = img.width / K, H = img.height / K;
  each(img, (u, v) => {
    const dx = (u - W / 2) / (W / 2), dy = (v - H / 2) / (H / 2);
    const r = Math.min(1, Math.sqrt(dx * dx + dy * dy) / 1.25);
    let c = mix(POM, POM_SHADE, 0.04 + 0.4 * r * r + (1 - light) * 0.2 + Math.max(0, dy) * 0.1);
    const fib = (hash(Math.floor(u * K), Math.floor(v * K), 7) - 0.5) * 0.035;
    const C = rgba(c);
    return [C[0] * (1 + fib), C[1] * (1 + fib), C[2] * (1 + fib), 255];
  });
  return img;
}

export default function () {
  const b = cosmetic("polar_pompom_beanie", {
    name: { en: "Polar Pals · Pompom Beanie", de: "Polar Pals · Bommelmütze" },
    description: {
      en: "A cosy knitted beanie in soft purple with a ribbed cuff and one big, fluffy snow-white pompom that boings, bounces while you walk and streams back when you fly.",
      de: "Eine kuschelige Strickmütze in sanftem Lila mit Rippbündchen und einem großen, flauschigen schneeweißen Bommel, der wippt, beim Gehen hüpft und im Flug nach hinten weht.",
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
  // crown: a soft vertical gradient, slightly darker towards the cuff
  const crownCol = (h) => (u, v) => mix(CROWN, CROWN_DARK, Math.min(1, v / h) * 0.35);
  const crownTop = (img) => knit(img, (u, v) => mix(CROWN, CROWN_DARK, Math.hypot(u - 4, v - 4) / 9), { seed: 4 });
  // (the cuff ends just above the eyes: the face stays free)
  hat.box([-4, 31, -4], [8, 4, 8], { sides: (img) => rim(knit(img, crownCol(4), { sh: 0.5, seed: 3 }), 0.08, 0.6), up: crownTop, down: crownTop }, { inflate: 0.6 });
  hat.box([-3, 35, -3], [6, 1, 6], { all: (img) => rim(knit(img, () => mix(CROWN, CROWN_DARK, 0.1), { seed: 5 }), 0.06, 0.5), up: crownTop }, { inflate: 0.5 });
  // folded cuff with ribs
  hat.box([-4, 29, -4], [8, 2, 8], { all: (img) => rim(knit(img, (u, v) => mix(CUFF, CUFF_DARK, v / 2 * 0.4), { ribs: true, sw: 0.75, sh: 0.5, seed: 6 }), 0.1, 0.5) }, { inflate: 0.75 });

  // springy knot + pompom (a rounded ball: core + six caps)
  const knot = b.bone("knot", { parent: "beanie", pivot: [0, 36, 0] });
  knot.box([-1, 36, -1], [2, 1, 2], { all: (img) => knit(img, () => CUFF, { seed: 8 }) });
  const pp = b.bone("pompom", { parent: "knot", pivot: [0, 37, 0] });
  const Y = 37.5;
  pp.box([-2.5, Y, -2.5], [5, 4, 5], { sides: (img) => pom(img, 0.5), up: (img) => pom(img, 1), down: (img) => pom(img, 0) }, { inflate: 0.3 });
  pp.box([-2, Y + 4, -2], [4, 1, 4], { sides: (img) => pom(img, 0.8), up: (img) => pom(img, 1), down: (img) => pom(img, 0.6) }, { inflate: 0.2 });
  pp.box([-2, Y - 1, -2], [4, 1, 4], { sides: (img) => pom(img, 0.2), up: (img) => pom(img, 0.4), down: (img) => pom(img, 0) }, { inflate: 0.2 });
  pp.box([-3.5, Y + 0.5, -2], [1, 3, 4], { all: (img) => pom(img, 0.45) }, { inflate: 0.2 });
  pp.box([2.5, Y + 0.5, -2], [1, 3, 4], { all: (img) => pom(img, 0.45) }, { inflate: 0.2 });
  pp.box([-2, Y + 0.5, -3.5], [4, 3, 1], { all: (img) => pom(img, 0.45) }, { inflate: 0.2 });
  pp.box([-2, Y + 0.5, 2.5], [4, 3, 1], { all: (img) => pom(img, 0.45) }, { inflate: 0.2 });
  b.locator("pompom", "pompom", [0, Y + 5.5, 0]);

  // idle: every 3 s a little "boing" (damped wobble), squash and stretch
  const idle = b.anim("idle", 3, true);
  idle.rot("knot", [[0, Z0], [0.12, [6, 0, 10]], [0.32, [-4, 0, -7]], [0.52, [3, 0, 4]], [0.75, [-1, 0, -2]], [1.0, Z0], [2.0, [1, 0, 1]], [3, Z0]]);
  idle.scale("pompom", [[0, [1, 1, 1]], [0.1, [1.12, 0.86, 1.12]], [0.25, [0.94, 1.1, 0.94]], [0.42, [1.04, 0.96, 1.04]], [0.6, [1, 1, 1]], [3, [1, 1, 1]]]);
  idle.particle(0.15, { particle: "minecraft:snowflake", locator: "pompom", count: 1, spread: [0.08, 0.03, 0.08], velocity: [0, 0.01, 0] });

  // walk: bounce with the steps (MC walk cycle ≈ 0.6 s per two steps)
  const walk = b.anim("walk", 0.6, true);
  walk.rot("knot", [[0, [10, 0, 6]], [0.15, [-4, 0, -2]], [0.3, [10, 0, -6]], [0.45, [-4, 0, 2]], [0.6, [10, 0, 6]]]);
  walk.scale("pompom", [[0, [1.08, 0.9, 1.08]], [0.15, [0.96, 1.06, 0.96]], [0.3, [1.08, 0.9, 1.08]], [0.45, [0.96, 1.06, 0.96]], [0.6, [1.08, 0.9, 1.08]]]);

  // fly: the pompom streams back in the wind and flutters
  const fly = b.anim("fly", 0.4, true);
  fly.rot("knot", [[0, [42, 0, 4]], [0.2, [48, 0, -4]], [0.4, [42, 0, 4]]]);
  fly.scale("pompom", [[0, [0.95, 1.08, 0.95]], [0.4, [0.95, 1.08, 0.95]]]);
  return b;
}
