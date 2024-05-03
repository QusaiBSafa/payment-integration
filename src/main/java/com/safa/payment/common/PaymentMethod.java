package com.safa.payment.common;

public enum PaymentMethod {
    CARD("Credit/debit card"),
    APPLE_PAY("Apple Pay");

    private String method;

    PaymentMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
