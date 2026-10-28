package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.ExternalSystem;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemRepository extends ListCrudRepository<ExternalSystem, Long>, JpaSpecificationExecutor<ExternalSystem> {

    ExternalSystem findByExternalCode(String externalCode);
}
