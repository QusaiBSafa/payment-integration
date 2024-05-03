package com.safa.payment.dto.telr;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class ReturnPageDto {

    private String authorised;

    private String declined;

    private String cancelled;

}
