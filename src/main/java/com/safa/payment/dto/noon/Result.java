package com.safa.payment.dto.noon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
    private String nextActions;
    private ResultOrderDetails order;
    private PaymentDetails paymentDetails;
}
