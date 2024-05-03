package com.safa.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safa.payment.common.KafkaEventType;
import com.safa.payment.common.PaymentGateway;
import com.safa.payment.common.PaymentMethod;
import com.safa.payment.common.PaymentStatus;
import com.safa.payment.dto.KafkaEventDto;
import com.safa.payment.dto.MoneyDto;
import com.safa.payment.dto.NotificationTransactionDto;
import com.safa.payment.dto.UserDto;
import com.safa.payment.dto.common.*;
import com.safa.payment.dto.telr.PaymentMethodDetailsDto;
import com.safa.payment.dto.telr.TelrPaymentTransactionIncomingDto;
import com.safa.payment.entity.*;
import com.safa.payment.exception.InternalPaymentException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

/**
 * Helper class to support other services
 */
@Slf4j
@Component
@Getter
public class PaymentUtil {

    public static final String PAYMENT_EVENT_NAME = "payment";

    public static final String REFERRAL_EVENT_NAME = "referral";

    public static final String PAYMENT_EVENT_SOURCE = "payment-service";
    public static final String PAY_NOW_PATH = "api/v1/pay-now/";

    public static final String CREATE_METHOD = "create";
    private static final String PAYMENT_NOTIFICATION_REFERENCE_FORMAT = "PAYMENT_%s_NOTIFICATION";
    private final ObjectMapper objectMapper;

    @Value("${sourceCountryShortCode}")
    public String sourceCountryShortCode;

    //TODO: rename to common
    // Url to return tha patient to our wlp subscription success, fail pages
    @Value("${telr.return.subscription.authorized:}")
    public String subscriptionAuthorizedPageURL;
    @Value("${telr.return.subscription.declined:}")
    public String subscriptionDeclinedPageUrl;
    @Value("${telr.return.subscription.cancelled:}")
    public String subscriptionCancelledPageURL;
    //TODO: rename to common
    // Url to return tha patient to our success, fail pages
    @Value("${telr.authorized}")
    public String authorizedPageURL;
    @Value("${telr.declined}")
    public String declinedPageUrl;
    @Value("${telr.cancelled}")
    public String cancelledPageURL;
    @Value("${serviceBaseUrl}")
    private String serviceBaseUrl;
    @Value("${default.payment.gateway:TELR}")
    private String defaultPaymentGateway;

    @Autowired
    PaymentUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static String getPaymentStatus(String paymentStatus) {
        String status;
        switch (paymentStatus) {
            case "A", "H" -> status = PaymentStatus.SUCCESS.getStatus();
            case "R" -> status = PaymentStatus.READY_FOR_PAYMENT.getStatus();
            case "E", ":" -> status = PaymentStatus.FAILED.getStatus();
            case "D" -> status = PaymentStatus.REFUSED.getStatus();
            case "C" -> status = PaymentStatus.CANCELLED.getStatus();
            //TODO: Confirm T with noon
            case "I", "T" -> status = PaymentStatus.INITIATED.getStatus();
            case "F" -> status = PaymentStatus.REFUNDED.getStatus();
            case "P" -> status = PaymentStatus.PARTIALLY_REVERSED.getStatus();
            case "S" -> status = PaymentStatus.REVERSED.getStatus();
            case "V" -> status = PaymentStatus.EXPIRED.getStatus();


            default -> status = PaymentStatus.UNKNOWN.getStatus();
        }
        return status;
    }

