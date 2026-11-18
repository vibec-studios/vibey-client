// Live sync check for cosmetics/emotes (SPEC §6/§7) against a LOCAL vibe-backend with AUTH_DEV_BYPASS=1:
// logs in a scripted watcher account, subscribes to the given game accounts and prints every loadout.updated /
// emote.played event plus the persisted loadouts (POST /cosmetics/lookup) as JSON lines.
//   VIBE_BACKEND=/path/to/vibe-backend tsx scripts/live-sync-watch.ts http://127.0.0.1:3200 Alex Bee
// Optional actions of the watcher account itself (it is not in any world, so this only shows that the gateway path
// works both ways): WATCH_EQUIP="cape=vibey:vibe_cape" WATCH_EMOTE="vibey:wave".
import { createHash } from "node:crypto";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const sdkPath = resolve(process.env.VIBE_BACKEND ?? "../vibe-backend", "packages/sdk/src/index.ts");
const base = process.argv[2] ?? "http://127.0.0.1:3200";
const names = process.argv.slice(3);
if (!/^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/.test(base)) throw new Error("local backends only (dev login)");
const out = (o: Record<string, unknown>) => console.log(JSON.stringify({ t: new Date().toISOString().slice(11, 23), ...o }));

/** Minecraft offline UUID of a name (what the dev login gives a game account). */
function offlineUuid(name: string): string {
  const h = createHash("md5").update("OfflinePlayer:" + name, "utf8").digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString("hex");
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

async function main() {
  const { VibeClient, VibeGateway } = await import(pathToFileURL(sdkPath).href);
  const api = new VibeClient({ baseUrl: base });
  const { serverId } = await api.auth.challenge();
  const me = await api.auth.verify("SyncWatcher", serverId, { name: "vibe-bot", version: "1.0.0", platform: "node" });
  const ids = names.map(offlineUuid);
  const byId = Object.fromEntries(names.map((n, i) => [ids[i], n]));
  out({ ev: "login", self: me.user.id, watching: byId });
  const gw = new VibeGateway(api, {});
  gw.on("state", (s: string) => {
    out({ ev: "gateway", state: s });
    if (s === "ready") gw.subscribeCosmetics(ids);
  });
  gw.on("loadout.updated", (d: any) => out({ ev: "loadout.updated", who: byId[d.userId] ?? d.userId, slots: d.slots }));
  gw.on("emote.played", (d: any) => out({ ev: "emote.played", who: byId[d.userId] ?? d.userId, emoteId: d.emoteId, action: d.action, at: d.at }));
  gw.connect();

  let last = "";
  const poll = async () => {
    try {
      const { users } = await api.cosmetics.lookup(ids);
      const s = JSON.stringify(Object.fromEntries(Object.entries(users).map(([k, v]: any) => [byId[k] ?? k, v.slots])));
      if (s !== last) out({ ev: "persisted", users: JSON.parse(s) });
      last = s;
    } catch (e: any) {
      out({ ev: "lookup.error", code: e.code, message: e.message });
    }
  };
  await poll();
  setInterval(poll, 2000);

  if (process.env.WATCH_EQUIP) {
    const [slot, id] = process.env.WATCH_EQUIP.split("=");
    await api.cosmetics.setLoadout({ slots: { [slot]: id }, emoteWheel: Array(8).fill(null) });
    out({ ev: "self.equip", slot, id });
  }
  if (process.env.WATCH_EMOTE) setTimeout(() => (gw.emote(process.env.WATCH_EMOTE), out({ ev: "self.emote", id: process.env.WATCH_EMOTE })), 3000);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
