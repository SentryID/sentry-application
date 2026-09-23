package dev.sentry.auth.login;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController implements LoginControllerSwagger {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public LoginRest.LoginResponse login(LoginRest.LoginRequest request) {
        return loginService.login(request);
    }
}
