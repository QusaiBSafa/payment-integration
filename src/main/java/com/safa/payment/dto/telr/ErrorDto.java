package com.safa.payment.dto.telr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ErrorDto {

    private String message;

    private String note;

    private String details;

}
