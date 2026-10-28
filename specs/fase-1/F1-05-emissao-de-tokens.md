# F1-05 — Emissão de access token e ID token

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-03, F1-07

## Contexto

O contrato atual devolve `userToken` e `applicationToken` sem formato definido. Na Fase 1 eles são substituídos pelos tokens padrão do OAuth/OIDC.

## Regras

- **RN-F1-05.1** O access token DEVE ser um JWT assinado com RS256 (D-03), com `typ` = `at+jwt` (RFC 9068).
- **RN-F1-05.2** Claims do access token: `iss`, `sub`, `aud`, `client_id`, `exp`, `iat`, `jti`, `scope`, `org` (uuid da organização do sistema).
- **RN-F1-05.3** O ID token DEVE seguir o OpenID Connect Core: `iss`, `sub`, `aud` (= `client_id`), `exp`, `iat`, `auth_time`, `nonce` (quando enviado), `sid`; com escopo `profile`, `name` e `preferred_username`; com escopo `email`, `email`.
- **RN-F1-05.4** `sub` DEVE ser o `uuid` do usuário, nunca o `id_user` interno nem o username.
- **RN-F1-05.5** `iss` DEVE ser o `SENTRY_ISSUER_URL` (F0-04).
- **RN-F1-05.6** `aud` do access token DEVE ser o `client_id` do sistema na Fase 1.
- **RN-F1-05.7** Vida padrão: access token 15 minutos, ID token 15 minutos, configuráveis por instalação.
- **RN-F1-05.8** Tokens NÃO DEVEM conter senha, hash, e-mail no access token, nem papéis/ações (autorização fica fora deste escopo).
- **RN-F1-05.9** A resposta do `/oauth2/token` DEVE ter `Cache-Control: no-store`.

## Critérios de aceite

- **CA-F1-05.1** Dado um login concluído, quando o access token é decodificado, então tem todos os claims de RN-F1-05.2, `alg=RS256` e `kid` presente no JWKS.
- **CA-F1-05.2** Dado um login com `nonce=abc`, então o ID token tem `nonce=abc`.
- **CA-F1-05.3** Dado um login sem escopo `email`, então o ID token não tem `email`.
- **CA-F1-05.4** Dado um access token emitido há 16 minutos, quando validado por uma biblioteca JWT padrão com o JWKS, então é rejeitado por expiração.
- **CA-F1-05.5** Dado qualquer token emitido, então `sub` é um UUID igual a `users.uuid`.

## Questões em aberto

- Incluir o claim `org` já na Fase 1? Recomendado: sim, é barato e evita mudar o contrato quando chegar o multi-tenant.
