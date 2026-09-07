package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.System;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemRepository extends ListCrudRepository<System, Long>, JpaSpecificationExecutor<System> {

    System findByExternalCode(String externalCode);
}
