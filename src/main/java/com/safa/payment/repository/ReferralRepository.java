package com.safa.payment.repository;

import com.safa.payment.entity.Referral;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ReferralRepository extends CrudRepository<Referral, Long> {

    Optional<Referral> findByRefereeId(long refereeId);

}
