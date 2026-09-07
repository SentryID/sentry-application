package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.System;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class SystemSpecification {

    private SystemSpecification() {
    }

    public static Specification<System> findBy(SystemRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idOrganization() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idOrganization"), queryParams.idOrganization()));
            }
            if (queryParams.externalCode() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("externalCode"), queryParams.externalCode()));
            }
            if (queryParams.description() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("description")), "%" + queryParams.description().toUpperCase() + "%"));
            }
            if (queryParams.url() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("url")), "%" + queryParams.url().toUpperCase() + "%"));
            }
            if (queryParams.uuid() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("uuid"), queryParams.uuid()));
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
