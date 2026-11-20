#!/usr/bin/env node
// Builds the release manifest (vibe-launcher/docs/VIBE-API-2.md §2) from the jars in build/dist.
//
// Normal path (.github/workflows/release.yml): CI attaches every jar and this manifest as `vibey-manifest.json`
// (with --hashes: sha256 + size of every file) to the GitHub release; the Vibe backend imports releases from GitHub
// itself. CI never talks to the backend. The Blockbench plugin is not part of client releases any more: it has its
// own GitHub releases (tag blockbench-v<version>, .github/workflows/blockbench-release.yml), so the manifest says
// `"blockbench": null`.
//
// Manual fallback: --publish uploads straight to the backend from your machine: files staged in batches below the
// proxy limit (POST /internal/client/v1/files), then the release (POST /internal/client/v1/releases, field
// "manifest" with each file's sha256, no file parts). Run it against `./gradlew buildAll` output:
//
//   node tools/release-manifest.mjs                       # check + print the manifest for the current mod_version
//   node tools/release-manifest.mjs --tag v0.2.0 --changelog-file notes.md --hashes --out build/release/vibey-manifest.json
//   node tools/release-manifest.mjs --tag v0.3.0 --prerelease …   # GitHub pre-release → channel "beta"
//   CLIENT_PUBLISH_TOKEN=… node tools/release-manifest.mjs --tag v0.2.0 --publish https://vibe.axolite.app/internal/client/v1/releases
//
// Sources of truth: gradle.properties (mod_version, archives_base_name), versions/<loader>-<line>/gradle.properties
// (minecraft_label → jar name), gradle/mc-versions.json (exact Minecraft versions per line), CHANGELOG.md
// (section "## <version>") unless a changelog file (the GitHub release body) is given.
// The token is only read from the environment and never printed.
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, openAsBlob, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const FABRIC_API = { modrinth: 'P7dR8mSH', name: 'Fabric API' };
const MAX_CHANGELOG = 20000;
const VERSION_RE = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/;

export function parseArgs(argv) {
  const a = { dist: 'build/dist', changelog: null, tag: null, version: null, out: null, publish: null, quiet: false, prerelease: false, hashes: false, root: null };
  for (let i = 0; i < argv.length; i++) {
    const k = argv[i], v = () => {
      if (i + 1 >= argv.length) throw new Error(`${k} needs a value`);
      return argv[++i];
    };
    if (k === '--dist') a.dist = v();
    else if (k === '--root') a.root = v();
    else if (k === '--blockbench') throw new Error('--blockbench: the Blockbench plugin has its own releases now (tag blockbench-v<version>, docs/BLOCKBENCH.md)');
    else if (k === '--changelog-file') a.changelog = v();
    else if (k === '--tag') a.tag = v();
    else if (k === '--version') a.version = v();
    else if (k === '--out') a.out = v();
    else if (k === '--publish') a.publish = v();
    else if (k === '--quiet') a.quiet = true;
    else if (k === '--prerelease') a.prerelease = true;
    else if (k === '--hashes') a.hashes = true;
    else if (k === '--help' || k === '-h') a.help = true;
    else throw new Error(`unknown argument ${k}`);
  }
  return a;
}

