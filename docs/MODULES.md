# Writing a Vibey Client module

Everything a module needs lives in `dev.henny.vibeyclient.api` (client side) and `dev.henny.vibeyclient.ui`.
Each module owns one package, `dev.henny.vibeyclient.module.<id>`, plus its i18n files and its mixin config, so
several people can work on different modules without touching the same files.

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

Existing stubs: `social`, `cosmetics`, `emotes`, `multiplayer` (conflicts with mod id `vibey`), `lyrics`
(conflicts with `vibeylyrics`, Fabric only: the package is excluded from the NeoForge build and not registered
there), `devlink`.

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
   `defaultEnabled()`) get `onEnable()`.

Rules: `onEnable`/`onDisable` must be reversible without a restart. Disabling a module disables everything that
depends on it; enabling a module enables its dependencies. A module whose conflicting standalone mod is installed
shows as "Provided by the installed standalone mod" and can't be switched on. Exceptions in hooks are caught and
logged (first 3 per module), they do not crash the game.

Other knobs on `VibeyModule`: `nameKey()`/`descriptionKey()` (default `<id>.name`, `<id>.description`),
`icon()` (texture id or null for a monogram tile), `accentColor()`, `visible()` (social hides itself while online
features are off), `canToggle()`, `createSettingsPanel()` (custom page instead of the generated one).

Look modules up with `ModuleManager.get().get(EmotesModule.class)` or `.get("emotes")`, check `isEnabled()`.

## Settings

Declare them as fields with the helpers; they are saved to `config/vibeyclient/<id>.json` (debounced, on a worker
thread) and rendered automatically in the Vibey menu.

| Helper | Type | UI |
|---|---|---|
| `bool(key, def)` | `BoolSetting` | switch |
| `intRange(key, def, min, max[, step])` | `IntSetting` (`.unit("%")`) | slider + value |
| `floatRange(key, def, min, max, step)` | `FloatSetting` (`.decimals(n)`) | slider + value |
| `choice(key, Enum.DEFAULT)` | `EnumSetting<E>` | ‹ value › cycler |
| `string(key, def[, maxLength])` | `StringSetting` | text field |
| `color(key, argb)` | `ColorSetting` (`.presets(...)`) | swatches |
| `keybind(key, Keys.X or Keys.UNBOUND)` | `KeybindSetting` (`.onPress(...)`, `isDown()`) | rebind button |
| `action(key, runnable)` | `ActionSetting` | button |

Every setting: `get()`, `set(v)` (clamped, listeners fire, save scheduled), `reset()`, `.onChange(v -> ...)`,
`.advanced()` (shown under "Advanced"), `.visibleWhen(() -> ...)`.

Labels come from your i18n file: `setting.<key>` (label), `setting.<key>.desc` (optional second line),
`setting.<key>.<enum value lower case>` for enum values, `setting.<key>.button` for action buttons.

Free-form persistent data (caches, lists): `settings().data()` is a `JsonObject` stored under `"_data"`; call
`settings().markDirty()` after changing it.

Global settings (`GlobalSettings`): `language` (auto/de/en), `onlineFeatures` (default on; when off: **no backend
connection at all**), `animations`, `openMenu` keybind (Right Shift), `backendUrl` (advanced; use
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
(icon key = a player UUID for a head). The sink is `ui/toast/ToastStack` (top right, max 4, over every screen and
on the HUD; `ToastStack.show(toast, buttons, durationMs, key)` for toasts with buttons).

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

## UI

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
- `VibeyScreen` is the menu (Right Shift, pause menu button). Add a top-level tab from `onLoad`:
  `VibeyTabs.register(new VibeyTabs.Tab("friends", "social.tab.friends", 10, FriendsPanel::new, this::isEnabled))`.
  A `Panel` draws into the content area with the `Ui` context. A module can replace its generated settings page with
  `createSettingsPanel()` (use `SettingsView.draw(ui, settings(), ...)` inside it to keep the generated rows).

## Mixins

One config per module: `vibeyclient.<id>.mixins.json`, package `dev.henny.vibeyclient.module.<id>.mixin`, plugin
`dev.henny.vibeyclient.mixin.VibeyMixinPlugin`. Put client mixins in `"client"`, common ones in `"mixins"`.
The plugin skips all mixins of a module when its conflicting standalone mod is installed (`ModConflicts`).
New conflicts: add them to `ModConflicts` (read before the game starts) – `conflictingMods()` uses it by default.
The lyrics config is not listed in `neoforge.mods.toml` and excluded from the NeoForge jar.

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
