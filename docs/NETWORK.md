# Network

Every HTTP request Vibey Client makes, how it is cached, and how to measure it. Code: `backend/net/*`
(Minecraft-free, so JUnit and the timing harness use it directly), `backend/BackendClient`, `backend/Gateway`,
`avatar/ItemLoader`.

## One client, no thread per request

- **One `HttpClient`** for everything (`Net.client()`): backend REST, gateway WebSocket, catalog, assets,
  thumbnails, skin heads, announcements, lyrics. HTTP/2 with keep-alive for https (one connection per host,
  requests multiplexed), HTTP/1.1 for `http://` (local dev backends: Node rejects h2c upgrades with a body).
  `Accept-Encoding: gzip` (Cloudflare compresses JSON: the catalog is 30 KB → 8 KB). Connect timeout 5 s.
- **Request timeouts by kind** (`Net.Kind`): `API` 10 s (REST, catalog, announcements), `QUICK` 5 s (meta /
  status badge), `BULK` 30 s (assets, thumbnails, skins, multipart uploads).
- **Threads:** the client's executor is a bounded pool of 3 daemon threads (`VibeyClient-Http-*`, gone after 30 s
  idle). Nothing blocks on it. REST calls are asynchronous end to end (`Rest`): no thread is held while a request
  or a retry wait is pending. The one thread that may block is `VibeyClient-Auth` (login with the Mojang join, token
  refresh, strictly serialised). Hashing, parsing and disk I/O of catalog assets run on `VibeyClient-Catalog`
  (2 threads).
  Before: 5 separate clients (one selector thread each); 4 of them completed responses on the 3-thread `Tasks` pool,
  and `BackendStatus`/`Heads` blocked `Tasks` workers in `send()`. Three blocked workers stalled every request of the
  client until its connect timeout (measured: 3 × 5 s timeouts; a busy pool delays everything by its busy time).
- **Warm-up gate** (`Net.send`): the first https request to a host, or the first after 25 s idle, opens the
  connection alone, and requests started meanwhile wait for its response headers (at most 2 s). Without it a burst
  of N cold requests opens N TLS connections (measured: 8 parallel cold requests = 8 connections).
- **Retries** (`Rest`, same rules as before): idempotent calls retry twice on network errors and 502/503/504
  (backoff 500 ms·2ⁿ ± 50 %), calls that never reached the server always; one wait for `429` with `Retry-After ≤ 5 s`;
  one token refresh on `401 token_expired/invalid_token`. Gateway reconnects: exponential, jittered, max 30 s.

## Request map

| When | Request | Cache / dedup | Notes |
|---|---|---|---|
| title screen (≤ 5 min), social ready, signed-loadout key | `GET /api/v1/meta` | `Meta`: one request for all callers, 5 min, `If-None-Match` if the backend sends an ETag | skipped while the gateway is connected (the title badge knows it is online) |
| start, every 6 h, language change | `GET /api/v1/announcements?…` | in-memory ETag | |
| start, then poll | `GET /api/v1/cosmetics/catalog` | disk copy `cache/catalog.json` + `catalog.etag` (+ `catalog.base`: whose copy) shown right away, then `If-None-Match` | poll every 60–75 min while the gateway is connected (it pushes `catalog.updated`, `ready.catalogVersion`), 10–13 min without; world join only without gateway |
| new/changed catalog items | `GET /api/v1/cosmetics/assets/<sha256>` | disk `cache/assets/<hash>.vibey`, immutable | ≤ 4 parallel, each hash once |
| wardrobe | `GET …/thumbnail.png` | disk `cache/thumbs/` | ≤ 4 parallel, each URL once |
| friends list, toasts | skin textures (`textures.minecraft.net`) | in memory, each URL once | decoded on `Tasks` |
| first tick, "Erneut prüfen" | `GET /api/v1/access` (logs in first if needed, no gateway) | approvals in `vibeyclient/access.json` for 7 days | ACCESS.md; nothing else runs while access is not approved |
| login | `POST /auth/challenge`, Mojang `joinServer`, `POST /auth/verify` | tokens in `auth.json` | auth thread |
| token about to expire | `POST /auth/refresh` | | strictly serialised |
| login done | `GET /ws` (WebSocket) → hello → identify → `ready` | | own HTTP/1.1 upgrade connection; heartbeat ping every 30 s |
| every fresh `ready` | `POST /api/v1/client/bootstrap` with the 9 GETs below, **or** the 9 single GETs | `Bootstrap`: each path handed out once within 20 s | feature-detected: `ready.features.clientBootstrap`; older backends get the single GETs |
| (bundled) | `friends/requests`, `blocks`, `chats`, `invites?direction=incoming&pendingOnly=true`, `notifications?limit=50`, `hosting/friends`, `hosting/sessions`, `cosmetics/me`, `cosmetics/loadout/signed` | | |
| players around (every 2–2.5 min, alone every 10–10.5 min), newcomers | `POST /api/v1/cosmetics/lookup` | batched per 2 s (40 ticks) | also `rank`/`tags`/`nameStyle`; changes are pushed as `nametag.updated` (new backends) |
| `ready.ranksVersion` / `ranks.updated` differs from the cached version | `GET /api/v1/ranks` | disk copy `cache/ranks.json` + `ranks.etag`, `If-None-Match` | old backends (no `ranksVersion`): never; see NAMETAGS.md |
| loadout change (debounced 1.2 s) | `PUT /api/v1/cosmetics/loadout` | answer carries the signed token | |
| signed token 30 min before expiry (6 h) | `GET /cosmetics/loadout/signed` | | |
| user actions | chats, invites, play links, search, submissions … | | unchanged |

