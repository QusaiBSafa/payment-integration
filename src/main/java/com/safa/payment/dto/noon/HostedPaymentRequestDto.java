package com.safa.payment.dto.noon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostedPaymentRequestDto extends com.safa.payment.dto.common.HostedPaymentRequestDto {

    private String apiOperation;

    private OrderRequestDto order;

    private ShippingDto billing;

    private ShippingDto shipping;

    private ConfigurationDto configuration;

}
