package com.github.loki4j.logback;

import com.github.loki4j.common.LogRecordBatch;

import static com.github.loki4j.common.ProtobufWriter.*;

import com.grafana.loki.protobuf.Logproto.StreamAdapter;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into Protobuf format specified by Loki API
 */
@NoAutoStart
public class ProtobufEncoder extends AbstractLoki4jEncoder {

    public String getContentType() {
        return "application/x-protobuf";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecordBatch batch) {
        var request = request();
        var streamBuilder = stream(labels(extractStreamKVPairs(batch.get(0).stream)), request);
        for (int i = 0; i < batch.size(); i++) {
            streamBuilder.addEntries(entry(batch.get(i)));
        }
        return compress(request.build().toByteArray());
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecordBatch batch) {
        var request = request();
        String currentStream = null;
        StreamAdapter.Builder streamBuilder = null;
        for (int i = 0; i < batch.size(); i++) {
            if (batch.get(i).stream != currentStream) {
                currentStream = batch.get(i).stream;
                streamBuilder = stream(labels(extractStreamKVPairs(currentStream)), request);
            }
            streamBuilder.addEntries(entry(batch.get(i)));
        }
        return compress(request.build().toByteArray());
    }

    private String labels(String[] labels) {
        var s = new StringBuilder();
        s.append('{');
        if (labels.length > 0) {
            for (int i = 0; i < labels.length; i+=2) {
                s.append(labels[i]);
                s.append('=');
                s.append('"');
                s.append(labels[i + 1].replace("\"", "\\\""));
                s.append('"');
                if (i < labels.length - 2)
                    s.append(',');
            }
        }
        s.append('}');
        return s.toString();
    }
    
}
