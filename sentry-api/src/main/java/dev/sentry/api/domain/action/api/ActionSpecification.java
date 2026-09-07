package dev.sentry.api.domain.action.api;

import dev.sentry.api.domain.action.Action;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ActionSpecification {

    private ActionSpecification() {
    }

    public static Specification<Action> findBy(ActionRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.actionType() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("actionType")), "%" + queryParams.actionType().toUpperCase() + "%"));
            }
            if (queryParams.idSystem() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idSystem"), queryParams.idSystem()));
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
