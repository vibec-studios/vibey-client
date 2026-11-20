# Video screens (Fabric 1.21.11)

Screens placed in the world – and a picture-in-picture overlay on the HUD – that play YouTube (official embedded
player), web pages, local video/music files and direct streams (mp4/webm/HLS/…). Screens with a public link can be
shared with other Vibey Client players on the same server. Research and licences: [VIDEO-RESEARCH.md](VIDEO-RESEARCH.md).

> See also: [MODULES.md](MODULES.md) · [SPEC.md](SPEC.md) §6 (server channel) · [UI.md](UI.md)

Placed screens are **r3** world objects (`libs/r3`): every gesture of the r3 interaction model works on them.
Part of the **Media** module as the feature `video` (`VideoFeature extends MediaFeature`, see [MEDIA.md](MEDIA.md)):
own on/off switch on Media's settings page, settings stored in `media.json` as `video.*`, strings in
`i18n/*/media.video.json`, data under `_data.video`. Only in the **Fabric 1.21.11** jar for now:
`gradle/modules.gradle` excludes `module/media/video/**` and `i18n/*/media.video.json` from every other target, and
`MediaFeatures` adds the feature by reflection (`addIfPresent`).

## What's new (r3 screens)

- Screens are **r3 world objects**: look at one and its control strip slides in (▶/❚❚, ■, ⏮ −10 s, ⏭ +10 s,
  seek bar, volume, ✎ link, ○/⚿ lock, ✕ remove, ⋯ more). No more controls window.
- **Hold the middle mouse** on a screen to move it (sticks to walls, floors, ceilings – or floats in the air),
  **wheel** to make it bigger/smaller, **look at its border** and drag (middle mouse) or wheel to scale one edge,
  **right click** for the menu (glowing, repeat, share, HUD overlay, use page, shape), **Ctrl+Z** undoes a move.
- **Placing** a new screen: left click places, right click / Esc cancels (was: right click places).
- The link is typed **into the screen**: ✎ (or a fresh screen) focuses the field, keys go there until Enter or Esc.
- Lock (○/⚿) stops accidental grabs; the buttons still work.
- Other players' screens: the strip shows **Load** instead of a separate window; **Set up** opens the download
  prompt when FFmpeg or the browser is missing.
- Sharing uses a new channel (`video:screen.2`, JSON) – screens are not exchanged with clients before this change.

## Using it

- **Screens key** (default **N**): look at a screen → its menu (⋯). Look anywhere else → a new empty screen
  follows the crosshair; left click places it and the link field opens (paste with Ctrl+V, Enter applies, … picks
  a file). An overlay-only screen comes back into the world with the same key.
- **Gestures** (r3 interaction model, see `libs/r3/docs/interaction.md`): look + left click = button, middle mouse
  hold = move, wheel = size (Shift while holding = distance), border = edge scale, right click = menu, Esc = cancel,
  Ctrl+Z = undo. Floor/ceiling screens turn with **R** while holding.
- **Strip** (shown while you look at the screen, 1.5 s after looking away, always while a menu or the link field is
  open): play/pause, stop, −10 s / +10 s, seek bar (files with a known length), volume, link, lock, remove, more.
- **More** (right click or ⋯): glowing (full bright) or lit by the world, repeat, share with others, HUD overlay,
  use page (MCEF pages), shape 16:9 · 4:3 · 1:1 · 21:9 · 9:16.
- **Settings** (Vibey menu → Media → *Video screens*): the switch, *Screens* (same as the key), the screens key,
  *Other players' screens* (Ask first / Always load / Hide), volume; under Advanced: max resolution (360p–1080p,
  default 720p), max frame rate (default 30), play distance (default 48 m), remove all my screens here.
- **Overlay:** *HUD overlay* in the menu; move and scale it in the HUD editor (element "Video overlay", id
  `media.video.overlay`). **Overlay only:** ✕ on a screen that is on the HUD takes it out of the world and keeps it
  as the overlay (not saved, like before); the screens key puts it back into the world; turning the overlay off
  removes it.

## What plays where

| Source | Player | Needs |
|---|---|---|
| YouTube link (`watch`, `youtu.be`, `shorts`, `live`, `embed`) | official embed `youtube.com/embed/<id>` in Chromium | MCEF Modern (optional mod) |
| other http(s) page | Chromium | MCEF Modern |
| local file, `file:` URL, direct media URL (`.mp4 .webm .mkv .m3u8 …`), rtmp/rtsp/srt | FFmpeg process | FFmpeg |