    /**
     * Map to PaymentTransaction
     */
    public PaymentTransaction mapTelrTransactionToPaymentTransaction(TelrPaymentTransactionIncomingDto transactionIncomingDto) {
        PaymentTransaction hostedPaymentTransaction = new PaymentTransaction();
        hostedPaymentTransaction.setCreatedAt(new Date());
        hostedPaymentTransaction.setTransactionCartId(transactionIncomingDto.getTran_cartid());
        hostedPaymentTransaction.setTransactionStore(transactionIncomingDto.getTran_store());
        hostedPaymentTransaction.setTransactionType(transactionIncomingDto.getTran_type());
        hostedPaymentTransaction.setTransactionReference(transactionIncomingDto.getTran_ref());
        hostedPaymentTransaction.setTransactionFirstReference(
                transactionIncomingDto.getTran_firstref());
        hostedPaymentTransaction.setTransactionPreviousReference(
                transactionIncomingDto.getTran_prevref());
        hostedPaymentTransaction.setTransactionDescription(transactionIncomingDto.getTran_desc());
        hostedPaymentTransaction.setTransactionCurrency(transactionIncomingDto.getTran_currency());
        hostedPaymentTransaction.setTransactionAmount(
                new BigDecimal(transactionIncomingDto.getTran_amount()));
        hostedPaymentTransaction.setTransactionStatus(transactionIncomingDto.getTran_status());
        hostedPaymentTransaction.setTransactionAuthCode(transactionIncomingDto.getCard_code());
        hostedPaymentTransaction.setTransactionAuthMessage(
                transactionIncomingDto.getTran_authmessage());
        hostedPaymentTransaction.setCardCode(transactionIncomingDto.getCard_code());
        hostedPaymentTransaction.setCardLast4(transactionIncomingDto.getCard_last4());
        hostedPaymentTransaction.setCardCheck(transactionIncomingDto.getCard_check());
        hostedPaymentTransaction.setCartLanguage(transactionIncomingDto.getCart_lang());
        hostedPaymentTransaction.setIntegrationId(
                Integer.parseInt(transactionIncomingDto.getIntegration_id()));
        hostedPaymentTransaction.setBillFirstname(transactionIncomingDto.getBill_fname());
        hostedPaymentTransaction.setBillSurename(transactionIncomingDto.getBill_sname());
        hostedPaymentTransaction.setBillAddress1(transactionIncomingDto.getBill_addr1());
        hostedPaymentTransaction.setBillCity(transactionIncomingDto.getBill_city());
        hostedPaymentTransaction.setBillCountry(transactionIncomingDto.getBill_country());
        hostedPaymentTransaction.setBillEmail(transactionIncomingDto.getBill_email());
        hostedPaymentTransaction.setBillPhone(transactionIncomingDto.getBill_phone1());
        hostedPaymentTransaction.setBillCheck(transactionIncomingDto.getBill_check());
        return hostedPaymentTransaction;
    }

    /**
     * Map to kafka payment transaction event
     */
    public KafkaEventDto<PurchaseOrderIncomingDto> mapToPurchaseOrderIncomingDto(String event)
            throws JsonProcessingException {
        KafkaEventDto<PurchaseOrderIncomingDto> kafkaEventDto =
                this.objectMapper.readValue(
                        event, new TypeReference<KafkaEventDto<PurchaseOrderIncomingDto>>() {
                        });
        return kafkaEventDto;
    }

    public KafkaEventDto<ConsultationIncomingDto> mapToConsultationDto(String event)
            throws JsonProcessingException {
        KafkaEventDto<ConsultationIncomingDto> kafkaEventDto =
                this.objectMapper.readValue(
                        event, new TypeReference<KafkaEventDto<ConsultationIncomingDto>>() {
                        });
        return kafkaEventDto;
    }

    public void validateAmount(Money amount) {
        if (amount == null || amount.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Order price is not set");
        }
    }

    public void validateAmount(MoneyDto amount) {
        if (amount == null || amount.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Order price is not set");
        }
    }

