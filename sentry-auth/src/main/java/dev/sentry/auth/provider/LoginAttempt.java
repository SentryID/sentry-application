package dev.sentry.auth.provider;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;

/**
 * Contexto de uma tentativa de login, levado nos {@code details} da autenticação.
 *
 * @param clientId {@code client_id} do sistema que pediu a autenticação
 * @param channel tela de login (PKCE) ou login direto (DIRECT)
 * @param ipAddress IP do cliente, já resolvido pelos proxies confiáveis (RN-F1-10.4)
 * @param userAgent cabeçalho {@code User-Agent} da requisição
 */
public record LoginAttempt(String clientId, Channel channel, String ipAddress, String userAgent) {
}
