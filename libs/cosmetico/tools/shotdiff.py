import sys
from pathlib import Path

from PIL import Image, ImageChops


def compare(a_path, b_path, out_dir):
    a = Image.open(a_path).convert("RGB")
    b = Image.open(b_path).convert("RGB")
    if a.size != b.size:
        return None
    diff = ImageChops.difference(a, b)
    gray = diff.convert("L")
    hist = gray.histogram()
    changed = sum(hist[1:])
    strong = sum(hist[17:])
    peak = max(hi for _, hi in diff.getextrema())
    if changed and out_dir:
        out_dir.mkdir(parents=True, exist_ok=True)
        gray.point(lambda v: 255 if v > 0 else 0).save(out_dir / a_path.name)
    return peak, changed, strong, a.size[0] * a.size[1]


def main():
    base, after = Path(sys.argv[1]), Path(sys.argv[2])
    out_dir = Path(sys.argv[3]) if len(sys.argv) > 3 else None
    print("| shot | max diff (0-255) | pixels changed | pixels > 16 |")
    print("|---|---|---|---|")
    for a_path in sorted(base.glob("*.png")):
        b_path = after / a_path.name
        if not b_path.exists():
            print(f"| {a_path.stem} | missing | | |")
            continue
        r = compare(a_path, b_path, out_dir)
        if r is None:
            print(f"| {a_path.stem} | size differs | | |")
            continue
        peak, changed, strong, total = r
        print(f"| {a_path.stem} | {peak} | {changed} ({100 * changed / total:.3f} %) | {strong} |")


if __name__ == "__main__":
    main()
