package edu.itc.shop.api.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

import edu.itc.shop.Main;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

class ProvidersTest extends JerseyTest {

    private static final String NEW_PRODUCT =
            "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":18.00,\"stock\":5}";

    @Override
    protected Application configure() {
        return Main.config();
    }

    @Test
    void getIsAllowedWithoutApiKey() {
        Response r = target("products").request().get();
        assertEquals(200, r.getStatus());
    }

    @Test
    void writeWithoutApiKeyIsRejected() {
        Response r = target("products").request()
                .post(Entity.json(NEW_PRODUCT));
        assertEquals(401, r.getStatus());
        assertEquals("application/problem+json", r.getMediaType().toString());
        assertNotNull(r.getHeaderString("X-Correlation-Id"));
    }

    @Test
    void writeWithWrongApiKeyIsRejected() {
        Response r = target("products").request()
                .header(ApiKeyFilter.HEADER, "wrong")
                .post(Entity.json(NEW_PRODUCT));
        assertEquals(401, r.getStatus());
    }

    @Test
    void writeWithApiKeySucceeds() {
        Response r = target("products").request()
                .header(ApiKeyFilter.HEADER, ApiKeyFilter.expectedKey())
                .post(Entity.json(NEW_PRODUCT));
        assertEquals(201, r.getStatus());
    }

    @Test
    void correlationIdIsGeneratedWhenMissing() {
        Response r = target("products").request().get();
        assertNotNull(r.getHeaderString(CorrelationFilter.HEADER));
    }

    @Test
    void correlationIdIsEchoed() {
        Response r = target("products").request()
                .header(CorrelationFilter.HEADER, "demo-123").get();
        assertEquals("demo-123", r.getHeaderString(CorrelationFilter.HEADER));
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
    void preflightFromAllowedOriginIsAnswered() {
        Response r = target("products").request()
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type, X-API-Key")
                .options();
        assertEquals(204, r.getStatus());
        assertEquals("http://localhost:5173", r.getHeaderString("Access-Control-Allow-Origin"));
        assertTrue(r.getHeaderString("Access-Control-Allow-Methods").contains("POST"));
        assertTrue(r.getHeaderString("Access-Control-Allow-Headers").contains("X-API-Key"));
    }

    @Test
    void disallowedOriginGetsNoCorsHeaders() {
        Response r = target("products").request()
                .header("Origin", "http://evil.example").get();
        assertEquals(200, r.getStatus());
        assertNull(r.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void gzipWhenRequested() throws IOException {
        Response r = target("products").request()
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip").get();
        assertEquals(200, r.getStatus());
        assertEquals("gzip", r.getHeaderString("Content-Encoding"));
        byte[] body = r.readEntity(byte[].class);
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("Kettle"));
        }
    }

    @Test
    void noGzipWithoutAcceptEncoding() {
        Response r = target("products").request()
                .header("Accept", "application/json").get();
        assertNull(r.getHeaderString("Content-Encoding"));
        assertTrue(r.readEntity(String.class).contains("Kettle"));
    }

    @Test
    void gzipRefusedWithQZero() {
        assertFalse(GzipWriterInterceptor.acceptsGzip("gzip;q=0"));
        assertTrue(GzipWriterInterceptor.acceptsGzip("deflate, gzip;q=0.8"));
        assertFalse(GzipWriterInterceptor.acceptsGzip("br"));
    }
}
