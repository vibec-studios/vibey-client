# New UI (vui documents)

The redesign of every Vibey Client screen as vui documents. Complaint we answer: *"the whole UI has far too much
text and is far too complicated"*. Result: one calm menu, icons + short labels, details on hover, everything in
≤ 2 clicks, 80 % fewer words.

> Sources: `src/main/resources/assets/vibeyclient/vui/**` (documents, no comments inside, everything is explained
> here) · `i18n/{en_us,de_de}/ui.json` · previews `docs/ui-preview/*.png` · sample controller state
> `docs/ui-preview/state/*.json` · rules [REBUILD.md](REBUILD.md) · library [libs/vui](../libs/vui/README.md) ·
> today's UI [UI.md](UI.md)

## Principles

1. **Labels ≤ 3 words.** Buttons, rows, tiles, chips. Module names ≤ 2 words / 12 characters (they share a tile with
   a switch).
2. **No paragraphs.** Anything longer than a label is a tooltip of one short sentence – or it is cut.
3. **One primary action per view.** Exactly one purple button: module page (*Edit crosshair*), wardrobe (*Wear*),
   onboarding (*Next*), HUD editor (*Done*), dialog (*Reset all*). Everything else is ghost, icon or row.
4. **Icons first.** Every place, tab and tool has a 9 × 9 icon; the label is its tooltip where space is short
   (header, rail, title corner, pause row).
5. **Hover for details.** Module tile → summary; setting label → what it does; friend → what they play; wardrobe
   item → name + rarity; locked row → why. Nothing explains itself in the layout.
6. **Advanced behind one "More" chip.** Each module page and the quick settings have one `⚙ More n` chip at the
   end. No disclosures, no "for creators" wording, no nested cards.
7. **≤ 2 clicks.** Every setting: open menu → tile → row. Every place: one header icon. Global settings: one gear.
8. **Direct manipulation.** Switches on tiles, keys rebindable where they are shown (onboarding too), HUD widgets
   dragged / wheel-resized with a bar under the selection, wardrobe player dragged to rotate, double click wears.
9. **Vibe look kept.** Deep black, square corners, 1 px lines, purple `$brand` = active/primary, teal `$brand2` =
   small captions and "ok", 150 ms `out` motion. Tokens in `vui/theme.json`.
10. **427 × 240 first.** Everything fits the smallest screen (GUI scale 3 at 1280 × 720) in English and German;
    640 × 360 just gets more columns.

## Numbers

Words in the menu texts the new `ui.json` replaces (core.json without `notice.*`, wardrobe.json without the emote
wheel, social.json `account/friends/status/conn/tab/pause`, cosmetics.json `slot/locked/new`):

| | Keys | Words EN | Words DE | Chars EN |
|---|---|---|---|---|
| Before | 379 | **1401** | 1290 | 7910 |
| After (`ui.json`) | 153 | **281** | 266 | 1526 |
| Change | −60 % | **−80 %** | −79 % | −81 % |

Average string: 1.84 words (EN), 1.74 (DE). Only tooltips have more than 3 words (max 6 EN / 7 DE).

| Area | Words before | Words after |
|---|---|---|
| Onboarding | 143 | 38 |
| HUD editor chrome | 434 | 37 |
| Status lines + tooltips | 120 | 30 |
| General tab → quick settings | 32 + 95 | 26 |
| About | 79 | 13 |

**Words on the first screen** (Vibey menu, module grid): today 60 words for 6 modules (10 per module: name +
two-line description, 5 tabs, search hint, creators hint). New: 26 words for 16 modules (1.6 per module).

## Screen map

