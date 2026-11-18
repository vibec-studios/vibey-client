#!/usr/bin/env node
// Local mock of the play-link routes (vibe-launcher docs/PLAY-LINKS.md §3) for testing the client before the real
// backend has them. In-memory, never talks to production.
//
//   node scripts/mock-play-links.mjs --port 3201 --upstream http://127.0.0.1:3200 --log build/review/playlinks/mock-requests.jsonl
//
// It serves POST/GET/DELETE /api/v1/play-links[/<code>] and a /j/<code> stub page itself, with the backend's
// validation rules (code charset, one active link per host, 10 new links per hour and user, at most 500 mods), and
// proxies everything else (login, gateway WebSocket, …) to --upstream, a LOCAL vibe-backend (AUTH_DEV_BYPASS). Point
// the client at the mock: -Dvibeyclient.api=http://127.0.0.1:3201 -Dvibeyclient.devAuth=true.
//
// Options: --port (3201) --upstream (none: everything else 404) --public-url (http://127.0.0.1:<port>)
//          --log <file.jsonl> --rate <n per hour> (10) --ttl <seconds> (43200) --relay-domains a.b,c.d (any)
//          --missing (answer 404 for the routes: a backend without play links)
// Debug: GET /__mock/log (requests so far), GET /__mock/links (active links), POST /__mock/reset.
import http from "node:http";
import net from "node:net";
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const args = process.argv.slice(2);
const opt = (name, def) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 && i + 1 < args.length && !args[i + 1].startsWith("--") ? args[i + 1] : def;
};
const flag = (name) => args.includes(`--${name}`);

const PORT = Number(opt("port", "3201"));
const UPSTREAM = opt("upstream", null) ? new URL(opt("upstream")) : null;
const PUBLIC_URL = opt("public-url", `http://127.0.0.1:${PORT}`).replace(/\/+$/, "");
const LOG_FILE = opt("log", null);
const RATE_MAX = Number(opt("rate", "10"));
const RATE_WINDOW_MS = 60 * 60 * 1000;
const TTL_SECONDS = Number(opt("ttl", String(12 * 60 * 60)));
const RELAY_DOMAINS = (opt("relay-domains", "") || "").split(",").map((s) => s.trim().toLowerCase()).filter(Boolean);
const MISSING = flag("missing");

// ---------------------------------------------------------------------------------------------- contract rules

export const ALPHABET = "23456789abcdefghjkmnpqrstuvwxyz"; // [a-z0-9] without 0/o, 1/i/l
export const CODE_RE = /^[2-9a-hjkmnp-z]{6,8}$/;
export const MAX_MODS = 500;
const ID_RE = /^[A-Za-z0-9._-]+$/;
const SOURCES = ["modrinth", "curseforge"];
const LOADERS = ["vanilla", "fabric", "quilt", "forge", "neoforge"];
const KINDS = ["mod", "resourcepack", "shader", "datapack"];
const RELAY_RE = /^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(:\d{1,5})?$/;

