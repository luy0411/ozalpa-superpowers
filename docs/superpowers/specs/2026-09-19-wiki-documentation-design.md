# Especificação de Design — Wiki de Documentação Estática (HTML/Bootstrap)

**Data:** 2026-09-19  
**Status:** Aprovado  
**Autor:** Antigravity / Superpowers  

---

## 1. Contexto e Objetivo

O repositório do projeto **BNPL REST API (Aplazo)** possui uma suíte extensa e estruturada de documentação técnica em formato Markdown (`README.md`, `docs/c4model.md`, `docs/erd.md`, `docs/operations/*.md`, `docs/challenge/` e especificações/planos do Superpowers).
O objetivo desta especificação é consolidar toda essa documentação técnica em um mini portal web estático, localizado em `docs/wiki/`, construído com **HTML5**, **Bootstrap 5.3**, ícones **Bootstrap Icons**, tema moderno responsivo (Dark/Light mode) e suporte à renderização de diagramas **Mermaid.js**.

A documentação deverá ser 100% navegável diretamente via browser (`file:///...`), sem necessidade de servidor HTTP ativo ou ferramentas de build de terceiros.

---

## 2. Requisitos e Diretrizes Globais

1. **Acesso Autônomo e Local (Offline First)**:
   - Os arquivos HTML devem funcionar diretamente ao abrir qualquer um deles em navegadores modernos via `file://`.
   - Recursos estáticos essenciais (estilos, ícones e scripts) organizados em `docs/wiki/assets/` com referências relativas.
2. **Framework e UI**:
   - Layout construído com **Bootstrap 5.3**.
   - Navegação lateral (Sidebar) fixa e recolhível em telas menores (Offcanvas / Toggle responsivo).
   - Barra superior (Navbar) com indicador de versão, busca instantânea e alternador de tema (Light / Dark).
   - Componentes visuais para rotas da API: badges coloridos (`POST` verde, `GET` azul), caixas de código formatadas com botão de cópia, tabelas estilizadas e callouts de alerta (`note`, `warning`, `tip`).
3. **Diagramas Interativos**:
   - Integração com **Mermaid.js** para renderizar os diagramas de C4 Context, C4 Containers, Entity-Relationship (ERD) e Diagramas de Sequência diretamente no navegador.
4. **Fidelidade e Completude**:
   - Nenhuma informação técnica dos documentos originais pode ser descartada ou resumida.

---

## 3. Arquitetura da Informação & Páginas da Wiki

A Wiki é composta pelas seguintes páginas organizadas dentro de `docs/wiki/`:

1. **`index.html` (Visão Geral & Quickstart)**:
   - Introdução do projeto BNPL REST API e validação metodológica com Superpowers.
   - Badges, stack tecnológica, links rápidos para as seções.
   - Instruções de build, testes, execução com Docker Compose e variáveis de ambiente.
2. **`c4model.html` (Arquitetura & Modelo C4)**:
   - Diagrama de Contexto de Sistema (C1) renderizado via Mermaid.
   - Diagrama de Contêineres (C2) renderizado via Mermaid.
   - Especificação Structurizr DSL (`workspace.dsl`) em bloco de código com destaque e cópia.
   - Tabela de rastreabilidade de contêineres e componentes.
3. **`erd.html` (Modelo de Dados & ERD)**:
   - Diagrama Entidade-Relacionamento (`erDiagram`) interativo com Mermaid.
   - Dicionário de Dados completo das tabelas `customers`, `loans` e `installments`.
   - Índices, constraints, integridade referencial e regras de negócio.
4. **`operations.html` (Catálogo de Operações da API)**:
   - Hub de navegação das 4 operações com resumo de endpoints, autenticação JWT e matriz de erros.
5. **`operations/customer-registration.html` (POST /v1/customers)**:
   - Diagrama de sequência completo, headers, validações de idade, faixas de limite e exemplos de payload (sucesso e erros `APZ000001`, `APZ000002`).
6. **`operations/customer-lookup.html` (GET /v1/customers/{customerId})**:
   - Diagrama de sequência, autenticação JWT Bearer, validações de formato UUID e erros (`APZ000003`, `APZ000004`).
7. **`operations/loan-creation.html` (POST /v1/loans)**:
   - Diagrama de sequência de concessão de empréstimo, regras dos Schemes 1 e 2, cálculo de juros, amortização em 5 parcelas quinzenais, conciliação de centavos e erros (`APZ000005`, `APZ000006`, `APZ000007`).
8. **`operations/loan-lookup.html` (GET /v1/loans/{loanId})**:
   - Diagrama de sequência, retorno das parcelas e cronogramas, status `NEXT`/`PENDING`/`ERROR`.
9. **`challenge.html` (Desafio BNPL & Requisitos Aplazo)**:
   - Especificação original do teste técnico, matriz de regras de negócio, códigos de erro padronizados e critérios de aceite.
10. **`specs.html` (Metodologia Superpowers & Especificações)**:
    - Registro formal do Design Spec e do Plano de Implementação executado autonomamente pelos subagentes.

---

## 4. Design Visual e Assets

### 4.1 Estrutura de Assets (`docs/wiki/assets/`)
- `css/bootstrap.min.css`: Bootstrap 5.3 base.
- `css/bootstrap-icons.min.css` (ou ícones SVG embutidos/CSS leve).
- `css/wiki.css`: Regras customizadas de layout, sidebar fixa/scrollável, badges de métodos REST, blocos de código com botão de cópia, modo escuro/claro suave.
- `js/bootstrap.bundle.min.js`: Suporte aos dropdowns, collapse e offcanvas.
- `js/mermaid.min.js`: Engine Mermaid para renderização de diagramas client-side.
- `js/wiki.js`: Script utilitário para:
  - Inicialização do Mermaid (com suporte automático aos temas claro/escuro).
  - Alternância de tema (`light` / `dark`) com persistência em `localStorage`.
  - Botões de cópia rápida para blocos de código (`pre code`).
  - Filtro/busca dinâmica no menu da sidebar.

---

## 5. Critérios de Aceite e Verificação

- [ ] Todas as 10 páginas HTML criadas e estruturadas em `docs/wiki/`.
- [ ] Diagramas Mermaid presentes no C4, ERD e nas 4 operações renderizando graficamente sem erros de sintaxe.
- [ ] Navegação cruzada entre todas as páginas funcionando perfeitamente via caminhos relativos em `file://`.
- [ ] Alternador de Dark/Light mode funcional e responsividade validada (mobile/desktop).
- [ ] Dicionário de dados, tabelas de códigos de erro e exemplos de JSON completos e fiéis aos `.md` originais.
