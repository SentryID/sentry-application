package dev.sentry.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.sentry.api.security.password.PasswordService;
import java.io.IOException;
import java.net.CookieManager;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Teste ponta a ponta da Fase 1 com o servidor numa porta real: um cliente OIDC genérico (Spring Security OAuth2 Client),
 * configurado só com o issuer e o {@code client_id}, faz login por PKCE, valida o ID token pelo JWKS, chama o
 * {@code /userinfo} e faz logout; um sistema confidencial renova o token; um sistema confiável faz login direto
 * (CA-F1-03.6 e critério de saída da Fase 1).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(TestcontainersConfiguration.class)
class EndToEndOidcTest {

    private static final int PORT = freePort();
    private static final String ISSUER = "http://127.0.0.1:" + PORT;
    private static final String SECRET = "segredo-do-sistema-e2e";
    private static final List<String> CLIENTS = List.of("E2E_SPA", "E2E_WEB");
    private static final List<String> USERS = List.of("e2e.user");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    private final HttpClient browser = HttpClient.newBuilder()
            .cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();

    @DynamicPropertySource
    static void serverOnFixedPort(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("sentry.issuer-url", () -> ISSUER);
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void createClientsAndUser() {
        OAuthFixtures.cleanUp(jdbcTemplate, CLIENTS, USERS);
        OAuthFixtures.createClient(jdbcTemplate, "E2E_SPA", "PUBLIC", null, false, "openid profile email");
        OAuthFixtures.createClient(jdbcTemplate, "E2E_WEB", "CONFIDENTIAL", passwordService.hash(SECRET), true,
                "openid profile offline_access");
        OAuthFixtures.createUserWithRole(jdbcTemplate, "e2e.user", passwordService.hash(OAuthFixtures.PASSWORD), "E2E_SPA");
        OAuthFixtures.grantRole(jdbcTemplate, "e2e.user", "E2E_WEB");
    }

    @AfterEach
    void cleanUp() {
        OAuthFixtures.cleanUp(jdbcTemplate, CLIENTS, USERS);
    }

    // só issuer e client_id: endpoints, JWKS e PKCE vêm da descoberta e da biblioteca
    private static ClientRegistration registration(String clientId, String secret, String... scopes) {
        var builder = ClientRegistrations.fromIssuerLocation(ISSUER).registrationId(clientId.toLowerCase())
                .clientId(clientId).redirectUri(OAuthFixtures.REDIRECT_URI).scope(scopes);
        if (secret == null) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            builder.clientSecret(secret).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }
        return builder.build();
    }

