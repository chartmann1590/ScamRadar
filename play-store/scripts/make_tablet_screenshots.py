"""Compose tablet marketing screenshots for Play Store.

Tablet listings use landscape (16:10) compositions. We reuse the captured phone
screenshots in a phone-style frame on the left, with brand background and a
headline + supporting line on the right.

Two output sizes:
  - 7-inch:  1920 x 1200
  - 10-inch: 2560 x 1600
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

PHONE_OUT_DIR = "h:/new-app/play-store/screenshots/phone"
TAB7_DIR = "h:/new-app/play-store/screenshots/tablet-7in"
TAB10_DIR = "h:/new-app/play-store/screenshots/tablet-10in"
os.makedirs(TAB7_DIR, exist_ok=True)
os.makedirs(TAB10_DIR, exist_ok=True)
RAW = "h:/new-app/play-store/screenshots/raw"

BG_DEEP = (8, 14, 50)
BG_BRAND = (45, 79, 224)
BG_HILITE = (118, 158, 255)
WHITE = (255, 255, 255)
CYAN = (135, 220, 255)
WARN = (255, 86, 96)
GREEN = (76, 217, 168)
AMBER = (255, 211, 105)

SLIDES = [
    ("01_home.png",
     "Paste any scam.\nGet an answer in 3 seconds.",
     "Free, on-device AI. No account. No upload.",
     CYAN,
     ["100% On-Device", "No Account", "Free Forever"]),
    ("03_scanning.png",
     "100% on your phone.\nNothing ever uploaded.",
     "Your messages literally cannot leak — we never receive them.",
     GREEN,
     ["Private by Design", "Works Offline", "No Tracking"]),
    ("04_result_scam.png",
     "Clear verdict.\nIn plain English.",
     "Suspicious • Likely Scam • Looks Safe — with a confidence score.",
     WARN,
     ["Confidence Score", "Red Flags Shown", "Plain English"]),
    ("05_result_scam_detail.png",
     "Know exactly\nwhat to do next.",
     "Red flags highlighted. Step-by-step actions you can take right now.",
     AMBER,
     ["What To Do", "Red Flag Phrases", "Share Card"]),
    ("09_result_safe.png",
     "Peace of mind\nfor real messages too.",
     "When it's legit, you'll know — fast.",
     GREEN,
     ["Looks Safe", "Confidence Score", "Plain English"]),
    ("11_library_detail.png",
     "Learn the 12 top\nscam patterns of 2026.",
     "Phishing, AI voice cloning, IRS, USPS, romance, crypto…",
     CYAN,
     ["12 Patterns", "Real Examples", "Spot Spotting Tips"]),
    ("07_history.png",
     "Your last 50 scans.\nLocal. Private. Yours.",
     "Stored only on your device. Delete anytime.",
     AMBER,
     ["Local Only", "Searchable", "Delete Anytime"]),
    ("08_settings.png",
     "Free forever.\nWorks offline.",
     "No subscription. No paywall. No ads in the scanner.",
     CYAN,
     ["Free", "Offline", "AI On-Device"]),
]


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


def text_size(draw, text, fnt):
    bbox = draw.textbbox((0, 0), text, font=fnt)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def radial(size, inner, outer, cx=None, cy=None):
    w, h = size
    cx = w / 2 if cx is None else cx
    cy = h / 2 if cy is None else cy
    img = Image.new("RGB", size, outer)
    px = img.load()
    max_d = math.hypot(max(cx, w - cx), max(cy, h - cy))
    for y in range(h):
        for x in range(w):
            d = min(1.0, math.hypot(x - cx, y - cy) / max_d)
            r = int(inner[0] * (1 - d) + outer[0] * d)
            g = int(inner[1] * (1 - d) + outer[1] * d)
            b = int(inner[2] * (1 - d) + outer[2] * d)
            px[x, y] = (r, g, b)
    return img


def round_rect_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size[0] - 1, size[1] - 1), radius=radius, fill=255)
    return mask


def clean_raw(im):
    iw, ih = im.size
    page_bg_top = im.getpixel((10, 110))
    page_bg_mid = im.getpixel((10, 1900))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, int(iw * 0.55), 90), fill=page_bg_top)
    d.rectangle((0, 1490, iw, 1840), fill=page_bg_mid)
    return im


def render_tablet(W, H, slide):
    raw_name, head, sub, accent, chips = slide
    base = radial((W, H), BG_HILITE, BG_DEEP, cx=W * 0.7, cy=H * 0.2).convert("RGBA")
    wash = Image.new("RGBA", (W, H), BG_BRAND + (200,))
    out = Image.alpha_composite(base, wash)

    # Radar arcs behind phone (right region)
    arcs = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ad = ImageDraw.Draw(arcs)
    rcx, rcy = int(W * 0.28), int(H * 0.55)
    for r, a in [(int(H * 0.55), 16), (int(H * 0.42), 22),
                 (int(H * 0.30), 28), (int(H * 0.18), 36)]:
        ad.ellipse([rcx - r, rcy - r, rcx + r, rcy + r],
                   outline=(255, 255, 255, a), width=4)
    out.alpha_composite(arcs)

    # Phone screenshot on left
    raw = Image.open(os.path.join(RAW, raw_name)).convert("RGB").copy()
    raw = clean_raw(raw)
    rw, rh = raw.size  # 1008 x 2244 → aspect ~0.45

    # Target phone height = 88% of H
    target_h = int(H * 0.86)
    target_w = int(rw * (target_h / rh))
    resized = raw.resize((target_w, target_h), Image.LANCZOS).convert("RGBA")
    mask = round_rect_mask((target_w, target_h), radius=int(target_w * 0.05))
    resized.putalpha(mask)

    phone_x = int(W * 0.04)
    phone_y = (H - target_h) // 2

    # Shadow
    shadow = Image.new("RGBA", (target_w + 120, target_h + 120), (0, 0, 0, 0))
    sh_mask = round_rect_mask((target_w, target_h), int(target_w * 0.05))
    shadow.paste((0, 0, 0, 180), (60, 80, 60 + target_w, 80 + target_h), sh_mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(36))
    out.alpha_composite(shadow, (phone_x - 60, phone_y - 80))
    out.alpha_composite(resized, (phone_x, phone_y))

    # Right side — text
    d = ImageDraw.Draw(out)
    rx = phone_x + target_w + int(W * 0.06)
    rw_avail = W - rx - int(W * 0.06)

    # Eyebrow
    fnt_eye = font(int(H * 0.025), True)
    eye = "SCAMRADAR · AI SCAM DETECTOR"
    d.text((rx, int(H * 0.10)), eye, fill=accent + (255,), font=fnt_eye)

    # Headline
    head_size = int(H * 0.075)
    fnt_h = font(head_size, True)
    y = int(H * 0.16)
    for line in head.split("\n"):
        d.text((rx, y), line, fill=WHITE + (255,), font=fnt_h)
        y += int(head_size * 1.15)

    # Subhead — word wrap
    fnt_s = font(int(H * 0.026))
    max_w = rw_avail
    words = sub.split()
    lines = []
    cur = ""
    for w in words:
        test = (cur + " " + w).strip()
        if d.textbbox((0, 0), test, font=fnt_s)[2] <= max_w:
            cur = test
        else:
            lines.append(cur); cur = w
    if cur:
        lines.append(cur)
    yy = y + int(H * 0.015)
    for line in lines[:3]:
        d.text((rx, yy), line, fill=(220, 230, 255, 255), font=fnt_s)
        yy += int(H * 0.035)

    # Chips
    fnt_chip = font(int(H * 0.020), True)
    chip_y = yy + int(H * 0.05)
    chip_colors = [GREEN, CYAN, AMBER]
    cx_pos = rx
    for label, color in zip(chips, chip_colors):
        tw2, _ = text_size(d, label, fnt_chip)
        chip_w = tw2 + int(H * 0.07)
        chip_h = int(H * 0.045)
        chip_layer = Image.new("RGBA", out.size, (0, 0, 0, 0))
        cd = ImageDraw.Draw(chip_layer)
        cd.rounded_rectangle(
            (cx_pos, chip_y, cx_pos + chip_w, chip_y + chip_h),
            radius=int(chip_h / 2), fill=(10, 18, 60, 160),
            outline=color + (240,), width=2)
        out.alpha_composite(chip_layer)
        d.ellipse((cx_pos + int(chip_h * 0.30), chip_y + int(chip_h * 0.36),
                   cx_pos + int(chip_h * 0.55), chip_y + int(chip_h * 0.61)),
                  fill=color + (255,))
        d.text((cx_pos + int(chip_h * 0.75), chip_y + int(chip_h * 0.18)),
               label, fill=WHITE + (255,), font=fnt_chip)
        cx_pos += chip_w + int(H * 0.018)
        if cx_pos > W - rx + phone_x:  # wrap if needed
            cx_pos = rx
            chip_y += chip_h + int(H * 0.015)

    # Bottom brand strip
    strip_h = int(H * 0.075)
    d.rectangle((0, H - strip_h, W, H), fill=(8, 14, 50, 255))
    fnt_b = font(int(strip_h * 0.34), True)
    fnt_bm = font(int(strip_h * 0.32))
    d.text((int(W * 0.04), H - strip_h + int(strip_h * 0.30)),
           "ScamRadar", fill=CYAN + (255,), font=fnt_b)
    right_text = "Paste it. Check it. Don't get scammed."
    rt_w = d.textbbox((0, 0), right_text, font=fnt_bm)[2]
    d.text((W - int(W * 0.04) - rt_w, H - strip_h + int(strip_h * 0.32)),
           right_text, fill=(220, 230, 255, 255), font=fnt_bm)

    return out.convert("RGB")


def main():
    for i, slide in enumerate(SLIDES, 1):
        img7 = render_tablet(1920, 1200, slide)
        img7.save(os.path.join(TAB7_DIR, f"tablet7_{i:02d}.png"), optimize=True)
        img10 = render_tablet(2560, 1600, slide)
        img10.save(os.path.join(TAB10_DIR, f"tablet10_{i:02d}.png"), optimize=True)
        print("Saved slide", i)


if __name__ == "__main__":
    main()
