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

Podes inspecionar os dados diretamente em **http://localhost:8080/h2-console**
(JDBC URL: `jdbc:h2:mem:energydb`, user: `sa`, password: em branco).

## Como correr — Frontend (React)

Num segundo terminal, a partir da raiz do projeto:

```bash
npm install
npm run dev
```

O frontend arranca em **http://localhost:3000** (ou a porta seguinte livre, o Vite indica no terminal).

**Importante:** o backend tem de estar a correr primeiro, senão o dashboard mostra o ecrã
de erro de ligação ("Não foi possível contactar o servidor").

## Credenciais de acesso

O sistema tem autenticação real (JWT + BCrypt), validada no backend — não há passwords
visíveis no código-fonte do frontend.

| Utilizador | Password | Papel |
|---|---|---|
| `genserico` | `EdmMz#2026!Adm` | ADMIN |
| `oxidia` | `EdmMz#2026!Adm` | ADMIN |
| `leysa` | `EdmMz#2026!Op` | OPERATOR |
| `helen` | `EdmMz#2026!Op` | OPERATOR |

A tela de login tem botões de acesso rápido para cada um destes utilizadores.

**Diferença entre papéis:**
- **ADMIN** (Genserico, Oxidia): pode simular ligação clandestina e regularizar setores, além de consultar tudo
- **OPERATOR** (Leysa, Helen): acesso de consulta a todos os dados e relatórios, sem ações de simulação

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
