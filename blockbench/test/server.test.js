'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const WebSocket = require('ws');
const Core = require('../vibey.js');
const { createMockGame } = require('./mock-game-server');
const { cosmeticSnapshot } = require('./fixtures');

function client(port) {
	const ws = new WebSocket(`ws://127.0.0.1:${port}`);
	const inbox = [];
	const waiters = [];
	ws.on('message', (d) => {
		const msg = JSON.parse(d.toString());
		const w = waiters.findIndex((x) => x.pred(msg));
		if (w >= 0) waiters.splice(w, 1)[0].resolve(msg);
		else inbox.push(msg);
	});
	const closed = new Promise((res) => ws.on('close', (code) => res(code)));
	return {
		ws,
		closed,
		open: () => new Promise((res, rej) => (ws.once('open', res), ws.once('error', rej))),
		send: (o) => ws.send(JSON.stringify(o)),
		next(pred = () => true) {
			const i = inbox.findIndex(pred);
			if (i >= 0) return Promise.resolve(inbox.splice(i, 1)[0]);
			return new Promise((resolve) => waiters.push({ pred, resolve }));
		},
	};
}

test('mock game server implements SPEC §8', async (t) => {
	const game = await createMockGame({ player: 'Tester' });
	t.after(() => game.close());
	assert.ok(game.token.length >= 24);

	await t.test('wrong token is rejected with 4001', async () => {
		const c = client(game.port);
		await c.open();
		c.send({ type: 'hello', token: 'wrong', client: 'test', version: '0' });
		assert.equal(await c.closed, 4001);
	});

	await t.test('messages before hello are rejected with 4001', async () => {
		const c = client(game.port);
		await c.open();
		c.send({ type: 'ping' });
		assert.equal(await c.closed, 4001);
	});

	await t.test('hello -> welcome, ping -> pong, push -> pushed, invalid push -> error, remove', async () => {
		const c = client(game.port);
		await c.open();
		c.send({ type: 'hello', token: game.token, client: 'blockbench-vibey', version: '1.0.0' });
		const welcome = await c.next((m) => m.type === 'welcome');
		assert.equal(welcome.player, 'Tester');
		assert.equal(typeof welcome.game, 'string');
		c.send({ type: 'ping' });
		assert.equal((await c.next((m) => m.type === 'pong')).type, 'pong');

		const bundle = Core.compileBundle(cosmeticSnapshot()).bundle;
		c.send({ type: 'push', bundle, action: 'equip' });
		const pushed = await c.next((m) => m.type === 'pushed' || m.type === 'error');
		assert.deepEqual(pushed, { type: 'pushed', id: 'test:seal_hat', ok: true, warnings: [] });
		assert.equal(game.state.items.get('test:seal_hat').id, 'test:seal_hat');

		const broken = JSON.parse(JSON.stringify(bundle));
		broken.model.bones[0].cubes[0].texture = 'missing';
		c.send({ type: 'push', bundle: broken, action: 'equip' });
		const err = await c.next((m) => m.type === 'error');
		assert.equal(err.id, 'test:seal_hat');
		assert.match(err.message, /cube texture missing/);

		c.send({ type: 'remove', id: 'test:seal_hat' });
		await game.waitFor('remove');
		assert.equal(game.state.items.has('test:seal_hat'), false);
		c.ws.close();
	});

	await t.test('messages over 8 MiB are refused', async () => {
		const c = client(game.port);
		await c.open();
		c.send({ type: 'hello', token: game.token, client: 'x', version: '1' });
		await c.next((m) => m.type === 'welcome');
		c.ws.send('x'.repeat(8 * 1024 * 1024 + 10));
		assert.equal(await c.closed, 1009);
	});
});

test('non-loopback peers are rejected with 4001', async (t) => {
	const game = await createMockGame({ remoteAddressOf: () => '192.168.1.20' });
	t.after(() => game.close());
	const c = client(game.port);
	assert.equal(await c.closed, 4001);
	assert.match(game.state.rejected[0].reason, /non-loopback/);
});
