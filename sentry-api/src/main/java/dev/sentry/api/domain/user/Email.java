package dev.sentry.api.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * E-mail normalizado. Guardar sempre em minúsculo é o que impede
 * {@code Maria@x.com} e {@code maria@x.com} virarem duas contas.
 */
@Embeddable
public record Email(@Column(name = "email", nullable = false, unique = true) String value) {

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!FORMATO.matcher(value).matches()) {
            throw new IllegalArgumentException("E-mail inválido: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
