#!/usr/bin/env python3
from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from pathlib import Path
import textwrap

root = Path(__file__).resolve().parent.parent
out_pdf = root / 'final_report.pdf'
br = root / 'benchmark_report.md'
gt = root / 'GC_TUNING.md'
ih = root / 'image_histograms.md'
before_png = root / 'hist_before.png'
after_png = root / 'hist_after.png'

# simple helper to draw wrapped text
class PDFWriter:
    def __init__(self, c, width, height, margin=40):
        self.c = c
        self.width = width
        self.height = height
        self.margin = margin
        self.cursor = height - margin
        self.c.setFont('Helvetica', 10)

    def newline(self, n=1):
        self.cursor -= 12 * n

    def ensure_space(self, needed):
        if self.cursor < self.margin + needed:
            self.c.showPage(); self.cursor = self.height - self.margin
            self.c.setFont('Helvetica', 10)

    def heading(self, text):
        self.ensure_space(24)
        self.c.setFont('Helvetica-Bold', 14)
        self.c.drawString(self.margin, self.cursor, text)
        self.cursor -= 18
        self.c.setFont('Helvetica', 10)

    def subheading(self, text):
        self.ensure_space(18)
        self.c.setFont('Helvetica-Bold', 11)
        self.c.drawString(self.margin, self.cursor, text)
        self.cursor -= 14
        self.c.setFont('Helvetica', 10)

    def para(self, text):
        self.ensure_space(12)
        wrapped = textwrap.wrap(text, 100)
        for line in wrapped:
            self.c.drawString(self.margin, self.cursor, line)
            self.cursor -= 12

    def code_snippet(self, lines):
        self.ensure_space(12 * (len(lines) + 1))
        self.c.setFillColor(colors.black)
        self.c.setStrokeColor(colors.lightgrey)
        x = self.margin
        w = self.width - 2 * self.margin
        y = self.cursor
        # background box
        self.c.setFillColor(colors.whitesmoke)
        self.c.rect(x - 4, y - 12 * len(lines) - 4, w + 8, 12 * len(lines) + 8, fill=1, stroke=0)
        self.c.setFillColor(colors.black)
        for line in lines:
            self.c.setFont('Courier', 8)
            self.c.drawString(x, self.cursor, line)
            self.cursor -= 12
        self.c.setFont('Helvetica', 10)
        self.cursor -= 6

    def image(self, path, height_px=180):
        if not path.exists(): return
        img = ImageReader(str(path))
        self.ensure_space(height_px + 10)
        self.c.drawImage(img, self.margin, self.cursor - height_px, width=self.width - 2*self.margin, height=height_px, preserveAspectRatio=True)
        self.cursor -= height_px + 10

c = canvas.Canvas(str(out_pdf), pagesize=A4)
width, height = A4
w = PDFWriter(c, width, height)

# Cover
w.heading('Histogram Equalization — Project Report')
w.para('Class: Parallel and Distributed Systems (SISMD)')
w.para('Authors:')
w.code_snippet(['1250532 - Leonardo Costa'])
w.newline(1)

# Introduction
w.heading('Introduction')
w.para('This report documents the implementation, tuning, and evaluation of several solutions for histogram equalization of images. It covers sequential and multiple parallel designs, garbage collector tuning, synchronization considerations, performance analysis and conclusions.')

# Objectives
w.heading('Objectives')
w.para('• Implement a correct histogram-equalization algorithm (sequential).')
w.para('• Implement and compare multiple parallelization strategies: manual threads, thread pools, Fork/Join, and CompletableFuture.')
w.para('• Measure performance and CPU utilization, and provide GC tuning evidence.')

# Implementation Approaches
w.heading('Implementation Approaches')
w.para('The core approach computes per-pixel luminosity (0..255), accumulates a histogram, computes the CDF mapping and applies the mapping to produce the equalized image. The mapping is precomputed into a Color palette to avoid per-pixel Color allocations.')

# Sequential
w.heading('Sequential Solution')
w.para('Description: single-threaded pass that computes histogram, cumulative distribution function (CDF), mapping and applies map to pixels.')
w.subheading('Key snippet')
w.code_snippet([
    'int[] hist = new int[256];',
    'for (int x=0; x<width; x++) for (int y=0; y<height; y++) {',
    '  int lum = computeLuminosity(r,g,b);',
    '  hist[lum]++;',
    '}',
    '/* compute cumulative and map[] */',
    'for (int x=0; x<width; x++) for (int y=0; y<height; y++) out[x][y] = palette[computeLuminosity(...)]'
])