export function readProperties(file) {
  const out = {};
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = /^\s*([^#!=:\s][^=:]*?)\s*[=:]\s*(.*)$/.exec(line);
    if (m) out[m[1]] = m[2].trim();
  }
  return out;
}

/** The "## <version>" section of CHANGELOG.md without its heading, or "". */
export function changelogSection(text, version) {
  const lines = text.split(/\r?\n/);
  const esc = version.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const start = lines.findIndex((l) => new RegExp(`^##\\s+v?${esc}(\\s|$)`).test(l));
  if (start < 0) return '';
  let end = lines.findIndex((l, i) => i > start && /^##\s/.test(l));
  if (end < 0) end = lines.length;
  return lines.slice(start + 1, end).join('\n').trim();
}

/** "beta" for a GitHub pre-release or any pre-release version (0.3.0-beta.1, 0.3.0-rc.1), else "release". */
export function channelOf(version, prerelease = false) {
  return prerelease || version.includes('-') ? 'beta' : 'release';
}

/** Every jar the build must produce: one per versions/<loader>-<line> directory. */
export function targets(root = ROOT) {
  const list = JSON.parse(readFileSync(join(root, 'gradle/mc-versions.json'), 'utf8'));
  const out = [];
  for (const dir of readdirSync(join(root, 'versions')).sort()) {
    const props = join(root, 'versions', dir, 'gradle.properties');
    if (!existsSync(props)) continue;
    const dash = dir.indexOf('-');
    const loader = dir.slice(0, dash), line = dir.slice(dash + 1);
    const p = readProperties(props);
    const mcVersions = list[line];
    if (!Array.isArray(mcVersions) || mcVersions.length === 0) throw new Error(`gradle/mc-versions.json has no entry for ${line} (${dir})`);
    if (!p.minecraft_label) throw new Error(`${dir}: minecraft_label missing`);
    out.push({ target: dir, loader, line, label: p.minecraft_label, mcVersions });
  }
  if (out.length === 0) throw new Error('no targets in versions/');
  const num = (s) => s.split('.').map((x) => parseInt(x, 10) || 0);
  const cmp = (a, b) => {
    const x = num(a), y = num(b);
    for (let i = 0; i < Math.max(x.length, y.length); i++) if ((x[i] ?? 0) !== (y[i] ?? 0)) return (x[i] ?? 0) - (y[i] ?? 0);
    return 0;
  };
  return out.sort((a, b) => a.loader.localeCompare(b.loader) || cmp(a.line, b.line));
}

export function sha256(file) {
  return createHash('sha256').update(readFileSync(file)).digest('hex');
}

export function buildManifest({ root = ROOT, dist, changelogText, tag, version, prerelease = false }) {
  const props = readProperties(join(root, 'gradle.properties'));
  const modVersion = props.mod_version;
  const base = props.archives_base_name || 'vibey-client';
  const tagVersion = tag ? tag.replace(/^refs\/tags\//, '').replace(/^v/, '') : null;
  const v = version || tagVersion || modVersion;
  if (!VERSION_RE.test(v)) throw new Error(`version "${v}" is not x.y.z[-pre][+build]`);
  // the jars carry mod_version in their names and metadata: a tag must match it (bump gradle.properties first)
  const core = (s) => s.replace(/[-+].*$/, '');
  if (core(v) !== core(modVersion)) throw new Error(`version ${v} (tag ${tag ?? '-'}) does not match mod_version ${modVersion} in gradle.properties`);
  const jarVersion = modVersion;

  const problems = [];
  const files = [];
  for (const t of targets(root)) {
    const file = `${base}-${t.loader}-${t.label}-${jarVersion}.jar`;
    const path = join(dist, file);
    if (!existsSync(path)) {
      problems.push(`missing ${file} (${t.target})`);
      continue;
    }
    files.push({
      manifest: { loader: t.loader, mcVersions: t.mcVersions, file, requires: t.loader === 'fabric' ? [FABRIC_API] : [] },
      path,
    });
  }
  if (existsSync(dist)) {
    const known = new Set(files.map((f) => f.manifest.file));
    for (const f of readdirSync(dist)) {
      if (f.endsWith('.jar') && !known.has(f) && f.endsWith(`-${jarVersion}.jar`)) problems.push(`unexpected jar ${f} in ${dist}`);
    }
  } else {
    problems.push(`${dist} does not exist (run ./gradlew buildAll)`);
  }

  if (problems.length) throw new Error(`release check failed:\n  ${problems.join('\n  ')}`);

  let changelog = (changelogText ?? '').trim();
  if (!changelog) {
    const text = readFileSync(join(root, 'CHANGELOG.md'), 'utf8');
    changelog = changelogSection(text, v) || changelogSection(text, core(v));
  }
  if (!changelog) throw new Error(`no changelog: empty release body and no "## ${v}" or "## ${core(v)}" section in CHANGELOG.md`);
  if (changelog.length > MAX_CHANGELOG) changelog = changelog.slice(0, MAX_CHANGELOG - 40).trimEnd() + '\n\n… (see CHANGELOG.md)';

  const manifest = {
    version: v,
    channel: channelOf(v, prerelease),
    changelog,
    files: files.map((f) => f.manifest),
    // the plugin is released on its own (blockbench-v* tags); the backend accepts null
    blockbench: null,
  };
  const uploads = files.map((f) => ({ name: f.manifest.file, path: f.path }));
  return { manifest, uploads };
}

/**
 * The manifest attached to the GitHub release as `vibey-manifest.json`: the same fields plus `sha256` and `size` of
 * every jar, so the backend can verify what it downloads. `file` = GitHub asset name.
 */
export function withHashes(manifest, uploads) {
  const info = new Map(uploads.map((u) => [u.name, { sha256: sha256(u.path), size: statSync(u.path).size }]));
  const of = (file) => {
    const i = info.get(file);
    if (!i) throw new Error(`no file for ${file}`);
    return i;
  };
  return {
    ...manifest,
    files: manifest.files.map((f) => ({ ...f, ...of(f.file) })),
    blockbench: null,
  };
}

/** Groups uploads into requests below the proxy limit (Cloudflare: 100 MB per request) and the server's 48 files. */
export function batches(uploads, maxBytes = BATCH_BYTES, maxFiles = 40) {
  const out = [];
  let cur = [], size = 0;
  for (const u of uploads) {
    const s = statSync(u.path).size;
    if (s > maxBytes) throw new Error(`${u.name} is ${s} bytes, more than one request may carry (${maxBytes})`);
    if (cur.length && (size + s > maxBytes || cur.length >= maxFiles)) {
      out.push(cur);
      cur = [];
      size = 0;
    }
    cur.push(u);
    size += s;
  }
  if (cur.length) out.push(cur);
  return out;
}

const BATCH_BYTES = 80 * 1024 * 1024;

/**
 * Staged publish (VIBE-API-2 §2): the files go up first in batches below the proxy limit
 * (`POST <base>/internal/client/v1/files` → `{ files: [{ file, sha256, size }] }`), then the release is created
 * with a manifest that names every file's sha256 and carries no file parts. `url` is the releases endpoint.
 */
export async function publish(url, token, manifest, uploads, { log = () => {} } = {}) {
  const filesUrl = url.replace(/\/releases\/?$/, '/files');
  const auth = { authorization: `Bearer ${token}` };
  const staged = new Map();
  const groups = batches(uploads);
  for (let i = 0; i < groups.length; i++) {
    const form = new FormData();
    for (const u of groups[i]) {
      form.append('file', await openAsBlob(u.path, { type: 'application/java-archive' }), u.name);
    }
    const res = await fetch(filesUrl, { method: 'POST', headers: auth, body: form });
    const text = await res.text();
    if (!res.ok) throw Object.assign(new Error(`file upload ${i + 1}/${groups.length} failed: HTTP ${res.status} ${text.slice(0, 2000)}`), { status: res.status });
    for (const f of JSON.parse(text).files ?? []) staged.set(f.file, f);
    log(`uploaded batch ${i + 1}/${groups.length} (${groups[i].length} files)`);
  }
  // the server's hashes must match ours (the release then refers to the staged bytes by sha256)
  const hashes = new Map();
  for (const u of uploads) {
    const local = sha256(u.path), remote = staged.get(u.name)?.sha256;
    if (remote !== local) throw new Error(`staged ${u.name}: server sha256 ${remote ?? '(missing)'} != local ${local}`);
    hashes.set(u.name, local);
  }
  const final = {
    ...manifest,
    files: manifest.files.map((f) => ({ ...f, sha256: hashes.get(f.file) })),
    blockbench: null,
  };
  const form = new FormData();
  form.append('manifest', JSON.stringify(final));
  const res = await fetch(url, { method: 'POST', headers: auth, body: form });
  const text = await res.text();
  if (res.status === 409) throw Object.assign(new Error(`version ${manifest.version} is already published (409)`), { status: 409 });
  if (!res.ok) throw Object.assign(new Error(`publish failed: HTTP ${res.status} ${text.slice(0, 2000)}`), { status: res.status });
  return JSON.parse(text);
}

async function main() {
  const a = parseArgs(process.argv.slice(2));
  if (a.help) {
    console.log(readFileSync(fileURLToPath(import.meta.url), 'utf8').split('\n').filter((l) => l.startsWith('//')).map((l) => l.slice(3)).join('\n'));
    return;
  }
  const changelogText = a.changelog && existsSync(a.changelog) ? readFileSync(a.changelog, 'utf8') : null;
  // --root: another checkout (tests); --dist is relative to it
  const root = a.root ? resolve(a.root) : ROOT;
  const { manifest, uploads } = buildManifest({
    root, dist: resolve(root, a.dist), changelogText, tag: a.tag, version: a.version,
    prerelease: a.prerelease,
  });
  const json = JSON.stringify(a.hashes ? withHashes(manifest, uploads) : manifest, null, 2);
  if (a.out) {
    mkdirSync(dirname(resolve(a.out)), { recursive: true });
    writeFileSync(resolve(a.out), json + '\n');
  }
  if (!a.quiet) {
    console.log(json);
    console.error(`\n${manifest.files.length} jars, channel ${manifest.channel}:`);
    for (const u of uploads) console.error(`  ${u.name}  ${statSync(u.path).size} B  sha256 ${sha256(u.path)}`);
  }
  if (a.publish) {
    const token = process.env.CLIENT_PUBLISH_TOKEN;
    if (!token) throw new Error('CLIENT_PUBLISH_TOKEN is not set');
    const res = await publish(a.publish, token, manifest, uploads, { log: (m) => console.error(m) });
    console.error(`published ${res.version ?? manifest.version} (${res.channel ?? manifest.channel}) with ${(res.files ?? []).length} files`);
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((e) => {
    console.error(`release-manifest: ${e.message}`);
    process.exit(e.status === 409 ? 3 : 1);
  });
}
