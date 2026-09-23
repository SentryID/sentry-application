package dev.sentry.api.domain.access.roleaction.api;

import dev.sentry.api.domain.access.roleaction.RoleAction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class RoleActionSpecification {

    private RoleActionSpecification() {
    }

    public static Specification<RoleAction> findBy(RoleActionRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idRole() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("role").get("id"), queryParams.idRole()));
            }
            if (queryParams.cdAction() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("action").get("id"), queryParams.cdAction()));
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
