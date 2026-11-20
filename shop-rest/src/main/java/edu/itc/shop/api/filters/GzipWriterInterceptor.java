package edu.itc.shop.api.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

@Provider
@Compress
@Priority(Priorities.ENTITY_CODER)
public class GzipWriterInterceptor implements WriterInterceptor {

    @Context
    private HttpHeaders requestHeaders;

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        context.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);

        if (!acceptsGzip(requestHeaders.getHeaderString(HttpHeaders.ACCEPT_ENCODING))) {
            context.proceed();
            return;
        }

        context.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, "gzip");
        context.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        OutputStream original = context.getOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(original);
        context.setOutputStream(gzip);
        try {
            context.proceed();
        } finally {
            gzip.finish();
            context.setOutputStream(original);
        }
    }

    static boolean acceptsGzip(String acceptEncoding) {
        if (acceptEncoding == null) {
            return false;
        }
        for (String part : acceptEncoding.split(",")) {
            String[] tokens = part.trim().split(";");
            if (!tokens[0].trim().equalsIgnoreCase("gzip")) {
                continue;
            }
            for (int i = 1; i < tokens.length; i++) {
                String param = tokens[i].trim().replace(" ", "");
                if (param.matches("q=0(\\.0{0,3})?")) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
