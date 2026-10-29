package dev.sentry.auth.oauth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.TestcontainersConfiguration;
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
class CorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createClient() {
        OAuthFixtures.createClient(jdbcTemplate, "CORS_SPA", "PUBLIC", null, false, "openid");
    }

    private ResultActions preflight(String path, String origin) throws Exception {
        return mockMvc.perform(options(path).header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"));
    }

    // um SPA cadastrado consegue chamar o token endpoint e o userinfo do navegador
    @Test
    void registeredOriginIsAllowed() throws Exception {
        preflight("/oauth2/token", "http://127.0.0.1:3000")
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"));
        preflight("/userinfo", "http://127.0.0.1:3000")
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"));
    }

    @Test
    void unknownOriginIsRejected() throws Exception {
        preflight("/oauth2/token", "https://malicioso.exemplo.com")
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
