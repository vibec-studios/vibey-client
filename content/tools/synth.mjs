// Synthesizes the few original sound effects used by the starter set (no samples, pure math) and encodes them
// to mono OGG Vorbis with ffmpeg/libvorbis. The OGGs are committed in content/sounds, so `npm run build` does not
// need ffmpeg – run `npm run synth` only when you change a sound here.
import { writeFileSync, mkdirSync, rmSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { tmpdir } from "node:os";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = join(ROOT, "sounds");
const RATE = 44100;

function wav(samples) {
  const n = samples.length;
  const buf = Buffer.alloc(44 + n * 2);
  buf.write("RIFF", 0); buf.writeUInt32LE(36 + n * 2, 4); buf.write("WAVE", 8);
  buf.write("fmt ", 12); buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(1, 22);
  buf.writeUInt32LE(RATE, 24); buf.writeUInt32LE(RATE * 2, 28); buf.writeUInt16LE(2, 32); buf.writeUInt16LE(16, 34);
  buf.write("data", 36); buf.writeUInt32LE(n * 2, 40);
  for (let i = 0; i < n; i++) buf.writeInt16LE(Math.round(Math.max(-1, Math.min(1, samples[i])) * 32000), 44 + i * 2);
  return buf;
}

function render(seconds, fn) {
  const out = new Float32Array(Math.round(seconds * RATE));
  for (let i = 0; i < out.length; i++) out[i] = fn(i / RATE);
  // normalize to -1 dBFS and fade the last 5 ms
  let peak = 0;
  for (const v of out) peak = Math.max(peak, Math.abs(v));
  const g = peak ? 0.89 / peak : 1;
  const fade = Math.round(0.005 * RATE);
  for (let i = 0; i < out.length; i++) out[i] *= g * Math.min(1, (out.length - i) / fade);
  return out;
}

/** Phase-continuous oscillator for a time-varying frequency. */
function osc() {
  let phase = 0, last = 0;
  return (t, f) => {
    phase += 2 * Math.PI * f * (t - last);
    last = t;
    return Math.sin(phase);
  };
}

const SOUNDS = {
  // bubbly pop: fast downward pitch sweep, very short
  pop: () => {
    const o = osc();
    return render(0.16, (t) => {
      const f = 380 + 900 * Math.exp(-t * 38);
      return o(t, f) * Math.exp(-t * 26) * Math.min(1, t * 400);
    });
  },
  // sparkly chime: E6–B6–E7 arpeggio with soft bell partials
  chime: () => {
    const notes = [[0, 1318.5], [0.06, 1975.5], [0.12, 2637.0]];
    return render(1.1, (t) => {
      let s = 0;
      for (const [t0, f] of notes) {
        const u = t - t0;
        if (u < 0) continue;
        const env = Math.exp(-u * 4.2) * Math.min(1, u * 300);
        s += env * (Math.sin(2 * Math.PI * f * u) + 0.35 * Math.sin(2 * Math.PI * f * 2.76 * u) * Math.exp(-u * 6));
      }
      return s;
    });
  },
  // cute squeak: upward glide with a little vibrato and a few harmonics
  squeak: () => {
    const o1 = osc(), o2 = osc(), o3 = osc();
    return render(0.32, (t) => {
      const glide = t < 0.12 ? 760 + 900 * (t / 0.12) : 1660 - 260 * ((t - 0.12) / 0.2);
      const f = glide * (1 + 0.025 * Math.sin(2 * Math.PI * 28 * t));
      const env = Math.min(1, t * 60) * Math.exp(-Math.max(0, t - 0.1) * 9);
      return env * (o1(t, f) + 0.4 * o2(t, f * 2) + 0.15 * o3(t, f * 3));
    });
  },
};

mkdirSync(OUT, { recursive: true });
const tmp = join(tmpdir(), `vibey-synth-${process.pid}`);
mkdirSync(tmp, { recursive: true });
for (const [name, make] of Object.entries(SOUNDS)) {
  const w = join(tmp, `${name}.wav`);
  writeFileSync(w, wav(make()));
  const out = join(OUT, `${name}.ogg`);
  execFileSync("ffmpeg", ["-y", "-loglevel", "error", "-i", w, "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", out]);
  console.log(`wrote ${out}`);
}
rmSync(tmp, { recursive: true, force: true });
