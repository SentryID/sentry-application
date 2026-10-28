# F0-01 — Migração do banco executável do zero

- **Fase:** 0
- **Status:** Proposto
- **Depende de:** —

## Contexto

A migração `V1__initial_schema.sql` do `sentry-api` não roda num PostgreSQL vazio: `role_actions.cd_action` é `BIGINT`, mas referencia `actions.cd_action`, que é `VARCHAR(255)`. Os serviços usam `ddl-auto: validate`, então também não sobem. Hoje o código vive no branch `init`; o `main` só tem o commit inicial.

## Regras

- **RN-F0-01.1** As migrações Flyway DEVEM executar sem erro num PostgreSQL vazio.
- **RN-F0-01.2** `role_actions.cd_action` DEVE ter o mesmo tipo de `actions.cd_action` (`VARCHAR(255)`).
- **RN-F0-01.3** O mapeamento JPA de todas as entidades DEVE passar na validação do Hibernate (`ddl-auto: validate`) contra o esquema migrado.
- **RN-F0-01.4** Como ainda não há instalação em produção, a correção PODE ser feita editando a V1. A partir do primeiro release, toda mudança DEVE ser uma nova migração (`V2__...`).
- **RN-F0-01.5** O branch `init` DEVE ser integrado ao `main` ao fim da Fase 0.

## Critérios de aceite

- **CA-F0-01.1** Dado um PostgreSQL vazio, quando o `sentry-auth` sobe, então o Flyway aplica todas as migrações e a aplicação fica saudável.
- **CA-F0-01.2** Dado o esquema migrado, quando o contexto Spring do `sentry-backend` sobe com `ddl-auto: validate`, então nenhum erro de validação de esquema ocorre.
- **CA-F0-01.3** Dado o esquema migrado, quando se insere um `role_action` apontando para uma ação existente pelo código (ex.: `SENTRY.USER.VIEW`), então a inserção é aceita.

## Notas técnicas

- O teste de CA-F0-01.1 e CA-F0-01.2 roda com Testcontainers (ver F0-05).
- A entidade `System` sombreia `java.lang.System`; vale renomear para `ExternalSystem` ou similar enquanto não há consumidores externos.

## Questões em aberto

- Renomear a entidade `System` agora (recomendado) ou depois?
