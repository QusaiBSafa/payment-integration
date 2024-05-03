package com.safa.payment.dto.noon;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderRequestDto {

    //Purchase order reference
    private String reference;

    private String amount;

    private String currency;
    //Short description of the order.
    private String name;
    //Short description of the order.
    private String description;
    //Pre-defined and limited to Web and Mobile.
    private String channel;
    //Value of pre-configured order route categories.
    private String category;

    private String ipAddress;

}
