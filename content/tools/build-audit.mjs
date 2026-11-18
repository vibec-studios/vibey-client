// Audit bundles: unusual feature combinations that must work purely from data (no code per item), plus bundles
// written for a newer client. Not part of the starter set (not in dist/, not in the jar); the client game test
// AuditBundlesGameTest loads them from content/audit/. Run: node tools/build-audit.mjs
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { Img } from "./canvas.mjs";
import { stringify } from "./vibey.mjs";

const OUT = new URL("../audit/", import.meta.url);
const pop = readFileSync(new URL("../sounds/pop.ogg", import.meta.url)).toString("base64");
const chime = readFileSync(new URL("../sounds/chime.ogg", import.meta.url)).toString("base64");

const png = (img) => img.toPNG().toString("base64");

/** Solid texture with a checker and a border so faces are easy to tell apart. */
function tile(w, h, a, b, edge) {
  const img = new Img(w, h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) img.set(x, y, ((x >> 1) + (y >> 1)) % 2 ? a : b);
  img.border(edge);
  return img;
}

/** Vertical strip of `frames` frames (w×h each), a bar moving down. */
function strip(w, h, frames) {
  const img = new Img(w, h * frames);
  for (let f = 0; f < frames; f++) {
    const v = img.view(0, f * h, w, h);
    v.fill("#1d1452");
    v.rect(0, Math.floor((f * h) / frames), w, Math.max(1, Math.floor(h / frames)), "#3fe0c5");
    v.border("#8b6cff");
  }
  return img;
}

const base = (kind, id, name, extra = {}) => ({
  format: "vibey", formatVersion: 1, kind, id: `vibeytest:${id}`,
  name: { en: name, de: name }, author: "Vibey audit", tags: ["audit"], rarity: "rare", ...extra,
});

const loopRot = (keys) => keys.map(([t, v]) => ({ t, v, i: "catmullrom" }));

const bundles = {};

// A. hat: HD texture, a catmull-rom looping orbit with a locator, vanilla + sprite particles, embedded + vanilla sound
// on locators, unknown event type / interpolation / fields everywhere.
bundles.sparkle_hat = {
  ...base("cosmetic", "sparkle_hat", "Audit Sparkle Hat"),
  future: { hologram: true, version: 7 },
  cosmetic: { slot: "hat", hideWithArmor: ["head", "tail"], hatPhysics: { wobble: 2 } },
  textures: [
    // HD: 64×64 PNG for a 16×16 UV space (4×)
    { id: "main", width: 16, height: 16, png: png(tile(64, 64, "#8b6cff", "#b6a3ff", "#33208c")), mipmaps: "auto" },
  ],
  sprites: [{ id: "spark", texture: "main", uv: [0, 0, 4, 4] }],
  sounds: [{ id: "pop", ogg: pop }],
  model: {
    texture: "main",
    bones: [
      { name: "brim", parent: null, attach: "head", pivot: [0, 32, 0], rotation: [0, 0, 0], physics: { spring: 0.4 },
        cubes: [{ from: [-5, 32, -5], to: [5, 33, 5], uv: { box: [0, 0] }, bevel: 1 }] },
      { name: "orbit", parent: "brim", pivot: [0, 33, 0], rotation: [0, 0, 0],
        cubes: [{ from: [5, 34, -1], to: [7, 36, 1], uv: { box: [0, 0] } }], emissive: true },
    ],
    locators: { orbit_tip: { bone: "orbit", position: [6, 36, 0] }, crown: { bone: "brim", position: [0, 34, 0] } },
  },
  animations: {
    idle: {
      length: 2, loop: true,
      bones: {
        orbit: { rotation: loopRot([[0, [0, 0, 0]], [0.5, [0, 90, 0]], [1, [0, 180, 0]], [1.5, [0, 270, 0]], [2, [0, 360, 0]]]) },
        brim: { position: [{ t: 0, v: [0, 0, 0] }, { t: 1, v: [0, 0.5, 0], i: "elastic" }, { t: 2, v: [0, 0, 0] }] },
      },
      events: [
        { t: 0, type: "sound", sound: "minecraft:block.amethyst_block.chime", volume: 0.3, pitch: 1.4 },
        { t: 0.25, type: "particle", particle: "minecraft:end_rod", locator: "orbit_tip", count: 2, spread: [0.02, 0.02, 0.02], velocity: [0, 0.01, 0] },
        { t: 0.75, type: "particle", sprite: "spark", locator: "orbit_tip", count: 3, lifetime: 0.6, size: 0.12, spread: [0.05, 0.05, 0.05], velocity: [0, 0.02, 0], gravity: -0.002, emissive: true },
        { t: 1.25, type: "particle", particle: "heart", locator: "crown", count: 1 },
        { t: 1.5, type: "sound", sound: "pop", locator: "orbit_tip", volume: 0.4, pitch: 1.2 },
        { t: 1.75, type: "light", color: "#ffffff", radius: 3 },
        { t: 1.9, type: "particle", particle: "minecraft:dust", locator: "crown" },
      ],
    },
    // an animation name the client does not know: ignored
    celebrate: { length: 1, loop: false, bones: { brim: { scale: [{ t: 0, v: [1, 1, 1] }, { t: 1, v: [2, 2, 2] }] } } },
  },
};

