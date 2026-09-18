package com.aplazo.bnpl.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    private final UUID customerId;

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found with id: " + customerId);
        this.customerId = customerId;
    }

    public CustomerNotFoundException(UUID customerId, String message) {
        super(message);
        this.customerId = customerId;
    }

    public CustomerNotFoundException(String message) {
        super(message);
        this.customerId = null;
    }

    public UUID getCustomerId() {
        return customerId;
    }
}
