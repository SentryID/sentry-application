# F2-02 — Política de senha e troca obrigatória

- **Fase:** 2
- **Status:** Proposto
- **Depende de:** F0-02, F0-03, F1-04

## Contexto

Hoje qualquer senha não vazia é aceita (só o `@NotBlank` do `UserRest.SaveRequest`), e o administrador criado pelo bootstrap com senha gerada não é obrigado a trocá-la (RN-F0-03.4 deixou a troca para a Fase 2). A política segue o NIST SP 800-63B: tamanho mínimo e lista de senhas proibidas, sem regras de composição nem expiração periódica.

## Regras

- **RN-F2-02.1** Toda senha definida (cadastro, troca, recuperação em F2-03, bootstrap com `SENTRY_ADMIN_PASSWORD`) DEVE ter de 8 a 128 caracteres, contados em pontos de código Unicode, após normalização NFKC.
- **RN-F2-02.2** A senha NÃO DEVE estar na lista de senhas comuns embarcada no `sentry-api`, nem ser igual (sem diferenciar maiúsculas) ao username ou à parte local do e-mail.
- **RN-F2-02.3** NÃO DEVE haver regra de composição (maiúscula, número, símbolo) nem expiração periódica.
- **RN-F2-02.4** Recusas DEVEM vir com uma mensagem em português que diga qual regra falhou, sem ecoar a senha.
- **RN-F2-02.5** `users` DEVE ganhar `password_change_required` (`BOOLEAN NOT NULL DEFAULT FALSE`) e `password_changed_at` (`TIMESTAMPTZ`).
- **RN-F2-02.6** O bootstrap com senha gerada DEVE marcar `password_change_required = true`. O `sentry-backend` DEVE permitir marcar o campo ao criar ou editar um usuário.
- **RN-F2-02.7** No login pela tela (F1-04), um usuário com troca obrigatória DEVE ser levado a `GET /password/change` antes de o fluxo de autorização continuar; só depois da troca o código é emitido.
- **RN-F2-02.8** A tela de troca DEVE pedir a senha atual e a nova duas vezes, com CSRF, e aplicar a política. Ao trocar, `password_change_required` volta a `false`, `password_changed_at` é gravado e as outras sessões SSO e os refresh tokens do usuário são revogados.
- **RN-F2-02.9** No login direto (F1-11), um usuário com troca obrigatória DEVE receber `403 {"error":"password_change_required"}`; a troca é feita só pela tela hospedada.

## Critérios de aceite

- **CA-F2-02.1** Dada a senha `1234567` (7 caracteres), então o cadastro de usuário responde `400` com a regra de tamanho.
- **CA-F2-02.2** Dada a senha `password123` (da lista), então a resposta é `400` com a regra de senha comum.
- **CA-F2-02.3** Dada uma senha de 12 letras minúsculas fora da lista, então é aceita (sem regra de composição).
- **CA-F2-02.4** Dado o administrador do bootstrap com senha gerada, quando faz o primeiro login no fluxo PKCE, então vê a tela de troca, e o sistema só recebe o código depois da troca.
- **CA-F2-02.5** Dada a troca concluída, então o refresh token emitido antes dela deixa de funcionar.
- **CA-F2-02.6** Dado um usuário com troca obrigatória no login direto, então a resposta é `403 password_change_required`.

## Notas técnicas

- A política fica num componente do `sentry-api` (`PasswordPolicy`), usado pelo `UserService`, pelo bootstrap e pelas telas do `sentry-auth`.
- Lista de senhas comuns: arquivo de texto no classpath, carregado uma vez em um `Set`.
- O desvio para a troca obrigatória é um `AuthenticationSuccessHandler` que guarda o pedido de autorização salvo e redireciona para `/password/change`.

## Questões em aberto

- Consultar também o Have I Been Pwned (k-anonimato, só os 5 primeiros caracteres do SHA-1 saem)? Recomendado: opcional por configuração, desligado por padrão, para não depender de serviço externo.
- Qual lista embarcar? Recomendado: as 10 mil senhas mais comuns do SecLists (licença MIT), filtradas para 8 caracteres ou mais.
- Tela "Alterar senha" para o usuário trocar quando quiser, fora da troca obrigatória? Recomendado: sim, a mesma tela, acessível com sessão SSO ativa.
