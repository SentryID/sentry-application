# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Convenções

- Todo o código (classes, métodos, variáveis, pacotes) e enums devem ser escritos em inglês.
- Exceção: mensagens de retorno de erro, que permanecem em português.
- Todo método criado ou alterado em services deve obrigatoriamente ter Javadoc correspondente e testes criados ou atualizados.
- Javadoc, comentários, descrições OpenAPI (`@Schema`) e documentação ficam em português. Regras de negócio são citadas pelo ID da spec (ex.: `RN-F1-02.7`, `F1-11`), inclusive nos commits: `tipo(escopo) Descrição (RN-...)`.
- Novo domínio CRUD: use a skill `create-domain` (o padrão de referência é o domínio `organization`).

# Comandos

Requer JDK 27 e Docker rodando (os testes de integração usam Testcontainers com PostgreSQL).

```bash
./mvnw clean verify                                   # build + todos os testes + JaCoCo (target/site/jacoco)
./mvnw -pl sentry-auth -am test -Dtest=LoginTest -Dsurefire.failIfNoSpecifiedTests=false   # um teste
./mvnw -pl sentry-auth -am test -Dtest='LoginTest#method' -Dsurefire.failIfNoSpecifiedTests=false

cp .env.example .env && docker compose up --build     # postgres + sentry-auth (8081) + sentry-backend (8080)
docker compose up -d postgres                         # só o banco, para rodar as apps pela IDE (DB_URL/DB_USERNAME/DB_PASSWORD do .env.example)
```

`SPRING_PROFILES_ACTIVE=dev` adiciona `classpath:db/dev` às locations do Flyway (seed repetível `R__dev_seed.sql`, usado por `examples/spa`). A CI (`.github/workflows/ci.yml`) roda `./mvnw -B verify` e gitleaks.

# Arquitetura

Multi-módulo Maven (Spring Boot 4, sem `spring-boot-starter-parent`: versões no `dependencyManagement` do pom raiz).

- **sentry-api** (`dev.sentry.api`): biblioteca sem `main`. Entidades, DTOs (`*Rest`), MapStruct, repositories, services, specifications em `domain/<entidade>/api/`, além de `security/password` e as migrações Flyway (`db/migration`, `V<n>__*.sql`). As apps a recebem pela `ApiAutoConfiguration` (registrada em `META-INF/spring/...AutoConfiguration.imports`), não por component scan.
- **sentry-backend** (`dev.sentry.backend`, 8080): só controllers CRUD (`FooController` + `FooControllerSwagger`) que delegam aos services do `sentry-api`.
- **sentry-auth** (`dev.sentry.auth`, 8081): servidor OAuth 2.1 / OIDC sobre o Spring Authorization Server. Só API; a tela de login é um front separado servido na mesma origem por proxy.
  - `oauth/`: configuração do authorization server; cada `System` do banco é um cliente OAuth (`SystemRegisteredClientRepository`), CORS pelas origens registradas, chave de assinatura JWT (`SigningKeyConfiguration`), claims customizadas, refresh tokens guardados com hash.
  - `provider/`: `UserAuthenticationProvider` é o único `AuthenticationProvider` (tela de login e login direto); delega a conferência da senha ao `CredentialVerifier` do `authentication_type` do usuário. Novo tipo (ex.: LDAP) = nova implementação de `CredentialVerifier`. Toda tentativa é registrada em `access_logs`.
  - `login/` (login direto para sistemas confiáveis), `session/` (sessão SSO, logout), `bootstrap/` (cria o administrador na primeira subida), `errors/`.

Lógica de negócio sempre em `sentry-api`; exposição HTTP no módulo da aplicação.

# Specs e docs

- `specs/`: requisitos no estilo Spec Driven Development (`fase-0..2/F<fase>-<nn>-*.md`), com regras `RN-...` e critérios de aceite que viram testes. `specs/README.md` é o índice; `ROADMAP.md` e `PROXIMOS-PASSOS.md` dão a ordem.
- `docs/`: guias de integração (frontend/PKCE, jornada de autenticação, LDAP, simulação de login).
