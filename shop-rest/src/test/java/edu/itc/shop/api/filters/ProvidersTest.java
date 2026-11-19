package edu.itc.shop.api.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.itc.shop.Main;
import jakarta.annotation.Priority;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

class ProvidersTest extends JerseyTest {

    private static final String NEW_PRODUCT =
            "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":18.00,\"stock\":5}";

    private final List<String> accessLog = new CopyOnWriteArrayList<>();
    private final Logger accessLogger = Logger.getLogger(CorrelationFilter.ACCESS_LOGGER);
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            accessLog.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @Override
    protected Application configure() {
        System.clearProperty(ApiKeyFilter.KEYS_PROPERTY);
        return Main.config();
    }

    @BeforeEach
    void captureAccessLog() {
        accessLogger.addHandler(capture);
    }

    @AfterEach
    void releaseAccessLog() {
        accessLogger.removeHandler(capture);
    }

    private Response createProduct(String apiKey) {
        var request = target("products").request();
        if (apiKey != null) {
            request = request.header(ApiKeyFilter.HEADER, apiKey);
        }
        return request.post(Entity.json(NEW_PRODUCT));
    }

    private static void assertCorsHeaders(Response r) {
        assertEquals(CorsFilter.ALLOWED_ORIGIN, r.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals(CorsFilter.ALLOWED_METHODS, r.getHeaderString("Access-Control-Allow-Methods"));
        assertEquals(CorsFilter.ALLOWED_HEADERS, r.getHeaderString("Access-Control-Allow-Headers"));
        assertEquals(CorsFilter.EXPOSED_HEADERS, r.getHeaderString("Access-Control-Expose-Headers"));
    }

    // --- API key ---

    @Test
    void getAndHeadArePublic() {
        assertEquals(200, target("products").request().get().getStatus());
        assertEquals(200, target("products").request().head().getStatus());
    }

    @Test
    void writeWithoutApiKeyIs401Problem() {
        Response r = createProduct(null);
        assertEquals(401, r.getStatus());
        assertEquals("application/problem+json", r.getMediaType().toString());
        assertNotNull(r.getHeaderString("WWW-Authenticate"));
        assertTrue(r.readEntity(String.class).contains("\"status\":401"));
    }

    @Test
    void writeWithWrongApiKeyIs401() {
        Response r = createProduct("wrong");
        assertEquals(401, r.getStatus());
        assertNotNull(r.getHeaderString("WWW-Authenticate"));
    }

    @Test
    void deleteWithoutApiKeyIs401() {
        assertEquals(401, target("products/1").request().delete().getStatus());
    }

    @Test
    void defaultApiKeyIsDemo123() {
        assertEquals(201, createProduct("demo-123").getStatus());
    }

    @Test
    void parseKeysSplitsAndTrimsCommaSeparatedValues() {
        assertEquals(java.util.Set.of("a", "b", "c"), ApiKeyFilter.parseKeys(" a, b ,,c "));
    }

    // --- CORS ---

    @Test
    void preflightReturns200WithCorsHeaders() {
        Response r = target("products").request()
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type, X-Api-Key")
                .options();
        assertEquals(200, r.getStatus());
        assertCorsHeaders(r);
    }

    @Test
    void corsHeadersOnNormalResponse() {
        Response r = target("products").request()
                .header("Origin", "http://localhost:5173").get();
        assertEquals(200, r.getStatus());
        assertCorsHeaders(r);
    }

    @Test
    void corsHeadersOnUnauthorizedResponse() {
        Response r = target("products").request()
                .header("Origin", "http://localhost:5173")
                .post(Entity.json(NEW_PRODUCT));
        assertEquals(401, r.getStatus());
        assertCorsHeaders(r);
    }

    // --- Correlation ID + access log ---

    @Test
    void correlationIdIsGeneratedWhenMissing() {
        assertNotNull(target("products").request().get().getHeaderString(CorrelationFilter.HEADER));
    }

    @Test
    void correlationIdIsEchoed() {
        Response r = target("products").request()
                .header(CorrelationFilter.HEADER, "trace-42").get();
        assertEquals("trace-42", r.getHeaderString(CorrelationFilter.HEADER));
    }

    @Test
    void unsafeCorrelationIdIsReplaced() {
        Response r = target("products").request()
                .header(CorrelationFilter.HEADER, "bad id<script>").get();
        String id = r.getHeaderString(CorrelationFilter.HEADER);
        assertNotNull(id);
        assertFalse(id.contains("<"));
    }

    @Test
    void correlationIdOnUnauthorizedResponse() {
        Response r = target("products").request()
                .header(CorrelationFilter.HEADER, "trace-401")
                .post(Entity.json(NEW_PRODUCT));
        assertEquals(401, r.getStatus());
        assertEquals("trace-401", r.getHeaderString(CorrelationFilter.HEADER));

        assertNotNull(createProduct(null).getHeaderString(CorrelationFilter.HEADER));
    }

    @Test
    void everyRequestWritesOneAccessLogLine() {
        target("products").request().header(CorrelationFilter.HEADER, "log-1").get();
        createProduct(null);
        target("products").request()
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header(CorrelationFilter.HEADER, "log-3")
                .options();

        assertEquals(3, accessLog.size(), accessLog.toString());
        assertEquals("GET /products -> 200 cid=log-1", accessLog.get(0).replace("/api", ""));
        assertTrue(accessLog.get(1).matches("POST /\\S*products -> 401 cid=\\S+"), accessLog.get(1));
        assertTrue(accessLog.get(2).matches("OPTIONS /\\S*products -> 200 cid=log-3"), accessLog.get(2));
    }

    @Test
    void apiKeyFilterRunsBeforeCorrelationFilter() {
        assertFalse(CorrelationFilter.class.isAnnotationPresent(PreMatching.class),
                "a @PreMatching filter would always run before ApiKeyFilter");
        int apiKey = ApiKeyFilter.class.getAnnotation(Priority.class).value();
        int correlation = CorrelationFilter.class.getAnnotation(Priority.class).value();
        assertTrue(apiKey < correlation);
    }

    // --- Gzip ---

    @Test
    void listIsGzippedWhenRequested() throws IOException {
        Response r = target("products").request()
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip").get();
        assertEquals(200, r.getStatus());
        assertEquals("gzip", r.getHeaderString("Content-Encoding"));
        assertTrue(r.getHeaderString("Vary").contains("Accept-Encoding"));
        byte[] body = r.readEntity(byte[].class);
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
            assertTrue(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("Kettle"));
        }
    }

    @Test
    void listIsPlainWithoutAcceptEncoding() {
        Response r = target("products").request().header("Accept", "application/json").get();
        assertNull(r.getHeaderString("Content-Encoding"));
        assertTrue(r.readEntity(String.class).contains("Kettle"));
    }

    @Test
    void singleProductIsNeverGzipped() {
        Response r = target("products/1").request()
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip").get();
        assertEquals(200, r.getStatus());
        assertNull(r.getHeaderString("Content-Encoding"));
        assertTrue(r.readEntity(String.class).contains("Kettle"));
    }

    @Test
    void acceptEncodingParsing() {
        assertTrue(GzipWriterInterceptor.acceptsGzip("gzip"));
        assertTrue(GzipWriterInterceptor.acceptsGzip("deflate, gzip;q=0.8"));
        assertFalse(GzipWriterInterceptor.acceptsGzip("gzip;q=0"));
        assertFalse(GzipWriterInterceptor.acceptsGzip("br"));
        assertFalse(GzipWriterInterceptor.acceptsGzip(null));
    }
}
