"""Membuat 6 latar arena (WebP) untuk Tarung Nusantara.

Tiap latar 1600x720. Bagian bawah (lantai ring) digambar ulang di dalam game,
jadi latar hanya perlu tanah yang gelap dan tenang di sana.
"""
import math
import os
import sys
import numpy as np
from PIL import Image, ImageDraw
from artlib import *

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
os.makedirs(OUT, exist_ok=True)
K = 2


def layer():
    im = Image.new("RGBA", (W * K, H * K), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def comp(base, lay):
    small = lay.resize((W, H), Image.LANCZOS)
    out = Image.alpha_composite(base.convert("RGBA"), small)
    return out.convert("RGB")


def P(pts):
    return [(x * K, y * K) for x, y in pts]


def stars(d, n, ymax, seed, col=(255, 244, 214, 200)):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        x, y = rng.uniform(0, W), rng.uniform(0, ymax)
        r = rng.choice([0.8, 1.0, 1.4, 1.8], p=[0.4, 0.3, 0.2, 0.1])
        d.ellipse(((x - r) * K, (y - r) * K, (x + r) * K, (y + r) * K), fill=col)


def tree(d, x, base, h, col, seed=0):
    rng = np.random.default_rng(seed)
    d.rectangle(P([(x - h * 0.03, base - h * 0.35), (x + h * 0.03, base)]), fill=col)
    for _ in range(7):
        cx = x + rng.uniform(-h * 0.22, h * 0.22)
        cy = base - h * rng.uniform(0.45, 0.85)
        r = h * rng.uniform(0.14, 0.24)
        d.ellipse(P([(cx - r, cy - r * 0.8), (cx + r, cy + r * 0.8)]), fill=col)


def palm(d, x, base, h, col, lean=0.2, seed=0):
    rng = np.random.default_rng(seed)
    tx, ty = x + lean * h, base - h
    pts = [(x - 4, base), (x + 4, base), (tx + 3, ty), (tx - 3, ty)]
    d.polygon(P(pts), fill=col)
    for i in range(8):
        a = math.radians(-160 + i * 45 + rng.uniform(-8, 8))
        ln = h * rng.uniform(0.32, 0.42)
        mx = tx + math.cos(a) * ln * 0.55
        my = ty + math.sin(a) * ln * 0.55 - ln * 0.18
        ex = tx + math.cos(a) * ln
        ey = ty + math.sin(a) * ln + ln * 0.22
        d.line(P([(tx, ty), (mx, my), (ex, ey)]), fill=col, width=int(5 * K / 2))


def fr(d, ys, bottom, color):
    pts = [(x * K, float(y) * K) for x, y in enumerate(ys)]
    pts += [((len(ys) - 1) * K, bottom * K), (0, bottom * K)]
    d.polygon(pts, fill=color)


def flare(base, x, y, r, col, s):
    return glow(base, x, y, r, col, s)


def ground(base, y0, top, bottom):
    g = vgrad(W, H - y0, [(0, top), (1, bottom)])
    base.paste(g, (0, y0))
    return base


# --------------------------------------------------------------------- Keraton
def keraton():
    base = vgrad(W, H, [(0, "#6E2E2B"), (0.30, "#B85A33"), (0.52, "#EBA24E"), (0.6, "#F6CB78")])
    base = flare(base, 800, 400, 520, "#FFD58A", 0.8)
    lay, d = layer()
    rng = np.random.default_rng(3)
    ys = ridge(W, 395, 26, 11, rough=3)
    fr(d, ys, H, (70, 38, 28, 255))
    ys = np.array(ys)
    d2 = d
    for x in (120, 300, 1330, 1500):
        tree(d2, x, 410, 200 + rng.uniform(-30, 40), (56, 30, 22, 255), seed=int(x))
    # pendopo joglo kiri dan kanan
    for cx, s in ((250, 1.0), (1350, 1.0)):
        d.polygon(P([(cx - 150 * s, 360), (cx + 150 * s, 360), (cx + 95 * s, 318), (cx - 95 * s, 318)]), fill=(52, 28, 22, 255))
        d.polygon(P([(cx - 95 * s, 318), (cx + 95 * s, 318), (cx + 45 * s, 286), (cx - 45 * s, 286)]), fill=(46, 24, 19, 255))
        d.rectangle(P([(cx - 120 * s, 360), (cx + 120 * s, 405)]), fill=(38, 20, 16, 255))
        for px in np.linspace(cx - 105 * s, cx + 105 * s, 6):
            d.rectangle(P([(px - 5, 362), (px + 5, 405)]), fill=(74, 40, 26, 255))
    # gapura candi bentar: dua sisi bertingkat dengan celah di tengah
    stone = (112, 76, 58, 255)
    dark = (78, 52, 40, 255)
    light = (150, 108, 82, 255)
    for side in (-1, 1):
        x0 = 800 + side * 38
        steps = [(150, 118), (128, 164), (106, 202), (84, 232), (62, 254), (40, 270)]
        for i, (wd, top) in enumerate(steps):
            xa, xb = sorted((x0, x0 + side * wd))
            ytop = 405 - top - 40 - i * 4
            ybot = 405 - (steps[i - 1][1] if i else 0)
            ytop = 405 - top
            d.rectangle(P([(xa, ytop), (xb, ybot)]), fill=stone if i % 2 == 0 else dark)
            d.line(P([(xa, ytop), (xb, ytop)]), fill=light, width=2 * K)
        # kepala gapura
        xa, xb = sorted((x0, x0 + side * 34))
        d.polygon(P([(xa, 135), (xb, 135), ((xa + xb) / 2 + side * 6, 96)]), fill=stone)
        # ukiran: garis horizontal tipis seperti susunan bata
        for i, (wd, top) in enumerate(steps):
            ybot = 405 - (steps[i - 1][1] if i else 0)
            for yy in range(int(405 - top) + 14, int(ybot) - 4, 16):
                d.line(P([(x0 + side * 4, yy), (x0 + side * (wd - 4), yy)]), fill=(60, 40, 32, 120), width=K)
    base = comp(base, lay)
    base = ground(base, 405, "#4A2C1E", "#1B0F0A")
    lay, d = layer()
    # lantai batu: garis susunan, miring ke titik hilang, sangat halus
    for i in range(-14, 15):
        d.line(P([(800 + i * 28, 408), (800 + i * 140, H)]), fill=(20, 10, 6, 90), width=K)
    for yy in (430, 462, 505, 560, 630):
        d.line(P([(0, yy), (W, yy)]), fill=(20, 10, 6, 90), width=K)
    # obor di tepi
    for x in (70, 1530):
        d.rectangle(P([(x - 5, 330), (x + 5, 420)]), fill=(52, 30, 22, 255))
    base = comp(base, lay)
    for x in (70, 1530):
        base = flare(base, x, 322, 150, "#FF9A3C", 0.9)
    return base


# ----------------------------------------------------------------------- Toba
def toba():
    base = vgrad(W, H, [(0, "#9DB0B7"), (0.3, "#C5D1D2"), (0.5, "#E5E7DC")])
    lay, d = layer()
    cols = [(139, 160, 168, 255), (108, 134, 144, 255), (78, 108, 118, 255)]
    bases = [330, 360, 385]
    amps = [70, 52, 34]
    for i in range(3):
        ys = ridge(W, bases[i], amps[i], 20 + i, rough=4, scale=0.8 + i * 0.3)
        fr(d, ys, 420, cols[i])
    base = comp(base, lay)
    # kabut antara lapisan
    for y0, a in ((335, 0.35), (365, 0.30)):
        fog = np.zeros((H, W), dtype=np.float32)
        yy = np.arange(H)[:, None]
        fog = np.exp(-((yy - y0) / 22.0) ** 2) * a
        arr = np.asarray(base).astype(np.float32)
        arr = arr * (1 - fog[:, :, None]) + 238 * fog[:, :, None]
        base = Image.fromarray(arr.astype(np.uint8), "RGB")
    # danau
    lake = vgrad(W, H - 392, [(0, "#8FAAB0"), (0.4, "#5F8089"), (1, "#26404A")])
    base.paste(lake, (0, 392))
    lay, d = layer()
    rng = np.random.default_rng(5)
    for _ in range(170):
        x = rng.uniform(0, W)
        y = rng.uniform(398, 470)
        ln = rng.uniform(14, 70) * (0.5 + (y - 398) / 90)
        d.line(P([(x, y), (x + ln, y)]), fill=(255, 255, 255, int(rng.uniform(40, 120))), width=K)
    # perahu
    bx, by = 1010, 424
    d.polygon(P([(bx - 70, by - 12), (bx - 56, by + 8), (bx + 58, by + 8), (bx + 74, by - 16), (bx + 60, by - 4), (bx - 58, by - 4)]), fill=(58, 40, 30, 255))
    d.line(P([(bx - 5, by - 6), (bx - 5, by - 58)]), fill=(58, 40, 30, 255), width=3 * K)
    d.polygon(P([(bx - 5, by - 56), (bx + 36, by - 24), (bx - 5, by - 24)]), fill=(214, 198, 168, 255))
    # rumah bolon di tepi kanan
    for hx, sc in ((1330, 1.0), (1520, 0.8)):
        w_ = 210 * sc
        top = 300
        d.polygon(P([(hx - w_ * 0.55, top + 38 * sc), (hx - w_ * 0.12, top - 36 * sc), (hx + w_ * 0.12, top - 36 * sc), (hx + w_ * 0.55, top + 38 * sc),
                     (hx + w_ * 0.38, top + 62 * sc), (hx - w_ * 0.38, top + 62 * sc)]), fill=(46, 36, 30, 255))
        d.polygon(P([(hx - w_ * 0.36, top + 62 * sc), (hx + w_ * 0.36, top + 62 * sc), (hx + w_ * 0.30, top + 128 * sc), (hx - w_ * 0.30, top + 128 * sc)]), fill=(74, 52, 40, 255))
        for i in range(5):
            xx = hx - w_ * 0.22 + i * w_ * 0.11
            d.polygon(P([(xx, top + 80 * sc), (xx + w_ * 0.05, top + 104 * sc), (xx - w_ * 0.05, top + 104 * sc)]), fill=((170, 38, 30, 255) if i % 2 == 0 else (232, 224, 208, 255)))
        for px in np.linspace(hx - w_ * 0.28, hx + w_ * 0.28, 6):
            d.rectangle(P([(px - 4, top + 128 * sc), (px + 4, top + 128 * sc + 34)]), fill=(40, 30, 24, 255))
    base = comp(base, lay)
    base = ground(base, 440, "#3E4440", "#171A17")
    return base


# ------------------------------------------------------------------- Makassar
def pinisi(d, x, y, s, hull, sail):
    d.polygon(P([(x - 150 * s, y - 24 * s), (x + 160 * s, y - 40 * s), (x + 120 * s, y + 12 * s), (x - 110 * s, y + 12 * s)]), fill=hull)
    d.polygon(P([(x + 160 * s, y - 40 * s), (x + 205 * s, y - 70 * s), (x + 150 * s, y - 28 * s)]), fill=hull)
    for mx, mh in ((x - 70 * s, 190), (x + 30 * s, 220), (x + 110 * s, 170)):
        d.line(P([(mx, y - 24 * s), (mx, y - mh * s)]), fill=hull, width=int(4 * K))
        d.polygon(P([(mx + 4, y - mh * s + 6), (mx + 66 * s, y - 40 * s), (mx + 4, y - 40 * s)]), fill=sail)
    d.polygon(P([(x - 150 * s, y - 28 * s), (x - 70 * s, y - 120 * s), (x - 70 * s, y - 28 * s)]), fill=sail)


def makassar():
    base = vgrad(W, H, [(0, "#4A2B4F"), (0.28, "#A6463F"), (0.46, "#EE8F4C"), (0.54, "#F8C06E")])
    base = flare(base, 560, 372, 460, "#FFD088", 0.85)
    sea = vgrad(W, H - 372, [(0, "#E48A4E"), (0.18, "#8B4A52"), (1, "#2C2340")])
    base.paste(sea, (0, 372))
    lay, d = layer()
    rng = np.random.default_rng(9)
    for _ in range(140):
        x = rng.uniform(0, W)
        y = rng.uniform(376, 440)
        ln = rng.uniform(20, 90)
        d.line(P([(x, y), (x + ln, y)]), fill=(255, 214, 150, int(rng.uniform(40, 130))), width=K)
    pinisi(d, 330, 392, 1.25, (38, 22, 28, 255), (226, 168, 132, 255))
    pinisi(d, 1090, 388, 0.8, (40, 24, 30, 255), (214, 150, 120, 255))
    pinisi(d, 800, 380, 0.45, (60, 36, 40, 255), (200, 140, 112, 255))
    # dermaga dan peti di sisi kanan
    d.rectangle(P([(1300, 395), (W, 430)]), fill=(44, 28, 26, 255))
    for px in range(1310, W, 46):
        d.rectangle(P([(px, 428), (px + 8, 470)]), fill=(34, 22, 22, 255))
    for cx in (1380, 1450, 1520):
        d.rectangle(P([(cx, 360), (cx + 56, 396)]), fill=(70, 44, 34, 255))
        d.line(P([(cx, 378), (cx + 56, 378)]), fill=(46, 28, 24, 255), width=2 * K)
    base = comp(base, lay)
    base = ground(base, 440, "#3A2622", "#150C0C")
    lay, d = layer()
    for yy in range(446, H, 30):
        d.line(P([(0, yy), (W, yy)]), fill=(14, 8, 8, 110), width=K)
    base = comp(base, lay)
    return base


# --------------------------------------------------------------------- Betang
def spiral(d, cx, cy, r, col, width):
    pts = []
    for i in range(60):
        t = i / 59
        a = t * 5.2 * math.pi
        rr = r * (1 - t * 0.85)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.line(P(pts), fill=col, width=width)


def betang():
    base = vgrad(W, H, [(0, "#070C1B"), (0.45, "#141F3A"), (0.6, "#1F2A44")])
    lay, d = layer()
    stars(d, 150, 280, 7)
    ys = ridge(W, 360, 60, 31, rough=4)
    fr(d, ys, H, (10, 18, 22, 255))
    for x in range(40, W, 90):
        tree(d, x + (x * 7) % 40, 388, 170 + (x * 13) % 90, (8, 15, 18, 255), seed=x)
    # rumah betang: panjang di atas tiang
    d.polygon(P([(190, 262), (1410, 262), (1480, 300), (120, 300)]), fill=(48, 30, 20, 255))
    d.rectangle(P([(150, 300), (1450, 372)]), fill=(66, 42, 26, 255))
    for x in range(160, 1450, 60):
        d.rectangle(P([(x, 300), (x + 8, 372)]), fill=(40, 24, 16, 255))
    # papan ukiran dan pintu
    for cx in (330, 560, 1040, 1270):
        d.rectangle(P([(cx - 70, 312), (cx + 70, 362)]), fill=(38, 22, 14, 255))
        spiral(d, cx - 30, 337, 18, (200, 150, 70, 255), 2 * K)
        spiral(d, cx + 30, 337, 18, (200, 150, 70, 255), 2 * K)
    d.rectangle(P([(740, 308), (860, 372)]), fill=(14, 8, 6, 255))
    d.polygon(P([(730, 308), (870, 308), (800, 276)]), fill=(120, 40, 28, 255))
    for x in range(180, 1430, 90):
        d.rectangle(P([(x, 372), (x + 12, 438)]), fill=(34, 20, 14, 255))
    base = comp(base, lay)
    base = ground(base, 438, "#2B1A10", "#0C0705")
    base = flare(base, 800, 470, 700, "#FF8A2A", 0.7)
    lay, d = layer()
    for yy in range(446, H, 26):
        d.line(P([(0, yy), (W, yy)]), fill=(10, 5, 3, 110), width=K)
    base = comp(base, lay)
    return base


# ----------------------------------------------------------------------- Bali
def bali():
    base = vgrad(W, H, [(0, "#4E9BD4"), (0.4, "#9ED0EA"), (0.55, "#D8EEF2")])
    lay, d = layer()
    rng = np.random.default_rng(14)
    for cx, cy, s in ((260, 110, 1.0), (880, 70, 1.3), (1360, 140, 0.9)):
        for i in range(6):
            ox, oy = rng.uniform(-60, 60) * s, rng.uniform(-12, 12) * s
            r = rng.uniform(34, 62) * s
            d.ellipse(P([(cx + ox - r, cy + oy - r * 0.55), (cx + ox + r, cy + oy + r * 0.55)]), fill=(255, 255, 255, 220))
    ys = ridge(W, 300, 40, 41, rough=3)
    fr(d, ys, H, (92, 150, 120, 255))
    # sawah bertingkat
    greens = [(116, 178, 70), (92, 160, 62), (140, 196, 80), (76, 140, 58), (104, 170, 66)]
    for i, y0 in enumerate((330, 362, 396, 430)):
        ys2 = ridge(W, y0, 12, 50 + i, rough=2, scale=0.5 + i * 0.2)
        fr(d, ys2, H, greens[i] + (255,))
        for x in range(0, W, 12):
            d.line(P([(x, ys2[x] + 6), (x + 8, ys2[x] + 6)]), fill=(255, 255, 255, 40), width=K)
    for x, h, lean, seed in ((90, 330, 0.12, 1), (240, 280, -0.1, 2), (1440, 340, -0.14, 3), (1530, 290, 0.08, 4)):
        palm(d, x, 430, h, (34, 70, 44, 255), lean, seed)
    # pura candi bentar kecil di tengah belakang
    stone = (150, 142, 124, 255)
    dk = (112, 106, 92, 255)
    for side in (-1, 1):
        x0 = 800 + side * 22
        for i, (wd, top) in enumerate([(70, 40), (58, 70), (46, 96), (34, 116), (22, 130)]):
            xa, xb = sorted((x0, x0 + side * wd))
            d.rectangle(P([(xa, 430 - top - 10), (xb, 430 - (0 if i == 0 else [40, 70, 96, 116][i - 1]) - 10)]), fill=stone if i % 2 == 0 else dk)
    # penjor: tiang bambu melengkung dengan hiasan
    for x, dirn in ((150, 1), (1450, -1)):
        pts = []
        for i in range(30):
            t = i / 29
            pts.append((x + dirn * (math.sin(t * 1.6) * 150 * t), 470 - t * 420))
        d.line(P(pts), fill=(206, 170, 74, 255), width=5 * K)
        for t in (0.55, 0.7, 0.85, 0.97):
            px, py = pts[int(t * 29)]
            d.ellipse(P([(px - 7, py), (px + 7, py + 30)]), fill=(250, 238, 196, 255))
            d.line(P([(px, py + 30), (px, py + 54)]), fill=(206, 170, 74, 255), width=2 * K)
    base = comp(base, lay)
    base = ground(base, 450, "#6B6A55", "#2A2B22")
    lay, d = layer()
    for yy in range(458, H, 34):
        d.line(P([(0, yy), (W, yy)]), fill=(30, 30, 22, 90), width=K)
    base = comp(base, lay)
    return base


# --------------------------------------------------------------------- Garuda
def garuda():
    base = vgrad(W, H, [(0, "#05070F"), (0.45, "#10172B"), (0.62, "#1B2440")])
    lay, d = layer()
    rng = np.random.default_rng(21)
    # tribun bertingkat
    for tier, (y0, y1) in enumerate(((170, 250), (250, 330), (330, 410))):
        shade = 24 + tier * 10
        d.rectangle(P([(0, y0), (W, y1)]), fill=(shade, shade + 6, shade + 22, 255))
        for _ in range(430):
            x = rng.uniform(0, W)
            y = rng.uniform(y0 + 6, y1 - 6)
            r = rng.uniform(4.5, 7)
            c = [(206, 60, 52), (238, 232, 220), (224, 168, 60), (96, 110, 150)][rng.integers(0, 4)]
            d.ellipse(P([(x - r, y - r), (x + r, y + r)]), fill=c + (255,))
            d.rectangle(P([(x - r * 1.1, y + r), (x + r * 1.1, y + r * 2.4)]), fill=tuple(int(v * 0.6) for v in c) + (255,))
    # bendera merah putih kecil di tribun
    for _ in range(46):
        x = rng.uniform(10, W - 10)
        y = rng.uniform(190, 400)
        d.line(P([(x, y), (x, y - 24)]), fill=(200, 190, 170, 255), width=K)
        d.rectangle(P([(x, y - 24), (x + 16, y - 18)]), fill=(206, 40, 38, 255))
        d.rectangle(P([(x, y - 18), (x + 16, y - 12)]), fill=(240, 236, 228, 255))
    # sayap garuda bergaya geometris di tengah belakang (bukan lambang negara)
    for side in (-1, 1):
        for i in range(7):
            ang = math.radians(-8 - i * 9)
            ln = 330 - i * 20
            x0, y0 = 800 + side * 40, 300 - i * 3
            x1 = x0 + side * math.cos(ang) * ln
            y1 = y0 + math.sin(ang) * ln * -0.55 * -1
            d.polygon(P([(x0, y0), (x1, y1 - 14), (x1 + side * 18, y1 + 10), (x0, y0 + 22)]), fill=(150, 30, 28, 235))
            d.line(P([(x0, y0), (x1, y1 - 14), (x1 + side * 18, y1 + 10), (x0, y0 + 22)]), fill=(227, 167, 47, 255), width=2 * K)
    base = comp(base, lay)
    base = ground(base, 410, "#202A44", "#0A0E1A")
    # lampu sorot
    for x, ang in ((200, 18), (500, 9), (1100, -9), (1400, -18)):
        lay, d = layer()
        a = math.radians(ang)
        tipx = x + math.tan(a) * 520
        d.polygon(P([(x - 8, 0), (x + 8, 0), (tipx + 120, 520), (tipx - 120, 520)]), fill=(255, 244, 206, 40))
        base = comp(base, lay)
        base = flare(base, x, 8, 90, "#FFF1C4", 0.9)
    lay, d = layer()
    for yy in range(420, H, 30):
        d.line(P([(0, yy), (W, yy)]), fill=(6, 8, 16, 100), width=K)
    base = comp(base, lay)
    return base


ARENAS = {
    "keraton": keraton, "toba": toba, "makassar": makassar,
    "betang": betang, "bali": bali, "garuda": garuda,
}

if __name__ == "__main__":
    for name, fn in ARENAS.items():
        img = fn()
        img = vignette(grain(img, 3.5, seed=len(name)), 0.38)
        path = os.path.join(OUT, f"{name}.webp")
        img.save(path, "WEBP", quality=74, method=6)
        print(name, img.size, os.path.getsize(path) // 1024, "KB")