/** Validates a POST body like the backend's zod schema; returns {value} or {issues}. Unknown keys are dropped. */
export function validateCreate(b) {
  const issues = [];
  const bad = (p, m) => issues.push({ path: p, message: m });
  if (!b || typeof b !== "object" || Array.isArray(b)) return { issues: [{ path: "", message: "object expected" }] };
  const out = {};
  const isId = (v) => typeof v === "string" && v.length >= 1 && v.length <= 64 && ID_RE.test(v);

  if (typeof b.relay_host !== "string") bad("relay_host", "required");
  else {
    const h = b.relay_host.trim().toLowerCase();
    if (h.length > 253 || !RELAY_RE.test(h)) bad("relay_host", "invalid relay address");
    else if (RELAY_DOMAINS.length && !RELAY_DOMAINS.some((d) => h.replace(/:\d+$/, "").endsWith("." + d))) bad("relay_host", "not a Vibey relay address");
    else out.relay_host = h;
  }
  if (typeof b.mc_version !== "string" || !b.mc_version.trim() || b.mc_version.trim().length > 40 || !/^[A-Za-z0-9._ +-]+$/.test(b.mc_version.trim())) bad("mc_version", "invalid");
  else out.mc_version = b.mc_version.trim();

  if (!b.loader || typeof b.loader !== "object") bad("loader", "required");
  else if (!LOADERS.includes(b.loader.kind)) bad("loader.kind", "invalid");
  else if (b.loader.version != null && (typeof b.loader.version !== "string" || b.loader.version.length > 60 || !/^[A-Za-z0-9._+-]*$/.test(b.loader.version))) bad("loader.version", "invalid");
  else out.loader = { kind: b.loader.kind, version: b.loader.version ?? null };

  if (b.pack == null) out.pack = null;
  else if (typeof b.pack !== "object" || !SOURCES.includes(b.pack.source) || !isId(b.pack.project_id) || !isId(b.pack.version_id)) bad("pack", "invalid pack");
  else if (b.pack.title != null && (typeof b.pack.title !== "string" || b.pack.title.trim().length > 120)) bad("pack.title", "invalid");
  else out.pack = { source: b.pack.source, project_id: b.pack.project_id, version_id: b.pack.version_id, title: b.pack.title?.trim() ?? null };

  if (b.mods == null) out.mods = [];
  else if (!Array.isArray(b.mods)) bad("mods", "array expected");
  else if (b.mods.length > MAX_MODS) bad("mods", `at most ${MAX_MODS} entries`);
  else {
    out.mods = [];
    b.mods.forEach((m, i) => {
      if (!m || typeof m !== "object" || !SOURCES.includes(m.source)) return bad(`mods.${i}.source`, "only modrinth and curseforge");
      if (!isId(m.project_id)) return bad(`mods.${i}.project_id`, "invalid id");
      if (m.version_id != null && !isId(m.version_id)) return bad(`mods.${i}.version_id`, "invalid id");
      if (m.kind !== undefined && !KINDS.includes(m.kind)) return bad(`mods.${i}.kind`, "invalid kind");
      out.mods.push({ source: m.source, project_id: m.project_id, version_id: m.version_id ?? null, ...(m.kind ? { kind: m.kind } : {}) });
    });
  }
  if (b.client_pack != null && (typeof b.client_pack !== "string" || b.client_pack.length > 32 || !/^[a-z0-9_-]+$/.test(b.client_pack))) bad("client_pack", "invalid");
  else out.client_pack = b.client_pack ?? null;
  if (b.world_name != null && (typeof b.world_name !== "string" || b.world_name.trim().length > 64)) bad("world_name", "invalid");
  else out.world_name = b.world_name?.trim() || null;
  if (b.max_players != null && (!Number.isInteger(b.max_players) || b.max_players < 1 || b.max_players > 1000)) bad("max_players", "1..1000");
  else out.max_players = b.max_players ?? null;
  return issues.length ? { issues } : { value: out };
}

export function newCode(len = 6) {
  const bytes = crypto.randomBytes(len);
  let s = "";
  for (let i = 0; i < len; i++) s += ALPHABET[bytes[i] % ALPHABET.length];
  return s;
}

// ---------------------------------------------------------------------------------------------- state

const links = new Map(); // code -> link
const hits = new Map(); // userId -> [timestamps]
const requests = [];

function userOf(req) {
  const m = /^Bearer\s+(.+)$/i.exec(req.headers.authorization ?? "");
  if (!m) return null;
  const parts = m[1].split(".");
  if (parts.length !== 3) return null;
  try {
    const p = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8"));
    if (typeof p.sub !== "string") return null;
    if (typeof p.exp === "number" && p.exp * 1000 < Date.now()) return { expired: true };
    return { id: p.sub, name: p.name ?? null, token: m[1] };
  } catch {
    return null;
  }
}

function prune() {
  const now = Date.now();
  for (const [c, l] of links) if (l.expiresAt <= now) links.delete(c);
}

async function hostName(user) {
  if (!UPSTREAM) return user.name ?? "Host";
  try {
    const r = await fetch(new URL("/api/v1/me", UPSTREAM), { headers: { authorization: `Bearer ${user.token}` } });
    const j = await r.json();
    return j?.user?.name ?? j?.name ?? "Host";
  } catch {
    return "Host";
  }
}

