"""Logo, ikon aplikasi, sprite penonton, dan huruf statis."""
import math
import os
import shutil
import sys
from PIL import Image, ImageDraw, ImageFont
from artlib import hex_rgb, kawung, down

ROOT = sys.argv[1] if len(sys.argv) > 1 else "../../app/src/main"
ASSETS = os.path.join(ROOT, "assets")
RES = os.path.join(ROOT, "res")

SOGAN = "#231710"
NILA = "#1D2A4F"
KUNYIT = "#E3A72F"
CABAI = "#B8281F"
KREM = "#F3E6CB"


def glove(d, cx, cy, s, body=CABAI, cuff=KUNYIT, outline=SOGAN):
    """Sarung tinju menghadap kanan. s = skala (lebar total ~ 2.2*s)."""
    ow = max(2, int(s * 0.07))
    d.rounded_rectangle((cx - 1.05 * s, cy + 0.35 * s, cx - 0.30 * s, cy + 1.05 * s), radius=s * 0.12, fill=cuff, outline=outline, width=ow)
    for i in range(3):
        yy = cy + 0.5 * s + i * 0.17 * s
        d.line((cx - 1.0 * s, yy, cx - 0.35 * s, yy), fill=outline, width=max(1, ow // 2))
    d.ellipse((cx - 0.95 * s, cy - 0.70 * s, cx + 1.05 * s, cy + 0.60 * s), fill=body, outline=outline, width=ow)
    d.ellipse((cx + 0.15 * s, cy - 0.22 * s, cx + 1.20 * s, cy + 0.50 * s), fill=body, outline=outline, width=ow)
    d.ellipse((cx - 0.55 * s, cy + 0.02 * s, cx + 0.45 * s, cy + 0.62 * s), fill=body, outline=outline, width=ow)
    d.arc((cx - 0.35 * s, cy - 0.58 * s, cx + 0.55 * s, cy - 0.05 * s), 200, 300, fill=(255, 255, 255, 120), width=max(2, int(s * 0.07)))
    d.line((cx + 0.28 * s, cy + 0.12 * s, cx + 0.95 * s, cy + 0.12 * s), fill=outline, width=ow)


def roundel(size, with_bg=True, k=4):
    s = size * k
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = s / 2
    if with_bg:
        d.ellipse((c - s * 0.48, c - s * 0.48, c + s * 0.48, c + s * 0.48), fill=NILA, outline=KUNYIT, width=int(s * 0.018))
        d.ellipse((c - s * 0.43, c - s * 0.43, c + s * 0.43, c + s * 0.43), outline=KUNYIT, width=max(2, int(s * 0.006)))
    for ox, oy in ((0, 0), (-0.22, -0.22), (0.22, -0.22), (-0.22, 0.22), (0.22, 0.22)):
        kawung(d, c + ox * s, c + oy * s, s * 0.17, None, outline=(227, 167, 47, 110), width=max(1, int(s * 0.004)))
    return im


def make_logo():
    w, h, k = 1024, 512, 2
    im = Image.new("RGBA", (w * k, h * k), (0, 0, 0, 0))
    r = roundel(300, True, k)
    r = r.resize((300 * k, 300 * k), Image.LANCZOS)
    im.alpha_composite(r, (w * k // 2 - 150 * k, 6 * k))
    d = ImageDraw.Draw(im)
    glove(d, w * k / 2 - 4 * k, 150 * k, 84 * k)
    f1 = ImageFont.truetype(os.path.join(ASSETS, "fonts", "Lora-Bold.ttf"), 128 * k)
    f2 = ImageFont.truetype(os.path.join(ASSETS, "fonts", "Lora-Bold.ttf"), 84 * k)
    for text, font, y, col in (("TARUNG", f1, 300 * k, KREM), ("NUSANTARA", f2, 410 * k, KUNYIT)):
        tw = d.textlength(text, font=font)
        x = w * k / 2 - tw / 2
        d.text((x + 3 * k, y + 4 * k), text, font=font, fill=(35, 23, 16, 255))
        d.text((x, y), text, font=font, fill=col)
    out = im.resize((w, h), Image.LANCZOS)
    out.save(os.path.join(ASSETS, "ui", "logo.webp"), "WEBP", quality=90, method=6)
    return out


def icon_foreground(px):
    """Latar transparan, isi di zona aman 66% tengah (adaptive icon)."""
    k = 4
    s = px * k
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    r = roundel(int(px * 0.66), True, k)
    r = r.resize((int(px * 0.66 * k), int(px * 0.66 * k)), Image.LANCZOS)
    im.alpha_composite(r, ((s - r.width) // 2, (s - r.height) // 2))
    d = ImageDraw.Draw(im)
    glove(d, s * 0.49, s * 0.49, s * 0.155)
    return im.resize((px, px), Image.LANCZOS)


def icon_legacy(px, round_=False):
    k = 4
    s = px * k
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if round_:
        d.ellipse((0, 0, s - 1, s - 1), fill=NILA)
    else:
        d.rounded_rectangle((0, 0, s - 1, s - 1), radius=int(s * 0.2), fill=NILA)
    fg = icon_foreground(px * 2).resize((int(s * 0.96), int(s * 0.96)), Image.LANCZOS)
    im.alpha_composite(fg, ((s - fg.width) // 2, (s - fg.height) // 2))
    return im.resize((px, px), Image.LANCZOS)


def make_icons():
    dens = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in dens.items():
        folder = os.path.join(RES, f"mipmap-{name}")
        os.makedirs(folder, exist_ok=True)
        icon_legacy(px).save(os.path.join(folder, "ic_launcher.png"))
        icon_legacy(px, True).save(os.path.join(folder, "ic_launcher_round.png"))
        icon_foreground(px * 108 // 48).save(os.path.join(folder, "ic_launcher_foreground.png"))
    icon_legacy(512).save(os.path.join(ROOT, "..", "..", "..", "docs", "ikon-512.png"))


def person(d, cx, base, s, skin, cloth, kind, arms_up, flag=None):
    """Penonton: kepala dan bahu. kind menentukan penutup kepala."""
    d.ellipse((cx - 0.42 * s, base - 0.38 * s, cx + 0.42 * s, base + 0.48 * s), fill=cloth)
    d.rectangle((cx - 0.42 * s, base + 0.15 * s, cx + 0.42 * s, base + 0.9 * s), fill=cloth)
    hy = base - 0.52 * s
    r = 0.27 * s
    d.ellipse((cx - r, hy - r, cx + r, hy + r), fill=skin)
    dark = (30, 22, 18, 255)
    if kind == "rambut":
        d.pieslice((cx - r * 1.05, hy - r * 1.15, cx + r * 1.05, hy + r * 0.9), 180, 360, fill=dark)
    elif kind == "peci":
        d.rectangle((cx - r * 0.95, hy - r * 1.25, cx + r * 0.95, hy - r * 0.35), fill=dark)
    elif kind == "caping":
        d.polygon([(cx - r * 1.9, hy - r * 0.3), (cx + r * 1.9, hy - r * 0.3), (cx, hy - r * 2.0)], fill=(196, 150, 70, 255))
    elif kind == "kerudung":
        d.ellipse((cx - r * 1.2, hy - r * 1.25, cx + r * 1.2, hy + r * 1.25), fill=(120, 40, 36, 255))
        d.ellipse((cx - r * 0.72, hy - r * 0.62, cx + r * 0.72, hy + r * 0.78), fill=skin)
    elif kind == "udeng":
        d.rectangle((cx - r * 1.0, hy - r * 1.15, cx + r * 1.0, hy - r * 0.3), fill=(236, 226, 196, 255))
        d.polygon([(cx + r * 0.3, hy - r * 1.15), (cx + r * 0.9, hy - r * 1.7), (cx + r * 1.1, hy - r * 0.9)], fill=(236, 226, 196, 255))
    elif kind == "topi":
        d.pieslice((cx - r * 1.1, hy - r * 1.3, cx + r * 1.1, hy + r * 0.7), 180, 360, fill=(190, 40, 36, 255))
        d.rectangle((cx - r * 1.4, hy - r * 0.3, cx + r * 0.2, hy - r * 0.1), fill=(190, 40, 36, 255))
    if arms_up:
        for sx in (-1, 1):
            d.line((cx + sx * 0.38 * s, base + 0.3 * s, cx + sx * 0.75 * s, base - 0.7 * s), fill=skin, width=int(0.17 * s))
            d.ellipse((cx + sx * 0.75 * s - 0.12 * s, base - 0.82 * s, cx + sx * 0.75 * s + 0.12 * s, base - 0.58 * s), fill=skin)
    if flag == "merah-putih":
        px = cx + 0.78 * s
        d.line((px, base - 1.1 * s, px, base + 0.1 * s), fill=(210, 200, 180, 255), width=int(0.07 * s))
        d.rectangle((px, base - 1.1 * s, px + 0.6 * s, base - 0.82 * s), fill=(206, 40, 38, 255))
        d.rectangle((px, base - 0.82 * s, px + 0.6 * s, base - 0.54 * s), fill=(244, 240, 232, 255))
    if flag == "penjor":
        px = cx + 0.7 * s
        d.line((px, base - 1.25 * s, px, base + 0.1 * s), fill=(206, 170, 74, 255), width=int(0.07 * s))
        d.ellipse((px - 0.12 * s, base - 1.25 * s, px + 0.12 * s, base - 0.85 * s), fill=(250, 238, 196, 255))


def make_crowd():
    cell, k = 96, 4
    skins = [(222, 186, 150), (196, 150, 110), (170, 118, 84), (146, 98, 70)]
    cloth = [(214, 206, 190), (206, 196, 176), (224, 218, 204), (198, 190, 172)]
    kinds = ["rambut", "peci", "caping", "kerudung", "udeng", "topi", "rambut", "peci"]
    flags = [None, None, None, None, "penjor", "merah-putih", None, None]
    ups = [False, True, False, True, False, True, True, False]
    sheet = Image.new("RGBA", (cell * len(kinds), cell), (0, 0, 0, 0))
    for i, kind in enumerate(kinds):
        im = Image.new("RGBA", (cell * k, cell * k), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        person(d, cell * k * 0.45, cell * k * 0.62, cell * k * 0.56, skins[i % 4] + (255,), cloth[i % 4] + (255,), kind, ups[i], flags[i])
        sheet.alpha_composite(im.resize((cell, cell), Image.LANCZOS), (i * cell, 0))
    sheet.save(os.path.join(ASSETS, "ui", "crowd.webp"), "WEBP", lossless=True, method=6)
    return sheet


def make_fonts():
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer
    src = "/usr/share/fonts/truetype/google-fonts"
    f = TTFont(os.path.join(src, "Lora-Variable.ttf"))
    inst = instancer.instantiateVariableFont(f, {"wght": 700})
    inst.save(os.path.join(ASSETS, "fonts", "Lora-Bold.ttf"))
    for name in ("Poppins-Medium.ttf", "Poppins-Bold.ttf"):
        shutil.copy(os.path.join(src, name), os.path.join(ASSETS, "fonts", name))


if __name__ == "__main__":
    os.makedirs(os.path.join(ASSETS, "fonts"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "ui"), exist_ok=True)
    make_fonts()
    make_logo()
    make_icons()
    make_crowd()
    print("selesai")
