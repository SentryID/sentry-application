package dev.sentry.api.domain.roleuser.api;

import dev.sentry.api.domain.roleuser.RoleUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class RoleUserSpecification {

    private RoleUserSpecification() {
    }

    public static Specification<RoleUser> findBy(RoleUserRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idGroup() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idGroup"), queryParams.idGroup()));
            }
            if (queryParams.idUser() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idUser"), queryParams.idUser()));
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
