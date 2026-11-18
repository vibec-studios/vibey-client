'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const Core = require('../vibey.js');
const { validate: specValidate } = require('./spec-validator');
const { cosmeticSnapshot, emoteSnapshot, pngB64, fakeOgg } = require('./fixtures');

const compileOk = (snap) => {
	const r = Core.compileBundle(snap);
	assert.deepEqual(r.errors, [], 'compile errors');
	const v = Core.validateBundle(r.bundle);
	assert.deepEqual(v.errors, [], 'plugin validator errors');
	assert.deepEqual(specValidate(r.bundle).errors, [], 'spec validator errors');
	return r.bundle;
};

test('exports load in Node without Blockbench', () => {
	assert.equal(typeof Core.compileBundle, 'function');
	assert.equal(globalThis.VibeyCore, Core);
});

test('player pivots match SPEC §5', () => {
	assert.deepEqual(Core.PLAYER_PIVOTS, {
		root: [0, 0, 0], head: [0, 24, 0], body: [0, 24, 0], right_arm: [5, 22, 0], left_arm: [-5, 22, 0], right_leg: [1.9, 12, 0], left_leg: [-1.9, 12, 0],
	});
	// the player faces north (-Z), so (like Blockbench's own player template) the right arm is at +X
	assert.ok(Core.PLAYER_CUBES.right_arm.from[0] > 0);
	assert.ok(Core.PLAYER_CUBES.left_arm.to[0] < 0);
	// every part converts to the vanilla PlayerModel numbers (PartPose.offset)
	assert.deepEqual(Core.toModelSpace(Core.PLAYER_PIVOTS.right_arm), [-5, 2, 0]);
	assert.deepEqual(Core.toModelSpace(Core.PLAYER_PIVOTS.left_arm), [5, 2, 0]);
	assert.deepEqual(Core.toModelSpace(Core.PLAYER_PIVOTS.right_leg), [-1.9, 12, 0]);
	assert.deepEqual(Core.toModelSpace(Core.PLAYER_PIVOTS.head), [0, 0, 0]);
});

test('box UV faces match Blockbench / vanilla skin layout', () => {
	const head = Core.boxUvFaces([8, 8, 8], [0, 0], false);
	assert.deepEqual(head.north, [8, 8, 16, 16]); // face
	assert.deepEqual(head.east, [0, 8, 8, 16]);
	assert.deepEqual(head.west, [16, 8, 24, 16]);
	assert.deepEqual(head.south, [24, 8, 32, 16]);
	assert.deepEqual(head.up, [16, 8, 8, 0]);
	assert.deepEqual(head.down, [24, 0, 16, 8]);
	const arm = Core.boxUvFaces([4, 12, 4], [40, 16], false);
	assert.deepEqual(arm.north, [44, 20, 48, 32]);
	const mirrored = Core.boxUvFaces([4, 12, 4], [40, 16], true);
	assert.deepEqual(mirrored.north, [48, 20, 44, 32]);
	assert.deepEqual(mirrored.east, [52, 20, 48, 32]); // east/west swapped and flipped
	// float sizes are not floored (like Minecraft's ModelPart)
	assert.deepEqual(Core.boxUvFaces([1.5, 2, 1], [0, 0], false).north, [1, 1, 2.5, 3]);
});

test('parseNumber / parseVec3', () => {
	assert.equal(Core.parseNumber('12.5'), 12.5);
	assert.equal(Core.parseNumber(' -3 '), -3);
	assert.equal(Core.parseNumber('', 1), 1);
	assert.ok(Number.isNaN(Core.parseNumber('math.sin(q.anim_time)')));
	assert.deepEqual(Core.parseVec3('0.2'), [0.2, 0.2, 0.2]);
	assert.deepEqual(Core.parseVec3('0, 0.05 ,0'), [0, 0.05, 0]);
	assert.equal(Core.parseVec3(''), undefined);
	assert.equal(Core.parseVec3('1 2'), null);
});

