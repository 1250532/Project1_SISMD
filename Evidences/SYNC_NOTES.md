## Synchronization and concurrency notes for Project1_SISMD

This document summarizes the synchronization choices made in the `Filters` implementations,
why they are safe, and the trade-offs. It also lists quick validation steps and suggested
alternatives you can try when exploring performance vs. simplicity.

### Small "contract" for the histogram-equalization implementations
- Inputs: `Color[][] image` (shared original image); worker threads only read pixels.
- Outputs: `Color[][] out` (new image) with same dimensions.
- Error modes: `InterruptedException` for thread/queue operations; run-time exceptions are propagated.

All implementations aim to preserve the sequential semantics: for every pixel (x,y),
the output is the histogram-equalized grayscale value computed from the pixel's RGB.

### Key synchronization/design decisions
1. Immutable input view (read-only sharing)
   - Each method starts by calling `Utils.copyImage(image)` and stores it into a `final Color[][] tmp`.
   - `Utils.copyImage` creates a new 2D array but copies references to `java.awt.Color` objects.
   - `Color` instances are immutable; therefore multiple threads may safely read the same `Color` objects
     without additional synchronization.

2. Per-thread local histograms
   - In thread-pool and manual-worker implementations, we use `int[][] localHists = new int[numThreads][256]`.
   - Each worker updates only its own `localHists[tid]` during phase 1. This avoids expensive atomic
     increments on shared counters and eliminates contention.
   - After all workers finish, the main thread aggregates the per-thread histograms into a single global `hist`.

3. Disjoint output partitioning (no locking for writes)
   - In phase 2 each worker is assigned a disjoint range of columns (x ranges) to write into the `out` matrix.
   - Because ranges are disjoint, there are no concurrent writes to the same pixel so no locks are needed.

4. Producer-consumer chunking for dynamic load balance
   - The producer/consumer implementation uses a `BlockingQueue<Range>` and a configurable `chunkSize`.
   - This provides dynamic balancing across workers at low coordination cost; smaller chunks improve balance
     but increase queue overhead.

5. Proper Executor / Pool shutdown
   - ExecutorService and ForkJoinPool are shut down in `finally` blocks and `awaitTermination` is used with
     a short timeout followed by `shutdownNow()` as fallback. This avoids thread leaks on abnormal termination.

### Why these choices are low-overhead and safe
- Avoiding shared atomic increments for every pixel considerably reduces synchronization cost; per-thread
  counters trade a small memory overhead (numThreads * 256 integers) for large lock-free throughput gains.
- Disjoint write ranges are the simplest way to guarantee data-race freedom for output writes.
- Read-only sharing of input avoids copying Color objects; `Utils.copyImage` already allocates a new array
  so workers cannot mutate the original pixel matrix layout.

### Edge cases and caveats
- If `Color` were mutable (it is not), the shared references would require deep-copying pixels or synchronization.
- Extremely small images or too many threads can make parallel versions slower due to task/coordination overhead.
- Chunk sizes influence performance: very small chunks increase queue overhead; very large chunks reduce
  load balancing and may leave some threads idle.

### Simple validation steps
1. Run `ApplyFilters` with sequential and each parallel method; the program already checks equality of outputs.
2. Add assertions (debug-only) verifying that each worker's assigned x-range is disjoint.
3. For correctness stress-testing, run the same image multiple times and validate outputs are identical.

### Suggested alternative approaches (for comparison)
- Atomic counters / LongAdder: replace per-thread arrays with `LongAdder[]` or `AtomicIntegerArray` to avoid
  explicit aggregation. This increases per-pixel overhead and is usually slower than per-thread aggregation.
- Strided updates: let each thread process every N-th column to improve cache locality in some cases.
- Use Java's `IntStream` parallel operations for a quick experiment (may be less controllable for chunking).

### Notes on measurement and tuning
- Single-run measurements can be noisy. Consider warmups and multiple trials when you need accurate numbers.
- Tune `chunkSize` (producer/consumer and CompletableFuture chunking) and Fork/Join threshold for your image sizes.

### Next steps (optional)
- Add a short CSV exporter for automated runs across different thread counts and chunk sizes.
- Implement a `LongAdder`-based version to measure the practical cost of atomics vs. per-thread aggregation.

---
Document created: %s
