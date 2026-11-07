"""Build a local review page from the actual Roborazzi captures (no mock screens)."""
from pathlib import Path
from html import escape

root = Path(__file__).resolve().parents[1]
reports = root / "app" / "build" / "reports"
screens = ["home", "game", "large-board", "settings", "pause", "tutorial", "demo", "victory", "time-up", "reward", "restart"]
sections = []
for screen in screens:
    cards = []
    for theme, name in [("orbital", "Orbital Glass · Dark"), ("pop", "Pop Machine · Light")]:
        relative = f"theme-screenshots/{theme}-{screen}.png"
        if not (reports / relative).is_file():
            raise SystemExit("Missing screenshot: " + relative + "; run recordRoborazziDebug first")
        cards.append(f'<article><h3>{name}</h3><img alt="{escape(name + " " + screen)}" src="{relative}"></article>')
    sections.append(f'<section><h2>{escape(screen.replace("-", " ").title())}</h2>{"".join(cards)}</section>')
page = """<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Equilibrium · Theme review</title>
<style>
body{margin:0;padding:32px;font:16px system-ui;background:#0c1020;color:#edf2ff}
h1{font-size:28px;margin:0}p{color:#b2bfd9}section{margin-top:36px}h2{font-size:20px}
article{display:inline-block;vertical-align:top;width:calc(50% - 18px);margin:0 12px 16px 0;background:#1a2338;padding:12px;box-sizing:border-box;border-radius:18px}
img{display:block;width:100%;height:auto;border-radius:10px}h3{margin:0 0 12px;font-size:16px}
@media(min-width:900px){article{max-width:360px}}@media(max-width:550px){body{padding:16px}article{width:100%}}
</style><h1>Equilibrium — Orbital Glass &amp; Pop Machine</h1>
<p>Rendered Android Compose screens from the app's UI tests.</p>
""" + "\n".join(sections) + "</html>"
target = reports / "ui-review.html"
target.write_text(page)
print(target)
