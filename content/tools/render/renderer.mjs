// Software preview renderer for `.vibey` bundles: evaluates bones/animations exactly by the spec conventions,
// builds textured quads and rasterizes them with a z-buffer (opaque/cutout, translucent, emissive + bloom).
//
// Convention (docs/SPEC.md §5 "Coordinate convention"): `.vibey` coordinates are Blockbench space (player faces
// north/-Z, right arm at +X). Blockbench → vanilla model space: x' = −x, y' = 24 − y, z' = z (Blockbench's Modded
// Entity exporter); rotations (rest + keyframes) → xRot = −rx, yRot = −ry, zRot = rz (ZYX order); position
// keyframes (offsets) → (−x, −y, z). Everything below this loader works in vanilla model space, exactly like the
// game. Model space → world (player at yaw 0 facing +Z, right arm at world −X): (x, 24 − y, −z).
import { Img, rgba, rng } from "../canvas.mjs";
import { decodePNG } from "../png.mjs";
import { I, T, S, Rx, Ry, Rzyx, mul, apply, applyDir, dot, cross, norm, sub, deg } from "./math.mjs";
import { PLAYER_PARTS, playerSkin } from "./player.mjs";

const WORLD = [1, 0, 0, 0, 0, -1, 0, 24, 0, 0, -1, 0, 0, 0, 0, 1];
const PLAYER_NAMES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg"];
export const mcPos = (p) => [-p[0], 24 - p[1], p[2]];
export const mcOff = (p) => [-p[0], -p[1], p[2]];
export const mcRot = (r) => [deg(-r[0]), deg(-r[1]), deg(r[2])];

// ------------------------------------------------------------------------------------------------ loading
export function loadBundle(json) {
  const tex = new Map();
  for (const t of json.textures ?? []) {
    const d = decodePNG(Buffer.from(t.png, "base64"));
    const img = Img.fromRGBA(d.width, d.height, d.data);
    const frames = t.animation ? Math.floor(d.height / t.height) : 1;
    tex.set(t.id, {
      id: t.id, img, width: t.width, height: t.height, frames,
      frameTime: t.animation?.frameTime ?? 1,
      order: t.animation?.frames ?? [...Array(frames).keys()],
    });
  }
  const bones = new Map((json.model?.bones ?? []).map((b) => [b.name, b]));
  const sprites = new Map((json.sprites ?? []).map((s) => [s.id, s]));
  return { json, tex, bones, sprites, locators: json.model?.locators ?? {} };
}

let SKIN = null;
function playerTex() {
  SKIN ??= { id: "__player", img: playerSkin(), width: 64, height: 64, frames: 1, frameTime: 1, order: [0] };
  return SKIN;
}

// ------------------------------------------------------------------------------------------------ animation
function catmull(p0, p1, p2, p3, t) {
  const t2 = t * t, t3 = t2 * t;
  return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
}

/**
 * Sample a channel (keyframes sorted by t) at time t – Blockbench's rules (SPEC §5 "Format details"), ported from
 * Blockbench 5.2's BoneAnimator.interpolate / Keyframe.getCatmullromLerp:
 * - segment A→B runs from A.v to B.pre ?? B.v; before the first key its `pre ?? v` is held, after the last key its v;
 * - A.i == "step" holds A; A linear and B linear/step → linear; A or B catmullrom → Catmull-Rom; otherwise
 *   (bezier without handles, not used by the starter set) linear;
 * - Catmull-Rom uses the neighbouring keys as control points (duplicating the end point when there is none, or when
 *   A/B is a split key with `pre`). In looping animations with ≥ 3 keys Blockbench wraps the missing neighbours
 *   (keys[n−2] before the first segment, keys[1] after the last one); the value itself never interpolates across
 *   the loop end.
 */
export function sampleChannel(keys, t, loop = false) {
  if (!keys?.length) return null;
  const n = keys.length;
  const pre = (k) => k.pre ?? k.v;
  if (t < keys[0].t) return pre(keys[0]);
  if (t >= keys[n - 1].t) return keys[n - 1].v;
  let k = 0;
  while (k < n - 1 && keys[k + 1].t <= t) k++;
  const a = keys[k], b = keys[k + 1];
  const f = (t - a.t) / (b.t - a.t || 1);
  const ia = a.i ?? "linear", ib = b.i ?? "linear";
  const end = pre(b);
  if (ia === "step") return a.v;
  if (ia === "catmullrom" || ib === "catmullrom") {
    const wrap = loop && n >= 3;
    const before = keys[k - 1] ?? (wrap ? keys[n - 2] : null);
    const after = keys[k + 2] ?? (wrap ? keys[1] : null);
    const p0 = before && !a.pre ? before.v : a.v;
    const p3 = after && !b.pre ? pre(after) : end;
    return [0, 1, 2].map((c) => catmull(p0[c], a.v[c], end[c], p3[c], f));
  }
  return [0, 1, 2].map((c) => a.v[c] + (end[c] - a.v[c]) * f);
}

