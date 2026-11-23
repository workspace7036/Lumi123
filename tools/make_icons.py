# Рисует шарик Люми: кадры анимации для шторки, значок строки состояния, иконку приложения.
# Цвет шарика меняется здесь (TOP / BOTTOM), потом: python3 tools/make_icons.py
import os, numpy as np
from PIL import Image, ImageDraw
TOP, BOTTOM = (47, 198, 245), (59, 124, 240)      # верх и низ шарика
HI = (210, 242, 255)                              # блик
B = os.path.join(os.path.dirname(__file__), '..', 'app/src/main/res/')

def ball(sz, look=0.0, blink=False):
    S = sz * 3
    y, x = np.mgrid[0:S, 0:S] / S
    t = np.clip((y - .06) / .88, 0, 1)[..., None]
    col = np.array(TOP) * (1 - t) + np.array(BOTTOM) * t
    d = np.hypot(x - .40, y - .22) / .36
    a = np.clip(1 - d, 0, 1)[..., None] ** 1.5 * .6
    col = col * (1 - a) + np.array(HI) * a
    rgba = np.dstack([col, np.full((S, S), 255)]).astype(np.uint8)
    m = Image.new('L', (S, S), 0); ImageDraw.Draw(m).ellipse([S * .06, S * .06, S * .94, S * .94], fill=255)
    im = Image.fromarray(rgba, 'RGBA'); im.putalpha(m)
    dr = ImageDraw.Draw(im); D = S * .88
    ew, eh = D * .085, (D * .02 if blink else D * .125)
    for cx in (.5 - .19, .5 + .19):
        cx = S * cx + look * D * .07; cy = S * .5 + D * .0
        dr.rounded_rectangle([cx - ew, cy - eh, cx + ew, cy + eh], radius=min(ew, eh), fill=(255, 255, 255, 255))
    return im.resize((sz, sz), Image.LANCZOS)

def status(sz):
    S = sz * 8; m = Image.new('L', (S, S), 0); d = ImageDraw.Draw(m)
    d.ellipse([S * .06, S * .06, S * .94, S * .94], fill=255)
    D = S * .88; ew, eh = D * .085, D * .125
    for cx in (.5 - .19, .5 + .19):
        cx *= S; d.rounded_rectangle([cx - ew, S * .5 - eh, cx + ew, S * .5 + eh], radius=ew, fill=0)
    o = Image.new('RGBA', (sz, sz), (255, 255, 255, 0)); o.putalpha(m.resize((sz, sz), Image.LANCZOS)); return o

os.makedirs(B + 'drawable-nodpi', exist_ok=True)
for n, kw in [('c', {}), ('l', {'look': -1}), ('r', {'look': 1}), ('b', {'blink': True})]:
    ball(144, **kw).save(B + f'drawable-nodpi/lumi_f_{n}.png')
ball(256).save(B + 'drawable-nodpi/lumi_large.png')
for dn, sz in [('mdpi', 24), ('hdpi', 36), ('xhdpi', 48), ('xxhdpi', 72), ('xxxhdpi', 96)]:
    os.makedirs(B + f'drawable-{dn}', exist_ok=True); status(sz).save(B + f'drawable-{dn}/ic_lumi.png')
for dn, sz in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
    os.makedirs(B + f'mipmap-{dn}', exist_ok=True); ball(sz).save(B + f'mipmap-{dn}/ic_launcher.png')
