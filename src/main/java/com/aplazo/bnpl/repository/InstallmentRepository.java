package com.aplazo.bnpl.repository;

import com.aplazo.bnpl.entity.InstallmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository<InstallmentEntity, Long> {

    List<InstallmentEntity> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    List<InstallmentEntity> findByLoanExternalIdOrderByInstallmentNumberAsc(UUID loanExternalId);
}
