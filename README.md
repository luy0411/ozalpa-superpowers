# Buy Now Pay Later (BNPL) REST API — Superpowers Validation Project

[![Superpowers](https://img.shields.io/badge/Developed%20with-Superpowers-8A2BE2.svg)](https://github.com/features)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Cucumber BDD](https://img.shields.io/badge/BDD-Cucumber-23D96C.svg)](https://cucumber.io/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Este repositório foi construído como um **projeto de validação do Superpowers e de suas capacidades de desenvolvimento autônomo e assistido por IA**. O objetivo é demonstrar na prática o rigor metodológico, a disciplina arquitetural e a execução ponta a ponta orientada por agentes especializados.

---

## 🎯 Sobre a Validação do Superpowers

O desenvolvimento desta solução foi conduzido integralmente utilizando as ferramentas e convenções do **Superpowers**:

1. **Brainstorming & Alinhamento de Intenção (`superpowers:brainstorming`)**:
   - Classificação do trabalho (Spike exploratório $\rightarrow$ Projeto Arquitetural $\rightarrow$ Bounded).
   - Extração e análise automatizada dos requisitos e do contrato OpenAPI do desafio.
   - Refinamento iterativo de requisitos de arquitetura, banco, segurança e testes com o desenvolvedor.
2. **Especificação de Design Técnica (`docs/superpowers/specs/`)**:
   - Elaboração da spec formal contendo modelagem relacional, fórmulas financeiras, políticas de crédito e regras de transação.
3. **Plano de Implementação com Tarefas Granulares (`superpowers:writing-plans`)**:
   - Decomposição em 11 tarefas modulares e independentes com gates de verificação técnica.
4. **Desenvolvimento Dirigido por Subagentes (`superpowers:subagent-driven-development`)**:
   - Disparo de subagentes isolados para implementação (`implementer`) e para revisão estrita de spec e qualidade de código (`reviewer`), sem contaminação de contexto.
   - Ciclos de **TDD** (Red-Green-Refactor) e **BDD** (cenários Gherkin vivos com Cucumber).
5. **Comandos Otimizados com RTK (Rust Token Killer)**:
   - Uso sistemático do prefixo `rtk` em chamadas de terminal, economizando tokens e preservando sinais críticos de compilação e teste.
6. **Finalização e Integração (`superpowers:finishing-a-development-branch`)**:
   - Verificação total da suíte (138 testes com 0 falhas) antes de qualquer integração ou push.

---

## 🏦 Visão Macro do Desafio BNPL (Aplazo Challenge)

O desafio consiste no desenvolvimento de um sistema de crédito sob demanda no modelo **Buy Now, Pay Later (BNPL)**, cobrindo o ciclo de vida completo de originação e acompanhamento de empréstimos para compras parceladas:

- **Elegibilidade e Cadastro Automático (`POST /v1/customers`)**:
  - Avaliação de idade em tempo de requisição: aceita apenas clientes entre **18 e 65 anos**.
  - Atribuição instantânea de limite de crédito baseado na faixa etária ($3.000 para 18–25 anos; $5.000 para 26–30 anos; $8.000 para 31–65 anos).
  - Emissão de token JWT assinado (`X-Auth-Token`) para utilização imediata.
- **Concessão de Empréstimo BNPL (`POST /v1/loans`)**:
  - Validação estrita contra o saldo de crédito remanescente do cliente e dedução atômica transacional.
  - Seleção dinâmica de esquema de amortização: **Scheme 1** (13% de juros para nomes iniciados com C, L, H) ou **Scheme 2** (16% de juros para cliente com ID > 25 ou fallback).
  - Divisão precisa em **5 parcelas quinzenais** (D+14 a D+70) com conciliação exata de centavos na última parcela.
- **Consultas Seguras (`GET /v1/customers/{id}` e `GET /v1/loans/{id}`)**:
  - Endpoints autenticados via JWT com detalhamento de limites e cronogramas de amortização.
- **Tratamento Padronizado de Erros**:
  - Respostas JSON estruturadas sob o contrato OpenAPI com códigos rastreáveis (`APZ000001` a `APZ000008`).

---

## 📚 Documentação Completa da Solução

- **[System Design Specification](docs/superpowers/specs/2026-09-18-bnpl-system-design.md)**: Especificação arquitetural, contratos, regras de negócio e critérios de aceite.
- **[Plano de Implementação Detalhado](docs/superpowers/plans/2026-09-18-bnpl-system.md)**: Plano de tarefas executado via Subagent-Driven Development.
- **[Modelo C4 & Structurizr](docs/c4model.md)**: Diagramas de Contexto (C1) e Contêineres (C2) com especificações Structurizr DSL e Mermaid.
- **[Modelo Entidade-Relacionamento & Dicionário de Dados](docs/erd.md)**: Diagrama relacional Mermaid `erDiagram`, tabelas, constraints e índices.
- **[Operações Detalhadas com Diagramas de Sequência](docs/operations/)**:
  - [POST /v1/customers — Cadastro de Cliente](docs/operations/customer-registration.md)
  - [GET /v1/customers/{customerId} — Consulta de Saldo de Cliente](docs/operations/customer-lookup.md)
  - [POST /v1/loans — Criação de Empréstimo e Parcelamento](docs/operations/loan-creation.md)
  - [GET /v1/loans/{loanId} — Consulta de Empréstimo e Parcelas](docs/operations/loan-lookup.md)

---

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or later (`java -version`)
- **Docker & Docker Compose**: Docker Engine 24+ and Docker Compose v2+ (`docker compose version`)
- **Node.js & npm** (opcional, para testes de API via Newman): Node.js 18+ e npm 9+ (`node -v`, `npm -v`)
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

## 🧪 Automated API Testing with Newman / Postman

O repositório inclui uma suíte automatizada de testes de API e contrato baseada em **Postman Collection v2.1** e executável via **Newman** (CLI do Postman).

A suíte cobre:
- **Fluxo E2E Encadeado (Happy Path)**:
  1. `POST /v1/customers`: Cadastro de cliente elegível (idade 35 anos) e captura dinâmica do ID e do token JWT (`X-Auth-Token`).
  2. `GET /v1/customers/{id}`: Consulta do perfil e limite de crédito concedido ($8.000,00).
  3. `POST /v1/loans`: Criação de empréstimo de $1.000,00 com aplicação do esquema de juros Scheme 1 (13%) e 5 parcelas quinzenais.
  4. `GET /v1/loans/{id}`: Consulta e validação do cronograma de amortização (soma total de $1.130,00 com conciliação exata de centavos).
  5. `GET /v1/customers/{id}`: Validação do saldo de crédito remanescente atualizado ($7.000,00).
- **Validações de Falha e Contrato (Error & Edge Cases)**:
  - Cadastro de cliente menor de idade (< 18 anos) retornando `400 Bad Request` com código `APZ000002`.
  - Requisição sem cabeçalho de autenticação retornando `401 Unauthorized` com código `APZ000007`.
  - Consulta de cliente inexistente retornando `404 Not Found` com código `APZ000005`.
  - Solicitação de empréstimo excedendo o limite de crédito disponível retornando `400 Bad Request` com código `APZ000006`.

### Pré-requisito
Certifique-se de que a aplicação esteja em execução na porta `8080` antes de rodar os testes:
```bash
# Opção A: Subir via Docker Compose
docker compose up -d

# Opção B: Subir via Maven local
rtk ./mvnw spring-boot:run
```

### Passo a Passo de Execução

#### 1. Instalar as dependências do projeto (Node.js)
```bash
rtk npm install
```

#### 2. Executar a suíte via NPM Script
```bash
rtk npm run test:api
```
*(ou simplesmente `rtk npm test`)*

#### 3. Execução direta via `npx newman` (sem dependências locais)
Se preferir rodar diretamente sem instalar `node_modules`:
```bash
rtk npx newman run postman/bnpl-api.postman_collection.json \
  -e postman/local.postman_environment.json \
  --reporters cli
```

#### 4. Gerando relatórios detalhados (HTML / JSON)
Você pode habilitar relatórios adicionais passando múltiplos reporters para o Newman:
```bash
rtk npx newman run postman/bnpl-api.postman_collection.json \
  -e postman/local.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export target/newman-results.json
```

Arquivos da suíte:
- **Coleção Postman**: `postman/bnpl-api.postman_collection.json`
- **Ambiente Local**: `postman/local.postman_environment.json`

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
