package dev.sentry.auth.login;

import static dev.sentry.auth.OAuthFixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.PkceFlow;
import dev.sentry.auth.TestcontainersConfiguration;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Sem {@code @Transactional}: o registro da tentativa grava em transação própria e precisa ver os dados. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class LoginTest {

    private static final String SECRET = "segredo-do-sistema-confiavel";
    private static final List<String> CLIENTS = List.of("DIR_TRUSTED", "DIR_PLAIN", "DIR_PUB");
    private static final List<String> USERS = List.of("direct.user");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @BeforeEach
    void createClientsAndUser() {
        OAuthFixtures.cleanUp(jdbcTemplate, CLIENTS, USERS);
        OAuthFixtures.createClient(jdbcTemplate, "DIR_TRUSTED", "CONFIDENTIAL", passwordService.hash(SECRET), true,
                "openid profile email offline_access");
        OAuthFixtures.createClient(jdbcTemplate, "DIR_PLAIN", "CONFIDENTIAL", passwordService.hash(SECRET), false,
                "openid profile");
        OAuthFixtures.createClient(jdbcTemplate, "DIR_PUB", "PUBLIC", null, true, "openid profile");
        OAuthFixtures.createUserWithRole(jdbcTemplate, "direct.user", passwordService.hash(PASSWORD), "DIR_TRUSTED");
        OAuthFixtures.grantRole(jdbcTemplate, "direct.user", "DIR_PLAIN");
        OAuthFixtures.grantRole(jdbcTemplate, "direct.user", "DIR_PUB");
    }

    @AfterEach
    void cleanUp() {
        OAuthFixtures.cleanUp(jdbcTemplate, CLIENTS, USERS);
    }

    private ResultActions directLogin(String clientId, String secret, String password, String scope) throws Exception {
        String scopeField = scope == null ? "" : ", \"scope\": \"" + scope + "\"";
        return mockMvc.perform(post("/api/v1/auth/login").with(httpBasic(clientId, secret))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"direct.user\", \"password\": \"" + password + "\"" + scopeField + "}"));
    }

    private void assertValidJwt(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        JWKSet jwks = JWKSet.parse(mockMvc.perform(get("/oauth2/jwks")).andReturn().getResponse().getContentAsString());
        assertThat(jwt.verify(new RSASSAVerifier((RSAKey) jwks.getKeyByKeyId(jwt.getHeader().getKeyID())))).isTrue();
    }

    // CA-F1-11.1, RN-F1-11.6, CA-F1-10.4
    @Test
    void trustedSystemGetsTokensAndNothingSensitiveIsLogged(CapturedOutput output) throws Exception {
        ResultActions result = directLogin("DIR_TRUSTED", SECRET, PASSWORD, "openid profile offline_access")
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.scope").value("openid profile offline_access"));
        String accessToken = PkceFlow.json(result, "$.access_token");
        String refreshToken = PkceFlow.json(result, "$.refresh_token");

        assertValidJwt(accessToken);
        assertValidJwt(PkceFlow.json(result, "$.id_token"));
        assertThat(SignedJWT.parse(accessToken).getJWTClaimsSet().getStringClaim("client_id")).isEqualTo("DIR_TRUSTED");
        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain(accessToken).doesNotContain(refreshToken);
    }

    // RN-F1-11.4: sem escopo, openid profile e nada de refresh token
    @Test
    void defaultScopeIsOpenidProfile() throws Exception {
        directLogin("DIR_TRUSTED", SECRET, PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("openid profile"))
                .andExpect(jsonPath("$.refresh_token").doesNotExist());
    }

    // CA-F1-11.2
    @Test
    void untrustedSystemIsUnauthorizedClient() throws Exception {
        directLogin("DIR_PLAIN", SECRET, PASSWORD, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("unauthorized_client"));
    }

    // CA-F1-11.3
    @Test
    void publicSystemIsInvalidClient() throws Exception {
        directLogin("DIR_PUB", "qualquer", PASSWORD, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void wrongClientSecretIsInvalidClient() throws Exception {
        directLogin("DIR_TRUSTED", "errado", PASSWORD, null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    // CA-F1-11.4, RN-F1-11.7, RN-F1-11.9
    @Test
    void wrongPasswordIsInvalidGrantAndRecordedAsDirect() throws Exception {
        directLogin("DIR_TRUSTED", SECRET, "errada", null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"invalid_grant\",\"error_description\":\"Usuário ou senha inválidos\"}"));

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM access_logs a JOIN systems s ON s.id_system = a.id_system
                WHERE s.external_code = 'DIR_TRUSTED' AND a.attempt_result = 'INVALID_CREDENTIALS' AND a.channel = 'DIRECT'""",
                Integer.class)).isEqualTo(1);
    }

    // CA-F1-11.5, RN-F1-11.8
    @Test
    void directLoginCreatesNoSsoSession() throws Exception {
        var response = directLogin("DIR_TRUSTED", SECRET, PASSWORD, null).andReturn().getResponse();

        assertThat(response.getCookie(PkceFlow.SESSION)).isNull();
        assertThat(PkceFlow.authorizeAnonymously(mockMvc, OAuthFixtures.authorizeUrl("DIR_PUB", "openid")))
                .isEqualTo("/login");
    }

    // CA-F1-11.6
    @Test
    void refreshTokenWorksOnTokenEndpoint() throws Exception {
        String refreshToken = PkceFlow.json(directLogin("DIR_TRUSTED", SECRET, PASSWORD, "openid offline_access"),
                "$.refresh_token");

        PkceFlow.refresh(mockMvc, "DIR_TRUSTED", SECRET, refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty());
    }

    @Test
    void scopeOutsideSystemIsInvalidScope() throws Exception {
        directLogin("DIR_PLAIN", SECRET, PASSWORD, "openid offline_access");
        mockMvc.perform(post("/api/v1/auth/login").with(httpBasic("DIR_TRUSTED", SECRET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"direct.user\", \"password\": \"" + PASSWORD + "\", \"scope\": \"openid admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_scope"));
    }

    // RN-F1-11.10
    @Test
    void plainHttpOutsideLocalhostIsRejected() throws Exception {
        mockMvc.perform(post(URI.create("http://sentry.exemplo.com/api/v1/auth/login")).with(httpBasic("DIR_TRUSTED", SECRET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"direct.user\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    // RN-F1-11.10: documentado no OpenAPI do sentry-auth
    @Test
    void endpointIsDocumentedInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/v1/auth/login")));
    }
}
