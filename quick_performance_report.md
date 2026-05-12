# Quick performance report

Input image: src.jpg

Warmup: 3, Trials: 7

| impl | threads | median_wall_ms | median_proc_cpu_ms | mem_used_MB_before | mem_used_MB_after |
|---|---:|---:|---:|---:|---:|
| sequential | 1 | 8.446 | 8.783 | 42.11 | 46.66 |
| manual | 1 | 13.060 | 13.029 | 42.03 | 55.30 |
| pool | 1 | 11.692 | 11.669 | 42.03 | 50.66 |
| fork-join | 1 | 11.934 | 11.908 | 42.03 | 50.66 |
| completablefuture | 1 | 11.844 | 11.833 | 42.03 | 50.67 |

| sequential | 1 | 10.513 | 10.498 | 42.03 | 46.67 |
| manual | 2 | 8.716 | 12.728 | 42.06 | 56.69 |
| pool | 2 | 6.403 | 9.969 | 42.08 | 52.00 |
| fork-join | 2 | 7.542 | 12.463 | 42.10 | 51.37 |
| completablefuture | 2 | 7.459 | 12.034 | 42.10 | 51.75 |

| sequential | 1 | 10.678 | 10.678 | 42.10 | 46.74 |
| manual | 4 | 6.901 | 13.604 | 42.10 | 58.74 |
| pool | 4 | 4.892 | 10.266 | 42.10 | 53.49 |
| fork-join | 4 | 6.756 | 15.909 | 42.11 | 51.63 |
| completablefuture | 4 | 6.060 | 14.367 | 42.10 | 52.95 |

| sequential | 1 | 9.647 | 9.635 | 42.10 | 46.52 |
| manual | 6 | 7.085 | 14.936 | 42.10 | 61.27 |
| pool | 6 | 5.728 | 15.308 | 42.10 | 54.74 |
| fork-join | 6 | 5.809 | 15.561 | 42.11 | 51.86 |
| completablefuture | 6 | 4.221 | 12.085 | 42.11 | 54.48 |

| sequential | 1 | 10.816 | 10.796 | 42.10 | 46.51 |
| manual | 8 | 7.583 | 16.411 | 42.11 | 63.02 |
| pool | 8 | 5.073 | 15.021 | 42.11 | 55.39 |
| fork-join | 8 | 5.450 | 15.908 | 42.12 | 52.13 |
| completablefuture | 8 | 5.475 | 15.126 | 42.11 | 55.10 |

