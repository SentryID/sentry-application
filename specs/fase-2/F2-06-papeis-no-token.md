# F2-06 — Papéis do usuário no token do sistema

- **Fase:** 2
- **Status:** Proposto
- **Depende de:** F1-05, F1-08

## Contexto

Na Fase 1 o token só diz quem é o usuário (RN-F1-05.8): cada sistema descobre por conta própria o que ele pode fazer, embora o Sentry já guarde os papéis (`roles`, `role_users`) e as ações (`actions`, `role_actions`) de cada sistema. A Fase 2 entrega os papéis do usuário naquele sistema; as ações e decisões de autorização mais finas ficam para um serviço próprio de autorização em fase futura.

## Regras

- **RN-F2-06.1** Um novo escopo `roles` DEVE ser aceito pelo cadastro de sistemas (RN-F1-01.6) e pela descoberta (`scopes_supported`).
- **RN-F2-06.2** Com o escopo `roles` concedido, o access token DEVE ter o claim `roles`: a lista de `system_role` dos papéis ativos do usuário naquele sistema (papéis de outros sistemas nunca entram).
- **RN-F2-06.3** O `/userinfo` DEVE devolver o mesmo claim `roles`, lido do banco na hora (como em RN-F1-08.3).
- **RN-F2-06.4** O ID token NÃO DEVE levar `roles`: ele descreve a autenticação, não a autorização.
- **RN-F2-06.5** O claim DEVE ter no máximo 50 papéis; acima disso, o token leva `roles_overflow: true` e o sistema deve consultar o `/userinfo`.
- **RN-F2-06.6** Ações (`role_actions`) NÃO DEVEM entrar no token na Fase 2.

## Critérios de aceite

- **CA-F2-06.1** Dado um usuário com os papéis `SYS001.VIEWER` e `SYS001.EDITOR` no `SYS001` e `SYS002.ADMIN` no `SYS002`, quando faz login no `SYS001` com o escopo `roles`, então o access token tem `roles = ["SYS001.EDITOR", "SYS001.VIEWER"]`.
- **CA-F2-06.2** Dado um login sem o escopo `roles`, então o access token não tem o claim.
- **CA-F2-06.3** Dado um papel desativado depois do login, então o `/userinfo` já não o devolve, e o próximo refresh emite um token sem ele.
- **CA-F2-06.4** Dado o ID token do mesmo login, então ele não tem `roles`.

## Notas técnicas

- O `TokenClaimsCustomizer` (F1-08) já consulta o papel ativo no sistema a cada emissão; a lista sai da mesma consulta.

## Questões em aberto

- Usar o `system_role` ou o `external_role` no claim? Recomendado: `system_role`, que é o nome que o próprio sistema cadastrou; o `external_role` fica para integrações com diretórios externos.
- Introspecção de token (RFC 7662) para sistemas que preferem consultar a ler o JWT? Recomendado: não na Fase 2; o `/userinfo` cobre a consulta atualizada.
