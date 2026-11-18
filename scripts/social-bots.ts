// Scripted friend accounts (Mira, Juno, Kai, Nova, Lumen) for SocialGameTest, using vibe-backend's TypeScript SDK.
// Needs a LOCAL backend with AUTH_DEV_BYPASS=1. Run with tsx (e.g. from vibe-backend/apps/api):
//   VIBE_BACKEND=/path/to/vibe-backend tsx scripts/social-bots.ts http://localhost:3100 VibeyTester [relaySecretFile]
// relaySecretFile (optional): secret of a relay worker of that backend, enables the simulated hosting of Juno.
import { readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const sdkPath = resolve(process.env.VIBE_BACKEND ?? "../vibe-backend", "packages/sdk/src/index.ts");
// loaded in main() (no top-level await: tsx may run this file as CommonJS)
let VibeClient: any;
let VibeGateway: any;
type VibeClient = any;
type VibeGateway = any;

const base = process.argv[2] ?? "http://localhost:3100";
const GAME = process.argv[3] ?? "VibeyTester";
const relaySecret = process.argv[4] ? readFileSync(process.argv[4], "utf8").trim() : null;
const client = { name: "vibe-bot", version: "1.0.0", platform: "node" };
const log = (...a: unknown[]) => console.log(new Date().toISOString().slice(11, 19), ...a);
const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

type Bot = { name: string; api: VibeClient; gw?: VibeGateway; id: string };

async function login(name: string, online = true, presence?: any): Promise<Bot> {
  const api = new VibeClient({ baseUrl: base });
  const { serverId } = await api.auth.challenge();
  const r = await api.auth.verify(name, serverId, client);
  const bot: Bot = { name, api, id: r.user.id };
  if (online) {
    bot.gw = new VibeGateway(api, { presence });
    bot.gw.on("error", (e) => log(name, "gateway error", e));
    bot.gw.connect();
  }
  log("logged in", name, r.user.id);
  return bot;
}

async function befriend(a: Bot, b: Bot) {
  try {
    const r = await a.api.friends.request({ userId: b.id });
    if (r.status === "pending" && r.request) await b.api.friends.accept(r.request.id);
  } catch (e: any) {
    if (e.code !== "conflict") log("befriend", a.name, b.name, e.code, e.message);
  }
}

async function relay(path: string, body: unknown) {
  if (!relaySecret) return null;
  const res = await fetch(`${base}/internal/relay/v1${path}`, {
    method: "POST",
    headers: { authorization: `Bearer ${relaySecret}`, "x-relay-id": "local-test", "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) log("relay", path, res.status, await res.text());
  return res.status === 204 ? null : res.json().catch(() => null);
}

async function main() {
  ({ VibeClient, VibeGateway } = await import(pathToFileURL(sdkPath).href));
  const mira = await login("Mira", true, { status: "online", activity: { type: "playing", world: "Seal Island", mcVersion: "1.21.11", joinable: false } });
  const juno = await login("Juno", true, { status: "busy", activity: { type: "playing", server: "play.vibe.example", mcVersion: "1.21.11", joinable: false } });
  const kai = await login("Kai", true, { status: "away", activity: { type: "launcher", instanceName: "Create: Seals", joinable: false } });
  const nova = await login("Nova", false);
  const lumen = await login("Lumen", false);
  await befriend(mira, juno);
  await mira.api.me.update({ statusText: "building a seal statue" });
  await juno.api.me.update({ statusText: "redstone all day" });

  // wait for the game account
  let gameId: string | null = null;
  while (!gameId) {
    try {
      const res = await mira.api.users.search(GAME, 5);
      gameId = res.find((u) => u.name.toLowerCase() === GAME.toLowerCase())?.id ?? null;
    } catch (e: any) {
      log("search", e.code);
    }
    if (!gameId) await sleep(1000);
  }
  log("game user", GAME, gameId);

  // reset relations from a previous run so the scenario repeats
  for (const b of [mira, juno, kai, nova, lumen]) {
    await b.api.friends.remove(gameId).catch(() => {});
    for (const r of await b.api.friends.requests()) {
      if (r.from.id !== gameId && r.to.id !== gameId) continue;
      if (r.direction === "incoming") await b.api.friends.decline(r.id).catch(() => {});
      else await b.api.friends.cancel(r.id).catch(() => {});
    }
    for (const c of await b.api.chats.list()) {
      if (c.type === "group" && c.members.some((m) => m.user.id === gameId)) await b.api.chats.removeMember(c.id, b.id).catch(() => {});
    }
  }
  log("reset done");

  const fromGame = (m: { authorId: string | null }) => m.authorId === gameId;

  // Mira: request; when accepted → DM with a few messages; answers every DM from the game.
  mira.gw!.on("friend.added", async (f) => {
    if (f.user.id !== gameId) return;
    log("Mira: now friends with game");
    const chat = await mira.api.chats.openDm(gameId!);
    for (const t of ["hey! welcome to Vibe", "wanna play later? I'm building a seal statue on Seal Island",
      "this one is a bit longer to see how wrapping looks in the sidebar: the statue has a little ice crown, a backpack and a very serious face"]) {
      await sleep(900);
      await mira.api.chats.send(chat.id, t, { nonce: `mira-${Date.now()}` });
    }
  });
  mira.gw!.on("message.created", async (m) => {
    if (!fromGame(m) || m.kind !== "text") return;
    log("Mira got", m.content);
    await sleep(600);
    mira.gw!.typing(m.chatId);
    await sleep(2500);
    await mira.api.chats.read(m.chatId, m.id).catch(() => {});
    await mira.api.chats.send(m.chatId, `got it: "${m.content}"`, { nonce: `mira-r-${Date.now()}` });
  });

  // Juno: accepts the game's request, then a group chat, hosting (simulated relay session) and an invite.
  juno.gw!.on("friend.request", async (r) => {
    if (r.direction !== "incoming" || r.from.id !== gameId) return;
    await sleep(800);
    await juno.api.friends.accept(r.id);
    log("Juno accepted the game's request");
    const group = await juno.api.chats.createGroup("Build crew", [mira.id, gameId!]);
    await sleep(500);
    await juno.api.chats.send(group.id, "welcome to the build crew!");
    await sleep(500);
    await mira.api.chats.send(group.id, "finally :) bring snacks");
    // Hosting via the relay API
    if (relaySecret) {
      await relay("/heartbeat", { version: "test", hosts: 1, players: 0, uptime: 10 });
      const auth = await relay("/authorize", { uuid: juno.id.replace(/-/g, ""), name: "Juno", ip: "127.0.0.1", client: "mod", kind: "world", slot: 0, mc: "1.21.11", version: "test", active: 0 });
      log("relay authorize", auth);
      await relay("/events", { events: [{ type: "host_online", uuid: juno.id.replace(/-/g, ""), slot: 0, address: "juno-test.vibey.localhost", at: new Date().toISOString(), ip: "127.0.0.1" }] });
      setInterval(() => relay("/heartbeat", { version: "test", hosts: 1, players: 0, uptime: 60 }), 30_000);
      await sleep(800);
      const mine = await juno.api.hosting.mine();
      const s = mine.find((x) => x.status === "online");
      log("Juno host session", s?.id, s?.address);
      juno.gw!.setPresence({ status: "online", activity: { type: "hosting", world: "Juno's Castle", mcVersion: "1.21.11", joinable: true,
        join: { type: "relay", address: "juno-test.vibey.localhost", port: 25565, hostSessionId: s?.id ?? null } } });
      await sleep(4000);
      const inv = await juno.api.invites.send({ kind: "world", toUserIds: [gameId!], target: { name: "Juno's Castle", mcVersion: "1.21.11", loader: "fabric" },
        hostSessionId: s?.id, message: "come build with us" });
      log("Juno invited the game", inv.map((i) => i.id));
    }
  });
  // The game sets its status text to "nova please" → Nova comes online (friend-online toast), offline again later.
  mira.gw!.on("user.updated", (u) => {
    if (u.id !== gameId || u.statusText !== "nova please" || nova.gw) return;
    log("Nova goes online");
    nova.gw = new VibeGateway(nova.api, { presence: { status: "online", activity: { type: "launcher", instanceName: "Seal Pack", joinable: false } } });
    nova.gw.connect();
    setTimeout(() => {
      nova.gw?.disconnect();
      nova.gw = undefined;
    }, 30_000);
  });
  juno.gw!.on("presence.update", (p) => {
    if (p.userId === gameId) log("Juno sees game presence", p.status, JSON.stringify(p.activity));
  });
  juno.gw!.on("invite.received", async (i) => {
    if (i.from.id !== gameId) return;
    log("Juno got invite from game", i.kind, i.target.name, "hostSession", i.hostSessionId);
    await sleep(500);
    const r = await juno.api.invites.accept(i.id).catch((e: any) => ({ error: e.code }));
    log("Juno accepted game invite →", JSON.stringify((r as any).connection ?? r));
  });
  juno.gw!.on("message.created", (m) => {
    if (fromGame(m)) log("Juno saw", m.content);
  });

  await mira.api.friends.request({ name: GAME }).then(() => log("Mira requested")).catch((e) => log("Mira request", e.code));
  await sleep(300);
  await kai.api.friends.request({ name: GAME }).then(() => log("Kai requested")).catch((e) => log("Kai request", e.code));
  await nova.api.friends.request({ name: GAME }).then(() => log("Nova requested")).catch((e) => log("Nova request", e.code));
  void lumen; // Lumen only receives (and never answers) the game's request
  log("bots ready");
  await new Promise(() => {});
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
