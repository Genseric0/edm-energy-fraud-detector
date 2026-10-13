# PostgreSQL: migrações versionadas

## Estado e âmbito

O perfil `postgres` usa Flyway para criar/evoluir o esquema e Hibernate com `ddl-auto=validate` para verificar a compatibilidade com as entidades. V1 cria as quatro tabelas da rede, V2 acrescenta índices e V3 cria as contas persistentes sem incluir passwords ou dados reais. `DOUBLE PRECISION` foi preservado para evitar uma conversão de valores implícita neste lote; BigDecimal/NUMERIC terá uma migração própria.

Isto não conclui a Fase 1: o modelo alvo completo, unicidade de leituras, precisão decimal e backup/restauro automatizado continuam pendentes. H2 é efémero e usa `create-drop`, com Flyway desactivado; não serve de validação das migrações PostgreSQL.

## Instalação nova (base vazia)

1. Criar uma base PostgreSQL 16 vazia com acesso limitado ao ambiente de desenvolvimento.
2. Configurar `SPRING_PROFILES_ACTIVE=postgres`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, as quatro passwords individuais e `JWT_SECRET`, conforme README.
3. Executar o backend. Flyway aplica V1–V3 antes de Hibernate validar o esquema.
4. Confirmar o histórico com uma conta autorizada:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;
```

5. Reiniciar: nenhuma migração deve ser reaplicada. Consultar a leitura anteriormente criada para confirmar persistência.

O DataSeeder existente ainda preenche uma rede sintética se a tabela de transformadores estiver vazia. Não ligar esta versão a uma base com dados reais. O perfil `demo` só é necessário para as rotas de mutação sintética; não é uma autorização para usar dados reais.

## Base antiga criada por Hibernate

Uma base não vazia e sem histórico Flyway é recusada por defeito. `baseline-on-migrate=false` é intencional: o arranque não deve assumir que tabelas desconhecidas correspondem a V1. `clean` também está desactivado.

Não apagar tabelas, activar baseline automático, usar `repair` ou regressar a `ddl-auto=update` para contornar esse erro.

Antes de migrar uma base antiga:

- Fazer backup, testar o restauro numa base isolada e interromper as escritas durante a transição.
- Comparar tabelas, tipos, nulabilidade, chaves, índices, sequences e dados existentes com V1–V3 e com as entidades.
- Preparar e rever um plano de adopção específico para essa base. Um baseline explícito só é válido se a equivalência tiver sido comprovada; este lote não oferece um script genérico de adopção.
- Executar primeiro a transição na cópia restaurada, validar registos e API, e obter revisão humana antes de aplicar ao original.

## Desenvolvimento e validação

Usar novas migrações `V4__...sql`, `V5__...sql`, etc. Nunca editar uma migração já aplicada. Checksums divergentes devem ser investigados, não ignorados.

`mvn -B verify -f backend-spring-boot/pom.xml` exige Docker para os testes Testcontainers. A ausência de Docker faz o teste falhar, sem substituição por H2 nem skip automático.

Os testes exercitam PostgreSQL 16 real: instalação limpa, repetição idempotente, evolução V1→V3 preservando leituras e vistorias, criação da tabela de contas, sequência de IDs, índices e recusa de esquema legado sem perda do registo de controlo. A CI visual executa adicionalmente o backend com Hibernate validate e confirma persistência após reinício.

## English summary

The PostgreSQL profile now uses Flyway migrations and Hibernate schema validation. Fresh empty databases migrate automatically; non-empty legacy schemas without Flyway history fail closed. Never enable automatic baseline or clean to bypass that failure. Back up, test restoration, compare the legacy schema and review a database-specific adoption plan first. Existing numeric types are preserved; decimal conversion and the full target model remain future work. Docker is required for the PostgreSQL Testcontainers tests.
