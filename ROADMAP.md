# GridAudit EDM — Mapa de Conclusão (Roadmap v1)

> Versão de 12/10/2026. Baseado no relatório de evolução do projecto e numa revisão estática do código (backend, frontend, CI).
> Estado: **protótipo avançado**. Objectivo: **produto apresentável e piloto-ready**, com base de dados, processos e interface completos.

## 0. Princípios inegociáveis

1. **Indício ≠ prova.** O sistema detecta discrepâncias; só a vistoria técnica confirma. Nunca acusa nem multa automaticamente.
2. **Sem multas/cobrança** até existir tabela oficial vigente confirmada pela EDM/ARENE (ver `docs/revisao-legal-cobrancas-2026.md`).
3. **Segregação de funções**: quem inspeciona não revê; quem revê não aprova.
4. **Tudo auditável**: nenhuma alteração sem autor, data, motivo e registo imutável.
5. **Minimização de dados pessoais**: só o necessário, com acesso por função e por área.
6. **Independente até haver autorização**: o projecto não é afiliado à EDM (ver secção 7, Identidade visual).
7. **Nenhum agente de IA faz push directo em `main`**: tudo por branch + PR + CI verde + revisão humana.

## 1. Arquitectura alvo (monólito modular)

```mermaid
flowchart LR
  U[Browser / PWA de campo] -->|HTTPS| API[Spring Boot 4.1 API]
  API --> DB[(PostgreSQL 16 + Flyway)]
  API --> OBJ[(MinIO / S3: anexos)]
  API --> JOB[Jobs agendados: balanço, alertas, notificações]
  JOB --> DB
  API --> OBS[Logs + métricas OpenTelemetry]
  CI[GitHub Actions] --> SEC[Semgrep · CodeQL · Trivy · gitleaks · ZAP]
  CI --> AG[Agentes de pentest: Shannon / Strix (manual ou semanal)]
```

Decisão: **monólito modular** (pacotes `auth`, `grid`, `readings`, `cases`, `evidence`, `audit`, `notify`, `admin`). Microserviços só se houver necessidade real.

## 2. Modelo de dados alvo (PostgreSQL + Flyway)

| Tabela | Campos-chave | Notas |
|---|---|---|
| `app_user` | id, username (único), password_hash, role, service_area_id, active, failed_attempts, locked_until, mfa_secret | Password própria por utilizador; bloqueio após falhas |
| `service_area` | id, name, type (`CENTRAL_GERAL`/`REGIONAL`), parent_id | Define qual central é competente (não por proximidade geométrica) |
| `transformer` | id, code (único), name, service_area_id, lat, lon, capacity_kva | |
| `transformer_reading` | id, transformer_id, period_start, period_end, supplied_kwh NUMERIC(14,3), source | **Série temporal** (hoje é um campo único) |
| `meter` | id, serial (único), transformer_id, install_location, remote_enabled, customer_ref | `customer_ref` pseudonimizado; dados do cliente fora desta tabela |
| `meter_reading` | id, meter_id, recorded_at, energy_kwh NUMERIC(14,3), source, ingested_by | `UNIQUE(meter_id, recorded_at, source)`; validação de sequência |
| `energy_balance` | id, transformer_id, period, supplied_kwh, billed_kwh, loss_pct, threshold_pct, model_version | Resultado reproduzível do cálculo |
| `alert` | id, balance_id, transformer_id, severity, status, created_at | Um alerta único por discrepância |
| `case_file` | id, alert_id, service_area_id, status, assigned_team, opened_at | Máquina de estados (secção 4) |
| `inspection` | id, case_id, meter_id, inspector_id, scheduled_at, performed_at, estimated_unaccounted_kwh, verified_kwh, outcome, notes | `fine_amount` **não existe** |
| `evidence_file` | id, inspection_id, storage_key, sha256, mime, size, taken_at, uploaded_by | Hash de integridade; limpeza de metadados sensíveis |
| `case_event` | id, case_id, seq, actor_id, type, payload jsonb, prev_hash, hash, created_at | **Só inserção**; hash encadeado; permissões de BD sem UPDATE/DELETE |
| `notification` | id, case_id, to_area_id, channel, sent_at, ack_at | Recibo de entrega |
| `config_parameter` | key, value, valid_from, source_ref, approved_by | Limiar, tarifas: sempre com fonte e aprovação |

