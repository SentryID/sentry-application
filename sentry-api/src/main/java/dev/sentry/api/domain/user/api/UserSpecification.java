package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> findBy(UserRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.name() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("name")), "%" + queryParams.name().toUpperCase() + "%"));
            }
            if (queryParams.username() != null) {
                predicate = cb.and(predicate, cb.like(cb.upper(root.get("username").get("value")),
                        "%" + queryParams.username().toUpperCase() + "%"));
            }
            if (queryParams.email() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("email").get("value")), "%" + queryParams.email().toUpperCase() + "%"));
            }
            if (queryParams.authenticationType() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("credentials").get("authenticationType"), queryParams.authenticationType()));
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
