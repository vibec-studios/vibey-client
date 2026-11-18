// Polar Pals · Igloo Backpack – a mini igloo of snow bricks on knitted straps. A seal pup lives inside: every few
// seconds it peeks out of the door, looks around, blinks, waves a flipper and ducks back in.
import { K, P, cosmetic, fur, rim, edges, each, ellipse, blush, eye, closedEye, stroke, arc, mix, rgba, hash, vnoise } from "./_polar.mjs";

const L = 8;
const Z0 = [0, 0, 0];

/** Snow bricks: staggered rows, blue-grey joints, soft blue shading towards the bottom. */
function bricks(img, o = {}) {
  const W = img.width / K, H = img.height / K;
  const bh = o.bh ?? 1.5, bw = o.bw ?? 3;
  each(img, (u, v) => {
    const row = Math.floor((v + (o.v0 ?? 0)) / bh);
    const off = row % 2 ? bw / 2 : 0;
    const fx = (u + off + (o.u0 ?? 0)) / bw, fy = (v + (o.v0 ?? 0)) / bh;
    const jx = Math.min(fx % 1, 1 - (fx % 1)) * bw, jy = Math.min(fy % 1, 1 - (fy % 1)) * bh;
    const n = vnoise(u * 2.5, v * 2.5, o.seed ?? 1);
    let c = mix(P.snow, P.snowShade, v / Math.max(H, 1) * 0.7 + (n - 0.5) * 0.3);
    if (hash(Math.floor(fx), row, o.seed ?? 1) > 0.82) c = mix(c, P.ice, 0.6);
    if (jx < 0.14 || jy < 0.14) c = mix(c, P.snowDeep, 0.8);
    else if (jy < 0.32 && (fy % 1) < 0.5) c = mix(c, "#ffffff", 0.5); // highlight under the top joint
    return c;
  });
  return img;
}

