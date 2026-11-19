#!/usr/bin/env node
// Builds `vibey-blockbench.json` for a standalone plugin release (docs/BLOCKBENCH.md "Releases"):
//
//   { "version", "file": "vibey.js", "sha256", "size", "min_client", "notes" }
//
// sha256/size describe the given vibey.js byte for byte (the file that is uploaded next to the json; CI takes it
// from the git blob, so it is LF). notes = the "Kurz:" line of the version's section in blockbench/CHANGELOG.md
// (German, short); --notes-out writes the whole section as the GitHub release text. min_client comes from
// blockbench/package.json ("vibey": { "minClient" }).
//
//   node blockbench/tools/release-json.mjs                                   # check + print for the current version
//   node blockbench/tools/release-json.mjs --tag blockbench-v0.2.4 --plugin build/blockbench/vibey.js \
//        --out build/blockbench/vibey-blockbench.json --notes-out build/blockbench/notes.md
//
// Fails when the tag, package.json, package-lock.json and VERSION in vibey.js disagree, the file has CRLF line
// endings, or the changelog has no section / no "Kurz:" line for the version.
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const BB = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const VERSION_RE = /^\d+\.\d+\.\d+$/;
export const TAG_PREFIX = 'blockbench-v';
export const MAX_NOTES = 300;

export function parseArgs(argv) {
  const a = { tag: null, plugin: null, out: null, notesOut: null, changelog: null, root: null, quiet: false };
  for (let i = 0; i < argv.length; i++) {
    const k = argv[i], v = () => {
      if (i + 1 >= argv.length) throw new Error(`${k} needs a value`);
      return argv[++i];
    };
    if (k === '--tag') a.tag = v();
    else if (k === '--plugin') a.plugin = v();
    else if (k === '--out') a.out = v();
    else if (k === '--notes-out') a.notesOut = v();
    else if (k === '--changelog') a.changelog = v();
    else if (k === '--root') a.root = v();
    else if (k === '--quiet') a.quiet = true;
    else if (k === '--help' || k === '-h') a.help = true;
    else throw new Error(`unknown argument ${k}`);
  }
  return a;
}

/** "blockbench-v0.2.4" / "refs/tags/blockbench-v0.2.4" → "0.2.4"; anything else throws. */
export function tagVersion(tag) {
  const t = String(tag).replace(/^refs\/tags\//, '');
  if (!t.startsWith(TAG_PREFIX)) throw new Error(`tag "${t}" does not start with ${TAG_PREFIX}`);
  const v = t.slice(TAG_PREFIX.length);
  if (!VERSION_RE.test(v)) throw new Error(`tag "${t}": "${v}" is not x.y.z`);
  return v;
}

/** The "## <version>" section of the changelog without its heading, or "". */
export function changelogSection(text, version) {
  const lines = text.split(/\r?\n/);
  const esc = version.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const start = lines.findIndex((l) => new RegExp(`^##\\s+v?${esc}(\\s|$)`).test(l));
  if (start < 0) return '';
  let end = lines.findIndex((l, i) => i > start && /^##\s/.test(l));
  if (end < 0) end = lines.length;
  return lines.slice(start + 1, end).join('\n').trim();
}

/** The "Kurz: …" line of a section (German short notes) and the section without it (release text). */
export function splitNotes(section) {
  const lines = section.split('\n');
  const i = lines.findIndex((l) => /^Kurz:\s*\S/.test(l.trim()));
  if (i < 0) return { notes: '', body: section.trim() };
  const notes = lines[i].trim().replace(/^Kurz:\s*/, '').trim();
  const body = lines.filter((_, j) => j !== i).join('\n').trim();
  return { notes, body };
}

export function sha256(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

/**
 * Checks the versions and builds { json, body }. pluginBytes: the exact bytes that are uploaded as vibey.js.
 * pkg/lock: parsed blockbench/package.json + package-lock.json.
 */
export function buildReleaseJson({ pluginBytes, pkg, lock, changelogText, tag }) {
  const problems = [];
  const version = pkg.version;
  if (!VERSION_RE.test(String(version))) problems.push(`package.json version "${version}" is not x.y.z`);
  if (tag) {
    const tv = tagVersion(tag);
    if (tv !== version) problems.push(`tag ${tag} does not match blockbench/package.json version ${version}`);
  }
  if (lock && (lock.version !== version || (lock.packages && lock.packages[''] && lock.packages[''].version !== version)))
    problems.push(`blockbench/package-lock.json says ${lock.version}, package.json ${version} (run npm install)`);
  const src = pluginBytes.toString('utf8');
  const m = /const VERSION\s*=\s*['"]([^'"]+)['"]/.exec(src);
  if (!m) problems.push('vibey.js has no `const VERSION = …`');
  else if (m[1] !== version) problems.push(`vibey.js says VERSION ${m[1]} but package.json ${version}`);
  if (src.includes('\r')) problems.push('vibey.js has CRLF line endings; the release carries the committed LF bytes (see .gitattributes)');
  const minClient = pkg.vibey && pkg.vibey.minClient;
  if (!minClient || !VERSION_RE.test(String(minClient))) problems.push('blockbench/package.json needs "vibey": { "minClient": "x.y.z" }');
  const section = changelogSection(changelogText || '', version);
  const { notes, body } = splitNotes(section);
  if (!section) problems.push(`blockbench/CHANGELOG.md has no "## ${version}" section`);
  else if (!notes) problems.push(`blockbench/CHANGELOG.md "## ${version}" has no "Kurz: …" line (German short notes)`);
  else if (notes.length > MAX_NOTES) problems.push(`"Kurz:" line of ${version} is ${notes.length} characters, at most ${MAX_NOTES}`);
  if (problems.length) throw new Error(`release check failed:\n  ${problems.join('\n  ')}`);
  const json = { version, file: 'vibey.js', sha256: sha256(pluginBytes), size: pluginBytes.length, min_client: minClient, notes };
  return { json, body: body || notes };
}

function main() {
  const a = parseArgs(process.argv.slice(2));
  if (a.help) {
    console.log(readFileSync(fileURLToPath(import.meta.url), 'utf8').split('\n').filter((l) => l.startsWith('//')).map((l) => l.slice(3)).join('\n'));
    return;
  }
  const bb = a.root ? resolve(a.root) : BB;
  const plugin = a.plugin ? resolve(a.plugin) : join(bb, 'vibey.js');
  const { json, body } = buildReleaseJson({
    pluginBytes: readFileSync(plugin),
    pkg: JSON.parse(readFileSync(join(bb, 'package.json'), 'utf8')),
    lock: JSON.parse(readFileSync(join(bb, 'package-lock.json'), 'utf8')),
    changelogText: readFileSync(a.changelog ? resolve(a.changelog) : join(bb, 'CHANGELOG.md'), 'utf8'),
    tag: a.tag,
  });
  const text = JSON.stringify(json, null, 2) + '\n';
  if (a.out) {
    mkdirSync(dirname(resolve(a.out)), { recursive: true });
    writeFileSync(resolve(a.out), text);
  }
  if (a.notesOut) {
    mkdirSync(dirname(resolve(a.notesOut)), { recursive: true });
    writeFileSync(resolve(a.notesOut), body + '\n');
  }
  if (!a.quiet) process.stdout.write(text);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    main();
  } catch (e) {
    console.error(`release-json: ${e.message}`);
    process.exit(1);
  }
}
