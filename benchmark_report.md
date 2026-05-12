# Benchmark report

Generated: 2026-05-12T17:13:26.924123Z

**Hardware:** cores=12, maxMemory=6144.00 MB

| impl | threads | best_wall_s | proc_cpu_s | cpu_util_pct | speedup_vs_seq | efficiency_pct |
|---|---:|---:|---:|---:|---:|---:|
| sequential | 1 | 0.009960 | 0.010231 | 8.56 | 1.000 (baseline) | 100.00 |
| manual | 12 | 0.005562 | 0.015188 | 22.75 | 1.791× | 14.92 |
| pool | 12 | 0.004423 | 0.014318 | 26.98 | 2.252× | 18.77 |
| fork-join | 12 | 0.003757 | 0.012961 | 28.75 | 2.651× | 22.09 |
| completablefuture | 12 | 0.004068 | 0.014220 | 29.13 | 2.448× | 20.40 |
