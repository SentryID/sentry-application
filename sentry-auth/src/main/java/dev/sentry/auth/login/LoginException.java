package dev.sentry.auth.login;

import org.springframework.http.HttpStatus;

/** Falha do login direto, respondida no formato de erro do OAuth ({@code error} e {@code error_description}). */
public class LoginException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public LoginException(HttpStatus status, String error, String description) {
        super(description);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
