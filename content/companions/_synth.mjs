export const RATE = 44100;

export function wav(samples) {
  const n = samples.length;
  const buf = Buffer.alloc(44 + n * 2);
  buf.write("RIFF", 0); buf.writeUInt32LE(36 + n * 2, 4); buf.write("WAVE", 8);
  buf.write("fmt ", 12); buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(1, 22);
  buf.writeUInt32LE(RATE, 24); buf.writeUInt32LE(RATE * 2, 28); buf.writeUInt16LE(2, 32); buf.writeUInt16LE(16, 34);
  buf.write("data", 36); buf.writeUInt32LE(n * 2, 40);
  for (let i = 0; i < n; i++) buf.writeInt16LE(Math.round(Math.max(-1, Math.min(1, samples[i])) * 32000), 44 + i * 2);
  return buf;
}

export function render(seconds, fn, gain = 0.89) {
  const out = new Float32Array(Math.round(seconds * RATE));
  for (let i = 0; i < out.length; i++) out[i] = fn(i / RATE);
  let peak = 0;
  for (const v of out) peak = Math.max(peak, Math.abs(v));
  const g = peak ? gain / peak : 1;
  const fade = Math.round(0.006 * RATE);
  for (let i = 0; i < out.length; i++) out[i] *= g * Math.min(1, (out.length - i) / fade);
  return out;
}

export function osc() {
  let phase = 0, last = 0;
  return (t, f) => {
    phase += 2 * Math.PI * f * (t - last);
    last = t;
    return Math.sin(phase);
  };
}

export function noise(seed) {
  let s = seed >>> 0;
  return () => {
    s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
    return s / 2147483648 - 1;
  };
}

export function lowpass(alpha) {
  let y = 0;
  return (x) => (y += alpha * (x - y));
}

export function honk(t, t0, len, f0, f1, o) {
  const u = t - t0;
  if (u < 0 || u > len) return 0;
  const env = Math.min(1, u * 90) * Math.exp(-Math.max(0, u - len * 0.35) * 14);
  const f = f0 + (f1 - f0) * (u / len) + 6 * Math.sin(2 * Math.PI * 30 * u);
  let s = 0;
  for (let h = 1; h <= 6; h++) s += o[h - 1](t, f * h) * [1, 0.7, 0.5, 0.35, 0.22, 0.12][h - 1];
  return Math.tanh(s * 1.6) * env;
}
