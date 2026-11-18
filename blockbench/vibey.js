/**
 * Vibey Client – Blockbench plugin (v1.0.0) by Vibe Craft Studios.
 *
 * Model formats "Vibey Cosmetic" and "Vibey Emote", import/export of `.vibey` bundles (docs/SPEC.md §5),
 * a reference player, validation and the live link into the running game (docs/SPEC.md §8).
 *
 * Install: Blockbench > File > Plugins > Load Plugin from File > vibey.js (file name must stay "vibey.js").
 * The pure conversion helpers are exposed as `module.exports` under Node (tests) and as
 * `window.VibeyCore` / `window.VibeyPlugin` inside Blockbench.
 */
(function () {
'use strict';
const VERSION = '1.0.0';

/* ===========================================================================
 * Vibey core – pure helpers, no Blockbench dependency.
 *
 * This part is shared between the Blockbench plugin and the Node test suite.
 * It converts a plain "project snapshot" into a `.vibey` bundle (SPEC §5) and
 * back, validates bundles, and contains small helpers for the live link (§8).
 * ======================================================================== */
const Core = (function () {
	'use strict';

	const FORMAT_VERSION = 1;
	const PLAYER_PARTS = ['head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'];
	const PLAYER_BONES = ['root'].concat(PLAYER_PARTS);
	/**
	 * Vanilla player part pivots in Blockbench space (SPEC §5) – the same space Blockbench's own player
	 * template and "Modded Entity" format use: the player faces north (-Z), so the RIGHT arm is at +X.
	 * Minecraft model space is x' = -x, y' = 24 - y, z' = z (right_arm [5,22,0] = PartPose.offset(-5, 2, 0)).
	 */
	const PLAYER_PIVOTS = {
		root: [0, 0, 0],
		head: [0, 24, 0],
		body: [0, 24, 0],
		right_arm: [5, 22, 0],
		left_arm: [-5, 22, 0],
		right_leg: [1.9, 12, 0],
		left_leg: [-1.9, 12, 0],
	};
	/** Classic (wide arm) player cubes, 64x64 skin layout box UV offsets. */
	const PLAYER_CUBES = {
		head: { from: [-4, 24, -4], to: [4, 32, 4], uv: [0, 0] },
		body: { from: [-4, 12, -2], to: [4, 24, 2], uv: [16, 16] },
		right_arm: { from: [4, 12, -2], to: [8, 24, 2], uv: [40, 16] },
		left_arm: { from: [-8, 12, -2], to: [-4, 24, 2], uv: [32, 48] },
		right_leg: { from: [-0.1, 0, -2], to: [3.9, 12, 2], uv: [0, 16] },
		left_leg: { from: [-3.9, 0, -2], to: [0.1, 12, 2], uv: [16, 48] },
	};
	/** Blockbench space -> Minecraft model space (positions). Rotations: [-rx, -ry, rz] like the Modded Entity exporter. */
	const toModelSpace = (p) => [p[0] === 0 ? 0 : -p[0], 24 - p[1], p[2]];
	const KINDS = ['cosmetic', 'emote'];
	const SLOTS = ['cape', 'wings', 'aura', 'pet', 'shield', 'hat', 'back'];
	const ATTACH = ['head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'root', 'world'];
	const SLOT_DEFAULT_ATTACH = { cape: 'body', wings: 'body', aura: 'world', pet: 'world', shield: 'left_arm', hat: 'head', back: 'body' };
	const RARITIES = ['common', 'rare', 'epic', 'legendary'];
	const ARMOR_SLOTS = ['head', 'chest', 'legs', 'feet'];
	const INTERPOLATIONS = ['linear', 'catmullrom', 'step', 'bezier'];
	const CHANNELS = ['rotation', 'position', 'scale'];
	const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];
	const ID_RE = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
	const LIMITS = {
		bundleBytes: 2 * 1024 * 1024,
		messageBytes: 8 * 1024 * 1024,
		textureSize: 512,
		stripHeight: 4096,
		bones: 256,
		cubes: 2048,
		soundBytes: 512 * 1024,
	};
	/** Name suffix + marker of bones that hold cubes placed directly in an emote player bone. */
	const GENERATED_SUFFIX = '_cubes';

	// ------------------------------------------------------------------ utils
	function round(n, digits) {
		const f = Math.pow(10, digits === undefined ? 5 : digits);
		const r = Math.round(n * f) / f;
		return Object.is(r, -0) ? 0 : r;
	}
	const roundVec = (v) => v.map((n) => round(n));
	const isNum = (n) => typeof n === 'number' && isFinite(n);
	const isVec = (v, len) => Array.isArray(v) && v.length === (len || 3) && v.every(isNum);
	const allZero = (v) => !v || v.every((n) => n === 0);
	const vecEq = (a, b, eps) => isVec(a) && isVec(b) && a.every((n, i) => Math.abs(n - b[i]) <= (eps || 1e-4));
	const clone = (o) => (o === undefined ? undefined : JSON.parse(JSON.stringify(o)));

	/** Parses a keyframe / form value into a number. Returns NaN for expressions. */
	function parseNumber(value, fallback) {
		if (typeof value === 'number') return isFinite(value) ? value : NaN;
		if (value === undefined || value === null) return fallback === undefined ? NaN : fallback;
		const s = String(value).replace(/\s+/g, '');
		if (s === '') return fallback === undefined ? NaN : fallback;
		if (/^[-+]?(\d+\.?\d*|\.\d+)(e[-+]?\d+)?$/i.test(s)) return parseFloat(s);
		return NaN;
	}
	/** Parses "0.2, 0.3 0.1" / "0.2" / [..] into a vec3 (single value is used for all axes). */
	function parseVec3(value) {
		if (Array.isArray(value)) return value.length === 3 && value.every(isNum) ? value.slice() : null;
		if (value === undefined || value === null || String(value).trim() === '') return undefined;
		const parts = String(value).trim().split(/[\s,;]+/).map((p) => parseNumber(p));
		if (parts.some((n) => !isFinite(n))) return null;
		if (parts.length === 1) return [parts[0], parts[0], parts[0]];
		if (parts.length === 3) return parts;
		return null;
	}
	function sanitizeId(name) {
		return String(name || '')
			.toLowerCase()
			.replace(/\.(png|ogg|jpe?g|tga)$/i, '')
			.replace(/[^a-z0-9_.-]+/g, '_')
			.replace(/^_+|_+$/g, '') || 'texture';
	}

	// ----------------------------------------------------------------- base64
	function base64ToBytes(b64) {
		if (typeof b64 !== 'string') return null;
		const clean = b64.replace(/\s+/g, '');
		if (clean.length % 4 !== 0 || !/^[A-Za-z0-9+/]*={0,2}$/.test(clean)) return null;
		if (typeof Buffer !== 'undefined' && Buffer.from) return new Uint8Array(Buffer.from(clean, 'base64'));
		const bin = atob(clean);
		const out = new Uint8Array(bin.length);
		for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
		return out;
	}
	function bytesToBase64(bytes) {
		if (bytes instanceof ArrayBuffer) bytes = new Uint8Array(bytes);
		if (typeof Buffer !== 'undefined' && Buffer.from) return Buffer.from(bytes).toString('base64');
		let bin = '';
		for (let i = 0; i < bytes.length; i += 0x8000) {
			bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
		}
		return btoa(bin);
	}
	function base64Size(b64) {
		const clean = String(b64 || '').replace(/\s+/g, '');
		const pad = clean.endsWith('==') ? 2 : clean.endsWith('=') ? 1 : 0;
		return Math.floor((clean.length * 3) / 4) - pad;
	}
	/** Reads width/height from a PNG's IHDR chunk. */
	function pngInfo(bytes) {
		const sig = [137, 80, 78, 71, 13, 10, 26, 10];
		if (!bytes || bytes.length < 24 || !sig.every((b, i) => bytes[i] === b)) return null;
		if (String.fromCharCode(bytes[12], bytes[13], bytes[14], bytes[15]) !== 'IHDR') return null;
		const u32 = (o) => ((bytes[o] << 24) | (bytes[o + 1] << 16) | (bytes[o + 2] << 8) | bytes[o + 3]) >>> 0;
		return { width: u32(16), height: u32(20) };
	}
	const isOgg = (bytes) => !!bytes && bytes.length >= 4 && bytes[0] === 0x4f && bytes[1] === 0x67 && bytes[2] === 0x67 && bytes[3] === 0x53;
	function utf8Length(str) {
		if (typeof TextEncoder !== 'undefined') return new TextEncoder().encode(str).length;
		return Buffer.byteLength(str, 'utf8');
	}

	// ----------------------------------------------------------------- box uv
	/**
	 * Face UVs of a box-UV cube exactly as Blockbench computes them
	 * (`Cube.preview_controller.updateUV`, unfloored sizes like Minecraft's ModelPart).
	 */
	function boxUvFaces(size, offset, mirror) {
		const [x, y, z] = size;
		const list = [
			{ face: 'east', from: [0, z], size: [z, y] },
			{ face: 'west', from: [z + x, z], size: [z, y] },
			{ face: 'up', from: [z + x, z], size: [-x, -z] },
			{ face: 'down', from: [z + x * 2, 0], size: [-x, z] },
			{ face: 'south', from: [z * 2 + x, z], size: [x, y] },
			{ face: 'north', from: [z, z], size: [x, y] },
		];
		if (mirror) {
			list.forEach((f) => {
				f.from[0] += f.size[0];
				f.size[0] *= -1;
			});
			const e = list[0];
			list[0] = { face: 'east', from: list[1].from, size: list[1].size };
			list[1] = { face: 'west', from: e.from, size: e.size };
		}
		const out = {};
		list.forEach((f) => {
			out[f.face] = [
				f.from[0] + offset[0],
				f.from[1] + offset[1],
				f.from[0] + f.size[0] + offset[0],
				f.from[1] + f.size[1] + offset[1],
			];
		});
		return out;
	}

	// -------------------------------------------------------------- keyframes
	/**
	 * Converts Blockbench keyframes of one channel into SPEC keyframes.
	 * Input keyframe: { time, interpolation, points: [{x,y,z}] (1 or 2 = pre/post), bezier? }
	 * Values stay in Blockbench convention (same as the editor shows them).
	 */
	function keyframesToSpec(keyframes, ctx) {
		const out = [];
		const sorted = keyframes.slice().sort((a, b) => a.time - b.time);
		for (const kf of sorted) {
			const pts = kf.points || [];
			if (!pts.length) continue;
			const read = (p) => ['x', 'y', 'z'].map((a) => parseNumber(p[a], ctx && ctx.channel === 'scale' ? 1 : 0));
			const post = read(pts[pts.length - 1]);
			const bad = post.concat(pts.length > 1 ? read(pts[0]) : []).some((n) => !isFinite(n));
			if (bad) {
				if (ctx && ctx.errors) {
					ctx.errors.push(`Animation "${ctx.animation}", bone "${ctx.bone}", ${ctx.channel} keyframe at ${round(kf.time, 3)}s uses an expression (Molang). Only plain numbers are supported.`);
				}
				continue;
			}
			const k = { t: round(kf.time, 4), v: roundVec(post) };
			const interp = kf.interpolation || 'linear';
			if (interp !== 'linear') k.i = interp;
			if (pts.length > 1) {
				const pre = roundVec(read(pts[0]));
				if (!vecEq(pre, k.v, 0)) k.pre = pre;
			}
			if (interp === 'bezier' && kf.bezier) k.bezier = clone(kf.bezier);
			out.push(k);
		}
		return out;
	}
	/** Inverse of keyframesToSpec. */
	function specToKeyframes(list) {
		return (list || []).map((k) => {
			const point = (v) => ({ x: v[0], y: v[1], z: v[2] });
			const kf = {
				time: k.t,
				interpolation: k.i || 'linear',
				points: k.pre ? [point(k.pre), point(k.v)] : [point(k.v)],
			};
			if (k.bezier) kf.bezier = clone(k.bezier);
			return kf;
		});
	}

	// ---------------------------------------------------------------- compile
	/**
	 * Builds a `.vibey` bundle from a plain project snapshot.
	 *
	 * snapshot = {
	 *   kind, meta: {id, name, description, author, tags, rarity, slot, hideWithArmor, pet, capeTexture, shieldTexture, emote, sounds, sprites},
	 *   textures: [{id, uvWidth, uvHeight, png, frameTime, frames, frameCount, interpolate, isDefault}],
	 *   nodes: [{type:'group', name, origin, rotation, attach, emissive, translucent, children}
	 *         | {type:'cube', name, from, to, inflate, mirror, origin, rotation, boxUv, uvOffset, texture, faces:{dir:{uv, texture}}}
	 *         | {type:'locator', name, position}],
	 *   animations: [{name, length, loop:'once'|'loop'|'hold', bones:{name:{rotation:[kf],position,scale}}, effects:{particle,sound,timeline}}]
	 * }
	 * Returns { bundle, errors, warnings } (errors here are conversion problems; run validateBundle as well).
	 */
	function compileBundle(snap) {
		const errors = [];
		const warnings = [];
		const meta = snap.meta || {};
		const kind = snap.kind;
		const isEmote = kind === 'emote';

		const bundle = { format: 'vibey', formatVersion: FORMAT_VERSION, kind, id: (meta.id || '').trim() };
		bundle.name = langObject(meta.name, true);
		const desc = langObject(meta.description, false);
		if (desc) bundle.description = desc;
		bundle.author = (meta.author || '').trim();
		bundle.tags = (meta.tags || []).map((t) => String(t).trim()).filter(Boolean);
		bundle.rarity = meta.rarity || 'common';

		// ---- textures
		const textures = snap.textures || [];
		const texIds = new Set();
		bundle.textures = textures.map((t) => {
			if (texIds.has(t.id)) errors.push(`Two textures are both called "${t.id}". Rename one of them (texture ids must be unique).`);
			texIds.add(t.id);
			const out = { id: t.id, width: round(t.uvWidth), height: round(t.uvHeight), png: t.png };
			if ((t.frameCount || 1) > 1) {
				const frames = (t.frames && t.frames.length ? t.frames : range(t.frameCount)).slice();
				const expanded = [];
				frames.forEach((f) => {
					if (typeof f === 'object' && f) {
						const times = Math.max(1, Math.round((f.time || t.frameTime || 1) / (t.frameTime || 1)));
						for (let i = 0; i < times; i++) expanded.push(f.index);
						warnings.push(`Texture "${t.id}": per-frame times are approximated by repeating frames.`);
					} else expanded.push(f);
				});
				out.animation = { frameTime: Math.max(1, Math.round(t.frameTime || 1)), frames: expanded };
				if (t.interpolate) out.animation.interpolate = true;
			}
			return out;
		});
		const defaultTex = (textures.find((t) => t.isDefault) || textures[0] || {}).id;

		// ---- cosmetic / emote blocks
		if (kind === 'cosmetic') {
			const c = { slot: meta.slot };
			if (meta.hideWithArmor && meta.hideWithArmor.length) c.hideWithArmor = meta.hideWithArmor.slice();
			if (meta.slot === 'pet') {
				const pet = {};
				if (meta.pet && meta.pet.follow) pet.follow = meta.pet.follow;
				if (meta.pet && isNum(Number(meta.pet.scale)) && Number(meta.pet.scale) !== 1) pet.scale = Number(meta.pet.scale);
				if (Object.keys(pet).length) c.pet = pet;
			}
			if (meta.slot === 'cape' && meta.capeTexture) c.cape = { texture: meta.capeTexture };
			if (meta.slot === 'shield' && meta.shieldTexture) c.shield = { texture: meta.shieldTexture };
			bundle.cosmetic = c;
		} else if (isEmote) {
			const e = meta.emote || {};
			const anims = snap.animations || [];
			const animName = e.animation && anims.find((a) => a.name === e.animation) ? e.animation : (anims[0] && anims[0].name) || e.animation || '';
			const main = anims.find((a) => a.name === animName);
			bundle.emote = {
				animation: animName,
				loop: main ? main.loop === 'loop' : !!e.loop,
				thirdPerson: e.thirdPerson !== false,
				stopOnMove: e.stopOnMove !== false,
				hideHeldItems: e.hideHeldItems !== false,
			};
		}

		// ---- model
		const bones = [];
		const locators = {};
		const boneNames = new Set();
		const playerBones = new Set();
		const texOf = (id) => (id && id !== defaultTex ? id : undefined);
		let cubeCount = 0;

		function compileCube(c) {
			cubeCount++;
			const cube = { from: roundVec(c.from), to: roundVec(c.to) };
			if (c.name && c.name !== 'cube') cube.name = c.name; // optional, editor-only (keeps names on round trips)
			if (c.inflate) cube.inflate = round(c.inflate);
			if (c.mirror) cube.mirror = true;
			if (!allZero(c.rotation) || !allZero(c.origin)) {
				cube.pivot = roundVec(c.origin || [0, 0, 0]);
				cube.rotation = roundVec(c.rotation || [0, 0, 0]);
			}
			const faces = c.faces || {};
			const allFaces = FACES.every((f) => faces[f] && faces[f].texture !== null);
			if (c.boxUv && allFaces) {
				// Box UV: the game derives all six face UVs from the offset + (unfloored) cube size.
				cube.uv = { box: roundVec(c.uvOffset || [0, 0]) };
				if (c.mirror) cube.mirror = true;
				const t = texOf(c.texture || firstFaceTexture(faces));
				if (t) cube.texture = t;
			} else {
				// Per-face UV (also used for box-UV cubes with hidden faces, using Blockbench's computed face UVs).
				// A face is either [u1,v1,u2,v2] or {"uv": [u1,v1,u2,v2], "texture": "<id>"}; omitted faces are not drawn.
				cube.uv = {};
				FACES.forEach((f) => {
					const face = faces[f];
					if (!face || face.texture === null) return;
					const uv = roundVec(face.uv);
					const t = texOf(face.texture || c.texture);
					cube.uv[f] = t ? { uv, texture: t } : uv;
				});
			}
			return cube;
		}

		function addBone(node, parent, attach, isRoot) {
			if (boneNames.has(node.name)) errors.push(`Two groups are both called "${node.name}". Bone names must be unique.`);
			boneNames.add(node.name);
			const bone = { name: node.name, parent: parent || null };
			if (isRoot) bone.attach = attach;
			bone.pivot = roundVec(node.origin || [0, 0, 0]);
			bone.rotation = roundVec(node.rotation || [0, 0, 0]);
			bone.cubes = [];
			bone.emissive = !!node.emissive;
			bone.translucent = !!node.translucent;
			bones.push(bone);
			for (const child of node.children || []) {
				if (child.type === 'cube') bone.cubes.push(compileCube(child));
				else if (child.type === 'group') addBone(child, node.name, null, false);
				else if (child.type === 'locator') addLocator(child, node.name);
			}
			return bone;
		}
		function addLocator(node, boneName) {
			if (locators[node.name]) errors.push(`Two locators are both called "${node.name}". Locator names must be unique.`);
			locators[node.name] = { bone: boneName, position: roundVec(node.position || [0, 0, 0]) };
		}

		const defaultAttach = isEmote ? 'root' : SLOT_DEFAULT_ATTACH[meta.slot] || 'body';
		for (const node of snap.nodes || []) {
			if (node.type === 'group' && isEmote && PLAYER_BONES.includes(node.name)) {
				compilePlayerBone(node, null);
			} else if (node.type === 'group') {
				addBone(node, null, node.attach || defaultAttach, true);
			} else {
				errors.push(`"${node.name}" (${node.type}) is not inside a group. Put every cube and locator into a group – root groups decide where the ${kind} is attached.`);
			}
		}
		function compilePlayerBone(node, parentPart) {
			const part = node.name;
			playerBones.add(part);
			if (part === 'root' ? parentPart !== null : parentPart !== 'root') {
				errors.push(part === 'root' ? 'Player bone "root" must stay at the top of the outliner.' : `Player bone "${part}" must be a direct child of "root".`);
			}
			if (!vecEq(node.origin || [0, 0, 0], PLAYER_PIVOTS[part])) {
				errors.push(`Player bone "${part}" pivot was moved to [${roundVec(node.origin).join(', ')}]; it must stay [${PLAYER_PIVOTS[part].join(', ')}] (Vibey > Repair player rig).`);
			}
			if (!allZero(node.rotation)) errors.push(`Player bone "${part}" has a rest rotation. Rotate it with keyframes in Animate mode instead.`);
			const looseCubes = [];
			for (const child of node.children || []) {
				if (child.type === 'group' && PLAYER_BONES.includes(child.name)) compilePlayerBone(child, part);
				else if (child.type === 'group') addBone(child, null, part, true);
				else if (child.type === 'cube') looseCubes.push(child);
				else if (child.type === 'locator') addLocator(child, part);
			}
			if (looseCubes.length) {
				let name = part + GENERATED_SUFFIX;
				let n = 2;
				while (boneNames.has(name) || nodeNameExists(snap.nodes, name)) name = part + GENERATED_SUFFIX + '_' + n++;
				boneNames.add(name);
				bones.push({
					name, parent: null, attach: part, pivot: PLAYER_PIVOTS[part].slice(), rotation: [0, 0, 0],
					cubes: looseCubes.map(compileCube), emissive: false, translucent: false, generated: true,
				});
			}
		}

		const hasModel = bones.length || Object.keys(locators).length;
		const modelOptional = isEmote || (kind === 'cosmetic' && (meta.slot === 'cape' || meta.slot === 'shield'));
		if (hasModel || !modelOptional) {
			bundle.model = {};
			if (defaultTex) bundle.model.texture = defaultTex;
			bundle.model.bones = bones;
			if (Object.keys(locators).length) bundle.model.locators = locators;
		}

		// ---- animations
		const animations = {};
		const embeddedSounds = new Set((meta.sounds || []).map((s) => s.id));
		for (const anim of snap.animations || []) {
			if (animations[anim.name]) errors.push(`Two animations are both called "${anim.name}".`);
			const a = { length: round(anim.length || 0, 4), loop: anim.loop === 'loop' };
			if (anim.loop === 'hold') a.hold = true;
			a.bones = {};
			for (const boneName of Object.keys(anim.bones || {})) {
				const chans = anim.bones[boneName];
				const out = {};
				for (const ch of CHANNELS) {
					if (!chans[ch] || !chans[ch].length) continue;
					const list = keyframesToSpec(chans[ch], { errors, animation: anim.name, bone: boneName, channel: ch });
					if (list.length) out[ch] = list;
				}
				if (Object.keys(out).length) a.bones[boneName] = out;
			}
			const events = compileEvents(anim, embeddedSounds, errors, warnings);
			if (events.length) a.events = events;
			animations[anim.name] = a;
		}
		if (Object.keys(animations).length) bundle.animations = animations;

		// ---- sprites / sounds
		const sprites = (meta.sprites || []).map((s) => ({ id: s.id, texture: s.texture, uv: roundVec(s.uv || [0, 0, 0, 0]) }));
		if (sprites.length) bundle.sprites = sprites;
		const sounds = (meta.sounds || []).map((s) => ({ id: s.id, ogg: s.ogg }));
		if (sounds.length) bundle.sounds = sounds;

		if (cubeCount === 0 && kind === 'cosmetic' && !modelOptional) warnings.push('The model has no cubes yet.');
		return { bundle, errors, warnings };
	}

	function compileEvents(anim, embeddedSounds, errors, warnings) {
		const fx = anim.effects || {};
		const events = [];
		const where = (kf) => `animation "${anim.name}" at ${round(kf.time, 3)}s`;
		const optNum = (ev, key, raw, label, kf, int) => {
			if (raw === undefined || raw === null || String(raw).trim() === '') return;
			const n = parseNumber(raw);
			if (!isFinite(n)) return errors.push(`${label} "${raw}" in ${where(kf)} is not a number.`);
			ev[key] = int ? Math.round(n) : round(n);
		};
		const optVec = (ev, key, raw, label, kf) => {
			const v = parseVec3(raw);
			if (v === undefined) return;
			if (v === null) return errors.push(`${label} "${raw}" in ${where(kf)} must be one number or three numbers like "0.2 0.2 0.2".`);
			ev[key] = roundVec(v);
		};
		for (const kf of fx.particle || []) {
			for (const p of kf.points || []) {
				const ev = { t: round(kf.time, 4), type: 'particle' };
				const sprite = (p.vibey_sprite || '').trim();
				let particle = (p.effect || '').trim();
				if (sprite) ev.sprite = sprite;
				else if (particle) {
					if (!particle.includes(':')) particle = 'minecraft:' + particle;
					ev.particle = particle;
				} else {
					errors.push(`Particle keyframe in ${where(kf)} has neither a particle id (e.g. minecraft:heart) nor a sprite.`);
					continue;
				}
				if (p.locator) ev.locator = p.locator;
				optNum(ev, 'count', p.vibey_count, 'Count', kf, true);
				optVec(ev, 'spread', p.vibey_spread, 'Spread', kf);
				optVec(ev, 'velocity', p.vibey_velocity, 'Velocity', kf);
				optNum(ev, 'lifetime', p.vibey_lifetime, 'Lifetime', kf);
				optNum(ev, 'size', p.vibey_size, 'Size', kf);
				optNum(ev, 'gravity', p.vibey_gravity, 'Gravity', kf);
				if (p.vibey_emissive) ev.emissive = true;
				events.push(ev);
			}
		}
		for (const kf of fx.sound || []) {
			for (const p of kf.points || []) {
				const sound = (p.effect || '').trim();
				if (!sound) {
					errors.push(`Sound keyframe in ${where(kf)} has no sound. Use a game sound id (minecraft:entity.player.levelup) or an embedded sound id.`);
					continue;
				}
				if (!sound.includes(':') && !embeddedSounds.has(sound)) {
					errors.push(`Sound "${sound}" in ${where(kf)} is neither an embedded sound (Vibey panel > Sounds) nor a namespaced id like minecraft:block.note_block.bell.`);
					continue;
				}
				const ev = { t: round(kf.time, 4), type: 'sound', sound };
				optNum(ev, 'volume', p.vibey_volume, 'Volume', kf);
				optNum(ev, 'pitch', p.vibey_pitch, 'Pitch', kf);
				if (p.locator) ev.locator = p.locator;
				events.push(ev);
			}
		}
		for (const kf of fx.timeline || []) {
			for (const p of kf.points || []) {
				const bone = (p.vibey_bone || '').trim();
				if (!bone) {
					if (p.script && String(p.script).trim()) warnings.push(`Timeline script in ${where(kf)} is ignored (only visibility events are supported).`);
					continue;
				}
				events.push({ t: round(kf.time, 4), type: 'visibility', bone, visible: !!p.vibey_visible });
			}
		}
		events.sort((a, b) => a.t - b.t);
		return events;
	}

	function firstFaceTexture(faces) {
		for (const f of FACES) if (faces[f] && faces[f].texture) return faces[f].texture;
		return undefined;
	}
	function nodeNameExists(nodes, name) {
		for (const n of nodes || []) {
			if (n.name === name) return true;
			if (n.children && nodeNameExists(n.children, name)) return true;
		}
		return false;
	}
	function langObject(obj, keepEmptyEn) {
		const out = {};
		if (obj && typeof obj === 'object') {
			for (const k of Object.keys(obj)) {
				const v = typeof obj[k] === 'string' ? obj[k].trim() : '';
				if (v) out[k] = v;
			}
		}
		if (!Object.keys(out).length) return keepEmptyEn ? { en: '' } : undefined;
		return out;
	}
	function range(n) {
		return Array.from({ length: Math.max(0, n | 0) }, (_, i) => i);
	}

	// -------------------------------------------------------------- decompile
	/** Converts a bundle back into a snapshot (inverse of compileBundle). */
	function decompileBundle(bundle) {
		const kind = bundle.kind;
		const isEmote = kind === 'emote';
		const c = bundle.cosmetic || {};
		const meta = {
			id: bundle.id || '',
			name: Object.assign({ en: '', de: '' }, bundle.name || {}),
			description: Object.assign({ en: '', de: '' }, bundle.description || {}),
			author: bundle.author || '',
			tags: (bundle.tags || []).slice(),
			rarity: bundle.rarity || 'common',
			slot: c.slot || 'hat',
			hideWithArmor: (c.hideWithArmor || []).slice(),
			pet: { follow: (c.pet && c.pet.follow) || '', scale: c.pet && isNum(c.pet.scale) ? c.pet.scale : 1 },
			capeTexture: (c.cape && c.cape.texture) || '',
			shieldTexture: (c.shield && c.shield.texture) || '',
			emote: Object.assign({ animation: '', loop: false, thirdPerson: true, stopOnMove: true, hideHeldItems: true }, bundle.emote || {}),
			sounds: (bundle.sounds || []).map((s) => ({ id: s.id, ogg: s.ogg })),
			sprites: (bundle.sprites || []).map((s) => ({ id: s.id, texture: s.texture, uv: (s.uv || []).slice() })),
		};
		const model = bundle.model || {};
		const defaultTex = model.texture || ((bundle.textures || [])[0] || {}).id;
		const textures = (bundle.textures || []).map((t) => {
			const anim = t.animation;
			let frameCount = 1;
			const bytes = base64ToBytes(t.png);
			const info = bytes && pngInfo(bytes);
			if (info && t.width && t.height) {
				frameCount = Math.max(1, Math.round((info.height * t.width) / (info.width * t.height)));
			}
			const isSeq = anim && anim.frames && anim.frames.every((f, i) => f === i) && anim.frames.length === frameCount;
			return {
				id: t.id, uvWidth: t.width, uvHeight: t.height, png: t.png,
				frameTime: anim ? anim.frameTime || 1 : 1,
				frames: anim && anim.frames && !isSeq ? anim.frames.slice() : [],
				frameCount, interpolate: !!(anim && anim.interpolate), isDefault: t.id === defaultTex,
			};
		});

		const byName = {};
		(model.bones || []).forEach((b) => (byName[b.name] = b));
		const groupNodes = {};
		const roots = [];
		const playerGroups = {};
		if (isEmote) {
			const root = { type: 'group', name: 'root', origin: [0, 0, 0], rotation: [0, 0, 0], children: [], player: true };
			playerGroups.root = root;
			roots.push(root);
			PLAYER_PARTS.forEach((p) => {
				const g = { type: 'group', name: p, origin: PLAYER_PIVOTS[p].slice(), rotation: [0, 0, 0], children: [], player: true };
				playerGroups[p] = g;
				root.children.push(g);
			});
		}
		const cubeToNode = (cube) => {
			const n = {
				type: 'cube', from: cube.from.slice(), to: cube.to.slice(), inflate: cube.inflate || 0, mirror: !!cube.mirror,
				origin: cube.pivot ? cube.pivot.slice() : [0, 0, 0], rotation: cube.rotation ? cube.rotation.slice() : [0, 0, 0], faces: {},
			};
			const uv = cube.uv || {};
			if (uv.box) {
				n.boxUv = true;
				n.uvOffset = uv.box.slice();
				n.texture = cube.texture || defaultTex;
				const size = [n.to[0] - n.from[0], n.to[1] - n.from[1], n.to[2] - n.from[2]];
				const faces = boxUvFaces(size, n.uvOffset, n.mirror);
				FACES.forEach((f) => (n.faces[f] = { uv: faces[f], texture: n.texture }));
			} else {
				n.boxUv = false;
				FACES.forEach((f) => {
					const face = uv[f];
					if (!face) n.faces[f] = { uv: [0, 0, 0, 0], texture: null };
					else if (Array.isArray(face)) n.faces[f] = { uv: face.slice(), texture: cube.texture || defaultTex };
					else n.faces[f] = { uv: face.uv.slice(), texture: face.texture || cube.texture || defaultTex };
				});
			}
			return n;
		};
		(model.bones || []).forEach((b) => {
			if (isEmote && b.generated && !b.parent && playerGroups[b.attach]) {
				(b.cubes || []).forEach((cube) => playerGroups[b.attach].children.push(Object.assign(cubeToNode(cube), { name: cube.name || 'cube' })));
				return;
			}
			const g = {
				type: 'group', name: b.name, origin: (b.pivot || [0, 0, 0]).slice(), rotation: (b.rotation || [0, 0, 0]).slice(),
				emissive: !!b.emissive, translucent: !!b.translucent, children: [],
			};
			if (!b.parent) g.attach = b.attach;
			groupNodes[b.name] = g;
			(b.cubes || []).forEach((cube) => g.children.push(Object.assign(cubeToNode(cube), { name: cube.name || 'cube' })));
		});
		(model.bones || []).forEach((b) => {
			const g = groupNodes[b.name];
			if (!g) return;
			if (b.parent && groupNodes[b.parent]) groupNodes[b.parent].children.push(g);
			else if (isEmote && !b.parent && playerGroups[b.attach]) {
				delete g.attach;
				playerGroups[b.attach].children.push(g);
			} else roots.push(g);
		});
		for (const name of Object.keys(model.locators || {})) {
			const l = model.locators[name];
			const node = { type: 'locator', name, position: (l.position || [0, 0, 0]).slice() };
			const target = groupNodes[l.bone] || playerGroups[l.bone];
			if (target) target.children.push(node);
			else roots.push(node);
		}

		const animations = Object.keys(bundle.animations || {}).map((name) => {
			const a = bundle.animations[name];
			const anim = { name, length: a.length || 0, loop: a.loop ? 'loop' : a.hold ? 'hold' : 'once', bones: {}, effects: { particle: [], sound: [], timeline: [] } };
			for (const bone of Object.keys(a.bones || {})) {
				anim.bones[bone] = {};
				for (const ch of CHANNELS) if (a.bones[bone][ch]) anim.bones[bone][ch] = specToKeyframes(a.bones[bone][ch]);
			}
			const fxByTime = { particle: {}, sound: {}, timeline: {} };
			const push = (ch, t, point) => {
				const key = String(t);
				if (!fxByTime[ch][key]) {
					fxByTime[ch][key] = { time: t, interpolation: 'linear', points: [] };
					anim.effects[ch].push(fxByTime[ch][key]);
				}
				fxByTime[ch][key].points.push(point);
			};
			const str = (v) => (v === undefined ? '' : Array.isArray(v) ? v.join(' ') : String(v));
			for (const ev of a.events || []) {
				if (ev.type === 'particle') {
					push('particle', ev.t, {
						effect: ev.particle || '', locator: ev.locator || '', vibey_sprite: ev.sprite || '',
						vibey_count: str(ev.count), vibey_spread: str(ev.spread), vibey_velocity: str(ev.velocity),
						vibey_lifetime: str(ev.lifetime), vibey_size: str(ev.size), vibey_gravity: str(ev.gravity), vibey_emissive: !!ev.emissive,
					});
				} else if (ev.type === 'sound') {
					push('sound', ev.t, { effect: ev.sound || '', locator: ev.locator || '', vibey_volume: str(ev.volume), vibey_pitch: str(ev.pitch) });
				} else if (ev.type === 'visibility') {
					push('timeline', ev.t, { vibey_bone: ev.bone || '', vibey_visible: !!ev.visible });
				}
			}
			return anim;
		});

		return { kind, meta, textures, nodes: roots, animations };
	}

	// --------------------------------------------------------------- validate
	/**
	 * Validates a bundle against SPEC §5. Returns {errors: string[], warnings: string[]}.
	 * options.skipBinary: do not decode PNG/OGG payloads (fast live validation in the editor).
	 */
	function validateBundle(b, options) {
		const opt = options || {};
		const errors = [];
		const warnings = [];
		const err = (m) => errors.push(m);
		if (!b || typeof b !== 'object' || Array.isArray(b)) return { errors: ['Bundle is not a JSON object.'], warnings };
		if (b.format !== 'vibey') err('"format" must be "vibey".');
		if (b.formatVersion !== FORMAT_VERSION) err(`"formatVersion" must be ${FORMAT_VERSION}.`);
		if (!KINDS.includes(b.kind)) err(`"kind" must be "cosmetic" or "emote" (got ${JSON.stringify(b.kind)}).`);
		if (typeof b.id !== 'string' || !ID_RE.test(b.id)) {
			err(`Id ${JSON.stringify(b.id || '')} is invalid. Use namespace:path with only a-z 0-9 _ . - (e.g. "myname:cool_hat"). Set it in Vibey > Properties.`);
		}
		if (!b.name || typeof b.name !== 'object' || typeof b.name.en !== 'string' || !b.name.en.trim()) err('An English name (name.en) is required. Set it in Vibey > Properties.');
		if (b.name && b.name.de !== undefined && typeof b.name.de !== 'string') err('name.de must be a string.');
		if (b.description !== undefined && (typeof b.description !== 'object' || Array.isArray(b.description))) err('description must be an object like {"en": "...", "de": "..."}.');
		if (b.author !== undefined && typeof b.author !== 'string') err('author must be a string.');
		if (b.tags !== undefined && (!Array.isArray(b.tags) || b.tags.some((t) => typeof t !== 'string'))) err('tags must be a list of strings.');
		if (b.rarity !== undefined && !RARITIES.includes(b.rarity)) err(`rarity must be one of ${RARITIES.join(', ')}.`);

		// textures
		const texIds = new Set();
		const textures = Array.isArray(b.textures) ? b.textures : [];
		if (b.textures !== undefined && !Array.isArray(b.textures)) err('textures must be a list.');
		textures.forEach((t, i) => {
			const label = `Texture "${t && t.id}"`;
			if (!t || typeof t.id !== 'string' || !t.id) return err(`Texture #${i + 1} has no id.`);
			if (texIds.has(t.id)) err(`${label} exists twice.`);
			texIds.add(t.id);
			if (!(t.width > 0) || !(t.height > 0)) err(`${label} needs a positive width and height.`);
			if (t.animation !== undefined) {
				const a = t.animation;
				if (!a || !(a.frameTime >= 1)) err(`${label}: animation.frameTime must be ≥ 1 tick.`);
				if (a && a.frames !== undefined && (!Array.isArray(a.frames) || a.frames.some((f) => !Number.isInteger(f) || f < 0))) err(`${label}: animation.frames must be a list of frame indices.`);
			}
			if (opt.skipBinary) return;
			const bytes = base64ToBytes(t.png);
			const info = bytes && pngInfo(bytes);
			if (!info) return err(`${label} is not a valid base64 PNG.`);
			const maxH = t.animation ? LIMITS.stripHeight : LIMITS.textureSize;
			if (info.width > LIMITS.textureSize || info.height > maxH) {
				err(`${label} is ${info.width}×${info.height} px; the limit is ${LIMITS.textureSize}×${LIMITS.textureSize}${t.animation ? ` (animated strips up to ${LIMITS.textureSize}×${LIMITS.stripHeight})` : ''}.`);
			}
			if (t.width > 0 && t.height > 0) {
				const frames = (info.height * t.width) / (info.width * t.height);
				if (Math.abs(frames - Math.round(frames)) > 0.01 || Math.round(frames) < 1) {
					err(`${label}: PNG ${info.width}×${info.height} does not match the UV size ${t.width}×${t.height} (or a vertical strip of it).`);
				} else if (Math.round(frames) > 1 && !t.animation) {
					warnings.push(`${label} is a strip of ${Math.round(frames)} frames but has no animation – only the first frame is shown.`);
				} else if (t.animation && t.animation.frames) {
					const max = Math.round(frames) - 1;
					if (t.animation.frames.some((f) => f > max)) err(`${label}: animation frame index out of range (0..${max}).`);
				}
			}
		});
		const hasTex = (id) => texIds.has(id);

		// cosmetic / emote blocks
		if (b.kind === 'cosmetic') {
			const c = b.cosmetic;
			if (!c || typeof c !== 'object') err('"cosmetic" block is missing.');
			else {
				if (!SLOTS.includes(c.slot)) err(`Slot ${JSON.stringify(c.slot)} is invalid. Use one of ${SLOTS.join(', ')}.`);
				if (c.hideWithArmor !== undefined && (!Array.isArray(c.hideWithArmor) || c.hideWithArmor.some((s) => !ARMOR_SLOTS.includes(s)))) err(`hideWithArmor may only contain ${ARMOR_SLOTS.join(', ')}.`);
				if (c.cape && c.cape.texture && !hasTex(c.cape.texture)) err(`Cape texture "${c.cape.texture}" does not exist.`);
				if (c.shield && c.shield.texture && !hasTex(c.shield.texture)) err(`Shield texture "${c.shield.texture}" does not exist.`);
				if (c.slot === 'cape' && !b.model && !(c.cape && c.cape.texture)) err('A cape needs either a cape texture (cosmetic.cape.texture) or a model.');
				if (c.slot === 'shield' && !b.model && !(c.shield && c.shield.texture)) err('A shield needs either a shield texture (cosmetic.shield.texture) or a model.');
				if (c.pet && c.pet.scale !== undefined && !(c.pet.scale > 0)) err('pet.scale must be a positive number.');
			}
			if (b.emote !== undefined) warnings.push('"emote" block is ignored for cosmetics.');
		}
		if (b.kind === 'emote') {
			const e = b.emote;
			if (!e || typeof e !== 'object') err('"emote" block is missing.');
			else {
				if (typeof e.animation !== 'string' || !b.animations || !b.animations[e.animation]) err(`Emote animation ${JSON.stringify(e && e.animation)} does not exist. Create an animation and select it in Vibey > Properties.`);
				['loop', 'thirdPerson', 'stopOnMove', 'hideHeldItems'].forEach((k) => {
					if (e[k] !== undefined && typeof e[k] !== 'boolean') err(`emote.${k} must be true or false.`);
				});
			}
		}

		// model
		const boneNames = new Set();
		const isEmote = b.kind === 'emote';
		const modelOptional = isEmote || (b.cosmetic && (b.cosmetic.slot === 'cape' || b.cosmetic.slot === 'shield'));
		const m = b.model;
		let cubeCount = 0;
		if (m === undefined) {
			if (!modelOptional && b.kind === 'cosmetic') err('This cosmetic has no model. Add a group with cubes.');
		} else if (!m || typeof m !== 'object') err('model must be an object.');
		else {
			if (m.texture !== undefined && !hasTex(m.texture)) err(`Model default texture "${m.texture}" does not exist.`);
			const bones = Array.isArray(m.bones) ? m.bones : [];
			if (!Array.isArray(m.bones)) err('model.bones must be a list.');
			if (!bones.length && !modelOptional) err('The model has no bones. Add at least one group.');
			if (bones.length > LIMITS.bones) err(`Too many bones: ${bones.length} (max ${LIMITS.bones}).`);
			bones.forEach((bone) => {
				if (!bone || typeof bone.name !== 'string' || !bone.name) return err('A bone has no name.');
				if (boneNames.has(bone.name)) err(`Bone "${bone.name}" exists twice.`);
				boneNames.add(bone.name);
				if (isEmote && PLAYER_BONES.includes(bone.name)) err(`Bone name "${bone.name}" is reserved for the player rig in emotes.`);
			});
			bones.forEach((bone) => {
				if (!bone || !bone.name) return;
				const label = `Bone "${bone.name}"`;
				if (bone.parent !== null && bone.parent !== undefined) {
					if (!boneNames.has(bone.parent)) err(`${label}: parent "${bone.parent}" does not exist.`);
					if (bone.attach !== undefined) warnings.push(`${label}: "attach" is only used on root bones and is ignored here.`);
				} else if (!ATTACH.includes(bone.attach)) {
					err(`${label}: root bones need "attach" (one of ${ATTACH.join(', ')}).`);
				}
				if (!isVec(bone.pivot)) err(`${label}: pivot must be [x, y, z].`);
				if (bone.rotation !== undefined && !isVec(bone.rotation)) err(`${label}: rotation must be [x, y, z].`);
				if (bone.emissive !== undefined && typeof bone.emissive !== 'boolean') err(`${label}: emissive must be true/false.`);
				if (bone.translucent !== undefined && typeof bone.translucent !== 'boolean') err(`${label}: translucent must be true/false.`);
				(bone.cubes || []).forEach((cube, ci) => {
					cubeCount++;
					const cl = `${label}, cube #${ci + 1}`;
					if (!isVec(cube.from) || !isVec(cube.to)) return err(`${cl}: from/to must be [x, y, z].`);
					if (cube.pivot !== undefined && !isVec(cube.pivot)) err(`${cl}: pivot must be [x, y, z].`);
					if (cube.rotation !== undefined && !isVec(cube.rotation)) err(`${cl}: rotation must be [x, y, z].`);
					if (cube.inflate !== undefined && !isNum(cube.inflate)) err(`${cl}: inflate must be a number.`);
					if (cube.texture !== undefined && !hasTex(cube.texture)) err(`${cl}: texture "${cube.texture}" does not exist.`);
					const uv = cube.uv;
					if (!uv || typeof uv !== 'object') return err(`${cl}: uv is missing.`);
					if (uv.box !== undefined) {
						if (!isVec(uv.box, 2)) err(`${cl}: uv.box must be [u, v].`);
					} else {
						const faces = Object.keys(uv).filter((k) => FACES.includes(k));
						if (!faces.length) warnings.push(`${cl} has no visible faces.`);
						faces.forEach((f) => {
							const face = uv[f];
							const arr = Array.isArray(face) ? face : face && face.uv;
							if (!isVec(arr, 4)) err(`${cl}: uv.${f} must be [u1, v1, u2, v2].`);
							if (face && !Array.isArray(face) && face.texture !== undefined && !hasTex(face.texture)) err(`${cl}: face ${f} uses missing texture "${face.texture}".`);
						});
					}
				});
			});
			// cycles
			const parentOf = {};
			bones.forEach((bone) => bone && bone.name && (parentOf[bone.name] = bone.parent || null));
			Object.keys(parentOf).forEach((name) => {
				const seen = new Set();
				let cur = name;
				while (cur) {
					if (seen.has(cur)) {
						err(`Bone hierarchy has a cycle around "${name}".`);
						break;
					}
					seen.add(cur);
					cur = parentOf[cur];
				}
			});
			if (cubeCount > LIMITS.cubes) err(`Too many cubes: ${cubeCount} (max ${LIMITS.cubes}).`);
			const usesDefault = bones.some((bone) => (bone.cubes || []).some((cube) => !cube.texture && (cube.uv && (cube.uv.box || Object.keys(cube.uv).some((f) => Array.isArray(cube.uv[f]))))));
			if (usesDefault && !m.texture && !texIds.size) err('Cubes need a texture, but the bundle has none. Add a texture.');
			for (const name of Object.keys(m.locators || {})) {
				const l = m.locators[name];
				if (!l || !(boneNames.has(l.bone) || (isEmote && PLAYER_BONES.includes(l.bone)))) err(`Locator "${name}" references missing bone "${l && l.bone}".`);
				if (!l || !isVec(l.position)) err(`Locator "${name}": position must be [x, y, z].`);
			}
		}
		const locatorNames = new Set(Object.keys((m && m.locators) || {}));

		// sprites / sounds
		const spriteIds = new Set();
		(b.sprites || []).forEach((s) => {
			if (!s || !s.id) return err('A sprite has no id.');
			if (spriteIds.has(s.id)) err(`Sprite "${s.id}" exists twice.`);
			spriteIds.add(s.id);
			if (!hasTex(s.texture)) err(`Sprite "${s.id}" uses missing texture "${s.texture}".`);
			if (!isVec(s.uv, 4)) err(`Sprite "${s.id}": uv must be [u1, v1, u2, v2].`);
		});
		const soundIds = new Set();
		(b.sounds || []).forEach((s) => {
			if (!s || !s.id) return err('An embedded sound has no id.');
			if (soundIds.has(s.id)) err(`Sound "${s.id}" exists twice.`);
			soundIds.add(s.id);
			if (opt.skipBinary) return;
			const bytes = base64ToBytes(s.ogg);
			if (!bytes || !isOgg(bytes)) err(`Sound "${s.id}" is not a valid base64 OGG Vorbis file.`);
			else if (bytes.length > LIMITS.soundBytes) err(`Sound "${s.id}" is ${Math.round(bytes.length / 1024)} KiB (max ${LIMITS.soundBytes / 1024} KiB).`);
		});

		// animations
		const animBoneOk = (name) => boneNames.has(name) || (isEmote && PLAYER_BONES.includes(name));
		if (b.animations !== undefined && (typeof b.animations !== 'object' || Array.isArray(b.animations))) err('animations must be an object.');
		for (const an of Object.keys(b.animations || {})) {
			const a = b.animations[an];
			const al = `Animation "${an}"`;
			if (!a || !(a.length >= 0)) err(`${al}: length must be a number ≥ 0 (seconds).`);
			if (a && a.loop !== undefined && typeof a.loop !== 'boolean') err(`${al}: loop must be true/false.`);
			for (const bone of Object.keys((a && a.bones) || {})) {
				if (!animBoneOk(bone)) err(`${al} animates "${bone}", which is not a bone${isEmote ? ' or player bone' : ''} of this model.`);
				for (const ch of Object.keys(a.bones[bone])) {
					if (!CHANNELS.includes(ch)) {
						warnings.push(`${al}, bone "${bone}": unknown channel "${ch}" is ignored.`);
						continue;
					}
					const list = a.bones[bone][ch];
					if (!Array.isArray(list)) {
						err(`${al}, bone "${bone}": ${ch} must be a list of keyframes.`);
						continue;
					}
					let last = -Infinity;
					list.forEach((k) => {
						if (!k || !isNum(k.t) || k.t < 0) return err(`${al}, bone "${bone}", ${ch}: keyframe time must be ≥ 0.`);
						if (a.length >= 0 && k.t > a.length + 1e-3) warnings.push(`${al}, bone "${bone}", ${ch}: keyframe at ${k.t}s is after the end (${a.length}s).`);
						if (k.t < last) err(`${al}, bone "${bone}", ${ch}: keyframes must be sorted by time.`);
						last = k.t;
						if (!isVec(k.v)) err(`${al}, bone "${bone}", ${ch} at ${k.t}s: v must be [x, y, z].`);
						if (k.pre !== undefined && !isVec(k.pre)) err(`${al}, bone "${bone}", ${ch} at ${k.t}s: pre must be [x, y, z].`);
						if (k.i !== undefined && !INTERPOLATIONS.includes(k.i)) err(`${al}, bone "${bone}", ${ch} at ${k.t}s: interpolation "${k.i}" is not supported (linear, catmullrom, step).`);
						if (k.i === 'bezier') warnings.push(`${al}, bone "${bone}", ${ch} at ${k.t}s: bezier keyframes may play back as linear in game.`);
					});
				}
			}
			((a && a.events) || []).forEach((ev) => {
				const el = `${al}, event at ${ev && ev.t}s`;
				if (!ev || !isNum(ev.t) || ev.t < 0) return err(`${al}: event time must be ≥ 0.`);
				if (ev.locator !== undefined && !locatorNames.has(ev.locator)) err(`${el}: locator "${ev.locator}" does not exist.`);
				if (ev.type === 'particle') {
					if (ev.sprite !== undefined) {
						if (!spriteIds.has(ev.sprite)) err(`${el}: sprite "${ev.sprite}" does not exist.`);
					} else if (typeof ev.particle !== 'string' || !ID_RE.test(ev.particle)) err(`${el}: particle must be an id like minecraft:heart.`);
					if (ev.count !== undefined && !(Number.isInteger(ev.count) && ev.count >= 1)) err(`${el}: count must be a whole number ≥ 1.`);
					['spread', 'velocity'].forEach((k) => ev[k] !== undefined && !isVec(ev[k]) && err(`${el}: ${k} must be [x, y, z].`));
					['lifetime', 'size', 'gravity'].forEach((k) => ev[k] !== undefined && !isNum(ev[k]) && err(`${el}: ${k} must be a number.`));
					if (ev.lifetime !== undefined && ev.lifetime <= 0) err(`${el}: lifetime must be > 0.`);
				} else if (ev.type === 'sound') {
					if (typeof ev.sound !== 'string' || !ev.sound) err(`${el}: sound is missing.`);
					else if (!ev.sound.includes(':') && !soundIds.has(ev.sound)) err(`${el}: embedded sound "${ev.sound}" does not exist.`);
					else if (ev.sound.includes(':') && !ID_RE.test(ev.sound)) err(`${el}: sound id "${ev.sound}" is invalid.`);
					['volume', 'pitch'].forEach((k) => ev[k] !== undefined && !(isNum(ev[k]) && ev[k] >= 0) && err(`${el}: ${k} must be a number ≥ 0.`));
				} else if (ev.type === 'visibility') {
					if (!boneNames.has(ev.bone)) err(`${el}: visibility bone "${ev.bone}" does not exist.`);
					if (typeof ev.visible !== 'boolean') err(`${el}: visible must be true/false.`);
				} else {
					err(`${el}: unknown event type ${JSON.stringify(ev.type)}.`);
				}
			});
		}

		// size
		if (!opt.skipBinary) {
			const size = utf8Length(JSON.stringify(b));
			if (size > LIMITS.bundleBytes) err(`The bundle is ${(size / 1048576).toFixed(2)} MiB; the limit is 2 MiB. Use smaller textures/sounds.`);
		}
		return { errors, warnings };
	}

	// -------------------------------------------------------------- stringify
	/** Readable JSON: objects indented, number arrays and small objects (keyframes, events) on one line. */
	function stringify(value, indent) {
		indent = indent || '  ';
		const prim = (x) => x === null || typeof x !== 'object';
		const inlineArr = (v) => Array.isArray(v) && v.every(prim);
		const small = (o) => Object.keys(o).every((k) => o[k] === undefined || prim(o[k]) || inlineArr(o[k])) && JSON.stringify(o).length <= 140;
		const rec = (v, pad) => {
			if (prim(v)) return JSON.stringify(v);
			const p2 = pad + indent;
			if (Array.isArray(v)) {
				if (!v.length) return '[]';
				if (inlineArr(v) && JSON.stringify(v).length <= 140) return '[' + v.map((x) => JSON.stringify(x)).join(', ') + ']';
				return '[\n' + v.map((x) => p2 + rec(x === undefined ? null : x, p2)).join(',\n') + '\n' + pad + ']';
			}
			const keys = Object.keys(v).filter((k) => v[k] !== undefined);
			if (!keys.length) return '{}';
			if (small(v)) return '{ ' + keys.map((k) => JSON.stringify(k) + ': ' + rec(v[k], pad)).join(', ') + ' }';
			return '{\n' + keys.map((k) => p2 + JSON.stringify(k) + ': ' + rec(v[k], p2)).join(',\n') + '\n' + pad + '}';
		};
		return rec(value, '') + '\n';
	}

	// ---------------------------------------------------------------- devlink
	function parseDevlink(text) {
		let data;
		try {
			data = typeof text === 'string' ? JSON.parse(text) : text;
		} catch (e) {
			throw new Error('devlink.json is not valid JSON.');
		}
		if (!data || typeof data !== 'object') throw new Error('devlink.json is empty.');
		const port = Number(data.port);
		if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('devlink.json has no valid "port".');
		if (typeof data.token !== 'string' || !data.token) throw new Error('devlink.json has no "token".');
		return { port, token: data.token, pid: data.pid };
	}
	/** Reconnect delay: 0.5 s, 1 s, 2 s, 4 s, 8 s, then 15 s. */
	function backoffDelay(attempt) {
		return Math.min(15000, 500 * Math.pow(2, Math.max(0, attempt)));
	}
	function devlinkPath(gameDir, sep) {
		const s = sep || (/\\/.test(gameDir) ? '\\' : '/');
		return gameDir.replace(/[\\/]+$/, '') + s + 'vibeyclient' + s + 'devlink.json';
	}
	function defaultGameDir(platform, env, home) {
		if (platform === 'win32') return (env.APPDATA || home + '\\AppData\\Roaming') + '\\.minecraft';
		if (platform === 'darwin') return home + '/Library/Application Support/minecraft';
		return home + '/.minecraft';
	}

	return {
		FORMAT_VERSION, PLAYER_PARTS, PLAYER_BONES, PLAYER_PIVOTS, PLAYER_CUBES, toModelSpace, KINDS, SLOTS, ATTACH, SLOT_DEFAULT_ATTACH,
		RARITIES, ARMOR_SLOTS, INTERPOLATIONS, FACES, ID_RE, LIMITS, GENERATED_SUFFIX,
		round, parseNumber, parseVec3, sanitizeId, base64ToBytes, bytesToBase64, base64Size, pngInfo, isOgg, utf8Length,
		boxUvFaces, keyframesToSpec, specToKeyframes, compileBundle, decompileBundle, validateBundle, stringify,
		parseDevlink, backoffDelay, devlinkPath, defaultGameDir,
	};
})();

if (typeof module === 'object' && module && module.exports && typeof BBPlugin === 'undefined') module.exports = Core;
try {
	(typeof window !== 'undefined' ? window : globalThis).VibeyCore = Core;
} catch (e) {}

/* ===========================================================================
 * Blockbench integration
 * ======================================================================== */
if (typeof BBPlugin === 'undefined' || typeof Blockbench === 'undefined') return;

const COSMETIC = 'vibey_cosmetic';
const EMOTE = 'vibey_emote';
const isVibey = () => typeof Format !== 'undefined' && !!Format && (Format.id === COSMETIC || Format.id === EMOTE);
const isEmoteFormat = () => isVibey() && Format.id === EMOTE;
const kindOf = () => (isEmoteFormat() ? 'emote' : 'cosmetic');

// ------------------------------------------------------------------ i18n
const STRINGS = {
	cosmetic: ['Cosmetic', 'Kosmetik'],
	emote: ['Emote', 'Emote'],
	format_cosmetic: ['Vibey Cosmetic', 'Vibey Kosmetik'],
	format_emote: ['Vibey Emote', 'Vibey Emote'],
	format_cosmetic_desc: ['Cape, wings, aura, pet, shield, hat or back item for Vibey Client.', 'Umhang, Flügel, Aura, Haustier, Schild, Hut oder Rücken-Item für Vibey Client.'],
	format_emote_desc: ['Player emote with props, particles and sounds for Vibey Client.', 'Spieler-Emote mit Props, Partikeln und Sounds für Vibey Client.'],
	format_info: [
		'The grey reference player shows exactly where things end up in game. Root groups choose what they attach to (Element panel > Attach). Export with File > Export > Vibey Bundle or push straight into the running game (Vibey > Push to game).',
		'Der graue Referenzspieler zeigt genau, wo alles im Spiel landet. Wurzelgruppen bestimmen, woran sie hängen (Element-Panel > Befestigung). Export über Datei > Exportieren > Vibey-Bundle oder direkt ins laufende Spiel (Vibey > An Spiel senden).',
	],
	format_emote_info: [
		'Animate the player bones root, head, body, right_arm, left_arm, right_leg and left_leg in Animate mode. Groups inside a player bone are props that move with it. Particles, sounds and visibility are effect keyframes (Timeline > Effects).',
		'Animiere die Spielerknochen root, head, body, right_arm, left_arm, right_leg und left_leg im Animationsmodus. Gruppen in einem Spielerknochen sind Props, die sich mitbewegen. Partikel, Sounds und Sichtbarkeit sind Effekt-Keyframes (Zeitleiste > Effekte).',
	],
	properties: ['Vibey Properties…', 'Vibey-Eigenschaften…'],
	properties_title: ['Vibey properties', 'Vibey-Eigenschaften'],
	validate: ['Validate', 'Prüfen'],
	validate_bundle: ['Validate bundle', 'Bundle prüfen'],
	export_bundle: ['Vibey Bundle (.vibey)', 'Vibey-Bundle (.vibey)'],
	import_bundle: ['Vibey Bundle (.vibey)', 'Vibey-Bundle (.vibey)'],
	toggle_reference: ['Show reference player', 'Referenzspieler anzeigen'],
	repair_rig: ['Repair player rig', 'Spieler-Rig reparieren'],
	connect: ['Connect to game', 'Mit Spiel verbinden'],
	connect_short: ['Connect', 'Verbinden'],
	disconnect: ['Disconnect', 'Trennen'],
	push: ['Push to game', 'An Spiel senden'],
	push_short: ['Push', 'Senden'],
	play: ['Play emote in game', 'Emote im Spiel abspielen'],
	play_short: ['Play', 'Abspielen'],
	auto_push: ['Auto-push on change', 'Bei Änderung automatisch senden'],
	link_settings: ['Live link settings…', 'Live-Link-Einstellungen…'],
	link_settings_title: ['Vibey live link', 'Vibey Live-Link'],
	add_sound: ['Add sound (OGG)…', 'Sound hinzufügen (OGG)…'],
	add_sprite: ['Add particle sprite…', 'Partikel-Sprite hinzufügen…'],
	sounds: ['Sounds', 'Sounds'],
	sprites: ['Sprites', 'Sprites'],
	live_link: ['Live link', 'Live-Link'],
	no_sounds: ['No embedded sounds', 'Keine eingebetteten Sounds'],
	no_sprites: ['No sprites', 'Keine Sprites'],
	f_id: ['ID', 'ID'],
	f_id_desc: ['namespace:path, only a-z 0-9 _ . - ("vibey" is reserved for official items)', 'namespace:pfad, nur a-z 0-9 _ . - ("vibey" ist für offizielle Items reserviert)'],
	f_name_en: ['Name (English)', 'Name (Englisch)'],
	f_name_de: ['Name (German)', 'Name (Deutsch)'],
	f_desc_en: ['Description (English)', 'Beschreibung (Englisch)'],
	f_desc_de: ['Description (German)', 'Beschreibung (Deutsch)'],
	f_author: ['Author', 'Autor'],
	f_tags: ['Tags', 'Tags'],
	f_tags_desc: ['Comma separated', 'Durch Komma getrennt'],
	f_rarity: ['Rarity', 'Seltenheit'],
	f_slot: ['Slot', 'Slot'],
	f_hide_with: ['Hide when wearing', 'Ausblenden bei Rüstung'],
	f_hide_head: ['Helmet', 'Helm'],
	f_hide_chest: ['Chestplate / Elytra', 'Brustpanzer / Elytren'],
	f_hide_legs: ['Leggings', 'Beinschutz'],
	f_hide_feet: ['Boots', 'Stiefel'],
	f_pet_follow: ['Pet follow point', 'Haustier-Folgepunkt'],
	f_pet_scale: ['Pet scale', 'Haustier-Skalierung'],
	f_cape_texture: ['Cape texture (64×32 vanilla layout)', 'Umhang-Textur (64×32 Vanilla-Layout)'],
	f_shield_texture: ['Shield texture (64×64 vanilla layout)', 'Schild-Textur (64×64 Vanilla-Layout)'],
	f_animation: ['Emote animation', 'Emote-Animation'],
	f_loop: ['Loop', 'Wiederholen'],
	f_third_person: ['Switch to third person', 'In dritte Person wechseln'],
	f_stop_on_move: ['Stop when moving', 'Bei Bewegung stoppen'],
	f_hide_held: ['Hide held items', 'Gehaltene Items ausblenden'],
	none: ['(none)', '(keine)'],
	auto: ['Auto', 'Automatisch'],
	attach: ['Attach to', 'Befestigen an'],
	emissive: ['Glowing (emissive)', 'Leuchtend (emissiv)'],
	translucent: ['Translucent', 'Durchscheinend'],
	invalid_id: ['The ID must look like namespace:path (a-z 0-9 _ . -).', 'Die ID muss wie namespace:pfad aussehen (a-z 0-9 _ . -).'],
	name_required: ['Please enter an English name.', 'Bitte einen englischen Namen eingeben.'],
	validation_title: ['Vibey bundle check', 'Vibey-Bundle-Prüfung'],
	validation_ok: ['No problems found – ready to export.', 'Keine Probleme gefunden – bereit zum Export.'],
	errors: ['Errors', 'Fehler'],
	warnings: ['Warnings', 'Warnungen'],
	export_blocked: ['Fix these problems before exporting:', 'Behebe diese Probleme vor dem Export:'],
	push_blocked: ['Fix these problems before pushing:', 'Behebe diese Probleme vor dem Senden:'],
	exported: ['Exported %0', '%0 exportiert'],
	imported: ['Imported %0', '%0 importiert'],
	import_failed: ['Could not import this .vibey file:', 'Diese .vibey-Datei konnte nicht importiert werden:'],
	link_off: ['Not connected', 'Nicht verbunden'],
	link_connecting: ['Connecting to %0…', 'Verbinde mit %0…'],
	link_connected: ['Connected – Minecraft %0 (%1)', 'Verbunden – Minecraft %0 (%1)'],
	link_short_off: ['offline', 'offline'],
	link_short_connecting: ['connecting…', 'verbinde…'],
	link_short_connected: ['connected', 'verbunden'],
	link_short_error: ['not connected', 'nicht verbunden'],
	link_unreachable: ['Game not reachable on 127.0.0.1:%0. Is Minecraft running with Vibey Client and Creator mode on?', 'Spiel auf 127.0.0.1:%0 nicht erreichbar. Läuft Minecraft mit Vibey Client und ist der Creator-Modus an?'],
	link_lost: ['Connection to the game was lost.', 'Verbindung zum Spiel verloren.'],
	link_retry: [' Retrying in %0 s…', ' Neuer Versuch in %0 s…'],
	link_token_rejected: ['The game rejected the token. Copy the current token from Vibey Client settings > Devlink, or enable "Find game automatically".', 'Das Spiel hat den Token abgelehnt. Kopiere den aktuellen Token aus Vibey Client > Einstellungen > Devlink oder aktiviere "Spiel automatisch finden".'],
	link_no_welcome: ['The game did not answer the hello message.', 'Das Spiel hat nicht auf die Begrüßung geantwortet.'],
	devlink_missing: ['Could not find %0. Start Minecraft with Vibey Client and turn on Creator mode (Settings > Devlink), or check the game directory in the live link settings.', '%0 nicht gefunden. Starte Minecraft mit Vibey Client und aktiviere den Creator-Modus (Einstellungen > Devlink) oder prüfe den Spielordner in den Live-Link-Einstellungen.'],
	token_missing: ['No token set. Enable "Find game automatically" or paste the token from the game settings.', 'Kein Token gesetzt. Aktiviere "Spiel automatisch finden" oder füge den Token aus den Spieleinstellungen ein.'],
	push_sent: ['Pushing %0…', 'Sende %0…'],
	push_ok: ['Pushed %0 to the game', '%0 an das Spiel gesendet'],
	push_failed: ['Game rejected %0: %1', 'Spiel hat %0 abgelehnt: %1'],
	push_timeout: ['No answer from the game for %0', 'Keine Antwort vom Spiel für %0'],
	push_invalid: ['Auto-push skipped: %0 problem(s) – see Validate', 'Auto-Senden übersprungen: %0 Problem(e) – siehe Prüfen'],
	push_queued: ['Connecting to the game – the push follows automatically.', 'Verbinde mit dem Spiel – das Senden folgt automatisch.'],
	push_too_big: ['The bundle is too big for the live link (max 8 MiB).', 'Das Bundle ist zu groß für den Live-Link (max. 8 MiB).'],
	ls_auto_find: ['Find game automatically', 'Spiel automatisch finden'],
	ls_auto_find_desc: ['Reads port and token from <game dir>/vibeyclient/devlink.json', 'Liest Port und Token aus <Spielordner>/vibeyclient/devlink.json'],
	ls_game_dir: ['Game directory', 'Spielordner'],
	ls_port: ['Port', 'Port'],
	ls_token: ['Token', 'Token'],
	ls_auto_push: ['Auto-push on change (300 ms debounce)', 'Bei Änderung automatisch senden (300 ms)'],
	ls_replay: ['Replay emote on auto-push', 'Emote beim automatischen Senden abspielen'],
	ls_auto_connect: ['Connect on startup', 'Beim Start verbinden'],
	ls_status: ['Status', 'Status'],
	sound_invalid: ['%0 is not an OGG Vorbis file.', '%0 ist keine OGG-Vorbis-Datei.'],
	sound_too_big: ['%0 is larger than 512 KiB.', '%0 ist größer als 512 KiB.'],
	sprite_title: ['Particle sprite', 'Partikel-Sprite'],
	sprite_id: ['Sprite id', 'Sprite-ID'],
	sprite_texture: ['Texture', 'Textur'],
	sprite_uv: ['UV (u1 v1 u2 v2)', 'UV (u1 v1 u2 v2)'],
	rename: ['Rename', 'Umbenennen'],
	new_id: ['New id', 'Neue ID'],
	id_taken: ['The id "%0" is already used.', 'Die ID "%0" wird schon verwendet.'],
	need_texture: ['Add a texture first.', 'Füge zuerst eine Textur hinzu.'],
	rig_repaired: ['Player rig repaired', 'Spieler-Rig repariert'],
	kp_sprite: ['Sprite (instead of particle)', 'Sprite (statt Partikel)'],
	kp_count: ['Count', 'Anzahl'],
	kp_spread: ['Spread (x y z)', 'Streuung (x y z)'],
	kp_velocity: ['Velocity (x y z)', 'Geschwindigkeit (x y z)'],
	kp_lifetime: ['Lifetime (s)', 'Lebensdauer (s)'],
	kp_size: ['Size', 'Größe'],
	kp_gravity: ['Gravity', 'Schwerkraft'],
	kp_emissive: ['Glowing', 'Leuchtend'],
	kp_volume: ['Volume', 'Lautstärke'],
	kp_pitch: ['Pitch', 'Tonhöhe'],
	kp_bone: ['Visibility: bone', 'Sichtbarkeit: Knochen'],
	kp_visible: ['Visible', 'Sichtbar'],
};
function isGerman() {
	try {
		if (typeof Language !== 'undefined' && Language.code) return String(Language.code).startsWith('de');
		return String(settings.language.value).startsWith('de');
	} catch (e) {
		return false;
	}
}
function T(key, ...args) {
	const entry = STRINGS[key];
	let s = entry ? entry[isGerman() ? 1 : 0] : key;
	args.forEach((a, i) => (s = s.split('%' + i).join(String(a))));
	return s;
}
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);

// ------------------------------------------------------------- settings
const LS_KEY = 'vibey.livelink';
const LinkSettings = Object.assign(
	{ port: 47821, token: '', autoFind: true, gameDir: '', autoPush: false, replayEmote: true, autoConnect: false },
	(() => {
		try {
			return JSON.parse(localStorage.getItem(LS_KEY) || '{}');
		} catch (e) {
			return {};
		}
	})()
);
function saveLinkSettings() {
	try {
		localStorage.setItem(LS_KEY, JSON.stringify(LinkSettings));
	} catch (e) {}
}
const PREFS_KEY = 'vibey.prefs';
function prefs() {
	try {
		return JSON.parse(localStorage.getItem(PREFS_KEY) || '{}');
	} catch (e) {
		return {};
	}
}
function savePrefs(p) {
	try {
		localStorage.setItem(PREFS_KEY, JSON.stringify(Object.assign(prefs(), p)));
	} catch (e) {}
}
const platform = () => (typeof SystemInfo !== 'undefined' && SystemInfo ? SystemInfo.platform : process.platform);
const pathSep = () => (platform() === 'win32' ? '\\' : '/');
function gameDir() {
	if (LinkSettings.gameDir) return LinkSettings.gameDir;
	const si = typeof SystemInfo !== 'undefined' && SystemInfo ? SystemInfo : {};
	return Core.defaultGameDir(platform(), { APPDATA: si.appdata_directory }, si.home_directory || '');
}
function readTextFile(path) {
	let out = null;
	try {
		Blockbench.readFile([path], { readtype: 'text', errorbox: false }, (files) => {
			out = files && files[0] ? files[0].content : null;
		});
	} catch (e) {}
	return out;
}

// ----------------------------------------------------------------- meta
function defaultMeta(kind) {
	const p = prefs();
	const ns = p.namespace || 'custom';
	return {
		id: `${ns}:new_${kind}`,
		name: { en: '', de: '' },
		description: { en: '', de: '' },
		author: p.author || 'Vibe Craft Studios',
		tags: [],
		rarity: 'common',
		slot: 'hat',
		hideWithArmor: [],
		pet: { follow: '', scale: 1 },
		capeTexture: '',
		shieldTexture: '',
		emote: { animation: 'main', loop: false, thirdPerson: true, stopOnMove: true, hideHeldItems: true },
		sounds: [],
		sprites: [],
		showReference: true,
	};
}
function getMeta(project) {
	project = project || (typeof Project !== 'undefined' ? Project : null);
	if (!project) return null;
	const kind = project.format && project.format.id === EMOTE ? 'emote' : 'cosmetic';
	if (!project.vibey || typeof project.vibey !== 'object' || Array.isArray(project.vibey)) project.vibey = {};
	// Normalize in place so references held by callers stay valid.
	const meta = project.vibey;
	const d = defaultMeta(kind);
	for (const k of Object.keys(d)) if (meta[k] === undefined) meta[k] = d[k];
	['name', 'description', 'pet', 'emote'].forEach((k) => {
		if (!meta[k] || typeof meta[k] !== 'object') meta[k] = {};
		for (const sub of Object.keys(d[k])) if (meta[k][sub] === undefined) meta[k][sub] = d[k][sub];
	});
	['tags', 'hideWithArmor', 'sounds', 'sprites'].forEach((k) => !Array.isArray(meta[k]) && (meta[k] = []));
	return meta;
}
function markChanged() {
	if (Project) Project.saved = false;
	refreshUI();
	if (typeof Validator !== 'undefined') Validator.validate();
	scheduleAutoPush();
}

// --------------------------------------------------------------- rig
const findGroup = (name) => Group.all.find((g) => g.name === name);
const isPlayerBone = (g) => isEmoteFormat() && g && Core.PLAYER_BONES.includes(g.name);
function createPlayerRig(withUndo) {
	if (withUndo) Undo.initEdit({ outliner: true, elements: [] });
	let root = findGroup('root');
	if (!root) root = new Group({ name: 'root', origin: [0, 0, 0] }).addTo().init();
	else {
		if (root.parent !== 'root') root.addTo();
		root.origin.V3_set(0, 0, 0);
		root.rotation.V3_set(0, 0, 0);
	}
	root.isOpen = true;
	Core.PLAYER_PARTS.forEach((part) => {
		let g = findGroup(part);
		if (!g) g = new Group({ name: part, origin: Core.PLAYER_PIVOTS[part].slice() }).addTo(root).init();
		else {
			if (g.parent !== root) g.addTo(root);
			g.origin.V3_set(Core.PLAYER_PIVOTS[part]);
			g.rotation.V3_set(0, 0, 0);
		}
	});
	Canvas.updateAllBones && Canvas.updateAllBones();
	Canvas.updateAll();
	if (withUndo) Undo.finishEdit('Repair Vibey player rig');
	Reference.ensure();
}

// ----------------------------------------------------- reference player
const REF_COLORS = {
	skin: [190, 196, 206],
	skinDark: [168, 174, 186],
	shirt: [122, 92, 240],
	shirtDark: [98, 72, 210],
	pants: [52, 52, 66],
	shoes: [24, 24, 31],
	eye: [20, 20, 26],
	eyeShine: [255, 255, 255],
	mouth: [120, 110, 130],
	band: [63, 224, 197],
};
/** Paints an original 64x64 mannequin skin in the standard player box-UV layout. */
function paintReferenceSkin() {
	const canvas = document.createElement('canvas');
	canvas.width = canvas.height = 64;
	const ctx = canvas.getContext('2d');
	const img = ctx.createImageData(64, 64);
	let seed = 1337;
	const rand = () => ((seed = (seed * 16807) % 2147483647) / 2147483647);
	const set = (x, y, c) => {
		if (x < 0 || y < 0 || x >= 64 || y >= 64) return;
		const n = 0.94 + rand() * 0.1;
		const i = (y * 64 + x) * 4;
		img.data[i] = Math.min(255, c[0] * n);
		img.data[i + 1] = Math.min(255, c[1] * n);
		img.data[i + 2] = Math.min(255, c[2] * n);
		img.data[i + 3] = 255;
	};
	const colorFor = (part, face, row, col, w, h) => {
		const C = REF_COLORS;
		if (part === 'head') {
			if (face === 'north') {
				if (row === 3 && (col === 1 || col === 5)) return C.eyeShine;
				if ((row === 3 || row === 4) && (col === 1 || col === 2 || col === 5 || col === 6)) return C.eye;
				if (row === 6 && col >= 3 && col <= 4) return C.mouth;
			}
			return face === 'up' ? C.skin : face === 'down' ? C.skinDark : C.skin;
		}
		if (part === 'body') return face === 'up' || face === 'down' ? C.shirtDark : row >= h - 1 ? C.shirtDark : C.shirt;
		if (part === 'right_arm' || part === 'left_arm') {
			if (face === 'up') return C.shirt;
			if (face === 'down') return C.skin;
			if (row < 4) return C.shirt;
			if (part === 'right_arm' && (row === 8 || row === 9)) return C.band; // teal wrist band marks the RIGHT arm (+X)
			return C.skin;
		}
		// legs
		if (face === 'up') return C.pants;
		if (face === 'down') return C.shoes;
		return row >= h - 2 ? C.shoes : C.pants;
	};
	Core.PLAYER_PARTS.forEach((part) => {
		const c = Core.PLAYER_CUBES[part];
		const size = c.to.map((v, i) => Math.round(v - c.from[i]));
		const faces = Core.boxUvFaces(size, c.uv, false);
		for (const face of Object.keys(faces)) {
			const [u1, v1, u2, v2] = faces[face];
			const x0 = Math.min(u1, u2), y0 = Math.min(v1, v2);
			const w = Math.abs(u2 - u1), h = Math.abs(v2 - v1);
			for (let r = 0; r < h; r++) for (let col = 0; col < w; col++) {
				// columns are counted from the face's left edge as seen from outside (u1 side)
				const cc = u2 >= u1 ? col : w - 1 - col;
				const rr = v2 >= v1 ? r : h - 1 - r;
				set(x0 + col, y0 + r, colorFor(part, face, rr, cc, w, h));
			}
		}
	});
	ctx.putImageData(img, 0, 0);
	return canvas;
}
const Reference = {
	material: null,
	getMaterial() {
		if (this.material) return this.material;
		const tex = new THREE.CanvasTexture(paintReferenceSkin());
		tex.magFilter = THREE.NearestFilter;
		tex.minFilter = THREE.NearestFilter;
		tex.generateMipmaps = false;
		if ('colorSpace' in tex && THREE.SRGBColorSpace) tex.colorSpace = THREE.SRGBColorSpace;
		this.material = new THREE.MeshBasicMaterial({ map: tex, vertexColors: true, side: THREE.DoubleSide, alphaTest: 0.05 });
		return this.material;
	},
	/** Builds one player part mesh, local to the part's pivot, with UVs exactly like a Blockbench box-UV cube. */
	buildPartMesh(part) {
		const c = Core.PLAYER_CUBES[part];
		const pivot = Core.PLAYER_PIVOTS[part];
		const geo = new THREE.BoxGeometry(1, 1, 1);
		geo.setShape(c.from.map((v, i) => v - pivot[i]), c.to.map((v, i) => v - pivot[i]));
		const faces = Core.boxUvFaces(c.to.map((v, i) => v - c.from[i]), c.uv, false);
		const shade = { east: 0.74, west: 0.74, up: 1, down: 0.55, south: 0.86, north: 0.9 };
		const uv = geo.attributes.uv;
		const colors = [];
		['east', 'west', 'up', 'down', 'south', 'north'].forEach((f, i) => {
			const [u1, v1, u2, v2] = faces[f];
			[[u1, v1], [u2, v1], [u1, v2], [u2, v2]].forEach((p, j) => uv.array.set([p[0] / 64, 1 - p[1] / 64], i * 8 + j * 2));
			for (let j = 0; j < 4; j++) colors.push(shade[f], shade[f], shade[f]);
		});
		uv.needsUpdate = true;
		geo.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
		geo.computeBoundingBox();
		geo.computeBoundingSphere();
		const mesh = new THREE.Mesh(geo, this.getMaterial());
		mesh.name = 'vibey_ref_' + part;
		mesh.userData.vibeyReference = true;
		return mesh;
	},
	ensure() {
		if (typeof Project === 'undefined' || !Project || !isVibey() || !Project.model_3d) return;
		const meta = getMeta();
		const visible = meta.showReference !== false;
		if (isEmoteFormat()) {
			// Reference cubes live inside the real player bone groups, so they follow the animation.
			Core.PLAYER_PARTS.forEach((part) => {
				const g = findGroup(part);
				const bone = g && g.mesh;
				if (!bone) return;
				let m = bone.children.find((c) => c.name === 'vibey_ref_' + part);
				if (!m) bone.add((m = this.buildPartMesh(part)));
				m.visible = visible && g.visibility !== false;
			});
		} else {
			let obj = Project.model_3d.children.find((c) => c.name === 'vibey_reference');
			if (!obj) {
				obj = new THREE.Object3D();
				obj.name = 'vibey_reference';
				Core.PLAYER_PARTS.forEach((part) => {
					const pivotObj = new THREE.Object3D();
					pivotObj.position.fromArray(Core.PLAYER_PIVOTS[part]);
					pivotObj.add(this.buildPartMesh(part));
					obj.add(pivotObj);
				});
				Project.model_3d.add(obj);
			}
			obj.visible = visible;
		}
	},
	removeAll() {
		(typeof ModelProject !== 'undefined' ? ModelProject.all : []).forEach((p) => {
			try {
				const o = p.model_3d && p.model_3d.children.find((c) => c.name === 'vibey_reference');
				if (o) p.model_3d.remove(o);
				Object.values(p.nodes_3d || {}).forEach((n) => n.children.filter((c) => c.userData && c.userData.vibeyReference).forEach((c) => n.remove(c)));
			} catch (e) {}
		});
	},
};
function setReferenceVisible(v) {
	const meta = getMeta();
	if (!meta) return;
	meta.showReference = !!v;
	Reference.ensure();
	refreshUI();
}

// ------------------------------------------------------ visibility preview
const hiddenByPreview = new Set();
function applyVisibilityPreview() {
	if (!isVibey() || typeof Animation === 'undefined' || !Animation.selected || !Modes.animate) return clearVisibilityPreview();
	const fx = Animation.selected.animators.effects;
	const state = {};
	const time = Timeline.time;
	if (fx && fx.timeline) {
		fx.timeline.slice().sort((a, b) => a.time - b.time).forEach((kf) => {
			if (kf.time <= time + 1e-6) kf.data_points.forEach((dp) => dp.vibey_bone && (state[dp.vibey_bone] = !!dp.vibey_visible));
		});
	}
	Group.all.forEach((g) => {
		if (!g.mesh) return;
		if (g.name in state) {
			g.mesh.visible = state[g.name] && g.visibility !== false;
			hiddenByPreview.add(g);
		} else if (hiddenByPreview.has(g)) {
			g.mesh.visible = g.visibility !== false;
			hiddenByPreview.delete(g);
		}
	});
}
function clearVisibilityPreview() {
	hiddenByPreview.forEach((g) => g.mesh && (g.mesh.visible = g.visibility !== false));
	hiddenByPreview.clear();
}

// ------------------------------------------------------------ snapshot
function ensureDefaultTexture() {
	if (!isVibey() || !Texture.all.length) return;
	if (!Texture.all.find((t) => t.use_as_default)) Texture.all[0].use_as_default = true;
}
function textureBase64(tex) {
	try {
		if (tex.canvas && tex.width) return tex.canvas.toDataURL('image/png').split(',')[1];
	} catch (e) {}
	const url = tex.getDataURL();
	if (url.startsWith('data:image/png;base64,')) return url.split(',')[1];
	return '';
}
function pickDataPoint(dp) {
	const o = {};
	for (const k in KeyframeDataPoint.properties) o[k] = dp[k];
	return o;
}
function keyframeSnap(kf) {
	const s = { time: kf.time, interpolation: kf.interpolation, points: kf.data_points.map((dp) => ({ x: dp.x, y: dp.y, z: dp.z })) };
	if (kf.interpolation === 'bezier') {
		s.bezier = {
			linked: kf.bezier_linked, left_time: kf.bezier_left_time.slice(), left_value: kf.bezier_left_value.slice(),
			right_time: kf.bezier_right_time.slice(), right_value: kf.bezier_right_value.slice(),
		};
	}
	return s;
}
/** Converts the open Blockbench project into the plain snapshot understood by Core.compileBundle. */
function snapshotProject(opts) {
	opts = opts || {};
	ensureDefaultTexture();
	const meta = getMeta();
	const texIdByUuid = {};
	const def = Texture.all.find((t) => t.use_as_default) || Texture.all[0];
	const textures = Texture.all.map((tex) => {
		const id = Core.sanitizeId(tex.name);
		texIdByUuid[tex.uuid] = id;
		const frameCount = tex.frameCount || 1;
		return {
			id,
			uvWidth: tex.uv_width || Project.texture_width,
			uvHeight: tex.uv_height || Project.texture_height,
			png: opts.skipBinary ? '' : textureBase64(tex),
			pixelWidth: tex.width,
			pixelHeight: tex.height,
			frameCount,
			frameTime: tex.frame_time || 1,
			frames: frameCount > 1 ? tex.getAnimationFrameIndices() || [] : [],
			interpolate: !!tex.frame_interpolate,
			isDefault: tex === def,
		};
	});
	const defId = def ? texIdByUuid[def.uuid] : undefined;
	const faceTex = (face) => (face.texture === null ? null : face.texture ? texIdByUuid[face.texture] || defId : defId);
	const nodeSnap = (node) => {
		if (node.export === false) return null;
		if (node instanceof Group) {
			return {
				type: 'group', name: node.name, origin: node.origin.slice(), rotation: node.rotation.slice(),
				attach: node.vibey_attach || '', emissive: !!node.vibey_emissive, translucent: !!node.vibey_translucent,
				children: node.children.map(nodeSnap).filter(Boolean),
			};
		}
		if (node instanceof Cube) {
			const faces = {};
			Core.FACES.forEach((f) => (faces[f] = { uv: node.faces[f].uv.slice(), texture: faceTex(node.faces[f]) }));
			return {
				type: 'cube', name: node.name, from: node.from.slice(), to: node.to.slice(), inflate: node.inflate || 0,
				mirror: !!node.mirror_uv, origin: node.origin.slice(), rotation: node.rotation.slice(),
				boxUv: !!node.box_uv, uvOffset: node.uv_offset.slice(), faces,
			};
		}
		if (node instanceof Locator) return { type: 'locator', name: node.name, position: node.position.slice() };
		return null;
	};
	const animations = (Animator.animations || []).map((anim) => {
		const a = { name: anim.name, length: anim.length, loop: anim.loop, bones: {}, effects: { particle: [], sound: [], timeline: [] } };
		for (const uuid in anim.animators) {
			const an = anim.animators[uuid];
			if (uuid === 'effects') {
				['particle', 'sound', 'timeline'].forEach((ch) => {
					a.effects[ch] = (an[ch] || []).map((kf) => ({ time: kf.time, points: kf.data_points.map(pickDataPoint) }));
				});
				continue;
			}
			const node = OutlinerNode.uuids[uuid];
			if (!(node instanceof Group) || node.export === false) continue;
			const chans = {};
			['rotation', 'position', 'scale'].forEach((ch) => {
				if (an[ch] && an[ch].length) chans[ch] = an[ch].map(keyframeSnap);
			});
			if (Object.keys(chans).length) a.bones[node.name] = chans;
		}
		return a;
	});
	return {
		kind: kindOf(),
		meta: JSON.parse(JSON.stringify(meta)),
		textures,
		nodes: Outliner.root.map(nodeSnap).filter(Boolean),
		animations,
	};
}
/** Compiles + validates the current project. */
function buildBundle(opts) {
	opts = opts || {};
	const snap = snapshotProject(opts);
	const res = Core.compileBundle(snap);
	const v = Core.validateBundle(res.bundle, { skipBinary: !!opts.skipBinary });
	const errors = res.errors.concat(v.errors);
	const warnings = res.warnings.concat(v.warnings);
	if (opts.skipBinary) {
		snap.textures.forEach((t) => {
			const maxH = t.frameCount > 1 ? Core.LIMITS.stripHeight : Core.LIMITS.textureSize;
			if (t.pixelWidth > Core.LIMITS.textureSize || t.pixelHeight > maxH) {
				errors.push(`Texture "${t.id}" is ${t.pixelWidth}×${t.pixelHeight} px; the limit is 512×512 (animated strips 512×4096).`);
			}
		});
		(snap.meta.sounds || []).forEach((s) => {
			if (Core.base64Size(s.ogg) > Core.LIMITS.soundBytes) errors.push(`Sound "${s.id}" is larger than 512 KiB.`);
		});
	}
	const uniq = (arr) => arr.filter((m, i) => arr.indexOf(m) === i);
	return { bundle: res.bundle, errors: uniq(errors), warnings: uniq(warnings), snapshot: snap };
}
function compileProjectJSON() {
	const r = buildBundle();
	return Object.assign(r, { json: Core.stringify(r.bundle) });
}

// ---------------------------------------------------------------- import
function applySnapshot(snap) {
	const meta = Object.assign(defaultMeta(snap.kind), snap.meta, { showReference: true });
	Project.vibey = meta;
	Project.name = (meta.id.split(':').pop() || 'vibey').replace(/[\/]/g, '_');
	const texUuid = {};
	snap.textures.forEach((t) => {
		const tex = new Texture({
			name: t.id, uv_width: t.uvWidth, uv_height: t.uvHeight, frame_time: t.frameTime || 1,
			frame_order_type: t.frames && t.frames.length ? 'custom' : 'loop', frame_order: (t.frames || []).join(' '),
			frame_interpolate: !!t.interpolate, use_as_default: !!t.isDefault,
		}).fromDataURL('data:image/png;base64,' + t.png).add(false);
		texUuid[t.id] = tex.uuid;
	});
	const def = snap.textures.find((t) => t.isDefault) || snap.textures[0];
	if (def) {
		Project.texture_width = def.uvWidth;
		Project.texture_height = def.uvHeight;
	}
	const build = (node, parent) => {
		if (node.type === 'group') {
			const g = new Group({
				name: node.name, origin: node.origin, rotation: node.rotation,
				vibey_attach: node.attach || '', vibey_emissive: !!node.emissive, vibey_translucent: !!node.translucent,
			}).addTo(parent).init();
			g.isOpen = !!node.player && node.name === 'root';
			(node.children || []).forEach((c) => build(c, g));
		} else if (node.type === 'cube') {
			const faces = {};
			Core.FACES.forEach((f) => {
				const face = node.faces[f];
				faces[f] = { uv: face.uv, texture: face.texture === null ? null : texUuid[face.texture] || false };
			});
			new Cube({
				name: node.name || 'cube', from: node.from, to: node.to, origin: node.origin, rotation: node.rotation,
				inflate: node.inflate || 0, box_uv: !!node.boxUv, uv_offset: node.uvOffset || [0, 0], mirror_uv: !!node.mirror, faces,
			}).addTo(parent).init();
		} else if (node.type === 'locator') {
			new Locator({ name: node.name, position: node.position }).addTo(parent).init();
		}
	};
	snap.nodes.forEach((n) => build(n, undefined));
	snap.animations.forEach((a) => {
		const data = { name: a.name, loop: a.loop, length: a.length, animators: {} };
		const kfData = (ch, kf) => {
			const d = { channel: ch, time: kf.time, interpolation: kf.interpolation || 'linear', data_points: kf.points.map((p) => Object.assign({}, p)) };
			if (kf.bezier) {
				d.bezier_linked = kf.bezier.linked !== false;
				d.bezier_left_time = kf.bezier.left_time;
				d.bezier_left_value = kf.bezier.left_value;
				d.bezier_right_time = kf.bezier.right_time;
				d.bezier_right_value = kf.bezier.right_value;
			}
			return d;
		};
		for (const boneName of Object.keys(a.bones)) {
			const g = findGroup(boneName);
			if (!g) continue;
			const keyframes = [];
			for (const ch of Object.keys(a.bones[boneName])) a.bones[boneName][ch].forEach((kf) => keyframes.push(kfData(ch, kf)));
			data.animators[g.uuid] = { name: boneName, type: 'bone', keyframes };
		}
		const fxKeyframes = [];
		['particle', 'sound', 'timeline'].forEach((ch) => (a.effects[ch] || []).forEach((kf) => fxKeyframes.push(kfData(ch, kf))));
		if (fxKeyframes.length) data.animators.effects = { name: 'Effects', type: 'effect', keyframes: fxKeyframes };
		new Animation(data).add(false);
	});
	Canvas.updateAll();
	Reference.ensure();
	Project.saved = true;
}
function importBundle(content, path) {
	let bundle;
	try {
		bundle = typeof content === 'string' ? JSON.parse(content) : content;
	} catch (e) {
		showProblems(T('import_failed'), { errors: ['The file is not valid JSON: ' + e.message], warnings: [] });
		return false;
	}
	const v = Core.validateBundle(bundle);
	if (!bundle || !Core.KINDS.includes(bundle.kind) || bundle.format !== 'vibey') {
		showProblems(T('import_failed'), { errors: v.errors.length ? v.errors : ['Unknown bundle kind.'], warnings: [] });
		return false;
	}
	const snap = Core.decompileBundle(bundle);
	setupProject(snap.kind === 'emote' ? Formats[EMOTE] : Formats[COSMETIC]);
	applySnapshot(snap);
	if (path) Project.vibey_source_path = path;
	refreshUI();
	if (v.errors.length || v.warnings.length) showProblems(T('import_failed').replace(':', '') + ' – ' + T('warnings'), v);
	else Blockbench.showQuickMessage(T('imported', bundle.id), 1500);
	return true;
}

// ---------------------------------------------------------------- export
function exportBundle() {
	const r = compileProjectJSON();
	if (r.errors.length) return showProblems(T('export_blocked'), r);
	const meta = getMeta();
	Blockbench.export(
		{
			resource_id: 'vibey_bundle',
			type: 'Vibey Bundle',
			extensions: ['vibey'],
			name: (meta.id.split(':').pop() || 'bundle').replace(/[\/]/g, '_'),
			content: r.json,
		},
		(path) => {
			Blockbench.showQuickMessage(T('exported', path ? path.split(/[\\/]/).pop() : meta.id), 2000);
			if (r.warnings.length) showProblems(T('warnings'), { errors: [], warnings: r.warnings });
		}
	);
}
function showProblems(title, r) {
	const list = (arr, cls) => '<ul class="vibey_problems ' + cls + '">' + arr.map((m) => '<li>' + esc(m) + '</li>').join('') + '</ul>';
	let html = '';
	if (!r.errors.length && !r.warnings.length) html = '<p>' + esc(T('validation_ok')) + '</p>';
	if (r.errors.length) html += '<h3 class="vibey_err">' + esc(T('errors')) + ' (' + r.errors.length + ')</h3>' + list(r.errors, 'err');
	if (r.warnings.length) html += '<h3 class="vibey_warn">' + esc(T('warnings')) + ' (' + r.warnings.length + ')</h3>' + list(r.warnings, 'warn');
	new Dialog({ id: 'vibey_problems', title: T('validation_title'), width: 620, singleButton: true, lines: ['<p>' + esc(title) + '</p>', html] }).show();
}
function validateNow() {
	const r = buildBundle();
	showProblems(r.errors.length ? T('export_blocked') : T('validation_ok'), r);
	refreshUI();
	return r;
}

// --------------------------------------------------------- dialogs
function openPropertiesDialog(isNew) {
	const meta = getMeta();
	const kind = kindOf();
	const texOptions = { '': T('none') };
	Texture.all.forEach((t) => (texOptions[Core.sanitizeId(t.name)] = t.name));
	const form = {
		id: { label: T('f_id'), type: 'text', value: meta.id, placeholder: 'myname:cool_hat', description: T('f_id_desc') },
		name_en: { label: T('f_name_en'), type: 'text', value: meta.name.en },
		name_de: { label: T('f_name_de'), type: 'text', value: meta.name.de },
		desc_en: { label: T('f_desc_en'), type: 'text', value: meta.description.en },
		desc_de: { label: T('f_desc_de'), type: 'text', value: meta.description.de },
		author: { label: T('f_author'), type: 'text', value: meta.author },
		tags: { label: T('f_tags'), type: 'text', value: meta.tags.join(', '), description: T('f_tags_desc') },
		rarity: { label: T('f_rarity'), type: 'select', value: meta.rarity, options: { common: 'Common', rare: 'Rare', epic: 'Epic', legendary: 'Legendary' } },
	};
	if (kind === 'cosmetic') {
		Object.assign(form, {
			_sep: '_',
			slot: { label: T('f_slot'), type: 'select', value: meta.slot, options: { cape: 'Cape', wings: 'Wings', aura: 'Aura', pet: 'Pet', shield: 'Shield', hat: 'Hat', back: 'Back' } },
			hide_head: { label: T('f_hide_with') + ': ' + T('f_hide_head'), type: 'checkbox', value: meta.hideWithArmor.includes('head') },
			hide_chest: { label: T('f_hide_with') + ': ' + T('f_hide_chest'), type: 'checkbox', value: meta.hideWithArmor.includes('chest') },
			hide_legs: { label: T('f_hide_with') + ': ' + T('f_hide_legs'), type: 'checkbox', value: meta.hideWithArmor.includes('legs') },
			hide_feet: { label: T('f_hide_with') + ': ' + T('f_hide_feet'), type: 'checkbox', value: meta.hideWithArmor.includes('feet') },
			pet_follow: { label: T('f_pet_follow'), type: 'text', value: meta.pet.follow, placeholder: 'shoulder_right', condition: (f) => f.slot === 'pet' },
			pet_scale: { label: T('f_pet_scale'), type: 'number', value: meta.pet.scale, min: 0.05, max: 10, step: 0.05, condition: (f) => f.slot === 'pet' },
			cape_texture: { label: T('f_cape_texture'), type: 'select', value: meta.capeTexture, options: texOptions, condition: (f) => f.slot === 'cape' },
			shield_texture: { label: T('f_shield_texture'), type: 'select', value: meta.shieldTexture, options: texOptions, condition: (f) => f.slot === 'shield' },
		});
	} else {
		const animOptions = {};
		Animator.animations.forEach((a) => (animOptions[a.name] = a.name));
		if (!Object.keys(animOptions).length) animOptions.main = 'main';
		const main = Animator.animations.find((a) => a.name === meta.emote.animation) || Animator.animations[0];
		Object.assign(form, {
			_sep: '_',
			animation: { label: T('f_animation'), type: 'select', value: main ? main.name : 'main', options: animOptions },
			loop: { label: T('f_loop'), type: 'checkbox', value: main ? main.loop === 'loop' : meta.emote.loop },
			third_person: { label: T('f_third_person'), type: 'checkbox', value: meta.emote.thirdPerson },
			stop_on_move: { label: T('f_stop_on_move'), type: 'checkbox', value: meta.emote.stopOnMove },
			hide_held: { label: T('f_hide_held'), type: 'checkbox', value: meta.emote.hideHeldItems },
		});
	}
	const dialog = new Dialog({
		id: 'vibey_properties',
		title: T('properties_title') + ' – ' + T(kind),
		width: 560,
		form,
		onConfirm(r) {
			const id = String(r.id || '').trim();
			if (!Core.ID_RE.test(id)) {
				Blockbench.showMessageBox({ title: T('properties_title'), message: T('invalid_id'), icon: 'error' });
				return false;
			}
			if (!String(r.name_en || '').trim()) {
				Blockbench.showMessageBox({ title: T('properties_title'), message: T('name_required'), icon: 'error' });
				return false;
			}
			applyProperties(r);
			dialog.hide();
		},
	});
	dialog.show();
	return dialog;
}
/** Applies a properties form result (also used by tests). */
function applyProperties(r) {
	const meta = getMeta();
	const kind = kindOf();
	const prevPath = meta.id.split(':').pop();
	meta.id = String(r.id !== undefined ? r.id : meta.id).trim();
	if (r.name_en !== undefined) meta.name.en = String(r.name_en).trim();
	if (r.name_de !== undefined) meta.name.de = String(r.name_de).trim();
	if (r.desc_en !== undefined) meta.description.en = String(r.desc_en).trim();
	if (r.desc_de !== undefined) meta.description.de = String(r.desc_de).trim();
	if (r.author !== undefined) meta.author = String(r.author).trim();
	if (r.tags !== undefined) meta.tags = String(r.tags).split(',').map((t) => t.trim()).filter(Boolean);
	if (r.rarity !== undefined) meta.rarity = r.rarity;
	if (kind === 'cosmetic') {
		if (r.slot !== undefined) meta.slot = r.slot;
		const armor = [];
		['head', 'chest', 'legs', 'feet'].forEach((s) => {
			const key = 'hide_' + s;
			if (r[key] === undefined ? meta.hideWithArmor.includes(s) : r[key]) armor.push(s);
		});
		meta.hideWithArmor = armor;
		if (r.pet_follow !== undefined) meta.pet.follow = String(r.pet_follow).trim();
		if (r.pet_scale !== undefined) meta.pet.scale = Number(r.pet_scale) || 1;
		if (r.cape_texture !== undefined) meta.capeTexture = r.cape_texture;
		if (r.shield_texture !== undefined) meta.shieldTexture = r.shield_texture;
	} else {
		if (r.animation !== undefined) meta.emote.animation = r.animation;
		if (r.third_person !== undefined) meta.emote.thirdPerson = !!r.third_person;
		if (r.stop_on_move !== undefined) meta.emote.stopOnMove = !!r.stop_on_move;
		if (r.hide_held !== undefined) meta.emote.hideHeldItems = !!r.hide_held;
		if (r.loop !== undefined) {
			meta.emote.loop = !!r.loop;
			const anim = Animator.animations.find((a) => a.name === meta.emote.animation);
			if (anim) anim.loop = r.loop ? 'loop' : anim.loop === 'hold' ? 'hold' : 'once';
		}
	}
	if (!Project.name || Project.name === prevPath || Project.name.startsWith('new_')) Project.name = meta.id.split(':').pop().replace(/[\/]/g, '_');
	if (typeof setProjectTitle === 'function') setProjectTitle();
	savePrefs({ namespace: meta.id.split(':')[0], author: meta.author });
	markChanged();
}

function openLinkSettings() {
	const devPath = () => Core.devlinkPath(gameDir(), pathSep());
	const dialog = new Dialog({
		id: 'vibey_link_settings',
		title: T('link_settings_title'),
		width: 560,
		form: {
			status: { label: T('ls_status'), type: 'info', text: esc(Link.message || T('link_off')) },
			autoFind: { label: T('ls_auto_find'), type: 'checkbox', value: LinkSettings.autoFind, description: T('ls_auto_find_desc') },
			gameDir: { label: T('ls_game_dir'), type: 'folder', value: gameDir(), condition: (f) => f.autoFind },
			devlink: { label: 'devlink.json', type: 'info', text: esc(devPath()), condition: (f) => f.autoFind },
			port: { label: T('ls_port'), type: 'number', value: LinkSettings.port, min: 1, max: 65535, step: 1, condition: (f) => !f.autoFind },
			token: { label: T('ls_token'), type: 'text', value: LinkSettings.token, condition: (f) => !f.autoFind },
			_sep: '_',
			autoPush: { label: T('ls_auto_push'), type: 'checkbox', value: LinkSettings.autoPush },
			replayEmote: { label: T('ls_replay'), type: 'checkbox', value: LinkSettings.replayEmote },
			autoConnect: { label: T('ls_auto_connect'), type: 'checkbox', value: LinkSettings.autoConnect },
		},
		buttons: [T('connect_short'), 'dialog.save', 'dialog.cancel'],
		onButton(index) {
			if (index === 2) return;
			const r = dialog.getFormResult();
			Object.assign(LinkSettings, {
				autoFind: !!r.autoFind,
				gameDir: r.gameDir && r.gameDir !== Core.defaultGameDir(platform(), { APPDATA: (SystemInfo || {}).appdata_directory }, (SystemInfo || {}).home_directory) ? r.gameDir : LinkSettings.gameDir && r.gameDir ? r.gameDir : '',
				port: Number(r.port) || 47821,
				token: String(r.token || '').trim(),
				autoPush: !!r.autoPush,
				replayEmote: !!r.replayEmote,
				autoConnect: !!r.autoConnect,
			});
			saveLinkSettings();
			refreshUI();
			if (index === 0) Link.connect();
		},
	});
	dialog.show();
}

function addSoundFiles(files) {
	const meta = getMeta();
	files.forEach((file) => {
		const bytes = new Uint8Array(file.content);
		const name = file.name || 'sound.ogg';
		if (!Core.isOgg(bytes)) return Blockbench.showMessageBox({ title: T('sounds'), message: T('sound_invalid', name), icon: 'error' });
		if (bytes.length > Core.LIMITS.soundBytes) return Blockbench.showMessageBox({ title: T('sounds'), message: T('sound_too_big', name), icon: 'error' });
		let id = Core.sanitizeId(name);
		let n = 2;
		while (meta.sounds.find((s) => s.id === id)) id = Core.sanitizeId(name) + '_' + n++;
		meta.sounds.push({ id, ogg: Core.bytesToBase64(bytes) });
	});
	markChanged();
}
function addSoundDialog() {
	Blockbench.import({ resource_id: 'vibey_sound', extensions: ['ogg'], type: 'OGG Vorbis', multiple: true, readtype: 'buffer' }, addSoundFiles);
}
function renameItem(listKey, id) {
	const meta = getMeta();
	const item = meta[listKey].find((s) => s.id === id);
	if (!item) return;
	Blockbench.textPrompt(T('new_id'), item.id, (value) => {
		value = Core.sanitizeId(value);
		if (!value || value === item.id) return;
		if (meta[listKey].find((s) => s.id === value)) return Blockbench.showQuickMessage(T('id_taken', value), 2000);
		item.id = value;
		markChanged();
	});
}
function removeItem(listKey, id) {
	const meta = getMeta();
	meta[listKey] = meta[listKey].filter((s) => s.id !== id);
	markChanged();
}
let audio = null;
function playSound(id) {
	const s = getMeta().sounds.find((x) => x.id === id);
	if (!s) return;
	if (audio) audio.pause();
	audio = new Audio('data:audio/ogg;base64,' + s.ogg);
	audio.play().catch(() => {});
}
function spriteDialog(existingId) {
	const meta = getMeta();
	if (!Texture.all.length) return Blockbench.showQuickMessage(T('need_texture'), 2000);
	const existing = meta.sprites.find((s) => s.id === existingId);
	const texOptions = {};
	Texture.all.forEach((t) => (texOptions[Core.sanitizeId(t.name)] = t.name));
	const dialog = new Dialog({
		id: 'vibey_sprite',
		title: T('sprite_title'),
		width: 420,
		form: {
			id: { label: T('sprite_id'), type: 'text', value: existing ? existing.id : 'sparkle' },
			texture: { label: T('sprite_texture'), type: 'select', value: existing ? existing.texture : Object.keys(texOptions)[0], options: texOptions },
			uv: { label: T('sprite_uv'), type: 'vector', dimensions: 4, value: existing ? existing.uv.slice() : [0, 0, 8, 8], step: 1 },
		},
		onConfirm(r) {
			const id = Core.sanitizeId(r.id);
			if (meta.sprites.find((s) => s.id === id && s !== existing)) {
				Blockbench.showQuickMessage(T('id_taken', id), 2000);
				return false;
			}
			const sprite = { id, texture: r.texture, uv: r.uv.map(Number) };
			if (existing) Object.assign(existing, sprite);
			else meta.sprites.push(sprite);
			markChanged();
			dialog.hide();
		},
	});
	dialog.show();
}

// ------------------------------------------------------------- live link
const Link = {
	ws: null,
	state: 'off', // off | connecting | connected | error
	message: '',
	desired: false,
	attempt: 0,
	retryTimer: null,
	pingTimer: null,
	welcomeTimer: null,
	lastPong: 0,
	game: null,
	endpoint: null,
	queued: null,
	pending: [],
	lastResult: '',
	setState(state, message) {
		this.state = state;
		this.message = message || '';
		refreshUI();
	},
	connect() {
		this.desired = true;
		this.attempt = 0;
		this.open();
	},
	disconnect() {
		this.desired = false;
		this.queued = null;
		clearTimeout(this.retryTimer);
		this.closeSocket();
		this.setState('off', T('link_off'));
	},
	closeSocket() {
		clearInterval(this.pingTimer);
		clearTimeout(this.welcomeTimer);
		const ws = this.ws;
		this.ws = null;
		if (ws) {
			ws.onclose = ws.onmessage = ws.onerror = ws.onopen = null;
			try {
				ws.close(1000);
			} catch (e) {}
		}
		this.failPending('Connection closed');
	},
	resolveEndpoint() {
		if (LinkSettings.autoFind) {
			const path = Core.devlinkPath(gameDir(), pathSep());
			const text = readTextFile(path);
			if (text == null) throw new Error(T('devlink_missing', path));
			const d = Core.parseDevlink(text);
			return { port: d.port, token: d.token, source: path };
		}
		if (!LinkSettings.token) throw new Error(T('token_missing'));
		return { port: Number(LinkSettings.port) || 47821, token: LinkSettings.token, source: 'manual' };
	},
	open() {
		clearTimeout(this.retryTimer);
		this.closeSocket();
		let ep;
		try {
			ep = this.resolveEndpoint();
		} catch (e) {
			return this.scheduleRetry(e.message);
		}
		this.endpoint = ep;
		this.setState('connecting', T('link_connecting', '127.0.0.1:' + ep.port));
		let ws;
		try {
			ws = new WebSocket('ws://127.0.0.1:' + ep.port);
		} catch (e) {
			return this.scheduleRetry(e.message);
		}
		this.ws = ws;
		ws.onopen = () => {
			this.send({ type: 'hello', token: ep.token, client: 'blockbench-vibey', version: VERSION });
			this.welcomeTimer = setTimeout(() => {
				if (this.ws === ws && this.state !== 'connected') {
					this.closeSocket();
					this.scheduleRetry(T('link_no_welcome'));
				}
			}, 5000);
		};
		ws.onmessage = (ev) => this.onMessage(ev.data);
		ws.onerror = () => {};
		ws.onclose = (ev) => this.onClose(ev, ws);
	},
	onClose(ev, ws) {
		if (ws !== this.ws) return;
		const wasConnected = this.state === 'connected';
		this.ws = null;
		clearInterval(this.pingTimer);
		clearTimeout(this.welcomeTimer);
		this.failPending('Connection closed');
		if (ev.code === 4001) {
			this.desired = false;
			return this.setState('error', T('link_token_rejected'));
		}
		if (!this.desired) return this.setState('off', T('link_off'));
		this.scheduleRetry(wasConnected ? T('link_lost') : T('link_unreachable', this.endpoint ? this.endpoint.port : LinkSettings.port));
	},
	scheduleRetry(message) {
		if (!this.desired) return this.setState('error', message);
		const delay = Core.backoffDelay(this.attempt++);
		this.setState('error', message + T('link_retry', Math.round(delay / 100) / 10));
		clearTimeout(this.retryTimer);
		this.retryTimer = setTimeout(() => this.desired && this.open(), delay);
	},
	send(obj) {
		if (this.ws && this.ws.readyState === 1) this.ws.send(JSON.stringify(obj));
	},
	onMessage(data) {
		let msg;
		try {
			msg = JSON.parse(data);
		} catch (e) {
			return;
		}
		switch (msg.type) {
			case 'welcome': {
				clearTimeout(this.welcomeTimer);
				this.attempt = 0;
				this.game = msg;
				this.lastPong = Date.now();
				this.setState('connected', T('link_connected', msg.game || '?', msg.player || '?'));
				clearInterval(this.pingTimer);
				this.pingTimer = setInterval(() => {
					if (Date.now() - this.lastPong > 25000 && this.ws) {
						const ws = this.ws;
						this.onClose({ code: 1006 }, ws);
						try {
							ws.close();
						} catch (e) {}
						return;
					}
					this.send({ type: 'ping' });
				}, 10000);
				const q = this.queued;
				this.queued = null;
				if (q) this.push(q.action);
				break;
			}
			case 'pong':
				this.lastPong = Date.now();
				break;
			case 'pushed':
			case 'error': {
				const idx = this.pending.findIndex((p) => p.id === msg.id);
				const p = idx >= 0 ? this.pending.splice(idx, 1)[0] : msg.type === 'pushed' ? this.pending.shift() : null;
				if (p) {
					clearTimeout(p.timer);
					p.resolve(msg);
				} else if (msg.type === 'error') {
					this.lastResult = msg.message || 'error';
					Blockbench.showQuickMessage('Vibey: ' + (msg.message || 'error'), 3000);
					refreshUI();
				}
				break;
			}
			case 'log':
				console[msg.level === 'warn' ? 'warn' : 'log']('[Vibey game] ' + msg.message);
				if (msg.level === 'warn') Blockbench.showQuickMessage('Vibey: ' + msg.message, 2500);
				break;
		}
	},
	failPending(reason) {
		const list = this.pending;
		this.pending = [];
		list.forEach((p) => {
			clearTimeout(p.timer);
			p.resolve({ type: 'error', id: p.id, message: reason });
		});
	},
	/** Pushes the open project. action: equip | play | none. Resolves with the game's answer. */
	push(action, opts) {
		opts = opts || {};
		if (!isVibey()) return Promise.resolve(null);
		const r = buildBundle();
		if (r.errors.length) {
			if (opts.auto) {
				this.lastResult = T('push_invalid', r.errors.length);
				refreshUI();
			} else showProblems(T('push_blocked'), r);
			return Promise.resolve({ type: 'error', message: 'invalid bundle', errors: r.errors });
		}
		const text = JSON.stringify({ type: 'push', bundle: r.bundle, action });
		if (Core.utf8Length(text) > Core.LIMITS.messageBytes) {
			Blockbench.showMessageBox({ title: 'Vibey', message: T('push_too_big'), icon: 'error' });
			return Promise.resolve({ type: 'error', message: 'too big' });
		}
		if (this.state !== 'connected' || !this.ws) {
			if (opts.auto) return Promise.resolve(null);
			this.queued = { action };
			if (!this.desired || this.state === 'error') this.connect();
			Blockbench.showQuickMessage(T('push_queued'), 2000);
			return Promise.resolve({ type: 'queued' });
		}
		const id = r.bundle.id;
		this.ws.send(text);
		this.lastResult = T('push_sent', id);
		refreshUI();
		return new Promise((resolve) => {
			const p = { id, resolve, timer: null };
			p.timer = setTimeout(() => {
				this.pending = this.pending.filter((x) => x !== p);
				resolve({ type: 'error', id, message: 'timeout', timeout: true });
			}, 10000);
			this.pending.push(p);
		}).then((res) => {
			if (res.type === 'pushed' && res.ok !== false) {
				this.lastResult = T('push_ok', id) + (res.warnings && res.warnings.length ? ' (' + res.warnings.length + ' ' + T('warnings') + ')' : '');
				if (!opts.auto) Blockbench.showQuickMessage(this.lastResult, 1500);
				if (res.warnings && res.warnings.length) res.warnings.forEach((w) => console.warn('[Vibey game] ' + w));
			} else if (res.timeout) {
				this.lastResult = T('push_timeout', id);
				Blockbench.showQuickMessage(this.lastResult, 2500);
			} else {
				this.lastResult = T('push_failed', id, res.message || res.error || '?');
				Blockbench.showMessageBox({ title: 'Vibey live link', message: this.lastResult, icon: 'error' });
			}
			refreshUI();
			return res;
		});
	},
	remove(id) {
		this.send({ type: 'remove', id });
	},
};
const defaultPushAction = () => (isEmoteFormat() ? 'play' : 'equip');
let autoPushTimer = null;
function scheduleAutoPush() {
	if (!LinkSettings.autoPush || Link.state !== 'connected' || !isVibey()) return;
	clearTimeout(autoPushTimer);
	const project = Project;
	autoPushTimer = setTimeout(() => {
		if (Project !== project || !isVibey()) return;
		Link.push(isEmoteFormat() ? (LinkSettings.replayEmote ? 'play' : 'none') : 'equip', { auto: true });
	}, 300);
}

// ------------------------------------------------------------- UI state
const UI = {
	kind: '', id: '', name: '', slot: '', showReference: true, sounds: [], sprites: [],
	linkState: 'off', linkText: '', lastResult: '', autoPush: false, active: false, errors: 0, warnings: 0,
};
let refreshQueued = false;
function refreshUI() {
	if (refreshQueued) return;
	refreshQueued = true;
	setTimeout(() => {
		refreshQueued = false;
		const meta = isVibey() ? getMeta() : null;
		UI.active = !!meta;
		UI.kind = meta ? kindOf() : '';
		UI.id = meta ? meta.id : '';
		UI.name = meta ? meta.name.en || meta.name.de || '' : '';
		UI.slot = meta && UI.kind === 'cosmetic' ? meta.slot : '';
		UI.showReference = meta ? meta.showReference !== false : true;
		UI.sounds = meta ? meta.sounds.map((s) => ({ id: s.id, kb: Math.max(1, Math.round(Core.base64Size(s.ogg) / 1024)) })) : [];
		UI.sprites = meta ? meta.sprites.map((s) => ({ id: s.id, texture: s.texture, uv: s.uv.join(' ') })) : [];
		UI.linkState = Link.state;
		UI.linkText = Link.message || T('link_off');
		UI.lastResult = Link.lastResult;
		UI.autoPush = !!LinkSettings.autoPush;
		if (meta && typeof Validator !== 'undefined') {
			const check = Validator.checks.find((c) => c.id === 'vibey_bundle');
			UI.errors = check ? check.errors.length : 0;
			UI.warnings = check ? check.warnings.length : 0;
		}
		updateStatusBar();
		if (typeof BarItems !== 'undefined' && BarItems.vibey_auto_push) BarItems.vibey_auto_push.setIcon(LinkSettings.autoPush ? 'check_box' : 'check_box_outline_blank');
		if (typeof BarItems !== 'undefined' && BarItems.vibey_toggle_reference) BarItems.vibey_toggle_reference.setIcon(UI.showReference ? 'check_box' : 'check_box_outline_blank');
	}, 0);
}
let statusMenu = null;
function updateStatusBar() {
	const bar = document.getElementById('status_bar');
	if (!bar) return;
	let el = document.getElementById('vibey_status');
	if (!el) {
		el = document.createElement('div');
		el.id = 'vibey_status';
		el.innerHTML = '<span class="vibey_dot"></span><span class="vibey_status_text"></span>';
		el.addEventListener('click', (e) => statusMenu && statusMenu.open(e));
	}
	if (el.parentNode !== bar) {
		const fps = bar.querySelector('.fps_counter_display');
		if (fps) bar.insertBefore(el, fps);
		else bar.appendChild(el);
	}
	el.className = 'f_right vibey_state_' + Link.state;
	el.style.display = isVibey() || Link.state !== 'off' ? '' : 'none';
	el.querySelector('.vibey_status_text').textContent = 'Vibey: ' + T('link_short_' + Link.state);
	el.title = (Link.message || T('link_off')) + (Link.lastResult ? '\n' + Link.lastResult : '');
}

// ------------------------------------------------------------------ CSS
const CSS = `
#vibey_status { cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0 8px; }
#vibey_status .vibey_dot, .vibey_panel .vibey_dot { width: 8px; height: 8px; display: inline-block; background: #6b6b75; flex-shrink: 0; }
.vibey_state_connected .vibey_dot { background: #3fe0c5; box-shadow: 0 0 6px #3fe0c5; }
.vibey_state_connecting .vibey_dot { background: #ffb547; }
.vibey_state_error .vibey_dot { background: #ff5470; }
.vibey_panel { padding: 6px 10px 10px; }
.vibey_panel h4 { margin: 12px 0 4px; display: flex; align-items: center; gap: 6px; font-size: 0.95em; color: var(--color-subtle_text); text-transform: uppercase; letter-spacing: .04em; }
.vibey_panel h4 .tool { margin-left: auto; }
.vibey_head { display: flex; align-items: center; gap: 8px; }
.vibey_badge { background: #8b6cff; color: #fff; padding: 0 6px; font-size: 0.85em; text-transform: uppercase; }
.vibey_id { font-family: var(--font-code, monospace); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vibey_dim { color: var(--color-subtle_text); }
.vibey_row { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.vibey_row button { min-width: 0; padding: 0 10px; height: 26px; border-radius: 0; }
.vibey_check { display: flex; align-items: center; gap: 6px; margin-top: 6px; cursor: pointer; }
.vibey_link { display: flex; align-items: flex-start; gap: 8px; margin-top: 2px; }
.vibey_link .vibey_dot { margin-top: 6px; }
.vibey_result { color: var(--color-subtle_text); font-size: 0.9em; margin-top: 4px; word-break: break-word; }
.vibey_list { list-style: none; margin: 0; padding: 0; }
.vibey_list li { display: flex; align-items: center; gap: 6px; padding: 2px 0; border-bottom: 1px solid var(--color-border); }
.vibey_list li .vibey_grow { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vibey_list li i { cursor: pointer; font-size: 18px; opacity: .75; }
.vibey_list li i:hover { opacity: 1; color: var(--color-accent); }
.vibey_issues { margin-top: 6px; cursor: pointer; }
.vibey_err { color: #ff5470; }
.vibey_warn { color: #ffb547; }
.vibey_problems { max-height: 360px; overflow: auto; padding-left: 18px; }
.vibey_problems li { margin: 3px 0; }
`;

// -------------------------------------------------------- plugin setup
const SEAL_SVG =
	'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" shape-rendering="crispEdges">' +
	'<rect x="1" y="2" width="14" height="13" fill="#8e96a3"/><rect x="1" y="2" width="14" height="2" fill="#a7aeb9"/>' +
	'<rect x="3" y="6" width="3" height="3" fill="#15151b"/><rect x="10" y="6" width="3" height="3" fill="#15151b"/>' +
	'<rect x="3" y="6" width="1" height="1" fill="#fff"/><rect x="10" y="6" width="1" height="1" fill="#fff"/>' +
	'<rect x="4" y="10" width="8" height="4" fill="#eef0f3"/><rect x="7" y="10" width="2" height="2" fill="#15151b"/>' +
	'<rect x="2" y="10" width="1" height="1" fill="#f29bb0"/><rect x="13" y="10" width="1" height="1" fill="#f29bb0"/></svg>';
const SEAL_ICON = 'data:image/svg+xml;base64,' + btoa(SEAL_SVG);

const disposables = [];
const listeners = [];
let restoreScriptCondition = null;
let intervalId = null;
function on(event, fn) {
	Blockbench.on(event, fn);
	listeners.push([event, fn]);
}

function setup() {
	// If the plugin file was executed twice (e.g. loaded while Blockbench was still restoring installed plugins),
	// dispose the older instance first so there is only ever one panel, status item and live link.
	if (window.VibeyPlugin && typeof window.VibeyPlugin.teardown === 'function') {
		try {
			window.VibeyPlugin.teardown();
		} catch (e) {
			console.warn('Vibey: could not dispose the previous instance', e);
		}
	}
	disposables.push(Blockbench.addCSS(CSS));

	// ---- properties
	disposables.push(new Property(ModelProject, 'object', 'vibey', { exposed: false, condition: { formats: [COSMETIC, EMOTE] } }));
	const attachOptions = { '': T('auto') };
	Core.ATTACH.forEach((a) => (attachOptions[a] = a));
	const groupSelectedOk = (needRoot) => {
		const g = Group.first_selected;
		if (!g || isPlayerBone(g)) return false;
		return needRoot ? g.parent === 'root' : true;
	};
	disposables.push(new Property(Group, 'string', 'vibey_attach', {
		condition: (g) => isVibey() && (g ? true : groupSelectedOk(true)),
		inputs: { element_panel: { input: { label: T('attach'), type: 'select', options: attachOptions } } },
	}));
	disposables.push(new Property(Group, 'boolean', 'vibey_emissive', {
		condition: (g) => isVibey() && (g ? true : groupSelectedOk(false)),
		inputs: { element_panel: { input: { label: T('emissive'), type: 'checkbox' } } },
	}));
	disposables.push(new Property(Group, 'boolean', 'vibey_translucent', {
		condition: (g) => isVibey() && (g ? true : groupSelectedOk(false)),
		inputs: { element_panel: { input: { label: T('translucent'), type: 'checkbox' } } },
	}));
	const kp = (type, name, label, channel) =>
		disposables.push(new Property(KeyframeDataPoint, type, name, {
			label,
			condition: (p) => isVibey() && !!p && !!p.keyframe && p.keyframe.channel === channel,
		}));
	kp('string', 'vibey_sprite', T('kp_sprite'), 'particle');
	kp('string', 'vibey_count', T('kp_count'), 'particle');
	kp('string', 'vibey_spread', T('kp_spread'), 'particle');
	kp('string', 'vibey_velocity', T('kp_velocity'), 'particle');
	kp('string', 'vibey_lifetime', T('kp_lifetime'), 'particle');
	kp('string', 'vibey_size', T('kp_size'), 'particle');
	kp('string', 'vibey_gravity', T('kp_gravity'), 'particle');
	kp('boolean', 'vibey_emissive', T('kp_emissive'), 'particle');
	kp('string', 'vibey_volume', T('kp_volume'), 'sound');
	kp('string', 'vibey_pitch', T('kp_pitch'), 'sound');
	kp('string', 'vibey_bone', T('kp_bone'), 'timeline');
	kp('boolean', 'vibey_visible', T('kp_visible'), 'timeline');
	// Molang scripts are not part of the .vibey format: hide the script field in Vibey projects.
	const scriptProp = KeyframeDataPoint.properties.script;
	if (scriptProp) {
		const orig = scriptProp.condition;
		scriptProp.condition = (p) => !isVibey() && Condition(orig, p);
		restoreScriptCondition = () => (scriptProp.condition = orig);
	}

	// ---- codec + formats
	const codec = new Codec('vibey', {
		name: 'Vibey Bundle',
		extension: 'vibey',
		remember: false,
		load_filter: { type: 'json', extensions: ['vibey'] },
		compile() {
			return compileProjectJSON().json;
		},
		load(model, file) {
			importBundle(model, file && file.path);
		},
		export() {
			exportBundle();
		},
		fileName() {
			const m = getMeta();
			return m ? m.id.split(':').pop().replace(/[\/]/g, '_') : 'bundle';
		},
	});
	disposables.push(codec);
	const common = {
		category: 'minecraft',
		target: 'Vibey Client',
		codec,
		box_uv: true,
		optional_box_uv: true,
		box_uv_float_size: true,
		single_texture_default: true,
		per_texture_uv_size: true,
		animated_textures: true,
		bone_rig: true,
		centered_grid: true,
		rotate_cubes: true,
		locators: true,
		animation_mode: true,
		display_mode: false,
		model_identifier: false,
		molang: false,
		node_name_regex: '\\w',
		animation_grouping: 'disabled',
		onActivation() {
			setTimeout(() => {
				ensureDefaultTexture();
				Reference.ensure();
				refreshUI();
			}, 0);
		},
	};
	const makeFormat = (id, kind) =>
		new ModelFormat(id, Object.assign({}, common, {
			name: T('format_' + kind),
			description: T('format_' + kind + '_desc'),
			icon: kind === 'emote' ? 'emoji_people' : 'checkroom',
			format_page: { content: [{ type: 'h3', text: T('format_' + kind) }, { text: T('format_info') }].concat(kind === 'emote' ? [{ text: T('format_emote_info') }] : []) },
			onSetup(project, newModel) {
				getMeta(project);
				if (newModel && kind === 'emote') {
					createPlayerRig(false);
					if (!Animator.animations.length) new Animation({ name: 'main', loop: 'once', length: 2 }).add(false);
				}
				setTimeout(() => Reference.ensure(), 0);
			},
			new() {
				if (newProject(this)) {
					setTimeout(() => openPropertiesDialog(true), 50);
					return true;
				}
				return false;
			},
		}));
	disposables.push(makeFormat(COSMETIC, 'cosmetic'));
	disposables.push(makeFormat(EMOTE, 'emote'));

	// ---- actions
	const A = (id, opts) => {
		const a = new Action(id, Object.assign({ category: 'file' }, opts));
		disposables.push(a);
		return a;
	};
	const exportAction = A('vibey_export', { name: T('export_bundle'), icon: 'file_upload', condition: isVibey, click: exportBundle });
	const importAction = A('vibey_import', {
		name: T('import_bundle'),
		icon: 'file_download',
		click() {
			Blockbench.import({ resource_id: 'vibey_bundle', extensions: ['vibey'], type: 'Vibey Bundle', readtype: 'text' }, (files) => {
				files.forEach((f) => importBundle(f.content, f.path));
			});
		},
	});
	A('vibey_properties', { name: T('properties'), icon: 'tune', condition: isVibey, click: () => openPropertiesDialog(false) });
	A('vibey_validate', { name: T('validate_bundle'), icon: 'checklist', condition: isVibey, click: validateNow });
	A('vibey_toggle_reference', { name: T('toggle_reference'), icon: 'check_box', category: 'view', condition: isVibey, click: () => setReferenceVisible(!(getMeta().showReference !== false)) });
	A('vibey_repair_rig', { name: T('repair_rig'), icon: 'accessibility_new', condition: isEmoteFormat, click: () => (createPlayerRig(true), Blockbench.showQuickMessage(T('rig_repaired'), 1500)) });
	A('vibey_connect', { name: T('connect'), icon: 'link', click: () => (Link.state === 'off' || Link.state === 'error' ? Link.connect() : Link.disconnect()) });
	A('vibey_push', { name: T('push'), icon: 'send', condition: isVibey, click: () => Link.push(defaultPushAction()) });
	A('vibey_play', { name: T('play'), icon: 'play_circle', condition: isEmoteFormat, click: () => Link.push('play') });
	A('vibey_auto_push', {
		name: T('auto_push'),
		icon: LinkSettings.autoPush ? 'check_box' : 'check_box_outline_blank',
		click() {
			LinkSettings.autoPush = !LinkSettings.autoPush;
			saveLinkSettings();
			refreshUI();
		},
	});
	A('vibey_link_settings', { name: T('link_settings'), icon: 'settings_ethernet', click: openLinkSettings });
	A('vibey_add_sound', { name: T('add_sound'), icon: 'music_note', condition: isVibey, click: addSoundDialog });
	A('vibey_add_sprite', { name: T('add_sprite'), icon: 'auto_awesome', condition: isVibey, click: () => spriteDialog() });
	MenuBar.addAction(exportAction, 'file.export');
	MenuBar.addAction(importAction, 'file.import');

	const menu = new BarMenu('vibey', [
		'vibey_properties', 'vibey_validate', '_',
		'vibey_toggle_reference', 'vibey_repair_rig', '_',
		'vibey_connect', 'vibey_push', 'vibey_play', 'vibey_auto_push', 'vibey_link_settings', '_',
		'vibey_add_sound', 'vibey_add_sprite', '_',
		'vibey_import', 'vibey_export',
	], { name: 'Vibey', condition: () => true });
	MenuBar.addMenu(menu, 'view');
	disposables.push(menu);
	statusMenu = new Menu('vibey_status_menu', ['vibey_connect', 'vibey_push', 'vibey_play', 'vibey_auto_push', '_', 'vibey_link_settings']);

	// ---- panel
	const panel = new Panel('vibey_panel', {
		name: 'Vibey',
		icon: SEAL_ICON,
		condition: { formats: [COSMETIC, EMOTE], modes: ['edit', 'animate', 'paint'] },
		default_position: { slot: 'right_bar', float_position: [0, 0], float_size: [300, 420], height: 420 },
		component: {
			name: 'vibey-panel',
			data() {
				return { ui: UI };
			},
			methods: {
				props: () => openPropertiesDialog(false),
				validate: validateNow,
				toggleRef: (v) => setReferenceVisible(v),
				connect: () => Link.connect(),
				disconnect: () => Link.disconnect(),
				push: () => Link.push(defaultPushAction()),
				play: () => Link.push('play'),
				settings: openLinkSettings,
				setAutoPush(v) {
					LinkSettings.autoPush = !!v;
					saveLinkSettings();
					refreshUI();
				},
				addSound: addSoundDialog,
				playSound,
				renameSound: (id) => renameItem('sounds', id),
				removeSound: (id) => removeItem('sounds', id),
				addSprite: () => spriteDialog(),
				editSprite: (id) => spriteDialog(id),
				removeSprite: (id) => removeItem('sprites', id),
				openValidator: () => Validator.openDialog(),
			},
			template: `
<div class="vibey_panel">
	<div class="vibey_head"><span class="vibey_badge">{{ ui.kind }}</span><span class="vibey_id" :title="ui.id">{{ ui.id || '—' }}</span></div>
	<div class="vibey_dim">{{ ui.name || '—' }}<template v-if="ui.slot"> · {{ ui.slot }}</template></div>
	<div class="vibey_row">
		<button @click="props()">${esc(T('properties'))}</button>
		<button @click="validate()">${esc(T('validate'))}</button>
	</div>
	<div class="vibey_issues" v-if="ui.errors || ui.warnings" @click="openValidator()">
		<span class="vibey_err" v-if="ui.errors">{{ ui.errors }} ${esc(T('errors'))}</span>
		<span class="vibey_warn" v-if="ui.warnings">&nbsp;{{ ui.warnings }} ${esc(T('warnings'))}</span>
	</div>
	<label class="vibey_check"><input type="checkbox" :checked="ui.showReference" @change="toggleRef($event.target.checked)"> ${esc(T('toggle_reference'))}</label>

	<h4>${esc(T('live_link'))}</h4>
	<div class="vibey_link" :class="'vibey_state_' + ui.linkState"><span class="vibey_dot"></span><span>{{ ui.linkText }}</span></div>
	<div class="vibey_row">
		<button v-if="ui.linkState === 'off' || ui.linkState === 'error'" @click="connect()">${esc(T('connect_short'))}</button>
		<button v-else @click="disconnect()">${esc(T('disconnect'))}</button>
		<button @click="push()">${esc(T('push_short'))}</button>
		<button v-if="ui.kind === 'emote'" @click="play()">${esc(T('play_short'))}</button>
		<button @click="settings()" title="${esc(T('link_settings'))}"><i class="material-icons" style="font-size:16px;vertical-align:middle">settings</i></button>
	</div>
	<label class="vibey_check"><input type="checkbox" :checked="ui.autoPush" @change="setAutoPush($event.target.checked)"> ${esc(T('auto_push'))}</label>
	<div class="vibey_result" v-if="ui.lastResult">{{ ui.lastResult }}</div>

	<h4>${esc(T('sounds'))}<i class="material-icons tool" @click="addSound()" title="${esc(T('add_sound'))}">add</i></h4>
	<ul class="vibey_list">
		<li v-for="s in ui.sounds" :key="s.id"><span class="vibey_grow">{{ s.id }}</span><span class="vibey_dim">{{ s.kb }} KB</span>
			<i class="material-icons" @click="playSound(s.id)">play_arrow</i><i class="material-icons" @click="renameSound(s.id)">edit</i><i class="material-icons" @click="removeSound(s.id)">delete</i></li>
		<li v-if="!ui.sounds.length" class="vibey_dim">${esc(T('no_sounds'))}</li>
	</ul>
	<h4>${esc(T('sprites'))}<i class="material-icons tool" @click="addSprite()" title="${esc(T('add_sprite'))}">add</i></h4>
	<ul class="vibey_list">
		<li v-for="s in ui.sprites" :key="s.id"><span class="vibey_grow">{{ s.id }}</span><span class="vibey_dim">{{ s.texture }} [{{ s.uv }}]</span>
			<i class="material-icons" @click="editSprite(s.id)">edit</i><i class="material-icons" @click="removeSprite(s.id)">delete</i></li>
		<li v-if="!ui.sprites.length" class="vibey_dim">${esc(T('no_sprites'))}</li>
	</ul>
</div>`,
		},
	});
	disposables.push(panel);

	// ---- validator
	const check = new ValidatorCheck('vibey_bundle', {
		condition: isVibey,
		update_triggers: ['finished_edit', 'undo', 'redo', 'select_project', 'load_project', 'add_texture', 'update_project_settings'],
		run() {
			const r = buildBundle({ skipBinary: true });
			r.errors.forEach((m) => this.fail({ message: m }));
			r.warnings.forEach((m) => this.warn({ message: m }));
			setTimeout(refreshUI, 60);
		},
	});
	disposables.push(check);

	// ---- events
	['finished_edit', 'undo', 'redo'].forEach((ev) => on(ev, () => isVibey() && (Reference.ensure(), scheduleAutoPush())));
	on('select_project', () => setTimeout(() => (Reference.ensure(), refreshUI()), 0));
	on('unselect_project', () => clearVisibilityPreview());
	on('select_mode', () => clearVisibilityPreview());
	on('add_texture', () => setTimeout(() => isVibey() && (ensureDefaultTexture(), markChanged()), 0));
	on('display_animation_frame', applyVisibilityPreview);
	on('close_project', () => setTimeout(refreshUI, 0));
	intervalId = setInterval(() => {
		try {
			Reference.ensure();
			updateStatusBar();
		} catch (e) {}
	}, 1000);

	window.VibeyPlugin = {
		VERSION, Core, Link, LinkSettings, UI, Reference,
		buildBundle, compileProjectJSON, snapshotProject, applySnapshot, importBundle, exportBundle,
		getMeta, applyProperties, createPlayerRig, setReferenceVisible, addSoundFiles, openPropertiesDialog,
		openLinkSettings, validateNow, saveLinkSettings, refreshUI, T, teardown,
	};
	refreshUI();
	if (LinkSettings.autoConnect) setTimeout(() => Link.connect(), 1500);
}

function teardown() {
	Link.disconnect();
	clearInterval(intervalId);
	clearTimeout(autoPushTimer);
	clearVisibilityPreview();
	Reference.removeAll();
	listeners.forEach(([ev, fn]) => Blockbench.removeListener(ev, fn));
	listeners.length = 0;
	if (restoreScriptCondition) restoreScriptCondition();
	if (statusMenu && statusMenu.delete) statusMenu.delete();
	disposables.reverse().forEach((d) => {
		try {
			d.delete();
		} catch (e) {}
	});
	disposables.length = 0;
	const el = document.getElementById('vibey_status');
	if (el) el.remove();
	delete window.VibeyPlugin;
}

BBPlugin.register('vibey', {
	title: 'Vibey Client',
	author: 'Vibe Craft Studios',
	description: 'Create Vibey Client cosmetics and emotes (.vibey) and push them live into the running game.',
	about:
		'Adds the **Vibey Cosmetic** and **Vibey Emote** formats with a reference player, `.vibey` import/export, validation and a live link to Vibey Client.\n\n' +
		'* New: File > New > Vibey Cosmetic / Vibey Emote\n* Export: File > Export > Vibey Bundle\n* Live link: Vibey > Connect to game / Push to game\n\nDocumentation: blockbench/README.md in the vibey-client repository.',
	icon: SEAL_ICON,
	version: VERSION,
	variant: 'desktop',
	min_version: '5.0.0',
	tags: ['Minecraft: Java Edition', 'Animation', 'Exporter'],
	await_loading: true,
	onload: setup,
	onunload: teardown,
});
})();
