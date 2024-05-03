package com.safa.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "promo")
@Getter
@Setter
@ToString
public class Promo extends BaseEntity {

    @Column(unique = true, name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "expiresAt")
    private Date expiresAt;

    @Column(name = "discount")
    private Double discount;

    @Column(columnDefinition = "integer DEFAULT 1")
    private Integer numberOfUsagePerUserLimit = 1;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean hidden;

    @OneToMany(mappedBy = "promo")
    private List<PromoUsage> promoUsageList;

    @ManyToOne
    private PromoCampaign campaign;

}
