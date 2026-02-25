# Jira Epics & Stories — Migração COBOL → React
## COG-GTM/COBOL-Legacy-Benchmark-Suite — Investment Portfolio Management System

> **Projeto Jira:** COG-GTM  
> **Repositório:** COG-GTM/COBOL-Legacy-Benchmark-Suite  
> **Devin Session:** https://app.devin.ai/sessions/e4855e1c8a3e46f7b4d39b498b7d9a3d  
> **Data:** 2026-02-25  

---

## Sumário de Epics

| Epic | Título | Fase | Prioridade | Story Points Total |
|------|--------|------|------------|-------------------|
| EPIC-01 | REST API Backend — Camada de Abstração de Dados | 1 | Alta | 55 SP |
| EPIC-02 | Frontend React — Componentes das Telas BMS | 2 | Alta | 47 SP |
| EPIC-03 | Autenticação JWT — Substituição do SECMGR/RACF | 3 | Alta | 34 SP |
| EPIC-04 | Batch Jobs Modernizados — Pipeline de Processamento | 4 | Média | 28 SP |
| EPIC-05 | Dashboards React — Migração dos Relatórios | 5 | Baixa | 35 SP |

**Total estimado:** ~199 Story Points

---

## Dependências entre Epics

```
EPIC-01 (REST API) ──────────────────────────────────────────────────────┐
    │                                                                      │
    ├──► EPIC-02 (Frontend React)  [depende de EPIC-01]                   │
    │                                                                      │
    ├──► EPIC-03 (Auth JWT)        [depende de EPIC-01]                   │
    │         │                                                            │
    │         └──► EPIC-02 (Frontend React) [depende de EPIC-03]          │
    │                                                                      │
EPIC-04 (Batch Jobs)  [independente — pode rodar em paralelo com EPIC-01] │
    │                                                                      │
    └──► EPIC-05 (Dashboards)      [depende de EPIC-01 + EPIC-04]         │
```

---

---

# EPIC-01 — REST API Backend: Camada de Abstração de Dados

