# Rebuild: libs + modules (contract)

Branch `henny/client-aenderungen`. Goal: a client an agent can fix in minutes, not hours. Three reusable libs (own
mods, usable without Vibey Client), the client split into a small core + modules, a much simpler UI.

This file is the contract between the agents of this round. Change it only through the coordinator.

## Ground rules

- **Step by step, never big bang.** Libs grow next to the old code; features move one by one. The client builds and
  runs after every merge. Old code is deleted only after the new path has parity *and* a test.
- **Fabric 1.21.11 first** (`vcsVersion`). New code is written, run and polished there; every other target must
  just compile (`//?` Stonecutter comments in the MC backend only). Porting is a later pass.
- **Pure core, thin backend.** Every lib has a `core` part with **no `net.minecraft` / loader imports** (plain Java
  21 + JOML + Gson), tested with JUnit in milliseconds. The per-version part (`mc`) only adapts drawing, input and
  hooks. A check task fails the build if a core file imports Minecraft.
- **Same API on every version.** Public API types never expose Minecraft classes (one exception: small `mc` entry
  helpers like opening a screen). Version differences live behind backend interfaces.
- **Less text.** UI shows the minimum: icon + short label, details on hover. No paragraphs in menus.
- **Direct manipulation everywhere.** Anything placed in the world or on the HUD can be grabbed, moved, resized and
  used where it is (see "Interaction model"). Think like that for every feature.
- **Licenses.** Apache-2.0. From other clients (NoRisk, Lunar, Feather, LabyMod, Essential, …) we take *ideas only*,
  never code or assets. Dependencies only with compatible licenses.
- **PC load.** At most 2 Gradle builds at once (`--max-workers=2`, only the target you need, never `buildAll`), at
  most **1 Minecraft instance**. Use the slot script: `bash C:/Users/henny/.t3/build-slots/slot.sh gradle -- <cmd>` /
  `slot.sh mc -- <cmd>` (waits for a free slot). Pure-core work uses `javac` + JUnit console, no Gradle.
- **No push to main, no release, no new GitHub repos** without the user's OK. Work on your own branch, commit there.

## Layout

```
libs/vui/         mod id `vui`        package dev.henny.vui        UI: markup, layout, styles, animation, binding, HUD, hot reload
libs/r3/          mod id `r3`         package dev.henny.r3         3D: world panels, 3D text, effects, interaction   (needs vui)
libs/cosmetico/   mod id `cosmetico`  package dev.henny.cosmetico  cosmetics engine: .vibey bundles, render, anim, fx (needs r3)
src/              Vibey Client (mod id vibeyclient): core + modules, ships the three libs jar-in-jar
```

Each lib: `src/core/java` (pure), `src/main/java` (MC backend, Stonecutter), `src/main/resources`
(`fabric.mod.json`, `META-INF/neoforge.mods.toml`, assets), `src/test/java` (JUnit for core), `README.md`
(what it is, 30-second example), `docs/` (full guide; also the source for the website developer docs).

Client modules: every module keeps its own package `dev.henny.vibeyclient.module.<id>` and declares in one place
which targets it supports (replaces the ad-hoc excludes in `gradle/common.gradle`). Modules depend on `api`/core and
the libs, never on each other (checked).

## vui – UI library

**Files.** `assets/<ns>/vui/<name>.vui`, Vue-like single file components (template + style + optional props), or the
same tree as `<name>.vui.json`. Both compile to one AST.

```html
<template>
  <column class="card" gap="4">
    <text class="title">{{ title }}</text>
    <row v-for="m in modules" :key="m.id" class="tile" :class="{ on: m.enabled }" @click="open(m)">
      <icon :src="m.icon"/> <text>{{ m.name }}</text> <toggle v-model="m.enabled"/>
    </row>
  </column>
</template>
<style>
.card { padding: 8; background: $panel; border: 1 $line; }
.tile:hover { background: $surface2; transition: background 150ms out; }
</style>
```

**Binding.** A Java controller object: fields/getters are read by `{{ }}`/`:prop`, methods are called by `@event`,
`v-model` writes back. Reactive state through `State<T>`/`ListState<T>` (no per-frame reflection, no per-frame
allocation). Explicit `Model` API too (`model.value("title", …)`, `model.action("open", …)`) for code without
reflection. Expressions: small safe language (paths, literals, `! && || == != < > + - * / ?:`, calls to controller
methods), compiled once.

**Layout.** Flexbox subset (`row`, `column`, `stack`, `grid`; grow/shrink/basis, gap, padding, margin, align,
justify, wrap, min/max), absolute/anchored positioning (HUD), scroll containers, text wrap/ellipsis. Units: GUI px,
`%`, `auto`.

