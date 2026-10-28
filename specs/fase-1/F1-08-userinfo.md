# F1-08 — Endpoint UserInfo

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-05

## Regras

- **RN-F1-08.1** `GET /userinfo` DEVE aceitar um access token Bearer emitido com escopo `openid`.
- **RN-F1-08.2** A resposta DEVE conter `sub` e os claims dos escopos concedidos: `profile` → `name`, `preferred_username`; `email` → `email`.
- **RN-F1-08.3** Os dados DEVEM ser lidos do banco no momento da chamada, não do token.
- **RN-F1-08.4** Usuário inativo ou excluído DEVE receber `401` com `WWW-Authenticate: Bearer error="invalid_token"`.

## Critérios de aceite

- **CA-F1-08.1** Dado um access token com `openid profile`, então a resposta tem `sub`, `name` e `preferred_username` e não tem `email`.
- **CA-F1-08.2** Dado um token sem `openid`, então a resposta é `403 insufficient_scope`.
- **CA-F1-08.3** Dado um usuário cujo nome mudou depois do login, então `/userinfo` devolve o nome novo.
