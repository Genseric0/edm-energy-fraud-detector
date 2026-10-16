# Segurança do GridAudit

O projecto é um protótipo independente, sem afiliação à EDM. Não usar dados reais nem tomar decisões de cobrança com base nos alertas. Uma discrepância não constitui prova de irregularidade.

## Reportar uma vulnerabilidade

Contactar o mantenedor por um canal privado previamente combinado. Se estiver disponível no separador Security do GitHub, usar a opção de reporte privado. Não incluir credenciais, tokens, dados pessoais ou passos de exploração com dados reais numa issue pública.

## Limites actuais

- As contas iniciais são persistidas com hash BCrypt e passwords individuais obrigatórias e distintas. A própria conta pode mudar a password pela interface/API, confirmando a actual. Administração de contas reais, recuperação de acesso, MFA e revogação por sessão permanecem pendentes.
- Cada pedido com token consulta o utilizador actual: contas removidas, desactivadas, bloqueadas ou expiradas não autenticam; as permissões vêm do servidor, não das claims antigas. `POST /api/auth/password` muda o hash e incrementa `auth_version` atomicamente com o evento de auditoria; tokens antigos deixam de autenticar nos pedidos seguintes. Uma edição directa do hash na BD não incrementa automaticamente a versão e não é o fluxo suportado. Pedidos já em execução não podem ser retroactivamente cancelados.
- Os tokens têm duração máxima de 15 minutos e ficam apenas na memória do navegador. Recarregar a página ou expirar a sessão exige novo login; refresh ainda não foi implementado.
- Login e mudança de password partilham até 10 pedidos por minuto por IP remoto e por processo, com estado limitado a 4096 IPs. O limite também conta operações bem-sucedidas. Falhas da password actual contam para bloqueio persistente da conta. Um proxy deve aplicar o seu próprio limite; múltiplas instâncias exigem estado partilhado. O backend ignora X-Forwarded-For nesta decisão.
- As rotas de simulação só são registadas com o perfil `demo`. Nunca activar esse perfil num ambiente com dados reais.
- Fixtures sintéticas exigem `EDM_SEED_ENABLED=true`; ficam desactivadas por defeito em H2 e PostgreSQL. Desligar esta opção não remove dados previamente populados. As quatro contas de bootstrap continuam a ser criadas numa base de contas vazia e não são cadastro de utilizadores reais.
- Os cabeçalhos DENY/CSP configurados no backend protegem a API. O servidor que alojar o frontend deve ter a sua própria política CSP, compatível com os recursos efectivamente utilizados.
- PostgreSQL usa Flyway V1–V4 e Hibernate validate, incluindo contas e eventos mínimos de mudança de password. Estes eventos não guardam passwords, hashes, tokens ou IP. A aplicação só os insere; trigger recusa UPDATE/DELETE no PostgreSQL. O proprietário da BD continua privilegiado. O modelo alvo completo, permissões por área, segregação de funções e auditoria de todos os processos permanecem pendentes. Bases antigas exigem adopção específica; baseline automático e clean estão desactivados.
- A versão de Spring Boot existente ainda precisa de actualização e testes de regressão.

## Segredos e histórico

1. Revogar/rodar os segredos anteriormente publicados no fornecedor original. Apagar um ficheiro ou reescrever o histórico não revoga um segredo.
2. Inventariar branches, tags, forks e clones afectados, sem copiar os valores para issues ou logs.
3. Fazer cópia de segurança e coordenar com todos os colaboradores antes de reescrever histórico.
4. Preparar a limpeza numa cópia isolada com uma ferramenta apropriada; rever as referências afectadas.
5. Só publicar a reescrita após autorização explícita do mantenedor, devido ao impacto nos clones e PRs.
6. Verificar novamente todas as referências e activar secret scanning/push protection onde disponíveis.

Este PR não confirma rotação de segredos, não reescreve histórico e não altera configurações administrativas de segurança do GitHub.
