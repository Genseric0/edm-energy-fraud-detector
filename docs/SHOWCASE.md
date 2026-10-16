# GridAudit — tour para recrutadores / recruiter tour

Projecto independente, sem afiliação à EDM. Protótipo em evolução; dados sintéticos.
O objectivo é apoiar investigação de discrepâncias de energia, nunca acusar clientes
ou aplicar multas automaticamente. Estimativas pro-rata não são medições individuais.

## Interface real

As imagens são capturas Playwright da aplicação React ligada à API Spring e PostgreSQL,
não mockups. As capacidades abaixo existem nesta branch, propostas nos PRs #7 e #8.

### Login

![Login GridAudit](screenshots/01-login.png)

Sessão com JWT em memória no navegador. Contas BCrypt persistentes e bloqueio por falhas.

### Dashboard desktop

![Dashboard desktop](screenshots/02-dashboard.png)

Balanço fornecido/faturado, gráfico e tabela pesquisável. O limiar é demonstrativo.

### Dashboard mobile

<img src="screenshots/03-dashboard-mobile.png" alt="Dashboard num viewport mobile de 390 pixels" width="390">

Layout adaptado a viewport mobile; tabelas largas têm deslocamento horizontal.

### Detalhe do transformador

![Detalhe do transformador](screenshots/04-transformer-detail.png)

Contadores, pesquisa, triagem e exportação PDF demonstrativa. Simulação só para ADMIN
quando o perfil demo está activo; fica escondida nesta captura sem esse perfil.

### Mudança de password — desktop e mobile

![Mudança de password no desktop](screenshots/05-password-desktop.png)

<img src="screenshots/06-password-mobile.png" alt="Diálogo de mudança de password num viewport mobile" width="390">

Disponível para a própria conta, incluindo OPERATOR. Confirma a password actual e a nova;
guardar exige novo login e invalida os tokens anteriores. O E2E percorre o formulário,
verifica rejeição da password/token antigos e entra com a nova password. Os campos estão
vazios nas capturas para evitar publicação de credenciais.

## Como avaliar

1. Seguir o [README](../README.md) para Java 17, Node 20, Maven, passwords próprias e PostgreSQL.
2. Executar `mvn -B verify -f backend-spring-boot/pom.xml` com Docker disponível.
3. Executar `npm ci` e `npm run build`.
4. Consultar o [workflow visual](../.github/workflows/visual-postgres.yml): arranque,
   login, persistência após reinício, screenshots e download PDF são verificáveis.

Não há URL de produção nem credenciais públicas de acesso. A aplicação local e os
artefactos da CI permitem avaliar as funcionalidades sem dados de clientes reais.

## English

These are real browser captures of the React UI connected to Spring and PostgreSQL,
using synthetic fixtures. The project demonstrates typed frontend development, REST APIs,
JWT/RBAC, versioned migrations, concurrent database tests and reproducible browser checks.
The dashboard, transformer drill-down, demonstration PDF and self-service password change
are implemented. Rotation atomically stores the BCrypt hash, increments the account token
version and inserts minimal audit metadata; the browser then requires a new login.
Synthetic grid fixtures require explicit opt-in and are disabled by default. Account
administration, MFA, refresh, full case workflow, offline PWA and production operations
remain roadmap items. Human review is required before merging dependent PRs.

## Proveniência / provenance

Capturas de 16/10/2026, código funcional `e001683a15c07a4f1ddb9ede36c6362835219b96`, workflow
[38849132607](https://github.com/Genseric0/edm-energy-fraud-detector/actions/runs/38849132607).
Viewports: desktop 1440×900, mobile 390×844; primeiras quatro capturas full-page,
diálogos capturados no viewport. Dados sintéticos do seed
e do teste de persistência. As capturas ficam versionadas aqui para não dependerem
da retenção temporária dos artefactos da CI.
