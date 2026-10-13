# Segurança do GridAudit

O projecto é um protótipo independente, sem afiliação à EDM. Não usar dados reais nem tomar decisões de cobrança com base nos alertas. Uma discrepância não constitui prova de irregularidade.

## Reportar uma vulnerabilidade

Contactar o mantenedor por um canal privado previamente combinado. Se estiver disponível no separador Security do GitHub, usar a opção de reporte privado. Não incluir credenciais, tokens, dados pessoais ou passos de exploração com dados reais numa issue pública.

## Limites actuais

- Ainda existem contas fixas em memória, agora com passwords individuais obrigatórias e distintas. A migração para utilizadores individuais em BD é obrigatória antes de um piloto.
- Cada pedido com token consulta o utilizador actual: contas removidas, desactivadas, bloqueadas ou expiradas não autenticam; as permissões vêm do servidor, não das claims antigas. Ainda não existe endpoint de administração de contas nem revogação persistente por sessão; alterar uma password não revoga por si só todos os tokens.
- Os tokens têm duração máxima de 15 minutos e ficam apenas na memória do navegador. Recarregar a página ou expirar a sessão exige novo login; refresh ainda não foi implementado.
- O login permite até 10 pedidos por minuto por IP remoto e por processo, com estado limitado a 4096 IPs. O limite também conta logins bem-sucedidos. Um proxy deve aplicar o seu próprio limite; múltiplas instâncias exigem estado partilhado. O backend ignora X-Forwarded-For nesta decisão.
- As rotas de simulação só são registadas com o perfil `demo`. Nunca activar esse perfil num ambiente com dados reais.
- Os cabeçalhos DENY/CSP configurados no backend protegem a API. O servidor que alojar o frontend deve ter a sua própria política CSP, compatível com os recursos efectivamente utilizados.
- PostgreSQL usa Flyway V1/V2 e Hibernate validate para as tabelas actuais. O modelo alvo completo, permissões por área, segregação de funções e auditoria imutável ainda não estão completos. Bases antigas exigem adopção específica; baseline automático e clean estão desactivados.
- A versão de Spring Boot existente ainda precisa de actualização e testes de regressão.

## Segredos e histórico

1. Revogar/rodar os segredos anteriormente publicados no fornecedor original. Apagar um ficheiro ou reescrever o histórico não revoga um segredo.
2. Inventariar branches, tags, forks e clones afectados, sem copiar os valores para issues ou logs.
3. Fazer cópia de segurança e coordenar com todos os colaboradores antes de reescrever histórico.
4. Preparar a limpeza numa cópia isolada com uma ferramenta apropriada; rever as referências afectadas.
5. Só publicar a reescrita após autorização explícita do mantenedor, devido ao impacto nos clones e PRs.
6. Verificar novamente todas as referências e activar secret scanning/push protection onde disponíveis.

Este PR não confirma rotação de segredos, não reescreve histórico e não altera configurações administrativas de segurança do GitHub.
