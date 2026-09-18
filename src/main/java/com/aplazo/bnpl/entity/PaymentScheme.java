package com.aplazo.bnpl.entity;

import java.math.BigDecimal;

public enum PaymentScheme {
    SCHEME_1(new BigDecimal("0.13")),
    SCHEME_2(new BigDecimal("0.16"));

    private final BigDecimal rate;

    PaymentScheme(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getInterestRate() {
        return rate;
    }
}
