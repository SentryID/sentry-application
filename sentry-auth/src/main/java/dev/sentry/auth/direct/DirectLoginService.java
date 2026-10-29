package dev.sentry.auth.direct;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.oauth.TokenClaimsCustomizer;
import dev.sentry.auth.provider.DatabaseAuthenticationProvider;
import dev.sentry.auth.provider.LoginAttempt;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;

/**
 * Login direto por API para sistemas confiáveis (F1-11): o sistema coleta a senha na própria tela e recebe os mesmos
 * tokens do fluxo PKCE, sem sessão SSO.
 */
@Service
public class DirectLoginService {

    /** Tipo de concessão gravado na autorização; não é anunciado na descoberta nem aceito no {@code /oauth2/token}. */
    public static final AuthorizationGrantType DIRECT_LOGIN =
            new AuthorizationGrantType("urn:sentry:params:oauth:grant-type:direct-login");
    static final String DEFAULT_SCOPE = "openid profile";

    private static final OAuth2TokenType ID_TOKEN = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);
    private static final Set<ClientAuthenticationMethod> SECRET_METHODS =
            Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, ClientAuthenticationMethod.CLIENT_SECRET_POST);

    private final RegisteredClientRepository registeredClientRepository;
    private final SystemRepository systemRepository;
    private final PasswordService passwordService;
    private final ProviderManager authenticationManager;
    private final OAuth2TokenGenerator<OAuth2Token> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;
    private final AuthorizationServerSettings authorizationServerSettings;

    public DirectLoginService(RegisteredClientRepository registeredClientRepository, SystemRepository systemRepository,
            PasswordService passwordService, List<AuthenticationProvider> authenticationProviders,
            OAuth2TokenGenerator<OAuth2Token> tokenGenerator, OAuth2AuthorizationService authorizationService,
            AuthorizationServerSettings authorizationServerSettings) {
        this.registeredClientRepository = registeredClientRepository;
        this.systemRepository = systemRepository;
        this.passwordService = passwordService;
        // os mesmos provedores da tela de login: o LDAP, quando entrar, vale aqui sem mudar este código (RN-F1-02.7)
        this.authenticationManager = new ProviderManager(authenticationProviders);
        this.tokenGenerator = tokenGenerator;
        this.authorizationService = authorizationService;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    /**
     * Autentica o sistema e o usuário e emite os tokens.
     *
     * @param clientId {@code client_id} do sistema chamador, do HTTP Basic
     * @param clientSecret segredo do sistema chamador, do HTTP Basic
     * @param request credenciais do usuário e escopos pedidos
     * @param ipAddress IP do cliente, para o registro da tentativa
     * @param userAgent {@code User-Agent} da requisição, para o registro da tentativa
     * @return os tokens no formato de resposta do OAuth
     * @throws DirectLoginException {@code 401 invalid_client} se o sistema não autenticar ou for público;
     *     {@code 403 unauthorized_client} se não for confiável; {@code 400 invalid_scope} se pedir escopo não permitido;
     *     {@code 401 invalid_grant} com a mensagem genérica se as credenciais do usuário falharem
     */
    public DirectLoginRest.TokenResponse login(String clientId, String clientSecret, DirectLoginRest.LoginRequest request,
            String ipAddress, String userAgent) {
        RegisteredClient client = authenticateClient(clientId, clientSecret);
        ExternalSystem system = systemRepository.findByExternalCode(clientId);
        if (!system.getIsTrusted()) {
            throw new DirectLoginException(HttpStatus.FORBIDDEN, "unauthorized_client",
                    "Este sistema não está autorizado a usar o login direto");
        }
        Set<String> scopes = requestedScopes(request.scope());
        if (!client.getScopes().containsAll(scopes)) {
            throw new DirectLoginException(HttpStatus.BAD_REQUEST, "invalid_scope", "Escopo não permitido para este sistema");
        }

        Authentication principal = authenticateUser(request, clientId, ipAddress, userAgent);
        return issueTokens(client, principal, scopes);
    }

    // RN-F1-11.2 e RN-F1-11.3: só cliente confidencial, com o segredo certo
    private RegisteredClient authenticateClient(String clientId, String clientSecret) {
        RegisteredClient client = clientId == null ? null : registeredClientRepository.findByClientId(clientId);
        boolean authenticated = client != null && client.getClientSecret() != null
                && client.getClientAuthenticationMethods().stream().anyMatch(SECRET_METHODS::contains)
                && passwordService.verify(clientSecret, client.getClientSecret()).matches();
        if (!authenticated) {
            throw new DirectLoginException(HttpStatus.UNAUTHORIZED, "invalid_client", "Falha na autenticação do sistema");
        }
        return client;
    }

    private Authentication authenticateUser(DirectLoginRest.LoginRequest request, String clientId, String ipAddress,
            String userAgent) {
        var attempt = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        attempt.setDetails(new LoginAttempt(clientId, Channel.DIRECT, ipAddress, userAgent));
        try {
            return authenticationManager.authenticate(attempt);
        } catch (AuthenticationException e) {
            throw new DirectLoginException(HttpStatus.UNAUTHORIZED, "invalid_grant",
                    DatabaseAuthenticationProvider.INVALID_CREDENTIALS_MESSAGE);
        }
    }

    private DirectLoginRest.TokenResponse issueTokens(RegisteredClient client, Authentication principal, Set<String> scopes) {
        OAuth2Authorization.Builder authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName(principal.getName())
                .authorizationGrantType(DIRECT_LOGIN)
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), principal)
                // RN-F1-06.5: a vida máxima do refresh conta deste login
                .attribute(TokenClaimsCustomizer.LOGIN_TIME_ATTRIBUTE, String.valueOf(Instant.now().getEpochSecond()));
        DefaultOAuth2TokenContext.Builder context = DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .principal(principal)
                .authorizationServerContext(serverContext())
                .authorization(authorization.build())
                .authorizedScopes(scopes)
                .authorizationGrantType(DIRECT_LOGIN)
                .authorizationGrant(principal);

        OAuth2Token jwt = tokenGenerator.generate(context.tokenType(OAuth2TokenType.ACCESS_TOKEN).build());
        var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, jwt.getTokenValue(),
                jwt.getIssuedAt(), jwt.getExpiresAt(), scopes);
        authorization.token(accessToken, metadata -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                ((ClaimAccessor) jwt).getClaims()));

        OAuth2RefreshToken refreshToken = (OAuth2RefreshToken) tokenGenerator.generate(
                context.tokenType(OAuth2TokenType.REFRESH_TOKEN).build());
        if (refreshToken != null) {
            authorization.refreshToken(refreshToken);
        }

        OidcIdToken idToken = null;
        if (scopes.contains("openid")) {
            OAuth2Token idJwt = tokenGenerator.generate(context.tokenType(ID_TOKEN).build());
            idToken = new OidcIdToken(idJwt.getTokenValue(), idJwt.getIssuedAt(), idJwt.getExpiresAt(),
                    ((ClaimAccessor) idJwt).getClaims());
            OidcIdToken claims = idToken;
            authorization.token(idToken, metadata -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                    claims.getClaims()));
        }
        authorizationService.save(authorization.build());

        return new DirectLoginRest.TokenResponse(accessToken.getTokenValue(), "Bearer",
                Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).toSeconds(),
                idToken == null ? null : idToken.getTokenValue(),
                refreshToken == null ? null : refreshToken.getTokenValue(),
                String.join(" ", scopes));
    }

    // fora da cadeia do Authorization Server não há AuthorizationServerContext; o issuer vem da mesma configuração
    private AuthorizationServerContext serverContext() {
        return new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return authorizationServerSettings.getIssuer();
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return authorizationServerSettings;
            }
        };
    }

    // RN-F1-11.4
    private static Set<String> requestedScopes(String scope) {
        String value = scope == null || scope.isBlank() ? DEFAULT_SCOPE : scope;
        return new LinkedHashSet<>(Arrays.asList(value.trim().split("\\s+")));
    }
}
