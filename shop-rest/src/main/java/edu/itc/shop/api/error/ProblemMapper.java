
package edu.itc.shop.api.error;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.itc.shop.api.dto.Problem;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProblemMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(ProblemMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {

        int status;
        String title;
        String detail;

        if (exception instanceof WebApplicationException webException) {

            status = webException.getResponse().getStatus();

            Response.Status statusInfo =
                    Response.Status.fromStatusCode(status);

            title = statusInfo != null
                    ? statusInfo.getReasonPhrase()
                    : "HTTP Error";

            detail = webException.getMessage() != null
                    ? webException.getMessage()
                    : title;

        } else {

            status = 500;
            title = "Internal Server Error";

            String referenceId = UUID.randomUUID().toString();

            LOGGER.log(
                    Level.SEVERE,
                    "Unexpected error. Reference ID: " + referenceId,
                    exception
            );

            detail = "An unexpected error occurred. Reference ID: "
                    + referenceId;
        }

        Problem problem = new Problem(
                "about:blank",
                title,
                status,
                detail,
                uriInfo.getRequestUri().getPath(),
                null
        );

        return Response.status(status)
                .type("application/problem+json")
                .entity(problem)
                .build();
    }
}
