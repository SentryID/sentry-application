package dev.sentry.auth;

import dev.sentry.auth.login.LoginException;
import dev.sentry.auth.login.LoginRest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tratamento de erros único dos controllers do sentry-auth: Problem Details (RFC 9457) com a mensagem em português no
 * {@code detail}, e o formato do OAuth no login direto. Os endpoints do Authorization Server têm o próprio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Erros do login direto no formato do OAuth ({@code error}, {@code error_description}), sem cache (RFC 6749, 5.2). */
    @ExceptionHandler(LoginException.class)
    ResponseEntity<LoginRest.ErrorResponse> handleLogin(LoginException exception) {
        var response = ResponseEntity.status(exception.getStatus()).cacheControl(CacheControl.noStore());
        if ("invalid_client".equals(exception.getError())) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"sentry\"");
        }
        return response.body(new LoginRest.ErrorResponse(exception.getError(), exception.getMessage()));
    }
}
