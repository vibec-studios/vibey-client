import { writeFileSync, mkdirSync, rmSync, readdirSync, existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join, dirname } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { tmpdir } from "node:os";
import { wav } from "./_synth.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const only = process.argv.slice(2);
const packs = readdirSync(HERE).filter((d) => existsSync(join(HERE, d, "model", "_sounds.mjs")) && (!only.length || only.includes(d)));
const tmp = join(tmpdir(), `companio-synth-${process.pid}`);
mkdirSync(tmp, { recursive: true });
for (const pack of packs) {
  const { SOUNDS } = await import(pathToFileURL(join(HERE, pack, "model", "_sounds.mjs")).href);
  for (const [name, make] of Object.entries(SOUNDS)) {
    const w = join(tmp, `${pack}_${name.replaceAll("/", "_")}.wav`);
    writeFileSync(w, wav(make()));
    const out = join(HERE, pack, `${name}.ogg`);
    mkdirSync(dirname(out), { recursive: true });
    execFileSync("ffmpeg", ["-y", "-loglevel", "error", "-i", w, "-ac", "1", "-c:a", "libvorbis", "-q:a", "3", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", out]);
    console.log(`wrote ${out}`);
  }
}
rmSync(tmp, { recursive: true, force: true });
