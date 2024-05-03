package com.safa.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class IncomingDto implements Serializable {

    @JsonProperty(required = true)
    @NotEmpty
    @NotBlank
    private String referenceId;

    @JsonProperty(required = true)
    @NotEmpty
    @NotBlank
    private String referenceType;

    @JsonProperty(required = true)
    @NotEmpty
    @NotBlank
    private String promoCode;

    private String platform;

    private String appVersion;

}