**Título Jira:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] REST API Backend — Camada de Abstração de Dados`  
**Fase:** 1 — Alta Prioridade  
**Objetivo:** Criar uma REST API (Node.js/Express ou Java/Spring Boot) que exponha os dados atualmente acessados pelos programas COBOL `INQPORT`, `INQHIST`, `DB2ONLN`, `CURSMGR` e `DB2RECV`, mantendo o DB2 e VSAM existentes como fonte de dados.  
**Estimativa Total:** 55 Story Points  
**Dependências de entrada:** Nenhuma (ponto de partida)  
**Dependências de saída:** EPIC-02, EPIC-03, EPIC-05

---

### Story EPIC-01-01 — Setup do Projeto Backend e Estrutura Base

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Setup do projeto REST API backend (Node.js/Express ou Java/Spring Boot)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2-3 dias)  
**Dependências:** Nenhuma

**Descrição:**  
Criar a estrutura base do projeto backend que servirá como camada de abstração entre o frontend React e os dados legados (DB2 + VSAM). O projeto deve incluir configuração de ambiente, estrutura de pastas, middleware base e documentação OpenAPI/Swagger.

**Critérios de Aceite:**
- [ ] Projeto inicializado com gerenciador de dependências (npm/Maven/Gradle)
- [ ] Estrutura de pastas definida: `src/routes/`, `src/controllers/`, `src/services/`, `src/middleware/`, `src/config/`
- [ ] Servidor HTTP rodando na porta configurável via variável de ambiente
- [ ] Health check endpoint: `GET /api/health` retornando `{ status: "ok", timestamp: "..." }`
- [ ] Documentação Swagger/OpenAPI disponível em `/api/docs`
- [ ] Variáveis de ambiente configuradas: `DB2_HOST`, `DB2_PORT`, `DB2_DATABASE` (`POSMVP`), `DB2_USER`, `DB2_PASSWORD`, `DB2_POOL_MAX` (100), `DB2_POOL_TIMEOUT` (300s)
- [ ] Dockerfile criado para containerização
- [ ] README com instruções de setup

**Notas Técnicas:**
- O banco DB2 existente (`POSMVP`) deve ser mantido — a API é apenas uma camada de abstração
- Replicar as configurações do pool de conexões do `DB2ONLN.cbl`: máximo de 100 conexões, timeout de 300s

---

### Story EPIC-01-02 — Pool de Conexões DB2 (Equivalente ao DB2ONLN + DB2RECV)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar pool de conexões DB2 com retry logic (equivalente a DB2ONLN + DB2RECV)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-01-01  
**Mapeamento COBOL:**
- `DB2ONLN.cbl` → Pool de conexões DB2 (máx 100, timeout 300s)
- `DB2RECV.cbl` → Retry logic (3 tentativas, intervalo de 2s)

**Descrição:**  
Implementar o gerenciador de conexões DB2 no backend moderno, replicando o comportamento do `DB2ONLN.cbl` (pool de até 100 conexões, timeout de 300s) e a lógica de recuperação do `DB2RECV.cbl` (3 tentativas de reconexão com intervalo de 2 segundos).

**Critérios de Aceite:**
- [ ] Pool de conexões configurado com `min: 2`, `max: 100`, `idleTimeoutMillis: 300000` (300s)
- [ ] Retry logic implementado: máximo 3 tentativas com intervalo de 2 segundos entre tentativas (equivalente ao `WS-MAX-RETRIES = 3` e `WS-RETRY-INTERVAL = 2` do `DB2RECV.cbl`)
- [ ] Endpoint `GET /api/health/db` retornando status do pool: `{ active, idle, total, max }`
- [ ] Logs estruturados para conexões abertas/fechadas/erros
- [ ] Rollback automático em caso de falha de transação (equivalente ao `P200-RECOVER-TRANSACTION` do `DB2RECV.cbl`)
- [ ] Testes unitários para retry logic

**Notas Técnicas:**
- `DB2ONLN.cbl` gerencia: `WS-MAX-CONNECTIONS = 100`, connect/disconnect/status
- `DB2RECV.cbl` gerencia: `WS-MAX-RETRIES = 3`, `WS-RETRY-INTERVAL = 2`, recovery de connection/transaction/cursor

---

### Story EPIC-01-03 — Endpoint de Posições do Portfólio (Equivalente ao INQPORT)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar GET /api/portfolios/{id}/positions (equivalente ao INQPORT.cbl)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-01-02  
**Mapeamento COBOL:**
- `INQPORT.cbl` → `GET /api/portfolios/{id}/positions`
- `POSMAP` (BMS) → Campos: `fundId`, `fundName`, `units`, `costBasis`, `marketValue`
- Tabelas DB2: `INVESTMENT_POSITIONS`, `PORTFOLIO_MASTER`
- VSAM: `POSFILE` (Position Master)

**Descrição:**  
Implementar o endpoint REST que substitui o programa `INQPORT.cbl`. O programa original lê do VSAM `POSFILE` via `EXEC CICS READ FILE('POSFILE')` e exibe os dados na tela `POSMAP`. O endpoint deve consultar as tabelas DB2 `INVESTMENT_POSITIONS` e `PORTFOLIO_MASTER` (mantendo o DB2 existente como fonte de dados).

**Critérios de Aceite:**
- [ ] `GET /api/portfolios/{portfolioId}/positions` retorna lista de posições
- [ ] Response body:
  ```json
  {
    "portfolioId": "PORT00001",
    "portfolioName": "...",
    "positions": [
      {
        "investmentId": "...",
        "fundName": "...",
        "quantity": 0.0000,
        "costBasis": 0.00,
        "marketValue": 0.00,
        "currencyCode": "USD",
        "positionDate": "YYYY-MM-DD"
      }
    ]
  }
  ```
- [ ] HTTP 404 quando portfólio não encontrado (equivalente ao `P900-NOT-FOUND` do `INQPORT.cbl`)
- [ ] HTTP 500 com mensagem de erro estruturada para erros de acesso a dados (equivalente ao `P999-ERROR-ROUTINE`)
- [ ] Suporte a query param `?date=YYYY-MM-DD` para consulta por data (usa view `CURRENT_POSITIONS`)
- [ ] Testes de integração com DB2

**Notas Técnicas:**
- `INQPORT.cbl` acessa `POSFILE` via VSAM; na API, usar tabela `INVESTMENT_POSITIONS` do DB2
- Campos do `POSMAP`: `FUNDOUT` (Fund ID), `NAMEOUT` (Fund Name), `UNITOUT` (Units), `COSTOUT` (Cost Basis), `VALOUT` (Market Value)

---

### Story EPIC-01-04 — Endpoint de Histórico de Transações com Paginação (Equivalente ao INQHIST + CURSMGR)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar GET /api/portfolios/{id}/history com paginação server-side (equivalente ao INQHIST.cbl + CURSMGR.cbl)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 13 SP (~5-6 dias)  
**Dependências:** EPIC-01-02  
**Mapeamento COBOL:**
- `INQHIST.cbl` → `GET /api/portfolios/{id}/history?from=&to=&page=&size=`
- `CURSMGR.cbl` → Paginação server-side (cursor DB2 → offset/limit na API)
- `HISMAP` (BMS) → 10 linhas por página (ROW1..ROW10)
- Tabela DB2: `POSHIST` (particionada por trimestre)

**Descrição:**  
Implementar o endpoint REST que substitui `INQHIST.cbl` + `CURSMGR.cbl`. O `INQHIST.cbl` usa cursores DB2 (via `CURSMGR`) para buscar histórico paginado — a tela `HISMAP` exibe 10 linhas por vez com navegação PF7/PF8. A API deve implementar paginação server-side com `page` e `size`, substituindo o cursor DB2 por `OFFSET/LIMIT`.

**Critérios de Aceite:**
- [ ] `GET /api/portfolios/{portfolioId}/history` retorna histórico paginado
- [ ] Query params suportados: `from` (data início), `to` (data fim), `page` (default: 0), `size` (default: 10, máx: 100)
- [ ] Response body:
  ```json
  {
    "portfolioId": "PORT00001",
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15,
    "hasNext": true,
    "hasPrevious": false,
    "transactions": [
      {
        "transactionId": "...",
        "transactionDate": "YYYY-MM-DD",
        "transactionTime": "HH:MM:SS",
        "transactionType": "BU",
        "securityId": "...",
        "quantity": 0.000,
        "price": 0.000,
        "amount": 0.00,
        "fees": 0.00,
        "totalAmount": 0.00,
        "costBasis": 0.00,
        "gainLoss": 0.00
      }
    ]
  }
  ```
- [ ] Ordenação padrão: `TRANS_DATE DESC` (igual ao SQL do `INQHIST.cbl`)
- [ ] Suporte a filtro por `transactionType` (BU=Buy, SL=Sell, TR=Transfer, FE=Fee)
- [ ] HTTP 404 quando portfólio não encontrado
- [ ] Testes de integração com paginação

**Notas Técnicas:**
- `CURSMGR.cbl` gerencia: declare/open/fetch/close de cursores DB2; `WS-MAX-ROWS = 20` para array fetch
- `INQHIST.cbl` SQL: `SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC`
- Tabela `POSHIST` é particionada por trimestre — usar índice `POSHIST_PK` (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)

---

### Story EPIC-01-05 — Endpoint de Detalhes do Portfólio (Equivalente ao PORTMSTR)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar CRUD de portfólios via REST API (equivalente ao PORTMSTR.cbl)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-01-02  
**Mapeamento COBOL:**
- `PORTMSTR.cbl` → CRUD completo de portfólios (Create/Read/Update/Delete)
- `PORTREAD.cbl`, `PORTADD.cbl`, `PORTUPDT.cbl`, `PORTDEL.cbl` → endpoints REST correspondentes
- Tabela DB2: `PORTFOLIO_MASTER`

**Descrição:**  
Implementar os endpoints REST para gerenciamento de portfólios, substituindo o programa `PORTMSTR.cbl` que realiza operações CRUD no arquivo VSAM `PORTFOLIO-FILE`. Na API, as operações serão realizadas na tabela DB2 `PORTFOLIO_MASTER`.

**Critérios de Aceite:**
- [ ] `GET /api/portfolios/{portfolioId}` — busca portfólio por ID
- [ ] `GET /api/portfolios?clientId={clientId}&status={status}` — lista portfólios (usa índice `IDX_PORT_MASTER_CLIENT`)
- [ ] `POST /api/portfolios` — cria novo portfólio (validação: ID deve começar com 'PORT' + 5 dígitos, nome obrigatório, status deve ser A/I/C)
- [ ] `PUT /api/portfolios/{portfolioId}` — atualiza portfólio (registra em `AUDITLOG`)
- [ ] `DELETE /api/portfolios/{portfolioId}` — remove portfólio
- [ ] Validações equivalentes ao `2100-VALIDATE-PORTFOLIO` do `PORTMSTR.cbl`
- [ ] Audit log gravado no DB2 `AUDITLOG` para operações de escrita (equivalente ao `2100-LOG-PORTFOLIO-UPDATE`)
- [ ] HTTP 409 para ID duplicado (equivalente ao `PORT-DUP-KEY`)

---

### Story EPIC-01-06 — Middleware de Tratamento de Erros e Logging (Equivalente ao ERRHNDL)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar middleware de tratamento de erros e logging no backend (equivalente ao ERRHNDL.cbl)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2 dias)  
**Dependências:** EPIC-01-01  
**Mapeamento COBOL:**
- `ERRHNDL.cbl` → Middleware de error handling + logging no DB2 `ERRLOG`
- Tabela DB2: `ERRLOG` (ERROR_TIMESTAMP, PROGRAM_ID, ERROR_TYPE, ERROR_SEVERITY, ERROR_CODE, ERROR_MESSAGE, PROCESS_DATE, PROCESS_TIME, USER_ID, ADDITIONAL_INFO)

**Descrição:**  
Implementar o middleware centralizado de tratamento de erros que replica o comportamento do `ERRHNDL.cbl`. O programa original: (1) inicializa o handler, (2) loga o erro no DB2 `ERRLOG`, (3) formata a mensagem, (4) determina a ação (abend/continue/return). Na API, isso se traduz em um middleware Express/Spring que captura exceções, loga no DB2 e retorna respostas HTTP padronizadas.

**Critérios de Aceite:**
- [ ] Middleware global de error handling captura todas as exceções não tratadas
- [ ] Erros gravados na tabela DB2 `ERRLOG` com campos: `ERROR_TIMESTAMP`, `PROGRAM_ID` (nome do endpoint/controller), `ERROR_TYPE` (S/A/D), `ERROR_SEVERITY` (1=Info, 2=Warning, 3=Error, 4=Severe), `ERROR_CODE`, `ERROR_MESSAGE`, `USER_ID`
- [ ] `TRACE_ID` gerado automaticamente para rastreamento (equivalente ao `ERR-TRACE-ID` do `ERRHNDL.cbl`)
- [ ] Response de erro padronizada:
  ```json
  {
    "error": {
      "code": "ERR-001",
      "message": "...",
      "traceId": "...",
      "timestamp": "..."
    }
  }
  ```
- [ ] Severidades mapeadas: FATAL → HTTP 500, WARNING → HTTP 400/404, INFO → HTTP 200 com aviso
- [ ] Procedure `ERRLOG_CLEANUP` equivalente: endpoint admin `DELETE /api/admin/error-logs?olderThanDays={n}`
- [ ] Testes unitários para o middleware

---

### Story EPIC-01-07 — Testes de Integração e Documentação da API

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Testes de integração e documentação OpenAPI da REST API`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-01-03, EPIC-01-04, EPIC-01-05, EPIC-01-06

