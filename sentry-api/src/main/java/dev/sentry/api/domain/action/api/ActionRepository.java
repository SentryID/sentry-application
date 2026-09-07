package dev.sentry.api.domain.action.api;

import dev.sentry.api.domain.action.Action;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends ListCrudRepository<Action, Long>, JpaSpecificationExecutor<Action> {

    Action findByIdSystemAndActionType(Long idSystem, String actionType);
}
