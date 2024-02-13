package com.github.loki4j.client.pipeline;

public class ExponentialBackoff {

    private final long minDelay;
    private final long maxDelay;

    private long currentDelay;

    public ExponentialBackoff(long minDelay, long maxDelay) {
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.currentDelay = minDelay;
    }

    public long nextDelay() {
        if (currentDelay >= maxDelay)
            return maxDelay;
        var delay = currentDelay;
        currentDelay = currentDelay * 2;
        return delay;
    }

    public void reset() {
        currentDelay = minDelay;
    }

    @Override
    public String toString() {
        return "ExponentialBackoff [minDelay=" + minDelay + ", maxDelay=" + maxDelay + ", currentDelay=" + currentDelay + "]";
    }
}
