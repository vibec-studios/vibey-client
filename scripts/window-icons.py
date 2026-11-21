from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parent.parent
src = Image.open(root / "assets" / "icon.png").convert("RGBA")
box = src.getbbox()
art = src.crop(box)
side = max(art.size)
square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
square.paste(art, ((side - art.width) // 2, (side - art.height) // 2))
out = root / "src" / "main" / "resources" / "assets" / "vibeyclient" / "icons"
out.mkdir(parents=True, exist_ok=True)
for n in (16, 32, 48, 128, 256):
    square.resize((n, n), Image.LANCZOS).save(out / f"icon_{n}x{n}.png", optimize=True)
    print(f"icon_{n}x{n}.png")
