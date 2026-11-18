// Validator for `.vibey` v1 bundles – implements every rule of docs/SPEC.md §5.
// Usage: node tools/validate.mjs <file.vibey> [...]   (exit code 1 on errors)
import { readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { readPNGHeader } from "./png.mjs";

export const LIMITS = {
  fileBytes: 2 * 1024 * 1024,
  textureMax: 512,
  stripMaxHeight: 4096,
  bones: 256,
  cubes: 2048,
  soundBytes: 512 * 1024,
};

const KINDS = ["cosmetic", "emote"];
const SLOTS = ["cape", "wings", "aura", "pet", "shield", "hat", "back"];
const RARITIES = ["common", "rare", "epic", "legendary"];
const ARMOR = ["head", "chest", "legs", "feet"];
const ATTACH = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "root", "world"];
const PLAYER_BONES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "root"];
const FACES = ["north", "east", "south", "west", "up", "down"];
const INTERP = ["linear", "catmullrom", "step", "bezier"];
const ID_RE = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const SOUND_ID_RE = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;

const isNum = (v) => typeof v === "number" && Number.isFinite(v);
const isVec3 = (v) => Array.isArray(v) && v.length === 3 && v.every(isNum);
const isObj = (v) => v !== null && typeof v === "object" && !Array.isArray(v);

function b64(str) {
  if (typeof str !== "string" || !/^[A-Za-z0-9+/]*={0,2}$/.test(str) || str.length % 4 !== 0) return null;
  return Buffer.from(str, "base64");
}

/**
 * Validate a bundle. `input` may be a Buffer/string (file bytes) or a parsed object.
 * Returns { ok, errors: string[], warnings: string[] }.
 */
