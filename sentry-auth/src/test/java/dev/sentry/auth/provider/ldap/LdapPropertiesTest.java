package dev.sentry.auth.provider.ldap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LdapPropertiesTest {

    private static LdapProperties withFilter(String filter) {
        return new LdapProperties("ldaps://ad:636", "DC=exemplo,DC=com", "uid={0},ou=people", "OU=Usuarios", filter,
                "svc-sentry@exemplo.com", "svc-s3nha", Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    @Test
    void usesSearchOnlyWithFilter() {
        assertTrue(withFilter("(sAMAccountName={0})").usesSearch());
        assertFalse(withFilter(null).usesSearch());
        assertFalse(withFilter(" ").usesSearch());
    }

    // RN-F0-02.6
    @Test
    void doesNotExposeManagerPasswordInToString() {
        assertFalse(withFilter("(sAMAccountName={0})").toString().contains("svc-s3nha"));
    }
}
