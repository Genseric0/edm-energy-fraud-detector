# Contribuir / Contributing

## Português

O GridAudit é um protótipo independente para estudar discrepâncias de energia. Um alerta é um indício para investigação técnica, não prova de fraude. Não usar dados pessoais reais em exemplos, testes, screenshots ou issues.

1. Leia o [README](README.md), [ROADMAP](ROADMAP.md), [progresso](docs/roadmap-execucao.md) e [SECURITY](SECURITY.md).
2. Use Java 17, Maven e Node.js 20. Configure as quatro passwords individuais e JWT_SECRET conforme o README. O protótipo ainda usa contas fixas em memória; PostgreSQL não torna essas contas persistentes.
3. Crie uma branch por melhoria. Explique o problema, o comportamento pretendido e os limites da alteração.
4. Execute `mvn -B verify -f backend-spring-boot/pom.xml`, `npm ci` e `npm run build`. O último comando verifica TypeScript e compila a interface.
5. Abra um PR com os ficheiros afectados, resultados dos testes e screenshots quando alterar a interface. Informe testes que não conseguiu executar.
6. Aguarde CI verde e revisão humana antes do merge. Um PR dependente deve indicar a sua base; não copiar alterações já propostas noutro PR.

As simulações exigem `SPRING_PROFILES_ACTIVE=demo` e dados sintéticos. Não executar pentests contra instalações de terceiros. Reportar vulnerabilidades conforme SECURITY.md, sem publicar segredos.

Contribuições iniciais úteis: corrigir instruções que não funcionam, melhorar mensagens em português/inglês, documentar acessibilidade e reproduzir um erro com dados sintéticos. Confirme primeiro as issues e PRs abertos para evitar duplicação. Não apresentar funcionalidades planeadas como existentes.

## English

GridAudit is an independent prototype for investigating energy discrepancies. An alert is an indication for technical investigation, not proof of fraud. Use synthetic data only in examples, tests, screenshots and issues.

- Read README, ROADMAP, the progress log and SECURITY before making changes.
- Use Java 17, Maven and Node.js 20. Set the four distinct per-account passwords and JWT_SECRET documented in README. Accounts are still held in memory, including when PostgreSQL is enabled.
- Work on a focused branch. Run `mvn -B verify -f backend-spring-boot/pom.xml`, `npm ci` and `npm run build`.
- Describe the problem, changed behaviour, limitations and actual test results in the PR. Include screenshots for UI changes and disclose checks you could not run.
- Require green CI and human review before merging. Clearly identify dependent PRs and avoid duplicate work.
- Demo mutations require the `demo` profile and synthetic data. Follow SECURITY.md for vulnerability reports. Do not test third-party deployments without authorisation.

Good first contributions include reproducible documentation fixes, Portuguese/English wording and accessibility improvements. Check open issues and PRs first. Do not describe planned features as shipped.
