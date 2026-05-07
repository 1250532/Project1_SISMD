import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Color;

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
    java.lang.management.OperatingSystemMXBean platformBean = ManagementFactory.getOperatingSystemMXBean();
    OperatingSystemMXBean osBean = null;
    if (platformBean instanceof OperatingSystemMXBean) osBean = (OperatingSystemMXBean) platformBean;

        // Warmup
        System.out.println("Warmup run (compute-only)...");
        filters.histogramEqualizedImage(128);

        int trials = 10;
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
        double baselineWall = bestWall / 1e9;
        System.out.printf("Best (min) wall time (sequential): %.6f s\n", baselineWall);

        // measure a single sequential run with process CPU if available (for resource utilization)
        double seqProcCpu = 0.0;
        double seqWallSample = 0.0;
        if (osBean != null) {
            long pBefore = osBean.getProcessCpuTime();
            long wBefore = System.nanoTime();
            filters.histogramEqualizedImage(128);
            long wAfter = System.nanoTime();
            long pAfter = osBean.getProcessCpuTime();
            seqProcCpu = (pAfter - pBefore) / 1e9;
            seqWallSample = (wAfter - wBefore) / 1e9;
            System.out.printf("Sequential sample: wall=%.6fs, proc_cpu=%.6fs\n", seqWallSample, seqProcCpu);
        } else {
            System.out.println("OS process CPU time not available; skipping proc-CPU sample for sequential.");
        }

        // End-to-end run (compute + write)
        System.out.println("Running one end-to-end (compute + write) run...");
        long start = System.nanoTime();
        filters.HistogramFilter("output_from_ApplyFilters.jpg", 128);
        long end = System.nanoTime();
        System.out.printf("End-to-end wall time: %.4f s\n", (end - start) / 1e9);

        // Run parallel manual-thread implementation and compare
        int numThreads = availableProcessors; // default to number of cores
        System.out.printf("\nRunning parallel manual-thread version with %d threads...\n", numThreads);
    // warmup
    filters.histogramEqualizedImageParallel(128, numThreads);
    long pProcBefore = osBean != null ? osBean.getProcessCpuTime() : -1L;
    long pStart = System.nanoTime();
    Color[][] parallelOut = filters.histogramEqualizedImageParallel(128, numThreads);
    long pEnd = System.nanoTime();
    long pProcAfter = osBean != null ? osBean.getProcessCpuTime() : -1L;
    double parallelWall = (pEnd - pStart) / 1e9;
    double parallelProcCpu = (pProcBefore >= 0 && pProcAfter >= pProcBefore) ? (pProcAfter - pProcBefore) / 1e9 : 0.0;
    double parallelCpuUtil = (parallelProcCpu > 0.0 && parallelWall > 0.0) ? (parallelProcCpu / (parallelWall * availableProcessors) * 100.0) : 0.0;
    System.out.printf("Parallel (manual-threads) wall time: %.6f s, proc_cpu=%.6f s, cpu_util=%.2f%%\n", parallelWall, parallelProcCpu, parallelCpuUtil);

    double speedup = baselineWall / parallelWall;
    double efficiency = speedup / (double) numThreads * 100.0; // percentage
    System.out.printf("Speedup (sequential / parallel manual-threads): %.3f\n", speedup);
    System.out.printf("Efficiency (speedup / threads): %.3f%%\n", efficiency);

        // Run thread-pool implementation
        System.out.printf("\nRunning thread-pool version with %d threads...\n", numThreads);
    filters.histogramEqualizedImageThreadPool(128, numThreads); // warmup
    long tpProcBefore = osBean != null ? osBean.getProcessCpuTime() : -1L;
    long tpStart = System.nanoTime();
    Color[][] poolOut = filters.histogramEqualizedImageThreadPool(128, numThreads);
    long tpEnd = System.nanoTime();
    long tpProcAfter = osBean != null ? osBean.getProcessCpuTime() : -1L;
    double poolWall = (tpEnd - tpStart) / 1e9;
    double poolProcCpu = (tpProcBefore >= 0 && tpProcAfter >= tpProcBefore) ? (tpProcAfter - tpProcBefore) / 1e9 : 0.0;
    double poolCpuUtil = (poolProcCpu > 0.0 && poolWall > 0.0) ? (poolProcCpu / (poolWall * availableProcessors) * 100.0) : 0.0;
    System.out.printf("Parallel (thread-pool) wall time: %.6f s, proc_cpu=%.6f s, cpu_util=%.2f%%\n", poolWall, poolProcCpu, poolCpuUtil);

    double speedupPool = baselineWall / poolWall;
    double efficiencyPool = speedupPool / (double) numThreads * 100.0;
    System.out.printf("Speedup (sequential / thread-pool): %.3f\n", speedupPool);
    System.out.printf("Efficiency (speedup / threads): %.3f%%\n", efficiencyPool);

    // (CompletableFuture measurement moved later, after seqOut is computed)

    // Prepare sequential output for correctness checks
    Color[][] seqOut = filters.histogramEqualizedImage(128);

    // Fork/Join implementation measurement
        System.out.printf("\nRunning Fork/Join version with %d parallelism...\n", numThreads);
        // warmup
        try { filters.histogramEqualizedImageForkJoin(128, numThreads); } catch (Throwable ignore) {}
        long fjBefore = osBean != null ? osBean.getProcessCpuTime() : -1L;
        long fjStart = System.nanoTime();
        Color[][] forkOut = filters.histogramEqualizedImageForkJoin(128, numThreads);
        long fjEnd = System.nanoTime();
        long fjAfter = osBean != null ? osBean.getProcessCpuTime() : -1L;
        double forkWall = (fjEnd - fjStart) / 1e9;
        double forkProcCpu = (fjBefore >= 0 && fjAfter >= fjBefore) ? (fjAfter - fjBefore) / 1e9 : 0.0;
        double forkCpuUtil = (forkProcCpu > 0.0 && forkWall > 0.0) ? (forkProcCpu / (forkWall * availableProcessors) * 100.0) : 0.0;
        double speedupFork = baselineWall / forkWall;
        double efficiencyFork = speedupFork / (double) numThreads * 100.0;
        System.out.printf("Fork/Join wall time: %.6f s, proc_cpu=%.6f s, cpu_util=%.2f%%\n", forkWall, forkProcCpu, forkCpuUtil);
        System.out.printf("Speedup (sequential / fork-join): %.3f\n", speedupFork);
        System.out.printf("Efficiency (speedup / threads): %.3f%%\n", efficiencyFork);

        // correctness check for fork-join
        boolean eqFork = true;
        outer3:
        for (int x = 0; x < seqOut.length; x++) {
            for (int y = 0; y < seqOut[0].length; y++) {
                if (!seqOut[x][y].equals(forkOut[x][y])) { eqFork = false; break outer3; }
            }
        }
        System.out.println("Fork-join output equals sequential output? " + (eqFork ? "YES" : "NO"));

    System.out.println();
        boolean eqManual = true;
        outer:
        for (int x = 0; x < seqOut.length; x++) {
            for (int y = 0; y < seqOut[0].length; y++) {
                if (!seqOut[x][y].equals(parallelOut[x][y])) { eqManual = false; break outer; }
            }
        }
    System.out.println("Non-pool parallel output equals sequential output? " + (eqManual ? "YES" : "NO"));

        boolean eqPool = true;
        outer2:
        for (int x = 0; x < seqOut.length; x++) {
            for (int y = 0; y < seqOut[0].length; y++) {
                if (!seqOut[x][y].equals(poolOut[x][y])) { eqPool = false; break outer2; }
            }
        }
    System.out.println("Thread-pool output equals sequential output? " + (eqPool ? "YES" : "NO"));
        System.out.println("Done.");

    // CompletableFuture-based implementation measurement (after seqOut computed)
    System.out.printf("\nRunning CompletableFuture version with %d threads...\n", numThreads);
    // warmup
    try { filters.histogramEqualizedImageCompletableFutures(128, numThreads); } catch (Throwable ignore) {}
    long cfProcBefore = osBean != null ? osBean.getProcessCpuTime() : -1L;
    long cfStart = System.nanoTime();
    Color[][] cfOut = filters.histogramEqualizedImageCompletableFutures(128, numThreads);
    long cfEnd = System.nanoTime();
    long cfProcAfter = osBean != null ? osBean.getProcessCpuTime() : -1L;
    double cfWall = (cfEnd - cfStart) / 1e9;
    double cfProcCpu = (cfProcBefore >= 0 && cfProcAfter >= cfProcBefore) ? (cfProcAfter - cfProcBefore) / 1e9 : 0.0;
    double cfCpuUtil = (cfProcCpu > 0.0 && cfWall > 0.0) ? (cfProcCpu / (cfWall * availableProcessors) * 100.0) : 0.0;
    double speedupCf = baselineWall / cfWall;
    double efficiencyCf = speedupCf / (double) numThreads * 100.0;
    System.out.printf("CompletableFuture wall time: %.6f s, proc_cpu=%.6f s, cpu_util=%.2f%%\n", cfWall, cfProcCpu, cfCpuUtil);
    System.out.printf("Speedup (sequential / completablefuture): %.3f\n", speedupCf);
    System.out.printf("Efficiency (speedup / threads): %.3f%%\n", efficiencyCf);

    // correctness check for completable future
    boolean eqCf = true;
    outer4:
    for (int x = 0; x < seqOut.length; x++) {
        for (int y = 0; y < seqOut[0].length; y++) {
            if (!seqOut[x][y].equals(cfOut[x][y])) { eqCf = false; break outer4; }
        }
    }
    System.out.println("CompletableFuture output equals sequential output? " + (eqCf ? "YES" : "NO"));

        // Write benchmark_report.md with the results and speedup/efficiency columns
        StringBuilder md = new StringBuilder();
        md.append("# Benchmark report\n\n");
        md.append(String.format(Locale.US, "Generated: %s\n\n", java.time.Instant.now().toString()));
        md.append(String.format(Locale.US, "**Hardware:** cores=%d, maxMemory=%.2f MB\n\n", availableProcessors, maxMemory / 1024.0 / 1024.0));
        md.append("| impl | threads | best_wall_s | proc_cpu_s | cpu_util_pct | speedup_vs_seq | efficiency_pct |\n");
        md.append("|---|---:|---:|---:|---:|---:|---:|\n");

        // sequential row: use baselineWall and seqProcCpu
        double seqCpuUtil = (seqProcCpu > 0.0 && baselineWall > 0.0) ? (seqProcCpu / (baselineWall * availableProcessors) * 100.0) : 0.0;
        md.append(String.format(Locale.US, "| %s | %d | %.6f | %.6f | %.2f | %.3f | %.2f |\n", "sequential", 1, baselineWall, seqProcCpu, seqCpuUtil, 1.0, 100.0));

        // manual
        md.append(String.format(Locale.US, "| %s | %d | %.6f | %.6f | %.2f | %.3f | %.2f |\n", "manual", numThreads, parallelWall, parallelProcCpu, parallelCpuUtil, speedup, efficiency));

        // pool
        md.append(String.format(Locale.US, "| %s | %d | %.6f | %.6f | %.2f | %.3f | %.2f |\n", "pool", numThreads, poolWall, poolProcCpu, poolCpuUtil, speedupPool, efficiencyPool));

    // fork-join
    md.append(String.format(Locale.US, "| %s | %d | %.6f | %.6f | %.2f | %.3f | %.2f |\n", "fork-join", numThreads, forkWall, forkProcCpu, forkCpuUtil, speedupFork, efficiencyFork));

    // completablefuture
    md.append(String.format(Locale.US, "| %s | %d | %.6f | %.6f | %.2f | %.3f | %.2f |\n", "completablefuture", numThreads, cfWall, cfProcCpu, cfCpuUtil, speedupCf, efficiencyCf));

        Path mdPath = Paths.get("benchmark_report.md");
        try {
            Files.write(mdPath, md.toString().getBytes());
            System.out.println("Wrote report to " + mdPath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed writing report: " + e.getMessage());
        }
    }

}
