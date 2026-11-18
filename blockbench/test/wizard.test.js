'use strict';
/** Unit tests for the wizard presets, templates, guide checks, friendly errors and the EN/DE strings (no Blockbench). */
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const Core = require('../vibey.js');
const { validate: specValidate } = require('./spec-validator');

const SOURCE = fs.readFileSync(path.join(__dirname, '..', 'vibey.js'), 'utf8');
const compileValid = (snap) => {
	const r = Core.compileBundle(snap);
	assert.deepEqual(r.errors, [], 'compile errors');
	assert.deepEqual(Core.validateBundle(r.bundle).errors, [], 'plugin validator');
	assert.deepEqual(specValidate(r.bundle).errors, [], 'independent SPEC validator');
	return r.bundle;
};
const starter = (slot, extra) =>
	Core.starterSnapshot(Object.assign({ kind: slot === 'emote' ? 'emote' : 'cosmetic', slot, id: Core.wizardId('', 'Frosty ' + slot, slot), name: { en: 'Frosty ' + slot, de: 'Frostig' } }, extra));
const findNode = (nodes, name) => {
	for (const n of nodes || []) {
		if (n.name === name) return n;
		const c = findNode(n.children, name);
		if (c) return c;
	}
	return null;
};

test('ids: slug from the name, own namespace, never "vibey"', () => {
	assert.equal(Core.slugify('Frosty Hat!'), 'frosty_hat');
	assert.equal(Core.slugify('  Größte Flügel  '), 'groesste_fluegel');
	assert.equal(Core.slugify('???'), '');
	assert.equal(Core.slugify('x'.repeat(80)).length, 48);
	assert.equal(Core.wizardId('', 'Frosty Hat'), 'creator:frosty_hat');
	assert.equal(Core.wizardId('henny', 'Frosty Hat'), 'henny:frosty_hat');
	assert.equal(Core.wizardId('vibey', 'Frosty Hat'), 'creator:frosty_hat', 'the official namespace is never suggested');
	assert.equal(Core.wizardId('Not valid!', '!!!', 'hat'), 'creator:my_hat');
	assert.equal(Core.checkWizardId('creator:frosty_hat'), '');
	assert.equal(Core.checkWizardId('vibey:frosty_hat'), 'id_reserved');
	assert.equal(Core.checkWizardId('Frosty Hat'), 'invalid_id');
	assert.ok(Core.ID_RE.test(Core.wizardId('', 'Über Cape 2000')));
});

test('wizard: every slot and the emote create a structure that exports and validates', () => {
	for (const slot of Core.SLOTS.concat(['emote'])) {
		const snap = starter(slot, { loop: true, length: 1.5 });
		const b = compileValid(snap);
		assert.equal(b.id, 'creator:frosty_' + slot);
		assert.deepEqual(b.name, { en: 'Frosty ' + slot, de: 'Frostig' });
		if (slot === 'emote') {
			assert.deepEqual(b.emote, { animation: 'main', loop: true, thirdPerson: true, stopOnMove: true, hideHeldItems: true });
			assert.deepEqual(Object.keys(b.animations), ['main']);
			assert.equal(b.animations.main.length, 1.5);
			assert.equal(findNode(snap.nodes, 'root').children.map((c) => c.name).join(), Core.PLAYER_PARTS.join());
			continue;
		}
		const p = Core.SLOT_PRESETS[slot];
		assert.equal(b.cosmetic.slot, slot);
		const tex = b.textures[0];
		assert.deepEqual([tex.width, tex.height], p.texture, slot + ' texture size');
		assert.deepEqual(Core.pngInfo(Core.base64ToBytes(tex.png)), { width: p.texture[0], height: p.texture[1] });
		if (p.textureOnly) {
			// texture-only: no model in the bundle, the preview geometry is editor-only
			assert.equal(b.model, undefined, slot + ' exports no model');
			assert.equal(b.cosmetic[slot].texture, slot);
			assert.ok(snap.nodes.every((n) => n.preview), 'only preview nodes');
			assert.equal(b.animations, undefined);
		} else {
			const root = b.model.bones.find((x) => !x.parent);
			assert.equal(root.name, p.root);
			assert.equal(root.attach, p.attach);
			assert.deepEqual(root.pivot, p.pivot);
			assert.deepEqual(Object.keys(b.animations), p.tracks, slot + ' animation tracks');
			Object.values(b.animations).forEach((a) => assert.equal(a.loop, true));
		}
	}
	// slot specifics
	const wings = compileValid(starter('wings'));
	assert.deepEqual(wings.model.bones.map((x) => [x.name, x.parent, x.attach]), [['wings', null, 'body'], ['wing_right', 'wings', undefined], ['wing_left', 'wings', undefined]]);
	assert.ok(wings.model.bones[1].pivot[0] > 0, 'right wing at +X (Blockbench space)');
	assert.ok(wings.model.bones[0].pivot[2] >= 2, 'wings sit at the back (+Z)');
	assert.deepEqual(compileValid(starter('hat')).cosmetic.hideWithArmor, ['head']);
	assert.deepEqual(compileValid(starter('pet')).cosmetic.pet, { follow: 'ground' });
	assert.equal(starter('pet').meta.showReference, false, 'the reference player would hide the pet at the origin');
	assert.deepEqual(compileValid(starter('aura')).model.locators, { aura_center: { bone: 'aura', position: [0, 16, 0] } });
	assert.equal(compileValid(starter('emote', { loop: false })).emote.loop, false);
});

