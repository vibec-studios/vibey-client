# Names and ranks (module `nametags`)

What other players see next to a name: the seal (runs Vibey Client), the player's rank icon, one extra badge and an
animated name style. Ranks are cosmetic identity given by the Vibe team, never permissions. Contract: [SPEC.md §13](SPEC.md),
backend: vibe-backend `docs/RANKS.md` and `docs/PROTOCOL.md` § Ränge und Namensstile.

```
[seal][rank][badge] Name        world nametag, tab list, chat (player chat only)
[rank][badge] Name              friends list (social), rank chip with label on the profile
```

## Code map

| Package | Job |
|---|---|
| `ranks` (pure, no Minecraft) | `RanksDoc`/`RankDef`/`NameStyleDef` + `RanksDocJson` (`GET /api/v1/ranks`), `Identity` + `IdentityJson` (rank, tags, nameStyle of one player; falls back to `LegacyBadges` when the backend has no `rank` field), `OwnRanks` (`Me.ranks`, `privacy.showRank`) |
| `ranks/style` (pure) | The colour function: `ColorMath` + one record per effect (`GradientStyle`, `WaveStyle`, `RainbowStyle`, `PulseStyle`, `ShimmerStyle`), `NameStyleJson` |
| `client/ranks` | `Ranks` (current definitions + own ranks, version counter for caches), `RanksCache` (disk copy + ETag GET), `RanksSync` (gateway listeners), `PlayerEntry` (one lookup entry → `VibeyPlayers`), `MyRanksApi` (own choices) |
| `client/names` | Drawing shared by modules: `IconFont`, `RankGlyphs` (rank → glyph), `NamePrefix`, `NameRecolor` (style on the name segment), `NameLine` (UI lines), `StyleClock`, `Profiles` (GameProfile compat) |
| `module/nametags` | `NameIcons` (entry for the mixins), `Surface` (settings + server rules → flags), `Decorator`, `DecorCache`, `AnimatedSet`, settings page (`NametagsPanel`, `MyNameCard`, `Chips`, `MyStyles`) |
| `module/nametags/mixin` | `NameTagMixin` (world nametag), `TabListMixin`, `ChatListenerMixin` |
| `module/social/ui/SocialNames` | Friends list name + profile rank chip |

## Data flow

- **Definitions:** at start the disk copy `vibeyclient/cache/ranks.json` (+ `.etag`) is shown. A conditional GET of
  `/api/v1/ranks` runs only when `ready.ranksVersion` or `ranks.updated {version}` (0–5 s jitter) differs from the
  version we have. Old backends send no `ranksVersion`: nothing is fetched. The bootstrap request is not used: the
  version in `ready` already says whether anything changed, so an unchanged document costs no request at all.
- **Per player:** the cosmetics lookup, `loadout.updated` and `nametag.updated` carry `rank`, `tags`, `nameStyle`
  (`PlayerEntry` → `VibeyPlayers.identity`). Without `rank`/`nameStyle` in the entry the old `badges` become
  rank `staff`/`supporter`/`creator` with the old glyphs. Friends come from `PublicUser` (`Model.User.identity`).
- **Own:** `ready.user` / `me.updated` → `OwnRanks`. When `showRank` is off, our own rank is hidden locally too, so we
  see what others see. The picker calls `PUT /api/v1/me/name-style`, `PUT /api/v1/me/rank`,
  `PATCH /api/v1/me {privacy:{showRank}}`; each answer is the new `Me`.

## Icons

Glyphs of the bitmap font `vibeyclient:icons` (7×7 art in 8×8 cells, ascent 7), so the text pipeline keeps working:
nametag background width, see-through pass, shadows, tab column width, chat wrapping, other mods restyling names.
Rows of 16 cells: row n = U+E0n0–U+E0nF.