    /**
     * Map to PurchaseOrder
     */
    public PurchaseOrder mapToPurchaseOrder(PurchaseOrderIncomingDto purchaseOrderIncomingDto) {
        PaymentTransactionDto paymentTransaction = purchaseOrderIncomingDto.getPaymentTransaction();
        if (paymentTransaction == null) {
            throw new InternalPaymentException("missing payment transaction:" + purchaseOrderIncomingDto);
        }
        UserDto user = purchaseOrderIncomingDto.getUser();

        if (user == null) {
            throw new InternalPaymentException("missing user details:" + purchaseOrderIncomingDto);
        }
        PurchaseOrder newPurchaseOrder = new PurchaseOrder();
        newPurchaseOrder.setFullName(user.getFullName());
        newPurchaseOrder.setCustomerId(user.getId());
        newPurchaseOrder.setEmail(user.getEmail());
        newPurchaseOrder.setReferenceType(paymentTransaction.getReferenceType());
        newPurchaseOrder.setReferenceId(paymentTransaction.getReferenceId());
        newPurchaseOrder.setPaymentGateway(PaymentGateway.valueOf(this.defaultPaymentGateway));

        UUID uuid = UUID.randomUUID();
        newPurchaseOrder.setUuid(uuid.toString());
        MoneyDto moneyDto = paymentTransaction.getAmount();
        if (moneyDto != null && moneyDto.getValue() != null && moneyDto.getCurrency() != null) {
            newPurchaseOrder.setAmount(new Money(moneyDto.getValue(), moneyDto.getCurrency()));
        }
        newPurchaseOrder.setCountry(user.getCountry());
        newPurchaseOrder.setCity(user.getCity());
        newPurchaseOrder.setFormattedAddress(user.getFormattedAddress());
        newPurchaseOrder.setPhoneNumber(user.getPhoneNumber());
        // If billing interval is not zero and not null then set interval and term in order to create payment agreement
        if (paymentTransaction.getBillingInterval() != null && paymentTransaction.getBillingInterval() > 0) {
            newPurchaseOrder.setBillingTerm(paymentTransaction.getBillingTerm());
            newPurchaseOrder.setBillingInterval(paymentTransaction.getBillingInterval());
        }
        return newPurchaseOrder;
    }

    public PurchaseOrderOutgoingDto mapToPurchaseOrderOutgoingDto(PurchaseOrder purchaseOrder) {
        return mapToPurchaseOrderOutgoingDto(purchaseOrder, null);
    }

    public PurchaseOrderOutgoingDto mapToPurchaseOrderOutgoingDto(PurchaseOrder purchaseOrder, Promo withPromo) {
        PurchaseOrderOutgoingDto purchaseOrderOutgoingDto = new PurchaseOrderOutgoingDto();
        purchaseOrderOutgoingDto.setUserId(purchaseOrder.getCustomerId());
        purchaseOrderOutgoingDto.setId(purchaseOrder.getId());
        purchaseOrderOutgoingDto.setReferenceId(purchaseOrder.getReferenceId());
        purchaseOrderOutgoingDto.setReferenceType(purchaseOrder.getReferenceType());
        purchaseOrderOutgoingDto.setAmount(new MoneyDto(purchaseOrder.getAmount().getValue(), purchaseOrder.getAmount().getCurrency()));
        if (withPromo != null) {
            purchaseOrderOutgoingDto.setDiscount(withPromo.getDiscount());
            Money amountAfterDiscount = purchaseOrder.getAmountAfterDiscount();
            purchaseOrderOutgoingDto.setAmountAfterDiscount(new MoneyDto(amountAfterDiscount.getValue(), amountAfterDiscount.getCurrency()));
            purchaseOrderOutgoingDto.setPromoCode(withPromo.getCode());
        } else if (StringUtils.isNotEmpty(purchaseOrder.getPromoCode()) && purchaseOrder.getAmountAfterDiscount() != null) {
            purchaseOrderOutgoingDto.setDiscount(purchaseOrder.getDiscount());
            purchaseOrderOutgoingDto.setPromoCode(purchaseOrder.getPromoCode());
            purchaseOrderOutgoingDto.setAmountAfterDiscount(new MoneyDto(purchaseOrder.getAmountAfterDiscount().getValue(), purchaseOrder.getAmountAfterDiscount().getCurrency()));
        }

        return purchaseOrderOutgoingDto;
    }

    public PaymentTransaction createPaymentTransaction(Money paidAmount, String status, String description) {
        final PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTransactionStatus(status);
        paymentTransaction.setTransactionAmount(paidAmount.getValue());
        paymentTransaction.setTransactionCurrency(paidAmount.getCurrency());
        paymentTransaction.setTransactionDescription(description);
        paymentTransaction.setIntegrationId(0);
        return paymentTransaction;
    }

