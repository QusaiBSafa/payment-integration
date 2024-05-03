package com.safa.payment.repository;

import com.safa.payment.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    PurchaseOrder findByReferenceIdAndReferenceTypeIgnoreCase(String referenceId, String referenceType);

    PurchaseOrder findByUuid(String uuid);
}
