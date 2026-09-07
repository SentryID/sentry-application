package dev.sentry.api.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TokenValidityTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 2, 20, 14, 0);

    @Test
    void consumirMarcaComoUsadoEGuardaAHora() {
        TokenValidity validity = TokenValidity.validUntil(AGORA.plusMinutes(10));

        TokenValidity consumido = validity.consume(AGORA);

        assertTrue(consumido.isUsed());
        assertEquals(AGORA, consumido.usedAt());
    }

    @Test
    void consumirDuasVezesFalha() {
        TokenValidity consumido = TokenValidity.validUntil(AGORA.plusMinutes(10)).consume(AGORA);

        ResponseStatusException erro =
                assertThrows(ResponseStatusException.class, () -> consumido.consume(AGORA.plusMinutes(1)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void consumirExpiradoFalha() {
        TokenValidity validity = TokenValidity.validUntil(AGORA.minusSeconds(1));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> validity.consume(AGORA));

        assertEquals(HttpStatus.GONE, erro.getStatusCode());
    }

    @Test
    void nasceNaoUsado() {
        TokenValidity validity = TokenValidity.validUntil(AGORA.plusMinutes(10));

        assertFalse(validity.isUsed());
        assertFalse(validity.isExpired(AGORA));
    }

    @Test
    void tokenSemValidadeEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> TokenValidity.validUntil(null));
    }
}
