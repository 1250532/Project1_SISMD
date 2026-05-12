#!/usr/bin/env python3
from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas
from pathlib import Path
import textwrap

root = Path(__file__).resolve().parent.parent
br = root / 'benchmark_report.md'
ih = root / 'image_histograms.md'
before_png = root / 'hist_before.png'
after_png = root / 'hist_after.png'
out_pdf = root / 'report_reportlab.pdf'

c = canvas.Canvas(str(out_pdf), pagesize=A4)
width, height = A4
margin = 40
cursor = height - margin

def draw_text_block(text, max_width=width-2*margin):
    global cursor
    wrapped = []
    for line in text.splitlines():
        wrapped.extend(textwrap.wrap(line, 100))
    for line in wrapped:
        if cursor < margin + 50:
            c.showPage();
            cursor = height - margin
        c.setFont('Helvetica', 9)
        c.drawString(margin, cursor, line)
        cursor -= 12

# Title
c.setFont('Helvetica-Bold', 16)
c.drawString(margin, cursor, 'Benchmark report & histograms')
c.setFont('Helvetica', 10)
c.drawString(margin + 300, cursor, '')
c.translate(0,0)
cursor -= 24

if br.exists():
    draw_text_block(br.read_text())
    cursor -= 12

# Insert histograms images (fit to width)
img_h = 200
if before_png.exists():
    img = ImageReader(str(before_png))
    if cursor < margin + img_h:
        c.showPage(); cursor = height - margin
    c.drawImage(img, margin, cursor - img_h, width=width-2*margin, height=img_h, preserveAspectRatio=True)
    cursor -= img_h + 10
if after_png.exists():
    img = ImageReader(str(after_png))
    if cursor < margin + img_h:
        c.showPage(); cursor = height - margin
    c.drawImage(img, margin, cursor - img_h, width=width-2*margin, height=img_h, preserveAspectRatio=True)
    cursor -= img_h + 10

# add numeric histogram tail (first/last sections)
if ih.exists():
    draw_text_block('\nNumeric histograms:\n')
    draw_text_block(ih.read_text())

c.save()
print('Wrote', out_pdf)
