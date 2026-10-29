package dev.sentry.api.domain.user.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.security.password.PasswordService;
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
        return new UserRest.SaveRequest("Maria Silva", "LOCAL", "maria.silva", PASSWORD, "maria@sentry.dev", true);
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
}
