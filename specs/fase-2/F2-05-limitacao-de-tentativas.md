# F2-05 — Limitação de tentativas e bloqueio temporário

- **Fase:** 2
- **Status:** Proposto
- **Depende de:** F1-10

## Contexto

A Fase 1 registra toda tentativa em `access_logs` (F1-10), mas não limita nada: é possível testar senhas sem parar, pela API de sessão (tela do front-end) ou pelo login direto. A Fase 2 lê esses registros para frear ataques de força bruta (contra uma conta) e de credential stuffing (de um IP contra muitas contas), sem permitir que um atacante bloqueie a conta de alguém por muito tempo.

## Regras

- **RN-F2-05.1** Por conta: 5 falhas consecutivas (`INVALID_CREDENTIALS`, `MFA_INVALID`) em 15 minutos DEVEM bloquear novas tentativas daquela conta por 15 minutos. Cada novo bloqueio no mesmo dia dobra o tempo, até 4 horas. Um login com sucesso zera a contagem.
- **RN-F2-05.2** Por IP: 20 falhas em 5 minutos, de qualquer conta, DEVEM bloquear aquele IP por 15 minutos.
- **RN-F2-05.3** Durante o bloqueio de conta a senha NÃO DEVE ser verificada, e a resposta DEVE ser a mesma mensagem genérica de F1-02, para não revelar que a conta existe nem que está bloqueada. O motivo real vai para `access_logs` como `ACCOUNT_LOCKED`.
- **RN-F2-05.4** Durante o bloqueio de IP, a API de sessão e o login direto DEVEM responder `429` com `Retry-After` e a mensagem "Muitas tentativas. Tente de novo em alguns minutos" (Problem Details na API de sessão, erro OAuth no login direto). O motivo vai para `access_logs` como `IP_THROTTLED`.
- **RN-F2-05.5** Os limites e tempos DEVEM ser configuráveis (`SENTRY_LOCKOUT_*`), e o IP DEVE ser o mesmo resolvido em F1-10 (proxies confiáveis).
- **RN-F2-05.6** Os pedidos de recuperação de senha (F2-03) DEVEM ter limite próprio: 3 por endereço informado por hora e 10 por IP por hora, sempre com a mesma resposta.
- **RN-F2-05.7** O administrador DEVE poder desbloquear uma conta pelo `sentry-backend`; o evento vai para o log de auditoria.
- **RN-F2-05.8** As consultas de contagem DEVEM ter índices próprios em `access_logs` (por `attempted_username` e por `ip_host`, com `attempted_at`), para não degradar o login.

## Critérios de aceite

- **CA-F2-05.1** Dadas 5 senhas erradas seguidas para `john.doe`, então a sexta tentativa, mesmo com a senha certa, falha com a mensagem genérica e o registro `ACCOUNT_LOCKED`.
- **CA-F2-05.2** Dados 16 minutos depois do bloqueio, então a senha certa volta a funcionar.
- **CA-F2-05.3** Dadas 20 falhas de um IP contra contas diferentes, então a próxima tentativa daquele IP no login direto recebe `429` com `Retry-After`.
- **CA-F2-05.4** Dado um login com sucesso depois de 4 falhas, então a contagem zera.
- **CA-F2-05.5** Dado um usuário bloqueado, quando o administrador o desbloqueia, então o próximo login com a senha certa funciona.

## Notas técnicas

- A contagem é feita com uma consulta em `access_logs` antes da verificação da senha; sem tabela nova de estado, porque o histórico já existe.
- Migração V5: índices `access_logs (lower(attempted_username), attempted_at)` e `access_logs (ip_host, attempted_at)`; o desbloqueio do administrador grava um registro `UNLOCKED` que a contagem trata como reinício.

## Questões em aberto

- Contar por conta ou por par conta + IP? Recomendado: por conta (protege de ataques distribuídos), com tempo curto para limitar o abuso de bloquear a conta alheia, mais o limite por IP.
- Pedir CAPTCHA depois de algumas falhas em vez de bloquear? Recomendado: não na Fase 2; exige serviço externo e o bloqueio progressivo basta por enquanto.
- Com várias instâncias do `sentry-auth`, a contagem no banco é compartilhada; precisa de cache (Redis)? Recomendado: não; o banco já é o ponto comum e os índices bastam no volume esperado.
