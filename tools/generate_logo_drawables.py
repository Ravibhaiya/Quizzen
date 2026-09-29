#!/usr/bin/env python3
"""Generate the Android logo/launcher/splash vector drawables from the single source `design/quizzen-logo.svg`.

Usage (from the repository root, Python 3.9+, no dependencies):

    python3 tools/generate_logo_drawables.py

Outputs (all under app/src/main/res/drawable, marked GENERATED - never edit them by hand):
    ic_logo.xml                   full logo art, used in the app header
    ic_splash_icon.xml            full logo art centred in the 288 dp splash-screen canvas
    ic_launcher_background.xml    gradient + swoosh, full bleed (adaptive icon background)
    ic_launcher_foreground.xml    wordmark scaled into the adaptive-icon safe zone
    ic_launcher_monochrome.xml    single-colour wordmark for themed icons (Android 13+)

Limitation: VectorDrawable has no blur filter, so the soft drop shadow under the letters in the SVG is not reproduced.
"""
import math
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SVG_PATH = ROOT / "design" / "quizzen-logo.svg"
OUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"
SVG_NS = "{http://www.w3.org/2000/svg}"

ART_SIZE = 1254.0            # SVG viewBox edge
LAUNCHER_WORDMARK_DP = 64.0  # wordmark width inside the 108 dp adaptive icon (safe zone is 66 dp)
LAUNCHER_CANVAS_DP = 108.0
SPLASH_CANVAS_DP = 288.0
SPLASH_ART_DP = 136.0        # a 136 dp square has a 192 dp diagonal = the splash icon's circular safe zone


def tag(el):
    return el.tag.replace(SVG_NS, "")


def load_svg():
    root = ET.parse(SVG_PATH).getroot()
    gradients = {}
    for g in root.iter(SVG_NS + "linearGradient"):
        gradients[g.get("id")] = {
            "x1": g.get("x1"), "y1": g.get("y1"), "x2": g.get("x2"), "y2": g.get("y2"),
            "stops": [(s.get("offset"), s.get("stop-color")) for s in g.findall(SVG_NS + "stop")],
        }
    clip_d = root.find(f".//{SVG_NS}clipPath/{SVG_NS}path").get("d")
    art = {"clip": clip_d, "glyphs": [], "gradients": gradients}
    for el in root:
        name = tag(el)
        if name == "g" and el.get("clip-path"):
            for child in el:
                fill = child.get("fill", "")
                if tag(child) == "rect":
                    art["background"] = fill[5:-1]
                elif tag(child) == "path":
                    art["swoosh"] = (child.get("d"), fill[5:-1])
        elif name == "g" and el.get("fill", "").upper() == "#FFFFFF" and not el.get("filter"):
            art["glyphs"] = [p.get("d") for p in el]
        elif name == "path" and el.get("fill", "").startswith("url("):
            art.setdefault("accents", []).append((el.get("d"), el.get("fill")[5:-1]))
    missing = [k for k in ("background", "swoosh", "accents") if k not in art] + ([] if art["glyphs"] else ["glyphs"])
    if missing:
        raise SystemExit(f"Unexpected SVG structure, missing: {missing}")
    return art


def bbox(path_datas):
    """Bounding box of absolute M/L/C/Z path data (Bezier curves are sampled)."""
    xs, ys = [], []
    for d in path_datas:
        tokens = re.findall(r"[MLCZ]|-?\d*\.?\d+", d)
        i, cur = 0, (0.0, 0.0)
        while i < len(tokens):
            cmd = tokens[i]
            if cmd not in "MLCZ":
                raise SystemExit(f"Unsupported path command near {tokens[i]!r}")
            i += 1
            if cmd in "ML":
                cur = (float(tokens[i]), float(tokens[i + 1])); i += 2
                xs.append(cur[0]); ys.append(cur[1])
            elif cmd == "C":
                p0 = cur
                p1 = (float(tokens[i]), float(tokens[i + 1]))
                p2 = (float(tokens[i + 2]), float(tokens[i + 3]))
                p3 = (float(tokens[i + 4]), float(tokens[i + 5])); i += 6
                for k in range(1, 9):
                    t = k / 8
                    u = 1 - t
                    xs.append(u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0])
                    ys.append(u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1])
                cur = p3
    return min(xs), min(ys), max(xs), max(ys)


def num(v):
    v = round(float(v), 3)
    return str(int(v)) if v == int(v) else str(v)


