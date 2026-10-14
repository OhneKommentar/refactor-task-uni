package com.instakill.api.rest;

import com.instakill.common.error.BadRequestException;
import com.instakill.common.error.ConflictException;
import com.instakill.common.error.ForbiddenException;
import com.instakill.common.error.InstakillException;
import com.instakill.common.error.NotFoundException;
import com.instakill.common.error.UnauthorizedException;
import com.instakill.common.error.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        problem.setProperty("errors", errors);
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://instakill.dev/errors/validation"));
        return problem;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://instakill.dev/errors/validation"));
        problem.setProperty("errors", ex.errors());
        return problem;
    }

    @ExceptionHandler({BadRequestException.class, ConflictException.class, NotFoundException.class,
            UnauthorizedException.class, ForbiddenException.class})
    public ProblemDetail handleKnown(InstakillException ex) {
        HttpStatus status = switch (ex) {
            case BadRequestException ignored -> HttpStatus.BAD_REQUEST;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case UnauthorizedException ignored -> HttpStatus.UNAUTHORIZED;
            case ForbiddenException ignored -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://instakill.dev/errors/" + status.value()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://instakill.dev/errors/500"));
        return problem;
    }
}
