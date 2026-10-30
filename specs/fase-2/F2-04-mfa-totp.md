# F2-04 — Autenticação em dois fatores (TOTP)

- **Fase:** 2
- **Status:** Proposto
- **Depende de:** F1-04, F1-05, F2-02

## Contexto

A tabela `mfa_tokens` da V1 (código, token, username, uso único) desenha um código enviado por canal externo, mas nada a usa. A Fase 2 adota TOTP (RFC 6238, aplicativos como Google Authenticator, Microsoft Authenticator, 1Password), que não depende de e-mail ou SMS, com códigos de recuperação para quem perder o aparelho.

## Regras

O `sentry-auth` não tem telas: a tela de login e as demais são do front-end (React), servido na mesma origem, que chama a API de sessão (`/api/v1/session/**`, cookie `SESSION` e CSRF por `X-XSRF-TOKEN`, ver `../../docs/integracao_frontend.md`).

- **RN-F2-04.1** O MFA DEVE ser opcional por usuário. Um sistema PODE exigi-lo para seus usuários (`systems.require_mfa`); o administrador da plataforma (`SENTRY.ADMIN`) DEVE sempre ter MFA.
- **RN-F2-04.2** O cadastro DEVE ser feito pela API de sessão, com sessão SSO ativa e confirmação da senha: `POST /api/v1/session/mfa/setup` devolve a chave em texto e o URI `otpauth://totp/Sentry:<username>?secret=...&issuer=Sentry` (o front-end desenha o QR code), e `POST /api/v1/session/mfa/confirm` só ativa o MFA após um código válido.
- **RN-F2-04.3** O segredo TOTP DEVE ter 160 bits e ser gravado cifrado (AES-256-GCM) com a chave de `SENTRY_MFA_ENCRYPTION_KEY`; sem a chave, o MFA fica indisponível e o serviço avisa no log.
- **RN-F2-04.4** Códigos: 6 dígitos, passo de 30 s, SHA-1, aceitando uma janela de ±1 passo. Um código já usado NÃO DEVE ser aceito de novo (guarda-se o último passo usado).
- **RN-F2-04.5** Na ativação DEVEM ser gerados 10 códigos de recuperação de uso único, mostrados uma única vez e gravados só como hash (Argon2id). Usar um deles conta como segundo fator.
- **RN-F2-04.6** No login pela API de sessão, depois da senha, um usuário com MFA ativo DEVE receber `200 {"mfaRequired": true}` sem `redirectUrl`, e um usuário sem MFA num sistema que o exige, `{"mfaSetupRequired": true}`. O código vai em `POST /api/v1/session/mfa` (TOTP ou código de recuperação), que devolve o `redirectUrl`. A sessão SSO só é considerada autenticada depois do segundo fator.
- **RN-F2-04.7** O ID token DEVE ter `amr`: `["pwd"]` só com senha, `["pwd","otp"]` com TOTP e `["pwd","otp"]` também com código de recuperação. O `auth_time` é o instante do segundo fator.
- **RN-F2-04.8** Um sistema PODE pedir MFA no fluxo com `acr_values=urn:sentry:mfa`; se a sessão SSO ativa não tiver segundo fator, ele é pedido.
- **RN-F2-04.9** No login direto (F1-11), um usuário com MFA DEVE informar `otp` no corpo; sem ele, a resposta é `401 {"error":"mfa_required"}`; com código errado, `invalid_grant` com a mensagem genérica.
- **RN-F2-04.10** Códigos TOTP e de recuperação errados DEVEM contar para a limitação de F2-05 e ser registrados em `access_logs` (`MFA_INVALID`, `MFA_SUCCESS`, `MFA_RECOVERY_USED`).
- **RN-F2-04.11** O administrador DEVE poder desativar o MFA de um usuário pelo `sentry-backend` (recuperação de conta); o evento vai para o log de auditoria.

## Critérios de aceite

- **CA-F2-04.1** Dado um usuário com MFA, quando acerta a senha, então recebe `mfaRequired`, e o `/oauth2/authorize` ainda não emite o código.
- **CA-F2-04.2** Dado um código TOTP válido, então o fluxo continua e o ID token tem `amr` com `otp`.
- **CA-F2-04.3** Dado o mesmo código TOTP usado duas vezes no mesmo passo, então a segunda tentativa é recusada.
- **CA-F2-04.4** Dado um código de recuperação, então funciona uma vez e na segunda é recusado.
- **CA-F2-04.5** Dado o banco, então o segredo TOTP não aparece em claro e os códigos de recuperação estão em hash.
- **CA-F2-04.6** Dado um sistema com `require_mfa` e um usuário sem MFA, então o login responde `mfaSetupRequired` e o código só é emitido depois do cadastro.
- **CA-F2-04.7** Dado o login direto sem `otp` para usuário com MFA, então a resposta é `401 mfa_required`.

## Notas técnicas

- Migração V5: tabela `user_mfa` (`id_user`, `secret_ciphertext`, `last_used_step`, `enabled_at`) e `mfa_recovery_codes` (`id_user`, `code_hash`, `used_at`); coluna `systems.require_mfa`.
- O Spring Security 7 tem suporte a múltiplos fatores (`FactorGrantedAuthority`, já usado no login da Fase 1 para o `auth_time`): o fator TOTP entra como uma segunda autoridade.
- O QR code é desenhado pelo front-end a partir do URI `otpauth://`; o `sentry-auth` não gera imagens.

## Questões em aberto

- O que fazer com a tabela `mfa_tokens` da V1? Recomendado: remover na V5, porque o desenho (código enviado por canal externo) não é o adotado e ela nunca foi usada.
- Oferecer também passkeys (WebAuthn), que o Spring Security 7 suporta? Recomendado: não na Fase 2; entra numa fase própria depois do TOTP.
- Lembrar o aparelho ("não pedir de novo por 30 dias")? Recomendado: não na Fase 2; a sessão SSO de 12 h já reduz os pedidos.
