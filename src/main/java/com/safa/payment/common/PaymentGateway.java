package com.safa.payment.common;

public enum PaymentGateway {
    TELR("TELR"),
    NOON("NOON");

    private final String method;

    PaymentGateway(String method) {
        this.method = method;
    }
    
}
