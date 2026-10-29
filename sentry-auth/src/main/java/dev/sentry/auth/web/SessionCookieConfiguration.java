package dev.sentry.auth.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * RN-F1-09.1: cookie da sessão SSO {@code HttpOnly}, {@code SameSite=Lax} e {@code Secure} quando a requisição é HTTPS
 * (o {@code request.isSecure()} já considera os proxies confiáveis). Declarado aqui para valer em qualquer ambiente, e não
 * só quando o Spring Boot aplica {@code server.servlet.session.cookie.*}.
 */
@Configuration(proxyBeanMethods = false)
public class SessionCookieConfiguration {

    @Bean
    CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }
}
