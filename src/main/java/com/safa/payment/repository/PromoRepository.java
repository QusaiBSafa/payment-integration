package com.safa.payment.repository;

import com.safa.payment.entity.Promo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface PromoRepository extends CrudRepository<Promo, Long> {

  /**
   * Finds a Promo entity by its code, ignoring case.
   *
   * @param code the code of the Promo entity to retrieve.
   * @return the Promo entity with the provided code, or null if no such Promo entity exists.
   */
  Promo findByCodeIgnoreCase(String code);

  /**
   * Finds all Promo entities where the 'hidden' field is false, and the 'expiresAt' field is after the specified date.
   *
   * @param date the date to compare with the 'expiresAt' field of the Promo entities.
   * @return a list of Promo entities matching the criteria, or an empty list if no such Promo entities exist.
   */
  List<Promo> findAllByHiddenIsFalseAndExpiresAtAfter(Date date);

  /**
   * Finds a Promo entity by its code, ignoring case, where the 'hidden' field is false, and the 'expiresAt' field is after the specified date.
   *
   * @param code the code of the Promo entity to retrieve.
   * @param date the date to compare with the 'expiresAt' field of the Promo entity.
   * @return the Promo entity matching the criteria, or null if no such Promo entity exists.
   */
  Promo findByCodeIgnoreCaseAndExpiresAtAfter(String code, Date date);
}
