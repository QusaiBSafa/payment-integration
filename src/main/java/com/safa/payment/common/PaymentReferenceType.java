package com.safa.payment.common;

public enum PaymentReferenceType {
    ORDER("order", "o"),
    CONSULTATION("consultation", "c"),
    LAB("lab", "l"),
    SUBSCRIPTION("subscription", "s");

    private String name;
    private String shortCode;

    PaymentReferenceType(String name, String shortCode) {
        this.name = name;
        this.shortCode = shortCode;
    }

    public static PaymentReferenceType getByShortCode(String shortCode) {
        if (ORDER.shortCode.equals(shortCode)) {
            return ORDER;
        } else if (CONSULTATION.shortCode.equals(shortCode)) {
            return CONSULTATION;
        } else if (LAB.shortCode.equals(shortCode)) {
            return LAB;
        } else if (SUBSCRIPTION.shortCode.equals(shortCode)) {
            return SUBSCRIPTION;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
}
