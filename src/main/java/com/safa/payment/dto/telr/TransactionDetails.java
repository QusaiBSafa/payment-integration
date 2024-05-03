package com.safa.payment.dto.telr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

import java.util.Date;

@JacksonXmlRootElement(localName = "transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TransactionDetails {

    public String id;

    @JacksonXmlProperty(localName = "prev_id")
    public String prevId;

    @JacksonXmlProperty(localName = "init_id")
    public String initId;

    @JacksonXmlProperty(localName = "agreementid")
    public String agreementId;

    public Double amount;

    @JacksonXmlProperty(localName = "amount_txt")
    public String amountTxt;

    public String currency;

    public String description;

    @JacksonXmlProperty(localName = "cartid")
    public String cartId;

    public int test;

    public Date date;
}
