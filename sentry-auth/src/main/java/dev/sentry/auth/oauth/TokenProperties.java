package dev.sentry.auth.oauth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Vida dos tokens, configurável por instalação (RN-F1-05.7, RN-F1-06.5).
 *
 * @param accessTokenTtl vida do access token
 * @param idTokenTtl vida do ID token
 * @param refreshTokenTtl vida do refresh token sem uso; renova a cada rotação
 * @param refreshTokenMaxTtl vida máxima da autorização, contada do login, mesmo com rotação
 */
@ConfigurationProperties("sentry.token")
public record TokenProperties(
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("15m") Duration idTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("90d") Duration refreshTokenMaxTtl) {
}
