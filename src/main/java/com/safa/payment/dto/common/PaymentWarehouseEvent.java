package com.safa.payment.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.safa.payment.dto.MoneyDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentWarehouseEvent {

    private long id;

    private Date createdAt;

    private Date updatedAt;

    private String referenceId;

    private String referenceType;

    private String uuid;

    private String status;

    private String link;

    private Long userId;

    private String fullName;

    private String phoneNumber;

    private Long promoId;

    private String promoCode;

    private Double promoDiscount;

    private Boolean promoUsedWithSuccessPayment;

    private MoneyDto amount;

    private MoneyDto amountAfterDiscount;

    private MoneyDto paidAmount;

}
