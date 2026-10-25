"""Generates the mod's hand-made pixel art: the aged paper sheet the trades are written on.
The writing itself uses the vanilla font.

    python tools/make_art.py

Output goes straight into src/main/resources/assets/librarianpreview/. Deterministic: the
same input always gives the same images.
"""

import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "librarianpreview"

PAPER_W, PAPER_H = 30, 24


def paper() -> None:
    rng = random.Random(1740)
    img = Image.new("RGBA", (PAPER_W, PAPER_H), (0, 0, 0, 0))
    base = (60, 50, 40)
    edge = (30, 24, 18)
    stains = [(7, 17, 3.2), (23, 6, 2.4), (19, 19, 1.6)]
    for y in range(PAPER_H):
        for x in range(PAPER_W):
            # Distance to the nearest edge drives the darker, yellowed rim.
            d = min(x, y, PAPER_W - 1 - x, PAPER_H - 1 - y)
            rim = max(0.0, 1.0 - d / 4.0) ** 1.6
            r, g, b = (base[i] + (edge[i] - base[i]) * rim for i in range(3))
            for sx, sy, sr in stains:
                dist = ((x - sx) ** 2 + (y - sy) ** 2) ** 0.5
                if dist < sr:
                    k = 0.28 * (1 - dist / sr)
                    r, g, b = r - 60 * k, g - 70 * k, b - 80 * k
            n = rng.randint(-4, 4)
            colour = tuple(max(0, min(255, int(c + n))) for c in (r, g, b))
            img.putpixel((x, y), colour + (255,))
    # Ragged edge: nibble the outermost pixels, and tear one corner.
    for x in range(PAPER_W):
        for y in (0, PAPER_H - 1):
            if rng.random() < 0.3:
                img.putpixel((x, y), (0, 0, 0, 0))
    for y in range(PAPER_H):
        for x in (0, PAPER_W - 1):
            if rng.random() < 0.3:
                img.putpixel((x, y), (0, 0, 0, 0))
    for y in range(3):
        for x in range(PAPER_W - 3 + y, PAPER_W):
            img.putpixel((x, y), (0, 0, 0, 0))
    out = ASSETS / "textures" / "paper.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


if __name__ == "__main__":
    paper()
    print("wrote", ASSETS / "textures" / "paper.png")
