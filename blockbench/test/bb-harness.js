'use strict';
/**
 * Starts the REAL installed Blockbench (Electron) with an isolated, temporary profile and drives it over the
 * Chrome DevTools Protocol. Shared by the e2e test (blockbench-e2e.js) and the review screenshots (wizard-shots.js).
 *
 * Only the Blockbench process started here is ever closed (taskkill /T on its own pid), and its temporary profile is
 * removed afterwards. The user's own Blockbench settings, plugins and recent projects are never touched.
 */
const { spawn, execFileSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const net = require('net');
const WebSocket = require('ws');

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const DEFAULT_EXE = path.join(process.env.LOCALAPPDATA || '', 'Programs', 'Blockbench', 'Blockbench.exe');

const freePort = () =>
	new Promise((res) => {
		const s = net.createServer().listen(0, '127.0.0.1', () => {
			const p = s.address().port;
			s.close(() => res(p));
		});
	});

class CDP {
	constructor(url) {
		this.ws = new WebSocket(url, { maxPayload: 256 * 1024 * 1024 });
		this.id = 0;
		this.pending = new Map();
		this.logs = []; // page errors / console.error + warn, for diagnosing failures
		this.ws.on('message', (m) => {
			const msg = JSON.parse(m);
			if (msg.method === 'Runtime.exceptionThrown') {
				const d = msg.params.exceptionDetails;
				this.logs.push('exception: ' + ((d.exception && d.exception.description) || d.text));
			} else if (msg.method === 'Runtime.consoleAPICalled' && /error|warn/.test(msg.params.type)) {
				this.logs.push(msg.params.type + ': ' + msg.params.args.map((a) => a.value !== undefined ? a.value : a.description || a.type).join(' '));
			}
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
	/** PNG screenshot of the window (optionally clipped to {x, y, width, height} in CSS pixels). */
	async capture(clip) {
		const params = { format: 'png' };
		if (clip) params.clip = Object.assign({ scale: 1 }, clip);
		const r = await this.send('Page.captureScreenshot', params);
		return Buffer.from(r.data, 'base64');
	}
	close() {
		try {
			this.ws.close();
		} catch (e) {}
	}
}

/**
 * Launches Blockbench. Returns { cdp, proc, profile, close() }.
 * opts: { exe, lang, width, height, keepOpen }
 */
async function launchBlockbench(opts = {}) {
	const exe = opts.exe || DEFAULT_EXE;
	if (!fs.existsSync(exe)) throw new Error('Blockbench not found at ' + exe + ' (use --blockbench <path>)');
	const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'vibey-bb-e2e-'));
	const cdpPort = await freePort();
	const args = ['--userData', profile, '--user-data-dir=' + profile, '--remote-debugging-port=' + cdpPort, '--lang=' + (opts.lang || 'en')];
	const proc = spawn(exe, args, { stdio: 'ignore', detached: false });
	let cdp = null;
	const close = async () => {
		if (cdp) cdp.close();
		if (opts.keepOpen) return;
		try {
			if (process.platform === 'win32') execFileSync('taskkill', ['/PID', String(proc.pid), '/T', '/F'], { stdio: 'ignore' });
			else proc.kill();
		} catch (e) {}
		await sleep(800);
		try {
			fs.rmSync(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 300 });
		} catch (e) {}
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
		if (!target) throw new Error('Blockbench page did not appear on the DevTools port');
		cdp = new CDP(target.webSocketDebuggerUrl);
		await cdp.open();
		await cdp.send('Runtime.enable').catch(() => {});
		if (opts.width && opts.height) {
			// a fixed window size makes screenshots comparable between runs
			await cdp.send('Emulation.setDeviceMetricsOverride', { width: opts.width, height: opts.height, deviceScaleFactor: 1, mobile: false }).catch(() => {});
		}
		await cdp.waitFor(
			() => typeof BBPlugin !== 'undefined' && typeof Formats !== 'undefined' && !!document.getElementById('status_bar') && typeof Interface !== 'undefined' && !!Interface.Panels,
			60000
		);
		// let Blockbench finish restoring installed plugins (it fetches the online plugin list first)
		await cdp.waitFor(() => typeof Plugins !== 'undefined' && !Plugins.loading_promise, 60000);
		await sleep(1500);
	} catch (e) {
		await close();
		throw e;
	}
	return { cdp, proc, profile, close };
}

module.exports = { CDP, launchBlockbench, freePort, sleep, DEFAULT_EXE };
