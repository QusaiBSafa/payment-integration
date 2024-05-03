package com.safa.payment.controller;


import com.safa.payment.dto.IncomingDto;
import com.safa.payment.dto.common.PaymentTransactionDto;
import com.safa.payment.dto.common.PurchaseOrderOutgoingDto;
import com.safa.payment.service.PurchaseOrderService;
import com.safa.payment.service.gateway.PaymentTransactionServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This controller is responsible for handling purchase order http requests
 *
 * @author Qusai Safa
 */
@RestController
@RequestMapping("/api/v1/purchase-order")
@CrossOrigin()
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PaymentTransactionServiceFactory paymentTransactionServiceFactory;

    @Autowired
    PurchaseOrderController(PurchaseOrderService purchaseOrderService, PaymentTransactionServiceFactory paymentTransactionServiceFactory) {
        this.purchaseOrderService = purchaseOrderService;
        this.paymentTransactionServiceFactory = paymentTransactionServiceFactory;
    }

    /**
     * Apply promo code for purchase order
     */
    @Operation(summary = "Apply promo code API", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponse(responseCode = "200", description = "Promo code applied", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurchaseOrderOutgoingDto.class))})
    @PostMapping(path = "/promo", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<PurchaseOrderOutgoingDto> applyPromoCode(
            @Valid @RequestBody IncomingDto incomingDto) throws Exception {
        PurchaseOrderOutgoingDto response =
                purchaseOrderService.applyPromoForPurchaseOrder(incomingDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Find purchase order by reference id and reference type
     */
    @Operation(summary = "Get purchase order", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Found the purchase order", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurchaseOrderOutgoingDto.class))}),
            @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content)})
    @GetMapping()
    public ResponseEntity<PurchaseOrderOutgoingDto> findPurchaseOrder(
            @PathParam("referenceId") String referenceId,
            @PathParam("referenceType") String referenceType) {
        PurchaseOrderOutgoingDto response =
                purchaseOrderService.findPurchaseOrder(referenceId, referenceType, false);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    /**
     * Find payment transaction details for a purchase order by reference id and reference type
     */
    @Operation(summary = "Get latest payment transaction for a purchase order", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Found the purchase order", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurchaseOrderOutgoingDto.class))}),
            @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content)})
    @GetMapping(path = "/payment-transaction")
    public ResponseEntity<PaymentTransactionDto> findLatestPaymentTransactionForPurchaseOrder(
            @PathParam("referenceId") String referenceId,
            @PathParam("referenceType") String referenceType) {
        PaymentTransactionDto response =
                purchaseOrderService.findPurchaseOrderLatestTransaction(referenceId, referenceType, false);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    /**
     * Cancel latest payment transaction for a reference id and type
     */
    @Operation(summary = "Cancel latest payment transaction for a purchase order with reference id and type", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Found the purchase order"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content)})
    @DeleteMapping("/payment-transaction")
    public ResponseEntity<String> cancelLatestPaymentTransactionForReference(
            @PathParam("referenceId") String referenceId,
            @PathParam("referenceType") String referenceType) {
        purchaseOrderService.cancelLatestPaymentTransactionForReference(referenceId, referenceType);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
