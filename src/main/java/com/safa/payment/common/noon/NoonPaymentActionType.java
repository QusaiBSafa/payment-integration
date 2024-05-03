package com.safa.payment.common.noon;

/**
 * https://docs.noonpayments.com/payment-api/reference/sale
 */
public enum NoonPaymentActionType {
    AUTHORIZE, // Authorization is an approval from the customer/buyer’s card issuing bank that the card is valid and has enough funds to cover the transaction amount
    SALE, // Sale is the type of transaction which combines the Authorization and Capture
    REVERSE, // Reverse (also referred as void authorization) is the type of transaction which alerts the card issuing bank to remove the hold on the amount which was taken during the Authorization call
    CAPTURE, // Capture is a follow-up call on the Authorization transaction which triggers the message to the card issuing bank to move the funds into the merchant account
    REFUND, // Refund is the type of transaction to trigger the request to move back the funds from the merchant account to the customer’s card.
}
