# Inventory module (`inventory`)

Tooltip extras, quick moves, inventory search and hotbar presets (features #9, #29, #30 of
[FEATURES.md](FEATURES.md)). **Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `inventory: ['fabric-1.21.11']`,
`BuiltinModules` registers it by reflection). Code: `src/main/java/dev/henny/vibeyclient/module/inventory`.

> See also: [MODULES.md](MODULES.md) · [HUD.md](HUD.md) (preset bar widget) · [SERVER-RULES.md](SERVER-RULES.md) ·
> [PVP.md](PVP.md) (no-advantage rule) · [libs/vui](../libs/vui/README.md) (search field)

## No advantage beyond vanilla

| Feature | Why it is fair |
|---|---|
| Durability % | Vanilla shows the durability bar and, with F3+H, the exact value. |
| Shulker preview | Vanilla lists the first five stacks of a shulker box in its tooltip; the grid shows the same data. Bundles keep vanilla's own content tooltip (a grid since 1.21.2). |
| Search | Only looks at items already on screen. |
| Quick moves | Only sends the normal clicks a player could make (pick up, place one, shift-click), **never faster than the click limit** (default 12/s, max 20/s). Off by default; server rule `inventory.tweaks`. |
| Hotbar presets | One vanilla swap click (the number-key swap) per slot, through the same limit; never more than 9 swaps per switch. Server rule `inventory.presets`. |

## Features

| Feature | Settings (section) | How |
|---|---|---|
| Durability % | `durability` (tooltip) | `tooltip/DurabilityLine` (Fabric `ItemTooltipCallback`) adds "Durability 67% (1061)" under the name, coloured by `core/DurabilityText` (teal > 60 %, yellow > 30 %, amber > 10 %, red). |
| Shulker preview | `preview` (tooltip) | `mixin/ItemStackTooltipMixin` returns a `tooltip/ContainerPreview` from `getTooltipImage` for items with a `container` component (shulker boxes: always 27 cells, others up to 54) and drops vanilla's text list (`addToTooltip(CONTAINER)`). `ContainerPreviewTooltip` (registered with `TooltipComponentCallback`) draws the 9-column grid with counts and a line in the box colour. No click, no key: hovering is enough. |
| Shift-drag | `tweaks` (off), `dragMove`, `dragMatching` (tweaks) | Shift + left press on a stack: vanilla quick-moves it; `screen/TweakInput` remembers the drag (`core/DragTracker`) and quick-moves every other stack the pointer passes while Shift and the button stay down (each slot once; with `dragMatching` only the same item). |
| Wheel moves | `wheelMove`, `wheelInvert` (tweaks) | Over a stack: wheel down = one item to the other side, wheel up = one matching item from the other side into this stack; with Shift a whole stack. Other side = container ↔ player inventory, or main ↔ hotbar in the player inventory. Bundles keep vanilla's wheel. Max 3 wheel moves wait in the queue, more notches are dropped. |
| Click limit | `clickRate` 4–20/s (default 12) | All tweak and preset clicks go through `click/ClickSender`: a queue of *intents* (max 32) planned lazily against the current menu (`core/MovePlanner`, `core/PresetPlanner`) and sent one click per `1/rate` s (`core/ClickPacer`, no bursts). A plan is dropped when the container changes; the player's own slot clicks are blocked while a plan is half done (cursor holds items). |
| Search | `search`, `searchKeep` (search) | In every container screen except creative (it has its own search): a vui field (`assets/vibeyclient/vui/inventory/search.vui`) above the top right corner of the container. Click it or **Ctrl+F**; Esc leaves the field (a second Esc closes the screen). Matching slots get a purple frame, the others are dimmed (`search/SlotHighlight` from a `renderSlots` hook); the field shows the hit count and a clear button. Query syntax (`core/SearchQuery`): words are AND-ed, case and accents ignored (ß = ss); a word matches name, item id, enchantments and the names of items inside shulkers/bundles; `@mod` matches the namespace; `-word` excludes. Matching is cached per slot and refreshed every 5 frames. |
| Hotbar presets | `presets`, `presetCount` (2–6, default 3), `presetAlways` (presets) · keys `presetKey` (**R**), `presetSaveKey` (unbound) · button *Save hotbar* | `presets/Presets` keeps the sets in the module data (`config/vibeyclient/inventory.json`, `_data.presets`; item id + custom name per slot). **Tap R** = next preset, **Shift + tap R** = save the hotbar into the current preset, **hold R** = the bar appears, the wheel picks a row, release switches. Switching plans one swap at a time (`core/PresetPlanner`: exact name first, then same item; never takes an item from a hotbar slot that already holds its own preset item; missing items are skipped). Only while no screen (or the player inventory) is open. |
| Preset bar | HUD widget `inventory.presets` (default bottom right) | `presets/PresetBarWidget`: one row per preset (number + 9 half-size item icons), the active one framed, the picked one filled. Shown while the key is held, 1.8 s after a switch, or always (`presetAlways`). Move/resize it in the HUD editor (*Edit HUD* button). All built-in HUD templates place it, so it is never hidden on a fresh install. |

