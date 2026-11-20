# Dev loop: change → reload → screenshot → check

Edit a `.vui` file and look at the result in the running game without restarting Minecraft. Built for AI agents
(and people) working from a shell: every step is one `curl` behind `scripts/vui-dev.sh`.

## Start the game once

```
bash C:/Users/henny/.t3/build-slots/slot.sh mc -- bash C:/Users/henny/.t3/build-slots/slot.sh gradle -- \
  ./gradlew --no-daemon --max-workers=2 :fabric-1.21.11:runClient          # keep it running
scripts/vui-dev.sh wait                                                    # until the bridge answers
```

`runClient` of the Fabric 1.21.x client targets passes `-Dvui.dev=true` and
`-Dvui.dev.roots=<repo>/src/main/resources;<repo>/libs/*/src/main/resources` (`gradle/vui-dev.gradle`), so `.vui`
files are read straight from the source folders. Another mod sets the same two properties in its own run config
(see [minecraft](minecraft.md#dev-mode)). `-Dvui.dev.port=<port>` (Gradle property of the same name for `runClient`)
moves the bridge if 7591 is taken; the script reads `VUI_PORT`.

## The loop

```
scripts/vui-dev.sh open vui:demo            # open a document as a screen (once)
# edit libs/vui/src/main/resources/assets/vui/vui/demo.vui and save
scripts/vui-dev.sh loop shot.png            # reload + errors + screenshot, prints "shot.png  errors=0  N ms"
# look at shot.png; on errors the JSON lists file:line:col, message and hint
```

Saving alone is enough (the watcher reloads within a tick); `loop` reloads explicitly so the screenshot is never of
the old file. Other commands:

| Command | Does |
|---|---|
| `tree [ns:path]` / `styles` | element tree with boxes, text, `at` (file:line:col); `styles` adds computed styles |
| `errors` | diagnostics of every open document |
| `click <id>` / `click '.tile:first-child'` | clicks the element's center |
| `key tab shift` / `type hello` | key press / typed text to the focused element |
| `screenshot out.png` | the whole game window as PNG |
| `reload` | rebuild all documents |

In game: `/vui open <id>`, `/vui demo`, `/vui reload`, `/vui hud`, `/vui inspect` (Ctrl+Shift+I), `/vui errors`.

## Measured

2026-11-20, Fabric 1.21.11 `runClient` (854×480 window), Windows, Intel Arc, a second Minecraft-free Gradle build
running on the same PC:

| Step | Time |
|---|---|
| edit `demo.vui` (sed) + `vui-dev.sh loop` (reload + errors + screenshot), 3 runs | **0.45 / 0.53 / 0.58 s** |
| of that, the bridge calls alone | 0.30 / 0.35 / 0.40 s |
| same with a typo in the file (2 errors with `file:line:col` + hint in the JSON and on screen) | 0.43 s |
| file saved → watcher swapped the tree (no `reload` call; client gametest `VuiGameTest`) | 46 ms |
| file saved → tree swapped → `/screenshot` PNG received (gametest) | 365 ms |

So the whole "change → reload → screenshot → check" loop takes **well under 1 second** (target: 10 s), without
restarting Minecraft. Starting the game once costs ~1 min warm (more on a fresh worktree).

## What reloads

Markup, styles, imported components and `.vui.json` files of every open document (screens, HUD widgets, overlays).
Controller state, scroll positions and focus survive. Java code and theme files need a restart (or `VuiMc.theme`).
A file with errors keeps the last good tree on screen and shows the errors on top; fix and save again.

## Checklist for an agent

1. `errors` must be `[]` for your document before you look at pixels.
2. `tree` shows exact boxes: check sizes/overflow there instead of guessing from the image.
3. `screenshot` and look at it: alignment, contrast, cut-off text.
4. Drive interactions with `click` / `key` / `type`, then `screenshot` again.
5. Never restart the game for markup or style changes.
