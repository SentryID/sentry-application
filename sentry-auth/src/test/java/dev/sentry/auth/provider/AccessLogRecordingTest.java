package dev.sentry.auth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.TestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Sem {@code @Transactional}: o registro grava em transação própria e precisa ver os dados já gravados. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccessLogRecordingTest {

    @Autowired
    private DatabaseAuthenticationProvider provider;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createUser() {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org Log', 'ORG_LOG', gen_random_uuid(), TRUE, FALSE)""");
        jdbcTemplate.update("""
                INSERT INTO systems (id_organization, external_code, uuid, is_active, is_deleted)
                SELECT id_organization, 'SYS_LOG', gen_random_uuid(), TRUE, FALSE
                FROM organizations WHERE external_code = 'ORG_LOG'""");
        jdbcTemplate.update("""
                INSERT INTO users (uuid, name, authentication_type, username, password, email, is_active, is_deleted)
                VALUES (gen_random_uuid(), 'Log Doe', 'DATABASE', 'log.doe', ?, 'log@sentry.dev', TRUE, FALSE)""",
                passwordService.hash("S3nha!forte"));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM access_logs WHERE id_system IN (SELECT id_system FROM systems WHERE external_code = 'SYS_LOG')");
        jdbcTemplate.update("DELETE FROM users WHERE username = 'log.doe'");
        jdbcTemplate.update("DELETE FROM systems WHERE external_code = 'SYS_LOG'");
        jdbcTemplate.update("DELETE FROM organizations WHERE external_code = 'ORG_LOG'");
    }

    private void attempt(String username, String password) {
        var token = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        token.setDetails(new LoginAttempt("SYS_LOG", Channel.PKCE, "203.0.113.7", "JUnit"));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(token));
    }

    private Map<String, Object> lastLog() {
        return jdbcTemplate.queryForMap("""
                SELECT a.* FROM access_logs a JOIN systems s ON s.id_system = a.id_system
                WHERE s.external_code = 'SYS_LOG' ORDER BY a.id_access_log DESC LIMIT 1""");
    }

    // CA-F1-10.1
    @Test
    void wrongPasswordIsRecordedWithUserSystemAndIp() {
        attempt("log.doe", "errada");

        Map<String, Object> row = lastLog();
        assertEquals("INVALID_CREDENTIALS", row.get("attempt_result"));
        assertEquals(jdbcTemplate.queryForObject("SELECT id_user FROM users WHERE username = 'log.doe'", Long.class),
                row.get("id_user"));
        assertEquals("203.0.113.7", row.get("ip_host"));
        assertEquals("PKCE", row.get("channel"));
    }

    // CA-F1-10.2
    @Test
    void unknownUserIsRecordedWithAttemptedUsername() {
        attempt("fulano", "qualquer");

        Map<String, Object> row = lastLog();
        assertEquals("USER_NOT_FOUND", row.get("attempt_result"));
        assertEquals("fulano", row.get("attempted_username"));
        assertNull(row.get("id_user"));
    }
}
