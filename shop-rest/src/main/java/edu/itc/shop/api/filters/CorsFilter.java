package edu.itc.shop.api.filters;

import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
@Priority(200)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:3000",
            "http://localhost:5173");

    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS =
            "Content-Type, Accept, Accept-Encoding, X-API-Key, X-Correlation-Id";
    private static final String EXPOSED_HEADERS = "Location, X-Correlation-Id";

    @Override
    public void filter(ContainerRequestContext request) {
        boolean preflight = HttpMethod.OPTIONS.equals(request.getMethod())
                && request.getHeaderString("Origin") != null
                && request.getHeaderString("Access-Control-Request-Method") != null;
        if (preflight) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MultivaluedMap<String, Object> headers = response.getHeaders();
        headers.add("Vary", "Origin");

        String origin = request.getHeaderString("Origin");
        if (origin == null || !ALLOWED_ORIGINS.contains(origin)) {
            return;
        }

        headers.putSingle("Access-Control-Allow-Origin", origin);
        headers.putSingle("Access-Control-Expose-Headers", EXPOSED_HEADERS);
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            headers.putSingle("Access-Control-Allow-Methods", ALLOWED_METHODS);
            headers.putSingle("Access-Control-Allow-Headers", ALLOWED_HEADERS);
            headers.putSingle("Access-Control-Max-Age", "3600");
        }
    }
}
