"""Feature graphic for Play Store (1024x500).

Renders a vivid hero with the brand shield on the left, a clear value
proposition on the right, and three quick-trust chips along the bottom.
"""
import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops

W, H = 1024, 500
OUT = "h:/new-app/play-store/feature-graphic/feature_graphic_1024x500.png"

BG_DEEP = (8, 14, 50)
BG_BRAND = (45, 79, 224)
BG_HILITE = (118, 158, 255)
WHITE = (255, 255, 255)
CYAN = (135, 220, 255)
WARN = (255, 86, 96)
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


def shield_polygon(cx, cy, w, h):
    left = cx - w / 2
    right = cx + w / 2
    return [
        (cx, cy - h * 0.45 - 6),
        (right - 3, cy - h * 0.45 + 8),
        (right, cy - h * 0.30),
        (right - 4, cy + h * 0.05),
        (right - 22, cy + h * 0.28),
        (cx, cy + h * 0.55),
        (left + 22, cy + h * 0.28),
        (left + 4, cy + h * 0.05),
        (left, cy - h * 0.30),
        (left + 3, cy - h * 0.45 + 8),
    ]


def draw_shield(out, cx, cy, w_s, h_s):
    pts = shield_polygon(cx, cy, w_s, h_s)
    shadow = Image.new("RGBA", out.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(
        [(x + 4, y + 14) for (x, y) in pts], fill=(0, 0, 0, 130))
    shadow = shadow.filter(ImageFilter.GaussianBlur(12))
    out.alpha_composite(shadow)

    sh = Image.new("RGBA", out.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).polygon(pts, fill=WHITE + (255,))
    out.alpha_composite(sh)

    icx, icy = cx, cy - 10
    outer_r, inner_r = int(w_s * 0.30), int(w_s * 0.20)
    rlayer = Image.new("RGBA", out.size, (0, 0, 0, 0))
    ImageDraw.Draw(rlayer).ellipse(
        [icx - outer_r, icy - outer_r, icx + outer_r, icy + outer_r],
        fill=BG_BRAND + (255,))
    hole = Image.new("L", out.size, 0)
    ImageDraw.Draw(hole).ellipse(
        [icx - inner_r, icy - inner_r, icx + inner_r, icy + inner_r], fill=255)
    rl_a = rlayer.split()[3]
    new_a = ImageChops.multiply(rl_a, ImageChops.invert(hole))
    rlayer.putalpha(new_a)
    out.alpha_composite(rlayer)

    beam = Image.new("RGBA", out.size, (0, 0, 0, 0))
    bd = ImageDraw.Draw(beam)
    ang = math.radians(-58)
    bx = icx + math.cos(ang) * (outer_r + int(w_s * 0.20))
    by = icy + math.sin(ang) * (outer_r + int(w_s * 0.20))
    bd.line([icx, icy, bx, by], fill=(180, 230, 255, 160), width=int(w_s*0.06))
    bd.line([icx, icy, bx, by], fill=WHITE + (255,), width=int(w_s*0.028))
    out.alpha_composite(beam.filter(ImageFilter.GaussianBlur(8)))
    out.alpha_composite(beam)

    dot = Image.new("RGBA", out.size, (0, 0, 0, 0))
    rdot = int(w_s * 0.05)
    ImageDraw.Draw(dot).ellipse(
        [bx - rdot, by - rdot, bx + rdot, by + rdot], fill=CYAN + (255,))
    out.alpha_composite(dot)


def make():
    base = radial((W, H), BG_HILITE, BG_DEEP, cx=200, cy=180).convert("RGBA")
    wash = Image.new("RGBA", (W, H), BG_BRAND + (200,))
    out = Image.alpha_composite(base, wash)

    # Radar rings around shield (left side)
    rings = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rings)
    cx, cy = 210, 230
    for r, a in [(230, 26), (180, 38), (130, 54), (85, 70)]:
        rd.ellipse([cx - r, cy - r, cx + r, cy + r],
                   outline=(255, 255, 255, a), width=3)
    out.alpha_composite(rings)

    draw_shield(out, cx, cy, 200, 260)

    d = ImageDraw.Draw(out)

    # Right column
    rx = 430

    # Eyebrow tag
    tag = "AI SCAM & PHISHING DETECTOR"
    fnt_tag = font(20, True)
    tw, th = text_size(d, tag, fnt_tag)
    pad_x, pad_y = 16, 8
    d.rounded_rectangle(
        (rx, 56, rx + tw + pad_x * 2, 56 + th + pad_y * 2 + 4),
        radius=22, fill=(255, 255, 255, 28),
        outline=(255, 255, 255, 160), width=2)
    d.text((rx + pad_x, 58 + pad_y - 2), tag, fill=CYAN, font=fnt_tag)

    # Headline — 3 lines
    fnt_h1 = font(58, True)
    fnt_h2 = font(58, True)
    y = 116
    d.text((rx, y), "Paste it. Check it.", fill=WHITE, font=fnt_h1)
    y += 68
    d.text((rx, y), "Don't get scammed.", fill=CYAN, font=fnt_h2)

    # Subhead
    fnt_sub = font(22)
    sub = "On-device AI catches scam texts, emails, and voicemails in 3 seconds."
    # wrap to two lines if needed
    y = 268
    d.text((rx, y), "On-device AI catches scam texts, emails,", fill=(220, 230, 255), font=fnt_sub)
    d.text((rx, y + 30), "and voicemails in 3 seconds. 100% private.", fill=(220, 230, 255), font=fnt_sub)

    # Bottom trust chips
    chips = [("100% On-Device", GREEN),
             ("No Account", CYAN),
             ("Free Forever", AMBER)]
    fnt_chip = font(20, True)
    chip_y = 396
    cx_pos = rx
    for label, color in chips:
        tw2, _ = text_size(d, label, fnt_chip)
        chip_w = tw2 + 50
        # Use a darker translucent chip via a fresh RGBA layer (so alpha works)
        chip_layer = Image.new("RGBA", out.size, (0, 0, 0, 0))
        cd = ImageDraw.Draw(chip_layer)
        cd.rounded_rectangle(
            (cx_pos, chip_y - 22, cx_pos + chip_w, chip_y + 22),
            radius=22, fill=(10, 18, 60, 160),
            outline=color + (240,), width=2)
        out.alpha_composite(chip_layer)
        d.ellipse((cx_pos + 14, chip_y - 6, cx_pos + 26, chip_y + 6),
                  fill=color + (255,))
        d.text((cx_pos + 36, chip_y - 13), label, fill=(255, 255, 255, 255),
               font=fnt_chip)
        cx_pos += chip_w + 12

    out_rgb = out.convert("RGB")
    out_rgb.save(OUT, optimize=True)
    print("Saved feature graphic at", OUT)


if __name__ == "__main__":
    make()
