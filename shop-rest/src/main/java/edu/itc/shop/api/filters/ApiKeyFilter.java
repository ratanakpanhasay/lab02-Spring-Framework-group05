package edu.itc.shop.api.filters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import edu.itc.shop.api.dto.Problem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    public static final String KEYS_PROPERTY = "shop.api.keys";
    public static final String DEFAULT_KEYS = "demo-123";

    private static final Set<String> PUBLIC_METHODS =
            Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    private final Set<String> validKeys =
            parseKeys(System.getProperty(KEYS_PROPERTY, DEFAULT_KEYS));

    static Set<String> parseKeys(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if (PUBLIC_METHODS.contains(request.getMethod())) {
            return;
        }

        if (!isValid(request.getHeaderString(HEADER))) {
            Problem problem = new Problem(
                    "about:blank",
                    "Unauthorized",
                    401,
                    "Missing or invalid " + HEADER + " header",
                    request.getUriInfo().getRequestUri().getPath(),
                    null);
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", "ApiKey realm=\"shop\"")
                    .type("application/problem+json")
                    .entity(problem)
                    .build());
        }
    }

    private boolean isValid(String supplied) {
        if (supplied == null) {
            return false;
        }
        byte[] candidate = supplied.getBytes(StandardCharsets.UTF_8);
        boolean match = false;
        for (String key : validKeys) {
            match |= MessageDigest.isEqual(candidate, key.getBytes(StandardCharsets.UTF_8));
        }
        return match;
    }
}
