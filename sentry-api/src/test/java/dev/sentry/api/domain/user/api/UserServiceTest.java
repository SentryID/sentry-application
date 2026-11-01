package dev.sentry.api.domain.user.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.User.AuthenticationType;
import dev.sentry.api.security.password.PasswordService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {

    private static final String PASSWORD = "S3nha!forte";
    private static final String HASH = "{argon2}$argon2id$v=19$m=19456,t=2,p=1$salt$hash";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordService passwordService = mock(PasswordService.class);
    private final UserService userService = new UserService(userRepository, userMapper, passwordService);

    private static UserRest.SaveRequest saveRequest() {
        return new UserRest.SaveRequest("Maria Silva", AuthenticationType.DATABASE, "maria.silva", PASSWORD, "maria@sentry.dev", true);
    }

    // RN-F0-02.7
    @Test
    void saveStoresHashedPassword() {
        when(userMapper.toEntity(any(UserRest.SaveRequest.class))).thenReturn(new User());
        when(passwordService.hash(PASSWORD)).thenReturn(HASH);

        userService.save(saveRequest());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(HASH, saved.getValue().getPassword());
    }

    @Test
    void saveRejectsDuplicatedUsername() {
        when(userRepository.findByUsernameIgnoreCase("maria.silva")).thenReturn(new User());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> userService.save(saveRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
        verify(passwordService, never()).hash(any());
    }

    @Test
    void saveRejectsDuplicatedEmail() {
        when(userRepository.findByEmail("maria@sentry.dev")).thenReturn(new User());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> userService.save(saveRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    // RN-F0-02.6
    @Test
    void saveRequestDoesNotExposePasswordInToString() {
        assertFalse(saveRequest().toString().contains(PASSWORD));
    }

    // RN-F1-02.7
    @Test
    void saveStoresLdapUserWithoutPassword() {
        User mapped = new User();
        mapped.setPassword(PASSWORD);
        when(userMapper.toEntity(any(UserRest.SaveRequest.class))).thenReturn(mapped);

        userService.save(ldapSaveRequest(null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertNull(saved.getValue().getPassword());
        verify(passwordService, never()).hash(any());
    }

    // RN-F1-02.7
    @Test
    void saveRejectsPasswordForLdapUser() {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> userService.save(ldapSaveRequest(PASSWORD)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Usuários LDAP não têm senha no Sentry", exception.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveRequiresPasswordForDatabaseUser() {
        var request = new UserRest.SaveRequest("Maria Silva", AuthenticationType.DATABASE, "maria.silva", " ",
                "maria@sentry.dev", true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.save(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Senha obrigatória para usuários DATABASE", exception.getReason());
        verify(userRepository, never()).save(any());
    }

    // RN-F1-02.7
    @Test
    void updateToLdapClearsStoredHash() {
        User user = new User();
        user.setPassword(HASH);
        user.setAuthenticationType(AuthenticationType.DATABASE);
        var request = new UserRest.UpdateRequest("Maria Silva", AuthenticationType.LDAP, "maria@sentry.dev", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            user.setAuthenticationType(AuthenticationType.LDAP);
            return null;
        }).when(userMapper).updateEntity(request, user);

        userService.update(1L, request);

        assertNull(user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void updateKeepsHashOfDatabaseUser() {
        User user = new User();
        user.setPassword(HASH);
        user.setAuthenticationType(AuthenticationType.DATABASE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.update(1L,
                new UserRest.UpdateRequest("Maria Silva", AuthenticationType.DATABASE, "maria@sentry.dev", true));

        assertEquals(HASH, user.getPassword());
    }

    private static UserRest.SaveRequest ldapSaveRequest(String password) {
        return new UserRest.SaveRequest("Maria Silva", AuthenticationType.LDAP, "maria.silva", password,
                "maria@sentry.dev", true);
    }
}
