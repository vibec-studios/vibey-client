import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PREVIEW = os.path.join(ROOT, 'docs', 'ui-preview')
SHOTS = os.path.join(ROOT, 'build', 'review', 'ui-new')
OUT = os.path.join(SHOTS, 'compare')
HEIGHT = 360

PAIRS = {
    'title': 'ui_title_hover_{s}_{l}',
    'menu-grid': 'ui_menu_grid_{s}_{l}',
    'menu-search': 'ui_menu_search_{s}_{l}',
    'menu-search-empty': 'ui_menu_search_empty_{s}_{l}',
    'menu-page': 'ui_menu_page_{s}_{l}',
    'menu-page-more': 'ui_menu_more_{s}_{l}',
    'menu-quick': 'ui_menu_quick_{s}_{l}',
    'menu-about': 'ui_menu_about_{s}_{l}',
    'menu-friends-off': 'ui_menu_friends_{s}_{l}',
    'onboarding-1': 'ui_onboarding1_{s}_{l}',
    'onboarding-2': 'ui_onboarding2_{s}_{l}',
    'onboarding-3': 'ui_onboarding3_{s}_{l}',
    'toast': 'ui_toast_{s}_{l}',
    'confirm': 'ui_confirm_{s}_{l}',
}
FIXED = {
    'pause': 'ui_pause_1280_s3',
    'pause-de': 'ui_pause_1280_s3_de',
    'wardrobe': 'wardrobe_s3_en_1_pet',
    'wardrobe-de': 'wardrobe_s3_de_1_cape',
    'wardrobe-empty': 'wardrobe_s2_en_11_search_empty',
}


def fit(img):
    w, h = img.size
    return img.resize((max(1, round(w * HEIGHT / h)), HEIGHT), Image.NEAREST)


def compose(preview_path, shot_path, out_path):
    a = fit(Image.open(preview_path).convert('RGB'))
    b = fit(Image.open(shot_path).convert('RGB'))
    canvas = Image.new('RGB', (a.width + b.width + 12, HEIGHT + 18), (20, 20, 24))
    canvas.paste(a, (0, 18))
    canvas.paste(b, (a.width + 12, 18))
    d = ImageDraw.Draw(canvas)
    d.text((4, 3), 'preview: ' + os.path.basename(preview_path), fill=(200, 200, 210))
    d.text((a.width + 16, 3), 'game: ' + os.path.basename(shot_path), fill=(200, 200, 210))
    canvas.save(out_path)


def main():
    os.makedirs(OUT, exist_ok=True)
    done = 0
    jobs = []
    for name, shot in PAIRS.items():
        for suffix, s, l in (('', '427', 'en'), ('-de', '427', 'de'), ('-640', '640', 'en')):
            jobs.append((name + suffix, shot.format(s=s, l=l)))
    jobs.extend(FIXED.items())
    for preview, shot in jobs:
        p = os.path.join(PREVIEW, preview + '.png')
        g = os.path.join(SHOTS, shot + '.png')
        if os.path.exists(p) and os.path.exists(g):
            compose(p, g, os.path.join(OUT, preview + '.png'))
            done += 1
        else:
            print('missing', preview if not os.path.exists(p) else shot, file=sys.stderr)
    print(done, 'comparisons in', OUT)


if __name__ == '__main__':
    main()
