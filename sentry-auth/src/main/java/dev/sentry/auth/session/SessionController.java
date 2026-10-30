package dev.sentry.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController implements SessionControllerSwagger {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public SessionRest.Context context(HttpServletRequest request) {
        return sessionService.context(request);
    }

    @Override
    public SessionRest.LoginResponse login(SessionRest.LoginRequest login, HttpServletRequest request,
            HttpServletResponse response) {
        return sessionService.login(login, request, response);
    }

    @Override
    public SessionRest.Me me() {
        return sessionService.me();
    }

    @Override
    public void logout(HttpServletRequest request) {
        sessionService.logout(request);
    }
}
