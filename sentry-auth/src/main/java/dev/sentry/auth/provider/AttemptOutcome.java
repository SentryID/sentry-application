package dev.sentry.auth.provider;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.user.User;

/**
 * Resultado de uma tentativa de login, antes de virar exceção ou autenticação.
 *
 * @param result motivo real, gravado em {@code access_logs}
 * @param user usuário encontrado, ou {@code null} quando não existe
 * @param system sistema que pediu a autenticação, ou {@code null} quando não existe
 */
record AttemptOutcome(AttemptResult result, User user, ExternalSystem system) {
}