export function localTime(anim, t) {
  if (anim.loop) return ((t % anim.length) + anim.length) % anim.length;
  return Math.min(Math.max(t, 0), anim.length);
}

/** Pose of all animated bones: { name: { rot, pos, scale } } plus visibility map. */
export function evalAnim(anim, t, weight = 1) {
  const lt = localTime(anim, t);
  const pose = {};
  for (const [name, ch] of Object.entries(anim.bones ?? {})) {
    const lp = !!anim.loop;
    const r = sampleChannel(ch.rotation, lt, lp), p = sampleChannel(ch.position, lt, lp), s = sampleChannel(ch.scale, lt, lp);
    pose[name] = {
      rot: r ? r.map((v) => v * weight) : null,
      pos: p ? p.map((v) => v * weight) : null,
      scale: s ? s.map((v) => 1 + (v - 1) * weight) : null,
    };
  }
  const vis = new Map();
  for (const e of [...(anim.events ?? [])].sort((a, b) => a.t - b.t)) if (e.type === "visibility" && e.t <= lt + 1e-9) vis.set(e.bone, e.visible);
  return { pose, vis };
}

function mergePoses(list) {
  const pose = {};
  const vis = new Map();
  for (const p of list) {
    for (const [k, v] of Object.entries(p.pose)) {
      const o = (pose[k] ??= { rot: null, pos: null, scale: null });
      if (v.rot) o.rot = o.rot ? o.rot.map((x, i) => x + v.rot[i]) : v.rot;
      if (v.pos) o.pos = o.pos ? o.pos.map((x, i) => x + v.pos[i]) : v.pos;
      if (v.scale) o.scale = o.scale ? o.scale.map((x, i) => x * v.scale[i]) : v.scale;
    }
    for (const [k, v] of p.vis) vis.set(k, v);
  }
  return { pose, vis };
}

// ------------------------------------------------------------------------------------------------ posing
/** node = { M (local-from-pivot → world), pivot (model space) } */
function childNode(parent, pivotSpec, restRot, anim) {
  const piv = mcPos(pivotSpec);
  const off = anim?.pos ? mcOff(anim.pos) : [0, 0, 0];
  const rot = [0, 1, 2].map((i) => (restRot?.[i] ?? 0) + (anim?.rot?.[i] ?? 0));
  const [rx, ry, rz] = mcRot(rot);
  const sc = anim?.scale ?? [1, 1, 1];
  const M = mul(mul(mul(parent.M, T(piv[0] - parent.pivot[0] + off[0], piv[1] - parent.pivot[1] + off[1], piv[2] - parent.pivot[2] + off[2])), Rzyx(rx, ry, rz)), S(sc[0], sc[1], sc[2]));
  return { M, pivot: piv };
}

/**
 * Compute all nodes (player parts + bundle bones) for a time.
 * opts: { anims: [{ name, t, weight }], worldOffset: [x,y,z] spec px, petScale, hideBones }
 */
export function computeNodes(B, opts = {}) {
  const anims = (opts.anims ?? []).filter((a) => B?.json.animations?.[a.name]).map((a) => evalAnim(B.json.animations[a.name], a.t, a.weight ?? 1));
  const { pose, vis } = mergePoses(anims);
  const nodes = new Map();
  const base = { M: WORLD, pivot: [0, 0, 0] };
  const root = childNode(base, [0, 0, 0], [0, 0, 0], pose.root);
  nodes.set("root", root);
  for (const p of PLAYER_PARTS) nodes.set(p.name, childNode(root, p.pivot, [0, 0, 0], pose[p.name]));
  const off = opts.worldOffset ?? [0, 0, 0];
  const s = opts.petScale ?? 1;
  const worldM = mul(mul(mul(mul(WORLD, T(...mcOff(off))), T(0, 24, 0)), S(s)), T(0, -24, 0));
  nodes.set("world", { M: worldM, pivot: [0, 0, 0] });
  const hidden = new Set();
  if (B) {
    const done = new Set();
    const visit = (b) => {
      if (done.has(b.name)) return;
      done.add(b.name);
      let parent;
      if (b.parent) {
        visit(B.bones.get(b.parent));
        parent = nodes.get("bone:" + b.parent);
        if (hidden.has(b.parent)) hidden.add(b.name);
      } else parent = nodes.get(b.attach ?? "body");
      nodes.set("bone:" + b.name, childNode(parent, b.pivot ?? [0, 0, 0], b.rotation, pose[b.name]));
      if (vis.get(b.name) === false || opts.hideBones?.includes(b.name)) hidden.add(b.name);
    };
    for (const b of B.bones.values()) visit(b);
  }
  return { nodes, hidden };
}

