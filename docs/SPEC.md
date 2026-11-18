# Vibey Client – Build Spec (shared contract for all contributors)

This file is the single source of truth that every part of the project (client mod, backend extension, Blockbench
plugin, starter content) builds against. If you need to change a contract here, change this file in the same commit
and say so in your report.

## 0. Product in one paragraph

**Vibey Client** is a Minecraft client mod by Vibe Craft Studios (GitHub org `vibec-studios`, repo
`vibec-studios/vibey-client`, private for now). It bundles a modular core, a complete client UI in the Vibe design,
the existing *Vibey Multiplayer* and *Vibey Lyrics* mods as modules, social features (friends, requests, invites,
chats, presence) on top of the live Vibe backend (`https://vibe.henny.dev`), a data-driven cosmetic system (capes,
wings, auras, pets, shields, hats, back items), an emote system with props/particles/sounds, and Blockbench plugins
with live export into the running game. Logo/mascot: the blocky grey seal ("Robbe 1"). **No shop, no payments, no
prices anywhere** – everything in the starter set is free.

Local paths on the dev machine:

| What | Path |
|---|---|
| This repo | `C:\Users\henny\.t3\projects\vibey-client` |
| Vibey Multiplayer (source of the multiplayer module + build setup) | `C:\Users\henny\.t3\projects\vibey-multiplayer` |
| Vibey Lyrics (source of the lyrics module; working copy has uncommitted newer changes – use the working copy) | `C:\Users\henny\.t3\projects\vibey-lyrics` |
| Vibe backend (Node/Fastify/Postgres, private, auto-deploys on push to `main`!) | `C:\Users\henny\.t3\projects\vibe-backend` |
| Vibe launcher (design tokens `src/styles/globals.css`, seal model `src/components/seal3d.ts`, `branding/vibe-seal-*.png`) | `C:\Users\henny\.t3\projects\vibe-launcher` |

## 1. Targets and build

- Same matrix and build layout as Vibey Multiplayer: Stonecutter **0.9.5**, `settings.gradle` with
  `fabric(...)`/`neo(...)` targets, `versions/<name>/gradle.properties`, `gradle/common.gradle`,
  `gradle/fabric.gradle` (1.21.x, Mojang mappings, Loom remap), `gradle/fabric-26.gradle` (26.x, unobfuscated,
  Java 25), `gradle/neoforge.gradle` (ModDev), `buildAll` task collecting jars in `build/dist`.
- Targets: Fabric and NeoForge for 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8 (compiled against 1.21.6), 1.21.10
  (compiled against 1.21.9), 1.21.11, 26.1, 26.2, 26.3. `vcsVersion`/active version = `fabric-1.21.11`.
- Version differences go through `//? if >=x` Stonecutter comments, concentrated in `Compat*` classes and a few
  bridge classes (see how `vibey-lyrics` does `Gfx`, `BaseScreen`, `WorldSink`, `Compat`).
- **Never switch the Stonecutter active version** (it rewrites `src/`). Build other versions with
  `./gradlew :fabric-1.21.1:build` etc. – that processes sources into the subproject build dir without touching `src/`.
- Mod id `vibeyclient`, name `Vibey Client`, Java package `dev.henny.vibeyclient`, maven group `dev.henny`,
  archives `vibey-client-<loader>-<mc label>`. Asset namespace `vibeyclient`. License MIT (same as the other mods).
- `environment: "*"` – the same jar can go on a server (it then only relays emote/loadout packets, see §6).
  Client-only classes must never be loaded on a dedicated server.
- Lyrics module is **Fabric-only** (like the standalone mod). On NeoForge it is simply not registered.
- Release builds contain **no offline/cracked account support**. Dev-only code paths (e.g. backend dev login) are
  gated behind `isDevelopmentEnvironment()` **and** a system property.

## 2. Coexistence with the standalone mods

- Standalone *Vibey Multiplayer* has mod id `vibey` (package `dev.henny.vibey`, payload `vibey:modlist`).
- Standalone *Vibey Lyrics* has mod id `vibeylyrics` (package `dev.henny.lyrics`, assets namespace `vibeylyrics`).
- The client contains copies of their code under `dev.henny.vibeyclient.module.multiplayer` / `...module.lyrics`
  with **their own** ids/namespaces/config files (no shared class names, payload ids, asset paths or config files).
