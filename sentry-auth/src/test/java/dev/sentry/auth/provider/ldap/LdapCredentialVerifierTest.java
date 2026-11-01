package dev.sentry.auth.provider.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.User.AuthenticationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.LdapAuthenticator;

// RN-F1-02.7
class LdapCredentialVerifierTest {

    private static final String PASSWORD = "S3nha!forte";

    private final LdapAuthenticator ldapAuthenticator = mock(LdapAuthenticator.class);
    private final LdapCredentialVerifier verifier = new LdapCredentialVerifier(ldapAuthenticator);
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("maria.silva");
    }

    @Test
    void verifiesLdapUsers() {
        assertEquals(AuthenticationType.LDAP, verifier.type());
    }

    @Test
    void acceptsWhenBindSucceeds() {
        assertTrue(verifier.verify(user, PASSWORD).matches());
    }

    @Test
    void rejectsWhenBindFails() {
        when(ldapAuthenticator.authenticate(any())).thenThrow(new BadCredentialsException("bind"));

        assertFalse(verifier.verify(user, PASSWORD).matches());
    }

    @Test
    void rejectsWithoutPropagatingWhenServerIsDown() {
        when(ldapAuthenticator.authenticate(any())).thenThrow(new IllegalStateException("connection refused"));

        assertFalse(verifier.verify(user, PASSWORD).matches());
    }

    // bind sem senha vira bind anônimo em muitos servidores
    @Test
    void rejectsEmptyPasswordWithoutBinding() {
        assertFalse(verifier.verify(user, null).matches());
        assertFalse(verifier.verify(user, "").matches());

        verify(ldapAuthenticator, never()).authenticate(any());
    }

    // o login é achado sem diferenciar maiúsculas; o DN usa o username cadastrado
    @Test
    void bindsWithRegisteredUsername() {
        verifier.verify(user, PASSWORD);

        ArgumentCaptor<Authentication> bind = ArgumentCaptor.forClass(Authentication.class);
        verify(ldapAuthenticator).authenticate(bind.capture());
        assertEquals("maria.silva", bind.getValue().getName());
        assertEquals(PASSWORD, bind.getValue().getCredentials());
    }
}
