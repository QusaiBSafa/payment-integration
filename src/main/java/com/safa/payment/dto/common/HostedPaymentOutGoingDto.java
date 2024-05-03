package com.safa.payment.dto.common;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class HostedPaymentOutGoingDto implements Serializable {

    private String url;

}
