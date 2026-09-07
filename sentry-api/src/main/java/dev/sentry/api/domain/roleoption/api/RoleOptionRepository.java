package dev.sentry.api.domain.roleoption.api;

import dev.sentry.api.domain.roleoption.RoleOption;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleOptionRepository
        extends ListCrudRepository<RoleOption, Long>, JpaSpecificationExecutor<RoleOption> {

    RoleOption findByIdRoleAndCdAction(Long idRole, Long cdAction);
}
