"""Generate 1920x1080 brand-styled frames for each feature video.

Reads play-store/video/feature_scripts.json. For each video, generates
one PNG per scene into feature_videos/<video_id>/frames/frame_NN.png.

Scene frame_type values supported:
- headline:    big text on a brand-colored radial background
- screenshot:  brand-colored panel on left, device screenshot on right
- mockup_qr:   QR-code mockup with brand styling
- cta:         large headline + subtitle on cta background
"""
import json
import math
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path("h:/new-app/play-store/video")
RAW = ROOT  # raw_*.png live here next to feature_scripts.json
EXTERNAL = Path("h:/new-app")  # for the older shield_caught screenshots

W, H = 1920, 1080
BG_DEEP = (8, 14, 50)
BG_BRAND = (45, 79, 224)
BG_PURPLE = (90, 50, 200)
BG_HILITE = (118, 158, 255)
BG_RED = (180, 40, 50)
BG_CTA_A = (45, 79, 224)
BG_CTA_B = (90, 50, 180)
WHITE = (255, 255, 255)
GREEN = (76, 217, 168)
AMBER = (255, 211, 105)


def font(size, bold=False):
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for c in candidates:
        try:
            return ImageFont.truetype(c, size)
        except Exception:
            pass
    return ImageFont.load_default()


def text_size(d, text, fnt):
    bbox = d.textbbox((0, 0), text, font=fnt)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def radial(size, inner, outer, cx=None, cy=None, r=None):
    w, h = size
    cx = w / 2 if cx is None else cx
    cy = h / 2 if cy is None else cy
    r = max(w, h) * 0.9 if r is None else r
    img = Image.new("RGB", size, outer)
    px = img.load()
    for y in range(h):
        for x in range(w):
            d = math.hypot(x - cx, y - cy) / r
            d = min(1.0, d)
            r_ = int(inner[0] * (1 - d) + outer[0] * d)
            g_ = int(inner[1] * (1 - d) + outer[1] * d)
            b_ = int(inner[2] * (1 - d) + outer[2] * d)
            px[x, y] = (r_, g_, b_)
    return img


def diagonal_gradient(size, a, b):
    w, h = size
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / w + y / h) / 2
            px[x, y] = (
                int(a[0] * (1 - t) + b[0] * t),
                int(a[1] * (1 - t) + b[1] * t),
                int(a[2] * (1 - t) + b[2] * t),
            )
    return img


