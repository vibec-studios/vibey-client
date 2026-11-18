// Polar Pals · Snowy Owl Wings – large feathered wings built like a real wing: arm → forearm → hand bones, lesser and
// greater coverts as scalloped layers, five secondaries and five primaries as separate feather bones. idle = folded
// on the back with a slow breath, walk = gentle bounce, fly = one big clean flap then a long glide with spread
// "finger" primaries (feathers drift off on the downstroke), glide (elytra) = wide soaring wings, sneak = tucked in
// tight. While a backpack is worn the wings sit further back (cosmetic.offsetWhen), so they never cut into it.
import { K, P, cosmetic, each, shape, stroke, ellipse, rim, mix, rgba, hash, vnoise } from "./_polar.mjs";

const WHITE = "#f8fafd", SHADOW = "#d9e1ec", DEEP = "#b9c6d8", MARK = "#5d6475", TIP = "#c9c2ff", TIP2 = "#a9e8ff";
const Z0 = [0, 0, 0];

const smooth = (a, b, x) => {
  const t = Math.max(0, Math.min(1, (x - a) / (b - a)));
  return t * t * (3 - 2 * t);
};

/**
 * One feather on a plane face: length along u, width along v. tipStart = the tip is at u = 0.
 * opts: { seed, marks (0..1 snowy-owl bars), under (paler underside), tint }
 */
function feather(img, tipStart, o = {}) {
  const L = img.width / K, H = img.height / K;
  each(img, () => [0, 0, 0, 0]);
  const asym = (o.asym ?? 0.12) * H;
  const inside = (u, v) => {
    const s = tipStart ? 1 - u / L : u / L; // 0 base … 1 tip
    let hw = (H / 2) * (0.3 + 0.7 * smooth(0, 0.3, s));
    if (s > 0.78) hw *= Math.sqrt(Math.max(0, 1 - ((s - 0.78) / 0.22) ** 2));
    return Math.abs(v - H / 2 - asym * s) <= hw;
  };
  const color = (u, v) => {
    const s = tipStart ? 1 - u / L : u / L;
    const across = (v - H / 2 - asym * s) / (H / 2); // −1..1
    let c = mix(o.under ? SHADOW : DEEP, WHITE, smooth(0, 0.45, s));
    // soft vane shading (darker towards one edge) + barb streaks
    const barb = vnoise(u * 3.2 + across * 2.2, across * 6, o.seed ?? 1) - 0.5;
    c = mix(c, SHADOW, Math.max(0, across) * 0.45 + barb * 0.12);
    // snowy owl bars near the tip
    if (o.marks) {
      for (const [p, w] of [[0.58, 0.05], [0.76, 0.045]]) {
        const d = Math.abs(s - p - across * 0.05);
        if (d < w && hash(Math.floor(u * 6), Math.floor(v * 6), o.seed ?? 1) < 0.85) c = mix(c, MARK, o.marks * (1 - d / w) * 0.9);
      }
    }
    // icy tip glaze (Vibe accent)
    if (s > 0.86) c = mix(c, o.tint ?? TIP, (s - 0.86) / 0.14 * 0.55);
    // rachis (shaft)
    if (Math.abs(across) < 0.09) c = mix(c, s < 0.3 ? "#9aa6b8" : "#e9eef5", 0.7);
    return c;
  };
  shape(img, inside, color);
  return img;
}

