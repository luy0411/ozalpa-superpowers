package com.aplazo.bnpl.cucumber.context;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TestContext {

    private String authToken;
    private UUID currentCustomerId;
    private String currentCustomerFirstName;
    private BigDecimal currentCreditLine;
    private BigDecimal currentAvailableCredit;
    private ResponseEntity<String> lastResponse;
    private UUID currentLoanId;

    public void reset() {
        this.authToken = null;
        this.currentCustomerId = null;
        this.currentCustomerFirstName = null;
        this.currentCreditLine = null;
        this.currentAvailableCredit = null;
        this.lastResponse = null;
        this.currentLoanId = null;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public UUID getCurrentCustomerId() {
        return currentCustomerId;
    }

    public void setCurrentCustomerId(UUID currentCustomerId) {
        this.currentCustomerId = currentCustomerId;
    }

    public String getCurrentCustomerFirstName() {
        return currentCustomerFirstName;
    }

    public void setCurrentCustomerFirstName(String currentCustomerFirstName) {
        this.currentCustomerFirstName = currentCustomerFirstName;
    }

    public BigDecimal getCurrentCreditLine() {
        return currentCreditLine;
    }

    public void setCurrentCreditLine(BigDecimal currentCreditLine) {
        this.currentCreditLine = currentCreditLine;
    }

    public BigDecimal getCurrentAvailableCredit() {
        return currentAvailableCredit;
    }

    public void setCurrentAvailableCredit(BigDecimal currentAvailableCredit) {
        this.currentAvailableCredit = currentAvailableCredit;
    }

    public ResponseEntity<String> getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ResponseEntity<String> lastResponse) {
        this.lastResponse = lastResponse;
    }

    public UUID getCurrentLoanId() {
        return currentLoanId;
    }

    public void setCurrentLoanId(UUID currentLoanId) {
        this.currentLoanId = currentLoanId;
    }
}
