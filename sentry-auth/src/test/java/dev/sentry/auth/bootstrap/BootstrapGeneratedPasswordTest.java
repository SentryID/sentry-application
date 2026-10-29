package dev.sentry.auth.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.TestcontainersConfiguration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** CA-F0-03.3: sem SENTRY_ADMIN_PASSWORD, a senha gerada sai uma vez no log e permite login. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "sentry.admin.username=generated.admin",
        "sentry.admin.email=generated.admin@sentry.local"})
@Import(TestcontainersConfiguration.class)
class BootstrapGeneratedPasswordTest {

    private static final Pattern GENERATED = Pattern.compile("criado com a senha gerada: (\\S+) ");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @Test
    void logsGeneratedPasswordOnceAndItMatchesStoredHash(CapturedOutput output) {
        Matcher matcher = GENERATED.matcher(output.getAll());
        assertTrue(matcher.find(), "senha gerada não apareceu no log");
        String password = matcher.group(1);

        assertTrue(password.length() >= 20);
        assertEquals(1, output.getAll().split(Pattern.quote(password), -1).length - 1);

        String stored = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = 'generated.admin'", String.class);
        assertTrue(passwordService.verify(password, stored).matches());
    }
}