Regras: `BigDecimal` no código; migrações Flyway versionadas (`validate` em produção, nunca `update`); índices em FKs e em `recorded_at`; backups `pg_dump` diários **com teste de restauro**.

## 3. Papéis e permissões

| Papel | Pode | Não pode |
|---|---|---|
| `ANALYST` | Ver alertas da sua área, fazer triagem | Aprovar, inspecionar |
| `DISPATCHER` | Atribuir equipas, agendar | Rever relatórios |
| `INSPECTOR` | Registar vistoria, fotos, leituras | Rever/aprovar o próprio caso |
| `SUPERVISOR` | Rever relatório de vistoria | Aprovar o que ele próprio inspecionou |
| `APPROVER` (eng.º chefe) | Decisão final (inconclusivo / descartado / irregularidade técnica verificada) | Alterar provas |
| `AUDITOR` | Leitura total + verificação da cadeia de auditoria | Qualquer escrita |
| `SYS_ADMIN` | Utilizadores, áreas, parâmetros | Decidir casos |

Aplicação: RBAC por papel + **âmbito por `service_area`** (ABAC) + regra no código **e** restrição na BD de que inspetor ≠ revisor ≠ aprovador no mesmo caso.

## 4. Máquina de estados do caso

`DETECTED → TRIAGED → ASSIGNED → SCHEDULED → INSPECTED → UNDER_REVIEW → DECIDED(INCONCLUSIVE | DISCARDED | VERIFIED_TECHNICAL_IRREGULARITY) → NOTIFIED → CLOSED`

- Cada transição é um `case_event` com autor e justificação.
- `REOPENED` por contestação, com novo ciclo de revisão.
- Horários de visita: padrões agregados e disponibilidade da equipa; **nunca** vigilância de horários privados do morador.

## 5. Fases, tarefas e critérios de aceitação

### Fase 0 — Higiene e segurança base (Semana 1)
- [ ] Limpar o histórico do Git (credenciais antigas) e confirmar rotação de segredos
- [ ] `simulate-theft` / `normalize` apenas com `@Profile("demo")` (ver SEC-01)
- [ ] Utilizadores em BD com password própria; rate limit no login; expiração curta + refresh
- [ ] `@ControllerAdvice` com 400/404/409 consistentes; `frameOptions DENY`; CSP; validar `JWT_SECRET` ≥ 32 bytes no arranque
- [ ] Dependabot, CodeQL, secret scanning, Actions fixadas por SHA, `SECURITY.md`, `LICENSE`
- [ ] Validar o workflow Playwright após a correcção CORS (P0 do relatório)
- **Aceitação:** matriz de testes papel × endpoint verde; nenhum segredo detectado no histórico; CI verde.

### Fase 1 — Base de dados completa (Semana 2)
- [ ] Migrações Flyway V1…Vn com o modelo da secção 2; `ddl-auto=validate`
- [ ] Migrar kWh e valores para `BigDecimal`/`NUMERIC`
- [ ] Testcontainers (Postgres real) em todos os testes de repositório
- [ ] Script de dados sintéticos realista (várias centrais, 50+ PT, milhares de contadores)
- [ ] Backup/restauro automatizado e testado
- **Aceitação:** migração limpa e incremental repetíveis; restauro de backup validado; zero perda de registos.

### Fase 2 — Núcleo de processos (Semanas 3–4)
- [ ] Alertas únicos por discrepância; casos com máquina de estados
- [ ] Atribuição de equipas, agendamento, vistoria, revisão, decisão, notificação, encerramento
- [ ] `case_event` encadeado por hash + endpoint de verificação de integridade
- [ ] Anexos (MinIO): SHA-256, limite de tamanho/tipo, antivírus, permissões por papel
- [ ] Notificações à Central Geral e à regional com recibo
- **Aceitação:** um caso percorre o ciclo completo com 3 utilizadores distintos; tentativa de auto-aprovação é recusada (teste automático).

