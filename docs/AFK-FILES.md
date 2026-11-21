# AFK-Modus – Datei-Vertrag Launcher ↔ Spiel (Schema 1)

Owner: Client-Agent A2 (vibey-client, `tools/afk-agent`). Gegenseite: Launcher-Agent A1.
Gilt für **jede** Instanz (Vanilla, Fabric, Quilt, Forge, NeoForge, beliebige Modpacks). Die Spielseite ist ein
generischer Java-Agent `vibe-afk-agent.jar`, der per `-javaagent:` angehängt wird. Der Vibey Client braucht es
nicht; ein späteres Vibey-Modul `afk` darf dieselben Dateien zusätzlich nutzen (siehe §9).

Alle Dateien liegen in `<instanz>/.vibe/` (gleicher Ordner wie `instance.json`, `session.json`). Kodierung UTF-8
ohne BOM, Zeilenende `\n`. Zeitstempel `ts` = Unix-Millisekunden. Unbekannte Felder immer ignorieren, fehlende
Felder = Standardwert. Ein höheres `schema` als bekannt: so viel lesen wie möglich, nicht abbrechen.

| Datei | Schreibt | Liest | Art |
|---|---|---|---|
| `afk.json` | Launcher | Agent | Steuerung, atomar ersetzt (tmp + rename) |
| `afk-state.json` | Agent | Launcher | aktueller Zustand, atomar ersetzt |
| `afk-events.jsonl` | Agent | Launcher | Ereignisse, nur angehängt, rotiert |
| `afk-outbox.jsonl` | Launcher | Agent | zu sendende Chat-Nachrichten/Befehle, nur angehängt |

---

## 1. Agent beziehen und anhängen

**Download:** GitHub-Release im Repo `vibec-studios/vibey-client`, Tag `afk-agent-v<version>` (eigener Workflow
`.github/workflows/afk-agent.yml`, nie als „latest“ markiert, Pre-Release-Flag aus). Assets:

- `vibe-afk-agent-<version>.jar`
- `vibe-afk-agent-<version>.jar.sha256` (Inhalt: `<64 hex>  vibe-afk-agent-<version>.jar`)

URL: `https://github.com/vibec-studios/vibey-client/releases/download/afk-agent-v<version>/vibe-afk-agent-<version>.jar`

Der Launcher **pinnt Version und SHA-256 in seinem Code** (eine Konstante pro Launcher-Release) und lädt den Jar
beim ersten Gebrauch nach `<launcher-home>/tools/afk-agent/<version>/vibe-afk-agent.jar`. Nach dem Download:
SHA-256 gegen die Konstante prüfen (nicht gegen die `.sha256`-Datei desselben Releases – die dient nur der
Anzeige/Kontrolle); bei Abweichung Datei löschen, AFK-Start abbrechen mit Fehlermeldung. Vor jedem Start Hash der
vorhandenen Datei erneut prüfen (kostet < 5 ms bei ~200 KiB). Kein Netz → normaler Start ohne Agent mit Hinweis.

Erste Version: **0.1.0**. Größe ca. 230 KiB (ASM 9.8 eingebettet und nach `dev.henny.vibeafk.shaded.asm` verschoben).
Der Build ist reproduzierbar gedacht (feste Zeitstempel im Jar), gepinnt wird aber immer der Hash des
**veröffentlichten** Release-Assets (aus der `.sha256`-Datei des Releases bei der Launcher-Freigabe übernehmen).

**JVM-Argument** (vor der Hauptklasse, zusätzlich zu den normalen JVM-Argumenten):

```
-javaagent:<absoluter Pfad zum Jar>=<absoluter Pfad zu <instanz>/.vibe>
```

Alles nach dem ersten `=` ist der `.vibe`-Ordner (Leerzeichen, Kommas erlaubt; als **ein** argv-Element übergeben,
nicht in eine Shell-Zeile bauen). Fehlt er, nimmt der Agent `<user.dir>/.vibe`. Der Agent ist Java-8-Bytecode und
läuft auf Java 8 … 25.

**Wann anhängen:** bei jeder Sitzung, solange das Feature-Flag an ist (auch bei normalem Start im Vordergrund),
damit „In den Hintergrund“ später ohne Neustart geht. Im Vordergrund kostet der Agent praktisch nichts (eine
Datei-Abfrage pro Sekunde, ein Hashtable-Lookup pro Frame).

