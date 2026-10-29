# Próximos passos — Fase 0

Situação em 29/10/2026. O `init` já foi integrado ao `main`; a 0.6, a 0.7 e a 0.4 estão na branch `fase-0/argon2id`.

## Feito

| # | Tarefa | Commit |
| --- | --- | --- |
| 0.1 | `seed-template.sql` removido; dados fictícios em `db/dev/R__dev_seed.sql`, só no perfil `dev` | `b113adc` |
| 0.2 | `role_actions.cd_action` em `VARCHAR(255)`; entidade `Action` com código natural (`String code`); `spring-boot-starter-flyway`; testes com Testcontainers (CA-F0-01.1 a .3) | `b113adc` |
| 0.3 | Entidade `System` renomeada para `ExternalSystem` | `b7dace0` |
| 0.6 | Argon2id com prefixo `{argon2}` via `DelegatingPasswordEncoder`; parâmetros em `sentry.security.password.argon2.*` (mínimos da OWASP validados na subida); `PasswordService.verify` devolve o novo hash quando os parâmetros ficaram mais fortes; coluna `salt` removida; `UserService.save` grava o hash; testes CA-F0-02.1 a .4 | branch `fase-0/argon2id` |
| 0.7 | `BootstrapService` no `sentry-auth`: na primeira subida cria a organização `SENTRY_PLATFORM`, o sistema `SENTRY`, o papel `SENTRY.ADMIN` e o administrador (`SENTRY_ADMIN_USERNAME`, `SENTRY_ADMIN_EMAIL`, `SENTRY_ADMIN_PASSWORD`); sem senha, gera uma de 24 caracteres e a mostra uma vez no log; idempotente pela organização; testes CA-F0-03.1 a .3 | branch `fase-0/argon2id` |
| 0.4 | `compose.yaml` com `postgres:17-alpine` (volume nomeado, healthcheck `pg_isready`), `sentry-auth` e `sentry-backend`; `Dockerfile` multi-stage na raiz (`ARG MODULE`; build com `maven:3-eclipse-temurin-27`, runtime `eclipse-temurin:27-jre-alpine`, usuário `sentry`); actuator só com `health` e probes; `sentry.issuer-url` (`SENTRY_ISSUER_URL`, padrão `http://localhost:8081`); `.env.example` completo; `README.md`. CA-F0-04.1 a .3 verificados à mão: tudo `UP` em ~30 s com build, bootstrap roda de novo após `down -v`, processo não é root | branch `fase-0/argon2id` |

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

## Pendências fora do código

- Trocar a senha exposta no commit `77462ee` em todo lugar onde é usada (RN-F0-03.2).

## Próximo passo: 0.5 — Integração contínua (F0-05)

A ordem segue: **0.5 → 0.8 → 0.9**. O runner precisa do JDK 27 e de Docker para os testes com Testcontainers.

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
| 0.5 | GitHub Actions: build, testes com Testcontainers, JaCoCo, gitleaks, Dependabot | JDK 27 no runner |
| 0.8 | Imagem `ghcr.io/sentryid/sentry-auth:<sha>` no merge e proteção do `main` | Proteção do branch é configuração do GitHub, feita por quem administra o repositório |
| 0.9 | Fechar a fase no `main` (o `init` já foi integrado) | Critério de saída da fase |

## Observação

Como a 0.6 alterou a `V1` (remoção de `salt`), um banco de desenvolvimento criado antes dela acusa erro de checksum no Flyway: basta recriá-lo.

Um banco que já subiu com o perfil `dev` acusa erro de validação do Flyway se for aberto sem esse perfil, porque a migração `R__dev_seed` não é mais encontrada. Para banco descartável de desenvolvimento isso basta; se incomodar, configurar `spring.flyway.ignore-migration-patterns`.
