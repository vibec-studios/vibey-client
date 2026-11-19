#!/usr/bin/env node
// Local mock of the backend's announcements route (vibe-launcher docs/DOMAINS.md "Hinweise an Nutzer") for testing
// the client's notices before / without the real backend. In-memory, never talks to production.
//
//   node scripts/mock-announcements.mjs --port 3480 --log build/review/domains/mock-requests.jsonl
//
// Serves GET /api/v1/announcements?app=launcher|client&version=<x>&lang=de|en → {"items":[…]} like the backend:
// filtered by `app` (items whose `apps` list it), texts in the requested language (default de). Items are set with
// POST /__mock/announcements {"items":[…]}; an item may carry `title`/`body` as strings or as {"de":…,"en":…}.
// GET /api/v1/meta answers a minimal meta document; everything else is 404. Point the client at it with
// -Dvibeyclient.api=http://127.0.0.1:3480.
// Debug: GET /__mock/log (requests so far), POST /__mock/reset (no items, empty log),
//        POST /__mock/config {"status": 500, "filter": false} (answer the route with that status, 200 = normal again;
//        filter false = hand out items for other apps too, to test the client's own `apps` check).
import http from "node:http";
import fs from "node:fs";
import path from "node:path";

const args = process.argv.slice(2);
const opt = (name, def) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 && i + 1 < args.length && !args[i + 1].startsWith("--") ? args[i + 1] : def;
};
const PORT = Number(opt("port", "3480"));
const LOG_FILE = opt("log", null);

let items = [];
let status = 200;
let filter = true;
let log = [];

const text = (v, lang) => (v && typeof v === "object" ? v[lang] ?? v.de ?? v.en ?? null : v ?? null);

function announcements(url) {
  const app = url.searchParams.get("app");
  const lang = url.searchParams.get("lang") === "en" ? "en" : "de";
  return {
    items: items
      .filter((it) => !filter || !app || !Array.isArray(it.apps) || it.apps.length === 0 || it.apps.includes(app))
      .map((it) => ({
        id: it.id,
        level: it.level ?? "info",
        title: text(it.title, lang),
        body: text(it.body, lang),
        url: it.url ?? null,
        until: it.until ?? null,
        apps: it.apps ?? ["launcher", "client"],
        max_version: it.max_version ?? null,
      })),
  };
}

function send(res, code, body) {
  const s = JSON.stringify(body);
  res.writeHead(code, { "content-type": "application/json", "content-length": Buffer.byteLength(s) });
  res.end(s);
}

function readBody(req) {
  return new Promise((resolve) => {
    let b = "";
    req.on("data", (c) => (b += c));
    req.on("end", () => {
      try {
        resolve(b ? JSON.parse(b) : {});
      } catch {
        resolve(null);
      }
    });
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://127.0.0.1:${PORT}`);
  const entry = { at: new Date().toISOString(), method: req.method, path: url.pathname, query: Object.fromEntries(url.searchParams) };
  if (!url.pathname.startsWith("/__mock/")) {
    log.push(entry);
    if (LOG_FILE) fs.appendFileSync(LOG_FILE, JSON.stringify(entry) + "\n");
  }
  if (req.method === "GET" && url.pathname === "/api/v1/announcements") {
    if (status !== 200) return send(res, status, { error: { code: "mock", message: `mock status ${status}` } });
    return send(res, 200, announcements(url));
  }
  if (req.method === "GET" && url.pathname === "/api/v1/meta") {
    return send(res, 200, { name: "mock-announcements", features: {}, motd: null, loadoutSigning: null });
  }
  if (req.method === "GET" && url.pathname === "/__mock/log") return send(res, 200, { requests: log });
  if (req.method === "POST" && url.pathname === "/__mock/reset") {
    items = [];
    status = 200;
    filter = true;
    log = [];
    return send(res, 200, { ok: true });
  }
  if (req.method === "POST" && url.pathname === "/__mock/announcements") {
    const b = await readBody(req);
    if (!b || !Array.isArray(b.items)) return send(res, 400, { error: "items[] expected" });
    items = b.items;
    return send(res, 200, { ok: true, count: items.length });
  }
  if (req.method === "POST" && url.pathname === "/__mock/config") {
    const b = await readBody(req);
    if (b && Number.isInteger(b.status)) status = b.status;
    if (b && typeof b.filter === "boolean") filter = b.filter;
    return send(res, 200, { ok: true, status, filter });
  }
  send(res, 404, { error: { code: "not_found", message: "mock" } });
});

if (LOG_FILE) fs.mkdirSync(path.dirname(path.resolve(LOG_FILE)), { recursive: true });
server.listen(PORT, "127.0.0.1", () => console.log(`mock announcements on http://127.0.0.1:${PORT}`));
