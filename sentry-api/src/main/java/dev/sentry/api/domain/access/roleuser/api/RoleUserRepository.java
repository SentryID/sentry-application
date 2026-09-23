package dev.sentry.api.domain.access.roleuser.api;

import dev.sentry.api.domain.access.roleuser.RoleUser;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleUserRepository extends ListCrudRepository<RoleUser, Long>, JpaSpecificationExecutor<RoleUser> {

    RoleUser findByRoleIdAndUserId(Long idRole, Long idUser);
}
