package dev.sentry.auth.provider.ldap;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** DN montado pelo {@code user-dn-pattern}, sem conta de serviço. */
class LdapDnPatternIntegrationTest extends LdapLoginIntegrationTestSupport {

    @DynamicPropertySource
    static void ldapProperties(DynamicPropertyRegistry registry) {
        registry.add("sentry.ldap.url", LdapLoginIntegrationTestSupport::ldapUrl);
        registry.add("sentry.ldap.base", () -> BASE);
        registry.add("sentry.ldap.user-dn-pattern", () -> "uid={0},ou=ti,ou=people");
    }
}
