package dev.sentry.backend.action;

import dev.sentry.api.domain.access.action.api.ActionRest;
import dev.sentry.api.domain.access.action.api.ActionService;
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
    public ActionRest.Response findByCode(String code) {
        return actionService.findByCode(code);
    }

    @Override
    public void save(ActionRest.SaveRequest request) {
        actionService.save(request);
    }

    @Override
    public void update(String code, ActionRest.UpdateRequest request) {
        actionService.update(code, request);
    }

    @Override
    public void delete(String code) {
        actionService.delete(code);
    }
}
