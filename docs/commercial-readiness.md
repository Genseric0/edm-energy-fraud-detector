# Critérios para uso por clientes

Estado: protótipo independente em evolução. Esta revisão local não é uma versão pronta
para venda/produção. O trabalho continua sujeito a publicação, CI e revisão humana.

## Ordem para um primeiro piloto

| Prioridade | Entrega | Critério de aceitação |
|---|---|---|
| 1 | Integrar a cadeia de alterações de segurança revista | PRs #2–#8 e nova proposta com CI e revisão humana; segredos antigos rodados pelo responsável |
| 2 | Contas reais e acesso por área | Convite/cadastro seguro, mudança inicial obrigatória, recuperação de acesso e matriz de testes papel × área × endpoint |
| 3 | Modelo de energia e proveniência | BigDecimal nos cálculos/totais, períodos compatíveis, parâmetros versionados e rejeição de duplicados/entradas inválidas no PostgreSQL |
| 4 | Processo completo de investigação | Alerta → caso → agenda → vistoria → revisão → decisão, com pessoas distintas e eventos auditáveis |
| 5 | Operação recuperável | HTTPS e configuração de produção verificados, limites no gateway, monitorização, backup e restauro demonstrado |
| 6 | Interface validada por utilizadores | Percurso por teclado/telemóvel, erros/vazio/carregamento, acessibilidade e testes com alguém além do autor |
| 7 | Actualização e manutenção | Framework/dependências actualizados com regressão, scans de segurança e processo de resposta a incidentes |

Para começar com uma organização, o alvo mais simples é uma implantação isolada por
cliente, com base, segredos e backups próprios. Essa implantação ainda não está pronta.
Um SaaS partilhado exigirá uma fronteira de organização em todas as consultas/escritas,
restrições na BD e testes de isolamento entre clientes. `service_area` resolve âmbito
operacional e não substitui isolamento entre organizações. Não há multitenancy implementado.

## Interface e produto

Já existem dashboard, detalhe de transformador, pesquisa, filtros, PDF demonstrativo e
mudança de password. A proposta local acrescenta administração de contas existentes,
alertas em cartões mobile, melhor tratamento de rede vazia e falhas de actualização.
Continuam pendentes cadastro/importação da grelha, formulários de leituras/vistorias,
casos, agenda, revisão e decisão. PWA offline, hardware AMI e notificações não existem
como integrações de produção e não devem ser anunciados como funcionalidades disponíveis.

Validar o primeiro piloto com uma organização e dados autorizados só depois dos critérios
técnicos acima. Definir com o proprietário do produto o âmbito, suporte e modelo comercial.
Não há preço, número de clientes, alcance ou receita comprovados nesta revisão.

Limiar e valores monetários continuam demonstrativos. Um indício não comprova fraude;
estimativas pro-rata não são medições individuais nem autorizam multas automáticas.