// ------------------------------------------------------------------------------------------------ geometry
// Face corner tables in model space (y down). Each entry: [corner selector, uv corner] where corner = [xMax?, yMax?, zMax?]
// and uv corner = [u2?, v2?] relative to the face rect [u1,v1,u2,v2].
const FACE_DEF = {
  west: { n: [-1, 0, 0], c: [[[0, 0, 0], [1, 0]], [[0, 0, 1], [0, 0]], [[0, 1, 1], [0, 1]], [[0, 1, 0], [1, 1]]] },
  north: { n: [0, 0, -1], c: [[[1, 0, 0], [1, 0]], [[0, 0, 0], [0, 0]], [[0, 1, 0], [0, 1]], [[1, 1, 0], [1, 1]]] },
  east: { n: [1, 0, 0], c: [[[1, 0, 1], [1, 0]], [[1, 0, 0], [0, 0]], [[1, 1, 0], [0, 1]], [[1, 1, 1], [1, 1]]] },
  south: { n: [0, 0, 1], c: [[[0, 0, 1], [1, 0]], [[1, 0, 1], [0, 0]], [[1, 1, 1], [0, 1]], [[0, 1, 1], [1, 1]]] },
  up: { n: [0, -1, 0], c: [[[1, 0, 1], [1, 0]], [[0, 0, 1], [0, 0]], [[0, 0, 0], [0, 1]], [[1, 0, 0], [1, 1]]] },
  down: { n: [0, 1, 0], c: [[[1, 1, 1], [1, 0]], [[0, 1, 1], [0, 0]], [[0, 1, 0], [0, 1]], [[1, 1, 0], [1, 1]]] },
};

function boxRects(u, v, w, h, d) {
  return {
    up: [u + d, v, u + d + w, v + d],
    down: [u + d + w, v, u + d + 2 * w, v + d],
    west: [u, v + d, u + d, v + d + h],
    north: [u + d, v + d, u + d + w, v + d + h],
    east: [u + d + w, v + d, u + 2 * d + w, v + d + h],
    south: [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
  };
}

/**
 * Emit quads for one cube given in model space (min/max), into `out`.
 * cube: { min, max, inflate, mirror, rects (per-face [u1,v1,u2,v2] or undefined), tex, cubeM (optional model-space matrix) }
 */
function emitCube(out, node, cube, flags, texScale = [1, 1]) {
  const inf = cube.inflate ?? 0;
  const mn = cube.min.map((v) => v - inf), mx = cube.max.map((v) => v + inf);
  const corner = (sel) => {
    let p = [sel[0] ? mx[0] : mn[0], sel[1] ? mx[1] : mn[1], sel[2] ? mx[2] : mn[2]];
    if (cube.mirror) p[0] = mn[0] + mx[0] - p[0];
    if (cube.cubeM) p = apply(cube.cubeM, p);
    return apply(node.M, sub(p, node.pivot));
  };
  for (const [face, def] of Object.entries(FACE_DEF)) {
    const rect = cube.rects[face];
    if (!rect) continue;
    let n = cube.mirror && (face === "east" || face === "west") ? [-def.n[0], 0, 0] : def.n;
    if (cube.cubeM) n = applyDir(cube.cubeM, n);
    n = norm(applyDir(node.M, n));
    const [u1, v1, u2, v2] = rect.map((x, i) => x * texScale[i % 2]);
    const pts = def.c.map(([sel, [su, sv]]) => {
      let uu = su ? u2 : u1;
      if (cube.mirror) uu = su ? u1 : u2;
      return { p: corner(sel), uv: [uu, sv ? v2 : v1] };
    });
    if (Math.abs(u2 - u1) < 1e-6 || Math.abs(v2 - v1) < 1e-6) continue;
    out.push({ pts, n, tex: cube.tex, rect: [Math.min(u1, u2), Math.min(v1, v2), Math.max(u1, u2), Math.max(v1, v2)], ...flags });
  }
}

function specCubeToModel(c) {
  const a = mcPos(c.from), b = mcPos(c.to);
  return { min: [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])], max: [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])] };
}

