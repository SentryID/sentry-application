package dev.sentry.api.security.password;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Parâmetros do Argon2id (RN-F0-02.2). Os padrões e os mínimos são os recomendados pela OWASP:
 * 19 MiB de memória, 2 iterações e paralelismo 1.
 *
 * @param memory memória em KiB
 * @param iterations número de iterações
 * @param parallelism grau de paralelismo
 */
@Validated
@ConfigurationProperties("sentry.security.password.argon2")
public record Argon2Properties(
        @DefaultValue("19456") @Min(19456) int memory,
        @DefaultValue("2") @Min(2) int iterations,
        @DefaultValue("1") @Min(1) int parallelism) {
}
