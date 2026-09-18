# Operação: Cadastro de Cliente (`POST /v1/customers`)

## 1. Visão de Produto e Negócio

### Objetivo
Permitir a entrada de novos clientes no ecossistema Buy Now, Pay Later (BNPL) da Aplazo, avaliando instantaneamente a elegibilidade e atribuindo uma linha de crédito inicial automática baseada na faixa etária do solicitante.

### Proposta de Valor
- **Onboarding sem atrito:** Processo instantâneo com decisão automática de crédito em tempo de requisição.
- **Autonomia para compras:** O cliente já recebe seu identificador único (`id`), limite de crédito aprovado e um token de autenticação JWT (`X-Auth-Token`) para realizar compras imediatamente.

### Regras de Negócio e Políticas de Crédito
1. **Faixa Etária de Aceitação:**
   - O cliente deve possuir idade entre **18 e 65 anos completos** no momento do cadastro.
   - Clientes com idade inferior a 18 anos ou superior a 65 anos são rejeitados com erro `400 Bad Request` e código de erro `APZ000002` (`INVALID_CUSTOMER_REQUEST`).
2. **Atribuição Automática de Linha de Crédito:**
   - **18 a 25 anos:** Crédito inicial de **$3.000,00**
   - **26 a 30 anos:** Crédito inicial de **$5.000,00**
   - **31 a 65 anos:** Crédito inicial de **$8.000,00**
3. **Disponibilidade Imediata:**
   - No momento da criação, o saldo disponível (`availableCreditLineAmount`) é igual à linha de crédito aprovada (`creditLineAmount`).
4. **Segurança e Continuidade:**
   - O endpoint é público (permite auto-cadastro).
   - A resposta inclui o cabeçalho `X-Auth-Token` com o JWT assinado contendo o UUID do cliente, permitindo consumo direto dos endpoints protegidos de empréstimo.

---

## 2. Diagrama de Sequência Interno (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente / E-commerce
    participant Controller as CustomerController
    participant Service as CustomerService
    participant Calculator as CreditLineCalculator
    participant Repo as CustomerRepository
    participant DB as PostgreSQL
    participant JWT as JwtTokenProvider

    Client->>Controller: POST /v1/customers (firstName, lastName, dateOfBirth)
    activate Controller

    Controller->>Controller: Valida campos (@Valid Jakarta)
    alt Campos inválidos
        Controller-->>Client: 400 Bad Request (APZ000002 / APZ000004)
    end

    Controller->>Service: createCustomer(CustomerRequest)
    activate Service

    Service->>Calculator: calculateCreditLine(dateOfBirth, today)
    activate Calculator
    alt Idade < 18 ou Idade > 65
        Calculator-->>Service: throw InvalidCustomerRequestException
        Service-->>Controller: Exception propagada
        Controller-->>Client: 400 Bad Request (APZ000002)
    else Idade Válida
        Calculator-->>Service: Retorna Limite ($3000 / $5000 / $8000)
    end
    deactivate Calculator

    Service->>Repo: save(CustomerEntity)
    activate Repo
    Repo->>DB: INSERT INTO customers (external_id, credit_line_amount, ...)
    DB-->>Repo: CustomerEntity persistido (id sequencial gerado)
    Repo-->>Service: CustomerEntity
    deactivate Repo

    Service->>JWT: generateToken(customer.externalId)
    activate JWT
    JWT-->>Service: Token JWT assinado (HMAC-SHA256)
    deactivate JWT

    Service-->>Controller: CustomerRegistrationResult (CustomerResponse, token)
    deactivate Service

    Controller-->>Client: 201 Created<br/>Location: /v1/customers/{id}<br/>X-Auth-Token: [JWT]<br/>Body: CustomerResponse
    deactivate Controller
```

---

## 3. Contrato da Requisição e Resposta

### Exemplo de Requisição
```http
POST /v1/customers HTTP/1.1
Content-Type: application/json

{
  "firstName": "Carlos",
  "lastName": "López",
  "secondLastName": "Pérez",
  "dateOfBirth": "1995-05-15"
}
```

### Exemplo de Resposta de Sucesso (HTTP 201 Created)
```http
HTTP/1.1 201 Created
Location: /v1/customers/3fa85f64-5717-4562-b3fc-2c963f66afa7
X-Auth-Token: eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "creditLineAmount": 5000.00,
  "availableCreditLineAmount": 5000.00,
  "createdAt": "2026-09-18T12:00:00Z"
}
```
