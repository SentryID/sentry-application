package dev.sentry.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** CA-F0-01.1: com o banco vazio, o Flyway aplica as migrações e o contexto sobe com {@code ddl-auto: validate}. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthenticationApplicationTests {

	@Test
	void contextLoads() {
	}

}
