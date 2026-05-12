# Benchmark report

Generated: 2026-05-12T13:25:04.440782Z

**Hardware:** cores=12, maxMemory=6144.00 MB

| impl | threads | best_wall_ms | proc_cpu_ms | cpu_util_pct | speedup_vs_seq | efficiency_pct |
|---|---:|---:|---:|---:|---:|---:|
| sequential | 1 | 9.407 | 9.654 | 8.55 | 1.000 (baseline) | 100.00 |
| manual | 6 | 5.807 | 12.066 | 17.31 | 1.620× | 27.00 |
| pool | 6 | 4.152 | 10.355 | 20.78 | 2.266× | 37.76 |
| fork-join | 6 | 4.750 | 12.564 | 22.04 | 1.980× | 33.00 |
| completablefuture | 6 | 4.541 | 13.351 | 24.50 | 2.071× | 34.52 |