class Writer:
    def __init__(self, art):
        self.art = art
        self.lines = []

    def emit(self, indent, text):
        self.lines.append("    " * indent + text)

    def path(self, indent, d, fill=None, gradient=None, even_odd=False):
        attrs = [f'android:pathData="{d}"']
        if even_odd:
            attrs.append('android:fillType="evenOdd"')
        if fill:
            attrs.append(f'android:fillColor="{fill}"')
        if gradient is None:
            self.emit(indent, "<path " + " ".join(attrs) + " />")
            return
        g = self.art["gradients"][gradient]
        self.emit(indent, "<path " + " ".join(attrs) + ">")
        self.emit(indent + 1, '<aapt:attr name="android:fillColor">')
        self.emit(indent + 2, '<gradient android:type="linear" '
                  f'android:startX="{num(g["x1"])}" android:startY="{num(g["y1"])}" '
                  f'android:endX="{num(g["x2"])}" android:endY="{num(g["y2"])}">')
        for offset, color in g["stops"]:
            self.emit(indent + 3, f'<item android:offset="{num(offset)}" android:color="#FF{color.lstrip("#").upper()}" />')
        self.emit(indent + 2, "</gradient>")
        self.emit(indent + 1, "</aapt:attr>")
        self.emit(indent, "</path>")

    def open_group(self, indent, **kw):
        attrs = " ".join(f'android:{k}="{num(v)}"' for k, v in kw.items())
        self.emit(indent, f"<group {attrs}>" if attrs else "<group>")

    def clip(self, indent):
        self.emit(indent, f'<clip-path android:pathData="{self.art["clip"]}" />')

    def backdrop(self, indent):
        self.path(indent, f"M0,0H{num(ART_SIZE)}V{num(ART_SIZE)}H0Z", gradient=self.art["background"])
        d, grad = self.art["swoosh"]
        self.path(indent, d, gradient=grad)

    def wordmark(self, indent, mono=False):
        for d in self.art["glyphs"]:
            self.path(indent, d, fill="#FF000000" if mono else "#FFFFFFFF", even_odd=True)
        for d, grad in self.art["accents"]:
            if mono:
                self.path(indent, d, fill="#FF000000")
            else:
                self.path(indent, d, gradient=grad)


def document(name, size_dp, viewport, body):
    header = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- GENERATED by tools/generate_logo_drawables.py from design/quizzen-logo.svg. Do not edit by hand. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    xmlns:aapt="http://schemas.android.com/aapt"',
        f'    android:width="{num(size_dp)}dp"',
        f'    android:height="{num(size_dp)}dp"',
        f'    android:viewportWidth="{num(viewport)}"',
        f'    android:viewportHeight="{num(viewport)}">',
    ]
    (OUT_DIR / f"{name}.xml").write_text("\n".join(header + body + ["</vector>"]) + "\n", encoding="utf-8")
    print(f"wrote {name}.xml")


def main():
    art = load_svg()
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    def full_art(indent, w):
        w.emit(indent, "<group>")
        w.clip(indent + 1)
        w.backdrop(indent + 1)
        w.emit(indent, "</group>")
        w.wordmark(indent)

    # Header logo: the art as designed.
    w = Writer(art)
    full_art(1, w)
    document("ic_logo", LAUNCHER_CANVAS_DP, ART_SIZE, w.lines)

    # Splash icon: art scaled to SPLASH_ART_DP and centred in a 288 dp canvas.
    viewport = ART_SIZE * SPLASH_CANVAS_DP / SPLASH_ART_DP
    w = Writer(art)
    offset = (viewport - ART_SIZE) / 2
    w.open_group(1, translateX=offset, translateY=offset)
    full_art(2, w)
    w.emit(1, "</group>")
    document("ic_splash_icon", SPLASH_CANVAS_DP, viewport, w.lines)

    # Adaptive icon: full-bleed background.
    w = Writer(art)
    w.backdrop(1)
    document("ic_launcher_background", LAUNCHER_CANVAS_DP, ART_SIZE, w.lines)

    # Adaptive icon foreground / monochrome: wordmark scaled to the safe zone and centred.
    x0, y0, x1, y1 = bbox(art["glyphs"] + [d for d, _ in art["accents"]])
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    scale = (LAUNCHER_WORDMARK_DP / LAUNCHER_CANVAS_DP * ART_SIZE) / (x1 - x0)
    centre = ART_SIZE / 2
    for name, mono in (("ic_launcher_foreground", False), ("ic_launcher_monochrome", True)):
        w = Writer(art)
        w.open_group(1, scaleX=scale, scaleY=scale, pivotX=cx, pivotY=cy,
                     translateX=centre - cx, translateY=centre - cy)
        w.wordmark(2, mono=mono)
        w.emit(1, "</group>")
        document(name, LAUNCHER_CANVAS_DP, ART_SIZE, w.lines)


if __name__ == "__main__":
    main()
