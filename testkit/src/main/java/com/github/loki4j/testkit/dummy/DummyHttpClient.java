package com.github.loki4j.testkit.dummy;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.http.LokiResponse;

public class DummyHttpClient implements Loki4jHttpClient {

    private volatile SendInvocation lastSend;
    private volatile CompletableFuture<SendInvocation> nextSendFuture = new CompletableFuture<>();
    private volatile int sendNo = 0;

    @Override
    public void close() throws Exception { }

    @Override
    public HttpConfig getConfig() {
        return HttpConfig.builder().build("test");
    }

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        var data = new byte[batch.remaining()];
        batch.get(data);
        var newSendFuture = new CompletableFuture<SendInvocation>();
        var send = new SendInvocation(++sendNo, data, newSendFuture);
        nextSendFuture.complete(send);
        nextSendFuture = newSendFuture;
        lastSend = send;
        return new LokiResponse(204, "");
    }

    public SendInvocation lastSend() {
        return lastSend;
    }

    public byte[] lastSendData() {
        return lastSend == null ? null : lastSend.data;
    }

    public SendInvocation captureSendInvocation() {
        return new SendInvocation(-1, new byte[0], nextSendFuture);
    }

    static SendInvocation waitForFuture(CompletableFuture<SendInvocation> sendFuture, long timeoutMs) {
        try {
            return sendFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Concurrency exception occurred while waiting for the next send", e);
        }
    } 

    public static class SendInvocation {
        public int sendNo;
        public byte[] data;
        private CompletableFuture<SendInvocation> next;
        public SendInvocation(int sendNo, byte[] batch, CompletableFuture<SendInvocation> next) {
            this.sendNo = sendNo;
            this.data = batch;
            this.next = next;
        }
        public SendInvocation waitForNextSend(long timeoutMs) {
            return waitForFuture(next, timeoutMs);
        }
    }
}
