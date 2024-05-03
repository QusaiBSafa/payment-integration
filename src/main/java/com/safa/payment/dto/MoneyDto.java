package com.safa.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MoneyDto {

    private BigDecimal value;

    private String currency;

}
