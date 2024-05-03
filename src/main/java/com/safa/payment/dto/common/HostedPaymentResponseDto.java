package com.safa.payment.dto.common;

import com.safa.payment.dto.telr.ErrorDto;
import com.safa.payment.dto.telr.OrderResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class HostedPaymentResponseDto {

    private String method;

    private String trace;

    private OrderResponseDto order;

    private ErrorDto error;
}
