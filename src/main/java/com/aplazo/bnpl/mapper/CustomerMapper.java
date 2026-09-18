package com.aplazo.bnpl.mapper;

import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.entity.CustomerEntity;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(CustomerEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CustomerResponse(
                entity.getExternalId(),
                entity.getCreditLineAmount(),
                entity.getAvailableCreditLineAmount(),
                entity.getCreatedAt()
        );
    }
}
