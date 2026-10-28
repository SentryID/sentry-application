# F1-07 — Descoberta OIDC e JWKS

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F0-04

## Regras

- **RN-F1-07.1** `GET /.well-known/openid-configuration` DEVE publicar issuer, endpoints (`authorization`, `token`, `userinfo`, `jwks`, `revocation`, `end_session`), escopos, `response_types_supported=["code"]`, `grant_types_supported`, `code_challenge_methods_supported=["S256"]` e algoritmos de assinatura.
- **RN-F1-07.2** `GET /oauth2/jwks` DEVE publicar as chaves públicas ativas em formato JWK, cada uma com `kid`.
- **RN-F1-07.3** A chave privada de assinatura DEVE vir de configuração externa (arquivo PEM montado ou variável de ambiente), nunca gerada a cada subida em produção.
- **RN-F1-07.4** Em modo de desenvolvimento, sem chave configurada, o sistema PODE gerar uma chave temporária e DEVE avisar no log.
- **RN-F1-07.5** Rotação: o JWKS DEVE suportar duas chaves ao mesmo tempo (atual e anterior), para que tokens emitidos antes da troca continuem válidos até expirar.
- **RN-F1-07.6** As duas respostas DEVEM ser públicas e cacheáveis (`Cache-Control: max-age=300` ou similar).

## Critérios de aceite

- **CA-F1-07.1** Dado o serviço no ar, quando se lê a descoberta, então o `issuer` é igual a `SENTRY_ISSUER_URL` e todos os endpoints apontam para ele.
- **CA-F1-07.2** Dado um token emitido, então seu `kid` existe no JWKS.
- **CA-F1-07.3** Dada a troca da chave atual por uma nova, mantendo a anterior configurada, então tokens assinados com a anterior continuam validando até expirar.
- **CA-F1-07.4** Dado o perfil de produção sem chave configurada, então o serviço não sobe e informa o motivo.
