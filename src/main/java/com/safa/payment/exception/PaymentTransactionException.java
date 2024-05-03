package com.safa.payment.exception;

public class PaymentTransactionException extends RuntimeException {

    public PaymentTransactionException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
