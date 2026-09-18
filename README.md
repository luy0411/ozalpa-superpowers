# Buy Now Pay Later (BNPL) REST API

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

A production-ready Buy Now Pay Later (BNPL) REST service developed for the **Aplazo Backend Challenge**. The platform automates customer onboarding, age-tiered credit line assignment, loan origination with biweekly installment schedules, credit limit tracking, and secure JWT-based authentication.

---

## Architecture & Tech Stack

- **Language & Runtime**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4.3 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`)
- **Security & Authentication**: Spring Security with Stateless JWT (`io.jsonwebtoken:jjwt-api:0.12.6`), issuance upon registration and Bearer authentication on protected endpoints
- **Database & Persistence**: PostgreSQL 16, Spring Data JPA / Hibernate with optimistic locking
- **Database Migrations**: Flyway 10 (`flyway-database-postgresql`)
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI (`springdoc-openapi-starter-webmvc-ui:2.8.5`)
- **Testing & Quality Assurance**:
  - JUnit 5 & Mockito for unit testing
  - Cucumber BDD (`cucumber-java:7.21.1`, `cucumber-spring`, `junit-platform-suite`) for end-to-end integration scenarios
  - Testcontainers (`testcontainers:1.20.4`, PostgreSQL 16 Alpine container) for realistic integration tests
  - JaCoCo (`jacoco-maven-plugin:0.8.12`) with automated verification rules (>80% minimum branch/instruction coverage enforced)

---

## Documentation Links

- **[System Design Specification](docs/superpowers/specs/2026-09-18-bnpl-system-design.md)**: Architectural decisions, domain models, business rules, credit line tiers, interest scheme definitions, and API specifications.
- **[Entity-Relationship Diagram & Data Dictionary](docs/erd.md)**: Complete database schema, Mermaid ERD, column types, constraints, and relational mappings.

---

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or later (`java -version`)
- **Docker & Docker Compose**: Docker Engine 24+ and Docker Compose v2+ (`docker compose version`)
- Maven Wrapper is included in the project (`./mvnw`).

---

## How to Build and Run Tests

Run the full verification suite including all unit tests, Testcontainers-backed integration tests, Cucumber BDD scenarios, and JaCoCo coverage enforcement:

```bash
rtk ./mvnw clean verify
```

- Executes all unit and Cucumber BDD tests (138+ steps and tests).
- Spins up an ephemeral PostgreSQL Testcontainer automatically.
- Enforces JaCoCo code coverage minimum thresholds.
- Generates the HTML coverage report at: `target/site/jacoco/index.html`.

---

## How to Run Locally with Maven

### 1. Start PostgreSQL

Start a local PostgreSQL 16 instance (or use the one provided via Docker Compose):

```bash
docker run --name bnpl-postgres \
  -e POSTGRES_DB=bnpldb \
  -e POSTGRES_USER=bnpluser \
  -e POSTGRES_PASSWORD=bnplpass \
  -p 5432:5432 -d postgres:16-alpine
```

### 2. Run the Application

```bash
rtk ./mvnw spring-boot:run
```

By default, the application connects to `jdbc:postgresql://localhost:5432/bnpldb` using credentials `bnpluser` / `bnplpass`. You can override configuration with environment variables:

```bash
DB_URL="jdbc:postgresql://localhost:5432/bnpldb" \
DB_USERNAME="bnpluser" \
DB_PASSWORD="bnplpass" \
rtk ./mvnw spring-boot:run
```

---

## How to Run with Docker Compose

To build and spin up both the PostgreSQL 16 database and the BNPL service container in one step:

```bash
docker compose up --build
```

To run in the background (detached mode):

```bash
docker compose up -d --build
```

To stop the services and teardown containers:

```bash
docker compose down -v
```

Services exposed:
- **BNPL REST API**: `http://localhost:8080`
- **PostgreSQL Database**: `localhost:5432` (`POSTGRES_DB=bnpl`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`)

---

## API Documentation

- **Swagger UI Interactive Console**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI v3 JSON Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Complete cURL Examples

### 1. Register Customer (`POST /v1/customers`)

Registers a customer, computes their credit line based on birth date (Age 18–25: $3,000.00; Age 26–30: $5,000.00; Age 31–65: $8,000.00), and returns the JWT in the `X-Auth-Token` response header.

```bash
curl -i -X POST http://localhost:8080/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Carlos",
    "lastName": "Wayne",
    "secondLastName": "Kane",
    "dateOfBirth": "1990-05-15"
  }'
```

**Example Response:**
```http
HTTP/1.1 201 Created
Location: /v1/customers/3fa85f64-5717-4562-b3fc-2c963f66afa7
X-Auth-Token: eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "creditLineAmount": 8000.00,
  "availableCreditLineAmount": 8000.00,
  "createdAt": "2026-09-18T12:00:00Z"
}
```

*Extract the token and customer ID from the response for the following requests:*

```bash
export TOKEN="<value-of-X-Auth-Token-header>"
export CUSTOMER_ID="3fa85f64-5717-4562-b3fc-2c963f66afa7"
```

---

### 2. Retrieve Customer by ID (`GET /v1/customers/{id}`)

Fetches customer profile details using the Bearer token.

```bash
curl -i -X GET http://localhost:8080/v1/customers/${CUSTOMER_ID} \
  -H "Authorization: Bearer ${TOKEN}"
```

**Example Response:**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "creditLineAmount": 8000.00,
  "availableCreditLineAmount": 8000.00,
  "createdAt": "2026-09-18T12:00:00Z"
}
```

---

### 3. Create Loan (`POST /v1/loans`)

Creates a new loan, validates credit line availability, computes interest commission based on the customer payment scheme (e.g. Scheme 1 with 13% commission for names starting with C, L, H), generates 5 biweekly installments, and reduces available credit.

```bash
curl -i -X POST http://localhost:8080/v1/loans \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "'"${CUSTOMER_ID}"'",
    "amount": 1000.00
  }'
```

**Example Response:**
```http
HTTP/1.1 201 Created
Location: /v1/loans/7fa85f64-5717-4562-b3fc-2c963f66afa8
Content-Type: application/json

{
  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa8",
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 1000.00,
  "status": "ACTIVE",
  "createdAt": "2026-09-18T12:05:00Z",
  "paymentPlan": {
    "commissionAmount": 130.00,
    "installments": [
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-10-02",
        "status": "NEXT"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-10-16",
        "status": "PENDING"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-10-30",
        "status": "PENDING"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-11-13",
        "status": "PENDING"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-11-27",
        "status": "PENDING"
      }
    ]
  }
}
```

*Extract the loan ID:*

```bash
export LOAN_ID="7fa85f64-5717-4562-b3fc-2c963f66afa8"
```

---

### 4. Retrieve Loan by ID (`GET /v1/loans/{id}`)

Fetches details of an existing loan and all installment schedules using the Bearer token.

```bash
curl -i -X GET http://localhost:8080/v1/loans/${LOAN_ID} \
  -H "Authorization: Bearer ${TOKEN}"
```

**Example Response:**
```json
{
  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa8",
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 1000.00,
  "status": "ACTIVE",
  "createdAt": "2026-09-18T12:05:00Z",
  "paymentPlan": {
    "commissionAmount": 130.00,
    "installments": [
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-10-02",
        "status": "NEXT"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-10-16",
        "status": "PENDING"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-10-30",
        "status": "PENDING"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-11-13",
        "status": "PENDING"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2026-11-27",
        "status": "PENDING"
      }
    ]
  }
}
```