### Fase 3 — Interface completa (Semanas 4–6)
Ecrãs: Login + MFA · Dashboard (KPIs + mapa dos PT) · Transformadores · Alertas · Casos (lista + linha do tempo) · Atribuição/agenda · **Vistoria em PWA offline** (fotos, GPS, assinatura) · Revisão · Decisão · Relatórios PDF/Excel · Auditoria (verificação de hash) · Administração · Notificações · Perfil/segurança.
- [ ] Sistema de design com *tokens* (cor, tipografia, espaçamento) num único ficheiro
- [ ] Português de Moçambique; moeda MT; datas locais
- [ ] Responsivo e utilizável em telemóvel de gama baixa; acessibilidade WCAG 2.1 AA
- [ ] Estados de erro, vazio e carregamento em todos os ecrãs; testes Playwright por fluxo
- **Aceitação:** screenshots desktop/mobile na CI; auditoria de acessibilidade sem falhas graves; fluxo completo executado por alguém que não é o autor.

### Fase 4 — Análise inteligente (Semanas 6–8)
- [ ] Perfis de consumo por contador e por transformador (sazonalidade, feriados)
- [ ] Detecção estatística/ML (ex.: Isolation Forest) **com explicação** do porquê do alerta
- [ ] Avaliação em dados sintéticos com taxa de falsos positivos reportada; sem afirmar precisão em dados reais
- [ ] Limiar e parâmetros em `config_parameter`, com fonte e aprovação
- **Aceitação:** relatório de avaliação reproduzível; cada alerta mostra os factores que o causaram.

### Fase 5 — Qualidade e segurança contínuas (em paralelo)
- Pirâmide de testes: unitários → integração (Testcontainers) → API (Schemathesis) → E2E (Playwright)
- Gates na CI: build, testes, Semgrep, CodeQL, Trivy, gitleaks, ZAP baseline
- Pentest com agentes (Shannon/Strix) **num ambiente isolado**, semanal ou antes de cada release; relatório antes/depois no repo
- Migração Spring Boot 3.2 → **4.1** (SEC-03) com testes de regressão

### Fase 6 — Operação e entrega (Semanas 8–10)
- [ ] Dockerfiles (backend e frontend), imagens por digest, `docker-compose` completo
- [ ] Observabilidade: logs estruturados, métricas, alarmes básicos
- [ ] Runbooks: arranque, backup/restauro, rotação de segredos, resposta a incidentes
- [ ] `THREAT_MODEL.md`, `SECURITY.md`, `docs/ARQUITECTURA.md`, manual do utilizador
- [ ] Vídeo/demo de 3 min + proposta de piloto para a EDM (secção 8)

## 6. Equipa de agentes de IA

| Agente | Missão | Entrega | Regra |
|---|---|---|---|
| **Arquitecto** | Desenhar módulos, ADRs, modelo de dados | `docs/adr/*.md` | Valida antes de codificar |
| **Backend** | Implementar API, regras, migrações | PR com testes | Sem tocar em segurança sem revisão |
| **BD** | Flyway, índices, seeds, backups | PR + teste de restauro | Nunca `ddl-auto=update` |
| **Frontend/UI** | Ecrãs, tokens de design, PWA | PR + screenshots | Seguir a secção 7 |
| **QA** | Testes (unit, integração, E2E), cobertura | PR de testes | Falha primeiro, corrige depois |
| **Segurança (red team)** | Shannon/Strix, ZAP, Semgrep | Relatório + issues | Só em ambiente isolado, orçamento de tokens definido |
| **Revisor** | Revisar PRs (segurança, performance, estilo) | Comentários no PR | Bloqueia merge se houver risco alto |
| **Documentação** | README, manual, changelog | PR de docs | Não afirmar o que não foi testado |

Regras de trabalho: uma tarefa por branch; commits convencionais; **CI verde e revisão humana antes do merge**; nenhum segredo, token ou dado real é partilhado com agentes; custos de API com limite.

## 7. Identidade visual (cores e logotipo da EDM)

- **Estado:** não encontrei publicamente uma paleta oficial nem um manual de marca da EDM. **Não vou inventar códigos de cor.**
- **Risco a evitar:** usar o logotipo oficial num sistema que **acusa suspeitas de fraude** pode ser confundido com um documento oficial da EDM (relatórios PDF, alertas, notificações). Isso gera risco legal e de reputação.
- **Política recomendada:**
  1. Usar as cores e o logotipo oficiais **apenas com autorização escrita da EDM** (idealmente com o manual de marca).
  2. Até lá, usar uma identidade própria **GridAudit** com paleta inspirada em empresas de energia e o aviso *«Projecto independente, sem afiliação à EDM»* no rodapé e nos PDFs.
  3. Implementar as cores como **tokens semânticos** (`--color-primary`, `--color-accent`, `--color-danger`, …) num único ficheiro (`theme.css`/Tailwind). Trocar para a paleta oficial passa a ser uma alteração de um ficheiro.
  4. Se tiveres o logotipo ou o manual de marca, envia-me o ficheiro e extraio as cores exactas.
