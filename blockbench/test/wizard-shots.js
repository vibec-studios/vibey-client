#!/usr/bin/env node
'use strict';
/**
 * Review screenshots of the wizard, the guide panel, the menu and the friendly errors in the REAL Blockbench
 * (isolated profile, mock game server), English plus a German pass. Writes the PNGs to test/out/shots/ and one
 * self-contained page with all of them embedded: blockbench/review/wizard.html.
 *
 *   npm run shots                     # both languages
 *   npm run shots -- --lang de        # only German
 *   npm run shots -- --blockbench <Blockbench.exe>
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { launchBlockbench, sleep } = require('./bb-harness');
const { createMockGame } = require('./mock-game-server');

const args = process.argv.slice(2);
const opt = (n, d) => (args.indexOf('--' + n) >= 0 ? args[args.indexOf('--' + n) + 1] : d);
const ROOT = path.resolve(__dirname, '..');
const PLUGIN = path.join(ROOT, 'vibey.js');
const OUT = path.join(__dirname, 'out', 'shots');
const HTML = path.join(ROOT, 'review', 'wizard.html');
const W = 1440, H = 900;

const shots = []; // { file, lang, group, title, note }

async function shoot(cdp, lang, name, group, title, note, opts = {}) {
	await sleep(opts.wait || 450);
	if (!opts.keepToast) await cdp.run(() => (document.querySelectorAll('#quick_message_box, .quick_message_box').forEach((e) => e.remove()), true));
	let clip = null;
	if (opts.selector) {
		clip = await cdp.run((sel, pad) => {
			const el = document.querySelector(sel);
			if (!el) return null;
			const r = el.getBoundingClientRect();
			return { x: Math.max(0, r.left - pad), y: Math.max(0, r.top - pad), width: Math.min(window.innerWidth, r.width + pad * 2), height: Math.min(window.innerHeight, r.height + pad * 2) };
		}, opts.selector, opts.pad === undefined ? 12 : opts.pad);
	}
	if (opts.clip) clip = opts.clip;
	const png = await cdp.capture(clip);
	const file = path.join(OUT, `${lang}-${name}.png`);
	fs.writeFileSync(file, png);
	shots.push({ file, lang, group, title, note });
	console.log('  shot ' + path.basename(file));
}

/** Clicks an element in Blockbench by CSS selector (real DOM click, Vue handlers run). */
const click = (cdp, sel) => cdp.run((s) => {
	const el = document.querySelector(s);
	if (!el) throw new Error('not found: ' + s);
	el.click();
	return true;
}, sel);
const type = (cdp, sel, text) => cdp.run((s, t) => {
	const el = document.querySelector(s);
	el.value = t;
	el.dispatchEvent(new Event('input', { bubbles: true }));
	return true;
}, sel, text);

