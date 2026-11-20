# Vibey Client UI

How the client's menus are built and how a module plugs into them. Every menu is a **vui document**
(`src/main/resources/assets/vibeyclient/vui/**`, Vue-like `.vui` files) bound to one small Java controller. The design
(principles, screen map, previews, contracts) is [UI-NEW.md](UI-NEW.md); the library is [libs/vui](../libs/vui/README.md).

> See also: [UI-NEW.md](UI-NEW.md) (design + contracts) · [MODULES.md](MODULES.md) (module API) ·
> [HUD.md](HUD.md) (HUD editor) · [vui dev loop](../libs/vui/docs/dev-loop.md)

## Rules

- **Less text.** Labels ≤ 3 words (module names ≤ 12 characters), one sentence at most in a tooltip, no paragraphs.
  Details go into `setting.<key>.desc` (shown on hover). `checkI18n` keeps both languages complete.
- **One primary action per view** (purple), everything else ghost, icon or row.
- **Icons first:** 9 × 9 pixel icons from `vui/icons.json` (`<ico name="gear"/>`), label in the tooltip where space is
  short.
- **427 × 240 GUI px first** (GUI scale 3 at 1280 × 720), English and German; wider screens get more columns.
- **Keyboard:** Tab / Shift+Tab move focus, Enter / Space activate, Esc steps back (key capture → field → popover →
  page → search → close). Typing anywhere in the menu searches.
- **Look:** Vibe tokens from `vui/theme.json` (`$brand`, `$brand2`, `$surface`, `$line` …), square corners, 1 px lines,
  150 ms `out` motion; the global *Animations* switch turns motion off (`VuiMc.animations`).
- **Code:** one controller class per document, short single-purpose files, no code comments – explanations live here.

## Packages

| Package | Job |
|---|---|
| `ui/vui` | glue: `VibeyVui` (translator from `Lang`, `theme.json`, animations switch, dev controllers), `Menus` (all ways into the menu), `ClassicUi` (switch back to the old screens), `SyncedScreen` (base screen: UI scale, pause like the parent, re-sync every 5 ticks), `WatchedState`, `HeadTextures` (player faces as vui textures), `HostGfx` (`Gfx` for code that draws into a vui screen), `DevUiCommands` |
| `ui/vui/settings` | the settings adapter: `Setting` → row descriptor for `components/setting-row` (`Rows`, `NumberRows`, `ChoiceRows`, `OptionRows`, `OtherRows`, `SettingRow`, `SettingValue`, `PageSections`) |
| `ui/vui/menu` | the Vibey menu: `MenuScreen`, `MenuController` (facade over the parts below), `ModuleGrid`/`ModuleTile`/`ModuleSwitch`, `ModulePage`, `QuickSettings`, `Connection`, `BugInfo`, `NoticeList`, `Places`, `ModuleLook` |
| `ui/vui/friends` | friends drawer: `FriendsPanel`, `Account`, `FriendRow` (data from the `FriendsDrawer` service) |
| `ui/vui/corner` | title-screen corner icons and the pause row: `Decorations`, `Decor`, `TitleCorner`, `PauseRow`, `CornerEntry`, `ProxyButton`, `Bounds` |
| `ui/vui/onboarding` | first-start welcome: `Onboarding`, `OnboardingController`, `KeyItem`, `Feature` |
| `ui/vui/toast` | toasts: `VuiToasts` (the `Notify` sink), `ToastRequest` |
| `ui/vui/dialog` | `Confirm.ask(...)` – the confirm modal |
| `module/social/vui` | `SocialDrawer` (the `FriendsDrawer` service), `AccountScreen` + `AccountController` + `PrivacyRows` |
| `module/cosmetics/vui` | wardrobe: `WardrobeVui`, `WardrobeController`, `Stage` (3D preview), `Thumbs`, `ItemList`, `Slots` |

## Screens

