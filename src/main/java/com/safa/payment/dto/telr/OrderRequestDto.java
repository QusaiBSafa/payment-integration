package com.safa.payment.dto.telr;
public class OrderRequestDto {

    private String ref;

    private String cartid;

    private String test;

    private String amount;
    /**
     * the number of times the payment is taken each month, if the value is 1 means the payment will be taken one time each month
     */
    private Integer interval;

    /**
     * Indicates the number of regular payments to take. Set this to zero to indicate an unlimited term.
     */
    private Integer term;

    private String currency;

    private String description;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getCartid() {
        return cartid;
    }

    public void setCartid(String cartid) {
        this.cartid = cartid;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getTerm() {
        return term;
    }

    public void setTerm(Integer term) {
        this.term = term;
    }
}
