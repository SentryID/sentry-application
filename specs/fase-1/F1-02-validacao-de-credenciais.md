# F1-02 — Validação de credenciais do usuário

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F0-02

## Contexto

`DatabaseAuthenticationProvider` existe vazio. Ele é o ponto único que decide se usuário e senha são válidos, usado pela tela de login (F1-04) e pelo login direto (F1-11).

## Regras

- **RN-F1-02.1** O usuário DEVE ser localizado por `username`, sem diferenciar maiúsculas de minúsculas. Proposta: aceitar também o e-mail.
- **RN-F1-02.2** A autenticação só DEVE ter sucesso se o usuário existir, `is_active = true`, `is_deleted = false`, `authentication_type = DATABASE` e a senha conferir com o hash.
- **RN-F1-02.3** Qualquer falha DEVE retornar a mesma mensagem ao usuário ("Usuário ou senha inválidos"), sem revelar se o usuário existe, está inativo ou a senha está errada.
- **RN-F1-02.4** Quando o usuário não existe, o sistema DEVE ainda executar uma verificação de hash simulada, para que o tempo de resposta não revele a existência do usuário.
- **RN-F1-02.5** O usuário DEVE ter permissão de acesso ao sistema externo que pediu a autenticação. Proposta: pelo menos um papel ativo (`role_users` → `roles`) naquele sistema.
- **RN-F1-02.6** O motivo real da falha DEVE ser registrado internamente (F1-10), nunca na resposta.
- **RN-F1-02.7** O provedor DEVE ser plugável por `authentication_type`, para que LDAP entre depois sem mudar F1-03 nem F1-11.

## Critérios de aceite

- **CA-F1-02.1** Dado um usuário ativo com senha correta e papel no sistema, quando autentica, então tem sucesso.
- **CA-F1-02.2** Dados (a) usuário inexistente, (b) senha errada, (c) usuário inativo, (d) usuário excluído, então os quatro recebem a mesma mensagem e o mesmo status.
- **CA-F1-02.3** Dado um usuário válido sem papel no sistema pedido, quando autentica, então falha com a mesma mensagem genérica e o motivo `NO_SYSTEM_ACCESS` é registrado.
- **CA-F1-02.4** Dado `JOHN.DOE` cadastrado como `john.doe`, quando autentica com `John.Doe`, então tem sucesso.
- **CA-F1-02.5** Dado um usuário com `authentication_type = LDAP`, então o provedor de banco não o autentica.

## Questões em aberto

- RN-F1-02.5 (exigir papel no sistema) já na Fase 1? Recomendado: sim, os dados já existem e evita que qualquer usuário entre em qualquer sistema.
- Aceitar e-mail como login, além do username?