    public PaymentTransactionDto createPaymentTransactionDto(PaymentTransaction paymentTransaction, final PurchaseOrder purchaseOrder) {
        PaymentTransactionDto paymentTransactionDto = new PaymentTransactionDto();
        paymentTransactionDto.setId(paymentTransaction.getId());
        paymentTransactionDto.setGatewayReference(purchaseOrder.getPaymentGateway().name());
        paymentTransactionDto.setUserId(purchaseOrder.getCustomerId());
        paymentTransactionDto.setReferenceId(purchaseOrder.getReferenceId());
        paymentTransactionDto.setReferenceType(purchaseOrder.getReferenceType());
        paymentTransactionDto.setAmount(new MoneyDto(purchaseOrder.getAmount().getValue(), purchaseOrder.getAmount().getCurrency()));
        String cardLast4 = paymentTransaction.getCardLast4();
        String cardCode = paymentTransaction.getCardCode();
        if (cardLast4 != null && cardCode != null) {
            // If telr card code is A1 this means apple pay, otherwise credit card payment
            paymentTransactionDto.setPaymentMethodDetails(new PaymentMethodDetailsDto(cardLast4, cardCode, cardCode.equals("A1") ? PaymentMethod.APPLE_PAY.getMethod() : PaymentMethod.CARD.getMethod()));
        }
        paymentTransactionDto.setPaidAmount(new MoneyDto(new BigDecimal("0.0"), "AED"));
        String status = getPaymentStatus(paymentTransaction.getTransactionStatus());
        if (Objects.equals(status, PaymentStatus.SUCCESS.getStatus())) {
            paymentTransactionDto.setPaidAmount(new MoneyDto(paymentTransaction.getTransactionAmount(), paymentTransaction.getTransactionCurrency()));
        }
        paymentTransactionDto.setLink(getPayNowUrl(purchaseOrder.getUuid()));
        paymentTransactionDto.setTransactionReference(paymentTransaction.getTransactionReference());
        String promoCode = purchaseOrder.getPromoCode();
        if (StringUtils.isNotEmpty(promoCode)) {
            paymentTransactionDto.setPromoCode(promoCode);
            Money amountAfterDiscount = purchaseOrder.getAmountAfterDiscount();
            paymentTransactionDto.setAmountAfterDiscount(new MoneyDto(amountAfterDiscount.getValue(), amountAfterDiscount.getCurrency()));
        }
        paymentTransactionDto.setUpdatedAt(paymentTransaction.getUpdatedAt());
        if (paymentTransaction.getExpiresAt() != null && new Date().after(paymentTransaction.getExpiresAt()) && !PaymentStatus.SUCCESS.getStatus().equals(status)) {
            paymentTransactionDto.setStatus(PaymentStatus.EXPIRED.getStatus());
        } else {
            paymentTransactionDto.setStatus(status);
        }
        return paymentTransactionDto;
    }


    public String getPayNowUrl(String uuid) {
        return String.join("", serviceBaseUrl, PAY_NOW_PATH, uuid);
    }

    public <T> String buildKafkaEvent(T data, KafkaEventType kafkaEventType, String eventName) {
        KafkaEventDto<T> kafkaEventDto =
                new KafkaEventDto<T>();
        kafkaEventDto.setEventName(eventName);
        kafkaEventDto.setEventType(kafkaEventType.getLabel());
        kafkaEventDto.setEventSource(PAYMENT_EVENT_SOURCE);
        kafkaEventDto.setEventTime(new Date());
        kafkaEventDto.setDetails(data);
        String kafkaMessage = null;
        try {
            kafkaMessage = this.objectMapper.writeValueAsString(kafkaEventDto);
        } catch (JsonProcessingException e) {
            throw new InternalPaymentException(e.getMessage(), e);
        }
        return kafkaMessage;
    }

    public NotificationTransactionDto createNotificationTransactionDto(final PaymentTransaction paymentTransaction) {
        NotificationTransactionDto notificationTransactionDto = new NotificationTransactionDto();
        final PurchaseOrder purchaseOrder = paymentTransaction.getPurchaseOrder();
        notificationTransactionDto.setParams(
                Map.of(
                        "link", getPayNowUrl(purchaseOrder.getUuid())
                ));
        notificationTransactionDto.setReferenceType(String.format(PAYMENT_NOTIFICATION_REFERENCE_FORMAT, purchaseOrder.getReferenceType().toUpperCase()));
        notificationTransactionDto.setReceiverId(purchaseOrder.getCustomerId());
        notificationTransactionDto.setMethods(Arrays.asList("ALL"));
        notificationTransactionDto.setSource("PAYMENT_SERVICE");
        return notificationTransactionDto;
    }

