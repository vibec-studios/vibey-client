#!/usr/bin/env node
'use strict';
/**
 * Review page "Direkt am Spieler bauen" (build on the player): before/after screenshots from the REAL Blockbench
 * (isolated profile) – the released 0.2.4 plugin (from the git tag blockbench-v0.2.4) against this checkout – plus
 * the in-game screenshots of test/ingame-check.js. Writes one self-contained page (JPEGs embedded):
 * blockbench/review/build-on-player.html (git-ignored).
 *
 *   node test/player-shots.js [--lang de] [--ingame test/out/ingame] [--blockbench <Blockbench.exe>]
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');
const { launchBlockbench, sleep } = require('./bb-harness');

const args = process.argv.slice(2);
const opt = (n, d) => (args.indexOf('--' + n) >= 0 ? args[args.indexOf('--' + n) + 1] : d);
const ROOT = path.resolve(__dirname, '..');
const PLUGIN = path.join(ROOT, 'vibey.js');
const LANG = opt('lang', 'de');
const INGAME = path.resolve(opt('ingame', path.join(__dirname, 'out', 'ingame')));
const HTML = path.resolve(opt('out', path.join(ROOT, 'review', 'build-on-player.html')));
const VERSION = require('../package.json').version;

const shots = {}; // key -> { before: [jpeg b64…], after: […] }
const put = (key, side, item) => ((shots[key] = shots[key] || { before: [], after: [] })[side].push(item));

async function jpeg(cdp, clip, quality) {
	const params = { format: 'jpeg', quality: quality || 80 };
	if (clip) params.clip = Object.assign({ scale: 1 }, clip);
	return (await cdp.send('Page.captureScreenshot', params)).data;
}
async function shoot(cdp, key, side, title, opts = {}) {
	await sleep(opts.wait || 500);
	if (!opts.keepToast) await cdp.run(() => (document.querySelectorAll('#quick_message_box, .quick_message_box, .toast_notification').forEach((e) => e.remove()), true));
	let clip = opts.clip || null;
	if (opts.selector) {
		clip = await cdp.run((sel, pad) => {
			const el = document.querySelector(sel);
			if (!el) return null;
			const r = el.getBoundingClientRect();
			return { x: Math.max(0, r.left - pad), y: Math.max(0, r.top - pad), width: Math.min(window.innerWidth, r.width + pad * 2), height: Math.min(window.innerHeight, r.height + pad * 2) };
		}, opts.selector, opts.pad === undefined ? 10 : opts.pad);
	}
	put(key, side, { title, img: await jpeg(cdp, clip, opts.quality) });
	console.log('  ' + side + ' · ' + key + ' · ' + title);
}
const size = (cdp, w, h) => cdp.send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: 1, mobile: false });
const scale = (cdp, z) => cdp.run((z) => {
	settings.interface_scale.set ? settings.interface_scale.set(z) : (settings.interface_scale.value = z);
	if (settings.interface_scale.onChange) settings.interface_scale.onChange();
	window.dispatchEvent(new Event('resize'));
	return true;
}, z);
const angle = (cdp, pos, target) => cdp.run((p, t) => (Preview.selected.loadAnglePreset({ position: p, target: t }), true), pos, target);

async function loadPlugin(cdp, file, wantOld) {
	await cdp.run(async (file, wantOld) => {
		localStorage.setItem('vibey.prefs', JSON.stringify({ welcomed: true, guide: true, namespace: 'creator' }));
		await new Plugin().loadFromFile({ path: file, name: 'vibey.js', content: '' }, false);
		for (let i = 0; i < 60 && !(window.VibeyPlugin && (VibeyPlugin.VERSION === '0.2.4') === wantOld); i++) await new Promise((r) => setTimeout(r, 100));
		if (Dialog.open) Dialog.open.close();
		return VibeyPlugin.VERSION;
	}, file, wantOld);
}
async function closeAll(cdp) {
	await cdp.run(async () => {
		if (Dialog.open) Dialog.open.close();
		while (ModelProject.all.length) await ModelProject.all[0].close(true);
		return true;
	});
}
async function create(cdp, opts) {
	await cdp.run(async (o) => {
		VibeyPlugin.createItem(o);
		for (let i = 0; i < 80 && !(Texture.all.length && Texture.all.every((t) => t.width)); i++) await new Promise((r) => setTimeout(r, 50));
		Modes.options.edit.select();
		return true;
	}, opts);
	await sleep(500);
}
/** Selects a group by name and shows the Element panel (where 0.2.4 had "Attach to"). */
const selectGroup = (cdp, name, element) => cdp.run((name, element) => {
	const g = Group.all.find((x) => x.name === name);
	if (!g) return false;
	g.select();
	g.openUp && g.openUp();
	updateSelection();
	if (element) try { Panels.element.getHostPanel().selectTab(Panels.element); } catch (e) {}
	return true;
}, name, element);