function view(l) {
  return {
    code: l.code,
    url: `${PUBLIC_URL}/j/${l.code}`,
    host: { uuid: l.hostId, name: l.hostName },
    relay_host: l.relay_host,
    mc_version: l.mc_version,
    loader: l.loader,
    pack: l.pack,
    mods: l.mods,
    client_pack: l.client_pack,
    world_name: l.world_name,
    max_players: l.max_players,
    active: l.expiresAt > Date.now(),
    created_at: new Date(l.createdAt).toISOString(),
    expires_at: new Date(l.expiresAt).toISOString(),
  };
}

// ---------------------------------------------------------------------------------------------- http

function log(entry) {
  const e = { at: new Date().toISOString(), ...entry };
  requests.push(e);
  console.log(`[mock] ${e.method} ${e.path} -> ${e.status}${e.note ? " (" + e.note + ")" : ""}`);
  if (LOG_FILE) {
    fs.mkdirSync(path.dirname(path.resolve(LOG_FILE)), { recursive: true });
    fs.appendFileSync(LOG_FILE, JSON.stringify(e) + "\n");
  }
}

function send(res, status, body, headers = {}) {
  if (body === undefined) {
    res.writeHead(status, headers);
    res.end();
    return;
  }
  const text = typeof body === "string" ? body : JSON.stringify(body);
  res.writeHead(status, { "content-type": typeof body === "string" ? "text/html; charset=utf-8" : "application/json", ...headers });
  res.end(text);
}

const err = (code, message, extra = {}) => ({ error: { code, message, ...extra } });

function readBody(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
  });
}

async function handlePlayLinks(req, res, url) {
  const method = req.method;
  const p = url.pathname;
  const raw = method === "POST" ? await readBody(req) : "";
  let body = null;
  try {
    body = raw ? JSON.parse(raw) : null;
  } catch {
    body = raw;
  }
  const done = (status, out, note, headers) => {
    log({ method, path: p, status, user: userOf(req)?.id ?? null, request: body, response: out ?? null, note });
    send(res, status, out, headers);
  };
  if (MISSING) return done(404, err("not_found", `Route ${method} ${p} gibt es nicht`), "--missing");
  prune();

  if (p === "/api/v1/play-links" && method === "POST") {
    const user = userOf(req);
    if (!user) return done(401, err("unauthorized", "Nicht angemeldet"));
    if (user.expired) return done(401, err("token_expired", "Token abgelaufen"));
    const v = validateCreate(body);
    if (v.issues) return done(400, err("validation_failed", "Ungültige Eingabe", { details: v.issues }));
    const now = Date.now();
    const recent = (hits.get(user.id) ?? []).filter((t) => t > now - RATE_WINDOW_MS);
    if (recent.length >= RATE_MAX) {
      const retry = Math.ceil((recent[0] + RATE_WINDOW_MS - now) / 1000);
      hits.set(user.id, recent);
      return done(429, err("rate_limited", "Zu viele neue Spiel-Links – bitte später erneut versuchen.", { retryAfter: retry }), null, { "retry-after": String(retry) });
    }
    recent.push(now);
    hits.set(user.id, recent);
    for (const [c, l] of links) if (l.hostId === user.id) links.delete(c); // one active link per host
    let code;
    for (let i = 0; i < 8 && (!code || links.has(code)); i++) code = newCode(i < 4 ? 6 : 8);
    const l = { code, hostId: user.id, hostName: await hostName(user), ...v.value, createdAt: now, expiresAt: now + TTL_SECONDS * 1000 };
    links.set(code, l);
    return done(201, { code, url: `${PUBLIC_URL}/j/${code}`, expires_at: new Date(l.expiresAt).toISOString() });
  }
  const m = /^\/api\/v1\/play-links\/([^/]+)$/.exec(p);
  if (m) {
    const code = decodeURIComponent(m[1]).toLowerCase();
    if (!CODE_RE.test(code)) return done(400, err("validation_failed", "Ungültiger Code"));
    const l = links.get(code);
    if (method === "GET") {
      if (!l) return done(404, err("not_found", "Dieser Spiel-Link ist abgelaufen oder existiert nicht"));
      return done(200, view(l), null, { "cache-control": "no-store" });
    }
    if (method === "DELETE") {
      const user = userOf(req);
      if (!user || user.expired) return done(401, err("unauthorized", "Nicht angemeldet"));
      if (!l) return done(404, err("not_found", "Spiel-Link nicht gefunden"));
      if (l.hostId !== user.id) return done(403, err("forbidden", "Nur der Host kann diesen Link löschen"));
      links.delete(code);
      return done(204, undefined);
    }
  }
  return done(405, err("method_not_allowed", `${method} ${p}`));
}

