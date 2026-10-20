# Execução do roadmap — Fase 0 em curso

Base revista: `231fd0de60dfbe9e84ae7f4602c40d5b7631ec30`, em 12/10/2026.
O [ROADMAP.md](../ROADMAP.md) preserva o plano recebido; caixas por marcar não significam funcionalidades já entregues.

## Quinto lote — 14/10/2026

- Estado verificado: PRs #2–#5 abertos, sem merge; HEAD do #5 `310fb95` com os três workflows aprovados. Main permanece `231fd0d`.
- Branch dependente `fix/account-lock-races`, base `feat/persistent-users` (#5). Fases 0/5: correcção de segurança descoberta antes de adicionar administração de contas.
- `AccountAuthenticationService`: sucesso revalida sob lock estado, identidade, hash e authVersion. Um login em curso não pode limpar um bloqueio aplicado entretanto. Bloqueios expirados iniciam nova janela de falhas; pedidos durante bloqueio não o prolongam.
- `AuthController`: token e resposta usam a conta revalidada, incluindo papel actual. `application.properties`: Open EntityManager in View desactivado também em H2 para não reutilizar estado de autenticação antigo entre transacções; PostgreSQL já tinha esta configuração.
- `AccountAuthenticationServiceTest`: regressões determinísticas de bloqueio, expiração exacta, alteração de password/versão/papel, desactivação, remoção e recriação. `AccountPersistenceTest`: PostgreSQL 16 real, cinco falhas concorrentes sem perda de incrementos, login antigo versus bloqueio confirmado e recuperação persistida.
- Validação: `git diff --check` aprovado; Maven indisponível localmente. No commit funcional `b16e8f3` do [PR #6](https://github.com/Genseric0/edm-energy-fraud-detector/pull/6), a [CI backend/frontend](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38509254732) aprovou 53 testes, zero falhas/erros/ignorados, build e smoke test. [Visual/PostgreSQL](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38509254507) e [CodeQL](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38509254500) também aprovados. Os checks de commits posteriores continuam obrigatórios.
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

## Quinto e sexto lotes — consolidação em 15/10/2026

Estado remoto confirmado: PRs #2–#7 abertos, em cadeia dependente; nenhum integrado em `main`.
O [PR #6](https://github.com/Genseric0/edm-energy-fraud-detector/pull/6) corrige concorrência
no bloqueio de contas: revalidação transaccional no sucesso do login, recuperação após
expiração e testes de falhas concorrentes em PostgreSQL. A base usada pelo PR #7 é
`7b18cebab46a4e8ad434c80a932212d31c467abb`.

O [PR #7](https://github.com/Genseric0/edm-energy-fraud-detector/pull/7) estabiliza a demonstração
e prepara a apresentação pública solicitada, em paralelo com a Fase 0 e qualidade da Fase 5:

- `AlertsTable.tsx`: simulação visível apenas para ADMIN quando a API confirma demo;
  ausência de capacidade mantém os comandos escondidos.
- `App.tsx`, `LoginView.tsx`, `SummaryCards.tsx`, `TransformerDetailView.tsx`,
  `DiscrepancyChart.tsx`, `constants/energy.ts` e `generateAuditPdf.ts`: identificação
  independente, valores demonstrativos, faturado distinto de pago, estimativa pro-rata
  distinta de medição individual; remoção de alegações de limiar legal e relatório oficial.
- Gráficos sem animação inicial apresentam imediatamente os valores completos.
- `e2e/capture.mjs`: verifica ausência de simulação fora de demo, erros JavaScript,
  detalhe, download PDF e textos; retira o cursor do gráfico antes das capturas.
- `README.md`, `docs/SHOWCASE.md`, `docs/screenshots/*.png`: tour PT/EN com quatro
  capturas reais (login, desktop, mobile, detalhe), dados sintéticos e proveniência.
  README passa a indicar configuração de credenciais antes do arranque e `npm ci`.

Build TypeScript/Vite local aprovado em 15/10/2026. O bundle principal ainda gera aviso
de tamanho; optimização por carregamento diferido permanece pendente. Maven/Docker não
estão disponíveis no ambiente local: validação backend e PostgreSQL é executada na CI.
O commit anterior `cbb3f107` tinha os três workflows aprovados. A nova validação funcional
e origem dos prints é `0d7f32b1fe2d46af9850580220795f895126f8ee`, no
[workflow visual/PostgreSQL](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38782377939).
Na [CI backend/frontend](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38782378040),
os logs confirmam **53 testes, zero falhas, zero erros e zero ignorados**, além do build
frontend e smoke de login/API aprovados. O [CodeQL](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38782377950)
também passou. Links relativos do README e tour verificados localmente.
Workflow visual/PostgreSQL aprovado: persistência após reinício, login, detalhe e download
PDF concluídos. As quatro imagens do artefacto `11995728996` foram inspeccionadas
visualmente antes de serem versionadas. O gráfico desktop já mostra as barras completas,
sem tooltip sobreposto; a captura mobile evidencia a tabela com deslocamento horizontal.

Métricas públicas em 15/10/2026: **0 estrelas, 0 forks**, variação zero face ao último
registo. O endpoint de contribuidores não está disponível pelo conector; contagem não
confirmada. Tráfego e alcance não foram medidos. Não houve divulgação externa.

**Próximo passo:** administração auditável de contas e mudança de password com invalidação
de tokens; depois refresh/revogação por sessão. SEC-02 exige rotação externa e tratamento
coordenado do histórico. Fase 1 mantém BigDecimal, unicidade temporal e backup/restauro.
Os PRs dependem de revisão humana na ordem #2 → #7. CI verde de um lote não conclui
a Fase 0 nem certifica prontidão para produção; fases 2–6 continuam pendentes.

## Sétimo lote — 16/10/2026

Base confirmada: PR #7, `073d135`, com backend/frontend, PostgreSQL/Playwright e CodeQL
aprovados. PRs #2–#7 continuam abertos, sem merge. O [PR #8](https://github.com/Genseric0/edm-energy-fraud-detector/pull/8),
branch `fix/production-boundaries`, depende de #7. Fase 0 em curso; qualidade da Fase 5
e suporte de dados da Fase 1 decorrem em paralelo.

- SEC-04: `PasswordChangeController`, DTO e `PasswordChangeService` permitem mudar
  apenas a password da própria sessão, com confirmação da actual. Lock pessimista
  revalida identidade, estado e `authVersion`; a nova password deve ser diferente e
  respeitar os limites BCrypt (12+ caracteres, até 72 bytes UTF-8).
- V4 e `AccountSecurityEvent`/repositório: hash, incremento de versão e evento mínimo
  são confirmados na mesma transacção. Falha de auditoria reverte a mudança. Evento
  não guarda passwords, hashes, tokens ou IP. Trigger PostgreSQL recusa UPDATE/DELETE;
  proprietário da BD continua privilegiado, sem garantia contra administração hostil.
- `LoginRateLimitFilter`: login e mudança de password partilham o limite por IP.
  Password actual errada conta para bloqueio persistente; DTOs não expõem passwords
  nas representações textuais e o login limita o tamanho dos campos recebidos.
- `Header`, `PasswordChangeDialog`, `authApi`: formulário acessível por teclado,
  confirmação, estado de envio, erros e novo login obrigatório após guardar.
- `DataSeeder` e configuração: grelha sintética desactivada por defeito, incluindo
  PostgreSQL; `EDM_SEED_ENABLED=true` apenas para demos isoladas e CI. Desligar a opção
  não apaga dados existentes. Contas de bootstrap continuam separadas e são pendência.
- Testes PostgreSQL: revogação real de token, rejeição da password antiga, novo login,
  isolamento de contas, anonimato, política UTF-8, bloqueio, duas rotações concorrentes,
  rollback de auditoria e trigger de protecção. Migrações verificadas até V4.
- E2E: formulário, fim de sessão, rejeição de token/password antigos e login novo;
  capturas desktop/mobile do diálogo sem passwords preenchidas.
- Documentação: README, CONTRIBUTING, runbook PostgreSQL e tour actualizados;
  [revisão de segurança/dados](security-review-2026-10-16.md) reconstrói os fluxos do
  próprio código e lista bloqueios para dados reais. Não foi feito pentest externo.

Validação local: TypeScript/Vite, sintaxe E2E e `git diff --check` aprovados. Maven/Docker
indisponíveis localmente; a CI principal do primeiro commit confirmou **65 testes sem
falhas/erros/ignorados**. A CI visual detectou que o opt-in de fixtures do job afectava
o teste negativo de base vazia; corrigida a configuração desse teste no commit
`ba2064dea545ff05dc9e48f6bbe84b925d450a90`.

A revisão visual identificou o diálogo no canto superior; corrigido em `e001683`,
com verificação de centragem desktop e limites do viewport mobile no E2E.
Validação final desse código funcional em 16/10/2026:

- [CI backend/frontend](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38849132602):
  **65 testes, zero falhas, zero erros e zero ignorados**, build e smoke aprovados.
- [PostgreSQL/Playwright](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38849132607):
  persistência após reinício, login, PDF, mudança de password, revogação e novo login aprovados.
- [CodeQL](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38849132548): aprovado.
- Seis capturas reais do artefacto `12024502522`, inspeccionadas visualmente e versionadas
  em `docs/screenshots`; origem `e001683a15c07a4f1ddb9ede36c6362835219b96`.
  Documentação/imagens são acrescentadas num commit posterior, sem alterar o código funcional.
- Links relativos do README, CONTRIBUTING, tour e relatórios verificados localmente.
  O aviso de tamanho do bundle frontend continua pendente.

Métricas públicas observadas em 16/10/2026: **0 estrelas e 0 forks**, variação zero.
Contribuidores e tráfego permanecem sem confirmação; nenhum alcance foi inferido.

**Próximo passo:** administração auditável de contas e autorização por área; depois
refresh/revogação por sessão. Em dados: BigDecimal/NUMERIC, unicidade/proveniência das
leituras e períodos de balanço compatíveis. SEC-02, actualização de framework e teste
de restauro continuam bloqueios de produção. A Fase 0 não está concluída e este lote
não foi integrado em main. Revisão humana continua obrigatória na cadeia #2 → #8.

## Oitavo lote — 16/10/2026, proposta local bloqueada na publicação

PR #8 confirmado aberto em `48ff136`, com CI backend/frontend, CodeQL e
PostgreSQL/Playwright aprovados, incluindo a revisão final de documentação. Os PRs
#2–#8 permanecem sem merge. Base do novo trabalho: #8; branch local
`feat/account-administration`. Fase 0 continua em curso.

Implementadas localmente API e interface para administrar contas existentes: consulta
paginada sem hashes, papel/estado, desbloqueio e revogação de tokens por conta. Password
actual e motivo são obrigatórios; versão esperada recusa formulários antigos. Lock
estável serializa transições, revalida o actor e protege o último ADMIN activo. V5
acrescenta guard e metadados de auditoria, preservando os eventos existentes.
Detalhes, ficheiros e critérios de teste em [proposta](account-administration-proposal.md).

Evidência local: build TypeScript/Vite, sintaxe E2E e diff aprovados. Foram escritos
testes PostgreSQL e E2E para autorização, revogação, concorrência, reautenticação,
rollback e migração, mas **não executados**: Maven/Docker não estão disponíveis neste
ambiente e a CI depende de publicar a branch. Não foram produzidos novos prints.

**Bloqueio:** GitHub devolveu `403 Resource not accessible by integration` na criação
da árvore de ficheiros. Não foi criado novo PR, não houve publicação deste lote e o
controlo de acesso não foi contornado. O trabalho foi preservado num commit local e
num pacote de retoma. A continuação automática foi pausada até restaurar acesso de
escrita ao repositório. Retoma: revalidar estado remoto, publicar branch dependente de
#8, executar CI, corrigir resultados, inspeccionar prints reais e só então pedir revisão.

Métricas públicas observadas em 16/10/2026: 0 estrelas e 0 forks, variação zero.
Contribuidores e tráfego não confirmados. Nenhuma fase marcada como concluída.

## Nono lote — 16/10/2026, qualidade dos registos e interface

Continuação local autorizada: `feat/readings-quality-ui`, sobre `8fbb686` (administração
proposta) e base remota `48ff136` do PR #8. Código funcional novo em `5b1d5b6`.
PRs #2–#8 continuam abertos/sem merge; nenhum novo PR foi criado.

- `MeterReadingRequest`, `InspectionCreateRequest` e entidades: BigDecimal, limite
  NUMERIC(14,3) e rejeição de valores não finitos, negativos, excedentes e precisão inválida.
- `MeterReadingController`/view e V6: autor da sessão nas novas leituras; legado sem
  autor confirmado fica NULL. Instantes acima de microssegundos são recusados e a
  unicidade contador/instante/origem protege contra repetição e escrita concorrente.
- V6 contém preflight sem limpeza/arredondamento implícitos; testes PostgreSQL adicionais
  preparados para preservação de registos e recusa de dados legados inválidos/duplicados.
- `AlertsTable`: cartões mobile com dados e acções legíveis, pesquisa com nome acessível
  e estados de filtro. Desktop mantém tabela. `App`: estado vazio, aviso de resumo
  desactualizado e protecção contra respostas de recarregamento fora de ordem.
- `LocalApiFlowsTest`: 13 fluxos H2 verificam API/JWT, administração, último ADMIN,
  desactivação concorrente, exactidão decimal, autor, duplicados e estimativas sem multa.
  Mockito usa subclass mock maker para funcionar sem auto-attach de agente na JVM.

**Validação executada:** Maven 3.9.9/Java 17, 65 testes locais seleccionados, zero
falhas/erros/ignorados, pacote backend gerado; compilação de todas as classes de teste
incluindo PostgreSQL aprovada. TypeScript/Vite, sintaxe E2E e diff aprovados.
Classes PostgreSQL não executadas: PostgresMigrationTest, AccountPersistenceTest e
AccountAdministrationTest. H2 não substitui esse critério. O ambiente recusou o arranque
do navegador por bloqueio de socket; nenhuma nova captura foi apresentada como verificada.

**Publicação:** nova tentativa concreta devolveu novamente 403 `Resource not accessible
by integration` na criação de árvore GitHub. Não foi tentado outro caminho de escrita.
Continuam pendentes acesso de escrita, publicação/PR, CI PostgreSQL/CodeQL/Playwright e
revisão humana. O pacote de retoma foi actualizado para preservar todos os commits locais.

[Qualidade e evidência](data-quality-local.md) e [critérios para clientes](commercial-readiness.md)
detalham o que falta: contas reais/áreas, isolamento, balanço temporal, processo de casos,
actualização de framework e backup/restauro. Não se declara uso real nem fase concluída.
Métricas observadas: 0 estrelas, 0 forks; variação zero. Sem dados de tráfego/contribuidores.
