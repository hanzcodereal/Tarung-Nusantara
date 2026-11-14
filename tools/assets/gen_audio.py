"""Sintesis musik daerah (7 lagu loop) dan efek suara Tarung Nusantara.

Semua suara dibuat dari nol dengan sintesis aditif dan filter (NumPy + SciPy),
lalu dikodekan ke OGG Vorbis mono dengan ffmpeg. Lagu-lagu adalah gubahan asli
bergaya instrumen daerah, bukan salinan lagu daerah yang ada.
"""
import os
import subprocess
import sys
import tempfile
import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 32000
OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(1945)


def T(d):
    return np.arange(int(d * SR), dtype=np.float64) / SR


def lp(x, f, order=2):
    return signal.sosfilt(signal.butter(order, f, "low", fs=SR, output="sos"), x)


def hp(x, f, order=2):
    return signal.sosfilt(signal.butter(order, f, "high", fs=SR, output="sos"), x)


def bp(x, lo, hi, order=2):
    return signal.sosfilt(signal.butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def noise(n):
    return rng.standard_normal(n)


def hz(root, semis):
    return root * 2.0 ** (semis / 12.0)


# ---------------------------------------------------------------- instrumen
def metallic(freq, dur, decay, partials=((1, 1.0), (2.76, 0.45), (5.4, 0.22), (8.93, 0.1)), detune=0.006, attack=0.003):
    t = T(dur)
    y = np.zeros_like(t)
    for r, a in partials:
        if freq * r > SR * 0.45:
            continue
        d = decay / (1 + 0.55 * (r - 1))
        y += a * np.exp(-t / d) * (np.sin(2 * np.pi * freq * r * t) + 0.6 * np.sin(2 * np.pi * freq * (1 + detune) * r * t))
    return y * np.minimum(1, t / attack) / 1.8


def gong(freq, dur=3.6, decay=1.7):
    parts = ((1, 1.0), (1.47, 0.6), (2.09, 0.5), (2.56, 0.35), (3.24, 0.25), (4.07, 0.14))
    y = metallic(freq, dur, decay, parts, 0.004, 0.012)
    t = T(dur)
    y += 0.5 * np.sin(2 * np.pi * freq * 0.5 * t) * np.exp(-t / (decay * 1.2))
    y += lp(noise(len(t)), 900) * np.exp(-t / 0.05) * 0.8
    return y


def drum(f0=160.0, dur=0.35, decay=0.09, tone=1.0, click=0.35):
    t = T(dur)
    fenv = f0 * (1 + 1.1 * np.exp(-t / 0.025))
    ph = 2 * np.pi * np.cumsum(fenv) / SR
    y = tone * np.sin(ph) * np.exp(-t / decay)
    y += click * bp(noise(len(t)), 400, 3500) * np.exp(-t / 0.018)
    return y


def tak(dur=0.12, lo=1500, hi=5000, decay=0.03):
    t = T(dur)
    return bp(noise(len(t)), lo, hi) * np.exp(-t / decay) * 1.4


def pluck(freq, dur=1.2, decay=0.5, bright=1.0):
    t = T(dur)
    y = np.zeros_like(t)
    for h in range(1, 15):
        if freq * h > SR * 0.42:
            break
        y += (1.0 / h ** 1.1) * np.exp(-t * (0.6 + 0.35 * h * bright) / decay) * np.sin(2 * np.pi * freq * h * (1 + 0.0004 * h * h) * t)
    return y * np.minimum(1, t / 0.002) * 0.9


def suling(freq, dur, vib=5.2, breath=0.1):
    t = T(dur)
    v = 1 + 0.005 * np.sin(2 * np.pi * vib * t) * np.minimum(1, t / 0.25)
    ph = 2 * np.pi * np.cumsum(freq * v) / SR
    y = np.sin(ph) + 0.26 * np.sin(2 * ph) + 0.09 * np.sin(3 * ph)
    y += breath * bp(noise(len(t)), freq * 0.9, min(freq * 3, SR * 0.45)) * 3
    env = np.minimum(1, t / 0.05) * np.clip((dur - t) / 0.09, 0, 1)
    return y * env * 0.7


def saw_pad(freq, dur, attack=0.5, rel=0.6, voices=(0.0, -0.004, 0.004), harm=10, cut=2400):
    t = T(dur)
    y = np.zeros_like(t)
    for dt in voices:
        for h in range(1, harm + 1):
            if freq * h > SR * 0.4:
                break
            y += np.sin(2 * np.pi * freq * (1 + dt) * h * t + dt * 90) / h
    env = np.minimum(1, t / attack) * np.clip((dur - t) / rel, 0, 1)
    return lp(y, cut) * env * 0.35


def brass(freq, dur, attack=0.05, rel=0.12):
    t = T(dur)
    v = 1 + 0.004 * np.sin(2 * np.pi * 5.5 * t) * np.minimum(1, t / 0.3)
    ph = 2 * np.pi * np.cumsum(freq * v) / SR
    y = np.zeros_like(t)
    for h in range(1, 13):
        if freq * h > SR * 0.4:
            break
        y += np.sin(h * ph) / h ** 0.9
    sweep = 900 + 2800 * np.minimum(1, t / 0.18)
    env = np.minimum(1, t / attack) * np.clip((dur - t) / rel, 0, 1)
    return lp(y, 3200) * env * 0.4


def reed(freq, dur):
    t = T(dur)
    v = 1 + 0.007 * np.sin(2 * np.pi * 6 * t) * np.minimum(1, t / 0.15)
    ph = 2 * np.pi * np.cumsum(freq * v) / SR
    y = np.sign(np.sin(ph)) * 0.5 + np.sin(ph) * 0.5
    y = bp(y, 700, 2600)
    env = np.minimum(1, t / 0.02) * np.clip((dur - t) / 0.05, 0, 1)
    return y * env * 1.1


def shaker(dur=0.09):
    t = T(dur)
    return hp(noise(len(t)), 5000) * np.exp(-t / 0.025) * 0.5


# --------------------------------------------------------------------- trek
class Track:
    def __init__(self, seconds):
        self.n = int(seconds * SR)
        self.buf = np.zeros(self.n, dtype=np.float64)

    def add(self, s, at, gain=1.0):
        i = int(round(at * SR)) % self.n
        s = s * gain
        end = i + len(s)
        if end <= self.n:
            self.buf[i:end] += s
        else:
            k = self.n - i
            self.buf[i:] += s[:k]
            self.buf[: end - self.n] += s[k:]


def finalize(buf, target_rms_db=-19.0, peak_db=-1.5):
    x = buf - np.mean(buf)
    rms = np.sqrt(np.mean(x ** 2)) + 1e-9
    x = x * (10 ** (target_rms_db / 20) / rms)
    x = np.tanh(x * 1.1) / 1.1
    peak = np.max(np.abs(x)) + 1e-9
    lim = 10 ** (peak_db / 20)
    if peak > lim:
        x = x * (lim / peak)
    return x


def save_ogg(x, name, q=3):
    wav = tempfile.mktemp(suffix=".wav")
    wavfile.write(wav, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
    out = os.path.join(OUT, name)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", str(q), out], check=True)
    os.remove(wav)
    print(f"{name:14s} {len(x) / SR:5.1f}s {os.path.getsize(out) // 1024:4d} KB")


# ------------------------------------------------------------------ gamelan
SLENDRO = [0, 2.4, 4.8, 7.2, 9.6]
PELOG = [0, 1.1, 3.0, 7.0, 8.0]


def deg(scale, d, root, octave=0):
    o, i = divmod(d, len(scale))
    return hz(root, scale[i] + 12 * (o + octave))


def gamelan(bpm, bars, scale, root, balungan, density=2, kotekan=False, ceng=False, gong_every=16, bowed=False, kendang_vol=0.6, bright=1.0):
    beat = 60.0 / bpm
    total = bars * 4 * beat
    tr = Track(total)
    nb = bars * 4
    for b in range(nb):
        t0 = b * beat
        d = balungan[b % len(balungan)]
        tr.add(metallic(deg(scale, d, root), 2.2, 1.3 * bright), t0, 0.9)
        tr.add(metallic(deg(scale, d, root, -1), 2.6, 1.6), t0, 0.5)
        nxt = balungan[(b + 1) % len(balungan)]
        steps = density * (2 if kotekan else 1)
        for s in range(steps):
            frac = s / steps
            pick = d if s % 2 == 0 else (nxt if s == steps - 1 else d + (1 if (b + s) % 3 == 0 else -1))
            vol = 0.42 if s % 2 == 0 else 0.3
            if kotekan:
                tr.add(metallic(deg(scale, pick + (s % 2), root, 1), 0.7, 0.45 * bright), t0 + frac * beat, vol)
            else:
                tr.add(metallic(deg(scale, pick, root, 1), 0.9, 0.55 * bright), t0 + frac * beat, vol)
        if b % 4 == 3:
            tr.add(metallic(deg(scale, d, root, -1) * 0.5, 3.5, 2.2, ((1, 1), (2.0, 0.3))), t0, 0.7)
        if b % 2 == 1:
            tr.add(gong(hz(root, -14), 1.4, 0.55), t0 + beat * 0.5, 0.18)
        if b % gong_every == gong_every - 1:
            tr.add(gong(hz(root, -26), 4.5, 2.2), t0 + beat * 0.98, 1.0)
        tr.add(drum(110, 0.3, 0.1, 1.0), t0 + beat * 0.0, kendang_vol * (0.9 if b % 2 == 0 else 0.5))
        tr.add(tak(), t0 + beat * 0.5, kendang_vol * 0.5)
        if b % 4 in (2, 3):
            tr.add(tak(0.1, 2200, 6000, 0.025), t0 + beat * 0.75, kendang_vol * 0.35)
        if ceng:
            tr.add(shaker(), t0 + beat * 0.25, 0.5)
            tr.add(shaker(), t0 + beat * 0.75, 0.5)
    if bowed:
        for b in range(0, nb, 4):
            d = balungan[b % len(balungan)]
            tr.add(saw_pad(deg(scale, d, root, 0), 4 * beat * 0.95, 0.6, 0.8, harm=7, cut=1500), b * beat, 0.5)
    return tr.buf


# -------------------------------------------------------------------- lagu
def song_menu():
    bal = [2, 2, 3, 2, 0, 0, 1, 0, 3, 3, 4, 3, 1, 1, 2, 1, 2, 3, 4, 3, 2, 1, 0, 1, 4, 4, 3, 4, 2, 2, 1, 2]
    return gamelan(72, 16, SLENDRO, 261.6, bal, density=2, bowed=True)


def song_keraton():
    bal = [0, 1, 2, 1, 4, 3, 2, 3, 0, 1, 3, 2, 4, 4, 3, 4, 1, 2, 3, 2, 0, 0, 1, 0, 3, 4, 3, 2, 1, 2, 1, 0]
    return gamelan(64, 16, SLENDRO, 246.9, bal, density=1, bowed=True, kendang_vol=0.5, bright=1.15)


def song_bali():
    bal = [0, 2, 1, 3, 2, 4, 3, 1, 0, 2, 4, 3, 1, 2, 0, 1, 3, 4, 2, 3, 4, 3, 1, 2, 0, 1, 2, 4, 3, 2, 1, 0]
    return gamelan(148, 40, PELOG, 293.7, bal, density=2, kotekan=True, ceng=True, gong_every=16, kendang_vol=0.8, bright=0.8)


def song_toba():
    bpm = 96
    beat = 60 / bpm
    bars = 28
    tr = Track(bars * 4 * beat)
    pent = [0, 2, 4, 7, 9]
    root = 293.7
    mel = [4, 3, 2, 3, 4, 4, 2, 0, 3, 3, 2, 1, 2, 3, 2, 0, 4, 5, 4, 3, 2, 3, 4, 2, 3, 2, 1, 0, 1, 2, 0, 0]
    tag = [190, 230, 270, 320, 380]
    for b in range(bars * 4):
        t0 = b * beat
        for s in range(3):
            tm = t0 + s * beat / 3
            tr.add(drum(tag[(b + s * 2) % 5] * 0.6, 0.35, 0.11, 1.0, 0.45), tm, 0.5 if s == 0 else 0.3)
        if b % 2 == 0:
            tr.add(tak(0.1, 1800, 5500, 0.03), t0 + beat * 2 / 3, 0.45)
        if b % 8 == 0:
            tr.add(gong(hz(root, -24), 3.5, 1.8), t0, 0.55)
        if b % 4 == 0:
            tr.add(metallic(hz(root, -12), 1.6, 1.1, ((1, 1), (2.0, 0.5), (3.0, 0.2))), t0, 0.35)
    for i in range(bars * 2):
        o, k = divmod(mel[i % len(mel)], 5)
        f = hz(root, pent[k] + 12 * (o + 1))
        t0 = i * 2 * beat
        tr.add(suling(f, 1.8 * beat), t0, 0.8)
        if i % 2 == 1:
            tr.add(suling(hz(f, 7), 1.0 * beat), t0 + beat, 0.28)
    return tr.buf


def song_makassar():
    bpm = 132
    beat = 60 / bpm
    bars = 36
    tr = Track(bars * 4 * beat)
    pent = [0, 2, 5, 7, 10]
    root = 220.0
    arp = [0, 2, 4, 2, 3, 2, 1, 2]
    prog = [0, 0, 3, 4, 0, 0, 4, 3]
    mel = [[4, 3, 4, 6, 4, 3, 2, 0], [2, 3, 4, 3, 2, 1, 0, 0], [4, 6, 7, 6, 4, 3, 4, 2], [3, 2, 3, 4, 2, 1, 0, 0]]
    for b in range(bars):
        base = prog[b % len(prog)]
        for s in range(8):
            tm = (b * 4 + s * 0.5) * beat
            d = base + arp[s]
            o, k = divmod(d, 5)
            tr.add(pluck(hz(root, pent[k] + 12 * o), 1.1, 0.55), tm, 0.55 if s % 2 == 0 else 0.4)
        for s in range(4):
            tm = (b * 4 + s) * beat
            tr.add(drum(135, 0.3, 0.09, 1.0, 0.4), tm, 0.7 if s % 2 == 0 else 0.45)
            tr.add(tak(0.09, 1700, 5200, 0.025), tm + beat * 0.5, 0.5)
            if s == 3:
                tr.add(tak(0.09, 1700, 5200, 0.025), tm + beat * 0.25, 0.4)
                tr.add(tak(0.09, 1700, 5200, 0.025), tm + beat * 0.75, 0.4)
        if b % 4 == 0:
            tr.add(gong(hz(root, -24), 3.0, 1.4), b * 4 * beat, 0.45)
        if b >= 8 and b % 8 < 6:
            phrase = mel[(b // 2) % len(mel)]
            for s, m in enumerate(phrase):
                o, k = divmod(m, 5)
                tr.add(reed(hz(root, pent[k] + 12 * (o + 1)), beat * 0.45), (b * 4 + (s % 8) * 0.5) * beat, 0.32)
    return tr.buf


def song_betang():
    bpm = 84
    beat = 60 / bpm
    bars = 22
    tr = Track(bars * 4 * beat)
    pent = [0, 3, 5, 7, 10]
    root = 164.8
    mel = [4, 3, 2, 3, 2, 0, 1, 0, 3, 4, 5, 4, 3, 2, 0, 0, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0, 1, 2, 0, 0, 0]
    for b in range(bars * 4):
        t0 = b * beat
        tr.add(pluck(root, 1.6, 0.9), t0, 0.5)
        tr.add(pluck(root * 1.5, 1.2, 0.8), t0 + beat * 0.5, 0.28)
        tr.add(drum(70, 0.5, 0.16, 1.2, 0.2), t0, 0.7 if b % 2 == 0 else 0.4)
        tr.add(shaker(), t0 + beat * 0.5, 0.4)
        tr.add(shaker(), t0 + beat * 0.75, 0.3)
        if b % 8 == 0:
            tr.add(gong(hz(root, -12), 4.2, 2.2), t0, 0.7)
        if b % 8 == 4:
            tr.add(gong(hz(root, -5), 3.0, 1.6), t0, 0.4)
    for i in range(bars * 2):
        o, k = divmod(mel[i % len(mel)], 5)
        f = hz(root, pent[k] + 12 * (o + 1))
        t0 = i * 2 * beat
        tr.add(pluck(f, 1.6, 0.8, 0.8), t0, 0.75)
        tr.add(pluck(hz(f, 12), 0.8, 0.4), t0 + beat * 0.5, 0.18)
        if i % 4 == 3:
            tr.add(pluck(hz(f, -5), 1.2, 0.6), t0 + beat * 1.5, 0.45)
    return tr.buf


def song_garuda():
    bpm = 108
    beat = 60 / bpm
    bars = 28
    tr = Track(bars * 4 * beat)
    root = 146.8
    chords = [(0, 3, 7), (-4, 0, 3), (3, 7, 10), (-2, 2, 5)]
    theme = [0, 3, 7, 5, 3, 7, 10, 7, 5, 3, 0, 3, 5, 7, 5, 3]
    for b in range(bars):
        t0 = b * 4 * beat
        ch = chords[(b // 2) % 4]
        for i, semi in enumerate(ch):
            tr.add(saw_pad(hz(root, semi + 12), 4 * beat, 0.7, 0.8), t0, 0.5)
        tr.add(saw_pad(hz(root, ch[0]), 4 * beat, 0.4, 0.6, harm=6, cut=800), t0, 0.7)
        for s in range(4):
            tr.add(drum(95, 0.45, 0.14, 1.2, 0.3), t0 + s * beat, 0.8 if s in (0, 2) else 0.35)
        if b % 2 == 1:
            for s in range(4):
                tr.add(drum(120, 0.25, 0.07, 0.9, 0.3), t0 + 3 * beat + s * beat / 4, 0.28)
        if b % 4 == 0:
            tr.add(gong(hz(root, -12), 4.0, 2.0), t0, 0.6)
        if b >= 4:
            for s in range(4):
                semi = theme[(b * 4 + s) % len(theme)]
                tr.add(brass(hz(root, semi + 24), beat * (0.9 if s % 2 == 0 else 0.45)), t0 + s * beat, 0.7)
                tr.add(metallic(hz(root, semi + 36), 1.0, 0.5), t0 + s * beat, 0.25)
    return tr.buf


def sting_splash():
    tr = Track(7.0)
    for i, d in enumerate([0, 1, 2, 3, 4, 3, 4]):
        tr.add(metallic(deg(SLENDRO, d, 261.6, 1), 1.4, 0.8), 0.7 + i * 0.4, 0.8)
    tr.add(gong(hz(130.8, -12), 6.0, 2.6), 0.0, 1.0)
    tr.add(gong(hz(130.8, -12), 4.0, 2.0), 3.8, 0.8)
    tr.add(drum(100, 0.4, 0.12, 1.0), 3.8, 0.8)
    return tr.buf


# --------------------------------------------------------------------- sfx
def sfx_punch(f0, dur, decay, crack, tone=1.0, lowcut=700):
    t = T(dur)
    y = drum(f0, dur, decay, tone, 0.0)
    y += crack * lp(noise(len(t)), lowcut) * np.exp(-t / 0.04)
    y += 0.4 * hp(noise(len(t)), 2500) * np.exp(-t / 0.012)
    return y


def sfx_whoosh(dur=0.22, f0=500, f1=2500):
    t = T(dur)
    n = noise(len(t))
    out = np.zeros_like(t)
    seg = 8
    sz = len(t) // seg
    for i in range(seg):
        fc = f0 + (f1 - f0) * (i / (seg - 1))
        out[i * sz:(i + 1) * sz] = bp(n[i * sz:(i + 1) * sz], fc * 0.6, min(fc * 1.6, SR * 0.45))
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 1.5
    return out * env * 1.6


def sfx_crowd(dur=2.0, level=1.0):
    t = T(dur)
    n = bp(noise(len(t)), 500, 2600)
    am = 0.55 + 0.45 * np.abs(lp(noise(len(t)), 14, 1))
    env = np.minimum(1, t / 0.2) * np.clip((dur - t) / 0.6, 0, 1)
    return n * am * env * level * 5


def sfx_dizzy():
    parts = []
    for f in (740, 660, 590, 520):
        t = T(0.14)
        v = 1 + 0.02 * np.sin(2 * np.pi * 14 * t)
        parts.append(np.sin(2 * np.pi * np.cumsum(f * v) / SR) * np.exp(-t / 0.09))
    return np.concatenate(parts) * 0.6


def sfx_coin():
    a = metallic(1568, 0.5, 0.18, ((1, 1), (2.0, 0.4), (3.0, 0.2)), 0.002)
    b = metallic(2093, 0.7, 0.25, ((1, 1), (2.0, 0.4), (3.0, 0.2)), 0.002)
    out = np.zeros(int(0.8 * SR))
    out[: len(a)] += a
    k = int(0.09 * SR)
    out[k:k + len(b)] += b
    return out * 0.7


def sfx_unlock():
    out = np.zeros(int(1.6 * SR))
    for i, d in enumerate([0, 1, 2, 3, 4]):
        s = metallic(deg(SLENDRO, d, 392.0, 0), 1.0, 0.5)
        k = int(i * 0.11 * SR)
        out[k:k + len(s)] += s
    s = gong(196.0, 1.2, 0.6)
    out[: len(s)] += s * 0.6
    return out * 0.7


def sfx_click():
    t = T(0.05)
    return (np.sin(2 * np.pi * 1300 * t) * np.exp(-t / 0.01) + 0.3 * tak(0.05, 2000, 6000, 0.01)[: len(t)]) * 0.8


def sfx_tick():
    t = T(0.09)
    return np.sin(2 * np.pi * 880 * t) * np.exp(-t / 0.03) * 0.9


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[: len(p)] += p
    return out


SFX = {
    "jab": lambda: sfx_punch(190, 0.16, 0.05, 0.6),
    "hook": lambda: sfx_punch(140, 0.24, 0.08, 0.9),
    "upper": lambda: sfx_punch(105, 0.32, 0.11, 1.1),
    "special": lambda: mix(sfx_punch(80, 0.5, 0.18, 1.4, 1.3, 1100), 0.5 * gong(180, 0.8, 0.3)),
    "block": lambda: mix(drum(260, 0.14, 0.04, 0.8, 0.9), 0.4 * tak(0.1, 800, 2500, 0.03)),
    "perfect": lambda: metallic(1046, 0.9, 0.4, ((1, 1), (2.76, 0.6), (5.4, 0.35), (8.9, 0.2)), 0.003),
    "whoosh": lambda: sfx_whoosh(0.2, 500, 2800),
    "dodge": lambda: sfx_whoosh(0.28, 1800, 500),
    "ko": lambda: mix(gong(110, 3.2, 1.6), drum(60, 0.8, 0.25, 1.4, 0.2)),
    "bell": lambda: mix(gong(392, 2.2, 1.0), 0.5 * metallic(784, 1.6, 0.6, ((1, 1), (2.7, 0.3)), 0.005)),
    "cheer": lambda: sfx_crowd(2.2, 1.0),
    "dizzy": sfx_dizzy,
    "down": lambda: mix(drum(55, 0.7, 0.22, 1.5, 0.25), 0.3 * lp(noise(int(0.7 * SR)), 300) * np.exp(-T(0.7) / 0.1)),
    "click": sfx_click,
    "coin": sfx_coin,
    "unlock": sfx_unlock,
    "tick": sfx_tick,
}


def save_sfx(name, x, peak_db=-3.0):
    x = np.asarray(x, dtype=np.float64)
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9) * 10 ** (peak_db / 20)
    n = len(x)
    fade = int(0.012 * SR)
    x[-fade:] *= np.linspace(1, 0, fade)
    save_ogg(x, name + ".ogg", q=4)


if __name__ == "__main__":
    songs = {
        "m_menu": song_menu, "m_keraton": song_keraton, "m_toba": song_toba,
        "m_makassar": song_makassar, "m_betang": song_betang, "m_bali": song_bali,
        "m_garuda": song_garuda,
    }
    for name, fn in songs.items():
        save_ogg(finalize(fn()), name + ".ogg", q=3)
    save_ogg(finalize(sting_splash(), -17.0), "s_splash.ogg", q=4)
    for name, fn in SFX.items():
        x = fn()
        save_sfx("x_" + name, x)
