# Hosting: Panel, Join-Prüfung, Sprachchat über das Relay

Modul `multiplayer`, Paket `module/multiplayer/host`. Verträge: Backend `docs/HOSTING-PANEL.md` (vibe-backend),
Relay `relay/UDP.md` und `relay/README.md` (vibey-multiplayer, ab Relay 1.5 bzw. 1.6).

## Hosting-Panel (Backend)

`HostPanel` sucht nach dem Online-Gehen die eigene Backend-Session (`GET /api/v1/hosting/sessions`, Art `world`,
Slot 0, gleiche Relay-Adresse) und lädt `GET …/panel`. Live über das Gateway:

| Event | Wirkung im Spiel |
|---|---|
| `hosting.kick` | nur wenn `hostId` = eigenes Konto und die Session passt: Spieler wird getrennt (`permanent` → Bann-Text) |
| `hosting.panel.updated` | Privatsphäre, Liste, Kicks und Slots neu |
| `hosting.update` | Session beendet → Panel leer, neue Suche |

Zusätzlich schickt Relay 1.6 die Zeile `{"t":"kick",…}` über die Steuerverbindung; auch dann wird getrennt.

**Join-Prüfung:** Jeder Spieler, der der gehosteten Welt beitritt, wird mit `POST …/join-check` geprüft (UUID + Name).
Bei `allow: false` wird er mit einem übersetzten Text getrennt (`kicked`, `not_friend`, `not_invited`). Ist das
Backend nicht erreichbar oder der Spieler nicht angemeldet, bleibt die Welt offen (wie vorher).

**Slots:** `policy.maxPlayers` gilt, wenn gesetzt, sonst die lokale Einstellung. Obergrenze bleibt die eingebaute
Grenze des integrierten Servers (8). Ändert der Host die Zahl im Menü, geht sie auch an `PATCH …/policy`.

**Menü (Verwalten):** Spieler-Tab mit Knopf „Mitglied“ (Rolle `member`/`guest`), Kick und Bannen über das Backend
(Bannen sperrt zusätzlich wie bisher am Relay). Zugang-Tab: „Wer darf rein?“ (Alle / Freunde / Einladung),
„Neue Spieler sind Mitglieder“, die Liste und aufhebbare Kicks. Ohne Vibe-Anmeldung steht dort ein Hinweis.

## Sprachchat (Simple Voice Chat) über das Relay

Nur wenn Simple Voice Chat installiert ist: Der Host fragt beim Relay `{"t":"udp","channels":["voice"]}` an. Das Relay
antwortet mit `udp_ready` (Token, Tunnel-Port, `voiceHost`). `UdpTunnel` hält einen UDP-Kanal zum Relay
(HELLO alle 15 s, bis zur Bestätigung alle 2 s) und leitet jede Gegenstelle über einen eigenen lokalen Socket an
`127.0.0.1:<SVC-Port>` weiter. Der lokale Port kommt aus `config/voicechat/voicechat-server.properties` (`port`,
Standard 24454, `-1` = LAN-Port).

`VoicePlugin` (SVC-Plugin, Fabric-Entrypoint `voicechat`, NeoForge `@ForgeVoicechatPlugin`) setzt per
`VoiceHostEvent` den Voice-Host auf `voiceHost` aus `udp_ready`, solange gehostet wird. Status im Tab „Erweitert“:
Aktiv, Verbinde, UDP blockiert (kein `udp_ok` nach 5 s), Relay kann das nicht (Relay < 1.5), vom Relay abgelehnt.

Bedrock (Geyser) ist nur für Server aus dem Launcher vorgesehen; der Tunnel kann Kanal 2 schon weiterleiten, das
Modul fragt ihn aber nicht an.

## Tests

`src/junit/…/multiplayer/host`: `UdpTunnelTest` (Fake-Relay per UDP: HELLO/ACK, DATA hin und zurück, CLOSE, Leerlauf,
fremde Absender), `TunnelUdpTest` (Fake-Relay per TCP+UDP: Anfrage, `udp_ready`, `udp_ok`, Ablehnung, altes Relay),
`PanelModelTest`, `RelayUdpTest` (gegen ein echtes Relay, nur mit `VIBEY_TEST_RELAY_UDP=host:port`, Relay mit
`ALLOW_OFFLINE=1`).