```
Title screen ── corner icons ─┬─ Wardrobe ──────────────── wardrobe/wardrobe
Pause menu ──── Vibey row ────┤─ Friends (badge) ───────── opens menu with the friends drawer
Right Shift ──────────────────┤─ Media (module entry)
                              └─ Vibey ──┐
                                         ▼
menu/shell ─ header: [VIBEY]=About · [Search] · Wardrobe · HUD · Friends · Settings · Close
   ├─ module grid (tiles: icon, name, switch; hover = summary; search hits "› Zoom")
   │     └─ click tile → module page (sections, rows, one primary, "More n")
   ├─ popover: quick settings (language, menu size, animations, online, menu key; More: server, bug info)
   ├─ popover: about (version, 4 link icons, Intro)
   └─ drawer: friends (me + status, requests chip, online friends, Offline n, + Add, Account)
HUD editor ─ hud/editor: dock + first-run key hint + selection bar
Onboarding ─ onboarding/onboarding: Welcome+language → Online? → Your keys
Overlays ─ toast/stack (bottom right) · dialog/confirm (modal)
```

| Goal | Clicks |
|---|---|
| Switch a module | menu → switch (1) |
| Change a setting | menu → tile → row (2) |
| Global setting (language, size, online, key) | menu → gear → row (2) |
| Wardrobe / HUD editor | title/pause icon (1) or menu → icon (2) |
| Chat with a friend / invite | menu → friends → friend or hover icon (2) |
| Advanced setting | menu → tile → More (2) + row |

## Documents

Ids are `vibeyclient:<path>`. Screens are opened with `vui.open(id, controller)`; parts and components are imported.

| Document | Kind | Job |
|---|---|---|
| `menu/shell` | screen | scrim + panel: header, body (grid or page), friends drawer |
| `menu/header` | part | wordmark (About popover), search, place icons, gear (quick settings popover), close |
| `menu/module-grid` | part | tile grid, empty search state, *Creator tools* chip + tiles |
| `menu/module-page` | part | page bar, section grid, *More* chip + advanced sections, empty state |
| `menu/quick-settings` | part | connection status line, global setting rows, *More* rows |
| `menu/about` | part | wordmark + version, website/source/bug/licenses icons, *Intro* |
| `menu/friends` | part | friends drawer with ready / off / signed out / error / connecting states |
| `title/corner` | overlay | icon buttons in a free corner of the vanilla title screen |
| `pause/row` | overlay | one labelled *Vibey* button + icon buttons under the vanilla pause buttons |
| `onboarding/onboarding` | screen | card with step dots, body, *Skip* / *Next* |
| `onboarding/step-welcome` · `step-online` · `step-keys` | part | the three steps |
| `toast/stack` | overlay | template for `ToastStack` |
| `dialog/confirm` | overlay | confirm modal |
| `hud/editor` | overlay | above the HUD layer in edit mode: hint, empty state, selection bar, dock |
| `hud/toolbar` · `hint` · `widget-bar` · `widget-library` · `preset-list` · `share-menu` · `context-menu` · `style-panel` (`style-box` · `style-text` · `style-size` · `style-row`) · `import-dialog` · `reset-dialog` · `server-badge` | part | dock, key hint, selection bar, widgets popover, presets popover, share popover, right-click menu, style popover, import modal, reset confirm, server badge |
| `wardrobe/wardrobe` | screen | bar + rail + grid + preview |
| `wardrobe/slot-rail` · `item-grid` · `preview-pane` | part | slot icons, item cards, player stage + primary |
| `components/ico` | component | `name` → `vibeyclient:ui/icons/<name>` (the one place that maps icon names to textures) |
| `components/icon-button` | component | 18 × 18 icon button: `icon`, `tip`, `badge`, `dot`, `active`, slot for a popover |
| `components/tile` | component | module tile: `m` |
| `components/section` | component | caption + slot |
| `components/setting-row` | component | one setting descriptor → label + control |
| `components/chip` | component | small toggle chip: `label`, `icon`, `count`, `on`, `tip` (used for *More*, filters) |
| `components/notice` | component | one-line warning chip with tooltip |
| `components/empty` | component | icon + title + optional primary (`@act`); `kind="error"` |
| `components/spin` | component | spinner + label |
| `components/friend-row` | component | head, name, hover actions, status dot |
| `components/head` | component | player head texture with fallback |
| `components/status-dot` | component | 5 px status square (online, away, busy, connecting pulse) |
| `components/item-card` | component | wardrobe thumbnail, worn/locked flag, rarity line |
| `components/choice-card` | component | big radio card (onboarding) |
| `components/step-dots` | component | clickable progress dots |
| `components/keycap` | component | static key label |
| `components/wordmark` | component | seal + VIBEY |
| `theme.json` | theme | Vibe tokens (`ThemeLoader.load`, groups `colors`/`sizes`/`times`) |