function landing(res, code) {
  prune();
  const l = links.get(code.toLowerCase());
  const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
  if (!l) {
    log({ method: "GET", path: `/j/${code}`, status: 404 });
    return send(res, 404, `<!doctype html><title>Link abgelaufen</title><p>Dieser Spiel-Link ist abgelaufen.</p>`);
  }
  const title = `${l.hostName} lädt dich ein${l.world_name ? ": " + l.world_name : ""}`;
  const pack = l.pack?.title ?? `Minecraft ${l.mc_version} · ${l.loader.kind}`;
  log({ method: "GET", path: `/j/${code}`, status: 200 });
  send(res, 200, `<!doctype html><html lang="de"><head><meta charset="utf-8"><title>${esc(title)}</title>
<meta property="og:title" content="${esc(title)}"><meta property="og:description" content="${esc(pack)}">
<meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:system-ui;background:#16161c;color:#eee;padding:32px">
<p>MOCK – nur lokal</p><h1>${esc(title)}</h1><p>${esc(pack)} · ${l.mods.length} Mods</p>
<p><a href="vibelauncher://join/${esc(l.code)}" style="color:#3fe0c5">Im Vibe Launcher öffnen</a></p></body></html>`);
}

function proxy(req, res) {
  if (!UPSTREAM) return send(res, 404, err("not_found", `mock: no upstream for ${req.method} ${req.url}`));
  const up = http.request(
    { host: UPSTREAM.hostname, port: UPSTREAM.port || 80, method: req.method, path: req.url, headers: { ...req.headers, host: UPSTREAM.host } },
    (r) => {
      res.writeHead(r.statusCode ?? 502, r.headers);
      r.pipe(res);
    },
  );
  up.on("error", (e) => send(res, 502, err("bad_gateway", `upstream: ${e.message}`)));
  req.pipe(up);
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host ?? "127.0.0.1"}`);
  try {
    if (url.pathname === "/__mock/log") return send(res, 200, requests);
    if (url.pathname === "/__mock/links") return send(res, 200, [...links.values()].map(view));
    if (url.pathname === "/__mock/reset" && req.method === "POST") {
      links.clear();
      hits.clear();
      requests.length = 0;
      return send(res, 204, undefined);
    }
    if (url.pathname === "/api/v1/play-links" || url.pathname.startsWith("/api/v1/play-links/")) return await handlePlayLinks(req, res, url);
    const j = /^\/j\/([^/]+)$/.exec(url.pathname);
    if (j && req.method === "GET") return landing(res, decodeURIComponent(j[1]));
    proxy(req, res);
  } catch (e) {
    console.error(e);
    send(res, 500, err("internal", String(e?.message ?? e)));
  }
});

// WebSocket (gateway) and other upgrades: raw pipe to the upstream.
server.on("upgrade", (req, socket, head) => {
  if (!UPSTREAM) return socket.destroy();
  const up = net.connect(Number(UPSTREAM.port || 80), UPSTREAM.hostname, () => {
    let h = `${req.method} ${req.url} HTTP/${req.httpVersion}\r\n`;
    for (let i = 0; i < req.rawHeaders.length; i += 2) {
      const k = req.rawHeaders[i];
      h += `${k}: ${k.toLowerCase() === "host" ? UPSTREAM.host : req.rawHeaders[i + 1]}\r\n`;
    }
    up.write(h + "\r\n");
    if (head?.length) up.write(head);
    socket.pipe(up).pipe(socket);
  });
  up.on("error", () => socket.destroy());
  socket.on("error", () => up.destroy());
});

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  server.listen(PORT, "127.0.0.1", () => {
    console.log(`[mock] play links on http://127.0.0.1:${PORT}${UPSTREAM ? ", rest -> " + UPSTREAM.href : ""}${MISSING ? " (--missing: routes answer 404)" : ""}`);
  });
}
export { server };