def draw_shield_dot(img, cx, cy, r, fill):
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)
    inset = int(r * 0.45)
    d.rectangle([cx - inset, cy - inset // 2, cx + inset, cy + inset // 2], fill=WHITE)


def wrap_text(d, text, fnt, max_w):
    words = text.split()
    lines = []
    cur = []
    for w in words:
        trial = (" ".join(cur + [w])).strip()
        if d.textlength(trial, font=fnt) <= max_w or not cur:
            cur.append(w)
        else:
            lines.append(" ".join(cur))
            cur = [w]
    if cur:
        lines.append(" ".join(cur))
    return lines


def bg_for(name):
    if name == "dark_brand":
        return radial((W, H), BG_BRAND, BG_DEEP, cx=W * 0.3, cy=H * 0.3)
    if name == "purple_brand":
        return radial((W, H), BG_PURPLE, BG_DEEP, cx=W * 0.7, cy=H * 0.3)
    if name == "warn_red":
        return radial((W, H), BG_RED, (40, 5, 10), cx=W * 0.4, cy=H * 0.5)
    if name == "cta":
        return diagonal_gradient((W, H), BG_CTA_A, BG_CTA_B)
    return Image.new("RGB", (W, H), BG_DEEP)


def make_headline_frame(scene, out):
    img = bg_for(scene.get("bg", "dark_brand"))
    d = ImageDraw.Draw(img)

    title_fnt = font(150, bold=True)
    sub_fnt = font(46)

    title = scene["headline"]
    subtitle = scene.get("subtitle", "")

    # Title — possibly wrap
    title_lines = wrap_text(d, title, title_fnt, W - 280)
    line_h = text_size(d, "Ag", title_fnt)[1] + 24
    total_title_h = line_h * len(title_lines)
    y = H * 0.32 - total_title_h * 0.4
    for line in title_lines:
        lw, _ = text_size(d, line, title_fnt)
        d.text(((W - lw) / 2, y), line, font=title_fnt, fill=WHITE)
        y += line_h

    if subtitle:
        sub_lines = wrap_text(d, subtitle, sub_fnt, W - 320)
        sub_line_h = text_size(d, "Ag", sub_fnt)[1] + 14
        sy = y + 30
        for line in sub_lines:
            lw, _ = text_size(d, line, sub_fnt)
            d.text(((W - lw) / 2, sy), line, font=sub_fnt, fill=(225, 230, 255))
            sy += sub_line_h

    # Brand mark in corner
    draw_shield_dot(img, 130, 130, 36, BG_HILITE)
    d.text((180, 102), "ScamRadar", font=font(34, bold=True), fill=WHITE)

    img.save(out, optimize=True)


def make_cta_frame(scene, out):
    img = bg_for(scene.get("bg", "cta"))
    d = ImageDraw.Draw(img)

    headline = scene["headline"]
    subtitle = scene.get("subtitle", "")

    # giant shield
    sd_cx, sd_cy, sd_r = W * 0.5, H * 0.32, 110
    draw_shield_dot(img, sd_cx, sd_cy, sd_r, WHITE)

    title_fnt = font(180, bold=True)
    sub_fnt = font(56, bold=True)

    lw, lh = text_size(d, headline, title_fnt)
    d.text(((W - lw) / 2, H * 0.45), headline, font=title_fnt, fill=WHITE)

    if subtitle:
        sw, _ = text_size(d, subtitle, sub_fnt)
        d.text(((W - sw) / 2, H * 0.45 + lh + 30), subtitle, font=sub_fnt, fill=(230, 232, 255))

    # url
    url = "scamradar.app"
    uw, _ = text_size(d, url, font(34))
    d.text(((W - uw) / 2, H * 0.9), url, font=font(34), fill=(220, 224, 255))

    img.save(out, optimize=True)


def make_screenshot_frame(scene, out):
    img = bg_for(scene.get("bg", "dark_brand"))
    d = ImageDraw.Draw(img)

    headline = scene["headline"]
    subtitle = scene.get("subtitle", "")

    # Left text panel
    title_fnt = font(110, bold=True)
    sub_fnt = font(40)

    title_lines = wrap_text(d, headline, title_fnt, 880)
    line_h = text_size(d, "Ag", title_fnt)[1] + 16
    y = H * 0.32
    for line in title_lines:
        d.text((120, y), line, font=title_fnt, fill=WHITE)
        y += line_h

    if subtitle:
        sub_lines = wrap_text(d, subtitle, sub_fnt, 880)
        sub_line_h = text_size(d, "Ag", sub_fnt)[1] + 12
        sy = y + 30
        for line in sub_lines:
            d.text((120, sy), line, font=sub_fnt, fill=(225, 230, 255))
            sy += sub_line_h

    # Brand mark
    draw_shield_dot(img, 120, 120, 36, BG_HILITE)
    d.text((170, 92), "ScamRadar", font=font(34, bold=True), fill=WHITE)

    # Right screenshot panel - find the screenshot
    ss_path = None
    candidates = [
        RAW / scene["screenshot"],
        EXTERNAL / scene["screenshot"],
    ]
    for c in candidates:
        if c.exists():
            ss_path = c
            break

    if ss_path is None:
        print(f"  ! screenshot not found: {scene['screenshot']}")
        img.save(out, optimize=True)
        return

    ss = Image.open(ss_path).convert("RGBA")
    # Fit into right half with margin
    target_h = int(H * 0.78)
    target_w = int(ss.width * target_h / ss.height)
    # If too wide for right half, scale by width instead
    max_w = int(W * 0.42)
    if target_w > max_w:
        target_w = max_w
        target_h = int(ss.height * target_w / ss.width)
    ss = ss.resize((target_w, target_h), Image.LANCZOS)

    # Drop shadow behind device
    shadow = Image.new("RGBA", (target_w + 60, target_h + 60), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([20, 20, target_w + 40, target_h + 40], radius=48,
                         fill=(0, 0, 0, 130))
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))

    # Rounded mask for screenshot
    mask = Image.new("L", (target_w, target_h), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, target_w, target_h], radius=40, fill=255)

    px = int(W * 0.62)
    py = int((H - target_h) / 2)
    img.paste(shadow, (px - 30, py - 30), shadow)
    img.paste(ss, (px, py), mask)

    img.save(out, optimize=True)


