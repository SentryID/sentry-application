package dev.sentry.api.domain.mfatoken.api;

import dev.sentry.api.domain.mfatoken.MfaToken;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class MfaTokenSpecification {

    private MfaTokenSpecification() {
    }

    public static Specification<MfaToken> findBy(MfaTokenRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.code() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("code"), queryParams.code()));
            }
            if (queryParams.token() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("token"), queryParams.token()));
            }
            if (queryParams.username() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("username")), "%" + queryParams.username().toUpperCase() + "%"));
            }
            if (queryParams.isUsed() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isUsed"), queryParams.isUsed()));
            }
            return predicate;
        };
    }
}
