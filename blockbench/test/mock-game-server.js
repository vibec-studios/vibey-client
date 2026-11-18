'use strict';
/**
 * Mock Vibey Client devlink server (docs/SPEC.md §8) for testing the Blockbench plugin without Minecraft.
 *
 * - binds 127.0.0.1 only, rejects non-loopback peers and wrong tokens with close code 4001
 * - max message size 8 MiB
 * - hello -> welcome, push -> pushed | error (bundle validated against SPEC §5), remove, ping -> pong
 *
 * CLI:  node test/mock-game-server.js [--port 47821] [--token abc...] [--game-dir <dir>]
 *       writes <game-dir>/vibeyclient/devlink.json so "Find game automatically" works.
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { WebSocketServer } = require('ws');
const { validate } = require('./spec-validator');

const LOOPBACK = new Set(['127.0.0.1', '::1', '::ffff:127.0.0.1']);

function createMockGame(options = {}) {
	const token = options.token || crypto.randomBytes(18).toString('base64url'); // 24 chars
	const state = {
		token,
		port: 0,
		connections: 0,
		hellos: [],
		pushes: [], // { bundle, action, ok, errors, warnings, at }
		removed: [],
		items: new Map(), // dev items by id
		rejected: [], // { reason, code }
		listeners: [],
	};
	const remoteAddressOf = options.remoteAddressOf || ((req) => req.socket.remoteAddress);
	const wss = new WebSocketServer({ host: '127.0.0.1', port: options.port || 0, maxPayload: 8 * 1024 * 1024 });
	const notify = (ev, data) => state.listeners.filter((l) => l.ev === ev).forEach((l) => l.fn(data));

	wss.on('connection', (ws, req) => {
		state.connections++;
		const addr = remoteAddressOf(req);
		if (!LOOPBACK.has(addr)) {
			state.rejected.push({ reason: 'non-loopback ' + addr, code: 4001 });
			ws.close(4001, 'loopback only');
			return;
		}
		let authed = false;
		const helloTimer = setTimeout(() => !authed && ws.close(4001, 'no hello'), options.helloTimeout || 5000);
		const send = (obj) => ws.readyState === 1 && ws.send(JSON.stringify(obj));
		ws.on('message', (data, isBinary) => {
			let msg;
			try {
				msg = JSON.parse(isBinary ? data.toString('utf8') : data.toString());
			} catch (e) {
				return send({ type: 'error', id: null, message: 'invalid JSON' });
			}
			if (!authed) {
				if (msg.type !== 'hello' || msg.token !== token) {
					state.rejected.push({ reason: msg.type !== 'hello' ? 'message before hello' : 'wrong token', code: 4001 });
					clearTimeout(helloTimer);
					return ws.close(4001, 'unauthorized');
				}
				authed = true;
				clearTimeout(helloTimer);
				state.hellos.push(msg);
				send({ type: 'welcome', game: options.game || '1.21.11', mod: options.mod || '1.0.0', player: options.player || 'MockPlayer' });
				notify('hello', msg);
				return;
			}
			switch (msg.type) {
				case 'ping':
					return send({ type: 'pong' });
				case 'push': {
					const bundle = msg.bundle;
					const id = bundle && bundle.id;
					const result = validate(bundle);
					const warnings = result.warnings.slice();
					if (!['equip', 'play', 'none'].includes(msg.action)) result.errors.push(`unknown action ${msg.action}`);
					if (bundle && bundle.kind === 'cosmetic' && msg.action === 'play') warnings.push('play is ignored for cosmetics');
					if (bundle && bundle.kind === 'emote' && msg.action === 'equip') warnings.push('equip is ignored for emotes');
					const entry = { bundle, action: msg.action, ok: !result.errors.length, errors: result.errors, warnings, at: Date.now() };
					state.pushes.push(entry);
					if (entry.ok) {
						state.items.set(id, bundle); // hot-replace same id
						send({ type: 'pushed', id, ok: true, warnings });
						send({ type: 'log', level: 'info', message: `${msg.action === 'play' ? 'Playing' : 'Loaded'} dev item ${id}` });
					} else {
						send({ type: 'error', id: id || null, message: 'Invalid bundle: ' + result.errors.join('; ') });
					}
					notify('push', entry);
					return;
				}
				case 'remove':
					state.items.delete(msg.id);
					state.removed.push(msg.id);
					notify('remove', msg.id);
					return send({ type: 'log', level: 'info', message: `Removed dev item ${msg.id}` });
				default:
					return send({ type: 'error', id: null, message: `unknown message type ${msg.type}` });
			}
		});
		ws.on('close', () => clearTimeout(helloTimer));
		ws.on('error', (err) => state.rejected.push({ reason: String(err.message || err), code: err.code }));
	});

	return new Promise((resolve, reject) => {
		wss.once('error', reject);
		wss.once('listening', () => {
			state.port = wss.address().port;
			resolve({
				get port() {
					return state.port;
				},
				token,
				state,
				on(ev, fn) {
					state.listeners.push({ ev, fn });
				},
				waitFor(ev, predicate = () => true, timeout = 15000) {
					return new Promise((res, rej) => {
						const t = setTimeout(() => rej(new Error(`timeout waiting for ${ev}`)), timeout);
						state.listeners.push({ ev, fn: (d) => predicate(d) && (clearTimeout(t), res(d)) });
					});
				},
				/** Writes <gameDir>/vibeyclient/devlink.json like the real client does. */
				writeDevlink(gameDir) {
					const dir = path.join(gameDir, 'vibeyclient');
					fs.mkdirSync(dir, { recursive: true });
					const file = path.join(dir, 'devlink.json');
					fs.writeFileSync(file, JSON.stringify({ port: state.port, token, pid: process.pid }, null, 2));
					return file;
				},
				close() {
					return new Promise((res) => {
						wss.clients.forEach((c) => c.terminate());
						wss.close(() => res());
					});
				},
			});
		});
	});
}

module.exports = { createMockGame };

if (require.main === module) {
	const args = process.argv.slice(2);
	const arg = (name, def) => {
		const i = args.indexOf('--' + name);
		return i >= 0 ? args[i + 1] : def;
	};
	createMockGame({ port: Number(arg('port', 47821)), token: arg('token') }).then((game) => {
		console.log(`Mock Vibey game listening on ws://127.0.0.1:${game.port}  token=${game.token}`);
		const dir = arg('game-dir');
		if (dir) console.log('Wrote ' + game.writeDevlink(dir));
		game.on('push', (p) => console.log(`push ${p.bundle && p.bundle.id} action=${p.action} ok=${p.ok}${p.errors.length ? ' errors=' + p.errors.join('; ') : ''}`));
		game.on('remove', (id) => console.log('remove ' + id));
	});
}
