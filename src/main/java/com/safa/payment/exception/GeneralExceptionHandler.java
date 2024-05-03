package com.safa.payment.exception;

import com.safa.payment.service.RestService;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * * Handle all exceptions and java bean validation errors for all endpoints income data that use
 * the @Valid annotation
 *
 * @author Qusai Safa
 * TODO send error message to slack
 */
@ControllerAdvice
@Slf4j
public class GeneralExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String ACCESS_DENIED = "Access denied!";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String ERROR_MESSAGE_TEMPLATE = "message: %s %n requested uri: %s";
    public static final String LIST_JOIN_DELIMITER = ",";
    public static final String FIELD_ERROR_SEPARATOR = ": ";
    private static final String ERRORS_FOR_PATH = "errors {} for path {}";
    private static final String PATH = "path";
    private static final String ERRORS = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String TIMESTAMP = "timestamp";
    private static final String TYPE = "type";


    private final RestService restService;

    @Autowired()
    public GeneralExceptionHandler(RestService restService) {
        this.restService = restService;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + FIELD_ERROR_SEPARATOR + error.getDefaultMessage())
                .collect(Collectors.toList());
        return getExceptionResponseEntity(ex, HttpStatus.BAD_REQUEST, request, validationErrors);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException exception,
                                                            WebRequest request) {
        final List<String> validationErrors = exception.getConstraintViolations().stream().map(
                        violation -> violation.getPropertyPath() + FIELD_ERROR_SEPARATOR + violation.getMessage())
                .collect(Collectors.toList());
        return getExceptionResponseEntity(exception, HttpStatus.BAD_REQUEST, request, validationErrors);
    }

    /**
     * A Payment Transaction request exception handler
     */
    @ExceptionHandler({PaymentTransactionException.class})
    public ResponseEntity<Object> handlePaymentTransactionExceptions(Exception exception,
                                                                     WebRequest request) {
        ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);
        final HttpStatus status =
                responseStatus != null ? responseStatus.value() : HttpStatus.BAD_REQUEST;
        return getObjectResponseEntity(exception, request, status);
    }


    @ExceptionHandler({InternalPaymentException.class})
    public ResponseEntity<Object> handleInternalPaymentExceptions(Exception exception,
                                                                  WebRequest request) {
        String message = exception.getMessage();
        logger.error(String.format(ERROR_MESSAGE_TEMPLATE, message, ""), exception);
        restService.postErrorMessage(exception.getMessage());
        return null;
    }

    @ExceptionHandler({ResponseStatusException.class})
    public ResponseEntity<Object> handleRequestStatusException(ResponseStatusException exception,
                                                               WebRequest request) {
        final HttpStatus status =
                HttpStatus.valueOf(exception.getStatusCode().value());
        return getObjectResponseEntity(exception, request, status);
    }

    /**
     * A general handler for all uncaught exceptions
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAllExceptions(Exception exception, WebRequest request) {
        ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);
        final HttpStatus status =
                responseStatus != null ? responseStatus.value() : HttpStatus.INTERNAL_SERVER_ERROR;
        return getObjectResponseEntity(exception, request, status);
    }

    private ResponseEntity<Object> getObjectResponseEntity(Exception exception, WebRequest request,
                                                           HttpStatus status) {
        final String localizedMessage = exception.getLocalizedMessage();
        final String path = request.getDescription(false);
        final String message =
                StringUtils.isNotEmpty(localizedMessage) ? localizedMessage : status.getReasonPhrase();
        final String errorMessage = String.format(ERROR_MESSAGE_TEMPLATE, message, path);
        logger.error(errorMessage, exception);
        restService.postErrorMessage(errorMessage);
        return getExceptionResponseEntity(exception, status, request,
                Collections.singletonList(message));
    }

    /**
     * Build a detailed information about the exception in the response
     */
    private ResponseEntity<Object> getExceptionResponseEntity(final Exception exception,
                                                              final HttpStatus status, final WebRequest request, final List<String> errors) {
        final Map<String, Object> body = new LinkedHashMap<>();
        String path = "";
        body.put(TIMESTAMP, Instant.now());
        if (status != null) {
            body.put(STATUS, status.value());
        }
        body.put(ERRORS, errors);
        body.put(TYPE, exception.getClass().getSimpleName());
        if (request != null) {
            path = request.getDescription(false);
            body.put(PATH, path);
        }
        body.put(MESSAGE, getMessageForStatus(status));
        final String errorsMessage =
                CollectionUtils.isNotEmpty(errors) ? errors.stream().filter(StringUtils::isNotEmpty)
                        .collect(Collectors.joining(LIST_JOIN_DELIMITER)) : status.getReasonPhrase();
        log.error(ERRORS_FOR_PATH, errorsMessage, path);
        return new ResponseEntity<>(body, status);
    }

    private String getMessageForStatus(HttpStatus status) {
        switch (status) {
            case UNAUTHORIZED:
                return ACCESS_DENIED;
            case BAD_REQUEST:
                return INVALID_REQUEST;
            default:
                return status.getReasonPhrase();
        }
    }
}