- If `vibey` / `vibeylyrics` is loaded, the matching module is auto-disabled ("provided by the installed standalone
  mod"), its payloads are not registered, and its mixins do not apply (mixin config plugin checks the mod list).
- Both standalone mods must keep working unchanged on their own.

## 3. Module API (core)

Required capabilities (the core author designs the concrete Java types; document them in `docs/MODULES.md`):

- `VibeyModule`: id, display name/description keys, icon, category, `dependencies()` (module ids),
  `conflictingMods()` (external mod ids → auto-disable), `defaultEnabled()`, `available()` (e.g. loader check),
  lifecycle `onLoad` (once, at startup, register settings/keybinds/packets), `onEnable`, `onDisable`, `tick`,
  plus hooks for HUD rendering and world join/leave.
- `ModuleRegistry`/`ModuleManager`: registration order, dependency resolution (disabling a dependency disables
  dependants), runtime enable/disable without restart wherever possible, state persisted.
- Typed settings per module (bool, int/float range, enum, string, color, keybind, action button), persisted as
  JSON in `config/vibeyclient/<module>.json`, auto-rendered in the settings UI, change listeners.
- Keybinds: declared in `onLoad` (registered with the loader at startup), only active while the module is enabled.
- Network channel: one envelope payload per direction; modules register typed messages by name. Server side
  relays (§6). Must handle "server does not have the mod" gracefully.
- Events/services bus: lightweight typed events (e.g. `WorldJoin`, `ScreenOpen`, `BackendConnected`) and a
  service locator so modules can talk (e.g. social asks multiplayer for the current host session).
- Global settings: language (auto/de/en), **online features on/off** (when off: no backend connection at all,
  social module hidden, cosmetics only local/server-packet), backend URL (advanced), UI scale/animations.
- Own i18n: `assets/vibeyclient/i18n/<lang>/<module>.json` merged at runtime (de_de + en_us required for every
  string). Vanilla lang files only for keybind names/categories and mod menu text.

Planned modules: `core-ui` (menus, title screen, toasts), `social`, `cosmetics`, `emotes`, `multiplayer`, `lyrics`,
`devlink` (Blockbench live link), `seal` (mascot easter eggs, optional).

## 4. Design system ("Vibe")

From the Vibe Launcher (`vibe-launcher/src/styles/globals.css`):

```
bg #000000   surface #0b0b0e   surface2 #121217   surface3 #18181f   panel #08080b
line rgba(255,255,255,.09)   line2 rgba(255,255,255,.16)
text #f4f4f5   muted #a1a1aa   dim #6b6b75
brand #8b6cff (purple)   brand2 / ok #3fe0c5 (teal)   danger #ff5470   warn #ffb547
```

- Square corners everywhere, 1 px lines, no gradients except subtle brand glows, no rounded buttons.
- Short snappy animations: 140–160 ms, ease `cubic-bezier(0.2, 0.8, 0.2, 1)`; fade/slide 6–8 px; never block
  input during an animation; no per-frame allocations that cause stutter.
- Pixel wordmark "VIBEY" may use a pixel font look; body text uses the Minecraft font (no huge custom fonts).
- Seal logo: `assets/icon.png` (512 px) in this repo; bigger renders in `vibe-launcher/branding/`.

## 5. The `.vibey` asset format (v1)

One self-contained UTF-8 JSON file per cosmetic or emote, extension `.vibey`. The client loads it directly; the
Blockbench plugin writes it; the backend stores and serves it unchanged (identified by SHA-256 of the file bytes).
Max size 2 MiB (backend-configurable).

```jsonc
{
  "format": "vibey",
  "formatVersion": 1,
  "kind": "cosmetic",                 // "cosmetic" | "emote"
  "id": "vibey:seal_buddy",           // namespace:path, [a-z0-9_.-], namespace "vibey" = official
  "name": { "en": "Seal Buddy", "de": "Robbenkumpel" },
  "description": { "en": "…", "de": "…" },   // optional
  "author": "Vibe Craft Studios",
  "tags": ["seal", "cute"],
  "rarity": "common",                 // common | rare | epic | legendary (cosmetic only, no prices!)

  "cosmetic": {                        // only for kind = cosmetic
    "slot": "pet",                    // cape | wings | aura | pet | shield | hat | back
    "hideWithArmor": ["chest"],       // optional: hide when this armor slot is worn (head|chest|legs|feet)
    "pet": { "follow": "shoulder_right", "scale": 1.0 },   // slot specific options, all optional
    "cape": { "texture": "cape" },     // cape: 64x32 vanilla cape layout texture, rendered by the vanilla cape model
    "shield": { "texture": "shield" }  // shield: 64x64 vanilla shield texture layout (base + face)
  },

  "emote": {                           // only for kind = emote
    "animation": "main",              // name in "animations"
    "loop": false,
    "thirdPerson": true,              // switch the local player to third person while playing
    "stopOnMove": true,               // movement input cancels the emote
    "hideHeldItems": true
  },

  "textures": [
    { "id": "main", "width": 64, "height": 64, "png": "<base64 PNG>",
      "animation": { "frameTime": 2, "frames": [0,1,2] } }   // optional: vertical strip, like .mcmeta
  ],

  "model": {                           // optional for capes/shields, required otherwise
    "texture": "main",                // default texture for cubes
    "bones": [
      {
        "name": "left_wing",
        "parent": null,               // bone name or null
        "attach": "body",            // only for root bones: head | body | right_arm | left_arm | right_leg | left_leg | root | world
        "pivot": [2, 22, 2],          // Blockbench coordinates (see below)
        "rotation": [0, 0, 0],        // degrees, Blockbench order/sign
        "cubes": [
          { "from": [1, 14, 2], "to": [9, 24, 3], "inflate": 0, "mirror": false,
            "pivot": [0,0,0], "rotation": [0,0,0],          // optional per-cube rotation
            "uv": { "box": [0, 0] }                          // box UV (Blockbench "box_uv") ...
            // ... or per face: "uv": { "north": [u1,v1,u2,v2], "east": …, "south", "west", "up", "down" }
            //     optional per-face "texture": "<id>", omitted faces are not drawn
          }
        ],
        "emissive": false,            // full-bright (glowing) rendering
        "translucent": false
      }
    ],
    "locators": { "hand_tip": { "bone": "right_arm_prop", "position": [0, 10, 0] } }  // for particles/sounds
  },

  "animations": {
    "idle": {
      "length": 2.0, "loop": true,                // seconds
      "bones": {
        "left_wing": {
          "rotation": [ { "t": 0, "v": [0, 0, 0], "i": "catmullrom" }, { "t": 1, "v": [0, 30, 0] } ],
          "position": [ … ], "scale": [ … ]       // i = linear (default) | catmullrom | step | bezier(not required)
        }
      },
      "events": [
        { "t": 0.5, "type": "particle", "particle": "minecraft:heart", "locator": "hand_tip",
          "count": 3, "spread": [0.2, 0.2, 0.2], "velocity": [0, 0.05, 0] },
        { "t": 0.5, "type": "particle", "sprite": "sparkle", "locator": "hand_tip", "count": 4,
          "lifetime": 0.8, "size": 0.15, "spread": [0.3,0.3,0.3], "velocity": [0,0.08,0], "gravity": -0.01, "emissive": true },
        { "t": 0.0, "type": "sound", "sound": "minecraft:entity.player.levelup", "volume": 1, "pitch": 1 },
        { "t": 1.0, "type": "sound", "sound": "pop" },                 // id of an embedded sound
        { "t": 1.2, "type": "visibility", "bone": "guitar", "visible": false }
      ]
    }
  },

  "sprites": [ { "id": "sparkle", "texture": "main", "uv": [0, 48, 8, 56] } ],   // optional, for sprite particles
  "sounds": [ { "id": "pop", "ogg": "<base64 OGG Vorbis>" } ]                   // optional
}
```

**Coordinate convention (identical to Blockbench "Modded Entity" / the vanilla player in Blockbench):** units are
pixels (16 = one block), origin at the player's feet, +Y up, the player faces **north (-Z)**, the player's right arm
is at -X. Vanilla player part pivots in this space: head/body `[0, 24, 0]`, right arm `[-5, 22, 0]`, left arm
`[5, 22, 0]`, right leg `[-1.9, 12, 0]`, left leg `[1.9, 12, 0]` (Blockbench shows the same reference player).
A root bone with `attach: "right_arm"` moves with the right arm: the client converts its pivot into the arm's local
space (MC model space is `x' = x`, `y' = 24 - y`, `z' = z`, rotations convert like Blockbench's Modded Entity
exporter). Rule that must hold: **what the creator sees on the reference player in Blockbench is exactly what the
game renders.** `attach: "root"` follows body yaw but not part animations; `attach: "world"` (pets, auras) is
positioned relative to the player's feet with the body yaw, and the client may smooth it (pets follow with lag).

**Emotes:** the `animations` of an emote may animate the six player bones by their vanilla names (`head`, `body`,
`right_arm`, `left_arm`, `right_leg`, `left_leg`) plus `root` (moves/rotates the whole player). Values are
offsets/rotations relative to the default standing pose, in the same Blockbench convention. Props are normal model
bones attached to a player part (`attach`). Events add particles and sounds. The layer of the player (hat/jacket/
sleeves/pants) follows its part automatically.

**Animation rules:** channel values are interpolated between keyframes; missing channels = rest pose; `loop`
animations wrap; emotes fade in/out over 0.15 s when starting/stopping. Cosmetic animation names with meaning:
`idle` (always), `walk` (blended by walk speed), `fly` (elytra/creative flying – wings), `sneak`.

Validation rules (client + backend): `format == "vibey"`, `formatVersion == 1`, `id` matches
`^[a-z0-9_.-]+:[a-z0-9_./-]+$`, kind/slot valid, all referenced textures/bones/sounds/sprites exist, textures are
PNG ≤ 512×512 (animated strips ≤ 512×4096), ≤ 256 bones, ≤ 2048 cubes, sounds OGG ≤ 512 KiB each.
Unknown fields are ignored (forward compatible).

## 6. Sync of cosmetics and emotes between players

Two independent channels; the client uses whatever is available and merges by player UUID:

1. **Backend (works on any server, even vanilla):** see §7. The client subscribes to the UUIDs of players around it
   and gets their loadouts and emote plays via the gateway.
2. **Server packets (only when the mod is also on the server, incl. hosted/LAN worlds):** custom payloads
   `vibeyclient:hello` (C→S: protocol version; S→C: ack), `vibeyclient:loadout` (C→S: own `{slot: id}`;
   S→C: `{uuid, slots}`), `vibeyclient:emote` (C→S: `{emoteId, action: play|stop}`; S→C: `{uuid, emoteId, action,
   startTick}`), plus the generic module envelope `vibeyclient:msg` (C→S: `{target uuid or nil, channel, bytes ≤ 8 KiB}`;
   S→C: `{sender uuid, channel, bytes}`, used by `VibeyNet.channel(...)`). The server just validates sizes/rates and
   relays to every other player that sent `hello`. Asset
   bytes never go over the server channel – clients fetch bundles by id from the backend catalog or use built-ins.

Built-in starter bundles also ship inside the jar (`assets/vibeyclient/builtin/*.vibey`) so cosmetics work offline
and when online features are off (then only visible to yourself and via server packets).

## 7. Backend contract (to be added to vibe-backend; existing endpoints must not change)

Feature flag `cosmetics` in `GET /api/v1/meta` → `features`. All new REST under `/api/v1/cosmetics`.

```ts
type CosmeticSlot = "cape" | "wings" | "aura" | "pet" | "shield" | "hat" | "back";
type CatalogItem = {
  id: string;                         // "vibey:seal_buddy" (from the bundle)
  kind: "cosmetic" | "emote";
  slot: CosmeticSlot | null;          // null for emotes
  name: { en: string; de?: string };
  description: { en?: string; de?: string };
  author: string; tags: string[];
  rarity: "common" | "rare" | "epic" | "legendary";
  availability: "default" | "granted"; // default = every user owns it; granted = only users it was granted to
  asset: { hash: string; size: number; url: string };   // url = /api/v1/cosmetics/assets/<sha256>
  thumbnailUrl: string | null;
  sort: number; createdAt: string; updatedAt: string;
};
type Loadout = {
  slots: Partial<Record<CosmeticSlot, string | null>>; // item id per slot
  emoteWheel: (string | null)[];                       // exactly 8 entries
};
```

| Method | Path | Auth | |
|---|---|---|---|
| GET | `/api/v1/cosmetics/catalog` | – | `{ version, items: CatalogItem[] }` (only enabled items; ETag/`If-None-Match`) |
| GET | `/api/v1/cosmetics/assets/{sha256}` | – | the `.vibey` bytes, `Cache-Control: public, max-age=31536000, immutable` |
| GET | `/api/v1/cosmetics/assets/{sha256}/thumbnail.png` | – | optional thumbnail (404 if none) |
| GET | `/api/v1/cosmetics/me` | Bearer | `{ owned: string[], loadout: Loadout }` (owned includes all `default` items) |
| PUT | `/api/v1/cosmetics/loadout` | Bearer | body `Loadout` → `Loadout`; 403 `forbidden` if an item is not owned, 400 if slot mismatch |
| POST | `/api/v1/cosmetics/lookup` | Bearer | `{ userIds: string[] (≤200) }` → `{ users: Record<userId, { slots }> }` (only Vibe users) |

Gateway (`/ws`), new client ops and events (existing ones unchanged):

| Direction | op / t | d |
|---|---|---|
| C→S op | `cosmetics.subscribe` | `{ userIds: string[] }` (≤ 200, replaces the previous set; empty = none) |
| C→S op | `emote` | `{ emoteId: string, action: "play" \| "stop" }` (must be owned; rate limit ~1/s) |
| S→C event | `loadout.updated` | `{ userId, slots }` – to everyone subscribed to `userId` and to the user's other sessions |
| S→C event | `emote.played` | `{ userId, emoteId, action, at }` – to everyone subscribed to `userId` (not echoed to the sending connection) |

Admin (`/admin/api/...`, existing cookie + CSRF + roles + audit log): list/search/filter cosmetics, upload a
`.vibey` (validated, metadata extracted), replace the asset (new version), edit metadata (names, description,
rarity, availability, sort, enabled), delete/disable, optional thumbnail upload, grant/revoke items per user
(also visible on the user detail page), stats (owners, equipped count). Admin UI page "Cosmetics" in the existing
Essential-like admin structure, original look. Seed: `apps/api/seed/cosmetics/*.vibey` are imported/updated on
startup as `availability: default` (upsert by id when the hash changed; admin edits of metadata are kept).

## 8. Blockbench live link ("devlink")

- The client (module `devlink`, setting "Creator mode", default **off**; on by default in the dev environment)
  runs a WebSocket server bound to **127.0.0.1 only**, default port **47821** (configurable).
- A random token (24+ chars, regenerable) is shown in the settings with a copy button and written to
  `<gameDir>/vibeyclient/devlink.json` as `{ "port": 47821, "token": "…", "pid": 1234 }` so the plugin can find it.
- Non-loopback connections and wrong tokens are rejected (close 4001). Max message size 8 MiB.
- Messages (JSON text frames):
  - plugin → game `{ "type": "hello", "token": "…", "client": "blockbench-vibey", "version": "1.0.0" }`
  - game → plugin `{ "type": "welcome", "game": "1.21.11", "mod": "1.0.0", "player": "Name" }`
  - plugin → game `{ "type": "push", "bundle": { ….vibey JSON… }, "action": "equip" | "play" | "none" }`
    → game loads it as a **dev item** (badge "DEV", local only, never sent to the backend or server), hot-replaces
    any previous version with the same id, equips it (cosmetic) or plays it on the local player (emote) and answers
    `{ "type": "pushed", "id": "…", "ok": true, "warnings": [] }` or `{ "type": "error", "id": "…", "message": "…" }`.
  - plugin → game `{ "type": "remove", "id": "…" }`, `{ "type": "ping" }` ↔ `{ "type": "pong" }`.
  - game → plugin `{ "type": "log", "level": "info"|"warn", "message": "…" }` (optional).
- Plugin side: auto-push on change (debounced ~300 ms, toggle), "Push now", "Play emote", connection status in the
  status bar.

## 9. Blockbench plugin

`blockbench/` in this repo. One plugin file `vibey.js` (plus README) with two model formats: **Vibey Cosmetic** and
**Vibey Emote**. Both show the vanilla player reference (toggleable), let the user pick slot/attach per root group,
edit metadata (id, names en/de, rarity, tags, emote options), keyframe animations with Blockbench's animator,
particle/sound events via Blockbench effect keyframes (or a custom panel), embed textures/sounds, import and export
`.vibey`, and do the live link from §8. Emote format: the player bones are real groups named `head`, `body`,
`right_arm`, `left_arm`, `right_leg`, `left_leg`, `root` so they can be animated directly.

## 10. Quality bar

- Verify for real: build all targets, start the client (`runClient` / client gametests with screenshots), check
  cosmetics and emotes in game, test the live link from real Blockbench, test social against a **local** backend
  instance with `AUTH_DEV_BYPASS` for multiple accounts. Never use the dev bypass against production.
- No copied third-party cosmetics, emotes, textures, animations or UI assets (no Essential assets). Everything
  original.
- No per-frame stutter: no blocking I/O on the render thread, assets load async, textures upload on the main
  thread in small batches.
- German and English for every user-facing string.
