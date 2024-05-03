package com.safa.payment.dto.telr;

import com.safa.payment.common.PaymentReferenceType;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CartIdentifier {

    private PaymentReferenceType referenceType;

    private String referenceId;

    private int requestNumber;

    private String sourceCountryShortCode;

    private long instant;

    /**
     * Create cart id in format :
     * {referenceTypeShortCode}_{referenceId}_{requestNumber}_{sourceCountryShortCode}
     */
    public String createCartId() {
        return referenceType.getShortCode()
                + "_"
                + referenceId
                + "_"
                + requestNumber
                + "_"
                + sourceCountryShortCode
                + "_"
                + instant;
    }
}
