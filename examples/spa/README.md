# Exemplo de SPA

Página estática que faz login no Sentry pelo fluxo Authorization Code + PKCE com a biblioteca genérica
[oidc-client-ts](https://github.com/authts/oidc-client-ts), configurada só com o issuer e o `client_id`. Usa o sistema
`SYS001` do seed de desenvolvimento (cliente público, redirecionamento para `http://127.0.0.1:3000/`).

O teste automatizado equivalente, com uma biblioteca OIDC genérica em Java, é o `EndToEndOidcTest` do `sentry-auth`.

Este exemplo é um **sistema cliente**: a senha é digitada na tela de login do front-end do Sentry (React), que precisa
estar no ar em `SENTRY_LOGIN_URL`, na mesma origem do `sentry-auth` (ver a seção 6 de `docs/integracao.md`). Ajuste o
`authority` do `index.html` para o `SENTRY_ISSUER_URL`.

## Rodar

1. Suba o ambiente com o seed de desenvolvimento:

   ```bash
   SPRING_PROFILES_ACTIVE=dev docker compose up --build
   ```

2. Crie um usuário com senha e dê a ele o papel `SYS001.VIEWER` (o seed não traz senhas):

   ```bash
   curl -X POST localhost:8080/users -H 'Content-Type: application/json' \
     -d '{"name": "Maria Silva", "authenticationType": "DATABASE", "username": "maria", "password": "Troque-esta-senha-1", "email": "maria@exemplo.com", "isActive": true}'
   curl 'localhost:8080/users?page=0&pageSize=1&username=maria'           # anote o id
   curl 'localhost:8080/roles?page=0&pageSize=1&systemRole=SYS001.VIEWER' # anote o id
   curl -X POST localhost:8080/role-users -H 'Content-Type: application/json' \
     -d '{"idRole": <id do papel>, "idUser": <id do usuário>, "isActive": true}'
   ```

3. Sirva esta pasta em `127.0.0.1:3000` (o Sentry só aceita `http` local em `127.0.0.1`):

   ```bash
   python -m http.server 3000 --bind 127.0.0.1 --directory examples/spa
   ```

4. Abra http://127.0.0.1:3000/ e clique em **Entrar**.

O SPA não recebe refresh token (cliente público); enquanto a sessão SSO existir, um novo login não pede senha.
