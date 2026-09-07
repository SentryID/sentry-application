package dev.sentry.api.domain.roleoption.api;

import dev.sentry.api.domain.roleoption.RoleOption;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class RoleOptionSpecification {

    private RoleOptionSpecification() {
    }

    public static Specification<RoleOption> findBy(RoleOptionRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idRole() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idRole"), queryParams.idRole()));
            }
            if (queryParams.cdAction() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cdAction"), queryParams.cdAction()));
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
