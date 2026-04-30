import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Locale;

/**
 * Small benchmark runner for the sequential implementation.
 * Usage: run from project root with the image path as first argument.
 */
public class BenchmarkSequential {

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        if (args.length < 1) {
            System.out.println("Usage: java BenchmarkSequential <input-image-path> [trials]");
            System.exit(1);
        }

        String imagePath = args[0];
        int trials = 5;
        if (args.length >= 2) {
            try { trials = Integer.parseInt(args[1]); } catch (NumberFormatException e) { /* keep default */ }
        }

        System.out.printf("Sequential benchmark for image: %s\n", imagePath);
        // Hardware info
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.printf("Available processors (cores): %d\n", availableProcessors);
        System.out.printf("Max memory (bytes): %d (%.2f MB)\n", maxMemory, maxMemory / 1024.0 / 1024.0);

        // warmup + trials
        Filters f = new Filters(imagePath);
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        boolean cpuTimeSupported = bean.isCurrentThreadCpuTimeSupported();

        // Warmup run (not timed for measurement)
        System.out.println("Warmup run (not counted)...");
        f.histogramEqualizedImage(128);

        long bestWall = Long.MAX_VALUE;
        long bestCpu = Long.MAX_VALUE;
        long totalPixels = f.image.length * f.image[0].length;

        for (int t = 0; t < trials; t++) {
            long startWall = System.nanoTime();
            long startCpu = cpuTimeSupported ? bean.getCurrentThreadCpuTime() : 0L;

            // compute-only (no IO) sequential workload
            f.histogramEqualizedImage(128);

            long endWall = System.nanoTime();
            long endCpu = cpuTimeSupported ? bean.getCurrentThreadCpuTime() : 0L;

            long wallNanos = endWall - startWall;
            long cpuNanos = cpuTimeSupported ? (endCpu - startCpu) : -1L;

            bestWall = Math.min(bestWall, wallNanos);
            if (cpuNanos >= 0) bestCpu = Math.min(bestCpu, cpuNanos);

            double seconds = wallNanos / 1e9;
            double throughput = totalPixels / seconds;

            System.out.printf("Trial %d: wall=%.4fs, throughput=%.0f pixels/s%s\n",
                    t + 1,
                    seconds,
                    throughput,
                    (cpuNanos >= 0 ? String.format(", cpu=%.4fs", cpuNanos / 1e9) : ""));
        }

        System.out.println("\nBest (min) times across trials:");
        System.out.printf("Best wall time: %.4f s\n", bestWall / 1e9);
        if (bestCpu != Long.MAX_VALUE) System.out.printf("Best CPU time:  %.4f s\n", bestCpu / 1e9);

        System.out.println("\nNotes:");
        System.out.println(" - This is the sequential baseline. Use this to compute speedup for parallel runs:");
        System.out.println("   speedup = (baseline_time) / (parallel_time)");
        System.out.println(" - Throughput above is pixels processed per second for the compute-only phase.");
    }
}
