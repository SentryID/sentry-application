package dev.sentry.auth.login;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private final LoginService loginService = new LoginService();

    @Test
    void loginIsNotImplementedYet() {
        var request = new LoginRest.LoginRequest("john.doe", "secret", "SYS001");

        assertThrows(UnsupportedOperationException.class, () -> loginService.login(request));
    }
}
