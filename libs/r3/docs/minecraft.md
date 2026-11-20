# r3 in a Minecraft mod

The r3 mod (`r3`, Fabric and NeoForge, Minecraft 1.21.1 – 26.3) runs one shared 3D scene in the client. Your mod
adds objects to it; r3 draws them in the world, feeds them the crosshair and the mouse, keeps vanilla from attacking
or scrolling the hotbar while a player uses them, shows first-time hints and stores placements. Everything you touch
is the version-independent core API (`dev.henny.r3.*`), plus a tiny facade `dev.henny.r3.mc.R3Mc`.

## Setup

Depend on `r3` and `vui` (r3 needs vui, see [libs/README.md](../../README.md#use-a-lib-from-another-mod)). r3 starts
its client on the **first client tick**, so do your setup there or hand it to `R3Mod.whenReady(...)` – Fabric does
not order the entrypoints of different mods, and on NeoForge the game is not up while mod constructors run.

```java
R3Mod.whenReady(() -> R3Mc.onWorld(new WorldListener() {
    @Override public void worldJoined(String worldKey) { restore(worldKey); }
    @Override public void worldLeft(String worldKey) { save(worldKey); }
}));
```

## Facade

| Call | Does |
|---|---|
| `R3Mc.scene()` | the client's `R3Scene` (add/remove objects, `interaction()` for events, `query()`) |
| `R3Mc.vuiContent(docId, controller)` | just the `VuiPanelContent` (e.g. as a `VideoSurface` overlay) |
| `R3Mc.panel(docId, controller)` | a `WorldObject` showing a vui document (`assets/<ns>/vui/<name>.vui`), 2 × 1.125 blocks, `InteractionRules.screen()` |
| `R3Mc.text(text)` | a `WorldObject` with a `Text3D` that faces the camera, not interactive |
| `R3Mc.add(object)` | add to the scene where it is |
| `R3Mc.place(object)` | add and start placing: follows the crosshair, sticks to block faces, left click places, right click/Esc cancels |
| `R3Mc.remove(object)` | remove (closes a vui panel's document) |
| `R3Mc.texture(id, handle)` | lets vui `src="id"` (and `TextureHandle.resource(id)`) draw a texture you own, e.g. a cover art you update |
| `R3Mc.dynamicTexture(name, w, h)` | a `TextureHandle` you upload ARGB pixels to (video frames) |
| `R3Mc.placements()` / `storage()` | `PlacementStore` of the current world, file storage |
| `R3Mc.worldKey()` / `onWorld(listener)` | `sp:<save folder>` / `mp:<address>`; join/leave callbacks |
| `R3Mc.vui()` | the `Vui` instance used for world panels |

## A vui panel with buttons

`assets/mymod/vui/player.vui`:

```html
<template>
  <column class="card" gap="5">
    <text>{{ title }}</text>
    <row gap="5">
      <button id="play" @click="play()">▶</button>
      <button id="stop" @click="stop()">■</button>
    </row>
  </column>
</template>
```

```java
WorldObject panel = R3Mc.panel("mymod:player", controller);
R3Mc.place(panel);
R3Mc.scene().interaction().listen(e -> {
    if (e.target() == panel && e.kind() == InteractionEventKind.ACTION) log(e.controlId());   // "play"
    if (e.target() == panel && e.changedPose()) save(panel);
});
```

- Looking at a button highlights it (`:hover` styles, the crosshair is the pointer), left click calls the
  controller's `@click` method and fires an `ACTION` event with the element id.
- **Controls** are elements with an `id` or a `@click` handler (or one of their children). A control without id gets
  `vui@<file>:<line>:<col>`. Everything else on the panel is body: middle mouse grabs, the wheel resizes – unless a
  vui scroll container actually scrolls.
- Panel pixels = `size × 64` per block (`WorldObject.pixelsPerBlock`); the document is laid out at that size in GUI
  pixels with the vanilla font (9 px lines).
- Clicked buttons drop their focus (no focus ring after a pointer click); a clicked **text field** keeps it and
  then gets the keyboard, see "Keyboard" below.

## 3D text and effects

```java
WorldObject title = R3Mc.text("Hello");
((Text3D) title.content()).style(TextStyle.DEFAULT.withGlow(0xFF5A4BFF, 0.8f)).pixelSize(0.03f);
title.transform().position(x, y, z);
R3Mc.add(title);

ParticleSystem ring = new ParticleSystem(128, ParticleStyle.glowDots(0xFF7FD4FF).withMode(RenderMode.ADDITIVE), 7)
        .emitter(new Emitter(EmitterShape.RING).radius(0.45f).rate(40).lifetime(0.8f, 1.4f).size(0.025f, 0.045f));
R3Mc.add(WorldObject.create("mymod:ring").content(ring).rules(new InteractionRules().interactive(false)));
```

Set `dimension(...)` on objects you create yourself (`R3Mc.scene().query().dimension()`); `R3Mc.panel/text` do it.

## Textures

| Handle | Id |
|---|---|
| `TextureHandle.resource("mymod:ui/bg", w, h)` | `assets/mymod/textures/ui/bg.png` (a full `ns:textures/....png` path works too) |
| vui `src="mymod:icons/gear"` / `url(...)` | same mapping, size read from the PNG header |
| `R3Mc.dynamicTexture("cam", 320, 180)` | `r3:dynamic/cam`; `upload(int[] argb)` on the render thread, `close()` frees it |

Any `TextureHandle` that also implements `McTexture` (a texture id string, may change every frame) is drawn as that
texture – Vibey's video screens hand r3 the FFmpeg frame texture or the MCEF browser view that way.

## Input

| Vanilla input | What r3 does |
|---|---|
| mouse buttons | `scene.press/release`; a consumed press is never seen by vanilla (no attack, use, pick block), nor its release |
| wheel | consumed when the scene resizes or the crosshair is on a resizable object (hotbar keeps its slot) |
| keys | Esc / R / Ctrl+Z go to the scene first; consumed while holding (Esc does not open the pause menu) |
| every tick | while `blocksVanillaMouse()`: queued attack/use/pick clicks are dropped and those keys released |

**Keyboard to panels.** While a panel content says `keyFocused()` (`KeyContent`; vui: a focused `textfield`),
every key press/repeat/release and every typed character goes to it instead of the game (`charTyped` mixin):
the player does not walk, chat does not open. **Esc** releases the focus (no pause menu), so does a click anywhere
else; F1–F12 still reach the game. Modifiers (Shift/Ctrl/Alt) come from the held keys, Ctrl+V reads the system
clipboard.

**Block outline.** While the crosshair is on an r3 object (or one is held/resized) the vanilla block outline is not
drawn (`BlockOutlineMixin`: `extractBlockOutline` 1.21.9–26.1, `submitBlockOutline` 26.2+, `renderHitOutline`
before 1.21.9).

Codes are GLFW numbers on every version (26.3's SDL scancodes and mouse numbers are mapped back). Letter keys follow
the keyboard layout (Ctrl+Z is the key labelled Z on QWERTZ too). Nothing is routed while a screen or overlay is open;
releases always are, so a grab never gets stuck.

Entry points (the mixins call them, tests can too): `R3Hooks.mouseButton(nativeButton, action)`,
`R3Hooks.scroll(notches)`, `R3Hooks.key(nativeKey, action)` with actions 1 press, 0 release, 2 repeat.

## Storage

`config/r3/`:

| File | Content |
|---|---|
| `hints.json` | first-time hints already seen |
| `bindings.json` | gesture bindings (`GestureBindings.toMap()`), loaded at start |
| `worlds/<key>-<hash>.json` | `PlacementStore` of one world/server, loaded on join, saved on leave (`R3Mc.storage().saveWorld()` any time) |

On leaving a world the scene is cleared; add your objects again in `worldJoined`.

## Sharing

`SyncChannel` stays an interface: the host mod carries `SyncCodec` JSON over its own network. For single player and
tests there is `LoopbackChannel` (`LoopbackChannel.singleplayer(uuid)`, or a `LoopbackHub` with one channel per
player).

## Rendering

| Minecraft | Hook | Drawing |
|---|---|---|
| 1.21.1 – 1.21.8 (Fabric) | `WorldRenderEvents.AFTER_ENTITIES` | straight into the buffer source, one `endBatch` per item |
| 1.21.9+ (Fabric, NeoForge) | mixin: end of `LevelRenderer.submitEntities` | submit collector, one `order(n)` per item |

- The canvas transforms every vertex on the CPU (camera-relative floats) and keeps the draw order: consecutive quads
  of the same mode/texture share one draw, a text or another texture starts the next.
- Render modes: `CUTOUT`/`TRANSLUCENT` → `text(texture)` (a white texture for plain colors), `EMISSIVE` → the same
  with full-bright light, `ADDITIVE` → `lightning()` untextured, `eyes(texture)` textured.
- Every quad is turned to face the camera, so winding never hides anything (panels are mirrored by their y-down
  pixel space).
- Panels: each draw call sits 0.012 px further toward the viewer (at most 160 steps ≈ 0.03 blocks), text uses the
  polygon-offset font mode – no z-fighting between a button and its label. Scissors clip quads on the CPU; text is
  dropped glyph by glyph when it leaves the scissor.
- Culling uses a camera frustum (`CameraFrustum`: fov from the options + 12° margin), not the game's.

## Dev command and test

`/r3 demo` puts a vui panel (title, ▶ ⏭ ■) on the block face you look at, a glowing "r3" above it and a particle
ring below. `/r3 clear` removes it. `R3GameTest` (client game test, Fabric 1.21.11) plays every gesture with real
mouse/keyboard input and writes screenshots to `build/review/r3/`:

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=R3GameTest
```

## Package map (`dev.henny.r3.mc`)

| Package | Job |
|---|---|
| (root) | `R3Mod` (id, log, `whenReady`), `R3Mc` facade, `R3Hooks` (entry points of mixins and loader events) |
| `client` | `R3Client` (scene, storage, input, frame/tick), `WorldSession` (join/leave), `FrameClock` |
| `compat` | per-version calls: `Game` (screen, level, dimension, partial tick, fov), `Ids` |
| `world` | `McWorldQuery`, `BlockRaycast`, `EntityLookup` |
| `view` | `CameraReader` (camera → `CameraState`, frustum) |
| `render` | `McWorldCanvas`, `McPanelCanvas`, `McFontMetrics`, draw list, render types, sinks |
| `texture` | `McTextures`, `DynamicTextureHandle`, `ExternalTextures` (`R3Mc.texture`), white texture |
| `input` | `InputRouter`, `HeldInput`, `NativeInput`/`SdlKeys`, `KeyLayout`, `VanillaGuard` |
| `hud` | `HintOverlay` (small hint line: under the crosshair, or below the hovered object when that covers it – `HintPlacement`) |
| `store` | `R3Storage`, `WorldKeyResolver` |
| `vui` | `WorldVui` (vui backend for world panels: font metrics, resources, textures) |
| `demo` | `/r3 demo` |
| `mixin` | mouse, keyboard (keys + characters), level renderer, block outline |
| `fabric`, `neoforge` | entrypoints: ticks, HUD layer, commands, render event (Fabric < 1.21.9) |