Only default token names are used, so the documents also render with the stock `Preview` and the default theme;
`theme.json` is the place for feature 28 (UI themes: light, high contrast, custom accent).

## Controller contracts

Writable fields are marked ✎ (`State<T>`, setter or non-final field – `v-model` and handlers assign them). Lists
are `ListState` or plain lists. Items may be records/beans or `Model`s. Every string the controller passes is
already translated; the keys it should use are named.

### Setting descriptor (`components/setting-row`)

Built from today's settings API, so module pages stay generated.

| Field | Type | From the Setting API |
|---|---|---|
| `id` | String | setting key |
| `kind` | `toggle` `slider` `choice` `key` `color` `text` `action` | `bool` / `intRange`,`floatRange` / `choice` / `keybind` / `color` / `string` / `action` |
| `label` | String ≤ 3 words | `setting.<key>` |
| `tip` | String, 1 sentence, optional | `setting.<key>.desc` |
| `value` ✎ | State | the setting value (write = `set`, listeners fire) |
| `min` `max` `step` | number | slider range |
| `display` | String | formatted slider value incl. unit/decimals (`4×`, `0.2s`, `120%`) |
| `options` | `[{value,label}]` | enum values, `setting.<key>.<value>` |
| `compact` | bool | true → segmented control (≤ 4 options, each ≤ 6 chars), else select |
| `conflict` | String | other binding's name, `''` if none (key rows) |
| `placeholder` | String | text rows |
| `button` | String | action label (`setting.<key>.button`) |
| `blocked` | bool | `ServerRules.isDisabled(serverRule)` – row dims, lock icon, tooltip `ui.module.blockedTip` |

Sections: `{ id, title, rows }` (`title` = `section.<key>`, `''` hides the caption). Rows hidden by
`.visibleWhen` are simply left out of `rows`. Keybinds go into the section they belong to (zoom key in *Zoom*);
modules without a fitting section put them into a section titled with their module name.

### `menu/shell` (+ header, module-grid, module-page, quick-settings, about, friends)

One controller for the whole menu.

| Field | Type | Notes |
|---|---|---|
| `view` | `'grid'` / `'page'` | |
| `panel` ✎ | `''` `'quick'` `'about'` `'friends'` | open popover/drawer (the document toggles it) |
| `query` ✎ | String | typing anywhere focuses search; controller filters `modules`/`creators`, sets `hit` |
| `status` | `online` `away` `busy` `connecting` `offline` | dot on the friends icon and in quick settings |
| `statusLabel` `statusTip` | String | `ui.status.*` (`tipOn` / `tipOff` / `tipDown`) |
| `requests` | int | badge + requests chip |
| `modules` `creators` | `[ModuleTile]` | creators = `ModuleCategory.CREATOR` |
| `showCreators` ✎ | bool | |
| `page` | `ModulePage` | the open module |
| `moreOpen` ✎ | bool | reset to false when a page opens |
| `quick` `quickMore` | `[Setting]` | `GlobalSettings`: language (compact), uiScale (compact), animations, onlineFeatures, openMenu · backendUrl (text), copy info (action) |
| `quickMoreOpen` ✎ | bool | |
| `version` | String | |
| `account` | `{ state, name, head, status ✎ }` | state: `ready` `off` `signedOut` `error` `connecting` |
| `statusOptions` | `[{value,label}]` | `ui.status.online/away/busy/invisible` |
| `friendsOnline` `friendsOffline` | `[Friend]` | |
| `showOffline` ✎ `adding` ✎ `addName` ✎ | | drawer state |

