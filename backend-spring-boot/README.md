# Backend — Sistema de Detecção de Perdas Não Técnicas (Spring Boot + H2)

API REST em **Java 17 / Spring Boot 3.2**, com base de dados em memória **H2**, implementando
o balanço energético por transformador e a autenticação da equipa EDM.

> Para instruções de arranque completas (backend + frontend) ver o `README.md` na raiz do projeto.

## Regra de Negócio

1. **Balanço Energético por Setor:**
   `Perda (kWh) = Energia Fornecida pelo Transformador − Σ (Energia Faturada dos Medidores)`
2. **Percentual de Perda:**
   `% Perda = (Perda (kWh) / Energia Fornecida) × 100`
3. **Classificação de Setores:**
   - `≤ 5.0%` → `Normal: Perda Técnica Aceitável` (dissipação Joule normal em condutores)
   - `> 5.0%` → `Alerta Crítico: Suspeita de Ligação Clandestina`

## Estrutura de Pastas

```
backend-spring-boot/
├── pom.xml
└── src/main/
    ├── java/com/energy/detection/
    │   ├── EnergyTheftDetectionApplication.java
    │   ├── config/
    │   │   ├── DataSeeder.java        # Popula H2 com dados de demo (Maputo)
    │   │   └── SecurityConfig.java    # RBAC, BCrypt, sessão stateless
    │   ├── controller/
    │   │   ├── AuthController.java    # Login e emissão de JWT
    │   │   └── EnergyController.java  # Endpoints de análise energética
    │   ├── dto/
    │   ├── model/
    │   ├── repository/
    │   ├── security/
    │   │   ├── JwtUtil.java           # Geração e validação de tokens
    │   │   └── JwtAuthFilter.java     # Intercepta e valida token em cada pedido
    │   └── service/
    │       └── EnergyAnalysisService.java
    └── resources/
        └── application.properties
```

## Como Rodar no IntelliJ IDEA

1. **File > Open...** e selecionar a pasta `backend-spring-boot` (ou o `pom.xml`)
2. **Open as Project**
3. Aguardar o IntelliJ resolver as dependências Maven
4. Confirmar JDK 17+ em *File > Project Structure > SDK*
5. Correr `EnergyTheftDetectionApplication.java` (Shift+F10)

A aplicação sobe na porta **8080**.

## Autenticação (JWT)

Todos os endpoints de `/api/energy/**` exigem um token JWT válido, exceto onde indicado.
O token é obtido em `/api/auth/login` e deve ser reenviado no cabeçalho:

```
Authorization: Bearer {token}
```

Utilizadores disponíveis (ver `SecurityConfig.java`): `genserico`, `oxidia` (ADMIN),
`leysa`, `helen` (OPERATOR). As passwords são definidas através de variáveis de ambiente; consultar o `README.md` da raiz.

## Endpoints REST

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | Pública | Autentica e devolve o token JWT |
| `GET` | `/api/auth/me` | Bearer token | Confirma a sessão ativa |
| `GET` | `/api/energy/summary` | ADMIN ou OPERATOR | Cards do dashboard |
| `GET` | `/api/energy/transformers` | ADMIN ou OPERATOR | Lista de transformadores com balanço |
| `GET` | `/api/energy/alerts` | Apenas ADMIN | Transformadores em Alerta Crítico |
| `GET` | `/api/energy/transformers/{id}` | ADMIN ou OPERATOR | Detalhe/análise de um transformador |
| `GET` | `/api/energy/transformers/{id}/details` | ADMIN ou OPERATOR | Detalhe + medidores com perda individualizada |
| `GET` | `/api/energy/transformers/{id}/meters` | ADMIN ou OPERATOR | Medidores conectados |
| `POST` | `/api/energy/transformers/{id}/simulate-theft` | Apenas ADMIN | Simula ligação clandestina |
| `POST` | `/api/energy/transformers/{id}/normalize` | Apenas ADMIN | Regulariza o setor |

## Console do H2 Database

A consola está desligada por defeito. Para depuração local, activar `H2_CONSOLE_ENABLED=true`; não expor em redes públicas.

- **URL:** `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:energydb`
- **User:** `sa` — **Password:** *(em branco)*

## Limitações conhecidas

Não existem endpoints `POST /api/energy/transformers` (novo transformador) nem
`POST /api/energy/transformers/{id}/meters` (novo medidor). Ver a secção
"Limitações conhecidas" do README principal.