export function validate(input) {
  const errors = [];
  const warnings = [];
  const err = (m) => errors.push(m);
  const warn = (m) => warnings.push(m);
  let v = input;
  if (Buffer.isBuffer(input) || typeof input === "string") {
    const bytes = Buffer.isBuffer(input) ? input : Buffer.from(input, "utf8");
    if (bytes.length > LIMITS.fileBytes) err(`file is ${bytes.length} bytes (max ${LIMITS.fileBytes})`);
    try {
      v = JSON.parse(bytes.toString("utf8"));
    } catch (e) {
      return { ok: false, errors: [`invalid JSON: ${e.message}`], warnings };
    }
  }
  if (!isObj(v)) return { ok: false, errors: ["root must be an object"], warnings };

  // ------------------------------------------------------------------ header
  if (v.format !== "vibey") err(`format must be "vibey"`);
  if (v.formatVersion !== 1) err(`formatVersion must be 1`);
  if (!KINDS.includes(v.kind)) err(`kind must be one of ${KINDS.join("|")}`);
  if (typeof v.id !== "string" || !ID_RE.test(v.id)) err(`id "${v.id}" does not match ${ID_RE}`);
  const checkLang = (field, required) => {
    const o = v[field];
    if (o === undefined) {
      if (required) err(`${field} is required`);
      return;
    }
    if (!isObj(o)) return err(`${field} must be an object {en, de}`);
    if (required && typeof o.en !== "string") err(`${field}.en is required`);
    for (const [k, s] of Object.entries(o)) if (typeof s !== "string") err(`${field}.${k} must be a string`);
    if (o.en !== undefined && o.de === undefined) warn(`${field}.de missing (German is required for user-facing strings)`);
  };
  checkLang("name", true);
  checkLang("description", false);
  if (v.author !== undefined && typeof v.author !== "string") err("author must be a string");
  if (v.tags !== undefined && (!Array.isArray(v.tags) || !v.tags.every((t) => typeof t === "string"))) err("tags must be an array of strings");
  if (v.rarity !== undefined && !RARITIES.includes(v.rarity)) err(`rarity must be one of ${RARITIES.join("|")}`);
  if (v.kind === "cosmetic" && v.rarity === undefined) warn("cosmetic has no rarity (defaults to common)");
  for (const k of ["price", "cost", "prices"]) if (k in v) err(`"${k}" is not allowed – no prices anywhere`);

  // ------------------------------------------------------------------ textures
  const textures = new Map();
  if (v.textures !== undefined && !Array.isArray(v.textures)) err("textures must be an array");
  for (const [i, t] of (v.textures ?? []).entries()) {
    const where = `textures[${i}]${t?.id ? ` (${t.id})` : ""}`;
    if (!isObj(t)) { err(`${where} must be an object`); continue; }
    if (typeof t.id !== "string" || !t.id) { err(`${where}.id is required`); continue; }
    if (textures.has(t.id)) err(`${where}: duplicate texture id`);
    if (!Number.isInteger(t.width) || !Number.isInteger(t.height) || t.width < 1 || t.height < 1) err(`${where}: width/height must be positive integers`);
    const buf = b64(t.png);
    let frames = 1;
    if (!buf) err(`${where}.png must be base64`);
    else {
      try {
        const h = readPNGHeader(buf);
        if (t.animation !== undefined) {
          if (!isObj(t.animation)) err(`${where}.animation must be an object`);
          if (h.width !== t.width) err(`${where}: PNG width ${h.width} != width ${t.width}`);
          if (h.height % t.height !== 0) err(`${where}: animated strip height ${h.height} is not a multiple of frame height ${t.height}`);
          frames = Math.floor(h.height / t.height);
          if (h.width > LIMITS.textureMax || h.height > LIMITS.stripMaxHeight) err(`${where}: animated strip ${h.width}x${h.height} exceeds ${LIMITS.textureMax}x${LIMITS.stripMaxHeight}`);
          if (t.height > LIMITS.textureMax) err(`${where}: frame height exceeds ${LIMITS.textureMax}`);
          const a = t.animation ?? {};
          if (a.frameTime !== undefined && !(Number.isInteger(a.frameTime) && a.frameTime >= 1)) err(`${where}.animation.frameTime must be an integer ≥ 1 (ticks)`);
          if (a.frames !== undefined) {
            if (!Array.isArray(a.frames) || !a.frames.length) err(`${where}.animation.frames must be a non-empty array`);
            else for (const f of a.frames) if (!Number.isInteger(f) || f < 0 || f >= frames) err(`${where}.animation.frames: frame ${f} out of range 0..${frames - 1}`);
          }
        } else {
          if (h.width !== t.width || h.height !== t.height) err(`${where}: PNG is ${h.width}x${h.height}, declared ${t.width}x${t.height}`);
          if (h.width > LIMITS.textureMax || h.height > LIMITS.textureMax) err(`${where}: ${h.width}x${h.height} exceeds ${LIMITS.textureMax}x${LIMITS.textureMax}`);
        }
      } catch (e) {
        err(`${where}: ${e.message}`);
      }
    }
    textures.set(t.id, { width: t.width, height: t.height, frames });
  }

  // ------------------------------------------------------------------ sounds / sprites
  const sounds = new Set();
  for (const [i, s] of (v.sounds ?? []).entries()) {
    const where = `sounds[${i}]`;
    if (!isObj(s) || typeof s.id !== "string") { err(`${where}.id is required`); continue; }
    if (sounds.has(s.id)) err(`${where}: duplicate sound id ${s.id}`);
    sounds.add(s.id);
    const buf = b64(s.ogg);
    if (!buf) err(`${where}.ogg must be base64`);
    else {
      if (buf.length > LIMITS.soundBytes) err(`${where}: ${buf.length} bytes (max ${LIMITS.soundBytes})`);
      if (buf.toString("ascii", 0, 4) !== "OggS") err(`${where}: not an OGG stream`);
      else if (!buf.includes(Buffer.from("\x01vorbis", "latin1"))) err(`${where}: OGG stream is not Vorbis`);
    }
  }
  const sprites = new Set();
  for (const [i, s] of (v.sprites ?? []).entries()) {
    const where = `sprites[${i}]`;
    if (!isObj(s) || typeof s.id !== "string") { err(`${where}.id is required`); continue; }
    if (sprites.has(s.id)) err(`${where}: duplicate sprite id ${s.id}`);
    sprites.add(s.id);
    const t = textures.get(s.texture);
    if (!t) err(`${where}: texture "${s.texture}" does not exist`);
    if (!Array.isArray(s.uv) || s.uv.length !== 4 || !s.uv.every(isNum)) err(`${where}.uv must be [u1,v1,u2,v2]`);
    else if (t && (Math.min(s.uv[0], s.uv[2]) < 0 || Math.max(s.uv[0], s.uv[2]) > t.width || Math.min(s.uv[1], s.uv[3]) < 0 || Math.max(s.uv[1], s.uv[3]) > t.height))
      err(`${where}.uv outside texture ${s.texture} (${t.width}x${t.height})`);
  }

  // ------------------------------------------------------------------ cosmetic / emote sections
  const c = v.cosmetic;
  if (v.kind === "cosmetic") {
    if (!isObj(c)) err("cosmetic section is required for kind=cosmetic");
    else {
      if (!SLOTS.includes(c.slot)) err(`cosmetic.slot must be one of ${SLOTS.join("|")}`);
      if (c.hideWithArmor !== undefined) {
        if (!Array.isArray(c.hideWithArmor)) err("cosmetic.hideWithArmor must be an array");
        else for (const a of c.hideWithArmor) if (!ARMOR.includes(a)) err(`cosmetic.hideWithArmor: "${a}" is not one of ${ARMOR.join("|")}`);
      }
      if (c.slot === "cape") {
        const tid = c.cape?.texture;
        const t = textures.get(tid);
        if (!t) err(`cosmetic.cape.texture "${tid}" does not exist`);
        else if (t.width !== 2 * t.height) warn(`cape texture ${tid} is ${t.width}x${t.height} (expected the 64x32 cape layout or a 2:1 multiple)`);
      }
      if (c.slot === "shield") {
        const tid = c.shield?.texture;
        const t = textures.get(tid);
        if (!t) err(`cosmetic.shield.texture "${tid}" does not exist`);
        else if (t.width !== t.height) warn(`shield texture ${tid} is ${t.width}x${t.height} (expected the 64x64 shield layout)`);
      }
      if (c.pet !== undefined) {
        if (!isObj(c.pet)) err("cosmetic.pet must be an object");
        else if (c.pet.scale !== undefined && !(isNum(c.pet.scale) && c.pet.scale > 0)) err("cosmetic.pet.scale must be a positive number");
      }
      if (!["cape", "shield"].includes(c.slot) && !isObj(v.model)) err(`model is required for slot ${c.slot}`);
    }
    if (v.emote !== undefined) warn("emote section ignored for kind=cosmetic");
  }
  const e = v.emote;
  if (v.kind === "emote") {
    if (!isObj(e)) err("emote section is required for kind=emote");
    else {
      if (typeof e.animation !== "string") err("emote.animation is required");
      else if (!v.animations?.[e.animation]) err(`emote.animation "${e.animation}" does not exist in animations`);
      for (const k of ["loop", "thirdPerson", "stopOnMove", "hideHeldItems"]) if (e[k] !== undefined && typeof e[k] !== "boolean") err(`emote.${k} must be a boolean`);
    }
    if (c !== undefined) warn("cosmetic section ignored for kind=emote");
  }

  // ------------------------------------------------------------------ model
  const bones = new Map();
  let cubeCount = 0;
  const m = v.model;
  if (m !== undefined) {
    if (!isObj(m)) err("model must be an object");
    else {
      if (m.texture !== undefined && !textures.has(m.texture)) err(`model.texture "${m.texture}" does not exist`);
      if (!Array.isArray(m.bones)) err("model.bones must be an array");
      const list = Array.isArray(m.bones) ? m.bones : [];
      if (list.length > LIMITS.bones) err(`${list.length} bones (max ${LIMITS.bones})`);
      for (const [i, b] of list.entries()) {
        if (!isObj(b) || typeof b.name !== "string" || !b.name) { err(`model.bones[${i}].name is required`); continue; }
        if (bones.has(b.name)) err(`duplicate bone "${b.name}"`);
        if (v.kind === "emote" && PLAYER_BONES.includes(b.name)) err(`bone "${b.name}" clashes with a player bone name`);
        bones.set(b.name, b);
      }
      for (const b of bones.values()) {
        const where = `bone "${b.name}"`;
        const parent = b.parent ?? null;
        if (parent !== null && !bones.has(parent)) err(`${where}: parent "${parent}" does not exist`);
        if (parent === null) {
          if (b.attach === undefined) warn(`${where}: root bone without attach (assuming "${v.kind === "emote" ? "root" : "body"}")`);
          else if (!ATTACH.includes(b.attach)) err(`${where}: attach "${b.attach}" is not one of ${ATTACH.join("|")}`);
        } else if (b.attach !== undefined && b.attach !== null) warn(`${where}: attach is only used on root bones`);
        if (b.pivot !== undefined && !isVec3(b.pivot)) err(`${where}: pivot must be [x,y,z]`);
        if (b.rotation !== undefined && !isVec3(b.rotation)) err(`${where}: rotation must be [x,y,z]`);
        for (const k of ["emissive", "translucent"]) if (b[k] !== undefined && typeof b[k] !== "boolean") err(`${where}: ${k} must be a boolean`);
        // cycle check
        const seen = new Set([b.name]);
        for (let p = parent; p !== null && bones.has(p); p = bones.get(p).parent ?? null) {
          if (seen.has(p)) { err(`${where}: parent cycle`); break; }
          seen.add(p);
        }
        if (b.cubes !== undefined && !Array.isArray(b.cubes)) { err(`${where}: cubes must be an array`); continue; }
        for (const [j, cube] of (b.cubes ?? []).entries()) {
          cubeCount++;
          const cw = `${where} cube ${j}`;
          if (!isObj(cube)) { err(`${cw} must be an object`); continue; }
          if (!isVec3(cube.from) || !isVec3(cube.to)) err(`${cw}: from/to must be [x,y,z]`);
          if (cube.inflate !== undefined && !isNum(cube.inflate)) err(`${cw}: inflate must be a number`);
          if (cube.mirror !== undefined && typeof cube.mirror !== "boolean") err(`${cw}: mirror must be a boolean`);
          if (cube.pivot !== undefined && !isVec3(cube.pivot)) err(`${cw}: pivot must be [x,y,z]`);
          if (cube.rotation !== undefined && !isVec3(cube.rotation)) err(`${cw}: rotation must be [x,y,z]`);
          const uv = cube.uv;
          if (!isObj(uv)) { err(`${cw}: uv is required ({box:[u,v]} or per-face)`); continue; }
          const texId = cube.texture ?? m.texture;
          if (uv.box !== undefined) {
            if (!Array.isArray(uv.box) || uv.box.length !== 2 || !uv.box.every(isNum)) err(`${cw}: uv.box must be [u,v]`);
            if (!textures.has(texId)) err(`${cw}: no texture (model.texture missing)`);
            else if (isVec3(cube.from) && isVec3(cube.to) && Array.isArray(uv.box)) {
              const t = textures.get(texId);
              const [w, h, d] = [0, 1, 2].map((k) => Math.abs(cube.to[k] - cube.from[k]));
              if (uv.box[0] < 0 || uv.box[1] < 0 || uv.box[0] + 2 * (w + d) > t.width + 1e-6 || uv.box[1] + d + h > t.height + 1e-6)
                warn(`${cw}: box UV region exceeds texture ${texId} (${t.width}x${t.height})`);
            }
          } else {
            let any = false;
            for (const f of FACES) {
              const fv = uv[f];
              if (fv === undefined) continue;
              any = true;
              const rect = Array.isArray(fv) ? fv : fv?.uv;
              const ftex = (isObj(fv) && fv.texture) || texId;
              if (!Array.isArray(rect) || rect.length !== 4 || !rect.every(isNum)) err(`${cw}: uv.${f} must be [u1,v1,u2,v2]`);
              if (!textures.has(ftex)) err(`${cw}: uv.${f} texture "${ftex}" does not exist`);
            }
            if (!any) warn(`${cw}: no faces`);
          }
        }
      }
      if (cubeCount > LIMITS.cubes) err(`${cubeCount} cubes (max ${LIMITS.cubes})`);
      if (m.locators !== undefined) {
        if (!isObj(m.locators)) err("model.locators must be an object");
        else
          for (const [name, l] of Object.entries(m.locators)) {
            if (!isObj(l)) { err(`locator "${name}" must be an object`); continue; }
            const ok = bones.has(l.bone) || (v.kind === "emote" && PLAYER_BONES.includes(l.bone));
            if (!ok) err(`locator "${name}": bone "${l.bone}" does not exist`);
            if (!isVec3(l.position)) err(`locator "${name}": position must be [x,y,z]`);
          }
      }
    }
  }
  const locators = isObj(m?.locators) ? m.locators : {};

  // ------------------------------------------------------------------ animations
  const anims = v.animations ?? {};
  if (!isObj(anims)) err("animations must be an object");
  else
    for (const [name, a] of Object.entries(anims)) {
      const where = `animation "${name}"`;
      if (!isObj(a)) { err(`${where} must be an object`); continue; }
      if (!isNum(a.length) || a.length <= 0) err(`${where}: length must be > 0 (seconds)`);
      if (a.loop !== undefined && typeof a.loop !== "boolean") err(`${where}: loop must be a boolean`);
      for (const [bone, chans] of Object.entries(a.bones ?? {})) {
        const ok = bones.has(bone) || (v.kind === "emote" && PLAYER_BONES.includes(bone));
        if (!ok) err(`${where}: bone "${bone}" does not exist`);
        if (!isObj(chans)) { err(`${where}.${bone} must be an object`); continue; }
        for (const [ch, keys] of Object.entries(chans)) {
          if (!["rotation", "position", "scale"].includes(ch)) { warn(`${where}.${bone}: unknown channel "${ch}"`); continue; }
          if (!Array.isArray(keys) || !keys.length) { err(`${where}.${bone}.${ch} must be a non-empty array`); continue; }
          let last = -Infinity;
          for (const k of keys) {
            if (!isObj(k) || !isNum(k.t) || !isVec3(k.v)) { err(`${where}.${bone}.${ch}: keyframes need {t, v:[x,y,z]}`); continue; }
            if (k.t < 0 || k.t > a.length + 1e-6) err(`${where}.${bone}.${ch}: keyframe t=${k.t} outside 0..${a.length}`);
            if (k.t < last) err(`${where}.${bone}.${ch}: keyframes must be sorted by t`);
            last = k.t;
            if (k.i !== undefined && !INTERP.includes(k.i)) err(`${where}.${bone}.${ch}: interpolation "${k.i}" unknown`);
            if (k.i === "bezier") warn(`${where}.${bone}.${ch}: bezier interpolation is optional for clients`);
          }
        }
      }
      for (const [j, ev] of (a.events ?? []).entries()) {
        const ew = `${where} event ${j}`;
        if (!isObj(ev)) { err(`${ew} must be an object`); continue; }
        if (!isNum(ev.t) || ev.t < 0 || ev.t > a.length + 1e-6) err(`${ew}: t must be within 0..${a.length}`);
        if (ev.type === "particle") {
          if ((ev.particle === undefined) === (ev.sprite === undefined)) err(`${ew}: needs exactly one of "particle" (vanilla id) or "sprite"`);
          if (ev.particle !== undefined && !(typeof ev.particle === "string" && SOUND_ID_RE.test(ev.particle))) err(`${ew}: particle "${ev.particle}" is not a valid id`);
          if (ev.sprite !== undefined && !sprites.has(ev.sprite)) err(`${ew}: sprite "${ev.sprite}" does not exist`);
          if (ev.locator !== undefined && !(ev.locator in locators)) err(`${ew}: locator "${ev.locator}" does not exist`);
          for (const k of ["spread", "velocity"]) if (ev[k] !== undefined && !isVec3(ev[k])) err(`${ew}: ${k} must be [x,y,z]`);
          for (const k of ["count", "lifetime", "size", "gravity"]) if (ev[k] !== undefined && !isNum(ev[k])) err(`${ew}: ${k} must be a number`);
        } else if (ev.type === "sound") {
          if (typeof ev.sound !== "string") err(`${ew}: sound is required`);
          else if (!sounds.has(ev.sound)) {
            if (!ev.sound.includes(":")) err(`${ew}: embedded sound "${ev.sound}" does not exist`);
            else if (!ev.sound.startsWith("minecraft:")) warn(`${ew}: sound "${ev.sound}" is neither embedded nor a vanilla minecraft: id`);
            else if (!SOUND_ID_RE.test(ev.sound)) err(`${ew}: sound id "${ev.sound}" is invalid`);
          }
          for (const k of ["volume", "pitch"]) if (ev[k] !== undefined && !isNum(ev[k])) err(`${ew}: ${k} must be a number`);
        } else if (ev.type === "visibility") {
          if (!bones.has(ev.bone)) err(`${ew}: bone "${ev.bone}" does not exist`);
          if (typeof ev.visible !== "boolean") err(`${ew}: visible must be a boolean`);
        } else err(`${ew}: unknown type "${ev.type}"`);
      }
    }

  // slot specific expectations (warnings only)
  if (v.kind === "cosmetic" && isObj(c)) {
    if (c.slot === "wings" && !anims.fly) warn("wings without a fly animation");
    if (["pet", "aura", "wings"].includes(c.slot) && !anims.idle) warn(`${c.slot} without an idle animation`);
    if (c.slot === "pet" && !anims.walk) warn("pet without a walk animation");
    for (const b of bones.values()) if (!b.parent && c.slot && ["pet", "aura"].includes(c.slot) && b.attach !== "world") warn(`${c.slot} root bone "${b.name}" is not attached to world`);
  }
  return { ok: errors.length === 0, errors, warnings };
}

// CLI
if (import.meta.url === pathToFileURL(process.argv[1] ?? "").href) {
  let bad = 0;
  for (const f of process.argv.slice(2)) {
    const r = validate(readFileSync(f));
    console.log(`${r.ok ? "OK  " : "FAIL"} ${f}`);
    for (const e of r.errors) console.log(`   error: ${e}`);
    for (const w of r.warnings) console.log(`   warn:  ${w}`);
    if (!r.ok) bad++;
  }
  process.exit(bad ? 1 : 0);
}
