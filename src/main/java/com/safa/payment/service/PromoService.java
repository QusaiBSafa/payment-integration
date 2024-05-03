package com.safa.payment.service;

import com.safa.payment.dto.common.PromoOutgoingDto;
import com.safa.payment.entity.Promo;
import com.safa.payment.entity.PromoUsage;
import com.safa.payment.entity.PurchaseOrder;
import com.safa.payment.repository.PromoRepository;
import com.safa.payment.repository.PromoUsageRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Promo code and promo usage service
 * Promo codes must be saved in DB in upper case
 *
 * @author Qusai Safa
 */
@Service
@Transactional
public class PromoService {

    private final PromoUsageRepository promoUsageRepository;
    private final PromoRepository promoRepository;

    @Autowired
    @Lazy
    public PromoService(PromoUsageRepository promoUsageRepository, PromoRepository promoRepository) {
        this.promoUsageRepository = promoUsageRepository;
        this.promoRepository = promoRepository;
    }

    /**
     * Save promo code if promo code is already applied for this order then just return it. if a new
     * apply then save the usage.
     */
    public PromoUsage savePromoUsage(PurchaseOrder purchaseOrder, Promo promo, boolean isFullDiscount) {
        // If the promo code is already applied with this purchase order
        PromoUsage promoUsage =
                this.promoUsageRepository.findTopByPurchaseOrderIdAndPromo_codeIgnoreCaseOrderByIdDesc(
                        purchaseOrder.getId(), promo.getCode());
        if (promoUsage != null) {
            return promoUsage;
        }
        // The first time to apply this promo code with this order
        promoUsage = new PromoUsage();
        promoUsage.setUserId(purchaseOrder.getCustomerId());
        promoUsage.setPurchaseOrderId(purchaseOrder.getId());
        promoUsage.setPromo(promo);
        // If full discount then the promo code is used successfully,
        // but when it is not full discount we set this flag after we get the success update from telr after the payment is done using the card.
        promoUsage.setUsedWithSuccessPayment(isFullDiscount);
        return this.promoUsageRepository.save(promoUsage);
    }

    public PromoOutgoingDto findPromoByCode(long userId, String code) {
        Promo promo = validatePromoCode(userId, code);
        PromoOutgoingDto promoOutgoingDto = new PromoOutgoingDto();
        promoOutgoingDto.setCode(promo.getCode());
        promoOutgoingDto.setValid(true);
        promoOutgoingDto.setDiscount(promo.getDiscount());
        return promoOutgoingDto;
    }

    public Set<PromoOutgoingDto> findAvailablePromoCodesForUser(long userId) {
        Set<PromoOutgoingDto> promoDtos = new HashSet<>();
        List<Promo> promos = promoRepository.findAllByHiddenIsFalseAndExpiresAtAfter(new Date());
        promos.forEach(
                (promo) -> {
                    PromoOutgoingDto promoOutgoingDto = new PromoOutgoingDto();
                    promoOutgoingDto.setCode(promo.getCode());

                    promoOutgoingDto.setDiscount(promo.getDiscount());
                    promoOutgoingDto.setValid(!isPromoCodeExceededUsageLimit(userId, promo));
                    promoDtos.add(promoOutgoingDto);
                });
        return promoDtos;
    }

    public Promo findPromoByCode(String code) {
        return promoRepository.findByCodeIgnoreCase(code);
    }

    public PromoUsage setPromoCodeSuccessfullyUsed(long purchaseOrderId, String code) {
        PromoUsage promoUsage =
                this.promoUsageRepository.findTopByPurchaseOrderIdAndPromo_codeIgnoreCaseOrderByIdDesc(
                        purchaseOrderId, code);
        promoUsage.setUsedWithSuccessPayment(true);
        return this.promoUsageRepository.save(promoUsage);
    }

    /**
     * check if promo code is valid for this user or not
     */
    public Promo validatePromoCode(long userId, String promoCode) throws ResponseStatusException {
        Promo promo = findPromoByCode(promoCode);
        if (promo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid promo code.");
        } else if (promo.getExpiresAt() == null || !promo.getExpiresAt().after(new Date())) {
            throw new ResponseStatusException(HttpStatus.GONE, String.format("The promo code '%s' is no longer valid.", promoCode));
        } else if (isPromoCodeExceededUsageLimit(userId, promo)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    String.format("You have already reached usage limit for the promo code or its associated campaign '%s' has been reached.", promoCode));
        }
        return promo;
    }

    /**
     * Check if promo code or it's campaign exceeded successful usages by a user
     */
    private boolean isPromoCodeExceededUsageLimit(long userId, Promo promo) {

        List<String> listOfCodes = promo.getCampaign() != null ? promo.getCampaign().getPromoCodes().stream().map(Promo::getCode).toList() : List.of(promo.getCode());
        long numberOfUsage = promoUsageRepository.countByUserIdAndPromo_codeIgnoreCaseInAndUsedWithSuccessPaymentTrue(userId, listOfCodes);
        long maxNumberOfUsage = promo.getCampaign() != null ? promo.getCampaign().getNumberOfUsagePerUserLimit() : promo.getNumberOfUsagePerUserLimit();

        return numberOfUsage >= maxNumberOfUsage;
    }
}