**Critérios de Aceite:**
- [ ] Testes de integração para todos os endpoints (cobertura mínima 80%)
- [ ] Documentação OpenAPI 3.0 completa em `/api/docs`
- [ ] Collection Postman/Insomnia exportada
- [ ] Testes de carga validando pool de 100 conexões DB2
- [ ] Pipeline CI/CD configurado (GitHub Actions)

---

---

# EPIC-02 — Frontend React: Componentes das Telas BMS

**Título Jira:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Frontend React — Migração das Telas BMS (MENMAP, POSMAP, HISMAP, ERRMAP)`  
**Fase:** 2 — Alta Prioridade  
**Objetivo:** Criar os componentes React que substituem as 4 telas BMS do arquivo `INQSET.bms` (`MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`), consumindo a REST API criada no EPIC-01.  
**Estimativa Total:** 47 SP  
**Dependências de entrada:** EPIC-01 (REST API deve estar disponível), EPIC-03 (Auth JWT)  
**Dependências de saída:** Nenhuma

---

### Story EPIC-02-01 — Setup do Projeto React e Estrutura Base

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Setup do projeto React com TypeScript, roteamento e cliente HTTP`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2-3 dias)  
**Dependências:** Nenhuma (pode ser desenvolvido em paralelo com EPIC-01)

**Descrição:**  
Criar a estrutura base do projeto React que substituirá as telas BMS do sistema CICS. O sistema atual usa telas de texto 24x80 com navegação por teclas PF; o React deve oferecer uma interface web moderna mantendo as mesmas funcionalidades.

**Critérios de Aceite:**
- [ ] Projeto criado com Vite + React + TypeScript
- [ ] Roteamento configurado com React Router v6: `/`, `/portfolio/:id/positions`, `/portfolio/:id/history`, `/login`
- [ ] Cliente HTTP configurado (Axios ou Fetch API) com base URL da REST API e interceptors para JWT
- [ ] Gerenciamento de estado configurado (React Context ou Zustand)
- [ ] Biblioteca de componentes UI configurada (Material UI, Ant Design ou Chakra UI)
- [ ] Variáveis de ambiente: `VITE_API_BASE_URL`
- [ ] ESLint + Prettier configurados
- [ ] Estrutura de pastas: `src/components/`, `src/pages/`, `src/services/`, `src/hooks/`, `src/types/`

---

### Story EPIC-02-02 — Componente MainMenu (Equivalente ao MENMAP)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar componente MainMenu.tsx (equivalente ao MENMAP do INQSET.bms)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2 dias)  
**Dependências:** EPIC-02-01  
**Mapeamento BMS:**
- `MENMAP` (INQSET.bms, linha 7-19) → `MainMenu.tsx`
- Campos BMS: `OPTION` (input numérico), `ERRMSG` (mensagem de erro em vermelho)
- Opções: 1=Portfolio Position Inquiry, 2=Transaction History, 3=Exit

**Descrição:**  
Criar o componente React `MainMenu.tsx` que substitui a tela `MENMAP`. A tela original é um menu de texto 24x80 com 3 opções numéricas. O componente React deve oferecer uma interface moderna com as mesmas 3 funcionalidades, mantendo a navegação equivalente.

**Critérios de Aceite:**
- [ ] Componente `MainMenu.tsx` criado em `src/pages/MainMenu.tsx`
- [ ] Exibe título "Portfolio Management System" (equivalente ao campo `INITIAL='Portfolio Management System'` do MENMAP)
- [ ] 3 opções de navegação: "Portfolio Position Inquiry" → `/portfolio/:id/positions`, "Transaction History" → `/portfolio/:id/history`, "Exit/Logout" → `/login`
- [ ] Input para Account/Portfolio ID antes de navegar para Position ou History
- [ ] Exibição de mensagens de erro (equivalente ao campo `ERRMSG` em vermelho no MENMAP)
- [ ] Responsivo para desktop e tablet
- [ ] Testes unitários com React Testing Library

