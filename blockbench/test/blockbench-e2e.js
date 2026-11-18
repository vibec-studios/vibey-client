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
 *
 * Blockbench runs with an isolated, temporary profile (--userData / --user-data-dir), so the user's own
 * Blockbench settings, plugins and recent projects are never touched. Only the process started here is closed.
 */
const { spawn, execFileSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const net = require('net');
const assert = require('assert/strict');
const WebSocket = require('ws');
const Core = require('../vibey.js');
const { validate: specValidate } = require('./spec-validator');
const { createMockGame } = require('./mock-game-server');
const { fakeOgg } = require('./fixtures');

const args = process.argv.slice(2);
const flag = (n) => args.includes('--' + n);
const opt = (n, d) => (args.indexOf('--' + n) >= 0 ? args[args.indexOf('--' + n) + 1] : d);
const ROOT = path.resolve(__dirname, '..');
const REPO = path.resolve(ROOT, '..');
const PLUGIN = path.join(ROOT, 'vibey.js');
const OUT = path.join(__dirname, 'out');
const SHOTS = path.join(REPO, 'docs', 'screenshots');
const BB_EXE = opt('blockbench', path.join(process.env.LOCALAPPDATA || '', 'Programs', 'Blockbench', 'Blockbench.exe'));
const LANG = opt('lang', 'en');
let realGameDir = opt('game-dir');
if (opt('devlink')) realGameDir = path.dirname(path.dirname(path.resolve(opt('devlink'))));
const REAL = !!realGameDir;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
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
const freePort = () =>
	new Promise((res) => {
		const s = net.createServer().listen(0, '127.0.0.1', () => {
			const p = s.address().port;
			s.close(() => res(p));
		});
	});

// ------------------------------------------------------------------ CDP
class CDP {
	constructor(url) {
		this.ws = new WebSocket(url, { maxPayload: 256 * 1024 * 1024 });
		this.id = 0;
		this.pending = new Map();
		this.ws.on('message', (m) => {
			const msg = JSON.parse(m);
			if (msg.id && this.pending.has(msg.id)) {
				this.pending.get(msg.id)(msg);
				this.pending.delete(msg.id);
			}
		});
	}
	open() {
		return new Promise((res, rej) => (this.ws.once('open', res), this.ws.once('error', rej)));
	}
	send(method, params = {}) {
		return new Promise((resolve, reject) => {
			const id = ++this.id;
			this.pending.set(id, (msg) => (msg.error ? reject(new Error(method + ': ' + msg.error.message)) : resolve(msg.result)));
			this.ws.send(JSON.stringify({ id, method, params }));
		});
	}
	async eval(expression) {
		const r = await this.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true, userGesture: true });
		if (r.exceptionDetails) throw new Error('page: ' + ((r.exceptionDetails.exception && r.exceptionDetails.exception.description) || r.exceptionDetails.text));
		return r.result.value;
	}
	/** Runs fn(...args) inside Blockbench (fn must be self-contained). */
	run(fn, ...a) {
		return this.eval(`(async () => (${fn.toString()})(...${JSON.stringify(a)}))()`);
	}
	async waitFor(fn, timeout = 15000, ...a) {
		const t0 = Date.now();
		let last;
		while (Date.now() - t0 < timeout) {
			try {
				last = await this.run(fn, ...a);
				if (last) return last;
			} catch (e) {
				last = e.message;
			}
			await sleep(150);
		}
		throw new Error('timeout waiting for ' + fn.toString().slice(0, 120) + ' (last: ' + JSON.stringify(last) + ')');
	}
	async screenshot(name) {
		if (flag('no-screenshots')) return null;
		await sleep(400);
		const r = await this.send('Page.captureScreenshot', { format: 'png' });
		fs.mkdirSync(SHOTS, { recursive: true });
		const file = path.join(SHOTS, name);
		fs.writeFileSync(file, Buffer.from(r.data, 'base64'));
		console.log('    screenshot ' + file);
		return file;
	}
	close() {
		try {
			this.ws.close();
		} catch (e) {}
	}
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
	assert.ok(fs.existsSync(BB_EXE), 'Blockbench not found at ' + BB_EXE + ' (use --blockbench <path>)');
	const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'vibey-bb-e2e-'));
	const gameDir = REAL ? realGameDir : fs.mkdtempSync(path.join(os.tmpdir(), 'vibey-game-'));
	const cdpPort = await freePort();
	console.log(`Blockbench: ${BB_EXE}\nIsolated profile: ${profile}\nGame dir: ${gameDir}${REAL ? ' (REAL GAME)' : ' (mock)'}`);

	let game = null;
	if (!REAL) {
		game = await createMockGame({ player: 'E2EPlayer', game: '1.21.11' });
		game.writeDevlink(gameDir);
		console.log(`Mock game on 127.0.0.1:${game.port}`);
	} else {
		assert.ok(fs.existsSync(Core.devlinkPath(gameDir, path.sep)), 'devlink.json not found in ' + gameDir);
	}

	const proc = spawn(BB_EXE, ['--userData', profile, '--user-data-dir=' + profile, '--remote-debugging-port=' + cdpPort, '--lang=' + LANG], { stdio: 'ignore', detached: false });
	console.log('Started Blockbench pid ' + proc.pid);
	let cdp;
	const shutdown = async () => {
		if (cdp) cdp.close();
		if (!flag('keep-open')) {
			try {
				if (process.platform === 'win32') execFileSync('taskkill', ['/PID', String(proc.pid), '/T', '/F'], { stdio: 'ignore' });
				else proc.kill();
			} catch (e) {}
			await sleep(800);
			try {
				fs.rmSync(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 300 });
			} catch (e) {}
			if (!REAL) fs.rmSync(gameDir, { recursive: true, force: true });
		}
		if (game) await game.close();
	};

	try {
		let target;
		for (let i = 0; i < 120 && !target; i++) {
			try {
				const list = await (await fetch(`http://127.0.0.1:${cdpPort}/json`)).json();
				target = list.find((t) => t.type === 'page' && t.url.includes('index.html'));
			} catch (e) {}
			if (!target) await sleep(500);
		}
		assert.ok(target, 'Blockbench page did not appear on the DevTools port');
		cdp = new CDP(target.webSocketDebuggerUrl);
		await cdp.open();
		await cdp.waitFor(page.ready, 60000);
		// let Blockbench finish restoring installed plugins (it fetches the online plugin list first)
		await cdp.waitFor(() => typeof Plugins !== 'undefined' && !Plugins.loading_promise, 60000);
		await sleep(1500);

		assert.equal(await cdp.eval('Language.code'), LANG, 'UI language (--lang) not applied');
		const bbVersion = await cdp.eval('Blockbench.version');
		console.log('Blockbench ' + bbVersion);

		console.log('\nPlugin');
		await check('plugin loads from file and registers formats/codec', async () => {
			assert.ok(await cdp.run(page.loadPlugin, PLUGIN));
			await cdp.waitFor(() => window.VibeyPlugin && Formats.vibey_cosmetic && Formats.vibey_emote && Codecs.vibey && MenuBar.menus.vibey);
			const info = await cdp.eval(`({ v: VibeyPlugin.VERSION, author: Plugins.registered.vibey.author, variant: Plugins.registered.vibey.variant, exp: !!BarItems.vibey_export, imp: !!BarItems.vibey_import })`);
			assert.deepEqual(info, { v: '1.0.0', author: 'Vibe Craft Studios', variant: 'desktop', exp: true, imp: true });
		});
		await check('loading the plugin file a second time keeps a single instance', async () => {
			await cdp.run(async (file) => {
				await new Plugin().loadFromFile({ path: file, name: 'vibey.js', content: '' }, false);
				return true;
			}, PLUGIN);
			await sleep(500);
			const r = await cdp.eval(`({ panels: document.querySelectorAll('.vibey_panel').length, status: document.querySelectorAll('#vibey_status').length, formats: !!Formats.vibey_cosmetic && !!Formats.vibey_emote, menus: Object.keys(MenuBar.menus).filter((k) => k === 'vibey').length })`);
			assert.deepEqual(r, { panels: 1, status: 1, formats: true, menus: 1 });
		});
		await cdp.run((dir) => {
			Object.assign(VibeyPlugin.LinkSettings, { autoFind: true, gameDir: dir, autoPush: false, replayEmote: true });
			VibeyPlugin.saveLinkSettings();
			return true;
		}, gameDir);

		// ================================================================ COSMETIC
		console.log('\nVibey Cosmetic');
		await check('new-project dialog asks for id, names, slot, rarity, tags', async () => {
			await cdp.eval('Formats.vibey_cosmetic.new(), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_properties');
			await cdp.run(() => {
				Dialog.open.setFormValues({ id: 'e2e:seal_hat', name_en: 'Seal Hat', name_de: 'Robbenhut', desc_en: 'A cozy hat with a wobbly top.', tags: 'seal, hat, e2e', rarity: 'epic', slot: 'hat', hide_head: true });
				return true;
			});
			await cdp.screenshot('blockbench-new-cosmetic-dialog.png');
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
			await cdp.screenshot('blockbench-cosmetic-edit.png');
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
			await cdp.screenshot('blockbench-validation.png');
			await cdp.eval('Dialog.open && Dialog.open.close(0), true');
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
				assert.equal(game.state.hellos[0].version, '1.0.0');
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
		if (!REAL) {
			await check('reconnects with backoff after the game restarts', async () => {
				const { port, token } = game;
				await game.close();
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'error' && /Retrying|Neuer Versuch/.test(VibeyPlugin.Link.message), 10000);
				const msg = await cdp.eval('VibeyPlugin.Link.message');
				assert.match(msg, /lost|not reachable/);
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
				assert.match(msg, /Creator mode/);
				await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 10000);
			});
		}
		await cdp.run(() => (VibeyPlugin.openLinkSettings(), true));
		await sleep(300);
		await cdp.screenshot('blockbench-live-link-settings.png');
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

		// ================================================================ EMOTE
		console.log('\nVibey Emote');
		await check('new emote project: dialog + player rig root > head/body/arms/legs with SPEC pivots', async () => {
			await cdp.eval('Formats.vibey_emote.new(), true');
			await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_properties');
			await cdp.run(() => {
				Dialog.open.setFormValues({ id: 'e2e:flag_wave', name_en: 'Flag Wave', name_de: 'Fahne schwenken', tags: 'wave, flag', third_person: true, stop_on_move: false, hide_held: true, loop: false });
				return true;
			});
			await cdp.screenshot('blockbench-new-emote-dialog.png');
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
			await cdp.screenshot('blockbench-emote-animate.png');
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
			await cdp.screenshot('blockbench-emote-pushed.png');
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
		await check('remove dev item + disconnect', async () => {
			const removed = game && game.waitFor('remove', (id) => id === 'e2e:flag_wave');
			await cdp.eval('VibeyPlugin.Link.remove("e2e:flag_wave"), true');
			if (game) await removed;
			await cdp.eval('VibeyPlugin.Link.disconnect(), true');
			assert.equal(await cdp.eval('VibeyPlugin.Link.state'), 'off');
		});
		await check('plugin unloads cleanly', async () => {
			const r = await cdp.eval(`(() => { Plugins.registered.vibey.unload(); return { formats: !!Formats.vibey_cosmetic, menu: !!MenuBar.menus.vibey, api: !!window.VibeyPlugin, status: !!document.getElementById('vibey_status') }; })()`);
			assert.deepEqual(r, { formats: false, menu: false, api: false, status: false });
		});
	} catch (e) {
		results.push({ name: 'harness', ok: false, error: String(e.stack || e) });
		console.error(e);
	} finally {
		await shutdown();
	}
	const failed = results.filter((r) => !r.ok);
	fs.writeFileSync(path.join(OUT, 'e2e-report.json'), JSON.stringify({ date: new Date().toISOString(), mode: REAL ? 'real-game' : 'mock', results }, null, 2));
	console.log(`\n${results.length - failed.length}/${results.length} checks passed${failed.length ? ' – FAILED: ' + failed.map((f) => f.name).join(', ') : ''}`);
	process.exit(failed.length ? 1 : 0);
}

main();
