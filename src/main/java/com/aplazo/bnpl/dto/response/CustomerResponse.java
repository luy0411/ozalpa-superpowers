package com.aplazo.bnpl.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        BigDecimal creditLineAmount,
        BigDecimal availableCreditLineAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt
) {
}
