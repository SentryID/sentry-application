# F1-06 — Refresh token com rotação

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-05

## Regras

- **RN-F1-06.1** O refresh token DEVE ser emitido somente quando o escopo `offline_access` for pedido e permitido para o sistema.
- **RN-F1-06.2** O refresh token DEVE ser opaco (não JWT), com pelo menos 256 bits aleatórios, e armazenado só como hash.
- **RN-F1-06.3** `POST /oauth2/token` com `grant_type=refresh_token` DEVE devolver um novo access token e um novo refresh token; o anterior é invalidado (rotação).
- **RN-F1-06.4** Se um refresh token já rotacionado for reapresentado, toda a família de tokens daquela autorização DEVE ser revogada (detecção de reuso).
- **RN-F1-06.5** Vida padrão: 30 dias de inatividade, com vida máxima absoluta de 90 dias, configuráveis.
- **RN-F1-06.6** O refresh DEVE falhar se o usuário ou o sistema estiverem inativos ou excluídos naquele momento.
- **RN-F1-06.7** O refresh token DEVE estar preso ao `client_id` que o recebeu.
- **RN-F1-06.8** `POST /oauth2/revoke` (RFC 7009) DEVE permitir ao sistema revogar um refresh token.

## Critérios de aceite

- **CA-F1-06.1** Dado um refresh token válido, quando usado, então volta um par novo e o token usado passa a responder `invalid_grant`.
- **CA-F1-06.2** Dado um refresh token já rotacionado, quando reapresentado, então responde `invalid_grant` e o refresh token mais novo da mesma família também deixa de funcionar.
- **CA-F1-06.3** Dado um usuário desativado depois do login, quando o sistema tenta o refresh, então responde `invalid_grant`.
- **CA-F1-06.4** Dado um refresh token do sistema A, quando apresentado pelo sistema B, então responde `invalid_grant`.
- **CA-F1-06.5** Dado um refresh token revogado via `/oauth2/revoke`, então ele não funciona mais.
