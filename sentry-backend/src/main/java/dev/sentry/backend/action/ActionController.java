package dev.sentry.backend.action;

import dev.sentry.api.domain.action.api.ActionRest;
import dev.sentry.api.domain.action.api.ActionService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActionController implements ActionControllerSwagger {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public List<ActionRest.Response> findByQueryParams(ActionRest.QueryRequest queryParams) {
        return actionService.findByQueryParams(queryParams);
    }

    @Override
    public ActionRest.Response findById(Long id) {
        return actionService.findById(id);
    }

    @Override
    public void save(ActionRest.SaveRequest request) {
        actionService.save(request);
    }

    @Override
    public void update(Long id, ActionRest.UpdateRequest request) {
        actionService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        actionService.delete(id);
    }
}
