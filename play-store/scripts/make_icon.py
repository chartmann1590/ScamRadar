"""Generate Play Store 512x512 app icon for ScamRadar.

Mirrors the in-app ic_launcher (blue background, white shield, blue radar
donut + scan beam) but rendered at marketing fidelity.
"""
import math
from PIL import Image, ImageDraw, ImageFilter

SIZE = 1024  # render @ 2x then downscale
OUT_512 = "h:/new-app/play-store/icon/icon_512.png"
OUT_1024 = "h:/new-app/play-store/icon/icon_1024.png"

BG_DEEP = (16, 26, 90)
BG_BRAND = (45, 79, 224)        # #2D4FE0 — exact brand color
BG_HILITE = (96, 138, 255)
WHITE = (255, 255, 255)
CYAN = (135, 220, 255)
SHIELD_SHADOW = (8, 16, 64)


def radial_gradient(size, inner, outer):
    img = Image.new("RGB", (size, size), outer)
    px = img.load()
    cx = cy = size / 2
    max_d = math.hypot(cx, cy)
    for y in range(size):
        for x in range(size):
            d = min(1.0, math.hypot(x - cx, y - cy) / max_d)
            r = int(inner[0] * (1 - d) + outer[0] * d)
            g = int(inner[1] * (1 - d) + outer[1] * d)
            b = int(inner[2] * (1 - d) + outer[2] * d)
            px[x, y] = (r, g, b)
    return img


def shield_polygon(cx, cy, w, h):
    """Classic heater shield with rounded shoulders and pointed bottom."""
    pts = []
    # Build with bezier-ish approximation using many points
    # Top edge — slight arch
    top_y = cy - h * 0.45
    shoulder_y = cy - h * 0.30
    mid_y = cy + h * 0.05
    tip_y = cy + h * 0.55
    left = cx - w / 2
    right = cx + w / 2
    pts.append((cx, top_y - 6))                # top mid
    pts.append((right - 4, top_y + 10))        # top right
    pts.append((right, shoulder_y))            # right shoulder
    pts.append((right - 4, mid_y))             # right mid
    pts.append((right - 26, cy + h * 0.28))    # right curve to tip
    pts.append((cx, tip_y))                    # bottom tip
    pts.append((left + 26, cy + h * 0.28))     # left curve to tip
    pts.append((left + 4, mid_y))              # left mid
    pts.append((left, shoulder_y))             # left shoulder
    pts.append((left + 4, top_y + 10))         # top left
    return pts


