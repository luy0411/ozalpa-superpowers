package com.aplazo.bnpl.cucumber.steps;

import com.aplazo.bnpl.cucumber.CucumberSpringConfiguration;
import com.aplazo.bnpl.cucumber.context.TestContext;
import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.request.LoanRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanSteps extends CucumberSpringConfiguration {

    @Autowired
    private TestContext testContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Given("a registered customer with first name {string}, last name {string}, second last name {string}, and age {int}")
    public void aRegisteredCustomerWithFirstLastNameSecondLastNameAndAge(String firstName, String lastName, String secondLastName, int age) throws Exception {
        LocalDate dateOfBirth = LocalDate.now().minusYears(age);
        CustomerRequest customerRequest = new CustomerRequest(firstName, lastName, secondLastName, dateOfBirth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CustomerRequest> requestEntity = new HttpEntity<>(customerRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/v1/customers",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String token = response.getHeaders().getFirst("X-Auth-Token");
        testContext.setAuthToken(token);

        JsonNode json = objectMapper.readTree(response.getBody());
        UUID customerId = UUID.fromString(json.get("id").asText());
        BigDecimal creditLine = new BigDecimal(json.get("creditLineAmount").asText());
        BigDecimal availableCredit = new BigDecimal(json.get("availableCreditLineAmount").asText());

        testContext.setCurrentCustomerId(customerId);
        testContext.setCurrentCustomerFirstName(firstName);
        testContext.setCurrentCreditLine(creditLine);
        testContext.setCurrentAvailableCredit(availableCredit);
    }

    @When("the customer requests a loan of amount {string}")
    public void theCustomerRequestsALoanOfAmount(String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        LoanRequest loanRequest = new LoanRequest(testContext.getCurrentCustomerId(), amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (testContext.getAuthToken() != null) {
            headers.setBearerAuth(testContext.getAuthToken());
        }

        HttpEntity<LoanRequest> requestEntity = new HttpEntity<>(loanRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/v1/loans",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        testContext.setLastResponse(response);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode json = objectMapper.readTree(response.getBody());
                if (json.has("id")) {
                    testContext.setCurrentLoanId(UUID.fromString(json.get("id").asText()));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse loan response", e);
            }
        }
    }

    @When("the customer requests a loan of amount {string} with a valid authentication token")
    public void theCustomerRequestsALoanOfAmountWithAValidAuthenticationToken(String amountStr) {
        theCustomerRequestsALoanOfAmount(amountStr);
    }

    @Then("the loan creation response status code should be {int}")
    public void theLoanCreationResponseStatusCodeShouldBe(int expectedStatus) {
        assertThat(testContext.getLastResponse().getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @And("the response should include a {string} header containing the loan id")
    public void theResponseShouldIncludeAHeaderContainingTheLoanId(String headerName) {
        assertThat(testContext.getLastResponse().getHeaders().containsKey(headerName)).isTrue();
        String location = testContext.getLastResponse().getHeaders().getFirst(headerName);
        assertThat(location).contains(testContext.getCurrentLoanId().toString());
    }

    @And("the customer available credit line should be reduced by {string}")
    public void theCustomerAvailableCreditLineShouldBeReducedBy(String deductedAmountStr) throws Exception {
        BigDecimal deductedAmount = new BigDecimal(deductedAmountStr);

        // Fetch updated customer from /v1/customers/{customerId}
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(testContext.getAuthToken());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> customerResponse = restTemplate.exchange(
                getBaseUrl() + "/v1/customers/" + testContext.getCurrentCustomerId(),
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertThat(customerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode json = objectMapper.readTree(customerResponse.getBody());
        BigDecimal currentAvailable = new BigDecimal(json.get("availableCreditLineAmount").asText());

        BigDecimal initialCredit = testContext.getCurrentCreditLine();
        assertThat(currentAvailable).isEqualByComparingTo(initialCredit.subtract(deductedAmount));
    }

    @And("the customer remaining available credit line should be {string}")
    public void theCustomerRemainingAvailableCreditLineShouldBe(String expectedRemainingStr) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(testContext.getAuthToken());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> customerResponse = restTemplate.exchange(
                getBaseUrl() + "/v1/customers/" + testContext.getCurrentCustomerId(),
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertThat(customerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode json = objectMapper.readTree(customerResponse.getBody());
        BigDecimal currentAvailable = new BigDecimal(json.get("availableCreditLineAmount").asText());

        assertThat(currentAvailable).isEqualByComparingTo(new BigDecimal(expectedRemainingStr));
    }

    @And("the loan payment plan commission amount should be {string}")
    public void theLoanPaymentPlanCommissionAmountShouldBe(String expectedCommission) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(json.has("paymentPlan")).isTrue();
        JsonNode paymentPlan = json.get("paymentPlan");
        assertThat(paymentPlan.has("commissionAmount")).isTrue();
        assertThat(new BigDecimal(paymentPlan.get("commissionAmount").asText()))
                .isEqualByComparingTo(new BigDecimal(expectedCommission));
    }

    @And("the loan should contain exactly {int} installments")
    public void theLoanShouldContainExactlyInstallments(int count) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        JsonNode installments = json.get("paymentPlan").get("installments");
        assertThat(installments.isArray()).isTrue();
        assertThat(installments.size()).isEqualTo(count);
    }

    @And("installment {int} should have status {string}")
    public void installmentShouldHaveStatus(int installmentNumber, String expectedStatus) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        JsonNode installments = json.get("paymentPlan").get("installments");
        JsonNode installment = installments.get(installmentNumber - 1);
        assertThat(installment.get("status").asText()).isEqualTo(expectedStatus);
    }

    @And("installment {int} scheduled payment date should be {int} days from today")
    public void installmentScheduledPaymentDateShouldBeDaysFromToday(int installmentNumber, int days) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        JsonNode installments = json.get("paymentPlan").get("installments");
        JsonNode installment = installments.get(installmentNumber - 1);
        String dateStr = installment.get("scheduledPaymentDate").asText();

        LocalDate expectedDate = LocalDate.now().plusDays(days);
        assertThat(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)).isEqualTo(expectedDate);
    }
}
