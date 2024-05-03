package com.safa.payment.dto.telr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
 import com.safa.payment.dto.common.PaymentTransactionIncomingDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelrPaymentTransactionIncomingDto extends PaymentTransactionIncomingDto {

    private String tran_store;

    private String tran_type;

    private String tran_class;

    private String tran_test;

    private String tran_ref;

    private String tran_prevref;

    private String tran_firstref;

    private String tran_currency;

    private String tran_order;

    private String tran_amount;

    private String tran_cartid;

    private String tran_desc;

    private String tran_status;

    private String tran_authcode;

    private String tran_authmessage;

    private String tran_check;

    private String card_code;

    private String card_last4;

    private String card_check;

    private String cart_lang;

    private String integration_id;

    private String bill_fname;

    private String bill_sname;

    private String bill_addr1;

    private String bill_city;

    private String bill_country;

    private String bill_email;

    private String bill_phone1;

    private String bill_check;

    private String link;
}
