# F0-04 — Ambiente local com Docker Compose

- **Fase:** 0
- **Status:** Proposto
- **Depende de:** F0-01

## Contexto

Hoje subir o projeto exige um PostgreSQL configurado à mão e as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Quem for validar ou contribuir precisa subir tudo com um comando.

## Regras

- **RN-F0-04.1** O repositório DEVE ter um `compose.yaml` que sobe PostgreSQL 17 e o `sentry-auth` com `docker compose up`.
- **RN-F0-04.2** O `sentry-auth` DEVE ter um `Dockerfile` multi-stage que gera uma imagem sem ferramentas de build, rodando como usuário não root.
- **RN-F0-04.3** Toda configuração sensível DEVE vir de variável de ambiente; um `.env.example` DEVE listar todas, sem valores reais. O `.env` DEVE estar no `../../.gitignore`.
- **RN-F0-04.4** O serviço DEVE expor `/actuator/health` (liveness e readiness), usado pelo healthcheck do Compose.
- **RN-F0-04.5** O `sentry-auth` DEVE ter uma URL pública configurável (`SENTRY_ISSUER_URL`), usada como `issuer` dos tokens (F1-07). Padrão local: `http://localhost:8081`.
- **RN-F0-04.6** O README do repositório DEVE explicar como subir, parar e zerar o ambiente.

## Critérios de aceite

- **CA-F0-04.1** Dada uma máquina só com Docker, quando se roda `cp .env.example .env && docker compose up`, então em até 2 minutos `/actuator/health` responde `UP`.
- **CA-F0-04.2** Dado o ambiente no ar, quando se roda `docker compose down -v` e sobe de novo, então o banco é recriado e o bootstrap (F0-03) roda de novo.
- **CA-F0-04.3** Dada a imagem gerada, quando se inspeciona o usuário do processo, então não é `root`.

## Questões em aberto

- Incluir o `sentry-backend` no mesmo Compose já na Fase 0? Recomendado: sim, porque é por ele que se cadastra o sistema externo na Fase 1.
