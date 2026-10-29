package dev.sentry.api.domain.access.roleuser.api;

import dev.sentry.api.domain.access.roleuser.RoleUser;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleUserRepository extends ListCrudRepository<RoleUser, Long>, JpaSpecificationExecutor<RoleUser> {

    RoleUser findByRoleIdAndUserId(Long idRole, Long idUser);

    /** RN-F1-02.5: o usuário tem pelo menos um papel ativo no sistema. */
    @Query("""
            SELECT count(ru) > 0 FROM RoleUser ru
            WHERE ru.user.id = :idUser AND ru.role.system.id = :idSystem
              AND ru.isActive = true AND ru.isDeleted = false
              AND ru.role.isActive = true AND ru.role.isDeleted = false""")
    boolean hasActiveRoleInSystem(Long idUser, Long idSystem);
}
