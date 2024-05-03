package com.safa.payment.dto.telr;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodDetailsDto {

    private String cardLast4;

    private String cardCode;

    private String method;

}