// B. pet: props as child bones, visibility toggles in idle AND walk, walk events (fire only while walking).
bundles.prop_pet = {
  ...base("cosmetic", "prop_pet", "Audit Prop Pet"),
  cosmetic: { slot: "pet", pet: { follow: "float", scale: 0.7, bobbing: "sine" } },
  textures: [{ id: "main", width: 32, height: 32, png: png(tile(32, 32, "#8f9aa6", "#cdd4db", "#2a2a30")) }],
  sounds: [{ id: "squeak", ogg: chime }],
  model: {
    texture: "main",
    bones: [
      { name: "body", parent: null, attach: "world", pivot: [0, 4, 0], rotation: [0, 0, 0],
        cubes: [{ from: [-3, 0, -3], to: [3, 6, 3], uv: { box: [0, 0] } }] },
      { name: "hat_prop", parent: "body", pivot: [0, 6, 0], rotation: [0, 0, 10],
        cubes: [{ from: [-2, 6, -2], to: [2, 8, 2], uv: { box: [0, 12] } }] },
      { name: "balloon", parent: "body", pivot: [3, 6, 0], rotation: [0, 0, 0], translucent: true,
        cubes: [{ from: [3, 10, -1.5], to: [6, 13, 1.5], uv: { box: [16, 12] } }, { from: [4.25, 6, -0.25], to: [4.75, 10, 0.25], uv: { box: [24, 0] } }] },
      { name: "sign", parent: "body", pivot: [0, 6, 0], rotation: [0, 0, 0],
        cubes: [{ from: [-3, 8, 3], to: [3, 11, 3.5], uv: { box: [0, 24] } }] },
    ],
    locators: { top: { bone: "hat_prop", position: [0, 8, 0] }, balloon: { bone: "balloon", position: [4.5, 13, 0] } },
  },
  animations: {
    idle: {
      length: 2, loop: true,
      bones: { balloon: { rotation: loopRot([[0, [0, 0, -8]], [1, [0, 0, 8]], [2, [0, 0, -8]]]) } },
      events: [
        { t: 0, type: "visibility", bone: "sign", visible: false },
        { t: 0, type: "visibility", bone: "balloon", visible: true },
        { t: 1.2, type: "visibility", bone: "balloon", visible: false },
        { t: 1.8, type: "visibility", bone: "balloon", visible: true },
        { t: 0.5, type: "particle", particle: "minecraft:note", locator: "balloon", count: 1 },
      ],
    },
    walk: {
      length: 1, loop: true,
      bones: { body: { position: [{ t: 0, v: [0, 0, 0] }, { t: 0.5, v: [0, 1, 0] }, { t: 1, v: [0, 0, 0] }] } },
      events: [
        { t: 0, type: "visibility", bone: "sign", visible: true },
        { t: 0, type: "visibility", bone: "balloon", visible: false },
        { t: 0.5, type: "particle", particle: "minecraft:cloud", locator: "top", count: 2, spread: [0.1, 0, 0.1] },
        { t: 0.9, type: "sound", sound: "squeak", locator: "top", volume: 0.2, pitch: 1.6 },
      ],
    },
  },
};