---

### Story EPIC-02-03 — Componente PortfolioView (Equivalente ao POSMAP)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar componente PortfolioView.tsx (equivalente ao POSMAP do INQSET.bms)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 13 SP (~5-6 dias)  
**Dependências:** EPIC-02-01, EPIC-01-03  
**Mapeamento BMS:**
- `POSMAP` (INQSET.bms, linhas 23-49) → `PortfolioView.tsx`
- Campos BMS: `ACCTIN` (input account), `FUNDOUT` (Fund ID), `NAMEOUT` (Fund Name), `UNITOUT` (Units), `COSTOUT` (Cost Basis), `VALOUT` (Market Value), `POSMSG` (mensagem de erro)
- Navegação: PF3=Exit, PF7=Previous, PF8=Next → botões React

**Descrição:**  
Criar o componente React `PortfolioView.tsx` que substitui a tela `POSMAP`. A tela original exibe posições de portfólio com campos: Account, Fund ID, Fund Name, Units, Cost Basis, Market Value. O componente deve consumir `GET /api/portfolios/{id}/positions` e exibir os dados em uma tabela moderna.

**Critérios de Aceite:**
- [ ] Componente `PortfolioView.tsx` criado em `src/pages/PortfolioView.tsx`
- [ ] Input de Account/Portfolio ID com busca ao submeter
- [ ] Tabela exibindo: Fund ID, Fund Name, Units (Quantity), Cost Basis, Market Value, Currency, Position Date
- [ ] Formatação monetária para Cost Basis e Market Value (equivalente ao `COLOR=TURQUOISE` dos campos de dados no POSMAP)
- [ ] Botões de navegação equivalentes às teclas PF: "← Anterior" (PF7), "Próximo →" (PF8), "Voltar ao Menu" (PF3)
- [ ] Loading state durante chamada à API
- [ ] Exibição de erro quando portfólio não encontrado (equivalente ao `POSMSG` em vermelho)
- [ ] Filtro por data de posição
- [ ] Testes unitários e de integração

---

### Story EPIC-02-04 — Componente HistoryView (Equivalente ao HISMAP)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar componente HistoryView.tsx (equivalente ao HISMAP do INQSET.bms)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 13 SP (~5-6 dias)  
**Dependências:** EPIC-02-01, EPIC-01-04  
**Mapeamento BMS:**
- `HISMAP` (INQSET.bms, linhas 53-85) → `HistoryView.tsx`
- Campos BMS: `HISAIN` (input account), `ROW1..ROW10` (10 linhas de dados), `HISMSG` (mensagem de erro)
- Colunas: Date, Type, Units, Price, Amount
- Navegação: PF3=Exit, PF7=Previous, PF8=Next → paginação React

**Descrição:**  
Criar o componente React `HistoryView.tsx` que substitui a tela `HISMAP`. A tela original exibe 10 linhas de histórico de transações por vez com navegação PF7/PF8. O componente deve consumir `GET /api/portfolios/{id}/history` com paginação server-side.

**Critérios de Aceite:**
- [ ] Componente `HistoryView.tsx` criado em `src/pages/HistoryView.tsx`
- [ ] Input de Account/Portfolio ID com busca ao submeter
- [ ] Tabela com colunas: Date, Type (BU/SL/TR/FE), Units (Quantity), Price, Amount, Fees, Total Amount, Cost Basis, Gain/Loss
- [ ] Paginação com 10 itens por página (equivalente às 10 linhas ROW1..ROW10 do HISMAP)
- [ ] Botões "← Anterior" (PF7) e "Próximo →" (PF8) para navegação entre páginas
- [ ] Filtros de data: `from` e `to` (date pickers)
- [ ] Filtro por tipo de transação (BU=Buy, SL=Sell, TR=Transfer, FE=Fee)
- [ ] Loading state durante chamada à API
- [ ] Exibição de erro (equivalente ao `HISMSG` em vermelho)
- [ ] Testes unitários e de integração

---

### Story EPIC-02-05 — Componente ErrorDisplay (Equivalente ao ERRMAP)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar componente ErrorDisplay.tsx e sistema de notificações (equivalente ao ERRMAP do INQSET.bms)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2 dias)  
**Dependências:** EPIC-02-01  
**Mapeamento BMS:**
- `ERRMAP` (INQSET.bms, linhas 89-99) → `ErrorDisplay.tsx` + sistema de toasts/notificações
- Campos BMS: `ERRCOUT` (Error Code em vermelho), `ERRDOUT` (Details em vermelho)
- Navegação: "Press ENTER to continue" → botão "OK" ou auto-dismiss

**Descrição:**  
Criar o componente `ErrorDisplay.tsx` e um sistema de notificações/toasts que substitui a tela `ERRMAP`. A tela original é uma tela de erro dedicada com Error Code e Details. No React, erros devem ser exibidos como toasts não-bloqueantes ou modais, dependendo da severidade.

**Critérios de Aceite:**
- [ ] Componente `ErrorDisplay.tsx` para erros fatais (modal bloqueante com Error Code + Details)
- [ ] Sistema de toasts para erros não-fatais (equivalente ao `ERR-WARNING` do `ERRHNDL.cbl`)
- [ ] Exibição de `traceId` para suporte (equivalente ao `ERR-TRACE-ID`)
- [ ] Severidades visuais: FATAL=vermelho/modal, WARNING=amarelo/toast, INFO=azul/toast
- [ ] Botão "OK" / auto-dismiss após 5s para warnings/info (equivalente ao "Press ENTER to continue")
- [ ] Hook `useErrorHandler()` para uso em outros componentes
- [ ] Testes unitários

---

### Story EPIC-02-06 — Integração Completa e Testes E2E

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Integração completa do frontend React com a REST API e testes E2E`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 6 SP (~2-3 dias)  
**Dependências:** EPIC-02-02, EPIC-02-03, EPIC-02-04, EPIC-02-05, EPIC-03-01

**Critérios de Aceite:**
- [ ] Fluxo completo: Login → Menu → Portfolio View → History View → Logout
- [ ] Testes E2E com Cypress ou Playwright cobrindo os 3 fluxos principais
- [ ] Build de produção funcionando (`npm run build`)
- [ ] Deploy em ambiente de staging

---

---

# EPIC-03 — Autenticação JWT: Substituição do SECMGR/RACF

**Título Jira:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Autenticação JWT/OAuth2 — Substituição do SECMGR.cbl e RACF`  
**Fase:** 3 — Alta Prioridade  
**Objetivo:** Implementar autenticação JWT substituindo o `SECMGR.cbl` (validação de usuário via RACF, autorização via DB2 `AUTHFILE`, audit log via `AUDITLOG`).  
**Estimativa Total:** 34 SP  
**Dependências de entrada:** EPIC-01-01 (backend base)  
**Dependências de saída:** EPIC-02 (frontend precisa de auth)

