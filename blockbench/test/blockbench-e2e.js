#!/usr/bin/env node
'use strict';
/**
 * End-to-end test against the REAL installed Blockbench (Electron), driven over the Chrome DevTools Protocol.
 *
 *   npm run test:blockbench                       # mock game server (SPEC §8) – default
 *   npm run test:blockbench -- --game-dir "%APPDATA%\.minecraft"   # real game: uses <dir>/vibeyclient/devlink.json
 *   npm run test:blockbench -- --devlink C:\path\to\devlink.json   # real game, explicit devlink.json
 *
 * Options: --blockbench <Blockbench.exe>  --keep-open  --no-screenshots  --lang en|de
 *          --docs-screenshots   write the screenshots to docs/screenshots (default: test/out/screenshots)
 *
 * Blockbench runs with an isolated, temporary profile (--userData / --user-data-dir), so the user's own
 * Blockbench settings, plugins and recent projects are never touched. Only the process started here is closed.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const assert = require('assert/strict');
const Core = require('../vibey.js');
const { validate: specValidate } = require('./spec-validator');
const { createMockGame } = require('./mock-game-server');
const { fakeOgg, pngB64 } = require('./fixtures');
const { launchBlockbench, sleep } = require('./bb-harness');

const args = process.argv.slice(2);
const flag = (n) => args.includes('--' + n);
const opt = (n, d) => (args.indexOf('--' + n) >= 0 ? args[args.indexOf('--' + n) + 1] : d);
const ROOT = path.resolve(__dirname, '..');
const REPO = path.resolve(ROOT, '..');
const PLUGIN = path.join(ROOT, 'vibey.js');
const OUT = path.join(__dirname, 'out');
const SHOTS = flag('docs-screenshots') ? path.join(REPO, 'docs', 'screenshots') : path.join(OUT, 'screenshots');
const LANG = opt('lang', 'en');
const DE = LANG === 'de';
let realGameDir = opt('game-dir');
if (opt('devlink')) realGameDir = path.dirname(path.dirname(path.resolve(opt('devlink'))));
const REAL = !!realGameDir;

const results = [];
function check(name, fn) {
	return Promise.resolve()
		.then(fn)
		.then(
			(v) => (results.push({ name, ok: true }), console.log('  \u2714 ' + name), v),
			(e) => {
				results.push({ name, ok: false, error: String((e && e.stack) || e) });
				console.log('  \u2716 ' + name + '\n      ' + String((e && e.message) || e).split('\n').join('\n      '));
			}
		);
}
async function screenshot(cdp, name) {
	if (flag('no-screenshots')) return null;
	await sleep(400);
	fs.mkdirSync(SHOTS, { recursive: true });
	const file = path.join(SHOTS, name);
	fs.writeFileSync(file, await cdp.capture());
	console.log('    screenshot ' + file);
	return file;
}

// ----------------------------------------------------- page-side helpers
const page = {
	ready: () => typeof BBPlugin !== 'undefined' && typeof Formats !== 'undefined' && !!document.getElementById('status_bar') && typeof Interface !== 'undefined' && !!Interface.Panels,
	loadPlugin: async (file) => {
		if (window.VibeyPlugin) return 'already';
		await new Plugin().loadFromFile({ path: file, name: 'vibey.js', content: '' }, false);
		return !!window.VibeyPlugin;
	},
	makeTextures: async () => {
		const paint = (w, h, fn) => {
			const c = document.createElement('canvas');
			c.width = w;
			c.height = h;
			const ctx = c.getContext('2d');
			for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
				ctx.fillStyle = fn(x, y);
				ctx.fillRect(x, y, 1, 1);
			}
			return c.toDataURL('image/png');
		};
		const main = new Texture({ name: 'main', uv_width: 64, uv_height: 64 })
			.fromDataURL(paint(64, 64, (x, y) => `hsl(${(x * 5 + y * 3) % 360}, 70%, ${45 + ((x ^ y) & 3) * 5}%)`))
			.add(false);
		const glow = new Texture({ name: 'glow', uv_width: 16, uv_height: 16, frame_time: 2 })
			.fromDataURL(paint(16, 64, (x, y) => `hsl(${Math.floor(y / 16) * 80 + 170}, 90%, ${55 + (x % 4) * 5}%)`))
			.add(false);
		for (let i = 0; i < 100 && !(main.width && glow.width); i++) await new Promise((r) => setTimeout(r, 50));
		return { main: [main.width, main.height], glow: [glow.width, glow.height, glow.frameCount] };
	},
};

async function main() {
	fs.mkdirSync(OUT, { recursive: true });
	const gameDir = REAL ? realGameDir : fs.mkdtempSync(path.join(os.tmpdir(), 'vibey-game-'));
	let game = null;
	if (!REAL) {
		game = await createMockGame({ player: 'E2EPlayer', game: '1.21.11' });
		game.writeDevlink(gameDir);
		console.log(`Mock game on 127.0.0.1:${game.port}`);
	} else {
		assert.ok(fs.existsSync(Core.devlinkPath(gameDir, path.sep)), 'devlink.json not found in ' + gameDir);
	}
	let bb = null;
	let cdp = null;
	const shutdown = async () => {
		if (bb) await bb.close();
		if (!flag('keep-open') && !REAL) fs.rmSync(gameDir, { recursive: true, force: true });
		if (game) await game.close();
	};

	try {
		bb = await launchBlockbench({ lang: LANG, exe: opt('blockbench'), keepOpen: flag('keep-open') });
		cdp = bb.cdp;
		console.log(`Blockbench pid ${bb.proc.pid}, isolated profile ${bb.profile}\nGame dir: ${gameDir}${REAL ? ' (REAL GAME)' : ' (mock)'}`);
		assert.equal(await cdp.eval('Language.code'), LANG, 'UI language (--lang) not applied');
		const bbVersion = await cdp.eval('Blockbench.version');
		console.log('Blockbench ' + bbVersion);

		console.log('\nPlugin');
		await check('plugin loads from file and registers formats/codec', async () => {
			assert.ok(await cdp.run(page.loadPlugin, PLUGIN));
			await cdp.waitFor(() => window.VibeyPlugin && Formats.vibey_cosmetic && Formats.vibey_emote && Codecs.vibey && MenuBar.menus.vibey);
			const info = await cdp.eval(`({ v: VibeyPlugin.VERSION, author: Plugins.registered.vibey.author, variant: Plugins.registered.vibey.variant, exp: !!BarItems.vibey_export, imp: !!BarItems.vibey_import })`);
			assert.deepEqual(info, { v: Core.VERSION, author: 'Vibe Craft Studios', variant: 'desktop', exp: true, imp: true });
		});
		await check('first start: the wizard opens by itself, once (remembered in local storage)', async () => {
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_wizard', 8000);
			const r = await cdp.eval(`({ welcomed: VibeyPlugin.prefs().welcomed, welcome: !!document.querySelector('#vibey_wizard .vw_welcome'), cards: [...document.querySelectorAll('#vibey_wizard .vw_card')].map((c) => c.getAttribute('data-value')) })`);
			assert.deepEqual(r, { welcomed: true, welcome: true, cards: ['cosmetic', 'emote'] });
			await cdp.eval('Dialog.open.close(), true');
		});
		await check('loading the plugin file a second time keeps a single instance', async () => {
			await cdp.run(async (file) => {
				await new Plugin().loadFromFile({ path: file, name: 'vibey.js', content: '' }, false);
				return true;
			}, PLUGIN);
			await sleep(500);
			const r = await cdp.eval(`({ panels: Object.keys(Panels).filter((k) => k === 'vibey_panel' || k === 'vibey_guide').length + (document.querySelectorAll('.vibey_panel').length > 1 ? 10 : 0), status: document.querySelectorAll('#vibey_status').length, formats: !!Formats.vibey_cosmetic && !!Formats.vibey_emote, menus: Object.keys(MenuBar.menus).filter((k) => k === 'vibey').length, wizard: Dialog.open ? Dialog.open.id : null })`);
			assert.deepEqual(r, { panels: 2, status: 1, formats: true, menus: 1, wizard: null }, 'one panel + one guide, no second welcome wizard');
		});
		await cdp.run((dir) => {
			Object.assign(VibeyPlugin.LinkSettings, { autoFind: true, gameDir: dir, autoPush: false, replayEmote: true });
			VibeyPlugin.saveLinkSettings();
			return true;
		}, gameDir);

		// ================================================================ COSMETIC
		console.log('\nVibey Cosmetic');
		await check('File > New > Vibey Cosmetic opens the wizard on the slot page', async () => {
			await cdp.eval('Formats.vibey_cosmetic.new(), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_wizard');
			const cards = await cdp.eval(`[...document.querySelectorAll('#vibey_wizard .vw_card')].map((c) => c.getAttribute('data-value'))`);
			assert.deepEqual(cards, Core.SLOTS);
			await cdp.eval('Dialog.open.close(), true');
		});
		await check('properties dialog asks for id, names, slot, rarity, tags', async () => {
			await cdp.eval('newProject(Formats.vibey_cosmetic), VibeyPlugin.openPropertiesDialog(true), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_properties');
			await cdp.run(() => {
				Dialog.open.setFormValues({ id: 'e2e:seal_hat', name_en: 'Seal Hat', name_de: 'Robbenhut', desc_en: 'A cozy hat with a wobbly top.', tags: 'seal, hat, e2e', rarity: 'epic', slot: 'hat', hide_head: true });
				return true;
			});
			await screenshot(cdp, 'blockbench-new-cosmetic-dialog.png');
			await cdp.eval('Dialog.open.confirm(), true');
			const meta = await cdp.eval('JSON.parse(JSON.stringify(VibeyPlugin.getMeta()))');
			assert.equal(meta.id, 'e2e:seal_hat');
			assert.deepEqual(meta.name, { en: 'Seal Hat', de: 'Robbenhut' });
			assert.equal(meta.slot, 'hat');
			assert.equal(meta.rarity, 'epic');
			assert.deepEqual(meta.tags, ['seal', 'hat', 'e2e']);
			assert.deepEqual(meta.hideWithArmor, ['head']);
			assert.equal(await cdp.eval('Format.id'), 'vibey_cosmetic');
		});

		await check('build cosmetic: textures (incl. animated strip), groups, cubes, locator, sound, sprite, animation, events', async () => {
			const tex = await cdp.run(page.makeTextures);
			assert.deepEqual(tex, { main: [64, 64], glow: [16, 64, 4] });
			const ogg = fakeOgg(200).toString('base64');
			const r = await cdp.run((oggB64) => {
				const main = Texture.all.find((t) => t.name === 'main');
				const glow = Texture.all.find((t) => t.name === 'glow');
				const hat = new Group({ name: 'hat', origin: [0, 24, 0], vibey_attach: 'head' }).addTo().init();
				new Cube({ name: 'brim', from: [-5, 32, -5], to: [5, 33, 5], box_uv: true, uv_offset: [0, 0] }).addTo(hat).init();
				const top = new Group({ name: 'top', origin: [0, 33, 0], rotation: [0, 10, 0], vibey_emissive: true }).addTo(hat).init();
				const f = (uv) => ({ uv, texture: glow.uuid });
				new Cube({
					name: 'crown', from: [-3, 33, -3], to: [3, 37, 3], inflate: 0.25, origin: [0, 35, 0], rotation: [0, 0, 22.5], box_uv: false,
					faces: { north: f([0, 0, 6, 4]), south: f([6, 0, 12, 4]), east: { uv: [0, 0, 1, 1], texture: null }, west: f([0, 4, 6, 8]), up: f([6, 6, 0, 0]), down: f([12, 0, 6, 6]) },
				}).addTo(top).init();
				new Locator({ name: 'tip', position: [0, 38, 0] }).addTo(top).init();
				const sleeve = new Group({ name: 'sleeve', origin: [5, 22, 0], vibey_attach: 'right_arm', vibey_translucent: true }).addTo().init();
				// exactly on the reference player's right arm (same box + uv as the arm)
				new Cube({ name: 'arm_cover', from: [4, 12, -2], to: [8, 24, 2], box_uv: true, uv_offset: [40, 16] }).addTo(sleeve).init();
				const bytes = Uint8Array.from(atob(oggB64), (c) => c.charCodeAt(0));
				VibeyPlugin.addSoundFiles([{ name: 'pop.ogg', content: bytes.buffer }]);
				VibeyPlugin.getMeta().sprites.push({ id: 'sparkle', texture: 'main', uv: [0, 48, 8, 56] });
				const anim = new Animation({ name: 'idle', loop: 'loop', length: 2 }).add(false);
				const ba = anim.getBoneAnimator(top);
				ba.addKeyframe({ channel: 'rotation', time: 0, interpolation: 'catmullrom', data_points: [{ x: 0, y: 0, z: 0 }] });
				ba.addKeyframe({ channel: 'rotation', time: 1, interpolation: 'catmullrom', data_points: [{ x: 0, y: 45, z: 0 }] });
				ba.addKeyframe({ channel: 'rotation', time: 2, interpolation: 'linear', data_points: [{ x: 0, y: 0, z: 0 }] });
				ba.addKeyframe({ channel: 'position', time: 1, interpolation: 'linear', data_points: [{ x: 0, y: 1.5, z: 0 }] });
				ba.addKeyframe({ channel: 'scale', time: 0.5, interpolation: 'step', data_points: [{ x: 1, y: 1, z: 1 }, { x: 1.2, y: 1.2, z: 1.2 }] });
				const fx = (anim.animators.effects = new EffectAnimator(anim));
				fx.addKeyframe({ channel: 'particle', time: 0.5, data_points: [
					{ effect: 'heart', locator: 'tip', vibey_count: '3', vibey_spread: '0.2', vibey_velocity: '0 0.05 0' },
					{ vibey_sprite: 'sparkle', locator: 'tip', vibey_count: '4', vibey_lifetime: '0.8', vibey_size: '0.15', vibey_gravity: '-0.01', vibey_emissive: true },
				] });
				fx.addKeyframe({ channel: 'sound', time: 0, data_points: [{ effect: 'minecraft:entity.player.levelup', vibey_volume: '1', vibey_pitch: '1.2' }] });
				fx.addKeyframe({ channel: 'sound', time: 1, data_points: [{ effect: 'pop', vibey_volume: '0.8' }] });
				fx.addKeyframe({ channel: 'timeline', time: 1.2, data_points: [{ vibey_bone: 'top', vibey_visible: false }] });
				fx.addKeyframe({ channel: 'timeline', time: 1.8, data_points: [{ vibey_bone: 'top', vibey_visible: true }] });
				Canvas.updateAll();
				return { groups: Group.all.length, cubes: Cube.all.length, sounds: VibeyPlugin.getMeta().sounds.map((s) => s.id), fx: !!anim.animators.effects };
			}, ogg);
			assert.deepEqual(r, { groups: 3, cubes: 3, sounds: ['pop'], fx: true });
		});

		await check('reference player: toggleable, and the right-arm cube coincides with the reference arm (north = -Z)', async () => {
			const r = await cdp.run(() => {
				VibeyPlugin.Reference.ensure();
				const ref = Project.model_3d.children.find((c) => c.name === 'vibey_reference');
				const refArm = ref.getObjectByName('vibey_ref_right_arm');
				const cube = Cube.all.find((c) => c.name === 'arm_cover');
				Project.model_3d.updateMatrixWorld(true);
				const b1 = new THREE.Box3().setFromObject(refArm);
				const b2 = new THREE.Box3().setFromObject(cube.mesh);
				const round = (v) => v.toArray().map((n) => Math.round(n * 1000) / 1000);
				// world-space normal of the cube's "north" face (face index 5 in Canvas.face_order)
				const nIdx = Canvas.face_order.indexOf('north');
				const pos = cube.mesh.geometry.attributes.position;
				const zs = [0, 1, 2, 3].map((j) => new THREE.Vector3().fromBufferAttribute(pos, nIdx * 4 + j).applyMatrix4(cube.mesh.matrixWorld).z);
				const headNorth = ref.getObjectByName('vibey_ref_head').geometry.attributes.position;
				const headZ = [0, 1, 2, 3].map((j) => headNorth.getZ(nIdx * 4 + j));
				const scene = Project.model_3d.position.toArray();
				VibeyPlugin.setReferenceVisible(false);
				const hidden = ref.visible;
				VibeyPlugin.setReferenceVisible(true);
				return { refBox: [round(b1.min), round(b1.max)], cubeBox: [round(b2.min), round(b2.max)], northZ: zs, headZ, scene, hidden, shown: ref.visible, sleevePivot: Group.all.find((g) => g.name === 'sleeve').origin.slice() };
			});
			assert.deepEqual(r.refBox, r.cubeBox, 'cube on the right arm must coincide with the reference right arm');
			assert.deepEqual(r.cubeBox, [[4, 12, -2], [8, 24, 2]].map((v) => v.map((n, i) => n + r.scene[i])));
			assert.ok(r.northZ.every((z) => Math.abs(z - (-2 + r.scene[2])) < 1e-6), 'north face is the -Z face: ' + r.northZ);
			assert.ok(r.headZ.every((z) => z === -4), 'reference head front (eyes, north) is at -Z');
			assert.deepEqual(r.sleevePivot, [5, 22, 0]);
			assert.equal(r.hidden, false);
			assert.equal(r.shown, true);
		});

		await check('element panel shows attach/emissive/translucent for root groups', async () => {
			const r = await cdp.run(async () => {
				Modes.options.edit.select();
				const g = Group.all.find((x) => x.name === 'sleeve');
				g.select();
				updateSelection();
				await new Promise((res) => setTimeout(res, 300));
				const form = Panels.element.form;
				return { attach: form.getResult().group__vibey_attach, translucent: form.getResult().group__vibey_translucent };
			});
			assert.deepEqual(r, { attach: 'right_arm', translucent: true });
			const panels = await cdp.eval(`[...document.querySelectorAll('.vibey_panel')].map(e => { const p = []; for (let n = e; n && p.length < 6; n = n.parentElement) p.push(n.id || n.className); return p.join(' < '); })`);
			assert.equal(panels.length, 1, 'Vibey panel must exist once: ' + JSON.stringify(panels));
			await cdp.eval(`Preview.selected.loadAnglePreset({position: [55, 42, -60], target: [0, 18, 0]}), true`);
			// show the Element tab (Attach / Glowing / Translucent) for the screenshot
			await cdp.eval(`(() => { try { const host = Panels.element.getHostPanel(); host.selectTab(Panels.element); } catch (e) {} return true; })()`);
			await screenshot(cdp, 'blockbench-cosmetic-edit.png');
		});

		let cosmeticJson, cosmeticBundle;
		await check('export .vibey: valid against SPEC (plugin validator + independent validator), coordinates as SPEC', async () => {
			const r = await cdp.eval('(() => { const r = VibeyPlugin.compileProjectJSON(); return { json: r.json, errors: r.errors, warnings: r.warnings }; })()');
			assert.deepEqual(r.errors, []);
			cosmeticJson = r.json;
			cosmeticBundle = JSON.parse(r.json);
			fs.writeFileSync(path.join(OUT, 'cosmetic.vibey'), r.json);
			assert.deepEqual(Core.validateBundle(cosmeticBundle).errors, []);
			assert.deepEqual(specValidate(cosmeticBundle).errors, []);
			const b = cosmeticBundle;
			assert.equal(b.id, 'e2e:seal_hat');
			assert.equal(b.cosmetic.slot, 'hat');
			assert.equal(b.textures.length, 2);
			assert.deepEqual(b.textures[1].animation, { frameTime: 2, frames: [0, 1, 2, 3] });
			const sleeve = b.model.bones.find((x) => x.name === 'sleeve');
			assert.deepEqual(sleeve, { name: 'sleeve', parent: null, attach: 'right_arm', pivot: [5, 22, 0], rotation: [0, 0, 0], cubes: [{ from: [4, 12, -2], to: [8, 24, 2], name: 'arm_cover', uv: { box: [40, 16] } }], emissive: false, translucent: true });
			const crown = b.model.bones.find((x) => x.name === 'top').cubes[0];
			assert.deepEqual(crown.uv.north, { uv: [0, 0, 6, 4], texture: 'glow' });
			assert.equal(crown.uv.east, undefined);
			assert.deepEqual(crown.pivot, [0, 35, 0]);
			assert.deepEqual(crown.rotation, [0, 0, 22.5]);
			const idle = b.animations.idle;
			assert.deepEqual(idle.bones.top.rotation, [{ t: 0, v: [0, 0, 0], i: 'catmullrom' }, { t: 1, v: [0, 45, 0], i: 'catmullrom' }, { t: 2, v: [0, 0, 0] }]);
			assert.deepEqual(idle.bones.top.scale, [{ t: 0.5, v: [1.2, 1.2, 1.2], i: 'step', pre: [1, 1, 1] }]);
			assert.deepEqual(idle.events.find((e) => e.sprite), { t: 0.5, type: 'particle', sprite: 'sparkle', locator: 'tip', count: 4, lifetime: 0.8, size: 0.15, gravity: -0.01, emissive: true });
			assert.deepEqual(idle.events.filter((e) => e.type === 'sound').map((e) => e.sound), ['minecraft:entity.player.levelup', 'pop']);
			assert.deepEqual(idle.events.filter((e) => e.type === 'visibility'), [{ t: 1.2, type: 'visibility', bone: 'top', visible: false }, { t: 1.8, type: 'visibility', bone: 'top', visible: true }]);
			assert.deepEqual(b.model.locators, { tip: { bone: 'top', position: [0, 38, 0] } });
		});

		await check('validation blocks broken projects with readable messages', async () => {
			const r = await cdp.run(() => {
				const meta = VibeyPlugin.getMeta();
				const id = meta.id;
				meta.id = 'Not Valid';
				const loose = new Cube({ name: 'loose', from: [0, 0, 0], to: [1, 1, 1] }).addTo().init();
				const res = VibeyPlugin.buildBundle();
				VibeyPlugin.validateNow();
				return { errors: res.errors, restore: (meta.id = id, loose.remove(), true) };
			});
			assert.ok(r.errors.some((e) => /Id "Not Valid" is invalid/.test(e)), r.errors.join('\n'));
			assert.ok(r.errors.some((e) => /"loose" \(cube\) is not inside a group/.test(e)), r.errors.join('\n'));
			const shown = await cdp.eval(`(() => { const d = document.querySelector('#vibey_problems'); return { items: [...d.querySelectorAll('.vibey_problems li')].map((li) => li.textContent), details: d.querySelector('details pre').textContent, buttons: [...d.querySelectorAll('.button_bar button')].map((b) => b.textContent) }; })()`);
			const T = (k, ...a) => Core.STRINGS[k][DE ? 1 : 0].replace(/%(\d)/g, (m, i) => a[i]);
			assert.ok(shown.items.includes(T('fp_id')), 'friendly id problem: ' + shown.items.join(' | '));
			assert.ok(shown.items.includes(T('fp_loose', 'loose')), shown.items.join(' | '));
			assert.match(shown.details, /Id "Not Valid" is invalid/, 'technical text under Details');
			assert.deepEqual(shown.buttons, [T('fe_open_properties'), 'OK']);
			await screenshot(cdp, 'blockbench-validation.png');
			await cdp.eval('Dialog.open && Dialog.open.cancel(), true');
			assert.deepEqual(await cdp.eval('VibeyPlugin.buildBundle().errors'), []);
		});

		// --------------------------------------------------------- live link
		console.log('\nLive link (' + (REAL ? 'real game' : 'mock game server') + ')');
		await check('connects via devlink.json ("Find game automatically") and shows status', async () => {
			await cdp.eval('VibeyPlugin.Link.connect(), true');
			await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 15000);
			const s = await cdp.eval(`({ text: document.querySelector('#vibey_status').textContent, title: document.querySelector('#vibey_status').title, cls: document.querySelector('#vibey_status').className })`);
			assert.match(s.text, LANG === 'de' ? /Vibey: verbunden/ : /Vibey: connected/);
			assert.match(s.cls, /vibey_state_connected/);
			if (!REAL) assert.match(s.title, /1\.21\.11 \(E2EPlayer\)/);
		});
		await check('"Push to game" delivers the bundle; it validates and equals the exported file', async () => {
			const waitPush = game && game.waitFor('push');
			const res = await cdp.eval('VibeyPlugin.Link.push("equip")');
			assert.equal(res.type, 'pushed', JSON.stringify(res));
			assert.equal(res.ok, true);
			if (game) {
				const p = await waitPush;
				assert.equal(p.action, 'equip');
				assert.equal(p.ok, true, p.errors.join('; '));
				assert.deepEqual(specValidate(p.bundle).errors, []);
				assert.deepEqual(p.bundle, cosmeticBundle);
				assert.equal(game.state.hellos[0].client, 'blockbench-vibey');
				assert.equal(game.state.hellos[0].version, Core.VERSION);
			}
		});
		await check('auto-push on change is debounced (~300 ms) and sends the edited model', async () => {
			const before = game ? game.state.pushes.length : 0;
			const waitPush = game && game.waitFor('push', (p) => p.bundle.model.bones[0].cubes[0].to[1] === 34.5);
			await cdp.run(async () => {
				VibeyPlugin.LinkSettings.autoPush = true;
				const brim = Cube.all.find((c) => c.name === 'brim');
				for (const y of [33.5, 34, 34.5]) {
					Undo.initEdit({ elements: [brim] });
					brim.to[1] = y;
					brim.preview_controller.updateGeometry(brim);
					Undo.finishEdit('e2e move');
					await new Promise((r) => setTimeout(r, 60));
				}
				return true;
			});
			const t0 = Date.now();
			if (game) {
				await waitPush;
				assert.equal(game.state.pushes.length - before, 1, 'three quick edits must result in exactly one push');
				assert.ok(Date.now() - t0 < 3000);
			} else await sleep(1500);
			await cdp.run(() => {
				VibeyPlugin.LinkSettings.autoPush = false;
				Undo.undo();
				Undo.undo();
				Undo.undo();
				return true;
			});
		});
		await check('older game (welcome "mod" < 0.3.0): the push still works and the plugin names what the game ignores', async () => {
			// real game: whatever it is (0.2.1 for the compatibility run); mock: pretend to be 0.2.1
			const realMod = await cdp.eval('VibeyPlugin.Link.game && VibeyPlugin.Link.game.mod');
			const mod = REAL ? realMod : '0.2.1';
			if (!REAL) await cdp.eval('VibeyPlugin.Link.game.mod = "0.2.1", true');
			const old = !!Core.parseVersion(mod) && Core.compareVersions(mod, '0.3.0') < 0;
			const r = await cdp.run(async () => {
				const meta = VibeyPlugin.getMeta();
				const before = meta.offsetWhen;
				meta.offsetWhen = [{ slot: 'back', offset: [0, 0, 2] }];
				const ws = VibeyPlugin.Link.ws;
				const orig = ws.send.bind(ws);
				let sent = null;
				ws.send = (t) => ((sent = JSON.parse(t)), orig(t));
				const res = await VibeyPlugin.Link.push('equip');
				ws.send = orig;
				VibeyPlugin.refreshUI();
				await new Promise((ok) => setTimeout(ok, 400));
				const el = document.querySelector('.vibey_compat');
				meta.offsetWhen = before;
				return { res, note: VibeyPlugin.Link.compatNote, shown: el ? el.textContent : '', thumbnail: !!(sent && sent.thumbnail), offsetWhen: !!(sent && sent.bundle.cosmetic.offsetWhen) };
			});
			assert.equal(r.res.type, 'pushed', JSON.stringify(r.res));
			assert.equal(r.offsetWhen, true, 'offsetWhen is still sent (an older game ignores it)');
			console.log('    game reports Vibey Client ' + mod + (r.thumbnail ? ', push carried a thumbnail' : '') + (r.note ? '\n    note: ' + r.note : ''));
			if (old) {
				assert.ok(r.note.includes('0.3.0') && r.note.includes(mod), r.note);
				assert.match(r.note, /offsetWhen/);
				if (r.thumbnail) assert.match(r.note, DE ? /Vorschaubilder/ : /thumbnails/);
				assert.ok(r.shown.includes(r.note), 'note in the Vibey panel');
				await screenshot(cdp, 'blockbench-older-client-note.png');
			} else assert.equal(r.note, '');
			if (!REAL) await cdp.eval(`VibeyPlugin.Link.game.mod = ${JSON.stringify(realMod)}, VibeyPlugin.Link.compatNote = '', true`);
		});
		if (!REAL) {
			await check('reconnects with backoff after the game restarts', async () => {
				const { port, token } = game;
				await game.close();
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'error' && /Retrying|Neuer Versuch/.test(VibeyPlugin.Link.message), 10000);
				const msg = await cdp.eval('VibeyPlugin.Link.message');
				assert.match(msg, /lost|not reachable|verloren|nicht erreichbar/);
				game = await createMockGame({ port, token, player: 'E2EPlayer' });
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 20000);
			});
			await check('wrong token: clear error, no endless retry', async () => {
				const file = Core.devlinkPath(gameDir, path.sep);
				const good = fs.readFileSync(file, 'utf8');
				fs.writeFileSync(file, JSON.stringify({ port: game.port, token: 'definitely-not-the-right-token', pid: 1 }));
				await cdp.eval('VibeyPlugin.Link.disconnect(), VibeyPlugin.Link.connect(), true');
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'error' && /token/i.test(VibeyPlugin.Link.message), 10000);
				assert.equal(await cdp.eval('VibeyPlugin.Link.desired'), false);
				fs.writeFileSync(file, good);
				await cdp.eval('VibeyPlugin.Link.connect(), true');
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 10000);
			});
			await check('missing devlink.json gives a helpful message', async () => {
				const msg = await cdp.run((dir) => {
					VibeyPlugin.Link.disconnect();
					const old = VibeyPlugin.LinkSettings.gameDir;
					VibeyPlugin.LinkSettings.gameDir = dir;
					try {
						VibeyPlugin.Link.resolveEndpoint();
						return 'no error';
					} catch (e) {
						return e.message;
					} finally {
						VibeyPlugin.LinkSettings.gameDir = old;
						VibeyPlugin.Link.connect();
					}
				}, path.join(gameDir, 'does-not-exist'));
				assert.match(msg, /devlink\.json/);
				assert.match(msg, /Creator mode|Creator-Modus/);
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 10000);
			});
		}
		await cdp.run(() => (VibeyPlugin.openLinkSettings(), true));
		await sleep(300);
		await screenshot(cdp, 'blockbench-live-link-settings.png');
		await cdp.eval('Dialog.open && Dialog.open.close(2), true');

		await check('import .vibey back into an editable project is lossless', async () => {
			const file = path.join(OUT, 'cosmetic.vibey');
			const r = await cdp.run(async (json, p) => {
				VibeyPlugin.importBundle(json, p);
				for (let i = 0; i < 100 && !Texture.all.every((t) => t.width); i++) await new Promise((res) => setTimeout(res, 50));
				const c = VibeyPlugin.compileProjectJSON();
				return { json: c.json, errors: c.errors, format: Format.id, groups: Group.all.map((g) => g.name), tabs: ModelProject.all.length };
			}, cosmeticJson, file);
			assert.deepEqual(r.errors, []);
			assert.equal(r.format, 'vibey_cosmetic');
			assert.deepEqual(r.groups.sort(), ['hat', 'sleeve', 'top']);
			assert.deepEqual(JSON.parse(r.json), cosmeticBundle);
			assert.equal(r.json, cosmeticJson, 'byte-identical re-export');
		});
		await check('re-export keeps an unedited semi-transparent texture byte-exact, painted pixels are still exported', async () => {
			// semi-transparent pixels do not survive a canvas round trip (premultiplied alpha), so the plugin must
			// keep the imported PNG as long as the texture is unchanged
			const bundle = JSON.parse(cosmeticJson);
			bundle.id = 'e2e:alpha_texture';
			const main = bundle.textures.find((t) => t.width === 64 && !t.animation);
			main.png = pngB64(64, 64, (x, y) => [(x * 37) & 255, (y * 53) & 255, (x * y) & 255, (x + y) % 5 === 0 ? 0 : 1 + ((x * 7 + y * 3) % 254)]);
			const json = Core.stringify(bundle);
			const r = await cdp.run(async (json, id) => {
				VibeyPlugin.importBundle(json, 'alpha.vibey');
				for (let i = 0; i < 100 && !Texture.all.every((t) => t.width); i++) await new Promise((res) => setTimeout(res, 50));
				const first = VibeyPlugin.compileProjectJSON().json;
				const tex = Texture.all.find((t) => t.name === id);
				tex.canvas.getContext('2d').fillStyle = '#ff0000';
				tex.canvas.getContext('2d').fillRect(0, 0, 2, 2);
				const second = JSON.parse(VibeyPlugin.compileProjectJSON().json).textures.find((t) => t.id === id).png;
				await Project.close(true);
				return { first, second };
			}, json, main.id);
			assert.ok(r.first === json, 'byte-identical re-export of the unedited texture');
			assert.notEqual(r.second, main.png, 'an edited texture must be exported from the canvas');
		});

		// ================================================================ EMOTE
		console.log('\nVibey Emote');
		await check('new emote project: dialog + player rig root > head/body/arms/legs with SPEC pivots', async () => {
			await cdp.eval('newProject(Formats.vibey_emote), VibeyPlugin.openPropertiesDialog(true), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_properties');
			await cdp.run(() => {
				Dialog.open.setFormValues({ id: 'e2e:flag_wave', name_en: 'Flag Wave', name_de: 'Fahne schwenken', tags: 'wave, flag', third_person: true, stop_on_move: false, hide_held: true, loop: false });
				return true;
			});
			await screenshot(cdp, 'blockbench-new-emote-dialog.png');
			await cdp.eval('Dialog.open.confirm(), true');
			const rig = await cdp.eval(`Group.all.map(g => ({ name: g.name, parent: g.parent === 'root' ? null : g.parent.name, origin: g.origin.slice() }))`);
			assert.deepEqual(rig, [
				{ name: 'root', parent: null, origin: [0, 0, 0] },
				{ name: 'head', parent: 'root', origin: [0, 24, 0] },
				{ name: 'body', parent: 'root', origin: [0, 24, 0] },
				{ name: 'right_arm', parent: 'root', origin: [5, 22, 0] },
				{ name: 'left_arm', parent: 'root', origin: [-5, 22, 0] },
				{ name: 'right_leg', parent: 'root', origin: [1.9, 12, 0] },
				{ name: 'left_leg', parent: 'root', origin: [-1.9, 12, 0] },
			]);
			const meta = await cdp.eval('JSON.parse(JSON.stringify(VibeyPlugin.getMeta()))');
			assert.equal(meta.id, 'e2e:flag_wave');
			assert.equal(meta.emote.stopOnMove, false);
			assert.equal(await cdp.eval('Animator.animations.map(a => a.name).join()'), 'main');
		});
		await check('build emote: prop on right arm, cube directly in a player bone, locator, keyframes, events', async () => {
			await cdp.run(page.makeTextures);
			const r = await cdp.run(() => {
				const arm = Group.all.find((g) => g.name === 'right_arm');
				const flag = new Group({ name: 'flag', origin: [6, 12, 0] }).addTo(arm).init();
				new Cube({ name: 'pole', from: [5.5, 2, -0.5], to: [6.5, 14, 0.5], box_uv: true, uv_offset: [0, 0] }).addTo(flag).init();
				new Cube({ name: 'cloth', from: [6, 9, 0], to: [6, 14, 8], box_uv: false, faces: { east: { uv: [0, 0, 8, 5] }, west: { uv: [8, 0, 0, 5] }, north: { uv: [0, 0, 0, 0], texture: null }, south: { uv: [0, 0, 0, 0], texture: null }, up: { uv: [0, 0, 0, 0], texture: null }, down: { uv: [0, 0, 0, 0], texture: null } } }).addTo(flag).init();
				new Cube({ name: 'glove', from: [3.5, 11.5, -2.5], to: [8.5, 14.5, 2.5], box_uv: true, uv_offset: [16, 16] }).addTo(arm).init();
				new Locator({ name: 'flag_tip', position: [6, 14, 8] }).addTo(flag).init();
				const anim = Animator.animations[0];
				anim.length = 2;
				const ra = anim.getBoneAnimator(arm);
				ra.addKeyframe({ channel: 'rotation', time: 0, data_points: [{ x: 0, y: 0, z: 0 }] });
				ra.addKeyframe({ channel: 'rotation', time: 0.5, interpolation: 'catmullrom', data_points: [{ x: 0, y: 0, z: 160 }] });
				ra.addKeyframe({ channel: 'rotation', time: 1, interpolation: 'catmullrom', data_points: [{ x: 0, y: 0, z: 130 }] });
				ra.addKeyframe({ channel: 'rotation', time: 2, data_points: [{ x: 0, y: 0, z: 0 }] });
				const root = anim.getBoneAnimator(Group.all.find((g) => g.name === 'root'));
				root.addKeyframe({ channel: 'position', time: 0, interpolation: 'step', data_points: [{ x: 0, y: 0, z: 0 }] });
				root.addKeyframe({ channel: 'position', time: 1, data_points: [{ x: 0, y: 1, z: 0 }] });
				const head = anim.getBoneAnimator(Group.all.find((g) => g.name === 'head'));
				head.addKeyframe({ channel: 'rotation', time: 0.5, data_points: [{ x: -15, y: 20, z: 0 }] });
				const fa = anim.getBoneAnimator(Group.all.find((g) => g.name === 'flag'));
				fa.addKeyframe({ channel: 'rotation', time: 0.75, data_points: [{ x: 0, y: 25, z: 0 }] });
				const fx = (anim.animators.effects = new EffectAnimator(anim));
				fx.addKeyframe({ channel: 'particle', time: 0.5, data_points: [{ effect: 'minecraft:note', locator: 'flag_tip', vibey_count: '2', vibey_spread: '0.3 0.3 0.3' }] });
				fx.addKeyframe({ channel: 'sound', time: 0.4, data_points: [{ effect: 'minecraft:entity.player.attack.sweep', vibey_pitch: '1.4' }] });
				fx.addKeyframe({ channel: 'timeline', time: 1.9, data_points: [{ vibey_bone: 'flag', vibey_visible: false }] });
				Canvas.updateAll();
				return Group.all.length;
			});
			assert.equal(r, 8);
		});
		await check('animate mode: reference cubes follow the animated player bones (screenshot)', async () => {
			const r = await cdp.run(async () => {
				Modes.options.animate.select();
				const anim = Animator.animations[0];
				anim.select();
				Timeline.setTime(0.5);
				Animator.preview();
				await new Promise((res) => setTimeout(res, 200));
				const arm = Group.all.find((g) => g.name === 'right_arm');
				const ref = arm.mesh.children.find((c) => c.name === 'vibey_ref_right_arm');
				Project.model_3d.updateMatrixWorld(true);
				const box = new THREE.Box3().setFromObject(ref);
				Timeline.setTime(1.95);
				Animator.preview();
				const flag = Group.all.find((g) => g.name === 'flag');
				const hiddenAtEnd = flag.mesh.visible === false;
				Timeline.setTime(0.5);
				Animator.preview();
				const visibleAgain = flag.mesh.visible === true;
				const fxKf = anim.animators.effects.particle[0];
				fxKf.select();
				return { refParent: ref.parent === arm.mesh, armTopY: Math.round(box.max.y * 10) / 10, hiddenAtEnd, visibleAgain };
			});
			assert.equal(r.refParent, true);
			assert.equal(r.hiddenAtEnd, true, 'visibility event at 1.9 s hides the flag in the preview');
			assert.equal(r.visibleAgain, true);
			assert.ok(r.armTopY > 30, 'raised arm reaches above the head, got ' + r.armTopY);
			await cdp.eval(`Preview.selected.loadAnglePreset({position: [50, 40, -65], target: [2, 20, 0]}), true`);
			await screenshot(cdp, 'blockbench-emote-animate.png');
		});
		let emoteBundle, emoteJson;
		await check('export emote: player channels + props + events validate against SPEC', async () => {
			const r = await cdp.eval('(() => { const r = VibeyPlugin.compileProjectJSON(); return { json: r.json, errors: r.errors }; })()');
			assert.deepEqual(r.errors, []);
			emoteJson = r.json;
			emoteBundle = JSON.parse(r.json);
			fs.writeFileSync(path.join(OUT, 'emote.vibey'), r.json);
			assert.deepEqual(Core.validateBundle(emoteBundle).errors, []);
			assert.deepEqual(specValidate(emoteBundle).errors, []);
			const b = emoteBundle;
			assert.deepEqual(b.emote, { animation: 'main', loop: false, thirdPerson: true, stopOnMove: false, hideHeldItems: true });
			assert.deepEqual(b.model.bones.map((x) => x.name), ['flag', 'right_arm_cubes']);
			const flagBone = b.model.bones[0];
			assert.equal(flagBone.attach, 'right_arm');
			assert.deepEqual(flagBone.pivot, [6, 12, 0]);
			assert.equal(b.model.bones[1].generated, true);
			assert.deepEqual(b.model.bones[1].pivot, [5, 22, 0]);
			assert.deepEqual(Object.keys(b.animations.main.bones).sort(), ['flag', 'head', 'right_arm', 'root']);
			assert.deepEqual(b.animations.main.bones.right_arm.rotation[1], { t: 0.5, v: [0, 0, 160], i: 'catmullrom' });
			assert.deepEqual(b.model.locators.flag_tip, { bone: 'flag', position: [6, 14, 8] });
			assert.deepEqual(b.animations.main.events.map((e) => e.type), ['sound', 'particle', 'visibility']);
		});
		await check('"Play emote in game" pushes with action "play"', async () => {
			const waitPush = game && game.waitFor('push', (p) => p.bundle.id === 'e2e:flag_wave');
			const res = await cdp.eval('VibeyPlugin.Link.push("play")');
			assert.equal(res.type, 'pushed', JSON.stringify(res));
			if (game) {
				const p = await waitPush;
				assert.equal(p.action, 'play');
				assert.ok(p.ok, p.errors.join('; '));
				assert.deepEqual(p.bundle, emoteBundle);
			}
			await screenshot(cdp, 'blockbench-emote-pushed.png');
		});
		await check('import emote .vibey round trip (player bones, generated cubes back in right_arm)', async () => {
			const r = await cdp.run(async (json) => {
				// the same path as File > Open / drag & drop: Blockbench picks the codec by its load_filter
				loadModelFile({ path: 'emote.vibey', name: 'emote.vibey', content: json });
				for (let i = 0; i < 100 && !Texture.all.every((t) => t.width); i++) await new Promise((res) => setTimeout(res, 50));
				const arm = Group.all.find((g) => g.name === 'right_arm');
				const c = VibeyPlugin.compileProjectJSON();
				return { json: c.json, errors: c.errors, armChildren: arm.children.map((x) => x.name) };
			}, emoteJson);
			assert.deepEqual(r.errors, []);
			assert.deepEqual(r.armChildren.sort(), ['flag', 'glove']);
			assert.equal(r.json, emoteJson);
		});
		await check('closing an emote tab while the visibility preview hides a bone does not throw', async () => {
			const r = await cdp.run(async () => {
				Modes.options.animate.select();
				Animator.animations[0].select();
				Timeline.setTime(1.95);
				Animator.preview();
				const hidden = Group.all.find((g) => g.name === 'flag').mesh.visible === false;
				const before = ModelProject.all.length;
				let error = null;
				try {
					await Project.close(true);
				} catch (e) {
					error = String((e && e.message) || e);
				}
				return { hidden, error, closed: ModelProject.all.length === before - 1 };
			});
			assert.equal(r.hidden, true, 'flag hidden at 1.95 s');
			assert.equal(r.error, null);
			assert.equal(r.closed, true, 'project tab closed');
		});
		await check('remove dev item + disconnect', async () => {
			const removed = game && game.waitFor('remove', (id) => id === 'e2e:flag_wave');
			await cdp.eval('VibeyPlugin.Link.remove("e2e:flag_wave"), true');
			if (game) await removed;
			await cdp.eval('VibeyPlugin.Link.disconnect(), true');
			assert.equal(await cdp.eval('VibeyPlugin.Link.state'), 'off');
		});
		// ================================================================ WIZARD + GUIDE
		console.log('\nWizard, guide, menu, friendly errors');
		const T = (k, ...a) => Core.STRINGS[k][DE ? 1 : 0].replace(/%(\d)/g, (m, i) => a[i]);
		await check('Vibey menu: few clear entries + Advanced submenu, names follow the language', async () => {
			const r = await cdp.run(() => {
				const m = MenuBar.menus.vibey;
				const top = m.structure.map((x) => (typeof x === 'string' ? x : x.id));
				const adv = m.structure.find((x) => x.id === 'vibey_advanced');
				const children = adv.children().map((x) => (typeof x === 'string' ? x : x.id));
				m.open();
				const shown = [...document.querySelectorAll('ul.contextMenu > li')].map((li) => li.textContent.trim()).filter(Boolean);
				const names = ['vibey_wizard', 'vibey_tutorial', 'vibey_test_live', 'vibey_export_submit'].map((id) => BarItems[id].name);
				return { top, children, shown, names, advName: adv.name };
			});
			assert.deepEqual(r.top, ['vibey_wizard', 'vibey_tutorial', '_', 'vibey_test_live', 'vibey_export_submit', '_', 'vibey_advanced']);
			for (const id of ['vibey_properties', 'vibey_import_file', 'vibey_link_settings', 'vibey_add_sound', 'vibey_add_sprite', 'vibey_validate', 'vibey_submissions', 'vibey_about']) assert.ok(r.children.includes(id), id + ' in Advanced');
			assert.deepEqual(r.names, [T('menu_new'), T('menu_tutorial'), T('menu_test_live'), T('menu_export_submit')]);
			assert.equal(r.advName, T('menu_advanced'));
			for (const n of r.names) assert.ok(r.shown.some((t) => t.includes(n)), 'menu shows ' + n + ': ' + r.shown.join(' | '));
			await cdp.eval('open_menu && open_menu.hide(), true');
		});
		let wingsBundle;
		await check('wizard through the real dialog (wings): structure, guide, export validates', async () => {
			// the properties test above remembered the namespace "e2e"; a fresh creator gets "creator:"
			assert.equal(await cdp.eval('VibeyPlugin.prefs().namespace'), 'e2e');
			await cdp.eval("VibeyPlugin.savePrefs({ namespace: '' }), VibeyPlugin.openWizard(), true");
			const click = (sel) => cdp.run((s) => (document.querySelector(s).click(), true), sel);
			await click('#vibey_wizard .vw_card[data-value=cosmetic]');
			await cdp.waitFor(() => !!document.querySelector('#vibey_wizard .vw_card[data-value=wings]'));
			await click('#vibey_wizard .vw_card[data-value=wings]');
			await cdp.waitFor(() => !!document.querySelector('#vibey_wizard input.vw_name_en'));
			// "Next" is refused without a name
			await click('#vibey_wizard .vw_next');
			assert.equal(await cdp.eval(`document.querySelector('#vibey_wizard .vw_error').textContent.trim()`), T('wz_need_name'));
			await cdp.run(() => {
				const set = (sel, v) => {
					const el = document.querySelector(sel);
					el.value = v;
					el.dispatchEvent(new Event('input', { bubbles: true }));
				};
				set('#vibey_wizard input.vw_name_en', 'Frost Wings');
				set('#vibey_wizard input.vw_name_de', 'Frostflügel');
				return true;
			});
			await sleep(100);
			assert.equal(await cdp.eval(`document.querySelector('#vibey_wizard .vw_id').textContent`), 'creator:frost_wings');
			await click('#vibey_wizard .vw_next');
			await cdp.waitFor(() => !!document.querySelector('#vibey_wizard .vw_card[data-value=neon_wings]'));
			await click('#vibey_wizard .vw_next'); // "Create" with "Start empty"
			await cdp.waitFor(() => !Dialog.open && Format.id === 'vibey_cosmetic' && Texture.all.length && Texture.all[0].width, 8000);
			await cdp.waitFor(() => VibeyPlugin.getMeta().wizard && VibeyPlugin.getMeta().wizard.textureHash, 5000);
			const r = await cdp.run(() => {
				const groups = Group.all.map((g) => ({ name: g.name, parent: g.parent === 'root' ? null : g.parent.name, attach: g.vibey_attach, origin: g.origin.slice() }));
				const c = VibeyPlugin.compileProjectJSON();
				return {
					groups, anims: Animator.animations.map((a) => a.name + ':' + a.loop), tex: Texture.all.map((t) => [t.name, t.width, t.height]),
					meta: { id: VibeyPlugin.getMeta().id, name: VibeyPlugin.getMeta().name, slot: VibeyPlugin.getMeta().slot },
					ref: VibeyPlugin.getMeta().showReference, guide: !!document.querySelector('.vibey_guide .vg_step'), errors: c.errors, json: c.json,
				};
			});
			assert.deepEqual(r.groups, [
				{ name: 'wings', parent: null, attach: 'body', origin: [0, 21, 2] },
				{ name: 'wing_right', parent: 'wings', attach: '', origin: [1.5, 21, 2.5] },
				{ name: 'wing_left', parent: 'wings', attach: '', origin: [-1.5, 21, 2.5] },
			]);
			assert.deepEqual(r.anims, ['idle:loop', 'walk:loop', 'fly:loop', 'sneak:loop', 'glide:loop']);
			assert.deepEqual(r.tex, [['main', 64, 64]]);
			assert.deepEqual(r.meta, { id: 'creator:frost_wings', name: { en: 'Frost Wings', de: 'Frostflügel' }, slot: 'wings' });
			assert.equal(r.ref, true);
			assert.equal(r.guide, true, 'the guide panel is shown');
			assert.deepEqual(r.errors, []);
			wingsBundle = JSON.parse(r.json);
			assert.deepEqual(Core.validateBundle(wingsBundle).errors, []);
			assert.deepEqual(specValidate(wingsBundle).errors, []);
			fs.writeFileSync(path.join(OUT, 'wizard-wings.vibey'), r.json);
		});
		await check('guide: checks fire (model, texture, animate, effects, live, export) and steps can be ticked', async () => {
			const state = () => cdp.run(() => (VibeyPlugin.refreshUI(), new Promise((res) => setTimeout(() => res(VibeyPlugin.UI.guide.steps.map((s) => s.id + ':' + s.state + ':' + s.check)), 80))));
			let s = await state();
			assert.deepEqual(s.map((x) => x.split(':').slice(0, 2).join(':')), ['model:todo', 'texture:todo', 'animate:todo', 'effects:optional', 'live:todo', 'export:todo']);
			assert.ok(s[0].endsWith(T('chk_model_empty', 'wings')), s[0]);
			assert.ok(s[1].endsWith(T('chk_tex_template')), s[1]);
			assert.ok(s[2].endsWith(T('chk_anim_empty', 'idle')), s[2]);
			assert.ok(s[4].endsWith(T('chk_live_off')) || s[4].endsWith(T('chk_live_connecting')), s[4]);
			assert.equal(await cdp.eval(`document.querySelector('.vibey_guide .vg_current .vg_check span').textContent.trim()`), T('chk_model_empty', 'wings'));
			await cdp.run(() => {
				const r = Group.all.find((g) => g.name === 'wing_right');
				new Cube({ name: 'feather', from: [1.5, 12, 2.5], to: [11.5, 24, 3.5], box_uv: true, uv_offset: [0, 0] }).addTo(r).init();
				const tex = Texture.all[0];
				const ctx = tex.canvas.getContext('2d');
				ctx.fillStyle = '#3fe0c5';
				ctx.fillRect(0, 0, 16, 16);
				const anim = Animator.animations.find((a) => a.name === 'idle');
				anim.getBoneAnimator(r).addKeyframe({ channel: 'rotation', time: 0, data_points: [{ x: 0, y: 0, z: 0 }] });
				anim.getBoneAnimator(r).addKeyframe({ channel: 'rotation', time: 1, data_points: [{ x: 0, y: -20, z: 0 }] });
				const fx = (anim.animators.effects = new EffectAnimator(anim));
				fx.addKeyframe({ channel: 'particle', time: 0.5, data_points: [{ effect: 'minecraft:snowflake' }] });
				return true;
			});
			s = await state();
			assert.deepEqual(s.map((x) => x.split(':').slice(0, 2).join(':')), ['model:ok', 'texture:ok', 'animate:ok', 'effects:ok', 'live:todo', 'export:todo']);
			// tick + next
			const g = await cdp.run(() => {
				VibeyPlugin.Guide.next(0);
				VibeyPlugin.Guide.tick(1, true);
				VibeyPlugin.refreshUI();
				return new Promise((res) => setTimeout(() => res({ current: VibeyPlugin.UI.guide.current, done: VibeyPlugin.getMeta().tutorial.done }), 80));
			});
			assert.deepEqual(g, { current: 1, done: { model: true, texture: true } });
		});
		await check('"Test live in game": friendly help when the game cannot be found, then it connects and pushes by itself', async () => {
			const missing = path.join(gameDir, 'no-game-here');
			await cdp.run((dir) => {
				VibeyPlugin.Link.disconnect();
				VibeyPlugin.LinkSettings.gameDir = dir;
				return true;
			}, missing);
			await cdp.eval('VibeyPlugin.testLive(), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_friendly', 8000);
			const d = await cdp.eval(`(() => { const d = document.querySelector('#vibey_friendly'); return { key: Dialog.open.vibey_key, title: d.querySelector('.vf_title').textContent, fixes: [...d.querySelectorAll('.vf_fix li')].map((l) => l.textContent), details: d.querySelector('details pre').textContent, buttons: [...d.querySelectorAll('.button_bar button')].map((b) => b.textContent) }; })()`);
			assert.equal(d.key, 'devlink_missing');
			assert.equal(d.title, T('fe_devlink_missing_title'));
			assert.deepEqual(d.fixes, T('fe_devlink_missing_fix').split('\n'));
			assert.ok(d.fixes.some((f) => /Creator/.test(f)), 'explains Creator mode');
			assert.match(d.details, /devlink\.json/);
			assert.deepEqual(d.buttons, [T('fe_try_again'), T('fe_open_settings'), T('fe_close')]);
			const guideHelp = await cdp.run(() => (VibeyPlugin.getMeta().tutorial.step = 4, VibeyPlugin.refreshUI(), new Promise((res) => setTimeout(() => res(!!document.querySelector('.vibey_guide .vg_howto')), 100))));
			assert.equal(guideHelp, true, 'the guide shows the how-to while not connected');
			await screenshot(cdp, 'blockbench-friendly-error.png');
			await cdp.eval('Dialog.open.cancel(), true');
			// the game appears: the plugin keeps looking and sends the queued item
			const waitPush = game && game.waitFor('push', (p) => p.bundle.id === 'creator:frost_wings');
			await cdp.run((dir) => (VibeyPlugin.LinkSettings.gameDir = dir, true), gameDir);
			await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 20000);
			if (game) {
				const p = await waitPush;
				assert.equal(p.action, 'equip');
				assert.ok(p.ok, p.errors.join('; '));
			}
			await cdp.waitFor(() => VibeyPlugin.sessionOf().pushed, 8000);
			const live = await cdp.run(() => (VibeyPlugin.refreshUI(), new Promise((res) => setTimeout(() => res(VibeyPlugin.UI.guide.steps.find((s) => s.id === 'live').state), 80))));
			assert.equal(live, 'ok');
			// and the button again while connected pushes right away
			const res = await cdp.eval('VibeyPlugin.testLive()');
			assert.equal(res.type, 'pushed', JSON.stringify(res));
		});
		await check('Export / Submit dialog; a game rejection is explained in plain words', async () => {
			await cdp.eval('VibeyPlugin.exportSubmitDialog(), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_export_submit');
			const cards = await cdp.eval(`[...document.querySelectorAll('#vibey_export_submit [data-value]')].map((e) => e.getAttribute('data-value'))`);
			assert.deepEqual(cards, ['save', 'submit', 'submissions']);
			await cdp.eval('Dialog.open.cancel(), true');
			// submit through the game (mock, or the real game signed in to a LOCAL backend): the guide marks the export
			// step done; the real run also sends the viewport thumbnail and checks My submissions + withdraw
			const sub = await cdp.run(async (withThumb) => {
				VibeyPlugin.exportSubmitDialog();
				await new Promise((r) => setTimeout(r, 50));
				document.querySelector('#vibey_export_submit [data-value=submit]').click();
				await new Promise((r) => setTimeout(r, 200));
				Dialog.open.setFormValues({ thumbnail: withThumb });
				Dialog.open.confirm();
				for (let i = 0; i < 400 && !VibeyPlugin.sessionOf().submitted && !(Dialog.open && Dialog.open.id === 'vibey_friendly'); i++) await new Promise((r) => setTimeout(r, 50));
				const ok = Dialog.open && Dialog.open.id;
				const text = Dialog.open && Dialog.open.object ? Dialog.open.object.textContent.replace(/\s+/g, ' ').trim() : '';
				if (Dialog.open) Dialog.open.cancel();
				return { submitted: VibeyPlugin.sessionOf().submitted, dialog: ok, text };
			}, REAL);
			assert.equal(sub.submitted, true, JSON.stringify(sub));
			if (game) assert.equal(game.state.submissions.slice(-1)[0].bundleId, 'creator:frost_wings');
			if (REAL) {
				console.log('    submitted: ' + sub.text);
				const list = await cdp.run(async () => {
					VibeyPlugin.exportSubmitDialog();
					await new Promise((r) => setTimeout(r, 50));
					document.querySelector('#vibey_export_submit [data-value=submissions]').click();
					for (let i = 0; i < 400 && !(Dialog.open && /vibey_submissions|vibey_friendly/.test(Dialog.open.id)); i++) await new Promise((r) => setTimeout(r, 50));
					await new Promise((r) => setTimeout(r, 200));
					const rows = [...document.querySelectorAll('.vibey-submissions tr')].map((tr) => tr.textContent.replace(/\s+/g, ' ').trim());
					return { dialog: Dialog.open && Dialog.open.id, rows, withdraw: document.querySelectorAll('.vibey-withdraw').length };
				});
				assert.equal(list.dialog, 'vibey_submissions', JSON.stringify(list));
				assert.ok(list.rows.some((r) => /Frost Wings|frost_wings/i.test(r)), JSON.stringify(list.rows));
				console.log('    my submissions: ' + list.rows.join(' | '));
				await screenshot(cdp, 'blockbench-real-submissions.png');
				const w = await cdp.run(async () => {
					const b = document.querySelector('.vibey-withdraw');
					if (!b) return 'no withdraw button';
					b.click();
					for (let i = 0; i < 200 && !b.disabled; i++) await new Promise((r) => setTimeout(r, 50));
					for (let i = 0; i < 400 && b.textContent === VibeyPlugin.T('withdraw'); i++) await new Promise((r) => setTimeout(r, 50));
					const t = b.textContent;
					Dialog.open && Dialog.open.cancel();
					return t;
				});
				assert.equal(w, T('withdrawn_ok'));
			}
			// the game refuses a push -> friendly dialog with the reason under Details
			const r = await cdp.run(async () => {
				const orig = VibeyPlugin.Link.ws.send.bind(VibeyPlugin.Link.ws);
				VibeyPlugin.Link.ws.send = () => setTimeout(() => VibeyPlugin.Link.onMessage(JSON.stringify({ type: 'error', id: 'creator:frost_wings', message: 'Invalid bundle: something the game did not like' })), 20);
				const res = await VibeyPlugin.Link.push('equip');
				VibeyPlugin.Link.ws.send = orig;
				const d = document.querySelector('#vibey_friendly');
				const out = { type: res.type, key: Dialog.open && Dialog.open.vibey_key, details: d && d.querySelector('details pre').textContent };
				if (Dialog.open) Dialog.open.cancel();
				return out;
			});
			assert.equal(r.type, 'error');
			assert.equal(r.key, 'game_rejected');
			assert.match(r.details, /something the game did not like/);
		});
		await check('wizard for every slot + an emote creates a valid, exportable structure', async () => {
			const out = await cdp.run(async () => {
				const res = {};
				for (const slot of ['cape', 'wings', 'aura', 'pet', 'shield', 'hat', 'back', 'emote']) {
					const kind = slot === 'emote' ? 'emote' : 'cosmetic';
					VibeyPlugin.createItem({ kind, slot: kind === 'emote' ? undefined : slot, name: { en: 'E2E ' + slot }, loop: true, length: 1.5 });
					for (let i = 0; i < 60 && Texture.all.some((t) => !t.width); i++) await new Promise((r) => setTimeout(r, 50));
					const c = VibeyPlugin.compileProjectJSON();
					res[slot] = {
						format: Format.id, errors: c.errors, json: c.json,
						groups: Group.all.map((g) => g.name + (g.vibey_preview ? '*' : '')),
						locked: Cube.all.filter((x) => x.vibey_preview).every((x) => x.locked && x.export === false),
						anims: Animator.animations.map((a) => a.name),
					};
					await Project.close(true);
				}
				return res;
			});
			for (const slot of Object.keys(out)) {
				const r = out[slot];
				assert.deepEqual(r.errors, [], slot);
				const b = JSON.parse(r.json);
				assert.deepEqual(Core.validateBundle(b).errors, [], slot);
				assert.deepEqual(specValidate(b).errors, [], slot);
				assert.equal(b.id, 'creator:e2e_' + slot);
				if (slot === 'emote') {
					assert.equal(r.format, 'vibey_emote');
					assert.deepEqual(r.groups, Core.PLAYER_BONES);
					assert.deepEqual(b.emote.loop, true);
					assert.equal(b.animations.main.length, 1.5);
					continue;
				}
				const p = Core.SLOT_PRESETS[slot];
				assert.equal(b.cosmetic.slot, slot);
				assert.deepEqual([b.textures[0].width, b.textures[0].height], p.texture);
				if (p.textureOnly) {
					assert.deepEqual(r.groups, [slot + '_preview*'], slot + ': only the never-exported preview');
					assert.equal(r.locked, true);
					assert.equal(b.model, undefined);
					assert.equal(b.cosmetic[slot].texture, slot);
				} else {
					assert.equal(r.groups[0], p.root);
					assert.equal(b.model.bones[0].attach, p.attach);
					assert.deepEqual(r.anims, p.tracks);
				}
			}
		});
		await check('templates: a starter item becomes an editable copy with the creator\'s id', async () => {
			const out = await cdp.run(async () => {
				const res = [];
				for (const [kind, slot, template] of [['cosmetic', 'hat', 'seal_beanie'], ['cosmetic', 'cape', 'vibe_cape'], ['emote', undefined, 'wave']]) {
					VibeyPlugin.createItem({ kind, slot, template, name: { en: 'My ' + template } });
					for (let i = 0; i < 60 && Texture.all.some((t) => !t.width); i++) await new Promise((r) => setTimeout(r, 50));
					const c = VibeyPlugin.compileProjectJSON();
					res.push({ template, errors: c.errors, json: c.json });
					await Project.close(true);
				}
				return res;
			});
			const starter = (name) => JSON.parse(fs.readFileSync(path.join(REPO, 'content', 'dist', name + '.vibey'), 'utf8'));
			for (const r of out) {
				assert.deepEqual(r.errors, [], r.template);
				const b = JSON.parse(r.json);
				assert.equal(b.id, 'creator:my_' + r.template);
				assert.deepEqual(specValidate(b).errors, []);
				const s = starter(r.template);
				assert.deepEqual(b.animations, s.animations, r.template + ' animations');
				assert.deepEqual(b.model, s.model, r.template + ' model');
				assert.deepEqual(b.textures.map((t) => t.png), s.textures.map((t) => t.png), r.template + ' textures byte-exact');
			}
		});
		await check('guide can be hidden and reopened (Vibey > Tutorial); the choice is remembered', async () => {
			const r = await cdp.run(async () => {
				VibeyPlugin.createItem({ kind: 'cosmetic', slot: 'hat', name: { en: 'Guide Hat' } });
				await new Promise((res) => setTimeout(res, 200));
				const visible = () => {
					const el = document.querySelector('#panel_vibey_guide');
					return !!el && !el.closest('.panel_container.hidden') && el.offsetParent !== null;
				};
				const before = visible();
				VibeyPlugin.Guide.hide();
				await new Promise((res) => setTimeout(res, 200));
				const hidden = !visible() && VibeyPlugin.prefs().guide === false;
				BarItems.vibey_tutorial.click();
				await new Promise((res) => setTimeout(res, 200));
				const again = visible() && VibeyPlugin.prefs().guide === true;
				await Project.close(true);
				return { before, hidden, again };
			});
			assert.deepEqual(r, { before: true, hidden: true, again: true });
		});
		await check('remove dev items + disconnect', async () => {
			await cdp.eval('VibeyPlugin.Link.remove("creator:frost_wings"), VibeyPlugin.Link.disconnect(), true');
			assert.equal(await cdp.eval('VibeyPlugin.Link.state'), 'off');
		});
		await check('plugin unloads cleanly', async () => {
			const r = await cdp.eval(`(() => { Plugins.registered.vibey.unload(); return { formats: !!Formats.vibey_cosmetic, menu: !!MenuBar.menus.vibey, api: !!window.VibeyPlugin, status: !!document.getElementById('vibey_status'), guide: !!Panels.vibey_guide, wizard: !!document.querySelector('#vibey_wizard') && !!Dialog.open }; })()`);
			assert.deepEqual(r, { formats: false, menu: false, api: false, status: false, guide: false, wizard: false });
		});
	} catch (e) {
		results.push({ name: 'harness', ok: false, error: String(e.stack || e) });
		console.error(e);
		if (cdp && cdp.logs.length) console.error('Page log:\n  ' + cdp.logs.slice(-15).join('\n  '));
	} finally {
		await shutdown();
	}
	const failed = results.filter((r) => !r.ok);
	fs.writeFileSync(path.join(OUT, 'e2e-report.json'), JSON.stringify({ date: new Date().toISOString(), mode: REAL ? 'real-game' : 'mock', results }, null, 2));
	console.log(`\n${results.length - failed.length}/${results.length} checks passed${failed.length ? ' – FAILED: ' + failed.map((f) => f.name).join(', ') : ''}`);
	process.exit(failed.length ? 1 : 0);
}

main();
