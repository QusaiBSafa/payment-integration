package com.safa.payment.entity;

import com.safa.payment.listener.PaymentTransactionEntityListener;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@ToString
@EntityListeners(PaymentTransactionEntityListener.class)
public class PaymentTransaction extends BaseEntity {

    @Column(name = "transaction_cart_id")
    private String transactionCartId;

    @Column(name = "transaction_store")
    private String transactionStore;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "transaction_previous_reference")
    private String transactionPreviousReference;

    @Column(name = "transaction_first_reference")
    private String transactionFirstReference;

    @Column(name = "transaction_currency")
    private String transactionCurrency;

    @Column(name = "transaction_amount")
    private BigDecimal transactionAmount;

    @Column(name = "transaction_description")
    private String transactionDescription;

    @Column(name = "transaction_status")
    private String transactionStatus;

    @Column(name = "transaction_auth_code")
    private String transactionAuthCode;

    @Column(name = "transaction_auth_message")
    private String transactionAuthMessage;

    @Column(name = "card_code")
    private String cardCode;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_check")
    private String cardCheck;

    @Column(name = "cart_language")
    private String cartLanguage;

    @Column(name = "integration_id")
    private int integrationId;

    @Column(name = "bill_firstname")
    private String billFirstname;

    @Column(name = "bill_surename")
    private String billSurename;

    @Column(name = "bill_address")
    private String billAddress1;

    @Column(name = "bill_city")
    private String billCity;

    @Column(name = "bill_country")
    private String billCountry;

    @Column(name = "bill_email")
    private String billEmail;

    @Column(name = "bill_phone")
    private String billPhone;

    @Column(name = "bill_check")
    private String billCheck;

    @Column(name = "expiresAt")
    private Date expiresAt;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

}
