package com.safa.payment.repository;

import com.safa.payment.entity.ReferralProgram;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ReferralProgramRepository extends CrudRepository<ReferralProgram, Long> {

    Optional<ReferralProgram> findByReferralCode(String referralCode);

}
