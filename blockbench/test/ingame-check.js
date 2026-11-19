#!/usr/bin/env node
'use strict';
/**
 * In-game check of "build on the player": builds test items on the reference player's groups in the REAL
 * Blockbench (isolated profile), pushes them into a running dev client over the live link and photographs them in
 * game through the client's DevDriver (dev runs only, -Dvibeyclient.devDriver=<port>):
 *
 *   ./gradlew :fabric-1.21.11:runClient -Dvibeyclient.devDriver=47911 -Dvibeyclient.quickPlay=<world>
 *   node test/ingame-check.js --game-dir <repo>/versions/fabric-1.21.11/run --driver 47911 [--out dir] [--lang de]
 *
 * Items: "On the player" (slot back) – a backpack on ◆ Back, a stick in the right hand on ◆ Right arm, an antenna
 * on ◆ Head with a ball on ◆ Above head, a band on ◆ Left leg – and a pet on ◆ Beside player.
 * Shots: standing, head turned + looking up, walking (pushed along by a water current, so the vanilla walk animation
 * swings arms and legs), sneaking (forced crouch under a slab) and the pet beside the player. The player is moved by
 * server commands only (creative test world with cheats).
 */
const fs = require('fs');
const os = require('os');
const net = require('net');
const path = require('path');
const assert = require('assert/strict');
const Core = require('../vibey.js');
const { launchBlockbench, sleep } = require('./bb-harness');

const args = process.argv.slice(2);
const opt = (n, d) => (args.indexOf('--' + n) >= 0 ? args[args.indexOf('--' + n) + 1] : d);
const GAME = path.resolve(opt('game-dir', ''));
const PORT = Number(opt('driver', 47911));
const OUT = path.resolve(opt('out', path.join(__dirname, 'out', 'ingame')));
const LANG = opt('lang', 'en');
const PLUGIN = path.join(__dirname, '..', 'vibey.js');

/** DevDriver: one command per line, one answer per line. */
function driver(port) {
	const sock = net.connect(port, '127.0.0.1');
	let buf = '';
	const waiting = [];
	sock.on('data', (d) => {
		buf += d.toString('utf8');
		let i;
		while ((i = buf.indexOf('\n')) >= 0) {
			const line = buf.slice(0, i).trim();
			buf = buf.slice(i + 1);
			const w = waiting.shift();
			if (w) w(line);
		}
	});
	const ready = new Promise((res, rej) => (sock.once('connect', res), sock.once('error', rej)));
	return {
		ready,
		send: (cmd) => new Promise((res) => (waiting.push(res), sock.write(cmd + '\n'))),
		close: () => sock.end(),
	};
}

const shotsDir = () => path.join(GAME, 'screenshots');
async function shot(d, name) {
	const before = new Set(fs.existsSync(shotsDir()) ? fs.readdirSync(shotsDir()) : []);
	await d.send('shot');
	for (let i = 0; i < 60; i++) {
		await sleep(100);
		const now = fs.existsSync(shotsDir()) ? fs.readdirSync(shotsDir()).filter((f) => !before.has(f) && f.endsWith('.png')) : [];
		if (now.length) {
			await sleep(300);
			const file = path.join(OUT, name + '.png');
			fs.copyFileSync(path.join(shotsDir(), now[0]), file);
			console.log('  shot ' + file);
			return file;
		}
	}
	throw new Error('no screenshot for ' + name);
}

