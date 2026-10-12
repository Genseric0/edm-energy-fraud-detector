# Execução do roadmap — primeiro lote da Fase 0

Base revista: `231fd0de60dfbe9e84ae7f4602c40d5b7631ec30`, em 12/10/2026.
O [ROADMAP.md](../ROADMAP.md) preserva o plano recebido; caixas por marcar não significam funcionalidades já entregues.

## Alterações neste lote

| Item | Alteração | Limitação |
|---|---|---|
| SEC-01 | Controller de simulação exclusivo do perfil `demo`; valores obrigatórios entre 0,001 e 1 000 000 kWh; controlos da interface dependem das capacidades da API | O perfil demo nunca deve ser activado com dados reais |
| SEC-04, parcial | Token apenas em memória, expiração máxima de 15 minutos e limite de login por IP | Contas em BD, password individual, MFA e refresh continuam pendentes |
| API-01 | Erros de validação 400, recurso inexistente 404 e conflito de integridade 409 com mensagens genéricas | Os endpoints existentes com respostas próprias mantêm o seu contrato |
| SEC-06 | JWT_SECRET validado em bytes UTF-8, X-Frame-Options DENY e CSP da API | CSP do alojamento frontend é um trabalho distinto |
| QA-01, parcial | Testes de autorização, perfil demo, validação, JWT, CORS e limite de login | Matriz de todos os endpoints, área e papéis futuros ainda pendente |
| QA-02 | Workflow visual/PostgreSQL passa a executar em PRs; browser/Vite/origem CORS usam 127.0.0.1; captura mobile adicionada | A execução na CI é a evidência para aceitar o lote |
| CI-01, parcial | Actions fixadas em commits, Dependabot, CodeQL e SECURITY.md | Secret scanning depende da configuração do GitHub; licença exige decisão do titular |
| Frontend | Build inclui `tsc --noEmit`; sessão limpa quando a API responde 401 | Recarregar exige novo login |

A CI da base já tinha sucesso no workflow visual/PostgreSQL; a observação do roadmap de que ainda faltava validar CORS era anterior a essa execução. O novo lote precisa da sua própria validação.

## Arranque de demonstração

Definir `EDM_ADMIN_PASSWORD`, `EDM_OPERATOR_PASSWORD` e `JWT_SECRET` (mínimo 32 bytes, aleatório). Para disponibilizar os comandos sintéticos de simulação, definir `SPRING_PROFILES_ACTIVE=demo`. Com PostgreSQL e dados exclusivamente sintéticos, os perfis podem ser `postgres,demo`.

Sem `demo`, os endpoints de simulação não existem (404 para administrador autenticado); operador continua sem autorização (403). A interface consulta `/api/energy/capabilities` e esconde esses comandos fora da demo.

## Validação

- Backend: `mvn -B verify -f backend-spring-boot/pom.xml`.
- Frontend: `npm ci && npm run build`.
- CI: login e API em demo; PostgreSQL com reinício; Playwright desktop/mobile; CodeQL.
- O resultado exacto das execuções fica no PR. A presença do workflow não equivale a um teste aprovado.

## Continuação obrigatória

1. SEC-02: rotação de segredos e limpeza coordenada do histórico; não foi executada qualquer reescrita.
2. SEC-04: utilizadores em BD com passwords individuais, bloqueio persistente, refresh seguro e invalidação de sessões.
3. Fase 1: Flyway, BigDecimal, leituras temporais únicas e backup/restauro testado.
4. Fase 2: áreas, papéis, casos, equipas, agenda, revisão independente, decisão, auditoria, anexos e notificações.
5. Fases 3–6: interface/PWA completa, acessibilidade, análise explicável, scans/pentest isolados, actualização de framework e operação.

Nenhum destes pontos deve ser apresentado como concluído por este lote. O merge exige CI verde e revisão humana, conforme o princípio 7 do roadmap.
