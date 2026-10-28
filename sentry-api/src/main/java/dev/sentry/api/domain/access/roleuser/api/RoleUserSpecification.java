package dev.sentry.api.domain.access.roleuser.api;

import dev.sentry.api.domain.access.roleuser.RoleUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class RoleUserSpecification {

    private RoleUserSpecification() {
    }

    public static Specification<RoleUser> findBy(RoleUserRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idRole() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("role").get("id"), queryParams.idRole()));
            }
            if (queryParams.idUser() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("user").get("id"), queryParams.idUser()));
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
