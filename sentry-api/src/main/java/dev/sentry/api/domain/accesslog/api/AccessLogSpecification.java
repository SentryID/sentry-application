package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public class AccessLogSpecification {

    private AccessLogSpecification() {
    }

    public static Specification<AccessLog> findBy(AccessLogRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryParams.idUser() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idUser"), queryParams.idUser()));
            }
            if (queryParams.idSystem() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("idSystem"), queryParams.idSystem()));
            }
            if (queryParams.attemptResult() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("attemptResult"), queryParams.attemptResult()));
            }
            if (queryParams.ipHost() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.upper(root.get("ipHost")), "%" + queryParams.ipHost().toUpperCase() + "%"));
            }
            if (queryParams.attemptedAtFrom() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("attemptedAt"), queryParams.attemptedAtFrom()));
            }
            if (queryParams.attemptedAtTo() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(
                        root.<LocalDateTime>get("attemptedAt"), queryParams.attemptedAtTo()));
            }
            return predicate;
        };
    }
}
