# -*- coding: utf-8 -*-
"""
蜀漆云馆 · 种子图片生成脚本
生成与展馆视觉（朱漆 / 黑漆 / 描金 / 宣纸）一致的占位插画，
用于替换早期演示数据里的随机图片（picsum.photos）。

用法：python generate_seed_images.py <输出目录>
默认输出到 qi/src/main/resources/static/seed/
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

FONT_KAI = "C:/Windows/Fonts/simkai.ttf"
FONT_SONG = "C:/Windows/Fonts/simsun.ttc"
FONT_HEI = "C:/Windows/Fonts/simhei.ttf"

INK = (18, 13, 11)
LACQUER = (38, 18, 15)
CINNABAR = (118, 30, 23)
VERMILION = (158, 46, 33)
GOLD = (208, 170, 108)
GOLD_BRIGHT = (235, 208, 152)
CREAM = (241, 228, 205)
JADE = (94, 118, 100)

S = 2  # 超采样倍数


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def gradient_bg(w, h, top, bottom):
    y = np.linspace(0, 1, h)[:, None, None]
    arr = np.array(top, dtype=np.float32) * (1 - y) + np.array(bottom, dtype=np.float32) * y
    arr = np.repeat(arr, w, axis=1)
    return Image.fromarray(arr.astype(np.uint8), "RGB")


def add_glow(img, cx, cy, rx, ry, color, alpha=90):
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w]
    d = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
    m = np.clip(1 - d, 0, 1) ** 2 * alpha
    layer = np.zeros((h, w, 3), dtype=np.float32)
    layer[:] = color
    base = np.asarray(img, dtype=np.float32)
    out = base * (1 - m[..., None] / 255.0) + layer * (m[..., None] / 255.0)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB")


def add_noise(img, amp=6):
    arr = np.asarray(img, dtype=np.float32)
    noise = np.random.default_rng(7).normal(0, amp, arr.shape[:2])[..., None]
    return Image.fromarray(np.clip(arr + noise, 0, 255).astype(np.uint8), "RGB")


def vignette(img, strength=0.55):
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w]
    nx = (xx - w / 2) / (w / 2)
    ny = (yy - h / 2) / (h / 2)
    r = np.sqrt(nx ** 2 + ny ** 2) / math.sqrt(2)
    mask = (1 - strength * r ** 2.2)[..., None]
    arr = np.asarray(img, dtype=np.float32) * mask
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB")


def paper_texture(img, alpha=10):
    """细密纸纹/漆面颗粒"""
    arr = np.asarray(img, dtype=np.float32)
    rng = np.random.default_rng(11)
    coarse = rng.normal(0, 1, (img.size[1] // 8 + 1, img.size[0] // 8 + 1))
    coarse = np.asarray(Image.fromarray(((coarse * 0.5 + 0.5) * 255).astype(np.uint8)).resize(img.size, Image.BICUBIC), dtype=np.float32)
    arr = arr * (1 - alpha / 255.0) + coarse[..., None] * (alpha / 255.0)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB")


def base_canvas(w, h, glow_color=VERMILION):
    img = gradient_bg(w, h, (52, 22, 18), (14, 10, 9))
    img = add_glow(img, w * 0.32, h * 0.30, w * 0.62, h * 0.66, glow_color, 82)
    img = add_glow(img, w * 0.80, h * 0.90, w * 0.42, h * 0.38, (70, 30, 20), 60)
    img = paper_texture(img, 12)
    img = add_noise(img, 5)
    return img


def frame(d, w, h, margin=0.055, color=GOLD, alpha=190):
    m = int(min(w, h) * margin)
    c = color + (alpha,)
    d.rounded_rectangle([m, m, w - m, h - m], radius=int(min(w, h) * 0.012), outline=c, width=max(2, S * 1))
    m2 = m + S * 8
    d.rounded_rectangle([m2, m2, w - m2, h - m2], radius=int(min(w, h) * 0.009),
                        outline=color + (int(alpha * 0.55),), width=max(1, S // 2))
    # 四角短线
    L = int(min(w, h) * 0.035)
    off = m + S * 18
    for (x1, y1, x2, y2) in [
        (off, off, off + L, off), (off, off, off, off + L),
        (w - off, off, w - off - L, off), (w - off, off, w - off, off + L),
        (off, h - off, off + L, h - off), (off, h - off, off, h - off - L),
        (w - off, h - off, w - off - L, h - off), (w - off, h - off, w - off, h - off - L),
    ]:
        d.line([(x1, y1), (x2, y2)], fill=color + (120,), width=S)


def load_font(path, size):
    return ImageFont.truetype(path, size)


def vtext(d, text, cx, top, font, fill, gap=None):
    """竖排文字（逐字居中）"""
    gap = gap or int(font.size * 1.25)
    y = top
    for ch in text:
        w = d.textlength(ch, font=font)
        d.text((cx - w / 2, y), ch, font=font, fill=fill)
        y += gap


def seal(d, x, y, size, ch, color=GOLD_BRIGHT):
    d.rectangle([x, y, x + size, y + size], outline=color, width=S * 2)
    f = load_font(FONT_SONG, int(size * 0.6))
    w = d.textlength(ch, font=f)
    d.text((x + (size - w) / 2, y + size * 0.16), ch, font=f, fill=color)


def spiral(d, cx, cy, r, turns=2.6, color=GOLD, width=4, phase=0.0):
    pts = []
    n = 90
    for i in range(n):
        t = i / (n - 1)
        ang = phase + t * turns * 2 * math.pi
        rr = r * (1 - 0.82 * t)
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    pts.append((cx + r * 0.18 * math.cos(phase), cy + r * 0.18 * math.sin(phase)))
    d.line(pts, fill=color, width=width, joint="curve")


def leaf(d, cx, cy, ln, ang, color, width=3):
    pts = []
    for i in range(16):
        t = i / 15
        w = math.sin(t * math.pi) * ln * 0.34
        x = cx + math.cos(ang) * (t - 0.5) * ln - math.sin(ang) * w
        y = cy + math.sin(ang) * (t - 0.5) * ln + math.cos(ang) * w
        pts.append((x, y))
    for i in range(15, -1, -1):
        t = i / 15
        w = math.sin(t * math.pi) * ln * 0.34
        x = cx + math.cos(ang) * (t - 0.5) * ln + math.sin(ang) * w
        y = cy + math.sin(ang) * (t - 0.5) * ln - math.cos(ang) * w
        pts.append((x, y))
    d.line(pts + [pts[0]], fill=color, width=width, joint="curve")


def cloud_band(d, x0, x1, y, scale, color=GOLD, width=4, flip=False):
    """一排云纹（螺旋 + 波带）"""
    n = max(2, int((x1 - x0) / (scale * 2.4)))
    for i in range(n + 1):
        x = x0 + (x1 - x0) * i / n
        yy = y + (math.sin(i * 1.7) * scale * 0.18) * (-1 if flip else 1)
        spiral(d, x, yy, scale, 2.2, color, width, phase=0.6 * i)
    pts = []
    for i in range(60):
        t = i / 59
        pts.append((x0 + (x1 - x0) * t, y + math.sin(t * math.pi * 2) * scale * 0.22 * (-1 if flip else 1)))
    d.line(pts, fill=color + (150,), width=max(2, width - 2), joint="curve")


def bezier(p0, p1, p2, n=24):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts


def cubic(p0, p1, p2, p3, n=36):
    pts = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        x = u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0]
        y = u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1]
        pts.append((x, y))
    return pts


def lacquer_tree(d, cx, base_y, height, scale, color=GOLD, width=5, drops=True):
    """漆树：S 形主干 + 上扬枝条 + 枝头叶 + 割口漆滴"""
    top_y = base_y - height
    trunk = cubic((cx, base_y),
                  (cx + scale * 0.26, base_y - height * 0.32),
                  (cx - scale * 0.26, base_y - height * 0.68),
                  (cx + scale * 0.06, top_y), 40)
    d.line(trunk, fill=color, width=width, joint="curve")

    # (沿主干位置, 方向, 横向伸展, 上扬)
    branches = [
        (0.22, -1, 0.62, 0.30), (0.34, 1, 0.56, 0.26),
        (0.47, -1, 0.68, 0.28), (0.60, 1, 0.54, 0.24),
        (0.72, -1, 0.60, 0.26), (0.82, 1, 0.44, 0.20),
    ]
    for t, side, reach, rise in branches:
        p0 = trunk[int(t * (len(trunk) - 1))]
        p2 = (p0[0] + side * scale * reach, p0[1] - scale * rise)
        p1 = (p0[0] + side * scale * reach * 0.62, p0[1] - scale * rise * 0.15)
        d.line(bezier(p0, p1, p2, 20), fill=color, width=max(2, width - 2), joint="curve")
        # 枝头小叶
        for k in (-1, 0, 1):
            lx = p2[0] + side * scale * 0.030 + k * scale * 0.045
            ly = p2[1] - scale * 0.030 + abs(k) * scale * 0.014
            r = scale * 0.036
            d.ellipse([lx - r, ly - r * 0.55, lx + r, ly + r * 0.55],
                      fill=(color[0], color[1], color[2], 190))

    if drops:
        # 割口：树干上的 V 形刀口 + 下方漆滴
        for t in (0.28, 0.52, 0.70):
            p = trunk[int(t * (len(trunk) - 1))]
            r = scale * 0.060
            d.line([(p[0] - r * 0.75, p[1] - r * 0.20), (p[0], p[1] + r * 0.55), (p[0] + r * 0.75, p[1] - r * 0.20)],
                   fill=CREAM, width=max(2, width - 1), joint="curve")
            dx, dy = p[0] + scale * 0.015, p[1] + scale * 0.075
            d.polygon([(dx - scale * 0.014, dy), (dx + scale * 0.014, dy), (dx, dy + scale * 0.050)],
                      fill=GOLD_BRIGHT)
            d.ellipse([dx - scale * 0.018, dy + scale * 0.040, dx + scale * 0.018, dy + scale * 0.072],
                      fill=GOLD_BRIGHT)
    return trunk


def bowl(d, cx, cy, w, h, color=GOLD, width=5):
    """侧视漆碗：碗沿椭圆 + 碗身弧线 + 圈足"""
    rx = w / 2
    ry = h * 0.20
    dep = h * 0.52
    d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], outline=color, width=width)
    pts = []
    for i in range(41):
        t = i / 40
        pts.append((cx - rx + 2 * rx * t, cy + math.sin(math.pi * t) * dep))
    d.line(pts, fill=color, width=width, joint="curve")
    d.line([(cx - w * 0.11, cy + dep * 0.86), (cx + w * 0.11, cy + dep * 0.86)], fill=color, width=width)
    d.line([(cx - w * 0.085, cy + dep * 0.86), (cx - w * 0.085, cy + dep * 0.98)], fill=color, width=max(2, width - 2))
    d.line([(cx + w * 0.085, cy + dep * 0.86), (cx + w * 0.085, cy + dep * 0.98)], fill=color, width=max(2, width - 2))


def box_shape(d, x0, y0, x1, y1, color=GOLD, width=5, pattern=True):
    d.rounded_rectangle([x0, y0, x1, y1], radius=(y1 - y0) * 0.10, outline=color, width=width)
    lid = y0 + (y1 - y0) * 0.30
    d.line([(x0 + width, lid), (x1 - width, lid)], fill=color, width=width)
    d.rounded_rectangle([x0 + width * 3, y0 + width * 3, x1 - width * 3, lid - width * 2],
                        radius=(y1 - y0) * 0.06, outline=color + (140,), width=max(2, width - 2))
    if pattern:
        cx, cy = (x0 + x1) / 2, (y0 + lid) / 2
        spiral(d, cx - (x1 - x0) * 0.14, cy, (x1 - x0) * 0.09, 2.0, GOLD, max(2, width - 2))
        spiral(d, cx + (x1 - x0) * 0.14, cy, (x1 - x0) * 0.09, 2.0, GOLD, max(2, width - 2), phase=math.pi)


def phoenix(d, cx, cy, size, color=GOLD, width=5):
    """写意凤鸟：S 形身 + 冠 + 长尾"""
    body = []
    for i in range(50):
        t = i / 49
        x = cx - size * 0.30 + size * 0.62 * t
        y = cy - math.sin(t * math.pi) * size * 0.26 + t * size * 0.08
        body.append((x, y))
    d.line(body, fill=color, width=width, joint="curve")
    # 头
    hx, hy = body[-1]
    d.ellipse([hx - size * 0.030, hy - size * 0.030, hx + size * 0.030, hy + size * 0.030], outline=color, width=width)
    d.polygon([(hx + size * 0.024, hy - size * 0.008), (hx + size * 0.062, hy + size * 0.006), (hx + size * 0.024, hy + size * 0.020)], fill=color)
    d.ellipse([hx - size * 0.008, hy - size * 0.006, hx + size * 0.004, hy + size * 0.006], fill=color)
    for k in range(2):
        d.arc([hx - size * 0.055 - k * size * 0.015, hy - size * 0.095 - k * size * 0.022,
               hx + size * 0.035, hy - size * 0.015 - k * size * 0.008], 300, 360, fill=color, width=max(2, width - 2))
    # 翼
    for k in range(2):
        d.arc([cx - size * 0.22 + k * size * 0.045, cy - size * 0.22 + k * size * 0.04,
               cx + size * 0.06 - k * size * 0.045, cy + size * 0.06 - k * size * 0.02],
              290, 70, fill=color + (210,), width=max(2, width - 1))
    # 尾羽（从身体前段向下后方舒展，控制在画面内）
    base_pt = body[4]
    for k, spread in enumerate([0.10, 0.18, 0.26]):
        pts = []
        for i in range(36):
            t = i / 35
            x = base_pt[0] - t * size * 0.30
            y = base_pt[1] - math.sin(t * math.pi * 0.8) * size * spread + t * size * (0.10 + k * 0.07)
            pts.append((x, y))
        d.line(pts, fill=color, width=max(2, width - 1), joint="curve")
        for j in range(1, 5):
            px, py = pts[int(j * 7)]
            d.line([(px, py), (px - size * 0.045, py + size * 0.038)], fill=color + (170,), width=max(1, width - 2))


def brush_stroke(d, x0, y0, x1, y1, thick, color):
    """带粗细变化的笔触（贝塞尔近似）"""
    pts = []
    for i in range(40):
        t = i / 39
        x = x0 + (x1 - x0) * t + math.sin(t * math.pi) * thick * 1.2
        y = y0 + (y1 - y0) * t - math.sin(t * math.pi) * thick * 1.6
        pts.append((x, y))
    d.line(pts, fill=color, width=thick, joint="curve")


def portrait(d, w, h, variant=1, color=GOLD, warm=True):
    cx = w * 0.5
    head_r = min(w, h) * 0.155
    head_y = h * 0.40
    # 肩颈
    shoulder = [(cx - w * 0.34, h * 0.98), (cx - w * 0.28, h * 0.70), (cx - w * 0.12, h * 0.60),
                (cx + w * 0.12, h * 0.60), (cx + w * 0.28, h * 0.70), (cx + w * 0.34, h * 0.98)]
    d.polygon(shoulder, fill=(22, 16, 13, 255))
    d.ellipse([cx - head_r, head_y - head_r * 1.12, cx + head_r, head_y + head_r * 1.12], fill=(22, 16, 13, 255))
    # 发髻
    d.ellipse([cx - head_r * 0.62, head_y - head_r * 1.75, cx + head_r * 0.62, head_y - head_r * 0.95], fill=(16, 11, 9, 255))
    if variant == 2:
        d.ellipse([cx + head_r * 0.30, head_y - head_r * 1.55, cx + head_r * 0.95, head_y - head_r * 0.95], fill=(16, 11, 9, 255))
    # 金色轮廓光
    d.arc([cx - head_r, head_y - head_r * 1.12, cx + head_r, head_y + head_r * 1.12], 190, 320, fill=color, width=S * 2)
    d.line(shoulder[2:5], fill=color + (170,), width=S * 2, joint="curve")
    # 衣领
    d.line([(cx - w * 0.10, h * 0.72), (cx, h * 0.86), (cx + w * 0.10, h * 0.72)], fill=color + (120,), width=S * 2)
    # 背景圆
    d.arc([cx - w * 0.30, head_y - w * 0.34, cx + w * 0.30, head_y + w * 0.26], 0, 360, fill=color + (70,), width=S)


def finish(img, w, h):
    img = img.filter(ImageFilter.SMOOTH)
    img = vignette(img, 0.5)
    return img.resize((w // S, h // S), Image.LANCZOS)


def new_layer(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def compose(base, layer):
    return Image.alpha_composite(base.convert("RGBA"), layer).convert("RGB")


# ----------------------------- 具体画面 -----------------------------

def chapter_main(w, h):
    img = base_canvas(w, h)
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    # 背景月轮
    d.arc([w * 0.52, h * 0.10, w * 0.98, h * 0.92], 0, 360, fill=GOLD + (38,), width=S * 2)
    lacquer_tree(d, w * 0.60, h * 0.88, h * 0.66, min(w, h) * 0.36, GOLD, S * 3)
    bowl(d, w * 0.84, h * 0.82, w * 0.15, h * 0.15, GOLD_BRIGHT, S * 2)
    vtext(d, "千年漆语", w * 0.20, h * 0.20, load_font(FONT_KAI, int(h * 0.105)), CREAM + (235,), int(h * 0.125))
    d.line([(w * 0.145, h * 0.235), (w * 0.145, h * 0.80)], fill=GOLD + (110,), width=S)
    d.text((w * 0.155, h * 0.845), "第 一 章 · 源 起", font=load_font(FONT_SONG, int(h * 0.036)), fill=GOLD + (200,))
    seal(d, w * 0.135, h * 0.14, int(h * 0.075), "漆")
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def chapter_branch(w, h):
    img = base_canvas(w, h, (150, 56, 38))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    start = (w * 0.10, h * 0.72)
    junction = (w * 0.34, h * 0.50)
    ends = [(w * 0.68, h * 0.24), (w * 0.76, h * 0.50), (w * 0.70, h * 0.78)]
    merge = (w * 0.90, h * 0.50)
    d.line([start, junction], fill=GOLD, width=S * 4, joint="curve")
    for i, e in enumerate(ends):
        pts = []
        for t in np.linspace(0, 1, 40):
            x = junction[0] + (e[0] - junction[0]) * t + math.sin(t * math.pi) * w * 0.03 * (i - 1)
            y = junction[1] + (e[1] - junction[1]) * t
            pts.append((x, y))
        d.line(pts, fill=GOLD if i != 1 else GOLD_BRIGHT, width=S * 3, joint="curve")
        d.ellipse([e[0] - S * 9, e[1] - S * 9, e[0] + S * 9, e[1] + S * 9], outline=GOLD_BRIGHT, width=S * 3)
        pts2 = [(e[0] + (merge[0] - e[0]) * t, e[1] + (merge[1] - e[1]) * t) for t in np.linspace(0, 1, 30)]
        d.line(pts2, fill=GOLD + (120,), width=S * 2, joint="curve")
    d.ellipse([merge[0] - S * 12, merge[1] - S * 12, merge[0] + S * 12, merge[1] + S * 12], fill=GOLD + (200,))
    d.ellipse([start[0] - S * 7, start[1] - S * 7, start[0] + S * 7, start[1] + S * 7], fill=CREAM + (230,))
    vtext(d, "分支参观", w * 0.15, h * 0.16, load_font(FONT_KAI, int(h * 0.09)), CREAM + (235,), int(h * 0.115))
    d.text((w * 0.15, h * 0.815), "不 同 路 径 · 同 样 抵 达", font=load_font(FONT_SONG, int(h * 0.032)), fill=GOLD + (190,))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def chapter_art(w, h):
    img = base_canvas(w, h, (128, 42, 30))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    # 器物剪影
    box_shape(d, w * 0.50, h * 0.34, w * 0.74, h * 0.62, GOLD, S * 3)
    bowl(d, w * 0.74, h * 0.70, w * 0.20, h * 0.20, GOLD_BRIGHT, S * 3)
    d.ellipse([w * 0.30, h * 0.56, w * 0.46, h * 0.72], outline=GOLD, width=S * 3)
    d.ellipse([w * 0.335, h * 0.595, w * 0.425, h * 0.685], outline=GOLD + (150,), width=S * 2)
    cloud_band(d, w * 0.52, w * 0.72, h * 0.44, min(w, h) * 0.05, GOLD, S * 2)
    vtext(d, "器以载道", w * 0.20, h * 0.24, load_font(FONT_KAI, int(h * 0.10)), CREAM + (235,), int(h * 0.12))
    d.text((w * 0.155, h * 0.80), "第 二 章 · 器 物", font=load_font(FONT_SONG, int(h * 0.034)), fill=GOLD + (200,))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def artifact_box(w, h):
    img = base_canvas(w, h, (140, 44, 32))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    box_shape(d, w * 0.22, h * 0.28, w * 0.80, h * 0.74, GOLD, S * 4)
    # 云纹带
    cloud_band(d, w * 0.26, w * 0.76, h * 0.46, min(w, h) * 0.06, GOLD_BRIGHT, S * 2)
    cloud_band(d, w * 0.26, w * 0.76, h * 0.66, min(w, h) * 0.055, GOLD_BRIGHT, S * 2, flip=True)
    # 中央开光
    d.rounded_rectangle([w * 0.40, h * 0.55, w * 0.62, h * 0.68], radius=S * 10, outline=CREAM + (160,), width=S * 2)
    d.text((w * 0.435, h * 0.565), "剔 红", font=load_font(FONT_KAI, int(h * 0.055)), fill=CREAM + (220,))
    d.text((w * 0.075, h * 0.845), "剔红云纹漆盒 · 藏品", font=load_font(FONT_SONG, int(h * 0.036)), fill=GOLD + (200,))
    seal(d, w * 0.845, h * 0.825, int(h * 0.075), "藏")
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def artifact_dish(w, h):
    img = base_canvas(w, h, (120, 40, 34))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    cx, cy, r = w * 0.50, h * 0.50, min(w, h) * 0.30
    d.ellipse([cx - r * 1.10, cy - r * 1.10, cx + r * 1.10, cy + r * 1.10], outline=GOLD, width=S * 4)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=GOLD_BRIGHT, width=S * 3)
    d.ellipse([cx - r * 0.86, cy - r * 0.86, cx + r * 0.86, cy + r * 0.86], outline=GOLD + (150,), width=S * 2)
    # 螺钿碎块（低饱和虹彩）
    rng = random.Random(3)
    palette = [(238, 226, 204), (206, 216, 204), (226, 208, 196), (196, 210, 214), (228, 214, 176)]
    for i in range(24):
        ang = rng.uniform(0, math.tau)
        rr = r * math.sqrt(rng.uniform(0.10, 0.72))
        px, py = cx + math.cos(ang) * rr, cy + math.sin(ang) * rr
        a = rng.uniform(0.055, 0.115) * r
        b = a * rng.uniform(0.26, 0.42)
        rot = rng.uniform(0, math.tau)
        ca, sa = math.cos(rot), math.sin(rot)
        pts = [
            (px + ca * a, py + sa * a),
            (px - sa * b, py + ca * b),
            (px - ca * a, py - sa * a),
            (px + sa * b, py - ca * b),
        ]
        d.polygon(pts, fill=palette[i % len(palette)] + (170,))
    # 中心花心
    d.ellipse([cx - r * 0.14, cy - r * 0.14, cx + r * 0.14, cy + r * 0.14], outline=GOLD_BRIGHT, width=S * 3)
    spiral(d, cx, cy, r * 0.11, 2.4, GOLD, S * 2)
    d.text((w * 0.06, h * 0.86), "嵌螺钿圆盘 · 藏品", font=load_font(FONT_SONG, int(h * 0.036)), fill=GOLD + (200,))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def artifact_screen(w, h):
    img = base_canvas(w, h, (132, 46, 34))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    n = 3
    total_w = w * 0.68
    panel_w = total_w / n
    x0 = w * 0.16
    top, bottom = h * 0.20, h * 0.80
    for i in range(n):
        x = x0 + i * panel_w
        skew = (i - 1) * S * 14
        pts = [(x + skew, top), (x + panel_w - S * 6 + skew, top + S * 6),
               (x + panel_w - S * 6, bottom), (x + S * 6, bottom)]
        d.line(pts + [pts[0]], fill=GOLD, width=S * 4, joint="curve")
        inner = [(p[0] + (14 if k == 0 else -14), p[1] + (16 if k == 1 else -16)) for k, p in enumerate(pts)]
        d.line(inner + [inner[0]], fill=GOLD + (120,), width=S * 2, joint="curve")
        ccx = x + panel_w / 2
        if i == 0:
            cloud_band(d, x + panel_w * 0.22, x + panel_w * 0.78, h * 0.36, panel_w * 0.15, GOLD_BRIGHT, S * 2)
        elif i == 1:
            phoenix(d, ccx, h * 0.48, panel_w * 0.95, GOLD_BRIGHT, S * 3)
        else:
            spiral(d, ccx, h * 0.36, panel_w * 0.16, 2.4, GOLD_BRIGHT, S * 2)
            spiral(d, ccx, h * 0.62, panel_w * 0.16, 2.4, GOLD_BRIGHT, S * 2, phase=math.pi)
    d.text((w * 0.06, h * 0.86), "漆画屏风 · 藏品", font=load_font(FONT_SONG, int(h * 0.036)), fill=GOLD + (200,))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def inheritor_img(w, h, variant=1):
    img = base_canvas(w, h, (120, 36, 28) if variant == 1 else (70, 62, 46))
    if variant == 2:
        img = add_glow(img, w * 0.72, h * 0.32, w * 0.5, h * 0.5, (96, 118, 100), 60)
    img = add_glow(img, w * 0.50, h * 0.36, w * 0.34, h * 0.34, (206, 150, 90), 48)
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    portrait(d, w, h, variant, GOLD_BRIGHT)
    vtext(d, "髹漆匠人", w * 0.865, h * 0.62, load_font(FONT_KAI, int(h * 0.052)), CREAM + (215,), int(h * 0.062))
    d.text((w * 0.09, h * 0.885), "传 承 人", font=load_font(FONT_SONG, int(h * 0.038)), fill=GOLD + (200,))
    seal(d, w * 0.80, h * 0.10, int(h * 0.075), "匠")
    frame(d, w, h, margin=0.045)
    return finish(compose(img, layer), w, h)


def motif_cloud(w, h):
    img = base_canvas(w, h, (118, 36, 28))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    d.arc([w * 0.10, h * 0.08, w * 0.90, h * 0.88], 0, 360, fill=GOLD + (60,), width=S * 2)
    rows = 3
    for r in range(rows):
        for c in range(3):
            cx = w * (0.28 + c * 0.22) + (r % 2) * w * 0.04
            cy = h * (0.28 + r * 0.22)
            spiral(d, cx, cy, min(w, h) * 0.055, 2.3, GOLD_BRIGHT if (r + c) % 2 == 0 else GOLD, S * 2, phase=0.4 * (r + c))
            d.arc([cx - w * 0.09, cy - h * 0.07, cx + w * 0.09, cy + h * 0.07], 200, 340, fill=GOLD + (160,), width=S * 2)
    vtext(d, "云纹", w * 0.865, h * 0.30, load_font(FONT_KAI, int(h * 0.075)), CREAM + (220,), int(h * 0.09))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def motif_grass(w, h):
    img = base_canvas(w, h, (112, 38, 30))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    for row, y0 in enumerate([h * 0.28, h * 0.58]):
        pts = []
        for i in range(120):
            t = i / 119
            pts.append((w * (0.10 + 0.80 * t), y0 + math.sin(t * math.pi * 3) * h * 0.075))
        d.line(pts, fill=GOLD, width=S * 3, joint="curve")
        for k in range(1, 12):
            px, py = pts[int(k * 10)]
            ang = math.atan2(pts[int(k * 10)][1] - pts[int(k * 10) - 1][1],
                             pts[int(k * 10)][0] - pts[int(k * 10) - 1][0])
            leaf(d, px, py - S * 7, min(w, h) * 0.085, ang - math.pi / 2, GOLD_BRIGHT, S * 2)
            leaf(d, px, py + S * 7, min(w, h) * 0.075, ang + math.pi / 2, GOLD + (190,), S * 2)
    vtext(d, "卷草纹", w * 0.86, h * 0.30, load_font(FONT_KAI, int(h * 0.075)), CREAM + (220,), int(h * 0.088))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


def motif_bird(w, h):
    img = base_canvas(w, h, (124, 40, 30))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    d.arc([w * 0.14, h * 0.10, w * 0.84, h * 0.86], 0, 360, fill=GOLD + (55,), width=S * 2)
    phoenix(d, w * 0.52, h * 0.50, min(w, h) * 0.72, GOLD_BRIGHT, S * 4)
    vtext(d, "凤鸟纹", w * 0.865, h * 0.30, load_font(FONT_KAI, int(h * 0.075)), CREAM + (220,), int(h * 0.088))
    frame(d, w, h)
    return finish(compose(img, layer), w, h)


# ---------- 章节内文配图（1200x700） ----------

def node_img(w, h, kind):
    img = base_canvas(w, h, (110, 34, 26))
    layer = new_layer(w, h)
    d = ImageDraw.Draw(layer)
    if kind == "tree":
        lacquer_tree(d, w * 0.32, h * 0.80, h * 0.52, min(w, h) * 0.26, GOLD, S * 3)
        lacquer_tree(d, w * 0.56, h * 0.86, h * 0.36, min(w, h) * 0.18, GOLD + (180,), S * 2)
        bowl(d, w * 0.78, h * 0.76, w * 0.16, h * 0.16, GOLD_BRIGHT, S * 2)
        label = "割漆"
    elif kind == "trip":
        # 制胎：木胎 + 车旋
        d.rounded_rectangle([w * 0.28, h * 0.40, w * 0.66, h * 0.72], radius=S * 18, outline=GOLD, width=S * 4)
        for k in range(6):
            y = h * (0.44 + k * 0.045)
            d.arc([w * 0.30, y - h * 0.03, w * 0.64, y + h * 0.03], 200, 340, fill=GOLD + (140,), width=S * 2)
        d.ellipse([w * 0.70, h * 0.46, w * 0.84, h * 0.60], outline=GOLD_BRIGHT, width=S * 3)
        d.line([(w * 0.66, h * 0.56), (w * 0.70, h * 0.54)], fill=CREAM + (200,), width=S * 3)
        label = "制胎"
    elif kind == "deco":
        # 髹涂描金：笔触与金屑
        for k, (x0, y0, x1, y1) in enumerate([
            (w * 0.18, h * 0.66, w * 0.52, h * 0.34),
            (w * 0.34, h * 0.74, w * 0.72, h * 0.40),
            (w * 0.24, h * 0.50, w * 0.62, h * 0.24),
        ]):
            brush_stroke(d, x0, y0, x1, y1, S * (10 - k * 2), GOLD_BRIGHT if k == 1 else GOLD)
        rng = random.Random(5)
        for _ in range(60):
            px, py = rng.uniform(w * 0.15, w * 0.8), rng.uniform(h * 0.2, h * 0.8)
            r = rng.uniform(1, 3) * S
            d.ellipse([px - r, py - r, px + r, py + r], fill=CREAM + (120,))
        d.polygon([(w * 0.76, h * 0.66), (w * 0.80, h * 0.30), (w * 0.83, h * 0.66)], fill=(30, 20, 16, 220))
        d.line([(w * 0.795, h * 0.30), (w * 0.78, h * 0.20)], fill=GOLD, width=S * 3)
        label = "髹涂描金"
    elif kind == "ending":
        cx, cy = w * 0.5, h * 0.55
        for k in range(12):
            ang = k * math.tau / 12
            d.line([(cx + math.cos(ang) * w * 0.14, cy + math.sin(ang) * h * 0.18),
                    (cx + math.cos(ang) * w * 0.42, cy + math.sin(ang) * h * 0.44)],
                   fill=GOLD + (80,), width=S)
        bowl(d, cx, cy, w * 0.30, h * 0.30, GOLD_BRIGHT, S * 4)
        d.arc([cx - w * 0.16, cy - h * 0.20, cx + w * 0.16, cy + h * 0.02], 0, 360, fill=GOLD + (150,), width=S * 2)
        label = "成器"
    elif kind == "view":
        # 器物特写：器沿金线
        d.arc([w * 0.10, h * 0.30, w * 0.90, h * 0.90], 200, 340, fill=GOLD, width=S * 4)
        d.arc([w * 0.18, h * 0.38, w * 0.82, h * 0.86], 200, 340, fill=GOLD_BRIGHT, width=S * 3)
        cloud_band(d, w * 0.18, w * 0.82, h * 0.62, min(w, h) * 0.05, GOLD_BRIGHT, S * 2)
        label = "器沿"
    elif kind == "life":
        # 生活器物：碗、盖盒、执壶
        bowl(d, w * 0.30, h * 0.52, w * 0.26, h * 0.26, GOLD, S * 3)
        d.rounded_rectangle([w * 0.50, h * 0.42, w * 0.66, h * 0.66], radius=S * 12,
                            outline=GOLD_BRIGHT, width=S * 3)
        d.line([(w * 0.50 + S * 4, h * 0.49), (w * 0.66 - S * 4, h * 0.49)], fill=GOLD_BRIGHT, width=S * 3)
        d.line([(w * 0.55, h * 0.42), (w * 0.61, h * 0.42)], fill=GOLD_BRIGHT, width=S * 4)
        # 罐：小口鼓腹
        d.ellipse([w * 0.72, h * 0.46, w * 0.90, h * 0.68], outline=GOLD, width=S * 3)
        d.line([(w * 0.795, h * 0.40), (w * 0.795, h * 0.47)], fill=GOLD, width=S * 3)
        d.line([(w * 0.825, h * 0.40), (w * 0.825, h * 0.47)], fill=GOLD, width=S * 3)
        d.ellipse([w * 0.788, h * 0.372, w * 0.832, h * 0.404], outline=GOLD, width=S * 3)
        d.line([(w * 0.765, h * 0.665), (w * 0.855, h * 0.665)], fill=GOLD, width=S * 3)
        d.arc([w * 0.74, h * 0.52, w * 0.88, h * 0.62], 200, 340, fill=GOLD + (150,), width=S * 2)
        label = "生活器物"
    else:  # art
        cloud_band(d, w * 0.10, w * 0.58, h * 0.26, min(w, h) * 0.07, GOLD_BRIGHT, S * 3)
        phoenix(d, w * 0.58, h * 0.58, min(w, h) * 0.82, GOLD, S * 4)
        label = "纹样"
    # 标题与角标
    d.text((w * 0.06, h * 0.855), label, font=load_font(FONT_KAI, int(h * 0.062)), fill=CREAM + (225,))
    d.text((w * 0.06, h * 0.925), "蜀 漆 云 馆 · 图 解", font=load_font(FONT_SONG, int(h * 0.028)), fill=GOLD + (170,))
    frame(d, w, h, margin=0.045)
    return finish(compose(img, layer), w, h)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "static", "seed")
    out = os.path.abspath(out)
    os.makedirs(out, exist_ok=True)

    tasks = [
        ("chapter-main", lambda: chapter_main(1500 * S, 840 * S)),
        ("chapter-branch", lambda: chapter_branch(1500 * S, 840 * S)),
        ("chapter-art", lambda: chapter_art(1500 * S, 840 * S)),
        ("art-box", lambda: artifact_box(1200 * S, 900 * S)),
        ("art-dish", lambda: artifact_dish(1200 * S, 900 * S)),
        ("art-screen", lambda: artifact_screen(1200 * S, 900 * S)),
        ("inheritor-1", lambda: inheritor_img(800 * S, 800 * S, 1)),
        ("inheritor-2", lambda: inheritor_img(800 * S, 800 * S, 2)),
        ("motif-cloud", lambda: motif_cloud(900 * S, 720 * S)),
        ("motif-grass", lambda: motif_grass(900 * S, 720 * S)),
        ("motif-bird", lambda: motif_bird(900 * S, 720 * S)),
        ("node-tree", lambda: node_img(1200 * S, 700 * S, "tree")),
        ("node-trip", lambda: node_img(1200 * S, 700 * S, "trip")),
        ("node-deco", lambda: node_img(1200 * S, 700 * S, "deco")),
        ("node-ending", lambda: node_img(1200 * S, 700 * S, "ending")),
        ("node-view", lambda: node_img(1200 * S, 700 * S, "view")),
        ("node-life", lambda: node_img(1200 * S, 700 * S, "life")),
        ("node-art", lambda: node_img(1200 * S, 700 * S, "art")),
    ]
    for name, fn in tasks:
        img = fn()
        path = os.path.join(out, name + ".png")
        img.save(path, optimize=True)
        print("generated", path, img.size)


if __name__ == "__main__":
    main()
