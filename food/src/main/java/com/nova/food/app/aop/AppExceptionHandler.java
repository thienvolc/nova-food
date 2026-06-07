package com.nova.food.app.aop;

import com.nova.food.app.dto.response.ErrorResponse;
import com.nova.food.app.dto.response.ErrorResponse.ValidationError;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

import static com.nova.food.domain.common.constant.ResponseCode.*;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleException(BusinessException ex) {
        ErrorResponse response = ErrorResponse.fromCodeAndMessage(
                ex.getResponseCode().getCode(), ex.getMessage());

        log.info("Business Exception: {}", response);
        log.debug(ex.getMessage(), ex);

        return new ResponseEntity<>(response, ex.getResponseCode().getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleException(
            MethodArgumentNotValidException ex) {

        List<ValidationError> errors = getAllValidationErrors(ex);
        log.info("Validation Exception: {}", errors);
        return ResponseEntity.status(VALIDATION_FAILED.getStatus())
                .body(ErrorResponse.fromValidationErrors(errors));
    }

    private List<ValidationError> getAllValidationErrors(
            MethodArgumentNotValidException ex) {

        return ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ValidationError(e.getField(), e.getCode(), e.getDefaultMessage()))
                .toList();
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleException(BadCredentialsException ex) {
        log.info("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(UNAUTHORIZED)
                .body(ErrorResponse.fromErrorCode(BAD_CREDENTIALS));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleException(AccessDeniedException ex) {
        log.info("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(FORBIDDEN)
                .body(ErrorResponse.fromErrorCode(ACCESS_DENIED));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
        log.info("Invalid request: {}", ex.getMessage());
        return ResponseEntity.status(INVALID_REQUEST.getStatus())
                .body(ErrorResponse.fromErrorCode(INVALID_REQUEST));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(EntityNotFoundException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(INVALID_REQUEST.getStatus())
                .body(ErrorResponse.fromErrorCode(INVALID_REQUEST));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(UsernameNotFoundException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(NOT_FOUND)
                .body(ErrorResponse.fromErrorCode(USERNAME_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.fromErrorCode(INTERNAL_EXCEPTION));
    }
}
