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

Social against a **local** backend (never the dev bypass against production): start vibe-backend with
`AUTH_DEV_BYPASS=1` (see its `docs/DEPLOY.md`), then

```sh
./gradlew :fabric-1.21.11:runClient -Dvibeyclient.devAuth=true -Dvibeyclient.api=http://localhost:3100 -Dvibeyclient.username=Alex
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.socialTest=true -Dvibeyclient.devAuth=true     -Dvibeyclient.api=http://localhost:3100 -Dvibeyclient.username=VibeyTester   # bots: scripts/social-bots.ts
```

`vibeyclient.devAuth` only works in a dev workspace (release builds always log in through Mojang). On NeoForge use
`127.0.0.1` instead of `localhost`.

Default backend (`https://vibe.henny.dev`) and relay are set in `gradle.properties` and baked into the generated
`BuildInfo` class. In game: **Right Shift** or the **Vibey** button in the pause menu opens the Vibey menu.

Layout: common, server-safe code in `dev.henny.vibeyclient` (`Vibey`, `Platform`, `Compat`, `net/`), client core in
`client/`, the module API in `api/`, UI in `ui/`, modules in `module/<id>/`, loader entry points in `fabric/` and
`neoforge/`.

License: MIT.
