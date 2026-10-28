# Próximos passos — Fase 0

Situação em 27/10/2026, branch `init`.

## Feito

| # | Tarefa | Commit |
| --- | --- | --- |
| 0.1 | `seed-template.sql` removido; dados fictícios em `db/dev/R__dev_seed.sql`, só no perfil `dev` | `b113adc` |
| 0.2 | `role_actions.cd_action` em `VARCHAR(255)`; entidade `Action` com código natural (`String code`); `spring-boot-starter-flyway`; testes com Testcontainers (CA-F0-01.1 a .3) | `b113adc` |
| 0.3 | Entidade `System` renomeada para `ExternalSystem` | `b7dace0` |

Achados que a spec não previa, já tratados:

- A entidade `Action` mapeava `cd_action` como `Long` gerado pelo banco; só corrigir o SQL não bastava para o `ddl-auto: validate`.
- No Spring Boot 4, `flyway-core` sozinho não roda as migrações: é preciso o `spring-boot-starter-flyway`.
- Como o código da ação é a chave primária, `ActionService.save` passou a recusar código duplicado (409); sem isso o `save` do JPA sobrescreveria a ação existente.

## Decisões tomadas

As recomendações das **Questões em aberto** da Fase 0 foram aceitas:

- Renomear `System` agora (feito na 0.3).
- `sentry-backend` entra no Compose (0.4).
- Cobertura de 80% informativa na Fase 0, bloqueante a partir da Fase 1.
- Não reescrever o histórico do Git: apenas trocar a senha exposta.
- Política de senha (mínimo de 8 caracteres, bloqueio de senhas comuns) fica para a Fase 1.

## Pendências fora do código

- Trocar a senha exposta no commit `77462ee` em todo lugar onde é usada (RN-F0-03.2).
- Instalar o JDK 25 (o `pom.xml` exige `release 25`). Até lá, os testes rodam em container:

  ```
  docker run --rm -v "<repo>:/workspace" -w /workspace -v sentry-m2:/root/.m2 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
    maven:3.9-eclipse-temurin-25 mvn verify
  ```

## Próximo passo: 0.6 — Argon2id (F0-02)

A ordem do roadmap foi ajustada para **0.6 → 0.7 → 0.4 → 0.5 → 0.8 → 0.9**: o bootstrap (0.7) depende do encoder, e o Compose (0.4) precisa do bootstrap para atender o CA-F0-04.2.

Plano da 0.6:

1. **Dependências no `sentry-api`:** `spring-security-crypto` e Bouncy Castle (`bcprov-jdk18on`), exigido pelo `Argon2PasswordEncoder`.
2. **Encoder compartilhado:** bean `PasswordEncoder` no `sentry-api`, um `DelegatingPasswordEncoder` com `argon2` como padrão, gerando `{argon2}$argon2id$v=19$m=19456,t=2,p=1$...` (RN-F0-02.1, .3).
3. **Parâmetros por propriedade:** `sentry.security.password.argon2.memory`, `.iterations` e `.parallelism`, com os mínimos da OWASP como padrão (RN-F0-02.2).
4. **Re-hash:** método de serviço que verifica a senha e, se `upgradeEncoding` indicar parâmetros mais fracos, devolve o novo hash para gravar (RN-F0-02.4). Ele fica pronto e testado na 0.6 e passa a ser chamado no login a partir da F1-02.
5. **Remover `salt`:** coluna sai da `V1` (permitido por RN-F0-01.4) e campo sai da entidade `User` (RN-F0-02.5).
6. **`UserService.save`:** grava a senha com hash; a resposta continua sem `password` e `salt` (RN-F0-02.7).
7. **Testes:**
   - CA-F0-02.1: integração no `sentry-backend` criando usuário pela API e lendo a linha em `users`.
   - CA-F0-02.2 e .3: unitários do encoder e do re-hash (hash com 8 MiB vira 19 MiB).
   - CA-F0-02.4: a resposta de consulta de usuário não tem `password` nem `salt`.

## Depois da 0.6

| # | Tarefa | Ponto de atenção |
| --- | --- | --- |
| 0.7 | Bootstrap idempotente do administrador e do sistema `SENTRY` | Senha gerada (≥ 20 caracteres) aparece uma única vez no log; o seed `dev` não pode colidir com a organização da plataforma |
| 0.4 | `compose.yaml` (PostgreSQL 17, `sentry-auth`, `sentry-backend`), `Dockerfile` multi-stage com Temurin 25 e usuário não root, actuator, `.env.example` completo | `SENTRY_ISSUER_URL` já entra aqui (RN-F0-04.5) |
| 0.5 | GitHub Actions: build, testes com Testcontainers, JaCoCo, gitleaks, Dependabot | JDK 25 no runner |
| 0.8 | Imagem `ghcr.io/sentryid/sentry-auth:<sha>` no merge e proteção do `main` | Proteção do branch é configuração do GitHub, feita por quem administra o repositório |
| 0.9 | Integrar `init` ao `main` | Critério de saída da fase |

## Observação

Um banco que já subiu com o perfil `dev` acusa erro de validação do Flyway se for aberto sem esse perfil, porque a migração `R__dev_seed` não é mais encontrada. Para banco descartável de desenvolvimento isso basta; se incomodar, configurar `spring.flyway.ignore-migration-patterns`.