    private static OAuth2AuthorizationRequest authorizationRequest(ClientRegistration registration) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(registration), "/oauth2/authorization");
        var request = new MockHttpServletRequest("GET", "/oauth2/authorization/" + registration.getRegistrationId());
        request.setServletPath("/oauth2/authorization/" + registration.getRegistrationId());
        return resolver.resolve(request);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return browser.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String url) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(url.startsWith("/") ? ISSUER + url : url)).GET().build());
    }

    private static String location(HttpResponse<?> response) {
        return response.headers().firstValue("Location").orElseThrow();
    }

    /** Navega como um usuário: autorização, tela de login preenchida, volta ao redirect_uri com o código. */
    private OAuth2AuthorizationResponse browserLogin(OAuth2AuthorizationRequest authorizationRequest) throws Exception {
        HttpResponse<String> toLogin = get(authorizationRequest.getAuthorizationRequestUri());
        HttpResponse<String> loginPage = get(location(toLogin));
        Matcher csrf = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(loginPage.body());
        assertThat(csrf.find()).as("token CSRF na tela de login").isTrue();
        String form = "username=e2e.user&password=" + URLEncoder.encode(OAuthFixtures.PASSWORD, StandardCharsets.UTF_8)
                + "&_csrf=" + csrf.group(1);
        HttpResponse<String> afterLogin = send(HttpRequest.newBuilder(URI.create(ISSUER + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build());
        String callback = location(get(location(afterLogin)));
        assertThat(callback).startsWith(OAuthFixtures.REDIRECT_URI);
        var params = UriComponentsBuilder.fromUriString(callback).build().getQueryParams();
        return OAuth2AuthorizationResponse.success(params.getFirst("code"))
                .redirectUri(OAuthFixtures.REDIRECT_URI).state(params.getFirst("state")).build();
    }

    private static OAuth2AccessTokenResponse exchange(ClientRegistration registration,
            OAuth2AuthorizationRequest authorizationRequest, OAuth2AuthorizationResponse authorizationResponse) {
        return new RestClientAuthorizationCodeTokenResponseClient().getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(
                registration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)));
    }

    private static OidcIdToken idToken(ClientRegistration registration, OAuth2AccessTokenResponse tokens) {
        Jwt jwt = new OidcIdTokenDecoderFactory().createDecoder(registration)
                .decode((String) tokens.getAdditionalParameters().get("id_token"));
        return new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
    }

    // CA-F1-03.6, RN-F1-09.5: SPA pública faz login, lê o /userinfo e sai
    @Test
    void genericOidcClientLogsInReadsUserInfoAndLogsOut() throws Exception {
        ClientRegistration spa = registration("E2E_SPA", null, "openid", "profile", "email");
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest(spa);
        assertThat(authorizationRequest.getAdditionalParameters()).containsKey("code_challenge");

        OAuth2AccessTokenResponse tokens = exchange(spa, authorizationRequest, browserLogin(authorizationRequest));
        OidcIdToken idToken = idToken(spa, tokens);
        OidcUser user = new OidcUserService().loadUser(new OidcUserRequest(spa, tokens.getAccessToken(), idToken));

        assertThat(idToken.getIssuer().toString()).isEqualTo(ISSUER);
        assertThat(idToken.getNonce()).isNotBlank();
        assertThat(user.getPreferredUsername()).isEqualTo("e2e.user");
        assertThat(user.getEmail()).isEqualTo("e2e.user@sentry.dev");

        String endSession = (String) spa.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint");
        HttpResponse<String> logout = get(UriComponentsBuilder.fromUriString(endSession)
                .queryParam("id_token_hint", idToken.getTokenValue())
                .queryParam("post_logout_redirect_uri", OAuthFixtures.LOGOUT_URI)
                .queryParam("state", "fim").encode().build().toUriString());
        assertThat(location(logout)).isEqualTo(OAuthFixtures.LOGOUT_URI + "?state=fim");
        assertThat(location(get(authorizationRequest(spa).getAuthorizationRequestUri()))).endsWith("/login");
    }

    // CA-F1-06.1 pelo cliente genérico: sistema confidencial renova o token
    @Test
    void confidentialClientRefreshesWithGenericLibrary() throws Exception {
        ClientRegistration web = registration("E2E_WEB", SECRET, "openid", "offline_access");
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest(web);
        OAuth2AccessTokenResponse tokens = exchange(web, authorizationRequest, browserLogin(authorizationRequest));

        OAuth2AccessTokenResponse refreshed = new RestClientRefreshTokenTokenResponseClient().getTokenResponse(
                new OAuth2RefreshTokenGrantRequest(web, tokens.getAccessToken(), tokens.getRefreshToken()));

        assertThat(refreshed.getAccessToken().getTokenValue()).isNotEqualTo(tokens.getAccessToken().getTokenValue());
        assertThat(refreshed.getRefreshToken().getTokenValue()).isNotEqualTo(tokens.getRefreshToken().getTokenValue());
    }

    // critério de saída da Fase 1: sistema confiável faz login direto e recebe os mesmos tokens
    @Test
    void trustedSystemLogsInDirectly() throws Exception {
        String basic = Base64.getEncoder().encodeToString(("E2E_WEB:" + SECRET).getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(ISSUER + "/api/v1/auth/login"))
                .header("Authorization", "Basic " + basic).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\": \"e2e.user\", \"password\": \"" + OAuthFixtures.PASSWORD + "\"}"))
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        String idToken = JsonPath.read(response.body(), "$.id_token");
        Jwt jwt = new OidcIdTokenDecoderFactory().createDecoder(registration("E2E_WEB", SECRET, "openid"))
                .decode(idToken);
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("e2e.user");
    }
}
