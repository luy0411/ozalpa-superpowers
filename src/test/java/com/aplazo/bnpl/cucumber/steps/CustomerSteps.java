package com.aplazo.bnpl.cucumber.steps;

import com.aplazo.bnpl.cucumber.CucumberSpringConfiguration;
import com.aplazo.bnpl.cucumber.context.TestContext;
import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.repository.CustomerRepository;
import com.aplazo.bnpl.repository.InstallmentRepository;
import com.aplazo.bnpl.repository.LoanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomerSteps extends CucumberSpringConfiguration {

    @Autowired
    private TestContext testContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    private CustomerRequest pendingCustomerRequest;

    @Before
    public void setup() {
        testContext.reset();
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Given("a customer with first name {string}, last name {string}, second last name {string}, and age {int}")
    public void aCustomerWithFirstLastNameSecondLastNameAndAge(String firstName, String lastName, String secondLastName, int age) {
        LocalDate dateOfBirth = LocalDate.now().minusYears(age);
        pendingCustomerRequest = new CustomerRequest(firstName, lastName, secondLastName, dateOfBirth);
        testContext.setCurrentCustomerFirstName(firstName);
    }

    @When("the customer submits the registration request")
    public void theCustomerSubmitsTheRegistrationRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CustomerRequest> requestEntity = new HttpEntity<>(pendingCustomerRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/v1/customers",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        testContext.setLastResponse(response);

        if (response.getStatusCode().is2xxSuccessful()) {
            String token = response.getHeaders().getFirst("X-Auth-Token");
            testContext.setAuthToken(token);

            try {
                JsonNode json = objectMapper.readTree(response.getBody());
                if (json.has("id")) {
                    testContext.setCurrentCustomerId(UUID.fromString(json.get("id").asText()));
                }
                if (json.has("creditLineAmount")) {
                    testContext.setCurrentCreditLine(new BigDecimal(json.get("creditLineAmount").asText()));
                }
                if (json.has("availableCreditLineAmount")) {
                    testContext.setCurrentAvailableCredit(new BigDecimal(json.get("availableCreditLineAmount").asText()));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse customer response", e);
            }
        }
    }

    @Then("the registration response status code should be {int}")
    public void theRegistrationResponseStatusCodeShouldBe(int expectedStatus) {
        assertThat(testContext.getLastResponse().getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @And("the response body should contain credit line amount {string}")
    public void theResponseBodyShouldContainCreditLineAmount(String expectedCreditLine) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(json.has("creditLineAmount")).isTrue();
        assertThat(new BigDecimal(json.get("creditLineAmount").asText()))
                .isEqualByComparingTo(new BigDecimal(expectedCreditLine));
    }

    @And("the response body should contain available credit line amount {string}")
    public void theResponseBodyShouldContainAvailableCreditLineAmount(String expectedCreditLine) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(json.has("availableCreditLineAmount")).isTrue();
        assertThat(new BigDecimal(json.get("availableCreditLineAmount").asText()))
                .isEqualByComparingTo(new BigDecimal(expectedCreditLine));
    }

    @And("the response should include an {string} header")
    public void theResponseShouldIncludeAnHeader(String headerName) {
        assertThat(testContext.getLastResponse().getHeaders().containsKey(headerName)).isTrue();
        assertThat(testContext.getLastResponse().getHeaders().getFirst(headerName)).isNotBlank();
    }

    @And("the response should include a {string} header containing the customer id")
    public void theResponseShouldIncludeAHeaderContainingTheCustomerId(String headerName) {
        assertThat(testContext.getLastResponse().getHeaders().containsKey(headerName)).isTrue();
        String location = testContext.getLastResponse().getHeaders().getFirst(headerName);
        assertThat(location).contains(testContext.getCurrentCustomerId().toString());
    }

    @And("the response error code should be {string}")
    public void theResponseErrorCodeShouldBe(String expectedErrorCode) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(json.has("code")).isTrue();
        assertThat(json.get("code").asText()).isEqualTo(expectedErrorCode);
    }

    @And("the response error name should be {string}")
    public void theResponseErrorNameShouldBe(String expectedErrorName) throws Exception {
        JsonNode json = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(json.has("error")).isTrue();
        assertThat(json.get("error").asText()).isEqualTo(expectedErrorName);
    }
}
