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
- **Look:** theme tokens (`$brand`, `$brand2`, `$on-brand`, `$surface`, `$line` …, see [Themes](#themes)) – never a
  hex colour in a document; square corners, 1 px lines,
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
| `ui/vui/theme` | UI themes: `UiThemes` (theme files + accent → `vui.useThemes`, live switch), `Accent` (custom accent → `brand`, `brand-hover`, `on-brand`), `Contrast` (WCAG maths), `ColorVision` (colour-blind simulation), `ThemeAudit` (the contrast/status checks), `DevThemeCommands` |
| `ui/vui/dialog` | `Confirm.ask(...)` – the confirm modal |
| `module/social/vui` | `SocialDrawer` (the `FriendsDrawer` service), `AccountScreen` + `AccountController` + `PrivacyRows` |
| `module/cosmetics/vui` | wardrobe: `WardrobeVui`, `WardrobeController`, `Stage` (3D preview), `Thumbs`, `ItemList`, `Slots`; emote wheel: `EmoteWheelVui` + `WheelController`, wheel editor: `WheelEditorVui` + `WheelEditorController`, shared `WheelGeometry` / `WheelFrame` / `WheelSlices`, `EmoteRows`, `WheelDevCommands` |

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
| Emote wheel | `wardrobe/wheel` (+ `wheel-ring`) | emote wheel key (hold), `WardrobeUi.openWheel` |
| Wheel editor | `wardrobe/wheel-editor` (+ `wheel-ring`, `components/emote-card`) | wardrobe wheel icon, empty slot click in the wheel, `WardrobeUi.openWheelEditor` |
| Social sidebar, media/clips/multiplayer screens | still immediate mode (`Ui`/`Widgets`, `CardScreen`) | unchanged |

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
  action, the switch) + an optional status card (`VibeyModule.statusCard()` → `StatusCard{kind ok/warn/error/off,
  label, text, detail}`, `components/status-card`, id `page-status`; devlink shows *Listening · 127.0.0.1:port · n
  Blockbench* there) + sections. Notices: why the module cannot run, *Disabled by this server* (`ServerBadge`) and
  `VibeyModule.notices()`.
- **Quick settings** (gear): language, menu size, theme, animations, online, menu key; *More*: own accent (+ accent
  colour while it is on), Vibe server, *Bug report info → Copy* (`BugInfo`), and *Classic menus* while the classic UI
  is on. Search also matches these rows: a hit shows a *Settings › label* tile (`qhit-<setting key>`, e.g. `qhit-theme`) first in the
  grid; clicking it opens the quick settings (and *More* if needed) with that row highlighted (`quickFocus`). Announcements (`Announcements.active()`)
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
one button; `ToastStack.dismiss(key)` still removes keyed toasts. Bottom right. Player-UUID icon keys become head
textures.

Toasts never block the screen under them: only their action button takes a click (`ToastStack.takesClick`, used by
`VuiToasts.click` and vui's `ToastHost`), everything else – the toast body, the gaps of the stack – passes the click
to the screen (e.g. the gallery's header buttons stay usable while *Image copied* is shown). Hovering pauses the
countdown only for toasts with a button, so a plain toast under a resting cursor still goes away on time.

### Settings adapter

Module pages are generated from the settings API (`docs/MODULES.md`):

| Setting | Row kind | Notes |
|---|---|---|
| `bool` | `toggle` | |
| `intRange` / `floatRange` | `slider` | `display` formats unit / decimals / percent (`0..1` floats) |
| `intRange(...).field()` | `text` | parsed on change |
| `choice` | `choice` | segmented when ≤ 4 options of ≤ 6 characters, else a select; labels `<label>.<value>` |
| `OptionSetting` | `choice` | options from a supplier, value read/written by lambdas (not stored) – e.g. nametags *My style*; `.preview(text, valueToEffect)` adds a live text before the control with a vui `text-effect` (*My style*: your name in the chosen style, `namestyle(<id>)`) |
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
stage is painted from `Document.beforeOverlays`, i.e. after the document tree but before popovers and tooltips, so a
tooltip over the stage is drawn above the player.

### Emote wheel and wheel editor

`EmoteWheelVui` (does not pause, no vanilla background) hosts `wardrobe/wheel`: 8 square slices on a circle around
the screen centre (slot 0 at the top, clockwise), the hovered emote's name in the middle, a small direction dot and
one hint line above the wheel. `WheelGeometry.fit` sizes it so it stays between the top edge and the hotbar/hearts
(44 GUI px) on every GUI scale; the centre is the screen centre, so the cursor MC puts there on open sits in the dead
zone (30 % of the radius) and nothing is preselected. Selection is by direction: move the mouse towards a slice →
it highlights (`hovered`), release the key → it plays and the wheel closes. A tap (released within 250 ms without
pointing) keeps it open: click plays; left click on an empty slice opens the editor; right click / Esc cancel.

`WheelEditorVui` hosts `wardrobe/wheel-editor`: bar (back, title, *n/8*, *Clear*, primary *Done*), the same
`wheel-ring` on the left (click = select a slot, right click = clear it, numbers on the slices) and the emotes as
`emote-card`s on the right (click = put on the selected slot; an emote already on another slot swaps; the selection
moves to the next empty slot). The old `EmoteWheelScreen` / `WheelEditorScreen` stay for the classic UI.

## Themes

Feature 28. *Theme* in the quick settings: `Vibe dark` (default, `vui/theme.json`), `Light`, `High contrast`,
`Deuteranopia`, `Protanopia`, `Tritanopia`; *More → Own accent* + colour replaces `brand`/`brand-hover`/`on-brand`.
Each theme is a vui theme JSON next to the base file (`vui/theme.<id>.json`, only the tokens it changes);
`UiThemes.apply` calls `vui.useThemes([theme.json, theme.<id>.json], accentTokens)` and reloads every open document,
so menus, HUD editor chrome, toasts, the emote wheel and world panels (music panel, video screens) change live.
Documents use tokens only (`$on-brand` for text on `$brand`, `$bg` for text on `$danger`). HUD widget contents are
still drawn by the old immediate-mode `ui/Theme` and keep the dark look.

| Theme | What changes |
|---|---|
| Vibe dark | the Vibe look; `brand` #7759ff (was #8b6cff: white button text now passes AA), darker hover, `dim` #80808b |
| Light | light surfaces, dark text, deeper purple/teal/red/amber for AA on white |
| High contrast | pure black, white text, brighter lines, light purple with black button text |
| Deuteranopia / Protanopia | status colours from the Okabe–Ito set: ok = blue, warn = yellow, danger = pink / light red |
| Tritanopia | magenta brand, teal ok, yellow warn, red danger |

Checks (`ThemeAudit`, JUnit `ThemeContrastTest`, gametest `PolishGameTest`): WCAG AA 4.5:1 for every text token on
every surface it is used on (`text`, `muted`, `dim` on panel/surface/surface2/surface3, `brand2` captions,
`on-brand` on `brand`/`brand-hover`, `bg` on `danger`, status colours on surface), 3:1 for non-text (`brand` icons and
borders, `dim` on surface3). The colour-blind sets are checked by simulating the vision (Machado 2009 matrices) and
measuring ΔE between ok / warn / danger: ≥ 60 in each set under its own condition (the dark theme: 32–48). A custom
accent always gets black or white text, whichever contrasts more (≥ 4.58:1 by construction), and is darkened (light
themes) or lightened (dark themes) in 4 % steps until it keeps 3:1 against `surface3` (e.g. teal #13a387 on Light
becomes a deeper teal), so icons and borders in the accent stay visible.

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
  `ToastGameTest`, `WardrobeUiGameTest`, `NoticesGameTest` (needs the mock), `PolishGameTest` (every theme EN + DE:
  menu grid, waypoints page, HUD editor + live contrast check; own accent; settings search hit; devlink card;
  shots in `build/review/polish/`). Screenshots are copied to
  `build/review/ui-new/`.
- **Click self-test on other versions:** `dev/ClickTest` (`-Dvibeyclient.clicktest=<dir>`) clicks the gear, the title
  corner button and a module tile of the new menu through the real mouse handler.
- Mouse buttons and keys: vui gets GLFW codes on every version (26.3 SDL codes are mapped in the vui backend);
  `MappingKeys` converts vanilla key mappings to and from vui keybind values (`-100 + button` for mouse buttons).
