# Writing a Vibey Client module

Everything a module needs lives in `dev.henny.vibeyclient.api` (client side) and `dev.henny.vibeyclient.ui`.
Each module owns one package, `dev.henny.vibeyclient.module.<id>`, plus its i18n files and its mixin config, so
several people can work on different modules without touching the same files.

> See also: [UI.md](UI.md) (design rules, menus, widgets) · [AVATAR.md](AVATAR.md) (cosmetics/emotes engine) ·
> [SPEC.md](SPEC.md) (shared contract) · [docs index](README.md)

## Layout of a module

```
src/main/java/dev/henny/vibeyclient/module/<id>/
    <Name>Module.java          extends VibeyModule, registered in module/BuiltinModules.java (one line)
    mixin/                     mixins of this module only
src/main/resources/
    vibeyclient.<id>.mixins.json            listed in fabric.mod.json and META-INF/neoforge.mods.toml
    assets/vibeyclient/i18n/en_us/<id>.json own strings (required)
    assets/vibeyclient/i18n/de_de/<id>.json German (required, every string)
    assets/vibeyclient/lang/{en_us,de_de}.json  only keybind names (shared file, add your lines)
```

Existing stubs: `social`, `cosmetics`, `emotes`, `multiplayer` (conflicts with mod id `vibey`), `media` (Fabric
only: the package is excluded from the NeoForge build and not registered there; its parts are *media features*, the
`lyrics` part conflicts with `vibeylyrics` – see [MEDIA.md](MEDIA.md)), `devlink`, `nametags` (seal, rank icons and name styles before player names; data in `client/VibeyPlayers`, `client/ranks`, see [NAMETAGS.md](NAMETAGS.md)), `pvp` (zoom, crosshair,
toggle sprint, outlines; Fabric 1.21.11 only for now, see [PVP.md](PVP.md)), `waypoints` (beacons, share codes, minimap, world map, death point, nether
coordinates; Fabric 1.21.11 only, see [WAYPOINTS.md](WAYPOINTS.md)), `info` (light level, chunk borders,
block/mob card with world pinning, food gain, tool warning, TNT timer; Fabric 1.21.11 only, see [INFO.md](INFO.md)), `camera` (freelook, orbit view, motion blur; Fabric 1.21.11 only, see [CAMERA.md](CAMERA.md)),
`visuals` (held item models with an in-game editor, swing, glint, item sizes, item physics, particle amount, part
`visuals.effects`: effects and per-type particle editor; Fabric 1.21.11 only, see [VISUALS.md](VISUALS.md)). The screenshot gallery is
part of `clips` ([CLIPS.md](CLIPS.md) §14). `chat` (timestamps, search, copy, mentions, filters, DM
toasts, movable chat window; Fabric 1.21.11 only, see [CHAT.md](CHAT.md)). Social also has Discord Rich Presence and
streamer mode (part `social.streamer`, Fabric 1.21.11), see [SOCIAL.md](SOCIAL.md); `api/privacy/Privacy` is the shared
streamer state any module can read. `inventory` (durability %, shulker preview, quick moves, search, hotbar presets; Fabric
1.21.11 only, see [INVENTORY.md](INVENTORY.md)), `servers` (live pings, search, favourites, MOTD preview, drag & drop, friends tab in the
multiplayer screen, own world order; Fabric 1.21.11 only, see [SERVERS.md](SERVERS.md)), `cinematic` (AFK camera flights and an
editable AFK screen; Fabric 1.21.11 only, see [CINEMATIC.md](CINEMATIC.md)), `screens` (screen editor: move/scale/hide the widgets of any menu, container panels, own
screen size, dim/blur, hide title; Fabric 1.21.11 only, see [SCREENS.md](SCREENS.md)), `companion` (a living companion from companion
packs, built on the `companio` lib; Fabric 1.21.11 only, see [COMPANION.md](COMPANION.md)).

## Targets and boundaries

