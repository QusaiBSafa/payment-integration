package com.safa.payment.common.noon;


import com.safa.payment.common.PaymentTransactionPaymentStatus;
import org.apache.commons.lang3.StringUtils;

public enum NoonPaymentStatus {
    DS_RESULT_VERIFIED("3DS_RESULT_VERIFIED", PaymentTransactionPaymentStatus.AUTHORIZED), // User has been successfully authenticated via the 3D Secure protocol.

    AUTHORIZED("AUTHORIZED", PaymentTransactionPaymentStatus.AUTHORIZED), // Order has been authorized (amount on hold on the user card) successfully with the amount provided
    CAPTURED("CAPTURED", PaymentTransactionPaymentStatus.AUTHORIZED), //  Order has been captured successfully and merchant should see the funds
    EXPIRED("EXPIRED", PaymentTransactionPaymentStatus.EXPIRED), // Declined
    FAILED("FAILED", PaymentTransactionPaymentStatus.ERROR), // Error
    CANCELLED("CANCELLED", PaymentTransactionPaymentStatus.CANCELLED), // Cancelled
    INITIATED("INITIATED", PaymentTransactionPaymentStatus.INITIATED),// Order has been initiated with the amount, reference, billing, shipping and other basic details.
    AUTHENTICATED("AUTHENTICATED", PaymentTransactionPaymentStatus.AUTHENTICATED),//Order has been authenticated and is ready for the Authorize/Sale operation
    REJECTED("REJECTED", PaymentTransactionPaymentStatus.DECLINED),// Order has been rejected during the fraud evaluation.
    REFUNDED("REFUNDED", PaymentTransactionPaymentStatus.REFUNDED),//Order has been fully refunded.
    LOCKED("LOCKED", PaymentTransactionPaymentStatus.LOCKED),//Order is marked as LOCKED due to maximum  number of allowed operations limit exceeded on the order
    REVERSED("REVERSED", PaymentTransactionPaymentStatus.REVERSED),//Order has been fully reversed
    PARTIALLY_REVERSED("PARTIALLY_REVERSED", PaymentTransactionPaymentStatus.PARTIALLY_REVERSED);// Order has been partially refunded

    private final PaymentTransactionPaymentStatus paymentTransactionPaymentStatus;

    private final String status;

    NoonPaymentStatus(String status, PaymentTransactionPaymentStatus paymentTransactionPaymentStatus) {
        this.paymentTransactionPaymentStatus = paymentTransactionPaymentStatus;
        this.status = status;
    }

    public static boolean isFailedStatus(String status) {
        return StringUtils.equals(EXPIRED.status, status)
                || StringUtils.equals(FAILED.status, status)
                || StringUtils.equals(REJECTED.status, status)
                || StringUtils.equals(LOCKED.status, status);
    }

    public static boolean isSuccessStatus(String status) {
        return CAPTURED.status.equals(status) || DS_RESULT_VERIFIED.status.equals(status);
    }

    public static boolean isCancelledStatus(String status) {
        return CANCELLED.status.equals(status);
    }

    public static PaymentTransactionPaymentStatus getPaymentTransactionPaymentStatus(String status) {
        // Match Noon's status to PaymentTransactionPaymentStatus
        for (NoonPaymentStatus s : NoonPaymentStatus.values()) {
            if (StringUtils.equals(s.status, status)) {
                return s.paymentTransactionPaymentStatus;
            }

        }
        throw new EnumConstantNotPresentException(NoonPaymentStatus.class, String.format("No enum constant for %s", status));
    }
}
