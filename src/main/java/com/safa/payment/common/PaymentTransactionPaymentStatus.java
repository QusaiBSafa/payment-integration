package com.safa.payment.common;

public enum PaymentTransactionPaymentStatus {
    HOLD_AUTHORIZED("H"), // Hold authorized
    AUTHORIZED("A"), // Authorized
    DECLINED("D"), // Declined
    ERROR("E"), // Error
    CANCELLED("C"), // Cancelled
    INITIATED("I"), // Initiated (We added this status to define initiate payment transaction record)
    READY_FOR_PAYMENT("R"),
    AUTHENTICATED("T"),
    REFUNDED("F"),
    PARTIALLY_REVERSED("P"),
    REVERSED("S"),
    LOCKED("L"),
    EXPIRED("V");

    private String shortCode;

    PaymentTransactionPaymentStatus(String shortCode) {
        this.shortCode = shortCode;
    }

    public static boolean isFailedStatus(String status) {
        return DECLINED.shortCode.equals(status)
                || ERROR.shortCode.equals(status)
                || CANCELLED.shortCode.equals(status);
    }

    public static boolean isSuccessStatus(String status) {
        return HOLD_AUTHORIZED.shortCode.equals(status) || AUTHORIZED.shortCode.equals(status);
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
}