| Screen | Document | Opened by |
|---|---|---|
| Vibey menu | `menu/shell` (+ `header`, `module-grid`, `module-page`, `quick-settings`, `about`, `friends`) | Right Shift, title corner *Vibey*, pause row *Vibey*, `Menus.open/openModule/openFriends/openQuick/openAbout` |
| Title corner | `title/corner` | drawn over the vanilla title screen (`Decorations.title`) |
| Pause row | `pause/row` | drawn over the vanilla pause screen (`Decorations.pause`) |
| Onboarding | `onboarding/onboarding` (+ 3 steps) | once before the first title screen (`TitleHook`), *Intro* in About |
| Account | `social/account` | the account icon in the friends drawer, the social sidebar |
| Wardrobe | `wardrobe/wardrobe` (+ rail, grid, preview) | title/pause *Wardrobe*, header shirt icon, cosmetics page *Open wardrobe* |
| Toasts | `toast/stack` | `Notify.*` from any thread |
| Confirm | `dialog/confirm` | `Confirm.ask(icon, title, text, yes, danger, onYes)` |
| Emote wheel, wheel editor, social sidebar, media/clips/multiplayer screens, HUD editor | still immediate mode (`Ui`/`Widgets`, `CardScreen`) | unchanged |

### Menu

`MenuScreen` hosts `menu/shell` with a `MenuController`. The controller is a thin facade: its public fields are the
`State`s of small parts (`ModuleGrid`, `QuickSettings`, `FriendsPanel`, `NoticeList`) so the documents keep the flat
names of the contract, and its methods delegate. `sync()` runs every 5 ticks (`SyncedScreen`) and keeps switches,
values, lists and the open page in step with the game; `document().refresh()` re-reads plain getters.

- **Grid:** one `ModuleTile` per visible module (`<id>.name`, icon from `ModuleLook.icon`, accent colour, switch =
  `ModuleSwitch`, a `State` that writes through to `ModuleManager.setEnabled`). Tooltip = `<id>.summary`.
  `ModuleCategory.CREATOR` modules sit behind the *Creator tools n* chip. Search (`query`) matches the name, the
  summary and every setting label; a setting hit shows *› label* on the tile.
- **Module page:** `ModulePage` = bar (back, icon, name with the description as tooltip, notice chips, the primary
  action, the switch) + sections. Notices: why the module cannot run, *Disabled by this server* (`ServerBadge`) and
  `VibeyModule.notices()`.
- **Quick settings** (gear): language, menu size, animations, online, menu key; *More*: Vibe server, *Bug report
  info → Copy* (`BugInfo`), and *Classic menus* while the classic UI is on. Announcements (`Announcements.active()`)
  are listed on top with *open link* and *hide*.
- **About** (click the wordmark): version, website / source / bug report / licenses, *Intro*.
- **Friends drawer:** filled from the `FriendsDrawer` service the social module registers while it is on and online:
  me + status, requests chip, online friends with hover actions (invite, join, chat), *Offline n*, *+ Add*, account
  icon. Without the service the drawer shows *Online is off* / *Not signed in* with one button.

### Title corner and pause row

The vanilla title and pause screens stay vanilla. `Decorations` builds the corner/row document when the screen
initialises and draws it after the screen (`ToastHooks.addScreenOverlay`), so tooltips are never covered by the title
logo. Input stays vanilla: one invisible `ProxyButton` (a `VibeyButton` that draws nothing) per entry sits exactly on
the drawn button, so clicks, Tab, Enter and narration work like any vanilla widget; the focused proxy's id is passed
to the document as `focus`. Entries come from `MenuEntries` as before. Position: the free corner (top right → top left
→ bottom left) and below the vanilla pause buttons (above, or icons only in the corner when there is no room).

### Toasts

`VuiToasts` is the `Notify` sink: requests from any thread are queued and pushed into a vui `ToastStack` with the
`toast/stack` template on the render thread. Kinds: info, ok, warn, error, invite (social toasts with an action). A
toast with an action (`Notify.Toast.action`, or the first button of `ToastStack.show(toast, buttons, ms, key)`) gets
one button; `ToastStack.dismiss(key)` still removes keyed toasts. Bottom right, hover pauses the countdown, click
dismisses. Player-UUID icon keys become head textures.

### Settings adapter

Module pages are generated from the settings API (`docs/MODULES.md`):

| Setting | Row kind | Notes |
|---|---|---|
| `bool` | `toggle` | |
| `intRange` / `floatRange` | `slider` | `display` formats unit / decimals / percent (`0..1` floats) |
| `intRange(...).field()` | `text` | parsed on change |
| `choice` | `choice` | segmented when ≤ 4 options of ≤ 6 characters, else a select; labels `<label>.<value>` |
| `OptionSetting` | `choice` | options from a supplier, value read/written by lambdas (not stored) – e.g. nametags *My style* |
| `string` | `text` | |
| `color` | `color` | presets from `ColorSetting.presets()` |
| `keybind` | `key` | rebinding writes the vanilla mapping; `conflict` names other mappings on the same key |
| `action` | `action` | the first non-advanced action is the page's primary button (`<label>.action`), the others are rows |

