package dev.sentry.auth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.access.roleuser.api.RoleUserRepository;
import dev.sentry.api.domain.accesslog.AccessLog;
import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.domain.accesslog.api.AccessLogService;
import dev.sentry.api.domain.organization.Organization;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.api.security.password.PasswordService.Verification;
import dev.sentry.auth.provider.DatabaseAuthenticationProvider.AttemptResult;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class DatabaseAuthenticationProviderTest {

    private static final String PASSWORD = "S3nha!forte";
    private static final String HASH = "{argon2}hash";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final RoleUserRepository roleUserRepository = mock(RoleUserRepository.class);
    private final PasswordService passwordService = mock(PasswordService.class);
    private final AccessLogService accessLogService = mock(AccessLogService.class);
    private DatabaseAuthenticationProvider provider;
    private User user;

    @BeforeEach
    void setUp() {
        provider = new DatabaseAuthenticationProvider(userRepository, systemRepository, roleUserRepository,
                passwordService, accessLogService);
        Organization organization = new Organization();
        ExternalSystem system = new ExternalSystem();
        system.setId(10L);
        system.setOrganization(organization);
        when(systemRepository.findByExternalCode("SYS001")).thenReturn(system);

        user = new User();
        user.setId(1L);
        user.setUsername("john.doe");
        user.setPassword(HASH);
        user.setAuthenticationType("DATABASE");
        when(userRepository.findByUsernameIgnoreCase("john.doe")).thenReturn(user);
        when(passwordService.verify(PASSWORD, HASH)).thenReturn(new Verification(true, null));
        when(passwordService.verify(eq("errada"), any())).thenReturn(new Verification(false, null));
        when(roleUserRepository.hasActiveRoleInSystem(1L, 10L)).thenReturn(true);
    }

    private static Authentication attempt(String username, String password) {
        var token = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        token.setDetails(new LoginAttempt("SYS001", Channel.PKCE, "127.0.0.1", "JUnit"));
        return token;
    }

    // CA-F1-02.1, RN-F1-05.4
    @Test
    void authenticatesActiveUserWithRoleAndReturnsUuidAsName() {
        Authentication result = provider.authenticate(attempt("john.doe", PASSWORD));

        assertEquals(user.getUuid().toString(), result.getName());
        assertEquals(true, result.isAuthenticated());
    }

    // CA-F1-02.2: inexistente, senha errada, inativo e excluído recebem a mesma resposta
    @Test
    void everyFailureGivesTheSameMessage() {
        assertGenericFailure("fulano", PASSWORD, u -> { });
        assertGenericFailure("john.doe", "errada", u -> { });
        assertGenericFailure("john.doe", PASSWORD, u -> u.setIsActive(false));
        assertGenericFailure("john.doe", PASSWORD, u -> {
            u.setIsActive(true);
            u.setIsDeleted(true);
        });
    }

    private void assertGenericFailure(String username, String password, Consumer<User> change) {
        change.accept(user);
        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> provider.authenticate(attempt(username, password)));
        assertEquals("Usuário ou senha inválidos", exception.getMessage());
    }

    @Test
    void reasonsAreKeptInternally() {
        assertEquals(AttemptResult.USER_NOT_FOUND, provider.evaluate("fulano", PASSWORD, "SYS001").result());
        assertEquals(AttemptResult.INVALID_CREDENTIALS, provider.evaluate("john.doe", "errada", "SYS001").result());
        user.setIsActive(false);
        assertEquals(AttemptResult.USER_INACTIVE, provider.evaluate("john.doe", PASSWORD, "SYS001").result());
    }

    // RN-F1-02.4
    @Test
    void unknownUserStillVerifiesAHash() {
        provider.evaluate("fulano", PASSWORD, "SYS001");

        verify(passwordService).verify(eq(PASSWORD), any());
    }

    // CA-F1-02.3, RN-F1-02.6
    @Test
    void userWithoutRoleInSystemHasNoAccessAndReasonIsRecorded() {
        when(roleUserRepository.hasActiveRoleInSystem(1L, 10L)).thenReturn(false);

        assertEquals(AttemptResult.NO_SYSTEM_ACCESS, provider.evaluate("john.doe", PASSWORD, "SYS001").result());
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(attempt("john.doe", PASSWORD)));
        assertEquals("NO_SYSTEM_ACCESS", recorded().getAttemptResult());
    }

    // RN-F1-10.1, RN-F1-10.5
    @Test
    void successIsRecordedWithChannelAndUserAgent() {
        provider.authenticate(attempt("john.doe", PASSWORD));

        AccessLog accessLog = recorded();
        assertEquals("SUCCESS", accessLog.getAttemptResult());
        assertEquals(user, accessLog.getUser());
        assertEquals(Channel.PKCE, accessLog.getChannel());
        assertEquals("JUnit", accessLog.getUserAgent());
        assertEquals("127.0.0.1", accessLog.getIpHost());
    }

    private AccessLog recorded() {
        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogService).recordAttempt(captor.capture());
        return captor.getValue();
    }

    // RN-F1-01.8
    @Test
    void inactiveOrUnknownSystemFails() {
        assertEquals(AttemptResult.SYSTEM_INACTIVE, provider.evaluate("john.doe", PASSWORD, "OUTRO").result());
        systemRepository.findByExternalCode("SYS001").getOrganization().setIsActive(false);
        assertEquals(AttemptResult.SYSTEM_INACTIVE, provider.evaluate("john.doe", PASSWORD, "SYS001").result());
        verify(roleUserRepository, never()).hasActiveRoleInSystem(anyLong(), anyLong());
    }

    // CA-F1-02.5
    @Test
    void ldapUserIsNotAuthenticatedByDatabaseProvider() {
        user.setAuthenticationType("LDAP");

        assertEquals(AttemptResult.INVALID_CREDENTIALS, provider.evaluate("john.doe", PASSWORD, "SYS001").result());
    }

    // RN-F0-02.4: o re-hash da 0.6 passa a ser gravado no login
    @Test
    void successUpgradesWeakHash() {
        when(passwordService.verify(PASSWORD, HASH)).thenReturn(new Verification(true, "{argon2}novo"));

        provider.evaluate("john.doe", PASSWORD, "SYS001");

        assertEquals("{argon2}novo", user.getPassword());
        verify(userRepository).save(user);
    }
}