def make_icon():
    base = radial_gradient(SIZE, BG_HILITE, BG_DEEP).convert("RGBA")

    # Solid brand-blue base mixed with the gradient for a vivid look
    brand = Image.new("RGBA", (SIZE, SIZE), BG_BRAND + (230,))
    out = Image.alpha_composite(base, brand)

    cx = cy = SIZE / 2

    # Radar rings behind the shield
    rings = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rings)
    for r, a, w in [(440, 28, 4), (360, 38, 4), (280, 52, 5)]:
        rd.ellipse([cx - r, cy - r, cx + r, cy + r],
                   outline=(255, 255, 255, a), width=w)
    rings = rings.filter(ImageFilter.GaussianBlur(0.5))
    out = Image.alpha_composite(out, rings)

    # Drop shadow for shield
    SW, SH = 460, 580
    shield_pts = shield_polygon(cx, cy - 14, SW, SH)
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.polygon([(x + 4, y + 18) for (x, y) in shield_pts],
               fill=SHIELD_SHADOW + (140,))
    shadow = shadow.filter(ImageFilter.GaussianBlur(14))
    out = Image.alpha_composite(out, shadow)

    # White shield
    shield = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(shield).polygon(shield_pts, fill=WHITE + (255,))
    out = Image.alpha_composite(out, shield)

    # Gentle top-left highlight on the shield
    hl = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hl)
    hl_pts = [
        (cx - SW / 2 + 4, cy - SH * 0.30),
        (cx,              cy - SH * 0.45 - 6),
        (cx - 6,          cy + 24),
        (cx - SW / 2 + 8, cy + 14),
    ]
    hd.polygon(hl_pts, fill=(255, 255, 255, 90))
    hl = hl.filter(ImageFilter.GaussianBlur(6))
    out = Image.alpha_composite(out, hl)

    # Radar donut inside the shield (blue ring)
    donut = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    dd = ImageDraw.Draw(donut)
    icx, icy = cx, cy - 24
    outer_r = 130
    inner_r = 88
    # Outer fill (ring background)
    dd.ellipse([icx - outer_r, icy - outer_r, icx + outer_r, icy + outer_r],
               fill=BG_BRAND + (255,))
    # Inner cutout
    dd.ellipse([icx - inner_r, icy - inner_r, icx + inner_r, icy + inner_r],
               fill=(0, 0, 0, 0))
    # The above won't actually cut because we're filling; use alpha mask
    donut2 = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(donut2)
    d2.ellipse([icx - outer_r, icy - outer_r, icx + outer_r, icy + outer_r],
               fill=BG_BRAND + (255,))
    mask = Image.new("L", (SIZE, SIZE), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.ellipse([icx - inner_r, icy - inner_r, icx + inner_r, icy + inner_r],
                  fill=255)
    # Punch the hole
    donut2.putalpha(
        Image.eval(
            Image.merge("L", [donut2.split()[3]]),
            lambda v: v,
        )
    )
    # Subtract mask from alpha
    a = donut2.split()[3]
    a = Image.eval(a, lambda v: v)
    # We'll just build a fresh ring via two ellipses with subtraction:
    ring_layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    rlayer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(rlayer)
    rdraw.ellipse([icx - outer_r, icy - outer_r, icx + outer_r, icy + outer_r],
                  fill=BG_BRAND + (255,))
    # Hole
    hole = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    hdraw = ImageDraw.Draw(hole)
    hdraw.ellipse([icx - inner_r, icy - inner_r, icx + inner_r, icy + inner_r],
                  fill=(255, 255, 255, 255))
    # Use hole as alpha mask: set those pixels in rlayer to transparent
    rl_a = rlayer.split()[3]
    h_a = hole.split()[3]
    new_a = Image.eval(rl_a, lambda v: v)
    # Subtract: new_a = rl_a where h_a==0 else 0
    from PIL import ImageChops
    inverted_hole = ImageChops.invert(h_a)
    new_a = ImageChops.multiply(rl_a, inverted_hole)
    rlayer.putalpha(new_a)
    out = Image.alpha_composite(out, rlayer)

    # Scan beam — bright cyan line from inner-ring center to upper-right
    beam = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    bd = ImageDraw.Draw(beam)
    ang = math.radians(-58)  # upper-right
    bx = icx + math.cos(ang) * (outer_r + 80)
    by = icy + math.sin(ang) * (outer_r + 80)
    # Outer glow line
    bd.line([icx, icy, bx, by], fill=(180, 230, 255, 160), width=22)
    bd.line([icx, icy, bx, by], fill=WHITE + (255,), width=10)
    beam_glow = beam.filter(ImageFilter.GaussianBlur(10))
    out = Image.alpha_composite(out, beam_glow)
    out = Image.alpha_composite(out, beam)

    # Sweep dot at the beam tip
    dot = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(dot).ellipse(
        [bx - 18, by - 18, bx + 18, by + 18], fill=CYAN + (255,)
    )
    dot = dot.filter(ImageFilter.GaussianBlur(2))
    out = Image.alpha_composite(out, dot)

    # Soft vignette at bottom
    vg = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    vd = ImageDraw.Draw(vg)
    vd.ellipse([cx - 500, SIZE - 160, cx + 500, SIZE + 140], fill=(0, 0, 0, 90))
    vg = vg.filter(ImageFilter.GaussianBlur(40))
    out = Image.alpha_composite(out, vg)

    out_rgb = out.convert("RGB")
    out_rgb.save(OUT_1024, optimize=True)
    out_rgb.resize((512, 512), Image.LANCZOS).save(OUT_512, optimize=True)
    print("Icon saved at 512 and 1024.")


if __name__ == "__main__":
    make_icon()