# Multithreaded without thread pools
w.heading('Multithreaded Solution (Without Thread Pools)')
w.para('Description: producer-consumer model where producer enqueues column ranges (chunks) and fixed worker threads consume ranges to compute local histograms and later apply mapping. Provides dynamic load balancing via chunk sizing.')
w.subheading('Key snippet')
w.code_snippet([
    'BlockingQueue<Range> q = new ArrayBlockingQueue<>(...);',
    'for (int t=0; t<numThreads; t++) new Thread(worker).start();',
    '/* producer: for x=0;x<width; x+=chunk q.put(new Range(x,end)) */',
    '/* workers take ranges, compute local histograms into localHists[tid] */'
])

# Multithreaded with thread pools
w.heading('Multithreaded Solution (With Thread Pools)')
w.para('Description: Use ExecutorService with a fixed thread pool; each worker computes its local histogram for a contiguous x-range and futures are used to wait and aggregate. Then pool tasks apply mapping in parallel. This reduces thread lifecycle overhead.')
w.subheading('Key snippet')
w.code_snippet([
    'ExecutorService pool = Executors.newFixedThreadPool(numThreads);',
    'for (t...) futures.add(pool.submit(() -> compute hist for range));',
    '/* wait futures, aggregate */',
    '/* submit apply tasks and wait */'
])

# Fork/Join
w.heading('Fork/Join Framework Solution')
w.para('Description: RecursiveTask/RecursiveAction split columns recursively until a small threshold. Combines local histograms via join and invokes a parallel apply phase.')
w.subheading('Key snippet')
w.code_snippet([
    'class HistTask extends RecursiveTask<int[]> {',
    '  if (range small) compute hist; else fork/join split',
    '}',
    'int[] hist = fjPool.invoke(new HistTask(0,width));'
])

# CompletableFuture-based
w.heading('CompletableFuture-Based Solution')
w.para('Description: supplyAsync tasks compute histogram chunks on an ExecutorService; use CompletableFuture.allOf to wait and aggregate, then runAsync to apply mapping. Provides a composable asynchronous API that uses a thread pool underneath.')
w.subheading('Key snippet')
w.code_snippet([
    'List<CompletableFuture<int[]>> histFutures = new ArrayList<>();',
    'histFutures.add(CompletableFuture.supplyAsync(() -> compute hist chunk, exec));',
    'CompletableFuture.allOf(...).join();'
])

# Garbage Collector Tuning
w.heading('Garbage Collector Tuning')
w.para('Workload characteristics: small live working set (~60MB) with many short-lived temporaries; mapping palette reduces per-pixel allocations.')
w.para('Observed the JVM chose G1; G1 produced short young pauses (~2–4ms). Some Full pauses were observed and attributed to explicit System.gc() calls in tests.')
w.subheading('Suggested flags')
w.code_snippet([
    '-XX:+UseG1GC -Xms512m -Xmx2g -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=30',
    '-XX:+DisableExplicitGC   // avoid System.gc() induced full GCs'
])

# Concurrency and Synchronization
w.heading('Concurrency and Synchronization')
w.para('Read-only local copies of the input image are used; workers write to disjoint output partitions so no fine-grained locking is required. Shared aggregation uses per-thread local hist arrays that are merged after the compute phase.')

# Performance Analysis
w.heading('Performance Analysis')
w.para('Summary of measured medians from the benchmark (seconds):')
# include table from benchmark_report.md
if br.exists():
    lines = br.read_text().splitlines()
    for line in lines:
        w.code_snippet([line])

w.para('The thread-pool and CompletableFuture variants reach nearly 2x speedup vs sequential in our runs; fork-join and manual threads are competitive. CPU util (proc_cpu / wall * cores) shows parallelism but not perfect scaling due to Amdahl-limited sections and overheads.')

# Conclusions
w.heading('Conclusions')
w.para('Parallelization provides substantial speedups for this CPU-bound image processing workload. Use of precomputed palette reduced allocation overhead. Thread pools and the Fork/Join framework are convenient primitives; producer-consumer is useful where dynamic balancing is important. Tuning GC (prefer G1 and disable explicit GC) reduces pause noise during measurements.')

# Visuals
w.heading('Histograms (Before / After)')
w.image(before_png)
w.image(after_png)

c.save()
print('Wrote', out_pdf)
