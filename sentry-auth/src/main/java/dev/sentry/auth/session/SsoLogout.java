package dev.sentry.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** Fim da sessão SSO pelo {@code /connect/logout} do OpenID Connect (F1-09). */
@Component
public class SsoLogout {

    private static final Logger log = LoggerFactory.getLogger(SsoLogout.class);
    private static final OAuth2TokenType ID_TOKEN = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

    private final OAuth2AuthorizationService authorizationService;
    private final JdbcTemplate jdbcTemplate;
    private final String loginUrl;

    public SsoLogout(OAuth2AuthorizationService authorizationService, JdbcTemplate jdbcTemplate,
            @Value("${sentry.login-url}") String loginUrl) {
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
        this.loginUrl = loginUrl;
    }

    /**
     * Logout válido: revoga os tokens da sessão, encerra a sessão e volta à {@code post_logout_redirect_uri}
     * cadastrada com o {@code state}, ou para a tela de login do front-end quando não há URI (RN-F1-09.5, .6).
     */
    public void onSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        var logout = (OidcLogoutAuthenticationToken) authentication;
        endSession(request, logout.getIdTokenHint());
        if (!StringUtils.hasText(logout.getPostLogoutRedirectUri())) {
            response.sendRedirect(loginUrl);
            return;
        }
        var redirect = UriComponentsBuilder.fromUriString(logout.getPostLogoutRedirectUri());
        if (StringUtils.hasText(logout.getState())) {
            redirect.queryParam("state", logout.getState());
        }
        response.sendRedirect(redirect.encode().build().toUriString());
    }

    /**
     * Logout recusado (ex.: {@code post_logout_redirect_uri} não cadastrada): a sessão acaba do mesmo jeito, mas o
     * navegador vai para a tela de login do front-end, nunca para a URI recusada (CA-F1-09.3).
     */
    public void onError(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        endSession(request, request.getParameter("id_token_hint"));
        response.sendRedirect(loginUrl);
    }

    private void endSession(HttpServletRequest request, String idTokenHint) {
        revokeSessionTokens(idTokenHint);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * RN-F1-09.6: revoga todas as autorizações (e os refresh tokens delas) emitidas na mesma sessão SSO do ID token.
     *
     * @param idTokenHint ID token apresentado no logout; sem ele, não há sessão a identificar
     */
    void revokeSessionTokens(String idTokenHint) {
        if (!StringUtils.hasText(idTokenHint)) {
            return;
        }
        OAuth2Authorization authorization = authorizationService.findByToken(idTokenHint, ID_TOKEN);
        if (authorization == null) {
            return;
        }
        String sid = authorization.getToken(OidcIdToken.class).getToken().getClaimAsString("sid");
        if (sid == null) {
            authorizationService.remove(authorization);
            return;
        }
        revokeBySid(authorization.getPrincipalName(), sid);
    }

    /**
     * RN-F1-09.6: revoga as autorizações (e os refresh tokens delas) emitidas na sessão SSO da requisição, para o logout
     * feito pela tela do front-end ({@code POST /api/v1/session/logout}), que não tem ID token. O {@code sid} dos ID
     * tokens é o SHA-256 do id da sessão em Base64 URL, como o Authorization Server o calcula.
     *
     * @param request requisição com o cookie da sessão a encerrar
     * @param principalName {@code sub} do usuário da sessão
     */
    public void revokeSessionTokens(HttpServletRequest request, String principalName) {
        HttpSession session = request.getSession(false);
        if (session == null || principalName == null) {
            return;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(session.getId().getBytes(StandardCharsets.US_ASCII));
            revokeBySid(principalName, Base64.getUrlEncoder().withoutPadding().encodeToString(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void revokeBySid(String principalName, String sid) {
        // ponytail: busca o sid no JSON dos claims gravados; se o volume crescer, guardar o sid numa coluna indexada
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM oauth2_authorization WHERE principal_name = ? AND oidc_id_token_metadata LIKE ?",
                String.class, principalName, "%\"sid\":\"" + sid + "\"%");
        for (String id : ids) {
            OAuth2Authorization sessionAuthorization = authorizationService.findById(id);
            if (sessionAuthorization != null) {
                authorizationService.remove(sessionAuthorization);
            }
        }
        log.atInfo().addKeyValue("sub", principalName).addKeyValue("revoked", ids.size()).log("Logout da sessão SSO");
    }
}
