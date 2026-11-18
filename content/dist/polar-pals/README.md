# Polar Pals – lokal testen

Premium-Kollektion für Vibey Client (8 Items, alle 3D und animiert). **Nur lokal** – nichts davon liegt im Katalog
oder auf dem Server. Übersicht mit Ingame-Screenshots: [`polar-pals.html`](polar-pals.html) (offline, eine Datei).

| Datei | Slot | Item |
|---|---|---|
| `polar_seal.vibey` | Pet | Robbenbaby (legendär) |
| `polar_penguin.vibey` | Pet | Pinguin (episch) |
| `polar_owl_wings.vibey` | Flügel | Schneeeulen-Flügel (legendär) |
| `polar_aurora_wings.vibey` | Flügel | Polarlicht-Schleierflügel (episch) |
| `polar_pompom_beanie.vibey` | Hut | Bommelmütze (selten) |
| `polar_igloo_backpack.vibey` | Rücken | Iglu-Rucksack (episch) |
| `polar_aurora_ring.vibey` | Aura | Polarlicht-Ring (episch) |
| `polar_snowfall_cape.vibey` | Umhang | Schneefall-Umhang (selten, HD) |

Vorschaubilder: `previews/`, Katalog-Thumbnails (256 × 256): `thumbnails/`, je ein Ingame-Screenshot: `ingame/`,
Import in echtes Blockbench: `blockbench/`.

## 1. Im Spiel (Creator-Modus + ein Befehl)

1. Minecraft mit Vibey Client ab 0.2.1 starten und eine Welt betreten (Fabric oder NeoForge, 1.21.1 – 26.3).
2. **Rechte Umschalttaste** → Vibey-Menü → Modul **Blockbench-Link** → **Creator-Modus** einschalten.
   Das Spiel schreibt dann `<Spielordner>/vibeyclient/devlink.json` (Port + Token, nur 127.0.0.1).
3. Im Ordner `content/` des Repos:

   ```sh
   npm run push:polar
   ```

   Das Skript findet das laufende Spiel selbst (`%APPDATA%\.minecraft`, alle Vibe-Launcher-Instanzen unter
   `%APPDATA%\VibeLauncher\instances\<id>\minecraft`, die Dev-Ordner `versions/*/run`). Bei mehreren Spielen:
   `npm run push:polar -- --instance <id>` oder `npm run push:polar -- --game-dir "<Spielordner>"`.
4. Pro Item steht eine Zeile da (`✓ … loaded + equipped` / `✗ … Grund`). Das erste Item je Slot wird direkt
   angelegt (Robbe, Schneeeulen-Flügel, Mütze, Iglu, Ring, Umhang), die anderen liegen in der **Garderobe**.
   Alle tragen das Abzeichen **DEV** und sind **nur für dich sichtbar** – sie werden nirgends hochgeladen und sind
   nach einem Neustart des Spiels wieder weg.

Gut zu sehen: Robbe und Pinguin haben lange `idle`-Schleifen (Robbe 20 s: blinzeln, Kopf schief, klatschen mit
Herzchen, Rolle, Nickerchen mit Zs); laufen = hüpfen + Bauchrutscher; fliegen (Kreativ/Elytra) und schleichen haben
eigene Posen. Flügel: beim Fliegen ein Flügelschlag + Gleiten, beim Schleichen eng angelegt.

## 2. Im Vibe Launcher (3D-Vorschau, ohne Einreichen)

1. Im Launcher mit Vibe verbunden sein (sonst zeigt der Tab nur „Mit Vibe verbinden“).
2. Seitenleiste **Cosmetics** → oben rechts **Creator** → Tab **Hochladen**.
3. **.vibey-Datei wählen** → **Auswählen** → eine Datei aus diesem Ordner öffnen. Die Datei wird nur lokal gelesen
   und geprüft.
4. Rechts unter **Vorschau** erscheint das Item an deinem Skin (drehbar, Emotes spielen ab).
5. **Nicht auf „Einreichen“ klicken** – erst dieser Klick schickt etwas an Vibe. Tab verlassen verwirft die Auswahl.

## Neu bauen

```sh
cd content
npm run build:polar                      # Quellen in src/polar-pals/ → dieser Ordner (+ previews, thumbnails)
node tools/validate.mjs dist/polar-pals/*.vibey
npm run check:blockbench -- --dir dist/polar-pals   # Plugin-Validator + Import/Export im echten Blockbench
```
