package com.safa.payment.service.gateway;

import com.safa.payment.dto.common.HostedPaymentOutGoingDto;
import com.safa.payment.entity.PaymentTransaction;
import com.safa.payment.entity.PromoUsage;
import com.safa.payment.entity.PurchaseOrder;

import java.util.List;

/**
 * @author Amoon
 */
public interface IPaymentTransactionService {

    void initPaymentTransaction(final PurchaseOrder purchaseOrder, boolean sendAutoNotification);

    PurchaseOrder getPurchaseOrder(
            String referenceId, String referenceType, boolean shouldExist);

    void fullDiscountPaymentTransaction(final PurchaseOrder purchaseOrder, PromoUsage promoUsage);

    HostedPaymentOutGoingDto getPayNowUrlByReference(String referenceId, String referenceType);

    PaymentTransaction validateOrderTransactionsStatus(List<PaymentTransaction> paymentTransactions, final PurchaseOrder purchaseOrder);

    String createHostedPaymentUrl(PurchaseOrder order);

    void cancelPaymentTransaction(PaymentTransaction paymentTransaction);
}
