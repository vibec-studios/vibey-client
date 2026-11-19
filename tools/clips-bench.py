"""Clips encoder benchmark (docs/CLIPS.md section 2): synthetic frames piped to FFmpeg in real time, the way the
game's readback would feed it, encoded into 2 s MPEG-TS ring segments; then the last 3 segments are joined with
-c copy into an MP4. Prints one JSON line per run: FFmpeg CPU (utime+stime over wall time, in % of ONE logical core
and of the whole CPU), max RSS, the writer's own CPU, late frames, bitrate, join time.

    python -I tools/clips-bench.py <empty out dir> [encoder ...]     # encoders: see ENC below (default: all)

Needs an `ffmpeg` on the PATH. libx264 entries are a software REFERENCE only (GPL, not in the LGPL build we ship).
Light on purpose: 6 s per run, about 2 minutes for everything. Delete the out dir afterwards.
"""
import os, subprocess, sys, time, re, shutil, json

if len(sys.argv) < 2:
    sys.exit(__doc__)
OUT = sys.argv[1]
SECONDS = 6
CORES = os.cpu_count()

def frames(w, h, n=6):
    # base: a horizontal gradient with coarse "blocks" (game-like flat areas), rows shifted per frame (motion),
    # plus a noisy strip (particles / foliage) that changes every frame.
    row = bytearray()
    for x in range(w):
        v = (x * 255 // w)
        blk = 40 if (x // 64) % 2 else 0
        row += bytes(((v + blk) & 255, (v // 2 + 60) & 255, (255 - v) & 255, 255))
    base = bytes(row) * h
    out = []
    stride = w * 4
    for i in range(n):
        shift = (i * 37 * 4) % stride
        f = bytearray(base[shift:] + base[:shift])
        strip = h // 8
        f[strip * stride * 3: strip * stride * 4] = os.urandom(strip * stride)
        out.append(bytes(f))
    return out

def run(name, w, h, fps, args):
    d = os.path.join(OUT, f"{name}_{h}p{fps}")
    shutil.rmtree(d, ignore_errors=True)
    os.makedirs(d)
    cmd = ["ffmpeg", "-hide_banner", "-loglevel", "info", "-nostats", "-benchmark",
           "-f", "rawvideo", "-pix_fmt", "nv12" if name.endswith("_nv12in") else "rgba", "-s", f"{w}x{h}", "-framerate", str(fps), "-i", "pipe:0",
           *args, "-g", str(fps * 2), "-an",
           "-f", "segment", "-segment_time", "2", "-segment_format", "mpegts", "-segment_wrap", "16",
           "-segment_list", os.path.join(d, "list.csv"), "-segment_list_type", "csv",
           os.path.join(d, "seg%03d.ts")]
    fr = frames(w, h)
    if name.endswith("_nv12in"):
        fr = [f[: w * h * 3 // 2] for f in fr]
    ef = open(os.path.join(d, "err.txt"), "wb")
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stderr=ef, stdout=subprocess.DEVNULL)
    n = SECONDS * fps
    t0 = time.perf_counter(); c0 = time.process_time(); late = 0
    try:
        for i in range(n):
            target = t0 + i / fps
            now = time.perf_counter()
            if now < target:
                time.sleep(target - now)
            elif now - target > 1 / fps:
                late += 1
            p.stdin.write(fr[i % len(fr)])
        p.stdin.close()
    except BrokenPipeError:
        pass
    wc = time.process_time() - c0
    p.wait(); ef.close()
    err = open(os.path.join(d, "err.txt"), encoding="utf-8", errors="replace").read()
    wall = time.perf_counter() - t0
    m = re.search(r"utime=([\d.]+)s stime=([\d.]+)s rtime=([\d.]+)s", err)
    rss = re.search(r"maxrss=(\d+)KiB", err)
    if not m:
        return {"name": name, "res": f"{h}p{fps}", "error": err.strip()[-300:]}
    cpu = float(m.group(1)) + float(m.group(2))
    rt = float(m.group(3))
    segs = sorted(f for f in os.listdir(d) if f.endswith(".ts"))
    size = sum(os.path.getsize(os.path.join(d, f)) for f in segs)
    # join the last 3 complete segments without re-encoding
    lst = os.path.join(d, "concat.txt")
    with open(lst, "w") as f:
        for s in segs[-3:]:
            f.write(f"file '{s}'\n")
    j0 = time.perf_counter()
    jr = subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", lst,
                         "-c", "copy", "-movflags", "+faststart", os.path.join(d, "clip.mp4")], capture_output=True)
    jt = time.perf_counter() - j0
    return {"name": name, "res": f"{h}p{fps}", "ffmpeg_cpu_core_pct": round(100 * cpu / rt, 1),
            "ffmpeg_cpu_total_pct": round(100 * cpu / rt / CORES, 1), "maxrss_mb": round(int(rss.group(1)) / 1024) if rss else None,
            "writer_cpu_core_pct": round(100 * wc / wall, 1), "late_frames": late, "frames": n,
            "mbit_s": round(size * 8 / wall / 1e6, 2), "segments": len(segs), "join_s": round(jt, 2),
            "join_ok": jr.returncode == 0}

ENC = {
    "qsv": ["-vf", "format=nv12", "-c:v", "h264_qsv", "-preset", "veryfast", "-look_ahead", "0"],
    "qsv_nv12in": ["-c:v", "h264_qsv", "-preset", "veryfast", "-look_ahead", "0"],
    "x264_nv12in": ["-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency"],
    "mf_hw": ["-vf", "format=nv12", "-c:v", "h264_mf", "-hw_encoding", "1", "-rate_control", "cbr"],
    "mf_sw": ["-vf", "format=nv12", "-c:v", "h264_mf", "-hw_encoding", "0", "-rate_control", "cbr"],
    "x264_vf": ["-pix_fmt", "yuv420p", "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency"],
}
RES = {"720p30": (1280, 720, 30, "6M"), "1080p60": (1920, 1080, 60, "12M")}
sel = sys.argv[2:] or list(ENC)
res = []
for r, (w, h, fps, br) in RES.items():
    for e in sel:
        out = run(e, w, h, fps, ENC[e] + ["-b:v", br, "-maxrate", br, "-bufsize", br])
        print(json.dumps(out), flush=True)
        res.append(out)
        time.sleep(1)
