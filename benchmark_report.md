# Benchmark report

Generated: 2026-05-06T09:42:20.466356Z

**Hardware:** cores=12, maxMemory=6144.00 MB

| impl | threads | best_wall_s | proc_cpu_s | cpu_util_pct | speedup_vs_seq | efficiency_pct |
|---|---:|---:|---:|---:|---:|---:|
| sequential | 1 | 0.008613 | 0.011531 | 11.16 | 1.000 | 100.00 |
| manual | 12 | 0.014071 | 0.091560 | 54.22 | 0.612 | 5.10 |
| pool | 12 | 0.013099 | 0.112440 | 71.53 | 0.658 | 5.48 |
| fork-join | 12 | 0.009657 | 0.081578 | 70.40 | 0.892 | 7.43 |
| completablefuture | 12 | 0.021568 | 0.201704 | 77.93 | 0.399 | 3.33 |
