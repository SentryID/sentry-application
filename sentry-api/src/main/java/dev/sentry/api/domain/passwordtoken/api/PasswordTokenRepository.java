package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordTokenRepository
        extends ListCrudRepository<PasswordToken, Long>, JpaSpecificationExecutor<PasswordToken> {
}
