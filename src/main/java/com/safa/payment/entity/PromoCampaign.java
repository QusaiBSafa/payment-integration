package com.safa.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Entity
@Table(name = "promo_campaign")
@Getter
@Setter
@ToString
public class PromoCampaign extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(columnDefinition = "integer DEFAULT 1")
    private Integer numberOfUsagePerUserLimit = 1;

    @OneToMany(mappedBy = "campaign")
    private List<Promo> promoCodes;

}