`gradle/modules.gradle` is the one place that says which targets build a module: `'all'`, a loader (`'fabric'`,
`'neoforge'`) or a list of target names; a dotted id is a part (`'media.video'` = `module/media/video`). For every
target that does not build it, `gradle/modules-filter.gradle` leaves out the package, `i18n/*/<id>[.*].json`,
`vibeyclient.<id>[.*].mixins.json`, the matching lines in `fabric.mod.json` / `neoforge.mods.toml` and the extra
resources listed in `clientModuleResources`. Modules present only in some jars are registered by reflection in
`BuiltinModules`. To port a module, widen its entry.

Modules must not use each other: `checkModuleBoundaries` (part of `:fabric-1.21.11:check` and `buildAll`) fails when
a file in `module/<a>/` names `dev.henny.vibeyclient.module.<b>.…`. Shared code goes to `api`, `client`, `ui` or a
lib. Code that only some targets build and several modules need goes into a shared package outside `module/`
(`sharedPackages` in `gradle/modules.gradle`, same target syntax as modules; it must not import a module, also
checked). Today: `media` (`media/ffmpeg`, `media/player`: FFmpeg lookup/download/setup screen and the file player
used by the video screens and Clips, see [VIDEO.md](VIDEO.md#shared-ffmpeg-and-player-code)). Existing exceptions
still to remove are listed in `knownModuleImports`.

`checkI18n` (every string in en_us and de_de, every literal key exists) runs in the same places, so a missing
translation fails `:fabric-1.21.11:build`.

## Client only

Modules only exist on the client. On a dedicated server the jar runs just `Vibey.initCommon` and the packet relay
(`net/ServerRelay`). Never reference `api`, `client`, `ui` or `module` classes from common code (`Vibey`,
`Platform`, `Compat`, `net`, `fabric/VibeyFabric`, `neoforge/VibeyNeoForge`, `neoforge/NeoForgePlatform`).
If a module ever needs server logic, put it in a server-safe class and wire it through the common entry points.

## Lifecycle

```java
public final class EmotesModule extends VibeyModule {
    public final KeybindSetting wheel = keybind("wheel", Keys.B).onPress(this::openWheel);
    public final FloatSetting volume = floatRange("volume", 1f, 0f, 1f, 0.05f);

    public EmotesModule() { super("emotes", ModuleCategory.COSMETICS); }

    @Override public void onLoad()    { /* once: services, net channels, event listeners, tabs */ }
    @Override public void onEnable()  { /* switched on (first time: first client tick) */ }
    @Override public void onDisable() { /* switched off at runtime: undo everything */ }
    @Override public void tick()      { /* 20/s while enabled, also on the title screen */ }
    @Override public void renderHud(Gfx g) { /* every frame while enabled and HUD visible */ }
    @Override public void onWorldJoin()  {}
    @Override public void onWorldLeave() {}
}
```

Order at startup (client thread):

1. Constructor and field initialisers: declare settings and keybinds.
2. `ModuleManager.load()`: conflicts (`conflictingMods()`, default from `ModConflicts`), `available()`,
   `dependencies()`; then for every module that can run: settings file is read, `onLoad()` is called.
   Do not use `Minecraft.getInstance()` state here (NeoForge constructs mods before the game is ready).
3. Keybinds are handed to the loader (only those of modules that can run).
4. First client tick: modules that are on (persisted in `config/vibeyclient/modules.json`, default
   `defaultEnabled()`) get `onEnable()`. Only once the beta access gate is open (ACCESS.md); until then no module
   runs, and `onLoad()` must not start network traffic or features. A denial later disables all modules again
   (`ModuleManager.suspend()`, the saved on/off choice stays).

Rules: `onEnable`/`onDisable` must be reversible without a restart. Disabling a module disables everything that
depends on it; enabling a module enables its dependencies. A module whose conflicting standalone mod is installed
shows as "Provided by the installed standalone mod" and can't be switched on. Exceptions in hooks are caught and
logged (first 3 per module), they do not crash the game.

Other knobs on `VibeyModule`: `nameKey()`/`descriptionKey()` (default `<id>.name`, `<id>.description`),
`icon()` (texture id or null for a monogram tile), `accentColor()`, `visible()` (social hides itself while online
features are off), `canToggle()`, `createSettingsPanel()` (classic UI only: custom page body),
`notices()` (warning chips in the page bar, the full text on hover, e.g. "Lyrics: provided by the installed Vibey
Lyrics"). The category picks the place in the menu: `ModuleCategory.CREATOR` modules sit behind the *Creator tools*
chip under the module grid. `<id>.name` ≤ 12 characters; `<id>.summary` (optional) is the tile's tooltip (default:
the description).

Look modules up with `ModuleManager.get().get(EmotesModule.class)` or `.get("emotes")`, check `isEnabled()`.

## Settings

Declare them as fields with the helpers; they are saved to `config/vibeyclient/<id>.json` (debounced, on a worker
thread) and rendered automatically in the Vibey menu.

| Helper | Type | UI |
|---|---|---|
| `bool(key, def)` | `BoolSetting` | switch |
| `intRange(key, def, min, max[, step])` | `IntSetting` (`.unit("%")`) | slider + value |
| `floatRange(key, def, min, max, step)` | `FloatSetting` (`.decimals(n)`) | slider + value |
| `choice(key, Enum.DEFAULT)` | `EnumSetting<E>` | segments / select; `.icons(e -> iconName)` = icon segments, the name on hover |
| `string(key, def[, maxLength])` | `StringSetting` | text field |
| `color(key, argb)` | `ColorSetting` (`.presets(...)`) | swatches |
| `keybind(key, Keys.X or Keys.UNBOUND)` | `KeybindSetting` (`.onPress(...)`, `isDown()`) | key field in its section (click, press a key) |
| `action(key, runnable)` | `ActionSetting` | button at the top of the module page (advanced: a row) |

Every setting: `get()`, `set(v)` (clamped, listeners fire, save scheduled), `reset()`, `.onChange(v -> ...)`,
`.section("key")` (titled card), `.advanced()`, `.serverRule("zoom")` (row shows *Disabled by this server* while
`ServerRules.isDisabled(key)`; the feature still has to check it itself), `.visibleWhen(() -> ...)`, `.labelKey("full.i18n.key")` (label from
another file, used by media features). Settings added after the file was read still get their stored value; keys the
current run doesn't declare are kept in the file. Keybinds: `.skipRegistration()` keeps one out of the controls
screen (a part that is off because a standalone mod provides it).

**The module page** (click on the module's tile in the Vibey menu) is generated from the settings, so write them for
normal players:

- Settings without a section come first; `.section("show")` groups settings into a card titled `section.show`
  (sections appear in the order of their first setting; 1 column at 427 px, more on wider screens).
- Keybinds go into their section; without one they join the untitled card (or a card titled with the module name).
  The first non-advanced `action(...)` is the page's one primary button (e.g. *Open wardrobe*); further actions are
  rows with a button.
- `.advanced()` is for **technical** things only (addresses, ports, tokens, debug switches, protocol details): they
  sit behind the *More n* chip at the bottom. A normal player should never need it.
- Labels ≤ 3 words, no jargon; the details go into `setting.<key>.desc` (one sentence, shown on hover). Every string
  in English and German. Dynamic choices that are not stored in the settings file: `OptionSetting`.
- The new menu does not show `createSettingsPanel()`; see [UI.md](UI.md#writing-ui-for-a-module).

Labels come from your i18n file: `setting.<key>` (label), `setting.<key>.desc` (optional second line),
`setting.<key>.<enum value lower case>` for enum values, `setting.<key>.button` for an action button in a row,
`setting.<key>.action` for the same action as a button at the top of the module page (default: the label),
`section.<key>` for section titles (`checkI18n` checks all of them; `.sectionLabel(fullKey)` takes the title from
another key, e.g. each media feature's card is titled with `media.<id>.name`).

Free-form persistent data (caches, lists): `settings().data()` is a `JsonObject` stored under `"_data"`; call
`settings().markDirty()` after changing it.

Global settings (`GlobalSettings`, the gear's quick settings): `language` (auto/de/en), `uiScale`, `animations`,
`onlineFeatures` (default on; when off: **no backend connection at all**), `openMenu` keybind (Right Shift),
`backendUrl` (advanced; use
`GlobalSettings.backend()`). Listen to `VibeyEvents.OnlineFeaturesChanged`.

## Keybinds

`keybind("wheel", Keys.B)` (key codes in `client/Keys` – do not import LWJGL, it is not on every
classpath) creates the mapping in the "Vibey Client" category of the vanilla controls
screen; the vanilla name key is `key.vibeyclient.<module>.<key>` and must be in `assets/vibeyclient/lang/*.json`.
Declare it in the constructor/field or `onLoad` – later throws. Use `.onPress(Runnable)` (the core consumes the
clicks) or `isDown()`; both only react while the module is enabled. The key itself is stored by vanilla in
`options.txt`. Version differences (KeyMapping.Category since 1.21.9, Fabric's KeyMappingHelper in 26.x) are
handled in `ClientCompat` and the loader entry points.

## Network (server channel, SPEC §6)

Works only when the server (dedicated, LAN or integrated) also runs Vibey Client; elsewhere everything returns
false and nothing breaks. On join the client sends `vibeyclient:hello`; when the server answers,
`VibeyNet.available()` turns true and `VibeyEvents.ServerHello` is posted.

```java
VibeyNet.sendLoadout(Map.of("cape", "vibey:seal_cape"));       // → others get VibeyEvents.RemoteLoadout
VibeyNet.sendEmote("vibey:wave", true);                         // → others get VibeyEvents.RemoteEmote (with server tick)
on(VibeyEvents.ServerHello.class, e -> VibeyNet.sendLoadout(current()));   // resend after joining
```

Module messages (typed, by name) go through the generic `vibeyclient:msg` envelope:

```java
record Wave(String text) {}
static final StreamCodec<FriendlyByteBuf, Wave> CODEC = StreamCodec.of(
        (buf, w) -> buf.writeUtf(w.text(), 64), buf -> new Wave(buf.readUtf(64)));

VibeyNet.Channel<Wave> waves = VibeyNet.channel("social:wave", CODEC);   // once, in onLoad
waves.onReceive((sender, wave) -> ...);                                   // client thread
waves.broadcast(new Wave("hi"));  /  waves.sendTo(uuid, new Wave("hi"));
```

Limits (`net/NetLimits`): channel `[a-z0-9_.:-]{1,64}`, message ≤ 8 KiB, loadout ≤ 16 slots, ids ≤ 128 chars in
the `.vibey` id format. The server relay (`net/ServerRelay`) only relays to players that said hello, rate-limits
per player (loadout 2/s, emote 2/s, messages 20/s, with bursts) and sends the current loadouts of everyone to a
player who just said hello. It never forwards asset bytes. Payloads are registered for every version on both loaders
(Fabric `playC2S/playS2C`, 26.x `serverboundPlay/clientboundPlay`; NeoForge `playBidirectional`, optional).

## Server rules (features a server switched off)

Servers can disable features with the open `vibey:rules` channel (admin docs and keys: `docs/SERVER-RULES.md`,
contract: vibe-launcher `docs/PLAY-LINKS.md` §5). The core (`client/ServerRulesClient`) receives and validates the
packet and feeds `api/ServerRules`; a module only has to:

```java
ServerRules.supports("zoom");                      // onLoad: the key goes into vibey:hello
if (ServerRules.isDisabled("zoom")) return;        // in the feature, every time it would act (cheap, any frame)
ServerRules.listen(() -> ...);                     // optional: react when rules arrive/clear (client thread)
public final BoolSetting enabled = bool("zoom", true).serverRule("zoom");   // row dimmed + badge (see Settings)
@Override public List<String> serverRuleKeys() { return List.of("zoom"); } // tile line + notice on the page
```

`ui/ServerBadge` is the one badge component: the settings page uses it for blocked rows (and once in a card's title
when every row of the card is blocked), custom panels call `ServerBadge.draw/drawRight` themselves (owner = any
stable object for the tooltip). Rules are cleared when the player leaves; the stored values are untouched.

## Events

```java
Events.subscribe(VibeyEvents.WorldJoin.class, e -> ...);   // global, returns a Subscription (cancel())
on(VibeyEvents.ScreenInit.class, e -> ...);                // in a module: only delivered while enabled
Events.post(new MyEvent(...));                             // your own records work too
```

Core events (client thread): `WorldJoin`, `WorldLeave`, `ScreenInit` (screen, widgets, add/remove widget – e.g.
add a title-screen button), `ModuleToggled`, `LanguageChanged`, `OnlineFeaturesChanged`, `ServerHello`,
`RemoteLoadout`, `RemoteEmote`. `BackendConnected`/`BackendDisconnected` are posted by the social module.

## Services

```java
Services.register(HostSessionService.class, impl);        // provider (onLoad/onEnable)
Services.get(HostSessionService.class).ifPresent(...);    // consumer, look up lazily
```

`HostingService` (api/service) is provided by the multiplayer module while it is enabled (it posts
`HostingService.HostingChanged` with the host-only address; the port comes from `port()`).
`WaypointPoints` (api/service) is provided by the waypoints module: the visible waypoints (name, dimension, block
position) for modules that only read them (the cinematic tour).
Put shared service interfaces in the providing module's package (e.g. `module.multiplayer.HostSessionService`).

## Vibe backend and toasts

The social module owns the backend connection (`backend/BackendClient`: Mojang-verified login, rotating tokens,
REST with retries, the realtime gateway) and registers it as `VibeBackend` while social and online features are on.

```java
Services.get(VibeBackend.class).ifPresent(b -> {
    if (!b.isReady()) return;                                         // logged in + gateway ready
    b.request("GET", "/api/v1/cosmetics/me", null)                   // token refresh is handled for you
            .thenAcceptAsync(json -> apply(json), Tasks::onMain)       // never join() on the render thread
            .exceptionally(t -> { /* VibeBackend.ApiException: status, code, retryAfter */ return null; });
    b.sendOp("cosmetics.subscribe", obj);                             // re-sent automatically after reconnect/resume
});
on(VibeBackend.Ready.class, e -> ...);                                // every fresh `ready` (not on resume)
AutoCloseable h = backend.onEvent("loadout.updated", d -> ...);       // client thread; close() to stop
```

`presence` and `cosmetics.subscribe` ops are remembered and sent again after a reconnect. Tokens live in
`<gameDir>/vibeyclient/auth.json` (per backend URL and Minecraft account), never in `config/`.

Toasts from any module: `Notify.info/success/warn/error(title, body)` or `Notify.show(new Notify.Toast(...))`
(icon key = a player UUID for a head, or a pixel icon name like `vibeyclient:host`). The sink is
`ui/vui/toast/VuiToasts` (vui `toast/stack`, bottom right, over every screen and on the HUD; a `Notify.Toast` action
becomes the toast's one button). `ToastStack.show(toast, buttons, durationMs, key)` / `ToastStack.dismiss(key)` still
work (first button = the action).

## Background work

Never block the render thread. `Tasks.run(...)`, `Tasks.supply(...)` (worker pool), `Tasks.onMain(...)` (back to
the client thread), `Tasks.later(ms, ...)`, `Tasks.debounce(key, ms, ...)`, `Tasks.executor()` (for an
`HttpClient`). Pending debounced work (settings saves) is flushed on exit.

## i18n

`assets/vibeyclient/i18n/<en_us|de_de>/<id>.json`, keys are prefixed with the file name: `"name"` in
`social.json` is `social.name`; nested objects are flattened (`{"setting": {"x": ".."}}` = `setting.x`).
Use `tr("key", args...)` in a module (= `Lang.tr("<id>.key")`), `Lang.tr(fullKey)`, `Lang.text(...)` for a
`Component`. Placeholders `{0}`, `{1}`. Missing German falls back to English, then to the key. The language follows
the global setting (auto = game language). Every user-facing string needs both languages. Vanilla lang files are
only for keybind names/categories and mod menu text.

## HUD elements

Anything a module draws on the HUD that players may want elsewhere should be a `HudElement` (`ui/hud`), so it shows
up in the HUD editor. New content should be a **widget** (`HudElement.builder("<module>.<name>")...widget(MyWidget::new)`,
usually a `TextWidget`): the core draws, styles and saves it and hides it when the server disables its
`.rules(key)`. Owner-drawn elements use `.render(...)` + `HudLayout.draw(g, element)` from `renderHud`.
Full API: **[HUD.md](HUD.md)**.

## UI

Design rules, title screen buttons / pause menu entries (`MenuEntries`), the module page, the player preview
service (`MenuPreview`), icons and the widget cheat sheet: **[docs/UI.md](UI.md)**.

- `Gfx` wraps GuiGraphics / GuiGraphicsExtractor (26.x): `rect`, `outline`, `text`, `bold`, `textScaled`,
  `image`, `scissor`, `push/translate/scale/pop`, `widget`. Use it for HUD and screens – never the vanilla drawing
  API directly (it differs per version).
- `BaseScreen`: version-neutral screen (`draw`, `click`, `release`, `drag`, `scroll`, `key`).
- `Theme`: SPEC §4 colors (`BG`, `SURFACE..3`, `PANEL`, `LINE`, `LINE2`, `TEXT`, `MUTED`, `DIM`, `BRAND`, `BRAND2`,
  `OK`, `DANGER`, `WARN`), `alpha()`, `mix()`.
- `Anim`: 150 ms eased transition (cubic-bezier(0.2, 0.8, 0.2, 1)), time based, no allocation; respects the
  animations setting. `ui.anim(key, slot, initial)` keeps one per widget.
- `Ui` + `Widgets` (immediate mode): `button`, `toggle`, `slider`, `cycler`, `textField`, `swatches`, `chip`,
  `caption`, `monogram`; `ScrollArea` for lists. Pattern: `if (Widgets.button(ui, key, x, y, w, h, label, Style.PRIMARY)) ...`
- `CardScreen`: one centered card in the Vibe look, immediate mode with hit areas (header, scrollable body, footer;
  `row`, `toggle`, `button`, `segmented`, `cycler`, `stepper`, `swatches`, `inputBox`, ...). Used by the host screens
  (multiplayer) and the lyrics screens; texts passed to `row` are keys relative to the module's i18n prefix.
- The menu is the vui `MenuScreen` (`Menus.open(parent)`, `Menus.openModule(parent, id)`); new screens are vui
  documents, see [UI.md](UI.md). `VibeyScreen`/`VibeyTabs`/`Panel` only serve the classic UI
  (`-Dvibeyclient.classicUi=true`).

## Mixins

One config per module: `vibeyclient.<id>.mixins.json`, package `dev.henny.vibeyclient.module.<id>.mixin`, plugin
`dev.henny.vibeyclient.mixin.VibeyMixinPlugin`. Put client mixins in `"client"`, common ones in `"mixins"`.
The plugin skips all mixins of a module when its conflicting standalone mod is installed (`ModConflicts`).
New conflicts: add them to `ModConflicts` (read before the game starts) – `conflictingMods()` uses it by default.
The media configs (`vibeyclient.media.*.mixins.json`) are not listed in `neoforge.mods.toml` and excluded from the
NeoForge jar. A part of a module can have its own config: the plugin checks `ModConflicts.of("<module>")` and
`ModConflicts.of("<module>.<part>")` for `dev.henny.vibeyclient.module.<module>.<part>.mixin`.

## Version-specific code

Sources are written for `fabric-1.21.11` (the Stonecutter active version – never switch it, it rewrites `src/`).
Use Stonecutter comments for differences and keep them in `Compat` (common), `client/ClientCompat` (client),
`ui/Gfx`, `ui/BaseScreen` and the loader entry points:

```java
//? if >=1.21.9 {
return new KeyMapping(name, keyCode, category());
//?} else
/*return new KeyMapping(name, keyCode, "key.categories.vibeyclient");*/
```

`//? if fabric {` / `//? if neoforge {` select the loader. Fabric-only code goes in `fabric/` (or a Fabric-only
module package excluded in `gradle/neoforge.gradle`). Build every target you touched:
`./gradlew :fabric-1.21.1:build :neoforge-26.3:build` or all with `./gradlew buildAll`.
