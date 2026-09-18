package com.aplazo.bnpl.exception;

import java.util.UUID;

public class LoanNotFoundException extends RuntimeException {

    private final UUID loanId;

    public LoanNotFoundException(UUID loanId) {
        super("Loan not found with id: " + loanId);
        this.loanId = loanId;
    }

    public LoanNotFoundException(UUID loanId, String message) {
        super(message);
        this.loanId = loanId;
    }

    public LoanNotFoundException(String message) {
        super(message);
        this.loanId = null;
    }

    public UUID getLoanId() {
        return loanId;
    }
}
