package com.safa.payment.controller;

import com.safa.payment.dto.IncomingDto;
import com.safa.payment.dto.common.HostedPaymentOutGoingDto;
import com.safa.payment.service.gateway.IPaymentTransactionService;
import com.safa.payment.service.gateway.PaymentTransactionServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hosted-payment")
@CrossOrigin()
public class HostedPaymentController {

    private final PaymentTransactionServiceFactory paymentTransactionServiceFactory;

    @Autowired
    HostedPaymentController(PaymentTransactionServiceFactory paymentTransactionServiceFactory) {
        this.paymentTransactionServiceFactory = paymentTransactionServiceFactory;
    }

    /**
     * Get payment page url through sending request to telr payment gateway
     */
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    @PostMapping("/link")
    public ResponseEntity<HostedPaymentOutGoingDto> getHostedPaymentPage(
            @RequestBody IncomingDto paymentIncomingDto) {
        // Find the payment gateway for this request
        IPaymentTransactionService paymentTransactionService = this.paymentTransactionServiceFactory.getInstanceByReference(paymentIncomingDto.getReferenceId(), paymentIncomingDto.getReferenceType());
        HostedPaymentOutGoingDto response =
                paymentTransactionService.getPayNowUrlByReference(
                        paymentIncomingDto.getReferenceId(), paymentIncomingDto.getReferenceType());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
