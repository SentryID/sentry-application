package dev.sentry.auth.oauth;

import dev.sentry.api.domain.access.roleuser.api.RoleUserRepository;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/** Claims do access token e do ID token (F1-05), lidos do banco no momento da emissão. */
@Component
public class TokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    /**
     * Atributo da autorização com o instante do login em segundos, usado quando não há código de autorização. É texto
     * porque o Authorization Server grava os atributos em JSON e só aceita tipos conhecidos.
     */
    public static final String LOGIN_TIME_ATTRIBUTE = "sentry.login_time";

    private static final Logger log = LoggerFactory.getLogger(TokenClaimsCustomizer.class);

    private final UserRepository userRepository;
    private final SystemRepository systemRepository;
    private final RoleUserRepository roleUserRepository;
    private final TokenProperties tokenProperties;

    public TokenClaimsCustomizer(UserRepository userRepository, SystemRepository systemRepository,
            RoleUserRepository roleUserRepository, TokenProperties tokenProperties) {
        this.userRepository = userRepository;
        this.systemRepository = systemRepository;
        this.roleUserRepository = roleUserRepository;
        this.tokenProperties = tokenProperties;
    }

    /**
     * Completa os claims. No access token: {@code typ=at+jwt}, {@code client_id} e {@code org} (RN-F1-05.1, .2). No ID
     * token: {@code name} e {@code preferred_username} com o escopo {@code profile}, {@code email} com o escopo
     * {@code email}, e a vida configurada (RN-F1-05.3, .7). O access token nunca leva e-mail, papéis nem ações
     * (RN-F1-05.8). Registra a emissão no log, sem o token (RN-F1-10.7).
     *
     * @param context token em montagem; o {@code sub} já é o {@code uuid} do usuário (RN-F1-05.4)
     * @throws OAuth2AuthenticationException {@code invalid_grant} se o usuário estiver inativo ou excluído, o que
     *     também barra o refresh (RN-F1-06.6); se não tiver papel ativo no sistema, o que vale também para o login por
     *     sessão SSO iniciado por outro sistema (RN-F1-02.5); ou se o refresh passar da vida máxima contada do login
     *     (RN-F1-06.5)
     */
    @Override
    public void customize(JwtEncodingContext context) {
        User user = userRepository.findByUuid(UUID.fromString(context.getPrincipal().getName()));
        if (user == null || !user.getIsActive() || user.getIsDeleted()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }
        if (AuthorizationGrantType.REFRESH_TOKEN.equals(context.getAuthorizationGrantType())
                && loginTime(context.getAuthorization()).plus(tokenProperties.refreshTokenMaxTtl()).isBefore(Instant.now())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }
        String clientId = context.getRegisteredClient().getClientId();
        ExternalSystem system = systemRepository.findByExternalCode(clientId);
        if (!roleUserRepository.hasActiveRoleInSystem(user.getId(), system.getId())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            context.getJwsHeader().type("at+jwt");
            context.getClaims().claim("client_id", clientId)
                    .claim("org", system.getOrganization().getUuid().toString());
        } else if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            context.getClaims().claims(claims -> {
                if (context.getAuthorizedScopes().contains(OidcScopes.PROFILE)) {
                    claims.put("name", user.getName());
                    claims.put("preferred_username", user.getUsername());
                }
                if (context.getAuthorizedScopes().contains(OidcScopes.EMAIL)) {
                    claims.put("email", user.getEmail());
                }
                claims.put("exp", ((Instant) claims.get("iat")).plus(tokenProperties.idTokenTtl()));
            });
        }
        log.atInfo()
                .addKeyValue("client_id", clientId)
                .addKeyValue("sub", context.getPrincipal().getName())
                .addKeyValue("grant_type", context.getAuthorizationGrantType().getValue())
                .addKeyValue("token_type", context.getTokenType().getValue())
                .log("Token emitido");
    }

    /**
     * Instante do login que originou a autorização: a emissão do código no fluxo PKCE, ou o atributo
     * {@value #LOGIN_TIME_ATTRIBUTE} gravado pelo login direto (F1-11).
     */
    static Instant loginTime(OAuth2Authorization authorization) {
        String loginTime = authorization.getAttribute(LOGIN_TIME_ATTRIBUTE);
        if (loginTime != null) {
            return Instant.ofEpochSecond(Long.parseLong(loginTime));
        }
        return authorization.getToken(OAuth2AuthorizationCode.class).getToken().getIssuedAt();
    }
}
