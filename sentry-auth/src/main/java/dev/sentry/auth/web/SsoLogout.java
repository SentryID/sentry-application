package dev.sentry.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public static final String LOGGED_OUT_PATH = "/logged-out";

    private static final Logger log = LoggerFactory.getLogger(SsoLogout.class);
    private static final OAuth2TokenType ID_TOKEN = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

    private final OAuth2AuthorizationService authorizationService;
    private final JdbcTemplate jdbcTemplate;

    public SsoLogout(OAuth2AuthorizationService authorizationService, JdbcTemplate jdbcTemplate) {
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Logout válido: revoga os tokens da sessão, encerra a sessão e volta à {@code post_logout_redirect_uri}
     * cadastrada com o {@code state}, ou mostra a página do Sentry quando não há URI (RN-F1-09.5, .6).
     */
    public void onSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        var logout = (OidcLogoutAuthenticationToken) authentication;
        endSession(request, logout.getIdTokenHint());
        if (!StringUtils.hasText(logout.getPostLogoutRedirectUri())) {
            response.sendRedirect(request.getContextPath() + LOGGED_OUT_PATH);
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
     * navegador fica numa página do Sentry, sem redirecionar (CA-F1-09.3).
     */
    public void onError(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        endSession(request, request.getParameter("id_token_hint"));
        response.sendRedirect(request.getContextPath() + LOGGED_OUT_PATH);
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
        // ponytail: busca o sid no JSON dos claims gravados; se o volume crescer, guardar o sid numa coluna indexada
        List<String> ids = sid == null ? List.of(authorization.getId()) : jdbcTemplate.queryForList(
                "SELECT id FROM oauth2_authorization WHERE principal_name = ? AND oidc_id_token_metadata LIKE ?",
                String.class, authorization.getPrincipalName(), "%\"sid\":\"" + sid + "\"%");
        for (String id : ids) {
            OAuth2Authorization sessionAuthorization = authorizationService.findById(id);
            if (sessionAuthorization != null) {
                authorizationService.remove(sessionAuthorization);
            }
        }
        log.atInfo().addKeyValue("sub", authorization.getPrincipalName()).addKeyValue("revoked", ids.size())
                .log("Logout da sessão SSO");
    }
}
