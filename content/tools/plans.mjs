// Default preview plans per kind/slot (panels for the preview sheet + the thumbnail shot).
// A bundle can override parts via `new Bundle({ …, preview: { … } })`.
const FRONT = { yaw: 32, pitch: 12 };
const BACK = { yaw: 212, pitch: 12 };

export function previewPlan(B, hints = {}) {
  const j = B.json;
  const anim = (name, t) => (j.animations?.[name] ? [{ name, t }] : []);
  const idleLen = j.animations?.idle?.length ?? 1;
  const tIdle = hints.t ?? idleLen * 0.3;
  const base = { player: true, time: tIdle, anims: anim("idle", tIdle) };
  let panels, thumb, sheet = {};

  if (j.kind === "emote") {
    const a = j.animations[j.emote.animation];
    const n = hints.frames ?? 8;
    const times = hints.times ?? [...Array(n).keys()].map((i) => (a.loop ? (i / n) * a.length : (i / (n - 1)) * a.length * 0.999));
    panels = times.map((t) => ({
      label: `t = ${t.toFixed(2)}s`, cam: hints.cam ?? FRONT, player: true, time: t, fitParticles: true,
      anims: [{ name: j.emote.animation, t }],
    }));
    const hero = hints.hero ?? a.length * 0.5;
    thumb = { cam: hints.cam ?? FRONT, player: true, time: hero, anims: [{ name: j.emote.animation, t: hero }], fitParticles: true };
    sheet = { cols: 4, panelW: 230, panelH: 270, sharedBounds: true, margin: 14 };
  } else {
    const slot = j.cosmetic.slot;
    switch (slot) {
      case "cape":
        panels = [
          { ...base, label: "back", cam: BACK },
          { ...base, label: "front", cam: FRONT },
          { label: "texture", texture: j.cosmetic.cape.texture, textureScale: 4 },
        ];
        thumb = { ...base, player: false, cam: { yaw: 200, pitch: 8 } };
        break;
      case "shield":
        panels = [
          { ...base, player: false, label: "front", cam: { yaw: 28, pitch: 10 }, shadow: false },
          { ...base, player: false, label: "back", cam: { yaw: 208, pitch: 10 }, shadow: false },
          { label: "texture", texture: j.cosmetic.shield.texture, textureScale: 4 },
        ];
        thumb = { ...base, player: false, cam: { yaw: 22, pitch: 8 } };
        break;
      case "wings": {
        const fl = (j.animations.fly?.length ?? 1) * 0.25;
        panels = [
          { ...base, label: "front · idle", cam: FRONT },
          { ...base, label: "back · idle", cam: BACK },
          { ...base, label: "back · fly", cam: { yaw: 160, pitch: 18 }, anims: anim("fly", fl), time: fl },
          { ...base, label: "top · idle", cam: { yaw: 180, pitch: 80 } },
        ];
        thumb = { ...base, player: false, cam: { yaw: 200, pitch: 14 } };
        break;
      }
      case "pet": {
        const off = hints.petOffset ?? [-20, 0, 6];
        const sc = j.cosmetic.pet?.scale ?? 1;
        const wl = (j.animations.walk?.length ?? 1) * 0.3;
        panels = [
          { ...base, label: "idle", cam: FRONT, worldOffset: off, petScale: sc },
          { ...base, label: "idle · close", ownFit: true, player: false, cam: { yaw: 35, pitch: 14 }, petScale: sc },
          { ...base, label: "walk", ownFit: true, player: false, cam: { yaw: 90, pitch: 10 }, anims: anim("walk", wl), time: wl, petScale: sc },
        ];
        thumb = { ...base, player: false, cam: { yaw: 28, pitch: 10 }, petScale: sc, maxScale: 60 };
        sheet = { maxScale: 14 };
        break;
      }
      case "aura":
        base.fitParticles = true;
        panels = [
          { ...base, label: "front", cam: FRONT },
          { ...base, label: "back", cam: BACK, time: tIdle + idleLen * 0.4, anims: anim("idle", tIdle + idleLen * 0.4) },
          { ...base, label: "top", cam: { yaw: 20, pitch: 60 } },
        ];
        thumb = { ...base, player: false, cam: { yaw: 30, pitch: 16 } };
        break;
      case "hat":
        panels = [
          { ...base, label: "front", cam: FRONT },
          { ...base, label: "back", cam: BACK },
          { ...base, label: "close", cam: { yaw: 35, pitch: 20 }, focusHead: true },
        ];
        thumb = { ...base, player: false, cam: { yaw: 32, pitch: 18 } };
        break;
      case "back":
      default:
        panels = [
          { ...base, label: "back", cam: BACK },
          { ...base, label: "front", cam: FRONT },
          { ...base, label: "side", cam: { yaw: 270, pitch: 10 } },
        ];
        thumb = { ...base, player: false, cam: { yaw: 210, pitch: 14 } };
    }
  }
  if (hints.panels) panels = hints.panels(panels, B);
  if (hints.thumb) thumb = { ...thumb, ...hints.thumb };
  return { panels, thumb, sheet: { ...sheet, ...(hints.sheet ?? {}) } };
}