// C. emote: three props on different player bones, each with a locator and its own particles, embedded sound on a
// prop locator, visibility toggles, root motion, catmull-rom loop with 3 keys (wraps), hold.
bundles.triple_prop_emote = {
  ...base("emote", "triple_prop_emote", "Audit Triple Props"),
  emote: { animation: "main", loop: true, thirdPerson: true, stopOnMove: false, hideHeldItems: true, cameraShake: 0.2 },
  textures: [
    { id: "main", width: 32, height: 32, png: png(tile(32, 32, "#3fe0c5", "#9af3e3", "#126b5e")) },
    { id: "warm", width: 16, height: 16, png: png(tile(16, 16, "#ffb547", "#ff5470", "#7a2a10")) },
  ],
  sprites: [{ id: "dot", texture: "warm", uv: [2, 2, 6, 6] }],
  sounds: [{ id: "pop", ogg: pop }],
  model: {
    texture: "main",
    bones: [
      { name: "wand", parent: null, attach: "right_arm", pivot: [6, 12, 0], rotation: [0, 0, 0],
        cubes: [{ from: [5.5, 4, -0.5], to: [6.5, 13, 0.5], uv: { box: [0, 0] } }] },
      { name: "lantern", parent: null, attach: "left_arm", pivot: [-6, 12, 0], rotation: [0, 0, 0],
        cubes: [{ from: [-8, 7, -2], to: [-4, 11, 2], uv: { box: [0, 16] }, texture: "warm" }], emissive: true },
      { name: "halo", parent: null, attach: "head", pivot: [0, 33, 0], rotation: [0, 0, 0],
        cubes: [{ from: [-4, 34, -4], to: [4, 35, 4], uv: { box: [0, 0] }, inflate: 0.2 }], emissive: true },
    ],
    locators: {
      wand_tip: { bone: "wand", position: [6, 4, 0] },
      lantern: { bone: "lantern", position: [-6, 9, 0] },
      halo: { bone: "halo", position: [0, 35, 0] },
      hand: { bone: "right_arm", position: [6, 12, 0] },
    },
  },
  animations: {
    main: {
      length: 2, loop: true,
      bones: {
        right_arm: { rotation: loopRot([[0, [-110, 0, 10]], [0.7, [-150, 0, 25]], [1.4, [-120, 0, -5]]]) },
        left_arm: { rotation: [{ t: 0, v: [-40, 0, -10] }] },
        root: { position: loopRot([[0, [0, 0, 0]], [1, [0, 1, 0]], [2, [0, 0, 0]]]) },
        halo: { rotation: [{ t: 0, v: [0, 0, 0] }, { t: 2, v: [0, 360, 0] }] },
        wand: { scale: [{ t: 0, v: [1, 1, 1], i: "step" }, { t: 1, v: [1, 1.3, 1] }] },
      },
      events: [
        { t: 0, type: "visibility", bone: "lantern", visible: true },
        { t: 0.1, type: "particle", sprite: "dot", locator: "wand_tip", count: 4, lifetime: 0.5, size: 0.1, spread: [0.05, 0.05, 0.05], velocity: [0, 0.03, 0], emissive: true },
        { t: 0.4, type: "particle", particle: "minecraft:flame", locator: "lantern", count: 2, spread: [0.05, 0.05, 0.05] },
        { t: 0.7, type: "particle", particle: "minecraft:end_rod", locator: "halo", count: 3, spread: [0.2, 0, 0.2] },
        { t: 0.9, type: "sound", sound: "pop", locator: "wand_tip", volume: 0.5 },
        { t: 1.1, type: "visibility", bone: "lantern", visible: false },
        { t: 1.3, type: "particle", particle: "minecraft:heart", locator: "hand", count: 1 },
        { t: 1.6, type: "visibility", bone: "lantern", visible: true },
        { t: 1.8, type: "sound", sound: "minecraft:entity.experience_orb.pickup", locator: "halo", volume: 0.3 },
      ],
    },
  },
};

