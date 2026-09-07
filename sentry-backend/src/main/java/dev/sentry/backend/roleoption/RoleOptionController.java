package dev.sentry.backend.roleoption;

import dev.sentry.api.domain.roleoption.api.RoleOptionRest;
import dev.sentry.api.domain.roleoption.api.RoleOptionService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleOptionController implements RoleOptionControllerSwagger {

    private final RoleOptionService roleOptionService;

    public RoleOptionController(RoleOptionService roleOptionService) {
        this.roleOptionService = roleOptionService;
    }

    @Override
    public List<RoleOptionRest.Response> findByQueryParams(RoleOptionRest.QueryRequest queryParams) {
        return roleOptionService.findByQueryParams(queryParams);
    }

    @Override
    public RoleOptionRest.Response findById(Long id) {
        return roleOptionService.findById(id);
    }

    @Override
    public void save(RoleOptionRest.SaveRequest request) {
        roleOptionService.save(request);
    }

    @Override
    public void update(Long id, RoleOptionRest.UpdateRequest request) {
        roleOptionService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        roleOptionService.delete(id);
    }
}
