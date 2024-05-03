package com.safa.payment.repository;

import com.safa.payment.entity.PromoUsage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PromoUsageRepository extends CrudRepository<PromoUsage, Long> {

    /**
     * Counts the number of successful usages by a specific user of any promo code from a provided list,
     * case-insensitive.
     *
     * @param userId the ID of the user.
     * @param promoCodes the list of promo codes to search for.
     * @return the number of successful usages by the user of any promo code from the provided list.
     */
    long countByUserIdAndPromo_codeIgnoreCaseInAndUsedWithSuccessPaymentTrue(long userId, List<String> promoCodes);

    /**
     * Finds the most recent successful usage of a specific promo code by a specific user,
     * case-insensitive.
     *
     * @param purchaseOrderId the ID of the purchase order.
     * @param promoCode the promo code to search for.
     * @return a PromoUsage object representing the most recent successful usage of the promo code by the user.
     */
    PromoUsage findTopByPurchaseOrderIdAndPromo_codeIgnoreCaseOrderByIdDesc(long purchaseOrderId, String promoCode);
}
