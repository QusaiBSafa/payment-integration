package com.safa.payment.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PromoOutgoingDto {

    private String code;

    private Double discount;

    /**
     * Is it possible to use the promo for a specific user one more time
     */
    private boolean valid;
}
