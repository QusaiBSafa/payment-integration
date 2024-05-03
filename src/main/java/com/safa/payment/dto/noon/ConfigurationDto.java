package com.safa.payment.dto.noon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationDto {

    // Redirect back for direct integration
    private String returnUrl;

    // Checkout page language
    private String locale;

    // Show save card checkbox
    private boolean tokenizeCc;

    private String styleProfile;

    private String paymentAction;


}
