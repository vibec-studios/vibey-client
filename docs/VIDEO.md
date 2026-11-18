# Video screens (Fabric 1.21.11)

Screens placed in the world – and a picture-in-picture overlay on the HUD – that play YouTube (official embedded
player), web pages, local video/music files and direct streams (mp4/webm/HLS/…). Screens with a public link can be
shared with other Vibey Client players on the same server. Research and licences: [VIDEO-RESEARCH.md](VIDEO-RESEARCH.md).

> See also: [MODULES.md](MODULES.md) · [SPEC.md](SPEC.md) §6 (server channel) · [UI.md](UI.md)

Only in the **Fabric 1.21.11** jar for now: `gradle/common.gradle` excludes `module/media/video/**` and
`i18n/*/video.json` from every other target, and `BuiltinModules` registers the module by reflection
(`registerIfPresent`).

## Using it

- **Screens key** (default **N**, "Video screens" in Controls): look at a screen → its controls; look anywhere else →
  *New screen*: paste a link / path or pick a file, choose the shape (16:9, 4:3, 1:1, 21:9, 9:16), then
  *Place in world* or *Overlay only*.
- **Placing** (no item needed): the screen sticks to the block face under the crosshair (wall, floor, ceiling),
  snapped to 1/8 block. Mouse wheel = size (Shift = fine), **R** turns floor/ceiling screens, right-click places,
  left-click (or the screens key) cancels. *Move* in the controls picks a placed screen up again.
- **Controls**: play/pause, seek bar (or ±10 s for streams), per-screen volume, show as overlay, width, glowing
  (full bright) or lit by the world, repeat, share with others, change source, move, remove, *Use page* (mouse and
  keyboard to the web page, e.g. to press a page's own buttons).
- **Settings** (module "Video screens"): screens key, *Other players' screens* (Ask first / Always load / Hide),
  volume, overlay position; under Advanced: max resolution (360p–1080p, default 720p), max frame rate (default 30),
  play distance (default 48 m), remove all my screens here.

## What plays where

| Source | Player | Needs |
|---|---|---|
| YouTube link (`watch`, `youtu.be`, `shorts`, `live`, `embed`) | official embed `youtube.com/embed/<id>` in Chromium | MCEF Modern (optional mod) |
| other http(s) page | Chromium | MCEF Modern |
| local file, `file:` URL, direct media URL (`.mp4 .webm .mkv .m3u8 …`), rtmp/rtsp/srt | FFmpeg process | FFmpeg |

If something is missing the screen says so and the screens key opens a **setup prompt** that names what will be
downloaded, the size, the source and the licence, and does nothing until the user presses *Download*:

- **FFmpeg** (Windows x64/arm64): newest `ffmpeg-n*-latest-win64-lgpl-shared` from github.com/BtbN/FFmpeg-Builds
  (~80 MB), checked against the release's `checksums.sha256`, unpacked to `<gameDir>/vibeyclient/ffmpeg/bin`
  (with `LICENSE.txt` and `SOURCE.txt`). Linux/macOS: the prompt explains the package manager / Homebrew; an
  `ffmpeg` on the PATH (or in /usr/bin, /usr/local/bin, /opt/homebrew/bin) is used as is.
- **MCEF Modern** (mod, LGPL-2.1): the release for this Minecraft version from the Modrinth API, checked with
  Modrinth's SHA-512, saved to `mods/` – restart needed. On first use MCEF downloads Chromium (~160 MB, Maven
  Central `me.friwi:jcef-natives-*`) into `config/mcef-modern/`. If MCEF was installed by hand, a second prompt asks
  before that Chromium download (stored as `_data.browserConsent`).

## How it works

- `Video` (the `VideoFeature`) owns all screens (`MediaScreen`: geometry, `MediaSource`, a **timeline** =
  playing + position at a wall-clock time). Each tick it gives the nearest ≤ 4 allowed screens in range a
  `MediaPlayer`, keeps it on the timeline (play/pause, seek when it drifts > 2 s), and sets the volume
  (`volume × screen volume × MASTER × RECORDS × distance falloff`). Out of range → paused, closed after 15 s.
- `FfmpegPlayer`: two FFmpeg processes per run. Video: `-vf fps=N,scale=W:H…,pad,format=rgba -f rawvideo` to stdout →
  3 preallocated direct buffers → `CommandEncoder.writeToTexture(GpuTexture, ByteBuffer, …)` once per new frame, only
  while the screen is in the frustum (no per-frame allocation). Audio: `-ac 1 -ar 48000 -f s16le` → streaming OpenAL
  source at the screen (positional; our own gain, rolloff 0); the audio clock drives the video, a wall clock when
  there is no audio. Seek = restart both processes with `-ss`. Processes are killed on close and on JVM exit.
