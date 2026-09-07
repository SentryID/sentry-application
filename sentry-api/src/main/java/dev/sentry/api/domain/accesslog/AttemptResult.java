package dev.sentry.api.domain.accesslog;

/**
 * Desfecho de uma tentativa de acesso. Enum em vez de texto livre porque uma trilha de
 * auditoria com {@code String} solta só acumula variação de grafia e fica inconsultável.
 */
public enum AttemptResult {

    SUCCESS,
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    USER_INACTIVE,
    MFA_REQUIRED,
    MFA_FAILED,
    BLOCKED
}
