package com.github.loki4j.common.pipeline;

import com.github.loki4j.common.http.HttpConfig;

/**
 * Configuration properties for Loki4j pipeline.
 */
public class PipelineConfig {

    /**
     * Name of this pipeline
     */
    public final String name;

    /**
     * Max number of events to put into a single batch before sending it to Loki
     */
    public final int batchMaxItems;

    /**
     * Max number of bytes a single batch (as counted by Loki) can contain.
     * This value should not be greater than server.grpc_server_max_recv_msg_size
     * in your Loki config
     */
    public final int batchMaxBytes;

    /**
     * Max time in milliseconds to wait before sending a batch to Loki, even if that
     * batch isn't full
     */
    public final long batchTimeoutMs;

    /**
     * If true, log records in batch are sorted by timestamp.
     * If false, records will be sent to Loki in arrival order.
     * Turn this on if you see 'entry out of order' error from Loki.
     */
    public final boolean sortByTime;

    /**
     * If you use only one label for all log records, you can
     * set this flag to true and save some CPU time on grouping records by label.
     */
    public final boolean staticLabels;

    /**
     * Max number of bytes to keep in the send queue.
     * When the queue is full, incoming log events are dropped
     */
    public final long sendQueueMaxBytes;

    /**
     * Use off-heap memory for storing intermediate data
     */
    public final boolean useDirectBuffers;

    /**
     * Wait util all remaining events are sent before shutdown the appender
     */
    public final boolean drainOnStop;

    /**
     * If true, Protobuf format is used instead of JSON
    */
    public final boolean useProtobuf;

    /**
     * Configuration properties for HTTP senders
     */
    public final HttpConfig httpConfig;

    public PipelineConfig(String name, int batchMaxItems, int batchMaxBytes, long batchTimeoutMs, boolean sortByTime,
            boolean staticLabels, long sendQueueMaxBytes, boolean useDirectBuffers, boolean drainOnStop,
            boolean useProtobuf, HttpConfig httpConfig) {
        this.name = name;
        this.batchMaxItems = batchMaxItems;
        this.batchMaxBytes = batchMaxBytes;
        this.batchTimeoutMs = batchTimeoutMs;
        this.sortByTime = sortByTime;
        this.staticLabels = staticLabels;
        this.sendQueueMaxBytes = sendQueueMaxBytes;
        this.useDirectBuffers = useDirectBuffers;
        this.drainOnStop = drainOnStop;
        this.useProtobuf = useProtobuf;
        this.httpConfig = httpConfig;
    }

}
