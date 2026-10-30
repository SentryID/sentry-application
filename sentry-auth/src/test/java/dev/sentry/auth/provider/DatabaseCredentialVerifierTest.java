package dev.sentry.auth.provider;

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
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.api.security.password.PasswordService.Verification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseCredentialVerifierTest {

    private static final String PASSWORD = "S3nha!forte";
    private static final String HASH = "{argon2}hash";

    private final PasswordService passwordService = mock(PasswordService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DatabaseCredentialVerifier verifier = new DatabaseCredentialVerifier(passwordService, userRepository);
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setPassword(HASH);
    }

    @Test
    void verifiesDatabaseUsers() {
        assertEquals(AuthenticationType.DATABASE, verifier.type());
    }

    @Test
    void matchingPasswordIsAccepted() {
        when(passwordService.verify(PASSWORD, HASH)).thenReturn(new Verification(true, null));

        assertTrue(verifier.verify(user, PASSWORD).matches());
    }

    @Test
    void wrongPasswordIsRejected() {
        when(passwordService.verify("errada", HASH)).thenReturn(new Verification(false, null));

        assertFalse(verifier.verify(user, "errada").matches());
    }

    // RN-F0-02.4: o novo hash só é gravado quando a tentativa inteira tem sucesso
    @Test
    void upgradedHashIsSavedOnlyOnSuccess() {
        when(passwordService.verify(PASSWORD, HASH)).thenReturn(new Verification(true, "{argon2}novo"));

        var verification = verifier.verify(user, PASSWORD);

        assertTrue(verification.matches());
        assertEquals(HASH, user.getPassword());
        verify(userRepository, never()).save(any());

        verification.onSuccess().run();

        assertEquals("{argon2}novo", user.getPassword());
        verify(userRepository).save(user);
    }
}
