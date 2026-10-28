package dev.sentry.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registra o domínio compartilhado (`dev.sentry.api`) em qualquer aplicação que tenha o
 * `sentry-api` no classpath. Ligada pelo arquivo
 * `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
 */
@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@ComponentScan
@EntityScan
@EnableJpaRepositories
@EnableJpaAuditing
public class ApiAutoConfiguration {}
