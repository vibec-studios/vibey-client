// Push every .vibey of a folder into a running Vibey Client over the Blockbench live link (devlink, SPEC §8).
// The items appear in the game as local DEV items (only you see them; nothing is uploaded). The first cosmetic of each
// slot is equipped, everything else (other cosmetics, emotes) is only loaded – nothing is played.
//
//   node tools/push-to-game.mjs [folder] [--game-dir <dir>] [--instance <launcher instance id>] [--no-equip]
//   npm run push:polar                         (= node tools/push-to-game.mjs dist/polar-pals)
//
// Finding the game: --game-dir wins. Otherwise every known game dir is checked for vibeyclient/devlink.json (written
// while "Creator mode" is on): %APPDATA%\.minecraft, the Vibe Launcher instances (%APPDATA%\VibeLauncher\instances\
// <id>\minecraft, or $VIBE_LAUNCHER_HOME\instances\…) and the dev run folders of this repo (versions/*/run). Files
// whose game process (pid) is no longer running are ignored; with several running games the newest file wins.
import { readFileSync, readdirSync, statSync, existsSync } from "node:fs";
import { join, resolve, dirname, basename } from "node:path";
import { homedir } from "node:os";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
const opt = (k) => {
  const i = args.indexOf(k);
  if (i < 0) return null;
  const v = args[i + 1];
  args.splice(i, 2);
  return v;
};
const flag = (k) => {
  const i = args.indexOf(k);
  if (i >= 0) args.splice(i, 1);
  return i >= 0;
};
const gameDirArg = opt("--game-dir");
const instance = opt("--instance");
const noEquip = flag("--no-equip");
if (flag("--help") || flag("-h")) {
  console.log(readFileSync(fileURLToPath(import.meta.url), "utf8").split("\n").slice(0, 12).join("\n").replace(/^\/\/ ?/gm, ""));
  process.exit(0);
}
const folder = resolve(args[0] ?? join(ROOT, "dist", "polar-pals"));

const C = process.stdout.isTTY ? { ok: "\x1b[32m", err: "\x1b[31m", dim: "\x1b[2m", b: "\x1b[1m", r: "\x1b[0m" } : { ok: "", err: "", dim: "", b: "", r: "" };

// ------------------------------------------------------------------------------------------------ find the game
function launcherHome() {
  if (process.env.VIBE_LAUNCHER_HOME) return process.env.VIBE_LAUNCHER_HOME;
  if (process.platform === "win32") return join(process.env.APPDATA ?? join(homedir(), "AppData", "Roaming"), "VibeLauncher");
  if (process.platform === "darwin") return join(homedir(), "Library", "Application Support", "VibeLauncher");
  return join(process.env.XDG_DATA_HOME ?? join(homedir(), ".local", "share"), "VibeLauncher");
}

function defaultMinecraft() {
  if (process.platform === "win32") return join(process.env.APPDATA ?? join(homedir(), "AppData", "Roaming"), ".minecraft");
  if (process.platform === "darwin") return join(homedir(), "Library", "Application Support", "minecraft");
  return join(homedir(), ".minecraft");
}

function candidates() {
  if (gameDirArg) return [resolve(gameDirArg)];
  const out = [];
  const inst = join(launcherHome(), "instances");
  if (instance) return [join(inst, instance, "minecraft")];
  out.push(defaultMinecraft());
  try {
    for (const d of readdirSync(inst)) out.push(join(inst, d, "minecraft"));
  } catch {}
  try {
    for (const v of readdirSync(join(ROOT, "..", "versions"))) out.push(join(ROOT, "..", "versions", v, "run"));
  } catch {}
  return out.filter((d) => existsSync(d));
}

function alive(pid) {
  if (!Number.isInteger(pid) || pid <= 0) return true; // unknown → assume yes
  try {
    process.kill(pid, 0);
    return true;
  } catch (e) {
    return e.code === "EPERM";
  }
}

function findLink() {
  const found = [];
  for (const dir of candidates()) {
    const f = join(dir, "vibeyclient", "devlink.json");
    if (!existsSync(f)) continue;
    try {
      const j = JSON.parse(readFileSync(f, "utf8"));
      const port = Number(j.port);
      if (!Number.isInteger(port) || typeof j.token !== "string" || !j.token) continue;
      found.push({ dir, file: f, port, token: j.token, pid: j.pid, alive: alive(j.pid), mtime: statSync(f).mtimeMs });
    } catch {}
  }
  const live = found.filter((x) => x.alive).sort((a, b) => b.mtime - a.mtime);
  return { live, stale: found.filter((x) => !x.alive) };
}

