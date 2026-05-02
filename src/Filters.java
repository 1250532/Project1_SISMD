
import java.awt.Color;
import java.io.IOException;

/**
 * @author Leonardo Costa
 * @contact 1250532@isep.ipp.pt
 * @version 1.1
 */
public class Filters {
    
    String file;
    Color image[][];

    // Constructor with filename for source image
    Filters(String filename) {
        this.file = filename;
        image = Utils.loadImage(filename);
    }

    
    public void HistogramFilter(String outputFile, int value) throws IOException {
        Color[][] tmp = histogramEqualizedImage(value);
        int[] hist = new int[256];
        // Write the computed image to disk
        Utils.writeImage(tmp, outputFile);
    }

    /**
     * Compute histogram-equalized image and return it (no file IO).
     * This allows benchmarking the CPU-bound part separately from image write.
     */
    public Color[][] histogramEqualizedImage(int value) {
        Color[][] tmp = Utils.copyImage(image);
        int[] hist = new int[256];
        int total_pixels = tmp.length * tmp[0].length;
        // Runs through entire matrix and computes luminosity
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                Color pixel = tmp[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = computeLuminosity(r, g, b);
                hist[lum]++;
            }
        }
        // Compute cumulative histogram
        int[] cumulative = new int[256];
        cumulative[0] = hist[0];
        for (int i = 1; i < 256; i++) {
            cumulative[i] = cumulative[i - 1] + hist[i];
        }
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cumulative[i] != 0) {
                cdfMin = cumulative[i];
                break;
            }
        }

        // Change each pixel of the output image
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                Color pixel = tmp[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = computeLuminosity(r, g, b);
                double cdf = (double) cumulative[lum] / (double) (total_pixels - cdfMin);
                int newLum = (int) Math.round(255.0 * cdf);
                tmp[i][j] = new Color(newLum, newLum, newLum);
            }
        }
        return tmp;
    }

    /**
     * Parallel histogram equalization using manually managed threads.
     * Strategy:
     * 1) Split image columns (x) among N worker threads to compute local histograms.
     * 2) Aggregate local histograms to global histogram and compute cumulative + mapping.
     * 3) Split image columns among N worker threads to apply mapping in parallel.
     *
     * This uses explicit Thread objects (no ExecutorService) to illustrate manual thread management.
     */
    public Color[][] histogramEqualizedImageParallel(int value, int numThreads) {
        // Delegate to a single producer-consumer implementation to avoid duplicated logic.
        Color[][] tmp = Utils.copyImage(image);
        int width = tmp.length;
        int chunk = Math.max(1, width / (numThreads * 4));
        return histogramEqualizedImageProducerConsumer(value, numThreads, chunk);
    }

    /**
     * Parallel histogram equalization using a thread pool (ExecutorService).
     * Steps similar to the manual-thread version but reuses threads from a fixed pool.
     */
    public Color[][] histogramEqualizedImageThreadPool(int value, int numThreads) {
        Color[][] tmp = Utils.copyImage(image);
        int width = tmp.length;
        int height = tmp[0].length;

        if (numThreads <= 1) return histogramEqualizedImage(value);

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        final int[][] localHists = new int[numThreads][256];
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

        // Phase 1: compute local histograms
        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            final int startX = (int) ((long) width * tid / numThreads);
            final int endX = (int) ((long) width * (tid + 1) / numThreads);
            futures.add(pool.submit(() -> {
                int[] hist = localHists[tid];
                for (int x = startX; x < endX; x++) {
                    for (int y = 0; y < height; y++) {
                        Color pixel = tmp[x][y];
                        int lum = computeLuminosity(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                        hist[lum]++;
                    }
                }
            }));
        }

        // wait phase 1
        for (java.util.concurrent.Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { throw new RuntimeException(e); }
        }

        // Aggregate local histograms
        int[] hist = new int[256];
        for (int t = 0; t < numThreads; t++) {
            int[] lh = localHists[t];
            for (int i = 0; i < 256; i++) hist[i] += lh[i];
        }

        int total_pixels = width * height;

        // Compute cumulative histogram
        int[] cumulative = new int[256];
        cumulative[0] = hist[0];
        for (int i = 1; i < 256; i++) cumulative[i] = cumulative[i - 1] + hist[i];

        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cumulative[i] != 0) { cdfMin = cumulative[i]; break; }
        }

        // Build mapping table
        int[] map = new int[256];
        for (int i = 0; i < 256; i++) {
            double cdf = (double) cumulative[i] / (double) (total_pixels - cdfMin);
            int newLum = (int) Math.round(255.0 * cdf);
            if (newLum < 0) newLum = 0;
            if (newLum > 255) newLum = 255;
            map[i] = newLum;
        }

        // Phase 2: apply mapping in parallel
        Color[][] out = new Color[width][height];
        futures.clear();
        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            final int startX = (int) ((long) width * tid / numThreads);
            final int endX = (int) ((long) width * (tid + 1) / numThreads);
            futures.add(pool.submit(() -> {
                for (int x = startX; x < endX; x++) {
                    for (int y = 0; y < height; y++) {
                        Color pixel = tmp[x][y];
                        int lum = computeLuminosity(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                        int newLum = map[lum];
                        out[x][y] = new Color(newLum, newLum, newLum);
                    }
                }
            }));
        }

        // wait phase 2
        for (java.util.concurrent.Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { throw new RuntimeException(e); }
        }

        pool.shutdown();
        try { if (!pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) pool.shutdownNow(); } catch (InterruptedException e) { pool.shutdownNow(); Thread.currentThread().interrupt(); }

        return out;
    }

    /**
     * Producer-consumer worker model: producers enqueue column ranges (chunks),
     * fixed worker threads consume ranges and either compute local histograms
     * (phase 1) or apply the mapping (phase 2). This allows dynamic load
     * balancing by tuning chunkSize.
     */
    public Color[][] histogramEqualizedImageProducerConsumer(int value, int numThreads, int chunkSize) {
        Color[][] tmp = Utils.copyImage(image);
        int width = tmp.length;
        int height = tmp[0].length;

        if (numThreads <= 1) return histogramEqualizedImage(value);

        // Helper small Range type
        class Range {
            final int start;
            final int end;
            final boolean poison;
            Range(int s, int e) { this.start = s; this.end = e; this.poison = false; }
            Range(boolean p) { this.start = -1; this.end = -1; this.poison = p; }
        }
        final Range POISON = new Range(true);

        // Phase 1: compute local histograms using worker-consumer pattern
        final int[][] localHists = new int[numThreads][256];
        java.util.concurrent.BlockingQueue<Range> queue = new java.util.concurrent.ArrayBlockingQueue<>(Math.max(16, (width / Math.max(1, chunkSize)) + numThreads));

        // Start consumer workers
        Thread[] workers = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            workers[t] = new Thread(() -> {
                try {
                    while (true) {
                        Range r = queue.take();
                        if (r.poison) break;
                        int[] hist = localHists[tid];
                        for (int x = r.start; x < r.end; x++) {
                            for (int y = 0; y < height; y++) {
                                Color pixel = tmp[x][y];
                                int lum = computeLuminosity(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                                hist[lum]++;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers[t].start();
        }

        // Producer: enqueue ranges
        for (int x = 0; x < width; x += chunkSize) {
            int end = Math.min(width, x + chunkSize);
            try { queue.put(new Range(x, end)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        // send poison pills to stop consumers
        for (int t = 0; t < numThreads; t++) {
            try { queue.put(POISON); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Wait consumers finish phase 1
        for (int t = 0; t < numThreads; t++) {
            try { workers[t].join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Aggregate local histograms
        int[] hist = new int[256];
        for (int t = 0; t < numThreads; t++) {
            int[] lh = localHists[t];
            for (int i = 0; i < 256; i++) hist[i] += lh[i];
        }

        int total_pixels = width * height;

        // Compute cumulative histogram and mapping as before
        int[] cumulative = new int[256];
        cumulative[0] = hist[0];
        for (int i = 1; i < 256; i++) cumulative[i] = cumulative[i - 1] + hist[i];
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) { if (cumulative[i] != 0) { cdfMin = cumulative[i]; break; } }
        int[] map = new int[256];
        for (int i = 0; i < 256; i++) {
            double cdf = (double) cumulative[i] / (double) (total_pixels - cdfMin);
            int newLum = (int) Math.round(255.0 * cdf);
            if (newLum < 0) newLum = 0; if (newLum > 255) newLum = 255;
            map[i] = newLum;
        }

        // Phase 2: apply mapping with producer-consumer pattern (reuse queue/workers)
        Color[][] out = new Color[width][height];
        java.util.concurrent.BlockingQueue<Range> queue2 = new java.util.concurrent.ArrayBlockingQueue<>(Math.max(16, (width / Math.max(1, chunkSize)) + numThreads));

        Thread[] appliers = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            appliers[t] = new Thread(() -> {
                try {
                    while (true) {
                        Range r = queue2.take();
                        if (r.poison) break;
                        for (int x = r.start; x < r.end; x++) {
                            for (int y = 0; y < height; y++) {
                                Color pixel = tmp[x][y];
                                int lum = computeLuminosity(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                                int newLum = map[lum];
                                out[x][y] = new Color(newLum, newLum, newLum);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            appliers[t].start();
        }

        // Producer for phase 2
        for (int x = 0; x < width; x += chunkSize) {
            int end = Math.min(width, x + chunkSize);
            try { queue2.put(new Range(x, end)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        for (int t = 0; t < numThreads; t++) {
            try { queue2.put(POISON); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Wait appliers finish
        for (int t = 0; t < numThreads; t++) {
            try { appliers[t].join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        return out;
    }

    public int computeLuminosity(int r, int g, int b) {
		return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
	}

}
