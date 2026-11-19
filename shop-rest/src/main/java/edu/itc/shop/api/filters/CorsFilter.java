package edu.itc.shop.api.filters;

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
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String ALLOWED_ORIGIN = "http://localhost:5173";
    public static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    public static final String ALLOWED_HEADERS = "Content-Type,X-Api-Key,X-Correlation-Id,If-Match";
    public static final String EXPOSED_HEADERS = "Location,ETag,Link,X-Total-Count,X-Correlation-Id";

    @Override
    public void filter(ContainerRequestContext request) {
        if (HttpMethod.OPTIONS.equals(request.getMethod())
                && request.getHeaderString("Access-Control-Request-Method") != null) {
            request.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MultivaluedMap<String, Object> headers = response.getHeaders();
        headers.putSingle("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
        headers.putSingle("Access-Control-Allow-Methods", ALLOWED_METHODS);
        headers.putSingle("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        headers.putSingle("Access-Control-Expose-Headers", EXPOSED_HEADERS);
    }
}