// Blockbench face → model-space face (FACE_DEF names are model space, "up" = visually up). Blockbench's east (+X) is
// model −X. The rect transform maps Blockbench's per-face UV orientation onto the vanilla box-UV orientation used by
// FACE_DEF (derived from Blockbench's own box-UV → per-face conversion, which must render like vanilla box UV:
// up = [u+d+w, v+d, u+d, v], down = [u+d+2w, v, u+d+w, v+d], the four sides unchanged).
const BB_FACE = {
  north: ["north", (r) => r],
  south: ["south", (r) => r],
  east: ["west", (r) => r],
  west: ["east", (r) => r],
  up: ["up", ([a, b, c, d]) => [c, d, a, b]],
  down: ["down", ([a, b, c, d]) => [c, b, a, d]],
};

function cubeRects(c, size) {
  if (c.uv?.box) return boxRects(c.uv.box[0], c.uv.box[1], ...size);
  const r = {};
  for (const [f, [mf, xf]] of Object.entries(BB_FACE)) {
    const fv = c.uv?.[f];
    if (fv) r[mf] = xf(Array.isArray(fv) ? fv : fv.uv);
  }
  return r;
}

/**
 * Build the scene (quads + particles) for a bundle at a time.
 * opts: { player: bool, anims: [{name,t,weight}], time (texture animation, s), worldOffset, petScale,
 *         capeOnly, particles: bool, extraParts }
 */
export function buildScene(B, opts = {}) {
  const quads = [];
  const time = opts.time ?? 0;
  const { nodes, hidden } = computeNodes(B, opts);
  const slot = B?.json.cosmetic?.slot;
  // ---------------------------------------------------------------- player
  if (opts.player) {
    const tex = playerTex();
    for (const p of PLAYER_PARTS) {
      const m = specCubeToModel(p);
      const size = [0, 1, 2].map((i) => m.max[i] - m.min[i]);
      emitCube(quads, nodes.get(p.name), { ...m, rects: boxRects(p.uv[0], p.uv[1], ...size), tex: { ...tex, frame: 0 } }, { player: true });
    }
  }
  if (!B) return { quads, particles: [], nodes };
  const texAt = (id) => {
    const t = B.tex.get(id);
    if (!t) return null;
    const fi = t.order[Math.floor((time * 20) / t.frameTime) % t.order.length];
    return { ...t, frame: fi };
  };
  // ---------------------------------------------------------------- cape (vanilla cape model on the body)
  if (slot === "cape") {
    const t = texAt(B.json.cosmetic.cape?.texture);
    const body = nodes.get("body");
    const M = mul(mul(mul(body.M, T(0, 0, 2)), Rx(deg(opts.capeAngle ?? 6))), Ry(Math.PI));
    const sc = t.width / 64;
    emitCube(quads, { M, pivot: [0, 0, 0] }, { min: [-5, 0, -1], max: [5, 16, 0], rects: boxRects(0, 0, 10, 16, 1), tex: t }, {}, [sc, sc]);
  }
  // ---------------------------------------------------------------- shield (vanilla shield model, standalone)
  if (slot === "shield" && opts.shieldNode !== false) {
    const t = texAt(B.json.cosmetic.shield?.texture);
    const sc = t.width / 64;
    const M = opts.shieldM ?? mul(WORLD, T(0, 8, 0));
    const node = { M, pivot: [0, 0, 0] };
    emitCube(quads, node, { min: [-6, -11, -2], max: [6, 11, -1], rects: boxRects(0, 0, 12, 22, 1), tex: t }, {}, [sc, sc]);
    emitCube(quads, node, { min: [-1, -3, -1], max: [1, 3, 5], rects: boxRects(26, 0, 2, 6, 6), tex: t }, {}, [sc, sc]);
  }
  // ---------------------------------------------------------------- model bones
  const defTex = B.json.model?.texture;
  for (const b of B.bones.values()) {
    if (hidden.has(b.name)) continue;
    const node = nodes.get("bone:" + b.name);
    for (const c of b.cubes ?? []) {
      const m = specCubeToModel(c);
      const size = [0, 1, 2].map((i) => Math.abs(c.to[i] - c.from[i]));
      let cubeM = null;
      if (c.rotation && c.rotation.some((v) => v)) {
        const cp = mcPos(c.pivot ?? [0, 0, 0]);
        cubeM = mul(mul(T(...cp), Rzyx(...mcRot(c.rotation))), T(-cp[0], -cp[1], -cp[2]));
      }
      const tex = texAt(c.texture ?? defTex);
      if (!tex) continue;
      emitCube(quads, node, { ...m, inflate: c.inflate, mirror: c.mirror, rects: cubeRects(c, size), tex, cubeM }, { emissive: !!b.emissive, translucent: !!b.translucent });
    }
  }
  const particles = opts.particles === false ? [] : simulateParticles(B, opts);
  return { quads, particles, nodes, textures: B.tex };
}

