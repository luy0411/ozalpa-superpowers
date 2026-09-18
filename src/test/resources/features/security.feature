Feature: Security and Authentication
  As the BNPL service API
  I want to restrict access to sensitive endpoints
  So that only authenticated users with valid JWT tokens can access resources

  Scenario: Access loans without token returns HTTP 401 Unauthorized
    When an unauthenticated request is made to create a loan
    Then the security response status code should be 401
    And the response error code should be "APZ000007"
    And the response error name should be "UNAUTHORIZED"

  Scenario: Access loans with valid token succeeds
    Given a registered customer with first name "Alice", last name "Smith", second last name "Jones", and age 25
    When the customer requests a loan of amount "500.00" with a valid authentication token
    Then the loan creation response status code should be 201
