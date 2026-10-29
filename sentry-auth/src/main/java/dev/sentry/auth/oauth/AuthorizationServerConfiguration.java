package dev.sentry.auth.oauth;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.auth.provider.LoginAttempt;
import dev.sentry.auth.web.LoginPageController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authorization Server do Spring Security (D-02) e as regras de acesso do sentry-auth. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
public class AuthorizationServerConfiguration {

    // RN-F1-03.8: só os fluxos usados na Fase 1 aparecem na descoberta
    private static final List<String> GRANT_TYPES = List.of("authorization_code", "refresh_token");
    private static final List<String> CLIENT_AUTH_METHODS = List.of("client_secret_basic", "client_secret_post", "none");
    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; frame-ancestors 'none'; base-uri 'none'; object-src 'none'";
    private static final Set<String> CACHEABLE_PATHS =
            Set.of("/.well-known/openid-configuration", "/.well-known/oauth-authorization-server", "/oauth2/jwks");

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        var authorizationServer = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(authorizationServer.getEndpointsMatcher())
                .with(authorizationServer, server -> server
                        .authorizationServerMetadataEndpoint(metadata -> metadata.authorizationServerMetadataCustomizer(
                                builder -> builder
                                        .grantTypes(types -> types.retainAll(GRANT_TYPES))
                                        .tokenEndpointAuthenticationMethods(methods -> methods.retainAll(CLIENT_AUTH_METHODS))))
                        .oidc(oidc -> oidc.providerConfigurationEndpoint(provider -> provider.providerConfigurationCustomizer(
                                builder -> builder
                                        .grantTypes(types -> types.retainAll(GRANT_TYPES))
                                        .tokenEndpointAuthenticationMethods(methods -> methods.retainAll(CLIENT_AUTH_METHODS))))))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .addFilterBefore(new CacheablePublicMetadataFilter(), HeaderWriterFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/error", "/css/**").permitAll()
                        .anyRequest().authenticated())
                // F1-04: tela própria; os detalhes levam o sistema do fluxo, o IP e o User-Agent ao provedor
                .formLogin(form -> form.loginPage("/login").permitAll()
                        .authenticationDetailsSource(request -> new LoginAttempt(
                                LoginPageController.requestedClientId(request), Channel.PKCE,
                                request.getRemoteAddr(), request.getHeader("User-Agent")))
                        .failureHandler((request, response, exception) -> {
                            request.getSession().setAttribute(LoginPageController.LAST_USERNAME,
                                    request.getParameter("username"));
                            response.sendRedirect(request.getContextPath() + "/login?error");
                        }))
                // RN-F1-04.6: X-Frame-Options DENY e Cache-Control no-store já são o padrão do Spring Security
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)));
        return http.build();
    }

    // RN-F1-05.5: o issuer é o SENTRY_ISSUER_URL
    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${sentry.issuer-url}") String issuerUrl) {
        return AuthorizationServerSettings.builder().issuer(issuerUrl).build();
    }

    @Bean
    OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * RN-F1-07.6: descoberta e JWKS são públicos e cacheáveis. O cabeçalho é gravado antes do {@link HeaderWriterFilter},
     * que só põe o {@code no-cache} padrão quando a resposta ainda não tem {@code Cache-Control}.
     */
    static class CacheablePublicMetadataFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (CACHEABLE_PATHS.contains(request.getRequestURI())) {
                response.setHeader("Cache-Control", "public, max-age=300");
            }
            chain.doFilter(request, response);
        }
    }
}