// ------------------------------------------------------------------------------------------------ particles
const VANILLA_PARTICLES = {
  "minecraft:note": { life: 0.6, color: null, glyph: "note", size: 0.25, emissive: true },
  "minecraft:snowflake": { life: 1.2, color: "#ffffff", glyph: "flake", size: 0.1, gravity: 0.0025 },
  "minecraft:heart": { life: 0.8, color: "#ff4d6d", glyph: "heart", size: 0.25 },
  "minecraft:end_rod": { life: 1.5, color: "#fffbe8", glyph: "dot", size: 0.12, emissive: true },
  "minecraft:happy_villager": { life: 0.8, color: "#5dd36b", glyph: "dot", size: 0.12 },
  "minecraft:firework": { life: 1.0, color: "#ffffff", glyph: "dot", size: 0.12, emissive: true },
  "minecraft:cloud": { life: 1.0, color: "#f0f0f0", glyph: "puff", size: 0.3 },
  "minecraft:poof": { life: 0.5, color: "#f0f0f0", glyph: "puff", size: 0.25 },
  "minecraft:bubble_pop": { life: 0.4, color: "#bfe8ff", glyph: "ring", size: 0.15 },
  "minecraft:splash": { life: 0.6, color: "#6fb7ff", glyph: "dot", size: 0.1, gravity: 0.04 },
};
const NOTE_COLORS = ["#5dd36b", "#3fe0c5", "#8b6cff", "#ff6fae", "#ffd25a"];

function locatorWorld(B, locName, te, opts, animName) {
  const loc = B.locators[locName];
  if (!loc) return [0, 12, 0];
  const anims = (opts.anims ?? []).map((a) => (a.name === animName ? { ...a, t: te } : a));
  const { nodes } = computeNodes(B, { ...opts, anims });
  const node = PLAYER_NAMES.includes(loc.bone) || loc.bone === "root" ? nodes.get(loc.bone) : nodes.get("bone:" + loc.bone);
  if (!node) return [0, 12, 0];
  return apply(node.M, sub(mcPos(loc.position), node.pivot));
}

/** Deterministic particle simulation: all particles alive at the current time of each playing animation. */
export function simulateParticles(B, opts) {
  const out = [];
  for (const a of opts.anims ?? []) {
    const anim = B.json.animations?.[a.name];
    if (!anim?.events?.length) continue;
    const loops = anim.loop ? Math.ceil(3 / anim.length) + 1 : 0;
    anim.events.forEach((ev, ei) => {
      if (ev.type !== "particle") return;
      const vp = ev.particle ? VANILLA_PARTICLES[ev.particle] ?? { life: 1, color: "#ffffff", glyph: "dot", size: 0.12 } : null;
      const life = ev.lifetime ?? vp?.life ?? 1;
      for (let k = 0; k <= loops; k++) {
        const te = ev.t + (anim.loop ? Math.floor(a.t / anim.length) * anim.length - k * anim.length : 0);
        const age = a.t - te;
        if (age < 0 || age > life) continue;
        if (!anim.loop && te > anim.length) continue;
        const origin = locatorWorld(B, ev.locator, te, opts, a.name);
        const r = rng(1000 + ei * 7919 + Math.round(te * 1000));
        const n = Math.round(ev.count ?? 1);
        for (let i = 0; i < n; i++) {
          const sp = ev.spread ?? [0, 0, 0];
          const vel = ev.velocity ?? [0, 0, 0];
          // spread/velocity are in blocks, Blockbench handedness → world: x and z flip (spread is a symmetric
          // half-extent, so only the velocity direction matters)
          const off = [(r() * 2 - 1) * sp[0], (r() * 2 - 1) * sp[1], (r() * 2 - 1) * sp[2]];
          const jitter = 0.3;
          const v = vel.map((x, c) => (c === 0 ? -x : x) * (1 + (r() * 2 - 1) * jitter));
          // a little random sideways drift so bursts fan out
          const drift = [(r() * 2 - 1) * Math.abs(vel[1]) * 0.35, 0, (r() * 2 - 1) * Math.abs(vel[1]) * 0.35];
          const g = ev.gravity ?? vp?.gravity ?? 0;
          const ticks = age * 20;
          const pos = [
            origin[0] + (off[0] + (v[0] + drift[0]) * ticks) * 16,
            origin[1] + (off[1] + v[1] * ticks - 0.5 * g * ticks * ticks) * 16,
            origin[2] - (off[2] + (v[2] + drift[2]) * ticks) * 16,
          ];
          const lifeFrac = age / life;
          let color = vp?.color ?? "#ffffff";
          if (vp?.glyph === "note") color = NOTE_COLORS[(ei + i) % NOTE_COLORS.length];
          out.push({
            p: pos,
            size: (ev.size ?? vp?.size ?? 0.15) * 16 * (lifeFrac > 0.7 ? 1 - (lifeFrac - 0.7) / 0.3 * 0.6 : 1),
            sprite: ev.sprite ? B.sprites.get(ev.sprite) : null,
            glyph: vp?.glyph,
            color,
            emissive: ev.emissive ?? vp?.emissive ?? false,
            alpha: lifeFrac > 0.75 ? 1 - (lifeFrac - 0.75) / 0.25 : 1,
            rot: Math.floor(r() * 4),
          });
        }
      }
    });
  }
  return out;
}

