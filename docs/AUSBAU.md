# Expansion round (contract)

Integration branch `henny/client-ausbau` (from main 0.4.0-beta.1). Every agent works on its own branch from it
(`henny/ausbau-<package>`), commits there and never pushes to main. The coordinator merges. `docs/REBUILD.md` rules
still apply (small files, no comments, pure core + JUnit, less text, direct manipulation, licenses, PC load).

New code is Fabric 1.21.11 first. New modules/parts are declared in `gradle/modules.gradle` as `['fabric-1.21.11']`,
so every other target builds without them.

## Packages

| # | Package | Module / part | Owner of |
|---|---|---|---|
| 1 | Item models | `visuals.items` | `module/visuals/items/**`, `module/visuals/mixin/items/**` |
| 2 | Effects + particles | `visuals.effects` | `module/visuals/effects/**`, `module/visuals/particles/**`, `module/visuals/mixin/effects/**` |
| 3 | AFK cinematics + AFK screen | `cinematic` | `module/cinematic/**` |
| 4 | Screen editor (every vanilla screen) | `screens` | `module/screens/**` |
| 5 | Server/world list drag & drop, friends tab | `servers` (+ small api in `api/service`) | `module/servers/**` |

## Shared files (append only, keep your lines together, expect merges)

`module/BuiltinModules.java`, `gradle/modules.gradle`, `fabric.mod.json` mixin list, `assets/vibeyclient/lang/*.json`
(keybind names), `docs/MODULES.md`, `docs/SERVER-RULES.md`, `src/gametest/resources/fabric.mod.json` (entrypoints).
`module/visuals/VisualsModule.java` belongs to package 1; package 2 adds its settings through one call into its own
`effects/EffectsPart` (one line in `VisualsModule`).

## Fairness

Nothing may show what a vanilla player could not see. Camera features never pass through blocks and stay near the
player. Anything that could be an advantage (lower fire overlay, less visual noise in PvP, cinematic camera) has a
server rule (`docs/SERVER-RULES.md`), named `<module>.<feature>`.

## Done means

Feature works in game on fabric-1.21.11; JUnit for pure logic; one Fabric client gametest
(`-Dvibeyclient.test.only=<Name>GameTest -Dvibeyclient.test.relay=off`) that takes review screenshots into
`build/review/<package>/`; `checkI18n` + `checkModuleBoundaries` green; `:fabric-1.21.11:build` green; one other
target compiles (e.g. `:neoforge-1.21.1:compileJava`); module doc `docs/<MODULE>.md` updated.
