package dev.sentry.auth.oauth;

import static dev.sentry.auth.OAuthFixtures.CODE_VERIFIER;
import static dev.sentry.auth.OAuthFixtures.PASSWORD;
import static dev.sentry.auth.OAuthFixtures.authorizeUrl;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UserInfoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EntityManager entityManager;

    private String userUuid;

    @BeforeEach
    void createClientAndUser() {
        OAuthFixtures.createClient(jdbcTemplate, "INFO", "PUBLIC", null, false, "openid profile email");
        userUuid = OAuthFixtures.createUserWithRole(jdbcTemplate, "info.user", passwordService.hash(PASSWORD), "INFO");
    }

    private String accessToken(String scope) throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("INFO", scope), "info.user", PASSWORD).code();
        return PkceFlow.json(PkceFlow.exchange(mockMvc, "INFO", null, code, CODE_VERIFIER), "$.access_token");
    }

    private ResultActions userInfo(String accessToken) throws Exception {
        return mockMvc.perform(get("/userinfo").header("Authorization", "Bearer " + accessToken));
    }

    // CA-F1-08.1
    @Test
    void profileScopeReturnsNameWithoutEmail() throws Exception {
        userInfo(accessToken("openid profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(userUuid))
                .andExpect(jsonPath("$.name").value("Nome info.user"))
                .andExpect(jsonPath("$.preferred_username").value("info.user"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void emailScopeReturnsEmail() throws Exception {
        userInfo(accessToken("openid email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("info.user@sentry.dev"))
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    // CA-F1-08.2
    @Test
    void tokenWithoutOpenidIsInsufficientScope() throws Exception {
        userInfo(accessToken("profile"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", containsString("insufficient_scope")));
    }

    // CA-F1-08.3, RN-F1-08.3
    @Test
    void nameChangedAfterLoginIsReturned() throws Exception {
        String token = accessToken("openid profile");
        jdbcTemplate.update("UPDATE users SET name = 'Nome Novo' WHERE username = 'info.user'");
        entityManager.clear();

        userInfo(token).andExpect(jsonPath("$.name").value("Nome Novo"));
    }

    // RN-F1-08.4
    @Test
    void inactiveUserGetsInvalidToken() throws Exception {
        String token = accessToken("openid profile");
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE username = 'info.user'");
        entityManager.clear();

        userInfo(token)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""));
    }
}
