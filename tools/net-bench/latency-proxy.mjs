#!/usr/bin/env node
// TCP proxy that adds a fixed one-way delay in both directions (RTT = 2 × delay), so a local backend behaves like
// one across the internet (HTTP and the /ws gateway alike). Order of the bytes is kept.
//
//   node tools/net-bench/latency-proxy.mjs --listen 3491 --target 127.0.0.1:3490 --delay 20
import net from "node:net";

const opt = (name, def) => {
  const i = process.argv.indexOf("--" + name);
  return i > 0 ? process.argv[i + 1] : def;
};
const listen = Number(opt("listen", "3491"));
const [host, port] = opt("target", "127.0.0.1:3490").split(":");
const delay = Number(opt("delay", "20"));

/** Pipes a → b, each chunk `delay` ms later (a queue keeps the order). */
function delayed(a, b) {
  a.on("data", (chunk) => setTimeout(() => b.writable && b.write(chunk), delay));
  a.on("end", () => setTimeout(() => b.end(), delay));
  a.on("error", () => b.destroy());
}

let conns = 0;
net.createServer((client) => {
  conns++;
  // the TCP handshake to the "far" server costs one RTT too
  client.pause();
  setTimeout(() => {
    const upstream = net.connect(Number(port), host, () => {
      client.resume();
    });
    delayed(client, upstream);
    delayed(upstream, client);
    upstream.on("error", () => client.destroy());
  }, 2 * delay);
}).listen(listen, "127.0.0.1", () => console.log(`latency proxy :${listen} → ${host}:${port}, +${delay} ms each way`));

process.on("uncaughtException", (e) => console.error("proxy error (kept running):", e.message));
process.on("SIGINT", () => {
  console.log(`connections: ${conns}`);
  process.exit(0);
});