// D. back item: per-face UV with three textures (animated interpolated strip, HD, plain), omitted face, cube-level
// texture on a box-UV cube, emissive child.
bundles.faces_back = {
  ...base("cosmetic", "faces_back", "Audit Faces Back"),
  cosmetic: { slot: "back" },
  textures: [
    { id: "main", width: 16, height: 16, png: png(tile(16, 16, "#18181f", "#6b6b75", "#f4f4f5")) },
    { id: "strip", width: 16, height: 16, png: png(strip(16, 16, 4)), animation: { frameTime: 5, interpolate: true, easing: "smooth" } },
    { id: "hd", width: 16, height: 16, png: png(tile(128, 128, "#ff5470", "#ffb547", "#000000")) },
  ],
  model: {
    texture: "main",
    bones: [
      { name: "panel", parent: null, attach: "body", pivot: [0, 24, 2], rotation: [0, 0, 0],
        cubes: [
          { from: [-4, 14, 2], to: [4, 22, 4], uv: {
            north: [0, 0, 8, 8],
            south: { uv: [0, 0, 16, 16], texture: "strip" },
            east: [0, 0, 2, 8], west: [0, 0, 2, 8],
            down: { uv: [0, 0, 8, 2], texture: "hd" },
          } },
        ] },
      { name: "badge", parent: "panel", pivot: [0, 18, 4], rotation: [0, 0, 45], emissive: true,
        cubes: [{ from: [-1.5, 16.5, 4], to: [1.5, 19.5, 5], uv: { box: [0, 0] }, texture: "hd" }] },
    ],
  },
  animations: {
    idle: { length: 4, loop: true, bones: { badge: { rotation: [{ t: 0, v: [0, 0, 45] }, { t: 4, v: [0, 0, 405] }] } } },
  },
};

// E. wings with a future attach point (degrades to body), unknown interpolation and an unknown section.
bundles.future_wings = {
  ...base("cosmetic", "future_wings", "Audit Future Wings"),
  cosmetic: { slot: "wings", wings: { flapSpeed: 2 } },
  textures: [{ id: "main", width: 32, height: 32, png: png(tile(32, 32, "#b6a3ff", "#dcd3ff", "#4a2fc0")) }],
  model: {
    texture: "main",
    bones: [
      { name: "left", parent: null, attach: "tail", pivot: [-1, 22, 2], rotation: [0, 20, 0],
        cubes: [{ from: [-11, 12, 2], to: [-1, 24, 3], uv: { box: [0, 0] } }] },
      { name: "right", parent: null, attach: "body", pivot: [1, 22, 2], rotation: [0, -20, 0],
        cubes: [{ from: [1, 12, 2], to: [11, 24, 3], uv: { box: [0, 0] }, mirror: true }] },
    ],
  },
  animations: {
    idle: {
      length: 1.5, loop: true,
      bones: {
        left: { rotation: [{ t: 0, v: [0, 20, 0], i: "bounce" }, { t: 0.75, v: [0, 35, 0], i: "catmullrom" }, { t: 1.5, v: [0, 20, 0] }] },
        right: { rotation: [{ t: 0, v: [0, -20, 0], i: "bezier", bezier: { left: [0, 0], right: [1, 1] } }, { t: 0.75, v: [0, -35, 0] }, { t: 1.5, v: [0, -20, 0] }] },
      },
    },
  },
};

// F–H. written for a newer client / unknown slot
bundles.needs_update = { ...base("cosmetic", "needs_update", "Audit Needs Update"), minClientVersion: "9.0.0",
  cosmetic: { slot: "hat" }, textures: bundles.faces_back.textures.slice(0, 1),
  model: { texture: "main", bones: [{ name: "b", parent: null, attach: "head", pivot: [0, 32, 0], cubes: [{ from: [-1, 32, -1], to: [1, 34, 1], uv: { box: [0, 0] } }] }] } };
bundles.future_format = { ...bundles.needs_update, id: "vibeytest:future_format", name: { en: "Audit Future Format" }, formatVersion: 2 };
delete bundles.future_format.minClientVersion;
bundles.unknown_slot = { ...bundles.needs_update, id: "vibeytest:unknown_slot", name: { en: "Audit Unknown Slot" }, cosmetic: { slot: "tail" } };
delete bundles.unknown_slot.minClientVersion;

mkdirSync(OUT, { recursive: true });
const index = [];
for (const [file, b] of Object.entries(bundles)) {
  writeFileSync(new URL(`${file}.vibey`, OUT), stringify(b) + "\n");
  index.push(file);
}
writeFileSync(new URL("index.json", OUT), JSON.stringify({ files: index.map((f) => `${f}.vibey`) }, null, 2) + "\n");
console.log(`wrote ${index.length} audit bundles to content/audit/`);
