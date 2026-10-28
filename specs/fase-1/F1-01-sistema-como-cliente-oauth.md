# F1-01 — Sistema externo registrado como cliente OAuth

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F0-01

## Contexto

A tabela `systems` já representa o sistema externo (organização, `external_code`, `url`, `scopes`), mas não guarda o que o OAuth exige: segredo, URIs de redirecionamento, tipo de cliente e fluxos permitidos.

## Regras

- **RN-F1-01.1** Cada sistema externo DEVE ter um `client_id` único e imutável. Proposta: usar o `external_code` existente.
- **RN-F1-01.2** Cada sistema DEVE ter um tipo: `CONFIDENTIAL` (tem backend e segredo) ou `PUBLIC` (SPA/mobile, sem segredo).
- **RN-F1-01.3** Sistemas `CONFIDENTIAL` DEVEM ter `client_secret` armazenado com hash (mesmo encoder de F0-02). O segredo em claro DEVE ser exibido uma única vez, na criação ou na rotação.
- **RN-F1-01.4** Cada sistema DEVE ter uma lista de `redirect_uris` e uma de `post_logout_redirect_uris`, comparadas por igualdade exata de string (sem curingas).
- **RN-F1-01.5** URIs de redirecionamento DEVEM usar `https`, exceto `http://localhost` e `http://127.0.0.1` em qualquer porta.
- **RN-F1-01.6** Cada sistema DEVE ter a lista de escopos permitidos. Escopos aceitos na Fase 1: `openid`, `profile`, `email`, `offline_access`.
- **RN-F1-01.7** Cada sistema DEVE ter a flag `trusted` (padrão `false`), que libera o login direto de F1-11.
- **RN-F1-01.8** Só sistemas com `is_active = true` e `is_deleted = false`, de organização ativa, PODEM iniciar autenticação.
- **RN-F1-01.9** O `sentry-backend` DEVE permitir criar, editar e desativar esses dados, e rotacionar o segredo.

## Critérios de aceite

- **CA-F1-01.1** Dado um sistema criado como `CONFIDENTIAL`, então a resposta da criação traz o `client_secret` uma vez e o banco guarda só o hash.
- **CA-F1-01.2** Dada uma `redirect_uri` `http://app.exemplo.com/cb`, quando se tenta salvá-la, então a API recusa com mensagem em português.
- **CA-F1-01.3** Dado um sistema desativado, quando ele inicia o fluxo de F1-03, então recebe `invalid_client` / `unauthorized_client`.
- **CA-F1-01.4** Dada a rotação do segredo, então o segredo antigo deixa de autenticar imediatamente.

## Modelo de dados proposto (migração V2)

| Tabela | Coluna | Tipo |
| --- | --- | --- |
| `systems` | `client_type` | `VARCHAR(20) NOT NULL` (`PUBLIC`, `CONFIDENTIAL`) |
| `systems` | `client_secret_hash` | `VARCHAR(255)` |
| `systems` | `is_trusted` | `BOOLEAN NOT NULL DEFAULT FALSE` |
| `system_redirect_uris` | `id_system`, `uri`, `uri_type` (`LOGIN`, `LOGOUT`) | nova tabela |

A coluna `scopes` passa a ser uma lista separada por espaço, validada contra os escopos aceitos.

## Notas técnicas

- Implementar um `RegisteredClientRepository` do Spring Authorization Server que lê de `systems`, em vez de usar a tabela `oauth2_registered_client` padrão.

## Questões em aberto

- `client_id` = `external_code` (legível, ex.: `SYS001`) ou o `uuid` do sistema? Recomendado: `external_code`.
