package dev.sentry.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS dos endpoints que um SPA chama por {@code fetch} (descoberta, JWKS, token, userinfo, revogação): só a origem de
 * uma URI de redirecionamento de sistema ativo é aceita, sem lista a manter à parte. Roda como filtro antes do Spring
 * Security, que responderia ao preflight {@code OPTIONS} com o redirecionamento para a tela de login.
 */
@Component
@Order(-102)
public class RegisteredOriginCorsFilter extends CorsFilter {

    private static final Set<String> SPA_ENDPOINTS = Set.of("/.well-known/openid-configuration",
            "/.well-known/oauth-authorization-server", "/oauth2/jwks", "/oauth2/token", "/oauth2/revoke", "/userinfo");

    public RegisteredOriginCorsFilter(JdbcTemplate jdbcTemplate) {
        super(request -> configurationFor(request, jdbcTemplate));
    }

    /**
     * Libera a origem da requisição quando ela é a origem de alguma URI cadastrada de um sistema ativo.
     *
     * @param request requisição com o cabeçalho {@code Origin}
     * @return a configuração para essa origem, ou {@code null} para não liberar o CORS
     */
    static CorsConfiguration configurationFor(HttpServletRequest request, JdbcTemplate jdbcTemplate) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || !SPA_ENDPOINTS.contains(request.getRequestURI()) || !isRegistered(origin, jdbcTemplate)) {
            return null;
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origin));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setMaxAge(300L);
        return configuration;
    }

    private static boolean isRegistered(String origin, JdbcTemplate jdbcTemplate) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM system_redirect_uris r JOIN systems s ON s.id_system = r.id_system
                    WHERE s.is_active AND NOT s.is_deleted
                      AND (r.uri = ? OR left(r.uri, length(?) + 1) = ? || '/'))""",
                Boolean.class, origin, origin, origin));
    }
}