async function run(lang) {
	console.log(`\n== ${lang}`);
	const gameDir = fs.mkdtempSync(path.join(os.tmpdir(), 'vibey-game-'));
	const bb = await launchBlockbench({ lang, width: W, height: H, exe: opt('blockbench') });
	const { cdp } = bb;
	let game = null;
	try {
		await cdp.run(() => {
			// isolated profile: no game yet, default dirs
			localStorage.removeItem('vibey.prefs');
			return true;
		});
		await cdp.run(async (file) => {
			await new Plugin().loadFromFile({ path: file, name: 'vibey.js', content: '' }, false);
			return true;
		}, PLUGIN);
		await cdp.run((dir) => {
			Object.assign(VibeyPlugin.LinkSettings, { autoFind: true, gameDir: dir, autoPush: false });
			VibeyPlugin.saveLinkSettings();
			return true;
		}, gameDir);
		// first start: the wizard opens by itself
		await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_wizard', 8000);
		const de = lang === 'de';
		await shoot(cdp, lang, '01-wizard-type', 'Wizard', de ? 'Schritt 1 – Art (öffnet sich beim ersten Start)' : 'Step 1 – type (opens by itself on the first start)', '', { selector: '#vibey_wizard' });
		await click(cdp, '#vibey_wizard .vw_card[data-value=cosmetic]');
		await sleep(300);
		await shoot(cdp, lang, '02-wizard-slot', 'Wizard', de ? 'Schritt 2 – Platz' : 'Step 2 – slot', '', { selector: '#vibey_wizard' });
		await click(cdp, '#vibey_wizard .vw_card[data-value=wings]');
		await sleep(300);
		await type(cdp, '#vibey_wizard input.vw_name_en', 'Frost Wings');
		await type(cdp, '#vibey_wizard input.vw_name_de', 'Frostflügel');
		await click(cdp, '#vibey_wizard .vw_adv_toggle');
		await shoot(cdp, lang, '03-wizard-name', 'Wizard', de ? 'Schritt 3 – Name (ID automatisch, unter „Erweitert“ änderbar)' : 'Step 3 – name (id generated, editable under "Advanced")', '', { selector: '#vibey_wizard' });
		await click(cdp, '#vibey_wizard .vw_next');
		await sleep(300);
		await shoot(cdp, lang, '04-wizard-template', 'Wizard', de ? 'Schritt 4 – leer oder aus einem Starter-Item' : 'Step 4 – start empty or from a starter item', '', { selector: '#vibey_wizard' });
		await click(cdp, '#vibey_wizard .vw_next');
		await cdp.waitFor(() => !Dialog.open && typeof Format !== 'undefined' && Format && Format.id === 'vibey_cosmetic', 8000);
		await sleep(900);
		await shoot(cdp, lang, '05-guide-model', 'Guide', de ? 'Nach „Erstellen“: Referenzspieler, Gruppen, Animationen, Textur und die Anleitung' : 'After "Create": reference player, groups, animations, texture and the guide', de ? 'Prüfung schlägt fehl: noch keine Würfel' : 'Failing check: no cubes yet', { keepToast: true });

		// model a little wing so the next checks pass
		await cdp.run(() => {
			const r = Group.all.find((g) => g.name === 'wing_right');
			const l = Group.all.find((g) => g.name === 'wing_left');
			new Cube({ name: 'feather_r', from: [1.5, 12, 2.5], to: [11.5, 24, 3.5], box_uv: true, uv_offset: [0, 0], rotation: [0, -20, 0], origin: [1.5, 21, 2.5] }).addTo(r).init();
			new Cube({ name: 'feather_l', from: [-11.5, 12, 2.5], to: [-1.5, 24, 3.5], box_uv: true, uv_offset: [0, 0], mirror_uv: true, rotation: [0, 20, 0], origin: [-1.5, 21, 2.5] }).addTo(l).init();
			Canvas.updateAll();
			VibeyPlugin.Guide.next(0);
			return true;
		});
		await shoot(cdp, lang, '06-guide-texture', 'Guide', de ? 'Schritt 2 – Textur noch die Vorlage' : 'Step 2 – texture is still the template', '');
		await cdp.run(() => {
			const tex = Texture.all[0];
			const ctx = tex.canvas.getContext('2d');
			for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) {
				ctx.fillStyle = `hsl(${190 + ((x + y) % 24)}, 80%, ${60 + ((x * 3 + y) % 20)}%)`;
				ctx.fillRect(x, y, 1, 1);
			}
			tex.updateChangesAfterEdit ? tex.updateChangesAfterEdit() : tex.updateSource(tex.canvas.toDataURL());
			VibeyPlugin.Guide.next(1);
			return true;
		});
		await shoot(cdp, lang, '07-guide-animate', 'Guide', de ? 'Schritt 3 – noch keine Keyframes in idle' : 'Step 3 – no keyframes in idle yet', '');
		await cdp.run(() => {
			const anim = Animator.animations.find((a) => a.name === 'idle');
			const g = Group.all.find((x) => x.name === 'wing_right');
			anim.getBoneAnimator(g).addKeyframe({ channel: 'rotation', time: 0, data_points: [{ x: 0, y: 0, z: 0 }] });
			anim.getBoneAnimator(g).addKeyframe({ channel: 'rotation', time: 1.2, data_points: [{ x: 0, y: -15, z: 0 }] });
			VibeyPlugin.Guide.next(2);
			VibeyPlugin.Guide.next(3);
			VibeyPlugin.refreshUI();
			return true;
		});
		await shoot(cdp, lang, '08-guide-live-off', 'Test live', de ? '„Live im Spiel testen“ – nicht verbunden: Anleitung in einfachen Worten' : '"Test live in game" – not connected: plain-words how-to', '');
		await click(cdp, '.vibey_guide .vg_live');
		await cdp.waitFor(() => Dialog.open && Dialog.open.id === 'vibey_friendly', 8000);
		await cdp.run(() => {
			const d = document.querySelector('#vibey_friendly details');
			if (d) d.open = true;
			return true;
		});
		await shoot(cdp, lang, '09-friendly-unreachable', 'Friendly errors', de ? 'Freundlicher Fehler: Spiel nicht gefunden (Details aufgeklappt)' : 'Friendly error: game not found (details expanded)', '', { selector: '#vibey_friendly' });
		await cdp.eval('Dialog.open && Dialog.open.cancel(), true');
		// start the mock game: the plugin keeps looking and pushes once it is there
		game = await createMockGame({ player: 'Henny', game: '1.21.11' });
		game.writeDevlink(gameDir);
		await cdp.waitFor(() => VibeyPlugin.Link.state === 'connected', 20000);
		await cdp.waitFor(() => VibeyPlugin.sessionOf().pushed, 10000);
		await cdp.run(() => {
			VibeyPlugin.getMeta().tutorial.step = 4;
			VibeyPlugin.refreshUI();
			return true;
		});
		await shoot(cdp, lang, '10-guide-live-connected', 'Test live', de ? 'Verbunden und gesendet (Mock-Spiel)' : 'Connected and sent (mock game)', '', { keepToast: true });
		await cdp.run(() => (VibeyPlugin.Guide.next(4), true));
		await shoot(cdp, lang, '11-guide-export', 'Guide', de ? 'Schritt 6 – exportieren oder einreichen' : 'Step 6 – export or submit', '');

		// menu
		await cdp.run(() => {
			const m = MenuBar.menus.vibey;
			m.open();
			return true;
		});
		await sleep(300);
		await cdp.run(() => {
			const li = [...document.querySelectorAll('ul.contextMenu li')].find((x) => x.getAttribute('menu_item') === 'vibey_advanced' || /Advanced|Erweitert/.test(x.textContent));
			if (li) li.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
			return true;
		});
		await shoot(cdp, lang, '12-menu', 'Menu', de ? 'Das Vibey-Menü mit „Erweitert ▸“' : 'The Vibey menu with "Advanced ▸"', '', { clip: { x: 0, y: 0, width: 1180, height: 620 }, wait: 600 });
		await cdp.run(() => (typeof open_menu !== 'undefined' && open_menu && open_menu.hide(), true));

		// export / submit dialog
		await cdp.run(() => (VibeyPlugin.exportSubmitDialog(), true));
		await shoot(cdp, lang, '13-export-submit', 'Export', de ? 'Exportieren / Einreichen…' : 'Export / Submit…', '', { selector: '#vibey_export_submit' });
		await cdp.eval('Dialog.open && Dialog.open.cancel(), true');

		// friendly validation problems
		await cdp.run(() => {
			const meta = VibeyPlugin.getMeta();
			meta.id = 'Frost Wings';
			new Cube({ name: 'stray', from: [0, 0, 0], to: [1, 1, 1] }).addTo().init();
			VibeyPlugin.exportSubmitDialog();
			return true;
		});
		await cdp.run(() => {
			const d = document.querySelector('#vibey_problems details');
			if (d) d.open = true;
			return true;
		});
		await shoot(cdp, lang, '14-friendly-problems', 'Friendly errors', de ? 'Export blockiert – verständlich erklärt' : 'Export blocked – explained in plain words', '', { selector: '#vibey_problems' });
		await cdp.eval('Dialog.open && Dialog.open.cancel(), true');

		// other wizard variants: emote options + cape template, shield preview
		await cdp.run(() => (VibeyPlugin.openWizard(), true));
		await click(cdp, '#vibey_wizard .vw_card[data-value=emote]');
		await sleep(300);
		await shoot(cdp, lang, '15-wizard-emote-options', 'Wizard', de ? 'Emote: Schritt 2 – Optionen' : 'Emote: step 2 – options', '', { selector: '#vibey_wizard' });
		await cdp.eval('Dialog.open && Dialog.open.cancel(), true');
		await cdp.run(() => (VibeyPlugin.createItem({ kind: 'cosmetic', slot: 'cape', name: { en: 'Night Cape' } }), true));
		await sleep(900);
		await cdp.run(() => {
			Preview.selected.loadAnglePreset({ position: [30, 30, 62], target: [0, 16, 0] });
			return true;
		});
		await shoot(cdp, lang, '16-guide-cape', 'Guide', de ? 'Umhang: nur Textur – Vorschau-Umhang mit Farb-Hilfe (wird nicht exportiert)' : 'Cape: texture only – preview cape with a colour guide (not exported)', '');
		await cdp.run(() => (VibeyPlugin.createItem({ kind: 'cosmetic', slot: 'shield', name: { en: 'Night Shield' } }), true));
		await sleep(900);
		await shoot(cdp, lang, '17-guide-shield', 'Guide', de ? 'Schild: Vorschau neben dem Spieler' : 'Shield: preview next to the player', '');
		await cdp.run(() => (VibeyPlugin.createItem({ kind: 'emote', name: { en: 'Happy Hop' }, loop: true }), true));
		await sleep(900);
		await cdp.run(() => (VibeyPlugin.getMeta().tutorial.step = 2, VibeyPlugin.refreshUI(), true));
		await shoot(cdp, lang, '18-guide-emote', 'Guide', de ? 'Emote: Spieler-Rig und leere Animation main' : 'Emote: player rig and an empty "main" animation', '');
	} finally {
		await bb.close();
		if (game) await game.close();
		fs.rmSync(gameDir, { recursive: true, force: true });
	}
}

