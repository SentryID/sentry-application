# F0-03 — Seed sem credenciais e bootstrap do administrador

- **Fase:** 0
- **Status:** Proposto
- **Depende de:** F0-01, F0-02

## Contexto

O `seed-template.sql`, num repositório público, contém um usuário real com senha em texto puro. O Sentry precisa de um jeito seguro de criar o primeiro administrador e o primeiro sistema numa instalação nova.

## Regras

- **RN-F0-03.1** Nenhum arquivo versionado DEVE conter senha, segredo de cliente ou dado pessoal real.
- **RN-F0-03.2** A senha exposta no histórico do repositório DEVE ser trocada em qualquer lugar onde seja usada.
- **RN-F0-03.3** Na primeira subida com banco vazio, o sentry-auth DEVE criar a organização da plataforma, o sistema `SENTRY` e um usuário administrador a partir de variáveis de ambiente (`SENTRY_ADMIN_USERNAME`, `SENTRY_ADMIN_EMAIL`, `SENTRY_ADMIN_PASSWORD`).
- **RN-F0-03.4** Se `SENTRY_ADMIN_PASSWORD` não for informada, o sistema DEVE gerar uma senha aleatória de pelo menos 20 caracteres, mostrá-la uma única vez no log de inicialização e exigir troca no primeiro login (a troca entra na Fase 2; até lá, apenas o aviso).
- **RN-F0-03.5** O bootstrap DEVE ser idempotente: se a organização da plataforma já existe, nada é criado.
- **RN-F0-03.6** Dados de exemplo para desenvolvimento PODEM existir, com nomes fictícios, em um perfil `dev` que nunca roda por padrão.

## Critérios de aceite

- **CA-F0-03.1** Dado um banco vazio e as três variáveis definidas, quando o serviço sobe, então existe um usuário com aquele username e senha com hash Argon2id.
- **CA-F0-03.2** Dado um banco já inicializado, quando o serviço sobe de novo, então nenhum registro novo é criado.
- **CA-F0-03.3** Dado um banco vazio sem `SENTRY_ADMIN_PASSWORD`, quando o serviço sobe, então o log contém a senha gerada uma vez, e ela permite login.
- **CA-F0-03.4** Dado o repositório, quando se busca por senhas e e-mails reais nos arquivos versionados, então nada é encontrado (verificação com gitleaks na CI, F0-05).

## Questões em aberto

- Reescrever o histórico do Git para remover a senha, ou apenas trocá-la? Recomendado: apenas trocar, já que reescrever histórico público não garante remoção.
