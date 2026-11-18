// node --test tools/release-manifest.test.mjs
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, test } from 'node:test';
import { batches, buildManifest, changelogSection, channelOf, publish, sha256, targets } from './release-manifest.mjs';

const dirs = [];
after(() => dirs.forEach((d) => rmSync(d, { recursive: true, force: true })));

function fixture({ version = '0.2.0', bbVersion = '0.2.0', jars = true, extraJar = false } = {}) {
  const root = mkdtempSync(join(tmpdir(), 'vibey-release-'));
  dirs.push(root);
  writeFileSync(join(root, 'gradle.properties'), `mod_version=${version}\narchives_base_name=vibey-client\n`);
  mkdirSync(join(root, 'gradle'));
  writeFileSync(join(root, 'gradle/mc-versions.json'), JSON.stringify({ _comment: 'x', '1.21.1': ['1.21', '1.21.1'], '1.21.10': ['1.21.9', '1.21.10'], '26.1': ['26.1', '26.1.1', '26.1.2'] }));
  for (const [dir, label] of [['fabric-1.21.1', '1.21-1.21.1'], ['fabric-1.21.10', '1.21.9-1.21.10'], ['fabric-26.1', '26.1'], ['neoforge-1.21.1', '1.21-1.21.1']]) {
    mkdirSync(join(root, 'versions', dir), { recursive: true });
    writeFileSync(join(root, 'versions', dir, 'gradle.properties'), `minecraft_version=x\nminecraft_label=${label}\n`);
  }
  mkdirSync(join(root, 'blockbench'));
  writeFileSync(join(root, 'blockbench/package.json'), JSON.stringify({ version: bbVersion }));
  writeFileSync(join(root, 'blockbench/vibey.js'), `const VERSION = '${bbVersion}';\n`);
  writeFileSync(join(root, 'CHANGELOG.md'), `# Changelog\n\n## ${version} – second\n\n- Nametag icons\n- OTA cosmetics\n\n## 0.1.0 – first\n\n- old\n`);
  const dist = join(root, 'build/dist');
  mkdirSync(dist, { recursive: true });
  if (jars) {
    for (const f of ['fabric-1.21-1.21.1', 'fabric-1.21.9-1.21.10', 'fabric-26.1', 'neoforge-1.21-1.21.1']) writeFileSync(join(dist, `vibey-client-${f}-${version}.jar`), `jar ${f}`);
  }
  if (extraJar) writeFileSync(join(dist, `vibey-client-fabric-9.9-${version}.jar`), 'x');
  return { root, dist, blockbench: join(root, 'blockbench/vibey.js') };
}

test('changelog section and channel', () => {
  const md = '## 0.2.0 – x\n\nnew\n\n## 0.1.0\n\nold\n';
  assert.equal(changelogSection(md, '0.2.0'), 'new');
  assert.equal(changelogSection(md, '0.1.0'), 'old');
  assert.equal(changelogSection(md, '0.3.0'), '');
  assert.equal(channelOf('0.2.0'), 'release');
  assert.equal(channelOf('0.3.0-beta.1'), 'beta');
});

test('real repo: every target has exact Minecraft versions', () => {
  const t = targets();
  assert.equal(t.length, 20);
  const by = Object.fromEntries(t.map((x) => [x.target, x.mcVersions]));
  assert.deepEqual(by['fabric-1.21.1'], ['1.21', '1.21.1']);
  assert.deepEqual(by['fabric-1.21.10'], ['1.21.9', '1.21.10']);
  assert.deepEqual(by['neoforge-1.21.8'], ['1.21.6', '1.21.7', '1.21.8']);
  assert.deepEqual(by['fabric-26.3'], ['26.3']);
  // natural order: 1.21.1 before 1.21.10
  assert.deepEqual(t.filter((x) => x.loader === 'fabric').map((x) => x.line).slice(0, 3), ['1.21.1', '1.21.3', '1.21.4']);
});

test('manifest from build output', () => {
  const f = fixture();
  const { manifest, uploads } = buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench, tag: 'v0.2.0' });
  assert.equal(manifest.version, '0.2.0');
  assert.equal(manifest.channel, 'release');
  assert.equal(manifest.changelog, '- Nametag icons\n- OTA cosmetics');
  assert.deepEqual(manifest.blockbench, { version: '0.2.0', file: 'vibey.js' });
  assert.deepEqual(manifest.files.map((x) => [x.loader, x.file, x.mcVersions.join(',')]), [
    ['fabric', 'vibey-client-fabric-1.21-1.21.1-0.2.0.jar', '1.21,1.21.1'],
    ['fabric', 'vibey-client-fabric-1.21.9-1.21.10-0.2.0.jar', '1.21.9,1.21.10'],
    ['fabric', 'vibey-client-fabric-26.1-0.2.0.jar', '26.1,26.1.1,26.1.2'],
    ['neoforge', 'vibey-client-neoforge-1.21-1.21.1-0.2.0.jar', '1.21,1.21.1'],
  ]);
  assert.deepEqual(manifest.files[0].requires, [{ modrinth: 'P7dR8mSH', name: 'Fabric API' }]);
  assert.deepEqual(manifest.files[3].requires, []);
  for (const k of ['url', 'sha256', 'size']) assert.ok(!(k in manifest.files[0]));
  assert.equal(uploads.length, 5);
});

