"""Compose marketing phone screenshots for the Play Store.

Each output is 1242x2208 (a generous Play Store phone size that scales cleanly
to 16:9 / 9:16 listings). We:
  1. Paint a brand-gradient background.
  2. Draw a headline and supporting line at the top.
  3. Mask out the device notification icons and the AdMob validator overlay
     from the raw capture, then place the phone screenshot.
  4. Add a brand strip at the bottom.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops

RAW = "h:/new-app/play-store/screenshots/raw"
OUT_DIR = "h:/new-app/play-store/screenshots/phone"
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1242, 2208

BG_DEEP = (8, 14, 50)
BG_BRAND = (45, 79, 224)
BG_HILITE = (118, 158, 255)
WHITE = (255, 255, 255)
CYAN = (135, 220, 255)
WARN = (255, 86, 96)
GREEN = (76, 217, 168)
AMBER = (255, 211, 105)
PHONE_BG = (16, 18, 24)

# Headline data: (raw_filename, headline, subheadline, accent)
SLIDES = [
    ("01_home.png",
     "Paste any scam.\nGet an answer in 3 seconds.",
     "Free, on-device AI. No account. No upload.",
     CYAN),
    ("03_scanning.png",
     "100% on your phone.\nNothing ever uploaded.",
     "Your messages literally cannot leak — we never receive them.",
     GREEN),
    ("04_result_scam.png",
     "Clear verdict.\nIn plain English.",
     "Suspicious • Likely Scam • Looks Safe — with a confidence score.",
     WARN),
    ("05_result_scam_detail.png",
     "Know exactly\nwhat to do next.",
     "Red flags highlighted. Step-by-step actions you can take right now.",
     AMBER),
    ("09_result_safe.png",
     "Peace of mind\nfor real messages too.",
     "When it's legit, you'll know — fast.",
     GREEN),
    ("11_library_detail.png",
     "Learn the 12 top\nscam patterns of 2026.",
     "Phishing, AI voice cloning, IRS, USPS, romance, crypto…",
     CYAN),
    ("07_history.png",
     "Your last 50 scans.\nLocal. Private. Yours.",
     "Stored only on your device. Delete anytime.",
     AMBER),
    ("08_settings.png",
     "Free forever.\nWorks offline.",
     "No subscription. No paywall. No ads in the scanner.",
     CYAN),
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
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius=radius, fill=255)
    return mask


def clean_raw(im):
    """Remove the device notification strip + AdMob validator + test ad banner.

    Strategy: sample the page background near both top and validator regions,
    then paint matching solid rectangles. This leaves a subtle "empty" stripe
    where the validator was, which is acceptable for marketing.
    """
    iw, ih = im.size  # 1008 x 2244

    # Sample background colors at safe spots
    page_bg_top = im.getpixel((10, 110))      # below the status bar
    page_bg_mid = im.getpixel((10, 1900))     # above the bottom nav

    d = ImageDraw.Draw(im)

    # Top status bar — preserve wifi/signal/battery on the right; clear
    # leftmost notification icons by painting matching bg
    d.rectangle((0, 0, int(iw * 0.55), 90), fill=page_bg_top)

    # Validator dialog region (~y=1490..1830) — paint with bg
    d.rectangle((0, 1490, iw, 1840), fill=page_bg_mid)

    return im


def headline(out, lines, accent_color):
    """Draw a multi-line headline at the top of the canvas with brand styling."""
    d = ImageDraw.Draw(out)
    # Eyebrow brand
    fnt_eye = font(28, True)
    eyebrow = "SCAMRADAR"
    d.text((68, 70), eyebrow, fill=accent_color + (255,), font=fnt_eye)
    # Headline
    fnt_h = font(82, True)
    y = 130
    for line in lines.split("\n"):
        d.text((68, y), line, fill=(255, 255, 255, 255), font=fnt_h)
        y += 96
    return y


def subheadline(out, y, text, color=(220, 230, 255)):
    d = ImageDraw.Draw(out)
    fnt_s = font(32)
    # word-wrap to fit width of 1106px (canvas 1242 minus 68 margin each side)
    max_w = W - 136
    words = text.split()
    lines = []
    cur = ""
    for w in words:
        test = (cur + " " + w).strip()
        if d.textbbox((0, 0), test, font=fnt_s)[2] <= max_w:
            cur = test
        else:
            lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    yy = y + 14
    for line in lines[:3]:
        d.text((68, yy), line, fill=color + (255,) if len(color) == 3 else color,
               font=fnt_s)
        yy += 42
    return yy


def add_phone_frame(canvas, screenshot, top, width):
    """Place the cleaned screenshot inside a soft phone frame on the canvas."""
    sw, sh = screenshot.size
    target_w = width
    scale = target_w / sw
    target_h = int(sh * scale)
    resized = screenshot.resize((target_w, target_h), Image.LANCZOS).convert("RGBA")

    # Rounded corners
    mask = round_rect_mask((target_w, target_h), radius=56)
    resized.putalpha(mask)

    # Soft shadow underneath
    shadow = Image.new("RGBA", (target_w + 80, target_h + 80), (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 180),
                 (40, 50, 40 + target_w, 50 + target_h),
                 round_rect_mask((target_w, target_h), 56))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))

    cx_x = (W - target_w) // 2
    canvas.alpha_composite(shadow, (cx_x - 40, top - 50))
    canvas.alpha_composite(resized, (cx_x, top))

    # Subtle device border
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle(
        (cx_x - 2, top - 2, cx_x + target_w + 1, top + target_h + 1),
        radius=58, outline=(255, 255, 255, 30), width=2,
    )
    return top + target_h


def render_slide(raw_name, head, sub, accent):
    base = radial((W, H), BG_HILITE, BG_DEEP, cx=W * 0.2, cy=H * 0.15).convert("RGBA")
    wash = Image.new("RGBA", (W, H), BG_BRAND + (210,))
    out = Image.alpha_composite(base, wash)

    # Background radar arcs (very subtle)
    arcs = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ad = ImageDraw.Draw(arcs)
    for r, a in [(640, 14), (520, 18), (400, 22), (280, 28)]:
        ad.ellipse([W / 2 - r, H * 0.55 - r, W / 2 + r, H * 0.55 + r],
                   outline=(255, 255, 255, a), width=3)
    out.alpha_composite(arcs)

    y_head_end = headline(out, head, accent)
    y_sub_end = subheadline(out, y_head_end, sub)

    raw = Image.open(os.path.join(RAW, raw_name)).convert("RGB").copy()
    raw = clean_raw(raw)

    phone_top = y_sub_end + 30
    phone_width = int(W * 0.78)
    add_phone_frame(out, raw, phone_top, phone_width)

    # Bottom brand strip
    d = ImageDraw.Draw(out)
    strip_h = 90
    d.rectangle((0, H - strip_h, W, H), fill=(8, 14, 50, 255))
    fnt_b = font(28, True)
    fnt_bm = font(28)
    bottom_left = "ScamRadar"
    bottom_right = "AI Scam & Phishing Detector"
    d.text((68, H - strip_h + 28), bottom_left, fill=CYAN + (255,), font=fnt_b)
    bx = d.textbbox((0, 0), bottom_right, font=fnt_bm)[2]
    d.text((W - 68 - bx, H - strip_h + 28), bottom_right,
           fill=(220, 230, 255, 255), font=fnt_bm)

    return out.convert("RGB")


def main():
    for i, (raw, h, s, a) in enumerate(SLIDES, 1):
        try:
            img = render_slide(raw, h, s, a)
            out = os.path.join(OUT_DIR, f"phone_{i:02d}.png")
            img.save(out, optimize=True)
            print("Saved", out)
        except Exception as e:
            print("Skipped", raw, "due to", e)


if __name__ == "__main__":
    main()
