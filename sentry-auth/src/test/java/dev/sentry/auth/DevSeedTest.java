package dev.sentry.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** RN-F0-03.6: os dados de exemplo só entram no perfil {@code dev} e não trazem credenciais. */
@SpringBootTest
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
class DevSeedTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devSeedLinksExampleUserToExampleSystem() {
        Integer links = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM role_users ru
                JOIN users u ON u.id_user = ru.id_user
                JOIN roles r ON r.id_role = ru.id_role
                JOIN systems s ON s.id_system = r.id_system
                WHERE u.username = 'john.doe' AND s.external_code = 'SYS001'""", Integer.class);

        assertEquals(1, links);
    }

    @Test
    void devSeedStoresNoPassword() {
        String password = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = 'john.doe'", String.class);

        assertNull(password);
    }
}
