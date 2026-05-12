#!/usr/bin/env python3
import re
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

root = Path(__file__).resolve().parent.parent
md = root / 'image_histograms.md'

if not md.exists():
    print('image_histograms.md not found')
    raise SystemExit(1)

text = md.read_text()

# parse the Before and After tables
sections = re.split(r'^##\s+', text, flags=re.M)
before = [0]*256
after = [0]*256
for sec in sections:
    if sec.strip().lower().startswith('before'):
        # find lines like: 132 | 1
        for line in sec.splitlines():
            m = re.match(r'\s*(\d+)\s*\|\s*(\d+)', line)
            if m:
                i = int(m.group(1))
                v = int(m.group(2))
                if 0 <= i <= 255:
                    before[i] = v
    if sec.strip().lower().startswith('after'):
        for line in sec.splitlines():
            m = re.match(r'\s*(\d+)\s*\|\s*(\d+)', line)
            if m:
                i = int(m.group(1))
                v = int(m.group(2))
                if 0 <= i <= 255:
                    after[i] = v

# helper to plot
def plot_hist(data, title, outname):
    x = list(range(256))
    y = data
    fig, ax = plt.subplots(figsize=(10,4))
    ax.bar(x, y, width=1.0, color='gray', edgecolor='black')
    ax.set_xlim(-1,256)
    ax.set_xlabel('Pixel intensity (0-255)')
    ax.set_ylabel('Frequency (pixel count)')
    ax.set_title(title)
    # set x ticks every 32
    xticks = list(range(0,256,32))
    ax.set_xticks(xticks)
    # show y ticks nicely
    ax.ticklabel_format(axis='y', style='plain')
    # annotate max value
    maxv = max(y)
    ax.text(0.99, 0.95, f'max={maxv}', transform=ax.transAxes, ha='right', va='top', fontsize=9)
    fig.tight_layout()
    fig.savefig(root / outname, dpi=150)
    plt.close(fig)

plot_hist(before, 'Histogram BEFORE (source image)', 'hist_before_precise.png')
plot_hist(after, 'Histogram AFTER (histogram-equalized)', 'hist_after_precise.png')

# also create an overlayed normalized histogram for comparison
import numpy as np
x = np.arange(256)
fig, ax = plt.subplots(figsize=(10,4))
ax.plot(x, before, label='Before', color='blue')
ax.plot(x, after, label='After', color='red')
ax.set_xlim(0,255)
ax.set_xlabel('Pixel intensity (0-255)')
ax.set_ylabel('Frequency (pixel count)')
ax.set_title('Before vs After (overlay)')
ax.legend()
ax.ticklabel_format(axis='y', style='plain')
fig.tight_layout()
fig.savefig(root / 'hist_overlay_precise.png', dpi=150)
plt.close(fig)

print('Wrote hist_before_precise.png, hist_after_precise.png, hist_overlay_precise.png')
