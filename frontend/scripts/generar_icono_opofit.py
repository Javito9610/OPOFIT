"""Genera PNG del icono OpoFit para launcher (adaptive icon foreground)."""
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    raise SystemExit("pip install pillow")

BLUE = (13, 71, 161, 255)
WHITE = (255, 255, 255, 255)
ORANGE = (255, 145, 0, 255)
TRANSPARENT = (0, 0, 0, 0)

SIZE = 512


def shield(draw, inset, fill):
    w = SIZE
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


def main():
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    # Fondo azul cuadrado (launcher background layer es aparte; foreground transparente)
    shield(draw, 0, WHITE)
    shield(draw, int(SIZE * 6 / 108), BLUE)

    s = SIZE / 108.0
    ocx, ocy = 36 * s, 54 * s
    draw.ellipse((ocx - 11 * s, ocy - 11 * s, ocx + 11 * s, ocy + 11 * s), fill=WHITE)
    draw.ellipse((ocx - 7 * s, ocy - 7 * s, ocx + 7 * s, ocy + 7 * s), fill=BLUE)

    # F
    draw.rectangle((58 * s, 34 * s, 80 * s, 40 * s), fill=WHITE)
    draw.rectangle((58 * s, 34 * s, 64 * s, 76 * s), fill=WHITE)
    draw.rectangle((64 * s, 48 * s, 78 * s, 54 * s), fill=WHITE)

    # Rayo
    bolt = [(49 * s, 26 * s), (41 * s, 50 * s), (49 * s, 50 * s), (44 * s, 74 * s), (67 * s, 50 * s), (59 * s, 50 * s), (65 * s, 26 * s)]
    draw.polygon(bolt, fill=ORANGE)

    root = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
    nodpi = root / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    out = nodpi / "ic_opofit_logo.png"
    img.save(out, "PNG")
    print(f"OK: {out}")


if __name__ == "__main__":
    main()
