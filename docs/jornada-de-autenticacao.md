# Jornada de autenticação

Este guia mostra, passo a passo, como um usuário entra num sistema usando o Sentry: o que o navegador e o sistema
enviam, o que o Sentry responde, e o que é cada token. Para cadastrar um sistema, montar o proxy e ver todas as
variáveis de configuração, use o [guia de integração](integracao_frontend.md).

## Quem participa

| Ator | O que faz | Endereço nos exemplos |
| --- | --- | --- |
| **Usuário** | A pessoa que entra, pelo navegador | — |
| **Sistema** (cliente) | A aplicação em que o usuário quer entrar (`client_id` `PORTAL`) | `https://portal.exemplo.com` |
| **Tela de login do Sentry** | Front-end em React onde a senha é digitada, na mesma origem do `sentry-auth` | `https://auth.exemplo.com/login` |
| **sentry-auth** | O servidor de autenticação: emite os tokens (é o *issuer*) | `https://auth.exemplo.com` |
| **sentry-backend** | A API de cadastro de organizações, sistemas, usuários e papéis | `https://admin.exemplo.com` |

Todos os valores de exemplo (segredos, tokens, códigos) são fictícios.

## Visão geral

O Sentry segue o **OpenID Connect** sobre o **OAuth 2.1**. O caminho padrão é o **Authorization Code com PKCE**: o
usuário digita a senha só na tela do Sentry, nunca no sistema, e o sistema recebe um código que troca por tokens.

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário (navegador)
    participant P as Sistema (PORTAL)
    participant S as Sentry
    participant L as Tela de login do Sentry

    U->>P: Clica em "Entrar"
    P->>U: Redireciona para /oauth2/authorize (com PKCE)
    U->>S: GET /oauth2/authorize
    S->>U: Sem sessão: redireciona para a tela de login
    U->>L: Abre /login
    L->>S: GET /api/v1/session/context
    L->>S: POST /api/v1/session/login (usuário e senha)
    S->>L: { redirectUrl }
    U->>S: Abre redirectUrl (retoma a autorização)
    S->>U: Redireciona para o redirect_uri com ?code=...&state=...
    U->>P: GET /callback?code=...
    P->>S: POST /oauth2/token (code + code_verifier)
    S->>P: access_token, id_token, refresh_token
    P->>S: GET /userinfo (opcional)
```

Se o usuário já tem sessão no Sentry (entrou antes em qualquer sistema), os passos da tela de login são pulados: o
código volta direto. Isso é o **SSO** (login único).

---

## Antes de tudo — Cadastrar o sistema

O sistema é cadastrado uma vez no `sentry-backend`. O `externalCode` vira o `client_id` e não muda depois.

```http
POST /systems HTTP/1.1
Host: admin.exemplo.com
Content-Type: application/json

{
  "idOrganization": 1,
  "externalCode": "PORTAL",
  "description": "Portal do Cliente",
  "scopes": "openid profile email offline_access",
  "clientType": "CONFIDENTIAL",
  "redirectUris": ["https://portal.exemplo.com/callback"],
  "postLogoutRedirectUris": ["https://portal.exemplo.com/"],
  "isTrusted": false,
  "isActive": true
}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json

{ "clientId": "PORTAL", "clientSecret": "exemplo-de-segredo-NAO-USE" }
```

| Escolha | Quando usar |
| --- | --- |
| `CONFIDENTIAL` | O sistema tem backend e guarda um segredo. O segredo aparece **só nesta resposta**; o Sentry guarda apenas o hash |
| `PUBLIC` | SPA ou app mobile, sem segredo (`clientSecret` vem nulo) |

- As URIs de redirecionamento são comparadas letra por letra. Use `https`; em desenvolvimento, `http://127.0.0.1:<porta>`
  (`localhost` não é aceito).
- Escopos possíveis: `openid`, `profile`, `email` e `offline_access`.
- Perdeu o segredo? `POST /systems/{id}/client-secret` gera outro; o antigo deixa de valer na hora.
- Para entrar no sistema, o usuário precisa de um papel ativo nele (`/roles` e `/role-users` do `sentry-backend`).