    /**
     * Map referral reward balance to notification DTO
     */
    public NotificationTransactionDto createReferralNotificationDto(final RewardsBalance rewardsBalance) {
        NotificationTransactionDto notificationTransactionDto = new NotificationTransactionDto();
        notificationTransactionDto.setReferenceType(String.format(PAYMENT_NOTIFICATION_REFERENCE_FORMAT, "REFERRAL"));
        notificationTransactionDto.setReceiverId(rewardsBalance.getUserId());
        notificationTransactionDto.setMethods(List.of("PUSH_NOTIFICATION"));
        notificationTransactionDto.setSource("PAYMENT_SERVICE");
        return notificationTransactionDto;
    }

    /**
     * Build the dto for reward balance reporting which is wrapper for the event that send to warehouse service
     */
    public RewardBalanceReportingDto createRewardingBalanceReportingDto(final RewardsBalance rewardsBalance, String referralCode, boolean isReferrer) {
        RewardBalanceReportingDto rewardBalanceReportingDto = new RewardBalanceReportingDto();
        rewardBalanceReportingDto.setId(rewardsBalance.getId());
        rewardBalanceReportingDto.setCreatedAt(rewardsBalance.getCreatedAt());
        rewardBalanceReportingDto.setUpdatedAt(rewardsBalance.getUpdatedAt());
        rewardBalanceReportingDto.setRewardType(rewardsBalance.getRewardType());
        rewardBalanceReportingDto.setValue(rewardsBalance.getValue());
        rewardBalanceReportingDto.setReferralCode(referralCode);
        rewardBalanceReportingDto.setUserId(rewardsBalance.getUserId());
        rewardBalanceReportingDto.setReferrer(isReferrer);
        return rewardBalanceReportingDto;
    }

    /**
     * Build payment to warehouse event DTO
     */
    public PaymentWarehouseEvent createPaymentWarehouseEvent(PurchaseOrder purchaseOrder, PaymentTransaction paymentTransaction, PromoUsage promoUsage) {
        PaymentWarehouseEvent paymentWarehouseEvent = new PaymentWarehouseEvent();
        paymentWarehouseEvent.setId(purchaseOrder.getId());
        paymentWarehouseEvent.setCreatedAt(purchaseOrder.getCreatedAt());
        paymentWarehouseEvent.setUpdatedAt(purchaseOrder.getUpdatedAt());
        paymentWarehouseEvent.setReferenceId(purchaseOrder.getReferenceId());
        paymentWarehouseEvent.setReferenceType(purchaseOrder.getReferenceType());
        paymentWarehouseEvent.setUuid(purchaseOrder.getUuid());
        paymentWarehouseEvent.setUserId(purchaseOrder.getCustomerId());
        paymentWarehouseEvent.setFullName(purchaseOrder.getFullName());
        paymentWarehouseEvent.setPhoneNumber(purchaseOrder.getPhoneNumber());
        if (promoUsage != null) {
            paymentWarehouseEvent.setPromoId(promoUsage.getPromo().getId());
            paymentWarehouseEvent.setPromoCode(promoUsage.getPromo().getCode());
            paymentWarehouseEvent.setPromoDiscount(promoUsage.getPromo().getDiscount());
            paymentWarehouseEvent.setAmountAfterDiscount(new MoneyDto(purchaseOrder.getAmountAfterDiscount().getValue(), purchaseOrder.getAmountAfterDiscount().getCurrency()));
            paymentWarehouseEvent.setPromoUsedWithSuccessPayment(promoUsage.getUsedWithSuccessPayment());
        }
        paymentWarehouseEvent.setStatus(getPaymentStatus(paymentTransaction.getTransactionStatus()));
        paymentWarehouseEvent.setLink(getPayNowUrl(purchaseOrder.getUuid()));
        paymentWarehouseEvent.setAmount(new MoneyDto(purchaseOrder.getAmount().getValue(), purchaseOrder.getAmount().getCurrency()));
        paymentWarehouseEvent.setPaidAmount(new MoneyDto(paymentTransaction.getTransactionAmount(), paymentTransaction.getTransactionCurrency()));
        return paymentWarehouseEvent;
    }
}
