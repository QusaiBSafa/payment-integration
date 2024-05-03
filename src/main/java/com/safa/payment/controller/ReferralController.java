package com.safa.payment.controller;


import com.safa.payment.dto.common.ReferralIncomingDto;
import com.safa.payment.dto.common.ReferralOutgoingDto;
import com.safa.payment.dto.common.RewardsBalanceOutgoingDto;
import com.safa.payment.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;


import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1/referrals")
@CrossOrigin("*")
public class ReferralController {

    private final ReferralService referralService;

    private final static String ROLE_ADMIN = "ROLE_ADMIN";

    @Autowired
    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }


    /**
     * Use referrer code
     */
    @Operation(summary = "Use referral code", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponse(responseCode = "200", description = "Referral code usage saved", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ReferralIncomingDto.class))})
    @PostMapping(value = "/apply", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ReferralOutgoingDto> applyReferralCode(HttpServletRequest request,
                                                                 @Valid @RequestBody ReferralIncomingDto referralIncomingDto) throws Exception {
        var userId = request.getUserPrincipal().getName();
        ReferralOutgoingDto referralOutgoingDto =
                referralService.applyReferralCode(referralIncomingDto, Long.parseLong(userId));
        return new ResponseEntity<>(referralOutgoingDto, HttpStatus.OK);
    }


    /**
     * Get loggedIn user rewards balance
     */
    @Operation(summary = "Get user rewards balance", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User has rewards balance", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "404", description = "No rewards found for this user", content = @Content)})
    @GetMapping("/rewards-balance")
    public ResponseEntity<List<RewardsBalanceOutgoingDto>> getRewardsBalanceForLoggedInUser(HttpServletRequest request, @PageableDefault(page = 0, size = 1)
    @SortDefault.SortDefaults({
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
    }) Pageable pageable) {
        var userId = request.getUserPrincipal().getName();
        return new ResponseEntity<>(referralService.getRewardsBalanceByUserId(Long.parseLong(userId), pageable), HttpStatus.OK);
    }

    /**
     * Get rewards balance by user id, this API for admin only
     */
    @Operation(summary = "Get user rewards balance by user id", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User has rewards balance", content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "404", description = "No rewards found for this user", content = @Content)})
    @GetMapping("/rewards-balance/users/{userId}")
    @Secured(ROLE_ADMIN)
    public ResponseEntity<List<RewardsBalanceOutgoingDto>> getRewardsBalanceByUserId(@PathParam("userId") long userId, HttpServletRequest request, @PageableDefault(page = 0, size = 1)
    @SortDefault.SortDefaults({
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
    }) Pageable pageable) {
        return new ResponseEntity<>(referralService.getRewardsBalanceByUserId(userId, pageable), HttpStatus.OK);
    }

}
