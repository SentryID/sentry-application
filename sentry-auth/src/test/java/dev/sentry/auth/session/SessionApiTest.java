package dev.sentry.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.PkceFlow;
import dev.sentry.auth.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SessionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @BeforeEach
    void createClientAndUser() {
        OAuthFixtures.createClient(jdbcTemplate, "SYS_PAGE", "PUBLIC", null, false, "openid profile");
        OAuthFixtures.createUserWithRole(jdbcTemplate, "page.user", passwordService.hash(OAuthFixtures.PASSWORD),
                "SYS_PAGE");
    }

    // F1-04: sem sessão, o authorize manda o navegador para a tela de login do front-end
    private Cookie startAuthorization() throws Exception {
        return mockMvc.perform(get(URI.create(OAuthFixtures.authorizeUrl("SYS_PAGE", "openid"))).accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", OAuthFixtures.LOGIN_URL))
                .andReturn().getResponse().getCookie(PkceFlow.SESSION);
    }

    // CA-F1-04.1
    @Test
    void contextShowsRequestingSystem() throws Exception {
        mockMvc.perform(get("/api/v1/session/context").cookie(startAuthorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("SYS_PAGE"))
                .andExpect(jsonPath("$.systemName").value("Portal SYS_PAGE"));
    }

    @Test
    void contextWithoutFlowIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/session/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemName").doesNotExist());
    }

    // CA-F1-04.2
    @Test
    void loginWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/session/login").cookie(startAuthorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"page.user\", \"password\": \"" + OAuthFixtures.PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    // CA-F1-04.3, RN-F1-02.3
    @Test
    void wrongPasswordGetsGenericMessage() throws Exception {
        PkceFlow.sessionLogin(mockMvc, startAuthorization(), "page.user", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos"))
                .andExpect(jsonPath("$.redirectUrl").doesNotExist());
    }

    // CA-F1-03.5 (parte), RN-F1-09.3: o login devolve a URL que retoma a autorização e renova a sessão
    @Test
    void loginReturnsUrlThatResumesAuthorization() throws Exception {
        Cookie before = startAuthorization();

        var login = PkceFlow.sessionLogin(mockMvc, before, "Page.User", OAuthFixtures.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value(containsString("/oauth2/authorize")))
                .andReturn();
        Cookie after = PkceFlow.sessionAfter(login, before);

        mockMvc.perform(get("/api/v1/session/me").cookie(after))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("page.user"));
        assertThat(after.getValue()).isNotEqualTo(before.getValue());
    }

    @Test
    void meWithoutSessionIsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/session/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    // RN-F1-09.1 (fim da sessão pela tela do front-end)
    @Test
    void logoutEndsSession() throws Exception {
        Cookie session = PkceFlow.loginAndAuthorize(mockMvc, OAuthFixtures.authorizeUrl("SYS_PAGE", "openid"),
                "page.user", OAuthFixtures.PASSWORD).session();

        mockMvc.perform(post("/api/v1/session/logout").cookie(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/session/me").cookie(session)).andExpect(jsonPath("$.authenticated").value(false));
    }

    // sem telas: nenhuma resposta é HTML
    @Test
    void unknownPathIsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/login").accept("text/html")).andExpect(status().isUnauthorized());
    }
}
