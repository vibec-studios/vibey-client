# Clips: proposal and contract draft (2026-11-19)

**Status: phases 1 and 2 built (Fabric 1.21.11), see §13.** This covers an optional module `clips` with a replay buffer ("save the last
30 s"), automatic clips in PvP (kill, multi-kill, clutch), an in-game gallery, and a file contract the Vibe Launcher
gallery is built against (§10). It is Fabric 1.21.11 first, then the other targets.

> See also: [VIDEO.md](VIDEO.md) / [VIDEO-RESEARCH.md](VIDEO-RESEARCH.md) (FFmpeg process, download prompt, playback)
> · [MEDIA.md](MEDIA.md) (WASAPI loopback helper) · [SERVER-RULES.md](SERVER-RULES.md) · [PVP.md](PVP.md) ·
> vibe-launcher `docs/PLAY-LINKS.md` §2 (instance files) and `docs/CONTRACT.md` (screenshots gallery)

## 0. Summary

| Topic | Recommendation |
|---|---|
| Capture | Vanilla's own async path, as used by `TracyFrameCapture`. After `blitToScreen`, a GPU blit of the main render target into a small fixed-size target (720p) and a GPU RGB→NV12 pass. Then `CommandEncoder.copyTextureToBuffer(..., callback)` (GL: PBO + fence, Vulkan: buffer copy + fence) and a map 1–3 frames later. Paced at 30/60 fps, independent of the game's fps. Works on the GL **and** Vulkan backends of 26.2/26.3 through the same API. 1.21.1–1.21.4: raw GL (`glBlitFramebuffer` + PBO + `glFenceSync`). |
| Encoder | **FFmpeg as a separate process** (the same LGPL BtbN download and the same consent prompt as video screens). Hardware first, in this order: `h264_nvenc` → `h264_amf` → `h264_qsv` → `h264_mf` (hardware MFT). Fallback: `h264_mf` software (Windows' own encoder), capped at 720p30. H.264 + AAC in MP4. |
| Replay buffer | Encoded 2 s GOP chunks of MPEG-TS in a **RAM ring** (default; 30 s at 720p30 is about 26 MB). A disk ring is optional for long buffers. Saving joins the last N s with `-c copy` (measured at 0.15–0.3 s) into a faststart MP4. Raw frames are ruled out: 3.3 GB per 30 s at 720p30 RGBA. |
| Resources | **Buffer off by default.** When on: 720p30 at 6 Mbit/s, hardware encoder. Recording stops in menus, the pause screen, unfocused or minimised windows, and at low FPS (hysteresis). FFmpeg exits after 60 s of pause. Hard caps on ring RAM, ring disk and the clips folder (2 GB default, oldest unkept clips deleted first). |
| Measured (§2.3) | Intel QSV at 720p30 with NV12 input: FFmpeg uses **~16 % of one core (≈1 % of the CPU)** and ~180 MB RSS. With RGBA input it uses 38 % of one core (FFmpeg does the colour conversion). Software at 1080p60: 1.5–2 cores. |
| FPS cost (estimate) | **≈1–3 % at 720p30 with a hardware encoder**, ≈3–6 % at 1080p60. To be measured in game with a built-in stats overlay (§1.4). |
| Audio | Phase 3. Windows **process loopback** (WASAPI, Win10 2004+) records only Minecraft's audio. It extends the existing PowerShell/C# loopback helper, and the PCM goes into the same FFmpeg via a localhost socket. Off by default. |
| Triggers | Client-side, from vanilla packets only. `ClientboundDamageEventPacket` (source = us) gives "we hit player P", and entity event 3 (death) for P within 8 s gives a kill. Translated death messages are a second signal. Multi-kill: 10 s window. Clutch: kill at ≤ 3 hearts or the last of ≥ 2 attackers. Post-roll is 3 s, events are merged, cooldown is 10 s. |
| Server rules | `clips` (no recording at all, ring discarded) and `clips.auto` (no automatic clips; the hotkey still works). The server address in metadata follows `discord.server` and the social "Hide server address" setting. |
| Contract | `<gameDir>/vibeyclient/clips/<id>.mp4 / .jpg / .json`. The JSON is written last (temp + rename) and marks the clip as complete. `kept` protects a clip from cleanup. The launcher may edit `kept`/`title`/`tags`. Deletion order: JSON first. |

## 1. Capture without noticeable FPS loss

### 1.1 What the versions use (checked in the decompiled jars)

| Targets | Render API | Readback primitive |
|---|---|---|
| 1.21.1–1.21.4 | `RenderTarget` with raw GL ids (`RenderSystem`, `GlStateManager`) | our own GL: `glBlitFramebuffer` into a small FBO, `glReadPixels` into a `GL_PIXEL_PACK_BUFFER`, `glFenceSync`, map on a later frame (all in GL 3.2 core, which MC requires) |
| 1.21.5–1.21.11, 26.1 | blaze3d `GpuDevice` / `CommandEncoder`, OpenGL only | `createRenderPass` (blit pipeline), `copyTextureToBuffer(tex, buf, 0, callback, mip)`, `mapBuffer(buf, read, write)` |
| 26.2 | blaze3d, **OpenGL + Vulkan** (`com.mojang.blaze3d.vulkan`) | same API, both backends |
| 26.3 | **renderpearl** (`com.mojang.renderpearl.api.*`, backends `opengl` + `vulkan`; `PreferredGraphicsApi`) | the same calls, renamed: `GpuFormat.RGBA8_UNORM`, `RenderSystem.getCompiledPipeline(...)`, `renderPass.setUniform("InSampler", ...)`, `draw(3, 1, 0, 0)`, `buffer.map(read, write)` |

Vanilla already ships this exact pipeline. `com.mojang.blaze3d.TracyFrameCapture` (used when the Tracy profiler is
attached) blits the main target into a 320×180 texture with `RenderPipelines.TRACY_BLIT` and calls
`copyTextureToBuffer` with a callback. It maps the buffer only once the callback has fired. On GL,
`GlCommandEncoder.copyTextureToBuffer` binds the buffer as `GL_PIXEL_PACK_BUFFER` (35051), calls `glReadPixels` and
`RenderSystem.queueFencedTask(callback)`. So this is a real asynchronous PBO + fence readback, and the 26.2/26.3
Vulkan backends implement the same contract. `Minecraft.runTick` calls it right after `renderTarget.blitToScreen()`
and before `window.updateDisplay`, which is our hook point too: a mixin or event at the same place. The frame then
includes the HUD and chat, exactly what the player sees.

### 1.2 Pipeline per captured frame

```
render thread (only when a capture is due)                      writer thread              FFmpeg process
─────────────────────────────────────────                       ─────────────              ──────────────
1. blit main target → 1280×720 (letterbox, LINEAR)    ~0.05 ms GPU
2. RGB→NV12 pass: Y → R8 1280×720, UV → RG8 640×360   ~0.05 ms GPU
3. copyTextureToBuffer ×2 into staging buffer k (of 3)  async DMA
4. (frame k+1..k+3) callback fired → map, hand slice ──────────► copy to byte[] ─► stdin pipe (rawvideo nv12)
   to the writer, unmap after it is done (next frame)            CFR pacing, dup/drop  → h264_* → MPEG-TS stdout
                                                                                       ─► reader thread: GOP chunks → RAM ring
```

- **Downscale before readback.** Capture runs at the clip size, never at the window size: 1440p is 14.7 MB per
  frame in RGBA, while 720p NV12 is 1.4 MB.
- **Colour conversion on the GPU.** NV12 is 1.5 B/px instead of 4, so the readback is 2.7× smaller and FFmpeg skips
  `swscale`. In the benchmark (§2.3) that cut FFmpeg's CPU at 720p30 from 38 % to 16 % of a core, and at 1080p60
  from 108 % to 35 %. The pass is a fullscreen-triangle shader with BT.709 limited-range coefficients and the
  colour range tagged on the encoder (`-color_range tv -colorspace bt709`). Fallback where a custom pipeline is a
  problem: RGBA readback, and FFmpeg converts.
- **Fixed output size.** The clip is always 16:9 (e.g. 1280×720), and other window aspects are letterboxed in the
  blit. Resizing the window therefore never restarts the encoder.
- **Frame pacing.** `nextCapture += 1/fps` on a monotonic clock, and a frame is captured only when it is due. At
  144 game fps and a 30 fps clip, 1 frame in ~5 does any work. If the game runs below the clip fps, the writer
  repeats the last frame so FFmpeg gets constant frame rate input, but the low-FPS pause (§7) normally kicks in
  first.
- **Threading.** Nothing blocks the render thread. If no staging buffer is free (the encoder fell behind), the
  frame is skipped and counted. Pipe writes, chunking and saving run on two daemon threads plus the `Tasks` pool.
  The writer copies straight from the mapped memory, and the render thread only unmaps it on a later frame. Keep
  the copy on the render thread (~0.2 ms) as a fallback if a backend objects.

### 1.3 Cost estimate per captured frame (to be confirmed in game)

| | 720p30 NV12 | 720p30 RGBA | 1080p60 NV12 | 1080p60 RGBA |
|---|---|---|---|---|
| bytes per frame | 1.38 MB | 3.69 MB | 3.11 MB | 8.29 MB |
| readback bandwidth | 41 MB/s | 111 MB/s | 187 MB/s | 498 MB/s |
| GPU blit + NV12 pass | ~0.1 ms | ~0.05 ms | ~0.15 ms | ~0.1 ms |
| DMA copy (async, PCIe 3.0 x16 ≈ 12 GB/s) | ~0.1 ms | ~0.3 ms | ~0.25 ms | ~0.7 ms |
| render thread: enqueue + map/unmap (copy on writer) | ~0.05 ms | ~0.05 ms | ~0.05 ms | ~0.05 ms |
| render thread if the copy has to stay there (~5–10 GB/s memcpy) | +0.15–0.3 ms | +0.4–0.8 ms | +0.3–0.6 ms | +0.8–1.7 ms |
| average per game frame at 144 fps | ≈0.01–0.08 ms | ≈0.02–0.17 ms | ≈0.03–0.27 ms | ≈0.07–0.75 ms |

Add the background load: the FFmpeg process plus our writer, 0.2–0.4 cores at 720p30 with hardware encoding (§2.3).
Hardware encoders run on the GPU's separate media engine (NVENC / QSV / VCN), not on the shader cores.
**Expected FPS loss: ≈1–3 % at 720p30 hardware, ≈3–6 % at 1080p60 hardware.** Software encoding at 1080p60 takes
1.5–2 cores, which is visible on CPU-bound PCs, so software is capped at 720p30. The figures are estimates from
the sizes and bandwidths above. Async readback is the established technique: OpenGL Insights ch. 28, the songho PBO
article, and OBS' staging surfaces (sources in §12). Mojang runs the same path every frame under Tracy.

### 1.4 How to measure in game later

`-Dvibeyclient.clips.stats=true` shows an overlay with: GPU time of the blit and NV12 passes (timer queries on GL,
where available), render-thread ms per capture, map latency in frames, skipped frames, writer queue depth, FFmpeg CPU
(`ProcessHandle.info().totalCpuDuration()` deltas) and RSS. A `ClipsGameTest` records 10 s at fixed scenes with the
buffer on and off and logs average and p99 frame time. This is not part of the research round (no Minecraft or
Gradle runs on this PC right now).

## 2. Encoder

### 2.1 Options

| Option | Licence fit (Apache-2.0 mod) | Verdict |
|---|---|---|
| **FFmpeg CLI, separate process**: BtbN `lgpl-shared` (Windows), the system package (Linux/macOS) | LGPL, never linked or bundled, downloaded only after consent and checked against `checksums.sha256`. This is already done for video screens (`player/Ffmpeg`). | **chosen**: one code path for encoding, segmenting, remuxing, thumbnails, trimming and playback |
| Hardware encoders through that FFmpeg | BtbN's build scripts enable `ffnvcodec` (NVENC headers, MIT), `amf` (MIT headers), `onevpl` (QSV via Intel libvpl, MIT; x64 only) and `openh264` in **every** variant including LGPL. They leave out only `x264`/`x265` (`[[ $VARIANT == lgpl* ]] && return -1`). Runtimes come with the GPU driver. | **chosen, preferred** |
| `h264_mf` (Windows Media Foundation through FFmpeg) | Windows' own encoder MFTs: a vendor hardware MFT or Microsoft's software H.264 encoder. Missing on Windows "N" editions without the Media Feature Pack. | fallback |
| `libopenh264` (in the BtbN LGPL build) | BSD-2 source, but Cisco's patent coverage applies only to **Cisco's binaries**, and BtbN builds it from source | last resort, with a note |
| libx264 | GPL, not in the LGPL build. If the user's own FFmpeg (e.g. a distro package) has it, invoking it as a separate program is fine. | Linux/macOS software fallback only |
| jcodec (pure Java, BSD) | fine | rejected: baseline-like H.264 encoder only, far below real time at 720p30, would run in the game JVM |
| Own Media Foundation helper (C# via PowerShell Add-Type, like the loopback helper) | fine (OS component) | later option for an "FFmpeg-free" path; would re-implement segmenting/muxing. Not for the MVP. |
| JavaCV / bytedeco (JNI FFmpeg) | Apache-2.0 + LGPL, 25–40 MB per platform | rejected: native code inside the game, big jars (same reasoning as VIDEO-RESEARCH §2) |

### 2.2 Encoder selection and settings

- **Probe** once per (GPU name, driver version, FFmpeg version) and cache the result in `_data.encoder`: a 2-frame
  test encode at the target size, `-f lavfi -i color=black:s=1280x720:r=30 -frames:v 2 -c:v <enc> -f null -`
  (~0.3 s each). Order on Windows: `h264_nvenc`, `h264_amf`, `h264_qsv`, `h264_mf -hw_encoding 1`, then software
  (`h264_mf -hw_encoding 0`, `libx264` if present, `libopenh264`). The settings page shows the chosen encoder
  ("Hardware: Intel Quick Sync") and offers *Auto / pick one*.
- **Stream layout.** H.264 High (Main for openh264), **no B-frames**, closed GOP, keyframe exactly every 2 s:
  `-g <2·fps> -force_key_frames expr:gte(t,n_forced*2)` plus the vendor's forced-IDR switch (`-forced_idr 1` for
  QSV, `-forced-idr 1` for NVENC). In the benchmark, QSV with only `-g 60` produced a 4.07 s first segment. Rate
  control is VBR with `-maxrate` at 1.5× the bitrate and a 2 s buffer. Low-latency presets: NVENC `p2 -tune ll`,
  QSV `veryfast -look_ahead 0`, AMF `-quality speed`.
- **Being nice to the game.** FFmpeg runs at `BELOW_NORMAL` priority: a one-shot `SetPriorityClass` through the
  existing PowerShell helper path on Windows, `nice -n 10` on Linux/macOS. Software encoders get `-threads 2`.
  stdin/stdout/stderr pipes are drained on daemon threads, and the process is killed on close and on JVM exit (as
  `FfmpegPlayer` does).
- **Bitrate presets** (H.264, Minecraft content): Low / Medium / High at 720p30 = 3 / **6** / 10 Mbit/s, at 1080p60
  = 8 / 12 / 20 Mbit/s. Custom values are allowed within 1–40 Mbit/s.
- NVIDIA limits concurrent NVENC sessions on GeForce cards. If OBS/ShadowPlay already hold all of them, the probe or
  start fails, and we fall back one step with a toast.

### 2.3 Measured: FFmpeg encode via a pipe (this PC)

`tools/clips-bench.py` pipes synthetic frames in real time into FFmpeg (rawvideo on stdin, the way the game would).
The frames are a moving gradient plus a strip of per-frame noise, which is harder to encode than typical gameplay.
FFmpeg writes 2 s MPEG-TS ring segments, and the last 3 are joined with `-c copy`. 6 s per run.
**Machine:** i5-12600K (16 logical), Intel Arc A750, 32 GB RAM, Gyan FFmpeg 9.0.2 "full" build. **Under load:**
other agents were building at the same time, and the Python writer missed some frame deadlines at 1080p60, so treat
the numbers as upper bounds. CPU = FFmpeg utime+stime over wall time.

| Encoder (input) | 720p30 @ 6 Mbit/s: % of 1 core / % of CPU / RSS | 1080p60 @ 12 Mbit/s: % of 1 core / % of CPU / RSS |
|---|---|---|
| **h264_qsv (NV12 in)**: GPU-side conversion, recommended | **15.5 % / 1.0 % / 180 MB** | **34.9 % / 2.2 % / 234 MB** |
| h264_qsv (RGBA in, FFmpeg converts) | 38.0 % / 2.4 % / 193 MB | 107.5 % / 6.7 % / 222 MB |
| h264_mf hardware (RGBA in) | 30.3 % / 1.9 % / 225 MB | 115.4 % / 7.2 % / 405 MB |
| h264_mf software (RGBA in) | 48.8 % / 3.0 % / 386 MB | 184.8 % / 11.6 % / 754 MB |
| libx264 veryfast, reference only (GPL), RGBA in | 68.9 % / 4.3 % / 106 MB | 153.3 % / 9.6 % / 327 MB |
| libx264 veryfast, reference only, NV12 in | 45.9 % / 2.9 % / 106 MB | 197.7 % / 12.4 % / 250 MB |

- **Pipe writer** (stands in for our Java writer): RGBA 3 % of a core at 720p30 and 15 % at 1080p60. NV12: 1.5 % and
  3.5 %.
- **Join** of the last 3 segments to a faststart MP4 with `-c copy`: 0.15–0.30 s. All joins were valid
  (ffprobe: exact frame counts, 6.000 s).
- **The FFmpeg process dominates RAM:** ~100–250 MB RSS with hardware encoders (the QSV/MF runtimes are big), up to
  750 MB with MF software at 1080p60. That is why software is capped at 720p30, and why FFmpeg is stopped after 60 s
  of pause. RSS of the BtbN LGPL shared build is expected in the same range and will be measured once it is wired up.
- Not measured here: NVENC and AMF (no such GPU), and the in-game readback (§1.3).

## 3. Replay buffer

### 3.1 Why encoded chunks (numbers)

| 30 s of … | 720p30 | 1080p60 |
|---|---|---|
| raw RGBA | 3.32 GB (111 MB/s) | 14.9 GB (498 MB/s) |
| raw NV12 | 1.24 GB (41 MB/s) | 5.6 GB (187 MB/s) |
| **H.264 Medium** (6 / 12 Mbit/s) + AAC 128 kbit/s | **≈23 MB** | **≈46 MB** |
| H.264 High (10 / 20 Mbit/s) + AAC | ≈38 MB | ≈76 MB |

Raw frames are out of the question. The ring keeps **2 s GOP chunks** plus one chunk of headroom, so 30 s means
about 32–34 s held. Medium: ~26 MB at 720p30 and ~52 MB at 1080p60.

### 3.2 Design

- **RAM ring (default).** FFmpeg writes one continuous MPEG-TS stream to stdout (`-f mpegts pipe:1`). A reader thread
  cuts it into chunks at TS packets that start a video PES with `random_access_indicator = 1` (FFmpeg sets it on
  keyframes). It caches PAT/PMT and stores chunks in pooled `byte[]` blocks. Oldest chunks are dropped while
  *duration > length + 2 s* or *bytes > RAM cap*. This is ~200 lines of pure Java and JUnit-testable with a recorded
  `.ts`. It writes nothing to disk while playing.
- **Disk ring (option, for buffers > 2 min or above the RAM cap).** FFmpeg's own segment muxer, as in the benchmark:
  `-f segment -segment_time 2 -segment_format mpegts -segment_wrap <⌈len/2⌉+3> -segment_list list.csv` into
  `<gameDir>/vibeyclient/clips/.buffer/`, deleted when the buffer stops. It writes ~2.7 GB/h at 6 Mbit/s, which is
  why it is not the default.
- **Saving** never happens on the render thread. Hotkey or trigger → wait for the post-roll (auto clips) → take the
  chunks covering *[now − N s, now]*, starting at a keyframe, so the clip is N to N+2 s long. Write them as
  `.ts` parts into `.tmp/`, one file per FFmpeg session (see pause below; the concat demuxer re-bases timestamps per
  file). Then run `ffmpeg -f concat -i list.txt -c copy -movflags +faststart .<id>.mp4.part` (~0.2 s), make the
  thumbnail (`-ss <highlight> -frames:v 1 -vf scale=480:-2 -q:v 4`, ~0.1 s), write the JSON and rename (§10.3).
  The ring keeps recording meanwhile.
- **Pauses** (menus, unfocused, low FPS, §7): no frames and **no audio samples** are fed (both are cut at the same
  wall-clock instant), so the clip simply has a jump cut and audio stays in sync. FFmpeg stays alive for 60 s of
  pause, then exits to free its ~200 MB. The ring keeps its chunks, and the next session starts a new chunk group.
- **Caps.** The RAM ring is 96 MB by default (range 32–256). The disk ring is 512 MB by default. If *length ×
  bitrate* exceeds a cap, the settings page shows the real length ("30 s → 26 s at this quality").

## 4. Audio (optional, Phase 3)

| Option | Verdict |
|---|---|
| OpenAL Soft loopback device (`ALC_SOFT_loopback`) | Minecraft's sound engine would have to render into our device instead of the real one: an invasive, version-specific mixin in `SoundEngine`/`Library`, and we would also have to play the result. Rejected. |
| Hooking Minecraft sound calls | OpenAL mixes internally, so there are no samples to tap. Rejected. |
| **WASAPI process loopback** (Windows 10 2004+, `AUDIOCLIENT_ACTIVATION_TYPE_PROCESS_LOOPBACK` with our PID, include tree) | **chosen for Windows.** Only the game's own sound (no Discord or Spotify) and no driver needed. It extends the media module's helper (`assets/vibeyclient/lyrics/loopback/windows.ps1`, C# compiled by Add-Type) with a `-pid` mode at 48 kHz stereo s16le. The helper stays shared, and the music-analysis use keeps the device-loopback mode. |
| WASAPI device loopback (what the helper does today) | fallback on older Windows. It records **everything** the PC plays (voice chat!), so it needs an explicit, warned opt-in. |
| Linux: PulseAudio/PipeWire monitor (`ffmpeg -f pulse -i <sink>.monitor`) | system mix only, same warning. Per-app capture needs a PipeWire node link: later. |
| macOS: ScreenCaptureKit app audio (13+) | needs a signed helper and the Screen Recording permission. Not planned. |

**Sync:** the helper streams PCM to Java. Java pads silence by wall clock (loopback delivers nothing while the
game is silent, see `LoopbackCapture.fresh()`) and forwards it to the **same FFmpeg** as a second input over
`tcp://127.0.0.1:<random port>`. FFmpeg's built-in `aac` encoder (LGPL) produces 128 kbit/s. Both inputs start and
pause at the same instant, and an *Audio offset* setting (±200 ms, default from the measured helper latency) covers
device latency. Sound is off by default.

## 5. Triggers (automatic clips)

Everything comes from what the vanilla client already receives. Nothing is sent and nothing is shown during play
except the "Clip saved" toast. Auto clips need the buffer to be running.

| Signal | Source (1.21.11) | Used for |
|---|---|---|
| We damaged entity X | `ClientboundDamageEventPacket(entityId, type, sourceCauseId, sourceDirectId, pos)`, broadcast to every tracking client since 1.19.4: `sourceCauseId == our id`, which covers bows, tridents and pets. Plus our own melee from `MultiPlayerGameMode.attack` as a backup. | "last hit" table: player → time |
| X died | entity event **3** (`LivingEntity.die` → `broadcastEntityEvent(this, 3)`), or a translated death message (`death.attack.*` with the killer argument = us) | kill confirmation |
| Our death | death screen | optional `death` trigger (off by default) |

- **kill:** a player we damaged within the last **8 s** dies. Entity event and death message are deduplicated per
  victim within 2 s. Health from entity data is deliberately not used: servers may hide or spoof it.
- **multikill:** ≥ 2 kills within **10 s** make one clip, `reason: multikill`, `kills: n`.
- **clutch:** a kill while our health ≤ **6 HP**, or the kill of the last of ≥ 2 players who damaged us in the last
  15 s.
- **Timing:** save **3 s after** the last event (post-roll, 0–10 s). Events during the post-roll extend it and end
  up in one clip with all `events[]`. Cooldown is 10 s, and there are at most 20 auto clips per session (setting) to
  protect the disk.
- Custom death messages (plain text) on some servers make the entity event the main signal. Servers that don't track
  us (out of range) produce no clip, which is fine.
- **Server rules** (to be added to SERVER-RULES.md once built): `clips` switches off recording entirely (ring
  discarded, hotkey shows "Clips are off on this server"). `clips.auto` switches off automatic clips only. Declared
  with `ServerRules.supports` in `onLoad`. A `ServerRules.listen` listener stops the buffer immediately and drops its
  RAM.

## 6. Platforms

- **Windows (first):** the FFmpeg download prompt is reused (BtbN `lgpl-shared`, x64/arm64; QSV not on arm64). All
  hardware encoders and the WASAPI process loopback are available.
- **Linux:** the system FFmpeg (the prompt explains the package manager). Encoders: `h264_nvenc` (if the distro
  build has it), `h264_vaapi` (Intel/AMD, needs `-vaapi_device` and `hwupload`), then `libx264`/`libopenh264`.
  Audio via the Pulse monitor (warned). Wayland/X11 don't matter, because we read our own framebuffer.
- **macOS:** Homebrew FFmpeg with `h264_videotoolbox` (hardware, built in). GL 4.1 has fences, so the 1.21.1–1.21.4
  path works. No audio at first.

## 7. Settings (module `clips`, `config/vibeyclient/clips.json`)

| Card | Setting (default) |
|---|---|
| Replay buffer | **Replay buffer** (off) · Save clip key (**F8**) · Length 10–120 s (**30**) · *Status line: "Recording · Hardware: Intel QSV · 26 MB RAM"* |
| Quality | Resolution 480p / **720p** / 1080p (1080p only with hardware) · Frame rate **30** / 60 · Quality Low / **Medium** / High / Custom Mbit/s · Encoder **Auto** / list |
| Automatic clips | Automatic clips (off until the buffer is on, then **on**) · Kill (**on**) · Multi-kill (**on**) · Clutch (**on**) · Own death (off) · Post-roll (**3 s**) · Max per session (**20**) |
| Saving resources | Pause when FPS are low (**on**) · FPS threshold (**auto** = clip fps + 10, i.e. 40 at 30 fps, 70 at 60 fps; 20–120) · Record in inventories/containers (off) · Buffer in RAM / on disk (**RAM**) · RAM cap (**96 MB**) · Disk cap (**512 MB**) |
| Sound | Record game sound (off) · Only Minecraft (**on**; off = whole PC, warned) · Offset (auto) |
| Storage | Clips folder limit (**2 GB**, 0.5–50 GB) · Delete oldest automatically (**on**, never kept clips) · Save server address (**on**, but always hidden when the server rule or "Hide server address" says so) · *Open folder* · *Open gallery* |
| HUD | Recording indicator `clips.indicator` (small dot, **on**; amber when paused, with the reason in the tooltip) |

**When recording runs** (all must hold): module on, buffer on, a world loaded, rule `clips` not set,
`mc.screen == null` or `ChatScreen` (inventories/containers only with the setting), window focused and not minimised,
and not paused for low FPS. **Low-FPS pause:** a 3 s rolling average of the real frame rate, measured only while
recording. It pauses when the average is **< threshold for 3 s** and resumes when it is **≥ threshold + 10 for 5 s**.
It also pauses when > 10 % of frames are skipped for lack of a free staging buffer over 5 s (encoder behind). The
first pause per session shows a toast ("Clips paused: low FPS"), later ones only the HUD dot.

## 8. UI

- **Toast** on save: "Clip saved · Kill · 0:31" with *View* and *Keep* (`ToastStack.show(toast, buttons, …)`).
  Failure: "Couldn't save the clip" with the reason.
- **Gallery** (Vibey menu → Clips → *Open gallery*, and an unbound "Clips gallery" key): a grid of thumbnails
  (JPEG → `NativeImage`, loaded lazily for visible tiles only), filter *All / Kept / Auto / Hotkey*, newest first,
  and a storage bar "1.2 of 2 GB".
- **Player:** reuses `module/media/video` `FfmpegPlayer` + `FrameTexture` (local files are already supported),
  drawn into a GUI rect, with a non-positional OpenAL source. Controls: play/pause, seek bar, volume, *Keep* (star),
  *Rename*, *Trim* (in/out marks → `-ss/-to -c copy`, snapped to the 2 s keyframes; exact re-encode later), *Delete*
  (confirm), *Open folder*, *Copy path*. The video feature is Fabric 1.21.11 only. Until it is ported, other targets
  get the gallery with *Open in system player*.

## 9. Privacy and fairness

Clips stay on the PC, and nothing is uploaded. Chat and HUD are part of the picture, which the first-time hint says
("Clips show your screen including chat"). The server address follows §10.4. Other players' **names** appear only
in `events[]`, never their UUIDs. No information beyond vanilla is used or shown, and automatic clips are passive.

## 10. Contract for the Vibe Launcher (draft, schema 1)

### 10.1 Location and files

- Folder: **`<gameDir>/vibeyclient/clips/`** (launcher: `instances/<id>/minecraft/vibeyclient/clips/`). The game
  creates it on the first save.
- One clip = three files with the same `<id>`: **`<id>.mp4`** (H.264 + optional AAC, `+faststart`), **`<id>.jpg`**
  (thumbnail, max 480 px wide), **`<id>.json`** (metadata, below).
- `<id>` = local time + reason: `YYYY-MM-DD_HH-mm-ss_<reason>`, with `_2`, `_3` … on a collision. Regex:
  `^\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}_[a-z]+(_\d+)?$`.
- The launcher **ignores** everything else: dotfiles (`.<id>.mp4.part`, `.<id>.json.tmp`), `.buffer/`, `.tmp/`,
  and files without a JSON.

### 10.2 `<id>.json`

```json
{
  "schema": 1,
  "id": "2026-11-19_21-04-33_multikill",
  "created_at": "2026-11-19T20:04:33Z",
  "video": "2026-11-19_21-04-33_multikill.mp4",
  "thumbnail": "2026-11-19_21-04-33_multikill.jpg",
  "duration_ms": 31967,
  "size_bytes": 24093112,
  "width": 1280, "height": 720, "fps": 30,
  "video_codec": "h264", "audio_codec": "aac",
  "reason": "multikill",
  "kills": 2,
  "highlight_ms": 26100,
  "events": [ { "t_ms": 21400, "type": "kill", "victim": "Steve" }, { "t_ms": 26100, "type": "kill", "victim": "Kai" } ],
  "player": { "name": "Alex", "uuid": "1c2d3e4f-…" },
  "mode": "multiplayer",
  "server": "play.example.net",
  "minecraft": "1.21.11", "loader": "fabric", "client": "vibey-client", "client_version": "0.4.0",
  "encoder": "h264_qsv",
  "kept": false,
  "title": "Bridge 1v2",
  "tags": ["bedwars"]
}
```

| Field | Rule |
|---|---|
| `schema` | `1`. Readers skip clips with a higher major schema they don't know. Unknown fields are ignored **and preserved** when rewriting. |
| `id`, `video`, `thumbnail` | file names in the same folder, no paths. `thumbnail` may be missing (generation failed): the launcher makes its own. |
| `created_at` | ISO-8601 UTC (`Z`), the time of the save (end of the clip) |
| `duration_ms`, `size_bytes`, `width`, `height`, `fps` | of the MP4 as written |
| `video_codec` | `h264` in schema 1. `audio_codec` is `aac`, or missing without sound. |
| `reason` | `hotkey`, `kill`, `multikill`, `clutch`, `death`. Readers show unknown values as "Clip". |
| `kills`, `highlight_ms`, `events[]` | auto clips. `t_ms` / `highlight_ms` count from the start of the clip, and `type` ∈ `kill`, `death`. Victim = player name, max 16 events. |
| `player` | the local player (name, dashed UUID) |
| `mode` | `singleplayer`, `hosting`, `multiplayer` (same words as `session.json`) |
| `server` | only for `multiplayer`. **Missing** when the server rule `discord.server` was active, when the social setting "Hide server address" or the clips setting "Save server address" is off. |
| `world` | only for `singleplayer` / `hosting` (world name, max 100) |
| `minecraft`, `loader`, `client`, `client_version`, `encoder` | informational |
| `kept` | `true` = never deleted by the automatic cleanup. Default `false`. |
| `title` (≤ 100 chars), `tags` (≤ 16 × ≤ 32 chars) | user data, optional |

As in `session.json`: fields that don't apply are **left out, not `null`**.

### 10.3 Writes, detection, edits, deletion

- **The game writes in this order:** `.<id>.mp4.part` → rename to `<id>.mp4`, then `<id>.jpg` (via `.part`), and
  **`<id>.json` last** (`.<id>.json.tmp` + atomic rename, `ATOMIC_MOVE` with a `REPLACE_EXISTING` fallback, like
  `SessionFile`). **A clip exists exactly when `<id>.json` exists and parses, and its `video` file exists.**
- **Detecting new clips:** watch the folder (e.g. Rust `notify`) for created or renamed `*.json` and debounce
  ~500 ms, or rescan when the gallery opens. Sort by `created_at`. A JSON that fails to parse is retried once after
  200 ms, then skipped (never deleted).
- **The launcher may edit** `kept`, `title` and `tags` only: read the JSON, change those fields, keep everything
  else, then write `.<id>.json.tmp` and rename it over the original. The game re-reads JSONs when the gallery opens
  and before cleanup. On a concurrent edit the last writer wins, which is acceptable for these fields.
- **Deletion (game or launcher, on user request):** delete **`<id>.json` first** (the clip disappears atomically
  for every reader), then `<id>.mp4` and `<id>.jpg`. A file locked by a player is retried later. The game removes
  orphans at start: an `.mp4`/`.jpg` without a JSON, or a `.part`/`.tmp` older than 1 h.
- **Automatic cleanup is done only by the game.** It runs after each save and at module start. While the total size
  of all clip files is above the limit, it deletes the oldest clip (by `created_at`) with `kept != true`. If kept
  clips alone exceed the limit, it deletes nothing more and shows a notice on the settings page. The launcher never
  deletes on its own. It may show `size_bytes` totals and a "Keep" star.

### 10.4 Privacy rules the launcher keeps

Clips are local files. The launcher must not upload them or their JSON without an explicit user action, and must
respect a missing `server` (it must not fill it in from `session.json`).

## 11. Effort and phases

| Phase | Scope | Estimate |
|---|---|---|
| **1 – MVP** (Fabric 1.21.11) | module `clips` (1.21.11-only wiring like `pvp`), capture hook + blit + NV12 pass + async readback, pacing, writer, FFmpeg session (probe, hardware order, priority), RAM ring + TS chunker (JUnit), save/remux/thumbnail/JSON (atomic), cleanup + `kept`, hotkey + toast, pause rules (screens, focus, low FPS with hysteresis), settings page, HUD dot, gallery with `FfmpegPlayer` (keep/rename/delete/open folder), stats overlay, `ClipsGameTest` (FPS with/without), docs + contract final | **6–8 days** |
| 2 – Auto clips | damage/death tracking, multi-kill/clutch, post-roll merge, cooldown, rules `clips` / `clips.auto`, a PvP gametest with a dummy player (DevDriver) | 2–3 days |
| 3 – Audio | process-loopback mode in the helper, TCP input, sync/offset, device-loopback opt-in, Linux Pulse | 3–4 days |
| 4 – Other versions | 1.21.5–1.21.10 + 26.1 (blaze3d, small diffs), 26.2/26.3 (Vulkan + renderpearl; test both backends), 1.21.1–1.21.4 (own GL path), NeoForge | 4–6 days |
| 5 – Extras | disk ring, exact trim (re-encode), "Include HUD" off (capture before the GUI pass), HEVC/AV1 hardware option | 2–4 days |
| Launcher | gallery against §10 (Tauri/WebView2 plays H.264/AAC MP4) | launcher team |

**Risks:** hardware encoder or driver quirks (keyframe alignment seen with QSV; vendor-specific flags; probe +
fallback chain); the Vulkan backends (26.2/26.3): mapping and callback timing must be verified, although vanilla
uses the same path; mods that replace the main target or present differently (Iris/Sodium: test). There is an
80 MB FFmpeg download, which is already accepted for video. FFmpeg RSS (~200 MB) is mitigated by stopping it in
pauses. GPU TDR flakiness on the Intel Arc test PC affects long gametests. Driver session limits (NVENC). Windows
"N" editions lack MF.

## 12. Sources

- Vanilla 1.21.11 / 26.3 (decompiled with Vineflower 1.10.1): `com.mojang.blaze3d.TracyFrameCapture`,
  `com.mojang.blaze3d.opengl.GlCommandEncoder.copyTextureToBuffer` (PBO 35051 + `queueFencedTask`),
  `Minecraft.runTick` (capture after `blitToScreen`), `ClientboundDamageEventPacket`, `LivingEntity.die` (event 3);
  26.2 `com.mojang.blaze3d.vulkan`, 26.3 `com.mojang.renderpearl.backend.{opengl,vulkan}`.
- Asynchronous readback: [OpenGL Insights ch. 28 "Asynchronous Buffer Transfers"](https://www.seas.upenn.edu/~pcozzi/OpenGLInsights/OpenGLInsights-AsynchronousBufferTransfers.pdf),
  [songho: OpenGL Pixel Buffer Object](http://www.songho.ca/opengl/gl_pbo.html),
  [Vulkan `vkCmdCopyImageToBuffer`](https://registry.khronos.org/vulkan/specs/latest/man/html/vkCmdCopyImageToBuffer.html),
  [OBS Studio (staging surfaces in libobs)](https://github.com/obsproject/obs-studio).
- FFmpeg: [segment muxer](https://ffmpeg.org/ffmpeg-formats.html#segment), [concat demuxer](https://ffmpeg.org/ffmpeg-formats.html#concat-1),
  [codecs (h264_mf, nvenc, qsv, amf)](https://ffmpeg.org/ffmpeg-codecs.html), [licence](https://ffmpeg.org/legal.html).
- Builds: [BtbN/FFmpeg-Builds](https://github.com/BtbN/FFmpeg-Builds) (README variants; `scripts.d/50-x264.sh`
  disabled for `lgpl*`, `50-ffnvcodec.sh`, `50-amf.sh`, `50-onevpl.sh`, `50-openh264.sh` enabled).
- Hardware SDK headers: [nv-codec-headers](https://github.com/FFmpeg/nv-codec-headers) (MIT),
  [AMD AMF](https://github.com/GPUOpen-LibrariesAndSDKs/AMF) (MIT), [Intel libvpl](https://github.com/intel/libvpl) (MIT);
  [NVENC support matrix](https://developer.nvidia.com/video-encode-and-decode-gpu-support-matrix-new).
- [OpenH264 binary licence (Cisco patent coverage)](https://www.openh264.org/BINARY_LICENSE.txt), [jcodec](https://github.com/jcodec/jcodec).
- WASAPI process loopback: [`AUDIOCLIENT_PROCESS_LOOPBACK_PARAMS`](https://learn.microsoft.com/en-us/windows/win32/api/audioclientactivationparams/ns-audioclientactivationparams-audioclient_process_loopback_params),
  [ApplicationLoopback sample](https://learn.microsoft.com/en-us/samples/microsoft/windows-classic-samples/applicationloopbackaudio-sample/).
- Repo: `module/media/video/player/{Ffmpeg,FfmpegPlayer,FrameTexture}`, `module/media/lyrics/fx/LoopbackCapture`,
  `launcher/{SessionFile,LauncherStatus}`, `api/ServerRules`; benchmark `tools/clips-bench.py`.

## 13. What is built (MVP + auto clips, Fabric 1.21.11)

Package `module/clips` (1.21.11-only wiring like `pvp`: gradle/modules.gradle leaves package, mixin config, strings and
the shader out of every other target; `BuiltinModules` registers it by reflection).

| Part | Where | Notes |
|---|---|---|
| Capture | `capture/FrameGrabber`, `mixin/MinecraftMixin`, `assets/vibeyclient/shaders/core/clips_nv12.fsh` | One fullscreen pass scales + letterboxes the main target and converts to NV12 (BT.709 limited) into an RGBA8 target of W/4 × 1.5·H, so its bytes **are** the NV12 frame (no RG8 format in 1.21.11). Three readback buffers, `copyTextureToBuffer` + fenced callback, paced at the clip fps. |
| Readback | `FrameGrabber.Readback` | **Finding:** blaze3d persistently maps `MAP_READ` buffers; on this PC (Intel Arc, dedicated) the mapping is uncached even with `USAGE_HINT_CLIENT_STORAGE`, so copying 1.38 MB takes 4–6 ms. Measured per capture on the render thread: MAP 4.4 ms, `glGetBufferSubData` 2.8 ms, **ASYNC 0.013 ms** (map on the render thread, copy on the `Vibey clips copy` thread, ~4 ms there, unmap a frame later). ASYNC is the default (`-Dvibeyclient.clips.readback=MAP|GET|ASYNC`). Also tried: a GPU copy into an own `GL_STREAM_READ` buffer, read on the render thread: 4–5 ms as well, dropped. The copy thread costs ~4 ms per captured frame (≈12 % of one core at 30 fps); a smaller clip size or a GPU-side encoder input would be the next lever. |
| Encoder | `encode/Encoder`, `EncoderProbe`, `EncodeSession`, `Priority` | Probe order NVENC → AMF → QSV → MF hw → MF sw → x264 → OpenH264, cached in `config/vibeyclient/clips-encoder.json`; 1080p/60 fps only with hardware. FFmpeg at below-normal priority (JNA `SetPriorityClass`), raw NV12 on stdin, MPEG-TS on stdout, closed 2 s GOPs (`-force_key_frames` + forced IDR), no B-frames. Writer thread keeps a constant frame rate (repeats the last frame when the game is slower). |
| Ring | `ring/TsChunker`, `ClipRing`, `Chunk`, `BlockPool` | Cut at video PES with `random_access_indicator`; PAT/PMT kept per session; pooled 64 KiB blocks; keeps length + 2 s, never above the RAM cap; an open chunk is only used up to its newest complete frame. JUnit: `TsChunkerTest`. |
| Saving | `ClipSaver`, `store/ClipStore`, `store/ClipMeta` | One `.ts` part per FFmpeg session → `-f concat -c copy -movflags +faststart` → `.<id>.mp4.part` → rename; thumbnail via `.part`; JSON last (atomic). Cleanup after every save and at start (never `kept`), orphans at start. Own I/O thread, not the shared `Tasks` pool. JUnit: `ClipStoreTest` (contract §10). |
| Rules | `Recorder` | Records only with module + buffer on, a world, no screen but chat (containers only with the setting), focused, not minimised, not low FPS (3 s below the threshold / 5 s above +10; the threshold never exceeds 85 % of the frame cap or the monitor rate with vsync), not "encoder behind" (> 10 % skipped in 5 s → 10 s pause). FFmpeg exits after 60 s of pause and on world leave (the ring is dropped on world leave). Server rule `clips` stops and drops the ring at once; `clips.auto` stops automatic clips. |
| Auto clips | `AutoClips`, `mixin/ClientPacketListenerMixin`, `MultiPlayerGameModeMixin`, Fabric `ClientReceiveMessageEvents.GAME` | As in §5; post-roll merge, 10 s cooldown, max per session. |
| UI | `ui/ClipsGalleryScreen`, `ui/ClipViewScreen`, `ui/Thumbs`, `ClipsHud`, `ClipsStats` | Gallery (filter, storage bar), player with the video screens' `FfmpegPlayer` (keep, rename, delete, open folder, copy path, kill marks on the seek bar). The REC dot is a plain HUD element (HUD templates hide unknown *widgets* on first start). `ClipsStats`: `-Dvibeyclient.clips.stats=true`. FFmpeg consent: the video screens' `SetupScreen` with Clips texts. |

Not built yet (prepared): sound (phase 3: the encoder command has no audio input yet, `audio_codec` stays absent),
other Minecraft versions and NeoForge (phase 4), disk ring and trimming (phase 5).