---

### Story EPIC-03-01 — Endpoint de Login e Geração de JWT (Equivalente ao SECMGR — SEC-VALIDATE)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar POST /api/auth/login com JWT (equivalente ao SECMGR.cbl — P100-VALIDATE-USER)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-01-01  
**Mapeamento COBOL:**
- `SECMGR.cbl` — `P100-VALIDATE-USER` (SEC-VALIDATE 'V') → `POST /api/auth/login`
- `SECMGR.cbl` — `P200-CHECK-AUTH` (SEC-AUTHORIZE 'A') → Middleware JWT de autorização
- `SECMGR.cbl` — `P300-LOG-ACCESS` (SEC-AUDIT 'L') → Audit log no DB2 `AUDITLOG`

**Descrição:**  
Implementar o endpoint de autenticação que substitui o `SECMGR.cbl`. O programa original valida usuário via `EXEC CICS ASSIGN USERID` (RACF) e verifica autorização na tabela DB2 `AUTHFILE`. A API deve implementar autenticação JWT com validação de credenciais e geração de token.

**Critérios de Aceite:**
- [ ] `POST /api/auth/login` aceita `{ "username": "...", "password": "..." }`
- [ ] Retorna JWT token com claims: `sub` (userId), `roles`, `exp` (expiração)
- [ ] JWT secret configurável via variável de ambiente `JWT_SECRET`
- [ ] Token expira em 8 horas (equivalente a uma sessão de trabalho)
- [ ] HTTP 401 para credenciais inválidas (equivalente ao `SEC-RESPONSE-CODE = 8` do `SECMGR.cbl`)
- [ ] HTTP 403 para usuário sem autorização (equivalente ao `'Access denied'` do `P200-CHECK-AUTH`)
- [ ] `POST /api/auth/logout` invalida o token (blacklist ou short-lived tokens)
- [ ] `POST /api/auth/refresh` para renovação de token
- [ ] Testes unitários

---

### Story EPIC-03-02 — Middleware de Autorização JWT (Equivalente ao SECMGR — SEC-AUTHORIZE)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar middleware de autorização JWT por recurso (equivalente ao SECMGR.cbl — P200-CHECK-AUTH)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-03-01  
**Mapeamento COBOL:**
- `SECMGR.cbl` — `P200-CHECK-AUTH`: `SELECT COUNT(*) FROM AUTHFILE WHERE USER_ID = ? AND RESOURCE = ? AND ACCESS_TYPE = ?`

**Descrição:**  
Implementar o middleware de autorização que verifica se o usuário autenticado tem permissão para acessar um recurso específico. O `SECMGR.cbl` consulta a tabela `AUTHFILE` com `USER_ID`, `RESOURCE_NAME` e `ACCESS_TYPE`. Na API, isso se traduz em um middleware JWT que valida roles/permissões por endpoint.

**Critérios de Aceite:**
- [ ] Middleware `authMiddleware` aplicado a todos os endpoints protegidos
- [ ] Validação do JWT em cada request (header `Authorization: Bearer <token>`)
- [ ] Mapeamento de roles: `ADMIN` (acesso total), `VIEWER` (somente leitura), `ANALYST` (leitura + relatórios)
- [ ] HTTP 401 para token ausente ou inválido
- [ ] HTTP 403 para token válido mas sem permissão no recurso
- [ ] Tabela `AUTHFILE` (ou equivalente) mantida no DB2 para compatibilidade
- [ ] Testes unitários para cada cenário de autorização

---

### Story EPIC-03-03 — Audit Log de Acesso (Equivalente ao SECMGR — SEC-AUDIT)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar audit log de acesso no DB2 AUDITLOG (equivalente ao SECMGR.cbl — P300-LOG-ACCESS)`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2 dias)  
**Dependências:** EPIC-03-01  
**Mapeamento COBOL:**
- `SECMGR.cbl` — `P300-LOG-ACCESS`: `INSERT INTO AUDITLOG (TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE)`

**Descrição:**  
Implementar o audit log de acesso que replica o `P300-LOG-ACCESS` do `SECMGR.cbl`. O programa original insere em `AUDITLOG` a cada acesso: timestamp, user_id, terminal_id, trans_id, program, access_type. Na API, isso deve ser feito via middleware que loga cada request autenticado.

**Critérios de Aceite:**
- [ ] Middleware de audit log registra cada request autenticado na tabela `AUDITLOG` do DB2
- [ ] Campos gravados: `TIMESTAMP`, `USER_ID`, `TERMINAL_ID` (IP do cliente), `TRANS_ID` (request ID), `PROGRAM` (endpoint), `ACCESS_TYPE` (GET/POST/PUT/DELETE)
- [ ] A tabela `AUDITLOG` existente no DB2 é mantida (sem migração de dados)
- [ ] `GET /api/admin/audit-logs` para consulta dos logs (somente role ADMIN)
- [ ] Testes unitários

---

### Story EPIC-03-04 — Componente de Login React

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar componente LoginPage.tsx para autenticação JWT no frontend React`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 5 SP (~2 dias)  
**Dependências:** EPIC-03-01, EPIC-02-01

**Critérios de Aceite:**
- [ ] Componente `LoginPage.tsx` com formulário de username/password
- [ ] Integração com `POST /api/auth/login`
- [ ] Armazenamento seguro do JWT (httpOnly cookie ou sessionStorage)
- [ ] Redirect automático para `/` após login bem-sucedido
- [ ] Redirect para `/login` quando token expirado (interceptor HTTP)
- [ ] Mensagem de erro para credenciais inválidas
- [ ] Testes unitários

---

### Story EPIC-03-05 — Testes de Segurança e Documentação

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Testes de segurança e documentação do sistema de autenticação JWT`  
**Tipo:** Story  
**Prioridade:** Alta  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-03-01, EPIC-03-02, EPIC-03-03, EPIC-03-04

**Critérios de Aceite:**
- [ ] Testes de penetração básicos (OWASP Top 10)
- [ ] Validação de expiração de token
- [ ] Testes de SQL injection nos endpoints de auth
- [ ] Documentação do fluxo de autenticação
- [ ] Rotação de JWT secret documentada

---

---

