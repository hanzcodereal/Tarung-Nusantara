"""Pembantu menggambar untuk aset Tarung Nusantara (Pillow + NumPy)."""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

W, H = 1600, 720


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def vgrad(w, h, stops):
    """Gradien vertikal. stops = [(posisi 0..1, '#rrggbb'), ...]"""
    ys = np.linspace(0, 1, h)
    pos = [s[0] for s in stops]
    out = np.zeros((h, w, 3), dtype=np.float32)
    for c in range(3):
        vals = [hex_rgb(s[1])[c] for s in stops]
        col = np.interp(ys, pos, vals)
        out[:, :, c] = col[:, None]
    return Image.fromarray(out.astype(np.uint8), "RGB")


def glow(img, cx, cy, radius, color, strength=1.0):
    """Cahaya radial lembut yang ditambahkan ke gambar RGB."""
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / radius
    a = np.clip(1 - d, 0, 1) ** 2 * strength
    base = np.asarray(img).astype(np.float32)
    col = np.array(hex_rgb(color), dtype=np.float32)
    base = base + (col[None, None, :]) * a[:, :, None]
    return Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB")


def ridge(w, base_y, amp, seed, rough=3, scale=1.0):
    """Garis punggung bukit/gunung dari jumlah sinus dengan fase acak."""
    rng = np.random.default_rng(seed)
    xs = np.arange(w, dtype=np.float32)
    y = np.zeros(w, dtype=np.float32)
    for k in range(1, rough + 3):
        freq = k * 0.0035 * scale
        y += (amp / k) * np.sin(xs * freq * 2 * math.pi + rng.uniform(0, 6.28))
    return base_y - np.abs(y) if rough > 3 else base_y + y


def fill_ridge(draw, ys, bottom, color):
    pts = [(x, float(y)) for x, y in enumerate(ys)]
    pts += [(len(ys) - 1, bottom), (0, bottom)]
    draw.polygon(pts, fill=color)


def grain(img, amount=5, seed=1):
    rng = np.random.default_rng(seed)
    a = np.asarray(img).astype(np.float32)
    n = rng.normal(0, amount, a.shape[:2])[:, :, None]
    return Image.fromarray(np.clip(a + n, 0, 255).astype(np.uint8), "RGB")


def vignette(img, strength=0.45):
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.sqrt(((xx - w / 2) / (w / 2)) ** 2 + ((yy - h / 2) / (h / 2)) ** 2)
    m = 1 - np.clip(d - 0.55, 0, 1) * strength * 1.6
    a = np.asarray(img).astype(np.float32) * m[:, :, None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB")


def blur(img, r):
    return img.filter(ImageFilter.GaussianBlur(r))


def ss_draw(w, h, bg=(0, 0, 0, 0), k=2):
    """Kanvas RGBA supersampled; kembalikan (image, draw, k)."""
    im = Image.new("RGBA", (w * k, h * k), bg)
    return im, ImageDraw.Draw(im), k


def down(im, k):
    return im.resize((im.width // k, im.height // k), Image.LANCZOS)


def kawung(draw, cx, cy, r, fill, outline=None, width=2):
    """Pola kawung: empat elips membentuk bunga. r = jari-jari total."""
    a = r * 0.62
    b = r * 0.34
    for ang in (0, 90, 180, 270):
        rad = math.radians(ang)
        ex = cx + math.cos(rad) * r * 0.36
        ey = cy + math.sin(rad) * r * 0.36
        if ang in (0, 180):
            bbox = (ex - a / 2 * 1.15, ey - b, ex + a / 2 * 1.15, ey + b)
        else:
            bbox = (ex - b, ey - a / 2 * 1.15, ex + b, ey + a / 2 * 1.15)
        draw.ellipse(bbox, fill=fill, outline=outline, width=width)
    draw.ellipse((cx - r * 0.12, cy - r * 0.12, cx + r * 0.12, cy + r * 0.12), fill=outline or fill)
