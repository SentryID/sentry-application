package dev.sentry.api.domain.access.role.api;

import dev.sentry.api.domain.access.role.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class RoleSpecification {

    private RoleSpecification() {
    }

    public static Specification<Role> findBy(RoleRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idSystem() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("system").get("id"), queryParams.idSystem()));
            }
            if (queryParams.idOrganization() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("organization").get("id"), queryParams.idOrganization()));
            }
            if (queryParams.systemRole() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("systemRole")), "%" + queryParams.systemRole().toUpperCase() + "%"));
            }
            if (queryParams.externalRole() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("externalRole")), "%" + queryParams.externalRole().toUpperCase() + "%"));
            }
            if (queryParams.roleType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("roleType"), queryParams.roleType()));
            }
            if (queryParams.isActive() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isActive"), queryParams.isActive()));
            }
            // sem filtro explícito, registros excluídos logicamente ficam fora da listagem
            predicate = cb.and(predicate,
                    cb.equal(root.get("isDeleted"), queryParams.isDeleted() != null && queryParams.isDeleted()));
            return predicate;
        };
    }
}
