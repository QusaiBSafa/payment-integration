package com.safa.payment.listener;

import com.safa.payment.entity.PaymentTransaction;
import com.safa.payment.util.CalendarUtil;
import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentTransactionEntityListener {

    @Value("${payment.expiryDays:10}")
    private int expiryDays;

    @PrePersist
    public void prePersist(PaymentTransaction paymentTransaction) {
        if (paymentTransaction.getCreatedAt() != null && paymentTransaction.getExpiresAt() == null) {
            paymentTransaction.setExpiresAt(CalendarUtil.addDaysToDate(paymentTransaction.getCreatedAt(), expiryDays));
        }
    }
}
