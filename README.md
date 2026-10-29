# Sentry

Aplicação open source de autenticação e autorização. Módulos:

- `sentry-auth` (porta 8081): autenticação.
- `sentry-backend` (porta 8080): cadastro de organizações, sistemas, usuários e permissões.
- `sentry-api`: código compartilhado (entidades, serviços, migrações Flyway).

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