/** Scalloped covert panel: rows of small rounded feathers, bottom edge cut out. */
function coverts(img, o = {}) {
  const W = img.width / K, H = img.height / K;
  each(img, () => [0, 0, 0, 0]);
  const rowH = o.rowH ?? 1.5, fw = o.fw ?? 1.7;
  const rows = Math.ceil(H / rowH);
  for (let r = 0; r < rows; r++) {
    const vy = (r + 1) * rowH - 0.2;
    const off = (r % 2) * fw * 0.5;
    for (let x = -fw; x < W + fw; x += fw) {
      const cx = x + off + fw / 2;
      const top = r === 0 ? 0 : vy - rowH * 1.6;
      const inside = (u, v) => v >= top && v <= vy && ((u - cx) / (fw * 0.56)) ** 2 + (Math.max(0, v - (vy - fw * 0.45)) / (fw * 0.45)) ** 2 <= 1;
      const col = (u, v) => {
        const t = (v - top) / (vy - top);
        let c = mix(o.under ? SHADOW : WHITE, o.under ? DEEP : SHADOW, t * 0.8);
        if (o.marks && hash(Math.round(cx * 3), r, 9) < o.marks) c = mix(c, MARK, smooth(0.55, 0.9, t) * 0.75);
        return c;
      };
      shape(img, inside, col, 1, K, [cx - fw, top, cx + fw, vy + 0.2]);
      // lower edge shadow line
      stroke(img, [[cx - fw * 0.45, vy - 0.15], [cx, vy + 0.02], [cx + fw * 0.45, vy - 0.15]], 0.14, DEEP, 0.6);
    }
  }
  return img;
}

/** Fluffy leading edge bar. */
function edgeFluff(img, seed) {
  const W = img.width / K, H = img.height / K;
  each(img, (u, v) => mix(WHITE, SHADOW, v / H * 0.6 + (vnoise(u * 4, v * 4, seed) - 0.5) * 0.25));
  return rim(img, 0.1, 0.5);
}

