import { render, osc, noise, lowpass } from "../../_synth.mjs";

const soft = (s, k = 3) => Math.tanh(s * k) / Math.tanh(k);
const env = (u, len, a = 0.005, r = 0.04) => (u < 0 || u > len ? 0 : Math.min(1, u / a) * Math.min(1, (len - u) / r));

function tones(notes, k = 2.4) {
  const o = osc();
  return (t) => {
    for (const [t0, len, f0, f1 = f0] of notes) {
      const u = t - t0;
      if (u >= 0 && u <= len) return soft(o(t, f0 + (f1 - f0) * (u / len)), k) * env(u, len);
    }
    o(t, 440);
    return 0;
  };
}

export const SOUNDS = {
  "sounds/beep": () => render(0.16, tones([[0, 0.15, 1320]])),
  "sounds/boop": () => render(0.2, tones([[0, 0.19, 520, 430]], 2)),
  "sounds/chirp": () => render(0.46, tones([[0, 0.1, 880], [0.12, 0.1, 1175], [0.24, 0.2, 1568, 1760]])),
  "sounds/startup": () => render(0.7, tones([[0, 0.12, 523], [0.13, 0.12, 659], [0.26, 0.12, 784], [0.39, 0.3, 1047, 1060]])),
  "sounds/error": () => {
    const o = osc(), n = noise(5);
    return render(0.55, (t) => {
      const gate = Math.floor(t / 0.09) % 2 ? 0.25 : 1;
      return (soft(o(t, 150), 6) * 0.8 + n() * 0.15) * gate * env(t, 0.55, 0.005, 0.08);
    }, 0.7);
  },
  "sounds/whirr": () => {
    const o = osc(), n = noise(9), lp = lowpass(0.08);
    return render(0.42, (t) => {
      const f = 260 + 380 * Math.sin(Math.PI * Math.min(1, t / 0.42));
      return (soft(o(t, f), 2) * 0.5 + lp(n()) * 1.5) * env(t, 0.42, 0.03, 0.08);
    }, 0.5);
  },
  "sounds/charge": () => {
    const o = osc(), o2 = osc();
    return render(1.0, (t) => ((o(t, 220 * 2 ** (t * 2.2))) * 0.7 + (o2(t, 440 * 2 ** (t * 2.2))) * 0.25) * env(t, 1.0, 0.05, 0.12) * (0.7 + 0.3 * Math.sin(t * 60)), 0.6);
  },
  "sounds/powerdown": () => {
    const o = osc();
    return render(0.8, (t) => soft(o(t, 900 * 2 ** (-t * 3.2)), 2) * env(t, 0.8, 0.01, 0.2), 0.6);
  },
  "sounds/scan": () => {
    const o = osc(), o2 = osc();
    return render(0.9, (t) => {
      const ping = Math.floor(t / 0.3);
      const u = t - ping * 0.3;
      return ((o(t, 1800 + ping * 240)) * Math.exp(-u * 18) + (o2(t, 600 + 900 * (t / 0.9))) * 0.18) * env(t, 0.9, 0.005, 0.05);
    }, 0.6);
  },
  "sounds/snore": () => {
    const o = osc();
    return render(1.4, (t) => {
      const breath = Math.sin(Math.PI * Math.min(1, t / 1.4)) ** 2;
      return soft(o(t, 180 + 40 * Math.sin(t * 4.5)), 1.5) * breath * (0.6 + 0.4 * Math.sin(t * 22));
    }, 0.4);
  },
};
