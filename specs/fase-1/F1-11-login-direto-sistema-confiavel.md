# F1-11 — Login direto por API para sistemas confiáveis

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-01, F1-02, F1-05

## Contexto

Decisão D-01: além do fluxo PKCE, o Sentry mantém um login direto, equivalente ao contrato atual do `LoginController` (`username`, `password`, `system`). Nele o sistema externo coleta a senha na própria tela e a envia ao Sentry, então só é permitido a sistemas marcados como confiáveis.

## Regras

- **RN-F1-11.1** O endpoint DEVE ser `POST /api/v1/auth/login`, separado da tela `POST /login` de F1-04.
- **RN-F1-11.2** O sistema chamador DEVE se autenticar como cliente confidencial (HTTP Basic com `client_id` e `client_secret`). O campo `system` do corpo deixa de existir: o sistema é o cliente autenticado.
- **RN-F1-11.3** Só sistemas `CONFIDENTIAL` com `is_trusted = true` PODEM usar o endpoint. Os demais recebem `403` com `unauthorized_client`.
- **RN-F1-11.4** O corpo DEVE ter `username`, `password` e, opcionalmente, `scope` (padrão `openid profile`).
- **RN-F1-11.5** A validação DEVE ser feita pelo mesmo provedor de F1-02, com as mesmas mensagens genéricas.
- **RN-F1-11.6** A resposta de sucesso DEVE seguir o formato de resposta de token do OAuth: `access_token`, `token_type=Bearer`, `expires_in`, `id_token`, `refresh_token` (com `offline_access`) e `scope`. Os tokens são os mesmos de F1-05 e F1-06.
- **RN-F1-11.7** Falha de credencial DEVE responder `401` com `{"error":"invalid_grant","error_description":"Usuário ou senha inválidos"}`.
- **RN-F1-11.8** O login direto NÃO DEVE criar sessão SSO (F1-09): é um login só para aquele sistema.
- **RN-F1-11.9** Toda tentativa DEVE ser registrada com canal `DIRECT` (F1-10).
- **RN-F1-11.10** O endpoint DEVE aceitar apenas HTTPS fora de localhost e DEVE ser documentado no OpenAPI do sentry-auth.

## Critérios de aceite

- **CA-F1-11.1** Dado um sistema confiável com segredo correto e credenciais válidas do usuário, então a resposta é `200` com `access_token` e `id_token` válidos pelo JWKS.
- **CA-F1-11.2** Dado um sistema confidencial não confiável, então a resposta é `403 unauthorized_client`, mesmo com credenciais válidas.
- **CA-F1-11.3** Dado um sistema público, então a resposta é `401 invalid_client`.
- **CA-F1-11.4** Dada uma senha errada, então a resposta é `401 invalid_grant` com a mensagem genérica, e um registro `INVALID_CREDENTIALS` com canal `DIRECT` existe.
- **CA-F1-11.5** Dado um login direto bem-sucedido, quando o usuário abre o fluxo PKCE de outro sistema, então a senha é pedida (não há sessão SSO).
- **CA-F1-11.6** Dado o refresh token devolvido, então ele funciona pelo `/oauth2/token` como em F1-06.

## Questões em aberto

- Quem pode marcar um sistema como confiável: só o superadministrador da plataforma? Recomendado: sim.
