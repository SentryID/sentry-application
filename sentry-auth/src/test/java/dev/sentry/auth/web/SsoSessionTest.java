package dev.sentry.auth.web;

import static dev.sentry.auth.OAuthFixtures.CODE_VERIFIER;
import static dev.sentry.auth.OAuthFixtures.LOGOUT_URI;
import static dev.sentry.auth.OAuthFixtures.PASSWORD;
import static dev.sentry.auth.OAuthFixtures.authorizeUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.PkceFlow;
import dev.sentry.auth.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriComponentsBuilder;

/** Sem {@code @Transactional}: a sessão SSO é gravada pelo Spring Session em transação própria. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SsoSessionTest {

    private static final String SECRET = "segredo-sso-a";
    private static final List<String> CLIENTS = List.of("SSO_A", "SSO_B", "SSO_C");
    private static final List<String> USERS = List.of("sso.user");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @BeforeEach
    void createClientsAndUser() {
        OAuthFixtures.cleanUp(jdbcTemplate, CLIENTS, USERS);
        OAuthFixtures.createClient(jdbcTemplate, "SSO_A", "CONFIDENTIAL", passwordService.hash(SECRET), false,
                "openid profile offline_access");
        OAuthFixtures.createClient(jdbcTemplate, "SSO_B", "PUBLIC", null, false, "openid profile");
        OAuthFixtures.createClient(jdbcTemplate, "SSO_C", "PUBLIC", null, false, "openid profile");
        OAuthFixtures.createUserWithRole(jdbcTemplate, "sso.user", passwordService.hash(PASSWORD), "SSO_A");
        OAuthFixtures.grantRole(jdbcTemplate, "sso.user", "SSO_B");
    }

    @AfterEach
    void cleanUp() {
        OAuthFixtures.cleanUp(jdbcTemplate, CLIENTS, USERS);
    }

    private PkceFlow.Result loginOnA() throws Exception {
        return PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("SSO_A", "openid offline_access"), "sso.user", PASSWORD);
    }

    private ResultActions exchangeOnA(String code) throws Exception {
        return PkceFlow.exchange(mockMvc, "SSO_A", SECRET, code, CODE_VERIFIER).andExpect(status().isOk());
    }

    // o /connect/logout lê os parâmetros do GET na query string
    private static URI logoutUrl(String idTokenHint, String postLogoutRedirectUri, String state) {
        var builder = UriComponentsBuilder.fromPath("/connect/logout").queryParam("id_token_hint", idTokenHint)
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri);
        if (state != null) {
            builder.queryParam("state", state);
        }
        return builder.encode().build().toUri();
    }

    private boolean asksForPassword(Cookie session, String clientId) throws Exception {
        return "/login".equals(PkceFlow.authorize(mockMvc, session, authorizeUrl(clientId, "openid")));
    }

    // RN-F1-09.1, RN-F1-09.3, RN-F1-09.4: cookie HttpOnly e SameSite=Lax, renovado no login, sessão no PostgreSQL
    @Test
    void loginCreatesPersistentSessionWithNewId() throws Exception {
        var start = mockMvc.perform(get(URI.create(authorizeUrl("SSO_A", "openid"))).accept("text/html"))
                .andReturn();
        Cookie before = start.getResponse().getCookie(PkceFlow.SESSION);

        Cookie after = loginOnA().session();

        assertThat(before.isHttpOnly()).isTrue();
        assertThat(before.getAttribute("SameSite")).isEqualToIgnoringCase("Lax");
        assertThat(after.getValue()).isNotEqualTo(before.getValue());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM spring_session WHERE principal_name = "
                + "(SELECT uuid::text FROM users WHERE username = 'sso.user')", Integer.class)).isEqualTo(1);
    }

    // CA-F1-09.1
    @Test
    void secondSystemGetsCodeWithoutPassword() throws Exception {
        Cookie session = loginOnA().session();

        String location = PkceFlow.authorize(mockMvc, session, authorizeUrl("SSO_B", "openid"));

        assertThat(PkceFlow.param(location, "code")).isNotBlank();
    }

    // RN-F1-02.5 com SSO: sem papel no sistema, a sessão não basta para receber token
    @Test
    void sessionDoesNotGrantSystemWithoutRole() throws Exception {
        Cookie session = loginOnA().session();
        String code = PkceFlow.param(PkceFlow.authorize(mockMvc, session, authorizeUrl("SSO_C", "openid")), "code");

        PkceFlow.exchange(mockMvc, "SSO_C", null, code, CODE_VERIFIER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    // CA-F1-09.2, RN-F1-09.6
    @Test
    void logoutWithRegisteredUriEndsSessionRevokesTokensAndReturnsState() throws Exception {
        PkceFlow.Result login = loginOnA();
        ResultActions tokens = exchangeOnA(login.code());

        String location = mockMvc.perform(get(logoutUrl(PkceFlow.json(tokens, "$.id_token"), LOGOUT_URI, "fim"))
                        .cookie(login.session()))
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(location).isEqualTo(LOGOUT_URI + "?state=fim");
        assertThat(asksForPassword(login.session(), "SSO_A")).isTrue();
        PkceFlow.refresh(mockMvc, "SSO_A", SECRET, PkceFlow.json(tokens, "$.refresh_token"))
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    // CA-F1-09.3
    @Test
    void logoutWithUnregisteredUriEndsSessionWithoutRedirect() throws Exception {
        PkceFlow.Result login = loginOnA();
        String idToken = PkceFlow.json(exchangeOnA(login.code()), "$.id_token");

        String location = mockMvc.perform(get(logoutUrl(idToken, "https://malicioso.exemplo.com/", null))
                        .cookie(login.session()))
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(location).isEqualTo(SsoLogout.LOGGED_OUT_PATH);
        assertThat(asksForPassword(login.session(), "SSO_A")).isTrue();
        mockMvc.perform(get(SsoLogout.LOGGED_OUT_PATH)).andExpect(status().isOk());
    }

    // CA-F1-09.4
    @Test
    void idleSessionAsksForPasswordAgain() throws Exception {
        Cookie session = loginOnA().session();
        long thirtyOneMinutesAgo = System.currentTimeMillis() - 31 * 60 * 1000L;
        jdbcTemplate.update("UPDATE spring_session SET last_access_time = ?, expiry_time = ? WHERE principal_name = "
                + "(SELECT uuid::text FROM users WHERE username = 'sso.user')", thirtyOneMinutesAgo,
                thirtyOneMinutesAgo + 30 * 60 * 1000L);

        assertThat(asksForPassword(session, "SSO_B")).isTrue();
    }

    // RN-F1-09.2: vida máxima de 12 horas, mesmo com uso
    @Test
    void sessionOlderThanMaxAgeAsksForPasswordAgain() throws Exception {
        Cookie session = loginOnA().session();
        jdbcTemplate.update("UPDATE spring_session SET creation_time = ? WHERE principal_name = "
                + "(SELECT uuid::text FROM users WHERE username = 'sso.user')",
                System.currentTimeMillis() - 13 * 60 * 60 * 1000L);

        assertThat(asksForPassword(session, "SSO_B")).isTrue();
    }

    // CA-F1-09.5
    @Test
    void promptLoginAsksForPasswordEvenWithSession() throws Exception {
        Cookie session = loginOnA().session();

        String location = PkceFlow.authorize(mockMvc, session, authorizeUrl("SSO_B", "openid", "prompt", "login"));

        assertThat(location).contains("/oauth2/authorize").doesNotContain("prompt");
        assertThat(asksForPassword(session, "SSO_B")).isTrue();
    }
}
