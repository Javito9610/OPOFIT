"""Genera PNG del logo OpoFit (in-app) y foreground del launcher con zona segura."""
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    raise SystemExit("pip install pillow")

NAVY = (27, 42, 74, 255)
WHITE = (255, 255, 255, 255)
ORANGE = (255, 145, 0, 255)
TRANSPARENT = (0, 0, 0, 0)

SIZE = 512
SAFE_RATIO = 0.58


def shield(draw, inset, fill, size=SIZE):
    w = size
    cx = w / 2
    top = w * (8 / 108) + inset
    side = w * (20 / 108) + inset
    right = w * (88 / 108) - inset
    mid = w * (53 / 108)
    bottom = w * (96 / 108) - inset
    pts = [
        (cx, top),
        (right, w * (21 / 108) + inset),
        (right, mid),
        (right, w * (74 / 108) - inset),
        (w * (74 / 108) - inset, w * (90 / 108) - inset),
        (cx, bottom),
        (w * (34 / 108) + inset, w * (90 / 108) - inset),
        (side, w * (74 / 108) - inset),
        (side, mid),
        (side, w * (21 / 108) + inset),
    ]
    draw.polygon(pts, fill=fill)


def draw_logo(draw, size=SIZE, offset_x=0, offset_y=0):
    s = size / 108.0

    def pt(x, y):
        return (offset_x + x * s, offset_y + y * s)

    shield(
        draw,
        0,
        WHITE,
        size=size,
    )
    shield(draw, int(size * 6 / 108), NAVY, size=size)

    ocx, ocy = 36 * s + offset_x, 54 * s + offset_y
    r_out, r_in = 11 * s, 7 * s
    draw.ellipse((ocx - r_out, ocy - r_out, ocx + r_out, ocy + r_out), fill=WHITE)
    draw.ellipse((ocx - r_in, ocy - r_in, ocx + r_in, ocy + r_in), fill=NAVY)

    draw.rectangle(
        (58 * s + offset_x, 34 * s + offset_y, 80 * s + offset_x, 40 * s + offset_y),
        fill=WHITE,
    )
    draw.rectangle((58 * s + offset_x, 34 * s + offset_y, 64 * s + offset_x, 76 * s + offset_y), fill=WHITE)
    draw.rectangle((64 * s + offset_x, 48 * s + offset_y, 78 * s + offset_x, 54 * s + offset_y), fill=WHITE)

    bolt = [
        pt(49, 26),
        pt(41, 50),
        pt(49, 50),
        pt(44, 74),
        pt(67, 50),
        pt(59, 50),
        pt(65, 26),
    ]
    draw.polygon(bolt, fill=ORANGE)


def logo_full():
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    draw_logo(draw)
    return img


def logo_launcher_safe():
    """Foreground 512px con logo centrado al 58% (zona segura adaptive icon)."""
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    inner = int(SIZE * SAFE_RATIO)
    margin = (SIZE - inner) // 2
    draw_logo(draw, size=inner, offset_x=margin, offset_y=margin)
    return img


def main():
    root = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
    nodpi = root / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)

    logo_full().save(nodpi / "ic_opofit_logo.png", "PNG")
    logo_launcher_safe().save(nodpi / "ic_launcher_foreground_asset.png", "PNG")
    print(f"OK: {nodpi / 'ic_opofit_logo.png'}")
    print(f"OK: {nodpi / 'ic_launcher_foreground_asset.png'}")


if __name__ == "__main__":
    main()
