# Recherche: echter fensterloser Modus (Headless) für AFK

Stand: 21.11.2026. Nur Recherche, nichts davon ist gebaut. Gebaut ist der AFK-Agent ([AFK-AGENT.md](AFK-AGENT.md)),
der das Fenster versteckt, die Bildrate auf 2 FPS bremst und den Ton stumm schaltet.

## Kurzfassung / Empfehlung

**Nicht einbauen, beim versteckten Fenster bleiben.** Ein echter Headless-Modus spart gegenüber unserem Agenten
kaum noch etwas (gemessen: der versteckte Client braucht 6 % eines CPU-Kerns und 0,3 % GPU, siehe AFK-AGENT.md),
kostet aber die wichtigste Eigenschaft: **„Bildschirm wieder anzeigen“ ist mit Headless unmöglich** – ohne
OpenGL/Vulkan-Kontext gibt es nichts, was man wieder anzeigen könnte; der Nutzer müsste das Spiel neu starten und
wäre dabei vom Server getrennt. Dazu kommen ein fremder Launcher-Pfad, Konto-Regeln und Inkompatibilitäten mit
Render-Mods. Sinnvoll wäre Headless nur für einen späteren, ausdrücklichen „Server-AFK ohne Bildschirm“ auf
einem Rechner ohne GPU (z. B. ein Vibe-Panel-Server) – dann als eigene Funktion, nicht als Ersatz.

## HeadlessMC (headlesshq/headlessmc)

| Punkt | Befund |
|---|---|
| Lizenz | MIT (Repo, auch hmc-specifics, hmc-optimizations). Einbetten/Verteilen wäre lizenzrechtlich möglich (Hinweis + Lizenztext mitliefern). |
| Aktivität | aktiv gepflegt, aktuell 3.0.0-RC9 (16.11.2026), letzter Push 21.11.2026, ~430 Sterne. 3.0 ist noch Release-Candidate. |
| Was es ist | ein eigener Kommandozeilen-Launcher (Login, Versionen, Loader, Modpacks, Server). Headless ist ein Startmodus davon (`launch … -lwjgl`). |
| Wie Headless geht | LWJGL wird beim Start umgeschrieben (eigener Patcher, `headlessmc-lwjgl`): Funktionen machen nichts oder liefern Platzhalterwerte. Es gibt Umleitungen für GLFW, OpenGL, Vulkan und **SDL (26.3)** – laut Quelltext (`SdlRedirections`: „Window and timer redirections for the SDL backend introduced in 26.3“). Alternative laut Doku: virtueller Framebuffer (Xvfb, nur Linux). |
| Loader | Client: Fabric, Forge, NeoForge (laut Doku). Quilt nicht genannt. Unabhängig von der Minecraft-Version, weil nur LWJGL gepatcht wird. |
| Steuerung im Spiel | über die Zusatz-Mods **hmc-specifics** (Befehle `msg`, `/`, `connect`, `gui`, `click` …) – versionsspezifisch, nicht für jede Version/Loader-Kombination vorhanden. Ohne sie ist das Spiel „blind“. |
| Konten | „Accounts will always be validated. Offline accounts can only be used to run the game headlessly in CI/CD pipelines.“ Für uns ok (wir haben echte MSA-Konten), aber die Konto-Prüfung liegt dann in HeadlessMC, nicht in unserem Launcher. |
| Leistung | Doku: der LWJGL-Patch „carries some overhead“; hmc-optimizations (patcht Minecraft selbst, versionsabhängig) soll Render-Code ganz überspringen. |

## Passt das zu Vibe?

1. **Bildschirm zurückholen: nein.** Im gepatchten Modus existiert kein Fenster und kein Grafikkontext. Texturen,
   Shader, Chunk-Meshes werden nie angelegt. Ein Umschalten zur Laufzeit würde bedeuten, alle Render-Ressourcen
   neu aufzubauen – das kann Minecraft nicht. Für den Nutzerwunsch („wenn nötig den Bildschirm wieder starten“)
   ist das ein Ausschlusskriterium.
2. **Unsere Mixins / Mods:** Vibey Client hängt an HUD-, GUI- und Render-Code (Post-Effekte, Shader in
   `assets/vibeyclient/shaders`, Motion Blur, Clips-Aufnahme, Kamera). Mit Platzhalter-LWJGL laufen diese Pfade
   gegen Stubs; Shader-Kompilierung, Framebuffer-Größen (0×0) und Clips (liest Bilddaten) würden mindestens
   Fehler werfen. Das müsste pro Modul abgesichert werden („headless erkannt → aus“). Fremde Render-Mods
   (Sodium, Iris, Shader-Packs) in beliebigen Modpacks sind ein Glücksspiel; HeadlessMC testet vor allem Vanilla
   und eigene Mods (mc-runtime-test).
3. **Bedienbarkeit:** HeadlessMC ist ein zweiter Launcher mit eigener Versions-/Bibliotheksverwaltung. Wir
   müssten entweder seinen Start-Pfad in unseren Launcher übernehmen (Patcher als Bibliothek, gepatchte LWJGL-Jars
   bereitstellen) oder den kompletten Launcher fernsteuern. Beides ist deutlich mehr als „ein Schalter“.
4. **Kompatibilität Versionen:** gut (LWJGL-Patch ist versionsunabhängig, 26.3/SDL wird abgedeckt).
5. **Ersparnis gegenüber unserem Agenten:** gering. Unser Agent rendert 2 Bilder pro Sekunde in ein unsichtbares
   Fenster. Gemessen auf Vanilla 1.21.11: 51,7 % → 6 % eines CPU-Kerns, GPU 6,7 % → 0,3 %. Der Rest ist
   Spiel-Logik (Ticks, Netzwerk, Chunks), die auch Headless läuft. RAM bleibt gleich (Texturen liegen bei uns im
   VRAM/RAM, Headless spart hier einige hundert MiB – der einzige echte Vorteil).

## Wenn wir es später doch wollen

- Eigene, schmale Variante statt HeadlessMC: unser Agent hängt schon an GLFW/SDL/Vulkan/OpenAL. Ein
  „Headless“-Schalter im Agenten wäre technisch eine Erweiterung derselben Hooks (z. B. `SDL_CreateWindow` /
  `glfwCreateWindow` mit 1×1 versteckt) – spart aber keinen Render-Code, nur die Fenstergröße. Wirklich headless
  heißt Render-Code überspringen, und das ist versionsspezifisch (wie hmc-optimizations).
- Für einen Linux-Server ohne GPU: HeadlessMC + Xvfb oder `-lwjgl` als eigener „Server-AFK“-Modus im Vibe Panel,
  mit klarer Ansage „kein Bildschirm, Neustart nötig zum Spielen“.

## Quellen

- https://github.com/headlesshq/headlessmc (README, `docs/launch.md`, `docs/specifics.md`, `docs/configuration.md`,
  Releases-Liste über die GitHub-API)
- Quelltext `headlessmc-lwjgl/.../redirections/SdlRedirections.java`, `GraphicsProviderRedirections.java`
  (Code-Suche im Repo)
- https://github.com/headlesshq/hmc-specifics, https://github.com/headlesshq/hmc-optimizations
- https://3arthqu4ke.github.io/headlessmc/launch
