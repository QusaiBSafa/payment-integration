package com.safa.payment.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SlackErrorMessageDto {

    private String errorMessage;

    private String code;

    public SlackErrorMessageDto(String errorMessage) {
        this.errorMessage = errorMessage;
    }


}