test('wizard: cape / shield preview shows the visible side of the vanilla layout', () => {
	const cape = Core.previewNodes('cape')[0];
	const cube = cape.children[0];
	assert.deepEqual(cube.faces.north.uv, [1, 1, 11, 17], 'outside of the vanilla cape');
	assert.equal(cape.rotation[1], 180, 'turned around so the outside faces backwards');
	// after the 180° turn around z = 2 the cape (z 1..2) ends up at z 2..3, right behind the back
	assert.deepEqual([4 - cube.to[2], 4 - cube.from[2]], [2, 3]);
	const shield = Core.previewNodes('shield')[0];
	assert.deepEqual(shield.children[0].faces.north.uv, [1, 1, 13, 23], 'front of the vanilla shield');
	assert.deepEqual(shield.children[1].uvOffset, [26, 0], 'vanilla handle');
	// the guide colours: visible side purple, inside dark, everything else transparent
	const img = Core.templateImage('cape', 64, 32);
	const px = (x, y) => Array.from(img.data.subarray((y * 64 + x) * 4, (y * 64 + x) * 4 + 4));
	assert.deepEqual(px(5, 8), [0x7a, 0x5c, 0xf0, 255]);
	assert.deepEqual(px(15, 8), [0x3b, 0x2f, 0x6b, 255]);
	assert.equal(px(60, 30)[3], 0);
});

test('PNG encoder writes valid PNGs (stored deflate)', () => {
	const img = Core.templateImage('model', 64, 64);
	const png = Buffer.from(Core.encodePng(img));
	assert.deepEqual(Core.pngInfo(png), { width: 64, height: 64 });
	// decode the IDAT with zlib (checks adler32 + block layout) and compare the pixels
	const idatLen = png.readUInt32BE(33);
	assert.equal(png.toString('ascii', 37, 41), 'IDAT');
	const raw = zlib.inflateSync(png.subarray(41, 41 + idatLen));
	assert.equal(raw.length, 64 * (64 * 4 + 1));
	assert.deepEqual(Array.from(raw.subarray(1, 5)), Array.from(img.data.subarray(0, 4)));
	assert.equal(zlib.crc32 ? zlib.crc32(png.subarray(37, 41 + idatLen)) : png.readUInt32BE(41 + idatLen), png.readUInt32BE(41 + idatLen));
});

test('templates: every embedded starter bundle becomes the creator\'s own valid item', () => {
	const groups = Object.keys(Core.ASSETS.templates);
	assert.deepEqual(groups.sort(), Core.SLOTS.concat(['emote']).sort(), 'templates for every slot and for emotes');
	for (const g of groups) {
		for (const t of Core.ASSETS.templates[g]) {
			assert.equal(t.bundle.id.split(':')[0], 'vibey');
			const snap = Core.templateSnapshot(t.bundle, { id: 'creator:my_' + t.key, name: { en: 'My ' + t.key }, author: 'Me', template: t.key });
			const b = compileValid(snap);
			assert.equal(b.id, 'creator:my_' + t.key);
			assert.deepEqual(b.name, { en: 'My ' + t.key });
			assert.equal(b.author, 'Me');
			assert.ok(!b.tags.includes('starter'));
			assert.equal(b.kind, g === 'emote' ? 'emote' : 'cosmetic');
			if (g !== 'emote') assert.equal(b.cosmetic.slot, g);
			assert.equal(snap.meta.wizard.template, t.key);
			// same model / animations as the starter item
			assert.deepEqual(b.animations, t.bundle.animations);
			assert.deepEqual(b.model, t.bundle.model);
			assert.ok(Core.ASSETS.thumbs[t.key], 'thumbnail for ' + t.key);
		}
	}
	for (const slot of Core.SLOTS.concat(['cosmetic', 'emote'])) assert.ok(Core.ASSETS.thumbs[Core.ASSETS.cards[slot]], 'card picture for ' + slot);
	Object.values(Core.ASSETS.thumbs).forEach((b64) => assert.deepEqual(Core.pngInfo(Core.base64ToBytes(b64)), { width: 64, height: 64 }));
	const size = JSON.stringify(Core.ASSETS).length;
	assert.ok(size < 400 * 1024, `embedded assets stay small (${Math.round(size / 1024)} KiB)`);
});

