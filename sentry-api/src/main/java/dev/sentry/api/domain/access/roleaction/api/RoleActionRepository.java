package dev.sentry.api.domain.access.roleaction.api;

import dev.sentry.api.domain.access.roleaction.RoleAction;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleActionRepository
        extends ListCrudRepository<RoleAction, Long>, JpaSpecificationExecutor<RoleAction> {

    RoleAction findByRoleIdAndActionId(Long idRole, Long cdAction);
}
