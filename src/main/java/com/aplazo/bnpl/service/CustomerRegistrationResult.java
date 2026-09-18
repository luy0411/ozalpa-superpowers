package com.aplazo.bnpl.service;

import com.aplazo.bnpl.dto.response.CustomerResponse;

public record CustomerRegistrationResult(
        CustomerResponse customerResponse,
        String token
) {
    public CustomerResponse customer() {
        return customerResponse;
    }
}
