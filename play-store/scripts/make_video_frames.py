"""Generate the 9 still frames used for the ScamRadar promo video.

Each frame is 1920x1080 and corresponds to one segment of the voiceover.
The frames are rendered with brand styling so that they look like a
unified promo when cut together via ffmpeg.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops

OUT = "h:/new-app/play-store/video/frames"
RAW = "h:/new-app/play-store/screenshots/raw"
ICON = "h:/new-app/play-store/icon/icon_1024.png"
os.makedirs(OUT, exist_ok=True)

W, H = 1920, 1080
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


def text_size(d, text, fnt):
    bbox = d.textbbox((0, 0), text, font=fnt)
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


def brand_bg(cx=None, cy=None):
    base = radial((W, H), BG_HILITE, BG_DEEP,
                  cx=cx or W * 0.3, cy=cy or H * 0.2).convert("RGBA")
    wash = Image.new("RGBA", (W, H), BG_BRAND + (205,))
    return Image.alpha_composite(base, wash)


def add_radar(out, cx=None, cy=None, scale=1.0):
    rings = Image.new("RGBA", out.size, (0, 0, 0, 0))
    rd = ImageDraw.Draw(rings)
    cx = cx if cx is not None else W / 2
    cy = cy if cy is not None else H / 2
    for r, a in [(int(620 * scale), 20), (int(500 * scale), 26),
                 (int(380 * scale), 34), (int(260 * scale), 44),
                 (int(160 * scale), 60)]:
        rd.ellipse([cx - r, cy - r, cx + r, cy + r],
                   outline=(255, 255, 255, a), width=4)
    out.alpha_composite(rings)


def clean_raw(im):
    iw, ih = im.size
    page_bg_top = im.getpixel((10, 110))
    page_bg_mid = im.getpixel((10, 1900))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, int(iw * 0.55), 90), fill=page_bg_top)
    d.rectangle((0, 1490, iw, 1840), fill=page_bg_mid)
    return im


def phone_panel(out, raw_name, x_center, height_frac=0.85, x_offset=0):
    raw = Image.open(os.path.join(RAW, raw_name)).convert("RGB").copy()
    raw = clean_raw(raw)
    rw, rh = raw.size
    target_h = int(H * height_frac)
    target_w = int(rw * (target_h / rh))
    resized = raw.resize((target_w, target_h), Image.LANCZOS).convert("RGBA")
    mask = round_rect_mask((target_w, target_h), radius=int(target_w * 0.05))
    resized.putalpha(mask)
    px = int(x_center - target_w / 2 + x_offset)
    py = (H - target_h) // 2

    shadow = Image.new("RGBA", (target_w + 160, target_h + 160), (0, 0, 0, 0))
    sh_mask = round_rect_mask((target_w, target_h), int(target_w * 0.05))
    shadow.paste((0, 0, 0, 190), (80, 100, 80 + target_w, 100 + target_h), sh_mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(40))
    out.alpha_composite(shadow, (px - 80, py - 100))
    out.alpha_composite(resized, (px, py))


def title_text(out, head_lines, color=WHITE, x=120, y=120, head_size=80, lead=1.15):
    d = ImageDraw.Draw(out)
    fnt = font(head_size, True)
    for line in head_lines:
        d.text((x, y), line, fill=color + (255,), font=fnt)
        y += int(head_size * lead)
    return y


def eyebrow(out, text, color=CYAN, x=120, y=70, size=28):
    d = ImageDraw.Draw(out)
    fnt = font(size, True)
    d.text((x, y), text, fill=color + (255,), font=fnt)


def scene_1():
    """Got a text you can't tell is real?"""
    out = brand_bg(cx=W * 0.7, cy=H * 0.5)
    add_radar(out, cx=W * 0.7, cy=H * 0.55, scale=1.1)
    phone_panel(out, "01_home.png", x_center=int(W * 0.72), height_frac=0.85)
    eyebrow(out, "SCAMRADAR")
    title_text(out, ["Got a text", "you can't tell", "is real?"], y=140, head_size=88)
    out.convert("RGB").save(os.path.join(OUT, "frame_01.png"))