**Optionen-Überlagerung (empfohlen, alle Loader):** Hintergrund heißt „Fenster verloren den Fokus“. Vanilla öffnet
dann das Pausemenü; im **Einzelspieler** hält das die Welt an. Für AFK im Einzelspieler setzt der Launcher vor dem
Start in `options.txt` `pauseOnLostFocus:false` (alten Wert merken, nach Spielende zurückschreiben). Im
Mehrspieler ist das egal (Pausemenü hält dort nichts an).

## 2. `afk.json` (Launcher → Agent)

```json
{
  "schema": 1,
  "session": "7f3c2a10-…",
  "mode": "background",
  "background_fps": 2,
  "mute": true,
  "hide_window": true,
  "auto_reconnect": { "enabled": false, "max_attempts": 5, "base_delay_s": 10, "max_delay_s": 300 },
  "alerts": { "low_health": 6, "low_food": 6 },
  "mappings": "C:/…/versions/1.21.11/client_mappings.txt",
  "updated_at": 1763726400000
}
```

| Feld | Typ | Standard | Bedeutung |
|---|---|---|---|
| `schema` | int | 1 | Vertragsversion |
| `session` | string | – | frei wählbare ID der Launcher-Sitzung; wird in `afk-state.json` gespiegelt |
| `mode` | `"background"` \| `"foreground"` | `foreground` | gewünschter Modus |
| `background_fps` | int 1…10 | 2 | Bildrate im Hintergrund (siehe Hinweis) |
| `mute` | bool | true | im Hintergrund Ton aus |
| `hide_window` | bool | true | im Hintergrund Fenster verstecken (inkl. Taskleiste) |
| `auto_reconnect.enabled` | bool | false | nach Kick/Verbindungsabbruch neu verbinden |
| `auto_reconnect.max_attempts` | int 1…50 | 5 | danach aufgeben (`reconnect_gave_up`) |
| `auto_reconnect.base_delay_s` | int 5…600 | 10 | Wartezeit vor Versuch 1; verdoppelt sich pro Versuch |
| `auto_reconnect.max_delay_s` | int 10…3600 | 300 | Obergrenze der Wartezeit |
| `alerts.low_health` | number 0…40 | 6 | `health_low`, sobald Gesundheit ≤ Wert (Halbherzen, 6 = 3 Herzen), 0 = aus |
| `alerts.low_food` | int 0…20 | 6 | `food_low`, sobald Hunger ≤ Wert, 0 = aus |
| `mappings` | string | null | Pfad zur Mojang-Datei `client_mappings.txt` dieser Version (aus der Versions-JSON `downloads.client_mappings`, SHA-1 dort prüfen). Nur nötig für obfuskierte Versionen (≤ 1.21.11); ab 26.1 egal |
| `updated_at` | int ms | – | nur Info |

- `background_fps`: Minecraft holt pro Bild höchstens 10 Spiel-Ticks nach. Bei 1 FPS läuft die Client-Simulation
  daher nur mit 10 statt 20 Ticks/s (Server merkt das nicht, aber Client-Animationen/Chat-Verarbeitung laufen halb
  so schnell). **2 FPS** ist der kleinste Wert ohne diesen Effekt und Standard. 1 ist erlaubt.
- Der Agent liest die Datei **einmal pro Sekunde** (mtime + Größe; neu parsen nur bei Änderung) und wendet eine
  Änderung beim nächsten Bild an (also nach ≤ 1 s + 1 Bild ≈ ≤ 1,5 s). Kaputtes JSON: letzter gültiger Stand bleibt,
  ein `error`-Ereignis (`code: "afk_json_invalid"`) wird einmal pro Änderung geschrieben.
- Datei fehlt → `foreground`. Löschen der Datei holt das Spiel also immer zurück.
- Wechsel `background → foreground` stellt exakt wieder her: Fenster sichtbar + in den Vordergrund (Fokus), Bildrate
  wie vorher (der Agent bremst nur, er ändert keine Spieloption), Lautstärke = zuletzt vom Spiel gesetzter Wert.
