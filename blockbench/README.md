# Vibey Client – Blockbench plugin

`vibey.js` adds everything needed to make **Vibey Client** cosmetics and emotes in [Blockbench](https://blockbench.net)
(desktop app, 5.x) and to push them live into the running game.

- Model formats **Vibey Cosmetic** and **Vibey Emote** (box UV + per-face UV, bone rig, locators, animation mode)
- Grey reference player (original texture, vanilla proportions and pivots) – toggleable, never exported
- Import / export of `.vibey` bundles (`docs/SPEC.md` §5) with embedded PNG textures (incl. animated strips),
  OGG sounds and particle sprites
- Particle, sound and visibility events as Blockbench effect keyframes, edited in the Keyframe panel
- Validation with readable messages (Vibey > Validate, live in Blockbench's validator)
- Live link (`docs/SPEC.md` §8): find the game automatically, push, play emote, auto-push on change, reconnect

Full guide with screenshots: [`docs/BLOCKBENCH.md`](../docs/BLOCKBENCH.md).

## Install

1. Blockbench > **File > Plugins…** > the "Load Plugin from File" icon (top right of the dialog) > pick
   `blockbench/vibey.js` and confirm.
2. The file name must stay `vibey.js` (Blockbench uses it as the plugin id).
3. To update, load the new file the same way (or use *Reload* in the plugin list).

The plugin is desktop only (`variant: "desktop"`); it needs Blockbench 5.0 or newer.

## Quick start

- **File > New > Vibey Cosmetic** or **Vibey Emote** → fill in the properties dialog (id, names, slot/emote options…).
- Model on the reference player. In a cosmetic, each *root group* picks what it is attached to
  (Element panel > **Attach to**). In an emote, animate the player bones `root`, `head`, `body`, `right_arm`,
  `left_arm`, `right_leg`, `left_leg`; groups inside them are props.
- **File > Export > Vibey Bundle (.vibey)**, or **Vibey > Push to game** while Minecraft runs with Creator mode on.

## Development

```sh
cd blockbench
npm install
npm test                 # unit tests + mock game server tests (Node ≥ 22, node:test)
npm run test:blockbench  # drives the installed Blockbench over CDP (isolated profile), see below
```

`npm run test:blockbench` starts the installed Blockbench (`%LOCALAPPDATA%\Programs\Blockbench\Blockbench.exe`,
override with `--blockbench <exe>`) with a **temporary, isolated profile** (`--userData` / `--user-data-dir`) and
`--remote-debugging-port`, loads the plugin, builds a cosmetic and an emote project through the real APIs and
dialogs, exports and validates both bundles, connects the live link to the mock game server
(`test/mock-game-server.js`), checks push / auto-push / reconnect / token errors, re-imports the files and compares
them byte for byte, and writes screenshots to `docs/screenshots/blockbench-*.png`. Only the Blockbench process it
started is closed afterwards.

Against the **real game** (Creator mode on):

```sh
npm run test:blockbench -- --game-dir "%APPDATA%\.minecraft"      # uses <dir>/vibeyclient/devlink.json
npm run test:blockbench -- --devlink C:\path\to\devlink.json
```

Other options: `--keep-open`, `--no-screenshots`, `--lang de`.

The mock game server can also run standalone for manual testing:

```sh
node test/mock-game-server.js --port 47821 --game-dir C:\temp\fakegame   # writes C:\temp\fakegame\vibeyclient\devlink.json
```

Then set *Vibey > Live link settings… > Game directory* to `C:\temp\fakegame`.

### Code layout

`vibey.js` is one file with two parts:

- **Core** (top): pure functions without Blockbench – snapshot → bundle compiler, bundle → snapshot decompiler,
  SPEC validator, keyframe conversion, box UV, devlink helpers. Exported as `module.exports` under Node and as
  `window.VibeyCore` in Blockbench.
- **Blockbench integration** (bottom): formats, codec, panel, dialogs, reference player, live link. Exposed for tests
  as `window.VibeyPlugin` while the plugin is loaded.

`test/spec-validator.js` is an independent validator written straight from SPEC §5 (it does not share code with the
plugin), used by the mock game server and the tests.
