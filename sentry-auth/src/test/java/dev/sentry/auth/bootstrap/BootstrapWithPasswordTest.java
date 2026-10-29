package dev.sentry.auth.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "sentry.admin.username=root.admin",
        "sentry.admin.email=root.admin@sentry.local",
        "sentry.admin.password=S3nha!forte"})
@Import(TestcontainersConfiguration.class)
class BootstrapWithPasswordTest {

    private static final List<String> TABLES = List.of("organizations", "systems", "roles", "users", "role_users");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BootstrapService bootstrapService;

    @Autowired
    private PasswordService passwordService;

    // CA-F0-03.1
    @Test
    void createsAdminWithArgon2idHash() {
        String stored = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = 'root.admin'", String.class);

        assertTrue(stored.startsWith("{argon2}$argon2id$"));
        assertTrue(passwordService.verify("S3nha!forte", stored).matches());
    }

    @Test
    void linksAdminToSentryAdminRoleOfPlatformSystem() {
        Integer links = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM role_users ru
                JOIN users u ON u.id_user = ru.id_user
                JOIN roles r ON r.id_role = ru.id_role
                JOIN systems s ON s.id_system = r.id_system
                JOIN organizations o ON o.id_organization = s.id_organization
                WHERE u.username = 'root.admin' AND r.system_role = 'SENTRY.ADMIN'
                  AND s.external_code = 'SENTRY' AND o.external_code = 'SENTRY_PLATFORM'""", Integer.class);

        assertEquals(1, links);
    }

    // CA-F0-03.2
    @Test
    void runningAgainCreatesNothing() {
        List<Integer> before = TABLES.stream().map(this::count).toList();

        bootstrapService.run(null);

        assertEquals(before, TABLES.stream().map(this::count).toList());
    }

    private Integer count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