- **Fokus (Windows):** Windows lässt einen Hintergrundprozess sein Fenster nicht selbst nach vorne holen (getestet:
  Fenster wird sichtbar, bleibt aber hinter dem Launcher). Der Launcher ruft deshalb **vor** dem Schreiben von
  `mode: "foreground"` `AllowSetForegroundWindow(<pid>)` auf (user32; der Launcher ist zu dem Zeitpunkt im
  Vordergrund, weil der Nutzer gerade geklickt hat). Danach holt der Agent das Fenster per `glfwFocusWindow` /
  `SDL_RaiseWindow` nach vorne. Alternativ darf der Launcher nach ≤ 2 s selbst `SetForegroundWindow` auf das
  Fenster der PID setzen.

## 3. `afk-state.json` (Agent → Launcher)

Geschrieben beim Start, bei jeder Änderung und spätestens alle 5 s (Herzschlag über `updated_at`).

```json
{
  "schema": 1,
  "agent_version": "0.1.0",
  "run": 1763726400123,
  "pid": 12345,
  "session": "7f3c2a10-…",
  "mode": "background",
  "running": true,
  "env": { "lwjgl": "3.3.3", "loader": "fabric", "mc": "1.21.11", "names": "intermediary", "java": "21.0.7" },
  "capabilities": {
    "window": true, "throttle": true, "mute": true,
    "chat_events": true, "game_state": true, "send": true, "reconnect": true
  },
  "game": {
    "screen": "world", "server": "play.example.net", "player": "Alex",
    "health": 20.0, "max_health": 20.0, "food": 20, "dead": false
  },
  "stats": { "frames": 3152, "fps": 2.0, "muted": true, "game_volume": 1.0, "window_hidden": true },
  "updated_at": 1763726405000
}
```

- `stats` (nur Anzeige/Diagnose): `frames` (Bilder seit Start), `fps` (Mittel seit dem letzten Schreiben),
  `muted`, `game_volume` (zuletzt vom Spiel gesetzte Listener-Lautstärke, sonst `null`), `window_hidden`.
- `run`: Startzeit des Agenten (ms) – neue Zahl = Spiel neu gestartet, `seq` beginnt wieder bei 1.
- `mode`: der **angewendete** Modus (kann der Datei kurz hinterherhinken).
- `running`: `false` im letzten Schreibvorgang beim Beenden (Shutdown-Hook). Kein Herzschlag > 15 s bei laufendem
  Prozess = Agent hängt/fehlt → Launcher fällt auf eigenes Fenster-Verstecken zurück.
- `env.lwjgl`: `"3.x.y"`, `"2"` oder `null` (nicht erkannt). `env.loader`: `vanilla`, `fabric`, `quilt`, `forge`,
  `neoforge`, `unknown`. `env.names`: Namensraum der Spielklassen zur Laufzeit: `mojang` (ab 26.1 und NeoForge/Forge
  mit Mojang-Namen), `obf` (Vanilla ≤ 1.21.11), `intermediary` (Fabric/Quilt ≤ 1.21.11), `srg` / `unknown`
  (nicht unterstützt für Spielzustand). `env.mc` kann `null` sein.
- `capabilities` – was in dieser Sitzung wirklich geht; der Launcher blendet UI danach ein/aus:
  - `window`: Agent kann Fenster verstecken/zeigen (LWJGL 3). Bei `false` (LWJGL 2, ≤ 1.12.2) versteckt der
    Launcher das Fenster selbst über die Prozess-ID (Win32 `ShowWindow`/`WS_EX_TOOLWINDOW`).
  - `throttle`, `mute`: Bildrate bremsen / Ton aus (LWJGL 3 und LWJGL 2).
  - `chat_events`: Chat aus dem Spiel-Log (`logs/latest.log`, Zeilen mit `[CHAT]`) – auf allen Versionen ab 1.8,
    falls das Log existiert.
  - `game_state`: Gesundheit, Hunger, Tod, Bildschirm, Server über Reflection (siehe §8).
  - `send`: Outbox wird verarbeitet. Bei `false` antwortet der Agent auf jede Outbox-Zeile mit
    `outbox_ack` `ok:false, error:"unsupported"`.
  - `reconnect`: Auto-Reconnect möglich.
  Fähigkeiten können während der Sitzung von `false` auf `true` wechseln (Spielklassen werden erst nach dem
  Ladebildschirm gefunden), nicht umgekehrt außer bei einem Fehler (dann `error`-Ereignis).
