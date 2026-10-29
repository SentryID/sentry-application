package dev.sentry.auth.oauth;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Claims do access token e do ID token (F1-05), lidos do banco no momento da emissão. */
@Component
public class TokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private static final Logger log = LoggerFactory.getLogger(TokenClaimsCustomizer.class);

    private final UserRepository userRepository;
    private final SystemRepository systemRepository;
    private final TokenProperties tokenProperties;

    public TokenClaimsCustomizer(UserRepository userRepository, SystemRepository systemRepository,
            TokenProperties tokenProperties) {
        this.userRepository = userRepository;
        this.systemRepository = systemRepository;
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
     *     também barra o refresh (RN-F1-06.6)
     */
    @Override
    @Transactional(readOnly = true)
    public void customize(JwtEncodingContext context) {
        User user = userRepository.findByUuid(UUID.fromString(context.getPrincipal().getName()));
        if (user == null || !user.getIsActive() || user.getIsDeleted()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }
        String clientId = context.getRegisteredClient().getClientId();
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            ExternalSystem system = systemRepository.findByExternalCode(clientId);
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
}
