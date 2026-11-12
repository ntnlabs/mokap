#!/usr/bin/env python3
"""Grafika pre Google Play: ikona 512×512 a feature graphic 1024×500 (SK, EN).

Ikona je presne launcher ikona appky (res/drawable/ic_launcher_foreground.xml + farby z colors.xml),
len vo väčšom rozlíšení. Žiadne logo kyberie. Spustenie: python3 make_graphics.py (Pillow, fonty DejaVu).
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = Path(__file__).resolve().parent
SS = 4  # supersampling, potom zmenšenie – hladké hrany

BG = (0x0B, 0x0F, 0x0C)
GREEN = (0x39, 0xFF, 0x88)
PINK = (0xFF, 0x2E, 0x9A)
TEXT = (0xE6, 0xE6, 0xE6)
DIM = (0x8C, 0x8C, 0x8C)

# Ťahy písmena K z ic_launcher_foreground.xml (viewport 108, strokeWidth 8, square cap).
K_STROKES = [((42, 34), (42, 74)), ((45, 56), (68, 34)), ((51, 50), (70, 74))]
K_WIDTH = 8
VISIBLE = (18, 72)  # adaptívna ikona: viditeľný je stred 72 z 108

FONT_DIR = Path("/usr/share/fonts/truetype/dejavu")


def font(bold: bool, size: int) -> ImageFont.FreeTypeFont:
    name = "DejaVuSansMono-Bold.ttf" if bold else "DejaVuSansMono.ttf"
    return ImageFont.truetype(str(FONT_DIR / name), size)


def stroke(draw: ImageDraw.ImageDraw, a, b, width: float, fill) -> None:
    """Čiara so štvorcovým koncom (ako strokeLineCap="square") – obdĺžnik predĺžený o polovicu šírky."""
    (x1, y1), (x2, y2) = a, b
    length = ((x2 - x1) ** 2 + (y2 - y1) ** 2) ** 0.5
    ux, uy = (x2 - x1) / length, (y2 - y1) / length
    h = width / 2
    px, py = -uy * h, ux * h
    x1, y1, x2, y2 = x1 - ux * h, y1 - uy * h, x2 + ux * h, y2 + uy * h
    draw.polygon([(x1 + px, y1 + py), (x2 + px, y2 + py), (x2 - px, y2 - py), (x1 - px, y1 - py)], fill=fill)


def draw_k(draw: ImageDraw.ImageDraw, left: float, top: float, size: float, fill=GREEN) -> None:
    """Písmeno K tak, ako ho launcher ukáže v štvorci size×size s ľavým horným rohom (left, top)."""
    offset, visible = VISIBLE
    scale = size / visible
    to = lambda p: (left + (p[0] - offset) * scale, top + (p[1] - offset) * scale)
    for a, b in K_STROKES:
        stroke(draw, to(a), to(b), K_WIDTH * scale, fill)


def icon() -> None:
    size = 512 * SS
    img = Image.new("RGB", (size, size), BG)
    draw_k(ImageDraw.Draw(img), 0, 0, size)
    img.resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)


def glow(base: Image.Image, layer: Image.Image, radius: int, strength: float) -> Image.Image:
    """Neónová žiara: rozmazaná kópia vrstvy pod ňou."""
    blurred = layer.filter(ImageFilter.GaussianBlur(radius))
    alpha = blurred.getchannel("A").point(lambda v: int(v * strength))
    blurred.putalpha(alpha)
    return Image.alpha_composite(Image.alpha_composite(base, blurred), layer)


def feature(lang: str, subtitle: str, tagline: str) -> None:
    w, h = 1024 * SS, 500 * SS
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))

    # Jemný prechod a mriežka v pozadí.
    bg = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        bg.line([(0, y), (w, y)], fill=tuple(int(c * (0.35 + 0.65 * t)) for c in BG) + (255,))
    grid = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    g = ImageDraw.Draw(grid)
    step = 32 * SS
    for x in range(0, w, step):
        g.line([(x, 0), (x, h)], fill=GREEN + (14,), width=SS)
    for y in range(0, h, step):
        g.line([(0, y), (w, y)], fill=GREEN + (14,), width=SS)
    img = Image.alpha_composite(img, grid)

    title_font = font(True, 128 * SS)
    sub_font = font(False, 30 * SS)
    tag_font = font(False, 24 * SS)
    tile = 200 * SS
    gap = 56 * SS
    probe = ImageDraw.Draw(img)
    title_w = probe.textlength("mokap", font=title_font)
    sub_w = probe.textlength(subtitle, font=sub_font)
    tag_w = probe.textlength(tagline, font=tag_font)
    text_w = max(title_w, sub_w, tag_w)
    left = (w - (tile + gap + text_w)) / 2
    tile_top = (h - tile) / 2

    # Dlaždica s ikonou (rovnaké K ako launcher).
    tile_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    t = ImageDraw.Draw(tile_layer)
    t.rounded_rectangle([left, tile_top, left + tile, tile_top + tile], radius=44 * SS, fill=BG + (255,),
                        outline=GREEN + (110,), width=3 * SS)
    draw_k(t, left, tile_top, tile)
    img = glow(img, tile_layer, 18 * SS, 0.55)

    # Názov, ružová linka, podtitul a čo appka vie.
    x = left + tile + gap
    ascent, descent = title_font.getmetrics()
    block_h = ascent + descent + 70 * SS
    top = (h - block_h) / 2 - 24 * SS
    title_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(title_layer).text((x, top), "mokap", font=title_font, fill=GREEN + (255,))
    img = glow(img, title_layer, 16 * SS, 0.7)
    d = ImageDraw.Draw(img)
    line_y = top + ascent + descent + 6 * SS
    d.rectangle([x + 4 * SS, line_y, x + 4 * SS + 120 * SS, line_y + 5 * SS], fill=PINK + (255,))
    d.text((x + 4 * SS, line_y + 26 * SS), subtitle, font=sub_font, fill=TEXT + (255,))
    d.text((x + 4 * SS, line_y + 76 * SS), tagline, font=tag_font, fill=DIM + (255,))

    img.convert("RGB").resize((1024, 500), Image.LANCZOS).save(OUT / f"feature-graphic-{lang}.png", optimize=True)


if __name__ == "__main__":
    icon()
    feature("sk", "neoficiálny klient pre kyberia.sk", "bookmarky · odpovede · K · pošta")
    feature("en", "unofficial client for kyberia.sk", "bookmarks · replies · K · mail")
