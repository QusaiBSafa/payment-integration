package com.safa.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * Save the rewards details claimed by each user (referrer or referee)
 *
 * @author Qusai Safa
 */
@Entity
@Table(name = "rewards_balance")
@Getter
@Setter
@ToString
public class RewardsBalance extends BaseEntity {

    @Column
    @Enumerated(EnumType.STRING)
    private RewardType rewardType;

    @Column
    private String value;

    /**
     * Ex: claimed when referral code AAA123 is used by user id 2222
     */
    @Column
    private String description;

    @Column
    private long userId;
}
