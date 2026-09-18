# Spec de Design: Sistema BNPL (Aplazo Backend Challenge)

- **Data:** 2026-09-18
- **Autor:** Antigravity / Luca Muco
- **Status:** Proposto / Validado em Brainstorming

---

## 1. Visão Geral e Objetivos

Desenvolver uma aplicação REST de Buy Now Pay Later (BNPL) baseada nos requisitos do [Aplazo Backend Challenge](file:///home/lucamuco/dev/git/ozalpa-superpowers/docs/challenge/README.md) e na especificação [take-home.openapi.yml](file:///home/lucamuco/dev/git/ozalpa-superpowers/docs/challenge/take-home.openapi.yml).

A aplicação contempla o ciclo de vida de crédito ao consumidor:
1. Cadastro de clientes com verificação de idade e concessão automática de limite de crédito.
2. Emissão de token JWT para autenticação/autorização nas operações de crédito.
3. Concessão de empréstimos (loans) para compras, validando saldo de crédito disponível.
4. Resolução dinâmica de esquemas de pagamento (taxa de juros e cálculo de 5 parcelas quinzenais).
5. Consulta de dados cadastrais e de empréstimos com cronograma de parcelas e status.

---

## 2. Tecnologias e Ferramentas

- **Linguagem:** Java 21 LTS (com suporte a Virtual Threads e Records).
- **Framework:** Spring Boot 3.4.x (Web, Security, Data JPA, Validation, Actuator).
- **Build Tool:** Apache Maven com wrapper (`mvnw`).
- **Banco de Dados:** PostgreSQL 16.
- **Migrações:** Flyway (`src/main/resources/db/migration`).
- **Segurança:** Spring Security 6 + JWT (HMAC-SHA256).
- **Testes Unitários:** JUnit 5 + Mockito + AssertJ (foco em cenários principais de sucesso e falhas essenciais, meta de cobertura de ~50%).
- **Testes de Integração:** Cucumber (BDD com cenários Gherkin em `.feature`) + SpringBootTest + Testcontainers (PostgreSQL).
- **Containerização:** Dockerfile multi-stage e `docker-compose.yml`.
- **Documentação de Banco:** Arquivo dedicado `docs/erd.md` com diagrama Mermaid (`erDiagram`) e Dicionário de Dados.

---

## 3. Arquitetura e Estrutura do Projeto

### 3.1 Padrão Arquitetural
Arquitetura em Camadas Clássica (Controller-Service-Repository) com DTOs em Java Records e Services de Domínio Isolados para regras financeiras e políticas de elegibilidade.

```text
src/main/java/com/aplazo/bnpl/
├── config/                     # Configurações de Security, Swagger/OpenAPI, Jackson, Clock
├── controller/                 # REST Controllers (CustomerController, LoanController)
├── dto/
│   ├── request/                # Records de requisição com validações Bean Validation
│   ├── response/               # Records de resposta estritamente compatíveis com OpenAPI
│   └── mapper/                 # Conversores entre DTOs e Entidades JPA
├── entity/                     # Entidades JPA (CustomerEntity, LoanEntity, InstallmentEntity)
├── repository/                 # Repositórios Spring Data JPA
├── service/                    # Regras de negócio da aplicação
│   ├── CustomerService.java    # Gestão de clientes e crédito
│   ├── LoanService.java        # Gestão de empréstimos e atualização de crédito
│   ├── CreditLineCalculator.java  # Cálculo da linha de crédito por faixa etária
│   └── PaymentSchemeResolver.java # Regras de escolha de esquema e cálculo de parcelas
├── security/                   # JwtTokenProvider, JwtAuthenticationFilter, SecurityFilterChain
└── exception/                  # Exceções customizadas e GlobalExceptionHandler (@RestControllerAdvice)
```

---

## 4. Modelo de Dados e Dicionário de Dados

A persistência será executada em PostgreSQL com migração inicial Flyway (`V1__initial_schema.sql`). Um documento separado `docs/erd.md` detalhará o diagrama Mermaid e os tipos.

### 4.1 Entidades e Tabelas

#### Tabela `customers`
| Coluna | Tipo SQL | Modificadores | Descrição |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Identificador sequencial interno (usado na regra de negócio `id > 25`). |
| `external_id` | UUID | NOT NULL UNIQUE | Identificador público exposto na API. |
| `first_name` | VARCHAR(100) | NOT NULL | Primeiro nome do cliente. |
| `last_name` | VARCHAR(100) | NOT NULL | Sobrenome paterno. |
| `second_last_name` | VARCHAR(100) | NOT NULL | Sobrenome materno. |
| `date_of_birth` | DATE | NOT NULL | Data de nascimento. |
| `credit_line_amount` | NUMERIC(15, 2) | NOT NULL | Limite de crédito concedido inicialmente. |
| `available_credit_line_amount` | NUMERIC(15, 2) | NOT NULL | Saldo de crédito restante disponível para novas compras. |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | Data/hora de registro do cliente. |

#### Tabela `loans`
| Coluna | Tipo SQL | Modificadores | Descrição |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Identificador sequencial interno. |
| `external_id` | UUID | NOT NULL UNIQUE | Identificador público exposto na API (`loanId`). |
| `customer_id` | BIGINT | NOT NULL REFERENCES customers(id) | Chave estrangeira para o cliente tomador. |
| `amount` | NUMERIC(15, 2) | NOT NULL | Valor original solicitado da compra/empréstimo. |
| `commission_amount` | NUMERIC(15, 2) | NOT NULL | Valor da comissão/juros calculado. |
| `total_amount` | NUMERIC(15, 2) | NOT NULL | Valor total financiado (`amount + commission_amount`). |
| `scheme_name` | VARCHAR(20) | NOT NULL | Nome do esquema aplicado (`SCHEME_1` ou `SCHEME_2`). |
| `interest_rate` | NUMERIC(5, 4) | NOT NULL | Taxa de juros aplicada (`0.13` ou `0.16`). |
| `status` | VARCHAR(20) | NOT NULL | Status do empréstimo (`ACTIVE`, `LATE`, `COMPLETED`). |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | Data/hora de criação da operação. |

#### Tabela `installments`
| Coluna | Tipo SQL | Modificadores | Descrição |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Identificador interno da parcela. |
| `loan_id` | BIGINT | NOT NULL REFERENCES loans(id) ON DELETE CASCADE | Vínculo com o empréstimo pai. |
| `installment_number` | INT | NOT NULL | Número ordinal da parcela (1 a 5). |
| `amount` | NUMERIC(15, 2) | NOT NULL | Valor individual da parcela. |
| `scheduled_payment_date` | DATE | NOT NULL | Data prevista de vencimento quinzenal. |
| `status` | VARCHAR(20) | NOT NULL | Status da parcela (`NEXT` para parcela 1, `PENDING` para 2-5). |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | Data/hora do registro. |

---

## 5. Regras de Negócio e Casos de Uso

### 5.1 Cadastro de Cliente (`POST /v1/customers`)
1. **Validação de Idade:**
   - Idade calculada: `Period.between(dateOfBirth, LocalDate.now()).getYears()`.
   - Idade permitida: entre **18 e 65 anos** (inclusivo).
   - Clientes com idade `< 18` ou `> 65` são rejeitados com erro `400 Bad Request` e código `APZ000002` (`INVALID_CUSTOMER_REQUEST`).
2. **Atribuição de Linha de Crédito:**
   - 18 a 25 anos: **$3.000,00**
   - 26 a 30 anos: **$5.000,00**
   - 31 a 65 anos: **$8.000,00**
   - Inicialmente, `availableCreditLineAmount = creditLineAmount`.
3. **Resposta:**
   - Status `201 Created`.
   - Header `Location: /v1/customers/{externalId}`.
   - Header `X-Auth-Token: <JWT>`.
   - Body compatível com `CustomerResponse` contendo `id` (UUID), `creditLineAmount`, `availableCreditLineAmount`, `createdAt`.

### 5.2 Empréstimo / Compra (`POST /v1/loans`)
1. **Autenticação:**
   - Endpoint protegido; exige `Authorization: Bearer <token>` ou `X-Auth-Token`.
2. **Validação de Limite:**
   - Busca cliente pelo `customerId` (UUID). Se inexistente, retorna `404 Not Found` com código `APZ000008` ou `APZ000005`.
   - Se `amount > availableCreditLineAmount`: retorna `400 Bad Request` com código `APZ000006` (`INVALID_LOAN_REQUEST`).
   - Bloqueio com transação atômica (`@Transactional`) para atualizar o saldo disponível do cliente: `availableCreditLineAmount = availableCreditLineAmount - amount`.
3. **Resolução do Esquema de Pagamento:**
   - **Regra 1:** Se `firstName` iniciar com `C`, `L` ou `H` (case-insensitive) $\rightarrow$ **Scheme 1** (13% de juros, 5 parcelas quinzenais).
   - **Regra 2:** Se o `id` sequencial do cliente for **> 25** $\rightarrow$ **Scheme 2** (16% de juros, 5 parcelas quinzenais).
   - **Regra 3:** Caso nenhuma das regras acima seja atendida $\rightarrow$ **Scheme 2** (16% de juros).
4. **Cálculos Financeiros e Parcelamento:**
   - `commissionAmount = amount * interestRate` (arredondamento `HALF_EVEN`, 2 casas decimais).
   - `totalAmount = amount + commissionAmount`.
   - `installmentAmount = (totalAmount / 5)` (tratamento de centavos residuais adicionados na última parcela para fechamento exato da soma).
   - 5 parcelas quinzenais:
     - Parcela 1: Vencimento em D+14 dias a partir da data da compra; status = `NEXT`.
     - Parcelas 2 a 5: Vencimentos em D+28, D+42, D+56, D+70 dias; status = `PENDING`.
   - Status do empréstimo: `ACTIVE`.
5. **Resposta:**
   - Status `201 Created`.
   - Header `Location: /v1/loans/{externalId}`.
   - Body compatível com `LoanResponse` contendo `id`, `customerId`, `amount`, `status`, `createdAt`, `paymentPlan` (com `commissionAmount` e a lista das 5 parcelas).

### 5.3 Consultas (`GET /v1/customers/{customerId}` e `GET /v1/loans/{loanId}`)
- Exigem autenticação JWT.
- Retornam HTTP 200 com DTO correspondente ou 404 caso o identificador não exista.

---

## 6. Tratamento de Exceções e Erros da API

Todas as respostas de erro devem obedecer ao contrato OpenAPI `ErrorResponse`:
```json
{
  "code": "APZ000002",
  "error": "INVALID_CUSTOMER_REQUEST",
  "timestamp": 1739397485,
  "message": "Customer must be between 18 and 65 years old",
  "path": "/v1/customers"
}
```

Tabela de Mapeamento de Erros:
- `APZ000001`: `INTERNAL_SERVER_ERROR` (500)
- `APZ000002`: `INVALID_CUSTOMER_REQUEST` (400 - validações de campos ou faixa etária rejeitada)
- `APZ000004`: `INVALID_REQUEST` (400 - UUID mal formatado ou corpo JSON inválido)
- `APZ000005`: `CUSTOMER_NOT_FOUND` (404)
- `APZ000006`: `INVALID_LOAN_REQUEST` (400 - valor excede o limite disponível ou valor <= 0)
- `APZ000007`: `UNAUTHORIZED` (401 - token ausente ou inválido)
- `APZ000008`: `LOAN_NOT_FOUND` (404)

---

## 7. Estratégia de Testes

### 7.1 Testes Unitários (JUnit 5 + Mockito)
- **Foco:** Meta de cobertura de ~50%, com foco estritamente nos cenários principais de sucesso e nas falhas essenciais, evitando acúmulo prematuro de testes unitários.
- **Classes-alvo essenciais:**
  - `CreditLineCalculatorTest`: Cenários válidos de concessão de limite e rejeição por idade.
  - `PaymentSchemeResolverTest`: Cenários principais de determinação de Scheme (1 e 2) e cálculo básico das parcelas.
  - `CustomerServiceTest` e `LoanServiceTest`: Fluxos fundamentais de cadastro e solicitação de empréstimo.

### 7.2 Testes Integrados em BDD (Cucumber + Testcontainers + SpringBootTest)
- **Foco:** Especificação viva dos cenários de negócio ponta a ponta com banco real (PostgreSQL em container).
- **Arquivos `.feature` (`src/test/resources/features/`):**
  1. `customer_registration.feature`:
     - Cadastro com sucesso para faixas de 18-25, 26-30 e 31-65 anos.
     - Rejeição de cliente menor de 18 anos (`APZ000002`).
     - Rejeição de cliente com mais de 65 anos (`APZ000002`).
     - Presença do token JWT no header `X-Auth-Token`.
  2. `loan_creation.feature`:
     - Criação de empréstimo dentro do limite de crédito atribuído.
     - Atualização correta do saldo remanescente de crédito (`availableCreditLineAmount`).
     - Tentativa de compra com valor superior ao saldo disponível (`APZ000006`).
     - Atribuição correta do Scheme 1 quando o nome começa com 'C', 'L' ou 'H'.
     - Atribuição correta do Scheme 2 quando o cliente possui ID sequencial > 25.
     - Verificação das 5 parcelas com datas quinzenais (D+14 a D+70) e status (`NEXT` para a primeira e `PENDING` para as demais).
  3. `security.feature`:
     - Bloqueio de acesso a `/v1/loans` sem token JWT (`401 UNAUTHORIZED`).
     - Acesso permitido utilizando o token gerado no cadastro.

---

## 8. Ambiente de Execução e Containerização

### 8.1 Dockerfile
Multi-stage build otimizado com cache de dependências:
- Build stage: `eclipse-temurin:21-jdk-alpine` executando `mvn clean package -DskipTests`.
- Run stage: `eclipse-temurin:21-jre-alpine` executando com usuário desprivilegiado (não-root).

### 8.2 Docker Compose (`docker-compose.yml`)
- Serviço `postgres`: Imagem `postgres:16-alpine`, com healthcheck via `pg_isready` e volume persistente.
- Serviço `app`: Constrói a imagem local, aguarda o healthcheck do PostgreSQL e expõe a porta `8080`.
- Variáveis de ambiente configuradas para conexão com o banco e segredo JWT.

---

## 9. Documentação Entregue

1. `docs/challenge/`: Cópia do README e OpenAPI originais para referência.
2. `docs/superpowers/specs/2026-09-18-bnpl-system-design.md`: Esta especificação de arquitetura e design.
3. `docs/erd.md`: Diagrama Entidade-Relacionamento em formato Mermaid (`erDiagram`) acompanhado do Dicionário de Dados completo.
4. `README.md` (na raiz do projeto): Instruções detalhadas de build, execução com Docker Compose, execução dos testes Cucumber e unitários, e exemplos de chamadas cURL/Swagger.
