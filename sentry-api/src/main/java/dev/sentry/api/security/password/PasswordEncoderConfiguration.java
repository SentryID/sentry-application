package dev.sentry.api.security.password;

import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Encoder de senha compartilhado por `sentry-auth` e `sentry-backend`. O hash sai com o prefixo do
 * algoritmo (`{argon2}$argon2id$...`), o que permite trocar o padrão no futuro sem migrar as senhas
 * já gravadas (RN-F0-02.1, RN-F0-02.3).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Argon2Properties.class)
public class PasswordEncoderConfiguration {

    static final String ARGON2_ID = "argon2";
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    @Bean
    PasswordEncoder passwordEncoder(Argon2Properties properties) {
        return delegatingEncoder(argon2(properties.memory(), properties.iterations(), properties.parallelism()));
    }

    static Argon2PasswordEncoder argon2(int memory, int iterations, int parallelism) {
        return new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, parallelism, memory, iterations);
    }

    static PasswordEncoder delegatingEncoder(Argon2PasswordEncoder argon2) {
        return new DelegatingPasswordEncoder(ARGON2_ID, Map.of(ARGON2_ID, argon2));
    }
}
