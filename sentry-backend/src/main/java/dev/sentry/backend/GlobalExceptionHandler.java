package dev.sentry.backend;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tratamento de erros único dos controllers do sentry-backend, em Problem Details (RFC 9457): a mensagem em português
 * das {@code ResponseStatusException} dos services vai no campo {@code detail}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
}
