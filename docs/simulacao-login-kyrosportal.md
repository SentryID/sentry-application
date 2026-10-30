# Simulação de login: juliang no KYROSPORTAL

Este roteiro cria no banco local um usuário, um sistema, um grupo e uma permissão e mostra como autenticar esse usuário
por HTTP. Todos os dados são fictícios e servem só para ambiente local.

| O quê | Valor |
| --- | --- |
| Usuário | `juliang`, senha `Kyros@123`, tipo `DATABASE` |
| Sistema (`client_id`) | `KYROSPORTAL`, confidencial e confiável, segredo `kyros-portal-segredo-local` |
| Organização | `KYROS` |
| Grupo (papel) | `PORTAL.COLABORADOR` |
| Permissão (ação) | `PORTAL.PONTO` |

Os arquivos ficam em [`docs/exemplos/kyrosportal/`](exemplos/kyrosportal/):

- [`kyrosportal.sql`](exemplos/kyrosportal/kyrosportal.sql): os inserts, que podem ser rodados mais de uma vez;
- [`kyrosportal.http`](exemplos/kyrosportal/kyrosportal.http): as requisições, para o HTTP Client do IntelliJ ou a
  extensão REST Client do VS Code.

## Como o Sentry decide o login

Para o `juliang` entrar no `KYROSPORTAL`, o `UserAuthenticationProvider` confere, nesta ordem:

1. o usuário existe (username sem diferenciar maiúsculas), está ativo e não foi excluído;
2. a senha confere com o hash Argon2id gravado, o que é feito pelo `DatabaseCredentialVerifier`, porque o tipo é `DATABASE`;
3. o sistema e a organização estão ativos;
4. o usuário tem **pelo menos um papel ativo no sistema**. É o vínculo em `role_users` com o `PORTAL.COLABORADOR`
   que libera o login.

No login direto, o sistema também precisa se autenticar com o próprio segredo e ser **confiável** (`is_trusted`).

A permissão `PORTAL.PONTO` **não influencia o login e não vai no token**: o access token não leva papéis nem ações
(RN-F1-05.8). Ela fica gravada para ser consultada no banco (veja a conferência no fim do SQL).

```mermaid
erDiagram
    organizations ||--o{ systems : "KYROS"
    systems ||--o{ roles : "KYROSPORTAL"
    systems ||--o{ actions : "KYROSPORTAL"
    roles ||--o{ role_actions : "PORTAL.COLABORADOR"
    actions ||--o{ role_actions : "PORTAL.PONTO"
    roles ||--o{ role_users : ""
    users ||--o{ role_users : "juliang"
```

## Passo 1 — Subir o banco e as aplicações

```bash
docker compose up -d postgres
```

Suba o `sentry-auth` (porta `8081`), pela IDE ou por `docker compose up -d sentry-auth`. O Flyway cria as tabelas
na primeira subida, e o SQL do próximo passo depende delas.

## Passo 2 — Inserir os dados

Com o `psql` do container:

```bash
docker compose exec -T postgres psql -U sentry -d sentry < docs/exemplos/kyrosportal/kyrosportal.sql
```

No PowerShell, troque o `<` por um pipe:

```powershell
Get-Content docs/exemplos/kyrosportal/kyrosportal.sql | docker compose exec -T postgres psql -U sentry -d sentry
```

Também dá para colar o arquivo no DBeaver ou no pgAdmin (banco `sentry`, usuário `sentry`, senha `sentry-local`,
porta `5432`).

No fim, a consulta de conferência deve trazer uma linha:

```
 username |                 sub                  |  client_id  |    system_role     |  cd_action
----------+--------------------------------------+-------------+--------------------+--------------
 juliang  | 6b1f...                              | KYROSPORTAL | PORTAL.COLABORADOR | PORTAL.PONTO
```

O `sub` é o `uuid` do usuário, que aparece como `sub` nos tokens.

### Sobre os hashes

Senha e segredo **nunca** ficam em claro no banco. Os valores do SQL foram gerados com os mesmos parâmetros do
`PasswordService` (`{argon2}$argon2id$v=19$m=19456,t=2,p=1$...`). Para trocar a senha, gere um hash novo. Uma forma é
criar o usuário pela API, que já grava o hash:

```bash
curl -X POST http://localhost:8080/users -H 'Content-Type: application/json' \
  -d '{"name": "Julian G", "authenticationType": "DATABASE", "username": "juliang",
       "password": "Kyros@123", "email": "juliang@kyros.local", "isActive": true}'
```

Nesse caso, pule o item 6 do SQL (o insert em `users`) e rode o resto.

## Passo 3 — Autenticar

### Login direto (uma requisição)

