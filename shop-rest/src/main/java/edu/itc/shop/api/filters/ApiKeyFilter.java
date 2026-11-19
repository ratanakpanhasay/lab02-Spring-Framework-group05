package edu.itc.shop.api.filters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import edu.itc.shop.api.dto.Problem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private static final Set<String> SAFE_METHODS =
            Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    private final byte[] expectedKey = expectedKey().getBytes(StandardCharsets.UTF_8);

    static String expectedKey() {
        String fromEnv = System.getenv("SHOP_API_KEY");
        return fromEnv != null && !fromEnv.isBlank() ? fromEnv : "dev-secret";
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return;
        }

        String supplied = request.getHeaderString(HEADER);
        if (supplied == null || !MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            Problem problem = new Problem(
                    "about:blank",
                    "Unauthorized",
                    401,
                    "Missing or invalid " + HEADER + " header",
                    request.getUriInfo().getRequestUri().getPath(),
                    null);
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", "ApiKey header=\"" + HEADER + "\"")
                    .type("application/problem+json")
                    .entity(problem)
                    .build());
        }
    }
}
