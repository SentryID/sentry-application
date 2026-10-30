# Sentry

Aplicação open source de autenticação e autorização. Módulos:

- `sentry-auth` (porta 8081): autenticação (OAuth 2.1 / OpenID Connect), só API: a tela de login é um front-end separado, servido na mesma origem por um proxy.
- `sentry-backend` (porta 8080): cadastro de organizações, sistemas, usuários e permissões.
- `sentry-api`: código compartilhado (entidades, serviços, migrações Flyway).

Para integrar um sistema (cadastro, login por PKCE ou direto, tokens e logout), veja [docs/integracao_frontend.md](docs/integracao_frontend.md). O passo a passo do login, com requisições, respostas e os tokens explicados, está em [docs/jornada-de-autenticacao.md](docs/jornada-de-autenticacao.md). Para conferir a senha num LDAP, veja [docs/provedor-ldap.md](docs/provedor-ldap.md). Um exemplo completo de login local, com SQL e requisição, está em [docs/simulacao-login-kyrosportal.md](docs/simulacao-login-kyrosportal.md).

## Ambiente local com Docker

Só é preciso ter o Docker.

Subir (PostgreSQL 17, `sentry-auth` e `sentry-backend`):

```bash
cp .env.example .env
docker compose up --build
```

Os dois serviços ficam prontos quando `/actuator/health` responde `UP`:

- http://localhost:8081/actuator/health
- http://localhost:8080/actuator/health

Os tokens são assinados com uma chave temporária, gerada a cada subida (`SENTRY_JWT_GENERATE_IF_MISSING=true` no `.env.example`). Em produção, configure `SENTRY_JWT_SIGNING_KEY` e deixe essa variável em `false`.

Na primeira subida, com o banco vazio, o `sentry-auth` cria o administrador. Se `SENTRY_ADMIN_PASSWORD` estiver vazia no `.env`, a senha é gerada e aparece **uma única vez** no log:

```bash
docker compose logs sentry-auth
```

Parar, mantendo o banco:

```bash
docker compose down
```

Zerar, apagando o banco (na próxima subida o administrador é criado de novo):

```bash
docker compose down -v
```

## Desenvolvimento

Requer o JDK 27. Os testes de integração usam Testcontainers, então o Docker precisa estar rodando:

```bash
./mvnw clean verify
```

Para rodar uma aplicação fora do Compose, suba só o banco (`docker compose up -d postgres`) e exporte as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` do `.env.example`.
