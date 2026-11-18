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
pixels (16 = one block), origin at the player's feet, +Y up, the player faces **north (-Z)**, so the player's right
arm is at **+X** (exactly like Blockbench's own player template). Vanilla player part pivots in this space:
head/body `[0, 24, 0]`, right arm `[5, 22, 0]`, left arm `[-5, 22, 0]`, right leg `[1.9, 12, 0]`, left leg
`[-1.9, 12, 0]`; cubes: head `[-4,24,-4]..[4,32,4]`, body `[-4,12,-2]..[4,24,2]`, right arm `[4,12,-2]..[8,24,2]`,
left arm `[-8,12,-2]..[-4,24,2]`, right leg `[-0.1,0,-2]..[3.9,12,2]`, left leg `[-3.9,0,-2]..[0.1,12,2]`.
A root bone with `attach: "right_arm"` moves with the right arm: the client converts its pivot into the arm's local
space. **MC model space is `x' = -x`, `y' = 24 - y`, `z' = z`** (this is what Blockbench's Modded Entity exporter
does; e.g. right arm `[5, 22, 0]` → vanilla `PartPose.offset(-5, 2, 0)`, its cube → `addBox(-3, -2, -2, 4, 12, 4)`).
Rotations (bone rest rotations *and* rotation keyframes, degrees, Blockbench euler order ZYX = MC `rotationZYX`)
convert to `(-x, -y, z)`; position keyframes (offsets) to `(-x, -y, z)`; scale keyframes are unchanged. (Mapping
`x' = +x` would render a mirror image of what Blockbench shows – text reversed, east/west box-UV sides swapped.)
Rule that must hold: **what the creator sees on the reference player in Blockbench is exactly what the
game renders.** `attach: "root"` follows body yaw but not part animations; `attach: "world"` (pets, auras) is
positioned relative to the player's feet with the body yaw, and the client may smooth it (pets follow with lag).

**Format details (as written by the Blockbench plugin):**
- `attach` is required on root bones (`parent: null`) and ignored on child bones. Bone names are unique.
- Cubes: `from`/`to`/`pivot` are absolute model coordinates (like bone pivots). `uv.box: [u, v]` = Blockbench/Minecraft
  box UV with the **unfloored** cube size (like `ModelPart`), `inflate` does not change UVs, `mirror` only applies to
  box UV. Per-face UV: each face is `[u1, v1, u2, v2]` or `{ "uv": [u1, v1, u2, v2], "texture": "<id>" }`; omitted
  faces are not drawn. Optional cube-level `"texture"` (texture of a box-UV cube / default for its faces). Texture
  lookup: face `texture` → cube `texture` → `model.texture` → first texture. UVs are in the texture's `width`×`height`.
- Textures: `width`/`height` are the UV size of **one frame**. The PNG may have a higher resolution with the same
  aspect ratio (HD) and may be a vertical strip of N frames, N = `round(pngHeight * width / (pngWidth * height))`.
  `animation.frames` defaults to `0..N-1`; optional `animation.interpolate: true` blends frames (like `.mcmeta`).
- Locators: `position` is in absolute model coordinates (rest pose); `bone` is a model bone or, in emotes, a player
  bone name. The locator follows the bone's animated transform.
- Keyframes: `v` is the value at `t`; optional `pre` is the value arriving at `t` from the previous keyframe (a jump).
  Between keyframes A→B: hold A if `A.i == "step"`; Catmull-Rom (neighbours as control points) if A or B is
  `catmullrom`; otherwise linear (Blockbench's rules). Before the first / after the last keyframe the value is held;
  looping does not interpolate across the loop end. `bezier` keyframes may carry a `bezier` handle object (optional).
- Animations may set `"hold": true` (with `loop: false`): keep the last frame until stopped (emotes).
- `model` is optional for capes, shields and emotes without props. Emote bones must not use the player bone names.
  In emotes, cubes placed directly in a player bone are exported as a root bone `<part>_cubes` with `attach: <part>`,
  pivot = the part pivot and `"generated": true` (editor hint). Cubes may carry an optional `name` (editor only).
- Events: `particle` must be a namespaced particle id (`minecraft:heart`) unless `sprite` is set. `sound` is a
  namespaced sound event id or the id of an embedded sound; sound events may have an optional `locator`.
  `visibility` events target model bones (not player bones); the state at `t` holds until the next event.
- `emote.loop` equals the `loop` of the emote's animation.

**Emotes:** the `animations` of an emote may animate the six player bones by their vanilla names (`head`, `body`,
`right_arm`, `left_arm`, `right_leg`, `left_leg`) plus `root` (moves/rotates the whole player). Values are
offsets/rotations relative to the default standing pose, in the same Blockbench convention. Props are normal model
bones attached to a player part (`attach`). Events add particles and sounds. The layer of the player (hat/jacket/
sleeves/pants) follows its part automatically.

**Animation rules:** channel values are interpolated between keyframes; missing channels = rest pose; `loop`
animations wrap; emotes fade in/out over 0.15 s when starting/stopping. Cosmetic animation names with meaning:
`idle` (always), `walk` (blended by walk speed), `fly` (elytra/creative flying – wings), `sneak`.

**Clarifications (binding; the Blockbench convention above wins everywhere):**

1. *X axis:* `.vibey` coordinates are **Blockbench space** (right arm at **+X**), converted to MC model space with
   `x' = -x, y' = 24 - y, z' = z` exactly as described above. The Blockbench plugin writes Blockbench's numbers
   unchanged. Everything that is a direction or offset in model space (position keyframes, particle `spread` and
   `velocity`, locator positions) follows the same handedness.
2. *Animated textures:* see "Format details" (frame size, vertical strip, `frameTime` in ticks).
3. *Interpolation:* Blockbench's rules as in "Format details".
4. *Visibility events:* a bone's visibility is the value of its last event at or before `t` (default visible);
   hidden bones hide their children. Bones that start hidden get an event at `t = 0`.
5. *Particle units:* `spread` in blocks, `velocity` in blocks/tick, `gravity` in blocks/tick² (positive falls),
   `size` in blocks, `lifetime` in seconds. The client adds a little randomness inside `spread`.
6. `pet.follow`: `"ground"` (default; walks/hops beside the player) or `"shoulder_right"`/`"shoulder_left"`/
   `"float"`. `pet.scale` scales about the model origin; the client chooses the offset beside the player.
7. Emotes: the `root` bone pivots at the feet `[0,0,0]`; the six player parts are its children, like vanilla.
8. Emotes also carry `rarity` (the catalog requires it).
9. *Catmull-Rom in looping animations* (≥ 3 keys): like Blockbench, the missing neighbour at either end is taken
   from the other end of the loop.

**Forward compatibility (client, binding since 0.2.0):** unknown fields are ignored everywhere; unknown event types
and animation names are skipped, unknown interpolations play as `linear`, unknown `attach` values fall back to
`body` (emotes `root`) – all with a warning. Optional top-level `minClientVersion` (also a catalog item field):
clients older than that (and clients that see `formatVersion` > 1) list the item greyed out ("Update Vibey Client")
and never render it. Unknown `kind`/`slot` values make the item invisible to that client (logged once).
Cosmetic animation events: `idle` always, `walk`/`fly`/`sneak` while that animation has ≥ 50 % weight;
visibility follows the strongest playing animation that has visibility events.

Validation rules (client + backend): `format == "vibey"`, `formatVersion == 1`, `id` matches
`^[a-z0-9_.-]+:[a-z0-9_./-]+$`, kind/slot valid, all referenced textures/bones/sounds/sprites exist, textures are
PNG ≤ 512×512 (animated strips ≤ 512×4096), ≤ 256 bones, ≤ 2048 cubes, sounds OGG ≤ 512 KiB each.
Unknown fields are ignored (forward compatible).

## 6. Sync of cosmetics and emotes between players

Two independent channels; the client uses whatever is available and merges by player UUID:

1. **Backend (works on any server, even vanilla):** see §7. The client subscribes to the UUIDs of players around it
   and gets their loadouts and emote plays via the gateway.
2. **Server packets (only when the mod is also on the server, incl. hosted/LAN worlds):** custom payloads
   `vibeyclient:hello` (C→S: protocol version; S→C: ack), `vibeyclient:loadout` (C→S: own `{slot: id}` + signed
   token; S→C: `{uuid, slots, token}`, see *Signed loadouts* below), `vibeyclient:emote` (C→S: `{emoteId, action: play|stop}`; S→C: `{uuid, emoteId, action,
   startTick}`), plus the generic module envelope `vibeyclient:msg` (C→S: `{target uuid or nil, channel, bytes ≤ 8 KiB}`;
   S→C: `{sender uuid, channel, bytes}`, used by `VibeyNet.channel(...)`). The server just validates sizes/rates and
   relays to every other player that sent `hello`. Since 0.2.0 it also announces peers: on `hello` the newcomer gets
   a (possibly empty) `loadout` of every peer and every peer an empty `loadout` of the newcomer, so clients know who
   runs Vibey Client (nametag icons, see §11). Asset
   bytes never go over the server channel – clients fetch bundles by id from the backend catalog or use built-ins.

**Signed loadouts (protocol 2, after 0.2.0; backend contract: vibe-launcher `docs/VIBE-API-2.md` §6).** The server
channel is not trusted: a modified client could claim any item. So:

- The backend publishes its Ed25519 key in `GET /api/v1/meta` as `loadoutSigning: {alg: "Ed25519", keyId, publicKey}`
  (`publicKey` = base64 of the 32 raw bytes, `keyId` = first 8 hex chars of SHA-256 over them) or `null`. The client
  reads it on login and whenever the menus fetch meta (`backend/LoadoutKeys`), checks `keyId`, and caches it per
  backend URL in `<game dir>/vibeyclient/cache/loadout-key.json`. `null` (or a backend without the field) removes it.
  A token with an unknown `kid` triggers a meta refetch (at most every 5 min; key rotation).
- Our token comes from `GET /api/v1/cosmetics/loadout/signed` → `{token, payload}` and from the `signed` field of the
  `PUT /api/v1/cosmetics/loadout` answer; it is fetched after every loadout/ownership change and again 30 min before
  `exp`. 404/501/413/`feature_disabled` = backend without signing → retry every 30 min, nothing breaks.
  `token = base64url(payloadJson) + "." + base64url(ed25519(payloadJson))`,
  `payload = {v: 1, kid, uuid, slots: {slot: id|null}, emotes: [id…], iat, exp}`.
- Wire: `hello` carries the protocol (now **2**). `loadout` = `uuid, varint n, n × (slot, id)` as in protocol 1,
  followed by `utf(token)` only when a token is attached (≤ 3072 chars on the wire; the backend keeps them ≤ 2 KiB). A
  tokenless packet is byte-identical to protocol 1 and the decoder reads the token only if bytes are left. Clients
  attach the token only when the server's hello said ≥ 2; the server strips it for peers whose hello said < 2. Older
  clients and servers therefore never see the extra bytes (their decoders would reject them).
- The server relay stores each peer's token with its loadout and forwards it unchanged (also to late joiners); it
  only checks length and characters (`[A-Za-z0-9_-]*.[A-Za-z0-9_-]*`) plus the existing rate limits. It never
  verifies signatures.
- Receivers verify with the JDK's Ed25519 (`net/LoadoutToken`, raw key wrapped as X.509 with prefix
  `302a300506032b6570032100`): signature over exactly the bytes of the first part, `v == 1`, `kid` = meta key,
  `uuid` = the player the server says sent it, `exp > now − 5 min`.
  - **Valid:** the slots of the token `payload` are shown (plus claimed slots whose item is available to everyone).
    It then competes with the backend loadout of that player (newest wins), as before.
  - **Missing/invalid/expired or no key:** only claimed items with availability `default` are shown (catalog
    `default`, or built-in and not overridden by the catalog). The rest is ignored and the player is looked up via
    `POST /api/v1/cosmetics/lookup` when the backend is available (at most once a minute per player); unverified
    server data never overrides what the backend says.
  - **Emotes over the server channel:** `default` emotes always; `granted` emotes only when they are listed under
    `emotes` in the sender's last valid (not expired) token on this server. Stops always pass. Gateway emotes come
    from the backend and are not filtered.

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
  - Submit to Vibe (since 0.2.0; the game forwards with its own Vibe login, VIBE-API-2 §3), every request carries a
    `requestId` that the answer repeats; failures are `{ "type": "error", "requestId", "message", "code"? }`:
    plugin → game `{ "type": "submit", "requestId", "bundle": {…}, "name", "description", "thumbnail"?: "<base64 PNG ≤ 256×256>" }`
    → `{ "type": "submitted", "requestId", "submission": {…} }` (multipart `POST /api/v1/cosmetics/submissions`);
    `{ "type": "submissions", "requestId" }` → `{ "type": "submissions", "requestId", "submissions": […] }`;
    `{ "type": "withdraw", "requestId", "submissionId" }` → `{ "type": "withdrawn", "requestId", "submission" }`.
    game → plugin `{ "type": "submission", "submission": {…} }` when the gateway reports `submission.updated`.
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

## 11. Round 2 (0.2.0): nametag icons, OTA cosmetics, distribution

Contract with the backend: `vibe-launcher/docs/VIBE-API-2.md`. Client side:

- **Nametag icons** (module `nametags`): a seal glyph before the name of players who run Vibey Client (lookup
  `client == "vibey"`, `loadout.updated`, any `vibeyclient:*` packet from them through the server, and ourselves) plus
  badge glyphs (`staff`, `creator`, `supporter`; unknown badges ignored, max 2) in the world nametag and the tab list.
  Glyphs U+E000–E003 of the own bitmap font `vibeyclient:icons`, prepended as a component (the vanilla name
  component is kept). Settings: nametag, tab list, badges.
- **OTA cosmetics:** catalog polled every 10 min (+0–3 min jitter), 3 s after world join, on `catalog.updated`
  (0–5 s jitter) and when `ready.catalogVersion` differs; ETag/304; unchanged hashes keep their parsed bundle; new
  assets download in parallel (4) on workers and swap in at once; owned list reloaded on `catalog.updated` (grants);
  one "New cosmetics available" toast per batch (setting). Catalog `creator` → "by <name>" in the wardrobe (the
  all-zero UUID = deleted user, localised).
- **Release:** `.github/workflows/release.yml` + `tools/release-manifest.mjs` (exact Minecraft versions per jar in
  `gradle/mc-versions.json`, checked by `./gradlew checkMcVersions`). CI attaches all jars, `vibey.js` and
  `vibey-manifest.json` (the §2 manifest plus `sha256`/`size` per file, `file` = asset name, uploaded last) to the
  GitHub release; the backend imports releases from GitHub (CI never calls vibe.henny.dev: Cloudflare's bot
  protection blocks GitHub runners). Manual fallback: `release-manifest.mjs --publish` from a dev machine (files
  staged in batches below the proxy limit, then the release by sha256).
