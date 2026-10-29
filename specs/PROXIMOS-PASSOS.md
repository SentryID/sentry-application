# Próximos passos — Fases 0 e 1

Situação em 29/10/2026. O `init` já foi integrado ao `main`; a 0.6, a 0.7, a 0.4 e a 0.5 estão na branch `fase-0/argon2id` (PR aberto); a 0.8 e a Fase 1 seguem na `fase-1/clientes-oauth`, criada a partir dela.

## Feito

| # | Tarefa | Commit |
| --- | --- | --- |
| 0.1 | `seed-template.sql` removido; dados fictícios em `db/dev/R__dev_seed.sql`, só no perfil `dev` | `b113adc` |
| 0.2 | `role_actions.cd_action` em `VARCHAR(255)`; entidade `Action` com código natural (`String code`); `spring-boot-starter-flyway`; testes com Testcontainers (CA-F0-01.1 a .3) | `b113adc` |
| 0.3 | Entidade `System` renomeada para `ExternalSystem` | `b7dace0` |
| 0.6 | Argon2id com prefixo `{argon2}` via `DelegatingPasswordEncoder`; parâmetros em `sentry.security.password.argon2.*` (mínimos da OWASP validados na subida); `PasswordService.verify` devolve o novo hash quando os parâmetros ficaram mais fortes; coluna `salt` removida; `UserService.save` grava o hash; testes CA-F0-02.1 a .4 | branch `fase-0/argon2id` |
| 0.7 | `BootstrapService` no `sentry-auth`: na primeira subida cria a organização `SENTRY_PLATFORM`, o sistema `SENTRY`, o papel `SENTRY.ADMIN` e o administrador (`SENTRY_ADMIN_USERNAME`, `SENTRY_ADMIN_EMAIL`, `SENTRY_ADMIN_PASSWORD`); sem senha, gera uma de 24 caracteres e a mostra uma vez no log; idempotente pela organização; testes CA-F0-03.1 a .3 | branch `fase-0/argon2id` |
| 0.4 | `compose.yaml` com `postgres:17-alpine` (volume nomeado, healthcheck `pg_isready`), `sentry-auth` e `sentry-backend`; `Dockerfile` multi-stage na raiz (`ARG MODULE`; build com `maven:3-eclipse-temurin-27`, runtime `eclipse-temurin:27-jre-alpine`, usuário `sentry`); actuator só com `health` e probes; `sentry.issuer-url` (`SENTRY_ISSUER_URL`, padrão `http://localhost:8081`); `.env.example` completo; `README.md`. CA-F0-04.1 a .3 verificados à mão: tudo `UP` em ~30 s com build, bootstrap roda de novo após `down -v`, processo não é root | branch `fase-0/argon2id` |
| 0.5 | `.github/workflows/ci.yml` em todo push e pull request: job `build` (Temurin 27, `./mvnw -B verify` com Testcontainers, cobertura de linhas por módulo no resumo do job, relatórios JaCoCo e surefire como artifact) e job `gitleaks` (CLI `zricethezav/gitleaks:v8.30.1` no histórico inteiro); JaCoCo no `pom.xml` raiz; `.github/dependabot.yml` semanal para maven, github-actions e docker. Gitleaks local sem achados no histórico; CA-F0-05.2 verificado com uma chave PEM falsa. Cobertura de linhas em 29/10: `sentry-auth` 92,6%, `sentry-backend` 31,0%, `sentry-api` 14,4% (só os testes do próprio módulo contam) | branch `fase-0/argon2id` |
| 0.8 | Job `image` no `ci.yml`: em push no `main`, depois de `build` e `gitleaks` verdes, publica `ghcr.io/<owner em minúsculas>/sentry-auth:<sha>` com o `GITHUB_TOKEN` (`packages: write`). A proteção do `main` fica com o usuário | branch `fase-1/clientes-oauth` |
| 1.1 | Migração `V2__oauth_clients_and_sessions.sql`: `systems` ganha `client_type`, `client_secret_hash`, `is_trusted` e `external_code NOT NULL` (é o `client_id`); `system_redirect_uris` (`LOGIN`/`LOGOUT`); `access_logs` ganha `attempted_username`, `channel` e `user_agent`; `oauth2_authorization`, `oauth2_authorization_consent` (Spring Authorization Server 7.1) e `spring_session*` (Spring Session JDBC 4.1), adaptados ao PostgreSQL. Entidades `ExternalSystem` e `AccessLog` ajustadas para o `ddl-auto: validate` | branch `fase-1/clientes-oauth` |

Achados que a spec não previa, já tratados:

