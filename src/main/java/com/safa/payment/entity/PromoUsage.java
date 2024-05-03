package com.safa.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "promo_usage")
@Getter
@Setter
@ToString
public class PromoUsage extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "product")
    private Long product;

    @ManyToOne
    @JoinColumn(name = "promo_code")
    private Promo promo;

    // If the usage is not count for this user due to failed payment
    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean usedWithSuccessPayment;

}
