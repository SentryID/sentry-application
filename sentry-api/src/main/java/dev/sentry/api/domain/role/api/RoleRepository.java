package dev.sentry.api.domain.role.api;

import dev.sentry.api.domain.role.Role;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends ListCrudRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    Role findByIdSystemAndSystemRole(Long idSystem, String systemRole);
}
