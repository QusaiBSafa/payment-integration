package com.safa.payment.repository;

import com.safa.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    PaymentTransaction findByTransactionReference(String transactionReference);

}