export default function () {
  const b = cosmetic("polar_owl_wings", {
    name: { en: "Polar Pals · Snowy Owl Wings", de: "Polar Pals · Schneeeulen-Flügel" },
    description: {
      en: "Big feathered wings with layered coverts, secondaries and primaries. Folded on your back while you walk, one clean flap and a long glide while flying, tucked in tight when you sneak.",
      de: "Große Federflügel mit geschichteten Deck-, Arm- und Handschwingen. Auf dem Rücken gefaltet beim Gehen, ein sauberer Flügelschlag und ein langer Gleitflug beim Fliegen, eng angelegt beim Schleichen.",
    },
    tags: ["polar-pals", "wings", "owl", "feathers", "animated"],
    rarity: "legendary",
    cosmetic: { slot: "wings", offsetWhen: [{ slot: "back", offset: [0, 1, 9] }] },
    preview: {
      panels: (p) => [
        { ...p[0], label: "front · idle (folded)" },
        { ...p[1], label: "back · idle (folded)" },
        { ...p[1], label: "back · fly · flap", anims: [{ name: "fly", t: 0.32 }], time: 0.32 },
        { ...p[1], label: "back · fly · glide", anims: [{ name: "fly", t: 1.8 }], time: 1.8, cam: { yaw: 200, pitch: 20 } },
        { ...p[0], label: "front · fly · glide", anims: [{ name: "fly", t: 1.8 }], time: 1.8 },
        { ...p[1], label: "back · glide (elytra)", anims: [{ name: "glide", t: 1.0 }], time: 1.0, cam: { yaw: 200, pitch: 24 } },
        { ...p[1], label: "side · sneak", anims: [{ name: "sneak", t: 0.5 }], time: 0.5, cam: { yaw: 270, pitch: 10 } },
        { ...p[1], label: "side · walk", anims: [{ name: "walk", t: 0.25 }], time: 0.25, cam: { yaw: 110, pitch: 10 } },
        { ...p[1], label: "rest pose (spread)", anims: [], cam: { yaw: 180, pitch: 10 } },
      ],
      sheet: { cols: 4, panelW: 300, panelH: 330 },
      thumb: { anims: [{ name: "fly", t: 1.8 }], time: 1.8, cam: { yaw: 200, pitch: 16 } },
    },
  });
  b.texture("main", { hd: K });
  // small falling feather sprite
  const tex = b.textures.get("main");
  const featherSprite = tex.region(4, 4, (img) => {
    each(img, () => [0, 0, 0, 0]);
    stroke(img, [[0.6, 3.5], [1.6, 2.2], [3.3, 0.6]], 0.9, WHITE);
    stroke(img, [[0.5, 3.6], [3.2, 0.7]], 0.14, "#9aa6b8");
    return img;
  });
  b.sprite("feather", "main", featherSprite);

  for (const [side, s] of [["r", 1], ["l", -1]]) {
    const X = (x) => s * x; // build the right wing, mirror for the left
    const box = (bone, from, size, paint, opts) => bone.box(s > 0 ? from : [-from[0] - size[0], from[1], from[2]], size, paint, opts);
    const tipStart = (face) => (face === "north") === (s > 0); // north face: u = 0 is +X
    const featherPaint = (seed, marks, len) => ({
      north: (img) => feather(img, tipStart("north"), { seed, under: true, marks: marks * 0.3 }),
      south: (img) => feather(img, tipStart("south"), { seed: seed + 1, marks, tint: seed % 2 ? TIP : TIP2 }),
    });
    const coverPaint = (marks, rowH, fw) => ({
      south: (img) => coverts(img, { marks, rowH, fw }),
      north: (img) => coverts(img, { under: true, rowH, fw }),
      up: (img) => edgeFluff(img, 3),
      sides: (img) => edgeFluff(img, 4),
      down: (img) => each(img, () => [0, 0, 0, 0]),
    });

    // wing_<side> sweeps the whole wing plane around the shoulder (y) / tilts it (x); every bone below it only
    // rotates inside the wing plane (z), which keeps the fold readable
    b.bone(`wing_${side}`, { attach: "body", pivot: [X(2), 21, 2.4] });
    const arm = b.bone(`${side}_arm`, { parent: `wing_${side}`, pivot: [X(2), 21, 2.4] });
    box(arm, [2, 20, 2.2], [7, 2, 1], { all: (img) => edgeFluff(img, 11) }, { inflate: 0.15 });
    const scap = b.bone(`${side}_lesser`, { parent: arm.name, pivot: [X(2), 21, 3.1] });
    box(scap, [2, 16.5, 3.1], [8, 4, 1], coverPaint(0.25, 1.3, 1.5));

    const fore = b.bone(`${side}_fore`, { parent: arm.name, pivot: [X(9), 21, 2.4] });
    box(fore, [9, 20, 2.2], [8, 2, 1], { all: (img) => edgeFluff(img, 12) }, { inflate: 0.15 });
    const cov = b.bone(`${side}_greater`, { parent: fore.name, pivot: [X(9), 20.5, 2.9] });
    box(cov, [8.5, 15, 2.9], [9, 5, 1], coverPaint(0.35, 1.6, 1.8));
    // secondaries: hang down from the forearm, slightly fanned
    [[9.6, 11, -96], [11.3, 12, -93], [13.0, 12, -90], [14.7, 12, -87], [16.4, 13, -83]].forEach(([x, len, ang], i) => {
      const f = b.bone(`${side}_sec${i + 1}`, { parent: fore.name, pivot: [X(x), 20.5, 2.55 + i * 0.02], rotation: [0, 0, s * ang] });
      box(f, [x, 19, 2.55 + i * 0.02], [len, 3, 0], featherPaint(20 + i, 0.55, len));
    });

    const hand = b.bone(`${side}_hand`, { parent: fore.name, pivot: [X(17), 21, 2.4] });
    box(hand, [17, 20, 2.2], [4, 2, 1], { all: (img) => edgeFluff(img, 13) }, { inflate: 0.15 });
    const alula = b.bone(`${side}_alula`, { parent: hand.name, pivot: [X(17), 21, 2.8] });
    box(alula, [16.5, 17, 2.8], [5, 3, 1], coverPaint(0.5, 1.4, 1.6));
    // primaries: the long "fingers", fanned from the hand
    [[17.6, 13, -78], [18.6, 15, -62], [19.6, 16, -46], [20.4, 17, -31], [21, 16, -17]].forEach(([x, len, ang], i) => {
      const f = b.bone(`${side}_p${i + 1}`, { parent: hand.name, pivot: [X(x), 21, 2.4 - i * 0.03], rotation: [0, 0, s * ang] });
      box(f, [x, 19.5, 2.4 - i * 0.03], [len, 3, 0], featherPaint(40 + i, 0.8, len));
    });
    b.locator(`tip_${side}`, `${side}_p4`, [X(36), 20, 2.4]);
  }

  // ------------------------------------------------------------------------------------------------ poses
  // offsets for the right wing (base = sweep [x, y], the rest are in-plane z angles); the left wing mirrors y / z
  const pose = (base, arm, fore, hand, sec, p) => ({ base, arm, fore, hand, sec, p });
  const FOLD = pose([4, -38, 0], 44, -152, 80, 100, -16);
  const TUCK = pose([2, -24, 0], 38, -160, 86, 106, -12);
  const GLIDE = pose([0, -10, 0], 6, -6, 4, 6, 0);
  const UP = pose([0, -22, 0], 56, -36, -30, 20, 26);
  const DOWN = pose([0, -4, 0], -34, 14, 18, -4, -10);
  const M = (v, s) => [v[0], v[1] * s, v[2] * s];
  const zr = (a, s) => [0, 0, a * s];

  /** Write [t, POSE, primary spread] keyframes for every wing bone. */
  const keys = (anim, list) => {
    for (const [side, s] of [["r", 1], ["l", -1]]) {
      anim.rot(`wing_${side}`, list.map(([t, p]) => [t, M(p.base, s)]));
      anim.rot(`${side}_arm`, list.map(([t, p]) => [t, zr(p.arm, s)]));
      anim.rot(`${side}_fore`, list.map(([t, p]) => [t, zr(p.fore, s)]));
      anim.rot(`${side}_hand`, list.map(([t, p]) => [t, zr(p.hand, s)]));
      for (let i = 0; i < 5; i++) {
        anim.rot(`${side}_sec${i + 1}`, list.map(([t, p]) => [t, zr(p.sec, s)]));
        anim.rot(`${side}_p${i + 1}`, list.map(([t, p, sp]) => [t, zr(p.p + (sp ?? 0) * (i - 2), s)]));
      }
    }
  };
  const lerpPose = (a, c, f) => {
    const l = (x, y) => (Array.isArray(x) ? x.map((v, i) => v + (y[i] - v) * f) : x + (y - x) * f);
    return Object.fromEntries(Object.keys(a).map((k) => [k, l(a[k], c[k])]));
  };

  // idle: folded, a slow breath (wings lift a touch and the primaries relax)
  const idle = b.anim("idle", 4, true);
  const breathe = lerpPose(FOLD, GLIDE, 0.06);
  keys(idle, [[0, FOLD, -13], [2, breathe, -11], [4, FOLD, -13]]);

  // walk: folded, bobbing with the steps
  const walk = b.anim("walk", 0.6, true);
  const bob = lerpPose(FOLD, UP, 0.07);
  keys(walk, [[0, FOLD, -13], [0.15, bob, -12], [0.3, FOLD, -13], [0.45, bob, -12], [0.6, FOLD, -13]]);

  // fly: one big clean flap (0 → 0.9 s), then a long glide with spread fingers and a soaring wobble
  const fly = b.anim("fly", 3.2, true);
  const soar1 = { ...GLIDE, arm: 10, base: [0, -13, 0] }, soar2 = { ...GLIDE, arm: 2, base: [0, -8, 0] };
  keys(fly, [[0, GLIDE, 3], [0.3, UP, 0], [0.62, DOWN, 5], [0.95, GLIDE, 4], [1.8, soar1, 5], [2.6, soar2, 4], [3.2, GLIDE, 3]]);
  for (const side of ["r", "l"]) {
    fly.particle(0.62, { sprite: "feather", locator: `tip_${side}`, count: 1, lifetime: 2.2, size: 0.18, spread: [0.2, 0.1, 0.2], velocity: [0, -0.02, 0.02], gravity: 0.002 });
  }

  // glide (elytra / long fall): wide open and soaring, a slow tilt and the finger feathers flexing
  const glide = b.anim("glide", 3, true);
  const g1 = { ...GLIDE, base: [-6, -6, 0], arm: 4 }, g2 = { ...GLIDE, base: [-4, -12, 0], arm: 10, hand: 0 };
  keys(glide, [[0, g1, 6], [1.5, g2, 3], [3, g1, 6]]);

  // sneak: tucked in tight
  const sneak = b.anim("sneak", 2, true);
  keys(sneak, [[0, TUCK, -14], [1, lerpPose(TUCK, FOLD, 0.15), -14], [2, TUCK, -14]]);
  return b;
}
