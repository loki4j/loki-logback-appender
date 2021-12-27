package com.github.loki4j.client.pipeline;

import java.nio.ByteBuffer;
import java.util.function.Function;

import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.http.LokiResponse;
import com.github.loki4j.client.pipeline.PipelineConfig.WriterFactory;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.writer.Writer;

import static com.github.loki4j.client.pipeline.PipelineConfig.defaultHttpClientFactory;
import static com.github.loki4j.client.pipeline.PipelineConfig.java;
import static com.github.loki4j.client.pipeline.PipelineConfig.json;

public final class SyncPipeline {

    private final Writer writer;

    /**
     * A HTTP client to use for pushing logs to Loki
     */
    private final Loki4jHttpClient httpClient;

    private final ByteBuffer buffer;

    public SyncPipeline(boolean useDirectBuffers, Integer batchMaxBytes, WriterFactory writerFactory,
            Function<HttpConfig, Loki4jHttpClient> httpClientFactory, HttpConfig httpConfig) {
        ByteBufferFactory bufferFactory = new ByteBufferFactory(useDirectBuffers);
        writer = writerFactory.factory.apply(batchMaxBytes, bufferFactory);
        httpClient = httpClientFactory.apply(httpConfig);
        buffer = bufferFactory.allocate(batchMaxBytes);
    }

    public SyncPipeline() {
        this(true, 4 * 1024 * 1024, json, defaultHttpClientFactory, java(5 * 60_000).build(json.contentType));
    }

    public LokiResponse send(LogRecordBatch batch) throws Exception {
        writer.serializeBatch(batch);
        buffer.clear();
        writer.toByteBuffer(buffer);

        return httpClient.send(buffer);
    }

    public void stop() throws Exception {
        httpClient.close();
    }

}
