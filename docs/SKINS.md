# Skins: change your skin in game (feature #34)

Module `skins` (Fabric 1.21.11 only for now, wiring like `pvp`/`clips`). It shows the Vibe Launcher's skin library
in game and switches the skin right away, without a restart. The Mojang skin upload stays in the launcher (it holds
the account token); the client only asks for it and refreshes its own player texture.

## Using it

- Vibey menu → *Skins* → *Skin library: Open…*, or the unbound key *Open skin library*.
- Click a skin: the launcher sets it on the Minecraft account, the client shows it at once (body, tab list,
  inventory, third person) with the right model (classic / slim; old 64×32 skins are converted like vanilla does).
- Other players see the new skin after you rejoin the server: servers cache the signed profile textures at login.
  The toast says so. Cape and elytra are kept.
- Without the Vibe Launcher (no `<gameDir>/.vibe/`), with an offline account, with a launcher sign-in that ran out,
  or with a skin list of another account, the screen says what is wrong and how to fix it; nothing is sent.

## Files (contract with the launcher)

All in `<instance>/.vibe/` (the folder of `instance.json` / `session.json`, vibe-launcher `docs/PLAY-LINKS.md` §2):

| File | Writer | Content |
|---|---|---|
| `skins.json` | launcher | schema 1: `account` (uuid, name, kind), `can_change` + `reason`, `active` (`library_id` or `sha1`, `variant`), `skins[]` (`id`, `name`, `variant`, `file`, `sha1`, `added_at`) |
| `skins/<file>.png` | launcher | copies of the library PNGs (≤ 512 KB, 64×64 or 64×32); the client never reads the launcher's data folder |
| `skin-request.json` | client | `{"schema":1,"id":"<uuid>","action":"apply","skin_id":"…","variant":"slim","requested_at":<s>}` |
| `skin-result.json` | launcher | `{"schema":1,"id":"<same>","status":"working|ok|error","skin_id","variant","error","message"}` |

Full contract with error codes, timing and the launcher's steps: vibe-launcher contract `14-34-client-launcher.md`
part B. Writes are temp file + rename on both sides; unknown fields are ignored. Entries with an id or file name
outside `[A-Za-z0-9_-]` (or a path) are dropped.

**Timing:** the client reads `skins.json` when the screen opens and every 2 s while it is open (only on a new
mtime). After a click it checks `skin-result.json` every 5 ticks. No `working` within 8 s → "The launcher doesn't
answer" and the request is withdrawn (deleted); `working` without a result for 45 s → timeout. At game start the
client deletes a leftover request and result.

## Code

| Part | Job |
|---|---|
| `skins/core` (pure Java, JUnit `SkinBridgeTest`) | `SkinLibrary` (parse + checks), `SkinRequest`, `SkinResult`, `BridgeFiles` (paths, atomic write, size caps) |
| `SkinSwap` | availability (launcher / list / account / can change), request → poll → result state machine on one I/O thread, toasts |
| `LocalSkin` + `mixin/PlayerInfoLocalSkinMixin` | after `ok`: uploads the PNG as a dynamic texture and returns a patched `PlayerSkin` from `PlayerInfo.getSkin` for the own UUID only (cached per original skin). Streamer mode's "hide skins" still wins (it cancels at HEAD). |
| `SkinImages` + `mixin/SkinTextureDownloaderInvoker` | PNG → `NativeImage` → vanilla `processLegacySkin` (64×32 conversion, alpha rules) → `DynamicTexture` |
| `ui/SkinLibraryScreen`, `SkinFigure`, `SkinPreviews` | grid of flat front previews (head, body, arms, legs with overlays), states, footer status; previews load lazily and are released on close |

Test: `RewindSkinsGameTest` (with a simulated launcher): library screen (de/en), request file, `working` → `ok`
→ own skin is the local texture with the slim model, third-person shot, `rate_limited` error, no answer (request
withdrawn), other account, offline account. Screenshots: `versions/fabric-1.21.11/build/run/clientGameTest/screenshots/`.
