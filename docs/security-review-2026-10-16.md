# Revisão de segurança e dados — 16/10/2026

Âmbito: código e configuração do próprio GridAudit, base do PR #7 e proposta no PR #8.
Método: reconstrução do fluxo navegador → filtros → controller → serviço → repositório/BD,
com testes de regressão sobre as hipóteses encontradas. Não foi feito pentest numa
instalação externa, nem inspeccionados dados de clientes. Não certifica ausência de falhas.

## Correcções neste lote

| Constatação no código | Alteração | Evidência pretendida |
|---|---|---|
| `DataSeeder` corria em qualquer base vazia, incluindo PostgreSQL | `EDM_SEED_ENABLED=false` por defeito; opt-in apenas em demos/CI | Arranque PostgreSQL sem seed deixa `transformers` vazia; teste do opt-in |
| Não havia operação suportada de mudança de password | API e diálogo para a própria conta, exigindo password actual | Teste autenticado como OPERATOR, sem poder escolher outra conta |
| Password e tokens precisavam de mudança coordenada | Hash BCrypt + incremento `auth_version` + evento na mesma transacção | Token e password antigos recusados; nova password aceite; conta distinta preservada |
| Operações simultâneas podem usar o mesmo estado de sessão | Lock pessimista e revalidação do estado/versão dentro da transacção | Apenas uma mudança concorrente confirma; a outra é recusada |
| Nova rota oferece outro local para adivinhar password | Mesma janela por IP do login e contagem persistente por conta | Cinco falhas bloqueiam a conta; limite partilhado devolve 429 |
| Auditoria não deve guardar credenciais | Evento contém ID da conta, instante, acção e versão; DTOs omitem passwords no `toString` | Testes de rollback quando auditoria falha e de omissão dos segredos |

A migração V4 cria `account_security_event`, índice temporal e unicidade por conta/versão.
A aplicação só expõe inserção no repositório; um trigger PostgreSQL recusa UPDATE/DELETE.
O proprietário da BD continua privilegiado e pode alterar esquema/triggers: não é uma
garantia contra um administrador comprometido. O H2 não executa as migrações PostgreSQL.
Tokens de pedidos futuros são rejeitados após a rotação; pedidos já em execução não
podem ser retroactivamente cancelados. A revogação continua a ser por conta, não por sessão.

## Bloqueios para dados reais, por dependência

| Prioridade | Pendência confirmada | Próxima alteração verificável |
|---|---|---|
| P0 | PRs dependentes ainda não revistos/integrados em main | Revisão humana na ordem #2–#8 e CI de integração |
| P0 | Segredos antigos no histórico; sem evidência de rotação externa | Rotação pelo responsável e limpeza coordenada, sem reescrita automática |
| P0 | Framework/backend ainda na versão anterior à actualização do roadmap | Actualização com testes de regressão e scans de dependências |
| P1 | Contas de bootstrap fixas e só ADMIN/OPERATOR | Administração auditável, utilizadores reais e segregação de funções |
| P1 | Dados pessoais acessíveis por papel sem âmbito por área | Autorização por área e DTOs com minimização de dados |
| P1 | kWh em `Double`; leituras sem unicidade temporal e sem autor persistido | Migração para `NUMERIC`/BigDecimal, chave única e proveniência |
| P1 | Balanço usa valores agregados sem períodos compatíveis | Séries temporais, parâmetros versionados e resultados reproduzíveis |
| P1 | Sem cadastro completo da grelha, casos/revisão/decisão segregados | APIs e UI persistentes com regras no serviço e BD |
| P1 | Sem evidência de backup/restauro automatizado ou operação TLS real | Restauro em ambiente isolado, runbook e validação do ambiente de destino |

Limiar de 5% e valor de 7,50 MT/kWh permanecem demonstrativos. A estimativa pro-rata
não mede energia desviada individualmente nem autoriza cobranças. Dados para demonstração
permanecem sintéticos e o projecto é independente da EDM.

## Validação

Resultados efectivos, links de CI e proveniência das capturas ficam no PR #8 e no
[registo de progresso](roadmap-execucao.md). Testes adicionados não equivalem a testes
aprovados. Este lote não conclui uma fase nem autoriza produção.