test('release body wins over CHANGELOG.md; beta tags', () => {
  const f = fixture({ version: '0.3.0-beta.1', bbVersion: '0.3.0-beta.1' });
  const { manifest } = buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench, tag: 'refs/tags/v0.3.0-beta.1', changelogText: '  From the release  ' });
  assert.equal(manifest.channel, 'beta');
  assert.equal(manifest.changelog, 'From the release');
});

test('refuses mismatches and missing files', () => {
  assert.throws(() => { const f = fixture(); buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench, tag: 'v0.2.1' }); }, /does not match mod_version/);
  assert.throws(() => { const f = fixture({ jars: false }); buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench }); }, /missing vibey-client-fabric-1.21-1.21.1-0.2.0.jar/);
  assert.throws(() => { const f = fixture({ extraJar: true }); buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench }); }, /unexpected jar/);
  assert.throws(() => { const f = fixture(); writeFileSync(join(f.root, 'blockbench/vibey.js'), "const VERSION = '9.9.9';"); buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench }); }, /says version 9.9.9/);
});

test('batches stay below the request limit', () => {
  const f = fixture();
  const ups = ['a', 'b', 'c', 'd', 'e'].map((n) => {
    const p = join(f.root, `${n}.jar`);
    writeFileSync(p, Buffer.alloc(40));
    return { name: `${n}.jar`, path: p };
  });
  assert.deepEqual(batches(ups, 100).map((b) => b.map((u) => u.name)), [['a.jar', 'b.jar'], ['c.jar', 'd.jar'], ['e.jar']]);
  assert.deepEqual(batches(ups, 1000, 3).map((b) => b.length), [3, 2]);
  assert.throws(() => batches(ups, 10), /more than one request/);
});

test('publish stages the files in batches, then creates the release by sha256 without file parts', async () => {
  const f = fixture();
  const { manifest, uploads } = buildManifest({ root: f.root, dist: f.dist, blockbench: f.blockbench });
  const seen = { auth: new Set(), stagedRequests: 0, staged: [], release: null, releaseFiles: -1 };
  const server = createServer((req, res) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', async () => {
      seen.auth.add(req.headers.authorization);
      const body = Buffer.concat(chunks);
      const form = await new Request('http://x', { method: 'POST', headers: { 'content-type': req.headers['content-type'] }, body }).formData();
      const files = [...form.values()].filter((v) => typeof v !== 'string');
      if (req.url.endsWith('/internal/client/v1/files')) {
        seen.stagedRequests++;
        const out = [];
        for (const file of files) {
          const bytes = Buffer.from(await file.arrayBuffer());
          seen.staged.push(file.name);
          out.push({ file: file.name, sha256: createHash('sha256').update(bytes).digest('hex'), size: bytes.length });
        }
        res.writeHead(201, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ files: out }));
        return;
      }
      seen.release = JSON.parse(form.get('manifest'));
      seen.releaseFiles = files.length;
      res.writeHead(seen.release.version === '0.2.0' ? 201 : 409, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ version: '0.2.0', channel: 'release', files: seen.release.files }));
    });
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  try {
    const url = `http://127.0.0.1:${server.address().port}/internal/client/v1/releases`;
    const res = await publish(url, 'secret-token', manifest, uploads);
    assert.equal(res.version, '0.2.0');
    assert.deepEqual([...seen.auth], ['Bearer secret-token']);
    assert.equal(seen.stagedRequests, 1);
    assert.deepEqual(seen.staged.sort(), uploads.map((u) => u.name).sort());
    assert.equal(seen.releaseFiles, 0);
    for (const file of seen.release.files) assert.equal(file.sha256, sha256(join(f.dist, file.file)));
    assert.equal(seen.release.blockbench.sha256, sha256(f.blockbench));
    assert.equal(seen.release.changelog, manifest.changelog);
    await assert.rejects(publish(url, 'secret-token', { ...manifest, version: '0.1.0' }, uploads), /already published/);
  } finally {
    server.close();
  }
});
