# GridAudit EDM — Sistema de Detecção de Perdas Não Técnicas e Ligações Clandestinas

Sistema de auditoria energética para a Electricidade de Moçambique (EDM), focado na
identificação de **Perdas Não Técnicas (PNT)** — roubo de energia através de ligações
clandestinas ("gatos") e adulteração de contadores.

**Metodologia:** balanço energético por transformador. Para cada posto de transformação (PT),
o sistema compara a energia efetivamente **fornecida** (medida no secundário do transformador)
com a soma da energia **faturada** pelos medidores residenciais a jusante. Uma discrepância
acima do limiar técnico de **5%** (norma EDM) é sinalizada como Alerta Crítico — indício de
ligação clandestina no circuito.

## Arquitetura

```
projeto/
├── backend-spring-boot/     Java 17 + Spring Boot 3.2 — API REST + motor de cálculo
│   └── H2 em memória (arranca já com dados de demonstração de Maputo)
└── src/                     React 19 + TypeScript + Vite — Dashboard
```

O frontend comunica com o backend via `/api/*`, encaminhado pelo proxy do Vite para
`http://localhost:8080` durante o desenvolvimento — não há problemas de CORS a configurar.

## Pré-requisitos

- **Java 17** ou superior (para o backend)
- **Node.js 18+** e **npm** (para o frontend)
- **Maven** (ou usar o `mvnw` incluído no projeto, se presente no IntelliJ)

## Como correr — Backend (Spring Boot)

### Opção A — IntelliJ IDEA (recomendado para a apresentação)

1. Abrir a pasta `backend-spring-boot/` como projeto Maven no IntelliJ
2. Aguardar o IntelliJ resolver as dependências (primeira vez pode demorar)
3. Correr a classe `EnergyTheftDetectionApplication.java` (botão ▶ verde)
4. Confirmar no terminal do IntelliJ que aparece: `Tomcat started on port 8080`

### Opção B — Terminal

```bash
cd backend-spring-boot
mvn spring-boot:run
```

O backend arranca em **http://localhost:8080**, com a base de dados H2 em memória já
populada com 6 transformadores e ~41 medidores de bairros reais de Maputo (Zimpeto,
Xipamanine, Mafalala, etc.) — ver `DataSeeder.java`.

A consola H2 está desactivada por defeito; para depuração local, configure `H2_CONSOLE_ENABLED=true` e visite `http://localhost:8080/h2-console` apenas na máquina de desenvolvimento.

## Como correr — Frontend (React)

Num segundo terminal, a partir da raiz do projeto:

```bash
npm install
npm run dev
```

O frontend arranca em **http://localhost:3000** (ou a porta seguinte livre, o Vite indica no terminal).

**Importante:** o backend tem de estar a correr primeiro, senão o dashboard mostra o ecrã
de erro de ligação ("Não foi possível contactar o servidor").

## Configuração segura de acesso

O backend utiliza JWT e BCrypt. As passwords **não são incluídas no repositório**.
Antes de iniciar o backend, configure estas variáveis de ambiente:

| Variável | Finalidade |
|---|---|
| `EDM_GENSERICO_PASSWORD` | Password individual de `genserico` (ADMIN) |
| `EDM_OXIDIA_PASSWORD` | Password individual de `oxidia` (ADMIN) |
| `EDM_LEYSA_PASSWORD` | Password individual de `leysa` (OPERATOR) |
| `EDM_HELEN_PASSWORD` | Password individual de `helen` (OPERATOR) |
| `JWT_SECRET` | Segredo aleatório com pelo menos 32 bytes para assinatura HS256 |
| `EDM_CORS_ORIGIN` | Origem permitida do frontend (por defeito `http://localhost:3000`) |
| `H2_CONSOLE_ENABLED` | `true` somente quando for preciso depurar localmente (por defeito `false`) |

Exemplo em Linux/macOS:

```bash
export EDM_GENSERICO_PASSWORD="$(openssl rand -hex 24)"
export EDM_OXIDIA_PASSWORD="$(openssl rand -hex 24)"
export EDM_LEYSA_PASSWORD="$(openssl rand -hex 24)"
export EDM_HELEN_PASSWORD="$(openssl rand -hex 24)"
export JWT_SECRET="$(openssl rand -hex 32)"
cd backend-spring-boot
mvn spring-boot:run
```

No PowerShell, defina os mesmos valores em `$env:EDM_GENSERICO_PASSWORD`, `$env:EDM_OXIDIA_PASSWORD`, `$env:EDM_LEYSA_PASSWORD`, `$env:EDM_HELEN_PASSWORD` e `$env:JWT_SECRET` antes de executar Maven. Não coloque valores reais em ficheiros versionados.

Cada password deve ser diferente, ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8. As antigas variáveis por papel deixam de ser aceites; é necessário configurar as quatro variáveis individuais antes de actualizar. Os exemplos geram valores novos a cada execução.

**Utilizadores de demonstração:** `genserico` e `oxidia` (ADMIN), `leysa` e `helen` (OPERATOR). Estes nomes não são contas de produção. O acesso rápido com passwords embutidas foi removido.

As credenciais anteriormente publicadas devem ser consideradas comprometidas e substituídas. Se o sistema já esteve acessível a terceiros, substitua também o segredo JWT e invalide sessões anteriores.

## Funcionalidades disponíveis

