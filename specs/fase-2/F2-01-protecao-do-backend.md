# F2-01 — Proteção da API de cadastro (sentry-backend)

- **Fase:** 2
- **Status:** Proposto
- **Depende de:** F1-05, F1-08

## Contexto

Na Fase 1 o `sentry-backend` (cadastro de organizações, sistemas, usuários, papéis e ações) não exige autenticação: qualquer um que alcance a porta 8080 cria usuários, marca sistemas como confiáveis (`isTrusted`) e lê `access_logs`. O próprio Sentry já emite tokens; o `sentry-backend` passa a ser um resource server dele, e o sistema `SENTRY` criado pelo bootstrap (F0-03) vira o cliente OAuth das ferramentas de administração.

## Regras

- **RN-F2-01.1** Todo endpoint do `sentry-backend`, exceto `/actuator/health/**`, DEVE exigir um access token Bearer emitido pelo `sentry-auth` (`iss` = `SENTRY_ISSUER_URL`), validado pelo JWKS, com `typ=at+jwt` e `aud` = `SENTRY`.
- **RN-F2-01.2** O usuário do token (`sub`) DEVE ter, no momento da chamada, um papel ativo `SENTRY.ADMIN` no sistema `SENTRY` da organização da plataforma. A checagem DEVE ler o banco, não o token, para que retirar o papel corte o acesso na hora.
- **RN-F2-01.3** Sem token ou com token inválido a resposta DEVE ser `401` com `WWW-Authenticate: Bearer`; com token válido e sem o papel, `403`. As mensagens de erro continuam em Problem Details, em português.
- **RN-F2-01.4** Marcar ou desmarcar `isTrusted` de um sistema DEVE exigir `SENTRY.ADMIN` (decisão de F1-11) e DEVE gerar um registro de auditoria no log estruturado (quem, quando, sistema, valor novo).
- **RN-F2-01.5** As colunas `created_by` e `updated_by` DEVEM ser preenchidas com o `sub` do token (hoje ficam nulas, exceto no bootstrap).
- **RN-F2-01.6** O bootstrap DEVE configurar o sistema `SENTRY` como cliente `PUBLIC` com PKCE e escopos `openid profile`, com as URIs de redirecionamento vindas de `SENTRY_ADMIN_REDIRECT_URIS`. Sem a variável, o sistema fica sem URIs e só pode ser usado após cadastro manual.
- **RN-F2-01.7** O Swagger UI do `sentry-backend` DEVE funcionar autenticado pelo fluxo PKCE do próprio Sentry (cliente `SENTRY`), para que a API possa ser operada sem ferramenta externa.

## Critérios de aceite

- **CA-F2-01.1** Dada uma chamada a `GET /systems` sem `Authorization`, então a resposta é `401`.
- **CA-F2-01.2** Dado um token válido de um usuário sem `SENTRY.ADMIN`, então `GET /systems` responde `403`.
- **CA-F2-01.3** Dado um token do administrador, então `POST /systems` cria o sistema e `created_by` é o `sub` do token.
- **CA-F2-01.4** Dado um token emitido para outro sistema (`aud` ≠ `SENTRY`), então a resposta é `401`, mesmo que o usuário seja administrador.
- **CA-F2-01.5** Dado um administrador cujo papel foi desativado depois do login, então a chamada seguinte com o mesmo token responde `403`.
- **CA-F2-01.6** Dado `PUT /systems/{id}` mudando `isTrusted`, então o log tem um evento de auditoria com o `sub` e o novo valor.

## Notas técnicas

- `spring-boot-starter-security-oauth2-resource-server` no `sentry-backend`, com `issuer-uri` = `SENTRY_ISSUER_URL` (no Compose, o `sentry-backend` busca o JWKS pela rede interna; o `iss` continua sendo a URL pública).
- A checagem do papel reaproveita `RoleUserRepository.hasActiveRoleInSystem` (F1-02).
- `AuditorAware<String>` lendo o `sub` do `SecurityContext`.

## Questões em aberto

- Validar o papel pelo banco (sempre atual) ou por um claim de papéis no token (F2-06, sem consulta)? Recomendado: banco, porque a API de cadastro é de baixo volume e a revogação imediata importa mais.
- Haverá administradores por organização (só gerenciam os próprios sistemas e usuários)? Recomendado: não na Fase 2; só o `SENTRY.ADMIN` da plataforma. O escopo por organização entra com o multi-tenant.
- Console de administração (tela) na Fase 2? Recomendado: não; o Swagger UI autenticado (RN-F2-01.7) basta até lá.