No polling duplicates a push: friends, presence, chats, invites, loadouts (`loadout.updated`), emotes and catalog
changes all arrive over the gateway.

## Numbers (2026-11-19)

Harness runs, median of 5 fresh JVMs each. "public" = production `https://vibe.axolite.app` (Cloudflare, DUS),
no login: meta, announcements, catalog (+ 35 assets when cold), then the wardrobe's 34 thumbnails. "auth" = local
vibe-backend (AUTH_DEV_BYPASS) behind `latency-proxy.mjs` with 40 ms RTT, cleartext HTTP/1.1: login (cold) or
refresh (warm), gateway → ready, then everything loaded after ready. Times in ms from process start of the measured
part (the first request of a fresh JVM includes ~300 ms of TLS/HttpClient class loading in both columns).

| Scenario | before | after |
|---|---|---|
| public cold: catalog + assets ready | 1210 | 959 |
| public cold: thumbnails ready | 1947 | 1140 |
| public warm: cosmetics visible | 1217 (after the 304) | **324** (disk copy) |
| public warm: everything revalidated | 1256 | 587 |
| public warm, 3 `Tasks` workers busy 2 s | 2668 | 668 |
| public: TCP connections to the backend | 3 | 1 |
| auth cold: all data after ready | 915 | 998 (old backend: 973) |
| auth warm: all data after ready | 899 | 850 (old backend: 926) |
| auth: requests (cold / warm) | 15 / 14 | 6 / 5 (old backend: 14 / 13) |
| auth: TCP connections | 11 | 3 (old backend: 10) |

Locally, without TLS or HTTP/2, the logged-in flow is about as fast as before. The gain there is requests and
connections. Over production (HTTP/2, TLS) one connection replaces up to 10 handshakes.

curl reference (production, median of 5, gzip): DNS 5 ms, TCP 5 ms, TLS 15 ms, TTFB 26–37 ms, total 50–78 ms per
request; a catalog 304 is 54 ms.

### Request counts

| | before | after (new backend) | after (old backend) |
|---|---|---|---|
| start, logged in, warm cache (incl. WebSocket) | 17–18 ¹ | 7 | 15 |
| per 10 min in a world, alone | ≈ 6 (+1 per world join) | ≈ 1.8 | ≈ 1.8 |
| per 10 min in a world with other players | ≈ 6 (+1 per world join) | ≈ 5.2 | ≈ 5.2 |
| title screen open, gateway connected | +1 meta per visit (≤ 5 min) | 0 | 0 |

¹ meta ×3, announcements, catalog, refresh, WebSocket, 7 social GETs, `cosmetics/me`, signed loadout, lookup, often
a second catalog 304 when `ready` arrived before the first catalog load finished. Per 10 min before: lookup every
2–2.5 min (4.4, also alone), catalog poll (0.9), token refresh (≈ 0.7); after: own lookup every 10 min when alone (1.0),
catalog safety poll (0.15), token refresh (≈ 0.7). Heartbeats are WebSocket frames, not requests (20 per 10 min,
unchanged). Cold start adds the challenge/verify instead of the refresh and the 35 assets/34 thumbnails, in both.

## Measuring

```sh
# production, public endpoints only (read-only)
node tools/net-bench/run.mjs public before cold          # or: after, warm, --busy 3, --runs 5, --json out.json
# logged-in flow against a LOCAL backend (vibe-backend apps/api, NODE_ENV=development AUTH_DEV_BYPASS=1,
# own PORT and DATABASE_URL=pglite://./data/<dir>, see its docs/DEPLOY.md) behind a 40 ms RTT link:
node tools/net-bench/latency-proxy.mjs --listen 3491 --target 127.0.0.1:3490 --delay 20
node tools/net-bench/run.mjs auth after warm --base http://127.0.0.1:3491
```

`run.mjs` compiles `tools/net-bench/*.java` with the client's `backend/net` classes (javac + Gson from the Gradle
cache, no Gradle run, no Minecraft) and prints medians per mark, per request (TTFB/total), requests, bytes (as
received) and TCP connections (netstat). "before" reproduces the 0.3.1 request code (own clients, executors,
blocking sends). Never point the `auth` scenario at production: the dev login only works on a local backend.

In game: `Net.requestCount()` / `Net.byteCount()` count everything that went through the shared client.