If something is missing the screen says so and the screens key opens a **setup prompt** that names what will be
downloaded, the size, the source and the licence, and does nothing until the user presses *Download*:

- **FFmpeg** (Windows x64/arm64): newest `ffmpeg-n*-latest-win64-lgpl-shared` from github.com/BtbN/FFmpeg-Builds
  (~75–80 MB), checked against the release's `checksums.sha256`, unpacked to `<gameDir>/vibeyclient/ffmpeg/bin`
  (with `LICENSE.txt` and `SOURCE.txt`). Linux/macOS: the prompt explains the package manager / Homebrew; an
  `ffmpeg` on the PATH (or in /usr/bin, /usr/local/bin, /opt/homebrew/bin) is used as is.
- **MCEF Modern** (mod, LGPL-2.1): the release for this Minecraft version from the Modrinth API, checked with
  Modrinth's SHA-512, saved to `mods/` – restart needed. On first use MCEF downloads Chromium (~160 MB, Maven
  Central `me.friwi:jcef-natives-*`) into `config/mcef-modern/`. If MCEF was installed by hand, a second prompt asks
  before that Chromium download (stored as `_data.browserConsent`).

## How it works

- `Video` (driven by `VideoFeature`) is the hub: screen registry, lifecycle (r3 world join/leave), tick. The work is
  split into `play/Players` (which screens get a player: ≤ 6 allowed screens in range, overlay first, then playing,
  then nearest; out of range → paused, closed after 15 s; texture uploads once per frame in `END_EXTRACTION`),
  `play/Follower` (timeline following: owner's real playback is the truth, re-based when it differs by > 0.75 s;
  other players' screens seek when they drift > 2 s files / 3 s streams, aiming ahead by the measured seek lag;
  volume = setting × screen × MASTER × RECORDS × distance falloff), `ScreenStatus` (status line, what is missing),
  `ScreenActions` (everything a button or key does), `ScreenStore` (persistence) and `net/VideoSync` (sharing).
- `MediaScreen` is the model (source, timeline, flags) plus its r3 `WorldObject` (`object`, null for an overlay-only
  screen). Geometry is the object's transform: position, quaternion, size (blocks) and scale (wheel).
- `world/ScreenObjects` builds the object: kind `vibeyclient:video`, an r3 `VideoSurface` whose texture is the
  player's picture (`PlayerTexture`: FFmpeg `FrameTexture` or MCEF `ExternalTexture`, by texture id) and whose
  **overlay** is the vui document `vibeyclient:video/screen` (`controls/ScreenControls` controller). The strip is
  laid out at 256 px width for every screen size (`pixelsPerBlock = 256 / width`), so it keeps its proportions.
  Rules: `InteractionRules.screen()` (keep aspect), width 0.5–16 m. `world/ScreenSurfaces` updates picture, status
  line, glow and the strip state 20×/s; `world/ScreenEvents` reacts to r3 events (placed, moved, resized, undo,
  cancelled, context menu).
- r3 draws the bezel/back plate, the picture (lightmapped `text` render type, glowing = full bright, else the light
  0.3 in front), the status line when there is no picture, and the strip a few millimetres in front.
- `FfmpegPlayer`/`WebPlayer`: unchanged, see "Shared FFmpeg and player code" and `web/`.

## Saving

Own screens are r3 placements (`config/r3/worlds/<world>-<hash>.json`, kind `vibeyclient:video`; feature data:
`kind`, `uri`, `glow`, `loop`, `vol`, `shared`, `pos`). They come back paused. Screens saved by older versions under
`_data.video.screens.<world>` in `media.json` are moved there on the first join of that world (`LegacyScreens`:
face/up directions become a quaternion + block-face anchor).

## Sharing (server channel)

Channel **`video:screen.2`** on the `vibeyclient:msg` envelope (needs Vibey Client on the server, incl. hosted
worlds). The payload is one r3 sync message as JSON (`SyncCodec`, ≤ 8192 chars), carried by `net/JsonChannel`
(an r3 `SyncChannel`), driven by r3 `SharedObjects` (kind `vibeyclient:video`):

