package com.safa.payment.service.gateway;

import com.safa.payment.common.PaymentGateway;
import com.safa.payment.entity.PurchaseOrder;
import com.safa.payment.service.PurchaseOrderService;
import com.safa.payment.service.gateway.Noon.NoonPaymentTransactionService;
import com.safa.payment.service.gateway.Telr.TelrPaymentTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Amoon
 */
@Slf4j
@Component
public class PaymentTransactionServiceFactory {

    private final PurchaseOrderService purchaseOrderService;
    private final TelrPaymentTransactionService telrPaymentTransactionService;
    private final NoonPaymentTransactionService noonPaymentTransactionService;


    @Autowired
    public PaymentTransactionServiceFactory(PurchaseOrderService purchaseOrderService, TelrPaymentTransactionService telrPaymentTransactionService, NoonPaymentTransactionService noonPaymentTransactionService) {
        this.purchaseOrderService = purchaseOrderService;
        this.telrPaymentTransactionService = telrPaymentTransactionService;
        this.noonPaymentTransactionService = noonPaymentTransactionService;
    }

    public IPaymentTransactionService getInstance(PaymentGateway gateway) {
        switch (gateway) {
            case TELR:
                return this.telrPaymentTransactionService;
            case NOON:
                return this.noonPaymentTransactionService;
            default:
                log.error("Invalid Gateway: " + gateway);
                throw new IllegalArgumentException("Invalid Gateway: " + gateway);
        }
    }

    public IPaymentTransactionService getInstanceByReference(String referenceId, String referenceType) {
        PurchaseOrder order = this.purchaseOrderService.findByReference(referenceId, referenceType);
        return getInstance(order.getPaymentGateway());
    }

    public IPaymentTransactionService getInstanceByUuid(String uuid) {
        PurchaseOrder order = this.purchaseOrderService.findByUuid(uuid);
        return getInstance(order.getPaymentGateway());
    }

}