async function guideMatrix(cdp, side) {
	await create(cdp, { kind: 'cosmetic', slot: 'wings', name: { en: 'Frost Wings', de: 'Frostflügel' } });
	for (const [w, h] of [[1280, 720], [1366, 768]]) {
		for (const z of [100, 125, 150]) {
			await size(cdp, w, h);
			await scale(cdp, z);
			await sleep(700);
			await cdp.run(async () => {
				const meta = VibeyPlugin.getMeta();
				meta.tutorial.step = 4; // "Test live in game" – the longest step
				VibeyPlugin.refreshUI();
				await new Promise((r) => setTimeout(r, 900));
				return true;
			});
			const info = await cdp.run(() => {
				const box = document.querySelector('.vibey_guide');
				const r = box.getBoundingClientRect();
				return { bottom: Math.round(r.bottom), vh: innerHeight, scroll: box.scrollHeight > box.clientHeight + 2 && getComputedStyle(box).overflowY === 'auto' };
			});
			const note = info.bottom > info.vh ? `Anleitung endet bei y=${info.bottom}, Fenster nur ${info.vh} hoch – nicht scrollbar` : info.scroll ? 'passt ins Fenster, scrollt, aktueller Schritt sichtbar' : 'passt ins Fenster';
			await shoot(cdp, `guide_${w}x${h}_${z}`, side, `${w}×${h} bei ${z} % – ${note}`, { quality: 72 });
		}
	}
	await scale(cdp, 100);
	await size(cdp, 1440, 900);
	await sleep(500);
	await closeAll(cdp);
}