`ModuleTile`: `id, name, icon, accent ('#rrggbb'), on ✎, available, blocked, tip (<id>.summary), reason
(ui.module.installed / unavailable / failed), hit (matched setting label or '')`.
`ModulePage`: `id, name, icon, accent, tip (<id>.description), on ✎, available, notices [{text, tip}], primary
(action Setting or null: first non-advanced action), sections [Section], more [Section], moreCount`.
`Friend`: `id, name, head (texture id), status, activity, canInvite, canJoin`.

| Action | When |
|---|---|
| `open(m)` | tile click → `view = 'page'` |
| `toggled(m)` | tile switch (after `m.on` was written; enable/disable the module) |
| `back()` | page → grid (Esc does the same) |
| `run(setting)` | action rows and the page's primary |
| `close()` | close the screen |
| `openWardrobe()` `openHud()` | header icons |
| `link(id)` | `website` `source` `issues` `licenses` |
| `showWelcome()` | About → Intro (onboarding) |
| `openInbox()` `chat(f)` `invite(f)` `join(f)` `addFriend(name)` `openAccount()` | friends drawer (social module) |
| `goOnline()` `signIn()` `retry()` | drawer empty states |

Keys: `ui.menu.*`, `ui.module.*`, `ui.settings.*`, `ui.quick.*`, `ui.status.*`, `ui.friends.*`, `ui.about.*`.

### `title/corner`

| Field / action | |
|---|---|
| `corner` | `top-right` `top-left` `bottom-left` (the free corner, as today) |
| `entries` | `MenuEntries.Place.TITLE` → `[{ id, icon, label, badge, dot }]`; `id == 'vibey'` gets the brand outline |
| `activate(e)` | run the entry |

### `pause/row`

| Field / action | |
|---|---|
| `x` `y` `width` | left/top/width in GUI px (below the vanilla button column) |
| `compact` | true when the GUI is too small → icons only, label in the tooltip |
| `main` | `{ id: 'vibey', icon: 'seal', label: ui.pause.vibey }` |
| `entries` | other `MenuEntries.Place.PAUSE` entries `[{ id, icon, label, badge, dot }]` |
| `activate(e)` | |

### `onboarding/onboarding`

| Field / action | |
|---|---|
| `step` | 0..2 |
| `features` | `[{ id, icon, label (ui.onboarding.feature.*), tip }]` |
| `language` ✎ `languages` | segmented: auto / English / Deutsch (switches live) |
| `online` ✎ | choice cards |
| `keys` | `[{ id, icon, label (ui.onboarding.menu/emotes/hud), value ✎ }]` – real keybinds, rebind in place |
| `next()` `skip()` `go(i)` | `next()` on the last step finishes (`GlobalSettings.onboarded`) |

### `toast/stack`

Use as `new ToastStack(vui, vui.registry().template("vibeyclient:toast/stack"))`.

| Field / action | |
|---|---|
| `toasts` | `[{ id, kind: info ok warn error invite, icon (texture id), title, text, action }]` |
| `dismiss(t)` | click on the toast |
| `act(t)` | its one button (*Join*, *Open*, *Undo*) – needs the ToastStack extension below |

Rule: `title` = the subject (a player name, *New cosmetics*), `text` ≤ 2 words (*Invites you*, *Online*,
*3 items*). Keys `ui.toast.*`.

### `dialog/confirm`

| Field / action | |
|---|---|
| `confirm` | `{ open, icon, title (≤ 3 words + ?), text ('' or ≤ 1 sentence), yes (the verb), danger }` |
| `accept()` `cancel()` | Esc / scrim click = cancel |

Keys: `ui.confirm.*` (sign out, reset HUD, remove friend).

