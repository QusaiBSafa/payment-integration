package com.safa.payment.entity;

import com.safa.payment.common.PaymentGateway;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Table(
        name = "purchase_order",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"referenceId", "referenceType"})})
@Entity
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class PurchaseOrder extends BaseEntity {

    @Column(nullable = false)
    private String referenceId;

    @Column()
    private String uuid;

    @Column(nullable = false)
    private String referenceType;

    @Column(nullable = false)
    private Long customerId;

    @Column()
    private String promoCode;

    @Column(name = "discount")
    private Double discount;

    @Column()
    private String fullName;

    @Embedded
    private Money amount;

    @Embedded()
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "amount_after_discount_value")),
            @AttributeOverride(name = "currency", column = @Column(name = "amount_after_discount_currency"))
    })
    private Money amountAfterDiscount;

    @Column()
    private String country;

    @Column()
    private String city;

    @Column()
    private String phoneNumber;

    @Column()
    private String formattedAddress;

    @Column()
    private Integer billingInterval;

    @Column()
    private Integer billingTerm;

    @Column()
    private String email;

    @OneToMany(mappedBy = "purchaseOrder", fetch = FetchType.EAGER)
    private List<PaymentTransaction> paymentTransactions;

    /**
     * Determine the payment gateway that will be used for this request
     */
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'TELR'")
    @Enumerated(EnumType.STRING)
    private PaymentGateway paymentGateway;

    /**
     * Language the payment will be shown in
     */
    @Column()
    private String language;
}
