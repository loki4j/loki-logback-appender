package com.github.loki4j.logback.performance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Benchmarker {

    private static <E> Map.Entry<Long, Integer> runBenchmark(Iterator<E> events, Consumer<E> func) {
        var started = System.nanoTime();
        var count = 0;
        while(events.hasNext()) {
            func.accept(events.next());
            count++;
        }
        return Map.entry(System.nanoTime() - started, count);
    }

    @SuppressWarnings("unchecked")
    public static <E> List<RunStat> run(Config<E> config) throws Exception {
        var tp = Executors.newFixedThreadPool(config.parFactor);
        var events = config.generator;

        var warmUpRuns = Math.max((int)(config.runs * 0.25), 1);

        var runStats = new ArrayList<RunStat>();
        for (Benchmark<?, E> b : config.benchmarks) {
            // warm up
            for (int run = 0; run < warmUpRuns; run++) {
                CompletableFuture
                    .supplyAsync(() -> runBenchmark(events.get(), b.func), tp)
                    .get();
            }

            var benchmarkStats = new ArrayList<RunStat>();
            var fs = new CompletableFuture[config.runs];
            var started = System.nanoTime();
            for (int run = 0; run < config.runs; run++) {
                fs[run] = CompletableFuture
                    .supplyAsync(() -> runBenchmark(events.get(), b.func), tp);
            }
            for (int i = 0; i < fs.length; i++) {
                var result =(Map.Entry<Long, Integer>)fs[i].get();
                var durationNs = result.getKey();
                var count = result.getValue();
                var stat = new RunStat(b.name, i, count, durationNs);
                benchmarkStats.add(stat);
                System.out.println(stat);
            }
            var effectiveDuration = System.nanoTime() - started;
            var totalStat = benchmarkStats.stream().reduce((a, i) -> {
                a.count += i.count;
                a.durationNs += i.durationNs;
                return a;
            }).get();
            totalStat.effectiveDurationNs = effectiveDuration;
            runStats.add(totalStat);

            b.finalize();
        }

        return runStats;
    }

    public static class RunStat {
        public String benchmarkName;
        public int runNo;
        public int count;
        public long durationNs;
        public long effectiveDurationNs;

        public RunStat(String benchmarkName, int runNo, int count, long durationNs) {
            this.benchmarkName = benchmarkName;
            this.runNo = runNo;
            this.count = count;
            this.durationNs = durationNs;
        }

        @Override
        public String toString() {
            return String.format("Run: %s #%s, duration = %.2f ms, throughput = %,.3f rps%s",
                benchmarkName, runNo, durationNs / 1e+6, 1.0 * count / (durationNs / 1e+9), effectiveDurationNs == 0L ? "" :
                    String.format(", eff duration = %.2f ms, eff throughput = %,.3f rps",
                        effectiveDurationNs / 1e+6, 1.0 * count / (effectiveDurationNs / 1e+9)));
        }

    }

    public static class Config<E> {
        public int runs;
        public int parFactor;
        public Supplier<Iterator<E>> generator;
        public List<Benchmark<?, E>> benchmarks;
    }

    public static class Benchmark<T, E> {
        public String name;
        public Consumer<E> func;
        public Consumer<T> finalizer;

        private T recipient;

        public void finalize() {
            finalizer.accept(recipient);
        }

        public static <T, E> Benchmark<T, E> of(String name, Supplier<T> initializer, BiConsumer<T, E> func, Consumer<T> finalizer) {
            var b = new Benchmark<T, E>();
            b.name = name;
            b.recipient = initializer.get();
            b.func = e -> func.accept(b.recipient, e);
            b.finalizer = finalizer;
            return b;
        }

        public static <T, E> Benchmark<T, E> of(String name, Supplier<T> initializer, BiConsumer<T, E> func) {
            return of(name, initializer, func, t -> {});
        }
    }

}