- `game.screen`: `loading`, `menu`, `world`, `death`, `disconnected`, `connecting`, `unknown`. `server`: Adresse
  oder `null` (Einzelspieler/Menü). Felder fehlen, wenn `game_state` `false` ist.

## 4. `afk-events.jsonl` (Agent → Launcher)

Eine JSON-Zeile pro Ereignis, immer mit `seq` (fortlaufend ab 1 pro `run`, lückenlos), `ts`, `type`:

```json
{"seq":1,"ts":1763726400200,"type":"agent_start","run":1763726400123,"agent_version":"0.1.0"}
{"seq":2,"ts":1763726401000,"type":"mode","mode":"background","window":true,"fps":2,"muted":true}
{"seq":3,"ts":1763726410000,"type":"connect","server":"play.example.net"}
{"seq":4,"ts":1763726420000,"type":"chat","text":"<Steve> hi Alex","sender":"Steve","source":"log"}
{"seq":5,"ts":1763726421000,"type":"mention","text":"<Steve> hi Alex","sender":"Steve"}
{"seq":6,"ts":1763726430000,"type":"whisper","text":"Steve whispers to you: psst","sender":"Steve"}
{"seq":7,"ts":1763726440000,"type":"health_low","health":5.0,"max_health":20.0}
{"seq":8,"ts":1763726450000,"type":"death","message":"Alex was slain by Zombie"}
{"seq":9,"ts":1763726460000,"type":"disconnect","server":"play.example.net","reason":"Kicked for being AFK"}
{"seq":10,"ts":1763726470000,"type":"reconnect_attempt","attempt":1,"delay_s":10,"server":"play.example.net"}
{"seq":11,"ts":1763726480000,"type":"outbox_ack","id":"b1","ok":true}
```

| `type` | Felder | Wann |
|---|---|---|
| `agent_start` | `run`, `agent_version` | Agent geladen (vor dem Spiel) |
| `agent_stop` | – | Shutdown-Hook (fehlt bei Absturz) |
| `mode` | `mode`, `window`, `fps`, `muted` (je bool/int, was tatsächlich angewendet wurde) | Modus angewendet |
| `capabilities` | wie `afk-state.json.capabilities` | Fähigkeiten haben sich geändert |
| `connect` | `server` (oder `null` = Einzelspieler), `world`? | Welt betreten |
| `disconnect` | `server`, `reason` (Text oder `null`) | Verbindung weg/gekickt (Trennen-Bildschirm) oder Welt verlassen (`reason: null`, `left: true`) |
| `chat` | `text`, `sender`?, `source` (`log`\|`game`) | jede Chat-/Systemzeile |
| `whisper` | `text`, `sender`? | zusätzlich zu `chat`, wenn als Flüstern erkannt |
| `mention` | `text`, `sender`? | zusätzlich zu `chat`, wenn der eigene Name vorkommt (nicht in eigenen Nachrichten) |
| `death` | `message`? | Todesbildschirm / Gesundheit 0 |
| `respawn` | – | wieder lebendig |
| `health_low` | `health`, `max_health` | Schwelle unterschritten (erneut erst nach Erholung über Schwelle + 2) |
| `food_low` | `food` | wie oben (Erholung + 2) |
| `health` | `health`, `max_health`, `food` | höchstens alle 30 s bei Änderung (für Verlaufsanzeige) |
| `reconnect_attempt` | `attempt`, `delay_s`, `server` | Versuch startet nach Wartezeit |
| `reconnect_gave_up` | `attempts`, `server` | `max_attempts` erreicht |
| `outbox_ack` | `id`, `ok`, `error`? | Outbox-Zeile verarbeitet |
| `error` | `code`, `message` | z. B. `afk_json_invalid`, `mappings_missing`, `mappings_invalid`, `hook_failed`, `outbox_invalid` |

