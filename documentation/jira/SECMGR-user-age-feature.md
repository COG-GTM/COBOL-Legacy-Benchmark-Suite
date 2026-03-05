# Feature: Retornar idade do usuário na validação do SECMGR

**Assignee (todos os itens):** Shawn

---

## Epic 1: Criação da estrutura de banco de dados (DB2)

**Título:** [EPIC] Criar tabela USERINFO no DB2 para dados de usuário  
**Assignee:** Shawn  
**Descrição:** Criar a tabela USERINFO no DB2 contendo USER_ID, USER_NAME, BIRTH_DATE e LAST_MAINT_DATE. Inclui criação de índice único e concessão de permissões para o aplicativo POSAPP.  
**Critérios de aceite:**
- Tabela USERINFO criada com DDL em `src/database/db2/USERINFO.sql`
- Índice único USERINFO_PK criado sobre USER_ID
- Permissão SELECT concedida para POSAPP
- Script de DDL validado e executado com sucesso no ambiente de desenvolvimento

**Story 1.1:** Criar DDL da tabela USERINFO  
- **Assignee:** Shawn  
- **Story Points:** 2  
- **Descrição:** Criar o arquivo `src/database/db2/USERINFO.sql` com CREATE TABLE, CREATE INDEX e GRANT seguindo o padrão existente (ex: ERRLOG.sql, POSHIST.sql).

**Story 1.2:** Validar e executar DDL no ambiente de desenvolvimento  
- **Assignee:** Shawn  
- **Story Points:** 1  
- **Descrição:** Executar o DDL no ambiente DB2 de desenvolvimento e validar que a tabela foi criada corretamente. Inserir dados de teste.

---

## Epic 2: Modificação do programa SECMGR

**Título:** [EPIC] Adicionar consulta de idade do usuário no SECMGR  
**Assignee:** Shawn  
**Descrição:** Modificar o programa `src/programs/online/SECMGR.cbl` para que, ao validar um usuário (operação 'V'), também consulte a tabela USERINFO, calcule a idade a partir da data de nascimento e retorne no campo SEC-USER-AGE da COMMAREA.  
**Critérios de aceite:**
- Campo SEC-USER-AGE adicionado à SECURITY-REQUEST-AREA na LINKAGE SECTION
- Variáveis de working-storage criadas para cálculo de idade
- Novo parágrafo P150-GET-USER-AGE implementado com SELECT na USERINFO e cálculo de idade
- Parágrafo P100-VALIDATE-USER modificado para chamar P150-GET-USER-AGE após validação bem-sucedida
- Tratamento de erros: SQLCODE 100 (usuário não encontrado na USERINFO) e erros genéricos tratados sem interromper a validação

**Story 2.1:** Adicionar campo SEC-USER-AGE à COMMAREA (LINKAGE SECTION)  
- **Assignee:** Shawn  
- **Story Points:** 1  
- **Descrição:** Adicionar `SEC-USER-AGE PIC S9(3) COMP` após SEC-ERROR-INFO na estrutura SECURITY-REQUEST-AREA.

**Story 2.2:** Adicionar variáveis de working-storage para cálculo de idade  
- **Assignee:** Shawn  
- **Story Points:** 1  
- **Descrição:** Criar área WS-USER-INFO-AREA com campos WS-BIRTH-DATE, WS-CURRENT-DATE-INT, WS-BIRTH-DATE-INT e WS-CALC-AGE.

**Story 2.3:** Implementar parágrafo P150-GET-USER-AGE  
- **Assignee:** Shawn  
- **Story Points:** 3  
- **Descrição:** Criar novo parágrafo que faz SELECT na USERINFO por USER_ID, calcula a idade usando método (YYYYMMDD_atual - YYYYMMDD_nascimento) / 10000, e move o resultado para SEC-USER-AGE. Tratar SQLCODE 0, 100 e OTHER.

**Story 2.4:** Modificar P100-VALIDATE-USER para chamar P150-GET-USER-AGE  
- **Assignee:** Shawn  
- **Story Points:** 1  
- **Descrição:** Após a validação bem-sucedida (MOVE 0 TO SEC-RESPONSE-CODE), adicionar PERFORM P150-GET-USER-AGE THRU P150-EXIT.

---

## Epic 3: Atualização dos programas chamadores