export default function () {
  const b = cosmetic("polar_igloo_backpack", {
    name: { en: "Polar Pals · Igloo Backpack", de: "Polar Pals · Iglu-Rucksack" },
    description: {
      en: "A mini igloo on knitted straps. A seal pup lives inside – now and then it peeks out of the door, blinks, waves and ducks back in.",
      de: "Ein Mini-Iglu an Strickgurten. Darin wohnt ein Robbenbaby – ab und zu schaut es aus der Tür, blinzelt, winkt und verschwindet wieder.",
    },
    tags: ["polar-pals", "back", "igloo", "seal", "animated"],
    rarity: "epic",
    cosmetic: { slot: "back", hideWithArmor: ["chest"] },
    preview: {
      panels: (p) => [
        { ...p[0], label: "back · pup peeks", anims: [{ name: "idle", t: 3.4 }], time: 3.4 },
        { ...p[0], label: "back · wave", anims: [{ name: "idle", t: 4.9 }], time: 4.9, cam: { yaw: 190, pitch: 16 } },
        { ...p[1], label: "front (straps)" },
        { ...p[2], label: "side", anims: [{ name: "idle", t: 3.4 }], time: 3.4 },
        { ...p[0], label: "back · empty", anims: [{ name: "idle", t: 0.5 }], time: 0.5, cam: { yaw: 150, pitch: 12 } },
      ],
      sheet: { cols: 5, panelW: 260, panelH: 320 },
      thumb: { anims: [{ name: "idle", t: 3.4 }], time: 3.4, cam: { yaw: 205, pitch: 14 } },
    },
  });
  b.texture("main", { hd: K });

  const pack = b.bone("igloo", { attach: "body", pivot: [0, 18, 2] });
  const br = (seed, extra = {}) => ({ all: (img) => rim(bricks(img, { seed, ...extra }), 0.08, 0.6) });
  // dome in layers (shrinking in x and z), resting on the back
  pack.box([-5, 12, 2.2], [10, 5, 7], br(1));
  pack.box([-4.5, 17, 2.4], [9, 2, 6], br(2, { v0: 0.5 }));
  pack.box([-3.5, 19, 2.8], [7, 2, 5], br(3));
  pack.box([-2.5, 21, 3.2], [5, 1, 4], br(4));
  pack.box([-1.5, 22, 3.7], [3, 1, 3], { all: (img) => rim(fur(img, { top: "#ffffff", bottom: P.snow, seed: 5, strands: 0.02 }), 0.1, 0.5) });
  // entrance tunnel facing away from the player (+Z) with a dark doorway
  pack.box([-2.5, 12, 9.2], [5, 4, 2], {
    ...br(6),
    south: (img) => {
      bricks(img, { seed: 7 });
      // arched doorway
      each(img, (u, v) => {
        const inside = Math.abs(u - 2.5) <= 1.55 && (v >= 1.6 || ((u - 2.5) / 1.55) ** 2 + ((v - 1.6) / 1.1) ** 2 <= 1);
        return inside ? mix("#1d2a44", "#0e1424", v / 4) : null;
      });
      return img;
    },
  });
  // a little teal pennant on top
  const flag = b.bone("flag", { parent: "igloo", pivot: [1, 23, 5] });
  flag.box([0.5, 23, 4.7], [1, 5, 1], { all: (img) => each(img, (u, v) => mix("#c9b28a", "#8f7a58", v / 5)) }, { inflate: -0.25 });
  const pennant = (img) => {
    each(img, () => [0, 0, 0, 0]);
    each(img, (u, v) => (v >= u * 0.5 && v <= 3 - u * 0.5 ? mix(P.teal, P.tealDark, u / 4) : null));
    ellipse(img, 1, 1.5, 0.45, 0.45, P.snow);
    return img;
  };
  const cloth = b.bone("pennant", { parent: "flag", pivot: [1, 27.5, 5.2] });
  cloth.box([1, 25, 5.2], [4, 3, 0], { north: pennant, south: pennant });

  // straps over the shoulders + chest strap
  const strapPaint = { all: (img) => rim(each(img, (u, v) => mix(P.brand, P.brandDark, (Math.floor(v * 2) % 2) * 0.25)), 0.15, 0.3) };
  const straps = b.bone("straps", { attach: "body", pivot: [0, 24, 0] });
  for (const s of [1, -1]) {
    const x = s > 0 ? 1.2 : -3.2;
    straps.box([x, 24, -2.3], [2, 0, 5], strapPaint, { inflate: 0.2 });
    straps.box([x, 14, -2.3], [2, 10, 0], strapPaint, { inflate: 0.2 });
    straps.box([x, 18, 2.2], [2, 6, 0], strapPaint, { inflate: 0.2 });
  }
  straps.box([-3, 18, -2.5], [6, 1, 0], { all: (img) => each(img, () => P.teal) }, { inflate: 0.2 });
  straps.box([-0.5, 17.5, -2.6], [1, 2, 0], { all: (img) => each(img, (u, v) => mix(P.snow, P.snowDeep, v / 2)) }, { inflate: 0.25 });

  // the seal pup inside (hidden in the dome at rest; idle slides it out through the door)
  const pup = b.bone("pup", { parent: "igloo", pivot: [0, 13, 6] });
  const pupFace = (img) => {
    fur(img, { top: P.sealTop, bottom: P.sealLow, seed: 40, strands: 0.04 });
    blush(img, 0.75, 2.75, 0.55, 0.4, P.blush, 0.6);
    blush(img, 3.25, 2.75, 0.55, 0.4, P.blush, 0.6);
    eye(img, 1.25, 1.6, 0.6, 0.72);
    eye(img, 2.75, 1.6, 0.6, 0.72, { mirror: true });
    // muzzle + nose
    ellipse(img, 2, 3.05, 0.95, 0.6, P.muzzle);
    ellipse(img, 2, 2.75, 0.35, 0.22, P.nose);
    stroke(img, arc(1.75, 3.1, 0.25, 0.2, 0, 180, 5), 0.1, P.mouth);
    stroke(img, arc(2.25, 3.1, 0.25, 0.2, 0, 180, 5), 0.1, P.mouth);
    return rim(img, 0.12, 0.5);
  };
  pup.box([-2, 12.5, 5], [4, 4, 4], { all: (img) => rim(fur(img, { top: P.sealTop, bottom: P.sealLow, seed: 41 }), 0.12, 0.6), south: pupFace });
  const pupLid = (img) => {
    each(img, () => [0, 0, 0, 0]);
    for (const cx of [1.25, 2.75]) {
      ellipse(img, cx, 1.6, 0.7, 0.8, mix(P.sealTop, P.sealLow, 0.4));
      closedEye(img, cx, 1.75, 1.0, "sleep", P.eye, 0.18);
    }
    return img;
  };
  const lids = b.bone("pup_lids", { parent: "pup", pivot: [0, 14, 8.9] });
  lids.box([-2, 12.5, 8.9], [4, 4, 0], { south: pupLid });
  const fin = b.bone("pup_fin", { parent: "pup", pivot: [1.8, 13.5, 8] });
  fin.box([1.8, 13, 7], [2, 1, 2], { all: (img) => rim(fur(img, { top: P.flipper, bottom: P.flipperDark, seed: 42 }), 0.15, 0.4) });
  b.locator("door", "igloo", [0, 13, 11.5]);

  // ------------------------------------------------------------------------------------------------ idle (8 s)
  const idle = b.anim("idle", L, true);
  const OUT = [0, 0, 2.9];
  idle.pos("pup", [[0, Z0], [2.0, Z0], [2.3, [0, 0.5, 3.4]], [2.45, OUT], [5.6, OUT], [5.75, [0, 0.3, 3.3]], [6.05, Z0], [L, Z0]]);
  idle.rot("pup", [[0, Z0], [2.4, Z0], [2.8, [0, 22, 8]], [3.3, [0, 22, 8]], [3.7, [0, -22, -8]], [4.2, [0, -22, -8]], [4.5, [6, 0, 0]], [5.4, [6, 0, 0]], [5.6, Z0], [L, Z0]]);
  idle.pos("pup_lids", [[0, Z0, "step"], [3.5, [0, 0, 0.25], "step"], [3.63, Z0, "step"], [5.1, [0, 0, 0.25], "step"], [5.22, Z0, "step"], [L, Z0, "step"]], "step");
  idle.rot("pup_fin", [[0, Z0], [4.5, Z0], [4.6, [0, 0, 70]], [4.75, [0, 0, 40]], [4.9, [0, 0, 75]], [5.05, [0, 0, 40]], [5.2, [0, 0, 70]], [5.4, Z0], [L, Z0]]);
  idle.rot("pennant", [[0, [0, 0, 0]], [1, [0, 14, 0]], [2, [0, -6, 0]], [3, [0, 12, 0]], [4, [0, -4, 0]], [5, [0, 16, 0]], [6, [0, -6, 0]], [7, [0, 10, 0]], [L, Z0]]);
  idle.particle(2.3, { particle: "minecraft:snowflake", locator: "door", count: 3, spread: [0.12, 0.05, 0.05], velocity: [0, 0.03, 0.02] });

  // walk: the pack bounces, the pennant flaps
  const walk = b.anim("walk", 0.6, true);
  walk.pos("igloo", [[0, Z0], [0.15, [0, 0.6, 0]], [0.3, Z0], [0.45, [0, 0.6, 0]], [0.6, Z0]]);
  walk.rot("igloo", [[0, [0, 0, -1.5]], [0.3, [0, 0, 1.5]], [0.6, [0, 0, -1.5]]]);
  walk.rot("pennant", [[0, [0, 20, 0]], [0.15, [0, 30, 0]], [0.3, [0, 18, 0]], [0.45, [0, 32, 0]], [0.6, [0, 20, 0]]]);
  return b;
}