| Code | Icon id | Used by |
|---|---|---|
| U+E000 | seal | runs Vibey Client |
| U+E001 | staff | old backends (`badges: staff`) |
| U+E002 | heart | free for ranks with `icon: heart` |
| U+E003 | pencil | creator |
| U+E010 | shield-crown | owner |
| U+E011 | shield | admin |
| U+E012 | shield-hammer (`hammer`) | mod |
| U+E013 | shield-code (`code`) | dev |
| U+E014 | star | partner |
| U+E015 | flask | beta |
| U+E016 | crown | supporter |
| U+E017–E019 | check, gem, flame | spare for new ranks |
| U+E02F | generic | any rank whose icon the client does not know, tinted with `RankDef.color` |

The eight system ranks use their glyph by rank id (staff = shields, supporter = crown); other ranks by `icon`, then
the tinted generic glyph. `node tools/gen-nametag-icons.mjs [--preview big.png]` writes `textures/font/icons.png`,
`font/icons.json` and **`docs/rank-icons.json`** (palette + 7×7 rows per icon + rank → icon map) for the backend
admin UI and the website (draw each pixel as an SVG rect; `tint: true` = multiply with the rank colour).

## Name styles

`colorAt(index, length, t)` in `ranks/style` is a line-by-line port of the backend's `nameStyleColorAt`
(formulas in vibe-backend `docs/RANKS.md`): doubles only, the same order of operations, `floor(x + 0.5)` rounding,
no trigonometry. `NameStyleVectorsTest` checks all 280 shared vectors (`src/junit/resources/ranks/`, copied from
`packages/shared/vectors/name-style-vectors.json`; copy it again when the backend changes it).

- Colour only, one component per character. Applied to the first text segment that contains the profile name;
  never when that segment has a colour other than white (team colours, rank prefixes of the server win). Not found
  (nick plugins) → icons only.
- Time is bucketed to 50 ms (≤ 20 rebuilds per second and player); unchanged input costs no allocation
  (`DecorCache`). World nametags: only the 16 nearest players with an animated style animate (`AnimatedSet`,
  updated each tick), the rest show t = 0. Tab list animates while it is open. Chat is static (t = 0): lines are
  laid out once when they arrive.
- Chat: `ChatListener.handlePlayerChatMessage` (signed player chat, same on 1.21.1–26.x); the sender name inside the
  chat type decoration gets the icons, the server's format stays. System messages are never touched.

## Settings and server rules

Module page: *My name* (preview, style chips, rank chips when 2+ ranks can be primary, "Show my rank"), *Where*
(above players, player list, chat), *Icons* (rank, extra badge), *Name styles* (on, animated).

Server rules (docs/SERVER-RULES.md): `nametags.seal`, `nametags.ranks` (rank + badge), `nametags.styles`,
`nametags.*`. They apply to the world nametag, the tab list and new chat lines; the own social UI is unaffected.

## Testing

- `./gradlew :fabric-1.21.11:test` – vectors, `RanksDocJson`, `IdentityJson`.
- `RanksGameTest` against a local backend (no Docker, pglite):

```sh
# vibe-backend apps/api, fresh data dir
NODE_ENV=development AUTH_DEV_BYPASS=1 PORT=3471 DATABASE_URL=pglite://<dir> JWT_SECRET=<32+> SECRET_KEY=<32+> \
  ADMIN_BOOTSTRAP_USER=seedowner ADMIN_BOOTSTRAP_PASSWORD=seed-password-123 tsx --conditions=source src/index.ts
RANKS_SEED_SECRET=<dir>/totp.txt node scripts/ranks-seed.mjs http://127.0.0.1:3471 > seed.json
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=RanksGameTest -Dvibeyclient.ranksSeed=seed.json \
  -Dvibeyclient.devAuth=true -Dvibeyclient.api=http://127.0.0.1:3471 -Dvibeyclient.username=VibeyTester \
  -Dvibeyclient.test.relay=off
```

The seed logs in four bots, grants ranks through the admin API (TOTP set up by the script), picks their styles and
befriends them with `VibeyTester`. Screenshots: `versions/fabric-1.21.11/build/run/clientGameTest/screenshots/*ranks_*`
(the run dir is wiped by the next run: copy them away).
