package dev.sentry.auth.provider.ldap;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

/**
 * Liga o LDAP só quando {@code sentry.ldap.url} está preenchida (RN-F1-02.7). Sem o {@link LdapCredentialVerifier},
 * o provedor trata usuários {@code LDAP} como sem verificador e responde {@code INVALID_CREDENTIALS}.
 */
@Configuration
@EnableConfigurationProperties(LdapProperties.class)
@ConditionalOnExpression("!'${sentry.ldap.url:}'.isBlank()")
class LdapConfiguration {

    @Bean
    DefaultSpringSecurityContextSource ldapContextSource(LdapProperties properties) {
        var contextSource = new DefaultSpringSecurityContextSource(properties.url() + "/" + properties.base());
        if (properties.usesSearch()) {
            // a busca é feita pela conta de serviço; o bind de cada login usa o DN e a senha do próprio usuário
            contextSource.setUserDn(properties.managerDn());
            contextSource.setPassword(properties.managerPassword());
        }
        // um LDAP lento não pode segurar as threads do login
        contextSource.setBaseEnvironmentProperties(Map.of(
                "com.sun.jndi.ldap.connect.timeout", String.valueOf(properties.connectTimeout().toMillis()),
                "com.sun.jndi.ldap.read.timeout", String.valueOf(properties.readTimeout().toMillis())));
        return contextSource;
    }

    @Bean
    LdapCredentialVerifier ldapCredentialVerifier(DefaultSpringSecurityContextSource ldapContextSource,
            LdapProperties properties) {
        var authenticator = new BindAuthenticator(ldapContextSource);
        // o username é escapado tanto no filtro quanto no DN (evita LDAP injection)
        if (properties.usesSearch()) {
            authenticator.setUserSearch(new FilterBasedLdapUserSearch(properties.userSearchBase(),
                    properties.userSearchFilter(), ldapContextSource));
        } else {
            authenticator.setUserDnPatterns(new String[] {properties.userDnPattern()});
        }
        return new LdapCredentialVerifier(authenticator);
    }
}
