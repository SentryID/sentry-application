# F1-09 — Sessão e logout

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-03

## Regras

- **RN-F1-09.1** Após login na tela (F1-04), o sentry-auth DEVE criar uma sessão SSO em cookie `HttpOnly`, `Secure` (fora de localhost) e `SameSite=Lax`.
- **RN-F1-09.2** A sessão DEVE expirar após 30 minutos de inatividade e 12 horas no máximo, configuráveis.
- **RN-F1-09.3** O identificador da sessão DEVE ser renovado no login (proteção contra fixação de sessão).
- **RN-F1-09.4** A sessão DEVE ser persistida no PostgreSQL (Spring Session JDBC), para que o serviço seja sem estado e escale horizontalmente.
- **RN-F1-09.5** `GET /connect/logout` (RP-initiated logout do OIDC) DEVE aceitar `id_token_hint`, `post_logout_redirect_uri` e `state`, encerrar a sessão SSO e redirecionar somente para URIs cadastradas (F1-01).
- **RN-F1-09.6** O logout DEVE revogar os refresh tokens emitidos naquela sessão (`sid`).
- **RN-F1-09.7** Um sistema PODE pedir `prompt=login` na autorização para forçar nova digitação de senha mesmo com sessão ativa.

## Critérios de aceite

- **CA-F1-09.1** Dado um usuário logado no sistema A, quando o sistema B inicia o fluxo de F1-03, então o código é emitido sem pedir senha.
- **CA-F1-09.2** Dado um logout com `post_logout_redirect_uri` cadastrada, então a sessão acaba e o navegador volta para ela com o `state`.
- **CA-F1-09.3** Dado um logout com `post_logout_redirect_uri` não cadastrada, então a sessão acaba e o usuário vê uma página do Sentry, sem redirecionar.
- **CA-F1-09.4** Dados 31 minutos sem atividade, então o próximo fluxo de F1-03 pede senha.
- **CA-F1-09.5** Dado `prompt=login`, então a senha é pedida mesmo com sessão ativa.

## Questões em aberto

- Back-channel logout (avisar os outros sistemas da sessão) fica fora da Fase 1? Recomendado: sim.
