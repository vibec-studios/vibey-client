<img src="assets/icon.svg" width="64" height="64" alt="">

# vibey-client

**Vibey Client** – the modular Minecraft client mod by Vibe Craft Studios (Fabric + NeoForge, Minecraft 1.21.1 →
26.3). Product contract: [docs/SPEC.md](docs/SPEC.md). Writing modules: [docs/MODULES.md](docs/MODULES.md).

## Development

Requires JDK 21 and JDK 25 (Minecraft 26.x); Gradle picks them via toolchains. Built with
[Stonecutter](https://stonecutter.kikugie.dev/) 0.9.5: one source tree, one Gradle subproject per loader and
Minecraft line (`versions/<loader>-<mc>/gradle.properties`). The sources are written for **fabric-1.21.11**;
never switch the Stonecutter active version (it rewrites `src/`), build other targets by name instead.

```sh
./gradlew buildAll                              # all 20 jars (10 Minecraft lines × Fabric/NeoForge) → build/dist
./gradlew :fabric-1.21.11:build                 # one target (also :neoforge-26.3:build, :fabric-1.21.1:build, ...)
./gradlew :fabric-1.21.11:runClient             # dev client (Fabric)
./gradlew :neoforge-1.21.11:runClient           # dev client (NeoForge)
./gradlew :fabric-1.21.11:runServer             # dedicated server (accept the EULA in versions/fabric-1.21.11/run-server)
./gradlew :fabric-1.21.11:runClientGameTest     # client game test, screenshots in versions/fabric-1.21.11/build/run/clientGameTest/screenshots
./gradlew :neoforge-1.21.11:runClient -Dvibeyclient.smoke=true   # smoke: boot, log "[vibeyclient-smoke]", quit
./gradlew :fabric-1.21.11:runServer -Dvibeyclient.smoke=true     # same for a dedicated server (+ checks no client classes loaded)
```

Default backend (`https://vibe.henny.dev`) and relay are set in `gradle.properties` and baked into the generated
`BuildInfo` class. In game: **Right Shift** or the **Vibey** button in the pause menu opens the Vibey menu.

Layout: common, server-safe code in `dev.henny.vibeyclient` (`Vibey`, `Platform`, `Compat`, `net/`), client core in
`client/`, the module API in `api/`, UI in `ui/`, modules in `module/<id>/`, loader entry points in `fabric/` and
`neoforge/`.

### Multiplayer and lyrics modules

Ports of the standalone mods *Vibey Multiplayer* (`vibey`) and *Vibey Lyrics* (`vibeylyrics`, Fabric only). If one of
them is installed, the matching module switches itself off (no payloads, keybinds, pause buttons or mixins). Settings
live in `config/vibeyclient/multiplayer.json` / `lyrics.json`; on first start the standalone mods' `config/vibey.json`
and `config/vibey-lyrics.json` are imported. Songs are shared with Vibey Lyrics in `.minecraft/vibey-lyrics/`.

```sh
# local relay for hosting tests (never test against the production relay)
cd ../vibey-multiplayer/relay && ALLOW_OFFLINE=1 PORT=26400 DATA_DIR=/tmp/relaydata node src/main.ts
./gradlew :fabric-1.21.11:runClientGameTest                          # hosts through 127.0.0.1:26400 (-Dvibeyclient.test.relay=off skips it)
./gradlew :fabric-1.21.11:runClientGameTest "-Dvibeyclient.test.addMods=<vibey.jar>;<vibeylyrics.jar>"   # conflict test
./gradlew :neoforge-1.21.11:runClient -Dvibeyclient.mpcheck=true -Dvibeyclient.mpcheck.relay=127.0.0.1:26400   # NeoForge screenshots → run/screenshots
```

The lyrics module bundles fonts under the SIL Open Font License 1.1 (Permanent Marker: Apache License 2.0) (Bebas Neue, Bungee,
Permanent Marker, Pacifico, Press Start 2P, Monoton, Poppins, Montserrat, Kanit, Fredoka, Unbounded, Caveat and
Kaushan Script). Their license texts are in `licenses/lyrics/fonts/` and inside every Fabric jar under
`licenses/vibeyclient/lyrics/`. The MP3 decoder [javamp3](https://github.com/delthas/JavaMP3) (MIT) is shipped inside
the Fabric jars.

License: MIT.