test('keyframe conversion: interpolation, pre/post, sorting and round trip', () => {
	const errors = [];
	const kfs = [
		{ time: 1, interpolation: 'catmullrom', points: [{ x: '0', y: '30', z: '0' }] },
		{ time: 0, interpolation: 'linear', points: [{ x: 0, y: 0, z: 0 }] },
		{ time: 2, interpolation: 'step', points: [{ x: '5', y: 0, z: 0 }, { x: '10', y: 0, z: 0 }] },
		{ time: 0.1 + 0.2, interpolation: 'linear', points: [{ x: 0.1 + 0.2, y: 0, z: 0 }] },
	];
	const spec = Core.keyframesToSpec(kfs, { errors, channel: 'rotation' });
	assert.deepEqual(errors, []);
	assert.deepEqual(spec, [
		{ t: 0, v: [0, 0, 0] },
		{ t: 0.3, v: [0.3, 0, 0] },
		{ t: 1, v: [0, 30, 0], i: 'catmullrom' },
		{ t: 2, v: [10, 0, 0], i: 'step', pre: [5, 0, 0] },
	]);
	const back = Core.specToKeyframes(spec);
	assert.deepEqual(Core.keyframesToSpec(back, { channel: 'rotation' }), spec);
	// scale defaults to 1, Molang is rejected with a readable message
	assert.deepEqual(Core.keyframesToSpec([{ time: 0, points: [{ x: '', y: '2', z: '' }] }], { channel: 'scale' })[0].v, [1, 2, 1]);
	const e2 = [];
	Core.keyframesToSpec([{ time: 0, points: [{ x: 'q.anim_time*10', y: 0, z: 0 }] }], { errors: e2, channel: 'rotation', animation: 'a', bone: 'b' });
	assert.match(e2[0], /expression \(Molang\)/);
});

test('coordinates: a cube exactly on the reference right arm exports with SPEC numbers', () => {
	const arm = Core.PLAYER_CUBES.right_arm;
	const snap = {
		kind: 'cosmetic',
		meta: { id: 'test:sleeve', name: { en: 'Sleeve' }, slot: 'back' },
		textures: [{ id: 'main', uvWidth: 64, uvHeight: 64, png: pngB64(64, 64), isDefault: true }],
		nodes: [{ type: 'group', name: 'sleeve', origin: [5, 22, 0], rotation: [0, 0, 0], attach: 'right_arm', children: [
			{ type: 'cube', name: 'cube', from: arm.from, to: arm.to, origin: [0, 0, 0], rotation: [0, 0, 0], boxUv: true, uvOffset: [40, 16], faces: Object.fromEntries(Core.FACES.map((f) => [f, { uv: [0, 0, 0, 0], texture: 'main' }])) },
		] }],
		animations: [],
	};
	const b = compileOk(snap);
	const bone = b.model.bones[0];
	assert.equal(bone.attach, 'right_arm');
	assert.deepEqual(bone.pivot, [5, 22, 0]);
	assert.deepEqual(bone.cubes[0].from, [4, 12, -2]);
	assert.deepEqual(bone.cubes[0].to, [8, 24, 2]);
	assert.deepEqual(bone.cubes[0].uv, { box: [40, 16] });
	// north (= the player's front, -Z) face of that box maps to the arm front in the skin layout
	assert.deepEqual(Core.boxUvFaces([4, 12, 4], bone.cubes[0].uv.box).north, [44, 20, 48, 32]);
	// SPEC conversion to MC model space (x' = -x, y' = 24 - y, z' = z), then relative to the arm pivot,
	// must give the vanilla right arm box: addBox(-3, -2, -2, 4, 12, 4) at PartPose.offset(-5, 2, 0).
	const mc = Core.toModelSpace;
	const pivotMc = mc(bone.pivot);
	assert.deepEqual(pivotMc, [-5, 2, 0]);
	const fromMc = mc([bone.cubes[0].to[0], bone.cubes[0].to[1], bone.cubes[0].from[2]]); // min corner in MC space (x and y flipped)
	assert.deepEqual(fromMc.map((v, i) => v - pivotMc[i]), [-3, -2, -2]);
});