- Chat-Texte sind reiner Text ohne Farbcodes (`§x` entfernt), höchstens 1024 Zeichen. Flüster-/Erwähnungserkennung
  ist eine Heuristik (Muster `X whispers to you:`, `X flüstert dir zu:`, `[X -> me]`, `[X -> mir]`, `From X:`,
  `X -> you:`), die Launcher-UI soll sie nicht als sicher darstellen. Keine Erwähnung bei eigenen Zeilen
  (`<Ich> …`, `* Ich winkt`, `Ich was slain by …` – Name am Zeilenanfang). Todesnachrichten kommen zusätzlich als
  `chat`; das eigentliche Ereignis ist `death`.
- `chat`-Rate: höchstens 50 Ereignisse pro Sekunde; darüber werden Zeilen verworfen und ein
  `{"type":"chat_dropped","count":n}` geschrieben.
- **Rotation:** überschreitet die Datei 2 MiB, benennt der Agent sie in `afk-events.1.jsonl` um (ersetzt eine
  vorhandene) und beginnt eine neue Datei; `seq` läuft weiter. Launcher liest ab gemerktem Byte-Offset; ist die
  Datei kleiner als der Offset oder ist die erste `seq` der Datei > gemerkte + 1, zuerst den Rest von
  `afk-events.1.jsonl` ab Offset lesen, dann die neue Datei ab 0. Unvollständige letzte Zeile (ohne `\n`) nicht
  verarbeiten, beim nächsten Lesen erneut versuchen.
- Der Agent schreibt jede Zeile mit einem einzigen `write` und `flush` (kein Puffer über Zeilen hinweg).

## 5. `afk-outbox.jsonl` (Launcher → Agent)

```json
{"id":"b1","type":"chat","text":"bin gleich wieder da"}
{"id":"b2","type":"command","text":"spawn"}
```

| Feld | Bedeutung |
|---|---|
| `id` | eindeutig pro Sitzung, 1…64 Zeichen `[A-Za-z0-9._-]` |
| `type` | `chat` (Nachricht) oder `command` (Befehl, mit oder ohne führendes `/`) |
| `text` | 1…256 Zeichen, keine Zeilenumbrüche/Steuerzeichen (`§` verboten) |

- Der Launcher hängt Zeilen nur an (eine Zeile pro `write`, mit `\n`). Der Agent liest neue vollständige Zeilen
  einmal pro Sekunde ab seinem Offset.
- Jede Zeile bekommt genau ein `outbox_ack` (auch ungültige: `ok:false, error:"invalid"`). Fehler:
  `invalid`, `unsupported`, `not_connected` (keine Welt), `rate_limited` (siehe unten), `failed` (Ausnahme im Spiel).
  `ok:true` heißt: an das Spiel übergeben, wie beim Tippen im Chat – der Server kann es trotzdem ablehnen.
- **Rate-Limit** (Schutz vor Spam-Kicks): höchstens 1 Zeile pro 1,5 s, Burst 3. Zeilen darüber warten in der
  Warteschlange (max. 20); was nicht in die Warteschlange passt, bekommt `rate_limited`. Zeilen, die älter als 60 s
  in der Warteschlange stehen, ebenfalls.
- Zeilen, die **vor** dem Agent-Start in der Datei standen, werden nicht gesendet, sondern mit
  `ok:false, error:"stale"` bestätigt (Schutz vor Doppelsendung nach Neustart). Der Launcher leert die Datei
  deshalb vor dem Start.
- Aufräumen: Der Launcher darf die Datei leeren (auf 0 Byte kürzen), sobald jede geschriebene `id` bestätigt ist.
  Der Agent erkennt „Datei kleiner als Offset“ und beginnt bei 0. Größe max. 256 KiB, darüber ignoriert der Agent
  neue Zeilen (`error`-Ereignis `outbox_too_large`).
- Senden geschieht über die normalen Spielwege auf dem Spiel-Thread (wie eine getippte Nachricht), keine
  Pakete von Hand.

## 6. Lebenszyklus

1. **Vor dem Start** (Launcher): `afk-events.jsonl`, `afk-events.1.jsonl`, `afk-outbox.jsonl`, `afk-state.json`
   löschen; `afk.json` mit dem gewünschten Modus schreiben (`background` bei „Im Hintergrund starten“, sonst
   `foreground`); Agent-Jar prüfen, `-javaagent:` setzen; ggf. Optionen-Überlagerung (§1).
2. **Agent-Start:** `agent_start`, `afk-state.json`. Im Hintergrund-Modus wird das Fenster gleich beim Erzeugen
   unsichtbar gehalten (kein Aufblitzen), soweit LWJGL 3.