**Styles.** CSS subset: tag, `.class`, `#id`, `:hover :focus :active :disabled :checked`, descendant/child,
variables/theme tokens (`$brand`, from a theme file), transitions, `@keyframes`.

**Animation.** Tweens + springs on any numeric style property, transitions, keyframes, enter/leave for `v-if`/
`v-for`, standard easings (`out` = cubic-bezier(0.2,0.8,0.2,1), 150 ms default). Global "animations off" switch.

**Components (built in).** box/row/column/stack/grid, text (rich, animated text effects: gradient, wave, rainbow,
pulse, shimmer), icon, image, button, toggle, checkbox, slider, textfield, select, segmented, tabs, scroll, list
(virtualised), tooltip, popover, modal, toast, keybind, color picker, progress, spinner, divider. Own components =
another `.vui` file with props + slots (`<slot/>`, named slots).

**Hosts.** `VuiScreen` (full screen), overlays/popovers, toasts, **HUD layer** (anchored widgets, edit mode with
drag / wheel-resize / snap, layouts saved per profile), in-world (via r3).

**Hot reload + dev tools** (dev runs or `-Dvui.dev=true`): watches the source resource folders, re-parses on save,
swaps the tree keeping controller state; errors show as an in-game overlay with file:line, never a crash.
Inspector (hover → node, file:line, computed style/layout). **Dev bridge**: localhost-only HTTP in dev mode
(`/screenshot`, `/tree`, `/open?doc=…`, `/reload`, `/click?id=…`) so an agent can change a file, see the result and
verify it without restarting Minecraft.

**Backend interface** (per version): `Canvas` (fill, gradient, line, text, texture, nine-slice, scissor, push/pop
transform, alpha), `TextMetrics`, `Input` (mouse, keys incl. layout-correct letters, text input, clipboard),
`Textures`. Everything above the backend is version-independent.

## r3 – 3D library

- **World objects:** `Panel` (a vui document rendered in the world: direct quads, or render-to-texture when needed),
  `Text3D` (billboard / fixed / follow entity, with vui text effects), `Mesh`, `Effect` (particles, beams, rings,
  glow, trails), `VideoSurface` (texture updated per frame).
- **Anchors:** block face (snap to walls/floor/ceiling), free position, entity-attached (e.g. above a player's
  head), camera-attached (HUD-like).
- **Render backend per version:** world render hook, render types (cutout/translucent/additive/emissive),
  lightmap, depth handling, frustum culling, distance fade.
- **Interaction model** (core, pure, used by every world/HUD object):
  - Crosshair (or mouse on a screen) = pointer. Ray → object → local (u,v).
  - Look at a control on the object (play, next, stop, …) → highlight; left click → action.
  - **Hold middle mouse** on the object → grab, it follows the view (distance kept, snaps to block faces when close).
  - **Mouse wheel** while looking at it / holding it → bigger/smaller (aspect kept). Shift+wheel → distance.
  - **Look at the border** → resize handle highlights; drag with middle mouse or wheel → scale that edge/corner.
  - Right click → small radial/context menu (close, lock, settings). Lock = no accidental grabs.
  - Every gesture rebindable; a 1-line hint near the crosshair the first time only.
  - Placements persist (per world/server) and sync where the feature needs it.

## cosmetico – cosmetics library

Everything the current `avatar/` engine does (it moves here): `.vibey` bundles (FORMAT.md, unchanged format),
items/slots/states/rarity, bone renderer + player parts, animations/emotes, effects/sounds/particles, previews and
thumbnails, self-test. Public API: register item sources, set a player's loadout, render hooks automatic, preview
widget (vui component). Vibey-specific parts (backend catalog, LoadoutSync, wardrobe screens) stay in the client.

## Ranks + animated names (client side)

Rank (from the backend profile/presence: e.g. owner, admin, mod, dev, creator, partner, supporter) → own icon before
the name in nametags, tab list, chat, friends list. Animated name styles (gradient, wave, rainbow, pulse, shimmer),
idea from NoRisk Client, built with vui text effects / r3 Text3D. Backend work only on a vibe-backend branch.

## Order

1. Research (players' wishes, 30 features from other clients), build infra (libs as Gradle projects + jar-in-jar),
   pure cores of vui and r3 – in parallel.
2. vui MC backend + hot reload + dev bridge; r3 MC backend; cosmetico extraction (with parity tests).
3. Move features over one by one: settings/menu (simplified), HUD, toasts, video screens → r3, lyrics world text,
   nametags + ranks + animated names, wardrobe.
4. New features from the research list, each with the interaction model.
5. Port to the other targets, full `buildAll`, docs for the website.
