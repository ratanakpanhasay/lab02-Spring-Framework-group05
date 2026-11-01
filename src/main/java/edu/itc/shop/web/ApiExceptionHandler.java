
package edu.itc.shop.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import edu.itc.shop.service.InsufficientStockException;
import edu.itc.shop.service.ProductNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    //not found 404
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage()
                );

        problem.setTitle("Not Found");
        return problem;
    }

    // insufficientStock 409
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        ex.getMessage()
                );

        problem.setTitle("Conflict");
        return problem;
    }

    //Bad Request (Validation Errors) 400
        
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed"
        );

        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);

        return problem;
    }


        // Handle missing resources (404)
    @ExceptionHandler(NoResourceFoundException.class)
        public ProblemDetail handleMissingResource(NoResourceFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "The requested resource was not found"
    );

    problem.setTitle("Not Found");
    return problem;
}
    //Handle 500 Internal Server Error
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred"
        );

        problem.setTitle("Internal Server Error");
        return problem;
    }





}