- A entidade `Action` mapeava `cd_action` como `Long` gerado pelo banco; só corrigir o SQL não bastava para o `ddl-auto: validate`.
- No Spring Boot 4, `flyway-core` sozinho não roda as migrações: é preciso o `spring-boot-starter-flyway`.
- Como o código da ação é a chave primária, `ActionService.save` passou a recusar código duplicado (409); sem isso o `save` do JPA sobrescreveria a ação existente.
- (0.6) O compilador não usava `-parameters` (o projeto não herda do `spring-boot-starter-parent`), e por isso todo endpoint com `@PathVariable Long id` sem nome explícito, como `GET /users/{id}`, respondia 500. A flag foi ligada no `pom.xml` raiz.
- (0.4) Pelo mesmo motivo, o `spring-boot-maven-plugin` não executava o `repackage` e o jar gerado não era executável (`no main manifest attribute`). O goal foi ligado no `pluginManagement` do `pom.xml` raiz.
- (0.4) As duas aplicações rodam o Flyway no mesmo banco ao subir juntas; o lock da tabela de histórico serializa as migrações e a subida simultânea funciona.
- (0.6) O `toString` do `UserRest.SaveRequest` mascara a senha, para ela não vazar se o request for logado (RN-F0-02.6).

## Decisões tomadas

As recomendações das **Questões em aberto** da Fase 0 foram aceitas:

- Renomear `System` agora (feito na 0.3).
- `sentry-backend` entra no Compose (0.4).
- Cobertura de 80% informativa na Fase 0, bloqueante a partir da Fase 1.
- Não reescrever o histórico do Git: apenas trocar a senha exposta.
- Política de senha (mínimo de 8 caracteres, bloqueio de senhas comuns) fica para a Fase 1.

Questões em aberto da Fase 1, recomendações aceitas em 29/10:

- `client_id` = `external_code` do sistema (F1-01).
- Exigir papel no sistema para autenticar (RN-F1-02.5) já na Fase 1; login só por username, sem e-mail (F1-02).
- Tela de login com a identidade visual do Sentry (F1-04).
- Claim `org` já na Fase 1 (F1-05).
- Back-channel logout fora da Fase 1 (F1-09).
- Só o superadministrador da plataforma (papel `SENTRY.ADMIN`) marca um sistema como confiável (F1-11).

## Pendências fora do código

- Trocar a senha exposta no commit `77462ee` em todo lugar onde é usada (RN-F0-03.2).

## Próximo passo: 1.2 — CRUD de cliente OAuth (F1-01)

Notas da 1.1:

- Sistemas que já existiam ficam `CONFIDENTIAL` sem segredo, o que não autentica até alguém rotacionar o segredo ou trocar o tipo.
- A tabela `oauth2_registered_client` não foi criada: os clientes vêm de `systems` (F1-01).

Falta da Fase 0, com o usuário:

- Proteger o `main` (RN-F0-05.4, CA-F0-05.1): merge só por pull request e com os checks `build` e `gitleaks` verdes.
- 0.9: mergear o PR da `fase-0/argon2id` no `main` e confirmar que a imagem aparece no GHCR (CA-F0-05.3).

Decisões da 0.5 que valem revisar:

- A gitleaks-action não foi usada porque exige licença em repositórios de organização; a CI roda a CLI pela imagem Docker.
- O gitleaks não acusa a senha exposta no commit `77462ee` (o formato não casa com nenhuma regra), então não há `.gitleaksignore`. A troca da senha continua pendente.
- A meta de 80% no `sentry-auth` só aparece no resumo do job; vira bloqueante (goal `check` do JaCoCo) na Fase 1.

Decisões da 0.7 que valem revisar:

- Sem `SENTRY_ADMIN_USERNAME` e `SENTRY_ADMIN_EMAIL`, o padrão é `admin` e `admin@sentry.local`.
- Além do que a RN-F0-03.3 pede, o bootstrap cria o papel `SENTRY.ADMIN` no sistema `SENTRY` e vincula o administrador a ele, ainda sem ações.
- Se já existir usuário com o login ou o e-mail do administrador (e a plataforma não existir), a subida falha com mensagem clara em vez de reaproveitar o usuário.
- O re-hash da 0.6 (`PasswordService.verify(...).upgradedHash()`) passa a ser gravado no login a partir da F1-02.

Decisões da 0.4 que valem revisar:

- O `compose.yaml` tem padrões locais para `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` (`sentry`/`sentry`/`sentry-local`), para subir mesmo com um `.env` antigo; servem só para desenvolvimento.
- O Postgres fica exposto na porta 5432 do host, para rodar as aplicações fora do Compose.

## Fila da Fase 0

| # | Tarefa | Ponto de atenção |
| --- | --- | --- |
| 0.9 | Fechar a fase no `main` (o `init` já foi integrado) | Critério de saída da fase |

## Observação

Como a 0.6 alterou a `V1` (remoção de `salt`), um banco de desenvolvimento criado antes dela acusa erro de checksum no Flyway: basta recriá-lo.

Um banco que já subiu com o perfil `dev` acusa erro de validação do Flyway se for aberto sem esse perfil, porque a migração `R__dev_seed` não é mais encontrada. Para banco descartável de desenvolvimento isso basta; se incomodar, configurar `spring.flyway.ignore-migration-patterns`.
