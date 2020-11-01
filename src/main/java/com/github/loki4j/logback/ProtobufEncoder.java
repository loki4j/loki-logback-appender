package com.github.loki4j.logback;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.protobuf.Logproto.EntryAdapter;
import com.github.loki4j.protobuf.Logproto.PushRequest;
import com.google.protobuf.Timestamp;

/**
 * Encoder that converts log batches into Protobuf format specified by Loki API
 */
public class ProtobufEncoder extends AbstractLoki4jEncoder {

    public String getContentType() {
        return "application/x-protobuf";
    }

	@Override
	protected byte[] encodeStaticLabels(LogRecord[] batch) {
        var request = PushRequest.newBuilder();
        var stream = request.addStreamsBuilder().setLabels(batch[0].stream);
        for (int i = 1; i < batch.length; i++) {
            stream.addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder()
                    .setSeconds(batch[i].timestampMs / 1000)
                    .setNanos((int)(batch[i].timestampMs % 1000) + batch[i].nanos))
                .setLine(batch[i].message));
        }            
		return request.build().toByteArray();
	}

	@Override
	protected byte[] encodeDynamicLabels(LogRecord[] batch) {
		// TODO Auto-generated method stub
		return null;
	}

    
    
}