test('guide: automatic checks follow the project', () => {
	const snap = starter('wings');
	let c = Core.tutorialChecks(snap, { textureUntouched: true, linkState: 'off' });
	assert.deepEqual(Object.keys(c), Core.TUTORIAL_STEPS);
	assert.deepEqual([c.model.state, c.model.key, c.model.args], ['todo', 'chk_model_empty', ['wings']]);
	assert.deepEqual([c.texture.state, c.texture.key], ['todo', 'chk_tex_template']);
	assert.deepEqual([c.animate.state, c.animate.key, c.animate.args], ['todo', 'chk_anim_empty', ['idle']]);
	assert.equal(c.effects.state, 'optional');
	assert.deepEqual([c.live.state, c.live.key], ['todo', 'chk_live_off']);
	assert.deepEqual([c.export.state, c.export.key], ['todo', 'chk_export_ready']);
	// model a wing, paint, animate, add a particle
	const wing = findNode(snap.nodes, 'wing_right');
	wing.children.push({ type: 'cube', name: 'feather', from: [1, 12, 2], to: [9, 22, 3], boxUv: true, uvOffset: [0, 0], faces: {} });
	snap.animations[0].bones.wing_right = { rotation: [{ time: 0, points: [{ x: 0, y: 0, z: 0 }] }, { time: 1, points: [{ x: 0, y: 20, z: 0 }] }] };
	snap.animations[0].effects.particle.push({ time: 0.5, points: [{ effect: 'minecraft:heart' }] });
	c = Core.tutorialChecks(snap, { textureUntouched: false, linkState: 'connected' });
	assert.deepEqual([c.model.state, c.model.args], ['ok', [1]]);
	assert.deepEqual([c.texture.state, c.texture.key], ['ok', 'chk_tex_ok']);
	assert.deepEqual([c.animate.state, c.animate.args], ['ok', [2, 'idle']]);
	assert.deepEqual([c.effects.state, c.effects.args], ['ok', [1]]);
	assert.deepEqual([c.live.state, c.live.key], ['todo', 'chk_live_connected']);
	c = Core.tutorialChecks(snap, { pushed: true, errors: 2 });
	assert.equal(c.live.state, 'ok');
	assert.deepEqual([c.export.state, c.export.args], ['error', [2]]);
	assert.equal(Core.tutorialChecks(snap, { exported: true }).export.key, 'chk_export_done');
	assert.equal(Core.tutorialChecks(snap, { submitted: true }).export.key, 'chk_export_submitted');
	// a loose cube is an error in the model step
	snap.nodes.push({ type: 'cube', name: 'stray', from: [0, 0, 0], to: [1, 1, 1] });
	assert.deepEqual(Core.tutorialChecks(snap, {}).model, { state: 'error', key: 'chk_model_loose', args: ['stray'] });
	// cape: nothing to model / animate, texture must be set as cape texture
	const cape = starter('cape');
	c = Core.tutorialChecks(cape, {});
	assert.deepEqual([c.model.key, c.animate.key, c.texture.state], ['chk_model_texture_only', 'chk_anim_not_needed', 'ok']);
	cape.meta.capeTexture = '';
	assert.equal(Core.tutorialChecks(cape, {}).texture.key, 'chk_tex_cape_unset');
	// emote: props and texture optional, main must be animated
	const emote = starter('emote');
	c = Core.tutorialChecks(emote, {});
	assert.deepEqual([c.model.state, c.texture.state, c.animate.key, c.animate.args], ['optional', 'optional', 'chk_anim_empty', ['main']]);
	emote.animations[0].bones.right_arm = { rotation: [{ time: 0, points: [{ x: 0, y: 0, z: 90 }] }] };
	assert.equal(Core.tutorialChecks(emote, {}).animate.state, 'ok');
});

