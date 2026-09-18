# Operação: Consulta de Empréstimo (`GET /v1/loans/{loanId}`)

## 1. Visão de Produto e Negócio

### Objetivo
Disponibilizar a visão detalhada de um empréstimo específico, fornecendo ao cliente e ao ecossistema de gestão de cobranças o plano completo de amortização, valor da comissão cobrada, status geral do crédito (`ACTIVE`, `LATE`, `COMPLETED`) e o status individual de cada parcela (`NEXT`, `PENDING`, `ERROR`).

### Proposta de Valor
- **Acompanhamento Financeiro:** O cliente visualiza com clareza quais parcelas estão por vencer, a próxima parcela a pagar (`NEXT`) e as datas programadas.
- **Gestão de Ciclo de Vida:** Suporta futuras integrações de liquidação de parcelas e conciliação bancária.

### Regras de Negócio
1. **Identificador Único:** Busca realizada exclusivamente pelo UUID público do empréstimo (`loanId`).
2. **Autenticação:** Exige token JWT válido (`ROLE_CUSTOMER`). Acesso sem credenciais retorna HTTP `401 Unauthorized` (`APZ000007`).
3. **Tratamento de Inexistência:** Caso o empréstimo não exista, a resposta é HTTP `404 Not Found` com código `APZ000008` (`LOAN_NOT_FOUND`).
4. **Formato de Erros:** IDs malformados que não atendem ao padrão UUID retornam HTTP `400 Bad Request` (`APZ000004`).

---

## 2. Diagrama de Sequência Interno (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente / Painel Financeiro
    participant Filter as JwtAuthenticationFilter
    participant Controller as LoanController
    participant Service as LoanService
    participant Repo as LoanRepository
    participant DB as PostgreSQL
    participant Mapper as LoanMapper

    Client->>Filter: GET /v1/loans/{loanId}<br/>Header: Authorization: Bearer [JWT]
    activate Filter

    Filter->>Filter: Valida token e permissões
    alt Token Inválido / Ausente
        Filter-->>Client: 401 Unauthorized (APZ000007)
    else Token Válido
        Filter->>Controller: Encaminha requisição autenticada
    end
    deactivate Filter

    activate Controller
    Controller->>Service: getLoanByExternalId(loanId)
    activate Service

    rect rgb(240, 248, 255)
    Note over Service, DB: Leitura Transacional (@Transactional(readOnly = true))
    Service->>Repo: findByExternalId(loanId)
    activate Repo
    Repo->>DB: SELECT * FROM loans WHERE external_id = ?<br/>(com lazy-loading das 5 parcelas de installments)
    DB-->>Repo: LoanEntity com lista de InstallmentEntity
    Repo-->>Service: Optional<LoanEntity>
    deactivate Repo
    end

    alt Empréstimo não encontrado
        Service-->>Controller: throw LoanNotFoundException
        Controller-->>Client: 404 Not Found (APZ000008 / LOAN_NOT_FOUND)
    else Empréstimo Encontrado
        Service->>Mapper: toLoanResponse(entity)
        activate Mapper
        Note over Mapper: Mapeia dados gerais, comissão e<br/>as 5 parcelas ordenadas
        Mapper-->>Service: LoanResponse
        deactivate Mapper
        Service-->>Controller: LoanResponse
    end
    deactivate Service

    Controller-->>Client: 200 OK<br/>Body: LoanResponse
    deactivate Controller
```

---

## 3. Contrato da Requisição e Resposta

### Exemplo de Requisição
```http
GET /v1/loans/7fa85f64-5717-4562-b3fc-2c963f66afa8 HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Exemplo de Resposta de Sucesso (HTTP 200 OK)
```http
HTTP/1.1 200 OK
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
