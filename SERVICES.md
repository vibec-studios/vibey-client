# Vibe online services – terms of use for software

Vibe Craft Studios operates online services for its own apps:

- the Vibe backend at `vibe.henny.dev`: accounts and login, friends, chats, presence, invites, world hosting
  sessions, the **cosmetics catalog**, **cosmetic assets** (bundles, thumbnails), loadouts and **loadout signing**,
  cosmetic **submissions**, **Vibey Client downloads** and release metadata, all APIs and the realtime gateway,
- the Vibey relay at `vibey.henny.dev`, `relay.vibey.henny.dev` and `*.vibey.henny.dev` (world hosting of the
  multiplayer module),
- any other host we use for the same purpose.

Terms of use for these services: <https://vibe.henny.dev/terms>.

The open source license of the code **does not include any right to use these services**. They are offered only to
end users of the **official builds** published by Vibe Craft Studios (the Vibey Client releases on
github.com/vibec-studios, the downloads through the Vibe Launcher and vibe.henny.dev, and the official Vibey
Multiplayer / Vibey Lyrics builds).

## Not allowed without our written permission

- shipping a fork, modified build or other software that connects to the Vibe services,
- using the services for other products, bots, scrapers or automation,
- downloading the cosmetics catalog or cosmetic assets in bulk, or redistributing them,
- getting around limits, ownership checks, loadout signatures, bans or other protections,
- presenting a modified build to the services as an official one.

## Allowed

- using the official builds normally,
- developing and testing changes against **your own local instance** of a backend or relay; contributions to our
  repositories are welcome,
- linking to our websites and to the official downloads.

We may block software, accounts or addresses that break these terms and may change or end the services at any time.
The services come without warranty, to the extent permitted by law.

## For fork maintainers

Online features in forks must be switched off or pointed at services you run yourself. The service addresses in the
code are there for the official builds only:

- `gradle.properties`: `default_backend` (Vibe backend) and `default_relay` (Vibey relay). Both are baked into the
  generated `BuildInfo` class (`DEFAULT_BACKEND`, `DEFAULT_RELAY`; generator in `gradle/common.gradle`), which is
  used by `api/GlobalSettings.java` (`backendUrl`), `backend/BackendClient.java`,
  `module/multiplayer/MultiplayerModule.java` and `module/multiplayer/host/HostConfig.java`.
- `src/main/java/dev/henny/vibeyclient/ui/screen/AboutPanel.java`: the website link (`WEBSITE`).
- `backend/BackendClient.java`: the guard that refuses dev logins against `vibe.henny.dev`.
- `tools/release-manifest.mjs` / `.github/workflows/release.yml`: our release process (GitHub releases imported by
  our backend, `--publish` to our backend); use your own.
- The Blockbench plugin talks only to the running game (devlink on localhost); submissions go through the game's
  backend connection.

Players can also change the backend under *Global settings > Advanced*; a fork must still not ship our addresses as
its defaults. Without a backend, cosmetics, emotes and the server channel keep working offline with the built-in
items (which a fork must replace, see [ASSETS-LICENSE.md](ASSETS-LICENSE.md)).
