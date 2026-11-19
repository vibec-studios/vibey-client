# Changelog – Vibey Blockbench plugin

The plugin (`vibey.js`) is released on its own, independent of Vibey Client: GitHub release `blockbench-v<version>`
with the assets `vibey.js` and `vibey-blockbench.json` (docs/BLOCKBENCH.md, "Releases"). Versions follow
`blockbench/package.json` and `VERSION` in `vibey.js`.

Every version section starts with a `Kurz:` line: one short German sentence that becomes `notes` in
`vibey-blockbench.json` (shown by the Vibe Launcher). The rest of the section is the GitHub release text.

## 0.2.5

Kurz: Direkt am Spieler bauen: fertige Spielergruppen (Kopf, Rücken, Arme …) statt Ankerpunkten, und die Anleitung lässt sich jetzt scrollen.

Works with **Vibey Client 0.2.0 and newer** – the `.vibey` format is unchanged.

- **Build directly on the player.** The reference player now has ready-made groups like the real player:
  ◆ Head, ◆ Above head, ◆ Body, ◆ Back, ◆ Right/Left shoulder, ◆ Hips, ◆ Right/Left arm, ◆ Right/Left leg and
  ◆ Around player (◆ Beside player for pets). Build your part where it belongs and put it into the matching group –
  it moves with that part of the player in game. The per-group *Attach to* setting is gone.
- Three ways to attach: drag the part into a group in the outliner, **right-click › Attach to …** (German *An …
  anheften*), or just build – a new part outside the groups snaps to the most plausible group by itself, with a short
  hint ("Attached to ◆ Back – change it with right-click"); Ctrl+Z undoes only that step.
- Player groups are coloured in the outliner (the groups of your slot are highlighted, e.g. ◆ Back for wings) and
  never exported. A stray drag of a player group snaps back.
- Pets are built **beside the player**, where the game draws them, and exported relative to their follow point as
  before.
- The wizard shows which group each slot builds on, prepares your group there and selects it; starter templates open
  in the new structure. Emotes use the same groups (◆ Whole player, ◆ Head, ◆ Right arm …).
- **Old projects and `.vibey` files convert silently** into the new structure; re-exports stay byte-identical (the
  whole starter set and 0.2.4 projects are checked in the tests).
- The **Vibey guide scrolls** instead of running off the bottom of the window, and keeps the current step in view
  (checked at 1280×720 and 1366×768 with 100 / 125 / 150 % scaling).

## 0.2.4

Kurz: Neuer Item-Assistent, Schritt-für-Schritt-Anleitung und verständliche Fehlermeldungen; läuft ab Vibey Client 0.2.0 (Vorschaubilder, neue Animationen und offsetWhen ab 0.3.0).

First standalone release (earlier versions came with the client releases; 0.2.2 and 0.2.3 were never released).
Works with **Vibey Client 0.2.0 and newer** (live link, push, Submit to Vibe). The parts marked *0.3.0* below are
ignored by older clients; the plugin then says so once instead of failing.

- **New item wizard** (Vibey → New item…): type, slot, name with automatic id, empty or starter template; creates
  the project with the right bones, attach points, empty animations and a template texture.
- **Vibey guide** panel with automatic checks and a step-by-step **Tutorial**; **Test live in game** connects by
  itself and explains Creator mode when the game is not reachable; one **Export / Submit** dialog.
- Tidy **Vibey menu**: New item…, Tutorial, Test live in game, Export / Submit…, Advanced ▸.
- **Plain-language errors** with fixes; the technical details are folded away.
- *0.3.0:* pushes carry a viewport **thumbnail** for the wardrobe card.
- *0.3.0:* optional cosmetic animations **sprint, glide, idle_long, happy** (the wizard prepares glide for wings,
  idle_long and happy for pets) and **cosmetic.offsetWhen** (move aside while another slot is worn).
- The plugin reads the client version from the live link and, on an older client, tells you which parts of your
  item it ignores ("… need Vibey Client 0.3.0 – your game has 0.2.1").
- Menu paths in hints are right for Vibey Client 0.2.x (Vibey menu → Modules → Blockbench link) and 0.3.0
  (… → Modules → For creators → Blockbench link).

## 0.2.1

Kurz: Mit Vibey Client 0.2.1 ausgeliefert.

Shipped as an asset of the Vibey Client 0.2.1 release: Submit to Vibe and My submissions through the game's live
link, `.vibey` import/export, live link with auto-push.
