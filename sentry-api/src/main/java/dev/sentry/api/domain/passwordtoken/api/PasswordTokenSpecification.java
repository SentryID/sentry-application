package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class PasswordTokenSpecification {

    private PasswordTokenSpecification() {
    }

    public static Specification<PasswordToken> findBy(PasswordTokenRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.token() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("token"), queryParams.token()));
            }
            if (queryParams.idUser() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idUser"), queryParams.idUser()));
            }
            if (queryParams.isUsed() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isUsed"), queryParams.isUsed()));
            }
            return predicate;
        };
    }
}
