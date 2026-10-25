"""Generates the mod's hand-made pixel art: the aged paper sheet and the pencil font.

The pencil font is derived from vanilla Minecraft's ascii.png (slanted, with graphite grain),
so pass the path of ascii.png extracted from your own 26.3 client jar:

    python tools/make_art.py <path to ascii.png>

Output goes straight into src/main/resources/assets/librarianpreview/. Deterministic: the
same input always gives the same images.
"""

import json
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "librarianpreview"

# Vanilla ascii.png layout (16 rows of 16 chars), copied from include/default.json.
ASCII_ROWS = [
    "ÀÁÂÈÊËÍÓÔÕÚßãõğİ",
    "ıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    " !\"#$%&'()*+,-./",
    "0123456789:;<=>?",
    "@ABCDEFGHIJKLMNO",
    "PQRSTUVWXYZ[\\]^_",
    "`abcdefghijklmno",
    "pqrstuvwxyz{|}~\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
]
# Only the printable ASCII rows are redrawn; everything else falls back to the vanilla font.
PENCIL_ROWS = range(2, 8)

ARROW = [
    "........",
    "....#...",
    ".....#..",
    "#######.",
    ".....#..",
    "....#...",
    "........",
    "........",
]


def pencil_font(ascii_png: Path) -> None:
    rng = random.Random(1907)
    src = Image.open(ascii_png).convert("RGBA")
    out = Image.new("RGBA", (128, 8 * len(PENCIL_ROWS)), (0, 0, 0, 0))
    for out_row, row in enumerate(PENCIL_ROWS):
        for col in range(16):
            for y in range(8):
                # Slant: the top half of each glyph leans one pixel to the right.
                shift = 1 if y < 4 else 0
                for x in range(8):
                    if src.getpixel((col * 8 + x, row * 8 + y))[3] == 0:
                        continue
                    nx = x + shift
                    if nx >= 8:
                        continue
                    # Graphite: a light grey grain that the text colour tints.
                    shade = rng.randint(205, 255)
                    alpha = rng.randint(165, 255)
                    out.putpixel((col * 8 + nx, out_row * 8 + y), (shade, shade, shade, alpha))
    font_dir = ASSETS / "textures" / "font"
    font_dir.mkdir(parents=True, exist_ok=True)
    out.save(font_dir / "pencil.png")

    arrow = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y, line in enumerate(ARROW):
        for x, c in enumerate(line):
            if c == "#":
                shade = rng.randint(205, 255)
                arrow.putpixel((x, y), (shade, shade, shade, rng.randint(180, 255)))
    arrow.save(font_dir / "pencil_arrow.png")

    providers = [
        {"type": "reference", "id": "minecraft:include/space"},
        {
            "type": "bitmap",
            "file": "librarianpreview:font/pencil.png",
            "ascent": 7,
            "chars": [ASCII_ROWS[r] for r in PENCIL_ROWS],
        },
        {
            "type": "bitmap",
            "file": "librarianpreview:font/pencil_arrow.png",
            "ascent": 7,
            "chars": ["→"],
        },
        {"type": "reference", "id": "minecraft:default"},
    ]
    (ASSETS / "font").mkdir(parents=True, exist_ok=True)
    with open(ASSETS / "font" / "pencil.json", "w", encoding="utf-8", newline="\n") as f:
        json.dump({"providers": providers}, f, indent=2, ensure_ascii=True)
        f.write("\n")


PAPER_W, PAPER_H = 30, 24


def paper() -> None:
    rng = random.Random(1740)
    img = Image.new("RGBA", (PAPER_W, PAPER_H), (0, 0, 0, 0))
    base = (222, 205, 163)
    edge = (176, 148, 98)
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
            n = rng.randint(-7, 7)
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
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    pencil_font(Path(sys.argv[1]))
    paper()
    print("wrote", ASSETS / "textures" / "paper.png", "and the pencil font")