test('friendly errors: validator messages map to plain words with a fix', () => {
	const snap = starter('hat');
	snap.meta.id = 'Frost Hat';
	snap.meta.name.en = '';
	snap.nodes.push({ type: 'cube', name: 'stray', from: [0, 0, 0], to: [1, 1, 1] });
	const r = Core.compileBundle(snap);
	const v = Core.validateBundle(r.bundle);
	const msgs = r.errors.concat(v.errors);
	const keys = msgs.map((m) => (Core.friendlyProblem(m) || {}).key);
	assert.ok(keys.includes('fp_id'), msgs.join('\n'));
	assert.ok(keys.includes('fp_name'));
	assert.deepEqual(Core.friendlyProblem(msgs.find((m) => /stray/.test(m))), { key: 'fp_loose', args: ['stray'] });
	assert.deepEqual(Core.friendlyProblem('Player bone "right_arm" pivot was moved to [1, 2, 3]; it must stay [5, 22, 0] (Vibey > Repair player rig).'), { key: 'fp_rig', args: ['right_arm'] });
	assert.deepEqual(Core.friendlyProblem('Two groups are both called "wing". Bone names must be unique.'), { key: 'fp_duplicate', args: ['wing'] });
	assert.equal(Core.friendlyProblem('Animation "idle", bone "x", rotation keyframe at 1s uses an expression (Molang). Only plain numbers are supported.').key, 'fp_molang');
	assert.equal(Core.friendlyProblem('something completely new'), null);
	// every rule key has a translation
	Core.FRIENDLY_RULES.forEach(([, key]) => assert.ok(Core.STRINGS[key], key));
	// live link problems
	assert.equal(Core.linkErrorKind('devlink_missing', ''), 'devlink_missing');
	assert.equal(Core.linkErrorKind('token', ''), 'token');
	assert.equal(Core.linkErrorKind('', 'Sign in to Vibe first'), 'not_signed_in');
	assert.equal(Core.linkErrorKind('timeout', ''), 'timeout');
	assert.equal(Core.linkErrorKind('', 'Game not reachable'), 'unreachable');
	['unreachable', 'devlink_missing', 'bad_devlink', 'token', 'timeout', 'lost', 'not_signed_in', 'game_rejected', 'submit_failed', 'too_big', 'import'].forEach((k) => {
		['title', 'msg', 'fix'].forEach((part) => assert.ok(Core.STRINGS[`fe_${k}_${part}`], `fe_${k}_${part}`));
	});
});

test('strings: English + German for every key, same placeholders, every used key exists', () => {
	const S = Core.STRINGS;
	for (const key of Object.keys(S)) {
		assert.ok(Array.isArray(S[key]) && S[key].length === 2, key);
		const [en, de] = S[key];
		assert.ok(typeof en === 'string' && en.trim(), key + ' en');
		assert.ok(typeof de === 'string' && de.trim(), key + ' de');
		const ph = (s) => (s.match(/%\d/g) || []).sort().join();
		assert.equal(ph(de), ph(en), 'placeholders of ' + key);
		const tags = (s) => (s.match(/<\/?b>/g) || []).join();
		assert.equal(tags(de), tags(en), 'markup of ' + key);
	}
	// keys used in the plugin source: T('key'…), STRINGS['key'] and dynamic families
	const used = new Set();
	for (const m of SOURCE.matchAll(/\bT\('([a-z0-9_]+)'/g)) used.add(m[1]);
	for (const m of SOURCE.matchAll(/key: '([a-z0-9_]+)'/g)) used.add(m[1].startsWith('chk_') ? m[1] : 'fe_' + m[1] + '_title');
	Core.SLOTS.forEach((s) => (used.add('slot_' + s), used.add('slot_' + s + '_desc')));
	Core.ATTACH.forEach((a) => used.add('attach_' + a));
	Core.RARITIES.forEach((r) => used.add('rarity_' + r));
	['off', 'connecting', 'connected', 'error'].forEach((s) => used.add('link_short_' + s));
	Object.keys(Core.SUBMISSION_STATUS).forEach((s) => used.add('status_' + s));
	['cosmetic', 'emote'].forEach((k) => ['wz_' + k, 'wz_' + k + '_desc', 'format_' + k, 'format_' + k + '_desc'].forEach((x) => used.add(x)));
	// T('prefix_' + x) families are listed above
	const missing = [...used].filter((k) => !k.endsWith('_') && !S[k]);
	assert.deepEqual(missing, [], 'keys used but not translated');
	// the menu names the launcher docs refer to
	assert.deepEqual(S.menu_new, ['New item…', 'Neues Item…']);
	assert.deepEqual(S.menu_tutorial, ['Tutorial', 'Anleitung']);
	assert.deepEqual(S.menu_test_live, ['Test live in game', 'Live im Spiel testen']);
	assert.deepEqual(S.menu_export_submit, ['Export / Submit…', 'Exportieren / Einreichen…']);
});

test('plugin version is the same everywhere', () => {
	const pkg = require('../package.json');
	const lock = require('../package-lock.json');
	assert.equal(Core.VERSION, pkg.version);
	assert.equal(lock.version, pkg.version);
	assert.equal(lock.packages[''].version, pkg.version);
	assert.ok(SOURCE.includes(`Blockbench plugin (v${pkg.version})`), 'header');
});