3. **Laufend:** Modus wechseln = `afk.json` neu schreiben. Ereignisse/Zustand lesen. Senden = Outbox.
4. **Fenster vom Nutzer geschlossen / Spiel beendet:** `agent_stop`, `afk-state.json` mit `running:false`.
5. **Nach dem Prozessende** (Launcher): Optionen-Überlagerung zurücknehmen, `afk.json` löschen. Die anderen Dateien
   dürfen für die Anzeige liegen bleiben bis zum nächsten Start.
6. **Absturz:** kein `agent_stop`; der Launcher erkennt das Prozessende selbst.

## 7. Fehlerfälle

| Fall | Verhalten |
|---|---|
| Agent-Jar fehlt/Hash falsch | Launcher startet ohne Agent (nur im Vordergrund) oder bricht AFK-Start ab, mit Meldung |
| `.vibe` nicht beschreibbar | Agent läuft ohne Dateien weiter (nur Vordergrund), schreibt nach stderr |
| `afk.json` kaputt | letzter gültiger Stand bleibt, `error afk_json_invalid` |
| keine Mappings auf obfuskierter Version | `game_state`/`send`/`reconnect` = false, `error mappings_missing`; Fenster/Bildrate/Ton/Chat-Log gehen trotzdem |
| Mappings passen nicht zur Version | dasselbe mit `mappings_invalid` |
| Hook-Fehler in LWJGL | betroffene Fähigkeit `false`, `error hook_failed`; das Spiel läuft unverändert weiter (Agent fängt alles) |
| LWJGL 2 (≤ 1.12.2) | `window:false` → Launcher versteckt per Win32; Bildrate/Ton gehen |
| Nutzer zeigt das Fenster selbst wieder an (Alt-Tab geht nicht, aber z. B. über den Launcher) | nur über `afk.json` – der Agent erzwingt im Hintergrund-Modus nichts erneut, außer beim Moduswechsel |

## 8. Was auf welcher Version/Loader geht (Agent 0.1.0)

| Bereich | Vanilla | Fabric/Quilt | NeoForge | Forge | Grundlage |
|---|---|---|---|---|---|
| Fenster aus/an, Taskleiste | 1.13+ (LWJGL 3) | 1.14+ | alle | 1.13+ | `GLFW.glfwHideWindow/ShowWindow` |
| Bildrate bremsen | alle | alle | alle | alle | `glfwSwapBuffers` / LWJGL 2 `Display.update` |
| Ton aus/an | alle | alle | alle | alle | `AL10.alListenerf(AL_GAIN)` |
| Chat-Ereignisse (Log) | 1.8+ | 1.14+ | alle | 1.8+ | `logs/latest.log`, `[CHAT]` |
| Spielzustand, Senden, Reconnect | 1.14.4+ (mit `mappings`) | 1.14.4+ (mit `mappings`, Intermediary aus dem Classpath) | 1.20.5+ (Mojang-Namen) | 1.20.6+ (Mojang-Namen); 1.17–1.20.4 nicht (SRG) | Reflection mit Mojang-Namen |

Ab 26.1 ist das Spiel nicht mehr obfuskiert: Spielzustand auf allen Loadern ohne `mappings`. Genaue, getestete
Matrix: vibey-client `docs/AFK-AGENT.md`.

## 9. Vibey-Client-Modul (optional, später)

Ist der Vibey Client in der Instanz, darf sein Modul `afk` zusätzlich `afk.json` lesen und im Hintergrund das
Welt-/GUI-Rendering aussetzen. Es schreibt **keine** eigenen Ereignisse in `afk-events.jsonl` (nur der Agent ist
Schreiber, damit `seq` eindeutig bleibt). Server-Regel-Schlüssel dafür: `afk` (sperrt nur die Vibey-Zusatzfunktion,
nicht den Agenten, der keine Spielregeln umgeht: keine Bewegung, kein Anti-AFK).

**Hinweis für die UI:** Der Agent bewegt den Spieler nie und umgeht keine AFK-Kicks. Viele Server kicken AFK-Spieler
oder verbieten AFK-Farmen – die UI soll das beim ersten AFK-Start erwähnen.
