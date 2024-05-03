package com.safa.payment.service.gateway;

import com.safa.payment.common.KafkaEventType;
import com.safa.payment.common.PaymentTransactionPaymentStatus;
import com.safa.payment.dto.NotificationTransactionDto;
import com.safa.payment.dto.common.HostedPaymentOutGoingDto;
import com.safa.payment.entity.Money;
import com.safa.payment.entity.PaymentTransaction;
import com.safa.payment.entity.PromoUsage;
import com.safa.payment.entity.PurchaseOrder;
import com.safa.payment.repository.PaymentTransactionRepository;
import com.safa.payment.service.KafkaProducerService;
import com.safa.payment.service.PromoService;
import com.safa.payment.service.PurchaseOrderService;
import com.safa.payment.service.RestService;
import com.safa.payment.util.PaymentUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This service responsible for creating payment url and handle the communication with payment
 * transaction repository
 *
 * @author Amoon
 */
@Service
@Transactional
@Slf4j
public abstract class CommonPaymentTransactionService implements IPaymentTransactionService {

    private static final String READY_FOR_PAYMENT_DESCRIPTION = "Ready for payment";
    private static final String FULL_DISCOUNT_DESCRIPTION = "Promo code covers the due amount";
    protected final PaymentTransactionRepository paymentTransactionRepository;
    protected final KafkaProducerService kafkaProducerService;
    protected final PaymentUtil paymentUtil;
    protected final PurchaseOrderService purchaseOrderService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final RestService restService;
    protected final PromoService promoService;

