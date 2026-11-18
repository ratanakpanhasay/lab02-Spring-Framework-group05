
package edu.itc.shop.api.error;

import java.util.List;
import java.util.Map;

import edu.itc.shop.api.dto.Problem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {

        List<Map<String, String>> errors =
                exception.getConstraintViolations().stream()
                        .map(this::toError)
                        .toList();

        Problem problem = new Problem(
                "about:blank",
                "Validation Failed",
                400,
                "One or more fields are invalid",
                uriInfo.getRequestUri().getPath(),
                errors
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type("application/problem+json")
                .entity(problem)
                .build();
    }

    private Map<String, String> toError(
            ConstraintViolation<?> violation) {

        return Map.of(
                "field", violation.getPropertyPath().toString(),
                "message", violation.getMessage()
        );
    }
}
