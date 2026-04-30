import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class ApplyFilters {

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);

        Scanner input = new Scanner(System.in);
        System.out.println("Insert the name of the file path you would like to use.");
        String filePath = input.nextLine();
        input.close();

        Filters filters = new Filters(filePath);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.printf("Available processors (cores): %d\n", availableProcessors);
        System.out.printf("Max memory (bytes): %d (%.2f MB)\n", maxMemory, maxMemory / 1024.0 / 1024.0);

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        boolean cpuTimeSupported = bean.isCurrentThreadCpuTimeSupported();

        // Warmup
        System.out.println("Warmup run (compute-only)...");
        filters.histogramEqualizedImage(128);

        int trials = 5;
        long bestWall = Long.MAX_VALUE;
        long bestCpu = Long.MAX_VALUE;
        long totalPixels = filters.image.length * filters.image[0].length;

        for (int t = 0; t < trials; t++) {
            long startWall = System.nanoTime();
            long startCpu = cpuTimeSupported ? bean.getCurrentThreadCpuTime() : 0L;

            // compute-only (no IO)
            filters.histogramEqualizedImage(128);

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

        System.out.println();
        System.out.printf("Best (min) wall time: %.4f s\n", bestWall / 1e9);
        if (bestCpu != Long.MAX_VALUE) System.out.printf("Best CPU time:  %.4f s\n", bestCpu / 1e9);

        // End-to-end run (compute + write)
        System.out.println("Running one end-to-end (compute + write) run...");
        long start = System.nanoTime();
        filters.HistogramFilter("output_from_ApplyFilters.jpg", 128);
        long end = System.nanoTime();
        System.out.printf("End-to-end wall time: %.4f s\n", (end - start) / 1e9);

        System.out.println();
        System.out.printf("Theoretical ideal speedup (perfect parallelism) = %d (number of processors)\n", availableProcessors);
        System.out.println("To compute actual speedup later, run a parallel version and compute: speedup = baseline_time / parallel_time");

        System.out.println("Done.");
    }

}
