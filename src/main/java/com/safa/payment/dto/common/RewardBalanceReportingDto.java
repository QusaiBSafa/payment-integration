package com.safa.payment.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class RewardBalanceReportingDto extends RewardsBalanceOutgoingDto {

    /**
     * Is the reward belongs to referrer if not then its belong to referee
     */
    @JsonProperty
    private boolean isReferrer;

    /**
     * Needed for warehouse, used referral code to claim this reward, ex: AAA1234
     */
    @JsonProperty
    private String referralCode;
}
