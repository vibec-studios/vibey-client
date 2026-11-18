# Vibey Client UI

How the client looks and how a module plugs into it. Code: `dev.henny.vibeyclient.ui` (widgets, menu shell),
`ui.screen` (module grid, module pages, General, About), `ui.title` (title screen buttons, onboarding),
`client/PauseMenu` (pause row). Tokens: `ui/Theme` (SPEC §4).

> See also: [MODULES.md](MODULES.md) (module API) · [SPEC §4](SPEC.md#4-design-system-vibe) (design tokens) ·
> [docs index](README.md)

## Design rules

- **Same product as the Vibe launcher:** deep black (`BG`, `PANEL`, `SURFACE*`), square corners, 1 px lines
  (`LINE`, `LINE2`), purple `BRAND` for "active/primary", teal `BRAND2` for small labels and "ok". No rounded
  corners, no gradients – the only soft thing allowed is a faint brand glow (stacked translucent rects).
- **Text:** Minecraft font for everything you read. `PixelFont` (blocky 5-row display font) only for the "VIBEY"
  wordmark and short brand words. Small uppercase captions (`Widgets.caption`) for section titles, dim text for
  secondary info. Every string in `i18n/en_us` **and** `i18n/de_de`; German is ~30 % longer – check it fits.
- **Crisp at every GUI scale:** draw with `Gfx` fills; icons are 9 × 9 pixel maps (`Icons`), the seal is rendered
  at screen-pixel density (`Seal`). Don't draw scaled-down PNGs.
- **Motion:** 150 ms, `cubic-bezier(0.2, 0.8, 0.2, 1)` (`Anim`), fade/slide 6–8 px. Never block input while
  something animates; no per-frame allocation in hot paths. `GlobalSettings.animations` off = no motion.
- **States:** hover and keyboard focus look the same (`ui.focusedLast()` → draw it like hover); otherwise the `Ui`
  context draws a 1 px brand focus ring. Active/selected = brand bar or underline. Disabled = `DIM`.
- **Layering:** since 1.21.6 text is drawn after shapes inside one layer – call `g.layer()` before anything that
  must cover text (overlays, popups, tooltips).
- **Smallest screen:** 427 × 240 GUI px (1280 × 720 at GUI scale 3). Everything must fit there; long content
  scrolls (`ScrollArea`), long labels clip with "…" and get a tooltip (`ui.tooltip`).

## Screens

| Screen | Class | Notes |
|---|---|---|
| Title | vanilla `TitleScreen` + `ui/title/TitleButtons` | The title screen stays vanilla. Vibey adds a row of small square icon buttons (entries of `MenuEntries.Place.TITLE`: wardrobe, friends with unread badge, media, Vibey menu) in a free corner: top right, else top left, else bottom left. Tooltips on hover, Tab/Enter work. |
| Onboarding | `ui/title/OnboardingScreen` | Once before the first title screen (`TitleHook` via `mixin/client/ScreenSwapMixin`, `GlobalSettings.onboarded()`), reopen from About. Skipped under Fabric's client gametest runner and the smoke test. |
| Vibey menu | `ui/VibeyScreen` | Right Shift. Tabs from `VibeyTabs`: *Modules* (grid → module pages), *General*, *Account* (social), *About*. Tab/Shift+Tab focus, Enter clicks, Ctrl+Tab switches tabs, Esc: cancel key capture → leave text field → module page back to the grid → clear search → close. Typing anywhere searches modules and settings. |
| Pause row | `client/PauseMenu` | One row under the vanilla buttons; above them or icon-only in the corner when the GUI is too small. |

Global *UI scale* (`GlobalSettings.uiScale`: game / 2× / 3× / 4×) sets the window GUI scale while a Vibey screen is
open (`UiScale.apply/restore`) – whole numbers only, so nothing gets blurry.

## Settings: module grid and pages

Built like the mod screens of the big clients, in the Vibe look (`ui/screen/ModulesPanel`, `SettingsView`):

- **Grid:** search bar (modules, descriptions and setting labels; a hit on a setting shows "› setting name" on the
  tile, a hit on a global setting adds a *General* tile), then one tile per module: icon, name, one or two lines of
  description, on/off switch, accent bar while on. Modules of category `CREATOR` (Blockbench link) sit in a
  collapsed *For creators* area under the grid.
- **Module page:** toolbar with *‹ Modules / name* and the on/off switch, then the intro (big icon, description,
  the module's buttons = its non-advanced `ActionSetting`s labelled `<label>.action` or the label, the first one
  primary; then `notices()`, e.g. parts provided by the standalone Vibey Lyrics), then the settings as cards:
  untitled first, then one card per `.section(...)`, three or more plain switches in two columns, the keys
  (key caps, conflicts in amber with a tooltip), and at the bottom the collapsed *Advanced · for creators* area with
  every `.advanced()` setting. Enums with up to four short values are segmented controls, else ‹ value ›.
- **General tab** (`GeneralPanel`): language, menu size, animations, online features (+ connection status with a
  tooltip), the menu key; advanced: Vibe server address, and technical info (version, game, server, connection,
  protocol, settings folder) with *Copy* for bug reports.
- **Account tab** (social `AccountPanel`): profile card (head, name, signed in or not, *Open friends* / *Sign out* or
  *Sign in*), status, privacy, devices, blocked players. Server address, account id, sign-in method and device
  addresses only in the advanced area.

Rule of thumb for texts: say what the player gets, no jargon; anything only a developer or creator needs is
`.advanced()`.

## In the world

- **Nametag icons** (module `nametags`): glyphs of the bitmap font `vibeyclient:icons` (`assets/vibeyclient/font/icons.json`,
  8 × 8 cells, art drawn in 7 × 7 like the vanilla font, generated by `tools/gen-nametag-icons.mjs`): U+E000 seal,
  U+E001 staff, U+E002 supporter, U+E003 creator. `NameIcons.glyphs(...)` gives a component for other UIs. They are
  crisp at every GUI scale because they are font pixels.
- **Wardrobe:** community items show *by <creator>* under the rarity in the preview column; items for a newer client
  are greyed out with a lock and *Update Vibey Client*. New items arrive with one *New cosmetics available* toast
  per batch.

## HUD editor

`ui/hud`: the HUD editor and the widget API for every module – drag, 8 resize handles (Shift = free), snapping
with guides and distance readouts, multi-select, undo/redo, right-click menu, style popover (global + per widget),
widget library, templates, share codes and a first-run intro. Open it with `HudLayout.openEditor(parent)`.
**Everything about it, and how to add a widget: [HUD.md](HUD.md).**

## Extension points

**Tabs in the Vibey menu** (in `onLoad`):

```java
VibeyTabs.register(new VibeyTabs.Tab("wardrobe", "cosmetics.tab.wardrobe", 10, WardrobePanel::new,
        () -> isEnabled(), Icons.SHIRT));
```

Conventional ids: `wardrobe`, `friends`, `account`. Order: Modules 0, About 1000. The icon is used when the window
is too narrow for all labels. A `Panel` draws into `(x, y, w, h)` every frame with the `Ui` context; it may
override `key(int)`, `typed(int)`, `onShow()`, `onHide()`.

**Title screen buttons and pause menu entries** (`ui/MenuEntries`, same id replaces):

```java
MenuEntries.register(MenuEntries.Place.TITLE, new MenuEntries.Entry("media", "media.menu.title",
        Icons.NOTE, 30, MenuEntries.whenEnabled("media"), parent -> ClientCompat.openScreen(new MediaScreen(parent))));
MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("friends", "social.menu.friends",
        Icons.FRIENDS, 20, MenuEntries.whenOnline("social"), parent -> ..., () -> unreadCount()));   // badge
```

On the title screen every entry is a 20 × 20 icon button (the label is its tooltip and narration), so give it a
clear `Icons` icon. Core orders – title: wardrobe 10, friends 20, media 30 (free for the media module), Vibey menu
100 (last). Pause: Vibey 0, wardrobe 10, friends 20. The core registers defaults for `wardrobe` / `friends` (open the tab
with that id, else the module's settings page); the cosmetics module replaces `wardrobe` with its full-screen
wardrobe and social replaces `friends` with the sidebar. There is no `host` pause entry: the multiplayer module puts
*Host World* in place of vanilla's *Open to LAN*. Entries of disabled modules are hidden via `visible`.

**Player preview** – `api/service/MenuPreview` (the cosmetics module registers `TitlePreview`, the player with all
equipped cosmetics, drag to rotate). The vanilla title screen does not show it; any Vibey screen that wants the
player model can look it up:

```java
Services.register(MenuPreview.class, new MenuPreview() {
    public void render(Gfx g, int x, int y, int w, int h, int mouseX, int mouseY, float delta) { /* player in the box */ }
    public boolean mouseClicked(double x, double y, int button) { return true; }          // start drag-rotate
    public boolean mouseDragged(double x, double y, int b, double dx, double dy) { yaw += dx; return true; }
});
```

Without one, show the seal (`Seal`) instead.

**Status numbers** – the social module registers `api/service/SocialStatus` (`onlineFriends()`, `badge()`); the
title *Friends* button shows the badge, the General tab "Online · 3 friends online". Backend reachability comes from
the public `GET /api/v1/meta` (`ui/title/BackendStatus`, refreshed when the title screen or the General tab opens),
only while online features are on.

## Widgets cheat sheet

Immediate mode (`Ui` + `Widgets`), call every frame, keys must be stable objects (newer helpers: `segmented`,
`keycap`, `disclosure`, `card`, `iconTile`, `drawToggle` for rows that are clickable as a whole):

```java
if (Widgets.button(ui, this, x, y, 80, 16, label, Widgets.Style.PRIMARY)) save();
if (Widgets.toggle(ui, setting, x, y, setting.on(), true)) setting.toggle();
float v = Widgets.slider(ui, key, x, y, 100, value);
int d = Widgets.cycler(ui, key, x, y, 120, valueLabel);          // -1 / +1
Widgets.textField(ui, key, x, y, 160, text, 64, s -> text = s);
ui.tooltip(key, x, y, w, h, "Shown after 350 ms");
if (ui.clicked(x, y, w, h)) ...;  boolean hot = ui.focusedLast() || ui.hover(x, y, w, h);
int top = scroll.begin(ui, x, y, w, h); ...; scroll.end(ui, bottom);
Icons.draw(g, Icons.SHIRT, x, y, Theme.MUTED);   PixelFont.drawExtruded(g, "VIBEY", x, y, 3, Theme.TEXT, Theme.BRAND);
```

On vanilla screens use `VibeyButton` (a real widget: Tab/Enter, narration). The seal: `new Seal(withIce)`,
`seal.draw(g, x, y, unit, hovered)`, `seal.close()` in `removed()`.

## Testing

`./gradlew :fabric-1.21.11:runClientGameTest` runs `VibeyUiGameTest`: vanilla title with the corner buttons (+ hover
tooltip), module grid, a module page, the advanced area closed and open, General, Account, search, About,
onboarding and the pause row, at GUI scales 2 / 3 and 1280 × 720 / 1920 × 1080, English and German. It also clicks
through the real mouse input (title button → menu, tile → module page, Esc → grid).
Screenshots: `versions/fabric-1.21.11/build/run/clientGameTest/screenshots/ui_*.png` – look at them.

**Click self-test (every version, both loaders).** The client gametest only runs on 1.21.11, so clicks on the other
versions are checked by `dev/ClickTest`: `./gradlew :fabric-26.3:runClient -Dvibeyclient.clicktest=<outdir>` (NeoForge:
env `VIBEYCLIENT_CLICKTEST=<outdir>`). It feeds real mouse presses/releases into the game's `MouseHandler` (through
`mixin/client/MouseHandlerAccessor`, so GUI scaling and the native button numbers are included) and checks an
immediate-mode button, a right-click area and a `VibeyButton` on a test screen, then a tab of the real Vibey menu, the
Vibey corner button on the vanilla title screen, a module tile (→ its page) and the key code round trip. It
logs `[vibeyclient-clicktest] PASS|FAIL <check>` and a `SUMMARY`, writes `<outdir>/clicktest.txt` plus screenshots
(`clicktest-screen.png`, `menu-before.png`, `menu-after.png`, `title-button-after.png`, `module-page.png`) and quits. Run it on 26.3 after touching input code.

Mouse buttons: our code (`Ui`, `click(x, y, button)`, previews) uses GLFW numbers (`Keys.MOUSE_LEFT` = 0, right 1,
middle 2). 26.3 (SDL) reports 1 left, 2 middle, 3 right; `BaseScreen` and `ToastHooks` convert with
`Keys.mouseFromNative`, so never compare `MouseButtonEvent#button()` directly.

Keys: screens compare GLFW codes (`Keys.ESCAPE`, `TAB`, ...). 26.3 reports SDL scancodes; `BaseScreen#key` and the
screen key listeners get `Keys.fromNative(code)` (identity before 26.3; keys without a GLFW constant arrive as
`Keys.NATIVE_ONLY + code`). `ClientCompat.matches` and the keybind capture convert back with `Keys.toNativeAny`.
