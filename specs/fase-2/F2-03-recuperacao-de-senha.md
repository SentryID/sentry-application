# F2-03 — Recuperação de senha por e-mail

- **Fase:** 2
- **Status:** Proposto
- **Depende de:** F2-02, F2-05

## Contexto

A tabela `password_tokens` existe desde a V1 (token, uso único, validade, usuário), mas nada a usa, e a tela de login (do front-end) não oferece o "Esqueci minha senha" (RN-F1-04.8). A recuperação usa um link de uso único enviado ao e-mail cadastrado.

## Regras

O `sentry-auth` não tem telas: a tela de login e as demais são do front-end (React), servido na mesma origem, que chama a API de sessão (`/api/v1/session/**`, cookie `SESSION` e CSRF por `X-XSRF-TOKEN`, ver `../../docs/integracao_frontend.md`).

- **RN-F2-03.1** A tela de login do front-end DEVE oferecer "Esqueci minha senha". O pedido de autorização em andamento continua guardado na sessão.
- **RN-F2-03.2** `POST /api/v1/password/forgot` DEVE aceitar username ou e-mail e responder sempre `202` com o mesmo corpo, exista o usuário ou não, esteja ativo ou não. O tempo de resposta NÃO DEVE revelar a diferença (o envio é assíncrono).
- **RN-F2-03.3** O token DEVE ter pelo menos 256 bits aleatórios, ir só no link do e-mail e ser gravado em `password_tokens` apenas como hash SHA-256. Ele vale 30 minutos (configurável) e uma única vez.
- **RN-F2-03.4** Gerar um token novo DEVE invalidar os tokens anteriores ainda não usados do mesmo usuário.
- **RN-F2-03.5** `POST /api/v1/password/reset/validate` DEVE dizer se um token ainda vale, para o front-end mostrar o formulário; token vencido, usado ou desconhecido recebe a mesma resposta `400` ("Link inválido ou expirado").
- **RN-F2-03.6** `POST /api/v1/password/reset` (token e nova senha) DEVE seguir a política de F2-02. Ao gravar: o token é marcado como usado, `password_change_required` volta a `false`, e todas as sessões SSO e refresh tokens do usuário são revogados.
- **RN-F2-03.7** Usuários com `authentication_type` diferente de `DATABASE` NÃO DEVEM receber e-mail de recuperação.
- **RN-F2-03.8** O envio DEVE usar SMTP configurável (`SENTRY_MAIL_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD`, `_FROM`, TLS). O Compose DEVE incluir o Mailpit para desenvolvimento.
- **RN-F2-03.9** Pedidos de recuperação DEVEM entrar na limitação de F2-05 (por endereço informado e por IP) e ser registrados em `access_logs` com resultados próprios (`RESET_REQUESTED`, `RESET_COMPLETED`, `RESET_INVALID_TOKEN`).
- **RN-F2-03.10** O e-mail DEVE estar em português, com o nome do Sentry, a validade do link e o aviso de ignorar se o pedido não foi da pessoa. O link DEVE ser `SENTRY_FRONTEND_URL` + `/reset?token=...` (a tela do front-end), nunca montado a partir do `Host` da requisição.

## Critérios de aceite

- **CA-F2-03.1** Dado um e-mail cadastrado e outro inexistente, então as duas respostas são idênticas, e só o primeiro gera e-mail no Mailpit.
- **CA-F2-03.2** Dado um token já usado, quando validado de novo, então a resposta é `400` "Link inválido ou expirado".
- **CA-F2-03.3** Dado um link com mais de 30 minutos, então é recusado.
- **CA-F2-03.4** Dado o banco, então `password_tokens.token` não contém o valor do link.
- **CA-F2-03.5** Dada a redefinição concluída, então o refresh token emitido antes deixa de funcionar e a senha antiga não autentica.
- **CA-F2-03.6** Dado um `Host` forjado na requisição, então o link do e-mail continua com `SENTRY_FRONTEND_URL`.

## Notas técnicas

- `spring-boot-starter-mail`; envio em `@Async` depois da resposta, com falha registrada no log sem o token.
- A coluna `password_tokens.token` passa a guardar o hash (migração V5); o tamanho atual de 255 basta.

## Questões em aberto

- Aceitar username além do e-mail no formulário? Recomendado: sim, porque o login é por username (decisão de F1-02).
- Redefinir a senha também autentica o usuário e continua o fluxo de autorização? Recomendado: não; o front-end volta à tela de login com a mensagem "Senha redefinida", para exigir a nova senha (e o MFA de F2-04).
