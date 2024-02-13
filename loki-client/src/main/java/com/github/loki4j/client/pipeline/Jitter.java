package com.github.loki4j.client.pipeline;

import java.util.concurrent.ThreadLocalRandom;

class Jitter {

    private final int maxJitter;

    public Jitter(int maxJitter) {
        this.maxJitter = maxJitter;
    }

    int nextJitter() {
        return maxJitter == 0 ? 0 : ThreadLocalRandom.current().nextInt(maxJitter);
    }
}
