package com.safa.payment.dto.common;


import com.safa.payment.dto.common.BaseEntityDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ReferralOutgoingDto extends BaseEntityDto {

    private long refereeId;

    private long referrerId;

    private long referralProgramId;

    private String referralCode;
}
