package com.safa.payment.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.safa.payment.dto.common.BaseEntityDto;
import com.safa.payment.entity.RewardType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class RewardsBalanceOutgoingDto extends BaseEntityDto {

    @JsonProperty
    private RewardType rewardType;

    @JsonProperty
    private String value;

    @JsonProperty
    private String description;

    @JsonProperty
    private long userId;

}
