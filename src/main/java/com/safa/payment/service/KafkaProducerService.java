package com.safa.payment.service;

import com.safa.payment.common.KafkaEventType;
import com.safa.payment.dto.*;
import com.safa.payment.dto.common.PaymentTransactionDto;
import com.safa.payment.dto.common.PaymentWarehouseEvent;
import com.safa.payment.dto.common.ReferralOutgoingDto;
import com.safa.payment.dto.common.RewardBalanceReportingDto;
import com.safa.payment.entity.PaymentTransaction;
import com.safa.payment.entity.PromoUsage;
import com.safa.payment.entity.PurchaseOrder;
import com.safa.payment.util.PaymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * This service send kafka events to a specified topic
 *
 * @author Qusai Safa
 */
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentUtil paymentUtil;

    @Value("${topic.payment-transaction.name}")
    private String paymentTransactionTopic;

    @Value("${topic.notifications.name:notifications_events}")
    private String notificationsTopic;

    @Value("${topic.rewards-balance.name:rewards_balance}")
    private String rewardsBalance;

    @Value("${topic.referral.name:referral}")
    private String referralTopic;

    @Value("${topic.payment-warehouse.name:payment_event}")
    private String paymentWarehouseTopic;


    Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    public KafkaProducerService(PaymentUtil paymentUtil, KafkaTemplate<String, String> kafkaTemplate) {
        this.paymentUtil = paymentUtil;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void sendPaymentTransactionEvent(final PaymentTransaction paymentTransaction, final PurchaseOrder purchaseOrder, PromoUsage promoUsage, KafkaEventType kafkaEventType) {
        final PaymentTransactionDto paymentTransactionDto = paymentUtil.createPaymentTransactionDto(paymentTransaction, purchaseOrder);
        String paymentTransactionMessage =
                paymentUtil.buildKafkaEvent(paymentTransactionDto, kafkaEventType, PaymentUtil.PAYMENT_EVENT_NAME);
        send(paymentTransactionTopic, paymentTransactionMessage, 3);

        final PaymentWarehouseEvent paymentWarehouseEvent = paymentUtil.createPaymentWarehouseEvent(paymentTransaction.getPurchaseOrder(), paymentTransaction, promoUsage);
        String paymentWarehouseMessage =
                paymentUtil.buildKafkaEvent(paymentWarehouseEvent, kafkaEventType, PaymentUtil.PAYMENT_EVENT_NAME);
        send(paymentWarehouseTopic, paymentWarehouseMessage, 3);

    }

    /**
     * Listen to internal event for new created referral which is published by referral service
     * And after that map to kafka event and send to referral topic which will be consumed by other microservices
     */
    @Async
    @EventListener(ReferralOutgoingDto.class)
    public void sendNewReferralEvent(ReferralOutgoingDto referral) {
        String referralMessage =
                paymentUtil.buildKafkaEvent(referral, KafkaEventType.UPDATE, PaymentUtil.REFERRAL_EVENT_NAME);
        send(referralTopic, referralMessage, 3);
    }


    @Async
    @EventListener(NotificationTransactionDto.class)
    public void sendNotificationEvent(NotificationTransactionDto notificationTransactionDto) {
        String notificationTransactionMessage =
                paymentUtil.buildKafkaEvent(notificationTransactionDto, KafkaEventType.UPDATE, PaymentUtil.PAYMENT_EVENT_NAME);
        send(notificationsTopic, notificationTransactionMessage, 3);
    }

    @Async
    @EventListener(RewardBalanceReportingDto.class)
    public void sendRewardBalanceReportingDto(RewardBalanceReportingDto rewardBalanceReportingDto) {
        String rewardBalanceReportingMessage =
                paymentUtil.buildKafkaEvent(rewardBalanceReportingDto, KafkaEventType.CREATE, PaymentUtil.PAYMENT_EVENT_NAME);
        send(rewardsBalance, rewardBalanceReportingMessage, 3);
    }

    public void send(String transactionTopic, String message, Integer retryTimes) {
        try {
            kafkaTemplate.send(transactionTopic, message);
            logger.info("Send message to kafka topic({}): {}", transactionTopic, message);
        } catch (Exception e) {
            logger.error("Error while sending kafka event", e);
            if (retryTimes > 0) {
                logger.info("Retry sending the event to kafka, retry number:" + retryTimes);
                send(transactionTopic, message, --retryTimes);
            }
        }
    }
}
