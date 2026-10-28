# 00 — Escopo, atores e convenções

## Objetivo das Fases 0 e 1

Ao fim da Fase 1, um sistema externo cadastrado no Sentry consegue autenticar um usuário pelo fluxo padrão OAuth 2.1 / OpenID Connect e receber tokens válidos, usando qualquer biblioteca OIDC genérica, sem SDK do Sentry.

## Atores

| Ator | Quem é | Onde está no modelo atual |
| --- | --- | --- |
| Usuário | Pessoa que se autentica | Tabela `users` |
| Sistema externo | Aplicação que pede a autenticação do usuário (cliente OAuth) | Tabela `systems` |
| Organização | Dona do sistema externo | Tabela `organizations` |
| sentry-auth | Servidor de autorização OAuth e provedor OpenID | Módulo `sentry-auth` (porta 8081) |

## Glossário

- **Cliente OAuth**: o sistema externo, identificado por `client_id`.
- **Cliente confidencial**: sistema com backend, que guarda um `client_secret`.
- **Cliente público**: SPA ou app mobile, sem segredo; obrigatoriamente usa PKCE.
- **PKCE**: prova de posse do código de autorização (RFC 7636), sempre com método `S256`.
- **Access token**: JWT de vida curta que o sistema externo envia às suas APIs.
- **ID token**: JWT do OpenID Connect com a identidade do usuário, consumido pelo sistema externo.
- **Refresh token**: token opaco para obter um novo access token sem novo login.
- **Sistema confiável**: sistema externo autorizado a usar o login direto (F1-11), porque coleta a senha do usuário na própria tela.
- **Sessão SSO**: sessão do usuário no próprio sentry-auth, mantida por cookie.

## Fora de escopo das Fases 0 e 1

- Autorização: papéis e ações no token, serviço `sentry-authorization`.
- MFA, recuperação de senha, bloqueio por tentativas (Fase 2).
- Login via LDAP e login social.
- Client Credentials (máquina a máquina) e introspection.
- Console de administração e SDKs.
- Cadastro de usuários e sistemas por tela: nas Fases 0 e 1, via API do `sentry-backend` ou SQL de bootstrap.

## Convenções da spec

- **IDs**: requisito `F<fase>-<nº>`; regra `RN-F<fase>-<nº>.<n>`; critério de aceite `CA-F<fase>-<nº>.<n>`.
- **Palavras normativas**: DEVE, NÃO DEVE, PODE (como MUST, MUST NOT, MAY da RFC 2119).
- **Critérios de aceite** em Dado / Quando / Então. Cada critério vira pelo menos um teste (convenção do `../CLAUDE.md`).
- **Status** de cada arquivo: `Proposto` → `Aprovado` → `Implementado`; `Ajustar` quando a revisão pede mudança.
- **Código** em inglês e mensagens de erro ao usuário em português, como no `../CLAUDE.md` do repositório.

## Decisões técnicas assumidas

| ID | Decisão | Motivo |
| --- | --- | --- |
| D-01 | Dois caminhos de login: Authorization Code + PKCE com tela hospedada como padrão (F1-03), e o login direto por API (`POST /api/v1/auth/login`, sucessor do `POST /login` atual) apenas para sistemas marcados como confiáveis (F1-11) | Decidido pelo Julian em 28/10/2026. PKCE é o padrão OAuth 2.1; o login direto preserva o contrato atual para sistemas próprios |
| D-02 | Usar o Authorization Server do Spring Security (Spring Authorization Server) em vez de implementar os fluxos à mão | Implementação mantida e testada dos endpoints OAuth/OIDC |
| D-03 | Tokens JWT assinados com RS256 | Suporte universal nas bibliotecas OIDC |
| D-04 | Hash de senha Argon2id | Recomendação atual da OWASP |
| D-05 | Banco único PostgreSQL com migrações Flyway no `sentry-api` | Já é a estrutura do repositório |
