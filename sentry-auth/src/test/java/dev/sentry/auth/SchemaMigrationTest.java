package dev.sentry.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationsAreAppliedSuccessfully() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1'", Integer.class);

        assertEquals(0, failed);
        assertEquals(1, applied);
    }

    // CA-F0-01.3
    @Test
    void roleActionReferencesActionByCode() {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org Teste', 'ORG_TEST', gen_random_uuid(), TRUE, FALSE)""");
        jdbcTemplate.update("""
                INSERT INTO systems (id_organization, external_code, uuid, is_active, is_deleted)
                SELECT id_organization, 'SYS_TEST', gen_random_uuid(), TRUE, FALSE
                FROM organizations WHERE external_code = 'ORG_TEST'""");
        jdbcTemplate.update("""
                INSERT INTO actions (cd_action, action_type, id_system, is_active, is_deleted)
                SELECT 'SENTRY.USER.VIEW', 'SEARCH', id_system, TRUE, FALSE
                FROM systems WHERE external_code = 'SYS_TEST'""");
        jdbcTemplate.update("""
                INSERT INTO roles (id_system, id_organization, system_role, uuid, is_active, is_deleted)
                SELECT id_system, id_organization, 'TEST.ROLE', gen_random_uuid(), TRUE, FALSE
                FROM systems WHERE external_code = 'SYS_TEST'""");

        int inserted = jdbcTemplate.update("""
                INSERT INTO role_actions (id_role, cd_action, is_active, is_deleted)
                SELECT id_role, 'SENTRY.USER.VIEW', TRUE, FALSE
                FROM roles WHERE system_role = 'TEST.ROLE'""");

        assertEquals(1, inserted);
    }
}
