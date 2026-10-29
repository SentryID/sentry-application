# Integração com o Sentry

Como cadastrar um sistema externo e autenticar usuários por ele. O Sentry é um provedor OpenID Connect padrão: qualquer
biblioteca OIDC funciona configurada só com o **issuer** e o **client_id**, sem SDK próprio.

Nos exemplos, o `sentry-auth` está em `http://localhost:8081` (o issuer, `SENTRY_ISSUER_URL`) e o `sentry-backend` em
`http://localhost:8080`.

## 1. Cadastrar o sistema

O sistema é criado no `sentry-backend`, na organização dona dele (`POST /systems`). O `externalCode` é o `client_id` e
não muda depois.

```bash
curl -X POST localhost:8080/systems -H 'Content-Type: application/json' -d '{
  "idOrganization": 1,
  "externalCode": "PORTAL",
  "description": "Portal do Cliente",
  "url": "https://portal.exemplo.com",
  "scopes": "openid profile email offline_access",
  "clientType": "CONFIDENTIAL",
  "redirectUris": ["https://portal.exemplo.com/callback"],
  "postLogoutRedirectUris": ["https://portal.exemplo.com/"],
  "isTrusted": false,
  "isActive": true
}'
```

| Campo | Regras |
| --- | --- |
| `clientType` | `CONFIDENTIAL` para sistemas com backend (recebem segredo); `PUBLIC` para SPA e apps mobile (sem segredo) |
| `scopes` | Só `openid`, `profile`, `email` e `offline_access`, separados por espaço |
| `redirectUris`, `postLogoutRedirectUris` | Comparadas por igualdade exata. Só `https`; em desenvolvimento, `http://127.0.0.1:<porta>` (`localhost` não é aceito, como no OAuth 2.1) e sem fragmento (`#`) |
| `isTrusted` | Libera o login direto (seção 3). Só para sistemas próprios que coletam a senha na própria tela |

Para um sistema `CONFIDENTIAL`, a resposta traz o segredo **uma única vez**: o banco guarda só o hash.

```json
{"clientId": "PORTAL", "clientSecret": "k3J9...-sG0"}
```

Perdeu o segredo ou precisa trocá-lo: `POST /systems/{id}/client-secret` gera outro, e o anterior deixa de valer na hora.

Para um usuário entrar no sistema, ele precisa de **um papel ativo nesse sistema** (`/roles` e `/role-users` do
`sentry-backend`). Sem papel, o login falha com a mesma mensagem de senha errada.

## 2. Login pelo navegador (Authorization Code + PKCE)

É o caminho padrão: a senha é digitada só na tela do Sentry.

1. A biblioteca lê a descoberta em `GET /.well-known/openid-configuration`.
2. O navegador vai para `GET /oauth2/authorize` com `response_type=code`, `client_id`, `redirect_uri`, `scope`, `state`,
   `nonce` e PKCE (`code_challenge` com `code_challenge_method=S256`; PKCE é obrigatório para todos os clientes).
3. Sem sessão no Sentry, o usuário vê a tela de login com o nome do sistema. Com sessão (SSO), o código volta sem pedir
   senha; `prompt=login` força a senha mesmo assim.
4. O navegador volta ao `redirect_uri` com `code` e o mesmo `state`. Erros (`invalid_request`, `invalid_scope`) voltam
   do mesmo jeito; `client_id` ou `redirect_uri` inválidos param numa página de erro do Sentry, sem redirecionar.
5. O sistema troca o código em `POST /oauth2/token` (`grant_type=authorization_code`, `code`, o mesmo `redirect_uri` e o
   `code_verifier`). Cliente confidencial se autentica com `client_secret_basic` ou `client_secret_post`; cliente público
   manda só o `client_id`. O código vale 60 segundos e uma única vez.

Exemplos prontos:

