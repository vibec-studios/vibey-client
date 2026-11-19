#!/usr/bin/env node
// Runs the network timing harness N times (one fresh JVM per run) and prints medians. See docs/NETWORK.md.
//
//   node tools/net-bench/run.mjs <public|auth> <before|after> <cold|warm> [--base URL] [--runs 5] [--busy 0] [--json out.json]
//
// Compiles tools/net-bench/*.java together with the Minecraft-free network classes of the client
// (src/main/java/dev/henny/vibeyclient/backend/net, api/service/VibeBackend, backend/J) against Gson from the Gradle cache.
// warm runs are preceded by one unmeasured priming run (fills the disk cache / stores the refresh token).
import { execFileSync, spawnSync } from "node:child_process";
import { existsSync, mkdirSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { join, resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const [scenario, variant, cache] = process.argv.slice(2, 5);
const opt = (name, def) => {
  const i = process.argv.indexOf("--" + name);
  return i > 0 ? process.argv[i + 1] : def;
};
const base = opt("base", "https://vibe.axolite.app");
const runs = Number(opt("runs", "5"));
const busy = opt("busy", "0");
if (!scenario || !variant || !cache) {
  console.error("usage: run.mjs <public|auth> <before|after> <cold|warm> [--base URL] [--runs 5] [--busy 0] [--json out.json]");
  process.exit(2);
}

function findGson() {
  const dir = join(homedir(), ".gradle/caches/modules-2/files-2.1/com.google.code.gson/gson");
  for (const v of readdirSync(dir).sort().reverse()) {
    for (const h of readdirSync(join(dir, v))) {
      const jar = join(dir, v, h, `gson-${v}.jar`);
      if (existsSync(jar)) return jar;
    }
  }
  throw new Error("Gson not found in the Gradle cache");
}

function walk(dir, out = []) {
  for (const f of readdirSync(dir)) {
    const p = join(dir, f);
    if (statSync(p).isDirectory()) walk(p, out);
    else if (p.endsWith(".java")) out.push(p);
  }
  return out;
}

const gson = findGson();
const classes = join(root, "build/net-bench/classes");
mkdirSync(classes, { recursive: true });
const src = [join(root, "tools/net-bench/NetBench.java")];
const netDir = join(root, "src/main/java/dev/henny/vibeyclient/backend/net");
if (variant === "after") {
  src.push(join(root, "tools/net-bench/NetBenchAfter.java"), ...walk(netDir),
    join(root, "src/main/java/dev/henny/vibeyclient/api/service/VibeBackend.java"),
    join(root, "src/main/java/dev/henny/vibeyclient/backend/J.java"));
}
execFileSync("javac", ["-nowarn", "-encoding", "UTF-8", "-cp", gson, "-d", classes, ...src], { stdio: "inherit" });
const sep = process.platform === "win32" ? ";" : ":";
const cp = [classes, gson].join(sep);

function once(c) {
  const r = spawnSync("java", [`-Dbench.cache=${join(root, "build/net-bench/cache")}`, "-cp", cp, "NetBench", scenario, variant, c, base, busy],
    { encoding: "utf8", timeout: 120_000 });
  const line = (r.stdout || "").split(/\r?\n/).find((l) => l.startsWith("RESULT "));
  if (!line) throw new Error(`run failed:\n${r.stdout}\n${r.stderr}`);
  return JSON.parse(line.slice(7));
}

const median = (xs) => {
  const s = xs.filter((x) => x != null).sort((a, b) => a - b);
  if (!s.length) return null;
  return s.length % 2 ? s[(s.length - 1) / 2] : Math.round((s[s.length / 2 - 1] + s[s.length / 2]) / 2);
};

if (cache === "warm") once("cold"); // priming
const results = [];
for (let i = 0; i < runs; i++) results.push(once(cache));

const marks = [...new Set(results.flatMap((r) => Object.keys(r.marks)))];
const summary = { scenario, variant, cache, base, busy: Number(busy), runs };
for (const m of marks) summary[m] = median(results.map((r) => r.marks[m]));
summary.requests = median(results.map((r) => r.requests));
summary.netRequests = median(results.map((r) => r.netRequests));
summary.bytes = median(results.map((r) => r.bytes));
summary.connections = median(results.map((r) => r.connections));
// per-label medians of TTFB/total and counts (first run's set of labels)
const byLabel = {};
for (const r of results) {
  for (const q of r.reqs) {
    const k = q.label;
    (byLabel[k] ??= { n: 0, ttfb: [], total: [], version: q.version, status: new Set() });
    byLabel[k].n++;
    byLabel[k].ttfb.push(q.ttfb);
    byLabel[k].total.push(q.total);
    byLabel[k].status.add(q.status);
  }
}
summary.perRequest = Object.fromEntries(Object.entries(byLabel).map(([k, v]) => [k, {
  perRun: +(v.n / runs).toFixed(1), ttfb: median(v.ttfb), total: median(v.total), version: v.version, status: [...v.status].join("/"),
}]));
console.log(JSON.stringify(summary, null, 2));
const out = opt("json");
if (out) writeFileSync(out, JSON.stringify({ summary, results }, null, 2));
