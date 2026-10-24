package com.example.library.api;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    /**
     * The publisher the state changes ride: the boundary is the one place
     * the process learns its dependency died, so it is the publisher of the
     * refusal's state change as well.
     */
    private final ApplicationEventPublisher publisher;

    ApiExceptionHandler(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

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
        // The title is the failing feature's: the validated body names it.
        String title = exception.getTarget() instanceof InvalidRequestTitle request
                ? request.invalidRequestTitle()
                : "Invalid request";
        problem.setTitle(title);
        problem.setDetail("The request body is not valid.");
        // The body one field at a time, the part a client actually needs.
        problem.setProperty(
                "errors",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + " " + error.getDefaultMessage())
                        .toList());
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
        problem.setProperty(
                "errors",
                exception.getParameterValidationResults().stream()
                        .map(result -> result.getMethodParameter().getParameterName() + ": "
                                + result.getResolvableErrors().get(0).getDefaultMessage())
                        .toList());
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

    /**
     * Answers a spent availability budget - the 502 a service owes its
     * caller when the dependency it fronts failed - and publishes the outage
     * the way the process tells the platform about itself: as state, on the
     * event the whole context reads.
     *
     * <p>What arrives here is the framework's own arithmetic: 7.0.9's retry
     * interceptor runs the {@code @Retryable} checker's every attempt and,
     * when the budget is spent, rethrows the last attempt's failure - a
     * {@link ResourceAccessException}, the wire failure's own type. There
     * is no unspent arrival of this type at the boundary, because the
     * availability read is the one outbound call the borrow makes and it is
     * made only through the checker's proxy; every read that reaches this
     * handler has already been replayed the budget's worth. So the spent
     * budget is exactly the moment the dependency stopped answering - and
     * the moment the process publishes its will on the availability state
     * machinery M09 taught:
     * {@code AvailabilityChangeEvent.publish(publisher, this,
     * ReadinessState.REFUSING_TRAFFIC)}. The readiness probe the platform
     * routes on reads the published state and answers {@code
     * OUT_OF_SERVICE} - the refusal is a routing decision, told to the
     * platform as state and never read back from the dependency itself, so
     * the routing answer and the refusal answer cannot drift apart. The
     * state moves back when a read lands again, published the same way in
     * the checker.
     *
     * @param exception the spent call's last wire failure
     * @return a {@code 502} problem detail titled in the availability
     *         feature's words
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ProblemDetail> handleAvailabilityServiceUnreachable(ResourceAccessException exception) {
        AvailabilityChangeEvent.publish(this.publisher, this, ReadinessState.REFUSING_TRAFFIC);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The availability service did not answer while the borrow asked whether the copy is on the shelf");
        problem.setTitle("Availability service is not answering");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
