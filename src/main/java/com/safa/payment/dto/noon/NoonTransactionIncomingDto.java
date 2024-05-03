package com.safa.payment.dto.noon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.safa.payment.dto.common.PaymentTransactionIncomingDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoonTransactionIncomingDto extends PaymentTransactionIncomingDto {

    @JsonProperty
    private String orderId;

    @JsonProperty
    private String orderStatus;

    @JsonProperty
    private String eventType;

    @JsonProperty
    private String eventId;

    @JsonProperty
    private String signature;

    @JsonProperty
    private String timeStamp;

    @JsonProperty
    private String originalOrderId;

    @JsonProperty
    private String merchantOrderReference;

    @JsonProperty
    private String attemptNumber;


}
