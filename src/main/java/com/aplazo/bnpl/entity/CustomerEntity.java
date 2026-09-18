package com.aplazo.bnpl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customers_external_id", columnNames = {"external_id"})
        }
)
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private UUID externalId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "second_last_name", nullable = false, length = 100)
    private String secondLastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "credit_line_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLineAmount;

    @Column(name = "available_credit_line_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableCreditLineAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CustomerEntity() {
    }

    public CustomerEntity(Long id, UUID externalId, String firstName, String lastName, String secondLastName,
                          LocalDate dateOfBirth, BigDecimal creditLineAmount, BigDecimal availableCreditLineAmount,
                          Instant createdAt) {
        this.id = id;
        this.externalId = externalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.secondLastName = secondLastName;
        this.dateOfBirth = dateOfBirth;
        this.creditLineAmount = creditLineAmount;
        this.availableCreditLineAmount = availableCreditLineAmount;
        this.createdAt = createdAt;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public BigDecimal getCreditLineAmount() {
        return creditLineAmount;
    }

    public void setCreditLineAmount(BigDecimal creditLineAmount) {
        this.creditLineAmount = creditLineAmount;
    }

    public BigDecimal getAvailableCreditLineAmount() {
        return availableCreditLineAmount;
    }

    public void setAvailableCreditLineAmount(BigDecimal availableCreditLineAmount) {
        this.availableCreditLineAmount = availableCreditLineAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private UUID externalId;
        private String firstName;
        private String lastName;
        private String secondLastName;
        private LocalDate dateOfBirth;
        private BigDecimal creditLineAmount;
        private BigDecimal availableCreditLineAmount;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder externalId(UUID externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder secondLastName(String secondLastName) {
            this.secondLastName = secondLastName;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder creditLineAmount(BigDecimal creditLineAmount) {
            this.creditLineAmount = creditLineAmount;
            return this;
        }

        public Builder availableCreditLineAmount(BigDecimal availableCreditLineAmount) {
            this.availableCreditLineAmount = availableCreditLineAmount;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CustomerEntity build() {
            return new CustomerEntity(id, externalId, firstName, lastName, secondLastName,
                    dateOfBirth, creditLineAmount, availableCreditLineAmount, createdAt);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerEntity that)) return false;
        return externalId != null && Objects.equals(externalId, that.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CustomerEntity{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", creditLineAmount=" + creditLineAmount +
                ", availableCreditLineAmount=" + availableCreditLineAmount +
                ", createdAt=" + createdAt +
                '}';
    }
}
