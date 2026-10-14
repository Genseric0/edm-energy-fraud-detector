# Execução do roadmap — Fase 0 em curso

Base revista: `231fd0de60dfbe9e84ae7f4602c40d5b7631ec30`, em 12/10/2026.
O [ROADMAP.md](../ROADMAP.md) preserva o plano recebido; caixas por marcar não significam funcionalidades já entregues.

## Quinto lote — 14/10/2026

- Estado verificado: PRs #2–#5 abertos, sem merge; HEAD do #5 `310fb95` com os três workflows aprovados. Main permanece `231fd0d`.
- Branch dependente `fix/account-lock-races`, base `feat/persistent-users` (#5). Fases 0/5: correcção de segurança descoberta antes de adicionar administração de contas.
- `AccountAuthenticationService`: sucesso revalida sob lock estado, identidade, hash e authVersion. Um login em curso não pode limpar um bloqueio aplicado entretanto. Bloqueios expirados iniciam nova janela de falhas; pedidos durante bloqueio não o prolongam.
- `AuthController`: token e resposta usam a conta revalidada, incluindo papel actual. `application.properties`: Open EntityManager in View desactivado também em H2 para não reutilizar estado de autenticação antigo entre transacções; PostgreSQL já tinha esta configuração.
- `AccountAuthenticationServiceTest`: regressões determinísticas de bloqueio, expiração exacta, alteração de password/versão/papel, desactivação, remoção e recriação. `AccountPersistenceTest`: PostgreSQL 16 real, cinco falhas concorrentes sem perda de incrementos, login antigo versus bloqueio confirmado e recuperação persistida.
- Validação local: `git diff --check` aprovado; Maven não instalado, testes delegados à CI do PR. O resultado final fica no comentário de validação desse PR; não assumir aprovação apenas pela adição dos testes.
- Apresentação: [notas bilingues](releases/2026-10-14-auth-locks.md), sem alegações de produção ou capturas de funcionalidades inexistentes. Não houve divulgação externa.
- Métricas de 14/10/2026: 0 estrelas, 0 forks (variação zero). Endpoint de contribuidores indisponível pelo conector; tráfego não medido.
- Próximo passo: administração auditável e rotação de password, seguida de refresh/revogação. SEC-02 depende de acção externa coordenada; licença exige decisão do titular. A Fase 0 não está concluída; não foi feito merge ou escrita directa em main.

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

Definir `EDM_GENSERICO_PASSWORD`, `EDM_OXIDIA_PASSWORD`, `EDM_LEYSA_PASSWORD`, `EDM_HELEN_PASSWORD` (distintas, 12+ caracteres, até 72 bytes UTF-8) e `JWT_SECRET` (mínimo 32 bytes, aleatório). Para disponibilizar os comandos sintéticos de simulação, definir `SPRING_PROFILES_ACTIVE=demo`. Com PostgreSQL e dados exclusivamente sintéticos, os perfis podem ser `postgres,demo`.

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


## Segundo lote — 12/10/2026, 20h Maputo

Base: `533dd567` do PR #2, ainda aberto e com os três workflows aprovados no início desta execução. Branch dependente: `fix/individual-credentials`; não integrada em main. A Fase 0 continua em curso.

- SEC-04: substituição das duas passwords por papel por quatro passwords individuais, sem fallback para as antigas variáveis. Configuração inválida, duplicada ou acima do limite BCrypt impede o arranque sem expor valores.
- SEC-04: o filtro JWT consulta UserDetailsService por pedido e aplica as permissões actuais; rejeita contas removidas, desactivadas, bloqueadas ou expiradas. Não é ainda a implementação de utilizadores em BD, administração de contas ou revogação por sessão.
- Autenticação: falhas de autenticação (incluindo conta bloqueada/desactivada) recebem resposta genérica 401 no login.
- QA: testes adicionais de isolamento das passwords, arranque inválido, limites UTF-8/BCrypt, redução de privilégios e estado da conta; workflows e E2E usam as novas variáveis. Workflows de PR abrangem também branches dependentes.
- Adopção: guia de contribuição em português e inglês, com arranque e verificação reproduzíveis e regras para contribuições seguras.
- Métricas públicas observadas em 12/10/2026: 0 estrelas e 0 forks, sem alteração face à observação anterior. Contagem de contribuidores indisponível pelo conector; tráfego/alcance não medidos.
- Validação: resultados concretos da CI serão registados no PR deste lote; não confundir testes adicionados com testes aprovados.

**Próximo passo:** migrar contas para BD com esquema versionado, bloqueio persistente e administração autorizada; depois refresh e revogação por sessão. SEC-02 continua dependente de rotação externa e limpeza coordenada do histórico. O PR #2 precisa de revisão humana; o segundo lote deve ser revisto sobre essa base e retargetado após integração. A Fase 0 não está concluída.


## Terceiro lote — 13/10/2026

O PR #3 (`8944eb7`) foi confirmado com 35 testes aprovados e CI backend/frontend, PostgreSQL/Playwright e CodeQL com sucesso. Foi marcado pronto para revisão; #2 e #3 continuam sem merge.

A preparação das contas em BD depende de um esquema versionado. Este lote inicia DB-01 da Fase 1 como pré-requisito de SEC-04, sem declarar a Fase 0 concluída:

- V1 descreve as quatro tabelas existentes sem conversão silenciosa dos valores de energia; V2 adiciona índices por FK/data.
- PostgreSQL passa de `ddl-auto=update` para `validate`; Flyway aplica e verifica migrações. Baseline automático e clean desactivados.
- H2 fica explicitamente efémero (`create-drop`), separado da validação de PostgreSQL.
- Testcontainers usa PostgreSQL 16 real: migração limpa, repetição, evolução incremental com preservação de leituras/vistorias/IDs e recusa de esquema legado sem perda do registo de controlo. Não há skip automático por ausência de Docker.
- Runbook em `docs/runbooks/postgres-migrations.md` documenta instalação nova e tratamento de bases antigas; não foi alterada nenhuma base do utilizador.
- Branch `feat/postgres-flyway` depende de `fix/individual-credentials` (#3). Validação do novo lote e link do PR ficam na descrição desse PR.
- Métricas observadas em 13/10/2026: 0 estrelas, 0 forks; variação zero. Contribuidores e tráfego não confirmados.

**Pendências:** contas persistentes, bloqueio/refresh/revogação, rotação de segredos, modelo alvo completo, BigDecimal, unicidade de leituras e backup/restauro automatizado. Próximo lote: adicionar utilizadores persistentes através de uma nova migração, com testes de autenticação e persistência. Ambas as fases 0 e 1 permanecem em curso; nada deste lote está integrado em main.

## Quarto lote — 13/10/2026, 20h Maputo

Base dependente: `22e23e4` do PR #4, confirmado aberto e com backend/frontend, PostgreSQL/Playwright e CodeQL aprovados. Os PRs #2–#4 continuam sem merge; este lote também não está integrado em `main`.

- SEC-04: contas migradas da memória para `app_user`, com username único, hash BCrypt, papel, estado, falhas, bloqueio e versão de autenticação persistidos. A migração Flyway V3 não inclui passwords nem dados pessoais.
- Bootstrap: as quatro contas sintéticas só são criadas numa base vazia; credenciais persistidas não são reescritas no reinício. Continuam obrigatórias passwords distintas de 12+ caracteres e até 72 bytes UTF-8 na primeira instalação.
- Defesa de login: cinco falhas consecutivas bloqueiam a conta por 15 minutos, com actualização serializada por lock pessimista. A mensagem externa permanece genérica e o limite adicional de 10 tentativas/minuto por IP continua activo.
- Sessões: JWT inclui `authVersion`; elevar esse valor invalida tokens já emitidos. Cada pedido continua a obter da BD o estado e as permissões actuais, evitando retenção de privilégios.
- PostgreSQL: teste de migração actualizado para V1→V3 e valida a nova tabela sem perder leituras ou vistorias.
- QA: 41 testes aprovados (zero falhas/erros), incluindo PostgreSQL 16 real; cobrem bootstrap, hashes/papéis, preservação após reinício, política de password, bloqueio, limpeza após sucesso, conta inexistente, redução de papel, estado da conta, invalidação por versão e migração V1→V3. Build frontend, smoke test autenticado, Playwright desktop/mobile, persistência após reinício e CodeQL também foram aprovados na CI do [PR #5](https://github.com/Genseric0/edm-energy-fraud-detector/pull/5).
- Métricas públicas observadas em 13/10/2026: 0 estrelas e 0 forks, sem alteração; não há dados confirmados de tráfego.

**Pendências:** administração autorizada de contas, rotação de password com incremento de `authVersion`, refresh/revogação por sessão, MFA e SEC-02. A Fase 0 não está concluída. Próximo lote: API administrativa auditável para ciclo de vida de contas e depois refresh/revogação; a Fase 1 mantém BigDecimal, unicidade temporal e backup/restauro pendentes.
