package com.aplazo.bnpl.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "loans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_loans_external_id", columnNames = {"external_id"})
        }
)
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private UUID externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_name", nullable = false, length = 20)
    private PaymentScheme schemeName;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InstallmentEntity> installments = new ArrayList<>();

    public LoanEntity() {
    }

    public LoanEntity(Long id, UUID externalId, CustomerEntity customer, BigDecimal amount,
                      BigDecimal commissionAmount, BigDecimal totalAmount, PaymentScheme schemeName,
                      BigDecimal interestRate, LoanStatus status, Instant createdAt,
                      List<InstallmentEntity> installments) {
        this.id = id;
        this.externalId = externalId;
        this.customer = customer;
        this.amount = amount;
        this.commissionAmount = commissionAmount;
        this.totalAmount = totalAmount;
        this.schemeName = schemeName;
        this.interestRate = interestRate;
        this.status = status;
        this.createdAt = createdAt;
        if (installments != null) {
            this.installments = installments;
            for (InstallmentEntity installment : installments) {
                installment.setLoan(this);
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.externalId == null) {
            this.externalId = UUID.randomUUID();
        }
    }

    public void addInstallment(InstallmentEntity installment) {
        if (installment != null) {
            this.installments.add(installment);
            installment.setLoan(this);
        }
    }

    public void removeInstallment(InstallmentEntity installment) {
        if (installment != null) {
            this.installments.remove(installment);
            installment.setLoan(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public void setExternalId(UUID externalId) {
        this.externalId = externalId;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentScheme getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(PaymentScheme schemeName) {
        this.schemeName = schemeName;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<InstallmentEntity> getInstallments() {
        return installments;
    }

    public void setInstallments(List<InstallmentEntity> installments) {
        this.installments = installments != null ? installments : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private UUID externalId;
        private CustomerEntity customer;
        private BigDecimal amount;
        private BigDecimal commissionAmount;
        private BigDecimal totalAmount;
        private PaymentScheme schemeName;
        private BigDecimal interestRate;
        private LoanStatus status;
        private Instant createdAt;
        private List<InstallmentEntity> installments = new ArrayList<>();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder externalId(UUID externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder customer(CustomerEntity customer) {
            this.customer = customer;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder commissionAmount(BigDecimal commissionAmount) {
            this.commissionAmount = commissionAmount;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder schemeName(PaymentScheme schemeName) {
            this.schemeName = schemeName;
            return this;
        }

        public Builder interestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
            return this;
        }

        public Builder status(LoanStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder installments(List<InstallmentEntity> installments) {
            this.installments = installments;
            return this;
        }

        public LoanEntity build() {
            return new LoanEntity(id, externalId, customer, amount, commissionAmount, totalAmount,
                    schemeName, interestRate, status, createdAt, installments);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanEntity that)) return false;
        return externalId != null && Objects.equals(externalId, that.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LoanEntity{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", amount=" + amount +
                ", commissionAmount=" + commissionAmount +
                ", totalAmount=" + totalAmount +
                ", schemeName=" + schemeName +
                ", interestRate=" + interestRate +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
