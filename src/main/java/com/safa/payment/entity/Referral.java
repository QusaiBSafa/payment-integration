package com.safa.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * Save the used referral code, and the user id who owns this referral (referrerId) and the user id who used this referrer(refereeId)
 *
 * @author Qusai Safa
 */
@Entity
@Table(name = "referral")
@Getter
@Setter
@ToString
public class Referral extends BaseEntity {

    @Column(nullable = false)
    private long refereeId;

    @Column(nullable = false)
    private long referrerId;

    @Column
    private long referralProgramId;

    @Column
    private String referralCode;

}
