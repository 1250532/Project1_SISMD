# Benchmark report

Generated: 2026-05-12T21:29:34.151535Z

**Hardware:** cores=12, maxMemory=6144.00 MB

| impl | threads | best_wall_ms | proc_cpu_ms | cpu_util_pct | gc_cycles | gc_total_pause_ms | speedup_vs_seq | efficiency_pct |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| sequential | 1 | 11.968 | 11.978 | 8.34 | 10 | 92 | 1.000 (baseline) | 100.00 |
| manual | 12 | 8.026 | 19.738 | 20.49 | 10 | 82 | 1.491× | 12.43 |
| pool | 12 | 6.299 | 18.518 | 24.50 | 10 | 99 | 1.900× | 15.83 |
| fork-join | 12 | 5.883 | 18.207 | 25.79 | 10 | 119 | 2.035× | 16.95 |
| completablefuture | 12 | 5.334 | 18.368 | 28.70 | 10 | 106 | 2.244× | 18.70 |
