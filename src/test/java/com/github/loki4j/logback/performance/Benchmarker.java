package com.github.loki4j.logback.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Benchmarker {

    private static <T> long runBenchmark(T[] events, Consumer<T> func) {
        var started = System.nanoTime();
        for (int i = 0; i < events.length; i++) {
            func.accept(events[i]);
        }
        return System.nanoTime() - started;
    }


    public static <T> List<RunStat> run(Config<T> config) throws Exception {
        var tp = Executors.newFixedThreadPool(config.parFactor);
        var events = config.generator.get();

        var warmUpRuns = Math.max((int)(config.runs * 0.25), 1);
        for (Benchmark<T> b : config.benchmarks) {
            for (int run = 0; run < warmUpRuns; run++) {
                CompletableFuture
                    .supplyAsync(() -> runBenchmark(events, b.func), tp)
                    .get();
            }
        }

        var runStats = new ArrayList<RunStat>();
        for (Benchmark<T> b : config.benchmarks) {
            var benchmarkStats = new ArrayList<RunStat>();
            var fs = new CompletableFuture[config.runs];
            for (int run = 0; run < config.runs; run++) {
                fs[run] = CompletableFuture
                    .supplyAsync(() -> runBenchmark(events, b.func), tp);
            }
            for (int i = 0; i < fs.length; i++) {
                var durationNs = (Long)fs[i].get();
                var stat = new RunStat(b.name, i, events.length, durationNs);
                benchmarkStats.add(stat);
                System.out.println(stat);
            }
            runStats.add(benchmarkStats.stream().reduce((a, i) -> {
                a.count += i.count;
                a.durationNs += i.durationNs;
                return a;
            }).get());
        }

        return runStats;
    }

    public static class RunStat {
        public String benchmarkName;
        public int runNo;
        public int count;
        public long durationNs;

        public RunStat(String benchmarkName, int runNo, int count, long durationNs) {
            this.benchmarkName = benchmarkName;
            this.runNo = runNo;
            this.count = count;
            this.durationNs = durationNs;
        }

        @Override
        public String toString() {
            return String.format(
                "Run: %s #%s, duration = %.2f ms, throughput = %.3f rps",
                    benchmarkName, runNo, durationNs / 1e+6, 1.0 * count / (durationNs / 1e+9));
        }

    }

    public static class Config<T> {
        public int runs;
        public int parFactor;
        public Supplier<T[]> generator;
        public List<Benchmark<T>> benchmarks;
    }

    public static class Benchmark<T> {
        public String name;
        public Consumer<T> func;

        public static <T> Benchmark<T> of(String name, Consumer<T> func) {
            var b = new Benchmark<T>();
            b.name = name;
            b.func = func;
            return b;
        }
    }
    
}