function writeHtml() {
	const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
	const groups = [];
	shots.forEach((s) => {
		const key = s.lang + ' · ' + s.group;
		let g = groups.find((x) => x.key === key);
		if (!g) groups.push((g = { key, lang: s.lang, group: s.group, items: [] }));
		g.items.push(s);
	});
	const img = (s) => 'data:image/png;base64,' + fs.readFileSync(s.file).toString('base64');
	const body = groups.map((g) => `
<section>
	<h2><span class="lang">${esc(g.lang.toUpperCase())}</span>${esc(g.group)}</h2>
	<div class="grid">${g.items.map((s) => `
		<figure><a href="#${esc(path.basename(s.file, '.png'))}"><img src="${img(s)}" alt="${esc(s.title)}" loading="lazy"></a>
			<figcaption><b>${esc(s.title)}</b>${s.note ? `<span>${esc(s.note)}</span>` : ''}</figcaption></figure>`).join('')}
	</div>
</section>`).join('');
	const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Vibey Wizard Review</title>
<style>
:root { --bg: #0e0d14; --panel: #17151f; --line: #2a2738; --text: #ecebf5; --dim: #9a97ab; --vibe: #8b6cff; --teal: #3fe0c5; }
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--text); font: 15px/1.5 system-ui, "Segoe UI", sans-serif; }
header { padding: 32px 24px 20px; border-bottom: 3px solid var(--vibe); background: linear-gradient(90deg, rgba(139,108,255,.18), rgba(63,224,197,.08)); }
header h1 { margin: 0 0 6px; font-size: 26px; } header p { margin: 0; color: var(--dim); max-width: 900px; }
main { padding: 8px 24px 48px; max-width: 1500px; margin: 0 auto; }
section { margin-top: 28px; }
h2 { display: flex; align-items: center; gap: 10px; font-size: 18px; margin: 0 0 12px; padding-bottom: 6px; border-bottom: 1px solid var(--line); }
.lang { background: var(--teal); color: #0e0d14; font-size: 12px; padding: 1px 6px; font-weight: 700; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(420px, 1fr)); gap: 16px; }
figure { margin: 0; background: var(--panel); border: 1px solid var(--line); }
figure img { display: block; width: 100%; height: auto; background: #000; }
figcaption { padding: 10px 12px; border-top: 2px solid var(--vibe); } figcaption b { display: block; } figcaption span { color: var(--dim); font-size: 13px; }
a:target img, figure:hover { outline: 2px solid var(--vibe); }
@media (max-width: 520px) { .grid { grid-template-columns: 1fr; } main, header { padding-left: 16px; padding-right: 16px; } }
</style></head><body>
<header><h1>Vibey Blockbench plugin ${esc(require('../package.json').version)} – wizard, guide, menu, friendly errors</h1>
<p>Screenshots from the real Blockbench (isolated profile) with the mock game server, generated by <code>npm run shots</code> on ${esc(new Date().toISOString().slice(0, 16).replace('T', ' '))} UTC.</p></header>
<main>${body}</main></body></html>
`;
	fs.mkdirSync(path.dirname(HTML), { recursive: true });
	fs.writeFileSync(HTML, html);
	console.log(`\nwrote ${HTML} (${Math.round(html.length / 1024)} KiB, ${shots.length} screenshots)`);
}

(async () => {
	fs.mkdirSync(OUT, { recursive: true });
	const langs = opt('lang') ? [opt('lang')] : ['en', 'de'];
	for (const lang of langs) await run(lang);
	writeHtml();
})().catch((e) => {
	console.error(e);
	if (shots.length) writeHtml();
	process.exit(1);
});
