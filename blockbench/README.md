# Vibey Client – Blockbench plugin

`vibey.js` adds everything needed to make **Vibey Client** cosmetics and emotes in [Blockbench](https://blockbench.net)
(desktop app, 5.x) and to push them live into the running game.

- **New item wizard** (type → slot → name → start empty or from a starter item) and the **Vibey guide**, a checklist
  with automatic checks that walks you from the first cube to *Test live in game* and *Export / Submit*
- Model formats **Vibey Cosmetic** and **Vibey Emote** (box UV + per-face UV, bone rig, locators, animation mode)
- **Build on the player**: the grey reference player (original texture, vanilla proportions and pivots) has
  ready-made groups – ◆ Head, ◆ Above head, ◆ Body, ◆ Back, shoulders, ◆ Hips, arms, legs, ◆ Around / Beside
  player. Put your part into the group it belongs to (drag, right-click › *Attach to …*, or let it snap by itself)
  and it moves with that part in game; the groups are never exported
- Import / export of `.vibey` bundles (`docs/SPEC.md` §5) with embedded PNG textures (incl. animated strips),
  OGG sounds and particle sprites
- Particle, sound and visibility events as Blockbench effect keyframes, edited in the Keyframe panel
- Validation and errors in plain words with a fix (technical details folded away), also in Blockbench's validator
- Live link (`docs/SPEC.md` §8): find the game automatically, push, play emote, auto-push on change, reconnect
- Submit to Vibe and My submissions through the running game
- English and German, following Blockbench's language setting

Full guide with screenshots: [`docs/BLOCKBENCH.md`](../docs/BLOCKBENCH.md).

## Install

The Vibe Launcher installs it for you. By hand: download `vibey.js` from the newest **Vibey Blockbench Plugin**
release ([GitHub releases](https://github.com/vibec-studios/vibey-client/releases), tags `blockbench-v<version>`).

1. Blockbench > **File > Plugins…** > the "Load Plugin from File" icon (top right of the dialog) > pick
   `vibey.js` and confirm.
2. The file name must stay `vibey.js` (Blockbench uses it as the plugin id).
3. To update, load the new file the same way (or use *Reload* in the plugin list).

The plugin is desktop only (`variant: "desktop"`); it needs Blockbench 5.0 or newer and, for the live link and
*Submit to Vibe*, Vibey Client 0.2.0 or newer. Wardrobe thumbnails, the optional animations `sprint` / `glide` /
`idle_long` / `happy` and `offsetWhen` need Vibey Client 0.3.0; with an older client the plugin says once what the game
ignores (it reads the version from the live link's `welcome`).

## Getting started

1. After installing, the **New Vibey item** wizard opens by itself (once). Later: **Vibey > New item…**
   (German: *Vibey > Neues Item…*) or *File > New > Vibey Cosmetic / Vibey Emote*.
2. Pick *Cosmetic* or *Emote*, the slot (cape, wings, aura, pet, shield, hat, back) or the emote options, a name, and
   *Start empty* or a starter item as template. The plugin puts your group into the right player group (wings →
   ◆ Back, hat → ◆ Head, pet → ◆ Beside player), creates empty animations (`idle`/`walk`/`fly`/`sneak` per slot,
   `main` for emotes) and a texture (vanilla cape/shield layout with a colour guide for texture-only slots).
   Build on the player – see [docs/BLOCKBENCH.md → Build on the player](../docs/BLOCKBENCH.md#build-on-the-player).
3. The **Vibey guide** in the left sidebar takes over: model → texture → animate → particles & sounds (optional) →
   **Test live in game** → export / submit. Each step checks your project ("No keyframes in idle yet", "Not connected to
   the game") and can be ticked off; it scrolls inside its panel on small screens. Reopen it with **Vibey > Tutorial**
   (*Anleitung*).
4. **Vibey > Test live in game** (*Live im Spiel testen*) finds the running game by itself. If it can't: start
   Minecraft with Vibey Client, open the Vibey menu (Right Shift) → **Modules → Blockbench link** (from Vibey Client
   0.3.0 under **For creators**) → turn on **Creator mode**, and click again.
5. **Vibey > Export / Submit…** (*Exportieren / Einreichen…*) saves a `.vibey` file or submits the item to Vibe.
   Everything else is under **Vibey > Advanced ▸** (*Erweitert*).

Full guide: [`docs/BLOCKBENCH.md`](../docs/BLOCKBENCH.md).

## Development

```sh
cd blockbench
npm install
npm test                 # unit tests (core, wizard, guide, strings, player groups) + mock game server tests (Node ≥ 22, node:test)
npm run test:blockbench  # drives the installed Blockbench over CDP (isolated profile), see below
npm run shots            # review screenshots of wizard, guide, menu and errors (EN + DE) -> review/wizard.html
npm run embed-assets     # re-embeds the wizard thumbnails + starter templates from content/dist into vibey.js
```

`npm run test:blockbench` starts the installed Blockbench (`%LOCALAPPDATA%\Programs\Blockbench\Blockbench.exe`,
override with `--blockbench <exe>`) with a **temporary, isolated profile** (`--userData` / `--user-data-dir`) and
`--remote-debugging-port`, loads the plugin, checks that the wizard opens on the first start, builds a cosmetic and an
emote project through the real APIs and dialogs, exports and validates both bundles, connects the live link to the mock
game server (`test/mock-game-server.js`), checks push / auto-push / reconnect / token errors, re-imports the files and
compares them byte for byte. It checks the player groups (labels, right-click *Attach to …*, auto-assign and its
undo, outliner drag, groups snapping back), imports every starter `.vibey` and compares the re-export byte for byte,
and opens `.bbmodel` projects made by the released 0.2.4 plugin (from the `blockbench-v0.2.4` tag) to check that they
convert and export the same bytes. It then runs the wizard through the real dialog, creates every slot and an emote (and
items from templates) and validates their exports, checks the menu, the guide's automatic checks, *Test live in game*
with and without a game, the friendly error dialogs and *Submit to Vibe*. Screenshots go to `test/out/screenshots/`
(`--docs-screenshots` writes them to `docs/screenshots/`). Only the Blockbench process it started is closed
afterwards; `test/bb-harness.js` holds the shared launcher.

Against the **real game** (Creator mode on):

```sh
npm run test:blockbench -- --game-dir "%APPDATA%\.minecraft"      # uses <dir>/vibeyclient/devlink.json
npm run test:blockbench -- --devlink C:\path\to\devlink.json
```

Other options: `--keep-open`, `--no-screenshots`, `--lang de` (run both languages before a release). With
`--game-dir` the run also submits the wizard item through the game (*Submit to Vibe*, *My submissions*, withdraw) –
use a dev client signed in to a **local** backend (`-Dvibeyclient.devAuth=true -Dvibeyclient.api=http://127.0.0.1:<port>`,
backend with `AUTH_DEV_BYPASS=1`), never production. To check compatibility with an older client, run that tag's
client (`git archive v0.2.1 | tar -x -C <dir>`, then `./gradlew :fabric-1.21.11:runClient` there) and point
`--game-dir` at its run directory; the "older game" check then expects the 0.3.0 note.

## Releases

Standalone GitHub releases, tag `blockbench-v<version>`, assets `vibey.js` + `vibey-blockbench.json`
(`tools/release-json.mjs`, notes from [`CHANGELOG.md`](CHANGELOG.md)). Steps: [docs/BLOCKBENCH.md →
Releases](../docs/BLOCKBENCH.md#releases).

`npm run shots` uses the same harness and the mock game to photograph every wizard step, the guide at several steps
(including failing checks and *Test live in game* connected / not connected), the menu, *Export / Submit* and the
friendly errors, in English and German, and writes one self-contained page `review/wizard.html` (git-ignored, the
images are embedded).

The mock game server can also run standalone for manual testing:

```sh
node test/mock-game-server.js --port 47821 --game-dir C:\temp\fakegame   # writes C:\temp\fakegame\vibeyclient\devlink.json
```

Then set *Vibey > Live link settings… > Game directory* to `C:\temp\fakegame`.

**In-game check** of the player groups against a dev client (`-Dvibeyclient.devDriver=<port>`, a creative world
with cheats): `node test/ingame-check.js --game-dir <repo>/versions/fabric-1.21.11/run --driver <port>` builds test
items on ◆ Back, ◆ Right arm, ◆ Head, ◆ Above head, ◆ Left leg and a pet on ◆ Beside player in the real Blockbench,
pushes them over the live link and photographs standing, head turned, sneaking (forced crouch under a slab), walking
(pushed by a water current) and the pet (screenshots in `test/out/ingame/`).

### Code layout

`vibey.js` is one file with two parts:

- **Core** (top): pure functions without Blockbench – snapshot → bundle compiler, bundle → snapshot decompiler,
  SPEC validator, keyframe conversion, box UV, devlink helpers. Exported as `module.exports` under Node and as
  `window.VibeyCore` in Blockbench.
- **Player groups** ("build on the player") are pure core as well: `REF_GROUPS` (ids, attach, vanilla pivots, zones),
  `refTree`, `bestRef` (auto-assign / import placement), `upgradeSnapshot` (old projects) and the export rules in
  `compileBundle`; the Blockbench side (`Refs`) keeps the groups in place, colours them and handles right-click /
  auto-assign.
- The **wizard / guide core** is pure too: `starterSnapshot` (what the wizard creates per slot), `templateSnapshot`,
  `tutorialChecks`, `friendlyProblem`, the EN/DE `STRINGS` and the embedded `ASSETS` (64×64 thumbnails and a few
  starter bundles, generated by `tools/embed-assets.js` between the `vibey-assets` markers – do not edit by hand).
- **Blockbench integration** (bottom): formats, codec, wizard, guide panel, Vibey panel, dialogs, friendly errors,
  reference player, live link. Exposed for tests as `window.VibeyPlugin` while the plugin is loaded.

`test/spec-validator.js` is an independent validator written straight from SPEC §5 (it does not share code with the
plugin), used by the mock game server and the tests.

## License

The plugin code is Apache-2.0 (see `LICENSE` and `NOTICE` in the repository root). The reference player it draws, the
seal icon, the embedded starter thumbnails and template bundles (copied from `content/dist`) and the names
"Vibey"/"Vibe" are not covered: see `NOTICE` and
https://github.com/vibec-studios/.github/blob/main/LICENSING.md.
