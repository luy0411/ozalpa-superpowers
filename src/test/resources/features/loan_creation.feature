Feature: Loan Creation
  As an authenticated customer
  I want to create a loan against my available credit line
  So that I can finance purchases with installments and appropriate scheme interest

  Scenario: Create loan within available credit line
    Given a registered customer with first name "David", last name "Miller", second last name "Clark", and age 28
    When the customer requests a loan of amount "2000.00"
    Then the loan creation response status code should be 201
    And the response should include a "Location" header containing the loan id
    And the customer available credit line should be reduced by "2000.00"
    And the customer remaining available credit line should be "3000.00"

  Scenario: Reject loan when amount exceeds available credit line
    Given a registered customer with first name "Eva", last name "Taylor", second last name "Brown", and age 22
    When the customer requests a loan of amount "4000.00"
    Then the loan creation response status code should be 400
    And the response error code should be "APZ000006"
    And the response error name should be "INVALID_LOAN_REQUEST"

  Scenario Outline: Assign Scheme 1 when first name starts with C, L, or H
    Given a registered customer with first name "<firstName>", last name "Martinez", second last name "Diaz", and age 35
    When the customer requests a loan of amount "1000.00"
    Then the loan creation response status code should be 201
    And the loan payment plan commission amount should be "130.00"

    Examples:
      | firstName |
      | Carlos    |
      | Laura     |
      | Hector    |

  Scenario: Assign Scheme 2 when first name does not start with C, L, or H
    Given a registered customer with first name "Pedro", last name "Santos", second last name "Vargas", and age 35
    When the customer requests a loan of amount "1000.00"
    Then the loan creation response status code should be 201
    And the loan payment plan commission amount should be "160.00"

  Scenario: Verify 5 installments generated with D+14 biweekly schedule and proper status
    Given a registered customer with first name "Carlos", last name "Santana", second last name "Lopez", and age 30
    When the customer requests a loan of amount "1000.00"
    Then the loan creation response status code should be 201
    And the loan should contain exactly 5 installments
    And installment 1 should have status "NEXT"
    And installment 2 should have status "PENDING"
    And installment 3 should have status "PENDING"
    And installment 4 should have status "PENDING"
    And installment 5 should have status "PENDING"
    And installment 1 scheduled payment date should be 14 days from today
    And installment 2 scheduled payment date should be 28 days from today
    And installment 3 scheduled payment date should be 42 days from today
    And installment 4 scheduled payment date should be 56 days from today
    And installment 5 scheduled payment date should be 70 days from today