    @Autowired
    @Lazy
    public CommonPaymentTransactionService(
            PaymentTransactionRepository paymentTransactionRepository,
            KafkaProducerService kafkaProducerService,
            PaymentUtil paymentUtil,
            PurchaseOrderService purchaseOrderService,
            RestService restService, PromoService promoService,
            ApplicationEventPublisher eventPublisher) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.paymentUtil = paymentUtil;
        this.purchaseOrderService = purchaseOrderService;
        this.eventPublisher = eventPublisher;
        this.restService = restService;
        this.promoService = promoService;
    }


    /**
     * Send request to telr payment gateway to get hosted payment page url;
     *
     * @see <a
     * href="https://telr.com/support/knowledge-base/hosted-payment-page-integration-guide/">...</a>
     */
    @Override
    public PurchaseOrder getPurchaseOrder(
            String referenceId, String referenceType, boolean shouldExist) {
        if (StringUtils.isEmpty(referenceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing order details");
        }
        return purchaseOrderService.findByReference(referenceId, referenceType, shouldExist);
    }


    /**
     * Init payment transaction for a purchase order after the purchase order is saved and consumed
     * from purchase order topic
     */
    @Override
    public void initPaymentTransaction(final PurchaseOrder purchaseOrder, boolean sendAutoNotification) {
        PaymentTransaction paymentTransaction =
                this.paymentUtil.createPaymentTransaction(
                        new Money(BigDecimal.ZERO, purchaseOrder.getAmount().getCurrency()),
                        PaymentTransactionPaymentStatus.READY_FOR_PAYMENT.getShortCode(),
                        READY_FOR_PAYMENT_DESCRIPTION);
        paymentTransaction.setPurchaseOrder(purchaseOrder);
        paymentTransaction = this.paymentTransactionRepository.saveAndFlush(paymentTransaction);
        if (sendAutoNotification) {
            NotificationTransactionDto notificationTransactionDto = paymentUtil.createNotificationTransactionDto(paymentTransaction);
            eventPublisher.publishEvent(notificationTransactionDto);
        }
        kafkaProducerService.sendPaymentTransactionEvent(paymentTransaction, purchaseOrder, null, KafkaEventType.CREATE);
    }

    /**
     * Apply full discount
     */
    @Override
    public void fullDiscountPaymentTransaction(final PurchaseOrder purchaseOrder, PromoUsage promoUsage) {
        PaymentTransaction paymentTransaction =
                this.paymentUtil.createPaymentTransaction(
                        purchaseOrder.getAmountAfterDiscount(),
                        //TODO: look into alternatives
                        PaymentTransactionPaymentStatus.AUTHORIZED.getShortCode(),
                        FULL_DISCOUNT_DESCRIPTION);
        paymentTransaction.setPurchaseOrder(purchaseOrder);
        paymentTransaction = this.paymentTransactionRepository.saveAndFlush(paymentTransaction);
        kafkaProducerService.sendPaymentTransactionEvent(paymentTransaction, purchaseOrder, promoUsage, KafkaEventType.UPDATE);
    }

    public abstract HostedPaymentOutGoingDto getPayNowUrlByReference(String referenceId, String referenceType);

    /**
     * Check if purchase order is already paid and change status for ready for payment
     */
    public PaymentTransaction validateOrderTransactionsStatus(List<PaymentTransaction> paymentTransactions, final PurchaseOrder purchaseOrder) {
        AtomicReference<PaymentTransaction> latestPaymentTransactionRef = new AtomicReference<>();
        paymentTransactions.forEach(
                (transaction) -> {
                    if (PaymentTransactionPaymentStatus.AUTHORIZED.getShortCode().equals(transaction.getTransactionStatus())
                            || PaymentTransactionPaymentStatus.HOLD_AUTHORIZED
                            .getShortCode()
                            .equals(transaction.getTransactionStatus())) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "This order was successfully paid before, you cannot pay again for same order");
                    }
                    if (PaymentTransactionPaymentStatus.READY_FOR_PAYMENT.getShortCode().equals(transaction.getTransactionStatus())) {
                        transaction.setTransactionStatus(PaymentTransactionPaymentStatus.INITIATED.getShortCode());
                        this.paymentTransactionRepository.saveAndFlush(transaction);
                        kafkaProducerService.sendPaymentTransactionEvent(transaction, purchaseOrder, null, KafkaEventType.CREATE);
                    }
                    if (latestPaymentTransactionRef.get() == null || transaction.getUpdatedAt().after(latestPaymentTransactionRef.get().getUpdatedAt())) {
                        latestPaymentTransactionRef.set(transaction);
                    }
                });

        PaymentTransaction latestPaymentTransaction = latestPaymentTransactionRef.get();
        if (latestPaymentTransaction != null && PaymentTransactionPaymentStatus.CANCELLED.getShortCode().equals(latestPaymentTransaction.getTransactionStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "This payment link is cancelled, you need a new payment link");
        } else if (latestPaymentTransaction != null && latestPaymentTransaction.getExpiresAt() != null && !latestPaymentTransaction.getExpiresAt().after(new Date())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "This payment link is expired, you need a new payment link");
        }
        return latestPaymentTransaction;
    }

    public abstract String createHostedPaymentUrl(PurchaseOrder order);

    public abstract void cancelPaymentTransaction(PaymentTransaction paymentTransaction);

    public void updateOrSavePaymentTransaction(PaymentTransaction newPaymentTransaction, PaymentTransaction oldPaymentTransaction, PurchaseOrder purchaseOrder) {
        // First latest transaction, save to current initiated transaction, for other transaction Create new record
        if (purchaseOrder.getPaymentTransactions().size() == 1
                && StringUtils.isNotEmpty(newPaymentTransaction.getCardLast4())) {
            // Override/update the initiated payment transaction with payment transaction details from gateway incoming dto
            if (oldPaymentTransaction.getTransactionStatus().equals(PaymentTransactionPaymentStatus.READY_FOR_PAYMENT.getShortCode())) {
                newPaymentTransaction.setId(oldPaymentTransaction.getId());
            }
        }
        String status = newPaymentTransaction.getTransactionStatus();
        String promoCode = purchaseOrder.getPromoCode();
        // If success payment, update promo usage to success
        PromoUsage promoUsage = null;
        if (StringUtils.isNotEmpty(promoCode) && PaymentTransactionPaymentStatus.isSuccessStatus(status)) {
            promoUsage = this.promoService.setPromoCodeSuccessfullyUsed(purchaseOrder.getId(), promoCode);
        }
        newPaymentTransaction.setPurchaseOrder(purchaseOrder);
        newPaymentTransaction = this.paymentTransactionRepository.saveAndFlush(newPaymentTransaction);
        kafkaProducerService.sendPaymentTransactionEvent(newPaymentTransaction, purchaseOrder, promoUsage, KafkaEventType.UPDATE);
    }

}
