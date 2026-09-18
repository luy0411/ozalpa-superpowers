# BNPL System (Aplazo Backend Challenge) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade Buy Now Pay Later (BNPL) REST API service in Java 21 and Spring Boot 3.4 that complies with the Aplazo challenge requirements and OpenAPI 3.0.1 specification.

**Architecture:** Layered architecture (Controller-Service-Repository) with isolated domain calculators for credit limits and payment schemes, JWT authentication, PostgreSQL persistence with Flyway migrations, and Cucumber BDD integration tests with Testcontainers.

**Tech Stack:** Java 21, Spring Boot 3.4.3, Spring Data JPA, Spring Security, JJWT, Flyway, PostgreSQL 16, Testcontainers, Cucumber (JUnit 5 engine), Mockito, AssertJ, Jacoco (~50% target coverage), Docker & Docker Compose.

**Spec:** [docs/superpowers/specs/2026-09-18-bnpl-system-design.md](file:///home/lucamuco/dev/git/ozalpa-superpowers/docs/superpowers/specs/2026-09-18-bnpl-system-design.md)

## Global Constraints

- Prefix every shell command with `rtk`: `rtk ./mvnw test`, `rtk git status`, etc.
- Java version: 21 (LTS).
- Strictly adhere to API contracts and error payloads from [take-home.openapi.yml](file:///home/lucamuco/dev/git/ozalpa-superpowers/docs/challenge/take-home.openapi.yml).
- Unit test coverage target: ~50% focused on core success and primary failure paths.
- Integration tests using Cucumber with Gherkin `.feature` specifications against real PostgreSQL via Testcontainers.
- Branch / Git: work directly on `main` repository branch with frequent atomic commits.

---

### Task 1: Database Documentation & ERD

**Files:**
- Create: `docs/erd.md`

**Interfaces:**
- Produces: Visual Mermaid `erDiagram` and complete Data Dictionary for `customers`, `loans`, and `installments` tables.

- [ ] **Step 1: Create `docs/erd.md` with Mermaid diagram and Data Dictionary**

Write `docs/erd.md` with:
- Mermaid `erDiagram` showing `CUSTOMERS ||--o{ LOANS : has` and `LOANS ||--|{ INSTALLMENTS : contains`.
- Detailed table descriptions for `customers`, `loans`, `installments` with columns, data types, constraints and business descriptions.

- [ ] **Step 2: Commit documentation**

```bash
rtk git add docs/erd.md
rtk git commit -m "docs: add entity-relationship diagram and data dictionary"
```

---

### Task 2: Project Scaffolding & Maven Configuration

**Files:**
- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `.gitignore`
- Create: `src/main/resources/application.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `src/main/java/com/aplazo/bnpl/BnplApplication.java`

**Interfaces:**
- Produces: Executable Spring Boot 3.4 / Java 21 foundation with dependencies for Web, Data JPA, Security, Flyway, Postgres, JJWT, Validation, Testcontainers, Cucumber, Jacoco.

- [ ] **Step 1: Create `pom.xml`**
Configure Spring Boot `3.4.3`, Java `21`, dependencies:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `org.postgresql:postgresql`
- `org.flywaydb:flyway-core`
- `org.flywaydb:flyway-database-postgresql`
- `io.jsonwebtoken:jjwt-api:0.12.6` (runtime `jjwt-impl`, `jjwt-jackson`)
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5`
- `org.springframework.boot:spring-boot-starter-test`
- `org.testcontainers:postgresql:1.20.4`
- `org.testcontainers:junit-jupiter:1.20.4`
- `io.cucumber:cucumber-java:7.21.1`
- `io.cucumber:cucumber-spring:7.21.1`
- `io.cucumber:cucumber-junit-platform-engine:7.21.1`
- `org.junit.platform:junit-platform-suite:1.11.4`
- Jacoco plugin with 50% coverage rule.

- [ ] **Step 2: Set up Maven Wrapper and `.gitignore`**
Include standard Java/Maven ignore rules (`target/`, `.idea/`, `*.iml`, etc.) and ensure `mvnw` is executable (`chmod +x mvnw`).

- [ ] **Step 3: Create `application.yml` and `application-test.yml`**
Configure datasource, JPA hibernate ddl-auto `validate`, flyway enabled, JWT secret & expiration properties.

- [ ] **Step 4: Create `BnplApplication.java`**
Spring Boot main class with `@SpringBootApplication`.

- [ ] **Step 5: Verify build compiles**
Run: `rtk ./mvnw compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**
```bash
rtk git add pom.xml mvnw mvnw.cmd .mvn .gitignore src/
rtk git commit -m "chore: scaffold Spring Boot 3.4 project with Java 21 and Maven dependencies"
```

---

### Task 3: Database Migrations (Flyway)

**Files:**
- Create: `src/main/resources/db/migration/V1__initial_schema.sql`

**Interfaces:**
- Produces: Database tables `customers`, `loans`, and `installments` with proper foreign keys, constraints, and indexes.

- [ ] **Step 1: Write `V1__initial_schema.sql`**
Create DDL:
- `customers`: `id BIGSERIAL PRIMARY KEY`, `external_id UUID NOT NULL UNIQUE`, `first_name VARCHAR(100) NOT NULL`, `last_name VARCHAR(100) NOT NULL`, `second_last_name VARCHAR(100) NOT NULL`, `date_of_birth DATE NOT NULL`, `credit_line_amount NUMERIC(15, 2) NOT NULL`, `available_credit_line_amount NUMERIC(15, 2) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- `loans`: `id BIGSERIAL PRIMARY KEY`, `external_id UUID NOT NULL UNIQUE`, `customer_id BIGINT NOT NULL REFERENCES customers(id)`, `amount NUMERIC(15, 2) NOT NULL`, `commission_amount NUMERIC(15, 2) NOT NULL`, `total_amount NUMERIC(15, 2) NOT NULL`, `scheme_name VARCHAR(20) NOT NULL`, `interest_rate NUMERIC(5, 4) NOT NULL`, `status VARCHAR(20) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- `installments`: `id BIGSERIAL PRIMARY KEY`, `loan_id BIGINT NOT NULL REFERENCES loans(id) ON DELETE CASCADE`, `installment_number INT NOT NULL`, `amount NUMERIC(15, 2) NOT NULL`, `scheduled_payment_date DATE NOT NULL`, `status VARCHAR(20) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- Indexes on `customers(external_id)`, `loans(external_id)`, `loans(customer_id)`, `installments(loan_id)`.

- [ ] **Step 2: Commit**
```bash
rtk git add src/main/resources/db/migration/V1__initial_schema.sql
rtk git commit -m "feat(db): add initial Flyway schema migration for customers, loans, and installments"
```

---

### Task 4: Domain Entities and Repositories

**Files:**
- Create: `src/main/java/com/aplazo/bnpl/entity/CustomerEntity.java`
- Create: `src/main/java/com/aplazo/bnpl/entity/LoanEntity.java`
- Create: `src/main/java/com/aplazo/bnpl/entity/InstallmentEntity.java`
- Create: `src/main/java/com/aplazo/bnpl/entity/LoanStatus.java`
- Create: `src/main/java/com/aplazo/bnpl/entity/InstallmentStatus.java`
- Create: `src/main/java/com/aplazo/bnpl/entity/PaymentScheme.java`
- Create: `src/main/java/com/aplazo/bnpl/repository/CustomerRepository.java`
- Create: `src/main/java/com/aplazo/bnpl/repository/LoanRepository.java`
- Create: `src/main/java/com/aplazo/bnpl/repository/InstallmentRepository.java`

**Interfaces:**
- Produces: JPA entities matching database schema and Spring Data JPA repositories with query methods:
  - `CustomerRepository.findByExternalId(UUID externalId)`
  - `LoanRepository.findByExternalId(UUID externalId)`

- [ ] **Step 1: Create enums and JPA entities**
Define `LoanStatus` (`ACTIVE`, `LATE`, `COMPLETED`), `InstallmentStatus` (`NEXT`, `PENDING`, `ERROR`), `PaymentScheme` (`SCHEME_1`, `SCHEME_2`).
Define `CustomerEntity`, `LoanEntity`, `InstallmentEntity` with mapping annotations (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`).

- [ ] **Step 2: Create repository interfaces**
Define `CustomerRepository`, `LoanRepository`, `InstallmentRepository` extending `JpaRepository`.

- [ ] **Step 3: Verify compilation**
Run: `rtk ./mvnw compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**
```bash
rtk git add src/main/java/com/aplazo/bnpl/entity/ src/main/java/com/aplazo/bnpl/repository/
rtk git commit -m "feat(domain): add JPA entities and Spring Data repositories"
```

---

### Task 5: Domain Services: CreditLineCalculator and PaymentSchemeResolver

**Files:**
- Create: `src/main/java/com/aplazo/bnpl/service/CreditLineCalculator.java`
- Create: `src/test/java/com/aplazo/bnpl/service/CreditLineCalculatorTest.java`
- Create: `src/main/java/com/aplazo/bnpl/service/PaymentSchemeResolver.java`
- Create: `src/main/java/com/aplazo/bnpl/service/model/PaymentPlanCalculation.java`
- Create: `src/main/java/com/aplazo/bnpl/service/model/InstallmentCalculation.java`
- Create: `src/test/java/com/aplazo/bnpl/service/PaymentSchemeResolverTest.java`

**Interfaces:**
- Produces:
  - `CreditLineCalculator.calculateCreditLine(LocalDate birthDate, LocalDate referenceDate): BigDecimal`
  - `PaymentSchemeResolver.resolveScheme(String firstName, Long internalCustomerId): PaymentScheme`
  - `PaymentSchemeResolver.calculatePlan(BigDecimal amount, PaymentScheme scheme, LocalDate purchaseDate): PaymentPlanCalculation`

- [ ] **Step 1: Write failing tests for `CreditLineCalculator`**
Test age brackets:
- 20 years old -> $3,000.00
- 28 years old -> $5,000.00
- 45 years old -> $8,000.00
- 17 years old -> throws `InvalidCustomerRequestException`
- 66 years old -> throws `InvalidCustomerRequestException`

- [ ] **Step 2: Run test to verify failure**
Run: `rtk ./mvnw test -Dtest=CreditLineCalculatorTest`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement `CreditLineCalculator`**
Implement age calculation via `Period.between` and bracket mapping ($3000, $5000, $8000). Throw `InvalidCustomerRequestException` if age < 18 or age > 65.

- [ ] **Step 4: Run test to verify pass**
Run: `rtk ./mvnw test -Dtest=CreditLineCalculatorTest`
Expected: PASS

- [ ] **Step 5: Write failing tests for `PaymentSchemeResolver`**
Test scheme rules:
- Name starting with 'C', 'L', 'H' -> `SCHEME_1` (13% interest)
- Name starting with 'J', but internalId = 26 (> 25) -> `SCHEME_2` (16% interest)
- Name starting with 'J', internalId = 10 -> `SCHEME_2` (fallback)
- Plan calculation: 5 installments, D+14 biweekly schedule, commission amount calculation.

- [ ] **Step 6: Implement `PaymentSchemeResolver`**
Implement scheme resolution and calculation of 5 installments, biweekly dates, commission amount, and penny-rounding adjustments on the 5th installment.

- [ ] **Step 7: Run tests to verify pass**
Run: `rtk ./mvnw test -Dtest=PaymentSchemeResolverTest`
Expected: PASS

- [ ] **Step 8: Commit**
```bash
rtk git add src/main/java/com/aplazo/bnpl/service/ src/test/java/com/aplazo/bnpl/service/
rtk git commit -m "feat(service): add CreditLineCalculator and PaymentSchemeResolver with unit tests"
```

---

### Task 6: DTOs, Mappers and Global Exception Handler (OpenAPI Spec Compliance)

**Files:**
- Create: `src/main/java/com/aplazo/bnpl/dto/request/CustomerRequest.java`
- Create: `src/main/java/com/aplazo/bnpl/dto/request/LoanRequest.java`
- Create: `src/main/java/com/aplazo/bnpl/dto/response/CustomerResponse.java`
- Create: `src/main/java/com/aplazo/bnpl/dto/response/LoanResponse.java`
- Create: `src/main/java/com/aplazo/bnpl/dto/response/PaymentPlanResponse.java`
- Create: `src/main/java/com/aplazo/bnpl/dto/response/InstallmentResponse.java`
- Create: `src/main/java/com/aplazo/bnpl/dto/response/ErrorResponse.java`
- Create: `src/main/java/com/aplazo/bnpl/exception/CustomerNotFoundException.java`
- Create: `src/main/java/com/aplazo/bnpl/exception/LoanNotFoundException.java`
- Create: `src/main/java/com/aplazo/bnpl/exception/InvalidCustomerRequestException.java`
- Create: `src/main/java/com/aplazo/bnpl/exception/InvalidLoanRequestException.java`
- Create: `src/main/java/com/aplazo/bnpl/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: API JSON contracts matching `take-home.openapi.yml` and unified error responses with `APZxxxxxx` codes and Unix timestamps.

- [ ] **Step 1: Define DTO records**
Create Java records matching OpenAPI schemas with Jakarta validation annotations (`@NotBlank`, `@NotNull`, `@Positive`).

- [ ] **Step 2: Define business exceptions**
Create exceptions with custom error messages.

- [ ] **Step 3: Implement `GlobalExceptionHandler`**
Handle:
- `InvalidCustomerRequestException` -> `APZ000002` (`INVALID_CUSTOMER_REQUEST`, 400)
- `MethodArgumentNotValidException` / `HttpMessageNotReadableException` -> `APZ000002` or `APZ000004` (400)
- `CustomerNotFoundException` -> `APZ000005` (`CUSTOMER_NOT_FOUND`, 404)
- `InvalidLoanRequestException` -> `APZ000006` (`INVALID_LOAN_REQUEST`, 400)
- `LoanNotFoundException` -> `APZ000008` (`LOAN_NOT_FOUND`, 404)
- General `Exception` -> `APZ000001` (`INTERNAL_SERVER_ERROR`, 500)

- [ ] **Step 4: Verify compilation**
Run: `rtk ./mvnw compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**
```bash
rtk git add src/main/java/com/aplazo/bnpl/dto/ src/main/java/com/aplazo/bnpl/exception/
rtk git commit -m "feat(api): add OpenAPI DTOs and GlobalExceptionHandler"
```

---

### Task 7: Security & JWT Infrastructure

**Files:**
- Create: `src/main/java/com/aplazo/bnpl/security/JwtTokenProvider.java`
- Create: `src/main/java/com/aplazo/bnpl/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/aplazo/bnpl/security/SecurityConfig.java`
- Create: `src/main/java/com/aplazo/bnpl/security/CustomAuthenticationEntryPoint.java`
- Create: `src/test/java/com/aplazo/bnpl/security/JwtTokenProviderTest.java`

**Interfaces:**
- Produces:
  - `JwtTokenProvider.generateToken(UUID customerId): String`
  - `JwtTokenProvider.validateToken(String token): boolean`
  - `JwtTokenProvider.getCustomerIdFromToken(String token): UUID`
  - Stateless `SecurityFilterChain` permitting `POST /v1/customers`, Swagger UI, Actuator, and securing all other routes.
  - Returns `APZ000007` (`UNAUTHORIZED`, 401) on missing/invalid token.

- [ ] **Step 1: Write test for `JwtTokenProvider`**
Test token generation, extraction of customer UUID subject, and validation.

- [ ] **Step 2: Implement `JwtTokenProvider`**
Use `io.jsonwebtoken` HMAC-SHA256 with configurable secret key and expiration.

- [ ] **Step 3: Implement `JwtAuthenticationFilter` and `CustomAuthenticationEntryPoint`**
Extract Bearer token from `Authorization` header or `X-Auth-Token` header. Handle 401 errors using `CustomAuthenticationEntryPoint` returning `APZ000007` format.

- [ ] **Step 4: Implement `SecurityConfig`**
Configure CSRF disabled, SessionCreationPolicy `STATELESS`, `authorizeHttpRequests`.

- [ ] **Step 5: Run tests**
Run: `rtk ./mvnw test -Dtest=JwtTokenProviderTest`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
rtk git add src/main/java/com/aplazo/bnpl/security/ src/test/java/com/aplazo/bnpl/security/
rtk git commit -m "feat(security): implement JWT provider, filter, and Spring Security configuration"
```

---

### Task 8: Customer Service & REST Controller

**Files:**
- Create: `src/main/java/com/aplazo/bnpl/service/CustomerService.java`
- Create: `src/test/java/com/aplazo/bnpl/service/CustomerServiceTest.java`
- Create: `src/main/java/com/aplazo/bnpl/controller/CustomerController.java`

**Interfaces:**
- Produces:
  - `CustomerService.createCustomer(CustomerRequest request): CustomerRegistrationResult`
  - `CustomerService.getCustomerByExternalId(UUID customerId): CustomerResponse`
  - `POST /v1/customers`: returns `201 Created` with `Location` and `X-Auth-Token` headers.
  - `GET /v1/customers/{customerId}`: returns `200 OK` or `404 Not Found`.

- [ ] **Step 1: Write unit test `CustomerServiceTest`**
Test customer creation and retrieval with repository and calculator mocks.

- [ ] **Step 2: Implement `CustomerService`**
Calculate credit limit using `CreditLineCalculator`, persist `CustomerEntity` with generated UUID, generate JWT via `JwtTokenProvider`.

- [ ] **Step 3: Implement `CustomerController`**
Map endpoints `/v1/customers` and `/v1/customers/{customerId}` per OpenAPI spec.

- [ ] **Step 4: Run unit test**
Run: `rtk ./mvnw test -Dtest=CustomerServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
rtk git add src/main/java/com/aplazo/bnpl/service/CustomerService.java src/main/java/com/aplazo/bnpl/controller/CustomerController.java src/test/java/com/aplazo/bnpl/service/CustomerServiceTest.java
rtk git commit -m "feat(customer): implement CustomerService and CustomerController"
```

---

### Task 9: Loan Service & REST Controller

**Files:**
- Create: `src/main/java/com/aplazo/bnpl/service/LoanService.java`
- Create: `src/test/java/com/aplazo/bnpl/service/LoanServiceTest.java`
- Create: `src/main/java/com/aplazo/bnpl/controller/LoanController.java`

**Interfaces:**
- Produces:
  - `LoanService.createLoan(LoanRequest request): LoanResponse`
  - `LoanService.getLoanByExternalId(UUID loanId): LoanResponse`
  - `POST /v1/loans`: validates available credit, creates loan + 5 installments, updates available credit, returns `201 Created` with `Location` header.
  - `GET /v1/loans/{loanId}`: returns `200 OK` or `404 Not Found`.

- [ ] **Step 1: Write unit test `LoanServiceTest`**
Test loan creation within limit, rejection when loan amount > available credit, and calculation delegation to `PaymentSchemeResolver`.

- [ ] **Step 2: Implement `LoanService`**
Validate customer existence, check credit line, deduct credit atomically (`@Transactional`), resolve scheme, generate installments, persist entities, and return DTO.

- [ ] **Step 3: Implement `LoanController`**
Map endpoints `/v1/loans` and `/v1/loans/{loanId}` with OpenAPI documentation annotations.

- [ ] **Step 4: Run unit test**
Run: `rtk ./mvnw test -Dtest=LoanServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
rtk git add src/main/java/com/aplazo/bnpl/service/LoanService.java src/main/java/com/aplazo/bnpl/controller/LoanController.java src/test/java/com/aplazo/bnpl/service/LoanServiceTest.java
rtk git commit -m "feat(loan): implement LoanService and LoanController"
```

---

### Task 10: Cucumber BDD Integration Test Suite

**Files:**
- Create: `src/test/java/com/aplazo/bnpl/cucumber/CucumberTestRunner.java`
- Create: `src/test/java/com/aplazo/bnpl/cucumber/CucumberSpringConfiguration.java`
- Create: `src/test/java/com/aplazo/bnpl/cucumber/steps/CustomerSteps.java`
- Create: `src/test/java/com/aplazo/bnpl/cucumber/steps/LoanSteps.java`
- Create: `src/test/java/com/aplazo/bnpl/cucumber/steps/SecuritySteps.java`
- Create: `src/test/resources/features/customer_registration.feature`
- Create: `src/test/resources/features/loan_creation.feature`
- Create: `src/test/resources/features/security.feature`

**Interfaces:**
- Produces: End-to-end BDD tests executed against a real PostgreSQL container managed by Testcontainers.

- [ ] **Step 1: Create Cucumber Configuration and Test Runner**
Configure `@Suite`, `@IncludeEngines("cucumber")`, `@SelectClasspathResource("features")`, and `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` with Testcontainers PostgreSQL container.

- [ ] **Step 2: Create `customer_registration.feature` and Step Definitions**
Scenarios:
- Successful customer registration for age 22 (assigned credit $3,000.00).
- Successful customer registration for age 28 (assigned credit $5,000.00).
- Successful customer registration for age 40 (assigned credit $8,000.00).
- Registration rejected for customer age 16 (`APZ000002`).
- Verification of `X-Auth-Token` header.

- [ ] **Step 3: Create `loan_creation.feature` and Step Definitions**
Scenarios:
- Create loan within available credit line (credit deducted correctly).
- Reject loan when amount exceeds available credit line (`APZ000006`).
- Assign Scheme 1 for customer with first name "Carlos" (13% interest rate).
- Verify 5 installments generated with D+14, D+28, D+42, D+56, D+70 dates.

- [ ] **Step 4: Create `security.feature` and Step Definitions**
Scenarios:
- Access `/v1/loans` without token returns HTTP 401 (`APZ000007`).
- Access `/v1/loans` with valid token succeeds.

- [ ] **Step 5: Run integration tests**
Run: `rtk ./mvnw test`
Expected: All Cucumber scenarios PASS and unit tests PASS.

- [ ] **Step 6: Commit**
```bash
rtk git add src/test/java/com/aplazo/bnpl/cucumber/ src/test/resources/features/
rtk git commit -m "test(bdd): add Cucumber integration test suite with Testcontainers"
```

---

### Task 11: Containerization, Documentation & Final Verification

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `README.md`

**Interfaces:**
- Produces: Fully containerized application ready for `docker compose up --build` and complete project documentation.

- [ ] **Step 1: Create `Dockerfile`**
Multi-stage build with `eclipse-temurin:21-jdk-alpine` builder and `eclipse-temurin:21-jre-alpine` runtime.

- [ ] **Step 2: Create `docker-compose.yml`**
Define `postgres` service (PostgreSQL 16 with healthcheck) and `app` service with environment variables.

- [ ] **Step 3: Create root `README.md`**
Detailed documentation:
- Architecture overview and technologies used.
- Prerequisites (Docker, Java 21).
- How to run locally with Maven.
- How to run with Docker Compose.
- How to run Cucumber and unit tests with coverage report.
- Swagger UI / OpenAPI documentation link (`http://localhost:8080/swagger-ui/index.html`).
- cURL examples for all endpoints.

- [ ] **Step 4: Run full verification build and check Jacoco coverage**
Run: `rtk ./mvnw clean verify`
Expected: `BUILD SUCCESS`, Jacoco coverage passes target.

- [ ] **Step 5: Commit**
```bash
rtk git add Dockerfile docker-compose.yml README.md
rtk git commit -m "chore: add Dockerfile, docker-compose, and project README"
```
