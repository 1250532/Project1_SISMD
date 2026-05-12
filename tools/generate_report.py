#!/usr/bin/env python3
import markdown
from weasyprint import HTML
from pathlib import Path

root = Path(__file__).resolve().parent.parent
br = root / 'benchmark_report.md'
ih = root / 'image_histograms.md'
before_png = root / 'hist_before.png'
after_png = root / 'hist_after.png'
out_html = root / 'report.html'
out_pdf = root / 'report.pdf'

md = ''
if br.exists():
    md += br.read_text() + '\n\n'
if ih.exists():
    # embed the histogram images near the hist markdown
    md += '## Histograms (visual)\n\n'
    if before_png.exists():
        md += f'![hist before](hist_before.png)\n\n'
    if after_png.exists():
        md += f'![hist after](hist_after.png)\n\n'
    md += ih.read_text()

# convert markdown to HTML
html = markdown.markdown(md, extensions=['tables'])
# simple wrapper
full = f"""
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>Benchmark report</title>
  <style>
    body {{ font-family: sans-serif; margin: 30px; }}
    img {{ max-width: 100%; height: auto; }}
    table {{ border-collapse: collapse; width: 100%; }}
    th, td {{ border: 1px solid #ddd; padding: 6px; font-size: 12px; }}
    th {{ background: #f2f2f2; text-align: left; }}
  </style>
</head>
<body>
{html}
</body>
</html>
"""
out_html.write_text(full, encoding='utf-8')
# render pdf
HTML(string=full, base_url=str(root)).write_pdf(str(out_pdf))
print('Wrote', out_html, out_pdf)