# EPIC-04 — Batch Jobs Modernizados: Pipeline de Processamento

**Título Jira:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Batch Jobs Modernizados — Pipeline TRNVAL00 → POSUPD00 → HISTLD00 como Cron Jobs`  
**Fase:** 4 — Média Prioridade  
**Objetivo:** Converter o pipeline batch COBOL (`TRNVAL00 → POSUPD00 → HISTLD00`) em jobs agendados modernos (cron jobs, AWS Lambda Scheduled, ou similar), mantendo a lógica de negócio e o DB2 como destino dos dados.  
**Estimativa Total:** 28 SP  
**Dependências de entrada:** EPIC-01-02 (pool de conexões DB2)  
**Dependências de saída:** EPIC-05 (dashboards consomem dados processados pelo batch)

**Nota importante:** O pipeline batch **não migra para React** — o React apenas consome os dados já processados via API. O batch continua sendo um processo separado executado no servidor.

---

### Story EPIC-04-01 — Job de Validação de Transações (Equivalente ao TRNVAL00)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar job de validação de transações (equivalente ao TRNVAL00.cbl)`  
**Tipo:** Story  
**Prioridade:** Média  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-01-02  
**Mapeamento COBOL:**
- `TRNVAL00` → Job de validação de transações (primeiro passo do pipeline)
- `BCHCTL00.cbl` → Controle de execução do batch (INIT/CHEK/UPDT/TERM)

**Critérios de Aceite:**
- [ ] Job implementado como script Node.js/Python ou Java com agendamento cron
- [ ] Lê transações pendentes da tabela `TRANSACTION_HISTORY` (status='P')
- [ ] Valida campos obrigatórios: `TRANSACTION_ID`, `PORTFOLIO_ID`, `TRANSACTION_DATE`, `INVESTMENT_ID`, `TRANSACTION_TYPE` (BU/SL/TR/FE), `QUANTITY`, `PRICE`, `AMOUNT`
- [ ] Transações inválidas marcadas com status='F' e motivo registrado
- [ ] Checkpoint a cada 1000 registros (equivalente ao `WS-COMMIT-THRESHOLD = 1000` do `HISTLD00.cbl`)
- [ ] Logs de execução: registros lidos, validados, rejeitados
- [ ] Controle de execução: não executa se job anterior ainda estiver rodando (equivalente ao `BCHCTL00.cbl`)
- [ ] Agendamento: diário às 02:00 UTC (configurável)

---

### Story EPIC-04-02 — Job de Atualização de Posições (Equivalente ao POSUPD00)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar job de atualização de posições (equivalente ao POSUPD00/POSUPDT.cbl)`  
**Tipo:** Story  
**Prioridade:** Média  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-04-01

**Critérios de Aceite:**
- [ ] Job executa após `TRNVAL00` (dependência de sequência)
- [ ] Processa transações validadas (status='P') e atualiza `INVESTMENT_POSITIONS`
- [ ] Calcula `MARKET_VALUE` e `COST_BASIS` atualizados
- [ ] Commit a cada 1000 registros com checkpoint
- [ ] Rollback em caso de erro (equivalente ao `ROLLBACK WORK` do `HISTLD00.cbl`)
- [ ] Logs de execução detalhados

---

### Story EPIC-04-03 — Job de Carga de Histórico no DB2 (Equivalente ao HISTLD00)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar job de carga de histórico no DB2 (equivalente ao HISTLD00.cbl)`  
**Tipo:** Story  
**Prioridade:** Média  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-04-02  
**Mapeamento COBOL:**
- `HISTLD00.cbl` → Job de carga de histórico (terceiro passo do pipeline)
- Campos mapeados: `TH-ACCOUNT-NO` → `PH-ACCOUNT-NO`, `TH-PORTFOLIO-ID` → `PH-PORTFOLIO-ID`, `TH-TRANS-DATE` → `PH-TRANS-DATE`, etc.
- Tabela destino: `POSHIST` (particionada por trimestre)

**Critérios de Aceite:**
- [ ] Job executa após `POSUPD00` (dependência de sequência)
- [ ] Carrega registros processados na tabela `POSHIST` do DB2
- [ ] Trata duplicatas: `SQLCODE = -803` → ignora (equivalente ao `IF SQLCODE = -803 CONTINUE` do `HISTLD00.cbl`)
- [ ] Commit a cada 1000 registros (`WS-COMMIT-THRESHOLD = 1000`)
- [ ] Checkpoint salvo após cada commit (equivalente ao `2310-UPDATE-CHECKPOINT`)
- [ ] Exibe estatísticas ao final: registros lidos, escritos, erros (equivalente ao `3400-DISPLAY-STATS`)
- [ ] Máximo de 100 erros antes de abortar (equivalente ao `WS-ERROR-COUNT > 100`)

---

### Story EPIC-04-04 — Orquestração do Pipeline e Monitoramento

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar orquestração do pipeline batch e endpoint de monitoramento`  
**Tipo:** Story  
**Prioridade:** Média  
**Story Points:** 4 SP (~1-2 dias)  
**Dependências:** EPIC-04-01, EPIC-04-02, EPIC-04-03

**Critérios de Aceite:**
- [ ] Orquestrador garante sequência: `TRNVAL00 → POSUPD00 → HISTLD00`
- [ ] Falha em qualquer etapa interrompe o pipeline (sem executar etapas seguintes)
- [ ] `GET /api/admin/batch/status` retorna status da última execução de cada job
- [ ] Alertas por email/Slack em caso de falha
- [ ] Logs centralizados com correlação entre os 3 jobs

---

---

# EPIC-05 — Dashboards React: Migração dos Relatórios

**Título Jira:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Dashboards React — Migração dos Relatórios RPTPOS00, RPTAUD00, RPTSTA00`  
**Fase:** 5 — Baixa Prioridade  
**Objetivo:** Migrar os 3 relatórios batch COBOL (`RPTPOS00`, `RPTAUD00`, `RPTSTA00`) para dashboards interativos no React, consumindo dados via REST API.  
**Estimativa Total:** 35 SP  
**Dependências de entrada:** EPIC-01 (REST API), EPIC-04 (dados processados pelo batch)  
**Dependências de saída:** Nenhuma

---

