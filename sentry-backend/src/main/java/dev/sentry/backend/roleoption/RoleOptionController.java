package dev.sentry.backend.roleoption;

import dev.sentry.api.domain.access.roleaction.api.RoleActionRest;
import dev.sentry.api.domain.access.roleaction.api.RoleActionService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleOptionController implements RoleOptionControllerSwagger {

    private final RoleActionService roleActionService;

    public RoleOptionController(RoleActionService roleActionService) {
        this.roleActionService = roleActionService;
    }

    @Override
    public List<RoleActionRest.Response> findByQueryParams(RoleActionRest.QueryRequest queryParams) {
        return roleActionService.findByQueryParams(queryParams);
    }

    @Override
    public RoleActionRest.Response findById(Long id) {
        return roleActionService.findById(id);
    }

    @Override
    public void save(RoleActionRest.SaveRequest request) {
        roleActionService.save(request);
    }

    @Override
    public void update(Long id, RoleActionRest.UpdateRequest request) {
        roleActionService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        roleActionService.delete(id);
    }
}
