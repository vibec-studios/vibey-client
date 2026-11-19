"""Collects the PvP gametest screenshots into build/review/pvp/ (needs Pillow).

    python tools/pvp-review.py [screenshots dir] [out dir]

Copies every NNNN_pvp_*.png without the counter prefix, adds 5x crops of the screen centre for the crosshair shots
(nearest neighbour, so single pixels stay visible) and one contact sheet of all crosshair styles.
"""
import os
import re
import shutil
import sys

from PIL import Image, ImageDraw

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, 'versions', 'fabric-1.21.11', 'build', 'run', 'clientGameTest', 'screenshots')
out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(root, 'build', 'review', 'pvp')
os.makedirs(out, exist_ok=True)

shots = {}
for f in sorted(os.listdir(src)):
    m = re.match(r'^\d+_(pvp_.+\.png)$', f)
    if m:
        shots[m.group(1)] = os.path.join(src, f)
for name, path in shots.items():
    shutil.copyfile(path, os.path.join(out, name))

CROP_W, CROP_H, ZOOM = 96, 60, 5
crops = []
for name, path in shots.items():
    if not name.startswith('pvp_1') and not name.startswith('pvp_2') and 'sprint' not in name:
        continue
    if 'editor' in name:
        continue
    im = Image.open(path).convert('RGB')
    cx, cy = im.width // 2, im.height // 2
    c = im.crop((cx - CROP_W // 2, cy - CROP_H // 2, cx + CROP_W // 2, cy + CROP_H // 2))
    c = c.resize((CROP_W * ZOOM, CROP_H * ZOOM), Image.NEAREST)
    crop_name = name.replace('.png', '_crop.png')
    c.save(os.path.join(out, crop_name))
    crops.append((name[:-4], c))

if crops:
    cols = 3
    cw, ch = CROP_W * ZOOM, CROP_H * ZOOM + 18
    sheet = Image.new('RGB', (cols * cw, ((len(crops) + cols - 1) // cols) * ch), (12, 12, 16))
    d = ImageDraw.Draw(sheet)
    for i, (label, c) in enumerate(crops):
        x, y = (i % cols) * cw, (i // cols) * ch
        sheet.paste(c, (x, y + 18))
        d.text((x + 6, y + 3), label, fill=(230, 230, 235))
    sheet.save(os.path.join(out, 'pvp_crosshair_sheet.png'))

print(f'{len(shots)} shots, {len(crops)} crops -> {out}')