- Dashboard com resumo agregado (energia fornecida, faturada, perdida, alertas críticos)
- Gráfico comparativo de discrepância por transformador
- Tabela de auditoria com filtro por status (crítico/normal) e busca
- Detalhe por transformador: cálculo individualizado de perda pro-rata por medidor,
  com diagnóstico de suspeita (`Suspeita de Bypass`, `Consumo Elevado`, `Regular`)
- Exportação de relatório de auditoria em PDF por transformador
- Simulação de ligação clandestina e regularização de setor (apenas ADMIN) — chama o
  backend real, que recalcula o balanço energético

## Limitações conhecidas (próximos passos)

- **Cadastro de novo transformador/setor** e **cadastro de novo medidor** ainda não têm
  endpoint no backend (`POST /api/energy/transformers` e `POST /api/energy/transformers/{id}/meters`
  não existem em `EnergyController.java`). Os formulários correspondentes foram removidos
  do frontend por esse motivo, para não simular funcionalidade que não persiste.
- Base de dados em memória (H2): os dados voltam ao estado inicial (`DataSeeder.java`)
  sempre que o backend reinicia. Para persistência real, migrar para MySQL/PostgreSQL
  (mudar `spring.datasource.url` e o driver em `application.properties`/`pom.xml`).
- Utilizadores geridos em memória no `SecurityConfig.java` — numa evolução futura,
  passariam para uma tabela `utilizador` própria, com hashing feito no cadastro em vez
  de fixo no arranque.

## Stack técnica

**Backend:** Java 17, Spring Boot 3.2, Spring Security, JWT (JJWT 0.12.6), BCrypt, H2, Lombok, Maven
**Frontend:** React 19, TypeScript, Vite, Tailwind CSS, Recharts, jsPDF

## Smart Metering — fase inicial

A base de dados distingue a localização física dos contadores: `POLE` (poste), `INSIDE_HOME` (interior da residência), `OUTSIDE_HOME` (exterior) ou `UNKNOWN` (não confirmado). O campo `remoteReadingEnabled` indica disponibilidade de telemedição, sem assumir que existe integração real.

Uma tabela `meter_readings` guarda leituras históricas com data, energia em kWh e origem. A API disponibiliza:

- `GET /api/energy/meters/{meterId}/readings`: consulta as últimas 100 leituras, para utilizadores autenticados.
- `POST /api/energy/meters/{meterId}/readings`: entrada **manual**, exclusiva a ADMIN, com JSON `{"recordedAt":"2026-10-12T07:00:00Z","energyKwh":125.5}`.

Nesta fase, os registos têm origem `MANUAL`. Ainda não existe integração com hardware, protocolo AMI, leitura automática ou validação metrológica. A base H2 continua a ser volátil: os históricos desaparecem quando se reinicia a aplicação.

**Importante:** variações de consumo e discrepâncias por transformador são indícios para investigação. A posição do contador e a análise estatística não comprovam que houve bypass ou fraude; é indispensável confirmação técnica. O limiar de 5% e a tarifa de 7,50 MT/kWh são valores de demonstração, não parâmetros oficiais validados.

Os testes de compilação e de API executam automaticamente no GitHub Actions após alterações e diariamente por agendamento; a confirmação deve ser feita no separador Actions do repositório.

## PostgreSQL persistente (desenvolvimento local)

O H2 continua a ser o perfil de demonstração por defeito. Para guardar os dados após reiniciar o servidor, utilizar PostgreSQL 16 com Docker.

1. Ter Docker Desktop instalado e iniciado. Criar um ficheiro `.env` **local** na raiz com `DB_PASSWORD` definido com valor privado forte (não publicar o ficheiro).
2. Na raiz do repositório executar `docker compose up -d postgres`.
3. No PowerShell, definir `$env:SPRING_PROFILES_ACTIVE="postgres"`, `$env:DB_PASSWORD="<a mesma password configurada no .env>"` (substituindo pelo valor local), além de as quatro passwords individuais da tabela acima e `JWT_SECRET`.
4. Executar `cd backend-spring-boot` e `mvn spring-boot:run`.
5. Reiniciar o backend e consultar novamente `GET /api/energy/meters/{meterId}/readings` para confirmar a persistência.

A configuração está em `application-postgres.properties`. Os dados ficam no volume Docker `gridaudit_pgdata`, que sobrevive a `docker compose down` (mas **não** a `docker compose down -v`).

**Atenção:** o esquema ainda usa `hibernate.ddl-auto=update` apenas nesta fase experimental; não usar como migração segura em produção. Fazer backups e introduzir migrações versionadas antes de operar com dados reais. Contas e dados demonstrativos do DataSeeder não são dados reais de clientes EDM.

## Roadmap e segurança

O [roadmap de conclusão](ROADMAP.md) define as fases restantes. Consultar o [registo de execução](docs/roadmap-execucao.md) para distinguir o que foi implementado das pendências e o [SECURITY.md](SECURITY.md) para os limites do protótipo.

Os comandos de simulação exigem agora o perfil `demo`; nunca o activar com dados reais. A sessão do navegador não é persistida: recarregar a página ou expirar o token (15 minutos) exige novo login. O login está limitado a 10 pedidos/minuto por IP e por instância.

Para contribuir, consulte [CONTRIBUTING.md](CONTRIBUTING.md) (português / English).
