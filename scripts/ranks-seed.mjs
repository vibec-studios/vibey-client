// Seeds a LOCAL vibe-backend (AUTH_DEV_BYPASS=1, ADMIN_BOOTSTRAP_USER/PASSWORD set) with ranked bot players for
// RanksGameTest. Never point it at production. Usage: node scripts/ranks-seed.mjs [base] [adminUser] [adminPassword]
import { createHmac } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';

const base = process.argv[2] ?? 'http://127.0.0.1:3100';
const adminUser = process.argv[3] ?? 'seedowner';
const adminPassword = process.argv[4] ?? 'seed-password-123';
const tester = process.env.RANKS_TESTER ?? 'VibeyTester';
const secretFile = process.env.RANKS_SEED_SECRET ?? '.ranks-seed-totp';
if (/axolite|henny\.dev/.test(base)) throw new Error('local backends only');

const BOTS = [
  { name: 'RankOwner', ranks: ['owner', 'beta'], style: 'rainbow' },
  { name: 'RankMod', ranks: ['mod', 'beta'], style: 'ocean-wave' },
  { name: 'RankFan', ranks: ['supporter'], style: 'heartbeat' },
  { name: 'RankPartner', ranks: ['partner', 'supporter'], style: 'gold-shimmer' },
];

async function http(method, path, { body, token, cookie, csrf } = {}) {
  const headers = { accept: 'application/json' };
  if (body !== undefined) headers['content-type'] = 'application/json';
  if (token) headers.authorization = `Bearer ${token}`;
  if (cookie) headers.cookie = cookie;
  if (csrf) headers['x-csrf-token'] = csrf;
  const r = await fetch(base + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await r.text();
  const json = text ? JSON.parse(text) : null;
  if (!r.ok) throw new Error(`${method} ${path} -> ${r.status} ${text}`);
  return { json, headers: r.headers };
}

async function login(name) {
  const { json: ch } = await http('POST', '/api/v1/auth/challenge', { body: {} });
  const { json } = await http('POST', '/api/v1/auth/verify', {
    body: { name, serverId: ch.serverId, client: { name: 'ranks-seed', version: '1', platform: 'node' } },
  });
  return { name, id: json.user.id, token: json.tokens?.accessToken ?? json.accessToken };
}

function b32(s) {
  const A = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  let bits = 0, value = 0; const out = [];
  for (const ch of s.toUpperCase().replace(/[^A-Z2-7]/g, '')) {
    value = (value << 5) | A.indexOf(ch); bits += 5;
    if (bits >= 8) { out.push((value >>> (bits - 8)) & 255); bits -= 8; }
  }
  return Buffer.from(out);
}

function totp(secret) {
  const buf = Buffer.alloc(8);
  buf.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 30000)));
  const h = createHmac('sha1', b32(secret)).update(buf).digest();
  const off = h[h.length - 1] & 0xf;
  const code = ((h[off] & 0x7f) << 24) | (h[off + 1] << 16) | (h[off + 2] << 8) | h[off + 3];
  return String(code % 1_000_000).padStart(6, '0');
}

async function admin() {
  const { json } = await http('POST', '/admin/api/auth/login', { body: { username: adminUser, password: adminPassword } });
  const secret = json.step === 'totp_setup' ? json.secret : existsSync(secretFile) ? readFileSync(secretFile, 'utf8').trim() : null;
  if (!secret) throw new Error(`no TOTP secret for step ${json.step}: use a fresh database`);
  if (json.step === 'totp_setup') writeFileSync(secretFile, secret);
  else await new Promise((ok) => setTimeout(ok, 31_000 - (Date.now() % 30_000)));
  const r = await http('POST', '/admin/api/auth/totp', { body: { ticket: json.ticket, code: totp(secret) } });
  const cookie = (r.headers.get('set-cookie') ?? '').split(';')[0];
  return { cookie, csrf: r.json.csrfToken };
}

const me = await login(tester);
const bots = [];
for (const b of BOTS) bots.push({ ...b, ...(await login(b.name)) });
const a = await admin();
const grant = (id, ranks) => http('PUT', `/admin/api/users/${id}/ranks`, { body: { ranks: ranks.map((r) => ({ id: r })) }, ...a });
for (const b of bots) {
  await grant(b.id, b.ranks);
  await http('PUT', '/api/v1/me/name-style', { body: { id: b.style }, token: b.token });
  const req = await http('POST', '/api/v1/friends/requests', { body: { userId: me.id }, token: b.token }).then((r) => r.json, () => null);
  if (req?.status === 'pending') await http('POST', `/api/v1/friends/requests/${req.request.id}/accept`, { body: {}, token: me.token }).catch(() => {});
}
await grant(me.id, ['partner', 'supporter', 'beta']);
console.log(JSON.stringify({ tester: me.id, bots: bots.map(({ name, id, ranks, style }) => ({ name, id, ranks, style })) }, null, 2));
