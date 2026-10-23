package com.example.library.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.example.library.availability.CopyNotAvailableException;
import com.example.library.book.CopyNotFoundException;
import com.example.library.book.TitleNotFoundException;
import com.example.library.loan.LoanLimitExceededException;

/**
 * Translates application exceptions into RFC 9457 problem details, served as
 * {@code application/problem+json}.
 *
 * <p>The boundary owns shape, the features own rules, and this layer owns
 * translation: each {@code @ExceptionHandler} picks a status and a title, in
 * the feature's words - a body that does not validate is titled by the
 * request itself ({@link InvalidRequestTitle}), a broken rule is titled by
 * the domain exception's handler. A refusal a client reads as a beige number
 * in a field error is not an answer.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TitleNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTitleNotFound(TitleNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Title not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequest(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle(titleOf(exception));
        problem.setDetail("The request body is not valid.");
        problem.setProperty("errors", fieldErrors(exception));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidParameter(HandlerMethodValidationException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request parameter");
        problem.setDetail("The request parameters are not valid.");
        // Framework 6.1 validated method parameters in the dispatch itself;
        // the exception's own status is the 400 this handler would pick, so
        // the body - the parameter named - is the part a handler adds.
        problem.setProperty("errors", parameterErrors(exception));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(LoanLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleLoanLimitExceeded(LoanLimitExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Loan limit reached");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(CopyNotAvailableException.class)
    public ResponseEntity<ProblemDetail> handleCopyNotAvailable(CopyNotAvailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Copy is not on the shelf");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(CopyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCopyNotFound(CopyNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Copy not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ResponseEntity<ProblemDetail> handleUnknownCopy(HttpClientErrorException.NotFound exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Copy is not known to the availability service");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private String titleOf(MethodArgumentNotValidException exception) {
        // The title is the failing feature's: the validated body names it.
        if (exception.getTarget() instanceof InvalidRequestTitle request) {
            return request.invalidRequestTitle();
        }
        return "Invalid request";
    }

    private List<String> fieldErrors(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();
    }

    private List<String> parameterErrors(HandlerMethodValidationException exception) {
        return exception.getParameterValidationResults().stream()
                .map(result -> result.getMethodParameter().getParameterName() + ": "
                        + result.getResolvableErrors().get(0).getDefaultMessage())
                .toList();
    }
}