// ------------------------------------------------------------------------------------------------ websocket client
function connect(link) {
  return new Promise((res, rej) => {
    const ws = new WebSocket(`ws://127.0.0.1:${link.port}`);
    const waiters = [];
    let welcome = null;
    const timer = setTimeout(() => rej(new Error("no welcome from the game within 5 s")), 5000);
    ws.addEventListener("open", () => ws.send(JSON.stringify({ type: "hello", token: link.token, client: "vibey-push", version: "1.0.0" })));
    ws.addEventListener("error", () => rej(new Error(`cannot connect to 127.0.0.1:${link.port} – is the game running with Creator mode on?`)));
    ws.addEventListener("close", (e) => {
      const why = e.code === 4001 ? "the game rejected the token (regenerated? restart Creator mode)" : `connection closed (${e.code} ${e.reason || ""})`;
      for (const w of waiters.splice(0)) w.rej(new Error(why));
      if (!welcome) rej(new Error(why));
    });
    ws.addEventListener("message", (ev) => {
      let m;
      try {
        m = JSON.parse(typeof ev.data === "string" ? ev.data : Buffer.from(ev.data).toString("utf8"));
      } catch {
        return;
      }
      if (m.type === "welcome") {
        welcome = m;
        clearTimeout(timer);
        res({ ws, welcome, waiters });
        return;
      }
      if (m.type === "log") return;
      const i = waiters.findIndex((w) => w.match(m));
      if (i >= 0) waiters.splice(i, 1)[0].res(m);
    });
  });
}

function request(link, msg, match, ms = 20000) {
  return new Promise((res, rej) => {
    const w = { match, res, rej };
    link.waiters.push(w);
    setTimeout(() => {
      const i = link.waiters.indexOf(w);
      if (i >= 0) {
        link.waiters.splice(i, 1);
        rej(new Error("the game did not answer within " + ms / 1000 + " s"));
      }
    }, ms);
    link.ws.send(JSON.stringify(msg));
  });
}

// ------------------------------------------------------------------------------------------------ main
// rarer items first, so the centrepiece of each slot is the one that gets equipped
const RANK = { legendary: 0, epic: 1, rare: 2, common: 3 };
const rarityOf = (f) => {
  try {
    return RANK[JSON.parse(readFileSync(join(folder, f), "utf8")).rarity] ?? 9;
  } catch {
    return 9;
  }
};
const files = existsSync(folder)
  ? readdirSync(folder).filter((f) => f.endsWith(".vibey")).map((f) => [rarityOf(f), f]).sort((a, b) => a[0] - b[0] || a[1].localeCompare(b[1])).map(([, f]) => f)
  : [];
if (!files.length) {
  console.error(`${C.err}No .vibey files in ${folder}${C.r}`);
  process.exit(1);
}
const { live, stale } = findLink();
if (!live.length) {
  console.error(`${C.err}${C.b}No running game with Creator mode found.${C.r}`);
  console.error(`Turn on Creator mode in the game (Vibey menu → Blockbench link) – it writes <game dir>/vibeyclient/devlink.json.`);
  const dirs = candidates();
  const shown = dirs.slice(0, 6).join("\n  ") + (dirs.length > 6 ? `\n  … and ${dirs.length - 6} more` : "");
  console.error(`Looked in ${dirs.length} game folder(s)${dirs.length ? ":\n  " + shown : ""}`);
  if (stale.length) console.error(`Ignored (game no longer running): ${stale.map((s) => s.file).join(", ")}`);
  console.error(`Use --game-dir <dir> (or --instance <id> for a Vibe Launcher instance) to point at the right game.`);
  process.exit(2);
}
const target = live[0];
if (live.length > 1) console.log(`${C.dim}Several games are running; using the newest: ${target.dir}${C.r}`);

let link;
try {
  link = await connect(target);
} catch (e) {
  console.error(`${C.err}${e.message}${C.r}`);
  process.exit(3);
}
const w = link.welcome;
console.log(`${C.b}Connected${C.r} to Minecraft ${w.game} · Vibey Client ${w.mod}${w.player ? " · " + w.player : ""}  ${C.dim}(${target.file})${C.r}`);
console.log(`Pushing ${files.length} item(s) from ${folder}\n`);

const equippedSlots = new Set();
let failed = 0;
for (const f of files) {
  let bundle;
  try {
    bundle = JSON.parse(readFileSync(join(folder, f), "utf8"));
  } catch (e) {
    console.log(`${C.err}✗${C.r} ${f}  ${C.err}not valid JSON: ${e.message}${C.r}`);
    failed++;
    continue;
  }
  const slot = bundle.kind === "emote" ? "emote" : bundle.cosmetic?.slot ?? "?";
  const equip = !noEquip && bundle.kind === "cosmetic" && !equippedSlots.has(slot);
  if (equip) equippedSlots.add(slot);
  const action = equip ? "equip" : "none";
  try {
    const r = await request(link, { type: "push", bundle, action }, (m) => (m.type === "pushed" || m.type === "error") && (m.id === bundle.id || m.id == null));
    if (r.type === "pushed" && r.ok !== false) {
      const warn = r.warnings?.length ? `  ${C.dim}warnings: ${r.warnings.join("; ")}${C.r}` : "";
      console.log(`${C.ok}✓${C.r} ${bundle.id.padEnd(30)} ${slot.padEnd(7)} ${equip ? "loaded + equipped" : "loaded"}${warn}`);
    } else {
      failed++;
      console.log(`${C.err}✗ ${bundle.id.padEnd(30)} ${slot.padEnd(7)} ${r.message ?? "rejected"}${C.r}`);
    }
  } catch (e) {
    failed++;
    console.log(`${C.err}✗ ${bundle.id.padEnd(30)} ${slot.padEnd(7)} ${e.message}${C.r}`);
  }
}
link.ws.close();
console.log(`\n${failed ? C.err : C.ok}${files.length - failed}/${files.length} pushed${C.r}. Open the wardrobe in game – the items carry a DEV badge and are only visible to you.`);
process.exit(failed ? 1 : 0);