- SPA com [oidc-client-ts](https://github.com/authts/oidc-client-ts): [`examples/spa`](../examples/spa).
- Java com Spring Security OAuth2 Client: `sentry-auth/src/test/java/dev/sentry/auth/EndToEndOidcTest.java`.

```properties
# Spring Boot: basta o issuer e o client_id
spring.security.oauth2.client.provider.sentry.issuer-uri=http://localhost:8081
spring.security.oauth2.client.registration.sentry.client-id=PORTAL
spring.security.oauth2.client.registration.sentry.client-secret=${PORTAL_CLIENT_SECRET}
spring.security.oauth2.client.registration.sentry.scope=openid,profile,email,offline_access
```

Um SPA chama a descoberta, o token e o `/userinfo` por `fetch`: o Sentry libera CORS para a origem de qualquer
`redirectUri` cadastrada de um sistema ativo.

## 3. Login direto (sistemas confiáveis)

Para sistemas próprios que já coletam usuário e senha na própria tela. Só sistemas `CONFIDENTIAL` com
`isTrusted=true`; o cadastro como confiável cabe ao administrador da plataforma (papel `SENTRY.ADMIN`).

```bash
curl -X POST https://sentry.exemplo.com/api/v1/auth/login \
  -u 'PORTAL:<client_secret>' -H 'Content-Type: application/json' \
  -d '{"username": "maria", "password": "<senha>", "scope": "openid profile offline_access"}'
```

```json
{"access_token": "...", "token_type": "Bearer", "expires_in": 900, "id_token": "...", "refresh_token": "...", "scope": "openid profile offline_access"}
```

| Situação | Resposta |
| --- | --- |
| Segredo errado ou sistema público | `401 {"error": "invalid_client"}` |
| Sistema não confiável | `403 {"error": "unauthorized_client"}` |
| Usuário ou senha inválidos (ou sem papel no sistema) | `401 {"error": "invalid_grant", "error_description": "Usuário ou senha inválidos"}` |
| Escopo não cadastrado | `400 {"error": "invalid_scope"}` |

O escopo padrão é `openid profile`. O login direto não cria sessão SSO e só aceita HTTPS fora de localhost. A
documentação OpenAPI está em `/swagger-ui.html` do `sentry-auth`.

## 4. Tokens

| Token | Formato | Vida padrão | Conteúdo |
| --- | --- | --- | --- |
| Access token | JWT RS256, `typ=at+jwt` | 15 min | `iss`, `sub` (uuid do usuário), `aud` e `client_id` (o sistema), `org` (uuid da organização), `scope`, `exp`, `iat`, `jti` |
| ID token | JWT RS256 | 15 min | `sub`, `aud`, `nonce`, `auth_time`, `sid`; `name` e `preferred_username` com `profile`; `email` com `email` |
| Refresh token | Opaco | 30 dias sem uso, 90 dias no total | Só com `offline_access` e só para clientes confidenciais |

Uma API que recebe o access token valida a assinatura pelo JWKS (`GET /oauth2/jwks`), o `iss`, o `aud` (o próprio
`client_id`), o `exp` e o `typ` `at+jwt`. O token não traz papéis nem ações.

**Refresh:** `POST /oauth2/token` com `grant_type=refresh_token`. Cada uso devolve um refresh token novo e invalida o
anterior; reapresentar um já usado revoga todos os tokens daquele login. Usuário ou sistema desativado, ou papel
removido, faz o refresh falhar com `invalid_grant`. Cliente público não recebe refresh token: renova com um novo fluxo
PKCE, que não pede senha enquanto houver sessão SSO.

**Revogação:** `POST /oauth2/revoke` com o refresh token (RFC 7009), autenticado como no `/oauth2/token`.

**Dados do usuário:** `GET /userinfo` com `Authorization: Bearer <access token>` devolve os claims dos escopos
concedidos, lidos do banco no momento da chamada.

## 5. Logout

`GET /connect/logout?id_token_hint=<id token>&post_logout_redirect_uri=<uri cadastrada>&state=<valor>` encerra a sessão
SSO, revoga os refresh tokens emitidos nela e volta à URI com o `state`. Com uma URI não cadastrada, a sessão acaba e o
navegador fica numa página do Sentry.

## 6. Configuração do sentry-auth

| Variável | Padrão | Uso |
| --- | --- | --- |
| `SENTRY_ISSUER_URL` | `http://localhost:8081` | URL pública; vira o `iss` dos tokens |
| `SENTRY_JWT_SIGNING_KEY` | — | Chave privada RSA (PEM, PKCS#8), o conteúdo ou `file:/caminho`. Obrigatória em produção |
| `SENTRY_JWT_PREVIOUS_KEY` | — | Chave anterior, mantida no JWKS durante a troca |
| `SENTRY_JWT_GENERATE_IF_MISSING` | `false` | Só em desenvolvimento: gera uma chave temporária a cada subida |
| `SENTRY_SESSION_IDLE_TIMEOUT` | `30m` | Sessão SSO sem uso |
| `SENTRY_SESSION_MAX_AGE` | `12h` | Vida máxima da sessão SSO |
| `SENTRY_TOKEN_ACCESS_TOKEN_TTL`, `SENTRY_TOKEN_ID_TOKEN_TTL` | `15m` | Vida do access token e do ID token |
| `SENTRY_TOKEN_REFRESH_TOKEN_TTL`, `SENTRY_TOKEN_REFRESH_TOKEN_MAX_TTL` | `30d`, `90d` | Vida do refresh token sem uso e máxima |

Atrás de um proxy reverso, configure `server.tomcat.remoteip.internal-proxies` com os IPs do proxy: só deles o
`X-Forwarded-For` é aceito no registro de tentativas.

Gerar uma chave de assinatura:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out signing-key.pem
```