def scene_2():
    """AI scams jumped twelve hundred percent..."""
    out = brand_bg(cx=W * 0.3, cy=H * 0.3)
    add_radar(out, cx=W * 0.8, cy=H * 0.55, scale=1.2)
    eyebrow(out, "THE NEW WAVE OF AI SCAMS")
    d = ImageDraw.Draw(out)
    fnt_b = font(220, True)
    d.text((120, 200), "1,210%", fill=CYAN + (255,), font=fnt_b)
    fnt_h = font(64, True)
    d.text((120, 460), "increase in AI-generated", fill=WHITE + (255,), font=fnt_h)
    d.text((120, 540), "scam messages in 2025.", fill=WHITE + (255,), font=fnt_h)
    fnt_s = font(36)
    d.text((120, 660),
           "Source: industry threat reports", fill=(210, 220, 255), font=fnt_s)
    # Right side big shield
    icon = Image.open(ICON).convert("RGBA")
    icon = icon.resize((520, 520), Image.LANCZOS)
    mask = round_rect_mask(icon.size, 90)
    icon.putalpha(mask)
    out.alpha_composite(icon, (W - 700, H // 2 - 260))
    out.convert("RGB").save(os.path.join(OUT, "frame_02.png"))


def scene_3():
    """Meet ScamRadar"""
    out = brand_bg(cx=W * 0.5, cy=H * 0.3)
    add_radar(out, cx=W * 0.5, cy=H * 0.55, scale=1.4)
    icon = Image.open(ICON).convert("RGBA")
    icon = icon.resize((420, 420), Image.LANCZOS)
    mask = round_rect_mask(icon.size, 76)
    icon.putalpha(mask)
    # Centered icon at top
    out.alpha_composite(icon, ((W - 420) // 2, 130))
    d = ImageDraw.Draw(out)
    fnt_h = font(140, True)
    text = "ScamRadar"
    tw, _ = text_size(d, text, fnt_h)
    d.text(((W - tw) // 2, 600), text, fill=WHITE + (255,), font=fnt_h)
    fnt_s = font(42, True)
    sub = "Free AI scam detector. On your phone."
    sw, _ = text_size(d, sub, fnt_s)
    d.text(((W - sw) // 2, 800), sub, fill=CYAN + (255,), font=fnt_s)
    out.convert("RGB").save(os.path.join(OUT, "frame_03.png"))


def scene_4():
    """Paste any sketchy message..."""
    out = brand_bg(cx=W * 0.7, cy=H * 0.5)
    add_radar(out, cx=W * 0.72, cy=H * 0.55, scale=1.1)
    phone_panel(out, "01_home.png", x_center=int(W * 0.72), height_frac=0.85)
    eyebrow(out, "ONE TAP")
    title_text(out, ["Paste it.", "Check it.", "Done."], head_size=96, y=140)
    d = ImageDraw.Draw(out)
    fnt_s = font(40)
    d.text((120, 540), "A clear answer in", fill=(210, 220, 255), font=fnt_s)
    d.text((120, 590), "three seconds.", fill=(210, 220, 255), font=fnt_s)
    out.convert("RGB").save(os.path.join(OUT, "frame_04.png"))


def scene_5():
    """Suspicious. Likely scam. Or, peace of mind."""
    out = brand_bg(cx=W * 0.5, cy=H * 0.3)
    add_radar(out, cx=W * 0.5, cy=H * 0.55, scale=1.5)
    # Phone left = scam result, phone right = safe result
    phone_panel(out, "04_result_scam.png",
                x_center=int(W * 0.3), height_frac=0.82)
    phone_panel(out, "09_result_safe.png",
                x_center=int(W * 0.7), height_frac=0.82)
    d = ImageDraw.Draw(out)
    fnt_t = font(56, True)
    title_text(out, ["Likely scam.   Or, peace of mind."],
               head_size=58, x=180, y=60)
    out.convert("RGB").save(os.path.join(OUT, "frame_05.png"))


def scene_6():
    """Everything stays on your phone."""
    out = brand_bg(cx=W * 0.7, cy=H * 0.5)
    add_radar(out, cx=W * 0.72, cy=H * 0.55, scale=1.1)
    phone_panel(out, "03_scanning.png", x_center=int(W * 0.72), height_frac=0.82)
    eyebrow(out, "PRIVATE BY DESIGN", color=GREEN)
    title_text(out, ["Nothing", "ever leaves", "your phone."], head_size=96, y=140)
    d = ImageDraw.Draw(out)
    fnt_s = font(36)
    d.text((120, 660), "On-device AI. No cloud.", fill=(210, 220, 255), font=fnt_s)
    d.text((120, 710), "No account. No tracking.", fill=(210, 220, 255), font=fnt_s)
    out.convert("RGB").save(os.path.join(OUT, "frame_06.png"))


def scene_7():
    """Learn the top scam patterns..."""
    out = brand_bg(cx=W * 0.5, cy=H * 0.3)
    add_radar(out, cx=W * 0.5, cy=H * 0.55, scale=1.5)
    phone_panel(out, "06_library.png",
                x_center=int(W * 0.3), height_frac=0.80)
    phone_panel(out, "07_history.png",
                x_center=int(W * 0.7), height_frac=0.80)
    eyebrow(out, "LEARN & TRACK", color=AMBER, x=600, y=40)
    d = ImageDraw.Draw(out)
    fnt_h = font(52, True)
    d.text((W // 2 - 360, 80), "12 scam patterns. 50 scans. Yours.",
           fill=WHITE + (255,), font=fnt_h)
    out.convert("RGB").save(os.path.join(OUT, "frame_07.png"))


def scene_8():
    """Free forever. No account. No upload."""
    out = brand_bg(cx=W * 0.5, cy=H * 0.4)
    add_radar(out, cx=W * 0.5, cy=H * 0.55, scale=1.6)
    d = ImageDraw.Draw(out)
    fnt_h = font(140, True)
    text = "Free Forever."
    tw, _ = text_size(d, text, fnt_h)
    d.text(((W - tw) // 2, 220), text, fill=WHITE + (255,), font=fnt_h)
    # Three big chips
    chips = [("No Account", CYAN), ("No Upload", GREEN), ("No Paywall", AMBER)]
    fnt_c = font(56, True)
    chip_y = 540
    chip_w = 460
    total_w = chip_w * 3 + 60 * 2
    cx_pos = (W - total_w) // 2
    for label, color in chips:
        chip_layer = Image.new("RGBA", out.size, (0, 0, 0, 0))
        cd = ImageDraw.Draw(chip_layer)
        cd.rounded_rectangle((cx_pos, chip_y, cx_pos + chip_w, chip_y + 130),
                             radius=65, fill=(10, 18, 60, 200),
                             outline=color + (240,), width=4)
        out.alpha_composite(chip_layer)
        # dot
        d.ellipse((cx_pos + 40, chip_y + 50, cx_pos + 80, chip_y + 90),
                  fill=color + (255,))
        tw2, _ = text_size(d, label, fnt_c)
        d.text((cx_pos + 110, chip_y + 30), label,
               fill=WHITE + (255,), font=fnt_c)
        cx_pos += chip_w + 60
    out.convert("RGB").save(os.path.join(OUT, "frame_08.png"))


def scene_9():
    """Don't get scammed. Get ScamRadar."""
    out = brand_bg(cx=W * 0.5, cy=H * 0.3)
    add_radar(out, cx=W * 0.5, cy=H * 0.55, scale=1.7)
    icon = Image.open(ICON).convert("RGBA")
    icon = icon.resize((360, 360), Image.LANCZOS)
    mask = round_rect_mask(icon.size, 64)
    icon.putalpha(mask)
    out.alpha_composite(icon, ((W - 360) // 2, 140))
    d = ImageDraw.Draw(out)
    fnt_h = font(124, True)
    line1 = "Don't get scammed."
    line2 = "Get ScamRadar."
    tw1, _ = text_size(d, line1, fnt_h)
    tw2, _ = text_size(d, line2, fnt_h)
    d.text(((W - tw1) // 2, 540), line1, fill=WHITE + (255,), font=fnt_h)
    d.text(((W - tw2) // 2, 680), line2, fill=CYAN + (255,), font=fnt_h)
    fnt_s = font(44, True)
    cta = "Search “ScamRadar” on Google Play."
    sw, _ = text_size(d, cta, fnt_s)
    d.text(((W - sw) // 2, 880), cta, fill=(220, 230, 255), font=fnt_s)
    out.convert("RGB").save(os.path.join(OUT, "frame_09.png"))


def main():
    scene_1(); print("1 ok")
    scene_2(); print("2 ok")
    scene_3(); print("3 ok")
    scene_4(); print("4 ok")
    scene_5(); print("5 ok")
    scene_6(); print("6 ok")
    scene_7(); print("7 ok")
    scene_8(); print("8 ok")
    scene_9(); print("9 ok")


if __name__ == "__main__":
    main()