async function before(cdp, oldPlugin) {
	console.log('\nBEFORE (0.2.4)');
	await loadPlugin(cdp, oldPlugin, true);
	await size(cdp, 1440, 900);
	// anchors: the old "Attach to" setting per root group
	await create(cdp, { kind: 'cosmetic', slot: 'hat', template: 'seal_beanie', name: { en: 'Seal Beanie', de: 'Robbenmütze' } });
	await selectGroup(cdp, 'beanie', true);
	await angle(cdp, [-34, 40, -52], [0, 26, 0]);
	await shoot(cdp, 'anchors', 'before', 'Mütze: Gruppe ganz oben in der Gliederung, „Befestigen an“ als Auswahl im Element-Panel');
	await cdp.run(() => {
		const el = [...document.querySelectorAll('#panel_element select, #panel_element .bar_select, #panel_element bb-select')].find((x) => /head|Kopf/.test(x.textContent || x.value || ''));
		if (el) el.click();
		return true;
	});
	await shoot(cdp, 'anchors', 'before', '„Befestigen an“ geöffnet: head, body, right_arm, … root, world – Begriffe statt Körperteile', { wait: 600 });
	await cdp.run(() => (document.querySelectorAll('.contextMenu, .bb_select_menu').forEach((m) => m.remove()), true));
	await closeAll(cdp);
	for (const [slot, template, name, group, cam] of [
		['hat', 'seal_beanie', 'Robbenmütze', 'beanie', [[-34, 40, -52], [0, 26, 0]]],
		['wings', 'crystal_wings', 'Kristallflügel', 'wing_left', [[44, 34, 58], [0, 18, 0]]],
		['back', 'seal_backpack', 'Robbenrucksack', 'backpack', [[44, 34, 58], [0, 18, 0]]],
		['pet', 'seal_buddy', 'Robbenkumpel', 'seal', [[-40, 26, -46], [0, 6, 0]]],
	]) {
		await create(cdp, { kind: 'cosmetic', slot, template, name: { en: name, de: name } });
		await selectGroup(cdp, group, true);
		await angle(cdp, cam[0], cam[1]);
		await shoot(cdp, 'place_' + slot, 'before', slot === 'pet' ? 'Haustier am Ursprung, Referenzspieler ausgeblendet (sonst überlappt er)' : 'Teil liegt am Spieler, die Gruppe hängt über „Befestigen an“ (' + group + ')');
		await closeAll(cdp);
	}
	// wizard slot page
	await cdp.run(() => (VibeyPlugin.openWizard({ kind: 'cosmetic' }), true));
	await sleep(700);
	await shoot(cdp, 'wizard_slot', 'before', 'Slot-Auswahl: kein Hinweis, wo das Item sitzt', { selector: '#vibey_wizard', pad: 4 });
	await cdp.run(() => (Dialog.open && Dialog.open.close(), true));
	await create(cdp, { kind: 'cosmetic', slot: 'wings', name: { en: 'Frost Wings', de: 'Frostflügel' } });
	await shoot(cdp, 'wizard_result', 'before', 'Nach dem Assistenten: Gruppe „wings“ ganz oben, Anker im Element-Panel');
	await closeAll(cdp);
	await guideMatrix(cdp, 'before');
}

