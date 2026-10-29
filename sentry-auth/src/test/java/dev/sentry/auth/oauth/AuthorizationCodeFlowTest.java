package dev.sentry.auth.oauth;

import static dev.sentry.auth.OAuthFixtures.CODE_VERIFIER;
import static dev.sentry.auth.OAuthFixtures.PASSWORD;
import static dev.sentry.auth.OAuthFixtures.REDIRECT_URI;
import static dev.sentry.auth.OAuthFixtures.authorizeUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.PkceFlow;
import dev.sentry.auth.TestcontainersConfiguration;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthorizationCodeFlowTest {

    private static final String SECRET = "segredo-do-sistema-confidencial";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @BeforeEach
    void createClientsAndUser() {
        OAuthFixtures.createClient(jdbcTemplate, "SPA", "PUBLIC", null, false, "openid profile email offline_access");
        OAuthFixtures.createClient(jdbcTemplate, "WEB", "CONFIDENTIAL", passwordService.hash(SECRET), false,
                "openid profile offline_access");
        String hash = passwordService.hash(PASSWORD);
        OAuthFixtures.createUserWithRole(jdbcTemplate, "flow.user", hash, "SPA");
        jdbcTemplate.update("""
                INSERT INTO roles (id_system, id_organization, system_role, uuid, is_active, is_deleted)
                SELECT id_system, id_organization, 'WEB.USER', gen_random_uuid(), TRUE, FALSE FROM systems
                WHERE external_code = 'WEB'""");
        jdbcTemplate.update("""
                INSERT INTO role_users (id_role, id_user, is_active, is_deleted)
                SELECT r.id_role, u.id_user, TRUE, FALSE FROM roles r, users u
                WHERE r.system_role = 'WEB.USER' AND u.username = 'flow.user'""");
    }

    // CA-F1-03.1
    @Test
    void publicClientCompletesPkceFlow() throws Exception {
        PkceFlow.Result result = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("SPA", "openid profile"),
                "flow.user", PASSWORD);

        assertThat(result.location()).startsWith(REDIRECT_URI);
        assertThat(PkceFlow.param(result.location(), "state")).isEqualTo("xyz");
        PkceFlow.exchange(mockMvc, "SPA", null, result.code(), CODE_VERIFIER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    // CA-F1-03.1 (refresh) e RN-F1-03.7: cliente confidencial com client_secret_basic recebe o refresh token
    @Test
    void confidentialClientWithOfflineAccessGetsRefreshToken() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("WEB", "openid offline_access"),
                "flow.user", PASSWORD).code();

        PkceFlow.exchange(mockMvc, "WEB", SECRET, code, CODE_VERIFIER)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.id_token").isNotEmpty());
    }

    // decisão da 1.7: cliente público não recebe refresh token
    @Test
    void publicClientNeverGetsRefreshToken() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("SPA", "openid offline_access"),
                "flow.user", PASSWORD).code();

        PkceFlow.exchange(mockMvc, "SPA", null, code, CODE_VERIFIER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refresh_token").doesNotExist());
    }

    // CA-F1-03.2, RN-F1-03.4: erro volta ao redirect_uri com o state
    @Test
    void authorizationWithoutCodeChallengeIsInvalidRequest() throws Exception {
        String url = authorizeUrl("SPA", "openid").replaceAll("&code_challenge(_method)?=[^&]*", "");

        String location = PkceFlow.authorizeAnonymously(mockMvc, url);

        assertThat(location).startsWith(REDIRECT_URI);
        assertThat(PkceFlow.param(location, "error")).isEqualTo("invalid_request");
        assertThat(PkceFlow.param(location, "state")).isEqualTo("xyz");
    }

    // RN-F1-03.2: plain não é aceito
    @Test
    void plainCodeChallengeIsRejected() throws Exception {
        String url = authorizeUrl("SPA", "openid").replace("code_challenge_method=S256", "code_challenge_method=plain");

        String location = PkceFlow.authorizeAnonymously(mockMvc, url);

        assertThat(PkceFlow.param(location, "error")).isEqualTo("invalid_request");
    }

    // RN-F1-03.4: escopo fora do cadastro do sistema
    @Test
    void scopeNotAllowedReturnsInvalidScope() throws Exception {
        String location = PkceFlow.authorizeAnonymously(mockMvc, authorizeUrl("WEB", "openid email"));

        assertThat(location).startsWith(REDIRECT_URI);
        assertThat(PkceFlow.param(location, "error")).isEqualTo("invalid_scope");
    }

    // CA-F1-03.3
    @Test
    void wrongCodeVerifierIsInvalidGrant() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("SPA", "openid"), "flow.user", PASSWORD).code();

        PkceFlow.exchange(mockMvc, "SPA", null, code, "verificador-errado-verificador-errado-verificador")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    // CA-F1-03.4, RN-F1-03.6
    @Test
    void reusedCodeIsRejectedAndRevokesRefreshToken() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("WEB", "openid offline_access"),
                "flow.user", PASSWORD).code();
        ResultActions first = PkceFlow.exchange(mockMvc, "WEB", SECRET, code, CODE_VERIFIER).andExpect(status().isOk());
        String refreshToken = PkceFlow.json(first, "$.refresh_token");

        PkceFlow.exchange(mockMvc, "WEB", SECRET, code, CODE_VERIFIER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        PkceFlow.refresh(mockMvc, "WEB", SECRET, refreshToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    // CA-F1-03.5, RN-F1-03.3
    @Test
    void unregisteredRedirectUriShowsErrorWithoutRedirect() throws Exception {
        String url = authorizeUrl("SPA", "openid").replace("3000/callback", "3000/outra");

        mockMvc.perform(get(URI.create(url)).accept("text/html"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Location"));
    }

    // RN-F1-03.7: o redirect_uri da troca tem de ser o mesmo da autorização
    @Test
    void exchangeWithDifferentRedirectUriFails() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("SPA", "openid"), "flow.user", PASSWORD).code();

        mockMvc.perform(post("/oauth2/token").contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "authorization_code").param("code", code).param("client_id", "SPA")
                        .param("redirect_uri", "http://127.0.0.1:3000/outra").param("code_verifier", CODE_VERIFIER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    // CA-F1-03.1 / RN-F1-03.5: com sessão SSO, o segundo pedido recebe o código sem a tela de login
    @Test
    void existingSessionGetsCodeWithoutLogin() throws Exception {
        var session = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("SPA", "openid"), "flow.user", PASSWORD)
                .session();

        String location = PkceFlow.authorize(mockMvc, session, authorizeUrl("SPA", "openid"));

        assertThat(PkceFlow.param(location, "code")).isNotBlank();
    }
}
