package edu.itc.shop.api.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

// Pre-matching so the start time is recorded even when CORS or ApiKeyFilter abort the request.
@Provider
@PreMatching
@Priority(1)
public class RequestTimingFilter implements ContainerRequestFilter {

    static final String START_PROPERTY = "requestStartNanos";

    @Override
    public void filter(ContainerRequestContext request) {
        request.setProperty(START_PROPERTY, System.nanoTime());
    }
}
