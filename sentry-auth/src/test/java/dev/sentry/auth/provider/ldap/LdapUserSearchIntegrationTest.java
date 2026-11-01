package dev.sentry.auth.provider.ldap;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Active Directory: a conta de serviço busca o usuário pelo filtro em toda a árvore abaixo da base da busca e o bind
 * usa o DN encontrado. O {@code uid} faz o papel do {@code sAMAccountName}, que não existe no esquema do servidor em
 * memória.
 */
class LdapUserSearchIntegrationTest extends LdapLoginIntegrationTestSupport {

    @DynamicPropertySource
    static void ldapProperties(DynamicPropertyRegistry registry) {
        registry.add("sentry.ldap.url", LdapLoginIntegrationTestSupport::ldapUrl);
        registry.add("sentry.ldap.base", () -> BASE);
        registry.add("sentry.ldap.user-search-base", () -> "ou=people");
        registry.add("sentry.ldap.user-search-filter", () -> "(&(objectClass=person)(uid={0}))");
        registry.add("sentry.ldap.manager-dn", () -> MANAGER_DN);
        registry.add("sentry.ldap.manager-password", () -> MANAGER_PASSWORD);
    }
}
