package dev.sentry.api.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Janela de validade de um token de uso único, compartilhada por {@code MfaToken} e
 * {@code PasswordToken}. É aqui que mora a regra: um token só é consumido uma vez, e só
 * antes de expirar.
 */
@Embeddable
public record TokenValidity(
        @Column(name = "valid_until") LocalDateTime validUntil,
        @Column(name = "is_used", nullable = false) Boolean isUsed,
        @Column(name = "used_at") LocalDateTime usedAt) {

    public TokenValidity {
        if (validUntil == null) {
            throw new IllegalArgumentException("Um token precisa de data de validade");
        }
    }

    public static TokenValidity validUntil(LocalDateTime limite) {
        return new TokenValidity(limite, false, null);
    }

    /** Marca o token como usado, ou recusa se já foi usado ou expirou. */
    public TokenValidity consume(LocalDateTime agora) {
        if (Boolean.TRUE.equals(isUsed)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Token já utilizado");
        }
        if (agora.isAfter(validUntil)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado");
        }
        return new TokenValidity(validUntil, true, agora);
    }

    public boolean isExpired(LocalDateTime agora) {
        return agora.isAfter(validUntil);
    }
}