### `hud/editor` (+ toolbar, hint, widget-bar, widget-library, preset-list, share-menu, context-menu, style-panel, import-dialog, reset-dialog, server-badge)

Drawn above the vui `HudLayer` by `ui/hud/editor/HudEditorScreen` (controller `EditorController`, details in
[HUD.md](HUD.md#editor)). The root stack has `pointer-events: none`: pointer input goes to the document where it has an
element (or while an overlay is open), everything else to the editor (drag = move, wheel = size, Shift+wheel =
widget value, Alt = no snap, arrows = nudge, right-click = menu).

| Field | |
|---|---|
| `firstRun` | shows the key hint (*Drag move · Wheel size · Alt free · Ctrl more*) until the first drag |
| `busy` | a widget is being moved or resized: hint and bar hidden, dock fades out |
| `widgetCount` | 0 → empty state with *Widgets* |
| `panel` ✎ | `''` `'widgets'` `'presets'` `'share'` `'style'` `'menu'` `'import'` `'reset'` |
| `snap` ✎ | magnet toggle |
| `canUndo` `canRedo` | |
| `dockTop` | the selection sits at the bottom centre: dock, hint and popovers flip to the top |
| `selected` | `{ name, pct, barX, barY, above, copy }` or null – the bar sits under (or above) the selection |
| `widgets` | `[{ id, name, tip, on, locked, canAdd }]` (library; `placed(w, on)`, `addCopy(w)`) |
| `presets` | `[{ id, name, icon, tip }]` (templates; `applyPreset(p)`) |
| `badges` | `[{ id, x, y, tip }]` – *Server off* badges on widgets the server disabled |
| `menu` | `{ x, y, items: [{ id, icon, label, keys }] }` – right-click menu (`menuPick(m)`) |
| `importCode` ✎ `importError` `files` `maxCode` | import dialog |
| `st` | style panel values (Model): `scope tab styleable oneLabel bg bgColor opacity corners border paint textColor accent shadow align variant scale minScale maxScale hasVariants autoAnchor` + `o_<key>` (own value) |
| `opts` `anchors` | the selected widget's options `[{ key, label, toggle, on, value, capture }]`, the 3 × 3 anchor grid |

Actions: `undo()` `redo()` `cancel()` `done()` `confirmReset()` `styleSelected()` `hideSelected()`
`resetSelected()` `copyCode()` `openImport()` `pasteCode()` `importNow()` `importFile(f)` `saveFile()`
`openFolder()` `styleScope(s)` `styleTab(t)` `set(key, value)` `inherit(key)` `setScale(s)` `setAnchor(id)`
`optionSet(o, on)` `optionStep(o, dir)` `optionCapture(o)`. Toasts (undo, hidden, copied, imported, preset applied)
go through `VuiMc.toasts()`. Keys `ui.hud.*`.

### `wardrobe/wardrobe` (+ slot-rail, item-grid, preview-pane)

| Field | |
|---|---|
| `query` ✎ `slot` ✎ | |
| `slots` | `[{ id, icon, name (ui.slot.*), count, worn }]` |
| `items` | `[{ id, name, tip (name · rarity · by), thumb, rarityColor, worn, locked, selected }]` |
| `current` | `{ name, rarity (ui.rarity.*), rarityColor, creator, worn, locked, lockReason }` or null |
| `playing` | emote preview running |
| `stillImage` | texture shown until the 3D preview is ready (`''` = none) |

The 3D player is drawn by the host into `doc.ref("stage").box()` after `doc.render`. Actions: `pick(it)`,
`wear(it)` (also double click), `playEmote()`, `openWheel()`, `resetView()` (also double click on the stage),
`rotate(dx, dy)` (drag on the stage), `close()`. Keys `ui.wardrobe.*`, `ui.slot.*`, `ui.rarity.*`.

## Icons

`components/ico` maps `name` → texture `vibeyclient:ui/icons/<name>` (tinted with `color`). Until vui can draw
pixel maps, the 9 × 9 maps of `ui/Icons` must be exported once to `assets/vibeyclient/textures/ui/icons/<name>.png`
(white on transparent). Used from `Icons`: `seal shirt friends gear search close back check info warn chat star
note crosshair cube wave host globe power person server`. New ones (rows top to bottom, `#` = on):

| Name | Rows |
|---|---|
| `plus` | `......... ....#.... ....#.... ....#.... .#######. ....#.... ....#.... ....#.... .........` |
| `undo` | `......... ..#...... .##...... #######.. .##....#. ..#....#. .......#. ..#####.. .........` |
| `redo` | `......... ......#.. ......##. ..####### .#....##. .#....#.. .#....... ..#####.. .........` |
| `magnet` | `##.....## ##.....## ......... ##.....## ##.....## ##.....## ###...### .#######. ..#####..` |
| `reset` | `..####.#. .#....##. #....###. #........ #.......# #.......# .#.....#. ..#####.. .........` |
| `lock` | `...###... ..#...#.. ..#...#.. .#######. .#######. .###.###. .###.###. .#######. .........` |
| `eye` | `......... ..#####.. .#.....#. #..###..# #..###..# .#.....#. ..#####.. ......... .........` |
| `layers` | `....#.... ..##.##.. ##.....## ..##.##.. #...#...# .##...##. ...###... ......... .........` |
| `play` | `.#....... .##...... .###..... .####.... .#####... .####.... .###..... .##...... .#.......` |
| `cape` | `.#######. .#.....#. .#######. .#######. ######### ######### ######### ######### #.#.#.#.#` |
| `wings` | `#.......# ##.....## ###...### ####.#### ######### .###.###. .##...##. .#.....#. .........` |
| `aura` | `..#####.. .#.....#. #..###..# #.#...#.# #.#.#.#.# #.#...#.# #..###..# .#.....#. ..#####..` |
| `paw` | `..#...#.. ..#...#.. #.......# #..###..# ..#####.. .#######. .#######. ..##.##.. .........` |
| `shield` | `######### ######### ######### ######### ######### .#######. ..#####.. ...###... ....#....` |
| `hat` | `......... ..#####.. ..#####.. ..#####.. ..#####.. ......... ######### ######### .........` |
| `bag` | `...###... ..#...#.. .#######. .#.....#. .#.###.#. .#.###.#. .#.....#. .#######. .........` |
| `bug` | `#..#.#..# .#.###.#. ..#####.. #.##.##.# .#######. ..#####.. #.##.##.# ..#####.. #..###..#` |
| `code` | `......... ..#...#.. .#..#..#. #...#...# .#..#..#. ..#...#.. ......... ......... .........` |
| `copy` | `#####.... #...#.... #..#####. #..#...#. ####...#. ...#...#. ...#####. ......... .........` |
| `hud` | `######### #.##....# #.##....# #.......# #.......# #....##.# #....##.# ######### .........` |
| `sliders` | `......... ..##..... ######### ..##..... ......... .....##.. ######### .....##.. .........` |
| `pin` | `..#####.. .#######. ##.....## ##.....## .##...##. ..##.##.. ...###... ....#.... .........` |
| `sparkle` | `....#.... ....#.... ...###... ######### ...###... ....#.... ....#.... #.......# .........` |

## What changed vs today, and why

| Today | New | Why |
|---|---|---|
| 5 tabs: Modules, General, Account, Wardrobe, About | One view; header icons; General → gear popover; Account → friends drawer; About → click the wordmark; Wardrobe tab dropped (the full-screen wardrobe is one icon away) | Tabs were 5 places for ~10 rows of global stuff. Players come for modules. |
| Tiles with 2-line description | Icon + name + switch; description = tooltip | 10 → 1.6 words per module; 16 modules fit one screen |
| *For creators* disclosure + hint text | *Creator tools n* chip | one word less, same place |
| Module page intro: big icon, description, several buttons, notices | One bar: back, icon, name (tooltip = description), notice chips, one primary, switch | One primary per view; nothing to read before the settings |
| Cards + two-column switch packing + separate *Keys* card | Grid of section cards (1 column at 427 px, 2–3 wider); key rows inside their section | Keys belong to their feature; layout reflows instead of special cases |
| *Advanced · for creators* collapsible | `More n` chip | one rule everywhere (page, quick settings) |
| General tab with technical table (version, game, server, protocol, folder, Copy) | Quick settings: 5 rows; More: server address + *Bug report info · Copy* | One copy button gives the same info to bug reports |
| Account tab: profile card with server/ID/sign-in, status, status text, privacy, devices, blocked | Drawer: me + status select, requests chip, friends with hover actions, *+ Add*, *Account* icon → existing account screen (privacy, devices, blocked, sign out) | What players use daily is in the drawer; the rest one click deeper |
| 8 long status tooltips | 3 short ones | |
| Onboarding: 3 steps, 143 words, privacy paragraphs | Welcome (4 feature icons + language), *Go online?* (2 cards, privacy in tooltips), *Your keys* (3 real keybinds) | Same decisions, 38 words; keys rebind right there |
| HUD editor: 3-step intro tour, dock, right-click menu, style popover with tabs, templates, share submenu, long hints | Dock (*Widgets*, *Presets*, undo/redo, snap, share, reset, *Cancel*, *Done*), first-run key hint, bar under the selected widget (size %, style, hide, reset) | Direct manipulation does the explaining; 434 → 37 words. Right-click menu, multi-select and style details can stay as host features |
| Wardrobe: word category list, item names + rarity words under thumbnails, *Reset view* button | Icon rail (tooltip: name: count, dot = something worn), thumbnails with a rarity line, preview with name + rarity + one primary, icon buttons for wheel/reset | Thumbnails are what you scan; the grid gets 5 columns at 427 px |
| Pause row: 3 labelled buttons | *Vibey* labelled + icon buttons | German labels cut today; icons don't |
| Toasts: title + sentence | Subject + ≤ 2 words + one action | readable at a glance in game |

Kept: title corner icons (already minimal), search (typing anywhere), Esc chain (key capture → field → page →
search → close), server-rule lock, accent colours per module, UI scale.

## Migration order

Each step lands on Fabric 1.21.11 behind a dev flag, gets gametest screenshots (EN + DE, 427 × 240 and
640 × 360) and replaces the old code only after parity.

1. **Plumbing** (Java): client `Vui` instance with the MC backend, `Lang.addFile("ui")` + `vui.functions().translator`,
   load `vui/theme.json`, export icon textures, a small adapter Setting → descriptor (table above), toast action
   support.
2. **Toasts** – `toast/stack` replaces the current toast drawing (lowest risk, used everywhere).
3. **Title corner + pause row** – replace `TitleButtons` / `PauseMenu` drawing; `MenuEntries` stays the API.
4. **Confirm dialog + empty states** – shared by everything after this.
5. **Vibey menu: shell, grid, module page** – generated pages from the descriptor adapter; custom
   `createSettingsPanel()` pages keep working inside the body until ported. Biggest win.
6. **Quick settings + About** – then delete the General and About tabs.
7. **Friends drawer** – social module fills `account`/`friends*`; delete the Account tab.
8. **Onboarding**.
9. **HUD editor chrome** – on top of the vui `HudLayer`/`HudEditor`.
10. **Wardrobe** – with the cosmetico preview widget in the stage.
11. **Text pass over modules** – labels ≤ 3 words, `.desc` ≤ 1 sentence, module names ≤ 12 chars; extend
    `checkI18n` to fail on longer labels.

## Previews

`docs/ui-preview/<screen>.png` (427 × 240 EN), `<screen>-640.png` (640 × 360 EN), `<screen>-de.png` (427 × 240 DE),
all at scale 3. State: `docs/ui-preview/state/<screen>.json` (+ `-de`). Screens that sit on vanilla screens or the
HUD are rendered inside small fixture documents in `docs/ui-preview/frames/` (fake title screen, pause menu, HUD
widgets, menu behind the dialog) – fixtures only, not shipped.

| Screen | Files |
|---|---|
| Title corner (hover: Friends) | `title*.png` |
| Pause row (hover: Friends) | `pause*.png` |
| Menu grid (hover: tile summary) | `menu-grid*.png` |
| Search with setting hits / no result | `menu-search*.png`, `menu-search-empty*.png` |
| Module page (hover: locked row) / scrolled with More open | `menu-page*.png`, `menu-page-more*.png` |
| Quick settings / About | `menu-quick*.png`, `menu-about*.png` |
| Friends drawer (hover: friend) / online off | `menu-friends*.png`, `menu-friends-off*.png` |
| Onboarding 1–3 | `onboarding-1*.png` … `onboarding-3*.png` |
| Toasts | `toast*.png` |
| HUD editor / widgets popover / presets popover | `hud-editor*.png`, `hud-widgets*.png`, `hud-presets*.png` |
| Wardrobe / empty search | `wardrobe*.png`, `wardrobe-empty*.png` |
| Confirm dialog | `confirm*.png` |

They were rendered with the vui core and a small preview wrapper (not in the repo) that adds what the stock
`Preview` lacks: the `ui.json` translator, `theme.json`, icon textures from the `Icons` pixel maps, icon tint,
sample heads/thumbnails, hover/scroll before the shot. The stock command works on every document too (exit 0 with a
matching state file) but shows i18n keys and white icons:

```
java -cp "out/core;gson.jar" dev.henny.vui.dev.Preview src/main/resources/assets/vibeyclient/vui/menu/shell.vui 427 240 out.png docs/ui-preview/state/menu-page.json --scale 3 --root src/main/resources
```

The headless 5 × 7 font is wider than Minecraft's and draws umlauts without dots; real text is a bit narrower, so
what fits in the previews fits in game.

## vui gaps found

| Gap | Effect here | Wish |
|---|---|---|
| `Preview` has no translator and no theme option | previews show keys | `--lang <ui.json>` (or `--i18n dir`), `--theme <file>` |
| Headless `ImageCanvas.texture` ignores `tint` | monochrome icons render white | tint like the MC backend |
| No icon source besides one texture per icon | 9 × 9 `Icons` maps must be exported to PNGs | `IconProvider` / pixel-map icons (`<icon set="vibeyclient" name="gear">`) |
| Object props of a component are `null` on their first evaluation | "cannot read 'on' of null" errors unless the default is `{}` | evaluate bound props before the first child binding |
| A document rule did not override an imported component's rule of equal specificity (`.pause-btn` vs `.icon-btn`) | needed `.pause-row .pause-btn` | check cascade order for fallthrough classes vs the docs |
| `ToastStack` has no action button / `act(t)` and `Toast` no `action` | toast template calls `act(t)` | `Toast.action(label, Runnable)` |
| `disabled` swallows hover, so a disabled control cannot explain itself | tooltip moved to the row label | tooltips on disabled elements |
| One `border-color` for all sides | tile accent = extra 2 px box | per-side border colours |
| No `aspect-ratio`, no grid spans | fixed thumbnail heights | `aspect-ratio`, `grid-column: span n` |
| `colorpicker` has no swatches/presets | Setting `.presets()` cannot show | `presets` prop |
| `slider` shows no value | controller formats `display` | optional value label with format |
| `segmented` vs `select` can't be chosen by fit | controller passes `compact` | auto mode that falls back to `select` when too wide |
| No way to declare a document's controller fields | contracts live in this file | a `<controller>` block (names + types) checked by `Preview`/tests |
| `keybind` stores keyboard keys only | zoom on mouse 4/5 not possible | mouse buttons |
| Headless font: no `·`, no umlaut dots | approximate German previews | a few more glyphs |
