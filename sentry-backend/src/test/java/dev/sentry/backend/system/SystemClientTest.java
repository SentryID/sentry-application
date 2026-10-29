package dev.sentry.backend.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.backend.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
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
class SystemClientTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EntityManager entityManager;

    private Long idOrganization;

    @BeforeEach
    void createOrganization() {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org Cliente', 'ORG_CLIENT', gen_random_uuid(), TRUE, FALSE)""");
        idOrganization = jdbcTemplate.queryForObject(
                "SELECT id_organization FROM organizations WHERE external_code = 'ORG_CLIENT'", Long.class);
    }

    private String createSystem(String clientType, String redirectUri) throws Exception {
        return mockMvc.perform(post("/systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idOrganization": %d, "externalCode": "SYS_CLIENT", "description": "Portal",
                                 "scopes": "openid profile", "clientType": "%s", "redirectUris": ["%s"],
                                 "postLogoutRedirectUris": [], "isTrusted": false, "isActive": true}
                                """.formatted(idOrganization, clientType, redirectUri)))
                .andReturn().getResponse().getContentAsString();
    }

    private String storedHash() {
        // o teste roda numa transação só; sem o flush, o UPDATE da rotação ainda não chegou ao banco
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT client_secret_hash FROM systems WHERE external_code = 'SYS_CLIENT'", String.class);
    }

    // CA-F1-01.1
    @Test
    void confidentialSystemGetsSecretOnceAndDatabaseKeepsOnlyHash() throws Exception {
        String body = createSystem("CONFIDENTIAL", "https://portal.sentry.dev/cb");
        String secret = JsonPath.read(body, "$.clientSecret");

        assertThat(JsonPath.<String>read(body, "$.clientId")).isEqualTo("SYS_CLIENT");
        assertThat(storedHash()).startsWith("{argon2}$argon2id$").doesNotContain(secret);
        assertThat(passwordService.verify(secret, storedHash()).matches()).isTrue();

        Long id = jdbcTemplate.queryForObject(
                "SELECT id_system FROM systems WHERE external_code = 'SYS_CLIENT'", Long.class);
        mockMvc.perform(get("/systems/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientType").value("CONFIDENTIAL"))
                .andExpect(jsonPath("$.redirectUris[0]").value("https://portal.sentry.dev/cb"))
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andExpect(jsonPath("$.clientSecretHash").doesNotExist());
    }

    // CA-F1-01.2
    @Test
    void httpRedirectUriIsRejectedWithMessageInPortuguese() throws Exception {
        mockMvc.perform(post("/systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idOrganization": %d, "externalCode": "SYS_CLIENT", "description": "Portal",
                                 "clientType": "PUBLIC", "redirectUris": ["http://app.exemplo.com/cb"],
                                 "isTrusted": false, "isActive": true}
                                """.formatted(idOrganization)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith(
                        "URI de redirecionamento inválida: http://app.exemplo.com/cb")));
    }

    // CA-F1-01.4
    @Test
    void rotatedSecretInvalidatesOldOneImmediately() throws Exception {
        String oldSecret = JsonPath.read(createSystem("CONFIDENTIAL", "https://portal.sentry.dev/cb"), "$.clientSecret");
        Long id = jdbcTemplate.queryForObject(
                "SELECT id_system FROM systems WHERE external_code = 'SYS_CLIENT'", Long.class);

        String body = mockMvc.perform(post("/systems/{id}/client-secret", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newSecret = JsonPath.read(body, "$.clientSecret");

        assertThat(newSecret).isNotEqualTo(oldSecret);
        assertThat(passwordService.verify(oldSecret, storedHash()).matches()).isFalse();
        assertThat(passwordService.verify(newSecret, storedHash()).matches()).isTrue();
    }
}