async function after(cdp) {
	console.log('\nAFTER (' + VERSION + ')');
	await loadPlugin(cdp, PLUGIN, false);
	await size(cdp, 1440, 900);
	await create(cdp, { kind: 'cosmetic', slot: 'hat', template: 'seal_beanie', name: { en: 'Seal Beanie', de: 'Robbenmütze' } });
	await selectGroup(cdp, 'beanie', false);
	await angle(cdp, [-34, 40, -52], [0, 26, 0]);
	await shoot(cdp, 'anchors', 'after', 'Fertige Spielergruppen wie am echten Spieler (türkis, „Spieler“), die Mütze steckt in ◆ Kopf');
	await cdp.run(() => {
		const g = VibeyPlugin.Refs.map();
		['body', 'right_arm', 'head'].forEach((id) => (g[id].isOpen = true));
		return true;
	});
	await shoot(cdp, 'anchors', 'after', 'Untergruppen: ◆ Über dem Kopf, ◆ Rücken, ◆ Schultern, ◆ Hüfte – kein Anker-Feld mehr im Element-Panel', { selector: '#panel_outliner', pad: 0 });
	await closeAll(cdp);
	for (const [slot, template, name, group, cam] of [
		['hat', 'seal_beanie', 'Robbenmütze', 'beanie', [[-34, 40, -52], [0, 26, 0]]],
		['wings', 'crystal_wings', 'Kristallflügel', 'wing_left', [[44, 34, 58], [0, 18, 0]]],
		['back', 'seal_backpack', 'Robbenrucksack', 'backpack', [[44, 34, 58], [0, 18, 0]]],
		['pet', 'seal_buddy', 'Robbenkumpel', 'seal', [[-30, 30, -56], [8, 8, 2]]],
	]) {
		await create(cdp, { kind: 'cosmetic', slot, template, name: { en: name, de: name } });
		await selectGroup(cdp, group, false);
		await angle(cdp, cam[0], cam[1]);
		const where = await cdp.run((n) => VibeyPlugin.refLabel(VibeyPlugin.refOfNode(Group.all.find((g) => g.name === n))), group);
		await shoot(cdp, 'place_' + slot, 'after', slot === 'pet' ? `Haustier steht neben dem Spieler – in ${where}, genau wo das Spiel es zeigt` : `Am Spieler gebaut, „${group}“ steckt in ${where}`);
		if (slot === 'back') {
			// right-click › An … anheften
			await cdp.run(async () => {
				const g = Group.all.find((x) => x.name === 'backpack');
				g.select();
				updateSelection();
				const li = document.getElementById(g.uuid);
				const r = li.querySelector('.outliner_object').getBoundingClientRect();
				g.showContextMenu(new MouseEvent('contextmenu', { clientX: r.left + 40, clientY: r.top + 10 }));
				await new Promise((res) => setTimeout(res, 400));
				const item = [...document.querySelectorAll('.contextMenu li')].find((x) => /anheften|Attach to/.test(x.textContent) && x.querySelector('ul, .contextMenu'));
				const target = item || [...document.querySelectorAll('.contextMenu li')].find((x) => /anheften|Attach to/.test(x.textContent));
				if (target) ['mouseenter', 'mouseover', 'mousemove'].forEach((t) => target.dispatchEvent(new MouseEvent(t, { bubbles: true })));
				await new Promise((res) => setTimeout(res, 500));
				return true;
			});
			await shoot(cdp, 'rightclick', 'after', 'Rechtsklick auf ein Teil › „An … anheften“ › Gruppe wählen (der Punkt zeigt, wo es gerade hängt)', { wait: 300 });
			await cdp.run(() => {
				if (window.open_menu && open_menu.hide) open_menu.hide();
				document.querySelectorAll('.contextMenu').forEach((m) => m.remove());
				const back = VibeyPlugin.Refs.map().back;
				const g = Group.all.find((x) => x.name === 'backpack');
				// a scarf made outside the groups, then dropped onto ◆ Body (what the outliner does on drop)
				const scarf = new Group({ name: 'schal', origin: [0, 24, 0] }).addTo().init();
				new Cube({ name: 'schal', from: [-4.5, 22, -2.5], to: [4.5, 24, 2.5], box_uv: true }).addTo(scarf).init();
				VibeyPlugin.Refs.declined.add(scarf.uuid);
				scarf.select();
				updateSelection();
				Canvas.updateAll();
				VibeyPlugin.Refs.styles();
				return !!back && !!g;
			});
			await shoot(cdp, 'drag', 'after', 'Vorher: der Schal liegt außerhalb der Spielergruppen (die Anleitung sagt es)', { selector: '#panel_outliner', pad: 0 });
			await cdp.run(async () => {
				const scarf = Group.all.find((x) => x.name === 'schal');
				Undo.initEdit({ outliner: true });
				scarf.addTo(VibeyPlugin.Refs.map().body);
				Undo.finishEdit('Drag elements in outliner');
				await new Promise((r) => setTimeout(r, 300));
				VibeyPlugin.Refs.map().body.isOpen = true;
				return true;
			});
			await shoot(cdp, 'drag', 'after', 'Nachher: in der Gliederung auf ◆ Körper gezogen – bewegt sich jetzt mit dem Körper', { selector: '#panel_outliner', pad: 0 });
			// auto-assign: a halo made outside the groups snaps to ◆ Über dem Kopf, with a hint
			await cdp.run(async () => {
				Undo.initEdit({ outliner: true, elements: [] });
				const halo = new Group({ name: 'heiligenschein', origin: [0, 38, 0] }).addTo().init();
				const c = new Cube({ name: 'ring', from: [-3, 37, -3], to: [3, 38, 3], box_uv: true }).addTo(halo).init();
				Undo.finishEdit('add halo', { outliner: true, elements: [c] });
				await new Promise((r) => setTimeout(r, 500));
				Preview.selected.loadAnglePreset({ position: [-34, 44, -52], target: [0, 28, 0] });
				return true;
			});
			await shoot(cdp, 'auto', 'after', 'Neues Teil außerhalb der Gruppen: hängt sich selbst an ◆ Über dem Kopf, mit Hinweis unten (Strg+Z macht es rückgängig)', { keepToast: true, wait: 200 });
		}
		await closeAll(cdp);
	}
	// wizard
	await cdp.run(() => (VibeyPlugin.openWizard({ kind: 'cosmetic' }), true));
	await sleep(700);
	await shoot(cdp, 'wizard_slot', 'after', 'Slot-Auswahl: jede Karte sagt, an welcher Spielergruppe gebaut wird', { selector: '#vibey_wizard', pad: 4 });
	await cdp.run(() => (Dialog.open && Dialog.open.close(), true));
	await create(cdp, { kind: 'cosmetic', slot: 'wings', name: { en: 'Frost Wings', de: 'Frostflügel' } });
	await shoot(cdp, 'wizard_result', 'after', 'Nach dem Assistenten (Flügel): ◆ Rücken lila hervorgehoben, „wings“ ausgewählt, Anleitung sagt wo');
	await closeAll(cdp);
	await create(cdp, { kind: 'cosmetic', slot: 'pet', name: { en: 'Buddy', de: 'Kumpel' } });
	await shoot(cdp, 'wizard_pet', 'after', 'Haustier: ◆ Neben dem Spieler hervorgehoben, Gruppe „pet“ steht am Folgepunkt');
	await closeAll(cdp);
	await create(cdp, { kind: 'emote', name: { en: 'Flag Wave', de: 'Fahne schwenken' }, loop: false, length: 2 });
	await cdp.run(() => (Modes.options.edit.select(), true));
	await shoot(cdp, 'wizard_emote', 'after', 'Emote: dieselben Spielergruppen unter ◆ Ganzer Spieler (die animierbaren Knochen)');
	await closeAll(cdp);
	await guideMatrix(cdp, 'after');
}

