package com.github.loki4j.client.pipeline;

import java.util.Random;
import java.util.function.Function;

class Jitter {

    private final Function<Integer, Long> randomSupplier;
    private final int maxJitter;

    Jitter(int maxJitter) {
        randomSupplier = maxJitter > 0
                ? new RandomFunction()
                : ignored -> 0L;
        this.maxJitter = maxJitter;
    }

    long generate() {
        return randomSupplier.apply(maxJitter);
    }

    private static class RandomFunction implements Function<Integer, Long> {
        private final ThreadLocal<Random> random = ThreadLocal.withInitial(Random::new);

        @Override
        public Long apply(Integer maxJitter) {
            return Long.valueOf(random.get().nextInt(maxJitter));
        }
    }

}
