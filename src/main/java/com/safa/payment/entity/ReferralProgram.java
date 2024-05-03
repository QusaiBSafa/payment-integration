package com.safa.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * Each referral program has a lookup code which is used in the referral code.
 * Also, the referral program contains the type of reward for both the referee and referrer
 *
 * @author Qusai Safa
 */
@Entity
@Table(name = "referral_program")
@Getter
@Setter
@ToString
public class ReferralProgram extends BaseEntity {

    @Column
    private String referralCode;

    /**
     * The user who owns this referral program, this value is nullable and we can relay on lookupCode + user Id in splitting the referral code.
     */
    @Column
    private Long referrerUserId;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private RewardType refereeRewardType;

    @Column
    private String refereeRewardValue;

    @Column
    @Enumerated(EnumType.STRING)
    private RewardType referrerRewardType;

    @Column
    private String referrerRewardValue;

    /**
     * Number of rewards the referrer can take per program
     */
    @Column
    private int referrerNumberOfRewards;

    /**
     * Number of rewards the referee can take per program
     */
    @Column
    private int refereeNumberOfRewards;

}