const GLYPHS = {
  note: ["..##", "..#.", "..#.", "###.", "##.."],
  flake: ["#.#.#", ".###.", "##.##", ".###.", "#.#.#"],
  heart: [".#.#.", "#####", "#####", ".###.", "..#.."],
  dot: ["##", "##"],
  puff: [".##.", "####", "####", ".##."],
  ring: [".##.", "#..#", "#..#", ".##."],
};

// ------------------------------------------------------------------------------------------------ camera
export function camera(yawDeg, pitchDeg) {
  const y = deg(yawDeg), p = deg(pitchDeg);
  const toCam = [Math.sin(y) * Math.cos(p), Math.sin(p), Math.cos(y) * Math.cos(p)];
  const fwd = toCam.map((v) => -v);
  const right = norm(cross(fwd, [0, 1, 0]));
  const up = cross(right, fwd);
  return { toCam, right, up };
}

const proj = (cam, p) => [dot(p, cam.right), -dot(p, cam.up), dot(p, cam.toCam)];

export function sceneBounds(scene, cam, b = { x0: Infinity, y0: Infinity, x1: -Infinity, y1: -Infinity }, withParticles = true) {
  for (const q of scene.quads)
    for (const pt of q.pts) {
      const [x, y] = proj(cam, pt.p);
      b.x0 = Math.min(b.x0, x); b.x1 = Math.max(b.x1, x); b.y0 = Math.min(b.y0, y); b.y1 = Math.max(b.y1, y);
    }
  for (const pa of withParticles ? scene.particles : []) {
    const [x, y] = proj(cam, pa.p);
    const r = pa.size / 2;
    b.x0 = Math.min(b.x0, x - r); b.x1 = Math.max(b.x1, x + r); b.y0 = Math.min(b.y0, y - r); b.y1 = Math.max(b.y1, y + r);
  }
  return b;
}

export function fitView(bounds, W, H, margin = 16, maxScale = 12) {
  const bw = Math.max(1, bounds.x1 - bounds.x0), bh = Math.max(1, bounds.y1 - bounds.y0);
  const scale = Math.min(maxScale, (W - 2 * margin) / bw, (H - 2 * margin) / bh);
  return { scale, cx: (bounds.x0 + bounds.x1) / 2, cy: (bounds.y0 + bounds.y1) / 2 };
}

// ------------------------------------------------------------------------------------------------ raster
function texel(tex, u, v) {
  const x = Math.min(tex.width - 1, Math.max(0, Math.floor(u)));
  const y = Math.min(tex.height - 1, Math.max(0, Math.floor(v))) + tex.frame * tex.height;
  const d = tex.img.data, i = (y * tex.img.width + x) * 4;
  return [d[i], d[i + 1], d[i + 2], d[i + 3]];
}

/**
 * Render a scene. opts: { width, height, ss (supersampling), cam, view {scale,cx,cy}, bloom }
 * Returns an Img with transparent background (premultiplied → straight alpha).
 */