def make_qr_mockup(scene, out):
    img = bg_for(scene.get("bg", "purple_brand"))
    d = ImageDraw.Draw(img)

    headline = scene["headline"]
    subtitle = scene.get("subtitle", "")

    title_fnt = font(110, bold=True)
    sub_fnt = font(40)

    title_lines = wrap_text(d, headline, title_fnt, 880)
    line_h = text_size(d, "Ag", title_fnt)[1] + 16
    y = H * 0.32
    for line in title_lines:
        d.text((120, y), line, font=title_fnt, fill=WHITE)
        y += line_h

    if subtitle:
        sub_lines = wrap_text(d, subtitle, sub_fnt, 880)
        sub_line_h = text_size(d, "Ag", sub_fnt)[1] + 12
        sy = y + 30
        for line in sub_lines:
            d.text((120, sy), line, font=sub_fnt, fill=(225, 230, 255))
            sy += sub_line_h

    # QR mockup on right
    qr_size = 460
    qr_x, qr_y = int(W * 0.65), int(H * 0.5 - qr_size / 2)
    panel = Image.new("RGBA", (qr_size + 80, qr_size + 80), (255, 255, 255, 250))
    pd = ImageDraw.Draw(panel)
    pd.rounded_rectangle([0, 0, qr_size + 80, qr_size + 80], radius=40,
                         fill=(255, 255, 255, 250))
    img.paste(panel, (qr_x - 40, qr_y - 40), panel)

    # Pseudo-QR pattern
    cell = qr_size // 25
    pseudo = [
        # finder patterns at corners
        [(0, 0, 6, 6), (18, 0, 24, 6), (0, 18, 6, 24)],
    ]
    qd = ImageDraw.Draw(img)
    # finder patterns
    for (a, b, c, dd) in pseudo[0]:
        qd.rectangle([qr_x + a * cell, qr_y + b * cell,
                      qr_x + c * cell, qr_y + dd * cell],
                     fill=(20, 20, 50))
        qd.rectangle([qr_x + (a + 1) * cell, qr_y + (b + 1) * cell,
                      qr_x + (c - 1) * cell, qr_y + (dd - 1) * cell],
                     fill=(255, 255, 255))
        qd.rectangle([qr_x + (a + 2) * cell, qr_y + (b + 2) * cell,
                      qr_x + (c - 2) * cell, qr_y + (dd - 2) * cell],
                     fill=(20, 20, 50))

    # random data cells (deterministic so it doesn't shimmer)
    rng = 12345
    for r in range(25):
        for col in range(25):
            # skip finder regions
            if (r < 7 and col < 7) or (r < 7 and col > 17) or (r > 17 and col < 7):
                continue
            rng = (rng * 1103515245 + 12345) & 0x7FFFFFFF
            if rng & 0x10000:
                qd.rectangle([qr_x + col * cell, qr_y + r * cell,
                              qr_x + (col + 1) * cell, qr_y + (r + 1) * cell],
                             fill=(20, 20, 50))

    # Shield overlay in QR center (brand)
    cx, cy = qr_x + qr_size // 2, qr_y + qr_size // 2
    qd.ellipse([cx - 50, cy - 50, cx + 50, cy + 50], fill=WHITE)
    draw_shield_dot(img, cx, cy, 38, BG_BRAND)

    # Brand mark
    draw_shield_dot(img, 120, 120, 36, BG_HILITE)
    d.text((170, 92), "ScamRadar", font=font(34, bold=True), fill=WHITE)

    img.save(out, optimize=True)


def main():
    cfg = json.loads((ROOT / "feature_scripts.json").read_text(encoding="utf-8"))
    for video in cfg["videos"]:
        vid = video["id"]
        frame_dir = ROOT / "feature_videos" / vid / "frames"
        frame_dir.mkdir(parents=True, exist_ok=True)
        print(f"[{vid}]")
        for i, scene in enumerate(video["scenes"], 1):
            out = frame_dir / f"frame_{i:02d}.png"
            ft = scene["frame_type"]
            if ft == "headline":
                make_headline_frame(scene, out)
            elif ft == "screenshot":
                make_screenshot_frame(scene, out)
            elif ft == "mockup_qr":
                make_qr_mockup(scene, out)
            elif ft == "cta":
                make_cta_frame(scene, out)
            else:
                print(f"  unknown frame_type: {ft}")
            print(f"  wrote {out.name}")

    print("DONE")


if __name__ == "__main__":
    main()