async function convertIngame(cdp) {
	if (!fs.existsSync(INGAME)) return [];
	const files = fs.readdirSync(INGAME).filter((f) => f.endsWith('.png')).sort();
	const out = [];
	for (const f of files) {
		const b64 = fs.readFileSync(path.join(INGAME, f)).toString('base64');
		const jpg = await cdp.run(async (b64) => {
			const img = new Image();
			await new Promise((res, rej) => ((img.onload = res), (img.onerror = rej), (img.src = 'data:image/png;base64,' + b64)));
			const c = document.createElement('canvas');
			c.width = img.width;
			c.height = img.height;
			c.getContext('2d').drawImage(img, 0, 0);
			return c.toDataURL('image/jpeg', 0.82).split(',')[1];
		}, b64);
		out.push({ name: f.replace(/\.png$/, ''), img: jpg });
	}
	return out;
}

const INGAME_TEXT = {
	'01_standing_front': 'Stehend von vorn: Antenne auf ◆ Kopf, Kugel auf ◆ Über dem Kopf, Stab in der rechten Hand (◆ Rechter Arm), Band am linken Bein, Pinguin daneben',
	'02_standing_back': 'Stehend von hinten: Rucksack auf ◆ Rücken',
	'03_head_turned_up': 'Kopf gedreht und nach oben: Antenne und Kugel drehen mit dem Kopf, der Körper bleibt',
	'04_head_turned_down': 'Kopf zur anderen Seite und nach unten',
	'05_standing_side': 'Stehend, schräg von hinten (Vergleich zum Schleichen)',
	'06_sneaking_side': 'Schleichen: der Körper neigt sich – der Rucksack (◆ Rücken → body) folgt',
	'06_sneaking_back': 'Schleichen von hinten',
	'08_pet_beside_front': 'Haustier neben dem Spieler (◆ Neben dem Spieler → world, Folgepunkt „ground“)',
	'09_pet_beside_back': 'Haustier von hinten',
};

