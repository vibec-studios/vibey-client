# Media module

Music, lyrics, skies and video in one module (`media`, Fabric only). Code: `dev.henny.vibeyclient.module.media`.
The module is a small host; everything that plays something is a **media feature** with its own switch, i18n file
and standalone-mod conflict. Built-in features:

| Feature | Package | What |
|---|---|---|
| `lyrics` | `module.media.lyrics` | Synced lyrics (LRCLIB, own `.lrc`, Spotify/browser via the system media controls), own songs (OGG/MP3/WAV), custom skies that react to the music, sky/front/world/HUD lyrics, 3D flight, now playing bar. Ported from Vibey Lyrics. Off on its own when the standalone `vibeylyrics` is installed. |
| `video` | `module.media.video` | Video screens in the world and a HUD overlay: YouTube (official embed), web pages, files, streams; sharing with other players. Fabric 1.21.11 only for now. See [VIDEO.md](VIDEO.md). |

> See also: [MODULES.md](MODULES.md) (module API) · [UI.md](UI.md#hud-editor) (HUD editor) · [SPEC §2](SPEC.md) ·
> [docs index](README.md)

## Writing a media feature

One package per feature: `dev.henny.vibeyclient.module.media.<id>`, registered with **one line** in
`module/media/MediaFeatures.java`:

```java
static void registerAll(MediaModule m) {
    m.addFeature(new LyricsFeature(m));
    m.addFeature(new VideoFeature(m));
}
```

```java
public final class VideoFeature extends MediaFeature {
    // Settings: stored in config/vibeyclient/media.json as "video.<key>", labels from media.video.json
    public final IntSetting volume = intRange("volume", 80, 0, 100, 5).unit("%");
    public final BoolSetting subtitles = bool("subtitles", true).advanced();
    // Keybind: vanilla name key.vibeyclient.media.video.<key> (in lang/en_us.json + de_de.json)
    public final KeybindSetting pause = keybind("pause", Keys.UNBOUND).onPress(this::togglePause);
    // A 2D element the player can move/scale in the HUD editor: id "media.video.overlay",
    // label media.video.hud.overlay
    public final HudElement overlay = hudElement(HudElement.builder("overlay")
            .size(160, 90)                        // unscaled GUI px (or suppliers)
            .defaults(Anchor.TOP_RIGHT, -6, 30)  // until the player moves it
            .scale(1f, 0.5f, 3f)                  // default, min, max
            .visibleWhen(() -> hasVideo())        // shown in the editor only while this holds
            .render(this::drawOverlay));          // (Gfx g, int w, int h, boolean preview)

    public VideoFeature(MediaModule media) { super(media, "video"); }

    @Override public void onLoad()    { /* once at startup: hooks, services (only if the feature can run) */ }
    @Override public void onEnable()  { /* Media on AND the feature's switch on */ }
    @Override public void onDisable() { /* undo everything, reversible without a restart */ }
    @Override public void tick()      { /* 20/s while enabled */ }
    @Override public void renderHud(Gfx g) { HudLayout.draw(g, overlay); }   // applies the player's placement
    @Override public void onWorldJoin()  {}
    @Override public void onWorldLeave() {}

    private void drawOverlay(Gfx g, int w, int h, boolean preview) {
        // draw into (0, 0)–(w, h); preview = the HUD editor wants sample content while nothing plays
    }
}
```

Rules:

- **Declare in the constructor / field initialisers** (settings, keybinds, HUD elements), like a module. Keybinds
  can't be created after the game started. `conflictingMods()` and `defaultEnabled()` are called from the
  `MediaFeature` constructor – don't use subclass fields in them.
- **Lifecycle:** `onLoad` once (skipped when a standalone mod replaces the feature or `available()` is false);
  `onEnable`/`onDisable` whenever Media or the feature's switch changes; `tick`, `renderHud`, `onWorldJoin`,
  `onWorldLeave` only while enabled. Exceptions are caught and logged (first 3). `isEnabled()`, `usable()`,
  `conflict()` tell you the state; `MediaModule.running(VideoFeature.class)` from anywhere.
- **Every feature gets a switch** (`enabledSetting`, key `<id>.enabled`, label `media.<id>.setting.enabled`) at the
  top of its settings. Keep the visible settings few and plain; put technical ones under `.advanced()`.
- **World rendering, mixins:** register your own world render hook in `onLoad` and check `isEnabled()` in it
  (see `LyricsFeature.registerHooks`). Mixins go in `vibeyclient.media.<id>.mixins.json`, package
  `dev.henny.vibeyclient.module.media.<id>.mixin`, plugin `dev.henny.vibeyclient.mixin.VibeyMixinPlugin` (it skips
  them when `ModConflicts.of("media")` or `ModConflicts.of("media.<id>")` names an installed mod). List the config in
  `fabric.mod.json` and exclude it in `gradle/neoforge.gradle` (the whole `module/media/**` package already is).
- **Conflicts:** add `"media.<id>", List.of("<modid>")` to `ModConflicts` if a standalone mod replaces your part. The
  settings page then shows "<feature>: provided by the installed <mod>" (`VibeyModule.notices()`) and your
  keybinds are not registered.
- **Strings:** `assets/vibeyclient/i18n/{en_us,de_de}/media.<id>.json` with `name`, `description`,
  `setting.enabled` (+ `.desc`), `setting.<key>` (+ `.desc`, enum values `setting.<key>.<value>`),
  `hud.<element>`. `tr("key")` in a feature = `media.<id>.key`. `./gradlew checkI18n` checks `*Feature.java`
  files (`super(media, "<id>")`) like modules.
- **Data:** `data()` is a `JsonObject` stored under `_data.<id>` in media.json; call `markDirty()` after a change.
- **Events:** `on(Type.class, e -> ...)` only delivers while the feature is enabled.

## Lyrics & skies (`lyrics`)

What the player sees (Vibey menu → Media): *Open the player*, *Lyrics position* (sky / in front / in the world /
HUD), *Look*, *Sky moves with the music* + *Effect strength*, *Music from the PC*, *Volume*. Advanced: how sky lyrics
move, soft flash on drops, use PC sound for effects, sync offset, menus, bar, sparkles, game music, online lookup,
clouds, shooting stars, lyrics over hills, keybinds. Everything else (fonts, colors, animation, sky picker) is in
the player screen (K) → *Advanced*.

### Lyrics in the world (r3)

Sky, front and world lyrics are one r3 world object (`lyrics/world/WorldText`, kind `vibeyclient:lyrics`) whose
content (`LyricsContent`, an r3 `CustomContent`) builds the `LyricLayout` frame every frame and paints it through r3's
`WorldCanvas` (`CanvasPainter`: words with the lyric font via `styledText`, glow rings, 3D sides, chroma edges, caption
boxes as quads) – the same look as before, now drawn by r3 (sky layer −1000, "over hills" = r3 shrinks it towards the
eye to 3.5 blocks). Where it sits is an r3 anchor (`LyricsPlacement`):

| Mode | Anchor |
|---|---|
| Sky – where you look (default) | `ViewFollowAnchor`: rests within ~20°, glides after 350 ms (at once beyond 55°), critically damped 0.5 s, ≤ 170°/s, 9° above the crosshair, 9–58° above the horizon |
| Sky – follow softly | `LowersWhenLookingDown(SoftFollowAnchor.soft)`: rests until you turn > 30° away, then glides back |
| Sky – glued to your view | `LowersWhenLookingDown(SoftFollowAnchor.follow)`: eases after every turn |
| Sky – one direction | `SoftFollowAnchor.fixed`: the yaw from when the song started |
| In front | `CameraAnchor(distance, 0, +6°, 4.5/s)` |
| In the world | a free object at the saved spot: **grab it with the middle mouse to move it, wheel to resize** (saved as `anchorX/Y/Z`, `anchorScale`) |

Sky size = distance × 0.013 × size (distance = render distance × 0.35, 32–96 blocks). After a pause (no song, other
position, hidden) of more than a second the anchor starts again where you look. The sky dome and sky effects still use
their own hook (`render/SkyHook` → `SkyDome`, `WorldSink`).

### Music panel (`lyrics/world/MusicPanel`)

A small vui panel in the world (`assets/vibeyclient/vui/music/panel.vui`, 1.5 × 0.5 blocks): cover, title, artist,
progress, ⏮ ▶/❚❚ ⏭ ✕ – from the same source as the now playing bar (`Playback`: songs from the folder or the PC's
player). Media settings → *Music panel* → *Place*: it follows the crosshair, left click places it on a wall/floor (or
in the air). Look at a button and left click. Hold the middle mouse to move it, wheel to resize, right click pins it
to your view (`SoftFollowAnchor`: it stays a little below where you look and comes along when you turn away; look at
it to use the buttons) and right click again unpins it. Saved per world as an r3 placement (`vibeyclient:music`).

When it shows: only while **Media is on, Lyrics is on and *Show panel*** (`media.lyrics.panelOn`, default on) is on.
The panel plays the lyrics feature's songs (`Playback`), so it is part of Lyrics: switching Lyrics or the whole Media
module off hides it, and *Show panel* hides it on its own. Hiding only removes the object from the scene
(`MusicPanel.sync(false)`); the saved placement stays and the panel comes back at the same spot when all three are on
again (`sync(true)` restores it once per world). *Place panel* turns *Show panel* on. The ✕ on the panel removes it
and its placement.

### Music analysis (`lyrics/fx`)

| Source | When | How |
|---|---|---|
| Own songs / demo | a song from the folder plays | the audio thread copies the decoded PCM (mono) into a ring buffer keyed by frame; the analysis thread reads the window that is **audible now** (sound-card position), so effects are in sync with what you hear |
| PC sound | music from the PC (Spotify, browser, ...) on Windows, setting *Use PC sound* | WASAPI loopback of the default output device through a small PowerShell/C# helper (`assets/vibeyclient/lyrics/loopback/windows.ps1`, compiled by Windows PowerShell's `Add-Type`): no permission prompt, no driver, nothing recorded or sent – the helper streams 12 kHz mono PCM to the game over stdout and quits with it |
| Lyrics timing | lyrics without audio, or PC music where loopback is off/unavailable (macOS, Linux) | word and line starts become soft beats, a tempo estimate from their spacing |

The analysis thread (`MusicAnalyzer`, ~86 Hz) runs a 2048/1024-point FFT with a Hann window and computes bass,
mid and high energy (adaptive normalisation), spectral flux onsets with an adaptive threshold, a tempo estimate
from the onset intervals and drops (short-term bass/loudness far above the long-term average after a quieter
part). Everything is preallocated: no allocation per hop or per frame. The render side (`SkyFx`) turns that into
gentle effects: the sky's color shifts with the energy, the horizon glows with the bass, beats make a group of
stars twinkle and shimmer motes appear, aurora curtains brighten with mids/highs, drops give a bloom (or, only with
*Soft flash on drops*, a soft flash). Epilepsy-safe by design: pulses at most 3 per second with soft attack/decay,
flashes off by default and at most one per 2.5 s, *Effect strength* scales everything (0 % = off).

## HUD editor

Media's settings page → *HUD layout → Edit* (or the *HUD editor* key) opens the generic HUD editor
(`ui/hud`, see [UI.md](UI.md#hud-editor)). Media registers *Lyrics* (`media.lyrics.text`, position HUD) and
*Now playing* (`media.lyrics.bar`); features add theirs with `hudElement(...)`.

## Migration from "lyrics"

Up to 0.2 the module was called `lyrics`. On the first start `MediaMigration` moves `config/vibeyclient/lyrics.json`
into `media.json` (`k` → `lyrics.k`, `_data` → `_data.lyrics`; the old file is kept as `lyrics.json.migrated`),
the on/off state in `modules.json` and the keybinds in `options.txt` (`key.vibeyclient.lyrics.*` →
`key.vibeyclient.media.lyrics.*`). The lazy sky mode "follow softly" (the old default) becomes "where you look".
Songs stay in `.minecraft/vibey-lyrics/` (shared with the standalone mod).