export function render(scene, opts) {
  const ss = opts.ss ?? 3;
  const W = opts.width * ss, H = opts.height * ss;
  const cam = opts.cam;
  const view = opts.view;
  const sc = view.scale * ss;
  const ox = W / 2 - view.cx * sc, oy = H / 2 - view.cy * sc;
  const N = W * H;
  const col = new Float32Array(N * 4); // premultiplied rgba
  const zb = new Float32Array(N).fill(-Infinity);
  const glow = new Float32Array(N * 3);

  const key = norm([...cam.right.map((v) => v * -0.55)].map((v, i) => v + cam.up[i] * 0.8 + cam.toCam[i] * 0.55));
  const light = (n) => {
    const kd = Math.max(0, dot(n, key));
    const top = Math.max(0, n[1]);
    return Math.min(1.04, 0.46 + 0.36 * kd + 0.3 * top);
  };

  const tris = [];
  for (const q of scene.quads) {
    if (dot(q.n, cam.toCam) <= 1e-4) continue; // back-face cull
    const P = q.pts.map((pt) => {
      const [x, y, z] = proj(cam, pt.p);
      return [x * sc + ox, y * sc + oy, z, pt.uv[0], pt.uv[1]];
    });
    const b = q.emissive ? 1 : light(q.n);
    const item = { q, b };
    tris.push([P[0], P[1], P[2], item], [P[0], P[2], P[3], item]);
  }

  const raster = (A, Bv, C, cb) => {
    const x0 = Math.max(0, Math.floor(Math.min(A[0], Bv[0], C[0]))), x1 = Math.min(W - 1, Math.ceil(Math.max(A[0], Bv[0], C[0])));
    const y0 = Math.max(0, Math.floor(Math.min(A[1], Bv[1], C[1]))), y1 = Math.min(H - 1, Math.ceil(Math.max(A[1], Bv[1], C[1])));
    const area = (Bv[0] - A[0]) * (C[1] - A[1]) - (Bv[1] - A[1]) * (C[0] - A[0]);
    if (Math.abs(area) < 1e-9) return;
    for (let y = y0; y <= y1; y++)
      for (let x = x0; x <= x1; x++) {
        const px = x + 0.5, py = y + 0.5;
        let w0 = ((Bv[0] - px) * (C[1] - py) - (Bv[1] - py) * (C[0] - px)) / area;
        let w1 = ((C[0] - px) * (A[1] - py) - (C[1] - py) * (A[0] - px)) / area;
        let w2 = 1 - w0 - w1;
        if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
        cb(x, y, w0, w1, w2);
      }
  };

  const shadePix = (item, A, Bv, C, w0, w1, w2) => {
    const r = item.q.rect;
    let u = A[3] * w0 + Bv[3] * w1 + C[3] * w2, v = A[4] * w0 + Bv[4] * w1 + C[4] * w2;
    u = Math.min(r[2] - 1e-3, Math.max(r[0] + 1e-3, u));
    v = Math.min(r[3] - 1e-3, Math.max(r[1] + 1e-3, v));
    return texel(item.q.tex, u, v);
  };

  // opaque / cutout
  for (const [A, Bv, C, item] of tris) {
    if (item.q.translucent) continue;
    raster(A, Bv, C, (x, y, w0, w1, w2) => {
      const z = A[2] * w0 + Bv[2] * w1 + C[2] * w2;
      const i = y * W + x;
      if (z <= zb[i]) return;
      const t = shadePix(item, A, Bv, C, w0, w1, w2);
      if (t[3] < 128) return;
      zb[i] = z;
      const b = item.b;
      col[i * 4] = (t[0] / 255) * b; col[i * 4 + 1] = (t[1] / 255) * b; col[i * 4 + 2] = (t[2] / 255) * b; col[i * 4 + 3] = 1;
      if (item.q.emissive) { glow[i * 3] = t[0] / 255; glow[i * 3 + 1] = t[1] / 255; glow[i * 3 + 2] = t[2] / 255; }
      else glow[i * 3] = glow[i * 3 + 1] = glow[i * 3 + 2] = 0;
    });
  }
  // translucent, back to front
  const trans = tris.filter((t) => t[3].q.translucent).sort((a, b) => (a[0][2] + a[1][2] + a[2][2]) - (b[0][2] + b[1][2] + b[2][2]));
  for (const [A, Bv, C, item] of trans) {
    raster(A, Bv, C, (x, y, w0, w1, w2) => {
      const z = A[2] * w0 + Bv[2] * w1 + C[2] * w2;
      const i = y * W + x;
      if (z <= zb[i]) return;
      const t = shadePix(item, A, Bv, C, w0, w1, w2);
      const a = t[3] / 255;
      if (a < 0.02) return;
      const b = item.b;
      for (let c = 0; c < 3; c++) col[i * 4 + c] = (t[c] / 255) * b * a + col[i * 4 + c] * (1 - a);
      col[i * 4 + 3] = a + col[i * 4 + 3] * (1 - a);
      if (item.q.emissive) for (let c = 0; c < 3; c++) glow[i * 3 + c] = Math.max(glow[i * 3 + c] * (1 - a), (t[c] / 255) * a);
      else for (let c = 0; c < 3; c++) glow[i * 3 + c] *= 1 - a;
    });
  }
  // particles (camera-facing squares)
  for (const pa of scene.particles) {
    const [x, y, z] = proj(cam, pa.p);
    const cx = x * sc + ox, cy = y * sc + oy;
    const size = Math.max(2, pa.size * sc);
    let sampler;
    if (pa.sprite) {
      const t = scene.textures?.get(pa.sprite.texture) ?? pa.spriteTex;
      const [u1, v1, u2, v2] = pa.sprite.uv;
      sampler = (fx, fy) => (t ? texel({ ...t, frame: 0 }, u1 + (u2 - u1) * fx, v1 + (v2 - v1) * fy) : [255, 255, 255, 255]);
    } else {
      const g = GLYPHS[pa.glyph ?? "dot"];
      const c = rgba(pa.color);
      sampler = (fx, fy) => (g[Math.floor(fy * g.length)][Math.floor(fx * g[0].length)] === "#" ? c : [0, 0, 0, 0]);
    }
    const x0 = Math.max(0, Math.floor(cx - size / 2)), x1 = Math.min(W - 1, Math.ceil(cx + size / 2));
    const y0 = Math.max(0, Math.floor(cy - size / 2)), y1 = Math.min(H - 1, Math.ceil(cy + size / 2));
    for (let py = y0; py <= y1; py++)
      for (let px = x0; px <= x1; px++) {
        const fx = (px + 0.5 - (cx - size / 2)) / size, fy = (py + 0.5 - (cy - size / 2)) / size;
        if (fx < 0 || fy < 0 || fx >= 1 || fy >= 1) continue;
        const i = py * W + px;
        if (z <= zb[i]) continue;
        const t = sampler(fx, fy);
        const a = (t[3] / 255) * pa.alpha;
        if (a < 0.05) continue;
        for (let c = 0; c < 3; c++) col[i * 4 + c] = (t[c] / 255) * a + col[i * 4 + c] * (1 - a);
        col[i * 4 + 3] = a + col[i * 4 + 3] * (1 - a);
        if (pa.emissive) for (let c = 0; c < 3; c++) glow[i * 3 + c] = Math.max(glow[i * 3 + c], (t[c] / 255) * a);
      }
  }

  // downsample
  const w = opts.width, h = opts.height;
  const out = new Float32Array(w * h * 4);
  const gl = new Float32Array(w * h * 3);
  const k = 1 / (ss * ss);
  for (let y = 0; y < h; y++)
    for (let x = 0; x < w; x++) {
      const o = (y * w + x);
      for (let sy = 0; sy < ss; sy++)
        for (let sx = 0; sx < ss; sx++) {
          const i = (y * ss + sy) * W + (x * ss + sx);
          for (let c = 0; c < 4; c++) out[o * 4 + c] += col[i * 4 + c] * k;
          for (let c = 0; c < 3; c++) gl[o * 3 + c] += glow[i * 3 + c] * k;
        }
    }
  // bloom
  const bloom = opts.bloom ?? 0.55;
  if (bloom > 0) {
    let g = gl;
    const rad = Math.max(2, Math.round(Math.min(w, h) / 70));
    for (let pass = 0; pass < 3; pass++) g = boxBlur(g, w, h, rad);
    for (let i = 0; i < w * h; i++) {
      let m = 0;
      for (let c = 0; c < 3; c++) {
        const add = g[i * 3 + c] * bloom;
        out[i * 4 + c] += add;
        m = Math.max(m, add);
      }
      out[i * 4 + 3] = Math.min(1, out[i * 4 + 3] + m * 0.9);
    }
  }
  const img = new Img(w, h);
  for (let i = 0; i < w * h; i++) {
    const a = Math.min(1, out[i * 4 + 3]);
    if (a <= 0.001) continue;
    img.data[i * 4] = Math.min(255, (out[i * 4] / a) * 255);
    img.data[i * 4 + 1] = Math.min(255, (out[i * 4 + 1] / a) * 255);
    img.data[i * 4 + 2] = Math.min(255, (out[i * 4 + 2] / a) * 255);
    img.data[i * 4 + 3] = a * 255;
  }
  return img;
}

function boxBlur(src, w, h, r) {
  const tmp = new Float32Array(src.length), dst = new Float32Array(src.length);
  const n = 2 * r + 1;
  for (let y = 0; y < h; y++)
    for (let c = 0; c < 3; c++) {
      let acc = 0;
      for (let x = -r; x <= r; x++) acc += src[(y * w + Math.min(w - 1, Math.max(0, x))) * 3 + c];
      for (let x = 0; x < w; x++) {
        tmp[(y * w + x) * 3 + c] = acc / n;
        acc += src[(y * w + Math.min(w - 1, x + r + 1)) * 3 + c] - src[(y * w + Math.max(0, x - r)) * 3 + c];
      }
    }
  for (let x = 0; x < w; x++)
    for (let c = 0; c < 3; c++) {
      let acc = 0;
      for (let y = -r; y <= r; y++) acc += tmp[(Math.min(h - 1, Math.max(0, y)) * w + x) * 3 + c];
      for (let y = 0; y < h; y++) {
        dst[(y * w + x) * 3 + c] = acc / n;
        acc += tmp[(Math.min(h - 1, y + r + 1) * w + x) * 3 + c] - tmp[(Math.max(0, y - r) * w + x) * 3 + c];
      }
    }
  return dst;
}
