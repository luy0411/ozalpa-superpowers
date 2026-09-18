# Operação: Criação de Empréstimo / Compra BNPL (`POST /v1/loans`)

## 1. Visão de Produto e Negócio

### Objetivo
Converter uma intenção de compra no checkout de um e-commerce parceiro em um empréstimo Buy Now, Pay Later (BNPL), validando a disponibilidade do saldo de crédito do cliente, determinando dinamicamente o plano de pagamento e agendando o cronograma de 5 parcelas quinzenais de amortização.

### Proposta de Valor
- **Financiamento Instantâneo:** Aprovação imediata sem necessidade de nova análise manual de bureau de crédito.
- **Transparência Financeira:** O cliente conhece com precisão o valor dos juros (comissão), o total financiado e as datas de vencimento exatas de cada parcela.

### Regras de Negócio e Cálculos Financeiros
1. **Controle Estrito de Saldo:**
   - O valor da compra (`amount`) não pode ser superior ao saldo disponível (`availableCreditLineAmount`) do cliente.
   - Caso exceda o saldo, a compra é rejeitada com HTTP `400 Bad Request` e código `APZ000006` (`INVALID_LOAN_REQUEST`), sem qualquer alteração na base de dados.
   - Sendo aprovada, a dedução do saldo disponível ocorre de forma atômica (`availableCreditLineAmount = availableCreditLineAmount - amount`).
2. **Resolução de Esquema de Pagamento (Ordem estrita de precedência):**
   - **Regra 1:** Se o primeiro nome do cliente começar com **'C'**, **'L'** ou **'H'** (case-insensitive) $\rightarrow$ Aplica-se o **Scheme 1** (taxa de juros de **13%**, 5 parcelas quinzenais).
   - **Regra 2:** Se o `id` sequencial do cliente for **maior que 25** (`id > 25`) $\rightarrow$ Aplica-se o **Scheme 2** (taxa de juros de **16%**, 5 parcelas quinzenais).
   - **Regra 3 (Fallback):** Para qualquer outro caso $\rightarrow$ Aplica-se o **Scheme 2** (taxa de juros de **16%**).
3. **Cálculos Financeiros e Conciliação de Centavos:**
   - **Comissão:** `commissionAmount = amount * interestRate` (arredondamento `HALF_EVEN` a 2 casas decimais).
   - **Total Financiado:** `totalAmount = amount + commissionAmount`.
   - **Parcelas:** O valor total é dividido em 5 parcelas base (`FLOOR`). Eventuais centavos residuais da dízima são somados à 5ª parcela, assegurando que a soma das 5 parcelas coincida exatamente com o `totalAmount` ao centavo.
4. **Agendamento Quinzenal (Biweekly):**
   - Parcela 1: Vencimento em **D+14** dias a contar da data da compra (`status: NEXT`).
   - Parcelas 2 a 5: Vencimentos em **D+28, D+42, D+56 e D+70** dias (`status: PENDING`).
   - O empréstimo nasce com status **`ACTIVE`**.

---

## 2. Diagrama de Sequência Interno (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Checkout as Checkout E-commerce / Cliente
    participant Filter as JwtAuthenticationFilter
    participant Controller as LoanController
    participant Service as LoanService
    participant CustRepo as CustomerRepository
    participant Resolver as PaymentSchemeResolver
    participant LoanRepo as LoanRepository
    participant DB as PostgreSQL
    participant Mapper as LoanMapper

    Checkout->>Filter: POST /v1/loans (customerId, amount)<br/>Header: Authorization: Bearer [JWT]
    activate Filter
    Filter->>Filter: Valida token JWT
    alt Token Inválido / Ausente
        Filter-->>Checkout: 401 Unauthorized (APZ000007)
    else Token Válido
        Filter->>Controller: Encaminha requisição autenticada
    end
    deactivate Filter

    activate Controller
    Controller->>Service: createLoan(LoanRequest)
    activate Service

    rect rgb(240, 248, 255)
    Note over Service, DB: Transação Atômica (@Transactional)
    Service->>CustRepo: findByExternalId(customerId)
    activate CustRepo
    CustRepo->>DB: SELECT * FROM customers WHERE external_id = ?
    DB-->>CustRepo: CustomerEntity
    CustRepo-->>Service: CustomerEntity
    deactivate CustRepo

    alt Cliente Inexistente
        Service-->>Controller: throw CustomerNotFoundException
        Controller-->>Checkout: 404 Not Found (APZ000008 / APZ000005)
    end

    alt amount > availableCreditLineAmount
        Service-->>Controller: throw InvalidLoanRequestException
        Controller-->>Checkout: 400 Bad Request (APZ000006 / INVALID_LOAN_REQUEST)
    end

    Service->>Service: availableCreditLineAmount -= amount
    Service->>CustRepo: save(customer)

    Service->>Resolver: resolveScheme(firstName, internalId)
    activate Resolver
    Resolver-->>Service: PaymentScheme (SCHEME_1 ou SCHEME_2)
    deactivate Resolver

    Service->>Resolver: calculatePlan(amount, scheme, purchaseDate)
    activate Resolver
    Note over Resolver: Calcula juros, total e 5 parcelas<br/>(D+14 a D+70 com ajuste de centavos)
    Resolver-->>Service: PaymentPlanCalculation
    deactivate Resolver

    Service->>LoanRepo: save(LoanEntity com 5 InstallmentEntities)
    activate LoanRepo
    LoanRepo->>DB: INSERT INTO loans (...)<br/>INSERT INTO installments (x5)
    DB-->>LoanRepo: LoanEntity persistido
    LoanRepo-->>Service: LoanEntity
    deactivate LoanRepo
    end

    Service->>Mapper: toLoanResponse(loan)
    activate Mapper
    Mapper-->>Service: LoanResponse
    deactivate Mapper

    Service-->>Controller: LoanResponse
    deactivate Service

    Controller-->>Checkout: 201 Created<br/>Location: /v1/loans/{loanId}<br/>Body: LoanResponse
    deactivate Controller
```

---

## 3. Contrato da Requisição e Resposta

### Exemplo de Requisição
```http
POST /v1/loans HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 1000.00
}
```

### Exemplo de Resposta de Sucesso (HTTP 201 Created)
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
