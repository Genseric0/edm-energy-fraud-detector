# Qualidade dos registos e interface — proposta local de 16/10/2026

Código funcional local `5b1d5b6`, sobre a proposta administrativa `8fbb686` e a base
remota do PR #8 (`48ff136`). Não publicado; GitHub voltou a recusar escrita com 403.

## Problemas encontrados e correcções

| Problema no fluxo reconstruído a partir do código | Alteração |
|---|---|
| Leituras/estimativas usavam Double e apenas validação de sinal | BigDecimal, até 11 dígitos inteiros e 3 decimais; valores não finitos, negativos e fora do intervalo recusados |
| Uma repetição de pedido podia criar duas leituras do mesmo contador/instante/origem | Chave única na BD, com resposta 409; teste de inserção concorrente |
| PostgreSQL guarda microssegundos, mas a API aceitava nanossegundos | Recusa explícita de precisão temporal superior, evitando arredondamento silencioso |
| Não havia autor persistido da ingestão | Novas leituras recebem username da sessão; campos do cliente não podem forjar autor/origem |
| Não existe autor confirmado dos registos antigos | V6 deixa ingested_by NULL; não inventa proveniência |
| Tabelas de alertas cortavam informação no telemóvel | Cartões mobile com fornecido, faturado, discrepância, taxa e acção; tabela preservada em desktop |
| Rede vazia mostrava métricas/gráfico sem contexto | Estado vazio explícito e recarregamento |
| Falha ao actualizar o resumo era silenciosa | Aviso de resumo possivelmente desactualizado; recarregamentos antigos não substituem os mais recentes |

V6 verifica valores existentes e duplicados antes de converter leituras/estimativas para
NUMERIC(14,3). Dados inválidos bloqueiam a migração para reconciliação revista. Não apaga
duplicados nem arredonda precisão excedente. A migração precisa de teste em PostgreSQL
e de um ensaio numa cópia restaurada; não foi aplicada em dados do utilizador.

## Validação efectiva

- Java 17, Maven 3.9.9: compilação de produção e de todos os testes aprovada.
- 65 testes locais seleccionados: zero falhas/erros/ignorados. Incluem 13 fluxos H2
  com filtros JWT, autorização, reautenticação administrativa, revogação, concorrência,
  exactidão decimal e rejeição de duplicados/valores inválidos.
- Classes PostgreSQL excluídas explicitamente desta execução local:
  `PostgresMigrationTest`, `AccountPersistenceTest`, `AccountAdministrationTest`.
  Estão compiladas, mas não executadas. A ausência de Docker não é apresentada como sucesso.
- TypeScript/Vite, sintaxe E2E e diff aprovados. Mantém-se aviso de tamanho do bundle.
- Mockito usa subclass mock maker, sem necessitar de anexar um agente à JVM local.
- Tentativa de navegador bloqueada pelo ambiente: criação de socket não permitida.
  Novas capturas e validação visual permanecem pendentes; não foram criados mockups.

## Limites de dados e segurança

Leituras manuais ainda não alimentam um balanço temporal reproduzível. Totais de rede,
contadores e cálculos agregados mantêm Double; esta não é a conclusão da migração decimal.
Estimativas de vistoria continuam preliminares, sem multas e sem prova de fraude.
Ainda faltam períodos compatíveis, validação de sequência/consumo, revisão independente,
parâmetros versionados, permissões por área e isolamento entre organizações.

Prioridade seguinte: restabelecer escrita GitHub, publicar branch/PR sobre #8, executar
PostgreSQL/CodeQL/Playwright, corrigir falhas e actualizar capturas reais com origem.
Depois: contas novas com convite/mudança obrigatória de password, âmbito por área,
modelo temporal e backup/restauro testado. Nenhuma fase é marcada como concluída.
