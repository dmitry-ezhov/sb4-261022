package com.example.library.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.library.book.TitleNotFoundException;

/**
 * Translates application exceptions into RFC 9457 problem details, served as
 * {@code application/problem+json}.
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
}
