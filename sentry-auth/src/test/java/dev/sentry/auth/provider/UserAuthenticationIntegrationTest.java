package dev.sentry.auth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserAuthenticationIntegrationTest {

    private static final String PASSWORD = "S3nha!forte";

    @Autowired
    private UserAuthenticationProvider provider;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createUserWithRole() {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org Login', 'ORG_LOGIN', gen_random_uuid(), TRUE, FALSE)""");
        jdbcTemplate.update("""
                INSERT INTO systems (id_organization, external_code, uuid, is_active, is_deleted)
                SELECT id_organization, 'SYS_LOGIN', gen_random_uuid(), TRUE, FALSE
                FROM organizations WHERE external_code = 'ORG_LOGIN'""");
        jdbcTemplate.update("""
                INSERT INTO roles (id_system, id_organization, system_role, uuid, is_active, is_deleted)
                SELECT id_system, id_organization, 'SYS_LOGIN.USER', gen_random_uuid(), TRUE, FALSE
                FROM systems WHERE external_code = 'SYS_LOGIN'""");
        jdbcTemplate.update("""
                INSERT INTO users (uuid, name, authentication_type, username, password, email, is_active, is_deleted)
                VALUES (gen_random_uuid(), 'John Doe', 'DATABASE', 'john.doe', ?, 'john@sentry.dev', TRUE, FALSE)""",
                passwordService.hash(PASSWORD));
        jdbcTemplate.update("""
                INSERT INTO role_users (id_role, id_user, is_active, is_deleted)
                SELECT r.id_role, u.id_user, TRUE, FALSE FROM roles r, users u
                WHERE r.system_role = 'SYS_LOGIN.USER' AND u.username = 'john.doe'""");
    }

    private String login(String username) {
        var token = UsernamePasswordAuthenticationToken.unauthenticated(username, PASSWORD);
        token.setDetails(new LoginAttempt("SYS_LOGIN", Channel.PKCE, "127.0.0.1", "JUnit"));
        return provider.authenticate(token).getName();
    }

    // CA-F1-02.1, CA-F1-02.4, CA-F1-05.5
    @Test
    void authenticatesIgnoringCaseAndReturnsUserUuid() {
        String uuid = jdbcTemplate.queryForObject("SELECT uuid::text FROM users WHERE username = 'john.doe'", String.class);

        assertEquals(uuid, login("John.Doe"));
    }

    // RN-F1-02.1: dois usernames que só diferem na caixa não podem coexistir
    @Test
    void databaseRejectsUsernameDifferingOnlyByCase() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO users (uuid, name, authentication_type, username, email, is_active, is_deleted)
                VALUES (gen_random_uuid(), 'Outro', 'DATABASE', 'JOHN.DOE', 'outro@sentry.dev', TRUE, FALSE)"""));
    }
}
