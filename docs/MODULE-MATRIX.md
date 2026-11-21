# Modul-Kompatibilität und Server-Regeln

Erzeugt mit `node tools/module-matrix.mjs` aus den Modul-Targets und dem [Regelprotokoll](SERVER-RULES.md).

Die Tabelle zeigt, was jeder Build enthält und welche Funktionen ein Server über das Vibey-Protokoll einschränken kann. Die tatsächlichen Regeln legt der jeweilige Server fest.

| Modul | Fabric (Minecraft) | NeoForge (Minecraft) | Regel-Keys |
|---|---|---|---|
| `accessibility` | 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 | 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 | – |
| `camera` | 1.21.11 | – | `camera.freelook`, `freelook`, `camera.orbit`, `camera.*` |
| `chat` | 1.21.11 | – | – |
| `cinematic` | 1.21.11 | – | `cinematic.afk` |
| `clips` | 1.21.11 | – | `clips`, `clips.auto` |
| `companion` | 1.21.11 | – | `companion.show` |
| `core` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | `discord.server` |
| `cosmetics` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | `cosmetics.*`, `cosmetics.pets`, `cosmetics.capes`, `cosmetics.wings`, `cosmetics.auras`, `cosmetics.shields`, `cosmetics.hats`, `cosmetics.backs` |
| `devlink` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | – |
| `emotes` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | `emotes` |
| `hud` | 1.21.11 | – | `hud.*`, `hud.fps`, `hud.cps`, `hud.keystrokes`, `hud.coords`, `hud.armor`, `hud.effects`, `hud.clock`, `hud.ping`, `hud.server`, `hud.speed`, `hud.time`, `hud.items`, `hud.memory`, `hud.compass`, `hud.timer` |
| `info` | 1.21.11 | – | `info.*`, `info.light`, `info.chunks`, `info.card`, `info.food`, `info.saturation`, `info.toolwarn`, `info.tnt` |
| `inventory` | 1.21.11 | – | `inventory.tweaks`, `inventory.presets` |
| `media` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | – | – |
| `media.video` | 1.21.11 | – | – |
| `multiplayer` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | – |
| `nametags` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | `nametags.*`, `nametags.seal`, `nametags.ranks`, `nametags.styles` |
| `pvp` | 1.21.11 | – | `zoom`, `togglesprint`, `togglesneak`, `crosshair`, `blockoutline`, `hitcolor`, `lowfire`, `lowshield`, `hud.togglestate`, `hud.*` |
| `screens` | 1.21.11 | – | – |
| `serverapi` | 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | – |
| `servers` | 1.21.11 | – | – |
| `social` | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | `discord.server` |
| `social.streamer` | 1.21.11 | – | – |
| `visuals` | 1.21.11 | – | `visuals.*`, `visuals.fire`, `visuals.pumpkin`, `visuals.totem`, `visuals.particles` |
| `visuals.effects` | 1.21.11 | – | – |
| `visuals.items` | 1.21.11 | – | `visuals.helditem` |
| `waypoints` | 1.21.11 | – | `waypoints.minimap` |
