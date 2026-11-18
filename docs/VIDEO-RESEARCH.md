# In-world video: research (2026-11-18)

Goal: media screens placed in the world (and a 2D overlay) that play YouTube, web pages, local files and direct
streams, optionally shared with other Vibey Client players. Target for the first round: Fabric 1.21.11.

> See also: [VIDEO.md](VIDEO.md) (what was built, how to use and wire it) · [MODULES.md](MODULES.md) · [SPEC.md](SPEC.md)

## 1. Web pages and YouTube: an embedded Chromium

Minecraft has no HTML engine, and YouTube may only be shown through its official player (section 3), so web content
needs a real browser. The established way is **MCEF** (Minecraft Chromium Embedded Framework): a mod that wraps
[java-cef](https://github.com/chromiumembedded/java-cef) (JCEF, BSD-3) in off-screen rendering mode and hands the
frames to Minecraft as a GPU texture. It was started by montoyo (WebDisplays), rewritten by the CinemaMod group
(used by CinemaMod / WebDisplays) and is now maintained in forks.

| Fork | MC / loader | Licence | Chromium | Notes |
|---|---|---|---|---|
| [CinemaMod/mcef](https://github.com/CinemaMod/mcef) ([Modrinth](https://modrinth.com/mod/mcef)) | up to 1.21.4, Fabric/NeoForge | LGPL-2.1 | 116 | natives from `mcef-download.cinemamod.com`, last update ~2 years ago |
| **[MCEF Modern](https://modrinth.com/mod/mcef-modern)** ([source](https://github.com/DimasKama/mcef-modern)) | **1.21.10–26.3, Fabric** | LGPL-2.1-only | **146** (JCEF 146.0.10) | 0.3.3 for 1.21.11 (2026-06-21), documented API, async init |
| [Keksuccino fork](https://modrinth.com/mod/mcef-keksuccino/version/2.1.8-1.21.11-fabric) | 1.21.11 Fabric | LGPL-2.1+ | 116-based | API of CinemaMod MCEF (`com.cinemamod.mcef`) |

**Chosen: MCEF Modern 0.3.3+mc1.21.11** (mod id `mcef-modern`, `net.dimaskama.mcef.api.MCEFApi` / `MCEFBrowser`):

- The jar is small (248 KB, `https://cdn.modrinth.com/data/YJFfu5pH/versions/NfheQM6w/…jar`, Modrinth maven
  `maven.modrinth:mcef-modern:0.3.3+mc1.21.11.jcef146.0.10`). On first use it downloads the CEF natives through
  [jcefmaven](https://github.com/jcefmaven/jcefmaven) from **Maven Central** (`me.friwi:jcef-natives-<platform>`):
  **~155 MB for Windows x64, ~141 MB for Linux x64**, macOS (Intel/Apple Silicon) and Linux arm64 are built too.
  They are unpacked to `config/mcef-modern/jcef/` (cache in `config/mcef-modern/cache/`).
- API: `MCEFApi.initialize()` (stage + percentage for a progress bar), `createBrowser(url, transparent)`,
  `resize`, mouse/key forwarding with the 1.21.11 input events, `getTextureView()` (a blaze3d `GpuTextureView`
  we can wrap in an `AbstractTexture` and draw like any other texture) and `getCefBrowser()` for `loadRequest`,
  `executeJavaScript` and JCEF message routers.
- **Licence:** LGPL lets an Apache-2.0 mod use it as a *separately installed, unmodified* library. We never bundle
  or modify it; we compile against its API (`modCompileOnly`) and, with the user's consent, download the official
  jar from Modrinth into `mods/` (restart needed). NOTICE lists it as an optional download.
- Caveats: audio is played by Chromium straight to the OS (not positional; we lower the page volume with distance);
  standard CEF builds have **no proprietary codecs** (no H.264/AAC in `<video>`, VP8/VP9/AV1/Opus/Vorbis work –
  enough for YouTube, not for most `.mp4` files); MCEF Modern starts CEF with `--disable-web-security` and
  `--autoplay-policy=no-user-gesture-required`, so we only open http(s) pages and keep other players' URLs behind
  an explicit opt-in.

Alternatives considered: a separate headless browser (Playwright/Chrome) streaming screenshots – a much bigger
download (~150–300 MB plus a runtime), extra process management and slow frame transport; plain JCEF/jcefmaven
ourselves – the same natives but we would re-implement what MCEF already does (OSR texture, input mapping, shutdown)
and could clash with other mods that ship MCEF. WaterMedia (VLC based) is
[PolyForm Strict](https://modrinth.com/mod/watermedia): no derivatives/commercial use, so not an option.

## 2. Local files and direct streams: FFmpeg

| Option | Licence | Size | Formats | Verdict |
|---|---|---|---|---|
| **FFmpeg CLI as a separate process** (LGPL build) | LGPL-2.1+ (separate program, not linked) | Windows `lgpl-shared` zip ~65 MB ([BtbN builds](https://github.com/BtbN/FFmpeg-Builds/releases)), Linux/macOS: system package | everything FFmpeg decodes (H.264/HEVC/VP9/AV1, HLS, RTMP, http) | **chosen** |
| FFmpeg via JavaCV/bytedeco (JNI) | JavaCPP Apache-2.0, FFmpeg LGPL (GPL variant `-gpl`) | ~25–40 MB per platform jar | same | native code inside the game process (crashes take the game down), big jars per platform, version pinning |
| VLCJ + installed VLC | VLCJ GPL-3 (commercial licence available) | VLC install | broad | GPL is not compatible with shipping our Apache-2.0 code linked to it; needs VLC installed |
| jcodec (pure Java) | BSD | ~1 MB | H.264 baseline/main only, no audio decoding of AAC in practice | too slow and too narrow |
| MCEF + `<video>` via `file://` | – | Chromium download | no H.264/AAC (see above) | only webm/ogg; would also need web security off for local files |

Why the FFmpeg process: no native code in the Minecraft JVM, no licence coupling (we start a program the user has
or downloads; we neither link nor redistribute it), one code path for files, http(s) streams and HLS on all three
OSes, and the build can be swapped. Decoding happens in FFmpeg's own threads; we read raw RGBA frames
(`-f rawvideo -pix_fmt rgba`, scaled and frame-rate-limited by FFmpeg itself) from stdout into a few preallocated
direct buffers and upload them with `CommandEncoder.writeToTexture(texture, ByteBuffer, …)` (no per-frame
allocation). Audio is a second, video-less FFmpeg process (`-f s16le -ac 1`) feeding a streaming OpenAL source at
the screen's position, so sound is positional and uses Minecraft's distance model; the audio clock drives the
video. Lookup order: an FFmpeg we downloaded before (`<gameDir>/vibeyclient/ffmpeg/`), then `ffmpeg` on the PATH.
Download offer (Windows x64/arm64 only, after asking): the BtbN `lgpl-shared` build from GitHub, checked against
the release's `checksums.sha256`. Linux/macOS: the prompt explains `apt/dnf/pacman install ffmpeg` / `brew install
ffmpeg` (those builds are the user's own system package).

## 3. YouTube rules (what we may do)

Sources: [YouTube API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies),
[Required Minimum Functionality](https://developers.google.com/youtube/terms/required-minimum-functionality),
[IFrame Player API](https://developers.google.com/youtube/iframe_api_reference),
[Player parameters](https://developers.google.com/youtube/player_parameters),
[YouTube Terms of Service](https://www.youtube.com/t/terms).

Allowed / required:
- Only the **official embedded player** (`https://www.youtube.com/embed/<id>?enablejsapi=1&…`); the plain embed
  needs no API key ("The YouTube IFrame Player API service … does not require authorization").
- Control through the player's own API (`playVideo`, `pauseVideo`, `seekTo`, `getCurrentTime`, `setVolume`).
- **Identify the app** with an HTTP `Referer` of the form `https://<app id>` – WebViews send none by default, and
  embeds without it now fail (error 153). We send `https://dev.henny.vibeyclient` (our mod's reverse-DNS id); we
  never pretend to be another site.
- Player viewport **≥ 200×200 px** (recommended ≥ 480×270 for 16:9): our browser surface never goes below 480×270.
- Autoplay only when the player is visible (we only play screens that are rendered / near), at most one autoplaying
  YouTube player at a time (others' screens never autoplay with sound).

Not allowed (and not built):
- Downloading, caching, ripping or re-encoding YouTube media (no yt-dlp, no stream URL extraction), offline play.
- Separating audio from video (no "audio only"/background playback of YouTube), modifying or blocking the player,
  its ads, branding, links or controls; drawing overlays on top of the player (our in-world frame is around it,
  never on it; the HUD overlay shows the player unobstructed).
- Charging for or gating playback.
- Embedding API credentials in an open-source client (we use none).

## 4. Recommendation

Two optional, separately installed components, each offered by an in-game prompt that names size, source and
licence and downloads nothing until the user agrees:

1. **FFmpeg** (LGPL build, separate process) for local files and direct streams – works without any other mod.
2. **MCEF Modern** (LGPL mod, + ~150 MB Chromium natives on first use) for YouTube and web pages.

Vibey Client keeps working without both; the video feature is Fabric 1.21.11 only in this round (other targets
exclude the package at build time).
