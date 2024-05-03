package com.safa.payment.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.safa.payment.dto.MoneyDto;
import com.safa.payment.dto.telr.PaymentMethodDetailsDto;
import com.safa.payment.entity.PaymentTransaction;
import com.safa.payment.entity.PurchaseOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PaymentTransactionDto {
    /**
     * {@link PaymentTransaction#getId()}
     */
    private Long id;

    private Long userId;

    private String gatewayReference;

    /**
     * {@link PurchaseOrder#getReferenceId()}
     */
    private String referenceId;

    /**
     * {@link PurchaseOrder#getReferenceType()}
     */
    private String referenceType;

    private MoneyDto amount;

    private MoneyDto paidAmount;

    private String promoCode;

    private MoneyDto amountAfterDiscount;

    private PaymentMethodDetailsDto paymentMethodDetails;

    private String status;

    private String transactionReference;

    private String link;

    private Integer billingInterval;

    private Integer billingTerm;

    private Date updatedAt;

}