- `WebPlayer` (only loaded with MCEF): `MCEFApi.createBrowser`, `loadRequest` with the Referer
  `https://dev.henny.vibeyclient/` (YouTube's API-client identity rule for WebView apps), control through the
  player's own API via `executeJavaScript` (`movie_player.playVideo/pauseVideo/seekTo/setVolume`, `<video>` for other
  pages), position reports through a JCEF message router (`window.vibeyVideoQuery`). Chromium plays sound itself:
  not positional, the distance lowers the page volume. MCEF's `GpuTextureView` is wrapped in
  `FrameTexture.External` so it draws like any texture.
- `ScreenRenderer` (Fabric `WorldRenderEvents.BEFORE_ENTITIES`, submit API): dark bezel/back plate
  (`textBackground`), the picture (`RenderTypes.text(texture)`: lightmapped, no shading; glowing = full bright),
  or a status line. Texture uploads happen in `END_EXTRACTION` (no render pass open).
- `Placement`: crosshair block hit → face/snap; wheel via the hotbar-slot delta (restored), clicks swallowed at
  `START_CLIENT_TICK` before vanilla uses them. No mixins.

## Sharing (server channel)

Channel `video:screen` on the `vibeyclient:msg` envelope (works when the server runs Vibey Client, incl. hosted
worlds). Messages: `UPSERT` (id, dimension, position, normal/up, size, kind, http(s) URL ≤ 1024, glowing, repeat,
playing, position ms), `STATE` (playing, position ms), `REMOVE`, `REQUEST` (newcomer asks; owners answer with a
random 0.3–1 s delay). Owners resend `STATE` every 10 s while playing; receivers set their timeline from it.

- **Never shared:** media bytes, local files/paths (`MediaSource.shareable()` = http/https only), screens with
  *Share* off, overlay-only screens.
- **Receiving:** `UrlPolicy.acceptRemote` (http/https only, no user:pass@, no localhost/private/link-local/CGNAT
  IPs or `.local/.lan/.internal` names – another player can't make us probe our LAN), max 8 screens per player,
  token bucket ~3 messages/s (burst 6) on top of the relay's limits, owner checked on every update/remove, screens of
  players who left are dropped.
- **Default "Ask first":** another player's screen shows "Alex shared youtube.com – press the screens key to
  load". Nothing is fetched (no request to that host, no sound) until the user presses *Load* for that screen
  (per session). *Always load* / *Hide* are the other choices. Rationale: loading a URL someone else picked reveals
  your IP to that host and can play sound – an explicit opt-in is the safe default; friends can switch to *Always*.

## Wiring into the Media module (for the integrator)

The feature is self-contained behind `VideoFeature` + `HudElement`; `VideoModule` is only a shim.

1. **Settings:** construct `new VideoSettings(hostId, settings(), this::isEnabled)` in a field initialiser of the
   Media module (keybinds can't be created later), then `new Video(cfg)`. Setting labels resolve as
   `<hostId>.setting.<key>` – move the `setting.*` lines of `i18n/*/video.json` into `media.json` (or keep
   them and prefix), and rename the keybind lang key `key.vibeyclient.video.screens` → `key.vibeyclient.<hostId>.screens`.
   All other strings use `Video.tr` = `video.*` and can stay in `video.json`.
2. **Lifecycle:** forward `onLoad/onEnable/onDisable/tick/renderHud/onWorldJoin/onWorldLeave` to the `VideoFeature`
   (exactly what `VideoModule` does), e.g. as a `MediaFeature` adapter.
3. **HUD editor:** take `VideoFeature.hudElements()` (one element, `video.overlay`): position = fractions of the
   free screen space (`x()*(screenW - w*scale)`), `setLayout(x, y, scale)` persists. `OverlayEditScreen` is a
   stand-in editor you can drop once the generic one exists; while a HUD editor is open the element should be drawn
   by the editor (`render(..., editing=true)`), `Video.renderHud` skips it only for `OverlayEditScreen` – add the
   generic editor screen class to that check.
4. **Registration:** remove `registerIfPresent(...VideoModule)` from `BuiltinModules` and delete `VideoModule`; the
   Media module can stay on all targets but must only touch the video classes on Fabric 1.21.11 (reflection or
   `//? if` guard), because the package is excluded elsewhere.
5. **Data:** own screens are stored per world (`sp:<level name>` / `mp:<server address>`) in the host's
   `settings().data()` under `screens`, the overlay layout under `overlay`, consent under `browserConsent`. When the
   host id changes from `video` to `media`, copy `config/vibeyclient/video.json` `_data` over once (or accept a
   fresh start – it is new in this round).

## Dev testing

- `-Dvibeyclient.mcef=true` adds MCEF Modern to `runClient` (else test the download prompt).
- Test media: generate your own, e.g.
  `ffmpeg -f lavfi -i testsrc2=size=1280x720:rate=30:duration=20 -f lavfi -i sine=frequency=440:beep_factor=4:duration=20 -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest testpattern.mp4`.
