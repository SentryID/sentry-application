package dev.sentry.api.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;

/** Login normalizado — mesma razão do {@link Email}: evitar contas duplicadas por caixa. */
@Embeddable
public record Username(@Column(name = "username", nullable = false, unique = true) String value) {

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