Detalhes de cada campo estão no [guia de integração](integracao_frontend.md#1-cadastrar-o-sistema).

## Passo 1 — O sistema descobre o Sentry

A biblioteca OIDC do sistema lê a descoberta uma vez e guarda os endereços.

```http
GET /.well-known/openid-configuration HTTP/1.1
Host: auth.exemplo.com
```

```json
{
  "issuer": "https://auth.exemplo.com",
  "authorization_endpoint": "https://auth.exemplo.com/oauth2/authorize",
  "token_endpoint": "https://auth.exemplo.com/oauth2/token",
  "userinfo_endpoint": "https://auth.exemplo.com/userinfo",
  "jwks_uri": "https://auth.exemplo.com/oauth2/jwks",
  "revocation_endpoint": "https://auth.exemplo.com/oauth2/revoke",
  "end_session_endpoint": "https://auth.exemplo.com/connect/logout",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code", "refresh_token"],
  "code_challenge_methods_supported": ["S256"],
  "id_token_signing_alg_values_supported": ["RS256"],
  "scopes_supported": ["openid"]
}
```

Na prática, basta configurar a biblioteca com o **issuer** e o **client_id**; o resto vem daqui.

## Passo 2 — O sistema manda o usuário para o Sentry

Antes de redirecionar, o sistema gera três valores aleatórios e os guarda até o retorno:

| Valor | Para que serve |
| --- | --- |
| `state` | Confere que a resposta é do pedido que o sistema fez (proteção contra CSRF) |
| `nonce` | Vai dentro do ID token; confere que o token é deste login |
| `code_verifier` | Segredo do PKCE, de 43 a 128 caracteres. Só o sistema conhece |

O `code_challenge` é o `code_verifier` passado por SHA-256 e codificado em Base64 URL, sem `=` no final:

```
code_verifier  = dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
code_challenge = BASE64URL(SHA256(code_verifier)) = E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
```

O navegador é redirecionado para:

```http
GET /oauth2/authorize?response_type=code
    &client_id=PORTAL
    &redirect_uri=https%3A%2F%2Fportal.exemplo.com%2Fcallback
    &scope=openid%20profile%20email%20offline_access
    &state=af0ifjsldkj
    &nonce=n-0S6_WzA2Mj
    &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
    &code_challenge_method=S256 HTTP/1.1
Host: auth.exemplo.com
```

| Parâmetro | Regra |
| --- | --- |
| `client_id` | O código do sistema cadastrado no Sentry |
| `redirect_uri` | Tem de ser **igual** a uma URI cadastrada (letra por letra) |
| `scope` | `openid` obrigatório; `profile` (nome), `email` e `offline_access` (refresh token) opcionais, se o sistema tiver permissão |
| `code_challenge_method` | Só `S256`. Sem PKCE, o pedido é recusado |
| `prompt=login` | Opcional: pede a senha mesmo que o usuário já tenha sessão |

Sem sessão, o Sentry guarda o pedido e manda o navegador para a tela de login:

```http
HTTP/1.1 302 Found
Location: https://auth.exemplo.com/login
Set-Cookie: SESSION=YjA3...; Path=/; HttpOnly; SameSite=Lax; Secure
```

## Passo 3 — O usuário faz login na tela do Sentry

A tela de login (React) conversa com o Sentry pela API de sessão, na mesma origem. Os cookies vão sozinhos.

**3.1 — A tela descobre qual sistema pediu o login**

```http
GET /api/v1/session/context HTTP/1.1
Host: auth.exemplo.com
Cookie: SESSION=YjA3...
```

```http
HTTP/1.1 200 OK
Set-Cookie: XSRF-TOKEN=4f8a...; Path=/
Content-Type: application/json

{ "clientId": "PORTAL", "systemName": "Portal do Cliente" }
```

O cookie `XSRF-TOKEN` é a proteção contra CSRF: a tela devolve o valor dele no cabeçalho `X-XSRF-TOKEN` em todo `POST`.

**3.2 — A tela envia usuário e senha**

```http
POST /api/v1/session/login HTTP/1.1
Host: auth.exemplo.com
Cookie: SESSION=YjA3...; XSRF-TOKEN=4f8a...
X-XSRF-TOKEN: 4f8a...
Content-Type: application/json

{ "username": "maria", "password": "minha senha" }
```

Deu certo: o Sentry cria a sessão (com um novo id) e diz para onde o navegador deve ir.

```http
HTTP/1.1 200 OK
Set-Cookie: SESSION=Nzk1...; Path=/; HttpOnly; SameSite=Lax; Secure
Content-Type: application/json

{ "redirectUrl": "https://auth.exemplo.com/oauth2/authorize?response_type=code&client_id=PORTAL&...&continue" }
```

Deu errado: a mensagem é sempre a mesma, para não revelar se o usuário existe, está inativo ou errou a senha.

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{ "status": 401, "title": "Unauthorized", "detail": "Usuário ou senha inválidos" }
```

Para entrar, o usuário precisa estar ativo e ter **pelo menos um papel ativo no sistema** que pediu o login. Toda
tentativa, com sucesso ou não, fica registrada.

**3.3 — A tela abre a `redirectUrl`**

```js
window.location.assign(redirectUrl);
```

## Passo 4 — O Sentry devolve o código ao sistema

```http
GET /oauth2/authorize?...&continue HTTP/1.1
Host: auth.exemplo.com
Cookie: SESSION=Nzk1...
```

```http
HTTP/1.1 302 Found
Location: https://portal.exemplo.com/callback?code=SplxlOBeZQQYbYS6WxSbIA&state=af0ifjsldkj
```

O sistema confere que o `state` é o que ele gerou. O **código** vale **60 segundos** e só pode ser usado **uma vez**.

Se algo der errado depois de o `redirect_uri` ser validado (escopo não permitido, PKCE ausente), o erro volta do
mesmo jeito: `?error=invalid_scope&state=af0ifjsldkj`. Se o `client_id` ou o `redirect_uri` forem inválidos, o Sentry
não redireciona: responde `400` em JSON.

## Passo 5 — O sistema troca o código por tokens

Essa chamada é feita **pelo sistema**, não pelo navegador do usuário (num SPA, a própria página faz a chamada).

Sistema com backend (`CONFIDENTIAL`), autenticado com o segredo:

```http
POST /oauth2/token HTTP/1.1
Host: auth.exemplo.com
Authorization: Basic UE9SVEFMOnNlZ3JlZG8tZG8tcG9ydGFs
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=https%3A%2F%2Fportal.exemplo.com%2Fcallback
&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

SPA ou app (`PUBLIC`), sem segredo: troca o `Authorization` por `&client_id=PORTAL` no corpo.

```http
HTTP/1.1 200 OK
Cache-Control: no-store
Content-Type: application/json

{
  "access_token": "eyJraWQiOiI4aHpO...",
  "token_type": "Bearer",
  "expires_in": 899,
  "scope": "openid profile email offline_access",
  "id_token": "eyJraWQiOiI4aHpO...",
  "refresh_token": "nzY8f2Kp..."
}
```

O Sentry confere que `SHA256(code_verifier)` é o `code_challenge` do passo 2. É isso que impede que alguém que
intercepte o código o use: sem o `code_verifier`, a troca falha com `400 invalid_grant`.

---

## Os tokens

| Token | Formato | Para quem | Vida | Para que serve |
| --- | --- | --- | --- | --- |
| **Access token** | JWT | As APIs do sistema | 15 minutos | Provar, a cada chamada de API, que o usuário está autenticado naquele sistema |
| **ID token** | JWT | O próprio sistema | 15 minutos | Dizer quem é o usuário e como ele se autenticou |
| **Refresh token** | Texto opaco | O backend do sistema | 30 dias sem uso, 90 dias no total | Pedir um access token novo sem pedir a senha de novo |

### O que é um JWT

Um JWT (JSON Web Token) são três partes em Base64 URL, separadas por ponto:

```
eyJraWQiOi...   .   eyJzdWIiOi...   .   SflKxwRJSM...
   cabeçalho            conteúdo            assinatura
```

- **Cabeçalho:** o algoritmo da assinatura e a chave usada.
- **Conteúdo (claims):** as informações, em JSON. **Não é cifrado**: qualquer um pode ler. Por isso o Sentry nunca põe
  senha nem dado sensível no token.
- **Assinatura:** feita com a chave privada do Sentry (RS256). Quem tem a chave pública (no JWKS) confere que o token
  veio do Sentry e não foi alterado.

### Access token por dentro

```json
// cabeçalho
{ "kid": "8hzNdCCj682wPadTeh6U4XtX508-ixABEeMPO3LJRog", "typ": "at+jwt", "alg": "RS256" }

// conteúdo
{
  "iss": "https://auth.exemplo.com",
  "sub": "d3828d44-fa89-4cc1-abda-4aec1eaa433b",
  "aud": "PORTAL",
  "client_id": "PORTAL",
  "org": "6f1c2e7a-0b3d-4e5f-9a8b-1c2d3e4f5a6b",
  "scope": ["openid", "profile", "email", "offline_access"],
  "iat": 1793290276,
  "nbf": 1793290276,
  "exp": 1793291176,
  "jti": "fe9460ed-1ac4-4dbe-90ab-fd50c82ed1d6"
}
```

| Claim | Significado |
| --- | --- |
| `iss` | Quem emitiu: o endereço do Sentry |
| `sub` | O usuário. É um identificador fixo (uuid), nunca o login nem o id interno |
| `aud` e `client_id` | Para qual sistema o token foi emitido |
| `org` | A organização dona do sistema |
| `scope` | O que foi concedido |
| `iat`, `nbf`, `exp` | Emitido em, válido a partir de, expira em (segundos desde 1970) |
| `jti` | Identificador único do token |

O access token **não** leva nome, e-mail, papéis nem permissões.

### ID token por dentro

```json
{
  "iss": "https://auth.exemplo.com",
  "sub": "d3828d44-fa89-4cc1-abda-4aec1eaa433b",
  "aud": "PORTAL",
  "azp": "PORTAL",
  "nonce": "n-0S6_WzA2Mj",
  "auth_time": 1793290270,
  "sid": "Xdvun2iKy7CVIRUHE2ULf7mbWKEVdeiarnoXwjbAVtA",
  "name": "Maria Silva",
  "preferred_username": "maria",
  "email": "maria@exemplo.com",
  "iat": 1793290276,
  "exp": 1793291176,
  "jti": "9b1e..."
}
```

| Claim | Significado |
| --- | --- |
| `nonce` | O mesmo do passo 2. O sistema DEVE conferir |
| `auth_time` | Quando o usuário digitou a senha |
| `sid` | A sessão do Sentry em que o login aconteceu |
| `name`, `preferred_username` | Só com o escopo `profile` |
| `email` | Só com o escopo `email` |

### Como uma API valida o access token

A cada requisição com `Authorization: Bearer <access token>`, a API do sistema:

1. Lê o `kid` do cabeçalho e pega a chave pública correspondente em `GET /oauth2/jwks` (guarde em cache; a resposta
   pode ser reaproveitada por 5 minutos).
2. Confere a assinatura RS256 com essa chave.
3. Confere `iss` (o endereço do Sentry), `aud` (o próprio `client_id`), `exp` (não expirou) e `typ` = `at+jwt`.

Qualquer biblioteca JWT faz isso. No Spring Boot, por exemplo, basta
`spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.exemplo.com`.

---

## Depois do login

### Dados atualizados do usuário

```http
GET /userinfo HTTP/1.1
Host: auth.exemplo.com
Authorization: Bearer eyJraWQiOiI4aHpO...
```

```json
{
  "sub": "d3828d44-fa89-4cc1-abda-4aec1eaa433b",
  "name": "Maria Silva",
  "preferred_username": "maria",
  "email": "maria@exemplo.com"
}
```

Os dados vêm do banco na hora, não do token: se o nome mudou depois do login, aqui já aparece o novo. Os campos
dependem dos escopos: `sub` sempre; `name` e `preferred_username` com `profile`; `email` com `email`.

| Situação | Resposta |
| --- | --- |
| Usuário desativado ou token vencido | `401` com `WWW-Authenticate: Bearer error="invalid_token"` |
| Token sem o escopo `openid` | `403` com `WWW-Authenticate: Bearer error="insufficient_scope"` |

### Renovar o access token

Quando o access token vence (15 minutos), o backend do sistema usa o refresh token:

```http
POST /oauth2/token HTTP/1.1
Host: auth.exemplo.com
Authorization: Basic UE9SVEFMOnNlZ3JlZG8tZG8tcG9ydGFs
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=nzY8f2Kp...
```

A resposta tem o mesmo formato do passo 5, com um **refresh token novo**. Guarde sempre o último:

- O refresh token usado deixa de valer na hora (rotação).
- Se um refresh token já usado for apresentado de novo, o Sentry entende que ele vazou e **revoga todos os tokens
  daquele login**, inclusive o mais novo.
- O refresh falha (`400 invalid_grant`) se o usuário ou o sistema forem desativados, se o usuário perder o papel no
  sistema, ou depois de 90 dias do login.
- Só sistemas `CONFIDENTIAL` recebem refresh token, e só com o escopo `offline_access`.

**Renovação de um SPA:** um SPA não recebe refresh token. Para renovar, ele refaz o passo 2 (um redirecionamento de
página inteira; com `oidc-client-ts`, `signinRedirect()`). Com sessão no Sentry, o código volta sem pedir senha. Com
`prompt=none`, o Sentry nunca mostra a tela: sem sessão, volta `?error=login_required&state=...`. A renovação dentro de um
iframe (`signinSilent()`) não funciona, porque o Sentry proíbe ser carregado em iframe (`X-Frame-Options: DENY`).

### Revogar um refresh token

```http
POST /oauth2/revoke HTTP/1.1
Host: auth.exemplo.com
Authorization: Basic UE9SVEFMOnNlZ3JlZG8tZG8tcG9ydGFs
Content-Type: application/x-www-form-urlencoded

token=nzY8f2Kp...&token_type_hint=refresh_token
```

Resposta `200`, sem corpo. A partir daí o token não renova mais.

### Sair (logout)

O sistema manda o navegador para:

```http
GET /connect/logout?id_token_hint=eyJraWQiOiI4aHpO...
    &post_logout_redirect_uri=https%3A%2F%2Fportal.exemplo.com%2F
    &state=fim HTTP/1.1
Host: auth.exemplo.com
```

```http
HTTP/1.1 302 Found
Location: https://portal.exemplo.com/?state=fim
```

O Sentry encerra a sessão e revoga os refresh tokens emitidos nela. A `post_logout_redirect_uri` tem de estar
cadastrada; se não estiver, a sessão acaba do mesmo jeito e o navegador vai para a tela de login do Sentry.

A tela de login também pode encerrar só a sessão no Sentry, com `POST /api/v1/session/logout` (com o cabeçalho
`X-XSRF-TOKEN`), que responde `204`. Esse caminho não revoga os refresh tokens dos sistemas; para isso, use o
`/connect/logout` acima ou o `/oauth2/revoke`.

### Quanto dura a sessão no Sentry

A sessão (o cookie `SESSION`) acaba depois de **30 minutos sem uso** ou **12 horas** no total, o que vier primeiro. Depois
disso, o próximo login pede a senha de novo.

---

## Login direto (sistemas confiáveis)

Alguns sistemas próprios coletam usuário e senha na própria tela. Para eles, e só para os marcados como **confiáveis**
pelo administrador, existe um caminho sem redirecionamentos:

```http
POST /api/v1/auth/login HTTP/1.1
Host: auth.exemplo.com
Authorization: Basic UE9SVEFMOnNlZ3JlZG8tZG8tcG9ydGFs
Content-Type: application/json

{ "username": "maria", "password": "minha senha", "scope": "openid profile offline_access" }
```

```json
{
  "access_token": "eyJraWQiOiI4aHpO...",
  "token_type": "Bearer",
  "expires_in": 900,
  "id_token": "eyJraWQiOiI4aHpO...",
  "refresh_token": "nzY8f2Kp...",
  "scope": "openid profile offline_access"
}
```

Os tokens são os mesmos do fluxo padrão, e o refresh token funciona no `/oauth2/token`. Esse login **não** cria sessão no
Sentry: entrar em outro sistema pede a senha de novo.

---

## Erros mais comuns

| Onde | Resposta | O que significa |
| --- | --- | --- |
| `/oauth2/authorize` | `400` JSON, sem redirecionar | `client_id` desconhecido ou desativado, ou `redirect_uri` não cadastrada |
| `/oauth2/authorize` | `?error=invalid_request` no `redirect_uri` | Faltou o PKCE, ou o método não é `S256` |
| `/oauth2/authorize` | `?error=invalid_scope` | Escopo que o sistema não pode pedir |
| `/api/v1/session/login` | `401` "Usuário ou senha inválidos" | Credenciais erradas, usuário inativo ou sem papel no sistema |
| `/api/v1/session/login` | `403` | Faltou o cabeçalho `X-XSRF-TOKEN` |
| `/oauth2/token` | `400 invalid_grant` | Código vencido ou já usado, `code_verifier` errado, `redirect_uri` diferente, refresh token inválido |
| `/oauth2/token` | `401 invalid_client` | Segredo errado ou sistema desativado |
| `/userinfo` | `401`, `WWW-Authenticate: Bearer error="invalid_token"` | Token vencido ou usuário desativado: faça o refresh ou um novo login |
| `/userinfo` | `403`, `WWW-Authenticate: Bearer error="insufficient_scope"` | O login não pediu o escopo `openid` |
| `/api/v1/auth/login` | `401 invalid_client` | Sistema sem HTTP Basic, com segredo errado ou público |
| `/api/v1/auth/login` | `403 unauthorized_client` | O sistema não é confiável para o login direto |

## Boas práticas para o sistema

- Use uma biblioteca OIDC pronta (por exemplo, `oidc-client-ts` no navegador ou o Spring Security OAuth2 Client); não
  monte os fluxos à mão.
- Confira o `state` no retorno e o `nonce` no ID token.
- Guarde o refresh token só no backend, nunca no navegador.
- Mande o access token apenas às suas próprias APIs, por HTTPS, no cabeçalho `Authorization`.
- Não leia dados do usuário no access token: use o ID token no login e o `/userinfo` quando precisar de dados atuais.
- Identifique o usuário pelo `sub`, nunca pelo `preferred_username` ou pelo e-mail, que podem mudar.

## Perguntas frequentes

**Preciso de um SDK do Sentry?** Não. Qualquer biblioteca OpenID Connect funciona configurada com o issuer e o
`client_id`.

**Por que o access token não tem o nome do usuário?** Ele serve para autorizar chamadas de API e vai em toda
requisição; dados pessoais ficam no ID token e no `/userinfo`.

**O token tem os papéis do usuário?** Ainda não. A entrega dos papéis no token, com o escopo `roles`, está prevista
para a Fase 2.

**O usuário entrou no sistema A. Precisa digitar a senha no sistema B?** Não, enquanto a sessão no Sentry estiver
ativa (até 30 minutos sem uso e 12 horas no total). Use `prompt=login` se o sistema B quiser pedir a senha mesmo assim.

**Meu `redirect_uri` com `localhost` é recusado.** Use `http://127.0.0.1:<porta>`; o Sentry segue a recomendação do
OAuth 2.1 e não aceita `localhost`.

**O usuário trocou de senha. Os tokens antigos continuam valendo?** O access token vale até expirar (no máximo 15
minutos). O refresh token deixa de renovar se o usuário for desativado ou perder o papel no sistema.
