# F1-10 — Registro de tentativas de autenticação

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-02

## Contexto

A tabela `access_logs` existe com usuário, sistema, data, resultado e IP. A Fase 1 passa a preenchê-la; o bloqueio por tentativas (que vai ler esses registros) fica na Fase 2.

## Regras

- **RN-F1-10.1** Toda tentativa de autenticação, pela tela (F1-04) ou pelo login direto (F1-11), DEVE gerar um registro em `access_logs`, com sucesso ou falha.
- **RN-F1-10.2** `attempt_result` DEVE ser um destes valores: `SUCCESS`, `INVALID_CREDENTIALS`, `USER_NOT_FOUND`, `USER_INACTIVE`, `NO_SYSTEM_ACCESS`, `SYSTEM_INACTIVE`.
- **RN-F1-10.3** Quando o usuário não existe, `id_user` fica nulo e o username digitado DEVE ser guardado em uma nova coluna `attempted_username`.
- **RN-F1-10.4** O IP DEVE considerar `X-Forwarded-For` apenas de proxies configurados como confiáveis.
- **RN-F1-10.5** O registro DEVE incluir o canal (`PKCE` ou `DIRECT`) e o `User-Agent`.
- **RN-F1-10.6** Uma falha ao gravar o registro NÃO DEVE impedir o login, mas DEVE gerar log de erro.
- **RN-F1-10.7** Emissão de token, refresh e logout DEVEM ser registrados no log estruturado da aplicação (JSON), sem tokens nem senhas.

## Critérios de aceite

- **CA-F1-10.1** Dada uma senha errada para `john.doe` no sistema `SYS001`, então existe um registro com `INVALID_CREDENTIALS`, o usuário, o sistema e o IP.
- **CA-F1-10.2** Dado um username inexistente `fulano`, então existe um registro `USER_NOT_FOUND` com `attempted_username = fulano` e `id_user` nulo.
- **CA-F1-10.3** Dado o banco de log indisponível só para a escrita do registro, então o login ainda funciona.
- **CA-F1-10.4** Dado qualquer log da aplicação, então nenhuma linha contém senha, access token ou refresh token.

## Modelo de dados (migração V2)

`access_logs` ganha `attempted_username VARCHAR(255)`, `channel VARCHAR(20)` e `user_agent VARCHAR(512)`.
