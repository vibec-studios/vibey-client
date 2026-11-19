'use strict';
/**
 * "Build on the player" (no Blockbench): the prefab player groups, how a part's group becomes its `attach`, the
 * pet anchor, auto-assign rules, import of every starter bundle, conversion of old projects and byte-identical
 * round trips.
 */
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const Core = require('../vibey.js');
const { validate: specValidate } = require('./spec-validator');

const DIST = path.join(__dirname, '..', '..', 'content', 'dist');
const starterFiles = () => {
	const list = (d) => (fs.existsSync(d) ? fs.readdirSync(d).filter((f) => f.endsWith('.vibey')).map((f) => path.join(d, f)) : []);
	return list(DIST).concat(list(path.join(DIST, 'polar-pals')));
};
const cube = (from, to, extra) => Object.assign({ type: 'cube', name: 'c', from, to, inflate: 0, mirror: false, origin: [0, 0, 0], rotation: [0, 0, 0], boxUv: true, uvOffset: [0, 0], faces: {} }, extra);
const group = (name, origin, children, extra) => Object.assign({ type: 'group', name, origin, rotation: [0, 0, 0], children: children || [] }, extra);
const tex = { id: 'main', uvWidth: 16, uvHeight: 16, png: 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAE0lEQVR4nGNgGAWjYBSMglEwCAAEEAABKmbVAgAAAABJRU5ErkJggg==', isDefault: true };
/** A cosmetic snapshot built "on the player": `parts` = { refId: [nodes] }. */
function onPlayer(slot, parts, meta) {
	const m = Object.assign({ id: 'test:item', name: { en: 'Item' }, slot, pet: { follow: '', scale: 1 } }, meta);
	const tree = Core.refTree('cosmetic', slot, m);
	Object.keys(parts).forEach((id) => parts[id].forEach((n) => tree.byId[id].children.push(n)));
	return { kind: 'cosmetic', meta: m, textures: [tex], nodes: tree.roots, animations: [] };
}
const compile = (snap) => {
	const r = Core.compileBundle(snap);
	assert.deepEqual(r.errors, [], 'compile errors');
	assert.deepEqual(Core.validateBundle(r.bundle, { skipBinary: true }).errors, [], 'plugin validator');
	return r.bundle;
};

test('player groups: vanilla pivots (SPEC §5), nesting, one set per project kind', () => {
	for (const p of Core.PLAYER_PARTS) assert.deepEqual(Core.REF_BY_ID[p].pivot, Core.PLAYER_PIVOTS[p], p);
	assert.deepEqual(Core.REF_BY_ID.root.pivot, Core.PLAYER_PIVOTS.root);
	const ids = (kind, slot) => Core.refGroupsFor(kind, slot).map((r) => r.id);
	assert.deepEqual(ids('cosmetic', 'hat'), ['head', 'above_head', 'body', 'back', 'shoulder_right', 'shoulder_left', 'hips', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'around']);
	assert.deepEqual(ids('cosmetic', 'pet'), ['head', 'above_head', 'body', 'back', 'shoulder_right', 'shoulder_left', 'hips', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'beside']);
	assert.deepEqual(ids('emote'), ['root', 'head', 'above_head', 'body', 'back', 'shoulder_right', 'shoulder_left', 'hips', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'around']);
	const t = Core.refTree('emote');
	assert.deepEqual(t.roots.map((n) => n.ref), ['root', 'around']);
	assert.deepEqual(t.byId.root.children.map((n) => n.ref), Core.PLAYER_PARTS, 'emote player bones under root, like the vanilla rig');
	assert.deepEqual(t.byId.body.children.map((n) => n.ref), ['back', 'shoulder_right', 'shoulder_left', 'hips']);
	assert.deepEqual(Core.refTree('cosmetic', 'wings').roots.map((n) => n.ref), ['head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'around']);
	// the pet anchor is where the game draws the pet (AvatarTicker / AvatarDraw)
	assert.deepEqual(Core.refPivot('beside', { pet: { follow: 'ground' } }), [15.2, 0, 5.6]);
	assert.deepEqual(Core.refPivot('beside', { pet: { follow: 'float' } }), [11.2, 24.8, 4]);
	assert.deepEqual(Core.refPivot('beside', { pet: { follow: 'shoulder_left' } }), [-6, 24, 0.5]);
	assert.deepEqual(Core.refPivot('beside', {}), [15.2, 0, 5.6], 'default follow = ground');
});

test('export: a part in a player group becomes a root bone with the mapped attach, coordinates absolute', () => {
	const map = {
		head: 'head', above_head: 'head', body: 'body', back: 'body', shoulder_right: 'body', shoulder_left: 'body', hips: 'body',
		right_arm: 'right_arm', left_arm: 'left_arm', right_leg: 'right_leg', left_leg: 'left_leg', around: 'world',
	};
	const parts = {};
	Object.keys(map).forEach((id, i) => {
		const p = Core.REF_BY_ID[id].pivot;
		const r = (v) => v.map((x) => Core.round(x));
		parts[id] = [group('part_' + id, r([p[0] + 0.5, p[1] + 1, p[2]]), [cube(r([p[0] - 1, p[1], p[2] - 1]), r([p[0] + 1, p[1] + 2, p[2] + 1 + i]))])];
	});
	const b = compile(onPlayer('back', parts));
	for (const id of Object.keys(map)) {
		const bone = b.model.bones.find((x) => x.name === 'part_' + id);
		const p = Core.REF_BY_ID[id].pivot;
		assert.equal(bone.attach, map[id], id + ' -> ' + map[id]);
		assert.equal(bone.parent, null);
		assert.deepEqual(bone.pivot, [p[0] + 0.5, p[1] + 1, p[2]].map((x) => Core.round(x)), id + ': pivot stays absolute');
		assert.deepEqual(bone.cubes[0].from, [p[0] - 1, p[1], p[2] - 1].map((x) => Core.round(x)), id + ': cube stays absolute');
	}
	assert.deepEqual(specValidate(b).errors, []);
	// the player groups themselves are never bones
	assert.ok(!b.model.bones.some((x) => Core.REF_BY_ID[x.name] || /◆/.test(x.name)));
	// a child group keeps its parent; only the topmost part gets attach
	const nested = compile(onPlayer('wings', { back: [group('wings', [0, 21, 2], [group('wing_right', [1.5, 21, 2.5], [cube([1.5, 12, 2.5], [11.5, 24, 3.5])])])] }));
	assert.deepEqual(nested.model.bones.map((x) => [x.name, x.parent, x.attach]), [['wings', null, 'body'], ['wing_right', 'wings', undefined]]);
});

test('export: cubes placed straight in a player group become a generated "<group>_cubes" bone', () => {
	const b = compile(onPlayer('back', { back: [cube([-3, 14, 2], [3, 22, 6]), { type: 'locator', name: 'zip', position: [0, 20, 6] }], right_arm: [cube([4, 12, -2], [8, 14, 2])] }));
	const back = b.model.bones.find((x) => x.name === 'back_cubes');
	assert.deepEqual([back.attach, back.pivot, back.generated, back.cubes.length], ['body', [0, 24, 2], true, 1]);
	assert.deepEqual(b.model.locators.zip, { bone: 'back_cubes', position: [0, 20, 6] });
	assert.deepEqual(b.model.bones.find((x) => x.name === 'right_arm_cubes').attach, 'right_arm');
	// and they come back as loose cubes in the same group
	const snap = Core.decompileBundle(b);
	const tree = (id) => (function find(nodes) {
		for (const n of nodes) {
			if (n.ref === id) return n;
			const c = n.children && find(n.children);
			if (c) return c;
		}
		return null;
	})(snap.nodes);
	assert.deepEqual(tree('back').children.map((n) => n.type + ':' + (n.name || '')), ['cube:c', 'locator:zip']);
	assert.equal(Core.stringify(Core.compileBundle(snap).bundle), Core.stringify(b));
});

test('pets: built beside the player, exported relative to their own origin (same bundle as before)', () => {
	const a = Core.PET_ANCHORS.ground;
	const at = (v) => v.map((x, i) => x + a[i]);
	const pet = group('pet', at([0, 0, 0]), [cube(at([-3, 0, -3]), at([3, 6, 3])), cube(at([-1, 6, -1]), at([1, 8, 1]), { origin: at([0, 7, 0]), rotation: [0, 15, 0] }), { type: 'locator', name: 'nose', position: at([0, 7, -2]) }]);
	const b = compile(onPlayer('pet', { beside: [pet] }, { pet: { follow: 'ground', scale: 1 } }));
	const bone = b.model.bones[0];
	assert.deepEqual([bone.attach, bone.pivot], ['world', [0, 0, 0]]);
	assert.deepEqual(bone.cubes[0].from, [-3, 0, -3]);
	assert.deepEqual(bone.cubes[1].pivot, [0, 7, 0], 'rotated cube pivot shifts too');
	assert.equal(bone.cubes[0].pivot, undefined, 'an unset cube pivot stays unset');
	assert.deepEqual(b.model.locators.nose.position, [0, 7, -2]);
	// import puts it back beside the player
	const snap = Core.decompileBundle(b);
	const back = Core.userParts(snap)[0];
	assert.equal(back.ref, 'beside');
	assert.deepEqual(back.node.origin, a);
	// float pets hover by the shoulder
	assert.deepEqual(Core.refTree('cosmetic', 'pet', { pet: { follow: 'float' } }).byId.beside.origin, [11.2, 24.8, 4]);
});

test('auto-assign: predictable rules (zone of the part centre, floating, around, slot fallback)', () => {
	const at = (from, to, slot, extra) => Core.bestRef(group('p', [0, 0, 0], [cube(from, to)]), Object.assign({ kind: 'cosmetic', slot: slot || 'hat' }, extra));
	assert.equal(at([-4.5, 28, -4.5], [4.5, 34, 4.5]), 'head', 'a beanie sits on the head');
	assert.equal(at([-5, 32, -5], [5, 33, 5]), 'head', 'a brim lying on the head top is a hat');
	assert.equal(at([-3, 36, -3], [3, 37, 3]), 'above_head', 'a halo floats above the head');
	assert.equal(at([-3, 14, 3], [3, 22, 7]), 'back', 'a backpack');
	assert.equal(at([-20, 14, 3], [-2, 22, 4], 'wings'), 'back', 'a wide wing on the back');
	assert.equal(at([5, 24, -2], [8, 26, 2]), 'shoulder_right', 'a pauldron');
	assert.equal(at([-5, 11, -3], [5, 13, 3]), 'hips', 'a belt');
	assert.equal(at([4, 12, -2], [8, 14, 2]), 'right_arm', 'a cuff on the right arm (+X)');
	assert.equal(at([-8, 12, -2], [-4, 14, 2]), 'left_arm');
	assert.equal(at([0, 0, -2], [3.9, 3, 2]), 'right_leg', 'a shoe');
	assert.equal(at([-12, 0, -12], [12, 1, 12]), 'around', 'a ring around the player');
	assert.equal(at([20, 0, 20], [22, 2, 22], 'wings'), 'back', 'far away -> the slot\'s group');
	assert.equal(at([-1, 0, -1], [1, 2, 1], 'pet'), 'beside', 'pets always go beside the player');
	assert.equal(at([-1, 30, -1], [1, 32, 1], 'aura'), 'around', 'auras always go around the player');
	assert.equal(Core.bestRef(group('empty', [0, 0, 0]), { kind: 'cosmetic', slot: 'wings' }), 'back', 'empty part -> the slot\'s group');
	// with an attach (import) only matching groups are candidates
	assert.equal(at([-3, 14, 3], [3, 22, 7], 'back', { attach: 'head' }), 'head');
	assert.equal(at([-3, 14, 3], [3, 22, 7], 'back', { attach: 'world' }), 'around');
	assert.equal(at([-3, 14, 3], [3, 22, 7], 'back', { attach: 'root' }), null, 'cosmetic root stays at the top level');
	assert.equal(Core.bestRef(group('p', [0, 0, 0], [cube([0, 0, 0], [1, 1, 1])]), { kind: 'emote', attach: 'root' }), 'root');
	assert.equal(Core.bestRef(group('p', [0, 0, 0], [cube([4, 12, -2], [8, 14, 2])]), { kind: 'emote' }), 'right_arm');
});

test('import: every starter .vibey lands on the player groups and re-exports byte for byte', () => {
	const files = starterFiles();
	assert.ok(files.length >= 30, 'starter set found (' + files.length + ')');
	const placed = {};
	for (const f of files) {
		const b = JSON.parse(fs.readFileSync(f, 'utf8'));
		const snap = Core.decompileBundle(b);
		const once = Core.stringify(Core.compileBundle(snap).bundle);
		// byte-identical to compiling the old (pre "build on the player") structure
		const legacy = Core.stringify(Core.compileBundle(Core.decompileBundle(b, { legacy: true })).bundle);
		assert.equal(once, legacy, path.basename(f) + ': same bytes as before');
		assert.equal(Core.stringify(Core.compileBundle(Core.decompileBundle(JSON.parse(once))).bundle), once, path.basename(f) + ': stable round trip');
		assert.deepEqual(Core.validateBundle(JSON.parse(once)).errors, [], path.basename(f));
		const parts = Core.userParts(snap);
		assert.ok(parts.every((p) => p.ref || p.node.attach === 'root'), path.basename(f) + ': nothing left outside the player groups');
		parts.forEach((p) => (placed[path.basename(f, '.vibey') + '/' + p.node.name] = p.ref));
	}
	assert.equal(placed['crystal_wings/wing_left'], 'back');
	assert.equal(placed['seal_backpack/backpack'], 'back');
	assert.equal(placed['seal_beanie/beanie'], 'head');
	assert.equal(placed['seal_buddy/seal'], 'beside');
	assert.equal(placed['ember_sparks/ring'], 'around');
	assert.equal(placed['clap/right_hand'], 'right_arm');
	assert.equal(placed['seal_summon/seal'], 'root');
	assert.equal(placed['guitar/guitar'], 'body');
});

test('old projects: root groups with attach (and emote rigs named root/head …) convert into the same bundle', () => {
	for (const f of starterFiles()) {
		const b = JSON.parse(fs.readFileSync(f, 'utf8'));
		const legacy = Core.decompileBundle(b, { legacy: true });
		const want = Core.stringify(Core.compileBundle(legacy).bundle);
		const up = Core.upgradeSnapshot(JSON.parse(JSON.stringify(legacy)));
		assert.ok(up.nodes.some((n) => n.ref), path.basename(f) + ': converted');
		assert.equal(Core.stringify(Core.compileBundle(up).bundle), want, path.basename(f) + ': same bundle after the conversion');
		assert.equal(Core.upgradeSnapshot(up), up, 'converting twice changes nothing');
	}
	// a project made by hand: bones in an order that the groups would change (head, arm, head) keep their order
	const snap = {
		kind: 'cosmetic', meta: { id: 'test:hand', name: { en: 'Hand' }, slot: 'hat' }, textures: [tex], animations: [],
		nodes: [
			group('hat', [0, 24, 0], [cube([-4, 32, -4], [4, 35, 4])], { attach: 'head' }),
			group('band', [5, 22, 0], [cube([4, 13, -2], [8, 15, 2])], { attach: 'right_arm' }),
			group('free', [0, 24, 0], [cube([3, 32, 0], [4, 38, 1])]),
		],
	};
	const want = Core.compileBundle(snap).bundle;
	assert.deepEqual(want.model.bones.map((x) => x.name + ':' + x.attach), ['hat:head', 'band:right_arm', 'free:head']);
	snap.nodes.forEach((n, i) => (n.order = i)); // what the plugin stores when it converts an old project
	const up = Core.upgradeSnapshot(snap);
	assert.deepEqual(Core.userParts(up).map((p) => p.node.name + '@' + p.ref), ['hat@head', 'free@head', 'band@right_arm']);
	assert.equal(Core.stringify(Core.compileBundle(up).bundle), Core.stringify(want), 'order kept by the hint');
	delete up.nodes.find((n) => n.ref === 'head').children[0].order;
});

test('validation: moved / rotated player groups and parts outside them are explained', () => {
	const snap = onPlayer('back', { back: [group('pack', [0, 24, 2], [cube([-3, 14, 2], [3, 22, 6])])] });
	const body = snap.nodes.find((n) => n.ref === 'body');
	body.children.find((n) => n.ref === 'back').origin = [0, 20, 2];
	let r = Core.compileBundle(snap);
	assert.ok(r.errors.some((e) => /^Player group "back" pivot was moved/.test(e)), r.errors.join('\n'));
	assert.equal(Core.friendlyProblem(r.errors[0]).key, 'fp_ref_moved');
	body.children.find((n) => n.ref === 'back').origin = [0, 24, 2];
	body.rotation = [0, 10, 0];
	r = Core.compileBundle(snap);
	assert.ok(r.errors.some((e) => /is rotated/.test(e)), r.errors.join('\n'));
	body.rotation = [0, 0, 0];
	snap.nodes.push(group('stray', [0, 0, 0], [cube([0, 0, 0], [1, 1, 1])]));
	r = Core.compileBundle(snap);
	assert.deepEqual(r.errors, []);
	const w = r.warnings.find((m) => /"stray" is not attached to the player/.test(m));
	assert.ok(w, r.warnings.join('\n'));
	assert.deepEqual(Core.friendlyProblem(w), { key: 'fp_outside', args: ['stray'] });
	assert.equal(r.bundle.model.bones.find((x) => x.name === 'stray').attach, 'body', 'slot default');
});