- **Nunca:** carimbar PDFs de vistoria com o logotipo oficial enquanto o sistema não estiver homologado.

## 8. Dependências externas e riscos

| Risco | Mitigação |
|---|---|
| Legislação sobre multas e sanções incerta | Manter bloqueio; confirmar com EDM/ARENE; pedir parecer jurídico |
| Dados pessoais de clientes | Minimizar; pseudonimizar; verificar a legislação moçambicana de protecção de dados antes de qualquer piloto |
| Falta de dados reais para validar o modelo | Dados sintéticos + proposta de piloto controlado com acordo escrito |
| Dependência de agentes de IA (custo/qualidade) | Orçamento, revisão humana obrigatória, ferramentas determinísticas na CI |
| Âmbito demasiado grande para uma pessoa | Entregar por fases; cada fase é demonstrável sozinha |

## 9. Backlog de correcções (prioridade)

| ID | Problema | Prior. | Fase |
|---|---|---|---|
| SEC-01 | Endpoints `simulate-theft`/`normalize` em produção (permitem esconder fraude); sem limites de valor | P0 | 0 |
| SEC-02 | Credenciais antigas no histórico do Git | P0 | 0 |
| SEC-03 | Spring Boot 3.2.3 sem suporte | P0 | 5 |
| SEC-04 | Utilizadores fixos no código, password partilhada por papel, sem rate limit, token em `localStorage` | P0 | 0 |
| QA-01 | Backend sem testes automáticos | P0 | 0 |
| QA-02 | Playwright/CORS (`127.0.0.1` vs `localhost`) por validar | P0 | 0 |
| DB-01 | `ddl-auto=update`; sem Flyway | P0 | 1 |
| DB-02 | kWh em `Double`; leituras sem unicidade/validação de sequência | P1 | 1 |
| SEC-05 | Autorização só por papel; sem âmbito por central | P1 | 2 |
| FEAT-01 | Vistoria sem transições, revisão ou trilho de auditoria | P1 | 2 |
| API-01 | Erros como `RuntimeException` (500); sem `@ControllerAdvice` | P1 | 0 |
| SEC-06 | `frameOptions sameOrigin`, sem CSP, `JWT_SECRET` sem validação de tamanho | P1 | 0 |
| CI-01 | Actions por tag (não SHA), sem Dependabot/CodeQL, sem `LICENSE`/`SECURITY.md` | P1 | 0 |
| UI-01 | Falta frontend de vistoria, anexos, notificações, mapa, relatórios de vistoria | P1 | 3 |
| UI-02 | Identidade visual: tokens + política da secção 7 | P2 | 3 |
| ML-01 | Detecção só por limiar fixo (5%) | P2 | 4 |

## 10. Cronograma

| Semana | Foco | Entregável demonstrável |
|---|---|---|
| 1 | Fase 0 | Repo limpo, testes de autorização, CI de segurança |
| 2 | Fase 1 | BD completa com migrações e dados sintéticos |
| 3–4 | Fase 2 | Caso completo de ponta a ponta (API) |
| 4–6 | Fase 3 | Interface completa e PWA de campo |
| 6–8 | Fase 4 | Detecção explicável + relatório de avaliação |
| 8–10 | Fase 6 | Imagens, runbooks, demo e proposta de piloto |
| contínuo | Fase 5 | Pentest com agentes, antes/depois |

## 11. Definição de «concluído» do projecto

- Ciclo completo de um caso com 3 utilizadores diferentes, sem possibilidade de auto-aprovação
- BD migrada por Flyway, restauro de backup testado
- Cobertura de testes de autorização por papel e por área
- Último relatório de pentest sem achados críticos ou altos em aberto
- Documentação (arquitectura, modelo de ameaças, runbooks, manual) publicada
- Aviso de independência presente enquanto não houver autorização da EDM
