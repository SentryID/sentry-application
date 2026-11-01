package dev.sentry.auth.provider.ldap;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.User.AuthenticationType;
import dev.sentry.auth.provider.CredentialVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.LdapAuthenticator;

/** Confere a senha de usuários {@code LDAP} com um bind no servidor LDAP (RN-F1-02.7). */
public class LdapCredentialVerifier implements CredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(LdapCredentialVerifier.class);

    private final LdapAuthenticator ldapAuthenticator;

    public LdapCredentialVerifier(LdapAuthenticator ldapAuthenticator) {
        this.ldapAuthenticator = ldapAuthenticator;
    }

    @Override
    public AuthenticationType type() {
        return AuthenticationType.LDAP;
    }

    /**
     * Faz o bind com o DN do usuário e a senha digitada. Senha vazia é recusada antes do bind, porque muitos
     * servidores aceitam bind sem senha como anônimo. Servidor fora do ar também recusa, sem derrubar o login.
     *
     * @param user usuário {@code LDAP}; o DN é montado com o username cadastrado no Sentry, não com o digitado
     * @param password senha em claro
     * @return aceita quando o bind dá certo
     */
    @Override
    public Verification verify(User user, String password) {
        if (password == null || password.isEmpty()) {
            return Verification.rejected();
        }
        try {
            ldapAuthenticator.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(user.getUsername(), password));
            return Verification.accepted();
        } catch (AuthenticationException e) {
            return Verification.rejected();
        } catch (RuntimeException e) {
            // servidor fora do ar ou mal configurado: a tentativa ainda é registrada como INVALID_CREDENTIALS
            log.error("Falha ao consultar o LDAP para o usuário {}", user.getUsername(), e);
            return Verification.rejected();
        }
    }
}
