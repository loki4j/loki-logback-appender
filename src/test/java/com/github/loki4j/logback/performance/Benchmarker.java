package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class Benchmarker {

    private static long runBenchmark(ILoggingEvent[] events, Consumer<ILoggingEvent> func) {
        var started = System.nanoTime();
        for (int i = 0; i < events.length; i++) {
            func.accept(events[i]);
        }
        return System.nanoTime() - started;
    }


    public static List<RunStat> run(Config config) {
        var events = generateEvents(config.events, 10);

        var warmUpRuns = Math.max((int)(config.runs * 0.25), 1);
        for (int b = 0; b < config.benchmarks.length; b++) {
            for (int run = 0; run < warmUpRuns; run++) {
                runBenchmark(events, config.benchmarks[b].func);
            }
        }

        var runStats = new ArrayList<RunStat>();
        for (int b = 0; b < config.benchmarks.length; b++) {
            var benchmarkName = config.benchmarks[b].name;
            var benchmarkStats = new ArrayList<RunStat>();
            for (int run = 0; run < config.runs; run++) {
                var durationNs = runBenchmark(events, config.benchmarks[b].func);
                var stat = new RunStat(benchmarkName, run, events.length, durationNs);
                benchmarkStats.add(stat);
                //System.out.println(stat);
            }
            runStats.add(benchmarkStats.stream().reduce((a, i) -> {
                a.count += i.count;
                a.durationNs += a.durationNs;
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

    public static class Config {
        public int runs;
        public int events;
        public boolean parallel;
        public Benchmark[] benchmarks;
    }

    public static class Benchmark {
        public String name;
        public Consumer<ILoggingEvent> func;

        public static Benchmark of(String name, Consumer<ILoggingEvent> func) {
            var b = new Benchmark();
            b.name = name;
            b.func = func;
            return b;
        }
    }
    
}
