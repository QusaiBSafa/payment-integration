package com.safa.payment.common;

public enum PaymentStatus {
    READY_FOR_PAYMENT("Ready for payment"),
    INITIATED("Payment initiated"),
    SUCCESS("Payment success"),
    FAILED("Payment failed"),
    REFUSED("Payment refused"),
    CANCELLED("Payment cancelled"),
    UNKNOWN("Unknown"),
    REFUNDED("Refunded"),
    PARTIALLY_REVERSED("Partially reserved"),
    REVERSED("Reserved"),
    EXPIRED("Expired");

    private String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
