# Roadmap — sentry-auth, Fases 0 e 1

A Fase 0 deixa o projeto executável, seguro e testável; a Fase 1 entrega a autenticação de um usuário por um sistema externo, pelo fluxo PKCE e pelo login direto. Cada tarefa abaixo fecha quando os critérios de aceite citados viram testes verdes na CI.

## Dependências

```mermaid
flowchart LR
  subgraph F0[Fase 0 · Fundação]
    F001[F0-01 Migração] --> F002[F0-02 Hash de senha]
    F001 --> F004[F0-04 Docker Compose]
    F002 --> F003[F0-03 Seed e bootstrap]
    F004 --> F005[F0-05 CI]
  end
  subgraph F1[Fase 1 · Autenticação]
    F101[F1-01 Sistema como cliente]
    F102[F1-02 Credenciais] --> F104[F1-04 Tela de login]
    F102 --> F110[F1-10 Registro de tentativas]
    F107[F1-07 Descoberta e JWKS]
    F101 --> F103[F1-03 PKCE]
    F104 --> F103
    F103 --> F105[F1-05 Tokens]
    F107 --> F105
    F105 --> F106[F1-06 Refresh]
    F105 --> F108[F1-08 UserInfo]
    F103 --> F109[F1-09 Sessão e logout]
    F105 --> F111[F1-11 Login direto]
    F101 --> F111
  end
  F001 --> F101
  F002 --> F102
  F004 --> F107
  F005 --> F1
```

## Fase 0 — Fundação

**Critério de saída:** o branch `init` está no `main`, a CI está verde, e `docker compose up` sobe PostgreSQL, `sentry-auth` e `sentry-backend` com o administrador criado por variável de ambiente.

| # | Tarefa | Requisito | Critérios |
| --- | --- | --- | --- |
| 0.1 | Trocar a senha exposta no seed e substituir o seed por dados fictícios | F0-03 | CA-F0-03.4 |
| 0.2 | Corrigir o tipo de `role_actions.cd_action` na V1 e validar o esquema com Testcontainers | F0-01 | CA-F0-01.1 a .3 |
| 0.3 | (Opcional) Renomear a entidade `System` | F0-01 | — |
| 0.4 | `compose.yaml`, `Dockerfile` do `sentry-auth`, `.env.example` e health check | F0-04 | CA-F0-04.1 a .3 |
| 0.5 | Workflow do GitHub Actions: build, testes, Testcontainers, JaCoCo, gitleaks | F0-05 | CA-F0-05.1, .2 |
| 0.6 | Encoder Argon2id com prefixo no `sentry-api`, uso no `sentry-backend`, remoção de `salt` | F0-02 | CA-F0-02.1 a .4 |
| 0.7 | Bootstrap idempotente do administrador e do sistema `SENTRY` | F0-03 | CA-F0-03.1 a .3 |
| 0.8 | Publicar imagem no GHCR e proteger o `main` | F0-05 | CA-F0-05.3 |
| 0.9 | Integrar `init` ao `main` | F0-01 | Saída da fase |

## Fase 1 — Autenticação de usuário por sistema externo

**Critério de saída:** uma aplicação de exemplo, usando só uma biblioteca OIDC genérica, faz login via PKCE, renova o token, chama `/userinfo` e faz logout; e um sistema confiável faz login direto e recebe os mesmos tokens.

| # | Tarefa | Requisito | Critérios |
| --- | --- | --- | --- |
| 1.1 | Migração V2: colunas de cliente em `systems`, `system_redirect_uris`, colunas novas em `access_logs`, tabelas de autorização e de sessão | F1-01, F1-10, F1-03, F1-09 | — |
| 1.2 | CRUD dos dados de cliente e rotação de segredo no `sentry-backend` | F1-01 | CA-F1-01.1, .2, .4 |
| 1.3 | `DatabaseAuthenticationProvider` com regra de acesso ao sistema e tempo constante | F1-02 | CA-F1-02.1 a .5 |
| 1.4 | Gravação de `access_logs` e log estruturado | F1-10 | CA-F1-10.1 a .4 |
| 1.5 | Configurar o Authorization Server: `RegisteredClientRepository` sobre `systems`, chave de assinatura externa, descoberta e JWKS | F1-07, F1-01 | CA-F1-07.1 a .4, CA-F1-01.3 |
| 1.6 | Tela de login Thymeleaf com CSRF e cabeçalhos de segurança | F1-04 | CA-F1-04.1 a .4 |
| 1.7 | Fluxo `/oauth2/authorize` + `/oauth2/token` com PKCE obrigatório | F1-03 | CA-F1-03.1 a .5 |
| 1.8 | Customização dos claims do access token e do ID token | F1-05 | CA-F1-05.1 a .5 |
| 1.9 | Refresh token opaco com rotação, detecção de reuso e `/oauth2/revoke` | F1-06 | CA-F1-06.1 a .5 |
| 1.10 | `/userinfo` lendo do banco | F1-08 | CA-F1-08.1 a .3 |
| 1.11 | Sessão SSO em JDBC, `prompt=login` e `/connect/logout` | F1-09 | CA-F1-09.1 a .5 |
| 1.12 | `POST /api/v1/auth/login` para sistemas confiáveis, removendo o `LoginController` atual | F1-11 | CA-F1-11.1 a .6 |
| 1.13 | Aplicação de exemplo (SPA com `oidc-client-ts`) e teste ponta a ponta | F1-03 | CA-F1-03.6, saída da fase |
| 1.14 | Documentação: como cadastrar um sistema e integrar pelos dois caminhos | Todos | — |

As tarefas 1.3 e 1.4 podem andar em paralelo com 1.2 e 1.5; 1.12 só depende de 1.3, 1.5 e 1.8, e pode ser feita antes de 1.11.

## Decisões pendentes que afetam o roadmap

Cada arquivo tem uma seção **Questões em aberto**. As que mudam o plano:

- F1-02: exigir papel no sistema para autenticar (muda a tarefa 1.3).
- F1-01: `client_id` = `external_code` ou `uuid` (muda a migração 1.1).
- F0-04: incluir o `sentry-backend` no Compose (muda a tarefa 0.4).
