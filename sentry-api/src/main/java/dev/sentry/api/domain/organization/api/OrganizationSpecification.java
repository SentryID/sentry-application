package dev.sentry.api.domain.organization.api;

import dev.sentry.api.domain.organization.Organization;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class OrganizationSpecification {

    private OrganizationSpecification() {
    }

    public static Specification<Organization> findBy(OrganizationRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.name() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("name")), "%" + queryParams.name().toUpperCase() + "%"));
            }
            if (queryParams.externalCode() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("externalCode"), queryParams.externalCode()));
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
