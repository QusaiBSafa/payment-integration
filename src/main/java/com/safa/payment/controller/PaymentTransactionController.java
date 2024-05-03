package com.safa.payment.controller;


import com.safa.payment.dto.noon.NoonTransactionIncomingDto;
import com.safa.payment.dto.telr.TelrPaymentTransactionIncomingDto;
import com.safa.payment.service.gateway.Noon.NoonPaymentTransactionService;
import com.safa.payment.service.gateway.Telr.TelrPaymentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Web hook APIs consume requests from third part APIs after payment proccess
 *
 * @author Qusai Safa
 */
@RestController
@RequestMapping("/api/v1/transaction")
@CrossOrigin()
public class PaymentTransactionController {


    public static final int WEBHOOK_MAX_RETRIES = 3;
    private final TelrPaymentTransactionService telrPaymentTransactionService;
    private final NoonPaymentTransactionService noonPaymentTransactionService;

    @Autowired
    PaymentTransactionController(TelrPaymentTransactionService telrPaymentTransactionService, NoonPaymentTransactionService noonPaymentTransactionService) {
        this.noonPaymentTransactionService = noonPaymentTransactionService;
        this.telrPaymentTransactionService = telrPaymentTransactionService;
    }

    /**
     * Telr gateway will send request to this API with the payment status and transaction details
     */
    @Operation(summary = "Payment transaction webhook API")
    @ApiResponse(responseCode = "200", description = "Webhook API to receive payment status from telr gateway")
    @PostMapping()
    public ResponseEntity<String> processTransaction(
            TelrPaymentTransactionIncomingDto paymentTransaction) {
        this.telrPaymentTransactionService.processPaymentTransaction(paymentTransaction);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Noon gateway will send request to this API with the payment status and transaction details
     */
    @Operation(summary = "Payment transaction webhook API")
    @ApiResponse(responseCode = "200", description = "Webhook API to receive payment status from noon gateway")
    @PostMapping("/noon")
    public ResponseEntity<String> processNoonTransaction(
            @RequestBody NoonTransactionIncomingDto paymentTransaction) {
        this.noonPaymentTransactionService.processPaymentTransaction(paymentTransaction, WEBHOOK_MAX_RETRIES);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Redirect to success or failed page based on payment status
     */
    @Operation(summary = "Redirect to status page based on noon payment status")
    @ApiResponse(responseCode = "302", description = "")
    @GetMapping(value = "/noon/redirect")
    public void redirectToStatusPage(
            HttpServletResponse httpServletResponse, @RequestParam("orderId") long orderId) {
        String url = this.noonPaymentTransactionService.getPaymentStatusPageUrl(orderId);
        // Find the payment gateway for this request
        httpServletResponse.setHeader("Location", url);
        httpServletResponse.setStatus(302);
    }
}
