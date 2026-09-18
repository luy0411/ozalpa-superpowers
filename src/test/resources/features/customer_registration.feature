Feature: Customer Registration
  As a prospective customer
  I want to register for a BNPL account
  So that I can receive an assigned credit line based on my age and authenticate with the service

  Scenario Outline: Successful customer registration for valid age brackets
    Given a customer with first name "<firstName>", last name "<lastName>", second last name "<secondLastName>", and age <age>
    When the customer submits the registration request
    Then the registration response status code should be 201
    And the response body should contain credit line amount "<expectedCreditLine>"
    And the response body should contain available credit line amount "<expectedCreditLine>"
    And the response should include an "X-Auth-Token" header
    And the response should include a "Location" header containing the customer id

    Examples:
      | firstName | lastName | secondLastName | age | expectedCreditLine |
      | Juan      | Perez    | Gomez          | 22  | 3000.00            |
      | Maria     | Lopez    | Hernandez      | 28  | 5000.00            |
      | Roberto   | Sanchez  | Ramirez        | 40  | 8000.00            |

  Scenario Outline: Rejected registration for customer with invalid age
    Given a customer with first name "Alex", last name "Doe", second last name "Smith", and age <age>
    When the customer submits the registration request
    Then the registration response status code should be 400
    And the response error code should be "APZ000002"
    And the response error name should be "INVALID_CUSTOMER_REQUEST"

    Examples:
      | age |
      | 16  |
      | 70  |
