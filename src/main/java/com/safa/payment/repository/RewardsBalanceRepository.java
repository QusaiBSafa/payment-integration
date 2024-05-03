package com.safa.payment.repository;

import com.safa.payment.entity.RewardsBalance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RewardsBalanceRepository extends CrudRepository<RewardsBalance, Long> {

    long countByUserId(long userId);

    List<RewardsBalance> findByUserId(long userid, Pageable pageable);

}
