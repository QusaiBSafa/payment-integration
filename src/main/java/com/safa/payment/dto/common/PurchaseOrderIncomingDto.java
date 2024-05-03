package com.safa.payment.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safa.payment.dto.UserDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PurchaseOrderIncomingDto {

    private PaymentTransactionDto paymentTransaction;

    private UserDto user;

    private boolean sendAutoNotification;

}
