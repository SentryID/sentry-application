package dev.sentry.auth.provider.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.OperationType;
import com.unboundid.ldif.LDIFReader;
import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.auth.TestcontainersConfiguration;
import dev.sentry.auth.provider.LoginAttempt;
import dev.sentry.auth.provider.UserAuthenticationProvider;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login de um usuário {@code LDAP} contra um servidor LDAP em memória (RN-F1-02.7). As subclasses só mudam como o DN
 * do usuário é achado. O servidor só aceita busca autenticada, como o Active Directory.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
abstract class LdapLoginIntegrationTestSupport {

    static final String BASE = "dc=sentry,dc=dev";
    static final String MANAGER_DN = "cn=svc-sentry,dc=sentry,dc=dev";
    static final String MANAGER_PASSWORD = "svc-s3nha";
    private static final String LDAP_PASSWORD = "S3nha!ldap";
    // um servidor para toda a JVM de testes; sem shutdown, porque as subclasses o compartilham
    static final InMemoryDirectoryServer LDAP = startLdap();

    @Autowired
    private UserAuthenticationProvider provider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static InMemoryDirectoryServer startLdap() {
        try (var ldif = new LDIFReader(LdapLoginIntegrationTestSupport.class.getResourceAsStream("/ldap/users.ldif"))) {
            var config = new InMemoryDirectoryServerConfig(BASE);
            config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 0));
            config.addAdditionalBindCredentials(MANAGER_DN, MANAGER_PASSWORD);
            config.setAuthenticationRequiredOperationTypes(EnumSet.of(OperationType.SEARCH));
            var server = new InMemoryDirectoryServer(config);
            server.importFromLDIF(true, ldif);
            server.startListening();
            return server;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String ldapUrl() {
        return "ldap://localhost:" + LDAP.getListenPort();
    }

    @BeforeEach
    void createLdapUserWithRole() {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org LDAP', 'ORG_LDAP', gen_random_uuid(), TRUE, FALSE)""");
        jdbcTemplate.update("""
                INSERT INTO systems (id_organization, external_code, uuid, is_active, is_deleted)
                SELECT id_organization, 'SYS_LDAP', gen_random_uuid(), TRUE, FALSE
                FROM organizations WHERE external_code = 'ORG_LDAP'""");
        jdbcTemplate.update("""
                INSERT INTO roles (id_system, id_organization, system_role, uuid, is_active, is_deleted)
                SELECT id_system, id_organization, 'SYS_LDAP.USER', gen_random_uuid(), TRUE, FALSE
                FROM systems WHERE external_code = 'SYS_LDAP'""");
        jdbcTemplate.update("""
                INSERT INTO users (uuid, name, authentication_type, username, email, is_active, is_deleted)
                VALUES (gen_random_uuid(), 'Maria Silva', 'LDAP', 'maria.silva', 'maria@sentry.dev', TRUE, FALSE)""");
        jdbcTemplate.update("""
                INSERT INTO role_users (id_role, id_user, is_active, is_deleted)
                SELECT r.id_role, u.id_user, TRUE, FALSE FROM roles r, users u
                WHERE r.system_role = 'SYS_LDAP.USER' AND u.username = 'maria.silva'""");
    }

    private String login(String password) {
        var token = UsernamePasswordAuthenticationToken.unauthenticated("maria.silva", password);
        token.setDetails(new LoginAttempt("SYS_LDAP", Channel.PKCE, "127.0.0.1", "JUnit"));
        return provider.authenticate(token).getName();
    }

    private String lastAttemptResult() {
        return jdbcTemplate.queryForObject("""
                SELECT a.attempt_result FROM access_logs a JOIN users u ON u.id_user = a.id_user
                WHERE u.username = 'maria.silva' ORDER BY a.id_access_log DESC LIMIT 1""", String.class);
    }

    @Test
    void authenticatesWithLdapPasswordAndReturnsUserUuid() {
        String uuid = jdbcTemplate.queryForObject("SELECT uuid::text FROM users WHERE username = 'maria.silva'",
                String.class);

        assertEquals(uuid, login(LDAP_PASSWORD));
        assertEquals("SUCCESS", lastAttemptResult());
    }

    @Test
    void rejectsWrongLdapPassword() {
        var exception = assertThrows(BadCredentialsException.class, () -> login("senha-errada"));

        assertEquals(UserAuthenticationProvider.INVALID_CREDENTIALS_MESSAGE, exception.getMessage());
        assertEquals("INVALID_CREDENTIALS", lastAttemptResult());
    }

    // o LDAP confere a senha, mas quem decide se o usuário pode entrar é o Sentry
    @Test
    void rejectsUserInactiveInSentryEvenWithRightLdapPassword() {
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE username = 'maria.silva'");

        assertThrows(BadCredentialsException.class, () -> login(LDAP_PASSWORD));
        assertEquals("USER_INACTIVE", lastAttemptResult());
    }
}
