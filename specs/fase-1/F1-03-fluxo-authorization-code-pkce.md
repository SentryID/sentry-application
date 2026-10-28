# F1-03 — Fluxo Authorization Code + PKCE

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-01, F1-02, F1-04

## Contexto

É o fluxo padrão de login: o sistema externo redireciona o usuário ao Sentry, o usuário se autentica na tela do Sentry, e o sistema recebe um código que troca por tokens.

## Regras

- **RN-F1-03.1** O endpoint `GET /oauth2/authorize` DEVE aceitar `response_type=code`, `client_id`, `redirect_uri`, `scope`, `state`, `nonce`, `code_challenge` e `code_challenge_method=S256`.
- **RN-F1-03.2** PKCE com `S256` DEVE ser obrigatório para todos os clientes, públicos e confidenciais. `plain` NÃO DEVE ser aceito.
- **RN-F1-03.3** Se `client_id` ou `redirect_uri` forem inválidos, o Sentry NÃO DEVE redirecionar: mostra uma página de erro.
- **RN-F1-03.4** Outros erros (escopo inválido, PKCE ausente) DEVEM voltar ao `redirect_uri` com `error` e o `state` original.
- **RN-F1-03.5** Sem sessão SSO válida (F1-09), o usuário DEVE ser levado à tela de login (F1-04). Com sessão válida, o código é emitido sem pedir senha de novo.
- **RN-F1-03.6** O código de autorização DEVE valer 60 segundos e ser de uso único. Um segundo uso DEVE invalidar os tokens emitidos com ele.
- **RN-F1-03.7** `POST /oauth2/token` com `grant_type=authorization_code` DEVE exigir `code`, `redirect_uri` idêntico ao da autorização e `code_verifier`. Clientes confidenciais também autenticam com `client_secret_basic` ou `client_secret_post`.
- **RN-F1-03.8** Os fluxos `implicit` e `password` do OAuth NÃO DEVEM estar habilitados (o login direto é o endpoint próprio de F1-11).
- **RN-F1-03.9** Tela de consentimento NÃO DEVE ser exibida na Fase 1: todos os sistemas são cadastrados pelo administrador.
- **RN-F1-03.10** O `POST /login` JSON atual do `LoginController` DEVE ser substituído pelo endpoint de F1-11; o caminho `/login` passa a servir a tela de F1-04.

## Critérios de aceite

- **CA-F1-03.1** Dado um cliente público com PKCE válido, quando o usuário faz login, então o navegador volta ao `redirect_uri` com `code` e o mesmo `state`, e a troca no `/oauth2/token` devolve access token, ID token e (com `offline_access`) refresh token.
- **CA-F1-03.2** Dada uma autorização sem `code_challenge`, então o retorno é `error=invalid_request`.
- **CA-F1-03.3** Dado um `code_verifier` que não confere, então `/oauth2/token` responde `400 invalid_grant`.
- **CA-F1-03.4** Dado um código já usado, quando é reapresentado, então responde `invalid_grant` e o refresh token emitido na primeira troca é revogado.
- **CA-F1-03.5** Dado um `redirect_uri` não cadastrado, então o Sentry mostra página de erro e não redireciona.
- **CA-F1-03.6** Dada uma biblioteca OIDC genérica (ex.: `oidc-client-ts` ou Spring Security OAuth2 Client) configurada só com issuer e `client_id`, então o login completa sem código específico do Sentry.

## Notas técnicas

- Implementado com o Authorization Server do Spring Security (D-02). Autorizações persistidas com `JdbcOAuth2AuthorizationService` em tabela própria (migração V2).
