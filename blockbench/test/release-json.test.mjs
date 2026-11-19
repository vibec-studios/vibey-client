// Tests for tools/release-json.mjs (vibey-blockbench.json of a standalone plugin release).
import test from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdtempSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildReleaseJson, changelogSection, splitNotes, tagVersion } from '../tools/release-json.mjs';

const BB = join(dirname(fileURLToPath(import.meta.url)), '..');
const SCRIPT = join(BB, 'tools', 'release-json.mjs');
const CHANGELOG = '# Changelog\n\n## 1.2.3\n\nKurz: Kurze Notiz.\n\n- English entry\n\n## 1.2.2\n\nKurz: Alt.\n';
const plugin = (v, eol = '\n') => Buffer.from(`/** header */${eol}const VERSION = '${v}';${eol}more();${eol}`);
const pkg = (v, minClient = '0.2.0') => ({ version: v, vibey: { minClient } });
const lock = (v) => ({ version: v, packages: { '': { version: v } } });

test('tag version', () => {
  assert.equal(tagVersion('blockbench-v0.2.4'), '0.2.4');
  assert.equal(tagVersion('refs/tags/blockbench-v1.0.0'), '1.0.0');
  assert.throws(() => tagVersion('v0.2.4'), /does not start with blockbench-v/);
  assert.throws(() => tagVersion('blockbench-v0.2'), /not x\.y\.z/);
});

test('changelog section and short German notes', () => {
  const s = changelogSection(CHANGELOG, '1.2.3');
  assert.equal(s, 'Kurz: Kurze Notiz.\n\n- English entry');
  assert.deepEqual(splitNotes(s), { notes: 'Kurze Notiz.', body: '- English entry' });
  assert.equal(changelogSection(CHANGELOG, '9.9.9'), '');
});

test('json: field order, sha256 and size of the exact bytes', () => {
  const bytes = plugin('1.2.3');
  const { json, body } = buildReleaseJson({ pluginBytes: bytes, pkg: pkg('1.2.3'), lock: lock('1.2.3'), changelogText: CHANGELOG, tag: 'blockbench-v1.2.3' });
  assert.deepEqual(Object.keys(json), ['version', 'file', 'sha256', 'size', 'min_client', 'notes']);
  assert.deepEqual(json, {
    version: '1.2.3', file: 'vibey.js', sha256: createHash('sha256').update(bytes).digest('hex'), size: bytes.length, min_client: '0.2.0', notes: 'Kurze Notiz.',
  });
  assert.equal(body, '- English entry');
});

test('refuses mismatching versions, CRLF, missing changelog/notes/minClient', () => {
  const ok = { pluginBytes: plugin('1.2.3'), pkg: pkg('1.2.3'), lock: lock('1.2.3'), changelogText: CHANGELOG, tag: 'blockbench-v1.2.3' };
  assert.throws(() => buildReleaseJson({ ...ok, tag: 'blockbench-v1.2.4' }), /does not match blockbench\/package.json version 1.2.3/);
  assert.throws(() => buildReleaseJson({ ...ok, pluginBytes: plugin('1.2.2') }), /VERSION 1.2.2/);
  assert.throws(() => buildReleaseJson({ ...ok, lock: lock('1.2.2') }), /package-lock/);
  assert.throws(() => buildReleaseJson({ ...ok, pluginBytes: plugin('1.2.3', '\r\n') }), /CRLF/);
  assert.throws(() => buildReleaseJson({ ...ok, changelogText: '# nothing' }), /no "## 1.2.3" section/);
  assert.throws(() => buildReleaseJson({ ...ok, changelogText: '## 1.2.3\n\n- only English\n' }), /no "Kurz: …" line/);
  assert.throws(() => buildReleaseJson({ ...ok, pkg: { version: '1.2.3' } }), /minClient/);
  // without a tag (local check) the versions must still agree
  assert.equal(buildReleaseJson({ ...ok, tag: null }).json.version, '1.2.3');
});

test('CLI writes the json and the release text', () => {
  const dir = mkdtempSync(join(tmpdir(), 'vibey-bb-release-'));
  mkdirSync(join(dir, 'bb'));
  writeFileSync(join(dir, 'bb', 'vibey.js'), plugin('1.2.3'));
  writeFileSync(join(dir, 'bb', 'package.json'), JSON.stringify(pkg('1.2.3', '0.2.0')));
  writeFileSync(join(dir, 'bb', 'package-lock.json'), JSON.stringify(lock('1.2.3')));
  writeFileSync(join(dir, 'bb', 'CHANGELOG.md'), CHANGELOG);
  const out = join(dir, 'out', 'vibey-blockbench.json'), notes = join(dir, 'out', 'notes.md');
  execFileSync(process.execPath, [SCRIPT, '--root', join(dir, 'bb'), '--tag', 'blockbench-v1.2.3', '--out', out, '--notes-out', notes, '--quiet']);
  const json = JSON.parse(readFileSync(out, 'utf8'));
  assert.equal(json.sha256, createHash('sha256').update(plugin('1.2.3')).digest('hex'));
  assert.equal(readFileSync(notes, 'utf8'), '- English entry\n');
  assert.throws(() => execFileSync(process.execPath, [SCRIPT, '--root', join(dir, 'bb'), '--tag', 'blockbench-v9.9.9', '--quiet'], { stdio: 'pipe' }), /does not match/);
});

test('real repo: versions agree, changelog has the current version, vibey.js is LF', () => {
  const p = JSON.parse(readFileSync(join(BB, 'package.json'), 'utf8'));
  const { json } = buildReleaseJson({
    pluginBytes: readFileSync(join(BB, 'vibey.js')),
    pkg: p,
    lock: JSON.parse(readFileSync(join(BB, 'package-lock.json'), 'utf8')),
    changelogText: readFileSync(join(BB, 'CHANGELOG.md'), 'utf8'),
    tag: `blockbench-v${p.version}`,
  });
  assert.equal(json.version, p.version);
  assert.ok(json.notes.length > 10);
});
