package edu.itc.shop.web;

import edu.itc.shop.service.UsernameTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Translates exceptions into RFC 9457 {@code application/problem+json} responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 404 for missing domain objects. */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(NoSuchElementException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * 400 with per-field error details for bean-validation failures.
     * <p>
     * Response shape:
     * <pre>{@code
     * {
     *   "type":   "about:blank",
     *   "title":  "Bad Request",
     *   "status": 400,
     *   "detail": "Validation failed",
     *   "instance": "/api/register",
     *   "errors": [
     *     {"field": "password", "message": "size must be between 12 and 72"}
     *   ]
     * }
     * }</pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Validation failed");

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of(
                        "field",   fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"
                ))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    /** 409 when a registration requests an already-taken username. */
    @ExceptionHandler(UsernameTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleUsernameTaken(UsernameTakenException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Conflict");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create("/api/register"));
        return problem;
    }
}
