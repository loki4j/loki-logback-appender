package com.github.loki4j.logback;

import java.net.URI;
import java.util.function.Function;

import com.github.loki4j.common.HttpHeaders;
import com.github.loki4j.common.LokiResponse;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.nio.AsyncClientEndpoint;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;

import ch.qos.logback.core.joran.spi.NoAutoStart;

@NoAutoStart
public class ApacheHttp5Sender extends AbstractHttpSender {

    private MinimalHttpAsyncClient client;
    private AsyncClientEndpoint endpoint;
    private Function<byte[], SimpleHttpRequest> requestBuilder;

    private FutureCallback<SimpleHttpResponse> callback = new FutureCallback<SimpleHttpResponse>() {
        @Override
        public void completed(final SimpleHttpResponse response) {
            System.out.println(url + " -> " + response.getCode());
        }
        @Override
        public void failed(final Exception ex) {
            System.out.println(url + " -> " + ex);
        }
        @Override
        public void cancelled() {
            System.out.println(url + " cancelled");
        }
    };

    @Override
    public void start() {
        client = HttpAsyncClients.createMinimal(
            HttpVersionPolicy.FORCE_HTTP_2,
            H2Config.DEFAULT,
            null,
            IOReactorConfig.DEFAULT);
            /*HttpVersionPolicy.FORCE_HTTP_1,
            null,
            Http1Config.DEFAULT,
            IOReactorConfig.DEFAULT);*/

        client.start();

		try {
			var uri = new URI(url);
            var leaseFuture = client.lease(new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort()), null);
            endpoint = leaseFuture.get();

            requestBuilder = (body) -> {
                var req = SimpleHttpRequests.post(uri);
                tenantId.ifPresent(tenant -> req.addHeader(HttpHeaders.X_SCOPE_ORGID, tenant));
                basicAuthToken.ifPresent(token -> req.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + token));
                req.setBody(body, ContentType.create(contentType));
                return req;
            };
		} catch (Exception e) {
			e.printStackTrace();
		}
        

        super.start();
    }

    @Override
    public void stop() {
        endpoint.releaseAndReuse();
        client.close(CloseMode.GRACEFUL);
        super.stop();
    }

    @Override
    public LokiResponse send(byte[] batch) {
        try {
            var rf = endpoint.execute(
                SimpleRequestProducer.create(requestBuilder.apply(batch)),
                SimpleResponseConsumer.create(),
                callback);
            //var r = rf.get();
            //return new LokiResponse(r.getCode(), r.getBodyText());
            return new LokiResponse(204, "");
        } catch (Exception e) {
            throw new RuntimeException("Error while sending batch to Loki", e);
        }
    }
    
}
