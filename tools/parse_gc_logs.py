#!/usr/bin/env python3
import re
import sys
from pathlib import Path

def parse_gc_log(path):
    text = Path(path).read_text(errors='ignore')
    # Find lines with "GC(n) ... <description> ... <duration>ms"
    # We'll match patterns like: GC(3) ... 5.752ms or 0.00s, but prefer ms lines
    ms_pattern = re.compile(r'GC\(\d+\).*?\s(\d+\.\d+)ms')
    # Also match lines with seconds: e.g., Real=0.01s (but those are cpu times). We focus on Pause lines
    matches = ms_pattern.findall(text)
    total_ms = sum(float(m) for m in matches)
    count = len(matches)
    return count, total_ms

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: parse_gc_logs.py gcfile1 [gcfile2 ...]')
        sys.exit(2)
    for p in sys.argv[1:]:
        c, s = parse_gc_log(p)
        print(f"{p}\t{c}\t{s:.3f}")
