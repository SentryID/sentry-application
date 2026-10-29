package dev.sentry.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
                "SELECT count(*) FROM flyway_schema_history WHERE version IN ('1', '2')", Integer.class);

        assertEquals(0, failed);
        assertEquals(2, applied);
    }

    // tarefa 1.1: tabelas da Fase 1 (F1-01, F1-03, F1-09)
    @Test
    void phaseOneTablesExist() {
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name IN ('system_redirect_uris', 'oauth2_authorization',
                                     'oauth2_authorization_consent', 'spring_session', 'spring_session_attributes')""",
                Integer.class);

        assertEquals(5, tables);
    }

    // RN-F1-01.2: só PUBLIC ou CONFIDENTIAL
    @Test
    void systemsRejectUnknownClientType() {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org Tipo', 'ORG_TYPE', gen_random_uuid(), TRUE, FALSE)""");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO systems (id_organization, external_code, client_type, uuid, is_active, is_deleted)
                SELECT id_organization, 'SYS_TYPE', 'OTHER', gen_random_uuid(), TRUE, FALSE
                FROM organizations WHERE external_code = 'ORG_TYPE'"""));
    }

    // RN-F0-02.5
    @Test
    void usersTableHasNoSaltColumn() {
        Integer saltColumns = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'users' AND column_name = 'salt'""", Integer.class);

        assertEquals(0, saltColumns);
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
