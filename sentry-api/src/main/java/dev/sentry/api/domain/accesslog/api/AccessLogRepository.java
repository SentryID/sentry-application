package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessLogRepository extends ListCrudRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {
}