test('cosmetic: compile -> both validators -> decompile -> compile is lossless', () => {
	const b1 = compileOk(cosmeticSnapshot());
	assert.equal(b1.cosmetic.slot, 'hat');
	assert.deepEqual(b1.cosmetic.hideWithArmor, ['head']);
	assert.equal(b1.model.texture, 'main');
	const hat = b1.model.bones.find((x) => x.name === 'hat');
	const top = b1.model.bones.find((x) => x.name === 'top');
	assert.equal(hat.attach, 'head');
	assert.equal(top.parent, 'hat');
	assert.equal(top.attach, undefined);
	assert.equal(top.emissive, true);
	assert.deepEqual(hat.cubes[0].uv, { box: [0, 0] });
	assert.deepEqual(top.cubes[0].uv.north, { uv: [0, 0, 6, 4], texture: 'glow' });
	assert.equal(top.cubes[0].uv.east, undefined, 'hidden face omitted');
	assert.deepEqual(top.cubes[0].pivot, [0, 35, 0]);
	assert.deepEqual(b1.model.locators, { tip: { bone: 'top', position: [0, 38, 0] } });
	assert.deepEqual(b1.textures[1].animation, { frameTime: 2, frames: [0, 1, 2, 3, 2, 1] });
	const idle = b1.animations.idle;
	assert.equal(idle.loop, true);
	assert.deepEqual(idle.bones.top.scale, [{ t: 0.5, v: [1.2, 1.2, 1.2], i: 'step', pre: [1, 1, 1] }]);
	assert.deepEqual(idle.events.map((e) => e.type), ['sound', 'particle', 'particle', 'sound', 'visibility']);
	assert.deepEqual(idle.events[1], { t: 0.5, type: 'particle', particle: 'minecraft:heart', locator: 'tip', count: 3, spread: [0.2, 0.2, 0.2], velocity: [0, 0.05, 0] });
	assert.deepEqual(idle.events[2], { t: 0.5, type: 'particle', sprite: 'sparkle', locator: 'tip', count: 4, lifetime: 0.8, size: 0.15, gravity: -0.01, emissive: true });
	assert.deepEqual(idle.events[4], { t: 1.2, type: 'visibility', bone: 'top', visible: false });
	const b2 = compileOk(Core.decompileBundle(b1));
	assert.deepEqual(b2, b1);
	// stringify is valid JSON and stable
	assert.deepEqual(JSON.parse(Core.stringify(b1)), b1);
});

test('emote: player bones, props, generated bone, locators on player bones, round trip', () => {
	const b1 = compileOk(emoteSnapshot());
	assert.deepEqual(b1.emote, { animation: 'main', loop: false, thirdPerson: true, stopOnMove: true, hideHeldItems: false });
	const names = b1.model.bones.map((x) => x.name);
	assert.ok(!names.some((n) => Core.PLAYER_BONES.includes(n)), 'player bones are not exported as model bones');
	const flag = b1.model.bones.find((x) => x.name === 'flag');
	assert.equal(flag.attach, 'right_arm');
	assert.equal(flag.parent, null);
	assert.equal(flag.translucent, true);
	const gen = b1.model.bones.find((x) => x.name === 'right_arm_cubes');
	assert.equal(gen.generated, true);
	assert.deepEqual(gen.pivot, [5, 22, 0]);
	assert.equal(gen.cubes[0].mirror, true);
	assert.deepEqual(b1.model.locators.hand, { bone: 'right_arm', position: [6, 10, 0] });
	assert.deepEqual(Object.keys(b1.animations.main.bones).sort(), ['flag', 'right_arm', 'root']);
	assert.equal(b1.animations.loop_variant.hold, true);
	const snap2 = Core.decompileBundle(b1);
	const rightArm = snap2.nodes[0].children.find((n) => n.name === 'right_arm');
	assert.ok(rightArm.children.find((c) => c.type === 'cube' && c.name === 'glove'), 'generated cubes return into the player bone');
	assert.deepEqual(compileOk(snap2), b1);
});

test('emote errors: moved player pivot, reserved names, loose cubes', () => {
	const snap = emoteSnapshot();
	snap.nodes[0].children[2].origin = [6, 22, 0];
	snap.nodes.push({ type: 'cube', name: 'loose', from: [0, 0, 0], to: [1, 1, 1], faces: {} });
	const r = Core.compileBundle(snap);
	assert.ok(r.errors.some((e) => /right_arm.*pivot/.test(e)));
	assert.ok(r.errors.some((e) => /not inside a group/.test(e)));
	const bad = compileOk(emoteSnapshot());
	bad.model.bones[0].name = 'head';
	assert.ok(Core.validateBundle(bad).errors.some((e) => /reserved/.test(e)));
	assert.ok(specValidate(bad).errors.length);
});

