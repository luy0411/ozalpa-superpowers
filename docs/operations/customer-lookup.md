# Operação: Consulta de Cliente (`GET /v1/customers/{customerId}`)

## 1. Visão de Produto e Negócio

### Objetivo
Permitir a consulta do status cadastral e financeiro de um cliente cadastrado, fornecendo a visibilidade de sua linha de crédito total e, principalmente, do **saldo disponível em tempo real** para novas compras no modelo Buy Now, Pay Later (BNPL).

### Proposta de Valor
- **Transparência de Limite:** Permite que o cliente ou checkout de e-commerce verifique instantaneamente o poder de compra remanescente antes de submeter uma nova compra.
- **Segurança de Acesso:** Protegido por autenticação JWT (Bearer ou `X-Auth-Token`), garantindo que dados cadastrais e limites só sejam acessados por clientes autenticados.

### Regras de Negócio
1. **Identificador Público:** A busca é realizada pelo UUID público do cliente (`customerId`), sem expor IDs sequenciais internos do banco de dados.
2. **Autenticação Obrigatória:** Requisições sem token válido retornam HTTP `401 Unauthorized` com código `APZ000007`.
3. **Tratamento de Inexistência:** Caso o `customerId` informado não exista na base de dados, a API retorna HTTP `404 Not Found` com código de erro `APZ000005` (`CUSTOMER_NOT_FOUND`).
4. **Validação de Formato:** Identificadores que não representam um UUID válido são interceptados com HTTP `400 Bad Request` (`APZ000004`).

---

## 2. Diagrama de Sequência Interno (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente / App Mobile
    participant Filter as JwtAuthenticationFilter
    participant Controller as CustomerController
    participant Service as CustomerService
    participant Repo as CustomerRepository
    participant DB as PostgreSQL
    participant Mapper as CustomerMapper

    Client->>Filter: GET /v1/customers/{customerId}<br/>Header: Authorization: Bearer [JWT]
    activate Filter

    Filter->>Filter: Valida token e extrai claims
    alt Token Ausente ou Inválido
        Filter-->>Client: 401 Unauthorized (APZ000007 / UNAUTHORIZED)
    else Token Válido
        Filter->>Filter: Autentica SecurityContext
        Filter->>Controller: Encaminha requisição
    end
    deactivate Filter

    activate Controller
    Controller->>Service: getCustomerByExternalId(customerId)
    activate Service

    Service->>Repo: findByExternalId(customerId)
    activate Repo
    Repo->>DB: SELECT * FROM customers WHERE external_id = ?
    DB-->>Repo: Registro encontrado / vazio
    Repo-->>Service: Optional<CustomerEntity>
    deactivate Repo

    alt Cliente não encontrado
        Service-->>Controller: throw CustomerNotFoundException
        Controller-->>Client: 404 Not Found (APZ000005 / CUSTOMER_NOT_FOUND)
    else Cliente Encontrado
        Service->>Mapper: toCustomerResponse(entity)
        activate Mapper
        Mapper-->>Service: CustomerResponse
        deactivate Mapper
        Service-->>Controller: CustomerResponse
    end
    deactivate Service

    Controller-->>Client: 200 OK<br/>Body: CustomerResponse
    deactivate Controller
```

---

## 3. Contrato da Requisição e Resposta

### Exemplo de Requisição
```http
GET /v1/customers/3fa85f64-5717-4562-b3fc-2c963f66afa7 HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Exemplo de Resposta de Sucesso (HTTP 200 OK)
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "creditLineAmount": 5000.00,
  "availableCreditLineAmount": 3800.00,
  "createdAt": "2026-09-18T12:00:00Z"
}
```