| op | content |
|---|---|
| `UPSERT` | placement record (id, dimension, position, quaternion, size, scale, glowing, anchor) + data `kind`, `uri` (http/https only), `glow`, `loop`, `playing`, `pos` |
| `STATE` | `{"playing": bool, "pos": ms}` – on play/pause/seek, re-based drift, and every 10 s while playing |
| `REMOVE` | id |
| `REQUEST` | newcomer asks; owners answer after a random 0.3–1 s |

- **Protocol change:** the old binary channel `video:screen` (byte op + FriendlyByteBuf fields, only block-face
  directions) is gone; it could not describe free-floating or scaled screens. Old and new clients don't see each
  other's screens; nothing breaks (unknown channels are ignored).
- **Conflict rule** (r3): the owner is the authority, revisions only go up (`RevisionClock`), whole state wins,
  other players' screens arrive `remote` (locked for grab/resize; buttons still work locally).
- **Never shared:** media bytes, local files/paths (`MediaSource.shareable()` = http/https only), screens with
  *Share* off, overlay-only screens.
- **Receiving:** `UrlPolicy.acceptRemote` (http/https only, no user:pass@, no localhost/private/link-local/CGNAT
  IPs or `.local/.lan/.internal` names), *Hide* drops everything, max 8 screens per player, token bucket burst 6 /
  3 per second (`SyncPolicy`), owner checked on every update/remove, players who left are forgotten
  (`forgetOwner`, checked every 5 s).
- **Default "Ask first":** another player's screen shows "Alex shared youtube.com – look here: Load". Nothing is
  fetched until **Load** on its strip (per session). *Always load* / *Hide* are the other choices.

## Code map

| Class | Role |
|---|---|
| `VideoFeature` | the Media feature: settings, keybind, HUD element `overlay` |
| `Video` | hub: screens, lifecycle, tick |
| `MediaScreen`, `Timeline`, `ScreenData` | model, shared playback timeline, feature data JSON |
| `MediaSource`, `UrlPolicy` | link parsing (YouTube/web/stream/file), what we accept from others |
| `ScreenActions`, `ScreenStatus`, `ScreenStore`, `LegacyScreens`, `OverlayView` | buttons/keys, status line, r3 placements, old-save migration, HUD overlay |
| `play/Players`, `play/Follower`, `play/PixelSize` | player selection, timeline following + volume, decode size |
| `world/ScreenObjects`, `world/ScreenSurfaces`, `world/ScreenEvents`, `world/PlayerTexture` | r3 object + strip, per-tick surface state, r3 events, player texture handle |
| `controls/ScreenControls`, `controls/ScreenState`, `controls/Shapes`, `controls/FilePicker` | strip controller (`assets/vibeyclient/vui/video/screen.vui`) |
| `net/VideoSync`, `net/JsonChannel`, `net/RemoteScreens` | sharing over `video:screen.2` |
| `media/player/*`, `media/ffmpeg/*` (shared with Clips) | FFmpeg playback, locating/downloading FFmpeg, GPU texture |
| `web/*` | MCEF Modern (loaded only when installed) |
| `ui/SetupScreen`, `ui/SetupScreens`, `ui/SetupNeed` | download prompts (still screens: they ask before downloading) |
| `VideoDev`, `VideoDevPlace` | DevDriver `video …` commands (dev only) |

Porting to more versions: rendering and input are r3's (all targets); left are `media/player/FrameTexture` (blaze3d
upload), the `END_EXTRACTION` hook in `Video.onLoad` and MCEF Modern's per-version API; then drop the exclusion in
`gradle/modules.gradle`.

## Dev testing

- `MediaR3GameTest` (Fabric 1.21.11, `-Dvibeyclient.test.only=MediaR3GameTest`): places a screen with real input,
  clicks ▶, grabs and moves it to another wall, wheel, edge scale, lock, types a link into the field (W does not walk,
  Esc releases), right-click menu + HUD overlay, then the music panel and sky lyrics. Screenshots and a frame per tick
  go to `build/review/media-r3/` (`frames/` → `demo.mp4` with ffmpeg). Test videos are made with the FFmpeg on the
  PATH (`testsrc2`/`mandelbrot` + a sine tone).
