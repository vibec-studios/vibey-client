# Access gate (Vibe beta waitlist)

During the beta the Vibey Client only runs for players whose Vibe access is approved. Everyone else gets plain
Minecraft: no modules, no Vibey menus, no HUD, no server channel, no background traffic except the access check
itself. Contract: Vibe beta waitlist v1, section "Vibey Client" (shared with vibe-backend and vibe-launcher).

Code: `access/*` (Minecraft-free, JUnit), `client/access/*` (game side), `backend/BackendClient#fetchAccess`,
`ui/vui/corner/AccessHint` + `vui/title/access.vui`.

## Flow

1. First client tick: `Access.tick()` starts the gate for the current game account (same key as `auth.json`:
   backend URL + profile UUID).
2. A fresh positive cache entry (`<gameDir>/vibeyclient/access.json`, younger than 7 days) opens the gate at once.
   The check still runs in the background and renews the entry.
3. `BackendClient.fetchAccess` logs in when there is no stored login (Mojang join + `/auth/challenge` +
   `/auth/verify`, as before, but **without** opening the gateway) and asks `GET /api/v1/access`.
4. `approved`: cached (timestamp per account), gate open, `ModuleManager.start()` enables the modules the player
   had on. Anything else: cache entry removed, gate closed, hint on the title screen.

| Answer | Gate | Cache |
|---|---|---|
| `approved` | open | written (7 days, renewed by every successful check) |
| `waitlist` / `none` / `rejected` / unknown status | closed | removed |
| network error, 5xx, 429, Mojang down | open if cached, else closed ("Vibe nicht erreichbar") | unchanged |
| no login possible (offline account, logged out on purpose, 401) | open if cached, else closed | unchanged |
| `404` on `/api/v1/access` (backend without the waitlist) | open | written |
| online features off | open if cached, else closed, **no request** | unchanged |

Negative answers are never stored: the next start checks again.

## Closing while running

- Any authenticated REST call answering `403` with `ACCESS_PENDING|ACCESS_REJECTED|ACCESS_NONE`
  (`Rest.Auth#accessDenied`) and a gateway close `4403` (`Gateway`, no reconnect) call `BackendClient`'s
  access listeners. The gate forgets the cache entry and closes: `ModuleManager.suspend()` disables every module
  (without touching the saved on/off choice), every open Vibey screen closes (menus, onboarding, wardrobe, emote
  wheel, waypoints: any screen class under `dev.henny.vibeyclient.`), the social module stops the backend
  connection.
- After a denial of an open gate one `GET /api/v1/access` fetches the details (waitlist position). It may only
  keep the gate closed; reopening needs "Erneut prüfen". A denial while already closed asks nothing more (no loop
  when routes and `/access` disagree).

## Title screen

While the gate is not open the Vibey corner buttons are replaced by one subtle line (top right, `title/access.vui`):

- `Vibey Client: Du stehst auf der Warteliste (Platz X)` (`waitlist`, position from the backend)
- `Vibey Client: Tritt im Vibe Launcher der Beta-Warteliste bei` (`none`)
- `Vibey Client: Dein Beta-Zugang wurde abgelehnt` (`rejected`, the reason in the tooltip)
- plus "nicht erreichbar", "Anmeldung nötig", "Online-Funktionen sind aus"

"Erneut prüfen" runs the check as a user action: it switches online features on if they were off and allows a
login even after a deliberate logout. Strings: `core.access.*` (de_de, en_us).

## Switching without a restart

When "Erneut prüfen" answers `approved`, the modules start right away and the title screen is rebuilt with the
Vibey buttons. This is the same path as a normal start (`ModuleManager.start()` already runs on the first client
tick, after the game is up) and every module supports being enabled at runtime from the menu. Not caught up until
the next world join: the Vibey server hello (`VibeyNet.onJoin`) when the gate opens while already in a world.

## What stays active while closed

- `ModuleManager.load()` (settings, i18n, `onLoad` registrations): no network, no features.
- Vibey text/vui infrastructure for the hint, the `LauncherStatus` cleanup of a stale `session.json`.
- Dedicated server side (`ServerRelay`): unchanged, it is not a client feature.

Gated in `VibeyCore`: module start, keybinds, server rules, ranks sync, launcher session file, announcements, HUD,
world join hooks, incoming Vibey payloads, the Vibey menu key, onboarding, pause-menu row. Cosmetics stay off until
the cosmetics module is enabled (`applyConfig()` in `onLoad`).

## Development

In a dev workspace (`isDevelopment()`) the gate is off, so `runClient`, game tests and smoke runs behave as before.
`-Dvibeyclient.accessGate=on` turns it on (use `-Dvibeyclient.devAuth=true` against a local backend). Release
builds always gate. The Gradle runs forward the property to the game JVM.

With `-Dvibeyclient.devDriver=<port>` the dev driver answers `access` (phase, status, position, started modules) and
`access recheck` (same as "Erneut prüfen").

The cache is a convenience, not a security boundary: the backend answers `403 ACCESS_*` to every gated route
anyway, which closes the gate again.

## Tests

`./gradlew :fabric-1.21.11:test --tests 'dev.henny.vibeyclient.access.*'` plus `RestAccessDeniedTest` and
`BackendAccessSourceTest`: gate states, 7-day cache, outage with and without cache, denial while open, late
answers, online features off, error mapping, 403 detection in `Rest`.
