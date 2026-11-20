# Companion packs

A **companion pack** is to a companion what a resource pack is to textures: a folder or `.zip` that brings one or
more living companions – model, textures, animations, props, sounds, particles, texts and **behavior** – into the
game. Packs contain **data only**. Nothing in a pack is ever executed as code, so a pack from a stranger is as safe as
a resource pack: the worst it can do is look bad, and every mistake is shown as a clear message (`file: path →
message`), never a crash.

This page is the complete reference. Everything here is implemented by companio 0.1 (`formatVersion` 1). The seal
pack (`content/companions/seal/`) uses every feature and is the best example to copy from.

Contents: [Layout](#layout) · [Manifest](#manifest-companionjson) · [Texts and languages](#texts-and-languages) ·
[Companion](#companion) · [Model and variants](#model-textures-and-variants) · [Animations](#animations) ·
[Props](#props) · [Sounds](#sounds) · [Particles](#particles) · [Movement](#movement) · [Looking](#looking) ·
[Needs and variables](#needs-and-variables) · [Behaviors](#behaviors) · [Events](#events) · [Actions](#actions) ·
[Expressions](#expressions) · [Tricks](#tricks) · [Player interaction](#what-the-player-can-do) ·
[Multiplayer](#multiplayer) · [Limits](#limits-and-validation) · [Workflow](#workflow-make-a-pack) ·
[Full example](#full-example)

## Layout

```
my_pack/                      a folder, or my_pack.zip with the same files at its root (or in one top folder)
  companion.json              the manifest (required)
  pack.png                    icon, square PNG, 64×64 is plenty (optional)
  lang/en_us.json             texts, English (recommended)
  lang/de_de.json             texts, German (recommended), any other <lang>_<region>.json works
  companions/penguin.json     one file per companion (or inline in the manifest)
  models/penguin.vibey        models: .vibey bundles (docs/FORMAT.md)
  textures/penguin_gold.png   extra textures for variants
  sounds/honk.ogg             OGG Vorbis sounds
```

Where packs live:

| Where | What |
|---|---|
| `.minecraft/companionpacks/` | the player's packs (`<name>.zip` or `<name>/`). The pack screen has an *Open folder* button. |
| inside a mod jar | built-in packs: `assets/<ns>/companionpacks/packs.json` lists them (companio ships seal, penguin, robot) |

Paths inside a pack are relative to the pack root, use `/`, lowercase `a-z 0-9 _ - . /`, no `..`.

## Manifest (`companion.json`)

```json
{
  "format": "companio-pack",
  "formatVersion": 1,
  "id": "penguin",
  "version": "1.0.0",
  "name": "@pack.name",
  "description": "@pack.description",
  "author": "You",
  "companions": ["companions/penguin.json"]
}
```

| Field | Required | Notes |
|---|---|---|
| `format` / `formatVersion` | yes | `"companio-pack"` / `1`. A higher `formatVersion` is shown as *needs a newer companio* and not loaded. |
| `id` | yes | `[a-z0-9_.-]{1,48}`. Unique: two packs with the same id → the later one in the list wins, a warning says so. |
| `version` | no | Free text shown in the pack screen. |
| `name`, `description`, `author` | `name` yes | [Texts](#texts-and-languages). Description: one short line. |
| `companions` | yes | List of paths to companion files, **or** companion objects inline. 1–16 entries. |

## Texts and languages

Every text field (names, descriptions, speech bubbles, trick names) accepts three forms:

```jsonc
"name": "Penguin"                                   // the same in every language
"name": { "en": "Penguin", "de": "Pinguin" }        // per language (en is the fallback)
"name": "@companion.penguin.name"                   // a key from lang/<lang>.json
```

`lang/en_us.json` is a flat map, like Minecraft's:

```json
{ "pack.name": "Penguin pack", "companion.penguin.name": "Penguin", "say.hello": "Hi! ♥" }
```

Lookup order: the game language (`de_de`), its base (`de`, i.e. any `de_*.json`), `en_us`, then the key itself (shown
as `@key` so you see it is missing). Speech bubble texts may use any character the Minecraft font has (`♥ ★ ♪ ☂ ☀ ❄
⚡ ☺ ✿ ✦`). Formatting codes (`§`) and control characters are removed; bubbles are cut at 40 characters.

## Companion

A companion file (`companions/<id>.json`). Only `id`, `name` and `model` are required; everything else has a sensible
default, so a first companion is ten lines.

```jsonc
{
  "id": "penguin",                        // [a-z0-9_]{1,32}, unique in the pack
  "name": "@companion.penguin.name",
  "description": "@companion.penguin.description",
  "model": "models/penguin.vibey",
  "scale": 0.8,                           // model scale (1 = the model as built)
  "size": { "default": 1.0, "min": 0.7, "max": 1.4 },   // the player may pick in this range (wheel on it / settings)
  "box": { "width": 0.6, "height": 0.8 }, // hit box in blocks at size 1 (looking at it, physics)
  "variants": [ … ],                      // texture variants
  "animations": { … },                    // which model animation plays for which movement
  "look": { … },                          // head turning
  "movement": { … },                      // speeds, swimming, flying, follow distances
  "props": { … },                         // umbrella, hat, ball … (bones shown/hidden by behaviors)
  "sounds": { … },                        // named sounds
  "particles": { … },                     // named particle bursts
  "needs": { … },                         // energy, happiness, curiosity, hunger …
  "vars": { … },                          // own numbers
  "tricks": [ … ],                        // entries in the right-click menu
  "behaviors": [ … ]                      // what it does when (the brain)
}
```

The player picks a companion as `<pack id>:<companion id>` (e.g. `penguin:penguin`).

## Model, textures and variants

`model` is a `.vibey` bundle (docs/FORMAT.md): kind `cosmetic`, slot `pet`, every root bone `"attach": "world"`, built
around its feet at the origin, **facing north (−Z)**. That is exactly what the Blockbench plugin exports for a pet and
what `content/tools` builds, so any Vibey pet works as a companion model as is. Model textures, animations, locators,
sprites, embedded sounds and animation events (particles, sounds, bone visibility) all work like on a pet.

```json
"variants": [
  { "id": "classic", "name": "@variant.classic" },
  { "id": "emperor", "name": "@variant.emperor", "textures": { "main": "textures/penguin_emperor.png" } }
]
```

- The first variant is the default. A variant replaces model textures **by texture id** with PNGs from the pack (same
  UV size as the bundle texture; HD = larger with the same aspect, like in the bundle).
- Without `variants` the companion has one variant, `default`.

## Animations

The companion's movement picks a **base animation**; behaviors can replace it or play **layers** on top.

```json
"animations": {
  "idle": "idle", "walk": "walk", "run": "sprint", "swim": "swim", "swim_idle": "float",
  "fly": "fly", "fall": "glide", "sit": "sit", "sleep": "sleep", "held": "dangle", "ride": "sit"
}
```

| Key | Plays while | If missing |
|---|---|---|
| `idle` | standing | the model's `idle` (required somewhere) |
| `walk` | walking; blended in by speed, played faster when faster | `idle` |
| `run` | moving faster than halfway between walk and run speed | `walk` |
| `swim` | moving in water | `walk` |
| `swim_idle` | floating in water | `swim` |
| `fly` | flying (only `movement.canFly`) | `fall` |
| `fall` | in the air for more than 0.3 s | `idle` |
| `sit` | `sit` movement / the *Sit* command | `idle` |
| `sleep` | `sleep` movement | `sit` |
| `held` | being carried by the player (middle mouse) | `fall` |
| `ride` | on the player's shoulder or head | `sit` |

Values are names of animations in the model. Base animations cross-fade over 0.2 s. Model animations named like the
keys are used automatically (a model with `idle`, `walk`, `swim` needs no `animations` block at all).

**Layers.** A behavior's `anim` and the `anim` action play animations on top of the base:

| `blend` | Effect |
|---|---|
| `add` (default for layers) | the layer's keyframes are added to the base (a clap that only keys the flippers keeps the body breathing) |
| `replace` | the base fades out while the layer plays (a full-body trick) |

Layers fade in/out over `fade` seconds (default 0.15). A layer started by the `anim` **action** plays once and fades
out at its end (looping ones until the behavior ends or `stop_anim`). A behavior's own `anim` field lasts for the
whole behavior: a looping animation loops, a non-looping one plays once and **holds its last frame** until the
behavior ends (open the umbrella, then keep it up). Up to 4 layers at once. Animation events in the model
(particles, sounds, visibility) fire for the base and every layer with weight ≥ 50 %.

## Props

Props are bones of the model that behaviors show and hide: an umbrella, a sou'wester, a beach ball, a party hat.

```json
"props": {
  "umbrella": { "bones": ["umbrella"] },
  "rain_hat": { "bones": ["hat_brim", "hat_top"], "shown": false },
  "scarf":    { "bones": ["scarf"], "shown": true }
}
```

- `bones`: one or more model bones (their children follow). `shown` (default `false`): the state at spawn.
- Hidden bones are not drawn at all. Show/hide with the behavior field `props` (only while the behavior runs) or the
  `prop` action (stays until changed). Animate props like any bone (open the umbrella in the layer that shows it).
- Expressions read the state with `prop("umbrella")`.

## Sounds

```json
"sounds": {
  "honk":   { "file": "sounds/honk.ogg", "volume": 0.8, "pitch": [0.9, 1.15] },
  "splash": { "sound": "minecraft:entity.generic.splash", "volume": 0.35 },
  "steps":  { "sound": "minecraft:block.snow.step", "volume": 0.2, "pitch": 1.3 }
}
```

- `file`: OGG Vorbis from the pack (mono = positional, ≤ 512 KiB), or `sound`: any vanilla/mod sound event id.
- `volume` (0–2, default 1), `pitch` (number or `[min, max]` random, 0.5–2, default 1).
- Pack sound files are also added to the model as embedded sounds with the file name as id (`sounds/honk.ogg` →
  `honk`), so animation events in the model can play them too.
- Everything plays at the companion's position, scaled by the player's companion volume setting.

## Particles

```json
"particles": {
  "hearts": { "particle": "minecraft:heart", "count": 3, "locator": "heart", "spread": [0.25, 0.1, 0.25], "velocity": [0, 0.05, 0] },
  "splash": { "particle": "minecraft:splash", "count": 12, "spread": [0.4, 0.1, 0.4], "velocity": [0, 0.15, 0] },
  "shiver": { "particle": "minecraft:snowflake", "count": 2, "locator": "body", "spread": [0.3, 0.2, 0.3] }
}
```

`particle`: a vanilla particle id with no options (`heart`, `happy_villager`, `note`, `splash`, `bubble`, `snowflake`,
`cloud`, `poof`, `end_rod`, `glow`, …). `locator`: a model locator (default: the companion's center). `spread` in blocks,
`velocity` in blocks/tick (+X = the companion's right, like FORMAT.md), `count` 1–32. Sprite particles from the
model's `sprites` are fired from animation events (FORMAT.md).

## Movement

All optional. Speeds in blocks per second, distances in blocks, angles in degrees.

```jsonc
"movement": {
  "walkSpeed": 3.2,            // 4.3 = player walking
  "runSpeed": 6.5,
  "swimSpeed": 2.6,
  "flySpeed": 0,               // > 0 together with canFly
  "canSwim": true,             // false: avoids water like lava, gets out fast
  "canFly": false,
  "jump": 1.1,                 // highest step it jumps up (1.1 = one block)
  "step": 0.6,                 // walks up this without jumping (slabs, snow)
  "turnSpeed": 300,            // body turn, degrees per second
  "follow": { "near": 1.8, "far": 4.5, "run": 9, "teleport": 24 },
  "wander": 5,                 // radius around home for "wander"
  "shoulder": [0.42, 1.38, 0.05],   // seat on the player's right shoulder (x right, y up, z back), at size 1
  "head": [0, 1.95, 0]              // seat on the player's head
}
```

The companion is client-side only: it is drawn by your game, has simple physics (gravity, water, steps), finds paths
over blocks your client knows (never across a drop of more than 3 blocks, never into lava or fire) and teleports next
to you when it is more than `follow.teleport` blocks away, loses its chunk or gets stuck for 3 s. It never touches the
world, pushes nobody and is never seen by the server.

**Movement modes** (behavior field `move`):

| `move` | What it does |
|---|---|
| `follow` (default) | stays between `near` and `far` from the player, walks, runs beyond `run`, swims after you |
| `near` | goes to a spot about `near` blocks next to the player and stays there (until the player is `far` away) |
| `stay` | stands where it is |
| `sit` | stays and plays the `sit` base animation |
| `sleep` | stays and plays the `sleep` base animation |
| `wander` | short random walks around its home (where it was told to stay, or the player) |
| `swim` | goes into water within 8 blocks and paddles around; like `follow` if there is none |
| `flee` | runs away from what it fears: the nearest of lava, fire, a mob (see `near.*`) – or from the player |
| `hop` | like `follow`, but jumps all the time (excited) |
| `circle` | runs circles around the player (a trick) |
| `shoulder` | climbs onto the player's right shoulder and rides there |
| `head` | climbs onto the player's head and rides there |
| `fly` | like `follow`, in the air (`canFly` only) |
| `none` | keeps its current motion, physics only |

Player commands override modes: **Stay** turns `follow`, `near`, `hop`, `fly` and `circle` into `stay` and keeps
`wander` around the stay spot; **Sit** turns every mode except `shoulder`, `head`, `flee` and `sleep` into `sit`. A
behavior with `"obeys": false` ignores commands (tricks, fleeing).

## Looking

```json
"look": { "bone": "head", "maxYaw": 55, "maxPitch": 30, "speed": 240 }
```

The `bone` (usually the head) turns towards what the companion looks at, added on top of every animation. Beyond
`maxYaw` the whole body turns. `speed` in degrees per second. Without `look` only the body turns. Behavior field
`look`: `player` (default while standing within 8 blocks), `move` (where it goes), `sky` (up, e.g. at the rain),
`around` (random glances), `none`.

## Needs and variables

```json
"needs": {
  "energy":    { "start": 1.0, "perMinute": -0.04 },
  "happiness": { "start": 0.7, "perMinute": -0.02 },
  "curiosity": { "start": 0.5, "perMinute": 0.03 },
  "hunger":    { "start": 0.2, "perMinute": 0.015 }
}
```

- A need is a number from 0 to 1 that drifts by `perMinute` all the time; behaviors change it while they run
  (`needs` field) or once (`need` action). Names are free (`[a-z_]{1,24}`, at most 8). Read with `need.energy`.
- Needs are saved with the companion, so a tired seal is still tired tomorrow.
- `"vars": { "tricks_done": 0 }` declares own numbers (at most 16), read `var.tricks_done`, changed by the `var`
  action. Vars are saved too.

## Behaviors

The brain is a list of behaviors. Every tick (20 times a second) companio looks for the behavior with the highest
`priority` whose condition holds and runs it. A behavior says how the companion moves, what it looks at, which
animation and props it shows, and which actions happen when it starts, while it runs and when it ends.

```jsonc
{
  "id": "umbrella",                 // [a-z0-9_]{1,32}, unique in the companion
  "priority": 60,                   // higher wins, 0–1000
  "when": "weather.rain && world.sky",   // condition (expression), default "true"
  "while": "weather.rain",          // keep running while this holds (default: "when")
  "on": null,                       // an event name: start only when it happens (see Events)
  "duration": [20, 40],             // seconds, number or [min, max]; default: until "while" is false
  "minDuration": 3,                 // never ends earlier (default 1)
  "cooldown": 30,                   // seconds before it may start again after it ended
  "weight": 1,                      // chance between behaviors of the same priority
  "interrupt": true,                // false: higher priorities wait until it ends
  "obeys": true,                    // false: ignores Sit/Stay
  "move": "follow",                 // movement mode
  "look": "player",                 // player | move | sky | around | none
  "anim": "umbrella_hold",          // a layer while it runs, see below
  "baseAnim": null,                 // replaces the base animation while it stands still
  "props": { "umbrella": true },    // shown while it runs
  "needs": { "happiness": 0.05 },   // change per minute while it runs
  "start": [ … ],                   // actions when it starts (may be delayed with "at")
  "repeat": [ { "every": [3, 6], "actions": [ … ] } ],   // actions while it runs
  "end": [ … ]                      // actions when it ends
}
```

**Choosing.** Candidates are behaviors that are not on cooldown, whose `when` is true and that either have no `on`
or whose `on` event happened this tick. The best candidate (highest priority; between equals by `weight`) replaces
the running behavior if that one has ended, or if the candidate has a higher priority and the running one has
`interrupt: true`. A running behavior ends when its `duration` is over, or – without `duration` – when `while` turns
false after `minDuration`, or with the `stop` action. Behaviors with `on` that have no `duration` last as long as
their `anim` layer (or 3 s). If nothing applies the companion simply follows the player's command (follow / stay /
sit) with its base animations.

**`anim`.** A model animation name, or an object: `{ "name": "clap", "blend": "add", "fade": 0.15, "speed": 1 }`. It
plays as a [layer](#animations) for the whole behavior. `baseAnim` replaces the base animation instead, but only while
the companion stands still (so a "sleep next to you" behavior walks over with `walk` and then plays `sleep`); use
`"move": "sleep"` + the `sleep` animation key for the common case.

**Repeat.** `"every"` is seconds (number or `[min, max]`, random each time); the first run happens after one interval.
Up to 4 repeat entries per behavior.

## Events

`on` names one of these. Most are noticed by companio itself (by comparing the world from tick to tick); `pet`,
`pick_up`, `place`, `rename` and the commands come from the player's interaction.

| Event | When |
|---|---|
| `spawn` | the companion appears (joining a world, picked in the settings, after reload) |
| `pet` | the player looks at it and left-clicks |
| `pick_up` / `place` | the player grabs it with the middle mouse / lets it go |
| `rename` | renamed (name tag or settings) |
| `command_sit`, `command_follow`, `command_stay` | the player picked that in the menu |
| `owner_hurt` | the player took damage |
| `owner_low_health` | the player's health fell below 30 % |
| `owner_eat` | the player started eating or drinking |
| `owner_jump` | the player jumped |
| `owner_sleep` / `owner_wake` | the player lay down in a bed / got up |
| `owner_look` | the player started looking at it |
| `item_change` | the player switched the item in the main hand |
| `rain_start` / `rain_stop` | rain (or snow) began / stopped where the companion is |
| `thunder_start` | a thunderstorm began |
| `night_start` / `day_start` | dusk / dawn (time 13000 / 23000) |
| `enter_water` / `leave_water` | the companion went into / out of water |
| `land` | it landed after falling more than 1 block |
| `teleport` | it teleported to the player |
| `companion_near` | another companion came within 4 blocks |

Every event also stamps `since("<event>")`.

## Actions

Lists of actions run in `start`, `repeat` and `end`. Each action is an object with `"do"` and its fields. **Every**
action may also have:

| Field | Meaning |
|---|---|
| `if` | expression; the action is skipped when false |
| `chance` | 0–1; probability that it happens |
| `at` | seconds after the list started (delayed; cancelled when the behavior ends early) |

| `do` | Fields | Effect |
|---|---|---|
| `anim` | `name`, `blend` (`add`/`replace`), `fade`, `speed` | play a [layer](#animations) once (looping animations: until the behavior ends) |
| `stop_anim` | `name` | fade that layer out |
| `prop` | `name`, `show` (bool) | show/hide a prop (stays after the behavior) |
| `sound` | `name` | a sound from `sounds`, or a sound id directly (`"minecraft:entity.cat.purr"`); `volume`, `pitch` override |
| `particle` | `name` | a burst from `particles`, or a particle id directly; `count`, `locator`, `spread`, `velocity` override |
| `say` | `text` (text or list of texts → random), `seconds` (default 2.5) | a speech bubble above it |
| `need` | need names with deltas, e.g. `"energy": -0.1` | change needs once (clamped to 0–1) |
| `var` | `name`, `value` (expression or number) | set a variable, e.g. `"value": "var.tricks_done + 1"` |
| `cooldown` | `name`, `seconds` | start a named cooldown (`cooldown("name")` is false until it is over) |
| `behavior` | `id` | switch to that behavior now (if it is not on cooldown) |
| `stop` | – | end the current behavior |
| `move` | `mode` | change the movement mode while the behavior runs |
| `look` | `at` | change the look target while the behavior runs |
| `hop` | `height` (blocks, default 0.5) | a little jump |
| `random` | `of`: list of action lists | run one of them, picked at random |

## Expressions

Conditions (`when`, `while`, `if`) and `var` values are small expressions. They are checked when the pack is loaded –
an unknown name or a typo is an error with a suggestion (`unknown variable "wether.rain", did you mean
"weather.rain"?`). There are no loops, no assignments and no access to anything but the values below.

```
weather.rain && !prop("umbrella")
time.night && need.energy < 0.4 && player.distance < 6
holding("minecraft:cod", "minecraft:salmon") && chance(0.3)
biome("snow", "frozen", "ice") || world.cold
self.inWater ? need.happiness > 0.2 : false
```

**Operators:** `!` `-` · `*` `/` `%` · `+` `-` (`+` joins strings) · `<` `<=` `>` `>=` · `==` `!=` · `&&` `||` ·
`a ? b : c`, parentheses; `and`, `or`, `not` work as words too. Literals: numbers (`0.5`), strings (`'text'` or
`"text"`), `true`, `false`. Values are numbers, strings or booleans; `0`, `""` and `false` are false.

**Values**

| Name | Type | Meaning |
|---|---|---|
| `weather.rain` | bool | rain or snow falls where the companion is (also when it is under a roof – see `world.sky`) |
| `weather.snow` | bool | `weather.rain` and it is cold there (snow instead of rain) |
| `weather.thunder` | bool | thunderstorm |
| `weather.clear` | bool | no rain |
| `self.wet` | bool | rain reaches it (sky above) or it is in water |
| `time.day`, `time.night` | bool | day 23000–13000, night 13000–23000 |
| `time.dawn`, `time.dusk` | bool | 22500–24000+500, 11500–13500 |
| `time.hour` | number | 0–24, 0 = midnight, 6 = sunrise |
| `world.dimension` | string | e.g. `minecraft:overworld`, `minecraft:the_nether` |
| `world.biome` | string | biome id at the companion, e.g. `minecraft:snowy_plains` |
| `world.cold`, `world.hot` | bool | biome temperature < 0.15 (snow) / > 1.0 (desert, nether) |
| `world.light` | number | 0–15 at the companion |
| `world.sky` | bool | open sky above the companion |
| `player.sneaking` `.sprinting` `.swimming` `.elytra` `.flying` `.sleeping` `.eating` `.onGround` `.inWater` `.riding` | bool | the player's state (`flying` = creative flight) |
| `player.hurt` | bool | hurt within the last second |
| `player.health` | number | 0–1 |
| `player.idle` | number | seconds without moving or turning (AFK ≈ `player.idle > 60`) |
| `player.speed` | number | blocks per second |
| `player.distance` | number | distance to the companion |
| `player.looking` | bool | the crosshair is on the companion |
| `player.held`, `player.offhand` | string | item ids in the hands (`minecraft:air` when empty) |
| `self.speed` | number | blocks per second |
| `self.moving`, `self.onGround`, `self.inWater`, `self.onIce`, `self.onSnow` | bool | its own state |
| `self.held` | bool | the player carries it |
| `self.riding` | bool | on the shoulder or head |
| `self.command` | string | `follow`, `stay` or `sit` |
| `self.behavior` | string | id of the running behavior |
| `self.time` | number | seconds the running behavior has run |
| `self.age` | number | seconds since it appeared |
| `self.size` | number | the size the player picked |
| `self.variant` | string | id of the variant |
| `near.water` `near.lava` `near.fire` | number | distance to the nearest such block within 8 blocks, `99` if none |
| `near.mob` | number | distance to the nearest hostile mob within 8 blocks, `99` if none |
| `near.player` | number | distance to the nearest other player, `99` if none |
| `near.companion` | number | distance to the nearest other companion, `99` if none |
| `need.<name>` | number | a declared need, 0–1 |
| `var.<name>` | number | a declared var |

Fair play: `near.*` only counts blocks and mobs your player can see (a clear line from your eyes) – a companion never
tells you about lava behind a wall or a creeper in a cave.

**Functions**

| Function | Result |
|---|---|
| `chance(p)` | true with probability `p` **per second** (independent of how often it is checked) |
| `random()`, `rand(a, b)` | random number 0–1 / a–b |
| `min(a, b)`, `max(a, b)`, `abs(x)`, `clamp(x, a, b)` | math |
| `cooldown("name")` | true when the named cooldown (action `cooldown`) is over or was never started |
| `since("event")` | seconds since that [event](#events) (or since behavior `<id>` started: `since("behavior:nap")`), 1000000 if never |
| `holding("id", …)` | the player holds one of these items (main or off hand); `*` is a wildcard: `"minecraft:*_fish"` |
| `biome("part", …)` | `world.biome` contains one of the parts |
| `is(x, a, b, …)` | `x` equals one of the values |
| `prop("name")` | that prop is shown |
| `hour(from, to)` | `time.hour` is in `[from, to)`, wrapping at midnight: `hour(22, 6)` |

## Tricks

```json
"tricks": [
  { "id": "ball", "name": "@trick.ball", "icon": "⚽", "behavior": "trick_ball" },
  { "id": "dance", "name": "@trick.dance", "icon": "♪", "behavior": "trick_dance" }
]
```

Tricks are listed in the companion's right-click menu (and in the settings). Picking one starts its `behavior` right
away, over any running behavior (its `when` must hold and it must not be on cooldown, else the companion shakes its
head with a `?` bubble). Up to 8.

## What the player can do

| Gesture | Effect |
|---|---|
| look at it | `player.looking`, event `owner_look`; the companion looks back (look `player`) |
| look + left click | **pet** → event `pet` |
| hold middle mouse on it | **pick it up** (event `pick_up`, base animation `held`), look somewhere, release → **put down** there (event `place`) |
| wheel on it | bigger / smaller within `size` |
| right click | menu: **Sit**, **Follow**, **Stay**, **Tricks**, **Name** |
| right click with a named name tag | **rename** (the item is not used up; event `rename`) |

Stay remembers the spot as the companion's *home* (`wander` walks around it).

## Multiplayer

On a server with Vibey Client's relay, other players with Vibey see your companion if they have the same pack (same
id **and** the same content hash; built-in packs always match). They see its position, base and layer animations,
props, size, variant, name and speech bubbles; the brain only runs on your client. Servers can hide companions with
the server rule `companion.show` (e.g. minigames).

## Limits and validation

| Rule | Limit |
|---|---|
| Pack size | ≤ 32 MiB in total, ≤ 512 files |
| Companions per pack | 1–16 |
| Model | a valid `.vibey` (FORMAT.md limits), ≤ 2 MiB |
| Variant textures | PNG ≤ 512×512, same aspect as the texture they replace |
| Sounds | OGG Vorbis ≤ 512 KiB each, ≤ 32 per companion |
| Behaviors | ≤ 64 per companion, ≤ 32 actions per list, ≤ 4 `repeat` entries, `at` ≤ 30 s |
| Expressions | ≤ 400 characters, nesting ≤ 24 |
| Props / needs / vars / tricks / layers | ≤ 16 / 8 / 16 / 8 / 4 |
| Speech bubbles | ≤ 40 characters shown |
| Ids | `[a-z0-9_]{1,32}` (pack id also `.` and `-`, ≤ 48) |

**Errors** stop the companion (or the pack) from loading. **Warnings** load anyway. Both are listed in the pack screen
(click a pack) and, after a reload, in a small panel at the top left of the game – each line as
`file: path → message`, e.g.

```
companions/penguin.json: behaviors[2].when → unknown variable "wether.rain", did you mean "weather.rain"?
companions/penguin.json: props.umbrella.bones[0] → the model has no bone "umbrela" (bones: body, head, umbrella, …)
models/penguin.vibey → textures[0]: PNG is 1024×1024, at most 512×512
```

Unknown fields are ignored with a warning (`unknown field "prioirty"`), so newer packs still load in older versions.

## Workflow: make a pack

1. Build the model in Blockbench (Vibey plugin, slot *Pet*) or with the content toolkit (`content/`), with the
   animations you need (`idle`, `walk`, … and your layers), locators for particles and the prop bones.
2. Make a folder in `.minecraft/companionpacks/`, put the `.vibey` into `models/` and write `companion.json` +
   `companions/<id>.json`.
3. In game: pack screen (Vibey menu → Companion → *Packs*, or `/companio packs`) → activate it → pick the companion.
4. Edit and save – **folder packs reload by themselves** (checked every second); the *Reload* button and
   `/companio reload` do it at once. Errors show at the top left, the companion keeps running with the last good
   version until the pack loads again.
5. `/companio debug` shows the running behavior, needs and the last events above the companion.
6. Check without the game: `java -cp "<companio, cosmetico, r3, vui core jars>;gson.jar;joml.jar"
   dev.henny.companio.tool.CheckPack <folder or zip>` prints every error and warning (exit code 1 on errors). The
   built-in packs are checked by companio's JUnit tests.

## Full example

A small penguin that follows, waddles, opens an umbrella in the rain, naps at night next to you and honks when you
pet it. (The real penguin pack does much more.)

`companion.json`

```json
{ "format": "companio-pack", "formatVersion": 1, "id": "tiny_penguin", "version": "1.0.0",
  "name": "@pack.name", "description": "@pack.description", "author": "You",
  "companions": ["companions/penguin.json"] }
```

`lang/en_us.json`

```json
{ "pack.name": "Tiny Penguin", "pack.description": "A penguin with an umbrella.",
  "penguin.name": "Penguin", "say.rain": "☂ Rain!", "say.night": "Zzz…", "say.pet": ["♥", "Honk!", "♥ ♥"] }
```

(a list in a lang file is allowed for `say` texts: one is picked at random)

`companions/penguin.json`

```json
{
  "id": "penguin",
  "name": "@penguin.name",
  "model": "models/penguin.vibey",
  "scale": 0.8,
  "size": { "default": 1, "min": 0.7, "max": 1.3 },
  "box": { "width": 0.5, "height": 0.8 },
  "look": { "bone": "head", "maxYaw": 60, "maxPitch": 30 },
  "movement": { "walkSpeed": 2.8, "runSpeed": 5.5, "swimSpeed": 4, "canSwim": true },
  "props": { "umbrella": { "bones": ["umbrella"] } },
  "sounds": {
    "honk": { "file": "sounds/honk.ogg", "volume": 0.7, "pitch": [0.9, 1.2] },
    "pop": { "sound": "minecraft:entity.chicken.egg", "volume": 0.4, "pitch": 1.6 }
  },
  "particles": { "hearts": { "particle": "minecraft:heart", "count": 3, "locator": "head", "spread": [0.2, 0.1, 0.2] } },
  "needs": { "energy": { "start": 1, "perMinute": -0.05 } },
  "behaviors": [
    {
      "id": "umbrella", "priority": 60,
      "when": "weather.rain && world.sky", "minDuration": 4,
      "props": { "umbrella": true }, "anim": "umbrella_hold",
      "start": [ { "do": "anim", "name": "umbrella_open" }, { "do": "sound", "name": "pop", "at": 0.3 },
                 { "do": "say", "text": "@say.rain", "chance": 0.5 } ]
    },
    {
      "id": "nap", "priority": 40,
      "when": "time.night && need.energy < 0.6 && !player.sprinting",
      "move": "near", "baseAnim": "sleep", "look": "none",
      "needs": { "energy": 0.6 },
      "repeat": [ { "every": [5, 9], "actions": [ { "do": "say", "text": "@say.night", "seconds": 2 } ] } ]
    },
    {
      "id": "petted", "priority": 80, "on": "pet", "cooldown": 1,
      "anim": { "name": "flap", "blend": "replace" },
      "start": [ { "do": "sound", "name": "honk" }, { "do": "particle", "name": "hearts" },
                 { "do": "say", "text": "@say.pet" } ]
    }
  ]
}
```

That's a complete, working pack. Add `variants`, `tricks`, more `needs` and behaviors from the seal pack as you go.