### Story EPIC-05-01 — Endpoints de Relatórios na REST API

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Implementar endpoints de relatórios na REST API (equivalente ao RPTPOS00, RPTAUD00, RPTSTA00)`  
**Tipo:** Story  
**Prioridade:** Baixa  
**Story Points:** 10 SP (~4-5 dias)  
**Dependências:** EPIC-01-03, EPIC-01-04  
**Mapeamento COBOL:**
- `RPTPOS00.cbl` → `GET /api/reports/positions` — Relatório diário de posições (portfolio summary, transaction activity, exceptions, performance metrics)
- `RPTAUD00.cbl` → `GET /api/reports/audit` — Relatório de auditoria (security audit trails, process audit, error summary, control verification)
- `RPTSTA00.cbl` → `GET /api/reports/statistics` — Relatório de estatísticas do sistema (DB2 calls, elapsed time, CPU, batch jobs success rate)

**Critérios de Aceite:**
- [ ] `GET /api/reports/positions?date=YYYY-MM-DD` — dados do relatório diário de posições
  - Portfolio position summary (equivalente ao `WS-POSITION-DETAIL` do `RPTPOS00.cbl`)
  - Campos: `portfolioId`, `description`, `quantity`, `currentValue`, `changePercent`
- [ ] `GET /api/reports/audit?from=&to=` — dados do relatório de auditoria
  - Audit trail (equivalente ao `WS-AUDIT-DETAIL` do `RPTAUD00.cbl`)
  - Error log summary (equivalente ao `WS-ERROR-DETAIL`)
- [ ] `GET /api/reports/statistics?from=&to=` — dados de estatísticas do sistema
  - DB2 metrics: calls, elapsed, CPU, wait (equivalente ao `WS-DB2-METRICS` do `RPTSTA00.cbl`)
  - Batch metrics: jobs, success rate, elapsed (equivalente ao `WS-BATCH-METRICS`)
- [ ] Suporte a export CSV/Excel (equivalente ao relatório de 132 colunas dos programas COBOL)
- [ ] Testes de integração

---

### Story EPIC-05-02 — Dashboard de Posições (Equivalente ao RPTPOS00)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar dashboard PositionsDashboard.tsx (equivalente ao RPTPOS00.cbl)`  
**Tipo:** Story  
**Prioridade:** Baixa  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-05-01, EPIC-02-01  
**Mapeamento COBOL:**
- `RPTPOS00.cbl` → `PositionsDashboard.tsx`
- Seções: Portfolio Position Summary, Transaction Activity, Exception Reporting, Performance Metrics

**Critérios de Aceite:**
- [ ] Componente `PositionsDashboard.tsx` em `src/pages/PositionsDashboard.tsx`
- [ ] Tabela de posições com: Portfolio ID, Description, Quantity, Market Value, Change % (equivalente ao `WS-POSITION-DETAIL`)
- [ ] Gráfico de barras/linha para variação de valor ao longo do tempo
- [ ] Seção de exceções (posições com variação > threshold configurável)
- [ ] Seletor de data para relatório diário
- [ ] Botão de export CSV/Excel
- [ ] Testes unitários

---

### Story EPIC-05-03 — Dashboard de Auditoria (Equivalente ao RPTAUD00)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar dashboard AuditDashboard.tsx (equivalente ao RPTAUD00.cbl)`  
**Tipo:** Story  
**Prioridade:** Baixa  
**Story Points:** 8 SP (~3-4 dias)  
**Dependências:** EPIC-05-01, EPIC-02-01  
**Mapeamento COBOL:**
- `RPTAUD00.cbl` → `AuditDashboard.tsx`
- Seções: Security Audit Trail, Process Audit, Error Summary, Control Verification

**Critérios de Aceite:**
- [ ] Componente `AuditDashboard.tsx` em `src/pages/AuditDashboard.tsx`
- [ ] Tabela de audit trail: Timestamp, Program, Type, Message (equivalente ao `WS-AUDIT-DETAIL`)
- [ ] Tabela de error log: Timestamp, Program, Error Code, Message (equivalente ao `WS-ERROR-DETAIL`)
- [ ] Filtros por período, programa, tipo de evento
- [ ] Gráfico de erros por severidade ao longo do tempo
- [ ] Somente acessível para role ADMIN
- [ ] Export CSV/Excel

---

### Story EPIC-05-04 — Dashboard de Estatísticas do Sistema (Equivalente ao RPTSTA00)

**Título:** `[COG-GTM/COBOL-Legacy-Benchmark-Suite] Criar dashboard SystemStatsDashboard.tsx (equivalente ao RPTSTA00.cbl)`  
**Tipo:** Story  
**Prioridade:** Baixa  
**Story Points:** 9 SP (~3-4 dias)  
**Dependências:** EPIC-05-01, EPIC-02-01  
**Mapeamento COBOL:**
- `RPTSTA00.cbl` → `SystemStatsDashboard.tsx`
- Métricas DB2: calls, elapsed, CPU, wait (equivalente ao `WS-DB2-METRICS`)
- Métricas Batch: jobs, success rate, elapsed (equivalente ao `WS-BATCH-METRICS`)

**Critérios de Aceite:**
- [ ] Componente `SystemStatsDashboard.tsx` em `src/pages/SystemStatsDashboard.tsx`
- [ ] Cards de métricas DB2: Total Calls, Avg Response Time, CPU Usage, Wait Time
- [ ] Cards de métricas Batch: Total Jobs, Success Rate %, Failed Jobs, Avg Elapsed Time
- [ ] Gráficos de tendência (trend analysis — equivalente ao `2430-WRITE-TREND-ANALYSIS` do `RPTSTA00.cbl`)
- [ ] Atualização automática a cada 5 minutos
- [ ] Somente acessível para role ADMIN/ANALYST

---

---

## Resumo de Estimativas por Epic

| Epic | Stories | Story Points | Estimativa (dias úteis) |
|------|---------|-------------|------------------------|
| EPIC-01 — REST API Backend | 7 | 55 SP | ~22-28 dias |
| EPIC-02 — Frontend React | 6 | 47 SP | ~19-24 dias |
| EPIC-03 — Auth JWT | 5 | 34 SP | ~14-18 dias |
| EPIC-04 — Batch Jobs | 4 | 28 SP | ~11-14 dias |
| EPIC-05 — Dashboards React | 4 | 35 SP | ~14-18 dias |
| **TOTAL** | **26** | **199 SP** | **~80-102 dias úteis** |

---

## Cronograma Sugerido (Sprints de 2 semanas)