async function main() {
	assert.ok(fs.existsSync(Core.devlinkPath(GAME, path.sep)), 'devlink.json not found – start the dev client with Creator mode (dev runs have it on): ' + GAME);
	fs.mkdirSync(OUT, { recursive: true });
	const d = driver(PORT);
	await d.ready;
	console.log('DevDriver: ' + (await d.send('status')));
	const bb = await launchBlockbench({ lang: LANG });
	const cdp = bb.cdp;
	const report = { items: {}, shots: [] };
	try {
		await cdp.run(async (file, dir) => {
			localStorage.setItem('vibey.prefs', JSON.stringify({ welcomed: true, guide: false }));
			await new Plugin().loadFromFile({ path: file, name: 'vibey.js', content: '' }, false);
			for (let i = 0; i < 50 && !window.VibeyPlugin; i++) await new Promise((r) => setTimeout(r, 100));
			Object.assign(VibeyPlugin.LinkSettings, { autoFind: true, gameDir: dir, autoPush: false });
			return true;
		}, PLUGIN, GAME);
		// ---- item 1: parts on head, above head, back, right arm and left leg
		report.items.onPlayer = await cdp.run(async () => {
			if (Dialog.open) Dialog.open.close();
			VibeyPlugin.createItem({ kind: 'cosmetic', slot: 'back', id: 'ingame:on_player', name: { en: 'On the player', de: 'Am Spieler' } });
			for (let i = 0; i < 60 && !(Texture.all[0] && Texture.all[0].width); i++) await new Promise((r) => setTimeout(r, 50));
			// a bright texture: purple top half, teal bottom half
			const tex = Texture.all[0];
			const ctx = tex.canvas.getContext('2d');
			ctx.fillStyle = '#8b6cff';
			ctx.fillRect(0, 0, 64, 32);
			ctx.fillStyle = '#3fe0c5';
			ctx.fillRect(0, 32, 64, 32);
			tex.updateChangesAfterEdit ? tex.updateChangesAfterEdit() : tex.updateSource(tex.canvas.toDataURL());
			const refs = VibeyPlugin.Refs.map();
			const add = (ref, name, origin, cubes) => {
				const g = ref === 'back' ? Group.all.find((x) => x.name === 'back') : new Group({ name, origin }).addTo(refs[ref]).init();
				cubes.forEach(([from, to, uv]) => new Cube({ name: name + '_cube', from, to, box_uv: true, uv_offset: uv || [0, 0] }).addTo(g).init());
			};
			add('back', 'back', null, [[[-4, 13, 2], [4, 23, 7], [0, 0]], [[-3, 23, 3], [3, 25, 6], [0, 0]]]);
			add('right_arm', 'stick', [6, 12, 0], [[[5.5, 0, -0.5], [6.5, 12, 0.5], [0, 32]], [[3.6, 11, -2.4], [8.4, 13, 2.4], [0, 32]]]);
			add('head', 'antenna', [0, 32, 0], [[[-0.5, 32, -0.5], [0.5, 38, 0.5], [0, 32]]]);
			add('above_head', 'ball', [0, 39, 0], [[[-1.5, 38, -1.5], [1.5, 41, 1.5], [0, 0]]]);
			add('left_leg', 'band', [-1.9, 12, 0], [[[-4.2, 4, -2.2], [0.2, 6, 2.2], [0, 32]]]);
			Canvas.updateAll();
			const r = VibeyPlugin.buildBundle();
			return { errors: r.errors, bones: r.bundle.model.bones.filter((b) => !b.parent).map((b) => b.name + ':' + b.attach) };
		});
		console.log('item on the player:', JSON.stringify(report.items.onPlayer));
		assert.deepEqual(report.items.onPlayer.errors, []);
		const pushed = await cdp.run(async () => {
			VibeyPlugin.Link.connect();
			for (let i = 0; i < 100 && VibeyPlugin.Link.state !== 'connected'; i++) await new Promise((r) => setTimeout(r, 100));
			const a = await VibeyPlugin.Link.push('equip');
			return { state: VibeyPlugin.Link.state, result: a && a.type, mod: VibeyPlugin.Link.modVersion || '' };
		});
		console.log('push on the player:', JSON.stringify(pushed));
		assert.equal(pushed.result, 'pushed');
		// ---- item 2: the pet beside the player (starter penguin copy, built on ◆ Beside player)
		report.items.pet = await cdp.run(async () => {
			VibeyPlugin.createItem({ kind: 'cosmetic', slot: 'pet', template: 'pixel_penguin', id: 'ingame:beside_pet', name: { en: 'Beside pet' } });
			for (let i = 0; i < 60 && Texture.all.some((t) => !t.width); i++) await new Promise((r) => setTimeout(r, 50));
			const part = Group.all.find((g) => !g.vibey_ref && g.parent && g.parent.vibey_ref);
			const r = VibeyPlugin.buildBundle();
			const a = await VibeyPlugin.Link.push('equip');
			return { errors: r.errors, part: part.name + '@' + part.parent.vibey_ref, origin: part.origin.slice(), exported: r.bundle.model.bones[0].pivot, attach: r.bundle.model.bones[0].attach, push: a && a.type };
		});
		console.log('pet:', JSON.stringify(report.items.pet));
		assert.equal(report.items.pet.push, 'pushed');
		await sleep(1500);

		// ---- the scene (test world with cheats); everything is placed relative to a marker entity, so it does not
		// drift when the player sinks into mud or is pushed by water
		const c = (cmd) => d.send('cmd ' + cmd);
		const at = (cmd) => c('execute at @e[tag=vibey_scene,limit=1] run ' + cmd);
		const pose = (yaw, pitch) => at(`tp @s ~ ~ ~ ${yaw} ${pitch}`);
		// survival: a flying creative player is not pushed by water and floats (no forced crouch); effects keep it safe
		for (const cmd of ['gamemode survival', 'effect give @s minecraft:resistance infinite 4 true', 'effect give @s minecraft:saturation infinite 0 true',
			'effect give @s minecraft:fire_resistance infinite 0 true', 'time set 6000',
			// gamerule names changed in 1.21.11 (snake_case); the old ones are tried too (older clients)
			'gamerule advance_time false', 'gamerule doDaylightCycle false', 'gamerule advance_weather false', 'gamerule doWeatherCycle false',
			'gamerule spawn_mobs false', 'gamerule doMobSpawning false', 'weather clear', 'kill @e[type=!player]', 'summon minecraft:marker ~ ~ ~ {Tags:["vibey_scene"]}']) await c(cmd);
		for (const cmd of ['fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 minecraft:white_concrete', 'fill ~-8 ~ ~-8 ~8 ~5 ~8 minecraft:air', 'tp @s ~ ~ ~ 0 0']) await at(cmd);
		await sleep(7000); // let the push / connect toasts fade
		await d.send('camera front');
		await sleep(1200);
		report.shots.push(await shot(d, '01_standing_front'));
		await d.send('camera back');
		await sleep(800);
		report.shots.push(await shot(d, '02_standing_back'));
		// head: turn + look up / down (the body stays, the head part turns – the antenna and the ball follow)
		await d.send('camera front');
		await pose(40, -35);
		await sleep(900);
		report.shots.push(await shot(d, '03_head_turned_up'));
		await pose(-40, 30);
		await sleep(900);
		report.shots.push(await shot(d, '04_head_turned_down'));
		await pose(0, 0);
		await sleep(600);
		await d.send('camera back');
		await pose(-60, 5);
		await sleep(900);
		report.shots.push(await shot(d, '05_standing_side'));
		// sneak: standing on mud (7/8 high) under a top slab leaves a 1.625 block gap – too low to stand (1.8), high
		// enough to crouch (1.5): the game forces the crouching pose; the backpack follows the body
		await at('setblock ~ ~-1 ~ minecraft:mud');
		await at('setblock ~ ~1 ~ minecraft:smooth_stone_slab[type=top]');
		await sleep(1800);
		report.shots.push(await shot(d, '06_sneaking_side'));
		await pose(0, 5);
		await sleep(900);
		report.shots.push(await shot(d, '06_sneaking_back'));
		await at('setblock ~ ~1 ~ minecraft:air');
		await at('setblock ~ ~-1 ~ minecraft:white_concrete');
		await at('tp @s ~ ~0.2 ~ 0 0');
		await sleep(1000);
		// walk: a water current in a channel pushes the player along +Z – the real walk animation swings arms and legs
		// (the view looks east, the current pushes south: the camera sees the walking player from the side)
		for (const cmd of ['fill ~-1 ~-1 ~-2 ~1 ~-1 ~14 minecraft:white_concrete', 'fill ~-1 ~ ~-2 ~-1 ~ ~14 minecraft:glass', 'fill ~1 ~ ~-2 ~1 ~ ~14 minecraft:glass',
			'setblock ~ ~ ~-2 minecraft:glass', 'setblock ~ ~ ~-1 minecraft:water']) await at(cmd);
		await pose(-90, 5);
		await d.send('camera front');
		for (let i = 0; i < 8; i++) {
			await sleep(i ? 180 : 1500);
			report.shots.push(await shot(d, '07_walking_' + (i + 1)));
		}
		await at('fill ~-3 ~ ~-3 ~3 ~2 ~15 minecraft:air');
		await at('fill ~-3 ~-1 ~-3 ~3 ~-1 ~15 minecraft:white_concrete');
		await pose(0, 0);
		// the pet: walks beside the player (attach "world" + follow point)
		await sleep(3000);
		await d.send('camera front');
		await sleep(800);
		report.shots.push(await shot(d, '08_pet_beside_front'));
		await d.send('camera back');
		await sleep(800);
		report.shots.push(await shot(d, '09_pet_beside_back'));
		await d.send('camera first');
		await c('kill @e[tag=vibey_scene]');
		await c('gamemode creative');
	} finally {
		fs.writeFileSync(path.join(OUT, 'report.json'), JSON.stringify(report, null, 2));
		d.close();
		await bb.close();
	}
}
main().catch((e) => {
	console.error(e);
	process.exit(1);
});
