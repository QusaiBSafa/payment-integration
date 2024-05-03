package com.safa.payment.dto.telr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class OrderResponseDto {

    private String ref;

    private String url;
}

