# Administração de contas existentes — proposta local

Estado em 16/10/2026: implementação local não publicada; validação parcial de backend
agora executada em H2. PostgreSQL/CI e validação no navegador continuam pendentes.
Base remota: `48ff1362ccd9060aa4b4f8425996ad12859528a4`, PR #8. Branch local:
`feat/account-administration`. Não existe novo PR para este lote.

## Comportamento implementado, ainda por validar na CI

- `GET /api/admin/accounts?page=0&size=20`: projecção paginada com ID, username,
  displayName, papel, estado, falhas, bloqueio e versão; nunca entidades/hashes.
- `PUT /api/admin/accounts/{id}`: papel/estado activo, `expectedVersion`, motivo de
  10–500 caracteres e password actual do administrador autenticado.
- `POST /api/admin/accounts/{id}/revoke-sessions` e `/unlock`: versão, motivo e
  password actual. Desbloqueio limpa falhas/bloqueio; ambas incrementam `authVersion`.
- Só ADMIN no filtro, controller e serviço. OPERATOR não ganha privilégios pelo UI.
- Revalidação transaccional de autor, papel, estado, bloqueio e versão. Mudanças
  administrativas usam um lock estável, impedindo transições simultâneas que removam
  todos os administradores. Último ADMIN activo não pode ser removido/demitido.
- Hash não muda nestas operações. Novos pedidos com tokens antigos da conta afectada
  devem ser recusados. Revogação continua por conta, não por sessão individual.
- Autor, motivo e antes/depois de papel/estado são inseridos na mesma transacção.
  Password actual errada conta para bloqueio; mutações administrativas partilham o
  limite de tentativas por IP do login/mudança de password.
- V5 é aditiva: linha de lock, campos de auditoria e validação de metadados. Não faz
  backfill nem altera eventos V4, passwords ou dados do utilizador. H2 create-drop
  inicializa a linha de lock separadamente, sem executar SQL PostgreSQL.
- Diálogo ADMIN no cabeçalho: estados de carregamento/erro/vazio, paginação,
  editar/desbloquear/revogar, confirmação de password/motivo e logout ao alterar a própria conta.

## Ficheiros e testes

Backend: `AccountAdministrationController`, `AccountAdministrationService`, DTOs
`AdminAccountView/Update/Action`, `AccountConflictException`, guard/repositório e
inicializador H2; alterações em SecurityConfig, ApiExceptionHandler, auditoria,
repositório de contas e filtro de limite. Migração `V5__account_administration.sql`.
Frontend: `adminAccountsApi.ts`, `AdminAccountsDialog.tsx`, `Header.tsx`.
Testes: `AccountAdministrationTest`, extensões a `PostgresMigrationTest`,
`LoginRateLimitTest` e `e2e/capture.mjs`.

Os testes adicionados cobrem 401/403, paginação/projecção, desactivação/reactivação,
redução de papel, revogação, desbloqueio, snapshots antigas, motivo, reautenticação,
bloqueio por falhas, último ADMIN, concorrência entre administradores, rollback de
auditoria e V4→V5. E2E foi preparado para gerir Helen sinteticamente e captar desktop/mobile.

Executados: TypeScript/Vite build, `node --check e2e/capture.mjs`, `git diff --check`,
compilação de todo o código Java/testes e 65 testes locais seleccionados. Os 13 testes
`LocalApiFlowsTest` usam H2 real e a cadeia Spring/JWT, incluindo gestão de estado,
revogação, autorização, último ADMIN e desactivação concorrente. O restante conjunto
seleccionado verifica autenticação, DTOs, políticas e limites. Não substitui PostgreSQL.
Aviso de tamanho do bundle permanece. Não executados: Testcontainers, migrações V5/V6,
CodeQL deste lote e Playwright completo. O ambiente bloqueia o arranque do navegador
(socket local não permitido). Não existem novas capturas verificadas: prints do PR #8.

## Bloqueio e retoma

GitHub recusou criar a árvore de ficheiros com **403 — Resource not accessible by
integration**. A publicação foi interrompida no controlo de acesso; não foi tentado
outro caminho de escrita. É necessário restaurar a ligação GitHub com permissão de
escrita em `Genseric0/edm-energy-fraud-detector`, depois publicar a branch e abrir PR
dependente de #8. Não aplicar a migração em dados reais sem CI e revisão.

Pendências fora deste lote: cadastro/convite de novas contas com mudança obrigatória
de password, recuperação de acesso, MFA, papéis/âmbito por área, segregação de funções
do domínio, refresh/revogação por sessão, precisão/unicidade das leituras e restauro.
Fase 0 continua aberta; nada deste lote está integrado em main.
