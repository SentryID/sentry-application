package dev.sentry.api.domain.organization.api;

import dev.sentry.api.domain.organization.Organization;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository
        extends ListCrudRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

    Organization findByExternalCode(String externalCode);
}
