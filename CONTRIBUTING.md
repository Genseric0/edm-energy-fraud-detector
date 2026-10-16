# Contribuir / Contributing

## Português

O GridAudit é um protótipo independente para estudar discrepâncias de energia. Um alerta é um indício para investigação técnica, não prova de fraude. Não usar dados pessoais reais em exemplos, testes, screenshots ou issues.

1. Leia o [README](README.md), [ROADMAP](ROADMAP.md), [progresso](docs/roadmap-execucao.md) e [SECURITY](SECURITY.md).
2. Use Java 17, Maven, Node.js 20 e Docker para os testes PostgreSQL/Testcontainers. Configure as quatro passwords individuais e JWT_SECRET conforme o README. Numa instalação vazia, essas credenciais criam as contas iniciais persistentes; não são reaplicadas após o primeiro arranque.
3. Crie uma branch por melhoria. Explique o problema, o comportamento pretendido e os limites da alteração.
4. Execute `mvn -B verify -f backend-spring-boot/pom.xml`, `npm ci` e `npm run build`. O último comando verifica TypeScript e compila a interface.
5. Abra um PR com os ficheiros afectados, resultados dos testes e screenshots quando alterar a interface. Informe testes que não conseguiu executar.
6. Aguarde CI verde e revisão humana antes do merge. Um PR dependente deve indicar a sua base; não copiar alterações já propostas noutro PR.

Os dados sintéticos exigem `EDM_SEED_ENABLED=true` numa base isolada. As simulações
exigem também `SPRING_PROFILES_ACTIVE=demo`. Desligar o seed não remove fixtures já
persistidas. Não executar pentests contra instalações de terceiros. Reportar
vulnerabilidades conforme SECURITY.md, sem publicar segredos.

Contribuições iniciais úteis: corrigir instruções que não funcionam, melhorar mensagens em português/inglês, documentar acessibilidade e reproduzir um erro com dados sintéticos. Confirme primeiro as issues e PRs abertos para evitar duplicação. Não apresentar funcionalidades planeadas como existentes.

## English

GridAudit is an independent prototype for investigating energy discrepancies. An alert is an indication for technical investigation, not proof of fraud. Use synthetic data only in examples, tests, screenshots and issues.

- Read README, ROADMAP, the progress log and SECURITY before making changes.
- Use Java 17, Maven, Node.js 20 and Docker for PostgreSQL/Testcontainers tests. Set the four distinct per-account passwords and JWT_SECRET documented in README. On an empty installation these credentials create the initial persistent accounts; they are not reapplied after first start.
- Work on a focused branch. Run `mvn -B verify -f backend-spring-boot/pom.xml`, `npm ci` and `npm run build`.
- Describe the problem, changed behaviour, limitations and actual test results in the PR. Include screenshots for UI changes and disclose checks you could not run.
- Require green CI and human review before merging. Clearly identify dependent PRs and avoid duplicate work.
- Synthetic fixtures require explicit `EDM_SEED_ENABLED=true` in an isolated database;
  demo mutations additionally require the `demo` profile. Disabling the seed does not
  remove previously stored fixtures. Follow SECURITY.md for vulnerability reports.
  Do not test third-party deployments without authorisation.

Good first contributions include reproducible documentation fixes, Portuguese/English wording and accessibility improvements. Check open issues and PRs first. Do not describe planned features as shipped.
