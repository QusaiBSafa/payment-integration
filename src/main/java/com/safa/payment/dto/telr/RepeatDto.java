package com.safa.payment.dto.telr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RepeatDto {

    private int interval;

    private int term;

    private String period;

    private String amount;

    private String currency;

    private String start;
}