The vui search field is hosted by the module itself: `search/SearchOverlay` opens the document with
`VuiMc.vui().open(...)` and drives a `DocumentView` from Fabric screen events (render, click, release, wheel, keys);
typed characters come from `mixin/KeyboardCharMixin` (vui overlays do not get chars). One `screen/ContainerSession` per
open container screen ties search, tweaks and the click pump together.

## Server rules

`inventory.tweaks` (shift-drag + wheel moves; the queue is cleared at once), `inventory.presets` (preset key, bar,
switching). Setting rows carry the keys (*Disabled by this server* badge); the HUD widget uses `.rules(...)`.

## Code map

| Package | Job |
|---|---|
| `inventory` | `InventoryModule` (settings, `*Active()` helpers), `InventoryRules` |
| `inventory.core` | pure Java, JUnit in `src/junit/java/.../module/inventory/core`: `MovePlanner`/`SlotFinder`/`SlotView`/`Section`/`Click`/`ClickKind`, `ClickPacer`, `BoundedQueue`, `DragTracker`, `SearchQuery`/`ItemFacts`/`TextFold`, `HotbarPreset`/`ItemRef`/`PresetSet`/`PresetPlanner`/`PresetJson`, `KeyGesture`, `DurabilityText` |
| `inventory.click` | `ClickSender` (paced queue), `Intent`, `WheelIntent`, `QuickMoveIntent`, `MenuSlots` (menu → `SlotView`s, item keys = id + component patch hash) |
| `inventory.screen` | `ScreenHooks` (Fabric screen events), `ContainerSessions`, `ContainerSession`, `TweakInput` |
| `inventory.search` | `SearchOverlay`, `SearchController`, `SearchState`, `ItemFactsReader`, `SlotHighlight` |
| `inventory.tooltip` | `TooltipHooks`, `DurabilityLine`, `PreviewItems`, `ContainerPreview`, `ContainerPreviewTooltip` |
| `inventory.presets` | `Presets`, `PresetIntent`, `PresetKeys`, `PresetHud`, `PresetBarWidget`, `ItemRefs` |
| `inventory.mixin` | `ContainerScreenAccess`, `ContainerScreenSlotsMixin`, `ItemStackTooltipMixin`, `KeyboardCharMixin`, `MouseScrollMixin` (wheel while the preset key is held) |

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.inventory.*"`.
- Game: `InventoryGameTest` (+ `InvTestKit`, `InventoryTooltipChecks`, `InventoryTweakChecks`, `InventoryPresetChecks`),
  screenshots in `build/review/inventory/NN_*.png`:

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=InventoryGameTest -Dvibeyclient.test.relay=off
```

It checks: module page en/de, tweaks off by default, preset bar not hidden by the HUD template; shulker grid (27
cells, vanilla list gone, back when the setting is off) and "67% (1061)" on a damaged diamond pickaxe; in a chest:
wheel down moves 1 item in 3 clicks, wheel up takes it back, Shift+wheel moves the stack, 6 fast notches queue only 3
moves and take at least 8 click gaps, Shift-drag moves sand + gravel and nothing else; Ctrl+F + "diamond" finds only
the diamonds, typing does not close the chest, Esc leaves the field first; presets: Shift+tap saves, tap switches
(2 swap clicks), hold + wheel picks preset 2 and release applies it; `inventory.tweaks` server rule stops the wheel.

Porting later: `GuiGraphics` → `GuiGraphicsExtractor` on 26.x (tooltip component, highlight), `KeyEvent`/
`MouseButtonEvent`/`CharacterEvent` exist since 1.21.9, `Identifier` was `ResourceLocation` before 1.21.11.
