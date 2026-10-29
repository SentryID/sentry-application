# Spec do sentry-auth — Fases 0 a 2

Especificação no estilo Spec Driven Development: cada requisito vive no seu próprio arquivo, é validado antes de virar código, e cada critério de aceite vira pelo menos um teste automatizado.

**Escopo deste pacote:** a autenticação de um usuário a pedido de um sistema externo (Fases 0 e 1) e a proteção das contas e da administração (Fase 2: política e recuperação de senha, MFA, limitação de tentativas, `sentry-backend` protegido, papéis no token). Ações e o serviço `sentry-authorization`, console, SDKs, LDAP e multi-tenant ficam fora.

## Como ler e validar

1. Leia [00-escopo.md](00-escopo.md): atores, glossário, convenções e decisões já tomadas.
2. Valide cada arquivo de requisito. Mude o campo **Status** de `Proposto` para `Aprovado` ou `Ajustar` e anote o que mudar em **Questões em aberto**.
3. O [ROADMAP.md](ROADMAP.md) ordena os requisitos, quebra em tarefas e define o critério de saída de cada fase.

## Índice

| ID | Requisito | Fase | Depende de | Status |
| --- | --- | --- | --- | --- |
| [F0-01](fase-0/F0-01-migracao-banco.md) | Migração do banco executável do zero | 0 | — | Proposto |
| [F0-02](fase-0/F0-02-hash-de-senha.md) | Armazenamento seguro de senha | 0 | F0-01 | Proposto |
| [F0-03](fase-0/F0-03-seed-e-bootstrap.md) | Seed sem credenciais e bootstrap do administrador | 0 | F0-01, F0-02 | Proposto |
| [F0-04](fase-0/F0-04-ambiente-local.md) | Ambiente local com Docker Compose | 0 | F0-01 | Proposto |
| [F0-05](fase-0/F0-05-integracao-continua.md) | Integração contínua | 0 | F0-04 | Proposto |
| [F1-01](fase-1/F1-01-sistema-como-cliente-oauth.md) | Sistema externo registrado como cliente OAuth | 1 | F0-01 | Proposto |
| [F1-02](fase-1/F1-02-validacao-de-credenciais.md) | Validação de credenciais do usuário | 1 | F0-02 | Proposto |
| [F1-03](fase-1/F1-03-fluxo-authorization-code-pkce.md) | Fluxo Authorization Code + PKCE | 1 | F1-01, F1-02, F1-04 | Proposto |
| [F1-04](fase-1/F1-04-tela-de-login.md) | Tela de login hospedada | 1 | F1-02 | Proposto |
| [F1-05](fase-1/F1-05-emissao-de-tokens.md) | Emissão de access token e ID token | 1 | F1-03, F1-07 | Proposto |
| [F1-06](fase-1/F1-06-refresh-token.md) | Refresh token com rotação | 1 | F1-05 | Proposto |
| [F1-07](fase-1/F1-07-descoberta-oidc-e-jwks.md) | Descoberta OIDC e JWKS | 1 | F0-04 | Proposto |
| [F1-08](fase-1/F1-08-userinfo.md) | Endpoint UserInfo | 1 | F1-05 | Proposto |
| [F1-09](fase-1/F1-09-logout-e-sessao.md) | Sessão e logout | 1 | F1-03 | Proposto |
| [F1-10](fase-1/F1-10-registro-de-tentativas.md) | Registro de tentativas de autenticação | 1 | F1-02 | Proposto |
| [F1-11](fase-1/F1-11-login-direto-sistema-confiavel.md) | Login direto por API para sistemas confiáveis | 1 | F1-01, F1-02, F1-05 | Proposto |
| [F2-01](fase-2/F2-01-protecao-do-backend.md) | Proteção da API de cadastro (sentry-backend) | 2 | F1-05, F1-08 | Proposto |
| [F2-02](fase-2/F2-02-politica-e-troca-de-senha.md) | Política de senha e troca obrigatória | 2 | F0-02, F0-03, F1-04 | Proposto |
| [F2-03](fase-2/F2-03-recuperacao-de-senha.md) | Recuperação de senha por e-mail | 2 | F2-02, F2-05 | Proposto |
| [F2-04](fase-2/F2-04-mfa-totp.md) | Autenticação em dois fatores (TOTP) | 2 | F1-04, F1-05, F2-02 | Proposto |
| [F2-05](fase-2/F2-05-limitacao-de-tentativas.md) | Limitação de tentativas e bloqueio temporário | 2 | F1-10 | Proposto |
| [F2-06](fase-2/F2-06-papeis-no-token.md) | Papéis do usuário no token do sistema | 2 | F1-05, F1-08 | Proposto |
