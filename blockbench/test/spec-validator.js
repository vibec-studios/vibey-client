'use strict';
/**
 * Independent `.vibey` validator written straight from docs/SPEC.md §5.
 * It intentionally does NOT reuse the plugin's validator so tests catch mistakes in either one.
 * Returns { errors: string[], warnings: string[] }.
 */
const ID_RE = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const SLOTS = ['cape', 'wings', 'aura', 'pet', 'shield', 'hat', 'back'];
const ATTACH = ['head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'root', 'world'];
const PLAYER = ['root', 'head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'];
const RARITY = ['common', 'rare', 'epic', 'legendary'];
const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];
const INTERP = ['linear', 'catmullrom', 'step', 'bezier'];

const vec = (v, n = 3) => Array.isArray(v) && v.length === n && v.every((x) => typeof x === 'number' && Number.isFinite(x));

function png(b64) {
	if (typeof b64 !== 'string') return null;
	const buf = Buffer.from(b64, 'base64');
	if (buf.length < 24 || buf.readUInt32BE(0) !== 0x89504e47 || buf.toString('ascii', 12, 16) !== 'IHDR') return null;
	return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}

function validate(b) {
	const errors = [];
	const warnings = [];
	const e = (m) => errors.push(m);
	if (!b || typeof b !== 'object') return { errors: ['not an object'], warnings };
	if (b.format !== 'vibey') e('format');
	if (b.formatVersion !== 1) e('formatVersion');
	if (!['cosmetic', 'emote'].includes(b.kind)) e('kind');
	if (typeof b.id !== 'string' || !ID_RE.test(b.id)) e('id');
	if (!b.name || typeof b.name.en !== 'string' || !b.name.en) e('name.en');
	if (b.rarity !== undefined && !RARITY.includes(b.rarity)) e('rarity');
	if (b.tags !== undefined && !(Array.isArray(b.tags) && b.tags.every((t) => typeof t === 'string'))) e('tags');

	const tex = new Map();
	for (const t of b.textures || []) {
		if (!t.id || tex.has(t.id)) e(`texture id ${t.id}`);
		const p = png(t.png);
		if (!p) e(`texture ${t.id} png`);
		else {
			if (p.w > 512 || p.h > (t.animation ? 4096 : 512)) e(`texture ${t.id} too big`);
			const frames = (p.h * t.width) / (p.w * t.height);
			if (!(frames >= 1) || Math.abs(frames - Math.round(frames)) > 0.01) e(`texture ${t.id} uv size mismatch`);
		}
		if (!(t.width > 0 && t.height > 0)) e(`texture ${t.id} size`);
		if (t.animation && !(t.animation.frameTime >= 1)) e(`texture ${t.id} frameTime`);
		tex.set(t.id, t);
	}
	if (b.kind === 'cosmetic') {
		if (!b.cosmetic || !SLOTS.includes(b.cosmetic.slot)) e('cosmetic.slot');
		const c = b.cosmetic || {};
		if (c.cape && c.cape.texture && !tex.has(c.cape.texture)) e('cape texture');
		if (c.shield && c.shield.texture && !tex.has(c.shield.texture)) e('shield texture');
		if (c.hideWithArmor && !c.hideWithArmor.every((s) => ['head', 'chest', 'legs', 'feet'].includes(s))) e('hideWithArmor');
		if (!b.model && !['cape', 'shield'].includes(c.slot)) e('model required');
	}
	if (b.kind === 'emote') {
		if (!b.emote || !b.animations || !b.animations[b.emote.animation]) e('emote.animation');
	}
	const bones = new Map();
	let cubes = 0;
	if (b.model) {
		if (b.model.texture !== undefined && !tex.has(b.model.texture)) e('model.texture');
		for (const bone of b.model.bones || []) {
			if (!bone.name || bones.has(bone.name)) e(`bone name ${bone.name}`);
			if (b.kind === 'emote' && PLAYER.includes(bone.name)) e(`reserved bone ${bone.name}`);
			bones.set(bone.name, bone);
		}
		for (const bone of b.model.bones || []) {
			if (bone.parent == null) {
				if (!ATTACH.includes(bone.attach)) e(`attach ${bone.name}`);
			} else if (!bones.has(bone.parent)) e(`parent ${bone.parent}`);
			if (!vec(bone.pivot)) e(`pivot ${bone.name}`);
			if (bone.rotation !== undefined && !vec(bone.rotation)) e(`rotation ${bone.name}`);
			for (const c of bone.cubes || []) {
				cubes++;
				if (!vec(c.from) || !vec(c.to)) e(`cube from/to in ${bone.name}`);
				if (c.texture !== undefined && !tex.has(c.texture)) e(`cube texture ${c.texture}`);
				if (!c.uv) e(`cube uv in ${bone.name}`);
				else if (c.uv.box) {
					if (!vec(c.uv.box, 2)) e('uv.box');
				} else {
					for (const f of Object.keys(c.uv)) {
						if (!FACES.includes(f)) continue;
						const face = c.uv[f];
						const arr = Array.isArray(face) ? face : face.uv;
						if (!vec(arr, 4)) e(`uv.${f}`);
						if (!Array.isArray(face) && face.texture !== undefined && !tex.has(face.texture)) e(`face texture ${face.texture}`);
					}
				}
				if (!c.texture && !b.model.texture && !tex.size) e('no texture');
			}
		}
		for (const name of bones.keys()) {
			const seen = new Set();
			for (let cur = name; cur != null; cur = bones.get(cur) && bones.get(cur).parent) {
				if (seen.has(cur)) {
					e(`bone cycle at ${name}`);
					break;
				}
				seen.add(cur);
			}
		}
		for (const [name, l] of Object.entries(b.model.locators || {})) {
			if (!(bones.has(l.bone) || (b.kind === 'emote' && PLAYER.includes(l.bone)))) e(`locator ${name} bone`);
			if (!vec(l.position)) e(`locator ${name} position`);
		}
	}
	if (bones.size > 256) e('bones > 256');
	if (cubes > 2048) e('cubes > 2048');
	const sprites = new Set();
	for (const s of b.sprites || []) {
		if (!tex.has(s.texture)) e(`sprite ${s.id} texture`);
		if (!vec(s.uv, 4)) e(`sprite ${s.id} uv`);
		sprites.add(s.id);
	}
	const sounds = new Set();
	for (const s of b.sounds || []) {
		const buf = Buffer.from(s.ogg || '', 'base64');
		if (buf.toString('ascii', 0, 4) !== 'OggS') e(`sound ${s.id} not ogg`);
		if (buf.length > 512 * 1024) e(`sound ${s.id} too big`);
		sounds.add(s.id);
	}
	const locators = new Set(Object.keys((b.model && b.model.locators) || {}));
	for (const [name, a] of Object.entries(b.animations || {})) {
		if (!(a.length >= 0)) e(`animation ${name} length`);
		for (const [bone, ch] of Object.entries(a.bones || {})) {
			if (!bones.has(bone) && !(b.kind === 'emote' && PLAYER.includes(bone))) e(`animation ${name} bone ${bone}`);
			for (const c of ['rotation', 'position', 'scale']) {
				for (const k of ch[c] || []) {
					if (!(k.t >= 0) || !vec(k.v)) e(`animation ${name} ${bone} ${c} keyframe`);
					if (k.i !== undefined && !INTERP.includes(k.i)) e(`interpolation ${k.i}`);
				}
			}
		}
		for (const ev of a.events || []) {
			if (!(ev.t >= 0)) e('event t');
			if (ev.locator !== undefined && !locators.has(ev.locator)) e(`event locator ${ev.locator}`);
			if (ev.type === 'particle') {
				if (ev.sprite !== undefined ? !sprites.has(ev.sprite) : !ID_RE.test(ev.particle || '')) e('particle ref');
			} else if (ev.type === 'sound') {
				if (!(String(ev.sound).includes(':') || sounds.has(ev.sound))) e(`sound ref ${ev.sound}`);
			} else if (ev.type === 'visibility') {
				if (!bones.has(ev.bone) || typeof ev.visible !== 'boolean') e('visibility');
			} else e(`event type ${ev.type}`);
		}
	}
	if (Buffer.byteLength(JSON.stringify(b)) > 2 * 1024 * 1024) e('bundle > 2 MiB');
	return { errors, warnings };
}

module.exports = { validate };
