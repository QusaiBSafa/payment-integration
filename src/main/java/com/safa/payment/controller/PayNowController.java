package com.safa.payment.controller;


import com.safa.payment.entity.PurchaseOrder;
import com.safa.payment.service.PurchaseOrderService;
import com.safa.payment.service.gateway.IPaymentTransactionService;
import com.safa.payment.service.gateway.PaymentTransactionServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * This controller responsible for creating the hosted payment and redirect to it based on the
 * purchase order UUID
 *
 * @author Qusai Safa
 */
@RestController
@RequestMapping("/api/v1/pay-now")
@CrossOrigin()
public class PayNowController {

    private final PaymentTransactionServiceFactory paymentTransactionServiceFactory;
    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    PayNowController(PaymentTransactionServiceFactory paymentTransactionServiceFactory, PurchaseOrderService purchaseOrderService) {
        this.paymentTransactionServiceFactory = paymentTransactionServiceFactory;
        this.purchaseOrderService = purchaseOrderService;

    }

    /**
     * Create telr payment link for a specific purchase order based on the path variable {uuid} and redirect to the created payment link
     */
    @Operation(summary = "Pay now API")
    @ApiResponse(responseCode = "302", description = "Pay-now API which creates instant telr payment link and redirect to telr hosted payment page")
    @GetMapping(value = "/{uuid}")
    public void redirectToPaymentPage(
            HttpServletResponse httpServletResponse, @PathVariable("uuid") String uuid) {
        PurchaseOrder order = this.purchaseOrderService.findByUuid(uuid);
        // Find the payment gateway for this request
        IPaymentTransactionService paymentTransactionService = this.paymentTransactionServiceFactory.getInstanceByUuid(uuid);
        String url =
                paymentTransactionService.createHostedPaymentUrl(order);
        httpServletResponse.setHeader("Location", url);
        httpServletResponse.setStatus(302);
    }

}
