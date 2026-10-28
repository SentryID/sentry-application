# F0-05 — Integração contínua

- **Fase:** 0
- **Status:** Proposto
- **Depende de:** F0-04

## Contexto

O repositório não tem CI. O `../../CLAUDE.md` exige testes para todo método de serviço; sem CI essa regra não é verificada.

## Regras

- **RN-F0-05.1** Todo push e pull request DEVE rodar, no GitHub Actions: build Maven, testes unitários e testes de integração.
- **RN-F0-05.2** Testes de integração DEVEM usar Testcontainers com o mesmo PostgreSQL do Compose, nunca H2.
- **RN-F0-05.3** A CI DEVE rodar varredura de segredos (gitleaks) e de dependências vulneráveis (OWASP Dependency-Check ou Dependabot).
- **RN-F0-05.4** O `main` DEVE ser protegido: merge só com CI verde e pull request.
- **RN-F0-05.5** A CI DEVE publicar o relatório de cobertura (JaCoCo). Meta de cobertura de linhas no `sentry-auth`: 80%.
- **RN-F0-05.6** Em merge no `main`, a CI DEVE gerar a imagem Docker do `sentry-auth` e publicá-la no GitHub Container Registry com a tag do commit.

## Critérios de aceite

- **CA-F0-05.1** Dado um pull request com um teste falhando, então o check da CI fica vermelho e o merge é bloqueado.
- **CA-F0-05.2** Dado um commit com uma string no formato de segredo (ex.: chave privada PEM), então a CI falha na etapa de gitleaks.
- **CA-F0-05.3** Dado um merge no `main`, então a imagem `ghcr.io/sentryid/sentry-auth:<sha>` passa a existir.

## Questões em aberto

- A meta de 80% de cobertura é bloqueante desde o início ou só informativa na Fase 0? Recomendado: informativa na Fase 0, bloqueante a partir da Fase 1.
