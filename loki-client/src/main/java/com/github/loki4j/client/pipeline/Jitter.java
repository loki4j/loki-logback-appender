package com.github.loki4j.client.pipeline;

import java.util.Random;
import java.util.function.Supplier;

class Jitter implements Supplier<Long> {

    private static final int MAX_JITTER = 1000;
    private final ThreadLocal<Random> random = ThreadLocal.withInitial(Random::new);

    @Override
    public Long get() {
        return Long.valueOf(random.get().nextInt(MAX_JITTER));
    }

}
