package dev.sentry.api.domain.access.action.api;

import dev.sentry.api.domain.access.action.Action;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends ListCrudRepository<Action, Long>, JpaSpecificationExecutor<Action> {

    Action findBySystemIdAndActionType(Long idSystem, String actionType);
}
