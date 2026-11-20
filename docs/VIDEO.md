# Video screens (Fabric 1.21.11)

Screens placed in the world – and a picture-in-picture overlay on the HUD – that play YouTube (official embedded
player), web pages, local video/music files and direct streams (mp4/webm/HLS/…). Screens with a public link can be
shared with other Vibey Client players on the same server. Research and licences: [VIDEO-RESEARCH.md](VIDEO-RESEARCH.md).

> See also: [MODULES.md](MODULES.md) · [SPEC.md](SPEC.md) §6 (server channel) · [UI.md](UI.md)

Part of the **Media** module as the feature `video` (`VideoFeature extends MediaFeature`, see [MEDIA.md](MEDIA.md)):
own on/off switch on Media's settings page, settings stored in `media.json` as `video.*`, strings in
`i18n/*/media.video.json`, data under `_data.video`. Only in the **Fabric 1.21.11** jar for now:
`gradle/modules.gradle` excludes `module/media/video/**` and `i18n/*/media.video.json` from every other target, and
`MediaFeatures` adds the feature by reflection (`addIfPresent`).

## Using it

- **Screens key** (default **N**, "Video screens" in Controls; or *Screens → Open* on Media's settings page): look at a screen → its controls; look anywhere else →
  *New screen*: paste a link / path or pick a file, choose the shape (16:9, 4:3, 1:1, 21:9, 9:16), then
  *Place in world* or *Overlay only*.
- **Placing** (no item needed): the screen sticks to the block face under the crosshair (wall, floor, ceiling),
  snapped to 1/8 block. Mouse wheel = size (Shift = fine), **R** turns floor/ceiling screens, right-click places,
  left-click (or the screens key) cancels. *Move* in the controls picks a placed screen up again.
- **Controls**: play/pause, seek bar (or ±10 s for streams), per-screen volume, show as overlay, width, glowing
  (full bright) or lit by the world, repeat, share with others, change source, move, remove, *Use page* (mouse and
  keyboard to the web page, e.g. to press a page's own buttons).
- **Settings** (Vibey menu → Media → *Video screens*): the switch, *Screens* (open), the screens key, *Other
  players' screens* (Ask first / Always load / Hide), volume; under Advanced: max resolution (360p–1080p, default
  720p), max frame rate (default 30), play distance (default 48 m), remove all my screens here.
- **Overlay:** *Show as overlay* in a screen's controls; move and scale it in the HUD editor (Media → *HUD layout →
  Edit*, element "Video overlay", id `media.video.overlay`, default top right; it is only listed there while a screen
  is shown as the overlay).

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

- `Video` (driven by `VideoFeature`) owns all screens (`MediaScreen`: geometry, `MediaSource`, a **timeline** =
  playing + position at a wall-clock time). Each tick it gives ≤ 6 allowed screens in range a `MediaPlayer`
  (overlay first, then playing screens, then the nearest), and sets the volume
  (`volume × screen volume × MASTER × RECORDS × distance falloff`). Out of range → paused, closed after 15 s.
  Own screens: the owner's real playback is the truth – the timeline is re-based on it when it differs by > 0.75 s
  (buffering) and the new state is sent. Other players' screens follow the timeline: seek when they drift > 2 s
  (files) / 3 s (streams, web), aiming ahead by the measured start-up lag of the previous seek. Saved screens come
  back paused after a rejoin (no surprise sound).
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
  `ExternalTexture` so it draws like any texture.
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

## Code map

| Class | Role |
|---|---|
| `VideoFeature` | the Media feature: settings, keybind, HUD element `overlay`, forwards the lifecycle to `Video` |
| `Video` | screens, players, timeline following, volume, persistence (`_data.video.screens.<world>`), overlay drawing |
| `MediaScreen`, `MediaSource`, `UrlPolicy` | model, link parsing (YouTube/web/stream/file), what we accept from others |
| `Placement`, `render/ScreenRenderer` | item-free placing, world rendering |
| `VideoNet` | `video:screen` channel (sharing) |
| `media/player/*`, `media/ffmpeg/*` (shared with Clips) | FFmpeg playback, locating/downloading FFmpeg, GPU texture, see below |
| `web/Web` (facade), `web/McefBridge`, `web/WebPlayer`, `web/BrowserScreen`, `web/McefInstaller` | MCEF Modern (loaded only when installed) |
| `ui/SourceScreen`, `ui/ControlsScreen`, `ui/SetupScreen` (browser downloads), `ui/SetupScreens` (`SetupNeed` → screen), `ui/AcceptScreen` | screens |
| `VideoDev` | DevDriver `video …` commands (dev only) |

Porting to more versions: the version-specific parts are `render/ScreenRenderer` (submit API), `media/player/FrameTexture`
(blaze3d `GpuTexture` upload), the Fabric world render events in `Video.onLoad`, `Placement` (hotbar slot API) and
MCEF Modern's per-version API; then drop the exclusion in `gradle/modules.gradle` (`media.video` in `clientModules`, `media` in `sharedPackages`).

## Dev testing

- `-Dvibeyclient.mcef=true` adds MCEF Modern to `runClient` (else test the download prompt; a jar the prompt put
  into `run*/mods/` is loaded in dev as well – Fabric remaps it at runtime).
- DevDriver (`-Dvibeyclient.devDriver=<port>`) gets `video …` commands (`VideoDev`): `list`, `place x y z normal
  width aspect source`, `placing <source>`, `click use|attack`, `wheel <n>`, `placement`, `play|pause|seek|open|
  controls|overlay|accept|remove <index>`, `others ask|always|never`, `setup ffmpeg|mod|natives`, `setupgo`,
  `hideffmpeg on|off` (ignore the system FFmpeg to test the download prompt), `edit`.
- Windows: once Chromium runs, start `runClient` in its **own console window** (e.g. `Start-Process` a .cmd).
  Sharing a console with the Gradle client got the client killed (daemon log: "client disconnection detected,
  canceling the build"), which ends the game.
- Test media: generate your own, e.g.
  `ffmpeg -f lavfi -i testsrc2=size=1280x720:rate=30:duration=20 -f lavfi -i sine=frequency=440:beep_factor=4:duration=20 -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest testpattern.mp4`.

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
