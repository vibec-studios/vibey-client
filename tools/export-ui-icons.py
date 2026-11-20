import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/vibeyclient/textures/ui/icons"
JAVA = [ROOT / "src/main/java/dev/henny/vibeyclient/ui/Icons.java",
        ROOT / "src/main/java/dev/henny/vibeyclient/ui/hud/HudIcons.java"]
DOC = ROOT / "docs/UI-NEW.md"


def constants(src):
    out = {}
    for name, value in re.findall(r'\b([A-Z_]+)\s*=\s*"([a-z0-9_]+)"', src):
        out[name] = value
    return out


def java_maps(src):
    names = constants(src)
    maps = {}
    for m in re.finditer(r'(?:ICONS\.put|Icons\.register|register)\(\s*([A-Z_]+)\s*,\s*(?:new String\[\]\s*\{)?((?:\s*"[.#]{9}"\s*,?)+)', src):
        rows = re.findall(r'"([.#]{9})"', m.group(2))
        if m.group(1) in names and len(rows) == 9:
            maps[names[m.group(1)]] = rows
    return maps


def doc_maps(text):
    maps = {}
    for name, rows in re.findall(r'^\| `([a-z0-9_]+)` \| `([.# ]+)` \|', text, re.M):
        parts = rows.split()
        if len(parts) == 9:
            maps[name] = parts
    return maps


def write(name, rows):
    img = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x, y), (255, 255, 255, 255))
    img.save(OUT / f"{name}.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    maps = {}
    for f in JAVA:
        maps.update(java_maps(f.read_text(encoding="utf8")))
    for name, rows in doc_maps(DOC.read_text(encoding="utf8")).items():
        maps.setdefault(name, rows)
    for name, rows in sorted(maps.items()):
        write(name, rows)
    print(f"{len(maps)} icons -> {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
