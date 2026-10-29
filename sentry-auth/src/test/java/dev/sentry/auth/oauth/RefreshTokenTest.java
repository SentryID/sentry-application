package dev.sentry.auth.oauth;

import static dev.sentry.auth.OAuthFixtures.CODE_VERIFIER;
import static dev.sentry.auth.OAuthFixtures.PASSWORD;
import static dev.sentry.auth.OAuthFixtures.authorizeUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.PkceFlow;
import dev.sentry.auth.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class RefreshTokenTest {

    private static final String SECRET_A = "segredo-do-sistema-a";
    private static final String SECRET_B = "segredo-do-sistema-b";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void createClientsAndUser() {
        OAuthFixtures.createClient(jdbcTemplate, "REF_A", "CONFIDENTIAL", passwordService.hash(SECRET_A), false,
                "openid profile offline_access");
        OAuthFixtures.createClient(jdbcTemplate, "REF_B", "CONFIDENTIAL", passwordService.hash(SECRET_B), false,
                "openid offline_access");
        OAuthFixtures.createUserWithRole(jdbcTemplate, "refresh.user", passwordService.hash(PASSWORD), "REF_A");
    }

    private ResultActions loginAndExchange(String scope) throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("REF_A", scope), "refresh.user", PASSWORD)
                .code();
        return PkceFlow.exchange(mockMvc, "REF_A", SECRET_A, code, CODE_VERIFIER).andExpect(status().isOk());
    }

    private String refreshToken() throws Exception {
        return PkceFlow.json(loginAndExchange("openid offline_access"), "$.refresh_token");
    }

    private void expectInvalidGrant(String clientId, String secret, String refreshToken) throws Exception {
        PkceFlow.refresh(mockMvc, clientId, secret, refreshToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    // RN-F1-06.1
    @Test
    void noRefreshTokenWithoutOfflineAccess() throws Exception {
        loginAndExchange("openid").andExpect(jsonPath("$.refresh_token").doesNotExist());
    }

    // RN-F1-06.2
    @Test
    void refreshTokenIsOpaqueAndStoredAsHash() throws Exception {
        String refreshToken = refreshToken();

        String stored = jdbcTemplate.queryForObject(
                "SELECT refresh_token_value FROM oauth2_authorization WHERE registered_client_id = 'REF_A'", String.class);
        assertThat(refreshToken).doesNotContain(".").hasSizeGreaterThanOrEqualTo(43);
        assertThat(stored).startsWith("sha256:").doesNotContain(refreshToken);
    }

    // CA-F1-06.1, RN-F1-06.3
    @Test
    void refreshRotatesTheToken() throws Exception {
        String first = refreshToken();

        ResultActions rotated = PkceFlow.refresh(mockMvc, "REF_A", SECRET_A, first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.id_token").isNotEmpty());
        String second = PkceFlow.json(rotated, "$.refresh_token");

        assertThat(second).isNotEqualTo(first);
        PkceFlow.refresh(mockMvc, "REF_A", SECRET_A, second).andExpect(status().isOk());
    }

    // CA-F1-06.2, RN-F1-06.4
    @Test
    void reusingRotatedTokenRevokesTheWholeFamily() throws Exception {
        String first = refreshToken();
        String second = PkceFlow.json(PkceFlow.refresh(mockMvc, "REF_A", SECRET_A, first), "$.refresh_token");

        expectInvalidGrant("REF_A", SECRET_A, first);
        expectInvalidGrant("REF_A", SECRET_A, second);
    }

    // CA-F1-06.3
    @Test
    void deactivatedUserCannotRefresh() throws Exception {
        String refreshToken = refreshToken();
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE username = 'refresh.user'");
        entityManager.clear();

        expectInvalidGrant("REF_A", SECRET_A, refreshToken);
    }

    // RN-F1-06.6: sistema desativado deixa de ser cliente
    @Test
    void deactivatedSystemCannotRefresh() throws Exception {
        String refreshToken = refreshToken();
        jdbcTemplate.update("UPDATE systems SET is_active = FALSE WHERE external_code = 'REF_A'");
        entityManager.clear();

        PkceFlow.refresh(mockMvc, "REF_A", SECRET_A, refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    // CA-F1-06.4, RN-F1-06.7
    @Test
    void refreshTokenIsBoundToItsClient() throws Exception {
        expectInvalidGrant("REF_B", SECRET_B, refreshToken());
    }

    // CA-F1-06.5, RN-F1-06.8
    @Test
    void revokedRefreshTokenStopsWorking() throws Exception {
        String refreshToken = refreshToken();

        mockMvc.perform(post("/oauth2/revoke").with(httpBasic("REF_A", SECRET_A))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", refreshToken).param("token_type_hint", "refresh_token"))
                .andExpect(status().isOk());

        expectInvalidGrant("REF_A", SECRET_A, refreshToken);
    }

    // RN-F1-06.5: vida máxima de 90 dias contada do login, mesmo com rotação
    @Test
    void refreshFailsAfterMaximumLifetime() throws Exception {
        String refreshToken = refreshToken();
        jdbcTemplate.update("""
                UPDATE oauth2_authorization SET authorization_code_issued_at = now() - INTERVAL '91 days'
                WHERE registered_client_id = 'REF_A'""");

        expectInvalidGrant("REF_A", SECRET_A, refreshToken);
    }
}
