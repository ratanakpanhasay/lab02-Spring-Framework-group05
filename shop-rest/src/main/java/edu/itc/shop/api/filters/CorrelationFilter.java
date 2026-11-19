package edu.itc.shop.api.filters;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String ACCESS_LOGGER = "edu.itc.shop.access";

    static final String PROPERTY = "correlationId";

    private static final Logger ACCESS_LOG = System.getLogger(ACCESS_LOGGER);
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    public void filter(ContainerRequestContext request) {
        request.setProperty(PROPERTY, resolveId(request));
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // Absent when an earlier filter (CORS preflight, ApiKeyFilter) aborted the request.
        Object stored = request.getProperty(PROPERTY);
        String id = stored != null ? stored.toString() : resolveId(request);
        response.getHeaders().putSingle(HEADER, id);

        String path = request.getUriInfo().getRequestUri().getRawPath();
        int status = response.getStatus();
        long durationMs = elapsedMillis(request);
        ACCESS_LOG.log(Level.INFO, () -> "%s %s -> %d [%s] %d ms".formatted(
                request.getMethod(), path, status, id, durationMs));
    }

    private static long elapsedMillis(ContainerRequestContext request) {
        Object start = request.getProperty(RequestTimingFilter.START_PROPERTY);
        return start instanceof Long nanos
                ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanos)
                : 0;
    }

    private static String resolveId(ContainerRequestContext request) {
        String incoming = request.getHeaderString(HEADER);
        return incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
    }
}
