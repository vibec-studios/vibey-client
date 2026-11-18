// Builder API for `.vibey` bundles (docs/SPEC.md §5) + deterministic JSON writer.
//
// Coordinates are Blockbench units as in the spec: pixels, origin at the player's feet, +Y up, the player
// faces north (-Z), the player's right arm is at -X. Cubes use box UV; their texture regions are packed
// automatically into the bundle's textures and painted by the cube's painter (see `Bone.box`).
import { Img } from "./canvas.mjs";

export const PLAYER_BONES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "root"];
export const ATTACH = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "root", "world"];
export const FACES = ["north", "east", "south", "west", "up", "down"];

/** Vanilla player part pivots (spec §5). */
export const PIVOTS = {
  head: [0, 24, 0],
  body: [0, 24, 0],
  right_arm: [-5, 22, 0],
  left_arm: [5, 22, 0],
  right_leg: [-1.9, 12, 0],
  left_leg: [1.9, 12, 0],
  root: [0, 0, 0],
};

/**
 * Box UV layout (identical to vanilla ModelPart cubes): for a cube of size w (x) × h (y) × d (z) at [u, v].
 * Returns per-face [u1, v1, u2, v2] rectangles in texture pixels.
 */
export function boxUV(u, v, w, h, d) {
  return {
    up: [u + d, v, u + d + w, v + d],
    down: [u + d + w, v, u + d + 2 * w, v + d],
    west: [u, v + d, u + d, v + d + h],
    north: [u + d, v + d, u + d + w, v + d + h],
    east: [u + d + w, v + d, u + 2 * d + w, v + d + h],
    south: [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
  };
}

const vec3 = (v) => (typeof v === "number" ? [v, v, v] : v);

// -------------------------------------------------------------------------------------------- textures
class Region {
  constructor(tex, w, h, paint) {
    this.tex = tex;
    this.w = w;
    this.h = h;
    this.paint = paint;
    this.u = null;
    this.v = null;
  }
}

export class Texture {
  /**
   * @param id texture id
   * @param opts { width, height } fixed frame size, or omit for auto-sizing (power of two that fits).
   *             { frames, frameTime, frameOrder } for animated vertical strips.
   */
  constructor(bundle, id, opts = {}) {
    this.bundle = bundle;
    this.id = id;
    this.fixed = opts.width != null;
    this.width = opts.width ?? 0;
    this.height = opts.height ?? 0;
    this.frames = opts.frames ?? 1;
    this.frameTime = opts.frameTime ?? 2;
    this.frameOrder = opts.frameOrder ?? null;
    this.regions = [];
    this.painters = []; // whole-frame painters (img, frameIndex)
    this.minSize = opts.minSize ?? 16;
  }

  /** Paint the whole frame (for fixed-layout textures like capes/shields). painter(img, frame) */
  paint(painter) {
    this.painters.push(painter);
    return this;
  }

  /** Reserve a w×h region, painted by painter(view, frame). Position is resolved at build time. */
  region(w, h, painter) {
    const r = new Region(this, w, h, painter);
    this.regions.push(r);
    return r;
  }

  _pack() {
    if (!this.regions.length) {
      if (!this.fixed) this.width = this.height = this.minSize;
      return;
    }
    const items = [...this.regions].sort((a, b) => b.h - a.h || b.w - a.w);
    const tryPack = (W, H) => {
      let x = 0, y = 0, shelf = 0;
      const pos = new Map();
      for (const r of items) {
        const w = Math.ceil(r.w), h = Math.ceil(r.h);
        if (w > W) return null;
        if (x + w > W) { x = 0; y += shelf; shelf = 0; }
        if (y + h > H) return null;
        pos.set(r, [x, y]);
        x += w;
        shelf = Math.max(shelf, h);
      }
      return pos;
    };
    let W = this.fixed ? this.width : this.minSize, H = this.fixed ? this.height : this.minSize;
    let pos = tryPack(W, H);
    while (!pos) {
      if (this.fixed) throw new Error(`texture ${this.id}: regions do not fit into ${this.width}x${this.height}`);
      if (W <= H) W *= 2; else H *= 2;
      if (W > 512 || H > 512) throw new Error(`texture ${this.id}: too many regions`);
      pos = tryPack(W, H);
    }
    for (const [r, [u, v]] of pos) { r.u = u; r.v = v; }
    this.width = W;
    this.height = H;
  }

  _render() {
    const img = new Img(this.width, this.height * this.frames);
    for (let f = 0; f < this.frames; f++) {
      const frame = img.view(0, f * this.height, this.width, this.height);
      for (const p of this.painters) p(frame, f);
      for (const r of this.regions) if (r.paint) r.paint(frame.view(r.u, r.v, Math.ceil(r.w), Math.ceil(r.h)), f);
    }
    return img;
  }
}

// -------------------------------------------------------------------------------------------- bones
export class Bone {
  constructor(bundle, name, opts = {}) {
    this.bundle = bundle;
    this.name = name;
    this.parent = opts.parent ?? null;
    this.attach = opts.attach ?? null;
    this.pivot = opts.pivot ?? [0, 0, 0];
    this.rotation = opts.rotation ?? [0, 0, 0];
    this.emissive = !!opts.emissive;
    this.translucent = !!opts.translucent;
    this.cubes = [];
  }

  /**
   * Add a box-UV cube. `from` and `size` in pixels (absolute model coordinates).
   * paint: function(faces, frame) receiving { north, south, east, west, up, down } image views
   *        (each face's view is laid out as in vanilla box UV), or an object of per-face painters
   *        { north: (img, frame) => …, …, sides: …, all: … }.
   * opts: { inflate, mirror, rotation, pivot, texture }
   */
  box(from, size, paint, opts = {}) {
    const [w, h, d] = size;
    for (const s of size) if (s !== Math.round(s)) throw new Error(`${this.name}: box UV cubes need integer sizes (${size})`);
    const tex = opts.texture ?? this.bundle.defaultTexture();
    const region = tex.region(2 * d + 2 * w, d + h, (img, frame) => {
      const L = boxUV(0, 0, w, h, d);
      const faces = {};
      for (const f of FACES) {
        const [u1, v1, u2, v2] = L[f];
        faces[f] = img.view(u1, v1, u2 - u1, v2 - v1);
      }
      if (typeof paint === "function") paint(faces, frame);
      else if (paint) {
        for (const f of FACES) {
          const p = paint[f] ?? (["north", "south", "east", "west"].includes(f) ? paint.sides : null) ?? paint.all;
          if (p) p(faces[f], frame, f);
        }
      }
    });
    const cube = {
      from: [...from],
      to: [from[0] + w, from[1] + h, from[2] + d],
      region,
      texture: tex,
      inflate: opts.inflate ?? 0,
      mirror: !!opts.mirror,
      rotation: opts.rotation ?? null,
      pivot: opts.pivot ?? null,
    };
    this.cubes.push(cube);
    return this;
  }

  /** Same as box() but with the cube centred on `center` (x/z) and starting at y = center[1]. */
  boxC(center, size, paint, opts) {
    return this.box([center[0] - size[0] / 2, center[1], center[2] - size[2] / 2], size, paint, opts);
  }

  child(name, opts = {}) {
    return this.bundle.bone(name, { ...opts, parent: this.name });
  }
}

// -------------------------------------------------------------------------------------------- animations
export class Anim {
  constructor(name, length, loop) {
    this.name = name;
    this.length = length;
    this.loop = loop;
    this.bones = {};
    this.events = [];
  }

  _ch(bone, channel, keys, interp) {
    const b = (this.bones[bone] ??= {});
    b[channel] = keys
      .map((k) => {
        const [t, v, i] = Array.isArray(k) ? k : [k.t, k.v, k.i];
        const kf = { t: +t.toFixed(4), v: vec3(v) };
        const ii = i ?? interp;
        if (ii && ii !== "linear") kf.i = ii;
        return kf;
      })
      .sort((a, b) => a.t - b.t);
    return this;
  }

  /** keys: [[t, [x,y,z]], …] or [[t, v, interp]]; interp default "catmullrom". */
  rot(bone, keys, interp = "catmullrom") { return this._ch(bone, "rotation", keys, interp); }
  pos(bone, keys, interp = "catmullrom") { return this._ch(bone, "position", keys, interp); }
  scale(bone, keys, interp = "catmullrom") { return this._ch(bone, "scale", keys, interp); }

  particle(t, opts) {
    this.events.push({ t, type: "particle", ...opts });
    return this;
  }
  sound(t, sound, opts = {}) {
    this.events.push({ t, type: "sound", sound, volume: opts.volume ?? 1, pitch: opts.pitch ?? 1 });
    return this;
  }
  visible(t, bone, visible) {
    this.events.push({ t, type: "visibility", bone, visible });
    return this;
  }

  toJSON() {
    const out = { length: this.length, loop: this.loop, bones: this.bones };
    if (this.events.length) out.events = [...this.events].sort((a, b) => a.t - b.t).map((e) => ({ ...e, t: +e.t.toFixed(4) }));
    return out;
  }
}

// -------------------------------------------------------------------------------------------- bundle
export class Bundle {
  /**
   * meta: { kind, id, name: {en, de}, description: {en, de}, tags, rarity,
   *         cosmetic: { slot, hideWithArmor, pet, cape, shield }, emote: { animation, loop, ... } }
   */
  constructor(meta) {
    this.meta = { author: "Vibe Craft Studios", ...meta };
    this.textures = new Map();
    this.bones = new Map();
    this.locators = {};
    this.anims = new Map();
    this.sprites = [];
    this.sounds = [];
    this.modelTexture = null;
    // preview hints (not written): camera, frames, …
    this.preview = meta.preview ?? {};
    delete this.meta.preview;
  }

  get id() { return this.meta.id; }
  get file() { return this.meta.id.split(":")[1].replace(/[/.]/g, "_"); }

  texture(id, opts) {
    const t = new Texture(this, id, opts);
    this.textures.set(id, t);
    if (!this.modelTexture && !opts?.notModel) this.modelTexture = id;
    return t;
  }

  defaultTexture() {
    if (!this.modelTexture) this.texture("main");
    return this.textures.get(this.modelTexture);
  }

  bone(name, opts = {}) {
    if (this.bones.has(name)) throw new Error(`duplicate bone ${name}`);
    if (this.meta.kind === "emote" && PLAYER_BONES.includes(name)) throw new Error(`bone name ${name} is reserved for the player`);
    const b = new Bone(this, name, opts);
    this.bones.set(name, b);
    return b;
  }

  locator(name, bone, position) {
    this.locators[name] = { bone, position };
    return this;
  }

  anim(name, length, loop = false) {
    const a = new Anim(name, length, loop);
    this.anims.set(name, a);
    return a;
  }

  /** Sprite for sprite particles; `region` comes from texture.region(…) or is a fixed [u1,v1,u2,v2]. */
  sprite(id, texture, region) {
    this.sprites.push({ id, texture, region });
    return this;
  }

  sound(id, ogg) {
    this.sounds.push({ id, ogg });
    return this;
  }

  /** Build the JSON object (textures packed + painted + PNG encoded). */
  build() {
    const m = this.meta;
    const out = {
      format: "vibey",
      formatVersion: 1,
      kind: m.kind,
      id: m.id,
      name: m.name,
    };
    if (m.description) out.description = m.description;
    out.author = m.author;
    out.tags = m.tags ?? [];
    if (m.rarity) out.rarity = m.rarity;
    if (m.kind === "cosmetic") out.cosmetic = m.cosmetic;
    if (m.kind === "emote") out.emote = m.emote;

    const pngs = {};
    out.textures = [];
    for (const t of this.textures.values()) {
      t._pack();
      const img = t._render();
      pngs[t.id] = img;
      const tj = { id: t.id, width: t.width, height: t.height, png: img.toPNG().toString("base64") };
      if (t.frames > 1) tj.animation = { frameTime: t.frameTime, frames: t.frameOrder ?? [...Array(t.frames).keys()] };
      out.textures.push(tj);
    }

    if (this.bones.size || m.kind === "emote" || !["cape", "shield"].includes(m.cosmetic?.slot)) {
      const model = {};
      if (this.modelTexture && this.bones.size) model.texture = this.modelTexture;
      model.bones = [...this.bones.values()].map((b) => {
        const bj = { name: b.name, parent: b.parent };
        if (!b.parent) bj.attach = b.attach;
        bj.pivot = b.pivot;
        bj.rotation = b.rotation;
        bj.cubes = b.cubes.map((c) => {
          const cj = { from: c.from, to: c.to };
          if (c.inflate) cj.inflate = c.inflate;
          if (c.mirror) cj.mirror = true;
          if (c.rotation) { cj.pivot = c.pivot ?? [0, 0, 0]; cj.rotation = c.rotation; }
          cj.uv = { box: [c.region.u, c.region.v] };
          if (c.texture.id !== this.modelTexture)
            throw new Error(`${b.name}: all cubes must use the model texture (${this.modelTexture}), got ${c.texture.id}`);
          return cj;
        });
        bj.emissive = b.emissive;
        bj.translucent = b.translucent;
        return bj;
      });
      if (Object.keys(this.locators).length) model.locators = this.locators;
      out.model = model;
    }

    out.animations = {};
    for (const a of this.anims.values()) out.animations[a.name] = a.toJSON();
    if (this.sprites.length)
      out.sprites = this.sprites.map((s) => {
        const r = s.region;
        const uv = Array.isArray(r) ? r : [r.u, r.v, r.u + r.w, r.v + r.h];
        return { id: s.id, texture: s.texture, uv };
      });
    if (this.sounds.length) out.sounds = this.sounds.map((s) => ({ id: s.id, ogg: Buffer.from(s.ogg).toString("base64") }));
    return { json: out, images: pngs };
  }
}

// -------------------------------------------------------------------------------------------- writer
const round = (n) => {
  const r = Math.round(n * 10000) / 10000;
  return Object.is(r, -0) ? 0 : r;
};

/** Pretty JSON: short arrays/objects inline, numbers rounded to 4 decimals, LF line endings. */
export function stringify(value, indent = "") {
  const inline = (v) => JSON.stringify(v, (k, x) => (typeof x === "number" ? round(x) : x));
  const flat = inline(value);
  if (flat.length + indent.length <= 110 || value === null || typeof value !== "object") return flat;
  const next = indent + "  ";
  if (Array.isArray(value)) return "[\n" + value.map((v) => next + stringify(v, next)).join(",\n") + "\n" + indent + "]";
  const entries = Object.entries(value).filter(([, v]) => v !== undefined);
  return "{\n" + entries.map(([k, v]) => next + JSON.stringify(k) + ": " + stringify(v, next)).join(",\n") + "\n" + indent + "}";
}
