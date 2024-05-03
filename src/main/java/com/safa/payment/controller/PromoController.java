package com.safa.payment.controller;

import com.safa.payment.dto.common.PromoOutgoingDto;
import com.safa.payment.service.PromoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This controller is responsible for handling promo codes http requests
 *
 * @author Qusai Safa
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin("*")
public class PromoController {

    private final PromoService promoService;

    @Autowired
    public PromoController(PromoService promoService) {
        this.promoService = promoService;
    }

    /**
     * Get All valid promo codes for a specific user
     */
    @Operation(summary = "Get all available promo codes for a user", security = {@SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/promo")
    public ResponseEntity<Set<PromoOutgoingDto>> getAllPromoCodes(@RequestParam(name = "userId") long userId) {
        return new ResponseEntity<>(promoService.findAvailablePromoCodesForUser(userId), HttpStatus.OK);
    }

    /**
     * Get Promo by code if valid
     */
    @Operation(summary = "Get Promo code API", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the promo", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = PromoOutgoingDto.class))}),
            @ApiResponse(responseCode = "404", description = "Invalid promo code", content = @Content),
            @ApiResponse(responseCode = "410", description = "The promo code '%s' is no longer valid", content = @Content),
            @ApiResponse(responseCode = "429", description = "You have already reached usage limit for the promo code or its associated campaign '%s' has been reached", content = @Content),
    })
    @GetMapping("/promo/{code}")
    public ResponseEntity<PromoOutgoingDto> getPromoByCode(@RequestParam(name = "userId") long userId, @PathVariable(name = "code") String code) {
        return new ResponseEntity<>(promoService.findPromoByCode(userId, code), HttpStatus.OK);
    }

    /**
     * TEMP: Get Promo by code if valid (Handle extra trailing slash to avoid throw 401 HTTP error)
     */
    @Operation(summary = "Get Promo code API", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Invalid promo code", content = @Content),
    })
    @GetMapping("/promo/")
    public ResponseEntity<PromoOutgoingDto> getPromoByCodes(@RequestParam(name = "userId") long userId) {
        return new ResponseEntity<>(promoService.findPromoByCode(userId, null), HttpStatus.OK);
    }

}