| Sprint | Epics/Stories | Foco |
|--------|--------------|------|
| Sprint 1 | EPIC-01-01, EPIC-01-02, EPIC-01-06, EPIC-02-01 | Setup backend + frontend + pool DB2 |
| Sprint 2 | EPIC-01-03, EPIC-01-04, EPIC-03-01 | Endpoints principais + Auth JWT |
| Sprint 3 | EPIC-01-05, EPIC-01-07, EPIC-03-02, EPIC-03-03 | CRUD portfólios + Middleware auth + Audit log |
| Sprint 4 | EPIC-02-02, EPIC-02-03, EPIC-03-04, EPIC-03-05 | Componentes React + Login |
| Sprint 5 | EPIC-02-04, EPIC-02-05, EPIC-02-06 | History + Error + Integração E2E |
| Sprint 6 | EPIC-04-01, EPIC-04-02, EPIC-04-03, EPIC-04-04 | Pipeline batch modernizado |
| Sprint 7 | EPIC-05-01, EPIC-05-02 | Endpoints relatórios + Dashboard posições |
| Sprint 8 | EPIC-05-03, EPIC-05-04 | Dashboards auditoria + estatísticas |

**Duração total estimada:** ~16 semanas (4 meses) com 1 equipe de 3-4 desenvolvedores

---

## Mapeamento Completo COBOL → Moderno

| Componente COBOL | Arquivo | Equivalente Moderno | Epic/Story |
|-----------------|---------|---------------------|------------|
| `MENMAP` (BMS) | `INQSET.bms` | `MainMenu.tsx` | EPIC-02-02 |
| `POSMAP` (BMS) | `INQSET.bms` | `PortfolioView.tsx` | EPIC-02-03 |
| `HISMAP` (BMS) | `INQSET.bms` | `HistoryView.tsx` | EPIC-02-04 |
| `ERRMAP` (BMS) | `INQSET.bms` | `ErrorDisplay.tsx` + toasts | EPIC-02-05 |
| `INQONLN.cbl` | online/ | React Router + App.tsx | EPIC-02-01 |
| `INQPORT.cbl` | online/ | `GET /api/portfolios/{id}/positions` | EPIC-01-03 |
| `INQHIST.cbl` | online/ | `GET /api/portfolios/{id}/history` | EPIC-01-04 |
| `SECMGR.cbl` (P100) | online/ | `POST /api/auth/login` (JWT) | EPIC-03-01 |
| `SECMGR.cbl` (P200) | online/ | Middleware JWT de autorização | EPIC-03-02 |
| `SECMGR.cbl` (P300) | online/ | Middleware de audit log | EPIC-03-03 |
| `CURSMGR.cbl` | online/ | Paginação server-side (OFFSET/LIMIT) | EPIC-01-04 |
| `ERRHNDL.cbl` | online/ | Middleware de error handling + ERRLOG | EPIC-01-06 |
| `DB2ONLN.cbl` | online/ | Pool de conexões DB2 (max=100, timeout=300s) | EPIC-01-02 |
| `DB2RECV.cbl` | online/ | Retry logic (3x, intervalo 2s) | EPIC-01-02 |
| `PORTMSTR.cbl` | portfolio/ | `CRUD /api/portfolios` | EPIC-01-05 |
| `TRNVAL00` | batch/ | Cron job de validação | EPIC-04-01 |
| `POSUPD00/POSUPDT.cbl` | batch/ | Cron job de atualização de posições | EPIC-04-02 |
| `HISTLD00.cbl` | batch/ | Cron job de carga no DB2 | EPIC-04-03 |
| `BCHCTL00.cbl` | batch/ | Orquestrador de pipeline | EPIC-04-04 |
| `RPTPOS00.cbl` | batch/ | `PositionsDashboard.tsx` + endpoint | EPIC-05-01/02 |
| `RPTAUD00.cbl` | batch/ | `AuditDashboard.tsx` + endpoint | EPIC-05-01/03 |
| `RPTSTA00.cbl` | batch/ | `SystemStatsDashboard.tsx` + endpoint | EPIC-05-01/04 |
| `UTLMNT00.cbl` | utility/ | Endpoints admin de manutenção | Backlog |
| `UTLMON00.cbl` | utility/ | Endpoints admin de monitoramento | Backlog |
| `UTLVAL00.cbl` | utility/ | Validações reutilizáveis no backend | Backlog |

---

## Mapeamento de Endpoints REST

| Programa COBOL | Endpoint REST | Método | Auth |
|---------------|--------------|--------|------|
| `INQPORT.cbl` | `/api/portfolios/{id}/positions` | GET | JWT |
| `INQHIST.cbl` | `/api/portfolios/{id}/history?from=&to=&page=&size=` | GET | JWT |
| `SECMGR.cbl` | `/api/auth/login` | POST | Público |
| `SECMGR.cbl` | `/api/auth/logout` | POST | JWT |
| `SECMGR.cbl` | `/api/auth/refresh` | POST | JWT |
| `PORTMSTR.cbl` | `/api/portfolios` | GET, POST | JWT |
| `PORTMSTR.cbl` | `/api/portfolios/{id}` | GET, PUT, DELETE | JWT |
| `ERRHNDL.cbl` | `/api/admin/error-logs` | GET, DELETE | JWT (ADMIN) |
| `SECMGR.cbl` (audit) | `/api/admin/audit-logs` | GET | JWT (ADMIN) |
| `DB2ONLN.cbl` | `/api/health/db` | GET | JWT (ADMIN) |
| `BCHCTL00.cbl` | `/api/admin/batch/status` | GET | JWT (ADMIN) |
| `RPTPOS00.cbl` | `/api/reports/positions` | GET | JWT |
| `RPTAUD00.cbl` | `/api/reports/audit` | GET | JWT (ADMIN) |
| `RPTSTA00.cbl` | `/api/reports/statistics` | GET | JWT (ADMIN/ANALYST) |

---

## Tabelas DB2 Mantidas (Sem Migração Imediata)

| Tabela DB2 | Usada por | Observação |
|-----------|----------|------------|
| `PORTFOLIO_MASTER` | EPIC-01-03, EPIC-01-05 | Mantida; API é camada de abstração |
| `INVESTMENT_POSITIONS` | EPIC-01-03 | Mantida |
| `TRANSACTION_HISTORY` | EPIC-01-04, EPIC-04-01 | Mantida |
| `POSHIST` | EPIC-01-04, EPIC-04-03 | Mantida; particionada por trimestre |
| `AUDITLOG` | EPIC-03-03 | Mantida; continua sendo populada |
| `ERRLOG` | EPIC-01-06 | Mantida; continua sendo populada |
| `AUTHFILE` | EPIC-03-02 | Mantida para compatibilidade |
| `RTNCODES` | EPIC-04 | Mantida para batch jobs |

---

*Documento gerado em 2026-02-25 | Devin Session: https://app.devin.ai/sessions/e4855e1c8a3e46f7b4d39b498b7d9a3d*
