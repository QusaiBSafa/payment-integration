package com.safa.payment.service;

import com.safa.payment.common.PaymentGateway;
import com.safa.payment.common.PaymentTransactionPaymentStatus;
import com.safa.payment.dto.IncomingDto;
import com.safa.payment.dto.MoneyDto;
import com.safa.payment.dto.common.PaymentTransactionDto;
import com.safa.payment.dto.common.PurchaseOrderIncomingDto;
import com.safa.payment.dto.common.PurchaseOrderOutgoingDto;
import com.safa.payment.dto.telr.TransactionDetails;
import com.safa.payment.entity.*;
import com.safa.payment.exception.InternalPaymentException;
import com.safa.payment.repository.PurchaseOrderRepository;
import com.safa.payment.service.gateway.PaymentTransactionServiceFactory;
import com.safa.payment.service.gateway.Telr.TelrRestService;
import com.safa.payment.util.PaymentUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * This service do the CRUD operations on {@link PurchaseOrder} Entity
 * Apply promo code for a specific purchase order
 *
 * @author Qusai Safa
 */
@Service
@Transactional
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentTransactionServiceFactory paymentTransactionServiceFactory;
    private final PromoService promoService;
    private final PaymentUtil paymentUtil;
    private final TelrRestService restService;

    @Value("${default.payment.gateway:TELR}")
    private String defaultPaymentGateway;

    @Autowired
    @Lazy
    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PaymentUtil paymentUtil,
            PromoService promoService, TelrRestService restService,
            PaymentTransactionServiceFactory paymentTransactionServiceFactory) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.paymentUtil = paymentUtil;
        this.promoService = promoService;
        this.restService = restService;
        this.paymentTransactionServiceFactory = paymentTransactionServiceFactory;
    }

    /**
     * Create purchase order (cart order), this record will be used to create the payment url
     */
    public void createPurchaseOrder(final PurchaseOrderIncomingDto purchaseOrderIncomingDto) {
        final PurchaseOrder purchaseOrder =
                this.purchaseOrderRepository.findByReferenceIdAndReferenceTypeIgnoreCase(
                        purchaseOrderIncomingDto.getPaymentTransaction().getReferenceId(),
                        purchaseOrderIncomingDto.getPaymentTransaction().getReferenceType());
        if (purchaseOrder != null) {
            updatePurchaseOrderDueAmount(
                    purchaseOrderIncomingDto.getPaymentTransaction(), purchaseOrder, purchaseOrderIncomingDto.isSendAutoNotification());
        } else {
            final PurchaseOrder newPurchaseOrder = this.paymentUtil.mapToPurchaseOrder(purchaseOrderIncomingDto);
            final PurchaseOrder savedPurchaseOrder = this.purchaseOrderRepository.saveAndFlush(newPurchaseOrder);
            this.paymentTransactionServiceFactory.getInstance(PaymentGateway.valueOf(this.defaultPaymentGateway)).initPaymentTransaction(savedPurchaseOrder, purchaseOrderIncomingDto.isSendAutoNotification());
        }
    }

    /**
     * Validate promo code Create promo usage entry for user and purchase order return discount
     * percentage, due amount, due amount after discount
     */
    public PurchaseOrderOutgoingDto applyPromoForPurchaseOrder(final IncomingDto incomingDto) throws Exception {
        PurchaseOrder purchaseOrder =
                this.purchaseOrderRepository.findByReferenceIdAndReferenceTypeIgnoreCase(
                        incomingDto.getReferenceId(), incomingDto.getReferenceType());
        if (purchaseOrder == null) {
            throw new Exception(
                    "Purchase order not found, reference id: "
                            + incomingDto.getReferenceId()
                            + "referenceType: "
                            + incomingDto.getReferenceType());
        }
        // Validate and get Promo
        Promo promo =
                promoService.validatePromoCode(purchaseOrder.getCustomerId(), incomingDto.getPromoCode());
        // Calculate discount
        try {
            Money dueAmount = purchaseOrder.getAmount();
            Money amountAfterDiscount = new Money();
            BigDecimal dueAmountValue = dueAmount.getValue();
            Double discount = promo.getDiscount();
            BigDecimal discountAmount = BigDecimal.valueOf((discount / 100)).multiply(dueAmountValue);
            BigDecimal dueAmountAfterDiscountValue = dueAmountValue.subtract(discountAmount);
            amountAfterDiscount.setValue(dueAmountAfterDiscountValue);
            amountAfterDiscount.setCurrency(dueAmount.getCurrency());

            purchaseOrder.setAmountAfterDiscount(amountAfterDiscount);
            purchaseOrder.setPromoCode(promo.getCode());
            purchaseOrder.setDiscount(promo.getDiscount());
            this.purchaseOrderRepository.saveAndFlush(purchaseOrder);
            // If full discount, send kafka event to backend service with success payment status
            if (discount == 100) {
                PromoUsage promoUsage = promoService.savePromoUsage(purchaseOrder, promo, true);
                this.paymentTransactionServiceFactory.getInstance(purchaseOrder.getPaymentGateway()).fullDiscountPaymentTransaction(purchaseOrder, promoUsage);
            } else {
                promoService.savePromoUsage(purchaseOrder, promo, false);
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
        return this.paymentUtil.mapToPurchaseOrderOutgoingDto(purchaseOrder);
    }

    /**
     * Update the purchase order
     */
    public void updatePurchaseOrder(PurchaseOrderIncomingDto purchaseOrderIncomingDto) throws Exception {
        PaymentTransactionDto paymentTransactionDto = purchaseOrderIncomingDto.getPaymentTransaction();
        if (paymentTransactionDto == null) {
            throw new Exception("missing payment transaction details");
        }
        PurchaseOrder oldPurchaseOrder =
                purchaseOrderRepository.findByReferenceIdAndReferenceTypeIgnoreCase(
                        paymentTransactionDto.getReferenceId(), paymentTransactionDto.getReferenceType());
        // Update purchase order
        if (oldPurchaseOrder != null) {
            updatePurchaseOrderDueAmount(paymentTransactionDto, oldPurchaseOrder, purchaseOrderIncomingDto.isSendAutoNotification());
        } else {
            createPurchaseOrder(purchaseOrderIncomingDto);
        }
    }

    /**
     * Cancel repeat payment agreement
     * 1- Get transaction details by the saved transaction reference
     * 2- From transaction details get the agreement id
     * 3- Cancel agreement using agreement id
     */
    public void cancelRepeatPaymentAgreement(PurchaseOrderIncomingDto purchaseOrderIncomingDto) {
        PaymentTransactionDto paymentTransactionDto = purchaseOrderIncomingDto.getPaymentTransaction();
        PurchaseOrder purchaseOrder =
                purchaseOrderRepository.findByReferenceIdAndReferenceTypeIgnoreCase(
                        paymentTransactionDto.getReferenceId(), paymentTransactionDto.getReferenceType());

        // if the purchase order is not of repeat payment type, so no agreement is created for this purchase order, and this mean the agreement is not created in telr.
        if (purchaseOrder.getBillingInterval() == null) {
            return;
        }

        // Get paid transaction
        PaymentTransaction paidTransaction = null;
        for (PaymentTransaction paymentTransaction : purchaseOrder.getPaymentTransactions()) {
            if (PaymentTransactionPaymentStatus.AUTHORIZED.getShortCode().equals(paymentTransaction.getTransactionStatus())
                    || PaymentTransactionPaymentStatus.HOLD_AUTHORIZED
                    .getShortCode()
                    .equals(paymentTransaction.getTransactionStatus())) {
                paidTransaction = paymentTransaction;
            }
        }

        if (paidTransaction == null) {
            String message = String.format("Cancel payment agreement Failed, no paid transaction for this purchase order with reference id %s, and reference type %s", paymentTransactionDto.getReferenceId(), paymentTransactionDto.getReferenceType());
            throw new InternalPaymentException(message);
        }

        // Get transaction details by calling telr transaction service, in order to get the payment agreement id.
        String transactionReference = paidTransaction.getTransactionReference();
        //TODO: Move this logic to Gateways services
        TransactionDetails transactionDetails = restService.getPaymentTransactionDetails(transactionReference);

        String agreementId = transactionDetails.getAgreementId();
        if (agreementId == null) {
            // Please
            throw new InternalPaymentException(String.format("Cancel payment agreement Failed, couldn't find the agreement id for transaction id %s,purchase order reference id %s, and reference type %s", transactionReference, paymentTransactionDto.getReferenceId(), paymentTransactionDto.getReferenceType()));
        }

        // Cancel repeat payment agreement
        restService.cancelRepeatPaymentAgreement(agreementId);
    }

    /**
     * Update purchase order fields
     */
    private PurchaseOrder updatePurchaseOrderDueAmount(
            PaymentTransactionDto paymentTransactionDto, PurchaseOrder oldPurchaseOrder, boolean sendAutoNotification) {
        MoneyDto moneyDto = paymentTransactionDto.getAmount();
        oldPurchaseOrder.setAmount(new Money(moneyDto.getValue(), moneyDto.getCurrency()));
        this.paymentTransactionServiceFactory.getInstance(oldPurchaseOrder.getPaymentGateway()).initPaymentTransaction(oldPurchaseOrder, sendAutoNotification);
        return purchaseOrderRepository.saveAndFlush(oldPurchaseOrder);
    }

    public PurchaseOrder findByReference(String referenceId, String referenceType) {
        return findByReference(referenceId, referenceType, true);
    }

    public PurchaseOrder findByReference(String referenceId, String referenceType, boolean shouldExist) {
        PurchaseOrder purchaseOrder =
                purchaseOrderRepository.findByReferenceIdAndReferenceTypeIgnoreCase(referenceId, referenceType.toLowerCase());
        if (purchaseOrder == null) {
            if (shouldExist) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, String.format("Purchase order not found, reference id: %s, referenceType: %s", referenceId, referenceType));
            } else {
                return null;
            }
        }
        return purchaseOrder;
    }

    public PurchaseOrder findPurchaseOrderById(Long id, boolean shouldExist) {
        Optional<PurchaseOrder> purchaseOrder =
                purchaseOrderRepository.findById(id);
        if (!purchaseOrder.isPresent() && shouldExist) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, String.format("Purchase order not found, id: %s", id));
        }
        return purchaseOrder.get();
    }

    public PurchaseOrderOutgoingDto findPurchaseOrder(String referenceId, String referenceType, boolean shouldExist) {
        PurchaseOrder purchaseOrder = findByReference(referenceId, referenceType, shouldExist);
        if (purchaseOrder == null) {
            return null;
        }
        String promoCode = purchaseOrder.getPromoCode();
        Promo promo = null;
        // Return with promo code if exist
        if (StringUtils.isNotEmpty(promoCode)) {
            promo = this.promoService.findPromoByCode(promoCode);
        }
        return paymentUtil.mapToPurchaseOrderOutgoingDto(purchaseOrder, promo);
    }

    public PaymentTransactionDto findPurchaseOrderLatestTransaction(String referenceId, String referenceType, boolean shouldExist) {
        PurchaseOrder purchaseOrder = findByReference(referenceId, referenceType, shouldExist);
        if (purchaseOrder == null) {
            return null;
        }
        // Get the last updated transaction
        PaymentTransaction paymentTransaction = null;
        List<PaymentTransaction> transactions = purchaseOrder.getPaymentTransactions();
        if (!CollectionUtils.isEmpty(transactions)) {
            Comparator<PaymentTransaction> updatedAtComparator = Comparator.comparing(PaymentTransaction::getUpdatedAt);
            paymentTransaction = transactions.stream().max(updatedAtComparator).orElse(null);
        }

        if (paymentTransaction == null) {
            if (shouldExist) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, String.format("No purchase transactions for this purchase order where found, reference id: %s, referenceType: %s", referenceId, referenceType));
            } else {
                return null;
            }
        }

        return paymentUtil.createPaymentTransactionDto(paymentTransaction, purchaseOrder);
    }

    public PurchaseOrder findByUuid(String uuid) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByUuid(uuid);
        if (purchaseOrder == null) {
            log.error("Invalid order uuid({})", uuid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Invalid order uuid(%s)", uuid));
        }
        return purchaseOrder;
    }

    public void cancelLatestPaymentTransactionForReference(String referenceId, String referenceType) {
        PurchaseOrder purchaseOrder = findByReference(referenceId, referenceType);
        PaymentTransaction paymentTransaction = null;
        List<PaymentTransaction> transactions = purchaseOrder.getPaymentTransactions();
        if (!org.springframework.util.CollectionUtils.isEmpty(transactions)) {
            Comparator<PaymentTransaction> updatedAtComparator = Comparator.comparing(PaymentTransaction::getUpdatedAt);
            paymentTransaction = transactions.stream().max(updatedAtComparator).orElse(null);
        }
        if (paymentTransaction == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, String.format("No payment transactions where found for reference id: %s and referenceType: %s", referenceId, referenceType));
        }

        this.paymentTransactionServiceFactory.getInstance(purchaseOrder.getPaymentGateway()).cancelPaymentTransaction(paymentTransaction);

    }
}
