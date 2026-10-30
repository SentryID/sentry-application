package dev.sentry.auth.provider;

/** Valores de {@code access_logs.attempt_result} (RN-F1-10.2), comuns a todos os provedores de autenticação. */
public enum AttemptResult {
    SUCCESS, INVALID_CREDENTIALS, USER_NOT_FOUND, USER_INACTIVE, NO_SYSTEM_ACCESS, SYSTEM_INACTIVE
}
