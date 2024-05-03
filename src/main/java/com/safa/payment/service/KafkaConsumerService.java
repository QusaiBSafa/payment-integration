package com.safa.payment.service;

import com.safa.payment.common.ConsultationStatus;
import com.safa.payment.common.KafkaEventType;
import com.safa.payment.common.PaymentReferenceType;
import com.safa.payment.dto.KafkaEventDto;
import com.safa.payment.dto.common.ConsultationIncomingDto;
import com.safa.payment.dto.common.PurchaseOrderIncomingDto;
import com.safa.payment.exception.InternalPaymentException;
import com.safa.payment.service.gateway.Telr.TelrRestService;
import com.safa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * This service fetch kafka events and uses appropriate handler.
 *
 * @author Qusai Safa
 */
@Service
@Slf4j
public class KafkaConsumerService {

  public static final String PURCHASE_ORDER_TOPIC = "payment_purchase_order";
  public static final String PAYMENT_GROUP_ID = "payment_group_id";
  public static final String ERROR_MESSAGE_TEMPLATE =
      "Kafka Consumer Topic %s %n message: %s %n localized: %s %n event: %s";


  private final PurchaseOrderService purchaseOrderService;

  private final ApplicationEventPublisher eventPublisher;
  private final PaymentUtil paymentUtil;
  private final RestService restService;

  @Autowired
  public KafkaConsumerService(PurchaseOrderService purchaseOrderService,
      ApplicationEventPublisher eventPublisher, PaymentUtil paymentUtil,
      TelrRestService telrRestService, RestService restService) {
    this.purchaseOrderService = purchaseOrderService;
    this.eventPublisher = eventPublisher;
    this.paymentUtil = paymentUtil;
    this.restService = restService;
  }

  @KafkaListener(topics = {PURCHASE_ORDER_TOPIC}, groupId = PAYMENT_GROUP_ID)
  public void consume(ConsumerRecord<String, String> record) {
    String event = record.value();
    try {
      KafkaEventDto<PurchaseOrderIncomingDto> kafkaEventDto =
          this.paymentUtil.mapToPurchaseOrderIncomingDto(event);
      if (kafkaEventDto == null) {
        throw new InternalPaymentException("Failed parsing consumed event, " + record.value());
      }
      PurchaseOrderIncomingDto purchaseOrderIncomingDto = kafkaEventDto.getDetails();
      if (purchaseOrderIncomingDto.getPaymentTransaction() == null) {
        throw new InternalPaymentException("Missing payment transaction, " + record.value());
      }
      KafkaEventType kafkaEventType =
          KafkaEventType.valueOf(kafkaEventDto.getEventType().toUpperCase());

      // Validate due amount if not delete event type, because in delete events no changes on amount
      if (!KafkaEventType.DELETE.equals(kafkaEventType)) {
        paymentUtil.validateAmount(purchaseOrderIncomingDto.getPaymentTransaction().getAmount());
      }

      switch (kafkaEventType) {
        case CREATE -> purchaseOrderService.createPurchaseOrder(purchaseOrderIncomingDto);
        case UPDATE -> purchaseOrderService.updatePurchaseOrder(purchaseOrderIncomingDto);
        case DELETE -> {
          PaymentReferenceType paymentReferenceType = PaymentReferenceType.valueOf(
              purchaseOrderIncomingDto.getPaymentTransaction().getReferenceType().toUpperCase());
          if (paymentReferenceType == PaymentReferenceType.SUBSCRIPTION) {
            purchaseOrderService.cancelRepeatPaymentAgreement(purchaseOrderIncomingDto);
          } else {
            purchaseOrderService.cancelLatestPaymentTransactionForReference(
                purchaseOrderIncomingDto.getPaymentTransaction().getReferenceId(),
                purchaseOrderIncomingDto.getPaymentTransaction().getReferenceType());
          }
        }
      }
    } catch (Exception exception) {
      this.restService.postErrorMessage(String.format(ERROR_MESSAGE_TEMPLATE, PURCHASE_ORDER_TOPIC,
          exception.getMessage(), exception.getLocalizedMessage(), event));
    }
  }

  /**
   * Consume consultation events
   */
  @KafkaListener(topics = {"${topic.consultation:consultation}"}, groupId = PAYMENT_GROUP_ID)
  public void consumeConsultationKafkaEvents(ConsumerRecord<String, String> record) {
    String event = record.value();
    try {
      KafkaEventDto<ConsultationIncomingDto> kafkaEventDto =
          this.paymentUtil.mapToConsultationDto(event);
      if (kafkaEventDto == null) {
        throw new InternalPaymentException("Failed parsing consumed event, " + record.value());
      }
      ConsultationIncomingDto consultationIncomingDto = kafkaEventDto.getDetails();
      if (consultationIncomingDto.getStatus().equals(ConsultationStatus.COMPLETED.toString())) {
        eventPublisher.publishEvent(consultationIncomingDto);
      }
    } catch (Exception exception) {
      this.restService.postErrorMessage(String.format(ERROR_MESSAGE_TEMPLATE, "consultations",
          exception.getMessage(), exception.getLocalizedMessage(), event));
    }
  }

}
