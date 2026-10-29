package dev.sentry.api.security.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PasswordEncoderConfiguration.class);

    @Test
    void usesOwaspParametersByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(PasswordEncoder.class).encode("x"))
                .startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$"));
    }

    // RN-F0-02.2
    @Test
    void parametersAreConfigurable() {
        contextRunner
                .withPropertyValues(
                        "sentry.security.password.argon2.memory=32768",
                        "sentry.security.password.argon2.iterations=3",
                        "sentry.security.password.argon2.parallelism=2")
                .run(context -> assertThat(context.getBean(PasswordEncoder.class).encode("x"))
                        .startsWith("{argon2}$argon2id$v=19$m=32768,t=3,p=2$"));
    }

    @Test
    void rejectsParametersBelowOwaspMinimum() {
        contextRunner
                .withPropertyValues("sentry.security.password.argon2.memory=8192")
                .run(context -> assertThat(context).hasFailed());
    }
}