O sistema se autentica por HTTP Basic (`KYROSPORTAL:kyros-portal-segredo-local`) e manda as credenciais do usuário
no corpo:

```http
POST http://localhost:8081/api/v1/auth/login
Authorization: Basic S1lST1NQT1JUQUw6a3lyb3MtcG9ydGFsLXNlZ3JlZG8tbG9jYWw=
Content-Type: application/json

{"username": "juliang", "password": "Kyros@123", "scope": "openid profile email offline_access"}
```

O mesmo com `curl`:

```bash
curl -i -X POST http://localhost:8081/api/v1/auth/login \
  -u 'KYROSPORTAL:kyros-portal-segredo-local' \
  -H 'Content-Type: application/json' \
  -d '{"username": "juliang", "password": "Kyros@123", "scope": "openid profile email offline_access"}'
```

Resposta esperada (`200`):

```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 900,
  "id_token": "eyJ...",
  "refresh_token": "...",
  "scope": "openid profile email offline_access"
}
```

Abra o `access_token` em [jwt.io](https://jwt.io) ou decodifique o payload em Base64. Ele deve trazer `sub` igual ao
`uuid` do `juliang`, `client_id` = `KYROSPORTAL` e `org` igual ao `uuid` da organização `KYROS`. Com o escopo
`profile`, o `id_token` traz `preferred_username` = `juliang`.

O login direto só aceita HTTP em `localhost`, `127.0.0.1` ou `[::1]`. Fora disso, use HTTPS.

### Pela tela de login (PKCE)

O sistema também tem a URI `http://localhost:4200/callback`, então o fluxo com a tela de login funciona. O passo a
passo com `code_verifier` e `code_challenge` está na [jornada de autenticação](jornada-de-autenticacao.md); use
`client_id=KYROSPORTAL` e `redirect_uri=http://localhost:4200/callback`.

## Passo 4 — Conferir o registro da tentativa

Toda tentativa, com sucesso ou não, vira uma linha em `access_logs` com o motivo real:

```sql
SELECT a.attempted_at, a.attempted_username, a.attempt_result, a.channel, a.ip_host
FROM access_logs a JOIN systems s ON s.id_system = a.id_system
WHERE s.external_code = 'KYROSPORTAL'
ORDER BY a.id_access_log DESC
LIMIT 10;
```

## Outros cenários

Cada caso abaixo muda um dado e mostra a resposta e o motivo que fica em `access_logs`. Para o usuário, a mensagem é
sempre a mesma (RN-F1-02.3).

| Cenário | Como provocar | Resposta | `attempt_result` |
| --- | --- | --- | --- |
| Senha errada | `"password": "senha-errada"` | `401 invalid_grant`, "Usuário ou senha inválidos" | `INVALID_CREDENTIALS` |
| Usuário inexistente | `"username": "fulano"` | `401 invalid_grant` | `USER_NOT_FOUND` |
| Usuário inativo | `UPDATE users SET is_active = FALSE WHERE username = 'juliang';` | `401 invalid_grant` | `USER_INACTIVE` |
| Sem o grupo | `UPDATE role_users SET is_active = FALSE WHERE id_user = (SELECT id_user FROM users WHERE username = 'juliang');` | `401 invalid_grant` | `NO_SYSTEM_ACCESS` |
| Segredo do sistema errado | `-u 'KYROSPORTAL:outro'` | `401 invalid_client` | sem registro: o usuário nem chega a ser conferido |
| Sistema não confiável | `UPDATE systems SET is_trusted = FALSE WHERE external_code = 'KYROSPORTAL';` | `403 unauthorized_client` | sem registro |
| Escopo não cadastrado | `"scope": "openid admin"` | `400 invalid_scope` | sem registro |

Depois de cada cenário, desfaça a mudança (troque `FALSE` por `TRUE`).

## Limpar

```sql
BEGIN;
DELETE FROM access_logs WHERE id_system = (SELECT id_system FROM systems WHERE external_code = 'KYROSPORTAL');
DELETE FROM oauth2_authorization WHERE registered_client_id = 'KYROSPORTAL';
DELETE FROM role_users WHERE id_user = (SELECT id_user FROM users WHERE username = 'juliang');
DELETE FROM role_actions WHERE cd_action = 'PORTAL.PONTO';
DELETE FROM roles WHERE id_system = (SELECT id_system FROM systems WHERE external_code = 'KYROSPORTAL');
DELETE FROM actions WHERE cd_action = 'PORTAL.PONTO';
DELETE FROM users WHERE username = 'juliang';
DELETE FROM systems WHERE external_code = 'KYROSPORTAL';
DELETE FROM organizations WHERE external_code = 'KYROS';
COMMIT;
```

O `system_redirect_uris` é apagado junto com o sistema (`ON DELETE CASCADE`).
