package com.safa.payment.dto.noon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentDetails {
    private String instrument;
    private String mode;
    private String integratorAccount;
    private String paymentInfo;
    private String brand;
    private String scheme;
    private String expiryMonth;
    private String expiryYear;
    private String isNetworkToken;
    private String cardType;
    private String cardCountry;
    private String cardCountryName;
    private String cardIssuerName;
    private String cardIssuerPhone;
    private String cardIssuerWebsite;
}
