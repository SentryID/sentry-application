package dev.sentry.auth.oauth;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.stereotype.Component;

/** Resposta do {@code /userinfo} (F1-08), lida do banco no momento da chamada, não do token. */
@Component
public class UserInfoMapper implements Function<OidcUserInfoAuthenticationContext, OidcUserInfo> {

    private final UserRepository userRepository;

    public UserInfoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Monta os claims do usuário dono do access token: sempre {@code sub}; com {@code profile}, {@code name} e
     * {@code preferred_username}; com {@code email}, {@code email} (RN-F1-08.2, .3).
     *
     * @param context access token já validado, com escopo {@code openid}
     * @return os claims
     * @throws OAuth2AuthenticationException {@code invalid_token} se o usuário estiver inativo ou excluído (RN-F1-08.4)
     */
    @Override
    public OidcUserInfo apply(OidcUserInfoAuthenticationContext context) {
        String sub = context.getAuthorization().getPrincipalName();
        User user = userRepository.findByUuid(UUID.fromString(sub));
        if (user == null || !user.getIsActive() || user.getIsDeleted()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_TOKEN);
        }
        Set<String> scopes = context.getAccessToken().getScopes();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", sub);
        if (scopes.contains(OidcScopes.PROFILE)) {
            claims.put("name", user.getName());
            claims.put("preferred_username", user.getUsername());
        }
        if (scopes.contains(OidcScopes.EMAIL)) {
            claims.put("email", user.getEmail());
        }
        return new OidcUserInfo(claims);
    }
}