- `-Dvibeyclient.mcef=true` adds MCEF Modern to `runClient`.
- DevDriver (`-Dvibeyclient.devDriver=<port>`) `video …`: `list`, `place x y z normal width aspect source`,
  `placing <source>`, `key`, `play|pause|seek|overlay|accept|remove|lock <index>`, `others ask|always|never`,
  `setup ffmpeg|mod|natives`, `setupgo`, `hideffmpeg on|off`, `edit`, `screen`.
- Windows: once Chromium runs, start `runClient` in its **own console window** (a `.cmd` via `Start-Process`).

## Shared FFmpeg and player code

The video screens and the Clips module both play files with FFmpeg. Modules must not use each other, so the shared
parts live outside `module/`, in `dev.henny.vibeyclient.media` (client core, Fabric 1.21.11 only like both modules;
`sharedPackages` in `gradle/modules.gradle` leaves the package and `i18n/*/media.ffmpeg.json` out of the other
targets, and `checkModuleBoundaries` fails if it imports a module).

`media/ffmpeg` (finding and installing the program):

- `Ffmpeg`: `find()` = our own download (`<gameDir>/vibeyclient/ffmpeg/bin`) first, then the `PATH`, on Linux/macOS also
  the usual system directories; cached until `refresh()`. `downloadable()` = Windows x64/arm64 only, elsewhere the
  setup screen explains the system package. `hiddenForTest` ignores everything but our own download (dev test of the
  prompt, `video hideffmpeg on`, `ClipsGameTest`).
- `FfmpegRelease`: `lookup()` asks the GitHub release API for the newest BtbN `ffmpeg-nX.Y-latest-<win64|winarm64>-lgpl-shared`
  asset and its sha256. The API is rate limited per IP (403/429, e.g. shared networks), so it falls back to
  `releases/latest/download/checksums.sha256` (`pickFromChecksums`, pure and covered by `FfmpegReleaseTest`) and takes
  the size from a `HEAD` request (only shown in the prompt). Never a GPL build.
- `FfmpegInstaller`: downloads to `download.zip.part`, verifies the sha256, unpacks `bin/*` into `bin.new`, swaps it
  for `bin`, writes `SOURCE.txt` / `LICENSE.txt`. FFmpeg always runs as a separate process, nothing is linked.
- `FfmpegSetupScreen(parent, FfmpegPurpose.PLAYBACK|RECORDING)`: asks before anything is downloaded (what, size, source,
  licence, target folder); the button starts lookup → download → done. Strings in `media.ffmpeg.json`
  (`playback.*` for video screens, `recording.*` for Clips). The video module's `SetupScreens.of(parent, SetupNeed)`
  picks this screen for `FFMPEG` and its own `SetupScreen` for the browser downloads.

`media/player` (playing a file):

- `MediaPlayer`: created and used on the client thread, decoding happens elsewhere. `frame(visible)` is called once per
  rendered frame before anything is drawn and uploads the newest due frame (skip uploads when not visible).
  `durationMs()` is -1 when unknown (live streams, web pages), `error()` is the user-facing reason for `ERROR`,
  `texture()` is null until something was decoded, `width()/height()` is the size the picture was letter-boxed to.
  `audio(gain, pos)`: gain 0..1 with all volume settings and the distance applied, `pos == null` = "in your head"
  (overlay). `audioOnly()` (e.g. an mp3), `interactive()` (a web page that wants input), `seekable()` (position can be
  trusted for drift correction).
- `FfmpegPlayer(ffmpeg, uri, w, h, fps, startMs, playing)`: one `FfmpegSession` per run (a seek starts a new one and
  keeps the known duration). `FfmpegCommand` builds the two command lines, `FfmpegProcesses` starts them (all live
  processes are killed on JVM exit). `FfmpegVideoLoop` reads raw RGBA frames into the session's 3 direct buffers,
  `FfmpegAudioLoop` feeds the streaming OpenAL source (8 × 50 ms buffers) from the PCM pipe and derives the audio clock
  from the source's sample offset, `FfmpegLogLoop` reads the duration and the last error line from FFmpeg's stderr.
  The audio clock drives the video; if no audio arrives within 2.5 s (or there is none) a wall clock does.
  `SoundTarget` is the gain/position shared with the audio thread.
- `FrameTexture`: a GPU texture we write raw RGBA frames into straight from a direct buffer (no `NativeImage` copy),
  render thread, outside a render pass. `ExternalTexture`: a texture whose GPU objects belong to someone else (the
  browser), we only point at them.
