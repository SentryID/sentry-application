package dev.sentry.api.domain.mfatoken.api;

import dev.sentry.api.domain.mfatoken.MfaToken;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MfaTokenRepository extends ListCrudRepository<MfaToken, Long>, JpaSpecificationExecutor<MfaToken> {
}
