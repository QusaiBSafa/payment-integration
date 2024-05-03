package com.safa.payment.exception;

/**
 * This class to handle exception which are not generated from http request, such as kafka event exceptions
 */
public class InternalPaymentException extends RuntimeException {

    public InternalPaymentException(Throwable err) {
        super(err);
    }

    public InternalPaymentException(String errorMessage) {
        super(errorMessage);
    }

    public InternalPaymentException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public InternalPaymentException() {
    }
}
