# Polar Pals – lokal testen

Premium-Kollektion für Vibey Client (8 Items, alle 3D und animiert). **Nur lokal** – nichts davon liegt im Katalog
oder auf dem Server. Übersicht mit Ingame-Screenshots und Vorher/Nachher: [`polar-pals.html`](polar-pals.html)
(offline, eine Datei).

| Datei | Slot | Item |
|---|---|---|
| `polar_seal.vibey` | Pet | Robbenbaby (legendär) – so groß wie der Pinguin |
| `polar_penguin.vibey` | Pet | Pinguin (episch) – schulterhoch |
| `polar_owl_wings.vibey` | Flügel | Schneeeulen-Flügel (legendär) |
| `polar_aurora_wings.vibey` | Flügel | Polarlicht-Schleierflügel (episch) |
| `polar_pompom_beanie.vibey` | Hut | Bommelmütze (selten) |
| `polar_igloo_backpack.vibey` | Rücken | Iglu-Rucksack (episch) |
| `polar_aurora_ring.vibey` | Aura | Polarlicht-Ring (episch) |
| `polar_snowfall_cape.vibey` | Umhang | Schneefall-Umhang (selten, HD) |

Vorschaubilder: `previews/`, Katalog-Thumbnails (256 × 256, gehen beim Push mit): `thumbnails/`,
Ingame-Screenshots: `ingame/` (vorher: `ingame/before/`, vor Runde 2: `ingame/before2/`, neue Zustände, Flügel + Rucksack, Nacht, Mütze: `ingame/states/`),
Import in echtes Blockbench: `blockbench/`.

## Runde 2

- **Robbe kleiner**: `pet.scale` 1.2 → 0.85. Liegend reicht sie dir etwa bis zur Hüfte, aufgerichtet (Klatschen)
  knapp unter die Schulter – ungefähr so groß wie der Pinguin, weiter rund und knuffig. Der Abstand neben dir passt
  sich automatisch an.
- **Flügel + Iglu-Rucksack**: Die Flügelwurzeln sitzen jetzt außen an den Schulterblättern. Mit Rucksack heben sich
  die Eulenflügel 2,5 px, rücken nur noch 6 px (statt 9 px) nach hinten und kippen 10° (`offsetWhen`), die
  Polarlicht-Flügel heben sich 2 px und rücken 4 px (statt 8 px) nach hinten. Beide sitzen damit seitlich am Iglu
  statt dahinter – ohne Lücke und ohne ineinander zu schneiden (Vorher/Nachher in `polar-pals.html`).

## Was ist neu (Feedback-Runde)

- **Pets** sind deutlich größer (Kopf etwa auf Schulterhöhe), laufen mit Abstand neben dir und nie durch dich durch.
- **Neue Zustände** (Vibey Client 0.2.2): `sprint` (Robbe: schneller Bauchrutscher, Pinguin: Bauchrutschen),
  `glide` (Elytra oder längerer Fall: Robbe/Pinguin als „Superman“, Flügel weit gespreizt), `idle_long`
  (ca. 10 s stillstehen: die Robbe hält ein Nickerchen, der Pinguin döst im Stehen ein – mit Zs) und `happy`
  (wenn du ein Emote spielst, jubeln die Pets mit Herzchen).
- **Mütze** ruhiger: drei Farben, ein klarer weißer Bommel ohne Sprenkel.
- **Polarlicht** (Ring + Schleierflügel): weiche, fließende Lichtbänder statt bunter Platten – durchscheinend, mit
  Farbverlauf von Mint über Blau zu Violett, wandernden Strahlen und weichen Rändern. Am schönsten nachts.
- **Flügel + Rucksack**: Mit dem Iglu-Rucksack rücken beide Flügelpaare automatisch nach hinten
  (`cosmetic.offsetWhen`), nichts schneidet mehr ineinander.
- **Thumbnails**: gepushte DEV-Items zeigen in der Garderobe ihr Bild statt der Buchstaben-Kachel.

## 1. Im Spiel (Creator-Modus + ein Befehl)

1. Minecraft mit Vibey Client starten und eine Welt betreten (Fabric oder NeoForge, 1.21.1 – 26.3). Die neuen
   Zustände, die weichen Polarlichter und die DEV-Thumbnails brauchen den Client aus diesem Branch (0.2.2); ältere
   Clients zeigen die Items auch, aber nur mit `idle`/`walk`/`fly`/`sneak`.
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
   Alle tragen das Abzeichen **DEV**, zeigen ihr Thumbnail und sind **nur für dich sichtbar** – sie werden nirgends
   hochgeladen und sind nach einem Neustart des Spiels wieder weg.

Zum Ausprobieren:

| Was du tust | Was passiert |
|---|---|
| stehen | `idle`: Robbe blinzelt, legt den Kopf schief, klatscht mit Herzchen, rollt sich; Pinguin zeigt einen Fisch |
| ~10 s stillstehen | `idle_long`: Robbe legt sich zum Nickerchen hin, Pinguin döst ein, Zs steigen auf |
| laufen | `walk`: Robbe robbt (galumph), Pinguin watschelt |
| sprinten | `sprint`: Bauchrutscher mit Schneespritzern |
| ein Emote spielen (z. B. Winken) | `happy`: die Pets hüpfen und jubeln mit Herzchen |
| Elytra / tiefer Fall | `glide`: Superman-Pose, Flügel weit gespreizt, Polarlicht weht nach hinten |
| Kreativ fliegen | `fly`: Flügelschlag + Gleiten, Pets flattern |
| schleichen | `sneak`: schüchtern / Flügel eng angelegt |
| Iglu-Rucksack + Flügel | Flügel heben sich und rücken weich ein Stück nach hinten, seitlich ans Iglu |

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
