package com.safa.payment.dto.noon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultOrderDetails {
    private String status;
    private Date creationTime;
    private int errorCode;
    private long id;
    private BigDecimal amount;
    private String currency;
    private String name;
    private String description;
    private String reference;
    private String category;
    private String channel;
}
