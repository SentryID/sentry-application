package dev.sentry.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.backend.TestcontainersConfiguration;
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
class UserPasswordStorageTest {

    private static final String PASSWORD = "S3nha!forte";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long createUser() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Maria Silva", "authenticationType": "LOCAL", "username": "maria.silva",
                                 "password": "%s", "email": "maria@sentry.dev", "isActive": true}
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject("SELECT id_user FROM users WHERE username = 'maria.silva'", Long.class);
    }

    // CA-F0-02.1
    @Test
    void createdUserHasArgon2idHashInDatabase() throws Exception {
        createUser();

        String stored = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = 'maria.silva'", String.class);

        assertThat(stored).startsWith("{argon2}$argon2id$").doesNotContain(PASSWORD);
    }

    // CA-F0-02.4
    @Test
    void userQueriesDoNotExposePasswordOrSalt() throws Exception {
        Long id = createUser();

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria.silva"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.salt").doesNotExist());

        mockMvc.perform(get("/users").param("page", "0").param("pageSize", "10").param("username", "maria.silva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("maria.silva"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].salt").doesNotExist());
    }
}
