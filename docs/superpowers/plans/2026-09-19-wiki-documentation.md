# Wiki de Documentação Estática (HTML / Bootstrap) — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidar toda a documentação do projeto BNPL (`README.md`, `c4model.md`, `erd.md`, `operations/*.md`, `challenge/`, `specs` e `plans`) em um portal estático moderno em `docs/wiki/`, com Bootstrap 5.3, suporte nativo a diagramas Mermaid, Dark Mode e navegação 100% autônoma via browser local.

**Architecture:** Estrutura multi-página estática com layout padrão (Sidebar responsiva, Navbar com busca e Dark Mode, Breadcrumbs e Área de Conteúdo). Inclusão de assets locais em `docs/wiki/assets/` (CSS, JS, ícones e Mermaid), garantindo navegação rápida e autônoma sem dependência de build ou servidor web.

**Tech Stack:** HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3, Bootstrap Icons, Mermaid.js.

**Spec:** [docs/superpowers/specs/2026-09-19-wiki-documentation-design.md](file:///home/lucamuco/dev/git/ozalpa-superpowers/docs/superpowers/specs/2026-09-19-wiki-documentation-design.md)

## Global Constraints

- Todos os links entre páginas devem ser relativos, funcionando perfeitamente sob o protocolo `file://`.
- Todo o conteúdo dos arquivos Markdown originais deve ser integralmente preservado, organizado e visualmente estilizado.
- Diagramas Mermaid devem ser renderizados de forma legível e responsiva no carregamento da página.
- Suporte a Dark Mode e Light Mode com transição suave e persistência em `localStorage`.

---

### Task 1: Scaffolding dos Assets e Utilitários da Wiki

**Files:**
- Create: `docs/wiki/assets/css/bootstrap.min.css`
- Create: `docs/wiki/assets/css/bootstrap-icons.min.css`
- Create: `docs/wiki/assets/css/wiki.css`
- Create: `docs/wiki/assets/js/bootstrap.bundle.min.js`
- Create: `docs/wiki/assets/js/mermaid.min.js`
- Create: `docs/wiki/assets/js/wiki.js`

**Interfaces:**
- Consumes: Nenhum.
- Produces: CSS, JS e estilos base utilizados por todas as páginas da Wiki.

- [ ] **Step 1: Baixar e salvar bibliotecas vendor em `docs/wiki/assets/`**
- [ ] **Step 2: Criar arquivo de estilização customizada `wiki.css` (layout com sidebar, badges, dark mode e tabs)**
- [ ] **Step 3: Criar script utilitário `wiki.js` (inicialização de tema dark/light, setup de mermaid e copy button)**
- [ ] **Step 4: Verificar arquivos criados e integridade dos assets**
- [ ] **Step 5: Commit dos assets base**

---

### Task 2: Página Inicial da Wiki (`index.html`)

**Files:**
- Create: `docs/wiki/index.html`

**Interfaces:**
- Consumes: `docs/wiki/assets/css/*`, `docs/wiki/assets/js/*`, conteúdo do `README.md`.
- Produces: Ponto de entrada da Wiki com navegação completa, resumo do projeto, badges, arquitetura de validação Superpowers e guia de execução.

- [ ] **Step 1: Criar template mestre em `index.html` com layout de sidebar, navbar e breadcrumb**
- [ ] **Step 2: Converter e estruturar o conteúdo do `README.md` com componentes visuais Bootstrap (cards, badges de status, callouts, passos de build)**
- [ ] **Step 3: Testar abertura e validação visual de `index.html`**
- [ ] **Step 4: Commit de `index.html`**

---

### Task 3: Páginas de Arquitetura C4 e Modelo de Dados ERD

**Files:**
- Create: `docs/wiki/c4model.html`
- Create: `docs/wiki/erd.html`

**Interfaces:**
- Consumes: `docs/c4model.md`, `docs/erd.md`, Mermaid.js.
- Produces: Visualização rica com diagramas C4 (C1 Context e C2 Container), Structurizr DSL, Diagrama ERD e Dicionário de Dados interativo.

- [ ] **Step 1: Criar `docs/wiki/c4model.html` com os diagramas Mermaid C1 e C2, bloco Structurizr DSL com botão de cópia e tabela de componentes**
- [ ] **Step 2: Criar `docs/wiki/erd.html` com o diagrama relacional Mermaid, tabelas de dados de `customers`, `loans` e `installments` com badges de PK/FK/UK**
- [ ] **Step 3: Validar a renderização dos diagramas Mermaid e tabelas**
- [ ] **Step 4: Commit de `c4model.html` e `erd.html`**

---

### Task 4: Catálogo de Operações e Páginas Específicas da API

**Files:**
- Create: `docs/wiki/operations.html`
- Create: `docs/wiki/operations/customer-registration.html`
- Create: `docs/wiki/operations/customer-lookup.html`
- Create: `docs/wiki/operations/loan-creation.html`
- Create: `docs/wiki/operations/loan-lookup.html`

**Interfaces:**
- Consumes: `docs/operations/*.md`, Mermaid.js.
- Produces: Catálogo completo de endpoints REST, diagramas de sequência interativos, payloads de requisição/resposta, matrizes de validação e códigos de erro da aplicação.

- [ ] **Step 1: Criar `docs/wiki/operations.html` como hub central com cards de endpoints e links para cada operação**
- [ ] **Step 2: Criar `docs/wiki/operations/customer-registration.html` (POST /v1/customers com diagrama de sequência)**
- [ ] **Step 3: Criar `docs/wiki/operations/customer-lookup.html` (GET /v1/customers/{id} com diagrama de sequência)**
- [ ] **Step 4: Criar `docs/wiki/operations/loan-creation.html` (POST /v1/loans com fluxo financeiro e conciliação de centavos)**
- [ ] **Step 5: Criar `docs/wiki/operations/loan-lookup.html` (GET /v1/loans/{id} com visualização de parcelas)**
- [ ] **Step 6: Validar diagramas de sequência e navegação interna relativa**
- [ ] **Step 7: Commit das páginas de operações**

---

### Task 5: Páginas do Desafio Aplazo, Metodologia e Especificações Técnicas

**Files:**
- Create: `docs/wiki/challenge.html`
- Create: `docs/wiki/specs.html`

**Interfaces:**
- Consumes: `docs/challenge/README.md`, `docs/challenge/take-home.openapi.yml`, `docs/superpowers/specs/*`, `docs/superpowers/plans/*`.
- Produces: Página com a especificação original do desafio BNPL da Aplazo e página com os registros de engenharia e metodologia Superpowers.

- [ ] **Step 1: Criar `docs/wiki/challenge.html` com detalhamento das regras de crédito, requisitos da Aplazo e download/visualização do contrato OpenAPI**
- [ ] **Step 2: Criar `docs/wiki/specs.html` integrando o histórico de especificações e o plano de desenvolvimento autônomo**
- [ ] **Step 3: Validar links e referências cruzadas**
- [ ] **Step 4: Commit de `challenge.html` e `specs.html`**

---

### Task 6: Verificação Completa e Atualização do README Principal

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: Todo o portal `docs/wiki/`.
- Produces: Atualização da seção de documentação do `README.md` apontando e orientando sobre a nova Wiki HTML interativa.

- [ ] **Step 1: Executar verificação estrutural (checar integridade dos links relativos, assets e renderização Mermaid)**
- [ ] **Step 2: Atualizar `README.md` adicionando destaque e instruções de navegação para `docs/wiki/index.html`**
- [ ] **Step 3: Commit final e encerramento**