test('validation catches broken references and limits', () => {
	const base = () => JSON.parse(JSON.stringify(compileOk(cosmeticSnapshot())));
	const expectErr = (mutate, re) => {
		const b = base();
		mutate(b);
		const v = Core.validateBundle(b);
		assert.ok(v.errors.some((e) => re.test(e)), `expected ${re} in ${JSON.stringify(v.errors)}`);
		assert.ok(specValidate(b).errors.length > 0, 'spec validator should also fail');
	};
	expectErr((b) => (b.id = 'Bad Id'), /Id "Bad Id" is invalid/);
	expectErr((b) => (b.name = { de: 'x' }), /English name/);
	expectErr((b) => (b.cosmetic.slot = 'boots'), /Slot "boots" is invalid/);
	expectErr((b) => (b.model.bones[0].attach = undefined), /root bones need "attach"/);
	expectErr((b) => (b.model.bones[1].parent = 'nope'), /parent "nope" does not exist/);
	expectErr((b) => (b.model.bones[0].cubes[0].texture = 'missing'), /texture "missing" does not exist/);
	expectErr((b) => (b.animations.idle.bones.ghost = { rotation: [{ t: 0, v: [0, 0, 0] }] }), /animates "ghost"/);
	expectErr((b) => (b.animations.idle.events[1].locator = 'nowhere'), /locator "nowhere" does not exist/);
	expectErr((b) => (b.animations.idle.events[2].sprite = 'nope'), /sprite "nope" does not exist/);
	expectErr((b) => (b.animations.idle.events[3].sound = 'missing_sound'), /embedded sound "missing_sound"/);
	expectErr((b) => (b.animations.idle.events[4].bone = 'nope'), /visibility bone "nope"/);
	expectErr((b) => (b.sounds[0].ogg = Buffer.from('RIFF0000').toString('base64')), /not a valid base64 OGG/);
	expectErr((b) => (b.sounds[0].ogg = Buffer.concat([fakeOgg(), Buffer.alloc(600 * 1024)]).toString('base64')), /KiB/);
	expectErr((b) => (b.textures[0].png = pngB64(1024, 1024)), /limit is 512/);
	expectErr((b) => (b.textures[0].png = 'notbase64!!'), /not a valid base64 PNG/);
	expectErr((b) => {
		b.model.bones[0].parent = 'top';
	}, /cycle/);
	expectErr((b) => {
		const cube = b.model.bones[0].cubes[0];
		b.model.bones[0].cubes = Array.from({ length: 2100 }, () => cube);
	}, /Too many cubes/);
	const big = base();
	big.textures.push({ id: 'huge', width: 512, height: 512, png: Buffer.alloc(2.2 * 1024 * 1024, 65).toString('base64') });
	assert.ok(Core.validateBundle(big).errors.some((e) => /2 MiB/.test(e)));
});

test('cape and shield cosmetics may skip the model', () => {
	const snap = cosmeticSnapshot();
	snap.meta.slot = 'cape';
	snap.meta.capeTexture = 'cape';
	snap.textures = [{ id: 'cape', uvWidth: 64, uvHeight: 32, png: pngB64(64, 32), isDefault: true }];
	snap.nodes = [];
	snap.animations = [];
	snap.meta.sprites = [];
	const b = compileOk(snap);
	assert.equal(b.model, undefined);
	assert.deepEqual(b.cosmetic.cape, { texture: 'cape' });
	const hat = cosmeticSnapshot();
	hat.nodes = [];
	hat.animations = [];
	assert.ok(Core.validateBundle(Core.compileBundle(hat).bundle).errors.some((e) => /no bones/.test(e)));
});

test('devlink helpers', () => {
	assert.deepEqual(Core.parseDevlink('{"port":47821,"token":"abcdefghijklmnopqrstuvwx","pid":42}'), { port: 47821, token: 'abcdefghijklmnopqrstuvwx', pid: 42 });
	assert.throws(() => Core.parseDevlink('{"port":0,"token":"x"}'), /port/);
	assert.throws(() => Core.parseDevlink('nope'), /not valid JSON/);
	assert.deepEqual([0, 1, 2, 3, 4, 5, 9].map(Core.backoffDelay), [500, 1000, 2000, 4000, 8000, 15000, 15000]);
	assert.equal(Core.devlinkPath('C:\\Users\\x\\AppData\\Roaming\\.minecraft'), 'C:\\Users\\x\\AppData\\Roaming\\.minecraft\\vibeyclient\\devlink.json');
	assert.equal(Core.devlinkPath('/home/x/.minecraft/'), '/home/x/.minecraft/vibeyclient/devlink.json');
	assert.equal(Core.defaultGameDir('win32', { APPDATA: 'C:\\A' }, 'C:\\H'), 'C:\\A\\.minecraft');
	assert.equal(Core.defaultGameDir('linux', {}, '/home/x'), '/home/x/.minecraft');
});