function writeHtml(ingame, mapping) {
	const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
	const img = (b64, alt) => `<img src="data:image/jpeg;base64,${b64}" alt="${esc(alt)}" loading="lazy">`;
	const fig = (s, cls) => `<figure class="${cls}">${img(s.img, s.title)}<figcaption>${esc(s.title)}</figcaption></figure>`;
	const pair = (key, title, text) => {
		const s = shots[key];
		if (!s) return '';
		const col = (side, label) => `<div class="col ${side}"><div class="tag">${label}</div>${s[side].length ? s[side].map((x) => fig(x, side)).join('') : '<p class="none">– gab es vorher nicht –</p>'}</div>`;
		return `<section><h2>${esc(title)}</h2>${text ? `<p class="lead">${text}</p>` : ''}<div class="pair">${col('before', 'Vorher · 0.2.4')}${col('after', 'Nachher · ' + VERSION)}</div></section>`;
	};
	const guideRows = [[1280, 720], [1366, 768]].map(([w, h]) => [100, 125, 150].map((z) => pair(`guide_${w}x${h}_${z}`, `Anleitung bei ${w}×${h}, Skalierung ${z} %`, '')).join('')).join('');
	const walking = ingame.filter((x) => x.name.startsWith('07_walking'));
	const ingameFigs = ingame.filter((x) => !x.name.startsWith('07_walking')).map((x) => `<figure class="after">${img(x.img, x.name)}<figcaption>${esc(INGAME_TEXT[x.name] || x.name)}</figcaption></figure>`).join('');
	const walkFigs = walking.map((x, i) => `<figure class="after">${img(x.img, x.name)}<figcaption>Laufen, Bild ${i + 1} (≈0,2 s Abstand)</figcaption></figure>`).join('');
	const table = mapping.map((m) => `<tr><td>◆ ${esc(m.de)}</td><td>${esc(m.en)}</td><td><code>${esc(m.attach)}</code></td><td><code>[${m.pivot.join(', ')}]</code></td></tr>`).join('');
	const html = `<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Direkt am Spieler bauen</title>
<style>
:root { --bg: #0e0d14; --panel: #17151f; --line: #2a2738; --text: #ecebf5; --dim: #9a97ab; --vibe: #8b6cff; --teal: #3fe0c5; --red: #ff5470; }
* { box-sizing: border-box; }
html, body { background: var(--bg); }
body { margin: 0; color: var(--text); font: 15px/1.55 system-ui, "Segoe UI", sans-serif; }
header { padding: 34px 24px 22px; border-bottom: 3px solid var(--vibe); background: linear-gradient(90deg, rgba(139,108,255,.20), rgba(63,224,197,.08)); }
header h1 { margin: 0 0 8px; font-size: 28px; } header p { margin: 4px 0; color: var(--dim); max-width: 980px; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; } .chip { padding: 3px 10px; border: 1px solid var(--line); background: var(--panel); font-size: 13px; }
.chip b { color: var(--teal); }
main { padding: 8px 24px 56px; max-width: 1560px; margin: 0 auto; }
section { margin-top: 34px; }
h2 { font-size: 19px; margin: 0 0 6px; padding-bottom: 6px; border-bottom: 1px solid var(--line); }
h3 { font-size: 16px; margin: 26px 0 8px; color: var(--teal); }
.lead { color: var(--dim); margin: 4px 0 12px; max-width: 1000px; }
.pair { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.col { min-width: 0; } .col figure + figure { margin-top: 12px; }
.tag { display: inline-block; font-size: 12px; font-weight: 700; padding: 2px 8px; margin-bottom: 8px; text-transform: uppercase; letter-spacing: .04em; }
.before .tag, .col.before .tag { background: #3a3647; color: var(--text); } .col.after .tag { background: var(--vibe); color: #fff; }
figure { margin: 0; background: var(--panel); border: 1px solid var(--line); }
figure.before { border-top: 3px solid #4a4659; } figure.after { border-top: 3px solid var(--teal); }
figure img { display: block; width: 100%; height: auto; background: #000; }
figcaption { padding: 9px 12px; font-size: 13.5px; color: var(--text); }
.none { color: var(--dim); font-style: italic; padding: 12px; border: 1px dashed var(--line); }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 14px; }
.walk { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
table { border-collapse: collapse; width: 100%; max-width: 980px; background: var(--panel); }
th, td { text-align: left; padding: 6px 10px; border-bottom: 1px solid var(--line); font-size: 14px; } th { color: var(--dim); font-weight: 600; }
code { font-family: Consolas, monospace; color: var(--teal); }
ul.facts { margin: 6px 0; padding-left: 20px; } ul.facts li { margin: 3px 0; }
@media (max-width: 900px) { .pair { grid-template-columns: 1fr; } .walk { grid-template-columns: 1fr 1fr; } }
@media (max-width: 520px) { main, header { padding-left: 16px; padding-right: 16px; } .grid, .walk { grid-template-columns: 1fr; } }
</style></head><body>
<header><h1>Direkt am Spieler bauen – Vibey Blockbench-Plugin ${esc(VERSION)}</h1>
<p>Review vor dem Release <code>blockbench-v${esc(VERSION)}</code>. Die Ankerpunkte sind weg: Der Referenzspieler hat fertige Gruppen wie der echte Spieler. Man baut sein Teil dort, wo es hingehört, und steckt es in die passende Gruppe (ziehen, Rechtsklick › „An … anheften“ oder es hängt sich selbst an). Dazu scrollt die Anleitung jetzt.</p>
<p>Alle Bilder aus dem echten Blockbench (isoliertes Profil, Deutsch) – links das veröffentlichte Plugin 0.2.4, rechts ${esc(VERSION)} – und aus dem laufenden Testspiel (Fabric 1.21.11, Live-Link). Erstellt ${esc(new Date().toISOString().slice(0, 16).replace('T', ' '))} UTC.</p>
<div class="chips"><span class="chip">.vibey-Format <b>unverändert</b></span><span class="chip">min_client <b>0.2.0</b></span><span class="chip">Startset + 0.2.4-Projekte <b>byte-gleich</b></span><span class="chip">Tests EN + DE <b>grün</b></span></div></header>
<main>
<section><h2>Spielergruppen → <code>attach</code> im Export</h2>
<p class="lead">Jede eigene Gruppe direkt in einer Spielergruppe wird ein Wurzelknochen mit diesem <code>attach</code>. Koordinaten bleiben absolut. Würfel direkt in einer Spielergruppe werden zum Knochen <code>&lt;gruppe&gt;_cubes</code>. Die Spielergruppen selbst werden nie exportiert.</p>
<table><tr><th>Gruppe</th><th>English</th><th>attach</th><th>Drehpunkt (Blockbench)</th></tr>${table}</table>
<ul class="facts"><li>Haustiere bauen in ◆ Neben dem Spieler – am Folgepunkt, an dem das Spiel sie zeigt; exportiert wird relativ dazu (wie bisher).</li><li>„Erweitert: dreht sich nur mit dem Körper (root)“ gibt es nur noch im Rechtsklick-Menü.</li></ul></section>
${pair('anchors', 'Ankerpunkte vs. fertige Spielergruppen', 'Vorher musste man für jede Wurzelgruppe im Element-Panel einen technischen Anker wählen. Jetzt sieht man am Spieler, wo es hängt.')}
${pair('place_hat', 'Hut auf den Spieler', '')}
${pair('place_wings', 'Flügel an den Rücken', '')}
${pair('place_back', 'Rucksack auf den Rücken', '')}
${pair('place_pet', 'Haustier neben den Spieler', 'Vorher stand das Haustier am Ursprung im Spieler und der Referenzspieler war ausgeblendet. Jetzt steht es dort, wo es im Spiel läuft.')}
${pair('rightclick', 'Rechtsklick › „An … anheften“', '')}
${pair('drag', 'In der Gliederung in eine Gruppe ziehen', '')}
${pair('auto', 'Automatisch anheften mit kurzem Hinweis', 'Regel: Haustiere → Neben dem Spieler, Auren → Um den Spieler; breiter als der Spieler → Um den Spieler; sonst die Zone, in der die Mitte des Teils liegt, sonst die nächste, sonst die Gruppe des Slots. Strg+Z direkt danach lässt das Teil, wo es war.')}
${pair('wizard_slot', 'Assistent: Slot wählen', '')}
${pair('wizard_result', 'Assistent: Ergebnis', '')}
${pair('wizard_pet', 'Assistent: Haustier', '')}
${pair('wizard_emote', 'Emote: gleiche Gruppen', '')}
<section><h2>Anleitung auf kleinen Bildschirmen</h2><p class="lead">Schritt 5 „Live im Spiel testen“ (der längste) bei 1280×720 und 1366×768 mit 100 / 125 / 150 % Skalierung (Blockbench-Oberflächenskalierung = Zoom-Faktor wie bei Windows-Anzeigeskalierung). Vorher lief sie unten aus dem Fenster und ließ sich nicht scrollen; jetzt scrollt sie und hält den aktuellen Schritt im Blick.</p></section>
${guideRows}
<section><h2>Im Spiel (Fabric 1.21.11, Live-Link)</h2><p class="lead">Ein Test-Item mit Teilen auf ◆ Rücken, ◆ Rechter Arm (Stab in der Hand), ◆ Kopf (Antenne), ◆ Über dem Kopf (Kugel) und ◆ Linkes Bein (Band), dazu ein Pinguin auf ◆ Neben dem Spieler – in Blockbench gebaut und per Live-Link gesendet. Bewegungen kommen nur aus dem Spiel: Kopf per <code>/tp</code>-Blickrichtung, Schleichen durch eine Stufe über dem Kopf (erzwungenes Ducken), Laufen durch eine Wasserströmung (echte Laufanimation).</p>
<div class="grid">${ingameFigs || '<p class="none">keine Spielbilder gefunden</p>'}</div>
<h3>Laufen: Arme und Beine schwingen – der Stab schwingt mit dem rechten Arm, das Band mit dem linken Bein</h3><div class="walk">${walkFigs}</div></section>
</main></body></html>
`;
	fs.mkdirSync(path.dirname(HTML), { recursive: true });
	fs.writeFileSync(HTML, html);
	console.log(`\nwrote ${HTML} (${Math.round(html.length / 1024)} KiB)`);
}

(async () => {
	const src = execFileSync('git', ['show', 'blockbench-v0.2.4:blockbench/vibey.js'], { cwd: ROOT, maxBuffer: 64 * 1024 * 1024 });
	const oldDir = fs.mkdtempSync(path.join(os.tmpdir(), 'vibey-old-plugin-'));
	const oldPlugin = path.join(oldDir, 'vibey.js');
	fs.writeFileSync(oldPlugin, src);
	const bb = await launchBlockbench({ lang: LANG, exe: opt('blockbench'), width: 1440, height: 900 });
	const cdp = bb.cdp;
	let ingame = [];
	let mapping = [];
	try {
		await before(cdp, oldPlugin);
		await after(cdp);
		ingame = await convertIngame(cdp);
		const Core = require('../vibey.js');
		const STR = Core.STRINGS;
		mapping = Core.REF_GROUPS.map((r) => ({ id: r.id, de: STR['ref_' + r.id][1], en: STR['ref_' + r.id][0], attach: r.attach, pivot: r.id === 'beside' ? ['Folgepunkt'] : r.pivot }));
	} finally {
		await bb.close();
		writeHtml(ingame, mapping);
	}
})().catch((e) => {
	console.error(e);
	process.exit(1);
});
