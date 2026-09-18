# Technical Test

## General Objective
Develop an application and an example of an online shopping REST system that covers the main events of the credit creation and management process in a BNPL (Buy Now Pay Later) business.

## Introduction
BNPL (Buy Now, Pay Later) is a credit modality that allows the purchase of products through installment payments, with the first payment occurring at the time of purchase or later.

Given this, one of the main technological requirements of the business is the creation of processes that support the creation and management of credit.

## Business Concepts
- **Clients:** Registered individuals on the business platform who are granted a credit line.
- **Credit Line:** The amount of money assigned to a client for product purchases.
- **Loan:** Each purchase is converted into a loan, deducting the loan amount from the client's credit line.
- **Payment Scheme and Interest Rate:** Each loan has an assigned payment scheme and interest rate. Generally, the more installments in the scheme, the higher the interest rate.

## Online Shopping System

### Endpoint 01: Client Registration
Create a REST endpoint to register a new client. Once stored in the database, the response should include the assigned credit line amount and the client identifier.

#### Credit Line Assignment Rules
The assigned credit line amount is based on the client's age:
- **$3,000** for clients aged **18 to 25** years.
- **$5,000** for clients aged **26 to 30** years.
- **$8,000** for clients aged **31 to 65** years.
- **Clients under 18 or over 65 are not accepted.**

#### Input & Output
| Input             | Output                 |
|------------------|------------------------|
| `name`           | `idCliente`            |
| `birthDate (YYYY/MM/DD)` | `Assigned Credit Amount` |

### Endpoint 02: Purchase Registration
Create a REST endpoint to register a client's purchase using the client ID retrieved from the previous endpoint. The purchase amount **must not exceed the assigned credit line**; otherwise, an appropriate error should be returned before storing the transaction in the database.

#### Input & Output
| Input         | Output         |
|--------------|---------------|
| `idCliente`  | `{ idCompra }` |
| `montoCompra` |               |

### Purchase Rules & Information
The system should have the following payment schemes:

| Payment Scheme | Number of Payments | Frequency | Interest Rate |
|---------------|-------------------|-----------|--------------|
| **Scheme 1** | 5                 | Biweekly  | 13%          |
| **Scheme 2** | 5                 | Biweekly  | 16%          |

#### Payment Scheme Assignment Rules
- Assign **Scheme 1** if the first name of the client starts with **C, L, or H**.
- Assign **Scheme 2** if the **client ID is greater than 25**.
- Evaluate the rules in order, applying only the first applicable one.
- If no rule applies, **Scheme 2 is assigned by default**.

### Internal Calculations
The system must calculate the following based on the purchase amount and the assigned scheme:
- **Purchase date**
- **Commission amount based on the interest rate**
- **Total purchase amount**
- **Amount per installment according to the number of payments**
- **Payment due dates according to the assigned scheme**

### Credit Line & Purchase Controls
Appropriate controls should be in place to manage purchases and credit line utilization.

## Considerations & Tools
Please use the following tools to develop the project:
- **Java 17**
- **Spring Boot 3**
- **Any relational database**
- **Spring Data**
- **Maven/Gradle**

### Additional Requirements
- **Logging (mandatory)**
- **Application execution via Docker** (include a `Dockerfile` or `docker-compose.yml`) *(mandatory)*
- **Unit tests with at least 60% coverage** *(desirable: 80% coverage)*
- **Use of test containers** *(desirable)*


## Development Guidelines

- **OpenAPI Specification:** Development should consider the descriptions provided in the OpenAPI specification located in this directory.
- **Java Version:** Java 17 or higher is required.
- **Spring Boot Version:** Spring Boot 3 or higher is required.
- **Spring Data:** Spring Data must be used for data access.
- **Build Tool:** You can use either Maven or Gradle.
- **Database:** Any database can be used, but PostgreSQL is preferred.
- **Logging:** A logging system should be implemented.
- **Docker and Docker Compose:** Development should primarily use Docker and Docker Compose for both development and CI/CD environments.
- **Testing:** Unit and integration tests should be included, using tools like Testcontainers.
- **Authorization:** Implement an authentication and authorization mechanism to secure the API endpoints. This is required for the frontend login functionality.


> Development of the test must be done in a branch named `feature/[candidate's first name]_[candidate's last name]` (if there are conflicts, add the second last name), for example: `feature/juan_perez`. Instead of making a pull request to the master branch, the candidate must provide access to their repository where the take-home test is hosted using a token.