**Título:** [EPIC] Atualizar COMMAREA nos programas que chamam SECMGR  
**Assignee:** Shawn  
**Descrição:** Atualizar a estrutura WS-SECURITY-REQUEST no programa `src/programs/online/INQONLN.cbl` (e qualquer outro programa chamador) para incluir o novo campo SEC-USER-AGE, garantindo compatibilidade da COMMAREA.  
**Critérios de aceite:**
- Campo SEC-USER-AGE adicionado em INQONLN.cbl na estrutura WS-SECURITY-REQUEST
- Todos os programas que chamam SECMGR via EXEC CICS LINK foram atualizados
- Tamanho da COMMAREA está consistente entre chamador e chamado

**Story 3.1:** Atualizar WS-SECURITY-REQUEST no INQONLN.cbl  
- **Assignee:** Shawn  
- **Story Points:** 1  
- **Descrição:** Adicionar `05 SEC-USER-AGE PIC S9(3) COMP` ao final da estrutura WS-SECURITY-REQUEST em `src/programs/online/INQONLN.cbl` (linhas 27-33).

**Story 3.2:** Verificar e atualizar demais programas chamadores  
- **Assignee:** Shawn  
- **Story Points:** 2  
- **Descrição:** Buscar todos os programas que referenciam SECMGR ou que declaram a estrutura WS-SECURITY-REQUEST e atualizá-los com o novo campo.

---

## Epic 4: Criação de copybook compartilhado (melhoria técnica)

**Título:** [EPIC] Extrair COMMAREA de segurança para copybook compartilhado  
**Assignee:** Shawn  
**Descrição:** Criar um copybook `src/copybook/online/SECREQ.cpy` contendo a estrutura SECURITY-REQUEST-AREA para evitar duplicação e divergência entre SECMGR e seus chamadores.  
**Critérios de aceite:**
- Copybook SECREQ.cpy criado com a estrutura completa (incluindo SEC-USER-AGE)
- SECMGR.cbl atualizado para usar COPY SECREQ
- INQONLN.cbl atualizado para usar COPY SECREQ
- Todos os demais chamadores atualizados

**Story 4.1:** Criar copybook SECREQ.cpy  
- **Assignee:** Shawn  
- **Story Points:** 1  
- **Descrição:** Criar `src/copybook/online/SECREQ.cpy` com a estrutura SECURITY-REQUEST-AREA completa.

**Story 4.2:** Substituir definições inline por COPY SECREQ em todos os programas  
- **Assignee:** Shawn  
- **Story Points:** 2  
- **Descrição:** Substituir as definições inline da COMMAREA em SECMGR.cbl e INQONLN.cbl por `COPY SECREQ`.

---

## Epic 5: Testes e validação

**Título:** [EPIC] Testar funcionalidade de retorno de idade no SECMGR  
**Assignee:** Shawn  
**Descrição:** Realizar testes unitários e de integração para garantir que a nova funcionalidade retorna corretamente a idade do usuário e que não houve regressão nas funcionalidades existentes (validação, autorização, auditoria).  
**Critérios de aceite:**
- Teste com usuário existente na USERINFO retorna idade correta
- Teste com usuário inexistente na USERINFO retorna idade 0 e mensagem informativa
- Teste de regressão: operações 'A' (autorização) e 'L' (auditoria) continuam funcionando normalmente
- Teste de COMMAREA: chamada do INQONLN ao SECMGR funciona com a nova estrutura

**Story 5.1:** Criar cenários de teste unitário para P150-GET-USER-AGE  
- **Assignee:** Shawn  
- **Story Points:** 2  
- **Descrição:** Testar cálculo de idade com diferentes datas de nascimento, incluindo edge cases (aniversário no dia corrente, ano bissexto).

**Story 5.2:** Executar testes de regressão nas operações existentes  
- **Assignee:** Shawn  
- **Story Points:** 2  
- **Descrição:** Garantir que as operações de validação (V), autorização (A) e auditoria (L) continuam funcionando conforme esperado após as mudanças.

**Story 5.3:** Teste de integração end-to-end (INQONLN → SECMGR)  
- **Assignee:** Shawn  
- **Story Points:** 2  
- **Descrição:** Testar o fluxo completo de INQONLN chamando SECMGR com a nova COMMAREA e verificar que a idade é retornada corretamente.

---

**Total estimado:** ~22 Story Points  
**Epics:** 5  
**Stories:** 12
