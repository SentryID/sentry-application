package dev.sentry.auth.provider.ldap;

import static dev.sentry.auth.provider.ldap.LdapLoginIntegrationTestSupport.BASE;
import static dev.sentry.auth.provider.ldap.LdapLoginIntegrationTestSupport.MANAGER_DN;
import static dev.sentry.auth.provider.ldap.LdapLoginIntegrationTestSupport.MANAGER_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sentry.api.domain.user.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Beans do {@link LdapConfiguration} contra o servidor em memória, sem contexto Spring nem banco (RN-F1-02.7). */
class LdapConfigurationTest {

    private static final String LDAP_PASSWORD = "S3nha!ldap";

    private static LdapCredentialVerifier verifier(LdapProperties properties) {
        var configuration = new LdapConfiguration();
        var contextSource = configuration.ldapContextSource(properties);
        contextSource.afterPropertiesSet();
        return configuration.ldapCredentialVerifier(contextSource, properties);
    }

    private static LdapProperties properties(String userDnPattern, String searchFilter, String managerPassword) {
        return new LdapProperties(LdapLoginIntegrationTestSupport.ldapUrl(), BASE, userDnPattern, "ou=people",
                searchFilter, MANAGER_DN, managerPassword, Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    private static User maria() {
        var user = new User();
        user.setUsername("maria.silva");
        return user;
    }

    // Active Directory: busca pela conta de serviço numa OU aninhada
    @Test
    void searchFindsUserInNestedOuAndBindsWithItsPassword() {
        var verifier = verifier(properties("ignorado", "(&(objectClass=person)(uid={0}))", MANAGER_PASSWORD));

        assertTrue(verifier.verify(maria(), LDAP_PASSWORD).matches());
        assertFalse(verifier.verify(maria(), "senha-errada").matches());
    }

    @Test
    void searchRejectsUnknownUser() {
        var verifier = verifier(properties("ignorado", "(&(objectClass=person)(uid={0}))", MANAGER_PASSWORD));
        var unknown = new User();
        unknown.setUsername("ninguem");

        assertFalse(verifier.verify(unknown, LDAP_PASSWORD).matches());
    }

    // com a senha da conta de serviço errada a busca falha e o login é recusado, sem exceção
    @Test
    void searchWithWrongManagerPasswordRejects() {
        var verifier = verifier(properties("ignorado", "(&(objectClass=person)(uid={0}))", "errada"));

        assertFalse(verifier.verify(maria(), LDAP_PASSWORD).matches());
    }

    // o filtro escapa o username: um curinga não acha ninguém
    @Test
    void searchEscapesUsername() {
        var verifier = verifier(properties("ignorado", "(&(objectClass=person)(uid={0}))", MANAGER_PASSWORD));
        var wildcard = new User();
        wildcard.setUsername("*");

        assertFalse(verifier.verify(wildcard, LDAP_PASSWORD).matches());
    }

    @Test
    void dnPatternBindsWithoutServiceAccount() {
        var verifier = verifier(properties("uid={0},ou=ti,ou=people", null, null));

        assertTrue(verifier.verify(maria(), LDAP_PASSWORD).matches());
        assertFalse(verifier.verify(maria(), "senha-errada").matches());
    }
}