Sections: settings without a section first (untitled), then one card per `.section(...)` in order of their first
setting. Keybinds without a section join the untitled card, or a card titled with the module name. `.advanced()`
settings go behind *More n*. `.visibleWhen(...)` rows are left out (the page is rebuilt when visibility changes).
`.serverRule(key)` rows dim with a lock (`blocked`). Custom `createSettingsPanel()` pages are **not** shown in the new
menu – port their content to settings (`OptionSetting`, actions) or to an own document.

### Wardrobe

`WardrobeVui` (a `SyncedScreen`) hosts `wardrobe/wardrobe`. Thumbnails are cosmetico's thumbnail textures registered
as vui textures (`Thumbs`); the 3D player is drawn by the host into `doc.ref("stage")` after the document (`Stage`
wraps the `AvatarPreview` service). Click = pick (tries the item on, plays an emote), double click or the primary
button = wear / take off, drag on the stage = rotate (`PreviewView.mouse`), double click on the stage = reset. The
emote wheel and the wheel editor are still the old screens.

## Classic UI

The old screens (`VibeyScreen` with tabs, `TitleButtons`, the old pause row, `OnboardingScreen`, the old toast
stack, `WardrobeScreen`) are still in the jar and come back with `-Dvibeyclient.classicUi=true` or the hidden global
setting `core.classicUi` (shown under *More* in the quick settings only while it is on). What can be deleted once the
new UI is approved: [UI-NEW.md → Deleting the classic UI](UI-NEW.md#deleting-the-classic-ui).

## Writing UI for a module

- Settings: declare them (`MODULES.md`); labels ≤ 3 words, `.desc` one sentence. That's all for most modules.
- A button on the page: `action("key", run)`; the first one is the page's primary.
- Dynamic choices: `settings().add(new OptionSetting("key", read, write, choices))`.
- An own screen: a `.vui` document under `assets/vibeyclient/vui/<module>/` + a controller + a `SyncedScreen`
  subclass (see `AccountScreen`). Declare the controller fields in a `<contract>` block.
- Title/pause entries: `MenuEntries.register(...)` as before; give each a pixel icon name from `vui/icons.json`.
- Toasts: `Notify.info/success/warn/error(title, body)`; ≤ 2 words body, subject as title.
- Confirmations: `Confirm.ask(...)`.

## Development and tests

- **Dev loop:** `runClient` reads `.vui` files from the source folders. With `-Dvibeyclient.devDriver=<port>` the
  DevDriver adds `menu [module|-] [panel]`, `title`, `pause`, `onboarding`, `lang en|de`, `uiscale X2`. Then
  `scripts/vui-dev.sh loop shot.png` (reload + errors + screenshot, ~0.4 s), `click <id>`, `tree`, `errors`.
- **Ids for tests and the bridge:** `open-about`, `open-wardrobe`, `open-hud`, `open-friends`, `open-quick`,
  `close-menu`, `tile-<module>`, `page-back`, `page-primary`, `page-switch`, `page-more`, `creators`,
  `dismiss-<notice>`, `act-<setting id>`, corner/pause `e-<entry>`.
- **Gametests** (`./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=...`):
  `VibeyUiGameTest` (every screen EN + DE at 427 × 240 and 640 × 360, search, onboarding steps, toast, confirm,
  real clicks: title corner → menu, tile → page, Esc, tile switch, gear at GUI scale 2/3/4, menu size, pause row),
  `ToastGameTest`, `WardrobeUiGameTest`, `NoticesGameTest` (needs the mock). Screenshots are copied to
  `build/review/ui-new/`.
- **Click self-test on other versions:** `dev/ClickTest` (`-Dvibeyclient.clicktest=<dir>`) clicks the gear, the title
  corner button and a module tile of the new menu through the real mouse handler.
- Mouse buttons and keys: vui gets GLFW codes on every version (26.3 SDL codes are mapped in the vui backend);
  `MappingKeys` converts vanilla key mappings to and from vui keybind values (`-100 + button` for mouse buttons).
