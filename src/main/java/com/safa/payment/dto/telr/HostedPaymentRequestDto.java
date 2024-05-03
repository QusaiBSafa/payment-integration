package com.safa.payment.dto.telr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HostedPaymentRequestDto extends com.safa.payment.dto.common.HostedPaymentRequestDto {

    private String method;

    private int store;

    private String authkey;

    private OrderRequestDto order;

    private RepeatDto repeat;

    private CustomerDto customer;

    @JsonProperty("return")
    private ReturnPageDto returnPage;

}
