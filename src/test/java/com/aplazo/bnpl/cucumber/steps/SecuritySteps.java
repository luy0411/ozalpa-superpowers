package com.aplazo.bnpl.cucumber.steps;

import com.aplazo.bnpl.cucumber.CucumberSpringConfiguration;
import com.aplazo.bnpl.cucumber.context.TestContext;
import com.aplazo.bnpl.dto.request.LoanRequest;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SecuritySteps extends CucumberSpringConfiguration {

    @Autowired
    private TestContext testContext;

    @When("an unauthenticated request is made to create a loan")
    public void anUnauthenticatedRequestIsMadeToCreateALoan() {
        LoanRequest loanRequest = new LoanRequest(UUID.randomUUID(), new BigDecimal("1000.00"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Do NOT set authorization header

        HttpEntity<LoanRequest> requestEntity = new HttpEntity<>(loanRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/v1/loans",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        testContext.setLastResponse(response);
    }

    @Then("the security response status code should be {int}")
    public void theSecurityResponseStatusCodeShouldBe(int expectedStatus) {
        assertThat(testContext.getLastResponse().getStatusCode().value()).isEqualTo(expectedStatus);
    }
}
