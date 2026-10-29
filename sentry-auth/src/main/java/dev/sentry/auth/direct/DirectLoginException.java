package dev.sentry.auth.direct;

import org.springframework.http.HttpStatus;

/** Falha do login direto, respondida no formato de erro do OAuth ({@code error} e {@code error_description}). */
public class DirectLoginException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public DirectLoginException(HttpStatus status, String error, String description) {
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
