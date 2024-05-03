package com.safa.payment.dto.common;

import com.safa.payment.dto.MoneyDto;
import com.safa.payment.dto.telr.PaymentMethodDetailsDto;
import com.safa.payment.entity.PurchaseOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PurchaseOrderOutgoingDto {
    private Long id;

    private Long userId;

    /**
     * {@link PurchaseOrder#getReferenceId()}
     */
    private String referenceId;

    /**
     * {@link PurchaseOrder#getReferenceType()}
     */
    private String referenceType;

    private MoneyDto amount;

    private Double discount;

    private MoneyDto amountAfterDiscount;

    private String promoCode;

    private PaymentMethodDetailsDto paymentMethodDetails;
}
