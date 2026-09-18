package com.aplazo.bnpl.repository;

import com.aplazo.bnpl.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    Optional<LoanEntity> findByExternalId(UUID externalId);

    List<LoanEntity> findByCustomerId(Long customerId);

    List<LoanEntity> findByCustomerExternalId(UUID customerExternalId);
}
