# F0-02 — Armazenamento seguro de senha

- **Fase:** 0
- **Status:** Proposto
- **Depende de:** F0-01

## Contexto

`users` tem colunas `password` e `salt` separadas, o que sugere um hash próprio. O seed atual grava a senha em texto puro. O sentry-auth precisa de um formato de hash padrão antes de validar credenciais (F1-02).

## Regras

- **RN-F0-02.1** Senhas DEVEM ser armazenadas com Argon2id, no formato PHC com salt e parâmetros embutidos (ex.: `$argon2id$v=19$m=19456,t=2,p=1$...`).
- **RN-F0-02.2** Parâmetros mínimos: memória 19 MiB, 2 iterações, paralelismo 1 (mínimo recomendado pela OWASP), configuráveis por propriedade.
- **RN-F0-02.3** O valor armazenado DEVE ter prefixo de algoritmo (padrão `DelegatingPasswordEncoder`, ex.: `{argon2}`), para permitir trocar o algoritmo no futuro sem migração em massa.
- **RN-F0-02.4** Quando os parâmetros configurados forem mais fortes que os do hash armazenado, o sistema DEVE refazer o hash no próximo login com sucesso.
- **RN-F0-02.5** A coluna `salt` DEVE ser removida (ou deixar de ser usada e ficar marcada para remoção).
- **RN-F0-02.6** Senhas NUNCA DEVEM aparecer em logs, respostas de API ou mensagens de erro.
- **RN-F0-02.7** Os endpoints de criação e atualização de usuário do `sentry-backend` DEVEM gravar a senha já com hash; a resposta NÃO DEVE devolver a senha nem o hash.

## Critérios de aceite

- **CA-F0-02.1** Dado um usuário criado pela API com senha `S3nha!forte`, quando se lê a linha em `users`, então `password` começa com `{argon2}$argon2id$` e não contém a senha em claro.
- **CA-F0-02.2** Dado um hash Argon2id armazenado, quando a senha correta é verificada, então o resultado é verdadeiro; com qualquer outra senha, falso.
- **CA-F0-02.3** Dado um hash com memória 8 MiB e configuração atual de 19 MiB, quando o usuário faz login com sucesso, então o hash armazenado passa a usar 19 MiB.
- **CA-F0-02.4** Dada qualquer consulta de usuário no `sentry-backend`, então a resposta não contém os campos `password` nem `salt`.

## Notas técnicas

- `Argon2PasswordEncoder` do Spring Security exige Bouncy Castle no classpath.
- O encoder fica no `sentry-api`, compartilhado por `sentry-auth` e `sentry-backend`.

## Questões em aberto

- Há regras de política de senha (tamanho mínimo, complexidade) para a Fase 1? Proposta: mínimo de 8 caracteres e bloqueio das 10 mil senhas mais comuns, alinhado ao NIST 800-63B.
