# Autenticação: bloqueios concorrentes / Concurrent account locks

Estado: proposta dependente do PR #5; não integrada em main. Só dados sintéticos.

## Português

Corrige uma condição de corrida: um login que já passou a verificação da password
não pode apagar um bloqueio aplicado entretanto por outros pedidos. Antes de emitir
o token, a aplicação volta a verificar a conta sob lock PostgreSQL, incluindo estado,
identidade, hash e versão de autenticação. A resposta usa as permissões actuais.

Após 15 minutos de bloqueio, a primeira nova falha inicia uma nova contagem, em vez
de bloquear imediatamente a conta por mais 15 minutos. Pedidos durante um bloqueio
activo não prolongam a penalização. O cliente continua a receber erro genérico 401.

Sem alterações visuais ou novas capturas: esta melhoria é interna. Os testes de
regressão incluem cinco transacções concorrentes contra PostgreSQL 16. Resultados
efectivamente executados constam do PR e do registo de execução.

Não equivale a prontidão para produção: administração auditável, MFA, refresh,
revogação por sessão e rotação externa de segredos continuam pendentes.

## English

A login that already passed password verification can no longer clear a lock set
by another request. Before issuing a token, the application rechecks account state,
identity, password hash and authentication version under a PostgreSQL row lock.
The response uses current permissions. Expired locks start a fresh failure window;
requests during an active lock do not extend it. Login failures remain generic.

Regression tests include five concurrent transactions against PostgreSQL 16. This
is a dependent proposal, not a main-branch release or production-readiness claim.
Audited account administration, MFA, refresh and per-session revocation remain open.

GridAudit é um projecto independente, sem afiliação à EDM. Indício não é prova;
nenhuma multa automática. / Independent project, not affiliated with EDM.
