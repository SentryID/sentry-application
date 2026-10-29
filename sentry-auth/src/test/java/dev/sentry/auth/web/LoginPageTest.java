package dev.sentry.auth.web;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.TestcontainersConfiguration;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class LoginPageTest {

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

    private MockHttpSession startAuthorization() throws Exception {
        return (MockHttpSession) mockMvc.perform(get(URI.create(OAuthFixtures.authorizeUrl("SYS_PAGE", "openid")))
                        .accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andReturn().getRequest().getSession();
    }

    // CA-F1-04.1
    @Test
    void loginPageShowsRequestingSystem() throws Exception {
        MockHttpSession session = startAuthorization();

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Para continuar em Portal SYS_PAGE")))
                .andExpect(content().string(not(containsString("Esqueci"))));
    }

    // CA-F1-04.2
    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/login").param("username", "page.user").param("password", OAuthFixtures.PASSWORD))
                .andExpect(status().isForbidden());
    }

    // CA-F1-04.3
    @Test
    void wrongPasswordKeepsUsernameAndClearsPassword() throws Exception {
        MockHttpSession session = startAuthorization();

        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "page.user").param("password", "errada"))
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", "").session(session))
                .andExpect(content().string(allOf(
                        containsString("Usuário ou senha inválidos"),
                        containsString("value=\"page.user\""),
                        not(containsString("errada")))));
    }

    // CA-F1-03.5 (parte): login certo volta ao fluxo de autorização
    @Test
    void successfulLoginResumesAuthorization() throws Exception {
        MockHttpSession session = startAuthorization();

        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "Page.User").param("password", OAuthFixtures.PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/oauth2/authorize")));
    }

    // CA-F1-04.4, RN-F1-04.6
    @Test
    void pageCannotBeFramedNorCached() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    // RN-F1-03.3: client_id desconhecido mostra a página de erro do Sentry, sem redirecionar
    @Test
    void unknownClientShowsErrorPage() throws Exception {
        mockMvc.perform(get(URI.create(OAuthFixtures.authorizeUrl("NAO_EXISTE", "openid"))).accept("text/html"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("client_id")))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void cssIsPublic() throws Exception {
        mockMvc.perform(get("/css/sentry.css")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/css")));
    }
}
