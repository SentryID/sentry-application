package dev.sentry.auth.login;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController implements LoginControllerSwagger {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public ResponseEntity<LoginRest.TokenResponse> login(LoginRest.LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(loginService.login(request, httpRequest));
    }
}
