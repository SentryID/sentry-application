# F1-04 — Tela de login hospedada

- **Fase:** 1
- **Status:** Proposto
- **Depende de:** F1-02

## Contexto

No fluxo de F1-03 a senha é digitada numa tela servida pelo próprio sentry-auth, nunca no sistema externo.

## Regras

- **RN-F1-04.1** O sentry-auth DEVE servir `GET /login` (formulário) e `POST /login` (envio), renderizados no servidor, sem depender do console.
- **RN-F1-04.2** A tela DEVE mostrar o nome do sistema externo que pediu o login (`systems.description`).
- **RN-F1-04.3** O formulário DEVE ter proteção CSRF.
- **RN-F1-04.4** Em falha, a tela DEVE mostrar a mensagem genérica de F1-02 e manter o username preenchido, nunca a senha.
- **RN-F1-04.5** A tela DEVE estar em português, com textos externalizados para tradução futura.
- **RN-F1-04.6** As respostas DEVEM ter `Content-Security-Policy` restritiva, `X-Frame-Options: DENY` e `Cache-Control: no-store`.
- **RN-F1-04.7** A tela DEVE ser utilizável em celular e atender WCAG 2.1 AA nos campos e mensagens.
- **RN-F1-04.8** Links "Esqueci minha senha" e "Criar conta" NÃO DEVEM aparecer na Fase 1.

## Critérios de aceite

- **CA-F1-04.1** Dado um fluxo de F1-03 iniciado pelo sistema `SYS001`, quando a tela abre, então mostra a descrição desse sistema.
- **CA-F1-04.2** Dado um `POST /login` sem token CSRF, então a resposta é `403`.
- **CA-F1-04.3** Dada uma senha errada, então a tela volta com "Usuário ou senha inválidos", username preenchido e campo de senha vazio.
- **CA-F1-04.4** Dada a página, então ela não pode ser carregada dentro de um iframe de outra origem.

## Notas técnicas

- Thymeleaf no próprio `sentry-auth`, com o `formLogin` do Spring Security apontando para o `DatabaseAuthenticationProvider`.

## Questões em aberto

- Identidade visual da tela (logo, cores) do Sentry ou da organização do sistema? Recomendado: do Sentry na Fase 1.
