# Camera module (`camera`)

Freelook, a rotatable third-person ("skin") view and motion blur (features #23, #24, #25 of
[FEATURES.md](FEATURES.md)). **Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `camera: ['fabric-1.21.11']`,
`BuiltinModules` registers it by reflection). Code: `src/main/java/dev/henny/vibeyclient/module/camera`.

> See also: [MODULES.md](MODULES.md) · [PVP.md](PVP.md) (zoom) · [SERVER-RULES.md](SERVER-RULES.md) ·
> [libs/r3](../libs/r3/README.md) (world objects keep the middle mouse)

## Features

| Feature | Settings (section `look` / `blur`) | How |
|---|---|---|
| Freelook | `freelook` (**off**), `freelookToggle` (off = hold), key `freelookKey` (**Left Alt**) | While active the mouse turns only the camera: `MouseHandlerMixin` wraps `LocalPlayer.turn` inside `turnPlayer` and hands the deltas to `look/FreeLook` (`core/LookAngles`, pitch clamped to ±90°). `CameraMixin` wraps `Entity.getViewYRot/getViewXRot` inside `Camera.setup`, so only the camera uses the free angles – the player, its head, attacks and movement keep the real view. From first person it switches to third person back and restores the previous view on release (unless the player pressed F5 meanwhile). |
| Orbit / skin view | `orbit` (on) | In third person, **middle-drag** rotates the camera around the player (`look/Orbit`, `core/OrbitOffset`), the **wheel while dragging** moves it closer/further (0.5–3× of vanilla's distance, `ModifyArg` on `Camera.getMaxZoom`). The offset stays while in third person and resets in first person. A short middle **tap** (`core/TapOrDrag`: < 300 ms, little movement) still picks the block when pick-block is on the middle mouse. When the crosshair is on an r3 object (or r3 holds one) the middle mouse stays with r3 (grab/move). |
| Motion blur | `motionBlur` (**off**), `blurStrength` 1–10 (4) | Post chain `vibeyclient:motion_blur` (`assets/vibeyclient/post_effect/motion_blur.json`, shader `shaders/post/motion_blur.fsh`): `mix(now, previous, weight)` into `swap`, copy to a **persistent** target `previous`, blit to `main`. Run by `GameRendererMixin` right after the level (and entity outlines), before the GUI – menus and HUD are never blurred. |

### Motion blur details

- The weight comes from `core/BlurMath.historyWeight(strength, dt) = min(0.97, exp(-dt / (0.012 s × strength)))`, so
  the trail lasts the same time at 30 or 500 fps (12 ms per strength step, 12–120 ms).
- History is dropped (weight 0) on the first frame after switching on, after a window resize and when the camera moved
  more than 4 blocks in one frame (teleport, respawn, dimension change) – no ghost images of the old place.
- The strength is written every frame into the pass's `MotionBlurConfig` uniform buffer: `blur/BlurUniform` replaces
  the buffer vanilla created from the JSON (accessors `PostChainAccessor`, `PostPassAccessor`) with one that has
  `USAGE_COPY_DST` and `writeToBuffer`s 16 bytes. If that ever fails it logs once and the chain keeps the JSON value
  (0.6) – blur still works, only without strength/reset. If the chain does not load at all (shader error, a resource
  pack breaks it) the module page shows *Motion blur: not available here.*
- Accessibility: off by default, the description says it can cause discomfort, a low default strength, never on menus.
- Cost: three full-screen passes (blend, copy, blit) – see "Frame time" below.

## Fairness and server rules

| Key | Blocks |
|---|---|
| `camera.freelook` (and the older key `freelook`) | Freelook (looking around without turning). |
| `camera.orbit` | The orbit drag in third person. Vanilla F5 stays. |
| `camera.*` | Both. |

Freelook is a small advantage (you see behind you while running forward), so it is **off by default** and has the
rule. The orbit view only shows what vanilla's front view (F5) also shows, so it is on by default but has its own key
for servers that want to forbid it. Motion blur is purely cosmetic (no rule). All keys are declared with
`ServerRules.supports` (`CameraRules`), settings carry `.serverRule(...)` (badge on the page), and a rule change ends
an active freelook/orbit at once.

## Zoom (pvp) and other mods

- Zoom (`pvp`, key C) and freelook stack: both wrap the same `LocalPlayer.turn` call with `@WrapOperation`, so the
  zoom's slower mouse applies and freelook still turns only the camera; the zoom keeps the wheel while its key is held,
  the orbit takes the wheel only while the middle mouse is down. `CameraGameTest` holds both keys together.
- Every hook is `require = 0` (`vibeyclient.camera.mixins.json` `defaultRequire: 0`); `@WrapOperation`/`@ModifyArg`
  stack with other mods that touch the same calls.

## Code map

| Package | Job |
|---|---|
| `camera` | `CameraModule` (settings, `*Active()`), `CameraRules` (keys) |
| `camera.core` | pure Java, JUnit in `src/junit/java/.../module/camera/core`: `LookAngles`, `OrbitOffset`, `TapOrDrag`, `BlurMath` |
| `camera.look` | `CameraState` (per-frame update, mouse routing, angles for the camera), `FreeLook`, `Orbit`, `R3Busy` |
| `camera.blur` | `MotionBlur` (chain, weight, reset), `BlurUniform` (uniform buffer) |
| `camera.mixin` | `CameraMixin`, `MouseHandlerMixin` (frame update, turn, middle button, wheel), `GameRendererMixin`, accessors |

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.camera.*"` (9 tests).
- Game: `CameraGameTest` (+ `CameraScene`, `CameraLookChecks`, `CameraOrbitChecks`, `CameraBlurChecks`), shots in
  `build/review/camera/`: module page EN/DE, freelook held (player unchanged, camera turned, third person) and
  released (first person, camera = player view), toggle mode, rules `camera.freelook`/`freelook`/`camera.*`,
  freelook + zoom together, orbit drag to the front ("skin view"), wheel distance, middle tap still picks the block,
  rule `camera.orbit`, blur off/on while turning (strength 10 and 3), still frame, frame time on/off.

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=CameraGameTest -Dvibeyclient.test.relay=off
```

## Frame time

Measured by `CameraGameTest` (`ReviewKit.compare`: 3 alternating rounds of 120 ticks off/on, medians, vsync off,
limit 260, no inactive-window cap; frame intervals from `FrameClockMixin`).

| Measured (2026-11-20, Intel Arc, 1280 × 720, shared PC) | off | on | cost |
|---|---|---|---|
| Motion blur (3 passes) | 1.810 ms (553 fps) | 2.208 ms (453 fps) | **+0.40 ms/frame** (≈ +2.4 % at 60 fps) |

The cost is GPU fill: three 1280 × 720 passes ≈ 0.13 ms each; it grows with the window size. Freelook and orbit
cost nothing measurable (a few float operations per frame, nothing allocated). A two-pass version would need two
persistent targets swapped every frame (post chains have no ping-pong), noted as a later improvement.
