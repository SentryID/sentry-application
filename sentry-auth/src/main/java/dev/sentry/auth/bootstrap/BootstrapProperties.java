package dev.sentry.auth.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Administrador criado na primeira subida (RN-F0-03.3). Lidas de `SENTRY_ADMIN_USERNAME`,
 * `SENTRY_ADMIN_EMAIL` e `SENTRY_ADMIN_PASSWORD`.
 *
 * @param username login do administrador
 * @param email e-mail do administrador
 * @param password senha do administrador; vazia faz o bootstrap gerar uma (RN-F0-03.4)
 */
@ConfigurationProperties("sentry.admin")
public record BootstrapProperties(
        @DefaultValue("admin") String username,
        @DefaultValue("admin@sentry.local") String email,
        String password) {

    // a senha não pode aparecer em logs (RN-F0-02.6)
    @Override
    public String toString() {
        return "BootstrapProperties[username=" + username + ", email=" + email + ", password=***]";
    }
}
